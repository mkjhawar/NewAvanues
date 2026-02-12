/**
 * StaticCommandRegistry.kt - Predefined static voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Registry of predefined voice commands that work system-wide,
 * independent of the current screen or app context.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType

/**
 * Registry of predefined static voice commands.
 *
 * These commands are always available regardless of screen context.
 * They provide system-level voice control functionality.
 *
 * Categories:
 * - Navigation: back, home, recent apps
 * - Media: play, pause, volume
 * - System: settings, notifications
 * - VoiceOS: mute, wake, dictation
 */
object StaticCommandRegistry {

    // ═══════════════════════════════════════════════════════════════════
    // DB-Loaded Commands (populated from commands_static table at runtime)
    // Falls back to hardcoded lists below when DB not yet loaded
    // ═══════════════════════════════════════════════════════════════════

    @Volatile
    private var _dbCommands: List<StaticCommand>? = null

    /**
     * Initialize registry with commands loaded from the database.
     * Called from CommandManager after CommandLoader seeds the DB.
     * Thread-safe via @Volatile.
     *
     * @param commands List of StaticCommand converted from DB entities
     */
    fun initialize(commands: List<StaticCommand>) {
        _dbCommands = commands
    }

    /**
     * Whether DB commands have been loaded.
     */
    fun isInitialized(): Boolean = _dbCommands != null

    /**
     * Clear DB commands (for testing or forced reload).
     */
    fun reset() {
        _dbCommands = null
    }

    // ═══════════════════════════════════════════════════════════════════
    // Registry Access Methods
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all static commands.
     * Returns DB-loaded commands (sourced from .VOS files via CommandLoader).
     * Returns empty list if DB not yet loaded — callers should ensure
     * CommandLoader.seedFromAssets() runs before voice pipeline starts.
     */
    fun all(): List<StaticCommand> = _dbCommands ?: emptyList()

    /**
     * Get all phrase strings (for speech engine vocabulary)
     */
    fun allPhrases(): List<String> = all().flatMap { it.phrases }

    /**
     * Get commands by category
     */
    fun byCategory(category: CommandCategory): List<StaticCommand> =
        all().filter { it.category == category }

    /**
     * Find command matching phrase
     */
    fun findByPhrase(phrase: String): StaticCommand? {
        val normalized = phrase.lowercase().trim()
        return all().find { cmd ->
            cmd.phrases.any { it.lowercase() == normalized }
        }
    }

    /**
     * Get command count
     */
    val commandCount: Int get() = all().size

    /**
     * Get phrase count
     */
    val phraseCount: Int get() = allPhrases().size

    // ═══════════════════════════════════════════════════════════════════
    // NLU/LLM Integration - QuantizedCommand Export
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all static commands as QuantizedCommand objects for NLU/LLM.
     *
     * Each phrase variant becomes a separate QuantizedCommand with:
     * - targetVuid = null (system commands, no element target)
     * - confidence = 1.0 (always available)
     * - metadata includes category, description, source
     *
     * @return List of QuantizedCommand for all static commands
     */
    fun allAsQuantized(): List<QuantizedCommand> {
        return all().flatMap { it.toQuantizedCommands() }
    }

    /**
     * Get static commands by category as QuantizedCommand.
     *
     * @param category Command category
     * @return List of QuantizedCommand for category
     */
    fun byCategoryAsQuantized(category: CommandCategory): List<QuantizedCommand> {
        return byCategory(category).flatMap { it.toQuantizedCommands() }
    }

    /**
     * Export all commands in AVU CMD format for NLU/LLM.
     *
     * Format: CMD:uuid:trigger:action:element_uuid:confidence
     * Static commands have empty element_uuid (system commands).
     *
     * @return Multi-line string with all commands in CMD format
     */
    fun toAvuFormat(): String {
        return allAsQuantized().joinToString("\n") { it.toCmdLine() }
    }

    /**
     * Get a concise NLU schema describing available commands.
     *
     * Returns a structured format suitable for LLM prompts:
     * - Category grouping
     * - Primary phrase + action type
     * - Description for context
     */
    fun toNluSchema(): String {
        return buildString {
            appendLine("# Static Voice Commands")
            appendLine()

            CommandCategory.entries.forEach { category ->
                val commands = byCategory(category)
                if (commands.isNotEmpty()) {
                    appendLine("## ${category.name}")
                    commands.forEach { cmd ->
                        appendLine("- ${cmd.primaryPhrase}: ${cmd.actionType.name} - ${cmd.description}")
                        if (cmd.phrases.size > 1) {
                            appendLine("  Aliases: ${cmd.phrases.drop(1).joinToString(", ")}")
                        }
                    }
                    appendLine()
                }
            }
        }
    }
}

/**
 * Represents a static/predefined voice command
 */
data class StaticCommand(
    /**
     * Unique command identifier from VOS file (e.g., "nav_back", "browser_refresh").
     * Empty string for hardcoded fallback commands.
     */
    val id: String = "",

    /**
     * Alternative phrases that trigger this command
     */
    val phrases: List<String>,

    /**
     * Action type to execute
     */
    val actionType: CommandActionType,

    /**
     * Command category for organization
     */
    val category: CommandCategory,

    /**
     * Human-readable description
     */
    val description: String,

    /**
     * Additional metadata for command execution
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Primary phrase (first in list)
     */
    val primaryPhrase: String get() = phrases.first()

    /**
     * Convert to QuantizedCommand objects for NLU/LLM.
     *
     * Creates one QuantizedCommand per phrase variant.
     * Static commands have:
     * - targetVuid = null (system command, no element target)
     * - confidence = 1.0 (always available)
     * - uuid = static__{category}__{normalized_phrase}
     *
     * @return List of QuantizedCommand for each phrase
     */
    fun toQuantizedCommands(): List<QuantizedCommand> {
        return phrases.map { phrase ->
            val normalizedPhrase = phrase.lowercase().replace(" ", "_")
            val commandAvid = "static__${category.name.lowercase()}__$normalizedPhrase"

            QuantizedCommand(
                avid = commandAvid,
                phrase = phrase,
                actionType = actionType,
                targetAvid = null, // Static commands have no target element
                confidence = 1.0f, // Static commands are always available
                metadata = metadata + mapOf(
                    "source" to "static",
                    "category" to category.name,
                    "description" to description,
                    "primary_phrase" to primaryPhrase
                )
            )
        }
    }

    /**
     * Convert primary phrase to a single QuantizedCommand.
     *
     * @return QuantizedCommand for primary phrase only
     */
    fun toQuantizedCommand(): QuantizedCommand {
        val normalizedPhrase = primaryPhrase.lowercase().replace(" ", "_")
        val commandAvid = "static__${category.name.lowercase()}__$normalizedPhrase"

        return QuantizedCommand(
            avid = commandAvid,
            phrase = primaryPhrase,
            actionType = actionType,
            targetAvid = null,
            confidence = 1.0f,
            metadata = metadata + mapOf(
                "source" to "static",
                "category" to category.name,
                "description" to description,
                "aliases" to phrases.drop(1).joinToString("|")
            )
        )
    }
}
