package com.augmentalis.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Side overlay panel for conversation history management.
 *
 * Displays a list of previous conversations, allows switching between them,
 * and provides the ability to start a new conversation. Slides in from the
 * right edge when triggered by voice command or UI action.
 *
 * Phase 4 Implementation (Task P4T01):
 * - Side panel (300dp width on desktop, 80% on mobile)
 * - Scrim overlay (60% black) behind panel
 * - Conversation list with titles, timestamps, message counts
 * - Current conversation highlighting
 * - Empty state support
 * - Material motion animations (300ms slide + fade)
 * - Full accessibility support (WCAG AA compliant)
 *
 * Design specifications:
 * - Width: 300dp (max 80% screen width on narrow devices)
 * - Height: Full screen
 * - Animation: slideInHorizontally from right + fadeIn (300ms)
 * - Scrim: 60% opacity black, dismisses on tap
 * - Header: Title + Close button + New Conversation button
 * - Items: Title, timestamp (relative), message count badge, checkmark for current
 *
 * @param show Whether the overlay is visible
 * @param conversations List of conversation summaries to display
 * @param currentConversationId ID of the currently active conversation (highlighted)
 * @param onDismiss Callback when user dismisses the overlay (scrim tap, close button)
 * @param onConversationSelected Callback when user taps a conversation (conversationId: String)
 * @param onNewConversation Callback when user taps "New Conversation" button
 * @param modifier Optional modifier for this composable
 */
@Composable
fun HistoryOverlay(
    show: Boolean,
    conversations: List<ConversationSummary>,
    currentConversationId: String?,
    onDismiss: () -> Unit,
    onConversationSelected: (conversationId: String) -> Unit,
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation specification: 300ms Material ease-in-out
    val animationDurationMs = 300

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(animationSpec = tween(durationMillis = animationDurationMs)),
        exit = fadeOut(animationSpec = tween(durationMillis = animationDurationMs)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Conversation history overlay" }
        ) {
            // Scrim overlay (60% black)
            Scrim(
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize()
            )

            // Side panel (slides in from right)
            AnimatedVisibility(
                visible = show,
                enter = slideInHorizontally(
                    initialOffsetX = { it }, // Start from right edge (full width)
                    animationSpec = tween(durationMillis = animationDurationMs)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it }, // Slide to right edge (full width)
                    animationSpec = tween(durationMillis = animationDurationMs)
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                HistoryPanel(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onDismiss = onDismiss,
                    onConversationSelected = onConversationSelected,
                    onNewConversation = onNewConversation
                )
            }
        }
    }
}

/**
 * Scrim overlay component (semi-transparent background).
 *
 * Displays a 60% opacity black overlay behind the history panel.
 * Tapping the scrim dismisses the overlay.
 *
 * @param onDismiss Callback when scrim is tapped
 * @param modifier Optional modifier for this composable
 */
@Composable
private fun Scrim(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .semantics { contentDescription = "Overlay background, tap to dismiss" }
    )
}

/**
 * History panel component (right side panel).
 *
 * Displays the conversation list with header, search, and actions.
 * Width is 300dp on desktop, 80% on narrow mobile devices.
 *
 * @param conversations List of conversation summaries
 * @param currentConversationId Currently active conversation ID
 * @param onDismiss Callback to close the panel
 * @param onConversationSelected Callback when conversation is selected
 * @param onNewConversation Callback when new conversation button is tapped
 */
