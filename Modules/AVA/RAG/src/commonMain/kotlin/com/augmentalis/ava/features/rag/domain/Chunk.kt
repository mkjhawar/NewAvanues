// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/domain/Chunk.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a chunk of text extracted from a document
 *
 * Chunks are the atomic units of retrieval in the RAG system.
 * Each chunk contains a portion of the document text along with
 * its embedding vector for semantic search.
 */
@Serializable
data class Chunk(
    val id: String,
    val documentId: String,
    val content: String,
    val chunkIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val metadata: ChunkMetadata,
    val createdAt: Instant
)

/**
 * Metadata associated with a chunk
 */
@Serializable
data class ChunkMetadata(
    val section: String? = null,
    val heading: String? = null,
    val pageNumber: Int? = null,
    val tokens: Int,
    val semanticType: SemanticType = SemanticType.PARAGRAPH,
    val importance: Float = 0.5f  // 0.0 to 1.0
)

/**
 * Semantic type of a chunk for better retrieval
 */
@Serializable
enum class SemanticType {
    /** Heading or title */
    HEADING,

    /** Regular paragraph */
    PARAGRAPH,

    /** List item */
    LIST_ITEM,

    /** Code block */
    CODE,

    /** Table cell or data */
    TABLE,

    /** Quote or callout */
    QUOTE,

    /** Caption or footnote */
    CAPTION
}

/**
 * A chunk with its embedding vector
 */
data class EmbeddedChunk(
    val chunk: Chunk,
    val embedding: Embedding
)

/**
 * Vector embedding for a chunk
 *
 * Embeddings can be stored in different formats:
 * - Float32: Full precision (384 floats = 1536 bytes)
 * - Int8: Quantized (384 bytes + 8 bytes metadata = 392 bytes)
 */
sealed class Embedding {
    abstract val dimension: Int

    /**
     * Full precision float32 embedding
     */
    data class Float32(val values: FloatArray) : Embedding() {
        override val dimension: Int get() = values.size

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Float32
            return values.contentEquals(other.values)
        }

        override fun hashCode(): Int {
            return values.contentHashCode()
        }
    }

    /**
     * Quantized int8 embedding (75% space savings)
     */
    data class Int8(
        val values: ByteArray,
        val scale: Float,
        val offset: Float
    ) : Embedding() {
        override val dimension: Int get() = values.size

        /**
         * Reconstruct approximate float32 values
         */
        fun toFloat32(): FloatArray {
            return FloatArray(values.size) { i ->
                (values[i].toInt() and 0xFF) * scale + offset
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Int8
            if (!values.contentEquals(other.values)) return false
            if (scale != other.scale) return false
            if (offset != other.offset) return false
            return true
        }

        override fun hashCode(): Int {
            var result = values.contentHashCode()
            result = 31 * result + scale.hashCode()
            result = 31 * result + offset.hashCode()
            return result
        }
    }

    companion object {
        /**
         * Quantize a float32 embedding to int8
         *
         * This reduces storage by 75% with ~3% accuracy loss
         */
        fun quantize(values: FloatArray): Int8 {
            val min = values.minOrNull() ?: 0f
            val max = values.maxOrNull() ?: 0f
            val scale = (max - min) / 255f
            val offset = min

            val quantized = ByteArray(values.size) { i ->
                ((values[i] - offset) / scale).toInt().toByte()
            }

            return Int8(quantized, scale, offset)
        }
    }
}

/**
 * Chunking strategy for document splitting
 */
@Serializable
enum class ChunkingStrategy {
    /** Fixed-size chunks with overlap */
    FIXED_SIZE,

    /** Semantic chunking by document structure */
    SEMANTIC,

    /** Hybrid: semantic + LLM-assisted for complex docs */
    HYBRID
}

/**
 * Configuration for chunking
 */
@Serializable
data class ChunkingConfig(
    val strategy: ChunkingStrategy = ChunkingStrategy.HYBRID,
    val maxTokens: Int = 512,
    val overlapTokens: Int = 50,
    val respectSectionBoundaries: Boolean = true,
    val minChunkTokens: Int = 100
)
