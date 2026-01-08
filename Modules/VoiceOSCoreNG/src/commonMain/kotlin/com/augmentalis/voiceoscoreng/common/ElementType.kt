package com.augmentalis.voiceoscoreng.common

/**
 * Element Type - UI element classification for AVU format.
 *
 * Maps Android/iOS class names to semantic element types
 * for compact representation in AVU exports.
 */
enum class ElementType {
    BUTTON,
    TEXT_FIELD,
    CHECKBOX,
    SWITCH,
    DROPDOWN,
    TAB,
    OTHER;

    companion object {
        /**
         * Classify element type from Android/iOS className.
         *
         * @param className Full class name (e.g., "android.widget.Button")
         * @return Classified ElementType
         */
        fun fromClassName(className: String?): ElementType {
            if (className == null) return OTHER

            // Order matters: check more specific types before generic ones
            return when {
                // Toggle must be checked before Button (ToggleButton contains "Button")
                className.contains("Toggle", ignoreCase = true) -> SWITCH
                className.contains("Switch", ignoreCase = true) -> SWITCH
                className.contains("CheckBox", ignoreCase = true) -> CHECKBOX
                className.contains("Button", ignoreCase = true) -> BUTTON
                className.contains("EditText", ignoreCase = true) -> TEXT_FIELD
                className.contains("TextInput", ignoreCase = true) -> TEXT_FIELD
                className.contains("AutoComplete", ignoreCase = true) -> TEXT_FIELD
                className.contains("Spinner", ignoreCase = true) -> DROPDOWN
                className.contains("DropDown", ignoreCase = true) -> DROPDOWN
                className.contains("Tab", ignoreCase = true) -> TAB
                else -> OTHER
            }
        }
    }
}
