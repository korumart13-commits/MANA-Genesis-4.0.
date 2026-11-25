package com.mana.os.core

/**
 * Host-testable OTA admission/state machine for MANA OS.
 *
 * This component never writes partitions. A device-specific Android adapter must
 * connect an admitted payload to update_engine after platform signing, AVB,
 * rollback and recovery configuration have been independently verified.
 */
enum class OtaState { IDLE, AVAILABLE, VERIFIED, STAGED, APPLYING, APPLIED, FAILED }

data class OtaManifest(
    val version: Long,
    val minVersion: Long,
    val product: String,
    val device: String,
    val fingerprintPrefix: String,
    val payloadSha256: String,
    val payloadSizeBytes: Long,
    val signingKeyId: String
)

data class OtaDeviceProfile(
    val product: String,
    val device: String,
    val fingerprint: String,
    val installedVersion: Long,
    val batteryPercent: Int,
    val charging: Boolean,
    val unmeteredNetwork: Boolean
)

fun interface OtaSignatureVerifier {
    fun verify(manifest: OtaManifest, payloadSha256: String): Boolean
}

data class OtaAdmission(
    val accepted: Boolean,
    val reasons: List<String>
)

class ManaOtaAdmissionController(
    private val signatureVerifier: OtaSignatureVerifier
) {
    var state: OtaState = OtaState.IDLE
        private set
    var lastFailure: String? = null
        private set

    fun inspect(manifest: OtaManifest, device: OtaDeviceProfile, observedSha256: String): OtaAdmission {
        val reasons = mutableListOf<String>()
        if (state == OtaState.APPLYING) reasons += "An update is already applying."
        if (manifest.version <= device.installedVersion) reasons += "Update is not newer than installed version."
        if (manifest.minVersion > device.installedVersion) reasons += "Installed version is below the update minimum."
        if (manifest.product != device.product) reasons += "Product mismatch."
        if (manifest.device != device.device) reasons += "Device mismatch."
        if (!device.fingerprint.startsWith(manifest.fingerprintPrefix)) reasons += "Build fingerprint mismatch."
        if (!manifest.payloadSha256.matches(Regex("[a-fA-F0-9]{64}"))) reasons += "Manifest SHA-256 is malformed."
        if (!observedSha256.equals(manifest.payloadSha256, ignoreCase = true)) reasons += "Payload SHA-256 mismatch."
        if (manifest.payloadSizeBytes <= 0) reasons += "Payload size is invalid."
        if (manifest.signingKeyId.isBlank()) reasons += "Signing key identity is missing."
        if (!signatureVerifier.verify(manifest, observedSha256)) reasons += "OTA signature verification failed."
        if (device.batteryPercent < 50 && !device.charging) reasons += "Battery is too low for unattended staging."
        if (!device.unmeteredNetwork) reasons += "Unmetered network is required for unattended staging."

        if (reasons.isEmpty()) {
            state = OtaState.VERIFIED
            lastFailure = null
        } else {
            state = OtaState.FAILED
            lastFailure = reasons.joinToString(" ")
        }
        return OtaAdmission(reasons.isEmpty(), reasons)
    }

    fun markStaged() {
        check(state == OtaState.VERIFIED) { "Only a verified payload can be staged." }
        state = OtaState.STAGED
    }

    fun markApplying() {
        check(state == OtaState.STAGED) { "Only a staged payload can be applied." }
        state = OtaState.APPLYING
    }

    fun markAppliedAfterSuccessfulBoot() {
        check(state == OtaState.APPLYING) { "No update is currently applying." }
        state = OtaState.APPLIED
    }

    fun markApplyFailed(reason: String) {
        check(state == OtaState.APPLYING || state == OtaState.STAGED)
        lastFailure = reason.ifBlank { "Unspecified OTA failure." }
        state = OtaState.FAILED
    }

    fun resetAfterFailure() {
        check(state == OtaState.FAILED)
        lastFailure = null
        state = OtaState.IDLE
    }
}