@Composable
private fun HistoryPanel(
    conversations: List<ConversationSummary>,
    currentConversationId: String?,
    onDismiss: () -> Unit,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 300.dp) // Max 300dp on desktop
            .fillMaxWidth(0.8f), // 80% width on narrow devices
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Conversation history panel" }
        ) {
            // Header section
            HistoryHeader(
                onClose = onDismiss,
                onNewConversation = onNewConversation
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Conversation list or empty state
            if (conversations.isEmpty()) {
                EmptyHistoryState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                ConversationList(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onConversationSelected = onConversationSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Header section of the history panel.
 *
 * Contains the title, close button, and new conversation button.
 * Follows Material 3 design with proper spacing and touch targets.
 *
 * @param onClose Callback when close button is tapped
 * @param onNewConversation Callback when new conversation button is tapped
 */
@Composable
private fun HistoryHeader(
    onClose: () -> Unit,
    onNewConversation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button (left side)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp) // WCAG AA minimum touch target
                .semantics { contentDescription = "Close conversation history" }
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Title (center)
        Text(
            text = "History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // New conversation button (right side)
        IconButton(
            onClick = onNewConversation,
            modifier = Modifier
                .size(48.dp) // WCAG AA minimum touch target
                .semantics { contentDescription = "Start new conversation" }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Empty state display when no conversations exist.
 *
 * Shows a friendly message prompting the user to start chatting.
 * Centered vertically and horizontally in the available space.
 *
 * @param modifier Optional modifier for this composable
 */
@Composable
private fun EmptyHistoryState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Placeholder for illustration (could be replaced with an icon or image)
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No conversations yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start chatting with AVA to\ncreate your first conversation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * Scrollable list of conversations.
 *
 * Displays each conversation as a ConversationItem with dividers.
 * Uses LazyColumn for efficient rendering of large lists.
 *
 * @param conversations List of conversation summaries
 * @param currentConversationId Currently active conversation ID (highlighted)
 * @param onConversationSelected Callback when conversation is tapped
 * @param modifier Optional modifier for this composable
 */
@Composable
private fun ConversationList(
    conversations: List<ConversationSummary>,
    currentConversationId: String?,
    onConversationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.semantics { contentDescription = "Conversation list" },
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = conversations,
            key = { it.id }
        ) { conversation ->
            ConversationItem(
                conversation = conversation,
                isCurrentConversation = conversation.id == currentConversationId,
                onClick = { onConversationSelected(conversation.id) }
            )
        }
    }
}

/**
 * Individual conversation item in the list.
 *
 * Displays:
 * - Conversation title (custom or first message preview)
 * - Relative timestamp (e.g., "2h ago", "Yesterday")
 * - Message count badge
 * - Checkmark icon if current conversation
 * - Highlighted background if current conversation
 *
 * @param conversation Conversation summary data
 * @param isCurrentConversation Whether this is the active conversation
 * @param onClick Callback when item is tapped
 */
@Composable
private fun ConversationItem(
    conversation: ConversationSummary,
    isCurrentConversation: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCurrentConversation) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    val displayTitle = conversation.title ?: conversation.firstMessagePreview
    val relativeTime = formatRelativeTimestamp(conversation.lastMessageTimestamp)

    // Accessibility description
    val accessibilityText = buildString {
        append("Conversation: $displayTitle, ")
        append("$relativeTime, ")
        append("${conversation.messageCount} messages")
        if (isCurrentConversation) {
            append(", currently selected")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            )
            .semantics { contentDescription = accessibilityText },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp) // WCAG AA minimum touch target
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section: Title, timestamp, message count
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentConversation) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Timestamp and message count row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timestamp
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    // Separator dot
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    // Message count badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${conversation.messageCount} msg${if (conversation.messageCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right section: Checkmark icon if current conversation
            if (isCurrentConversation) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Currently selected conversation",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 8.dp)
                )
            }
        }
    }

    // Divider between items
    Divider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/**
 * Data class representing a conversation summary.
 *
 * Contains metadata for displaying a conversation in the history list.
 * Used by the HistoryOverlay component to render conversation items.
 *
 * @property id Unique conversation ID
 * @property title Custom conversation title (null if using first message preview)
 * @property firstMessagePreview Preview of the first message (used as fallback title)
 * @property messageCount Total number of messages in the conversation
 * @property lastMessageTimestamp Unix timestamp (milliseconds) of the last message
 */
data class ConversationSummary(
    val id: String,
    val title: String?,
    val firstMessagePreview: String,
    val messageCount: Int,
    val lastMessageTimestamp: Long
)

/**
 * Formats Unix timestamp to human-readable relative time.
 *
 * Examples:
 * - "Just now" (< 1 minute)
 * - "2m ago" (< 1 hour)
 * - "2h ago" (< 24 hours)
 * - "Yesterday" (< 48 hours)
 * - "Jan 15" (< 1 year)
 * - "Jan 15, 2024" (>= 1 year)
 *
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted relative time string
 */
private fun formatRelativeTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 2 -> "Yesterday"
        days < 365 -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

// ==================== Previews ====================

/**
 * Preview: Empty state (no conversations)
 */
@Preview(name = "Empty State", showBackground = true)
@Composable
private fun HistoryOverlayEmptyPreview() {
    MaterialTheme {
        HistoryOverlay(
            show = true,
            conversations = emptyList(),
            currentConversationId = null,
            onDismiss = {},
            onConversationSelected = {},
            onNewConversation = {}
        )
    }
}

/**
 * Preview: With conversations (light theme)
 */
@Preview(name = "With Conversations - Light", showBackground = true)
@Composable
private fun HistoryOverlayWithConversationsPreview() {
    val mockConversations = listOf(
        ConversationSummary(
            id = "conv_1",
            title = "Smart Home Control",
            firstMessagePreview = "Turn on the lights",
            messageCount = 24,
            lastMessageTimestamp = System.currentTimeMillis() - 7200000 // 2 hours ago
        ),
        ConversationSummary(
            id = "conv_2",
            title = null, // Uses first message as title
            firstMessagePreview = "What's the weather like today?",
            messageCount = 8,
            lastMessageTimestamp = System.currentTimeMillis() - 86400000 // Yesterday
        ),
        ConversationSummary(
            id = "conv_3",
            title = "Music Playlist",
            firstMessagePreview = "Play some jazz music",
            messageCount = 15,
            lastMessageTimestamp = System.currentTimeMillis() - 259200000 // 3 days ago
        ),
        ConversationSummary(
            id = "conv_4",
            title = "Grocery Shopping",
            firstMessagePreview = "Add milk to my shopping list",
            messageCount = 42,
            lastMessageTimestamp = System.currentTimeMillis() - 604800000 // 1 week ago
        )
    )

    MaterialTheme {
        HistoryOverlay(
            show = true,
            conversations = mockConversations,
            currentConversationId = "conv_1", // First conversation is current
            onDismiss = {},
            onConversationSelected = {},
            onNewConversation = {}
        )
    }
}

