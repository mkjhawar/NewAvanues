/**
 * NumberOverlayStyle.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09 12:37:30 PDT
 */
/**
 * NumberOverlayStyle.kt
 *
 * Purpose: Data classes for aesthetic number overlay styling configuration
 * Implements Material Design 3 specifications with accessibility support
 *
 * Created: 2025-10-09 12:37:30 PDT
 */
package com.augmentalis.voiceoscore.ui.overlays

import android.graphics.Color
import android.graphics.Typeface
import com.augmentalis.voiceos.accessibility.AnchorPoint
import com.augmentalis.voiceos.accessibility.BadgeStyle
import com.augmentalis.voiceos.accessibility.ElementVoiceState

/**
 * Main styling configuration for number overlay badges
 *
 * Material Design 3 Specifications:
 * - Circle diameter: 32dp (16dp radius)
 * - Text size: 14sp (bold, white)
 * - Color coding: Green (enabled), Orange (needs setup), Grey (disabled)
 * - Drop shadow: 2dp offset, 4dp blur, 40% black
 */
data class NumberOverlayStyle(
    // Position configuration
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT,
    val offsetX: Int = 4,  // pixels from element edge (inside boundary)
    val offsetY: Int = 4,

    // Circle dimensions (Material Design 3)
    val circleRadius: Int = 16,  // 32dp diameter total
    val strokeWidth: Float = 2f,  // white border for contrast

    // Color scheme (Material Design 3 palette)
    val hasNameColor: Int = Color.parseColor("#4CAF50"),      // Material Green 500
    val noNameColor: Int = Color.parseColor("#FF9800"),       // Material Orange 500
    val disabledColor: Int = Color.parseColor("#9E9E9E"),     // Material Grey 500
    val strokeColor: Int = Color.WHITE,

    // Number styling
    val numberColor: Int = Color.WHITE,
    val numberSize: Float = 14f,  // sp (scales with system font settings)
    val fontWeight: Typeface = Typeface.DEFAULT_BOLD,

    // Visual effects
    val dropShadow: Boolean = true,
    val shadowRadius: Float = 4f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 2f,
    val shadowColor: Int = Color.parseColor("#66000000")  // 40% black
) {
    companion object {
        /**
         * Default standard style (Material Design 3)
         */
        fun standard() = NumberOverlayStyle()

        /**
         * High contrast mode for visually impaired users
         * - Increased stroke width (3dp)
         * - Larger radius (20dp = 40dp diameter)
         * - Darker colors for better contrast
         */
        fun highContrast() = NumberOverlayStyle(
            circleRadius = 20,
            strokeWidth = 3f,
            hasNameColor = Color.BLACK,
            noNameColor = Color.parseColor("#FF6600"),  // Darker orange
            disabledColor = Color.parseColor("#666666"),  // Darker grey
            shadowRadius = 6f,
            shadowColor = Color.parseColor("#99000000")  // 60% black
        )

        /**
         * Large text mode for accessibility
         * - Larger circle (24dp radius = 48dp diameter)
         * - Larger text (18sp)
         * - Increased spacing
         */
        fun largeText() = NumberOverlayStyle(
            circleRadius = 24,
            numberSize = 18f,
            offsetX = 6,
            offsetY = 6,
            strokeWidth = 3f
        )

        /**
         * Compact mode for screens with many elements
         * - Smaller circle (12dp radius = 24dp diameter)
         * - Smaller text (12sp)
         * - No shadow for performance
         */
        fun compact() = NumberOverlayStyle(
            circleRadius = 12,
            numberSize = 12f,
            offsetX = 2,
            offsetY = 2,
            strokeWidth = 1.5f,
            dropShadow = false
        )
    }
}

/**
 * Anchor point positions for badge placement
 *
 * Determines where the circular badge is positioned relative to the element bounds
 * TOP_RIGHT is default as it's most common in UI patterns (notifications, badges)
 */

/**
 * Visual state of an element for color coding
 *
 * Color Semantics:
 * - ENABLED_WITH_NAME: Green (#4CAF50) - Element is voice-enabled with custom name
 * - ENABLED_NO_NAME: Orange (#FF9800) - Element is voice-enabled but needs user to set name
 * - DISABLED: Grey (#9E9E9E) - Element is not voice-interactive
 */

/**
 * Style variant selector for different use cases
 */
enum class StyleVariant {
    /**
     * Standard Material Design 3 style
     * 32dp diameter, 14sp text, full effects
     */
    STANDARD,

    /**
     * High contrast for visually impaired
     * 40dp diameter, thicker borders, darker colors
     */
    HIGH_CONTRAST,

    /**
     * Large text for accessibility
     * 48dp diameter, 18sp text, increased spacing
     */
    LARGE_TEXT,

    /**
     * Compact for crowded screens
     * 24dp diameter, 12sp text, no shadow
     */
    COMPACT
}

/**
 * Extension function to get style from variant
 */
fun StyleVariant.toStyle(): NumberOverlayStyle = when (this) {
    StyleVariant.STANDARD -> NumberOverlayStyle.standard()
    StyleVariant.HIGH_CONTRAST -> NumberOverlayStyle.highContrast()
    StyleVariant.LARGE_TEXT -> NumberOverlayStyle.largeText()
    StyleVariant.COMPACT -> NumberOverlayStyle.compact()
}

/**
 * Configuration for rendering optimization
 */
data class RenderConfig(
    /**
     * Enable hardware acceleration for rendering
     * Recommended: true (uses GPU for better performance)
     */
    val hardwareAcceleration: Boolean = true,

    /**
     * Use Paint object pooling to avoid allocations
     * Recommended: true (reduces GC pressure)
     */
    val paintPooling: Boolean = true,

    /**
     * Cache text bounds measurements
     * Recommended: true (avoids repeated calculations)
     */
    val cacheTextBounds: Boolean = true,

    /**
     * Use partial invalidation (only redraw changed regions)
     * Recommended: true (better performance)
     */
    val partialInvalidation: Boolean = true,

    /**
     * Maximum number of overlays to render per frame
     * Higher values = more screen coverage, lower performance
     * Recommended: 100 for good balance
     */
    val maxOverlaysPerFrame: Int = 100,

    /**
     * Target frame time in milliseconds (16ms = 60 FPS)
     */
    val targetFrameTimeMs: Long = 16
)
