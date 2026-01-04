/**
 * BoundaryDetector.kt - Prevent cursor from leaving screen bounds
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import com.augmentalis.voiceos.accessibility.ScreenEdge

/**
 * Boundary check result
 *
 * @param isInBounds Whether point is within bounds
 * @param clampedX Clamped X coordinate
 * @param clampedY Clamped Y coordinate
 * @param nearEdge Which edge (if any) the point is near
 * @param distanceToEdge Distance to nearest edge in pixels
 */
data class BoundaryCheckResult(
    val isInBounds: Boolean,
    val clampedX: Float,
    val clampedY: Float,
    val nearEdge: ScreenEdge,
    val distanceToEdge: Float
)

/**
 * Safe area insets (notches, nav bars, etc.)
 *
 * @param left Left inset in pixels
 * @param top Top inset in pixels
 * @param right Right inset in pixels
 * @param bottom Bottom inset in pixels
 */
data class SafeAreaInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    /**
     * Apply insets to bounds
     */
    fun applyToBounds(bounds: Rect): Rect {
        return Rect(
            bounds.left + left,
            bounds.top + top,
            bounds.right - right,
            bounds.bottom - bottom
        )
    }

    /**
     * Check if any insets are present
     */
    fun hasInsets(): Boolean = left > 0 || top > 0 || right > 0 || bottom > 0
}

/**
 * Boundary configuration
 *
 * @param edgeThreshold Distance from edge to be considered "near edge"
 * @param respectSafeArea Whether to respect safe area insets
 * @param allowOverscroll Allow small overscroll before clamping
 * @param overscrollDistance Maximum overscroll distance
 */
data class BoundaryConfig(
    val edgeThreshold: Float = DEFAULT_EDGE_THRESHOLD,
    val respectSafeArea: Boolean = true,
    val allowOverscroll: Boolean = false,
    val overscrollDistance: Float = DEFAULT_OVERSCROLL_DISTANCE
) {
    companion object {
        const val DEFAULT_EDGE_THRESHOLD = 50f // pixels
        const val DEFAULT_OVERSCROLL_DISTANCE = 20f // pixels
    }
}

/**
 * Boundary detector
 *
 * Detects and enforces screen boundaries with:
 * - Edge detection (left, right, top, bottom)
 * - Bounds enforcement (clamp to screen)
 * - Multi-display support
 * - Safe area calculation (notches, nav bars)
 * - Configurable edge threshold and overscroll
 */
