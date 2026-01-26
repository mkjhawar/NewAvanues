// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.dto

import kotlinx.datetime.Clock

/**
 * Data Transfer Object for DatabaseVersion table.
 * Tracks database version for persistence check.
 *
 * Corresponds to DatabaseVersion.sq schema.
 * Single-row table (id = 1) for version tracking.
 */
data class DatabaseVersionDTO(
    val id: Long = 1,
    val jsonVersion: String,
    val loadedAt: Long,
    val commandCount: Long,
    val locales: String // JSON array string: ["en-US", "es-ES"]
) {
    companion object {
        /**
         * Create version entity from load result.
         */
        fun create(
            jsonVersion: String,
            commandCount: Int,
            locales: List<String>
        ): DatabaseVersionDTO {
            return DatabaseVersionDTO(
                id = 1,
                jsonVersion = jsonVersion,
                loadedAt = Clock.System.now().toEpochMilliseconds(),
                commandCount = commandCount.toLong(),
                locales = "[\"${locales.joinToString("\",\"")}\"]"
            )
        }
    }

    /**
     * Parse locales JSON array to list.
     * Simple parser for format: ["en-US","es-ES"]
     */
    fun getLocaleList(): List<String> {
        return try {
            locales
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
     * Check if this version is older than specified time.
     */
    fun isOlderThan(maxAgeMs: Long): Boolean {
        return Clock.System.now().toEpochMilliseconds() - loadedAt > maxAgeMs
    }
}

/**
 * Extension to convert SQLDelight entity to DTO.
 */
fun com.augmentalis.database.Database_version.toDatabaseVersionDTO(): DatabaseVersionDTO = DatabaseVersionDTO(
    id = id,
    jsonVersion = json_version,
    loadedAt = loaded_at,
    commandCount = command_count,
    locales = locales
)
