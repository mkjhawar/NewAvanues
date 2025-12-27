/**
 * CommandAssignmentDialog.kt - UI for manual voice command assignment
 *
 * Part of Metadata Quality Overlay & Manual Command Assignment feature (VOS-META-001)
 * Created: 2025-12-03
 * Updated: 2025-12-03 - Ocean Theme Glassmorphic Styling
 *
 * Jetpack Compose dialog for recording and assigning voice commands to UI elements.
 * Supports primary commands and synonyms.
 *
 * STYLING: Ocean Theme Glassmorphic Design
 * - Blue/teal gradient backgrounds with transparency
 * - Glass-like blur effects
 * - Smooth animations and transitions
 * - Depth with subtle shadows
 *
 * MAGICUI MIGRATION: 1:1 Component Mapping
 * - OceanCard -> MagicCard
 * - OceanButton -> MagicButton
 * - OceanTextField -> MagicTextField
 * - When MagicUI is ready, simple find/replace will migrate all components
 */
package com.augmentalis.voiceoscore.commands

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.ElementCommandDTO
import com.augmentalis.voiceoscore.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Command Assignment Dialog - Ocean Theme
 *
 * Show dialog for recording and assigning voice commands to an element.
 *
 * @param elementUuid Element UUID (from ThirdPartyUuidGenerator)
 * @param appId Package name
 * @param onDismiss Called when dialog is dismissed
 * @param onCommandSaved Called when command is successfully saved
 */
@Composable
fun CommandAssignmentDialog(
    elementUuid: String,
    appId: String,
    onDismiss: () -> Unit,
    onCommandSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: CommandAssignmentViewModel = viewModel(
        factory = CommandAssignmentViewModelFactory(context, elementUuid, appId)
    )

    LaunchedEffect(Unit) {
        viewModel.loadExistingCommands()
    }

    Dialog(onDismissRequest = onDismiss) {
        // Glassmorphic ocean-themed card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            DialogContent(
                viewModel = viewModel,
                onDismiss = onDismiss,
                onCommandSaved = onCommandSaved
            )
        }
    }
}

