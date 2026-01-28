package com.augmentalis.voiceoscoreng.service

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.CommandGenerator
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HashUtils

private const val TAG = "ElementExtractor"

/**
 * Extracts UI elements from accessibility tree.
 *
 * Extracted from VoiceOSAccessibilityService.kt for SOLID compliance.
 * Single Responsibility: Extract and process accessibility nodes.
 */
object ElementExtractor {

    /**
     * Dynamic container types that indicate list/dynamic content.
     */
    private val dynamicContainerTypes = setOf(
        "RecyclerView",
        "ListView",
        "GridView",
        "ViewPager",
        "ViewPager2",
        "ScrollView",
        "HorizontalScrollView",
        "NestedScrollView",
        "LazyColumn",       // Compose
        "LazyRow",          // Compose
        "LazyVerticalGrid", // Compose
        "LazyHorizontalGrid" // Compose
    )

    /**
     * Check if a class name is a dynamic container.
     */
    fun isDynamicContainer(className: String): Boolean {
        val simpleName = className.substringAfterLast(".")
        return dynamicContainerTypes.any { simpleName.contains(it, ignoreCase = true) }
    }

    /**
     * Extract all elements from an accessibility node tree.
     *
     * @param node Root accessibility node to start extraction
     * @param elements Output list to collect ElementInfo objects
     * @param hierarchy Output list to collect hierarchy information
     * @param seenHashes Set to track seen element hashes for deduplication
     * @param duplicates Output list to collect duplicate element info
     * @param depth Current depth in the tree (starts at 0)
     * @param parentIndex Parent element index (null for root)
     * @param inDynamicContainer Whether currently inside a dynamic container
     * @param containerType Type of the current dynamic container
     * @param listIndex Index within the list (for dynamic container items)
     */
    fun extractElements(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        hierarchy: MutableList<HierarchyNode>,
        seenHashes: MutableSet<String>,
        duplicates: MutableList<DuplicateInfo>,
        depth: Int,
        parentIndex: Int? = null,
        inDynamicContainer: Boolean = false,
        containerType: String = "",
        listIndex: Int = -1
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val className = node.className?.toString() ?: ""

        // Check if THIS node is a dynamic container
        val isContainer = isDynamicContainer(className)
        val currentContainerType = if (isContainer) className.substringAfterLast(".") else containerType
        val isInDynamic = inDynamicContainer || isContainer

        val element = ElementInfo(
            className = className,
            resourceId = node.viewIdResourceName ?: "",
            text = node.text?.toString() ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            isEnabled = node.isEnabled,
            packageName = node.packageName?.toString() ?: "",
            // Dynamic content tracking
            isInDynamicContainer = isInDynamic && !isContainer, // Container itself is not dynamic, its children are
            containerType = if (isInDynamic && !isContainer) currentContainerType else "",
            listIndex = if (isInDynamic && !isContainer) listIndex else -1
        )

        // Generate hash for deduplication - use className|resourceId|text (NOT bounds, as bounds make every element unique)
        val hashInput = "${element.className}|${element.resourceId}|${element.text}"
        val hash = HashUtils.calculateHash(hashInput).take(16)

        if (seenHashes.contains(hash)) {
            Log.d(TAG, "DUPLICATE FOUND: hash=$hash class=${element.className.substringAfterLast(".")} text='${element.text.take(20)}'")
            duplicates.add(
                DuplicateInfo(
                    hash = hash,
                    element = element,
                    firstSeenIndex = elements.indexOfFirst { e ->
                        val h = HashUtils.calculateHash("${e.className}|${e.resourceId}|${e.text}").take(16)
                        h == hash
                    }
                )
            )
        } else {
            seenHashes.add(hash)
        }

        val currentIndex = elements.size
        elements.add(element)

        // Track hierarchy
        hierarchy.add(
            HierarchyNode(
                index = currentIndex,
                depth = depth,
                parentIndex = parentIndex,
                childCount = node.childCount,
                className = element.className.substringAfterLast(".")
            )
        )

        // Recurse into children
        // If this is a dynamic container, track child indices for list items
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                // Children of a dynamic container get the list index from their position
                val childListIndex = if (isContainer) i else listIndex
                extractElements(
                    child, elements, hierarchy, seenHashes, duplicates,
                    depth + 1, currentIndex,
                    isInDynamic, currentContainerType, childListIndex
                )
                child.recycle()
            }
        }
    }

    /**
     * Find actual list item rows (like email rows) from dynamic container elements.
     *
     * Strategy: Look for elements that have the email content pattern.
     * Gmail emails have contentDescription like "Unread, , , SenderName, , Subject..."
     * We use this pattern to identify actual email rows.
     *
     * For each valid email row, we use its own bounds for badge positioning.
     *
     * @param listItems Elements with listIndex >= 0 (inside dynamic containers)
     * @param allElements All elements for context
     * @return List of top-level row elements (one per listIndex)
     */
    fun findTopLevelListItems(
        listItems: List<ElementInfo>,
        allElements: List<ElementInfo>
    ): List<ElementInfo> {
        Log.d(TAG, "findTopLevelListItems: ${listItems.size} items with listIndex >= 0")

        // Filter for elements that look like actual list item rows
        // Email rows in Gmail typically have:
        // - contentDescription starting with "Unread," or containing sender/subject
        // - Valid bounds with reasonable height (not tiny icons)
        // - Are clickable or long-clickable
        val emailRows = listItems.filter { element ->
            // Must have valid bounds
            val hasValidBounds = !(element.bounds.left == 0 && element.bounds.top == 0 &&
                element.bounds.right == 0 && element.bounds.bottom == 0)
            if (!hasValidBounds) return@filter false

            // Must be actionable
            val isActionable = element.isClickable || element.isLongClickable
            if (!isActionable) return@filter false

            // Check for email-like content (Gmail specific pattern)
            val content = element.contentDescription.ifBlank { element.text }
            val looksLikeEmailRow = content.startsWith("Unread,") ||
                content.startsWith("Starred,") ||
                content.startsWith("Read,") ||
                (content.contains(",") && content.length > 30) // Long comma-separated content

            // Also check bounds height - email rows are typically 80-200px tall
            val height = element.bounds.bottom - element.bounds.top
            val reasonableHeight = height in 60..300

            val isEmailRow = looksLikeEmailRow && reasonableHeight

            if (isEmailRow) {
                val label = CommandGenerator.extractShortLabel(element) ?: "?"
                Log.d(TAG, "  EMAIL ROW: '$label' bounds=(${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}) h=$height")
            }

            isEmailRow
        }

        Log.d(TAG, "findTopLevelListItems: found ${emailRows.size} email rows")

        // Deduplicate by listIndex (keep first/best per row)
        val deduped = emailRows
            .groupBy { it.listIndex }
            .mapNotNull { (_, elements) -> elements.firstOrNull() }
            .sortedBy { it.bounds.top }

        Log.d(TAG, "findTopLevelListItems: ${deduped.size} unique rows after dedup")

        return deduped
    }

    /**
     * Derive labels for ALL elements by looking at child TextViews when parent has no text.
     * Returns a map of elementIndex -> derivedLabel
     *
     * @param elements List of all extracted elements
     * @param hierarchy Hierarchy information for parent-child relationships
     * @return Map of element index to derived label string
     */
    fun deriveElementLabels(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>
    ): Map<Int, String> {
        val labels = mutableMapOf<Int, String>()

        elements.forEachIndexed { index, element ->
            // First try the element's own content
            var label: String? = when {
                element.text.isNotBlank() -> element.text.take(30)
                element.contentDescription.isNotBlank() -> element.contentDescription.take(30)
                element.resourceId.isNotBlank() -> element.resourceId.substringAfterLast("/").replace("_", " ")
                else -> null
            }

            // If no label, look at children (especially for clickable Views wrapping TextViews)
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
                                label = childElement.contentDescription.take(30)
                                break
                            }
                        }
                    }
                }
            }

            // Store the label (or fallback to class name)
            labels[index] = label ?: element.className.substringAfterLast(".")
        }

        return labels
    }
}
