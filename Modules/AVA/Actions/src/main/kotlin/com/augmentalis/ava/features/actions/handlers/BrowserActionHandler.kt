/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler
import timber.log.Timber

/**
 * BrowserActionHandler - Handles web browser voice commands
 *
 * Integrates AVA voice commands with WebAvanue browser for:
 * - Dynamic page element interaction (click buttons, links)
 * - Navigation commands (back, forward, refresh)
 * - Scroll commands (scroll up, down, to top/bottom)
 * - Form interaction (type text, submit)
 *
 * When WebAvanue browser is active, this handler enables voice control
 * of any web page through automatically extracted voice commands.
 *
 * Intent Flow:
 * 1. User says "click submit button"
 * 2. NLU classifies as browser.click intent
 * 3. BrowserActionHandler extracts target "submit"
 * 4. WebVoiceCommandIntegration finds matching element
 * 5. WebAvanue browser executes click
 *
 * @created 2025-12-01
 */
class BrowserActionHandler : IntentActionHandler {

    companion object {
        private const val TAG = "BrowserActionHandler"

        // Intent patterns this handler responds to
        val SUPPORTED_INTENTS = listOf(
            "browser.click",
            "browser.scroll",
            "browser.navigate",
            "browser.type",
            "browser.search",
            "browser.open",
            "browser.go_back",
            "browser.go_forward",
            "browser.refresh",
            "browser.list_actions"
        )

        // Patterns to extract target from utterance
        private val CLICK_PATTERNS = listOf(
            Regex("""(?:click|press|tap|select)\s+(?:on\s+)?(?:the\s+)?(.+?)(?:\s+button|\s+link)?$""", RegexOption.IGNORE_CASE),
            Regex("""(?:open|go\s+to)\s+(?:the\s+)?(.+?)(?:\s+link)?$""", RegexOption.IGNORE_CASE)
        )

        private val TYPE_PATTERNS = listOf(
            Regex("""(?:type|enter|input|write)\s+(.+?)(?:\s+in(?:to)?|\s+on)?(?:\s+.+)?$""", RegexOption.IGNORE_CASE)
        )

        private val SCROLL_PATTERNS = listOf(
            Regex("""scroll\s+(up|down|left|right|to\s+top|to\s+bottom)""", RegexOption.IGNORE_CASE)
        )
    }

    override val intent = "browser.*"  // Wildcard - handles all browser.* intents

    // Reference to WebAvanue controller (set when browser is active)
    private var browserController: BrowserControllerInterface? = null

    /**
     * Interface for browser control operations
     * Implemented by WebAvanue integration layer
     */
    interface BrowserControllerInterface {
        suspend fun clickByCommand(command: String): Boolean
        suspend fun typeText(text: String, selector: String? = null): Boolean
        suspend fun scrollUp(): Boolean
        suspend fun scrollDown(): Boolean
        suspend fun scrollToTop(): Boolean
        suspend fun scrollToBottom(): Boolean
        suspend fun goBack(): Boolean
        suspend fun goForward(): Boolean
        suspend fun refresh(): Boolean
        suspend fun getAvailableCommands(): List<String>
        fun getCurrentUrl(): String
        fun getCurrentTitle(): String
    }

    /**
     * Set the browser controller (call when WebAvanue becomes active)
     */
    fun setBrowserController(controller: BrowserControllerInterface?) {
        this.browserController = controller
        Timber.i("$TAG: Browser controller ${if (controller != null) "connected" else "disconnected"}")
    }

    /**
     * Check if browser control is available
     */
    fun isBrowserActive(): Boolean = browserController != null

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        Timber.d("$TAG: Processing browser command: '$utterance'")

        val controller = browserController
        if (controller == null) {
            return ActionResult.Failure(
                message = "Browser is not active. Open the browser first to use voice commands."
            )
        }

