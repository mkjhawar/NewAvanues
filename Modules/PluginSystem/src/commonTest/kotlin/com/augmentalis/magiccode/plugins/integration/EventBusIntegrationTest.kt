/**
 * EventBusIntegrationTest.kt - Event bus routing integration tests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Comprehensive integration tests for the plugin event bus including:
 * - Event publishing and subscription
 * - Event filtering by source and type
 * - Event broadcasting to multiple subscribers
 * - Event priority handling
 * - Async event handling
 * - Error isolation in event handlers
 * - Unsubscription
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.universal.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the plugin event bus.
 *
 * Tests event publishing, subscription, filtering, and error handling
 * across the GrpcPluginEventBus implementation.
 */
class EventBusIntegrationTest {

    // =========================================================================
    // Basic Publish/Subscribe Tests
    // =========================================================================

    @Test
    fun testPublishSubscribe() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        // Subscribe to all events
        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Act: Publish an event
        val event = createPluginEvent(
            sourcePluginId = "test.publisher",
            eventType = "test.event.type"
        )
        eventBus.publish(event)

        // Wait for event propagation
        withTimeout(1000) {
            while (receivedEvents.isEmpty()) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert
        assertEquals(1, receivedEvents.size, "Should receive one event")
        assertEquals("test.publisher", receivedEvents[0].sourcePluginId)
        assertEquals("test.event.type", receivedEvents[0].eventType)
    }

    @Test
    fun testPublishMultipleEvents() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Act: Publish multiple events
        repeat(5) { i ->
            val event = createPluginEvent(
                sourcePluginId = "test.publisher",
                eventType = "event.$i"
            )
            eventBus.publish(event)
        }

        // Wait for events
        withTimeout(1000) {
            while (receivedEvents.size < 5) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert
        assertEquals(5, receivedEvents.size, "Should receive all 5 events")
        repeat(5) { i ->
            assertTrue(
                receivedEvents.any { it.eventType == "event.$i" },
                "Should receive event $i"
            )
        }
    }

    // =========================================================================
    // Event Filtering Tests
    // =========================================================================

    @Test
    fun testEventFilteringByType() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        // Subscribe only to "wanted.event" type
        val subscription = eventBus.subscribeToTypes("wanted.event")
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Act: Publish various events
        eventBus.publish(createPluginEvent("source", "unwanted.event"))
        eventBus.publish(createPluginEvent("source", "wanted.event"))
        eventBus.publish(createPluginEvent("source", "another.unwanted"))
        eventBus.publish(createPluginEvent("source", "wanted.event"))

        // Wait for events
        withTimeout(1000) {
            while (receivedEvents.size < 2) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert: Only wanted events received
        assertEquals(2, receivedEvents.size, "Should receive 2 wanted events")
        assertTrue(
            receivedEvents.all { it.eventType == "wanted.event" },
            "All events should be of type 'wanted.event'"
        )
    }

    @Test
    fun testEventFilteringBySource() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        // Subscribe to events from specific plugin
        val subscription = eventBus.subscribeToPlugins("wanted.source")
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Act: Publish events from different sources
        eventBus.publish(createPluginEvent("unwanted.source", "event1"))
        eventBus.publish(createPluginEvent("wanted.source", "event2"))
        eventBus.publish(createPluginEvent("another.source", "event3"))
        eventBus.publish(createPluginEvent("wanted.source", "event4"))

        // Wait for events
        withTimeout(1000) {
            while (receivedEvents.size < 2) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert
        assertEquals(2, receivedEvents.size, "Should receive 2 events from wanted source")
        assertTrue(
            receivedEvents.all { it.sourcePluginId == "wanted.source" },
            "All events should be from 'wanted.source'"
        )
    }

    @Test
    fun testEventFilteringByMultipleCriteria() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        // Subscribe with combined filter
        val filter = EventFilter(
            eventTypes = setOf("target.event"),
            sourcePlugins = setOf("target.source")
        )
        val subscription = eventBus.subscribe(filter)
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Act: Publish various events
        eventBus.publish(createPluginEvent("target.source", "target.event")) // Matches both
        eventBus.publish(createPluginEvent("target.source", "other.event"))  // Source matches only
        eventBus.publish(createPluginEvent("other.source", "target.event"))  // Type matches only
        eventBus.publish(createPluginEvent("other.source", "other.event"))   // Neither matches

