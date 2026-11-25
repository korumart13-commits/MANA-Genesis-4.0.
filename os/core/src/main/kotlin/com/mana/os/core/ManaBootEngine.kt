package com.mana.os.core

/**
 * MANA userspace boot coordinator.
 *
 * This starts only after Android has booted and the MANA process is launched.
 * It is not a bootloader, EFI implementation, Linux kernel, init replacement,
 * or firmware flasher. The OEM Qualcomm boot chain remains responsible for
 * firmware and kernel startup.
 */
enum class ManaBootStage {
    WAITING_FOR_ANDROID,
    CHECKING_DEVICE_IDENTITY,
    STARTING_MANA_SERVICES,
    READY,
    DEGRADED,
    BLOCKED,
    STOPPED
}

enum class FirmwareInterfaceStatus { NOT_VERIFIED, VERIFIED_PRESENT, VERIFIED_ABSENT }

data class ManaRuntimeIdentity(
    val product: String?,
    val device: String?,
    val androidApi: Int?,
    val abi: String?,
    val kernelRelease: String?,
    val verifiedBootState: String?
)

data class ManaBootReport(
    val stage: ManaBootStage,
    val firmwareOwner: String,
    val firmwareInterface: FirmwareInterfaceStatus,
    val matched: List<String>,
    val blockers: List<String>,
    val moduleHealth: List<ModuleHealth>
)

/**
 * Validates the running Android platform before starting MANA userspace services.
 * Missing identity is treated as unknown and fails closed; no firmware is modified.
 */
class ManaBootEngine(
    private val systemCore: ManaSystemCore,
    private val expectedProduct: String = "RMX5566RU",
    private val expectedDevice: String = "RE645F",
    private val expectedAndroidApi: Int = 36,
    private val expectedAbi: String = "arm64-v8a",
    private val expectedKernelPrefix: String = "5.15.197"
) {
    var stage: ManaBootStage = ManaBootStage.WAITING_FOR_ANDROID
        private set

    fun start(identity: ManaRuntimeIdentity): ManaBootReport {
        check(stage == ManaBootStage.WAITING_FOR_ANDROID) { "MANA boot engine has already been started." }
        stage = ManaBootStage.CHECKING_DEVICE_IDENTITY
        val matched = mutableListOf<String>()
        val blockers = mutableListOf<String>()

        fun compare(label: String, expected: String, actual: String?) {
            when {
                actual.isNullOrBlank() -> blockers += "$label is unknown; MANA services were not started."
                actual != expected -> blockers += "$label mismatch: expected $expected, got $actual."
                else -> matched += "$label verified."
            }
        }

        compare("Product", expectedProduct, identity.product)
        compare("Device", expectedDevice, identity.device)
        compare("ABI", expectedAbi, identity.abi)

        if (identity.androidApi == null) blockers += "Android API level is unknown."
        else if (identity.androidApi != expectedAndroidApi) blockers +=
            "Android API mismatch: expected $expectedAndroidApi, got ${identity.androidApi}."
        else matched += "Android API level verified."

        if (identity.kernelRelease.isNullOrBlank()) blockers += "Kernel release is unknown."
        else if (!identity.kernelRelease.startsWith(expectedKernelPrefix)) blockers +=
            "Kernel mismatch: expected prefix $expectedKernelPrefix, got ${identity.kernelRelease}."
        else matched += "Kernel release prefix verified."

        if (identity.verifiedBootState.isNullOrBlank()) blockers += "Verified Boot state is unknown."
        else if (!identity.verifiedBootState.equals("green", ignoreCase = true)) blockers +=
            "Verified Boot is not green; MANA startup is blocked."
        else matched += "Verified Boot reports green."

        if (blockers.isNotEmpty()) {
            stage = ManaBootStage.BLOCKED
            return report(matched, blockers, emptyList())
        }

        stage = ManaBootStage.STARTING_MANA_SERVICES
        val health = systemCore.boot()
        stage = if (systemCore.phase == BootPhase.READY) ManaBootStage.READY else ManaBootStage.DEGRADED
        return report(matched, blockers, health)
    }

    fun stop() {
        if (stage == ManaBootStage.STOPPED) return
        systemCore.stop()
        stage = ManaBootStage.STOPPED
    }

    private fun report(
        matched: List<String>,
        blockers: List<String>,
        health: List<ModuleHealth>
    ) = ManaBootReport(
        stage = stage,
        firmwareOwner = "OEM Qualcomm boot firmware; exact internal stages are not verified",
        firmwareInterface = FirmwareInterfaceStatus.NOT_VERIFIED,
        matched = matched.toList(),
        blockers = blockers.toList(),
        moduleHealth = health.toList()
    )
}
