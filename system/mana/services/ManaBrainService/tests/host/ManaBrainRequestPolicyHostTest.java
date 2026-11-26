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

    public static void main(String[] args) {
        expect(ManaBrainRequestPolicy.validate(null, false, false, false, false) != null,
                "null prompt must be rejected");
        expect(ManaBrainRequestPolicy.validate("  ", false, false, false, false) != null,
                "blank prompt must be rejected");
        expect(ManaBrainRequestPolicy.validate("private", true, true, false, false) != null,
                "sensitive cloud request must be rejected");
        expect(ManaBrainRequestPolicy.validate("hello", false, false, true, true) != null,
                "memory export without cloud consent must be rejected");
        expect(ManaBrainRequestPolicy.validate("hello", false, true, false, true) != null,
                "memory export without memory consent must be rejected");
        expect(ManaBrainRequestPolicy.validate("hello", false, false, false, false) == null,
                "local request must be accepted");
        expect(ManaBrainRequestPolicy.validate("hello", false, true, false, false) == null,
                "cloud request without memory export must be accepted");
        expect(ManaBrainRequestPolicy.validate("hello", false, true, true, true) == null,
                "memory export with both consents must be accepted");
        System.out.println("MANA Brain request policy: " + assertions + " assertions passed.");
    }
}
