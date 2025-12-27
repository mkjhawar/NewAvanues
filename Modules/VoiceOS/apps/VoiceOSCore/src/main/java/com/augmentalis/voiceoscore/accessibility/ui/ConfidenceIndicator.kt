/**
 * ConfidenceIndicator.kt - Visual confidence feedback for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Assistant
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import com.augmentalis.voiceos.speech.confidence.ConfidenceScorer

/**
 * Simple confidence indicator showing color-coded dot and percentage
 *
 * @param confidence Normalized confidence (0.0 to 1.0)
 * @param modifier Modifier for layout
 */
@Composable
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val level = ConfidenceScorer().getConfidenceLevel(confidence)
    val color = getConfidenceColor(level)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(8.dp)
    ) {
        // Color-coded status dot
        Canvas(modifier = Modifier.size(16.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Percentage display
        Text(
            text = "${(confidence * 100).toInt()}%",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Detailed confidence indicator with progress bar
 *
 * @param confidence Normalized confidence (0.0 to 1.0)
 * @param text Recognized text
 * @param showPercentage Whether to show percentage value
 * @param modifier Modifier for layout
 */
@Composable
fun DetailedConfidenceIndicator(
    confidence: Float,
    text: String,
    showPercentage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val level = ConfidenceScorer().getConfidenceLevel(confidence)
    val color = getConfidenceColor(level)

    // Animate the confidence bar
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(durationMillis = 300),
        label = "confidence_animation"
    )

    Column(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Header with text and percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            if (showPercentage) {
                Text(
                    text = "${(confidence * 100).toInt()}%",
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Confidence progress bar
        ConfidenceProgressBar(
            confidence = animatedConfidence,
            color = color,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Confidence level text
        Text(
            text = getConfidenceLevelText(level),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Circular confidence indicator with animated ring
 *
 * @param confidence Normalized confidence (0.0 to 1.0)
 * @param size Size of the indicator
 * @param strokeWidth Width of the ring
 * @param showPercentage Whether to show percentage in center
 * @param modifier Modifier for layout
 */
@Composable
fun CircularConfidenceIndicator(
    confidence: Float,
    size: Dp = 60.dp,
    strokeWidth: Dp = 6.dp,
    showPercentage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val level = ConfidenceScorer().getConfidenceLevel(confidence)
    val color = getConfidenceColor(level)

    // Animate the confidence
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(durationMillis = 500),
        label = "circular_confidence_animation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                style = Stroke(width = strokeWidth.toPx())
            )
        }

        // Confidence arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 360f * animatedConfidence
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Percentage text in center
        if (showPercentage) {
            Text(
                text = "${(confidence * 100).toInt()}%",
                color = color,
                fontSize = (size.value / 4).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Confidence progress bar
 */
@Composable
private fun ConfidenceProgressBar(
    confidence: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .background(
                color = Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(confidence)
                .background(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

/**
 * Get color for confidence level
 */
private fun getConfidenceColor(level: ConfidenceLevel): Color {
    return when (level) {
        ConfidenceLevel.HIGH -> Color(0xFF4CAF50)      // Green
        ConfidenceLevel.MEDIUM -> Color(0xFFFFEB3B)    // Yellow
        ConfidenceLevel.LOW -> Color(0xFFFF9800)       // Orange
        ConfidenceLevel.REJECT -> Color(0xFFF44336)    // Red
    }
}

/**
 * Get text description for confidence level
 */
private fun getConfidenceLevelText(level: ConfidenceLevel): String {
    return when (level) {
        ConfidenceLevel.HIGH -> "High Confidence - Executing"
        ConfidenceLevel.MEDIUM -> "Medium Confidence - Confirm?"
        ConfidenceLevel.LOW -> "Low Confidence - Check Alternatives"
        ConfidenceLevel.REJECT -> "Low Confidence - Not Recognized"
    }
}

/**
 * Compact confidence badge for overlay use
 *
 * @param confidence Normalized confidence (0.0 to 1.0)
 * @param modifier Modifier for layout
 */
@Composable
fun ConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val level = ConfidenceScorer().getConfidenceLevel(confidence)
    val color = getConfidenceColor(level)

    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${(confidence * 100).toInt()}%",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Confidence level indicator with threshold markers
 * Shows where confidence falls relative to threshold levels
 *
 * @param confidence Normalized confidence (0.0 to 1.0)
 * @param modifier Modifier for layout
 */
@Composable
fun ConfidenceThresholdIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val level = ConfidenceScorer().getConfidenceLevel(confidence)
    val color = getConfidenceColor(level)

    Column(modifier = modifier.padding(8.dp)) {
        // Confidence value
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Confidence",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = "${(confidence * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar with threshold markers
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    )
            )

            // Confidence fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(confidence)
                    .height(12.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(6.dp)
                    )
            )

            // Threshold markers
            Box(
                modifier = Modifier
                    .fillMaxWidth(ConfidenceScorer.THRESHOLD_HIGH)
                    .height(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(Color.White)
                        .align(Alignment.CenterEnd)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(ConfidenceScorer.THRESHOLD_MEDIUM)
                    .height(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(Color.White)
                        .align(Alignment.CenterEnd)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(ConfidenceScorer.THRESHOLD_LOW)
                    .height(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(Color.White)
                        .align(Alignment.CenterEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Level text
        Text(
            text = level.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
