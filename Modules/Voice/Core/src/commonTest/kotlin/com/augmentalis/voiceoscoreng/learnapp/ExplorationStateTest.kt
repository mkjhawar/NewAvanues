package com.augmentalis.voiceoscoreng.learnapp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for ExplorationState enum and ExplorationSession data class.
 *
 * ExplorationState represents the lifecycle states of a LearnApp exploration session.
 * ExplorationSession tracks the session context, progress, and state transitions.
 *
 * Phase 14: LearnApp System - Exploration State Management
 */
class ExplorationStateTest {

    // ==================== ExplorationState Enum Values Tests ====================

    @Test
    fun `IDLE state should exist`() {
        val state = ExplorationState.IDLE
        assertNotNull(state)
        assertEquals("IDLE", state.name)
    }

    @Test
    fun `INITIALIZING state should exist`() {
        val state = ExplorationState.INITIALIZING
        assertNotNull(state)
        assertEquals("INITIALIZING", state.name)
    }

    @Test
    fun `SCANNING state should exist`() {
        val state = ExplorationState.SCANNING
        assertNotNull(state)
        assertEquals("SCANNING", state.name)
    }

    @Test
    fun `PROCESSING state should exist`() {
        val state = ExplorationState.PROCESSING
        assertNotNull(state)
        assertEquals("PROCESSING", state.name)
    }

    @Test
    fun `LEARNING state should exist`() {
        val state = ExplorationState.LEARNING
        assertNotNull(state)
        assertEquals("LEARNING", state.name)
    }

    @Test
    fun `PAUSED state should exist`() {
        val state = ExplorationState.PAUSED
        assertNotNull(state)
        assertEquals("PAUSED", state.name)
    }

    @Test
    fun `COMPLETING state should exist`() {
        val state = ExplorationState.COMPLETING
        assertNotNull(state)
        assertEquals("COMPLETING", state.name)
    }

    @Test
    fun `COMPLETED state should exist`() {
        val state = ExplorationState.COMPLETED
        assertNotNull(state)
        assertEquals("COMPLETED", state.name)
    }

    @Test
    fun `ERROR state should exist`() {
        val state = ExplorationState.ERROR
        assertNotNull(state)
        assertEquals("ERROR", state.name)
    }

    @Test
    fun `should have exactly nine enum values`() {
        val entries = ExplorationState.entries
        assertEquals(9, entries.size)
    }

    @Test
    fun `entries should maintain declaration order`() {
        val entries = ExplorationState.entries.toList()
        assertEquals(ExplorationState.IDLE, entries[0])
        assertEquals(ExplorationState.INITIALIZING, entries[1])
        assertEquals(ExplorationState.SCANNING, entries[2])
        assertEquals(ExplorationState.PROCESSING, entries[3])
        assertEquals(ExplorationState.LEARNING, entries[4])
        assertEquals(ExplorationState.PAUSED, entries[5])
        assertEquals(ExplorationState.COMPLETING, entries[6])
        assertEquals(ExplorationState.COMPLETED, entries[7])
        assertEquals(ExplorationState.ERROR, entries[8])
    }

    // ==================== ExplorationSession Creation Tests ====================

    @Test
    fun `ExplorationSession should be created with default values`() {
        val session = ExplorationSession(
            sessionId = "test_session_1",
            packageName = "com.example.app"
        )

        assertEquals("test_session_1", session.sessionId)
        assertEquals("com.example.app", session.packageName)
        assertEquals(ExplorationState.IDLE, session.state)
        assertEquals(0, session.elementsScanned)
        assertEquals(0, session.elementsLearned)
        assertNull(session.errorMessage)
        assertNull(session.endTime)
        assertTrue(session.startTime > 0)
    }

    @Test
    fun `ExplorationSession should be created with custom startTime`() {
        val customTime = 1000000L
        val session = ExplorationSession(
            sessionId = "test_session_2",
            packageName = "com.example.app",
            startTime = customTime
        )

        assertEquals(customTime, session.startTime)
    }

