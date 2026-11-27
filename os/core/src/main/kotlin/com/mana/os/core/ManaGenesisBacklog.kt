package com.mana.os.core

import java.time.Instant

enum class GenesisWorkStatus {
    IMPLEMENTED, SOURCE_TESTED, INTEGRATION_PENDING, DEVICE_VALIDATION_PENDING, BLOCKED
}

enum class GenesisWorkPriority { CRITICAL, HIGH, NORMAL, LOW }

data class GenesisWorkItem(
    val id: String,
    val title: String,
    val description: String,
    val priority: GenesisWorkPriority,
    val status: GenesisWorkStatus,
    val dependencies: Set<String> = emptySet(),
    val requiredEvidence: Set<String> = emptySet(),
    val ownerApprovalRequired: Boolean = false
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
        require(title.isNotBlank() && description.isNotBlank())
    }
}

data class GenesisCapabilitySnapshot(
    val deviceFingerprint: String,
    val androidApi: Int,
    val installedModules: Set<String>,
    val availableCapabilities: Set<String>,
    val observedAt: Instant = Instant.now()
) {
    init {
        require(deviceFingerprint.isNotBlank())
        require(androidApi > 0)
    }
}

data class GenesisDevelopmentPlan(
    val snapshot: GenesisCapabilitySnapshot,
    val orderedWork: List<GenesisWorkItem>,
    val blockedWork: List<GenesisWorkItem>,
    val generatedAt: Instant = Instant.now()
)

/**
 * Durable-plan domain model for first-boot self-completion.
 *
 * The Android host is responsible for persisting snapshots and work items in the
 * encrypted MANA workspace. This planner never assumes missing hardware support,
 * grants permissions, runs generated code or installs system images.
 */
class ManaGenesisBacklog(private val workItems: List<GenesisWorkItem>) {
    init {
        require(workItems.map { it.id }.distinct().size == workItems.size) {
            "Genesis work item identifiers must be unique."
        }
        val ids = workItems.map { it.id }.toSet()
        workItems.forEach { item ->
            require(item.dependencies.all { it in ids }) {
                "Unknown dependency in work item ${item.id}."
            }
        }
        validateAcyclic()
    }

    fun plan(snapshot: GenesisCapabilitySnapshot): GenesisDevelopmentPlan {
        val missing = workItems.filter { item ->
            item.status !in setOf(GenesisWorkStatus.IMPLEMENTED, GenesisWorkStatus.SOURCE_TESTED) ||
                item.requiredEvidence.any { it !in snapshot.availableCapabilities }
        }
        val missingIds = missing.map { it.id }.toSet()
        val blocked = missing.filter { item ->
            item.status == GenesisWorkStatus.BLOCKED ||
                item.dependencies.any { dependency ->
                    dependency in missingIds && workItems.single { it.id == dependency }.status == GenesisWorkStatus.BLOCKED
                }
        }
        val blockedIds = blocked.map { it.id }.toSet()
        val ordered = topologicalOrder(missing.filter { it.id !in blockedIds })
        return GenesisDevelopmentPlan(snapshot, ordered, blocked)
    }

    private fun topologicalOrder(items: List<GenesisWorkItem>): List<GenesisWorkItem> {
        val selected = items.associateBy { it.id }
        val visited = mutableSetOf<String>()
        val result = mutableListOf<GenesisWorkItem>()
        fun visit(id: String) {
            if (!visited.add(id)) return
            selected[id]?.dependencies?.filter { it in selected }?.sorted()?.forEach(::visit)
            selected[id]?.let(result::add)
        }
        items.sortedWith(compareBy<GenesisWorkItem> { it.priority.ordinal }.thenBy { it.id })
            .forEach { visit(it.id) }
        return result
    }

    private fun validateAcyclic() {
        val byId = workItems.associateBy { it.id }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (id in visiting) error("Genesis work dependency cycle detected at $id.")
            if (!visited.add(id)) return
            visiting += id
            byId.getValue(id).dependencies.forEach(::visit)
            visiting -= id
        }
        workItems.forEach { visit(it.id) }
    }

    companion object {
        /** Initial completion backlog. Statuses are intentionally conservative. */
        fun initial(): ManaGenesisBacklog = ManaGenesisBacklog(listOf(
            GenesisWorkItem("brain.gpt-link", "Connect MANA Brain to GPT",
                "Reuse the official OpenAI API adapter; add owner-consented engineering sessions, patch review and build-log feedback.",
                GenesisWorkPriority.HIGH, GenesisWorkStatus.INTEGRATION_PENDING,
                requiredEvidence = setOf("owner_openai_api_key", "cloud_consent")),
            GenesisWorkItem("forge.patch-pipeline", "Complete self-editing pipeline",
                "Connect Brain-generated diffs to isolated CI, tests, artifact provenance and signed update staging.",
                GenesisWorkPriority.CRITICAL, GenesisWorkStatus.INTEGRATION_PENDING,
                dependencies = setOf("brain.gpt-link"), requiredEvidence = setOf("isolated_ci", "artifact_signing")),
            GenesisWorkItem("security.lab", "Build defensive security research lab",
                "Add quarantined static analysis, harmless behavior simulation, detection-rule tests and remediation workflow.",
                GenesisWorkPriority.NORMAL, GenesisWorkStatus.INTEGRATION_PENDING,
                dependencies = setOf("forge.patch-pipeline")),
            GenesisWorkItem("android.permission-onboarding", "Finish permission onboarding",
                "Explain and request Android runtime permissions only when each user-enabled feature needs them.",
                GenesisWorkPriority.HIGH, GenesisWorkStatus.INTEGRATION_PENDING),
            GenesisWorkItem("android.platform-integration", "Unify app prototype and AOSP services",
                "Integrate the Gradle app, platform Binder service, Soong product and SELinux policy in a reproducible Android 16 checkout.",
                GenesisWorkPriority.CRITICAL, GenesisWorkStatus.INTEGRATION_PENDING,
                dependencies = setOf("android.permission-onboarding"),
                requiredEvidence = setOf("android16_aosp_checkout", "selinux_policy_tests")),
            GenesisWorkItem("device.rmx5566.sources", "Verify exact RMX5566 hardware sources",
                "Obtain matching OEM kernel, device tree, vendor interfaces, partition metadata and restore package.",
                GenesisWorkPriority.CRITICAL, GenesisWorkStatus.BLOCKED,
                requiredEvidence = setOf("verified_rmx5566_kernel", "verified_device_tree", "verified_vendor", "stock_restore")),
            GenesisWorkItem("device.rom-build", "Build and validate RMX5566 system image",
                "Build only after exact sources and restore path are verified; test boot, AVB, SELinux, hardware and rollback.",
                GenesisWorkPriority.CRITICAL, GenesisWorkStatus.DEVICE_VALIDATION_PENDING,
                dependencies = setOf("android.platform-integration", "device.rmx5566.sources"),
                requiredEvidence = setOf("aosp_product_build", "avb_verified", "device_boot_test", "rollback_test"),
                ownerApprovalRequired = true),
            GenesisWorkItem("device.first-boot-self-completion", "Run first-boot MANA completion",
                "Inventory installed capabilities, persist missing work, develop compatible modules, test and stage safe updates.",
                GenesisWorkPriority.HIGH, GenesisWorkStatus.INTEGRATION_PENDING,
                dependencies = setOf("forge.patch-pipeline", "android.platform-integration"))
        ))
    }
}
