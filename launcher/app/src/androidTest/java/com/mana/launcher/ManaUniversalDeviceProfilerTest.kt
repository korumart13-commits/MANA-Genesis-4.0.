package com.mana.launcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManaUniversalDeviceProfilerTest {
    @Test
    fun inspectReturnsProfileWithCoreDeviceFacts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profile = ManaUniversalDeviceProfiler.inspect(context)

        assertTrue(profile.schemaVersion >= 1)
        assertTrue(profile.facts.isNotEmpty())
        assertTrue(profile.capabilities.isNotEmpty())
        assertNotNull(profile.facts.firstOrNull { it.key == "manufacturer" })
        assertNotNull(profile.facts.firstOrNull { it.key == "model" })
        assertNotNull(profile.facts.firstOrNull { it.key == "api_level" })
        assertNotNull(profile.facts.firstOrNull { it.key == "supported_abis" })
        assertNotNull(profile.capabilities.firstOrNull { it.id == "device_audit" })
    }

    @Test
    fun jsonReportIsValidAndContainsSchemaAndArrays() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = JSONObject(ManaUniversalDeviceProfiler.inspect(context).toJson())

        assertTrue(json.getInt("schemaVersion") >= 1)
        assertTrue(json.getJSONArray("facts").length() > 0)
        assertTrue(json.getJSONArray("capabilities").length() > 0)
        assertFalse(json.getString("model").isBlank())
    }

    @Test
    fun repeatedInspectionDoesNotCrash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repeat(3) {
            val profile = ManaUniversalDeviceProfiler.inspect(context)
            assertTrue(profile.facts.any { fact -> fact.key == "api_level" })
        }
    }
}
