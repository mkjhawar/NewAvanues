// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/data/InMemoryRAGRepository.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.data

import com.augmentalis.ava.features.rag.domain.*
import com.augmentalis.ava.features.rag.embeddings.EmbeddingProvider
import com.augmentalis.ava.features.rag.parser.DocumentParser
import com.augmentalis.ava.features.rag.parser.DocumentParserFactory
import com.augmentalis.ava.features.rag.parser.TextChunker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlin.math.sqrt

/**
 * In-memory implementation of RAGRepository
 *
 * Used for testing and development. Stores all data in memory.
 * Phase 3 will add persistent storage with SQLite-vec.
 */
class InMemoryRAGRepository(
    private val embeddingProvider: EmbeddingProvider,
    private val chunkingConfig: ChunkingConfig = ChunkingConfig()
) : RAGRepository {

    private val documents = mutableMapOf<String, Document>()
    private val chunks = mutableMapOf<String, MutableList<EmbeddedChunk>>()
    private val textChunker = TextChunker(chunkingConfig)

    override suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult> {
        return try {
            // Generate document ID
            val documentId = generateDocumentId(request.filePath)

            // Detect document type from file extension
            val extension = request.filePath.substringAfterLast('.', "")
            val documentType = DocumentType.fromExtension(extension)
                ?: return Result.failure(IllegalArgumentException("Unsupported file type: $extension"))

            // Create document entity
            val now = Clock.System.now()
            val document = Document(
                id = documentId,
                title = request.title ?: request.filePath.substringAfterLast('/'),
                filePath = request.filePath,
                fileType = documentType,
                sizeBytes = 0,  // Note: File size calculation requires platform-specific code
                createdAt = now,
                modifiedAt = now,
                status = DocumentStatus.PENDING,
                metadata = request.metadata
            )

            // Store document
            documents[documentId] = document

            // Process immediately if requested
            if (request.processImmediately) {
                processDocuments(documentId)
            }

            Result.success(
                AddDocumentResult(
                    documentId = documentId,
                    status = document.status,
                    message = "Document added successfully"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDocument(documentId: String): Result<Document?> {
        return Result.success(documents[documentId])
    }

    override fun listDocuments(status: DocumentStatus?): Flow<Document> = flow {
        val filteredDocs = if (status != null) {
            documents.values.filter { it.status == status }
        } else {
            documents.values
        }

        filteredDocs.forEach { emit(it) }
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            documents.remove(documentId)
            chunks.remove(documentId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processDocuments(documentId: String?): Result<Int> {
        return try {
            val docsToProcess = if (documentId != null) {
                listOfNotNull(documents[documentId])
            } else {
                documents.values.filter { it.status == DocumentStatus.PENDING }
            }

            var processedCount = 0

            for (doc in docsToProcess) {
                // Update status to PROCESSING
                documents[doc.id] = doc.copy(status = DocumentStatus.PROCESSING)

                try {
                    // Parse document
                    val parser = DocumentParserFactory.getParser(doc.fileType)
                        ?: throw IllegalStateException("No parser available for ${doc.fileType}")

                    val parseResult = parser.parse(doc.filePath, doc.fileType)
                    if (parseResult.isFailure) {
                        throw parseResult.exceptionOrNull() ?: Exception("Parse failed")
                    }

                    val parsedDoc = parseResult.getOrThrow()

                    // Chunk document
                    val docChunks = textChunker.chunk(doc, parsedDoc)

                    // Generate embeddings
                    val chunkTexts = docChunks.map { it.content }
                    val embeddingResult = embeddingProvider.embedBatch(chunkTexts)

                    if (embeddingResult.isFailure) {
                        throw embeddingResult.exceptionOrNull() ?: Exception("Embedding failed")
                    }

                    val embeddings = embeddingResult.getOrThrow()

                    // Store chunks with embeddings
                    val embeddedChunks = docChunks.zip(embeddings).map { (chunk, embedding) ->
                        EmbeddedChunk(chunk, embedding)
                    }

                    chunks[doc.id] = embeddedChunks.toMutableList()

                    // Update document status
                    documents[doc.id] = doc.copy(
                        status = DocumentStatus.INDEXED,
                        indexedAt = Clock.System.now(),
                        chunkCount = embeddedChunks.size
                    )

                    processedCount++
                } catch (e: Exception) {
                    // Mark as failed
                    documents[doc.id] = doc.copy(status = DocumentStatus.FAILED)
                    throw e
                }
            }

            Result.success(processedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun search(query: SearchQuery): Result<SearchResponse> {
        return try {
            val startTime = Clock.System.now().toEpochMilliseconds()

            // Generate query embedding
            val queryEmbeddingResult = embeddingProvider.embed(query.query)
            if (queryEmbeddingResult.isFailure) {
                return Result.failure(
                    queryEmbeddingResult.exceptionOrNull() ?: Exception("Query embedding failed")
                )
            }

            val queryEmbedding = queryEmbeddingResult.getOrThrow()

            // Collect all chunks to search
            val allChunks = mutableListOf<Pair<EmbeddedChunk, Document>>()

            for ((docId, docChunks) in chunks) {
                val doc = documents[docId] ?: continue

                // Apply filters
                if (!matchesFilters(doc, query.filters)) continue

                docChunks.forEach { embeddedChunk ->
                    allChunks.add(embeddedChunk to doc)
                }
            }

            // Calculate similarities
            val results = allChunks.map { (embeddedChunk, doc) ->
                val similarity = cosineSimilarity(queryEmbedding, embeddedChunk.embedding)

                SearchResult(
                    chunk = embeddedChunk.chunk,
                    similarity = similarity,
                    document = if (query.includeContent) doc else null
                )
            }
                .filter { it.similarity >= query.minSimilarity }
                .sortedByDescending { it.similarity }
                .take(query.maxResults)

            val endTime = Clock.System.now().toEpochMilliseconds()

            Result.success(
                SearchResponse(
                    query = query.query,
                    results = results,
                    totalResults = results.size,
                    searchTimeMs = endTime - startTime
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChunks(documentId: String): Result<List<Chunk>> {
        return try {
            val embeddedChunks = chunks[documentId] ?: emptyList()
            Result.success(embeddedChunks.map { it.chunk })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatistics(): Result<RAGStatistics> {
        return try {
            val stats = RAGStatistics(
                totalDocuments = documents.size,
                indexedDocuments = documents.values.count { it.status == DocumentStatus.INDEXED },
                pendingDocuments = documents.values.count { it.status == DocumentStatus.PENDING },
                failedDocuments = documents.values.count { it.status == DocumentStatus.FAILED },
                totalChunks = chunks.values.sumOf { it.size },
                storageUsedBytes = estimateStorageUsage(),
                lastIndexedAt = documents.values
                    .mapNotNull { it.indexedAt }
                    .maxOfOrNull { it.toString() }
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return try {
            documents.clear()
            chunks.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper functions

    private fun generateDocumentId(filePath: String): String {
        return "doc_${filePath.hashCode().toString(16)}_${Clock.System.now().toEpochMilliseconds()}"
    }

    private fun matchesFilters(document: Document, filters: SearchFilters): Boolean {
        // Document ID filter
        if (filters.documentIds != null && document.id !in filters.documentIds) {
            return false
        }

        // Document type filter
        if (filters.documentTypes != null && document.fileType !in filters.documentTypes) {
            return false
        }

        // Date range filter
        if (filters.dateRange != null) {
            val timestampMs = document.createdAt.toEpochMilliseconds()
            if (!filters.dateRange.contains(timestampMs)) {
                return false
            }
        }

        // Metadata filter - all filter entries must match document metadata
        if (filters.metadata != null) {
            for ((key, value) in filters.metadata) {
                if (document.metadata[key] != value) {
                    return false
                }
            }
        }

        return true
    }

    private fun cosineSimilarity(a: Embedding, b: Embedding): Float {
        val aVec = when (a) {
            is Embedding.Float32 -> a.values
            is Embedding.Int8 -> a.toFloat32()
        }

        val bVec = when (b) {
            is Embedding.Float32 -> b.values
            is Embedding.Int8 -> b.toFloat32()
        }

        if (aVec.size != bVec.size) {
            throw IllegalArgumentException("Embedding dimensions don't match: ${aVec.size} vs ${bVec.size}")
        }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in aVec.indices) {
            dotProduct += aVec[i] * bVec[i]
            normA += aVec[i] * aVec[i]
            normB += bVec[i] * bVec[i]
        }

        if (normA == 0f || normB == 0f) return 0f

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    private fun estimateStorageUsage(): Long {
        var totalBytes = 0L

        // Documents
        totalBytes += documents.size * 1024L  // Rough estimate

        // Chunks (text + embeddings)
        for (docChunks in chunks.values) {
            for (embeddedChunk in docChunks) {
                // Text content
                totalBytes += embeddedChunk.chunk.content.length * 2L  // UTF-16

                // Embedding
                totalBytes += when (embeddedChunk.embedding) {
                    is Embedding.Float32 -> embeddedChunk.embedding.dimension * 4L  // 4 bytes per float
                    is Embedding.Int8 -> embeddedChunk.embedding.dimension + 8L  // 1 byte per int8 + metadata
                }
            }
        }

        return totalBytes
    }
}
