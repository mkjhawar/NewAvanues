/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.AvanueChip
import com.augmentalis.avanueui.components.AvanueSurface
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceavanue.ui.sync.PhraseSuggestionDialog

@Composable
fun CommandsSection(
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
                onRemoveSynonym = callbacks.onRemoveSynonym,
                onSuggestPhrase = callbacks.onSuggestPhrase
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
    onRemoveSynonym: (String) -> Unit,
    onSuggestPhrase: (String, String, String, String) -> Unit = { _, _, _, _ -> }
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
                    onAddSynonym = onAddSynonym,
                    onSuggestPhrase = onSuggestPhrase
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
    onAddSynonym: (String, List<String>) -> Unit = { _, _ -> },
    onSuggestPhrase: (String, String, String, String) -> Unit = { _, _, _, _ -> }
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
                        onAddAlias = { alias -> onAddSynonym(command.phrase, listOf(alias)) },
                        onSuggestPhrase = onSuggestPhrase
                    )
                }
            }
        }
    }
}

/**
 * Formats a CommandActionType into a readable function label.
 * e.g. SCROLL_DOWN -> "Scroll Down", MEDIA_PLAY -> "Media Play"
 */
private fun formatActionType(actionType: CommandActionType): String {
    return actionType.name.lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommandRow(
    command: StaticCommand,
    onToggle: () -> Unit,
    onAddAlias: (String) -> Unit = {},
    onSuggestPhrase: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {
    var expanded by remember { mutableStateOf(false) }
    var showAliasField by remember { mutableStateOf(false) }
    var aliasText by remember { mutableStateOf("") }
    var showSuggestionDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // Collapsed row: checkbox + primary phrase + action badge + expand chevron
        AvanueSurface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingTokens.sm, vertical = SpacingTokens.xs),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = command.enabled, onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = AvanueTheme.colors.success, uncheckedColor = AvanueTheme.colors.textSecondary),
                    modifier = Modifier.semantics {
                        contentDescription = "Voice: toggle ${command.phrase} command"
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\"${command.phrase}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatActionType(command.actionType),
                            style = MaterialTheme.typography.labelSmall,
                            color = AvanueTheme.colors.info
                        )
                    }
                    if (command.description.isNotEmpty()) {
                        Text(command.description, style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
                    }
                }
                if (command.synonyms.isNotEmpty()) {
                    Text(
                        text = "+${command.synonyms.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textTertiary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded) "Collapse" else "Expand",
                    tint = AvanueTheme.colors.textSecondary, modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded detail: synonyms as chips + add alias
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, top = SpacingTokens.xs, bottom = SpacingTokens.sm),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
            ) {
                // Primary phrase label
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Primary:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textTertiary
                    )
                    AvanueChip(
                        onClick = {},
                        label = {
                            Text(
                                text = command.phrase,
                                style = MaterialTheme.typography.labelSmall,
                                color = AvanueTheme.colors.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    )
                }

                // Synonyms (alternate trigger phrases)
                if (command.synonyms.isNotEmpty()) {
                    Text(
                        text = "Synonyms:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textTertiary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                    ) {
                        command.synonyms.forEach { synonym ->
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
                    }
                }

                // Function (action type)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Function:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.textTertiary
                    )
                    Text(
                        text = formatActionType(command.actionType),
                        style = MaterialTheme.typography.labelSmall,
                        color = AvanueTheme.colors.info,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Add alias + suggest translation chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                ) {
                    AvanueChip(
                        onClick = { showAliasField = !showAliasField },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = AvanueTheme.colors.primary)
                                Text("Add Alias", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.primary)
                            }
                        }
                    )
                    AvanueChip(
                        onClick = { showSuggestionDialog = true },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = AvanueTheme.colors.info)
                                Text("Suggest Translation", style = MaterialTheme.typography.labelSmall, color = AvanueTheme.colors.info)
                            }
                        }
                    )
                }

                AnimatedVisibility(visible = showAliasField) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aliasText,
                            onValueChange = { aliasText = it },
                            label = { Text("New alias phrase") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Voice: input Alias Phrase" },
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
                            },
                            modifier = Modifier.semantics { contentDescription = "Voice: click Add Alias" }
                        ) {
                            Text("Add", color = AvanueTheme.colors.primary)
                        }
                    }
                }
            }
        }

        // Phrase suggestion dialog
        if (showSuggestionDialog) {
            PhraseSuggestionDialog(
                commandId = command.id,
                originalPhrase = command.phrase,
                locale = java.util.Locale.getDefault().toLanguageTag(),
                onSubmit = onSuggestPhrase,
                onDismiss = { showSuggestionDialog = false }
            )
        }
    }
}

// ──────────────── TAB 1: DYNAMIC APP COMMANDS ────────────────

@Composable
fun DynamicCommandsInfoTab(dynamicCount: Int) {
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