class BoundaryDetector(
    private val context: Context,
    private val config: BoundaryConfig = BoundaryConfig()
) {
    companion object {
        private const val TAG = "BoundaryDetector"
    }

    // Window manager for display access
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    // Current display metrics
    private var displayMetrics: DisplayMetrics = getDisplayMetrics()

    // Current safe area insets
    private var safeAreaInsets: SafeAreaInsets = SafeAreaInsets()

    // Effective bounds (screen bounds minus safe area)
    private var effectiveBounds: Rect = calculateEffectiveBounds()

    init {
        Log.d(TAG, "BoundaryDetector initialized")
        Log.d(TAG, "Screen size: ${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}")
        Log.d(TAG, "Safe area insets: $safeAreaInsets")
        Log.d(TAG, "Effective bounds: $effectiveBounds")
    }

    /**
     * Check if point is within bounds
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return BoundaryCheckResult with bounds check and clamped values
     */
    fun checkBounds(x: Float, y: Float): BoundaryCheckResult {
        val bounds = effectiveBounds

        // Check if point is within bounds (with optional overscroll)
        val overscroll = if (config.allowOverscroll) config.overscrollDistance else 0f
        val isInBounds = x >= bounds.left - overscroll &&
                x <= bounds.right + overscroll &&
                y >= bounds.top - overscroll &&
                y <= bounds.bottom + overscroll

        // Clamp to bounds
        val clampedX = x.coerceIn(bounds.left.toFloat(), bounds.right.toFloat())
        val clampedY = y.coerceIn(bounds.top.toFloat(), bounds.bottom.toFloat())

        // Detect nearest edge
        val nearEdge = detectNearEdge(clampedX, clampedY)

        // Calculate distance to nearest edge
        val distanceToEdge = calculateDistanceToEdge(clampedX, clampedY)

        return BoundaryCheckResult(
            isInBounds = isInBounds,
            clampedX = clampedX,
            clampedY = clampedY,
            nearEdge = nearEdge,
            distanceToEdge = distanceToEdge
        )
    }

    /**
     * Clamp point to bounds
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return Clamped PointF
     */
    fun clampToBounds(x: Float, y: Float): PointF {
        val bounds = effectiveBounds
        return PointF(
            x.coerceIn(bounds.left.toFloat(), bounds.right.toFloat()),
            y.coerceIn(bounds.top.toFloat(), bounds.bottom.toFloat())
        )
    }

    /**
     * Check if point is near any edge
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if point is within edge threshold of any edge
     */
    fun isNearEdge(x: Float, y: Float): Boolean {
        return detectNearEdge(x, y) != ScreenEdge.NONE
    }

    /**
     * Detect which edge (if any) a point is near
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return ScreenEdge enum
     */
    fun detectNearEdge(x: Float, y: Float): ScreenEdge {
        val bounds = effectiveBounds
        val threshold = config.edgeThreshold

        val nearLeft = x - bounds.left <= threshold
        val nearRight = bounds.right - x <= threshold
        val nearTop = y - bounds.top <= threshold
        val nearBottom = bounds.bottom - y <= threshold

        return when {
            nearLeft && nearTop -> ScreenEdge.TOP_LEFT
            nearRight && nearTop -> ScreenEdge.TOP_RIGHT
            nearLeft && nearBottom -> ScreenEdge.BOTTOM_LEFT
            nearRight && nearBottom -> ScreenEdge.BOTTOM_RIGHT
            nearLeft -> ScreenEdge.LEFT
            nearRight -> ScreenEdge.RIGHT
            nearTop -> ScreenEdge.TOP
            nearBottom -> ScreenEdge.BOTTOM
            else -> ScreenEdge.NONE
        }
    }

    /**
     * Calculate distance to nearest edge
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return Distance in pixels to nearest edge
     */
    fun calculateDistanceToEdge(x: Float, y: Float): Float {
        val bounds = effectiveBounds

        val distanceToLeft = x - bounds.left
        val distanceToRight = bounds.right - x
        val distanceToTop = y - bounds.top
        val distanceToBottom = bounds.bottom - y

        return minOf(distanceToLeft, distanceToRight, distanceToTop, distanceToBottom)
    }

    /**
     * Get effective screen bounds (accounting for safe area)
     *
     * @return Rect representing effective bounds
     */
    fun getEffectiveBounds(): Rect = effectiveBounds

    /**
     * Get raw screen bounds (full display area)
     *
     * @return Rect representing full screen bounds
     */
    fun getRawBounds(): Rect {
        return Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    /**
     * Get safe area insets
     *
     * @return SafeAreaInsets
     */
    fun getSafeAreaInsets(): SafeAreaInsets = safeAreaInsets

    /**
     * Update safe area insets from WindowInsets
     *
     * @param insets WindowInsets from view
     */
    fun updateSafeAreaInsets(insets: WindowInsets?) {
        if (insets == null || !config.respectSafeArea) {
            safeAreaInsets = SafeAreaInsets()
            effectiveBounds = calculateEffectiveBounds()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val systemBarInsets = insets.getInsets(WindowInsets.Type.systemBars())
            val cutoutInsets = insets.getInsets(WindowInsets.Type.displayCutout())

            safeAreaInsets = SafeAreaInsets(
                left = maxOf(systemBarInsets.left, cutoutInsets.left),
                top = maxOf(systemBarInsets.top, cutoutInsets.top),
                right = maxOf(systemBarInsets.right, cutoutInsets.right),
                bottom = maxOf(systemBarInsets.bottom, cutoutInsets.bottom)
            )
        } else {
            @Suppress("DEPRECATION")
            safeAreaInsets = SafeAreaInsets(
                left = insets.systemWindowInsetLeft,
                top = insets.systemWindowInsetTop,
                right = insets.systemWindowInsetRight,
                bottom = insets.systemWindowInsetBottom
            )
        }

        effectiveBounds = calculateEffectiveBounds()

        Log.d(TAG, "Safe area insets updated: $safeAreaInsets")
        Log.d(TAG, "Effective bounds updated: $effectiveBounds")
    }

    /**
     * Update display metrics (call when screen size changes)
     */
    fun updateDisplayMetrics() {
        displayMetrics = getDisplayMetrics()
        effectiveBounds = calculateEffectiveBounds()

        Log.d(TAG, "Display metrics updated")
        Log.d(TAG, "Screen size: ${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}")
        Log.d(TAG, "Effective bounds: $effectiveBounds")
    }

    /**
     * Get current display ID
     *
     * @return Display ID
     */
    fun getDisplayId(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.displayId ?: Display.DEFAULT_DISPLAY
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.displayId
        }
    }

    /**
     * Get display metrics
     */
    private fun getDisplayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.getRealMetrics(metrics)
                ?: windowManager.defaultDisplay.getRealMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }

        return metrics
    }

    /**
     * Calculate effective bounds (raw bounds minus safe area insets)
     */
    private fun calculateEffectiveBounds(): Rect {
        val rawBounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

        return if (config.respectSafeArea && safeAreaInsets.hasInsets()) {
            safeAreaInsets.applyToBounds(rawBounds)
        } else {
            rawBounds
        }
    }

    /**
     * Get screen center point
     *
     * @return PointF at screen center
     */
    fun getScreenCenter(): PointF {
        return PointF(
            effectiveBounds.centerX().toFloat(),
            effectiveBounds.centerY().toFloat()
        )
    }

    /**
     * Check if two points are on the same edge
     *
     * @param x1 First point X
     * @param y1 First point Y
     * @param x2 Second point X
     * @param y2 Second point Y
     * @return true if both points are near the same edge
     */
    fun areOnSameEdge(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val edge1 = detectNearEdge(x1, y1)
        val edge2 = detectNearEdge(x2, y2)

        if (edge1 == ScreenEdge.NONE || edge2 == ScreenEdge.NONE) {
            return false
        }

        return edge1 == edge2
    }

    /**
     * Validate point is within screen bounds, log warning if not
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param label Label for logging
     * @return Clamped point
     */
    fun validateAndClamp(x: Float, y: Float, label: String = "Point"): PointF {
        val result = checkBounds(x, y)

        if (!result.isInBounds) {
            Log.w(TAG, "$label is out of bounds: ($x, $y) -> (${result.clampedX}, ${result.clampedY})")
            Log.w(TAG, "Distance to edge: ${result.distanceToEdge}px, near edge: ${result.nearEdge}")
        }

        return PointF(result.clampedX, result.clampedY)
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        Log.d(TAG, "BoundaryDetector disposed")
    }
}
