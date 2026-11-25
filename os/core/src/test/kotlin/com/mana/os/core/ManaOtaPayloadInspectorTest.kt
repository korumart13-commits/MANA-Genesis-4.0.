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
        assertEquals("c17ece6f41c1e07ff2b2ea4e03bb77b5c3eb87ba97decca553a9a00ac652e4e0", result.sha256)
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
