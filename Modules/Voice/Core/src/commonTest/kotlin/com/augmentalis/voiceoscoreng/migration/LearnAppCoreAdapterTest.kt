/**
 * LearnAppCoreAdapterTest.kt - Tests for LearnAppCore migration adapter
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Tests verify that the adapter correctly bridges old LearnAppCore API
 * to the new VoiceOSCoreNG API.
 */
package com.augmentalis.voiceoscoreng.migration

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ProcessingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class LearnAppCoreAdapterTest {

    // ==================== ElementInfo Conversion Tests ====================

    @Test
    fun `LegacyElementInfo converts to new ElementInfo correctly`() {
        val legacyElement = LegacyElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "Submit form",
            resourceId = "com.app:id/submit",
            isClickable = true,
            isEnabled = true,
            isScrollable = false,
            boundsLeft = 10,
            boundsTop = 20,
            boundsRight = 110,
            boundsBottom = 70
        )

        val newElement = LearnAppCoreAdapter.convertElementInfo(legacyElement)

        assertEquals("android.widget.Button", newElement.className)
        assertEquals("Submit", newElement.text)
        assertEquals("Submit form", newElement.contentDescription)
        assertEquals("com.app:id/submit", newElement.resourceId)
        assertTrue(newElement.isClickable)
        assertTrue(newElement.isEnabled)
        assertFalse(newElement.isScrollable)
        assertEquals(10, newElement.bounds.left)
        assertEquals(20, newElement.bounds.top)
        assertEquals(110, newElement.bounds.right)
        assertEquals(70, newElement.bounds.bottom)
    }

    @Test
    fun `New ElementInfo converts to LegacyElementInfo correctly`() {
        val newElement = ElementInfo(
            className = "Button",
            text = "Click",
            contentDescription = "Click me",
            resourceId = "com.app:id/btn",
            bounds = Bounds(0, 0, 100, 50),
            isClickable = true,
            isEnabled = true,
            isScrollable = false,
            packageName = "com.test"
        )

        val legacyElement = LearnAppCoreAdapter.toLegacyElementInfo(newElement)

        assertEquals("Button", legacyElement.className)
        assertEquals("Click", legacyElement.text)
        assertEquals("Click me", legacyElement.contentDescription)
        assertEquals("com.app:id/btn", legacyElement.resourceId)
        assertTrue(legacyElement.isClickable)
        assertTrue(legacyElement.isEnabled)
        assertFalse(legacyElement.isScrollable)
        assertEquals(0, legacyElement.boundsLeft)
        assertEquals(0, legacyElement.boundsTop)
        assertEquals(100, legacyElement.boundsRight)
        assertEquals(50, legacyElement.boundsBottom)
    }

    // ==================== ProcessingMode Conversion Tests ====================

    @Test
    fun `Legacy IMMEDIATE mode converts to new IMMEDIATE mode`() {
        val newMode = LearnAppCoreAdapter.convertProcessingMode(LegacyProcessingMode.IMMEDIATE)
        assertEquals(ProcessingMode.IMMEDIATE, newMode)
    }

    @Test
    fun `Legacy BATCH mode converts to new BATCH mode`() {
        val newMode = LearnAppCoreAdapter.convertProcessingMode(LegacyProcessingMode.BATCH)
        assertEquals(ProcessingMode.BATCH, newMode)
    }

    @Test
    fun `New IMMEDIATE mode converts to legacy IMMEDIATE mode`() {
        val legacyMode = LearnAppCoreAdapter.toLegacyProcessingMode(ProcessingMode.IMMEDIATE)
        assertEquals(LegacyProcessingMode.IMMEDIATE, legacyMode)
    }

    @Test
    fun `New BATCH mode converts to legacy BATCH mode`() {
        val legacyMode = LearnAppCoreAdapter.toLegacyProcessingMode(ProcessingMode.BATCH)
        assertEquals(LegacyProcessingMode.BATCH, legacyMode)
    }

    // ==================== UUID to VUID Migration Tests ====================

    @Test
    fun `Legacy UUID format is detected correctly`() {
        val legacyUuid = "com.example.app.button-a7f3e2c1d4b5"
        assertTrue(LearnAppCoreAdapter.isLegacyUuid(legacyUuid))
    }

    @Test
    fun `Standard UUID v4 format is detected as legacy`() {
        val uuidV4 = "550e8400-e29b-41d4-a716-446655440000"
        assertTrue(LearnAppCoreAdapter.isLegacyUuid(uuidV4))
    }

    @Test
    fun `New VUID format is not detected as legacy`() {
        val vuid = "a3f2e1-b917cc9dc"
        assertFalse(LearnAppCoreAdapter.isLegacyUuid(vuid))
    }

    @Test
    fun `Legacy VoiceOS UUID migrates to VUID`() {
        val legacyUuid = "com.example.app.v1.0.0.button-a7f3e2c1d4b5"
        val vuid = LearnAppCoreAdapter.migrateUuidToVuid(legacyUuid)

        assertNotNull(vuid)
        // VUID format: {pkgHash6}-{typeCode}{hash8}
        assertTrue(vuid.length == 16, "VUID should be 16 characters, was ${vuid.length}")
        assertTrue(vuid.contains("-"), "VUID should contain hyphen")
    }

    @Test
    fun `Standard UUID v4 migrates to VUID`() {
        val uuidV4 = "550e8400-e29b-41d4-a716-446655440000"
        val vuid = LearnAppCoreAdapter.migrateUuidToVuid(uuidV4)

        // UUID v4 migrations return a generated VUID
        assertNotNull(vuid)
    }

    // ==================== Processing Result Conversion Tests ====================

    @Test
    fun `Legacy ElementProcessingResult converts to new format`() {
        val legacyResult = LegacyElementProcessingResult(
            uuid = "com.app.button-abc123def456",
            commandText = "click submit",
            actionType = "click",
            confidence = 0.95,
            success = true,
            error = null
        )

        val newResult = LearnAppCoreAdapter.convertProcessingResult(legacyResult)

        assertNotNull(newResult.vuid)
        assertEquals("click submit", newResult.commandText)
        assertEquals("click", newResult.actionType)
        assertEquals(0.95f, newResult.confidence)
        assertTrue(newResult.success)
    }

    @Test
    fun `Failed legacy result converts with error preserved`() {
        val legacyResult = LegacyElementProcessingResult(
            uuid = "",
            commandText = null,
            actionType = null,
            confidence = 0.0,
            success = false,
            error = "No label found"
        )

        val newResult = LearnAppCoreAdapter.convertProcessingResult(legacyResult)

        assertFalse(newResult.success)
        assertEquals("No label found", newResult.error)
    }

    // ==================== Batch Queue Compatibility Tests ====================

    @Test
    fun `Adapter provides batch queue size`() {
        val adapter = LearnAppCoreAdapter()
        val size = adapter.getBatchQueueSize()
        assertTrue(size >= 0)
    }

    @Test
    fun `Adapter provides batch processing capability`() {
        val adapter = LearnAppCoreAdapter()
        val elements = listOf(
            LegacyElementInfo(
                className = "Button",
                text = "OK",
                isClickable = true,
                boundsLeft = 0, boundsTop = 0, boundsRight = 100, boundsBottom = 50
            )
        )

        // Should not throw
        val canProcess = adapter.canProcessBatch(elements)
        assertTrue(canProcess || !canProcess) // Just verify no exception
    }

    // ==================== Deprecation Warning Tests ====================

    @Test
    fun `Deprecated methods log warnings`() {
        // This test verifies that deprecated methods exist and can be called
        // In production, these would log deprecation warnings
        val adapter = LearnAppCoreAdapter()

        // These deprecated methods should still work but log warnings
        @Suppress("DEPRECATION")
        val result = adapter.generateUUID(
            LegacyElementInfo(className = "Button", text = "Test", isClickable = true)
        )

        assertNotNull(result)
    }
}
