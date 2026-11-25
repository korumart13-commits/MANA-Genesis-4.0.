package com.mana.os.core

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Ed25519OtaSignatureVerifierTest {
    private val digest = "c".repeat(64)

    private fun signedManifest(): Pair<OtaManifest, java.security.KeyPair> {
        val pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val unsigned = OtaManifest(
            version = 8,
            minVersion = 5,
            product = "RMX5566RU",
            device = "RE645F",
            fingerprintPrefix = "realme/RMX5566RU/RE645F:16/",
            payloadSha256 = digest,
            payloadSizeBytes = 4096,
            signingKeyId = "mana-release-2026",
            signatureBase64 = ""
        )
        val signer = Signature.getInstance("Ed25519")
        signer.initSign(pair.private)
        signer.update(Ed25519OtaSignatureVerifier.canonicalBytes(unsigned))
        val signed = unsigned.copy(signatureBase64 = Base64.getEncoder().encodeToString(signer.sign()))
        return signed to pair
    }

    @Test fun acceptsManifestSignedByPinnedReleaseKey() {
        val (manifest, pair) = signedManifest()
        val verifier = Ed25519OtaSignatureVerifier(mapOf("mana-release-2026" to pair.public))
        assertTrue(verifier.verify(manifest, digest))
    }

    @Test fun rejectsManifestModifiedAfterSigning() {
        val (manifest, pair) = signedManifest()
        val verifier = Ed25519OtaSignatureVerifier(mapOf("mana-release-2026" to pair.public))
        assertFalse(verifier.verify(manifest.copy(version = manifest.version + 1), digest))
    }

    @Test fun rejectsUnknownSigningKey() {
        val (manifest, _) = signedManifest()
        val verifier = Ed25519OtaSignatureVerifier(emptyMap())
        assertFalse(verifier.verify(manifest, digest))
    }

    @Test fun rejectsPayloadDigestDifferentFromSignedManifest() {
        val (manifest, pair) = signedManifest()
        val verifier = Ed25519OtaSignatureVerifier(mapOf("mana-release-2026" to pair.public))
        assertFalse(verifier.verify(manifest, "d".repeat(64)))
    }

    @Test fun rejectsMalformedSignatureEncoding() {
        val (manifest, pair) = signedManifest()
        val verifier = Ed25519OtaSignatureVerifier(mapOf("mana-release-2026" to pair.public))
        assertFalse(verifier.verify(manifest.copy(signatureBase64 = "%%%"), digest))
    }
}
