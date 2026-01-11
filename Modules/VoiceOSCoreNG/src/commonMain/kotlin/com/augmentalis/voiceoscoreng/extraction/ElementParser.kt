/**
 * ElementParser.kt - HTML and accessibility tree parsing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.extraction

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Parser for extracting ElementInfo from HTML and accessibility JSON.
 */
object ElementParser {

    // Interactive HTML elements to extract
    private val INTERACTIVE_TAGS = setOf(
        "button", "input", "select", "textarea", "a",
        "[role=button]", "[role=link]", "[role=checkbox]",
        "[role=radio]", "[role=menuitem]", "[role=tab]"
    )

    /**
     * Parses HTML string and extracts interactive elements.
     */
    fun parseHtml(html: String): List<ElementInfo> {
        if (html.isBlank()) return emptyList()

        val elements = mutableListOf<ElementInfo>()

        // Simple regex-based extraction for common interactive elements
        // Button elements
        val buttonRegex = Regex("""<button[^>]*(?:id=["']([^"']*)["'])?[^>]*>([^<]*)</button>""", RegexOption.IGNORE_CASE)
        buttonRegex.findAll(html).forEach { match ->
            val id = match.groupValues.getOrNull(1) ?: ""
            val text = match.groupValues.getOrNull(2)?.trim() ?: ""
            elements.add(ElementInfo(
                className = "button",
                text = text,
                resourceId = id,
                isClickable = true
            ))
        }

        // Input elements
        val inputRegex = Regex("""<input[^>]*(?:id=["']([^"']*)["'])?[^>]*(?:aria-label=["']([^"']*)["'])?[^>]*>""", RegexOption.IGNORE_CASE)
        inputRegex.findAll(html).forEach { match ->
            val id = match.groupValues.getOrNull(1) ?: ""
            val ariaLabel = match.groupValues.getOrNull(2) ?: ""
            elements.add(ElementInfo(
                className = "input",
                resourceId = id,
                contentDescription = ariaLabel,
                isClickable = true
            ))
        }

        // Link elements
        val linkRegex = Regex("""<a[^>]*(?:id=["']([^"']*)["'])?[^>]*>([^<]*)</a>""", RegexOption.IGNORE_CASE)
        linkRegex.findAll(html).forEach { match ->
            val id = match.groupValues.getOrNull(1) ?: ""
            val text = match.groupValues.getOrNull(2)?.trim() ?: ""
            elements.add(ElementInfo(
                className = "a",
                text = text,
                resourceId = id,
                isClickable = true
            ))
        }

        // Select elements
        val selectRegex = Regex("""<select[^>]*(?:id=["']([^"']*)["'])?[^>]*>""", RegexOption.IGNORE_CASE)
        selectRegex.findAll(html).forEach { match ->
            val id = match.groupValues.getOrNull(1) ?: ""
            elements.add(ElementInfo(
                className = "select",
                resourceId = id,
                isClickable = true
            ))
        }

        return elements
    }

    /**
     * Parses accessibility JSON and extracts elements.
     */
    fun parseAccessibilityJson(json: String): List<ElementInfo> {
        if (json.isBlank()) return emptyList()

        val elements = mutableListOf<ElementInfo>()

        // Normalize JSON by removing newlines for simpler regex matching
        val normalizedJson = json.replace("\n", " ").replace("\r", " ")

        // Simple JSON parsing for elements array
        val elementRegex = Regex(
            """"className"\s*:\s*"([^"]*)"""" +
            """.*?"text"\s*:\s*"([^"]*)"""" +
            """.*?"resourceId"\s*:\s*"([^"]*)""""
        )

        for (match: MatchResult in elementRegex.findAll(normalizedJson)) {
            elements.add(ElementInfo(
                className = match.groupValues.getOrNull(1) ?: "",
                text = match.groupValues.getOrNull(2) ?: "",
                resourceId = match.groupValues.getOrNull(3) ?: ""
            ))
        }

        return elements
    }

    /**
     * Generates an XPath expression for an element.
     */
    fun generateXPath(element: ElementInfo): String {
        val tag = element.className.lowercase()

        return when {
            element.resourceId.isNotBlank() ->
                "//$tag[@id='${element.resourceId}']"
            element.text.isNotBlank() ->
                "//$tag[text()='${element.text}']"
            element.contentDescription.isNotBlank() ->
                "//$tag[@aria-label='${element.contentDescription}']"
            else ->
                "//$tag"
        }
    }

    /**
     * Filters to only actionable elements (clickable or scrollable).
     */
    fun filterActionable(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { it.isClickable || it.isScrollable }
    }

    /**
     * Filters to elements with voice labels (text or content description).
     */
    fun filterWithContent(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter {
            it.text.isNotBlank() || it.contentDescription.isNotBlank()
        }
    }

    /**
     * Removes duplicate elements based on resourceId.
     */
    fun deduplicate(elements: List<ElementInfo>): List<ElementInfo> {
        val seen = mutableSetOf<String>()
        return elements.filter { element ->
            val key = element.resourceId ?: return@filter true
            if (key in seen) {
                false
            } else {
                seen.add(key)
                true
            }
        }
    }
}
