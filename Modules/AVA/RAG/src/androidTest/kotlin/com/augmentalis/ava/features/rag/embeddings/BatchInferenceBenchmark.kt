// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/embeddings/BatchInferenceBenchmark.kt
// created: 2025-11-28
// author: AVA AI Team - Batch Inference Specialist
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.features.rag.domain.Embedding
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance benchmarks for batch ONNX inference
 *
 * Validates Phase 3.2 (P2) optimization:
 * - 20x speedup: 10s → 500ms for 1,000 chunks
 * - Memory safety: No OOM on 10,000 chunk batch
 * - Accuracy: Batch results identical to sequential
 *
 * Performance Targets:
 * - 1,000 texts sequential: ~10,000ms (10s)
 * - 1,000 texts batch: <500ms (20x speedup)
 * - Accuracy: >99% cosine similarity between methods
 */
@RunWith(AndroidJUnit4::class)
class BatchInferenceBenchmark {

    private lateinit var embeddingProvider: ONNXEmbeddingProvider

    @Before
    fun setup() {
        embeddingProvider = ONNXEmbeddingProvider(
            context = ApplicationProvider.getApplicationContext(),
            modelId = "AVA-384-Base-INT8"
        )
    }

    @Test
    fun benchmarkBatchVsSequential_1000Texts() = runBlocking {
        val texts = List(1000) { "Sample text number $it for embedding benchmark test" }

        // Sequential processing (old way)
        val sequentialStart = System.currentTimeMillis()
        val sequentialEmbeddings = mutableListOf<Embedding.Float32>()
        for (text in texts) {
            val result = embeddingProvider.embed(text)
            if (result.isSuccess) {
                sequentialEmbeddings.add(result.getOrThrow())
            }
        }
        val sequentialTime = System.currentTimeMillis() - sequentialStart

        // Batch processing (new way)
        val batchStart = System.currentTimeMillis()
        val batchResult = embeddingProvider.embedBatch(texts)
        val batchTime = System.currentTimeMillis() - batchStart

        assertTrue("Batch processing failed", batchResult.isSuccess)
        val batchEmbeddings = batchResult.getOrThrow()

        // Calculate speedup
        val speedup = sequentialTime.toFloat() / batchTime

        Log.i("BatchBenchmark", "=== Batch vs Sequential (1,000 texts) ===")
        Log.i("BatchBenchmark", "Sequential: ${sequentialTime}ms")
        Log.i("BatchBenchmark", "Batch: ${batchTime}ms")
        Log.i("BatchBenchmark", "Speedup: ${String.format("%.1f", speedup)}x")

        // Verify embeddings count
        assertEquals("Sequential embedding count", 1000, sequentialEmbeddings.size)
        assertEquals("Batch embedding count", 1000, batchEmbeddings.size)

        // Verify 20x speedup (allow some variance: minimum 15x)
        assertTrue(
            "Expected >15x speedup, got ${String.format("%.1f", speedup)}x (sequential: ${sequentialTime}ms, batch: ${batchTime}ms)",
            speedup > 15f
        )

        // Verify batch processing meets target (<500ms for 1000 texts)
        assertTrue(
            "Batch processing should complete in <500ms, took ${batchTime}ms",
            batchTime < 500
        )
    }

    @Test
    fun testBatchAccuracy() = runBlocking {
        val texts = listOf(
            "hello world",
            "goodbye world",
            "test text for embeddings",
            "artificial intelligence assistant",
            "machine learning model"
        )

        // Sequential embeddings
        val sequential = texts.map { text ->
            embeddingProvider.embed(text).getOrThrow()
        }

        // Batch embeddings
        val batch = embeddingProvider.embedBatch(texts).getOrThrow()

        // Verify same count
        assertEquals("Embedding count mismatch", texts.size, batch.size)

        // Verify results are nearly identical (>99% similarity)
        for (i in texts.indices) {
            val similarity = cosineSimilarity(sequential[i].values, batch[i].values)

            Log.i("BatchAccuracy", "Text $i: similarity = ${String.format("%.4f", similarity)}")

            assertTrue(
                "Text $i: Expected >0.99 similarity, got ${String.format("%.4f", similarity)}",
                similarity > 0.99f
            )
        }
    }

