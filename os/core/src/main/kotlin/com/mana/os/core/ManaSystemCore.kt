package com.mana.os.core

import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class BootPhase { COLD, HARDWARE_DISCOVERY, SECURITY_INIT, CORE_SERVICES, USER_SERVICES, READY, DEGRADED, SHUTTING_DOWN, STOPPED }
enum class ModuleState { REGISTERED, STARTING, RUNNING, DEGRADED, STOPPING, STOPPED, FAILED }

data class DeviceFacts(
    val product: String,
    val device: String,
    val androidApi: Int,
    val kernelRelease: String,
    val abi: String,
    val activeSlot: String? = null
) {
    init {
        require(product.isNotBlank() && device.isNotBlank())
        require(androidApi >= 1 && kernelRelease.isNotBlank() && abi.isNotBlank())
    }
}

data class ModuleHealth(
    val id: String,
    val state: ModuleState,
    val detail: String? = null,
    val changedAt: Instant = Instant.now()
)

interface ManaSystemModule {
    val id: String
    val dependencies: Set<String> get() = emptySet()
    fun start(context: ManaSystemContext)
    fun stop(context: ManaSystemContext) {}
}

data class ManaSystemContext(
    val device: DeviceFacts,
    val services: MutableMap<String, Any> = mutableMapOf()
)

/**
 * Userspace system supervisor for MANA OS. This is not a Linux kernel replacement:
 * it coordinates MANA system services above the device's Linux/Android kernel.
 */
class ManaSystemCore(private val device: DeviceFacts) {
    private val lock = ReentrantLock()
    private val modules = linkedMapOf<String, ManaSystemModule>()
    private val health = linkedMapOf<String, ModuleHealth>()
    private val context = ManaSystemContext(device)
    private val startedOrder = mutableListOf<String>()

    @Volatile var phase: BootPhase = BootPhase.COLD
        private set

    fun register(module: ManaSystemModule) = lock.withLock {
        check(phase == BootPhase.COLD) { "Modules can only be registered before boot." }
        require(module.id.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
        require(module.id !in modules) { "Duplicate module: ${module.id}" }
        modules[module.id] = module
        health[module.id] = ModuleHealth(module.id, ModuleState.REGISTERED)
    }

    fun boot(): List<ModuleHealth> = lock.withLock {
        check(phase == BootPhase.COLD) { "Boot already started." }
        phase = BootPhase.HARDWARE_DISCOVERY
        val startupOrder = try {
            validateDependencies()
            dependencyOrder()
        } catch (configurationError: Exception) {
            phase = BootPhase.DEGRADED
            throw configurationError
        }
        try {
            phase = BootPhase.SECURITY_INIT
            // Reserved integration phase. Production wiring must register and verify the
            // platform.security module; this host-JVM supervisor does not enforce OS policy.
            phase = BootPhase.CORE_SERVICES
            for (id in startupOrder) {
                val module = modules.getValue(id)
                health[id] = ModuleHealth(id, ModuleState.STARTING)
                try {
                    module.start(context)
                    health[id] = ModuleHealth(id, ModuleState.RUNNING)
                    startedOrder += id
                } catch (error: Exception) {
                    health[id] = ModuleHealth(id, ModuleState.FAILED, error.message ?: error::class.java.simpleName)
                    throw error
                }
            }
            phase = BootPhase.USER_SERVICES
            phase = BootPhase.READY
        } catch (error: Throwable) {
            // Roll back only modules that actually started, in reverse dependency order.
            startedOrder.asReversed().forEach { id ->
                val module = modules.getValue(id)
                health[id] = ModuleHealth(id, ModuleState.STOPPING)
                try { module.stop(context) } catch (_: Exception) { /* Continue best-effort rollback. */ }
                health[id] = ModuleHealth(id, ModuleState.STOPPED)
            }
            startedOrder.clear()
            phase = BootPhase.DEGRADED
            if (error !is Exception) throw error
        }
        health.values.toList()
    }

    fun stop() = lock.withLock {
        if (phase == BootPhase.COLD || phase == BootPhase.SHUTTING_DOWN) return
        phase = BootPhase.SHUTTING_DOWN
        startedOrder.asReversed().forEach { id ->
            val module = modules.getValue(id)
            health[id] = ModuleHealth(id, ModuleState.STOPPING)
            try { module.stop(context) } catch (_: Exception) { /* Continue stopping remaining modules. */ }
            health[id] = ModuleHealth(id, ModuleState.STOPPED)
        }
        startedOrder.clear()
        phase = BootPhase.STOPPED
    }

    fun healthSnapshot(): List<ModuleHealth> = lock.withLock { health.values.toList() }

    private fun validateDependencies() {
        modules.values.forEach { module ->
            val missing = module.dependencies - modules.keys
            require(missing.isEmpty()) { "Module ${module.id} has missing dependencies: ${missing.joinToString()}" }
            require(module.id !in module.dependencies) { "Module ${module.id} cannot depend on itself." }
        }
    }

    private fun dependencyOrder(): List<String> {
        val result = mutableListOf<String>()
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            check(visiting.add(id)) { "Dependency cycle detected at $id" }
            modules.getValue(id).dependencies.sorted().forEach(::visit)
            visiting.remove(id)
            visited.add(id)
            result += id
        }
        modules.keys.forEach(::visit)
        return result
    }
}
