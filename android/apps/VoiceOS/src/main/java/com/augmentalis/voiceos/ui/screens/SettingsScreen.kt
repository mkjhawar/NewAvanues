/**
 * SettingsScreen.kt - Settings for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.BuildConfig
import com.augmentalis.voiceos.util.AccessibilityServiceHelper
import com.augmentalis.voiceos.viewmodel.SettingsViewModel
import com.augmentalis.voiceoscoreng.service.VoiceOSAccessibilityService
import org.koin.androidx.compose.koinViewModel

/**
 * Settings screen for VoiceOS configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var showWakeWordDialog by remember { mutableStateOf(false) }
    var showVoiceEngineDialog by remember { mutableStateOf(false) }
    var showLearnedAppsDialog by remember { mutableStateOf(false) }
    var showRescanEverythingDialog by remember { mutableStateOf(false) }

    // Wake Word Dialog
    if (showWakeWordDialog) {
        AlertDialog(
            onDismissRequest = { showWakeWordDialog = false },
            title = { Text("Wake Word") },
            text = {
                Column {
                    Text(
                        text = "The current wake word is 'Hey Ava'.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Custom wake words will be available in a future update. Stay tuned!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWakeWordDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Voice Engine Dialog
    if (showVoiceEngineDialog) {
        val engines = listOf(
            "Default" to "System default speech recognition",
            "Google Cloud" to "High accuracy, requires internet",
            "Vosk" to "Offline recognition, good accuracy",
            "Whisper" to "AI-powered, best accuracy (offline)"
        )
        AlertDialog(
            onDismissRequest = { showVoiceEngineDialog = false },
            title = { Text("Voice Engine") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    engines.forEach { (name, description) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setVoiceEngine(name) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.voiceEngine == name,
                                onClick = { viewModel.setVoiceEngine(name) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium
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
            },
            confirmButton = {
                TextButton(onClick = { showVoiceEngineDialog = false }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceEngineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Learned Apps Dialog
    if (showLearnedAppsDialog) {
        AlertDialog(
            onDismissRequest = { showLearnedAppsDialog = false },
            title = { Text("Learned Apps") },
            text = {
                Column {
                    Text(
                        text = "No apps have been learned yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Use 'Learn Apps' from the home screen to teach VoiceOS about your apps and create custom voice commands.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLearnedAppsDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLearnedAppsDialog = false
                        launchLearnAppFromSettings(context)
                    }
                ) {
                    Text("Open LearnApp")
                }
            }
        )
    }

    // Rescan Everything Confirmation Dialog
    if (showRescanEverythingDialog) {
        AlertDialog(
            onDismissRequest = { showRescanEverythingDialog = false },
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
                        showRescanEverythingDialog = false
                        VoiceOSAccessibilityService.rescanEverything()
                        Toast.makeText(context, "All screens cleared. Will rescan on next visit.", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanEverythingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Voice Settings Section
            SettingsSection(title = "Voice") {
                SettingsItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Wake Word",
                    subtitle = "Hey Ava",
                    onClick = { showWakeWordDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.Mic,
                    title = "Voice Engine",
                    subtitle = settings.voiceEngine,
                    onClick = { showVoiceEngineDialog = true }
                )

                SettingsToggleItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Continuous Listening",
                    subtitle = "Keep listening after command",
                    checked = settings.continuousListening,
                    onCheckedChange = { viewModel.setContinuousListening(it) }
                )
            }

            // Accessibility Section
            SettingsSection(title = "Accessibility") {
                SettingsItem(
                    icon = Icons.Default.Accessibility,
                    title = "Accessibility Settings",
                    subtitle = "Enable/disable VoiceOS service",
                    onClick = {
                        AccessibilityServiceHelper.openAccessibilitySettings(context)
                    }
                )

                SettingsItem(
                    icon = Icons.Default.Apps,
                    title = "Learned Apps",
                    subtitle = "Manage app-specific commands",
                    onClick = { showLearnedAppsDialog = true }
                )
            }

            // Feedback Section
            SettingsSection(title = "Feedback") {
                SettingsToggleItem(
                    icon = Icons.Default.Palette,
                    title = "Visual Feedback",
                    subtitle = "Show overlays when listening",
                    checked = settings.visualFeedback,
                    onCheckedChange = { viewModel.setVisualFeedback(it) }
                )

                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Audio Feedback",
                    subtitle = "Play sounds for actions",
                    checked = settings.audioFeedback,
                    onCheckedChange = { viewModel.setAudioFeedback(it) }
                )
            }

            // Scanning Section
            SettingsSection(title = "Scanning") {
                SettingsToggleItem(
                    icon = Icons.Default.Search,
                    title = "Continuous Monitoring",
                    subtitle = "Auto-scan when screen changes",
                    checked = settings.continuousScanningEnabled,
                    onCheckedChange = { viewModel.setContinuousScanningEnabled(it) }
                )

                SettingsToggleItem(
                    icon = Icons.Default.Code,
                    title = "Developer Mode",
                    subtitle = "Show developer options",
                    checked = settings.developerModeEnabled,
                    onCheckedChange = { viewModel.setDeveloperModeEnabled(it) }
                )
            }

            // Developer Options Section (conditionally visible)
            if (settings.developerModeEnabled) {
                SettingsSection(title = "Developer Options") {
                    SettingsToggleItem(
                        icon = Icons.Default.Visibility,
                        title = "Show Slider Drawer",
                        subtitle = "Manual scan control overlay",
                        checked = settings.showSliderDrawer,
                        onCheckedChange = { viewModel.setShowSliderDrawer(it) }
                    )

                    SettingsItem(
                        icon = Icons.Default.Refresh,
                        title = "Rescan Current App",
                        subtitle = "Clear cache for current app",
                        onClick = {
                            VoiceOSAccessibilityService.rescanCurrentApp()
                            Toast.makeText(context, "Current app cache cleared. Will rescan on next screen change.", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsItem(
                        icon = Icons.Default.Build,
                        title = "Rescan Everything",
                        subtitle = "Clear all cached screen data",
                        onClick = { showRescanEverythingDialog = true }
                    )
                }
            }

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$title icon",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate to $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val toggleState = if (checked) "enabled" else "disabled"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, $subtitle, currently $toggleState"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$title icon",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

/**
 * Launch LearnApp from settings with proper error handling.
 *
 * Attempts to launch the LearnApp activity. If LearnApp is not installed,
 * shows a Toast message informing the user.
 *
 * @param context Android context for launching activities and showing Toast
 */
private fun launchLearnAppFromSettings(context: android.content.Context) {
    try {
        val intent = Intent().apply {
            setClassName(
                "com.augmentalis.learnapp",
                "com.augmentalis.learnapp.LearnAppActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "LearnApp is not installed. Please install it to learn apps.",
            Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Failed to launch LearnApp: ${e.message ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
    }
}
