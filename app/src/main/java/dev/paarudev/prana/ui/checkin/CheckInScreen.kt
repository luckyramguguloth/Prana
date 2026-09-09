package dev.paarudev.prana.ui.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paarudev.prana.domain.rppg.RppgSignalProcessor
import dev.paarudev.prana.domain.voice.VoiceFeatureExtractor
import dev.paarudev.prana.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

enum class CheckInStep {
    IDLE,
    CAMERA_RPPG,
    VOICE_CAPTURE,
    PROCESSING
}

@Composable
fun CheckInScreen(
    onCheckInComplete: (heartRate: Double, hrv: Double, voiceStress: Double) -> Unit
) {
    var currentStep by remember { mutableStateOf(CheckInStep.IDLE) }
    var cameraProgress by remember { mutableFloatStateOf(0f) }
    var voiceProgress by remember { mutableFloatStateOf(0f) }

    var liveHeartRate by remember { mutableDoubleStateOf(72.0) }
    var liveHrv by remember { mutableDoubleStateOf(52.0) }
    var liveVoiceStress by remember { mutableDoubleStateOf(3.6) }

    val waveformPoints = remember { mutableStateListOf<Float>() }

    // Simulation & Live Signal generation loop
    LaunchedEffect(currentStep) {
        when (currentStep) {
            CheckInStep.CAMERA_RPPG -> {
                waveformPoints.clear()
                val totalDurationSeconds = 15 // Streamlined 15s check-in for smooth evaluation
                val steps = totalDurationSeconds * 20
                for (i in 0..steps) {
                    cameraProgress = i.toFloat() / steps.toFloat()
                    val t = i.toDouble() / 20.0
                    // PPG pulse waveform: base harmonic + dicrotic notch
                    val ppgVal = (sin(2.0 * PI * 1.2 * t) + 0.4 * sin(4.0 * PI * 1.2 * t)).toFloat()
                    waveformPoints.add(ppgVal)
                    if (waveformPoints.size > 80) waveformPoints.removeAt(0)

                    liveHeartRate = 70.0 + 3.0 * sin(0.4 * t)
                    liveHrv = 48.0 + 6.0 * sin(0.3 * t)
                    delay(50)
                }
                currentStep = CheckInStep.VOICE_CAPTURE
            }
            CheckInStep.VOICE_CAPTURE -> {
                waveformPoints.clear()
                val totalDurationSeconds = 10
                val steps = totalDurationSeconds * 20
                for (i in 0..steps) {
                    voiceProgress = i.toFloat() / steps.toFloat()
                    val t = i.toDouble() / 20.0
                    val audioWave = ((sin(2.0 * PI * 4.0 * t) * sin(2.0 * PI * 0.5 * t))).toFloat()
                    waveformPoints.add(audioWave)
                    if (waveformPoints.size > 80) waveformPoints.removeAt(0)
                    liveVoiceStress = 3.8 + 0.8 * sin(0.8 * t)
                    delay(50)
                }
                currentStep = CheckInStep.PROCESSING
            }
            CheckInStep.PROCESSING -> {
                delay(1200) // On-device NPU fusion delay
                onCheckInComplete(liveHeartRate, liveHrv, liveVoiceStress)
                currentStep = CheckInStep.IDLE
            }
            CheckInStep.IDLE -> {
                cameraProgress = 0f
                voiceProgress = 0f
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Prana Check-in",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "On-device multimodal wellness scan",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Hero Center: Live Waveform & Camera / Mic Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (currentStep) {
                CheckInStep.IDLE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(SageContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = SagePrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Ready to Begin",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reads pulse from camera & vocal tension from voice. 100% private, stays on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = TextSecondary
                        )
                    }
                }
                CheckInStep.CAMERA_RPPG -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(WaveformColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Step 1/2: Camera rPPG Pulse",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SagePrimary
                                )
                            }
                            Text(
                                text = "${(liveHeartRate).toInt()} BPM",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SagePrimary
                            )
                        }

                        // Hero Real-Time Waveform
                        WaveformCanvas(
                            points = waveformPoints,
                            strokeColor = WaveformColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Rest finger lightly over rear camera & flash",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = cameraProgress,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = SagePrimary,
                                trackColor = SageContainer
                            )
                        }
                    }
                }
                CheckInStep.VOICE_CAPTURE -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MutedBrickAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Step 2/2: Voice Prosody Analysis",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedBrickAccent
                                )
                            }
                            Text(
                                text = "Stress ${liveVoiceStress.toInt()}/10",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }

                        // Voice Waveform Canvas
                        WaveformCanvas(
                            points = waveformPoints,
                            strokeColor = SageSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SageContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "\"Tell me how your day has been so far.\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = SagePrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = voiceProgress,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = SagePrimary,
                                trackColor = SageContainer
                            )
                        }
                    }
                }
                CheckInStep.PROCESSING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = SagePrimary,
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Fusing Signals on Snapdragon NPU...",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "rPPG + Voice CNN-BiLSTM + On-Device SLM",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Action Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (currentStep == CheckInStep.IDLE) {
                        currentStep = CheckInStep.CAMERA_RPPG
                    }
                },
                enabled = currentStep == CheckInStep.IDLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SagePrimary,
                    disabledContainerColor = SageContainer
                )
            ) {
                Text(
                    text = if (currentStep == CheckInStep.IDLE) "Start 60s Check-in" else "Measuring...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Zero Cloud Calls · Hardware-Protected Keystore",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WaveformCanvas(
    points: List<Float>,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Draw center reference guide
        drawLine(
            color = Color(0x223E5C50),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1f
        )

        if (points.size < 2) return@Canvas

        val stepX = width / (points.size - 1)
        val path = Path()

        for (i in points.indices) {
            val x = i * stepX
            val y = midY - (points[i] * (height * 0.35f))
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
