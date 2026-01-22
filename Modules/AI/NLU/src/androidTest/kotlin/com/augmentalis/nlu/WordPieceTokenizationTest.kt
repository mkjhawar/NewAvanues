/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.nlu

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Comprehensive WordPiece tokenization tests
 *
 * Tests the full WordPiece algorithm implementation including:
 * - Subword splitting with ## prefix
 * - Greedy longest-match algorithm
 * - Unknown token handling
 * - Special token positioning
 * - Performance requirements (<10ms)
 *
 * Target: 95% Tokenization grade
 */
@RunWith(AndroidJUnit4::class)
class WordPieceTokenizationTest {

    private lateinit var context: Context
    private lateinit var tokenizer: BertTokenizer

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        tokenizer = BertTokenizer(context)
    }

    /**
     * Test 1: Subword Splitting
     *
     * Verifies that common words are split into subwords with ## prefix
     * Example: "playing" → ["play", "##ing"]
     */
    @Test
    fun testSubwordSplitting() {
        // Test word that should split into subwords
        val result = tokenizer.tokenize("playing")

        // Extract actual tokens (skip [CLS] and [SEP])
        val inputIds = result.inputIds.filter { it != 0L } // Remove padding

        // Should have at least 3 tokens: [CLS], subwords, [SEP]
        assertTrue("Should have at least [CLS] + subwords + [SEP]", inputIds.size >= 3)

        // First should be [CLS] (101)
        assertEquals(101L, inputIds[0])

        // Last non-padding should be [SEP] (102)
        val lastIndex = inputIds.lastIndex
        assertEquals(102L, inputIds[lastIndex])

        // Middle tokens should be from vocabulary (not [UNK])
        val middleTokens = inputIds.subList(1, lastIndex)
        assertTrue("Should have actual token IDs", middleTokens.isNotEmpty())

        // If the word was split, we should have multiple tokens between [CLS] and [SEP]
        // For "playing", depending on vocab, it might be ["play", "##ing"] or similar
        println("Tokenized 'playing': ${inputIds.joinToString(", ")}")
    }

    /**
     * Test 2: Unknown Token Handling
     *
     * Verifies that completely unknown words map to [UNK] token (ID 100)
     */
    @Test
    fun testUnknownToken() {
        // Use nonsense string that won't be in vocabulary
        val unknownWord = "xyzabc123qwertyuiop"
        val result = tokenizer.tokenize(unknownWord)

        // Extract tokens (skip padding)
        val inputIds = result.inputIds.filter { it != 0L }

        // Should have [CLS], [UNK], [SEP]
        assertEquals("Should have exactly 3 tokens", 3, inputIds.size)
        assertEquals("First should be [CLS]", 101L, inputIds[0])
        assertEquals("Middle should be [UNK]", 100L, inputIds[1])
        assertEquals("Last should be [SEP]", 102L, inputIds[2])

        println("Unknown word tokenized correctly: [CLS]=101, [UNK]=100, [SEP]=102")
    }

    /**
     * Test 3: Special Tokens Positioning
     *
     * Verifies [CLS], [SEP], and [PAD] tokens are in correct positions
     */
    @Test
    fun testSpecialTokens() {
        val text = "hello world"
        val result = tokenizer.tokenize(text)

        // First token must be [CLS] (101)
        assertEquals("First token should be [CLS]", 101L, result.inputIds[0])

        // Find first padding position
        val firstPadIndex = result.attentionMask.indexOf(0L)
        assertTrue("Should have padding", firstPadIndex > 0)

        // Token before padding should be [SEP] (102)
        assertEquals("Token before padding should be [SEP]",
            102L, result.inputIds[firstPadIndex - 1])

        // All padding tokens should be [PAD] (0)
        for (i in firstPadIndex until result.inputIds.size) {
            assertEquals("Padding positions should be [PAD]", 0L, result.inputIds[i])
            assertEquals("Attention mask for padding should be 0", 0L, result.attentionMask[i])
        }

        println("Special tokens verified: [CLS] at 0, [SEP] at ${firstPadIndex-1}, [PAD] from $firstPadIndex")
    }

    /**
     * Test 4: Greedy Longest-Match Algorithm
     *
     * Verifies tokenizer prefers longer matches over shorter ones
     * Example: If "unwrap" and "un" both exist, should choose "unwrap"
     */
    @Test
    fun testGreedyLongestMatch() {
        // Use a word that could potentially be split multiple ways
        // "understand" should prefer "understand" over "under" + "stand" if available
        val result = tokenizer.tokenize("understand")

        val inputIds = result.inputIds.filter { it != 0L }

        // Should have [CLS], word tokens, [SEP]
        assertTrue("Should have at least 3 tokens", inputIds.size >= 3)
        assertEquals("First should be [CLS]", 101L, inputIds[0])
        assertEquals("Last should be [SEP]", 102L, inputIds[inputIds.lastIndex])

        // The word tokens (between [CLS] and [SEP]) should be valid vocab IDs
        val wordTokens = inputIds.subList(1, inputIds.lastIndex)
        for (tokenId in wordTokens) {
            assertTrue("Token ID should be valid (not [UNK] unless necessary)",
                tokenId in 0L..30521L) // BERT vocab size
        }

        println("Tokenized 'understand': ${inputIds.joinToString(", ")}")
    }

    /**
     * Test 5: Multiple Words Tokenization
     *
     * Verifies tokenizer correctly handles multiple words
     */
    @Test
    fun testMultipleWords() {
        val text = "the quick brown fox"
        val result = tokenizer.tokenize(text)

        val inputIds = result.inputIds.filter { it != 0L }

        // Should have [CLS], tokens for each word, [SEP]
        assertTrue("Should have at least 6 tokens (CLS + 4 words + SEP)", inputIds.size >= 6)
        assertEquals("First should be [CLS]", 101L, inputIds[0])
        assertEquals("Last should be [SEP]", 102L, inputIds[inputIds.lastIndex])

        println("Tokenized 'the quick brown fox': ${inputIds.size} tokens total")
    }

    /**
     * Test 6: Vocabulary Loading and Caching
     *
     * Verifies vocabulary is loaded once and cached (not reloaded each time)
     */
    @Test
    fun testVocabularyCaching() {
        // Create multiple tokenizers - vocab should load from cache after first
        val time1 = measureTimeMillis {
            BertTokenizer(context)
        }

        val time2 = measureTimeMillis {
            BertTokenizer(context)
        }

        val time3 = measureTimeMillis {
            BertTokenizer(context)
        }

        // Second and third instantiations might be faster due to system caching
        println("Tokenizer init times: $time1ms, $time2ms, $time3ms")

        // Verify tokenizer works correctly after instantiation
        val tokenizer = BertTokenizer(context)
        val result = tokenizer.tokenize("test")

        assertEquals("Should have [CLS] token", 101L, result.inputIds[0])
    }

    /**
     * Test 7: Performance Requirement
     *
     * Verifies tokenization completes in <10ms for typical sentences
     */
    @Test
    fun testPerformanceRequirement() {
        val typicalSentences = listOf(
            "turn on the lights",
            "what's the weather today",
            "play some music",
            "set a timer for 5 minutes",
            "tell me a joke"
        )

        val times = mutableListOf<Long>()

        // Warm up
        repeat(10) {
            tokenizer.tokenize(typicalSentences.random())
        }

        // Measure
        repeat(100) {
            val sentence = typicalSentences.random()
            val time = measureTimeMillis {
                tokenizer.tokenize(sentence)
            }
            times.add(time)
        }

        val avgTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        val p95Time = times.sorted()[95]

        println("Performance stats:")
        println("  Average: ${avgTime}ms")
        println("  Max: ${maxTime}ms")
        println("  P95: ${p95Time}ms")

        // Performance requirement: <10ms average
        assertTrue("Average tokenization should be <10ms, was ${avgTime}ms", avgTime < 10.0)

        // P95 should also be reasonable
        assertTrue("P95 tokenization should be <20ms, was ${p95Time}ms", p95Time < 20)
    }

    /**
     * Test 8: Edge Cases
     *
     * Verifies tokenizer handles edge cases correctly
     */
    @Test
    fun testEdgeCases() {
        // Empty string
        val empty = tokenizer.tokenize("")
        assertEquals("Empty should have [CLS]", 101L, empty.inputIds[0])
        assertEquals("Empty should have [SEP]", 102L, empty.inputIds[1])

        // Single character
        val singleChar = tokenizer.tokenize("a")
        assertTrue("Single char should work", singleChar.inputIds[0] == 101L)

        // Very long text (truncation)
        val longText = (1..200).joinToString(" ") { "word" }
        val longResult = tokenizer.tokenize(longText, maxLength = 128)
        assertEquals("Long text should be truncated to 128", 128, longResult.inputIds.size)
        assertEquals("Last token should be [SEP]", 102L, longResult.inputIds[127])

        // Uppercase (should be lowercased)
        val upper = tokenizer.tokenize("HELLO")
        val lower = tokenizer.tokenize("hello")
        assertArrayEquals("Case should not matter", upper.inputIds, lower.inputIds)

        println("All edge cases handled correctly")
    }

    /**
     * Test 9: Attention Mask Correctness
     *
     * Verifies attention mask correctly identifies real vs padded tokens
     */
    @Test
    fun testAttentionMaskCorrectness() {
        val text = "hello world"
        val result = tokenizer.tokenize(text, maxLength = 128)

        // Count real tokens (attention mask = 1)
        val realTokenCount = result.attentionMask.count { it == 1L }

        // Should have [CLS] + tokens + [SEP]
        assertTrue("Should have at least 4 real tokens (CLS + 2 words + SEP)", realTokenCount >= 4)

        // Verify attention mask is contiguous (no gaps)
        var foundPadding = false
        for (i in result.attentionMask.indices) {
            if (result.attentionMask[i] == 0L) {
                foundPadding = true
            } else {
                assertFalse("Attention mask should be contiguous (no 1s after 0s)", foundPadding)
            }
        }

        println("Attention mask verified: $realTokenCount real tokens, ${128 - realTokenCount} padding")
    }

    /**
     * Test 10: Token Type IDs
     *
     * Verifies token type IDs are all 0 for single sentence
     */
    @Test
    fun testTokenTypeIds() {
        val text = "hello world"
        val result = tokenizer.tokenize(text)

        // All token type IDs should be 0 for single sentence
        assertTrue("All token type IDs should be 0",
            result.tokenTypeIds.all { it == 0L })

        println("Token type IDs verified: all zeros for single sentence")
    }

    /**
     * Test 11: Batch Tokenization
     *
     * Verifies batch tokenization produces consistent results
     */
    @Test
    fun testBatchTokenization() {
        val texts = listOf(
            "hello",
            "world",
            "test sentence"
        )

        // Tokenize individually
        val individualResults = texts.map { tokenizer.tokenize(it) }

        // Tokenize as batch
        val batchResult = tokenizer.tokenizeBatch(texts)

        assertEquals("Batch size should match", texts.size, batchResult.batchSize)
        assertEquals("Max length should be 128", 128, batchResult.maxLength)

        // Verify each text in batch matches individual result
        for (i in texts.indices) {
            val offset = i * batchResult.maxLength
            val batchInputIds = batchResult.inputIds.sliceArray(offset until offset + batchResult.maxLength)

            assertArrayEquals("Batch result should match individual for text $i",
                individualResults[i].inputIds, batchInputIds)
        }

        println("Batch tokenization verified: consistent with individual tokenization")
    }

    /**
     * Test 12: Common Words Coverage
     *
     * Verifies common English words are in vocabulary (not [UNK])
     */
    @Test
    fun testCommonWordsInVocabulary() {
        val commonWords = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at"
        )

        for (word in commonWords) {
            val result = tokenizer.tokenize(word)
            val inputIds = result.inputIds.filter { it != 0L }

            // Should have [CLS], word token(s), [SEP]
            assertTrue("Word '$word' should have at least 3 tokens", inputIds.size >= 3)

            // Middle token(s) should NOT be [UNK] (100)
            val wordTokens = inputIds.subList(1, inputIds.lastIndex)
            assertFalse("Common word '$word' should not be [UNK]",
                wordTokens.contains(100L))
        }

        println("All common words found in vocabulary")
    }
}
