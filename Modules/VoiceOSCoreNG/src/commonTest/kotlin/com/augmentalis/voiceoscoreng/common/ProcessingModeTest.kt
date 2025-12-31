package com.augmentalis.voiceoscoreng.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for ProcessingMode enum.
 *
 * ProcessingMode defines how the VoiceOS accessibility engine processes UI elements:
 * - IMMEDIATE: JIT mode for real-time, one-at-a-time processing
 * - BATCH: Exploration mode for collecting and processing elements in batches
 */
class ProcessingModeTest {

    // ==================== Enum Values Existence Tests ====================

    @Test
    fun `IMMEDIATE mode should exist`() {
        val mode = ProcessingMode.IMMEDIATE
        assertNotNull(mode)
        assertEquals("IMMEDIATE", mode.name)
    }

    @Test
    fun `BATCH mode should exist`() {
        val mode = ProcessingMode.BATCH
        assertNotNull(mode)
        assertEquals("BATCH", mode.name)
    }

    @Test
    fun `should have exactly two enum values`() {
        val entries = ProcessingMode.entries
        assertEquals(2, entries.size)
    }

    // ==================== Description Property Tests ====================

    @Test
    fun `IMMEDIATE mode should have correct description`() {
        val mode = ProcessingMode.IMMEDIATE
        assertEquals("JIT mode - process elements one at a time", mode.description)
    }

    @Test
    fun `BATCH mode should have correct description`() {
        val mode = ProcessingMode.BATCH
        assertEquals("Exploration mode - collect and process in batches", mode.description)
    }

    // ==================== MaxBatchSize Property Tests ====================

    @Test
    fun `IMMEDIATE mode should have maxBatchSize of 1`() {
        val mode = ProcessingMode.IMMEDIATE
        assertEquals(1, mode.maxBatchSize)
    }

    @Test
    fun `BATCH mode should have maxBatchSize of 100`() {
        val mode = ProcessingMode.BATCH
        assertEquals(100, mode.maxBatchSize)
    }

    @Test
    fun `maxBatchSize should be positive for all modes`() {
        ProcessingMode.entries.forEach { mode ->
            assertTrue(mode.maxBatchSize > 0, "maxBatchSize for ${mode.name} should be positive")
        }
    }

    // ==================== TimeoutMs Property Tests ====================

    @Test
    fun `IMMEDIATE mode should have short timeout`() {
        val mode = ProcessingMode.IMMEDIATE
        // IMMEDIATE mode needs fast response - 100ms timeout
        assertEquals(100L, mode.timeoutMs)
    }

    @Test
    fun `BATCH mode should have longer timeout`() {
        val mode = ProcessingMode.BATCH
        // BATCH mode can take more time - 5000ms (5 seconds) timeout
        assertEquals(5000L, mode.timeoutMs)
    }

    @Test
    fun `timeoutMs should be positive for all modes`() {
        ProcessingMode.entries.forEach { mode ->
            assertTrue(mode.timeoutMs > 0L, "timeoutMs for ${mode.name} should be positive")
        }
    }

    // ==================== valueOf Tests ====================

    @Test
    fun `valueOf should return IMMEDIATE for IMMEDIATE string`() {
        val mode = ProcessingMode.valueOf("IMMEDIATE")
        assertEquals(ProcessingMode.IMMEDIATE, mode)
    }

    @Test
    fun `valueOf should return BATCH for BATCH string`() {
        val mode = ProcessingMode.valueOf("BATCH")
        assertEquals(ProcessingMode.BATCH, mode)
    }

    @Test
    fun `valueOf should throw for invalid string`() {
        var exceptionThrown = false
        try {
            ProcessingMode.valueOf("INVALID")
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown, "valueOf should throw IllegalArgumentException for invalid value")
    }

    // ==================== Entries Iteration Tests ====================

    @Test
    fun `entries should contain IMMEDIATE`() {
        val entries = ProcessingMode.entries
        assertTrue(entries.contains(ProcessingMode.IMMEDIATE))
    }

    @Test
    fun `entries should contain BATCH`() {
        val entries = ProcessingMode.entries
        assertTrue(entries.contains(ProcessingMode.BATCH))
    }

    @Test
    fun `entries should be iterable`() {
        var count = 0
        ProcessingMode.entries.forEach { _ ->
            count++
        }
        assertEquals(2, count)
    }

    @Test
    fun `entries should maintain declaration order`() {
        val entries = ProcessingMode.entries.toList()
        assertEquals(ProcessingMode.IMMEDIATE, entries[0])
        assertEquals(ProcessingMode.BATCH, entries[1])
    }

    // ==================== Ordinal Tests ====================

    @Test
    fun `IMMEDIATE should have ordinal 0`() {
        assertEquals(0, ProcessingMode.IMMEDIATE.ordinal)
    }

    @Test
    fun `BATCH should have ordinal 1`() {
        assertEquals(1, ProcessingMode.BATCH.ordinal)
    }

    // ==================== Property Consistency Tests ====================

    @Test
    fun `IMMEDIATE mode properties should be consistent with JIT semantics`() {
        val mode = ProcessingMode.IMMEDIATE
        // JIT means: small batch (1), fast timeout, immediate processing
        assertEquals(1, mode.maxBatchSize)
        assertTrue(mode.timeoutMs <= 500L, "IMMEDIATE timeout should be 500ms or less for JIT responsiveness")
    }

    @Test
    fun `BATCH mode properties should be consistent with exploration semantics`() {
        val mode = ProcessingMode.BATCH
        // Exploration means: large batch, longer timeout for comprehensive processing
        assertTrue(mode.maxBatchSize >= 50, "BATCH maxBatchSize should be at least 50 for exploration")
        assertTrue(mode.timeoutMs >= 1000L, "BATCH timeout should be at least 1000ms for exploration")
    }
}
