package com.mana.system.brain;

import org.junit.Test;
import static org.junit.Assert.*;

/** Host-side policy tests; no device or Android runtime is required. */
public final class ManaBrainRequestPolicyTest {
    private static String validate(String text, boolean sensitive, boolean cloud,
            String provider, boolean memory, boolean memoryToCloud) {
        return ManaBrainRequestPolicy.validate(text, sensitive, cloud, provider, memory, memoryToCloud);
    }

    @Test public void rejectsNullOrBlankPrompt() {
        assertNotNull(validate(null, false, false, null, false, false));
        assertNotNull(validate("  ", false, false, null, false, false));
    }

    @Test public void rejectsOversizedPrompt() {
        assertNotNull(validate("x".repeat(ManaBrainRequestPolicy.MAX_PROMPT_CHARS + 1),
                false, false, null, false, false));
    }

    @Test public void rejectsSensitiveCloudAndProviderOnLocalRequest() {
        assertNotNull(validate("private", true, true, null, false, false));
        assertNotNull(validate("hello", false, false, "openai", false, false));
    }

    @Test public void rejectsUnknownProviderAndAcceptsKnownProvider() {
        assertNotNull(validate("hello", false, true, "unknown-provider", false, false));
        assertNull(validate("hello", false, true, "anthropic", false, false));
    }

    @Test public void rejectsMemoryExportWithoutBothConsents() {
        assertNotNull(validate("hello", false, false, null, true, true));
        assertNotNull(validate("hello", false, true, null, false, true));
    }

    @Test public void acceptsLocalRequestWithoutCloudConsent() {
        assertNull(validate("hello", false, false, null, false, false));
    }

    @Test public void acceptsCloudRequestWithoutMemoryExport() {
        assertNull(validate("hello", false, true, null, false, false));
    }

    @Test public void acceptsMemoryExportOnlyWithBothConsents() {
        assertNull(validate("hello", false, true, null, true, true));
    }
}