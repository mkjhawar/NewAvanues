// filename: features/overlay/src/test/java/com/augmentalis/ava/features/overlay/OverlayControllerTest.kt
// created: 2025-11-02 00:15:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 4 - Testing
// agent: Engineer | mode: ACT

package com.augmentalis.ava.features.overlay

import androidx.compose.ui.geometry.Offset
import com.augmentalis.ava.features.overlay.controller.OverlayController
import com.augmentalis.ava.features.overlay.controller.OverlayState
import com.augmentalis.ava.features.overlay.controller.Suggestion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OverlayController state management
 */
class OverlayControllerTest {

    private lateinit var controller: OverlayController

    @Before
    fun setup() {
        controller = OverlayController()
    }

    @Test
    fun `initial state is Docked and collapsed`() {
        assertEquals(OverlayState.Docked, controller.state.value)
        assertEquals(false, controller.expanded.value)
    }

    @Test
    fun `expand changes state to Listening and expanded true`() {
        controller.expand()

        assertEquals(OverlayState.Listening, controller.state.value)
        assertEquals(true, controller.expanded.value)
    }

    @Test
    fun `collapse resets state to Docked and clears transcript`() {
        // Setup
        controller.expand()
        controller.onTranscript("test transcript")

        // Act
        controller.collapse()

        // Assert
        assertEquals(OverlayState.Docked, controller.state.value)
        assertEquals(false, controller.expanded.value)
        assertNull(controller.transcript.value)
        assertNull(controller.response.value)
    }

    @Test
    fun `onTranscript updates transcript and changes to Processing state`() {
        val testText = "Hello AVA"

        controller.onTranscript(testText)

        assertEquals(testText, controller.transcript.value)
        assertEquals(OverlayState.Processing, controller.state.value)
    }

    @Test
    fun `onResponse updates response and changes to Responding state`() {
        val testResponse = "Hi! How can I help?"

        controller.onResponse(testResponse)

        assertEquals(testResponse, controller.response.value)
        assertEquals(OverlayState.Responding, controller.state.value)
    }

    @Test
    fun `updateOrbPosition adds delta to current position`() {
        val initialPosition = controller.orbPosition.value
        val delta = Offset(10f, 20f)

        controller.updateOrbPosition(delta)

        val expectedPosition = Offset(
            initialPosition.x + delta.x,
            initialPosition.y + delta.y
        )
        assertEquals(expectedPosition, controller.orbPosition.value)
    }

    @Test
    fun `setOrbPosition sets absolute position`() {
        val newPosition = Offset(100f, 200f)

        controller.setOrbPosition(newPosition)

        assertEquals(newPosition, controller.orbPosition.value)
    }

    @Test
    fun `updateSuggestions replaces suggestions list`() {
        val newSuggestions = listOf(
            Suggestion("Test 1", "test1"),
            Suggestion("Test 2", "test2")
        )

        controller.updateSuggestions(newSuggestions)

        assertEquals(newSuggestions, controller.suggestions.value)
    }

    @Test
    fun `executeSuggestion calls callback if set`() = runTest {
        var called = false
        var receivedSuggestion: Suggestion? = null

        controller.onSuggestionExecute = { suggestion ->
            called = true
            receivedSuggestion = suggestion
        }

        val testSuggestion = Suggestion("Test", "test")
        controller.executeSuggestion(testSuggestion)

        assertTrue(called)
        assertEquals(testSuggestion, receivedSuggestion)
    }

    @Test
    fun `onError updates response and changes to Error state`() {
        val errorMessage = "Network error"

        controller.onError(errorMessage)

        assertEquals("Error: $errorMessage", controller.response.value)
        assertEquals(OverlayState.Error, controller.state.value)
    }

    @Test
    fun `reset clears all state and returns to Docked`() {
        // Setup
        controller.expand()
        controller.onTranscript("test")
        controller.onResponse("response")

        // Act
        controller.reset()

        // Assert
        assertEquals(OverlayState.Docked, controller.state.value)
        assertEquals(false, controller.expanded.value)
        assertNull(controller.transcript.value)
        assertNull(controller.response.value)
    }

    @Test
    fun `startListening expands panel and clears previous state`() {
        // Setup
        controller.onTranscript("old transcript")
        controller.onResponse("old response")

        // Act
        controller.startListening()

        // Assert
        assertEquals(OverlayState.Listening, controller.state.value)
        assertEquals(true, controller.expanded.value)
        assertNull(controller.transcript.value)
        assertNull(controller.response.value)
    }
}
