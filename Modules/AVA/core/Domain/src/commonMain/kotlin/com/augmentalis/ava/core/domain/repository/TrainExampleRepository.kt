package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TrainExample
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Teach-Ava training examples
 */
interface TrainExampleRepository {
    
    /**
     * Add a new training example
     * Hash-based deduplication prevents duplicates
     * @param example Training example
     * @return Result with success/error (error if duplicate)
     */
    suspend fun addTrainExample(example: TrainExample): Result<TrainExample>
    
    /**
     * Get all training examples for an intent
     * @param intent Intent name
     * @return Flow emitting training examples
     */
    fun getExamplesForIntent(intent: String): Flow<List<TrainExample>>
    
    /**
     * Get all training examples for a locale
     * @param locale Locale code (e.g., "en-US", "es-ES")
     * @return Flow emitting training examples
     */
    fun getExamplesForLocale(locale: String): Flow<List<TrainExample>>
    
    /**
     * Find duplicate training example by hash
     * @param exampleHash MD5 hash of (utterance + intent)
     * @return Result containing example if exists, null otherwise
     */
    suspend fun findDuplicate(exampleHash: String): Result<TrainExample?>
    
    /**
     * Increment usage count when example is matched
     * @param id Example ID
     * @param timestamp When it was used
     * @return Result with success/error
     */
    suspend fun incrementUsage(id: Long, timestamp: Long): Result<Unit>
    
    /**
     * Get all training examples (for export/backup)
     * @return Flow emitting all examples
     */
    fun getAllExamples(): Flow<List<TrainExample>>

    /**
     * Delete a training example
     * @param id Example ID
     * @return Result with success/error
     */
    suspend fun deleteTrainExample(id: Long): Result<Unit>
}
