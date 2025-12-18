/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from SQLiteRAGRepository (SRP)
 */

package com.augmentalis.rag.handlers

import android.content.Context
import android.util.Log
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.Rag_document
import com.augmentalis.rag.domain.*
import com.augmentalis.rag.embeddings.EmbeddingProvider
import com.augmentalis.rag.parser.DocumentParser
import com.augmentalis.rag.parser.PdfParser
import com.augmentalis.rag.parser.TextChunker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * DocumentIngestionHandler - Single Responsibility: Document Ingestion
 *
 * Extracted from SQLiteRAGRepository as part of SOLID refactoring.
 * Handles all document ingestion operations:
 * - Document addition and validation
 * - Document parsing
 * - Chunk creation
 * - Embedding generation orchestration
 *
 * @param context Android context
 * @param database AVADatabase instance
 * @param embeddingProvider Embedding provider for vector generation
 * @param chunkingConfig Configuration for text chunking
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-17
 */
class DocumentIngestionHandler(
    private val context: Context,
    private val database: AVADatabase,
    private val embeddingProvider: EmbeddingProvider,
    private val chunkingConfig: ChunkingConfig = ChunkingConfig()
) {
    companion object {
        private const val TAG = "DocumentIngestionHandler"
    }

    private val documentQueries by lazy { database.rAGDocumentQueries }
    private val chunkQueries by lazy { database.rAGChunkQueries }
    private val textChunker = TextChunker(chunkingConfig)

    // ==================== Document Addition ====================

    /**
     * Add a document to the RAG system.
     *
     * @param request Add document request
     * @return Result with document ID and status
     */
    suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult> =
        withContext(Dispatchers.IO) {
            try {
                // Check if document already exists
                val existingDoc = documentQueries.selectByPath(request.filePath).executeAsOneOrNull()
                if (existingDoc != null) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Document already indexed: ${request.filePath}")
                    )
                }

                // Get file info
                val file = File(request.filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("File not found: ${request.filePath}")
                    )
                }

                // Detect document type
                val extension = file.extension
                val docType = DocumentType.fromExtension(extension)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Unsupported file type: $extension")
                    )

                // Create document
                val documentId = UUID.randomUUID().toString()
                val now = Clock.System.now()

                // Insert document using SQLDelight
                documentQueries.insert(
                    id = documentId,
                    title = request.title ?: file.nameWithoutExtension,
                    file_path = request.filePath,
                    document_type = docType.name,
                    total_pages = 0,
                    size_bytes = file.length(),
                    added_timestamp = now.toString(),
                    last_accessed_timestamp = null,
                    metadata_json = kotlinx.serialization.json.Json.encodeToString(
                        kotlinx.serialization.serializer(),
                        request.metadata
                    ),
                    content_checksum = null
                )

                Log.d(TAG, "Document added: $documentId (${file.name})")

                // Process immediately if requested
                val status = if (request.processImmediately) {
                    processDocument(documentId, docType, request.filePath)
                    DocumentStatus.INDEXED
                } else {
                    DocumentStatus.PENDING
                }

                Result.success(
                    AddDocumentResult(
                        documentId = documentId,
                        status = status,
                        message = if (status == DocumentStatus.INDEXED) {
                            "Document indexed successfully"
                        } else {
                            "Document added, pending processing"
                        }
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add document: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Process a document: parse, chunk, and generate embeddings.
     *
     * @param documentId Document ID
     * @param docType Document type
     * @param filePath File path
     */
    suspend fun processDocument(
        documentId: String,
        docType: DocumentType,
        filePath: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Processing document: $documentId")

        // Get the document entity
        val docEntity = documentQueries.selectById(documentId).executeAsOneOrNull()
            ?: throw Exception("Document not found: $documentId")

        // Get parser
        val parser: DocumentParser = when (docType) {
            DocumentType.PDF -> PdfParser(context)
            else -> throw Exception("Unsupported document type: $docType")
        }

        // Parse document
        val parseResult = parser.parse(filePath, docType)
        if (parseResult.isFailure) {
            throw parseResult.exceptionOrNull() ?: Exception("Failed to parse document")
        }

        val parsedDoc = parseResult.getOrThrow()
        Log.d(TAG, "Parsed document: ${parsedDoc.totalPages} pages")

        // Create temporary Document object for chunker
        val file = File(filePath)
        val now = Clock.System.now()
        val document = Document(
            id = documentId,
            title = docEntity.title,
            filePath = filePath,
            fileType = docType,
            sizeBytes = file.length(),
            createdAt = Instant.parse(docEntity.added_timestamp),
            modifiedAt = now,
            status = DocumentStatus.PROCESSING,
            metadata = emptyMap()
        )

        // Chunk text
        val domainChunks = textChunker.chunk(document, parsedDoc)
        Log.d(TAG, "Created ${domainChunks.size} chunks")

        val texts = domainChunks.map { it.content }

        // Generate embeddings using batch processing
        val embeddingResult = embeddingProvider.embedBatch(texts)
        val embeddings = if (embeddingResult.isFailure) {
            documentQueries.deleteById(documentId)
            throw embeddingResult.exceptionOrNull() ?: Exception("Failed to generate embeddings")
        } else {
            embeddingResult.getOrThrow()
        }

        Log.d(TAG, "Generated ${embeddings.size} embeddings")

        // Insert chunks
        insertChunks(documentId, domainChunks, embeddings, now.toString())
        Log.i(TAG, "Document processed successfully: $documentId")
    }

    /**
     * Add document with pre-computed chunks and embeddings.
     *
     * @param document Document domain object
     * @param chunks Pre-chunked document chunks
     * @return Result of operation
     */
    suspend fun addDocumentBatch(
        document: Document,
        chunks: List<Chunk>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Insert document
            documentQueries.insert(
                id = document.id,
                title = document.title,
                file_path = document.filePath,
                document_type = document.fileType.name,
                total_pages = 0,
                size_bytes = document.sizeBytes,
                added_timestamp = document.createdAt.toString(),
                last_accessed_timestamp = null,
                metadata_json = kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.serializer(),
                    document.metadata
                ),
                content_checksum = null
            )

            // Extract chunk texts
            val chunkTexts = chunks.map { it.content }

            // Generate embeddings in batch
            val embeddingResult = embeddingProvider.embedBatch(chunkTexts)
            val embeddings = if (embeddingResult.isFailure) {
                documentQueries.deleteById(document.id)
                throw embeddingResult.exceptionOrNull() ?: Exception("Failed to generate embeddings")
            } else {
                embeddingResult.getOrThrow()
            }

            // Insert chunks
            insertChunks(document.id, chunks, embeddings, Clock.System.now().toString())

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add document batch: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== Document Retrieval ====================

    /**
     * Get a document by ID.
     *
     * @param documentId Document ID
     * @return Document or null if not found
     */
    suspend fun getDocument(documentId: String): Result<Document?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = documentQueries.selectById(documentId).executeAsOneOrNull()
                    ?: return@withContext Result.success(null)
                val chunkCount = chunkQueries.countByDocument(documentId).executeAsOne().toInt()
                Result.success(toDomainDocument(entity, chunkCount))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Delete a document and all its chunks.
     *
     * @param documentId Document ID
     * @return Result of operation
     */
    suspend fun deleteDocument(documentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                chunkQueries.deleteByDocument(documentId)
                documentQueries.deleteById(documentId)
                Log.d(TAG, "Deleted document: $documentId")
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== Helper Methods ====================

    private fun insertChunks(
        documentId: String,
        chunks: List<Chunk>,
        embeddings: List<Embedding>,
        timestamp: String
    ) {
        chunks.forEachIndexed { index, chunk ->
            val embedding = embeddings[index]
            chunkQueries.insert(
                id = chunk.id,
                document_id = documentId,
                chunk_index = chunk.chunkIndex.toLong(),
                content = chunk.content,
                token_count = chunk.metadata.tokens.toLong(),
                start_offset = chunk.startOffset.toLong(),
                end_offset = chunk.endOffset.toLong(),
                page_number = chunk.metadata.pageNumber?.toLong(),
                section_title = chunk.metadata.section,
                embedding_blob = serializeEmbedding(embedding),
                embedding_type = "float32",
                embedding_dimension = getEmbeddingDimension(embedding).toLong(),
                quant_scale = null,
                quant_offset = null,
                cluster_id = null,
                distance_to_centroid = null,
                created_timestamp = timestamp,
                is_encrypted = false,
                encryption_key_version = null
            )
        }
    }

    private fun getEmbeddingDimension(embedding: Embedding): Int {
        return when (embedding) {
            is Embedding.Float32 -> embedding.values.size
            is Embedding.Int8 -> embedding.values.size
        }
    }

    private fun serializeEmbedding(embedding: Embedding): ByteArray {
        val floatValues = when (embedding) {
            is Embedding.Float32 -> embedding.values
            is Embedding.Int8 -> embedding.toFloat32()
        }
        val buffer = ByteBuffer.allocate(floatValues.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(floatValues)
        return buffer.array()
    }

    private fun toDomainDocument(entity: Rag_document, chunkCount: Int): Document {
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
            metadata = try {
                kotlinx.serialization.json.Json.decodeFromString(
                    entity.metadata_json ?: "{}"
                )
            } catch (e: Exception) {
                emptyMap()
            },
            chunkCount = chunkCount
        )
    }
}
