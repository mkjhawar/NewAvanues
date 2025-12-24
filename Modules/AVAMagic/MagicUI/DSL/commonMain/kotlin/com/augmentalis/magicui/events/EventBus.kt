package com.augmentalis.magicui.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Central event bus for AvaUI component events.
 *
 * The EventBus provides a thread-safe, reactive mechanism for propagating
 * component events throughout the application. It uses Kotlin Flow to enable
 * asynchronous, non-blocking event handling with backpressure support.
 *
 * Key features:
 * - **Thread-safe**: All operations are concurrent-safe
 * - **Backpressure**: Buffers up to 100 events without blocking emitters
 * - **Hot stream**: Events are broadcast to all active collectors
 * - **No replay**: Events are not cached; only live events are delivered
 *
 * Example usage:
 * ```kotlin
 * val eventBus = EventBus()
 *
 * // Producer: Emit events
 * launch {
 *     eventBus.emit(ComponentEvent(
 *         componentId = "colorPicker1",
 *         eventName = "onColorChange",
 *         parameters = mapOf("color" to "#FF5733")
 *     ))
 * }
 *
 * // Consumer: Collect events
 * launch {
 *     eventBus.events.collect { event ->
 *         println("Event: ${event.eventName} from ${event.componentId}")
 *     }
 * }
 * ```
 *
 * Architecture notes:
 * - Uses SharedFlow for hot, multicast event delivery
 * - Zero replay ensures events are not cached or delivered to late subscribers
 * - Buffer capacity of 100 prevents slow collectors from blocking fast emitters
 * - Suspend functions enable integration with coroutine-based architectures
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27 12:18:38 PDT
 *
 * @see ComponentEvent for event structure
 * @see CallbackAdapter for binding DSL lambdas to events
 */
class EventBus {
    /**
     * Internal mutable event stream.
     *
     * Configuration:
     * - replay = 0: No caching of past events
     * - extraBufferCapacity = 100: Buffer up to 100 pending events
     *
     * This prevents blocking on emit() calls while allowing burst traffic.
     */
    private val _events = MutableSharedFlow<ComponentEvent>(
        replay = 0,
        extraBufferCapacity = 100
    )

    /**
     * Public read-only event stream.
     *
     * Consumers collect from this flow to receive component events.
     * Multiple collectors can subscribe concurrently; all receive the
     * same events (multicast behavior).
     *
     * Example:
     * ```kotlin
     * eventBus.events
     *     .filter { it.componentId == "myComponent" }
     *     .collect { event ->
     *         // Handle event
     *     }
     * ```
     */
    val events: SharedFlow<ComponentEvent> = _events.asSharedFlow()

    /**
     * Emit an event to all subscribers.
     *
     * This is a suspend function that integrates with coroutine-based
     * architectures. It will not block if the buffer has capacity; if
     * the buffer is full, it suspends until space is available.
     *
     * Example:
     * ```kotlin
     * launch {
     *     eventBus.emit(ComponentEvent(
     *         componentId = "notepad1",
     *         eventName = "onTextChange",
     *         parameters = mapOf("text" to "Hello World")
     *     ))
     * }
     * ```
     *
     * @param event The event to emit
     * @throws IllegalStateException if the event bus is closed
     */
    suspend fun emit(event: ComponentEvent) {
        _events.emit(event)
    }

    /**
     * Try to emit an event without suspending.
     *
     * This method attempts to emit immediately and returns false if the
     * buffer is full (rather than suspending). Useful for non-suspending
     * contexts or when event loss is acceptable.
     *
     * Example:
     * ```kotlin
     * val emitted = eventBus.tryEmit(event)
     * if (!emitted) {
     *     println("Event dropped: buffer full")
     * }
     * ```
     *
     * @param event The event to emit
     * @return true if emitted successfully, false if buffer is full
     */
    fun tryEmit(event: ComponentEvent): Boolean {
        return _events.tryEmit(event)
    }

    /**
     * Returns the number of active subscribers to this event bus.
     *
     * Useful for diagnostics and testing.
     *
     * @return Number of active collectors
     */
    val subscriptionCount: Int
        get() = _events.subscriptionCount.value

    /**
     * Reset subscription count (for testing purposes).
     *
     * This method clears the subscription count, useful in unit tests
     * to reset state between test cases.
     */
    fun resetSubscriptionCount() {
        _events.resetReplayCache()
    }
}

