/**
 * Overlay Integration Tests
 *
 * Tests overlay service functionality including:
 * - Z-index management
 * - Dialog queue system
 * - Voice orb interactions
 * - Panel expand/collapse
 *
 * Created: 2025-12-03
 * Author: AVA AI Team
 */

package com.augmentalis.ava.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.features.overlay.service.DialogQueueManager
import com.augmentalis.ava.features.overlay.service.DialogPriority
import com.augmentalis.ava.features.overlay.service.DialogRequest
import com.augmentalis.ava.features.overlay.service.DialogState
import com.augmentalis.ava.features.overlay.service.OverlayLayer
import com.augmentalis.ava.features.overlay.service.OverlayZIndexManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any existing state
        OverlayZIndexManager.clear()
        DialogQueueManager.dismissAll()
    }

    @After
    fun cleanup() {
        OverlayZIndexManager.clear()
        DialogQueueManager.dismissAll()
    }

    // ==================== Z-Index Manager Tests ====================

    @Test
    fun testZIndexRegistration() {
        val orbId = "voice_orb"
        val panelId = "glass_panel"

        val orbZIndex = OverlayZIndexManager.register(orbId, OverlayLayer.INTERACTIVE)
        val panelZIndex = OverlayZIndexManager.register(panelId, OverlayLayer.CONTENT)

        // Interactive layer should have higher z-index than content
        assertTrue("Orb should be above panel", orbZIndex > panelZIndex)

        // Verify retrieval
        assertEquals(orbZIndex, OverlayZIndexManager.getZIndex(orbId))
        assertEquals(panelZIndex, OverlayZIndexManager.getZIndex(panelId))
    }

    @Test
    fun testZIndexBringToFront() {
        val id1 = "window1"
        val id2 = "window2"

        val zIndex1 = OverlayZIndexManager.register(id1, OverlayLayer.CONTENT)
        val zIndex2 = OverlayZIndexManager.register(id2, OverlayLayer.CONTENT)

        // window2 should be higher initially (registered later)
        assertTrue("Later registered should be higher", zIndex2 > zIndex1)

        // Bring window1 to front
        val newZIndex1 = OverlayZIndexManager.bringToFront(id1)

        assertNotNull("Bring to front should return new z-index", newZIndex1)
        assertTrue("Window1 should now be higher", newZIndex1!! > zIndex2)
    }

    @Test
    fun testZIndexLayerOrdering() {
        // Register windows in different layers
        val background = OverlayZIndexManager.register("bg", OverlayLayer.BACKGROUND)
        val content = OverlayZIndexManager.register("content", OverlayLayer.CONTENT)
        val interactive = OverlayZIndexManager.register("button", OverlayLayer.INTERACTIVE)
        val dialog = OverlayZIndexManager.register("dialog", OverlayLayer.DIALOG)
        val alert = OverlayZIndexManager.register("alert", OverlayLayer.ALERT)

        // Verify layer ordering
        assertTrue("Content > Background", content > background)
        assertTrue("Interactive > Content", interactive > content)
        assertTrue("Dialog > Interactive", dialog > interactive)
        assertTrue("Alert > Dialog", alert > dialog)
    }

    @Test
    fun testZIndexMoveToLayer() {
        val id = "movable_window"

        // Start in content layer
        val contentZIndex = OverlayZIndexManager.register(id, OverlayLayer.CONTENT)

        // Move to dialog layer
        val dialogZIndex = OverlayZIndexManager.moveToLayer(id, OverlayLayer.DIALOG)

        assertNotNull("Move should return new z-index", dialogZIndex)
        assertTrue("Dialog layer should be higher", dialogZIndex!! > contentZIndex)
    }

    @Test
    fun testZIndexUnregister() {
        val id = "temp_window"

        OverlayZIndexManager.register(id, OverlayLayer.CONTENT)
        assertNotNull("Should be registered", OverlayZIndexManager.getZIndex(id))

        OverlayZIndexManager.unregister(id)
        assertNull("Should be unregistered", OverlayZIndexManager.getZIndex(id))
    }

    @Test
    fun testZIndexGetWindowsInLayer() {
        OverlayZIndexManager.register("content1", OverlayLayer.CONTENT)
        OverlayZIndexManager.register("content2", OverlayLayer.CONTENT)
        OverlayZIndexManager.register("dialog1", OverlayLayer.DIALOG)

        val contentWindows = OverlayZIndexManager.getWindowsInLayer(OverlayLayer.CONTENT)
        val dialogWindows = OverlayZIndexManager.getWindowsInLayer(OverlayLayer.DIALOG)

        assertEquals(2, contentWindows.size)
        assertEquals(1, dialogWindows.size)
        assertTrue(contentWindows.contains("content1"))
        assertTrue(contentWindows.contains("content2"))
        assertTrue(dialogWindows.contains("dialog1"))
    }

    // ==================== Dialog Queue Tests ====================

    @Test
    fun testDialogQueueEnqueue() = runTest {
        val dialog = DialogRequest(
            title = "Test",
            message = "Test message",
            priority = DialogPriority.NORMAL
        )

        DialogQueueManager.enqueue(dialog)

        // Wait for state update
        delay(100)

        val state = DialogQueueManager.state.value
        assertTrue("Dialog should be showing", state is DialogState.Showing)
        assertEquals(dialog.title, (state as DialogState.Showing).dialog.title)
    }

    @Test
    fun testDialogQueueDismiss() = runTest {
        val dialog = DialogRequest(
            title = "Dismissable",
            message = "Will be dismissed"
        )

        DialogQueueManager.enqueue(dialog)
        delay(100)

        assertTrue("Should be showing", DialogQueueManager.isShowing())

        DialogQueueManager.dismiss()
        delay(100)

        assertFalse("Should not be showing", DialogQueueManager.isShowing())
    }

    @Test
    fun testDialogQueuePriorityPreemption() = runTest {
        // Queue normal priority dialog
        val normal = DialogRequest(
            title = "Normal",
            message = "Normal priority",
            priority = DialogPriority.NORMAL
        )
        DialogQueueManager.enqueue(normal)
        delay(100)

        assertEquals("Normal", (DialogQueueManager.state.value as DialogState.Showing).dialog.title)

        // Queue critical priority - should preempt
        val critical = DialogRequest(
            title = "Critical",
            message = "Critical priority",
            priority = DialogPriority.CRITICAL
        )
        DialogQueueManager.enqueue(critical)
        delay(100)

        // Critical should now be showing
        val current = (DialogQueueManager.state.value as DialogState.Showing).dialog
        assertEquals("Critical", current.title)
    }

    @Test
    fun testDialogQueueSequencing() = runTest {
        val dialog1 = DialogRequest(title = "First", message = "First dialog")
        val dialog2 = DialogRequest(title = "Second", message = "Second dialog")
        val dialog3 = DialogRequest(title = "Third", message = "Third dialog")

        DialogQueueManager.enqueue(dialog1)
        DialogQueueManager.enqueue(dialog2)
        DialogQueueManager.enqueue(dialog3)
        delay(100)

        // First should be showing
        assertEquals("First", (DialogQueueManager.state.value as DialogState.Showing).dialog.title)
        assertEquals(2, DialogQueueManager.queueSize())

        // Dismiss first
        DialogQueueManager.dismiss()
        delay(100)

        // Second should now be showing
        assertEquals("Second", (DialogQueueManager.state.value as DialogState.Showing).dialog.title)
        assertEquals(1, DialogQueueManager.queueSize())

        // Dismiss second
        DialogQueueManager.dismiss()
        delay(100)

        // Third should now be showing
        assertEquals("Third", (DialogQueueManager.state.value as DialogState.Showing).dialog.title)
        assertEquals(0, DialogQueueManager.queueSize())
    }

    @Test
    fun testDialogBuilders() {
        // Test convenience builders
        val info = DialogQueueManager.Builder.info("Title", "Message")
        assertEquals(DialogPriority.NORMAL, info.priority)
        assertNotNull(info.primaryAction)

        val error = DialogQueueManager.Builder.error(message = "Error occurred")
        assertEquals(DialogPriority.HIGH, error.priority)
        assertFalse(error.dismissOnOutsideClick)

        val toast = DialogQueueManager.Builder.toast("Quick message", 2000)
        assertEquals(DialogPriority.LOW, toast.priority)
        assertEquals(2000L, toast.autoDismissMs)
    }

    @Test
    fun testDialogDismissAll() = runTest {
        repeat(5) {
            DialogQueueManager.enqueue(
                DialogRequest(title = "Dialog $it", message = "Message $it")
            )
        }
        delay(100)

        assertTrue(DialogQueueManager.isShowing())
        assertTrue(DialogQueueManager.queueSize() > 0)

        DialogQueueManager.dismissAll()
        delay(100)

        assertFalse(DialogQueueManager.isShowing())
        assertEquals(0, DialogQueueManager.queueSize())
    }

    @Test
    fun testDialogStats() = runTest {
        val dialog = DialogRequest(title = "Test", message = "Test")
        DialogQueueManager.enqueue(dialog)
        delay(100)

        DialogQueueManager.dismiss()
        delay(100)

        val stats = DialogQueueManager.getStats()
        assertTrue((stats["dialogsShown"] as Int) > 0)
        assertTrue((stats["dialogsDismissed"] as Int) > 0)
    }
}
