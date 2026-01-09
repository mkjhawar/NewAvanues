package com.augmentalis.voiceoscoreng.common

/**
 * Represents the bounds of a UI element
 */
data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2

    companion object {
        val EMPTY = Bounds(0, 0, 0, 0)

        fun fromString(boundsStr: String): Bounds? {
            return try {
                val parts = boundsStr.split(",").map { it.trim().toInt() }
                if (parts.size == 4) {
                    Bounds(parts[0], parts[1], parts[2], parts[3])
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun toString(): String = "$left,$top,$right,$bottom"
}

/**
 * Contains information about a UI element for voice accessibility processing.
 *
 * This is the core data class used across VoiceOSCoreNG for representing
 * scraped UI elements that can be targeted by voice commands.
 *
 * @property className The class name of the UI element (e.g., "Button", "EditText")
 * @property resourceId The resource ID if available (e.g., "com.app:id/submit_btn")
 * @property text The text content displayed by the element
 * @property contentDescription The accessibility content description
 * @property bounds The screen coordinates of the element
 * @property isClickable Whether the element responds to click events
 * @property isScrollable Whether the element is scrollable
 * @property isEnabled Whether the element is currently enabled
 * @property packageName The package name of the host application
 */
data class ElementInfo(
    val className: String,
    val resourceId: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val bounds: Bounds = Bounds.EMPTY,
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEnabled: Boolean = true,
    val packageName: String = "",
    // Compose semantics support (P2)
    val semanticsRole: String = "",           // e.g., "Button", "Checkbox", "Tab"
    val stateDescription: String = "",        // e.g., "Checked", "Unchecked", "Selected"
    val isSelected: Boolean = false,          // For tabs, chips, list items
    val isChecked: Boolean? = null,           // For checkboxes, switches (null = not applicable)
    val testTag: String = ""                  // Compose testTag for identification
) {
    /**
     * Get the best available label for voice recognition
     * Priority: text > contentDescription > resourceId simple name
     */
    val voiceLabel: String
        get() = when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            resourceId.isNotBlank() -> resourceId.substringAfterLast("/").replace("_", " ")
            else -> className.substringAfterLast(".")
        }

    /**
     * Check if this element has meaningful content for voice targeting
     */
    val hasVoiceContent: Boolean
        get() = text.isNotBlank() || contentDescription.isNotBlank() || resourceId.isNotBlank()

    /**
     * Check if this element is actionable (clickable or scrollable)
     */
    val isActionable: Boolean
        get() = isClickable || isScrollable

    companion object {
        /**
         * Create an empty ElementInfo (useful for testing)
         */
        val EMPTY = ElementInfo(className = "")

        /**
         * Create ElementInfo for a simple button
         */
        fun button(
            text: String,
            resourceId: String = "",
            bounds: Bounds = Bounds.EMPTY,
            packageName: String = ""
        ) = ElementInfo(
            className = "Button",
            text = text,
            resourceId = resourceId,
            bounds = bounds,
            isClickable = true,
            isEnabled = true,
            packageName = packageName
        )

        /**
         * Create ElementInfo for a text input field
         */
        fun input(
            hint: String = "",
            resourceId: String = "",
            bounds: Bounds = Bounds.EMPTY,
            packageName: String = ""
        ) = ElementInfo(
            className = "EditText",
            contentDescription = hint,
            resourceId = resourceId,
            bounds = bounds,
            isClickable = true,
            isEnabled = true,
            packageName = packageName
        )
    }
}