    @Test
    fun testMemorySafety_10000Chunks() = runBlocking {
        // Test that we can process 10,000 chunks without OOM
        val texts = List(10000) { "Memory safety test chunk $it" }

        var peakMemoryMB = 0L

        val time = measureTimeMillis {
            val result = embeddingProvider.embedBatch(texts)

            assertTrue("Batch processing failed for 10k chunks", result.isSuccess)

            val embeddings = result.getOrThrow()
            assertEquals("Should get 10k embeddings", 10000, embeddings.size)

            // Check memory usage
            val runtime = Runtime.getRuntime()
            val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            peakMemoryMB = usedMemoryMB
        }

        Log.i("BatchMemory", "=== Memory Safety Test (10,000 chunks) ===")
        Log.i("BatchMemory", "Time: ${time}ms")
        Log.i("BatchMemory", "Peak memory: ${peakMemoryMB}MB")

        // Should complete in reasonable time (< 5 seconds for 10k chunks)
        assertTrue(
            "10k chunks should process in <5s, took ${time}ms",
            time < 5000
        )

        // Memory should stay reasonable (< 500MB)
        assertTrue(
            "Memory usage should be <500MB, used ${peakMemoryMB}MB",
            peakMemoryMB < 500
        )
    }

    @Test
    fun testBatchSizeHandling() = runBlocking {
        // Test various batch sizes
        val batchSizes = listOf(1, 10, 32, 100, 500)

        for (batchSize in batchSizes) {
            val texts = List(batchSize) { "Test text $it" }

            val time = measureTimeMillis {
                val result = embeddingProvider.embedBatch(texts)

                assertTrue("Batch size $batchSize failed", result.isSuccess)

                val embeddings = result.getOrThrow()
                assertEquals(
                    "Expected $batchSize embeddings",
                    batchSize,
                    embeddings.size
                )
            }

            Log.i("BatchSize", "Batch size $batchSize: ${time}ms")
        }
    }

    @Test
    fun testEmbeddingDimensions() = runBlocking {
        val texts = listOf("test text 1", "test text 2", "test text 3")

        val result = embeddingProvider.embedBatch(texts)
        assertTrue("Batch embedding failed", result.isSuccess)

        val embeddings = result.getOrThrow()

        // Verify all embeddings have correct dimension (384 for all-MiniLM-L6-v2)
        embeddings.forEach { embedding ->
            assertEquals(
                "Expected 384 dimensions",
                384,
                embedding.values.size
            )
        }

        // Verify embeddings are normalized (L2 norm should be ~1.0)
        embeddings.forEach { embedding ->
            val norm = kotlin.math.sqrt(
                embedding.values.sumOf { (it * it).toDouble() }
            ).toFloat()

            assertTrue(
                "L2 norm should be ~1.0, got ${String.format("%.4f", norm)}",
                kotlin.math.abs(norm - 1.0f) < 0.01f
            )
        }
    }

    @Test
    fun testEmptyBatch() = runBlocking {
        val result = embeddingProvider.embedBatch(emptyList())

        assertTrue("Empty batch should succeed", result.isSuccess)
        assertEquals("Empty batch should return empty list", 0, result.getOrThrow().size)
    }

    @Test
    fun testSingleTextBatch() = runBlocking {
        val result = embeddingProvider.embedBatch(listOf("single text"))

        assertTrue("Single text batch should succeed", result.isSuccess)
        assertEquals("Should get 1 embedding", 1, result.getOrThrow().size)
    }

    /**
     * Helper: Calculate cosine similarity between two embeddings
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        val denominator = kotlin.math.sqrt(norm1 * norm2)
        return if (denominator > 0f) dotProduct / denominator else 0f
    }
}
