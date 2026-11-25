package com.mana.launcher

import com.mana.os.core.BootPhase
import com.mana.os.core.DeviceFacts
import com.mana.os.core.ManaSystemContext
import com.mana.os.core.ManaSystemCore
import com.mana.os.core.ManaSystemModule
import com.mana.os.core.ManaSystemStatusSnapshot
import com.mana.os.core.ModuleCriticality
import com.mana.os.core.ModuleFailurePolicy
import com.mana.os.core.SystemModuleDescriptor

/**
 * App-process supervisor adapter. It reports only capabilities owned by this APK;
 * it does not claim to supervise Android init, the Linux kernel, SELinux or privileged services.
 */
class ManaAndroidRuntimeSupervisor(
    device: DeviceFacts,
    private val probes: Map<String, () -> Boolean>
) {
    private val descriptors = listOf(
        SystemModuleDescriptor(
            "app.storage", "com.mana.launcher.storage", emptySet(),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED,
            "app-private filesDir; no shared-storage authority"
        ),
        SystemModuleDescriptor(
            "app.memory", "com.mana.launcher.memory", setOf("app.storage"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED,
            "encrypted app-private MANA memory"
        ),
        SystemModuleDescriptor(
            "app.brain", "com.mana.launcher.brain", setOf("app.memory"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED,
            "in-process MANA Brain bridge; local runtime first"
        ),
        SystemModuleDescriptor(
            "app.launcher", "com.mana.launcher.home", setOf("app.brain"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED,
            "MANA launcher activity UI"
        )
    )

    private val core = ManaSystemCore(device)

    init {
        descriptors.forEach { descriptor ->
            core.register(object : ManaSystemModule {
                override val id: String = descriptor.id
                override val dependencies: Set<String> = descriptor.dependencies

                override fun start(context: ManaSystemContext) {
                    val available = probes[id]?.invoke() == true
                    check(available) { "Capability probe failed for $id" }
                }
            })
        }
    }

    fun start(): ManaSystemStatusSnapshot {
        check(core.phase == BootPhase.COLD) { "MANA Android runtime already started." }
        core.boot()
        return snapshot()
    }

    fun snapshot(): ManaSystemStatusSnapshot = core.statusSnapshot(descriptors)
}
