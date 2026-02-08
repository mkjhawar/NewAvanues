/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.home

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.avamagic.ui.foundation.GlassCard
import com.augmentalis.avamagic.ui.foundation.GlassChip
import com.augmentalis.avamagic.ui.foundation.GlassSurface
import com.augmentalis.avamagic.ui.foundation.PulseDot
import com.augmentalis.avamagic.ui.foundation.StatusBadge
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.foundation.state.LastHeardCommand
import com.augmentalis.foundation.state.ServiceState

/**
 * Aggregated callbacks for all command management actions.
 */
data class CommandCallbacks(
    val onToggleStaticCommand: (String) -> Unit = {},
    val onAddCustomCommand: (String, List<String>) -> Unit = { _, _ -> },
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
@Composable
fun HomeScreen(
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
            onAddCustomCommand = viewModel::addCustomCommand,
            onRemoveCustomCommand = viewModel::removeCustomCommand,
            onToggleCustomCommand = viewModel::toggleCustomCommand,
            onAddSynonym = viewModel::addSynonym,
            onRemoveSynonym = viewModel::removeSynonym
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AvanueTheme.colors.background)
    ) {
        val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

        if (isLandscape) {
            DashboardLandscape(
                uiState = uiState,
                onNavigateToBrowser = onNavigateToBrowser,
                onNavigateToSettings = onNavigateToSettings,
                callbacks = commandCallbacks
            )
        } else {
            DashboardPortrait(
                uiState = uiState,
                onNavigateToBrowser = onNavigateToBrowser,
                onNavigateToSettings = onNavigateToSettings,
                callbacks = commandCallbacks
            )
        }
    }
}

// ──────────────────────────── LANDSCAPE ────────────────────────────

@Composable
private fun DashboardLandscape(
    uiState: DashboardUiState,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
    callbacks: CommandCallbacks
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingTokens.md),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md)
    ) {
        // Column 1: MODULES
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MODULES",
                    style = MaterialTheme.typography.labelLarge,
                    color = AvanueTheme.colors.textSecondary
                )
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AvanueTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
            Text(
                text = "SYSTEM",
                style = MaterialTheme.typography.labelLarge,
                color = AvanueTheme.colors.textSecondary
            )
            SystemHealthBar(permissions = uiState.permissions)
            if (uiState.hasLastCommand) {
                LastHeardCard(command = uiState.lastHeardCommand)
            }
        }

        // Column 3: COMMANDS (scrollable)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            Text(
                text = "COMMANDS",
                style = MaterialTheme.typography.labelLarge,
                color = AvanueTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = SpacingTokens.sm)
            )
            CommandsSection(
                commands = uiState.commands,
                callbacks = callbacks,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ──────────────────────────── PORTRAIT ────────────────────────────

@Composable
private fun DashboardPortrait(
    uiState: DashboardUiState,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
    callbacks: CommandCallbacks
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.md)
    ) {
        // Header with title and settings gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SpacingTokens.lg, bottom = SpacingTokens.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VoiceAvanue",
                style = MaterialTheme.typography.headlineSmall,
                color = AvanueTheme.colors.textPrimary
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AvanueTheme.colors.textSecondary
                )
            }
        }

        Column(
            modifier = Modifier.padding(bottom = SpacingTokens.md),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
        ) {
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
            SystemHealthBar(permissions = uiState.permissions)
            if (uiState.hasLastCommand) {
                LastHeardCard(command = uiState.lastHeardCommand)
            }
        }

        Text(
            text = "COMMANDS",
            style = MaterialTheme.typography.labelLarge,
            color = AvanueTheme.colors.textSecondary,
            modifier = Modifier.padding(vertical = SpacingTokens.sm)
        )
        CommandsSection(
            commands = uiState.commands,
            callbacks = callbacks,
            modifier = Modifier.weight(1f)
        )
    }
}

// ──────────────────────────── MODULE CARD ────────────────────────────

