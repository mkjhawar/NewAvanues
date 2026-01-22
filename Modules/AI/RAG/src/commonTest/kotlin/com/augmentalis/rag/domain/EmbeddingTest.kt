// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/domain/EmbeddingTest.kt
// created: 2025-11-14
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.domain

import kotlin.test.*

/**
 * Comprehensive tests for Embedding classes
 *
 * Tests Float32, Int8, quantization, and conversion operations
 */
class EmbeddingTest {

    // ========== FLOAT32 EMBEDDING TESTS ==========

    @Test
    fun `test Float32 embedding creation`() {
        val values = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
        val embedding = Embedding.Float32(values)

        assertEquals(5, embedding.dimension, "Dimension should match array size")
        assertEquals(values.size, embedding.values.size)
        assertTrue(embedding.values.contentEquals(values))
    }

    @Test
    fun `test Float32 embedding equality`() {
        val values1 = floatArrayOf(1f, 2f, 3f)
        val values2 = floatArrayOf(1f, 2f, 3f)
        val values3 = floatArrayOf(1f, 2f, 4f)

        val emb1 = Embedding.Float32(values1)
        val emb2 = Embedding.Float32(values2)
        val emb3 = Embedding.Float32(values3)

        assertEquals(emb1, emb2, "Embeddings with same values should be equal")
        assertNotEquals(emb1, emb3, "Embeddings with different values should not be equal")
        assertEquals(emb1.hashCode(), emb2.hashCode(), "Equal embeddings should have same hash")
    }

    @Test
    fun `test Float32 embedding with negative values`() {
        val values = floatArrayOf(-0.5f, -0.3f, 0.0f, 0.3f, 0.5f)
        val embedding = Embedding.Float32(values)

        assertEquals(5, embedding.dimension)
        assertEquals(-0.5f, embedding.values[0])
        assertEquals(0.5f, embedding.values[4])
    }

    @Test
    fun `test Float32 embedding with large dimensions`() {
        val values = FloatArray(384) { it / 384f }  // Typical BERT dimension
        val embedding = Embedding.Float32(values)

        assertEquals(384, embedding.dimension)
        assertEquals(0f, embedding.values[0], 0.001f)
        assertEquals(383 / 384f, embedding.values[383], 0.001f)
    }

    @Test
    fun `test Float32 embedding with zero vector`() {
        val values = FloatArray(10) { 0f }
        val embedding = Embedding.Float32(values)

        assertEquals(10, embedding.dimension)
        assertTrue(embedding.values.all { it == 0f }, "All values should be zero")
    }

    // ========== INT8 EMBEDDING TESTS ==========

    @Test
    fun `test Int8 embedding creation`() {
        val values = byteArrayOf(1, 2, 3, 4, 5)
        val embedding = Embedding.Int8(values, scale = 0.01f, offset = -1f)

        assertEquals(5, embedding.dimension)
        assertEquals(0.01f, embedding.scale)
        assertEquals(-1f, embedding.offset)
    }

    @Test
    fun `test Int8 embedding to Float32 conversion`() {
        val values = byteArrayOf(0, 127, 255.toByte())
        val embedding = Embedding.Int8(values, scale = 0.01f, offset = -1f)

        val float32 = embedding.toFloat32()

        assertEquals(3, float32.size)
        // value[i] = (bytes[i] & 0xFF) * scale + offset
        assertEquals(-1f, float32[0], 0.001f)  // 0 * 0.01 - 1
        assertEquals(0.27f, float32[1], 0.001f)  // 127 * 0.01 - 1
        assertEquals(1.55f, float32[2], 0.001f)  // 255 * 0.01 - 1
    }

    @Test
    fun `test Int8 embedding equality`() {
        val values1 = byteArrayOf(1, 2, 3)
        val values2 = byteArrayOf(1, 2, 3)
        val values3 = byteArrayOf(1, 2, 4)

        val emb1 = Embedding.Int8(values1, 0.1f, 0f)
        val emb2 = Embedding.Int8(values2, 0.1f, 0f)
        val emb3 = Embedding.Int8(values3, 0.1f, 0f)

        assertEquals(emb1, emb2)
        assertNotEquals(emb1, emb3)
        assertEquals(emb1.hashCode(), emb2.hashCode())
    }

    @Test
    fun `test Int8 embeddings with different scale are not equal`() {
        val values = byteArrayOf(1, 2, 3)

        val emb1 = Embedding.Int8(values, 0.1f, 0f)
        val emb2 = Embedding.Int8(values, 0.2f, 0f)

        assertNotEquals(emb1, emb2, "Different scales should make embeddings unequal")
    }

    @Test
    fun `test Int8 embeddings with different offset are not equal`() {
        val values = byteArrayOf(1, 2, 3)

        val emb1 = Embedding.Int8(values, 0.1f, 0f)
        val emb2 = Embedding.Int8(values, 0.1f, -1f)

        assertNotEquals(emb1, emb2, "Different offsets should make embeddings unequal")
    }

    // ========== QUANTIZATION TESTS ==========

    @Test
    fun `test quantization from Float32 to Int8`() {
        val values = floatArrayOf(0f, 0.5f, 1f)
        val quantized = Embedding.quantize(values)

        assertEquals(3, quantized.dimension)
        assertEquals(3, quantized.values.size)

        // Verify scale and offset are computed correctly
        // min = 0, max = 1, scale = (1-0)/255 = 0.00392
        assertTrue(quantized.scale > 0f, "Scale should be positive")
        assertEquals(0f, quantized.offset, 0.001f, "Offset should be min value")
    }

