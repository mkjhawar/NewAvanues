/**
 * CommandStatusOverlay.kt - Voice command status overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Command processing state
 */
enum class CommandState {
    LISTENING,      // Listening for voice input
    PROCESSING,     // Processing/recognizing speech
    EXECUTING,      // Executing the command
    SUCCESS,        // Command executed successfully
    ERROR          // Error occurred
}

/**
 * Overlay that displays voice command status
 */
class CommandStatusOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var isShowing = false

    // Mutable state for command status
    private var commandState = mutableStateOf("")
    private var stateState = mutableStateOf(CommandState.LISTENING)
    private var messageState = mutableStateOf<String?>(null)

    /**
     * Show status with command and state
     */
    fun showStatus(
        command: String,
        state: CommandState,
        message: String? = null
    ) {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }

        commandState.value = command
        stateState.value = state
        messageState.value = message

        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
    }

    /**
     * Update status without recreating overlay
     */
    fun updateStatus(
        command: String? = null,
        state: CommandState? = null,
        message: String? = null
    ) {
        command?.let { commandState.value = it }
        state?.let { stateState.value = it }
        if (message != null) {
            messageState.value = message
        }
    }

    /**
     * Hide the overlay
     */
    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try {
                    windowManager.removeView(it)
                    isShowing = false
                } catch (e: IllegalArgumentException) {
                    // View not attached, ignore
                }
            }
        }
    }

    /**
     * Check if overlay is currently visible
     */
    fun isVisible(): Boolean = isShowing

    /**
     * Dispose and clean up resources
     */
    fun dispose() {
        hide()
        overlayView = null
    }

    /**
     * Create the compose view for the overlay
     */
    private fun createOverlayView(): ComposeView {
        return ComposeView(context).apply {
            setContent {
                val command by remember { commandState }
                val state by remember { stateState }
                val message by remember { messageState }

                CommandStatusUI(
                    command = command,
                    state = state,
                    message = message
                )
            }
        }
    }

    /**
     * Create window layout parameters for overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80
        }
    }
}

/**
 * Composable UI for command status
 */
@Composable
private fun CommandStatusUI(
    command: String,
    state: CommandState,
    message: String?
) {
    AnimatedVisibility(
        visible = command.isNotEmpty(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(min = 200.dp, max = 320.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xEE000000) // Semi-transparent black
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // State icon with animation
                StateIcon(state = state)

                // Command details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // State label
                    Text(
                        text = getStateLabel(state),
                        color = getStateColor(state),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Command text
                    Text(
                        text = command,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Optional message
                    message?.let {
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated state icon
 */
@Composable
private fun StateIcon(state: CommandState) {
    val (icon, color) = getStateIconAndColor(state)

    // Pulsing animation for LISTENING and PROCESSING
    val infiniteTransition = rememberInfiniteTransition(label = "stateIconAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == CommandState.LISTENING || state == CommandState.PROCESSING) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // Rotation animation for PROCESSING and EXECUTING
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == CommandState.PROCESSING || state == CommandState.EXECUTING) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = state.name,
            tint = color,
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
        )
    }
}

/**
 * Get icon and color for state
 */
private fun getStateIconAndColor(state: CommandState): Pair<ImageVector, Color> {
    return when (state) {
        CommandState.LISTENING -> Icons.Default.Mic to Color(0xFF2196F3)      // Blue
        CommandState.PROCESSING -> Icons.Default.HourglassEmpty to Color(0xFFFFC107) // Amber
        CommandState.EXECUTING -> Icons.Default.PlayArrow to Color(0xFF9C27B0)  // Purple
        CommandState.SUCCESS -> Icons.Default.Check to Color(0xFF4CAF50)     // Green
        CommandState.ERROR -> Icons.Default.Close to Color(0xFFF44336)       // Red
    }
}

/**
 * Get color for state
 */
private fun getStateColor(state: CommandState): Color {
    return when (state) {
        CommandState.LISTENING -> Color(0xFF2196F3)      // Blue
        CommandState.PROCESSING -> Color(0xFFFFC107)     // Amber
        CommandState.EXECUTING -> Color(0xFF9C27B0)      // Purple
        CommandState.SUCCESS -> Color(0xFF4CAF50)        // Green
        CommandState.ERROR -> Color(0xFFF44336)          // Red
    }
}

/**
 * Get label for state
 */
private fun getStateLabel(state: CommandState): String {
    return when (state) {
        CommandState.LISTENING -> "LISTENING"
        CommandState.PROCESSING -> "PROCESSING"
        CommandState.EXECUTING -> "EXECUTING"
        CommandState.SUCCESS -> "SUCCESS"
        CommandState.ERROR -> "ERROR"
    }
}
