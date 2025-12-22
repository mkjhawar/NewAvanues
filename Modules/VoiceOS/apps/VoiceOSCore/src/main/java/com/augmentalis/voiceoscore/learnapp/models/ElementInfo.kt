/**
 * ElementInfo.kt - UI element information model
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Data model for UI element properties
 */

package com.augmentalis.voiceoscore.learnapp.models

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Element Info
 *
 * Represents complete information about a UI element.
 * Extracted from AccessibilityNodeInfo during exploration.
 */
data class ElementInfo(
    val className: String,
    val text: String = "",
    val contentDescription: String = "",
    val resourceId: String = "",
    val isClickable: Boolean = false,
    val isEnabled: Boolean = false,
    val isPassword: Boolean = false,
    val isScrollable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isCheckable: Boolean = false,
    val isFocusable: Boolean = false,
    val bounds: Rect = Rect(),
    val node: AccessibilityNodeInfo? = null,
    var uuid: String? = null,
    var classification: String? = null,
    val index: Int = 0,
    val indexInParent: Int = 0,
    val depth: Int = 0,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val parent: ElementInfo? = null,
    val children: List<ElementInfo>? = null,
    val elementHash: String = ""
) {

    /**
     * Get display name (text or contentDescription)
     */
    fun getDisplayName(): String {
        return when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            resourceId.isNotBlank() -> resourceId.substringAfterLast('/')
            else -> "Unknown"
        }
    }

    /**
     * Check if element is EditText field
     */
    fun isEditText(): Boolean {
        return className.contains("EditText", ignoreCase = true)
    }

    /**
     * Check if element is Button
     */
    fun isButton(): Boolean {
        return className.contains("Button", ignoreCase = true)
    }

    /**
     * Check if element is ImageView
     */
    fun isImageView(): Boolean {
        return className.contains("ImageView", ignoreCase = true) ||
               className.contains("ImageButton", ignoreCase = true)
    }

    /**
     * Check if element has meaningful content
     */
    fun hasMeaningfulContent(): Boolean {
        return text.isNotBlank() || contentDescription.isNotBlank()
    }

    /**
     * Extract element type string
     */
    fun extractElementType(): String {
        val lowerClassName = className.lowercase()

        return when {
            lowerClassName.contains("button") -> "button"
            lowerClassName.contains("textview") -> "text"
            lowerClassName.contains("edittext") -> "input"
            lowerClassName.contains("imageview") || lowerClassName.contains("imagebutton") -> "image"
            lowerClassName.contains("checkbox") -> "checkbox"
            lowerClassName.contains("radiobutton") -> "radio"
            lowerClassName.contains("switch") -> "switch"
            lowerClassName.contains("seekbar") || lowerClassName.contains("slider") -> "slider"
            lowerClassName.contains("viewgroup") || lowerClassName.contains("layout") -> "container"
            else -> "view"
        }
    }

    /**
     * Create copy without node reference (for serialization)
     */
    fun withoutNode(): ElementInfo {
        return copy(node = null)
    }

    /**
     * Recycle the AccessibilityNodeInfo to free memory.
     * Should be called when the ElementInfo is no longer needed.
     */
    fun recycleNode() {
        try {
            node?.recycle()
        } catch (e: Exception) {
            // Ignore - node may already be recycled
        }
    }

    /**
     * Get a stable identifier for this element.
     * Combines class, resourceId, text, and bounds to create a unique identifier.
     */
    fun stableId(): String {
        if (elementHash.isNotBlank()) return elementHash
        uuid?.let { return it } // Return UUID if present (already null-checked)

        // Create stable ID from element properties
        val parts = listOf(
            className,
            resourceId,
            text.take(50),
            contentDescription.take(50),
            "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
        )
        return parts.joinToString("|").hashCode().toString(16)
    }

    /**
     * Get stability score based on element properties.
     * Higher score means more stable/reliable identification.
     */
    fun stabilityScore(): Float {
        var score = 0f
        if (resourceId.isNotBlank()) score += 40f
        if (text.isNotBlank()) score += 25f
        if (contentDescription.isNotBlank()) score += 20f
        if (className.isNotBlank()) score += 10f
        if (bounds.width() > 0 && bounds.height() > 0) score += 5f
        return score.coerceAtMost(100f)
    }

    override fun toString(): String {
        return "ElementInfo(class=$className, text=$text, desc=$contentDescription, clickable=$isClickable)"
    }

    companion object {
        /**
         * Create ElementInfo from AccessibilityNodeInfo
         */
        fun fromNode(node: AccessibilityNodeInfo): ElementInfo {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            return ElementInfo(
                className = node.className?.toString() ?: "",
                text = node.text?.toString() ?: "",
                contentDescription = node.contentDescription?.toString() ?: "",
                resourceId = node.viewIdResourceName ?: "",
                isClickable = node.isClickable,
                isEnabled = node.isEnabled,
                isPassword = node.isPassword,
                isScrollable = node.isScrollable,
                isLongClickable = node.isLongClickable,
                isEditable = node.isEditable,
                isCheckable = node.isCheckable,
                isFocusable = node.isFocusable,
                bounds = bounds,
                node = node
            )
        }

        /**
         * Create ElementInfo with extended properties
         */
        fun fromNode(
            node: AccessibilityNodeInfo,
            index: Int = 0,
            indexInParent: Int = 0,
            depth: Int = 0,
            screenWidth: Int = 0,
            screenHeight: Int = 0
        ): ElementInfo {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            return ElementInfo(
                className = node.className?.toString() ?: "",
                text = node.text?.toString() ?: "",
                contentDescription = node.contentDescription?.toString() ?: "",
                resourceId = node.viewIdResourceName ?: "",
                isClickable = node.isClickable,
                isEnabled = node.isEnabled,
                isPassword = node.isPassword,
                isScrollable = node.isScrollable,
                isLongClickable = node.isLongClickable,
                isEditable = node.isEditable,
                isCheckable = node.isCheckable,
                isFocusable = node.isFocusable,
                bounds = bounds,
                node = node,
                index = index,
                indexInParent = indexInParent,
                depth = depth,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
    }
}
