/**
 * HomeScreen.kt - Main dashboard for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.augmentalis.voiceos.ui.theme.VoiceOSColors
import com.augmentalis.voiceos.util.AccessibilityServiceHelper

/**
 * Home screen showing VoiceOS status and quick actions.
 */
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isServiceEnabled by remember { mutableStateOf(false) }

    // Refresh service status on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = AccessibilityServiceHelper.isVoiceOSServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial check
    isServiceEnabled = AccessibilityServiceHelper.isVoiceOSServiceEnabled(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        StatusCard(
            isServiceEnabled = isServiceEnabled,
            onOpenAccessibilitySettings = {
                AccessibilityServiceHelper.openAccessibilitySettings(context)
            }
        )

        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.School,
                label = "Learn Apps",
                onClick = {
                    // Launch LearnApp
                    try {
                        val intent = Intent().apply {
                            setClassName(
                                "com.augmentalis.learnapp",
                                "com.augmentalis.learnapp.LearnAppActivity"
                            )
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // LearnApp not installed - could show a message
                    }
                },
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.List,
                label = "Commands",
                onClick = { /* TODO: Show commands list */ },
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Accessibility,
                label = "Accessibility",
                onClick = {
                    AccessibilityServiceHelper.openAccessibilitySettings(context)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Statistics Card
        StatisticsCard()

        // Accessibility Settings Button (Always Visible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accessibility Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Manage VoiceOS accessibility service",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        AccessibilityServiceHelper.openAccessibilitySettings(context)
                    }
                ) {
                    Text("Open")
                }
            }
        }

        // Help Section
        OutlinedButton(
            onClick = { /* TODO: Show help */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Help,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Help & Support")
        }
    }
}

@Composable
private fun StatusCard(
    isServiceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceEnabled)
                VoiceOSColors.StatusActive.copy(alpha = 0.1f)
            else
                VoiceOSColors.StatusInactive.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isServiceEnabled)
                    VoiceOSColors.StatusActive
                else
                    VoiceOSColors.StatusInactive
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isServiceEnabled) "Voice Active" else "Voice Inactive",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isServiceEnabled)
                    VoiceOSColors.StatusActive
                else
                    VoiceOSColors.StatusInactive
            )

            Text(
                text = if (isServiceEnabled)
                    "Say 'Hey Ava' to start"
                else
                    "Enable accessibility service to use voice commands",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onOpenAccessibilitySettings,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Accessibility Settings")
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun StatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatRow(label = "Apps Learned", value = "0")
            StatRow(label = "Commands Available", value = "0")
            StatRow(label = "Commands Today", value = "0")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
