// filename: features/nlu/src/commonMain/kotlin/com/augmentalis/ava/features/nlu/BertTokenizer.kt
// created: 2025-11-02
// author: Claude Code
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - KMP Migration

package com.augmentalis.ava.features.nlu

/**
 * BERT tokenization result
 */
data class TokenizationResult(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        val otherResult = other as? TokenizationResult ?: return false

        if (!inputIds.contentEquals(otherResult.inputIds)) return false
        if (!attentionMask.contentEquals(otherResult.attentionMask)) return false
        if (!tokenTypeIds.contentEquals(otherResult.tokenTypeIds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputIds.contentHashCode()
        result = 31 * result + attentionMask.contentHashCode()
        result = 31 * result + tokenTypeIds.contentHashCode()
        return result
    }
}

/**
 * Cross-platform BERT tokenizer
 *
 * Platform-specific implementations:
 * - Android: TensorFlow Lite Support or custom WordPiece
 * - iOS: Custom WordPiece implementation
 * - Desktop: Custom WordPiece implementation
 * - Web: TensorFlow.js tokenizer
 */
expect class BertTokenizer {
    /**
     * Tokenize text into BERT input format
     * @param text Input text
     * @param maxLength Maximum sequence length (default 128)
     * @return TokenizationResult with input_ids, attention_mask, token_type_ids
     */
    fun tokenize(text: String, maxLength: Int = 128): TokenizationResult

    /**
     * Tokenize a pair of texts (for sentence pair tasks)
     */
    fun tokenizePair(
        textA: String,
        textB: String,
        maxLength: Int = 128
    ): TokenizationResult
}
