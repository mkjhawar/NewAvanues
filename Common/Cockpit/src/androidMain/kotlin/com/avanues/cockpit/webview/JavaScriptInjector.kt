package com.avanues.cockpit.webview

import android.webkit.WebView
import com.augmentalis.voiceoslogger.VoiceOsLogger

/**
 * JavaScript injection system for voice-controlled web interactions
 *
 * Ported from Task_Cockpit JsCommands.kt with Cockpit enhancements.
 * Injects JavaScript into WebView windows to enable voice command execution.
 *
 * **Core Concept:**
 * - Voice commands are mapped to JavaScript snippets
 * - When VoiceOS recognizes a command, we inject and execute the corresponding JS
 * - Enables hands-free web browsing: "Click sign in", "Fill email field", "Scroll down"
 *
 * **Architecture:**
 * - Command maps stored per domain (commands only work on specific sites)
 * - JavaScript can manipulate DOM, click buttons, fill forms, etc.
 * - Special doMouseClick() function for reliable click simulation
 *
 * **Voice-First Integration:**
 * - Primary method for web app control via VoiceOS
 * - Supports custom command files (.avaw format - encrypted JSON)
 * - Fallback to accessibility service if JS injection fails
 */
object JavaScriptInjector {

    /**
     * Command registry: domain → (voice command → JavaScript code)
     *
     * Example:
     * "github.com" → {
     *   "SIGN IN" → "document.querySelector('a[href=\"/login\"]').click();",
     *   "CREATE REPOSITORY" → "document.querySelector('[data-target=\"new-repo\"]').click();"
     * }
     */
    private val commandMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    /**
     * Injects and executes JavaScript in a WebView
     *
     * Core execution method. Takes JS code and runs it in the WebView context.
     *
     * Voice flow:
     * 1. VoiceOS hears "Click sign in"
     * 2. Cockpit looks up command for current domain
     * 3. Calls executeJavaScript() with corresponding JS
     * 4. WebView executes JS (clicks the button)
     * 5. VoiceOS announces "Clicked sign in button"
     *
     * @param webView Target WebView instance
     * @param javascript JavaScript code to execute
     * @param callback Optional callback with result
     */
    fun executeJavaScript(
        webView: WebView,
        javascript: String,
        callback: ((String) -> Unit)? = null
    ) {
        try {
            webView.evaluateJavascript(javascript) { result ->
                VoiceOsLogger.d("JavaScriptInjector", "Executed JS: $javascript, Result: $result")
                callback?.invoke(result)
            }
        } catch (e: Exception) {
            VoiceOsLogger.e("JavaScriptInjector", "Failed to execute JavaScript", e)
            callback?.invoke("ERROR: ${e.message}")
        }
    }

    /**
     * Executes a voice command for the current page
     *
     * Looks up the command in the registry for the current domain and executes it.
     *
     * Voice commands:
     * - "Click sign in" → Clicks sign-in button on current page
     * - "Fill email field" → Focuses email input
     * - "Submit form" → Submits the form
     *
     * @param webView Target WebView instance
     * @param command Voice command (uppercase, e.g., "SIGN IN")
     * @param domain Current page domain (e.g., "github.com")
     * @return True if command was found and executed
     */
    fun executeVoiceCommand(
        webView: WebView,
        command: String,
        domain: String
    ): Boolean {
        val normalizedCommand = command.uppercase().trim()
        val normalizedDomain = domain.lowercase().trim()

        val domainCommands = commandMap[normalizedDomain]
        if (domainCommands == null) {
            VoiceOsLogger.w("JavaScriptInjector", "No commands registered for domain: $normalizedDomain")
            return false
        }

        val javascript = domainCommands[normalizedCommand]
        if (javascript == null) {
            VoiceOsLogger.w("JavaScriptInjector", "Command not found: $normalizedCommand for $normalizedDomain")
            return false
        }

        executeJavaScript(webView, javascript)
        return true
    }

