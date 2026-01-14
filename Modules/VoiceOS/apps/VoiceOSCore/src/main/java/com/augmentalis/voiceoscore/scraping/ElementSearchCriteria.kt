/**
 * ElementSearchCriteria.kt - Search criteria for Tier 3 element lookup
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI-Assisted Implementation
 * Created: 2025-12-01
 *
 * Data class representing search criteria for real-time element lookup.
 * Used by ElementSearchEngine to find elements in the live accessibility tree.
 *
 * Part of Voice Command Element Persistence feature (Phase 3 - Tier 3)
 */
package com.augmentalis.voiceoscore.scraping

import android.graphics.Rect

/**
 * Search criteria for Tier 3 real-time element search
 *
 * Contains multiple properties for element matching. Search engine tries
 * them in priority order:
 * 1. viewIdResourceName (95% reliability)
 * 2. bounds + text combination (85% reliability)
 * 3. className + contentDescription (70% reliability)
 * 4. text only (60% reliability - fallback)
 *
 * ## Usage:
 * ```kotlin
 * val criteria = ElementSearchCriteria(
 *     viewIdResourceName = "com.example:id/submit_button",
 *     text = "Submit"
 * )
 * val element = searchEngine.findElement(criteria)
 * ```
 */
data class ElementSearchCriteria(
    /**
     * View ID resource name (e.g., "com.example:id/submit_button")
     * Most reliable identifier - survives app updates if ID unchanged
     */
    val viewIdResourceName: String? = null,

    /**
     * Screen bounds of element
     * Used with text for position-based matching
     */
    val bounds: Rect? = null,

    /**
     * Visible text content
     * Combined with bounds or used alone for text-based search
     */
    val text: String? = null,

    /**
     * Accessibility content description
     * Used for elements without visible text (icons, images)
     */
    val contentDescription: String? = null,

    /**
     * Class name (e.g., "android.widget.Button")
     * Combined with contentDescription for type-based matching
     */
    val className: String? = null,

    /**
     * Element hash from previous scraping
     * Highest confidence match if still valid
     */
    val elementHash: String? = null,

    /**
     * Desired action type (click, long_click, scroll, type)
     * Filters results to elements supporting this action
     */
    val actionType: String? = null
) {
    /**
     * Check if criteria has any search properties set
     */
    fun isEmpty(): Boolean {
        return viewIdResourceName == null &&
               bounds == null &&
               text == null &&
               contentDescription == null &&
               className == null &&
               elementHash == null
    }

    /**
     * Check if criteria can match by viewId (highest priority)
     */
    fun hasViewId(): Boolean = !viewIdResourceName.isNullOrBlank()

    /**
     * Check if criteria can match by bounds + text (medium priority)
     */
    fun hasBoundsAndText(): Boolean = bounds != null && !text.isNullOrBlank()

    /**
     * Check if criteria can match by class + description (lower priority)
     */
    fun hasClassAndDescription(): Boolean =
        !className.isNullOrBlank() && !contentDescription.isNullOrBlank()

    /**
     * Check if criteria can match by text only (fallback)
     */
    fun hasTextOnly(): Boolean = !text.isNullOrBlank()

    companion object {
        /**
         * Create criteria from voice command target
         * Parses natural language target into search properties
         */
        fun fromVoiceTarget(target: String): ElementSearchCriteria {
            return ElementSearchCriteria(
                text = target,
                contentDescription = target
            )
        }

        /**
         * Create criteria from database element
         * Used to re-find an element after app state changes
         */
        fun fromDatabaseElement(
            elementHash: String,
            viewIdResourceName: String?,
            text: String?,
            contentDescription: String?,
            className: String?,
            boundsString: String?
        ): ElementSearchCriteria {
            val bounds = boundsString?.let { parseBounds(it) }

            return ElementSearchCriteria(
                elementHash = elementHash,
                viewIdResourceName = viewIdResourceName,
                text = text,
                contentDescription = contentDescription,
                className = className,
                bounds = bounds
            )
        }

        /**
         * Parse bounds string "left,top,right,bottom" to Rect
         */
        private fun parseBounds(boundsString: String): Rect? {
            return try {
                val parts = boundsString.split(",")
                if (parts.size == 4) {
                    Rect(
                        parts[0].toInt(),
                        parts[1].toInt(),
                        parts[2].toInt(),
                        parts[3].toInt()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
