/**
 * EventPriorityManager.kt - Adaptive event filtering based on system resources
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-13
 *
 * Purpose: Intelligently filter accessibility events based on memory pressure and event importance.
 * 
 * Strategy: Under memory pressure, drop low-priority events (scrolling, focus changes) while
 * preserving critical user interactions (clicks, text input). This prevents OOM while maintaining
 * core functionality.
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.view.accessibility.AccessibilityEvent
import android.util.Log

/**
 * Event Priority Levels
 * 
 * Determines which events are processed under different memory pressure conditions.
 * 
 * Priority Hierarchy:
 * - CRITICAL: User-initiated actions, never dropped (clicks, text input)
 * - HIGH: Important state changes, dropped only under extreme pressure (window changes)
 * - MEDIUM: Useful updates, dropped under medium pressure (content changes)
 * - LOW: UI feedback, first to drop (scrolling, focus, selection)
 */
enum class EventPriority {
    CRITICAL,  // User interactions - NEVER drop
    HIGH,      // Window state changes - drop only under HIGH pressure
    MEDIUM,    // Content changes - drop under MEDIUM pressure
    LOW        // UI feedback - drop under LOW pressure
}

/**
 * Event Priority Manager
 * 
 * Classifies accessibility events by importance and determines whether they should
 * be processed based on current memory pressure.
 * 
 * Decision Matrix:
 * ```
 * Memory Pressure | Critical | High | Medium | Low
 * ----------------|----------|------|--------|----
 * NONE            | ✓        | ✓    | ✓      | ✓
 * LOW             | ✓        | ✓    | ✓      | ✗
 * MEDIUM          | ✓        | ✓    | ✗      | ✗
 * HIGH            | ✓        | ✗    | ✗      | ✗
 * ```
 * 
 * Usage:
 * ```kotlin
 * val manager = EventPriorityManager()
 * val shouldProcess = manager.shouldProcessEvent(
 *     event = accessibilityEvent,
 *     throttleLevel = resourceMonitor.getThrottleRecommendation()
 * )
 * if (shouldProcess) {
 *     processEvent(event)
 * }
 * ```
 */
class EventPriorityManager {
    
    companion object {
        private const val TAG = "EventPriorityManager"
    }
    
    // Metrics tracking
    private val droppedEventCounts = mutableMapOf<EventPriority, Long>()
    private val processedEventCounts = mutableMapOf<EventPriority, Long>()
    
    /**
     * Get priority level for an accessibility event
     * 
     * Classification Logic:
     * 
     * CRITICAL (User Actions):
     * - Clicks: User explicitly triggered an action
     * - Text input: User is typing, must capture for commands
     * - Long clicks: User initiated interaction
     * 
     * HIGH (State Changes):
     * - Window state changed: App launched, screen changed
     * - Important for tracking app context and scraping triggers
     * 
     * MEDIUM (Content Updates):
     * - Window content changed: UI updated, new elements appeared
     * - Useful for keeping element cache fresh, but not critical
     * 
     * LOW (UI Feedback):
     * - Scrolling: User scrolling, generates many events
     * - Focus changes: Keyboard/accessibility focus moved
     * - Selection: Item selected in list
     * - Can be safely dropped without functional impact
     * 
     * @param event AccessibilityEvent to classify
     * @return EventPriority level
     */
    fun getEventPriority(event: AccessibilityEvent): EventPriority {
        return when (event.eventType) {
            // CRITICAL: User-initiated actions - NEVER drop these
            AccessibilityEvent.TYPE_VIEW_CLICKED -> EventPriority.CRITICAL
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> EventPriority.CRITICAL
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> EventPriority.CRITICAL
            
            // HIGH: Window state changes - important for app context
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> EventPriority.HIGH
            
            // MEDIUM: Content updates - useful but not critical
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> EventPriority.MEDIUM
            
            // LOW: UI feedback - can be dropped under pressure
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> EventPriority.LOW
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> EventPriority.LOW
            AccessibilityEvent.TYPE_VIEW_SELECTED -> EventPriority.LOW
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> EventPriority.LOW
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> EventPriority.LOW
            
            // Default: Classify unknown events as LOW priority
            else -> {
                Log.v(TAG, "Unknown event type ${event.eventType}, classifying as LOW priority")
                EventPriority.LOW
            }
        }
    }
    
