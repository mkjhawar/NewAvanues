/**
 * HelpMenuHandler.kt - Handles help menu and command discovery
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 * Updated: 2026-01-29 - Integrated with comprehensive HelpCommandDataProvider
 *
 * KMP handler for displaying available voice commands and help information.
 * Supports command search and category-based help organization.
 *
 * ## Usage:
 * ```kotlin
 * val handler = HelpMenuHandler()
 *
 * // Show all help
 * val result = handler.handleCommand("help")
 *
 * // Search commands
 * val searchResult = handler.handleCommand("help volume")
 *
 * // Get rich help data for UI
 * val richCategories = handler.getRichCategories()
 * ```
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.help.HelpCommandDataProvider
import com.augmentalis.voiceoscore.help.HelpCategory as RichHelpCategory
import com.augmentalis.voiceoscore.help.HelpCommand as RichHelpCommand

/**
 * Represents a category of help commands (legacy format).
 *
 * @param name The category name (e.g., "Navigation", "Selection")
 * @param commands List of commands in this category
 */
data class HelpCategory(
    val name: String,
    val commands: List<HelpCommand>
)

/**
 * Represents a single help command entry (legacy format).
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
 *
 * This handler uses HelpCommandDataProvider for comprehensive command data
 * while maintaining backward compatibility with the legacy HelpCategory/HelpCommand format.
 */
class HelpMenuHandler {

    private val customCategories = mutableListOf<HelpCategory>()

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
     * @return HelpResult with all categories (legacy format)
     */
    fun showAllHelp(): HelpResult {
        return HelpResult(success = true, categories = getCategories())
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
        // Use rich search from provider
        val richResults = HelpCommandDataProvider.searchCommands(query)

        if (richResults.isEmpty()) {
            return HelpResult(success = false, error = "No help found for: $query")
        }

        // Convert to legacy format
        val results = richResults.map { it.toLegacyCommand() }
        return HelpResult(success = true, searchResults = results)
    }

    /**
     * Registers a custom help category.
     *
     * Allows extensions to add their own command help.
     *
     * @param category The category to register
     */
    fun registerCategory(category: HelpCategory) {
        customCategories.add(category)
    }

    /**
     * Gets all registered help categories (legacy format).
     *
     * Combines built-in categories from HelpCommandDataProvider with custom categories.
     *
     * @return Immutable list of all categories
     */
    fun getCategories(): List<HelpCategory> {
        val builtInCategories = HelpCommandDataProvider.getCategories().map { it.toLegacyCategory() }
        return builtInCategories + customCategories
    }

    /**
     * Gets the count of registered categories.
     *
     * @return Number of categories
     */
    fun getCategoryCount(): Int = HelpCommandDataProvider.getCategories().size + customCategories.size

    // ═══════════════════════════════════════════════════════════════════
    // Rich Data Access (for modern UI)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Gets all categories with rich data (for modern UI).
     *
     * Returns the full HelpCategory data including icons, colors, and variations.
     *
     * @return List of rich help categories
     */
    fun getRichCategories(): List<RichHelpCategory> = HelpCommandDataProvider.getCategories()

    /**
     * Gets quick reference data for table view.
     *
     * @return List of quick reference entries
     */
    fun getQuickReference() = HelpCommandDataProvider.getQuickReference()

    /**
     * Gets total command count.
     */
    fun getTotalCommandCount(): Int = HelpCommandDataProvider.getTotalCommandCount()

    /**
     * Searches commands with rich data.
     *
     * @param query Search query
     * @return List of matching rich commands
     */
    fun searchRichCommands(query: String): List<RichHelpCommand> =
        HelpCommandDataProvider.searchCommands(query)
}

// ═══════════════════════════════════════════════════════════════════
// Extension Functions for Format Conversion
// ═══════════════════════════════════════════════════════════════════

/**
 * Convert rich HelpCategory to legacy format.
 */
private fun RichHelpCategory.toLegacyCategory(): HelpCategory {
    return HelpCategory(
        name = this.title,
        commands = this.commands.map { it.toLegacyCommand() }
    )
}

/**
 * Convert rich HelpCommand to legacy format.
 */
private fun RichHelpCommand.toLegacyCommand(): HelpCommand {
    return HelpCommand(
        phrase = this.primaryPhrase,
        description = this.description,
        examples = this.variations
    )
}
