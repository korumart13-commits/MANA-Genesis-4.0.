package com.mana.launcher

import android.content.Context
import com.mana.os.core.ManaMemoryService

/**
 * Process-level owner for the service-created MANA Engine and its shared dependencies.
 * Only ManaEngineService is allowed to construct/start the Engine runtime.
 */
object ManaEngineProcessOwner {
    private val lock = Any()
    @Volatile private var engine: ManaEngineAndroidRuntime? = null
    @Volatile private var brain: ManaBrainBridge? = null
    @Volatile private var inference: LocalModelRuntime? = null
    @Volatile private var memory: ManaMemoryService? = null

    fun prepare(
        brainBridge: ManaBrainBridge,
        localInference: LocalModelRuntime,
        memoryService: ManaMemoryService
    ) {
        synchronized(lock) {
            check(engine == null) { "MANA Engine is already running" }
            brain = brainBridge
            inference = localInference
            memory = memoryService
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

    fun isInstalled(): Boolean = engine != null
    fun engineRuntime(): ManaEngineAndroidRuntime? = engine
    fun brainBridge(): ManaBrainBridge? = brain
    fun localModelRuntime(): LocalModelRuntime? = inference
    fun memoryService(): ManaMemoryService? = memory
    fun isRunning(): Boolean = engine?.health()?.state == com.mana.os.core.ManaEngineState.READY

    fun shutdown() {
        synchronized(lock) {
            runCatching { engine?.close() }
            engine = null
            brain?.shutdown()
            brain = null
            inference?.shutdown()
            inference = null
            memory = null
        }
    }
}
