package com.augmentalis.universal.assetmanager

/**
 * Built-in asset libraries that come with the Asset Management System
 *
 * This provides a curated selection of commonly used icons from Material Design
 * that can be used immediately without additional configuration.
 */
object BuiltInLibraries {

    /**
     * Material Icons library with a curated selection of commonly used icons
     *
     * Icons are provided in SVG format compatible with both light and dark themes.
     * All icons follow Material Design guidelines.
     */
    val MaterialIcons = IconLibrary(
        id = "material-icons",
        name = "Material Icons",
        version = "1.0.0",
        description = "Curated selection of Material Design icons for common UI needs",
        icons = listOf(
            // Navigation Icons
            createMaterialIcon(
                id = "home",
                name = "Home",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>""",
                category = "Navigation",
                tags = listOf("house", "building", "main", "residence")
            ),
            createMaterialIcon(
                id = "menu",
                name = "Menu",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>""",
                category = "Navigation",
                tags = listOf("hamburger", "list", "options")
            ),
            createMaterialIcon(
                id = "arrow-back",
                name = "Arrow Back",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>""",
                category = "Navigation",
                tags = listOf("left", "previous", "return")
            ),
            createMaterialIcon(
                id = "arrow-forward",
                name = "Arrow Forward",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>""",
                category = "Navigation",
                tags = listOf("right", "next", "continue")
            ),
            createMaterialIcon(
                id = "close",
                name = "Close",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>""",
                category = "Navigation",
                tags = listOf("cancel", "exit", "dismiss", "x")
            ),

            // Action Icons
            createMaterialIcon(
                id = "settings",
                name = "Settings",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 00.12-.61l-1.92-3.32a.488.488 0 00-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94L14.4 2.81a.488.488 0 00-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58a.49.49 0 00-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>""",
                category = "Action",
                tags = listOf("preferences", "config", "options", "gear")
            ),
            createMaterialIcon(
                id = "search",
                name = "Search",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>""",
                category = "Action",
                tags = listOf("find", "magnify", "lookup")
            ),
            createMaterialIcon(
                id = "favorite",
                name = "Favorite",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>""",
                category = "Action",
                tags = listOf("heart", "like", "love")
            ),
            createMaterialIcon(
                id = "delete",
                name = "Delete",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>""",
                category = "Action",
                tags = listOf("remove", "trash", "bin")
            ),
            createMaterialIcon(
                id = "add",
                name = "Add",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>""",
                category = "Action",
                tags = listOf("plus", "create", "new")
            ),

            // Communication Icons
            createMaterialIcon(
                id = "email",
                name = "Email",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>""",
                category = "Communication",
                tags = listOf("mail", "message", "inbox")
            ),
            createMaterialIcon(
                id = "phone",
                name = "Phone",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>""",
                category = "Communication",
                tags = listOf("call", "telephone", "contact")
            ),
            createMaterialIcon(
                id = "chat",
                name = "Chat",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z"/></svg>""",
                category = "Communication",
                tags = listOf("message", "conversation", "talk")
            ),

            // User/Person Icons
            createMaterialIcon(
                id = "person",
                name = "Person",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>""",
                category = "User",
                tags = listOf("user", "avatar", "profile", "account")
            ),
            createMaterialIcon(
                id = "group",
                name = "Group",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>""",
                category = "User",
                tags = listOf("people", "users", "team", "group")
            ),
            createMaterialIcon(
                id = "account-circle",
                name = "Account Circle",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"/></svg>""",
                category = "User",
                tags = listOf("profile", "avatar", "user")
            ),

            // File/Content Icons
            createMaterialIcon(
                id = "description",
                name = "Description",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>""",
                category = "File",
                tags = listOf("document", "file", "text")
            ),
            createMaterialIcon(
                id = "folder",
                name = "Folder",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>""",
                category = "File",
                tags = listOf("directory", "files", "storage")
            ),
            createMaterialIcon(
                id = "image",
                name = "Image",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>""",
                category = "File",
                tags = listOf("photo", "picture", "media")
            ),

            // Alert/Status Icons
            createMaterialIcon(
                id = "check-circle",
                name = "Check Circle",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>""",
                category = "Status",
                tags = listOf("success", "done", "complete")
            ),
            createMaterialIcon(
                id = "error",
                name = "Error",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>""",
                category = "Status",
                tags = listOf("warning", "alert", "danger")
            ),
            createMaterialIcon(
                id = "info",
                name = "Info",
                svg = """<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>""",
                category = "Status",
                tags = listOf("information", "help", "details")
            )
        ),
        metadata = mapOf(
            "author" to "Google",
            "license" to "Apache-2.0",
            "source" to "Material Design Icons",
            "website" to "https://material.io/icons"
        )
    )

    /**
     * Register all built-in libraries with the AssetManager
     */
    suspend fun registerAll() {
        AssetManager.registerIconLibrary(MaterialIcons, persist = false)
    }

    /**
     * Helper function to create a Material Design icon
     */
    private fun createMaterialIcon(
        id: String,
        name: String,
        svg: String,
        category: String,
        tags: List<String>
    ): Icon {
        return Icon(
            id = id,
            name = name,
            svg = svg,
            png = null, // SVG only for now, PNG variants can be generated if needed
            tags = tags,
            category = category,
            keywords = listOf(id) + tags
        )
    }

    /**
     * Get a list of all categories in built-in libraries
     */
    fun getCategories(): List<String> {
        return MaterialIcons.icons
            .mapNotNull { it.category }
            .distinct()
            .sorted()
    }

    /**
     * Get icons by category from built-in libraries
     */
    fun getIconsByCategory(category: String): List<Icon> {
        return MaterialIcons.icons.filter { it.category == category }
    }

    /**
     * Get total number of built-in icons
     */
    fun getIconCount(): Int {
        return MaterialIcons.icons.size
    }

    /**
     * Example usage documentation
     */
    object Examples {
        /**
         * Example: Register and use built-in icons
         */
        suspend fun registerAndUse() {
            // Register all built-in libraries
            registerAll()

            // Search for icons
            val homeIcon = AssetManager.getIcon("material-icons:home")

            // Use in UI components
            // Icon("material-icons:home")
        }

        /**
         * Example: Browse available icons
         */
        suspend fun browseIcons() {
            registerAll()

            // Get all categories
            val categories = getCategories()
            println("Available categories: $categories")

            // Get icons in a specific category
            val navigationIcons = getIconsByCategory("Navigation")
            println("Navigation icons: ${navigationIcons.map { it.name }}")

            // Search for icons
            val searchResults = AssetManager.searchIcons(
                query = "person",
                libraryIds = setOf("material-icons")
            )
            println("Found ${searchResults.size} person-related icons")
        }
    }
}
