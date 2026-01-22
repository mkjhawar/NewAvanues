// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/search/BM25Scorer.kt
// created: 2025-11-27
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.search

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * BM25 (Best Match 25) scoring algorithm for keyword search
 *
 * BM25 is a probabilistic ranking function used to estimate the relevance
 * of documents to a given search query. It's widely used in information
 * retrieval and is the foundation of many search engines.
 *
 * Standard parameters:
 * - k1 = 1.5 (term saturation parameter, controls term frequency scaling)
 * - b = 0.75 (length normalization parameter, 0 = no normalization, 1 = full normalization)
 *
 * Formula:
 * BM25(D,Q) = Σ IDF(qi) * (f(qi,D) * (k1 + 1)) / (f(qi,D) + k1 * (1 - b + b * |D| / avgdl))
 *
 * where:
 * - D is a document
 * - Q is a query containing keywords q1, ..., qn
 * - f(qi, D) is the frequency of term qi in document D
 * - |D| is the length of document D
 * - avgdl is the average document length in the collection
 * - IDF(qi) is the inverse document frequency of query term qi
 *
 * Phase 2.4: Hybrid Search for RAG
 * Task P1-RAG-002: BM25 Scorer Implementation
 */
class BM25Scorer(
    private val k1: Float = 1.5f,
    private val b: Float = 0.75f
) {
    /**
     * Tokenize text into words for BM25 scoring
     * Uses simple whitespace and punctuation splitting with lowercase normalization
     */
    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
    }

    /**
     * Compute BM25 score for a document given a query
     *
     * @param queryTerms Tokenized query terms
     * @param document Document text
     * @param docLength Document length in tokens
     * @param avgDocLength Average document length across corpus
     * @param totalDocs Total number of documents in corpus
     * @param termDocFrequencies Map of term -> number of docs containing term
     * @return BM25 score (higher = better match)
     */
    fun score(
        queryTerms: List<String>,
        document: String,
        docLength: Int,
        avgDocLength: Float,
        totalDocs: Int,
        termDocFrequencies: Map<String, Int>
    ): Float {
        if (queryTerms.isEmpty() || document.isBlank()) return 0f
        if (totalDocs == 0 || avgDocLength == 0f) return 0f

        val docTokens = tokenize(document)
        val termFrequencies = docTokens.groupingBy { it }.eachCount()

        var score = 0f

        for (term in queryTerms) {
            val tf = termFrequencies[term] ?: 0
            val df = termDocFrequencies[term] ?: 0

            // Skip terms not in document or not in corpus
            if (tf == 0 || df == 0) continue

            // IDF = log((N - df + 0.5) / (df + 0.5))
            // Robertson-Sparck Jones weight
            // When term appears in all docs, IDF can be negative or near-zero
            // We use max(epsilon, IDF) to ensure common terms still contribute
            // based on TF and length normalization (BM25+ variant)
            val numerator = (totalDocs - df + 0.5f)
            val denominator = (df + 0.5f)

            // Skip if denominator is invalid
            if (denominator <= 0f) continue

            // Use small epsilon (0.001) to ensure scoring even for universal terms
            val rawIdf = if (numerator <= 0f) 0f else ln(numerator / denominator).toFloat()
            val idf = maxOf(0.001f, rawIdf)

            // Normalized TF with saturation
            // The (k1 + 1) in numerator and k1 in denominator create saturation effect
            val normalizedTF = (tf * (k1 + 1)) /
                (tf + k1 * (1 - b + b * (docLength / avgDocLength)))

            score += idf * normalizedTF
        }

        return score
    }

    /**
     * Compute BM25 scores for multiple documents
     *
     * @param query Query string
     * @param documents List of document texts
     * @param termDocFrequencies Precomputed term document frequencies
     * @return List of BM25 scores, one per document
     */
    fun scoreDocuments(
        query: String,
        documents: List<String>,
        termDocFrequencies: Map<String, Int>
    ): List<Float> {
        if (documents.isEmpty()) return emptyList()

        val queryTerms = tokenize(query)
        val totalDocs = documents.size

        // Compute average document length
        var totalTokens = 0
        val docLengths = documents.map { doc ->
            val tokens = tokenize(doc)
            totalTokens += tokens.size
            tokens.size
        }
        val avgDocLength = totalTokens.toFloat() / totalDocs

        // Score each document
        return documents.mapIndexed { index, doc ->
            score(
                queryTerms = queryTerms,
                document = doc,
                docLength = docLengths[index],
                avgDocLength = avgDocLength,
                totalDocs = totalDocs,
                termDocFrequencies = termDocFrequencies
            )
        }
    }

    /**
     * Build term document frequency map from a corpus
     *
     * @param documents List of document texts
     * @return Map of term -> number of documents containing that term
     */
    fun buildTermDocFrequencies(documents: List<String>): Map<String, Int> {
        val termDocFreq = mutableMapOf<String, Int>()

        for (doc in documents) {
            val uniqueTerms = tokenize(doc).toSet()
            for (term in uniqueTerms) {
                termDocFreq[term] = termDocFreq.getOrDefault(term, 0) + 1
            }
        }

        return termDocFreq
    }

    /**
     * Compute BM25 score for a single document with automatic corpus stats
     * Convenience method when you have the full corpus available
     *
     * @param query Query string
     * @param document Target document text
     * @param corpus Full document corpus (including target document)
     * @return BM25 score
     */
    fun scoreWithCorpus(
        query: String,
        document: String,
        corpus: List<String>
    ): Float {
        val termDocFreq = buildTermDocFrequencies(corpus)
        val queryTerms = tokenize(query)
        val docTokens = tokenize(document)
        val avgDocLength = corpus.sumOf { tokenize(it).size }.toFloat() / corpus.size

        return score(
            queryTerms = queryTerms,
            document = document,
            docLength = docTokens.size,
            avgDocLength = avgDocLength,
            totalDocs = corpus.size,
            termDocFrequencies = termDocFreq
        )
    }
}
