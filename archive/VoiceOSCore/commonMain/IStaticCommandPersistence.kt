/**
 * IStaticCommandPersistence.kt - Interface for static command persistence
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Platform-agnostic interface for persisting static commands.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandCategory

/**
 * Interface for static command persistence operations.
 *
 * Platform implementations handle the actual database storage:
 * - Android: SQLDelight via VoiceOS database module
 * - iOS: Core Data or SQLite
 * - Desktop: SQLite or file-based storage
 */
interface IStaticCommandPersistence {

    /**
     * Populate database with static commands.
     *
     * @return Number of commands inserted
     */
    suspend fun populateStaticCommands(): Int

    /**
     * Check if static commands are already populated.
     *
     * @return true if commands exist in database
     */
    suspend fun isPopulated(): Boolean

    /**
     * Populate only if not already present.
     *
     * @return Number of commands inserted, or 0 if already populated
     */
    suspend fun populateIfNeeded(): Int

    /**
     * Force refresh all static commands.
     *
     * @return Number of commands inserted
     */
    suspend fun refresh(): Int

    /**
     * Get all static command phrases for speech engine vocabulary.
     *
     * @return List of all trigger phrases and synonyms
     */
    suspend fun getAllPhrases(): List<String>

    /**
     * Get a static command by its ID.
     *
     * @param commandId The command identifier (e.g., "OPEN_APP", "SCROLL_DOWN")
     * @return StaticCommandMatch if found, null otherwise
     */
    suspend fun getCommandById(commandId: String): StaticCommandMatch?

    /**
     * Get all synonyms for a command ID.
     *
     * @param commandId The command identifier
     * @return List of synonyms including the trigger phrase, or empty if not found
     */
    suspend fun getSynonymsForCommand(commandId: String): List<String>
}

/**
 * Result of static command lookup.
 */
data class StaticCommandMatch(
    val commandId: String,
    val triggerPhrase: String,
    val action: String,
    val category: CommandCategory,
    val synonyms: List<String>
)
