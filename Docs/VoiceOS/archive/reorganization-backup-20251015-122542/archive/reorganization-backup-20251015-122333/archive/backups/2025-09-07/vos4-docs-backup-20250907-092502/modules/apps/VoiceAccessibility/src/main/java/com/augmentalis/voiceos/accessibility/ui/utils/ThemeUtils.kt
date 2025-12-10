/**
 * ThemeUtils.kt - Glassmorphism theme utilities for VoiceOS Accessibility
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Glassmorphism UI utilities matching VoiceCursor style for consistency.
 * Based on VoiceOS-SRS ThemeUtils but enhanced for accessibility app.
 */
package com.augmentalis.voiceos.accessibility.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for glassmorphism effect
 */
data class GlassMorphismConfig(
    val cornerRadius: Dp,
    val backgroundOpacity: Float,
    val borderOpacity: Float,
    val borderWidth: Dp,
    val tintColor: Color,
    val tintOpacity: Float,
    val noiseOpacity: Float = 0.05f
)

/**
 * Depth level for layered glassmorphism effects
 */
data class DepthLevel(val depth: Float)

/**
 * Default glassmorphism configurations
 */
object GlassMorphismDefaults {
    
    val Primary = GlassMorphismConfig(
        cornerRadius = 16.dp,
        backgroundOpacity = 0.1f,
        borderOpacity = 0.2f,
        borderWidth = 1.dp,
        tintColor = Color(0xFF4285F4),
        tintOpacity = 0.15f
    )
    
    val Secondary = GlassMorphismConfig(
        cornerRadius = 12.dp,
        backgroundOpacity = 0.08f,
        borderOpacity = 0.15f,
        borderWidth = 0.5.dp,
        tintColor = Color(0xFF673AB7),
        tintOpacity = 0.12f
    )
    
    val Card = GlassMorphismConfig(
        cornerRadius = 16.dp,
        backgroundOpacity = 0.1f,
        borderOpacity = 0.2f,
        borderWidth = 1.dp,
        tintColor = Color.White,
        tintOpacity = 0.1f
    )
    
    val Button = GlassMorphismConfig(
        cornerRadius = 8.dp,
        backgroundOpacity = 0.15f,
        borderOpacity = 0.3f,
        borderWidth = 1.dp,
        tintColor = Color(0xFF4CAF50),
        tintOpacity = 0.2f
    )
}

/**
 * Apply glassmorphism effect to any composable
 */
fun Modifier.glassMorphism(
    config: GlassMorphismConfig,
    depth: DepthLevel,
    isDarkTheme: Boolean = true
): Modifier {
    val shape = RoundedCornerShape(config.cornerRadius)
    
    return this
        .clip(shape)
        .background(
            brush = createGlassBrush(config, depth, isDarkTheme),
            shape = shape
        )
        .border(
            width = config.borderWidth,
            brush = createBorderBrush(config, isDarkTheme),
            shape = shape
        )
}

/**
 * Create glass background brush with depth and tint
 */
private fun createGlassBrush(
    config: GlassMorphismConfig,
    depth: DepthLevel,
    isDarkTheme: Boolean
): Brush {
    val baseColor = if (isDarkTheme) Color.White else Color.Black
    val tintColor = config.tintColor
    
    return Brush.verticalGradient(
        colors = listOf(
            baseColor.copy(alpha = config.backgroundOpacity * (1f + depth.depth * 0.3f)),
            tintColor.copy(alpha = config.tintOpacity),
            baseColor.copy(alpha = config.backgroundOpacity * (1f - depth.depth * 0.2f))
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )
}

/**
 * Create border brush with subtle glow effect
 */
private fun createBorderBrush(
    config: GlassMorphismConfig,
    isDarkTheme: Boolean
): Brush {
    val baseColor = if (isDarkTheme) Color.White else Color.Black
    
    return Brush.verticalGradient(
        colors = listOf(
            baseColor.copy(alpha = config.borderOpacity * 1.5f),
            config.tintColor.copy(alpha = config.borderOpacity),
            baseColor.copy(alpha = config.borderOpacity * 0.8f)
        )
    )
}

/**
 * Enhanced glassmorphism with blur effect (for high-end devices)
 */
fun Modifier.glassMorphismBlur(
    config: GlassMorphismConfig,
    depth: DepthLevel,
    blurRadius: Dp = 8.dp,
    isDarkTheme: Boolean = true
): Modifier {
    return this
        .blur(radius = blurRadius)
        .glassMorphism(config, depth, isDarkTheme)
}

/**
 * Floating card style with enhanced depth
 */
fun Modifier.floatingCard(
    elevation: Dp = 8.dp,
    cornerRadius: Dp = 16.dp,
    tintColor: Color = Color(0xFF4285F4)
): Modifier {
    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = cornerRadius,
            backgroundOpacity = 0.12f,
            borderOpacity = 0.25f,
            borderWidth = 1.dp,
            tintColor = tintColor,
            tintOpacity = 0.18f
        ),
        depth = DepthLevel(elevation.value / 8f)
    )
}

