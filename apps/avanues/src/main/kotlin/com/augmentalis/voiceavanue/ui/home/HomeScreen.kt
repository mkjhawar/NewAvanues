/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.components.AvanueChip
import com.augmentalis.avanueui.components.AvanueSurface
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.foundation.state.LastHeardCommand
import com.augmentalis.foundation.state.ServiceState
import com.augmentalis.voiceoscore.CommandActionType

/**
 * Aggregated callbacks for all command management actions.
 */
data class CommandCallbacks(
    val onToggleStaticCommand: (String) -> Unit = {},
    val onAddCustomCommand: (String, List<String>, CommandActionType, String, List<MacroStep>) -> Unit = { _, _, _, _, _ -> },
    val onRemoveCustomCommand: (String) -> Unit = {},
    val onToggleCustomCommand: (String) -> Unit = {},
    val onAddSynonym: (String, List<String>) -> Unit = { _, _ -> },
    val onRemoveSynonym: (String) -> Unit = {}
)

/**
 * Main home screen for the Avanues app.
 * Reactive Service Bus dashboard showing module status, system health, and commands.
 * Landscape: 3-column no-scroll layout (smart glasses compatible).
 * Portrait: fixed top + scrollable commands.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCommands: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val commandCallbacks = remember(viewModel) {
        CommandCallbacks(
            onToggleStaticCommand = viewModel::toggleCommand,
            onAddCustomCommand = { name, phrases, actionType, target, steps ->
                viewModel.addCustomCommand(name, phrases, actionType, target, steps)
            },
            onRemoveCustomCommand = viewModel::removeCustomCommand,
            onToggleCustomCommand = viewModel::toggleCustomCommand,
            onAddSynonym = viewModel::addSynonym,
            onRemoveSynonym = viewModel::removeSynonym
        )
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            AvanueTheme.colors.background,
            AvanueTheme.colors.surface.copy(alpha = 0.6f),
            AvanueTheme.colors.background
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
    ) {
        val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

        if (isLandscape) {
            DashboardLandscape(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onNavigateToBrowser = onNavigateToBrowser,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToCommands = onNavigateToCommands,
                callbacks = commandCallbacks
            )
        } else {
            DashboardPortrait(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onNavigateToBrowser = onNavigateToBrowser,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToCommands = onNavigateToCommands,
                callbacks = commandCallbacks
            )
        }
    }
}

/**
 * Full-screen Commands management screen.
 * Launched from the CommandsSummaryCard on the dashboard.
 */
@Composable
fun CommandsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val callbacks = remember(viewModel) {
        CommandCallbacks(
            onToggleStaticCommand = viewModel::toggleCommand,
            onAddCustomCommand = { name, phrases, actionType, target, steps ->
                viewModel.addCustomCommand(name, phrases, actionType, target, steps)
            },
            onRemoveCustomCommand = viewModel::removeCustomCommand,
            onToggleCustomCommand = viewModel::toggleCustomCommand,
            onAddSynonym = viewModel::addSynonym,
            onRemoveSynonym = viewModel::removeSynonym
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AvanueTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = SpacingTokens.md)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SpacingTokens.sm, bottom = SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AvanueTheme.colors.textPrimary
                )
            }
            Text(
                text = "Voice Commands",
                style = MaterialTheme.typography.headlineSmall,
                color = AvanueTheme.colors.textPrimary
            )
        }

        CommandsSection(
            commands = uiState.commands,
            callbacks = callbacks,
            modifier = Modifier.weight(1f)
        )
    }
}

// ──────────────────────────── LANDSCAPE ────────────────────────────

@Composable
private fun DashboardLandscape(
    uiState: DashboardUiState,
    onNavigateBack: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCommands: () -> Unit,
    callbacks: CommandCallbacks
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingTokens.md)
    ) {
        // Header with back + title
        DashboardHeader(
            onNavigateBack = onNavigateBack,
            onNavigateToSettings = onNavigateToSettings,
            serviceRunning = uiState.permissions.accessibilityEnabled
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = SpacingTokens.sm),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md)
        ) {
            // Column 1: MODULES
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
            ) {
                SectionLabel("MODULES")
                uiState.modules.forEach { module ->
                    ModuleCard(
                        module = module,
                        onClick = {
                            when (module.moduleId) {
                                "webavanue" -> onNavigateToBrowser()
                                else -> onNavigateToSettings()
                            }
                        }
                    )
                }
            }

            // Column 2: SYSTEM + LAST HEARD
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
            ) {
                SectionLabel("SYSTEM")
                SystemHealthBar(permissions = uiState.permissions)
                if (uiState.hasLastCommand) {
                    LastHeardCard(command = uiState.lastHeardCommand)
                }
            }

            // Column 3: COMMANDS
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
            ) {
                SectionLabel("COMMANDS")
                CommandsSummaryCard(commands = uiState.commands, onClick = onNavigateToCommands)
            }
        }
    }
}

