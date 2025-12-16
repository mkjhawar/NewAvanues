// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/embeddings/QuantizationBenchmarkTest.kt
// created: 2025-11-28
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Storage benchmark for INT8 quantization
 *
 * Validates Phase 3.1 requirements:
 * - 75% storage reduction
 * - <2% accuracy loss
 * - <1ms quantization per vector
 *
 * Simulates real-world RAG usage with 10,000 document chunks.
 */
class QuantizationBenchmarkTest {

    /**
     * Benchmark storage savings with 10,000 embeddings
     *
     * Expected results:
     * - Float32: ~15.36 MB
     * - INT8: ~3.84 MB
     * - Reduction: 75%
     */
    @Test
    fun testStorageSavingsWithTenThousandEmbeddings() {
        val numEmbeddings = 10_000
        val dimension = 384

        println("\n=== Storage Benchmark: 10,000 Embeddings ===")

        // Generate random embeddings (simulating real document chunks)
        val embeddings = List(numEmbeddings) {
            FloatArray(dimension) { Random.nextFloat() * 2f - 1f }
        }

        // Calculate Float32 storage
        val float32BytesPerEmbedding = dimension * 4  // 4 bytes per float
        val totalFloat32Bytes = numEmbeddings * float32BytesPerEmbedding
        val totalFloat32MB = totalFloat32Bytes / 1024.0 / 1024.0

        // Quantize all embeddings and measure time
        val quantizedEmbeddings = mutableListOf<QuantizedEmbedding>()
        val quantizationTimeMs = measureTimeMillis {
            embeddings.forEach { embedding ->
                quantizedEmbeddings.add(Quantization.quantizeToInt8(embedding))
            }
        }

        // Calculate INT8 storage
        val totalInt8Bytes = quantizedEmbeddings.sumOf { it.getStorageSize() }
        val totalInt8MB = totalInt8Bytes / 1024.0 / 1024.0

        // Calculate reduction
        val reductionBytes = totalFloat32Bytes - totalInt8Bytes
        val reductionPercent = (reductionBytes.toDouble() / totalFloat32Bytes) * 100.0

        // Calculate average time per embedding
        val avgTimePerEmbedding = quantizationTimeMs.toDouble() / numEmbeddings

        // Print results
        println("Float32 Storage:")
        println("  Total: ${"%.2f".format(totalFloat32MB)} MB")
        println("  Per embedding: $float32BytesPerEmbedding bytes")
        println()
        println("INT8 Storage:")
        println("  Total: ${"%.2f".format(totalInt8MB)} MB")
        println("  Per embedding: ${totalInt8Bytes / numEmbeddings} bytes (avg)")
        println()
        println("Storage Reduction:")
        println("  Absolute: ${"%.2f".format(reductionBytes / 1024.0 / 1024.0)} MB")
        println("  Percentage: ${"%.2f".format(reductionPercent)}%")
        println()
        println("Performance:")
        println("  Total quantization time: $quantizationTimeMs ms")
        println("  Average per embedding: ${"%.3f".format(avgTimePerEmbedding)} ms")
        println()

        // Verify requirements
        assertTrue(
            reductionPercent > 74.0 && reductionPercent < 76.0,
            "Storage reduction ${"%.2f".format(reductionPercent)}% is not ~75%"
        )

        assertTrue(
            avgTimePerEmbedding < 1.0,
            "Average quantization time ${"%.3f".format(avgTimePerEmbedding)} ms exceeds 1ms threshold"
        )
    }

