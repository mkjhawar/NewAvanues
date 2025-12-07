/**
 * CursorConfiguration.kt - Cursor appearance and behavior configuration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 */
package com.augmentalis.voiceos.cursor

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Cursor appearance and behavior configuration
 *
 * Parcelable data class for IPC communication.
 * Configures how the cursor looks and behaves.
 */
@Parcelize
data class CursorConfiguration(
    /**
     * Cursor size in density-independent pixels (dp)
     * Default: 48dp (Material Design touch target size)
     */
    val size: Float = 48f,

    /**
     * Cursor color in ARGB format
     * Default: 0xFF2196F3 (Material Blue 500)
     */
    val color: Int = 0xFF2196F3.toInt(),

    /**
     * Cursor opacity (0.0 = fully transparent, 1.0 = fully opaque)
     * Default: 0.8 (80% opaque)
     */
    val opacity: Float = 0.8f,

    /**
     * Whether cursor movement animations are enabled
     * Default: true
     */
    val animationEnabled: Boolean = true,

    /**
     * Cursor movement animation duration in milliseconds
     * Default: 300ms
     */
    val animationDuration: Long = 300L,

    /**
     * Whether haptic feedback is enabled for cursor actions
     * Default: true
     */
    val hapticFeedback: Boolean = true,

    /**
     * Click target radius in density-independent pixels (dp)
     * Area around cursor center that detects clicks
     * Default: 24dp (half of default cursor size)
     */
    val clickRadius: Float = 24f
) : Parcelable {

    companion object {
        /**
         * Default configuration
         */
        fun default(): CursorConfiguration {
            return CursorConfiguration()
        }

        /**
         * Large cursor configuration (for accessibility)
         */
        fun large(): CursorConfiguration {
            return CursorConfiguration(
                size = 64f,
                clickRadius = 32f
            )
        }

        /**
         * Small cursor configuration (for precision)
         */
        fun small(): CursorConfiguration {
            return CursorConfiguration(
                size = 32f,
                clickRadius = 16f
            )
        }

        /**
         * High contrast configuration (for visibility)
         */
        fun highContrast(): CursorConfiguration {
            return CursorConfiguration(
                color = 0xFFFFFF00.toInt(), // Yellow
                opacity = 1.0f,
                size = 56f
            )
        }

        /**
         * Performance configuration (no animations)
         */
        fun performance(): CursorConfiguration {
            return CursorConfiguration(
                animationEnabled = false,
                animationDuration = 0L,
                hapticFeedback = false
            )
        }
    }

    /**
     * Validate configuration values
     *
     * @return true if configuration is valid, false otherwise
     */
    fun isValid(): Boolean {
        return size > 0 &&
                opacity in 0.0f..1.0f &&
                animationDuration >= 0 &&
                clickRadius > 0
    }

    /**
     * Get color with applied opacity
     *
     * @return ARGB color with opacity applied to alpha channel
     */
    fun getColorWithOpacity(): Int {
        val alpha = (opacity * 255).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }
}