// ──────────────────────────── PORTRAIT ────────────────────────────

@Composable
private fun DashboardPortrait(
    uiState: DashboardUiState,
    onNavigateBack: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCommands: () -> Unit,
    callbacks: CommandCallbacks
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.md)
            .navigationBarsPadding()
    ) {
        // Rich header with back button, title, status, and settings
        DashboardHeader(
            onNavigateBack = onNavigateBack,
            onNavigateToSettings = onNavigateToSettings,
            serviceRunning = uiState.permissions.accessibilityEnabled
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
        ) {
            // Module cards section
            SectionLabel("MODULES")

            uiState.voiceAvanue?.let { module ->
                ModuleCard(module = module, onClick = onNavigateToSettings)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
            ) {
                uiState.webAvanue?.let { module ->
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleCard(module = module, onClick = onNavigateToBrowser)
                    }
                }
                uiState.voiceCursor?.let { module ->
                    Box(modifier = Modifier.weight(1f)) {
                        ModuleCard(module = module, onClick = onNavigateToSettings)
                    }
                }
            }

            // System health section
            SectionLabel("SYSTEM")
            SystemHealthBar(permissions = uiState.permissions)

            if (uiState.hasLastCommand) {
                LastHeardCard(command = uiState.lastHeardCommand)
            }

            // Commands summary
            SectionLabel("COMMANDS")
            CommandsSummaryCard(commands = uiState.commands, onClick = onNavigateToCommands)

            Spacer(modifier = Modifier.height(SpacingTokens.md))
        }
    }
}

// ──────────────────────────── HEADER ────────────────────────────

@Composable
private fun DashboardHeader(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    serviceRunning: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SpacingTokens.sm, bottom = SpacingTokens.md),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = AvanueTheme.colors.textPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "VoiceOS\u00AE Avanues",
                style = MaterialTheme.typography.headlineSmall,
                color = AvanueTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (serviceRunning) AvanueTheme.colors.success
                            else AvanueTheme.colors.error
                        )
                )
                Text(
                    text = if (serviceRunning) "Service active" else "Service inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = AvanueTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = AvanueTheme.colors.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = SpacingTokens.xs)
    )
}

// ──────────────────────────── MODULE CARD ────────────────────────────

/**
 * Returns the status border color: green=running, blue=ready, orange=degraded, red=error, red=stopped.
 */
@Composable
private fun statusBorderColor(state: ServiceState): androidx.compose.ui.graphics.Color {
    return when (state) {
        is ServiceState.Running -> AvanueTheme.colors.success
        is ServiceState.Ready -> AvanueTheme.colors.info
        is ServiceState.Degraded -> AvanueTheme.colors.warning
        is ServiceState.Error -> AvanueTheme.colors.error
        is ServiceState.Stopped -> AvanueTheme.colors.error
    }
}

/**
 * Returns a human-readable status label and color for a ServiceState.
 */
@Composable
private fun statusLabel(state: ServiceState): Pair<String, androidx.compose.ui.graphics.Color> {
    return when (state) {
        is ServiceState.Running -> "ON" to AvanueTheme.colors.success
        is ServiceState.Ready -> "READY" to AvanueTheme.colors.info
        is ServiceState.Degraded -> "DEGRADED" to AvanueTheme.colors.warning
        is ServiceState.Error -> "ERROR" to AvanueTheme.colors.error
        is ServiceState.Stopped -> "OFF" to AvanueTheme.colors.error
    }
}

/**
 * Returns a recognizable icon for each module type.
 */
