/**
 * MetadataNotificationQueue.kt - Notification queue manager
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/metadata/MetadataNotificationQueue.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Manages queue of metadata notifications
 */

package com.augmentalis.learnapp.metadata

import android.content.Context
import android.content.SharedPreferences
import com.augmentalis.learnapp.models.ElementInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.PriorityQueue

/**
 * Metadata Notification Queue
 *
 * Manages queue of insufficient metadata notifications.
 * Supports batching, prioritization, and session-based preferences.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val queue = MetadataNotificationQueue(context)
 *
 * // Queue notification
 * queue.queueNotification(element, MetadataQuality.POOR, screenHash)
 *
 * // Get next notification
 * val next = queue.getNextNotification()
 *
 * // Skip all for session
 * queue.skipAllForSession()
 * ```
 *
 * @property context Application context
 *
 * @since 1.0.0
 */
class MetadataNotificationQueue(
    private val context: Context
) {
    /**
     * Preferences key
     */
    private val prefsName = "metadata_notifications"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /**
     * Priority queue for notifications
     */
    private val queue = PriorityQueue<MetadataNotificationItem>(
        compareByDescending { it.getPriority() }
    )

    /**
     * Skip all state for current session
     */
    private val _skipAllForSession = MutableStateFlow(false)
    val skipAllForSession: StateFlow<Boolean> = _skipAllForSession.asStateFlow()

    /**
     * Queue size flow
     */
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    /**
     * Batch size (notifications to accumulate before showing)
     */
    var batchSize: Int = 5
        set(value) {
            field = value.coerceIn(1, 20)
        }

    /**
     * Maximum queue size
     */
    var maxQueueSize: Int = 50
        set(value) {
            field = value.coerceIn(10, 200)
        }

    /**
     * Elements already processed (to avoid duplicates)
     */
    private val processedElements = mutableSetOf<String>()

    init {
        // Load batch size from preferences
        batchSize = prefs.getInt("batch_size", 5)
        maxQueueSize = prefs.getInt("max_queue_size", 50)
    }

    /**
     * Queue notification for element
     *
     * @param element Element with poor metadata
     * @param quality Quality assessment
     * @param screenHash Current screen hash
     */
    fun queueNotification(
        element: ElementInfo,
        quality: MetadataQuality,
        screenHash: String
    ) {
        // Skip if "skip all" enabled
        if (_skipAllForSession.value) {
            return
        }

        // Generate element fingerprint to avoid duplicates
        val fingerprint = generateElementFingerprint(element)
        if (processedElements.contains(fingerprint)) {
            return
        }

        // Check queue size limit
        if (queue.size >= maxQueueSize) {
            // Remove lowest priority item
            val lowest = queue.poll()
            lowest?.let {
                processedElements.remove(generateElementFingerprint(it.element))
            }
        }

        // Generate suggestions
        val suggestions = MetadataSuggestionGenerator.generateSuggestions(element)

        // Create notification item
        val item = MetadataNotificationItem(
            element = element,
            quality = quality,
            suggestions = suggestions,
            screenHash = screenHash
        )

        // Add to queue
        queue.offer(item)
        processedElements.add(fingerprint)
        _queueSize.value = queue.size
    }

    /**
     * Get next notification to show
     *
     * @return Next notification or null if queue empty
     */
    fun getNextNotification(): MetadataNotificationItem? {
        if (_skipAllForSession.value) {
            return null
        }

        val item = queue.poll()
        _queueSize.value = queue.size
        return item
    }

    /**
     * Peek next notification without removing
     *
     * @return Next notification or null
     */
    fun peekNextNotification(): MetadataNotificationItem? {
        return queue.peek()
    }

    /**
     * Check if ready to show (batch size reached)
     *
     * @return true if should show notification
     */
    fun isReadyToShow(): Boolean {
        return queue.size >= batchSize && !_skipAllForSession.value
    }

    /**
     * Skip current notification
     *
     * @param item Notification to skip
     */
    fun skipNotification(item: MetadataNotificationItem) {
        // Mark as processed (already in set)
        _queueSize.value = queue.size
    }

    /**
     * Skip all notifications for current session
     */
    fun skipAllForSession() {
        _skipAllForSession.value = true
        clearQueue()
    }

    /**
     * Reset "skip all" state
     */
    fun resetSkipAll() {
        _skipAllForSession.value = false
    }

    /**
     * Clear queue
     */
    fun clearQueue() {
        queue.clear()
        processedElements.clear()
        _queueSize.value = 0
    }

    /**
     * Update batch size and persist to preferences
     *
     * @param size New batch size
     */
    fun updateBatchSize(size: Int) {
        batchSize = size
        prefs.edit().putInt("batch_size", size).apply()
    }

    /**
     * Update maximum queue size and persist to preferences
     *
     * @param size New max size
     */
    fun updateMaxQueueSize(size: Int) {
        maxQueueSize = size
        prefs.edit().putInt("max_queue_size", size).apply()
    }

    /**
     * Get current queue size
     *
     * @return Number of queued notifications
     */
    fun getCurrentSize(): Int {
        return queue.size
    }

    /**
     * Generate element fingerprint for deduplication
     *
     * @param element Element to fingerprint
     * @return Fingerprint string
     */
    private fun generateElementFingerprint(element: ElementInfo): String {
        return buildString {
            append(element.className)
            append("|")
            append(element.text)
            append("|")
            append(element.contentDescription)
            append("|")
            append(element.resourceId)
            append("|")
            append(element.bounds.flattenToString())
        }
    }
}
