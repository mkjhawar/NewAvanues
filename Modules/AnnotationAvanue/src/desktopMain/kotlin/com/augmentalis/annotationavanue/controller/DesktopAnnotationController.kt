package com.augmentalis.annotationavanue.controller

import com.augmentalis.annotationavanue.model.AnnotationState
import com.augmentalis.annotationavanue.model.AnnotationTool
import com.augmentalis.annotationavanue.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Desktop (JVM) implementation of [IAnnotationController].
 *
 * State management is identical to [AndroidAnnotationController] — MutableStateFlow
 * drives Compose reactivity on Desktop just as it does on Android.
 *
 * The only Desktop-specific addition is [save], which flattens all strokes onto a
 * [BufferedImage] via [java.awt.Graphics2D] and returns a PNG [ByteArray] via
 * [javax.imageio.ImageIO]. Rendering honours [AnnotationTool] variants:
 *
 * - PEN / HIGHLIGHTER: cubic Bezier path through all StrokePoints
 * - ERASER: cleared region (DST_OUT composite) matching the stroke path
 * - LINE: straight line from first to last point
 * - ARROW: LINE + filled arrowhead at the destination end
 * - RECTANGLE: outline rect bounded by first and last points
 * - CIRCLE: outline ellipse bounded by first and last points
 *
 * Canvas size for [save] defaults to 1920×1080 but can be supplied as parameters.
 *
 * Author: Manoj Jhawar
 */
class DesktopAnnotationController : IAnnotationController {

    private val _state = MutableStateFlow(AnnotationState())
    override val state: StateFlow<AnnotationState> = _state.asStateFlow()

    // -------------------------------------------------------------------------
    // Tool / color / width
    // -------------------------------------------------------------------------

    override fun selectTool(tool: AnnotationTool) {
        _state.update { it.copy(currentTool = tool, isErasing = tool == AnnotationTool.ERASER) }
    }

    override fun setColor(color: Long) {
        _state.update { it.copy(strokeColor = color) }
    }

    override fun setStrokeWidth(width: Float) {
        _state.update { it.copy(strokeWidth = width.coerceIn(1f, 40f)) }
    }

    // -------------------------------------------------------------------------
    // Stroke management
    // -------------------------------------------------------------------------

    override fun addStroke(stroke: Stroke) {
        _state.update { current ->
            current.copy(
                strokes = current.strokes + stroke,
                redoStack = emptyList()
            )
        }
    }

    override fun removeStroke(id: String) {
        _state.update { current ->
            val removed = current.strokes.find { it.id == id }
            current.copy(
                strokes = current.strokes.filter { it.id != id },
                undoStack = if (removed != null) current.undoStack + removed else current.undoStack
            )
        }
    }

    override fun undo() {
        _state.update { current ->
            if (current.strokes.isEmpty()) return@update current
            val last = current.strokes.last()
            current.copy(
                strokes = current.strokes.dropLast(1),
                redoStack = current.redoStack + last
            )
        }
    }

    override fun redo() {
        _state.update { current ->
            if (current.redoStack.isEmpty()) return@update current
            val last = current.redoStack.last()
            current.copy(
                strokes = current.strokes + last,
                redoStack = current.redoStack.dropLast(1)
            )
        }
    }

