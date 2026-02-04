/**
 * ScreenHierarchyTypes.kt - Enums and types for Screen Hierarchy System
 *
 * Supporting types for the Universal Screen Hierarchy System including:
 * - Screen types and classifications
 * - Component roles
 * - Field and action types
 * - Complexity levels
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

/**
 * Screen type classification
 *
 * Categorizes screens by their primary purpose to enable
 * context-aware command processing and AI understanding.
 */
enum class ScreenType(val displayName: String) {
    LOGIN("Login"),
    SIGNUP("Sign Up"),
    CHECKOUT("Checkout"),
    CART("Shopping Cart"),
    SETTINGS("Settings"),
    HOME("Home"),
    SEARCH("Search"),
    PROFILE("Profile"),
    DETAIL("Detail View"),
    LIST("List View"),
    FORM("Form"),
    CONTENT("Content"),
    DIALOG("Dialog"),
    NAVIGATION("Navigation"),
    DASHBOARD("Dashboard"),
    UNKNOWN("Unknown");

    companion object {
        /**
         * Infer screen type from keywords
         */
        fun fromKeywords(keywords: List<String>): ScreenType {
            val keywordSet = keywords.map { it.lowercase() }.toSet()

            return when {
                keywordSet.any { it in setOf("login", "signin", "sign in", "password") } -> LOGIN
                keywordSet.any { it in setOf("signup", "register", "create account") } -> SIGNUP
                keywordSet.any { it in setOf("checkout", "payment", "billing") } -> CHECKOUT
                keywordSet.any { it in setOf("cart", "basket", "bag") } -> CART
                keywordSet.any { it in setOf("settings", "preferences", "configuration") } -> SETTINGS
                keywordSet.any { it in setOf("home", "dashboard", "main") } -> HOME
                keywordSet.any { it in setOf("search", "find", "query") } -> SEARCH
                keywordSet.any { it in setOf("profile", "account", "user") } -> PROFILE
                keywordSet.any { it in setOf("detail", "details", "view") } -> DETAIL
                keywordSet.any { it in setOf("list", "browse", "results") } -> LIST
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Primary action classification
 *
 * Identifies the main user action available on a screen.
 */
enum class PrimaryAction(val displayName: String, val voiceCommand: String) {
    SUBMIT("Submit", "submit"),
    SEARCH("Search", "search"),
    BROWSE("Browse", "browse"),
    PURCHASE("Purchase", "buy"),
    NAVIGATE("Navigate", "go"),
    VIEW("View", "view"),
    EDIT("Edit", "edit"),
    DELETE("Delete", "delete"),
    SHARE("Share", "share"),
    SAVE("Save", "save"),
    CANCEL("Cancel", "cancel"),
    BACK("Go Back", "back"),
    UNKNOWN("Unknown", "");

    companion object {
        /**
         * Determine primary action from available actions
         */
        fun fromActions(actions: List<ActionElement>): PrimaryAction? {
            if (actions.isEmpty()) return null

            // Priority order: Submit > Save > Search > Purchase > Navigate
            return when {
                actions.any { it.actionType == ActionType.SUBMIT } -> SUBMIT
                actions.any { it.actionType == ActionType.SAVE } -> SAVE
                actions.any { it.actionType == ActionType.SEARCH } -> SEARCH
                actions.any { it.actionType == ActionType.PURCHASE } -> PURCHASE
                actions.any { it.actionType == ActionType.NAVIGATE } -> NAVIGATE
                actions.any { it.actionType == ActionType.DELETE } -> DELETE
                else -> VIEW
            }
        }
    }
}

/**
 * Component semantic role
 *
 * Defines the semantic purpose of a UI component.
 */
enum class ComponentRole {
    /** Container for other components */
    CONTAINER,

    /** Action trigger (button, link) */
    ACTION,

    /** Data input (text field, checkbox) */
    INPUT,

    /** Data display (text, image) */
    DISPLAY,

    /** Navigation element (tab, menu) */
    NAVIGATION,

    /** Feedback element (alert, toast) */
    FEEDBACK,

    /** Decorative element */
    DECORATION,

    /** Unknown role */
    UNKNOWN;

    companion object {
        /**
         * Infer role from component type
         */
        fun fromComponentType(type: String): ComponentRole {
            return when (type.lowercase()) {
                "button", "iconbutton", "floatingactionbutton" -> ACTION
                "textfield", "checkbox", "switch", "slider", "radiobutton", "dropdown" -> INPUT
                "text", "image", "icon", "badge", "chip" -> DISPLAY
                "column", "row", "container", "card", "scrollview", "grid", "stack" -> CONTAINER
                "tabs", "bottomnav", "appbar", "drawer", "breadcrumb" -> NAVIGATION
                "alert", "snackbar", "toast", "modal", "dialog" -> FEEDBACK
                "divider", "spacer", "skeleton" -> DECORATION
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Field type for form inputs
 */
enum class FieldType(val displayName: String) {
    TEXT("Text"),
    EMAIL("Email"),
    PASSWORD("Password"),
    NUMBER("Number"),
    PHONE("Phone"),
    URL("URL"),
    DATE("Date"),
    TIME("Time"),
    DATETIME("Date & Time"),
    SEARCH("Search"),
    MULTILINE("Multiline Text"),
    DROPDOWN("Dropdown"),
    CHECKBOX("Checkbox"),
    RADIO("Radio Button"),
    SWITCH("Switch"),
    SLIDER("Slider"),
    COLOR("Color Picker"),
    FILE("File Upload"),
    UNKNOWN("Unknown");

    companion object {
        /**
         * Infer field type from component and hints
         */
        fun fromComponent(
            componentType: String,
            contentDescription: String?,
            placeholder: String?
        ): FieldType {
            val hints = listOfNotNull(contentDescription, placeholder)
                .joinToString(" ")
                .lowercase()

            return when {
                componentType.lowercase() == "checkbox" -> CHECKBOX
                componentType.lowercase() == "switch" -> SWITCH
                componentType.lowercase() == "slider" -> SLIDER
                componentType.lowercase() == "radiobutton" -> RADIO
                componentType.lowercase() == "dropdown" -> DROPDOWN
                hints.contains("email") -> EMAIL
                hints.contains("password") -> PASSWORD
                hints.contains("phone") || hints.contains("tel") -> PHONE
                hints.contains("url") || hints.contains("website") -> URL
                hints.contains("date") -> DATE
                hints.contains("time") -> TIME
                hints.contains("search") -> SEARCH
                hints.contains("number") || hints.contains("amount") -> NUMBER
                else -> TEXT
            }
        }
    }
}

/**
 * Action type classification
 */
enum class ActionType(val displayName: String) {
    SUBMIT("Submit"),
    CANCEL("Cancel"),
    NAVIGATE("Navigate"),
    DELETE("Delete"),
    EDIT("Edit"),
    SAVE("Save"),
    SEARCH("Search"),
    PURCHASE("Purchase"),
    SHARE("Share"),
    DOWNLOAD("Download"),
    UPLOAD("Upload"),
    REFRESH("Refresh"),
    FILTER("Filter"),
    SORT("Sort"),
    CLOSE("Close"),
    EXPAND("Expand"),
    COLLAPSE("Collapse"),
    UNKNOWN("Unknown");

    companion object {
        /**
         * Infer action type from button text
         */
        fun fromButtonText(text: String): ActionType {
            val lowerText = text.lowercase()

            return when {
                lowerText in setOf("submit", "send", "confirm", "ok") -> SUBMIT
                lowerText in setOf("cancel", "dismiss", "no") -> CANCEL
                lowerText in setOf("back", "next", "continue", "go") -> NAVIGATE
                lowerText in setOf("delete", "remove", "trash") -> DELETE
                lowerText in setOf("edit", "modify", "change") -> EDIT
                lowerText in setOf("save", "update") -> SAVE
                lowerText in setOf("search", "find") -> SEARCH
                lowerText in setOf("buy", "purchase", "checkout") -> PURCHASE
                lowerText in setOf("share", "send") -> SHARE
                lowerText in setOf("download", "save as") -> DOWNLOAD
                lowerText in setOf("upload", "attach") -> UPLOAD
                lowerText in setOf("refresh", "reload") -> REFRESH
                lowerText in setOf("filter") -> FILTER
                lowerText in setOf("sort") -> SORT
                lowerText in setOf("close", "exit") -> CLOSE
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Data element type
 */
enum class DataType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    LIST,
    TABLE,
    CHART,
    MAP,
    ICON,
    BADGE,
    UNKNOWN
}

/**
 * Complexity level
 */
enum class ComplexityLevel(val displayName: String, val description: String) {
    SIMPLE(
        "Simple",
        "Few components, shallow hierarchy, minimal interactions"
    ),
    MODERATE(
        "Moderate",
        "Multiple sections, moderate depth, several interactions"
    ),
    COMPLEX(
        "Complex",
        "Many components, deep hierarchy, numerous interactions"
    ),
    VERY_COMPLEX(
        "Very Complex",
        "Highly nested structure, extensive interactions, multiple forms"
    )
}

/**
 * Voice command types
 */
enum class VoiceCommandType(val displayName: String) {
    CLICK("Click"),
    TYPE("Type"),
    SELECT("Select"),
    SCROLL("Scroll"),
    NAVIGATE("Navigate"),
    TOGGLE("Toggle"),
    SUBMIT("Submit"),
    CLEAR("Clear"),
    FOCUS("Focus"),
    EXPAND("Expand"),
    COLLAPSE("Collapse"),
    UNKNOWN("Unknown");

    companion object {
        /**
         * Determine command type from component type
         */
        fun fromComponentType(componentType: String): VoiceCommandType {
            return when (componentType.lowercase()) {
                "button", "iconbutton" -> CLICK
                "textfield" -> TYPE
                "checkbox", "radiobutton", "dropdown" -> SELECT
                "switch" -> TOGGLE
                "scrollview", "list" -> SCROLL
                "tabs", "bottomnav" -> NAVIGATE
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Screen context update reason
 */
enum class ScreenUpdateReason {
    /** Initial screen load */
    INITIAL_LOAD,

    /** Navigation to different screen */
    NAVIGATION,

    /** User interaction changed state */
    USER_INTERACTION,

    /** Data refresh */
    DATA_REFRESH,

    /** Component added/removed */
    STRUCTURAL_CHANGE,

    /** Timer/periodic update */
    PERIODIC_UPDATE,

    /** External trigger */
    EXTERNAL_TRIGGER
}

/**
 * Command resolution confidence
 */
enum class CommandConfidence(val threshold: Float) {
    /** Very high confidence (95%+) */
    VERY_HIGH(0.95f),

    /** High confidence (80-95%) */
    HIGH(0.80f),

    /** Medium confidence (60-80%) */
    MEDIUM(0.60f),

    /** Low confidence (40-60%) */
    LOW(0.40f),

    /** Very low confidence (<40%) */
    VERY_LOW(0.0f);

    companion object {
        /**
         * Get confidence level from score
         */
        fun fromScore(score: Float): CommandConfidence {
            return when {
                score >= VERY_HIGH.threshold -> VERY_HIGH
                score >= HIGH.threshold -> HIGH
                score >= MEDIUM.threshold -> MEDIUM
                score >= LOW.threshold -> LOW
                else -> VERY_LOW
            }
        }
    }
}
