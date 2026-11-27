package com.mana.launcher

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build

/**
 * Read-only probe for Android's managed-device OTA installation path.
 *
 * This class does not install updates, reboot, access block devices, or change
 * device-owner state. It only reports whether the public DPM entry point could
 * be considered by a separately reviewed installer flow.
 */
data class ManaOtaCapability(
    val apiLevel: Int,
    val deviceOwner: Boolean,
    val profileOwner: Boolean,
    val dpmInstallApiPresent: Boolean,
    val ordinaryAppCanInstallSystemOta: Boolean,
    val status: String,
    val explanation: String
)

object ManaOtaCapabilityProbe {
    fun inspect(context: Context): ManaOtaCapability {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
            as? DevicePolicyManager
        val deviceOwner = runCatching {
            dpm?.isDeviceOwnerApp(context.packageName) == true
        }.getOrDefault(false)
        val profileOwner = runCatching {
            dpm?.isProfileOwnerApp(context.packageName) == true
        }.getOrDefault(false)
        val apiPresent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val managedOwner = deviceOwner || profileOwner

        val status = when {
            !apiPresent -> "UNAVAILABLE_API_LEVEL"
            managedOwner -> "MANAGED_OWNER_PATH_REQUIRES_OTA_VALIDATION"
            else -> "BLOCKED_NO_DEVICE_OWNER"
        }
        val explanation = when {
            !apiPresent ->
                "DevicePolicyManager.installSystemUpdate requires Android 10 (API 29) or newer."
            managedOwner ->
                "Managed-owner status detected. This does not prove that this device accepts this OTA; package signature, target build, policy and OEM update support must still be validated."
            else ->
                "A normal installed APK is not Device Owner or Profile Owner. A runtime permission cannot grant this role; no system OTA installation is available through this API."
        }

        return ManaOtaCapability(
            apiLevel = Build.VERSION.SDK_INT,
            deviceOwner = deviceOwner,
            profileOwner = profileOwner,
            dpmInstallApiPresent = apiPresent,
            ordinaryAppCanInstallSystemOta = apiPresent && managedOwner,
            status = status,
            explanation = explanation
        )
    }
}