    @Test
    fun `ExplorationSession should be created with custom state`() {
        val session = ExplorationSession(
            sessionId = "test_session_3",
            packageName = "com.example.app",
            state = ExplorationState.SCANNING
        )

        assertEquals(ExplorationState.SCANNING, session.state)
    }

    // ==================== generateSessionId Tests ====================

    @Test
    fun `generateSessionId should create non-empty string`() {
        val sessionId = ExplorationSession.generateSessionId()
        assertTrue(sessionId.isNotEmpty())
    }

    @Test
    fun `generateSessionId should start with session prefix`() {
        val sessionId = ExplorationSession.generateSessionId()
        assertTrue(sessionId.startsWith("session_"))
    }

    @Test
    fun `generateSessionId should create unique IDs`() {
        val id1 = ExplorationSession.generateSessionId()
        val id2 = ExplorationSession.generateSessionId()
        // Due to random component, IDs should typically be different
        // Note: There's a small probability they could be the same if called at exact same ms
        // We test multiple generations to ensure uniqueness
        val ids = (1..10).map { ExplorationSession.generateSessionId() }.toSet()
        assertTrue(ids.size > 1, "Generated session IDs should be unique")
    }

    // ==================== State Transition Tests - Valid Transitions ====================

    @Test
    fun `transition from IDLE to INITIALIZING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example"
        )

        assertTrue(session.transition(ExplorationState.INITIALIZING))
        assertEquals(ExplorationState.INITIALIZING, session.state)
    }

    @Test
    fun `transition from INITIALIZING to SCANNING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.INITIALIZING
        )

        assertTrue(session.transition(ExplorationState.SCANNING))
        assertEquals(ExplorationState.SCANNING, session.state)
    }

    @Test
    fun `transition from INITIALIZING to ERROR should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.INITIALIZING
        )

        assertTrue(session.transition(ExplorationState.ERROR))
        assertEquals(ExplorationState.ERROR, session.state)
    }

    @Test
    fun `transition from SCANNING to PROCESSING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        assertTrue(session.transition(ExplorationState.PROCESSING))
        assertEquals(ExplorationState.PROCESSING, session.state)
    }

    @Test
    fun `transition from SCANNING to PAUSED should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        assertTrue(session.transition(ExplorationState.PAUSED))
        assertEquals(ExplorationState.PAUSED, session.state)
    }

    @Test
    fun `transition from SCANNING to ERROR should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        assertTrue(session.transition(ExplorationState.ERROR))
        assertEquals(ExplorationState.ERROR, session.state)
    }

    @Test
    fun `transition from PROCESSING to LEARNING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PROCESSING
        )

        assertTrue(session.transition(ExplorationState.LEARNING))
        assertEquals(ExplorationState.LEARNING, session.state)
    }

    @Test
    fun `transition from PROCESSING to ERROR should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PROCESSING
        )

        assertTrue(session.transition(ExplorationState.ERROR))
        assertEquals(ExplorationState.ERROR, session.state)
    }

    @Test
    fun `transition from LEARNING to SCANNING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.LEARNING
        )

        assertTrue(session.transition(ExplorationState.SCANNING))
        assertEquals(ExplorationState.SCANNING, session.state)
    }

    @Test
    fun `transition from LEARNING to COMPLETING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.LEARNING
        )

        assertTrue(session.transition(ExplorationState.COMPLETING))
        assertEquals(ExplorationState.COMPLETING, session.state)
    }

    @Test
    fun `transition from LEARNING to ERROR should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.LEARNING
        )

        assertTrue(session.transition(ExplorationState.ERROR))
        assertEquals(ExplorationState.ERROR, session.state)
    }

    @Test
    fun `transition from PAUSED to SCANNING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PAUSED
        )

        assertTrue(session.transition(ExplorationState.SCANNING))
        assertEquals(ExplorationState.SCANNING, session.state)
    }

    @Test
    fun `transition from PAUSED to COMPLETING should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PAUSED
        )

        assertTrue(session.transition(ExplorationState.COMPLETING))
        assertEquals(ExplorationState.COMPLETING, session.state)
    }

    @Test
    fun `transition from COMPLETING to COMPLETED should succeed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETING
        )

        assertTrue(session.transition(ExplorationState.COMPLETED))
        assertEquals(ExplorationState.COMPLETED, session.state)
    }

    @Test
    fun `transition from COMPLETED to IDLE should succeed for restart`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETED
        )

        assertTrue(session.transition(ExplorationState.IDLE))
        assertEquals(ExplorationState.IDLE, session.state)
    }

    @Test
    fun `transition from ERROR to IDLE should succeed for recovery`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.ERROR
        )

        assertTrue(session.transition(ExplorationState.IDLE))
        assertEquals(ExplorationState.IDLE, session.state)
    }

    // ==================== State Transition Tests - Invalid Transitions ====================

    @Test
    fun `transition from IDLE to SCANNING should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example"
        )

        assertFalse(session.transition(ExplorationState.SCANNING))
        assertEquals(ExplorationState.IDLE, session.state)
    }

    @Test
    fun `transition from IDLE to COMPLETED should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example"
        )

        assertFalse(session.transition(ExplorationState.COMPLETED))
        assertEquals(ExplorationState.IDLE, session.state)
    }

    @Test
    fun `transition from INITIALIZING to LEARNING should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.INITIALIZING
        )

        assertFalse(session.transition(ExplorationState.LEARNING))
        assertEquals(ExplorationState.INITIALIZING, session.state)
    }

    @Test
    fun `transition from SCANNING to COMPLETED should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        assertFalse(session.transition(ExplorationState.COMPLETED))
        assertEquals(ExplorationState.SCANNING, session.state)
    }

    @Test
    fun `transition from PAUSED to LEARNING should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PAUSED
        )

        assertFalse(session.transition(ExplorationState.LEARNING))
        assertEquals(ExplorationState.PAUSED, session.state)
    }

    @Test
    fun `transition from COMPLETING to SCANNING should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETING
        )

        assertFalse(session.transition(ExplorationState.SCANNING))
        assertEquals(ExplorationState.COMPLETING, session.state)
    }

    @Test
    fun `transition from COMPLETED to SCANNING should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETED
        )

        assertFalse(session.transition(ExplorationState.SCANNING))
        assertEquals(ExplorationState.COMPLETED, session.state)
    }

    @Test
    fun `transition from ERROR to SCANNING should fail`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.ERROR
        )

        assertFalse(session.transition(ExplorationState.SCANNING))
        assertEquals(ExplorationState.ERROR, session.state)
    }

    // ==================== isActive Tests ====================

    @Test
    fun `isActive should return false for IDLE`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.IDLE
        )

        assertFalse(session.isActive())
    }

    @Test
    fun `isActive should return true for INITIALIZING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.INITIALIZING
        )

        assertTrue(session.isActive())
    }

    @Test
    fun `isActive should return true for SCANNING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        assertTrue(session.isActive())
    }

    @Test
    fun `isActive should return true for PROCESSING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PROCESSING
        )

        assertTrue(session.isActive())
    }

    @Test
    fun `isActive should return true for LEARNING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.LEARNING
        )

        assertTrue(session.isActive())
    }

    @Test
    fun `isActive should return false for PAUSED`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PAUSED
        )

        assertFalse(session.isActive())
    }

    @Test
    fun `isActive should return false for COMPLETING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETING
        )

        assertFalse(session.isActive())
    }

    @Test
    fun `isActive should return false for COMPLETED`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETED
        )

        assertFalse(session.isActive())
    }

    @Test
    fun `isActive should return false for ERROR`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.ERROR
        )

        assertFalse(session.isActive())
    }

    // ==================== canPause Tests ====================

    @Test
    fun `canPause should return false for IDLE`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.IDLE
        )

        assertFalse(session.canPause())
    }

    @Test
    fun `canPause should return false for INITIALIZING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.INITIALIZING
        )

        assertFalse(session.canPause())
    }

    @Test
    fun `canPause should return true for SCANNING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        assertTrue(session.canPause())
    }

    @Test
    fun `canPause should return false for PROCESSING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PROCESSING
        )

        assertFalse(session.canPause())
    }

    @Test
    fun `canPause should return false for LEARNING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.LEARNING
        )

        assertFalse(session.canPause())
    }

    @Test
    fun `canPause should return false for PAUSED`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.PAUSED
        )

        assertFalse(session.canPause())
    }

    @Test
    fun `canPause should return false for COMPLETING`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETING
        )

        assertFalse(session.canPause())
    }

    @Test
    fun `canPause should return false for COMPLETED`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.COMPLETED
        )

        assertFalse(session.canPause())
    }

    @Test
    fun `canPause should return false for ERROR`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.ERROR
        )

        assertFalse(session.canPause())
    }

    // ==================== getDuration Tests ====================

    @Test
    fun `getDuration should return null when not completed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            startTime = 1000L
        )

        assertNull(session.getDuration())
    }

    @Test
    fun `getDuration should return correct duration when completed`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            startTime = 1000L,
            endTime = 5000L
        )

        assertEquals(4000L, session.getDuration())
    }

    @Test
    fun `getDuration should return correct duration after transition to COMPLETED`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            startTime = 1000L,
            state = ExplorationState.COMPLETING
        )

        // Before transition
        assertNull(session.getDuration())

        // After transition to COMPLETED
        session.transition(ExplorationState.COMPLETED)

        // endTime should be set automatically
        assertNotNull(session.endTime)
        assertNotNull(session.getDuration())
    }

    @Test
    fun `getDuration should return correct duration after transition to ERROR`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            startTime = 1000L,
            state = ExplorationState.SCANNING
        )

        // Before transition
        assertNull(session.getDuration())

        // After transition to ERROR
        session.transition(ExplorationState.ERROR)

        // endTime should be set automatically
        assertNotNull(session.endTime)
        assertNotNull(session.getDuration())
    }

    // ==================== incrementScanned and incrementLearned Tests ====================

    @Test
    fun `incrementScanned should increase elementsScanned by 1`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example"
        )

        assertEquals(0, session.elementsScanned)

        session.incrementScanned()
        assertEquals(1, session.elementsScanned)

        session.incrementScanned()
        assertEquals(2, session.elementsScanned)
    }

    @Test
    fun `incrementLearned should increase elementsLearned by 1`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example"
        )

        assertEquals(0, session.elementsLearned)

        session.incrementLearned()
        assertEquals(1, session.elementsLearned)

        session.incrementLearned()
        assertEquals(2, session.elementsLearned)
    }

    @Test
    fun `incrementScanned and incrementLearned should be independent`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example"
        )

        session.incrementScanned()
        session.incrementScanned()
        session.incrementScanned()
        session.incrementLearned()

        assertEquals(3, session.elementsScanned)
        assertEquals(1, session.elementsLearned)
    }

    // ==================== setError Tests ====================

    @Test
    fun `setError should set errorMessage`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        session.setError("Test error message")

        assertEquals("Test error message", session.errorMessage)
    }

    @Test
    fun `setError should transition to ERROR state`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        session.setError("Test error message")

        assertEquals(ExplorationState.ERROR, session.state)
    }

    @Test
    fun `setError should set endTime`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        assertNull(session.endTime)

        session.setError("Test error message")

        assertNotNull(session.endTime)
    }

    @Test
    fun `setError from state without ERROR transition should still set errorMessage`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.IDLE
        )

        // IDLE cannot transition to ERROR directly
        session.setError("Test error message")

        // errorMessage should still be set
        assertEquals("Test error message", session.errorMessage)
        // But state should remain IDLE since transition is invalid
        assertEquals(ExplorationState.IDLE, session.state)
    }

    // ==================== Full Lifecycle Tests ====================

    @Test
    fun `full exploration lifecycle should work correctly`() {
        val session = ExplorationSession(
            sessionId = ExplorationSession.generateSessionId(),
            packageName = "com.example.app"
        )

        // Initial state
        assertEquals(ExplorationState.IDLE, session.state)
        assertFalse(session.isActive())

        // Start exploration
        assertTrue(session.transition(ExplorationState.INITIALIZING))
        assertTrue(session.isActive())

        // Begin scanning
        assertTrue(session.transition(ExplorationState.SCANNING))
        assertTrue(session.isActive())
        assertTrue(session.canPause())

        // Scan some elements
        session.incrementScanned()
        session.incrementScanned()
        assertEquals(2, session.elementsScanned)

        // Process scanned elements
        assertTrue(session.transition(ExplorationState.PROCESSING))
        assertTrue(session.isActive())
        assertFalse(session.canPause())

        // Learn commands
        assertTrue(session.transition(ExplorationState.LEARNING))
        assertTrue(session.isActive())
        session.incrementLearned()
        assertEquals(1, session.elementsLearned)

        // Back to scanning for more
        assertTrue(session.transition(ExplorationState.SCANNING))
        session.incrementScanned()
        assertEquals(3, session.elementsScanned)

        // Process again
        assertTrue(session.transition(ExplorationState.PROCESSING))
        assertTrue(session.transition(ExplorationState.LEARNING))
        session.incrementLearned()
        assertEquals(2, session.elementsLearned)

        // Complete the exploration
        assertTrue(session.transition(ExplorationState.COMPLETING))
        assertFalse(session.isActive())

        assertTrue(session.transition(ExplorationState.COMPLETED))
        assertFalse(session.isActive())
        assertNotNull(session.endTime)
        assertNotNull(session.getDuration())

        // Verify final stats
        assertEquals(3, session.elementsScanned)
        assertEquals(2, session.elementsLearned)
    }

    @Test
    fun `pause and resume lifecycle should work correctly`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        // Pause during scanning
        assertTrue(session.canPause())
        assertTrue(session.transition(ExplorationState.PAUSED))
        assertFalse(session.isActive())
        assertFalse(session.canPause())

        // Resume scanning
        assertTrue(session.transition(ExplorationState.SCANNING))
        assertTrue(session.isActive())
        assertTrue(session.canPause())
    }

    @Test
    fun `error recovery lifecycle should work correctly`() {
        val session = ExplorationSession(
            sessionId = "test",
            packageName = "com.example",
            state = ExplorationState.SCANNING
        )

        // Simulate error during scanning
        session.setError("Network connection lost")
        assertEquals(ExplorationState.ERROR, session.state)
        assertEquals("Network connection lost", session.errorMessage)
        assertNotNull(session.endTime)

        // Recovery - go back to IDLE
        assertTrue(session.transition(ExplorationState.IDLE))
        assertEquals(ExplorationState.IDLE, session.state)

        // Can start new exploration
        assertTrue(session.transition(ExplorationState.INITIALIZING))
        assertEquals(ExplorationState.INITIALIZING, session.state)
    }

    // ==================== currentTimeMillis Tests ====================

    @Test
    fun `currentTimeMillis should return positive value`() {
        val time = ExplorationSession.currentTimeMillis()
        assertTrue(time > 0)
    }

    @Test
    fun `currentTimeMillis should return increasing values`() {
        val time1 = ExplorationSession.currentTimeMillis()
        // Small delay to ensure time progresses
        var sum = 0
        for (i in 1..1000) sum += i
        val time2 = ExplorationSession.currentTimeMillis()

        assertTrue(time2 >= time1, "Time should not go backwards")
    }
}
