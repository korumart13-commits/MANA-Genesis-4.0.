package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ManaMicroworldServiceTest {
    private fun service(store: MicroworldStore = InMemoryMicroworldStore(), maxSpaces: Int = 8): ManaMicroworldService =
        ManaMicroworldService(store, MicroworldLimits(maxSpaces = maxSpaces), Clock.fixed(Instant.parse("2026-11-26T20:00:00Z"), ZoneOffset.UTC)) {
            java.util.UUID.randomUUID().toString()
        }

    @Test fun createsLinkedWorldAndPersistsEveryRevision() {
        val store = InMemoryMicroworldStore()
        val world = service(store)
        val root = world.createSpace("Главная область")
        val room = world.createSpace("Лаборатория", parentId = root.id)
        val item = world.createObject(room.id, "Кристалл", tags = setOf("материал", "образец"))
        world.recordObservation(item.id, MicroworldEvidenceKind.USER_INPUT, "Поверхность отражает свет", "Пользователь")
        val experiment = world.createExperiment(room.id, "Изменится ли отражение?", "Сравнить два описанных состояния")
        world.finishExperiment(experiment.id, "Результат сохранён как наблюдение")

        val restored = service(store).snapshot()
        assertEquals(6, restored.revision)
        assertEquals(2, restored.spaces.size)
        assertEquals(1, restored.objects.size)
        assertEquals(1, restored.observations.size)
        assertEquals(MicroworldExperimentState.COMPLETED, restored.experiments.single().state)
        assertEquals(root.id, restored.spaces.first { it.id == room.id }.parentId)
    }

    @Test fun pauseBlocksWorldMutationsUntilResumed() {
        val world = service()
        world.pause()
        assertFailsWith<IllegalStateException> { world.createSpace("Новая область") }
        world.resume()
        assertEquals(MicroworldRunState.ACTIVE, world.createSpace("Новая область").let { world.snapshot().runState })
    }

    @Test fun enforcesConfiguredSpaceBudget() {
        val world = service(maxSpaces = 1)
        world.createSpace("Первая")
        assertFailsWith<IllegalArgumentException> { world.createSpace("Вторая") }
        assertEquals(1, world.snapshot().spaces.size)
    }

    @Test fun rejectsOrphanReferencesAndEmptyEvidence() {
        val world = service()
        assertFailsWith<IllegalArgumentException> { world.createObject("missing", "Объект") }
        assertFailsWith<IllegalArgumentException> { world.recordObservation(null, MicroworldEvidenceKind.LOCAL_OBSERVATION, "   ", "sensor") }
        assertEquals(0, world.snapshot().revision)
    }

    @Test fun closedExperimentsCannotBeRewritten() {
        val world = service()
        val space = world.createSpace("Песочница")
        val experiment = world.createExperiment(space.id, "Гипотеза", "Наблюдать")
        world.finishExperiment(experiment.id, "Отменено", completed = false)
        assertFailsWith<IllegalArgumentException> { world.finishExperiment(experiment.id, "Перезапись") }
    }
}
