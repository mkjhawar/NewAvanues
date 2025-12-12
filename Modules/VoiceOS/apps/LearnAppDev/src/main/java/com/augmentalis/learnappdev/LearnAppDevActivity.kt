/**
 * LearnAppDevActivity.kt - Developer Edition main activity
 *
 * Full-featured developer edition with:
 * - Neo4j graph visualization
 * - Full exploration logs
 * - Element tree inspector
 * - Unencrypted AVU export
 * - Real-time event stream viewer
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappdev

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.jitlearning.IAccessibilityEventListener
import com.augmentalis.jitlearning.IElementCaptureService
import com.augmentalis.jitlearning.JITState
import com.augmentalis.jitlearning.ScreenChangeEvent
import com.augmentalis.jitlearning.ParcelableNodeInfo
import com.augmentalis.learnappcore.exploration.*
import com.augmentalis.learnappcore.export.*
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.safety.*
import java.text.SimpleDateFormat
import java.util.*

// Ocean Blue XR Theme - Developer Edition (Dark Mode with Cyan Accent)
private object OceanDevTheme {
    // Primary Colors (Ocean Blue Dark Mode)
    val Primary = Color(0xFF60A5FA)
    val PrimaryVariant = Color(0xFF3B82F6)
    val PrimaryContainer = Color(0xFF1E3A5F)

    // Developer Accent (Cyan - distinguishes from User Edition)
    val Accent = Color(0xFF22D3EE)
    val AccentVariant = Color(0xFF06B6D4)

    // Secondary
    val Secondary = Color(0xFF818CF8)

    // Semantic Colors (Dark Mode)
    val Success = Color(0xFF34D399)
    val Error = Color(0xFFF87171)
    val Warning = Color(0xFFFBBF24)
    val Info = Color(0xFF60A5FA)

    // Surface (Glassmorphic Dark)
    val Surface = Color(0xFF0F172A)
    val SurfaceVariant = Color(0xFF1E293B)
    val GlassSurface = Color(0x14FFFFFF)
    val GlassBorder = Color(0x26FFFFFF)

    // Background
    val Background = Color(0xFF0C1929)

    // Console
    val ConsoleBackground = Color(0xFF0D0D0D)

    // Status
    val StatusActive = Color(0xFF34D399)
    val StatusPaused = Color(0xFFFBBF24)
    val StatusIdle = Color(0xFF6B7280)
    val StatusExploring = Color(0xFF60A5FA)
    val StatusError = Color(0xFFF87171)
    val StatusCompleted = Color(0xFF34D399)
}

/**
 * Log entry for developer console
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = ""
) {
    fun formatted(): String {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
        return "[$time] ${level.prefix} $tag: $message"
    }
}

enum class LogLevel(val prefix: String, val color: Color) {
    DEBUG("D", Color(0xFF9E9E9E)),
    INFO("I", Color(0xFF60A5FA)),   // Ocean Blue
    WARN("W", Color(0xFFFBBF24)),   // Ocean Warning
    ERROR("E", Color(0xFFF87171)),  // Ocean Error
    EVENT("E", Color(0xFFA78BFA))   // Purple for events
}

/**
 * Developer edition UI state
 */
data class DevUiState(
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
    val lastExportPath: String? = null,
    val neo4jConnected: Boolean = false,
    val eventStreamActive: Boolean = false
)

/**
 * LearnAppDev Main Activity
 *
 * Developer edition with full debugging capabilities.
 */
class LearnAppDevActivity : ComponentActivity(), SafetyCallback {

    companion object {
        private const val TAG = "LearnAppDevActivity"
        private const val DEFAULT_PACKAGE = "com.example.app"
        private const val DEFAULT_APP_NAME = "Example App"
        private const val MAX_LOG_ENTRIES = 500
    }

    // AIDL service binding
    private var jitService: IElementCaptureService? = null
    private var isBound = false

    // UI state
    private val jitState = mutableStateOf<JITState?>(null)
    private val devUiState = mutableStateOf(DevUiState())
    private val logEntries = mutableStateListOf<LogEntry>()
    private val currentElements = mutableStateListOf<ParcelableNodeInfo>()

    // Exploration components
    private lateinit var explorationState: ExplorationState
    private lateinit var safetyManager: SafetyManager
    private lateinit var avuExporter: AVUExporter

