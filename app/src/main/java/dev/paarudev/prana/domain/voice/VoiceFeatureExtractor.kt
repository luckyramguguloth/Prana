package dev.paarudev.prana.domain.voice

import kotlin.math.*

/**
 * Acoustic & Prosodic Voice Feature Extractor for Speech Stress Analysis.
 *
 * Extracts acoustic markers linked to autonomic arousal:
 * 1. Root-Mean-Square (RMS) energy & energy variance.
 * 2. Pitch / Fundamental Frequency (F0) tracking via normalized autocorrelation.
 * 3. Zero-Crossing Rate (ZCR) - indicator of spectral noisiness / glottal tension.
 * 4. Spectral Centroid - measure of high-frequency vocal harmonic energy.
 * 5. Composite Voice Stress Index (0.0 to 10.0 scale) and categorical classification.
 */
class VoiceFeatureExtractor(
    val sampleRateHz: Int = 16000,
    val windowSizeMs: Int = 30,
    val hopSizeMs: Int = 15
) {

    enum class StressCategory {
        CALM,
        NEUTRAL,
        STRESSED
    }

    data class VoiceFeatures(
        val meanRmsEnergy: Double,
        val energyVariance: Double,
        val meanPitchF0Hz: Double,
        val pitchJitterPercent: Double,
        val meanZeroCrossingRate: Double,
        val spectralCentroidHz: Double,
        val stressScore: Double, // 0.0 (very calm) to 10.0 (high acute stress)
        val category: StressCategory,
        val featureTensor: FloatArray // 16-dimensional feature vector for TFLite model
    )

    /**
     * Analyzes raw 16-bit PCM audio samples (normalized -1.0 to 1.0 or raw short values).
     */
    fun extractFeatures(pcmSamples: ShortArray): VoiceFeatures {
        if (pcmSamples.size < sampleRateHz * 0.5) {
            // Buffer too short (< 0.5s) - return neutral baseline
            return createDefaultFeatures()
        }

        val floatSamples = FloatArray(pcmSamples.size) { i ->
            pcmSamples[i].toFloat() / 32768.0f
        }
        return extractFeaturesFromFloat(floatSamples)
    }

    fun extractFeaturesFromFloat(samples: FloatArray): VoiceFeatures {
        val windowSamples = (sampleRateHz * windowSizeMs / 1000)
        val hopSamples = (sampleRateHz * hopSizeMs / 1000)
        val numFrames = max(1, (samples.size - windowSamples) / hopSamples)

        val frameRms = DoubleArray(numFrames)
        val frameZcr = DoubleArray(numFrames)
        val frameF0 = DoubleArray(numFrames)
        val frameCentroid = DoubleArray(numFrames)

        var voicedFramesCount = 0

        for (f in 0 until numFrames) {
            val start = f * hopSamples
            val end = min(samples.size, start + windowSamples)
            val frame = FloatArray(end - start) { idx -> samples[start + idx] }

            // 1. RMS Energy
            var sumSq = 0.0
            var zcrCount = 0
            for (i in frame.indices) {
                val s = frame[i].toDouble()
                sumSq += s * s
                if (i > 0 && ((frame[i] >= 0 && frame[i - 1] < 0) || (frame[i] < 0 && frame[i - 1] >= 0))) {
                    zcrCount++
                }
            }
            val rms = sqrt(sumSq / max(1, frame.size))
            frameRms[f] = rms
            frameZcr[f] = zcrCount.toDouble() / max(1, frame.size)

            // 2. Pitch / F0 via Autocorrelation (human vocal range 75Hz to 400Hz)
            if (rms > 0.015) { // Voiced speech threshold
                val pitch = estimateFrameF0(frame, sampleRateHz)
                if (pitch > 0.0) {
                    frameF0[voicedFramesCount] = pitch
                    voicedFramesCount++
                }
            }

            // 3. Spectral Centroid approximation
            frameCentroid[f] = calculateApproxSpectralCentroid(frame, sampleRateHz)
        }

        // Aggregate statistics
        val meanRms = frameRms.average()
        val energyVar = calculateVariance(frameRms, meanRms)
        val meanZcr = frameZcr.average()

        val validF0 = if (voicedFramesCount > 0) frameF0.sliceArray(0 until voicedFramesCount) else doubleArrayOf(140.0)
        val meanPitch = validF0.average()
        val pitchJitter = calculateJitter(validF0, meanPitch)
        val meanCentroid = frameCentroid.average()

        // Autonomic stress scoring heuristic derived from acoustic correlates:
        // Elevated pitch + high pitch jitter + high spectral centroid + high energy variance = acute tension
        var rawStress = 0.0

        // Pitch component (baseline ~120-160Hz, elevated > 210Hz)
        val pitchFactor = ((meanPitch - 110.0) / 140.0).coerceIn(0.0, 1.0)
        rawStress += pitchFactor * 3.5

        // Jitter / instability component
        val jitterFactor = (pitchJitter / 6.0).coerceIn(0.0, 1.0)
        rawStress += jitterFactor * 2.5

        // Spectral centroid (brightness / vocal strain, typical 800 - 3000 Hz)
        val centroidFactor = ((meanCentroid - 900.0) / 1800.0).coerceIn(0.0, 1.0)
        rawStress += centroidFactor * 2.5

        // Energy dynamics
        val energyFactor = (energyVar * 15.0).coerceIn(0.0, 1.0)
        rawStress += energyFactor * 1.5

        val stressScore = round(rawStress.coerceIn(1.0, 9.8) * 10.0) / 10.0

        val category = when {
            stressScore < 4.0 -> StressCategory.CALM
            stressScore < 6.8 -> StressCategory.NEUTRAL
            else -> StressCategory.STRESSED
        }

        // 16-element feature vector normalized for TFLite CNN-BiLSTM inference
        val tensor = FloatArray(16) { idx ->
            when (idx) {
                0 -> meanRms.toFloat()
                1 -> energyVar.toFloat()
                2 -> (meanPitch / 400.0).toFloat()
                3 -> (pitchJitter / 10.0).toFloat()
                4 -> meanZcr.toFloat()
                5 -> (meanCentroid / 4000.0).toFloat()
                6 -> (stressScore / 10.0).toFloat()
                in 7..15 -> ((frameRms.getOrNull(idx % max(1, frameRms.size)) ?: 0.0) * 2.0).toFloat()
                else -> 0.0f
            }
        }

        return VoiceFeatures(
            meanRmsEnergy = round(meanRms * 1000.0) / 1000.0,
            energyVariance = round(energyVar * 1000.0) / 1000.0,
            meanPitchF0Hz = round(meanPitch * 10.0) / 10.0,
            pitchJitterPercent = round(pitchJitter * 10.0) / 10.0,
            meanZeroCrossingRate = round(meanZcr * 1000.0) / 1000.0,
            spectralCentroidHz = round(meanCentroid * 10.0) / 10.0,
            stressScore = stressScore,
            category = category,
            featureTensor = tensor
        )
    }

    private fun estimateFrameF0(frame: FloatArray, fs: Int): Double {
        val minLag = fs / 400 // 400 Hz maximum F0
        val maxLag = fs / 75  // 75 Hz minimum F0
        if (frame.size < maxLag * 2) return 0.0

        var bestLag = -1
        var maxCorr = 0.0

        for (lag in minLag..maxLag) {
            var corr = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            for (i in 0 until frame.size - lag) {
                corr += frame[i] * frame[i + lag]
                norm1 += frame[i] * frame[i]
                norm2 += frame[i + lag] * frame[i + lag]
            }
            val norm = sqrt(norm1 * norm2)
            val normCorr = if (norm > 1e-6) corr / norm else 0.0

            if (normCorr > maxCorr && normCorr > 0.35) {
                maxCorr = normCorr
                bestLag = lag
            }
        }

        return if (bestLag > 0) fs.toDouble() / bestLag else 0.0
    }

    private fun calculateApproxSpectralCentroid(frame: FloatArray, fs: Int): Double {
        var num = 0.0
        var den = 0.0
        val n = frame.size
        for (i in 0 until n / 2) {
            val freq = (i.toDouble() * fs) / n
            val mag = abs(frame[i].toDouble())
            num += freq * mag
            den += mag
        }
        return if (den > 1e-6) num / den else 1200.0
    }

    private fun calculateVariance(values: DoubleArray, mean: Double): Double {
        if (values.size < 2) return 0.0
        var sum = 0.0
        for (v in values) {
            val d = v - mean
            sum += d * d
        }
        return sum / values.size
    }

    private fun calculateJitter(pitches: DoubleArray, meanPitch: Double): Double {
        if (pitches.size < 2 || meanPitch < 1e-3) return 1.5
        var sumDiff = 0.0
        for (i in 0 until pitches.size - 1) {
            sumDiff += abs(pitches[i + 1] - pitches[i])
        }
        val avgDiff = sumDiff / (pitches.size - 1)
        return (avgDiff / meanPitch) * 100.0
    }

    private fun createDefaultFeatures(): VoiceFeatures {
        return VoiceFeatures(
            meanRmsEnergy = 0.045,
            energyVariance = 0.002,
            meanPitchF0Hz = 135.0,
            pitchJitterPercent = 1.8,
            meanZeroCrossingRate = 0.08,
            spectralCentroidHz = 1450.0,
            stressScore = 4.2,
            category = StressCategory.NEUTRAL,
            featureTensor = FloatArray(16) { 0.2f }
        )
    }
}