@Composable
private fun ModuleCard(module: ModuleStatus, onClick: () -> Unit) {
    GlassCard(
        onClick = onClick,
        glassLevel = GlassLevel.MEDIUM,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Responsive padding: compact on narrow cards, standard on wide
            val isCompact = maxWidth < 200.dp
            val cardPadding = if (isCompact) SpacingTokens.sm else SpacingTokens.md

            Column(
                modifier = Modifier.fillMaxWidth().padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseDot(state = module.state, dotSize = 12.dp)
                    Text(
                        text = module.displayName,
                        style = if (isCompact) MaterialTheme.typography.labelLarge
                               else MaterialTheme.typography.titleSmall,
                        color = AvanueTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(state = module.state)
                }
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (module.metadata.isNotEmpty()) {
                    Text(
                        text = module.metadata.entries.joinToString(" \u00B7 ") { "${it.key}: ${it.value}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textPrimary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ──────────────────────────── SYSTEM HEALTH ────────────────────────────

@Composable
private fun SystemHealthBar(permissions: PermissionStatus) {
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
    ) {
        if (permissions.allGranted) {
            GlassSurface(
                glassLevel = GlassLevel.LIGHT,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, "All OK", tint = AvanueTheme.colors.success, modifier = Modifier.size(20.dp))
                    Text("All permissions granted", style = MaterialTheme.typography.bodyMedium, color = AvanueTheme.colors.textPrimary)
                }
            }
        } else {
            Text("PERMISSIONS REQUIRED", style = MaterialTheme.typography.labelMedium, color = AvanueTheme.colors.error)
            if (!permissions.microphoneGranted) {
                PermissionErrorCard("Microphone", "Required for voice recognition", Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            }
            if (!permissions.accessibilityEnabled) {
                PermissionErrorCard("Accessibility Service", "Required for voice control", Settings.ACTION_ACCESSIBILITY_SETTINGS)
            }
            if (!permissions.overlayEnabled) {
                PermissionErrorCard("Display Over Apps", "Required for voice cursor", Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            }
            if (!permissions.notificationsEnabled) {
                PermissionErrorCard(
                    "Notifications", "Required for system alerts",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    else Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                )
            }
        }
    }
}

@Composable
private fun PermissionErrorCard(title: String, description: String, action: String) {
    val context = LocalContext.current
    GlassCard(
        onClick = {
            val intent = Intent(action).apply {
                when (action) {
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS ->
                        data = android.net.Uri.parse("package:${context.packageName}")
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS ->
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
            try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        },
        glassLevel = GlassLevel.LIGHT,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = AvanueTheme.colors.error, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = AvanueTheme.colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open settings", tint = AvanueTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ──────────────────────────── LAST HEARD ────────────────────────────

@Composable
private fun LastHeardCard(command: LastHeardCommand) {
    val timeAgo = remember(command.timestampMs) { formatTimeAgo(command.timestampMs) }
    GlassCard(glassLevel = GlassLevel.LIGHT, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
        ) {
            Text("LAST HEARD", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.textSecondary)
            Text("\"${command.phrase}\"", style = MaterialTheme.typography.titleMedium, color = AvanueTheme.colors.textPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md)) {
                Text("Confidence: ${(command.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
                Text(timeAgo, style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
            }
        }
    }
}

// ──────────────────────────── COMMANDS ────────────────────────────

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
            CommandTab("\u2248 Synonyms", 3, selectedTab) { selectedTab = 3 }
        }

        when (selectedTab) {
            0 -> StaticCommandsTab(
                categories = commands.staticCategories,
                onToggleCommand = callbacks.onToggleStaticCommand
            )
            1 -> DynamicCommandsInfoTab(dynamicCount = commands.dynamicCount)
            2 -> CustomCommandsTab(
                customCommands = commands.customCommands,
                onAdd = callbacks.onAddCustomCommand,
                onRemove = callbacks.onRemoveCustomCommand,
                onToggle = callbacks.onToggleCustomCommand
            )
            3 -> SynonymsTab(
                entries = commands.synonymEntries,
                onAdd = callbacks.onAddSynonym,
                onRemove = callbacks.onRemoveSynonym
            )
        }
    }
}

@Composable
private fun CommandTab(label: String, index: Int, selectedTab: Int, onClick: () -> Unit) {
    val isSelected = index == selectedTab
    GlassChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) AvanueTheme.colors.info else AvanueTheme.colors.textSecondary
            )
        },
        glass = isSelected,
        glassLevel = if (isSelected) GlassLevel.MEDIUM else GlassLevel.LIGHT
    )
}

// ──────────────── TAB 0: STATIC COMMANDS ────────────────

@Composable
private fun StaticCommandsTab(categories: List<CommandCategory>, onToggleCommand: (String) -> Unit) {
    if (categories.isEmpty()) {
        EmptyStateMessage("No static commands loaded")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            items(categories, key = { it.name }) { category ->
                ExpandableCommandCategory(category = category, onToggleCommand = onToggleCommand)
            }
        }
    }
}

@Composable
private fun ExpandableCommandCategory(category: CommandCategory, onToggleCommand: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
        GlassSurface(
            onClick = { expanded = !expanded },
            glassLevel = GlassLevel.LIGHT,
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
                    CommandRow(command = command, onToggle = { onToggleCommand(command.id) })
                }
            }
        }
    }
}

