package com.mana.os.core

import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class ManaConnectorKind { AI_MODEL, GOOGLE_SERVICE, INTERNET_SERVICE, PRODUCTIVITY, CLOUD_RUNTIME }
enum class ManaConnectorState { ENABLED, DISABLED, UNAVAILABLE }

data class ManaConnectorDescriptor(
    val id: String,
    val displayName: String,
    val kind: ManaConnectorKind,
    val capabilities: Set<String>,
    val state: ManaConnectorState = ManaConnectorState.DISABLED,
    val supportsSensitiveData: Boolean = false
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
        require(displayName.isNotBlank() && displayName.length <= 120)
        require(capabilities.isNotEmpty() && capabilities.size <= 32)
        require(capabilities.all { it.matches(Regex("[a-z][a-z0-9_.-]{1,63}")) })
    }
}

/** Provider adapters must use official APIs and keep credentials outside source code. */
interface ManaServiceConnector {
    val descriptor: ManaConnectorDescriptor
    suspend fun execute(capability: String, request: String): String
}

data class ManaConnectorGrant(
    val connectorId: String,
    val allowedCapabilities: Set<String>,
    val expiresAt: Instant? = null
)

/**
 * One registry for up to 100 AI and service connectors. Registration does not imply
 * authorization. Network access and credentials remain the responsibility of adapters.
 */
class ManaConnectorRegistry(
    private val clock: Clock = Clock.systemUTC(),
    private val maximumConnectors: Int = 100
) {
    private val connectors = linkedMapOf<String, ManaServiceConnector>()
    private val grants = linkedMapOf<String, ManaConnectorGrant>()

    init { require(maximumConnectors in 1..100) }

    @Synchronized
    fun register(connector: ManaServiceConnector) {
        val id = connector.descriptor.id
        require(id !in connectors) { "Connector already registered: $id" }
        require(connectors.size < maximumConnectors) { "MANA connector limit reached" }
        connectors[id] = connector
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean) {
        val old = connectors[id] ?: error("Unknown connector: $id")
        val state = if (enabled) ManaConnectorState.ENABLED else ManaConnectorState.DISABLED
        connectors[id] = object : ManaServiceConnector {
            override val descriptor = old.descriptor.copy(state = state)
            override suspend fun execute(capability: String, request: String) =
                old.execute(capability, request)
        }
        if (!enabled) grants.remove(id)
    }

    @Synchronized
    fun authorize(grant: ManaConnectorGrant) {
        val connector = connectors[grant.connectorId] ?: error("Unknown connector: ${grant.connectorId}")
        require(connector.descriptor.state == ManaConnectorState.ENABLED) { "Connector is disabled" }
        require(grant.allowedCapabilities.isNotEmpty())
        require(grant.allowedCapabilities.all { it in connector.descriptor.capabilities }) {
            "Grant contains unsupported capabilities"
        }
        require(grant.expiresAt == null || grant.expiresAt.isAfter(clock.instant()))
        grants[grant.connectorId] = grant
    }

    @Synchronized
    fun revoke(id: String) { grants.remove(id) }

    suspend fun invoke(id: String, capability: String, request: String, sensitive: Boolean = false): String {
        require(request.isNotBlank() && request.length <= 32_000)
        val pair = synchronized(this) {
            val connector = connectors[id] ?: error("Unknown connector: $id")
            check(connector.descriptor.state == ManaConnectorState.ENABLED) { "Connector is not enabled" }
            val grant = grants[id] ?: error("Connector has no user authorization")
            check(grant.expiresAt == null || clock.instant().isBefore(grant.expiresAt)) {
                grants.remove(id)
                "Connector authorization expired"
            }
            check(capability in grant.allowedCapabilities) { "Capability is not authorized" }
            check(!sensitive || connector.descriptor.supportsSensitiveData) {
                "Connector is not approved for sensitive data"
            }
            connector
        }
        return pair.execute(capability, request)
    }

    @Synchronized
    fun catalog(): List<ManaConnectorDescriptor> = connectors.values.map { it.descriptor }

    @Synchronized
    fun count(): Int = connectors.size
}

data class ManaProviderCandidate(
    val connectorId: String,
    val capability: String,
    val available: Boolean,
    val supportsSensitiveData: Boolean,
    val estimatedCostMicros: Long,
    val priority: Int = 0
) {
    init { require(estimatedCostMicros >= 0) }
}

/** Deterministic provider selection; sensitive requests never route to cloud by default. */
class ManaProviderRouter {
    fun choose(
        candidates: List<ManaProviderCandidate>,
        sensitive: Boolean,
        cloudAllowed: Boolean
    ): ManaProviderCandidate? = candidates.asSequence()
        .filter { it.available }
        .filter { !sensitive || it.supportsSensitiveData }
        .filter { cloudAllowed || it.connectorId == "local.llama_cpp" }
        .sortedWith(compareBy<ManaProviderCandidate> { it.estimatedCostMicros }
            .thenByDescending { it.priority }
            .thenBy { it.connectorId })
        .firstOrNull()
}

