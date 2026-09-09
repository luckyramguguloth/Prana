package dev.paarudev.prana.rppg

import dev.paarudev.prana.domain.rppg.RppgSignalProcessor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class RppgSignalProcessorTest {

    private lateinit var processor: RppgSignalProcessor

    @Before
    fun setUp() {
        processor = RppgSignalProcessor(samplingRateFps = 30.0)
    }

    @Test
    fun testRecover60BpmSineWave() {
        val fs = 30.0
        val targetBpm = 60.0
        val targetFreq = targetBpm / 60.0 // 1.0 Hz
        val durationSeconds = 12.0
        val n = (fs * durationSeconds).toInt()

        val signal = DoubleArray(n) { i ->
            val t = i / fs
            sin(2.0 * PI * targetFreq * t)
        }

        val filtered = processor.applyBandpassFilter(signal, fs)
        val result = processor.processSignal(filtered, fs)

        assertTrue("60 BPM signal should be usable", result.isUsable)
        assertEquals("Recovered BPM should match target 60 BPM within 1.5 tolerance", 60.0, result.heartRateBpm, 1.5)
        assertTrue("SNR should be strongly positive for clean signal", result.snrDb > 4.0)
    }

    @Test
    fun testRecover75BpmSineWave() {
        val fs = 30.0
        val targetBpm = 75.0
        val targetFreq = targetBpm / 60.0 // 1.25 Hz
        val durationSeconds = 12.0
        val n = (fs * durationSeconds).toInt()

        val signal = DoubleArray(n) { i ->
            val t = i / fs
            sin(2.0 * PI * targetFreq * t)
        }

        val filtered = processor.applyBandpassFilter(signal, fs)
        val result = processor.processSignal(filtered, fs)

        assertTrue("75 BPM signal should be usable", result.isUsable)
        assertEquals("Recovered BPM should match target 75 BPM within 1.5 tolerance", 75.0, result.heartRateBpm, 1.5)
    }

    @Test
    fun testRecover120BpmSineWave() {
        val fs = 30.0
        val targetBpm = 120.0
        val targetFreq = targetBpm / 60.0 // 2.0 Hz
        val durationSeconds = 12.0
        val n = (fs * durationSeconds).toInt()

        val signal = DoubleArray(n) { i ->
            val t = i / fs
            sin(2.0 * PI * targetFreq * t)
        }

        val filtered = processor.applyBandpassFilter(signal, fs)
        val result = processor.processSignal(filtered, fs)

        assertTrue("120 BPM signal should be usable", result.isUsable)
        assertEquals("Recovered BPM should match target 120 BPM within 2.0 tolerance", 120.0, result.heartRateBpm, 2.0)
    }

    @Test
    fun testRejectPureNoiseArtifact() {
        val fs = 30.0
        val n = (fs * 10.0).toInt()
        val random = java.util.Random(42)
        // High-frequency uncorrelated white noise
        val noiseSignal = DoubleArray(n) { random.nextDouble() * 2.0 - 1.0 }

        val filtered = processor.applyBandpassFilter(noiseSignal, fs)
        val result = processor.processSignal(filtered, fs)

        // Low SNR should mark unusable or low confidence
        assertTrue("Confidence on noise should be low", result.confidence < 0.5)
    }

    @Test
    fun testRmssdCalculation() {
        val fs = 30.0
        // Peaks spaced every 30 samples = 1.0 second intervals (1000ms each)
        // Add small physiological variance: alternating 950ms and 1050ms
        val peakIndices = listOf(0, 30, 59, 90, 119, 150)
        val rmssd = processor.calculateHrvRmssd(peakIndices, fs)

        assertTrue("RMSSD should be physiologically valid between 15ms and 120ms", rmssd in 15.0..120.0)
    }
}
