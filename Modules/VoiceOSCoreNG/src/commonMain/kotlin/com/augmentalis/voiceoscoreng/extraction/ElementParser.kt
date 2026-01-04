package com.augmentalis.voiceoscoreng.extraction

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlinx.serialization.json.*

/**
 * ElementParser - Shared HTML and Accessibility parsing for KMP.
 *
 * Provides platform-agnostic parsing of:
 * - HTML content from WebViews
 * - Accessibility tree JSON from native platforms
 * - XPath generation for element targeting
 *
 * Used by the hybrid extraction system across Android, iOS, and Desktop.
 */
object ElementParser {

    // Interactive HTML element tags
    private val INTERACTIVE_TAGS = setOf(
        "button", "input", "select", "textarea", "a",
        "label", "option", "details", "summary"
    )

    // Attributes that indicate interactivity
    private val INTERACTIVE_ROLES = setOf(
        "button", "link", "checkbox", "radio", "textbox",
        "combobox", "listbox", "menuitem", "tab", "switch"
    )

    /**
     * Parse HTML content into ElementInfo objects.
     *
     * Extracts interactive elements (buttons, inputs, links, etc.)
     * with their attributes for voice targeting.
     *
     * @param html Raw HTML string
     * @return List of extracted ElementInfo objects
     */
    fun parseHtml(html: String): List<ElementInfo> {
        if (html.isBlank()) return emptyList()

        val elements = mutableListOf<ElementInfo>()

        try {
            // Simple regex-based extraction for KMP compatibility
            // Full DOM parsing would require platform-specific libraries

            // Extract button elements
            extractElements(html, "button", elements)

            // Extract input elements
            extractElements(html, "input", elements)

            // Extract anchor elements
            extractElements(html, "a", elements)

            // Extract select elements
            extractElements(html, "select", elements)

            // Extract textarea elements
            extractElements(html, "textarea", elements)

            // Extract elements with role attribute
            extractElementsByRole(html, elements)

        } catch (e: Exception) {
            // Return partial results on error
        }

        return elements
    }

    /**
     * Extract elements of a specific tag type.
     */
    private fun extractElements(html: String, tag: String, elements: MutableList<ElementInfo>) {
        // Pattern 1: <tag attrs>content</tag> (with or without attributes)
        val contentPattern = Regex("<$tag(?:\\s+([^>]*))?\\s*>([^<]*)</$tag>", RegexOption.IGNORE_CASE)

        // Pattern 2: Self-closing <tag attrs/> or <tag attrs>
        val selfClosingPattern = Regex("<$tag(?:\\s+([^>]*))?\\s*/?>", RegexOption.IGNORE_CASE)

        // First extract elements with content
        contentPattern.findAll(html).forEach { match ->
            val attrs = match.groupValues.getOrNull(1) ?: ""
            val textContent = match.groupValues.getOrNull(2)?.trim() ?: ""

            val element = parseElementAttributes(tag, attrs, textContent)
            if (element != null) {
                elements.add(element)
            }
        }

        // Extract self-closing elements and elements where we only need the opening tag
        // (select has child options, we just need its attributes)
        if (tag in setOf("input", "select", "textarea", "img", "br", "hr", "meta", "link")) {
            selfClosingPattern.findAll(html).forEach { match ->
                val attrs = match.groupValues.getOrNull(1) ?: ""

                val element = parseElementAttributes(tag, attrs, "")
                if (element != null && elements.none { it.resourceId == element.resourceId && element.resourceId.isNotBlank() }) {
                    elements.add(element)
                }
            }
        }
    }

