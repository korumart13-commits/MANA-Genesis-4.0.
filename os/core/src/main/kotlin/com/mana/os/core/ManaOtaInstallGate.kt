package com.mana.os.core

/**
 * Fail-closed preflight for a platform-owned OTA installer.
 *
 * This gate decides whether a verified payload may be handed to the device's
 * supported update engine. It does not write partitions, change boot slots,
 * bypass AVB, unlock the bootloader, or reboot the device.
 */
data class ManaOtaPreflight(
    val exactDeviceMatch: Boolean,
    val exactBuildFamilyMatch: Boolean,
    val trustedSignatureVerified: Boolean,
    val payloadHashVerified: Boolean,
    val rollbackIndexAccepted: Boolean,
    val supportedOtaBackendAvailable: Boolean,
    val recoveryPathVerified: Boolean,
    val powerConnected: Boolean,
    val batteryPercent: Int,
    val temperatureCelsius: Double,
    val freeBytes: Long,
    val requiredFreeBytes: Long,
    val storageHealthy: Boolean,
    val ownerAuthorizationGranted: Boolean
)

data class ManaOtaDecision(
    val allowed: Boolean,
    val blockers: List<String>
)

object ManaOtaInstallGate {
    fun evaluate(state: ManaOtaPreflight): ManaOtaDecision {
        val blockers = buildList {
            if (!state.exactDeviceMatch) add("DEVICE_MISMATCH")
            if (!state.exactBuildFamilyMatch) add("BUILD_FAMILY_MISMATCH")
            if (!state.trustedSignatureVerified) add("SIGNATURE_NOT_VERIFIED")
            if (!state.payloadHashVerified) add("PAYLOAD_HASH_NOT_VERIFIED")
            if (!state.rollbackIndexAccepted) add("ROLLBACK_INDEX_REJECTED")
            if (!state.supportedOtaBackendAvailable) add("SUPPORTED_OTA_BACKEND_UNAVAILABLE")
            if (!state.recoveryPathVerified) add("RECOVERY_PATH_NOT_VERIFIED")
            if (!state.powerConnected && state.batteryPercent < MIN_BATTERY_WITHOUT_POWER) add("INSUFFICIENT_BATTERY")
            if (state.batteryPercent !in 0..100) add("INVALID_BATTERY_READING")
            if (!state.temperatureCelsius.isFinite() || state.temperatureCelsius > MAX_TEMPERATURE_C) add("TEMPERATURE_UNSAFE")
            if (state.freeBytes < state.requiredFreeBytes || state.requiredFreeBytes <= 0) add("INSUFFICIENT_STORAGE")
            if (!state.storageHealthy) add("STORAGE_HEALTH_UNCONFIRMED")
            if (!state.ownerAuthorizationGranted) add("OWNER_AUTHORIZATION_REQUIRED")
        }
        return ManaOtaDecision(blockers.isEmpty(), blockers)
    }

    private const val MIN_BATTERY_WITHOUT_POWER = 80
    private const val MAX_TEMPERATURE_C = 42.0
}
