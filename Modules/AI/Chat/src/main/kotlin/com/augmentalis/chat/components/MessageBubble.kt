package com.augmentalis.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avanueui.ColorTokens
import com.avanueui.GlassIntensity
import com.avanueui.ShapeTokens
import com.avanueui.SizeTokens
import com.augmentalis.chat.domain.SourceCitation
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Message bubble component for displaying user and AVA messages.
 *
 * Displays messages with appropriate styling based on the sender (user vs AVA),
 * including background color, alignment, timestamp formatting, and confidence badges.
 *
 * Phase 2 Enhancement: Confidence badges with three visual states:
 * - High (>70%): Green badge with percentage only
 * - Medium (50-70%): Yellow badge with "Confirm?" button
 * - Low (<50%): Red badge with "Teach AVA" button
 *
 * Phase 3 Enhancement (P3T02): Long-press context menu
 * - Long-press on any message to show "Teach AVA this" option
 * - Allows teaching AVA from any message, not just low-confidence ones
 *
 * RAG Phase 2 Enhancement: Source citations
 * - Display document sources that contributed to RAG-enhanced responses
 * - Collapsible citations section with Material 3 chips
 * - Shows document title, page number (if available), and similarity score
 *
 * @param content The message text content
 * @param isUserMessage True if message is from user, false if from AVA
 * @param timestamp Unix timestamp (milliseconds) when message was created
 * @param confidence Optional confidence score (0.0-1.0) for AVA messages (Phase 2)
 * @param onConfirm Callback when user taps "Confirm?" button (medium confidence)
 * @param onTeachAva Callback when user taps "Teach AVA" button (low confidence)
 * @param onLongPress Callback when user long-presses the message bubble (Phase 3, P3T02)
 * @param onSpeak Callback when user taps speak button (Phase 1.2)
 * @param onStopSpeaking Callback when user taps stop button (Phase 1.2)
 * @param isSpeaking True if this message is currently being spoken (Phase 1.2)
 * @param ttsEnabled True if TTS is available (Phase 1.2)
 * @param sourceCitations Optional list of source citations from RAG retrieval (RAG Phase 2)
 * @param modifier Optional modifier for this composable
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    content: String,
    isUserMessage: Boolean,
    timestamp: Long,
    confidence: Float? = null,
    onConfirm: (() -> Unit)? = null,
    onTeachAva: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    onStopSpeaking: (() -> Unit)? = null,
    isSpeaking: Boolean = false,
    ttsEnabled: Boolean = false,
    sourceCitations: List<SourceCitation> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Context menu state (Phase 3, P3T02)
    var showContextMenu by remember { mutableStateOf(false) }
    // Citations expansion state (RAG Phase 2)
    var citationsExpanded by remember { mutableStateOf(sourceCitations.isNotEmpty()) }

    // Determine alignment based on sender
    val alignment = if (isUserMessage) Alignment.End else Alignment.Start

    // Ocean Glass theme colors
    val backgroundColor = if (isUserMessage) {
        ColorTokens.Primary.copy(alpha = 0.9f)  // User: Teal with slight transparency
    } else {
        ColorTokens.GlassMedium  // AVA: Glass effect (15% white)
    }
    val textColor = if (isUserMessage) {
        ColorTokens.OnPrimary
    } else {
        ColorTokens.TextPrimary
    }

    // Bubble shape with asymmetric corners
    val bubbleShape = RoundedCornerShape(
        topStart = ShapeTokens.Large,
        topEnd = ShapeTokens.Large,
        bottomStart = if (isUserMessage) ShapeTokens.Large else ShapeTokens.ExtraSmall,
        bottomEnd = if (isUserMessage) ShapeTokens.ExtraSmall else ShapeTokens.Large
    )

    // Format timestamp to relative time
    val relativeTime = formatRelativeTime(timestamp)

    // Accessibility description
    val accessibilityDescription = if (isUserMessage) {
        "You said: $content, $relativeTime"
    } else {
        "AVA said: $content, $relativeTime" + (confidence?.let { ", confidence ${(it * 100).toInt()}%" } ?: "") +
        if (sourceCitations.isNotEmpty()) ", ${sourceCitations.size} sources cited" else ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { contentDescription = accessibilityDescription },
        horizontalAlignment = alignment
    ) {
        // Message bubble with Ocean Glass design
        Box(
            modifier = Modifier.align(alignment)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = SizeTokens.ChatBubbleMaxWidth) // Using design token
                    .clip(bubbleShape)
                    .background(color = backgroundColor)
                    .then(
                        // Add glass border for AVA messages
                        if (!isUserMessage) {
                            Modifier.border(
                                width = 1.dp,
                                color = ColorTokens.OutlineVariant,
                                shape = bubbleShape
                            )
                        } else Modifier
                    )
                    .combinedClickable(
                        onClick = { /* Regular click does nothing (handled by buttons if needed) */ },
                        onLongClick = {
                            if (onLongPress != null) {
                                showContextMenu = true
                                onLongPress()
                            }
                        },
                        onLongClickLabel = "Show options menu"
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }

            // Context menu with Ocean Glass styling
            if (onLongPress != null) {
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    modifier = Modifier
                        .background(color = ColorTokens.GlassHeavy)
                        .semantics { contentDescription = "Message options menu" }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = ColorTokens.Primary
                                )
                                Text(
                                    text = "Teach AVA this",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ColorTokens.TextPrimary
                                )
                            }
                        },
                        onClick = {
                            showContextMenu = false
                            onLongPress()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SizeTokens.MinTouchTarget)
                    )
                }
            }
        }

        // Row for timestamp and TTS button - Ocean Glass design
        Row(
            modifier = Modifier
                .align(alignment)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Timestamp with Ocean theme colors
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTokens.TextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // TTS button for AVA messages (Phase 1.2)
            // High Priority Fix: Increased size for better touch target
            if (!isUserMessage && onSpeak != null && onStopSpeaking != null && ttsEnabled) {
                TTSButton(
                    isSpeaking = isSpeaking,
                    enabled = ttsEnabled,
                    onSpeak = onSpeak,
                    onStop = onStopSpeaking,
                    modifier = Modifier.size(40.dp) // Increased from 32dp for better touch target
                )
            }
        }

        // Confidence badge (Phase 2 - P2T05)
        // High Priority Fix: Improved spacing for better visual hierarchy
        if (!isUserMessage && confidence != null) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                ConfidenceBadge(
                    confidence = confidence,
                    onConfirm = onConfirm,
                    onTeachAva = onTeachAva,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp) // Increased from 4dp for breathing room
                )
            }
        }

        // Source citations section (RAG Phase 2)
        if (!isUserMessage && sourceCitations.isNotEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SourceCitationsSection(
                    citations = sourceCitations,
                    isExpanded = citationsExpanded,
                    onExpandedChange = { citationsExpanded = it },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Confidence badge component for AVA messages.
 *
 * Displays a color-coded badge indicating AVA's confidence in her response,
 * with appropriate action buttons based on confidence level.
 *
 * Design specifications:
 * - High confidence (>70%): Green badge, percentage only
 * - Medium confidence (50-70%): Yellow badge, "Confirm?" button
 * - Low confidence (<50%): Red badge, "Teach AVA" button
 *
 * Accessibility:
 * - Uses both color AND text to convey confidence level (WCAG AA compliant)
 * - 48dp minimum touch targets for interactive buttons
 * - Semantic content descriptions for screen readers
 *
 * @param confidence Confidence score (0.0-1.0)
 * @param onConfirm Callback when user taps "Confirm?" (medium confidence)
 * @param onTeachAva Callback when user taps "Teach AVA" (low confidence)
 * @param modifier Optional modifier for this composable
 */
@Composable
private fun ConfidenceBadge(
    confidence: Float,
    onConfirm: (() -> Unit)?,
    onTeachAva: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val percentage = (confidence * 100).toInt()
    val confidenceLevel = when {
        confidence >= 0.7f -> ConfidenceLevel.HIGH
        confidence >= 0.5f -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }

    // Ocean theme color scheme based on confidence level
    val badgeColor = when (confidenceLevel) {
        ConfidenceLevel.HIGH -> ColorTokens.Success    // SeafoamGreen
        ConfidenceLevel.MEDIUM -> ColorTokens.Warning  // Amber
        ConfidenceLevel.LOW -> ColorTokens.Error       // CoralRed
    }

    val contentColor = ColorTokens.OnPrimary // High contrast against all badge colors

    // Accessibility description
    val accessibilityText = when (confidenceLevel) {
        ConfidenceLevel.HIGH -> "High confidence: $percentage percent"
        ConfidenceLevel.MEDIUM -> "Medium confidence: $percentage percent, tap to confirm"
        ConfidenceLevel.LOW -> "Low confidence: $percentage percent, tap to teach AVA"
    }

    Column(
        modifier = modifier.semantics { contentDescription = accessibilityText },
        horizontalAlignment = Alignment.End
    ) {
        // Badge with percentage
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = badgeColor,
            modifier = Modifier.height(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Color indicator circle
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(contentColor.copy(alpha = 0.9f))
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Percentage text
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action buttons based on confidence level - Ocean theme
        when (confidenceLevel) {
            ConfidenceLevel.MEDIUM -> {
                if (onConfirm != null) {
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = SizeTokens.MinTouchTarget),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorTokens.Primary
                        )
                    ) {
                        Text(
                            text = "Confirm?",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            ConfidenceLevel.LOW -> {
                if (onTeachAva != null) {
                    FilledTonalButton(
                        onClick = onTeachAva,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = SizeTokens.MinTouchTarget),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ColorTokens.Error.copy(alpha = 0.15f),
                            contentColor = ColorTokens.Error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Teach AVA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            ConfidenceLevel.HIGH -> {
                // No action button for high confidence
            }
        }
    }
}

/**
 * Confidence level enum for badge variants.
 */
private enum class ConfidenceLevel {
    HIGH,   // >70%: Green badge
    MEDIUM, // 50-70%: Yellow badge
    LOW     // <50%: Red badge
}

/**
 * Source citations section component for RAG-enhanced messages.
 *
 * Displays document sources as collapsible Material 3 chips with:
 * - Document title
 * - Page number (if available)
 * - Similarity percentage
 *
 * Design specifications:
 * - Compact header with toggle button when collapsed
 * - Expandable flow row of chips when expanded
 * - Material 3 AssistChip for each citation
 * - Leading icon showing document symbol
 * - Proper spacing and alignment
 *
 * @param citations List of source citations to display
 * @param isExpanded Whether the section is expanded
 * @param onExpandedChange Callback when user toggles expansion
 * @param modifier Optional modifier for this composable
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SourceCitationsSection(
    citations: List<SourceCitation>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Source citations section with ${citations.size} sources" }
    ) {
        // Collapsible header with Ocean Glass styling
        Surface(
            shape = RoundedCornerShape(ShapeTokens.Small),
            color = ColorTokens.GlassLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onExpandedChange(!isExpanded) },
                        onLongClickLabel = "Toggle citations"
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ColorTokens.Primary
                    )
                    Text(
                        text = "Sources (${citations.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTokens.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = { onExpandedChange(!isExpanded) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse sources" else "Expand sources",
                        modifier = Modifier.size(20.dp),
                        tint = ColorTokens.Primary
                    )
                }
            }
        }

        // Expanded content with citation chips
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                citations.forEach { citation ->
                    AssistChip(
                        onClick = { /* Optional: navigate to document */ },
                        label = {
                            Text(
                                text = citation.format(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTokens.TextPrimary,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = ColorTokens.Primary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ColorTokens.GlassMedium,
                            labelColor = ColorTokens.TextPrimary,
                            leadingIconContentColor = ColorTokens.Primary
                        ),
                        border = BorderStroke(1.dp, ColorTokens.OutlineVariant),
                        modifier = Modifier.semantics {
                            contentDescription = "${citation.documentTitle}${citation.pageNumber?.let { " page $it" } ?: ""}, ${citation.similarityPercent} percent similarity"
                        }
                    )
                }
            }
        }
    }
}

/**
 * Formats Unix timestamp to human-readable relative time.
 *
 * Examples:
 * - "Just now" (< 1 minute)
 * - "2m ago" (< 1 hour)
 * - "1h ago" (< 24 hours)
 * - "Yesterday 3:15 PM" (< 48 hours)
 * - "Jan 15, 3:15 PM" (older)
 *
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted relative time string
 */
private fun formatRelativeTime(timestamp: Long): String {
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
        days < 2 -> {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday ${timeFormat.format(Date(timestamp))}"
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

// ==================== Previews ====================

@Preview(name = "User Message", showBackground = true)
@Composable
private fun UserMessagePreview() {
    MaterialTheme {
        MessageBubble(
            content = "Turn on the lights",
            isUserMessage = true,
            timestamp = System.currentTimeMillis() - 120000 // 2 minutes ago
        )
    }
}

@Preview(name = "High Confidence (>70%)", showBackground = true)
@Composable
private fun HighConfidencePreview() {
    MaterialTheme {
        MessageBubble(
            content = "I'll control the lights.",
            isUserMessage = false,
            timestamp = System.currentTimeMillis() - 60000, // 1 minute ago
            confidence = 0.85f
        )
    }
}

@Preview(name = "Medium Confidence (50-70%)", showBackground = true)
@Composable
private fun MediumConfidencePreview() {
    MaterialTheme {
        MessageBubble(
            content = "Did you want me to control the lights?",
            isUserMessage = false,
            timestamp = System.currentTimeMillis() - 60000,
            confidence = 0.65f,
            onConfirm = { /* User confirms */ }
        )
    }
}

@Preview(name = "Low Confidence (<50%)", showBackground = true)
@Composable
private fun LowConfidencePreview() {
    MaterialTheme {
        MessageBubble(
            content = "I'm not sure I understood. Would you like to teach me?",
            isUserMessage = false,
            timestamp = System.currentTimeMillis() - 30000,
            confidence = 0.35f,
            onTeachAva = { /* Open teach dialog */ }
        )
    }
}

@Preview(name = "All Variants - Light", showBackground = true)
@Composable
private fun AllVariantsLightPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User message
            MessageBubble(
                content = "Turn on the lights",
                isUserMessage = true,
                timestamp = System.currentTimeMillis() - 180000
            )

            // High confidence AVA response
            MessageBubble(
                content = "I'll control the lights.",
                isUserMessage = false,
                timestamp = System.currentTimeMillis() - 120000,
                confidence = 0.85f
            )

            // Medium confidence AVA response
            MessageBubble(
                content = "Did you want me to check the weather?",
                isUserMessage = false,
                timestamp = System.currentTimeMillis() - 60000,
                confidence = 0.65f,
                onConfirm = { }
            )

            // Low confidence AVA response
            MessageBubble(
                content = "I'm not sure I understood. Would you like to teach me?",
                isUserMessage = false,
                timestamp = System.currentTimeMillis() - 30000,
                confidence = 0.35f,
                onTeachAva = { }
            )
        }
    }
}

