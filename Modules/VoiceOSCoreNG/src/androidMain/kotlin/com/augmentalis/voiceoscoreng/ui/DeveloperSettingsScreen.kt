package com.augmentalis.voiceoscoreng.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscoreng.features.LearnAppConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle

/**
 * Developer Settings Screen for VoiceOSCoreNG.
 *
 * Allows developers to:
 * - Toggle developer mode
 * - Override tier limits
 * - Enable/disable individual features
 * - View current configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var devModeEnabled by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.enabled) }
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

    val currentTier = LearnAppDevToggle.getCurrentTier()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Developer Settings",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Developer Mode Toggle
            DevModeCard(
                enabled = devModeEnabled,
                onToggle = { enabled ->
                    devModeEnabled = enabled
                    if (enabled) {
                        LearnAppConfig.DeveloperSettings.enable(unlockAll = false)
                    } else {
                        LearnAppConfig.DeveloperSettings.disable()
                        // Reset local state
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

            if (devModeEnabled) {
                // Limits Section
                SettingsSection(title = "Limits", icon = Icons.Default.Tune) {
                    OutlinedTextField(
                        value = maxElements,
                        onValueChange = { value ->
                            maxElements = value
                            LearnAppConfig.DeveloperSettings.maxElementsPerScan = value.toIntOrNull()
                        },
                        label = { Text("Max Elements per Scan") },
                        placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.MAX_ELEMENTS_PER_SCAN}") },
                        supportingText = { Text("-1 for unlimited") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = maxApps,
                        onValueChange = { value ->
                            maxApps = value
                            LearnAppConfig.DeveloperSettings.maxAppsLearned = value.toIntOrNull()
                        },
                        label = { Text("Max Apps Learned") },
                        placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.MAX_APPS_LEARNED}") },
                        supportingText = { Text("-1 for unlimited") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = batchTimeout,
                        onValueChange = { value ->
                            batchTimeout = value
                            LearnAppConfig.DeveloperSettings.batchTimeoutMs = value.toLongOrNull()
                        },
                        label = { Text("Batch Timeout (ms)") },
                        placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.BATCH_TIMEOUT_MS}ms") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = explorationDepth,
                        onValueChange = { value ->
                            explorationDepth = value
                            LearnAppConfig.DeveloperSettings.explorationDepth = value.toIntOrNull()
                        },
                        label = { Text("Exploration Depth") },
                        placeholder = { Text("Default: ${LearnAppConfig.LiteDefaults.EXPLORATION_DEPTH}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Feature Toggles Section
                SettingsSection(title = "Feature Overrides", icon = Icons.Default.ToggleOn) {
                    FeatureToggle(
                        title = "AI Features",
                        description = "Element classification, naming, suggestions",
                        checked = forceAI,
                        onCheckedChange = {
                            forceAI = it
                            LearnAppConfig.DeveloperSettings.forceEnableAI = it
                        }
                    )

                    FeatureToggle(
                        title = "NLU Features",
                        description = "Natural language understanding for voice commands",
                        checked = forceNLU,
                        onCheckedChange = {
                            forceNLU = it
                            LearnAppConfig.DeveloperSettings.forceEnableNLU = it
                        }
                    )

                    FeatureToggle(
                        title = "Exploration Mode",
                        description = "Full app exploration and batch learning",
                        checked = forceExploration,
                        onCheckedChange = {
                            forceExploration = it
                            LearnAppConfig.DeveloperSettings.forceEnableExploration = it
                        }
                    )

                    FeatureToggle(
                        title = "Framework Detection",
                        description = "Detect Flutter, Unity, React Native, WebView",
                        checked = forceFrameworkDetection,
                        onCheckedChange = {
                            forceFrameworkDetection = it
                            LearnAppConfig.DeveloperSettings.forceEnableFrameworkDetection = it
                        }
                    )

                    FeatureToggle(
                        title = "Caching",
                        description = "Cache screen states for performance",
                        checked = forceCaching,
                        onCheckedChange = {
                            forceCaching = it
                            LearnAppConfig.DeveloperSettings.forceEnableCaching = it
                        }
                    )

                    FeatureToggle(
                        title = "Analytics",
                        description = "Track usage and command metrics",
                        checked = forceAnalytics,
                        onCheckedChange = {
                            forceAnalytics = it
                            LearnAppConfig.DeveloperSettings.forceEnableAnalytics = it
                        }
                    )

                    FeatureToggle(
                        title = "Debug Overlay",
                        description = "Show debug information overlay",
                        checked = enableDebugOverlay,
                        onCheckedChange = {
                            enableDebugOverlay = it
                            LearnAppConfig.DeveloperSettings.enableDebugOverlay = it
                        }
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
                                onClick = {
                                    selectedProcessingMode = mode
                                    LearnAppConfig.DeveloperSettings.processingModeOverride = mode
                                }
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

                // Current Config Summary
                SettingsSection(title = "Current Configuration", icon = Icons.Default.Info) {
                    val summary = LearnAppConfig.getSummary()
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    )
                }
            }
        }
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
                            if (enabled) "Overrides active" else "Tap to enable",
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
