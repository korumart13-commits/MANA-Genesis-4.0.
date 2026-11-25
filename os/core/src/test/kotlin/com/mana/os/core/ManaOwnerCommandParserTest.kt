package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse

class ManaOwnerCommandParserTest {
    @Test fun parsesExactTorchCommands() {
        assertEquals("true", ManaOwnerCommandParser.parseOwnerCommand("Включи фонарик")?.parameters?.get("enabled"))
        assertEquals("false", ManaOwnerCommandParser.parseOwnerCommand("выключи вспышку")?.parameters?.get("enabled"))
    }

    @Test fun parsesBoundedBrightnessOnly() {
        val request = ManaOwnerCommandParser.parseOwnerCommand("Установи яркость экрана на 120")
        assertEquals("set_brightness", request?.action)
        assertEquals("120", request?.parameters?.get("level"))
        assertNull(ManaOwnerCommandParser.parseOwnerCommand("поставь яркость на 999"))
    }

    @Test fun parsesPackageLaunchOnlyWithExplicitPackageIdentifier() {
        val request = ManaOwnerCommandParser.parseOwnerCommand("открой пакет com.android.settings")
        assertEquals("launch_app", request?.action)
        assertEquals("com.android.settings", request?.parameters?.get("package"))
        assertNull(ManaOwnerCommandParser.parseOwnerCommand("открой что-нибудь"))
    }

    @Test fun disruptiveRequestNeverArrivesPreconfirmed() {
        val request = ManaOwnerCommandParser.parseOwnerCommand("включи не беспокоить")
        assertEquals("set_do_not_disturb", request?.action)
        assertFalse(request?.ownerConfirmed ?: true)
    }

    @Test fun ignoresUnrecognizedOrPromptInjectionText() {
        assertNull(ManaOwnerCommandParser.parseOwnerCommand("выполни команду rm -rf /"))
        assertNull(ManaOwnerCommandParser.parseOwnerCommand("сделай что-нибудь с телефоном"))
    }
}
