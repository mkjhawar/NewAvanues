package com.augmentalis.avanues.avamagic.ui.core.form

import com.augmentalis.avanues.avamagic.components.core.*

/**
 * IconPicker Component
 *
 * An icon selection component with search, categories, and preset icon libraries.
 * Supports Material Icons, Font Awesome, SF Symbols, and custom icon sets.
 *
 * Features:
 * - Search with fuzzy matching
 * - Category filtering
 * - Multiple icon libraries
 * - Recent icons tracking
 * - Favorites
 * - Grid or list display
 * - Icon preview
 *
 * Platform mappings:
 * - Android: Material Icons library
 * - iOS: SF Symbols
 * - Web: Font Awesome or custom icon fonts
 *
 * Usage:
 * ```kotlin
 * // Simple icon picker
 * IconPickerComponent(
 *     value = "home",
 *     label = "Choose Icon",
 *     icons = IconLibrary.MATERIAL_COMMON,
 *     onIconChanged = { icon ->
 *         updateIcon(icon)
 *     }
 * )
 *
 * // Full icon picker with search
 * IconPickerComponent(
 *     value = "favorite",
 *     library = IconLibrary.MaterialIcons,
 *     showSearch = true,
 *     showCategories = true,
 *     showRecent = true,
 *     categories = listOf("Action", "Content", "Navigation"),
 *     onIconChanged = { icon ->
 *         saveIcon(icon)
 *     }
 * )
 * ```
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
data class IconPickerComponent(
    val value: String = "",
    val label: String? = null,
    val library: IconLibrary = IconLibrary.MaterialIcons,
    val icons: List<IconData> = IconLibraryPresets.MATERIAL_COMMON,
    val categories: List<String> = emptyList(),
    val showSearch: Boolean = true,
    val showCategories: Boolean = false,
    val showRecent: Boolean = false,
    val showFavorites: Boolean = false,
    val recentIcons: List<String> = emptyList(),
    val favoriteIcons: List<String> = emptyList(),
    val maxRecent: Int = 20,
    val gridColumns: Int = 6,
    val iconSize: IconSize = IconSize.Medium,
    val placeholder: String = "search...",
    val helperText: String? = null,
    val errorText: String? = null,
    val required: Boolean = false,
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onIconChanged: ((String) -> Unit)? = null,
    val onCategoryChanged: ((String) -> Unit)? = null
) {
    init {
        require(icons.isNotEmpty()) { "IconPicker must have at least one icon" }
        require(gridColumns > 0) { "gridColumns must be greater than 0" }
        require(maxRecent > 0) { "maxRecent must be greater than 0" }

        // Validate value exists in icons if not empty
        if (value.isNotBlank()) {
            require(icons.any { it.name == value }) {
                "Selected icon '$value' must be in the icons list"
            }
        }

        // Validate recent icons
        recentIcons.forEach { icon ->
            require(icons.any { it.name == icon }) {
                "Recent icon '$icon' must be in the icons list"
            }
        }

        // Validate favorite icons
        favoriteIcons.forEach { icon ->
            require(icons.any { it.name == icon }) {
                "Favorite icon '$icon' must be in the icons list"
            }
        }
    }

}

/**
 * Icon data
 */
data class IconData(
    val name: String,
    val label: String = name,
    val category: String = "Other",
    val tags: List<String> = emptyList(),
    val codepoint: String? = null
) {
    init {
        require(name.isNotBlank()) { "Icon name cannot be blank" }
        require(label.isNotBlank()) { "Icon label cannot be blank" }
    }
}

/**
 * Icon library type
 */
enum class IconLibrary {
    /**
     * Material Icons (~2,400 icons)
     */
    MaterialIcons,

    /**
     * Font Awesome (~1,500 icons)
     */
    FontAwesome,

    /**
     * SF Symbols (iOS only, ~4,000 symbols)
     */
    SFSymbols,

    /**
     * Custom icon set
     */
    Custom
}

/**
 * Icon size options
 */
enum class IconSize {
    Small,
    Medium,
    Large
}

/**
 * Common icon library presets
 */
object IconLibraryPresets {
    /**
     * Most common Material Icons (50 icons)
     */
    val MATERIAL_COMMON = listOf(
        IconData("home", "Home", "Action"),
        IconData("favorite", "Favorite", "Action"),
        IconData("star", "Star", "Action"),
        IconData("settings", "Settings", "Action"),
        IconData("search", "Search", "Action"),
        IconData("menu", "Menu", "Navigation"),
        IconData("close", "Close", "Navigation"),
        IconData("arrow_back", "Back", "Navigation"),
        IconData("arrow_forward", "Forward", "Navigation"),
        IconData("more_vert", "More", "Navigation"),
        IconData("person", "Person", "Social"),
        IconData("group", "Group", "Social"),
        IconData("share", "Share", "Social"),
        IconData("email", "Email", "Communication"),
        IconData("phone", "Phone", "Communication"),
        IconData("chat", "Chat", "Communication"),
        IconData("notifications", "Notifications", "Communication"),
        IconData("edit", "Edit", "Content"),
        IconData("delete", "Delete", "Content"),
        IconData("add", "Add", "Content"),
        IconData("remove", "Remove", "Content"),
        IconData("save", "Save", "Content"),
        IconData("check", "Check", "Action"),
        IconData("cancel", "Cancel", "Action"),
        IconData("info", "Info", "Action"),
        IconData("warning", "Warning", "Action"),
        IconData("error", "Error", "Action"),
        IconData("help", "Help", "Action"),
        IconData("download", "Download", "File"),
        IconData("upload", "Upload", "File"),
        IconData("folder", "Folder", "File"),
        IconData("file", "File", "File"),
        IconData("image", "Image", "File"),
        IconData("video", "Video", "File"),
        IconData("audio", "Audio", "File"),
        IconData("calendar", "Calendar", "Action"),
        IconData("clock", "Clock", "Action"),
        IconData("location", "Location", "Maps"),
        IconData("map", "Map", "Maps"),
        IconData("shopping_cart", "Cart", "Action"),
        IconData("payment", "Payment", "Action"),
        IconData("lock", "Lock", "Action"),
        IconData("unlock", "Unlock", "Action"),
        IconData("visibility", "Visible", "Action"),
        IconData("visibility_off", "Hidden", "Action"),
        IconData("thumb_up", "Like", "Action"),
        IconData("thumb_down", "Dislike", "Action"),
        IconData("refresh", "Refresh", "Action"),
        IconData("sync", "Sync", "Action"),
        IconData("print", "Print", "Action")
    )

