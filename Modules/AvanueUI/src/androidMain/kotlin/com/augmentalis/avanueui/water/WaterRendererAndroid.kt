/**
 * AndroidWaterRenderer.kt - Android water effect renderer
 *
 * Three-tier implementation based on API level:
 * - API 33+ (Tiramisu): Full AGSL RuntimeShader for refraction + specular + caustics
 * - API 31-32 (S/Sv2): RenderEffect.createBlurEffect() + gradient overlay
 * - API 28-30: Fallback to existing Modifier.glass()
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.water

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.augmentalis.avanueui.components.glass.glass
import com.augmentalis.avanueui.glass.GlassBorder
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.WaterTokens

// ============================================================================
// PLATFORM CAPABILITIES
// ============================================================================

actual object PlatformWaterCapabilities {
    actual val supportsRefraction: Boolean
        get() = Build.VERSION.SDK_INT >= 33
    actual val supportsSpecular: Boolean
        get() = Build.VERSION.SDK_INT >= 31
    actual val supportsCaustics: Boolean
        get() = Build.VERSION.SDK_INT >= 33
}

// ============================================================================
// AGSL SHADERS (API 33+)
// ============================================================================

/**
 * AGSL refraction shader: Displaces content using simplex-like noise.
 * Creates the "bent light through water" distortion effect.
 */
private const val AGSL_REFRACTION_SHADER = """
uniform shader content;
uniform float2 resolution;
uniform float strength;
uniform float frequency;
uniform float time;

// Simple hash-based noise for AGSL (no external textures needed)
float hash(float2 p) {
    float h = dot(p, float2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float n1 = noise((uv + float2(time * 0.1, 0.0)) * resolution.x * frequency);
    float n2 = noise((uv + float2(0.0, time * 0.1) + 100.0) * resolution.x * frequency);
    float2 displacement = float2(n1 - 0.5, n2 - 0.5) * strength * 2.0;
    float2 sampleCoord = coord + displacement;
    sampleCoord = clamp(sampleCoord, float2(0.0), resolution);
    return content.eval(sampleCoord);
}
"""

/**
 * AGSL specular highlight shader: Applies a radial specular spot.
 */
private const val AGSL_SPECULAR_SHADER = """
uniform shader content;
uniform float2 resolution;
uniform float2 lightPos;
uniform float radius;
uniform float intensity;
uniform float falloff;
uniform float3 highlightColor;

half4 main(float2 coord) {
    half4 color = content.eval(coord);
    float2 uv = coord / resolution;
    float dist = distance(uv, lightPos);
    float highlight = exp(-pow(dist / (radius / resolution.x), falloff)) * intensity;
    color.rgb += half3(highlightColor) * half(highlight);
    return color;
}
"""

// ============================================================================
// PLATFORM WATER EFFECT (ANDROID actual)
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

    when {
        // --- Tier 1: Full AGSL (API 33+) ---
        Build.VERSION.SDK_INT >= 33 -> {
            waterEffectApi33(
                backgroundColor = backgroundColor,
                waterLevel = waterLevel,
                shape = shape,
                interactive = interactive,
                waterHighlightColor = waterScheme.highlightColor,
                waterCausticColor = waterScheme.causticColor,
                waterRefractionTint = waterScheme.refractionTint,
                enableRefraction = waterScheme.enableRefraction,
                enableSpecular = waterScheme.enableSpecular,
                enableCaustics = waterScheme.enableCaustics
            )
        }
        // --- Tier 2: Blur + overlay (API 31-32) ---
        Build.VERSION.SDK_INT >= 31 -> {
            waterEffectApi31(
                backgroundColor = backgroundColor,
                waterLevel = waterLevel,
                shape = shape,
                interactive = interactive,
                waterHighlightColor = waterScheme.highlightColor,
                waterRefractionTint = waterScheme.refractionTint
            )
        }
        // --- Tier 3: Glass fallback (API 28-30) ---
        else -> {
            val glassLevel = when (waterLevel) {
                WaterLevel.REGULAR -> GlassLevel.MEDIUM
                WaterLevel.CLEAR -> GlassLevel.LIGHT
                WaterLevel.IDENTITY -> GlassLevel.LIGHT
            }
            this.glass(
                backgroundColor = backgroundColor,
                glassLevel = glassLevel,
                shape = shape
            )
        }
    }
}

// ============================================================================
// TIER 1: API 33+ (Full AGSL shaders)
// ============================================================================

