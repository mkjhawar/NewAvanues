/**
 * TreeViewHandler.kt
 *
 * Created: 2026-01-27
 * Last Modified: 2026-01-28
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for tree view navigation and manipulation
 * Features: Expand/collapse nodes, tree navigation, node selection by name
 * Location: VoiceIntegration module handlers
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler architecture with executor pattern
 * - v1.0.0 (2026-01-27): Initial implementation with full tree view support
 */

package com.augmentalis.avamagic.voice.handlers.display

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

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
 * - Delegates execution to TreeViewExecutor
 * - Implements BaseHandler for VoiceOS integration
 * - Stateless command processing
 *
 * @since 2.0.0
 */
class TreeViewHandler(
    private val executor: TreeViewExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "TreeViewHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Command prefixes for voice recognition
        private const val TREE_PREFIX = "tree"
        private const val EXPAND_PREFIX = "expand"
        private const val COLLAPSE_PREFIX = "collapse"
        private const val GO_TO_PREFIX = "go to"
        private const val SELECT_PREFIX = "select"
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
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

    /**
     * Execute tree view command
     */
    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        val commandText = command.phrase.lowercase().trim()
        Log.d { "Processing tree view command: '$commandText'" }

        return try {
            when {
                // Tree-prefixed commands
                commandText.startsWith("$TREE_PREFIX ") -> {
                    processTreePrefixedCommand(commandText.removePrefix("$TREE_PREFIX ").trim())
                }

                // Expand commands
                commandText.startsWith(EXPAND_PREFIX) -> {
                    processExpandCommand(commandText)
                }

                // Collapse commands
                commandText.startsWith(COLLAPSE_PREFIX) -> {
                    processCollapseCommand(commandText)
                }

                // Navigation commands
                commandText.startsWith(GO_TO_PREFIX) -> {
                    processNavigationCommand(commandText)
                }

                // Selection commands
                commandText.startsWith(SELECT_PREFIX) -> {
                    processSelectCommand(commandText)
                }

                // Standalone navigation commands
                isNavigationCommand(commandText) -> {
                    processStandaloneNavigationCommand(commandText)
                }

                else -> {
                    Log.d { "Unrecognized tree view command: $commandText" }
                    HandlerResult.notHandled()
                }
            }
        } catch (e: Exception) {
            Log.e({ "Error processing command: $commandText" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    /**
     * Convert executor result to HandlerResult
     */
    private fun handleResult(result: TreeViewResult, successMessage: String): HandlerResult {
        return when (result) {
            is TreeViewResult.Success -> HandlerResult.Success(
                message = result.message ?: successMessage,
                data = result.data ?: emptyMap()
            )
            is TreeViewResult.Error -> HandlerResult.failure(
                reason = result.message,
                recoverable = result.recoverable
            )
            is TreeViewResult.NotFound -> HandlerResult.failure(
                reason = result.message,
                recoverable = true
            )
            is TreeViewResult.NoAccessibility -> HandlerResult.failure(
                reason = "Accessibility service not available",
                recoverable = false
            )
        }
    }

    /**
     * Process tree-prefixed commands (e.g., "tree expand settings")
     */
    private suspend fun processTreePrefixedCommand(command: String): HandlerResult {
        return when {
            command.startsWith("expand") -> processExpandCommand(command)
            command.startsWith("collapse") -> processCollapseCommand(command)
            command.startsWith("select") -> processSelectCommand(command)
            else -> HandlerResult.notHandled()
        }
    }

    /**
     * Process expand commands
     */
    private suspend fun processExpandCommand(command: String): HandlerResult {
        val parts = command.split(" ", limit = 2)
        val target = if (parts.size > 1) parts[1].trim() else ""

        return when {
            target.equals("all", ignoreCase = true) -> {
                handleResult(executor.expandAllNodes(), "Expanded all nodes")
            }
            target.isNotEmpty() -> {
                handleResult(executor.expandNodeByName(target), "Expanded node: $target")
            }
            else -> {
                handleResult(executor.expandFocusedNode(), "Expanded focused node")
            }
        }
    }

    /**
     * Process collapse commands
     */
    private suspend fun processCollapseCommand(command: String): HandlerResult {
        val parts = command.split(" ", limit = 2)
        val target = if (parts.size > 1) parts[1].trim() else ""

        return when {
            target.equals("all", ignoreCase = true) -> {
                handleResult(executor.collapseAllNodes(), "Collapsed all nodes")
            }
            target.isNotEmpty() -> {
                handleResult(executor.collapseNodeByName(target), "Collapsed node: $target")
            }
            else -> {
                handleResult(executor.collapseFocusedNode(), "Collapsed focused node")
            }
        }
    }

    /**
     * Process navigation commands with "go to" prefix
     */
    private suspend fun processNavigationCommand(command: String): HandlerResult {
        val target = command.removePrefix(GO_TO_PREFIX).trim()

        return when (target) {
            "parent" -> handleResult(executor.navigateToParent(), "Navigated to parent node")
            "child", "first child" -> handleResult(executor.navigateToFirstChild(), "Navigated to first child")
            else -> HandlerResult.notHandled()
        }
    }

    /**
     * Process select commands
     */
    private suspend fun processSelectCommand(command: String): HandlerResult {
        val parts = command.split(" ", limit = 2)
        val nodeName = if (parts.size > 1) parts[1].trim() else ""

        return if (nodeName.isNotEmpty()) {
            handleResult(executor.selectNodeByName(nodeName), "Selected node: $nodeName")
        } else {
            Log.w { "Select command missing node name" }
            HandlerResult.failure("Select command requires a node name", recoverable = true)
        }
    }

    /**
     * Process standalone navigation commands (enter, next, previous)
     */
    private suspend fun processStandaloneNavigationCommand(command: String): HandlerResult {
        return when (command) {
            "enter" -> handleResult(executor.navigateToFirstChild(), "Entered child node")
            "next", "next sibling" -> handleResult(executor.navigateToNextSibling(), "Navigated to next sibling")
            "previous", "previous sibling" -> handleResult(executor.navigateToPreviousSibling(), "Navigated to previous sibling")
            else -> HandlerResult.notHandled()
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
}

/**
 * Result sealed class for tree view operations
 */
sealed class TreeViewResult {
    data class Success(
        val message: String? = null,
        val data: Map<String, Any>? = null
    ) : TreeViewResult()

    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : TreeViewResult()

    data class NotFound(
        val message: String = "Tree node not found"
    ) : TreeViewResult()

    data object NoAccessibility : TreeViewResult()
}

/**
 * Executor interface for tree view operations
 * Platform-specific implementations handle accessibility interactions
 */
interface TreeViewExecutor {
    /**
     * Expand the currently focused node
     */
    suspend fun expandFocusedNode(): TreeViewResult

    /**
     * Collapse the currently focused node
     */
    suspend fun collapseFocusedNode(): TreeViewResult

    /**
     * Expand a specific node by name/text
     */
    suspend fun expandNodeByName(nodeName: String): TreeViewResult

    /**
     * Collapse a specific node by name/text
     */
    suspend fun collapseNodeByName(nodeName: String): TreeViewResult

    /**
     * Expand all expandable nodes in the tree
     */
    suspend fun expandAllNodes(): TreeViewResult

    /**
     * Collapse all collapsible nodes in the tree
     */
    suspend fun collapseAllNodes(): TreeViewResult

    /**
     * Navigate to the parent of the currently focused node
     */
    suspend fun navigateToParent(): TreeViewResult

    /**
     * Navigate to the first child of the currently focused node
     */
    suspend fun navigateToFirstChild(): TreeViewResult

    /**
     * Navigate to the next sibling of the currently focused node
     */
    suspend fun navigateToNextSibling(): TreeViewResult

    /**
     * Navigate to the previous sibling of the currently focused node
     */
    suspend fun navigateToPreviousSibling(): TreeViewResult

    /**
     * Select a node by name/text and focus it
     */
    suspend fun selectNodeByName(nodeName: String): TreeViewResult

    /**
     * Get handler status
     */
    fun getStatus(): TreeViewStatus
}

/**
 * Status information for TreeViewHandler
 */
data class TreeViewStatus(
    val hasAccessibilityService: Boolean,
    val focusedNodeName: String? = null
)
