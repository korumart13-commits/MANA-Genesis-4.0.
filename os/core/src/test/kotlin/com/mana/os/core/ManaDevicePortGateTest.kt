package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManaDevicePortGateTest {
    private val requirement = DevicePortRequirement(
        product = "RMX5566RU", device = "RE645F", androidApi = 36,
        abi = "arm64-v8a", kernelReleasePrefix = "5.15.197"
    )
    private val matching = DevicePortFacts(
        product = "RMX5566RU", device = "RE645F", androidApi = 36,
        abi = "arm64-v8a", kernelRelease = "5.15.197-android13",
        bootloaderUnlocked = true, dynamicPartitions = true, virtualAb = true,
        verifiedBootState = "green"
    )

    @Test fun acceptsOnlyFullyMatchingAndKnownBootChainFacts() {
        val result = ManaDevicePortGate.evaluate(requirement, matching)
        assertEquals(DevicePortStatus.MATCH, result.status)
        assertTrue(result.blockers.isEmpty())
        assertTrue(result.unknowns.isEmpty())
    }

    @Test fun rejectsDifferentDeviceFamily() {
        val result = ManaDevicePortGate.evaluate(requirement, matching.copy(
            product = "RMX5253", device = "unknown"
        ))
        assertEquals(DevicePortStatus.MISMATCH, result.status)
        assertTrue(result.blockers.any { it.contains("Product mismatch") })
        assertTrue(result.blockers.any { it.contains("Device mismatch") })
    }

    @Test fun lockedBootloaderBlocksFlashing() {
        val result = ManaDevicePortGate.evaluate(requirement, matching.copy(bootloaderUnlocked = false))
        assertEquals(DevicePortStatus.MISMATCH, result.status)
        assertTrue(result.blockers.any { it.contains("Bootloader is locked") })
    }

    @Test fun missingBootChainFactsRemainUnknownNotAssumedSafe() {
        val result = ManaDevicePortGate.evaluate(requirement, matching.copy(
            bootloaderUnlocked = null, dynamicPartitions = null, virtualAb = null,
            verifiedBootState = null
        ))
        assertEquals(DevicePortStatus.UNKNOWN, result.status)
        assertEquals(4, result.unknowns.size)
    }

    @Test fun rejectsWrongKernelEvenWhenOtherFactsMatch() {
        val result = ManaDevicePortGate.evaluate(requirement, matching.copy(kernelRelease = "4.19.157"))
        assertEquals(DevicePortStatus.MISMATCH, result.status)
        assertTrue(result.blockers.any { it.contains("Kernel release mismatch") })
    }
}
