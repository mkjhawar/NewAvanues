/**
 * DisplayUtils.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.accessibility.ui.utils

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Display Utilities
 *
 * Provides utility functions for display metrics and screen size
 */
object DisplayUtils {
    /**
     * Get real screen size
     *
     * @param context Android context
     * @return Screen size as Point
     */
    fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }

    /**
     * Get real display metrics
     *
     * @param context Android context
     * @return Display metrics
     */
    fun getRealDisplayMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        return metrics
    }

    /**
     * Get display width in pixels
     *
     * @param context Android context
     * @return Display width
     */
    fun getDisplayWidth(context: Context): Int {
        return getRealScreenSize(context).x
    }

    /**
     * Get display height in pixels
     *
     * @param context Android context
     * @return Display height
     */
    fun getDisplayHeight(context: Context): Int {
        return getRealScreenSize(context).y
    }
}