@Preview(name = "All Variants - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AllVariantsDarkPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User message
            MessageBubble(
                content = "Turn on the lights",
                isUserMessage = true,
                timestamp = System.currentTimeMillis() - 180000
            )

            // High confidence AVA response
            MessageBubble(
                content = "I'll control the lights.",
                isUserMessage = false,
                timestamp = System.currentTimeMillis() - 120000,
                confidence = 0.85f
            )

            // Medium confidence AVA response
            MessageBubble(
                content = "Did you want me to check the weather?",
                isUserMessage = false,
                timestamp = System.currentTimeMillis() - 60000,
                confidence = 0.65f,
                onConfirm = { }
            )

            // Low confidence AVA response
            MessageBubble(
                content = "I'm not sure I understood. Would you like to teach me?",
                isUserMessage = false,
                timestamp = System.currentTimeMillis() - 30000,
                confidence = 0.35f,
                onTeachAva = { }
            )
        }
    }
}

@Preview(name = "Badge Only - High", showBackground = true)
@Composable
private fun HighConfidenceBadgePreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ConfidenceBadge(
                confidence = 0.85f,
                onConfirm = null,
                onTeachAva = null
            )
        }
    }
}

@Preview(name = "Badge Only - Medium", showBackground = true)
@Composable
private fun MediumConfidenceBadgePreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ConfidenceBadge(
                confidence = 0.65f,
                onConfirm = { },
                onTeachAva = null
            )
        }
    }
}

