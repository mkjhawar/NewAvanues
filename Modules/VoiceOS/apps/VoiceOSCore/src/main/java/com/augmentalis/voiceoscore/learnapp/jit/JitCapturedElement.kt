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
 * @property isEnabled Whether element is enabled
 * @property isScrollable Whether element supports scrolling
 * @property bounds Element bounds on screen
 * @property uuid Optional pre-assigned UUID for the element
 */
data class JitCapturedElement(
    val className: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val isClickable: Boolean = false,
    val isEnabled: Boolean = true,
    val isScrollable: Boolean = false,
    val bounds: Rect = Rect(),
    val uuid: String? = null
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
            isEnabled: Boolean,
            isScrollable: Boolean,
            bounds: Rect
        ): JitCapturedElement {
            return JitCapturedElement(
                className = className?.toString() ?: "",
                text = text?.toString(),
                contentDescription = contentDescription?.toString(),
                viewIdResourceName = viewIdResourceName,
                isClickable = isClickable,
                isEnabled = isEnabled,
                isScrollable = isScrollable,
                bounds = bounds
            )
        }
    }
}
