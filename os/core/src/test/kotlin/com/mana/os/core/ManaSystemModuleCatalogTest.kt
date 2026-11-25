package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManaSystemModuleCatalogTest {
    @Test fun catalogHasNoMissingDependenciesOrDuplicates() {
        assertTrue(ManaSystemModuleCatalog.validate().isEmpty())
        assertEquals(ManaSystemModuleCatalog.modules.size,
            ManaSystemModuleCatalog.modules.map { it.id }.toSet().size)
    }

    @Test fun dependencyOrderPlacesFoundationsBeforeFeatures() {
        val order = ManaSystemModuleCatalog.dependencyOrder()
        assertTrue(order.indexOf("platform.security") < order.indexOf("mana.brain"))
        assertTrue(order.indexOf("mana.brain") < order.indexOf("mana.avatar"))
        assertTrue(order.indexOf("mana.systemui") < order.indexOf("mana.launcher"))
        assertTrue(order.indexOf("mana.contacts") < order.indexOf("mana.receptionist"))
    }

    @Test fun optionalFeaturesCannotBlockBootByPolicy() {
        val optional = ManaSystemModuleCatalog.modules.filter { it.criticality == ModuleCriticality.OPTIONAL }
        assertTrue(optional.isNotEmpty())
        assertTrue(optional.all { it.failurePolicy == ModuleFailurePolicy.DISABLE_MODULE })
    }

    @Test fun requiredModulesAreNeverSilentlyDisabled() {
        val required = ManaSystemModuleCatalog.modules.filter { it.criticality == ModuleCriticality.REQUIRED }
        assertTrue(required.isNotEmpty())
        assertTrue(required.all { it.failurePolicy != ModuleFailurePolicy.DISABLE_MODULE })
    }
}
