/**
 * VoiceOS Element Extractor - Shared JavaScript for web element extraction
 *
 * Extracts interactive elements from web pages for voice targeting.
 * Used by Android WebView, iOS WKWebView, and Desktop CDP.
 *
 * Returns JSON: { elements: [...], metadata: {...} }
 */
(function() {
    'use strict';

    // Interactive element selectors
    var INTERACTIVE_SELECTORS = [
        'button',
        'input:not([type="hidden"])',
        'a[href]',
        'select',
        'textarea',
        '[role="button"]',
        '[role="link"]',
        '[role="checkbox"]',
        '[role="radio"]',
        '[role="textbox"]',
        '[role="combobox"]',
        '[role="listbox"]',
        '[role="menuitem"]',
        '[role="tab"]',
        '[role="switch"]',
        '[onclick]',
        '[tabindex]:not([tabindex="-1"])'
    ];

    /**
     * Extract all interactive elements from the page
     */
    function extractElements() {
        var elements = [];
        var seen = {};

        var selector = INTERACTIVE_SELECTORS.join(', ');
        var nodes = document.querySelectorAll(selector);

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

    /**
     * Extract info from a single element
     */
    function extractElementInfo(el) {
        // Skip hidden elements
        if (!isVisible(el)) return null;

        var rect = el.getBoundingClientRect();
        var tagName = el.tagName.toLowerCase();

        // Get identifying attributes
        var id = el.id || '';
        var ariaLabel = el.getAttribute('aria-label') || '';
        var placeholder = el.getAttribute('placeholder') || '';
        var title = el.getAttribute('title') || '';
        var name = el.getAttribute('name') || '';
        var role = el.getAttribute('role') || '';
        var type = el.getAttribute('type') || '';

        // Get visible text
        var text = getVisibleText(el);

        // Determine content description
        var contentDesc = ariaLabel || placeholder || title || '';

        // Resource ID (prefer id, then name)
        var resourceId = id || name || '';

        // Skip if no identifiable content
        if (!text && !contentDesc && !resourceId) return null;

        // Determine if clickable/scrollable
        var isClickable = isElementClickable(el, tagName, role);
        var isScrollable = isElementScrollable(el);
        var isEnabled = !el.disabled && !el.hasAttribute('aria-disabled');

        return {
            className: tagName,
            resourceId: resourceId,
            text: text,
            contentDescription: contentDesc,
            bounds: formatBounds(rect),
            clickable: isClickable,
            scrollable: isScrollable,
            enabled: isEnabled,
            packageName: window.location.hostname,
            attributes: {
                role: role,
                type: type,
                href: el.getAttribute('href') || ''
            }
        };
    }

    /**
     * Check if element is visible
     */
    function isVisible(el) {
        if (!el) return false;

        var style = window.getComputedStyle(el);
        if (style.display === 'none') return false;
        if (style.visibility === 'hidden') return false;
        if (parseFloat(style.opacity) === 0) return false;

        var rect = el.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) return false;

        return true;
    }

    /**
     * Get visible text content
     */
    function getVisibleText(el) {
        // For inputs, return value
        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
            return el.value || '';
        }

        // For buttons and links, get inner text
        var text = el.innerText || el.textContent || '';
        return text.trim().substring(0, 100); // Limit length
    }

    /**
     * Check if element is clickable
     */
    function isElementClickable(el, tagName, role) {
        var clickableTags = ['button', 'a', 'input', 'select', 'textarea'];
        var clickableRoles = ['button', 'link', 'checkbox', 'radio', 'menuitem', 'tab', 'switch'];

        if (clickableTags.indexOf(tagName) >= 0) return true;
        if (clickableRoles.indexOf(role) >= 0) return true;
        if (el.hasAttribute('onclick')) return true;
        if (el.hasAttribute('tabindex')) return true;

        return false;
    }

    /**
     * Check if element is scrollable
     */
    function isElementScrollable(el) {
        var style = window.getComputedStyle(el);
        var overflow = style.overflow + style.overflowX + style.overflowY;
        return overflow.indexOf('scroll') >= 0 || overflow.indexOf('auto') >= 0;
    }

    /**
     * Format bounds as string: "left,top,right,bottom"
     */
    function formatBounds(rect) {
        return Math.round(rect.left) + ',' +
               Math.round(rect.top) + ',' +
               Math.round(rect.right) + ',' +
               Math.round(rect.bottom);
    }

    /**
     * Check for duplicate elements
     */
    function isDuplicate(info, seen) {
        if (info.resourceId && seen[info.resourceId]) return true;
        var key = info.className + ':' + info.text + ':' + info.bounds;
        return !!seen[key];
    }

    /**
     * Mark element as seen
     */
    function markSeen(info, seen) {
        if (info.resourceId) seen[info.resourceId] = true;
        var key = info.className + ':' + info.text + ':' + info.bounds;
        seen[key] = true;
    }

    // Execute and return JSON
    return JSON.stringify(extractElements());
})();
