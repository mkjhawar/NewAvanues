/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Unit tests for StatusIndicatorState (SOLID refactoring)
 */

package com.augmentalis.chat.state

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StatusIndicatorState.
 *
 * Tests cover:
 * - Model loaded state (NLU, LLM)
 * - NLU ready state
 * - Last responder tracking
 * - LLM fallback state
 * - Convenience methods
 * - Status summary
 *
 * Note: Tests for flash mode are skipped as they require Android Context
 * and DeveloperPreferences integration.
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-18
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatusIndicatorStateTest {

    private lateinit var mockContext: Context
    private lateinit var statusIndicatorState: StatusIndicatorState

    @Before
    fun setUp() {
        // Mock android.util.Log static methods
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockContext = mockk(relaxed = true)
        // Mock SharedPreferences for DeveloperPreferences
        val mockSharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPrefs

        statusIndicatorState = StatusIndicatorState(mockContext)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // ==================== Model Loaded State Tests ====================

    @Test
    fun `initial NLU loaded state is false`() {
        assertFalse(statusIndicatorState.isNLULoaded.value)
    }

    @Test
    fun `initial LLM loaded state is true`() {
        // LLM defaults to true (assumed always loaded via API)
        assertTrue(statusIndicatorState.isLLMLoaded.value)
    }

    @Test
    fun `setNLULoaded updates state`() {
        statusIndicatorState.setNLULoaded(true)
        assertTrue(statusIndicatorState.isNLULoaded.value)

        statusIndicatorState.setNLULoaded(false)
        assertFalse(statusIndicatorState.isNLULoaded.value)
    }

    @Test
    fun `setLLMLoaded updates state`() {
        statusIndicatorState.setLLMLoaded(false)
        assertFalse(statusIndicatorState.isLLMLoaded.value)

        statusIndicatorState.setLLMLoaded(true)
        assertTrue(statusIndicatorState.isLLMLoaded.value)
    }

    // ==================== NLU Ready State Tests ====================

    @Test
    fun `initial NLU ready state is false`() {
        assertFalse(statusIndicatorState.isNLUReady.value)
    }

    @Test
    fun `setNLUReady updates state`() {
        statusIndicatorState.setNLUReady(true)
        assertTrue(statusIndicatorState.isNLUReady.value)

        statusIndicatorState.setNLUReady(false)
        assertFalse(statusIndicatorState.isNLUReady.value)
    }

    // ==================== Last Responder Tests ====================

    @Test
    fun `initial last responder is null`() {
        assertNull(statusIndicatorState.lastResponder.value)
    }

    @Test
    fun `initial last responder timestamp is zero`() {
        assertEquals(0L, statusIndicatorState.lastResponderTimestamp.value)
    }

    @Test
    fun `setLastResponder updates responder and timestamp`() {
        val beforeTimestamp = System.currentTimeMillis()

        statusIndicatorState.setLastResponder("NLU")

        val afterTimestamp = System.currentTimeMillis()

        assertEquals("NLU", statusIndicatorState.lastResponder.value)
        assertTrue(statusIndicatorState.lastResponderTimestamp.value >= beforeTimestamp)
        assertTrue(statusIndicatorState.lastResponderTimestamp.value <= afterTimestamp)
    }

    @Test
    fun `clearLastResponder sets responder to null`() {
        statusIndicatorState.setLastResponder("NLU")
        statusIndicatorState.clearLastResponder()

        assertNull(statusIndicatorState.lastResponder.value)
        // Note: timestamp is NOT reset by clearLastResponder
    }

    @Test
    fun `isActiveHighlightVisible returns true within duration`() {
        statusIndicatorState.setLastResponder("NLU")

        // Immediately after setting, should be visible
        assertTrue(statusIndicatorState.isActiveHighlightVisible())
    }

    @Test
    fun `isActiveHighlightVisible returns false when timestamp is zero`() {
        // Initial state - no responder set
        assertFalse(statusIndicatorState.isActiveHighlightVisible())
    }

    // ==================== LLM Fallback Tests ====================

    @Test
    fun `initial LLM fallback invoked is false`() {
        assertFalse(statusIndicatorState.llmFallbackInvoked.value)
    }

    @Test
    fun `setLLMFallbackInvoked updates state`() {
        statusIndicatorState.setLLMFallbackInvoked(true)
        assertTrue(statusIndicatorState.llmFallbackInvoked.value)

        statusIndicatorState.setLLMFallbackInvoked(false)
        assertFalse(statusIndicatorState.llmFallbackInvoked.value)
    }

    @Test
    fun `resetLLMFallbackInvoked sets to false`() {
        statusIndicatorState.setLLMFallbackInvoked(true)
        statusIndicatorState.resetLLMFallbackInvoked()

        assertFalse(statusIndicatorState.llmFallbackInvoked.value)
    }

    // ==================== Convenience Methods Tests ====================

    @Test
    fun `markNLUResponded sets responder to NLU`() {
        statusIndicatorState.markNLUResponded()

        assertEquals("NLU", statusIndicatorState.lastResponder.value)
        assertTrue(statusIndicatorState.lastResponderTimestamp.value > 0)
    }

    @Test
    fun `markLLMResponded sets responder to LLM`() {
        statusIndicatorState.markLLMResponded()

        assertEquals("LLM", statusIndicatorState.lastResponder.value)
        assertTrue(statusIndicatorState.lastResponderTimestamp.value > 0)
    }

    @Test
    fun `markLLMFallbackResponded sets fallback and responder`() {
        statusIndicatorState.markLLMFallbackResponded()

        assertTrue(statusIndicatorState.llmFallbackInvoked.value)
        assertEquals("LLM", statusIndicatorState.lastResponder.value)
    }

    @Test
    fun `resetForNewMessage resets fallback flag`() {
        statusIndicatorState.setLLMFallbackInvoked(true)
        statusIndicatorState.resetForNewMessage()

        assertFalse(statusIndicatorState.llmFallbackInvoked.value)
    }

    // ==================== Status Summary Tests ====================

    @Test
    fun `getStatusSummary returns formatted summary`() {
        statusIndicatorState.setNLULoaded(true)
        statusIndicatorState.setLLMLoaded(true)
        statusIndicatorState.setNLUReady(true)
        statusIndicatorState.setLastResponder("NLU")
        statusIndicatorState.setLLMFallbackInvoked(false)

        val summary = statusIndicatorState.getStatusSummary()

        assertTrue(summary.contains("NLU Loaded: true"))
        assertTrue(summary.contains("LLM Loaded: true"))
        assertTrue(summary.contains("NLU Ready: true"))
        assertTrue(summary.contains("Last Responder: NLU"))
        assertTrue(summary.contains("LLM Fallback: false"))
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `multiple rapid responder changes track latest`() {
        statusIndicatorState.markNLUResponded()
        val nluTimestamp = statusIndicatorState.lastResponderTimestamp.value

        // Simulate rapid change
        Thread.sleep(10)
        statusIndicatorState.markLLMResponded()
        val llmTimestamp = statusIndicatorState.lastResponderTimestamp.value

        assertEquals("LLM", statusIndicatorState.lastResponder.value)
        assertTrue(llmTimestamp >= nluTimestamp)
    }

    @Test
    fun `state transitions NLU not ready to ready`() {
        // Initial state
        assertFalse(statusIndicatorState.isNLUReady.value)
        assertFalse(statusIndicatorState.isNLULoaded.value)

        // NLU loading
        statusIndicatorState.setNLULoaded(true)
        assertTrue(statusIndicatorState.isNLULoaded.value)
        assertFalse(statusIndicatorState.isNLUReady.value)

        // NLU ready
        statusIndicatorState.setNLUReady(true)
        assertTrue(statusIndicatorState.isNLULoaded.value)
        assertTrue(statusIndicatorState.isNLUReady.value)
    }

    @Test
    fun `state transitions NLU ready to not loaded`() {
        // Start with ready state
        statusIndicatorState.setNLULoaded(true)
        statusIndicatorState.setNLUReady(true)

        // Simulate unload
        statusIndicatorState.setNLUReady(false)
        statusIndicatorState.setNLULoaded(false)

        assertFalse(statusIndicatorState.isNLULoaded.value)
        assertFalse(statusIndicatorState.isNLUReady.value)
    }
}
