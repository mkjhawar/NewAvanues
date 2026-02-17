package com.augmentalis.pdfavanue

import com.augmentalis.pdfavanue.model.PdfDocument
import com.augmentalis.pdfavanue.model.PdfPage
import com.augmentalis.pdfavanue.model.PdfSearchResult

/**
 * Platform-agnostic PDF rendering engine.
 * Android: PdfRenderer, iOS: PDFKit, Desktop: PDFBox.
 */
interface IPdfEngine {
    suspend fun openDocument(uri: String, password: String? = null): PdfDocument
    suspend fun closeCurrent()
    fun getPage(index: Int): PdfPage?
    suspend fun renderPage(index: Int, width: Int, height: Int): ByteArray
    suspend fun search(query: String): List<PdfSearchResult>
    suspend fun extractText(pageIndex: Int): String
    fun isOpen(): Boolean
}
