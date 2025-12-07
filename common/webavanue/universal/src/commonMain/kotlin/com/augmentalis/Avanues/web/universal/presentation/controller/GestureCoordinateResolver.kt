package com.augmentalis.Avanues.web.universal.presentation.controller

/**
 * GestureCoordinateResolver - Resolves gesture coordinates with fallback logic
 *
 * Provides platform-agnostic coordinate resolution for gestures. The resolution
 * follows a priority chain:
 * 1. Explicit coordinates (if provided and valid)
 * 2. Last touch point (if tracked)
 * 3. Viewport center (fallback)
 *
 * Usage:
 * ```
 * val resolver = GestureCoordinateResolver(viewportWidth = 360f, viewportHeight = 640f)
 * val coords = resolver.resolve(x = -1f, y = -1f) // Returns viewport center
 * ```
 */
class GestureCoordinateResolver(
    private var viewportWidth: Float = DEFAULT_VIEWPORT_WIDTH,
    private var viewportHeight: Float = DEFAULT_VIEWPORT_HEIGHT
) {
    /**
     * Last tracked touch point
     */
    private var lastTouchX: Float? = null
    private var lastTouchY: Float? = null

    /**
     * Update viewport dimensions
     *
     * @param width New viewport width
     * @param height New viewport height
     */
    fun updateViewport(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
    }

    /**
     * Update last touch point
     *
     * @param x Touch X coordinate
     * @param y Touch Y coordinate
     */
    fun updateLastTouch(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
    }

    /**
     * Clear last touch point
     */
    fun clearLastTouch() {
        lastTouchX = null
        lastTouchY = null
    }

    /**
     * Resolve coordinates with fallback logic
     *
     * Priority:
     * 1. Provided coordinates (if both x >= 0 && y >= 0)
     * 2. Last touch point (if available)
     * 3. Viewport center (fallback)
     *
     * @param x Provided X coordinate (-1 for automatic)
     * @param y Provided Y coordinate (-1 for automatic)
     * @return Pair of (x, y) coordinates in viewport pixels
     */
    fun resolve(x: Float, y: Float): Pair<Float, Float> {
        // Priority 1: Use provided coordinates if valid
        if (x >= 0f && y >= 0f) {
            return Pair(x, y)
        }

        // Priority 2: Use last touch point if available
        val touchX = lastTouchX
        val touchY = lastTouchY
        if (touchX != null && touchY != null) {
            return Pair(touchX, touchY)
        }

        // Priority 3: Fallback to viewport center
        return Pair(viewportWidth / 2f, viewportHeight / 2f)
    }

    /**
     * Resolve coordinates, ensuring they're within viewport bounds
     *
     * @param x Provided X coordinate (-1 for automatic)
     * @param y Provided Y coordinate (-1 for automatic)
     * @return Pair of (x, y) coordinates clamped to viewport
     */
    fun resolveAndClamp(x: Float, y: Float): Pair<Float, Float> {
        val resolved = resolve(x, y)
        return Pair(
            resolved.first.coerceIn(0f, viewportWidth),
            resolved.second.coerceIn(0f, viewportHeight)
        )
    }

    /**
     * Check if coordinates are within viewport bounds
     *
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return true if coordinates are within viewport
     */
    fun isWithinViewport(x: Float, y: Float): Boolean {
        return x >= 0f && x <= viewportWidth && y >= 0f && y <= viewportHeight
    }

    /**
     * Get viewport center coordinates
     *
     * @return Pair of (x, y) at viewport center
     */
    fun getViewportCenter(): Pair<Float, Float> {
        return Pair(viewportWidth / 2f, viewportHeight / 2f)
    }

    /**
     * Get current viewport dimensions
     *
     * @return Pair of (width, height)
     */
    fun getViewportSize(): Pair<Float, Float> {
        return Pair(viewportWidth, viewportHeight)
    }

    /**
     * Check if last touch point is available
     *
     * @return true if a last touch point has been tracked
     */
    fun hasLastTouch(): Boolean {
        return lastTouchX != null && lastTouchY != null
    }

    companion object {
        // Default viewport dimensions (typical mobile screen)
        const val DEFAULT_VIEWPORT_WIDTH = 360f
        const val DEFAULT_VIEWPORT_HEIGHT = 640f
    }
}
