/**
 * ThemeUtils.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/ui/ThemeUtils.kt
 *
 * Created: 2025-01-26 03:30 PST
 * Last Modified: 2025-01-23
 * Author: Manoj Jhawar
 * Version: 2.0.0
 *
 * Purpose: ARVision theme utilities with glass morphism effects for VoiceCursor
 * Module: VoiceCursor System
 *
 * Changelog:
 * - v1.0.0 (2025-01-26 03:30 PST): Initial theme stubs for compilation validation
 * - v2.0.0 (2025-01-23): Full implementation with glass morphism effects
 *
 * @deprecated Scheduled for consolidation into libraries/VoiceUIElements
 * See: VOSFIX-006 in VoiceOS-Backlog-CodeAnalysis-251227-V1.md
 * ARVisionColors and DepthLevel enum should be merged with VoiceOSCore version.
 */

package com.augmentalis.voiceos.cursor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for glass morphism visual effects
 * Provides full ARVision-style translucent appearance
 */
data class GlassMorphismConfig(
    val cornerRadius: Dp = 12.dp,
    val backgroundOpacity: Float = 0.15f,
    val borderOpacity: Float = 0.2f,
    val borderWidth: Dp = 1.dp,
    val tintColor: Color = Color(0xFF007AFF), // ARVision Blue
    val tintOpacity: Float = 0.1f,
    val noiseOpacity: Float = 0.05f,
    val blurRadius: Dp = 16.dp
)

/**
 * Depth level for layered UI elements
 * Controls shadow and elevation effects
 */
enum class DepthLevel(val depth: Float, val blurAmount: Dp, val shadowAlpha: Float) {
    SURFACE(0f, 0.dp, 0f),
    CARD(1f, 4.dp, 0.05f),
    ELEVATED(2f, 8.dp, 0.1f),
    MODAL(3f, 16.dp, 0.15f),
    OVERLAY(4f, 24.dp, 0.2f)
}

/**
 * Glass morphism modifier for ARVision theme
 * Creates translucent, blurred background with subtle borders
 */
fun Modifier.glassMorphism(
    config: GlassMorphismConfig = GlassMorphismConfig(),
    depth: DepthLevel = DepthLevel.CARD,
    isDarkTheme: Boolean = false
): Modifier = composed {
    val backgroundColor = if (isDarkTheme) {
        Color.White.copy(alpha = config.backgroundOpacity * 0.8f)
    } else {
        Color.Black.copy(alpha = config.backgroundOpacity)
    }
    
    val borderColor = if (isDarkTheme) {
        Color.White.copy(alpha = config.borderOpacity)
    } else {
        Color.Black.copy(alpha = config.borderOpacity * 0.5f)
    }
    
    val gradientColors = listOf(
        config.tintColor.copy(alpha = config.tintOpacity),
        config.tintColor.copy(alpha = config.tintOpacity * 0.5f),
        Color.Transparent
    )
    
    this
        .clip(RoundedCornerShape(config.cornerRadius))
        .background(
            brush = Brush.verticalGradient(
                colors = gradientColors
            )
        )
        .background(
            color = backgroundColor,
            shape = RoundedCornerShape(config.cornerRadius)
        )
        .border(
            width = config.borderWidth,
            color = borderColor,
            shape = RoundedCornerShape(config.cornerRadius)
        )
        .then(
            if (depth != DepthLevel.SURFACE) {
                Modifier.blur(radius = depth.blurAmount * 0.1f)
            } else {
                Modifier
            }
        )
}

/**
 * ARVision system colors
 */
object ARVisionColors {
    val SystemBlue = Color(0xFF007AFF)
    val SystemTeal = Color(0xFF30B0C7)
    val SystemPurple = Color(0xFFAF52DE)
    val SystemGreen = Color(0xFF34C759)
    val SystemRed = Color(0xFFFF3B30)
    val SystemOrange = Color(0xFFFF9500)
    val SystemYellow = Color(0xFFFFCC00)
    val SystemGray = Color(0xFF8E8E93)
    
    // Glass morphism specific
    val GlassBackground = Color(0x1A000000)
    val GlassBorder = Color(0x33FFFFFF)
    val GlassShadow = Color(0x1A000000)
}

/**
 * Create a glass panel modifier with standard settings
 */
@Composable
fun Modifier.glassPanel(
    cornerRadius: Dp = 16.dp,
    isDarkTheme: Boolean = false
): Modifier {
    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = cornerRadius,
            backgroundOpacity = if (isDarkTheme) 0.2f else 0.15f,
            borderOpacity = if (isDarkTheme) 0.3f else 0.2f,
            tintColor = ARVisionColors.SystemBlue
        ),
        depth = DepthLevel.CARD,
        isDarkTheme = isDarkTheme
    )
}

/**
 * Create a glass button modifier
 */
@Composable
fun Modifier.glassButton(
    isPressed: Boolean = false,
    isDarkTheme: Boolean = false
): Modifier {
    val opacity = if (isPressed) 0.3f else 0.2f
    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = 8.dp,
            backgroundOpacity = opacity,
            borderOpacity = 0.25f,
            borderWidth = 1.5.dp,
            tintColor = ARVisionColors.SystemBlue
        ),
        depth = if (isPressed) DepthLevel.SURFACE else DepthLevel.ELEVATED,
        isDarkTheme = isDarkTheme
    )
}