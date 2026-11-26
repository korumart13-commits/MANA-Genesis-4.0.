package com.mana.system.brain;

/** Dependency-free executable host test for MANA Brain request policy. */
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
        expect(validate(null, false, false, null, false, false) != null, "null prompt rejected");
        expect(validate("  ", false, false, null, false, false) != null, "blank prompt rejected");
        expect(validate("private", true, true, null, false, false) != null, "sensitive cloud rejected");
        expect(validate("x".repeat(ManaBrainRequestPolicy.MAX_PROMPT_CHARS + 1), false, false, null, false, false) != null,
                "oversized prompt rejected");
        expect(validate("hello", false, false, "openai", false, false) != null, "provider on local request rejected");
        expect(validate("hello", false, true, "unknown-provider", false, false) != null, "unknown provider rejected");
        expect(validate("hello", false, true, "openai", false, false) == null, "OpenAI accepted");
        expect(validate("hello", false, true, "gemini", false, false) == null, "Gemini accepted");
        expect(validate("hello", false, false, null, true, true) != null, "memory export without cloud consent rejected");
        expect(validate("hello", false, true, null, false, true) != null, "memory export without memory consent rejected");
        expect(validate("hello", false, false, null, false, false) == null, "local request accepted");
        expect(validate("hello", false, true, null, false, false) == null, "cloud request without memory export accepted");
        expect(validate("hello", false, true, null, true, true) == null, "memory export with both consents accepted");
        System.out.println("MANA Brain request policy: " + assertions + " assertions passed.");
    }
}
