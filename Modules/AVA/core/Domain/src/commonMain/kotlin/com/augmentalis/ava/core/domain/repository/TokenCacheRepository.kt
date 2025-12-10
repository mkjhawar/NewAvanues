package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TokenCache
import com.augmentalis.ava.core.domain.model.TokenCacheSourceType
import com.augmentalis.ava.core.domain.model.TokenCacheStats

/**
 * Repository for token cache operations.
 * Provides pre-tokenized content lookup to avoid repeated tokenization.
 *
 * VOS4 Pattern: Model-specific cache with hash-based lookup
 */
interface TokenCacheRepository {

    /**
     * Get cached tokens for text with specific model.
     * Returns null if not cached.
     */
    suspend fun getCachedTokens(textHash: String, modelId: String): Result<TokenCache?>

    /**
     * Cache tokenized content.
     */
    suspend fun cacheTokens(cache: TokenCache): Result<TokenCache>

    /**
     * Get or compute tokens. If cached, returns cached tokens and updates access stats.
     * If not cached, calls tokenize function and caches result.
     */
    suspend fun getOrCacheTokens(
        text: String,
        modelId: String,
        sourceType: TokenCacheSourceType,
        sourceId: String?,
        tokenize: suspend (String) -> List<Int>
    ): Result<List<Int>>

    /**
     * Invalidate all cache entries for a model (when model changes).
     */
    suspend fun invalidateForModel(modelId: String): Result<Int>

    /**
     * Invalidate cache entries for a specific source (when content changes).
     */
    suspend fun invalidateForSource(sourceType: TokenCacheSourceType, sourceId: String): Result<Int>

    /**
     * Get cache statistics by model.
     */
    suspend fun getCacheStats(): Result<List<TokenCacheStats>>

    /**
     * Get total cache entry count.
     */
    suspend fun getCacheCount(): Result<Long>

    /**
     * Cleanup old entries not accessed since timestamp (LRU eviction).
     */
    suspend fun cleanupOldEntries(olderThan: Long): Result<Int>

    /**
     * Clear entire cache.
     */
    suspend fun clearCache(): Result<Unit>
}
