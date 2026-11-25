package com.mana.os.core

import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

/**
 * Verifies OTA release manifests with pinned Ed25519 public keys.
 *
 * Public keys must be provisioned by a trusted build/release process and must
 * never be accepted from the OTA download itself. This verifier does not write
 * partitions or replace Android Verified Boot.
 */
class Ed25519OtaSignatureVerifier(
    trustedReleaseKeys: Map<String, PublicKey>
) : OtaSignatureVerifier {
    private val keys = trustedReleaseKeys.toMap()

    override fun verify(manifest: OtaManifest, payloadSha256: String): Boolean {
        val key = keys[manifest.signingKeyId] ?: return false
        if (manifest.signatureBase64.isBlank()) return false
        if (!payloadSha256.equals(manifest.payloadSha256, ignoreCase = true)) return false
        return try {
            val signatureBytes = Base64.getDecoder().decode(manifest.signatureBase64)
            val verifier = Signature.getInstance("Ed25519")
            verifier.initVerify(key)
            verifier.update(canonicalBytes(manifest))
            verifier.verify(signatureBytes)
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        /** Stable UTF-8 signed representation; signature bytes are excluded. */
        fun canonicalBytes(manifest: OtaManifest): ByteArray {
            val fields = listOf(
                manifest.version.toString(),
                manifest.minVersion.toString(),
                manifest.product,
                manifest.device,
                manifest.fingerprintPrefix,
                manifest.payloadSha256.lowercase(),
                manifest.payloadSizeBytes.toString(),
                manifest.signingKeyId
            )
            return fields.joinToString("\n", postfix = "\n").toByteArray(StandardCharsets.UTF_8)
        }
    }
}