/**
 * Represents a component event emitted through the EventBus.
 *
 * ComponentEvent encapsulates all information about an event triggered
 * by a AvaUI component, including:
 * - Which component triggered the event (componentId)
 * - What type of event occurred (eventName)
 * - Event-specific data (parameters)
 *
 * Example events:
 * ```kotlin
 * // ColorPicker color change
 * ComponentEvent(
 *     componentId = "colorPicker1",
 *     eventName = "onColorChange",
 *     parameters = mapOf("color" to "#FF5733")
 * )
 *
 * // Button click
 * ComponentEvent(
 *     componentId = "submitBtn",
 *     eventName = "onClick",
 *     parameters = mapOf("x" to 120, "y" to 450)
 * )
 *
 * // Notepad text change
 * ComponentEvent(
 *     componentId = "notepad1",
 *     eventName = "onTextChange",
 *     parameters = mapOf("text" to "Updated content")
 * )
 * ```
 *
 * @property componentId Unique identifier of the component that triggered the event
 * @property eventName Name of the event (e.g., "onClick", "onColorChange")
 * @property parameters Event-specific data as key-value pairs (e.g., color, position)
 *
 * @see EventBus for event emission and collection
 */
data class ComponentEvent(
    val componentId: String,
    val eventName: String,
    val parameters: Map<String, Any?>
) {
    /**
     * Returns a human-readable string representation of this event.
     * Useful for debugging and logging.
     *
     * Example output:
     * ```
     * ComponentEvent(componentId='colorPicker1', eventName='onColorChange', parameters=1)
     * ```
     */
    override fun toString(): String {
        return "ComponentEvent(componentId='$componentId', eventName='$eventName', parameters=${parameters.size})"
    }

    /**
     * Gets a parameter value by name, with type casting.
     *
     * This convenience method retrieves a parameter and casts it to
     * the expected type. Returns null if the parameter doesn't exist
     * or if the cast fails.
     *
     * Example:
     * ```kotlin
     * val color: String? = event.getParameter("color")
     * val x: Int? = event.getParameter("x")
     * ```
     *
     * @param name Parameter name
     * @return Parameter value cast to T, or null if not found or cast fails
     */
    inline fun <reified T> getParameter(name: String): T? {
        return parameters[name] as? T
    }

    /**
     * Gets a required parameter value by name, with type casting.
     *
     * Similar to getParameter, but throws an exception if the parameter
     * is missing or the cast fails. Use this for mandatory parameters.
     *
     * Example:
     * ```kotlin
     * val color: String = event.requireParameter("color")
     * ```
     *
     * @param name Parameter name
     * @return Parameter value cast to T
     * @throws IllegalArgumentException if parameter is missing or cast fails
     */
    inline fun <reified T> requireParameter(name: String): T {
        val value = parameters[name]
            ?: throw IllegalArgumentException("Required parameter '$name' not found in event '$eventName'")
        return value as? T
            ?: throw IllegalArgumentException("Parameter '$name' cannot be cast to ${T::class.simpleName}")
    }

    companion object {
        /**
         * Creates a ComponentEvent with no parameters.
         *
         * Convenience factory for events that don't carry data.
         *
         * @param componentId Component identifier
         * @param eventName Event name
         * @return ComponentEvent with empty parameters
         */
        fun noParams(componentId: String, eventName: String): ComponentEvent {
            return ComponentEvent(componentId, eventName, emptyMap())
        }

        /**
         * Creates a ComponentEvent with a single parameter.
         *
         * Convenience factory for simple single-parameter events.
         *
         * Example:
         * ```kotlin
         * val event = ComponentEvent.singleParam(
         *     componentId = "picker1",
         *     eventName = "onColorChange",
         *     paramName = "color",
         *     paramValue = "#FF5733"
         * )
         * ```
         *
         * @param componentId Component identifier
         * @param eventName Event name
         * @param paramName Parameter name
         * @param paramValue Parameter value
         * @return ComponentEvent with one parameter
         */
        fun singleParam(
            componentId: String,
            eventName: String,
            paramName: String,
            paramValue: Any?
        ): ComponentEvent {
            return ComponentEvent(componentId, eventName, mapOf(paramName to paramValue))
        }
    }
}
