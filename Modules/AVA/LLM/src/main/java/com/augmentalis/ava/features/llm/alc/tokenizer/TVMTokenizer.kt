/**
 * TVM Tokenizer Service
 *
 * Single Responsibility: Convert between text and token IDs using TVM runtime
 *
 * Wraps TVM runtime tokenizer with a clean interface for encoding/decoding.
 * Handles errors gracefully and provides caching for common sequences.
 *
 * Created: 2025-10-31
 */

package com.augmentalis.ava.features.llm.alc.tokenizer

import com.augmentalis.ava.features.llm.alc.TVMRuntime
import com.augmentalis.ava.features.llm.alc.streaming.ITokenizer
import timber.log.Timber

/**
 * Tokenizer implementation using TVM runtime
 *
 * @param runtime TVM runtime instance with tokenizer functions loaded
 */
class TVMTokenizer(
    private val runtime: TVMRuntime
) : ITokenizer {

    // Simple cache for common tokens (e.g., spaces, punctuation)
    private val encodeCache = mutableMapOf<String, List<Int>>()
    private val decodeCache = mutableMapOf<List<Int>, String>()

    override fun encode(text: String): List<Int> {
        return try {
            // Check cache first
            encodeCache[text]?.let { return it }

            // Encode via TVM runtime
            val tokens = runtime.tokenize(text)

            // Cache if it's a small, common sequence
            if (text.length <= CACHE_TEXT_LENGTH_LIMIT && encodeCache.size < MAX_CACHE_SIZE) {
                encodeCache[text] = tokens
            }

            tokens
        } catch (e: Exception) {
            Timber.e(e, "Failed to encode text: ${text.take(50)}...")
            throw TokenizationException("Encoding failed: ${e.message}", e)
        }
    }

    override fun decode(tokens: List<Int>): String {
        return try {
            // Check cache first
            decodeCache[tokens]?.let { return it }

            // Decode via TVM runtime
            val text = runtime.detokenize(tokens)

            // Cache if it's a small sequence
            if (tokens.size <= CACHE_TOKEN_LENGTH_LIMIT && decodeCache.size < MAX_CACHE_SIZE) {
                decodeCache[tokens] = text
            }

            text
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode tokens: ${tokens.take(10)}...")
            throw TokenizationException("Decoding failed: ${e.message}", e)
        }
    }

    /**
     * Clear tokenization caches
     */
    fun clearCache() {
        encodeCache.clear()
        decodeCache.clear()
        Timber.d("Tokenization caches cleared")
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "encode_cache_size" to encodeCache.size,
            "decode_cache_size" to decodeCache.size
        )
    }

    companion object {
        private const val CACHE_TEXT_LENGTH_LIMIT = 10 // Cache strings <= 10 chars
        private const val CACHE_TOKEN_LENGTH_LIMIT = 5 // Cache token sequences <= 5 tokens
        private const val MAX_CACHE_SIZE = 1000 // Max entries per cache
    }
}

/**
 * Exception thrown during tokenization
 */
class TokenizationException(message: String, cause: Throwable? = null) : Exception(message, cause)
