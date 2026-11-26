package com.mana.system.brain;

/**
 * Dependency-free executable host test for the request policy.
 * Run with javac/java in CI; the parallel JUnit source is used by Soong.
 */
public final class ManaBrainRequestPolicyHostTest {
    private static int assertions;

    private static void expect(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static String validate(String text, boolean sensitive, boolean cloud,
            String provider, boolean memory, boolean memoryToCloud) {
        return ManaBrainRequestPolicy.validate(text, sensitive, cloud, provider, memory, memoryToCloud);
    }

    public static void main(String[] args) {
        expect(validate(null, false, false, null, false, false) != null, "null prompt must be rejected");
        expect(validate("  ", false, false, null, false, false) != null, "blank prompt must be rejected");
        expect(validate("private", true, true, null, false, false) != null, "sensitive cloud request must be rejected");
        expect(validate("x".repeat(ManaBrainRequestPolicy.MAX_PROMPT_CHARS + 1), false, false, null, false, false) != null,
                "oversized prompt must be rejected");
        expect(validate("hello", false, false, "openai", false, false) != null,
                "cloud provider on local-only request must be rejected");
        expect(validate("hello", false, true, "unknown-provider", false, false) != null,
                "unknown provider must be rejected");
        expect(validate("hello", false, true, "openai", false, false) == null,
                "supported cloud provider must be accepted");
        expect(validate("hello", false, true, "gemini", false, false) == null,
                "Gemini provider alias must be accepted");
        expect(validate("hello", false, false, null, true, true) != null,
                "memory export without cloud consent must be rejected");
        expect(validate("hello", false, true, null, false, true) != null,
                "memory export without memory consent must be rejected");
        expect(validate("hello", false, false, null, false, false) == null,
                "local request must be accepted");
        expect(validate("hello", false, true, null, false, false) == null,
                "cloud request without memory export must be accepted");
        expect(validate("hello", false, true, null, true, true) == null,
                "memory export with both consents must be accepted");
        System.out.println("MANA Brain request policy: " + assertions + " assertions passed.");
    }
}