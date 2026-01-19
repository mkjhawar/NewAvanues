package com.augmentalis.voiceoscore.commands.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscore.commands.ElementCommandManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compose dialog for recording voice commands.
 *
 * States:
 * - IDLE: Show element info, "Record" button enabled
 * - RECORDING: Animated mic, stop button enabled
 * - RECORDED: Show phrase, playback/re-record/save buttons
 * - SAVING: Progress indicator
 * - SUCCESS: Confirmation message, auto-dismiss after 2s
 * - ERROR: Error message with retry option
 */

data class ElementInfo(
    val uuid: String,
    val type: String,
    val genericLabel: String,
    val appId: String,
    val existingCommands: List<String> = emptyList()
)

enum class DialogState {
    IDLE,
    RECORDING,
    RECORDED,
    SAVING,
    SUCCESS,
    ERROR
}

@Composable
fun CommandAssignmentDialog(
    elementInfo: ElementInfo,
    commandManager: ElementCommandManager,
    onDismiss: () -> Unit,
    onCommandSaved: (String) -> Unit,
    onRecordAudio: (onResult: (String?, Double) -> Unit) -> Unit // Speech recognition callback
) {
    var dialogState by remember { mutableStateOf(DialogState.IDLE) }
    var recordedPhrase by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(0.0) }
    var errorMessage by remember { mutableStateOf("") }
    var isSynonym by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Auto-dismiss on success
    LaunchedEffect(dialogState) {
        if (dialogState == DialogState.SUCCESS) {
            delay(2000)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (dialogState != DialogState.RECORDING && dialogState != DialogState.SAVING) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = if (isSynonym) "Add Synonym" else "Assign Voice Command",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Element preview
                ElementPreview(elementInfo)

                // State-specific content
                when (dialogState) {
                    DialogState.IDLE -> IdleContent(
                        onRecordClick = {
                            dialogState = DialogState.RECORDING
                            onRecordAudio { phrase, conf ->
                                if (phrase != null && phrase.isNotBlank()) {
                                    recordedPhrase = phrase
                                    confidence = conf
                                    dialogState = DialogState.RECORDED
                                } else {
                                    errorMessage = "Could not recognize speech. Please try again."
                                    dialogState = DialogState.ERROR
                                }
                            }
                        }
                    )

                    DialogState.RECORDING -> RecordingIndicator()

                    DialogState.RECORDED -> PhraseReview(
                        phrase = recordedPhrase,
                        confidence = confidence,
                        onReRecord = {
                            dialogState = DialogState.IDLE
                            recordedPhrase = ""
                        }
                    )

                    DialogState.SAVING -> SavingIndicator()

                    DialogState.SUCCESS -> SuccessMessage(
                        phrase = recordedPhrase,
                        isSynonym = isSynonym
                    )

                    DialogState.ERROR -> ErrorMessage(
                        message = errorMessage,
                        onRetry = {
                            dialogState = DialogState.IDLE
                            errorMessage = ""
                        }
                    )
                }

                // Add synonym checkbox (only in IDLE or RECORDED states)
                if (dialogState == DialogState.IDLE || dialogState == DialogState.RECORDED) {
                    if (elementInfo.existingCommands.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSynonym,
                                onCheckedChange = { isSynonym = it }
                            )
                            Text(
                                text = "Add as synonym",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (dialogState) {
                DialogState.RECORDED -> {
                    Button(
                        onClick = {
                            scope.launch {
                                dialogState = DialogState.SAVING

                                val result = if (isSynonym) {
                                    commandManager.addSynonym(
                                        elementInfo.uuid,
                                        recordedPhrase,
                                        elementInfo.appId
                                    )
                                } else {
                                    commandManager.assignCommand(
                                        elementInfo.uuid,
                                        recordedPhrase,
                                        elementInfo.appId
                                    )
                                }

                                result.fold(
                                    onSuccess = {
                                        dialogState = DialogState.SUCCESS
                                        onCommandSaved(recordedPhrase)
                                    },
                                    onFailure = { e ->
                                        errorMessage = e.message ?: "Failed to save command"
                                        dialogState = DialogState.ERROR
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            when (dialogState) {
                DialogState.IDLE, DialogState.RECORDED, DialogState.ERROR -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
                DialogState.RECORDING -> {
                    TextButton(onClick = {
                        dialogState = DialogState.IDLE
                    }) {
                        Text("Stop")
                    }
                }
                else -> {}
            }
        }
    )
}

@Composable
private fun ElementPreview(elementInfo: ElementInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Element Info",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Type:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = elementInfo.type,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Label:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = elementInfo.genericLabel,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Show existing commands if any
            if (elementInfo.existingCommands.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Existing commands:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                elementInfo.existingCommands.forEach { command ->
                    Text(
                        text = "• $command",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onRecordClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Tap to record a voice command",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onRecordClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Record Command")
        }
    }
}

@Composable
private fun RecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Recording",
            modifier = Modifier.size(48.dp * scale),
            tint = Color.Red
        )
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Red
        )
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = Color.Red
        )
    }
}

@Composable
private fun PhraseReview(
    phrase: String,
    confidence: Double,
    onReRecord: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Recorded",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Recorded phrase:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "\"$phrase\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Confidence: ${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        TextButton(onClick = onReRecord) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Re-record")
        }
    }
}

@Composable
private fun SavingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Saving command...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SuccessMessage(phrase: String, isSynonym: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = "Success",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF4CAF50)
        )
        Text(
            text = if (isSynonym) "Synonym added!" else "Command saved!",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "\"$phrase\"",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
