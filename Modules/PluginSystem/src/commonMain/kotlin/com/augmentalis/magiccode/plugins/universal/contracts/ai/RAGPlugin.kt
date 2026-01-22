/**
 * RAGPlugin.kt - Retrieval-Augmented Generation plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for RAG plugins that combine document retrieval
 * with LLM generation for context-aware responses. Essential for
 * knowledge-grounded accessibility assistance.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.ai

import com.augmentalis.magiccode.plugins.universal.PluginCapability
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import kotlinx.serialization.Serializable

/**
 * Plugin interface for Retrieval-Augmented Generation.
 *
 * RAGPlugin extends [UniversalPlugin] to provide document indexing,
 * retrieval, and context-aware generation capabilities. RAG enables
 * LLMs to answer questions using specific knowledge bases:
 *
 * - **App Documentation**: Help users with app-specific features
 * - **Command History**: Suggest commands based on past usage
 * - **User Preferences**: Personalize responses based on user data
 * - **Accessibility Guides**: Provide contextual accessibility help
 *
 * ## Capabilities
 * Implementations should advertise:
 * - [PluginCapability.RAG_DOCUMENT] - Document processing
 * - [PluginCapability.RAG_RETRIEVAL] - Vector store/retrieval
 * - [PluginCapability.RAG_CONTEXT] - Context injection
 *
 * ## Architecture
 * ```
 * Documents -> Chunking -> Embedding -> Vector Store
 *                                           |
 * Query -> Embedding -> Similarity Search --+
 *                                           |
 *                          Retrieved Chunks -> LLM -> Response
 * ```
 *
 * ## Implementation Example
 * ```kotlin
 * class LocalRAGPlugin(
 *     private val embeddingPlugin: EmbeddingPlugin,
 *     private val llmPlugin: LLMPlugin
 * ) : RAGPlugin {
 *     override val pluginId = "com.augmentalis.rag.local"
 *     override val vectorStoreId = "local_faiss"
 *     override val chunkSize = 512
 *     override val chunkOverlap = 50
 *
 *     override suspend fun retrieve(query: String, topK: Int): List<RetrievedChunk> {
 *         val queryEmbedding = embeddingPlugin.embed(query)
 *         return vectorStore.search(queryEmbedding, topK)
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see UniversalPlugin
 * @see EmbeddingPlugin
 * @see LLMPlugin
 */
interface RAGPlugin : UniversalPlugin {

    // =========================================================================
    // Configuration Properties
    // =========================================================================

    /**
     * Identifier for the vector store used by this plugin.
     *
     * Common vector stores:
     * - "faiss" - FAISS local vector store
     * - "hnswlib" - HNSW-based store
     * - "chroma" - Chroma DB
     * - "pinecone" - Pinecone cloud store
     * - "weaviate" - Weaviate DB
     * - "memory" - In-memory store (for testing)
     */
    val vectorStoreId: String

    /**
     * Size of document chunks in characters.
     *
     * Smaller chunks:
     * - More precise retrieval
     * - More context entries fit in LLM window
     * - May lose context continuity
     *
     * Larger chunks:
     * - Better context continuity
     * - Fewer retrievals needed
     * - May include irrelevant content
     *
     * Typical values: 256-1024 characters
     */
    val chunkSize: Int

    /**
     * Overlap between adjacent chunks in characters.
     *
     * Overlap helps maintain context across chunk boundaries.
     * Typical values: 10-20% of [chunkSize]
     */
    val chunkOverlap: Int

    // =========================================================================
    // Indexing Methods
    // =========================================================================

    /**
     * Index documents into the vector store.
     *
     * Processes documents through the RAG pipeline:
     * 1. Chunk documents according to [chunkSize] and [chunkOverlap]
     * 2. Generate embeddings for each chunk
     * 3. Store chunks and embeddings in vector store
     *
     * ## Document Metadata
     * Document metadata is preserved and returned with retrieved chunks.
     * Use metadata for:
     * - Source tracking (file path, URL)
     * - Filtering during retrieval
     * - Display in citations
     *
     * @param documents List of documents to index
     * @return IndexResult with statistics and any errors
     * @see Document
     * @see IndexResult
     */
    suspend fun index(documents: List<Document>): IndexResult

    // =========================================================================
    // Retrieval Methods
    // =========================================================================

    /**
     * Retrieve relevant chunks for a query.
     *
     * Performs semantic search to find document chunks most similar
     * to the query. Returns chunks sorted by relevance score.
     *
     * ## Usage
     * ```kotlin
     * val chunks = ragPlugin.retrieve("How do I enable dark mode?", topK = 5)
     * for (chunk in chunks) {
     *     println("Score: ${chunk.score}, Source: ${chunk.metadata["source"]}")
     *     println(chunk.content)
     * }
     * ```
     *
     * @param query The search query
     * @param topK Maximum number of chunks to return (default: 5)
     * @return List of retrieved chunks sorted by relevance
     * @see RetrievedChunk
     */
    suspend fun retrieve(query: String, topK: Int = 5): List<RetrievedChunk>

