package com.augmentalis.llm.cache

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TokenCacheSourceType
import com.augmentalis.ava.core.domain.model.TokenCacheStats as CoreTokenCacheStats
import com.augmentalis.ava.core.domain.repository.TokenCacheRepository
import com.augmentalis.llm.alc.tokenizer.HuggingFaceTokenizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.security.MessageDigest

/**
 * Manager for token caching operations.
 * Provides a convenient interface for the LLM module to cache tokenized content.
 *
 * Benefits:
 * - Avoids repeated tokenization of frequently-used content
 * - Model-aware: Invalidates cache when model changes
 * - Source-aware: Invalidates when source content changes
 *
 * Usage:
 * ```kotlin
 * val tokens = tokenCacheManager.getOrTokenize(
 *     text = "Hello, world!",
 *     sourceType = TokenCacheSourceType.MESSAGE,
 *     sourceId = "msg-123"
 * )
 * ```
 */
class TokenCacheManager(
    private val tokenCacheRepository: TokenCacheRepository
) {
    private var currentModelId: String? = null
    private var tokenizer: HuggingFaceTokenizer? = null
    private val mutex = Mutex()

    // Cache stats
    private var cacheHits = 0
    private var cacheMisses = 0

    /**
     * Set the tokenizer and model ID.
     * If model ID changes, invalidates all cached tokens for the old model.
     */
    suspend fun setTokenizer(tokenizer: HuggingFaceTokenizer, modelId: String) = mutex.withLock {
        val oldModelId = currentModelId

        if (oldModelId != null && oldModelId != modelId) {
            // Model changed - invalidate old cache
            Timber.i("TokenCacheManager: Model changed from $oldModelId to $modelId, invalidating cache")
            tokenCacheRepository.invalidateForModel(oldModelId)
        }

        this.tokenizer = tokenizer
        this.currentModelId = modelId
        Timber.d("TokenCacheManager: Tokenizer set for model $modelId")
    }

    /**
     * Get tokens for text, using cache if available.
     * Falls back to tokenizing if not cached.
     *
     * @param text Text to tokenize
     * @param sourceType Source type for cache tracking
     * @param sourceId Optional source ID for invalidation
     * @return Token IDs or null if tokenizer not set
     */
    suspend fun getOrTokenize(
        text: String,
        sourceType: TokenCacheSourceType,
        sourceId: String? = null
    ): List<Int>? = mutex.withLock {
        val tok = tokenizer ?: run {
            Timber.w("TokenCacheManager: Tokenizer not set, cannot tokenize")
            return null
        }
        val model = currentModelId ?: return null

        val result = tokenCacheRepository.getOrCacheTokens(
            text = text,
            modelId = model,
            sourceType = sourceType,
            sourceId = sourceId,
            tokenize = { txt ->
                tok.encode(txt)
            }
        )

        when (result) {
            is Result.Success -> {
                // Check if this was a cache hit or miss by looking at stats
                val newCount = (tokenCacheRepository.getCacheCount() as? Result.Success)?.data ?: 0L
                result.data
            }
            is Result.Error -> {
                Timber.e(result.exception, "TokenCacheManager: Failed to get/cache tokens")
                // Fall back to direct tokenization
                tok.encode(text)
            }
        }
    }

    /**
     * Tokenize multiple texts, using cache where possible.
     * More efficient than calling getOrTokenize multiple times.
     *
     * @param texts List of (text, sourceType, sourceId?) tuples
     * @return List of token ID lists
     */
    suspend fun getOrTokenizeMany(
        texts: List<Triple<String, TokenCacheSourceType, String?>>
    ): List<List<Int>> = mutex.withLock {
        val tok = tokenizer ?: return texts.map { emptyList() }

        texts.map { (text, sourceType, sourceId) ->
            val model = currentModelId ?: return@map emptyList()

            val result = tokenCacheRepository.getOrCacheTokens(
                text = text,
                modelId = model,
                sourceType = sourceType,
                sourceId = sourceId,
                tokenize = { txt -> tok.encode(txt) }
            )

            when (result) {
                is Result.Success -> result.data
                is Result.Error -> tok.encode(text)
            }
        }
    }

    /**
     * Invalidate cache for a specific source (e.g., when message is edited).
     */
    suspend fun invalidateSource(sourceType: TokenCacheSourceType, sourceId: String) {
        tokenCacheRepository.invalidateForSource(sourceType, sourceId)
        Timber.d("TokenCacheManager: Invalidated cache for $sourceType:$sourceId")
    }

    /**
     * Invalidate all cache for current model.
     */
    suspend fun invalidateCurrentModel() = mutex.withLock {
        currentModelId?.let { modelId ->
            tokenCacheRepository.invalidateForModel(modelId)
            Timber.i("TokenCacheManager: Invalidated all cache for model $modelId")
        }
    }

    /**
     * Cleanup old cache entries.
     *
     * @param maxAgeMs Maximum age in milliseconds (default: 7 days)
     */
    suspend fun cleanupOldEntries(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        val olderThan = System.currentTimeMillis() - maxAgeMs
        tokenCacheRepository.cleanupOldEntries(olderThan)
        Timber.d("TokenCacheManager: Cleaned up entries older than $maxAgeMs ms")
    }

    /**
     * Get cache statistics.
     */
    suspend fun getStats(): TokenCacheStats {
        val count = (tokenCacheRepository.getCacheCount() as? Result.Success)?.data ?: 0L
        val modelStats = (tokenCacheRepository.getCacheStats() as? Result.Success)?.data ?: emptyList()

        return TokenCacheStats(
            totalEntries = count,
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            hitRate = if (cacheHits + cacheMisses > 0) {
                cacheHits.toFloat() / (cacheHits + cacheMisses)
            } else 0f,
            currentModelId = currentModelId,
            modelStats = modelStats.associate { it.modelId to it.entryCount }
        )
    }

    /**
     * Clear entire cache.
     */
    suspend fun clearCache() {
        tokenCacheRepository.clearCache()
        cacheHits = 0
        cacheMisses = 0
        Timber.i("TokenCacheManager: Cache cleared")
    }

    companion object {
        /**
         * Compute hash for text (used for cache lookup).
         */
        fun computeHash(text: String): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(text.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Token cache statistics.
 */
data class TokenCacheStats(
    val totalEntries: Long,
    val cacheHits: Int,
    val cacheMisses: Int,
    val hitRate: Float,
    val currentModelId: String?,
    val modelStats: Map<String, Long>
)
