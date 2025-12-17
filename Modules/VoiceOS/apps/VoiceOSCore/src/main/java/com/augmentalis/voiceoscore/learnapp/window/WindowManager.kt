/**
 * WindowManager.kt - Wrapper for managing windows during exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Manages window tracking and screen dimensions during exploration
 */

package com.augmentalis.voiceoscore.learnapp.window

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager as AndroidWindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Window Type
 *
 * Type of window detected during exploration.
 */
enum class WindowType {
    /** Main application window */
    MAIN_APP,

    /** System overlay window */
    OVERLAY,

    /** Dialog window */
    DIALOG,

    /** System window (status bar, navigation bar, etc.) */
    SYSTEM,

    /** Input method (keyboard) window */
    INPUT_METHOD,

    /** Unknown window type */
    UNKNOWN
}

/**
 * Window Info
 *
 * Information about a window detected during exploration.
 */
data class WindowInfo(
    val id: Int,
    val type: WindowType,
    val packageName: String?,
    val title: String?,
    val bounds: Rect,
    val isFocused: Boolean,
    val isActive: Boolean,
    val layer: Int,
    val rootNode: AccessibilityNodeInfo?
)

/**
 * Window Manager
 *
 * Manages window tracking during exploration.
 * Provides screen dimensions and window state information.
 */
class WindowManager(private val service: AccessibilityService) {
    companion object {
        private const val TAG = "LearnAppWindowManager"
    }

    /**
     * Nested reference to WindowType for convenient access
     * Allows usage like WindowManager.WindowType.MAIN_APP
     */
    object WindowType {
        val MAIN_APP = com.augmentalis.voiceoscore.learnapp.window.WindowType.MAIN_APP
        val OVERLAY = com.augmentalis.voiceoscore.learnapp.window.WindowType.OVERLAY
        val DIALOG = com.augmentalis.voiceoscore.learnapp.window.WindowType.DIALOG
        val SYSTEM = com.augmentalis.voiceoscore.learnapp.window.WindowType.SYSTEM
        val INPUT_METHOD = com.augmentalis.voiceoscore.learnapp.window.WindowType.INPUT_METHOD
        val UNKNOWN = com.augmentalis.voiceoscore.learnapp.window.WindowType.UNKNOWN
    }

    // Cached screen dimensions
    private var _screenWidth: Int = 0
    private var _screenHeight: Int = 0

    val screenWidth: Int get() = _screenWidth
    val screenHeight: Int get() = _screenHeight

    init {
        updateScreenDimensions()
    }

    /**
     * Update screen dimensions from display metrics
     */
    fun updateScreenDimensions() {
        try {
            val windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as AndroidWindowManager
            val displayMetrics = DisplayMetrics()

            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            _screenWidth = displayMetrics.widthPixels
            _screenHeight = displayMetrics.heightPixels

            Log.d(TAG, "Screen dimensions: ${_screenWidth}x${_screenHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get screen dimensions", e)
            // Use reasonable defaults
            _screenWidth = 1080
            _screenHeight = 1920
        }
    }

    /**
     * Get currently active windows
     *
     * @return List of active accessibility windows
     */
    fun getActiveWindows(): List<AccessibilityWindowInfo> {
        return try {
            service.windows ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get windows", e)
            emptyList()
        }
    }

    /**
     * Get root node of active app window
     *
     * @param packageName Target package name (null for any active window)
     * @return Root accessibility node or null
     */
    fun getRootInActiveWindow(packageName: String? = null): AccessibilityNodeInfo? {
        try {
            // If no specific package, return default active window root
            if (packageName == null) {
                return service.rootInActiveWindow
            }

            // Find window for specific package
            val windows = getActiveWindows()
            for (window in windows) {
                val root = window.root
                if (root?.packageName?.toString() == packageName) {
                    return root
                }
            }

            // Fallback to active window
            return service.rootInActiveWindow
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get root node", e)
            return null
        }
    }

    /**
     * Check if bounds are within screen
     *
     * @param bounds Rect to check
     * @return true if bounds are at least partially visible
     */
    fun isOnScreen(bounds: Rect): Boolean {
        if (bounds.isEmpty) return false
        return bounds.right > 0 &&
               bounds.bottom > 0 &&
               bounds.left < _screenWidth &&
               bounds.top < _screenHeight
    }