@Preview(name = "Badge Only - Low", showBackground = true)
@Composable
private fun LowConfidenceBadgePreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ConfidenceBadge(
                confidence = 0.35f,
                onConfirm = null,
                onTeachAva = { }
            )
        }
    }
}

// ==================== Phase 3 Long-Press Previews ====================

@Preview(name = "Message with Long-Press (User)", showBackground = true)
@Composable
private fun MessageWithLongPressUserPreview() {
    MaterialTheme {
        MessageBubble(
            content = "Turn on the lights",
            isUserMessage = true,
            timestamp = System.currentTimeMillis() - 120000,
            onLongPress = { /* Open teach dialog */ }
        )
    }
}

@Preview(name = "Message with Long-Press (AVA)", showBackground = true)
@Composable
private fun MessageWithLongPressAvaPreview() {
    MaterialTheme {
        MessageBubble(
            content = "I'll control the lights.",
            isUserMessage = false,
            timestamp = System.currentTimeMillis() - 60000,
            confidence = 0.85f,
            onLongPress = { /* Open teach dialog */ }
        )
    }
}

@Preview(
    name = "Message with Long-Press - Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MessageWithLongPressDarkPreview() {
    MaterialTheme {
        MessageBubble(
            content = "Turn on the lights",
            isUserMessage = true,
            timestamp = System.currentTimeMillis() - 120000,
            onLongPress = { /* Open teach dialog */ }
        )
    }
}

