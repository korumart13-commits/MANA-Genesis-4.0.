package com.mana.os.core

import java.util.Locale
import java.util.UUID

/**
 * Conservative parser for direct owner-issued device commands.
 *
 * It is intentionally deterministic: it does not interpret arbitrary model output,
 * scripts, shell commands or free-form parameters. Unknown wording returns null.
 */
object ManaOwnerCommandParser {
    fun parseOwnerCommand(text: String): ManaActionRequest? {
        val normalized = text.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null

        val actionAndParameters = when (normalized) {
            "включи фонарик", "включи вспышку", "turn on flashlight" ->
                "set_torch" to mapOf("enabled" to "true")
            "выключи фонарик", "выключи вспышку", "turn off flashlight" ->
                "set_torch" to mapOf("enabled" to "false")
            "включи не беспокоить", "включи режим не беспокоить", "enable do not disturb" ->
                "set_do_not_disturb" to mapOf("enabled" to "true")
            "выключи не беспокоить", "выключи режим не беспокоить", "disable do not disturb" ->
                "set_do_not_disturb" to mapOf("enabled" to "false")
            else -> {
                val brightness = Regex("^(?:установи |поставь )?яркость(?: экрана)? на? (\\d{1,3})$")
                    .matchEntire(normalized)?.groupValues?.get(1)?.toIntOrNull()
                if (brightness != null && brightness in 0..255) {
                    "set_brightness" to mapOf("level" to brightness.toString())
                } else {
                    val packageName = Regex("^(?:открой пакет |запусти пакет |open package )([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)$")
                        .matchEntire(text.trim())?.groupValues?.get(1)
                    if (packageName != null) "launch_app" to mapOf("package" to packageName) else null
                }
            }
        } ?: return null

        return ManaActionRequest(
            requestId = "owner-command-${UUID.randomUUID()}",
            action = actionAndParameters.first,
            parameters = actionAndParameters.second,
            ownerConfirmed = false
        )
    }
}
