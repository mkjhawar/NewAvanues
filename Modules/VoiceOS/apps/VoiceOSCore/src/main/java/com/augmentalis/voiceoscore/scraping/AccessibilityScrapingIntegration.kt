/**
 * AccessibilityScrapingIntegration.kt - UI scraping integration for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-03
 *
 * Provides UI element scraping and command generation capabilities
 * for accessibility-based voice control.
 */
package com.augmentalis.voiceoscore.scraping

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Accessibility Scraping Integration
 *
 * Orchestrates UI element scraping for accessibility-based voice control.
 * Handles screen state tracking, element extraction, and command generation.
 *
 * @property context Application context
 * @property accessibilityService Accessibility service for UI access
 */
class AccessibilityScrapingIntegration(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {
    companion object {
        private const val TAG = "AccessibilityScrapingIntegration"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var isInitialized = false

    init {
        Log.d(TAG, "AccessibilityScrapingIntegration initializing...")
        isInitialized = true
        Log.i(TAG, "AccessibilityScrapingIntegration initialized")
    }

    /**
     * Check if integration is ready
     */
    val isReady: Boolean
        get() = isInitialized

    /**
     * Handle accessibility event
     *
     * @param event Accessibility event to process
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isInitialized) {
            Log.d(TAG, "Skipping event - not initialized")
            return
        }

        // Process window content changes for scraping
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                scope.launch {
                    try {
                        val rootNode = accessibilityService.rootInActiveWindow
                        if (rootNode != null) {
                            val elements = scrapeScreen(rootNode)
                            Log.d(TAG, "Scraped ${elements.size} elements from event")
                            rootNode.recycle()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing accessibility event", e)
                    }
                }
            }
        }
    }

    /**
     * Scrape current screen for UI elements
     *
     * @param rootNode Root accessibility node
     * @return List of scraped elements
     */
    fun scrapeScreen(rootNode: AccessibilityNodeInfo?): List<ScrapedElement> {
        if (rootNode == null) {
            Log.w(TAG, "Cannot scrape: root node is null")
            return emptyList()
        }

        val elements = mutableListOf<ScrapedElement>()
        scrapeNodeRecursive(rootNode, elements, 0)
        return elements
    }

    private fun scrapeNodeRecursive(
        node: AccessibilityNodeInfo,
        elements: MutableList<ScrapedElement>,
        depth: Int
    ) {
        if (depth > 50) return // Prevent infinite recursion

        // Extract element info
        val element = extractElement(node, depth)
        if (element != null) {
            elements.add(element)
        }

        // Process children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scrapeNodeRecursive(child, elements, depth + 1)
                child.recycle()
            }
        }
    }

    private fun extractElement(node: AccessibilityNodeInfo, depth: Int): ScrapedElement? {
        val className = node.className?.toString() ?: return null
        val text = node.text?.toString()
        val contentDescription = node.contentDescription?.toString()
        val resourceId = node.viewIdResourceName

        // Skip elements without any identifying information
        if (text.isNullOrBlank() && contentDescription.isNullOrBlank() && resourceId.isNullOrBlank()) {
            return null
        }

        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)

        return ScrapedElement(
            className = className,
            text = text,
            contentDescription = contentDescription,
            resourceId = resourceId,
            bounds = bounds,
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            isCheckable = node.isCheckable,
            isChecked = node.isChecked,
            isEnabled = node.isEnabled,
            depth = depth
        )
    }

    /**
     * Learn an app by scraping its UI elements
     *
     * @param packageName Package name of the app to learn
     * @return LearnAppResult with learning outcome
     */
    suspend fun learnApp(packageName: String): LearnAppResult {
        Log.d(TAG, "Learning app: $packageName")

        if (!isInitialized) {
            return LearnAppResult(
                success = false,
                message = "Integration not initialized"
            )
        }

        return try {
            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                return LearnAppResult(
                    success = false,
                    message = "No active window"
                )
            }

            val elements = scrapeScreen(rootNode)
            rootNode.recycle()

            LearnAppResult(
                success = true,
                message = "Scraped ${elements.size} elements",
                elementsDiscovered = elements.size,
                newElements = elements.size,
                updatedElements = 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error learning app: $packageName", e)
            LearnAppResult(
                success = false,
                message = "Error: ${e.message}"
            )
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up AccessibilityScrapingIntegration")
        isInitialized = false
    }
}

/**
 * Scraped UI Element
 *
 * Represents a UI element extracted from accessibility tree.
 */
data class ScrapedElement(
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val bounds: android.graphics.Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val depth: Int
)

/**
 * LearnApp Result
 *
 * Data class representing results of learn app operations.
 */
data class LearnAppResult(
    val success: Boolean,
    val message: String,
    val elementsDiscovered: Int = 0,
    val newElements: Int = 0,
    val updatedElements: Int = 0
)
