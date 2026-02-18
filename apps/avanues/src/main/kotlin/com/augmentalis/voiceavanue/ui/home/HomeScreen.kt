/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.foundation.state.LastHeardCommand
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
    val onRemoveSynonym: (String) -> Unit = {},
    val onSuggestPhrase: (commandId: String, originalPhrase: String, suggestedPhrase: String, locale: String) -> Unit = { _, _, _, _ -> }
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
            onRemoveSynonym = viewModel::removeSynonym,
            onSuggestPhrase = viewModel::submitPhraseSuggestion
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
            onRemoveSynonym = viewModel::removeSynonym,
            onSuggestPhrase = viewModel::submitPhraseSuggestion
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
                text = buildAnnotatedString {
                    append("VoiceOS")
                    withStyle(SpanStyle(
                        baselineShift = BaselineShift.Superscript,
                        fontSize = 12.sp
                    )) {
                        append("\u00AE")
                    }
                    append(" Avanues")
                },
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

// ──────────────────────────── SHARED UTILITIES ────────────────────────────

@Composable
fun ConfirmDeleteDialog(
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
fun EmptyStateMessage(message: String) {
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