    /**
     * Generate a response using retrieved context.
     *
     * Combines retrieval and generation:
     * 1. Retrieve relevant chunks for the query
     * 2. Construct a prompt with retrieved context
     * 3. Generate response using the underlying LLM
     *
     * This is a convenience method that handles the full RAG pipeline.
     *
     * ## Context Injection
     * The prompt is constructed as:
     * ```
     * System: Use the following context to answer the question.
     * Context: [retrieved chunks]
     * Question: [query]
     * ```
     *
     * @param query The user's question
     * @param topK Number of context chunks to retrieve (default: 5)
     * @return GenerationResponse from the LLM with context
     * @see retrieve
     * @see GenerationResponse
     */
    suspend fun generateWithContext(query: String, topK: Int = 5): GenerationResponse

    // =========================================================================
    // Management Methods
    // =========================================================================

    /**
     * Clear all indexed documents from the vector store.
     *
     * This permanently removes all indexed data. Use with caution.
     *
     * @return Result indicating success or failure
     */
    fun clearIndex(): Result<Unit>
}

// =============================================================================
// Document Data Classes
// =============================================================================

/**
 * Document to be indexed for RAG retrieval.
 *
 * Represents a unit of content to be chunked, embedded, and stored
 * in the vector store.
 *
 * ## Usage
 * ```kotlin
 * val doc = Document(
 *     id = "help_dark_mode",
 *     content = "To enable dark mode, go to Settings > Display > Theme...",
 *     metadata = mapOf(
 *         "source" to "user_guide.md",
 *         "section" to "display_settings",
 *         "lastUpdated" to "2026-01-22"
 *     )
 * )
 * ```
 *
 * @property id Unique document identifier
 * @property content Document text content
 * @property metadata Additional metadata (source, author, date, etc.)
 *
 * @since 1.0.0
 * @see RAGPlugin.index
 */
@Serializable
data class Document(
    val id: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get the document length in characters.
     */
    val length: Int get() = content.length

    /**
     * Check if the document is empty.
     */
    fun isEmpty(): Boolean = content.isBlank()

    /**
     * Get metadata value by key.
     */
    fun getMetadata(key: String): String? = metadata[key]

    /**
     * Create a copy with additional metadata.
     */
    fun withMetadata(key: String, value: String): Document =
        copy(metadata = metadata + (key to value))

    companion object {
        /**
         * Create a document from text with auto-generated ID.
         */
        fun fromText(content: String, source: String? = null): Document {
            val id = "doc_${content.hashCode()}"
            val meta = source?.let { mapOf("source" to it) } ?: emptyMap()
            return Document(id = id, content = content, metadata = meta)
        }

        /**
         * Create multiple documents from a list of texts.
         */
        fun fromTexts(texts: List<String>, sourcePrefix: String = "doc"): List<Document> =
            texts.mapIndexed { index, text ->
                Document(
                    id = "${sourcePrefix}_$index",
                    content = text,
                    metadata = mapOf("index" to index.toString())
                )
            }
    }
}

/**
 * Retrieved chunk from the vector store.
 *
 * Contains the chunk content, its source document, relevance score,
 * and any associated metadata.
 *
 * ## Scoring
 * Scores are typically cosine similarity (0.0-1.0) where higher is more relevant.
 * Scores above 0.7 usually indicate good relevance.
 *
 * @property documentId ID of the source document
 * @property content Chunk text content
 * @property score Relevance score (0.0-1.0, higher is better)
 * @property metadata Chunk/document metadata
 *
 * @since 1.0.0
 * @see RAGPlugin.retrieve
 */
@Serializable
data class RetrievedChunk(
    val documentId: String,
    val content: String,
    val score: Float,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if this chunk is highly relevant (score > 0.7).
     */
    fun isHighlyRelevant(): Boolean = score > 0.7f

    /**
     * Check if this chunk is moderately relevant (score > 0.5).
     */
    fun isRelevant(): Boolean = score > 0.5f

    /**
     * Get the source from metadata if available.
     */
    val source: String? get() = metadata["source"]

    /**
     * Get chunk length in characters.
     */
    val length: Int get() = content.length

    companion object {
        /**
         * Create a chunk with minimal info.
         */
        fun simple(documentId: String, content: String, score: Float): RetrievedChunk =
            RetrievedChunk(
                documentId = documentId,
                content = content,
                score = score
            )
    }
}

/**
 * Result of indexing documents.
 *
 * Contains statistics about the indexing operation and any errors
 * that occurred during processing.
 *
 * @property documentsIndexed Number of documents successfully indexed
 * @property chunksCreated Total number of chunks created
 * @property errors List of error messages for failed documents
 *
 * @since 1.0.0
 * @see RAGPlugin.index
 */
