package dev.paarudev.prana

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.paarudev.prana.data.db.WellnessEntry
import dev.paarudev.prana.data.export.EncryptedPdfExporter
import dev.paarudev.prana.domain.fusion.WellnessFusionEngine
import dev.paarudev.prana.domain.insight.InsightGenerator
import dev.paarudev.prana.ml.LlmRunner
import dev.paarudev.prana.ui.checkin.CheckInScreen
import dev.paarudev.prana.ui.insight.InsightScreen
import dev.paarudev.prana.ui.settings.SettingsScreen
import dev.paarudev.prana.ui.theme.*
import dev.paarudev.prana.ui.trend.TrendScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val fusionEngine = WellnessFusionEngine()
    private val insightGenerator = InsightGenerator()
    private lateinit var llmRunner: LlmRunner
    private lateinit var pdfExporter: EncryptedPdfExporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmRunner = LlmRunner(this)
        pdfExporter = EncryptedPdfExporter(this)

        val app = application as PranaApp
        val dao = app.database.wellnessDao()

        setContent {
            PranaTheme {
                var selectedTab by remember { mutableIntStateOf(0) }

                // Check-in dynamic state
                var currentHr by remember { mutableDoubleStateOf(72.0) }
                var currentHrv by remember { mutableDoubleStateOf(52.0) }
                var currentVoiceStress by remember { mutableDoubleStateOf(3.8) }
                var currentInsightText by remember { mutableStateOf("") }
                var currentFusedResult by remember {
                    mutableStateOf(fusionEngine.fuseSignals(72.0, 52.0, 3.8))
                }

                // Database state
                val entriesFlow = remember { dao.getAllEntries() }
                val entriesList by entriesFlow.collectAsState(initial = emptyList())

                var avgHr by remember { mutableDoubleStateOf(72.0) }
                var avgHrv by remember { mutableDoubleStateOf(50.0) }
                var avgStress by remember { mutableDoubleStateOf(4.0) }

                LaunchedEffect(entriesList) {
                    withContext(Dispatchers.IO) {
                        avgHr = dao.getAverageHeartRate() ?: 72.0
                        avgHrv = dao.getAverageHrv() ?: 50.0
                        avgStress = dao.getAverageVoiceStress() ?: 4.0
                    }
                }

                // Runtime Permissions Management
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var hasAudioPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
                    hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
                }

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission || !hasAudioPermission) {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceCard,
                            contentColor = TextPrimary,
                            tonalElevation = 6.dp
                        ) {
                            NavBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = Icons.Default.Favorite,
                                label = "Check-in"
                            )
                            NavBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = Icons.Default.Lightbulb,
                                label = "Insight"
                            )
                            NavBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = Icons.Default.Timeline,
                                label = "7-Day Trend"
                            )
                            NavBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = Icons.Default.Security,
                                label = "Privacy"
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (selectedTab) {
                            0 -> {
                                CheckInScreen(
                                    onCheckInComplete = { hr, hrv, stress ->
                                        currentHr = hr
                                        currentHrv = hrv
                                        currentVoiceStress = stress

                                        val baseline = WellnessFusionEngine.Baseline(
                                            avgHeartRateBpm = avgHr,
                                            avgHrvMs = avgHrv,
                                            avgVoiceStress = avgStress
                                        )

                                        val fused = fusionEngine.fuseSignals(hr, hrv, stress, baseline)
                                        currentFusedResult = fused

                                        currentInsightText = insightGenerator.generateInsight(
                                            hr = hr,
                                            hrv = hrv,
                                            voiceStress = stress,
                                            state = fused.state,
                                            baseline = baseline,
                                            llmProvider = llmRunner
                                        )

                                        selectedTab = 1 // Navigate to insight
                                    }
                                )
                            }
                            1 -> {
                                InsightScreen(
                                    heartRateBpm = currentHr,
                                    hrvMs = currentHrv,
                                    voiceStress = currentVoiceStress,
                                    insightText = if (currentInsightText.isBlank()) {
                                        "Your pulse and vocal markers are closely aligned with your weekly baseline. You are holding a steady, sustainable rhythm today."
                                    } else currentInsightText,
                                    fusedResult = currentFusedResult,
                                    onSaveToTrend = {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            dao.insertEntry(
                                                WellnessEntry(
                                                    heartRateBpm = currentHr,
                                                    hrvRmssdMs = currentHrv,
                                                    voiceStressScore = currentVoiceStress,
                                                    wellnessScore = currentFusedResult.wellnessScore,
                                                    wellnessState = currentFusedResult.state.displayName,
                                                    insightText = currentInsightText
                                                )
                                            )
                                        }
                                        Toast.makeText(this@MainActivity, "Saved to secure database", Toast.LENGTH_SHORT).show()
                                    },
                                    onNavigateToTrend = {
                                        selectedTab = 2
                                    }
                                )
                            }
                            2 -> {
                                TrendScreen(
                                    entries = entriesList,
                                    avgHr = avgHr,
                                    avgHrv = avgHrv,
                                    avgStress = avgStress,
                                    onStartCheckIn = { selectedTab = 0 }
                                )
                            }
                            3 -> {
                                SettingsScreen(
                                    onExportEncryptedPdf = { passcode ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val recent = dao.getRecentEntries(14)
                                            val export = pdfExporter.generateAndEncryptReport(recent, passcode)
                                            withContext(Dispatchers.Main) {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/octet-stream"
                                                    putExtra(Intent.EXTRA_STREAM, export.contentUri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                startActivity(Intent.createChooser(shareIntent, "Share Encrypted Wellness Report"))
                                            }
                                        }
                                    },
                                    onDeleteAllData = {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            dao.deleteAllEntries()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "All data wiped cleanly (DPDP Act)", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = SagePrimary,
            selectedTextColor = SagePrimary,
            indicatorColor = SageContainer,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
        )
    )
}
