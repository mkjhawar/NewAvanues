/**
 * ICommandRepository.kt - Command storage repository interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Repository interface for command persistence.
 * Allows VoiceOSCoreNG to work with any database implementation.
 */
package com.augmentalis.voiceoscoreng.repository

import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import kotlinx.coroutines.flow.Flow

/**
 * Repository for voice command storage and retrieval.
 *
 * Implementations:
 * - SQLDelightCommandRepository (production)
 * - InMemoryCommandRepository (testing)
 */
interface ICommandRepository {

    /**
     * Save a generated command.
     */
    suspend fun save(command: QuantizedCommand): Result<Unit>

    /**
     * Save multiple commands in batch.
     */
    suspend fun saveAll(commands: List<QuantizedCommand>): Result<Unit>

    /**
     * Get all commands for an app.
     */
    suspend fun getByApp(packageName: String): List<QuantizedCommand>

    /**
     * Get all commands for a screen.
     */
    suspend fun getByScreen(packageName: String, screenId: String): List<QuantizedCommand>

    /**
     * Get command by VUID.
     */
    suspend fun getByVuid(vuid: String): QuantizedCommand?

    /**
     * Delete commands for a screen.
     */
    suspend fun deleteByScreen(packageName: String, screenId: String): Result<Unit>

    /**
     * Delete all commands for an app.
     */
    suspend fun deleteByApp(packageName: String): Result<Unit>

    /**
     * Observe commands for a screen as flow.
     */
    fun observeByScreen(packageName: String, screenId: String): Flow<List<QuantizedCommand>>

    /**
     * Get count of commands for an app.
     */
    suspend fun countByApp(packageName: String): Long
}

/**
 * In-memory implementation for testing.
 */
class InMemoryCommandRepository : ICommandRepository {
    private val commands = mutableMapOf<String, QuantizedCommand>()

    override suspend fun save(command: QuantizedCommand): Result<Unit> {
        commands[command.vuid] = command
        return Result.success(Unit)
    }

    override suspend fun saveAll(commands: List<QuantizedCommand>): Result<Unit> {
        commands.forEach { this.commands[it.vuid] = it }
        return Result.success(Unit)
    }

    override suspend fun getByApp(packageName: String): List<QuantizedCommand> {
        return commands.values.filter { it.metadata["packageName"] == packageName }
    }

    override suspend fun getByScreen(packageName: String, screenId: String): List<QuantizedCommand> {
        return commands.values.filter {
            it.metadata["packageName"] == packageName &&
            it.metadata["screenId"] == screenId
        }
    }

    override suspend fun getByVuid(vuid: String): QuantizedCommand? {
        return commands[vuid]
    }

    override suspend fun deleteByScreen(packageName: String, screenId: String): Result<Unit> {
        commands.entries.removeIf {
            it.value.metadata["packageName"] == packageName &&
            it.value.metadata["screenId"] == screenId
        }
        return Result.success(Unit)
    }

    override suspend fun deleteByApp(packageName: String): Result<Unit> {
        commands.entries.removeIf { it.value.metadata["packageName"] == packageName }
        return Result.success(Unit)
    }

    override fun observeByScreen(packageName: String, screenId: String): Flow<List<QuantizedCommand>> {
        return kotlinx.coroutines.flow.flowOf(getByScreenSync(packageName, screenId))
    }

    private fun getByScreenSync(packageName: String, screenId: String): List<QuantizedCommand> {
        return commands.values.filter {
            it.metadata["packageName"] == packageName &&
            it.metadata["screenId"] == screenId
        }
    }

    override suspend fun countByApp(packageName: String): Long {
        return commands.values.count { it.metadata["packageName"] == packageName }.toLong()
    }
}
