/**
 * URLBarInteractionManager.kt - Multi-method URL bar interaction manager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (Claude Code Assistant)
 * Created: 2025-10-13
 *
 * Multi-method URL bar interaction manager supporting:
 * - Voice command method (text-to-speech "go to [url]")
 * - Accessibility method (direct tree traversal and text injection)
 * - Keyboard method (simulated key events)
 * - Auto-detection method (tries all methods in order)
 *
 * Features:
 * - Browser detection (Chrome, Firefox, Brave, Opera, etc.)
 * - Multiple node finding strategies (resource ID, class name, content description)
 * - Preference-based method selection
 * - Comprehensive error handling and logging
 * - Thread-safe operations
 */
package com.augmentalis.voiceoscore.url

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import java.util.Locale

/**
 * URLBarInteractionManager - Main manager class for URL bar interactions
 *
 * Supports multiple interaction methods:
 * 1. VOICE - Uses text-to-speech to say "go to [url]" (browser must support voice)
 * 2. ACCESSIBILITY - Direct accessibility tree traversal and text injection
 * 3. KEYBOARD - Simulates keyboard input character by character
 * 4. AUTO - Tries methods in order of reliability until one succeeds
 *
 * Usage:
 * ```kotlin
 * val manager = URLBarInteractionManager(context, accessibilityService)
 * manager.setPreferredMethod(InteractionMethod.AUTO)
 * val success = manager.navigateToURL("https://example.com")
 * ```
 */
