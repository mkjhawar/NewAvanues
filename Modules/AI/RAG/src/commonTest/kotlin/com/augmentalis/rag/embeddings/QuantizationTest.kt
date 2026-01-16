// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/embeddings/QuantizationTest.kt
// created: 2025-11-28
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.embeddings

import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Comprehensive tests for INT8 quantization
 *
 * Tests cover:
 * - Quantization/dequantization roundtrip
 * - Accuracy preservation (<2% loss)
 * - Storage reduction (75%)
 * - Edge cases (constant, zero, extreme values)
 * - Cosine similarity preservation
 */
class QuantizationTest {

    /**
     * Test basic quantization roundtrip
     */
    @Test
    fun testQuantizationRoundtrip() {
        val original = FloatArray(384) { Random.nextFloat() * 2f - 1f }  // Range [-1, 1]

        val quantized = Quantization.quantizeToInt8(original)
        val reconstructed = Quantization.dequantizeFromInt8(quantized)

        // Check dimensions match
        assertEquals(original.size, reconstructed.size)

        // Verify storage reduction
        assertEquals(384, quantized.data.size)  // 1 byte each

        // Verify each value is reasonably close
        for (i in original.indices) {
            val error = (original[i] - reconstructed[i]).absoluteValue
            assertTrue(
                error < 0.02f,
                "Value $i: error $error exceeds threshold (original: ${original[i]}, reconstructed: ${reconstructed[i]})"
            )
        }
    }

    /**
     * Test accuracy loss is within acceptable threshold (<2%)
     */
    @Test
    fun testAccuracyLossThreshold() {
        val original = FloatArray(384) { Random.nextFloat() }

        val quantized = Quantization.quantizeToInt8(original)
        val metrics = Quantization.measureAccuracyLoss(original, quantized)

        // Accuracy loss should be < 2%
        assertTrue(
            metrics.accuracyLoss < 2f,
            "Accuracy loss ${metrics.accuracyLoss}% exceeds 2% threshold"
        )

        // Cosine similarity should be > 0.98
        assertTrue(
            metrics.cosineSimilarity > 0.98f,
            "Cosine similarity ${metrics.cosineSimilarity} is too low"
        )

        // Check quality threshold helper
        assertTrue(metrics.meetsQualityThreshold())
    }

    /**
     * Test storage reduction is 75%
     */
    @Test
    fun testStorageReduction() {
        val original = FloatArray(384) { Random.nextFloat() }
        val quantized = Quantization.quantizeToInt8(original)

        val float32Size = 384 * 4  // 1,536 bytes
        val int8Size = quantized.getStorageSize()  // 384 bytes + 8 bytes metadata

        val expectedReduction = ((float32Size - int8Size).toFloat() / float32Size) * 100f

        assertTrue(
            expectedReduction > 74f && expectedReduction < 76f,
            "Storage reduction $expectedReduction% is not ~75%"
        )

        // Test helper method
        val reduction = quantized.getStorageReduction()
        assertTrue(reduction > 74f && reduction < 76f)
    }

    /**
     * Test cosine similarity is preserved between two embeddings
     */
    @Test
    fun testCosineSimilarityPreserved() {
        val emb1 = FloatArray(384) { Random.nextFloat() }
        val emb2 = FloatArray(384) { Random.nextFloat() }

        // Compute original similarity
        val originalSim = cosineSimilarity(emb1, emb2)

        // Quantize both
        val quant1 = Quantization.quantizeToInt8(emb1)
        val quant2 = Quantization.quantizeToInt8(emb2)

        // Compute quantized similarity
        val quantizedSim = Quantization.cosineSimilarityQuantized(quant1, quant2)

        // Should differ by < 2%
        val difference = (originalSim - quantizedSim).absoluteValue
        assertTrue(
            difference < 0.02f,
            "Cosine similarity difference $difference exceeds 2% threshold"
        )
    }

    /**
     * Test identical embeddings remain identical after quantization
     */
    @Test
    fun testIdenticalEmbeddings() {
        val emb1 = FloatArray(384) { Random.nextFloat() }
        val emb2 = emb1.copyOf()

        val quant1 = Quantization.quantizeToInt8(emb1)
        val quant2 = Quantization.quantizeToInt8(emb2)

        val similarity = Quantization.cosineSimilarityQuantized(quant1, quant2)

        // Identical embeddings should have cosine similarity very close to 1.0
        assertTrue(
            similarity > 0.999f,
            "Identical embeddings should have ~1.0 similarity, got $similarity"
        )
    }

    /**
     * Test orthogonal embeddings remain orthogonal after quantization
     */
    @Test
    fun testOrthogonalEmbeddings() {
        // Create two orthogonal vectors
        val emb1 = FloatArray(384) { if (it < 192) 1f else 0f }
        val emb2 = FloatArray(384) { if (it >= 192) 1f else 0f }

        // Normalize
        normalizeL2(emb1)
        normalizeL2(emb2)

        val quant1 = Quantization.quantizeToInt8(emb1)
        val quant2 = Quantization.quantizeToInt8(emb2)

        val similarity = Quantization.cosineSimilarityQuantized(quant1, quant2)

        // Orthogonal embeddings should have cosine similarity close to 0
        assertTrue(
            similarity.absoluteValue < 0.1f,
            "Orthogonal embeddings should have ~0 similarity, got $similarity"
        )
    }

    /**
     * Test edge case: constant embedding
     */
    @Test
    fun testConstantEmbedding() {
        val constant = FloatArray(384) { 0.5f }

        val quantized = Quantization.quantizeToInt8(constant)
        val reconstructed = Quantization.dequantizeFromInt8(quantized)

        // All values should be the same
        for (i in reconstructed.indices) {
            assertEquals(
                constant[i],
                reconstructed[i],
                0.001f,
                "Constant value not preserved at index $i"
            )
        }
    }

