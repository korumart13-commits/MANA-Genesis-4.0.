package com.mana.os.core

enum class ModuleCriticality { REQUIRED, OPTIONAL }
enum class ModuleFailurePolicy { ABORT_BOOT, DISABLE_MODULE, ENTER_DEGRADED }

data class SystemModuleDescriptor(
    val id: String,
    val packageName: String,
    val dependencies: Set<String>,
    val criticality: ModuleCriticality,
    val failurePolicy: ModuleFailurePolicy,
    val permissionBoundary: String
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_.-]{1,63}"))) { "Invalid module id: $id" }
        require(packageName.startsWith("com.mana.")) { "MANA package must use com.mana namespace." }
        require(permissionBoundary.isNotBlank())
        require(criticality != ModuleCriticality.REQUIRED ||
            failurePolicy == ModuleFailurePolicy.ABORT_BOOT ||
            failurePolicy == ModuleFailurePolicy.ENTER_DEGRADED) {
            "Required modules cannot be silently disabled."
        }
    }
}

/**
 * Source-of-truth catalog for the integrated MANA OS service graph.
 * Descriptors are architecture metadata; they do not claim that Android
 * Binder services or privileged packages have already been implemented.
 */
object ManaSystemModuleCatalog {
    val modules: List<SystemModuleDescriptor> = listOf(
        SystemModuleDescriptor("platform.hardware", "com.mana.platform.hardware", emptySet(),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ABORT_BOOT, "read-only device facts"),
        SystemModuleDescriptor("platform.security", "com.mana.platform.security", setOf("platform.hardware"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ABORT_BOOT, "SELinux and permission policy"),
        SystemModuleDescriptor("platform.storage", "com.mana.platform.storage", setOf("platform.security"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "scoped app-private storage"),
        SystemModuleDescriptor("mana.modelmanager", "com.mana.modelmanager", setOf("platform.storage", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "model files and license metadata"),
        SystemModuleDescriptor("mana.brain", "com.mana.brain", setOf("platform.security", "platform.storage", "mana.modelmanager"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "local inference; cloud opt-in only"),
        SystemModuleDescriptor("mana.avatar", "com.mana.avatar", setOf("mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "camera/microphone only by explicit permission"),
        SystemModuleDescriptor("mana.systemui", "com.mana.systemui", setOf("platform.security", "mana.brain"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "system UI Binder APIs"),
        SystemModuleDescriptor("mana.launcher", "com.mana.launcher", setOf("mana.systemui"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "home role and user profile"),
        SystemModuleDescriptor("mana.settings", "com.mana.settings", setOf("mana.systemui"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "settings provider APIs"),
        SystemModuleDescriptor("mana.controlcenter", "com.mana.controlcenter", setOf("mana.systemui"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "approved system toggles"),
        SystemModuleDescriptor("mana.diary", "com.mana.diary", setOf("platform.storage", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "private user diary data"),
        SystemModuleDescriptor("mana.lifemanager", "com.mana.lifemanager", setOf("platform.storage", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "user-approved personal data"),
        SystemModuleDescriptor("mana.contacts", "com.mana.contacts", setOf("platform.security", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "contacts permission required"),
        SystemModuleDescriptor("mana.receptionist", "com.mana.receptionist", setOf("mana.contacts", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "phone and contacts permissions"),
        SystemModuleDescriptor("mana.gameintelligence", "com.mana.gameintelligence", setOf("mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "per-game user consent"),
        SystemModuleDescriptor("mana.updater", "com.mana.updater", setOf("platform.security", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "signed updates and rollback metadata")
,
        SystemModuleDescriptor("mana.memory", "com.mana.memory", setOf("platform.storage", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "encrypted local memory; user controls export and deletion"),
        SystemModuleDescriptor("mana.voice", "com.mana.voice", setOf("mana.brain", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "microphone and speech APIs only after explicit consent"),
        SystemModuleDescriptor("mana.adaptiveintelligence", "com.mana.adaptiveintelligence", setOf("mana.brain", "mana.memory"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "source validation; network learning opt-in; audited proposals"),
        SystemModuleDescriptor("mana.futureupdates", "com.mana.futureupdates", setOf("mana.brain", "mana.updater", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "voice/text ideas; owner approval required for code and updates"),
        SystemModuleDescriptor("mana.yandex", "com.mana.yandex", setOf("mana.brain", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "official APIs only; provider consent and credentials"),
        SystemModuleDescriptor("mana.vk", "com.mana.vk", setOf("mana.brain", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "official APIs only; provider consent and credentials"),
        SystemModuleDescriptor("mana.developercenter", "com.mana.developercenter", setOf("mana.brain", "platform.security", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "owner diagnostics; no implicit root or Device Owner"),
        SystemModuleDescriptor("mana.localnode", "com.mana.localnode", setOf("platform.security", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "LAN-only server off by default; paired devices and revocable keys"),
        SystemModuleDescriptor("mana.deviceecosystem", "com.mana.deviceecosystem", setOf("platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "explicit Bluetooth/Wi-Fi pairing and scoped device permissions"),
        SystemModuleDescriptor("mana.recovery", "com.mana.recovery", setOf("platform.security", "platform.storage", "mana.updater"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "verified recovery and rollback; device-specific boot chain only")    )

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val ids = modules.map { it.id }
        if (ids.size != ids.toSet().size) errors += "Duplicate module identifiers."
        val known = ids.toSet()
        modules.forEach { module ->
            (module.dependencies - known).forEach { missing ->
                errors += "${module.id} depends on missing module ${missing}."
            }
            if (module.id in module.dependencies) errors += "${module.id} depends on itself."
        }
        return errors
    }

    fun dependencyOrder(): List<String> {
        val errors = validate()
        require(errors.isEmpty()) { errors.joinToString(" ") }
        val byId = modules.associateBy { it.id }
        val result = mutableListOf<String>()
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            check(visiting.add(id)) { "Dependency cycle detected at $id" }
            byId.getValue(id).dependencies.sorted().forEach(::visit)
            visiting.remove(id)
            visited += id
            result += id
        }
        modules.forEach { visit(it.id) }
        return result
    }
}
