package com.augmentalis.annotationavanue.controller

import com.augmentalis.annotationavanue.model.AnnotationState
import com.augmentalis.annotationavanue.model.AnnotationTool
import com.augmentalis.annotationavanue.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android implementation of [IAnnotationController].
 * Manages annotation state via MutableStateFlow for Compose reactivity.
 *
 * Undo/redo uses stack-based approach: undo pops last stroke to redoStack,
 * redo pops from redoStack back to strokes. Adding a new stroke clears redoStack.
 */
class AndroidAnnotationController : IAnnotationController {

    private val _state = MutableStateFlow(AnnotationState())
    override val state: StateFlow<AnnotationState> = _state.asStateFlow()

    override fun selectTool(tool: AnnotationTool) {
        _state.update { it.copy(currentTool = tool, isErasing = tool == AnnotationTool.ERASER) }
    }

    override fun setColor(color: Long) {
        _state.update { it.copy(strokeColor = color) }
    }

    override fun setStrokeWidth(width: Float) {
        _state.update { it.copy(strokeWidth = width.coerceIn(1f, 40f)) }
    }

    override fun addStroke(stroke: Stroke) {
        _state.update { current ->
            current.copy(
                strokes = current.strokes + stroke,
                redoStack = emptyList() // Clear redo on new stroke
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

    override fun toJson(): String {
        return AnnotationSerializer.stateToJson(_state.value)
    }

    override fun fromJson(json: String) {
        _state.value = AnnotationSerializer.stateFromJson(json)
    }

    /**
     * Initialize from Cockpit's strokesJson field.
     * Used when loading a Whiteboard frame's persisted data.
     */
    fun loadFromStrokesJson(strokesJson: String) {
        val strokes = AnnotationSerializer.strokesFromJson(strokesJson)
        _state.update { it.copy(strokes = strokes) }
    }

    /**
     * Export current strokes as JSON for Cockpit persistence.
     */
    fun toStrokesJson(): String {
        return AnnotationSerializer.strokesToJson(_state.value.strokes)
    }
}
