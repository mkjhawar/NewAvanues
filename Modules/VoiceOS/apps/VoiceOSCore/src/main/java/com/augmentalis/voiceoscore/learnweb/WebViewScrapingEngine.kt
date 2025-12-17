/**
 * WebViewScrapingEngine.kt - DOM extraction with JavaScript injection
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/WebViewScrapingEngine.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * WebView scraping engine using JavaScript injection for DOM extraction with hierarchy tracking
 */

package com.augmentalis.voiceoscore.learnweb

import android.content.Context
import android.util.Log
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
 * Builds element hierarchy with parent-child relationships.
 *
 * @property context Application context
 *
 * @since 1.0.0
 */
class WebViewScrapingEngine(private val context: Context) {

    companion object {
        private const val TAG = "WebViewScrapingEngine"

        /**
         * Maximum text length to extract from elements
         */
        private const val MAX_TEXT_LENGTH = 100
    }

    /**
     * Extract DOM structure from WebView
     *
     * Injects JavaScript to traverse DOM and extract element metadata.
     * Builds parent-child hierarchy and filters relevant elements.
     *
     * @param webView WebView instance
     * @return List of scraped web elements
     */
    suspend fun extractDOMStructure(webView: WebView): List<ScrapedWebElement> {
        return suspendCancellableCoroutine { continuation ->
            val jsCode = buildDOMExtractionScript()

            webView.post {
                webView.evaluateJavascript(jsCode) { result ->
                    try {
                        if (result == null || result == "null") {
                            Log.e(TAG, "JavaScript returned null")
                            continuation.resume(emptyList())
                            return@evaluateJavascript
                        }

                        // Remove quotes from JSON string
                        val jsonString = result.trim('"').replace("\\\"", "\"")
                            .replace("\\n", "")
                            .replace("\\t", "")

                        val jsonArray = JSONArray(jsonString)
                        val elements = parseElementsFromJSON(jsonArray, webView.url ?: "")

                        Log.d(TAG, "Extracted ${elements.size} elements from DOM")
                        continuation.resume(elements)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse DOM extraction result", e)
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    }

    /**
     * Build DOM extraction JavaScript
     *
     * Creates JavaScript code that:
     * 1. Generates XPath for each element
     * 2. Extracts element metadata (tag, text, aria, bounds)
     * 3. Builds parent-child hierarchy
     * 4. Filters interactive/visible elements
     *
     * @return JavaScript code
     */
    private fun buildDOMExtractionScript(): String {
        return """
        (function() {
            // Generate XPath for element
            function getXPath(element) {
                if (element.id !== '') {
                    return '//*[@id="' + element.id + '"]';
                }
                if (element === document.body) {
                    return '/html/body';
                }

                var ix = 0;
                var siblings = element.parentNode ? element.parentNode.childNodes : [];
                for (var i = 0; i < siblings.length; i++) {
                    var sibling = siblings[i];
                    if (sibling === element) {
                        var parentPath = element.parentNode ? getXPath(element.parentNode) : '';
                        return parentPath + '/' + element.tagName.toLowerCase() + '[' + (ix + 1) + ']';
                    }
                    if (sibling.nodeType === 1 && sibling.tagName === element.tagName) {
                        ix++;
                    }
                }
                return '';
            }

            // Generate hash for element
            function hashElement(element) {
                var str = element.tagName + ':' + getXPath(element);
                var hash = 0;
                for (var i = 0; i < str.length; i++) {
                    var char = str.charCodeAt(i);
                    hash = ((hash << 5) - hash) + char;
                    hash = hash & hash;
                }
                return 'elem_' + Math.abs(hash).toString(16);
            }

            // Check if element is interactive
            function isInteractive(element) {
                var tag = element.tagName.toUpperCase();
                var interactiveTags = ['A', 'BUTTON', 'INPUT', 'SELECT', 'TEXTAREA'];

                if (interactiveTags.indexOf(tag) !== -1) {
                    return true;
                }

                if (element.onclick || element.hasAttribute('onclick')) {
                    return true;
                }

                var role = element.getAttribute('role');
                if (role && ['button', 'link', 'menuitem', 'tab'].indexOf(role) !== -1) {
                    return true;
                }

                return false;
            }

            // Check if element is visible
            function isVisible(element) {
                if (element.offsetParent === null) {
                    return false;
                }

                var style = window.getComputedStyle(element);
                if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
                    return false;
                }

                var rect = element.getBoundingClientRect();
                if (rect.width === 0 || rect.height === 0) {
                    return false;
                }

                return true;
            }

            // Extract element data
            function extractElement(element, parentHash) {
                var rect = element.getBoundingClientRect();
                var text = element.innerText || element.textContent || '';
                text = text.trim().substring(0, $MAX_TEXT_LENGTH);

                return {
                    elementHash: hashElement(element),
                    tagName: element.tagName.toUpperCase(),
                    xpath: getXPath(element),
                    text: text || null,
                    ariaLabel: element.getAttribute('aria-label') || null,
                    role: element.getAttribute('role') || null,
                    parentElementHash: parentHash,
                    clickable: isInteractive(element),
                    visible: isVisible(element),
                    bounds: JSON.stringify({
                        x: Math.round(rect.left),
                        y: Math.round(rect.top),
                        width: Math.round(rect.width),
                        height: Math.round(rect.height)
                    })
                };
            }

            // Traverse DOM recursively
            function traverseDOM(node, parentHash, results) {
                if (node.nodeType !== 1) {
                    return;
                }

                var elementData = extractElement(node, parentHash);

                // Filter: only include interactive or visible elements with content
                if (elementData.clickable ||
                    (elementData.visible && (elementData.text || elementData.ariaLabel))) {
                    results.push(elementData);

                    // Traverse children with this element as parent
                    for (var i = 0; i < node.children.length; i++) {
                        traverseDOM(node.children[i], elementData.elementHash, results);
                    }
                }
            }

            // Start extraction from body
            var results = [];
            traverseDOM(document.body, null, results);

            return JSON.stringify(results);
        })();
        """.trimIndent()
    }

    /**
     * Parse elements from JSON array
     *
     * @param jsonArray JSON array from JavaScript
     * @param websiteUrlHash Website URL hash
     * @return List of scraped web elements
     */
    private fun parseElementsFromJSON(jsonArray: JSONArray, url: String): List<ScrapedWebElement> {
        val elements = mutableListOf<ScrapedWebElement>()
        val urlHash = hashURL(url)

        for (i in 0 until jsonArray.length()) {
            try {
                val jsonObject = jsonArray.getJSONObject(i)
                val element = parseElement(jsonObject, urlHash)
                elements.add(element)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse element at index $i", e)
            }
        }

        return elements
    }

    /**
     * Parse single element from JSON
     *
     * @param jsonObject JSON object
     * @param websiteUrlHash Website URL hash
     * @return Scraped web element
     */
    private fun parseElement(jsonObject: JSONObject, websiteUrlHash: String): ScrapedWebElement {
        return ScrapedWebElement(
            id = 0, // Auto-generated
            websiteUrlHash = websiteUrlHash,
            elementHash = jsonObject.getString("elementHash"),
            tagName = jsonObject.getString("tagName"),
            xpath = jsonObject.getString("xpath"),
            text = jsonObject.optString("text", null),
            ariaLabel = jsonObject.optString("ariaLabel", null),
            role = jsonObject.optString("role", null),
            parentElementHash = jsonObject.optString("parentElementHash", null),
            clickable = jsonObject.getBoolean("clickable"),
            visible = jsonObject.getBoolean("visible"),
            bounds = jsonObject.getString("bounds")
        )
    }

    /**
     * Get page title from WebView
     *
     * @param webView WebView instance
     * @return Page title
     */
    suspend fun getPageTitle(webView: WebView): String {
        return suspendCancellableCoroutine { continuation ->
            webView.post {
                webView.evaluateJavascript("document.title") { result ->
                    val title = result?.trim('"') ?: "Untitled"
                    continuation.resume(title)
                }
            }
        }
    }

    /**
     * Click element by XPath
     *
     * @param webView WebView instance
     * @param xpath XPath selector
     * @return True if successful
     */
    suspend fun clickElement(webView: WebView, xpath: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val jsCode = """
                (function() {
                    function getElementByXPath(xpath) {
                        return document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                    }

                    var element = getElementByXPath('$xpath');
                    if (element) {
                        element.click();
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()

            webView.post {
                webView.evaluateJavascript(jsCode) { result ->
                    val success = result == "true"
                    continuation.resume(success)
                }
            }
        }
    }

    /**
     * Scroll to element by XPath
     *
     * @param webView WebView instance
     * @param xpath XPath selector
     * @return True if successful
     */
    suspend fun scrollToElement(webView: WebView, xpath: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val jsCode = """
                (function() {
                    function getElementByXPath(xpath) {
                        return document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                    }

                    var element = getElementByXPath('$xpath');
                    if (element) {
                        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()

            webView.post {
                webView.evaluateJavascript(jsCode) { result ->
                    val success = result == "true"
                    continuation.resume(success)
                }
            }
        }
    }

    /**
     * Hash URL for storage
     *
     * @param url Full URL
     * @return SHA-256 hash
     */
    private fun hashURL(url: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hash URL: $url", e)
            url.hashCode().toString()
        }
    }
}
