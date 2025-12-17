/**
 * NumberOverlayRenderer.kt - Renders circular number badges on UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.res.Resources
import android.graphics.*

/**
 * Renderer for number overlay badges
 *
 * Supports multiple badge styles (circle, square, rounded rect) with Material 3 colors
 */
class NumberOverlayRenderer(
    private val overlayStyle: NumberOverlayStyle,
    private val resources: Resources
) {

    // Paint objects (reused for performance)
    private val circlePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.WHITE
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val shadowPaint = Paint().apply {
        isAntiAlias = true
    }

    /**
     * Draw number badge on canvas at element position
     *
     * @param canvas Canvas to draw on
     * @param elementBounds Bounds of the UI element
     * @param number Number to display (1-999)
     * @param state Voice state of the element (determines color)
     */
    fun drawNumberBadge(
        canvas: Canvas,
        elementBounds: Rect,
        number: Int,
        state: ElementVoiceState
    ) {
        // Calculate badge center position
        val center = calculateAnchorPosition(elementBounds)

        // Draw drop shadow (if enabled)
        if (overlayStyle.dropShadow) {
            drawShadow(canvas, center)
        }

        // Draw badge background
        drawBadgeBackground(canvas, center, state)

        // Draw badge stroke/border
        if (overlayStyle.badgeStyle != BadgeStyle.OUTLINED_CIRCLE) {
            drawBadgeStroke(canvas, center)
        }

        // Draw number text
        drawNumberText(canvas, center, number)
    }

    /**
     * Calculate badge center position based on anchor point
     */
    private fun calculateAnchorPosition(elementBounds: Rect): PointF {
        val radius = overlayStyle.circleRadius.toFloat()

        return when (overlayStyle.anchorPoint) {
            AnchorPoint.TOP_RIGHT -> PointF(
                elementBounds.right - overlayStyle.offsetX - radius,
                elementBounds.top + overlayStyle.offsetY + radius
            )
            AnchorPoint.TOP_LEFT -> PointF(
                elementBounds.left + overlayStyle.offsetX + radius,
                elementBounds.top + overlayStyle.offsetY + radius
            )
            AnchorPoint.BOTTOM_RIGHT -> PointF(
                elementBounds.right - overlayStyle.offsetX - radius,
                elementBounds.bottom - overlayStyle.offsetY - radius
            )
            AnchorPoint.BOTTOM_LEFT -> PointF(
                elementBounds.left + overlayStyle.offsetX + radius,
                elementBounds.bottom - overlayStyle.offsetY - radius
            )
        }
    }

    /**
     * Draw drop shadow beneath badge
     */
    private fun drawShadow(canvas: Canvas, center: PointF) {
        shadowPaint.apply {
            color = overlayStyle.shadowColor
            maskFilter = BlurMaskFilter(overlayStyle.shadowRadius, BlurMaskFilter.Blur.NORMAL)
        }

        val shadowCenter = PointF(center.x, center.y + overlayStyle.shadowOffsetY)

        when (overlayStyle.badgeStyle) {
            BadgeStyle.FILLED_CIRCLE, BadgeStyle.OUTLINED_CIRCLE -> {
                canvas.drawCircle(
                    shadowCenter.x,
                    shadowCenter.y,
                    overlayStyle.circleRadius.toFloat(),
                    shadowPaint
                )
            }
            BadgeStyle.SQUARE -> {
                val rect = RectF(
                    shadowCenter.x - overlayStyle.circleRadius,
                    shadowCenter.y - overlayStyle.circleRadius,
                    shadowCenter.x + overlayStyle.circleRadius,
                    shadowCenter.y + overlayStyle.circleRadius
                )
                canvas.drawRect(rect, shadowPaint)
            }
            BadgeStyle.ROUNDED_RECT -> {
                val rect = RectF(
                    shadowCenter.x - overlayStyle.circleRadius,
                    shadowCenter.y - overlayStyle.circleRadius,
                    shadowCenter.x + overlayStyle.circleRadius,
                    shadowCenter.y + overlayStyle.circleRadius
                )
                canvas.drawRoundRect(rect, 8f, 8f, shadowPaint)
            }
        }
    }

    /**
     * Draw badge background (filled or outlined)
     */
    private fun drawBadgeBackground(canvas: Canvas, center: PointF, state: ElementVoiceState) {
        val color = when (state) {
            ElementVoiceState.ENABLED_WITH_NAME -> overlayStyle.hasNameColor
            ElementVoiceState.ENABLED_NO_NAME -> overlayStyle.noNameColor
            ElementVoiceState.DISABLED -> overlayStyle.disabledColor
        }

        circlePaint.color = color

        when (overlayStyle.badgeStyle) {
            BadgeStyle.FILLED_CIRCLE -> {
                canvas.drawCircle(center.x, center.y, overlayStyle.circleRadius.toFloat(), circlePaint)
            }
            BadgeStyle.OUTLINED_CIRCLE -> {
                strokePaint.apply {
                    this.color = color
                    strokeWidth = overlayStyle.strokeWidth
                }
                canvas.drawCircle(center.x, center.y, overlayStyle.circleRadius.toFloat(), strokePaint)
            }
            BadgeStyle.SQUARE -> {
                val rect = RectF(
                    center.x - overlayStyle.circleRadius,
                    center.y - overlayStyle.circleRadius,
                    center.x + overlayStyle.circleRadius,
                    center.y + overlayStyle.circleRadius
                )
                canvas.drawRect(rect, circlePaint)
            }
            BadgeStyle.ROUNDED_RECT -> {
                val rect = RectF(
                    center.x - overlayStyle.circleRadius,
                    center.y - overlayStyle.circleRadius,
                    center.x + overlayStyle.circleRadius,
                    center.y + overlayStyle.circleRadius
                )
                canvas.drawRoundRect(rect, 8f, 8f, circlePaint)
            }
        }
    }

    /**
     * Draw white stroke/border around badge
     */
    private fun drawBadgeStroke(canvas: Canvas, center: PointF) {
        strokePaint.apply {
            color = Color.WHITE
            strokeWidth = overlayStyle.strokeWidth
        }

        when (overlayStyle.badgeStyle) {
            BadgeStyle.FILLED_CIRCLE -> {
                canvas.drawCircle(center.x, center.y, overlayStyle.circleRadius.toFloat(), strokePaint)
            }
            BadgeStyle.SQUARE -> {
                val rect = RectF(
                    center.x - overlayStyle.circleRadius,
                    center.y - overlayStyle.circleRadius,
                    center.x + overlayStyle.circleRadius,
                    center.y + overlayStyle.circleRadius
                )
                canvas.drawRect(rect, strokePaint)
            }
            BadgeStyle.ROUNDED_RECT -> {
                val rect = RectF(
                    center.x - overlayStyle.circleRadius,
                    center.y - overlayStyle.circleRadius,
                    center.x + overlayStyle.circleRadius,
                    center.y + overlayStyle.circleRadius
                )
                canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
            }
            BadgeStyle.OUTLINED_CIRCLE -> {
                // Already drawn in drawBadgeBackground
            }
        }
    }

    /**
     * Draw number text centered in badge
     */
    private fun drawNumberText(canvas: Canvas, center: PointF, number: Int) {
        textPaint.apply {
            color = overlayStyle.numberColor
            textSize = overlayStyle.numberSize * resources.displayMetrics.scaledDensity
            typeface = overlayStyle.fontWeight
        }

        val numberText = number.toString()
        val textBounds = Rect()
        textPaint.getTextBounds(numberText, 0, numberText.length, textBounds)

        // Center vertically (adjust for text baseline)
        val textY = center.y + (textBounds.height() / 2f)

        canvas.drawText(numberText, center.x, textY, textPaint)
    }

    /**
     * Get touch bounds for hit testing
     */
    fun getTouchBounds(elementBounds: Rect): RectF {
        val center = calculateAnchorPosition(elementBounds)
        val touchRadius = overlayStyle.circleRadius + 8f  // Add 8px padding for easier touch

        return RectF(
            center.x - touchRadius,
            center.y - touchRadius,
            center.x + touchRadius,
            center.y + touchRadius
        )
    }

    /**
     * Check if point is within badge bounds
     */
    fun contains(elementBounds: Rect, x: Float, y: Float): Boolean {
        val touchBounds = getTouchBounds(elementBounds)
        return touchBounds.contains(x, y)
    }

    /**
     * Get estimated badge size (for layout calculations)
     */
    fun getBadgeSize(): Int {
        return overlayStyle.circleRadius * 2
    }

    /**
     * Companion object for utility functions
     */
    companion object {
        /**
         * Create default renderer
         */
        fun createDefault(resources: Resources): NumberOverlayRenderer {
            return NumberOverlayRenderer(NumberOverlayStyles.DEFAULT, resources)
        }

        /**
         * Create high contrast renderer
         */
        fun createHighContrast(resources: Resources): NumberOverlayRenderer {
            return NumberOverlayRenderer(NumberOverlayStyles.HIGH_CONTRAST, resources)
        }

        /**
         * Create large text renderer
         */
        fun createLargeText(resources: Resources): NumberOverlayRenderer {
            return NumberOverlayRenderer(NumberOverlayStyles.LARGE_TEXT, resources)
        }

        /**
         * Create renderer with custom style
         */
        fun create(style: NumberOverlayStyle, resources: Resources): NumberOverlayRenderer {
            return NumberOverlayRenderer(style, resources)
        }

        /**
         * Maximum number that can be displayed (for layout purposes)
         */
        const val MAX_DISPLAY_NUMBER = 999
    }
}
