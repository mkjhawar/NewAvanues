package com.augmentalis.voiceoscore.webview

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * VOSWebViewSample - Sample usage of VOSWebView
 *
 * This file demonstrates how to use VOSWebView with voice commands.
 * It shows:
 * - Basic setup and configuration
 * - Command listener implementation
 * - Command execution from Kotlin
 * - Command execution from JavaScript
 * - Error handling
 * - Command discovery
 */
object VOSWebViewSample {

    private const val TAG = "VOSWebViewSample"

    /**
     * Example 1: Basic Setup
     *
     * Create a VOSWebView and set up command listener
     */
    fun basicSetup(context: Context): VOSWebView {
        // Create WebView
        val webView = VOSWebView(context)

        // Configure layout
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Set command listener
        webView.setCommandListener(object : VOSWebView.CommandListener {
            override fun onCommandExecuted(command: String, success: Boolean) {
                Log.d(TAG, "Command executed: $command, success: $success")
                // Handle success - e.g., show toast, update UI
            }

            override fun onCommandError(command: String, error: String) {
                Log.e(TAG, "Command error: $command, error: $error")
                // Handle error - e.g., show error message, retry
            }

            override fun onCommandsDiscovered(commands: List<String>) {
                Log.d(TAG, "Commands discovered: ${commands.size}")
                commands.forEach { cmd ->
                    Log.d(TAG, "  - $cmd")
                }
                // Handle discovered commands - e.g., update voice command list
            }
        })

        // Load a URL
        webView.loadUrl("https://example.com")

        return webView
    }

    /**
     * Example 2: Execute Commands from Kotlin
     *
     * Execute voice commands programmatically from Kotlin code
     */
    fun executeCommandsFromKotlin(webView: VOSWebView) {
        // Click a button
        webView.clickElement("//button[@id='submit']")

        // Focus an input field
        webView.focusElement("//input[@name='username']")

        // Fill an input field
        webView.fillInput("//input[@name='username']", "john.doe")

        // Scroll to an element
        webView.scrollToElement("//div[@id='content']")

        // Execute generic command
        webView.executeCommand("CLICK", "//a[@href='/login']")
    }

    /**
     * Example 3: Execute Commands from JavaScript
     *
     * Show how to execute commands from JavaScript code injected into the page
     */
    fun executeCommandsFromJavaScript(webView: VOSWebView) {
        // Method 1: Using window.VOS directly
        val js1 = """
            window.VOS.clickElement("//button[@id='submit']");
        """.trimIndent()
        webView.evaluateJavascript(js1, null)

        // Method 2: Using window.VOSCommands helper
        val js2 = """
            window.VOSCommands.click("//button[@id='submit']");
            window.VOSCommands.focus("//input[@name='username']");
            window.VOSCommands.fill("//input[@name='username']", "john.doe");
        """.trimIndent()
        webView.evaluateJavascript(js2, null)

        // Method 3: Get available commands
        val js3 = """
            var commands = window.VOSCommands.getAvailable();
            console.log('Available commands:', commands);
        """.trimIndent()
        webView.evaluateJavascript(js3, null)
    }

    /**
     * Example 4: Voice Command Integration
     *
     * Integrate with voice recognition to execute commands
     */
    fun voiceCommandIntegration(
        webView: VOSWebView,
        voiceCommand: String
    ) {
        // Parse voice command and execute appropriate action
        when {
            voiceCommand.startsWith("click", ignoreCase = true) -> {
                // Extract target from voice command
                // E.g., "click submit button" -> "submit"
                val target = extractTarget(voiceCommand)
                val xpath = findElementByText(target)
                webView.clickElement(xpath)
            }

            voiceCommand.startsWith("fill", ignoreCase = true) -> {
                // E.g., "fill username with john doe"
                val (field, value) = extractFieldAndValue(voiceCommand)
                val xpath = findInputByName(field)
                webView.fillInput(xpath, value)
            }

            voiceCommand.startsWith("scroll to", ignoreCase = true) -> {
                val target = extractTarget(voiceCommand)
                val xpath = findElementByText(target)
                webView.scrollToElement(xpath)
            }

            voiceCommand.startsWith("focus", ignoreCase = true) -> {
                val target = extractTarget(voiceCommand)
                val xpath = findElementByText(target)
                webView.focusElement(xpath)
            }

            else -> {
                Log.w(TAG, "Unknown voice command: $voiceCommand")
            }
        }
    }

