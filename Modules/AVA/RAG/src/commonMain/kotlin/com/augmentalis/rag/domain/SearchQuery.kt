// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/domain/SearchQuery.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.domain

import kotlinx.serialization.Serializable

/**
 * Query for semantic search in the RAG system
 */
@Serializable
data class SearchQuery(
    val query: String,
    val maxResults: Int = 10,
    val minSimilarity: Float = 0.5f,
    val filters: SearchFilters = SearchFilters(),
    val includeContent: Boolean = true
)

/**
 * Filters to narrow down search results
 *
 * Phase 3.0: Extended with author, tags, and preset support
 * Phase 3.1: Extended with file size filters for metadata filtering
 */
@Serializable
data class SearchFilters(
    val documentIds: List<String>? = null,
    val documentTypes: List<DocumentType>? = null,
    val dateRange: DateRange? = null,
    val metadata: Map<String, String>? = null,
    val semanticTypes: List<SemanticType>? = null,

    // Phase 3.0: Advanced filters
    val authors: List<String>? = null,
    val tags: List<String>? = null,
    val bookmarkedOnly: Boolean = false,
    val annotatedOnly: Boolean = false,

    // Phase 3.1: Metadata filters (file size)
    val minFileSize: Long? = null,  // Minimum file size in bytes
    val maxFileSize: Long? = null   // Maximum file size in bytes
)

/**
 * Date range filter
 */
@Serializable
data class DateRange(
    val start: String? = null,  // ISO 8601
    val end: String? = null     // ISO 8601
) {
    /**
     * Check if a timestamp falls within this date range
     *
     * @param timestampMs Timestamp in milliseconds since epoch
     * @return True if timestamp is within range
     */
    fun contains(timestampMs: Long): Boolean {
        if (start != null) {
            val startInstant = kotlinx.datetime.Instant.parse(start)
            val timestamp = kotlinx.datetime.Instant.fromEpochMilliseconds(timestampMs)
            if (timestamp < startInstant) return false
        }

        if (end != null) {
            val endInstant = kotlinx.datetime.Instant.parse(end)
            val timestamp = kotlinx.datetime.Instant.fromEpochMilliseconds(timestampMs)
            if (timestamp > endInstant) return false
        }

        return true
    }
}

/**
 * Result of a semantic search
 */
@Serializable
data class SearchResult(
    val chunk: Chunk,
    val similarity: Float,
    val document: Document? = null,
    val highlights: List<String> = emptyList()
)

/**
 * Complete search response
 */
@Serializable
data class SearchResponse(
    val query: String,
    val results: List<SearchResult>,
    val totalResults: Int,
    val searchTimeMs: Long,
    val cacheHit: Boolean = false
)

/**
 * Search performance metrics
 */
data class SearchMetrics(
    val queryEmbeddingTimeMs: Long,
    val vectorSearchTimeMs: Long,
    val documentFetchTimeMs: Long,
    val totalTimeMs: Long,
    val candidatesEvaluated: Int,
    val cacheHit: Boolean
)

/**
 * Filter preset for quick access to common filter combinations
 *
 * Phase 3.0: Allows users to save and reuse filter configurations
 */
@Serializable
data class FilterPreset(
    val id: String,
    val name: String,
    val description: String? = null,
    val filters: SearchFilters,
    val createdAt: String, // ISO 8601
    val isPinned: Boolean = false,
    val useCount: Int = 0
)
