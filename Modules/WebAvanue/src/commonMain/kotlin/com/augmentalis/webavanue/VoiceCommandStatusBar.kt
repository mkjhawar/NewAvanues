package com.augmentalis.webavanue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Voice command status for the status bar
 */
sealed class VoiceCommandStatus {
    object Idle : VoiceCommandStatus()
    object Scanning : VoiceCommandStatus()
    data class Ready(val commandCount: Int, val isWhitelisted: Boolean) : VoiceCommandStatus()
    data class Listening(val partialResult: String? = null) : VoiceCommandStatus()
    data class Processing(val command: String) : VoiceCommandStatus()
    data class Executed(val command: String, val success: Boolean) : VoiceCommandStatus()
    data class Error(val message: String) : VoiceCommandStatus()
}

/**
 * Compact voice command status bar
 *
 * Shows current status: scanning, ready (with command count), listening, etc.
 * Designed to sit at the top or bottom of the browser with minimal footprint.
 */
@Composable
fun VoiceCommandStatusBar(
    status: VoiceCommandStatus,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        is VoiceCommandStatus.Idle -> AvanueTheme.colors.surface
        is VoiceCommandStatus.Scanning -> AvanueTheme.colors.surfaceElevated
        is VoiceCommandStatus.Ready -> AvanueTheme.colors.surfaceElevated
        is VoiceCommandStatus.Listening -> AvanueTheme.colors.primary.copy(alpha = 0.15f)
        is VoiceCommandStatus.Processing -> AvanueTheme.colors.warning.copy(alpha = 0.15f)
        is VoiceCommandStatus.Executed -> if (status.success)
            AvanueTheme.colors.success.copy(alpha = 0.15f)
        else
            AvanueTheme.colors.error.copy(alpha = 0.15f)
        is VoiceCommandStatus.Error -> AvanueTheme.colors.error.copy(alpha = 0.15f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(0.dp),
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status indicator
            StatusIndicator(status)

            // Status text
            StatusText(status, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))

            // Action hint
            ActionHint(status)
        }
    }
}

/**
 * Animated status indicator icon
 */
