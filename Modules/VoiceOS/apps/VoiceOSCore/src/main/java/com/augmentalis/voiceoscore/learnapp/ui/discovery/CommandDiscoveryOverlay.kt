/**
 * CommandDiscoveryOverlay.kt - Real-time visual overlay for voice command discovery
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-08
 *
 * Visual overlay showing voice commands on actual UI elements for user discovery.
 * Toggle with "Show voice commands" / "Hide voice commands".
 */

package com.augmentalis.voiceoscore.learnapp.ui.discovery

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceoscore.accessibility.ui.overlays.BaseOverlay
import com.augmentalis.voiceoscore.accessibility.ui.overlays.OverlayType
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Element with voice command metadata for overlay display
 */
data class ElementWithCommand(
    val bounds: Rect,
    val voiceCommand: String,
    val confidence: Float,
    val elementType: String,  // "button", "tab", "input", etc.
    val isGenerated: Boolean = false,  // True if label is auto-generated
    val generationStrategy: String? = null,  // "position", "spatial", "context"
    val description: String? = null  // Human-readable description
) {
    /**
     * Get color based on confidence level
     */
    fun getConfidenceColor(): Color = when {
        confidence >= 0.85f -> Color(0xFF4CAF50)  // Green - high confidence
        confidence >= 0.60f -> Color(0xFFFFC107)  // Yellow - medium confidence
        else -> Color(0xFFFF5722)  // Red - low confidence
    }

    /**
     * Get border width based on confidence
     */
    fun getBorderWidth(): Float = when {
        confidence >= 0.85f -> 3f
        confidence >= 0.60f -> 2f
        else -> 1.5f
    }
}

/**
 * Command Discovery Overlay
 *
 * Shows voice commands overlaid on actual UI elements with:
 * - Color-coded confidence (green=high, yellow=medium, red=low)
 * - Semi-transparent labels floating above elements
 * - Auto-hide after timeout
 * - Voice commands: "Show voice commands" / "Hide voice commands"
 *
 * ## Usage:
 * ```kotlin
 * val overlay = CommandDiscoveryOverlay(context)
 *
 * // Show commands for current screen
 * overlay.showCommands(listOf(
 *     ElementWithCommand(
 *         bounds = Rect(100, 200, 300, 250),
 *         voiceCommand = "Tab 1",
 *         confidence = 0.85f,
 *         elementType = "tab"
 *     )
 * ))
 *
 * // Auto-hide after 10 seconds
 * overlay.showWithTimeout(10_000)
 * ```
 */
