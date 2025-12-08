package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.TokenCache
import com.augmentalis.ava.core.domain.model.TokenCacheSourceType
import com.augmentalis.ava.core.domain.model.TokenCacheStats
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.augmentalis.ava.core.data.db.Token_cache as DbTokenCache

/**
 * Mapper functions for Domain TokenCache <-> SQLDelight Token_cache
 *
 * Uses binary BLOB for token IDs (4 bytes per token, little-endian)
 */

/**
 * Convert SQLDelight Token_cache to Domain TokenCache
 */
fun DbTokenCache.toDomain(): TokenCache {
    return TokenCache(
        id = id,
        textHash = text_hash,
        modelId = model_id,
        tokenIds = bytesToIntList(token_ids),
        tokenCount = token_count.toInt(),
        createdAt = created_at,
        lastAccessed = last_accessed,
        accessCount = access_count.toInt(),
        sourceType = TokenCacheSourceType.valueOf(source_type),
        sourceId = source_id
    )
}

/**
 * Convert Domain TokenCache to SQLDelight insert parameters
 */
fun TokenCache.toInsertParams(): TokenCacheInsertParams {
    return TokenCacheInsertParams(
        text_hash = textHash,
        model_id = modelId,
        token_ids = intListToBytes(tokenIds),
        token_count = tokenCount.toLong(),
        created_at = createdAt,
        last_accessed = lastAccessed,
        access_count = accessCount.toLong(),
        source_type = sourceType.name,
        source_id = sourceId
    )
}

/**
 * Parameters for inserting a token cache entry via SQLDelight
 */
data class TokenCacheInsertParams(
    val text_hash: String,
    val model_id: String,
    val token_ids: ByteArray,
    val token_count: Long,
    val created_at: Long,
    val last_accessed: Long,
    val access_count: Long,
    val source_type: String,
    val source_id: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TokenCacheInsertParams) return false
        return text_hash == other.text_hash && model_id == other.model_id
    }

    override fun hashCode(): Int = 31 * text_hash.hashCode() + model_id.hashCode()
}

// Binary token ID conversion utilities

/**
 * Convert ByteArray (BLOB) to List<Int>
 * Each int is stored as 4 bytes in little-endian format
 */
private fun bytesToIntList(bytes: ByteArray): List<Int> {
    if (bytes.isEmpty()) return emptyList()
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val intCount = bytes.size / 4
    return List(intCount) { buffer.getInt() }
}

/**
 * Convert List<Int> to ByteArray (BLOB)
 * Each int is stored as 4 bytes in little-endian format
 */
private fun intListToBytes(ints: List<Int>): ByteArray {
    if (ints.isEmpty()) return ByteArray(0)
    val buffer = ByteBuffer.allocate(ints.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    ints.forEach { buffer.putInt(it) }
    return buffer.array()
}
