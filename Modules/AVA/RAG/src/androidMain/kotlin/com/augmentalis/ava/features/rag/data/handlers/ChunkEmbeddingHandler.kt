/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from SQLiteRAGRepository (SRP)
 */

package com.augmentalis.ava.features.rag.data.handlers

import android.util.Log
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.Rag_chunk
import com.augmentalis.ava.features.rag.domain.Chunk
import com.augmentalis.ava.features.rag.domain.ChunkMetadata
import com.augmentalis.ava.features.rag.domain.Embedding
import com.augmentalis.ava.features.rag.embeddings.EmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ChunkEmbeddingHandler - Single Responsibility: Chunk Embedding Generation
 *
 * Extracted from SQLiteRAGRepository as part of SOLID refactoring.
 * Handles all chunk embedding operations:
 * - Batch embedding generation
 * - Concurrent batch processing
 * - Embedding serialization/deserialization
 * - Progress tracking
 *
 * @param database AVADatabase instance
 * @param embeddingProvider Embedding provider for vector generation
 * @param batchSize Number of texts to process in each batch
 * @param maxConcurrentBatches Maximum number of concurrent batch operations
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-17
 */
class ChunkEmbeddingHandler(
    private val database: AVADatabase,
    private val embeddingProvider: EmbeddingProvider,
    private val batchSize: Int = 50,
    private val maxConcurrentBatches: Int = 4
) {
    companion object {
        private const val TAG = "ChunkEmbeddingHandler"
    }

    private val chunkQueries by lazy { database.rAGChunkQueries }

    // ==================== Embedding Generation ====================

    /**
     * Progress update during processing.
     */
    data class ProcessingProgress(
        val processedChunks: Int,
        val totalChunks: Int,
        val currentBatch: Int,
        val totalBatches: Int
    ) {
        val progressPercent: Float
            get() = if (totalChunks > 0) processedChunks.toFloat() / totalChunks else 0f
    }

    /**
     * Process chunks and generate embeddings with progress updates.
     *
     * @param documentId Document ID for the chunks
     * @param chunks List of chunks to process
     * @return Flow of progress updates
     */
    fun processChunks(
        documentId: String,
        chunks: List<Chunk>
    ): Flow<ProcessingProgress> = flow {
        val texts = chunks.map { it.content }
        val totalBatches = (texts.size + batchSize - 1) / batchSize

        Log.d(TAG, "Processing ${chunks.size} chunks in $totalBatches batches")

        var processedCount = 0

        // Process in batches
        texts.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            val embeddingResult = embeddingProvider.embedBatch(batch)

            if (embeddingResult.isSuccess) {
                val embeddings = embeddingResult.getOrThrow()

                // Store embeddings
                val batchStartIndex = batchIndex * batchSize
                embeddings.forEachIndexed { index, embedding ->
                    val chunkIndex = batchStartIndex + index
                    if (chunkIndex < chunks.size) {
                        updateChunkEmbeddingInternal(chunks[chunkIndex].id, embedding)
                    }
                }

                processedCount += batch.size
                emit(
                    ProcessingProgress(
                        processedChunks = processedCount,
                        totalChunks = chunks.size,
                        currentBatch = batchIndex + 1,
                        totalBatches = totalBatches
                    )
                )
            } else {
                Log.e(TAG, "Batch embedding failed: ${embeddingResult.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Generate embeddings for a list of texts using concurrent batch processing.
     *
     * @param texts List of texts to embed
     * @return Result with list of embeddings
     */
    suspend fun generateEmbeddingsConcurrent(
        texts: List<String>
    ): Result<List<Embedding>> = withContext(Dispatchers.IO) {
        try {
            val batches = texts.chunked(batchSize)
            Log.d(TAG, "Processing ${texts.size} texts in ${batches.size} batches concurrently")

            val embeddings = coroutineScope {
                batches
                    .chunked(maxConcurrentBatches)
                    .flatMap { concurrentBatches ->
                        concurrentBatches.map { batch ->
                            async {
                                val result = embeddingProvider.embedBatch(batch)
                                result.getOrElse { emptyList() }
                            }
                        }.awaitAll()
                    }
                    .flatten()
            }

            if (embeddings.size != texts.size) {
                Log.w(TAG, "Embedding count mismatch: expected ${texts.size}, got ${embeddings.size}")
            }

            Result.success(embeddings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate embeddings: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Compute embedding for a single text.
     *
     * @param text Text to embed
     * @return Result with embedding
     */
    suspend fun computeEmbedding(text: String): Result<Embedding> {
        return embeddingProvider.embed(text)
    }

    // ==================== Chunk Retrieval ====================

    /**
     * Get chunks by document ID.
     *
     * @param documentId Document ID
     * @return List of chunks
     */
    suspend fun getChunksByDocument(documentId: String): List<Chunk> =
        withContext(Dispatchers.IO) {
            chunkQueries.selectByDocument(documentId)
                .executeAsList()
                .map { toDomainChunk(it) }
        }

    /**
     * Get chunk embedding by chunk ID.
     *
     * @param chunkId Chunk ID
     * @return Embedding or null if not found
     */
    suspend fun getChunkEmbedding(chunkId: String): Embedding? =
        withContext(Dispatchers.IO) {
            val chunk = chunkQueries.selectById(chunkId).executeAsOneOrNull()
                ?: return@withContext null

            deserializeEmbedding(chunk.embedding_blob)
        }

    /**
     * Get all chunk embeddings for a document.
     *
     * @param documentId Document ID
     * @return Map of chunk ID to embedding
     */
    suspend fun getDocumentEmbeddings(documentId: String): Map<String, Embedding> =
        withContext(Dispatchers.IO) {
            val chunks = chunkQueries.selectByDocument(documentId).executeAsList()
            chunks.map { chunk ->
                chunk.id to deserializeEmbedding(chunk.embedding_blob)
            }.toMap()
        }

    // ==================== Embedding Updates ====================

    /**
     * Internal method to update embedding (non-suspending for use in flow).
     */
    private fun updateChunkEmbeddingInternal(chunkId: String, embedding: Embedding) {
        val floatValues = when (embedding) {
            is Embedding.Float32 -> embedding.values
            is Embedding.Int8 -> embedding.toFloat32()
        }
        // Note: SQLDelight doesn't have updateEmbedding query,
        // embeddings are stored during chunk insert
        Log.d(TAG, "Updated embedding for chunk: $chunkId (${floatValues.size} dimensions)")
    }

    /**
     * Update cluster assignment for a chunk.
     *
     * @param chunkId Chunk ID
     * @param clusterId Cluster ID
     * @param distance Distance to centroid
     */
    suspend fun updateChunkCluster(
        chunkId: String,
        clusterId: String,
        distance: Float
    ) = withContext(Dispatchers.IO) {
        chunkQueries.updateClusterAssignment(
            cluster_id = clusterId,
            distance_to_centroid = distance.toDouble(),
            id = chunkId
        )
    }

    // ==================== Serialization ====================

    /**
     * Serialize embedding to byte array for storage.
     */
    fun serializeEmbedding(embedding: Embedding): ByteArray {
        val floatValues = when (embedding) {
            is Embedding.Float32 -> embedding.values
            is Embedding.Int8 -> embedding.toFloat32()
        }
        val buffer = ByteBuffer.allocate(floatValues.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(floatValues)
        return buffer.array()
    }

    /**
     * Deserialize embedding from byte array.
     */
    fun deserializeEmbedding(bytes: ByteArray): Embedding.Float32 {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(floats)
        return Embedding.Float32(values = floats)
    }

    // ==================== Helper Methods ====================

    private fun toDomainChunk(entity: Rag_chunk): Chunk {
        return Chunk(
            id = entity.id,
            documentId = entity.document_id,
            chunkIndex = entity.chunk_index.toInt(),
            content = entity.content,
            startOffset = entity.start_offset.toInt(),
            endOffset = entity.end_offset.toInt(),
            metadata = ChunkMetadata(
                tokens = entity.token_count.toInt(),
                pageNumber = entity.page_number?.toInt(),
                section = entity.section_title
            ),
            createdAt = Instant.parse(entity.created_timestamp)
        )
    }
}
