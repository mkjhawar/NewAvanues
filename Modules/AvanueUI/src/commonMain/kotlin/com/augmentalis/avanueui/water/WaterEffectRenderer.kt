/**
 * WaterEffectRenderer.kt - Platform capability declarations for water effects
 *
 * Each platform actual reports what rendering features it supports.
 * The common WaterExtensions code uses these to decide fallback paths.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.water

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Platform-reported water rendering capabilities.
 * Checked at runtime by [waterEffect] to select optimal render path.
 */
expect object PlatformWaterCapabilities {
    /** AGSL API 33+ on Android, iOS 26+ glassEffect, Skia displacement on Desktop */
    val supportsRefraction: Boolean
    /** Specular highlight overlay support */
    val supportsSpecular: Boolean
    /** Animated caustic pattern support */
    val supportsCaustics: Boolean
}

/**
 * Platform-specific water effect modifier implementation.
 *
 * Each platform provides the actual rendering:
 * - Android: AGSL RuntimeShader (API 33+), RenderEffect blur (API 31+), glass fallback
 * - iOS: native .glassEffect() (iOS 26+), UIVisualEffectView fallback
 * - Desktop: Skia ImageFilter displacement + blur
 */
@Composable
expect fun Modifier.platformWaterEffect(
    backgroundColor: Color,
    waterLevel: WaterLevel,
    shape: Shape,
    interactive: Boolean
): Modifier
