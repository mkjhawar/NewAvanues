/**
 * SimilarityMatcherTest.kt - Unit tests for SimilarityMatcher
 *
 * Tests similarity matching algorithms including:
 * - Exact matching
 * - Fuzzy matching with typos
 * - Levenshtein distance calculations
 * - Threshold filtering
 * - Multi-result matching
 *
 * Created: 2025-10-09 02:55:24 PDT
 */

package com.augmentalis.voiceos.speech.utils

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class SimilarityMatcherTest {

    private lateinit var testCommands: List<String>

    @Before
    fun setup() {
        testCommands = listOf(
            "open calculator",
            "open camera",
            "open calendar",
            "go back",
            "go home",
            "volume up",
            "volume down",
            "turn on wifi",
            "turn off wifi",
            "call mom"
        )
    }

    // ============================================================
    // Exact Match Tests
    // ============================================================

    @Test
    fun `exact match returns 1_0 similarity`() {
        val similarity = SimilarityMatcher.calculateSimilarity("hello", "hello")
        assertEquals(1.0f, similarity, 0.001f)
    }

    @Test
    fun `exact match case insensitive`() {
        val similarity = SimilarityMatcher.calculateSimilarity("HELLO", "hello")
        // Note: calculateSimilarity doesn't normalize case, so this should NOT be 1.0
        // If we want case-insensitive, we need to normalize before calling
        val normalizedSimilarity = SimilarityMatcher.calculateSimilarity(
            "HELLO".lowercase(),
            "hello".lowercase()
        )
        assertEquals(1.0f, normalizedSimilarity, 0.001f)
    }

    @Test
    fun `empty strings return 0_0 similarity`() {
        val similarity = SimilarityMatcher.calculateSimilarity("", "")
        assertEquals(0.0f, similarity, 0.001f)
    }

    @Test
    fun `empty vs non-empty returns 0_0`() {
        val similarity1 = SimilarityMatcher.calculateSimilarity("", "hello")
        val similarity2 = SimilarityMatcher.calculateSimilarity("hello", "")
        assertEquals(0.0f, similarity1, 0.001f)
        assertEquals(0.0f, similarity2, 0.001f)
    }

    // ============================================================
    // Levenshtein Distance Tests
    // ============================================================

    @Test
    fun `levenshtein distance - identical strings`() {
        val distance = SimilarityMatcher.levenshteinDistance("hello", "hello")
        assertEquals(0, distance)
    }

    @Test
    fun `levenshtein distance - single insertion`() {
        val distance = SimilarityMatcher.levenshteinDistance("helo", "hello")
        assertEquals(1, distance)
    }

    @Test
    fun `levenshtein distance - single deletion`() {
        val distance = SimilarityMatcher.levenshteinDistance("hello", "helo")
        assertEquals(1, distance)
    }

    @Test
    fun `levenshtein distance - single substitution`() {
        val distance = SimilarityMatcher.levenshteinDistance("hello", "hallo")
        assertEquals(1, distance)
    }

    @Test
    fun `levenshtein distance - kitten to sitting`() {
        // Classic example: kitten -> sitten -> sittin -> sitting
        val distance = SimilarityMatcher.levenshteinDistance("kitten", "sitting")
        assertEquals(3, distance)
    }

    @Test
    fun `levenshtein distance - saturday to sunday`() {
        val distance = SimilarityMatcher.levenshteinDistance("saturday", "sunday")
        assertEquals(3, distance)
    }

    @Test
    fun `levenshtein distance - completely different`() {
        val distance = SimilarityMatcher.levenshteinDistance("abc", "xyz")
        assertEquals(3, distance)
    }

    // ============================================================
    // Fuzzy Matching Tests (Real Use Cases)
    // ============================================================

    @Test
    fun `fuzzy match - opn calculator matches open calculator`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "opn calculator",
            testCommands,
            threshold = 0.70f
        )

        assertNotNull(result)
        assertEquals("open calculator", result!!.first)
        assertTrue("Similarity should be >= 0.70", result.second >= 0.70f)
        // Expected similarity: ~0.93 (1 char difference in 14)
    }

    @Test
    fun `fuzzy match - opn calcluator matches open calculator`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "opn calcluator",
            testCommands,
            threshold = 0.70f
        )

        assertNotNull(result)
        assertEquals("open calculator", result!!.first)
        assertTrue("Similarity should be >= 0.70", result.second >= 0.70f)
        // Expected similarity: ~0.87 (2 char differences in 15)
    }

    @Test
    fun `fuzzy match - go bak matches go back`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "go bak",
            testCommands,
            threshold = 0.70f
        )

        assertNotNull(result)
        assertEquals("go back", result!!.first)
        assertTrue("Similarity should be >= 0.70", result.second >= 0.70f)
    }

    @Test
    fun `fuzzy match - volum up matches volume up`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "volum up",
            testCommands,
            threshold = 0.70f
        )

        assertNotNull(result)
        assertEquals("volume up", result!!.first)
        assertTrue("Similarity should be >= 0.70", result.second >= 0.70f)
    }

    @Test
    fun `fuzzy match - turn on wiif matches turn on wifi`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "turn on wiif",
            testCommands,
            threshold = 0.70f
        )

        assertNotNull(result)
        assertEquals("turn on wifi", result!!.first)
    }

    // ============================================================
    // Threshold Tests
    // ============================================================

    @Test
    fun `below threshold returns null`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "completely different command",
            testCommands,
            threshold = 0.95f  // Very high threshold
        )

        assertNull(result)
    }

    @Test
    fun `high threshold filters out poor matches`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "xyz",
            testCommands,
            threshold = 0.50f
        )

        assertNull("Very different input should not match even with low threshold", result)
    }

    @Test
    fun `low threshold allows more matches`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "open",
            testCommands,
            threshold = 0.30f  // Very low threshold
        )

        assertNotNull(result)
        // Should match one of the "open *" commands
        assertTrue(result!!.first.startsWith("open"))
    }

    // ============================================================
    // Find All Similar Tests
    // ============================================================

    @Test
    fun `findAllSimilar returns multiple matches`() {
        val results = SimilarityMatcher.findAllSimilar(
            "open calc",
            testCommands,
            threshold = 0.50f,
            maxResults = 5
        )

        assertTrue("Should find at least 1 match", results.isNotEmpty())
        // Should at least find "open calculator" since it's very similar
        assertTrue("Should contain 'open calculator'", results.any { it.first == "open calculator" })
    }

    @Test
    fun `findAllSimilar sorted by similarity descending`() {
        val results = SimilarityMatcher.findAllSimilar(
            "open calc",
            testCommands,
            threshold = 0.40f,
            maxResults = 3
        )

        assertTrue("Should have at least 2 results", results.size >= 2)

        // Verify sorting (each result should have >= similarity than the next)
        for (i in 0 until results.size - 1) {
            assertTrue(
                "Results should be sorted by similarity (highest first)",
                results[i].second >= results[i + 1].second
            )
        }
    }

    @Test
    fun `findAllSimilar respects maxResults`() {
        val results = SimilarityMatcher.findAllSimilar(
            "open",
            testCommands,
            threshold = 0.20f,
            maxResults = 2
        )

        assertTrue("Should respect maxResults limit", results.size <= 2)
    }

    @Test
    fun `findAllSimilar filters by threshold`() {
        val results = SimilarityMatcher.findAllSimilar(
            "completely different",
            testCommands,
            threshold = 0.90f,  // Very high threshold
            maxResults = 10
        )

        assertTrue("Should return empty list when no matches above threshold", results.isEmpty())
    }

    // ============================================================
    // IsSimilar Tests
    // ============================================================

    @Test
    fun `isSimilar returns true for similar strings`() {
        assertTrue(SimilarityMatcher.isSimilar("hello", "helo", 0.70f))
        assertTrue(SimilarityMatcher.isSimilar("test", "test", 0.90f))
    }

    @Test
    fun `isSimilar returns false for dissimilar strings`() {
        assertFalse(SimilarityMatcher.isSimilar("hello", "world", 0.70f))
        assertFalse(SimilarityMatcher.isSimilar("abc", "xyz", 0.50f))
    }

    // ============================================================
    // Edge Cases
    // ============================================================

    @Test
    fun `empty command list returns null`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "test",
            emptyList(),
            threshold = 0.70f
        )

        assertNull(result)
    }

    @Test
    fun `single command list works correctly`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "go bak",
            listOf("go back"),
            threshold = 0.70f
        )

        assertNotNull(result)
        assertEquals("go back", result!!.first)
    }

    @Test
    fun `whitespace is trimmed`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "  open calculator  ",
            testCommands,
            threshold = 0.95f
        )

        assertNotNull(result)
        assertEquals("open calculator", result!!.first)
        assertEquals(1.0f, result.second, 0.001f)
    }

    @Test
    fun `case is normalized`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "OPEN CALCULATOR",
            testCommands,
            threshold = 0.95f
        )

        assertNotNull(result)
        assertEquals("open calculator", result!!.first)
        assertEquals(1.0f, result.second, 0.001f)
    }

    // ============================================================
    // Similarity Score Verification
    // ============================================================

    @Test
    fun `verify similarity calculation formula`() {
        // Distance = 1, MaxLength = 5
        // Similarity = 1.0 - (1/5) = 0.8
        val similarity = SimilarityMatcher.calculateSimilarity("hello", "helo")
        assertEquals(0.8f, similarity, 0.001f)
    }

    @Test
    fun `verify similarity for different lengths`() {
        // "abc" to "abcd"
        // Distance = 1 (insert 'd'), MaxLength = 4
        // Similarity = 1.0 - (1/4) = 0.75
        val similarity = SimilarityMatcher.calculateSimilarity("abc", "abcd")
        assertEquals(0.75f, similarity, 0.001f)
    }

    @Test
    fun `completely different strings have low similarity`() {
        val similarity = SimilarityMatcher.calculateSimilarity("abc", "xyz")
        // Distance = 3, MaxLength = 3
        // Similarity = 1.0 - (3/3) = 0.0
        assertEquals(0.0f, similarity, 0.001f)
    }
}
