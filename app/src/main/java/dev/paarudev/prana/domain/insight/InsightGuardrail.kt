package dev.paarudev.prana.domain.insight

/**
 * Regulatory & Medical Safety Guardrail.
 *
 * Enforces non-diagnostic wellness compliance as mandated by health regulations
 * and hackathon specifications. Scans model outputs for prohibited clinical tokens
 * and enforces safe, encouraging, non-medical fallback phrasing.
 */
class InsightGuardrail {

    companion object {
        // Disallowed medical diagnostic / clinical keywords
        val PROHIBITED_KEYWORDS = listOf(
            "diagnos",
            "disease",
            "illness",
            "disorder",
            "cancer",
            "prescri",
            "medicat",
            "cure",
            "treatment",
            "patholog",
            "infarct",
            "hypertens",
            "arrhythmi",
            "tachycard",
            "bradycard",
            "cardiac arrest",
            "depression",
            "anxiety disorder",
            "doctor",
            "physician"
        )
    }

    data class ValidationResult(
        val isCompliant: Boolean,
        val detectedViolations: List<String>,
        val sanitizedText: String
    )

    fun validate(rawText: String, fallbackText: String): ValidationResult {
        if (rawText.isBlank()) {
            return ValidationResult(
                isCompliant = false,
                detectedViolations = listOf("empty_text"),
                sanitizedText = fallbackText
            )
        }

        val lower = rawText.lowercase()
        val violations = mutableListOf<String>()

        for (term in PROHIBITED_KEYWORDS) {
            if (lower.contains(term)) {
                violations.add(term)
            }
        }

        return if (violations.isEmpty()) {
            ValidationResult(
                isCompliant = true,
                detectedViolations = emptyList(),
                sanitizedText = rawText.trim()
            )
        } else {
            ValidationResult(
                isCompliant = false,
                detectedViolations = violations,
                sanitizedText = fallbackText.trim()
            )
        }
    }
}