    /**
     * Material Icons - Navigation category
     */
    val MATERIAL_NAVIGATION = listOf(
        IconData("menu", "Menu", "Navigation"),
        IconData("close", "Close", "Navigation"),
        IconData("arrow_back", "Back", "Navigation"),
        IconData("arrow_forward", "Forward", "Navigation"),
        IconData("arrow_upward", "Up", "Navigation"),
        IconData("arrow_downward", "Down", "Navigation"),
        IconData("chevron_left", "Chevron Left", "Navigation"),
        IconData("chevron_right", "Chevron Right", "Navigation"),
        IconData("expand_more", "Expand More", "Navigation"),
        IconData("expand_less", "Expand Less", "Navigation"),
        IconData("more_vert", "More Vertical", "Navigation"),
        IconData("more_horiz", "More Horizontal", "Navigation"),
        IconData("first_page", "First Page", "Navigation"),
        IconData("last_page", "Last Page", "Navigation"),
        IconData("fullscreen", "Fullscreen", "Navigation"),
        IconData("fullscreen_exit", "Exit Fullscreen", "Navigation")
    )

    /**
     * Material Icons - Action category
     */
    val MATERIAL_ACTION = listOf(
        IconData("home", "Home", "Action"),
        IconData("favorite", "Favorite", "Action"),
        IconData("star", "Star", "Action"),
        IconData("settings", "Settings", "Action"),
        IconData("search", "Search", "Action"),
        IconData("check", "Check", "Action"),
        IconData("cancel", "Cancel", "Action"),
        IconData("done", "Done", "Action"),
        IconData("info", "Info", "Action"),
        IconData("warning", "Warning", "Action"),
        IconData("error", "Error", "Action"),
        IconData("help", "Help", "Action"),
        IconData("calendar", "Calendar", "Action"),
        IconData("clock", "Clock", "Action"),
        IconData("shopping_cart", "Shopping Cart", "Action"),
        IconData("payment", "Payment", "Action"),
        IconData("lock", "Lock", "Action"),
        IconData("unlock", "Unlock", "Action"),
        IconData("visibility", "Visibility", "Action"),
        IconData("visibility_off", "Visibility Off", "Action"),
        IconData("thumb_up", "Thumb Up", "Action"),
        IconData("thumb_down", "Thumb Down", "Action"),
        IconData("refresh", "Refresh", "Action"),
        IconData("sync", "Sync", "Action"),
        IconData("print", "Print", "Action")
    )

    /**
     * Font Awesome common icons
     */
    val FONT_AWESOME_COMMON = listOf(
        IconData("fa-home", "Home", "Common"),
        IconData("fa-user", "User", "Common"),
        IconData("fa-heart", "Heart", "Common"),
        IconData("fa-star", "Star", "Common"),
        IconData("fa-search", "Search", "Common"),
        IconData("fa-cog", "Settings", "Common"),
        IconData("fa-bars", "Menu", "Common"),
        IconData("fa-times", "Close", "Common"),
        IconData("fa-envelope", "Email", "Common"),
        IconData("fa-phone", "Phone", "Common"),
        IconData("fa-check", "Check", "Common"),
        IconData("fa-trash", "Delete", "Common"),
        IconData("fa-edit", "Edit", "Common"),
        IconData("fa-download", "Download", "Common"),
        IconData("fa-upload", "Upload", "Common")
    )

    /**
     * SF Symbols common icons (iOS)
     */
    val SF_SYMBOLS_COMMON = listOf(
        IconData("house", "Home", "Common"),
        IconData("person", "Person", "Common"),
        IconData("heart", "Heart", "Common"),
        IconData("star", "Star", "Common"),
        IconData("magnifyingglass", "Search", "Common"),
        IconData("gearshape", "Settings", "Common"),
        IconData("line.3.horizontal", "Menu", "Common"),
        IconData("xmark", "Close", "Common"),
        IconData("envelope", "Email", "Common"),
        IconData("phone", "Phone", "Common"),
        IconData("checkmark", "Check", "Common"),
        IconData("trash", "Delete", "Common"),
        IconData("pencil", "Edit", "Common"),
        IconData("arrow.down", "Download", "Common"),
        IconData("arrow.up", "Upload", "Common")
    )

    /**
     * Get icons for a specific library
     */
    fun forLibrary(library: IconLibrary): List<IconData> {
        return when (library) {
            IconLibrary.MaterialIcons -> MATERIAL_COMMON
            IconLibrary.FontAwesome -> FONT_AWESOME_COMMON
            IconLibrary.SFSymbols -> SF_SYMBOLS_COMMON
            IconLibrary.Custom -> emptyList()
        }
    }

    /**
     * Get icons for a specific category
     */
    fun forCategory(category: String, library: IconLibrary = IconLibrary.MaterialIcons): List<IconData> {
        val allIcons = forLibrary(library)
        return allIcons.filter { it.category.equals(category, ignoreCase = true) }
    }
}
