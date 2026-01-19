/**
 * AccessibilityNodeAdapter.kt - Converts AccessibilityNodeInfo to ElementInfo
 *
 * Provides conversion from Android's AccessibilityNodeInfo to the KMP
 * ElementInfo data class used throughout VoiceOSCore.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 */
package com.augmentalis.voiceoscore

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Adapter for converting Android AccessibilityNodeInfo to KMP ElementInfo.
 *
 * This adapter bridges the platform-specific AccessibilityNodeInfo with
 * the cross-platform ElementInfo data class used by CommandGenerator
 * and ActionCoordinator.
 */
object AccessibilityNodeAdapter {

    /**
     * Dynamic container class names that indicate list/recycler content.
     */
    private val DYNAMIC_CONTAINERS = setOf(
        "RecyclerView",
        "ListView",
        "ScrollView",
        "HorizontalScrollView",
        "ViewPager",
        "ViewPager2",
        "LazyColumn",
        "LazyRow"
    )

    /**
     * Convert a single AccessibilityNodeInfo to ElementInfo.
     *
     * @param node The Android accessibility node
     * @param listIndex Position in list (-1 if not in list)
     * @param isInDynamicContainer Whether this node is inside a dynamic container
     * @param containerType The type of container if in one
     * @return ElementInfo representation of the node
     */
    fun toElementInfo(
        node: AccessibilityNodeInfo,
        listIndex: Int = -1,
        isInDynamicContainer: Boolean = false,
        containerType: String = ""
    ): ElementInfo {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val className = node.className?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""
        val text = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        val packageName = node.packageName?.toString() ?: ""

        // Generate AVID fingerprint
        val avid = ElementFingerprint.generate(
            className = className,
            packageName = packageName,
            resourceId = resourceId,
            text = text,
            contentDesc = contentDescription
        )

        return ElementInfo(
            className = className,
            resourceId = resourceId,
            text = text,
            contentDescription = contentDescription,
            bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            isEnabled = node.isEnabled,
            packageName = packageName,
            avid = avid,
            node = node,
            isInDynamicContainer = isInDynamicContainer,
            containerType = containerType,
            listIndex = listIndex,
            // Compose semantics (if available via extras)
            semanticsRole = extractSemanticsRole(node),
            stateDescription = node.stateDescription?.toString() ?: "",
            isSelected = node.isSelected,
            isChecked = if (node.isCheckable) node.isChecked else null,
            testTag = extractTestTag(node)
        )
    }

    /**
     * Check if a class name represents a dynamic container.
     */
    fun isDynamicContainer(className: String): Boolean {
        return DYNAMIC_CONTAINERS.any { className.contains(it, ignoreCase = true) }
    }

    /**
     * Extract Compose semantics role if available.
     */
    private fun extractSemanticsRole(node: AccessibilityNodeInfo): String {
        // Compose exposes role via className or extras
        val className = node.className?.toString() ?: return ""
        return when {
            className.contains("Button") -> "Button"
            className.contains("CheckBox") -> "Checkbox"
            className.contains("Switch") -> "Switch"
            className.contains("Tab") -> "Tab"
            className.contains("RadioButton") -> "RadioButton"
            className.contains("Slider") -> "Slider"
            className.contains("EditText") -> "TextField"
            className.contains("Image") -> "Image"
            else -> ""
        }
    }

    /**
     * Extract Compose testTag if available.
     * In Compose, testTag is typically exposed via viewIdResourceName.
     */
    private fun extractTestTag(node: AccessibilityNodeInfo): String {
        val resourceId = node.viewIdResourceName ?: return ""
        // Compose testTag often appears as the resource ID suffix
        return if (resourceId.contains(":id/")) {
            resourceId.substringAfterLast(":id/")
        } else {
            ""
        }
    }
}
