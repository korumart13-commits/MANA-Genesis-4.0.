package com.mana.os.core

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/** A typed request emitted by MANA Brain after interpreting the owner's instruction. */
data class ManaActionRequest(
    val requestId: String,
    val principal: String = "mana.brain",
    val action: String,
    val parameters: Map<String, String> = emptyMap(),
    val ownerConfirmed: Boolean = false,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(requestId.isNotBlank())
        require(principal.isNotBlank())
        require(action.isNotBlank())
        require(parameters.keys.none(String::isBlank))
    }
}

enum class ManaActionRisk { ROUTINE, PERSONAL, DISRUPTIVE, FIRMWARE }
enum class ManaActionOutcome { EXECUTED, NEEDS_OWNER_CONFIRMATION, DENIED, FAILED }

data class ManaActionResult(
    val requestId: String,
    val outcome: ManaActionOutcome,
    val message: String,
    val at: Instant = Instant.now()
)

data class ManaActionSpec(
    val id: String,
    val resource: String,
    val risk: ManaActionRisk,
    val sensitivity: DataSensitivity = DataSensitivity.PERSONAL,
    val requiredParameters: Set<String> = emptySet(),
    val optionalParameters: Set<String> = emptySet()
)

/** Platform adapter. Android implementation must use documented Android APIs and granted roles. */
fun interface ManaDeviceActionExecutor {
    fun execute(request: ManaActionRequest): String
}

/**
 * Safe autonomy boundary between the language model and the operating system.
 * The model can propose only registered typed actions; it never receives a shell or root API.
 * This JVM policy layer is not itself Android permission enforcement or privileged firmware code.
 */
class ManaAutonomyController(
    private val security: ManaSecurityService,
    private val executor: ManaDeviceActionExecutor,
    private val principal: String = "mana.brain"
) {
    private val specs = linkedMapOf<String, ManaActionSpec>()
    private val audit = CopyOnWriteArrayList<ManaActionResult>()

    @Synchronized
    fun register(spec: ManaActionSpec) {
        require(spec.id.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
        require(spec.id !in specs) { "Action already registered: ${spec.id}" }
        require(spec.requiredParameters.intersect(spec.optionalParameters).isEmpty())
        specs[spec.id] = spec
    }

    fun request(request: ManaActionRequest): ManaActionResult {
        val spec = synchronized(this) { specs[request.action] }
            ?: return record(request, ManaActionOutcome.DENIED, "Action is not in the MANA allowlist.")

        if (request.principal != principal) {
            return record(request, ManaActionOutcome.DENIED, "Only the configured MANA Brain principal may request device actions.")
        }
        val missing = spec.requiredParameters - request.parameters.keys
        val unexpected = request.parameters.keys - (spec.requiredParameters + spec.optionalParameters)
        if (missing.isNotEmpty() || unexpected.isNotEmpty()) {
            return record(request, ManaActionOutcome.DENIED,
                buildString {
                    if (missing.isNotEmpty()) append("Missing parameters: ${missing.sorted().joinToString()}. ")
                    if (unexpected.isNotEmpty()) append("Unexpected parameters: ${unexpected.sorted().joinToString()}.")
                }.trim())
        }

        // Firmware changes are intentionally outside the general AI action channel.
        // They must go through the signed OTA admission pipeline and a separate owner action.
        if (spec.risk == ManaActionRisk.FIRMWARE) {
            return record(request, ManaActionOutcome.DENIED,
                "Firmware actions are blocked here; use the signed, device-matched MANA OTA pipeline.")
        }
        if (spec.risk == ManaActionRisk.DISRUPTIVE && !request.ownerConfirmed) {
            return record(request, ManaActionOutcome.NEEDS_OWNER_CONFIRMATION,
                "This action changes device behaviour and requires explicit owner confirmation.")
        }

        val capability = SecurityCapability(principal, spec.resource, SecurityAction.EXECUTE)
        val decision = security.authorize(capability, spec.sensitivity)
        if (decision.effect != SecurityEffect.ALLOW) {
            return record(request, ManaActionOutcome.DENIED, decision.reason)
        }

        return try {
            val detail = executor.execute(request)
            record(request, ManaActionOutcome.EXECUTED, detail.ifBlank { "Action completed." })
        } catch (error: Exception) {
            record(request, ManaActionOutcome.FAILED, error.message ?: error::class.java.simpleName)
        }
    }

    fun auditSnapshot(): List<ManaActionResult> = audit.toList()

    private fun record(request: ManaActionRequest, outcome: ManaActionOutcome, message: String) =
        ManaActionResult(request.requestId, outcome, message).also(audit::add)
}

/** Minimal baseline action catalogue. Platform adapters may register more after review. */
object ManaDefaultActions {
    val specs = listOf(
        ManaActionSpec("launch_app", "device.apps.launch", ManaActionRisk.ROUTINE,
            requiredParameters = setOf("package")),
        ManaActionSpec("set_torch", "device.torch", ManaActionRisk.ROUTINE,
            requiredParameters = setOf("enabled")),
        ManaActionSpec("set_brightness", "device.display.brightness", ManaActionRisk.ROUTINE,
            requiredParameters = setOf("level")),
        ManaActionSpec("set_volume", "device.audio.volume", ManaActionRisk.ROUTINE,
            requiredParameters = setOf("stream", "level")),
        ManaActionSpec("set_do_not_disturb", "device.notifications.dnd", ManaActionRisk.DISRUPTIVE,
            requiredParameters = setOf("enabled")),
        ManaActionSpec("send_message", "personal.messaging.send", ManaActionRisk.DISRUPTIVE,
            sensitivity = DataSensitivity.SENSITIVE,
            requiredParameters = setOf("recipient", "text")),
        ManaActionSpec("install_verified_update", "system.update.install", ManaActionRisk.FIRMWARE,
            requiredParameters = setOf("package_id", "sha256"))
    )
}