@Composable
private fun CommandRow(command: StaticCommand, onToggle: () -> Unit) {
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
            GlassSurface(
                glassLevel = GlassLevel.LIGHT,
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
    onAdd: (String, List<String>) -> Unit,
    onRemove: (String) -> Unit,
    onToggle: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
        GlassChip(
            onClick = { showAddDialog = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = AvanueTheme.colors.info)
                    Text("Add Command", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.info)
                }
            },
            glass = true,
            glassLevel = GlassLevel.LIGHT
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
                        onRemove = { onRemove(command.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomCommandDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phrases ->
                onAdd(name, phrases)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CustomCommandRow(
    command: CustomCommandInfo,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    GlassSurface(
        glassLevel = GlassLevel.LIGHT,
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

@Composable
private fun AddCustomCommandDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phrasesText by remember { mutableStateOf("") }

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
            Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val phrases = phrasesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (name.isNotBlank() && phrases.isNotEmpty()) {
                        onConfirm(name, phrases)
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

// ──────────────── TAB 3: SYNONYMS ────────────────

@Composable
private fun SynonymsTab(
    entries: List<SynonymEntryInfo>,
    onAdd: (String, List<String>) -> Unit,
    onRemove: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
        GlassChip(
            onClick = { showAddDialog = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = AvanueTheme.colors.info)
                    Text("Add Synonym", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.info)
                }
            },
            glass = true,
            glassLevel = GlassLevel.LIGHT
        )

        if (entries.isEmpty()) {
            EmptyStateMessage("No synonyms configured")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
            ) {
                items(entries, key = { it.canonical }) { entry ->
                    SynonymRow(entry = entry, onRemove = { onRemove(entry.canonical) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddSynonymDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { canonical, synonyms ->
                onAdd(canonical, synonyms)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SynonymRow(entry: SynonymEntryInfo, onRemove: () -> Unit) {
    GlassSurface(
        glassLevel = GlassLevel.LIGHT,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.canonical,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.info
                )
                Text(
                    text = entry.synonyms.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Remove", tint = AvanueTheme.colors.error, modifier = Modifier.size(18.dp))
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
        focusedBorderColor = AvanueTheme.colors.info,
        unfocusedBorderColor = AvanueTheme.colors.textDisabled,
        focusedLabelColor = AvanueTheme.colors.info,
        unfocusedLabelColor = AvanueTheme.colors.textSecondary,
        cursorColor = AvanueTheme.colors.info
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Synonym Mapping", color = AvanueTheme.colors.textPrimary) },
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

// ──────────────────────────── SHARED ────────────────────────────

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
