package com.augmentalis.voiceoscore.webview

import android.content.Context
import android.util.Log
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

/**
 * WebCommandExecutor - Executes voice commands on web pages
 *
 * This class generates and executes JavaScript code to perform actions on web pages
 * based on voice commands. It supports:
 * - CLICK: Click an element
 * - FOCUS: Focus an element
 * - SCROLL_TO: Scroll to an element
 * - FILL_INPUT: Fill an input field
 * - SUBMIT: Submit a form
 * - SELECT: Select an option from a dropdown
 * - CHECK: Check/uncheck a checkbox
 * - RADIO: Select a radio button
 *
 * All commands use XPath to target elements for maximum flexibility.
 *
 * @property context The Android context
 */
class WebCommandExecutor(private val context: Context) {

    companion object {
        private const val TAG = "WebCommandExecutor"

        // Command types
        const val CMD_CLICK = "CLICK"
        const val CMD_FOCUS = "FOCUS"
        const val CMD_SCROLL_TO = "SCROLL_TO"
        const val CMD_FILL_INPUT = "FILL_INPUT"
        const val CMD_SUBMIT = "SUBMIT"
        const val CMD_SELECT = "SELECT"
        const val CMD_CHECK = "CHECK"
        const val CMD_RADIO = "RADIO"
    }

    private val availableCommands = mutableListOf<String>()

    /**
     * Execute a command on a web page
     *
     * @param webView The WebView to execute the command in
     * @param command The command type
     * @param xpath The XPath to the target element
     * @param value Optional value for commands like FILL_INPUT
     * @return True if the command was initiated successfully
     */
    fun executeCommand(
        webView: WebView,
        command: String,
        xpath: String,
        value: String = ""
    ): Boolean {
        Log.d(TAG, "Executing command: $command on xpath: $xpath with value: $value")

        val jsCode = when (command.uppercase()) {
            CMD_CLICK -> generateClickJS(xpath)
            CMD_FOCUS -> generateFocusJS(xpath)
            CMD_SCROLL_TO -> generateScrollJS(xpath)
            CMD_FILL_INPUT -> generateFillJS(xpath, value)
            CMD_SUBMIT -> generateSubmitJS(xpath)
            CMD_SELECT -> generateSelectJS(xpath, value)
            CMD_CHECK -> generateCheckJS(xpath, value)
            CMD_RADIO -> generateRadioJS(xpath)
            else -> {
                Log.e(TAG, "Unknown command: $command")
                return false
            }
        }

        // Execute JavaScript and handle result
        webView.evaluateJavascript(jsCode) { result ->
            Log.d(TAG, "Command $command result: $result")
            val success = result?.trim('"') == "true"
            if (!success) {
                Log.w(TAG, "Command $command failed on xpath: $xpath")
            }
        }

        return true
    }

    /**
     * Get list of available commands
     *
     * @return JSON string array of available commands
     */
    fun getAvailableCommands(): String {
        val jsonArray = JSONArray(availableCommands)
        return jsonArray.toString()
    }

    /**
     * Update list of available commands
     *
     * @param commands List of command strings
     */
    fun updateAvailableCommands(commands: List<String>) {
        availableCommands.clear()
        availableCommands.addAll(commands)
        Log.d(TAG, "Updated available commands: ${commands.size} commands")
    }

    /**
     * Generate JavaScript to click an element
     *
     * @param xpath The XPath to the element
     * @return JavaScript code as a string
     */
    private fun generateClickJS(xpath: String): String = """
        (function() {
            try {
                var element = document.evaluate('$xpath', document, null,
                    XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                if (element) {
                    // Scroll into view first
                    element.scrollIntoView({behavior: 'smooth', block: 'center'});

                    // Wait a bit for scroll, then click
                    setTimeout(function() {
                        element.click();
                        console.log('[VOS] Clicked element: $xpath');
                    }, 100);

                    return true;
                } else {
                    console.error('[VOS] Element not found: $xpath');
                    return false;
                }
            } catch (e) {
                console.error('[VOS] Click error:', e);
                return false;
            }
        })();
    """.trimIndent()

    /**
     * Generate JavaScript to focus an element
     *
     * @param xpath The XPath to the element
     * @return JavaScript code as a string
     */
    private fun generateFocusJS(xpath: String): String = """
        (function() {
            try {
                var element = document.evaluate('$xpath', document, null,
                    XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                if (element) {
                    element.focus();
                    console.log('[VOS] Focused element: $xpath');
                    return true;
                } else {
                    console.error('[VOS] Element not found: $xpath');
                    return false;
                }
            } catch (e) {
                console.error('[VOS] Focus error:', e);
                return false;
            }
        })();
    """.trimIndent()

    /**
     * Generate JavaScript to scroll to an element
     *
     * @param xpath The XPath to the element
     * @return JavaScript code as a string
     */
    private fun generateScrollJS(xpath: String): String = """
        (function() {
            try {
                var element = document.evaluate('$xpath', document, null,
                    XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                if (element) {
                    element.scrollIntoView({
                        behavior: 'smooth',
                        block: 'center',
                        inline: 'nearest'
                    });
                    console.log('[VOS] Scrolled to element: $xpath');
                    return true;
                } else {
                    console.error('[VOS] Element not found: $xpath');
                    return false;
                }
            } catch (e) {
                console.error('[VOS] Scroll error:', e);
                return false;
            }
        })();
    """.trimIndent()