    /**
     * Registers a voice command for a specific domain
     *
     * Adds a new voice command to the registry.
     *
     * Example:
     * ```kotlin
     * registerCommand(
     *   domain = "github.com",
     *   command = "SIGN IN",
     *   javascript = "document.querySelector('a[href=\"/login\"]').click();"
     * )
     * ```
     *
     * @param domain Target domain (e.g., "github.com")
     * @param command Voice command (uppercase, e.g., "SIGN IN")
     * @param javascript JavaScript code to execute
     */
    fun registerCommand(domain: String, command: String, javascript: String) {
        val normalizedDomain = domain.lowercase().trim()
        val normalizedCommand = command.uppercase().trim()

        val domainCommands = commandMap.getOrPut(normalizedDomain) { mutableMapOf() }

        // Add doMouseClick() helper if JavaScript uses it
        val finalJavascript = if (javascript.contains("doMouseClick")) {
            "$javascript\n$CLICK_HELPER_FUNCTION"
        } else {
            javascript
        }

        domainCommands[normalizedCommand] = finalJavascript

        VoiceOsLogger.d("JavaScriptInjector", "Registered command: $normalizedCommand for $normalizedDomain")
    }

    /**
     * Registers multiple commands from a JSON structure
     *
     * Bulk import commands from configuration files.
     *
     * Expected JSON format:
     * ```json
     * [
     *   {
     *     "host": "github.com",
     *     "commands": [
     *       {"command": "SIGN IN", "js": "document.querySelector('a[href=\"/login\"]').click();"},
     *       {"command": "CREATE REPO", "js": "document.querySelector('[data-target=\"new-repo\"]').click();"}
     *     ]
     *   }
     * ]
     * ```
     *
     * @param commandsJson JSON string with command definitions
     */
    fun registerCommandsFromJson(commandsJson: String) {
        try {
            // TODO: Parse JSON and register commands
            // Implementation depends on JSON library used (kotlinx.serialization or Gson)
            VoiceOsLogger.w("JavaScriptInjector", "JSON parsing not yet implemented")
        } catch (e: Exception) {
            VoiceOsLogger.e("JavaScriptInjector", "Failed to parse command JSON", e)
        }
    }

    /**
     * Unregisters all commands for a domain
     *
     * Voice command: "Forget commands for [domain]"
     *
     * @param domain Target domain
     */
    fun unregisterDomain(domain: String) {
        val normalizedDomain = domain.lowercase().trim()
        commandMap.remove(normalizedDomain)
        VoiceOsLogger.d("JavaScriptInjector", "Unregistered all commands for: $normalizedDomain")
    }

    /**
     * Clears all registered commands
     *
     * Voice command: "Clear all custom commands"
     */
    fun clearAllCommands() {
        commandMap.clear()
        VoiceOsLogger.d("JavaScriptInjector", "Cleared all registered commands")
    }

    /**
     * Gets all registered commands for a domain
     *
     * Used by VoiceOS to announce available commands.
     * Voice command: "What commands are available?"
     *
     * @param domain Target domain
     * @return List of voice commands (uppercase)
     */
    fun getCommandsForDomain(domain: String): List<String> {
        val normalizedDomain = domain.lowercase().trim()
        return commandMap[normalizedDomain]?.keys?.toList() ?: emptyList()
    }

    /**
     * Gets all registered domains
     *
     * @return List of domains with custom commands
     */
    fun getRegisteredDomains(): List<String> {
        return commandMap.keys.toList()
    }

    // ==================== Built-in JavaScript Utilities ====================

    /**
     * Scrolls the page by a given amount
     *
     * Voice commands: "Scroll down", "Scroll up", "Scroll to top", "Scroll to bottom"
     *
     * @param webView Target WebView
     * @param deltaY Vertical scroll amount (positive = down, negative = up)
     */
    fun scrollPage(webView: WebView, deltaY: Int) {
        val javascript = "window.scrollBy(0, $deltaY);"
        executeJavaScript(webView, javascript)
    }

    /**
     * Scrolls to the top of the page
     *
     * Voice command: "Go to top", "Scroll to top"
     *
     * @param webView Target WebView
     */
    fun scrollToTop(webView: WebView) {
        val javascript = "window.scrollTo(0, 0);"
        executeJavaScript(webView, javascript)
    }

    /**
     * Scrolls to the bottom of the page
     *
     * Voice command: "Go to bottom", "Scroll to bottom"
     *
     * @param webView Target WebView
     */
    fun scrollToBottom(webView: WebView) {
        val javascript = "window.scrollTo(0, document.body.scrollHeight);"
        executeJavaScript(webView, javascript)
    }

