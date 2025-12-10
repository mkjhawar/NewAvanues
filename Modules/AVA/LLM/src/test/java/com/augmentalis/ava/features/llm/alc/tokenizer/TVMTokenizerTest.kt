package com.augmentalis.ava.features.llm.alc.tokenizer

import com.augmentalis.ava.features.llm.alc.TVMRuntime
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for TVMTokenizer (MLC-LLM native tokenizer)
 *
 * Tests the correct tokenizer implementation that wraps TVMRuntime.
 * This tests the wrapper layer, not the underlying MLC-LLM native tokenizer.
 *
 * Architecture:
 * - TVMRuntime.tokenize() → MLC-LLM native (HuggingFace via Rust FFI)
 * - TVMTokenizer wraps TVMRuntime with caching
 *
 * Created: 2025-11-14
 * Replaces: Old DJL SentencePiece tests (deleted)
 */
class TVMTokenizerTest {

    private lateinit var mockRuntime: TVMRuntime
    private lateinit var tokenizer: TVMTokenizer

    @Before
    fun setup() {
        mockRuntime = mockk(relaxed = true)
        tokenizer = TVMTokenizer(mockRuntime)
    }

    // ===== Basic Encoding Tests =====

    @Test
    fun `encode should call runtime tokenize`() {
        val text = "Hello world"
        val expectedTokens = listOf(1, 2, 3, 4, 5)

        every { mockRuntime.tokenize(text) } returns expectedTokens

        val result = tokenizer.encode(text)

        assertEquals(expectedTokens, result)
        verify(exactly = 1) { mockRuntime.tokenize(text) }
    }

    @Test
    fun `encode should return tokens for simple text`() {
        val text = "Test"
        val expectedTokens = listOf(100, 200)

        every { mockRuntime.tokenize(text) } returns expectedTokens

        val result = tokenizer.encode(text)

        assertNotNull("Tokens should not be null", result)
        assertEquals(expectedTokens, result)
    }

