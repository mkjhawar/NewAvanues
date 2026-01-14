/**
 * JitCapturedElement.kt - Element captured during JIT learning
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Data class representing a UI element captured during Just-In-Time learning.
 * Used for passive screen-by-screen learning without full exploration.
 */

package com.augmentalis.voiceoscore.learnapp.jit

import android.graphics.Rect

/**
 * JIT Captured Element
 *
 * Represents a UI element captured during Just-In-Time (JIT) learning.
 * Contains minimal information needed for command generation.
 *
 * This is a lightweight representation compared to full ElementInfo,
 * optimized for passive learning performance.
 *
 * @property className Android class name of the element
 * @property text Text content of the element
 * @property contentDescription Accessibility content description
 * @property viewIdResourceName Android resource ID of the element
 * @property isClickable Whether element is clickable
 * @property isLongClickable Whether element supports long click
 * @property isEnabled Whether element is enabled
 * @property isEditable Whether element is editable (text input)
 * @property isCheckable Whether element is checkable
 * @property isFocusable Whether element is focusable
 * @property isScrollable Whether element supports scrolling
 * @property bounds Element bounds on screen
 * @property uuid Optional pre-assigned UUID for the element
 * @property elementHash Hash of element's identifying properties
 * @property depth Depth in the view hierarchy
 * @property indexInParent Index among siblings
 */
data class JitCapturedElement(
    val className: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isEnabled: Boolean = true,
    val isEditable: Boolean = false,
    val isCheckable: Boolean = false,
    val isFocusable: Boolean = false,
    val isScrollable: Boolean = false,
    val bounds: Rect = Rect(),
    val uuid: String? = null,
    val elementHash: String = "",
    val depth: Int = 0,
    val indexInParent: Int = 0
) {
    /**
     * Get the best label for this element
     *
     * Priority: text > contentDescription > resourceId suffix
     */
    fun getBestLabel(): String? {
        return text?.takeIf { it.isNotBlank() }
            ?: contentDescription?.takeIf { it.isNotBlank() }
            ?: viewIdResourceName?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
    }

    /**
     * Check if element has any usable label
     */
    fun hasLabel(): Boolean = getBestLabel() != null

    /**
     * Check if element is actionable (clickable and enabled)
     */
    fun isActionable(): Boolean = isClickable && isEnabled

    /**
     * Create a copy with a new UUID
     */
    fun withUuid(newUuid: String): JitCapturedElement = copy(uuid = newUuid)

    companion object {
        /**
         * Create from accessibility node info values
         */
        fun from(
            className: CharSequence?,
            text: CharSequence?,
            contentDescription: CharSequence?,
            viewIdResourceName: String?,
            isClickable: Boolean,
            isLongClickable: Boolean = false,
            isEnabled: Boolean,
            isEditable: Boolean = false,
            isCheckable: Boolean = false,
            isFocusable: Boolean = false,
            isScrollable: Boolean,
            bounds: Rect,
            depth: Int = 0,
            indexInParent: Int = 0
        ): JitCapturedElement {
            // Generate element hash from identifying properties
            val hashInput = buildString {
                append(className ?: "")
                append(viewIdResourceName ?: "")
                append(text ?: "")
                append(contentDescription ?: "")
                append(bounds.toShortString())
            }
            val elementHash = hashInput.hashCode().toString(16)

            return JitCapturedElement(
                className = className?.toString() ?: "",
                text = text?.toString(),
                contentDescription = contentDescription?.toString(),
                viewIdResourceName = viewIdResourceName,
                isClickable = isClickable,
                isLongClickable = isLongClickable,
                isEnabled = isEnabled,
                isEditable = isEditable,
                isCheckable = isCheckable,
                isFocusable = isFocusable,
                isScrollable = isScrollable,
                bounds = bounds,
                elementHash = elementHash,
                depth = depth,
                indexInParent = indexInParent
            )
        }
    }
}
