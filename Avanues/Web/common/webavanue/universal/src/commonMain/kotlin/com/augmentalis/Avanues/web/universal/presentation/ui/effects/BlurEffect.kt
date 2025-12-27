package com.augmentalis.Avanues.web.universal.presentation.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * BlurEffect - Platform-agnostic blur interface
 *
 * Provides glassmorphism/blur effects across all platforms
 * Platform-specific implementations handle:
 * - Android: RenderEffect (API 31+) or translucent fallback
 * - iOS: UIBlurEffect
 * - Desktop: Translucent surfaces
 * - Web: CSS backdrop-filter
 */

/**
 * Check if current platform/device supports blur effects
 */
expect fun supportsBlur(): Boolean

/**
 * Apply glassmorphism blur effect
 *
 * @param blurRadius Blur radius (platform-specific units)
 * @param backgroundColor Fallback/tint color (with alpha for translucency)
 * @param borderColor Optional border color for glass edge effect
 * @param borderWidth Border width in platform units
 */
@Composable
expect fun Modifier.glassmorphism(
    blurRadius: Float = 40f,
    backgroundColor: Color = Color(0x143B82F6),
    borderColor: Color? = Color(0x263B82F6),
    borderWidth: Float = 1f
): Modifier
