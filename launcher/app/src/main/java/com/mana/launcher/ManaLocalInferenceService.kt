package com.mana.launcher

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import com.mana.brain.ILocalInferenceCallback
import com.mana.brain.ILocalInferenceService
import java.io.File

/**
 * Hosts the actual LiteRT-LM engine in the companion app process.
 * Only the installed MANA Assistant package may invoke this exported Binder endpoint.
 */
class ManaLocalInferenceService : Service() {
    private val runtime by lazy { LocalModelRuntime(File(cacheDir, "mana-system-litert-cache")) }
    private val loadLock = Any()
    private val pendingLoads = mutableListOf<(Result<Unit>) -> Unit>()
    private var loading = false

    private val binder = object : ILocalInferenceService.Stub() {
        override fun isModelReady(): Boolean {
            enforceAssistantCaller()
            return modelFile().isFile
        }

        override fun generate(requestId: String?, prompt: String?, callback: ILocalInferenceCallback?) {
            enforceAssistantCaller()
            if (callback == null) return
            if (requestId.isNullOrBlank() || prompt.isNullOrBlank() || prompt.length > MAX_PROMPT_CHARS) {
                safeComplete(callback, requestId, null, ERROR_INVALID_REQUEST, "Invalid local inference request.")
                return
            }
            ensureLoaded { loaded ->
                loaded.fold(
                    onSuccess = {
                        val output = StringBuilder()
                        runtime.generate(
                            prompt,
                            onChunk = { chunk -> output.append(chunk) },
                            callback = { result ->
                                result.fold(
                                    onSuccess = {
                                        val text = output.toString()
                                        if (text.isBlank()) safeComplete(callback, requestId, null, ERROR_EMPTY_OUTPUT, "LiteRT-LM returned no text.")
                                        else safeComplete(callback, requestId, text, 0, null)
                                    },
                                    onFailure = { error ->
                                        safeComplete(callback, requestId, null, ERROR_INFERENCE, error.message ?: "LiteRT-LM inference failed.")
                                    }
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        safeComplete(callback, requestId, null, ERROR_MODEL_UNAVAILABLE, error.message ?: "Local model is unavailable.")
                    }
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun modelFile() = File(filesDir, "mana-models/selected-model.litertlm")

    private fun ensureLoaded(completion: (Result<Unit>) -> Unit) {
        if (runtime.isReady()) {
            completion(Result.success(Unit))
            return
        }
        var shouldStartLoad = false
        synchronized(loadLock) {
            if (runtime.isReady()) {
                completion(Result.success(Unit))
                return
            }
            pendingLoads.add(completion)
            if (!loading) {
                loading = true
                shouldStartLoad = true
            }
        }
        if (!shouldStartLoad) return
        val model = modelFile()
        if (!model.isFile || !model.canRead()) {
            finishLoad(Result.failure(IllegalStateException("Import a .litertlm model in MANA Genesis first.")))
            return
        }
        runtime.load(model) { result ->
            finishLoad(result.map { Unit })
        }
    }

    private fun finishLoad(result: Result<Unit>) {
        val callbacks: List<(Result<Unit>) -> Unit>
        synchronized(loadLock) {
            loading = false
            callbacks = pendingLoads.toList()
            pendingLoads.clear()
        }
        callbacks.forEach { it(result) }
    }

    private fun enforceAssistantCaller() {
        val packages = packageManager.getPackagesForUid(Binder.getCallingUid()) ?: emptyArray()
        if (!packages.contains(ALLOWED_CALLER)) {
            throw SecurityException("Only MANA Assistant may call the local inference provider.")
        }
    }

    private fun safeComplete(
        callback: ILocalInferenceCallback,
        requestId: String?,
        text: String?,
        code: Int,
        message: String?
    ) {
        try { callback.onComplete(requestId, text, code, message) }
        catch (_: RemoteException) { /* Request owner disconnected. */ }
    }

    override fun onDestroy() {
        if (::runtime.isInitialized) runtime.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val ALLOWED_CALLER = "com.mana.assistant"
        private const val MAX_PROMPT_CHARS = 32768
        private const val ERROR_INVALID_REQUEST = 1100
        private const val ERROR_MODEL_UNAVAILABLE = 1101
        private const val ERROR_INFERENCE = 1102
        private const val ERROR_EMPTY_OUTPUT = 1103
    }
}