    // Event listener for real-time streaming
    private val eventListener = object : IAccessibilityEventListener.Stub() {
        override fun onScreenChanged(event: ScreenChangeEvent) {
            addLog(LogLevel.EVENT, "SCREEN", "Screen changed: ${event.toIpcString()}")
            runOnUiThread {
                devUiState.value = devUiState.value.copy(eventStreamActive = true)
            }
        }

        override fun onElementAction(elementUuid: String, actionType: String, success: Boolean) {
            val result = if (success) "OK" else "FAIL"
            addLog(LogLevel.EVENT, "ACTION", "$actionType on $elementUuid: $result")
        }

        override fun onScrollDetected(direction: String, distance: Int, newElementsCount: Int) {
            addLog(LogLevel.EVENT, "SCROLL", "$direction ${distance}px, $newElementsCount new elements")
        }

        override fun onDynamicContentDetected(screenHash: String, regionId: String) {
            addLog(LogLevel.EVENT, "DYNAMIC", "Screen $screenHash, region $regionId")
        }

        override fun onMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int) {
            addLog(LogLevel.EVENT, "MENU", "$menuId: $visibleItems/$totalItems items")
        }

        override fun onLoginScreenDetected(packageName: String, screenHash: String) {
            addLog(LogLevel.WARN, "LOGIN", "Detected in $packageName, screen $screenHash")
        }

