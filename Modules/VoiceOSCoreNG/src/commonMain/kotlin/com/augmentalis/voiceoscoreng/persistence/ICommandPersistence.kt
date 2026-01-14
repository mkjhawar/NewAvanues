/**
 * ICommandPersistence.kt - Platform-agnostic command persistence interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * This interface defines the contract for persisting QuantizedCommands.
 * Platform-specific implementations can delegate to:
 * - Android: SQLDelightGeneratedCommandRepository
 * - iOS: Core Data or SQLite
 * - Desktop: SQLite or file-based storage
 *
 * By defining this interface in commonMain, JITLearner and other components
 * can persist commands without directly depending on the database module.
 */
package com.augmentalis.voiceoscoreng.persistence

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Interface for persisting voice commands to storage.
 *
 * Implementations should be thread-safe and handle errors gracefully.
 * All operations are suspending for async/database compatibility.
 *
 * ## Usage
 *
 * ```kotlin
 * // Android implementation
 * class AndroidCommandPersistence(
 *     private val repository: IGeneratedCommandRepository
 * ) : ICommandPersistence {
 *     override suspend fun insertBatch(commands: List<QuantizedCommand>) {
 *         repository.insertBatch(commands.map { it.toGeneratedCommandDTO() })
 *     }
 * }
 *
 * // Use with JITLearner
 * val persistence = AndroidCommandPersistence(repository)
 * val learner = JITLearner(persistence = persistence)
 * ```
 */
interface ICommandPersistence {

    /**
     * Insert a batch of commands into storage.
     *
     * This is the primary method for persisting learned commands.
     * Implementation should use batch operations for efficiency.
     *
     * @param commands List of commands to persist
     * @throws Exception if persistence fails
     */
    suspend fun insertBatch(commands: List<QuantizedCommand>)

    /**
     * Insert a single command into storage.
     *
     * @param command Command to persist
     * @return ID assigned by storage, or null if ID is not applicable
     * @throws Exception if persistence fails
     */
    suspend fun insert(command: QuantizedCommand): Long?

    /**
     * Get all commands for a specific package.
     *
     * @param packageName App package identifier
     * @return List of commands for the package
     */
    suspend fun getByPackage(packageName: String): List<QuantizedCommand>

    /**
     * Get command count for a package.
     *
     * @param packageName App package identifier
     * @return Number of commands stored for the package
     */
    suspend fun countByPackage(packageName: String): Long

    /**
     * Delete all commands for a package.
     *
     * Used when app is uninstalled or user wants to reset learning.
     *
     * @param packageName App package identifier
     * @return Number of commands deleted
     */
    suspend fun deleteByPackage(packageName: String): Int
}

/**
 * No-op implementation of ICommandPersistence.
 *
 * Use when database is not available or persistence is disabled.
 * Commands are accepted but not stored.
 */
object NoOpCommandPersistence : ICommandPersistence {
    override suspend fun insertBatch(commands: List<QuantizedCommand>) {
        // No-op: commands not persisted
    }

    override suspend fun insert(command: QuantizedCommand): Long? {
        // No-op: return null to indicate no ID assigned
        return null
    }

    override suspend fun getByPackage(packageName: String): List<QuantizedCommand> {
        // No data stored
        return emptyList()
    }

    override suspend fun countByPackage(packageName: String): Long {
        // No data stored
        return 0
    }

    override suspend fun deleteByPackage(packageName: String): Int {
        // No data to delete
        return 0
    }
}
