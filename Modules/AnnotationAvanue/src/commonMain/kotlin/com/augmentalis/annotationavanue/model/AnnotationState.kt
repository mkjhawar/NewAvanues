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
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

@Serializable
data class Stroke(
    val points: List<StrokePoint>,
    val color: Long,
    val width: Float,
    val tool: AnnotationTool
)

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

enum class AnnotationTool {
    PEN,
    HIGHLIGHTER,
    ERASER,
    ARROW,
    RECTANGLE,
    CIRCLE,
    TEXT
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
