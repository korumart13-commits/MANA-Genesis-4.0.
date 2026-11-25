package com.mana.os.core

enum class DevicePortStatus { MATCH, MISMATCH, UNKNOWN }

data class DevicePortFacts(
    val product: String?,
    val device: String?,
    val androidApi: Int?,
    val abi: String?,
    val kernelRelease: String?,
    val bootloaderUnlocked: Boolean?,
    val dynamicPartitions: Boolean?,
    val virtualAb: Boolean?,
    val verifiedBootState: String?
)

data class DevicePortRequirement(
    val product: String,
    val device: String,
    val androidApi: Int,
    val abi: String,
    val kernelReleasePrefix: String
)

data class DevicePortCheck(
    val status: DevicePortStatus,
    val blockers: List<String>,
    val unknowns: List<String>,
    val matched: List<String>
)

/**
 * Read-only hardware/boot-chain compatibility preflight.
 * It never unlocks, flashes, changes slots, or modifies verified boot.
 */
object ManaDevicePortGate {
    fun evaluate(requirement: DevicePortRequirement, facts: DevicePortFacts): DevicePortCheck {
        val blockers = mutableListOf<String>()
        val unknowns = mutableListOf<String>()
        val matched = mutableListOf<String>()

        fun compare(label: String, expected: String, actual: String?) {
            when {
                actual == null || actual.isBlank() -> unknowns += "$label was not supplied."
                actual != expected -> blockers += "$label mismatch: expected $expected, got $actual."
                else -> matched += "$label matches."
            }
        }
        compare("Product", requirement.product, facts.product)
        compare("Device", requirement.device, facts.device)
        compare("ABI", requirement.abi, facts.abi)
        when {
            facts.androidApi == null -> unknowns += "Android API level was not supplied."
            facts.androidApi != requirement.androidApi -> blockers +=
                "Android API mismatch: expected ${requirement.androidApi}, got ${facts.androidApi}."
            else -> matched += "Android API level matches."
        }
        when {
            facts.kernelRelease == null -> unknowns += "Kernel release was not supplied."
            !facts.kernelRelease.startsWith(requirement.kernelReleasePrefix) -> blockers +=
                "Kernel release mismatch: required prefix ${requirement.kernelReleasePrefix}."
            else -> matched += "Kernel release prefix matches."
        }
        if (facts.bootloaderUnlocked == null) unknowns += "Bootloader unlock state is unknown."
        else if (!facts.bootloaderUnlocked) blockers += "Bootloader is locked; flashing is not permitted by this gate."
        else matched += "Bootloader reports unlocked."
        if (facts.dynamicPartitions == null) unknowns += "Dynamic-partition layout is unverified."
        else if (!facts.dynamicPartitions) blockers += "Expected dynamic partitions are absent."
        else matched += "Dynamic partitions are present."
        if (facts.virtualAb == null) unknowns += "Virtual A/B status is unverified."
        else if (!facts.virtualAb) blockers += "Expected Virtual A/B support is absent."
        else matched += "Virtual A/B support is present."
        if (facts.verifiedBootState.isNullOrBlank()) unknowns += "Verified Boot state is unknown."
        else if (!facts.verifiedBootState.equals("green", ignoreCase = true)) blockers +=
            "Verified Boot is not in the expected green state."
        else matched += "Verified Boot reports green."

        val status = when {
            blockers.isNotEmpty() -> DevicePortStatus.MISMATCH
            unknowns.isNotEmpty() -> DevicePortStatus.UNKNOWN
            else -> DevicePortStatus.MATCH
        }
        return DevicePortCheck(status, blockers, unknowns, matched)
    }
}
