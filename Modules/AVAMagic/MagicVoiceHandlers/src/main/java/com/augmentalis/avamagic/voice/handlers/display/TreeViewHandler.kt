/**
 * TreeViewHandler.kt
 *
 * Created: 2026-01-27
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for tree view navigation and manipulation
 * Features: Expand/collapse nodes, tree navigation, node selection by name
 * Location: CommandManager module (handlers)
 *
 * Changelog:
 * - v1.0.0 (2026-01-27): Initial implementation with full tree view support
 */

package com.augmentalis.avamagic.voice.handlers.display

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Voice command handler for tree view navigation and manipulation.
 *
 * Supports voice commands for:
 * - Expand/collapse specific nodes or all nodes
 * - Navigate to parent, child, or sibling nodes
 * - Select nodes by name
 *
 * Design:
 * - Command parsing and routing only (no direct UI manipulation)
 * - Delegates execution to accessibility service actions
 * - Implements CommandHandler for CommandRegistry integration
 * - Thread-safe singleton pattern
 *
 * @since 1.0.0
 */
class TreeViewHandler private constructor(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "TreeViewHandler"
        private const val MODULE_ID = "tree_view"

        @Volatile
        private var instance: TreeViewHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): TreeViewHandler {
            return instance ?: synchronized(this) {
                instance ?: TreeViewHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // Command prefixes for voice recognition
        private const val TREE_PREFIX = "tree"
        private const val EXPAND_PREFIX = "expand"
        private const val COLLAPSE_PREFIX = "collapse"
        private const val GO_TO_PREFIX = "go to"
        private const val SELECT_PREFIX = "select"

        // Tree view class names to identify tree nodes
        private val TREE_VIEW_CLASSES = setOf(
            "android.widget.ExpandableListView",
            "androidx.recyclerview.widget.RecyclerView",
            "android.widget.TreeView",
            "android.widget.ListView"
        )

        // Expandable node class indicators
        private val EXPANDABLE_CLASSES = setOf(
            "android.widget.ExpandableListView",
            "android.widget.TreeView"
        )
    }

    // CommandHandler interface implementation
    override val moduleId: String = MODULE_ID

    override val supportedCommands: List<String> = listOf(
        // Expand commands
        "expand [node]",
        "expand all",
        "tree expand [node]",
        "tree expand all",

        // Collapse commands
        "collapse [node]",
        "collapse all",
        "tree collapse [node]",
        "tree collapse all",

        // Navigation commands
        "go to parent",
        "go to child",
        "enter",
        "next sibling",
        "next",
        "previous sibling",
        "previous",

        // Selection commands
        "select [node]",
        "tree select [node]"
    )

    private val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Integration state
    private var isInitialized = false
    private var isRegistered = false

    // Accessibility service reference holder
    private var accessibilityServiceProvider: (() -> android.accessibilityservice.AccessibilityService?)? = null

    init {
        initialize()
        // Register with CommandRegistry automatically
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize command handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            isInitialized = true
            Log.d(TAG, "TreeViewHandler initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * Set the accessibility service provider for tree operations.
     * Must be called before tree operations can function.
     *
     * @param provider Lambda that returns the current AccessibilityService or null
     */
    fun setAccessibilityServiceProvider(provider: () -> android.accessibilityservice.AccessibilityService?) {
        accessibilityServiceProvider = provider
        Log.d(TAG, "Accessibility service provider set")
    }

    /**
     * CommandHandler interface: Check if this handler can process the command
     * (command is already normalized by CommandRegistry)
     */
    override fun canHandle(command: String): Boolean {
        return when {
            command.startsWith(TREE_PREFIX) -> true
            command.startsWith(EXPAND_PREFIX) -> true
            command.startsWith(COLLAPSE_PREFIX) -> true
            command.startsWith(GO_TO_PREFIX) -> true
            command.startsWith(SELECT_PREFIX) -> true
            isNavigationCommand(command) -> true
            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     * (command is already normalized by CommandRegistry)
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized for command processing")
            return false
        }

        Log.d(TAG, "Processing tree view command: '$command'")

        return try {
            when {
                // Tree-prefixed commands
                command.startsWith("$TREE_PREFIX ") -> {
                    processTreePrefixedCommand(command.removePrefix("$TREE_PREFIX ").trim())
                }

                // Expand commands
                command.startsWith(EXPAND_PREFIX) -> {
                    processExpandCommand(command)
                }

                // Collapse commands
                command.startsWith(COLLAPSE_PREFIX) -> {
                    processCollapseCommand(command)
                }

                // Navigation commands
                command.startsWith(GO_TO_PREFIX) -> {
                    processNavigationCommand(command)
                }

                // Selection commands
                command.startsWith(SELECT_PREFIX) -> {
                    processSelectCommand(command)
                }

                // Standalone navigation commands
                isNavigationCommand(command) -> {
                    processStandaloneNavigationCommand(command)
                }

                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $command", e)
            false
        }
    }

    /**
     * Process tree-prefixed commands (e.g., "tree expand settings")
     */
    private suspend fun processTreePrefixedCommand(command: String): Boolean {
        return when {
            command.startsWith("expand") -> processExpandCommand(command)
            command.startsWith("collapse") -> processCollapseCommand(command)
            command.startsWith("select") -> processSelectCommand(command)
            else -> false
        }
    }

    /**
     * Process expand commands
     */
    private suspend fun processExpandCommand(command: String): Boolean {
        val parts = command.split(" ", limit = 2)
        val target = if (parts.size > 1) parts[1].trim() else ""

        return when {
            target.equals("all", ignoreCase = true) -> {
                expandAllNodes()
            }
            target.isNotEmpty() -> {
                expandNodeByName(target)
            }
            else -> {
                // Expand focused node
                expandFocusedNode()
            }
        }
    }

    /**
     * Process collapse commands
     */
    private suspend fun processCollapseCommand(command: String): Boolean {
        val parts = command.split(" ", limit = 2)
        val target = if (parts.size > 1) parts[1].trim() else ""

        return when {
            target.equals("all", ignoreCase = true) -> {
                collapseAllNodes()
            }
            target.isNotEmpty() -> {
                collapseNodeByName(target)
            }
            else -> {
                // Collapse focused node
                collapseFocusedNode()
            }
        }
    }

    /**
     * Process navigation commands with "go to" prefix
     */
    private suspend fun processNavigationCommand(command: String): Boolean {
        val target = command.removePrefix(GO_TO_PREFIX).trim()

        return when (target) {
            "parent" -> navigateToParent()
            "child", "first child" -> navigateToFirstChild()
            else -> false
        }
    }

    /**
     * Process select commands
     */
    private suspend fun processSelectCommand(command: String): Boolean {
        val parts = command.split(" ", limit = 2)
        val nodeName = if (parts.size > 1) parts[1].trim() else ""

        return if (nodeName.isNotEmpty()) {
            selectNodeByName(nodeName)
        } else {
            Log.w(TAG, "Select command missing node name")
            false
        }
    }

    /**
     * Process standalone navigation commands (enter, next, previous)
     */
    private suspend fun processStandaloneNavigationCommand(command: String): Boolean {
        return when (command) {
            "enter" -> navigateToFirstChild()
            "next", "next sibling" -> navigateToNextSibling()
            "previous", "previous sibling" -> navigateToPreviousSibling()
            else -> false
        }
    }

    /**
     * Check if command is a standalone navigation command
     */
    private fun isNavigationCommand(command: String): Boolean {
        return command in setOf(
            "enter", "next", "next sibling", "previous", "previous sibling"
        )
    }

    // ==================== Tree Operations ====================

    /**
     * Expand the currently focused node
     */
    private fun expandFocusedNode(): Boolean {
        val service = getAccessibilityService() ?: return false
        val focusedNode = getFocusedNode(service) ?: return false

        return try {
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
            if (result) {
                Log.d(TAG, "Expanded focused node")
            } else {
                Log.w(TAG, "Failed to expand focused node - may not be expandable")
            }
            result
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Collapse the currently focused node
     */
    private fun collapseFocusedNode(): Boolean {
        val service = getAccessibilityService() ?: return false
        val focusedNode = getFocusedNode(service) ?: return false

        return try {
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
            if (result) {
                Log.d(TAG, "Collapsed focused node")
            } else {
                Log.w(TAG, "Failed to collapse focused node - may not be collapsible")
            }
            result
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Expand a specific node by name/text
     */
    private fun expandNodeByName(nodeName: String): Boolean {
        val service = getAccessibilityService() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        return try {
            val targetNode = findNodeByText(rootNode, nodeName)
            if (targetNode != null) {
                val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
                if (result) {
                    Log.d(TAG, "Expanded node: $nodeName")
                } else {
                    Log.w(TAG, "Failed to expand node: $nodeName - may not be expandable")
                }
                targetNode.recycle()
                result
            } else {
                Log.w(TAG, "Node not found: $nodeName")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Collapse a specific node by name/text
     */
    private fun collapseNodeByName(nodeName: String): Boolean {
        val service = getAccessibilityService() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        return try {
            val targetNode = findNodeByText(rootNode, nodeName)
            if (targetNode != null) {
                val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
                if (result) {
                    Log.d(TAG, "Collapsed node: $nodeName")
                } else {
                    Log.w(TAG, "Failed to collapse node: $nodeName - may not be collapsible")
                }
                targetNode.recycle()
                result
            } else {
                Log.w(TAG, "Node not found: $nodeName")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Expand all expandable nodes in the tree
     */
    private fun expandAllNodes(): Boolean {
        val service = getAccessibilityService() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        return try {
            val expandedCount = performActionOnAllExpandableNodes(rootNode, AccessibilityNodeInfo.ACTION_EXPAND)
            Log.d(TAG, "Expanded $expandedCount nodes")
            expandedCount > 0
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Collapse all collapsible nodes in the tree
     */
    private fun collapseAllNodes(): Boolean {
        val service = getAccessibilityService() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        return try {
            val collapsedCount = performActionOnAllExpandableNodes(rootNode, AccessibilityNodeInfo.ACTION_COLLAPSE)
            Log.d(TAG, "Collapsed $collapsedCount nodes")
            collapsedCount > 0
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Navigate to the parent of the currently focused node
     */
    private fun navigateToParent(): Boolean {
        val service = getAccessibilityService() ?: return false
        val focusedNode = getFocusedNode(service) ?: return false

        return try {
            val parent = focusedNode.parent
            if (parent != null) {
                val result = parent.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                if (result) {
                    Log.d(TAG, "Navigated to parent node")
                } else {
                    Log.w(TAG, "Failed to focus parent node")
                }
                parent.recycle()
                result
            } else {
                Log.w(TAG, "No parent node available")
                false
            }
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Navigate to the first child of the currently focused node
     */
    private fun navigateToFirstChild(): Boolean {
        val service = getAccessibilityService() ?: return false
        val focusedNode = getFocusedNode(service) ?: return false

        return try {
            if (focusedNode.childCount > 0) {
                val firstChild = focusedNode.getChild(0)
                if (firstChild != null) {
                    val result = firstChild.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                    if (result) {
                        Log.d(TAG, "Navigated to first child node")
                    } else {
                        Log.w(TAG, "Failed to focus first child node")
                    }
                    firstChild.recycle()
                    result
                } else {
                    Log.w(TAG, "First child is null")
                    false
                }
            } else {
                Log.w(TAG, "No child nodes available")
                false
            }
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Navigate to the next sibling of the currently focused node
     */
    private fun navigateToNextSibling(): Boolean {
        val service = getAccessibilityService() ?: return false
        val focusedNode = getFocusedNode(service) ?: return false

        return try {
            val parent = focusedNode.parent
            if (parent != null) {
                val currentIndex = findChildIndex(parent, focusedNode)
                if (currentIndex >= 0 && currentIndex < parent.childCount - 1) {
                    val nextSibling = parent.getChild(currentIndex + 1)
                    if (nextSibling != null) {
                        val result = nextSibling.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                        if (result) {
                            Log.d(TAG, "Navigated to next sibling")
                        }
                        nextSibling.recycle()
                        parent.recycle()
                        return result
                    }
                }
                parent.recycle()
                Log.w(TAG, "No next sibling available")
                false
            } else {
                Log.w(TAG, "No parent node - cannot find siblings")
                false
            }
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Navigate to the previous sibling of the currently focused node
     */
    private fun navigateToPreviousSibling(): Boolean {
        val service = getAccessibilityService() ?: return false
        val focusedNode = getFocusedNode(service) ?: return false

        return try {
            val parent = focusedNode.parent
            if (parent != null) {
                val currentIndex = findChildIndex(parent, focusedNode)
                if (currentIndex > 0) {
                    val prevSibling = parent.getChild(currentIndex - 1)
                    if (prevSibling != null) {
                        val result = prevSibling.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                        if (result) {
                            Log.d(TAG, "Navigated to previous sibling")
                        }
                        prevSibling.recycle()
                        parent.recycle()
                        return result
                    }
                }
                parent.recycle()
                Log.w(TAG, "No previous sibling available")
                false
            } else {
                Log.w(TAG, "No parent node - cannot find siblings")
                false
            }
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Select a node by name/text and focus it
     */
    private fun selectNodeByName(nodeName: String): Boolean {
        val service = getAccessibilityService() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        return try {
            val targetNode = findNodeByText(rootNode, nodeName)
            if (targetNode != null) {
                // Try to select and focus the node
                var result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SELECT)
                if (!result) {
                    // Fallback to accessibility focus
                    result = targetNode.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                }
                if (!result) {
                    // Fallback to regular focus
                    result = targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                }

                if (result) {
                    Log.d(TAG, "Selected node: $nodeName")
                } else {
                    Log.w(TAG, "Failed to select node: $nodeName")
                }
                targetNode.recycle()
                result
            } else {
                Log.w(TAG, "Node not found: $nodeName")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Get the accessibility service instance
     */
    private fun getAccessibilityService(): android.accessibilityservice.AccessibilityService? {
        val service = accessibilityServiceProvider?.invoke()
        if (service == null) {
            Log.w(TAG, "Accessibility service not available")
        }
        return service
    }

    /**
     * Get the currently focused accessibility node
     */
    private fun getFocusedNode(service: android.accessibilityservice.AccessibilityService): AccessibilityNodeInfo? {
        // Try accessibility focus first
        var focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (focused != null) return focused

        // Fall back to input focus
        focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) return focused

        // As last resort, try to get the first focusable node from root
        val root = service.rootInActiveWindow
        if (root != null) {
            focused = findFirstFocusableNode(root)
            if (focused != root) {
                root.recycle()
            }
        }

        if (focused == null) {
            Log.w(TAG, "No focused node found")
        }
        return focused
    }

    /**
     * Find a node by its text content or content description
     */
    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val searchText = text.lowercase()

        // Check current node
        val nodeText = rootNode.text?.toString()?.lowercase() ?: ""
        val contentDesc = rootNode.contentDescription?.toString()?.lowercase() ?: ""

        if (nodeText.contains(searchText) || contentDesc.contains(searchText)) {
            return AccessibilityNodeInfo.obtain(rootNode)
        }

        // Recursively search children
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) {
                if (found != child) {
                    child.recycle()
                }
                return found
            }
            child.recycle()
        }

        return null
    }

    /**
     * Find the first focusable node in the tree
     */
    private fun findFirstFocusableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocusable || node.isAccessibilityFocused) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val focusable = findFirstFocusableNode(child)
            if (focusable != null) {
                if (focusable != child) {
                    child.recycle()
                }
                return focusable
            }
            child.recycle()
        }

        return null
    }

    /**
     * Find the index of a child node within its parent
     */
    private fun findChildIndex(parent: AccessibilityNodeInfo, child: AccessibilityNodeInfo): Int {
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i)
            if (sibling != null) {
                val matches = nodesMatch(sibling, child)
                sibling.recycle()
                if (matches) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Check if two nodes represent the same UI element
     */
    private fun nodesMatch(node1: AccessibilityNodeInfo, node2: AccessibilityNodeInfo): Boolean {
        // Compare key properties to determine if nodes match
        return node1.className == node2.className &&
                node1.viewIdResourceName == node2.viewIdResourceName &&
                node1.text?.toString() == node2.text?.toString() &&
                node1.contentDescription?.toString() == node2.contentDescription?.toString()
    }

    /**
     * Perform an action on all expandable/collapsible nodes in the tree
     * @return Number of nodes the action was performed on
     */
    private fun performActionOnAllExpandableNodes(
        rootNode: AccessibilityNodeInfo,
        action: Int
    ): Int {
        var count = 0

        // Check if current node can perform the action
        val actionList = rootNode.actionList
        val canPerformAction = actionList?.any { it.id == action } == true

        if (canPerformAction) {
            if (rootNode.performAction(action)) {
                count++
            }
        }

        // Recursively process children
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            count += performActionOnAllExpandableNodes(child, action)
            child.recycle()
        }

        return count
    }

    /**
     * Check if handler is ready for operation
     */
    fun isReady(): Boolean = isInitialized && accessibilityServiceProvider != null

    /**
     * Get handler status
     */
    fun getStatus(): TreeViewHandlerStatus {
        return TreeViewHandlerStatus(
            isInitialized = isInitialized,
            hasAccessibilityService = accessibilityServiceProvider != null,
            commandsSupported = supportedCommands.size
        )
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        // Unregister from CommandRegistry
        CommandRegistry.unregisterHandler(moduleId)
        commandScope.cancel()
        accessibilityServiceProvider = null
        instance = null
        Log.d(TAG, "TreeViewHandler disposed")
    }
}

/**
 * Status information for TreeViewHandler
 */
data class TreeViewHandlerStatus(
    val isInitialized: Boolean,
    val hasAccessibilityService: Boolean,
    val commandsSupported: Int
)
