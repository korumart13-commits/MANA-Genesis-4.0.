package com.mana.launcher

/**
 * Owns the single MANA Engine instance for the lifetime of ManaEngineService,
 * rather than tying it to a launcher Activity recreation.
 */
object ManaEngineProcessOwner {
    private val lock = Any()
    @Volatile private var engine: ManaEngineAndroidRuntime? = null
    @Volatile private var brain: ManaBrainBridge? = null
    @Volatile private var inference: LocalInferenceRuntime? = null

    fun install(
        engineRuntime: ManaEngineAndroidRuntime,
        brainBridge: ManaBrainBridge,
        localInference: LocalInferenceRuntime
    ) {
        synchronized(lock) {
            check(engine == null) { "MANA Engine process owner is already initialized" }
            engine = engineRuntime
            brain = brainBridge
            inference = localInference
        }
    }

    fun isInstalled(): Boolean = engine != null

    fun isRunning(): Boolean = engine?.health()?.state == com.mana.os.core.ManaEngineState.READY

    fun shutdown() {
        synchronized(lock) {
            runCatching { engine?.close() }
            engine = null
            brain?.shutdown()
            brain = null
            inference?.shutdown()
            inference = null
        }
    }
}