    /**
     * Extract elements with role="..." attribute.
     */
    private fun extractElementsByRole(html: String, elements: MutableList<ElementInfo>) {
        val rolePattern = Regex("role\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val existingIds = elements.mapNotNull { it.resourceId.takeIf { id -> id.isNotBlank() } }.toSet()

        // Find all elements with role attribute
        val tagPattern = Regex("<(\\w+)\\s+([^>]*role\\s*=\\s*[\"'][^\"']+[\"'][^>]*)>", RegexOption.IGNORE_CASE)

        tagPattern.findAll(html).forEach { match ->
            val tagName = match.groupValues[1]
            val attrs = match.groupValues[2]

            val roleMatch = rolePattern.find(attrs)
            val role = roleMatch?.groupValues?.get(1)?.lowercase() ?: ""

            if (role in INTERACTIVE_ROLES && tagName.lowercase() !in INTERACTIVE_TAGS) {
                val element = parseElementAttributes(tagName, attrs, "")
                if (element != null && element.resourceId !in existingIds) {
                    elements.add(element)
                }
            }
        }
    }

    /**
     * Parse attributes from an element tag.
     */
    private fun parseElementAttributes(tag: String, attrs: String, textContent: String): ElementInfo? {
        val id = extractAttribute(attrs, "id") ?: ""
        val ariaLabel = extractAttribute(attrs, "aria-label") ?: ""
        val placeholder = extractAttribute(attrs, "placeholder") ?: ""
        val title = extractAttribute(attrs, "title") ?: ""
        val name = extractAttribute(attrs, "name") ?: ""
        val disabled = attrs.contains("disabled", ignoreCase = true)

        // For form inputs (input, select, textarea), aria-label goes to contentDescription
        val isFormInput = tag.lowercase() in setOf("input", "select", "textarea")

        // Determine best text label (visible text content only)
        val text = if (isFormInput) {
            textContent.takeIf { it.isNotBlank() } ?: ""
        } else {
            textContent.takeIf { it.isNotBlank() }
                ?: ariaLabel.takeIf { it.isNotBlank() }
                ?: title.takeIf { it.isNotBlank() }
                ?: ""
        }

        // For form inputs, aria-label is the primary content description
        val contentDesc = ariaLabel.takeIf { it.isNotBlank() }
            ?: placeholder.takeIf { it.isNotBlank() }
            ?: title.takeIf { it.isNotBlank() && it != text }
            ?: ""

        // Determine resource ID
        val resourceId = id.takeIf { it.isNotBlank() }
            ?: name.takeIf { it.isNotBlank() }
            ?: ""

        // Skip elements without any identifiable content
        if (text.isBlank() && contentDesc.isBlank() && resourceId.isBlank()) {
            return null
        }

        return ElementInfo(
            className = tag.lowercase(),
            resourceId = resourceId,
            text = text,
            contentDescription = contentDesc,
            bounds = Bounds.EMPTY,
            isClickable = true,
            isScrollable = false,
            isEnabled = !disabled,
            packageName = ""
        )
    }

    /**
     * Extract attribute value from attributes string.
     */
    private fun extractAttribute(attrs: String, name: String): String? {
        val pattern = Regex("$name\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        return pattern.find(attrs)?.groupValues?.get(1)
    }

    /**
     * Parse accessibility tree JSON into ElementInfo objects.
     *
     * Expected format:
     * {
     *   "elements": [
     *     {
     *       "className": "android.widget.Button",
     *       "text": "Submit",
     *       "resourceId": "com.app:id/submit",
     *       "bounds": "0,0,100,50",
     *       "clickable": true,
     *       "scrollable": false,
     *       "enabled": true,
     *       "contentDescription": ""
     *     }
     *   ]
     * }
     *
     * @param json JSON string from accessibility extraction
     * @return List of ElementInfo objects
     */
    fun parseAccessibilityJson(json: String): List<ElementInfo> {
        if (json.isBlank()) return emptyList()

        return try {
            val jsonElement = Json.parseToJsonElement(json)
            val elementsArray = jsonElement.jsonObject["elements"]?.jsonArray ?: return emptyList()

            elementsArray.mapNotNull { element ->
                parseJsonElement(element.jsonObject)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parse a single JSON object into ElementInfo.
     */
    private fun parseJsonElement(obj: JsonObject): ElementInfo? {
        return try {
            val className = obj["className"]?.jsonPrimitive?.content ?: return null
            val text = obj["text"]?.jsonPrimitive?.content ?: ""
            val resourceId = obj["resourceId"]?.jsonPrimitive?.content ?: ""
            val contentDesc = obj["contentDescription"]?.jsonPrimitive?.content ?: ""
            val boundsStr = obj["bounds"]?.jsonPrimitive?.content ?: ""
            val clickable = obj["clickable"]?.jsonPrimitive?.boolean ?: false
            val scrollable = obj["scrollable"]?.jsonPrimitive?.boolean ?: false
            val enabled = obj["enabled"]?.jsonPrimitive?.boolean ?: true
            val packageName = obj["packageName"]?.jsonPrimitive?.content ?: ""

            ElementInfo(
                className = className,
                text = text,
                resourceId = resourceId,
                contentDescription = contentDesc,
                bounds = Bounds.fromString(boundsStr) ?: Bounds.EMPTY,
                isClickable = clickable,
                isScrollable = scrollable,
                isEnabled = enabled,
                packageName = packageName
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate XPath for targeting an element.
     *
     * Creates a unique XPath based on available attributes.
     *
     * @param element The ElementInfo to generate XPath for
     * @return XPath string for element selection
     */
    fun generateXPath(element: ElementInfo): String {
        val tag = element.className.lowercase()

        return when {
            // Prefer ID-based selection
            element.resourceId.isNotBlank() -> {
                "//$tag[@id='${escapeXPath(element.resourceId)}']"
            }
            // Use text content
            element.text.isNotBlank() -> {
                "//$tag[contains(text(),'${escapeXPath(element.text)}')]"
            }
            // Use aria-label/content description
            element.contentDescription.isNotBlank() -> {
                "//$tag[@aria-label='${escapeXPath(element.contentDescription)}']"
            }
            // Fallback to just tag
            else -> "//$tag"
        }
    }

    /**
     * Escape special characters for XPath.
     */
    private fun escapeXPath(value: String): String {
        return value.replace("'", "\\'")
    }

    /**
     * Filter to only actionable elements (clickable or scrollable).
     *
     * @param elements List of all elements
     * @return Filtered list of actionable elements
     */
    fun filterActionable(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { it.isClickable || it.isScrollable }
    }

    /**
     * Filter to elements with voice-targetable content.
     *
     * @param elements List of all elements
     * @return Elements with text, contentDescription, or resourceId
     */
    fun filterWithContent(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { it.hasVoiceContent }
    }

    /**
     * Remove duplicate elements based on resourceId.
     *
     * Keeps the first occurrence of each unique resourceId.
     *
     * @param elements List of elements (may contain duplicates)
     * @return Deduplicated list
     */
    fun deduplicate(elements: List<ElementInfo>): List<ElementInfo> {
        val seen = mutableSetOf<String>()
        return elements.filter { element ->
            val key = element.resourceId.takeIf { it.isNotBlank() }
                ?: "${element.className}:${element.text}:${element.bounds}"

            if (key in seen) {
                false
            } else {
                seen.add(key)
                true
            }
        }
    }
}
