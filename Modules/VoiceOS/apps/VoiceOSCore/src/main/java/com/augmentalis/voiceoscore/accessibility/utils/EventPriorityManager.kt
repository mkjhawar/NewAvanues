/**
 * EventPriorityManager.kt - Manages accessibility event priorities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Manages priority ordering for accessibility events to ensure
 * high-priority events (like user input) are processed before
 * lower-priority events (like background updates).
 *
 * STUB: This class was referenced but not implemented. Added as stub to allow build.
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Event Priority Manager
 *
 * Manages priority-based ordering of accessibility events.
 * Higher priority events are processed before lower priority events.
 */
class EventPriorityManager {
    companion object {
        private const val TAG = "EventPriorityManager"

        // Priority levels (higher = more urgent)
        const val PRIORITY_CRITICAL = 100
        const val PRIORITY_HIGH = 75
        const val PRIORITY_NORMAL = 50
        const val PRIORITY_LOW = 25
        const val PRIORITY_BACKGROUND = 0

        // Event type to priority mappings
        private val EVENT_PRIORITIES = mapOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED to PRIORITY_CRITICAL,
            AccessibilityEvent.TYPE_VIEW_FOCUSED to PRIORITY_HIGH,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED to PRIORITY_HIGH,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED to PRIORITY_NORMAL,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED to PRIORITY_LOW,
            AccessibilityEvent.TYPE_VIEW_SCROLLED to PRIORITY_LOW,
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED to PRIORITY_BACKGROUND
        )
    }

    data class PrioritizedEvent(
        val event: AccessibilityEvent,
        val priority: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : Comparable<PrioritizedEvent> {
        override fun compareTo(other: PrioritizedEvent): Int {
            // Higher priority first, then earlier timestamp
            val priorityCompare = other.priority.compareTo(this.priority)
            return if (priorityCompare != 0) priorityCompare else this.timestamp.compareTo(other.timestamp)
        }
    }

    private val eventQueue = PriorityQueue<PrioritizedEvent>()
    private val lock = ReentrantLock()
    private val isProcessing = AtomicBoolean(false)

    /**
     * Get priority for an event type.
     *
     * @param eventType The accessibility event type
     * @return The priority level for this event type
     */
    fun getPriorityForEvent(eventType: Int): Int {
        return EVENT_PRIORITIES[eventType] ?: PRIORITY_NORMAL
    }

    /**
     * Enqueue an event for priority-based processing.
     *
     * @param event The accessibility event
     * @return true if the event was enqueued
     */
    fun enqueue(event: AccessibilityEvent): Boolean {
        val priority = getPriorityForEvent(event.eventType)
        val prioritizedEvent = PrioritizedEvent(
            event = AccessibilityEvent.obtain(event),
            priority = priority
        )

        lock.withLock {
            eventQueue.add(prioritizedEvent)
            Log.v(TAG, "Enqueued event type ${event.eventType} with priority $priority")
        }
        return true
    }

    /**
     * Dequeue the next highest priority event.
     *
     * @return The next event to process, or null if queue is empty
     */
    fun dequeue(): AccessibilityEvent? {
        lock.withLock {
            return eventQueue.poll()?.event
        }
    }

    /**
     * Check if there are events in the queue.
     */
    fun hasEvents(): Boolean {
        lock.withLock {
            return eventQueue.isNotEmpty()
        }
    }

    /**
     * Get the current queue size.
     */
    fun queueSize(): Int {
        lock.withLock {
            return eventQueue.size
        }
    }

    /**
     * Clear all events from the queue.
     */
    fun clear() {
        lock.withLock {
            while (eventQueue.isNotEmpty()) {
                eventQueue.poll()?.event?.recycle()
            }
        }
        Log.d(TAG, "Event queue cleared")
    }

    /**
     * Check if an event should be processed immediately (bypass queue).
     *
     * @param eventType The event type to check
     * @return true if this event type should bypass the queue
     */
    fun shouldBypassQueue(eventType: Int): Boolean {
        return getPriorityForEvent(eventType) >= PRIORITY_CRITICAL
    }
}
