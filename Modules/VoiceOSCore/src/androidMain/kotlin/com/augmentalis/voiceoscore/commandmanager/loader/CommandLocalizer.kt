/**
 * CommandLocalizer.kt - Manages locale switching and command localization
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.commandmanager.database.CommandDatabase
import com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Helper class for managing command localization and locale switching.
 *
 * Features:
 * - Runtime locale switching
 * - Preload common locales
 * - Export/import user commands
 * - Locale preference persistence
 * - Command resolution with fallback
 *
 * Usage:
 * ```kotlin
 * val localizer = CommandLocalizer.create(context)
 * localizer.setLocale("es-ES")
 * val commands = localizer.getCurrentCommands()
 * ```
 *
 * @property context Application context
 * @property commandLoader Command loader instance
 * @property commandDao Database access for querying commands
 */
class CommandLocalizer(
    private val context: Context,
    private val commandLoader: CommandLoader,
    private val commandDao: com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
) {
    private val _currentLocale = MutableStateFlow(Locale.getDefault().toLanguageTag())
    val currentLocale: Flow<String> = _currentLocale.asStateFlow()

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "CommandLocalizer"
        private const val PREFS_NAME = "command_localizer_prefs"
        private const val KEY_CURRENT_LOCALE = "current_locale"
        private const val FALLBACK_LOCALE = "en-US"

        /**
         * Common locales to preload for better performance.
         */
        val COMMON_LOCALES = listOf(
            "en-US",  // English (United States)
            "es-ES",  // Spanish (Spain)
            "fr-FR",  // French (France)
            "de-DE",  // German (Germany)
            "it-IT",  // Italian (Italy)
            "pt-BR",  // Portuguese (Brazil)
            "ja-JP",  // Japanese (Japan)
            "zh-CN",  // Chinese (Simplified)
            "ko-KR",  // Korean (Korea)
            "ru-RU"   // Russian (Russia)
        )

        /**
         * Create CommandLocalizer instance.
         *
         * @param context Application context
         * @return Configured CommandLocalizer instance
         */
        fun create(context: Context): CommandLocalizer {
            val database = CommandDatabase.getInstance(context)
            val commandLoader = CommandLoader(
                context,
                database.voiceCommandDao(),
                database.databaseVersionDao()
            )
            return CommandLocalizer(context, commandLoader, database.voiceCommandDao())
        }
    }

    init {
        // Restore saved locale preference
        val savedLocale = preferences.getString(KEY_CURRENT_LOCALE, null)
        if (savedLocale != null) {
            _currentLocale.value = savedLocale
            Log.d(TAG, "Restored saved locale: $savedLocale")
        }
    }

    /**
     * Initialize localizer with current locale and fallback.
     *
     * Loads:
     * 1. English fallback
     * 2. Current user locale
     *
     * @throws Exception if initialization fails
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            commandLoader.initializeCommands()
            Log.d(TAG, "CommandLocalizer initialized with locale: ${_currentLocale.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CommandLocalizer", e)
            throw e
        }
    }

    /**
     * Set current locale and load commands if needed.
     *
     * Steps:
     * 1. Validate locale is supported
     * 2. Load locale commands if not already loaded
     * 3. Update current locale state
     * 4. Save preference
     *
     * @param locale Locale code (e.g., "es-ES")
     * @return True if locale was set successfully
     */
    suspend fun setLocale(locale: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Don't reload if already current locale
            if (_currentLocale.value == locale) {
                Log.d(TAG, "Locale $locale already active")
                return@withContext true
            }

            // Try to load locale (will skip if already loaded)
            val isFallback = locale == FALLBACK_LOCALE
            commandLoader.loadLocale(locale, isFallback)

            // Update current locale
            _currentLocale.value = locale

            // Save preference
            preferences.edit().putString(KEY_CURRENT_LOCALE, locale).apply()

            Log.d(TAG, "Locale set to: $locale")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to set locale to $locale", e)
            return@withContext false
        }
    }

    /**
     * Get commands for current locale.
     *
     * @return List of commands for current locale
     */
    suspend fun getCurrentCommands(): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        return@withContext commandDao.getCommandsForLocale(_currentLocale.value)
    }

    /**
     * Get fallback (English) commands.
     *
     * @return List of English commands
     */
    suspend fun getFallbackCommands(): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        return@withContext commandDao.getFallbackCommands()
    }

    /**
     * Resolve command with fallback support.
     *
     * Resolution order:
     * 1. Try current locale
     * 2. Try fallback (English)
     * 3. Return null if not found
     *
     * @param phrase Voice phrase spoken by user
     * @return Matching command or null
     */
    suspend fun resolveCommand(phrase: String): VoiceCommandEntity? = withContext(Dispatchers.IO) {
        val normalizedPhrase = phrase.trim().lowercase()

        // Try current locale first
        val currentCommands = getCurrentCommands()
        val currentMatch = findMatchingCommand(currentCommands, normalizedPhrase)
        if (currentMatch != null) {
            Log.d(TAG, "Resolved command in current locale: ${currentMatch.id}")
            return@withContext currentMatch
        }

        // Fallback to English if different from current locale
        if (_currentLocale.value != FALLBACK_LOCALE) {
            val fallbackCommands = getFallbackCommands()
            val fallbackMatch = findMatchingCommand(fallbackCommands, normalizedPhrase)
            if (fallbackMatch != null) {
                Log.d(TAG, "Resolved command using fallback: ${fallbackMatch.id}")
                return@withContext fallbackMatch
            }
        }

        // No match found
        Log.d(TAG, "No command matched for phrase: $phrase")
        return@withContext null
    }

    /**
     * Find matching command in list by phrase.
     *
     * Matches against:
     * - Primary text (exact match)
     * - Synonyms (exact match)
     *
     * @param commands List of commands to search
     * @param phrase Normalized phrase to match
     * @return First matching command or null
     */
    private fun findMatchingCommand(
        commands: List<VoiceCommandEntity>,
        phrase: String
    ): VoiceCommandEntity? {
        return commands.firstOrNull { command ->
            // Check primary text
            if (command.primaryText.lowercase() == phrase) {
                return@firstOrNull true
            }

            // Check synonyms
            val synonyms = parseSynonyms(command.synonyms)
            synonyms.any { it.lowercase() == phrase }
        }
    }

    /**
     * Parse synonyms from JSON array string.
     *
     * @param synonymsJson JSON array string (e.g., ["next", "advance"])
     * @return List of synonym strings
     */
    private fun parseSynonyms(synonymsJson: String): List<String> {
        return try {
            org.json.JSONArray(synonymsJson).let { jsonArray ->
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing synonyms: $synonymsJson", e)
            emptyList()
        }
    }

    /**
     * Preload common locales for better performance.
     *
     * Loads locales in background:
     * - en-US (always)
     * - Other common locales if available
     *
     * @param locales List of locale codes to preload (default: COMMON_LOCALES)
     */
    suspend fun preloadCommonLocales(
        locales: List<String> = COMMON_LOCALES
    ) = withContext(Dispatchers.IO) {
        try {
            var loadedCount = 0
            var skippedCount = 0

            locales.forEach { locale ->
                try {
                    val isFallback = locale == FALLBACK_LOCALE
                    commandLoader.loadLocale(locale, isFallback)
                    loadedCount++
                    Log.d(TAG, "Preloaded locale: $locale")
                } catch (e: Exception) {
                    skippedCount++
                    Log.d(TAG, "Locale not available, skipped: $locale")
                }
            }

            Log.d(TAG, "Preloaded $loadedCount locales, skipped $skippedCount")

        } catch (e: Exception) {
            Log.e(TAG, "Error preloading locales", e)
        }
    }

    /**
     * Get all available locales that have commands loaded.
     *
     * @return List of locale codes
     */
    suspend fun getAvailableLocales(): List<String> = withContext(Dispatchers.IO) {
        return@withContext commandLoader.getAvailableLocales()
    }

    /**
     * Reload current locale commands from JSON.
     *
     * Use when:
     * - JSON file has been updated
     * - Commands need to be refreshed
     *
     * @return True if reload was successful
     */
    suspend fun reloadCurrentLocale(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val locale = _currentLocale.value
            val isFallback = locale == FALLBACK_LOCALE
            // Delete old commands for this locale first
            commandDao.deleteCommandsForLocale(locale)
            // Then reload
            val result = commandLoader.loadLocale(locale, isFallback)
            Log.d(TAG, "Reloaded current locale: $locale, result: $result")
            result is CommandLoader.LoadResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload current locale", e)
            false
        }
    }

    /**
     * Clear command cache.
     *
     * Note: CommandLoader queries database directly without caching,
     * so this is a no-op. Reserved for future caching implementation.
     */
    fun clearCache() {
        // TODO: Implement caching layer if needed for performance
        Log.d(TAG, "clearCache() called - no cache to clear (database queries are direct)")
    }

    /**
     * Get cache statistics for monitoring.
     *
     * Note: No caching currently implemented.
     * @return Empty map (no cache stats available)
     */
    fun getCacheStats(): Map<String, Int> {
        // TODO: Implement caching layer if needed for performance
        return emptyMap()
    }

    /**
     * Reset to system default locale.
     *
     * @return True if reset was successful
     */
    suspend fun resetToSystemLocale(): Boolean = withContext(Dispatchers.IO) {
        val systemLocale = Locale.getDefault().toLanguageTag()
        return@withContext setLocale(systemLocale)
    }

    /**
     * Check if a locale is currently loaded.
     *
     * @param locale Locale code to check
     * @return True if locale has commands in database
     */
    suspend fun isLocaleLoaded(locale: String): Boolean = withContext(Dispatchers.IO) {
        val database = CommandDatabase.getInstance(context)
        return@withContext database.voiceCommandDao().hasCommandsForLocale(locale)
    }

    /**
     * Get command by ID for current locale.
     *
     * @param commandId Command ID (action_id)
     * @return Command entity or null if not found
     */
    suspend fun getCommand(commandId: String): VoiceCommandEntity? = withContext(Dispatchers.IO) {
        val database = CommandDatabase.getInstance(context)
        val dao = database.voiceCommandDao()

        // Try current locale first
        val currentCommand = dao.getCommand(commandId, _currentLocale.value)
        if (currentCommand != null) {
            return@withContext currentCommand
        }

        // Fallback to English
        if (_currentLocale.value != FALLBACK_LOCALE) {
            return@withContext dao.getCommand(commandId, FALLBACK_LOCALE)
        }

        return@withContext null
    }
}
