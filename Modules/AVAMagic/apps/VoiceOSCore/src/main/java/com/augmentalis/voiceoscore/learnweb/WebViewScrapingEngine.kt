/**
 * WebViewScrapingEngine.kt - WebView DOM scraping with JavaScript injection
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/WebViewScrapingEngine.kt
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Extracts DOM structure from WebView using JavaScript injection
 * Generates voice commands for interactive web elements
 */

package com.augmentalis.voiceoscore.learnweb

import android.content.Context
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebView Scraping Engine
 *
 * Extracts DOM structure from WebView using JavaScript injection.
 * Supports interactive element detection and command generation.
 *
 * Features:
 * - JavaScript DOM traversal
 * - XPath generation
 * - ARIA label extraction
 * - Element visibility detection
 * - Text content extraction
 *
 * @property context Application context
 *
 * @since 1.0.0
 */
class WebViewScrapingEngine(private val context: Context) {

    companion object {
        private const val TAG = "WebViewScrapingEngine"

        /**
         * Interactive element tags for command generation
         */
        private val INTERACTIVE_TAGS = setOf(
            "BUTTON", "A", "INPUT", "SELECT", "TEXTAREA",
            "LABEL", "SUMMARY", "DETAILS", "VIDEO", "AUDIO"
        )

        /**
         * Interactive ARIA roles for command generation
         */
        private val INTERACTIVE_ROLES = setOf(
            "button", "link", "tab", "menuitem", "menuitemcheckbox",
            "menuitemradio", "option", "radio", "checkbox", "switch",
            "textbox", "searchbox", "combobox", "slider", "spinbutton"
        )

        /**
         * Maximum text length for element extraction
         */
        private const val MAX_TEXT_LENGTH = 100

        /**
         * XPath special characters that must be escaped to prevent injection
         */
        private val XPATH_ESCAPE_CHARS = mapOf(
            "'" to "\\'",
            "\"" to "\\\"",
            "\\" to "\\\\",
            "\n" to "\\n",
            "\r" to "\\r",
            "\t" to "\\t"
        )
    }

    /**
     * Escape XPath string for safe JavaScript injection
     *
     * SECURITY: Prevents XSS attacks by escaping special characters
     * in XPath expressions before JavaScript evaluation.
     *
     * @param xpath Raw XPath string
     * @return Escaped XPath safe for JS template literals
     */
    private fun escapeXPath(xpath: String): String {
        var escaped = xpath
        XPATH_ESCAPE_CHARS.forEach { (char, replacement) ->
            escaped = escaped.replace(char, replacement)
        }
        // Additional safety: reject XPaths with potential JS injection patterns
        if (escaped.contains("javascript:", ignoreCase = true) ||
            escaped.contains("</script>", ignoreCase = true) ||
            escaped.contains("onerror=", ignoreCase = true) ||
            escaped.contains("onload=", ignoreCase = true)) {
            Log.w(TAG, "Potentially malicious XPath rejected: $xpath")
            return ""
        }
        return escaped
    }

    /**
     * Extract DOM structure from WebView
     *
     * Injects JavaScript to traverse DOM and extract interactive elements.
     *
     * @param webView Target WebView
     * @return List of scraped web elements
     */
    suspend fun extractDOMStructure(webView: WebView): List<ScrapedWebElement> {
        val domJson = injectJavaScript(webView, DOM_EXTRACTION_SCRIPT)
        return parseDOMElements(domJson)
    }

    /**
     * Get page title from WebView
     *
     * @param webView Target WebView
     * @return Page title or empty string
     */
    suspend fun getPageTitle(webView: WebView): String {
        return try {
            val titleJson = injectJavaScript(webView, "document.title")
            titleJson.trim('"')
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get page title", e)
            ""
        }
    }

