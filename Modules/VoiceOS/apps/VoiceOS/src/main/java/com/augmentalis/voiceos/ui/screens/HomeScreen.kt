/**
 * HomeScreen.kt - Main dashboard for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.ui.theme.VoiceOSColors
import com.augmentalis.voiceos.util.AccessibilityServiceHelper
import com.augmentalis.voiceos.util.rememberAccessibilityServiceState
import com.augmentalis.voiceos.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Home screen showing VoiceOS status and quick actions.
 */
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val isServiceEnabled by rememberAccessibilityServiceState(context)
    val uiState by viewModel.uiState.collectAsState()
    var showCommandsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Commands Dialog
    if (showCommandsDialog) {
        AlertDialog(
            onDismissRequest = { showCommandsDialog = false },
            title = { Text("Voice Commands") },
            text = {
                Column {
                    Text(
                        text = "Available commands will appear here after you learn apps.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Use 'Learn Apps' to teach VoiceOS about your apps and discover available voice commands.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommandsDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Support") },
            text = {
                Column {
                    Text(
                        text = "Getting Started with VoiceOS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Enable the VoiceOS accessibility service\n" +
                            "2. Say 'Hey Ava' to activate voice commands\n" +
                            "3. Use 'Learn Apps' to teach VoiceOS about your apps\n" +
                            "4. Try saying 'scroll down' or 'go back'",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "For more help, visit our documentation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showHelpDialog = false
                        val helpUrl = "https://augmentalis.com/voiceos/help"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Docs")
                }
            }
        )
    }

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
                    launchLearnApp(context)
                },
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Commands",
                onClick = { showCommandsDialog = true },
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
        StatisticsCard(
            appsLearned = uiState.appsLearned,
            commandsAvailable = uiState.commandsAvailable,
            commandsToday = uiState.commandsToday,
            isLoading = uiState.isLoading
        )

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
                    contentDescription = "Accessibility settings icon",
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
            onClick = { showHelpDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Help,
                contentDescription = "Help and support",
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
                contentDescription = if (isServiceEnabled) "Voice active" else "Voice inactive",
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
                    contentDescription = "Open accessibility settings",
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
                contentDescription = label,
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
private fun StatisticsCard(
    appsLearned: Long,
    commandsAvailable: Long,
    commandsToday: Long,
    isLoading: Boolean
) {
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

            if (isLoading) {
                Text(
                    text = "Loading statistics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                StatRow(label = "Apps Learned", value = appsLearned.toString())
                StatRow(label = "Commands Available", value = commandsAvailable.toString())
                StatRow(label = "Commands Today", value = commandsToday.toString())
            }
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

/**
 * Launch LearnApp with proper error handling.
 *
 * Attempts to launch the LearnApp activity. If LearnApp is not installed,
 * shows a Toast message informing the user and attempts to open the Play Store.
 *
 * @param context Android context for launching activities and showing Toast
 */
private fun launchLearnApp(context: android.content.Context) {
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
        showLearnAppNotInstalledError(context)
    } catch (e: SecurityException) {
        Toast.makeText(
            context,
            "Permission denied to launch LearnApp",
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

/**
 * Show error when LearnApp is not installed.
 *
 * Displays a Toast message and attempts to open the Play Store listing.
 *
 * @param context Android context for launching activities and showing Toast
 */
private fun showLearnAppNotInstalledError(context: android.content.Context) {
    Toast.makeText(
        context,
        "LearnApp is not installed. Opening Play Store...",
        Toast.LENGTH_LONG
    ).show()

    try {
        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.augmentalis.learnapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(playStoreIntent)
    } catch (e: ActivityNotFoundException) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.augmentalis.learnapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Toast.makeText(
                context,
                "LearnApp is not installed. Please install it from the Play Store.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
