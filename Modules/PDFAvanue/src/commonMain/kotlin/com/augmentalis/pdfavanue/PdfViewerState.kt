package com.augmentalis.pdfavanue

import com.augmentalis.pdfavanue.model.PdfDocument
import com.augmentalis.pdfavanue.model.PdfSearchResult
import kotlinx.serialization.Serializable

@Serializable
data class PdfViewerState(
    val document: PdfDocument? = null,
    val currentPage: Int = 0,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<PdfSearchResult> = emptyList(),
    val activeSearchIndex: Int = -1,
    val viewMode: PdfViewMode = PdfViewMode.SINGLE_PAGE,
    val isNightMode: Boolean = false
) {
    val hasDocument: Boolean get() = document != null
    val pageCount: Int get() = document?.pageCount ?: 0
    val canGoNext: Boolean get() = currentPage < pageCount - 1
    val canGoPrevious: Boolean get() = currentPage > 0
}

enum class PdfViewMode { SINGLE_PAGE, CONTINUOUS_SCROLL, TWO_PAGE_SPREAD }