@Serializable
data class IndexResult(
    val documentsIndexed: Int,
    val chunksCreated: Int,
    val errors: List<String> = emptyList()
) {
    /**
     * Check if indexing completed without errors.
     */
    fun isSuccess(): Boolean = errors.isEmpty()

    /**
     * Check if some documents failed to index.
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Get average chunks per document.
     */
    val averageChunksPerDocument: Float
        get() = if (documentsIndexed > 0) {
            chunksCreated.toFloat() / documentsIndexed
        } else 0f

    companion object {
        /**
         * Create a success result.
         */
        fun success(documentsIndexed: Int, chunksCreated: Int): IndexResult =
            IndexResult(
                documentsIndexed = documentsIndexed,
                chunksCreated = chunksCreated
            )

        /**
         * Create an empty result (no documents processed).
         */
        val EMPTY = IndexResult(documentsIndexed = 0, chunksCreated = 0)

        /**
         * Create a failed result.
         */
        fun failure(error: String): IndexResult =
            IndexResult(
                documentsIndexed = 0,
                chunksCreated = 0,
                errors = listOf(error)
            )
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Index a single document.
 *
 * @param document The document to index
 * @return IndexResult for the single document
 */
suspend fun RAGPlugin.indexDocument(document: Document): IndexResult =
    index(listOf(document))

/**
 * Index text content directly.
 *
 * @param content Text content to index
 * @param id Optional document ID
 * @param metadata Optional metadata
 * @return IndexResult
 */
suspend fun RAGPlugin.indexText(
    content: String,
    id: String? = null,
    metadata: Map<String, String> = emptyMap()
): IndexResult {
    val docId = id ?: "doc_${content.hashCode()}"
    return index(listOf(Document(id = docId, content = content, metadata = metadata)))
}

/**
 * Retrieve and format chunks as context string.
 *
 * @param query The search query
 * @param topK Number of chunks to retrieve
 * @param separator Separator between chunks
 * @return Formatted context string
 */
suspend fun RAGPlugin.retrieveAsContext(
    query: String,
    topK: Int = 5,
    separator: String = "\n\n---\n\n"
): String {
    val chunks = retrieve(query, topK)
    return chunks.joinToString(separator) { chunk ->
        val source = chunk.source?.let { "[$it] " } ?: ""
        "$source${chunk.content}"
    }
}

/**
 * Check if any documents are indexed.
 *
 * Performs a test query to check if the index has content.
 *
 * @return true if the index contains documents
 */
suspend fun RAGPlugin.hasIndexedDocuments(): Boolean {
    return try {
        retrieve("test", topK = 1).isNotEmpty()
    } catch (e: Exception) {
        false
    }
}

/**
 * Retrieve with score threshold.
 *
 * @param query The search query
 * @param minScore Minimum relevance score (0.0-1.0)
 * @param maxResults Maximum number of results
 * @return Chunks with score >= minScore
 */
suspend fun RAGPlugin.retrieveWithThreshold(
    query: String,
    minScore: Float = 0.5f,
    maxResults: Int = 10
): List<RetrievedChunk> {
    return retrieve(query, maxResults).filter { it.score >= minScore }
}

// =============================================================================
// RAG Configuration
// =============================================================================

/**
 * Configuration for RAG behavior.
 *
 * Can be used to customize RAG plugin behavior at runtime.
 *
 * @property chunkSize Size of document chunks
 * @property chunkOverlap Overlap between chunks
 * @property topK Default number of chunks to retrieve
 * @property minScore Minimum relevance score for filtering
 * @property maxContextLength Maximum context length for LLM
 *
 * @since 1.0.0
 */
@Serializable
data class RAGConfig(
    val chunkSize: Int = 512,
    val chunkOverlap: Int = 50,
    val topK: Int = 5,
    val minScore: Float = 0.5f,
    val maxContextLength: Int = 4000
) {
    /**
     * Validate configuration.
     */
    fun validate(): List<String> = buildList {
        if (chunkSize <= 0) add("chunkSize must be positive")
        if (chunkOverlap < 0) add("chunkOverlap cannot be negative")
        if (chunkOverlap >= chunkSize) add("chunkOverlap must be less than chunkSize")
        if (topK <= 0) add("topK must be positive")
        if (minScore < 0f || minScore > 1f) add("minScore must be between 0 and 1")
        if (maxContextLength <= 0) add("maxContextLength must be positive")
    }

    companion object {
        /**
         * Default configuration suitable for most use cases.
         */
        val DEFAULT = RAGConfig()

        /**
         * Configuration optimized for precision (smaller chunks, stricter filtering).
         */
        val PRECISE = RAGConfig(
            chunkSize = 256,
            chunkOverlap = 25,
            topK = 3,
            minScore = 0.7f
        )

        /**
         * Configuration optimized for recall (larger chunks, more results).
         */
        val COMPREHENSIVE = RAGConfig(
            chunkSize = 1024,
            chunkOverlap = 100,
            topK = 10,
            minScore = 0.3f,
            maxContextLength = 8000
        )
    }
}
