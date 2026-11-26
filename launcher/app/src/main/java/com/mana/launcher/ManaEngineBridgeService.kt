package com.mana.launcher

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import com.mana.brain.IManaEngineBackend
import com.mana.brain.IManaEngineBackendCallback
import com.mana.brain.ManaBrainRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Signature-protected cross-process endpoint for the platform MANA Brain service.
 * The process owner remains the only owner of inference, encrypted memory and Microworld.
 */
class ManaEngineBridgeService : Service() {
    private val work = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var initialized = false

    private val binder = object : IManaEngineBackend.Stub() {
        override fun getProtocolVersion(): Int {
            enforceAuthorizedCaller()
            return PROTOCOL_VERSION
        }

        override fun getEngineState(): String {
            enforceAuthorizedCaller()
            if (!initialized) return "UNAVAILABLE_ENGINE_NOT_INITIALIZED"
            if (!ManaEngineProcessOwner.isRunning()) return "ENGINE_DEGRADED"
            return if (ManaEngineProcessOwner.localModelRuntime()?.isReady() == true) {
                "READY"
            } else {
                "READY_NO_LOCAL_MODEL"
            }
        }

        override fun think(request: ManaBrainRequest?, callback: IManaEngineBackendCallback?) {
            enforceAuthorizedCaller()
            if (callback == null) return
            if (request == null) {
                callback.onError("INVALID_REQUEST", "MANA Brain request is missing.")
                return
            }
            val policyError = validate(request)
            if (policyError != null) {
                callback.onError("POLICY_DENIED", policyError)
                return
            }
            if (!initialized || !ManaEngineProcessOwner.isRunning()) {
                callback.onError("ENGINE_NOT_READY", "The local MANA Engine is not ready.")
                return
            }
            work.launch {
                try {
                    val runtime = checkNotNull(ManaEngineProcessOwner.engineRuntime())
                    val result = runtime.think(
                        text = request.text,
                        allowCloud = request.allowCloud,
                        sensitive = request.sensitive,
                        preferredCloudProvider = request.preferredCloudProvider,
                        includePersonalMemory = request.includePersonalMemory,
                        memoryQuery = request.memoryQuery?.takeIf { it.isNotBlank() } ?: request.text,
                        allowPersonalMemoryToCloud = request.allowPersonalMemoryToCloud
                    )
                    callback.onResult(result.text, result.provider, result.local, result.memoryItemsUsed)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: RemoteException) {
                    android.util.Log.w(TAG, "MANA Brain client disconnected before callback", error)
                } catch (error: Exception) {
                    runCatching {
                        callback.onError("ENGINE_ERROR", error.message ?: error.javaClass.simpleName)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            ManaEngineProcessOwner.acquire(this, ManaAndroidKeyProvider())
            initialized = true
            ManaEngineProcessOwner.ensureSavedModelLoaded(this) { result ->
                result.onFailure { error ->
                    android.util.Log.w(TAG, "Verified local model is not ready", error)
                }
            }
        } catch (error: Exception) {
            initialized = false
            android.util.Log.e(TAG, "Unable to acquire MANA Engine for platform bridge", error)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action != ACTION_BIND) return null
        return binder
    }

    override fun onDestroy() {
        work.cancel()
        if (initialized) ManaEngineProcessOwner.release()
        initialized = false
        super.onDestroy()
    }

    private fun enforceAuthorizedCaller() {
        if (checkCallingPermission(BIND_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Caller lacks $BIND_PERMISSION")
        }
    }

    private fun validate(request: ManaBrainRequest): String? {
        val text = request.text ?: return "A non-empty prompt is required."
        if (text.isBlank()) return "A non-empty prompt is required."
        if (text.length > MAX_PROMPT_CHARS) return "Prompt exceeds the 32768-character limit."
        if (request.sensitive && request.allowCloud) return "Sensitive requests must remain local."
        val provider = request.preferredCloudProvider?.trim().orEmpty()
        if (!request.allowCloud && provider.isNotEmpty()) return "A cloud provider cannot be selected for a local-only request."
        if (provider.isNotEmpty() && provider.lowercase() !in SUPPORTED_PROVIDERS) {
            return "The requested cloud provider is not supported."
        }
        if (request.allowPersonalMemoryToCloud && (!request.allowCloud || !request.includePersonalMemory)) {
            return "Cloud memory export requires both cloud and personal-memory consent."
        }
        return null
    }

    companion object {
        const val ACTION_BIND = "com.mana.action.BIND_ENGINE_BACKEND"
        const val BIND_PERMISSION = "com.mana.permission.BIND_BRAIN"
        private const val PROTOCOL_VERSION = 1
        private const val MAX_PROMPT_CHARS = 32_768
        private const val TAG = "MANA-EngineBridge"
        private val SUPPORTED_PROVIDERS = setOf(
            "openai", "anthropic", "google", "gemini", "xai", "grok", "yandex", "vk"
        )
    }
}
