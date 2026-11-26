#include <jni.h>
#include <llama.h>

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace {
struct ModelHandle {
    llama_model * model = nullptr;
    int32_t context_size = 0;
    int32_t threads = 0;
};

std::once_flag backend_once;

void throw_java(JNIEnv * env, const char * message) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls != nullptr) env->ThrowNew(cls, message);
}

std::string from_java(JNIEnv * env, jstring value) {
    if (value == nullptr) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

bool add_token(llama_batch_ext * batch, llama_token token, llama_pos position, bool output) {
    const int32_t index = llama_batch_ext_add_token(batch, 0, token);
    if (index < 0) return false;
    if (!llama_batch_ext_set_pos(batch, index, &position)) return false;
    return llama_batch_ext_set_output_logits(batch, index, output);
}

void release_generation(
    llama_context * context,
    llama_batch_ext * batch,
    llama_sampler * sampler
) {
    if (sampler != nullptr) llama_sampler_free(sampler);
    if (batch != nullptr) llama_batch_ext_free(batch);
    if (context != nullptr) llama_free(context);
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_mana_os_core_JniManaLlamaCppRuntime_nativeLoadModel(
    JNIEnv * env, jobject /* thiz */, jstring model_path, jint context_size, jint threads
) {
    const std::string path = from_java(env, model_path);
    if (path.empty() || context_size < 256 || threads < 1) {
        throw_java(env, "Invalid llama.cpp model configuration");
        return 0;
    }

    std::call_once(backend_once, [] { llama_backend_init(); });

    llama_model_params params = llama_model_default_params();
    // CPU-only baseline is deliberate. GPU offload must be enabled only after device testing.
    params.n_gpu_layers = 0;
    llama_model * model = llama_model_load_from_file(path.c_str(), params);
    if (model == nullptr) {
        throw_java(env, "llama.cpp could not load the GGUF model");
        return 0;
    }

    auto handle = std::make_unique<ModelHandle>();
    handle->model = model;
    handle->context_size = context_size;
    handle->threads = threads;
    return static_cast<jlong>(reinterpret_cast<intptr_t>(handle.release()));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mana_os_core_JniManaLlamaCppRuntime_nativeGenerate(
    JNIEnv * env, jobject /* thiz */, jlong raw_handle, jstring prompt_value,
    jint max_tokens, jfloat temperature
) {
    auto * handle = reinterpret_cast<ModelHandle *>(static_cast<intptr_t>(raw_handle));
    if (handle == nullptr || handle->model == nullptr) {
        throw_java(env, "Invalid llama.cpp model handle");
        return nullptr;
    }

    const std::string prompt = from_java(env, prompt_value);
    if (prompt.empty() || max_tokens < 1 || max_tokens > 8192 ||
        temperature < 0.0f || temperature > 2.0f) {
        throw_java(env, "Invalid llama.cpp generation request");
        return nullptr;
    }

    const llama_vocab * vocab = llama_model_get_vocab(handle->model);
    const int32_t token_count = -llama_tokenize(
        vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()), nullptr, 0, true, true
    );
    if (token_count <= 0 || token_count + max_tokens > handle->context_size) {
        throw_java(env, "Prompt and output exceed the configured llama.cpp context");
        return nullptr;
    }

    std::vector<llama_token> prompt_tokens(static_cast<size_t>(token_count));
    const int32_t tokenized = llama_tokenize(
        vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()),
        prompt_tokens.data(), token_count, true, true
    );
    if (tokenized < 0) {
        throw_java(env, "llama.cpp prompt tokenization failed");
        return nullptr;
    }

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = static_cast<uint32_t>(handle->context_size);
    context_params.n_batch = static_cast<uint32_t>(handle->context_size);
    context_params.n_threads = handle->threads;
    context_params.n_threads_batch = handle->threads;

    llama_context * context = llama_init_from_model(handle->model, context_params);
    if (context == nullptr) {
        throw_java(env, "llama.cpp could not create an inference context");
        return nullptr;
    }

    llama_batch_ext * batch = llama_batch_ext_init(context);
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
    if (batch == nullptr || sampler == nullptr) {
        release_generation(context, batch, sampler);
        throw_java(env, "llama.cpp could not allocate inference resources");
        return nullptr;
    }

    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    llama_batch_ext_clear(batch);
    for (int32_t i = 0; i < token_count; ++i) {
        if (!add_token(batch, prompt_tokens[static_cast<size_t>(i)], i, i == token_count - 1)) {
            release_generation(context, batch, sampler);
            throw_java(env, "llama.cpp could not prepare prompt tokens");
            return nullptr;
        }
    }

    std::string output;
    for (int32_t generated = 0; generated < max_tokens; ++generated) {
        if (llama_process(context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) {
            release_generation(context, batch, sampler);
            throw_java(env, "llama.cpp decode failed");
            return nullptr;
        }

        const llama_token next = llama_sampler_sample(sampler, context, -1);
        if (llama_vocab_is_eog(vocab, next)) break;

        std::vector<char> piece(256);
        int32_t piece_size = llama_token_to_piece(vocab, next, piece.data(),
            static_cast<int32_t>(piece.size()), 0, true);
        if (piece_size < 0) {
            piece.resize(static_cast<size_t>(-piece_size));
            piece_size = llama_token_to_piece(vocab, next, piece.data(),
                static_cast<int32_t>(piece.size()), 0, true);
        }
        if (piece_size < 0) {
            release_generation(context, batch, sampler);
            throw_java(env, "llama.cpp token decoding failed");
            return nullptr;
        }
        output.append(piece.data(), static_cast<size_t>(piece_size));

        llama_batch_ext_clear(batch);
        const llama_pos position = token_count + generated;
        if (!add_token(batch, next, position, true)) {
            release_generation(context, batch, sampler);
            throw_java(env, "llama.cpp could not prepare generated token");
            return nullptr;
        }
    }

    release_generation(context, batch, sampler);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mana_os_core_JniManaLlamaCppRuntime_nativeReleaseModel(
    JNIEnv * env, jobject /* thiz */, jlong raw_handle
) {
    auto * handle = reinterpret_cast<ModelHandle *>(static_cast<intptr_t>(raw_handle));
    if (handle == nullptr) return;
    if (handle->model != nullptr) llama_model_free(handle->model);
    delete handle;
}
