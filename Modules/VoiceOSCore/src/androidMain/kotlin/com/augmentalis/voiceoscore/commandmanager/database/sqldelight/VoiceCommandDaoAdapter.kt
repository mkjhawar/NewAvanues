/**
 * VoiceCommandDaoAdapter.kt - SQLDelight adapter for VoiceCommandDao
 *
 * Purpose: Bridge between Room-style DAO interface and SQLDelight queries
 * Provides the same API as Room VoiceCommandDao for backward compatibility
 */

package com.augmentalis.voiceoscore.commandmanager.database.sqldelight

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.VoiceCommandDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Data class matching Room VoiceCommandEntity structure
 */
data class VoiceCommandEntity(
    val uid: Long = 0,
    val id: String,
    val locale: String,
    val primaryText: String,
    val synonyms: String,
    val description: String,
    val category: String,
    /** CommandActionType name (v2.0+). Empty string for v1.0 (falls back to id). */
    val actionType: String = "",
    /** JSON metadata string (v2.0+). Empty string if no metadata. */
    val metadata: String = "",
    /** Domain this command belongs to: "app" (global), "web" (browser), "notes", "cockpit", etc. */
    val domain: String = "app",
    val priority: Int = 50,
    val isFallback: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** v3.1: Android resource ID for Layer 3 BoundsResolver lookup (e.g., "com.app:id/btn_save") */
    val resourceId: String? = null,
    /** v3.1: Content-hash fingerprint for cross-session element matching */
    val elementHash: String? = null,
    /** v3.1: Element class name for tree search (e.g., "android.widget.Button") */
    val className: String? = null
) {
    companion object {
        fun getCategoryFromId(actionId: String): String {
            return actionId.substringBefore("_", "unknown")
        }

        fun parseSynonyms(synonymsJson: String): List<String> {
            return try {
                synonymsJson
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Parse metadata JSON string into Map.
         * Returns empty map if metadata is empty or malformed.
         */
        fun parseMetadata(metadataJson: String): Map<String, String> {
            if (metadataJson.isBlank()) return emptyMap()
            return try {
                val json = org.json.JSONObject(metadataJson)
                val map = mutableMapOf<String, String>()
                json.keys().forEach { key -> map[key] = json.optString(key, "") }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    /** Resolved action: v2.0 actionType, falling back to command id for v1.0 */
    val resolvedAction: String get() = actionType.ifEmpty { id }

    fun toDTO(): VoiceCommandDTO = VoiceCommandDTO(
        id = uid,
        commandId = id,
        locale = locale,
        triggerPhrase = primaryText,
        synonyms = synonyms,
        action = resolvedAction,
        description = description,
        category = category,
        domain = domain,
        priority = priority.toLong(),
        isFallback = if (isFallback) 1L else 0L,
        isEnabled = 1L,
        resourceId = resourceId,
        elementHash = elementHash,
        className = className,
        createdAt = createdAt,
        updatedAt = createdAt
    )
}

/**
 * Locale statistics data class
 */
data class LocaleStats(
    val locale: String,
    val count: Int,
    val is_fallback: Int
)

/**
 * SQLDelight adapter implementing VoiceCommandDao-like interface.
 *
 * @deprecated This Room-to-SQLDelight bridge is unnecessary indirection now that
 * [com.augmentalis.voiceoscore.loader.StaticCommandPersistenceImpl] uses raw
 * voiceCommandQueries directly. Still used by CommandLoader and CommandManager.
 * Will be removed when those callers are migrated to use raw queries.
 */
@Deprecated("Use VoiceOSDatabase.voiceCommandQueries directly")
class VoiceCommandDaoAdapter(private val database: VoiceOSDatabase) {

    private val queries = database.voiceCommandQueries

    // ==================== INSERT ====================

    suspend fun insert(command: VoiceCommandEntity): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val hasTargeting = command.resourceId != null || command.elementHash != null || command.className != null
        if (hasTargeting) {
            queries.insertCommandWithTargeting(
                command_id = command.id,
                locale = command.locale,
                trigger_phrase = command.primaryText,
                synonyms = command.synonyms,
                action = command.resolvedAction,
                description = command.description,
                category = command.category,
                domain = command.domain,
                priority = command.priority.toLong(),
                is_fallback = if (command.isFallback) 1L else 0L,
                is_enabled = 1L,
                element_hash = command.elementHash,
                resource_id = command.resourceId,
                class_name = command.className,
                created_at = command.createdAt,
                updated_at = now
            )
        } else {
            queries.insertCommandFull(
                command_id = command.id,
                locale = command.locale,
                trigger_phrase = command.primaryText,
                synonyms = command.synonyms,
                action = command.resolvedAction,
                description = command.description,
                category = command.category,
                domain = command.domain,
                priority = command.priority.toLong(),
                is_fallback = if (command.isFallback) 1L else 0L,
                is_enabled = 1L,
                created_at = command.createdAt,
                updated_at = now
            )
        }
        queries.transactionWithResult {
            queries.lastInsertRowId().executeAsOne()
        }
    }

    suspend fun insertBatch(commands: List<VoiceCommandEntity>): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        val now = System.currentTimeMillis()
        queries.transaction {
            commands.forEach { command ->
                val hasTargeting = command.resourceId != null || command.elementHash != null || command.className != null
                if (hasTargeting) {
                    queries.insertCommandWithTargeting(
                        command_id = command.id,
                        locale = command.locale,
                        trigger_phrase = command.primaryText,
                        synonyms = command.synonyms,
                        action = command.resolvedAction,
                        description = command.description,
                        category = command.category,
                        domain = command.domain,
                        priority = command.priority.toLong(),
                        is_fallback = if (command.isFallback) 1L else 0L,
                        is_enabled = 1L,
                        element_hash = command.elementHash,
                        resource_id = command.resourceId,
                        class_name = command.className,
                        created_at = command.createdAt,
                        updated_at = now
                    )
                } else {
                    queries.insertCommandFull(
                        command_id = command.id,
                        locale = command.locale,
                        trigger_phrase = command.primaryText,
                        synonyms = command.synonyms,
                        action = command.resolvedAction,
                        description = command.description,
                        category = command.category,
                        domain = command.domain,
                        priority = command.priority.toLong(),
                        is_fallback = if (command.isFallback) 1L else 0L,
                        is_enabled = 1L,
                        created_at = command.createdAt,
                        updated_at = now
                    )
                }
                ids.add(queries.lastInsertRowId().executeAsOne())
            }
        }
        ids
    }

    // ==================== QUERY ====================

    suspend fun getCommandsForLocale(locale: String): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.getCommandsByLocale(locale).executeAsList().map { it.toEntity() }
    }

    fun getCommandsForLocaleFlow(locale: String): Flow<List<VoiceCommandEntity>> = flow {
        emit(getCommandsForLocale(locale))
    }

    suspend fun getFallbackCommands(): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.getFallbackCommands().executeAsList().map { it.toEntity() }
    }

    suspend fun getCommand(commandId: String, locale: String): VoiceCommandEntity? = withContext(Dispatchers.IO) {
        queries.getCommand(commandId, locale).executeAsOneOrNull()?.toEntity()
    }

    suspend fun getAllCommands(): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.getAllCommands().executeAsList().map { it.toEntity() }
    }

    suspend fun getAllLocales(): List<String> = withContext(Dispatchers.IO) {
        queries.getAllLocales().executeAsList()
    }

    suspend fun getCommandsByCategory(category: String, locale: String): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.getCommandsByCategoryAndLocale(category, locale).executeAsList().map { it.toEntity() }
    }

    suspend fun getCommandsByLocale(locale: String): List<VoiceCommandEntity> = getCommandsForLocale(locale)

    suspend fun getCommandsWithFallback(locale: String): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.getCommandsWithFallback(locale, locale).executeAsList().map { it.toEntity() }
    }

    suspend fun searchCommands(searchText: String, locale: String): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.searchCommands(locale, searchText, searchText, searchText).executeAsList().map { it.toEntity() }
    }

    suspend fun getCommandCount(locale: String): Int = withContext(Dispatchers.IO) {
        queries.countByLocale(locale).executeAsOne().toInt()
    }

    suspend fun hasCommandsForLocale(locale: String): Boolean = withContext(Dispatchers.IO) {
        queries.hasCommandsForLocale(locale).executeAsOne() > 0L
    }

    // ==================== UPDATE ====================

    suspend fun update(command: VoiceCommandEntity): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        queries.updateCommand(
            trigger_phrase = command.primaryText,
            action = command.resolvedAction,
            category = command.category,
            priority = command.priority.toLong(),
            is_enabled = 1L,
            updated_at = now,
            id = command.uid
        )
        1 // SQLDelight doesn't return affected rows for update
    }

    suspend fun updatePriority(commandId: String, locale: String, priority: Int): Int = withContext(Dispatchers.IO) {
        queries.updatePriority(priority.toLong(), commandId, locale)
        1
    }

    // ==================== DELETE ====================

    suspend fun delete(command: VoiceCommandEntity): Int = withContext(Dispatchers.IO) {
        queries.deleteCommand(command.uid)
        1
    }

    suspend fun deleteCommand(commandId: String, locale: String): Int = withContext(Dispatchers.IO) {
        // First get the command ID, then delete
        val entity = getCommand(commandId, locale)
        if (entity != null) {
            queries.deleteCommand(entity.uid)
            1
        } else {
            0
        }
    }

    suspend fun deleteCommandsForLocale(locale: String): Int = withContext(Dispatchers.IO) {
        val count = getCommandCount(locale)
        queries.deleteCommandsForLocale(locale)
        count
    }

    suspend fun deleteAllCommands(): Int = withContext(Dispatchers.IO) {
        val count = queries.countCommands().executeAsOne().toInt()
        queries.deleteAllCommands()
        count
    }

    suspend fun deleteByCategory(category: String): Int = withContext(Dispatchers.IO) {
        // First count commands in this category
        val allCommands = getAllCommands()
        val count = allCommands.count { it.category == category }
        queries.deleteByCategory(category)
        count
    }

    // ==================== UTILITY ====================

    suspend fun getDatabaseStats(): List<LocaleStats> = withContext(Dispatchers.IO) {
        queries.getDatabaseStats().executeAsList().map {
            val isFallbackValue = it.is_fallback
            LocaleStats(
                locale = it.locale,
                count = it.count.toInt(),
                is_fallback = if (isFallbackValue != null) isFallbackValue.toInt() else 0
            )
        }
    }

    suspend fun getGlobalCommands(locale: String = "en-US"): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.getGlobalCommands(locale).executeAsList().map { it.toEntity() }
    }

    suspend fun getCommandsForApp(locale: String = "en-US"): List<VoiceCommandEntity> = withContext(Dispatchers.IO) {
        queries.getCommandsForApp(locale).executeAsList().map { it.toEntity() }
    }

    suspend fun getScreensForApp(packageName: String): List<String> {
        // Placeholder - schema extension needed
        return emptyList()
    }

    suspend fun getCommandsForScreen(packageName: String, screenName: String, locale: String = "en-US"): List<VoiceCommandEntity> {
        // Placeholder - schema extension needed
        return emptyList()
    }

    // ==================== EXTENSION ====================

    // RENAMED (2025-12-05): Voice_commands -> Commands_static
    private fun com.augmentalis.database.Commands_static.toEntity(): VoiceCommandEntity = VoiceCommandEntity(
        uid = this.id,
        id = this.command_id,
        locale = this.locale,
        primaryText = this.trigger_phrase,
        synonyms = this.synonyms,
        description = this.description,
        category = this.category,
        actionType = this.action,
        domain = this.domain,
        priority = this.priority.toInt(),
        isFallback = this.is_fallback == 1L,
        createdAt = this.created_at,
        resourceId = this.resource_id,
        elementHash = this.element_hash,
        className = this.class_name
    )
}
