package com.mana.system.brain;

import org.junit.Test;
import static org.junit.Assert.*;

/** Host-side policy tests; no device or Android runtime is required. */
public final class ManaBrainRequestPolicyTest {
    @Test public void rejectsNullOrBlankPrompt() {
        assertNotNull(ManaBrainRequestPolicy.validate(null, false, false, false, false));
        assertNotNull(ManaBrainRequestPolicy.validate("  ", false, false, false, false));
    }

    @Test public void rejectsSensitiveCloudRequest() {
        assertNotNull(ManaBrainRequestPolicy.validate("private", true, true, false, false));
    }

    @Test public void rejectsMemoryExportWithoutBothConsents() {
        assertNotNull(ManaBrainRequestPolicy.validate("hello", false, false, true, true));
        assertNotNull(ManaBrainRequestPolicy.validate("hello", false, true, false, true));
    }

    @Test public void acceptsLocalRequestWithoutCloudConsent() {
        assertNull(ManaBrainRequestPolicy.validate("hello", false, false, false, false));
    }

    @Test public void acceptsCloudRequestWithoutMemoryExport() {
        assertNull(ManaBrainRequestPolicy.validate("hello", false, true, false, false));
    }

    @Test public void acceptsMemoryExportOnlyWithBothConsents() {
        assertNull(ManaBrainRequestPolicy.validate("hello", false, true, true, true));
    }
}
