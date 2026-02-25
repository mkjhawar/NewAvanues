/**
 * ThemeOverrides.kt - Per-property overrides layered on the base AvanueUI v5.1 theme
 *
 * Null values = use base theme defaults. Non-null values = override.
 * Used by ThemePresets to fine-tune specific visual properties without
 * changing the core 3-axis system (palette x materialMode x appearance).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Optional per-property overrides layered on top of the base AvanueUI v5.1 theme.
 * Null values = use base theme defaults. Non-null values = override.
 *
 * Used by ThemePresets to fine-tune specific visual properties without
 * changing the core 3-axis system (palette x materialMode x appearance).
 */
@Immutable
data class ThemeOverrides(
    // Shape
    val cornerRadiusSm: Dp? = null,
    val cornerRadiusMd: Dp? = null,
    val cornerRadiusLg: Dp? = null,
    val cornerRadiusXl: Dp? = null,
    // Elevation
    val defaultElevation: Dp? = null,
    val cardElevation: Dp? = null,
    // Border
    val borderWidth: Dp? = null,
    val borderOpacity: Float? = null,
    // Shadow (for neumorphic dual-shadow)
    val dualShadow: Boolean = false,
    val lightShadowOffset: Dp? = null,
    val darkShadowOffset: Dp? = null,
    val lightShadowBlur: Dp? = null,
    val darkShadowBlur: Dp? = null,
    // Glass (for visionOS-style)
    val glassBlur: Dp? = null,
    val specularHighlight: Boolean = false,
    val ambientTint: Boolean = false,
    // Animation
    val springDamping: Float? = null,
    val transitionDurationMs: Int? = null,
    // Layout
    val minTouchTarget: Dp? = null,
    val panelCurvature: Float? = null,   // 0f = flat, 1f = fully curved
    // Typography
    val typographyScale: Float? = null,
) {
    companion object {
        /** No overrides — all defaults from the base theme. */
        val None = ThemeOverrides()
    }
}

/** Provides ThemeOverrides to the composition tree. */
val LocalThemeOverrides = staticCompositionLocalOf { ThemeOverrides.None }
