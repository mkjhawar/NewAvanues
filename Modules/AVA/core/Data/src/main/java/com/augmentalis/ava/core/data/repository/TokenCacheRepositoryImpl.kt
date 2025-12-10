package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.TokenCacheQueries
import com.augmentalis.ava.core.data.mapper.toDomain
import com.augmentalis.ava.core.data.mapper.toInsertParams
import com.augmentalis.ava.core.domain.model.TokenCache
import com.augmentalis.ava.core.domain.model.TokenCacheSourceType
import com.augmentalis.ava.core.domain.model.TokenCacheStats
import com.augmentalis.ava.core.domain.repository.TokenCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Implementation of TokenCacheRepository using SQLDelight.
 * Provides pre-tokenized content caching to avoid repeated tokenization.
 *
 * VOS4 Pattern: Model-specific cache with hash-based lookup
 */
class TokenCacheRepositoryImpl(
    private val tokenCacheQueries: TokenCacheQueries
) : TokenCacheRepository {

    override suspend fun getCachedTokens(textHash: String, modelId: String): Result<TokenCache?> =
        withContext(Dispatchers.IO) {
            try {
                val cached = tokenCacheQueries.selectByHashAndModel(textHash, modelId)
                    .executeAsOneOrNull()

                if (cached != null) {
                    // Update access stats
                    tokenCacheQueries.updateAccessStats(
                        last_accessed = System.currentTimeMillis(),
                        text_hash = textHash,
                        model_id = modelId
                    )
                }

                Result.Success(cached?.toDomain())
            } catch (e: Exception) {
                Result.Error(exception = e, message = "Failed to get cached tokens")
            }
        }

    override suspend fun cacheTokens(cache: TokenCache): Result<TokenCache> =
        withContext(Dispatchers.IO) {
            try {
                val params = cache.toInsertParams()
                tokenCacheQueries.insert(
                    text_hash = params.text_hash,
                    model_id = params.model_id,
                    token_ids = params.token_ids,
                    token_count = params.token_count,
                    created_at = params.created_at,
                    last_accessed = params.last_accessed,
                    access_count = params.access_count,
                    source_type = params.source_type,
                    source_id = params.source_id
                )
                Result.Success(cache)
            } catch (e: Exception) {
                Result.Error(exception = e, message = "Failed to cache tokens")
            }
        }

    override suspend fun getOrCacheTokens(
        text: String,
        modelId: String,
        sourceType: TokenCacheSourceType,
        sourceId: String?,
        tokenize: suspend (String) -> List<Int>
    ): Result<List<Int>> = withContext(Dispatchers.IO) {
        try {
            val textHash = computeHash(text)

            // Check cache first
            val cached = tokenCacheQueries.selectByHashAndModel(textHash, modelId)
                .executeAsOneOrNull()

            if (cached != null) {
                // Cache hit - update access stats and return
                tokenCacheQueries.updateAccessStats(
                    last_accessed = System.currentTimeMillis(),
                    text_hash = textHash,
                    model_id = modelId
                )
                return@withContext Result.Success(cached.toDomain().tokenIds)
            }

            // Cache miss - tokenize and cache
            val tokenIds = tokenize(text)
            val now = System.currentTimeMillis()

            val cacheEntry = TokenCache(
                textHash = textHash,
                modelId = modelId,
                tokenIds = tokenIds,
                tokenCount = tokenIds.size,
                createdAt = now,
                lastAccessed = now,
                accessCount = 1,
                sourceType = sourceType,
                sourceId = sourceId
            )

            val params = cacheEntry.toInsertParams()
            tokenCacheQueries.insert(
                text_hash = params.text_hash,
                model_id = params.model_id,
                token_ids = params.token_ids,
                token_count = params.token_count,
                created_at = params.created_at,
                last_accessed = params.last_accessed,
                access_count = params.access_count,
                source_type = params.source_type,
                source_id = params.source_id
            )

            Result.Success(tokenIds)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to get or cache tokens")
        }
    }

    override suspend fun invalidateForModel(modelId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                tokenCacheQueries.deleteByModel(modelId)
                // SQLDelight doesn't return affected row count directly
                Result.Success(0)
            } catch (e: Exception) {
                Result.Error(exception = e, message = "Failed to invalidate cache for model")
            }
        }

    override suspend fun invalidateForSource(
        sourceType: TokenCacheSourceType,
        sourceId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            tokenCacheQueries.deleteBySource(sourceType.name, sourceId)
            Result.Success(0)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to invalidate cache for source")
        }
    }

    override suspend fun getCacheStats(): Result<List<TokenCacheStats>> =
        withContext(Dispatchers.IO) {
            try {
                val stats = tokenCacheQueries.countByModel().executeAsList().map { row ->
                    TokenCacheStats(
                        modelId = row.model_id,
                        entryCount = row.entry_count,
                        totalTokens = row.total_tokens ?: 0L
                    )
                }
                Result.Success(stats)
            } catch (e: Exception) {
                Result.Error(exception = e, message = "Failed to get cache stats")
            }
        }

    override suspend fun getCacheCount(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val count = tokenCacheQueries.count().executeAsOne()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to get cache count")
        }
    }

    override suspend fun cleanupOldEntries(olderThan: Long): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                tokenCacheQueries.deleteOldEntries(olderThan)
                Result.Success(0)
            } catch (e: Exception) {
                Result.Error(exception = e, message = "Failed to cleanup old entries")
            }
        }

    override suspend fun clearCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            tokenCacheQueries.deleteAll()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to clear cache")
        }
    }

    companion object {
        /**
         * Compute MD5 hash of text for cache lookup
         */
        fun computeHash(text: String): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(text.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
