/**
 * DatabaseVersionDaoAdapter.kt - SQLDelight adapter for DatabaseVersionDao
 *
 * Purpose: Bridge between Room-style DAO interface and SQLDelight queries
 * Provides the same API as Room DatabaseVersionDao for backward compatibility
 */

package com.augmentalis.voiceoscore.commandmanager.database.sqldelight

import com.augmentalis.database.VoiceOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Data class matching Room DatabaseVersionEntity structure
 */
data class DatabaseVersionEntity(
    val id: Int = 1,
    val jsonVersion: String,
    val loadedAt: Long,
    val commandCount: Int,
    val locales: String // JSON array string
) {
    companion object {
        fun create(
            jsonVersion: String,
            commandCount: Int,
            locales: List<String>
        ): DatabaseVersionEntity {
            return DatabaseVersionEntity(
                jsonVersion = jsonVersion,
                loadedAt = System.currentTimeMillis(),
                commandCount = commandCount,
                locales = JSONArray(locales).toString()
            )
        }
    }

    /**
     * Parse locales JSON array to list.
     */
    fun getLocaleList(): List<String> {
        return try {
            val jsonArray = JSONArray(locales)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if this version is older than specified time.
     */
    fun isOlderThan(maxAgeMs: Long): Boolean {
        return System.currentTimeMillis() - loadedAt > maxAgeMs
    }
}

/**
 * SQLDelight adapter implementing DatabaseVersionDao-like interface
 */
class DatabaseVersionDaoAdapter(private val database: VoiceOSDatabase) {

    private val queries = database.databaseVersionQueries

    /**
     * Get current database version.
     * @return Version info or null if database never initialized
     */
    suspend fun getVersion(): DatabaseVersionEntity? = withContext(Dispatchers.IO) {
        queries.getVersion().executeAsOneOrNull()?.toEntity()
    }

    /**
     * Set/update database version.
     * Uses REPLACE strategy to overwrite existing version (id = 1)
     */
    suspend fun setVersion(version: DatabaseVersionEntity) = withContext(Dispatchers.IO) {
        queries.setVersion(
            json_version = version.jsonVersion,
            loaded_at = version.loadedAt,
            command_count = version.commandCount.toLong(),
            locales = version.locales
        )
    }

    /**
     * Clear version info (force reload on next init).
     */
    suspend fun clearVersion() = withContext(Dispatchers.IO) {
        queries.clearVersion()
    }

    /**
     * Check if version exists.
     */
    suspend fun hasVersion(): Boolean = withContext(Dispatchers.IO) {
        queries.hasVersion().executeAsOne() > 0L
    }

    /**
     * Get JSON version only.
     */
    suspend fun getJsonVersion(): String? = withContext(Dispatchers.IO) {
        queries.getJsonVersion().executeAsOneOrNull()
    }

    // ==================== EXTENSION ====================

    private fun com.augmentalis.database.Database_version.toEntity(): DatabaseVersionEntity = DatabaseVersionEntity(
        id = id.toInt(),
        jsonVersion = json_version,
        loadedAt = loaded_at,
        commandCount = command_count.toInt(),
        locales = locales
    )
}
