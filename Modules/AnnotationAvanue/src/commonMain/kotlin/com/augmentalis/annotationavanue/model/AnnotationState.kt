package com.augmentalis.annotationavanue.model

import kotlinx.serialization.Serializable

/**
 * State for the annotation/whiteboard canvas.
 * Tracks strokes, current tool, color, and undo/redo history.
 */
@Serializable
data class AnnotationState(
    val strokes: List<Stroke> = emptyList(),
    val currentTool: AnnotationTool = AnnotationTool.PEN,
    val strokeColor: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 4f,
    val isErasing: Boolean = false,
    val undoStack: List<Stroke> = emptyList(),
    val redoStack: List<Stroke> = emptyList()
) {
    val canUndo: Boolean get() = strokes.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}

/**
 * A single drawing stroke composed of points, color, width, and tool type.
 * Raw points are stored — Bezier smoothing is applied at render time only.
 */
@Serializable
data class Stroke(
    val id: String = "",
    val points: List<StrokePoint>,
    val color: Long,
    val width: Float,
    val tool: AnnotationTool,
    val alpha: Float = 1f
)

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

/**
 * Available annotation tools.
 * Shapes (RECTANGLE, CIRCLE, ARROW, LINE) use first+last points as bounding box.
 * Freehand tools (PEN, HIGHLIGHTER) use all points for path rendering.
 */
@Serializable
enum class AnnotationTool {
    PEN,
    HIGHLIGHTER,
    ERASER,
    ARROW,
    RECTANGLE,
    CIRCLE,
    LINE
}

/** Preset annotation colors. */
object AnnotationColors {
    val BLACK = 0xFF000000L
    val WHITE = 0xFFFFFFFF
    val RED = 0xFFE53935L
    val BLUE = 0xFF1E88E5L
    val GREEN = 0xFF43A047L
    val YELLOW = 0xFFFDD835L
    val ORANGE = 0xFFFB8C00L
    val PURPLE = 0xFF8E24AAL
    val PINK = 0xFFEC407AL
    val BROWN = 0xFF6D4C41L
    val GRAY = 0xFF757575L
    val CYAN = 0xFF00ACC1L

    val PRESETS: List<Long> = listOf(
        BLACK, WHITE, RED, BLUE, GREEN, YELLOW,
        ORANGE, PURPLE, PINK, BROWN, GRAY, CYAN
    )
}

/**
 * State for signature capture mode — simplified annotation
 * focused on pen-only strokes with completion callback.
 */
@Serializable
data class SignatureState(
    val strokes: List<Stroke> = emptyList(),
    val strokeWidth: Float = 3f,
    val strokeColor: Long = 0xFFFFFFFF,
    val isComplete: Boolean = false
)
