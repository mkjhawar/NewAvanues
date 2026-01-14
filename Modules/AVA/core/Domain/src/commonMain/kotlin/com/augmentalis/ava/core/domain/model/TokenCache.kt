package com.augmentalis.ava.core.domain.model

/**
 * Token cache entry for pre-tokenized database content.
 * Keyed by (textHash, modelId) to support model-specific vocabularies.
 *
 * VOS4 Pattern: Cache invalidation on model change
 */
data class TokenCache(
    val id: Long = 0,
    val textHash: String,
    val modelId: String,
    val tokenIds: List<Int>,
    val tokenCount: Int,
    val createdAt: Long,
    val lastAccessed: Long,
    val accessCount: Int = 0,
    val sourceType: TokenCacheSourceType,
    val sourceId: String? = null
)

/**
 * Source types for token cache entries
 */
enum class TokenCacheSourceType {
    MESSAGE,        // Chat message content
    MEMORY,         // Long-term memory content
    TRAIN_EXAMPLE,  // Training example utterance
    RAG_DOCUMENT,   // RAG document chunk
    SYSTEM_PROMPT,  // System prompt template
    OTHER           // Other content
}

/**
 * Token cache statistics
 */
data class TokenCacheStats(
    val modelId: String,
    val entryCount: Long,
    val totalTokens: Long
)
