package com.augmentalis.pdfavanue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.augmentalis.pdfavanue.model.PdfDocument
import com.augmentalis.pdfavanue.model.PdfPage
import com.augmentalis.pdfavanue.model.PdfSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Android PDF engine using built-in PdfRenderer API.
 * Thread-safe via mutex — PdfRenderer only allows one page open at a time.
 *
 * Text extraction uses Android 15 (API 35) PdfRenderer text content API when available.
 * On older API levels, text extraction is unavailable and search returns empty results.
 */
class AndroidPdfEngine(private val context: Context) : IPdfEngine {

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentDocument: PdfDocument? = null
    private val renderMutex = Mutex()

    /** Cache of extracted text per page index. Cleared when a new document is opened. */
    private val textCache = mutableMapOf<Int, String>()

    override suspend fun openDocument(uri: String, password: String?): PdfDocument =
        withContext(Dispatchers.IO) {
            closeCurrent()
            val pfd = when {
                uri.startsWith("content://") ->
                    context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
                        ?: throw IllegalStateException("Cannot open content URI: $uri")
                uri.startsWith("/") || uri.startsWith("file://") ->
                    ParcelFileDescriptor.open(
                        File(uri.removePrefix("file://")),
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                else -> throw IllegalArgumentException("Unsupported URI scheme: $uri")
            }
            val pdfRenderer = PdfRenderer(pfd)
            fileDescriptor = pfd
            renderer = pdfRenderer
            textCache.clear()
            val doc = PdfDocument(uri = uri, pageCount = pdfRenderer.pageCount)
            currentDocument = doc
            doc
        }

    override suspend fun closeCurrent() = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            renderer?.close(); renderer = null
            fileDescriptor?.close(); fileDescriptor = null
            currentDocument = null
            textCache.clear()
        }
    }

    override fun getPage(index: Int): PdfPage? {
        val r = renderer ?: return null
        if (index < 0 || index >= r.pageCount) return null
        val page = r.openPage(index)
        val pdfPage = PdfPage(index, page.width.toFloat(), page.height.toFloat())
        page.close()
        return pdfPage
    }

    override suspend fun renderPage(index: Int, width: Int, height: Int): ByteArray =
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                val r = renderer ?: throw IllegalStateException("No document open")
                val page = r.openPage(index)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                bitmap.recycle()
                stream.toByteArray()
            }
        }

    /**
     * Render a page to a Bitmap directly instead of ByteArray.
     * Used by continuous scroll mode to avoid double encode/decode overhead.
     */
    suspend fun renderPageBitmap(index: Int, width: Int, height: Int): Bitmap =
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                val r = renderer ?: throw IllegalStateException("No document open")
                val page = r.openPage(index)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            }
        }

    override suspend fun extractText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        textCache[pageIndex]?.let { return@withContext it }

        if (Build.VERSION.SDK_INT < 35) {
            return@withContext ""
        }

        renderMutex.withLock {
            val r = renderer ?: return@withContext ""
            if (pageIndex < 0 || pageIndex >= r.pageCount) return@withContext ""
            val page = r.openPage(pageIndex)
            val text = try {
                // API 35+ provides getTextContents() on PdfRenderer.Page
                val textContents = page.textContents
                textContents.joinToString("") { it.text }
            } catch (_: Exception) {
                // If the method doesn't exist or fails, return empty
                ""
            } finally {
                page.close()
            }
            textCache[pageIndex] = text
            text
        }
    }

    override suspend fun search(query: String): List<PdfSearchResult> =
        withContext(Dispatchers.IO) {
            val r = renderer ?: return@withContext emptyList()
            if (query.isBlank()) return@withContext emptyList()

            val results = mutableListOf<PdfSearchResult>()
            val lowerQuery = query.lowercase()

            for (pageIndex in 0 until r.pageCount) {
                val pageText = extractText(pageIndex)
                if (pageText.isBlank()) continue

                val lowerText = pageText.lowercase()
                var searchStart = 0
                var matchCount = 0
                while (true) {
                    val foundAt = lowerText.indexOf(lowerQuery, searchStart)
                    if (foundAt == -1) break

                    // Extract context around the match (up to 40 chars before/after)
                    val contextStart = (foundAt - 40).coerceAtLeast(0)
                    val contextEnd = (foundAt + query.length + 40).coerceAtMost(pageText.length)
                    val matchContext = pageText.substring(contextStart, contextEnd)

                    results.add(
                        PdfSearchResult(
                            pageIndex = pageIndex,
                            matchText = matchContext,
                            matchIndex = matchCount
                        )
                    )
                    matchCount++
                    searchStart = foundAt + query.length
                }
            }
            results
        }

    override fun isOpen(): Boolean = renderer != null
}
