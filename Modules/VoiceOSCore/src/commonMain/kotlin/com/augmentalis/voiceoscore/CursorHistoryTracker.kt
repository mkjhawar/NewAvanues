package com.augmentalis.voiceoscore

/**
 * Tracks cursor position history with undo/redo support.
 *
 * This class is part of the VoiceOSCoreNG cursor system (Phase 11).
 * It maintains a history of cursor positions allowing users to navigate
 * back and forth through their cursor movement history.
 *
 * Features:
 * - Configurable maximum history size with automatic trimming
 * - Undo/redo navigation through position history
 * - Forward history clearing when recording new positions after undo
 *
 * @property maxHistory The maximum number of positions to keep in history.
 *                      Oldest entries are removed when this limit is exceeded.
 *                      Defaults to 50.
 */
class CursorHistoryTracker(
    private val maxHistory: Int = 50
) {
    /**
     * Internal mutable list storing the position history.
     * Positions are stored in chronological order (oldest first).
     */
    private val history = mutableListOf<CursorPosition>()

    /**
     * Index pointing to the current position in history.
     * -1 indicates empty history, otherwise points to valid index in history.
     */
    private var currentIndex: Int = -1

    /**
     * Records a new cursor position to the history.
     *
     * If the current index is not at the end of history (i.e., after undo operations),
     * any forward history is cleared before adding the new position.
     *
     * If the history exceeds maxHistory after recording, the oldest entries
     * are removed to maintain the limit.
     *
     * @param position The cursor position to record
     */
    fun record(position: CursorPosition) {
        // Remove any forward history if we're not at the end
        if (currentIndex < history.lastIndex) {
            history.subList(currentIndex + 1, history.size).clear()
        }

        // Add new position
        history.add(position)

        // Trim if exceeds max
        while (history.size > maxHistory) {
            history.removeAt(0)
        }

        currentIndex = history.lastIndex
    }

    /**
     * Checks if undo is available.
     *
     * @return true if there is at least one previous position in history
     *         that can be navigated to, false otherwise
     */
    fun canUndo(): Boolean = currentIndex > 0

    /**
     * Checks if redo is available.
     *
     * @return true if there is at least one forward position in history
     *         that can be navigated to, false otherwise
     */
    fun canRedo(): Boolean = currentIndex < history.lastIndex

    /**
     * Navigates to the previous position in history.
     *
     * @return The previous CursorPosition if available, null otherwise
     */
    fun undo(): CursorPosition? {
        if (!canUndo()) return null
        currentIndex--
        return history[currentIndex]
    }

    /**
     * Navigates to the next position in history.
     *
     * @return The next CursorPosition if available, null otherwise
     */
    fun redo(): CursorPosition? {
        if (!canRedo()) return null
        currentIndex++
        return history[currentIndex]
    }

    /**
     * Returns the current position in history.
     *
     * @return The current CursorPosition, or null if history is empty
     */
    fun getCurrent(): CursorPosition? {
        return if (currentIndex >= 0 && currentIndex <= history.lastIndex) {
            history[currentIndex]
        } else null
    }

    /**
     * Returns an immutable copy of the entire position history.
     *
     * The returned list is a snapshot and will not reflect future changes
     * to the tracker's internal history.
     *
     * @return A list of all CursorPositions in history, in chronological order
     */
    fun getHistory(): List<CursorPosition> = history.toList()

    /**
     * Returns the current size of the history.
     *
     * Note: This reflects the total number of positions stored, not affected
     * by the current undo/redo position.
     *
     * @return The number of positions in history
     */
    fun getHistorySize(): Int = history.size

    /**
     * Clears all history and resets the tracker to initial state.
     *
     * After calling clear(), canUndo() and canRedo() will return false,
     * and getCurrent() will return null.
     */
    fun clear() {
        history.clear()
        currentIndex = -1
    }
}
