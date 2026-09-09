package dev.paarudev.prana.ui.settings

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paarudev.prana.ui.theme.*

@Composable
fun SettingsScreen(
    onExportEncryptedPdf: (passcode: String) -> Unit,
    onDeleteAllData: () -> Unit
) {
    val scrollState = rememberScrollState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exportPasscode by remember { mutableStateOf("") }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = "Privacy & Security",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 26.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "DPDP Act 2023 compliant · Zero-network architecture",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy Guarantee Badge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SageContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, SageSecondary)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SagePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "100% On-Device Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SagePrimary
                    )
                    Text(
                        text = "Zero network permissions in release manifest. No data ever leaves this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // DPDP Act 2023 Principles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "India DPDP Act (2023) Alignment",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                DpdpItem(
                    title = "Purpose Limitation",
                    desc = "Biometric pulse & voice processed exclusively for user-requested wellness awareness."
                )
                DpdpItem(
                    title = "Data Minimization",
                    desc = "Raw video & audio frames discarded immediately after on-device feature extraction."
                )
                DpdpItem(
                    title = "Right to Erasure",
                    desc = "One-tap complete wipe of the hardware-encrypted database with zero residual traces."
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hardware & ML Acceleration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "On-Device ML & Hardware Engines",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                EngineItem(name = "rPPG Optical Engine", tech = "CHROM Algorithm + INT8 Bandpass FFT", accel = "Snapdragon NPU")
                EngineItem(name = "Voice Stress Classifier", tech = "Distilled CNN-BiLSTM (Acoustic Prosody)", accel = "LiteRT / NNAPI")
                EngineItem(name = "Guidance Generator", tech = "Gemma-3 270M / Llama 3.2 1B (Local SLM)", accel = "LiteRT-LM Engine")
                EngineItem(name = "Persistence Encryption", tech = "SQLCipher + Android Keystore AES-256", accel = "Hardware TEE / StrongBox")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Data Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Data Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export as Encrypted PDF (AES-256)", color = Color.White)
                }

                if (exportSuccessMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportSuccessMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SagePrimary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedBrickAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MutedBrickAccent)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All My Data (Right to Erasure)")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Export Password Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Set Report Password", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text(
                        text = "Your clinical PDF report will be encrypted with AES-256-GCM using this passcode before export.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exportPasscode,
                        onValueChange = { exportPasscode = it },
                        label = { Text("Passcode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportPasscode.isNotBlank()) {
                            onExportEncryptedPdf(exportPasscode)
                            exportSuccessMessage = "Report encrypted successfully with AES-256-GCM and saved to secure cache."
                            showExportDialog = false
                            exportPasscode = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                ) {
                    Text("Encrypt & Export", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete All Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Data Erasure", style = MaterialTheme.typography.titleMedium, color = MutedBrickAccent) },
            text = {
                Text(
                    text = "This will immediately wipe all recorded check-ins, waveforms, and trends from the encrypted SQLCipher database on this phone. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedBrickAccent)
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun DpdpItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SagePrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun EngineItem(name: String, tech: String, accel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            Text(text = tech, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = TextSecondary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceSubtle)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = accel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SagePrimary)
        }
    }
}