    /**
     * Determine if event should be processed based on priority and memory pressure
     * 
     * Decision Logic:
     * - HIGH pressure: Only process CRITICAL events
     * - MEDIUM pressure: Process CRITICAL and HIGH events
     * - LOW pressure: Process CRITICAL, HIGH, and MEDIUM events
     * - NONE pressure: Process all events
     * 
     * This ensures user interactions are never dropped while reducing load
     * on the system during memory pressure.
     * 
     * @param event AccessibilityEvent to evaluate
     * @param throttleLevel Current memory pressure level
     * @return true if event should be processed, false if should be dropped
     */
    fun shouldProcessEvent(
        event: AccessibilityEvent,
        throttleLevel: ResourceMonitor.ThrottleLevel
    ): Boolean {
        val priority = getEventPriority(event)
        
        val shouldProcess = when (throttleLevel) {
            ResourceMonitor.ThrottleLevel.HIGH -> {
                // Extreme pressure: Only critical user actions
                priority == EventPriority.CRITICAL
            }
            ResourceMonitor.ThrottleLevel.MEDIUM -> {
                // Moderate pressure: Critical and high priority
                priority == EventPriority.CRITICAL || priority == EventPriority.HIGH
            }
            ResourceMonitor.ThrottleLevel.LOW -> {
                // Light pressure: Drop only low priority
                priority != EventPriority.LOW
            }
            ResourceMonitor.ThrottleLevel.NONE -> {
                // No pressure: Process everything
                true
            }
        }
        
        // Update metrics
        if (shouldProcess) {
            processedEventCounts[priority] = (processedEventCounts[priority] ?: 0) + 1
        } else {
            droppedEventCounts[priority] = (droppedEventCounts[priority] ?: 0) + 1
            Log.v(TAG, "Dropped event type=${event.eventType} priority=$priority throttle=$throttleLevel")
        }
        
        return shouldProcess
    }
    
    /**
     * Get event processing metrics
     * 
     * Useful for monitoring and debugging adaptive filtering behavior.
     * 
     * @return Map of statistics including drop rates by priority
     */
    fun getMetrics(): EventMetrics {
        val totalProcessed = processedEventCounts.values.sum()
        val totalDropped = droppedEventCounts.values.sum()
        val total = totalProcessed + totalDropped
        
        val dropRateByPriority = EventPriority.values().associate { priority ->
            val processed = processedEventCounts[priority] ?: 0
            val dropped = droppedEventCounts[priority] ?: 0
            val priorityTotal = processed + dropped
            val dropRate = if (priorityTotal > 0) {
                (dropped.toFloat() / priorityTotal * 100).toInt()
            } else 0
            
            priority to DropStats(
                processed = processed,
                dropped = dropped,
                dropRate = dropRate
            )
        }
        
        return EventMetrics(
            totalProcessed = totalProcessed,
            totalDropped = totalDropped,
            overallDropRate = if (total > 0) (totalDropped.toFloat() / total * 100).toInt() else 0,
            dropRateByPriority = dropRateByPriority
        )
    }
    
    /**
     * Reset metrics counters
     * 
     * Useful for starting fresh measurement windows or after configuration changes.
     */
    fun resetMetrics() {
        droppedEventCounts.clear()
        processedEventCounts.clear()
        Log.d(TAG, "Metrics reset")
    }
    
    /**
     * Log current metrics to logcat
     * 
     * Outputs human-readable summary of event processing statistics.
     */
    fun logMetrics() {
        val metrics = getMetrics()
        
        Log.i(TAG, "=== Event Processing Metrics ===")
        Log.i(TAG, "Total Processed: ${metrics.totalProcessed}")
        Log.i(TAG, "Total Dropped: ${metrics.totalDropped}")
        Log.i(TAG, "Overall Drop Rate: ${metrics.overallDropRate}%")
        Log.i(TAG, "")
        Log.i(TAG, "By Priority:")
        
        EventPriority.values().forEach { priority ->
            val stats = metrics.dropRateByPriority[priority]
            if (stats != null) {
                Log.i(TAG, "  $priority: ${stats.processed} processed, ${stats.dropped} dropped (${stats.dropRate}%)")
            }
        }
        
        Log.i(TAG, "================================")
    }
}

/**
 * Event processing metrics
 */
data class EventMetrics(
    val totalProcessed: Long,
    val totalDropped: Long,
    val overallDropRate: Int,  // Percentage
    val dropRateByPriority: Map<EventPriority, DropStats>
)

/**
 * Drop statistics for a specific priority level
 */
data class DropStats(
    val processed: Long,
    val dropped: Long,
    val dropRate: Int  // Percentage
)

/**
 * Event type name mapper for logging
 * 
 * Converts AccessibilityEvent type codes to human-readable names.
 */
object EventTypeNames {
    fun getName(eventType: Int): String = when (eventType) {
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "VIEW_LONG_CLICKED"
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "VIEW_TEXT_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "VIEW_SCROLLED"
        AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUSED"
        AccessibilityEvent.TYPE_VIEW_SELECTED -> "VIEW_SELECTED"
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> "VIEW_ACCESSIBILITY_FOCUSED"
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> "VIEW_ACCESSIBILITY_FOCUS_CLEARED"
        else -> "UNKNOWN($eventType)"
    }
}