class URLBarInteractionManager(
    private val context: Context,
    private val accessibilityService: VoiceOSService
) {
    companion object {
        private const val TAG = "URLBarInteractionManager"
        private const val ACTION_DELAY_MS = 100L
        private const val TTS_TIMEOUT_MS = 5000L
    }

    /**
     * Interaction methods for URL bar navigation
     */
    enum class InteractionMethod {
        VOICE,           // Voice command "go to [url]"
        ACCESSIBILITY,   // Accessibility tree traversal
        KEYBOARD,        // Simulate keyboard input
        AUTO             // Auto-detect best method
    }

    /**
     * Supported browser types with specific resource IDs
     */
    enum class BrowserType(val packageName: String, val urlBarId: String?) {
        CHROME("com.android.chrome", "com.android.chrome:id/url_bar"),
        FIREFOX("org.mozilla.firefox", "org.mozilla.firefox:id/url_bar_title"),
        BRAVE("com.brave.browser", "com.brave.browser:id/url_bar"),
        OPERA("com.opera.browser", "com.opera.browser:id/url_field"),
        EDGE("com.microsoft.emmx", "com.microsoft.emmx:id/url_bar"),
        SAMSUNG("com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser:id/location_bar_edit_text"),
        UNKNOWN("", null);

        companion object {
            /**
             * Detect browser type from package name
             */
            fun fromPackageName(packageName: String?): BrowserType {
                if (packageName == null) return UNKNOWN
                return values().find { packageName.contains(it.packageName) } ?: UNKNOWN
            }
        }
    }

    private var preferredMethod: InteractionMethod = InteractionMethod.AUTO
    private val preferences: URLBarPreferences = URLBarPreferences(context)

    @Volatile
    private var ttsInstance: TextToSpeech? = null

    @Volatile
    private var ttsReady = false

    init {
        // Load preferred method from preferences
        preferredMethod = preferences.getPreferredMethod()
        Log.d(TAG, "URLBarInteractionManager initialized with method: $preferredMethod")
    }

    /**
     * Set preferred interaction method
     *
     * @param method The interaction method to use
     */
    fun setPreferredMethod(method: InteractionMethod) {
        preferredMethod = method
        preferences.setPreferredMethod(method)
        Log.i(TAG, "Preferred method set to: $method")
    }

    /**
     * Navigate to URL using preferred method
     *
     * @param url The URL to navigate to (should include http:// or https://)
     * @return true if navigation was successful
     */
    fun navigateToURL(url: String): Boolean {
        Log.i(TAG, "Navigating to URL: $url (method: $preferredMethod)")

        // Normalize URL (add https:// if missing protocol)
        val normalizedUrl = normalizeURL(url)

        // Check for browser-specific preference
        val browser = detectBrowser()
        val browserMethod = preferences.getBrowserSpecificMethod(browser)
        val methodToUse = browserMethod ?: preferredMethod

        Log.d(TAG, "Detected browser: $browser, using method: $methodToUse")

        return when (methodToUse) {
            InteractionMethod.VOICE -> navigateViaVoice(normalizedUrl)
            InteractionMethod.ACCESSIBILITY -> navigateViaAccessibility(normalizedUrl)
            InteractionMethod.KEYBOARD -> navigateViaKeyboard(normalizedUrl)
            InteractionMethod.AUTO -> navigateViaAuto(normalizedUrl)
        }
    }

    /**
     * Focus URL bar without entering text
     *
     * @return true if focus was successful
     */
    fun focusURLBar(): Boolean {
        Log.d(TAG, "Focusing URL bar")

        val urlBarNode = findURLBarNode()
        if (urlBarNode == null) {
            Log.w(TAG, "Could not find URL bar to focus")
            return false
        }

        return try {
            val success = urlBarNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            Log.d(TAG, "URL bar focus result: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error focusing URL bar: Failed to set focus action on navigation node. Cause: ${e.javaClass.simpleName} - ${e.message}. Impact: URL bar interaction unavailable", e)
            false
        } finally {
            urlBarNode.recycle()
        }
    }

    /**
     * Clear URL bar text
     *
     * @return true if clear was successful
     */
    fun clearURLBar(): Boolean {
        Log.d(TAG, "Clearing URL bar")

        val urlBarNode = findURLBarNode()
        if (urlBarNode == null) {
            Log.w(TAG, "Could not find URL bar to clear")
            return false
        }

        return try {
            // Focus first
            urlBarNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            Thread.sleep(ACTION_DELAY_MS)

            // Clear text
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            val success = urlBarNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "URL bar clear result: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing URL bar: Failed to clear text from URL bar node. Cause: ${e.javaClass.simpleName} - ${e.message}. Impact: URL bar text cannot be cleared", e)
            false
        } finally {
            urlBarNode.recycle()
        }
    }

    /**
     * Get URL bar accessibility node (caller must recycle!)
     *
     * @return AccessibilityNodeInfo for URL bar, or null if not found
     */
    fun getURLBarNode(): AccessibilityNodeInfo? {
        return findURLBarNode()
    }

    /**
     * Navigate using voice command (text-to-speech)
     *
     * Works if browser supports voice commands.
     * Says "go to [url]" using text-to-speech.
     */
    private fun navigateViaVoice(url: String): Boolean {
        Log.d(TAG, "Attempting voice navigation to: $url")

        try {
            // Initialize TTS if not ready
            if (ttsInstance == null || !ttsReady) {
                var initSuccess = false
                var tts: TextToSpeech? = null
                tts = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ttsReady = true
                        tts?.setLanguage(Locale.US)
                        initSuccess = true
                        Log.d(TAG, "TTS initialized successfully")
                    } else {
                        Log.e(TAG, "TTS initialization failed with status: $status")
                    }
                }
                ttsInstance = tts

                // Wait for TTS initialization
                val startTime = SystemClock.elapsedRealtime()
                while (!ttsReady && SystemClock.elapsedRealtime() - startTime < TTS_TIMEOUT_MS) {
                    Thread.sleep(100)
                }

                if (!ttsReady) {
                    Log.e(TAG, "TTS initialization timeout")
                    return false
                }
            }

            // Speak the command
            val command = "go to $url"
            val result = ttsInstance?.speak(command, TextToSpeech.QUEUE_FLUSH, null, "URL_NAVIGATION")

            if (result == TextToSpeech.SUCCESS) {
                Log.i(TAG, "Voice command spoken successfully: $command")
                return true
            } else {
                Log.e(TAG, "TTS speak failed with result: $result")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in voice navigation: Failed to navigate to URL using TTS voice command. Cause: ${e.javaClass.simpleName} - ${e.message}. Impact: Voice navigation method unavailable, fallback to other methods", e)
            return false
        }
    }

    /**
     * Navigate using accessibility tree traversal
     *
     * Most reliable method - directly finds and manipulates URL bar node.
     */
    private fun navigateViaAccessibility(url: String): Boolean {
        Log.d(TAG, "Attempting accessibility navigation to: $url")

        val urlBarNode = findURLBarNode()
        if (urlBarNode == null) {
            Log.w(TAG, "Could not find URL bar for accessibility navigation")
            return false
        }

        return try {
            // Focus URL bar
            urlBarNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            Thread.sleep(ACTION_DELAY_MS)

            // Clear existing text
            val clearArgs = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            urlBarNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearArgs)
            Thread.sleep(ACTION_DELAY_MS / 2)

            // Set new URL
            val setArgs = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, url)
            }
            val setText = urlBarNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setArgs)

            if (!setText) {
                Log.w(TAG, "Failed to set URL bar text")
                return false
            }

            Thread.sleep(ACTION_DELAY_MS)

            // Press enter (click action on URL bar submits)
            val clicked = urlBarNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            Log.i(TAG, "Accessibility navigation result: setText=$setText, clicked=$clicked")
            setText && clicked
        } catch (e: Exception) {
            Log.e(TAG, "Error in accessibility navigation: Failed to navigate via accessibility tree traversal. Cause: ${e.javaClass.simpleName} - ${e.message}. Impact: Accessibility navigation method failed, trying fallback methods", e)
            false
        } finally {
            urlBarNode.recycle()
        }
    }

    /**
     * Navigate using text injection (ACTION_SET_TEXT or clipboard)
     *
     * Uses AccessibilityService-compatible text injection methods:
     * 1. ACTION_SET_TEXT - Direct text injection (Android 5.0+)
     * 2. Clipboard + ACTION_PASTE - Fallback method
     *
     * Note: The old keyboard character-by-character method was replaced because
     * AccessibilityService doesn't support dispatchKeyEvent(). These new methods
     * are more reliable and work across all Android versions.
     */
    private fun navigateViaKeyboard(url: String): Boolean {
        Log.d(TAG, "Attempting text injection navigation to: $url")

        val urlBarNode = findURLBarNode()
        if (urlBarNode == null) {
            Log.w(TAG, "Could not find URL bar for text injection")
            return false
        }

        return try {
            // Focus URL bar
            val focused = urlBarNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            if (!focused) {
                Log.w(TAG, "Failed to focus URL bar")
            }
            Thread.sleep(ACTION_DELAY_MS)

            // Try Method 1: Direct text injection (Android 5.0+)
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, url)
            val textSet = urlBarNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            if (textSet) {
                Log.d(TAG, "✓ URL set via ACTION_SET_TEXT")
                Thread.sleep(ACTION_DELAY_MS)

                // Trigger navigation by clicking the node (URL bar will navigate on click)
                val clicked = urlBarNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked) {
                    Log.d(TAG, "Click failed, URL may still navigate on focus change")
                }

                Log.i(TAG, "Text injection navigation completed successfully")
                true
            } else {
                // Fallback Method 2: Clipboard + Paste
                Log.d(TAG, "ACTION_SET_TEXT failed, trying clipboard method")
                setTextViaClipboard(urlBarNode, url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in text injection navigation: Failed to inject text into URL bar using ACTION_SET_TEXT. Cause: ${e.javaClass.simpleName} - ${e.message}. Impact: Text injection navigation failed, clipboard method may be tried", e)
            false
        } finally {
            urlBarNode.recycle()
        }
    }

    /**
     * Set text via clipboard and paste action (fallback method)
     */
    private fun setTextViaClipboard(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            // Copy text to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("url", text)
            clipboard.setPrimaryClip(clip)

            Log.d(TAG, "Text copied to clipboard: $text")

            // Select all existing text (select from start to end)
            val selectArgs = Bundle()
            selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, node.text?.length ?: 0)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)
            Thread.sleep(100)

            // Paste from clipboard
            val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (pasted) {
                Log.d(TAG, "✓ URL pasted from clipboard")
                Thread.sleep(ACTION_DELAY_MS)

                // Trigger navigation by clicking
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                Log.i(TAG, "Clipboard navigation completed successfully")
                true
            } else {
                Log.w(TAG, "Failed to paste from clipboard")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in clipboard method: Failed to inject URL via clipboard paste operation. Cause: ${e.javaClass.simpleName} - ${e.message}. Impact: Clipboard fallback navigation failed", e)
            false
        }
    }

    /**
     * Auto-detect best navigation method
     *
     * Tries methods in order of reliability:
     * 1. Accessibility (most reliable)
     * 2. Keyboard (fallback)
     * 3. Voice (least reliable, requires voice support)
     */
    private fun navigateViaAuto(url: String): Boolean {
        Log.d(TAG, "Attempting auto-detection navigation to: $url")

        val methods = listOf(
            InteractionMethod.ACCESSIBILITY to { navigateViaAccessibility(url) },
            InteractionMethod.KEYBOARD to { navigateViaKeyboard(url) },
            InteractionMethod.VOICE to { navigateViaVoice(url) }
        )

        for ((method, navFunction) in methods) {
            try {
                Log.d(TAG, "Auto-detection trying method: $method")
                if (navFunction()) {
                    Log.i(TAG, "Successfully navigated using auto-detected method: $method")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Method $method failed, trying next", e)
            }
        }

        Log.e(TAG, "All auto-detection methods failed")
        return false
    }

    /**
     * Find URL bar node using multiple strategies
     *
     * Strategies (in order):
     * 1. Find by browser-specific resource ID
     * 2. Find by class name (EditText with URL-like content)
     * 3. Find by content description (common URL bar descriptions)
     * 4. Find by hint text (common URL bar hints)
     *
     * @return AccessibilityNodeInfo for URL bar, or null if not found (caller must recycle!)
     */
    private fun findURLBarNode(): AccessibilityNodeInfo? {
        val rootNode = accessibilityService.rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "No active window to search for URL bar")
            return null
        }

        try {
            // Strategy 1: Find by browser-specific resource ID
            val browser = detectBrowser()
            browser.urlBarId?.let { urlBarId ->
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(urlBarId)
                if (nodes.isNotEmpty()) {
                    Log.d(TAG, "Found URL bar by resource ID: $urlBarId")
                    // Recycle all except first
                    for (i in 1 until nodes.size) {
                        nodes[i].recycle()
                    }
                    return nodes[0]
                }
            }

            // Strategy 2: Find by class name (EditText with URL-like content)
            val editTextNodes = rootNode.findAccessibilityNodeInfosByViewId("android.widget.EditText")
            for (node in editTextNodes) {
                val text = node.text?.toString() ?: ""
                val hint = node.hintText?.toString() ?: ""

                if (isURLBarLike(text, hint)) {
                    Log.d(TAG, "Found URL bar by EditText analysis (text='$text', hint='$hint')")
                    // Recycle others
                    editTextNodes.filter { it != node }.forEach { it.recycle() }
                    return node
                }
            }

            // Recycle all EditText nodes if none matched
            editTextNodes.forEach { it.recycle() }

            // Strategy 3: Find by content description
            val urlBarDescriptions = listOf(
                "Address and search bar",
                "URL bar",
                "Search or type web address",
                "Address bar",
                "Search or type URL",
                "Website address"
            )

            for (description in urlBarDescriptions) {
                val nodes = findNodesByContentDescription(rootNode, description)
                if (nodes.isNotEmpty()) {
                    Log.d(TAG, "Found URL bar by content description: $description")
                    // Recycle all except first
                    for (i in 1 until nodes.size) {
                        nodes[i].recycle()
                    }
                    return nodes[0]
                }
            }

            // Strategy 4: Find by hint text patterns
            val result = findNodeByHintTextPattern(rootNode)
            if (result != null) {
                Log.d(TAG, "Found URL bar by hint text pattern")
                return result
            }

            Log.w(TAG, "Could not find URL bar using any strategy")
            return null
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Check if text/hint indicates URL bar
     */
    private fun isURLBarLike(text: String, hint: String): Boolean {
        val combined = "$text $hint".lowercase()
        return combined.startsWith("http") ||
               combined.startsWith("https") ||
               combined.contains(".com") ||
               combined.contains(".org") ||
               combined.contains(".net") ||
               combined.contains("search") ||
               combined.contains("address") ||
               combined.contains("url")
    }

    /**
     * Find nodes by content description (case-insensitive)
     */
    private fun findNodesByContentDescription(
        root: AccessibilityNodeInfo,
        description: String
    ): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        findNodesByContentDescriptionRecursive(root, description.lowercase(), results)
        return results
    }

    /**
     * Recursive helper for content description search
     */
    private fun findNodesByContentDescriptionRecursive(
        node: AccessibilityNodeInfo,
        description: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        val nodeDesc = node.contentDescription?.toString()?.lowercase()
        if (nodeDesc != null && nodeDesc.contains(description)) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByContentDescriptionRecursive(child, description, results)
            child.recycle()
        }
    }

    /**
     * Find node by hint text pattern matching
     */
    private fun findNodeByHintTextPattern(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val hintPatterns = listOf(
            "search or type",
            "enter url",
            "type url",
            "web address"
        )

        return findNodeByHintTextPatternRecursive(root, hintPatterns)
    }

    /**
     * Recursive helper for hint text search
     */
    private fun findNodeByHintTextPatternRecursive(
        node: AccessibilityNodeInfo,
        patterns: List<String>
    ): AccessibilityNodeInfo? {
        val hint = node.hintText?.toString()?.lowercase()
        if (hint != null) {
            for (pattern in patterns) {
                if (hint.contains(pattern)) {
                    return node
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByHintTextPatternRecursive(child, patterns)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }

        return null
    }

    /**
     * Detect current browser type
     */
    private fun detectBrowser(): BrowserType {
        val packageName = accessibilityService.rootInActiveWindow?.packageName?.toString()
        val browser = BrowserType.fromPackageName(packageName)
        Log.d(TAG, "Detected browser: $browser (package: $packageName)")
        return browser
    }

    /**
     * Inject key event (down and up)
     *
     * TODO: AccessibilityService does not have direct dispatchKeyEvent() support.
     * This requires either:
     * 1. Root permissions with input command
     * 2. Instrumentation (requires same process)
     * 3. Alternative approach using ACTION_SET_TEXT or clipboard
     *
     * For now, this method is stubbed out. The keyboard method will fail,
     * but other methods (VOICE, ACCESSIBILITY) should still work.
     */
    private fun injectKeyEvent(keyCode: Int, metaState: Int = 0) {
        Log.w(TAG, "injectKeyEvent() not supported in AccessibilityService context")
        Log.w(TAG, "  KeyCode: $keyCode, MetaState: $metaState")
        Log.w(TAG, "  Consider using VOICE or ACCESSIBILITY methods instead")

        // AccessibilityService doesn't support dispatchKeyEvent()
        // The keyboard method will not work without this functionality
    }

    /**
     * Get key code for character
     *
     * Maps common URL characters to key codes.
     * Does not support all special characters.
     */
    private fun getKeyCodeForChar(char: Char): Int? {
        return when (char.lowercaseChar()) {
            'a' -> KeyEvent.KEYCODE_A
            'b' -> KeyEvent.KEYCODE_B
            'c' -> KeyEvent.KEYCODE_C
            'd' -> KeyEvent.KEYCODE_D
            'e' -> KeyEvent.KEYCODE_E
            'f' -> KeyEvent.KEYCODE_F
            'g' -> KeyEvent.KEYCODE_G
            'h' -> KeyEvent.KEYCODE_H
            'i' -> KeyEvent.KEYCODE_I
            'j' -> KeyEvent.KEYCODE_J
            'k' -> KeyEvent.KEYCODE_K
            'l' -> KeyEvent.KEYCODE_L
            'm' -> KeyEvent.KEYCODE_M
            'n' -> KeyEvent.KEYCODE_N
            'o' -> KeyEvent.KEYCODE_O
            'p' -> KeyEvent.KEYCODE_P
            'q' -> KeyEvent.KEYCODE_Q
            'r' -> KeyEvent.KEYCODE_R
            's' -> KeyEvent.KEYCODE_S
            't' -> KeyEvent.KEYCODE_T
            'u' -> KeyEvent.KEYCODE_U
            'v' -> KeyEvent.KEYCODE_V
            'w' -> KeyEvent.KEYCODE_W
            'x' -> KeyEvent.KEYCODE_X
            'y' -> KeyEvent.KEYCODE_Y
            'z' -> KeyEvent.KEYCODE_Z
            '0' -> KeyEvent.KEYCODE_0
            '1' -> KeyEvent.KEYCODE_1
            '2' -> KeyEvent.KEYCODE_2
            '3' -> KeyEvent.KEYCODE_3
            '4' -> KeyEvent.KEYCODE_4
            '5' -> KeyEvent.KEYCODE_5
            '6' -> KeyEvent.KEYCODE_6
            '7' -> KeyEvent.KEYCODE_7
            '8' -> KeyEvent.KEYCODE_8
            '9' -> KeyEvent.KEYCODE_9
            '.' -> KeyEvent.KEYCODE_PERIOD
            '/' -> KeyEvent.KEYCODE_SLASH
            ':' -> KeyEvent.KEYCODE_SEMICOLON // Shift+; = :
            '-' -> KeyEvent.KEYCODE_MINUS
            '_' -> KeyEvent.KEYCODE_MINUS // Shift+- = _
            '=' -> KeyEvent.KEYCODE_EQUALS
            '?' -> KeyEvent.KEYCODE_SLASH // Shift+/ = ?
            ' ' -> KeyEvent.KEYCODE_SPACE
            else -> null
        }
    }

    /**
     * Normalize URL (add https:// if missing)
     */
    private fun normalizeURL(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("www.") -> "https://$url"
            url.contains(".") -> "https://$url"
            else -> "https://www.google.com/search?q=$url" // Fallback to search
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        ttsInstance?.let { tts ->
            try {
                tts.stop()
                tts.shutdown()
                Log.d(TAG, "TTS instance cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up TTS: Failed to shutdown TextToSpeech instance during cleanup. Cause: ${e.javaClass.simpleName} - ${e.message}. Impact: Resource leak possible if TTS not properly released", e)
            }
        }
        ttsInstance = null
        ttsReady = false
    }
}

/**
 * URLBarPreferences - Manages user preferences for URL bar interaction
 *
 * Stores:
 * - Global preferred method
 * - Browser-specific methods
 * - Usage statistics (future)
 */
class URLBarPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "url_bar_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_PREFERRED_METHOD = "preferred_method"
        private const val KEY_BROWSER_PREFIX = "browser_"
    }

    /**
     * Get global preferred interaction method
     */
    fun getPreferredMethod(): URLBarInteractionManager.InteractionMethod {
        val methodName = prefs.getString(KEY_PREFERRED_METHOD, "AUTO") ?: "AUTO"
        return try {
            URLBarInteractionManager.InteractionMethod.valueOf(methodName)
        } catch (e: IllegalArgumentException) {
            Log.w("URLBarPreferences", "Invalid method name: $methodName, using AUTO")
            URLBarInteractionManager.InteractionMethod.AUTO
        }
    }

    /**
     * Set global preferred interaction method
     */
    fun setPreferredMethod(method: URLBarInteractionManager.InteractionMethod) {
        prefs.edit().putString(KEY_PREFERRED_METHOD, method.name).apply()
    }

    /**
     * Get browser-specific interaction method
     *
     * @param browser The browser type
     * @return The preferred method for this browser, or null if not set
     */
    fun getBrowserSpecificMethod(
        browser: URLBarInteractionManager.BrowserType
    ): URLBarInteractionManager.InteractionMethod? {
        if (browser == URLBarInteractionManager.BrowserType.UNKNOWN) return null

        val key = "$KEY_BROWSER_PREFIX${browser.name.lowercase()}_method"
        val methodName = prefs.getString(key, null) ?: return null

        return try {
            URLBarInteractionManager.InteractionMethod.valueOf(methodName)
        } catch (e: IllegalArgumentException) {
            Log.w("URLBarPreferences", "Invalid browser method: $methodName")
            null
        }
    }

    /**
     * Set browser-specific interaction method
     *
     * @param browser The browser type
     * @param method The preferred method for this browser
     */
    fun setBrowserSpecificMethod(
        browser: URLBarInteractionManager.BrowserType,
        method: URLBarInteractionManager.InteractionMethod
    ) {
        if (browser == URLBarInteractionManager.BrowserType.UNKNOWN) return

        val key = "$KEY_BROWSER_PREFIX${browser.name.lowercase()}_method"
        prefs.edit().putString(key, method.name).apply()
    }

    /**
     * Clear browser-specific method (use global default)
     *
     * @param browser The browser type
     */
    fun clearBrowserSpecificMethod(browser: URLBarInteractionManager.BrowserType) {
        if (browser == URLBarInteractionManager.BrowserType.UNKNOWN) return

        val key = "$KEY_BROWSER_PREFIX${browser.name.lowercase()}_method"
        prefs.edit().remove(key).apply()
    }

    /**
     * Get all browser-specific methods
     *
     * @return Map of browser to method
     */
    fun getAllBrowserMethods(): Map<URLBarInteractionManager.BrowserType, URLBarInteractionManager.InteractionMethod> {
        val result = mutableMapOf<URLBarInteractionManager.BrowserType, URLBarInteractionManager.InteractionMethod>()

        for (browser in URLBarInteractionManager.BrowserType.values()) {
            if (browser == URLBarInteractionManager.BrowserType.UNKNOWN) continue

            getBrowserSpecificMethod(browser)?.let { method ->
                result[browser] = method
            }
        }

        return result
    }
}

