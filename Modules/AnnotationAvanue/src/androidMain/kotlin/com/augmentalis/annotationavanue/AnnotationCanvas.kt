package com.augmentalis.annotationavanue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.augmentalis.annotationavanue.model.AnnotationTool
import com.augmentalis.annotationavanue.model.StrokePoint
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Full-featured annotation/whiteboard canvas with drawing tools, undo/redo,
 * and color selection. Renders strokes as paths on a Compose Canvas.
 *
 * @param initialStrokes Pre-existing strokes to render.
 * @param onStrokesChanged Called whenever the stroke list changes (for persistence).
 * @param modifier Layout modifier.
 */
@Composable
fun AnnotationCanvas(
    initialStrokes: List<com.augmentalis.annotationavanue.model.Stroke> = emptyList(),
    onStrokesChanged: (List<com.augmentalis.annotationavanue.model.Stroke>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var strokes by remember { mutableStateOf(initialStrokes.toMutableList()) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var undoneStrokes by remember { mutableStateOf<List<com.augmentalis.annotationavanue.model.Stroke>>(emptyList()) }
    var currentTool by remember { mutableStateOf(AnnotationTool.PEN) }
    var strokeWidth by remember { mutableStateOf(4f) }
    val strokeColor = colors.onBackground

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentTool = AnnotationTool.PEN }) {
                Icon(
                    Icons.Default.Create, "Pen",
                    tint = if (currentTool == AnnotationTool.PEN) colors.primary else colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { currentTool = AnnotationTool.HIGHLIGHTER }) {
                Icon(
                    Icons.Default.Create, "Highlighter",
                    tint = if (currentTool == AnnotationTool.HIGHLIGHTER) colors.tertiary else colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { currentTool = AnnotationTool.ERASER }) {
                Icon(
                    Icons.Default.Circle, "Eraser",
                    tint = if (currentTool == AnnotationTool.ERASER) colors.error else colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = {
                    if (strokes.isNotEmpty()) {
                        undoneStrokes = undoneStrokes + strokes.last()
                        strokes = strokes.dropLast(1).toMutableList()
                        onStrokesChanged(strokes)
                    }
                },
                enabled = strokes.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Undo, "Undo",
                    tint = if (strokes.isNotEmpty()) colors.onSurface else colors.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = {
                    if (undoneStrokes.isNotEmpty()) {
                        strokes = (strokes + undoneStrokes.last()).toMutableList()
                        undoneStrokes = undoneStrokes.dropLast(1)
                        onStrokesChanged(strokes)
                    }
                },
                enabled = undoneStrokes.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Redo, "Redo",
                    tint = if (undoneStrokes.isNotEmpty()) colors.onSurface else colors.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = {
                strokes = mutableListOf()
                undoneStrokes = emptyList()
                onStrokesChanged(strokes)
            }) {
                Icon(Icons.Default.Delete, "Clear", tint = colors.error, modifier = Modifier.size(20.dp))
            }
        }

        // Drawing canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.background)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentTool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(StrokePoint(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + StrokePoint(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                if (currentPoints.size >= 2) {
                                    if (currentTool == AnnotationTool.ERASER) {
                                        // Remove strokes that intersect with eraser path
                                        val eraserPoints = currentPoints
                                        strokes = strokes.filter { stroke ->
                                            !stroke.points.any { sp ->
                                                eraserPoints.any { ep ->
                                                    val dx = sp.x - ep.x
                                                    val dy = sp.y - ep.y
                                                    dx * dx + dy * dy < 400f // 20px radius
                                                }
                                            }
                                        }.toMutableList()
                                    } else {
                                        val newStroke = com.augmentalis.annotationavanue.model.Stroke(
                                            points = currentPoints,
                                            color = strokeColor.value.toLong(),
                                            width = if (currentTool == AnnotationTool.HIGHLIGHTER) strokeWidth * 3 else strokeWidth,
                                            tool = currentTool
                                        )
                                        strokes = (strokes + newStroke).toMutableList()
                                        undoneStrokes = emptyList()
                                    }
                                    onStrokesChanged(strokes)
                                }
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                // Render committed strokes
                strokes.forEach { stroke ->
                    if (stroke.points.size < 2) return@forEach
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                    }
                    val alpha = if (stroke.tool == AnnotationTool.HIGHLIGHTER) 0.4f else 1f
                    drawPath(
                        path = path,
                        color = Color(stroke.color.toULong()).copy(alpha = alpha),
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Render current in-progress stroke
                if (currentPoints.size >= 2) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        for (i in 1 until currentPoints.size) {
                            lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                    }
                    val isHighlighter = currentTool == AnnotationTool.HIGHLIGHTER
                    val isEraser = currentTool == AnnotationTool.ERASER
                    drawPath(
                        path = path,
                        color = when {
                            isEraser -> Color(strokeColor.value).copy(alpha = 0.3f)
                            isHighlighter -> Color(strokeColor.value).copy(alpha = 0.4f)
                            else -> Color(strokeColor.value)
                        },
                        style = Stroke(
                            width = when {
                                isEraser -> 20f
                                isHighlighter -> strokeWidth * 3
                                else -> strokeWidth
                            },
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
