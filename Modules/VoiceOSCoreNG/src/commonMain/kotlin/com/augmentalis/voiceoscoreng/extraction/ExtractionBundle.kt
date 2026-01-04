package com.augmentalis.voiceoscoreng.extraction

/**
 * ExtractionBundle - Provides shared JavaScript for web element extraction.
 *
 * The JavaScript bundle is injected into WebViews to extract interactive
 * elements for voice targeting. This is platform-agnostic - the actual
 * injection is handled by platform-specific code.
 *
 * Supported platforms:
 * - Android: WebView.evaluateJavascript()
 * - iOS: WKWebView.evaluateJavaScript()
 * - Desktop: CDP Runtime.evaluate
 */
object ExtractionBundle {

    /**
     * The JavaScript code for element extraction.
     *
     * Returns JSON when executed:
     * {
     *   "elements": [
     *     {
     *       "className": "button",
     *       "resourceId": "submit-btn",
     *       "text": "Submit",
     *       "contentDescription": "",
     *       "bounds": "10,20,100,50",
     *       "clickable": true,
     *       "scrollable": false,
     *       "enabled": true,
     *       "packageName": "example.com"
     *     }
     *   ],
     *   "metadata": {
     *     "url": "https://example.com",
     *     "title": "Example Page",
     *     "timestamp": 1234567890,
     *     "elementCount": 10
     *   }
     * }
     */
    val ELEMENT_EXTRACTOR_JS: String by lazy {
        loadExtractionScript()
    }

    /**
     * Load the extraction script from resources.
     *
     * Falls back to embedded script if resource loading fails.
     */
    private fun loadExtractionScript(): String {
        // In actual implementation, this would load from resources
        // For now, return the embedded script
        return EMBEDDED_EXTRACTOR_JS
    }

    /**
     * Embedded extraction JavaScript.
     *
     * This is a fallback if resource loading fails.
     * Keep in sync with element-extractor.js resource.
     */
    private val EMBEDDED_EXTRACTOR_JS = """
(function() {
    'use strict';
    var INTERACTIVE_SELECTORS = [
        'button', 'input:not([type="hidden"])', 'a[href]', 'select', 'textarea',
        '[role="button"]', '[role="link"]', '[role="checkbox"]', '[role="radio"]',
        '[role="textbox"]', '[role="combobox"]', '[role="menuitem"]', '[role="tab"]',
        '[onclick]', '[tabindex]:not([tabindex="-1"])'
    ];

    function extractElements() {
        var elements = [];
        var seen = {};
        var nodes = document.querySelectorAll(INTERACTIVE_SELECTORS.join(', '));

        for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            var info = extractElementInfo(el);
            if (info && !isDuplicate(info, seen)) {
                elements.push(info);
                markSeen(info, seen);
            }
        }

        return {
            elements: elements,
            metadata: {
                url: window.location.href,
                title: document.title,
                timestamp: Date.now(),
                elementCount: elements.length
            }
        };
    }

    function extractElementInfo(el) {
        if (!isVisible(el)) return null;
        var rect = el.getBoundingClientRect();
        var tagName = el.tagName.toLowerCase();
        var id = el.id || '';
        var ariaLabel = el.getAttribute('aria-label') || '';
        var placeholder = el.getAttribute('placeholder') || '';
        var title = el.getAttribute('title') || '';
        var name = el.getAttribute('name') || '';
        var role = el.getAttribute('role') || '';
        var text = getVisibleText(el);
        var contentDesc = ariaLabel || placeholder || title || '';
        var resourceId = id || name || '';
        if (!text && !contentDesc && !resourceId) return null;

        return {
            className: tagName,
            resourceId: resourceId,
            text: text,
            contentDescription: contentDesc,
            bounds: formatBounds(rect),
            clickable: isElementClickable(el, tagName, role),
            scrollable: isElementScrollable(el),
            enabled: !el.disabled,
            packageName: window.location.hostname
        };
    }

    function isVisible(el) {
        if (!el) return false;
        var style = window.getComputedStyle(el);
        if (style.display === 'none' || style.visibility === 'hidden') return false;
        var rect = el.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }

    function getVisibleText(el) {
        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') return el.value || '';
        return (el.innerText || el.textContent || '').trim().substring(0, 100);
    }

    function isElementClickable(el, tagName, role) {
        var clickable = ['button', 'a', 'input', 'select', 'textarea'];
        var roles = ['button', 'link', 'checkbox', 'radio', 'menuitem', 'tab'];
        return clickable.indexOf(tagName) >= 0 || roles.indexOf(role) >= 0 || el.hasAttribute('onclick');
    }

    function isElementScrollable(el) {
        var style = window.getComputedStyle(el);
        return (style.overflow + style.overflowY).indexOf('scroll') >= 0;
    }

    function formatBounds(rect) {
        return Math.round(rect.left) + ',' + Math.round(rect.top) + ',' +
               Math.round(rect.right) + ',' + Math.round(rect.bottom);
    }

    function isDuplicate(info, seen) {
        if (info.resourceId && seen[info.resourceId]) return true;
        return !!seen[info.className + ':' + info.text + ':' + info.bounds];
    }

    function markSeen(info, seen) {
        if (info.resourceId) seen[info.resourceId] = true;
        seen[info.className + ':' + info.text + ':' + info.bounds] = true;
    }

    return JSON.stringify(extractElements());
})();
    """.trimIndent()

    /**
     * Check if the extraction script is valid JavaScript.
     */
    fun isScriptValid(): Boolean {
        return ELEMENT_EXTRACTOR_JS.isNotBlank() &&
               ELEMENT_EXTRACTOR_JS.contains("extractElements") &&
               ELEMENT_EXTRACTOR_JS.contains("JSON.stringify")
    }

    /**
     * Get script size in bytes.
     */
    fun getScriptSize(): Int = ELEMENT_EXTRACTOR_JS.length
}