private fun moduleIcon(moduleId: String): ImageVector {
    return when (moduleId) {
        "voiceavanue" -> Icons.Default.Mic
        "webavanue" -> Icons.Default.Public
        "voicecursor" -> Icons.Default.Mouse
        else -> Icons.Default.Apps
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ModuleCard(module: ModuleStatus, onClick: () -> Unit) {
    val borderColor = statusBorderColor(module.state)
    val icon = moduleIcon(module.moduleId)
    val (statusText, statusColor) = statusLabel(module.state)
    val isStopped = module.state is ServiceState.Stopped
    val contentAlpha = if (isStopped) 0.6f else 1f

    AvanueCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 200.dp
            val cardPadding = if (isCompact) SpacingTokens.sm else SpacingTokens.md

            Row(
                modifier = Modifier.fillMaxWidth().padding(cardPadding),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Module icon with status dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 32.dp else 40.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = borderColor.copy(alpha = contentAlpha),
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(borderColor)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = module.displayName,
                            style = if (isCompact) MaterialTheme.typography.labelLarge
                                   else MaterialTheme.typography.titleSmall,
                            color = AvanueTheme.colors.textPrimary.copy(alpha = contentAlpha),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary.copy(alpha = contentAlpha),
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (module.metadata.isNotEmpty()) {
                        Text(
                            text = module.metadata.entries.joinToString(" \u00B7 ") { "${it.key}: ${it.value}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = AvanueTheme.colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = AvanueTheme.colors.textDisabled,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ──────────────────────────── SYSTEM HEALTH ────────────────────────────

@Composable
private fun SystemHealthBar(permissions: PermissionStatus) {
    val context = LocalContext.current

    // Microphone: runtime permission dialog (falls back to App Info if permanently denied)
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled by onResume re-check in DashboardViewModel */ }

    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
    ) {
        if (permissions.allGranted) {
            AvanueCard(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AvanueTheme.colors.success.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, "All OK", tint = AvanueTheme.colors.success, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            "All permissions granted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "System ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.success
                        )
                    }
                }
            }
        } else {
            Text(
                "PERMISSIONS REQUIRED",
                style = MaterialTheme.typography.labelMedium,
                color = AvanueTheme.colors.error,
                fontWeight = FontWeight.Bold
            )
            if (!permissions.microphoneGranted) {
                PermissionErrorCard("Microphone", "Tap to grant microphone access") {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            if (!permissions.accessibilityEnabled) {
                PermissionErrorCard("Accessibility Service", "Find and enable VoiceOS\u00AE") {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        Toast.makeText(
                            context,
                            "Find \"VoiceOS\u00AE\" under Downloaded/Installed services and enable it",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            }
            if (!permissions.overlayEnabled) {
                PermissionErrorCard("Display Over Apps", "Required for voice cursor") {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            }
            if (!permissions.notificationsEnabled) {
                PermissionErrorCard("Notifications", "Required for service status alerts") {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionErrorCard(title: String, description: String, onClick: () -> Unit) {
    AvanueCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AvanueTheme.colors.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AvanueTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(description, style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open settings", tint = AvanueTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

// ──────────────────────────── LAST HEARD ────────────────────────────

@Composable
private fun LastHeardCard(command: LastHeardCommand) {
    val timeAgo = remember(command.timestampMs) { formatTimeAgo(command.timestampMs) }
    AvanueCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AvanueTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = AvanueTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "\"${command.phrase}\"",
                    style = MaterialTheme.typography.titleSmall,
                    color = AvanueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md)) {
                    Text(
                        "${(command.confidence * 100).toInt()}% match",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.success
                    )
                    Text(timeAgo, style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.textTertiary)
                }
            }
        }
    }
}

// ──────────────────────────── COMMANDS SUMMARY ────────────────────────────

/**
 * Compact card showing command counts with tap-to-open-full-screen.
 * Replaces the inline CommandsSection for space efficiency.
 */
@Composable
private fun CommandsSummaryCard(commands: CommandsUiState, onClick: () -> Unit) {
    AvanueCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${commands.staticCount + commands.customCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AvanueTheme.colors.info,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "voice commands",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AvanueTheme.colors.textPrimary
                    )
                }
                Text(
                    text = "${commands.staticCount} static \u00B7 ${commands.customCount} custom \u00B7 ${commands.synonymCount} verbs",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open commands",
                tint = AvanueTheme.colors.info,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ──────────────────────────── COMMANDS (FULL SCREEN) ────────────────────────────

@Composable
private fun CommandsSection(
    commands: CommandsUiState,
    callbacks: CommandCallbacks,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
            CommandTab("Static", 0, selectedTab) { selectedTab = 0 }
            CommandTab("App", 1, selectedTab) { selectedTab = 1 }
            CommandTab("+ Custom", 2, selectedTab) { selectedTab = 2 }
        }

        when (selectedTab) {
            0 -> StaticCommandsTab(
                categories = commands.staticCategories,
                synonymEntries = commands.synonymEntries,
                onToggleCommand = callbacks.onToggleStaticCommand,
                onAddSynonym = callbacks.onAddSynonym,
                onRemoveSynonym = callbacks.onRemoveSynonym
            )
            1 -> DynamicCommandsInfoTab(dynamicCount = commands.dynamicCount)
            2 -> CustomCommandsTab(
                customCommands = commands.customCommands,
                onAdd = callbacks.onAddCustomCommand,
                onRemove = callbacks.onRemoveCustomCommand,
                onToggle = callbacks.onToggleCustomCommand
            )
        }
    }
}

@Composable
private fun CommandTab(label: String, index: Int, selectedTab: Int, onClick: () -> Unit) {
    val isSelected = index == selectedTab
    AvanueChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) AvanueTheme.colors.primary else AvanueTheme.colors.textSecondary
            )
        }
    )
}

// ──────────────── TAB 0: STATIC COMMANDS ────────────────

@Composable
private fun StaticCommandsTab(
    categories: List<CommandCategory>,
    synonymEntries: List<SynonymEntryInfo>,
    onToggleCommand: (String) -> Unit,
    onAddSynonym: (String, List<String>) -> Unit,
    onRemoveSynonym: (String) -> Unit
) {
    if (categories.isEmpty() && synonymEntries.isEmpty()) {
        EmptyStateMessage("No static commands loaded")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            items(categories, key = { it.name }) { category ->
                ExpandableCommandCategory(
                    category = category,
                    onToggleCommand = onToggleCommand,
                    onAddSynonym = onAddSynonym
                )
            }
            if (synonymEntries.isNotEmpty()) {
                item(key = "verb_synonyms") {
                    VerbSynonymsCategory(
                        entries = synonymEntries,
                        onAddSynonym = onAddSynonym,
                        onRemoveSynonym = onRemoveSynonym
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableCommandCategory(
    category: CommandCategory,
    onToggleCommand: (String) -> Unit,
    onAddSynonym: (String, List<String>) -> Unit = { _, _ -> }
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
        AvanueSurface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.name, style = MaterialTheme.typography.titleSmall, color = AvanueTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                Text("${category.commands.size}", style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded) "Collapse" else "Expand",
                    tint = AvanueTheme.colors.textSecondary, modifier = Modifier.size(20.dp)
                )
            }
        }
        if (expanded) {
            Column(Modifier.fillMaxWidth().padding(start = SpacingTokens.md), verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
                category.commands.forEach { command ->
                    CommandRow(
                        command = command,
                        onToggle = { onToggleCommand(command.id) },
                        onAddAlias = { alias -> onAddSynonym(command.phrase, listOf(alias)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandRow(
    command: StaticCommand,
    onToggle: () -> Unit,
    onAddAlias: (String) -> Unit = {}
) {
    var showAliasField by remember { mutableStateOf(false) }
    var aliasText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = command.enabled, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = AvanueTheme.colors.success, uncheckedColor = AvanueTheme.colors.textSecondary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(command.phrase, style = MaterialTheme.typography.bodyMedium, color = AvanueTheme.colors.textPrimary)
                if (command.description.isNotEmpty()) {
                    Text(command.description, style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
                }
                if (command.synonyms.isNotEmpty()) {
                    Text(
                        "Also: ${command.synonyms.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textDisabled
                    )
                }
            }
            AvanueChip(
                onClick = { showAliasField = !showAliasField },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = AvanueTheme.colors.primary)
                        Text("Alias", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.primary)
                    }
                }
            )
        }
        AnimatedVisibility(visible = showAliasField) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, top = SpacingTokens.xs, bottom = SpacingTokens.xs),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = { Text("New alias phrase") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AvanueTheme.colors.textPrimary,
                        unfocusedTextColor = AvanueTheme.colors.textPrimary,
                        focusedBorderColor = AvanueTheme.colors.primary,
                        unfocusedBorderColor = AvanueTheme.colors.textDisabled,
                        focusedLabelColor = AvanueTheme.colors.primary,
                        unfocusedLabelColor = AvanueTheme.colors.textSecondary,
                        cursorColor = AvanueTheme.colors.primary
                    )
                )
                TextButton(
                    onClick = {
                        val trimmed = aliasText.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddAlias(trimmed)
                            aliasText = ""
                            showAliasField = false
                        }
                    }
                ) {
                    Text("Add", color = AvanueTheme.colors.primary)
                }
            }
        }
    }
}

// ──────────────── TAB 1: DYNAMIC APP COMMANDS ────────────────

@Composable
private fun DynamicCommandsInfoTab(dynamicCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(SpacingTokens.lg),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            tint = AvanueTheme.colors.textSecondary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Dynamic App Commands",
            style = MaterialTheme.typography.titleMedium,
            color = AvanueTheme.colors.textPrimary
        )
        Text(
            text = "Commands are auto-generated from the screen you're viewing. " +
                    "Every clickable element becomes a voice command automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (dynamicCount > 0) {
            AvanueSurface(
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = "$dynamicCount commands on current screen",
                    style = MaterialTheme.typography.labelLarge,
                    color = AvanueTheme.colors.info,
                    modifier = Modifier.padding(
                        horizontal = SpacingTokens.lg,
                        vertical = SpacingTokens.md
                    )
                )
            }
        }
    }
}