    override fun clear() {
        _state.update { AnnotationState() }
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    override fun toJson(): String = AnnotationSerializer.stateToJson(_state.value)

    override fun fromJson(json: String) {
        _state.value = AnnotationSerializer.stateFromJson(json)
    }

    /**
     * Restore strokes only — mirrors [AndroidAnnotationController.loadFromStrokesJson].
     * Used by Cockpit to reload a persisted Whiteboard frame.
     */
    fun loadFromStrokesJson(strokesJson: String) {
        val strokes = AnnotationSerializer.strokesFromJson(strokesJson)
        _state.update { it.copy(strokes = strokes) }
    }

    /**
     * Export current strokes as JSON — mirrors [AndroidAnnotationController.toStrokesJson].
     */
    fun toStrokesJson(): String = AnnotationSerializer.strokesToJson(_state.value.strokes)

    // -------------------------------------------------------------------------
    // Desktop-only: rasterize to PNG ByteArray
    // -------------------------------------------------------------------------

    /**
     * Render all current strokes to a [BufferedImage] of the given [canvasWidth] x
     * [canvasHeight] and encode as a PNG [ByteArray].
     *
     * Background is filled with [backgroundColor] (defaults to transparent black for
     * overlays, or opaque white for document export depending on call site).
     *
     * Returns an empty array if there are no strokes to render.
     */
    fun save(
        canvasWidth: Int = 1920,
        canvasHeight: Int = 1080,
        backgroundColor: Color = Color(0, 0, 0, 0)
    ): ByteArray {
        val strokes = _state.value.strokes
        if (strokes.isEmpty()) return ByteArray(0)

        val image = BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

            // Fill background
            g2d.color = backgroundColor
            g2d.fillRect(0, 0, canvasWidth, canvasHeight)

            for (stroke in strokes) {
                if (stroke.points.isEmpty()) continue
                val awtColor = argbLongToAwtColor(stroke.color, stroke.alpha)
                val awtStroke = BasicStroke(
                    stroke.width,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
                )
                g2d.stroke = awtStroke

                when (stroke.tool) {
                    AnnotationTool.ERASER -> {
                        // Erase by drawing in the background color with full opacity
                        g2d.color = Color(backgroundColor.red, backgroundColor.green, backgroundColor.blue, 255)
                        renderFreePath(g2d, stroke)
                    }

                    AnnotationTool.PEN -> {
                        g2d.color = awtColor
                        renderFreePath(g2d, stroke)
                    }

                    AnnotationTool.HIGHLIGHTER -> {
                        // Highlighter: semi-transparent, wider, flat caps
                        val hlColor = Color(awtColor.red, awtColor.green, awtColor.blue, 100)
                        g2d.color = hlColor
                        g2d.stroke = BasicStroke(
                            stroke.width * 2.5f,
                            BasicStroke.CAP_SQUARE,
                            BasicStroke.JOIN_MITER
                        )
                        renderFreePath(g2d, stroke)
                    }

                    AnnotationTool.LINE -> {
                        g2d.color = awtColor
                        val first = stroke.points.first()
                        val last = stroke.points.last()
                        g2d.draw(Line2D.Float(first.x, first.y, last.x, last.y))
                    }

                    AnnotationTool.ARROW -> {
                        g2d.color = awtColor
                        val first = stroke.points.first()
                        val last = stroke.points.last()
                        g2d.draw(Line2D.Float(first.x, first.y, last.x, last.y))
                        drawArrowhead(g2d, first.x, first.y, last.x, last.y, stroke.width)
                    }

                    AnnotationTool.RECTANGLE -> {
                        g2d.color = awtColor
                        val first = stroke.points.first()
                        val last = stroke.points.last()
                        val x = minOf(first.x, last.x)
                        val y = minOf(first.y, last.y)
                        val w = Math.abs(last.x - first.x)
                        val h = Math.abs(last.y - first.y)
                        g2d.draw(Rectangle2D.Float(x, y, w, h))
                    }

                    AnnotationTool.CIRCLE -> {
                        g2d.color = awtColor
                        val first = stroke.points.first()
                        val last = stroke.points.last()
                        val x = minOf(first.x, last.x)
                        val y = minOf(first.y, last.y)
                        val w = Math.abs(last.x - first.x)
                        val h = Math.abs(last.y - first.y)
                        g2d.draw(Ellipse2D.Float(x, y, w, h))
                    }
                }
            }
        } finally {
            g2d.dispose()
        }

        val out = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", out)
        return out.toByteArray()
    }

    // -------------------------------------------------------------------------
    // Private rendering helpers
    // -------------------------------------------------------------------------

    /**
     * Render a freehand path using a cubic Bezier approximation through all points.
     * For single-point strokes (dot), draws a small filled circle instead.
     */
    private fun renderFreePath(g2d: java.awt.Graphics2D, stroke: Stroke) {
        val points = stroke.points
        if (points.size == 1) {
            val r = stroke.width / 2f
            g2d.fill(Ellipse2D.Float(points[0].x - r, points[0].y - r, r * 2, r * 2))
            return
        }
        val path = java.awt.geom.GeneralPath()
        path.moveTo(points[0].x.toDouble(), points[0].y.toDouble())
        if (points.size == 2) {
            path.lineTo(points[1].x.toDouble(), points[1].y.toDouble())
        } else {
            // Catmull-Rom → cubic Bezier conversion for smooth curves
            for (i in 1 until points.size - 1) {
                val prev = points[i - 1]
                val curr = points[i]
                val next = points[i + 1]
                val cp1x = curr.x - (next.x - prev.x) / 6.0
                val cp1y = curr.y - (next.y - prev.y) / 6.0
                val cp2x = curr.x + (next.x - prev.x) / 6.0
                val cp2y = curr.y + (next.y - prev.y) / 6.0
                path.curveTo(cp1x, cp1y, cp2x, cp2y, curr.x.toDouble(), curr.y.toDouble())
            }
            path.lineTo(points.last().x.toDouble(), points.last().y.toDouble())
        }
        g2d.draw(path)
    }

    /**
     * Draw a filled arrowhead at (x2, y2) pointing away from (x1, y1).
     * Arrow size scales with [strokeWidth].
     */
    private fun drawArrowhead(
        g2d: java.awt.Graphics2D,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        strokeWidth: Float
    ) {
        val arrowLength = (strokeWidth * 4f).coerceAtLeast(12f)
        val arrowAngle = 0.45 // radians (~26°)
        val angle = Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val x3 = x2 - arrowLength * Math.cos(angle - arrowAngle)
        val y3 = y2 - arrowLength * Math.sin(angle - arrowAngle)
        val x4 = x2 - arrowLength * Math.cos(angle + arrowAngle)
        val y4 = y2 - arrowLength * Math.sin(angle + arrowAngle)
        val arrowHead = java.awt.Polygon(
            intArrayOf(x2.toInt(), x3.toInt(), x4.toInt()),
            intArrayOf(y2.toInt(), y3.toInt(), y4.toInt()),
            3
        )
        g2d.fill(arrowHead)
    }

    /**
     * Convert an ARGB packed Long (0xAARRGGBB) to a [java.awt.Color] with
     * the stroke alpha applied on top.
     */
    private fun argbLongToAwtColor(argb: Long, strokeAlpha: Float): Color {
        val a = ((argb ushr 24) and 0xFF).toInt()
        val r = ((argb ushr 16) and 0xFF).toInt()
        val g = ((argb ushr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        val effectiveAlpha = ((a / 255f) * strokeAlpha * 255).toInt().coerceIn(0, 255)
        return Color(r, g, b, effectiveAlpha)
    }
}