/**
 * Durable cloud execution is supplied by a deployment-specific queue implementation.
 * This contract alone does not create a server or guarantee 24/7 uptime.
 */
data class ManaBackgroundJob(
    val id: String,
    val taskType: String,
    val payload: String,
    val createdAt: Instant,
    val notBefore: Instant,
    val attempts: Int = 0
)

interface ManaBackgroundJobStore {
    fun enqueue(job: ManaBackgroundJob)
    fun claim(workerId: String, now: Instant, leaseDuration: Duration): ManaBackgroundJob?
    fun complete(jobId: String, workerId: String)
    fun retry(jobId: String, workerId: String, notBefore: Instant)
}

class ManaCloudHomeQueue(
    private val store: ManaBackgroundJobStore,
    private val clock: Clock = Clock.systemUTC(),
    private val maxPayloadChars: Int = 32_000
) {
    fun schedule(taskType: String, payload: String, notBefore: Instant = clock.instant()): ManaBackgroundJob {
        require(taskType.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
        require(payload.isNotBlank() && payload.length <= maxPayloadChars)
        require(!notBefore.isBefore(clock.instant()))
        val job = ManaBackgroundJob(UUID.randomUUID().toString(), taskType, payload, clock.instant(), notBefore)
        store.enqueue(job)
        return job
    }

    fun claim(workerId: String, leaseDuration: Duration = Duration.ofMinutes(5)): ManaBackgroundJob? {
        require(workerId.isNotBlank() && workerId.length <= 128)
        require(!leaseDuration.isNegative && !leaseDuration.isZero && leaseDuration <= Duration.ofHours(1))
        return store.claim(workerId, clock.instant(), leaseDuration)
    }
}

enum class ManaPaymentState { AWAITING_CONFIRMATION, CONFIRMED, EXECUTED, CANCELLED, EXPIRED, FAILED }

data class ManaPaymentIntent(
    val id: String,
    val payee: String,
    val amount: BigDecimal,
    val currency: String,
    val memo: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val state: ManaPaymentState
)

interface ManaPaymentGateway {
    /** Called only after explicit confirmation of the exact displayed intent. */
    fun transfer(payee: String, amount: BigDecimal, currency: String, memo: String): String
}

/**
 * Payment preparation is allowed; transfer is impossible until the owner confirms the
 * exact payee, amount, currency and memo using the intent ID. No automatic payment path.
 */
class ManaPaymentApprovalService(
    private val gateway: ManaPaymentGateway,
    private val clock: Clock = Clock.systemUTC(),
    private val confirmationWindow: Duration = Duration.ofMinutes(10)
) {
    private val intents = linkedMapOf<String, ManaPaymentIntent>()

    init { require(!confirmationWindow.isNegative && !confirmationWindow.isZero) }

    @Synchronized
    fun prepare(payee: String, amount: BigDecimal, currency: String, memo: String = ""): ManaPaymentIntent {
        require(payee.isNotBlank() && payee.length <= 200)
        require(amount > BigDecimal.ZERO && amount.scale() <= 2 && amount.precision() <= 14)
        require(currency.matches(Regex("[A-Z]{3}")))
        require(memo.length <= 500)
        val now = clock.instant()
        val intent = ManaPaymentIntent(UUID.randomUUID().toString(), payee.trim(), amount,
            currency, memo.trim(), now, now.plus(confirmationWindow), ManaPaymentState.AWAITING_CONFIRMATION)
        intents[intent.id] = intent
        return intent
    }

    @Synchronized
    fun confirm(intentId: String): String {
        val old = intents[intentId] ?: error("Payment intent not found")
        check(old.state == ManaPaymentState.AWAITING_CONFIRMATION) { "Payment is not awaiting confirmation" }
        if (!clock.instant().isBefore(old.expiresAt)) {
            intents[intentId] = old.copy(state = ManaPaymentState.EXPIRED)
            error("Payment confirmation expired; prepare a new intent")
        }
        // Persist the confirmed transition before calling the external gateway to prevent replay.
        intents[intentId] = old.copy(state = ManaPaymentState.CONFIRMED)
        return try {
            val receipt = gateway.transfer(old.payee, old.amount, old.currency, old.memo)
            check(receipt.isNotBlank()) { "Payment gateway returned an empty receipt" }
            intents[intentId] = old.copy(state = ManaPaymentState.EXECUTED)
            receipt
        } catch (failure: Exception) {
            intents[intentId] = old.copy(state = ManaPaymentState.FAILED)
            throw failure
        }
    }

    @Synchronized
    fun cancel(intentId: String) {
        val old = intents[intentId] ?: error("Payment intent not found")
        check(old.state == ManaPaymentState.AWAITING_CONFIRMATION)
        intents[intentId] = old.copy(state = ManaPaymentState.CANCELLED)
    }

    @Synchronized
    fun get(intentId: String): ManaPaymentIntent? = intents[intentId]
}
