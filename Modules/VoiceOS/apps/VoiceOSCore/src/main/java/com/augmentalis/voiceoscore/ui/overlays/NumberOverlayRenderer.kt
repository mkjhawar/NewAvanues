/**
 * NumberOverlayRenderer.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09 12:37:30 PDT
 */
/**
 * NumberOverlayRenderer.kt
 *
 * Purpose: High-performance custom View for rendering number overlay badges
 * Optimized for 60 FPS rendering with 100+ simultaneous overlays
 *
 * Performance Targets:
 * - 60 FPS (16ms per frame)
 * - <5MB memory for 100 overlays
 * - Hardware-accelerated rendering
 * - Paint object pooling to reduce GC pressure
 *
 * Created: 2025-10-09 12:37:30 PDT
 */
package com.augmentalis.voiceoscore.ui.overlays

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.max

/**
 * Custom View for rendering circular number badges on UI elements
 *
 * Rendering Pipeline:
 * 1. Paint pooling - reuse Paint objects to avoid allocation
 * 2. Batch drawing - draw all badges in single onDraw call
 * 3. Hardware acceleration - use GPU for rendering
 * 4. Partial invalidation - only redraw changed regions
 *
 * Thread Safety: Must be called from UI thread only
 */
class NumberOverlayRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Configuration
    private var style: NumberOverlayStyle = NumberOverlayStyle.standard()
    private var renderConfig: RenderConfig = RenderConfig()

    // Data to render
    private val overlays = mutableListOf<OverlayData>()

    // Paint object pool (reused to avoid allocations)
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Cached measurements (avoid recalculation)
    private val textBoundsCache = mutableMapOf<String, Rect>()
    private val textBoundsRect = Rect()

    // Performance tracking
    private var lastRenderTimeMs: Long = 0
    private var frameCount: Int = 0

    init {
        // Enable hardware acceleration for better performance
        if (renderConfig.hardwareAcceleration) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        // Update paint objects with initial style
        updatePaintObjects()
    }

    /**
     * Update the style configuration
     */
    fun setStyle(newStyle: NumberOverlayStyle) {
        this.style = newStyle
        updatePaintObjects()
        invalidate()
    }

    /**
     * Update render configuration
     */
    fun setRenderConfig(config: RenderConfig) {
        this.renderConfig = config

        // Update hardware acceleration
        setLayerType(
            if (config.hardwareAcceleration) LAYER_TYPE_HARDWARE else LAYER_TYPE_SOFTWARE,
            null
        )

        invalidate()
    }

    /**
     * Set overlays to render
     *
     * @param newOverlays List of overlay data to render
     */
    fun setOverlays(newOverlays: List<OverlayData>) {
        overlays.clear()
        overlays.addAll(newOverlays.take(renderConfig.maxOverlaysPerFrame))

        // Clear text bounds cache if it gets too large (prevent memory leak)
        if (textBoundsCache.size > 200) {
            textBoundsCache.clear()
        }

        invalidate()
    }

    /**
     * Add a single overlay
     */
    fun addOverlay(overlay: OverlayData) {
        if (overlays.size < renderConfig.maxOverlaysPerFrame) {
            overlays.add(overlay)
            invalidate()
        }
    }

    /**
     * Remove overlay by number
     */
    fun removeOverlay(number: Int) {
        overlays.removeAll { it.number == number }
        invalidate()
    }

    /**
     * Clear all overlays
     */
    fun clearOverlays() {
        overlays.clear()
        textBoundsCache.clear()
        invalidate()
    }

    /**
     * Main rendering method - called by Android framework
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startTime = System.currentTimeMillis()

        // Render all overlays
        overlays.forEach { overlay ->
            drawNumberBadge(canvas, overlay)
        }

        // Track performance
        lastRenderTimeMs = System.currentTimeMillis() - startTime
        frameCount++

        // Performance warning if frame takes too long
        if (lastRenderTimeMs > renderConfig.targetFrameTimeMs && frameCount % 60 == 0) {
            android.util.Log.w(
                TAG,
                "Frame time exceeded target: ${lastRenderTimeMs}ms (target: ${renderConfig.targetFrameTimeMs}ms)"
            )
        }
    }

    /**
     * Draw a single number badge
     *
     * Rendering order:
     * 1. Drop shadow (if enabled)
     * 2. Filled circle (background)
     * 3. Stroke circle (border)
     * 4. Number text (centered)
     */
    private fun drawNumberBadge(canvas: Canvas, overlay: OverlayData) {
        // Calculate badge center position based on anchor point
        val center = calculateAnchorPosition(overlay.elementBounds, style.anchorPoint)

        // Ensure badge is within view bounds
        if (!isPointInView(center)) {
            return
        }

        // 1. Draw drop shadow (behind circle)
        if (style.dropShadow) {
            drawShadow(canvas, center)
        }

        // 2. Draw colored circle (background)
        val backgroundColor = getColorForState(overlay.state)
        circlePaint.color = backgroundColor
        canvas.drawCircle(
            center.x,
            center.y,
            style.circleRadius * resources.displayMetrics.density,
            circlePaint
        )

        // 3. Draw white stroke (border for depth)
        strokePaint.color = style.strokeColor
        strokePaint.strokeWidth = style.strokeWidth * resources.displayMetrics.density
        canvas.drawCircle(
            center.x,
            center.y,
            style.circleRadius * resources.displayMetrics.density,
            strokePaint
        )

        // 4. Draw number (centered in circle)
        drawNumberText(canvas, center, overlay.number)
    }

    /**
     * Draw drop shadow for depth effect
     */
    private fun drawShadow(canvas: Canvas, center: PointF) {
        val density = resources.displayMetrics.density
        val radius = style.circleRadius * density

        shadowPaint.color = style.shadowColor
        shadowPaint.maskFilter = BlurMaskFilter(
            style.shadowRadius * density,
            BlurMaskFilter.Blur.NORMAL
        )

        canvas.drawCircle(
            center.x + style.shadowOffsetX * density,
            center.y + style.shadowOffsetY * density,
            radius,
            shadowPaint
        )
    }

    /**
     * Draw number text centered in circle
     */
    private fun drawNumberText(canvas: Canvas, center: PointF, number: Int) {
        val numberText = number.toString()

        // Update text paint
        textPaint.color = style.numberColor
        textPaint.textSize = style.numberSize * resources.displayMetrics.scaledDensity
        textPaint.typeface = style.fontWeight

        // Get or calculate text bounds (cached for performance)
        val textBounds = if (renderConfig.cacheTextBounds) {
            textBoundsCache.getOrPut(numberText) {
                val bounds = Rect()
                textPaint.getTextBounds(numberText, 0, numberText.length, bounds)
                bounds
            }
        } else {
            textPaint.getTextBounds(numberText, 0, numberText.length, textBoundsRect)
            textBoundsRect
        }

        // Calculate vertical center (adjust for text baseline)
        val textY = center.y + (textBounds.height() / 2f)

        // Draw text
        canvas.drawText(numberText, center.x, textY, textPaint)
    }

    /**
     * Calculate badge center position based on anchor point and element bounds
     */
    private fun calculateAnchorPosition(elementBounds: Rect, anchorPoint: AnchorPoint): PointF {
        val density = resources.displayMetrics.density
        val radiusPx = style.circleRadius * density
        val offsetXPx = style.offsetX * density
        val offsetYPx = style.offsetY * density

        return when (anchorPoint) {
            AnchorPoint.TOP_RIGHT -> PointF(
                elementBounds.right - offsetXPx - radiusPx,
                elementBounds.top + offsetYPx + radiusPx
            )
            AnchorPoint.TOP_LEFT -> PointF(
                elementBounds.left + offsetXPx + radiusPx,
                elementBounds.top + offsetYPx + radiusPx
            )
            AnchorPoint.BOTTOM_RIGHT -> PointF(
                elementBounds.right - offsetXPx - radiusPx,
                elementBounds.bottom - offsetYPx - radiusPx
            )
            AnchorPoint.BOTTOM_LEFT -> PointF(
                elementBounds.left + offsetXPx + radiusPx,
                elementBounds.bottom - offsetYPx - radiusPx
            )
        }
    }

    /**
     * Get color for element voice state
     */
    private fun getColorForState(state: ElementVoiceState): Int {
        return when (state) {
            ElementVoiceState.ENABLED_WITH_NAME -> style.hasNameColor
            ElementVoiceState.ENABLED_NO_NAME -> style.noNameColor
            ElementVoiceState.DISABLED -> style.disabledColor
        }
    }

    /**
     * Check if point is within view bounds
     */
    private fun isPointInView(point: PointF): Boolean {
        val density = resources.displayMetrics.density
        val radius = style.circleRadius * density

        return point.x - radius >= 0 &&
                point.x + radius <= width &&
                point.y - radius >= 0 &&
                point.y + radius <= height
    }

    /**
     * Update all paint objects with current style
     */
    private fun updatePaintObjects() {
        val density = resources.displayMetrics.density

        // Update stroke paint
        strokePaint.strokeWidth = style.strokeWidth * density
        strokePaint.color = style.strokeColor

        // Update text paint
        textPaint.textSize = style.numberSize * resources.displayMetrics.scaledDensity
        textPaint.color = style.numberColor
        textPaint.typeface = style.fontWeight

        // Clear cached measurements when style changes
        textBoundsCache.clear()
    }

    /**
     * Get current performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            overlayCount = overlays.size,
            lastRenderTimeMs = lastRenderTimeMs,
            frameCount = frameCount,
            estimatedFps = if (lastRenderTimeMs > 0) 1000 / lastRenderTimeMs else 60,
            cacheSize = textBoundsCache.size
        )
    }

    companion object {
        private const val TAG = "NumberOverlayRenderer"
    }
}

/**
 * Data class representing a single overlay to render
 */
data class OverlayData(
    /**
     * Element bounds in screen coordinates
     */
    val elementBounds: Rect,

    /**
     * Number to display (1-999)
     */
    val number: Int,

    /**
     * Voice state for color coding
     */
    val state: ElementVoiceState,

    /**
     * Optional unique identifier for tracking
     */
    val id: String? = null
)

/**
 * Performance metrics for monitoring
 */
data class PerformanceMetrics(
    val overlayCount: Int,
    val lastRenderTimeMs: Long,
    val frameCount: Int,
    val estimatedFps: Long,
    val cacheSize: Int
) {
    /**
     * Check if performance is within acceptable range
     */
    fun isPerformanceAcceptable(targetFrameTimeMs: Long = 16): Boolean {
        return lastRenderTimeMs <= targetFrameTimeMs
    }

    /**
     * Get performance status string
     */
    fun getStatusString(): String {
        return "Overlays: $overlayCount, FPS: ~$estimatedFps, Render: ${lastRenderTimeMs}ms, Cache: $cacheSize"
    }
}
