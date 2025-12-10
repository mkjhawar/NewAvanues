// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/embeddings/SimpleTokenizer.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

/**
 * Simple whitespace-based tokenizer for initial implementation
 *
 * NOTE: This is a simplified tokenizer for Phase 2 development.
 * Phase 3 will implement proper BERT WordPiece tokenization using
 * ONNX Runtime Extensions or HuggingFace tokenizers.
 *
 * Current limitations:
 * - No subword tokenization
 * - No special tokens ([CLS], [SEP])
 * - No vocabulary mapping
 * - Whitespace splitting only
 *
 * This is sufficient for testing the ONNX Runtime integration
 * and repository functionality.
 */
object SimpleTokenizer {
    private const val MAX_SEQ_LENGTH = 128
    private const val PAD_TOKEN_ID = 0
    private const val UNK_TOKEN_ID = 100
    private const val CLS_TOKEN_ID = 101
    private const val SEP_TOKEN_ID = 102

    /**
     * Tokenization result with input IDs and attention mask
     */
    data class TokenizedInput(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenCount: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as TokenizedInput

            if (!inputIds.contentEquals(other.inputIds)) return false
            if (!attentionMask.contentEquals(other.attentionMask)) return false
            if (tokenCount != other.tokenCount) return false

            return true
        }

        override fun hashCode(): Int {
            var result = inputIds.contentHashCode()
            result = 31 * result + attentionMask.contentHashCode()
            result = 31 * result + tokenCount
            return result
        }
    }

    /**
     * Tokenize text into input IDs and attention mask
     *
     * Simplified approach:
     * 1. Lowercase text
     * 2. Split on whitespace
     * 3. Add [CLS] at start, [SEP] at end
     * 4. Pad to MAX_SEQ_LENGTH
     * 5. Create attention mask (1 for real tokens, 0 for padding)
     *
     * @param text Text to tokenize
     * @return TokenizedInput with inputIds and attentionMask
     */
    fun tokenize(text: String): TokenizedInput {
        // Normalize text
        val normalized = text.lowercase().trim()

        // Split into words (simplified - no subword tokenization)
        val words = normalized.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(MAX_SEQ_LENGTH - 2)  // Reserve space for [CLS] and [SEP]

        // Create input IDs: [CLS] + word_ids + [SEP] + padding
        val inputIds = LongArray(MAX_SEQ_LENGTH) { PAD_TOKEN_ID.toLong() }
        val attentionMask = LongArray(MAX_SEQ_LENGTH) { 0L }

        // Add [CLS] token
        inputIds[0] = CLS_TOKEN_ID.toLong()
        attentionMask[0] = 1L

        // Add word tokens (using simple hash-based IDs for now)
        var position = 1
        for (word in words) {
            inputIds[position] = wordToId(word).toLong()
            attentionMask[position] = 1L
            position++
        }

        // Add [SEP] token
        if (position < MAX_SEQ_LENGTH) {
            inputIds[position] = SEP_TOKEN_ID.toLong()
            attentionMask[position] = 1L
            position++
        }

        return TokenizedInput(
            inputIds = inputIds,
            attentionMask = attentionMask,
            tokenCount = position
        )
    }

    /**
     * Tokenize multiple texts in batch
     */
    fun tokenizeBatch(texts: List<String>): List<TokenizedInput> {
        return texts.map { tokenize(it) }
    }

    /**
     * Simple word-to-ID mapping using hash
     *
     * This is a placeholder for proper vocabulary lookup.
     * Returns a consistent ID for each word, within BERT's typical vocab range.
     */
    private fun wordToId(word: String): Int {
        // Use hash to get consistent ID between 103 and 30000
        // (BERT vocab typically ranges from 0-30522)
        val hash = word.hashCode()
        return 103 + (hash.absoluteValue % 29897)
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}
