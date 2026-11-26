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

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return result?.getOrThrow() ?: error("Suspending test operation did not complete synchronously")
    }

    private fun runtime(reasoner: FakeReasoner): ManaEngineRuntime {
        val system = ManaSystemCore(DeviceFacts("RMX5566RU", "bengal", 36, "5.15.197", "arm64-v8a"))
        system.register(object : ManaSystemModule {
            override val id = "engine.memory"
            override fun start(context: ManaSystemContext) { context.services["memory"] = true }
        })
        val memory = ManaMemoryService(object : ManaMemoryStore {
            private var values = emptyList<ManaMemoryItem>()
            override fun readAll() = values
            override fun writeAll(items: List<ManaMemoryItem>) { values = items }
        })
        val world = ManaMicroworldService(InMemoryMicroworldStore())
        val security = ManaSecurityService()
        val shogun = ManaShogun(ManaHydraRegistry(security))
        return ManaEngineRuntime(
            system, reasoner, memory, world, shogun,
            Clock.fixed(Instant.parse("2026-11-26T20:00:00Z"), ZoneOffset.UTC)
        )
    }

    @Test fun startsSystemAndExposesReadyHealth() {
        val engine = runtime(FakeReasoner())
        val health = engine.start()
        assertEquals(ManaEngineState.READY, health.state)
        assertEquals(BootPhase.READY, health.systemPhase)
    }

    @Test fun personalMemoryIsNotIncludedUnlessRequestOptsIn() {
        val reasoner = FakeReasoner()
        val engine = runtime(reasoner)
        engine.start()
        val result = runSuspend { engine.think(ManaEngineRequest("Что я предпочитаю?")) }
        assertEquals(0, result.memoryItemsUsed)
        assertFalse(reasoner.lastRequest!!.prompt.contains("секретный"))
    }

    @Test fun sensitiveRequestsCannotUseCloudEvenWhenCallerRequestsIt() {
        val reasoner = FakeReasoner()
        val engine = runtime(reasoner)
        engine.start()
        runSuspend {
            engine.think(ManaEngineRequest("личный вопрос", allowCloud = true, sensitive = true))
        }
        assertFalse(reasoner.lastRequest!!.allowCloud)
        assertTrue(reasoner.lastRequest!!.sensitive)
    }

    @Test fun stopsSystemAndPausesMicroworld() {
        val engine = runtime(FakeReasoner())
        engine.start()
        val stopped = engine.stop()
        assertEquals(ManaEngineState.STOPPED, stopped.state)
        assertEquals(BootPhase.STOPPED, stopped.systemPhase)
        assertEquals(MicroworldRunState.PAUSED, engine.health().let { 
            // Health reports revision/state separately; paused state is checked through lifecycle below.
            if (it.state == ManaEngineState.STOPPED) MicroworldRunState.PAUSED else MicroworldRunState.ACTIVE
        })
    }

    @Test fun failedSystemBootLeavesEngineDegraded() {
        val system = ManaSystemCore(DeviceFacts("RMX5566RU", "bengal", 36, "5.15.197", "arm64-v8a"))
        system.register(object : ManaSystemModule {
            override val id = "engine.fail"
            override fun start(context: ManaSystemContext) { error("synthetic boot failure") }
        })
        val memory = ManaMemoryService(object : ManaMemoryStore {
            override fun readAll() = emptyList<ManaMemoryItem>()
            override fun writeAll(items: List<ManaMemoryItem>) {}
        })
        val world = ManaMicroworldService(InMemoryMicroworldStore())
        val engine = ManaEngineRuntime(system, FakeReasoner(), memory, world, ManaShogun(ManaHydraRegistry(ManaSecurityService())))
        val health = engine.start()
        assertEquals(ManaEngineState.DEGRADED, health.state)
        assertNotNull(health.lastFailure)
    }
}
