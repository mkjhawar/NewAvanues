/**
 * DisplayUtils.kt - Modern display metrics utilities
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-06
 * 
 * Provides modern WindowMetrics API for Android 11+ with backward compatibility.
 * Replaces deprecated getRealSize() usage with proper API level handling.
 */
package com.augmentalis.voiceos.accessibility.ui.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Utility object for modern display metrics handling
 */
object DisplayUtils {
    
    /**
     * Get real screen size using modern WindowMetrics API for Android 11+ (API 30+)
     * with fallback to deprecated getRealSize() for older versions.
     * 
     * @param context Context to get WindowManager from
     * @return Point containing screen width and height in pixels
     */
    fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use modern WindowMetrics API for Android 11+ (API 30+)
            val bounds = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            // Fallback to deprecated API for older versions
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(point)
            point
        }
    }
    
    /**
     * Get real screen size using modern WindowMetrics API for Android 11+ (API 30+)
     * with fallback to deprecated getRealSize() for older versions.
     * 
     * @param windowManager WindowManager instance
     * @return Point containing screen width and height in pixels
     */
    fun getRealScreenSize(windowManager: WindowManager): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use modern WindowMetrics API for Android 11+ (API 30+)
            val bounds = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            // Fallback to deprecated API for older versions
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(point)
            point
        }
    }
    
    /**
     * Get display metrics using modern WindowMetrics API for Android 11+ (API 30+)
     * with fallback to deprecated getRealMetrics() for older versions.
     * 
     * @param context Context to get WindowManager from
     * @return DisplayMetrics containing display properties
     */
    fun getRealDisplayMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return getRealDisplayMetrics(windowManager)
    }
    
    /**
     * Get display metrics using modern WindowMetrics API for Android 11+ (API 30+)
     * with fallback to deprecated getRealMetrics() for older versions.
     * 
     * @param windowManager WindowManager instance
     * @return DisplayMetrics containing display properties
     */
    fun getRealDisplayMetrics(windowManager: WindowManager): DisplayMetrics {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use modern WindowMetrics API for Android 11+ (API 30+)
            val bounds = windowManager.currentWindowMetrics.bounds
            val metrics = DisplayMetrics()
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            // Set density and other properties from system resources
            val systemMetrics = Resources.getSystem().displayMetrics
            metrics.density = systemMetrics.density
            metrics.densityDpi = systemMetrics.densityDpi
            @Suppress("DEPRECATION")
            metrics.scaledDensity = systemMetrics.scaledDensity
            metrics.xdpi = systemMetrics.xdpi
            metrics.ydpi = systemMetrics.ydpi
            metrics
        } else {
            // Fallback to deprecated API for older versions
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)
            metrics
        }
    }
}