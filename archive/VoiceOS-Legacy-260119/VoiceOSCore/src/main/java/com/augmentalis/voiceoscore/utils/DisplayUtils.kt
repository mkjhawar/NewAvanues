/**
 * DisplayUtils.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Utility functions for display/screen operations
 */
package com.augmentalis.voiceoscore.utils

import android.content.Context
import android.graphics.Rect
import android.view.WindowManager

/**
 * Display Utilities
 *
 * Helper functions for screen size and bounds calculations
 */
object DisplayUtils {

    /**
     * Get screen size in pixels
     *
     * @param context Android context
     * @return Pair of (width, height) in pixels
     */
    fun getScreenSize(context: Context): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * Get screen bounds as Rect
     *
     * @param context Android context
     * @return Screen bounds rectangle
     */
    fun getScreenBounds(context: Context): Rect {
        val (width, height) = getScreenSize(context)
        return Rect(0, 0, width, height)
    }

    /**
     * Get display density
     *
     * @param context Android context
     * @return Density (eg 1.0, 1.5, 2.0, 3.0)
     */
    fun getDisplayDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    /**
     * Convert dp to pixels
     *
     * @param context Android context
     * @param dp Value in dp
     * @return Value in pixels
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * getDisplayDensity(context)).toInt()
    }
}
