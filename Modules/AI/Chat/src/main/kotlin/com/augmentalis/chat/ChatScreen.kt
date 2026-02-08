package com.augmentalis.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.augmentalis.chat.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import com.avanueui.GlassIntensity
import com.avanueui.GlassSurface
import com.avanueui.GlassCard
import com.avanueui.GlassIndicator
import com.avanueui.GlassTextField
import com.avanueui.OceanButton
import com.avanueui.OceanButtonStyle
import com.avanueui.OceanGradients
import com.avanueui.OceanShapes
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.chat.components.ConversationSummary
import com.augmentalis.chat.components.HistoryOverlay
import com.augmentalis.chat.components.MessageBubble
import com.augmentalis.chat.components.StatusIndicator
import com.augmentalis.chat.components.NLUState
import com.augmentalis.chat.components.LLMState
import com.augmentalis.chat.components.TeachAvaBottomSheet
import com.augmentalis.chat.components.ConfidenceLearningDialog
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Main chat screen for AVA AI conversation interface.
 *
 * Displays a conversation with message history, text input, and voice input options.
 * This is the primary user interaction point for communicating with AVA.
 *
 * @param viewModel ViewModel for managing chat state
 * @param modifier Optional modifier for this composable
 * @param onNavigateBack Callback when user navigates back from this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onVoiceInput: (() -> Unit)? = null
) {
    // Collect state from ViewModel
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isNLUReady by viewModel.isNLUReady.collectAsState()

    // Status Indicator state (REQ-001, REQ-002, REQ-003)
    val isNLULoaded by viewModel.isNLULoaded.collectAsState()
    val isLLMLoaded by viewModel.isLLMLoaded.collectAsState()
    val lastResponder by viewModel.lastResponder.collectAsState()
    val lastResponderTimestamp by viewModel.lastResponderTimestamp.collectAsState()

    // Compute NLU state for StatusIndicator
    val nluState = when {
        !isNLULoaded -> NLUState.NOT_LOADED
        !isNLUReady -> NLUState.INITIALIZING
        lastResponder == "NLU" &&
            (System.currentTimeMillis() - lastResponderTimestamp < 2000) -> NLUState.ACTIVE
        else -> NLUState.READY
    }

    // Compute LLM state for StatusIndicator
    val llmState = when {
        !isLLMLoaded -> LLMState.NOT_LOADED
        lastResponder == "LLM" &&
            (System.currentTimeMillis() - lastResponderTimestamp < 2000) -> LLMState.ACTIVE
        else -> LLMState.READY
    }

    // Teach-AVA bottom sheet state (Phase 3)
    val showTeachBottomSheet by viewModel.showTeachBottomSheet.collectAsState()
    val currentTeachMessageId by viewModel.currentTeachMessageId.collectAsState()
    val candidateIntents by viewModel.candidateIntents.collectAsState()

    // Get current message being taught
    val currentTeachMessage = currentTeachMessageId?.let {
        viewModel.getCurrentTeachMessage()
    }

    // Confidence learning dialog state (REQ-004)
    val confidenceLearningDialogState by viewModel.confidenceLearningDialogState.collectAsState()

    // Developer settings (REQ-007)
    val isFlashModeEnabled by viewModel.isFlashModeEnabled.collectAsState()

    // History overlay state (Phase 4)
    val showHistoryOverlay by viewModel.showHistoryOverlay.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()

    // Pagination state (Phase 5, Task P5T04)
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState()
    val totalMessageCount by viewModel.totalMessageCount.collectAsState()

    // TTS state (Phase 1.2 - Ready checks)
    val isTTSReady by viewModel.isTTSReady.collectAsState()
    val isTTSSpeaking by viewModel.isTTSSpeaking.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val ttsSettings by viewModel.ttsSettings.collectAsState()

    // LazyColumn state for auto-scrolling
    val listState = rememberLazyListState()

    // Snackbar host state for error messages (Critical UX Fix #2)
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error as Snackbar instead of blocking banner (Critical UX Fix #2)
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.clearError()
            }
        }
    }

    // Ocean Glass UI v2.2: No TopAppBar - transparent background shows Ocean gradient
    // StatusIndicator moved to NavigationRail header in landscape mode
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent, // Transparent for Ocean gradient
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.error
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Compact Status Header - Ocean Glass UI v2.2
            // Shows AVA/AI status in a minimal glass bar at top
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                intensity = GlassIntensity.LIGHT,
                shape = RoundedCornerShape(ShapeTokens.sm),
                showBorder = false
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator (left side)
                    StatusIndicator(
                        nluState = nluState,
                        llmState = llmState,
                        isTestingModeEnabled = isFlashModeEnabled
                    )

                    // Teach button (right side) - only when messages exist
                    if (messages.isNotEmpty()) {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                val lastMessage = messages.lastOrNull()
                                if (lastMessage != null) {
                                    viewModel.activateTeachMode(lastMessage.id)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .semantics {
                                    contentDescription = "Teach AVA"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "Teach AVA",
                                tint = AvanueTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Message list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    // Empty state with Ocean Glass design
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            modifier = Modifier.padding(32.dp),
                            intensity = GlassIntensity.MEDIUM,
                            shape = RoundedCornerShape(ShapeTokens.lg)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                // Large icon with Ocean primary color
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = AvanueTheme.colors.primary.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                // Primary message
                                Text(
                                    text = stringResource(R.string.chat_empty_state_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = AvanueTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Secondary guidance
                                Text(
                                    text = stringResource(R.string.chat_empty_state_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AvanueTheme.colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                // Example prompts in a glass chip style
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        stringResource(R.string.chat_example_time),
                                        stringResource(R.string.chat_example_lights),
                                        stringResource(R.string.chat_example_reminder)
                                    ).forEach { example ->
                                        Text(
                                            text = example,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AvanueTheme.colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // LazyColumn with messages (reverse layout - latest at bottom)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // Load More button at top (Phase 5, Task P5T04)
                        // Edge case: Boundary check to prevent negative message count display
                        if (hasMoreMessages && totalMessageCount > messages.size) {
                            item(key = "load_more_button") {
                                // Edge case: Calculate remaining messages safely (prevent negative)
                                val remainingMessages = maxOf(0, totalMessageCount - messages.size)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadMoreMessages() },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = if (isLoading) {
                                                "Loading..."
                                            } else {
                                                "Load More ($remainingMessages older messages)"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
                            val isAvaMessage = message.role == MessageRole.ASSISTANT
                            MessageBubble(
                                content = message.content,
                                isUserMessage = message.role == MessageRole.USER,
                                timestamp = message.timestamp,
                                confidence = message.confidence,
                                onConfirm = { /* No-op: Confirmation feature deferred to future phase */ },
                                onTeachAva = { viewModel.activateTeachMode(message.id) },
                                onLongPress = { viewModel.activateTeachMode(message.id) },
                                // TTS parameters (Phase 1.2 - Ready checks)
                                onSpeak = if (isAvaMessage) {
                                    { viewModel.speakMessage(message.content, message.id) }
                                } else null,
                                onStopSpeaking = if (isAvaMessage) {
                                    { viewModel.stopSpeaking() }
                                } else null,
                                isSpeaking = speakingMessageId == message.id && isTTSSpeaking,
                                ttsEnabled = isTTSReady && ttsSettings.enabled
                            )
                        }

                        // Typing indicator - Ocean Glass design
                        // Shows below messages instead of overlapping them
                        if (isLoading) {
                            item(key = "typing_indicator") {
                                GlassSurface(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .widthIn(max = 160.dp),
                                    intensity = GlassIntensity.LIGHT,
                                    shape = RoundedCornerShape(
                                        topStart = ShapeTokens.lg,
                                        topEnd = ShapeTokens.lg,
                                        bottomStart = ShapeTokens.xs,
                                        bottomEnd = ShapeTokens.lg
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = AvanueTheme.colors.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.chat_typing_indicator),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AvanueTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input field with send button and voice input
            MessageInputField(
                onSendMessage = { text ->
                    viewModel.sendMessage(text)
                },
                onVoiceInput = onVoiceInput,
                enabled = !isLoading,
                isLoading = isLoading, // Pass loading state for send button indicator
                ragEnabled = viewModel.ragEnabled,
                selectedDocumentIds = viewModel.selectedDocumentIds,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Mutual exclusion: Only show one modal at a time
        // Priority: ConfidenceLearningDialog > TeachAvaBottomSheet
        // This prevents transparent layers from stacking and causing visual chaos

        // Confidence Learning Dialog (REQ-004) - Higher priority, shows first
        val isConfidenceDialogShowing = confidenceLearningDialogState != null

        confidenceLearningDialogState?.let { state ->
            ConfidenceLearningDialog(
                state = state,
                onConfirm = { viewModel.confirmInterpretation() },
                onSelectAlternate = { alternate -> viewModel.selectAlternateIntent(alternate) },
                onDismiss = { viewModel.dismissConfidenceLearningDialog() }
            )
        }

        // Teach AVA bottom sheet (Phase 3)
        // Only show when ConfidenceLearningDialog is NOT showing (mutual exclusion)
        // Edge case: Defensive null check - both conditions must be true
        if (showTeachBottomSheet && currentTeachMessage != null && !isConfidenceDialogShowing) {
            // Edge case: Validate message has content before showing bottom sheet
            val messageContent = currentTeachMessage.content
            if (messageContent.isNotBlank()) {
                TeachAvaBottomSheet(
                    show = showTeachBottomSheet,
                    onDismiss = { viewModel.dismissTeachBottomSheet() },
                    messageId = currentTeachMessage.id,
                    userUtterance = messageContent,
                    suggestedIntent = currentTeachMessage.intent,
                    existingIntents = candidateIntents,
                    onSubmit = { utterance, intent ->
                        viewModel.handleTeachAva(currentTeachMessage.id, intent)
                    }
                )
            }
        }

        // ADR-014 Phase D3: Accessibility permission prompt dialog
        val showAccessibilityPrompt by viewModel.showAccessibilityPrompt.collectAsState()
        if (showAccessibilityPrompt) {
            AccessibilityPromptDialog(
                onDismiss = { viewModel.dismissAccessibilityPrompt() },
                onOpenSettings = { viewModel.openAccessibilitySettings() }
            )
        }

        // History overlay (Phase 4)
        HistoryOverlay(
            show = showHistoryOverlay,
            conversations = conversations.map { conv ->
                ConversationSummary(
                    id = conv.id,
                    title = conv.title,
                    firstMessagePreview = conv.preview,
                    messageCount = conv.messageCount,
                    lastMessageTimestamp = conv.updatedAt
                )
            },
            currentConversationId = activeConversationId,
            onDismiss = { viewModel.dismissHistory() },
            onConversationSelected = { conversationId ->
                viewModel.switchConversation(conversationId)
            },
            onNewConversation = { viewModel.createNewConversation() }
        )
    }
}

/**
 * Message input field component with send button and voice input.
 *
 * Phase 1.2: Added voice input support for hands-free message entry.
 * RAG Phase 2: Added visual RAG active indicator above input field.
 * Critical UX Fix #1: Added loading indicator in send button.
 *
 * @param onSendMessage Callback when user sends a message
 * @param enabled Whether input is enabled (disabled during loading)
 * @param isLoading Whether a message is being processed (shows spinner in send button)
 * @param ragEnabled StateFlow indicating if RAG is enabled
 * @param selectedDocumentIds StateFlow with list of selected document IDs
 * @param modifier Optional modifier for this composable
 */
@Composable
private fun MessageInputField(
    onSendMessage: (String) -> Unit,
    onVoiceInput: (() -> Unit)? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    ragEnabled: StateFlow<Boolean> = MutableStateFlow(false),
    selectedDocumentIds: StateFlow<List<String>> = MutableStateFlow(emptyList()),
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    // RAG state
    val ragEnabledValue by ragEnabled.collectAsState()
    val selectedDocumentIdsValue by selectedDocumentIds.collectAsState()
    val isRAGActive = ragEnabledValue && selectedDocumentIdsValue.isNotEmpty()

    // Function to handle sending message
    val sendMessage = {
        val trimmedText = text.trim()
        if (trimmedText.isNotBlank()) {
            onSendMessage(trimmedText)
            text = "" // Clear input after sending
        }
    }


    Column(
        modifier = modifier
    ) {
        // RAG Active Indicator - Ocean Glass Design
        // Shows when RAG is enabled and documents are selected
        if (isRAGActive) {
            GlassIndicator(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                intensity = GlassIntensity.MEDIUM
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "RAG Active",
                    modifier = Modifier.size(20.dp),
                    tint = AvanueTheme.colors.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Searching Your Documents",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.textPrimary
                    )
                    Text(
                        text = "${selectedDocumentIdsValue.size} document${if (selectedDocumentIdsValue.size != 1) "s" else ""} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary
                    )
                }
            }
        }

        // Input area with Ocean Glass styling - consistent radii on all corners
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            intensity = GlassIntensity.LIGHT,
            shape = RoundedCornerShape(ShapeTokens.md), // Consistent radii all corners
            showBorder = true
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice input button - positioned left of text field for easy access
                if (onVoiceInput != null) {
                    IconButton(
                        onClick = onVoiceInput,
                        enabled = enabled && !isLoading,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Input",
                            tint = AvanueTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            // Handle Enter/Return key to send message
                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                if (text.trim().isNotBlank()) {
                                    sendMessage()
                                    true // Consume the event
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        },
                    // Context-aware placeholder text
                    placeholder = {
                        Text(
                            text = if (isRAGActive) {
                                stringResource(R.string.chat_input_rag_placeholder)
                            } else {
                                stringResource(R.string.chat_input_placeholder)
                            },
                            color = AvanueTheme.colors.textDisabled
                        )
                    },
                    enabled = enabled,
                    maxLines = 4,
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            sendMessage()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0DFFFFFF),
                        unfocusedContainerColor = Color(0x0DFFFFFF).copy(alpha = 0.5f),
                        focusedBorderColor = AvanueTheme.colors.primary,
                        unfocusedBorderColor = AvanueTheme.colors.border,
                        focusedTextColor = AvanueTheme.colors.textPrimary,
                        unfocusedTextColor = AvanueTheme.colors.textPrimary,
                        cursorColor = AvanueTheme.colors.primary
                    ),
                    shape = RoundedCornerShape(ShapeTokens.md)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Send button with loading indicator - Ocean styled
                IconButton(
                    onClick = sendMessage,
                    enabled = enabled && text.isNotBlank() && !isLoading,
                    modifier = Modifier.size(48.dp) // WCAG AA touch target
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = AvanueTheme.colors.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = if (text.isNotBlank()) "Send message" else "Type a message first",
                            tint = if (text.isNotBlank()) {
                                AvanueTheme.colors.primary
                            } else {
                                AvanueTheme.colors.textDisabled
                            }
                        )
                    }
                }
            }
        }
    }

    // Voice transcription handling removed - will be re-implemented with VoiceOS in Phase 4.0
}

/**
 * ADR-014 Phase D3: Accessibility permission prompt dialog.
 *
 * Shown when user triggers a gesture/cursor command but VoiceOS accessibility
 * service is not enabled. Guides user to enable the service for full functionality.
 *
 * @param onDismiss Callback when user dismisses the dialog
 * @param onOpenSettings Callback when user wants to open accessibility settings
 */
@Composable
private fun AccessibilityPromptDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = AvanueTheme.colors.primary
            )
        },
        title = {
            Text(
                text = "Enable Accessibility Service",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "To use voice commands for gestures, scrolling, and screen control, " +
                    "please enable the VoiceOS Accessibility Service in your device settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            OceanButton(
                onClick = onOpenSettings,
                style = OceanButtonStyle.PRIMARY
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            OceanButton(
                onClick = onDismiss,
                style = OceanButtonStyle.SECONDARY
            ) {
                Text("Later")
            }
        },
        containerColor = AvanueTheme.colors.surfaceVariant,
        shape = RoundedCornerShape(ShapeTokens.lg)
    )
}

/**
 * Preview for ChatScreen in light theme with empty state.
 */
@Preview(showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    MaterialTheme {
        ChatScreen()
    }
}

/**
 * Preview for ChatScreen in dark theme with messages.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatScreenWithMessagesPreview() {
    MaterialTheme {
        // Note: Preview with mock ViewModel would be added here
        // For now, shows empty state
        ChatScreen()
    }
}
