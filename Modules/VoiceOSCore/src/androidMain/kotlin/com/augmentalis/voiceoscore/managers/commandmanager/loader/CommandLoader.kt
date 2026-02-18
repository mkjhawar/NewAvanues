/**
 * CommandLoader.kt - Load voice commands from VOS files into database
 *
 * Purpose: Load commands with automatic English fallback
 * Strategy:
 * 1. ALWAYS load English first (is_fallback = true)
 * 2. Then load user's system locale (if different)
 * 3. Resolution: user locale → English fallback → null
 *
 * Uses VosParser (commonMain KMP) which auto-detects format:
 * - v2.1 JSON: legacy format with embedded maps
 * - v3.0 Compact: pipe-delimited with compiled maps
 */

package com.augmentalis.voiceoscore.managers.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.loader.VosParsedCommand
import com.augmentalis.voiceoscore.loader.VosParseResult
import com.augmentalis.voiceoscore.loader.VosParser
import com.augmentalis.voiceoscore.managers.commandmanager.database.CommandDatabase
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.DatabaseVersionDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.DatabaseVersionEntity
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandEntity
import java.io.FileNotFoundException
import java.util.Locale

/**
 * Loader for VOS command files with English fallback support.
 * v3: Uses VosParser (KMP) with auto-detection of JSON v2.1 and compact v3.0.
 * v2: Added persistence check to avoid reloading on every app restart.
 */
