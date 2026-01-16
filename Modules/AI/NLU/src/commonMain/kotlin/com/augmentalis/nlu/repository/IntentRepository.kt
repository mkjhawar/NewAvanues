/**
 * IntentRepository - Data access layer for UnifiedIntent
 *
 * Provides CRUD operations and search functionality for intents
 * stored in SQLDelight database.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.nlu.repository

import com.augmentalis.nlu.model.IntentSource
import com.augmentalis.nlu.model.UnifiedIntent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for intent data access.
 *
 * Platform-specific implementations will use SQLDelight-generated queries.
 */
interface IntentRepository {

    /**
     * Get all intents
     */
    suspend fun getAll(): List<UnifiedIntent>

    /**
     * Get all intents as Flow for reactive updates
     */
    fun getAllAsFlow(): Flow<List<UnifiedIntent>>

    /**
     * Get intent by ID
     */
    suspend fun getById(id: String): UnifiedIntent?

    /**
     * Get intents by category
     */
    suspend fun getByCategory(category: String): List<UnifiedIntent>

    /**
     * Get intents by source
     */
    suspend fun getBySource(source: String): List<UnifiedIntent>

    /**
     * Get intents by locale
     */
    suspend fun getByLocale(locale: String): List<UnifiedIntent>

    /**
     * Search intents by phrase
     */
    suspend fun searchByPhrase(query: String): List<UnifiedIntent>

    /**
     * Find intent by exact pattern match
     */
    suspend fun findByPattern(pattern: String): UnifiedIntent?

    /**
     * Find intent by exact synonym match
     */
    suspend fun findBySynonym(synonym: String): UnifiedIntent?

    /**
     * Get intents with embeddings
     */
    suspend fun getWithEmbeddings(): List<UnifiedIntent>

    /**
     * Save single intent
     */
    suspend fun save(intent: UnifiedIntent)

    /**
     * Save multiple intents
     */
    suspend fun saveAll(intents: List<UnifiedIntent>)

    /**
     * Delete intent by ID
     */
    suspend fun delete(id: String)

    /**
     * Delete all intents from source
     */
    suspend fun deleteBySource(source: String)

    /**
     * Clear all intents
     */
    suspend fun clear()

    /**
     * Get intent count
     */
    suspend fun count(): Long

    /**
     * Get count by category
     */
    suspend fun countByCategory(): Map<String, Long>

    /**
     * Get all categories
     */
    suspend fun getCategories(): List<String>
}

/**
 * Intent loading result
 */
data class IntentLoadResult(
    val loaded: Int,
    val updated: Int,
    val errors: List<String>
)

/**
 * Intent repository factory
 */
expect object IntentRepositoryFactory {
    /**
     * Create repository instance
     *
     * Platform-specific implementation creates SQLDelight driver.
     */
    fun create(context: Any?): IntentRepository
}