    /**
     * Example 5: Advanced Usage - Form Filling
     *
     * Fill an entire form with voice commands
     */
    fun fillForm(webView: VOSWebView) {
        // Fill login form
        webView.fillInput("//input[@name='username']", "john.doe")
        webView.fillInput("//input[@name='password']", "secret123")
        webView.clickElement("//button[@type='submit']")

        // Or use sequential commands with delays
        webView.fillInput("//input[@name='username']", "john.doe")

        // Wait for any validation
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            webView.fillInput("//input[@name='password']", "secret123")
        }, 500)

        // Wait again, then submit
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            webView.clickElement("//button[@type='submit']")
        }, 1000)
    }

    /**
     * Example 6: Error Handling
     *
     * Handle errors and retry logic
     */
    fun errorHandlingExample(webView: VOSWebView) {
        webView.setCommandListener(object : VOSWebView.CommandListener {
            override fun onCommandExecuted(command: String, success: Boolean) {
                if (success) {
                    Log.d(TAG, "Command succeeded: $command")
                } else {
                    Log.w(TAG, "Command failed: $command - retrying...")
                    retryCommand(webView, command)
                }
            }

            override fun onCommandError(command: String, error: String) {
                Log.e(TAG, "Command error: $command - $error")

                when {
                    error.contains("Element not found") -> {
                        // Try alternative XPath
                        Log.d(TAG, "Trying alternative element locator...")
                    }

                    error.contains("timeout") -> {
                        // Retry after delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            Log.d(TAG, "Retrying after timeout...")
                        }, 1000)
                    }

                    else -> {
                        // Handle other errors
                        Log.e(TAG, "Unhandled error: $error")
                    }
                }
            }

            override fun onCommandsDiscovered(commands: List<String>) {
                if (commands.isEmpty()) {
                    Log.w(TAG, "No commands discovered - page may not be fully loaded")
                } else {
                    Log.d(TAG, "Discovered ${commands.size} commands")
                }
            }
        })
    }

    /**
     * Example 7: Command Discovery and Mapping
     *
     * Discover commands from the page and map them to voice commands
     */
    fun commandDiscoveryExample(webView: VOSWebView) {
        webView.setCommandListener(object : VOSWebView.CommandListener {
            override fun onCommandExecuted(command: String, success: Boolean) {
                // Not used in this example
            }

            override fun onCommandError(command: String, error: String) {
                // Not used in this example
            }

            override fun onCommandsDiscovered(commands: List<String>) {
                // Build voice command mapping
                val voiceCommandMap = mutableMapOf<String, String>()

                commands.forEach { buttonText ->
                    // Normalize for voice recognition
                    val voiceCommand = normalizeForVoice(buttonText)

                    // Find XPath for this button
                    val xpath = "//button[contains(text(), '$buttonText')] | " +
                               "//a[contains(text(), '$buttonText')] | " +
                               "//input[@value='$buttonText']"

                    voiceCommandMap[voiceCommand] = xpath

                    Log.d(TAG, "Voice command: '$voiceCommand' -> XPath: $xpath")
                }

                // Store mapping for voice recognition
                // E.g., save to shared preferences or database
                Log.d(TAG, "Built voice command map with ${voiceCommandMap.size} commands")
            }
        })
    }

    // Helper functions

    private fun extractTarget(voiceCommand: String): String {
        // Simple extraction - in production, use NLP
        val words = voiceCommand.split(" ")
        return words.drop(1).joinToString(" ")
    }

    private fun extractFieldAndValue(voiceCommand: String): Pair<String, String> {
        // E.g., "fill username with john doe" -> ("username", "john doe")
        val parts = voiceCommand.split(" with ")
        val field = parts[0].replace("fill ", "").trim()
        val value = parts.getOrElse(1) { "" }.trim()
        return Pair(field, value)
    }

    private fun findElementByText(text: String): String {
        // Generate XPath to find element by text
        return "//*[contains(text(), '$text')]"
    }

    private fun findInputByName(name: String): String {
        // Generate XPath to find input by name
        return "//input[@name='$name']"
    }

    private fun retryCommand(webView: VOSWebView, command: String) {
        // Implement retry logic
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Retrying command: $command")
            // Re-execute command
        }, 1000)
    }

    private fun normalizeForVoice(text: String): String {
        // Normalize button text for voice recognition
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
    }
}

/**
 * Usage in an Activity:
 *
 * class WebViewActivity : AppCompatActivity() {
 *     private lateinit var webView: VOSWebView
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // Create and setup WebView
 *         webView = VOSWebViewSample.basicSetup(this)
 *
 *         // Add to layout
 *         setContentView(webView)
 *
 *         // Load URL
 *         webView.loadUrl("https://example.com")
 *     }
 *
 *     fun onVoiceCommand(command: String) {
 *         // Handle voice command
 *         VOSWebViewSample.voiceCommandIntegration(webView, command)
 *     }
 *
 *     override fun onBackPressed() {
 *         if (webView.canGoBack()) {
 *             webView.goBack()
 *         } else {
 *             super.onBackPressed()
 *         }
 *     }
 *
 *     override fun onDestroy() {
 *         webView.destroy()
 *         super.onDestroy()
 *     }
 * }
 */
