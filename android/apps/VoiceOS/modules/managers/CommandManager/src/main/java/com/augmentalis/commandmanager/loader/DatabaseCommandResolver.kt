/**
 * DatabaseCommandResolver.kt - Resolve commands from database
 *
 * Purpose: Bridge between database-stored commands (from compact JSON)
 * and CommandProcessor's CommandDefinition format
 *
 * This resolves the issue where static commands from compact JSON were not executing
 * because CommandProcessor only used hardcoded CommandDefinitions.
 */

package com.augmentalis.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.database.dto.VoiceCommandDTO
import com.augmentalis.voiceos.command.CommandContext
import com.augmentalis.voiceos.command.CommandDefinition
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves commands from database to CommandDefinition format
 * Provides integration between database-stored commands and CommandProcessor
 *
 * MIGRATED TO SQLDELIGHT - Uses VoiceOSDatabaseManager
 */
class DatabaseCommandResolver(
    private val context: Context,
    private val voiceCommandRepository: IVoiceCommandRepository
) {

    companion object {
        private const val TAG = "DatabaseCommandResolver"

        /**
         * Convenience factory method
         */
        fun create(context: Context): DatabaseCommandResolver {
            val databaseManager = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context))
            return DatabaseCommandResolver(
                context,
                databaseManager.voiceCommands
            )
        }
    }

    /**
     * Get all commands as CommandDefinitions from database
     *
     * @param locale Locale to load (defaults to system locale)
     * @param includeFallback Whether to include fallback locale commands
     * @return List of CommandDefinition objects
     */
    suspend fun getAllCommandDefinitions(
        locale: String? = null,
        includeFallback: Boolean = true
    ): List<CommandDefinition> = withContext(Dispatchers.IO) {
        try {
            // Determine locale to use
            val targetLocale = locale ?: getSystemLocale()

            // Get commands from database
            val commands = if (includeFallback) {
                // Get both target locale and fallback (en-US)
                voiceCommandRepository.getByLocaleWithFallback(targetLocale)
            } else {
                // Get only target locale
                voiceCommandRepository.getByLocale(targetLocale)
            }

            // Convert to CommandDefinitions
            val definitions = commands.mapNotNull { dto ->
                convertDTOToDefinition(dto)
            }

            Log.i(TAG, "Loaded ${definitions.size} command definitions from database (locale: $targetLocale)")

            definitions

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load command definitions from database", e)
            emptyList()
        }
    }

    /**
     * Get commands for specific category
     *
     * @param category Category name (e.g., "navigation", "scroll")
     * @param locale Locale to load
     * @return List of CommandDefinition objects for category
     */
    suspend fun getCommandsByCategory(
        category: String,
        locale: String? = null
    ): List<CommandDefinition> = withContext(Dispatchers.IO) {
        try {
            val targetLocale = locale ?: getSystemLocale()
            val commands = voiceCommandRepository.getByCategory(category)

            val definitions = commands.mapNotNull { dto ->
                convertDTOToDefinition(dto)
            }

            Log.d(TAG, "Loaded ${definitions.size} commands for category: $category")

            definitions

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load commands for category: $category", e)
            emptyList()
        }
    }

    /**
     * Get contextual commands based on app context
     *
     * @param commandContext Current app context (package, view, etc.)
     * @param locale Locale to load
     * @return List of CommandDefinition objects relevant to context
     */
    suspend fun getContextualCommands(
        commandContext: CommandContext,
        locale: String? = null
    ): List<CommandDefinition> = withContext(Dispatchers.IO) {
        try {
            val targetLocale = locale ?: getSystemLocale()

            // Determine relevant categories based on context
            val relevantCategories = determineRelevantCategories(commandContext)

            // Get all commands for relevant categories
            val commands = relevantCategories.flatMap { category ->
                voiceCommandRepository.getByCategory(category)
            }.filter { it.locale == targetLocale || it.locale == "en-US" } // Fallback to en-US

            // Also include global commands (navigation, system, etc.)
            val globalCommands = (voiceCommandRepository.getByCategory("navigation") +
                    voiceCommandRepository.getByCategory("system"))
                    .filter { it.locale == targetLocale || it.locale == "en-US" }

            val allCommands = (commands + globalCommands).distinctBy { it.id }

            val definitions = allCommands.mapNotNull { dto ->
                convertDTOToDefinition(dto)
            }

            Log.d(TAG, "Loaded ${definitions.size} contextual commands")

            definitions

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load contextual commands", e)
            emptyList()
        }
    }

    /**
     * Search commands by text (for command palette, autocomplete, etc.)
     *
     * @param searchText Text to search for
     * @param locale Locale to search in
     * @return List of matching CommandDefinition objects
     */
    suspend fun searchCommands(
        searchText: String,
        locale: String? = null
    ): List<CommandDefinition> = withContext(Dispatchers.IO) {
        try {
            val targetLocale = locale ?: getSystemLocale()
            val commands = voiceCommandRepository.searchByTrigger(searchText)
                .filter { it.locale == targetLocale || it.locale == "en-US" }

            val definitions = commands.mapNotNull { dto ->
                convertDTOToDefinition(dto)
            }

            Log.d(TAG, "Search for '$searchText' returned ${definitions.size} commands")

            definitions

        } catch (e: Exception) {
            Log.e(TAG, "Failed to search commands", e)
            emptyList()
        }
    }

    // Private helper methods

    /**
     * Convert VoiceCommandDTO to CommandDefinition
     *
     * Maps database format to CommandProcessor format
     */
    private fun convertDTOToDefinition(dto: VoiceCommandDTO): CommandDefinition? {
        return try {
            // Current schema: VoiceCommandDTO has triggerPhrase, action, category
            // No synonyms or description in current schema
            // Build patterns list with just the trigger phrase
            val patterns = listOf(dto.triggerPhrase)

            CommandDefinition(
                id = dto.id.toString(),
                name = dto.triggerPhrase, // Use trigger phrase as display name
                description = "${dto.category}: ${dto.action}", // Generate description from category and action
                category = dto.category.uppercase(),
                patterns = patterns,
                requiredContext = determineRequiredContext(dto.category).toSet(),
                parameters = emptyList() // Static commands don't have parameters
            )

        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert DTO ${dto.id} to definition", e)
            null
        }
    }

    /**
     * Parse synonyms from JSON array string
     * NOTE: Current schema doesn't support synonyms
     * Kept for future enhancement when synonyms field is added
     */
    private fun parseSynonyms(synonymsJson: String?): List<String> {
        if (synonymsJson.isNullOrBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(synonymsJson)
            val synonyms = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                synonyms.add(jsonArray.getString(i))
            }

            synonyms

        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse synonyms: $synonymsJson", e)
            emptyList()
        }
    }

    /**
     * Determine required context based on category
     */
    private fun determineRequiredContext(category: String): List<String> {
        return when (category.lowercase()) {
            "editing", "dictation" -> listOf("text_input")
            "browser" -> listOf("browser")
            "keyboard" -> listOf("keyboard_visible")
            else -> emptyList() // No context required
        }
    }

    /**
     * Determine relevant categories based on app context
     */
    private fun determineRelevantCategories(context: CommandContext): List<String> {
        val categories = mutableListOf<String>()

        // Always include navigation and system
        categories.add("navigation")
        categories.add("system")

        // Context-specific categories
        if (context.viewId?.contains("EditText") == true ||
            context.viewId?.contains("Input") == true) {
            categories.add("editing")
            categories.add("dictation")
            categories.add("keyboard")
        }

        if (context.packageName?.contains("browser") == true) {
            categories.add("browser")
            categories.add("scroll")
        }

        // Always include these common categories
        categories.addAll(listOf("cursor", "scroll", "volume", "overlays"))

        return categories.distinct()
    }

    /**
     * Get system locale in format matching our database locales
     */
    private fun getSystemLocale(): String {
        val locale = java.util.Locale.getDefault()
        val language = locale.language
        val country = locale.country

        return "$language-$country"
    }

    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val allCommands = voiceCommandRepository.getAll()
            val totalCommands = allCommands.size
            val locales = allCommands.map { it.locale }.distinct()
            val categories = allCommands.groupBy { it.category }.mapValues { it.value.size }

            mapOf(
                "totalCommands" to totalCommands,
                "locales" to locales,
                "categories" to categories
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get database stats", e)
            emptyMap()
        }
    }
}
