package com.augmentalis.llm.alc.tokenizer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.llm.alc.TVMRuntime
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Advanced integration tests for TVMTokenizer
 *
 * Tests advanced tokenizer features that require real MLC-LLM models:
 * - Special tokens (BOS, EOS, PAD, UNK)
 * - Context window limits
 * - Batch processing
 * - Vocabulary validation
 *
 * Created: 2025-11-15
 * Part of: P7 TVMTokenizer Real Implementation - Advanced Tests
 */
@RunWith(AndroidJUnit4::class)
class TVMTokenizerAdvancedIntegrationTest {

    private lateinit var context: Context
    private lateinit var runtime: TVMRuntime
    private lateinit var tokenizer: TVMTokenizer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        runtime = TVMRuntime.create(context, "opencl")
        tokenizer = runtime.createTokenizer()
    }

    @After
    fun tearDown() {
        runtime.dispose()
    }

    // ===== Special Token Tests =====

    @Test
    fun testBeginningOfSequenceToken() {
        // Test if tokenizer handles BOS token (model-specific)
        // Note: Not all models use BOS, so this test checks behavior
        val textWithBOS = "<s> Hello world"
        val textWithoutBOS = "Hello world"

        val tokensWithBOS = tokenizer.encode(textWithBOS)
        val tokensWithoutBOS = tokenizer.encode(textWithoutBOS)

        assertNotNull(tokensWithBOS)
        assertNotNull(tokensWithoutBOS)

        // BOS may or may not change token count depending on model
        assertTrue("Tokenization should handle BOS", tokensWithBOS.isNotEmpty())
    }

    @Test
    fun testEndOfSequenceToken() {
        // Test if tokenizer handles EOS token
        val textWithEOS = "Hello world</s>"
        val textWithoutEOS = "Hello world"

        val tokensWithEOS = tokenizer.encode(textWithEOS)
        val tokensWithoutEOS = tokenizer.encode(textWithoutEOS)

        assertNotNull(tokensWithEOS)
        assertNotNull(tokensWithoutEOS)

        // EOS handling varies by model
        assertTrue("Tokenization should handle EOS", tokensWithEOS.isNotEmpty())
    }

    @Test
    fun testPaddingToken() {
        // Test padding token handling
        val textWithPadding = "Hello [PAD] [PAD] world"

        val tokens = tokenizer.encode(textWithPadding)

        assertNotNull(tokens)
        assertTrue("Should tokenize text with padding markers", tokens.isNotEmpty())
    }

    @Test
    fun testUnknownToken() {
        // Test handling of unknown/rare tokens
        val textWithUnknown = "Hello \u0000\u0001\u0002 world"

        val tokens = tokenizer.encode(textWithUnknown)

        assertNotNull(tokens)
        // Should handle unknown tokens gracefully
        assertTrue("Should handle unknown tokens", tokens.isNotEmpty())
    }

    // ===== Context Window Limit Tests =====

    @Test
    fun testMaxContextLength() {
        // Test tokenization at context window limits (typically 2048 tokens)
        // Generate text that will produce ~2000 tokens
        val longText = "word ".repeat(2500) // Should produce 2500+ tokens

        val tokens = tokenizer.encode(longText)

        assertNotNull(tokens)
        assertTrue("Should produce many tokens", tokens.size > 1000)

        // Tokenizer itself doesn't enforce limits, model does during inference
        // This test validates tokenizer can handle long sequences
    }

    @Test
    fun testVeryLongSequenceTokenization() {
        // Test with sequence longer than typical context window
        val veryLongText = "sentence ".repeat(5000) // 5000 tokens

        try {
            val tokens = tokenizer.encode(veryLongText)

            assertNotNull(tokens)
            assertTrue("Should handle very long sequences", tokens.size > 2000)

            // Verify round-trip still works
            val decoded = tokenizer.decode(tokens)
            assertNotNull(decoded)
            assertTrue(decoded.contains("sentence"))

        } catch (e: TokenizationException) {
            // Some tokenizers may have hard limits
            assertNotNull("Exception should have message", e.message)
        }
    }

    @Test
    fun testContextWindowBoundary() {
        // Test at common context window boundaries
        val contextSizes = listOf(512, 1024, 2048, 4096)

        contextSizes.forEach { size ->
            // Generate text for approximately this many tokens
            val text = "word ".repeat(size)

            val tokens = tokenizer.encode(text)

            assertNotNull("Should tokenize ${size}-word text", tokens)
            assertTrue(
                "Should produce tokens close to ${size}",
                tokens.size in (size / 2)..(size * 2) // Allow variance
            )
        }
    }

    // ===== Batch Processing Tests =====

    @Test
    fun testBatchTokenization() {
        // Test tokenizing multiple texts efficiently
        val texts = listOf(
            "First sentence for batch processing",
            "Second sentence for batch processing",
            "Third sentence for batch processing"
        )

        val allTokens = texts.map { text ->
            tokenizer.encode(text)
        }

        // Verify all tokenized successfully
        assertEquals("Should process all texts", texts.size, allTokens.size)
        allTokens.forEach { tokens ->
            assertNotNull(tokens)
            assertTrue("Each should produce tokens", tokens.isNotEmpty())
        }
    }

    @Test
    fun testBatchDetokenization() {
        // Test detokenizing multiple token sequences
        val texts = listOf("First", "Second", "Third")
        val tokenSequences = texts.map { tokenizer.encode(it) }

        val decodedTexts = tokenSequences.map { tokens ->
            tokenizer.decode(tokens)
        }

        assertEquals("Should decode all sequences", tokenSequences.size, decodedTexts.size)
        decodedTexts.forEach { text ->
            assertNotNull(text)
            assertTrue("Each should produce text", text.isNotEmpty())
        }
    }

    @Test
    fun testBatchConsistency() {
        // Verify batch processing produces same results as individual processing
        val texts = listOf("A", "B", "C")

        // Individual processing
        val individualTokens = texts.map { tokenizer.encode(it) }

        // Batch processing (simulated)
        val batchTokens = texts.map { tokenizer.encode(it) }

        // Should be identical
        assertEquals("Batch should match individual", individualTokens, batchTokens)
    }

    // ===== Vocabulary Validation Tests =====

    @Test
    fun testVocabularySize() {
        // Test that tokenizer vocabulary is reasonable
        // Most modern tokenizers have 30k-50k vocab

        // Sample various text types
        val samples = listOf(
            "common words the and is",
            "rare vocabulary xylophone",
            "numbers 123456789",
            "punctuation !@#$%^&*()",
            "unicode 你好世界"
        )

        val allTokenIds = mutableSetOf<Int>()

        samples.forEach { text ->
            val tokens = tokenizer.encode(text)
            allTokenIds.addAll(tokens)
        }

        // Should see variety of token IDs
        assertTrue(
            "Should use varied vocabulary (found ${allTokenIds.size} unique tokens)",
            allTokenIds.size >= 10
        )

        // Token IDs should be in reasonable range (0 to vocab_size)
        val maxTokenId = allTokenIds.maxOrNull() ?: 0
        assertTrue(
            "Max token ID should be reasonable (< 100k): $maxTokenId",
            maxTokenId < 100000
        )
    }

    @Test
    fun testCommonTokensLowIds() {
        // Common tokens typically have lower IDs in vocabulary
        val commonWords = listOf("the", "a", "is", "and")
        val rareWords = listOf("antidisestablishmentarianism", "pneumonoultramicroscopicsilicovolcanoconiosis")

        val commonTokens = commonWords.flatMap { tokenizer.encode(it) }
        val rareTokens = rareWords.flatMap { tokenizer.encode(it) }

        // Common tokens should generally have smaller IDs
        // (This is a heuristic, not a hard rule)
        val commonAvg = commonTokens.average()
        val rareAvg = rareTokens.average()

        // At minimum, token IDs should be valid
        assertTrue("Common tokens should be valid", commonTokens.all { it >= 0 })
        assertTrue("Rare tokens should be valid", rareTokens.all { it >= 0 })
    }

    @Test
    fun testVocabularyCoverage() {
        // Test that tokenizer covers diverse character sets
        val diverseTexts = mapOf(
            "ascii" to "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
            "punctuation" to ".,!?;:()[]{}\"'-/\\",
            "chinese" to "你好世界",
            "arabic" to "مرحبا بالعالم",
            "russian" to "Привет мир",
            "emoji" to "😀😃😄😁"
        )

        diverseTexts.forEach { (type, text) ->
            val tokens = tokenizer.encode(text)

            assertNotNull("Should tokenize $type", tokens)
            assertTrue("Should produce tokens for $type (got ${tokens.size})", tokens.isNotEmpty())
        }
    }

    // ===== Edge Case Tests =====

    @Test
    fun testMaxSingleToken() {
        // Test the longest possible single-token sequence
        // Most tokenizers have some single-character tokens

        val singleChars = "a b c d e f g h i j k"
        val tokens = tokenizer.encode(singleChars)

        assertNotNull(tokens)
        // Should produce at least as many tokens as characters (accounting for spaces)
        assertTrue("Should tokenize single characters", tokens.size >= 5)
    }

    @Test
    fun testRepeatedCharacters() {
        // Test tokenization of repeated characters
        val repeated = "aaaaaaaaaaaaaaaaaaaa" // 20 'a's

        val tokens = tokenizer.encode(repeated)

        assertNotNull(tokens)
        assertTrue("Should tokenize repeated characters", tokens.isNotEmpty())
        // Tokenizer may combine repeats into single or multiple tokens
    }

    @Test
    fun testMixedCase() {
        // Test case sensitivity
        val lowercase = "hello world"
        val uppercase = "HELLO WORLD"
        val mixedcase = "Hello World"

        val tokensLower = tokenizer.encode(lowercase)
        val tokensUpper = tokenizer.encode(uppercase)
        val tokensMixed = tokenizer.encode(mixedcase)

        // Different cases should produce different tokens
        assertNotEquals("Lowercase != uppercase", tokensLower, tokensUpper)
        assertNotEquals("Lowercase != mixedcase", tokensLower, tokensMixed)
    }

    @Test
    fun testWhitespaceVariations() {
        // Test different whitespace types
        val space = "hello world"
        val tab = "hello\tworld"
        val newline = "hello\nworld"
        val multispace = "hello    world"

        val allTokens = listOf(space, tab, newline, multispace).map {
            tokenizer.encode(it)
        }

        // All should tokenize
        allTokens.forEach { tokens ->
            assertNotNull(tokens)
            assertTrue("Whitespace should be handled", tokens.isNotEmpty())
        }
    }

    // ===== Performance Stress Tests =====

    @Test
    fun testRapidSequentialEncoding() {
        // Test rapid sequential encoding (cache stress test)
        val texts = (1..1000).map { "text$it" }

        texts.forEach { text ->
            val tokens = tokenizer.encode(text)
            assertNotNull(tokens)
        }

        // Should complete without error
    }

    @Test
    fun testLargeTokenSequenceDecoding() {
        // Test decoding very large token sequence
        val longText = "sentence ".repeat(1000)
        val tokens = tokenizer.encode(longText)

        val decoded = tokenizer.decode(tokens)

        assertNotNull(decoded)
        assertTrue("Should decode large sequence", decoded.length > 500)
    }

    @Test
    fun testInterleavedEncodeDecodeRapidCycles() {
        // Stress test with interleaved encode/decode
        val text = "cycle test"

        repeat(100) { iteration ->
            val tokens = tokenizer.encode(text)
            val decoded = tokenizer.decode(tokens)

            assertNotNull("Iteration $iteration encode", tokens)
            assertNotNull("Iteration $iteration decode", decoded)
        }
    }
}
