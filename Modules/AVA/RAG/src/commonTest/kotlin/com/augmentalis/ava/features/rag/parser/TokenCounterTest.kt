// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/parser/TokenCounterTest.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenCounterTest {

    @Test
    fun testCountTokensBasic() {
        val text = "Hello world, this is a test."
        val tokenCount = TokenCounter.countTokens(text)

        // "Hello", "world,", "this", "is", "a", "test." = 6 words
        // 6 * 1.3 = 7.8 ≈ 7 tokens
        assertEquals(7, tokenCount, "Basic token counting should work")
    }

    @Test
    fun testCountTokensEmpty() {
        val text = ""
        val tokenCount = TokenCounter.countTokens(text)
        assertEquals(0, tokenCount, "Empty string should have 0 tokens")
    }

    @Test
    fun testCountTokensWhitespace() {
        val text = "   \n\t   "
        val tokenCount = TokenCounter.countTokens(text)
        assertEquals(0, tokenCount, "Whitespace-only string should have 0 tokens")
    }

    @Test
    fun testCountTokensSingleWord() {
        val text = "Hello"
        val tokenCount = TokenCounter.countTokens(text)
        // 1 word * 1.3 = 1.3 ≈ 1 token
        assertEquals(1, tokenCount, "Single word should count correctly")
    }

    @Test
    fun testCountTokensMultipleSpaces() {
        val text = "Hello    world     test"
        val tokenCount = TokenCounter.countTokens(text)
        // 3 words * 1.3 = 3.9 ≈ 3 tokens
        assertEquals(3, tokenCount, "Multiple spaces should be handled")
    }

    @Test
    fun testCountTokensSubstring() {
        val text = "The quick brown fox jumps over the lazy dog"
        val tokenCount = TokenCounter.countTokens(text, 4, 19)
        // Substring: "quick brown fox" = 3 words * 1.3 = 3.9 ≈ 3 tokens
        assertEquals(3, tokenCount, "Substring token counting should work")
    }

    @Test
    fun testCountTokensSubstringOutOfBounds() {
        val text = "Hello world"
        val tokenCount = TokenCounter.countTokens(text, 50, 100)
        assertEquals(0, tokenCount, "Out of bounds substring should return 0")
    }

    @Test
    fun testFindOffsetForTokenCount() {
        val text = "The quick brown fox jumps over the lazy dog"

        // Find offset for ~3 tokens (should be around "The quick brown")
        val offset = TokenCounter.findOffsetForTokenCount(text, 0, 3)

        // Should be somewhere reasonable (not at start, not past end)
        assertTrue(offset >= 0, "Offset should be non-negative")
        assertTrue(offset <= text.length, "Offset should be within text")

        // Should have moved forward from start
        assertTrue(offset > 0, "Offset should be after start for non-zero tokens")
    }

    @Test
    fun testFindOffsetForTokenCountZeroTokens() {
        val text = "Hello world"
        val offset = TokenCounter.findOffsetForTokenCount(text, 0, 0)
        assertEquals(0, offset, "Zero tokens should return start offset")
    }

    @Test
    fun testFindOffsetForTokenCountPastEnd() {
        val text = "Hello world"
        val offset = TokenCounter.findOffsetForTokenCount(text, 0, 1000)
        assertEquals(text.length, offset, "Large token count should return text length")
    }

    @Test
    fun testGetTokenBoundaries() {
        val text = "The quick brown fox jumps over the lazy dog"
        val boundaries = TokenCounter.getTokenBoundaries(text, 5)

        // Should have at least 2 boundaries (start and end)
        assertTrue(boundaries.size >= 2, "Should have at least start and end boundaries")

        // First boundary should be 0
        assertEquals(0, boundaries.first(), "First boundary should be 0")

        // Last boundary should be text length
        assertEquals(text.length, boundaries.last(), "Last boundary should be text length")

        // Boundaries should be in ascending order
        for (i in 0 until boundaries.size - 1) {
            assertTrue(boundaries[i] < boundaries[i + 1], "Boundaries should be ascending")
        }
    }

    @Test
    fun testGetTokenBoundariesShortText() {
        val text = "Hello"
        val boundaries = TokenCounter.getTokenBoundaries(text, 10)

        // Short text should have just start and end
        assertEquals(2, boundaries.size, "Short text should have 2 boundaries")
        assertEquals(0, boundaries[0])
        assertEquals(text.length, boundaries[1])
    }

    @Test
    fun testGetTokenBoundariesEmpty() {
        val text = ""
        val boundaries = TokenCounter.getTokenBoundaries(text, 10)

        // Empty text should have at least start boundary
        assertTrue(boundaries.isNotEmpty(), "Should have at least one boundary")
        assertEquals(0, boundaries[0], "First boundary should be 0")

        // Last boundary should be 0 (text length)
        assertEquals(0, boundaries.last(), "Last boundary should be 0 for empty text")
    }

    @Test
    fun testTokenCountConsistency() {
        // Test that token counting is consistent with offset finding
        val text = "The quick brown fox jumps over the lazy dog"
        val targetTokens = 5

        val offset = TokenCounter.findOffsetForTokenCount(text, 0, targetTokens)
        val actualTokens = TokenCounter.countTokens(text, 0, offset)

        // Actual tokens should be close to target (within 2 due to rounding)
        val difference = kotlin.math.abs(actualTokens - targetTokens)
        assertTrue(difference <= 2, "Token count should be consistent with offset (diff: $difference)")
    }

    @Test
    fun testLongText() {
        // Test with a longer piece of text
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris."

        val tokenCount = TokenCounter.countTokens(text)

        // Rough estimate: ~30 words * 1.3 = ~39 tokens
        assertTrue(tokenCount > 30, "Long text should have reasonable token count")
        assertTrue(tokenCount < 50, "Token count should not be excessive")
    }
}
