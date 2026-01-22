// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/parser/TokenCounter.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.parser

/**
 * Simple token counter for text chunking
 *
 * Uses whitespace-based tokenization as an approximation.
 * This is faster than proper subword tokenization and sufficient for chunking.
 */
object TokenCounter {
    /**
     * Count tokens in text using whitespace splitting
     *
     * This approximates GPT-style tokenization:
     * - ~1.3 tokens per word on average
     * - Punctuation counts as separate tokens
     *
     * @param text Text to count tokens in
     * @return Approximate token count
     */
    fun countTokens(text: String): Int {
        if (text.isEmpty()) return 0

        // Split on whitespace and count words
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

        // Approximate: 1.3 tokens per word
        // This accounts for subword splitting (e.g., "running" → "run" + "ning")
        return (words.size * 1.3).toInt()
    }

    /**
     * Count tokens in a substring
     *
     * @param text Full text
     * @param startOffset Start character offset
     * @param endOffset End character offset
     * @return Token count for substring
     */
    fun countTokens(text: String, startOffset: Int, endOffset: Int): Int {
        if (startOffset >= endOffset || startOffset >= text.length) return 0

        val substring = text.substring(
            startOffset.coerceIn(0, text.length),
            endOffset.coerceIn(0, text.length)
        )

        return countTokens(substring)
    }

    /**
     * Find character offset for a target token count
     *
     * Returns the character offset that approximately corresponds to
     * the target token count, searching forward from startOffset.
     *
     * @param text Full text
     * @param startOffset Starting character offset
     * @param targetTokens Target token count
     * @return Character offset for target tokens
     */
    fun findOffsetForTokenCount(
        text: String,
        startOffset: Int,
        targetTokens: Int
    ): Int {
        if (startOffset >= text.length) return text.length
        if (targetTokens <= 0) return startOffset

        val substring = text.substring(startOffset)
        val words = substring.split(Regex("\\s+"))

        // Calculate approximate words needed
        val targetWords = (targetTokens / 1.3).toInt()

        if (targetWords >= words.size) return text.length

        // Find character offset by counting words
        var currentOffset = startOffset
        var wordCount = 0

        for (i in words.indices) {
            if (wordCount >= targetWords) break

            val word = words[i]
            val wordStart = text.indexOf(word, currentOffset)
            if (wordStart == -1) break

            currentOffset = wordStart + word.length
            wordCount++
        }

        return currentOffset.coerceAtMost(text.length)
    }

    /**
     * Split text into token boundaries
     *
     * Returns character offsets that correspond to natural token boundaries
     * (whitespace between words).
     *
     * @param text Text to split
     * @param maxTokens Maximum tokens per chunk
     * @return List of character offsets for chunk boundaries
     */
    fun getTokenBoundaries(text: String, maxTokens: Int): List<Int> {
        val boundaries = mutableListOf(0)
        var currentOffset = 0

        while (currentOffset < text.length) {
            val nextOffset = findOffsetForTokenCount(text, currentOffset, maxTokens)

            if (nextOffset <= currentOffset) break

            boundaries.add(nextOffset)
            currentOffset = nextOffset
        }

        // Always include end of text
        if (boundaries.last() != text.length) {
            boundaries.add(text.length)
        }

        return boundaries
    }
}
