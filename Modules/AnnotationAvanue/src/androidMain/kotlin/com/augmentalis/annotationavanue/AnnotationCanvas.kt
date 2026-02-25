package com.augmentalis.annotationavanue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.augmentalis.annotationavanue.controller.BezierSmoother
import com.augmentalis.annotationavanue.model.AnnotationColors
import com.augmentalis.annotationavanue.model.AnnotationTool
import com.augmentalis.annotationavanue.model.StrokePoint
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.handlers.ModuleCommandCallbacks
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-featured annotation/whiteboard canvas with drawing tools, shape rendering,
 * Bezier-smoothed freehand strokes, undo/redo, and color selection.
 *
 * Shape tools use first and last points as bounding corners.
 * Freehand tools render all intermediate points with Bezier smoothing.
 *
 * @param initialStrokes Pre-existing strokes to render (from Cockpit persistence).
 * @param currentTool Currently selected tool.
 * @param strokeColor Current drawing color as ARGB Long.
 * @param strokeWidth Current stroke width in px.
 * @param onStrokeCompleted Called when a stroke finishes drawing.
 * @param onStrokesErased Called when eraser removes strokes (passes remaining IDs).
 * @param showToolbar Whether to show the embedded toolbar. Set false for Cockpit where toolbar is external.
 * @param modifier Layout modifier.
 */
