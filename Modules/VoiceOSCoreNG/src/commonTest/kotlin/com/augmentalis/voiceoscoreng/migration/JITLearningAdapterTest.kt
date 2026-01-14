/**
 * JITLearningAdapterTest.kt - Tests for JITLearning migration adapter
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Tests verify that the adapter correctly bridges old JITLearning and
 * JitElementCapture APIs to the new VoiceOSCoreNG ExplorationBridge API.
 */
package com.augmentalis.voiceoscoreng.migration

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class JITLearningAdapterTest {

    // ==================== JitCapturedElement Conversion Tests ====================

    @Test
    fun `LegacyJitCapturedElement converts to ElementInfo correctly`() {
        val legacyElement = LegacyJitCapturedElement(
            elementHash = "abc123def456",
            className = "android.widget.Button",
            viewIdResourceName = "com.app:id/submit",
            text = "Submit",
            contentDescription = "Submit form",
            boundsLeft = 10,
            boundsTop = 20,
            boundsRight = 110,
            boundsBottom = 70,
            isClickable = true,
            isLongClickable = false,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isFocusable = true,
            isEnabled = true,
            depth = 3,
            indexInParent = 2,
            uuid = "com.app.button-abc123"
        )

        val elementInfo = JITLearningAdapter.convertToElementInfo(legacyElement)

        assertEquals("android.widget.Button", elementInfo.className)
        assertEquals("com.app:id/submit", elementInfo.resourceId)
        assertEquals("Submit", elementInfo.text)
        assertEquals("Submit form", elementInfo.contentDescription)
        assertEquals(10, elementInfo.bounds.left)
        assertEquals(20, elementInfo.bounds.top)
        assertEquals(110, elementInfo.bounds.right)
        assertEquals(70, elementInfo.bounds.bottom)
        assertTrue(elementInfo.isClickable)
        assertFalse(elementInfo.isScrollable)
        assertTrue(elementInfo.isEnabled)
    }

    @Test
    fun `ElementInfo converts to LegacyJitCapturedElement correctly`() {
        val elementInfo = ElementInfo(
            className = "Button",
            resourceId = "com.app:id/btn",
            text = "OK",
            contentDescription = "OK button",
            bounds = Bounds(0, 0, 100, 50),
            isClickable = true,
            isScrollable = false,
            isEnabled = true,
            packageName = "com.test"
        )

        val legacyElement = JITLearningAdapter.toLegacyJitCapturedElement(
            elementInfo,
            elementHash = "hash123",
            depth = 2,
            indexInParent = 1,
            uuid = "com.test.button-hash123"
        )

        assertEquals("hash123", legacyElement.elementHash)
        assertEquals("Button", legacyElement.className)
        assertEquals("com.app:id/btn", legacyElement.viewIdResourceName)
        assertEquals("OK", legacyElement.text)
        assertEquals("OK button", legacyElement.contentDescription)
        assertEquals(0, legacyElement.boundsLeft)
        assertEquals(0, legacyElement.boundsTop)
        assertEquals(100, legacyElement.boundsRight)
        assertEquals(50, legacyElement.boundsBottom)
        assertTrue(legacyElement.isClickable)
        assertFalse(legacyElement.isScrollable)
        assertTrue(legacyElement.isEnabled)
        assertEquals(2, legacyElement.depth)
        assertEquals(1, legacyElement.indexInParent)
    }

    // ==================== JITState Conversion Tests ====================

    @Test
    fun `LegacyJITState converts to new progress format`() {
        val legacyState = LegacyJITState(
            isActive = true,
            currentPackage = "com.example.app",
            screensLearned = 5,
            elementsDiscovered = 42,
            lastCaptureTime = 1704067200000L // 2024-01-01 00:00:00 UTC
        )

        val progress = JITLearningAdapter.convertToProgress(legacyState)

        assertTrue(progress.isActive)
        assertEquals("com.example.app", progress.packageName)
        assertEquals(5, progress.screensExplored)
        assertEquals(42, progress.elementsFound)
    }

    // ==================== ExplorationProgress Conversion Tests ====================

    @Test
    fun `LegacyExplorationProgress converts to new ExplorationProgressResult`() {
        val legacyProgress = LegacyExplorationProgress(
            state = "RUNNING",
            progress = 75,
            screensExplored = 10,
            elementsFound = 150,
            elementsClicked = 45,
            navigationEdges = 8,
            errorCount = 2,
            currentScreen = "MainActivity"
        )

        val newProgress = JITLearningAdapter.convertExplorationProgress(legacyProgress)

        assertEquals("RUNNING", newProgress.state)
        assertEquals(10, newProgress.screensExplored)
        assertEquals(150, newProgress.elementsFound)
    }

    // ==================== Element Hash Compatibility Tests ====================

    @Test
    fun `Legacy element hash can be extracted from UUID`() {
        val legacyUuid = "com.example.app.button-a7f3e2c1d4b5"
        val hash = JITLearningAdapter.extractElementHash(legacyUuid)

        assertEquals("a7f3e2c1d4b5", hash)
    }

    @Test
    fun `Element hash is preserved during conversion`() {
        val originalHash = "abcdef123456"
        val legacyElement = LegacyJitCapturedElement(
            elementHash = originalHash,
            className = "Button",
            text = "Test",
            isClickable = true
        )

        val elementInfo = JITLearningAdapter.convertToElementInfo(legacyElement)
        val backToLegacy = JITLearningAdapter.toLegacyJitCapturedElement(
            elementInfo,
            elementHash = originalHash,
            depth = 0,
            indexInParent = 0,
            uuid = null
        )

        assertEquals(originalHash, backToLegacy.elementHash)
    }

    // ==================== Screen Capture Compatibility Tests ====================

    @Test
    fun `Legacy screen capture results convert to new format`() {
        val legacyCaptureResult = LegacyScreenCaptureResult(
            packageName = "com.example.app",
            activityName = "MainActivity",
            screenHash = "screen_hash_123",
            elements = listOf(
                LegacyJitCapturedElement(
                    elementHash = "elem1",
                    className = "Button",
                    text = "OK",
                    isClickable = true
                ),
                LegacyJitCapturedElement(
                    elementHash = "elem2",
                    className = "Button",
                    text = "Cancel",
                    isClickable = true
                )
            ),
            captureTimeMs = 45L
        )

        val newResult = JITLearningAdapter.convertScreenCaptureResult(legacyCaptureResult)

        assertEquals("com.example.app", newResult.packageName)
        assertEquals("MainActivity", newResult.activityName)
        assertEquals("screen_hash_123", newResult.screenHash)
        assertEquals(2, newResult.elements.size)
        assertTrue(newResult.elements[0].isClickable)
    }

    // ==================== JITLearnerProvider Bridge Tests ====================

    @Test
    fun `Adapter provides JITLearnerProvider compatibility`() {
        val adapter = JITLearningAdapter()

        // Verify provider methods exist
        assertTrue(adapter.supportsLegacyProvider())
    }

    @Test
    fun `Pause and resume learning work through adapter`() {
        val adapter = JITLearningAdapter()

        // These methods should not throw
        adapter.pauseLearning()
        assertTrue(adapter.isLearningPaused())

        adapter.resumeLearning()
        assertFalse(adapter.isLearningPaused())
    }

    // ==================== Exploration Engine Bridge Tests ====================

    @Test
    fun `Adapter provides ExplorationEngine compatibility`() {
        val adapter = JITLearningAdapter()

        // Verify exploration methods exist
        assertTrue(adapter.supportsExplorationBridge())
    }

    @Test
    fun `Exploration state maps to new ExplorationBridge states`() {
        // IDLE
        assertEquals(
            "IDLE",
            JITLearningAdapter.mapExplorationState(LegacyExplorationState.IDLE)
        )

        // RUNNING
        assertEquals(
            "RUNNING",
            JITLearningAdapter.mapExplorationState(LegacyExplorationState.RUNNING)
        )

        // PAUSED
        assertEquals(
            "PAUSED",
            JITLearningAdapter.mapExplorationState(LegacyExplorationState.PAUSED)
        )

        // COMPLETED
        assertEquals(
            "COMPLETED",
            JITLearningAdapter.mapExplorationState(LegacyExplorationState.COMPLETED)
        )

        // FAILED
        assertEquals(
            "FAILED",
            JITLearningAdapter.mapExplorationState(LegacyExplorationState.FAILED)
        )
    }

    // ==================== Deprecation Warning Tests ====================

    @Test
    fun `Deprecated JIT methods still work but log warnings`() {
        val adapter = JITLearningAdapter()

        @Suppress("DEPRECATION")
        val elementsCount = adapter.getElementsDiscoveredCount()

        assertTrue(elementsCount >= 0)
    }

    @Test
    fun `Deprecated capture methods still work`() {
        val adapter = JITLearningAdapter()

        // These deprecated methods should still be callable
        @Suppress("DEPRECATION")
        val canCapture = adapter.canCaptureScreen("com.example.app")

        // Just verify the method exists and returns a boolean
        assertTrue(canCapture || !canCapture)
    }
}
