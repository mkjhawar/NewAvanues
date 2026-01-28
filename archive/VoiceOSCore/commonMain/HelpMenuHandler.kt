/**
 * HelpMenuHandler.kt - Handles help menu and command discovery
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP handler for displaying available voice commands and help information.
 * Supports command search and category-based help organization.
 */
package com.augmentalis.voiceoscore

/**
 * Represents a category of help commands.
 *
 * @param name The category name (e.g., "Navigation", "Selection")
 * @param commands List of commands in this category
 */
data class HelpCategory(
    val name: String,
    val commands: List<HelpCommand>
)

/**
 * Represents a single help command entry.
 *
 * @param phrase The voice command phrase
 * @param description Human-readable description of what the command does
 * @param examples Optional list of example usages
 */
data class HelpCommand(
    val phrase: String,
    val description: String,
    val examples: List<String> = emptyList()
)

/**
 * Result of a help operation.
 *
 * @param success Whether the operation succeeded
 * @param categories List of help categories (for showAllHelp)
 * @param searchResults List of matching commands (for searchHelp)
 * @param error Error message if operation failed
 */
data class HelpResult(
    val success: Boolean,
    val categories: List<HelpCategory>? = null,
    val searchResults: List<HelpCommand>? = null,
    val error: String? = null
)

/**
 * Handler for help menu and command discovery.
 *
 * Provides users with information about available voice commands.
 * Supports:
 * - Displaying all available commands by category
 * - Searching for specific commands
 * - Registering custom command categories
 */
class HelpMenuHandler {

    private val categories = mutableListOf<HelpCategory>()

    init {
        registerDefaultCategories()
    }

    /**
     * Registers the default help categories with standard voice commands.
     */
    private fun registerDefaultCategories() {
        categories.add(
            HelpCategory(
                name = "Navigation",
                commands = listOf(
                    HelpCommand("go back", "Navigate to previous screen"),
                    HelpCommand("go home", "Return to home screen"),
                    HelpCommand("scroll up/down", "Scroll the current view"),
                    HelpCommand("open [app]", "Open an application by name")
                )
            )
        )

        categories.add(
            HelpCategory(
                name = "Selection",
                commands = listOf(
                    HelpCommand(
                        phrase = "tap [number]",
                        description = "Tap numbered element",
                        examples = listOf("tap 3", "tap five")
                    ),
                    HelpCommand("select all", "Select all text"),
                    HelpCommand("copy", "Copy selected text to clipboard"),
                    HelpCommand("paste", "Paste from clipboard")
                )
            )
        )

        categories.add(
            HelpCategory(
                name = "Cursor",
                commands = listOf(
                    HelpCommand("cursor up/down/left/right", "Move cursor in direction"),
                    HelpCommand("click", "Click at cursor position"),
                    HelpCommand("cursor faster/slower", "Adjust cursor speed")
                )
            )
        )
    }

    /**
     * Handles a help-related voice command.
     *
     * Supported commands:
     * - "help" or "show help" - Shows all help categories
     * - "what can i say" - Shows all help categories
     * - "help [query]" - Searches for commands matching the query
     *
     * @param command The voice command to handle
     * @return HelpResult with categories, search results, or error
     */
    fun handleCommand(command: String): HelpResult {
        val normalizedCommand = command.lowercase().trim()

        return when {
            normalizedCommand == "help" || normalizedCommand == "show help" -> showAllHelp()
            normalizedCommand.startsWith("help ") -> searchHelp(command.substringAfter("help "))
            normalizedCommand == "what can i say" -> showAllHelp()
            else -> HelpResult(success = false, error = "Unknown help command: $command")
        }
    }

    /**
     * Shows all available help categories and commands.
     *
     * @return HelpResult with all categories
     */
    fun showAllHelp(): HelpResult {
        return HelpResult(success = true, categories = categories.toList())
    }

    /**
     * Searches for commands matching the given query.
     *
     * Searches both command phrases and descriptions.
     *
     * @param query The search query
     * @return HelpResult with matching commands or error if none found
     */
    fun searchHelp(query: String): HelpResult {
        val normalizedQuery = query.lowercase().trim()
        val results = mutableListOf<HelpCommand>()

        for (category in categories) {
            for (cmd in category.commands) {
                if (cmd.phrase.contains(normalizedQuery) ||
                    cmd.description.lowercase().contains(normalizedQuery)
                ) {
                    results.add(cmd)
                }
            }
        }

        return if (results.isEmpty()) {
            HelpResult(success = false, error = "No help found for: $query")
        } else {
            HelpResult(success = true, searchResults = results)
        }
    }

    /**
     * Registers a custom help category.
     *
     * Allows extensions to add their own command help.
     *
     * @param category The category to register
     */
    fun registerCategory(category: HelpCategory) {
        categories.add(category)
    }

    /**
     * Gets all registered help categories.
     *
     * @return Immutable list of all categories
     */
    fun getCategories(): List<HelpCategory> = categories.toList()

    /**
     * Gets the count of registered categories.
     *
     * @return Number of categories
     */
    fun getCategoryCount(): Int = categories.size
}
