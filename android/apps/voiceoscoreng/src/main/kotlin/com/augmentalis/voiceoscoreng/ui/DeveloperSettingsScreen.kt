package com.augmentalis.voiceoscoreng.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.commandmanager.LearnAppConfig
import com.augmentalis.commandmanager.LearnAppDevToggle

/**
 * Developer settings screen for VoiceOSCoreNG test app.
 *
 * Provides controls for:
 * - Test mode toggle
 * - Tier selection (LITE/PRO/DEV)
 * - Feature flags
 * - Scanning controls
 * - Debug options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onDismiss: () -> Unit,
    scanningCallbacks: ScanningCallbacks = ScanningCallbacks()
) {
    var testModeEnabled by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.enabled) }
    var currentTier by remember { mutableStateOf(LearnAppDevToggle.getCurrentTier()) }
    var continuousMonitoring by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Developer Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Mode Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Test Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Test Mode")
                        Text(
                            "Unlock all features for testing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = testModeEnabled,
                        onCheckedChange = { enabled ->
                            testModeEnabled = enabled
                            if (enabled) {
                                LearnAppConfig.enableTestMode()
                            } else {
                                LearnAppConfig.reset()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tier Selection
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Tier Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LearnAppDevToggle.Tier.entries.forEach { tier ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTier == tier,
                            onClick = {
                                currentTier = tier
                                LearnAppDevToggle.setTier(tier)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(tier.name)
                            Text(
                                when (tier) {
                                    LearnAppDevToggle.Tier.LITE -> "Basic features only"
                                    LearnAppDevToggle.Tier.DEV -> "All features + debug tools"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanning Controls
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Scanning Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Monitoring")
                        Text(
                            "Auto-scan on screen change",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = continuousMonitoring,
                        onCheckedChange = { enabled ->
                            continuousMonitoring = enabled
                            scanningCallbacks.onSetContinuousMonitoring(enabled)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = scanningCallbacks.onRescanCurrentApp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rescan App")
                    }
                    OutlinedButton(
                        onClick = scanningCallbacks.onRescanEverything,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feature Flags (read-only display)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Feature Flags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val config = LearnAppConfig.getConfig()
                val features = listOf(
                    "AI Processing" to config.enableAI,
                    "NLU Processing" to config.enableNLU,
                    "Exploration Mode" to config.enableExploration,
                    "Framework Detection" to config.enableFrameworkDetection,
                    "Caching" to config.cacheEnabled,
                    "Analytics" to config.analyticsEnabled,
                    "Debug Overlay" to config.enableDebugOverlay
                )

                features.forEach { (name, enabled) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name)
                        Icon(
                            if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (enabled) "Enabled" else "Disabled",
                            tint = if (enabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
