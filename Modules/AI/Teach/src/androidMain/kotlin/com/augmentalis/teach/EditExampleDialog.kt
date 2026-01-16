package com.augmentalis.teach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.augmentalis.ava.core.domain.model.TrainExample
import java.security.MessageDigest

/**
 * Dialog for editing existing training examples
 * Allows modification of utterance, intent, and locale
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExampleDialog(
    example: TrainExample,
    onDismiss: () -> Unit,
    onExampleUpdated: (TrainExample) -> Unit,
    modifier: Modifier = Modifier
) {
    var utterance by remember { mutableStateOf(example.utterance) }
    var intent by remember { mutableStateOf(example.intent) }
    var locale by remember { mutableStateOf(example.locale) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val hasChanges = utterance != example.utterance ||
                     intent != example.intent ||
                     locale != example.locale

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                Text(
                    text = "Edit Training Example",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Utterance input
                OutlinedTextField(
                    value = utterance,
                    onValueChange = {
                        utterance = it
                        showError = false
                    },
                    label = { Text("What the user says") },
                    placeholder = { Text("e.g., Turn on the lights") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    isError = showError && utterance.isBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Intent input
                OutlinedTextField(
                    value = intent,
                    onValueChange = {
                        intent = it
                        showError = false
                    },
                    label = { Text("Intent name") },
                    placeholder = { Text("e.g., control_lights") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    isError = showError && intent.isBlank(),
                    supportingText = {
                        Text(
                            text = "Use lowercase with underscores (e.g., check_weather)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Locale selector
                OutlinedTextField(
                    value = locale,
                    onValueChange = { locale = it },
                    label = { Text("Locale") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "Language code (e.g., en-US, es-ES)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                // Show usage stats
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Usage Statistics",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Times used:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${example.usageCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        example.lastUsed?.let { lastUsedTime ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Last used:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatRelativeTime(lastUsedTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (validateInput(utterance, intent)) {
                                val updatedExample = createUpdatedExample(
                                    original = example,
                                    newUtterance = utterance.trim(),
                                    newIntent = intent.trim(),
                                    newLocale = locale.trim()
                                )
                                onExampleUpdated(updatedExample)
                            } else {
                                showError = true
                                errorMessage = "Please fill in all fields"
                            }
                        },
                        enabled = hasChanges
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

private fun validateInput(utterance: String, intent: String): Boolean {
    return utterance.isNotBlank() && intent.isNotBlank()
}

private fun createUpdatedExample(
    original: TrainExample,
    newUtterance: String,
    newIntent: String,
    newLocale: String
): TrainExample {
    // Regenerate hash if utterance or intent changed
    val hashInput = "$newUtterance:$newIntent"
    val hash = MessageDigest.getInstance("MD5")
        .digest(hashInput.toByteArray())
        .joinToString("") { "%02x".format(it) }

    return original.copy(
        exampleHash = hash,
        utterance = newUtterance,
        intent = newIntent,
        locale = newLocale
    )
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 2592000_000 -> "${diff / 86400_000}d ago"
        else -> "${diff / 2592000_000}mo ago"
    }
}
