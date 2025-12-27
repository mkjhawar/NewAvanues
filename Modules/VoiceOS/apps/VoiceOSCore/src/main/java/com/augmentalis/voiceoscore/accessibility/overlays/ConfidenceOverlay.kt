/**
 * ConfidenceOverlay.kt - Real-time confidence indicator overlay
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import com.augmentalis.voiceos.speech.confidence.ConfidenceResult

/**
 * Overlay that displays real-time confidence during speech recognition
 */
class ConfidenceOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var isShowing = false

    // Mutable state for confidence updates
    private var confidenceState = mutableStateOf(0f)
    private var levelState = mutableStateOf(ConfidenceLevel.HIGH)
    private var textState = mutableStateOf("")

    /**
     * Show the confidence overlay with initial result
     */
    fun show(confidenceResult: ConfidenceResult) {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }

        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }

        updateConfidence(confidenceResult)
    }

    /**
     * Update confidence values without recreating overlay
     */
    fun updateConfidence(result: ConfidenceResult) {
        confidenceState.value = result.confidence
        levelState.value = result.level
        textState.value = result.text
    }

    /**
     * Hide the confidence overlay
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
                val confidence by remember { confidenceState }
                val level by remember { levelState }
                val text by remember { textState }

                ConfidenceIndicatorUI(
                    confidence = confidence,
                    level = level,
                    text = text
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
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 16
        }
    }
}

/**
 * Composable UI for confidence indicator
 */
@Composable
private fun ConfidenceIndicatorUI(
    confidence: Float,
    level: ConfidenceLevel,
    text: String
) {
    // Animated color based on confidence level
    val targetColor = when (level) {
        ConfidenceLevel.HIGH -> Color(0xFF4CAF50)      // Green
        ConfidenceLevel.MEDIUM -> Color(0xFFFFEB3B)    // Yellow
        ConfidenceLevel.LOW -> Color(0xFFFF9800)       // Orange
        ConfidenceLevel.REJECT -> Color(0xFFF44336)    // Red
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "confidenceColor"
    )

    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(durationMillis = 200),
        label = "confidenceValue"
    )

    Card(
        modifier = Modifier
            .padding(8.dp)
            .widthIn(min = 120.dp, max = 200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xEE000000) // Semi-transparent black
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Confidence indicator circle
            Canvas(
                modifier = Modifier
                    .size(28.dp)
            ) {
                drawCircle(
                    color = animatedColor,
                    radius = size.minDimension / 2
                )
            }

            // Confidence details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Percentage
                Text(
                    text = "${(animatedConfidence * 100).toInt()}%",
                    color = animatedColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Confidence level
                Text(
                    text = level.name,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )

                // Recognized text (if available)
                if (text.isNotEmpty()) {
                    Text(
                        text = text.take(15) + if (text.length > 15) "..." else "",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
