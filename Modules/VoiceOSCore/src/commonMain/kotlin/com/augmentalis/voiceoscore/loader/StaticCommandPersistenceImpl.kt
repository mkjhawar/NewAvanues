/**
 * StaticCommandPersistenceImpl.kt - KMP implementation of IStaticCommandPersistence
 *
 * Loads VOS seed files, parses them with VosParser, and persists to SQLDelight
 * via voiceCommandQueries. Uses a fileReader lambda for platform-agnostic file I/O.
 *
 * This replaces the two broken loading paths:
 * - Path A (KMP): VoiceOSCore.initialize() → this class
 * - Path B (Android): CommandManager → CommandLoader (now handled by CommandManager.initialize() separately)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.loader

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.voiceoscore.CommandCategory
import com.augmentalis.voiceoscore.IStaticCommandPersistence
import com.augmentalis.voiceoscore.StaticCommandMatch
import com.augmentalis.voiceoscore.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * KMP implementation of [IStaticCommandPersistence].
 *
 * Uses raw SQLDelight queries directly — no adapter indirection.
 * Version tracking via database_version table prevents redundant reloads.
 *
 * @param database The shared VoiceOSDatabase instance
 * @param fileReader Platform-specific file reader lambda. Takes a relative asset path
 *   (e.g., "localization/commands/en-US.app.vos") and returns file contents or null.
 * @param fallbackLocale The locale loaded as fallback (isFallback=true). Default: "en-US"
 */
class StaticCommandPersistenceImpl(
    private val database: VoiceOSDatabase,
    private val fileReader: (String) -> String?,
    private val fallbackLocale: String = "en-US"
) : IStaticCommandPersistence {

    companion object {
        private const val VERSION = "2.1"
        private const val COMMANDS_DIR = "localization/commands"
    }

    private val voiceCommandQueries get() = database.voiceCommandQueries
    private val databaseVersionQueries get() = database.databaseVersionQueries

    override suspend fun populateStaticCommands(): Int = withContext(Dispatchers.Default) {
        val totalInserted = loadLocale(fallbackLocale, isFallback = true)

        // Load system locale if different from fallback
        val systemLocale = getSystemLocale()
        val additionalInserted = if (systemLocale != fallbackLocale) {
            loadLocale(systemLocale, isFallback = false)
        } else 0

        val total = totalInserted + additionalInserted

        // Save version
        val now = currentTimeMillis()
        databaseVersionQueries.setVersion(
            json_version = VERSION,
            loaded_at = now,
            command_count = total.toLong(),
            locales = if (systemLocale != fallbackLocale) "$fallbackLocale,$systemLocale" else fallbackLocale
        )

        total
    }

    override suspend fun isPopulated(): Boolean = withContext(Dispatchers.Default) {
        voiceCommandQueries.countByLocale(fallbackLocale).executeAsOne() > 0
    }

    override suspend fun populateIfNeeded(): Int = withContext(Dispatchers.Default) {
        // Check version — if matches, skip
        val currentVersion = try {
            databaseVersionQueries.getJsonVersion().executeAsOneOrNull()
        } catch (_: Exception) {
            null
        }

        if (currentVersion == VERSION && isPopulated()) {
            return@withContext 0
        }

        populateStaticCommands()
    }

    override suspend fun refresh(): Int = withContext(Dispatchers.Default) {
        // Clear version to force reload
        databaseVersionQueries.clearVersion()
        voiceCommandQueries.deleteAllCommands()
        populateStaticCommands()
    }

    override suspend fun getAllPhrases(): List<String> = withContext(Dispatchers.Default) {
        val commands = voiceCommandQueries.getAllCommands().executeAsList()
        buildList {
            for (cmd in commands) {
                add(cmd.trigger_phrase)
                val synonyms = VosParser.parseSynonymsJson(cmd.synonyms)
                addAll(synonyms)
            }
        }.distinct()
    }

    override suspend fun getCommandById(commandId: String): StaticCommandMatch? =
        withContext(Dispatchers.Default) {
            // Get the command for fallback locale first, then any locale
            val cmd = voiceCommandQueries.getCommand(commandId, fallbackLocale).executeAsOneOrNull()
                ?: voiceCommandQueries.getCommandsByCommandId(commandId).executeAsList().firstOrNull()
                ?: return@withContext null

            val category = try {
                CommandCategory.valueOf(cmd.category)
            } catch (_: IllegalArgumentException) {
                CommandCategory.CUSTOM
            }

            StaticCommandMatch(
                commandId = cmd.command_id,
                triggerPhrase = cmd.trigger_phrase,
                action = cmd.action,
                category = category,
                synonyms = VosParser.parseSynonymsJson(cmd.synonyms)
            )
        }

    override suspend fun getSynonymsForCommand(commandId: String): List<String> =
        withContext(Dispatchers.Default) {
            val cmd = voiceCommandQueries.getCommand(commandId, fallbackLocale).executeAsOneOrNull()
                ?: return@withContext emptyList()

            buildList {
                add(cmd.trigger_phrase)
                addAll(VosParser.parseSynonymsJson(cmd.synonyms))
            }
        }

    // ═══════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Load both .app.vos and .web.vos for a given locale.
     * Returns total number of commands inserted.
     */
    private fun loadLocale(locale: String, isFallback: Boolean): Int {
        var total = 0
        total += loadVosFile("$COMMANDS_DIR/$locale.app.vos", isFallback)
        total += loadVosFile("$COMMANDS_DIR/$locale.web.vos", isFallback)
        return total
    }

    /**
     * Load a single VOS file and insert its commands into the database.
     */
    private fun loadVosFile(path: String, isFallback: Boolean): Int {
        val content = fileReader(path) ?: return 0

        val result = VosParser.parse(content, isFallback)
        if (result is VosParseResult.Error) {
            println("[StaticCommandPersistence] Parse error for $path: ${result.message}")
            return 0
        }

        val parsed = (result as VosParseResult.Success).commands
        if (parsed.isEmpty()) return 0

        val now = currentTimeMillis()
        database.transaction {
            for (cmd in parsed) {
                voiceCommandQueries.insertCommandFull(
                    command_id = cmd.id,
                    locale = cmd.locale,
                    trigger_phrase = cmd.primaryText,
                    synonyms = VosParser.synonymsToJson(cmd.synonyms),
                    action = cmd.actionType.ifEmpty { cmd.id },
                    description = cmd.description,
                    category = cmd.category,
                    domain = cmd.domain,
                    priority = 50L,
                    is_fallback = if (cmd.isFallback) 1L else 0L,
                    is_enabled = 1L,
                    created_at = now,
                    updated_at = now
                )
            }
        }

        println("[StaticCommandPersistence] Loaded ${parsed.size} commands from $path")
        return parsed.size
    }

    /**
     * Get the system locale in BCP-47 format (e.g., "en-US").
     * Falls back to [fallbackLocale] if detection fails.
     */
    private fun getSystemLocale(): String {
        // In KMP commonMain we don't have direct access to system locale.
        // The fileReader lambda implicitly knows the platform, so we rely
        // on the fallback locale. Platform code can override this by
        // passing the correct locale through configuration.
        return fallbackLocale
    }
}