/**
 * Preview: With conversations (dark theme)
 */
@Preview(
    name = "With Conversations - Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HistoryOverlayDarkPreview() {
    val mockConversations = listOf(
        ConversationSummary(
            id = "conv_1",
            title = "Smart Home Control",
            firstMessagePreview = "Turn on the lights",
            messageCount = 24,
            lastMessageTimestamp = System.currentTimeMillis() - 7200000
        ),
        ConversationSummary(
            id = "conv_2",
            title = null,
            firstMessagePreview = "What's the weather like today?",
            messageCount = 8,
            lastMessageTimestamp = System.currentTimeMillis() - 86400000
        ),
        ConversationSummary(
            id = "conv_3",
            title = "Music Playlist",
            firstMessagePreview = "Play some jazz music",
            messageCount = 15,
            lastMessageTimestamp = System.currentTimeMillis() - 259200000
        )
    )

    MaterialTheme {
        HistoryOverlay(
            show = true,
            conversations = mockConversations,
            currentConversationId = "conv_2", // Second conversation is current
            onDismiss = {},
            onConversationSelected = {},
            onNewConversation = {}
        )
    }
}

/**
 * Preview: Current conversation highlighted
 */
@Preview(name = "Current Conversation Highlighted", showBackground = true)
@Composable
private fun HistoryOverlayCurrentHighlightedPreview() {
    val mockConversations = listOf(
        ConversationSummary(
            id = "conv_1",
            title = "Weather and News",
            firstMessagePreview = "Tell me the weather",
            messageCount = 12,
            lastMessageTimestamp = System.currentTimeMillis() - 3600000
        ),
        ConversationSummary(
            id = "conv_2",
            title = "Current Chat",
            firstMessagePreview = "This is the active conversation",
            messageCount = 5,
            lastMessageTimestamp = System.currentTimeMillis() - 300000 // 5 minutes ago
        ),
        ConversationSummary(
            id = "conv_3",
            title = "Old Discussion",
            firstMessagePreview = "An older conversation",
            messageCount = 30,
            lastMessageTimestamp = System.currentTimeMillis() - 172800000 // 2 days ago
        )
    )

    MaterialTheme {
        HistoryOverlay(
            show = true,
            conversations = mockConversations,
            currentConversationId = "conv_2", // Middle conversation is current
            onDismiss = {},
            onConversationSelected = {},
            onNewConversation = {}
        )
    }
}

/**
 * Preview: Panel only (no scrim) - Light theme
 */
@Preview(name = "Panel Only - Light", showBackground = true)
@Composable
private fun HistoryPanelPreview() {
    val mockConversations = listOf(
        ConversationSummary(
            id = "conv_1",
            title = "Smart Home",
            firstMessagePreview = "Turn on the lights",
            messageCount = 24,
            lastMessageTimestamp = System.currentTimeMillis() - 7200000
        ),
        ConversationSummary(
            id = "conv_2",
            title = null,
            firstMessagePreview = "What's the weather like?",
            messageCount = 8,
            lastMessageTimestamp = System.currentTimeMillis() - 86400000
        )
    )

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            HistoryPanel(
                conversations = mockConversations,
                currentConversationId = "conv_1",
                onDismiss = {},
                onConversationSelected = {},
                onNewConversation = {}
            )
        }
    }
}

/**
 * Preview: Panel only (no scrim) - Dark theme
 */
@Preview(
    name = "Panel Only - Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HistoryPanelDarkPreview() {
    val mockConversations = listOf(
        ConversationSummary(
            id = "conv_1",
            title = "Smart Home",
            firstMessagePreview = "Turn on the lights",
            messageCount = 24,
            lastMessageTimestamp = System.currentTimeMillis() - 7200000
        ),
        ConversationSummary(
            id = "conv_2",
            title = null,
            firstMessagePreview = "What's the weather like?",
            messageCount = 8,
            lastMessageTimestamp = System.currentTimeMillis() - 86400000
        )
    )

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            HistoryPanel(
                conversations = mockConversations,
                currentConversationId = "conv_1",
                onDismiss = {},
                onConversationSelected = {},
                onNewConversation = {}
            )
        }
    }
}

/**
 * Preview: Empty state panel only
 */
@Preview(name = "Empty State Panel", showBackground = true)
@Composable
private fun HistoryPanelEmptyPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            HistoryPanel(
                conversations = emptyList(),
                currentConversationId = null,
                onDismiss = {},
                onConversationSelected = {},
                onNewConversation = {}
            )
        }
    }
}
