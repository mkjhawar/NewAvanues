package com.augmentalis.noteavanue

import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Snapshot-based undo/redo manager for compose-rich-editor [RichTextState].
 *
 * compose-rich-editor RC13 does not expose a native undo/redo API, so this
 * manager captures Markdown snapshots before each formatting operation and
 * restores them on undo/redo. Snapshots are deduplicated — calling
 * [captureSnapshot] with unchanged content is a no-op.
 *
 * The undo stack stores at most [maxHistory] snapshots (oldest dropped first).
 * Any new snapshot clears the redo stack (standard undo tree behavior).
 *
 * Usage:
 * ```
 * val undoManager = remember(richTextState) { RichTextUndoManager(richTextState) }
 * // Before formatting:
 * undoManager.captureSnapshot()
 * richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
 * // On undo command:
 * undoManager.undo()
 * ```
 */
class RichTextUndoManager(
    private val richTextState: RichTextState,
    private val maxHistory: Int = 50
) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    /**
     * Capture a Markdown snapshot of the current editor state.
     *
     * Duplicate consecutive snapshots are ignored (e.g. toggling bold twice
     * without typing in between). The redo stack is cleared on new snapshots
     * to maintain a linear undo history.
     */
    fun captureSnapshot() {
        val md = richTextState.toMarkdown()
        if (undoStack.lastOrNull() != md) {
            undoStack.addLast(md)
            if (undoStack.size > maxHistory) undoStack.removeFirst()
            redoStack.clear()
        }
    }

    /**
     * Undo the last formatting operation by restoring the previous snapshot.
     *
     * Requires at least 2 entries in the undo stack (current + previous).
     * The current state is pushed onto the redo stack.
     *
     * @return `true` if undo was performed, `false` if nothing to undo
     */
    fun undo(): Boolean {
        if (undoStack.size < 2) return false
        val current = undoStack.removeLast()
        redoStack.addLast(current)
        richTextState.setMarkdown(undoStack.last())
        return true
    }

    /**
     * Redo the last undone operation by restoring from the redo stack.
     *
     * The restored state is pushed back onto the undo stack.
     *
     * @return `true` if redo was performed, `false` if nothing to redo
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val snapshot = redoStack.removeLast()
        undoStack.addLast(snapshot)
        richTextState.setMarkdown(snapshot)
        return true
    }

    /** Whether there is a previous state to undo to (needs 2+ entries: current + previous). */
    val canUndo: Boolean get() = undoStack.size >= 2

    /** Whether there is a state to redo. */
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
