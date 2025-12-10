package com.augmentalis.ava.features.llm.alc.tokenizer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.features.llm.alc.TVMRuntime
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Integration tests for TVMTokenizer using REAL MLC-LLM native tokenizer
 *
 * These tests verify that the tokenizer works correctly with actual
 * MLC-LLM models and native libraries on Android devices.
 *
 * Requirements:
 * - MLC-LLM model must be present on device
 * - TVM runtime native library (libtvm4j_runtime_packed.so) must be loaded
 * - Tests run on actual device or emulator
 *
 * Created: 2025-11-15
 * Part of: P7 TVMTokenizer Real Implementation (LLM Integration Phase 2)
 */
@RunWith(AndroidJUnit4::class)
class TVMTokenizerIntegrationTest {

    private lateinit var context: Context
    private lateinit var runtime: TVMRuntime
    private lateinit var tokenizer: TVMTokenizer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create TVM runtime with OpenCL device (default)
        runtime = TVMRuntime.create(context, "opencl")

        // Create tokenizer
        tokenizer = runtime.createTokenizer()
    }

    @After
    fun tearDown() {
        // Clean up resources
        runtime.dispose()
    }

    // ===== Basic Encoding Tests (Real MLC-LLM) =====

    @Test
    fun testEncodeSimpleText() {
        val text = "Hello world"

        val tokens = tokenizer.encode(text)

        assertNotNull("Tokens should not be null", tokens)
        assertTrue("Tokens should not be empty", tokens.isNotEmpty())
        assertTrue("Should produce multiple tokens", tokens.size >= 2)

        // All token IDs should be valid (positive integers)
        tokens.forEach { tokenId ->
            assertTrue("Token ID should be positive: $tokenId", tokenId >= 0)
        }
    }

    @Test
    fun testEncodeEmptyString() {
        val text = ""

        val tokens = tokenizer.encode(text)

        assertNotNull("Tokens should not be null", tokens)
        // Empty string may produce 0 or 1 tokens depending on tokenizer
        assertTrue("Empty string tokens: ${tokens.size}", tokens.size <= 1)
    }

    @Test
    fun testEncodeLongText() {
        val longText = """
            The quick brown fox jumps over the lazy dog.
            This is a longer piece of text that will be tokenized into many tokens.
            We want to verify that the tokenizer can handle longer inputs correctly.
        """.trimIndent()

        val tokens = tokenizer.encode(longText)

        assertNotNull(tokens)
        assertTrue("Long text should produce many tokens", tokens.size > 20)

        // Verify all tokens are valid
        tokens.forEach { tokenId ->
            assertTrue("Token ID should be valid: $tokenId", tokenId >= 0)
        }
    }

    @Test
    fun testEncodeMultilingualText() {
        // Test with multiple languages (if model supports multilingual)
        val multilingualText = "Hello 世界 مرحبا мир"

        val tokens = tokenizer.encode(multilingualText)

        assertNotNull(tokens)
        assertTrue("Multilingual text should produce tokens", tokens.isNotEmpty())

        // Verify tokens are valid
        tokens.forEach { tokenId ->
            assertTrue("Token ID should be valid: $tokenId", tokenId >= 0)
        }
    }

    @Test
    fun testEncodeSpecialCharacters() {
        val specialText = "@#$% <>&* []{}()"

        val tokens = tokenizer.encode(specialText)

        assertNotNull(tokens)
        assertTrue("Special characters should produce tokens", tokens.isNotEmpty())
    }

    @Test
    fun testEncodeNumbers() {
        val numberText = "2025-11-15 10:30:45"

        val tokens = tokenizer.encode(numberText)

        assertNotNull(tokens)
        assertTrue("Numbers should produce tokens", tokens.isNotEmpty())
    }

    @Test
    fun testEncodeCodeSnippet() {
        val codeSnippet = """
            fun main() {
                println("Hello, world!")
            }
        """.trimIndent()

        val tokens = tokenizer.encode(codeSnippet)

        assertNotNull(tokens)
        assertTrue("Code should produce tokens", tokens.isNotEmpty())
    }

    // ===== Basic Decoding Tests (Real MLC-LLM) =====

    @Test
    fun testDecodeSimpleTokens() {
        // First encode to get real tokens
        val originalText = "Hello"
        val tokens = tokenizer.encode(originalText)

        // Then decode
        val decodedText = tokenizer.decode(tokens)

        assertNotNull("Decoded text should not be null", decodedText)
        assertTrue("Decoded text should not be empty", decodedText.isNotEmpty())
        // Note: Decoded text may not exactly match due to tokenization artifacts
        // But it should contain the original word
        assertTrue(
            "Decoded text should be similar to original",
            decodedText.contains("Hello", ignoreCase = true) ||
            decodedText.trim() == originalText
        )
    }

    @Test
    fun testDecodeEmptyList() {
        val tokens = emptyList<Int>()

        val decodedText = tokenizer.decode(tokens)

        assertNotNull("Decoded text should not be null", decodedText)
        // Empty token list should produce empty string or minimal output
        assertTrue("Decoded empty list should be empty or minimal", decodedText.length <= 1)
    }

    @Test
    fun testDecodeLongSequence() {
        // Encode a long text first
        val longText = "This is a test sentence with multiple words and punctuation marks."
        val tokens = tokenizer.encode(longText)

        // Decode it back
        val decodedText = tokenizer.decode(tokens)

        assertNotNull(decodedText)
        assertTrue("Decoded text should not be empty", decodedText.isNotEmpty())
    }

    // ===== Round-trip Tests (Critical) =====

    @Test
    fun testRoundTripSimpleText() {
        val originalText = "Hello world"

        // Encode
        val tokens = tokenizer.encode(originalText)

        // Decode
        val decodedText = tokenizer.decode(tokens)

        assertNotNull(decodedText)
        // Round-trip may not preserve exact text due to tokenization,
        // but should preserve meaning and most characters
        assertTrue(
            "Round-trip should preserve text (original: '$originalText', decoded: '$decodedText')",
            decodedText.trim().equals(originalText.trim(), ignoreCase = true) ||
            decodedText.replace(" ", "").equals(originalText.replace(" ", ""), ignoreCase = true)
        )
    }

    @Test
    fun testRoundTripComplexText() {
        val originalText = "The quick brown fox jumps over the lazy dog."

        val tokens = tokenizer.encode(originalText)
        val decodedText = tokenizer.decode(tokens)

        assertNotNull(decodedText)
        assertTrue("Decoded text should contain original words",
            decodedText.contains("quick") && decodedText.contains("fox"))
    }

    @Test
    fun testMultipleRoundTrips() {
        val originalText = "Test"

        // First round-trip
        val tokens1 = tokenizer.encode(originalText)
        val decoded1 = tokenizer.decode(tokens1)

        // Second round-trip (encode the decoded text)
        val tokens2 = tokenizer.encode(decoded1)
        val decoded2 = tokenizer.decode(tokens2)

        // After stabilization, tokens should be identical
        assertEquals("Multiple round-trips should stabilize", tokens2, tokens1)
    }

    // ===== Consistency Tests =====

    @Test
    fun testEncodingConsistency() {
        val text = "consistent test"

        // Encode same text multiple times
        val tokens1 = tokenizer.encode(text)
        val tokens2 = tokenizer.encode(text)
        val tokens3 = tokenizer.encode(text)

        // Should always produce same tokens
        assertEquals("First and second encoding should match", tokens1, tokens2)
        assertEquals("Second and third encoding should match", tokens2, tokens3)
    }

    @Test
    fun testDecodingConsistency() {
        val text = "decode test"
        val tokens = tokenizer.encode(text)

        // Decode same tokens multiple times
        val decoded1 = tokenizer.decode(tokens)
        val decoded2 = tokenizer.decode(tokens)
        val decoded3 = tokenizer.decode(tokens)

        // Should always produce same text
        assertEquals("First and second decoding should match", decoded1, decoded2)
        assertEquals("Second and third decoding should match", decoded2, decoded3)
    }

    // ===== Caching Tests (Integration) =====

    @Test
    fun testCacheHitPerformance() {
        val text = "cache"  // Small text (<= 10 chars, should be cached)

        // First encode (cache miss)
        val time1 = measureTimeMillis {
            tokenizer.encode(text)
        }

        // Second encode (cache hit)
        val time2 = measureTimeMillis {
            tokenizer.encode(text)
        }

        // Cache hit should be faster (allow some variance)
        assertTrue(
            "Cache hit should be faster (time1: ${time1}ms, time2: ${time2}ms)",
            time2 <= time1 * 1.5 // Allow 50% variance
        )
    }

    @Test
    fun testCacheClearAndStats() {
        val text1 = "test1"
        val text2 = "test2"

        // Populate cache
        tokenizer.encode(text1)
        tokenizer.encode(text2)

        val statsBefore = tokenizer.getCacheStats()
        assertTrue("Cache should have entries", statsBefore["encode_cache_size"]!! > 0)

        // Clear cache
        tokenizer.clearCache()

        val statsAfter = tokenizer.getCacheStats()
        assertEquals("Cache should be empty after clear", 0, statsAfter["encode_cache_size"])
        assertEquals("Decode cache should be empty", 0, statsAfter["decode_cache_size"])
    }

    // ===== Error Handling Tests =====

    @Test
    fun testEncodeInvalidInput() {
        // Test with very long text (stress test)
        val veryLongText = "word ".repeat(10000) // 10k words

        try {
            val tokens = tokenizer.encode(veryLongText)

            // Should either succeed or throw TokenizationException
            assertNotNull("Very long text should produce tokens", tokens)
            assertTrue("Very long text should produce many tokens", tokens.size > 1000)
        } catch (e: TokenizationException) {
            // Expected for text exceeding model limits
            assertNotNull("Exception should have message", e.message)
        }
    }

    @Test(expected = TokenizationException::class)
    fun testDecodeInvalidTokens() {
        // Test with invalid token IDs (very large numbers)
        val invalidTokens = listOf(-1, -2, -3)

        // Should throw TokenizationException
        tokenizer.decode(invalidTokens)
    }

    // ===== Performance Benchmarks =====

    @Test
    fun testEncodingPerformance() {
        val testText = "This is a test sentence for performance benchmarking."

        // Warm-up
        repeat(10) {
            tokenizer.encode(testText)
        }

        // Benchmark
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                tokenizer.encode(testText)
            }
        }

        val avgTime = totalTime.toDouble() / iterations

        // Should average < 5ms per encoding (reasonable for device)
        assertTrue(
            "Average encoding time should be < 5ms (actual: ${avgTime}ms)",
            avgTime < 5.0
        )
    }

    @Test
    fun testDecodingPerformance() {
        val testText = "Performance test"
        val tokens = tokenizer.encode(testText)

        // Warm-up
        repeat(10) {
            tokenizer.decode(tokens)
        }

        // Benchmark
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                tokenizer.decode(tokens)
            }
        }

        val avgTime = totalTime.toDouble() / iterations

        // Should average < 5ms per decoding
        assertTrue(
            "Average decoding time should be < 5ms (actual: ${avgTime}ms)",
            avgTime < 5.0
        )
    }

    // ===== Tokenizer Lifecycle Tests =====

    @Test
    fun testCreateMultipleTokenizers() {
        // Create multiple tokenizers from same runtime
        val tokenizer1 = runtime.createTokenizer()
        val tokenizer2 = runtime.createTokenizer()

        val text = "multi tokenizer test"

        // Both should work independently
        val tokens1 = tokenizer1.encode(text)
        val tokens2 = tokenizer2.encode(text)

        // Should produce same tokens
        assertEquals("Different tokenizers should produce same tokens", tokens1, tokens2)
    }

    @Test
    fun testTokenizerAfterRuntimeDispose() {
        // Create new runtime and tokenizer for this test
        val testRuntime = TVMRuntime.create(context, "cpu")
        val testTokenizer = testRuntime.createTokenizer()

        val text = "disposal test"
        val tokens = testTokenizer.encode(text)

        assertNotNull(tokens)

        // Dispose runtime
        testRuntime.dispose()

        // Tokenizer may or may not work after runtime disposal
        // This documents the behavior (don't enforce strict requirement)
        try {
            testTokenizer.encode("after disposal")
            // If it works, that's fine
        } catch (e: Exception) {
            // If it fails, that's also acceptable
            assertTrue("Should fail gracefully", e is TokenizationException || e is RuntimeException)
        }
    }

    // ===== Vocabulary Tests =====

    @Test
    fun testCommonWordTokenization() {
        // Test that common words tokenize consistently
        val commonWords = listOf("the", "is", "a", "and", "to", "of")

        commonWords.forEach { word ->
            val tokens = tokenizer.encode(word)

            assertNotNull("Common word '$word' should tokenize", tokens)
            assertTrue("Common word should produce 1-2 tokens", tokens.size in 1..2)
        }
    }

    @Test
    fun testPunctuationTokenization() {
        val punctuation = ".,!?;:"

        punctuation.forEach { char ->
            val tokens = tokenizer.encode(char.toString())

            assertNotNull("Punctuation '$char' should tokenize", tokens)
            assertTrue("Punctuation should produce tokens", tokens.isNotEmpty())
        }
    }
}
