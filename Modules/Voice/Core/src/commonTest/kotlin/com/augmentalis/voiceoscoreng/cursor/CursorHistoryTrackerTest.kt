package com.augmentalis.voiceoscoreng.cursor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for CursorHistoryTracker - Phase 11 of VoiceOSCoreNG cursor system.
 *
 * Implements undo/redo history tracking for cursor positions with configurable
 * maximum history size and forward history clearing on new recordings.
 */
class CursorHistoryTrackerTest {

    // ========================================
    // Record Tests
    // ========================================

    @Test
    fun `record adds position to history`() {
        val tracker = CursorHistoryTracker()
        val position = CursorPosition(100, 200, 1000L)

        tracker.record(position)

        assertEquals(1, tracker.getHistorySize())
        assertEquals(position, tracker.getCurrent())
    }

    @Test
    fun `record adds multiple positions to history`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        assertEquals(3, tracker.getHistorySize())
        assertEquals(pos3, tracker.getCurrent())
    }

    @Test
    fun `record clears forward history when not at end`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        // Undo twice to go back to pos1
        tracker.undo() // now at pos2
        tracker.undo() // now at pos1

        // Record new position - should clear pos2 and pos3 from forward history
        val newPos = CursorPosition(300, 400, 4000L)
        tracker.record(newPos)

        assertEquals(2, tracker.getHistorySize())
        assertEquals(newPos, tracker.getCurrent())
        assertFalse(tracker.canRedo(), "Forward history should be cleared")
    }

    // ========================================
    // Max History Trimming Tests
    // ========================================

    @Test
    fun `record trims oldest entries when exceeding maxHistory`() {
        val maxHistory = 3
        val tracker = CursorHistoryTracker(maxHistory = maxHistory)

        val pos1 = CursorPosition(100, 100, 1000L)
        val pos2 = CursorPosition(200, 200, 2000L)
        val pos3 = CursorPosition(300, 300, 3000L)
        val pos4 = CursorPosition(400, 400, 4000L)
        val pos5 = CursorPosition(500, 500, 5000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        assertEquals(3, tracker.getHistorySize())

        tracker.record(pos4)

        assertEquals(3, tracker.getHistorySize())

        tracker.record(pos5)

        assertEquals(3, tracker.getHistorySize())

        // Oldest entries should be removed, so history should be [pos3, pos4, pos5]
        val history = tracker.getHistory()
        assertEquals(pos3, history[0])
        assertEquals(pos4, history[1])
        assertEquals(pos5, history[2])
    }

    @Test
    fun `record with default maxHistory of 50`() {
        val tracker = CursorHistoryTracker()

        // Add 60 positions
        repeat(60) { i ->
            tracker.record(CursorPosition(i, i, i.toLong()))
        }

        // Should be trimmed to 50
        assertEquals(50, tracker.getHistorySize())

        // Latest 50 positions should remain (indices 10-59)
        val history = tracker.getHistory()
        assertEquals(CursorPosition(10, 10, 10L), history.first())
        assertEquals(CursorPosition(59, 59, 59L), history.last())
    }

    // ========================================
    // canUndo Tests
    // ========================================

    @Test
    fun `canUndo returns false when history is empty`() {
        val tracker = CursorHistoryTracker()

        assertFalse(tracker.canUndo())
    }

    @Test
    fun `canUndo returns false when at start of history`() {
        val tracker = CursorHistoryTracker()
        val position = CursorPosition(100, 200, 1000L)

        tracker.record(position)

        // At start (index 0), cannot undo further
        assertFalse(tracker.canUndo())
    }

    @Test
    fun `canUndo returns true when has previous history`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)

        assertTrue(tracker.canUndo())
    }

    @Test
    fun `canUndo returns true after redo when still has history before`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        tracker.undo() // at pos2
        tracker.redo() // back at pos3

        assertTrue(tracker.canUndo())
    }

    // ========================================
    // undo Tests
    // ========================================

    @Test
    fun `undo returns null when cannot undo`() {
        val tracker = CursorHistoryTracker()

        assertNull(tracker.undo())
    }

    @Test
    fun `undo returns null when at start of history`() {
        val tracker = CursorHistoryTracker()
        val position = CursorPosition(100, 200, 1000L)

        tracker.record(position)

        assertNull(tracker.undo())
    }

    @Test
    fun `undo returns previous position`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)

        val undone = tracker.undo()

        assertEquals(pos1, undone)
        assertEquals(pos1, tracker.getCurrent())
    }

    @Test
    fun `undo can be called multiple times`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        assertEquals(pos2, tracker.undo())
        assertEquals(pos1, tracker.undo())
        assertNull(tracker.undo()) // Cannot undo past start
    }

    // ========================================
    // canRedo Tests
    // ========================================

    @Test
    fun `canRedo returns false when history is empty`() {
        val tracker = CursorHistoryTracker()

        assertFalse(tracker.canRedo())
    }

    @Test
    fun `canRedo returns false when at end of history`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)

        // At end, cannot redo
        assertFalse(tracker.canRedo())
    }

    @Test
    fun `canRedo returns true after undo`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.undo()

        assertTrue(tracker.canRedo())
    }

    @Test
    fun `canRedo returns false after undo then record`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.undo()
        tracker.record(CursorPosition(300, 400, 3000L))

        assertFalse(tracker.canRedo())
    }

    // ========================================
    // redo Tests
    // ========================================

    @Test
    fun `redo returns null when cannot redo`() {
        val tracker = CursorHistoryTracker()

        assertNull(tracker.redo())
    }

    @Test
    fun `redo returns null when at end of history`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)

        assertNull(tracker.redo())
    }

    @Test
    fun `redo returns next position after undo`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.undo()

        val redone = tracker.redo()

        assertEquals(pos2, redone)
        assertEquals(pos2, tracker.getCurrent())
    }

    @Test
    fun `redo can be called multiple times after multiple undos`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        tracker.undo() // at pos2
        tracker.undo() // at pos1

        assertEquals(pos2, tracker.redo())
        assertEquals(pos3, tracker.redo())
        assertNull(tracker.redo()) // Cannot redo past end
    }

    // ========================================
    // getCurrent Tests
    // ========================================

    @Test
    fun `getCurrent returns null when history is empty`() {
        val tracker = CursorHistoryTracker()

        assertNull(tracker.getCurrent())
    }

    @Test
    fun `getCurrent returns last recorded position`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)

        assertEquals(pos2, tracker.getCurrent())
    }

    @Test
    fun `getCurrent returns correct position after undo`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.undo()

        assertEquals(pos1, tracker.getCurrent())
    }

    @Test
    fun `getCurrent returns correct position after redo`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.undo()
        tracker.redo()

        assertEquals(pos2, tracker.getCurrent())
    }

    // ========================================
    // getHistory Tests
    // ========================================

    @Test
    fun `getHistory returns empty list when no records`() {
        val tracker = CursorHistoryTracker()

        assertTrue(tracker.getHistory().isEmpty())
    }

    @Test
    fun `getHistory returns immutable copy of history`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)

        tracker.record(pos1)

        val history = tracker.getHistory()

        assertEquals(1, history.size)
        assertEquals(pos1, history[0])

        // Verify it's a copy by checking that adding to internal history
        // doesn't affect the returned list
        tracker.record(CursorPosition(200, 300, 2000L))

        assertEquals(1, history.size) // Original should still have 1
        assertEquals(2, tracker.getHistory().size) // New call should have 2
    }

    @Test
    fun `getHistory returns all recorded positions in order`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        val history = tracker.getHistory()

        assertEquals(3, history.size)
        assertEquals(pos1, history[0])
        assertEquals(pos2, history[1])
        assertEquals(pos3, history[2])
    }

    // ========================================
    // getHistorySize Tests
    // ========================================

    @Test
    fun `getHistorySize returns 0 when empty`() {
        val tracker = CursorHistoryTracker()

        assertEquals(0, tracker.getHistorySize())
    }

    @Test
    fun `getHistorySize returns correct count`() {
        val tracker = CursorHistoryTracker()

        tracker.record(CursorPosition(100, 200, 1000L))
        assertEquals(1, tracker.getHistorySize())

        tracker.record(CursorPosition(150, 250, 2000L))
        assertEquals(2, tracker.getHistorySize())

        tracker.record(CursorPosition(200, 300, 3000L))
        assertEquals(3, tracker.getHistorySize())
    }

    @Test
    fun `getHistorySize unchanged by undo and redo`() {
        val tracker = CursorHistoryTracker()

        tracker.record(CursorPosition(100, 200, 1000L))
        tracker.record(CursorPosition(150, 250, 2000L))
        tracker.record(CursorPosition(200, 300, 3000L))

        assertEquals(3, tracker.getHistorySize())

        tracker.undo()
        assertEquals(3, tracker.getHistorySize())

        tracker.undo()
        assertEquals(3, tracker.getHistorySize())

        tracker.redo()
        assertEquals(3, tracker.getHistorySize())
    }

    // ========================================
    // clear Tests
    // ========================================

    @Test
    fun `clear resets history to empty`() {
        val tracker = CursorHistoryTracker()

        tracker.record(CursorPosition(100, 200, 1000L))
        tracker.record(CursorPosition(150, 250, 2000L))
        tracker.record(CursorPosition(200, 300, 3000L))

        assertEquals(3, tracker.getHistorySize())

        tracker.clear()

        assertEquals(0, tracker.getHistorySize())
        assertTrue(tracker.getHistory().isEmpty())
    }

    @Test
    fun `clear resets current position to null`() {
        val tracker = CursorHistoryTracker()

        tracker.record(CursorPosition(100, 200, 1000L))
        tracker.clear()

        assertNull(tracker.getCurrent())
    }

    @Test
    fun `clear resets undo and redo state`() {
        val tracker = CursorHistoryTracker()

        tracker.record(CursorPosition(100, 200, 1000L))
        tracker.record(CursorPosition(150, 250, 2000L))
        tracker.undo()

        assertTrue(tracker.canRedo())

        tracker.clear()

        assertFalse(tracker.canUndo())
        assertFalse(tracker.canRedo())
    }

    @Test
    fun `clear allows fresh recording after clearing`() {
        val tracker = CursorHistoryTracker()

        tracker.record(CursorPosition(100, 200, 1000L))
        tracker.record(CursorPosition(150, 250, 2000L))
        tracker.clear()

        val newPos = CursorPosition(300, 400, 3000L)
        tracker.record(newPos)

        assertEquals(1, tracker.getHistorySize())
        assertEquals(newPos, tracker.getCurrent())
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    fun `undo followed by redo returns to same state`() {
        val tracker = CursorHistoryTracker()
        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        tracker.undo()
        tracker.undo()
        tracker.redo()
        tracker.redo()

        assertEquals(pos3, tracker.getCurrent())
        assertEquals(3, tracker.getHistorySize())
    }

    @Test
    fun `maxHistory of 1 keeps only latest position`() {
        val tracker = CursorHistoryTracker(maxHistory = 1)

        val pos1 = CursorPosition(100, 200, 1000L)
        val pos2 = CursorPosition(150, 250, 2000L)
        val pos3 = CursorPosition(200, 300, 3000L)

        tracker.record(pos1)
        tracker.record(pos2)
        tracker.record(pos3)

        assertEquals(1, tracker.getHistorySize())
        assertEquals(pos3, tracker.getCurrent())
        assertFalse(tracker.canUndo())
    }

    @Test
    fun `recording same position multiple times is allowed`() {
        val tracker = CursorHistoryTracker()
        val position = CursorPosition(100, 200, 1000L)

        tracker.record(position)
        tracker.record(position)
        tracker.record(position)

        assertEquals(3, tracker.getHistorySize())
    }
}
