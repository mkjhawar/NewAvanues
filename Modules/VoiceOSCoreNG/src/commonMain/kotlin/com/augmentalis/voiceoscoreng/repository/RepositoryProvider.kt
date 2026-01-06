/**
 * RepositoryProvider.kt - Repository dependency injection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Provides repository instances for VoiceOSCoreNG.
 * Can be configured with real implementations or mocks.
 */
package com.augmentalis.voiceoscoreng.repository

import com.augmentalis.voiceoscoreng.speech.currentTimeMillis

/**
 * Repository provider for dependency injection.
 *
 * Usage:
 * ```kotlin
 * // Configure with SQLDelight repositories (production)
 * RepositoryProvider.configureWithSQLDelight(
 *     commandRepository = sqlDelightGeneratedCommandRepo,
 *     elementRepository = sqlDelightScrapedElementRepo
 * )
 *
 * // Or configure with custom implementations
 * RepositoryProvider.configure(
 *     commandRepository = myCommandRepo,
 *     vuidRepository = myVuidRepo
 * )
 *
 * // Or use defaults (in-memory)
 * RepositoryProvider.useDefaults()
 *
 * // Access repositories
 * val commands = RepositoryProvider.commands
 * val vuids = RepositoryProvider.vuids
 * ```
 */
object RepositoryProvider {

    private var _commandRepository: ICommandRepository? = null
    private var _vuidRepository: IVuidRepository? = null
    private var _configHistory: IConfigHistoryRepository? = null
    private var _isUsingSQLDelight: Boolean = false

    /**
     * Command repository instance
     */
    val commands: ICommandRepository
        get() = _commandRepository
            ?: throw IllegalStateException("RepositoryProvider not configured. Call configure() first.")

    /**
     * VUID repository instance
     */
    val vuids: IVuidRepository
        get() = _vuidRepository
            ?: throw IllegalStateException("RepositoryProvider not configured. Call configure() first.")

    /**
     * Config history repository instance
     */
    val configHistory: IConfigHistoryRepository
        get() = _configHistory
            ?: throw IllegalStateException("RepositoryProvider not configured. Call configure() first.")

    /**
     * Whether repositories are backed by SQLDelight.
     */
    val isUsingSQLDelight: Boolean
        get() = _isUsingSQLDelight

    /**
     * Configure with SQLDelight repositories from VoiceOS/core/database.
     *
     * This is the recommended production configuration that saves
     * commands and elements to the same database as VoiceOSCore.
     *
     * @param commandRepository ICommandRepository implementation backed by SQLDelight
     * @param vuidRepository IVuidRepository implementation backed by SQLDelight
     * @param configHistoryRepository Optional config history repository
     */
    fun configureWithSQLDelight(
        commandRepository: ICommandRepository,
        vuidRepository: IVuidRepository,
        configHistoryRepository: IConfigHistoryRepository? = null
    ) {
        _commandRepository = commandRepository
        _vuidRepository = vuidRepository
        _configHistory = configHistoryRepository ?: InMemoryConfigHistoryRepository()
        _isUsingSQLDelight = true
    }

    /**
     * Configure with custom repository implementations.
     */
    fun configure(
        commandRepository: ICommandRepository,
        vuidRepository: IVuidRepository,
        configHistoryRepository: IConfigHistoryRepository? = null
    ) {
        _commandRepository = commandRepository
        _vuidRepository = vuidRepository
        _configHistory = configHistoryRepository ?: InMemoryConfigHistoryRepository()
        _isUsingSQLDelight = false
    }

    /**
     * Use default in-memory implementations (for testing).
     */
    fun useDefaults() {
        _commandRepository = InMemoryCommandRepository()
        _vuidRepository = InMemoryVuidRepository()
        _configHistory = InMemoryConfigHistoryRepository()
        _isUsingSQLDelight = false
    }

    /**
     * Check if repositories are configured.
     */
    fun isConfigured(): Boolean =
        _commandRepository != null && _vuidRepository != null

    /**
     * Reset all repositories (for testing).
     */
    fun reset() {
        _commandRepository = null
        _vuidRepository = null
        _configHistory = null
        _isUsingSQLDelight = false
    }
}

/**
 * Config history repository for tracking speech/command configurations.
 */
interface IConfigHistoryRepository {

    /**
     * Save a configuration snapshot.
     */
    suspend fun save(entry: ConfigHistoryEntry): Result<Unit>

    /**
     * Get configuration history.
     */
    suspend fun getHistory(limit: Int = 50): List<ConfigHistoryEntry>

    /**
     * Get last active configuration.
     */
    suspend fun getLast(): ConfigHistoryEntry?

    /**
     * Clear history.
     */
    suspend fun clear(): Result<Unit>
}

/**
 * Configuration history entry.
 */
data class ConfigHistoryEntry(
    val id: String,
    val speechEngine: String,
    val speechMode: String,
    val language: String,
    val commandCount: Int,
    val activeApp: String?,
    val activeScreen: String?,
    val timestamp: Long = currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * In-memory config history (for testing).
 */
class InMemoryConfigHistoryRepository : IConfigHistoryRepository {
    private val entries = mutableListOf<ConfigHistoryEntry>()

    override suspend fun save(entry: ConfigHistoryEntry): Result<Unit> {
        entries.add(0, entry)
        if (entries.size > 100) entries.removeAt(entries.lastIndex)
        return Result.success(Unit)
    }

    override suspend fun getHistory(limit: Int): List<ConfigHistoryEntry> {
        return entries.take(limit)
    }

    override suspend fun getLast(): ConfigHistoryEntry? = entries.firstOrNull()

    override suspend fun clear(): Result<Unit> {
        entries.clear()
        return Result.success(Unit)
    }
}
