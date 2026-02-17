/**
 * ElementExtractor.kt - Extracts UI elements from accessibility tree
 *
 * Migrated from VoiceOS to Avanues consolidated app.
 * Handles element extraction, hierarchy tracking, deduplication,
 * and dynamic container detection.
 *
 * Pure algorithms (container classification, deduplication model, element hashing)
 * are in KMP: ElementClassification.kt. This file contains only the Android-specific
 * AccessibilityNodeInfo tree traversal.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.DuplicateInfo
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HierarchyNode
import com.augmentalis.voiceoscore.calculateElementHash
import com.augmentalis.voiceoscore.isDynamicContainer

/**
 * Extracts UI elements from accessibility tree.
 * Single Responsibility: Extract and process accessibility nodes.
 */
object ElementExtractor {

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

        val hash = calculateElementHash(element)

        if (seenHashes.contains(hash)) {
            duplicates.add(
                DuplicateInfo(
                    hash = hash,
                    element = element,
                    firstSeenIndex = elements.indexOfFirst { e ->
                        calculateElementHash(e) == hash
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
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

}

fun AccessibilityNodeInfo.isPerformClickable(): Boolean {
    return isClickable || isEditable || isSelected || isCheckable || isLongClickable || isContextClickable
}
