package com.avanues.voiceos.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

/**
 * Accessibility Command Executor for VoiceOS
 *
 * Interfaces with Android AccessibilityService to execute UI automation commands.
 *
 * Capabilities:
 * - Open apps by package name
 * - Find UI elements (by resource_id, text, content_description)
 * - Click elements
 * - Input text
 * - Scroll
 * - Navigate back/home
 *
 * Reference: Developer Manual Chapter 36, ADR-006
 */
class AccessibilityCommandExecutor(
    private val accessibilityService: AccessibilityService
) {

    companion object {
        private const val TAG = "AccessibilityExecutor"
        private const val ELEMENT_SEARCH_TIMEOUT_MS = 5000L // 5 seconds
        private const val ELEMENT_POLL_INTERVAL_MS = 100L // 100ms
    }

    /**
     * Execution result for command steps
     */
    data class ExecutionResult(
        val success: Boolean,
        val message: String? = null,
        val executedSteps: Int = 0,
        val failedAtStep: Int? = null
    )

    /**
     * Open app by package name
     *
     * @param packageName Package name of app to open (e.g., "com.microsoft.teams")
     * @return ExecutionResult
     */
    fun openApp(packageName: String): ExecutionResult {
        return try {
            Log.d(TAG, "Opening app: $packageName")

            val intent = accessibilityService.packageManager.getLaunchIntentForPackage(packageName)

            if (intent == null) {
                return ExecutionResult(
                    success = false,
                    message = "App not installed: $packageName",
                    failedAtStep = null
                )
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            accessibilityService.startActivity(intent)

            // Wait for app to open
            Thread.sleep(1500) // 1.5 seconds

            ExecutionResult(success = true, executedSteps = 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Failed to open app: ${e.message}",
                failedAtStep = null
            )
        }
    }

    /**
     * Click UI element
     *
     * Finds element by resource_id, text, or content_description, then clicks it.
     *
     * @param elementId Resource ID (e.g., "call_button")
     * @param text Text content to match
     * @param contentDescription Content description to match
     * @return ExecutionResult
     */
    fun clickElement(
        elementId: String? = null,
        text: String? = null,
        contentDescription: String? = null
    ): ExecutionResult {
        return try {
            Log.d(TAG, "Clicking element: id=$elementId, text=$text, desc=$contentDescription")

            val node = findElement(elementId, text, contentDescription)

            if (node == null) {
                return ExecutionResult(
                    success = false,
                    message = "Element not found: id=$elementId, text=$text, desc=$contentDescription",
                    failedAtStep = null
                )
            }

            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()

            if (!clicked) {
                return ExecutionResult(
                    success = false,
                    message = "Failed to click element (not clickable or action failed)",
                    failedAtStep = null
                )
            }

            // Wait for UI to update
            Thread.sleep(500)

            ExecutionResult(success = true, executedSteps = 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error clicking element: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Click error: ${e.message}",
                failedAtStep = null
            )
        }
    }

    /**
     * Input text into element
     *
     * @param elementId Resource ID of text field
     * @param text Text to input
     * @return ExecutionResult
     */
    fun inputText(elementId: String?, text: String): ExecutionResult {
        return try {
            Log.d(TAG, "Inputting text: id=$elementId, text=$text")

            val node = findElement(elementId = elementId)

            if (node == null) {
                return ExecutionResult(
                    success = false,
                    message = "Text field not found: $elementId",
                    failedAtStep = null
                )
            }

            // Focus on element
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            // Set text (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val arguments = android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                node.recycle()

                if (!success) {
                    return ExecutionResult(
                        success = false,
                        message = "Failed to set text (action failed)",
                        failedAtStep = null
                    )
                }
            } else {
                node.recycle()
                return ExecutionResult(
                    success = false,
                    message = "Text input not supported on Android < 5.0",
                    failedAtStep = null
                )
            }

            // Wait for UI to update
            Thread.sleep(500)

            ExecutionResult(success = true, executedSteps = 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error inputting text: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Input text error: ${e.message}",
                failedAtStep = null
            )
        }
    }

    /**
     * Select element (click + verify selection)
     *
     * @param text Text content to match
     * @param contentDescription Content description to match
     * @return ExecutionResult
     */
    fun selectElement(text: String? = null, contentDescription: String? = null): ExecutionResult {
        return try {
            Log.d(TAG, "Selecting element: text=$text, desc=$contentDescription")

            val node = findElement(text = text, contentDescription = contentDescription)

            if (node == null) {
                return ExecutionResult(
                    success = false,
                    message = "Element not found: text=$text, desc=$contentDescription",
                    failedAtStep = null
                )
            }

            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()

            if (!clicked) {
                return ExecutionResult(
                    success = false,
                    message = "Failed to select element (not clickable)",
                    failedAtStep = null
                )
            }

            // Wait for UI to update
            Thread.sleep(500)

            ExecutionResult(success = true, executedSteps = 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error selecting element: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Select error: ${e.message}",
                failedAtStep = null
            )
        }
    }

    /**
     * Scroll in direction
     *
     * @param direction "up", "down", "left", "right"
     * @return ExecutionResult
     */
    fun scroll(direction: String): ExecutionResult {
        return try {
            Log.d(TAG, "Scrolling: $direction")

            val rootNode = accessibilityService.rootInActiveWindow

            if (rootNode == null) {
                return ExecutionResult(
                    success = false,
                    message = "No active window to scroll",
                    failedAtStep = null
                )
            }

            val action = when (direction.lowercase()) {
                "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else -> {
                    rootNode.recycle()
                    return ExecutionResult(
                        success = false,
                        message = "Unsupported scroll direction: $direction",
                        failedAtStep = null
                    )
                }
            }

            val scrollable = findScrollableNode(rootNode)

            if (scrollable == null) {
                rootNode.recycle()
                return ExecutionResult(
                    success = false,
                    message = "No scrollable element found",
                    failedAtStep = null
                )
            }

            val scrolled = scrollable.performAction(action)
            scrollable.recycle()
            rootNode.recycle()

            if (!scrolled) {
                return ExecutionResult(
                    success = false,
                    message = "Scroll action failed",
                    failedAtStep = null
                )
            }

            // Wait for scroll animation
            Thread.sleep(300)

            ExecutionResult(success = true, executedSteps = 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Scroll error: ${e.message}",
                failedAtStep = null
            )
        }
    }

    /**
     * Navigate back
     *
     * @return ExecutionResult
     */
    fun navigateBack(): ExecutionResult {
        return try {
            Log.d(TAG, "Navigating back")

            val success = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

            if (!success) {
                return ExecutionResult(
                    success = false,
                    message = "Back navigation failed",
                    failedAtStep = null
                )
            }

            // Wait for navigation
            Thread.sleep(500)

            ExecutionResult(success = true, executedSteps = 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating back: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Navigate back error: ${e.message}",
                failedAtStep = null
            )
        }
    }

    /**
     * Navigate home
     *
     * @return ExecutionResult
     */
    fun navigateHome(): ExecutionResult {
        return try {
            Log.d(TAG, "Navigating home")

            val success = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

            if (!success) {
                return ExecutionResult(
                    success = false,
                    message = "Home navigation failed",
                    failedAtStep = null
                )
            }

            // Wait for navigation
            Thread.sleep(500)

            ExecutionResult(success = true, executedSteps = 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating home: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Navigate home error: ${e.message}",
                failedAtStep = null
            )
        }
    }

    // ========================================
    // Element Finding (Private Methods)
    // ========================================

    /**
     * Find UI element by resource_id, text, or content_description
     *
     * Polls for element with timeout to handle async UI updates.
     *
     * @param elementId Resource ID (e.g., "call_button" matches "com.example:id/call_button")
     * @param text Text content to match (exact or contains)
     * @param contentDescription Content description to match (exact or contains)
     * @return AccessibilityNodeInfo or null if not found
     */
    private fun findElement(
        elementId: String? = null,
        text: String? = null,
        contentDescription: String? = null
    ): AccessibilityNodeInfo? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < ELEMENT_SEARCH_TIMEOUT_MS) {
            val rootNode = accessibilityService.rootInActiveWindow

            if (rootNode != null) {
                val node = findElementRecursive(rootNode, elementId, text, contentDescription)

                if (node != null) {
                    return node
                }

                rootNode.recycle()
            }

            // Poll interval
            Thread.sleep(ELEMENT_POLL_INTERVAL_MS)
        }

        Log.w(TAG, "Element not found after ${ELEMENT_SEARCH_TIMEOUT_MS}ms: id=$elementId, text=$text, desc=$contentDescription")
        return null
    }

    /**
     * Recursive element search
     */
    private fun findElementRecursive(
        node: AccessibilityNodeInfo,
        elementId: String?,
        text: String?,
        contentDescription: String?
    ): AccessibilityNodeInfo? {
        // Match by resource ID
        if (elementId != null && node.viewIdResourceName != null) {
            if (node.viewIdResourceName.endsWith(elementId)) {
                return node
            }
        }

        // Match by text
        if (text != null && node.text != null) {
            if (node.text.toString().contains(text, ignoreCase = true)) {
                return node
            }
        }

        // Match by content description
        if (contentDescription != null && node.contentDescription != null) {
            if (node.contentDescription.toString().contains(contentDescription, ignoreCase = true)) {
                return node
            }
        }

        // Recurse through children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val found = findElementRecursive(child, elementId, text, contentDescription)

            if (found != null) {
                return found
            }

            child.recycle()
        }

        return null
    }

    /**
     * Find scrollable node
     */
    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val scrollable = findScrollableNode(child)

            if (scrollable != null) {
                return scrollable
            }

            child.recycle()
        }

        return null
    }
}
