package com.mana.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Compile-time and field-contract check of the shared AIDL-generated Java types. */
class AidlContractCompileTest {
    @Test
    fun generatedBinderAndParcelableTypesArePresent() {
        assertEquals("com.mana.brain.BrainBinderRequest", BrainBinderRequest::class.java.name)
        assertEquals("com.mana.brain.BrainBinderResponse", BrainBinderResponse::class.java.name)
        assertEquals("com.mana.brain.IBrainService", IBrainService::class.java.name)
        assertEquals("com.mana.brain.IBrainCallback", IBrainCallback::class.java.name)
        assertEquals("com.mana.brain.ILocalInferenceService", ILocalInferenceService::class.java.name)
        assertEquals("com.mana.brain.ILocalInferenceCallback", ILocalInferenceCallback::class.java.name)

        val request = BrainBinderRequest()
        request.requestId = "aidl-contract"
        request.prompt = "hello"
        request.allowCloud = false
        request.sensitive = true
        assertEquals("aidl-contract", request.requestId)
        assertEquals("hello", request.prompt)
        assertNotNull(BrainBinderRequest.CREATOR)

        val response = BrainBinderResponse()
        response.requestId = "aidl-contract"
        response.text = "world"
        response.provider = "litertlm-local"
        response.local = true
        assertEquals("world", response.text)
        assertNotNull(BrainBinderResponse.CREATOR)
    }
}
