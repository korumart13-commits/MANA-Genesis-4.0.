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
    @Test fun ownerRequestedModulesAreRepresentedInCatalog() {
        val ids = ManaSystemModuleCatalog.modules.map { it.id }.toSet()
        val required = setOf(
            "mana.brain", "mana.modelmanager", "mana.cloudgateway", "mana.memory", "mana.voice",
            "mana.avatar3d", "mana.calltelephony", "mana.gamebridge", "mana.integrationhub",
            "mana.avatar", "mana.adaptiveintelligence", "mana.futureupdates",
            "mana.yandex", "mana.vk", "mana.diary", "mana.lifemanager",
            "mana.contacts", "mana.receptionist", "mana.gameintelligence",
            "mana.launcher", "mana.systemui", "mana.settings", "mana.controlcenter",
            "mana.updater", "mana.recovery", "mana.localnode",
            "mana.deviceecosystem", "mana.developercenter"
        )
        assertTrue(ids.containsAll(required), "Missing owner-requested modules: ${required - ids}")
    }

    @Test fun extendedModuleDependenciesRemainAcyclicAndCloudIsOptional() {
        val order = ManaSystemModuleCatalog.dependencyOrder()
        val cloud = ManaSystemModuleCatalog.modules.single { it.id == "mana.cloudgateway" }
        val brain = ManaSystemModuleCatalog.modules.single { it.id == "mana.brain" }
        assertEquals(ModuleCriticality.OPTIONAL, cloud.criticality)
        assertTrue("mana.cloudgateway" !in brain.dependencies)
        assertTrue("mana.memory" in brain.dependencies)
        assertTrue(order.indexOf("mana.brain") < order.indexOf("mana.voice"))
        assertTrue(order.indexOf("mana.brain") < order.indexOf("mana.adaptiveintelligence"))
        assertTrue(order.indexOf("mana.updater") < order.indexOf("mana.recovery"))
    }

}
