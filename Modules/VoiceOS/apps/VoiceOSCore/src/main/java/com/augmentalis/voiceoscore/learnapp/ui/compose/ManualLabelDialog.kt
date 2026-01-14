/**
 * ManualLabelDialog.kt - Compose equivalent of learnapp_manual_label_dialog.xml
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v12.1)
 * Created: 2025-12-22
 *
 * Material3 Compose dialog for renaming elements with voice commands
 */

package com.augmentalis.voiceoscore.learnapp.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * Element Preview Data
 *
 * Information about the element being renamed
 */
data class ElementPreview(
    val type: String,
    val currentLabel: String,
    val position: String
)

/**
 * Manual Label Dialog (Compose)
 *
 * Replaces learnapp_manual_label_dialog.xml with Material3 Compose
 *
 * @param title Dialog title
 * @param message Dialog message/instructions
 * @param elementPreview Preview of the element being renamed
 * @param suggestions Optional quick-select suggestions
 * @param initialValue Initial value for the text field
 * @param onSave Callback when user saves the label
 * @param onCancel Callback when user cancels
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualLabelDialog(
    title: String = "Rename Element",
    message: String = "Enter a custom voice command for this element.",
    elementPreview: ElementPreview,
    suggestions: List<String>? = null,
    initialValue: String = "",
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var labelText by remember { mutableStateOf(initialValue) }
    var selectedSuggestion by remember { mutableStateOf<String?>(null) }

    // Update text field when suggestion is selected
    LaunchedEffect(selectedSuggestion) {
        selectedSuggestion?.let { labelText = it }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Quick Select Chips (if suggestions provided)
            if (!suggestions.isNullOrEmpty()) {
                Text(
                    text = "Quick Select:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        FilterChip(
                            selected = selectedSuggestion == suggestion,
                            onClick = { selectedSuggestion = suggestion },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }

            // Element Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = elementPreview.type,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Current label: ${elementPreview.currentLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Position: ${elementPreview.position}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Input Field
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                label = { Text("Voice command") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onSave(labelText) },
                    enabled = labelText.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }
}
