/**
 * CommandPersistencePlugin.kt - Command Persistence Plugin contract for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for plugins that handle storage and retrieval of
 * learned voice commands. Enables pluggable persistence backends (local DB,
 * cloud sync, encrypted storage, etc.).
 */
package com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.commandmanager.QuantizedCommand
import kotlinx.coroutines.flow.Flow

/**
 * Command Persistence Plugin contract for command storage.
 *
 * Persistence plugins handle saving, loading, and querying learned voice commands.
 * Different implementations can provide various storage backends:
 * - Local SQLite/Room database
 * - Cloud-synced storage
 * - Encrypted secure storage
 * - In-memory (testing/development)
 *
 * ## Design Principles
 * - **Async Operations**: All I/O operations are suspending functions
 * - **Package-Scoped**: Commands are organized by app package name
 * - **Observable**: Changes can be observed via Flow
 * - **Searchable**: Full-text search support for commands
 *
 * ## Implementation Example
 * ```kotlin
 * class SqliteCommandPersistence : CommandPersistencePlugin {
 *     private val database: CommandDatabase by lazy {
 *         CommandDatabase.create(context.appDataDir)
 *     }
 *
 *     override suspend fun saveCommand(command: QuantizedCommand): Result<Unit> {
 *         return try {
 *             database.commandDao().insert(command.toEntity())
 *             Result.success(Unit)
 *         } catch (e: Exception) {
 *             Result.failure(e)
 *         }
 *     }
 *
 *     override suspend fun loadCommands(packageName: String): List<QuantizedCommand> {
 *         return database.commandDao()
 *             .getByPackage(packageName)
 *             .map { it.toQuantizedCommand() }
 *     }
 *
 *     override suspend fun deleteCommand(avid: String): Result<Unit> {
 *         return try {
 *             database.commandDao().deleteByAvid(avid)
 *             Result.success(Unit)
 *         } catch (e: Exception) {
 *             Result.failure(e)
 *         }
 *     }
 *
 *     override suspend fun searchCommands(query: String): List<QuantizedCommand> {
 *         return database.commandDao()
 *             .search("%$query%")
 *             .map { it.toQuantizedCommand() }
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see QuantizedCommand
 * @see CommandQuery
 * @see CommandChangeEvent
 */
interface CommandPersistencePlugin : UniversalPlugin {

    // =========================================================================
    // Core CRUD Operations
    // =========================================================================

    /**
     * Save a command to persistent storage.
     *
     * If a command with the same AVID exists, it will be updated.
     * Otherwise, a new command is inserted.
     *
     * @param command The command to save
     * @return Result indicating success or failure with error details
     */
    suspend fun saveCommand(command: QuantizedCommand): Result<Unit>

    /**
     * Save multiple commands in a batch operation.
     *
     * More efficient than saving commands individually when dealing
     * with multiple commands (e.g., bulk import).
     *
     * @param commands List of commands to save
     * @return Result with count of successfully saved commands
     */
    suspend fun saveCommands(commands: List<QuantizedCommand>): Result<Int> {
        var savedCount = 0
        for (command in commands) {
            if (saveCommand(command).isSuccess) {
                savedCount++
            }
        }
        return Result.success(savedCount)
    }

    /**
     * Load all commands for a specific package.
     *
     * @param packageName App package name to filter by
     * @return List of commands for the package, empty if none found
     */
    suspend fun loadCommands(packageName: String): List<QuantizedCommand>

    /**
     * Load a specific command by its AVID.
     *
     * @param avid Command AVID to look up
     * @return The command if found, null otherwise
     */
    suspend fun loadCommand(avid: String): QuantizedCommand? {
        return searchCommands(avid).firstOrNull { it.avid == avid }
    }

    /**
     * Delete a command by its AVID.
     *
     * @param avid AVID of the command to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteCommand(avid: String): Result<Unit>

    /**
     * Delete all commands for a specific package.
     *
     * Use with caution - this permanently removes all learned commands
     * for an application.
     *
     * @param packageName Package name to delete commands for
     * @return Result with count of deleted commands
     */
    suspend fun deleteCommandsForPackage(packageName: String): Result<Int>

    /**
     * Search commands by phrase or metadata.
     *
     * Performs a text search across command phrases and metadata.
     * Implementations should support partial matching.
     *
     * @param query Search query string
     * @return List of matching commands
     */
    suspend fun searchCommands(query: String): List<QuantizedCommand>

    // =========================================================================
    // Advanced Query Operations
    // =========================================================================

