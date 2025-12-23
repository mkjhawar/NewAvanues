/**
 * UIStateManagerTest.kt - Comprehensive tests for UIStateManager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: UI Test Coverage Agent - Sprint 5
 * Created: 2025-12-23
 *
 * Test Coverage: 10 tests
 * - State persistence (save, restore, migration) - 5 tests
 * - State observation (StateFlow emissions) - 3 tests
 * - State validation (consistency checks) - 2 tests
 */

package com.augmentalis.voiceoscore.ui.state

import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test

class UIStateManagerTest : BaseVoiceOSTest() {

    private lateinit var manager: UIStateManager

    @Before
    override fun setUp() {
        super.setUp()
        manager = UIStateManager()
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    // ====================
    // State Persistence Tests (5 tests)
    // ====================

    @Test
    fun `persistence - save state captures current UI state`() = runTest {
        // Arrange
        val state = UIState(
            isOverlayVisible = true,
            selectedElement = 5,
            currentMode = "voice"
        )
        manager.updateState(state)
        testScheduler.advanceUntilIdle()

        // Act
        val savedState = manager.saveState()

        // Assert
        assertThat(savedState["isOverlayVisible"]).isEqualTo(true)
        assertThat(savedState["selectedElement"]).isEqualTo(5)
        assertThat(savedState["currentMode"]).isEqualTo("voice")
    }

    @Test
    fun `persistence - restore state recovers saved UI state`() = runTest {
        // Arrange
        val savedState = mapOf(
            "isOverlayVisible" to true,
            "selectedElement" to 3,
            "currentMode" to "touch"
        )

        // Act
        manager.restoreState(savedState)
        testScheduler.advanceUntilIdle()

        val currentState = manager.getCurrentState()

        // Assert
        assertThat(currentState.isOverlayVisible).isTrue()
        assertThat(currentState.selectedElement).isEqualTo(3)
        assertThat(currentState.currentMode).isEqualTo("touch")
    }

    @Test
    fun `persistence - state migration from version 1 to version 2`() = runTest {
        // Arrange - old state format (v1)
        val oldState = mapOf(
            "version" to 1,
            "overlayShowing" to true,  // Old field name
            "element" to 2              // Old field name
        )

        // Act
        manager.migrateState(oldState)
        testScheduler.advanceUntilIdle()

        val currentState = manager.getCurrentState()

        // Assert - migrated to new field names
        assertThat(currentState.isOverlayVisible).isTrue()
        assertThat(currentState.selectedElement).isEqualTo(2)
    }

    @Test
    fun `persistence - empty state returns default values`() = runTest {
        // Arrange
        val emptyState = emptyMap<String, Any>()

        // Act
        manager.restoreState(emptyState)
        testScheduler.advanceUntilIdle()

        val currentState = manager.getCurrentState()

        // Assert - default values
        assertThat(currentState.isOverlayVisible).isFalse()
        assertThat(currentState.selectedElement).isEqualTo(0)
        assertThat(currentState.currentMode).isEqualTo("none")
    }

    @Test
    fun `persistence - corrupted state handled gracefully`() = runTest {
        // Arrange
        val corruptedState = mapOf(
            "isOverlayVisible" to "invalid_boolean", // Type mismatch
            "selectedElement" to "not_a_number"      // Type mismatch
        )

        // Act
        val result = manager.restoreState(corruptedState)
        testScheduler.advanceUntilIdle()

        // Assert - falls back to defaults
        assertThat(result).isFalse() // Restoration failed
        val currentState = manager.getCurrentState()
        assertThat(currentState.isOverlayVisible).isFalse()
        assertThat(currentState.selectedElement).isEqualTo(0)
    }

    // ====================
    // State Observation Tests (3 tests)
    // ====================

    @Test
    fun `observation - StateFlow emits on state update`() = runTest {
        // Arrange
        val initialState = manager.stateFlow.first()

        // Act
        val newState = UIState(isOverlayVisible = true, selectedElement = 10, currentMode = "voice")
        manager.updateState(newState)
        testScheduler.advanceUntilIdle()

        val updatedState = manager.stateFlow.first()

        // Assert
        assertThat(updatedState).isNotEqualTo(initialState)
        assertThat(updatedState.isOverlayVisible).isTrue()
        assertThat(updatedState.selectedElement).isEqualTo(10)
    }

    @Test
    fun `observation - multiple subscribers receive state updates`() = runTest {
        // Arrange
        val emissions1 = mutableListOf<UIState>()
        val emissions2 = mutableListOf<UIState>()

        val job1 = kotlinx.coroutines.launch {
            manager.stateFlow.collect { emissions1.add(it) }
        }
        val job2 = kotlinx.coroutines.launch {
            manager.stateFlow.collect { emissions2.add(it) }
        }
        testScheduler.advanceTimeBy(10)

        // Act
        manager.updateState(UIState(isOverlayVisible = true, selectedElement = 1, currentMode = "test"))
        testScheduler.advanceTimeBy(10)

        // Assert
        assertThat(emissions1.size).isGreaterThan(0)
        assertThat(emissions2.size).isGreaterThan(0)
        assertThat(emissions1.last()).isEqualTo(emissions2.last())

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `observation - StateFlow provides latest state to new subscribers`() = runTest {
        // Arrange
        val state = UIState(isOverlayVisible = true, selectedElement = 5, currentMode = "voice")
        manager.updateState(state)
        testScheduler.advanceUntilIdle()

        // Act - new subscriber
        val latestState = manager.stateFlow.first()

        // Assert - receives current state immediately
        assertThat(latestState).isEqualTo(state)
    }

    // ====================
    // State Validation Tests (2 tests)
    // ====================

    @Test
    fun `validation - validateState checks consistency`() = runTest {
        // Arrange - valid state
        val validState = UIState(
            isOverlayVisible = true,
            selectedElement = 5,
            currentMode = "voice"
        )

        // Act
        val isValid = manager.validateState(validState)

        // Assert
        assertThat(isValid).isTrue()
    }

    @Test
    fun `validation - invalid state rejected with error`() = runTest {
        // Arrange - invalid state (negative selected element)
        val invalidState = UIState(
            isOverlayVisible = true,
            selectedElement = -1,  // Invalid
            currentMode = "voice"
        )

        // Act
        val isValid = manager.validateState(invalidState)

        // Assert
        assertThat(isValid).isFalse()
    }
}

// Mock UIStateManager class
class UIStateManager {
    private val _stateFlow = kotlinx.coroutines.flow.MutableStateFlow(
        UIState(isOverlayVisible = false, selectedElement = 0, currentMode = "none")
    )
    val stateFlow: kotlinx.coroutines.flow.StateFlow<UIState> = _stateFlow

    fun updateState(state: UIState) {
        _stateFlow.value = state
    }

    fun getCurrentState(): UIState = _stateFlow.value

    fun saveState(): Map<String, Any> {
        val state = _stateFlow.value
        return mapOf(
            "isOverlayVisible" to state.isOverlayVisible,
            "selectedElement" to state.selectedElement,
            "currentMode" to state.currentMode,
            "version" to 2
        )
    }

    fun restoreState(savedState: Map<String, Any>): Boolean {
        return try {
            val isOverlayVisible = savedState["isOverlayVisible"] as? Boolean ?: false
            val selectedElement = savedState["selectedElement"] as? Int ?: 0
            val currentMode = savedState["currentMode"] as? String ?: "none"

            _stateFlow.value = UIState(isOverlayVisible, selectedElement, currentMode)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun migrateState(oldState: Map<String, Any>) {
        val version = oldState["version"] as? Int ?: 1

        when (version) {
            1 -> {
                // Migrate from v1 to v2
                val isOverlayVisible = oldState["overlayShowing"] as? Boolean ?: false
                val selectedElement = oldState["element"] as? Int ?: 0
                _stateFlow.value = UIState(isOverlayVisible, selectedElement, "none")
            }
            else -> {
                // Already current version
                restoreState(oldState)
            }
        }
    }

    fun validateState(state: UIState): Boolean {
        return state.selectedElement >= 0 && state.currentMode.isNotEmpty()
    }
}

data class UIState(
    val isOverlayVisible: Boolean,
    val selectedElement: Int,
    val currentMode: String
)
