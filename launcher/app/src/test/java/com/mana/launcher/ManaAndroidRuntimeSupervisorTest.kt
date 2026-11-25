package com.mana.launcher

import com.mana.os.core.DeviceFacts
import com.mana.os.core.SystemReadiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManaAndroidRuntimeSupervisorTest {
    private val device = DeviceFacts(
        product = "test-product",
        device = "test-device",
        androidApi = 36,
        kernelRelease = "test-kernel",
        abi = "arm64-v8a"
    )

    @Test fun reportsReadyWhenAllAppCapabilitiesAreAvailable() {
        val supervisor = ManaAndroidRuntimeSupervisor(
            device,
            mapOf(
                "app.storage" to { true },
                "app.memory" to { true },
                "app.brain" to { true },
                "app.launcher" to { true }
            )
        )

        val snapshot = supervisor.start()

        assertEquals(SystemReadiness.READY, snapshot.readiness)
        assertTrue(snapshot.modules.all { it.state.name == "RUNNING" })
    }

    @Test fun blocksReadinessWhenEncryptedMemoryIsUnavailable() {
        val supervisor = ManaAndroidRuntimeSupervisor(
            device,
            mapOf(
                "app.storage" to { true },
                "app.memory" to { false },
                "app.brain" to { true },
                "app.launcher" to { true }
            )
        )

        val snapshot = supervisor.start()

        assertEquals(SystemReadiness.BLOCKED, snapshot.readiness)
        assertTrue(snapshot.requiredFailures.any { it.startsWith("app.memory:") })
    }
}
