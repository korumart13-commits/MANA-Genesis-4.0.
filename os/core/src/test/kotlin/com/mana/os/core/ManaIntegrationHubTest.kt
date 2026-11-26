package com.mana.os.core

import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking

class ManaIntegrationHubTest {
    private val now = Instant.parse("2026-11-26T20:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private class FakeConnector(
        override val descriptor: ManaConnectorDescriptor
    ) : ManaServiceConnector {
        override suspend fun execute(capability: String, request: String) = "$capability:$request"
    }

    @Test
    fun connectorRequiresGrantAndEnforcesCapacity() = runBlocking {
        val registry = ManaConnectorRegistry(clock, maximumConnectors = 1)
        val descriptor = ManaConnectorDescriptor(
            "google.calendar", "Google Calendar", ManaConnectorKind.GOOGLE_SERVICE,
            setOf("calendar.read"), state = ManaConnectorState.ENABLED
        )
        registry.register(FakeConnector(descriptor))
        assertFailsWith<IllegalStateException> {
            runBlocking { registry.invoke("google.calendar", "calendar.read", "today") }
        }
        registry.authorize(ManaConnectorGrant("google.calendar", setOf("calendar.read"), now.plusSeconds(60)))
        assertEquals("calendar.read:today", registry.invoke("google.calendar", "calendar.read", "today"))
        assertFailsWith<IllegalArgumentException> {
            registry.register(FakeConnector(descriptor.copy(id = "another.service")))
        }
    }

    @Test
    fun providerRouterKeepsSensitiveRequestsLocal() {
        val router = ManaProviderRouter()
        val candidates = listOf(
            ManaProviderCandidate("cloud.gpt", "chat", true, false, 0),
            ManaProviderCandidate("local.llama_cpp", "chat", true, true, 10)
        )
        assertEquals("local.llama_cpp", router.choose(candidates, sensitive = true, cloudAllowed = true)?.connectorId)
        assertEquals("cloud.gpt", router.choose(candidates, sensitive = false, cloudAllowed = true)?.connectorId)
    }

    @Test
    fun paymentCannotExecuteBeforeConfirmationAndCannotReplay() {
        var transfers = 0
        val service = ManaPaymentApprovalService(
            gateway = object : ManaPaymentGateway {
                override fun transfer(payee: String, amount: BigDecimal, currency: String, memo: String): String {
                    transfers++
                    return "receipt-1"
                }
            },
            clock = clock
        )
        val intent = service.prepare("Samarkand Hotel", BigDecimal("25.00"), "EUR", "Reservation")
        assertEquals(ManaPaymentState.AWAITING_CONFIRMATION, intent.state)
        assertEquals(0, transfers)
        assertEquals("receipt-1", service.confirm(intent.id))
        assertEquals(1, transfers)
        assertEquals(ManaPaymentState.EXECUTED, service.get(intent.id)?.state)
        assertFailsWith<IllegalStateException> { service.confirm(intent.id) }
        assertEquals(1, transfers)
    }

    @Test
    fun paymentCanBeCancelledWithoutTransfer() {
        var transfers = 0
        val service = ManaPaymentApprovalService(
            gateway = object : ManaPaymentGateway {
                override fun transfer(payee: String, amount: BigDecimal, currency: String, memo: String): String {
                    transfers++
                    return "unexpected"
                }
            },
            clock = clock
        )
        val intent = service.prepare("Payee", BigDecimal("1.00"), "EUR")
        service.cancel(intent.id)
        assertEquals(ManaPaymentState.CANCELLED, service.get(intent.id)?.state)
        assertEquals(0, transfers)
    }

    @Test
    fun cloudQueueCreatesDurableJobContract() {
        var saved: ManaBackgroundJob? = null
        val store = object : ManaBackgroundJobStore {
            override fun enqueue(job: ManaBackgroundJob) { saved = job }
            override fun claim(workerId: String, now: Instant, leaseDuration: Duration): ManaBackgroundJob? = saved
            override fun complete(jobId: String, workerId: String) {}
            override fun retry(jobId: String, workerId: String, notBefore: Instant) {}
        }
        val queue = ManaCloudHomeQueue(store, clock)
        val job = queue.schedule("knowledge.refresh", "approved research")
        assertEquals(job, saved)
        assertNotNull(queue.claim("cloud-worker-1"))
    }
}
