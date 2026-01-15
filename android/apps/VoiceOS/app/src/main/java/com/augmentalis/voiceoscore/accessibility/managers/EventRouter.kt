/**
 * EventRouter.kt - Centralized accessibility event routing and prioritization
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v18)
 * Created: 2026-01-15
 *
 * Extracts event routing logic from VoiceOSService to follow Single Responsibility Principle.
 * Handles accessibility event type routing, priority management, and event queuing.
 *
 * P2-8e: Part of SOLID refactoring - EventRouter extracts ~200 lines from VoiceOSService
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.UIElement
import com.augmentalis.voiceoscore.accessibility.utils.EventPriority
import com.augmentalis.voiceoscore.accessibility.utils.EventPriorityManager
import com.augmentalis.voiceoscore.config.DynamicPackageConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * Event Router
 *
 * Centralizes accessibility event routing for VoiceOSService:
 * - Event type dispatch (WINDOW_STATE_CHANGED, WINDOW_CONTENT_CHANGED, etc.)
 * - Event priority management (filter low-priority events under memory pressure)
 * - Event queuing during initialization
 * - Event deduplication to prevent double-processing
 * - Package name extraction and validation
 *
 * ARCHITECTURE:
 * Events flow through this router which decides:
 * 1. Should the event be processed? (priority check, service ready check)
 * 2. Should it be queued? (during initialization)
 * 3. Which integrations receive it? (scraping, LearnApp, web)
 * 4. How to update caches? (node cache, command cache)
 *
 * @param serviceScope Coroutine scope for async operations
 * @param commandScope Coroutine scope for command processing (Dispatchers.IO)
 * @param uiScrapingEngine UI element extraction engine
 * @param eventPriorityManager Event priority calculator
 * @param cacheLock Read-write lock for cache synchronization
 * @param nodeCache UI element cache
 * @param commandCache Normalized command text cache
 * @param isServiceReady Supplier to check if service is ready
 * @param isLowResourceMode Supplier to check if in low resource mode
 * @param verboseLogging Whether to enable verbose logging
 */