        override fun onError(errorCode: String, message: String) {
            addLog(LogLevel.ERROR, "JIT", "[$errorCode] $message")
        }
    }

    /**
     * Service connection for JIT service binding
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            addLog(LogLevel.INFO, TAG, "Connected to JIT service")
            jitService = IElementCaptureService.Stub.asInterface(service)
            isBound = true
            updateJITState()

            // Register event listener
            try {
                jitService?.registerEventListener(eventListener)
                addLog(LogLevel.INFO, TAG, "Event listener registered")
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Failed to register event listener: ${e.message}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            addLog(LogLevel.WARN, TAG, "Disconnected from JIT service")
            jitService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addLog(LogLevel.INFO, TAG, "LearnAppDev activity created")

        // Initialize exploration components
        initializeComponents()

        // Bind to JIT service
        bindToJITService()

        // Set Compose UI with Ocean Blue XR Developer Theme (Dark Mode)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = OceanDevTheme.Primary,
                    onPrimary = Color.White,
                    primaryContainer = OceanDevTheme.PrimaryContainer,
                    secondary = OceanDevTheme.Secondary,
                    surface = OceanDevTheme.Surface,
                    surfaceVariant = OceanDevTheme.SurfaceVariant,
                    background = OceanDevTheme.Background,
                    error = OceanDevTheme.Error
                )
            ) {
                LearnAppDevUI(
                    jitState = jitState.value,
                    devState = devUiState.value,
                    logEntries = logEntries,
                    elements = currentElements,
                    onStartExploration = { startExploration() },
                    onStopExploration = { stopExploration() },
                    onPauseJIT = { pauseJIT() },
                    onResumeJIT = { resumeJIT() },
                    onRefreshState = { updateAllState() },
                    onExport = { exportToAvu() },
                    onClearLogs = { clearLogs() },
                    onQueryElements = { queryCurrentElements() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try {
                jitService?.unregisterEventListener(eventListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister event listener", e)
            }
            unbindService(serviceConnection)
            isBound = false
        }
    }

    /**
     * Initialize exploration components
     */
    private fun initializeComponents() {
        explorationState = ExplorationState(DEFAULT_PACKAGE, DEFAULT_APP_NAME)
        safetyManager = SafetyManager.create(this)
        avuExporter = AVUExporter(this, ExportMode.DEVELOPER)

        // Set exploration state callback
        explorationState.setCallback(object : ExplorationStateCallback {
            override fun onPhaseChanged(oldPhase: ExplorationPhase, newPhase: ExplorationPhase) {
                addLog(LogLevel.INFO, "PHASE", "$oldPhase -> $newPhase")
                updateDevUiState()
            }

            override fun onScreenChanged(previousHash: String, newFingerprint: ScreenFingerprint) {
                addLog(LogLevel.INFO, "SCREEN", "Hash: ${newFingerprint.screenHash}")
                updateDevUiState()
            }

            override fun onElementsDiscovered(count: Int) {
                addLog(LogLevel.INFO, "ELEMENTS", "Discovered $count elements")
                updateDevUiState()
            }

            override fun onElementClicked(element: ElementInfo) {
                addLog(LogLevel.INFO, "CLICK", element.getDisplayName())
                updateDevUiState()
            }

            override fun onNavigation(record: NavigationRecord) {
                addLog(LogLevel.INFO, "NAV", "${record.fromScreenHash} -> ${record.toScreenHash}")
                updateDevUiState()
            }

            override fun onWaitingForUser(reason: String) {
                addLog(LogLevel.WARN, "WAIT", reason)
                runOnUiThread {
                    Toast.makeText(this@LearnAppDevActivity, reason, Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(message: String) {
                addLog(LogLevel.ERROR, "ERROR", message)
                runOnUiThread {
                    Toast.makeText(this@LearnAppDevActivity, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }

            override fun onExplorationComplete(stats: ExplorationStats) {
                addLog(LogLevel.INFO, "COMPLETE", "Stats: ${stats.toStaLine()}")
                updateDevUiState()
            }
        })
    }

    /**
     * Add log entry
     */
    private fun addLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )

        runOnUiThread {
            logEntries.add(0, entry)
            if (logEntries.size > MAX_LOG_ENTRIES) {
                logEntries.removeAt(logEntries.size - 1)
            }
        }

        // Also log to Logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
            LogLevel.EVENT -> Log.v(tag, message)
        }
    }

    /**
     * Clear logs
     */
    private fun clearLogs() {
        logEntries.clear()
        addLog(LogLevel.INFO, TAG, "Logs cleared")
    }

    /**
     * Bind to JIT learning service
     */
    private fun bindToJITService() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.augmentalis.voiceoscore",
                "com.augmentalis.jitlearning.JITLearningService"
            )
        }

        try {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            addLog(LogLevel.INFO, TAG, "Binding to JIT service...")
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Failed to bind to JIT service: ${e.message}")
        }
    }

    /**
     * Update JIT state from service
     */
    private fun updateJITState() {
        try {
            jitState.value = jitService?.queryState()
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Failed to query JIT state: ${e.message}")
        }
    }

    /**
     * Update developer UI state
     */
    private fun updateDevUiState() {
        val stats = explorationState.getStats()
        val loginResult = safetyManager.isLoginScreen()

        devUiState.value = DevUiState(
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
            lastExportPath = devUiState.value.lastExportPath,
            neo4jConnected = devUiState.value.neo4jConnected,
            eventStreamActive = devUiState.value.eventStreamActive
        )
    }

    /**
     * Update all state
     */
    private fun updateAllState() {
        updateJITState()
        updateDevUiState()
    }

    /**
     * Query current elements from JIT service
     */
    private fun queryCurrentElements() {
        try {
            val screenInfo = jitService?.getCurrentScreenInfo()
            if (screenInfo != null) {
                currentElements.clear()
                currentElements.add(screenInfo)
                // Add children if available
                currentElements.addAll(screenInfo.children)
                addLog(LogLevel.INFO, TAG, "Queried ${currentElements.size} elements")
            } else {
                addLog(LogLevel.WARN, TAG, "No screen info available")
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Failed to query elements: ${e.message}")
        }
    }

    /**
     * Start app exploration
     */
    private fun startExploration() {
        addLog(LogLevel.INFO, TAG, "Starting exploration...")

        explorationState.reset()
        safetyManager.reset()
        explorationState.start()
        explorationState.beginExploring()

        pauseJIT()
        updateDevUiState()
    }

    /**
     * Stop app exploration
     */
    private fun stopExploration() {
        addLog(LogLevel.INFO, TAG, "Stopping exploration...")

        explorationState.complete()
        resumeJIT()
        updateDevUiState()
    }

    /**
     * Export exploration data to AVU file
     */
    private fun exportToAvu() {
        addLog(LogLevel.INFO, TAG, "Exporting to AVU format (Developer mode)...")

        try {
            val elements = explorationState.getElements()
            val commands = CommandGenerator.generateCommands(elements, explorationState.packageName)
            val validCommands = CommandGenerator.validateCommands(
                CommandGenerator.deduplicateCommands(commands)
            )
            val synonyms = CommandGenerator.generateAllSynonyms(validCommands)

            val result = avuExporter.export(explorationState, validCommands, synonyms)

            if (result.success) {
                devUiState.value = devUiState.value.copy(lastExportPath = result.filePath)
                addLog(LogLevel.INFO, TAG, "Export successful: ${result.filePath}")
                Toast.makeText(
                    this,
                    "Exported ${result.lineCount} lines",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                addLog(LogLevel.ERROR, TAG, "Export failed: ${result.errorMessage}")
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Export error: ${e.message}")
        }
    }

    /**
     * Pause JIT capture
     */
    private fun pauseJIT() {
        try {
            jitService?.pauseCapture()
            updateJITState()
            addLog(LogLevel.INFO, TAG, "JIT capture paused")
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Failed to pause JIT: ${e.message}")
        }
    }

    /**
     * Resume JIT capture
     */
    private fun resumeJIT() {
        try {
            jitService?.resumeCapture()
            updateJITState()
            addLog(LogLevel.INFO, TAG, "JIT capture resumed")
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Failed to resume JIT: ${e.message}")
        }
    }

    // ============================================================
    // SafetyCallback implementation
    // ============================================================

    override fun onLoginDetected(loginType: LoginType, message: String) {
        addLog(LogLevel.WARN, "SAFETY", "Login detected: $loginType - $message")
        explorationState.waitForUser(message)
        updateDevUiState()
    }

    override fun onDangerousElement(element: ElementInfo, reason: DoNotClickReason) {
        addLog(LogLevel.WARN, "SAFETY", "DNC: ${element.getDisplayName()} - ${reason.description}")
        explorationState.recordDangerousElement(element, reason)
        updateDevUiState()
    }

    override fun onDynamicRegionConfirmed(region: DynamicRegion) {
        addLog(LogLevel.INFO, "SAFETY", "Dynamic region: ${region.regionId}")
        explorationState.recordDynamicRegion(region)
        updateDevUiState()
    }

    override fun onLoopDetected(screenHash: String, visitCount: Int) {
        addLog(LogLevel.WARN, "SAFETY", "Loop detected: $screenHash ($visitCount visits)")
    }
}

/**
 * LearnAppDev Compose UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnAppDevUI(
    jitState: JITState?,
    devState: DevUiState,
    logEntries: List<LogEntry>,
    elements: List<ParcelableNodeInfo>,
    onStartExploration: () -> Unit,
    onStopExploration: () -> Unit,
    onPauseJIT: () -> Unit,
    onResumeJIT: () -> Unit,
    onRefreshState: () -> Unit,
    onExport: () -> Unit,
    onClearLogs: () -> Unit,
    onQueryElements: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Status", "Logs", "Elements")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("AvaLearnPro")
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = OceanDevTheme.Accent
                            ) {
                                Text(
                                    "DEV",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanDevTheme.Background
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OceanDevTheme.Surface
                    )
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> StatusTab(
                modifier = Modifier.padding(padding),
                jitState = jitState,
                devState = devState,
                onStartExploration = onStartExploration,
                onStopExploration = onStopExploration,
                onPauseJIT = onPauseJIT,
                onResumeJIT = onResumeJIT,
                onRefreshState = onRefreshState,
                onExport = onExport
            )
            1 -> LogsTab(
                modifier = Modifier.padding(padding),
                logEntries = logEntries,
                onClearLogs = onClearLogs
            )
            2 -> ElementsTab(
                modifier = Modifier.padding(padding),
                elements = elements,
                onQueryElements = onQueryElements
            )
        }
    }
}

/**
 * Status Tab
 */
@Composable
fun StatusTab(
    modifier: Modifier = Modifier,
    jitState: JITState?,
    devState: DevUiState,
    onStartExploration: () -> Unit,
    onStopExploration: () -> Unit,
    onPauseJIT: () -> Unit,
    onResumeJIT: () -> Unit,
    onRefreshState: () -> Unit,
    onExport: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isExploring = devState.phase == ExplorationPhase.EXPLORING ||
            devState.phase == ExplorationPhase.WAITING_USER

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // JIT Status
        DevCard(title = "JIT Service") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status")
                Text(
                    if (jitState?.isActive == true) "ACTIVE" else "PAUSED",
                    color = if (jitState?.isActive == true) OceanDevTheme.StatusActive else OceanDevTheme.StatusPaused,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Screens")
                Text("${jitState?.screensLearned ?: 0}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Elements")
                Text("${jitState?.elementsDiscovered ?: 0}")
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
                    Text("↻")
                }
            }
        }

        // Exploration Status
        DevCard(title = "Exploration") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Phase")
                Text(
                    devState.phase.name,
                    color = when (devState.phase) {
                        ExplorationPhase.EXPLORING -> OceanDevTheme.StatusExploring
                        ExplorationPhase.COMPLETED -> OceanDevTheme.StatusCompleted
                        ExplorationPhase.ERROR -> OceanDevTheme.StatusError
                        else -> OceanDevTheme.StatusIdle
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            DevStatRow("Screens", devState.screensExplored.toString())
            DevStatRow("Elements", devState.elementsDiscovered.toString())
            DevStatRow("Clicked", devState.elementsClicked.toString())
            DevStatRow("Coverage", "${"%.1f".format(devState.coverage)}%")

            Button(
                onClick = if (isExploring) onStopExploration else onStartExploration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExploring) OceanDevTheme.Error else OceanDevTheme.Success
                )
            ) {
                Text(if (isExploring) "STOP" else "START")
            }
        }

        // Safety Status
        DevCard(title = "Safety") {
            DevStatRow("DNC Skipped", devState.dangerousElementsSkipped.toString())
            DevStatRow("Dynamic Regions", devState.dynamicRegionsDetected.toString())
            DevStatRow("Menus Found", devState.menusDiscovered.toString())
            if (devState.isOnLoginScreen) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = OceanDevTheme.Warning.copy(alpha = 0.2f)
                ) {
                    Text(
                        "LOGIN DETECTED: ${devState.loginType}",
                        modifier = Modifier.padding(8.dp),
                        color = OceanDevTheme.Warning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Export
        DevCard(title = "Export") {
            devState.lastExportPath?.let {
                Text(
                    "Last: ${it.substringAfterLast("/")}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Button(
                onClick = onExport,
                enabled = devState.elementsDiscovered > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export AVU (Dev)")
            }
        }
    }
}

/**
 * Logs Tab
 */
@Composable
fun LogsTab(
    modifier: Modifier = Modifier,
    logEntries: List<LogEntry>,
    onClearLogs: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${logEntries.size} entries", color = Color.Gray)
            TextButton(onClick = onClearLogs) {
                Text("Clear")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OceanDevTheme.ConsoleBackground)
                .padding(8.dp)
        ) {
            items(logEntries) { entry ->
                Text(
                    text = entry.formatted(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = entry.level.color,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Elements Tab
 */
@Composable
fun ElementsTab(
    modifier: Modifier = Modifier,
    elements: List<ParcelableNodeInfo>,
    onQueryElements: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${elements.size} elements", color = Color.Gray)
            Button(onClick = onQueryElements) {
                Text("Query")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(elements) { element ->
                ElementCard(element = element)
            }
        }
    }
}

/**
 * Element Card
 */
@Composable
fun ElementCard(element: ParcelableNodeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = OceanDevTheme.SurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                element.getDisplayName(),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                element.getShortClassName(),
                fontSize = 12.sp,
                color = OceanDevTheme.StatusIdle
            )
            if (element.resourceId.isNotEmpty()) {
                Text(
                    element.resourceId,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = OceanDevTheme.Success
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (element.isClickable) ActionChip("click")
                if (element.isLongClickable) ActionChip("long")
                if (element.isEditable) ActionChip("edit")
                if (element.isScrollable) ActionChip("scroll")
            }
            Text(
                "[${element.boundsLeft},${element.boundsTop},${element.boundsRight},${element.boundsBottom}]",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = OceanDevTheme.StatusIdle
            )
        }
    }
}

/**
 * Action Chip
 */
@Composable
fun ActionChip(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = OceanDevTheme.Primary.copy(alpha = 0.2f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = OceanDevTheme.Primary
        )
    }
}

/**
 * Developer Card
 */
@Composable
fun DevCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = OceanDevTheme.SurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = OceanDevTheme.Accent
            )
            content()
        }
    }
}

/**
 * Developer Stat Row
 */
@Composable
fun DevStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White)
    }
}
