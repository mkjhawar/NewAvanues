/**
 * CommandStatusOverlayTest.kt - TDD tests for CommandStatusOverlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * TDD tests for voice command status overlay.
 * Tests cover visibility, state transitions, and resource cleanup.
 */
package com.augmentalis.voiceoscoreng.overlay

import com.augmentalis.voiceoscoreng.features.CommandStatusOverlay
import com.augmentalis.voiceoscoreng.features.CommandState
import com.augmentalis.voiceoscoreng.features.currentTimeMillis
import com.augmentalis.voiceoscoreng.features.IOverlay
import com.augmentalis.voiceoscoreng.features.OverlayData
import com.augmentalis.voiceoscoreng.features.StatusUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class CommandStatusOverlayTest {

    private lateinit var overlay: CommandStatusOverlay

    @BeforeTest
    fun setup() {
        overlay = CommandStatusOverlay()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Identity Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay has command_status id`() {
        assertEquals("command_status", overlay.id)
    }

    @Test
    fun `overlay id is stable across lifecycle`() {
        assertEquals("command_status", overlay.id)
        overlay.show()
        assertEquals("command_status", overlay.id)
        overlay.hide()
        assertEquals("command_status", overlay.id)
        overlay.dispose()
        assertEquals("command_status", overlay.id)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Initial State Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay starts hidden`() {
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `overlay starts with LISTENING state`() {
        assertEquals(CommandState.LISTENING, overlay.currentState)
    }

    @Test
    fun `stateFlow initially emits null`() = runTest {
        assertNull(overlay.stateFlow.value)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // showStatus Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `showStatus makes overlay visible`() {
        assertFalse(overlay.isVisible)

        overlay.showStatus("click button", CommandState.LISTENING, "Listening...")

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `showStatus with LISTENING state sets currentState to LISTENING`() {
        overlay.showStatus("click button", CommandState.LISTENING, "Listening...")

        assertEquals(CommandState.LISTENING, overlay.currentState)
    }

    @Test
    fun `showStatus with PROCESSING state sets currentState to PROCESSING`() {
        overlay.showStatus("click button", CommandState.PROCESSING, "Processing...")

        assertEquals(CommandState.PROCESSING, overlay.currentState)
    }

    @Test
    fun `showStatus with EXECUTING state sets currentState to EXECUTING`() {
        overlay.showStatus("click button", CommandState.EXECUTING, "Executing...")

        assertEquals(CommandState.EXECUTING, overlay.currentState)
    }

    @Test
    fun `showStatus with SUCCESS state sets currentState to SUCCESS`() {
        overlay.showStatus("click button", CommandState.SUCCESS, "Done!")

        assertEquals(CommandState.SUCCESS, overlay.currentState)
    }

    @Test
    fun `showStatus with ERROR state sets currentState to ERROR`() {
        overlay.showStatus("click button", CommandState.ERROR, "Failed")

        assertEquals(CommandState.ERROR, overlay.currentState)
    }

    @Test
    fun `showStatus emits StatusUpdate to stateFlow`() = runTest {
        overlay.showStatus("click submit", CommandState.PROCESSING, "Processing command...")

        val update = overlay.stateFlow.value
        assertNotNull(update)
        assertEquals("click submit", update.command)
        assertEquals(CommandState.PROCESSING, update.state)
        assertEquals("Processing command...", update.message)
    }

    @Test
    fun `showStatus sets timestamp in StatusUpdate`() = runTest {
        val beforeTime = currentTimeMillis()
        overlay.showStatus("test command", CommandState.LISTENING, "Message")
        val afterTime = currentTimeMillis()

        val update = overlay.stateFlow.value
        assertNotNull(update)
        assertTrue(update.timestamp >= beforeTime)
        assertTrue(update.timestamp <= afterTime)
    }

    @Test
    fun `showStatus called multiple times keeps overlay visible`() {
        overlay.showStatus("first", CommandState.LISTENING, "First")
        assertTrue(overlay.isVisible)

        overlay.showStatus("second", CommandState.PROCESSING, "Second")
        assertTrue(overlay.isVisible)

        overlay.showStatus("third", CommandState.SUCCESS, "Third")
        assertTrue(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // updateStatus Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `updateStatus changes state without affecting visibility`() {
        overlay.showStatus("command", CommandState.LISTENING, "Initial")
        assertTrue(overlay.isVisible)
        assertEquals(CommandState.LISTENING, overlay.currentState)

        overlay.updateStatus("command", CommandState.PROCESSING, "Updated")

        assertTrue(overlay.isVisible) // Still visible
        assertEquals(CommandState.PROCESSING, overlay.currentState)
    }

    @Test
    fun `updateStatus changes command text`() = runTest {
        overlay.showStatus("original", CommandState.LISTENING, "Message")

        overlay.updateStatus("updated", CommandState.PROCESSING, "New message")

        val update = overlay.stateFlow.value
        assertNotNull(update)
        assertEquals("updated", update.command)
    }

    @Test
    fun `updateStatus changes message`() = runTest {
        overlay.showStatus("command", CommandState.LISTENING, "Original message")

        overlay.updateStatus("command", CommandState.LISTENING, "New message")

        val update = overlay.stateFlow.value
        assertNotNull(update)
        assertEquals("New message", update.message)
    }

    @Test
    fun `updateStatus emits new StatusUpdate to stateFlow`() = runTest {
        overlay.showStatus("command", CommandState.LISTENING, "First")
        val firstUpdate = overlay.stateFlow.value

        overlay.updateStatus("command", CommandState.SUCCESS, "Second")
        val secondUpdate = overlay.stateFlow.value

        assertNotNull(firstUpdate)
        assertNotNull(secondUpdate)
        assertEquals(CommandState.LISTENING, firstUpdate.state)
        assertEquals(CommandState.SUCCESS, secondUpdate.state)
    }

    @Test
    fun `updateStatus works when overlay is not visible`() {
        assertFalse(overlay.isVisible)

        overlay.updateStatus("command", CommandState.PROCESSING, "Message")

        assertEquals(CommandState.PROCESSING, overlay.currentState)
        assertFalse(overlay.isVisible) // Still not visible
    }

    // ═══════════════════════════════════════════════════════════════════════
    // show and hide Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show makes overlay visible`() {
        assertFalse(overlay.isVisible)

        overlay.show()

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `hide makes overlay invisible`() {
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.hide()

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `hide after showStatus makes overlay invisible`() {
        overlay.showStatus("command", CommandState.LISTENING, "Message")
        assertTrue(overlay.isVisible)

        overlay.hide()

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `multiple show calls are idempotent`() {
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.show()
        overlay.show()

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `multiple hide calls are idempotent`() {
        overlay.show()
        overlay.hide()
        assertFalse(overlay.isVisible)

        overlay.hide()
        overlay.hide()

        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // toggle Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `toggle shows hidden overlay`() {
        assertFalse(overlay.isVisible)

        overlay.toggle()

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `toggle hides visible overlay`() {
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.toggle()

        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // update Tests (IOverlay interface)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `update with Status data shows status`() {
        val statusData = OverlayData.Status("Processing...", CommandState.PROCESSING)

        overlay.update(statusData)

        assertEquals(CommandState.PROCESSING, overlay.currentState)
    }

    @Test
    fun `update with non-Status data is ignored`() {
        overlay.showStatus("original", CommandState.LISTENING, "Message")
        val originalState = overlay.currentState

        // These should be ignored since they're not Status data
        overlay.update(OverlayData.Confidence(0.95f, "text"))
        overlay.update(OverlayData.NumberedItems(emptyList()))
        overlay.update(OverlayData.ContextMenu(emptyList(), null))

        assertEquals(originalState, overlay.currentState)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // dispose Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `dispose makes overlay invisible`() {
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.dispose()

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `dispose clears stateFlow`() = runTest {
        overlay.showStatus("command", CommandState.SUCCESS, "Message")
        assertNotNull(overlay.stateFlow.value)

        overlay.dispose()

        assertNull(overlay.stateFlow.value)
    }

    @Test
    fun `show has no effect after dispose`() {
        overlay.dispose()

        overlay.show()

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `showStatus has no effect after dispose`() {
        overlay.dispose()

        overlay.showStatus("command", CommandState.LISTENING, "Message")

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `updateStatus has no effect after dispose`() {
        overlay.showStatus("original", CommandState.LISTENING, "Message")
        overlay.dispose()

        overlay.updateStatus("new", CommandState.SUCCESS, "New message")

        // Should still be null (cleared by dispose)
        assertNull(overlay.stateFlow.value)
    }

    @Test
    fun `update has no effect after dispose`() {
        overlay.showStatus("original", CommandState.LISTENING, "Message")
        overlay.dispose()

        overlay.update(OverlayData.Status("new", CommandState.SUCCESS))

        assertNull(overlay.stateFlow.value)
    }

    @Test
    fun `dispose can be called multiple times safely`() {
        overlay.show()

        overlay.dispose()
        overlay.dispose()
        overlay.dispose()

        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // State Lifecycle Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `state transitions through full lifecycle`() {
        // LISTENING -> PROCESSING -> EXECUTING -> SUCCESS
        overlay.showStatus("click button", CommandState.LISTENING, "Listening...")
        assertEquals(CommandState.LISTENING, overlay.currentState)

        overlay.updateStatus("click button", CommandState.PROCESSING, "Processing...")
        assertEquals(CommandState.PROCESSING, overlay.currentState)

        overlay.updateStatus("click button", CommandState.EXECUTING, "Executing...")
        assertEquals(CommandState.EXECUTING, overlay.currentState)

        overlay.updateStatus("click button", CommandState.SUCCESS, "Done!")
        assertEquals(CommandState.SUCCESS, overlay.currentState)
    }

    @Test
    fun `state can transition to ERROR from any state`() {
        // From LISTENING
        overlay.showStatus("command", CommandState.LISTENING, "Listening")
        overlay.updateStatus("command", CommandState.ERROR, "Error")
        assertEquals(CommandState.ERROR, overlay.currentState)

        // From PROCESSING
        overlay.showStatus("command", CommandState.PROCESSING, "Processing")
        overlay.updateStatus("command", CommandState.ERROR, "Error")
        assertEquals(CommandState.ERROR, overlay.currentState)

        // From EXECUTING
        overlay.showStatus("command", CommandState.EXECUTING, "Executing")
        overlay.updateStatus("command", CommandState.ERROR, "Error")
        assertEquals(CommandState.ERROR, overlay.currentState)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // StatusUpdate Data Class Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `StatusUpdate contains all required fields`() {
        val update = StatusUpdate(
            command = "click button",
            state = CommandState.SUCCESS,
            message = "Button clicked"
        )

        assertEquals("click button", update.command)
        assertEquals(CommandState.SUCCESS, update.state)
        assertEquals("Button clicked", update.message)
        assertTrue(update.timestamp > 0)
    }

    @Test
    fun `StatusUpdate equality works correctly`() {
        val timestamp = currentTimeMillis()
        val update1 = StatusUpdate("cmd", CommandState.SUCCESS, "msg", timestamp)
        val update2 = StatusUpdate("cmd", CommandState.SUCCESS, "msg", timestamp)
        val update3 = StatusUpdate("different", CommandState.SUCCESS, "msg", timestamp)

        assertEquals(update1, update2)
        assertFalse(update1 == update3)
    }

    @Test
    fun `StatusUpdate copy works correctly`() {
        val original = StatusUpdate("cmd", CommandState.LISTENING, "msg")
        val copied = original.copy(state = CommandState.SUCCESS)

        assertEquals("cmd", copied.command)
        assertEquals(CommandState.SUCCESS, copied.state)
        assertEquals("msg", copied.message)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Integration with IOverlay Contract
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `CommandStatusOverlay implements IOverlay interface`() {
        val iOverlay: IOverlay = overlay

        // Verify interface methods work
        assertEquals("command_status", iOverlay.id)
        assertFalse(iOverlay.isVisible)

        iOverlay.show()
        assertTrue(iOverlay.isVisible)

        iOverlay.hide()
        assertFalse(iOverlay.isVisible)

        iOverlay.update(OverlayData.Status("test", CommandState.LISTENING))

        iOverlay.dispose()
        assertFalse(iOverlay.isVisible)
    }
}
