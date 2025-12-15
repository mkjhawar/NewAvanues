/**
 * LearnAppActivity.kt - Main activity for LearnApp standalone app
 *
 * Provides UI for manual app exploration and JIT service coordination.
 * Includes safety indicators, exploration statistics, and AVU export.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Updated: 2025-12-11 (Phase 3: Safety indicators)
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * ## Features:
 *
 * - App selection for exploration
 * - Start/stop exploration controls
 * - JIT statistics display (screens learned, elements discovered)
 * - Safety indicators (DNC count, dynamic regions, login detection)
 * - Exploration phase tracking
 * - AVU export functionality
 * - Pause/resume JIT capture buttons
 * - AIDL binding to JIT service
 *
 * ## Architecture:
 *
 * ```
 * LearnAppActivity
 * ├─ Compose UI (Material 3)
 * │   ├─ JIT Status Card
 * │   ├─ Safety Indicators Card
 * │   ├─ Exploration Controls Card
 * │   └─ Export Card
 * ├─ ServiceConnection (AIDL binding to JIT)
 * ├─ ExplorationState (state management)
 * ├─ SafetyManager (safety coordination)
 * └─ AVUExporter (file generation)
 * ```
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
 */

package com.augmentalis.learnapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.jitlearning.IElementCaptureService
import com.augmentalis.jitlearning.JITState
import com.augmentalis.learnappcore.exploration.*
import com.augmentalis.learnappcore.export.*
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.safety.*

// Ocean Blue XR Theme Colors (from UI Guidelines V2)
private object OceanTheme {
    // Primary Colors
    val Primary = Color(0xFF3B82F6)
    val PrimaryDark = Color(0xFF60A5FA)
    val PrimaryContainer = Color(0xFFDBEAFE)

    // Secondary
    val Secondary = Color(0xFF06B6D4)

    // Semantic
    val Success = Color(0xFF10B981)
    val Error = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)

    // Surface (Glassmorphic)
    val Surface = Color(0xFFF0F9FF)
    val SurfaceVariant = Color(0xFFE0F2FE)
    val GlassSurface = Color(0x14FFFFFF)
    val GlassBorder = Color(0x26FFFFFF)

    // Status
    val StatusActive = Color(0xFF10B981)
    val StatusPaused = Color(0xFFF59E0B)
    val StatusIdle = Color(0xFF6B7280)
    val StatusExploring = Color(0xFF3B82F6)
    val StatusError = Color(0xFFEF4444)
    val StatusCompleted = Color(0xFF10B981)
}

/**
 * UI state for exploration
 */
data class ExplorationUiState(
    val phase: ExplorationPhase = ExplorationPhase.IDLE,
    val screensExplored: Int = 0,
    val elementsDiscovered: Int = 0,
    val elementsClicked: Int = 0,
    val coverage: Float = 0f,
    val dangerousElementsSkipped: Int = 0,
    val dynamicRegionsDetected: Int = 0,
    val menusDiscovered: Int = 0,
    val isOnLoginScreen: Boolean = false,
    val loginType: String = "",
    val lastExportPath: String? = null
)

/**
 * LearnApp Main Activity
 *
 * Launcher activity providing UI for app exploration and JIT coordination.
 * Includes safety indicators and AVU export functionality.
 */
class LearnAppActivity : ComponentActivity(), SafetyCallback {

    companion object {
        private const val TAG = "LearnAppActivity"
        private const val DEFAULT_PACKAGE = "com.example.app"
        private const val DEFAULT_APP_NAME = "Example App"
    }

    // AIDL service binding
    private var jitService: IElementCaptureService? = null
    private var isBound = false

    // UI state
    private val jitState = mutableStateOf<JITState?>(null)
    private val explorationUiState = mutableStateOf(ExplorationUiState())

    // Exploration components
    private lateinit var explorationState: ExplorationState
    private lateinit var safetyManager: SafetyManager
    private lateinit var avuExporter: AVUExporter

