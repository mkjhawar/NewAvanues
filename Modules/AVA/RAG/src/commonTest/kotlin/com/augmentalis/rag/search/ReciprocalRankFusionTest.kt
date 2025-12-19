// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/search/ReciprocalRankFusionTest.kt
// created: 2025-11-27
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ReciprocalRankFusion
 *
 * Phase 2.4: Task P1-RAG-003
 */
class ReciprocalRankFusionTest {

    private val rrf = ReciprocalRankFusion(k = 60)

    @Test
    fun testBasicFusion() {
        val list1 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 0.9f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.8f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.7f)
        )

        val list2 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc2", 0.95f),
            ReciprocalRankFusion.ScoredDocument("doc1", 0.85f),
            ReciprocalRankFusion.ScoredDocument("doc4", 0.75f)
        )

        val fused = rrf.fuse(list1, list2)

        // Doc2 appears first in both lists, should rank highest
        assertEquals("doc2", fused[0].documentId, "Doc appearing in top of both lists should rank first")
    }

    @Test
    fun testMultiListBoost() {
        val list1 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 0.9f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.5f)
        )

        val list2 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc2", 0.6f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.8f)
        )

        val fused = rrf.fuse(list1, list2)

        // Doc2 appears in both lists, should get boost even though individually lower ranked
        assertTrue(fused.any { it.documentId == "doc2" }, "Doc in multiple lists should appear in results")
    }

    @Test
    fun testSingleList() {
        val list = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 0.9f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.8f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.7f)
        )

        val fused = rrf.fuse(list)

        // Single list should maintain order
        assertEquals(3, fused.size)
        assertEquals("doc1", fused[0].documentId)
    }

    @Test
    fun testEmptyLists() {
        val fused = rrf.fuse()
        assertEquals(0, fused.size, "Empty input should return empty result")
    }

    @Test
    fun testRRFScoreCalculation() {
        // With k=60, rank 1 should give score 1/61, rank 2 gives 1/62, etc.
        val list = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f)
        )

        val fused = rrf.fuse(list)
        val expectedScore = 1.0f / 61.0f

        assertEquals(expectedScore, fused[0].score, 0.001f, "RRF score should be 1/(k+rank)")
    }

    @Test
    fun testWeightedFusion() {
        val list1 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 0.9f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.8f)
        )

        val list2 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc2", 0.95f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.85f)
        )

        // Give list1 higher weight
        val fused = rrf.fuseWeighted(list1 to 0.7f, list2 to 0.3f)

        // Results should be influenced by weights
        assertTrue(fused.isNotEmpty())
        assertTrue(fused.any { it.documentId == "doc1" || it.documentId == "doc2" })
    }

    @Test
    fun testWeightNormalization() {
        val list1 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f)
        )

        val list2 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc2", 1.0f)
        )

        // Weights don't sum to 1.0, should be normalized
        val fused = rrf.fuseWeighted(list1 to 2.0f, list2 to 3.0f)

        // Both documents should appear
        assertEquals(2, fused.size)
    }

    @Test
    fun testRankPositionMatters() {
        val list = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.9f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.8f),
            ReciprocalRankFusion.ScoredDocument("doc4", 0.7f)
        )

        val fused = rrf.fuse(list)

        // Higher ranked documents should have higher RRF scores
        assertTrue(fused[0].score > fused[1].score)
        assertTrue(fused[1].score > fused[2].score)
        assertTrue(fused[2].score > fused[3].score)
    }

    @Test
    fun testDisagreementBetweenRankers() {
        // List1 prefers doc1, List2 prefers doc2
        val list1 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.1f)
        )

        val list2 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc2", 1.0f),
            ReciprocalRankFusion.ScoredDocument("doc1", 0.1f)
        )

        val fused = rrf.fuse(list1, list2)

        // Both should appear but exact order depends on RRF calculation
        assertEquals(2, fused.size)
        val docIds = fused.map { it.documentId }.toSet()
        assertTrue(docIds.contains("doc1"))
        assertTrue(docIds.contains("doc2"))
    }

    @Test
    fun testManyLists() {
        val list1 = listOf(ReciprocalRankFusion.ScoredDocument("doc1", 1.0f))
        val list2 = listOf(ReciprocalRankFusion.ScoredDocument("doc2", 1.0f))
        val list3 = listOf(ReciprocalRankFusion.ScoredDocument("doc3", 1.0f))
        val list4 = listOf(ReciprocalRankFusion.ScoredDocument("doc1", 1.0f)) // doc1 appears twice

        val fused = rrf.fuse(list1, list2, list3, list4)

        // Doc1 appears in 2 lists, should rank higher
        assertEquals("doc1", fused[0].documentId, "Document in multiple lists should rank highest")
    }

    @Test
    fun testFusionStats() {
        val list1 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.9f)
        )

        val list2 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc2", 1.0f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.9f)
        )

        val stats = rrf.analyzeFusion(list1, list2, topK = 2)

        // Doc2 appears in both lists
        assertEquals(1, stats.documentsInMultipleLists)
        assertEquals(3, stats.totalDocuments)
    }

    @Test
    fun testIdenticalRankings() {
        val list1 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.9f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.8f)
        )

        val list2 = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f),
            ReciprocalRankFusion.ScoredDocument("doc2", 0.9f),
            ReciprocalRankFusion.ScoredDocument("doc3", 0.8f)
        )

        val fused = rrf.fuse(list1, list2)

        // Order should be preserved
        assertEquals("doc1", fused[0].documentId)
        assertEquals("doc2", fused[1].documentId)
        assertEquals("doc3", fused[2].documentId)
    }

    @Test
    fun testDifferentKValues() {
        val rrf60 = ReciprocalRankFusion(k = 60)
        val rrf100 = ReciprocalRankFusion(k = 100)

        val list = listOf(
            ReciprocalRankFusion.ScoredDocument("doc1", 1.0f)
        )

        val fused60 = rrf60.fuse(list)
        val fused100 = rrf100.fuse(list)

        // Higher k should give lower score
        assertTrue(fused60[0].score > fused100[0].score, "Lower k should give higher RRF score")
    }
}
