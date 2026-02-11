/**
 * PhraseSuggestionDialog.kt - Dialog for submitting alternative voice command phrases
 *
 * Part of Phase C crowd-sourcing foundation. Users can suggest alternative
 * phrases for existing voice commands, stored locally for review/export.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.voiceavanue.ui.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.augmentalis.avanueui.components.AvanueButton
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.SpacingTokens

@Composable
fun PhraseSuggestionDialog(
    commandId: String,
    originalPhrase: String,
    locale: String,
    onSubmit: (commandId: String, originalPhrase: String, suggestedPhrase: String, locale: String) -> Unit,
    onDismiss: () -> Unit
) {
    var suggestedPhrase by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AvanueTheme.colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(SpacingTokens.lg)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Suggest Alternative Phrase",
                    style = MaterialTheme.typography.titleMedium,
                    color = AvanueTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(SpacingTokens.md))

                Text(
                    text = "Current phrase:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AvanueTheme.colors.textSecondary
                )
                Text(
                    text = "\"$originalPhrase\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(SpacingTokens.sm))

                Text(
                    text = "Locale: $locale",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(SpacingTokens.md))

                OutlinedTextField(
                    value = suggestedPhrase,
                    onValueChange = { suggestedPhrase = it },
                    label = { Text("Your suggestion") },
                    placeholder = { Text("e.g., \"open browser\"") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(SpacingTokens.lg))

                AvanueButton(
                    onClick = {
                        if (suggestedPhrase.isNotBlank()) {
                            onSubmit(commandId, originalPhrase, suggestedPhrase.trim(), locale)
                            onDismiss()
                        }
                    },
                    enabled = suggestedPhrase.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Suggestion")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Cancel",
                        color = AvanueTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
