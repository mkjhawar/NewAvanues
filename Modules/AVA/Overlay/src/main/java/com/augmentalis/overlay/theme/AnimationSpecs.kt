// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/theme/AnimationSpecs.kt
// created: 2025-11-01 22:30:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 2 - Glassmorphic UI
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.theme

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntSize

/**
 * Animation specifications for AVA overlay system.
 *
 * Centralized motion design constants ensuring consistent timing
 * and easing across all overlay animations.
 *
 * @author Manoj Jhawar
 */
object OverlayAnimations {

    // Panel expand/collapse animations
    val panelExpand = tween<IntSize>(
        durationMillis = 220,
        easing = FastOutSlowInEasing
    )

    val panelCollapse = tween<IntSize>(
        durationMillis = 180,
        easing = LinearOutSlowInEasing
    )

    // Orb pulse animation (idle state)
    val orbPulse = infiniteRepeatable<Float>(
        animation = tween(2000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    // Fade in/out animations
    val fadeIn = tween<Float>(
        durationMillis = 150,
        easing = LinearEasing
    )

    val fadeOut = tween<Float>(
        durationMillis = 100,
        easing = LinearEasing
    )

    // Rotation animation (processing spinner)
    val spinnerRotation = infiniteRepeatable<Float>(
        animation = tween(1000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )

    // Glow pulse (speaking state)
    val glowPulse = infiniteRepeatable<Float>(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    // Waveform animation (listening state)
    val waveformPulse = infiniteRepeatable<Float>(
        animation = tween(600, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    // Suggestion chip appearance
    val chipSlideIn = tween<Float>(
        durationMillis = 200,
        delayMillis = 50,
        easing = FastOutSlowInEasing
    )

    // Scale animation for interactive feedback
    val pressScale = tween<Float>(
        durationMillis = 100,
        easing = LinearOutSlowInEasing
    )
}
