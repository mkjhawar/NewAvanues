/**
 * DesktopWaterRenderer.kt - Desktop (JVM/Skia) water effect renderer
 *
 * Uses Compose Desktop's Skia-backed rendering:
 * - Blur via graphicsLayer + BlurEffect (Skia ImageFilter.makeBlur)
 * - Specular highlight via drawWithContent radial gradient
 * - Caustic shimmer via animated linear gradient overlay
 *
 * Refraction displacement is approximated using animated gradient offsets
 * (true Skia ImageFilter.makeDisplacement requires a noise texture bitmap
 * which adds complexity; the gradient approximation is visually sufficient).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.water

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.WaterTokens

// ============================================================================
// PLATFORM CAPABILITIES
// ============================================================================

actual object PlatformWaterCapabilities {
    /** Skia displacement approximation via animated gradients */
    actual val supportsRefraction: Boolean = true
    actual val supportsSpecular: Boolean = true
    actual val supportsCaustics: Boolean = true
}

// ============================================================================
// PLATFORM WATER EFFECT (Desktop actual)
// ============================================================================

@Composable
actual fun Modifier.platformWaterEffect(
    backgroundColor: Color,
    waterLevel: WaterLevel,
    shape: Shape,
    interactive: Boolean
): Modifier = composed {
    val density = LocalDensity.current
    val waterScheme = AvanueTheme.water

    val blurRadius = when (waterLevel) {
        WaterLevel.REGULAR -> WaterTokens.blurRegular
        WaterLevel.CLEAR -> WaterTokens.blurClear
        WaterLevel.IDENTITY -> WaterTokens.blurIdentity
    }
    val overlayOpacity = when (waterLevel) {
        WaterLevel.REGULAR -> WaterTokens.overlayOpacityRegular
        WaterLevel.CLEAR -> WaterTokens.overlayOpacityClear
        WaterLevel.IDENTITY -> 0f
    }

    // Interactive press scale
    val pressScale = if (interactive) {
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) WaterTokens.pressScaleFactor else 1f,
            animationSpec = tween(durationMillis = WaterTokens.pressScaleDuration),
            label = "waterPressScale"
        )
        scale
    } else 1f

    // Animated time for caustics
    val infiniteTransition = rememberInfiniteTransition(label = "desktopWater")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WaterTokens.shimmerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "desktopWaterTime"
    )

    this
        .clip(shape)
        // Skia blur (via Compose Desktop's blur modifier)
        .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        // Refraction tint overlay
        .background(
            color = waterScheme.refractionTint.copy(alpha = overlayOpacity),
            shape = shape
        )
        // Specular highlight
        .then(
            if (waterScheme.enableSpecular) {
                Modifier.drawWithContent {
                    drawContent()
                    val specRadiusPx = with(density) { WaterTokens.specularRadius.toPx() }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                waterScheme.highlightColor.copy(
                                    alpha = WaterTokens.specularIntensity * WaterTokens.highlightOpacity
                                ),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.2f),
                            radius = specRadiusPx
                        ),
                        radius = specRadiusPx,
                        center = Offset(size.width * 0.5f, size.height * 0.2f)
                    )
                }
            } else Modifier
        )
        // Caustic shimmer (REGULAR only)
        .then(
            if (waterScheme.enableCaustics && waterLevel == WaterLevel.REGULAR) {
                Modifier.drawWithContent {
                    drawContent()
                    val causticAlpha = WaterTokens.causticIntensity *
                        (0.5f + 0.5f * kotlin.math.sin(time * 2f * Math.PI.toFloat()))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                waterScheme.causticColor.copy(alpha = causticAlpha),
                                Color.Transparent,
                                waterScheme.causticColor.copy(alpha = causticAlpha * 0.6f),
                                Color.Transparent
                            ),
                            start = Offset(size.width * time, 0f),
                            end = Offset(size.width * (time + 0.5f), size.height)
                        )
                    )
                }
            } else Modifier
        )
}
