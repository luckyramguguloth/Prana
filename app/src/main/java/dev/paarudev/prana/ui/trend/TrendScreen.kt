package dev.paarudev.prana.ui.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paarudev.prana.data.db.WellnessEntry
import dev.paarudev.prana.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrendScreen(
    entries: List<WellnessEntry>,
    avgHr: Double,
    avgHrv: Double,
    avgStress: Double,
    onStartCheckIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "7-Day Trajectory",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 26.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Longitudinal recovery & autonomic baseline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onStartCheckIn,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SagePrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Check-in", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Baseline Metrics Strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BaselineStatItem("Resting Pulse", "${avgHr.toInt()}", "bpm")
                Divider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = SurfaceBorder
                )
                BaselineStatItem("Autonomic HRV", "${avgHrv.toInt()}", "ms")
                Divider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = SurfaceBorder
                )
                BaselineStatItem("Voice Stress", String.format(Locale.US, "%.1f", avgStress), "/10")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Interactive 7-Day Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Wellness Scores",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Last 7 entries",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bar Chart Canvas
                WellnessBarChart(
                    entries = entries.take(7).reversed(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Recorded Check-in History",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // History Entries List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { item ->
                HistoryItemCard(entry = item)
            }
        }
    }
}

@Composable
fun BaselineStatItem(title: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SagePrimary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = unit, fontSize = 10.sp, color = TextTertiary)
        }
    }
}

@Composable
fun WellnessBarChart(
    entries: List<WellnessEntry>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = maxOf(1, entries.size)
        val barWidth = (size.width / (count * 2.2f))
        val maxScore = 100f

        entries.forEachIndexed { index, entry ->
            val x = (index * (size.width / count)) + (barWidth * 0.6f)
            val barHeight = (entry.wellnessScore.toFloat() / maxScore) * (size.height * 0.75f)
            val y = size.height - barHeight

            val barColor = if (entry.wellnessScore >= 75) SagePrimary else if (entry.wellnessScore >= 55) SageSecondary else MutedBrickAccent

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }
    }
}

@Composable
fun HistoryItemCard(entry: WellnessEntry) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(entry.timestampMs))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.wellnessState,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pulse: ${entry.heartRateBpm.toInt()} bpm · HRV: ${entry.hrvRmssdMs.toInt()} ms · Voice: ${entry.voiceStressScore}/10",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.wellnessScore >= 70) SageContainer else AccentContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entry.wellnessScore}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (entry.wellnessScore >= 70) SagePrimary else MutedBrickAccent
                )
            }
        }
    }
}
