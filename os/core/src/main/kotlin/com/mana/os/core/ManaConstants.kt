package com.mana.os.core

/**
 * Central, platform-neutral configuration for MANA XZ / MANA Genesis 4.0.
 *
 * This file contains defaults and identifiers only. It does not grant Android
 * permissions, enable privileged access, start background work, or guarantee
 * that a model/backend is available. Platform code must verify capabilities
 * before acting and must allow the owner to change user-facing settings.
 */
object ManaConstants {
    object Identity {
        const val PRODUCT_NAME = "MANA XZ"
        const val BRAIN_NAME = "MANA Brain"
        const val OS_NAME = "MANA OS"
        const val PROJECT_NAME = "MANA Genesis"
        const val VERSION_NAME = "4.0.0"
        const val VERSION_CODE = 40000
        const val CODENAME = "GENESIS"
        const val DEFAULT_LANGUAGE_TAG = "ru-RU"
        const val DEFAULT_TIME_ZONE = "system"
    }

    object Brand {
        const val COLOR_BACKGROUND = "#071426"
        const val COLOR_SURFACE = "#102B50"
        const val COLOR_PRIMARY = "#1765A8"
        const val COLOR_ACCENT = "#54C8F5"
        const val COLOR_TEXT = "#EAF4FF"
        const val COLOR_SECONDARY_TEXT = "#91A9C5"
        const val BOOT_WORDMARK = "MANA"
    }

    object Platform {
        const val MIN_ANDROID_API = 29
        const val REFERENCE_ANDROID_API = 36
        const val TARGET_DEVICE_MODEL = "RMX5566"
        const val TARGET_DEVICE_VARIANT = "RMX5566RU"
        const val TARGET_DEVICE_MARKETING_NAME = "realme C85 4G"
        const val TARGET_ABI = "arm64-v8a"
        const val DEVICE_PROFILE_MUST_BE_VERIFIED = true
        const val PHYSICAL_RAM_MIB_MUST_BE_MEASURED = true
    }

    object Engine {
        const val ENGINE_NAME = "MANA Engine"
        const val ENGINE_VERSION = "2.0"
        const val ARCHITECTURE_ID = "mana-brain-local-first-v2"
        const val DEFAULT_BACKEND = "auto"
        const val BACKEND_LLAMA_CPP = "llama.cpp"
        const val BACKEND_LITERT_LM = "LiteRT-LM"
        const val BACKEND_EXECUTORCH = "ExecuTorch"
        const val BACKEND_MLC = "MLC-LLM"
        const val DEFAULT_CPU_THREADS = 2
        const val MAX_DEFAULT_CPU_THREADS = 4
        const val DEFAULT_CONTEXT_TOKENS = 2048
        const val MAX_DEFAULT_CONTEXT_TOKENS = 4096
        const val KEEP_MODEL_RESIDENT_BY_DEFAULT = false
        const val UNLOAD_MODEL_ON_CRITICAL_MEMORY_PRESSURE = true
        const val CANCEL_GENERATION_ON_THERMAL_CRITICAL = true
        const val REQUIRE_RUNTIME_BENCHMARK_BEFORE_BACKEND_SELECTION = true
        const val REQUIRE_DEVICE_CAPABILITY_CHECK = true
    }

    object Models {
        const val MODEL_GEMMA_4_E2B = "gemma-4-E2B-it"
        const val GEMMA_4_E2B_FORMAT = "litertlm"
        const val GEMMA_4_E2B_RELATIVE_PATH = "models/Gemma/gemma-4-E2B-it.litertlm"
        const val LLAMA_CPP_MODEL_FORMAT = "gguf"
        const val LLAMA_CPP_MODEL_DIRECTORY = "models/GGUF"
        const val MODEL_DOWNLOAD_REQUIRES_OWNER_CONSENT = true
        const val MODEL_LICENSE_MUST_BE_ACCEPTED = true
        const val VERIFY_MODEL_SHA256 = true
        const val NEVER_TREAT_ONE_MODEL_FORMAT_AS_ANOTHER = true
    }

    object Providers {
        const val LOCAL_PROVIDER = "local"
        const val OPENAI = "openai"
        const val ANTHROPIC = "anthropic"
        const val GOOGLE_GEMINI = "google-gemini"
        const val XAI_GROK = "xai-grok"
        const val CLOUD_ENABLED_BY_DEFAULT = false
        const val CLOUD_API_KEYS_MUST_NOT_BE_BUNDLED = true
        const val CLOUD_REQUESTS_REQUIRE_USER_SETTING = true
        const val PERSONAL_MEMORY_TO_CLOUD_REQUIRES_SEPARATE_CONSENT = true
        val SUPPORTED_CLOUD_PROVIDERS = listOf(OPENAI, ANTHROPIC, GOOGLE_GEMINI, XAI_GROK)
    }

