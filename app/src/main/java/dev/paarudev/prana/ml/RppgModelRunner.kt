package dev.paarudev.prana.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * LiteRT (TensorFlow Lite) Runner for rPPG Signal Quality & Motion Artifact Rejection.
 * Leverages Qualcomm Snapdragon NPU via NNAPI delegate with CPU fallback.
 */
class RppgModelRunner(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null
    var isNpuAccelerated: Boolean = false
        private set

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        try {
            val modelBuffer = loadModelFile("models/rppg_quality.tflite")
            val options = Interpreter.Options()

            try {
                // Attempt hardware NPU acceleration via NNAPI
                val delegate = NnApiDelegate()
                options.addDelegate(delegate)
                nnApiDelegate = delegate
                isNpuAccelerated = true
            } catch (e: Exception) {
                isNpuAccelerated = false
                options.setNumThreads(4)
            }

            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            // Model file not found or synthetic mode
            interpreter = null
        }
    }

    data class QualityEvaluation(
        val isUsable: Boolean,
        val qualityScore: Float,
        val motionArtifactDetected: Boolean
    )

    fun evaluateFrameQuality(greenChannelVariance: Float, snrEstimate: Float): QualityEvaluation {
        val interp = interpreter
        if (interp != null) {
            try {
                val input = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
                input.putFloat(greenChannelVariance)
                input.putFloat(snrEstimate)
                input.rewind()

                val output = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
                interp.run(input, output)
                output.rewind()

                val usableProb = output.float
                val motionProb = output.float

                return QualityEvaluation(
                    isUsable = usableProb >= 0.5f,
                    qualityScore = usableProb,
                    motionArtifactDetected = motionProb >= 0.5f
                )
            } catch (e: Exception) {
                // Fallback to algorithmic heuristic
            }
        }

        // Robust algorithmic fallback
        val isMotion = greenChannelVariance > 45.0f || snrEstimate < 1.5f
        val quality = (snrEstimate / 10.0f).coerceIn(0.0f, 1.0f)
        return QualityEvaluation(
            isUsable = !isMotion && snrEstimate >= 1.8f,
            qualityScore = quality,
            motionArtifactDetected = isMotion
        )
    }

    private fun loadModelFile(assetPath: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(assetPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter?.close()
        nnApiDelegate?.close()
    }
}
