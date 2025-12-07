/**
 * VoiceCursorEventHandler.kt - Handles voice cursor events and commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.util.Log
import com.augmentalis.voiceoscore.accessibility.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Cursor event types
 */
sealed class CursorEvent {
    /**
     * Move cursor event
     */
    data class Move(
        val direction: Direction,
        val distance: Float = DEFAULT_MOVE_DISTANCE
    ) : CursorEvent()

    /**
     * MoveTo absolute position event
     */
    data class MoveTo(
        val x: Float,
        val y: Float
    ) : CursorEvent()

    /**
     * Click event
     */
    data class Click(
        val x: Float? = null, // null = current cursor position
        val y: Float? = null
    ) : CursorEvent()

    /**
     * Long press event
     */
    data class LongPress(
        val x: Float? = null,
        val y: Float? = null,
        val duration: Long = DEFAULT_LONG_PRESS_DURATION
    ) : CursorEvent()

    /**
     * Drag event
     */
    data class Drag(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val duration: Long = DEFAULT_DRAG_DURATION
    ) : CursorEvent()

    /**
     * Center cursor event
     */
    object Center : CursorEvent()

    /**
     * Show cursor event
     */
    object Show : CursorEvent()

    /**
     * Hide cursor event
     */
    object Hide : CursorEvent()

    /**
     * Toggle cursor visibility event
     */
    object Toggle : CursorEvent()

    companion object {
        const val DEFAULT_MOVE_DISTANCE = 100f // pixels
        const val DEFAULT_LONG_PRESS_DURATION = 1000L // ms
        const val DEFAULT_DRAG_DURATION = 500L // ms
    }
}

/**
 * Movement direction enum
 */
enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UP_LEFT,
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_RIGHT
}

/**
 * Event result
 */
data class EventResult(
    val success: Boolean,
    val event: CursorEvent,
    val message: String? = null,
    val executionTime: Long = 0
)

/**
 * Event configuration
 *
 * @param debounceMs Debounce duration for event processing
 * @param maxQueueSize Maximum event queue size
 * @param moveDistance Default move distance in pixels
 */
data class EventConfig(
    val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    val maxQueueSize: Int = DEFAULT_MAX_QUEUE_SIZE,
    val moveDistance: Float = CursorEvent.DEFAULT_MOVE_DISTANCE
) {
    companion object {
        const val DEFAULT_DEBOUNCE_MS = 100L
        const val DEFAULT_MAX_QUEUE_SIZE = 50
    }
}

/**
 * Voice cursor event handler
 *
 * Handles cursor events with:
 * - Event dispatching (move, click, long-press, drag)
 * - Voice command processing
 * - Event queue management with debouncing
 * - Callback registration
 * - Async event processing via coroutines
 */
