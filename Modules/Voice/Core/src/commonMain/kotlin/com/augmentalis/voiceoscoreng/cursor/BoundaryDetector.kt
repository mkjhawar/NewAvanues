package com.augmentalis.voiceoscoreng.cursor

/**
 * Represents the screen boundaries with optional safe insets.
 *
 * Safe insets define areas that should be avoided (e.g., notch, status bar, navigation bar).
 *
 * @property width Total screen width in pixels
 * @property height Total screen height in pixels
 * @property safeInsetTop Safe area inset from the top edge
 * @property safeInsetBottom Safe area inset from the bottom edge
 * @property safeInsetLeft Safe area inset from the left edge
 * @property safeInsetRight Safe area inset from the right edge
 */
data class ScreenBounds(
    val width: Int,
    val height: Int,
    val safeInsetTop: Int = 0,
    val safeInsetBottom: Int = 0,
    val safeInsetLeft: Int = 0,
    val safeInsetRight: Int = 0
) {
    /**
     * The usable width within safe insets.
     */
    val safeWidth: Int get() = width - safeInsetLeft - safeInsetRight

    /**
     * The usable height within safe insets.
     */
    val safeHeight: Int get() = height - safeInsetTop - safeInsetBottom
}

/**
 * Detects and manages screen boundaries for cursor movement.
 *
 * Provides functionality for:
 * - Checking if positions are within screen bounds
 * - Checking if positions are within safe bounds (excluding system UI areas)
 * - Clamping positions to valid ranges
 * - Detecting which edge a position is at
 *
 * @property bounds The current screen bounds configuration
 */
class BoundaryDetector(
    private var bounds: ScreenBounds = ScreenBounds(1080, 1920)
) {
    /**
     * Updates the screen bounds.
     *
     * @param bounds New screen bounds to use
     */
    fun setBounds(bounds: ScreenBounds) {
        this.bounds = bounds
    }

    /**
     * Returns the current screen bounds.
     *
     * @return Current ScreenBounds configuration
     */
    fun getBounds(): ScreenBounds = bounds

    /**
     * Checks if a position is within the full screen bounds.
     *
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return true if position is within bounds (0 <= x < width, 0 <= y < height)
     */
    fun isWithinBounds(x: Int, y: Int): Boolean {
        return x in 0 until bounds.width && y in 0 until bounds.height
    }

    /**
     * Checks if a position is within the safe bounds (excluding inset areas).
     *
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return true if position is within safe bounds
     */
    fun isWithinSafeBounds(x: Int, y: Int): Boolean {
        return x in bounds.safeInsetLeft until (bounds.width - bounds.safeInsetRight) &&
               y in bounds.safeInsetTop until (bounds.height - bounds.safeInsetBottom)
    }

    /**
     * Clamps a position to be within the full screen bounds.
     *
     * @param x X coordinate to clamp
     * @param y Y coordinate to clamp
     * @return Pair of clamped (x, y) coordinates within [0, width-1] and [0, height-1]
     */
    fun clamp(x: Int, y: Int): Pair<Int, Int> {
        return Pair(
            x.coerceIn(0, bounds.width - 1),
            y.coerceIn(0, bounds.height - 1)
        )
    }

    /**
     * Clamps a position to be within the safe bounds.
     *
     * @param x X coordinate to clamp
     * @param y Y coordinate to clamp
     * @return Pair of clamped (x, y) coordinates within safe area
     */
    fun clampToSafe(x: Int, y: Int): Pair<Int, Int> {
        return Pair(
            x.coerceIn(bounds.safeInsetLeft, bounds.width - bounds.safeInsetRight - 1),
            y.coerceIn(bounds.safeInsetTop, bounds.height - bounds.safeInsetBottom - 1)
        )
    }

    /**
     * Determines which edge a position is at, if any.
     *
     * Edge detection priority: LEFT > RIGHT > TOP > BOTTOM
     *
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return The Edge the position is at, or null if not at any edge
     */
    fun getEdge(x: Int, y: Int): Edge? {
        return when {
            x <= 0 -> Edge.LEFT
            x >= bounds.width - 1 -> Edge.RIGHT
            y <= 0 -> Edge.TOP
            y >= bounds.height - 1 -> Edge.BOTTOM
            else -> null
        }
    }

    /**
     * Represents the edges of the screen.
     */
    enum class Edge {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }
}