    /**
     * Gets the current page title
     *
     * Used for voice announcements: "You're on GitHub homepage"
     *
     * @param webView Target WebView
     * @param callback Callback with page title
     */
    fun getPageTitle(webView: WebView, callback: (String) -> Unit) {
        val javascript = "document.title"
        executeJavaScript(webView, javascript) { result ->
            // evaluateJavascript returns JSON-encoded string, remove quotes
            val title = result.trim('"')
            callback(title)
        }
    }

    /**
     * Gets the current scroll position
     *
     * Used for state persistence (WindowState.scrollY)
     *
     * @param webView Target WebView
     * @param callback Callback with (scrollX, scrollY)
     */
    fun getScrollPosition(webView: WebView, callback: (Pair<Int, Int>) -> Unit) {
        val javascript = "[window.scrollX, window.scrollY]"
        executeJavaScript(webView, javascript) { result ->
            try {
                // Result format: "[123, 456]"
                val cleaned = result.trim('[', ']', '"')
                val parts = cleaned.split(",")
                val scrollX = parts[0].trim().toIntOrNull() ?: 0
                val scrollY = parts[1].trim().toIntOrNull() ?: 0
                callback(Pair(scrollX, scrollY))
            } catch (e: Exception) {
                VoiceOsLogger.e("JavaScriptInjector", "Failed to parse scroll position", e)
                callback(Pair(0, 0))
            }
        }
    }

    /**
     * Clicks an element at specific coordinates
     *
     * More reliable than JavaScript click events for some sites.
     * Ported from JsCommands.kt doMouseClick() function.
     *
     * Voice commands: "Click there", "Tap the button"
     *
     * @param webView Target WebView
     * @param xRatio X coordinate (0.0 to 1.0, relative to viewport width)
     * @param yRatio Y coordinate (0.0 to 1.0, relative to viewport height)
     */
    fun clickAtCoordinates(webView: WebView, xRatio: Float, yRatio: Float) {
        // Convert viewport-relative coordinates to pixels
        val pixelX = (xRatio * webView.width).toInt()
        val pixelY = (yRatio * webView.height).toInt()

        // Simulate touch event
        webView.post {
            try {
                val downTime = System.currentTimeMillis()
                val eventTime = System.currentTimeMillis() + 100

                // NOTE: Actual touch event simulation would go here
                // For now, we'll use JavaScript-based clicking as fallback
                val javascript = """
                    (function() {
                        let x = $pixelX;
                        let y = $pixelY;
                        let element = document.elementFromPoint(x, y);
                        if (element) {
                            element.click();
                            return 'Clicked element at (' + x + ', ' + y + ')';
                        } else {
                            return 'No element found at coordinates';
                        }
                    })();
                """.trimIndent()

                executeJavaScript(webView, javascript)
            } catch (e: Exception) {
                VoiceOsLogger.e("JavaScriptInjector", "Failed to click at coordinates", e)
            }
        }
    }

    /**
     * Fills a text input field
     *
     * Voice commands: "Fill email", "Type password", "Enter username"
     *
     * @param webView Target WebView
     * @param selector CSS selector for input field (e.g., "input[type='email']")
     * @param value Text to fill
     */
    fun fillTextField(webView: WebView, selector: String, value: String) {
        // Escape single quotes in value
        val escapedValue = value.replace("'", "\\'")

        val javascript = """
            (function() {
                let input = document.querySelector('$selector');
                if (input) {
                    input.value = '$escapedValue';
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'Filled field';
                } else {
                    return 'Field not found';
                }
            })();
        """.trimIndent()

        executeJavaScript(webView, javascript)
    }

    // ==================== Constants ====================

    /**
     * Mouse click helper function
     *
     * Ported from JsCommands.kt CLICK_FUNCTION.
     * More reliable than JavaScript .click() for some elements.
     *
     * Usage in custom commands:
     * ```javascript
     * let button = document.querySelector('.submit-btn');
     * doMouseClick(button);
     * ```
     */
    private const val CLICK_HELPER_FUNCTION = """
function doMouseClick(node) {
    let rect = node.getBoundingClientRect();
    let xCoordinate = (rect.left + rect.right) / (2 * window.innerWidth);
    let yCoordinate = (rect.top + rect.bottom) / (2 * window.innerHeight);

    // Call back to Kotlin/Java to perform native click
    if (window.webview && window.webview.doMouseClick) {
        window.webview.doMouseClick(xCoordinate, yCoordinate);
    } else {
        // Fallback to JavaScript click
        node.click();
    }
}
"""

    private const val LOG_TAG = "JavaScriptInjector"
}
