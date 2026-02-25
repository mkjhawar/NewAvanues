/**
 * MacroContext.kt - Execution context for macro operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-09
 *
 * Purpose: Provide runtime context for macro execution
 * Key Features:
 * - Variable storage/retrieval
 * - Screen state access
 * - Accessibility service reference
 * - Execution metadata
 */
package com.augmentalis.voiceoscore.commandmanager.macros

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Macro Context
 *
 * Provides execution context for macros including:
 * - Variable storage (scoped to macro execution)
 * - Screen state inspection
 * - Accessibility service access
 * - Execution tracking
 *
 * ## Usage
 *
 * ```kotlin
 * val context = MacroContext(accessibilityService)
 *
 * // Store variables
 * context.setVariable("username", "john_doe")
 * context.setVariable("attemptCount", 3)
 *
 * // Retrieve variables
 * val username = context.getVariable<String>("username")
 * val count = context.getVariable<Int>("attemptCount")
 *
 * // Check screen state
 * if (context.screenContains("Login successful")) {
 *     // Proceed...
 * }
 *
 * // Get current package
 * val currentApp = context.getCurrentPackageName()
 * ```
 */
@Suppress("DEPRECATION") // AccessibilityEvent.recycle() and obtain() deprecated but no replacement yet
class MacroContext(
    private val accessibilityService: AccessibilityService
) {

    // Variable storage (scoped to this macro execution)
    private val variables = mutableMapOf<String, Any>()

    // Execution state
    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.IDLE)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    // Execution metadata
    var startTime: Long = 0
        private set
    var currentStepIndex: Int = 0
        private set
    var totalSteps: Int = 0
        private set

    /**
     * Set a variable in the context
     *
     * @param name Variable name
     * @param value Variable value
     */
    fun setVariable(name: String, value: Any) {
        variables[name] = value
    }

    /**
     * Get a variable from the context
     *
     * @param name Variable name
     * @return Variable value or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getVariable(name: String): T? {
        return variables[name] as? T
    }

    /**
     * Check if a variable exists
     *
     * @param name Variable name
     * @return true if variable exists
     */
    fun hasVariable(name: String): Boolean {
        return variables.containsKey(name)
    }

    /**
     * Remove a variable
     *
     * @param name Variable name
     */
    fun removeVariable(name: String) {
        variables.remove(name)
    }

    /**
     * Clear all variables
     */
    fun clearVariables() {
        variables.clear()
    }

    /**
     * Get all variable names
     *
     * @return Set of variable names
     */
    fun getVariableNames(): Set<String> {
        return variables.keys.toSet()
    }

    /**
     * Get root accessibility node
     *
     * @return Root AccessibilityNodeInfo or null
     */
    fun getRootNode(): AccessibilityNodeInfo? {
        return accessibilityService.rootInActiveWindow
    }

    /**
     * Check if screen contains text
     *
     * Searches the entire accessibility tree for the specified text.
     *
     * @param text Text to search for (case-insensitive)
     * @return true if text is found anywhere on screen
     */
    fun screenContains(text: String): Boolean {
        val rootNode = getRootNode() ?: return false
        return try {
            findTextInNode(rootNode, text.lowercase())
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Recursively search for text in accessibility tree
     */
    private fun findTextInNode(node: AccessibilityNodeInfo, searchText: String): Boolean {
        // Check node's text
        if (node.text?.toString()?.lowercase()?.contains(searchText) == true) {
            return true
        }

        // Check content description
        if (node.contentDescription?.toString()?.lowercase()?.contains(searchText) == true) {
            return true
        }

        // Check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (findTextInNode(child, searchText)) {
                    return true
                }
            } finally {
                child.recycle()
            }
        }

        return false
    }

    /**
     * Get current package name (foreground app)
     *
     * @return Package name or null
     */
    fun getCurrentPackageName(): String? {
        return getRootNode()?.use { it.packageName?.toString() }
    }

    /**
     * Find node by text
     *
     * @param text Text to search for
     * @return First matching AccessibilityNodeInfo or null
     */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = getRootNode() ?: return null
        return findNodeByTextRecursive(rootNode, text.lowercase())
    }

    /**
     * Recursively find node by text
     */
    private fun findNodeByTextRecursive(node: AccessibilityNodeInfo, searchText: String): AccessibilityNodeInfo? {
        // Check this node
        if (node.text?.toString()?.lowercase()?.contains(searchText) == true) {
            return node
        }

        if (node.contentDescription?.toString()?.lowercase()?.contains(searchText) == true) {
            return node
        }

        // Check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByTextRecursive(child, searchText)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }

        return null
    }

    /**
     * Check if element with text is clickable
     *
     * @param text Text to search for
     * @return true if found and clickable
     */
    fun isClickable(text: String): Boolean {
        val node = findNodeByText(text)
        return try {
            node?.isClickable == true
        } finally {
            node?.recycle()
        }
    }

    /**
     * Check if element with text is enabled
     *
     * @param text Text to search for
     * @return true if found and enabled
     */
    fun isEnabled(text: String): Boolean {
        val node = findNodeByText(text)
        return try {
            node?.isEnabled == true
        } finally {
            node?.recycle()
        }
    }

    /**
     * Count nodes with text
     *
     * @param text Text to search for
     * @return Number of nodes containing text
     */
    fun countNodesWithText(text: String): Int {
        val rootNode = getRootNode() ?: return 0
        return try {
            countNodesRecursive(rootNode, text.lowercase())
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Recursively count nodes with text
     */
    private fun countNodesRecursive(node: AccessibilityNodeInfo, searchText: String): Int {
        var count = 0

        // Check this node
        if (node.text?.toString()?.lowercase()?.contains(searchText) == true ||
            node.contentDescription?.toString()?.lowercase()?.contains(searchText) == true) {
            count++
        }

        // Check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                count += countNodesRecursive(child, searchText)
            } finally {
                child.recycle()
            }
        }

        return count
    }

    /**
     * Get accessibility service reference
     *
     * Provides direct access to the accessibility service for advanced operations.
     *
     * @return AccessibilityService instance
     */
    fun getAccessibilityService(): AccessibilityService {
        return accessibilityService
    }

    /**
     * Update execution state
     */
    internal fun updateState(newState: ExecutionState) {
        _executionState.value = newState
    }

    /**
     * Start execution tracking
     *
     * @param totalSteps Total number of steps in macro
     */
    internal fun startExecution(totalSteps: Int) {
        this.startTime = System.currentTimeMillis()
        this.currentStepIndex = 0
        this.totalSteps = totalSteps
        updateState(ExecutionState.RUNNING)
    }

    /**
     * Update current step index
     *
     * @param stepIndex Current step index
     */
    internal fun updateStepIndex(stepIndex: Int) {
        this.currentStepIndex = stepIndex
    }

    /**
     * Complete execution
     */
    internal fun completeExecution() {
        updateState(ExecutionState.COMPLETED)
    }

    /**
     * Fail execution
     *
     * @param error Error message
     */
    internal fun failExecution(error: String) {
        updateState(ExecutionState.FAILED(error))
    }

    /**
     * Get execution duration in milliseconds
     *
     * @return Duration since start or 0 if not started
     */
    fun getExecutionDuration(): Long {
        return if (startTime > 0) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
    }

    /**
     * Get execution progress (0.0 to 1.0)
     *
     * @return Progress as float
     */
    fun getProgress(): Float {
        return if (totalSteps > 0) {
            currentStepIndex.toFloat() / totalSteps.toFloat()
        } else {
            0f
        }
    }

    /**
     * Reset context for new execution
     */
    fun reset() {
        clearVariables()
        startTime = 0
        currentStepIndex = 0
        totalSteps = 0
        updateState(ExecutionState.IDLE)
    }
}

/**
 * Execution State - Represents macro execution state
 */
sealed class ExecutionState {
    object IDLE : ExecutionState()
    object RUNNING : ExecutionState()
    object PAUSED : ExecutionState()
    object COMPLETED : ExecutionState()
    data class FAILED(val error: String) : ExecutionState()
}

/**
 * Extension function to safely use AccessibilityNodeInfo
 */
@Suppress("DEPRECATION")
private fun <T> AccessibilityNodeInfo.use(block: (AccessibilityNodeInfo) -> T): T {
    return try {
        block(this)
    } finally {
        this.recycle()
    }
}
