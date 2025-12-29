/**
 * NumberHandler.kt - Number overlay display and control handler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Migration Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Handler for number overlay display and interaction
 * Shows numbered overlays on UI elements for voice selection
 */
class NumberHandler(
    private val service: IVoiceOSContext
) : ActionHandler {

    companion object {
        private const val TAG = "NumberHandler"
        
        // Supported actions
        val SUPPORTED_ACTIONS = listOf(
            "show numbers",
            "hide numbers",
            "numbers on",
            "numbers off",
            "toggle numbers",
            "number overlay",
            "label elements",
            "click number",
            "select number",
            "tap number"
        )
    }

    // THREAD SAFETY FIX: Added @Volatile for cross-thread visibility
    @Volatile
    private var isNumberOverlayVisible = false
    // THREAD SAFETY FIX: Replaced mutableMapOf with ConcurrentHashMap for thread-safe access
    private val numberedElements = ConcurrentHashMap<Int, ElementInfo>()
    @Volatile
    private var currentNumberCount = 0
    private val numberScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Information about a numbered UI element
     */
    data class ElementInfo(
        val node: AccessibilityNodeInfo,
        val bounds: Rect,
        val description: String,
        val isClickable: Boolean,
        val isScrollable: Boolean
    )

    override fun initialize() {
        Log.d(TAG, "Initializing NumberHandler")
        // Initialize number overlay system
    }

    override fun canHandle(action: String): Boolean {
        return SUPPORTED_ACTIONS.any { supportedAction -> action.contains(supportedAction, ignoreCase = true) } || isNumberCommand(action)
    }

    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        Log.d(TAG, "Executing number action: $action")

        return when {
            // Show number overlay
            action.contains("show numbers", ignoreCase = true) ||
            action.contains("numbers on", ignoreCase = true) ||
            action.contains("label elements", ignoreCase = true) ||
            action.contains("number overlay", ignoreCase = true) -> {
                showNumberOverlay()
            }

            // Hide number overlay
            action.contains("hide numbers", ignoreCase = true) ||
            action.contains("numbers off", ignoreCase = true) -> {
                hideNumberOverlay()
            }

            // Toggle number overlay
            action.contains("toggle numbers", ignoreCase = true) -> {
                toggleNumberOverlay()
            }

            // Handle number commands (e.g., "click 5", "tap 3", "select 7")
            isNumberCommand(action) -> {
                handleNumberCommand(action)
            }

            else -> {
                Log.w(TAG, "Unknown number action: $action")
                false
            }
        }
    }

    /**
     * Show number overlay on interactive elements
     */
    private fun showNumberOverlay(): Boolean {
        return try {
            if (isNumberOverlayVisible) {
                Log.d(TAG, "Number overlay already visible")
                return true
            }

            Log.i(TAG, "Showing number overlay")
            
            val rootNode = service.getRootNodeInActiveWindow()
            if (rootNode == null) {
                Log.w(TAG, "No root node available")
                return false
            }

            // Clear previous numbering
            clearNumberedElements()

            // Find and number interactive elements
            val interactiveElements = findInteractiveElements(rootNode)
            
            if (interactiveElements.isEmpty()) {
                Log.w(TAG, "No interactive elements found")
                showFeedback("No interactive elements found on screen")
                return false
            }

            // Assign numbers to elements
            interactiveElements.forEachIndexed { index, elementInfo ->
                val number = index + 1
                numberedElements[number] = elementInfo
                currentNumberCount = number
            }

            isNumberOverlayVisible = true

            // Show overlay visualization
            displayNumberOverlays()

            // Provide feedback
            showFeedback("Showing ${numberedElements.size} numbered elements. Say 'tap [number]' to interact or 'hide numbers' to close.")

            // Auto-hide after timeout
            numberScope.launch {
                delay(30000) // 30 seconds
                if (isNumberOverlayVisible) {
                    hideNumberOverlay()
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing number overlay", e)
            false
        }
    }

    /**
     * Hide number overlay
     */
    private fun hideNumberOverlay(): Boolean {
        return try {
            if (!isNumberOverlayVisible) {
                Log.d(TAG, "Number overlay already hidden")
                return true
            }

            Log.i(TAG, "Hiding number overlay")
            
            isNumberOverlayVisible = false
            clearNumberedElements()

            // Hide overlay visualization
            hideNumberOverlays()

            showFeedback("Number overlay hidden")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding number overlay", e)
            false
        }
    }

    /**
     * Toggle number overlay visibility
     */
    private fun toggleNumberOverlay(): Boolean {
        return if (isNumberOverlayVisible) {
            hideNumberOverlay()
        } else {
            showNumberOverlay()
        }
    }

    /**
     * Handle number-based commands (e.g., "tap 5", "click 3")
     */
    private fun handleNumberCommand(action: String): Boolean {
        if (!isNumberOverlayVisible) {
            Log.w(TAG, "Number overlay not visible for command: $action")
            showFeedback("Show numbers first by saying 'show numbers'")
            return false
        }

        return try {
            val number = extractNumberFromCommand(action)
            if (number == null) {
                Log.w(TAG, "Could not extract number from command: $action")
                return false
            }

            val elementInfo = numberedElements[number]
            if (elementInfo == null) {
                Log.w(TAG, "No element found for number: $number")
                showFeedback("Number $number not found. Available: 1-$currentNumberCount")
                return false
            }

            Log.d(TAG, "Executing action on element $number: ${elementInfo.description}")

            // Determine action type
            val success = when {
                action.contains("click", ignoreCase = true) ||
                action.contains("tap", ignoreCase = true) ||
                action.contains("select", ignoreCase = true) -> {
                    clickElement(elementInfo)
                }
                action.contains("scroll", ignoreCase = true) -> {
                    scrollElement(elementInfo)
                }
                action.contains("long", ignoreCase = true) -> {
                    longPressElement(elementInfo)
                }
                else -> {
                    // Default to click
                    clickElement(elementInfo)
                }
            }

            if (success) {
                showFeedback("Tapped element $number: ${elementInfo.description}")
                
                // Hide overlay after successful interaction
                numberScope.launch {
                    delay(1000)
                    hideNumberOverlay()
                }
            } else {
                showFeedback("Could not interact with element $number")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error handling number command", e)
            false
        }
    }

    /**
     * Find interactive elements on screen
     */
    private fun findInteractiveElements(rootNode: AccessibilityNodeInfo): List<ElementInfo> {
        val elements = mutableListOf<ElementInfo>()
        
        try {
            findInteractiveElementsRecursive(rootNode, elements)
            
            // Sort by position (top to bottom, left to right)
            elements.sortWith { a, b ->
                when {
                    a.bounds.top != b.bounds.top -> a.bounds.top - b.bounds.top
                    else -> a.bounds.left - b.bounds.left
                }
            }
            
            Log.d(TAG, "Found ${elements.size} interactive elements")
        } catch (e: Exception) {
            Log.e(TAG, "Error finding interactive elements", e)
        }
        
        return elements
    }

    /**
     * Recursively find interactive elements
     */
    private fun findInteractiveElementsRecursive(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>
    ) {
        try {
            // Check if this node is interactive
            if (isInteractiveElement(node)) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                // Skip elements that are too small or off-screen
                if (bounds.width() > 20 && bounds.height() > 20 && 
                    bounds.left >= 0 && bounds.top >= 0) {
                    
                    val description = getElementDescription(node)
                    val elementInfo = ElementInfo(
                        node = node,
                        bounds = bounds,
                        description = description,
                        isClickable = node.isClickable,
                        isScrollable = node.isScrollable
                    )
                    
                    elements.add(elementInfo)
                    Log.v(TAG, "Added element: $description at $bounds")
                }
            }

            // Recursively check children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                child?.let {
                    findInteractiveElementsRecursive(it, elements)
                    // it.recycle() // Deprecated - Android handles this automatically
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recursive element finding", e)
        }
    }

    /**
     * Check if a node is interactive
     */
    private fun isInteractiveElement(node: AccessibilityNodeInfo): Boolean {
        return node.isClickable || 
               node.isScrollable || 
               node.isCheckable || 
               node.isEditable ||
               node.isFocusable ||
               node.actionList.any { 
                   it.id == AccessibilityNodeInfo.ACTION_CLICK ||
                   it.id == AccessibilityNodeInfo.ACTION_LONG_CLICK ||
                   it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
                   it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
               }
    }

    /**
     * Get description for an element
     */
    private fun getElementDescription(node: AccessibilityNodeInfo): String {
        return when {
            !node.contentDescription.isNullOrEmpty() -> node.contentDescription.toString()
            !node.text.isNullOrEmpty() -> node.text.toString()
            !node.className.isNullOrEmpty() -> node.className.toString().substringAfterLast('.')
            else -> "Element"
        }
    }

    /**
     * Click on an element
     */
    private fun clickElement(elementInfo: ElementInfo): Boolean {
        return try {
            elementInfo.node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking element", e)
            false
        }
    }

    /**
     * Long press on an element
     */
    private fun longPressElement(elementInfo: ElementInfo): Boolean {
        return try {
            elementInfo.node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } catch (e: Exception) {
            Log.e(TAG, "Error long pressing element", e)
            false
        }
    }

    /**
     * Scroll an element
     */
    private fun scrollElement(elementInfo: ElementInfo): Boolean {
        return try {
            elementInfo.node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling element", e)
            false
        }
    }

    /**
     * Check if action is a number command
     */
    private fun isNumberCommand(action: String): Boolean {
        val words = action.lowercase().split(" ")
        return words.any { word ->
            word.toIntOrNull() != null && 
            (words.contains("tap") || words.contains("click") || words.contains("select") || 
             words.contains("scroll") || words.contains("long"))
        }
    }

    /**
     * Extract number from command text
     */
    private fun extractNumberFromCommand(action: String): Int? {
        val words = action.lowercase().split(" ")
        return words.firstNotNullOfOrNull { it.toIntOrNull() }
    }

    /**
     * Clear numbered elements
     */
    private fun clearNumberedElements() {
        numberedElements.forEach { (_, _) ->
            try {
                // elementInfo.node.recycle() // Deprecated - Android handles this automatically
            } catch (e: Exception) {
                Log.w(TAG, "Error recycling node", e)
            }
        }
        numberedElements.clear()
        currentNumberCount = 0
    }

    /**
     * Display number overlays using NumberedSelectionOverlay
     *
     * TODO (Future): Integrate with overlay manager when IVoiceOSContext is extended
     * with getOverlayManager() method. For now, logs numbered elements for debugging.
     */
    private fun displayNumberOverlays() {
        try {
            Log.d(TAG, "Displaying number overlays for ${numberedElements.size} elements")

            // TODO: Implement when overlay manager is available
            // val overlayManager = service.getOverlayManager()
            // val selectableItems = numberedElements.map { (number, elementInfo) ->
            //     SelectableItem(number, elementInfo.description, elementInfo.bounds) {
            //         handleNumberCommand("tap $number")
            //     }
            // }
            // overlayManager?.showNumberedOverlay(selectableItems)

            // Log numbered elements for debugging
            numberedElements.forEach { (number, elementInfo) ->
                Log.v(TAG, "  $number: ${elementInfo.description} at ${elementInfo.bounds}")
            }
            Log.i(TAG, "Number overlay requested with ${numberedElements.size} items (overlay integration pending)")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying number overlays", e)
        }
    }

    /**
     * Hide number overlays via overlay manager
     *
     * TODO (Future): Integrate with overlay manager when available
     */
    private fun hideNumberOverlays() {
        try {
            Log.d(TAG, "Hiding number overlays")
            // TODO: Implement when overlay manager is available
            // val overlayManager = service.getOverlayManager()
            // overlayManager?.hideNumberedOverlay()
            Log.i(TAG, "Number overlay hide requested (overlay integration pending)")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding number overlays", e)
        }
    }

    /**
     * Show user feedback
     */
    private fun showFeedback(message: String) {
        try {
            Toast.makeText(service.context, message, Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Feedback: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing feedback", e)
        }
    }

    /**
     * Get current numbered elements (for external access)
     */
    fun getNumberedElements(): Map<Int, ElementInfo> {
        return numberedElements.toMap()
    }

    /**
     * Check if number overlay is visible
     */
    fun isNumberOverlayActive(): Boolean {
        return isNumberOverlayVisible
    }

    override fun dispose() {
        Log.d(TAG, "Disposing NumberHandler")
        numberScope.cancel()
        hideNumberOverlay()
        clearNumberedElements()
    }
}