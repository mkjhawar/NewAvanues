// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/ui/GradientUtils.kt
// created: 2025-11-06
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient utilities matching HTML demo design
 *
 * HTML demo gradient: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)
 */

// Primary gradient colors (matching HTML demo)
val GradientStartColor = Color(0xFF6366F1) // #6366f1 (Indigo)
val GradientEndColor = Color(0xFF8B5CF6)   // #8b5cf6 (Purple)

/**
 * Create the main gradient brush (135deg diagonal)
 */
@Composable
fun rememberGradientBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(GradientStartColor, GradientEndColor),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

/**
 * Modifier for gradient background
 */
fun Modifier.gradientBackground(): Modifier {
    return this.background(
        brush = Brush.linearGradient(
            colors = listOf(GradientStartColor, GradientEndColor),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
}

/**
 * Animated gradient for send button
 */
@Composable
fun rememberAnimatedGradientBrush(enabled: Boolean = true): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (enabled) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    return Brush.linearGradient(
        colors = listOf(
            GradientStartColor,
            GradientEndColor,
            GradientStartColor
        ),
        start = Offset(offset * 1000, offset * 1000),
        end = Offset((1 - offset) * 1000, (1 - offset) * 1000)
    )
}

/**
 * Message slide-in animation spec (matching HTML demo)
 */
val MessageSlideInSpec = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * Button scale animation spec
 */
val ButtonScaleSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/**
 * Fade in animation spec
 */
val FadeInSpec = tween<Float>(
    durationMillis = 300,
    easing = FastOutSlowInEasing
)