    /**
     * Query commands with advanced filtering options.
     *
     * @param query Query parameters for filtering
     * @return List of commands matching the query
     */
    suspend fun queryCommands(query: CommandQuery): List<QuantizedCommand> {
        // Default implementation filters loadCommands result
        val baseCommands = if (query.packageName != null) {
            loadCommands(query.packageName)
        } else {
            getAllCommands()
        }

        return baseCommands
            .filter { command ->
                (query.actionType == null || command.actionType == query.actionType) &&
                        (query.minConfidence == null || command.confidence >= query.minConfidence) &&
                        (query.phraseContains == null || command.phrase.contains(query.phraseContains, ignoreCase = true)) &&
                        (query.targetAvid == null || command.targetAvid == query.targetAvid)
            }
            .let { commands ->
                when (query.sortBy) {
                    CommandSortBy.PHRASE -> commands.sortedBy { it.phrase }
                    CommandSortBy.CONFIDENCE -> commands.sortedByDescending { it.confidence }
                    CommandSortBy.ACTION_TYPE -> commands.sortedBy { it.actionType.name }
                    CommandSortBy.AVID -> commands.sortedBy { it.avid }
                    null -> commands
                }
            }
            .let { if (query.descending) it.reversed() else it }
            .drop(query.offset)
            .take(query.limit)
    }

    /**
     * Get all commands across all packages.
     *
     * @return List of all stored commands
     */
    suspend fun getAllCommands(): List<QuantizedCommand>

    /**
     * Get count of commands for a package.
     *
     * @param packageName Package name to count commands for
     * @return Number of commands for the package
     */
    suspend fun getCommandCount(packageName: String): Int {
        return loadCommands(packageName).size
    }

    /**
     * Get total count of all stored commands.
     *
     * @return Total number of commands
     */
    suspend fun getTotalCommandCount(): Int {
        return getAllCommands().size
    }

    /**
     * Get list of all packages that have stored commands.
     *
     * @return Set of package names
     */
    suspend fun getPackagesWithCommands(): Set<String> {
        return getAllCommands().mapNotNull { it.packageName }.toSet()
    }

    // =========================================================================
    // Observation
    // =========================================================================

    /**
     * Observe changes to commands.
     *
     * Returns a Flow that emits [CommandChangeEvent] whenever commands
     * are added, updated, or deleted.
     *
     * @return Flow of command change events
     */
    fun observeChanges(): Flow<CommandChangeEvent>

    /**
     * Observe commands for a specific package.
     *
     * Returns a Flow that emits the current list of commands and updates
     * whenever commands for the package change.
     *
     * @param packageName Package name to observe
     * @return Flow of command lists
     */
    fun observeCommands(packageName: String): Flow<List<QuantizedCommand>>

    // =========================================================================
    // Maintenance Operations
    // =========================================================================

    /**
     * Check if the persistence store is available and healthy.
     *
     * @return true if persistence is available
     */
    suspend fun isAvailable(): Boolean

    /**
     * Get storage statistics.
     *
     * @return StorageStats with usage information
     */
    suspend fun getStorageStats(): StorageStats {
        val commands = getAllCommands()
        return StorageStats(
            totalCommands = commands.size,
            packagesCount = commands.mapNotNull { it.packageName }.toSet().size,
            estimatedSizeBytes = commands.sumOf { estimateCommandSize(it) }
        )
    }

    /**
     * Compact/optimize the storage.
     *
     * Implementations may use this to vacuum databases, clean up
     * orphaned data, etc.
     *
     * @return Result indicating success or failure
     */
    suspend fun compact(): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Export all commands to a portable format.
     *
     * @param format Export format
     * @return Exported data as string
     */
    suspend fun exportCommands(format: ExportFormat = ExportFormat.JSON): String

    /**
     * Import commands from exported data.
     *
     * @param data Exported command data
     * @param format Format of the data
     * @param mode Import mode (merge, replace, skip)
     * @return Result with import statistics
     */
    suspend fun importCommands(
        data: String,
        format: ExportFormat = ExportFormat.JSON,
        mode: ImportMode = ImportMode.MERGE
    ): Result<ImportResult>
}

/**
 * Query parameters for advanced command filtering.
 *
 * @property packageName Filter by package name (null = all packages)
 * @property actionType Filter by action type
 * @property minConfidence Minimum confidence threshold
 * @property phraseContains Filter by phrase containing text
 * @property targetAvid Filter by target element AVID
 * @property sortBy Field to sort by
 * @property descending Sort in descending order
 * @property limit Maximum results to return
 * @property offset Number of results to skip (for pagination)
 */
data class CommandQuery(
    val packageName: String? = null,
    val actionType: com.augmentalis.commandmanager.CommandActionType? = null,
    val minConfidence: Float? = null,
    val phraseContains: String? = null,
    val targetAvid: String? = null,
    val sortBy: CommandSortBy? = null,
    val descending: Boolean = false,
    val limit: Int = 100,
    val offset: Int = 0
) {
    companion object {
        /** Query for all commands */
        val ALL = CommandQuery()

        /** Query builder for fluent construction */
        fun forPackage(packageName: String) = CommandQuery(packageName = packageName)
    }
}

