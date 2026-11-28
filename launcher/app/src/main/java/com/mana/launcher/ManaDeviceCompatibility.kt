package com.mana.launcher

/**
 * Separates universal Android app eligibility from the qualified RMX5566 device profile.
 * A profile mismatch must never disable the portable MANA app; it only keeps device-specific
 * integrations and firmware work gated until the exact hardware identity is confirmed.
 */
data class ManaDeviceIdentity(
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val fingerprint: String,
    val apiLevel: Int
)

data class ManaDeviceCompatibility(
    val canRunUniversalSetup: Boolean,
    val matchesRmx5566TestProfile: Boolean,
    val profileMismatchReasons: List<String>
)

object ManaDeviceCompatibilityPolicy {
    const val MIN_ANDROID_API = 29

    fun evaluate(identity: ManaDeviceIdentity): ManaDeviceCompatibility {
        val mismatches = buildList {
            if (!identity.manufacturer.equals("realme", ignoreCase = true)) add("manufacturer")
            if (!identity.model.contains("RMX5566", ignoreCase = true)) add("model")
            if (!identity.device.equals("RE645F", ignoreCase = true)) add("device")
            if (!identity.product.equals("RE645F", ignoreCase = true)) add("product")
            if (identity.apiLevel != 36) add("api_level")
            if (!identity.fingerprint.startsWith("realme/RMX5566RU/RE645F:16/", ignoreCase = true)) {
                add("fingerprint")
            }
        }
        return ManaDeviceCompatibility(
            canRunUniversalSetup = identity.apiLevel >= MIN_ANDROID_API,
            matchesRmx5566TestProfile = mismatches.isEmpty(),
            profileMismatchReasons = mismatches
        )
    }
}
