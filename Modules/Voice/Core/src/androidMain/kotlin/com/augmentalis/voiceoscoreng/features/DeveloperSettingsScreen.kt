package com.augmentalis.voiceoscoreng.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscoreng.features.LearnAppConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle

/**
 * Settings tabs for organizing developer settings.
 */
enum class SettingsTab(val title: String, val icon: ImageVector) {
    BASIC("Basic", Icons.Default.Settings),
    VOICE("Voice", Icons.Default.Mic),
    DEVELOPER("Developer", Icons.Default.Code)
}

/**
 * Developer Settings Screen for VoiceOSCoreNG.
 *
 * Organized into tabs:
 * - Basic: Theme, High Contrast, Large Text, Reduce Motion
 * - Voice: Confidence Threshold, Speech Engine, Confirmation Mode
 * - Developer: Debug Overlay, Processing Mode, Limits, Framework Detection
 */
/**
 * Callbacks for scanning operations in DeveloperSettingsScreen.
 * These are provided by the app since VoiceOSAccessibilityService is in the app layer.
 */
data class ScanningCallbacks(
    val onSetContinuousMonitoring: (Boolean) -> Unit = {},
    val onRescanCurrentApp: () -> Unit = {},
    val onRescanEverything: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scanningCallbacks: ScanningCallbacks = ScanningCallbacks()
) {
    var devModeEnabled by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.enabled) }
    var selectedTab by remember { mutableStateOf(SettingsTab.BASIC) }

    // Developer settings state
    var maxElements by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.maxElementsPerScan?.toString() ?: "") }
    var maxApps by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.maxAppsLearned?.toString() ?: "") }
    var forceAI by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.forceEnableAI) }
    var forceNLU by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.forceEnableNLU) }
    var forceExploration by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.forceEnableExploration) }
    var forceFrameworkDetection by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.forceEnableFrameworkDetection) }
    var forceCaching by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.forceEnableCaching) }
    var forceAnalytics by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.forceEnableAnalytics) }
    var enableDebugOverlay by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.enableDebugOverlay) }
    var batchTimeout by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.batchTimeoutMs?.toString() ?: "") }
    var explorationDepth by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.explorationDepth?.toString() ?: "") }
    var selectedProcessingMode by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.processingModeOverride) }

    // Basic settings state
    var isDarkTheme by remember { mutableStateOf(false) }
    var highContrastEnabled by remember { mutableStateOf(false) }
    var largeTextEnabled by remember { mutableStateOf(false) }
    var reduceMotionEnabled by remember { mutableStateOf(false) }

    // Scanning settings state
    var continuousScanningEnabled by remember { mutableStateOf(true) }
    var showSliderDrawer by remember { mutableStateOf(false) }
    var showRescanConfirmDialog by remember { mutableStateOf(false) }

    // Voice settings state
    var confidenceThreshold by remember { mutableStateOf(0.7f) }
    var selectedSpeechEngine by remember { mutableStateOf("System Default") }
    var confirmationMode by remember { mutableStateOf("Always") }

    val currentTier = LearnAppDevToggle.getCurrentTier()

    // Rescan Everything Confirmation Dialog
    if (showRescanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRescanConfirmDialog = false },
            title = { Text("Rescan Everything") },
            text = {
                Column {
                    Text(
                        text = "This will clear ALL cached screen data for all apps.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "All screens will be re-scanned on next visit. This may temporarily slow down voice command recognition.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRescanConfirmDialog = false
                        scanningCallbacks.onRescanEverything()
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Determine which tabs to show
    val visibleTabs = if (devModeEnabled) {
        SettingsTab.entries
    } else {
        listOf(SettingsTab.BASIC, SettingsTab.VOICE)
    }

    // Reset to BASIC tab if Developer tab becomes hidden while selected
    LaunchedEffect(devModeEnabled) {
        if (!devModeEnabled && selectedTab == SettingsTab.DEVELOPER) {
            selectedTab = SettingsTab.BASIC
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
            actions = {
                // Current tier indicator
                AssistChip(
                    onClick = { },
                    label = { Text(currentTier.name) },
                    leadingIcon = {
                        Icon(
                            if (currentTier == LearnAppDevToggle.Tier.DEV) Icons.Default.Code else Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Developer Mode Card (always visible at top)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            DevModeCard(
                enabled = devModeEnabled,
                onToggle = { enabled ->
                    devModeEnabled = enabled
                    if (enabled) {
                        LearnAppConfig.DeveloperSettings.enable(unlockAll = false)
                    } else {
                        LearnAppConfig.DeveloperSettings.disable()
                        // Reset developer state
                        maxElements = ""
                        maxApps = ""
                        forceAI = false
                        forceNLU = false
                        forceExploration = false
                        forceFrameworkDetection = false
                        forceCaching = false
                        forceAnalytics = false
                        enableDebugOverlay = false
                        batchTimeout = ""
                        explorationDepth = ""
                        selectedProcessingMode = null
                    }
                },
                onUnlockAll = {
                    devModeEnabled = true
                    LearnAppConfig.DeveloperSettings.enable(unlockAll = true)
                    forceAI = true
                    forceNLU = true
                    forceExploration = true
                    forceFrameworkDetection = true
                    forceCaching = true
                    forceAnalytics = true
                    enableDebugOverlay = true
                    maxElements = "-1"
                    maxApps = "-1"
                }
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            visibleTabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) },
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }

        // Tab Content
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                SettingsTab.BASIC -> BasicSettingsContent(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChange = { isDarkTheme = it },
                    highContrastEnabled = highContrastEnabled,
                    onHighContrastChange = { highContrastEnabled = it },
                    largeTextEnabled = largeTextEnabled,
                    onLargeTextChange = { largeTextEnabled = it },
                    reduceMotionEnabled = reduceMotionEnabled,
                    onReduceMotionChange = { reduceMotionEnabled = it },
                    // Scanning settings
                    continuousScanningEnabled = continuousScanningEnabled,
                    onContinuousScanningChange = {
                        continuousScanningEnabled = it
                        scanningCallbacks.onSetContinuousMonitoring(it)
                    },
                    showSliderDrawer = showSliderDrawer,
                    onShowSliderDrawerChange = { showSliderDrawer = it },
                    devModeEnabled = devModeEnabled,
                    onRescanCurrentApp = { scanningCallbacks.onRescanCurrentApp() },
                    onRescanEverything = { showRescanConfirmDialog = true }
                )

                SettingsTab.VOICE -> VoiceSettingsContent(
                    confidenceThreshold = confidenceThreshold,
                    onConfidenceChange = { confidenceThreshold = it },
                    selectedSpeechEngine = selectedSpeechEngine,
                    onSpeechEngineChange = { selectedSpeechEngine = it },
                    confirmationMode = confirmationMode,
                    onConfirmationModeChange = { confirmationMode = it }
                )

                SettingsTab.DEVELOPER -> DeveloperSettingsContent(
                    enableDebugOverlay = enableDebugOverlay,
                    onDebugOverlayChange = {
                        enableDebugOverlay = it
                        LearnAppConfig.DeveloperSettings.enableDebugOverlay = it
                    },
                    selectedProcessingMode = selectedProcessingMode,
                    onProcessingModeChange = {
                        selectedProcessingMode = it
                        LearnAppConfig.DeveloperSettings.processingModeOverride = it
                    },
                    maxElements = maxElements,
                    onMaxElementsChange = { value ->
                        maxElements = value
                        LearnAppConfig.DeveloperSettings.maxElementsPerScan = value.toIntOrNull()
                    },
                    maxApps = maxApps,
                    onMaxAppsChange = { value ->
                        maxApps = value
                        LearnAppConfig.DeveloperSettings.maxAppsLearned = value.toIntOrNull()
                    },
                    batchTimeout = batchTimeout,
                    onBatchTimeoutChange = { value ->
                        batchTimeout = value
                        LearnAppConfig.DeveloperSettings.batchTimeoutMs = value.toLongOrNull()
                    },
                    explorationDepth = explorationDepth,
                    onExplorationDepthChange = { value ->
                        explorationDepth = value
                        LearnAppConfig.DeveloperSettings.explorationDepth = value.toIntOrNull()
                    },
                    forceAI = forceAI,
                    onForceAIChange = {
                        forceAI = it
                        LearnAppConfig.DeveloperSettings.forceEnableAI = it
                    },
                    forceNLU = forceNLU,
                    onForceNLUChange = {
                        forceNLU = it
                        LearnAppConfig.DeveloperSettings.forceEnableNLU = it
                    },
                    forceExploration = forceExploration,
                    onForceExplorationChange = {
                        forceExploration = it
                        LearnAppConfig.DeveloperSettings.forceEnableExploration = it
                    },
                    forceFrameworkDetection = forceFrameworkDetection,
                    onForceFrameworkDetectionChange = {
                        forceFrameworkDetection = it
                        LearnAppConfig.DeveloperSettings.forceEnableFrameworkDetection = it
                    },
                    forceCaching = forceCaching,
                    onForceCachingChange = {
                        forceCaching = it
                        LearnAppConfig.DeveloperSettings.forceEnableCaching = it
                    },
                    forceAnalytics = forceAnalytics,
                    onForceAnalyticsChange = {
                        forceAnalytics = it
                        LearnAppConfig.DeveloperSettings.forceEnableAnalytics = it
                    }
                )
            }
        }
    }
}

@Composable
private fun BasicSettingsContent(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    highContrastEnabled: Boolean,
    onHighContrastChange: (Boolean) -> Unit,
    largeTextEnabled: Boolean,
    onLargeTextChange: (Boolean) -> Unit,
    reduceMotionEnabled: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
    // Scanning settings
    continuousScanningEnabled: Boolean,
    onContinuousScanningChange: (Boolean) -> Unit,
    showSliderDrawer: Boolean,
    onShowSliderDrawerChange: (Boolean) -> Unit,
    devModeEnabled: Boolean,
    onRescanCurrentApp: () -> Unit,
    onRescanEverything: () -> Unit
) {
    // Scanning Section (Primary feature)
    SettingsSection(title = "Scanning", icon = Icons.Default.Search) {
        FeatureToggle(
            title = "Continuous Monitoring",
            description = "Auto-scan when screen changes",
            checked = continuousScanningEnabled,
            onCheckedChange = onContinuousScanningChange
        )
    }

    // Developer Scanning Options (only visible when dev mode is enabled)
    if (devModeEnabled) {
        SettingsSection(title = "Developer Scanning", icon = Icons.Default.Build) {
            FeatureToggle(
                title = "Show Slider Drawer",
                description = "Manual scan control overlay",
                checked = showSliderDrawer,
                onCheckedChange = onShowSliderDrawerChange
            )

            // Rescan Current App button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rescan Current App",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Clear cache for current app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = onRescanCurrentApp) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rescan")
                }
            }

            // Rescan Everything button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rescan Everything",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Clear all cached screen data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onRescanEverything,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }
    }

    SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
        FeatureToggle(
            title = "Dark Theme",
            description = "Use dark color scheme throughout the app",
            checked = isDarkTheme,
            onCheckedChange = onDarkThemeChange
        )

        FeatureToggle(
            title = "High Contrast",
            description = "Increase contrast for better visibility",
            checked = highContrastEnabled,
            onCheckedChange = onHighContrastChange
        )
    }

    SettingsSection(title = "Accessibility", icon = Icons.Default.Accessibility) {
        FeatureToggle(
            title = "Large Text",
            description = "Increase text size for readability",
            checked = largeTextEnabled,
            onCheckedChange = onLargeTextChange
        )

        FeatureToggle(
            title = "Reduce Motion",
            description = "Minimize animations and transitions",
            checked = reduceMotionEnabled,
            onCheckedChange = onReduceMotionChange
        )
    }
}

@Composable
private fun VoiceSettingsContent(
    confidenceThreshold: Float,
    onConfidenceChange: (Float) -> Unit,
    selectedSpeechEngine: String,
    onSpeechEngineChange: (String) -> Unit,
    confirmationMode: String,
    onConfirmationModeChange: (String) -> Unit
) {
    SettingsSection(title = "Recognition", icon = Icons.Default.RecordVoiceOver) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Confidence Threshold: ${(confidenceThreshold * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Minimum confidence required to accept voice commands",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = confidenceThreshold,
                onValueChange = onConfidenceChange,
                valueRange = 0.5f..1.0f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    SettingsSection(title = "Speech Engine", icon = Icons.Default.SettingsVoice) {
        val engines = listOf("System Default", "Google Speech", "Whisper (Local)")
        engines.forEach { engine ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSpeechEngine == engine,
                    onClick = { onSpeechEngineChange(engine) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = engine,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    SettingsSection(title = "Confirmation", icon = Icons.Default.CheckCircle) {
        val modes = listOf(
            "Always" to "Always confirm before executing commands",
            "Destructive Only" to "Confirm only for delete/modify actions",
            "Never" to "Execute commands immediately"
        )
        modes.forEach { (mode, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = confirmationMode == mode,
                    onClick = { onConfirmationModeChange(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = mode,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperSettingsContent(
    enableDebugOverlay: Boolean,
    onDebugOverlayChange: (Boolean) -> Unit,
    selectedProcessingMode: LearnAppConfig.ProcessingMode?,
    onProcessingModeChange: (LearnAppConfig.ProcessingMode?) -> Unit,
    maxElements: String,
    onMaxElementsChange: (String) -> Unit,
    maxApps: String,
    onMaxAppsChange: (String) -> Unit,
    batchTimeout: String,
    onBatchTimeoutChange: (String) -> Unit,
    explorationDepth: String,
    onExplorationDepthChange: (String) -> Unit,
    forceAI: Boolean,
    onForceAIChange: (Boolean) -> Unit,
    forceNLU: Boolean,
    onForceNLUChange: (Boolean) -> Unit,
    forceExploration: Boolean,
    onForceExplorationChange: (Boolean) -> Unit,
    forceFrameworkDetection: Boolean,
    onForceFrameworkDetectionChange: (Boolean) -> Unit,
    forceCaching: Boolean,
    onForceCachingChange: (Boolean) -> Unit,
    forceAnalytics: Boolean,
    onForceAnalyticsChange: (Boolean) -> Unit
) {
    // Debug Overlay
    SettingsSection(title = "Debug", icon = Icons.Default.BugReport) {
        FeatureToggle(
            title = "Debug Overlay",
            description = "Show debug information overlay on screen",
            checked = enableDebugOverlay,
            onCheckedChange = onDebugOverlayChange
        )
    }

    // Processing Mode Section
    SettingsSection(title = "Processing Mode", icon = Icons.Default.Speed) {
        val modes = listOf(null) + LearnAppConfig.ProcessingMode.entries
        modes.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedProcessingMode == mode,
                    onClick = { onProcessingModeChange(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = mode?.name ?: "Default (use tier setting)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = when (mode) {
                            LearnAppConfig.ProcessingMode.IMMEDIATE -> "Process elements immediately"
                            LearnAppConfig.ProcessingMode.BATCH -> "Collect and process in batches"
                            LearnAppConfig.ProcessingMode.HYBRID -> "Immediate for common, batch for complex"
                            null -> "Uses tier default"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Limits Section
    SettingsSection(title = "Limits", icon = Icons.Default.Tune) {
        OutlinedTextField(
            value = maxElements,
            onValueChange = onMaxElementsChange,
            label = { Text("Max Elements per Scan") },
            placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.MAX_ELEMENTS_PER_SCAN}") },
            supportingText = { Text("-1 for unlimited") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = maxApps,
            onValueChange = onMaxAppsChange,
            label = { Text("Max Apps Learned") },
            placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.MAX_APPS_LEARNED}") },
            supportingText = { Text("-1 for unlimited") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = batchTimeout,
            onValueChange = onBatchTimeoutChange,
            label = { Text("Batch Timeout (ms)") },
            placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.BATCH_TIMEOUT_MS}ms") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = explorationDepth,
            onValueChange = onExplorationDepthChange,
            label = { Text("Exploration Depth") },
            placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.EXPLORATION_DEPTH}") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    // Feature Overrides Section
    SettingsSection(title = "Feature Overrides", icon = Icons.Default.ToggleOn) {
        FeatureToggle(
            title = "AI Features",
            description = "Element classification, naming, suggestions",
            checked = forceAI,
            onCheckedChange = onForceAIChange
        )

        FeatureToggle(
            title = "NLU Features",
            description = "Natural language understanding for voice commands",
            checked = forceNLU,
            onCheckedChange = onForceNLUChange
        )

        FeatureToggle(
            title = "Exploration Mode",
            description = "Full app exploration and batch learning",
            checked = forceExploration,
            onCheckedChange = onForceExplorationChange
        )

        FeatureToggle(
            title = "Framework Detection",
            description = "Detect Flutter, Unity, React Native, WebView",
            checked = forceFrameworkDetection,
            onCheckedChange = onForceFrameworkDetectionChange
        )

        FeatureToggle(
            title = "Caching",
            description = "Cache screen states for performance",
            checked = forceCaching,
            onCheckedChange = onForceCachingChange
        )

        FeatureToggle(
            title = "Analytics",
            description = "Track usage and command metrics",
            checked = forceAnalytics,
            onCheckedChange = onForceAnalyticsChange
        )
    }

    // Current Config Summary
    SettingsSection(title = "Current Configuration", icon = Icons.Default.Info) {
        val summary = LearnAppConfig.getSummary()
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        )
    }
}

@Composable
private fun DevModeCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onUnlockAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DeveloperMode,
                        contentDescription = null,
                        tint = if (enabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Developer Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (enabled) "Developer tab unlocked" else "Tap to unlock developer options",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onUnlockAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock All Features")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun FeatureToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
