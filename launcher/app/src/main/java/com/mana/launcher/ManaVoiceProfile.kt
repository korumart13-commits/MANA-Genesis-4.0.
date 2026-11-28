package com.mana.launcher

/** Deterministic preference order for the Russian MANA voice; audio is supplied by Android TTS. */
object ManaVoiceProfile {
    data class Candidate(
        val name: String,
        val localeTag: String,
        val quality: Int,
        val requiresNetwork: Boolean
    )

    private val femaleMarkers = listOf("female", "woman", "жен", "alena", "alyona", "irina", "anna", "milena")

    fun choose(candidates: List<Candidate>): Candidate? =
        candidates
            .filter {
                it.localeTag.substringBefore('-').substringBefore('_').equals("ru", ignoreCase = true) &&
                    !it.requiresNetwork
            }
            .maxWithOrNull(
                compareBy<Candidate> { if (femaleMarkers.any { marker -> it.name.contains(marker, ignoreCase = true) }) 1 else 0 }
                    .thenBy { if (!it.requiresNetwork) 1 else 0 }
                    .thenBy { it.quality }
                    .thenBy { if (it.localeTag.contains("ru-RU", ignoreCase = true)) 1 else 0 }
            )
}
