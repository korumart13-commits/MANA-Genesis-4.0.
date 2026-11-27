package com.mana.launcher

/** Conservative last-line scrubber for diagnostic text. */
object ManaDiagnosticRedactor {
    private val patterns = listOf(
        Regex("(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*") to "Bearer [REDACTED]",
        Regex("(?i)\\b(sk-[A-Za-z0-9_-]{12,}|sk-proj-[A-Za-z0-9_-]{12,})\\b") to "[REDACTED_API_KEY]",
        Regex("(?i)\\b(gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\\b") to "[REDACTED_GITHUB_TOKEN]",
        Regex("(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|password|passwd|secret)\\s*[:=]\\s*[^\\s,;]+") to "$1=[REDACTED]",
        Regex("(?i)([?&](?:token|key|api_key|access_token|code|password)=)[^&#\\s]+") to "$1[REDACTED]",
        Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b") to "[REDACTED_EMAIL]",
        Regex("(?<![A-Za-z0-9])\\+?\\d[\\d ()-]{7,}\\d(?![A-Za-z0-9])") to "[REDACTED_PHONE]"
    )
    fun redact(input: String): String {
        var result = input
        patterns.forEach { (pattern, replacement) -> result = pattern.replace(result, replacement) }
        return result
    }
}