class EventRouter(
    private val serviceScope: CoroutineScope,
    private val commandScope: CoroutineScope,
    private val uiScrapingEngine: UIScrapingEngine,
    private val eventPriorityManager: EventPriorityManager,
    private val cacheLock: ReentrantReadWriteLock,
    private val nodeCache: MutableList<UIElement>,
    private val commandCache: MutableList<String>,
    private val isServiceReady: () -> Boolean,
    private val isLowResourceMode: () -> Boolean,
    private val verboseLogging: () -> Boolean,
    private val rootNodeProvider: () -> AccessibilityNodeInfo?,
    private val contextProvider: () -> android.content.Context
) {

    companion object {
        private const val TAG = "EventRouter"

        // Event queue configuration
        private const val MAX_QUEUED_EVENTS = 50

        // Event deduplication TTL (5 seconds)
        private const val EVENT_ID_TTL_MS = 5000L
    }

    /**
     * Event listener interface for integrations to receive routed events
     */
    interface EventListener {
        /**
         * Called when an accessibility event should be processed
         * @param event The accessibility event
         */
        fun onAccessibilityEvent(event: AccessibilityEvent)
    }

    // Event listeners (integrations that receive events)
    private val eventListeners = CopyOnWriteArrayList<EventListener>()

    // Window state change listener for special handling
    private var windowStateChangeListener: ((String?, String?) -> Unit)? = null

    // Event queue for buffering events during initialization
    private val pendingEvents = ConcurrentLinkedQueue<AccessibilityEvent>()

    // Event deduplication map: eventId -> timestamp
    private val processedEventIds = ConcurrentHashMap<String, Long>()

    // LearnApp initialization state tracking
    // State: 0=not started, 1=in progress, 2=complete
    private val learnAppInitState = AtomicInteger(0)

    // Callback to trigger LearnApp initialization
    private var learnAppInitializer: (() -> Unit)? = null

    /**
     * Register an event listener
     */
    fun addEventListener(listener: EventListener) {
        eventListeners.add(listener)
        Log.d(TAG, "Event listener registered: ${listener.javaClass.simpleName}")
    }

    /**
     * Unregister an event listener
     */
    fun removeEventListener(listener: EventListener) {
        eventListeners.remove(listener)
        Log.d(TAG, "Event listener unregistered: ${listener.javaClass.simpleName}")
    }

    /**
     * Set window state change listener for ScreenActivityDetector
     */
    fun setWindowStateChangeListener(listener: (packageName: String?, className: String?) -> Unit) {
        windowStateChangeListener = listener
    }

    /**
     * Set LearnApp initializer callback
     * Called on first event to initialize LearnApp after FLAG_RETRIEVE_INTERACTIVE_WINDOWS
     */
    fun setLearnAppInitializer(initializer: () -> Unit) {
        learnAppInitializer = initializer
    }

    /**
     * Get current LearnApp initialization state
     * @return 0=not started, 1=in progress, 2=complete
     */
    fun getLearnAppInitState(): Int = learnAppInitState.get()

    /**
     * Set LearnApp initialization state
     */
    fun setLearnAppInitState(state: Int) {
        learnAppInitState.set(state)
    }

    /**
     * Route an accessibility event to appropriate handlers
     *
     * This is the main entry point for event processing.
     * Decides whether to process, queue, or drop events based on:
     * - Service readiness
     * - Memory pressure (low resource mode)
     * - Event priority
     * - LearnApp initialization state
     *
     * @param event The accessibility event to route
     */
    fun routeEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Queue events during service initialization
        if (!isServiceReady()) {
            queueEvent(event)
            Log.d(TAG, "Service not ready, event queued (queue size=${pendingEvents.size})")
            return
        }

        // Adaptive event filtering based on memory pressure
        val eventPriority = eventPriorityManager.getEventPriority(event)
        val shouldProcess = !isLowResourceMode() || eventPriority >= EventPriority.HIGH

        if (!shouldProcess) {
            Log.v(TAG, "Event filtered due to memory pressure: type=${event.eventType}, priority=$eventPriority")
            return
        }

        // Deferred LearnApp initialization on first event
        handleLearnAppInitialization(event)

        // If LearnApp still initializing, queue the event
        val initState = learnAppInitState.get()
        if (initState < 2) {
            queueEvent(event)
            return
        }

        // Process any queued events first
        processQueuedEvents()

        // Route the event
        try {
            routeEventToHandlers(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error routing accessibility event: ${event.eventType}", e)
        }
    }

    /**
     * Handle deferred LearnApp initialization on first event
     */
    private fun handleLearnAppInitialization(event: AccessibilityEvent) {
        val initState = learnAppInitState.get()
        if (initState == 0) {
            // Try to claim initialization (atomic compare-and-set)
            if (learnAppInitState.compareAndSet(0, 1)) {
                Log.i(TAG, "First event received - triggering LearnApp initialization")
                learnAppInitializer?.invoke()
            }
        }
    }

    /**
     * Route event to all registered handlers and perform type-specific processing
     */
    private fun routeEventToHandlers(event: AccessibilityEvent) {
        // Forward to all registered event listeners
        eventListeners.forEach { listener ->
            try {
                listener.onAccessibilityEvent(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error in event listener ${listener.javaClass.simpleName}", e)
            }
        }

        // Extract package name
        var packageName = event.packageName?.toString()
        val currentPackage = extractCurrentPackage()

        // Handle cases where packageName might be null
        if (packageName == null && currentPackage != null) {
            val isWindowChange = isWindowChangeEvent(event)

            // Use dynamic package configuration
            if (isWindowChange && !DynamicPackageConfig.shouldMonitorPackage(contextProvider(), currentPackage)) {
                return // Skip for non-monitored packages
            }

            if (isWindowChange) {
                packageName = currentPackage
            } else {
                return // Can't proceed without package name
            }
        }

        if (packageName == null) return

        // Route based on event type
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event, packageName)
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event, packageName)
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event, packageName)
            }

            else -> {
                if (verboseLogging()) {
                    Log.v(TAG, "Unhandled event type: ${event.eventType} for $packageName")
                }
            }
        }
    }

    /**
     * Handle TYPE_WINDOW_CONTENT_CHANGED events
     */
    private fun handleWindowContentChanged(event: AccessibilityEvent, packageName: String) {
        serviceScope.launch {
            val commands = uiScrapingEngine.extractUIElementsAsync(event)
            val normalizedCommands = commands.map { it.normalizedText }

            cacheLock.writeLock().withLock {
                nodeCache.clear()
                nodeCache.addAll(commands)
                commandCache.clear()
                commandCache.addAll(normalizedCommands)
            }

            Log.d(TAG, "TYPE_WINDOW_CONTENT_CHANGED: ${normalizedCommands.size} commands scraped")
            if (verboseLogging()) {
                Log.d(TAG, "Scraped commands for $packageName: $commandCache")
            }
        }
    }

    /**
     * Handle TYPE_WINDOW_STATE_CHANGED events
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent, packageName: String) {
        // Notify window state change listener (ScreenActivityDetector)
        commandScope.launch {
            try {
                windowStateChangeListener?.invoke(
                    event.packageName?.toString(),
                    event.className?.toString()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in window state change listener", e)
            }
        }

        // Update UI scraping cache
        commandScope.launch {
            val commands = uiScrapingEngine.extractUIElementsAsync(event)
            val normalizedCommands = commands.map { it.normalizedText }

            cacheLock.writeLock().withLock {
                nodeCache.clear()
                nodeCache.addAll(commands)
                commandCache.clear()
                commandCache.addAll(normalizedCommands)
            }

            Log.d(TAG, "TYPE_WINDOW_STATE_CHANGED: ${normalizedCommands.size} commands scraped")
            if (verboseLogging()) {
                Log.d(TAG, "Scraped commands for $packageName: $commandCache")
            }
        }
    }

    /**
     * Handle TYPE_VIEW_CLICKED events
     */
    private fun handleViewClicked(event: AccessibilityEvent, packageName: String) {
        if (verboseLogging()) {
            Log.d(TAG, "Click event in $packageName: ${event.className}")
        }

        // Light UI refresh after clicks
        serviceScope.launch {
            val commands = uiScrapingEngine.extractUIElementsAsync(event)
            val normalizedCommands = commands.map { it.normalizedText }

            cacheLock.writeLock().withLock {
                nodeCache.clear()
                nodeCache.addAll(commands)
                commandCache.clear()
                commandCache.addAll(normalizedCommands)
            }

            Log.d(TAG, "TYPE_VIEW_CLICKED: ${normalizedCommands.size} commands scraped")
            if (verboseLogging()) {
                Log.d(TAG, "Scraped commands for $packageName: $commandCache")
            }
        }
    }

    /**
     * Extract current package name from root node
     */
    private fun extractCurrentPackage(): String? {
        val root = rootNodeProvider()
        val packageName = root?.packageName?.toString()
        recycleNodeTree(root)
        return packageName
    }

    /**
     * Check if event is a window change event
     */
    private fun isWindowChangeEvent(event: AccessibilityEvent): Boolean {
        return event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    /**
     * Queue an event for later processing during initialization
     */
    private fun queueEvent(event: AccessibilityEvent) {
        // Deduplication check
        val eventId = getEventId(event)
        if (isEventProcessed(eventId)) {
            Log.d(TAG, "Event already processed, skipping queue: $eventId")
            return
        }

        if (pendingEvents.size < MAX_QUEUED_EVENTS) {
            try {
                // Create a copy to avoid recycling issues
                val eventCopy = AccessibilityEvent.obtain(event)
                pendingEvents.offer(eventCopy)
                markEventProcessed(eventId)
                Log.d(TAG, "Event queued (total: ${pendingEvents.size})")
            } catch (e: Exception) {
                Log.e(TAG, "Error queuing event", e)
            }
        } else {
            Log.w(TAG, "Event queue full ($MAX_QUEUED_EVENTS), dropping event")
        }
    }

    /**
     * Process all queued events
     */
    fun processQueuedEvents() {
        if (pendingEvents.isEmpty()) return

        Log.i(TAG, "Processing ${pendingEvents.size} queued events")

        var processedCount = 0
        while (true) {
            val event = pendingEvents.poll() ?: break
            try {
                routeEventToHandlers(event)
                processedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Error processing queued event", e)
            } finally {
                event.recycle()
            }
        }

        Log.i(TAG, "Processed $processedCount queued events")
    }

    /**
     * Generate unique ID for event deduplication
     */
    private fun getEventId(event: AccessibilityEvent): String {
        val packageName = event.packageName?.toString() ?: "null"
        val className = event.className?.toString() ?: "null"
        val eventType = event.eventType
        val timestamp = event.eventTime
        val text = event.text.joinToString("|")
        return "$packageName:$className:$eventType:$timestamp:$text"
    }

    /**
     * Check if event was already processed (with TTL-based expiration)
     */
    private fun isEventProcessed(eventId: String): Boolean {
        val timestamp = processedEventIds[eventId] ?: return false
        val now = System.currentTimeMillis()

        if (now - timestamp > EVENT_ID_TTL_MS) {
            processedEventIds.remove(eventId)
            return false
        }

        return true
    }

    /**
     * Mark event as processed
     */
    private fun markEventProcessed(eventId: String) {
        processedEventIds[eventId] = System.currentTimeMillis()

        // Periodic cleanup of expired entries
        if (processedEventIds.size > 100) {
            cleanupExpiredEventIds()
        }
    }

    /**
     * Remove expired event IDs from deduplication cache
     */
    private fun cleanupExpiredEventIds() {
        val now = System.currentTimeMillis()
        val iterator = processedEventIds.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > EVENT_ID_TTL_MS) {
                iterator.remove()
            }
        }
    }

    /**
     * Recursively recycle AccessibilityNodeInfo and all descendants
     */
    private fun recycleNodeTree(node: AccessibilityNodeInfo?) {
        if (node == null) return

        try {
            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i)
                    recycleNodeTree(child)
                } catch (e: Exception) {
                    Log.w(TAG, "Error recycling child node: ${e.message}")
                }
            }

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in recycleNodeTree: ${e.message}")
        }
    }

    /**
     * Get pending event count (for diagnostics)
     */
    fun getPendingEventCount(): Int = pendingEvents.size

    /**
     * Clear all queued events (for cleanup)
     */
    fun clearQueuedEvents() {
        while (true) {
            val event = pendingEvents.poll() ?: break
            event.recycle()
        }
        Log.d(TAG, "Cleared all queued events")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        clearQueuedEvents()
        processedEventIds.clear()
        eventListeners.clear()
        windowStateChangeListener = null
        learnAppInitializer = null
        Log.i(TAG, "EventRouter cleanup complete")
    }
}
