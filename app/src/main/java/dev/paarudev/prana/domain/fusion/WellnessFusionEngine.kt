package dev.paarudev.prana.domain.fusion

import kotlin.math.*

/**
 * Multimodal Wellness Fusion Engine.
 *
 * Fuses optical rPPG metrics (Heart Rate, HRV RMSSD) and acoustic prosody (Voice Stress Index)
 * relative to the user's 7-day baseline to produce a unified, non-diagnostic Wellness Score and State.
 */
class WellnessFusionEngine {

    enum class WellnessState(val displayName: String, val summary: String) {
        OPTIMAL_RECOVERY("Optimal Recovery", "Autonomic tone is high and stress markers are minimal."),
        CALM_BALANCED("Calm & Balanced", "Heart rate and vocal markers are closely aligned with your baseline."),
        MILD_STRAIN("Mild Strain", "Slightly elevated cardiovascular or vocal markers detected."),
        HIGH_STRESS("Elevated Tension", "Autonomic markers indicate notable physical or mental stress."),
        FATIGUED("Fatigued / Low Reserve", "Depressed HRV and lower vocal activation indicate a need for recovery.")
    }

    data class Baseline(
        val avgHeartRateBpm: Double = 72.0,
        val avgHrvMs: Double = 48.0,
        val avgVoiceStress: Double = 4.0,
        val daysSampled: Int = 7
    )

    data class FactorBreakdown(
        val name: String,
        val impactPercent: Int, // e.g. +15 or -10
        val note: String
    )

    data class FusedResult(
        val wellnessScore: Int, // 0 to 100
        val state: WellnessState,
        val autonomicToneHrvScore: Int, // 0 to 100
        val cardiovascularScore: Int, // 0 to 100
        val vocalTensionScore: Int, // 0 to 100
        val factors: List<FactorBreakdown>
    )

    fun fuseSignals(
        heartRateBpm: Double,
        hrvMs: Double,
        voiceStress: Double,
        baseline: Baseline = Baseline()
    ): FusedResult {
        // 1. Cardiovascular Score (Target: close to baseline or 60-75 BPM resting)
        val hrDelta = heartRateBpm - baseline.avgHeartRateBpm
        val hrScore = when {
            hrDelta <= 2.0 -> 95.0
            hrDelta <= 8.0 -> 82.0 - (hrDelta - 2.0) * 2.0
            hrDelta <= 18.0 -> 70.0 - (hrDelta - 8.0) * 2.5
            else -> max(30.0, 45.0 - (hrDelta - 18.0) * 1.5)
        }.coerceIn(20.0, 100.0)

        // 2. Autonomic Tone / HRV Score (Higher RMSSD = better parasympathetic recovery)
        val hrvRatio = if (baseline.avgHrvMs > 5.0) hrvMs / baseline.avgHrvMs else 1.0
        val hrvScore = when {
            hrvRatio >= 1.25 -> 96.0
            hrvRatio >= 1.00 -> 85.0 + (hrvRatio - 1.0) * 44.0
            hrvRatio >= 0.75 -> 65.0 + (hrvRatio - 0.75) * 80.0
            hrvRatio >= 0.50 -> 45.0 + (hrvRatio - 0.50) * 80.0
            else -> max(20.0, 30.0 + hrvRatio * 30.0)
        }.coerceIn(20.0, 100.0)

        // 3. Vocal Tension Score (Lower voice stress = higher wellness score)
        // voiceStress is 0.0 (very calm) to 10.0 (extreme stress)
        val vocalScore = (100.0 - (voiceStress * 8.5)).coerceIn(15.0, 100.0)

        // Weighted Multimodal Fusion:
        // HRV autonomic reserve (40%), Cardiovascular pace (30%), Vocal tension (30%)
        val composite = (hrvScore * 0.40) + (hrScore * 0.30) + (vocalScore * 0.30)
        val finalScore = round(composite).toInt().coerceIn(10, 99)

        // Determine Wellness State Bucket
        val state = when {
            finalScore >= 85 && voiceStress < 3.8 -> WellnessState.OPTIMAL_RECOVERY
            finalScore >= 70 && voiceStress < 5.8 -> WellnessState.CALM_BALANCED
            finalScore in 50..69 -> WellnessState.MILD_STRAIN
            voiceStress >= 7.0 || finalScore < 45 -> WellnessState.HIGH_STRESS
            else -> WellnessState.FATIGUED
        }

        // Generate transparent contributing factors
        val factors = mutableListOf<FactorBreakdown>()

        if (hrvRatio < 0.85) {
            factors.add(FactorBreakdown("Heart Rate Variability", -15, "HRV is ${round((1.0 - hrvRatio) * 100).toInt()}% below your 7-day average, signaling reduced recovery reserve."))
        } else if (hrvRatio > 1.15) {
            factors.add(FactorBreakdown("Heart Rate Variability", +15, "HRV is elevated above baseline, indicating strong autonomic recovery."))
        } else {
            factors.add(FactorBreakdown("Heart Rate Variability", 0, "HRV matches your typical baseline range."))
        }

        if (abs(hrDelta) > 7.0) {
            val direction = if (hrDelta > 0) "above" else "below"
            factors.add(FactorBreakdown("Resting Heart Rate", if (hrDelta > 0) -10 else +5, "Heart rate is ${abs(round(hrDelta)).toInt()} bpm $direction your weekly average."))
        } else {
            factors.add(FactorBreakdown("Resting Heart Rate", +5, "Resting pulse is steady and consistent with weekly norms."))
        }

        if (voiceStress > 6.5) {
            factors.add(FactorBreakdown("Voice Arousal", -15, "Acoustic markers show elevated vocal tension and pitch variation."))
        } else if (voiceStress < 3.5) {
            factors.add(FactorBreakdown("Voice Arousal", +10, "Voice frequency and tone reflect a relaxed, calm state."))
        } else {
            factors.add(FactorBreakdown("Voice Arousal", 0, "Vocal stress markers are in the moderate, neutral zone."))
        }

        return FusedResult(
            wellnessScore = finalScore,
            state = state,
            autonomicToneHrvScore = round(hrvScore).toInt(),
            cardiovascularScore = round(hrScore).toInt(),
            vocalTensionScore = round(vocalScore).toInt(),
            factors = factors
        )
    }
}
