/**
 * ElementLabels.kt - Shared label pipeline for overlay element labeling
 *
 * Platform-independent label derivation and cleaning for UI element overlays.
 * Used by Android (accessibility tree) and iOS (VoiceOver) to produce clean,
 * human-readable labels for interactive elements.
 *
 * Single source of truth: all overlay label text flows through this pipeline.
 * No platform-specific label cleaning should exist outside this file.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

/**
 * Hierarchy node for tracking parent-child relationships in the element tree.
 *
 * Platform-specific extractors (Android AccessibilityNodeInfo, iOS AXUIElement)
 * populate this during tree traversal. The shared label pipeline then uses it
 * for child-walking fallback when an element has no direct label.
 */
data class HierarchyNode(
    val index: Int,
    val depth: Int,
    val parentIndex: Int?,
    val childCount: Int,
    val className: String
)

/**
 * Shared label pipeline for overlay element labeling.
 *
 * ## Architecture
 *
 * ```
 * Platform extraction (Android/iOS)
 *   → List<ElementInfo> + List<HierarchyNode>
 *   → ElementLabels.deriveElementLabels()    ← SINGLE SOURCE OF TRUTH
 *   → Map<Int, String>                       ← cleaned labels for all elements
 *   → Overlay generators consume labels map
 * ```
 *
 * ## Why This Exists
 *
 * Apps like Gmail produce comma-separated contentDescription strings:
 * "Unread, Starred, , John Doe, Subject, snippet, 2:30 PM"
 *
 * Without cleaning, overlay labels show the raw mess. This pipeline strips
 * status prefixes, empty segments, and resource ID noise to produce clean
 * labels like "John Doe" or "Archive".
 */
object ElementLabels {

    // ===== Status Words =====

    /**
     * Common status prefixes in email/messaging apps.
     * Lowercase for case-insensitive matching.
     * These are stripped from the beginning of comma-separated contentDescriptions.
     */
    private val STATUS_WORDS = setOf(
        "unread", "read", "starred", "not starred",
        "important", "not important", "has attachment",
        "sent", "draft", "spam", "trash",
        "flagged", "unflagged", "archived",
        "pinned", "muted", "snoozed"
    )

    // ===== Label Cleaning =====

    /**
     * Clean comma-separated contentDescription strings.
     *
     * Handles Gmail-style "Unread, , , Sender, Subject, snippet, date" by:
     * 1. Splitting on commas
     * 2. Removing blank segments
     * 3. Dropping leading status words (case-insensitive)
     * 4. Returning the first meaningful segment
     *
     * @param raw The raw contentDescription string
     * @return Cleaned label, max 30 characters
     */
    fun cleanCommaLabel(raw: String): String {
        if (!raw.contains(",")) return raw.take(30)

        val parts = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return raw.take(30)

        val meaningful = parts.dropWhile { it.lowercase() in STATUS_WORDS }
        return (meaningful.firstOrNull() ?: parts.firstOrNull() ?: raw).take(30)
    }

    /**
     * Clean a resource ID into a human-readable label.
     *
     * "com.google.android.gm:id/action_archive" → "Archive"
     * "com.app:id/btn_send_message" → "Send message"
     *
     * @param resourceId The full resource ID string
     * @return Human-readable label, max 30 characters
     */
    fun cleanResourceId(resourceId: String): String {
        return resourceId
            .substringAfterLast("/")
            .removePrefix("action_")
            .removePrefix("btn_")
            .removePrefix("menu_")
            .removePrefix("ic_")
            .removePrefix("img_")
            .replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            .take(30)
    }

    // ===== Element Classification =====

    /**
     * Determines if an element is an icon-only interactive element.
     *
     * Icon-only = clickable, no visible text, small size, but has metadata
     * (contentDescription or resourceId) for deriving a label.
     *
     * These elements receive text labels (Layer 1) in the overlay system,
     * displayed below the icon regardless of whether numbered badges are on.
     *
     * @param element The element to classify
     * @return true if this is an icon-only interactive element
     */
    fun isIconOnlyElement(element: ElementInfo): Boolean {
        if (element.text.isNotBlank()) return false
        if (!element.isClickable && !element.isLongClickable) return false
        if (element.isScrollable || element.isEditable) return false
        if (element.contentDescription.isBlank() && element.resourceId.isBlank()) return false

        val width = element.bounds.right - element.bounds.left
        val height = element.bounds.bottom - element.bounds.top
        if (width <= 0 || height <= 0) return false
        if (width > 300 || height > 300) return false

        return true
    }