    /**
     * Service connection for JIT service binding
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "Connected to JIT service")
            jitService = IElementCaptureService.Stub.asInterface(service)
            isBound = true
            updateJITState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Disconnected from JIT service")
            jitService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "LearnApp activity created")

        // Initialize exploration components
        initializeComponents()

        // Bind to JIT service
        bindToJITService()

        // Set Compose UI with Ocean Blue XR Theme
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = OceanTheme.Primary,
                    onPrimary = Color.White,
                    primaryContainer = OceanTheme.PrimaryContainer,
                    secondary = OceanTheme.Secondary,
                    surface = OceanTheme.Surface,
                    surfaceVariant = OceanTheme.SurfaceVariant,
                    error = OceanTheme.Error
                )
            ) {
                LearnAppUI(
                    jitState = jitState.value,
                    explorationState = explorationUiState.value,
                    onStartExploration = { startExploration() },
                    onStopExploration = { stopExploration() },
                    onPauseJIT = { pauseJIT() },
                    onResumeJIT = { resumeJIT() },
                    onRefreshState = { updateAllState() },
                    onExport = { exportToAvu() }
                )
            }
        }
    }

    /**
     * Initialize exploration components
     */
    private fun initializeComponents() {
        explorationState = ExplorationState(DEFAULT_PACKAGE, DEFAULT_APP_NAME)
        safetyManager = SafetyManager.create(this)
        avuExporter = AVUExporter(this, ExportMode.USER)

        // Set exploration state callback
        explorationState.setCallback(object : ExplorationStateCallback {
            override fun onPhaseChanged(oldPhase: ExplorationPhase, newPhase: ExplorationPhase) {
                updateExplorationUiState()
            }

            override fun onScreenChanged(previousHash: String, newFingerprint: ScreenFingerprint) {
                updateExplorationUiState()
            }

            override fun onElementsDiscovered(count: Int) {
                updateExplorationUiState()
            }

            override fun onElementClicked(element: ElementInfo) {
                updateExplorationUiState()
            }

            override fun onNavigation(record: NavigationRecord) {
                updateExplorationUiState()
            }

            override fun onWaitingForUser(reason: String) {
                runOnUiThread {
                    Toast.makeText(this@LearnAppActivity, reason, Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(this@LearnAppActivity, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }

            override fun onExplorationComplete(stats: ExplorationStats) {
                updateExplorationUiState()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    /**
     * Bind to JIT learning service
     */
    private fun bindToJITService() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.augmentalis.voiceoscore",  // VoiceOSCore package
                "com.augmentalis.jitlearning.JITLearningService"
            )
        }

        try {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(TAG, "Binding to JIT service...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind to JIT service", e)
        }
    }

    /**
     * Update JIT state from service
     */
    private fun updateJITState() {
        try {
            jitState.value = jitService?.queryState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query JIT state", e)
        }
    }

    /**
     * Update exploration UI state from ExplorationState
     */
    private fun updateExplorationUiState() {
        val stats = explorationState.getStats()
        val loginResult = safetyManager.isLoginScreen()

        explorationUiState.value = ExplorationUiState(
            phase = explorationState.phase,
            screensExplored = stats.screensExplored,
            elementsDiscovered = stats.elementsDiscovered,
            elementsClicked = stats.elementsClicked,
            coverage = stats.coverage,
            dangerousElementsSkipped = stats.dangerousElementsSkipped,
            dynamicRegionsDetected = stats.dynamicRegionsDetected,
            menusDiscovered = explorationState.getMenus().size,
            isOnLoginScreen = loginResult.isLoginScreen,
            loginType = loginResult.loginType?.name ?: "",
            lastExportPath = explorationUiState.value.lastExportPath
        )
    }

    /**
     * Update all state
     */
    private fun updateAllState() {
        updateJITState()
        updateExplorationUiState()
    }

    /**
     * Start app exploration
     */
    private fun startExploration() {
        Log.i(TAG, "Starting exploration...")

        // Reset and start exploration state
        explorationState.reset()
        safetyManager.reset()
        explorationState.start()
        explorationState.beginExploring()

        // Pause JIT while exploring (mutual exclusion)
        pauseJIT()

        updateExplorationUiState()
        Toast.makeText(this, "Exploration started", Toast.LENGTH_SHORT).show()
    }

    /**
     * Stop app exploration
     */
    private fun stopExploration() {
        Log.i(TAG, "Stopping exploration...")

        explorationState.complete()

        // Resume JIT after exploring
        resumeJIT()

        updateExplorationUiState()
        Toast.makeText(this, "Exploration stopped", Toast.LENGTH_SHORT).show()
    }

    /**
     * Export exploration data to AVU file
     */
    private fun exportToAvu() {
        Log.i(TAG, "Exporting to AVU format...")

        try {
            // Generate commands from elements
            val elements = explorationState.getElements()
            val commands = CommandGenerator.generateCommands(elements, explorationState.packageName)
            val validCommands = CommandGenerator.validateCommands(
                CommandGenerator.deduplicateCommands(commands)
            )

            // Generate synonyms
            val synonyms = CommandGenerator.generateAllSynonyms(validCommands)

            // Export
            val result = avuExporter.export(explorationState, validCommands, synonyms)

            if (result.success) {
                explorationUiState.value = explorationUiState.value.copy(
                    lastExportPath = result.filePath
                )
                Toast.makeText(
                    this,
                    "Exported ${result.lineCount} lines to ${result.filePath}",
                    Toast.LENGTH_LONG
                ).show()
                Log.i(TAG, "Export successful: ${result.filePath}")
            } else {
                Toast.makeText(
                    this,
                    "Export failed: ${result.errorMessage}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Export failed: ${result.errorMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Export error", e)
            Toast.makeText(this, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Pause JIT capture
     */
    private fun pauseJIT() {
        try {
            jitService?.pauseCapture()
            updateJITState()
            Log.i(TAG, "JIT capture paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause JIT", e)
        }
    }

    /**
     * Resume JIT capture
     */
    private fun resumeJIT() {
        try {
            jitService?.resumeCapture()
            updateJITState()
            Log.i(TAG, "JIT capture resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume JIT", e)
        }
    }

    // ============================================================
    // SafetyCallback implementation
    // ============================================================

    override fun onLoginDetected(loginType: LoginType, message: String) {
        Log.i(TAG, "Login detected: $loginType - $message")
        explorationState.waitForUser(message)
        updateExplorationUiState()
    }

    override fun onDangerousElement(element: ElementInfo, reason: DoNotClickReason) {
        Log.w(TAG, "Dangerous element: ${element.getDisplayName()} - ${reason.description}")
        explorationState.recordDangerousElement(element, reason)
        updateExplorationUiState()
    }

    override fun onDynamicRegionConfirmed(region: DynamicRegion) {
        Log.i(TAG, "Dynamic region confirmed: ${region.regionId} - ${region.changeType}")
        explorationState.recordDynamicRegion(region)
        updateExplorationUiState()
    }

    override fun onLoopDetected(screenHash: String, visitCount: Int) {
        Log.w(TAG, "Loop detected: screen $screenHash visited $visitCount times")
        Toast.makeText(
            this,
            "Loop detected - screen visited $visitCount times",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * LearnApp Compose UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnAppUI(
    jitState: JITState?,
    explorationState: ExplorationUiState,
    onStartExploration: () -> Unit,
    onStopExploration: () -> Unit,
    onPauseJIT: () -> Unit,
    onResumeJIT: () -> Unit,
    onRefreshState: () -> Unit,
    onExport: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AvaLearnLite Explorer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // JIT Status Card
            JITStatusCard(
                jitState = jitState,
                onPauseJIT = onPauseJIT,
                onResumeJIT = onResumeJIT,
                onRefreshState = onRefreshState
            )

            // Exploration Status Card
            ExplorationStatusCard(
                explorationState = explorationState,
                onStartExploration = onStartExploration,
                onStopExploration = onStopExploration
            )

            // Safety Indicators Card
            SafetyIndicatorsCard(explorationState = explorationState)

            // Export Card
            ExportCard(
                explorationState = explorationState,
                onExport = onExport,
                enabled = explorationState.elementsDiscovered > 0
            )
        }
    }
}

/**
 * JIT Status Card
 */
@Composable
fun JITStatusCard(
    jitState: JITState?,
    onPauseJIT: () -> Unit,
    onResumeJIT: () -> Unit,
    onRefreshState: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("JIT Learning Status", style = MaterialTheme.typography.titleMedium)
                StatusBadge(
                    text = if (jitState?.isActive == true) "Active" else "Paused",
                    color = if (jitState?.isActive == true) OceanTheme.StatusActive else OceanTheme.StatusPaused
                )
            }

            if (jitState != null) {
                StatRow("Screens Learned", jitState.screensLearned.toString())
                StatRow("Elements Discovered", jitState.elementsDiscovered.toString())
                jitState.currentPackage?.let {
                    StatRow("Current Package", it)
                }
            } else {
                Text(
                    "Not connected to JIT service",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPauseJIT,
                    enabled = jitState?.isActive == true,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pause")
                }

                OutlinedButton(
                    onClick = onResumeJIT,
                    enabled = jitState?.isActive == false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resume")
                }

                IconButton(onClick = onRefreshState) {
                    Text("↻", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Exploration Status Card
 */
@Composable
fun ExplorationStatusCard(
    explorationState: ExplorationUiState,
    onStartExploration: () -> Unit,
    onStopExploration: () -> Unit
) {
    val isExploring = explorationState.phase == ExplorationPhase.EXPLORING ||
            explorationState.phase == ExplorationPhase.WAITING_USER

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExploring) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("App Exploration", style = MaterialTheme.typography.titleMedium)
                StatusBadge(
                    text = explorationState.phase.name,
                    color = when (explorationState.phase) {
                        ExplorationPhase.EXPLORING -> OceanTheme.StatusExploring
                        ExplorationPhase.WAITING_USER -> OceanTheme.StatusPaused
                        ExplorationPhase.COMPLETED -> OceanTheme.StatusCompleted
                        ExplorationPhase.ERROR -> OceanTheme.StatusError
                        else -> OceanTheme.StatusIdle
                    }
                )
            }

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox("Screens", explorationState.screensExplored.toString())
                StatBox("Elements", explorationState.elementsDiscovered.toString())
                StatBox("Clicked", explorationState.elementsClicked.toString())
                StatBox("Coverage", "${"%.0f".format(explorationState.coverage)}%")
            }

            // Control button
            Button(
                onClick = if (isExploring) onStopExploration else onStartExploration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExploring) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(if (isExploring) "Stop Exploration" else "Start Exploration")
            }
        }
    }
}

/**
 * Safety Indicators Card
 */
@Composable
fun SafetyIndicatorsCard(explorationState: ExplorationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Safety Status", style = MaterialTheme.typography.titleMedium)

            // Safety stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SafetyStatBox(
                    label = "DNC Skipped",
                    value = explorationState.dangerousElementsSkipped.toString(),
                    color = if (explorationState.dangerousElementsSkipped > 0) {
                        OceanTheme.Warning
                    } else {
                        OceanTheme.Success
                    }
                )
                SafetyStatBox(
                    label = "Dynamic Regions",
                    value = explorationState.dynamicRegionsDetected.toString(),
                    color = if (explorationState.dynamicRegionsDetected > 0) {
                        OceanTheme.Primary
                    } else {
                        OceanTheme.StatusIdle
                    }
                )
                SafetyStatBox(
                    label = "Menus Found",
                    value = explorationState.menusDiscovered.toString(),
                    color = if (explorationState.menusDiscovered > 0) {
                        OceanTheme.Secondary
                    } else {
                        OceanTheme.StatusIdle
                    }
                )
            }

            // Login screen warning
            if (explorationState.isOnLoginScreen) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = OceanTheme.Warning.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("!", fontWeight = FontWeight.Bold, color = OceanTheme.Warning)
                        Column {
                            Text(
                                "Login Screen Detected",
                                fontWeight = FontWeight.Medium,
                                color = OceanTheme.Warning
                            )
                            Text(
                                "Type: ${explorationState.loginType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OceanTheme.StatusIdle
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Export Card
 */
@Composable
fun ExportCard(
    explorationState: ExplorationUiState,
    onExport: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Export", style = MaterialTheme.typography.titleMedium)

            explorationState.lastExportPath?.let { path ->
                Text(
                    "Last export: ${path.substringAfterLast("/")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onExport,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export to AVU (.vos)")
            }

            if (!enabled) {
                Text(
                    "Explore an app first to enable export",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Status Badge component
 */
@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Stat Row component
 */
@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

/**
 * Stat Box component
 */
@Composable
fun StatBox(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Safety Stat Box component
 */
@Composable
fun SafetyStatBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
