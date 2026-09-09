package dev.paarudev.prana.voice

import dev.paarudev.prana.domain.voice.VoiceFeatureExtractor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class VoiceFeatureExtractorTest {

    private lateinit var extractor: VoiceFeatureExtractor

    @Before
    fun setUp() {
        extractor = VoiceFeatureExtractor(sampleRateHz = 16000)
    }

    @Test
    fun testShortAudioFallback() {
        // Less than 0.5s audio should return safe default features
        val shortBuffer = ShortArray(100)
        val features = extractor.extractFeatures(shortBuffer)

        assertNotNull(features)
        assertEquals(VoiceFeatureExtractor.StressCategory.NEUTRAL, features.category)
        assertEquals(16, features.featureTensor.size)
    }

    @Test
    fun testLowPitchCalmAudioFeatures() {
        val fs = 16000
        val durationSec = 2.0
        val n = (fs * durationSec).toInt()
        val f0 = 120.0 // Low, calm vocal pitch

        val samples = FloatArray(n) { i ->
            val t = i.toDouble() / fs
            (0.3 * sin(2.0 * PI * f0 * t) + 0.1 * sin(2.0 * PI * 2 * f0 * t)).toFloat()
        }

        val features = extractor.extractFeaturesFromFloat(samples)

        assertNotNull(features)
        assertTrue("Pitch should be detected near 120Hz", features.meanPitchF0Hz in 90.0..160.0)
        assertTrue("Calm pitch should result in non-elevated stress", features.stressScore < 6.5)
        assertEquals(16, features.featureTensor.size)
    }

    @Test
    fun testHighPitchTenseAudioFeatures() {
        val fs = 16000
        val durationSec = 2.0
        val n = (fs * durationSec).toInt()
        val f0 = 280.0 // Elevated tense vocal pitch + higher harmonic brightness

        val samples = FloatArray(n) { i ->
            val t = i.toDouble() / fs
            (0.5 * sin(2.0 * PI * f0 * t) + 0.3 * sin(2.0 * PI * 3 * f0 * t)).toFloat()
        }

        val features = extractor.extractFeaturesFromFloat(samples)

        assertNotNull(features)
        assertTrue("High pitch audio stress score should be elevated", features.stressScore >= 5.0)
    }
}