    @Test
    fun `encode should handle empty string`() {
        val text = ""
        val expectedTokens = emptyList<Int>()

        every { mockRuntime.tokenize(text) } returns expectedTokens

        val result = tokenizer.encode(text)

        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun `encode should handle whitespace`() {
        val text = "   \n  \t  "
        val expectedTokens = listOf(32) // Space token

        every { mockRuntime.tokenize(text) } returns expectedTokens

        val result = tokenizer.encode(text)

        assertNotNull(result)
        assertEquals(expectedTokens, result)
    }

    @Test
    fun `encode should handle long text`() {
        val longText = "This is a longer piece of text that should be tokenized into multiple tokens"
        val expectedTokens = List(20) { it + 1 } // 20 tokens

        every { mockRuntime.tokenize(longText) } returns expectedTokens

        val result = tokenizer.encode(longText)

        assertEquals(20, result.size)
        assertEquals(expectedTokens, result)
    }

    @Test
    fun `encode should handle multilingual text`() {
        val multilingualText = "Hello 世界 مرحبا мир"
        val expectedTokens = listOf(100, 200, 300, 400)

        every { mockRuntime.tokenize(multilingualText) } returns expectedTokens

        val result = tokenizer.encode(multilingualText)

        assertNotNull(result)
        assertEquals(expectedTokens, result)
    }

    @Test
    fun `encode should handle special characters`() {
        val specialText = "@#$% special chars!"
        val expectedTokens = listOf(50, 60, 70, 80)

        every { mockRuntime.tokenize(specialText) } returns expectedTokens

        val result = tokenizer.encode(specialText)

        assertEquals(expectedTokens, result)
    }

    @Test
    fun `encode should handle numbers`() {
        val numberText = "2025 10:30"
        val expectedTokens = listOf(10, 20, 30)

        every { mockRuntime.tokenize(numberText) } returns expectedTokens

        val result = tokenizer.encode(numberText)

        assertEquals(expectedTokens, result)
    }

    // ===== Basic Decoding Tests =====

    @Test
    fun `decode should call runtime detokenize`() {
        val tokens = listOf(1, 2, 3, 4, 5)
        val expectedText = "Hello world"

        every { mockRuntime.detokenize(tokens) } returns expectedText

        val result = tokenizer.decode(tokens)

        assertEquals(expectedText, result)
        verify(exactly = 1) { mockRuntime.detokenize(tokens) }
    }

    @Test
    fun `decode should return text for simple tokens`() {
        val tokens = listOf(100, 200)
        val expectedText = "Test"

        every { mockRuntime.detokenize(tokens) } returns expectedText

        val result = tokenizer.decode(tokens)

        assertNotNull("Text should not be null", result)
        assertEquals(expectedText, result)
    }

    @Test
    fun `decode should handle empty list`() {
        val tokens = emptyList<Int>()
        val expectedText = ""

        every { mockRuntime.detokenize(tokens) } returns expectedText

        val result = tokenizer.decode(tokens)

        assertEquals("", result)
    }

    @Test
    fun `decode should handle single token`() {
        val tokens = listOf(42)
        val expectedText = "hello"

        every { mockRuntime.detokenize(tokens) } returns expectedText

        val result = tokenizer.decode(tokens)

        assertEquals(expectedText, result)
    }

    @Test
    fun `decode should handle long token sequence`() {
        val tokens = List(50) { it + 1 }
        val expectedText = "This is a long decoded text"

        every { mockRuntime.detokenize(tokens) } returns expectedText

        val result = tokenizer.decode(tokens)

        assertEquals(expectedText, result)
    }

    // ===== Caching Tests =====

    @Test
    fun `encode should cache small texts`() {
        val text = "cache"  // <= 10 chars
        val tokens = listOf(1, 2, 3)

        every { mockRuntime.tokenize(text) } returns tokens

        // First call - should hit runtime
        val result1 = tokenizer.encode(text)
        assertEquals(tokens, result1)

        // Second call - should use cache
        val result2 = tokenizer.encode(text)
        assertEquals(tokens, result2)

        // Verify runtime called only once (cached second time)
        verify(exactly = 1) { mockRuntime.tokenize(text) }
    }

    @Test
    fun `encode should not cache long texts`() {
        val longText = "This is a text longer than 10 characters"  // > 10 chars
        val tokens = listOf(1, 2, 3, 4, 5)

        every { mockRuntime.tokenize(longText) } returns tokens

        // First call
        tokenizer.encode(longText)
        // Second call
        tokenizer.encode(longText)

        // Should call runtime twice (no caching for long text)
        verify(exactly = 2) { mockRuntime.tokenize(longText) }
    }

    @Test
    fun `decode should cache small token sequences`() {
        val tokens = listOf(1, 2, 3)  // <= 5 tokens
        val text = "cached"

        every { mockRuntime.detokenize(tokens) } returns text

        // First call - should hit runtime
        val result1 = tokenizer.decode(tokens)
        assertEquals(text, result1)

        // Second call - should use cache
        val result2 = tokenizer.decode(tokens)
        assertEquals(text, result2)

        // Verify runtime called only once
        verify(exactly = 1) { mockRuntime.detokenize(tokens) }
    }

    @Test
    fun `decode should not cache long token sequences`() {
        val longTokens = List(20) { it + 1 }  // > 5 tokens
        val text = "not cached"

        every { mockRuntime.detokenize(longTokens) } returns text

        // First call
        tokenizer.decode(longTokens)
        // Second call
        tokenizer.decode(longTokens)

        // Should call runtime twice (no caching for long sequences)
        verify(exactly = 2) { mockRuntime.detokenize(longTokens) }
    }

    @Test
    fun `clearCache should clear both encode and decode caches`() {
        val text = "test"
        val tokens = listOf(1, 2, 3)

        every { mockRuntime.tokenize(text) } returns tokens
        every { mockRuntime.detokenize(tokens) } returns text

        // Populate caches
        tokenizer.encode(text)
        tokenizer.decode(tokens)

        // Clear caches
        tokenizer.clearCache()

        // Next calls should hit runtime again
        tokenizer.encode(text)
        tokenizer.decode(tokens)

        verify(exactly = 2) { mockRuntime.tokenize(text) }
        verify(exactly = 2) { mockRuntime.detokenize(tokens) }
    }

    @Test
    fun `getCacheStats should return cache sizes`() {
        val text1 = "test1"
        val text2 = "test2"
        val tokens1 = listOf(1, 2)
        val tokens2 = listOf(3, 4)

        every { mockRuntime.tokenize(any()) } returns tokens1
        every { mockRuntime.detokenize(any()) } returns "text"

        // Populate encode cache
        tokenizer.encode(text1)
        tokenizer.encode(text2)

        // Populate decode cache
        tokenizer.decode(tokens1)
        tokenizer.decode(tokens2)

        val stats = tokenizer.getCacheStats()

        assertEquals(2, stats["encode_cache_size"])
        assertEquals(2, stats["decode_cache_size"])
    }

    @Test
    fun `getCacheStats should return zero for empty caches`() {
        val stats = tokenizer.getCacheStats()

        assertEquals(0, stats["encode_cache_size"])
        assertEquals(0, stats["decode_cache_size"])
    }

    // ===== Round-trip Tests =====

    @Test
    fun `encode then decode should return original text`() {
        val originalText = "Hello world"
        val tokens = listOf(1, 2, 3, 4, 5)

        every { mockRuntime.tokenize(originalText) } returns tokens
        every { mockRuntime.detokenize(tokens) } returns originalText

        val encoded = tokenizer.encode(originalText)
        val decoded = tokenizer.decode(encoded)

        assertEquals(originalText, decoded)
    }

    @Test
    fun `multiple encode decode cycles should be consistent`() {
        val text = "Test"
        val tokens = listOf(100, 200)

        every { mockRuntime.tokenize(text) } returns tokens
        every { mockRuntime.detokenize(tokens) } returns text

        // Cycle 1
        val encoded1 = tokenizer.encode(text)
        val decoded1 = tokenizer.decode(encoded1)

        // Cycle 2
        val encoded2 = tokenizer.encode(text)
        val decoded2 = tokenizer.decode(encoded2)

        assertEquals(encoded1, encoded2)
        assertEquals(decoded1, decoded2)
        assertEquals(text, decoded1)
        assertEquals(text, decoded2)
    }

    // ===== Error Handling Tests =====

    @Test(expected = TokenizationException::class)
    fun `encode should throw TokenizationException on runtime error`() {
        val text = "error test"

        every { mockRuntime.tokenize(text) } throws RuntimeException("Native error")

        tokenizer.encode(text)
    }

    @Test(expected = TokenizationException::class)
    fun `decode should throw TokenizationException on runtime error`() {
        val tokens = listOf(1, 2, 3)

        every { mockRuntime.detokenize(tokens) } throws RuntimeException("Native error")

        tokenizer.decode(tokens)
    }

    @Test
    fun `TokenizationException should preserve cause`() {
        val text = "error"
        val rootCause = RuntimeException("Root cause")

        every { mockRuntime.tokenize(text) } throws rootCause

        try {
            tokenizer.encode(text)
            fail("Should have thrown TokenizationException")
        } catch (e: TokenizationException) {
            assertNotNull("Should preserve cause", e.cause)
            assertEquals(rootCause, e.cause)
        }
    }

    // ===== Consistency Tests =====

    @Test
    fun `same text should always produce same tokens`() {
        val text = "consistent"
        val tokens = listOf(1, 2, 3)

        every { mockRuntime.tokenize(text) } returns tokens

        val result1 = tokenizer.encode(text)
        val result2 = tokenizer.encode(text)
        val result3 = tokenizer.encode(text)

        assertEquals(result1, result2)
        assertEquals(result2, result3)
    }

    @Test
    fun `different texts should produce different tokens`() {
        val text1 = "first"
        val text2 = "second"
        val tokens1 = listOf(1, 2)
        val tokens2 = listOf(3, 4)

        every { mockRuntime.tokenize(text1) } returns tokens1
        every { mockRuntime.tokenize(text2) } returns tokens2

        val result1 = tokenizer.encode(text1)
        val result2 = tokenizer.encode(text2)

        assertNotEquals(result1, result2)
    }

    // ===== Cache Limit Tests =====

    @Test
    fun `encode cache should respect MAX_CACHE_SIZE limit`() {
        every { mockRuntime.tokenize(any()) } returns listOf(1, 2, 3)

        // Add 1001 entries (exceeds MAX_CACHE_SIZE of 1000)
        for (i in 0..1000) {
            tokenizer.encode("t$i")  // All <= 10 chars
        }

        val stats = tokenizer.getCacheStats()

        // Should not exceed 1000
        assertTrue("Cache should not exceed 1000 entries", stats["encode_cache_size"]!! <= 1000)
    }

    @Test
    fun `decode cache should respect MAX_CACHE_SIZE limit`() {
        every { mockRuntime.detokenize(any()) } returns "text"

        // Add 1001 entries (exceeds MAX_CACHE_SIZE of 1000)
        for (i in 0..1000) {
            tokenizer.decode(listOf(i, i+1))  // All <= 5 tokens
        }

        val stats = tokenizer.getCacheStats()

        // Should not exceed 1000
        assertTrue("Cache should not exceed 1000 entries", stats["decode_cache_size"]!! <= 1000)
    }
}
