package com.mana.system.brain;

/**
 * Pure request-policy validation shared by the platform service and host-side tests.
 * This class deliberately has no Android dependencies.
 */
final class ManaBrainRequestPolicy {
    static final int MAX_PROMPT_CHARS = 32_768;

    private ManaBrainRequestPolicy() {}

    static String validate(
            String text,
            boolean sensitive,
            boolean allowCloud,
            String preferredCloudProvider,
            boolean includePersonalMemory,
            boolean allowPersonalMemoryToCloud) {
        if (text == null || text.trim().isEmpty()) {
            return "A non-empty prompt is required.";
        }
        if (text.length() > MAX_PROMPT_CHARS) {
            return "Prompt exceeds the 32768-character limit.";
        }
        if (sensitive && allowCloud) {
            return "Sensitive requests must not be sent to a cloud provider.";
        }
        if (!allowCloud && preferredCloudProvider != null
                && !preferredCloudProvider.trim().isEmpty()) {
            return "A cloud provider cannot be selected for a local-only request.";
        }
        if (preferredCloudProvider != null && !preferredCloudProvider.trim().isEmpty()
                && !isSupportedProvider(preferredCloudProvider)) {
            return "The requested cloud provider is not supported.";
        }
        if (allowPersonalMemoryToCloud && (!allowCloud || !includePersonalMemory)) {
            return "Cloud memory export requires cloud and personal-memory consent.";
        }
        return null;
    }

    private static boolean isSupportedProvider(String provider) {
        switch (provider.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "openai":
            case "anthropic":
            case "google":
            case "gemini":
            case "xai":
            case "grok":
            case "yandex":
            case "vk":
                return true;
            default:
                return false;
        }
    }
}