package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * gRPC-based Plugin Event Bus implementation.
 *
 * Uses SharedFlow for local broadcasting and is designed to integrate with
 * gRPC streaming for cross-process communication (following voiceos.proto pattern).
 *
 * Features:
 * - Configurable replay buffer for late subscribers
 * - Extra buffer capacity for burst handling
 * - Named subscription tracking for debugging/monitoring
 * - Thread-safe subscription management
 *
 * @param replay Number of events to replay to new subscribers (default: 10)
 * @param extraBufferCapacity Additional buffer capacity for high-throughput scenarios (default: 100)
 */
class GrpcPluginEventBus(
    replay: Int = DEFAULT_REPLAY,
    extraBufferCapacity: Int = DEFAULT_BUFFER_CAPACITY
) : PluginEventBus {

    /**
     * Internal SharedFlow for event broadcasting.
     * Configured with replay and extra buffer for handling burst traffic.
     */
    private val _events = MutableSharedFlow<PluginEvent>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity
    )

    /**
     * Mutex for thread-safe subscription management.
     */
    private val mutex = Mutex()

    /**
     * Map of named subscriptions for tracking and debugging.
     * Key: subscription ID, Value: filter being used
     */
    private val subscriptions = mutableMapOf<String, EventFilter>()

    /**
     * Counter for generating unique event IDs when not provided.
     */
    private var eventCounter = 0L

    /**
     * Publish an event to all subscribers.
     *
     * If the event has an empty eventId, a unique ID will be generated.
     *
     * @param event The event to publish
     * @return Number of subscribers that will receive this event (matching the filter)
     */
    override suspend fun publish(event: PluginEvent): Int {
        val eventWithId = if (event.eventId.isEmpty()) {
            event.copy(eventId = generateEventId())
        } else {
            event
        }

        // Emit to the SharedFlow
        _events.emit(eventWithId)

        // Count matching subscribers
        return mutex.withLock {
            subscriptions.values.count { it.matches(eventWithId) }
        }
    }

    /**
     * Subscribe to events with optional filter.
     *
     * @param filter Filter to apply to incoming events
     * @return Flow of matching events
     */
    override fun subscribe(filter: EventFilter): Flow<PluginEvent> {
        return _events.asSharedFlow().filter { filter.matches(it) }
    }

    /**
     * Subscribe to specific event types.
     *
     * @param eventTypes Variable number of event type strings
     * @return Flow of events matching the specified types
     */
    override fun subscribeToTypes(vararg eventTypes: String): Flow<PluginEvent> {
        return subscribe(EventFilter(eventTypes = eventTypes.toSet()))
    }

    /**
     * Subscribe to events from specific plugins.
     *
     * @param pluginIds Variable number of plugin IDs
     * @return Flow of events from the specified plugins
     */
    override fun subscribeToPlugins(vararg pluginIds: String): Flow<PluginEvent> {
        return subscribe(EventFilter(sourcePlugins = pluginIds.toSet()))
    }

    /**
     * Register a named subscription for tracking and debugging.
     *
     * Named subscriptions allow monitoring which plugins are subscribed to which events.
     * This is useful for debugging and understanding event flow in the system.
     *
     * @param subscriptionId Unique identifier for this subscription
     * @param filter The filter being used for this subscription
     */
    suspend fun registerSubscription(subscriptionId: String, filter: EventFilter) {
        mutex.withLock {
            subscriptions[subscriptionId] = filter
        }
    }

    /**
     * Unregister a named subscription.
     *
     * Should be called when a plugin unsubscribes from events.
     *
     * @param subscriptionId The subscription ID to remove
     * @return true if the subscription was found and removed, false otherwise
     */
    suspend fun unregisterSubscription(subscriptionId: String): Boolean {
        return mutex.withLock {
            subscriptions.remove(subscriptionId) != null
        }
    }

    /**
     * Get the current number of registered subscriptions.
     *
     * @return Count of named subscriptions
     */
    suspend fun getSubscriptionCount(): Int {
        return mutex.withLock {
            subscriptions.size
        }
    }

    /**
     * Get all registered subscription IDs.
     *
     * @return Set of subscription IDs
     */
    suspend fun getSubscriptionIds(): Set<String> {
        return mutex.withLock {
            subscriptions.keys.toSet()
        }
    }

    /**
     * Get the filter for a specific subscription.
     *
     * @param subscriptionId The subscription ID to look up
     * @return The filter, or null if not found
     */
    suspend fun getSubscriptionFilter(subscriptionId: String): EventFilter? {
        return mutex.withLock {
            subscriptions[subscriptionId]
        }
    }

    /**
     * Clear all subscriptions.
     *
     * Useful for testing or system reset scenarios.
     */
    suspend fun clearSubscriptions() {
        mutex.withLock {
            subscriptions.clear()
        }
    }

    /**
     * Generate a unique event ID.
     *
     * Uses a combination of timestamp and counter for uniqueness.
     * Thread-safe using mutex lock.
     */
    private suspend fun generateEventId(): String {
        val counter = mutex.withLock {
            eventCounter++
        }
        return "evt_${currentTimeMillis()}_$counter"
    }

    companion object {
        /**
         * Default number of events to replay to new subscribers.
         */
        const val DEFAULT_REPLAY = 10

        /**
         * Default extra buffer capacity for handling burst traffic.
         */
        const val DEFAULT_BUFFER_CAPACITY = 100

        /**
         * Create an event bus with no replay (stateless).
         */
        fun stateless(): GrpcPluginEventBus = GrpcPluginEventBus(replay = 0)

        /**
         * Create an event bus optimized for high throughput.
         */
        fun highThroughput(): GrpcPluginEventBus = GrpcPluginEventBus(
            replay = 50,
            extraBufferCapacity = 500
        )

        /**
         * Create an event bus optimized for low memory usage.
         */
        fun lowMemory(): GrpcPluginEventBus = GrpcPluginEventBus(
            replay = 5,
            extraBufferCapacity = 20
        )
    }
}

