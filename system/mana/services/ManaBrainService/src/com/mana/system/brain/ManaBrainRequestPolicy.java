package com.mana.system.brain;

/**
 * Pure request-policy validation shared by the platform service and host-side tests.
 * This class deliberately has no Android dependencies.
 */
final class ManaBrainRequestPolicy {
    private ManaBrainRequestPolicy() {}

    static String validate(
            String text,
            boolean sensitive,
            boolean allowCloud,
            boolean includePersonalMemory,
            boolean allowPersonalMemoryToCloud) {
        if (text == null || text.trim().isEmpty()) {
            return "A non-empty prompt is required.";
        }
        if (sensitive && allowCloud) {
            return "Sensitive requests must not be sent to a cloud provider.";
        }
        if (allowPersonalMemoryToCloud && (!allowCloud || !includePersonalMemory)) {
            return "Cloud memory export requires cloud and personal-memory consent.";
        }
        return null;
    }
}
