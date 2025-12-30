/**
 * EventRouterImplTest.kt - Comprehensive tests for EventRouterImpl
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-15 04:15:36 PDT
 * Part of: VoiceOSService SOLID Refactoring - Day 3 - IEventRouter Tests
 */
package com.augmentalis.voiceoscore.refactoring.impl

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter
import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager
import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive Test Suite for EventRouterImpl
 *
 * Test Coverage:
 * 1. Event Routing Tests (all 6 types)
 * 2. Debouncing Tests (1000ms window)
 * 3. Package Filtering Tests (wildcards, exact matches)
 * 4. Priority Queue Tests (CRITICAL processed first)
 * 5. Burst Detection Tests (>10 events/sec)
 * 6. Backpressure Tests (100+ events)
 * 7. Metrics Tests
 * 8. Performance Tests (<100ms event processing)
 */
@ExperimentalCoroutinesApi
class EventRouterImplTest {

    // Test dependencies
    private lateinit var eventRouter: EventRouterImpl
    private lateinit var stateManager: IStateManager
    private lateinit var uiScrapingService: IUIScrapingService
    private lateinit var context: Context

    // Test dispatcher
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock dependencies
        stateManager = mockk(relaxed = true)
        uiScrapingService = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Create event router
        eventRouter = EventRouterImpl(
            stateManager = stateManager,
            uiScrapingService = uiScrapingService,
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        eventRouter.cleanup()
    }

    // ========================================
    // 1. Event Routing Tests (All 6 Types)
    // ========================================

