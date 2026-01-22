/**
 * PluginEventBus.kt - Plugin event bus interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the event bus interface for inter-plugin communication.
 * Types (PluginEvent, EventFilter) are defined in PluginTypes.kt
 */
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.Flow

/**
 * Plugin Event Bus for inter-plugin communication.
 *
 * Provides pub/sub messaging between plugins using reactive Flows.
 * Uses gRPC streaming under the hood via UniversalRPC for cross-process
 * communication.
 *
 * ## Usage Example
 * ```kotlin
 * // Publishing events
 * val event = PluginEvent(
 *     eventId = "evt_123",
 *     sourcePluginId = "com.example.myplugin",
 *     eventType = PluginEvent.TYPE_CAPABILITY_AVAILABLE,
 *     payload = mapOf("capabilityId" to "speech.recognition")
 * )
 * val subscriberCount = eventBus.publish(event)
 *
 * // Subscribing to events
 * eventBus.subscribeToTypes(PluginEvent.TYPE_VOICE_COMMAND)
 *     .collect { event ->
 *         handleVoiceCommand(event)
 *     }
 * ```
 *
 * @see GrpcPluginEventBus for the default implementation
 * @see PluginEvent
 * @see EventFilter
 * @since 1.0.0
 */
interface PluginEventBus {

    /**
     * Publish an event to all subscribers.
     *
     * The event is dispatched to all subscribers whose filters match the event.
     * This is an asynchronous operation that completes when all local subscribers
     * have been notified.
     *
     * @param event The event to publish
     * @return Number of subscribers that received the event
     */
    suspend fun publish(event: PluginEvent): Int

    /**
     * Subscribe to events with optional filter.
     *
     * Returns a Flow that emits events matching the provided filter.
     * The Flow is hot and will continue emitting events until cancelled.
     *
     * @param filter Filter to apply to incoming events (default: ALL - receive all events)
     * @return Flow of matching events
     * @see EventFilter
     */
    fun subscribe(filter: EventFilter = EventFilter.ALL): Flow<PluginEvent>

    /**
     * Subscribe to specific event types.
     *
     * Convenience method for subscribing to events by type.
     *
     * @param eventTypes Variable number of event type strings to subscribe to
     * @return Flow of events matching the specified types
     */
    fun subscribeToTypes(vararg eventTypes: String): Flow<PluginEvent>

    /**
     * Subscribe to events from specific plugins.
     *
     * Convenience method for subscribing to events by source plugin.
     *
     * @param pluginIds Variable number of plugin IDs to subscribe to
     * @return Flow of events from the specified plugins
     */
    fun subscribeToPlugins(vararg pluginIds: String): Flow<PluginEvent>
}
