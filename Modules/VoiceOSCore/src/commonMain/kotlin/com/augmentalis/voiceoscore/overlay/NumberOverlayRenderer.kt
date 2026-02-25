/**
 * NumberOverlayRenderer.kt - Renders circular number badges on UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 *
 * KMP-compatible renderer for number overlay badges.
 * Provides platform-agnostic logic for:
 * - Badge position calculation based on anchor points
 * - Color selection based on element state
 * - Touch bounds calculation for hit testing
 * - Rendering parameter generation
 *
 * Platform-specific rendering implementations use the RenderingParams
 * returned by this class to draw badges using native graphics APIs.
 *
 * Ported from VoiceOSCore legacy implementation:
 * - Removed Android-specific Canvas/Paint dependencies
 * - Added RenderingParams for platform-agnostic rendering
 * - Kept calculation logic identical for visual consistency
 */
package com.augmentalis.voiceoscore

/**
 * 2D point with floating-point coordinates.
 *
 * Used for badge center positioning.
 *
 * @property x X coordinate
 * @property y Y coordinate
 */
data class PointF(
    val x: Float,
    val y: Float
)

/**
 * Rectangle with floating-point coordinates.
 *
 * Used for touch bounds and shape rendering.
 *
 * @property left X coordinate of left edge
 * @property top Y coordinate of top edge
 * @property right X coordinate of right edge
 * @property bottom Y coordinate of bottom edge
 */
data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    /**
     * Width of the rectangle.
     */
    val width: Float get() = right - left

    /**
     * Height of the rectangle.
     */
    val height: Float get() = bottom - top

    /**
     * Check if a point is inside this rectangle.
     *
     * @param x X coordinate of point
     * @param y Y coordinate of point
     * @return true if point is inside bounds
     */
    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x < right && y >= top && y < bottom
    }

    companion object {
        /**
         * Create RectF centered on a point with given radius.
         */
        fun fromCenter(centerX: Float, centerY: Float, radius: Float): RectF {
            return RectF(
                left = centerX - radius,
                top = centerY - radius,
                right = centerX + radius,
                bottom = centerY + radius
            )
        }
    }
}

/**
 * Parameters for rendering a number badge.
 *
 * Contains all information needed by platform-specific renderers to draw
 * a badge. This decouples calculation logic from rendering implementation.
 *
 * @property number The number to display
 * @property numberText String representation of the number
 * @property center Badge center point
 * @property radius Badge radius in pixels
 * @property shape Badge shape (circle, square, etc.)
 * @property accessibilityShape Shape for colorblind accessibility (null if not using shape accessibility)
 * @property badgeColor Background color for filled styles, stroke color for outlined
 * @property textColor Color for the number text
 * @property textSize Text size in sp
 * @property fontWeight Font weight (100-900)
 * @property hasStroke Whether to draw white stroke around badge
 * @property strokeWidth Width of stroke in pixels
 * @property strokeColor Color of the stroke
 * @property hasShadow Whether to draw drop shadow
 * @property shadowColor Shadow color
 * @property shadowRadius Shadow blur radius
 * @property shadowOffsetY Vertical shadow offset
 * @property cornerRadius Corner radius for rounded rect shape
 */
data class RenderingParams(
    val number: Int,
    val numberText: String,
    val center: PointF,
    val radius: Float,
    val shape: BadgeShape,
    val accessibilityShape: AccessibilityShape?,
    val badgeColor: Long,
    val textColor: Long,
    val textSize: Float,
    val fontWeight: Int,
    val hasStroke: Boolean,
    val strokeWidth: Float,
    val strokeColor: Long,
    val hasShadow: Boolean,
    val shadowColor: Long,
    val shadowRadius: Float,
    val shadowOffsetY: Float,
    val cornerRadius: Float
)

/**
 * Renderer for number overlay badges.
 *
 * Calculates positions, colors, and rendering parameters for number badges
 * displayed over UI elements. Platform-specific implementations use these
 * parameters to draw badges using native graphics APIs.
 *
 * ## Usage
 *
 * ```kotlin
 * val renderer = NumberOverlayRenderer(NumberOverlayStyles.DEFAULT)
 *
 * // Get rendering params for a single item
 * val item = NumberedItem(1, "Submit", bounds, isEnabled = true, hasName = true)
 * val params = renderer.getRenderingParams(item)
 *
 * // Use params for platform-specific rendering
 * canvas.drawCircle(params.center.x, params.center.y, params.radius, paint)
 * ```
 *
 * ## Batch Rendering
 *
 * ```kotlin
 * val items = listOf(item1, item2, item3)
 * val paramsList = renderer.getBatchRenderingParams(items)
 *
 * paramsList.forEach { params ->
 *     drawBadge(params)
 * }
 * ```
 *
 * @param style Style configuration for the badges
 */
