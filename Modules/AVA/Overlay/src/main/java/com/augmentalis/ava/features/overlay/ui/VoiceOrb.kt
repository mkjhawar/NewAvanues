// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/VoiceOrb.kt
// created: 2025-11-01 22:30:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 2 - Glassmorphic UI
// agent: Engineer | mode: ACT

package com.augmentalis.ava.features.overlay.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.features.overlay.controller.OrbState
import com.augmentalis.ava.features.overlay.theme.OceanGlassColors
import com.augmentalis.ava.features.overlay.theme.OverlayAnimations
import com.augmentalis.ava.features.overlay.theme.orbSolidEffect
import kotlin.math.roundToInt

/**
 * Voice orb - draggable microphone bubble for voice activation.
 *
 * Displays a 64dp circular button that can be dragged anywhere on screen.
 * Visual state changes based on voice recognition status with smooth animations.
 *
 * @param position Current screen position (x, y coordinates)
 * @param state Visual state (Idle, Listening, Processing, Speaking)
 * @param onTap Callback when orb is tapped
 * @param onDrag Callback with position delta when dragged
 * @param modifier Optional modifier
 * @author Manoj Jhawar
 */
@Composable
fun VoiceOrb(
    position: Offset,
    state: OrbState,
    onTap: () -> Unit,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .size(64.dp)
            .orbSolidEffect()  // Ocean Glass v2.3 - solid colors for stability
            .clickable { onTap() }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            OrbState.Idle -> IdleMicIcon()
            OrbState.Listening -> ListeningWaveform()
            OrbState.Processing -> ProcessingSpinner()
            OrbState.Speaking -> SpeakingGlow()
        }
    }
}

/**
 * Idle state: Gentle pulsing microphone icon
 */
@Composable
private fun IdleMicIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = OverlayAnimations.orbPulse,
        label = "pulse_scale"
    )

    Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Voice input",
        tint = OceanGlassColors.TextPrimary,
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
    )
}

/**
 * Listening state: Animated waveform (3 bars)
 */
@Composable
private fun ListeningWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600 + (index * 100), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp * scale)
                    .clip(CircleShape)
                    .background(OceanGlassColors.CoralBlue)
            )
        }
    }
}

/**
 * Processing state: Rotating spinner
 */
@Composable
private fun ProcessingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = OverlayAnimations.spinnerRotation,
        label = "spinner_rotation"
    )

    Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Processing",
        tint = OceanGlassColors.CoralBlue,
        modifier = Modifier
            .size(28.dp)
            .rotate(rotation)
    )
}

/**
 * Speaking state: Pulsing glow effect
 */
@Composable
private fun SpeakingGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = OverlayAnimations.glowPulse,
        label = "glow_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OceanGlassColors.CoralBlue.copy(alpha * 0.3f))
        )

        // Icon
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Speaking",
            tint = OceanGlassColors.TextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}
