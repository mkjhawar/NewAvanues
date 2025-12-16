package com.augmentalis.ava.features.teach

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
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import java.security.MessageDigest

/**
 * Dialog for adding new training examples
 * Implements hash-based deduplication following VOS4 patterns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExampleDialog(
    onDismiss: () -> Unit,
    onExampleAdded: (TrainExample) -> Unit,
    modifier: Modifier = Modifier
) {
    var utterance by remember { mutableStateOf("") }
    var intent by remember { mutableStateOf("") }
    var locale by remember { mutableStateOf("en-US") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

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
                    text = "Add Training Example",
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
                                val example = createTrainExample(
                                    utterance = utterance.trim(),
                                    intent = intent.trim(),
                                    locale = locale.trim()
                                )
                                onExampleAdded(example)
                            } else {
                                showError = true
                                errorMessage = "Please fill in all fields"
                            }
                        }
                    ) {
                        Text("Add Example")
                    }
                }
            }
        }
    }
}

private fun validateInput(utterance: String, intent: String): Boolean {
    return utterance.isNotBlank() && intent.isNotBlank()
}

private fun createTrainExample(
    utterance: String,
    intent: String,
    locale: String
): TrainExample {
    val hashInput = "$utterance:$intent"
    val hash = MessageDigest.getInstance("MD5")
        .digest(hashInput.toByteArray())
        .joinToString("") { "%02x".format(it) }

    return TrainExample(
        exampleHash = hash,
        utterance = utterance,
        intent = intent,
        locale = locale,
        source = TrainExampleSource.MANUAL,
        createdAt = System.currentTimeMillis()
    )
}
