package com.augmentalis.webavanue

/**
 * JavaScript bridge for DOM scraping in WebAvanue.
 *
 * Provides full DOM access for VoiceOS voice command generation.
 * Unlike accessibility API, this provides:
 * - Complete DOM tree structure
 * - CSS selectors for precise targeting
 * - ARIA roles and labels
 * - Computed bounding boxes
 * - Form field types and states
 */
object DOMScraperBridge {

    /**
     * JavaScript code to inject into WebView for DOM scraping.
     *
     * Returns JSON with all interactive elements and their properties.
     */
    const val SCRAPER_SCRIPT = """
(function() {
    'use strict';

    // Configuration
    const MAX_ELEMENTS = 500;
    const MAX_TEXT_LENGTH = 100;
    const MAX_DEPTH = 15;

    // ===== GARBAGE TEXT FILTERS =====
    // Language-agnostic patterns that indicate non-voice-command content

    const GARBAGE_EXACT = new Set([
        'undefined', 'null', 'nan', 'NaN', 'NULL',
        '[object object]', '[Object object]',
        'function', 'error', 'exception',
        '...', '---', '___', 'true', 'false', ''
    ]);

    // Repetitive words (multi-language)
    const REPETITIVE_WORDS = new Set([
        // English
        'comma', 'dot', 'dash', 'space', 'tab', 'enter', 'null', 'undefined', 'nan', 'true', 'false',
        // German
        'komma', 'punkt', 'strich', 'leerzeichen', 'eingabe', 'undefiniert', 'wahr', 'falsch',
        // Spanish
        'coma', 'punto', 'guion', 'espacio', 'nulo', 'indefinido', 'verdadero',
        // French
        'virgule', 'tiret', 'espace', 'entrer', 'nul', 'indéfini', 'vrai', 'faux'
    ]);

    // Patterns that indicate garbage text
    const GARBAGE_PATTERNS = [
        /^[a-z]+(-[a-z]+){2,}$/i,                    // CSS classes: btn-primary-disabled
        /^[A-Za-z0-9+/=]{20,}$/,                      // Base64/hash strings
        /^(0x)?[a-f0-9]{8,}$/i,                       // Hex strings
        /^[\s\p{P}]+$/u,                              // Just punctuation/whitespace
        /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i, // UUIDs
        /^\[?object\s*\w*\]?$/i,                      // [object Object]
        /^\w+@[a-f0-9]+$/i                            // Object@hash
    ];

    // Check if text is garbage
    function isGarbageText(text) {
        if (!text) return true;
        const trimmed = text.trim();

        // Too short
        if (trimmed.length <= 1) return true;

        // Exact match
        if (GARBAGE_EXACT.has(trimmed.toLowerCase())) return true;

        // Pattern match
        for (const pattern of GARBAGE_PATTERNS) {
            if (pattern.test(trimmed)) return true;
        }

        // Detect repetitive words: "comma comma com"
        const words = trimmed.toLowerCase().split(/[\s,]+/).filter(w => w.length > 0);
        if (words.length >= 2) {
            const firstWord = words[0];
            const prefix = firstWord.substring(0, 3);
            let samePrefix = 0;
            for (const word of words) {
                if (word.startsWith(prefix)) samePrefix++;
            }
            if (samePrefix >= 2) {
                for (const repWord of REPETITIVE_WORDS) {
                    if (firstWord.startsWith(repWord.substring(0, 3))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Clean label text (returns null if garbage)
    function cleanLabel(text) {
        if (!text) return '';
        const trimmed = text.trim();
        if (isGarbageText(trimmed)) return '';
        // Truncate very long text
        return trimmed.length > MAX_TEXT_LENGTH ? trimmed.substring(0, MAX_TEXT_LENGTH) : trimmed;
    }

    // Element types we care about
    const INTERACTIVE_TAGS = new Set([
        'A', 'BUTTON', 'INPUT', 'SELECT', 'TEXTAREA', 'LABEL',
        'SUMMARY', 'DETAILS', 'DIALOG', 'MENU', 'MENUITEM'
    ]);

    const INTERACTIVE_ROLES = new Set([
        'button', 'link', 'menuitem', 'option', 'tab', 'checkbox',
        'radio', 'textbox', 'searchbox', 'combobox', 'listbox',
        'slider', 'switch', 'spinbutton', 'menuitemcheckbox',
        'menuitemradio', 'treeitem', 'gridcell', 'row'
    ]);

    // Generate unique ID for elements
    let idCounter = 0;
    function generateId() {
        return 'vos_' + (++idCounter);
    }

    // Get computed bounding rect
    function getBounds(element) {
        const rect = element.getBoundingClientRect();
        return {
            left: Math.round(rect.left + window.scrollX),
            top: Math.round(rect.top + window.scrollY),
            right: Math.round(rect.right + window.scrollX),
            bottom: Math.round(rect.bottom + window.scrollY),
            width: Math.round(rect.width),
            height: Math.round(rect.height)
        };
    }

    // Check if element is visible
    function isVisible(element) {
        const style = window.getComputedStyle(element);
        if (style.display === 'none') return false;
        if (style.visibility === 'hidden') return false;
        if (parseFloat(style.opacity) === 0) return false;

        const rect = element.getBoundingClientRect();
        if (rect.width === 0 && rect.height === 0) return false;

        return true;
    }

    // Check if element is interactive
    function isInteractive(element) {
        // By tag
        if (INTERACTIVE_TAGS.has(element.tagName)) return true;

        // By role
        const role = element.getAttribute('role');
        if (role && INTERACTIVE_ROLES.has(role.toLowerCase())) return true;

        // By attributes
        if (element.hasAttribute('onclick')) return true;
        if (element.hasAttribute('tabindex') && element.tabIndex >= 0) return true;

        // By contenteditable
        if (element.isContentEditable) return true;

        // Check for click listeners (heuristic: cursor pointer)
        const style = window.getComputedStyle(element);
        if (style.cursor === 'pointer') return true;

        return false;
    }

    // Get accessible name (similar to accessibility API)
    // Filters out garbage text that shouldn't be voice commands
    function getAccessibleName(element) {
        // aria-label takes precedence
        const ariaLabel = element.getAttribute('aria-label');
        if (ariaLabel) {
            const cleaned = cleanLabel(ariaLabel);
            if (cleaned) return cleaned;
        }

        // aria-labelledby
        const labelledBy = element.getAttribute('aria-labelledby');
        if (labelledBy) {
            const labelElement = document.getElementById(labelledBy);
            if (labelElement) {
                const cleaned = cleanLabel(labelElement.textContent);
                if (cleaned) return cleaned;
            }
        }

        // For inputs, check associated label
        if (element.id) {
            const label = document.querySelector('label[for="' + element.id + '"]');
            if (label) {
                const cleaned = cleanLabel(label.textContent);
                if (cleaned) return cleaned;
            }
        }

        // Placeholder for inputs
        if (element.placeholder) {
            const cleaned = cleanLabel(element.placeholder);
            if (cleaned) return cleaned;
        }

        // Title attribute
        if (element.title) {
            const cleaned = cleanLabel(element.title);
            if (cleaned) return cleaned;
        }

        // alt for images
        if (element.alt) {
            const cleaned = cleanLabel(element.alt);
            if (cleaned) return cleaned;
        }

        // Inner text (truncated and cleaned)
        const text = element.textContent || '';
        return cleanLabel(text) || '';
    }

    // Get element type for command generation
    function getElementType(element) {
        const tag = element.tagName.toLowerCase();
        const type = element.type ? element.type.toLowerCase() : '';
        const role = (element.getAttribute('role') || '').toLowerCase();

        if (tag === 'a') return 'link';
        if (tag === 'button' || role === 'button') return 'button';
        if (tag === 'input') {
            if (type === 'submit' || type === 'button') return 'button';
            if (type === 'checkbox') return 'checkbox';
            if (type === 'radio') return 'radio';
            if (type === 'text' || type === 'search' || type === 'email' || type === 'password' || type === 'tel' || type === 'url') return 'input';
            return 'input';
        }
        if (tag === 'select') return 'dropdown';
        if (tag === 'textarea') return 'input';
        if (role === 'tab') return 'tab';
        if (role === 'menuitem') return 'menuitem';
        if (role === 'listbox' || role === 'combobox') return 'dropdown';

        return 'element';
    }

    // Generate CSS selector for element
    function generateSelector(element) {
        if (element.id) {
            return '#' + CSS.escape(element.id);
        }

        let selector = element.tagName.toLowerCase();

        // Add classes (first 2)
        const classes = Array.from(element.classList).slice(0, 2);
        if (classes.length > 0) {
            selector += '.' + classes.map(c => CSS.escape(c)).join('.');
        }

        // Add index if needed
        const parent = element.parentElement;
        if (parent) {
            const siblings = Array.from(parent.children).filter(
                el => el.tagName === element.tagName
            );
            if (siblings.length > 1) {
                const index = siblings.indexOf(element) + 1;
                selector += ':nth-of-type(' + index + ')';
            }
        }

        return selector;
    }

    // Generate unique XPath for element
    function generateXPath(element) {
        if (element.id) {
            return '//*[@id="' + element.id + '"]';
        }

        const parts = [];
        let current = element;

        while (current && current.nodeType === Node.ELEMENT_NODE) {
            let index = 1;
            let sibling = current.previousElementSibling;

            while (sibling) {
                if (sibling.tagName === current.tagName) {
                    index++;
                }
                sibling = sibling.previousElementSibling;
            }

            const tagName = current.tagName.toLowerCase();
            const indexPart = index > 1 ? '[' + index + ']' : '';
            parts.unshift(tagName + indexPart);

            current = current.parentElement;
        }

        return '/' + parts.join('/');
    }

    // Extract element info
    function extractElement(element, depth) {
        const bounds = getBounds(element);
        const name = getAccessibleName(element);
        const type = getElementType(element);

        return {
            id: generateId(),
            tag: element.tagName.toLowerCase(),
            type: type,
            name: name,
            role: element.getAttribute('role') || '',
            ariaLabel: element.getAttribute('aria-label') || '',
            placeholder: element.placeholder || '',
            value: element.value || '',
            href: (typeof element.href === 'string' ? element.href : (element.href && element.href.baseVal ? element.href.baseVal : '')) || '',
            selector: generateSelector(element),
            xpath: generateXPath(element),
            bounds: bounds,
            depth: depth,
            isDisabled: element.disabled || element.getAttribute('aria-disabled') === 'true',
            isChecked: element.checked || element.getAttribute('aria-checked') === 'true',
            isExpanded: element.getAttribute('aria-expanded') === 'true',
            hasPopup: element.getAttribute('aria-haspopup') || '',
            inputType: element.type || ''
        };
    }

    // Main scrape function
    function scrapeDOM() {
        const elements = [];
        const seen = new Set();
        idCounter = 0;

        function traverse(node, depth) {
            if (depth > MAX_DEPTH) return;
            if (elements.length >= MAX_ELEMENTS) return;
            if (!(node instanceof Element)) return;

            // Skip hidden elements
            if (!isVisible(node)) return;

            // Extract interactive elements
            if (isInteractive(node)) {
                // Avoid duplicates
                if (!seen.has(node)) {
                    seen.add(node);
                    elements.push(extractElement(node, depth));
                }
            }

            // Recurse into children
            for (const child of node.children) {
                traverse(child, depth + 1);
            }
        }

        traverse(document.body, 0);

        return {
            url: window.location.href,
            title: document.title,
            timestamp: Date.now(),
            viewport: {
                width: window.innerWidth,
                height: window.innerHeight,
                scrollX: window.scrollX,
                scrollY: window.scrollY,
                pageWidth: document.documentElement.scrollWidth,
                pageHeight: document.documentElement.scrollHeight
            },
            elements: elements,
            elementCount: elements.length
        };
    }

    // Click element by selector or ID
    function clickElement(selectorOrId) {
        let element = document.getElementById(selectorOrId);
        if (!element) {
            element = document.querySelector(selectorOrId);
        }
        if (element) {
            element.click();
            return { success: true, message: 'Clicked element' };
        }
        return { success: false, message: 'Element not found: ' + selectorOrId };
    }

    // Focus element
    function focusElement(selectorOrId) {
        let element = document.getElementById(selectorOrId);
        if (!element) {
            element = document.querySelector(selectorOrId);
        }
        if (element && element.focus) {
            element.focus();
            return { success: true, message: 'Focused element' };
        }
        return { success: false, message: 'Element not found or not focusable' };
    }

    // Input text
    function inputText(selectorOrId, text) {
        let element = document.getElementById(selectorOrId);
        if (!element) {
            element = document.querySelector(selectorOrId);
        }
        if (element && (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA')) {
            element.value = text;
            element.dispatchEvent(new Event('input', { bubbles: true }));
            element.dispatchEvent(new Event('change', { bubbles: true }));
            return { success: true, message: 'Input text set' };
        }
        return { success: false, message: 'Element not found or not an input' };
    }

    // Scroll to element
    function scrollToElement(selectorOrId) {
        let element = document.getElementById(selectorOrId);
        if (!element) {
            element = document.querySelector(selectorOrId);
        }
        if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return { success: true, message: 'Scrolled to element' };
        }
        return { success: false, message: 'Element not found' };
    }

    // Return API
    return JSON.stringify({
        scrape: scrapeDOM(),
        version: '1.0.0'
    });
})();
"""

