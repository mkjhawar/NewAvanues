/**
 * CommandLoader.kt - Load voice commands from JSON into database
 * 
 * Purpose: Load commands with automatic English fallback
 * Strategy:
 * 1. ALWAYS load English first (is_fallback = true)
 * 2. Then load user's system locale (if different)
 * 3. Resolution: user locale → English fallback → null
 */

package com.augmentalis.voiceoscore.managers.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.managers.commandmanager.database.CommandDatabase
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.DatabaseVersionDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.DatabaseVersionEntity
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
import java.io.FileNotFoundException
import java.util.Locale

/**
 * Loader for voice command JSON files with English fallback support
 * Version 2: Added persistence check to avoid reloading on every app restart
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
        private const val FILE_EXTENSION = ".VOS"  // VOS format (compact JSON)
        
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
            val requiredVersion = "2.0" // From VOS files (v2.0 format with action_map/category_map/meta_map)

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

            Log.i(TAG, "Loading commands from JSON (version: $requiredVersion)...")

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
            
            // Load VOS file from assets
            val vosFile = "$COMMANDS_PATH/$locale$FILE_EXTENSION"
            val vosString = try {
                context.assets.open(vosFile).bufferedReader().use { it.readText() }
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "VOS file not found: $vosFile")
                return LoadResult.LocaleNotFound(locale)
            }
            
            // Parse VOS (compact JSON format)
            val parseResult = ArrayJsonParser.parseCommandsJson(vosString, isFallback)
            when (parseResult) {
                is ArrayJsonParser.ParseResult.Success -> {
                    // Insert into database
                    commandDao.insertBatch(parseResult.commands)
                    
                    Log.i(TAG, "Loaded ${parseResult.commands.size} commands for $locale (fallback: $isFallback)")
                    
                    return LoadResult.Success(
                        commandCount = parseResult.commands.size,
                        locales = listOf(locale)
                    )
                }
                is ArrayJsonParser.ParseResult.Error -> {
                    return LoadResult.Error("Parse error: ${parseResult.message}")
                }
            }
            
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
            context.assets.list(COMMANDS_PATH)
                ?.filter { it.endsWith(FILE_EXTENSION) }
                ?.map { it.removeSuffix(FILE_EXTENSION) }
                ?: emptyList()
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
}
