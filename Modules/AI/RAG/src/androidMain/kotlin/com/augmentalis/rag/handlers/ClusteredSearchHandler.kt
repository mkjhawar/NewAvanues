/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from SQLiteRAGRepository (SRP)
 */

package com.augmentalis.rag.handlers

import android.util.Log
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.Rag_chunk
import com.augmentalis.rag.cache.QueryCache
import com.augmentalis.rag.clustering.KMeansClustering
import com.augmentalis.rag.domain.*
import com.augmentalis.rag.embeddings.EmbeddingProvider
import com.augmentalis.rag.search.BM25Scorer
import com.augmentalis.rag.search.ReciprocalRankFusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ClusteredSearchHandler - Single Responsibility: RAG Search Operations
 *
 * Extracted from SQLiteRAGRepository as part of SOLID refactoring.
 * Handles all search-related operations:
 * - Linear and clustered vector search
 * - K-means clustering management
 * - Hybrid search (BM25 + semantic)
 * - Query caching
 * - Result ranking with RRF
 *
 * @param database AVADatabase instance
 * @param embeddingProvider Embedding provider for query vectors
 * @param clusterCount Number of K-means clusters
 * @param topClusters Number of top clusters to search
 * @param queryCache Optional query cache for performance
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
class ClusteredSearchHandler(
    private val database: AVADatabase,
    private val embeddingProvider: EmbeddingProvider,
    private val clusterCount: Int = 256,
    private val topClusters: Int = 3,
    private val queryCache: QueryCache? = null
) {
    companion object {
        private const val TAG = "ClusteredSearchHandler"
    }

    private val documentQueries by lazy { database.rAGDocumentQueries }
    private val chunkQueries by lazy { database.rAGChunkQueries }
    private val clusterQueries by lazy { database.rAGClusterQueries }

    private val kMeans = KMeansClustering(k = clusterCount)
    private val bm25Scorer = BM25Scorer(k1 = 1.5f, b = 0.75f)
    private val rrfFusion = ReciprocalRankFusion(k = 60)

    // ==================== Search Operations ====================

    /**
     * Perform semantic search with optional clustering.
     *
     * @param query Search query
     * @return SearchResponse with ranked results
     */
    suspend fun search(query: SearchQuery): Result<SearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                // 1. Get or compute query embedding
                var cacheHit = false
                val queryEmbedding = queryCache?.get(query.query)?.let { cachedEmbedding ->
                    cacheHit = true
                    cachedEmbedding
                } ?: run {
                    val embeddingResult = embeddingProvider.embed(query.query)
                    if (embeddingResult.isFailure) {
                        return@withContext Result.failure(
                            embeddingResult.exceptionOrNull()
                                ?: Exception("Failed to generate query embedding")
                        )
                    }
                    val embedding = embeddingResult.getOrThrow() as Embedding.Float32
                    queryCache?.put(query.query, embedding)
                    embedding
                }

                // 2. Choose search strategy
                val clusterCount = clusterQueries.count().executeAsOne()
                val rankedResults = if (clusterCount > 0) {
                    searchWithClusters(queryEmbedding.values, query)
                } else {
                    searchLinear(queryEmbedding.values, query)
                }

                // 3. Convert to domain models
                val searchResults = rankedResults.map { (chunkEntity, similarity, embedding) ->
                    val document = documentQueries.selectById(chunkEntity.document_id).executeAsOneOrNull()
                    SearchResult(
                        chunk = toDomainChunk(chunkEntity),
                        similarity = similarity,
                        document = document?.let { toDomainDocument(it) },
                        highlights = listOf(extractSnippet(chunkEntity.content, query.query))
                    )
                }

                // 4. Update last accessed timestamp
                rankedResults.firstOrNull()?.let { (chunkEntity, _, _) ->
                    documentQueries.updateLastAccessed(
                        Clock.System.now().toString(),
                        chunkEntity.document_id
                    )
                }

                val endTime = System.currentTimeMillis()

                Result.success(
                    SearchResponse(
                        query = query.query,
                        results = searchResults,
                        totalResults = searchResults.size,
                        searchTimeMs = endTime - startTime,
                        cacheHit = cacheHit
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Search failed: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Linear search through all chunks (no clustering).
     */
    private fun searchLinear(
        queryEmbedding: FloatArray,
        query: SearchQuery
    ): List<Triple<Rag_chunk, Float, Embedding.Float32>> {
        val allChunks = chunkQueries.selectAll().executeAsList()

        return allChunks
            .map { chunkEntity ->
                val chunkEmbedding = deserializeEmbedding(chunkEntity.embedding_blob)
                val similarity = cosineSimilarity(queryEmbedding, chunkEmbedding.values)
                Triple(chunkEntity, similarity, chunkEmbedding)
            }
            .filter { (_, similarity, _) -> similarity >= query.minSimilarity }
            .sortedByDescending { (_, similarity, _) -> similarity }
            .take(query.maxResults)
    }

    /**
     * Two-stage clustered search.
     * Stage 1: Find top-k nearest clusters
     * Stage 2: Search only chunks in those clusters
     */
    private fun searchWithClusters(
        queryEmbedding: FloatArray,
        query: SearchQuery
    ): List<Triple<Rag_chunk, Float, Embedding.Float32>> {
        // Stage 1: Find nearest clusters
        val allClusters = clusterQueries.selectAll().executeAsList()
        val clusterCentroids = allClusters.map { cluster ->
            val centroid = deserializeCentroid(cluster.centroid_blob, cluster.embedding_dimension.toInt())
            cluster.id to centroid
        }

        val nearestClusters = clusterCentroids
            .map { (clusterId, centroid) ->
                val distance = kMeans.findNearestCentroid(queryEmbedding, arrayOf(centroid)).second
                clusterId to distance
            }
            .sortedBy { (_, distance) -> distance }
            .take(topClusters)
            .map { (clusterId, _) -> clusterId }

        // Stage 2: Search chunks in top clusters
        val candidateChunks = nearestClusters.flatMap { clusterId ->
            chunkQueries.selectByCluster(clusterId).executeAsList()
        }

        return candidateChunks
            .map { chunkEntity ->
                val chunkEmbedding = deserializeEmbedding(chunkEntity.embedding_blob)
                val similarity = cosineSimilarity(queryEmbedding, chunkEmbedding.values)
                Triple(chunkEntity, similarity, chunkEmbedding)
            }
            .filter { (_, similarity, _) -> similarity >= query.minSimilarity }
            .sortedByDescending { (_, similarity, _) -> similarity }
            .take(query.maxResults)
    }

    // ==================== Clustering Operations ====================

    /**
     * Clustering statistics result.
     */
    data class ClusteringStats(
        val clusterCount: Int,
        val chunkCount: Int,
        val iterations: Int,
        val clusteringTimeMs: Long,
        val averageClusterSize: Float,
        val inertia: Float
    )

    /**
     * Rebuild clusters using K-means.
     *
     * @return ClusteringStats with metrics
     */
    suspend fun rebuildClusters(): Result<ClusteringStats> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()

            val allChunks = chunkQueries.selectAll().executeAsList()
            if (allChunks.isEmpty()) {
                return@withContext Result.failure(Exception("No chunks to cluster"))
            }

            Log.d(TAG, "Rebuilding clusters for ${allChunks.size} chunks...")

            // Extract embeddings
            val embeddings = allChunks.map { chunk ->
                deserializeEmbedding(chunk.embedding_blob).values
            }

            // Run K-means clustering
            val result = kMeans.cluster(embeddings)

            // Delete old clusters
            clusterQueries.deleteAll()

            // Save new clusters
            val now = Clock.System.now().toString()
            result.centroids.forEachIndexed { index, centroid ->
                clusterQueries.insert(
                    id = "cluster_$index",
                    centroid_blob = serializeCentroid(centroid),
                    embedding_dimension = centroid.size.toLong(),
                    chunk_count = result.clusterSizes[index].toLong(),
                    created_timestamp = now,
                    last_updated_timestamp = now
                )
            }

            // Assign chunks to clusters
            for (i in allChunks.indices) {
                val chunk = allChunks[i]
                val clusterId = "cluster_${result.assignments[i]}"
                val centroid = result.centroids[result.assignments[i]]
                val distance = kMeans.findNearestCentroid(embeddings[i], arrayOf(centroid)).second

                chunkQueries.updateClusterAssignment(clusterId, distance.toDouble(), chunk.id)
            }

            val endTime = System.currentTimeMillis()

            Log.i(TAG, "Clustering complete: ${result.centroids.size} clusters, ${result.iterations} iterations")

            Result.success(
                ClusteringStats(
                    clusterCount = result.centroids.size,
                    chunkCount = allChunks.size,
                    iterations = result.iterations,
                    clusteringTimeMs = endTime - startTime,
                    averageClusterSize = allChunks.size.toFloat() / result.centroids.size,
                    inertia = result.inertia
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Clustering failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Find top-k nearest clusters for a query embedding.
     *
     * @param queryEmbedding Query embedding vector
     * @param k Number of clusters to return
     * @return List of cluster IDs
     */
    fun findTopClusters(queryEmbedding: FloatArray, k: Int = topClusters): List<String> {
        val allClusters = clusterQueries.selectAll().executeAsList()

        return allClusters
            .map { cluster ->
                val centroid = deserializeCentroid(cluster.centroid_blob, cluster.embedding_dimension.toInt())
                val distance = kMeans.findNearestCentroid(queryEmbedding, arrayOf(centroid)).second
                cluster.id to distance
            }
            .sortedBy { (_, distance) -> distance }
            .take(k)
            .map { (clusterId, _) -> clusterId }
    }

    // ==================== Utility Functions ====================

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vector dimensions must match" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    private fun deserializeEmbedding(bytes: ByteArray): Embedding.Float32 {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(floats)
        return Embedding.Float32(values = floats)
    }

    private fun deserializeCentroid(bytes: ByteArray, dimension: Int): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        return FloatArray(dimension) { buffer.getFloat() }
    }

    private fun serializeCentroid(centroid: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(centroid.size * 4).order(ByteOrder.BIG_ENDIAN)
        centroid.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun extractSnippet(content: String, query: String, maxLength: Int = 200): String {
        val queryWords = query.lowercase().split(" ").filter { it.isNotBlank() }
        val contentLower = content.lowercase()

        val firstMatch = queryWords.firstNotNullOfOrNull { word ->
            val index = contentLower.indexOf(word)
            if (index >= 0) index else null
        } ?: 0

        val start = maxOf(0, firstMatch - maxLength / 2)
        val end = minOf(content.length, start + maxLength)

        return if (start > 0) "..." + content.substring(start, end) else content.substring(start, end)
    }

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

    private fun toDomainDocument(entity: com.augmentalis.ava.core.data.db.Rag_document): Document {
        val chunkCount = chunkQueries.countByDocument(entity.id).executeAsOne().toInt()
        return Document(
            id = entity.id,
            title = entity.title,
            filePath = entity.file_path,
            fileType = DocumentType.valueOf(entity.document_type),
            sizeBytes = entity.size_bytes,
            createdAt = Instant.parse(entity.added_timestamp),
            modifiedAt = entity.last_accessed_timestamp?.let { Instant.parse(it) }
                ?: Instant.parse(entity.added_timestamp),
            status = if (chunkCount > 0) DocumentStatus.INDEXED else DocumentStatus.PENDING,
            metadata = emptyMap(),
            chunkCount = chunkCount
        )
    }
}
