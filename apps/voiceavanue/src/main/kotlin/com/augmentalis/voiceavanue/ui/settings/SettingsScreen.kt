/**
 * SettingsScreen.kt - App settings and configuration
 *
 * Configure VoiceOS, WebAvanue, and VoiceCursor modules.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Settings state
    var dwellClickEnabled by remember { mutableStateOf(true) }
    var dwellClickDelay by remember { mutableStateOf(1500f) }
    var cursorSmoothing by remember { mutableStateOf(true) }
    var voiceFeedback by remember { mutableStateOf(true) }
    var autoStartOnBoot by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Permissions Section
            item {
                SettingsSectionHeader(title = "Permissions")
            }

            item {
                SettingsItem(
                    title = "Accessibility Service",
                    subtitle = "Required for voice control and screen reading",
                    icon = Icons.Default.Accessibility,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }

            item {
                SettingsItem(
                    title = "Overlay Permission",
                    subtitle = "Required for cursor and HUD display",
                    icon = Icons.Default.Layers,
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
            }

            // VoiceCursor Section
            item {
                SettingsSectionHeader(title = "VoiceCursor")
            }

            item {
                SettingsSwitch(
                    title = "Dwell Click",
                    subtitle = "Auto-click when cursor stays still",
                    icon = Icons.Default.TouchApp,
                    checked = dwellClickEnabled,
                    onCheckedChange = { dwellClickEnabled = it }
                )
            }

            item {
                SettingsSlider(
                    title = "Dwell Click Delay",
                    subtitle = "${dwellClickDelay.toInt()} ms",
                    icon = Icons.Default.Timer,
                    value = dwellClickDelay,
                    valueRange = 500f..3000f,
                    onValueChange = { dwellClickDelay = it }
                )
            }

            item {
                SettingsSwitch(
                    title = "Cursor Smoothing",
                    subtitle = "Reduce cursor jitter",
                    icon = Icons.Default.Tune,
                    checked = cursorSmoothing,
                    onCheckedChange = { cursorSmoothing = it }
                )
            }

            // Voice Section
            item {
                SettingsSectionHeader(title = "Voice Control")
            }

            item {
                SettingsSwitch(
                    title = "Voice Feedback",
                    subtitle = "Speak command confirmations",
                    icon = Icons.Default.RecordVoiceOver,
                    checked = voiceFeedback,
                    onCheckedChange = { voiceFeedback = it }
                )
            }

            item {
                SettingsItem(
                    title = "Voice Commands",
                    subtitle = "View and customize commands",
                    icon = Icons.Default.List,
                    onClick = { /* TODO: Navigate to commands list */ }
                )
            }

            item {
                SettingsItem(
                    title = "Wake Word",
                    subtitle = "Configure activation phrase",
                    icon = Icons.Default.Mic,
                    onClick = { /* TODO: Wake word settings */ }
                )
            }

            // Browser Section
            item {
                SettingsSectionHeader(title = "WebAvanue Browser")
            }

            item {
                SettingsItem(
                    title = "Search Engine",
                    subtitle = "Google",
                    icon = Icons.Default.Search,
                    onClick = { /* TODO: Search engine picker */ }
                )
            }

            item {
                SettingsItem(
                    title = "Custom Search Engines",
                    subtitle = "Add your own search providers",
                    icon = Icons.Default.Add,
                    onClick = { /* TODO: Custom search engine list */ }
                )
            }

            // System Section
            item {
                SettingsSectionHeader(title = "System")
            }

            item {
                SettingsSwitch(
                    title = "Start on Boot",
                    subtitle = "Launch AVA when device starts",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = autoStartOnBoot,
                    onCheckedChange = { autoStartOnBoot = it }
                )
            }

            item {
                SettingsItem(
                    title = "RPC Servers",
                    subtitle = "VoiceOS: 50051, WebAvanue: 50055",
                    icon = Icons.Default.Hub,
                    onClick = { /* TODO: RPC server status */ }
                )
            }

            // About Section
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0-alpha01",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }

            item {
                SettingsItem(
                    title = "Licenses",
                    subtitle = "Open source licenses",
                    icon = Icons.Default.Description,
                    onClick = { /* TODO: Show licenses */ }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = AvanueTheme.colors.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AvanueTheme.colors.textSecondary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AvanueTheme.colors.textSecondary
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun SettingsSlider(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AvanueTheme.colors.textSecondary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}
