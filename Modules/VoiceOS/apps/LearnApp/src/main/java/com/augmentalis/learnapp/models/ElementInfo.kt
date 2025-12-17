/**
 * ElementInfo.kt - UI element information model
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/models/ElementInfo.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Data model for UI element properties
 */

package com.augmentalis.learnapp.models

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Element Info
 *
 * Represents complete information about a UI element.
 * Extracted from AccessibilityNodeInfo during exploration.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val element = ElementInfo(
 *     className = "android.widget.Button",
 *     text = "Like",
 *     contentDescription = "Like button",
 *     resourceId = "com.instagram.android:id/like_btn",
 *     isClickable = true,
 *     isEnabled = true,
 *     bounds = Rect(100, 200, 300, 400),
 *     node = accessibilityNode
 * )
 * ```
 *
 * @property className Android class name (e.g., "android.widget.Button")
 * @property text Visible text content
 * @property contentDescription Accessibility content description
 * @property resourceId Resource ID (e.g., "com.app:id/button")
 * @property isClickable Whether element is clickable
 * @property isEnabled Whether element is enabled
 * @property isPassword Whether element is a password field
 * @property isScrollable Whether element is scrollable
 * @property bounds Screen bounds (x, y, width, height)
 * @property node Reference to AccessibilityNodeInfo (for actions)
 * @property uuid Generated UUID (set after registration)
 *
 * @since 1.0.0
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
    val bounds: Rect = Rect(),
    val node: AccessibilityNodeInfo? = null,
    var uuid: String? = null,
    var classification: String? = null
) {

    /**
     * Get display name (text or contentDescription)
     *
     * @return Human-readable name
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
     *
     * @return true if EditText
     */
    fun isEditText(): Boolean {
        return className.contains("EditText", ignoreCase = true)
    }

    /**
     * Check if element is Button
     *
     * @return true if Button
     */
    fun isButton(): Boolean {
        return className.contains("Button", ignoreCase = true)
    }

    /**
     * Check if element is ImageView
     *
     * @return true if ImageView
     */
    fun isImageView(): Boolean {
        return className.contains("ImageView", ignoreCase = true) ||
               className.contains("ImageButton", ignoreCase = true)
    }

    /**
     * Check if element has meaningful content
     *
     * @return true if has text or contentDescription
     */
    fun hasMeaningfulContent(): Boolean {
        return text.isNotBlank() || contentDescription.isNotBlank()
    }

    /**
     * Extract element type string
     *
     * @return Element type (button, text, input, image, etc.)
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
     *
     * @return Copy without node
     */
    fun withoutNode(): ElementInfo {
        return copy(node = null)
    }

    override fun toString(): String {
        return """
            ElementInfo:
            - Class: $className
            - Text: $text
            - Description: $contentDescription
            - ResourceId: $resourceId
            - Clickable: $isClickable
            - Enabled: $isEnabled
            - Type: ${extractElementType()}
        """.trimIndent()
    }

    companion object {
        /**
         * Create ElementInfo from AccessibilityNodeInfo
         *
         * @param node Accessibility node
         * @return ElementInfo
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
                bounds = bounds,
                node = node
            )
        }
    }
}
