/**
 * IOSWaterRenderer.kt - iOS water effect renderer
 *
 * Two-tier implementation:
 * - iOS 26+: Maps to native SwiftUI .glassEffect(.regular/.clear/.identity)
 *   via Compose interop bridge. The native effect is applied in SwiftUI layer.
 * - iOS 17-25: Compose-based blur + gradient overlay (no native glass API)
 *
 * Note: The actual native .glassEffect() call happens in the iOS Renderer bridge
 * (Renderers/iOS/). This file provides the Compose-side parameters and fallback.
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalDensity
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.WaterTokens
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIDevice

// ============================================================================
// PLATFORM CAPABILITIES
// ============================================================================

actual object PlatformWaterCapabilities {
    private val iosVersion: Int by lazy {
        UIDevice.currentDevice.systemVersion
            .split(".").firstOrNull()?.toIntOrNull() ?: 0
    }

    /** iOS 26+ has native .glassEffect() API */
    actual val supportsRefraction: Boolean
        get() = iosVersion >= 26
    actual val supportsSpecular: Boolean
        get() = iosVersion >= 17
    actual val supportsCaustics: Boolean
        get() = iosVersion >= 26
}

// ============================================================================
// PLATFORM WATER EFFECT (iOS actual)
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

    // Lifecycle-aware pause: skip expensive drawing when app is backgrounded
    var isActive by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val bgObserver = center.addObserverForName(
            UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue
        ) { _ -> isActive = false }
        val fgObserver = center.addObserverForName(
            UIApplicationWillEnterForegroundNotification, null, NSOperationQueue.mainQueue
        ) { _ -> isActive = true }
        onDispose {
            center.removeObserver(bgObserver)
            center.removeObserver(fgObserver)
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

    // On iOS 26+, the native .glassEffect() is applied by the iOS Renderer bridge.
    // Here we provide the Compose-side overlay, specular, and interaction effects.
    // On older iOS, we do a full Compose-based water simulation.

    this
        .clip(shape)
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        // Tinted overlay
        .background(
            color = waterScheme.refractionTint.copy(alpha = overlayOpacity),
            shape = shape
        )
        // Specular highlight (skipped when backgrounded)
        .then(
            if (waterScheme.enableSpecular && isActive) {
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
        // Caustic shimmer (REGULAR level only, skipped when backgrounded)
        .then(
            if (waterScheme.enableCaustics && waterLevel == WaterLevel.REGULAR && isActive) {
                val infiniteTransition = rememberInfiniteTransition(label = "iosWaterCaustic")
                val time by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = WaterTokens.shimmerDuration,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "iosCausticTime"
                )
                Modifier.drawWithContent {
                    drawContent()
                    val causticAlpha = WaterTokens.causticIntensity *
                        (0.5f + 0.5f * kotlin.math.sin(time * 2f * Math.PI.toFloat()))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                waterScheme.causticColor.copy(alpha = causticAlpha),
                                Color.Transparent
                            ),
                            start = Offset(size.width * time, 0f),
                            end = Offset(size.width * (time + 0.4f), size.height)
                        )
                    )
                }
            } else Modifier
        )
}