/*
 * SAMPLE USAGE:
 *
 * // Initialize manager
 * val urlManager = URLBarInteractionManager(context, voiceOSService)
 *
 * // Use default method (AUTO)
 * urlManager.navigateToURL("https://example.com")
 *
 * // Set specific method
 * urlManager.setPreferredMethod(URLBarInteractionManager.InteractionMethod.ACCESSIBILITY)
 * urlManager.navigateToURL("https://google.com")
 *
 * // Set browser-specific method
 * val prefs = URLBarPreferences(context)
 * prefs.setBrowserSpecificMethod(
 *     URLBarInteractionManager.BrowserType.CHROME,
 *     URLBarInteractionManager.InteractionMethod.KEYBOARD
 * )
 *
 * // Focus URL bar without navigation
 * urlManager.focusURLBar()
 *
 * // Clear URL bar
 * urlManager.clearURLBar()
 *
 * // Get URL bar node for custom operations
 * val urlBarNode = urlManager.getURLBarNode()
 * urlBarNode?.let { node ->
 *     // Do custom operations...
 *     node.recycle() // Always recycle!
 * }
 *
 * // Cleanup when done
 * urlManager.cleanup()
 *
 * INTEGRATION WITH VOICE COMMANDS:
 *
 * // In VoiceOSService handleVoiceCommand():
 * val urlManager = URLBarInteractionManager(this, this)
 *
 * when {
 *     command.startsWith("go to ") -> {
 *         val url = command.removePrefix("go to ").trim()
 *         urlManager.navigateToURL(url)
 *     }
 *     command == "focus url bar" -> {
 *         urlManager.focusURLBar()
 *     }
 *     command == "clear url bar" -> {
 *         urlManager.clearURLBar()
 *     }
 * }
 *
 * TESTING DIFFERENT METHODS:
 *
 * fun testAllMethods(url: String) {
 *     val methods = listOf(
 *         URLBarInteractionManager.InteractionMethod.ACCESSIBILITY,
 *         URLBarInteractionManager.InteractionMethod.KEYBOARD,
 *         URLBarInteractionManager.InteractionMethod.VOICE
 *     )
 *
 *     for (method in methods) {
 *         urlManager.setPreferredMethod(method)
 *         val success = urlManager.navigateToURL(url)
 *         Log.d(TAG, "Method $method: ${if (success) "SUCCESS" else "FAILED"}")
 *         Thread.sleep(5000) // Wait between tests
 *     }
 * }
 */
