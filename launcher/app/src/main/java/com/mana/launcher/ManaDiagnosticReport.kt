package com.mana.launcher

import org.json.JSONArray
import org.json.JSONObject

data class ManaDiagnosticModule(val id: String, val status: String, val evidence: String) {
    init { require(status in ALLOWED_STATUSES) { "Unsupported diagnostic status: $status" } }
    companion object {
        val ALLOWED_STATUSES = setOf("IMPLEMENTED", "PARTIAL", "NOT_IMPLEMENTED", "BLOCKED", "NEEDS_DEVICE_TEST")
    }
}

data class ManaDiagnosticReport(
    val generatedAtUtc: String,
    val appVersion: String,
    val androidApi: Int,
    val supportedAbis: List<String>,
    val permissions: Map<String, String>,
    val modules: List<ManaDiagnosticModule>,
    val limitations: List<String>
) {
    fun toJson(): String {
        val root = JSONObject()
            .put("schema", "mana-diagnostic-report/v1")
            .put("generatedAtUtc", generatedAtUtc)
            .put("appVersion", appVersion)
            .put("androidApi", androidApi)
            .put("supportedAbis", JSONArray(supportedAbis))
        root.put("permissions", JSONObject(permissions))
        root.put("modules", JSONArray().apply {
            modules.forEach { module ->
                put(JSONObject().put("id", module.id).put("status", module.status)
                    .put("evidence", ManaDiagnosticRedactor.redact(module.evidence)))
            }
        })
        root.put("limitations", JSONArray().apply {
            limitations.forEach { put(ManaDiagnosticRedactor.redact(it)) }
        })
        return root.toString(2)
    }

    fun toMarkdown(): String = buildString {
        appendLine("# MANA — диагностический отчёт")
        appendLine()
        appendLine("- Схема: `mana-diagnostic-report/v1`")
        appendLine("- Создан: $generatedAtUtc")
        appendLine("- Версия приложения: $appVersion")
        appendLine("- Android API: $androidApi")
        appendLine("- ABI: ${supportedAbis.joinToString()}")
        appendLine()
        appendLine("## Разрешения")
        permissions.forEach { (name, state) -> appendLine("- $name: $state") }
        appendLine()
        appendLine("## Модули")
        modules.forEach { appendLine("- **${it.id}** — ${it.status}: ${ManaDiagnosticRedactor.redact(it.evidence)}") }
        appendLine()
        appendLine("## Ограничения")
        limitations.forEach { appendLine("- ${ManaDiagnosticRedactor.redact(it)}") }
        appendLine()
        appendLine("_Отчёт не содержит журналов, переписки, контактов, Android ID, точной геолокации или секретных ключей._")
    }.let(ManaDiagnosticRedactor::redact)
}
