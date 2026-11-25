package com.mana.os.core

import java.io.InputStream
import java.security.MessageDigest

data class OtaPayloadDigest(
    val sizeBytes: Long,
    val sha256: String
)

/**
 * Bounded streaming digest for a downloaded OTA payload.
 *
 * The caller owns the stream and the staging file. This helper does not extract
 * archives, invoke update_engine, or write to device partitions.
 */
object ManaOtaPayloadInspector {
    fun inspect(input: InputStream, maxBytes: Long): OtaPayloadDigest {
        require(maxBytes > 0L) { "Maximum OTA payload size must be positive." }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            require(total <= maxBytes - count) { "OTA payload exceeds the configured size limit." }
            total += count
            digest.update(buffer, 0, count)
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        return OtaPayloadDigest(total, hex)
    }
}
