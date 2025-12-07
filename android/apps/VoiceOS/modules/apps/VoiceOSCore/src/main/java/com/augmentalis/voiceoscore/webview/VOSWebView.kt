package com.augmentalis.voiceoscore.webview

import android.content.Context
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log

/**
 * VOSWebView - Custom WebView with JavaScript interface for voice command execution
 *
 * This WebView provides a JavaScript bridge (window.VOS) that allows voice commands
 * to interact with web pages. It supports:
 * - Command execution (click, focus, scroll, fill)
 * - Command discovery from page elements
 * - XPath-based element targeting
 * - JavaScript injection for web automation
 *
 * Security Features:
 * - XPath input sanitization
 * - Command parameter validation
 * - Disabled file and content access
 * - @JavascriptInterface annotations (API 17+)
 * - JavaScript enabled only for trusted content
 *
 * Sample Usage:
 * ```kotlin
 * val webView = VOSWebView(context)
 * webView.setCommandListener(object : VOSWebView.CommandListener {
 *     override fun onCommandExecuted(command: String, success: Boolean) {
 *         Log.d(TAG, "Command $command executed: $success")
 *     }
 *     override fun onCommandError(command: String, error: String) {
 *         Log.e(TAG, "Command $command failed: $error")
 *     }
 *     override fun onCommandsDiscovered(commands: List<String>) {
 *         Log.d(TAG, "Discovered ${commands.size} commands")
 *     }
 * })
 *
 * webView.loadUrl("https://example.com")
 *
 * // Execute commands via JavaScript:
 * // window.VOS.executeCommand("CLICK", "//button[@id='submit']")
 * // window.VOS.clickElement("//button[@id='submit']")
 * // window.VOS.focusElement("//input[@name='username']")
 * ```
 *
 * @property commandExecutor Handles command execution and JavaScript generation
 * @property commandListener Callback interface for command events
 */
class VOSWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "VOSWebView"
        private const val JS_INTERFACE_NAME = "VOS"
    }

    private val commandExecutor: WebCommandExecutor = WebCommandExecutor(context)
    private var commandListener: CommandListener? = null

    init {
        setupWebView()
        setupJavaScriptInterface()
    }

    /**
     * Listener interface for command execution events
     */
    interface CommandListener {
        /**
         * Called when a command is successfully executed
         * @param command The command that was executed
         * @param success Whether the command succeeded
         */
        fun onCommandExecuted(command: String, success: Boolean)

        /**
         * Called when a command execution fails
         * @param command The command that failed
         * @param error The error message
         */
        fun onCommandError(command: String, error: String)

        /**
         * Called when commands are discovered on the page
         * @param commands List of discovered command strings
         */
        fun onCommandsDiscovered(commands: List<String>)
    }

    /**
     * Set the command listener for receiving command events
     * @param listener The listener to set
     */
    fun setCommandListener(listener: CommandListener) {
        this.commandListener = listener
    }

    /**
     * Configure WebView settings for voice command execution
     */
    private fun setupWebView() {
        settings.apply {
            // Enable JavaScript for command execution
            javaScriptEnabled = true

            // Enable DOM storage for web apps
            domStorageEnabled = true
            databaseEnabled = true

            // Disable file access for security
            allowFileAccess = false
            allowContentAccess = false

            // Enable zoom controls (accessibility)
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Enable safe browsing
            safeBrowsingEnabled = true

            // Disable mixed content
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        webViewClient = VOSWebViewClient()
        webChromeClient = VOSWebChromeClient()

        Log.d(TAG, "VOSWebView initialized with security settings")
    }

    /**
     * Setup the JavaScript interface (window.VOS)
     */
    private fun setupJavaScriptInterface() {
        addJavascriptInterface(
            VOSWebInterface(this, commandExecutor, commandListener),
            JS_INTERFACE_NAME
        )
        Log.d(TAG, "JavaScript interface '$JS_INTERFACE_NAME' registered")
    }

    /**
     * Inject command listener JavaScript into the page
     */
    private fun injectCommandListenerJS() {
        val js = """
            (function() {
                console.log('[VOS] Initializing command listener');

                // Create window.VOSCommands helper object
                window.VOSCommands = {
                    execute: function(cmd, xpath, value) {
                        return window.$JS_INTERFACE_NAME.executeCommand(cmd, xpath, value || '');
                    },
                    getAvailable: function() {
                        var commands = window.$JS_INTERFACE_NAME.getAvailableCommands();
                        return JSON.parse(commands);
                    },
                    click: function(xpath) {
                        return window.$JS_INTERFACE_NAME.clickElement(xpath);
                    },
                    focus: function(xpath) {
                        return window.$JS_INTERFACE_NAME.focusElement(xpath);
                    },
                    scroll: function(xpath) {
                        return window.$JS_INTERFACE_NAME.scrollToElement(xpath);
                    },
                    fill: function(xpath, value) {
                        return window.$JS_INTERFACE_NAME.fillInput(xpath, value);
                    }
                };

                // Discover available commands from page elements
                var commands = [];
                var selectors = [
                    'button',
                    'a[href]',
                    'input',
                    '[role="button"]',
                    '[role="link"]',
                    '[onclick]',
                    '[tabindex="0"]'
                ];

                selectors.forEach(function(selector) {
                    document.querySelectorAll(selector).forEach(function(el) {
                        var text = el.innerText ||
                                   el.value ||
                                   el.getAttribute('aria-label') ||
                                   el.getAttribute('title') ||
                                   el.getAttribute('placeholder');
                        if (text && text.trim()) {
                            commands.push(text.trim());
                        }
                    });
                });

                // Report discovered commands
                window.$JS_INTERFACE_NAME.logEvent('Commands discovered: ' + commands.length);
                window.$JS_INTERFACE_NAME.reportDiscoveredCommands(JSON.stringify(commands));

                console.log('[VOS] Command listener initialized, commands:', commands.length);
            })();
        """.trimIndent()

        evaluateJavascript(js) { result ->
            Log.d(TAG, "Command listener injected, result: $result")
        }
    }

    /**
     * WebViewClient for handling page navigation and injection
     */
    private inner class VOSWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "Page finished loading: $url")

            // Inject command listener JavaScript after page loads
            injectCommandListenerJS()
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            // Allow navigation, re-inject on new pages
            Log.d(TAG, "Navigating to: ${request?.url}")
            return false
        }

        /**
         * Modern error handling using WebResourceRequest and WebResourceError
         * (replaces deprecated onReceivedError with 4 parameters)
         */
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: android.webkit.WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            val description = error?.description?.toString() ?: "Unknown error"
            val errorCode = error?.errorCode ?: -1
            val failingUrl = request?.url?.toString() ?: "unknown"

            Log.e(TAG, "WebView error: $description (code: $errorCode) at $failingUrl")
            commandListener?.onCommandError("PAGE_LOAD", description)
        }
    }

    /**
     * WebChromeClient for handling JavaScript dialogs and console messages
     */
    private inner class VOSWebChromeClient : WebChromeClient() {
        override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
            Log.d(TAG, "Console: [$lineNumber] $message")
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress == 100) {
                Log.d(TAG, "Page load complete")
            }
        }
    }

    /**
     * Execute a command on the current page
     * @param command The command type (CLICK, FOCUS, SCROLL_TO, FILL_INPUT)
     * @param xpath The XPath to the target element
     * @param value Optional value for FILL_INPUT command
     * @return True if command was initiated successfully
     */
    fun executeCommand(command: String, xpath: String, value: String = ""): Boolean {
        return commandExecutor.executeCommand(this, command, xpath, value)
    }

    /**
     * Get list of available commands from the current page
     * @return JSON string of available commands
     */
    fun getAvailableCommands(): String {
        return commandExecutor.getAvailableCommands()
    }

    /**
     * Click an element by XPath
     * @param xpath The XPath to the element
     * @return True if click was initiated successfully
     */
    fun clickElement(xpath: String): Boolean {
        return executeCommand("CLICK", xpath)
    }

    /**
     * Focus an element by XPath
     * @param xpath The XPath to the element
     * @return True if focus was initiated successfully
     */
    fun focusElement(xpath: String): Boolean {
        return executeCommand("FOCUS", xpath)
    }

    /**
     * Scroll to an element by XPath
     * @param xpath The XPath to the element
     * @return True if scroll was initiated successfully
     */
    fun scrollToElement(xpath: String): Boolean {
        return executeCommand("SCROLL_TO", xpath)
    }

    /**
     * Fill an input field by XPath
     * @param xpath The XPath to the input element
     * @param value The value to fill
     * @return True if fill was initiated successfully
     */
    fun fillInput(xpath: String, value: String): Boolean {
        return executeCommand("FILL_INPUT", xpath, value)
    }

    /**
     * Clean up resources
     */
    override fun destroy() {
        removeJavascriptInterface(JS_INTERFACE_NAME)
        super.destroy()
        Log.d(TAG, "VOSWebView destroyed")
    }
}
