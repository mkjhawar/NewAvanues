/**
 * EdgeVisualFeedback.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/EdgeVisualFeedback.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Visual feedback component for cursor edge detection
 * Module: VoiceCursor System
 */

package com.augmentalis.voiceos.cursor.view

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Visual feedback component that shows edge detection and bounce effects
 */
@Composable
fun EdgeVisualFeedback(
    isAtEdge: Boolean,
    edgeType: EdgeType,
    screenWidth: Int,
    screenHeight: Int,
    modifier: Modifier = Modifier
) {
    
    // Animation for edge glow effect
    val glowAnimation by animateFloatAsState(
        targetValue = if (isAtEdge) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "edge_glow"
    )
    
    // Pulsing animation when at edge
    val infiniteTransition = rememberInfiniteTransition(label = "edge_pulse")
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    if (glowAnimation > 0.01f) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
        ) {
            drawEdgeGlow(
                edgeType = edgeType,
                intensity = glowAnimation,
                pulse = if (isAtEdge) pulseAnimation else 0f,
                screenSize = Size(screenWidth.toFloat(), screenHeight.toFloat())
            )
        }
    }
}

/**
 * Draw edge glow effect based on edge type
 */
fun DrawScope.drawEdgeGlow(
    edgeType: EdgeType,
    intensity: Float,
    pulse: Float,
    screenSize: Size
) {
    val glowColor = Color(0xFF007AFF) // ARVision blue
    val baseOpacity = 0.3f * intensity
    val pulseOpacity = baseOpacity + (0.2f * pulse * intensity)
    
    val glowWidth = 8.dp.toPx()
    val extendedGlowWidth = glowWidth + (glowWidth * 0.5f * pulse)
    
    when (edgeType) {
        EdgeType.LEFT -> {
            drawVerticalEdgeGlow(
                x = 0f,
                height = screenSize.height,
                width = extendedGlowWidth,
                color = glowColor,
                opacity = pulseOpacity,
                isLeft = true
            )
        }
        EdgeType.RIGHT -> {
            drawVerticalEdgeGlow(
                x = screenSize.width - extendedGlowWidth,
                height = screenSize.height,
                width = extendedGlowWidth,
                color = glowColor,
                opacity = pulseOpacity,
                isLeft = false
            )
        }
        EdgeType.TOP -> {
            drawHorizontalEdgeGlow(
                y = 0f,
                width = screenSize.width,
                height = extendedGlowWidth,
                color = glowColor,
                opacity = pulseOpacity,
                isTop = true
            )
        }
        EdgeType.BOTTOM -> {
            drawHorizontalEdgeGlow(
                y = screenSize.height - extendedGlowWidth,
                width = screenSize.width,
                height = extendedGlowWidth,
                color = glowColor,
                opacity = pulseOpacity,
                isTop = false
            )
        }
        EdgeType.TOP_LEFT -> {
            drawCornerGlow(
                center = Offset(0f, 0f),
                radius = extendedGlowWidth * 1.5f,
                color = glowColor,
                opacity = pulseOpacity,
                startAngle = 0f,
                sweepAngle = 90f
            )
        }
        EdgeType.TOP_RIGHT -> {
            drawCornerGlow(
                center = Offset(screenSize.width, 0f),
                radius = extendedGlowWidth * 1.5f,
                color = glowColor,
                opacity = pulseOpacity,
                startAngle = 90f,
                sweepAngle = 90f
            )
        }
        EdgeType.BOTTOM_LEFT -> {
            drawCornerGlow(
                center = Offset(0f, screenSize.height),
                radius = extendedGlowWidth * 1.5f,
                color = glowColor,
                opacity = pulseOpacity,
                startAngle = 270f,
                sweepAngle = 90f
            )
        }
        EdgeType.BOTTOM_RIGHT -> {
            drawCornerGlow(
                center = Offset(screenSize.width, screenSize.height),
                radius = extendedGlowWidth * 1.5f,
                color = glowColor,
                opacity = pulseOpacity,
                startAngle = 180f,
                sweepAngle = 90f
            )
        }
        EdgeType.NONE -> {
            // No glow
        }
    }
}

/**
 * Draw vertical edge glow (left/right edges)
 */
fun DrawScope.drawVerticalEdgeGlow(
    x: Float,
    height: Float,
    width: Float,
    color: Color,
    opacity: Float,
    isLeft: Boolean
) {
    val gradient = if (isLeft) {
        Brush.horizontalGradient(
            colors = listOf(
                color.copy(alpha = opacity),
                color.copy(alpha = opacity * 0.5f),
                Color.Transparent
            ),
            startX = x,
            endX = x + width
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = opacity * 0.5f),
                color.copy(alpha = opacity)
            ),
            startX = x,
            endX = x + width
        )
    }
    
    drawRect(
        brush = gradient,
        topLeft = Offset(x, 0f),
        size = Size(width, height)
    )
}

/**
 * Draw horizontal edge glow (top/bottom edges)
 */
fun DrawScope.drawHorizontalEdgeGlow(
    y: Float,
    width: Float,
    height: Float,
    color: Color,
    opacity: Float,
    isTop: Boolean
) {
    val gradient = if (isTop) {
        Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = opacity),
                color.copy(alpha = opacity * 0.5f),
                Color.Transparent
            ),
            startY = y,
            endY = y + height
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = opacity * 0.5f),
                color.copy(alpha = opacity)
            ),
            startY = y,
            endY = y + height
        )
    }
    
    drawRect(
        brush = gradient,
        topLeft = Offset(0f, y),
        size = Size(width, height)
    )
}

/**
 * Draw corner glow for corner edges
 */
fun DrawScope.drawCornerGlow(
    center: Offset,
    radius: Float,
    color: Color,
    opacity: Float,
    startAngle: Float,
    sweepAngle: Float
) {
    val gradient = Brush.radialGradient(
        colors = listOf(
            color.copy(alpha = opacity),
            color.copy(alpha = opacity * 0.7f),
            color.copy(alpha = opacity * 0.3f),
            Color.Transparent
        ),
        center = center,
        radius = radius
    )
    
    // Draw arc for corner glow
    drawArc(
        brush = gradient,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
}

/**
 * Click target assistance visual feedback
 */
@Composable
fun ClickTargetAssistance(
    targetPosition: Offset?,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    targetPosition?.let { target ->
        
        // Animation for target highlight
        val highlightAnimation by animateFloatAsState(
            targetValue = confidence,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            label = "target_highlight"
        )
        
        // Pulsing animation
        val infiniteTransition = rememberInfiniteTransition(label = "target_pulse")
        val pulseAnimation by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        if (highlightAnimation > 0.01f) {
            Canvas(
                modifier = modifier.fillMaxSize()
            ) {
                drawTargetHighlight(
                    center = target,
                    intensity = highlightAnimation,
                    scale = pulseAnimation
                )
            }
        }
    }
}

/**
 * Draw target highlight circle
 */
fun DrawScope.drawTargetHighlight(
    center: Offset,
    intensity: Float,
    scale: Float
) {
    val baseRadius = 24.dp.toPx()
    val radius = baseRadius * scale
    val color = Color(0xFF00C851) // Success green
    
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.1f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.5f
        ),
        radius = radius * 1.5f,
        center = center
    )
    
    // Main ring
    drawCircle(
        color = color.copy(alpha = 0.6f * intensity),
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Inner ring
    drawCircle(
        color = color.copy(alpha = 0.3f * intensity),
        radius = radius * 0.7f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    
    // Center dot
    drawCircle(
        color = color.copy(alpha = 0.8f * intensity),
        radius = 3.dp.toPx(),
        center = center
    )
}