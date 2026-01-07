/**
 * ExtractionBundle.kt - JavaScript extraction bundle for web elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.extraction

/**
 * Contains the JavaScript bundle for extracting interactive elements from web pages.
 */
object ExtractionBundle {

    /**
     * JavaScript IIFE that extracts interactive elements from a web page.
     * Returns JSON with element data including bounds, text, and accessibility info.
     */
    val ELEMENT_EXTRACTOR_JS: String = """
(function() {
    'use strict';

    function isVisible(el) {
        if (!el) return false;
        const style = window.getComputedStyle(el);
        if (style.display === 'none' || style.visibility === 'hidden') return false;
        if (parseFloat(style.opacity) === 0) return false;
        const rect = el.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }

    function formatBounds(rect) {
        return Math.round(rect.left) + ',' + Math.round(rect.top) + ',' +
               Math.round(rect.right) + ',' + Math.round(rect.bottom);
    }

    function isDuplicate(elements, newEl) {
        return elements.some(el =>
            el.resourceId && el.resourceId === newEl.resourceId
        );
    }

    function extractElements() {
        const results = [];
        const selectors = [
            'button',
            'input',
            'select',
            'textarea',
            'a[href]',
            '[role=button]',
            '[role=link]',
            '[role=checkbox]',
            '[role=radio]',
            '[role=menuitem]',
            '[role=tab]',
            '[onclick]',
            '[tabindex]:not([tabindex="-1"])'
        ];

        const allElements = document.querySelectorAll(selectors.join(','));

        allElements.forEach(function(el) {
            if (!isVisible(el)) return;

            const rect = el.getBoundingClientRect();
            const ariaLabel = el.getAttribute('aria-label') || '';
            const disabled = el.hasAttribute('disabled') || el.getAttribute('aria-disabled') === 'true';

            const elementData = {
                className: el.tagName.toLowerCase(),
                text: el.textContent ? el.textContent.trim().substring(0, 100) : '',
                resourceId: el.id || null,
                contentDescription: ariaLabel,
                bounds: formatBounds(rect),
                isClickable: true,
                isEnabled: !disabled,
                role: el.getAttribute('role') || ''
            };

            if (!isDuplicate(results, elementData)) {
                results.push(elementData);
            }
        });

        return {
            url: window.location.href,
            title: document.title,
            elements: results,
            timestamp: Date.now()
        };
    }

    return JSON.stringify(extractElements());
})();
""".trimIndent()

    /**
     * Validates that the script is properly formed.
     */
    fun isScriptValid(): Boolean {
        return ELEMENT_EXTRACTOR_JS.isNotBlank() &&
               ELEMENT_EXTRACTOR_JS.contains("extractElements") &&
               ELEMENT_EXTRACTOR_JS.contains("JSON.stringify")
    }

    /**
     * Returns the size of the script in bytes.
     */
    fun getScriptSize(): Int {
        return ELEMENT_EXTRACTOR_JS.encodeToByteArray().size
    }
}
