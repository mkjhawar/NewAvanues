/**
 * ElementExtractor.kt - Extracts UI elements from accessibility tree
 *
 * Migrated from VoiceOS to Avanues consolidated app.
 * Handles element extraction, hierarchy tracking, deduplication,
 * and dynamic container detection.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.foundation.util.HashUtils
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo

/**
 * Hierarchy node for tracking parent-child relationships in the accessibility tree.
 */
data class HierarchyNode(
    val index: Int,
    val depth: Int,
    val parentIndex: Int?,
    val childCount: Int,
    val className: String
)

/**
 * Information about a duplicate element found during extraction.
 */
data class DuplicateInfo(
    val hash: String,
    val element: ElementInfo,
    val firstSeenIndex: Int
)

/**
 * Extracts UI elements from accessibility tree.
 * Single Responsibility: Extract and process accessibility nodes.
 */
object ElementExtractor {

    private val dynamicContainerTypes = setOf(
        "RecyclerView", "ListView", "GridView",
        "ViewPager", "ViewPager2",
        "ScrollView", "HorizontalScrollView", "NestedScrollView",
        "LazyColumn", "LazyRow", "LazyVerticalGrid", "LazyHorizontalGrid"
    )

    fun isDynamicContainer(className: String): Boolean {
        val simpleName = className.substringAfterLast(".")
        return dynamicContainerTypes.any { simpleName.contains(it, ignoreCase = true) }
    }

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
        listIndex: Int = -1,
        isParentClickable: Boolean = false
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val className = node.className?.toString() ?: ""
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
            isEditable = node.isEditable,
            isCheckable = node.isCheckable,
            isContextClickable = node.isContextClickable,
            isParentClickable = false,
            packageName = node.packageName?.toString() ?: "",
            isInDynamicContainer = isInDynamic && !isContainer,
            containerType = if (isInDynamic && !isContainer) currentContainerType else "",
            listIndex = if (isInDynamic && !isContainer) listIndex else -1
        )

        val hashInput = "${element.className}|${element.resourceId}|${element.text}"
        val hash = HashUtils.calculateHash(hashInput).take(16)

        if (seenHashes.contains(hash)) {
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

        hierarchy.add(
            HierarchyNode(
                index = currentIndex,
                depth = depth,
                parentIndex = parentIndex,
                childCount = node.childCount,
                className = element.className.substringAfterLast(".")
            )
        )

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val childListIndex = if (isContainer) i else listIndex
                val canChildrenInheritClickability = isParentClickable || node.isPerformClickable()
                extractElements(
                    child, elements, hierarchy, seenHashes, duplicates,
                    depth + 1, currentIndex,
                    isInDynamic, currentContainerType, childListIndex,
                    isParentClickable = canChildrenInheritClickability
                )
                child.recycle()
            }
        }
    }

    fun findTopLevelListItems(
        listItems: List<ElementInfo>,
        allElements: List<ElementInfo>
    ): List<ElementInfo> {
        val emailRows = listItems.filter { element ->
            val hasValidBounds = !(element.bounds.left == 0 && element.bounds.top == 0 &&
                    element.bounds.right == 0 && element.bounds.bottom == 0)
            if (!hasValidBounds) return@filter false

            val isActionable = element.isClickable || element.isLongClickable
            if (!isActionable) return@filter false

            val content = element.contentDescription.ifBlank { element.text }
            val looksLikeEmailRow = content.startsWith("Unread,") ||
                    content.startsWith("Starred,") ||
                    content.startsWith("Read,") ||
                    (content.contains(",") && content.length > 30)

            val height = element.bounds.bottom - element.bounds.top
            val reasonableHeight = height in 60..300

            looksLikeEmailRow && reasonableHeight
        }

        return emailRows
            .groupBy { it.listIndex }
            .mapNotNull { (_, elements) -> elements.firstOrNull() }
            .sortedBy { it.bounds.top }
    }

    fun deriveElementLabels(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>
    ): Map<Int, String> {
        val labels = mutableMapOf<Int, String>()

        elements.forEachIndexed { index, element ->
            var label: String? = when {
                element.text.isNotBlank() -> element.text.take(30)
                element.contentDescription.isNotBlank() -> element.contentDescription.take(30)
                element.resourceId.isNotBlank() -> element.resourceId.substringAfterLast("/").replace("_", " ")
                else -> null
            }

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

            labels[index] = label ?: element.className.substringAfterLast(".")
        }

        return labels
    }
}

fun AccessibilityNodeInfo.isPerformClickable(): Boolean {
    return isClickable || isEditable || isSelected || isCheckable || isLongClickable || isContextClickable
}
