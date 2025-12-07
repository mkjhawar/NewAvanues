/**
 * NumberHandler.kt - Handler for numbered element interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-11-26
 * Implemented: 2025-11-27
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.accessibility.VoiceOSService

/**
 * Handler for numbered element interactions
 *
 * Provides voice commands to show numbered overlays on interactive elements
 * and select them by number for interaction.
 */
class NumberHandler(
    private val service: VoiceOSService
) : ActionHandler {

    companion object {
        private const val TAG = "NumberHandler"

        val SUPPORTED_ACTIONS = listOf(
            "show numbers",
            "hide numbers",
            "clear numbers",
            "number", "select number", "tap number",
            "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine"
        )
    }

    /**
     * Information about a numbered UI element
     */
    data class ElementInfo(
        val bounds: Rect,
        val description: String,
        val isClickable: Boolean,
        val viewIdResourceName: String? = null,
        val className: String? = null,
        val text: String? = null
    )

    /**
     * Current numbered elements mapping
     * Maps number -> ElementInfo
     */
    private val numberedElements = mutableMapOf<Int, ElementInfo>()

    /**
     * Whether number overlay is currently shown
     */
    private var isNumberOverlayVisible = false

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()

        Log.d(TAG, "Executing number action: $normalizedAction")

        return when {
            // Show numbers
            normalizedAction == "show numbers" -> {
                showNumbers()
                true
            }

            // Hide/Clear numbers
            normalizedAction == "hide numbers" ||
            normalizedAction == "clear numbers" -> {
                clearNumbers()
                true
            }

            // Select by number: "number 5", "tap number 3", "select number 7"
            normalizedAction.startsWith("number ") ||
            normalizedAction.startsWith("tap number ") ||
            normalizedAction.startsWith("select number ") -> {
                val numberStr = normalizedAction
                    .removePrefix("number ")
                    .removePrefix("tap number ")
                    .removePrefix("select number ")
                    .trim()

                val number = numberStr.toIntOrNull()
                if (number != null) {
                    selectNumber(number)
                } else {
                    Log.w(TAG, "Invalid number: $numberStr")
                    false
                }
            }

            // Direct number selection: "one", "two", etc.
            normalizedAction in listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine") -> {
                val number = when (normalizedAction) {
                    "one" -> 1
                    "two" -> 2
                    "three" -> 3
                    "four" -> 4
                    "five" -> 5
                    "six" -> 6
                    "seven" -> 7
                    "eight" -> 8
                    "nine" -> 9
                    else -> null
                }
                if (number != null) {
                    selectNumber(number)
                } else {
                    false
                }
            }

            else -> false
        }
    }

    override fun canHandle(action: String): Boolean {
        val normalizedAction = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { supportedAction ->
            normalizedAction.startsWith(supportedAction)
        }
    }

    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }

    /**
     * Show numbered overlay for interactive elements
     * Collects clickable elements from current screen and assigns numbers
     */
    private fun showNumbers() {
        try {
            val rootNode = service.rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "No root node available for showing numbers")
                return
            }

            // Clear previous mappings
            numberedElements.clear()

            // Collect clickable elements
            val clickableElements = mutableListOf<AccessibilityNodeInfo>()
            collectClickableElements(rootNode, clickableElements)

            // Assign numbers to elements
            clickableElements.forEachIndexed { index, node ->
                if (index < 9) { // Limit to 9 numbers for voice simplicity
                    val number = index + 1
                    val elementInfo = createElementInfo(node)
                    numberedElements[number] = elementInfo
                }
            }

            // TODO: Show actual overlay UI
            // This would integrate with NumberOverlay component
            isNumberOverlayVisible = true

            Log.d(TAG, "Showing numbers for ${numberedElements.size} elements")

            // Note: recycle() removed - Android handles AccessibilityNodeInfo cleanup automatically
            // clickableElements.forEach { it.recycle() }
            // rootNode.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "Error showing numbers", e)
        }
    }

    /**
     * Select element by number
     */
    private fun selectNumber(number: Int): Boolean {
        if (!isNumberOverlayVisible) {
            Log.w(TAG, "Number overlay not visible, cannot select number")
            return false
        }

        val elementInfo = numberedElements[number]
        if (elementInfo == null) {
            Log.w(TAG, "No element mapped to number $number")
            return false
        }

        try {
            // Find the node again and perform click
            val rootNode = service.rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "No root node available for number selection")
                return false
            }

            val targetNode = findNodeByBounds(rootNode, elementInfo.bounds)
            if (targetNode != null) {
                val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                // Note: recycle() removed - Android handles cleanup automatically
                // targetNode.recycle()
                // rootNode.recycle()

                if (result) {
                    Log.d(TAG, "Successfully clicked number $number")
                    // Clear numbers after successful selection
                    clearNumbers()
                }

                return result
            } else {
                Log.w(TAG, "Could not find node for number $number")
                // Note: recycle() removed - Android handles cleanup automatically
                // rootNode.recycle()
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error selecting number $number", e)
            return false
        }
    }

    /**
     * Clear numbered overlay
     */
    private fun clearNumbers() {
        numberedElements.clear()
        isNumberOverlayVisible = false

        // TODO: Hide actual overlay UI
        Log.d(TAG, "Cleared number overlay")
    }

    /**
     * Recursively collect clickable elements
     */
    private fun collectClickableElements(
        node: AccessibilityNodeInfo,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isClickable && node.isVisibleToUser) {
            result.add(AccessibilityNodeInfo.obtain(node))
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectClickableElements(child, result)
                // Note: recycle() removed - Android handles cleanup automatically
                // child.recycle()
            }
        }
    }

    /**
     * Create ElementInfo from AccessibilityNodeInfo
     */
    private fun createElementInfo(node: AccessibilityNodeInfo): ElementInfo {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        return ElementInfo(
            bounds = bounds,
            description = node.contentDescription?.toString() ?: node.text?.toString() ?: "",
            isClickable = node.isClickable,
            viewIdResourceName = node.viewIdResourceName,
            className = node.className?.toString(),
            text = node.text?.toString()
        )
    }

    /**
     * Find node by screen bounds
     */
    private fun findNodeByBounds(
        root: AccessibilityNodeInfo,
        targetBounds: Rect
    ): AccessibilityNodeInfo? {
        val nodeBounds = Rect()
        root.getBoundsInScreen(nodeBounds)

        if (nodeBounds == targetBounds) {
            return AccessibilityNodeInfo.obtain(root)
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                val result = findNodeByBounds(child, targetBounds)
                // Note: recycle() removed - Android handles cleanup automatically
                // child.recycle()
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }
}
