package com.mana.launcher

import com.mana.brain.InferenceProvider

/**
 * Engineering inference adapter that routes through the single MANA Brain:
 * local LiteRT-LM first; explicitly enabled cloud providers are only fallback.
 */
class ManaBrainEngineeringProvider : InferenceProvider {
    override val id: String = "mana-brain-engineering"

    override fun isAvailable(): Boolean {
        val brain = ManaEngineProcessOwner.brainBridge() ?: return false
        val localReady = ManaEngineProcessOwner.localModelRuntime()?.isReady() == true
        val cloudReady = ManaEngineProcessOwner.cloudGateway()?.isAvailable() == true ||
            ManaEngineProcessOwner.yandexProvider()?.isAvailable() == true
        return brain.let { localReady || cloudReady }
    }

    override suspend fun generate(prompt: String): String {
        val brain = ManaEngineProcessOwner.brainBridge()
            ?: error("MANA Brain service is not running. Start the MANA Engine first.")
        require(prompt.isNotBlank())
        return brain.respondDetailed(
            prompt = prompt,
            allowCloud = true,
            sensitive = false
        ).text
    }

    fun statusLabel(): String {
        if (ManaEngineProcessOwner.brainBridge() == null) return "MANA Brain не запущен"
        val localReady = ManaEngineProcessOwner.localModelRuntime()?.isReady() == true
        val cloudReady = ManaEngineProcessOwner.cloudGateway()?.isAvailable() == true ||
            ManaEngineProcessOwner.yandexProvider()?.isAvailable() == true
        return when {
            localReady && cloudReady -> "локальная модель; облачный резерв только по разрешению"
            localReady -> "локальная модель (offline)"
            cloudReady -> "облачный резерв только по разрешению"
            else -> "движок запущен, но модель не загружена и резерв не настроен"
        }
    }

    fun modelLabel(): String = ManaEngineProcessOwner.localModelRuntime()?.modelName()
        ?: ManaEngineProcessOwner.cloudGateway()?.model()
        ?: "неизвестна"
}
