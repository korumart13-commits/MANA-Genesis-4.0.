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
        assertTrue(order.indexOf("platform.kernel") < order.indexOf("platform.hardware"))
        assertTrue(order.indexOf("platform.hardware") < order.indexOf("platform.android16"))
        assertTrue(order.indexOf("mana.localruntime") < order.indexOf("mana.brain"))
        assertTrue(order.indexOf("mana.brain") < order.indexOf("mana.systemcore"))
        assertTrue(order.indexOf("mana.systemcore") < order.indexOf("mana.systemui"))
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
            "mana.deviceecosystem", "mana.developercenter", "mana.autodev", "mana.kernellab",
            "mana.finance", "mana.lifestudio", "mana.microuniverse", "platform.kernel",
            "platform.android16", "mana.systemcore", "mana.localruntime", "mana.stockappbridge"
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
        val memory = ManaSystemModuleCatalog.modules.single { it.id == "mana.memory" }
        val modelManager = ManaSystemModuleCatalog.modules.single { it.id == "mana.modelmanager" }
        assertEquals(ModuleCriticality.REQUIRED, memory.criticality)
        assertEquals(ModuleCriticality.REQUIRED, modelManager.criticality)
        assertTrue(order.indexOf("mana.brain") < order.indexOf("mana.voice"))
        assertTrue(order.indexOf("mana.brain") < order.indexOf("mana.adaptiveintelligence"))
        assertTrue(order.indexOf("mana.updater") < order.indexOf("mana.recovery"))
        assertTrue(order.indexOf("mana.developercenter") < order.indexOf("mana.autodev"))
        assertTrue(order.indexOf("mana.autodev") < order.indexOf("mana.kernellab"))
        assertTrue(order.indexOf("mana.diary") < order.indexOf("mana.lifestudio"))
        assertTrue(order.indexOf("mana.gameintelligence") < order.indexOf("mana.microuniverse"))
    }

}
