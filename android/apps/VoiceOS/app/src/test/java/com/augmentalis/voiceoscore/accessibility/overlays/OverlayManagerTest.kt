/**
 * OverlayManagerTest.kt - Unit tests for OverlayManager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.augmentalis.voiceos.speech.confidence.Alternate
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import com.augmentalis.voiceos.speech.confidence.ConfidenceResult
import com.augmentalis.voiceos.speech.confidence.ScoringMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OverlayManager
 */
class OverlayManagerTest {

    private lateinit var context: Context
    private lateinit var windowManager: WindowManager
    private lateinit var overlayManager: OverlayManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        windowManager = mockk(relaxed = true)

        every { context.getSystemService(Context.WINDOW_SERVICE) } returns windowManager
        every { context.applicationContext } returns context

        overlayManager = OverlayManager.getInstance(context)
    }

    @After
    fun teardown() {
        overlayManager.dispose()
    }

    @Test
    fun `test singleton instance`() {
        val instance1 = OverlayManager.getInstance(context)
        val instance2 = OverlayManager.getInstance(context)

        assertSame("OverlayManager should be singleton", instance1, instance2)
    }

    @Test
    fun `test showConfidence adds to active overlays`() {
        val result = createTestConfidenceResult()

        overlayManager.showConfidence(result)

        assertTrue(overlayManager.isOverlayVisible("confidence"))
        assertTrue(overlayManager.isAnyVisible())
    }

    @Test
    fun `test hideConfidence removes from active overlays`() {
        val result = createTestConfidenceResult()

        overlayManager.showConfidence(result)
        overlayManager.hideConfidence()

        assertFalse(overlayManager.isOverlayVisible("confidence"))
    }

    @Test
    fun `test showNumberedSelection adds to active overlays`() {
        val items = createTestSelectableItems()

        overlayManager.showNumberedSelection(items)

        assertTrue(overlayManager.isOverlayVisible("numberedSelection"))
    }

    @Test
    fun `test showCommandStatus adds to active overlays`() {
        overlayManager.showCommandStatus("test command", CommandState.LISTENING)

        assertTrue(overlayManager.isOverlayVisible("commandStatus"))
    }

    @Test
    fun `test showContextMenu adds to active overlays`() {
        val items = createTestMenuItems()

        overlayManager.showContextMenu(items)

        assertTrue(overlayManager.isOverlayVisible("contextMenu"))
    }

    @Test
    fun `test hideAll clears all overlays`() {
        // Show multiple overlays
        overlayManager.showConfidence(createTestConfidenceResult())
        overlayManager.showCommandStatus("test", CommandState.LISTENING)
        overlayManager.showContextMenu(createTestMenuItems())

        // Verify they're active
        assertTrue(overlayManager.isAnyVisible())

        // Hide all
        overlayManager.hideAll()

        // Verify all cleared
        assertFalse(overlayManager.isAnyVisible())
        assertEquals("Success message", 0, overlayManager.getActiveOverlays().size)
    }

    @Test
    fun `test showListening convenience method`() {
        overlayManager.showListening("test partial")

        assertTrue("Error message", overlayManager.isOverlayVisible("commandStatus"))
    }

    @Test
    fun `test showProcessing convenience method`() {
        overlayManager.showProcessing("test command")

        assertTrue(overlayManager.isOverlayVisible("commandStatus"))
    }

    @Test
    fun `test showExecuting convenience method`() {
        overlayManager.showExecuting("test command")

        assertTrue(overlayManager.isOverlayVisible("commandStatus"))
    }

    @Test
    fun `test showSuccess convenience method`() {
        overlayManager.showSuccess("test command")

        assertTrue(overlayManager.isOverlayVisible("commandStatus"))
    }

    @Test
    fun `test showError convenience method`() {
        overlayManager.showError("test command", "test error message")

        assertTrue(overlayManager.isOverlayVisible("commandStatus"))
    }

    @Test
    fun `test getActiveOverlays returns correct set`() {
        overlayManager.showConfidence(createTestConfidenceResult())
        overlayManager.showCommandStatus("test", CommandState.LISTENING)

        val active = overlayManager.getActiveOverlays()

        assertEquals(2, active.size)
        assertTrue(active.contains("confidence"))
        assertTrue(active.contains("commandStatus"))
    }

    @Test
    fun `test showContextMenu hides conflicting overlays`() {
        // Show numbered selection first
        overlayManager.showNumberedSelection(createTestSelectableItems())
        assertTrue(overlayManager.isOverlayVisible("numberedSelection"))

        // Show context menu (should hide numbered selection)
        overlayManager.showContextMenu(createTestMenuItems())

        assertFalse(overlayManager.isOverlayVisible("numberedSelection"))
        assertTrue(overlayManager.isOverlayVisible("contextMenu"))
    }

    @Test
    fun `test showNumberedSelection hides conflicting overlays`() {
        // Show context menu first
        overlayManager.showContextMenu(createTestMenuItems())
        assertTrue(overlayManager.isOverlayVisible("contextMenu"))

        // Show numbered selection (should hide context menu)
        overlayManager.showNumberedSelection(createTestSelectableItems())

        assertFalse(overlayManager.isOverlayVisible("contextMenu"))
        assertTrue(overlayManager.isOverlayVisible("numberedSelection"))
    }

    @Test
    fun `test dispose clears all state`() {
        overlayManager.showConfidence(createTestConfidenceResult())
        overlayManager.showCommandStatus("test", CommandState.LISTENING)

        overlayManager.dispose()

        assertFalse(overlayManager.isAnyVisible())
    }

    // Helper methods

    private fun createTestConfidenceResult(): ConfidenceResult {
        return ConfidenceResult(
            text = "test command",
            confidence = 0.85f,
            level = ConfidenceLevel.HIGH,
            alternates = emptyList(),
            scoringMethod = ScoringMethod.ANDROID_STT
        )
    }

    private fun createTestSelectableItems(): List<SelectableItem> {
        return listOf(
            SelectableItem(
                number = 1,
                label = "Item 1",
                bounds = Rect(0, 0, 100, 100),
                action = {}
            ),
            SelectableItem(
                number = 2,
                label = "Item 2",
                bounds = Rect(0, 100, 100, 200),
                action = {}
            )
        )
    }

    private fun createTestMenuItems(): List<MenuItem> {
        return listOf(
            MenuItem(
                id = "item1",
                label = "Item 1",
                icon = Icons.Default.Check,
                number = 1,
                action = {}
            ),
            MenuItem(
                id = "item2",
                label = "Item 2",
                icon = Icons.Default.Check,
                number = 2,
                action = {}
            )
        )
    }
}
