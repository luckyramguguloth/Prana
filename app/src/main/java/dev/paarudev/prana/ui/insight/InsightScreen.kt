package dev.paarudev.prana.ui.insight

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paarudev.prana.domain.fusion.WellnessFusionEngine
import dev.paarudev.prana.ui.theme.*

@Composable
fun InsightScreen(
    heartRateBpm: Double,
    hrvMs: Double,
    voiceStress: Double,
    insightText: String,
    fusedResult: WellnessFusionEngine.FusedResult,
    onSaveToTrend: () -> Unit,
    onNavigateToTrend: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Smooth score resolution animation
    var animationTriggered by remember { mutableStateOf(false) }
    val animatedScore by animateIntAsState(
        targetValue = if (animationTriggered) fusedResult.wellnessScore else 0,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "scoreAnim"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Today's Insight",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Multimodal signals fused on-device",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            if (fusedResult.wellnessScore >= 70) SageContainer else AccentContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animatedScore",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (fusedResult.wellnessScore >= 70) SagePrimary else MutedBrickAccent
                        )
                        Text(
                            text = "/ 100",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = fusedResult.state.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fusedResult.state.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Three Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricPill(
                title = "Pulse",
                value = "${heartRateBpm.toInt()}",
                unit = "BPM",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = "HRV (RMSSD)",
                value = "${hrvMs.toInt()}",
                unit = "ms",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = "Voice Stress",
                value = "$voiceStress",
                unit = "/10",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // On-Device SLM Generated Guidance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Guidance",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SageContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SagePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Guardrail Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SagePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = insightText,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Generated by on-device SLM · Non-diagnostic wellness advice only",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contributing Factors Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Contributing Signal Factors",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                for (f in fusedResult.factors) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SageSecondary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = f.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = f.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = {
                onSaveToTrend()
                onNavigateToTrend()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & View 7-Day Trend", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MetricPill(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}
