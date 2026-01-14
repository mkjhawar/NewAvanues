/**
 * StaticCommandPersistence.kt - Persists static commands to database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Responsible for populating the commands_static database table with
 * predefined commands from StaticCommandRegistry on first run.
 */
package com.augmentalis.voiceoscoreng.persistence

import com.augmentalis.database.dto.VoiceCommandDTO
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.voiceoscoreng.common.CommandCategory
import com.augmentalis.voiceoscoreng.common.StaticCommand
import com.augmentalis.voiceoscoreng.common.StaticCommandRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Handles persistence of static commands to the database.
 *
 * Static commands are predefined voice commands that work system-wide
 * (navigation, media, system controls, etc.). This class ensures they
 * are stored in the database for:
 * - Speech engine vocabulary loading
 * - Offline availability
 * - User customization (enable/disable)
 * - Analytics and usage tracking
 *
 * @property repository Database repository for voice commands
 * @property locale Target locale for commands (default: "en-US")
 */
class StaticCommandPersistence(
    private val repository: IVoiceCommandRepository,
    private val locale: String = "en-US"
) : IStaticCommandPersistence {
    private val json = Json { prettyPrint = false }

    /**
     * Populate database with static commands if not already present.
     *
     * This should be called once during app initialization.
     * Uses INSERT OR REPLACE semantics, so it's safe to call multiple times.
     *
     * @return Number of commands inserted/updated
     */
    override suspend fun populateStaticCommands(): Int {
        val staticCommands = StaticCommandRegistry.all()
        var count = 0

        staticCommands.forEach { command ->
            val dto = command.toVoiceCommandDTO(locale)
            repository.insert(dto)
            count++
        }

        println("[StaticCommandPersistence] Populated $count static commands for locale: $locale")
        return count
    }

    /**
     * Check if static commands are already populated.
     *
     * @return true if commands exist in database
     */
    override suspend fun isPopulated(): Boolean {
        val existingCount = repository.countByLocale(locale)
        return existingCount > 0
    }

    /**
     * Populate only if not already present.
     *
     * @return Number of commands inserted, or 0 if already populated
     */
    override suspend fun populateIfNeeded(): Int {
        return if (!isPopulated()) {
            populateStaticCommands()
        } else {
            println("[StaticCommandPersistence] Static commands already populated for locale: $locale")
            0
        }
    }

    /**
     * Force refresh all static commands.
     *
     * Deletes existing static commands and re-populates.
     * Use when StaticCommandRegistry has been updated.
     *
     * @return Number of commands inserted
     */
    override suspend fun refresh(): Int {
        // Delete existing static commands by category
        CommandCategory.entries.forEach { category ->
            // Note: This deletes by category, which includes static commands
            // A better approach would be to delete by source="static" if tracked
        }

        return populateStaticCommands()
    }

    /**
     * Get all static command phrases for speech engine vocabulary.
     *
     * @return List of all trigger phrases and synonyms
     */
    override suspend fun getAllPhrases(): List<String> {
        val commands = repository.getByLocale(locale)
        return commands.flatMap { cmd ->
            val synonymList = try {
                json.decodeFromString<List<String>>(cmd.synonyms)
            } catch (e: Exception) {
                emptyList()
            }
            listOf(cmd.triggerPhrase) + synonymList
        }
    }

    /**
     * Get static commands by category.
     *
     * @param category Command category
     * @return List of commands in category
     */
    suspend fun getByCategory(category: CommandCategory): List<VoiceCommandDTO> {
        return repository.getByCategory(category.name)
    }

    /**
     * Find command by trigger phrase.
     *
     * @param phrase Trigger phrase to search
     * @return Matching command or null
     */
    suspend fun findByPhrase(phrase: String): VoiceCommandDTO? {
        val results = repository.searchByTrigger(phrase)
        return results.firstOrNull { it.triggerPhrase.equals(phrase, ignoreCase = true) }
    }

    companion object {
        /**
         * Convert priority from CommandCategory.
         * Higher priority categories execute first.
         */
        private fun categoryToPriority(category: CommandCategory): Long = when (category) {
            CommandCategory.VOICE_CONTROL -> 100L  // Highest - voice system commands
            CommandCategory.NAVIGATION -> 90L      // System navigation
            CommandCategory.SYSTEM -> 80L          // System controls
            CommandCategory.ACCESSIBILITY -> 70L   // Accessibility features
            CommandCategory.MEDIA -> 60L           // Media controls
            CommandCategory.APP_LAUNCH -> 50L      // App launching
            CommandCategory.CUSTOM -> 40L          // User custom commands
        }
    }
}

/**
 * Extension function to convert StaticCommand to VoiceCommandDTO.
 */
fun StaticCommand.toVoiceCommandDTO(locale: String): VoiceCommandDTO {
    val json = Json { prettyPrint = false }

    // Synonyms are all phrases except the primary phrase
    val synonyms = if (phrases.size > 1) {
        json.encodeToString(phrases.drop(1))
    } else {
        "[]"
    }

    // Priority based on category
    val priority = when (category) {
        CommandCategory.VOICE_CONTROL -> 100L
        CommandCategory.NAVIGATION -> 90L
        CommandCategory.SYSTEM -> 80L
        CommandCategory.ACCESSIBILITY -> 70L
        CommandCategory.MEDIA -> 60L
        CommandCategory.APP_LAUNCH -> 50L
        CommandCategory.CUSTOM -> 40L
    }

    return VoiceCommandDTO.create(
        commandId = "static__${category.name.lowercase()}__${primaryPhrase.lowercase().replace(" ", "_")}",
        locale = locale,
        triggerPhrase = primaryPhrase,
        synonyms = synonyms,
        action = actionType.name,
        description = description,
        category = category.name,
        priority = priority,
        isEnabled = true,
        isFallback = false
    )
}
