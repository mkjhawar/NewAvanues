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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.AvanueChip
import com.augmentalis.avanueui.components.AvanueSurface
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme

@Composable
fun VerbSynonymsCategory(
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
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Voice: input New Synonym for ${entry.canonical}" },
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
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Voice: click Add Synonym for ${entry.canonical}"
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Voice: input Canonical Verb" },
                    colors = textFieldColors
                )
                OutlinedTextField(
                    value = synonymsText,
                    onValueChange = { synonymsText = it },
                    label = { Text("Synonyms (comma-separated)") },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Voice: input Synonym List" },
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
                },
                modifier = Modifier.semantics { contentDescription = "Voice: click Add Verb Synonym" }
            ) {
                Text("Add", color = AvanueTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "Voice: click Cancel" }
            ) {
                Text("Cancel", color = AvanueTheme.colors.textSecondary)
            }
        },
        containerColor = AvanueTheme.colors.surface
    )
}
