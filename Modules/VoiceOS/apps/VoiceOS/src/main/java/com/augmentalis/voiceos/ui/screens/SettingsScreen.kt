/**
 * SettingsScreen.kt - Settings for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.BuildConfig
import com.augmentalis.voiceos.util.AccessibilityServiceHelper

/**
 * Settings screen for VoiceOS configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                    onClick = { /* TODO: Configure wake word */ }
                )

                SettingsItem(
                    icon = Icons.Default.Mic,
                    title = "Voice Engine",
                    subtitle = "Default",
                    onClick = { /* TODO: Select voice engine */ }
                )

                var continuousListening by remember { mutableStateOf(false) }
                SettingsToggleItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Continuous Listening",
                    subtitle = "Keep listening after command",
                    checked = continuousListening,
                    onCheckedChange = { continuousListening = it }
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
                    onClick = { /* TODO: Show learned apps */ }
                )
            }

            // Feedback Section
            SettingsSection(title = "Feedback") {
                var visualFeedback by remember { mutableStateOf(true) }
                SettingsToggleItem(
                    icon = Icons.Default.Palette,
                    title = "Visual Feedback",
                    subtitle = "Show overlays when listening",
                    checked = visualFeedback,
                    onCheckedChange = { visualFeedback = it }
                )

                var audioFeedback by remember { mutableStateOf(true) }
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Audio Feedback",
                    subtitle = "Play sounds for actions",
                    checked = audioFeedback,
                    onCheckedChange = { audioFeedback = it }
                )
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
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
            contentDescription = null,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
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
