package com.mana.os.core

import java.io.File
import java.time.Duration
import java.time.Instant
import javax.crypto.KeyGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaPersonalServicesTest {
    private fun diaryKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test fun diaryIsEncryptedAndSurvivesReload() {
        val directory = java.nio.file.Files.createTempDirectory("mana-diary").toFile()
        val file = File(directory, "diary.bin")
        val key = diaryKey()
        val provider = ManaSecretKeyProvider { key }
        val service = ManaDiaryService(EncryptedFileManaDiaryStore(file, provider))
        val entry = service.create("Private", "secret diary text", setOf("personal"))
        assertFalse(String(file.readBytes(), Charsets.UTF_8).contains("secret diary text"))
        val reloaded = ManaDiaryService(EncryptedFileManaDiaryStore(file, provider))
        assertEquals("secret diary text", reloaded.search("secret").single().body)
        assertEquals(entry.id, reloaded.search("Private").single().id)
        directory.deleteRecursively()
    }

    @Test fun diaryDeletionRequiresExplicitOwnerApproval() {
        val file = File(java.nio.file.Files.createTempDirectory("mana-diary-delete").toFile(), "diary.bin")
        val service = ManaDiaryService(EncryptedFileManaDiaryStore(file, ManaSecretKeyProvider { diaryKey() }))
        val entry = service.create("Entry", "Body")
        assertFailsWith<IllegalArgumentException> { service.delete(entry.id, approvedByUser = false) }
        service.delete(entry.id, approvedByUser = true)
        assertTrue(service.search("").isEmpty())
        file.parentFile?.deleteRecursively()
    }

    @Test fun lifeManagerRequiresApprovalForConsequentialTasks() {
        val life = ManaLifeManagerService()
        val task = life.add("Send an important message", requiresConfirmation = true)
        assertFailsWith<IllegalArgumentException> { life.complete(task.id, approvedByUser = false) }
        assertTrue(life.complete(task.id, approvedByUser = true).completed)
    }



    @Test fun expiredMemoryIsExcludedFromRecallButRemainsAReviewCandidate() {
        val directory = java.nio.file.Files.createTempDirectory("mana-memory-expiry").toFile()
        val key = diaryKey()
        val provider = ManaSecretKeyProvider { key }
        val file = File(directory, "memory.bin")
        val now = Instant.parse("2026-11-25T12:00:00Z")
        val service = ManaMemoryService(
            EncryptedFileManaMemoryStore(EncryptedFileManaDiaryStore(file, provider, "mana.memory.expiry"))
        )
        val expired = service.remember(
            "temporary",
            "expired note",
            expiresAt = now.minusSeconds(1),
            now = now.minusSeconds(60)
        )
        assertTrue(service.recall("expired", now).isEmpty())
        assertEquals(expired.id, service.expiredCandidates(now).single().id)
        directory.deleteRecursively()
    }

    @Test fun memoryIsEncryptedPersistentAndOwnerControlled() {
        val directory = java.nio.file.Files.createTempDirectory("mana-memory").toFile()
        val key = diaryKey()
        val provider = ManaSecretKeyProvider { key }
        val file = File(directory, "memory.bin")
        val service = ManaMemoryService(
            EncryptedFileManaMemoryStore(EncryptedFileManaDiaryStore(file, provider, "mana.memory.test"))
        )
        val item = service.remember("preference", "prefers offline local-first MANA", pinned = true)
        assertFalse(String(file.readBytes(), Charsets.UTF_8).contains("offline local-first"))
        assertFailsWith<IllegalArgumentException> { service.forget(item.id, approvedByUser = false) }
        val reloaded = ManaMemoryService(
            EncryptedFileManaMemoryStore(EncryptedFileManaDiaryStore(file, provider, "mana.memory.test"))
        )
        assertEquals(item.id, reloaded.recall("offline").single().id)
        assertTrue(reloaded.expiredCandidates().isEmpty())
        reloaded.forget(item.id, approvedByUser = true)
        assertTrue(reloaded.recall().isEmpty())
        directory.deleteRecursively()
    }

    @Test fun lifeManagerCanPersistTasksInEncryptedLocalStore() {
        val directory = java.nio.file.Files.createTempDirectory("mana-life").toFile()
        val key = diaryKey()
        val provider = ManaSecretKeyProvider { key }
        val store = EncryptedFileManaLifeTaskStore(
            EncryptedFileManaDiaryStore(File(directory, "life.bin"), provider)
        )
        val manager = ManaLifeManagerService(store)
        val task = manager.add("Review MANA update", Instant.parse("2026-12-01T10:00:00Z"), requiresConfirmation = true)
        manager.complete(task.id, approvedByUser = true)
        val reloaded = ManaLifeManagerService(
            EncryptedFileManaLifeTaskStore(EncryptedFileManaDiaryStore(File(directory, "life.bin"), provider))
        )
        assertTrue(reloaded.list(includeCompleted = true).single().completed)
        assertEquals(task.title, reloaded.list(includeCompleted = true).single().title)
        directory.deleteRecursively()
    }

    @Test fun receptionistPreservesRequestedDefaultsAndConsentGate() {
        val decision = ManaSmartReceptionistPolicy().onIncomingCall(numberKnown = false)
        assertEquals(Duration.ofSeconds(10), decision.delay)
        assertTrue(decision.unknownCaller)
        assertTrue(decision.offerSummary)
        assertTrue(decision.requiresOwnerConsent)
    }

    @Test fun gameActionsNeedSupportedAdapterAndOwnerApproval() {
        val adapter = object : ManaGameAdapter {
            override val id = "test-adapter"
            override fun supports(gameId: String, action: ManaGameAction) =
                gameId == "test-game" && action in setOf(ManaGameAction.READ_STATE, ManaGameAction.MOVE)
            override fun execute(gameId: String, action: ManaGameAction, payload: String) = "ok:$action"
        }
        val game = ManaGameIntelligenceService(listOf(adapter))
        game.beginSession("test-game", "test-adapter")
        assertEquals("ok:READ_STATE", game.perform("test-game", ManaGameAction.READ_STATE))
        assertFailsWith<IllegalArgumentException> {
            game.perform("test-game", ManaGameAction.MOVE, ownerApproved = false)
        }
        assertEquals("ok:MOVE", game.perform("test-game", ManaGameAction.MOVE, ownerApproved = true))
        assertFailsWith<IllegalArgumentException> {
            game.perform("test-game", ManaGameAction.BUILD, ownerApproved = true)
        }
    }

    @Test fun futureUpdateRequiresOwnerApprovalBeforeReleaseAndInstall() {
        val updates = ManaFutureUpdateService()
        val idea = updates.capture("Add safe local model update")
        updates.advance(idea.id, ManaUpdateStage.PLANNED)
        updates.advance(idea.id, ManaUpdateStage.BUILDING)
        updates.advance(idea.id, ManaUpdateStage.TESTED)
        assertFailsWith<IllegalArgumentException> {
            updates.advance(idea.id, ManaUpdateStage.OWNER_APPROVED)
        }
        updates.advance(idea.id, ManaUpdateStage.OWNER_APPROVED, ownerApproved = true)
        updates.advance(idea.id, ManaUpdateStage.INSTALLED, ownerApproved = true)
        assertEquals(ManaUpdateStage.INSTALLED, updates.list().single().stage)
    }
    @Test fun futureUpdateIdeasPersistEncryptedAcrossReloadAndDeletionNeedsApproval() {
        val directory = java.nio.file.Files.createTempDirectory("mana-future-updates").toFile()
        val key = diaryKey()
        val provider = ManaSecretKeyProvider { key }
        val file = File(directory, "future.bin")
        val first = ManaFutureUpdateService(
            EncryptedFileManaFutureUpdateStore(
                EncryptedFileManaDiaryStore(file, provider, "mana.future.test")
            )
        )
        val idea = first.capture("Add owner-approved safe MANA update pipeline")
        first.advance(idea.id, ManaUpdateStage.PLANNED)
        assertFalse(String(file.readBytes(), Charsets.UTF_8).contains("owner-approved safe"))
        val reloaded = ManaFutureUpdateService(
            EncryptedFileManaFutureUpdateStore(
                EncryptedFileManaDiaryStore(file, provider, "mana.future.test")
            )
        )
        assertEquals(ManaUpdateStage.PLANNED, reloaded.list().single().stage)
        assertFailsWith<IllegalArgumentException> { reloaded.delete(idea.id, ownerApproved = false) }
        reloaded.delete(idea.id, ownerApproved = true)
        assertTrue(reloaded.list().isEmpty())
        directory.deleteRecursively()
    }

}