// ──────────────── TAB 2: CUSTOM COMMANDS ────────────────

@Composable
private fun CustomCommandsTab(
    customCommands: List<CustomCommandInfo>,
    onAdd: (String, List<String>, CommandActionType, String, List<MacroStep>) -> Unit,
    onRemove: (String) -> Unit,
    onToggle: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var commandToDelete by remember { mutableStateOf<CustomCommandInfo?>(null) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
        AvanueChip(
            onClick = { showAddDialog = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = AvanueTheme.colors.info)
                    Text("Add Command", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.info)
                }
            }
        )

        if (customCommands.isEmpty()) {
            EmptyStateMessage("No custom commands yet.\nTap + to create one.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
            ) {
                items(customCommands, key = { it.id }) { command ->
                    CustomCommandRow(
                        command = command,
                        onToggle = { onToggle(command.id) },
                        onRemove = { commandToDelete = command }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomCommandDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phrases, actionType, actionTarget, steps ->
                onAdd(name, phrases, actionType, actionTarget, steps)
                showAddDialog = false
            }
        )
    }

    commandToDelete?.let { command ->
        ConfirmDeleteDialog(
            title = "Delete Command",
            message = "Delete \"${command.name}\"? This cannot be undone.",
            onConfirm = {
                onRemove(command.id)
                commandToDelete = null
            },
            onDismiss = { commandToDelete = null }
        )
    }
}

