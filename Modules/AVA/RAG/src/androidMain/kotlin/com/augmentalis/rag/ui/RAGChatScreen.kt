// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/ui/RAGChatScreen.kt
// created: 2025-11-06
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.rag.chat.Source
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * RAG Chat Screen
 *
 * Features:
 * - Streaming chat responses
 * - Source citations
 * - Message history
 * - Error handling
 * - Auto-scroll to latest message
 * - Adaptive layout: Two-pane in landscape (sources sidebar + chat)
 *
 * Usage:
 * ```kotlin
 * RAGChatScreen(
 *     viewModel = chatViewModel,
 *     onNavigateToDocuments = { navController.navigate("documents") }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RAGChatScreen(
    viewModel: RAGChatViewModel,
    onNavigateToDocuments: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()
    val windowSizeClass = rememberWindowSizeClass()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Collect all unique sources from all messages
    val allSources = remember(messages) {
        messages.flatMap { it.sources }
            .distinctBy { it.title }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RAG Chat", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToDocuments) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back to Documents",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Clear chat button
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        enabled = messages.isNotEmpty() && !isGenerating
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Clear Chat",
                            tint = Color.White.copy(alpha = if (messages.isNotEmpty() && !isGenerating) 1f else 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.gradientBackground()
            )
        }
    ) { paddingValues ->
        if (windowSizeClass.isLandscape && windowSizeClass.isMediumOrExpandedWidth) {
            // LANDSCAPE: Two-pane layout (sources sidebar + chat)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // LEFT PANE: Sources Sidebar (35% width)
                if (allSources.isNotEmpty()) {
                    SourcesSidebar(
                        sources = allSources,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.35f)
                    )
                }

                // RIGHT PANE: Chat (65% width)
                ChatPane(
                    messages = messages,
                    error = error,
                    listState = listState,
                    isGenerating = isGenerating,
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            viewModel.askQuestion(inputText)
                            inputText = ""
                        }
                    },
                    onStop = { viewModel.stopGeneration() },
                    onClearError = { viewModel.clearError() },
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(if (allSources.isNotEmpty()) 0.65f else 1f),
                    showSourcesInMessages = false // Sources shown in sidebar
                )
            }
        } else {
            // PORTRAIT: Single column layout (original)
            ChatPane(
                messages = messages,
                error = error,
                listState = listState,
                isGenerating = isGenerating,
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && !isGenerating) {
                        viewModel.askQuestion(inputText)
                        inputText = ""
                    }
                },
                onStop = { viewModel.stopGeneration() },
                onClearError = { viewModel.clearError() },
                modifier = Modifier.fillMaxSize(),
                showSourcesInMessages = true // Sources shown inline
            )
        }
    }
}

/**
 * Sources Sidebar for landscape mode
 */
@Composable
private fun SourcesSidebar(
    sources: List<Source>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Source,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sources (${sources.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sources list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sources, key = { it.title }) { source ->
                    SourceCard(source)
                }
            }
        }
    }
}

/**
 * Source card for sidebar
 */
@Composable
private fun SourceCard(source: Source) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Page ${source.page ?: "?"} • ${(source.similarity * 100).toInt()}% match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Chat pane - works for both portrait and landscape
 */
@Composable
private fun ChatPane(
    messages: List<ChatMessageUI>,
    error: String?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isGenerating: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
    showSourcesInMessages: Boolean = true
) {
    Column(modifier = modifier) {
        // Error banner
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearError) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }
            }
        }

        // Messages
        if (messages.isEmpty()) {
            EmptyChatState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        showSources = showSourcesInMessages
                    )
                }
            }
        }

        // Input area
        ChatInputBar(
            value = inputText,
            onValueChange = onInputTextChange,
            onSend = onSend,
            isGenerating = isGenerating,
            onStop = onStop
        )
    }
}

@Composable
private fun EmptyChatState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ask a Question",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "I'll search your documents and provide accurate answers with sources",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageUI,
    showSources: Boolean = true
) {
    val isUser = message.role == MessageRole.USER
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Slide-in animation (matching HTML demo)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = MessageSlideInSpec
        ) + fadeIn(animationSpec = FadeInSpec)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
            modifier = Modifier.widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Message content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Streaming indicator
                if (message.isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generating...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sources (only if showSources is true)
                if (showSources && message.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sources:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.sources.forEach { source ->
                        SourceChip(source)
                    }
                }

                // Timestamp
                Text(
                    text = dateFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        }
    }
}

@Composable
private fun SourceChip(source: Source) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Page ${source.page ?: "?"} • ${(source.similarity * 100).toInt()}% match",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask a question...") },
                maxLines = 4,
                enabled = !isGenerating,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                // Stop button
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, "Stop")
                }
            } else {
                // Send button with gradient (matching HTML demo)
                FilledIconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank(),
                    modifier = if (value.isNotBlank()) Modifier.gradientBackground() else Modifier,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (value.isNotBlank()) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (value.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}
