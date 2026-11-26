cmake_minimum_required(VERSION 3.22.1)
project(mana_llama_jni LANGUAGES C CXX)

include(FetchContent)

# Reproducible upstream dependency. Update this tag only together with JNI API review.
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_BLAS OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG v1.1.0
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(llama_cpp)

add_library(mana_llama_jni SHARED mana_llama_jni.cpp)
target_compile_features(mana_llama_jni PRIVATE cxx_std_17)
target_compile_options(mana_llama_jni PRIVATE -Wall -Wextra -Werror=return-type)
target_link_libraries(mana_llama_jni PRIVATE llama)

find_library(android-log-lib log)
if(android-log-lib)
    target_link_libraries(mana_llama_jni PRIVATE ${android-log-lib})
endif()
