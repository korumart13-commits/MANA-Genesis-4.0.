package com.mana.os.core

import java.time.Instant

data class ManaHydraCall(
    val toolId: String,
    val principal: String,
    val input: String,
    val sensitivity: DataSensitivity = DataSensitivity.PERSONAL
)

data class ManaHydraResult(val toolId: String, val output: String, val completedAt: Instant)

interface ManaHydraTool {
    val id: String
    val capability: SecurityCapability
    fun execute(input: String): String
}

/**
 * Permission-gated capability registry. Every invocation is checked against explicit
 * grants; registration alone never grants permission. This is a host-side policy layer,
 * not a replacement for Android permissions, Binder identity checks, or SELinux.
 */
class ManaHydraRegistry(private val security: ManaSecurityService) {
    private val tools = linkedMapOf<String, ManaHydraTool>()

    @Synchronized
    fun register(tool: ManaHydraTool) {
        require(tool.id.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
        require(tool.id !in tools) { "Duplicate Hydra tool: ${tool.id}" }
        tools[tool.id] = tool
    }

    fun invoke(call: ManaHydraCall, now: Instant = Instant.now()): ManaHydraResult {
        val tool = synchronized(this) { tools[call.toolId] } ?: error("Unknown Hydra tool: ${call.toolId}")
        val capability = tool.capability.copy(principal = call.principal)
        val decision = security.authorize(capability, call.sensitivity, now)
        check(decision.effect == SecurityEffect.ALLOW) { "Hydra invocation denied: ${decision.reason}" }
        val output = tool.execute(call.input)
        return ManaHydraResult(tool.id, output, now)
    }

    @Synchronized
    fun registeredTools(): List<String> = tools.keys.toList()
}

data class ShogunTask(val id: String, val objective: String, val calls: List<ManaHydraCall>) {
    init {
        require(id.isNotBlank() && objective.isNotBlank())
    }
}

data class ShogunTaskReport(
    val taskId: String,
    val results: List<ManaHydraResult>,
    val completed: Boolean,
    val failure: String? = null
)

/**
 * Deterministic Shogun execution coordinator. Planning is supplied as typed calls;
 * no generated shell/code is executed. Each Hydra call independently passes authorization.
 */
class ManaShogun(private val hydra: ManaHydraRegistry) {
    fun execute(task: ShogunTask): ShogunTaskReport {
        val results = mutableListOf<ManaHydraResult>()
        for (call in task.calls) {
            try {
                results += hydra.invoke(call)
            } catch (failure: Exception) {
                return ShogunTaskReport(
                    taskId = task.id,
                    results = results.toList(),
                    completed = false,
                    failure = failure.message ?: failure::class.java.simpleName
                )
            }
        }
        return ShogunTaskReport(task.id, results.toList(), completed = true)
    }
}