@Preview(name = "Message with Citations", showBackground = true)
@Composable
private fun MessageWithCitationsPreview() {
    MaterialTheme {
        MessageBubble(
            content = "Based on your documents, the system requirements are as follows: minimum 4GB RAM, 100MB storage, and compatible OS.",
            isUserMessage = false,
            timestamp = System.currentTimeMillis() - 60000,
            confidence = 0.85f,
            sourceCitations = listOf(
                SourceCitation(
                    documentTitle = "User Manual v2.1",
                    pageNumber = 5,
                    similarityPercent = 92
                ),
                SourceCitation(
                    documentTitle = "Technical Specifications",
                    pageNumber = null,
                    similarityPercent = 87
                ),
                SourceCitation(
                    documentTitle = "Installation Guide",
                    pageNumber = 2,
                    similarityPercent = 78
                )
            )
        )
    }
}

@Preview(name = "Message with Citations - Collapsed", showBackground = true)
@Composable
private fun MessageWithCollapsedCitationsPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            SourceCitationsSection(
                citations = listOf(
                    SourceCitation("User Manual v2.1", 5, 92),
                    SourceCitation("Technical Specs", null, 87)
                ),
                isExpanded = false,
                onExpandedChange = { }
            )
        }
    }
}

@Preview(name = "Message with Citations - Expanded", showBackground = true)
@Composable
private fun MessageWithExpandedCitationsPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            SourceCitationsSection(
                citations = listOf(
                    SourceCitation("User Manual v2.1", 5, 92),
                    SourceCitation("Technical Specifications", null, 87),
                    SourceCitation("Installation Guide", 2, 78),
                    SourceCitation("FAQ Document", 12, 65)
                ),
                isExpanded = true,
                onExpandedChange = { }
            )
        }
    }
}
