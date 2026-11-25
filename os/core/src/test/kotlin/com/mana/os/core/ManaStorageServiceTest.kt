package com.mana.os.core

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManaStorageServiceTest {
    private val now = Instant.parse("2026-11-25T12:00:00Z")
    private fun item(id: String = "doc-1", pinned: Boolean = false,
                     sensitivity: DataSensitivity = DataSensitivity.PERSONAL) =
        StorageItem(id, "Document", 4096, now.minus(Duration.ofDays(120)),
            sensitivity, pinned)

    @Test fun recommendsArchiveButDoesNotMoveAutomatically() {
        val service = ManaStorageService()
        service.register(item())
        assertEquals(StorageAction.ARCHIVE, service.recommend("doc-1", now).action)
        assertEquals(StorageDisposition.ACTIVE, service.inventory().single().disposition)
    }

    @Test fun requiresApprovalAndSupportsRestore() {
        val service = ManaStorageService()
        service.register(item())
        assertFailsWith<IllegalArgumentException> {
            service.approveAndApply("doc-1", StorageAction.ARCHIVE, false, now)
        }
        assertEquals(StorageDisposition.ARCHIVED,
            service.approveAndApply("doc-1", StorageAction.ARCHIVE, true, now).disposition)
        assertEquals(StorageDisposition.ACTIVE, service.restore("doc-1", true, now).disposition)
        assertEquals(2, service.operationHistory().size)
    }

    @Test fun permanentDeletionIsDisabledEvenWithApproval() {
        val service = ManaStorageService()
        service.register(item())
        assertFailsWith<IllegalArgumentException> {
            service.approveAndApply("doc-1", StorageAction.DELETE, true, now)
        }
        assertEquals(StorageDisposition.ACTIVE, service.inventory().single().disposition)
    }

    @Test fun pinnedAndSecretItemsAreNeverSuggestedForArchiving() {
        val service = ManaStorageService()
        service.register(item("pinned", pinned = true))
        service.register(item("secret", sensitivity = DataSensitivity.SECRET))
        assertEquals(StorageAction.KEEP, service.recommend("pinned", now).action)
        assertEquals(StorageAction.KEEP, service.recommend("secret", now).action)
        assertFailsWith<IllegalArgumentException> {
            service.approveAndApply("pinned", StorageAction.ARCHIVE, true, now)
        }
    }

    @Test fun rejectsDuplicateIds() {
        val service = ManaStorageService()
        service.register(item())
        assertFailsWith<IllegalArgumentException> { service.register(item()) }
        assertTrue(service.inventory().size == 1)
    }
}
