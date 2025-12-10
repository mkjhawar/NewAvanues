package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.TrainExampleQueries
import com.augmentalis.ava.core.data.mapper.toDomain
import com.augmentalis.ava.core.data.mapper.toInsertParams
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Implementation of TrainExampleRepository using SQLDelight
 * VOS4 Pattern: Hash-based deduplication for training examples
 *
 * Updated: Room removed, now uses SQLDelight queries directly
 */
class TrainExampleRepositoryImpl(
    private val trainExampleQueries: TrainExampleQueries
) : TrainExampleRepository {

    override suspend fun addTrainExample(example: TrainExample): Result<TrainExample> = withContext(Dispatchers.IO) {
        try {
            // Check for duplicate using hash
            val existingExample = trainExampleQueries.findByHash(example.exampleHash).executeAsOneOrNull()
            if (existingExample != null) {
                return@withContext Result.Error(
                    exception = IllegalArgumentException("Duplicate training example"),
                    message = "This example already exists"
                )
            }

            // Insert new example
            val params = example.toInsertParams()
            trainExampleQueries.insertWithReturn(
                example_hash = params.example_hash,
                utterance = params.utterance,
                intent = params.intent,
                locale = params.locale,
                source = params.source,
                created_at = params.created_at,
                usage_count = params.usage_count,
                last_used = params.last_used,
                // ADR-013: Self-learning columns with defaults
                confidence = 0.8,           // Default confidence for manual examples
                user_confirmed = 0L,        // Not confirmed yet (false)
                times_matched = 0L,         // Never matched
                embedding_vector = null,    // Will be computed lazily by NLUSelfLearner
                embedding_dimension = 384L  // Standard embedding dimension
            )

            // Get the last inserted ID
            val insertedId = trainExampleQueries.lastInsertRowId().executeAsOne()
            val inserted = example.copy(id = insertedId)
            Result.Success(inserted)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to add training example")
        }
    }

    override fun getExamplesForIntent(intent: String): Flow<List<TrainExample>> {
        return trainExampleQueries.selectByIntent(intent)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { examples -> examples.map { it.toDomain() } }
    }

    override fun getExamplesForLocale(locale: String): Flow<List<TrainExample>> {
        return trainExampleQueries.selectByLocale(locale)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { examples -> examples.map { it.toDomain() } }
    }

    override suspend fun findDuplicate(exampleHash: String): Result<TrainExample?> = withContext(Dispatchers.IO) {
        try {
            val example = trainExampleQueries.findByHash(exampleHash).executeAsOneOrNull()
            Result.Success(example?.toDomain())
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to check for duplicate")
        }
    }

    override suspend fun incrementUsage(id: Long, timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            trainExampleQueries.updateUsageStats(last_used = timestamp, id = id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to increment usage")
        }
    }

    override fun getAllExamples(): Flow<List<TrainExample>> {
        return trainExampleQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { examples -> examples.map { it.toDomain() } }
    }

    override suspend fun deleteTrainExample(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            trainExampleQueries.delete(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to delete training example")
        }
    }

    companion object {
        /**
         * Generate MD5 hash for deduplication
         * VOS4 Pattern: Hash-based uniqueness
         */
        fun generateHash(utterance: String, intent: String): String {
            val input = "$utterance$intent"
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