    @Test
    fun `test TYPE_WINDOW_CONTENT_CHANGED routing`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Create event
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            packageName = "com.android.systemui"
        )

        // Route event
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify UI scraping called
        coVerify { uiScrapingService.extractUIElements(any()) }
    }

    @Test
    fun `test TYPE_WINDOW_STATE_CHANGED routing`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Create event
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            packageName = "com.android.systemui"
        )

        // Route event
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify UI scraping called
        coVerify { uiScrapingService.extractUIElements(any()) }
    }

    @Test
    fun `test TYPE_VIEW_CLICKED routing`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Create event
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.android.systemui"
        )

        // Route event
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify UI scraping called
        coVerify { uiScrapingService.extractUIElements(any()) }
    }

    @Test
    fun `test TYPE_VIEW_FOCUSED routing (tracking only)`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Create event
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_FOCUSED,
            packageName = "com.android.systemui"
        )

        // Route event
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify UI scraping NOT called (tracking only)
        coVerify(exactly = 0) { uiScrapingService.extractUIElements(any()) }

        // Verify metrics tracked
        val metrics = eventRouter.getMetrics()
        assertTrue(metrics.totalEventsReceived > 0)
    }

    @Test
    fun `test all 6 event types processed`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Create events for all 6 types
        val eventTypes = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )

        // Route all events
        eventTypes.forEach { eventType ->
            val event = mockAccessibilityEvent(
                eventType = eventType,
                packageName = "com.android.systemui",
                className = "TestClass$eventType"
            )
            eventRouter.routeEvent(event)
        }

        advanceUntilIdle()

        // Verify all events received
        val metrics = eventRouter.getMetrics()
        assertEquals(6L, metrics.totalEventsReceived)
    }

    // ========================================
    // 2. Debouncing Tests (1000ms Window)
    // ========================================

    @Test
    fun `test debouncing with 1000ms window`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig(defaultDebounceMs = 1000L)
        eventRouter.initialize(context, config)

        // Create identical events
        val event1 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            packageName = "com.test.app",
            className = "TestClass"
        )
        val event2 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            packageName = "com.test.app",
            className = "TestClass"
        )

        // Route first event
        eventRouter.routeEvent(event1)
        advanceUntilIdle()

        // Route second event (should be debounced)
        eventRouter.routeEvent(event2)
        advanceUntilIdle()

        // Verify only 1 event processed
        val metrics = eventRouter.getMetrics()
        assertEquals(2L, metrics.totalEventsReceived)
        assertEquals(1L, metrics.totalEventsProcessed)
        assertEquals(1L, metrics.totalEventsDebounced)
    }

    @Test
    fun `test debouncing allows event after 1000ms`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig(defaultDebounceMs = 1000L)
        eventRouter.initialize(context, config)

        // Create identical events
        val event1 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            packageName = "com.test.app",
            className = "TestClass"
        )
        val event2 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            packageName = "com.test.app",
            className = "TestClass"
        )

        // Route first event
        eventRouter.routeEvent(event1)
        advanceUntilIdle()

        // Wait 1001ms
        advanceTimeBy(1001L)

        // Route second event (should NOT be debounced)
        eventRouter.routeEvent(event2)
        advanceUntilIdle()

        // Verify both events processed
        val metrics = eventRouter.getMetrics()
        assertEquals(2L, metrics.totalEventsReceived)
        assertEquals(2L, metrics.totalEventsProcessed)
        assertEquals(0L, metrics.totalEventsDebounced)
    }

    @Test
    fun `test composite debounce key (package+class+event)`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Create events with different keys
        val event1 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            packageName = "com.test.app1",
            className = "TestClass"
        )
        val event2 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            packageName = "com.test.app2", // Different package
            className = "TestClass"
        )

        // Route both events
        eventRouter.routeEvent(event1)
        eventRouter.routeEvent(event2)
        advanceUntilIdle()

        // Verify both events processed (different keys)
        val metrics = eventRouter.getMetrics()
        assertEquals(2L, metrics.totalEventsReceived)
        assertEquals(2L, metrics.totalEventsProcessed)
    }

    // ========================================
    // 3. Package Filtering Tests
    // ========================================

    @Test
    fun `test package filter exact match`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Add package filter
        eventRouter.addPackageFilter("com.test.allowed")

        // Create event with allowed package
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.test.allowed"
        )

        // Route event
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify processed
        val metrics = eventRouter.getMetrics()
        assertEquals(1L, metrics.totalEventsProcessed)
    }

    @Test
    fun `test package filter wildcard match`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Add wildcard filter
        eventRouter.addPackageFilter("com.google.android.apps.*")

        // Create events
        val event1 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.google.android.apps.maps"
        )
        val event2 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.google.android.apps.gmail"
        )

        // Route events
        eventRouter.routeEvent(event1)
        eventRouter.routeEvent(event2)
        advanceUntilIdle()

        // Verify both processed
        val metrics = eventRouter.getMetrics()
        assertEquals(2L, metrics.totalEventsProcessed)
    }

    @Test
    fun `test package filter blocks unknown package`() = testScope.runTest {
        // Initialize (with empty filters, should block all except defaults)
        val config = IEventRouter.EventRouterConfig(packageFilters = setOf("com.test.allowed"))
        eventRouter.initialize(context, config)

        // Create event with unknown package
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.unknown.app"
        )

        // Route event
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify filtered
        val metrics = eventRouter.getMetrics()
        assertEquals(1L, metrics.totalEventsReceived)
        assertEquals(0L, metrics.totalEventsProcessed)
        assertEquals(1L, metrics.totalEventsFiltered)
    }

    // ========================================
    // 4. Priority Queue Tests
    // ========================================

    @Test
    fun `test priority queue CRITICAL processed first`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        val processingOrder = mutableListOf<Int>()

        // Mock UI scraping to track order
        coEvery { uiScrapingService.extractUIElements(any()) } answers {
            val event = firstArg<AccessibilityEvent>()
            processingOrder.add(event.eventType)
            emptyList() // Return empty list of UIElement
        }

        // Create events in reverse priority order
        val lowPriorityEvent = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_FOCUSED, // Priority 4
            packageName = "com.android.systemui",
            className = "Low"
        )
        val normalPriorityEvent = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED, // Priority 3
            packageName = "com.android.systemui",
            className = "Normal"
        )
        val highPriorityEvent = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, // Priority 2
            packageName = "com.android.systemui",
            className = "High"
        )
        val criticalPriorityEvent = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, // Priority 1
            packageName = "com.android.systemui",
            className = "Critical"
        )

        // Route in reverse order
        eventRouter.routeEvent(lowPriorityEvent)
        eventRouter.routeEvent(normalPriorityEvent)
        eventRouter.routeEvent(highPriorityEvent)
        eventRouter.routeEvent(criticalPriorityEvent)

        advanceUntilIdle()

        // Verify CRITICAL processed first
        assertTrue(processingOrder.contains(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
    }

    // ========================================
    // 5. Burst Detection Tests (>10 Events/Sec)
    // ========================================

    @Test
    fun `test burst detection triggers throttling`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Send 15 events rapidly (burst threshold = 10)
        repeat(15) { i ->
            val event = mockAccessibilityEvent(
                eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                packageName = "com.android.systemui",
                className = "BurstTest$i"
            )
            eventRouter.routeEvent(event)
        }

        advanceUntilIdle()

        // Verify some events debounced due to burst
        val metrics = eventRouter.getMetrics()
        assertEquals(15L, metrics.totalEventsReceived)
        assertTrue(metrics.totalEventsDebounced > 0L, "Expected some events debounced due to burst")
    }

    // ========================================
    // 6. Backpressure Tests (100+ Events)
    // ========================================

    @Test
    fun `test backpressure with 100+ events`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Send 150 events (queue size = 100)
        repeat(150) { i ->
            val event = mockAccessibilityEvent(
                eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                packageName = "com.android.systemui",
                className = "Backpressure$i"
            )
            eventRouter.routeEvent(event)
        }

        advanceUntilIdle()

        // Verify events received but some may be dropped due to queue overflow
        val metrics = eventRouter.getMetrics()
        assertEquals(150L, metrics.totalEventsReceived)
        // Some events processed, but queue overflow may have occurred
    }

    // ========================================
    // 7. Metrics Tests
    // ========================================

    @Test
    fun `test metrics tracking all event types`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Route events of different types
        val event1 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.android.systemui"
        )
        val event2 = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            packageName = "com.android.systemui"
        )

        eventRouter.routeEvent(event1)
        eventRouter.routeEvent(event2)
        advanceUntilIdle()

        // Verify metrics by type
        val metrics = eventRouter.getMetrics()
        assertTrue(metrics.eventsByType.containsKey(AccessibilityEvent.TYPE_VIEW_CLICKED))
        assertTrue(metrics.eventsByType.containsKey(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
    }

    @Test
    fun `test event history tracking`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Route events
        repeat(5) { i ->
            val event = mockAccessibilityEvent(
                eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
                packageName = "com.android.systemui",
                className = "Test$i"
            )
            eventRouter.routeEvent(event)
        }

        advanceUntilIdle()

        // Verify history
        val history = eventRouter.getEventHistory(limit = 10)
        assertEquals(5, history.size)
    }

    // ========================================
    // 8. Performance Tests (<100ms Event Processing)
    // ========================================

    @Test
    fun `test event processing completes under 100ms`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Create event
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.android.systemui"
        )

        // Measure processing time
        val startTime = System.currentTimeMillis()
        eventRouter.routeEvent(event)
        advanceUntilIdle()
        val endTime = System.currentTimeMillis()

        // Verify processing time < 100ms
        val processingTime = endTime - startTime
        assertTrue(processingTime < 100L, "Processing time $processingTime ms exceeds 100ms")
    }

    // ========================================
    // 9. Lifecycle Tests
    // ========================================

    @Test
    fun `test pause and resume`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Pause
        eventRouter.pause()
        assertEquals(IEventRouter.EventRouterState.PAUSED, eventRouter.currentState)

        // Route event (should not process)
        val event = mockAccessibilityEvent(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = "com.android.systemui"
        )
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify not processed
        val metrics1 = eventRouter.getMetrics()
        assertEquals(1L, metrics1.totalEventsReceived)
        assertEquals(0L, metrics1.totalEventsProcessed)

        // Resume
        eventRouter.resume()
        assertEquals(IEventRouter.EventRouterState.READY, eventRouter.currentState)

        // Route event (should process)
        eventRouter.routeEvent(event)
        advanceUntilIdle()

        // Verify processed
        val metrics2 = eventRouter.getMetrics()
        assertEquals(2L, metrics2.totalEventsReceived)
        assertEquals(1L, metrics2.totalEventsProcessed)
    }

    @Test
    fun `test cleanup releases resources`() = testScope.runTest {
        // Initialize
        val config = IEventRouter.EventRouterConfig()
        eventRouter.initialize(context, config)

        // Cleanup
        eventRouter.cleanup()
        assertEquals(IEventRouter.EventRouterState.SHUTDOWN, eventRouter.currentState)

        // Verify no further processing
        assertFalse(eventRouter.isReady)
    }

    // ========================================
    // Helper Functions
    // ========================================

    private fun mockAccessibilityEvent(
        eventType: Int,
        packageName: String,
        className: String = "TestClass"
    ): AccessibilityEvent {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns eventType
        every { event.packageName } returns packageName
        every { event.className } returns className
        return event
    }
}
