// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/search/BM25ScorerTest.kt
// created: 2025-11-27
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for BM25Scorer
 *
 * Phase 2.4: Task P1-RAG-002
 */
class BM25ScorerTest {

    private val scorer = BM25Scorer()

    @Test
    fun testBasicScoring() {
        val corpus = listOf(
            "machine learning is a subset of artificial intelligence",
            "deep learning uses neural networks",
            "natural language processing is part of AI"
        )

        val query = "machine learning"
        val document = corpus[0]

        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val score = scorer.scoreWithCorpus(query, document, corpus)

        // Document containing both query terms should have positive score
        assertTrue(score > 0f, "Score should be positive for matching document, got $score")
    }

    @Test
    fun testExactMatchScoresHigher() {
        val corpus = listOf(
            "machine learning is important for AI",
            "artificial intelligence and robotics",
            "deep learning neural networks"
        )

        val query = "machine learning"
        val scores = scorer.scoreDocuments(
            query,
            corpus,
            scorer.buildTermDocFrequencies(corpus)
        )

        // First document has exact match, should score highest
        assertTrue(scores[0] > scores[1], "Exact match should score higher")
        assertTrue(scores[0] > scores[2], "Exact match should score higher")
    }

    @Test
    fun testIDFCalculation() {
        val corpus = listOf(
            "the quick brown fox",
            "the lazy dog",
            "unique term here"
        )

        val query = "unique"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)

        // "unique" appears in 1 doc, "the" appears in 2 docs
        assertEquals(1, termDocFreq["unique"])
        assertEquals(2, termDocFreq["the"])

        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        // Document with rare term "unique" should score highest
        assertTrue(scores[2] > scores[0], "Rare term should boost score")
        assertTrue(scores[2] > scores[1], "Rare term should boost score")
    }

    @Test
    fun testTermFrequencySaturation() {
        val corpus = listOf(
            "test test test test test",  // High TF
            "test document",              // Low TF
            "no match here"
        )

        val query = "test"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        // First document should score higher but not 5x higher due to saturation
        assertTrue(scores[0] > scores[1], "Higher TF should score higher")
        assertTrue(scores[0] < scores[1] * 5, "Saturation should prevent linear scaling")
    }

    @Test
    fun testLengthNormalization() {
        val corpus = listOf(
            "machine learning",
            "machine learning is a powerful technology used in many applications today"
        )

        val query = "machine learning"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        // Shorter document should score higher due to length normalization
        assertTrue(scores[0] > scores[1], "Shorter document should score higher with same terms")
    }

    @Test
    fun testNoMatch() {
        val corpus = listOf(
            "artificial intelligence",
            "deep learning",
            "neural networks"
        )

        val query = "quantum computing"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        // All scores should be zero for non-matching query
        assertTrue(scores.all { it == 0f }, "No match should result in zero scores")
    }

    @Test
    fun testPartialMatch() {
        val corpus = listOf(
            "machine learning and deep learning",
            "only deep learning here",
            "something else entirely"
        )

        val query = "machine learning"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        // First document matches both terms
        assertTrue(scores[0] > scores[1], "Full match should beat partial match")
        assertTrue(scores[1] > scores[2], "Partial match should beat no match")
    }

    @Test
    fun testCaseInsensitivity() {
        val corpus = listOf(
            "Machine Learning is great",
            "ARTIFICIAL INTELLIGENCE",
            "deep learning"
        )

        val query = "machine learning"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        // Case should not matter
        assertTrue(scores[0] > 0f, "Uppercase document should match lowercase query")
    }

    @Test
    fun testEmptyQuery() {
        val corpus = listOf("some document text")
        val query = ""
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        assertEquals(0f, scores[0], "Empty query should return zero score")
    }

    @Test
    fun testEmptyDocument() {
        val corpus = listOf("", "non-empty document")
        val query = "test"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        assertEquals(0f, scores[0], "Empty document should return zero score")
    }

    @Test
    fun testMultiTermQuery() {
        val corpus = listOf(
            "machine learning artificial intelligence neural networks",
            "machine learning only",
            "artificial intelligence only",
            "neural networks only"
        )

        val query = "machine learning artificial intelligence"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)
        val scores = scorer.scoreDocuments(query, corpus, termDocFreq)

        // First document contains all query terms
        assertTrue(scores[0] > scores[1], "All terms should score higher")
        assertTrue(scores[0] > scores[2], "All terms should score higher")
        assertTrue(scores[0] > scores[3], "All terms should score higher")
    }

    @Test
    fun testTermDocFrequencyBuilding() {
        val corpus = listOf(
            "the quick brown fox",
            "the lazy dog",
            "quick brown dog"
        )

        val termDocFreq = scorer.buildTermDocFrequencies(corpus)

        assertEquals(2, termDocFreq["the"])
        assertEquals(2, termDocFreq["quick"])
        assertEquals(2, termDocFreq["brown"])
        assertEquals(2, termDocFreq["dog"])
        assertEquals(1, termDocFreq["fox"])
        assertEquals(1, termDocFreq["lazy"])
    }

    @Test
    fun testStandardParameters() {
        // Default parameters should be k1=1.5, b=0.75
        val defaultScorer = BM25Scorer()
        val customScorer = BM25Scorer(k1 = 1.5f, b = 0.75f)

        val corpus = listOf("test document")
        val query = "test"
        val termDocFreq = scorer.buildTermDocFrequencies(corpus)

        val score1 = defaultScorer.scoreWithCorpus(query, corpus[0], corpus)
        val score2 = customScorer.scoreWithCorpus(query, corpus[0], corpus)

        assertEquals(score1, score2, "Default and explicit standard parameters should match")
    }
}
