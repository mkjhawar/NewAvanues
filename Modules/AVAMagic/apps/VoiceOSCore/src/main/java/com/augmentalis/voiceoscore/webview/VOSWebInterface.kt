package com.augmentalis.voiceoscore.webview

import android.webkit.JavascriptInterface
import android.util.Log
import org.json.JSONArray

/**
 * VOSWebInterface - JavaScript interface for VOSWebView
 *
 * This class is exposed to JavaScript as window.VOS and provides methods
 * for executing voice commands on web pages.
 *
 * All methods must be annotated with @JavascriptInterface for security (API 17+).
 *
 * JavaScript Usage:
 * ```javascript
 * // Execute command
 * window.VOS.executeCommand("CLICK", "//button[@id='submit']", "");
 *
 * // Get available commands
 * var commands = JSON.parse(window.VOS.getAvailableCommands());
 *
 * // Click element
 * window.VOS.clickElement("//button[@id='submit']");
 *
 * // Focus element
 * window.VOS.focusElement("//input[@name='username']");
 *
 * // Scroll to element
 * window.VOS.scrollToElement("//div[@id='content']");
 *
 * // Fill input
 * window.VOS.fillInput("//input[@name='username']", "john.doe");
 *
 * // Log event
 * window.VOS.logEvent("Custom event message");
 * ```
 *
 * @property webView The VOSWebView instance
 * @property executor The WebCommandExecutor for command execution
 * @property listener The CommandListener for event callbacks
 */
class VOSWebInterface(
    private val webView: VOSWebView,
    private val executor: WebCommandExecutor,
    private val listener: VOSWebView.CommandListener?
) {
    companion object {
        private const val TAG = "VOSWebInterface"
    }

    /**
     * Execute a voice command on the web page
     *
     * @param command The command type (CLICK, FOCUS, SCROLL_TO, FILL_INPUT)
     * @param xpath The XPath to the target element
     * @param value Optional value for FILL_INPUT command
     * @return True if command was executed successfully
     */
    @JavascriptInterface
    fun executeCommand(command: String, xpath: String, value: String = ""): Boolean {
        Log.d(TAG, "executeCommand: command=$command, xpath=$xpath, value=$value")

        // Validate inputs
        if (command.isBlank()) {
            Log.e(TAG, "Command is blank")
            listener?.onCommandError(command, "Command is blank")
            return false
        }

        if (xpath.isBlank()) {
            Log.e(TAG, "XPath is blank")
            listener?.onCommandError(command, "XPath is blank")
            return false
        }

        // Sanitize XPath to prevent injection
        val sanitizedXPath = sanitizeXPath(xpath)
        if (sanitizedXPath != xpath) {
            Log.w(TAG, "XPath was sanitized: $xpath -> $sanitizedXPath")
        }

        // Execute command
        return try {
            val result = executor.executeCommand(webView, command, sanitizedXPath, value)
            listener?.onCommandExecuted(command, result)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed", e)
            listener?.onCommandError(command, e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Get list of available commands from the current page
     *
     * @return JSON array string of available commands
     */
    @JavascriptInterface
    fun getAvailableCommands(): String {
        Log.d(TAG, "getAvailableCommands called")
        return executor.getAvailableCommands()
    }

    /**
     * Click an element by XPath
     *
     * @param xpath The XPath to the element
     * @return True if click was successful
     */
    @JavascriptInterface
    fun clickElement(xpath: String): Boolean {
        Log.d(TAG, "clickElement: xpath=$xpath")
        return executeCommand("CLICK", xpath, "")
    }

    /**
     * Focus an element by XPath
     *
     * @param xpath The XPath to the element
     * @return True if focus was successful
     */
    @JavascriptInterface
    fun focusElement(xpath: String): Boolean {
        Log.d(TAG, "focusElement: xpath=$xpath")
        return executeCommand("FOCUS", xpath, "")
    }

    /**
     * Scroll to an element by XPath
     *
     * @param xpath The XPath to the element
     * @return True if scroll was successful
     */
    @JavascriptInterface
    fun scrollToElement(xpath: String): Boolean {
        Log.d(TAG, "scrollToElement: xpath=$xpath")
        return executeCommand("SCROLL_TO", xpath, "")
    }

    /**
     * Fill an input field by XPath
     *
     * @param xpath The XPath to the input element
     * @param value The value to fill
     * @return True if fill was successful
     */
    @JavascriptInterface
    fun fillInput(xpath: String, value: String): Boolean {
        Log.d(TAG, "fillInput: xpath=$xpath, value=$value")
        return executeCommand("FILL_INPUT", xpath, value)
    }

    /**
     * Log an event from JavaScript
     *
     * @param message The message to log
     */
    @JavascriptInterface
    fun logEvent(message: String) {
        Log.d(TAG, "JavaScript: $message")
    }

    /**
     * Report discovered commands from JavaScript
     *
     * @param commandsJson JSON array string of discovered commands
     */
    @JavascriptInterface
    fun reportDiscoveredCommands(commandsJson: String) {
        Log.d(TAG, "reportDiscoveredCommands: $commandsJson")

        try {
            val jsonArray = JSONArray(commandsJson)
            val commands = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                commands.add(jsonArray.getString(i))
            }

            listener?.onCommandsDiscovered(commands)
            Log.d(TAG, "Reported ${commands.size} discovered commands")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse discovered commands", e)
            listener?.onCommandError("DISCOVER", e.message ?: "Failed to parse commands")
        }
    }

    /**
     * Sanitize XPath to prevent JavaScript injection
     *
     * Removes or escapes potentially dangerous characters:
     * - Single quotes are escaped
     * - Double quotes are escaped
     * - Script tags are removed
     * - Event handlers are removed
     *
     * @param xpath The XPath to sanitize
     * @return The sanitized XPath
     */
    private fun sanitizeXPath(xpath: String): String {
        return com.augmentalis.voiceos.text.TextSanitizers.sanitizeXPath(xpath)
    }

    /**
     * Validate that a string is safe for use in JavaScript
     *
     * @param value The value to validate
     * @return True if the value is safe
     */
    private fun isJavaScriptSafe(value: String): Boolean {
        return com.augmentalis.voiceos.text.TextSanitizers.isJavaScriptSafe(value)
    }

    /**
     * Escape a string for safe use in JavaScript
     *
     * @param value The value to escape
     * @return The escaped value
     */
    private fun escapeForJavaScript(value: String): String {
        return com.augmentalis.voiceos.text.TextSanitizers.escapeForJavaScript(value)
    }
}