    /**
     * Generate JavaScript to fill an input field
     *
     * @param xpath The XPath to the input element
     * @param value The value to fill
     * @return JavaScript code as a string
     */
    private fun generateFillJS(xpath: String, value: String): String {
        // Escape value for JavaScript
        val escapedValue = value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        return """
            (function() {
                try {
                    var element = document.evaluate('$xpath', document, null,
                        XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                    if (element) {
                        // Focus the element first
                        element.focus();

                        // Set the value
                        element.value = '$escapedValue';

                        // Trigger input event for React/Vue/Angular apps
                        var event = new Event('input', { bubbles: true });
                        element.dispatchEvent(event);

                        // Trigger change event
                        var changeEvent = new Event('change', { bubbles: true });
                        element.dispatchEvent(changeEvent);

                        console.log('[VOS] Filled input: $xpath');
                        return true;
                    } else {
                        console.error('[VOS] Element not found: $xpath');
                        return false;
                    }
                } catch (e) {
                    console.error('[VOS] Fill error:', e);
                    return false;
                }
            })();
        """.trimIndent()
    }

    /**
     * Generate JavaScript to submit a form
     *
     * @param xpath The XPath to the form or submit button
     * @return JavaScript code as a string
     */
    private fun generateSubmitJS(xpath: String): String = """
        (function() {
            try {
                var element = document.evaluate('$xpath', document, null,
                    XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                if (element) {
                    // If it's a form, submit it
                    if (element.tagName.toLowerCase() === 'form') {
                        element.submit();
                    } else {
                        // Otherwise, click it (likely a submit button)
                        element.click();
                    }
                    console.log('[VOS] Submitted form: $xpath');
                    return true;
                } else {
                    console.error('[VOS] Element not found: $xpath');
                    return false;
                }
            } catch (e) {
                console.error('[VOS] Submit error:', e);
                return false;
            }
        })();
    """.trimIndent()

    /**
     * Generate JavaScript to select an option from a dropdown
     *
     * @param xpath The XPath to the select element
     * @param value The value or text of the option to select
     * @return JavaScript code as a string
     */
    private fun generateSelectJS(xpath: String, value: String): String {
        val escapedValue = value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")

        return """
            (function() {
                try {
                    var element = document.evaluate('$xpath', document, null,
                        XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                    if (element && element.tagName.toLowerCase() === 'select') {
                        // Try to find option by value
                        var option = element.querySelector('option[value="$escapedValue"]');

                        // If not found, try by text
                        if (!option) {
                            var options = element.querySelectorAll('option');
                            for (var i = 0; i < options.length; i++) {
                                if (options[i].text.trim() === '$escapedValue') {
                                    option = options[i];
                                    break;
                                }
                            }
                        }

                        if (option) {
                            element.value = option.value;

                            // Trigger change event
                            var event = new Event('change', { bubbles: true });
                            element.dispatchEvent(event);

                            console.log('[VOS] Selected option: $escapedValue');
                            return true;
                        } else {
                            console.error('[VOS] Option not found: $escapedValue');
                            return false;
                        }
                    } else {
                        console.error('[VOS] Select element not found: $xpath');
                        return false;
                    }
                } catch (e) {
                    console.error('[VOS] Select error:', e);
                    return false;
                }
            })();
        """.trimIndent()
    }

    /**
     * Generate JavaScript to check/uncheck a checkbox
     *
     * @param xpath The XPath to the checkbox
     * @param value "true" to check, "false" to uncheck, empty to toggle
     * @return JavaScript code as a string
     */
    private fun generateCheckJS(xpath: String, value: String): String {
        val shouldCheck = when (value.lowercase()) {
            "true", "1", "yes", "on" -> "true"
            "false", "0", "no", "off" -> "false"
            else -> "null" // Toggle
        }

        return """
            (function() {
                try {
                    var element = document.evaluate('$xpath', document, null,
                        XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                    if (element && element.type === 'checkbox') {
                        var shouldCheck = $shouldCheck;

                        if (shouldCheck === null) {
                            // Toggle
                            element.checked = !element.checked;
                        } else {
                            element.checked = shouldCheck;
                        }

                        // Trigger change event
                        var event = new Event('change', { bubbles: true });
                        element.dispatchEvent(event);

                        console.log('[VOS] Checkbox checked: ' + element.checked);
                        return true;
                    } else {
                        console.error('[VOS] Checkbox not found: $xpath');
                        return false;
                    }
                } catch (e) {
                    console.error('[VOS] Check error:', e);
                    return false;
                }
            })();
        """.trimIndent()
    }

    /**
     * Generate JavaScript to select a radio button
     *
     * @param xpath The XPath to the radio button
     * @return JavaScript code as a string
     */
    private fun generateRadioJS(xpath: String): String = """
        (function() {
            try {
                var element = document.evaluate('$xpath', document, null,
                    XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;

                if (element && element.type === 'radio') {
                    element.checked = true;

                    // Trigger change event
                    var event = new Event('change', { bubbles: true });
                    element.dispatchEvent(event);

                    console.log('[VOS] Radio button selected: $xpath');
                    return true;
                } else {
                    console.error('[VOS] Radio button not found: $xpath');
                    return false;
                }
            } catch (e) {
                console.error('[VOS] Radio error:', e);
                return false;
            }
        })();
    """.trimIndent()
}