@Composable
private fun DialogContent(
    viewModel: CommandAssignmentViewModel,
    onDismiss: () -> Unit,
    onCommandSaved: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()

    Column {
        // Header with Ocean gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OceanGradients.Teal)
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            Column {
                Text(
                    text = "Assign Voice Command",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OceanColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Speak a voice command to control this element",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recording Section
        RecordingSection(
            recordingState = recordingState,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() },
            onCancelRecording = { viewModel.cancelRecording() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Existing Commands
        if (uiState.existingCommands.isNotEmpty()) {
            ExistingCommandsSection(
                commands = uiState.existingCommands,
                onDeleteCommand = { viewModel.deleteCommand(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error Message
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ErrorMessage(message = uiState.errorMessage ?: "")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons with Ocean styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            // Cancel button - Glass style
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = OceanGlass.Border,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    "Cancel",
                    color = OceanColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Save button - Teal gradient
            Button(
                onClick = {
                    viewModel.saveCommand { commandPhrase ->
                        onCommandSaved(commandPhrase)
                        onDismiss()
                    }
                },
                enabled = recordingState is RecordingState.Result,
                modifier = Modifier
                    .background(
                        brush = OceanGradients.Teal,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    "Save",
                    color = OceanColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun RecordingSection(
    recordingState: RecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    // Ocean glass panel
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(
                width = 1.dp,
                color = OceanGlass.Border,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                brush = OceanGradients.Glass,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (recordingState) {
            is RecordingState.Ready -> {
                RecordButton(onClick = onStartRecording)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tap to record",
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanColors.TextSecondary
                )
            }

            is RecordingState.Listening -> {
                PulsingRecordButton(onClick = onStopRecording)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = recordingState.partialText ?: "Listening...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OceanColors.TealPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to stop",
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanColors.TextSecondary
                )
            }

            is RecordingState.Processing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = OceanColors.TealPrimary,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Processing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanColors.TextPrimary
                )
            }

            is RecordingState.Result -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    modifier = Modifier.size(56.dp),
                    tint = OceanColors.Success
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\"${recordingState.text}\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OceanColors.TealPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confidence: ${(recordingState.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanColors.TextSecondary
                )
            }

            is RecordingState.Error -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(56.dp),
                    tint = OceanColors.Error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = recordingState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanColors.Error
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onCancelRecording,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = OceanGlass.Border,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        "Try Again",
                        color = OceanColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordButton(onClick: () -> Unit) {
    // Ocean gradient record button
    Box(
        modifier = Modifier
            .size(72.dp)
            .border(
                width = 2.dp,
                brush = OceanGradients.Teal,
                shape = CircleShape
            )
            .background(
                brush = OceanGradients.Teal,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Record",
                tint = OceanColors.TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun PulsingRecordButton(onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    // Pulsing red/teal button with glow effect
    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale.value)
            .border(
                width = 2.dp,
                color = OceanColors.Error.copy(alpha = alpha.value),
                shape = CircleShape
            )
            .background(
                color = OceanColors.Error.copy(alpha = alpha.value),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop",
                tint = OceanColors.TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun ExistingCommandsSection(
    commands: List<ElementCommandDTO>,
    onDeleteCommand: (Long) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Existing Commands",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = OceanColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            items(commands) { command ->
                CommandItem(
                    command = command,
                    onDelete = { onDeleteCommand(command.id) }
                )
            }
        }
    }
}

@Composable
private fun CommandItem(
    command: ElementCommandDTO,
    onDelete: () -> Unit
) {
    // Ocean glass card for each command
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = if (command.isSynonym) {
                    OceanGlass.Border
                } else {
                    OceanColors.TealSecondary.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (command.isSynonym) {
            OceanGlass.Surface
        } else {
            OceanColors.TealPrimary.copy(alpha = 0.15f)
        },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = if (command.isSynonym) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                OceanGlass.Blur,
                                OceanGlass.Surface
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                OceanColors.TealPrimary.copy(alpha = 0.1f),
                                OceanColors.TealGlow.copy(alpha = 0.1f)
                            )
                        )
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!command.isSynonym) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Primary",
                        modifier = Modifier.size(18.dp),
                        tint = OceanColors.TealPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = command.commandPhrase,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (command.isSynonym) FontWeight.Normal else FontWeight.Bold,
                    color = if (command.isSynonym) OceanColors.TextSecondary else OceanColors.TextPrimary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = OceanColors.Error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    // Ocean glass error panel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(
                width = 1.dp,
                color = OceanColors.Error.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = OceanColors.Error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = OceanColors.Error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = OceanColors.TextPrimary
        )
    }
}

/**
 * UI State for command assignment dialog
 */
data class CommandAssignmentUiState(
    val existingCommands: List<ElementCommandDTO> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for command assignment dialog
 */
class CommandAssignmentViewModel(
    private val context: Context,
    private val elementUuid: String,
    private val appId: String,
    private val databaseManager: VoiceOSDatabaseManager,
    private val elementCommandManager: ElementCommandManager
) : ViewModel() {

    private val speechRecorder = SpeechRecorder(context)

    private val _uiState = MutableStateFlow(CommandAssignmentUiState())
    val uiState: StateFlow<CommandAssignmentUiState> = _uiState.asStateFlow()

    val recordingState = speechRecorder.state

    init {
        // Observe recording state for errors
        viewModelScope.launch {
            recordingState.collect { state ->
                if (state is RecordingState.Error) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = state.message
                    )
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                }
            }
        }
    }

    /**
     * Load existing commands for element
     */
    fun loadExistingCommands() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val commands = elementCommandManager.getCommands(elementUuid)
                _uiState.value = _uiState.value.copy(
                    existingCommands = commands,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load commands: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Start voice recording
     */
    fun startRecording() {
        speechRecorder.startListening()
    }

    /**
     * Stop voice recording
     */
    fun stopRecording() {
        speechRecorder.stopListening()
    }

    /**
     * Cancel recording and reset
     */
    fun cancelRecording() {
        speechRecorder.reset()
    }

    /**
     * Save command to database
     */
    fun saveCommand(onSuccess: (String) -> Unit) {
        val currentState = recordingState.value
        if (currentState !is RecordingState.Result) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No command to save"
            )
            return
        }

        viewModelScope.launch {
            try {
                val commandPhrase = currentState.text
                val result = elementCommandManager.assignCommand(
                    elementUuid = elementUuid,
                    phrase = commandPhrase,
                    appId = appId
                )
                val id = result.getOrDefault(-1L)

                if (id > 0) {
                    onSuccess(commandPhrase)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to save command (duplicate or invalid)"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error saving command: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete a command
     */
    fun deleteCommand(commandId: Long) {
        viewModelScope.launch {
            try {
                elementCommandManager.deleteCommand(commandId)
                loadExistingCommands() // Reload list
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete command: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecorder.destroy()
    }
}

/**
 * Factory for creating CommandAssignmentViewModel
 */
class CommandAssignmentViewModelFactory(
    private val context: Context,
    private val elementUuid: String,
    private val appId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Get database manager singleton
        val databaseManager = VoiceOSDatabaseManager.getInstance(
            com.augmentalis.database.DatabaseDriverFactory(context)
        )
        val elementCommandManager = ElementCommandManager(
            databaseManager.elementCommands,
            databaseManager.qualityMetrics
        )

        @Suppress("UNCHECKED_CAST")
        return CommandAssignmentViewModel(
            context,
            elementUuid,
            appId,
            databaseManager,
            elementCommandManager
        ) as T
    }
}