    /**
     * Check if bounds are fully visible on screen
     *
     * @param bounds Rect to check
     * @return true if bounds are fully visible
     */
    fun isFullyVisible(bounds: Rect): Boolean {
        if (bounds.isEmpty) return false
        return bounds.left >= 0 &&
               bounds.top >= 0 &&
               bounds.right <= _screenWidth &&
               bounds.bottom <= _screenHeight
    }

    /**
     * Get visible portion of bounds
     *
     * @param bounds Original bounds
     * @return Intersection with screen bounds
     */
    fun getVisibleBounds(bounds: Rect): Rect {
        val screenBounds = Rect(0, 0, _screenWidth, _screenHeight)
        val result = Rect()
        result.setIntersect(bounds, screenBounds)
        return result
    }

    /**
     * Get window information for all active windows
     *
     * @return List of WindowInfo for all active windows
     */
    fun getWindowInfoList(): List<WindowInfo> {
        return try {
            getActiveWindows().map { window ->
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                WindowInfo(
                    id = window.id,
                    type = mapWindowType(window.type),
                    packageName = window.root?.packageName?.toString(),
                    title = window.title?.toString(),
                    bounds = bounds,
                    isFocused = window.isFocused,
                    isActive = window.isActive,
                    layer = window.layer,
                    rootNode = window.root
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get window info list", e)
            emptyList()
        }
    }

    /**
     * Get app windows with retry
     *
     * @param maxRetries Maximum retry attempts
     * @param delayMs Delay between retries in milliseconds
     * @return List of WindowInfo for app windows
     */
    suspend fun getAppWindowsWithRetry(
        maxRetries: Int = 3,
        delayMs: Long = 100
    ): List<WindowInfo> {
        repeat(maxRetries) { attempt ->
            val windows = getWindowInfoList().filter {
                it.type == WindowType.MAIN_APP || it.type == WindowType.DIALOG
            }
            if (windows.isNotEmpty()) {
                return windows
            }
            if (attempt < maxRetries - 1) {
                kotlinx.coroutines.delay(delayMs)
            }
        }
        return emptyList()
    }

    /**
     * Map AccessibilityWindowInfo type to WindowType enum
     */
    private fun mapWindowType(type: Int): com.augmentalis.voiceoscore.learnapp.window.WindowType {
        return when (type) {
            AccessibilityWindowInfo.TYPE_APPLICATION -> com.augmentalis.voiceoscore.learnapp.window.WindowType.MAIN_APP
            AccessibilityWindowInfo.TYPE_SYSTEM -> com.augmentalis.voiceoscore.learnapp.window.WindowType.SYSTEM
            AccessibilityWindowInfo.TYPE_INPUT_METHOD -> com.augmentalis.voiceoscore.learnapp.window.WindowType.INPUT_METHOD
            AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> com.augmentalis.voiceoscore.learnapp.window.WindowType.OVERLAY
            else -> com.augmentalis.voiceoscore.learnapp.window.WindowType.UNKNOWN
        }
    }

    /**
     * Get app windows (convenience method)
     *
     * @param packageName Optional package name filter
     * @param launcherDetector Optional launcher detector for excluding launcher (use LauncherDetector object)
     * @return List of WindowInfo for application windows
     */
    fun getAppWindows(
        packageName: String? = null,
        launcherDetector: Any? = null  // Accept LauncherDetector object
    ): List<WindowInfo> {
        return getWindowInfoList().filter { window ->
            val isAppWindow = window.type == com.augmentalis.voiceoscore.learnapp.window.WindowType.MAIN_APP ||
                             window.type == com.augmentalis.voiceoscore.learnapp.window.WindowType.DIALOG

            val matchesPackage = packageName == null || window.packageName == packageName

            // Use LauncherDetector.isLauncher if provided
            val isNotLauncher = if (launcherDetector == com.augmentalis.voiceoscore.learnapp.detection.LauncherDetector &&
                                   window.packageName != null) {
                !com.augmentalis.voiceoscore.learnapp.detection.LauncherDetector.isLauncher(
                    service.applicationContext,
                    window.packageName
                )
            } else {
                true
            }

            isAppWindow && matchesPackage && isNotLauncher
        }
    }

    /**
     * Get log string representation of current window state
     *
     * @return Human-readable string of current windows
     */
    fun toLogString(): String {
        val windows = getWindowInfoList()
        if (windows.isEmpty()) return "No windows"

        return windows.joinToString("\n") { w ->
            "  [${w.type}] ${w.packageName ?: "unknown"} - ${w.title ?: "no title"} " +
            "(focused=${w.isFocused}, layer=${w.layer})"
        }
    }
}