@Composable
private fun CustomCommandRow(
    command: CustomCommandInfo,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    AvanueSurface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(command.name, style = MaterialTheme.typography.bodyMedium, color = AvanueTheme.colors.textPrimary)
                Text(
                    command.phrases.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
                Text(
                    buildString {
                        if (command.isMacro) {
                            append("macro (${command.steps.size} steps)")
                        } else {
                            append(command.actionType.name.lowercase().replace('_', ' '))
                            if (command.actionTarget.isNotBlank()) append(" → ${command.actionTarget}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (command.isMacro) AvanueTheme.colors.warning else AvanueTheme.colors.info
                )
            }
            Switch(
                checked = command.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AvanueTheme.colors.success,
                    checkedTrackColor = AvanueTheme.colors.success.copy(alpha = 0.3f),
                    uncheckedThumbColor = AvanueTheme.colors.textDisabled,
                    uncheckedTrackColor = AvanueTheme.colors.textDisabled.copy(alpha = 0.3f)
                )
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Remove", tint = AvanueTheme.colors.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * User-friendly action groups for the dropdown.
 * Maps readable labels to CommandActionType values.
 */
private val actionTypeGroups = listOf(
    "Click / Tap" to CommandActionType.CLICK,
    "Long Press" to CommandActionType.LONG_CLICK,
    "Type Text" to CommandActionType.TYPE,
    "Navigate Back" to CommandActionType.BACK,
    "Go Home" to CommandActionType.HOME,
    "Open App" to CommandActionType.OPEN_APP,
    "Open Settings" to CommandActionType.OPEN_SETTINGS,
    "Scroll Down" to CommandActionType.SCROLL_DOWN,
    "Scroll Up" to CommandActionType.SCROLL_UP,
    "Play Media" to CommandActionType.MEDIA_PLAY,
    "Pause Media" to CommandActionType.MEDIA_PAUSE,
    "Volume Up" to CommandActionType.VOLUME_UP,
    "Volume Down" to CommandActionType.VOLUME_DOWN,
    "Mute" to CommandActionType.VOLUME_MUTE,
    "Take Screenshot" to CommandActionType.SCREENSHOT,
    "Flashlight On" to CommandActionType.FLASHLIGHT_ON,
    "Flashlight Off" to CommandActionType.FLASHLIGHT_OFF,
    "Start Dictation" to CommandActionType.DICTATION_START,
    "Show Commands" to CommandActionType.SHOW_COMMANDS,
    "Custom" to CommandActionType.CUSTOM,
    "Macro (Multi-Step)" to CommandActionType.MACRO
)

/**
 * Action types that require a target parameter in single-action mode.
 */
private val targetRequiringActions = listOf(
    CommandActionType.OPEN_APP, CommandActionType.TYPE, CommandActionType.NAVIGATE, CommandActionType.CUSTOM
)

/**
 * Action types available for individual macro steps (excludes MACRO itself).
 */
private val macroStepActionTypes = actionTypeGroups.filter { it.second != CommandActionType.MACRO }

@Composable
private fun AddCustomCommandDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, CommandActionType, String, List<MacroStep>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phrasesText by remember { mutableStateOf("") }
    var selectedActionType by remember { mutableStateOf(CommandActionType.CLICK) }
    var actionTarget by remember { mutableStateOf("") }
    var actionDropdownExpanded by remember { mutableStateOf(false) }

    // Macro steps state
    var macroSteps by remember { mutableStateOf(listOf(MacroStep(CommandActionType.CLICK))) }

    val isMacroMode = selectedActionType == CommandActionType.MACRO
    val selectedLabel = actionTypeGroups.find { it.second == selectedActionType }?.first ?: "Click / Tap"
    val needsTarget = selectedActionType in targetRequiringActions

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AvanueTheme.colors.textPrimary,
        unfocusedTextColor = AvanueTheme.colors.textPrimary,
        focusedBorderColor = AvanueTheme.colors.info,
        unfocusedBorderColor = AvanueTheme.colors.textDisabled,
        focusedLabelColor = AvanueTheme.colors.info,
        unfocusedLabelColor = AvanueTheme.colors.textSecondary,
        cursorColor = AvanueTheme.colors.info
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Command", color = AvanueTheme.colors.textPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.md),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Command name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                OutlinedTextField(
                    value = phrasesText,
                    onValueChange = { phrasesText = it },
                    label = { Text("Trigger phrases (comma-separated)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                // Action type dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action") },
                        trailingIcon = {
                            IconButton(onClick = { actionDropdownExpanded = !actionDropdownExpanded }) {
                                Icon(
                                    if (actionDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    "Select action",
                                    tint = AvanueTheme.colors.textSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = actionDropdownExpanded,
                        onDismissRequest = { actionDropdownExpanded = false }
                    ) {
                        actionTypeGroups.forEach { (label, type) ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(label, color = AvanueTheme.colors.textPrimary) },
                                onClick = {
                                    selectedActionType = type
                                    actionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isMacroMode) {
                    // Macro steps UI
                    Text(
                        "Steps (executed sequentially)",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.textSecondary
                    )

                    macroSteps.forEachIndexed { index, step ->
                        MacroStepRow(
                            step = step,
                            stepNumber = index + 1,
                            canDelete = macroSteps.size > 1,
                            textFieldColors = textFieldColors,
                            onUpdate = { updated ->
                                macroSteps = macroSteps.toMutableList().also { it[index] = updated }
                            },
                            onDelete = {
                                macroSteps = macroSteps.toMutableList().also { it.removeAt(index) }
                            },
                            onMoveUp = if (index > 0) {
                                {
                                    macroSteps = macroSteps.toMutableList().also {
                                        val tmp = it[index]
                                        it[index] = it[index - 1]
                                        it[index - 1] = tmp
                                    }
                                }
                            } else null,
                            onMoveDown = if (index < macroSteps.lastIndex) {
                                {
                                    macroSteps = macroSteps.toMutableList().also {
                                        val tmp = it[index]
                                        it[index] = it[index + 1]
                                        it[index + 1] = tmp
                                    }
                                }
                            } else null
                        )
                    }

                    AvanueChip(
                        onClick = { macroSteps = macroSteps + MacroStep(CommandActionType.CLICK) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = AvanueTheme.colors.info)
                                Text("Add Step", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.info)
                            }
                        },
                    )
                } else if (needsTarget) {
                    // Single action target
                    OutlinedTextField(
                        value = actionTarget,
                        onValueChange = { actionTarget = it },
                        label = {
                            Text(
                                when (selectedActionType) {
                                    CommandActionType.OPEN_APP -> "App package (e.g., com.google.chrome)"
                                    CommandActionType.TYPE -> "Text to type"
                                    CommandActionType.NAVIGATE -> "Screen or URL"
                                    else -> "Action target"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val phrases = phrasesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (name.isNotBlank() && phrases.isNotEmpty()) {
                        if (isMacroMode) {
                            onConfirm(name, phrases, CommandActionType.MACRO, "", macroSteps)
                        } else {
                            onConfirm(name, phrases, selectedActionType, actionTarget.trim(), emptyList())
                        }
                    }
                }
            ) {
                Text("Add", color = AvanueTheme.colors.info)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AvanueTheme.colors.textSecondary)
            }
        },
        containerColor = AvanueTheme.colors.surface
    )
}

/**
 * A single macro step row with action type picker, target field, delay slider,
 * and ordering/delete controls.
 */
@Composable
private fun MacroStepRow(
    step: MacroStep,
    stepNumber: Int,
    canDelete: Boolean,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onUpdate: (MacroStep) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    var stepDropdownExpanded by remember { mutableStateOf(false) }
    val stepLabel = macroStepActionTypes.find { it.second == step.actionType }?.first ?: "Click / Tap"
    val stepNeedsTarget = step.actionType in targetRequiringActions

    AvanueSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(SpacingTokens.sm),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
        ) {
            // Step header with number, ordering, and delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Step $stepNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = AvanueTheme.colors.info
                )
                Row {
                    onMoveUp?.let {
                        IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, "Move up", tint = AvanueTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    onMoveDown?.let {
                        IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, "Move down", tint = AvanueTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Remove step", tint = AvanueTheme.colors.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Action type dropdown for this step
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = stepLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Action", style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = {
                        IconButton(onClick = { stepDropdownExpanded = !stepDropdownExpanded }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if (stepDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                "Select",
                                tint = AvanueTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = stepDropdownExpanded,
                    onDismissRequest = { stepDropdownExpanded = false }
                ) {
                    macroStepActionTypes.forEach { (label, type) ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textPrimary) },
                            onClick = {
                                onUpdate(step.copy(actionType = type))
                                stepDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Target field if needed
            if (stepNeedsTarget) {
                OutlinedTextField(
                    value = step.target,
                    onValueChange = { onUpdate(step.copy(target = it)) },
                    label = {
                        Text(
                            when (step.actionType) {
                                CommandActionType.OPEN_APP -> "App package"
                                CommandActionType.TYPE -> "Text to type"
                                CommandActionType.NAVIGATE -> "Screen or URL"
                                else -> "Target"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }

            // Delay slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
            ) {
                Text(
                    "Delay:",
                    style = MaterialTheme.typography.labelSmall,
                    color = AvanueTheme.colors.textSecondary
                )
                androidx.compose.material3.Slider(
                    value = step.delayMs.toFloat(),
                    onValueChange = { onUpdate(step.copy(delayMs = it.toLong())) },
                    valueRange = 0f..5000f,
                    steps = 9,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${step.delayMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }
    }
}

// ──────────────── VERB SYNONYMS (inside Static tab) ────────────────

@Composable
private fun VerbSynonymsCategory(
    entries: List<SynonymEntryInfo>,
    onAddSynonym: (String, List<String>) -> Unit,
    onRemoveSynonym: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
        AvanueSurface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Verb synonyms",
                    style = MaterialTheme.typography.titleSmall,
                    color = AvanueTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${entries.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded) "Collapse" else "Expand",
                    tint = AvanueTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = SpacingTokens.md),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
            ) {
                entries.forEach { entry ->
                    ExpandableSynonymEntry(
                        entry = entry,
                        onAddSynonym = onAddSynonym,
                        onRemove = if (entry.isDefault) null else {{ onRemoveSynonym(entry.canonical) }}
                    )
                }
                // Add new synonym verb mapping
                AvanueChip(
                    onClick = { showAddDialog = true },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = AvanueTheme.colors.primary)
                            Text("Add Verb", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.primary)
                        }
                    },
                )
            }
        }
    }

    if (showAddDialog) {
        AddSynonymDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { canonical, synonyms ->
                onAddSynonym(canonical, synonyms)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableSynonymEntry(
    entry: SynonymEntryInfo,
    onAddSynonym: (String, List<String>) -> Unit,
    onRemove: (() -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddSynonymField by remember { mutableStateOf(false) }
    var newSynonymText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        AvanueSurface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.canonical,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.primary
                )
                if (entry.isDefault) {
                    Text(
                        text = "BUILT-IN",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textDisabled
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${entry.synonyms.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded) "Collapse" else "Expand",
                    tint = AvanueTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = SpacingTokens.md, top = SpacingTokens.xs, bottom = SpacingTokens.sm)
            ) {
                Text(
                    "Also responds to:",
                    style = MaterialTheme.typography.labelSmall,
                    color = AvanueTheme.colors.textTertiary,
                    modifier = Modifier.padding(bottom = SpacingTokens.xs)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                ) {
                    entry.synonyms.forEach { synonym ->
                        AvanueChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = synonym,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AvanueTheme.colors.textSecondary
                                )
                            }
                        )
                    }

                    // Add synonym chip
                    AvanueChip(
                        onClick = { showAddSynonymField = !showAddSynonymField },
                        label = {
                            Icon(Icons.Default.Add, "Add", modifier = Modifier.size(14.dp), tint = AvanueTheme.colors.primary)
                        },
                    )

                    // Remove verb button (only for user-created)
                    if (onRemove != null) {
                        AvanueChip(
                            onClick = onRemove,
                            label = {
                                Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(14.dp), tint = AvanueTheme.colors.error)
                            }
                        )
                    }
                }

                // Inline add synonym field
                AnimatedVisibility(visible = showAddSynonymField) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = SpacingTokens.sm),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSynonymText,
                            onValueChange = { newSynonymText = it },
                            label = { Text("New synonym") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AvanueTheme.colors.textPrimary,
                                unfocusedTextColor = AvanueTheme.colors.textPrimary,
                                focusedBorderColor = AvanueTheme.colors.primary,
                                unfocusedBorderColor = AvanueTheme.colors.textDisabled,
                                focusedLabelColor = AvanueTheme.colors.primary,
                                unfocusedLabelColor = AvanueTheme.colors.textSecondary,
                                cursorColor = AvanueTheme.colors.primary
                            )
                        )
                        TextButton(
                            onClick = {
                                val trimmed = newSynonymText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onAddSynonym(entry.canonical, listOf(trimmed))
                                    newSynonymText = ""
                                    showAddSynonymField = false
                                }
                            }
                        ) {
                            Text("Add", color = AvanueTheme.colors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSynonymDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var canonical by remember { mutableStateOf("") }
    var synonymsText by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AvanueTheme.colors.textPrimary,
        unfocusedTextColor = AvanueTheme.colors.textPrimary,
        focusedBorderColor = AvanueTheme.colors.primary,
        unfocusedBorderColor = AvanueTheme.colors.textDisabled,
        focusedLabelColor = AvanueTheme.colors.primary,
        unfocusedLabelColor = AvanueTheme.colors.textSecondary,
        cursorColor = AvanueTheme.colors.primary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Verb Synonym", color = AvanueTheme.colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)) {
                OutlinedTextField(
                    value = canonical,
                    onValueChange = { canonical = it },
                    label = { Text("Canonical action (e.g., \"click\")") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                OutlinedTextField(
                    value = synonymsText,
                    onValueChange = { synonymsText = it },
                    label = { Text("Synonyms (comma-separated)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val synonyms = synonymsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (canonical.isNotBlank() && synonyms.isNotEmpty()) {
                        onConfirm(canonical.trim(), synonyms)
                    }
                }
            ) {
                Text("Add", color = AvanueTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AvanueTheme.colors.textSecondary)
            }
        },
        containerColor = AvanueTheme.colors.surface
    )
}

// ──────────────────────────── SHARED ────────────────────────────

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = AvanueTheme.colors.textPrimary) },
        text = { Text(message, color = AvanueTheme.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = AvanueTheme.colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AvanueTheme.colors.textSecondary)
            }
        },
        containerColor = AvanueTheme.colors.surface
    )
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = AvanueTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTimeAgo(timestampMs: Long): String {
    if (timestampMs == 0L) return ""
    val diff = System.currentTimeMillis() - timestampMs
    return when {
        diff < 1000L -> "just now"
        diff < 60_000L -> "${diff / 1000}s ago"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
