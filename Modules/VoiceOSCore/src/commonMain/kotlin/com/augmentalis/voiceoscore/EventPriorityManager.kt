/**
 * EventPriorityManager.kt - Adaptive event filtering based on resource pressure
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Intelligently filters events based on priority and resource pressure.
 * Under memory pressure, drops low-priority events while preserving critical actions.
 */
package com.augmentalis.voiceoscore

/**
 * Event priority levels determining processing under different resource conditions.
 *
 * Decision Matrix:
 * ```
 * Throttle Level | Critical | High | Medium | Low
 * ---------------|----------|------|--------|----
 * NONE           | Process  | Yes  | Yes    | Yes
 * LOW            | Process  | Yes  | Yes    | Drop
 * MEDIUM         | Process  | Yes  | Drop   | Drop
 * HIGH           | Process  | Drop | Drop   | Drop
 * ```
 */
enum class EventPriority {
    /** User-initiated actions - NEVER drop (clicks, text input) */
    CRITICAL,
    /** Important state changes - drop only under extreme pressure (window changes) */
    HIGH,
    /** Useful updates - drop under medium pressure (content changes) */
    MEDIUM,
    /** UI feedback - first to drop (scrolling, focus, selection) */
    LOW
}

/**
 * Statistics for event drop tracking.
 */
data class EventDropStats(
    val processed: Long,
    val dropped: Long
) {
    val dropRate: Int get() = if (processed + dropped > 0) {
        ((dropped.toDouble() / (processed + dropped)) * 100).toInt()
    } else 0
}

/**
 * Event processing metrics.
 */
data class EventMetrics(
    val totalProcessed: Long,
    val totalDropped: Long,
    val overallDropRate: Int,
    val statsByPriority: Map<EventPriority, EventDropStats>
)

/**
 * Event priority manager for adaptive event filtering.
 *
 * Usage:
 * ```kotlin
 * val priorityManager = EventPriorityManager()
 *
 * // When receiving an event
 * val priority = priorityManager.classifyEvent(eventType)
 * val throttle = resourceMonitor.getThrottleRecommendation()
 *
 * if (priorityManager.shouldProcess(priority, throttle)) {
 *     processEvent(event)
 * }
 * ```
 */
class EventPriorityManager {

    // Metrics tracking
    private val processedCounts = mutableMapOf<EventPriority, Long>()
    private val droppedCounts = mutableMapOf<EventPriority, Long>()

    /**
     * Determine if an event should be processed based on priority and throttle level.
     *
     * @param priority Event priority classification
     * @param throttleLevel Current resource throttle recommendation
     * @return true if event should be processed, false to drop
     */
    fun shouldProcess(priority: EventPriority, throttleLevel: ThrottleLevel): Boolean {
        val shouldProcess = when (throttleLevel) {
            ThrottleLevel.HIGH -> priority == EventPriority.CRITICAL
            ThrottleLevel.MEDIUM -> priority == EventPriority.CRITICAL || priority == EventPriority.HIGH
            ThrottleLevel.LOW -> priority != EventPriority.LOW
            ThrottleLevel.NONE -> true
        }

        // Track metrics
        if (shouldProcess) {
            processedCounts[priority] = (processedCounts[priority] ?: 0) + 1
        } else {
            droppedCounts[priority] = (droppedCounts[priority] ?: 0) + 1
        }

        return shouldProcess
    }

    /**
     * Get current event processing metrics.
     */
    fun getMetrics(): EventMetrics {
        val totalProcessed = processedCounts.values.sum()
        val totalDropped = droppedCounts.values.sum()
        val total = totalProcessed + totalDropped

        val statsByPriority = EventPriority.entries.associateWith { priority ->
            EventDropStats(
                processed = processedCounts[priority] ?: 0,
                dropped = droppedCounts[priority] ?: 0
            )
        }

        return EventMetrics(
            totalProcessed = totalProcessed,
            totalDropped = totalDropped,
            overallDropRate = if (total > 0) ((totalDropped.toDouble() / total) * 100).toInt() else 0,
            statsByPriority = statsByPriority
        )
    }

    /**
     * Reset metrics counters.
     */
    fun resetMetrics() {
        processedCounts.clear()
        droppedCounts.clear()
    }
}

/**
 * Android-specific event type classifier.
 * Maps AccessibilityEvent types to EventPriority.
 */
object AndroidEventClassifier {
    // AccessibilityEvent type constants (avoid direct dependency)
    private const val TYPE_VIEW_CLICKED = 1
    private const val TYPE_VIEW_LONG_CLICKED = 2
    private const val TYPE_VIEW_TEXT_CHANGED = 16
    private const val TYPE_WINDOW_STATE_CHANGED = 32
    private const val TYPE_WINDOW_CONTENT_CHANGED = 2048
    private const val TYPE_VIEW_SCROLLED = 4096
    private const val TYPE_VIEW_FOCUSED = 8
    private const val TYPE_VIEW_SELECTED = 4

    /**
     * Classify Android accessibility event type to EventPriority.
     *
     * @param eventType AccessibilityEvent.eventType value
     * @return Appropriate EventPriority
     */
    fun classify(eventType: Int): EventPriority {
        return when (eventType) {
            TYPE_VIEW_CLICKED, TYPE_VIEW_LONG_CLICKED, TYPE_VIEW_TEXT_CHANGED ->
                EventPriority.CRITICAL
            TYPE_WINDOW_STATE_CHANGED ->
                EventPriority.HIGH
            TYPE_WINDOW_CONTENT_CHANGED ->
                EventPriority.MEDIUM
            TYPE_VIEW_SCROLLED, TYPE_VIEW_FOCUSED, TYPE_VIEW_SELECTED ->
                EventPriority.LOW
            else ->
                EventPriority.LOW
        }
    }
}