    /**
     * JavaScript for clicking an element by VoiceOS ID.
     */
    fun clickElementScript(vosId: String): String = """
(function() {
    const elements = document.querySelectorAll('[data-vos-id="$vosId"]');
    if (elements.length > 0) {
        elements[0].click();
        return JSON.stringify({ success: true });
    }
    return JSON.stringify({ success: false, error: 'Element not found' });
})();
"""

    /**
     * JavaScript for focusing an element.
     */
    fun focusElementScript(selector: String): String = """
(function() {
    const el = document.querySelector('$selector');
    if (el) {
        el.focus();
        return JSON.stringify({ success: true });
    }
    return JSON.stringify({ success: false, error: 'Element not found' });
})();
"""

    /**
     * JavaScript for inputting text.
     */
    fun inputTextScript(selector: String, text: String): String {
        val escapedText = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        return """
(function() {
    const el = document.querySelector('$selector');
    if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
        el.value = '$escapedText';
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
        return JSON.stringify({ success: true });
    }
    return JSON.stringify({ success: false, error: 'Element not found or not an input' });
})();
"""
    }

    /**
     * JavaScript for scrolling to element.
     */
    fun scrollToElementScript(selector: String): String = """
(function() {
    const el = document.querySelector('$selector');
    if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return JSON.stringify({ success: true });
    }
    return JSON.stringify({ success: false, error: 'Element not found' });
})();
"""

    /**
     * JavaScript to highlight an element (for debugging/testing).
     */
    fun highlightElementScript(selector: String): String = """
(function() {
    const el = document.querySelector('$selector');
    if (el) {
        const originalOutline = el.style.outline;
        el.style.outline = '3px solid #FF0000';
        setTimeout(() => { el.style.outline = originalOutline; }, 2000);
        return JSON.stringify({ success: true });
    }
    return JSON.stringify({ success: false, error: 'Element not found' });
})();
"""
}
