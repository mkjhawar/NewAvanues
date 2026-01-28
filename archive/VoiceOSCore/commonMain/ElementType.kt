package com.augmentalis.voiceoscore

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
    IMAGE_BUTTON,
    LIST_ITEM,
    MENU,
    TEXT,
    IMAGE,
    CONTAINER,
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
                // ImageButton before Button
                className.contains("ImageButton", ignoreCase = true) -> IMAGE_BUTTON
                className.contains("Button", ignoreCase = true) -> BUTTON
                className.contains("EditText", ignoreCase = true) -> TEXT_FIELD
                className.contains("TextInput", ignoreCase = true) -> TEXT_FIELD
                className.contains("AutoComplete", ignoreCase = true) -> TEXT_FIELD
                className.contains("Spinner", ignoreCase = true) -> DROPDOWN
                className.contains("DropDown", ignoreCase = true) -> DROPDOWN
                className.contains("Tab", ignoreCase = true) -> TAB
                // New types
                className.contains("ListView", ignoreCase = true) -> LIST_ITEM
                className.contains("RecyclerView", ignoreCase = true) -> LIST_ITEM
                className.contains("Menu", ignoreCase = true) -> MENU
                className.contains("PopupMenu", ignoreCase = true) -> MENU
                className.contains("ImageView", ignoreCase = true) -> IMAGE
                className.contains("TextView", ignoreCase = true) -> TEXT
                className.contains("Layout", ignoreCase = true) -> CONTAINER
                className.contains("ViewGroup", ignoreCase = true) -> CONTAINER
                className.contains("FrameLayout", ignoreCase = true) -> CONTAINER
                className.contains("LinearLayout", ignoreCase = true) -> CONTAINER
                className.contains("RelativeLayout", ignoreCase = true) -> CONTAINER
                className.contains("ConstraintLayout", ignoreCase = true) -> CONTAINER
                else -> OTHER
            }
        }
    }
}
