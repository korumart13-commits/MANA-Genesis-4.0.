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
        SystemModuleDescriptor("platform.android16", "com.mana.platform.android16", setOf("platform.hardware", "platform.security"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ABORT_BOOT, "AOSP Android 16 framework and ART platform"),
        SystemModuleDescriptor("platform.kernel", "com.mana.platform.kernel", emptySet(),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ABORT_BOOT, "device-matched Linux kernel; retain stock kernel until exact OEM source and ABI are verified"),
        SystemModuleDescriptor("platform.hardware", "com.mana.platform.hardware", setOf("platform.kernel"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ABORT_BOOT, "read-only device facts"),
        SystemModuleDescriptor("platform.security", "com.mana.platform.security", setOf("platform.hardware"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ABORT_BOOT, "SELinux and permission policy"),
        SystemModuleDescriptor("platform.storage", "com.mana.platform.storage", setOf("platform.security"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "scoped app-private storage"),
        SystemModuleDescriptor("mana.modelmanager", "com.mana.modelmanager", setOf("platform.storage", "platform.security"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "local model files, integrity and license metadata"),
        SystemModuleDescriptor("mana.localruntime", "com.mana.localruntime", setOf("mana.modelmanager", "platform.storage"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "LiteRT-LM offline inference runtime and lifecycle"),
        SystemModuleDescriptor("mana.cloudgateway", "com.mana.cloudgateway", setOf("platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "OpenAI, Anthropic, Google and xAI adapters; per-provider opt-in; secrets in secure storage"),
        SystemModuleDescriptor("mana.brain", "com.mana.brain", setOf("platform.security", "platform.storage", "mana.localruntime", "mana.memory"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "single MANA identity; local inference first; cloud opt-in only"),
        SystemModuleDescriptor("mana.avatar", "com.mana.avatar", setOf("mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "camera/microphone only by explicit permission"),
        SystemModuleDescriptor("mana.avatar3d", "com.mana.avatar3d", setOf("mana.avatar", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "local glTF assets, overlay permission, user-controlled rendering"),
        SystemModuleDescriptor("mana.systemcore", "com.mana.systemcore", setOf("platform.android16", "platform.storage", "mana.brain"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "MANA service lifecycle, dependency ordering, health and rollback supervisor"),
        SystemModuleDescriptor("mana.systemui", "com.mana.systemui", setOf("platform.security", "mana.brain", "mana.systemcore"),
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
        SystemModuleDescriptor("mana.lifestudio", "com.mana.lifestudio", setOf("mana.lifemanager", "mana.diary", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "user-approved life planning and personal workspace"),
        SystemModuleDescriptor("mana.finance", "com.mana.finance", setOf("platform.security", "platform.storage", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "private finance records; no payment or transfer without explicit owner authorization"),
        SystemModuleDescriptor("mana.microuniverse", "com.mana.microuniverse", setOf("mana.brain", "mana.memory", "mana.gameintelligence"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "sandboxed world simulation, micro-worlds and creator tools"),
        SystemModuleDescriptor("mana.contacts", "com.mana.contacts", setOf("platform.security", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "contacts permission required"),
        SystemModuleDescriptor("mana.receptionist", "com.mana.receptionist", setOf("mana.contacts", "mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "phone and contacts permissions"),
        SystemModuleDescriptor("mana.calltelephony", "com.mana.calltelephony", setOf("mana.receptionist", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "owner-granted Telecom role; immediate Android screening response; no covert call recording"),
        SystemModuleDescriptor("mana.gameintelligence", "com.mana.gameintelligence", setOf("mana.brain"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "per-game user consent"),
        SystemModuleDescriptor("mana.gamebridge", "com.mana.gamebridge", setOf("mana.gameintelligence", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "official game APIs and owner-controlled Minecraft Script Event bridge; no anti-cheat bypass"),
        SystemModuleDescriptor("mana.updater", "com.mana.updater", setOf("platform.security", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "signed updates and rollback metadata"),
        SystemModuleDescriptor("mana.memory", "com.mana.memory", setOf("platform.storage", "platform.security"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "encrypted local memory; user controls export and deletion"),
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
        SystemModuleDescriptor("mana.integrationhub", "com.mana.integrationhub", setOf("mana.brain", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "Yandex, VK, cloud providers and paired devices; scoped tokens and owner consent"),
        SystemModuleDescriptor("mana.developercenter", "com.mana.developercenter", setOf("mana.brain", "platform.security", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "owner diagnostics; no implicit root or Device Owner"),
        SystemModuleDescriptor("mana.autodev", "com.mana.autodev", setOf("mana.brain", "mana.developercenter", "mana.updater", "platform.security", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "isolated source editing, tests, candidate branches and rollback; no direct mutation of running binaries"),
        SystemModuleDescriptor("mana.kernellab", "com.mana.kernellab", setOf("mana.autodev", "mana.developercenter", "platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "review-only MANA kernel patchsets until exact OEM kernel source, ABI and recovery are verified"),
        SystemModuleDescriptor("mana.localnode", "com.mana.localnode", setOf("platform.security", "platform.storage"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "LAN-only server off by default; paired devices and revocable keys"),
        SystemModuleDescriptor("mana.deviceecosystem", "com.mana.deviceecosystem", setOf("platform.security"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "explicit Bluetooth/Wi-Fi pairing and scoped device permissions"),
        SystemModuleDescriptor("mana.recovery", "com.mana.recovery", setOf("platform.security", "platform.storage", "mana.updater"),
            ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "verified recovery and rollback; device-specific boot chain only"),
        SystemModuleDescriptor("mana.stockappbridge", "com.mana.stockappbridge", setOf("platform.hardware", "platform.security"),
            ModuleCriticality.REQUIRED, ModuleFailurePolicy.ENTER_DEGRADED, "preserve and launch OEM camera, phone, radio, sensor and device applications")
    )

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