class VoiceCursorEventHandler(
    private val config: EventConfig = EventConfig()
) {
    companion object {
        private const val TAG = "VoiceCursorEventHandler"

        // Supported voice commands
        private val DIRECTION_COMMANDS = mapOf(
            "up" to Direction.UP,
            "down" to Direction.DOWN,
            "left" to Direction.LEFT,
            "right" to Direction.RIGHT,
            "up left" to Direction.UP_LEFT,
            "up right" to Direction.UP_RIGHT,
            "down left" to Direction.DOWN_LEFT,
            "down right" to Direction.DOWN_RIGHT
        )

        private val ACTION_COMMANDS = setOf(
            "click", "tap",
            "long press", "hold",
            "center", "center cursor",
            "show", "show cursor",
            "hide", "hide cursor",
            "toggle", "toggle cursor"
        )
    }

    // Coroutine scope for async event processing
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Event queue
    private val eventQueue = ConcurrentLinkedQueue<CursorEvent>()

    // Event debouncer
    private val debouncer = Debouncer(config.debounceMs)

    // Event result flow (for reactive updates)
    private val _eventResultFlow = MutableSharedFlow<EventResult>(replay = 0)
    val eventResultFlow: SharedFlow<EventResult> = _eventResultFlow.asSharedFlow()

    // Event callbacks
    private val eventCallbacks = mutableMapOf<String, MutableList<(CursorEvent) -> Unit>>()
    private val resultCallbacks = mutableListOf<(EventResult) -> Unit>()

    // Processing state
    private var processingJob: Job? = null

    init {
        Log.d(TAG, "VoiceCursorEventHandler initialized with config: $config")
    }

    /**
     * Process voice command
     *
     * Converts voice command to cursor event and dispatches it
     *
     * @param command Voice command string
     * @return true if command was recognized and queued
     */
    fun processVoiceCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase().trim()
        Log.d(TAG, "Processing voice command: '$normalizedCommand'")

        // Parse command and create event
        val event = parseCommand(normalizedCommand)

        if (event != null) {
            dispatchEvent(event)
            return true
        } else {
            Log.w(TAG, "Unknown voice command: '$normalizedCommand'")
            return false
        }
    }

    /**
     * Dispatch cursor event
     *
     * Adds event to queue for async processing
     *
     * @param event Cursor event to dispatch
     */
    fun dispatchEvent(event: CursorEvent) {
        // Check queue size
        if (eventQueue.size >= config.maxQueueSize) {
            Log.w(TAG, "Event queue full (${eventQueue.size}), dropping oldest event")
            eventQueue.poll() // Remove oldest event
        }

        // Add event to queue
        eventQueue.offer(event)

        Log.d(TAG, "Event dispatched: ${event::class.simpleName} (queue size: ${eventQueue.size})")

        // Start processing if not already running
        if (processingJob == null || processingJob?.isActive != true) {
            startEventProcessing()
        }
    }

    /**
     * Register event callback for specific event type
     *
     * @param eventType Event type class name (e.g., "Move", "Click")
     * @param callback Function to call when event occurs
     */
    fun registerEventCallback(eventType: String, callback: (CursorEvent) -> Unit) {
        val callbacks = eventCallbacks.getOrPut(eventType) { mutableListOf() }
        callbacks.add(callback)
        Log.d(TAG, "Event callback registered for type '$eventType' (total: ${callbacks.size})")
    }

    /**
     * Unregister event callback
     *
     * @param eventType Event type class name
     * @param callback Function to remove
     */
    fun unregisterEventCallback(eventType: String, callback: (CursorEvent) -> Unit) {
        eventCallbacks[eventType]?.remove(callback)
        Log.d(TAG, "Event callback unregistered for type '$eventType'")
    }

    /**
     * Register result callback
     *
     * @param callback Function to call when event completes
     */
    fun registerResultCallback(callback: (EventResult) -> Unit) {
        resultCallbacks.add(callback)
        Log.d(TAG, "Result callback registered (total: ${resultCallbacks.size})")
    }

    /**
     * Unregister result callback
     */
    fun unregisterResultCallback(callback: (EventResult) -> Unit) {
        resultCallbacks.remove(callback)
        Log.d(TAG, "Result callback unregistered (total: ${resultCallbacks.size})")
    }

    /**
     * Clear all callbacks
     */
    fun clearCallbacks() {
        eventCallbacks.clear()
        resultCallbacks.clear()
        Log.d(TAG, "All callbacks cleared")
    }

    /**
     * Get queue size
     */
    fun getQueueSize(): Int = eventQueue.size

    /**
     * Clear event queue
     */
    fun clearQueue() {
        eventQueue.clear()
        Log.d(TAG, "Event queue cleared")
    }

    /**
     * Get supported voice commands
     */
    fun getSupportedCommands(): List<String> {
        return DIRECTION_COMMANDS.keys.toList() + ACTION_COMMANDS.toList()
    }

    /**
     * Parse voice command to cursor event
     */
    private fun parseCommand(command: String): CursorEvent? {
        // Check direction commands
        DIRECTION_COMMANDS[command]?.let { direction ->
            return CursorEvent.Move(direction, config.moveDistance)
        }

        // Check action commands
        return when {
            command in setOf("click", "tap") -> CursorEvent.Click()
            command in setOf("long press", "hold") -> CursorEvent.LongPress()
            command in setOf("center", "center cursor") -> CursorEvent.Center
            command in setOf("show", "show cursor") -> CursorEvent.Show
            command in setOf("hide", "hide cursor") -> CursorEvent.Hide
            command in setOf("toggle", "toggle cursor") -> CursorEvent.Toggle
            else -> null
        }
    }

    /**
     * Start async event processing
     */
    private fun startEventProcessing() {
        processingJob = scope.launch {
            Log.d(TAG, "Event processing started")

            while (eventQueue.isNotEmpty()) {
                val event = eventQueue.poll() ?: break

                // Apply debouncing
                val debounceKey = event::class.simpleName ?: "unknown"
                if (!debouncer.shouldProceed(debounceKey)) {
                    Log.v(TAG, "Event debounced: $debounceKey")
                    continue
                }

                // Process event
                processEvent(event)
            }

            Log.d(TAG, "Event processing completed")
        }
    }

    /**
     * Process individual event
     */
    private suspend fun processEvent(event: CursorEvent) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Processing event: ${event::class.simpleName}")

        try {
            // Notify event callbacks
            notifyEventCallbacks(event)

            // Create result (actual execution happens in callbacks)
            val executionTime = System.currentTimeMillis() - startTime
            val result = EventResult(
                success = true,
                event = event,
                message = "Event processed successfully",
                executionTime = executionTime
            )

            // Emit result to flow
            _eventResultFlow.emit(result)

            // Notify result callbacks
            notifyResultCallbacks(result)

            Log.d(TAG, "Event processed successfully in ${executionTime}ms: ${event::class.simpleName}")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing event: ${event::class.simpleName}", e)

            val executionTime = System.currentTimeMillis() - startTime
            val result = EventResult(
                success = false,
                event = event,
                message = "Error: ${e.message}",
                executionTime = executionTime
            )

            // Emit error result
            _eventResultFlow.emit(result)
            notifyResultCallbacks(result)
        }
    }

    /**
     * Notify event callbacks
     */
    private fun notifyEventCallbacks(event: CursorEvent) {
        val eventType = event::class.simpleName ?: return
        val callbacks = eventCallbacks[eventType] ?: return

        callbacks.forEach { callback ->
            try {
                callback(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error in event callback for $eventType", e)
            }
        }
    }

    /**
     * Notify result callbacks
     */
    private fun notifyResultCallbacks(result: EventResult) {
        resultCallbacks.forEach { callback ->
            try {
                callback(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error in result callback", e)
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        processingJob?.cancel()
        processingJob = null
        clearQueue()
        clearCallbacks()
        debouncer.clearAll()
        Log.d(TAG, "VoiceCursorEventHandler disposed")
    }
}
