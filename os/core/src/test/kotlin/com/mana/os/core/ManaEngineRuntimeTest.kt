package com.mana.os.core

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManaEngineRuntimeTest {
    private class FakeReasoner : ManaReasoningPort {
        var lastRequest: ManaReasoningRequest? = null
        override suspend fun respond(request: ManaReasoningRequest): ManaReasoningResult {
            lastRequest = request
            return ManaReasoningResult("ответ", "local-test", true)
        }
    }

    private class MemoryStore : ManaMemoryStore {
        private var values = emptyList<ManaMemoryItem>()
        override fun readAll() = values
        override fun writeAll(items: List<ManaMemoryItem>) { values = items.toList() }
    }

    private class WorldStore : MicroworldStore {
        private var value = MicroworldSnapshot()
        override fun load() = value
        override fun save(snapshot: MicroworldSnapshot) { value = snapshot }
    }

    private data class Fixture(
        val engine: ManaEngineRuntime,
        val memory: ManaMemoryService,
        val worldStore: WorldStore
    )

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return result?.getOrThrow() ?: error("Suspending test operation did not complete synchronously")
    }

    private fun fixture(reasoner: FakeReasoner): Fixture {
        val system = ManaSystemCore(DeviceFacts("RMX5566RU", "bengal", 36, "5.15.197", "arm64-v8a"))
        system.register(object : ManaSystemModule {
            override val id = "engine.memory"
            override fun start(context: ManaSystemContext) { context.services["memory"] = true }
        })
        val memory = ManaMemoryService(MemoryStore())
        val worldStore = WorldStore()
        val world = ManaMicroworldService(worldStore)
        val security = ManaSecurityService()
        val shogun = ManaShogun(ManaHydraRegistry(security))
        val engine = ManaEngineRuntime(
            system, reasoner, memory, world, shogun,
            Clock.fixed(Instant.parse("2026-11-26T20:00:00Z"), ZoneOffset.UTC)
        )
        return Fixture(engine, memory, worldStore)
    }

    @Test fun startsSystemAndExposesReadyHealth() {
        val fixture = fixture(FakeReasoner())
        val health = fixture.engine.start()
        assertEquals(ManaEngineState.READY, health.state)
        assertEquals(BootPhase.READY, health.systemPhase)
    }

    @Test fun personalMemoryRequiresRequestOptIn() {
        val reasoner = FakeReasoner()
        val fixture = fixture(reasoner)
        fixture.memory.remember("preference", "люблю синий цвет", DataSensitivity.PERSONAL)
        fixture.memory.remember("private", "секретный пароль не хранить в промпте", DataSensitivity.SECRET)
        fixture.engine.start()

        val withoutOptIn = runSuspend { fixture.engine.think(ManaEngineRequest("Что я предпочитаю?")) }
        assertEquals(0, withoutOptIn.memoryItemsUsed)
        assertFalse(reasoner.lastRequest!!.prompt.contains("люблю синий цвет"))

        val withOptIn = runSuspend {
            fixture.engine.think(ManaEngineRequest("Что я предпочитаю?", includePersonalMemory = true))
        }
        assertEquals(1, withOptIn.memoryItemsUsed)
        assertTrue(reasoner.lastRequest!!.prompt.contains("люблю синий цвет"))
        assertFalse(reasoner.lastRequest!!.prompt.contains("секретный пароль"))
    }

    @Test fun personalMemoryDoesNotReachCloudWithoutSeparateConsent() {
        val reasoner = FakeReasoner()
        val fixture = fixture(reasoner)
        fixture.memory.remember("preference", "личное предпочтение", DataSensitivity.PERSONAL)
        fixture.engine.start()
        runSuspend {
            fixture.engine.think(ManaEngineRequest(
                "вопрос", allowCloud = true, includePersonalMemory = true
            ))
        }
        assertFalse(reasoner.lastRequest!!.prompt.contains("личное предпочтение"))
        assertTrue(reasoner.lastRequest!!.allowCloud)
    }

    @Test fun sensitiveRequestsCannotUseCloudEvenWhenCallerRequestsIt() {
        val reasoner = FakeReasoner()
        val fixture = fixture(reasoner)
        fixture.engine.start()
        runSuspend {
            fixture.engine.think(ManaEngineRequest("личный вопрос", allowCloud = true, sensitive = true))
        }
        assertFalse(reasoner.lastRequest!!.allowCloud)
        assertTrue(reasoner.lastRequest!!.sensitive)
    }

    @Test fun stopsSystemAndPersistsMicroworldPause() {
        val fixture = fixture(FakeReasoner())
        fixture.engine.start()
        val stopped = fixture.engine.stop()
        assertEquals(ManaEngineState.STOPPED, stopped.state)
        assertEquals(BootPhase.STOPPED, stopped.systemPhase)
        assertEquals(MicroworldRunState.PAUSED, fixture.worldStore.load().runState)
    }

    @Test fun failedSystemBootLeavesEngineDegraded() {
        val system = ManaSystemCore(DeviceFacts("RMX5566RU", "bengal", 36, "5.15.197", "arm64-v8a"))
        system.register(object : ManaSystemModule {
            override val id = "engine.fail"
            override fun start(context: ManaSystemContext) { error("synthetic boot failure") }
        })
        val memory = ManaMemoryService(MemoryStore())
        val worldStore = WorldStore()
        val world = ManaMicroworldService(worldStore)
        val engine = ManaEngineRuntime(system, FakeReasoner(), memory, world, ManaShogun(ManaHydraRegistry(ManaSecurityService())))
        val health = engine.start()
        assertEquals(ManaEngineState.DEGRADED, health.state)
        assertNotNull(health.lastFailure)
    }
}
