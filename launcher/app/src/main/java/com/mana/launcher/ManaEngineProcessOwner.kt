package com.mana.launcher

import android.content.Context
import com.mana.os.core.ManaMemoryService
import com.mana.os.core.EncryptedFileManaMemoryStore
import com.mana.os.core.EncryptedFileManaDiaryStore

/**
 * Process-level owner for the service-created MANA Engine and its shared dependencies.
 * The foreground service and signature-protected Binder bridge share one reference-counted owner.
 */
object ManaEngineProcessOwner {
    private val lock = Any()
    @Volatile private var engine: ManaEngineAndroidRuntime? = null
    @Volatile private var brain: ManaBrainBridge? = null
    @Volatile private var inference: LocalModelRuntime? = null
    @Volatile private var memory: ManaMemoryService? = null
    @Volatile private var cloud: ManaCloudGateway? = null
    @Volatile private var yandex: ManaYandexAiStudioProvider? = null
    private var serviceOwners = 0
    private var modelLoadRequested = false

    fun prepare(
        brainBridge: ManaBrainBridge,
        localInference: LocalModelRuntime,
        memoryService: ManaMemoryService
    ) {
        synchronized(lock) {
            check(engine == null) { "MANA Engine is already running" }
            check(brain == null && inference == null && memory == null) {
                "MANA Engine dependencies are already prepared; refusing to replace live process resources"
            }
            brain = brainBridge
            inference = localInference
            memory = memoryService
        }
    }

    /**
     * Reference-counted ownership shared by the foreground UI service and the
     * signature-protected remote Binder endpoint. Resources are created once per process.
     */
    fun acquire(context: Context, keyProvider: ManaAndroidKeyProvider) {
        synchronized(lock) {
            if (serviceOwners == 0) {
                try {
                    initializeDependenciesFromService(context, keyProvider)
                    initializeFromService(context, keyProvider)
                } catch (error: Exception) {
                    shutdownResourcesLocked()
                    throw error
                }
            }
            serviceOwners += 1
        }
    }

    fun release() {
        synchronized(lock) {
            if (serviceOwners == 0) return
            serviceOwners -= 1
            if (serviceOwners == 0) shutdownResourcesLocked()
        }
    }

    /**
     * Load a previously saved/selected model. Catalog-managed models are hash-verified; model
     * initialization run off the main thread; this method never downloads or accepts a license.
     */
    fun ensureSavedModelLoaded(
        context: Context,
        callback: ((Result<String>) -> Unit)? = null
    ) {
        val app = context.applicationContext
        val local = synchronized(lock) {
            val runtime = inference ?: return
            if (runtime.isReady() || modelLoadRequested) return
            modelLoadRequested = true
            runtime
        }
        Thread({
            try {
                val catalog = ManaModelCatalog(app)
                val model = catalog.verifiedSavedModel()
                    ?: java.io.File(app.filesDir, "mana-models/selected-model.litertlm")
                        .takeIf { it.isFile && it.canRead() }
                if (model == null) {
                    synchronized(lock) { modelLoadRequested = false }
                    return@Thread
                }
                local.load(model) { result ->
                    synchronized(lock) { modelLoadRequested = false }
                    callback?.invoke(result)
                }
            } catch (error: Exception) {
                synchronized(lock) { modelLoadRequested = false }
                callback?.invoke(Result.failure(error))
            }
        }, "MANA-Model-Bootstrap").apply {
            isDaemon = true
            start()
        }
    }

    fun initializeDependenciesFromService(context: Context, keyProvider: ManaAndroidKeyProvider) {
        synchronized(lock) {
            if (brain != null && inference != null && memory != null) return
            val app = context.applicationContext
            val local = LocalModelRuntime(java.io.File(app.cacheDir, "mana-litert-cache"))
            val cloudGateway = ManaCloudGateway(app, keyProvider)
            val yandexProvider = ManaYandexAiStudioProvider(app)
            val brainBridge = ManaBrainBridge(local, listOf(cloudGateway, yandexProvider))
            val memoryFile = java.io.File(app.filesDir, "mana/memory.bin")
            val memoryService = try {
                ManaMemoryService(
                    EncryptedFileManaMemoryStore(
                        EncryptedFileManaDiaryStore(memoryFile, keyProvider, "mana.memory.v1")
                    )
                )
            } catch (error: Exception) {
                val recoveryCopy = runCatching {
                    ManaEncryptedDataRecovery.preserve(
                        memoryFile,
                        java.io.File(app.filesDir, "mana/recovery"),
                        "memory"
                    )
                }.getOrNull()
                val detail = recoveryCopy?.name ?: "recovery-copy-unavailable"
                throw IllegalStateException(
                    "MANA encrypted memory could not be opened. Original data was not modified; recovery: $detail",
                    error
                )
            }
            inference = local
            cloud = cloudGateway
            yandex = yandexProvider
            memory = memoryService
            brain = brainBridge
        }
    }

    fun initializeFromService(context: Context, keyProvider: ManaAndroidKeyProvider): ManaEngineAndroidRuntime {
        synchronized(lock) {
            engine?.let { return it }
            val brainBridge = checkNotNull(brain) { "MANA Brain dependencies were not prepared" }
            val memoryService = checkNotNull(memory) { "MANA memory service was not prepared" }
            val runtime = ManaEngineAndroidRuntime(context.applicationContext, brainBridge, memoryService, keyProvider)
            val health = runtime.start()
            engine = runtime
            if (health.state != com.mana.os.core.ManaEngineState.READY) {
                android.util.Log.w("MANA", "Engine started with limited health: ${health.lastFailure ?: health.state.name}")
            }
            return runtime
        }
    }

    fun isPrepared(): Boolean = synchronized(lock) {
        brain != null && inference != null && memory != null
    }
    fun isInstalled(): Boolean = engine != null
    fun engineRuntime(): ManaEngineAndroidRuntime? = engine
    fun brainBridge(): ManaBrainBridge? = brain
    fun localModelRuntime(): LocalModelRuntime? = inference
    fun memoryService(): ManaMemoryService? = memory
    fun cloudGateway(): ManaCloudGateway? = cloud
    fun yandexProvider(): ManaYandexAiStudioProvider? = yandex
    fun isRunning(): Boolean = engine?.health()?.state == com.mana.os.core.ManaEngineState.READY

    fun shutdown() {
        synchronized(lock) {
            serviceOwners = 0
            shutdownResourcesLocked()
        }
    }

    private fun shutdownResourcesLocked() {
        runCatching { engine?.close() }
        engine = null
        brain?.shutdown()
        brain = null
        inference?.shutdown()
        inference = null
        memory = null
        cloud = null
        yandex = null
        modelLoadRequested = false
    }
}
