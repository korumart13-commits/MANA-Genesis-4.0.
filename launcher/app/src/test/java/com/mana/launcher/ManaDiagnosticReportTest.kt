package com.mana.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManaDiagnosticReportTest {
    @Test fun redactsCommonSecretsAndPersonalData() {
        val source = "Authorization: Bearer abc.def.ghi api_key=sk-proj-abcdefghijklmnop " +
            "github_pat_abcdefghijklmnopqrstuvwxyz123456 user@example.com +1 (415) 555-0199 " +
            "https://example.test/cb?access_token=verysecret"
        val result = ManaDiagnosticRedactor.redact(source)
        listOf("abc.def.ghi", "sk-proj-abcdefghijklmnop", "github_pat_abcdefghijklmnopqrstuvwxyz123456",
            "user@example.com", "415) 555-0199", "verysecret").forEach {
            assertFalse("Leaked value: $it", result.contains(it))
        }
    }

    @Test fun serializesVersionedReportAndStatus() {
        val report = ManaDiagnosticReport(
            "2026-11-27T00:00:00Z", "4", 36, listOf("arm64-v8a"),
            mapOf("RECORD_AUDIO" to "NOT_GRANTED_OR_NOT_REQUESTED"),
            listOf(ManaDiagnosticModule("brain", "NEEDS_DEVICE_TEST", "Not tested")),
            listOf("Physical device test pending")
        )
        assertTrue(report.toJson().contains("mana-diagnostic-report/v1"))
        assertTrue(report.toJson().contains("NEEDS_DEVICE_TEST"))
        assertTrue(report.toMarkdown().contains("Physical device test pending"))
    }

    @Test fun rejectsUnknownStatus() {
        try {
            ManaDiagnosticModule("brain", "READY", "No evidence")
            throw AssertionError("Unknown status must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: only completeness-guard statuses are accepted.
        }
    }

    @Test fun redactsEvidenceAndLimitationsBeforeExport() {
        val report = ManaDiagnosticReport(
            "2026-11-27T00:00:00Z", "4", 36, listOf("arm64-v8a"), emptyMap(),
            listOf(ManaDiagnosticModule("brain", "PARTIAL", "token=abc123456789")),
            listOf("Contact admin@example.com")
        )
        val exported = report.toJson() + report.toMarkdown()
        assertFalse(exported.contains("abc123456789"))
        assertFalse(exported.contains("admin@example.com"))
    }
}