class CommandLoader(
    private val context: Context,
    private val commandDao: VoiceCommandDaoAdapter,
    private val versionDao: DatabaseVersionDaoAdapter
) {
    
    companion object {
        private const val TAG = "CommandLoader"
        private const val COMMANDS_PATH = "localization/commands"
        private const val FALLBACK_LOCALE = "en-US"
        private const val FILE_EXTENSION = ".VOS"  // Legacy monolithic format (unused)
        private const val FILE_EXTENSION_APP = ".app.vos"  // App domain commands
        private const val FILE_EXTENSION_WEB = ".web.vos"  // Web/browser domain commands
        
        /**
         * Convenience method to create loader with database instance
         */
        fun create(context: Context): CommandLoader {
            val database = CommandDatabase.getInstance(context)
            return CommandLoader(
                context,
                database.voiceCommandDao(),
                database.databaseVersionDao()
            )
        }
    }
    
    /**
     * Initialize command database
     * Loads English first, then user's system locale
     *
     * Version 2: Added persistence check - only loads if version mismatch or empty
     *
     * @return LoadResult with success/failure status
     */
    suspend fun initializeCommands(): LoadResult {
        try {
            Log.d(TAG, "Initializing command database...")

            // 0. CHECK if database already loaded with correct version
            val existingVersion = versionDao.getVersion()
            val requiredVersion = "3.1" // v3.1: domain activation system — domain column in commands_static

            if (existingVersion != null && existingVersion.jsonVersion == requiredVersion) {
                val commandCount = commandDao.getCommandCount(FALLBACK_LOCALE)
                if (commandCount > 0) {
                    Log.i(TAG, "✅ Database already initialized (v${existingVersion.jsonVersion}, ${existingVersion.commandCount} commands)")
                    Log.i(TAG, "   Skipping reload (loaded at ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(existingVersion.loadedAt)})")
                    return LoadResult.Success(
                        commandCount = existingVersion.commandCount,
                        locales = existingVersion.getLocaleList()
                    )
                }
            }

            Log.i(TAG, "Loading commands from VOS files (version: $requiredVersion)...")

            // 1. ALWAYS load English first (fallback)
            val englishResult = loadLocale(FALLBACK_LOCALE, isFallback = true)
            if (englishResult !is LoadResult.Success) {
                return LoadResult.Error("Failed to load English fallback: ${englishResult}")
            }
            
            Log.i(TAG, "✅ English fallback loaded: ${englishResult.commandCount} commands")
            
            // 2. Load user's system locale (if different from English)
            val systemLocale = getSystemLocale()
            if (systemLocale != FALLBACK_LOCALE) {
                val userResult = loadLocale(systemLocale, isFallback = false)
                when (userResult) {
                    is LoadResult.Success -> {
                        Log.i(TAG, "✅ User locale loaded: $systemLocale (${userResult.commandCount} commands)")
                    }
                    is LoadResult.LocaleNotFound -> {
                        Log.w(TAG, "⚠️ User locale $systemLocale not found, using English fallback")
                    }
                    is LoadResult.Error -> {
                        Log.w(TAG, "⚠️ Failed to load user locale $systemLocale: ${userResult.message}")
                    }
                }
            }
            
            // Get statistics
            val stats = commandDao.getDatabaseStats()
            val totalCommands = stats.sumOf { it.count }
            val locales = stats.map { it.locale }

            // Save version info to prevent reload on next startup
            val versionEntity = DatabaseVersionEntity.create(
                jsonVersion = requiredVersion,
                commandCount = totalCommands,
                locales = locales
            )
            versionDao.setVersion(versionEntity)

            Log.i(TAG, "✅ Command database initialized: $totalCommands commands across ${locales.joinToString(", ")}")
            Log.i(TAG, "   Version $requiredVersion saved - will skip reload on next startup")

            return LoadResult.Success(
                commandCount = totalCommands,
                locales = locales
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize commands", e)
            return LoadResult.Error("Initialization failed: ${e.message}")
        }
    }
    
    /**
     * Load commands for a specific locale
     * 
     * @param locale Locale code (e.g., "en-US", "es-ES")
     * @param isFallback Whether this is the fallback locale
     * @return LoadResult indicating success or failure
     */
    suspend fun loadLocale(locale: String, isFallback: Boolean): LoadResult {
        try {
            // Check if already loaded
            if (commandDao.hasCommandsForLocale(locale)) {
                val count = commandDao.getCommandCount(locale)
                Log.d(TAG, "Locale $locale already loaded ($count commands)")
                return LoadResult.Success(commandCount = count, locales = listOf(locale))
            }

            // Load both domain files (.app.vos + .web.vos) and merge
            val allCommands = mutableListOf<VoiceCommandEntity>()

            // Load app domain commands
            val appFile = "$COMMANDS_PATH/$locale$FILE_EXTENSION_APP"
            val appString = try {
                context.assets.open(appFile).bufferedReader().use { it.readText() }
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "App VOS file not found: $appFile")
                null
            }

            if (appString != null) {
                val appResult = VosParser.parse(appString, isFallback)
                if (appResult is VosParseResult.Success) {
                    allCommands.addAll(appResult.commands.map { it.toEntity() })
                    Log.d(TAG, "Loaded ${appResult.commands.size} app commands for $locale")
                } else if (appResult is VosParseResult.Error) {
                    Log.e(TAG, "Failed to parse app VOS for $locale: ${appResult.message}")
                }
            }

            // Load web domain commands
            val webFile = "$COMMANDS_PATH/$locale$FILE_EXTENSION_WEB"
            val webString = try {
                context.assets.open(webFile).bufferedReader().use { it.readText() }
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "Web VOS file not found: $webFile")
                null
            }

            if (webString != null) {
                val webResult = VosParser.parse(webString, isFallback)
                if (webResult is VosParseResult.Success) {
                    allCommands.addAll(webResult.commands.map { it.toEntity() })
                    Log.d(TAG, "Loaded ${webResult.commands.size} web commands for $locale")
                } else if (webResult is VosParseResult.Error) {
                    Log.e(TAG, "Failed to parse web VOS for $locale: ${webResult.message}")
                }
            }

            // If neither file found, locale is not available
            if (appString == null && webString == null) {
                return LoadResult.LocaleNotFound(locale)
            }

            if (allCommands.isEmpty()) {
                return LoadResult.Error("No commands parsed for $locale")
            }

            // Insert merged commands into database
            commandDao.insertBatch(allCommands)

            Log.i(TAG, "Loaded ${allCommands.size} commands for $locale (app+web merged, fallback: $isFallback)")

            return LoadResult.Success(
                commandCount = allCommands.size,
                locales = listOf(locale)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load locale $locale", e)
            return LoadResult.Error("Load failed: ${e.message}")
        }
    }
    
    /**
     * Reload all commands (clear and reload)
     * Useful for testing or after updates
     */
    suspend fun reloadAll(): LoadResult {
        try {
            Log.d(TAG, "Reloading all commands...")

            // Clear existing commands
            commandDao.deleteAllCommands()

            // Reinitialize
            return initializeCommands()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload commands", e)
            return LoadResult.Error("Reload failed: ${e.message}")
        }
    }

    /**
     * Force reload all commands (clears version tracking + commands)
     * Use this when JSON files have been updated or for manual reload from settings
     *
     * Version 2: Added for Phase 2.4c - Dynamic Command Updates
     */
    suspend fun forceReload(): LoadResult {
        try {
            Log.i(TAG, "Force reloading all commands (clearing version tracking)...")

            // Clear version tracking (forces reload)
            versionDao.clearVersion()

            // Clear existing commands
            commandDao.deleteAllCommands()

            // Reinitialize (will load from JSON and save new version)
            return initializeCommands()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to force reload commands", e)
            return LoadResult.Error("Force reload failed: ${e.message}")
        }
    }
    
    /**
     * Load additional locale (for multi-language users)
     */
    suspend fun loadAdditionalLocale(locale: String): LoadResult {
        return loadLocale(locale, isFallback = false)
    }
    
    /**
     * Get system locale in format matching our JSON files
     * Examples: "en-US", "es-ES", "fr-FR", "de-DE"
     */
    private fun getSystemLocale(): String {
        val locale = Locale.getDefault()
        val language = locale.language // "en", "es", "fr", "de"
        val country = locale.country   // "US", "ES", "FR", "DE"
        
        // Build locale string: "en-US", "es-ES", etc.
        val localeString = "$language-$country"
        
        Log.d(TAG, "System locale detected: $localeString")
        
        return localeString
    }
    
    /**
     * Get available locales from assets
     */
    fun getAvailableLocales(): List<String> {
        return try {
            val files = context.assets.list(COMMANDS_PATH) ?: emptyArray()
            // Scan for .app.vos files — a locale is available if it has at least the app domain file
            val appLocales = files
                .filter { it.endsWith(FILE_EXTENSION_APP) }
                .map { it.removeSuffix(FILE_EXTENSION_APP) }
                .toSet()
            val webLocales = files
                .filter { it.endsWith(FILE_EXTENSION_WEB) }
                .map { it.removeSuffix(FILE_EXTENSION_WEB) }
                .toSet()
            // Return locales that have both app and web files (complete locales)
            val complete = appLocales.intersect(webLocales)
            Log.d(TAG, "Available locales: $complete (app=${appLocales.size}, web=${webLocales.size})")
            complete.sorted()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list available locales", e)
            emptyList()
        }
    }
    
    /**
     * Load result sealed class
     */
    sealed class LoadResult {
        data class Success(
            val commandCount: Int,
            val locales: List<String>
        ) : LoadResult()

        data class LocaleNotFound(val locale: String) : LoadResult()

        data class Error(val message: String) : LoadResult()
    }

    /**
     * Map KMP [VosParsedCommand] to Android [VoiceCommandEntity].
     * Converts synonyms from List<String> to JSON array string for DB storage.
     */
    private fun VosParsedCommand.toEntity() = VoiceCommandEntity(
        id = id,
        locale = locale,
        primaryText = primaryText,
        synonyms = VosParser.synonymsToJson(synonyms),
        description = description,
        category = category,
        actionType = actionType,
        metadata = metadata,
        domain = domain,
        priority = 50,
        isFallback = isFallback
    )
}
