// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/embeddings/SimpleTokenizerTest.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleTokenizerTest {

    @Test
    fun testTokenizeBasic() {
        val text = "Hello world"
        val result = SimpleTokenizer.tokenize(text)

        // Should have [CLS], "hello", "world", [SEP], padding...
        assertEquals(128, result.inputIds.size, "Should have 128 tokens (max seq length)")
        assertEquals(128, result.attentionMask.size, "Should have 128 mask values")

        // Check special tokens
        assertEquals(101L, result.inputIds[0], "First token should be [CLS]")
        assertEquals(102L, result.inputIds[3], "Should have [SEP] after 2 words")

        // Check attention mask
        assertEquals(1L, result.attentionMask[0], "First token should be attended")
        assertEquals(1L, result.attentionMask[1], "Second token should be attended")
        assertEquals(1L, result.attentionMask[2], "Third token should be attended")
        assertEquals(1L, result.attentionMask[3], "[SEP] should be attended")
        assertEquals(0L, result.attentionMask[4], "Padding should not be attended")

        // Token count should be 4 ([CLS] + 2 words + [SEP])
        assertEquals(4, result.tokenCount)
    }

    @Test
    fun testTokenizeEmpty() {
        val text = ""
        val result = SimpleTokenizer.tokenize(text)

        // Should have [CLS], [SEP], padding...
        assertEquals(101L, result.inputIds[0], "Should have [CLS]")
        assertEquals(102L, result.inputIds[1], "Should have [SEP] immediately")
        assertEquals(2, result.tokenCount, "Token count should be 2")
    }

    @Test
    fun testTokenizeLongText() {
        // Create text longer than MAX_SEQ_LENGTH
        val words = (1..150).map { "word$it" }
        val text = words.joinToString(" ")

        val result = SimpleTokenizer.tokenize(text)

        // Should be truncated to MAX_SEQ_LENGTH
        assertEquals(128, result.inputIds.size)

        // Should have [CLS] at start
        assertEquals(101L, result.inputIds[0])

        // Count real tokens (non-padding)
        val realTokenCount = result.attentionMask.count { it == 1L }

        // Should be close to MAX_SEQ_LENGTH (126 words + [CLS] + [SEP])
        assertTrue(realTokenCount <= 128, "Should not exceed max seq length")
        assertTrue(realTokenCount >= 100, "Should have substantial number of tokens")
    }

    @Test
    fun testTokenizeWithPunctuation() {
        val text = "Hello, world! How are you?"
        val result = SimpleTokenizer.tokenize(text)

        // Punctuation is kept with words in this simple tokenizer
        assertTrue(result.tokenCount > 2, "Should tokenize words with punctuation")
        assertTrue(result.tokenCount < 10, "Should not over-tokenize")
    }

    @Test
    fun testTokenizeCaseNormalization() {
        val text1 = "Hello World"
        val text2 = "hello world"

        val result1 = SimpleTokenizer.tokenize(text1)
        val result2 = SimpleTokenizer.tokenize(text2)

        // Should produce same tokens (case-insensitive)
        assertEquals(result1.tokenCount, result2.tokenCount)

        // Token IDs should match (after CLS)
        assertEquals(result1.inputIds[1], result2.inputIds[1], "First word should match")
        assertEquals(result1.inputIds[2], result2.inputIds[2], "Second word should match")
    }

    @Test
    fun testTokenizeBatch() {
        val texts = listOf(
            "Hello world",
            "How are you",
            "This is a test"
        )

        val results = SimpleTokenizer.tokenizeBatch(texts)

        assertEquals(3, results.size, "Should tokenize all texts")

        // Each result should be valid
        results.forEach { result ->
            assertEquals(128, result.inputIds.size)
            assertEquals(128, result.attentionMask.size)
            assertEquals(101L, result.inputIds[0], "Should start with [CLS]")
            assertTrue(result.tokenCount > 0, "Should have tokens")
        }
    }

    @Test
    fun testTokenizeConsistency() {
        val text = "The quick brown fox"

        // Tokenize same text multiple times
        val result1 = SimpleTokenizer.tokenize(text)
        val result2 = SimpleTokenizer.tokenize(text)

        // Should produce identical results
        assertTrue(result1.inputIds.contentEquals(result2.inputIds), "Input IDs should match")
        assertTrue(result1.attentionMask.contentEquals(result2.attentionMask), "Attention mask should match")
        assertEquals(result1.tokenCount, result2.tokenCount, "Token count should match")
    }

    @Test
    fun testTokenizeWhitespace() {
        val text = "   Hello    world   "
        val result = SimpleTokenizer.tokenize(text)

        // Should handle extra whitespace correctly
        // [CLS] + "hello" + "world" + [SEP] = 4 tokens
        assertEquals(4, result.tokenCount)
    }

    @Test
    fun testAttentionMaskCorrectness() {
        val text = "One two three"
        val result = SimpleTokenizer.tokenize(text)

        // Count real tokens (where mask = 1)
        val realTokens = result.attentionMask.count { it == 1L }
        assertEquals(result.tokenCount, realTokens, "Attention mask count should match token count")

        // After real tokens, all should be padding (mask = 0)
        for (i in result.tokenCount until result.attentionMask.size) {
            assertEquals(0L, result.attentionMask[i], "Padding should have mask = 0 at position $i")
        }
    }

    @Test
    fun testPaddingCorrectness() {
        val text = "Hello"
        val result = SimpleTokenizer.tokenize(text)

        // After real tokens, all should be PAD (ID = 0)
        for (i in result.tokenCount until result.inputIds.size) {
            assertEquals(0L, result.inputIds[i], "Padding should have ID = 0 at position $i")
        }
    }
}
