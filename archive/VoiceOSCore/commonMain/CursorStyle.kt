/**
 * CursorStyle.kt - Cursor styling and appearance configuration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 * Phase: 11 - Cursor System
 *
 * Provides cursor shape definitions and style presets for the VoiceOS
 * cursor system, supporting accessibility-focused visual customization.
 */
package com.augmentalis.voiceoscore

/**
 * Available cursor shapes for visual representation.
 *
 * Each shape serves different use cases:
 * - CIRCLE: Default general-purpose cursor
 * - CROSSHAIR: Precision targeting for fine selections
 * - POINTER: Traditional pointing cursor
 * - DOT: Minimal visual footprint
 */
enum class CursorShape {
    CIRCLE,
    CROSSHAIR,
    POINTER,
    DOT
}

/**
 * Data class defining cursor visual style and animation properties.
 *
 * Supports customization of:
 * - Shape and dimensions
 * - Colors (fill and border)
 * - Transparency
 * - Pulse animation
 *
 * @property shape The visual shape of the cursor
 * @property size Diameter/size in density-independent pixels
 * @property color Fill color in ARGB format (0xAARRGGBB)
 * @property borderColor Border/outline color in ARGB format
 * @property borderWidth Border thickness in pixels
 * @property opacity Overall transparency (0.0 = invisible, 1.0 = opaque)
 * @property pulseEnabled Whether cursor has pulsing animation
 * @property pulseSpeed Animation speed multiplier (1.0 = normal, 2.0 = fast)
 */
data class CursorStyle(
    val shape: CursorShape = CursorShape.CIRCLE,
    val size: Int = 32,
    val color: Long = 0xFF2196F3L,  // Material Blue 500
    val borderColor: Long = 0xFFFFFFFFL,  // White
    val borderWidth: Int = 2,
    val opacity: Float = 0.8f,
    val pulseEnabled: Boolean = true,
    val pulseSpeed: Float = 1.0f  // 1.0 = normal, 2.0 = fast
) {
    companion object {
        /**
         * Default cursor style with blue color, white border, and pulse animation.
         * Suitable for general use with good visibility.
         */
        val DEFAULT = CursorStyle()

        /**
         * Crosshair cursor for precision targeting.
         * Larger size, red color, no pulse animation for steady targeting.
         */
        val CROSSHAIR = CursorStyle(
            shape = CursorShape.CROSSHAIR,
            size = 48,
            color = 0xFFFF0000L,  // Red
            borderWidth = 1,
            pulseEnabled = false
        )

        /**
         * Minimal cursor with smallest visual footprint.
         * Dot shape, small size, no border or pulse.
         */
        val MINIMAL = CursorStyle(
            shape = CursorShape.DOT,
            size = 16,
            borderWidth = 0,
            pulseEnabled = false
        )

        /**
         * High contrast cursor for accessibility.
         * Yellow on black for maximum visibility, full opacity.
         */
        val HIGH_CONTRAST = CursorStyle(
            shape = CursorShape.CIRCLE,
            color = 0xFFFFFF00L,  // Yellow
            borderColor = 0xFF000000L,  // Black
            borderWidth = 3,
            opacity = 1.0f
        )
    }
}
