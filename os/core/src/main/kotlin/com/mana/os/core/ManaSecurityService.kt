package com.mana.os.core

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

enum class DataSensitivity { PUBLIC, PERSONAL, SENSITIVE, SECRET }
enum class SecurityAction { READ, WRITE, EXECUTE, SHARE, DELETE, NETWORK }
enum class SecurityEffect { ALLOW, DENY }

data class SecurityCapability(
    val principal: String,
    val resource: String,
    val action: SecurityAction
) {
    init {
        require(principal.isNotBlank() && resource.isNotBlank())
    }
}

data class SecurityGrant(
    val capability: SecurityCapability,
    val expiresAt: Instant? = null
)

data class SecurityAuditEvent(
    val capability: SecurityCapability,
    val effect: SecurityEffect,
    val reason: String,
    val at: Instant = Instant.now()
)

data class SecurityDecision(
    val effect: SecurityEffect,
    val reason: String,
    val auditEvent: SecurityAuditEvent
)

/**
 * Host-testable MANA authorization policy.
 * This is not Android permission enforcement, SELinux, or a sandbox.
 */
class ManaSecurityService {
    private val grants = linkedMapOf<SecurityCapability, SecurityGrant>()
    private val explicitDenials = mutableSetOf<SecurityCapability>()
    private val audit = CopyOnWriteArrayList<SecurityAuditEvent>()

    @Synchronized
    fun grant(grant: SecurityGrant) {
        explicitDenials.remove(grant.capability)
        grants[grant.capability] = grant
    }

    @Synchronized
    fun revoke(capability: SecurityCapability) {
        grants.remove(capability)
    }

    @Synchronized
    fun deny(capability: SecurityCapability) {
        grants.remove(capability)
        explicitDenials.add(capability)
    }

    @Synchronized
    fun authorize(
        capability: SecurityCapability,
        dataSensitivity: DataSensitivity = DataSensitivity.PERSONAL,
        now: Instant = Instant.now()
    ): SecurityDecision {
        val denial = when {
            capability in explicitDenials -> "Explicitly denied by policy"
            dataSensitivity == DataSensitivity.SECRET &&
                capability.action in setOf(SecurityAction.SHARE, SecurityAction.NETWORK, SecurityAction.DELETE) ->
                "Secret data cannot be shared, sent to a network, or deleted by this policy"
            else -> null
        }
        val grant = grants[capability]
        val effect: SecurityEffect
        val reason: String
        when {
            denial != null -> { effect = SecurityEffect.DENY; reason = denial }
            grant == null -> { effect = SecurityEffect.DENY; reason = "No explicit grant (default deny)" }
            grant.expiresAt != null && !now.isBefore(grant.expiresAt) -> {
                grants.remove(capability)
                effect = SecurityEffect.DENY
                reason = "Grant expired"
            }
            else -> { effect = SecurityEffect.ALLOW; reason = "Matching explicit grant" }
        }
        val event = SecurityAuditEvent(capability, effect, reason, now)
        audit += event
        return SecurityDecision(effect, reason, event)
    }

    fun auditSnapshot(): List<SecurityAuditEvent> = audit.toList()
}
