package dev.paarudev.prana.domain.insight

import dev.paarudev.prana.domain.fusion.WellnessFusionEngine

/**
 * On-Device Guidance Generator.
 *
 * Generates plain-language, non-diagnostic wellness insights (max 3 sentences)
 * powered by on-device Small Language Models (Gemma 3 / Llama 3.2 via MediaPipe/LiteRT-LM)
 * with deterministic fallback and strict medical guardrail enforcement.
 */
class InsightGenerator(
    private val guardrail: InsightGuardrail = InsightGuardrail()
) {

    interface LlmInferenceProvider {
        fun isAvailable(): Boolean
        fun generate(prompt: String): String?
    }

    fun buildPrompt(
        hr: Double,
        hrv: Double,
        voiceStress: Double,
        baseline: WellnessFusionEngine.Baseline
    ): String {
        return """
            You are a calm, non-diagnostic wellness coach. Given these on-device measurements,
            write ONE short paragraph (max 3 sentences) of plain-language, encouraging,
            non-medical guidance. Never mention diseases or diagnoses.

            Today: HR=${hr.toInt()} bpm, HRV=${hrv.toInt()} ms, voice_stress=${voiceStress}/10.
            7-day average: HR=${baseline.avgHeartRateBpm.toInt()} bpm, HRV=${baseline.avgHrvMs.toInt()} ms, voice_stress=${baseline.avgVoiceStress}/10.
        """.trimIndent()
    }

    fun generateInsight(
        hr: Double,
        hrv: Double,
        voiceStress: Double,
        state: WellnessFusionEngine.WellnessState,
        baseline: WellnessFusionEngine.Baseline = WellnessFusionEngine.Baseline(),
        llmProvider: LlmInferenceProvider? = null
    ): String {
        val safeFallback = generateSafeFallback(hr, hrv, voiceStress, state, baseline)
        val prompt = buildPrompt(hr, hrv, voiceStress, baseline)

        if (llmProvider != null && llmProvider.isAvailable()) {
            val rawOutput = llmProvider.generate(prompt)
            if (!rawOutput.isNullOrBlank()) {
                val validation = guardrail.validate(rawOutput, safeFallback)
                return validation.sanitizedText
            }
        }

        // Return verified safe deterministic guidance
        return safeFallback
    }

    fun generateSafeFallback(
        hr: Double,
        hrv: Double,
        voiceStress: Double,
        state: WellnessFusionEngine.WellnessState,
        baseline: WellnessFusionEngine.Baseline
    ): String {
        val hrDelta = (hr - baseline.avgHeartRateBpm).toInt()
        val hrvDelta = (hrv - baseline.avgHrvMs).toInt()

        return when (state) {
            WellnessFusionEngine.WellnessState.HIGH_STRESS -> {
                val stressStr = String.format(java.util.Locale.US, "%.1f", voiceStress)
                "Your HRV is lower today (${hrv.toInt()} ms, $hrvDelta ms from baseline) and your voice reads more tense ($stressStr/10) than usual. A gentle 5-minute slow-breathing break before your next meeting may help restore balance. Step away from notifications and hydrate."
            }
            WellnessFusionEngine.WellnessState.OPTIMAL_RECOVERY -> {
                "Your autonomic recovery is notably strong today with an HRV of ${hrv.toInt()} ms and relaxed vocal markers. Your nervous system is well-rested and prepared for deep focus or demanding physical activity. Keep this positive rhythm going."
            }
            WellnessFusionEngine.WellnessState.MILD_STRAIN -> {
                val hrNote = if (hrDelta > 0) "resting pulse is $hrDelta bpm above average" else "markers indicate slight physical load"
                "Your $hrNote while your vocal tone remains relatively steady. Make sure to schedule short stretch breaks between focused work blocks. A quick glass of water and outdoor air can ease afternoon fatigue."
            }
            WellnessFusionEngine.WellnessState.FATIGUED -> {
                "Your recovery reserve is somewhat reduced today with lower vocal energy dynamics. Prioritize restorative downtime this evening rather than pushing through extra tasks. Consider an early wind-down routine to support rest."
            }
            WellnessFusionEngine.WellnessState.CALM_BALANCED -> {
                "Your pulse and vocal markers are closely aligned with your weekly baseline. You are holding a steady, sustainable pace throughout your day. Maintain your current hydration and steady routine."
            }
        }
    }
}
