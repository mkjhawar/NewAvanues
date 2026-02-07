/**
 * PulseDot.kt - Animated service state indicator for the Avanues dashboard
 *
 * Renders a dot with concentric pulse rings when the service is active.
 * Uses OceanDesignTokens for all colors and animations.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avamagic.ui.foundation

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.foundation.state.ServiceState

/**
 * PulseDot - Animated dot indicator that reflects service lifecycle state.
 *
 * - Running: Green dot with expanding concentric pulse rings (breathing effect)
 * - Ready: Blue dot with subtle steady glow (no pulse)
 * - Stopped: Gray dot, no animation
 * - Error: Red dot with fast pulse
 * - Degraded: Amber dot with slow pulse
 *
 * @param state Current service state from ServiceStateProvider
 * @param modifier Modifier for sizing and positioning
 * @param dotSize Size of the center dot. Total canvas = dotSize * 3 for pulse room.
 * @param pulseDurationMs Duration of one pulse cycle in milliseconds
 */
@Composable
fun PulseDot(
    state: ServiceState,
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    pulseDurationMs: Int = 2000
) {
    val dotColor = when (state) {
        is ServiceState.Running -> OceanDesignTokens.State.success     // #10B981
        is ServiceState.Ready -> OceanDesignTokens.State.info          // #3B82F6
        is ServiceState.Stopped -> OceanDesignTokens.Text.disabled     // #64748B
        is ServiceState.Error -> OceanDesignTokens.State.error         // #EF4444
        is ServiceState.Degraded -> OceanDesignTokens.State.warning    // #F59E0B
    }

    val shouldPulse = state is ServiceState.Running ||
            state is ServiceState.Error ||
            state is ServiceState.Degraded

    val effectiveDuration = when (state) {
        is ServiceState.Error -> pulseDurationMs / 2       // Fast pulse for errors
        is ServiceState.Degraded -> (pulseDurationMs * 1.5).toInt()  // Slow pulse
        else -> pulseDurationMs                             // Normal 2s cycle
    }

    val canvasSize = dotSize * 3

    if (shouldPulse) {
        PulseDotAnimated(
            dotColor = dotColor,
            dotSize = dotSize,
            canvasSize = canvasSize,
            durationMs = effectiveDuration,
            modifier = modifier
        )
    } else if (state is ServiceState.Ready) {
        GlowDot(
            dotColor = dotColor,
            dotSize = dotSize,
            canvasSize = canvasSize,
            modifier = modifier
        )
    } else {
        StaticDot(
            dotColor = dotColor,
            dotSize = dotSize,
            canvasSize = canvasSize,
            modifier = modifier
        )
    }
}

@Composable
private fun PulseDotAnimated(
    dotColor: Color,
    dotSize: Dp,
    canvasSize: Dp,
    durationMs: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Ring 1 — starts immediately
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    // Ring 2 — offset by ~35% of cycle
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, delayMillis = (durationMs * 0.35).toInt(), easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, delayMillis = (durationMs * 0.35).toInt(), easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    // Ring 3 — offset by ~70% of cycle
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, delayMillis = (durationMs * 0.7).toInt(), easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3Scale"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, delayMillis = (durationMs * 0.7).toInt(), easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3Alpha"
    )

    Canvas(modifier = modifier.size(canvasSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (dotSize / 2).toPx()

        // Expanding rings (drawn behind the dot)
        drawCircle(
            color = dotColor.copy(alpha = ring3Alpha),
            radius = baseRadius * ring3Scale,
            center = center
        )
        drawCircle(
            color = dotColor.copy(alpha = ring2Alpha),
            radius = baseRadius * ring2Scale,
            center = center
        )
        drawCircle(
            color = dotColor.copy(alpha = ring1Alpha),
            radius = baseRadius * ring1Scale,
            center = center
        )

        // Solid center dot (always on top)
        drawCircle(
            color = dotColor,
            radius = baseRadius,
            center = center
        )
    }
}

@Composable
private fun GlowDot(
    dotColor: Color,
    dotSize: Dp,
    canvasSize: Dp,
    modifier: Modifier = Modifier
) {
    // Subtle breathing glow for Ready state (no expanding rings)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Canvas(modifier = modifier.size(canvasSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (dotSize / 2).toPx()

        // Soft glow halo
        drawCircle(
            color = dotColor.copy(alpha = glowAlpha),
            radius = baseRadius * 2f,
            center = center
        )

        // Solid center dot
        drawCircle(
            color = dotColor,
            radius = baseRadius,
            center = center
        )
    }
}

@Composable
private fun StaticDot(
    dotColor: Color,
    dotSize: Dp,
    canvasSize: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(canvasSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (dotSize / 2).toPx()

        drawCircle(
            color = dotColor,
            radius = baseRadius,
            center = center
        )
    }
}

/**
 * StatusBadge - Pill-shaped status label using GlassChip styling.
 *
 * @param state ServiceState to display
 * @param modifier Modifier
 */
@Composable
fun StatusBadge(
    state: ServiceState,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (state) {
        is ServiceState.Running -> "ACTIVE" to OceanDesignTokens.State.success
        is ServiceState.Ready -> "READY" to OceanDesignTokens.State.info
        is ServiceState.Stopped -> "OFF" to OceanDesignTokens.Text.disabled
        is ServiceState.Error -> "ERROR" to OceanDesignTokens.State.error
        is ServiceState.Degraded -> "DEGRADED" to OceanDesignTokens.State.warning
    }

    GlassChip(
        onClick = {},
        label = {
            androidx.compose.material3.Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = color
            )
        },
        modifier = modifier,
        enabled = false,
        glass = true,
        glassLevel = GlassLevel.LIGHT
    )
}