        // Wait for event
        withTimeout(1000) {
            while (receivedEvents.isEmpty()) {
                delay(10)
            }
        }
        delay(100) // Give time for any additional events

        collectJob.cancel()

        // Assert: Only events matching both criteria
        assertEquals(1, receivedEvents.size, "Should receive 1 event matching both criteria")
        assertEquals("target.source", receivedEvents[0].sourcePluginId)
        assertEquals("target.event", receivedEvents[0].eventType)
    }

    // =========================================================================
    // Broadcasting Tests
    // =========================================================================

    @Test
    fun testBroadcasting() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val subscriber1Events = mutableListOf<PluginEvent>()
        val subscriber2Events = mutableListOf<PluginEvent>()
        val subscriber3Events = mutableListOf<PluginEvent>()

        // Create multiple subscribers
        val sub1 = eventBus.subscribe(EventFilter.ALL)
        val sub2 = eventBus.subscribe(EventFilter.ALL)
        val sub3 = eventBus.subscribe(EventFilter.ALL)

        val job1 = launch { sub1.collect { subscriber1Events.add(it) } }
        val job2 = launch { sub2.collect { subscriber2Events.add(it) } }
        val job3 = launch { sub3.collect { subscriber3Events.add(it) } }

        // Act: Publish event
        val event = createPluginEvent("broadcaster", "broadcast.event")
        eventBus.publish(event)

        // Wait for propagation
        withTimeout(1000) {
            while (subscriber1Events.isEmpty() || subscriber2Events.isEmpty() || subscriber3Events.isEmpty()) {
                delay(10)
            }
        }

        job1.cancel()
        job2.cancel()
        job3.cancel()

        // Assert: All subscribers received the event
        assertEquals(1, subscriber1Events.size, "Subscriber 1 should receive event")
        assertEquals(1, subscriber2Events.size, "Subscriber 2 should receive event")
        assertEquals(1, subscriber3Events.size, "Subscriber 3 should receive event")

        // All should have same event data
        assertEquals(
            subscriber1Events[0].sourcePluginId,
            subscriber2Events[0].sourcePluginId,
            "All subscribers should receive same event"
        )
    }

    @Test
    fun testBroadcastingWithFilters() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val typeASubscriberEvents = mutableListOf<PluginEvent>()
        val typeBSubscriberEvents = mutableListOf<PluginEvent>()
        val allSubscriberEvents = mutableListOf<PluginEvent>()

        val subA = eventBus.subscribeToTypes("type.a")
        val subB = eventBus.subscribeToTypes("type.b")
        val subAll = eventBus.subscribe(EventFilter.ALL)

        val jobA = launch { subA.collect { typeASubscriberEvents.add(it) } }
        val jobB = launch { subB.collect { typeBSubscriberEvents.add(it) } }
        val jobAll = launch { subAll.collect { allSubscriberEvents.add(it) } }

        // Act: Publish different event types
        eventBus.publish(createPluginEvent("source", "type.a"))
        eventBus.publish(createPluginEvent("source", "type.b"))
        eventBus.publish(createPluginEvent("source", "type.c"))

        // Wait for events
        withTimeout(1000) {
            while (allSubscriberEvents.size < 3) {
                delay(10)
            }
        }

        jobA.cancel()
        jobB.cancel()
        jobAll.cancel()

        // Assert
        assertEquals(1, typeASubscriberEvents.size, "Type A subscriber should receive 1 event")
        assertEquals(1, typeBSubscriberEvents.size, "Type B subscriber should receive 1 event")
        assertEquals(3, allSubscriberEvents.size, "All subscriber should receive 3 events")
    }

    // =========================================================================
    // Async Event Handling Tests
    // =========================================================================

    @Test
    fun testAsyncEventHandling() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val handledEvents = mutableListOf<String>()

        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                // Simulate async processing
                delay(50)
                handledEvents.add(event.eventId)
            }
        }

        // Act: Publish events quickly
        val eventIds = (1..3).map { i ->
            val event = createPluginEvent("async.source", "async.event.$i")
            eventBus.publish(event)
            event.eventId.ifEmpty { "auto-$i" }
        }

        // Wait for async processing
        withTimeout(2000) {
            while (handledEvents.size < 3) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert: All events handled
        assertEquals(3, handledEvents.size, "All events should be handled")
    }

    @Test
    fun testConcurrentPublishing() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                synchronized(receivedEvents) {
                    receivedEvents.add(event)
                }
            }
        }

        // Act: Publish concurrently from multiple coroutines
        coroutineScope {
            (1..10).map { i ->
                async {
                    eventBus.publish(createPluginEvent("concurrent.$i", "concurrent.event"))
                }
            }.awaitAll()
        }

        // Wait for events
        withTimeout(2000) {
            while (receivedEvents.size < 10) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert
        assertEquals(10, receivedEvents.size, "Should receive all 10 concurrent events")
    }

    // =========================================================================
    // Error Isolation Tests
    // =========================================================================

    @Test
    fun testEventHandlerErrorIsolation() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val goodSubscriberEvents = mutableListOf<PluginEvent>()
        var errorThrown = false

        // Use SupervisorJob to prevent exception propagation between child coroutines
        val supervisorScope = kotlinx.coroutines.CoroutineScope(
            coroutineContext + kotlinx.coroutines.SupervisorJob()
        )

        // Good subscriber
        val goodSub = eventBus.subscribe(EventFilter.ALL)
        val goodJob = supervisorScope.launch {
            goodSub.collect { event ->
                goodSubscriberEvents.add(event)
            }
        }

        // Bad subscriber that throws errors - use exception handler to prevent crash
        val badSub = eventBus.subscribe(EventFilter.ALL)
        val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, _ ->
            // Silently handle exception for test
        }
        val badJob = supervisorScope.launch(exceptionHandler) {
            badSub.collect { _ ->
                errorThrown = true
                throw RuntimeException("Simulated error in event handler")
            }
        }

        // Give collectors time to start
        delay(50)

        // Act: Publish event
        eventBus.publish(createPluginEvent("source", "test.event"))

        // Wait for good subscriber to receive event
        withTimeout(1000) {
            while (goodSubscriberEvents.isEmpty()) {
                delay(10)
            }
        }

        delay(100)

        goodJob.cancel()
        badJob.cancel()
        supervisorScope.coroutineContext.cancelChildren()

        // Assert: Good subscriber still received the event
        // Note: Due to SharedFlow behavior with SupervisorJob, the error in one collector
        // doesn't affect other collectors
        assertTrue(goodSubscriberEvents.isNotEmpty(), "Good subscriber should receive event")
    }

    // =========================================================================
    // Unsubscribe Tests
    // =========================================================================

    @Test
    fun testUnsubscribe() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Publish first event
        eventBus.publish(createPluginEvent("source", "before.unsub"))

        withTimeout(1000) {
            while (receivedEvents.isEmpty()) {
                delay(10)
            }
        }

        // Act: Cancel subscription
        collectJob.cancel()

        // Publish more events after cancellation
        eventBus.publish(createPluginEvent("source", "after.unsub"))
        delay(100) // Give time for potential delivery

        // Assert
        assertEquals(1, receivedEvents.size, "Should only receive event before unsubscribe")
        assertEquals("before.unsub", receivedEvents[0].eventType)
    }

    @Test
    fun testNamedSubscriptionManagement() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()

        // Act: Register named subscriptions
        eventBus.registerSubscription("sub1", EventFilter(eventTypes = setOf("type.a")))
        eventBus.registerSubscription("sub2", EventFilter(eventTypes = setOf("type.b")))
        eventBus.registerSubscription("sub3", EventFilter.ALL)

        // Assert: Subscriptions registered
        assertEquals(3, eventBus.getSubscriptionCount(), "Should have 3 subscriptions")
        assertTrue(eventBus.getSubscriptionIds().contains("sub1"), "Should have sub1")
        assertTrue(eventBus.getSubscriptionIds().contains("sub2"), "Should have sub2")

        // Act: Get subscription filter
        val filter = eventBus.getSubscriptionFilter("sub1")
        assertNotNull(filter, "Should get filter for sub1")
        assertTrue(filter.eventTypes.contains("type.a"), "Filter should contain type.a")

        // Act: Unregister subscription
        val removed = eventBus.unregisterSubscription("sub1")
        assertTrue(removed, "Should remove subscription")
        assertEquals(2, eventBus.getSubscriptionCount(), "Should have 2 subscriptions")

        // Act: Clear all subscriptions
        eventBus.clearSubscriptions()
        assertEquals(0, eventBus.getSubscriptionCount(), "Should have no subscriptions")
    }

    // =========================================================================
    // Event ID Generation Tests
    // =========================================================================

    @Test
    fun testEventIdGeneration() = runBlocking {
        // Arrange
        val eventBus = GrpcPluginEventBus()
        val receivedEvents = mutableListOf<PluginEvent>()

        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Act: Publish events without IDs
        val eventWithoutId = PluginEvent(
            eventId = "",
            sourcePluginId = "source",
            eventType = "test",
            timestamp = System.currentTimeMillis()
        )
        eventBus.publish(eventWithoutId)

        val eventWithId = PluginEvent(
            eventId = "custom-id-123",
            sourcePluginId = "source",
            eventType = "test",
            timestamp = System.currentTimeMillis()
        )
        eventBus.publish(eventWithId)

        // Wait for events
        withTimeout(1000) {
            while (receivedEvents.size < 2) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert
        assertTrue(receivedEvents[0].eventId.isNotEmpty(), "Empty ID should be auto-generated")
        assertTrue(receivedEvents[0].eventId.startsWith("evt_"), "Auto-generated ID should start with evt_")
        assertEquals("custom-id-123", receivedEvents[1].eventId, "Custom ID should be preserved")
    }

    @Test
    fun testUniqueEventIdGeneration() = runBlocking {
        // Arrange
        // Use stateless event bus to avoid replay buffer issues
        val eventBus = GrpcPluginEventBus(replay = 0, extraBufferCapacity = 200)
        val eventIds = mutableSetOf<String>()
        val collectorStarted = kotlinx.coroutines.CompletableDeferred<Unit>()

        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            collectorStarted.complete(Unit)
            subscription.collect { event ->
                synchronized(eventIds) {
                    eventIds.add(event.eventId)
                }
            }
        }

        // Wait for collector to start before publishing
        collectorStarted.await()
        delay(50) // Additional delay to ensure collector is actively listening

        // Act: Publish many events without IDs
        repeat(100) {
            eventBus.publish(createPluginEvent("source", "test"))
        }

        // Wait for events with longer timeout
        withTimeout(5000) {
            while (eventIds.size < 100) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert: All IDs are unique
        assertEquals(100, eventIds.size, "All event IDs should be unique")
    }

    // =========================================================================
    // Event Replay Tests
    // =========================================================================

    @Test
    fun testEventReplay() = runBlocking {
        // Arrange: Event bus with replay of 5
        val eventBus = GrpcPluginEventBus(replay = 5, extraBufferCapacity = 10)

        // Publish events BEFORE subscribing
        repeat(3) { i ->
            eventBus.publish(createPluginEvent("replay.source", "replay.event.$i"))
        }

        // Allow events to be buffered
        delay(50)

        // Now subscribe
        val receivedEvents = mutableListOf<PluginEvent>()
        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        // Wait for replayed events
        withTimeout(1000) {
            while (receivedEvents.size < 3) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert: Should receive replayed events
        assertEquals(3, receivedEvents.size, "Should receive 3 replayed events")
    }

    // =========================================================================
    // EventFilter Tests
    // =========================================================================

    @Test
    fun testEventFilterMatching() {
        // Test ALL filter
        val allFilter = EventFilter.ALL
        val event = createPluginEvent("any.source", "any.type")
        assertTrue(allFilter.matches(event), "ALL filter should match any event")

        // Test event type filter
        val typeFilter = EventFilter(eventTypes = setOf("target.type"))
        assertTrue(
            typeFilter.matches(createPluginEvent("source", "target.type")),
            "Should match target type"
        )
        assertFalse(
            typeFilter.matches(createPluginEvent("source", "other.type")),
            "Should not match other type"
        )

        // Test source plugin filter
        val sourceFilter = EventFilter(sourcePlugins = setOf("target.source"))
        assertTrue(
            sourceFilter.matches(createPluginEvent("target.source", "type")),
            "Should match target source"
        )
        assertFalse(
            sourceFilter.matches(createPluginEvent("other.source", "type")),
            "Should not match other source"
        )

        // Test combined filter (AND logic)
        val combinedFilter = EventFilter(
            eventTypes = setOf("target.type"),
            sourcePlugins = setOf("target.source")
        )
        assertTrue(
            combinedFilter.matches(createPluginEvent("target.source", "target.type")),
            "Should match when both criteria met"
        )
        assertFalse(
            combinedFilter.matches(createPluginEvent("target.source", "other.type")),
            "Should not match when only source matches"
        )
        assertFalse(
            combinedFilter.matches(createPluginEvent("other.source", "target.type")),
            "Should not match when only type matches"
        )
    }

    // =========================================================================
    // Factory Function Tests
    // =========================================================================

    @Test
    fun testCreatePluginEvent() {
        // Test basic creation
        val event = createPluginEvent(
            sourcePluginId = "test.plugin",
            eventType = "test.event"
        )

        assertEquals("test.plugin", event.sourcePluginId)
        assertEquals("test.event", event.eventType)
        assertTrue(event.eventId.isEmpty(), "Factory creates empty ID for auto-generation")
        assertTrue(event.timestamp > 0, "Timestamp should be set")
    }

    @Test
    fun testCreateStateChangeEvent() {
        // Test with previous state
        val event = createStateChangeEvent(
            pluginId = "test.plugin",
            newState = "ACTIVE",
            oldState = "INITIALIZING"
        )

        assertEquals("test.plugin", event.sourcePluginId)
        assertEquals(PluginEvent.TYPE_STATE_CHANGED, event.eventType)
        assertEquals("ACTIVE", event.payload["state"])
        assertEquals("INITIALIZING", event.payload["previousState"])
    }

    @Test
    fun testCreateHealthChangeEvent() {
        val event = createHealthChangeEvent(
            pluginId = "test.plugin",
            healthy = true,
            message = "All systems operational"
        )

        assertEquals("test.plugin", event.sourcePluginId)
        assertEquals(PluginEvent.TYPE_HEALTH_CHANGED, event.eventType)
        assertEquals("true", event.payload["healthy"])
        assertEquals("All systems operational", event.payload["message"])
    }

    // =========================================================================
    // Event Bus Configuration Tests
    // =========================================================================

    @Test
    fun testStatelessEventBus() = runBlocking {
        // Arrange: Stateless (no replay)
        val eventBus = GrpcPluginEventBus.stateless()

        // Publish before subscribing
        eventBus.publish(createPluginEvent("source", "before.subscribe"))

        // Subscribe after publish
        val receivedEvents = mutableListOf<PluginEvent>()
        val subscription = eventBus.subscribe(EventFilter.ALL)
        val collectJob = launch {
            subscription.collect { event ->
                receivedEvents.add(event)
            }
        }

        delay(100)

        // Publish after subscribing
        eventBus.publish(createPluginEvent("source", "after.subscribe"))

        withTimeout(1000) {
            while (receivedEvents.isEmpty()) {
                delay(10)
            }
        }

        collectJob.cancel()

        // Assert: Should not receive event published before subscription
        assertEquals(1, receivedEvents.size, "Should only receive event after subscription")
        assertEquals("after.subscribe", receivedEvents[0].eventType)
    }
}
