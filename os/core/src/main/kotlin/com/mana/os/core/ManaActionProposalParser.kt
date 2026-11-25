package com.mana.os.core

import java.util.UUID

data class ManaActionProposal(
    val request: ManaActionRequest,
    val marker: String
)

/**
 * Parses a single typed action proposal emitted by MANA Brain.
 *
 * Only actions already present in ManaDefaultActions are considered. Parameter
 * names and values are validated again here and later by ManaAutonomyController
 * and the Android adapter. The returned request is never owner-confirmed.
 */
object ManaActionProposalParser {
    private val tag = Regex("<mana_action\\s+([^<>]*?)/>")
    private val attribute = Regex("""([a-z_]+)="([^"]*)"""")

    fun parse(response: String): ManaActionProposal? {
        val matches = tag.findAll(response).toList()
        if (matches.size != 1) return null
        val match = matches.single()
        val attributeText = match.groupValues[1]
        val attributes = attribute.findAll(attributeText).toList()
        if (attributes.isEmpty() || attribute.replace(attributeText, "").isNotBlank()) return null
        val names = attributes.map { it.groupValues[1] }
        if (names.size != names.toSet().size) return null
        val values = attributes.associate { it.groupValues[1] to it.groupValues[2] }
        val action = values["action"] ?: return null
        val parameters = values - "action"
        val spec = ManaDefaultActions.specs.singleOrNull { it.id == action } ?: return null
        if (spec.risk == ManaActionRisk.FIRMWARE) return null
        if (!parameters.keys.containsAll(spec.requiredParameters)) return null
        if ((parameters.keys - spec.requiredParameters - spec.optionalParameters).isNotEmpty()) return null

        val valid = when (action) {
            "launch_app" -> parameters["package"]?.matches(
                Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
            ) == true
            "set_torch", "set_do_not_disturb" ->
                parameters["enabled"] == "true" || parameters["enabled"] == "false"
            "set_brightness" -> (parameters["level"]?.toIntOrNull() ?: -1) in 0..255
            "set_volume" -> parameters["stream"] in listOf("music", "ring", "alarm", "notification", "voice_call") &&
                (parameters["level"]?.toIntOrNull() ?: -1) in 0..100
            else -> false
        }
        if (!valid) return null

        return ManaActionProposal(
            request = ManaActionRequest(
                requestId = "brain-proposal-${UUID.randomUUID()}",
                action = action,
                parameters = parameters,
                ownerConfirmed = false
            ),
            marker = match.value
        )
    }
}