class NumberOverlayRenderer(
    private val style: NumberOverlayStyle
) {

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Position Calculation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate badge center position based on anchor point and element bounds.
     *
     * The badge is positioned at one of the four corners of the element,
     * with offsets applied to position it inside or outside the element.
     *
     * @param elementBounds Bounds of the UI element
     * @return Center point for the badge
     */
    fun calculateBadgeCenter(elementBounds: Rect): PointF {
        val radius = style.circleRadius

        return when (style.anchorPoint) {
            AnchorPoint.TOP_RIGHT -> PointF(
                x = elementBounds.right - style.offsetX - radius,
                y = elementBounds.top + style.offsetY + radius
            )
            AnchorPoint.TOP_LEFT -> PointF(
                x = elementBounds.left + style.offsetX + radius,
                y = elementBounds.top + style.offsetY + radius
            )
            AnchorPoint.BOTTOM_RIGHT -> PointF(
                x = elementBounds.right - style.offsetX - radius,
                y = elementBounds.bottom - style.offsetY - radius
            )
            AnchorPoint.BOTTOM_LEFT -> PointF(
                x = elementBounds.left + style.offsetX + radius,
                y = elementBounds.bottom - style.offsetY - radius
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Color Selection
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select badge color based on element state.
     *
     * Priority:
     * 1. Disabled elements always use disabledColor
     * 2. Enabled elements with names use hasNameColor
     * 3. Enabled elements without names use noNameColor
     *
     * @param item Numbered item with state information
     * @return Color value in 0xAARRGGBB format
     */
    fun selectBadgeColor(item: NumberedItem): Long {
        return when {
            !item.isEnabled -> style.disabledColor
            item.hasName -> style.hasNameColor
            else -> style.noNameColor
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Accessibility Shape Selection
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get accessibility shape for colorblind differentiation based on element state.
     *
     * Provides dual encoding (color + shape) for users with color vision deficiency (CVD).
     * Shape selection follows this logic:
     *
     * | State                          | Shape   | Rationale                              |
     * |--------------------------------|---------|----------------------------------------|
     * | isEnabled=true, hasName=true   | CIRCLE  | Primary actionable items (named)       |
     * | isEnabled=true, hasName=false  | SQUARE  | Actionable but requires number to select |
     * | isEnabled=false                | DIAMOND | Visually distinct "unavailable" state  |
     *
     * ## Accessibility Rationale
     *
     * The circle-square-diamond progression provides distinct visual differentiation:
     * - **CIRCLE**: Smooth, continuous shape signals "ready to use" with voice name
     * - **SQUARE**: Angular but stable shape signals "usable" but needs number
     * - **DIAMOND**: Rotated/tilted shape signals "different" or "unavailable"
     *
     * This pattern is consistent with common accessibility guidelines that recommend
     * using multiple visual channels (color, shape, pattern) to convey information.
     *
     * @param item Numbered item with state information
     * @return AccessibilityShape based on item state, or null if shape accessibility is disabled
     * @see AccessibilityShape
     * @see NumberOverlayStyle.useShapeAccessibility
     */
    fun getAccessibilityShape(item: NumberedItem): AccessibilityShape? {
        if (!style.useShapeAccessibility) {
            return null
        }

        return when {
            !item.isEnabled -> AccessibilityShape.DIAMOND
            item.hasName -> AccessibilityShape.CIRCLE
            else -> AccessibilityShape.SQUARE
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Touch Bounds
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get touch bounds for hit testing.
     *
     * Touch bounds are expanded by TOUCH_PADDING to make badges easier to tap.
     *
     * @param elementBounds Bounds of the UI element
     * @return Rectangle representing the touchable area
     */
    fun getTouchBounds(elementBounds: Rect): RectF {
        val center = calculateBadgeCenter(elementBounds)
        val touchRadius = style.circleRadius + TOUCH_PADDING

        return RectF.fromCenter(center.x, center.y, touchRadius)
    }

    /**
     * Check if a point is within the badge touch bounds.
     *
     * @param elementBounds Bounds of the UI element
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @return true if the point is within touch bounds
     */
    fun contains(elementBounds: Rect, x: Float, y: Float): Boolean {
        val touchBounds = getTouchBounds(elementBounds)
        return touchBounds.contains(x, y)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Size
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get badge diameter for layout calculations.
     *
     * @return Badge diameter in pixels
     */
    fun getBadgeSize(): Int {
        return (style.circleRadius * 2).toInt()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rendering Parameters
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all rendering parameters for a numbered item.
     *
     * Returns a platform-agnostic data structure containing all information
     * needed to draw the badge using native graphics APIs.
     *
     * When [NumberOverlayStyle.useShapeAccessibility] is enabled, the
     * [RenderingParams.accessibilityShape] field will contain the shape
     * to use for colorblind-friendly rendering. Platform renderers should
     * use this shape instead of [RenderingParams.shape] when it is non-null.
     *
     * @param item Numbered item to render
     * @return Rendering parameters for the badge
     */
    fun getRenderingParams(item: NumberedItem): RenderingParams {
        val center = calculateBadgeCenter(item.bounds)
        val badgeColor = selectBadgeColor(item)
        val accessibilityShape = getAccessibilityShape(item)

        // Outlined style has no separate stroke (the outline IS the badge)
        val hasStroke = style.badgeStyle != BadgeShape.OUTLINED_CIRCLE

        // Corner radius for rounded rectangles
        val cornerRadius = if (style.badgeStyle == BadgeShape.ROUNDED_RECT) {
            DEFAULT_CORNER_RADIUS
        } else {
            0f
        }

        return RenderingParams(
            number = item.number,
            numberText = item.number.toString(),
            center = center,
            radius = style.circleRadius,
            shape = style.badgeStyle,
            accessibilityShape = accessibilityShape,
            badgeColor = badgeColor,
            textColor = style.numberColor,
            textSize = style.numberSize,
            fontWeight = style.fontWeight,
            hasStroke = hasStroke,
            strokeWidth = style.strokeWidth,
            strokeColor = STROKE_COLOR,
            hasShadow = style.dropShadow,
            shadowColor = style.shadowColor,
            shadowRadius = style.shadowRadius,
            shadowOffsetY = style.shadowOffsetY,
            cornerRadius = cornerRadius
        )
    }

    /**
     * Get rendering parameters for multiple items.
     *
     * Use this for batch rendering to avoid repeated calculations.
     *
     * @param items List of numbered items to render
     * @return List of rendering parameters, one per item
     */
    fun getBatchRenderingParams(items: List<NumberedItem>): List<RenderingParams> {
        return items.map { getRenderingParams(it) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Companion Object
    // ═══════════════════════════════════════════════════════════════════════════

    companion object {
        /**
         * Padding added to touch bounds for easier interaction.
         */
        const val TOUCH_PADDING = 8f

        /**
         * Default corner radius for rounded rectangle badges.
         */
        const val DEFAULT_CORNER_RADIUS = 8f

        /**
         * Default stroke color (white).
         */
        const val STROKE_COLOR = 0xFFFFFFFF

        /**
         * Maximum number that can be displayed (for layout purposes).
         */
        const val MAX_DISPLAY_NUMBER = 999

        /**
         * Create renderer with default style.
         */
        fun createDefault(): NumberOverlayRenderer {
            return NumberOverlayRenderer(NumberOverlayStyles.DEFAULT)
        }

        /**
         * Create renderer with high contrast style.
         */
        fun createHighContrast(): NumberOverlayRenderer {
            return NumberOverlayRenderer(NumberOverlayStyles.HIGH_CONTRAST)
        }

        /**
         * Create renderer with large text style.
         */
        fun createLargeText(): NumberOverlayRenderer {
            return NumberOverlayRenderer(NumberOverlayStyles.LARGE_TEXT)
        }

        /**
         * Create renderer with shape-based accessibility for colorblind users.
         *
         * Uses [AccessibilityShape] differentiation where badge shapes vary
         * based on element state, providing dual encoding (color + shape).
         *
         * @see AccessibilityShape
         * @see NumberOverlayStyles.SHAPE_ACCESSIBLE
         */
        fun createShapeAccessible(): NumberOverlayRenderer {
            return NumberOverlayRenderer(NumberOverlayStyles.SHAPE_ACCESSIBLE)
        }
    }
}
