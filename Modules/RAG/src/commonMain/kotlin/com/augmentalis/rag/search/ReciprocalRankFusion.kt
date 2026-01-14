// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/search/ReciprocalRankFusion.kt
// created: 2025-11-27
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.search

/**
 * Reciprocal Rank Fusion - combines multiple ranking lists
 *
 * RRF is a method for combining multiple ranked result lists into a single
 * unified ranking. It's particularly effective for hybrid search where you
 * want to combine results from different retrieval methods (e.g., semantic
 * search and keyword search).
 *
 * Formula:
 * RRF(d) = Σ 1 / (k + rank_i(d))
 *
 * where:
 * - d is a document
 * - rank_i(d) is the rank of document d in the i-th ranking list
 * - k is a constant (typically 60) to prevent division by small numbers
 *
 * Properties:
 * - Documents appearing in multiple rankings get boosted
 * - Top-ranked documents in any list get significant weight
 * - Robust to differences in score scales between rankers
 * - No need to normalize scores from different systems
 *
 * Standard k = 60 (empirically determined in research)
 *
 * Phase 2.4: Hybrid Search for RAG
 * Task P1-RAG-003: Reciprocal Rank Fusion Implementation
 *
 * Reference:
 * Cormack, G. V., Clarke, C. L., & Buettcher, S. (2009).
 * Reciprocal rank fusion outperforms condorcet and individual rank learning methods.
 * In Proceedings of the 32nd international ACM SIGIR conference.
 */
class ReciprocalRankFusion(private val k: Int = 60) {

    /**
     * Document with its computed score
     */
    data class ScoredDocument(
        val documentId: String,
        val score: Float
    )

    /**
     * Fuse multiple ranked lists into a single unified ranking
     *
     * Each list should be ordered by relevance (most relevant first).
     * The algorithm will compute RRF scores and return a new ranking.
     *
     * @param rankedLists Multiple lists of (documentId, score) pairs
     * @return Unified ranking using RRF, sorted by RRF score descending
     */
    fun fuse(vararg rankedLists: List<ScoredDocument>): List<ScoredDocument> {
        if (rankedLists.isEmpty()) return emptyList()
        if (rankedLists.size == 1) return rankedLists[0]

        val rrfScores = mutableMapOf<String, Float>()

        // Process each ranked list
        for (rankedList in rankedLists) {
            rankedList.forEachIndexed { index, doc ->
                // Rank is 1-based (first document has rank 1)
                val rank = index + 1

                // RRF contribution: 1 / (k + rank)
                val rrfScore = 1.0f / (k + rank)

                // Accumulate scores for documents appearing in multiple lists
                rrfScores[doc.documentId] =
                    rrfScores.getOrDefault(doc.documentId, 0f) + rrfScore
            }
        }

        // Convert to scored documents and sort by RRF score descending
        return rrfScores.entries
            .map { (docId, score) -> ScoredDocument(docId, score) }
            .sortedByDescending { it.score }
    }

    /**
     * Fuse ranked lists with weights
     *
     * Allows giving different importance to different ranking methods.
     * For example, you might want semantic search to have 70% weight
     * and keyword search to have 30% weight.
     *
     * @param rankedListsWithWeights Pairs of (rankedList, weight)
     * @return Unified ranking using weighted RRF
     */
    fun fuseWeighted(vararg rankedListsWithWeights: Pair<List<ScoredDocument>, Float>): List<ScoredDocument> {
        if (rankedListsWithWeights.isEmpty()) return emptyList()
        if (rankedListsWithWeights.size == 1) return rankedListsWithWeights[0].first

        val rrfScores = mutableMapOf<String, Float>()

        // Normalize weights to sum to 1.0
        val totalWeight = rankedListsWithWeights.sumOf { it.second.toDouble() }.toFloat()

        // Process each ranked list with its weight
        for ((rankedList, weight) in rankedListsWithWeights) {
            val normalizedWeight = if (totalWeight > 0f) weight / totalWeight else 1f / rankedListsWithWeights.size

            rankedList.forEachIndexed { index, doc ->
                val rank = index + 1
                val rrfScore = normalizedWeight * (1.0f / (k + rank))
                rrfScores[doc.documentId] =
                    rrfScores.getOrDefault(doc.documentId, 0f) + rrfScore
            }
        }

        return rrfScores.entries
            .map { (docId, score) -> ScoredDocument(docId, score) }
            .sortedByDescending { it.score }
    }

    /**
     * Analyze fusion statistics
     *
     * Useful for understanding how the fusion is working
     *
     * @param rankedLists Multiple ranked lists
     * @param topK Number of top results to analyze
     * @return Fusion statistics
     */
    fun analyzeFusion(vararg rankedLists: List<ScoredDocument>, topK: Int = 10): FusionStats {
        val fused = fuse(*rankedLists)
        val topResults = fused.take(topK)

        // Count how many top results came from each list
        val sourceDistribution = mutableMapOf<Int, Int>()
        for (doc in topResults) {
            for ((listIndex, list) in rankedLists.withIndex()) {
                if (list.any { it.documentId == doc.documentId }) {
                    sourceDistribution[listIndex] = sourceDistribution.getOrDefault(listIndex, 0) + 1
                }
            }
        }

        // Count documents appearing in multiple lists
        val docAppearances = mutableMapOf<String, Int>()
        for (list in rankedLists) {
            for (doc in list) {
                docAppearances[doc.documentId] = docAppearances.getOrDefault(doc.documentId, 0) + 1
            }
        }
        val multiListDocs = docAppearances.count { it.value > 1 }

        return FusionStats(
            totalDocuments = fused.size,
            documentsInMultipleLists = multiListDocs,
            sourceDistribution = sourceDistribution,
            averageRrfScore = topResults.map { it.score }.average().toFloat()
        )
    }

    /**
     * Statistics about fusion results
     */
    data class FusionStats(
        val totalDocuments: Int,
        val documentsInMultipleLists: Int,
        val sourceDistribution: Map<Int, Int>,
        val averageRrfScore: Float
    )
}
