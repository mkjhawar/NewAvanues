// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/domain/SearchFiltersTest.kt
// created: 2025-11-22
// author: AVA AI Team - Agent 3
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class SearchFiltersTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `empty search filters work correctly`() {
        val filters = SearchFilters()

        assertEquals(null, filters.documentTypes)
        assertEquals(null, filters.authors)
        assertEquals(null, filters.tags)
        assertFalse(filters.bookmarkedOnly)
        assertFalse(filters.annotatedOnly)
    }

    @Test
    fun `search filters with all options serialize correctly`() {
        val filters = SearchFilters(
            documentTypes = listOf(DocumentType.PDF, DocumentType.DOCX),
            authors = listOf("John Doe", "Jane Smith"),
            tags = listOf("important", "research"),
            bookmarkedOnly = true,
            annotatedOnly = true,
            dateRange = DateRange(
                start = "2024-01-01",
                end = "2024-12-31"
            )
        )

        val serialized = json.encodeToString(filters)
        val deserialized = json.decodeFromString<SearchFilters>(serialized)

        assertEquals(filters, deserialized)
    }

    @Test
    fun `filter preset serialization works`() {
        val preset = FilterPreset(
            id = "preset-1",
            name = "My Research Filters",
            description = "Filters for AI research papers",
            filters = SearchFilters(
                documentTypes = listOf(DocumentType.PDF),
                tags = listOf("AI", "ML")
            ),
            createdAt = "2024-01-01T00:00:00Z",
            isPinned = true,
            useCount = 10
        )

        val serialized = json.encodeToString(preset)
        val deserialized = json.decodeFromString<FilterPreset>(serialized)

        assertEquals(preset, deserialized)
    }

    @Test
    fun `date range validation works`() {
        val range = DateRange(
            start = "2024-01-01",
            end = "2024-12-31"
        )

        assertEquals("2024-01-01", range.start)
        assertEquals("2024-12-31", range.end)
    }

    @Test
    fun `bookmarked only filter works`() {
        val filters = SearchFilters(bookmarkedOnly = true)
        assertTrue(filters.bookmarkedOnly)
        assertFalse(filters.annotatedOnly)
    }

    @Test
    fun `annotated only filter works`() {
        val filters = SearchFilters(annotatedOnly = true)
        assertTrue(filters.annotatedOnly)
        assertFalse(filters.bookmarkedOnly)
    }

    @Test
    fun `multiple document types filter works`() {
        val filters = SearchFilters(
            documentTypes = listOf(
                DocumentType.PDF,
                DocumentType.DOCX,
                DocumentType.TXT
            )
        )

        assertEquals(3, filters.documentTypes?.size)
        assertTrue(filters.documentTypes?.contains(DocumentType.PDF) == true)
    }

    @Test
    fun `filter preset tracks usage`() {
        val preset = FilterPreset(
            id = "preset-2",
            name = "Quick Filter",
            filters = SearchFilters(),
            createdAt = "2024-01-01T00:00:00Z",
            useCount = 5
        )

        val updated = preset.copy(useCount = preset.useCount + 1)
        assertEquals(6, updated.useCount)
    }
}
