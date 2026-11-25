package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ManaActionProposalParserTest {
    @Test fun parsesOneAllowlistedProposalWithoutConfirmingIt() {
        val proposal = ManaActionProposalParser.parse(
            "Сейчас могу включить фонарик. <mana_action action=\"set_torch\" enabled=\"true\"/>"
        )
        assertEquals("set_torch", proposal?.request?.action)
        assertEquals("true", proposal?.request?.parameters?.get("enabled"))
        assertFalse(proposal?.request?.ownerConfirmed ?: true)
    }

    @Test fun rejectsUnknownActionsAndFirmwareUpdates() {
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"run_shell\" command=\"id\"/>"))
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"install_verified_update\" package_id=\"x\" sha256=\"y\"/>"))
    }

    @Test fun rejectsMissingUnexpectedOrInvalidParameters() {
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"set_brightness\"/>"))
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"set_brightness\" level=\"999\"/>"))
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"set_torch\" enabled=\"true\" extra=\"x\"/>"))
    }

    @Test fun rejectsMultipleTagsAndMalformedAttributes() {
        assertNull(ManaActionProposalParser.parse(
            "<mana_action action=\"set_torch\" enabled=\"true\"/><mana_action action=\"set_torch\" enabled=\"false\"/>"
        ))
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"set_torch\" enabled=true/>"))
    }

    @Test fun validatesPackageAndVolumeAllowlist() {
        assertEquals(
            "com.android.settings",
            ManaActionProposalParser.parse("<mana_action action=\"launch_app\" package=\"com.android.settings\"/>")
                ?.request?.parameters?.get("package")
        )
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"launch_app\" package=\"../../bad\"/>"))
        assertNull(ManaActionProposalParser.parse("<mana_action action=\"set_volume\" stream=\"master\" level=\"2\"/>"))
    }
}
