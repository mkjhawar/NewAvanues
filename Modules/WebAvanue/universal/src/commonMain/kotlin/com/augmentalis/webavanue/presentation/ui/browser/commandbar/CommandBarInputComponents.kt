package com.augmentalis.webavanue.ui.screen.browser.commandbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ui.screen.theme.OceanTheme

/**
 * TextCommandInput - Text command input field
 */
@Composable
fun TextCommandInput(
    visible: Boolean,
    onCommand: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf("") }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Surface(
            color = Color(0xFF0F3460).copy(alpha = 0.95f),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Type: back, forward, go to...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C6C6C)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF60A5FA),
                    unfocusedBorderColor = Color(0xFF2D4A6F),
                    cursorColor = Color(0xFF60A5FA)
                )
            )
        }
    }
}

/**
 * ListeningIndicator - Overlay shown when voice input is active
 */
@Composable
fun ListeningIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFFA78BFA).copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            WaveBar(delayMillis = index * 100)
                        }
                    }
                    Text(
                        text = "Listening...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * WaveBar - Animated wave bar for listening indicator
 */
@Composable
fun WaveBar(
    delayMillis: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_height"
    )

    Box(
        modifier = modifier
            .width(4.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White)
    )
}

/**
 * VoiceCommandsPanel - Help panel showing available voice commands
 * AR/XR optimized: larger touch targets, better spacing, muted radius (12dp)
 */
@Composable
fun VoiceCommandsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = listOf(
        "go back" to "Navigate back",
        "go forward" to "Navigate forward",
        "go home" to "Go to home page",
        "refresh" to "Reload page",
        "scroll up" to "Scroll page up",
        "scroll down" to "Scroll page down",
        "new tab" to "Open new tab",
        "close tab" to "Close current tab",
        "show tabs" to "Show 3D tabs view",
        "show favorites" to "Show favorites shelf",
        "go to [url]" to "Navigate to URL"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            color = OceanTheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 12.dp,
            modifier = modifier
                .width(320.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Commands",
                        style = MaterialTheme.typography.titleMedium,
                        color = OceanTheme.textPrimary
                    )
                    // AR/XR: Larger close button (48dp minimum touch target)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            color = OceanTheme.surfaceElevated,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = OceanTheme.textSecondary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = OceanTheme.border.copy(alpha = 0.5f))

                commands.forEach { (command, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OceanTheme.surfaceElevated, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = OceanTheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = command,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OceanTheme.textSecondary
                        )
                    }
                }
            }
        }
    }
}