/**
 * Sort field for command queries.
 */
enum class CommandSortBy {
    PHRASE,
    CONFIDENCE,
    ACTION_TYPE,
    AVID
}

/**
 * Event representing a change to stored commands.
 */
sealed class CommandChangeEvent {
    /** Timestamp of the change */
    abstract val timestamp: Long

    /**
     * A command was added or updated.
     */
    data class Upserted(
        val command: QuantizedCommand,
        val isNew: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : CommandChangeEvent()

    /**
     * A command was deleted.
     */
    data class Deleted(
        val avid: String,
        val packageName: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : CommandChangeEvent()

    /**
     * Multiple commands were changed (bulk operation).
     */
    data class BulkChange(
        val addedCount: Int,
        val updatedCount: Int,
        val deletedCount: Int,
        val packageName: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : CommandChangeEvent()

    /**
     * Storage was cleared or reset.
     */
    data class Cleared(
        val packageName: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : CommandChangeEvent()
}

/**
 * Storage statistics.
 *
 * @property totalCommands Total number of stored commands
 * @property packagesCount Number of unique packages with commands
 * @property estimatedSizeBytes Estimated storage size in bytes
 */
data class StorageStats(
    val totalCommands: Int,
    val packagesCount: Int,
    val estimatedSizeBytes: Long
)

/**
 * Format for export/import operations.
 */
enum class ExportFormat {
    /** JSON format */
    JSON,

    /** AVU text format (native VoiceOSCore format) */
    AVU,

    /** CSV format */
    CSV
}

/**
 * Mode for import operations.
 */
enum class ImportMode {
    /** Merge with existing commands (update if AVID exists) */
    MERGE,

    /** Replace all existing commands */
    REPLACE,

    /** Skip commands that already exist */
    SKIP_EXISTING
}

/**
 * Result of an import operation.
 *
 * @property imported Number of commands imported
 * @property updated Number of existing commands updated
 * @property skipped Number of commands skipped
 * @property errors List of error messages for failed imports
 */
data class ImportResult(
    val imported: Int,
    val updated: Int,
    val skipped: Int,
    val errors: List<String> = emptyList()
) {
    val totalProcessed: Int get() = imported + updated + skipped
    val hasErrors: Boolean get() = errors.isNotEmpty()
}

/**
 * Estimate the storage size of a command.
 *
 * @param command Command to estimate size for
 * @return Estimated size in bytes
 */
private fun estimateCommandSize(command: QuantizedCommand): Long {
    // Rough estimate: AVID + phrase + action type + target + metadata
    return (command.avid.length * 2 +
            command.phrase.length * 2 +
            command.actionType.name.length * 2 +
            (command.targetAvid?.length ?: 0) * 2 +
            command.metadata.entries.sumOf { (k, v) -> k.length * 2 + v.length * 2 } +
            32).toLong() // Overhead for Float, object references, etc.
}

/**
 * Extension functions for CommandPersistencePlugin.
 */

/**
 * Check if a command with the given AVID exists.
 *
 * @param avid AVID to check
 * @return true if command exists
 */
suspend fun CommandPersistencePlugin.commandExists(avid: String): Boolean {
    return loadCommand(avid) != null
}

/**
 * Update a command's confidence score.
 *
 * @param avid Command AVID
 * @param newConfidence New confidence value
 * @return Result indicating success or failure
 */
suspend fun CommandPersistencePlugin.updateConfidence(
    avid: String,
    newConfidence: Float
): Result<Unit> {
    val command = loadCommand(avid) ?: return Result.failure(
        NoSuchElementException("Command not found: $avid")
    )
    return saveCommand(command.copy(confidence = newConfidence))
}

/**
 * Get commands sorted by confidence (highest first).
 *
 * @param packageName Package to get commands for
 * @param limit Maximum commands to return
 * @return List of commands sorted by confidence
 */
suspend fun CommandPersistencePlugin.getTopCommands(
    packageName: String,
    limit: Int = 10
): List<QuantizedCommand> {
    return loadCommands(packageName)
        .sortedByDescending { it.confidence }
        .take(limit)
}

/**
 * Find commands that match a phrase (case-insensitive).
 *
 * @param phrase Phrase to match
 * @param packageName Optional package filter
 * @return List of matching commands
 */
suspend fun CommandPersistencePlugin.findByPhrase(
    phrase: String,
    packageName: String? = null
): List<QuantizedCommand> {
    val commands = if (packageName != null) {
        loadCommands(packageName)
    } else {
        getAllCommands()
    }
    return commands.filter { it.phrase.equals(phrase, ignoreCase = true) }
}