    /**
     * Filter list items to find top-level rows (email rows, message items, etc.).
     *
     * Identifies rows in list-based apps by matching patterns in their
     * contentDescription (e.g., "Unread, Sender, Subject, date") and
     * checking for reasonable row height.
     *
     * Case-insensitive prefix matching for robustness across Android versions,
     * locales, and app variants.
     *
     * @param listItems Elements within dynamic containers (listIndex >= 0)
     * @param allElements All extracted elements (for future hierarchy lookups)
     * @return Deduplicated, sorted list of top-level row elements
     */
    fun findTopLevelListItems(
        listItems: List<ElementInfo>,
        allElements: List<ElementInfo>
    ): List<ElementInfo> {
        val rows = listItems.filter { element ->
            val hasValidBounds = !(element.bounds.left == 0 && element.bounds.top == 0 &&
                    element.bounds.right == 0 && element.bounds.bottom == 0)
            if (!hasValidBounds) return@filter false

            val isActionable = element.isClickable || element.isLongClickable
            if (!isActionable) return@filter false

            val content = element.contentDescription.ifBlank { element.text }
            val contentLower = content.lowercase()
            val looksLikeListRow = contentLower.startsWith("unread,") ||
                    contentLower.startsWith("starred,") ||
                    contentLower.startsWith("read,") ||
                    contentLower.startsWith("flagged,") ||
                    contentLower.startsWith("important,") ||
                    (content.contains(",") && content.length > 30)

            val height = element.bounds.bottom - element.bounds.top
            val reasonableHeight = height in 60..300

            looksLikeListRow && reasonableHeight
        }

        return rows
            .groupBy { it.listIndex }
            .mapNotNull { (_, group) -> group.firstOrNull() }
            .sortedBy { it.bounds.top }
    }

    // ===== Label Derivation =====

    /**
     * Derive clean labels for all extracted elements.
     *
     * This is the SINGLE SOURCE OF TRUTH for overlay label text.
     * All overlay generators (numbered badges, icon labels) consume this map.
     * No generator should derive labels independently.
     *
     * Priority per element:
     * 1. `text` — already human-readable visible text
     * 2. `contentDescription` → [cleanCommaLabel] strips status prefixes
     * 3. `resourceId` → [cleanResourceId] humanizes the ID
     * 4. Walk direct children for text/contentDescription
     * 5. Fallback to simplified class name
     *
     * @param elements All extracted elements from accessibility tree
     * @param hierarchy Parent-child relationship data for child walking
     * @return Map from element index to cleaned label string
     */
    fun deriveElementLabels(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>
    ): Map<Int, String> {
        val labels = mutableMapOf<Int, String>()

        elements.forEachIndexed { index, element ->
            var label: String? = when {
                element.text.isNotBlank() -> element.text.take(30)
                element.contentDescription.isNotBlank() -> cleanCommaLabel(element.contentDescription)
                element.resourceId.isNotBlank() -> cleanResourceId(element.resourceId)
                else -> null
            }

            // Child-walking fallback: look at direct children for label text
            if (label == null) {
                val node = hierarchy.getOrNull(index)
                if (node != null && node.childCount > 0) {
                    for (childIdx in (index + 1) until minOf(index + 10, elements.size)) {
                        val childNode = hierarchy.getOrNull(childIdx) ?: continue
                        if (childNode.depth <= node.depth) break
                        if (childNode.depth == node.depth + 1) {
                            val childElement = elements[childIdx]
                            if (childElement.text.isNotBlank()) {
                                label = childElement.text.take(30)
                                break
                            }
                            if (childElement.contentDescription.isNotBlank()) {
                                label = cleanCommaLabel(childElement.contentDescription)
                                break
                            }
                        }
                    }
                }
            }

            labels[index] = label ?: element.className.substringAfterLast(".")
        }

        return labels
    }
}
