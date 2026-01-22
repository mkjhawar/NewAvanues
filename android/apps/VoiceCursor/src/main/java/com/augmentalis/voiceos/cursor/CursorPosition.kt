/**
 * CursorPosition.kt - Cursor position data
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 */
package com.augmentalis.voiceos.cursor

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Cursor position with X/Y coordinates
 *
 * Parcelable data class for IPC communication.
 * Represents absolute screen coordinates where the cursor is positioned.
 */
@Parcelize
data class CursorPosition(
    /**
     * X coordinate in pixels (0 = left edge of screen)
     */
    val x: Float,

    /**
     * Y coordinate in pixels (0 = top edge of screen)
     */
    val y: Float
) : Parcelable {

    companion object {
        /**
         * Create position at origin (0, 0)
         */
        fun origin(): CursorPosition {
            return CursorPosition(0f, 0f)
        }

        /**
         * Create center position for given screen dimensions
         *
         * @param screenWidth Screen width in pixels
         * @param screenHeight Screen height in pixels
         * @return Position at screen center
         */
        fun center(screenWidth: Int, screenHeight: Int): CursorPosition {
            return CursorPosition(
                x = screenWidth / 2f,
                y = screenHeight / 2f
            )
        }

        /**
         * Create position from integer coordinates
         *
         * @param x X coordinate in pixels
         * @param y Y coordinate in pixels
         * @return CursorPosition with float coordinates
         */
        fun fromInt(x: Int, y: Int): CursorPosition {
            return CursorPosition(x.toFloat(), y.toFloat())
        }
    }

    /**
     * Check if position is within screen bounds
     *
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return true if position is within bounds, false otherwise
     */
    fun isWithinBounds(screenWidth: Int, screenHeight: Int): Boolean {
        return x >= 0 && x < screenWidth && y >= 0 && y < screenHeight
    }

    /**
     * Clamp position to screen bounds
     *
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return New position clamped to screen bounds
     */
    fun clampToBounds(screenWidth: Int, screenHeight: Int): CursorPosition {
        return CursorPosition(
            x = x.coerceIn(0f, screenWidth.toFloat() - 1),
            y = y.coerceIn(0f, screenHeight.toFloat() - 1)
        )
    }
}
