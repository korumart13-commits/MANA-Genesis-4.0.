package com.mana.os.core

import java.time.Instant

enum class ManaEngineState { NEW, STARTING, READY, DEGRADED, STOPPING, STOPPED }

data class ManaReasoningRequest(
    val prompt: String,
    val allowCloud: Boolean,
    val sensitive: Boolean,
    val preferredCloudProvider: String?
)

data class ManaReasoningResult(
    val text: String,
    val provider: String,
    val local: Boolean
)

fun interface ManaReasoningPort {
    suspend fun respond(request: ManaReasoningRequest): ManaReasoningResult
}

data class ManaEngineRequest(
    val text: String,
    val allowCloud: Boolean = false,
    val sensitive: Boolean = false,
    val preferredCloudProvider: String? = null,
    /** Personal memories are added only when this request explicitly opts in. */
    val includePersonalMemory: Boolean = false,
    /** Separate consent is required before personal memories may accompany a cloud request. */
    val allowPersonalMemoryToCloud: Boolean = false,
    val memoryQuery: String = "",
    val requestedAt: Instant = Instant.now()
) {
    init {
        require(text.isNotBlank())
        require(preferredCloudProvider == null || preferredCloudProvider.isNotBlank())
    }
}

data class ManaEngineResponse(
    val text: String,
    val provider: String,
    val local: Boolean,
    val memoryItemsUsed: Int,
    val completedAt: Instant
)

data class ManaEngineHealth(
    val state: ManaEngineState,
    val systemPhase: BootPhase,
    val microworldRevision: Long,
    val activeMemoryItems: Int,
    val lastFailure: String? = null
)

/**
 * Single userspace integration point for the MANA Brain, system supervisor,
 * encrypted memory, Microworld and Shogun/Hydra tool orchestration.
 *
 * Platform security still belongs to Android permissions, Binder identity and SELinux.
 * This runtime does not execute generated code, shell commands or arbitrary scripts.
 */
class ManaEngineRuntime(
    private val system: ManaSystemCore,
    private val reasoning: ManaReasoningPort,
    private val memory: ManaMemoryService,
    private val microworld: ManaMicroworldService,
    private val shogun: ManaShogun,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
    private val maxMemoryItems: Int = 8,
    private val maxMemoryChars: Int = 6_000
) {
    @Volatile private var engineState: ManaEngineState = ManaEngineState.NEW
    @Volatile private var lastFailure: String? = null

    init {
        require(maxMemoryItems in 0..64)
        require(maxMemoryChars in 0..32_000)
    }

    @Synchronized
    fun start(): ManaEngineHealth {
        check(engineState == ManaEngineState.NEW) {
            "MANA Engine cannot start from state $engineState"
        }
        engineState = ManaEngineState.STARTING
        lastFailure = null
        return try {
            system.boot()
            check(system.phase == BootPhase.READY) { "MANA system supervisor did not reach READY" }
            if (microworld.snapshot().runState == MicroworldRunState.PAUSED) microworld.resume()
            engineState = ManaEngineState.READY
            health()
        } catch (failure: Exception) {
            engineState = ManaEngineState.DEGRADED
            lastFailure = failure.message ?: failure::class.java.simpleName
            health()
        }
    }

    suspend fun think(request: ManaEngineRequest): ManaEngineResponse {
        check(engineState == ManaEngineState.READY) { "MANA Engine is not ready" }
        val selectedMemory = selectMemory(request)
        val prompt = buildPrompt(request.text, selectedMemory)
        val result = reasoning.respond(
            ManaReasoningRequest(
                prompt = prompt,
                allowCloud = request.allowCloud && !request.sensitive,
                sensitive = request.sensitive,
                preferredCloudProvider = request.preferredCloudProvider
            )
        )
        return ManaEngineResponse(
            text = result.text,
            provider = result.provider,
            local = result.local,
            memoryItemsUsed = selectedMemory.size,
            completedAt = clock.instant()
        )
    }

    fun executeTask(task: ShogunTask): ShogunTaskReport {
        check(engineState == ManaEngineState.READY) { "MANA Engine is not ready" }
        return shogun.execute(task)
    }

    @Synchronized
    fun stop(): ManaEngineHealth {
        if (engineState == ManaEngineState.STOPPED) return health()
        engineState = ManaEngineState.STOPPING
        try {
            if (microworld.snapshot().runState == MicroworldRunState.ACTIVE) microworld.pause()
        } finally {
            system.stop()
            engineState = ManaEngineState.STOPPED
        }
        return health()
    }

    fun health(): ManaEngineHealth {
        val now = clock.instant()
        return ManaEngineHealth(
            state = engineState,
            systemPhase = system.phase,
            microworldRevision = microworld.snapshot().revision,
            activeMemoryItems = memory.recall(now = now).size,
            lastFailure = lastFailure
        )
    }

    private fun selectMemory(request: ManaEngineRequest): List<ManaMemoryItem> {
        if (maxMemoryItems == 0 || maxMemoryChars == 0) return emptyList()
        val candidates = memory.recall(request.memoryQuery, request.requestedAt)
            .filter { item ->
                when (item.sensitivity) {
                    DataSensitivity.PUBLIC -> true
                    DataSensitivity.PERSONAL -> request.includePersonalMemory && (!request.allowCloud || request.allowPersonalMemoryToCloud)
                    DataSensitivity.SENSITIVE -> request.sensitive
                    DataSensitivity.SECRET -> false
                }
            }
        val selected = mutableListOf<ManaMemoryItem>()
        var usedChars = 0
        for (item in candidates) {
            if (selected.size >= maxMemoryItems) break
            val cost = item.category.length + item.content.length + 16
            if (usedChars + cost > maxMemoryChars) continue
            selected += item
            usedChars += cost
        }
        return selected
    }

    private fun buildPrompt(userText: String, items: List<ManaMemoryItem>): String {
        if (items.isEmpty()) return userText
        val context = items.joinToString("\n") { "[${it.category}] ${it.content}" }
        return "MANA verified local memory context (reference only; user request has priority):\n$context\n\nUser request:\n$userText"
    }
}