    /**
     * Benchmark accuracy preservation with 10,000 embeddings
     *
     * Expected results:
     * - Average accuracy loss: <2%
     * - Max accuracy loss: <5%
     * - Average cosine similarity: >0.98
     */
    @Test
    fun testAccuracyPreservationWithTenThousandEmbeddings() {
        val numEmbeddings = 10_000
        val dimension = 384

        println("\n=== Accuracy Benchmark: 10,000 Embeddings ===")

        // Generate random embeddings
        val embeddings = List(numEmbeddings) {
            FloatArray(dimension) { Random.nextFloat() * 2f - 1f }
        }

        // Quantize and measure accuracy
        var totalAccuracyLoss = 0.0
        var maxAccuracyLoss = 0.0
        var totalCosineSim = 0.0
        var minCosineSim = 1.0
        var totalMse = 0.0

        embeddings.forEach { embedding ->
            val quantized = Quantization.quantizeToInt8(embedding)
            val metrics = Quantization.measureAccuracyLoss(embedding, quantized)

            totalAccuracyLoss += metrics.accuracyLoss
            if (metrics.accuracyLoss > maxAccuracyLoss) {
                maxAccuracyLoss = metrics.accuracyLoss.toDouble()
            }

            totalCosineSim += metrics.cosineSimilarity
            if (metrics.cosineSimilarity < minCosineSim) {
                minCosineSim = metrics.cosineSimilarity.toDouble()
            }

            totalMse += metrics.mse
        }

        // Calculate averages
        val avgAccuracyLoss = totalAccuracyLoss / numEmbeddings
        val avgCosineSim = totalCosineSim / numEmbeddings
        val avgMse = totalMse / numEmbeddings

        // Print results
        println("Accuracy Metrics:")
        println("  Average accuracy loss: ${"%.4f".format(avgAccuracyLoss)}%")
        println("  Max accuracy loss: ${"%.4f".format(maxAccuracyLoss)}%")
        println("  Average cosine similarity: ${"%.6f".format(avgCosineSim)}")
        println("  Min cosine similarity: ${"%.6f".format(minCosineSim)}")
        println("  Average MSE: ${"%.8f".format(avgMse)}")
        println()

        // Verify requirements
        assertTrue(
            avgAccuracyLoss < 2.0,
            "Average accuracy loss ${"%.4f".format(avgAccuracyLoss)}% exceeds 2% threshold"
        )

        assertTrue(
            maxAccuracyLoss < 5.0,
            "Max accuracy loss ${"%.4f".format(maxAccuracyLoss)}% exceeds 5% threshold"
        )

        assertTrue(
            avgCosineSim > 0.98,
            "Average cosine similarity ${"%.6f".format(avgCosineSim)} is below 0.98 threshold"
        )
    }

    /**
     * Benchmark semantic search preservation
     *
     * Tests that ranking order is preserved after quantization.
     */
    @Test
    fun testSemanticSearchRankingPreservation() {
        val numDocuments = 1000
        val dimension = 384

        println("\n=== Semantic Search Ranking Benchmark ===")

        // Create query and document embeddings
        val queryEmbedding = FloatArray(dimension) { Random.nextFloat() }
        val documentEmbeddings = List(numDocuments) {
            FloatArray(dimension) { Random.nextFloat() }
        }

        // Compute similarities with Float32
        val float32Similarities = documentEmbeddings.map { doc ->
            cosineSimilarity(queryEmbedding, doc)
        }
        val float32Ranking = float32Similarities
            .withIndex()
            .sortedByDescending { it.value }
            .map { it.index }

        // Quantize all embeddings
        val queryQuantized = Quantization.quantizeToInt8(queryEmbedding)
        val docsQuantized = documentEmbeddings.map { Quantization.quantizeToInt8(it) }

        // Compute similarities with INT8
        val int8Similarities = docsQuantized.map { doc ->
            Quantization.cosineSimilarityQuantized(queryQuantized, doc)
        }
        val int8Ranking = int8Similarities
            .withIndex()
            .sortedByDescending { it.value }
            .map { it.index }

        // Calculate ranking correlation (top-k overlap)
        val topK = listOf(1, 5, 10, 20, 50, 100)
        println("Top-K Ranking Preservation:")

        topK.forEach { k ->
            val float32TopK = float32Ranking.take(k).toSet()
            val int8TopK = int8Ranking.take(k).toSet()
            val overlap = float32TopK.intersect(int8TopK).size
            val preservation = (overlap.toDouble() / k) * 100.0

            println("  Top-$k: ${"%.1f".format(preservation)}% overlap ($overlap/$k)")

            // Should have high overlap for small k
            if (k <= 20) {
                assertTrue(
                    preservation > 80.0,
                    "Top-$k preservation ${"%.1f".format(preservation)}% is too low"
                )
            }
        }

        // Calculate Spearman rank correlation
        val rankCorrelation = calculateSpearmanCorrelation(float32Similarities, int8Similarities)
        println()
        println("Spearman Rank Correlation: ${"%.4f".format(rankCorrelation)}")

        assertTrue(
            rankCorrelation > 0.95,
            "Rank correlation ${"%.4f".format(rankCorrelation)} is too low"
        )
    }

