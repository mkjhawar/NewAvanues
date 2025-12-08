/**
 * LearnAppDebugOverlay.kt - Visual debug overlay for LearnApp exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Full-screen overlay that draws colored boxes around elements to visualize:
 * - VUID assignment status (has UUID or not)
 * - Learning source (LearnApp full vs JIT passive)
 * - Navigation links (where element leads to, where it came from)
 * - Element classification (safe, dangerous, login, etc.)
 *
 * ## Color Coding
 * - Green: LearnApp-learned element
 * - Blue: JIT-learned element
 * - Yellow: Has VUID but not linked yet
 * - Orange: Currently being explored
 * - Gray: Not yet learned
 * - Red: Dangerous element (skipped)
 *
 * ## Link Indicators
 * - ↗ Arrow: Links to another screen (downstream)
 * - ← Arrow: Linked from another screen (upstream)
 */
package com.augmentalis.voiceoscore.learnapp.debugging

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

/**
 * Debug overlay view that draws element highlights and legend
 *
 * @property state Current debug overlay state
 */
class LearnAppDebugOverlay(
    context: Context,
    private var state: DebugOverlayState
) : View(context) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 24f
        color = DebugColors.TEXT_WHITE
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }

    private val smallTextPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 18f
        color = DebugColors.TEXT_SECONDARY
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }

    private val legendPaint = Paint().apply {
        style = Paint.Style.FILL
        color = DebugColors.LEGEND_BG
        isAntiAlias = true
    }

    private val arrowPath = Path()

    /**
     * Update overlay state and redraw
     */
    fun updateState(newState: DebugOverlayState) {
        state = newState
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val screenState = state.currentScreen ?: return

        // Draw element highlights
        screenState.elements.forEach { element ->
            drawElementHighlight(canvas, element)
        }

        // Draw legend at bottom
        drawLegend(canvas, screenState)
    }

    /**
     * Draw highlight box around element with annotations
     */
    private fun drawElementHighlight(canvas: Canvas, element: DebugElementState) {
        val bounds = element.bounds
        if (bounds.isEmpty) return

        // Choose color based on learning source and state
        val color = getElementColor(element)
        boxPaint.color = color

        // Draw semi-transparent fill for better visibility
        fillPaint.color = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawRect(bounds, fillPaint)

        // Draw border
        canvas.drawRect(bounds, boxPaint)

        // Draw annotations based on verbosity
        when (state.verbosity) {
            DebugVerbosity.MINIMAL -> {
                // Just color, no text
            }
            DebugVerbosity.STANDARD -> {
                drawStandardAnnotations(canvas, element)
            }
            DebugVerbosity.VERBOSE -> {
                drawVerboseAnnotations(canvas, element)
            }
        }

        // Draw link indicators
        drawLinkIndicators(canvas, element)
    }

    /**
     * Get color for element based on state
     */
    private fun getElementColor(element: DebugElementState): Int {
        return when {
            element.isDangerous -> DebugColors.DANGEROUS_RED
            element.learningSource == LearningSource.EXPLORING -> DebugColors.EXPLORING_ORANGE
            element.learningSource == LearningSource.LEARNAPP -> DebugColors.LEARNAPP_GREEN
            element.learningSource == LearningSource.JIT -> DebugColors.JIT_BLUE
            element.vuid != null -> DebugColors.HAS_VUID_YELLOW
            else -> DebugColors.UNLEARNED_GRAY
        }
    }

    /**
     * Draw standard annotations (VUID truncated)
     */
    private fun drawStandardAnnotations(canvas: Canvas, element: DebugElementState) {
        val bounds = element.bounds
        val vuidText = element.vuid?.take(8) ?: "—"

        // Draw VUID at top-left inside bounds
        textPaint.textSize = 20f
        val textY = bounds.top + 24f
        val textX = bounds.left + 6f

        if (bounds.height() > 30) {
            canvas.drawText(vuidText, textX, textY, textPaint)
        }
    }

    /**
     * Draw verbose annotations (full info)
     */
    private fun drawVerboseAnnotations(canvas: Canvas, element: DebugElementState) {
        val bounds = element.bounds

        // Only draw if element is tall enough
        if (bounds.height() < 50) {
            drawStandardAnnotations(canvas, element)
            return
        }

        val textX = bounds.left + 6f
        var textY = bounds.top + 22f

        // Line 1: VUID
        textPaint.textSize = 18f
        val vuidText = element.vuid?.take(12) ?: "no VUID"
        canvas.drawText(vuidText, textX, textY, textPaint)
        textY += 18f

        // Line 2: Display name (truncated)
        if (bounds.height() > 70) {
            smallTextPaint.textSize = 14f
            val displayName = element.displayName.take(15)
            canvas.drawText(displayName, textX, textY, smallTextPaint)
            textY += 16f
        }

        // Line 3: Links (if space)
        if (bounds.height() > 90) {
            element.linksToScreen?.let { dest ->
                smallTextPaint.textSize = 12f
                canvas.drawText("↗ ${dest.take(8)}", textX, textY, smallTextPaint)
                textY += 14f
            }
            element.linkedFromScreen?.let { src ->
                smallTextPaint.textSize = 12f
                canvas.drawText("← ${src.take(8)}", textX, textY, smallTextPaint)
            }
        }
    }

    /**
     * Draw link indicators (arrows)
     */
    private fun drawLinkIndicators(canvas: Canvas, element: DebugElementState) {
        val bounds = element.bounds

        // Draw outgoing link arrow (↗) at top-right
        if (element.linksToScreen != null) {
            drawArrow(canvas, bounds.right - 20f, bounds.top + 10f, true, DebugColors.LINK_CYAN)
        }

        // Draw incoming link indicator (•) at bottom-left
        if (element.linkedFromScreen != null) {
            fillPaint.color = DebugColors.LINK_CYAN
            canvas.drawCircle(bounds.left + 8f, bounds.bottom - 8f, 4f, fillPaint)
        }
    }

    /**
     * Draw arrow indicator
     */
    private fun drawArrow(canvas: Canvas, x: Float, y: Float, outgoing: Boolean, color: Int) {
        arrowPath.reset()
        fillPaint.color = color

        if (outgoing) {
            // Outgoing arrow (↗)
            arrowPath.moveTo(x, y + 10f)
            arrowPath.lineTo(x + 10f, y)
            arrowPath.lineTo(x + 10f, y + 4f)
            arrowPath.lineTo(x + 4f, y + 4f)
            arrowPath.lineTo(x + 4f, y + 10f)
            arrowPath.close()
        } else {
            // Incoming arrow (←)
            arrowPath.moveTo(x + 10f, y + 5f)
            arrowPath.lineTo(x, y + 5f)
            arrowPath.lineTo(x + 4f, y)
            arrowPath.moveTo(x, y + 5f)
            arrowPath.lineTo(x + 4f, y + 10f)
        }

        canvas.drawPath(arrowPath, fillPaint)
    }

    /**
     * Draw legend at bottom of screen
     */
    private fun drawLegend(canvas: Canvas, screenState: DebugScreenState) {
        val legendHeight = if (state.verbosity == DebugVerbosity.VERBOSE) 180f else 120f
        val legendY = height - legendHeight - 100f // Account for nav bar
        val legendRect = RectF(16f, legendY, width - 16f, legendY + legendHeight)

        // Background
        canvas.drawRoundRect(legendRect, 12f, 12f, legendPaint)

        // Title
        textPaint.textSize = 20f
        textPaint.color = DebugColors.TEXT_WHITE
        canvas.drawText("DEBUG OVERLAY", 30f, legendY + 28f, textPaint)

        // Screen info
        smallTextPaint.textSize = 14f
        smallTextPaint.color = DebugColors.TEXT_SECONDARY
        val screenInfo = "Screen: ${screenState.screenHash.take(8)} | ${screenState.activityName.substringAfterLast('.')}"
        canvas.drawText(screenInfo, 30f, legendY + 48f, smallTextPaint)

        // Stats
        val statsText = "${screenState.learnedElements}/${screenState.totalElements} learned | " +
                "${screenState.exploredElements} explored | " +
                "${state.totalScreensExplored} screens"
        canvas.drawText(statsText, 30f, legendY + 66f, smallTextPaint)

        // Legend items
        val legendItems = listOf(
            Pair(DebugColors.LEARNAPP_GREEN, "LearnApp"),
            Pair(DebugColors.JIT_BLUE, "JIT"),
            Pair(DebugColors.HAS_VUID_YELLOW, "Has VUID"),
            Pair(DebugColors.EXPLORING_ORANGE, "Exploring"),
            Pair(DebugColors.UNLEARNED_GRAY, "Unlearned"),
            Pair(DebugColors.DANGEROUS_RED, "Dangerous")
        )

        val itemWidth = (legendRect.width() - 40f) / 3
        legendItems.forEachIndexed { index, (color, label) ->
            val row = index / 3
            val col = index % 3
            val x = 30f + col * itemWidth
            val y = legendY + 90f + row * 24f

            // Color box
            fillPaint.color = color
            canvas.drawRect(x, y - 12f, x + 16f, y + 4f, fillPaint)

            // Label
            smallTextPaint.textSize = 12f
            smallTextPaint.color = DebugColors.TEXT_WHITE
            canvas.drawText(label, x + 22f, y, smallTextPaint)
        }

        // Link indicators legend (verbose only)
        if (state.verbosity == DebugVerbosity.VERBOSE) {
            val linkY = legendY + 150f
            smallTextPaint.textSize = 12f
            canvas.drawText("↗ Links to screen", 30f, linkY, smallTextPaint)
            canvas.drawText("• Linked from screen", 180f, linkY, smallTextPaint)
        }
    }
}