        // Determine action type from utterance
        return when {
            isClickCommand(utterance) -> handleClick(controller, utterance)
            isTypeCommand(utterance) -> handleType(controller, utterance)
            isScrollCommand(utterance) -> handleScroll(controller, utterance)
            isNavigationCommand(utterance) -> handleNavigation(controller, utterance)
            isListCommand(utterance) -> handleListCommands(controller)
            else -> handleGenericClick(controller, utterance)
        }
    }

    // ==================== Command Detection ====================

    private fun isClickCommand(utterance: String): Boolean {
        val normalized = utterance.lowercase()
        return normalized.startsWith("click") ||
               normalized.startsWith("press") ||
               normalized.startsWith("tap") ||
               normalized.startsWith("select")
    }

    private fun isTypeCommand(utterance: String): Boolean {
        val normalized = utterance.lowercase()
        return normalized.startsWith("type") ||
               normalized.startsWith("enter") ||
               normalized.startsWith("input") ||
               normalized.startsWith("write")
    }

    private fun isScrollCommand(utterance: String): Boolean {
        return utterance.lowercase().contains("scroll")
    }

    private fun isNavigationCommand(utterance: String): Boolean {
        val normalized = utterance.lowercase()
        return normalized.contains("go back") ||
               normalized.contains("go forward") ||
               normalized.contains("refresh") ||
               normalized.contains("reload")
    }

    private fun isListCommand(utterance: String): Boolean {
        val normalized = utterance.lowercase()
        return normalized.contains("what can i") ||
               normalized.contains("list commands") ||
               normalized.contains("show actions") ||
               normalized.contains("available")
    }

    // ==================== Command Handlers ====================

    private suspend fun handleClick(
        controller: BrowserControllerInterface,
        utterance: String
    ): ActionResult {
        // Extract target from utterance
        val target = extractTarget(utterance, CLICK_PATTERNS)
        if (target.isNullOrBlank()) {
            return ActionResult.Failure(
                message = "I didn't catch what you want to click. Try saying 'click submit' or 'press login'."
            )
        }

        Timber.d("$TAG: Click target extracted: '$target'")

        // Try to find and click
        val success = controller.clickByCommand(target)
        return if (success) {
            ActionResult.Success(
                message = "Clicked $target",
                data = mapOf("action" to "click", "target" to target)
            )
        } else {
            // Check available commands for suggestions
            val available = controller.getAvailableCommands().take(5)
            val suggestion = if (available.isNotEmpty()) {
                " Try: ${available.joinToString(", ")}"
            } else {
                ""
            }
            ActionResult.Failure(
                message = "Could not find '$target' on this page.$suggestion"
            )
        }
    }

    private suspend fun handleType(
        controller: BrowserControllerInterface,
        utterance: String
    ): ActionResult {
        // Extract text to type
        val text = extractTarget(utterance, TYPE_PATTERNS)
        if (text.isNullOrBlank()) {
            return ActionResult.Failure(
                message = "What would you like to type?"
            )
        }

        val success = controller.typeText(text)
        return if (success) {
            ActionResult.Success(
                message = "Typed: $text",
                data = mapOf("action" to "type", "text" to text)
            )
        } else {
            ActionResult.Failure(
                message = "Could not type text. Make sure a text field is focused."
            )
        }
    }

    private suspend fun handleScroll(
        controller: BrowserControllerInterface,
        utterance: String
    ): ActionResult {
        val normalized = utterance.lowercase()

        val success = when {
            normalized.contains("to top") || normalized.contains("to the top") -> {
                controller.scrollToTop()
            }
            normalized.contains("to bottom") || normalized.contains("to the bottom") -> {
                controller.scrollToBottom()
            }
            normalized.contains("up") -> {
                controller.scrollUp()
            }
            normalized.contains("down") -> {
                controller.scrollDown()
            }
            else -> {
                controller.scrollDown() // Default
            }
        }

        val direction = when {
            normalized.contains("to top") -> "to top"
            normalized.contains("to bottom") -> "to bottom"
            normalized.contains("up") -> "up"
            else -> "down"
        }

        return if (success) {
            ActionResult.Success(
                message = "Scrolled $direction",
                data = mapOf("action" to "scroll", "direction" to direction)
            )
        } else {
            ActionResult.Failure(message = "Could not scroll")
        }
    }

    private suspend fun handleNavigation(
        controller: BrowserControllerInterface,
        utterance: String
    ): ActionResult {
        val normalized = utterance.lowercase()

        val (success, action) = when {
            normalized.contains("back") -> {
                controller.goBack() to "back"
            }
            normalized.contains("forward") -> {
                controller.goForward() to "forward"
            }
            normalized.contains("refresh") || normalized.contains("reload") -> {
                controller.refresh() to "refresh"
            }
            else -> {
                false to "unknown"
            }
        }

        return if (success) {
            ActionResult.Success(
                message = when (action) {
                    "back" -> "Navigated back"
                    "forward" -> "Navigated forward"
                    "refresh" -> "Page refreshed"
                    else -> "Done"
                },
                data = mapOf("action" to action)
            )
        } else {
            ActionResult.Failure(
                message = when (action) {
                    "back" -> "Cannot go back - already at first page"
                    "forward" -> "Cannot go forward - already at last page"
                    else -> "Navigation failed"
                }
            )
        }
    }

    private suspend fun handleListCommands(controller: BrowserControllerInterface): ActionResult {
        val commands = controller.getAvailableCommands()

        return if (commands.isNotEmpty()) {
            val summary = buildString {
                append("Available commands on this page: ")
                append(commands.take(10).joinToString(", "))
                if (commands.size > 10) {
                    append(", and ${commands.size - 10} more")
                }
            }
            ActionResult.Success(
                message = summary,
                data = mapOf(
                    "action" to "list",
                    "commands" to commands,
                    "count" to commands.size
                )
            )
        } else {
            ActionResult.Success(
                message = "No interactive elements found on this page.",
                data = mapOf("action" to "list", "count" to 0)
            )
        }
    }

    private suspend fun handleGenericClick(
        controller: BrowserControllerInterface,
        utterance: String
    ): ActionResult {
        // Try to click using the full utterance as the target
        val success = controller.clickByCommand(utterance)
        return if (success) {
            ActionResult.Success(
                message = "Done",
                data = mapOf("action" to "click", "target" to utterance)
            )
        } else {
            val available = controller.getAvailableCommands().take(5)
            ActionResult.Failure(
                message = "I couldn't find that on the page. " +
                        if (available.isNotEmpty()) "Try: ${available.joinToString(", ")}" else ""
            )
        }
    }

    // ==================== Helpers ====================

    private fun extractTarget(utterance: String, patterns: List<Regex>): String? {
        for (pattern in patterns) {
            val match = pattern.find(utterance)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }
}
