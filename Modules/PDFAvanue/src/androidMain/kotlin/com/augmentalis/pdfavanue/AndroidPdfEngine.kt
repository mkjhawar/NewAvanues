package com.augmentalis.pdfavanue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
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
 */
class AndroidPdfEngine(private val context: Context) : IPdfEngine {

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentDocument: PdfDocument? = null
    private val renderMutex = Mutex()

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
            val doc = PdfDocument(uri = uri, pageCount = pdfRenderer.pageCount)
            currentDocument = doc
            doc
        }

    override suspend fun closeCurrent() = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            renderer?.close(); renderer = null
            fileDescriptor?.close(); fileDescriptor = null
            currentDocument = null
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

    override suspend fun search(query: String): List<PdfSearchResult> = emptyList()
    override suspend fun extractText(pageIndex: Int): String = ""
    override fun isOpen(): Boolean = renderer != null
}
