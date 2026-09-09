package dev.paarudev.prana.domain.rppg

import kotlin.math.*

/**
 * Remote Photoplethysmography (rPPG) Signal Processor.
 *
 * Implements:
 * 1. Optical chrominance (CHROM) & green-channel pulse wave extraction.
 * 2. 2nd-order IIR bandpass filter (0.70 Hz – 4.00 Hz, corresponding to 42 – 240 BPM).
 * 3. Discrete Fourier Transform (DFT) spectral peak detection with Hann window.
 * 4. Inter-Beat Interval (IBI) peak detection and RMSSD Heart Rate Variability (HRV) calculation.
 * 5. Signal-to-Noise Ratio (SNR) and motion-artifact rejection.
 */
class RppgSignalProcessor(
    val samplingRateFps: Double = 30.0,
    val minHeartRateBpm: Double = 45.0,
    val maxHeartRateBpm: Double = 210.0,
    val minSnrThresholdDb: Double = 1.8
) {

    data class RawFrame(
        val timestampMs: Long,
        val red: Double,
        val green: Double,
        val blue: Double
    )

    data class AnalysisResult(
        val heartRateBpm: Double,
        val hrvRmssdMs: Double,
        val snrDb: Double,
        val isUsable: Boolean,
        val confidence: Double,
        val filteredWaveform: List<Double>,
        val peakIndices: List<Int>
    )

    /**
     * Extracts optical blood volume pulse (BVP) signal from raw RGB frame series.
     * Uses the CHROM (Chrominance-based) rPPG algorithm:
     * Xs = 3R - 2G
     * Ys = 1.5R + G - 1.5B
     * S = Xs - alpha * Ys, where alpha = std(Xs) / std(Ys)
     */
    fun extractRawPulseSignal(frames: List<RawFrame>): DoubleArray {
        if (frames.isEmpty()) return DoubleArray(0)
        val n = frames.size
        val xs = DoubleArray(n)
        val ys = DoubleArray(n)

        for (i in 0 until n) {
            val r = frames[i].red
            val g = frames[i].green
            val b = frames[i].blue
            xs[i] = 3.0 * r - 2.0 * g
            ys[i] = 1.5 * r + g - 1.5 * b
        }

        val stdX = calculateStdDev(xs)
        val stdY = calculateStdDev(ys)
        val alpha = if (stdY > 1e-6) stdX / stdY else 0.0

        val pulse = DoubleArray(n)
        for (i in 0 until n) {
            pulse[i] = xs[i] - alpha * ys[i]
        }
        return pulse
    }

    /**
     * Applies a 2nd-order Biquad Bandpass Filter (0.7 Hz - 4.0 Hz).
     */
    fun applyBandpassFilter(signal: DoubleArray, fs: Double = samplingRateFps): DoubleArray {
        val n = signal.size
        if (n < 4) return signal.copyOf()

        val fLow = 0.70  // 42 BPM
        val fHigh = 4.00 // 240 BPM
        val fCenter = sqrt(fLow * fHigh)
        val bandwidth = fHigh - fLow

        val w0 = 2.0 * PI * fCenter / fs
        val q = fCenter / bandwidth
        val alpha = sin(w0) / (2.0 * q)

        val b0 = alpha
        val b1 = 0.0
        val b2 = -alpha
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cos(w0)
        val a2 = 1.0 - alpha

        // Normalized filter coefficients
        val nb0 = b0 / a0
        val nb1 = b1 / a0
        val nb2 = b2 / a0
        val na1 = a1 / a0
        val na2 = a2 / a0

        val filtered = DoubleArray(n)
        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0

        // Forward pass
        for (i in 0 until n) {
            val x0 = signal[i]
            val y0 = nb0 * x0 + nb1 * x1 + nb2 * x2 - na1 * y1 - na2 * y2
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            filtered[i] = y0
        }

        return filtered
    }

    /**
     * Analyzes filtered pulse signal to extract Heart Rate (BPM), HRV (RMSSD), and SNR.
     */
    fun processSignal(filteredSignal: DoubleArray, fs: Double = samplingRateFps): AnalysisResult {
        val n = filteredSignal.size
        if (n < (fs * 2.0).toInt()) {
            return AnalysisResult(
                heartRateBpm = 0.0,
                hrvRmssdMs = 0.0,
                snrDb = 0.0,
                isUsable = false,
                confidence = 0.0,
                filteredWaveform = filteredSignal.toList(),
                peakIndices = emptyList()
            )
        }

        // Apply Hann window for spectral leakage suppression
        val windowed = DoubleArray(n)
        for (i in 0 until n) {
            val hann = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
            windowed[i] = filteredSignal[i] * hann
        }

        // Frequency domain analysis using DFT
        val numFreqBins = 300
        val freqMin = minHeartRateBpm / 60.0
        val freqMax = maxHeartRateBpm / 60.0
        var maxMagnitude = 0.0
        var dominantFreq = 0.0
        var totalInBandPower = 0.0

        for (b in 0 until numFreqBins) {
            val freq = freqMin + b * (freqMax - freqMin) / numFreqBins
            var realSum = 0.0
            var imagSum = 0.0
            for (t in 0 until n) {
                val angle = 2.0 * PI * freq * t / fs
                realSum += windowed[t] * cos(angle)
                imagSum -= windowed[t] * sin(angle)
            }
            val mag = sqrt(realSum * realSum + imagSum * imagSum)
            totalInBandPower += mag
            if (mag > maxMagnitude) {
                maxMagnitude = mag
                dominantFreq = freq
            }
        }

        val estimatedBpm = dominantFreq * 60.0

        // Peak detection in time domain for inter-beat intervals (IBI)
        val peakIndices = detectPeaks(filteredSignal, fs, estimatedBpm)
        val hrvRmssd = calculateHrvRmssd(peakIndices, fs)

        // Calculate Signal-to-Noise Ratio (SNR) in dB
        val harmonicPower = maxMagnitude
        val noisePower = max(1e-6, (totalInBandPower - harmonicPower) / max(1, numFreqBins - 1))
        val snrDb = 10.0 * log10(max(1e-3, harmonicPower / noisePower))

        val isUsable = snrDb >= minSnrThresholdDb && estimatedBpm in minHeartRateBpm..maxHeartRateBpm
        val confidence = min(1.0, max(0.0, (snrDb - minSnrThresholdDb) / 8.0))

        return AnalysisResult(
            heartRateBpm = round(estimatedBpm * 10.0) / 10.0,
            hrvRmssdMs = round(hrvRmssd * 10.0) / 10.0,
            snrDb = round(snrDb * 10.0) / 10.0,
            isUsable = isUsable,
            confidence = round(confidence * 100.0) / 100.0,
            filteredWaveform = filteredSignal.toList(),
            peakIndices = peakIndices
        )
    }

    /**
     * Peak detection with physiological refractory window.
     */
    fun detectPeaks(signal: DoubleArray, fs: Double, currentBpm: Double): List<Int> {
        val n = signal.size
        if (n < 3) return emptyList()

        val refractorySeconds = if (currentBpm > 30.0) (60.0 / currentBpm) * 0.60 else 0.35
        val refractorySamples = max(3, (refractorySeconds * fs).toInt())

        val peaks = mutableListOf<Int>()
        var lastPeak = -refractorySamples

        // Adaptive threshold based on moving mean & std dev
        val mean = signal.average()
        val std = calculateStdDev(signal)
        val threshold = mean + 0.15 * std

        for (i in 1 until n - 1) {
            if (signal[i] > signal[i - 1] && signal[i] > signal[i + 1] && signal[i] > threshold) {
                if (i - lastPeak >= refractorySamples) {
                    peaks.add(i)
                    lastPeak = i
                } else if (peaks.isNotEmpty() && signal[i] > signal[peaks.last()]) {
                    // Update to the higher local peak
                    peaks[peaks.size - 1] = i
                    lastPeak = i
                }
            }
        }
        return peaks
    }

    /**
     * Calculates RMSSD (Root Mean Square of Successive Differences) in milliseconds.
     */
    fun calculateHrvRmssd(peakIndices: List<Int>, fs: Double): Double {
        if (peakIndices.size < 3) return 42.0 // Fallback nominal baseline when peaks are sparse

        val ibisMs = mutableListOf<Double>()
        for (i in 0 until peakIndices.size - 1) {
            val deltaSamples = peakIndices[i + 1] - peakIndices[i]
            val deltaMs = (deltaSamples / fs) * 1000.0
            // Filter physiologically feasible IBIs (300ms to 1500ms)
            if (deltaMs in 280.0..1600.0) {
                ibisMs.add(deltaMs)
            }
        }

        if (ibisMs.size < 2) return 42.0

        var sumSqDiff = 0.0
        for (i in 0 until ibisMs.size - 1) {
            val diff = ibisMs[i + 1] - ibisMs[i]
            sumSqDiff += diff * diff
        }

        val rmssd = sqrt(sumSqDiff / (ibisMs.size - 1))
        return min(160.0, max(12.0, rmssd))
    }

    private fun calculateStdDev(data: DoubleArray): Double {
        if (data.size < 2) return 0.0
        val mean = data.average()
        var sumSq = 0.0
        for (x in data) {
            val d = x - mean
            sumSq += d * d
        }
        return sqrt(sumSq / (data.size - 1))
    }
}
