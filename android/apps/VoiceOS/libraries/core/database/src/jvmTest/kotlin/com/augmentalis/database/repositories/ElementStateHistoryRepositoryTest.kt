/**
 * ElementStateHistoryRepositoryTest.kt - Tests for ElementStateHistory repository
 *
 * Tests new method added to IElementStateHistoryRepository:
 * - getCurrentState
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-27
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ElementStateHistoryDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ElementStateHistoryRepositoryTest : BaseRepositoryTest() {

    // ==================== Helper Functions ====================

    /**
     * Create test ElementStateHistoryDTO
     */
    private fun createStateChange(
        elementHash: String,
        screenHash: String = "screen-hash",
        stateType: String,
        oldValue: String? = null,
        newValue: String? = null,
        timestamp: Long = now(),
        triggeredBy: String = "user"
    ): ElementStateHistoryDTO {
        return ElementStateHistoryDTO(
            id = 0, // Auto-generated
            elementHash = elementHash,
            screenHash = screenHash,
            stateType = stateType,
            oldValue = oldValue,
            newValue = newValue,
            changedAt = timestamp,
            triggeredBy = triggeredBy
        )
    }

    // ==================== getCurrentState Tests ====================

    @Test
    fun testGetCurrentStateNull() = runTest {
        val repo = databaseManager.elementStateHistory

        val currentState = repo.getCurrentState("element-1", "checked")
        assertNull(currentState)
    }

    @Test
    fun testGetCurrentStateSingle() = runTest {
        val repo = databaseManager.elementStateHistory

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            oldValue = "false",
            newValue = "true",
            timestamp = now()
        ))

        val currentState = repo.getCurrentState("element-1", "checked")
        assertNotNull(currentState)
        assertEquals("checked", currentState.stateType)
        assertEquals("true", currentState.newValue)
    }

    @Test
    fun testGetCurrentStateMultipleReturnsLatest() = runTest {
        val repo = databaseManager.elementStateHistory

        // Insert state changes at different times
        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            oldValue = "false",
            newValue = "true",
            timestamp = past(10000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            oldValue = "true",
            newValue = "false",
            timestamp = past(5000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            oldValue = "false",
            newValue = "true",
            timestamp = now()
        ))

        val currentState = repo.getCurrentState("element-1", "checked")
        assertNotNull(currentState)
        assertEquals("true", currentState.newValue)
        // Should be the most recent one
        assertTrue(currentState.changedAt >= past(1000))
    }

    @Test
    fun testGetCurrentStateFiltersByStateType() = runTest {
        val repo = databaseManager.elementStateHistory

        // Insert different state types
        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            newValue = "true",
            timestamp = past(5000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "enabled",
            newValue = "false",
            timestamp = now()
        ))

        // Get current state for "checked" - should return older one, not "enabled"
        val checkedState = repo.getCurrentState("element-1", "checked")
        assertNotNull(checkedState)
        assertEquals("checked", checkedState.stateType)
        assertEquals("true", checkedState.newValue)

        // Get current state for "enabled"
        val enabledState = repo.getCurrentState("element-1", "enabled")
        assertNotNull(enabledState)
        assertEquals("enabled", enabledState.stateType)
        assertEquals("false", enabledState.newValue)
    }

    @Test
    fun testGetCurrentStateFiltersByElement() = runTest {
        val repo = databaseManager.elementStateHistory

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            newValue = "true"
        ))

        repo.insert(createStateChange(
            elementHash = "element-2",
            stateType = "checked",
            newValue = "false"
        ))

        val state1 = repo.getCurrentState("element-1", "checked")
        assertNotNull(state1)
        assertEquals("true", state1.newValue)

        val state2 = repo.getCurrentState("element-2", "checked")
        assertNotNull(state2)
        assertEquals("false", state2.newValue)
    }

    // ==================== Integration Tests ====================

    @Test
    fun testGetCurrentStateAfterDelete() = runTest {
        val repo = databaseManager.elementStateHistory

        val id = repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            newValue = "true"
        ))

        assertNotNull(repo.getCurrentState("element-1", "checked"))

        repo.deleteById(id)

        assertNull(repo.getCurrentState("element-1", "checked"))
    }

    @Test
    fun testGetCurrentStateWithMultipleTypes() = runTest {
        val repo = databaseManager.elementStateHistory

        // Track multiple state types for same element
        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            newValue = "true",
            timestamp = past(10000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "enabled",
            newValue = "true",
            timestamp = past(8000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "focused",
            newValue = "false",
            timestamp = past(6000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            newValue = "false",
            timestamp = now()
        ))

        // Each state type should return its most recent value
        assertEquals("false", repo.getCurrentState("element-1", "checked")?.newValue)
        assertEquals("true", repo.getCurrentState("element-1", "enabled")?.newValue)
        assertEquals("false", repo.getCurrentState("element-1", "focused")?.newValue)
    }

    @Test
    fun testGetCurrentStateAfterClear() = runTest {
        val repo = databaseManager.elementStateHistory

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "checked",
            newValue = "true"
        ))

        assertNotNull(repo.getCurrentState("element-1", "checked"))

        repo.deleteAll()

        assertNull(repo.getCurrentState("element-1", "checked"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun testGetCurrentStateEmptyStrings() = runTest {
        val repo = databaseManager.elementStateHistory

        val currentState = repo.getCurrentState("", "")
        assertNull(currentState)
    }

    @Test
    fun testGetCurrentStateWithNullValues() = runTest {
        val repo = databaseManager.elementStateHistory

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "text",
            oldValue = "Hello",
            newValue = null  // Cleared
        ))

        val currentState = repo.getCurrentState("element-1", "text")
        assertNotNull(currentState)
        assertEquals("text", currentState.stateType)
        assertNull(currentState.newValue)
        assertEquals("Hello", currentState.oldValue)
    }

    @Test
    fun testGetCurrentStateOrdering() = runTest {
        val repo = databaseManager.elementStateHistory

        // Insert in random order
        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "value",
            newValue = "second",
            timestamp = past(5000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "value",
            newValue = "first",
            timestamp = past(10000)
        ))

        repo.insert(createStateChange(
            elementHash = "element-1",
            stateType = "value",
            newValue = "third",
            timestamp = now()
        ))

        // Should return the most recent (third)
        val currentState = repo.getCurrentState("element-1", "value")
        assertNotNull(currentState)
        assertEquals("third", currentState.newValue)
    }
}
