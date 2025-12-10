/**
 * ScreenTransitionRepositoryTest.kt - Tests for ScreenTransition repository
 *
 * Tests all methods for IScreenTransitionRepository:
 * - insert / getById
 * - getFromScreen, getToScreen, getByTrigger
 * - getFrequent
 * - recordTransition (special method)
 * - deleteById, deleteByScreen, deleteAll
 * - count
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-27
 */

package com.avanues.database.repositories

import com.avanues.database.dto.ScreenTransitionDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ScreenTransitionRepositoryTest : BaseRepositoryTest() {

    // ==================== Helper Functions ====================

    /**
     * Create test ScreenTransitionDTO
     */
    private fun createTransition(
        fromScreenHash: String,
        toScreenHash: String,
        triggerElementHash: String? = "element-1",
        triggerAction: String = "click",
        transitionCount: Long = 1,
        avgDurationMs: Long = 100,
        lastTransitionAt: Long = now()
    ): ScreenTransitionDTO {
        return ScreenTransitionDTO(
            id = 0, // Auto-generated
            fromScreenHash = fromScreenHash,
            toScreenHash = toScreenHash,
            triggerElementHash = triggerElementHash,
            triggerAction = triggerAction,
            transitionCount = transitionCount,
            avgDurationMs = avgDurationMs,
            lastTransitionAt = lastTransitionAt
        )
    }

    // ==================== Insert / Get Tests ====================

    @Test
    fun testInsertAndGetById() = runTest {
        val repo = databaseManager.screenTransitions

        val transition = createTransition(
            fromScreenHash = "screen-1",
            toScreenHash = "screen-2",
            triggerElementHash = "button-1",
            triggerAction = "click"
        )

        val id = repo.insert(transition)
        assertTrue(id > 0)

        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertEquals("screen-1", retrieved.fromScreenHash)
        assertEquals("screen-2", retrieved.toScreenHash)
        assertEquals("button-1", retrieved.triggerElementHash)
        assertEquals("click", retrieved.triggerAction)
    }

    @Test
    fun testGetByIdNull() = runTest {
        val repo = databaseManager.screenTransitions

        val retrieved = repo.getById(999)
        assertNull(retrieved)
    }

    @Test
    fun testInsertReturnsId() = runTest {
        val repo = databaseManager.screenTransitions

        val id1 = repo.insert(createTransition("screen-1", "screen-2"))
        val id2 = repo.insert(createTransition("screen-2", "screen-3"))

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        assertNotEquals(id1, id2)
    }

    // ==================== Query Tests ====================

    @Test
    fun testGetFromScreenEmpty() = runTest {
        val repo = databaseManager.screenTransitions

        val transitions = repo.getFromScreen("screen-1")
        assertEquals(0, transitions.size)
    }

    @Test
    fun testGetFromScreenMultiple() = runTest {
        val repo = databaseManager.screenTransitions

        // Screen-1 → Screen-2
        repo.insert(createTransition("screen-1", "screen-2"))
        // Screen-1 → Screen-3
        repo.insert(createTransition("screen-1", "screen-3"))
        // Screen-2 → Screen-3
        repo.insert(createTransition("screen-2", "screen-3"))

        val fromScreen1 = repo.getFromScreen("screen-1")
        assertEquals(2, fromScreen1.size)

        val fromScreen2 = repo.getFromScreen("screen-2")
        assertEquals(1, fromScreen2.size)
    }

    @Test
    fun testGetToScreenEmpty() = runTest {
        val repo = databaseManager.screenTransitions

        val transitions = repo.getToScreen("screen-2")
        assertEquals(0, transitions.size)
    }

    @Test
    fun testGetToScreenMultiple() = runTest {
        val repo = databaseManager.screenTransitions

        // Screen-1 → Screen-3
        repo.insert(createTransition("screen-1", "screen-3"))
        // Screen-2 → Screen-3
        repo.insert(createTransition("screen-2", "screen-3"))
        // Screen-1 → Screen-2
        repo.insert(createTransition("screen-1", "screen-2"))

        val toScreen3 = repo.getToScreen("screen-3")
        assertEquals(2, toScreen3.size)

        val toScreen2 = repo.getToScreen("screen-2")
        assertEquals(1, toScreen2.size)
    }

    @Test
    fun testGetByTriggerEmpty() = runTest {
        val repo = databaseManager.screenTransitions

        val transitions = repo.getByTrigger("button-1")
        assertEquals(0, transitions.size)
    }

    @Test
    fun testGetByTriggerMultiple() = runTest {
        val repo = databaseManager.screenTransitions

        repo.insert(createTransition("screen-1", "screen-2", triggerElementHash = "button-1"))
        repo.insert(createTransition("screen-2", "screen-3", triggerElementHash = "button-1"))
        repo.insert(createTransition("screen-1", "screen-3", triggerElementHash = "button-2"))

        val byButton1 = repo.getByTrigger("button-1")
        assertEquals(2, byButton1.size)

        val byButton2 = repo.getByTrigger("button-2")
        assertEquals(1, byButton2.size)
    }

    @Test
    fun testGetFrequentEmpty() = runTest {
        val repo = databaseManager.screenTransitions

        val frequent = repo.getFrequent(10)
        assertEquals(0, frequent.size)
    }

    @Test
    fun testGetFrequentOrderedByCount() = runTest {
        val repo = databaseManager.screenTransitions

        // Insert transitions with different counts
        repo.insert(createTransition("screen-1", "screen-2", transitionCount = 5))
        repo.insert(createTransition("screen-2", "screen-3", transitionCount = 10))
        repo.insert(createTransition("screen-3", "screen-4", transitionCount = 3))
        repo.insert(createTransition("screen-4", "screen-5", transitionCount = 7))

        val top2 = repo.getFrequent(2)
        assertEquals(2, top2.size)
        // First should have highest count (10)
        assertEquals(10, top2[0].transitionCount)
        // Second should have second highest (7)
        assertEquals(7, top2[1].transitionCount)
    }

    @Test
    fun testGetFrequentLimit() = runTest {
        val repo = databaseManager.screenTransitions

        // Insert 5 transitions
        repeat(5) {
            repo.insert(createTransition("screen-$it", "screen-${it + 1}", transitionCount = (it + 1).toLong()))
        }

        val top3 = repo.getFrequent(3)
        assertEquals(3, top3.size)

        val all = repo.getFrequent(10)
        assertEquals(5, all.size)
    }

    // ==================== recordTransition Tests ====================

    @Test
    fun testRecordTransitionCreatesNew() = runTest {
        val repo = databaseManager.screenTransitions

        // Record a transition
        repo.recordTransition(
            fromScreenHash = "screen-1",
            toScreenHash = "screen-2",
            durationMs = 150,
            timestamp = now()
        )

        val transitions = repo.getFromScreen("screen-1")
        assertEquals(1, transitions.size)
    }

    @Test
    fun testRecordTransitionUpdatesExisting() = runTest {
        val repo = databaseManager.screenTransitions

        // Create initial transition (matching what recordTransition uses)
        repo.insert(createTransition(
            fromScreenHash = "screen-1",
            toScreenHash = "screen-2",
            triggerElementHash = null,
            triggerAction = "navigation",
            transitionCount = 1,
            avgDurationMs = 100
        ))

        val initialCount = repo.count()

        // Record same transition again
        repo.recordTransition(
            fromScreenHash = "screen-1",
            toScreenHash = "screen-2",
            durationMs = 200,
            timestamp = now()
        )

        // Should not create new record
        assertEquals(initialCount, repo.count())

        // Should update existing record
        val transitions = repo.getFromScreen("screen-1")
        assertEquals(1, transitions.size)
    }

    // ==================== Delete Tests ====================

    @Test
    fun testDeleteById() = runTest {
        val repo = databaseManager.screenTransitions

        val id1 = repo.insert(createTransition("screen-1", "screen-2"))
        val id2 = repo.insert(createTransition("screen-2", "screen-3"))

        assertEquals(2, repo.count())

        repo.deleteById(id1)

        assertEquals(1, repo.count())
        assertNull(repo.getById(id1))
        assertNotNull(repo.getById(id2))
    }

    @Test
    fun testDeleteByIdNonexistent() = runTest {
        val repo = databaseManager.screenTransitions

        repo.insert(createTransition("screen-1", "screen-2"))

        // Should not throw
        repo.deleteById(999)

        assertEquals(1, repo.count())
    }

    @Test
    fun testDeleteByScreen() = runTest {
        val repo = databaseManager.screenTransitions

        // Transitions involving screen-2
        repo.insert(createTransition("screen-1", "screen-2"))
        repo.insert(createTransition("screen-2", "screen-3"))
        repo.insert(createTransition("screen-2", "screen-4"))

        // Transition not involving screen-2
        repo.insert(createTransition("screen-1", "screen-3"))

        assertEquals(4, repo.count())

        repo.deleteByScreen("screen-2")

        // Should delete transitions where screen-2 is either from or to
        val remaining = repo.count()
        assertEquals(1, remaining)

        // Only screen-1 → screen-3 should remain
        assertEquals(0, repo.getFromScreen("screen-2").size)
        assertEquals(0, repo.getToScreen("screen-2").size)
        assertEquals(1, repo.getFromScreen("screen-1").size)
    }

    @Test
    fun testDeleteAll() = runTest {
        val repo = databaseManager.screenTransitions

        repo.insert(createTransition("screen-1", "screen-2"))
        repo.insert(createTransition("screen-2", "screen-3"))
        repo.insert(createTransition("screen-3", "screen-4"))

        assertEquals(3, repo.count())

        repo.deleteAll()

        assertEquals(0, repo.count())
    }

    // ==================== Count Tests ====================

    @Test
    fun testCountEmpty() = runTest {
        val repo = databaseManager.screenTransitions

        assertEquals(0, repo.count())
    }

    @Test
    fun testCountMultiple() = runTest {
        val repo = databaseManager.screenTransitions

        repo.insert(createTransition("screen-1", "screen-2"))
        repo.insert(createTransition("screen-2", "screen-3"))
        repo.insert(createTransition("screen-3", "screen-4"))

        assertEquals(3, repo.count())
    }

    @Test
    fun testCountAfterDelete() = runTest {
        val repo = databaseManager.screenTransitions

        val id1 = repo.insert(createTransition("screen-1", "screen-2"))
        repo.insert(createTransition("screen-2", "screen-3"))

        assertEquals(2, repo.count())

        repo.deleteById(id1)

        assertEquals(1, repo.count())
    }

    // ==================== Edge Cases ====================

    @Test
    fun testNullTriggerElement() = runTest {
        val repo = databaseManager.screenTransitions

        val transition = createTransition(
            fromScreenHash = "screen-1",
            toScreenHash = "screen-2",
            triggerElementHash = null
        )

        val id = repo.insert(transition)

        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertNull(retrieved.triggerElementHash)
    }

    @Test
    fun testEmptyStringFields() = runTest {
        val repo = databaseManager.screenTransitions

        val transition = createTransition(
            fromScreenHash = "",
            toScreenHash = "",
            triggerElementHash = "",
            triggerAction = ""
        )

        val id = repo.insert(transition)

        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertEquals("", retrieved.fromScreenHash)
        assertEquals("", retrieved.toScreenHash)
        assertEquals("", retrieved.triggerElementHash)
        assertEquals("", retrieved.triggerAction)
    }

    @Test
    fun testLargeNumbers() = runTest {
        val repo = databaseManager.screenTransitions

        val transition = createTransition(
            fromScreenHash = "screen-1",
            toScreenHash = "screen-2",
            transitionCount = Long.MAX_VALUE,
            avgDurationMs = Long.MAX_VALUE,
            lastTransitionAt = Long.MAX_VALUE
        )

        val id = repo.insert(transition)

        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertEquals(Long.MAX_VALUE, retrieved.transitionCount)
        assertEquals(Long.MAX_VALUE, retrieved.avgDurationMs)
        assertEquals(Long.MAX_VALUE, retrieved.lastTransitionAt)
    }

    @Test
    fun testZeroValues() = runTest {
        val repo = databaseManager.screenTransitions

        val transition = createTransition(
            fromScreenHash = "screen-1",
            toScreenHash = "screen-2",
            transitionCount = 0,
            avgDurationMs = 0,
            lastTransitionAt = 0
        )

        val id = repo.insert(transition)

        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertEquals(0, retrieved.transitionCount)
        assertEquals(0, retrieved.avgDurationMs)
        assertEquals(0, retrieved.lastTransitionAt)
    }

    // ==================== Integration Tests ====================

    @Test
    fun testCompleteNavigationFlow() = runTest {
        val repo = databaseManager.screenTransitions

        // User navigates: Screen-1 → Screen-2 → Screen-3 → Screen-1 (loop)
        repo.insert(createTransition("screen-1", "screen-2", transitionCount = 5))
        repo.insert(createTransition("screen-2", "screen-3", transitionCount = 3))
        repo.insert(createTransition("screen-3", "screen-1", transitionCount = 2))

        // Verify navigation graph
        assertEquals(2, repo.getFromScreen("screen-1").size + repo.getToScreen("screen-1").size)

        // Most frequent should be screen-1 → screen-2
        val frequent = repo.getFrequent(1)
        assertEquals(1, frequent.size)
        assertEquals("screen-1", frequent[0].fromScreenHash)
        assertEquals("screen-2", frequent[0].toScreenHash)
    }

    @Test
    fun testTransitionIsolation() = runTest {
        val repo = databaseManager.screenTransitions

        // App1 transitions
        repo.insert(createTransition("app1-screen1", "app1-screen2"))
        repo.insert(createTransition("app1-screen2", "app1-screen3"))

        // App2 transitions
        repo.insert(createTransition("app2-screen1", "app2-screen2"))

        // Verify isolation
        assertEquals(2, repo.getFromScreen("app1-screen1").size + repo.getFromScreen("app1-screen2").size)
        assertEquals(1, repo.getFromScreen("app2-screen1").size)

        // Delete app1 transitions
        repo.deleteByScreen("app1-screen1")
        repo.deleteByScreen("app1-screen2")
        repo.deleteByScreen("app1-screen3")

        // App2 should remain
        assertEquals(1, repo.count())
    }

    @Test
    fun testMultipleTriggerElements() = runTest {
        val repo = databaseManager.screenTransitions

        // Same screens, different triggers
        repo.insert(createTransition(
            "screen-1", "screen-2",
            triggerElementHash = "button-submit",
            triggerAction = "click"
        ))
        repo.insert(createTransition(
            "screen-1", "screen-2",
            triggerElementHash = "button-next",
            triggerAction = "click"
        ))
        repo.insert(createTransition(
            "screen-1", "screen-2",
            triggerElementHash = "link-skip",
            triggerAction = "tap"
        ))

        // All transitions go from screen-1 to screen-2
        assertEquals(3, repo.getFromScreen("screen-1").size)

        // But different triggers
        assertEquals(1, repo.getByTrigger("button-submit").size)
        assertEquals(1, repo.getByTrigger("button-next").size)
        assertEquals(1, repo.getByTrigger("link-skip").size)
    }

    @Test
    fun testRecordTransitionWorkflow() = runTest {
        val repo = databaseManager.screenTransitions

        // User navigates from screen-1 to screen-2 multiple times
        repo.recordTransition("screen-1", "screen-2", 100, past(10000))
        repo.recordTransition("screen-1", "screen-2", 150, past(5000))
        repo.recordTransition("screen-1", "screen-2", 120, now())

        // Should only have one transition record (updated each time)
        val transitions = repo.getFromScreen("screen-1")
        assertEquals(1, transitions.size)

        // Verify it's the most recent
        val transition = transitions[0]
        assertEquals("screen-2", transition.toScreenHash)
    }
}
