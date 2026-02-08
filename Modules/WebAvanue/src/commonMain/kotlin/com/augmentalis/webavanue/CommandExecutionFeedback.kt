package com.augmentalis.webavanue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.ShapeTokens
import kotlinx.coroutines.delay

/**
 * Command execution result type
 */
sealed class CommandExecutionResult {
    data class Success(
        val command: String,
        val action: String,
        val target: String? = null
    ) : CommandExecutionResult()

    data class Failure(
        val command: String,
        val reason: String
    ) : CommandExecutionResult()

    data class Disambiguate(
        val command: String,
        val options: List<DisambiguationOption>
    ) : CommandExecutionResult()

    data class NotFound(
        val command: String,
        val suggestions: List<String> = emptyList()
    ) : CommandExecutionResult()
}

/**
 * Disambiguation option when multiple matches exist
 */
data class DisambiguationOption(
    val index: Int,
    val text: String,
    val elementType: String,
    val preview: String
)

/**
 * Main command execution feedback overlay
 *
 * Shows visual feedback for voice command execution including:
 * - Success with animation
 * - Failure with reason
 * - Disambiguation when multiple matches
 * - Not found with suggestions
 */
@Composable
fun CommandExecutionFeedback(
    result: CommandExecutionResult?,
    onDismiss: () -> Unit,
    onSelectOption: ((Int) -> Unit)? = null,
    onRetrySuggestion: ((String) -> Unit)? = null,
    autoDismissMs: Long = 2500L,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss for success/failure
    LaunchedEffect(result) {
        if (result is CommandExecutionResult.Success || result is CommandExecutionResult.Failure) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = result != null,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        when (result) {
            is CommandExecutionResult.Success -> SuccessFeedback(
                command = result.command,
                action = result.action,
                target = result.target
            )
            is CommandExecutionResult.Failure -> FailureFeedback(
                command = result.command,
                reason = result.reason,
                onDismiss = onDismiss
            )
            is CommandExecutionResult.Disambiguate -> DisambiguationFeedback(
                command = result.command,
                options = result.options,
                onSelect = onSelectOption ?: {},
                onDismiss = onDismiss
            )
            is CommandExecutionResult.NotFound -> NotFoundFeedback(
                command = result.command,
                suggestions = result.suggestions,
                onRetry = onRetrySuggestion,
                onDismiss = onDismiss
            )
            null -> {}
        }
    }
}

/**
 * Success feedback with animated checkmark
 */
@Composable
private fun SuccessFeedback(
    command: String,
    action: String,
    target: String?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successScale"
    )

    Surface(
        modifier = Modifier.widthIn(max = 280.dp),
        shape = RoundedCornerShape(16.dp),
        color = AvanueTheme.colors.success.copy(alpha = 0.15f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated success icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(AvanueTheme.colors.success.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Success",
                    tint = AvanueTheme.colors.success,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Command text
            Text(
                text = "\"$command\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AvanueTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action description
            Text(
                text = if (target != null) "$action: $target" else action,
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Failure feedback with error details
 */
@Composable
private fun FailureFeedback(
    command: String,
    reason: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(max = 300.dp),
        shape = RoundedCornerShape(16.dp),
        color = AvanueTheme.colors.error.copy(alpha = 0.15f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AvanueTheme.colors.error.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Failed",
                    tint = AvanueTheme.colors.error,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "\"$command\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AvanueTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * Disambiguation feedback when multiple matches exist
 */
@Composable
private fun DisambiguationFeedback(
    command: String,
    options: List<DisambiguationOption>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(max = 340.dp),
        shape = RoundedCornerShape(16.dp),
        color = AvanueTheme.colors.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = AvanueTheme.colors.warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Which one?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AvanueTheme.colors.textPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AvanueTheme.colors.iconSecondary
                    )
                }
            }

            Text(
                text = "\"$command\" matches multiple elements:",
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            options.forEachIndexed { index, option ->
                Surface(
                    onClick = { onSelect(option.index) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ShapeTokens.md),
                    color = AvanueTheme.colors.surfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Index badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AvanueTheme.colors.iconPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${option.index}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AvanueTheme.colors.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${option.elementType} - ${option.preview}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary,
                                maxLines = 1
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AvanueTheme.colors.iconSecondary
                        )
                    }
                }

                if (index < options.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Say the number or tap to select",
                style = MaterialTheme.typography.labelSmall,
                color = AvanueTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Not found feedback with suggestions
 */
@Composable
private fun NotFoundFeedback(
    command: String,
    suggestions: List<String>,
    onRetry: ((String) -> Unit)?,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(max = 320.dp),
        shape = RoundedCornerShape(16.dp),
        color = AvanueTheme.colors.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Question icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AvanueTheme.colors.warning.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QuestionMark,
                    contentDescription = "Not found",
                    tint = AvanueTheme.colors.warning,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "\"$command\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AvanueTheme.colors.textPrimary
            )

            Text(
                text = "No matching command found",
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.textSecondary
            )

            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Did you mean:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AvanueTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                suggestions.take(3).forEach { suggestion ->
                    Surface(
                        onClick = { onRetry?.invoke(suggestion) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = RoundedCornerShape(ShapeTokens.sm),
                        color = AvanueTheme.colors.iconPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "\"$suggestion\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.iconPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { /* Show all commands */ }) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Help")
                }
            }
        }
    }
}

/**
 * Mini toast-style feedback for quick confirmations
 */
@Composable
fun CommandToast(
    message: String,
    icon: ImageVector = Icons.Default.Check,
    iconTint: Color = AvanueTheme.colors.success,
    visible: Boolean,
    onDismiss: () -> Unit,
    autoDismissMs: Long = 1500L,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AvanueTheme.colors.surfaceElevated,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textPrimary
                )
            }
        }
    }
}

/**
 * Listening indicator overlay
 *
 * Shows animated feedback while actively listening for voice input.
 */
@Composable
fun ListeningIndicator(
    isListening: Boolean,
    partialResult: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isListening,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "listening")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "listenScale"
        )

        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(20.dp),
            color = AvanueTheme.colors.iconPrimary.copy(alpha = 0.15f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing microphone
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(AvanueTheme.colors.iconPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = AvanueTheme.colors.iconPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = partialResult ?: "Listening...",
                    style = MaterialTheme.typography.titleMedium,
                    color = AvanueTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Speak your command",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}