    object Memory {
        const val DATABASE_NAME = "mana_memory.db"
        const val ENCRYPTION_REQUIRED = true
        const val PERSONAL_MEMORY_OPT_IN_PER_REQUEST = true
        const val SECRET_DATA_IN_PROMPTS_ALLOWED = false
        const val DEFAULT_MAX_MEMORY_ITEMS_PER_REQUEST = 8
        const val DEFAULT_MAX_MEMORY_CHARS_PER_REQUEST = 6000
        const val DIARY_MODULE_ID = "mana.diary"
        const val KNOWLEDGE_MODULE_ID = "mana.knowledge"
    }

    object Storage {
        const val PRIVATE_ROOT_DIRECTORY = "mana-xz"
        const val MODELS_DIRECTORY = "models"
        const val MEMORY_DIRECTORY = "memory"
        const val MODULES_DIRECTORY = "modules"
        const val STAGING_DIRECTORY = "staging"
        const val BACKUPS_DIRECTORY = "backups"
        const val LOGS_DIRECTORY = "logs"
        const val REPORTS_DIRECTORY = "reports"
        const val CONFIG_DIRECTORY = "config"
    }

    object Packages {
        const val MODULE_PACKAGE_EXTENSION = ".manapack"
        const val MANIFEST_PATH = "META-INF/manapack.properties"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val PAYLOAD_HASH_ALGORITHM = "SHA-256"
        const val MAX_PACKAGE_BYTES = 4L * 1024L * 1024L * 1024L
        const val REQUIRE_TRUSTED_SIGNATURE = true
        const val CHECK_DEVICE_AND_API_COMPATIBILITY = true
        const val INSTALL_IN_PRIVATE_STORAGE = true
        const val REQUIRE_ROLLBACK_SUPPORT = true
        const val REQUIRE_OWNER_CONFIRMATION_FOR_INSTALL = true
    }

    object Safety {
        const val LOCAL_FIRST = true
        const val ARBITRARY_GENERATED_CODE_EXECUTION_ENABLED = false
        const val SILENT_APP_INSTALLATION_ALLOWED = false
        const val SILENT_SYSTEM_PARTITION_MODIFICATION_ALLOWED = false
        const val SILENT_BOOTLOADER_UNLOCK_ALLOWED = false
        const val SILENT_FIRMWARE_FLASH_ALLOWED = false
        const val CONFIRM_BEFORE_CALLS_OR_MESSAGES = true
        const val CONFIRM_BEFORE_PAYMENTS = true
        const val CONFIRM_BEFORE_PUBLICATION = true
        const val CONFIRM_BEFORE_DELETION = true
        const val CONFIRM_BEFORE_PACKAGE_INSTALLATION = true
        const val SIGNED_UPDATES_REQUIRED = true
        const val UPDATE_ROLLBACK_REQUIRED = true
    }

    object Avatar {
        const val AVATAR_ID = "mana.primary"
        const val DEFAULT_VOICE_LANGUAGE = "ru-RU"
        const val DEFAULT_VOICE_STYLE = "soft-expressive"
        const val IDENTITY_MUST_REMAIN_STABLE = true
        const val OUTFITS_AND_ACCESSORIES_MAY_CHANGE = true
    }

    object Modules {
        const val BRAIN = "mana.brain"
        const val LAUNCHER = "mana.launcher"
        const val SYSTEM_UI = "mana.systemui"
        const val SETTINGS = "mana.settings"
        const val CONTROL_CENTER = "mana.controlcenter"
        const val SMART_RECEPTIONIST = "mana.smart-receptionist"
        const val CONTACT_INTELLIGENCE = "mana.contact-intelligence"
        const val LIFE_MANAGER = "mana.life-manager"
        const val FUTURE_UPDATES = "mana.future-updates"
        const val GAME_INTELLIGENCE = "mana.game-intelligence"
        const val YANDEX = "mana.yandex"
        const val VK = "mana.vk"
        const val PACK_MANAGER = "mana.packmanager"
        const val DEVICE_COMPANIONS = "mana.device-companions"

        val PLANNED_MODULE_IDS = listOf(
            BRAIN,
            LAUNCHER,
            SYSTEM_UI,
            SETTINGS,
            CONTROL_CENTER,
            SMART_RECEPTIONIST,
            CONTACT_INTELLIGENCE,
            LIFE_MANAGER,
            Memory.DIARY_MODULE_ID,
            FUTURE_UPDATES,
            GAME_INTELLIGENCE,
            YANDEX,
            VK,
            PACK_MANAGER,
            DEVICE_COMPANIONS
        )
    }

    /** Values used in diagnostics; these are targets, not claims of test completion. */
    object Diagnostics {
        val ANDROID_API_SMOKE_TEST_TARGETS = listOf(29, 33, 36)
        const val DEVICE_INSTALL_TEST_REQUIRED = true
        const val REAL_DEVICE_INFERENCE_TEST_REQUIRED = true
        const val REPORT_UNVERIFIED_FEATURES_AS_UNVERIFIED = true
    }
}
