/**
 * HelpCommandData.kt - Data model for voice command help screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-29
 *
 * Provides structured data for the help screen, including command categories,
 * individual commands with variations, and quick reference data.
 *
 * Phase 2 (260211): Commands now derive from StaticCommandRegistry (DB-backed)
 * instead of being hardcoded. Template commands (with [element], [text], etc.)
 * remain static since they document parametric usage patterns, not DB entries.
 */
package com.augmentalis.voiceoscore.help

import com.augmentalis.voiceoscore.CommandCategory
import com.augmentalis.voiceoscore.StaticCommand
import com.augmentalis.voiceoscore.StaticCommandRegistry

/**
 * Represents a single voice command with its variations.
 *
 * @property primaryPhrase The main command phrase (e.g., "scroll down")
 * @property variations Alternative ways to say the same command
 * @property description Brief description of what the command does
 * @property actionResult The result/feedback after executing (e.g., "Scrolls page down")
 */
data class HelpCommand(
    val primaryPhrase: String,
    val variations: List<String> = emptyList(),
    val description: String,
    val actionResult: String
) {
    /**
     * All phrases including primary and variations.
     */
    val allPhrases: List<String>
        get() = listOf(primaryPhrase) + variations
}

/**
 * Represents a category of commands in the help screen.
 *
 * @property id Unique identifier for the category
 * @property title Display title (e.g., "Navigation")
 * @property iconName Icon identifier (Material icon name)
 * @property commands List of commands in this category
 * @property color Optional color for the category card (hex string)
 */
data class HelpCategory(
    val id: String,
    val title: String,
    val iconName: String,
    val commands: List<HelpCommand>,
    val color: String? = null
) {
    val commandCount: Int get() = commands.size

    val previewText: String
        get() = commands.take(3).joinToString(", ") { "\"${it.primaryPhrase}\"" } +
                if (commands.size > 3) "..." else ""
}

/**
 * Quick reference entry for the table view.
 */
data class QuickReferenceEntry(
    val command: String,
    val variations: String,
    val action: String
)

/**
 * Complete help screen data.
 */
data class HelpScreenData(
    val categories: List<HelpCategory>,
    val quickReference: List<QuickReferenceEntry>
)

/**
 * Convert StaticCommand to HelpCommand for help screen display.
 */
private fun StaticCommand.toHelpCommand(): HelpCommand = HelpCommand(
    primaryPhrase = primaryPhrase,
    variations = phrases.drop(1),
    description = description,
    actionResult = description
)

/**
 * Provides all help command data for the help screen.
 *
 * Non-template commands derive from StaticCommandRegistry (DB-backed single source of truth).
 * Template commands (with [element], [text] patterns) remain static — they document
 * parametric usage patterns that don't correspond to fixed DB entries.
 */
object HelpCommandDataProvider {

    // ═══════════════════════════════════════════════════════════════════
    // Template Commands — parametric patterns not in DB
    // ═══════════════════════════════════════════════════════════════════

    private val appControlTemplates = listOf(
        HelpCommand(
            primaryPhrase = "open [app name]",
            variations = listOf("launch [app]", "start [app]"),
            description = "Open any installed app",
            actionResult = "Launches the specified app"
        )
    )

    private val uiInteractionTemplates = listOf(
        HelpCommand(
            primaryPhrase = "click [element]",
            variations = listOf("tap [element]", "press [element]"),
            description = "Tap on an element by name",
            actionResult = "Clicks the specified element"
        ),
        HelpCommand(
            primaryPhrase = "long press [element]",
            variations = listOf("long click [element]", "hold [element]"),
            description = "Long press on element",
            actionResult = "Long presses the element"
        ),
        HelpCommand(
            primaryPhrase = "double tap [element]",
            variations = listOf("double click [element]"),
            description = "Double tap on element",
            actionResult = "Double taps the element"
        ),
        HelpCommand(
            primaryPhrase = "tap [number]",
            variations = listOf("click [number]", "[number]"),
            description = "Tap numbered element",
            actionResult = "Clicks element with that number"
        ),
        HelpCommand(
            primaryPhrase = "expand [element]",
            variations = emptyList(),
            description = "Expand a collapsible section",
            actionResult = "Expands the section"
        ),
        HelpCommand(
            primaryPhrase = "collapse [element]",
            variations = emptyList(),
            description = "Collapse an expanded section",
            actionResult = "Collapses the section"
        ),
        HelpCommand(
            primaryPhrase = "toggle [element]",
            variations = listOf("check [element]", "uncheck [element]"),
            description = "Toggle a checkbox or switch",
            actionResult = "Toggles the element state"
        )
    )

    private val textInputTemplates = listOf(
        HelpCommand(
            primaryPhrase = "type [text]",
            variations = listOf("enter text [text]", "input [text]"),
            description = "Type text into focused field",
            actionResult = "Enters the specified text"
        ),
        HelpCommand(
            primaryPhrase = "clear text",
            variations = listOf("clear all"),
            description = "Clear all text in field",
            actionResult = "Clears the text field"
        ),
        HelpCommand(
            primaryPhrase = "search [query]",
            variations = listOf("find [query]"),
            description = "Search for text",
            actionResult = "Initiates search"
        )
    )