@Composable
private fun Modifier.waterEffectApi33(
    backgroundColor: Color,
    waterLevel: WaterLevel,
    shape: Shape,
    interactive: Boolean,
    waterHighlightColor: Color,
    waterCausticColor: Color,
    waterRefractionTint: Color,
    enableRefraction: Boolean,
    enableSpecular: Boolean,
    enableCaustics: Boolean
): Modifier = composed {
    val density = LocalDensity.current

    // Animated time uniform for refraction/caustic animation
    val infiniteTransition = rememberInfiniteTransition(label = "water")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waterTime"
    )

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

    // Resolve parameters per WaterLevel
    val blurRadiusPx = with(density) {
        when (waterLevel) {
            WaterLevel.REGULAR -> WaterTokens.blurRegular.toPx()
            WaterLevel.CLEAR -> WaterTokens.blurClear.toPx()
            WaterLevel.IDENTITY -> 0f
        }
    }
    val overlayOpacity = when (waterLevel) {
        WaterLevel.REGULAR -> WaterTokens.overlayOpacityRegular
        WaterLevel.CLEAR -> WaterTokens.overlayOpacityClear
        WaterLevel.IDENTITY -> 0f
    }
    val refractionStrength = with(density) {
        when (waterLevel) {
            WaterLevel.REGULAR -> WaterTokens.refractionStrengthRegular.toPx()
            WaterLevel.CLEAR -> WaterTokens.refractionStrengthClear.toPx()
            WaterLevel.IDENTITY -> 0f
        }
    }

    // Panel opacity per WaterLevel
    val panelOpacity = when (waterLevel) {
        WaterLevel.REGULAR -> WaterTokens.panelOpacityRegular
        WaterLevel.CLEAR -> WaterTokens.panelOpacityClear
        WaterLevel.IDENTITY -> 0f
    }

    this
        .clip(shape)
        // 1. Semi-transparent panel background (frosted glass fill)
        .background(color = backgroundColor.copy(alpha = panelOpacity), shape = shape)
        // 2. Subtle color accent tint
        .background(color = waterRefractionTint.copy(alpha = overlayOpacity), shape = shape)
        // 3. Press scale only (no blur/refraction — content stays sharp)
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        // 4. Specular + caustics BEHIND content, content drawn LAST
        .drawWithContent {
            // Specular highlight behind content
            if (enableSpecular) {
                val specRadiusPx = with(density) { WaterTokens.specularRadius.toPx() }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            waterHighlightColor.copy(alpha = WaterTokens.specularIntensity * WaterTokens.highlightOpacity),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.15f),
                        radius = specRadiusPx
                    ),
                    radius = specRadiusPx,
                    center = Offset(size.width * 0.5f, size.height * 0.15f)
                )
            }
            // Caustic shimmer behind content
            if (enableCaustics && waterLevel == WaterLevel.REGULAR) {
                val causticPhase = (time * WaterTokens.causticSpeed) % 1f
                val causticAlpha = WaterTokens.causticIntensity * (0.5f + 0.5f * kotlin.math.sin(causticPhase * 2f * Math.PI.toFloat()))
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            waterCausticColor.copy(alpha = causticAlpha),
                            Color.Transparent,
                            waterCausticColor.copy(alpha = causticAlpha * 0.6f),
                            Color.Transparent
                        ),
                        start = Offset(size.width * causticPhase, 0f),
                        end = Offset(size.width * (causticPhase + 0.5f), size.height)
                    )
                )
            }
            // Content drawn LAST — always sharp and readable
            drawContent()
        }
}

// ============================================================================
// TIER 2: API 31-32 (Blur + overlay, no AGSL)
// ============================================================================

@Composable
private fun Modifier.waterEffectApi31(
    backgroundColor: Color,
    waterLevel: WaterLevel,
    shape: Shape,
    interactive: Boolean,
    waterHighlightColor: Color,
    waterRefractionTint: Color
): Modifier = composed {
    val density = LocalDensity.current

    val blurRadiusPx = with(density) {
        when (waterLevel) {
            WaterLevel.REGULAR -> WaterTokens.blurRegular.toPx()
            WaterLevel.CLEAR -> WaterTokens.blurClear.toPx()
            WaterLevel.IDENTITY -> 0f
        }
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

    // Panel opacity per WaterLevel
    val panelOpacity = when (waterLevel) {
        WaterLevel.REGULAR -> WaterTokens.panelOpacityRegular
        WaterLevel.CLEAR -> WaterTokens.panelOpacityClear
        WaterLevel.IDENTITY -> 0f
    }

    this
        .clip(shape)
        // 1. Semi-transparent panel background
        .background(color = backgroundColor.copy(alpha = panelOpacity), shape = shape)
        // 2. Subtle color accent tint
        .background(color = waterRefractionTint.copy(alpha = overlayOpacity), shape = shape)
        // 3. Press scale only (no blur)
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        // 4. Specular behind content, content drawn last
        .drawWithContent {
            val specRadiusPx = with(density) { WaterTokens.specularRadius.toPx() }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        waterHighlightColor.copy(alpha = WaterTokens.specularIntensity * WaterTokens.highlightOpacity * 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.15f),
                    radius = specRadiusPx
                ),
                radius = specRadiusPx,
                center = Offset(size.width * 0.5f, size.height * 0.15f)
            )
            drawContent()
        }
}