@Composable
private fun StatusIndicator(status: VoiceCommandStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusIndicator")

    when (status) {
        is VoiceCommandStatus.Idle -> {
            Icon(
                Icons.Default.MicOff,
                contentDescription = "Idle",
                tint = AvanueTheme.colors.iconDisabled,
                modifier = Modifier.size(20.dp)
            )
        }

        is VoiceCommandStatus.Scanning -> {
            // Pulsing animation for scanning
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scanPulse"
            )

            Icon(
                Icons.Default.Search,
                contentDescription = "Scanning",
                tint = AvanueTheme.colors.iconPrimary,
                modifier = Modifier.size(20.dp).scale(scale)
            )
        }

        is VoiceCommandStatus.Ready -> {
            // Badge with command count
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Ready",
                    tint = AvanueTheme.colors.success,
                    modifier = Modifier.size(20.dp)
                )
                // Command count badge
                if (status.commandCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-4).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AvanueTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (status.commandCount > 99) "99+" else status.commandCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        is VoiceCommandStatus.Listening -> {
            // Animated microphone for listening
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "listenPulse"
            )

            Icon(
                Icons.Default.Mic,
                contentDescription = "Listening",
                tint = AvanueTheme.colors.iconPrimary,
                modifier = Modifier.size(20.dp).scale(scale)
            )
        }

        is VoiceCommandStatus.Processing -> {
            // Spinning indicator for processing
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AvanueTheme.colors.warning
            )
        }

        is VoiceCommandStatus.Executed -> {
            Icon(
                if (status.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (status.success) "Success" else "Failed",
                tint = if (status.success) AvanueTheme.colors.success else AvanueTheme.colors.error,
                modifier = Modifier.size(20.dp)
            )
        }

        is VoiceCommandStatus.Error -> {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = AvanueTheme.colors.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Status text description
 */
@Composable
private fun StatusText(
    status: VoiceCommandStatus,
    modifier: Modifier = Modifier
) {
    val text = when (status) {
        is VoiceCommandStatus.Idle -> "Voice commands off"
        is VoiceCommandStatus.Scanning -> "Scanning page for commands..."
        is VoiceCommandStatus.Ready -> {
            val savedText = if (status.isWhitelisted) " (saved)" else ""
            "${status.commandCount} voice commands ready$savedText"
        }
        is VoiceCommandStatus.Listening -> status.partialResult ?: "Listening..."
        is VoiceCommandStatus.Processing -> "\"${status.command}\""
        is VoiceCommandStatus.Executed -> if (status.success)
            "\"${status.command}\" executed"
        else
            "\"${status.command}\" failed"
        is VoiceCommandStatus.Error -> status.message
    }

    val color = when (status) {
        is VoiceCommandStatus.Error,
        is VoiceCommandStatus.Executed -> if (status is VoiceCommandStatus.Executed && status.success)
            AvanueTheme.colors.textPrimary
        else
            AvanueTheme.colors.error
        else -> AvanueTheme.colors.textPrimary
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * Action hint on the right side
 */
@Composable
private fun ActionHint(status: VoiceCommandStatus) {
    val hint = when (status) {
        is VoiceCommandStatus.Idle -> "Tap to enable"
        is VoiceCommandStatus.Scanning -> null
        is VoiceCommandStatus.Ready -> "Say a command"
        is VoiceCommandStatus.Listening -> "Speak now"
        is VoiceCommandStatus.Processing -> null
        is VoiceCommandStatus.Executed -> "Tap for more"
        is VoiceCommandStatus.Error -> "Tap to retry"
    }

    if (hint != null) {
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = AvanueTheme.colors.textSecondary
        )
    }
}

/**
 * Expanded voice command status panel
 *
 * Shows detailed information when the status bar is tapped.
 * Includes recent commands, available commands preview, and settings.
 */
@Composable
fun VoiceCommandStatusPanel(
    status: VoiceCommandStatus,
    recentCommands: List<String>,
    availableCommands: List<String>,
    isWhitelisted: Boolean,
    onDismiss: () -> Unit,
    onViewAllCommands: () -> Unit,
    onSaveWebApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = AvanueTheme.colors.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voice Commands",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AvanueTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AvanueTheme.colors.iconSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AvanueTheme.colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(status)
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusText(status)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent commands
            if (recentCommands.isNotEmpty()) {
                Text(
                    text = "Recent Commands",
                    style = MaterialTheme.typography.titleSmall,
                    color = AvanueTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                recentCommands.take(3).forEach { command ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = AvanueTheme.colors.iconSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\"$command\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Available commands preview
            if (availableCommands.isNotEmpty()) {
                Text(
                    text = "Try saying...",
                    style = MaterialTheme.typography.titleSmall,
                    color = AvanueTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                availableCommands.take(5).forEach { command ->
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = AvanueTheme.colors.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "\"$command\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onViewAllCommands,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("View All Commands")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Save web app prompt
            if (!isWhitelisted && status is VoiceCommandStatus.Ready && status.commandCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = AvanueTheme.colors.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save this site for faster voice commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onSaveWebApp) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Floating command count badge
 *
 * Small floating indicator that shows command count.
 * Can be placed in a corner of the browser.
 */
@Composable
fun VoiceCommandBadge(
    commandCount: Int,
    isScanning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")

    val alpha by if (isScanning) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = AvanueTheme.colors.surfaceElevated.copy(alpha = 0.9f * alpha),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isScanning) Icons.Default.Search else Icons.Default.Mic,
                contentDescription = null,
                tint = if (isScanning)
                    AvanueTheme.colors.warning
                else
                    AvanueTheme.colors.success,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isScanning) "..." else commandCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AvanueTheme.colors.textPrimary
            )
        }
    }
}