    /**
     * Test edge case: zero embedding
     */
    @Test
    fun testZeroEmbedding() {
        val zero = FloatArray(384) { 0f }

        val quantized = Quantization.quantizeToInt8(zero)
        val reconstructed = Quantization.dequantizeFromInt8(quantized)

        // All values should be zero
        for (i in reconstructed.indices) {
            assertEquals(0f, reconstructed[i], 0.001f)
        }
    }

    /**
     * Test edge case: extreme values
     */
    @Test
    fun testExtremeValues() {
        val extreme = FloatArray(384) { i ->
            if (i % 2 == 0) -1000f else 1000f
        }

        val quantized = Quantization.quantizeToInt8(extreme)
        val metrics = Quantization.measureAccuracyLoss(extreme, quantized)

        // Even with extreme values, accuracy should be good
        assertTrue(metrics.cosineSimilarity > 0.95f)
    }

    /**
     * Test empty embedding throws exception
     */
    @Test
    fun testEmptyEmbeddingThrows() {
        assertFailsWith<IllegalArgumentException> {
            Quantization.quantizeToInt8(FloatArray(0))
        }
    }

    /**
     * Test dimension mismatch throws exception
     */
    @Test
    fun testDimensionMismatchThrows() {
        val emb1 = FloatArray(384) { Random.nextFloat() }
        val emb2 = FloatArray(768) { Random.nextFloat() }

        val quant1 = Quantization.quantizeToInt8(emb1)
        val quant2 = Quantization.quantizeToInt8(emb2)

        assertFailsWith<IllegalArgumentException> {
            Quantization.cosineSimilarityQuantized(quant1, quant2)
        }
    }

    /**
     * Test QuantizedEmbedding data class validation
     */
    @Test
    fun testQuantizedEmbeddingValidation() {
        val validData = ByteArray(384)

        // Valid construction
        val valid = QuantizedEmbedding(validData, 0.1f, 0.5f, 384)
        assertEquals(384, valid.dimension)

        // Invalid construction (size mismatch)
        assertFailsWith<IllegalArgumentException> {
            QuantizedEmbedding(validData, 0.1f, 0.5f, 768)
        }
    }

    /**
     * Test storage size calculation
     */
    @Test
    fun testStorageSizeCalculation() {
        val embedding = FloatArray(384) { Random.nextFloat() }
        val quantized = Quantization.quantizeToInt8(embedding)

        // Should be 384 bytes (data) + 8 bytes (scale + offset)
        assertEquals(392, quantized.getStorageSize())
    }

    /**
     * Test metrics toString output
     */
    @Test
    fun testMetricsToString() {
        val metrics = QuantizationMetrics(
            mse = 0.000123f,
            maxError = 0.015f,
            cosineSimilarity = 0.9987f,
            accuracyLoss = 0.13f
        )

        val output = metrics.toString()
        assertTrue(output.contains("MSE"))
        assertTrue(output.contains("Max Error"))
        assertTrue(output.contains("Cosine Similarity"))
        assertTrue(output.contains("Accuracy Loss"))
    }

    /**
     * Test quantization is deterministic
     */
    @Test
    fun testDeterministic() {
        val embedding = FloatArray(384) { Random.nextFloat() }

        val quant1 = Quantization.quantizeToInt8(embedding)
        val quant2 = Quantization.quantizeToInt8(embedding)

        assertEquals(quant1, quant2)
    }

    /**
     * Test large batch (10,000 embeddings) for performance validation
     */
    @Test
    fun testLargeBatch() {
        val batchSize = 10000
        val embeddings = List(batchSize) { FloatArray(384) { Random.nextFloat() } }

        var totalFloat32Size = 0L
        var totalInt8Size = 0L
        var totalAccuracyLoss = 0f

        for (embedding in embeddings) {
            val quantized = Quantization.quantizeToInt8(embedding)
            val metrics = Quantization.measureAccuracyLoss(embedding, quantized)

            totalFloat32Size += embedding.size * 4
            totalInt8Size += quantized.getStorageSize()
            totalAccuracyLoss += metrics.accuracyLoss
        }

        val averageAccuracyLoss = totalAccuracyLoss / batchSize
        val storageReduction = ((totalFloat32Size - totalInt8Size).toFloat() / totalFloat32Size) * 100f

        // Average accuracy loss should be < 2%
        assertTrue(
            averageAccuracyLoss < 2f,
            "Average accuracy loss $averageAccuracyLoss% exceeds 2%"
        )

        // Storage reduction should be ~75%
        assertTrue(
            storageReduction > 74f && storageReduction < 76f,
            "Storage reduction $storageReduction% is not ~75%"
        )

        // Print statistics for verification
        println("Large batch test ($batchSize embeddings):")
        println("  Float32 size: ${totalFloat32Size / 1024 / 1024} MB")
        println("  INT8 size: ${totalInt8Size / 1024 / 1024} MB")
        println("  Storage reduction: ${"%.2f".format(storageReduction)}%")
        println("  Average accuracy loss: ${"%.4f".format(averageAccuracyLoss)}%")
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

        val denominator = sqrt(normA * normB)
        return if (denominator > 1e-8f) dotProduct / denominator else 0f
    }

    private fun normalizeL2(array: FloatArray) {
        val norm = sqrt(array.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0) {
            for (i in array.indices) {
                array[i] /= norm
            }
        }
    }
}
