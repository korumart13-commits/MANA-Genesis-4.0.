package com.mana.os.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaSecurityServiceTest {
    private val capability = SecurityCapability("mana.diary", "user.diary", SecurityAction.READ)

    @Test fun deniesByDefaultAndAudits() {
        val service = ManaSecurityService()
        val decision = service.authorize(capability)
        assertEquals(SecurityEffect.DENY, decision.effect)
        assertTrue(decision.reason.contains("default deny"))
        assertEquals(1, service.auditSnapshot().size)
    }

    @Test fun allowsOnlyMatchingExplicitGrant() {
        val service = ManaSecurityService()
        service.grant(SecurityGrant(capability))
        assertEquals(SecurityEffect.ALLOW, service.authorize(capability).effect)
        assertEquals(SecurityEffect.DENY,
            service.authorize(capability.copy(action = SecurityAction.SHARE)).effect)
    }

    @Test fun revokeAndExplicitDenyTakeEffect() {
        val service = ManaSecurityService()
        service.grant(SecurityGrant(capability))
        service.revoke(capability)
        assertEquals(SecurityEffect.DENY, service.authorize(capability).effect)
        service.grant(SecurityGrant(capability))
        service.deny(capability)
        assertEquals(SecurityEffect.DENY, service.authorize(capability).effect)
    }

    @Test fun expiredGrantIsRemoved() {
        val service = ManaSecurityService()
        service.grant(SecurityGrant(capability, Instant.parse("2026-01-01T00:00:00Z")))
        val result = service.authorize(capability, now = Instant.parse("2026-01-02T00:00:00Z"))
        assertEquals(SecurityEffect.DENY, result.effect)
        assertTrue(result.reason.contains("expired"))
        assertFalse(service.auditSnapshot().isEmpty())
    }

    @Test fun secretDataCannotBeSharedOrDeletedEvenWithGrant() {
        val service = ManaSecurityService()
        val share = capability.copy(action = SecurityAction.SHARE)
        val delete = capability.copy(action = SecurityAction.DELETE)
        service.grant(SecurityGrant(share))
        service.grant(SecurityGrant(delete))
        assertEquals(SecurityEffect.DENY,
            service.authorize(share, DataSensitivity.SECRET).effect)
        assertEquals(SecurityEffect.DENY,
            service.authorize(delete, DataSensitivity.SECRET).effect)
    }
}