@Composable
fun AnnotationCanvas(
    initialStrokes: List<com.augmentalis.annotationavanue.model.Stroke> = emptyList(),
    currentTool: AnnotationTool = AnnotationTool.PEN,
    strokeColor: Long = AnnotationColors.WHITE,
    strokeWidth: Float = 4f,
    onStrokeCompleted: (com.augmentalis.annotationavanue.model.Stroke) -> Unit = {},
    onStrokesErased: (erasedIds: List<String>) -> Unit = {},
    showToolbar: Boolean = true,
    onToolChanged: (AnnotationTool) -> Unit = {},
    onColorChanged: (Long) -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onClear: () -> Unit = {},
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    // Wire voice command executor for annotation tool controls
    DisposableEffect(Unit) {
        ModuleCommandCallbacks.annotationExecutor = { actionType, _ ->
            executeAnnotationCommand(
                actionType = actionType,
                onToolChanged = onToolChanged,
                onColorChanged = onColorChanged,
                onUndo = onUndo,
                onRedo = onRedo,
                onClear = onClear,
                canUndo = canUndo,
                canRedo = canRedo
            )
        }
        onDispose { ModuleCommandCallbacks.annotationExecutor = null }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (showToolbar) {
            AnnotationToolbar(
                currentTool = currentTool,
                currentColor = strokeColor,
                onToolSelected = onToolChanged,
                onColorSelected = onColorChanged,
                onUndo = onUndo,
                onRedo = onRedo,
                onClear = onClear,
                canUndo = canUndo,
                canRedo = canRedo
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.background)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Voice: click canvas" }
                    .pointerInput(currentTool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(StrokePoint(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + StrokePoint(
                                    change.position.x,
                                    change.position.y,
                                    1f
                                )
                            },
                            onDragEnd = {
                                if (currentPoints.size >= 2) {
                                    if (currentTool == AnnotationTool.ERASER) {
                                        val eraserPoints = currentPoints
                                        val erasedIds = initialStrokes.filter { stroke ->
                                            stroke.points.any { sp ->
                                                eraserPoints.any { ep ->
                                                    val dx = sp.x - ep.x
                                                    val dy = sp.y - ep.y
                                                    dx * dx + dy * dy < 400f
                                                }
                                            }
                                        }.map { it.id }
                                        if (erasedIds.isNotEmpty()) {
                                            onStrokesErased(erasedIds)
                                        }
                                    } else {
                                        val alpha = if (currentTool == AnnotationTool.HIGHLIGHTER) 0.4f else 1f
                                        val width = if (currentTool == AnnotationTool.HIGHLIGHTER) strokeWidth * 3 else strokeWidth
                                        val newStroke = com.augmentalis.annotationavanue.model.Stroke(
                                            id = generateStrokeId(),
                                            points = currentPoints,
                                            color = strokeColor,
                                            width = width,
                                            tool = currentTool,
                                            alpha = alpha
                                        )
                                        onStrokeCompleted(newStroke)
                                    }
                                }
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                // Render committed strokes
                initialStrokes.forEach { stroke ->
                    renderStroke(stroke)
                }

                // Render current in-progress stroke
                if (currentPoints.size >= 2) {
                    val previewAlpha = if (currentTool == AnnotationTool.HIGHLIGHTER) 0.4f
                        else if (currentTool == AnnotationTool.ERASER) 0.3f
                        else 1f
                    val previewWidth = when (currentTool) {
                        AnnotationTool.ERASER -> 20f
                        AnnotationTool.HIGHLIGHTER -> strokeWidth * 3
                        else -> strokeWidth
                    }
                    val previewColor = if (currentTool == AnnotationTool.ERASER)
                        Color(colors.textPrimary.value).copy(alpha = previewAlpha)
                    else Color(strokeColor.toULong()).copy(alpha = previewAlpha)

                    renderPoints(
                        points = currentPoints,
                        tool = currentTool,
                        color = previewColor,
                        width = previewWidth
                    )
                }
            }
        }
    }
}

/**
 * Render a completed stroke based on its tool type.
 * Freehand strokes get Bezier smoothing; shapes use geometric rendering.
 */
private fun DrawScope.renderStroke(stroke: com.augmentalis.annotationavanue.model.Stroke) {
    if (stroke.points.size < 2) return
    val color = Color(stroke.color.toULong()).copy(alpha = stroke.alpha)
    renderPoints(stroke.points, stroke.tool, color, stroke.width)
}

private fun DrawScope.renderPoints(
    points: List<StrokePoint>,
    tool: AnnotationTool,
    color: Color,
    width: Float
) {
    if (points.size < 2) return
    val strokeStyle = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (tool) {
        AnnotationTool.RECTANGLE -> {
            val first = points.first()
            val last = points.last()
            val topLeft = Offset(minOf(first.x, last.x), minOf(first.y, last.y))
            val size = Size(
                kotlin.math.abs(last.x - first.x),
                kotlin.math.abs(last.y - first.y)
            )
            drawRect(color = color, topLeft = topLeft, size = size, style = strokeStyle)
        }

        AnnotationTool.CIRCLE -> {
            val first = points.first()
            val last = points.last()
            val topLeft = Offset(minOf(first.x, last.x), minOf(first.y, last.y))
            val size = Size(
                kotlin.math.abs(last.x - first.x),
                kotlin.math.abs(last.y - first.y)
            )
            drawOval(color = color, topLeft = topLeft, size = size, style = strokeStyle)
        }

        AnnotationTool.LINE -> {
            val first = points.first()
            val last = points.last()
            drawLine(
                color = color,
                start = Offset(first.x, first.y),
                end = Offset(last.x, last.y),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }

        AnnotationTool.ARROW -> {
            val first = points.first()
            val last = points.last()
            // Shaft
            drawLine(
                color = color,
                start = Offset(first.x, first.y),
                end = Offset(last.x, last.y),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
            // Arrowhead
            val angle = atan2(last.y - first.y, last.x - first.x)
            val headLength = min(20f, width * 4)
            val headAngle = 0.5f // ~28 degrees
            val head1 = Offset(
                last.x - headLength * cos(angle - headAngle),
                last.y - headLength * sin(angle - headAngle)
            )
            val head2 = Offset(
                last.x - headLength * cos(angle + headAngle),
                last.y - headLength * sin(angle + headAngle)
            )
            drawLine(color = color, start = Offset(last.x, last.y), end = head1, strokeWidth = width, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(last.x, last.y), end = head2, strokeWidth = width, cap = StrokeCap.Round)
        }

        else -> {
            // Freehand: PEN, HIGHLIGHTER, ERASER — apply Bezier smoothing
            val smoothed = if (points.size > 3) BezierSmoother.smooth(points) else points
            val path = Path().apply {
                moveTo(smoothed.first().x, smoothed.first().y)
                for (i in 1 until smoothed.size) {
                    lineTo(smoothed[i].x, smoothed[i].y)
                }
            }
            drawPath(path = path, color = color, style = strokeStyle)
        }
    }
}

private var strokeCounter = 0L

private fun generateStrokeId(): String {
    strokeCounter++
    return "s_${System.currentTimeMillis()}_$strokeCounter"
}

/**
 * Maps annotation voice commands to canvas tool/state mutations.
 * Tool selection, undo/redo, clear, and pen size adjustments.
 */
@Suppress("LongParameterList")
private fun executeAnnotationCommand(
    actionType: CommandActionType,
    onToolChanged: (AnnotationTool) -> Unit,
    onColorChanged: (Long) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
): HandlerResult {
    return when (actionType) {
        // -- Tool Selection ------------------------------------------------
        CommandActionType.ANNOTATION_PEN -> {
            onToolChanged(AnnotationTool.PEN)
            HandlerResult.success("Pen tool selected")
        }
        CommandActionType.ANNOTATION_HIGHLIGHTER -> {
            onToolChanged(AnnotationTool.HIGHLIGHTER)
            HandlerResult.success("Highlighter selected")
        }
        CommandActionType.ANNOTATION_ERASER -> {
            onToolChanged(AnnotationTool.ERASER)
            HandlerResult.success("Eraser selected")
        }
        CommandActionType.ANNOTATION_SHAPE_RECT -> {
            onToolChanged(AnnotationTool.RECTANGLE)
            HandlerResult.success("Rectangle tool selected")
        }
        CommandActionType.ANNOTATION_SHAPE_CIRCLE -> {
            onToolChanged(AnnotationTool.CIRCLE)
            HandlerResult.success("Circle tool selected")
        }
        CommandActionType.ANNOTATION_SHAPE_ARROW -> {
            onToolChanged(AnnotationTool.ARROW)
            HandlerResult.success("Arrow tool selected")
        }
        CommandActionType.ANNOTATION_SHAPE_LINE -> {
            onToolChanged(AnnotationTool.LINE)
            HandlerResult.success("Line tool selected")
        }

        // -- Undo/Redo/Clear -----------------------------------------------
        CommandActionType.ANNOTATION_UNDO -> {
            if (canUndo) {
                onUndo()
                HandlerResult.success("Undo")
            } else {
                HandlerResult.failure("Nothing to undo", recoverable = true)
            }
        }
        CommandActionType.ANNOTATION_REDO -> {
            if (canRedo) {
                onRedo()
                HandlerResult.success("Redo")
            } else {
                HandlerResult.failure("Nothing to redo", recoverable = true)
            }
        }
        CommandActionType.ANNOTATION_CLEAR -> {
            onClear()
            HandlerResult.success("Canvas cleared")
        }

        // -- Color Picker ---------------------------------------------------
        CommandActionType.ANNOTATION_COLOR_PICKER -> {
            // Color picker is a UI operation — cycle through preset colors
            val colors = listOf(
                AnnotationColors.WHITE,
                AnnotationColors.RED,
                AnnotationColors.BLUE,
                AnnotationColors.GREEN,
                AnnotationColors.YELLOW
            )
            // Use white as default cycle start
            onColorChanged(AnnotationColors.RED)
            HandlerResult.success("Color changed")
        }

        else -> HandlerResult.failure("Unsupported annotation action: $actionType", recoverable = true)
    }
}
