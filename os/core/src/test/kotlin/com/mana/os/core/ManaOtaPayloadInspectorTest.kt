package com.mana.os.core

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManaOtaPayloadInspectorTest {
    @Test fun calculatesExactLengthAndSha256() {
        val bytes = "MANA OTA test payload".toByteArray()
        val result = ManaOtaPayloadInspector.inspect(ByteArrayInputStream(bytes), 1024)
        assertEquals(bytes.size.toLong(), result.sizeBytes)
        assertEquals("e6cfd3e9f7fcd4d6b9f8cce9f4a5b2e8e5e5d9e1a4f2f90e6f7c5b2d7a9c1a00", result.sha256)
    }

    @Test fun rejectsPayloadOverConfiguredLimit() {
        val bytes = ByteArray(33) { 1 }
        assertFailsWith<IllegalArgumentException> {
            ManaOtaPayloadInspector.inspect(ByteArrayInputStream(bytes), 32)
        }
    }

    @Test fun rejectsNonPositiveLimit() {
        assertFailsWith<IllegalArgumentException> {
            ManaOtaPayloadInspector.inspect(ByteArrayInputStream(byteArrayOf(1)), 0)
        }
    }
}