    /**
     * Benchmark database size reduction for realistic RAG scenario
     */
    @Test
    fun testRealisticRAGDatabaseSize() {
        println("\n=== Realistic RAG Database Size Benchmark ===")

        // Realistic scenario: 200-page technical manual
        val pagesPerDocument = 200
        val chunksPerPage = 3  // ~500 tokens per chunk
        val totalChunks = pagesPerDocument * chunksPerPage  // 600 chunks
        val dimension = 384

        println("Scenario: 200-page technical manual")
        println("  Pages: $pagesPerDocument")
        println("  Chunks per page: $chunksPerPage")
        println("  Total chunks: $totalChunks")
        println()

        // Generate embeddings
        val embeddings = List(totalChunks) {
            FloatArray(dimension) { Random.nextFloat() }
        }

        // Calculate sizes
        val float32Size = embeddings.size * dimension * 4
        val int8Size = embeddings.sumOf {
            Quantization.quantizeToInt8(it).getStorageSize()
        }

        val float32MB = float32Size / 1024.0 / 1024.0
        val int8MB = int8Size / 1024.0 / 1024.0
        val savings = float32Size - int8Size
        val savingsMB = savings / 1024.0 / 1024.0
        val savingsPercent = (savings.toDouble() / float32Size) * 100.0

        println("Database Size:")
        println("  Float32: ${"%.2f".format(float32MB)} MB")
        println("  INT8: ${"%.2f".format(int8MB)} MB")
        println("  Savings: ${"%.2f".format(savingsMB)} MB (${"%.1f".format(savingsPercent)}%)")
        println()

        // 10 documents scenario
        val numDocuments = 10
        val totalWithMultipleDocuments = totalChunks * numDocuments

        val float32SizeMultiple = totalWithMultipleDocuments * dimension * 4
        val int8SizeMultiple = totalWithMultipleDocuments * (dimension + 8)

        val float32MBMultiple = float32SizeMultiple / 1024.0 / 1024.0
        val int8MBMultiple = int8SizeMultiple / 1024.0 / 1024.0

        println("Scaled to $numDocuments documents ($totalWithMultipleDocuments chunks):")
        println("  Float32: ${"%.2f".format(float32MBMultiple)} MB")
        println("  INT8: ${"%.2f".format(int8MBMultiple)} MB")
        println("  Savings: ${"%.2f".format(float32MBMultiple - int8MBMultiple)} MB")
        println()
    }

    // Helper functions

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = kotlin.math.sqrt(normA * normB)
        return if (denominator > 1e-8f) dotProduct / denominator else 0f
    }

    private fun calculateSpearmanCorrelation(
        values1: List<Float>,
        values2: List<Float>
    ): Double {
        require(values1.size == values2.size)

        // Convert to ranks
        val ranks1 = values1.withIndex()
            .sortedByDescending { it.value }
            .withIndex()
            .associate { it.value.index to it.index }

        val ranks2 = values2.withIndex()
            .sortedByDescending { it.value }
            .withIndex()
            .associate { it.value.index to it.index }

        // Calculate correlation
        val n = values1.size
        var sumSquaredDiff = 0.0

        for (i in values1.indices) {
            val rank1 = ranks1[i] ?: 0
            val rank2 = ranks2[i] ?: 0
            val diff = rank1 - rank2
            sumSquaredDiff += diff * diff
        }

        return 1.0 - (6.0 * sumSquaredDiff) / (n * (n * n - 1))
    }
}
