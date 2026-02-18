package com.augmentalis.annotationavanue.controller

import com.augmentalis.annotationavanue.model.AnnotationState
import com.augmentalis.annotationavanue.model.AnnotationTool
import com.augmentalis.annotationavanue.model.Stroke
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for the annotation/whiteboard canvas.
 * Manages tool selection, stroke operations, undo/redo, and persistence.
 *
 * Architecture: Defined in commonMain, implemented per-platform in androidMain/desktopMain.
 * Android impl wraps MutableStateFlow; Desktop impl uses similar pattern.
 */
interface IAnnotationController {

    /** Observable annotation state. */
    val state: StateFlow<AnnotationState>

    /** Select a drawing tool. */
    fun selectTool(tool: AnnotationTool)

    /** Set the current stroke color (ARGB Long). */
    fun setColor(color: Long)

    /** Set the current stroke width in dp. */
    fun setStrokeWidth(width: Float)

    /** Add a completed stroke to the canvas. Clears redo stack. */
    fun addStroke(stroke: Stroke)

    /** Remove a stroke by its ID (for eraser). */
    fun removeStroke(id: String)

    /** Undo the last stroke. Moves it to redo stack. */
    fun undo()

    /** Redo the last undone stroke. */
    fun redo()

    /** Clear all strokes. */
    fun clear()

    /** Serialize current state to JSON for persistence. */
    fun toJson(): String

    /** Restore state from JSON. */
    fun fromJson(json: String)

    /** Increase pen width by 2dp. */
    fun penSizeUp() {
        val current = state.value.strokeWidth
        setStrokeWidth(minOf(current + 2f, 40f))
    }

    /** Decrease pen width by 2dp. */
    fun penSizeDown() {
        val current = state.value.strokeWidth
        setStrokeWidth(maxOf(current - 2f, 1f))
    }
}