    private val mediaTemplates = listOf(
        HelpCommand(
            primaryPhrase = "set volume [number]",
            variations = emptyList(),
            description = "Set volume to specific level",
            actionResult = "Sets volume to specified %"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category Metadata — static (icons, colors, titles)
    // ═══════════════════════════════════════════════════════════════════

    private data class CategoryMeta(
        val id: String,
        val title: String,
        val iconName: String,
        val color: String
    )

    private val categoryMetaMap = mapOf(
        "navigation" to CategoryMeta("navigation", "Navigation", "navigation", "#4285F4"),
        "app_control" to CategoryMeta("app_control", "App Control", "apps", "#34A853"),
        "ui_interaction" to CategoryMeta("ui_interaction", "UI Interaction", "touch_app", "#FBBC04"),
        "text_input" to CategoryMeta("text_input", "Text Input", "keyboard", "#EA4335"),
        "system" to CategoryMeta("system", "System", "settings", "#9C27B0"),
        "media" to CategoryMeta("media", "Media", "play_circle", "#FF5722"),
        "voiceos" to CategoryMeta("voiceos", "VoiceOS", "mic", "#00BCD4"),
        "web_gestures" to CategoryMeta("web_gestures", "Web Gestures", "gesture", "#E91E63")
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category Builders — derive from StaticCommandRegistry + templates
    // ═══════════════════════════════════════════════════════════════════

    private fun buildNavigationCategory(): HelpCategory {
        val meta = categoryMetaMap["navigation"]!!
        val commands = StaticCommandRegistry.byCategory(CommandCategory.NAVIGATION)
            .map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, commands, meta.color)
    }

    private fun buildAppControlCategory(): HelpCategory {
        val meta = categoryMetaMap["app_control"]!!
        val registryCommands = (
            StaticCommandRegistry.byCategory(CommandCategory.APP_LAUNCH) +
            StaticCommandRegistry.byCategory(CommandCategory.APP_CONTROL)
        ).map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, appControlTemplates + registryCommands, meta.color)
    }

    private fun buildUiInteractionCategory(): HelpCategory {
        val meta = categoryMetaMap["ui_interaction"]!!
        val registryCommands = StaticCommandRegistry.byCategory(CommandCategory.ACCESSIBILITY)
            .map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, uiInteractionTemplates + registryCommands, meta.color)
    }

    private fun buildTextInputCategory(): HelpCategory {
        val meta = categoryMetaMap["text_input"]!!
        val registryCommands = (
            StaticCommandRegistry.byCategory(CommandCategory.TEXT) +
            StaticCommandRegistry.byCategory(CommandCategory.INPUT)
        ).map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, textInputTemplates + registryCommands, meta.color)
    }

    private fun buildSystemCategory(): HelpCategory {
        val meta = categoryMetaMap["system"]!!
        val commands = StaticCommandRegistry.byCategory(CommandCategory.SYSTEM)
            .map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, commands, meta.color)
    }

    private fun buildMediaCategory(): HelpCategory {
        val meta = categoryMetaMap["media"]!!
        val registryCommands = StaticCommandRegistry.byCategory(CommandCategory.MEDIA)
            .map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, registryCommands + mediaTemplates, meta.color)
    }

    private fun buildVoiceOSCategory(): HelpCategory {
        val meta = categoryMetaMap["voiceos"]!!
        val commands = StaticCommandRegistry.byCategory(CommandCategory.VOICE_CONTROL)
            .map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, commands, meta.color)
    }

    private fun buildWebGestureCategory(): HelpCategory {
        val meta = categoryMetaMap["web_gestures"]!!
        val commands = (
            StaticCommandRegistry.byCategory(CommandCategory.BROWSER) +
            StaticCommandRegistry.byCategory(CommandCategory.WEB_GESTURE)
        ).map { it.toHelpCommand() }
        return HelpCategory(meta.id, meta.title, meta.iconName, commands, meta.color)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all help categories.
     * Commands derive from StaticCommandRegistry (DB-backed) + static templates.
     */
    fun getCategories(): List<HelpCategory> = listOf(
        buildNavigationCategory(),
        buildAppControlCategory(),
        buildUiInteractionCategory(),
        buildTextInputCategory(),
        buildSystemCategory(),
        buildMediaCategory(),
        buildVoiceOSCategory(),
        buildWebGestureCategory()
    )

    /**
     * Get quick reference entries for table view.
     */
    fun getQuickReference(): List<QuickReferenceEntry> {
        val allCommands = getCategories().flatMap { it.commands }
        return allCommands.map { cmd ->
            QuickReferenceEntry(
                command = cmd.primaryPhrase,
                variations = cmd.variations.take(2).joinToString(", ").ifEmpty { "-" },
                action = cmd.actionResult
            )
        }
    }

    /**
     * Get complete help screen data.
     */
    fun getHelpScreenData(): HelpScreenData {
        return HelpScreenData(
            categories = getCategories(),
            quickReference = getQuickReference()
        )
    }

    /**
     * Find commands matching a search query.
     */
    fun searchCommands(query: String): List<HelpCommand> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.lowercase().trim()

        return getCategories()
            .flatMap { it.commands }
            .filter { cmd ->
                cmd.primaryPhrase.lowercase().contains(normalizedQuery) ||
                cmd.variations.any { it.lowercase().contains(normalizedQuery) } ||
                cmd.description.lowercase().contains(normalizedQuery)
            }
    }

    /**
     * Get commands by category ID.
     */
    fun getCommandsByCategory(categoryId: String): List<HelpCommand> {
        return getCategories().find { it.id == categoryId }?.commands ?: emptyList()
    }

    /**
     * Get total command count across all categories.
     */
    fun getTotalCommandCount(): Int {
        return getCategories().sumOf { it.commandCount }
    }

    /**
     * Get all phrases for speech engine registration.
     */
    fun getAllPhrases(): List<String> {
        return getCategories()
            .flatMap { it.commands }
            .flatMap { it.allPhrases }
            .distinct()
    }
}