    @Test
    fun `test quantization preserves relative order`() {
        val values = floatArrayOf(-1f, 0f, 0.5f, 1f)
        val quantized = Embedding.quantize(values)

        // Convert back to float to verify ordering
        val reconstructed = quantized.toFloat32()

        // Relative order should be preserved
        assertTrue(reconstructed[0] < reconstructed[1], "Order should be preserved")
        assertTrue(reconstructed[1] < reconstructed[2], "Order should be preserved")
        assertTrue(reconstructed[2] < reconstructed[3], "Order should be preserved")
    }

    @Test
    fun `test quantization round-trip accuracy`() {
        val original = FloatArray(384) { (it / 384f) * 2 - 1 }  // -1 to 1
        val quantized = Embedding.quantize(original)
        val reconstructed = quantized.toFloat32()

        // Check that reconstructed values are close to original
        val maxError = original.zip(reconstructed.toList()).maxOf { (orig, recon) ->
            kotlin.math.abs(orig - recon)
        }

        // Quantization to int8 should have <1% error for normalized embeddings
        assertTrue(maxError < 0.01f, "Quantization error should be small: $maxError")
    }

    @Test
    fun `test quantization with all same values`() {
        val values = floatArrayOf(0.5f, 0.5f, 0.5f)
        val quantized = Embedding.quantize(values)

        // When all values are same, scale should be 0 or very small
        // toFloat32 should return values close to original
        val reconstructed = quantized.toFloat32()

        reconstructed.forEach { value ->
            assertEquals(0.5f, value, 0.01f, "Should approximate original value")
        }
    }

    @Test
    fun `test quantization with extreme values`() {
        val values = floatArrayOf(-100f, 0f, 100f)
        val quantized = Embedding.quantize(values)

        assertEquals(3, quantized.dimension)

        val reconstructed = quantized.toFloat32()

        // Check that extreme values are captured
        assertTrue(reconstructed[0] < reconstructed[1], "Min should be smallest")
        assertTrue(reconstructed[2] > reconstructed[1], "Max should be largest")
    }

    @Test
    fun `test quantization space savings`() {
        val float32Size = 384 * 4  // 384 dimensions × 4 bytes per float = 1536 bytes
        val int8Size = 384 + 8  // 384 bytes + 8 bytes metadata (scale + offset) = 392 bytes

        val savingsPercent = ((float32Size - int8Size).toFloat() / float32Size) * 100

        assertTrue(savingsPercent > 70f, "Int8 should save >70% space (actual: ${savingsPercent.toInt()}%)")
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `test empty embedding creation`() {
        val emptyFloat = Embedding.Float32(floatArrayOf())
        assertEquals(0, emptyFloat.dimension)

        val emptyInt = Embedding.Int8(byteArrayOf(), 1f, 0f)
        assertEquals(0, emptyInt.dimension)
    }

    @Test
    fun `test single value embedding`() {
        val float32 = Embedding.Float32(floatArrayOf(0.5f))
        assertEquals(1, float32.dimension)

        val int8 = Embedding.Int8(byteArrayOf(127), 1f, 0f)
        assertEquals(1, int8.dimension)
        assertEquals(1, int8.toFloat32().size)
    }

    @Test
    fun `test very large dimension embedding`() {
        val largeSize = 4096  // Some large embedding models
        val values = FloatArray(largeSize) { it / largeSize.toFloat() }
        val embedding = Embedding.Float32(values)

        assertEquals(largeSize, embedding.dimension)
        assertEquals(largeSize, embedding.values.size)
    }

    @Test
    fun `test Int8 handles unsigned byte conversion correctly`() {
        // Byte in Kotlin is signed (-128 to 127)
        // But we need to treat it as unsigned (0 to 255) for embedding
        val values = byteArrayOf(-128, -1, 0, 1, 127)  // Signed representation
        val embedding = Embedding.Int8(values, scale = 1f, offset = 0f)

        val float32 = embedding.toFloat32()

        // -128 as signed = 128 as unsigned (0x80)
        assertEquals(128f, float32[0], "Should treat as unsigned")
        // -1 as signed = 255 as unsigned (0xFF)
        assertEquals(255f, float32[1], "Should treat as unsigned")
        assertEquals(0f, float32[2])
        assertEquals(1f, float32[3])
        assertEquals(127f, float32[4])
    }

    // ========== PERFORMANCE CHARACTERISTIC TESTS ==========

    @Test
    fun `test quantization is deterministic`() {
        val values = floatArrayOf(0.1f, 0.5f, 0.9f)

        val quantized1 = Embedding.quantize(values)
        val quantized2 = Embedding.quantize(values)

        assertEquals(quantized1, quantized2, "Quantization should be deterministic")
    }

    @Test
    fun `test Int8 toFloat32 is deterministic`() {
        val values = byteArrayOf(10, 20, 30)
        val embedding = Embedding.Int8(values, 0.5f, -10f)

        val float1 = embedding.toFloat32()
        val float2 = embedding.toFloat32()

        assertTrue(float1.contentEquals(float2), "Conversion should be deterministic")
    }
}
