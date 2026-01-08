/**
 * WebCommandCoordinator.kt - Web command execution coordinator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 */
package com.augmentalis.voiceoscore.web

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnweb.ScrapedWebElement
import com.augmentalis.voiceoscore.learnweb.GeneratedWebCommand
import com.augmentalis.voiceoscore.learnweb.toGeneratedWebCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt
import org.json.JSONObject

/**
 * Helper function to parse bounds JSON and extract coordinates
 */
private fun ScrapedWebElement.getBoundsX(): Int {
    return try {
        val boundsJson = JSONObject(this.bounds)
        boundsJson.optInt("x", 0)
    } catch (e: Exception) {
        0
    }
}

private fun ScrapedWebElement.getBoundsY(): Int {
    return try {
        val boundsJson = JSONObject(this.bounds)
        boundsJson.optInt("y", 0)
    } catch (e: Exception) {
        0
    }
}

/**
 * Coordinates web command execution
 *
 * Integrates LearnWeb database with voice commands to enable
 * voice control of web content in browsers.
 *
 * Flow:
 * 1. Detect if current app is a browser
 * 2. Get current URL from address bar
 * 3. Query WebScrapingDatabase for matching command
 * 4. Find web element by XPath/selector
 * 5. Execute action via accessibility
 */
class WebCommandCoordinator(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {

    companion object {
        private const val TAG = "WebCommandCoordinator"

        /**
         * Known browser package names
         * Add more as needed
         */
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",                // Chrome
            "org.mozilla.firefox",               // Firefox
            "com.brave.browser",                 // Brave
            "com.opera.browser",                 // Opera
            "com.microsoft.emmx",                // Edge
            "com.sec.android.app.sbrowser",      // Samsung Internet
            "com.duckduckgo.mobile.android",     // DuckDuckGo
            "org.chromium.webview_shell",        // WebView test shell
            "com.kiwibrowser.browser"            // Kiwi Browser
        )

        /**
         * Address bar resource IDs for URL extraction
         * Map: packageName → url_bar resource ID
         */
        private val URL_BAR_IDS = mapOf(
            "com.android.chrome" to "com.android.chrome:id/url_bar",
            "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "com.brave.browser" to "com.brave.browser:id/url_bar",
            "com.opera.browser" to "com.opera.browser:id/url_field",
            "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
            "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text"
        )
    }

    private val databaseManager: VoiceOSDatabaseManager = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context))

    /**
     * Check if current app is a browser
     */
    fun isCurrentAppBrowser(packageName: String?): Boolean {
        val isBrowser = packageName in BROWSER_PACKAGES
        if (isBrowser) {
            Log.d(TAG, "Browser detected: $packageName")
        }
        return isBrowser
    }

    /**
     * Process web command
     *
     * @param command Voice command text
     * @param currentPackage Current app package name
     * @return true if command was handled, false otherwise
     */
    suspend fun processWebCommand(
        command: String,
        currentPackage: String
    ): Boolean = withContext(Dispatchers.IO) {

        if (!isCurrentAppBrowser(currentPackage)) {
            Log.d(TAG, "Not a browser, skipping web command processing")
            return@withContext false
        }

        try {
            Log.i(TAG, "Processing web command: '$command' in $currentPackage")

            // Get current URL from browser address bar
            val url = getCurrentURL(currentPackage) ?: run {
                Log.w(TAG, "Could not extract current URL from browser")
                return@withContext false
            }

            Log.d(TAG, "Current URL: $url")

            // Find matching command in database
            val webCommand = findMatchingWebCommand(command, url)
            if (webCommand == null) {
                Log.d(TAG, "No matching web command found for: '$command' on URL: $url")
                return@withContext false
            }

            Log.i(TAG, "Matched web command: ${webCommand.commandText} → ${webCommand.action}")

            // Get associated web element by hash
            val sqlDelightElement = databaseManager.scrapedWebElementQueries.getByElementHash(webCommand.elementHash).executeAsOneOrNull()
            if (sqlDelightElement == null) {
                Log.w(TAG, "Web element not found (hash): ${webCommand.elementHash}")
                Log.w(TAG, "  Element may no longer exist or page structure changed")
                return@withContext false
            }

            // Convert SQLDelight element to data class
            // Note: SQLDelight uses Long for boolean columns (0/1)
            val element = ScrapedWebElement(
                id = sqlDelightElement.id,
                websiteUrlHash = sqlDelightElement.website_url_hash,
                elementHash = sqlDelightElement.element_hash,
                tagName = sqlDelightElement.tag_name,
                xpath = sqlDelightElement.xpath,
                text = sqlDelightElement.text,
                ariaLabel = sqlDelightElement.aria_label,
                role = sqlDelightElement.role,
                parentElementHash = sqlDelightElement.parent_element_hash,
                clickable = sqlDelightElement.clickable == 1L,
                visible = sqlDelightElement.visible == 1L,
                bounds = sqlDelightElement.bounds
            )

            // Execute web action
            val success = executeWebAction(element, webCommand.action)

            if (success) {
                // Update usage statistics
                // SQLDelight incrementUsage takes: last_used_at, id (positional parameters)
                databaseManager.generatedWebCommandQueries.incrementUsage(
                    last_used_at = System.currentTimeMillis(),
                    id = webCommand.id
                )
                Log.i(TAG, "✓ Web command executed successfully: ${webCommand.commandText}")
            } else {
                Log.w(TAG, "✗ Web command execution failed: ${webCommand.commandText}")
            }

            return@withContext success

        } catch (e: Exception) {
            Log.e(TAG, "Error processing web command", e)
            return@withContext false
        }
    }

    /**
     * Get current URL from browser address bar
     *
     * Uses accessibility to extract URL text from address bar
     */
    private suspend fun getCurrentURL(packageName: String): String? = withContext(Dispatchers.Main) {
        try {
            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "Root node unavailable")
                return@withContext null
            }

            // Try to find URL bar by resource ID
            val urlBarId = URL_BAR_IDS[packageName]
            var urlNode: AccessibilityNodeInfo? = null

            if (urlBarId != null) {
                urlNode = findNodeByResourceId(rootNode, urlBarId)
            }

            // If not found by ID, try heuristic search
            if (urlNode == null) {
                urlNode = findUrlBarHeuristic(rootNode)
            }

            val url = urlNode?.text?.toString()

            // Cleanup
            urlNode?.recycle()
            rootNode.recycle()

            // Normalize URL
            return@withContext normalizeUrl(url)

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting URL from browser", e)
            return@withContext null
        }
    }

    /**
     * Find node by resource ID
     */
    private fun findNodeByResourceId(root: AccessibilityNodeInfo, resourceId: String): AccessibilityNodeInfo? {
        try {
            val nodes = root.findAccessibilityNodeInfosByViewId(resourceId)
            return if (nodes.isNotEmpty()) {
                nodes[0] // Return first match
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding node by resource ID", e)
            return null
        }
    }

    /**
     * Find URL bar using heuristic search
     *
     * Looks for EditText nodes containing URL-like text (http://, https://, www., .com)
     */
    private fun findUrlBarHeuristic(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        try {
            // Check if current node is URL bar
            if (node.className?.toString()?.contains("EditText") == true) {
                val text = node.text?.toString() ?: ""
                if (isUrlLike(text)) {
                    return node
                }
            }

            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findUrlBarHeuristic(child)
                if (found != null) {
                    child.recycle()
                    return found
                }
                child.recycle()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in heuristic URL bar search", e)
        }

        return null
    }

    /**
     * Check if text looks like a URL
     */
    private fun isUrlLike(text: String): Boolean {
        return text.startsWith("http://") ||
               text.startsWith("https://") ||
               text.startsWith("www.") ||
               text.contains(".com") ||
               text.contains(".org") ||
               text.contains(".net")
    }

    /**
     * Normalize URL for database lookup
     *
     * Removes http/https, www, trailing slashes, query params
     */
    private fun normalizeUrl(url: String?): String? {
        if (url == null) return null

        var normalized = url.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")

        // Remove query params and fragments
        normalized = normalized.split("?")[0].split("#")[0]

        // Remove trailing slash
        normalized = normalized.trimEnd('/')

        return if (normalized.isBlank()) null else normalized
    }

    /**
     * Find matching web command in database
     *
     * Tries exact match first, then fuzzy matching
     */
    private suspend fun findMatchingWebCommand(
        commandText: String,
        url: String
    ): GeneratedWebCommand? {
        val normalizedCommand = commandText.lowercase().trim()
        val normalizedUrl = normalizeUrl(url) ?: return null

        // Get all commands for this URL
        val commands = databaseManager.generatedWebCommandQueries.getCommandsForUrl(normalizedUrl, normalizedUrl)
            .executeAsList()
            .map { it.toGeneratedWebCommand() }

        Log.d(TAG, "Found ${commands.size} commands for URL: $normalizedUrl")

        if (commands.isEmpty()) {
            // No commands learned for this URL
            return null
        }

        // Try exact match
        for (cmd in commands) {
            if (cmd.commandText.equals(normalizedCommand, ignoreCase = true)) {
                return cmd
            }
        }

        // Try fuzzy match (contains)
        for (cmd in commands) {
            if (normalizedCommand.contains(cmd.commandText, ignoreCase = true) ||
                cmd.commandText.contains(normalizedCommand, ignoreCase = true)) {
                return cmd
            }
        }

        return null
    }

    /**
     * Execute web action via accessibility
     *
     * Finds element by XPath/selector and performs action
     */
    private suspend fun executeWebAction(
        element: ScrapedWebElement,
        actionType: String
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Executing web action: $actionType on element: ${element.xpath}")

            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                Log.e(TAG, "Root node unavailable for web action execution")
                return@withContext false
            }

            // Find target element
            // Note: XPath not directly supported by accessibility, so we use heuristics
            val targetNode = findWebElementBySelector(rootNode, element)
            if (targetNode == null) {
                Log.e(TAG, "Target web element not found: ${element.xpath}")
                rootNode.recycle()
                return@withContext false
            }

            Log.d(TAG, "Found target web element: ${targetNode.className}")

            // Execute action
            val success = when (actionType.lowercase()) {
                "click" -> {
                    Log.d(TAG, "Performing click on web element")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                "long_click" -> {
                    Log.d(TAG, "Performing long click on web element")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                }
                "focus" -> {
                    Log.d(TAG, "Performing focus on web element")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                }
                "scroll_to" -> {
                    Log.d(TAG, "Scrolling to web element")
                    scrollToElement(targetNode)
                }
                else -> {
                    Log.w(TAG, "Unknown web action type: $actionType")
                    false
                }
            }

            // Cleanup
            targetNode.recycle()
            rootNode.recycle()

            return@withContext success

        } catch (e: Exception) {
            Log.e(TAG, "Error executing web action", e)
            return@withContext false
        }
    }

    /**
     * Find web element by selector
     *
     * Uses heuristic matching based on:
     * - Element text content
     * - Element class name
     * - Element bounds (approximate position)
     */
    private fun findWebElementBySelector(
        root: AccessibilityNodeInfo,
        element: ScrapedWebElement
    ): AccessibilityNodeInfo? {
        try {
            // Strategy 1: Match by text content
            element.text?.takeIf { it.isNotBlank() }?.let { searchText ->
                val nodes = root.findAccessibilityNodeInfosByText(searchText)
                if (nodes.isNotEmpty()) {
                    // Find best match by position if multiple
                    return findClosestByBounds(nodes, element.getBoundsX(), element.getBoundsY())
                }
            }

            // Strategy 2: Traverse tree and match by attributes
            return findWebElementRecursive(root, element)

        } catch (e: Exception) {
            Log.e(TAG, "Error finding web element by selector", e)
            return null
        }
    }

    /**
     * Recursive search for web element matching attributes
     */
    private fun findWebElementRecursive(
        node: AccessibilityNodeInfo,
        target: ScrapedWebElement
    ): AccessibilityNodeInfo? {
        try {
            // Check if current node matches
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""

            val textMatch = target.text?.takeIf { it.isNotBlank() }?.let { targetText ->
                text.contains(targetText, ignoreCase = true) ||
                contentDesc.contains(targetText, ignoreCase = true)
            } ?: false

            val tagMatch = target.tagName?.let { node.className?.toString()?.contains(it, ignoreCase = true) == true } ?: true

            if (textMatch && tagMatch) {
                // Additional verification: check bounds proximity
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                val distance = sqrt(
                    (bounds.centerX() - target.getBoundsX()).toDouble().pow(2.0) +
                    (bounds.centerY() - target.getBoundsY()).toDouble().pow(2.0)
                )

                // If position is reasonably close (within 200 pixels), consider it a match
                if (distance < 200) {
                    return node
                }
            }

            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findWebElementRecursive(child, target)
                if (found != null) {
                    child.recycle()
                    return found
                }
                child.recycle()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in recursive web element search", e)
        }

        return null
    }

    /**
     * Find closest node to target position from list of candidates
     */
    private fun findClosestByBounds(
        nodes: List<AccessibilityNodeInfo>,
        targetX: Int,
        targetY: Int
    ): AccessibilityNodeInfo? {
        var closestNode: AccessibilityNodeInfo? = null
        var minDistance = Double.MAX_VALUE

        for (node in nodes) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val distance = sqrt(
                (bounds.centerX() - targetX).toDouble().pow(2.0) +
                (bounds.centerY() - targetY).toDouble().pow(2.0)
            )

            if (distance < minDistance) {
                minDistance = distance
                closestNode = node
            }
        }

        return closestNode
    }

    /**
     * Scroll to make element visible
     */
    private fun scrollToElement(node: AccessibilityNodeInfo): Boolean {
        try {
            // Get element bounds
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // Find scrollable parent
            var parent: AccessibilityNodeInfo? = node.parent
            while (parent != null) {
                if (parent.isScrollable) {
                    // Scroll forward until element is visible
                    // This is a simple implementation - could be improved
                    parent.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    parent.recycle()
                    return true
                }
                val nextParent = parent.parent
                parent.recycle()
                parent = nextParent
            }

            Log.w(TAG, "No scrollable parent found for element")
            return false

        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling to element", e)
            return false
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "WebCommandCoordinator cleaned up")
        // No resources to cleanup currently
    }
}
