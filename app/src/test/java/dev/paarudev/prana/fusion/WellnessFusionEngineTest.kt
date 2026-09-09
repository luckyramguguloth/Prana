package dev.paarudev.prana.fusion

import dev.paarudev.prana.domain.fusion.WellnessFusionEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WellnessFusionEngineTest {

    private lateinit var engine: WellnessFusionEngine
    private val baseline = WellnessFusionEngine.Baseline(
        avgHeartRateBpm = 70.0,
        avgHrvMs = 50.0,
        avgVoiceStress = 4.0
    )

    @Before
    fun setUp() {
        engine = WellnessFusionEngine()
    }

    @Test
    fun testOptimalRecoveryState() {
        // High HRV (75ms > 50ms), resting HR slightly below baseline (64 BPM), calm voice (2.1)
        val result = engine.fuseSignals(
            heartRateBpm = 64.0,
            hrvMs = 75.0,
            voiceStress = 2.1,
            baseline = baseline
        )

        assertEquals(WellnessFusionEngine.WellnessState.OPTIMAL_RECOVERY, result.state)
        assertTrue("Wellness score should be high for optimal recovery", result.wellnessScore >= 85)
        assertTrue(result.autonomicToneHrvScore >= 85)
    }

    @Test
    fun testCalmBalancedState() {
        // Near baseline metrics
        val result = engine.fuseSignals(
            heartRateBpm = 71.0,
            hrvMs = 49.0,
            voiceStress = 3.9,
            baseline = baseline
        )

        assertEquals(WellnessFusionEngine.WellnessState.CALM_BALANCED, result.state)
        assertTrue("Score should be between 70 and 88", result.wellnessScore in 70..88)
    }

    @Test
    fun testHighStressState() {
        // High HR (+22 BPM), severely depressed HRV (22ms), high vocal tension (8.5)
        val result = engine.fuseSignals(
            heartRateBpm = 92.0,
            hrvMs = 22.0,
            voiceStress = 8.5,
            baseline = baseline
        )

        assertEquals(WellnessFusionEngine.WellnessState.HIGH_STRESS, result.state)
        assertTrue("Wellness score should be low under high stress", result.wellnessScore < 50)
        assertTrue(result.vocalTensionScore < 40)
    }

    @Test
    fun testMildStrainState() {
        // Moderately elevated HR (79 BPM), slightly lower HRV (42ms), moderate voice stress (5.6)
        val result = engine.fuseSignals(
            heartRateBpm = 79.0,
            hrvMs = 42.0,
            voiceStress = 5.6,
            baseline = baseline
        )

        assertEquals(WellnessFusionEngine.WellnessState.MILD_STRAIN, result.state)
        assertTrue("Score should be in mild strain band", result.wellnessScore in 50..69)
    }

    @Test
    fun testFactorBreakdownCompleteness() {
        val result = engine.fuseSignals(
            heartRateBpm = 85.0,
            hrvMs = 30.0,
            voiceStress = 7.2,
            baseline = baseline
        )

        assertEquals(3, result.factors.size)
        assertTrue(result.factors.any { it.name == "Heart Rate Variability" })
        assertTrue(result.factors.any { it.name == "Resting Heart Rate" })
        assertTrue(result.factors.any { it.name == "Voice Arousal" })
    }
}