    /**
     * Click element by XPath
     *
     * SECURITY: XPath is escaped to prevent XSS injection
     *
     * @param webView Target WebView
     * @param xpath XPath selector
     * @return true if click succeeded
     */
    suspend fun clickElement(webView: WebView, xpath: String): Boolean {
        val safeXPath = escapeXPath(xpath)
        if (safeXPath.isEmpty()) {
            Log.w(TAG, "Empty or malicious XPath rejected")
            return false
        }

        return try {
            val script = """
                (function() {
                    var element = document.evaluate(
                        '$safeXPath',
                        document,
                        null,
                        XPathResult.FIRST_ORDERED_NODE_TYPE,
                        null
                    ).singleNodeValue;

                    if (element) {
                        element.click();
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()

            val result = injectJavaScript(webView, script)
            result.trim() == "true"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to click element", e)
            false
        }
    }

    /**
     * Scroll to element by XPath
     *
     * SECURITY: XPath is escaped to prevent XSS injection
     *
     * @param webView Target WebView
     * @param xpath XPath selector
     * @return true if scroll succeeded
     */
    suspend fun scrollToElement(webView: WebView, xpath: String): Boolean {
        val safeXPath = escapeXPath(xpath)
        if (safeXPath.isEmpty()) {
            Log.w(TAG, "Empty or malicious XPath rejected")
            return false
        }

        return try {
            val script = """
                (function() {
                    var element = document.evaluate(
                        '$safeXPath',
                        document,
                        null,
                        XPathResult.FIRST_ORDERED_NODE_TYPE,
                        null
                    ).singleNodeValue;

                    if (element) {
                        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()

            val result = injectJavaScript(webView, script)
            result.trim() == "true"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scroll to element", e)
            false
        }
    }

    /**
     * Inject JavaScript and get result
     *
     * @param webView Target WebView
     * @param script JavaScript code
     * @return JavaScript execution result as string
     */
    private suspend fun injectJavaScript(webView: WebView, script: String): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                webView.evaluateJavascript(script) { result ->
                    if (result != null) {
                        continuation.resume(result)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("JavaScript returned null")
                        )
                    }
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Parse DOM elements from JSON
     *
     * @param json JSON string from JavaScript
     * @return List of scraped web elements
     */
    private fun parseDOMElements(json: String): List<ScrapedWebElement> {
        return try {
            val elements = mutableListOf<ScrapedWebElement>()
            val jsonArray = JSONArray(json.trim('"').replace("\\\"", "\""))

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val element = parseElement(obj)
                if (element != null) {
                    elements.add(element)
                }
            }

            Log.d(TAG, "Parsed ${elements.size} elements from DOM")
            elements
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse DOM elements", e)
            emptyList()
        }
    }

    /**
     * Parse single element from JSON object
     *
     * @param obj JSON object
     * @return Scraped web element or null
     */
    private fun parseElement(obj: JSONObject): ScrapedWebElement? {
        return try {
            val tagName = obj.getString("tagName")
            val xpath = obj.getString("xpath")
            val text = obj.optString("text", "").take(MAX_TEXT_LENGTH)
            val ariaLabel = obj.optString("ariaLabel", null)
            val role = obj.optString("role", null)
            val clickable = obj.optBoolean("clickable", false)
            val visible = obj.optBoolean("visible", false)

            val bounds = obj.getJSONObject("bounds")
            val boundsJson = """{"x":${bounds.getInt("x")},"y":${bounds.getInt("y")},"width":${bounds.getInt("width")},"height":${bounds.getInt("height")}}"""

            val elementHash = hashElement(tagName, xpath, text)
            val parentElementHash = obj.optString("parentHash", null)

            ScrapedWebElement(
                id = 0,
                websiteUrlHash = "",  // Set by caller
                elementHash = elementHash,
                tagName = tagName,
                xpath = xpath,
                text = text.ifBlank { null },
                ariaLabel = ariaLabel?.ifBlank { null },
                role = role?.ifBlank { null },
                parentElementHash = parentElementHash?.ifBlank { null },
                clickable = clickable,
                visible = visible,
                bounds = boundsJson
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse element", e)
            null
        }
    }

    /**
     * Generate hash for element deduplication
     *
     * @param tagName HTML tag name
     * @param xpath XPath selector
     * @param text Element text
     * @return SHA-256 hash
     */
    private fun hashElement(tagName: String, xpath: String, text: String): String {
        val input = "$tagName:$xpath:$text"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * DOM extraction JavaScript
     *
     * Traverses DOM and extracts interactive elements with metadata.
     */
    private val DOM_EXTRACTION_SCRIPT = """
        (function() {
            var elements = [];
            var elementIndex = 0;

            function getXPath(element) {
                if (element.id) {
                    return '//*[@id="' + element.id + '"]';
                }

                if (element === document.body) {
                    return '/html/body';
                }

                var siblings = element.parentNode.childNodes;
                var index = 1;

                for (var i = 0; i < siblings.length; i++) {
                    var sibling = siblings[i];
                    if (sibling === element) {
                        break;
                    }
                    if (sibling.nodeType === 1 && sibling.nodeName === element.nodeName) {
                        index++;
                    }
                }

                return getXPath(element.parentNode) + '/' + element.nodeName.toLowerCase() + '[' + index + ']';
            }

            function isVisible(element) {
                var style = window.getComputedStyle(element);
                return style.display !== 'none' &&
                       style.visibility !== 'hidden' &&
                       style.opacity !== '0' &&
                       element.offsetWidth > 0 &&
                       element.offsetHeight > 0;
            }

            function isClickable(element) {
                var tag = element.tagName.toUpperCase();
                var role = element.getAttribute('role');

                var interactiveTags = ['BUTTON', 'A', 'INPUT', 'SELECT', 'TEXTAREA', 'LABEL', 'SUMMARY', 'DETAILS'];
                var interactiveRoles = ['button', 'link', 'tab', 'menuitem', 'option', 'radio', 'checkbox'];

                return interactiveTags.includes(tag) ||
                       interactiveRoles.includes(role) ||
                       element.onclick != null ||
                       element.hasAttribute('onclick');
            }

            function getText(element) {
                var text = element.innerText || element.textContent || '';
                return text.trim().substring(0, 100);
            }

            function getBounds(element) {
                var rect = element.getBoundingClientRect();
                return {
                    x: Math.round(rect.left),
                    y: Math.round(rect.top),
                    width: Math.round(rect.width),
                    height: Math.round(rect.height)
                };
            }

            function extractElement(element, parentHash) {
                var tagName = element.tagName.toUpperCase();
                var xpath = getXPath(element);
                var text = getText(element);
                var ariaLabel = element.getAttribute('aria-label') || '';
                var role = element.getAttribute('role') || '';
                var clickable = isClickable(element);
                var visible = isVisible(element);
                var bounds = getBounds(element);

                if (clickable || visible) {
                    elements.push({
                        tagName: tagName,
                        xpath: xpath,
                        text: text,
                        ariaLabel: ariaLabel,
                        role: role,
                        clickable: clickable,
                        visible: visible,
                        bounds: bounds,
                        parentHash: parentHash || null
                    });
                }
            }

            function traverseDOM(node, parentHash) {
                if (node.nodeType === 1) {
                    extractElement(node, parentHash);

                    var children = node.children;
                    for (var i = 0; i < children.length; i++) {
                        traverseDOM(children[i], null);
                    }
                }
            }

            traverseDOM(document.body, null);

            return JSON.stringify(elements);
        })();
    """.trimIndent()
}
