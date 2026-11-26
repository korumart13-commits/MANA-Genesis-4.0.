package com.mana.brain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Compile-time check of the shared AIDL-generated Java contract. */
class AidlContractCompileTest {
    @Test
    fun generatedBinderAndParcelableTypesArePresent() {
        assertEquals("com.mana.brain.BrainBinderRequest", BrainBinderRequest::class.java.name)
        assertEquals("com.mana.brain.BrainBinderResponse", BrainBinderResponse::class.java.name)
        assertEquals("com.mana.brain.IBrainService", IBrainService::class.java.name)
        assertEquals("com.mana.brain.IBrainCallback", IBrainCallback::class.java.name)
        assertEquals("com.mana.brain.ILocalInferenceService", ILocalInferenceService::class.java.name)
        assertEquals("com.mana.brain.ILocalInferenceCallback", ILocalInferenceCallback::class.java.name)
    }
}