/**
 * Extension function to create a PluginEvent with common defaults.
 *
 * @param sourcePluginId The plugin ID emitting this event
 * @param eventType The type of event
 * @param payload Optional key-value payload
 * @param payloadJson Optional JSON payload for complex data
 * @return A new PluginEvent instance
 */
fun createPluginEvent(
    sourcePluginId: String,
    eventType: String,
    payload: Map<String, String> = emptyMap(),
    payloadJson: String? = null
): PluginEvent = PluginEvent(
    eventId = "", // Will be auto-generated on publish
    sourcePluginId = sourcePluginId,
    eventType = eventType,
    timestamp = currentTimeMillis(),
    payload = payload,
    payloadJson = payloadJson
)

/**
 * Extension function to create a state change event.
 *
 * @param pluginId The plugin ID whose state changed
 * @param oldState The previous state (optional)
 * @param newState The new state
 * @return A new PluginEvent for state change
 */
fun createStateChangeEvent(
    pluginId: String,
    newState: String,
    oldState: String? = null
): PluginEvent = PluginEvent(
    eventId = "",
    sourcePluginId = pluginId,
    eventType = PluginEvent.TYPE_STATE_CHANGED,
    timestamp = currentTimeMillis(),
    payload = buildMap {
        put("state", newState)
        if (oldState != null) {
            put("previousState", oldState)
        }
    }
)

/**
 * Extension function to create a health change event.
 *
 * @param pluginId The plugin ID whose health changed
 * @param healthy Whether the plugin is healthy
 * @param message Optional health message
 * @return A new PluginEvent for health change
 */
fun createHealthChangeEvent(
    pluginId: String,
    healthy: Boolean,
    message: String = ""
): PluginEvent = PluginEvent(
    eventId = "",
    sourcePluginId = pluginId,
    eventType = PluginEvent.TYPE_HEALTH_CHANGED,
    timestamp = currentTimeMillis(),
    payload = mapOf(
        "healthy" to healthy.toString(),
        "message" to message
    )
)