class CommandDiscoveryOverlay(
    context: Context
) : BaseOverlay(context, OverlayType.FULLSCREEN) {

    companion object {
        private const val TAG = "CommandDiscoveryOverlay"
        private const val DEFAULT_TIMEOUT_MS = 10_000L
        private const val LABEL_PADDING_DP = 8
        private const val LABEL_OFFSET_DP = 4
        private const val MIN_LABEL_WIDTH_DP = 80
        private const val ANIMATION_DURATION_MS = 300
    }

    // Mutable state for command elements
    private val commandElements = mutableStateListOf<ElementWithCommand>()
    private var screenWidth by mutableStateOf(0)
    private var screenHeight by mutableStateOf(0)
    private var autoHideJob: kotlinx.coroutines.Job? = null

    /**
     * Show commands on overlay
     *
     * @param elements List of elements with voice commands to display
     */
    fun showCommands(elements: List<ElementWithCommand>) {
        overlayScope.launch {
            Log.d(TAG, "Showing ${elements.size} voice commands")

            // Update state
            commandElements.clear()
            commandElements.addAll(elements)

            // Show overlay
            if (!isVisible()) {
                show()
            }
        }
    }

    /**
     * Show commands with auto-hide timeout
     *
     * @param timeoutMs Timeout in milliseconds (default: 10 seconds)
     */
    fun showWithTimeout(timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        // Cancel existing timeout
        autoHideJob?.cancel()

        // Schedule auto-hide
        autoHideJob = overlayScope.launch {
            delay(timeoutMs)
            Log.d(TAG, "Auto-hiding after ${timeoutMs}ms")
            hide()
        }
    }

    /**
     * Clear all commands and hide overlay
     */
    fun clearCommands() {
        overlayScope.launch {
            commandElements.clear()
            autoHideJob?.cancel()
            hide()
        }
    }

    /**
     * Get command count
     */
    fun getCommandCount(): Int = commandElements.size

    @Composable
    override fun OverlayContent() {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current

        // Update screen dimensions
        LaunchedEffect(configuration) {
            screenWidth = (configuration.screenWidthDp * density.density).toInt()
            screenHeight = (configuration.screenHeightDp * density.density).toInt()
        }

        // Animated fade-in
        AnimatedVisibility(
            visible = commandElements.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(ANIMATION_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(ANIMATION_DURATION_MS))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Draw command labels
                commandElements.forEach { element ->
                    CommandLabelOverlayItem(
                        element = element,
                        density = density
                    )
                }

                // Show summary at bottom
                CommandSummaryBar(
                    totalCommands = commandElements.size,
                    highConfidence = commandElements.count { it.confidence >= 0.85f },
                    mediumConfidence = commandElements.count { it.confidence in 0.60f..0.84f },
                    lowConfidence = commandElements.count { it.confidence < 0.60f }
                )
            }
        }
    }

    @Composable
    private fun CommandLabelOverlayItem(
        element: ElementWithCommand,
        density: androidx.compose.ui.unit.Density
    ) {
        val bounds = element.bounds
        val color = element.getConfidenceColor()
        val borderWidth = element.getBorderWidth()

        // Position label above element
        val labelOffsetY = bounds.top - with(density) { (40.dp).toPx() }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = bounds.left,
                        y = labelOffsetY.toInt().coerceAtLeast(0)
                    )
                }
                .widthIn(min = MIN_LABEL_WIDTH_DP.dp)
        ) {
            // Highlight box around element
            Canvas(
                modifier = Modifier
                    .width(with(density) { bounds.width().toDp() })
                    .height(with(density) { bounds.height().toDp() })
            ) {
                drawRoundRect(
                    color = color.copy(alpha = 0.3f),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = borderWidth * density.density)
                )
            }

            // Voice command label
            Surface(
                modifier = Modifier
                    .padding(LABEL_PADDING_DP.dp)
                    .widthIn(min = MIN_LABEL_WIDTH_DP.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.85f),
                shadowElevation = 4.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Voice command text
                    Text(
                        text = "\"${element.voiceCommand}\"",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Confidence indicator
                    if (element.isGenerated) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Confidence dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(element.confidence * 100).toInt()}%",
                                color = color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Description (for generated labels)
                    element.description?.let { desc ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BoxScope.CommandSummaryBar(
        totalCommands: Int,
        highConfidence: Int,
        mediumConfidence: Int,
        lowConfidence: Int
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.9f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Voice Commands Available: $totalCommands",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ConfidenceBadge(
                        count = highConfidence,
                        label = "High",
                        color = Color(0xFF4CAF50)
                    )
                    ConfidenceBadge(
                        count = mediumConfidence,
                        label = "Medium",
                        color = Color(0xFFFFC107)
                    )
                    ConfidenceBadge(
                        count = lowConfidence,
                        label = "Low",
                        color = Color(0xFFFF5722)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Say \"Hide voice commands\" to dismiss",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun ConfidenceBadge(
        count: Int,
        label: String,
        color: Color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = RoundedCornerShape(6.dp))
            )
            Text(
                text = "$count $label",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }

    override fun dispose() {
        autoHideJob?.cancel()
        overlayScope.cancel()
        commandElements.clear()
        super.dispose()
    }
}
