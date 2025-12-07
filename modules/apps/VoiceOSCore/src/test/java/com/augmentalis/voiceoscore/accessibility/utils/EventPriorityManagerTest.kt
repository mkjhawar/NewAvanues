/**
 * EventPriorityManagerTest.kt - Tests for adaptive event filtering
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-11-13
 *
 * Purpose: Verify EventPriorityManager correctly filters events based on memory pressure
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.view.accessibility.AccessibilityEvent
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test suite for EventPriorityManager
 *
 * Verifies:
 * 1. Event classification (CRITICAL, HIGH, MEDIUM, LOW)
 * 2. Filtering logic under different memory pressure levels
 * 3. Metrics tracking
 * 4. Edge cases
 */
class EventPriorityManagerTest {

    private lateinit var manager: EventPriorityManager
    private lateinit var mockEvent: AccessibilityEvent

    @Before
    fun setup() {
        manager = EventPriorityManager()
        mockEvent = mock()
    }

    @After
    fun teardown() {
        manager.resetMetrics()
    }

    // ========== Event Classification Tests ==========

    @Test
    fun `VIEW_CLICKED is classified as CRITICAL`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.CRITICAL, priority)
    }

    @Test
    fun `VIEW_LONG_CLICKED is classified as CRITICAL`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.CRITICAL, priority)
    }

    @Test
    fun `VIEW_TEXT_CHANGED is classified as CRITICAL`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.CRITICAL, priority)
    }

    @Test
    fun `WINDOW_STATE_CHANGED is classified as HIGH`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.HIGH, priority)
    }

    @Test
    fun `WINDOW_CONTENT_CHANGED is classified as MEDIUM`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.MEDIUM, priority)
    }

    @Test
    fun `VIEW_SCROLLED is classified as LOW`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.LOW, priority)
    }

    @Test
    fun `VIEW_FOCUSED is classified as LOW`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.LOW, priority)
    }

    @Test
    fun `VIEW_SELECTED is classified as LOW`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SELECTED)
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.LOW, priority)
    }

    @Test
    fun `unknown event type is classified as LOW`() {
        whenever(mockEvent.eventType).thenReturn(99999) // Unknown type
        
        val priority = manager.getEventPriority(mockEvent)
        
        assertEquals(EventPriority.LOW, priority)
    }

    // ========== Filtering Logic Tests ==========

    @Test
    fun `CRITICAL events always processed regardless of memory pressure`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        
        // Test all pressure levels
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.LOW))
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM))
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.HIGH))
    }

    @Test
    fun `HIGH priority events dropped only under HIGH memory pressure`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.LOW))
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM))
        assertFalse(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.HIGH))
    }

    @Test
    fun `MEDIUM priority events dropped under MEDIUM and HIGH memory pressure`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.LOW))
        assertFalse(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM))
        assertFalse(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.HIGH))
    }

    @Test
    fun `LOW priority events dropped under any memory pressure`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
        assertFalse(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.LOW))
        assertFalse(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM))
        assertFalse(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.HIGH))
    }

    @Test
    fun `all events processed when no memory pressure`() {
        // Test all priority levels
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
        
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
        
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
        
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        assertTrue(manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE))
    }

    // ========== Metrics Tests ==========

    @Test
    fun `metrics track processed events correctly`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        
        // Process 5 critical events with no pressure
        repeat(5) {
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE)
        }
        
        val metrics = manager.getMetrics()
        
        assertEquals(5L, metrics.totalProcessed)
        assertEquals(0L, metrics.totalDropped)
        assertEquals(0, metrics.overallDropRate)
    }

    @Test
    fun `metrics track dropped events correctly`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        
        // Try to process 5 low-priority events under high pressure (all dropped)
        repeat(5) {
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.HIGH)
        }
        
        val metrics = manager.getMetrics()
        
        assertEquals(0L, metrics.totalProcessed)
        assertEquals(5L, metrics.totalDropped)
        assertEquals(100, metrics.overallDropRate)
    }

    @Test
    fun `metrics calculate drop rate by priority correctly`() {
        // Process mixed events under medium pressure
        
        // 3 critical events (all processed)
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        repeat(3) {
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM)
        }
        
        // 2 high events (all processed)
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        repeat(2) {
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM)
        }
        
        // 4 medium events (all dropped)
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        repeat(4) {
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM)
        }
        
        // 5 low events (all dropped)
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        repeat(5) {
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM)
        }
        
        val metrics = manager.getMetrics()
        
        // Total: 5 processed (3 critical + 2 high), 9 dropped (4 medium + 5 low)
        assertEquals(5L, metrics.totalProcessed)
        assertEquals(9L, metrics.totalDropped)
        
        // Drop rates by priority
        val criticalStats = metrics.dropRateByPriority[EventPriority.CRITICAL]!!
        assertEquals(3L, criticalStats.processed)
        assertEquals(0L, criticalStats.dropped)
        assertEquals(0, criticalStats.dropRate)
        
        val highStats = metrics.dropRateByPriority[EventPriority.HIGH]!!
        assertEquals(2L, highStats.processed)
        assertEquals(0L, highStats.dropped)
        assertEquals(0, highStats.dropRate)
        
        val mediumStats = metrics.dropRateByPriority[EventPriority.MEDIUM]!!
        assertEquals(0L, mediumStats.processed)
        assertEquals(4L, mediumStats.dropped)
        assertEquals(100, mediumStats.dropRate)
        
        val lowStats = metrics.dropRateByPriority[EventPriority.LOW]!!
        assertEquals(0L, lowStats.processed)
        assertEquals(5L, lowStats.dropped)
        assertEquals(100, lowStats.dropRate)
    }

    @Test
    fun `resetMetrics clears all counters`() {
        whenever(mockEvent.eventType).thenReturn(AccessibilityEvent.TYPE_VIEW_CLICKED)
        
        // Process some events
        repeat(10) {
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE)
        }
        
        // Reset
        manager.resetMetrics()
        
        // Verify reset
        val metrics = manager.getMetrics()
        assertEquals(0L, metrics.totalProcessed)
        assertEquals(0L, metrics.totalDropped)
    }

    @Test
    fun `metrics with no events return zero stats`() {
        val metrics = manager.getMetrics()
        
        assertEquals(0L, metrics.totalProcessed)
        assertEquals(0L, metrics.totalDropped)
        assertEquals(0, metrics.overallDropRate)
        
        // All priority stats should be zero
        EventPriority.values().forEach { priority ->
            val stats = metrics.dropRateByPriority[priority]!!
            assertEquals(0L, stats.processed)
            assertEquals(0L, stats.dropped)
            assertEquals(0, stats.dropRate)
        }
    }

    // ========== Edge Cases ==========

    @Test
    fun `multiple event types processed correctly under varying pressure`() {
        // Simulate realistic event stream under medium pressure
        val events = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,           // CRITICAL - processed
            AccessibilityEvent.TYPE_VIEW_SCROLLED,          // LOW - dropped
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,   // HIGH - processed
            AccessibilityEvent.TYPE_VIEW_FOCUSED,           // LOW - dropped
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,      // CRITICAL - processed
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, // MEDIUM - dropped
            AccessibilityEvent.TYPE_VIEW_SCROLLED,          // LOW - dropped
            AccessibilityEvent.TYPE_VIEW_CLICKED            // CRITICAL - processed
        )
        
        events.forEach { eventType ->
            whenever(mockEvent.eventType).thenReturn(eventType)
            manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.MEDIUM)
        }
        
        val metrics = manager.getMetrics()
        
        // Expected: 4 processed (2 clicks + 1 window state + 1 text), 4 dropped (3 scrolls + 1 content)
        assertEquals(4L, metrics.totalProcessed)
        assertEquals(4L, metrics.totalDropped)
        assertEquals(50, metrics.overallDropRate) // 50% dropped
    }

    @Test
    fun `event type names mapped correctly`() {
        assertEquals("VIEW_CLICKED", EventTypeNames.getName(AccessibilityEvent.TYPE_VIEW_CLICKED))
        assertEquals("VIEW_SCROLLED", EventTypeNames.getName(AccessibilityEvent.TYPE_VIEW_SCROLLED))
        assertEquals("WINDOW_STATE_CHANGED", EventTypeNames.getName(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
        assertEquals("UNKNOWN(99999)", EventTypeNames.getName(99999))
    }

    // ========== Integration-like Tests ==========

    @Test
    fun `high memory pressure scenario - only critical events processed`() {
        val eventTypes = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        
        var processed = 0
        var dropped = 0
        
        eventTypes.forEach { eventType ->
            whenever(mockEvent.eventType).thenReturn(eventType)
            if (manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.HIGH)) {
                processed++
            } else {
                dropped++
            }
        }
        
        // Only 2 critical events should be processed (clicked, text changed)
        assertEquals(2, processed)
        assertEquals(3, dropped)
    }

    @Test
    fun `no memory pressure scenario - all events processed`() {
        val eventTypes = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        )
        
        var processed = 0
        
        eventTypes.forEach { eventType ->
            whenever(mockEvent.eventType).thenReturn(eventType)
            if (manager.shouldProcessEvent(mockEvent, ResourceMonitor.ThrottleLevel.NONE)) {
                processed++
            }
        }
        
        // All events should be processed
        assertEquals(5, processed)
    }
}