/**
 * Interactive button style with hover-like effect
 */
fun Modifier.interactiveGlass(
    isPressed: Boolean = false,
    tintColor: Color = Color(0xFF4CAF50)
): Modifier {
    val config = if (isPressed) {
        GlassMorphismConfig(
            cornerRadius = 8.dp,
            backgroundOpacity = 0.2f,
            borderOpacity = 0.4f,
            borderWidth = 1.5.dp,
            tintColor = tintColor,
            tintOpacity = 0.3f
        )
    } else {
        GlassMorphismConfig(
            cornerRadius = 8.dp,
            backgroundOpacity = 0.15f,
            borderOpacity = 0.3f,
            borderWidth = 1.dp,
            tintColor = tintColor,
            tintOpacity = 0.2f
        )
    }
    
    return this.glassMorphism(
        config = config,
        depth = DepthLevel(if (isPressed) 0.2f else 0.6f)
    )
}

/**
 * Status indicator style (for service status, permissions, etc.)
 */
fun Modifier.statusIndicator(
    isActive: Boolean,
    activeColor: Color = Color(0xFF00C853),
    inactiveColor: Color = Color(0xFFFF5722)
): Modifier {
    val tintColor = if (isActive) activeColor else inactiveColor
    
    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = 8.dp,
            backgroundOpacity = 0.1f,
            borderOpacity = 0.3f,
            borderWidth = 1.dp,
            tintColor = tintColor,
            tintOpacity = 0.25f
        ),
        depth = DepthLevel(0.4f)
    )
}

/**
 * Navigation card style for main menu items
 */
fun Modifier.navigationCard(
    isEnabled: Boolean = true,
    tintColor: Color = Color(0xFF2196F3)
): Modifier {
    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = 16.dp,
            backgroundOpacity = if (isEnabled) 0.1f else 0.05f,
            borderOpacity = if (isEnabled) 0.2f else 0.1f,
            borderWidth = 1.dp,
            tintColor = if (isEnabled) tintColor else Color.Gray,
            tintOpacity = if (isEnabled) 0.15f else 0.08f
        ),
        depth = DepthLevel(if (isEnabled) 0.6f else 0.2f)
    )
}

/**
 * Header style for app title and branding
 */
fun Modifier.headerGlass(): Modifier {
    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = 16.dp,
            backgroundOpacity = 0.1f,
            borderOpacity = 0.2f,
            borderWidth = 1.dp,
            tintColor = Color(0xFF4285F4),
            tintOpacity = 0.15f
        ),
        depth = DepthLevel(0.8f)
    )
}

/**
 * Theme utility object with common functions
 */
object ThemeUtils {
    /**
     * Get appropriate text color for glassmorphism backgrounds
     */
    fun getTextColor(isDarkTheme: Boolean = true, alpha: Float = 1f): Color {
        return if (isDarkTheme) {
            Color.White.copy(alpha = alpha)
        } else {
            Color.Black.copy(alpha = alpha)
        }
    }
    
    /**
     * Get secondary text color
     */
    fun getSecondaryTextColor(isDarkTheme: Boolean = true): Color {
        return getTextColor(isDarkTheme, alpha = 0.7f)
    }
    
    /**
     * Get disabled text color
     */
    fun getDisabledTextColor(isDarkTheme: Boolean = true): Color {
        return getTextColor(isDarkTheme, alpha = 0.4f)
    }
}