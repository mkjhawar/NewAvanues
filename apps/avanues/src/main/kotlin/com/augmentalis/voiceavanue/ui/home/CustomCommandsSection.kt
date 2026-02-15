/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.AvanueChip
import com.augmentalis.avanueui.components.AvanueSurface
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceoscore.CommandActionType

/**
 * User-friendly action groups for the dropdown.
 * Maps readable labels to CommandActionType values.
 */
val actionTypeGroups = listOf(
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
val targetRequiringActions = listOf(
    CommandActionType.OPEN_APP, CommandActionType.TYPE, CommandActionType.NAVIGATE, CommandActionType.CUSTOM
)

/**
 * Action types available for individual macro steps (excludes MACRO itself).
 */
val macroStepActionTypes = actionTypeGroups.filter { it.second != CommandActionType.MACRO }

@Composable
fun CustomCommandsTab(
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
                            if (command.actionTarget.isNotBlank()) append(" \u2192 ${command.actionTarget}")
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
