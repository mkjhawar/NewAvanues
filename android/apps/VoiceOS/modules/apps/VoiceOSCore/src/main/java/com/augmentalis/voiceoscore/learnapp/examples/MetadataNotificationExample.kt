/**
 * MetadataNotificationExample.kt - Integration example
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/examples/MetadataNotificationExample.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-10-13
 *
 * Example integration of metadata notification system
 * This file demonstrates how to use the notification system in LearnApp exploration
 */

package com.augmentalis.voiceoscore.learnapp.examples

import android.content.Context
import com.augmentalis.voiceoscore.learnapp.database.dao.LearnAppDao
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine
import com.augmentalis.voiceoscore.learnapp.metadata.MetadataNotificationQueue
import com.augmentalis.voiceoscore.learnapp.metadata.MetadataQuality
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.ui.metadata.InsufficientMetadataNotification

/**
 * Metadata Notification Example
 *
 * Demonstrates integration of metadata notification system
 * into LearnApp exploration workflow.
 *
 * ## Integration Steps
 *
 * 1. Initialize components during engine setup
 * 2. Assess metadata quality during exploration
 * 3. Queue poor quality elements
 * 4. Show notification when batch ready
 * 5. Handle user actions (label, skip, skip all)
 *
 * @property context Application context
 * @property learnAppDao Database access object for manual labels
 * @property explorationEngine Engine for controlling exploration flow
 */
class MetadataNotificationExample(
    private val context: Context,
    private val learnAppDao: LearnAppDao,
    private val explorationEngine: ExplorationEngine
) {

    /**
     * Notification queue
     */
    private val queue = MetadataNotificationQueue(context).apply {
        // Configure batch size (show after 5 poor elements)
        updateBatchSize(5)

        // Configure max queue size
        updateMaxQueueSize(50)
    }

    /**
     * Notification manager
     */
    private val notification = InsufficientMetadataNotification(context, queue)

    /**
     * Current screen hash
     */
    private var currentScreenHash: String = ""

    /**
     * Example: Explore screen and check metadata
     *
     * @param elements Elements discovered on screen
     * @param screenHash Current screen fingerprint
     */
    fun exploreScreen(elements: List<ElementInfo>, screenHash: String) {
        currentScreenHash = screenHash

        // Assess each element
        elements.forEach { element ->
            assessAndQueue(element)
        }

        // Show notification if batch ready
        if (queue.isReadyToShow()) {
            showNotification()
        }
    }

    /**
     * Assess element metadata quality and queue if poor
     *
     * @param element Element to assess
     */
    private fun assessAndQueue(element: ElementInfo) {
        // Skip if "skip all" is active
        if (queue.skipAllForSession.value) {
            return
        }

        // Assess metadata quality
        val quality = MetadataQuality.assess(element)

        // Log quality for debugging
        println("Element quality: $quality - ${element.getDisplayName()}")

        // Queue if poor (requires notification)
        if (MetadataQuality.requiresNotification(quality)) {
            queue.queueNotification(
                element = element,
                quality = quality,
                screenHash = currentScreenHash
            )

            println("Queued poor element: ${element.getDisplayName()} (queue size: ${queue.getCurrentSize()})")
        }
    }

    /**
     * Show notification for next queued item
     */
    private fun showNotification() {
        notification.showNextNotification(
            onLabelProvided = { item, label ->
                handleLabelProvided(item, label)
            },
            onSkip = {
                handleSkip()
            },
            onSkipAll = {
                handleSkipAll()
            }
        )
    }

    /**
     * Handle user providing manual label
     *
     * @param item Notification item
     * @param label User-provided label
     */
    private fun handleLabelProvided(
        item: com.augmentalis.voiceoscore.learnapp.metadata.MetadataNotificationItem,
        label: String
    ) {
        println("User provided label: '$label' for element: ${item.element.getDisplayName()}")

        // Save to database
        // TODO: Add insertManualLabel() to LearnAppDao interface when manual labeling feature is implemented
        // learnAppDao.insertManualLabel(
        //     elementUuid = item.element.uuid!!,
        //     manualLabel = label,
        //     timestamp = System.currentTimeMillis(),
        //     screenHash = item.screenHash
        // )

        // Resume exploration
        resumeExploration()
    }

    /**
     * Handle user skipping notification
     */
    private fun handleSkip() {
        println("User skipped notification (remaining: ${queue.getCurrentSize()})")

        // Resume exploration
        resumeExploration()
    }

    /**
     * Handle user skipping all notifications
     */
    private fun handleSkipAll() {
        println("User skipped all notifications for session")

        // Queue is now disabled for session
        queue.skipAllForSession()

        // Resume exploration
        resumeExploration()
    }

    /**
     * Resume exploration after notification action
     */
    private fun resumeExploration() {
        println("Resuming exploration...")

        // Continue exploration
        explorationEngine.resumeExploration()
    }

    /**
     * Reset notification system for new app
     */
    fun resetForNewApp() {
        // Clear queue
        queue.clearQueue()

        // Reset "skip all" state
        queue.resetSkipAll()

        println("Notification system reset for new app")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        notification.cleanup()
        queue.clearQueue()
    }

    /**
     * Example: Create test elements with varying quality
     */
    companion object {
        /**
         * Create example elements for testing
         */
        fun createTestElements(): List<ElementInfo> {
            return listOf(
                // Excellent quality
                ElementInfo(
                    className = "android.widget.Button",
                    text = "Submit",
                    contentDescription = "Submit button",
                    resourceId = "com.example.app:id/submit_btn",
                    isClickable = true
                ),

                // Good quality
                ElementInfo(
                    className = "android.widget.ImageButton",
                    contentDescription = "Search",
                    resourceId = "com.example.app:id/search",
                    isClickable = true
                ),

                // Acceptable quality
                ElementInfo(
                    className = "android.widget.TextView",
                    text = "Welcome"
                ),

                // Poor quality (triggers notification)
                ElementInfo(
                    className = "android.widget.Button",
                    isClickable = true
                ),

                // Poor quality (triggers notification)
                ElementInfo(
                    className = "android.widget.ImageView",
                    isClickable = true
                )
            )
        }

        /**
         * Example usage
         *
         * Note: This example requires instances of LearnAppDao and ExplorationEngine
         * which should be obtained from your actual application context.
         */
        fun exampleUsage(
            context: Context,
            learnAppDao: LearnAppDao,
            explorationEngine: ExplorationEngine
        ) {
            // Initialize
            val example = MetadataNotificationExample(context, learnAppDao, explorationEngine)

            // Create test elements
            val elements = createTestElements()

            // Explore screen
            example.exploreScreen(
                elements = elements,
                screenHash = "screen_12345"
            )

            // Notification will show automatically when batch ready
            // (after 5 poor quality elements by default)

            // Reset for new app
            example.resetForNewApp()

            // Cleanup when done
            example.cleanup()
        }
    }
}
