package com.augmentalis.cockpit.mvp.content

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.DocumentType
import com.avanues.cockpit.core.window.WindowContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Document viewer content renderer for WindowContent.DocumentContent
 *
 * Routes to specialized viewers based on document type:
 * - PDF: Google Docs viewer (WebView fallback)
 * - IMAGE: BitmapFactory with Image composable
 * - TEXT: BufferedReader with scrollable Text
 * - UNKNOWN: Unsupported type message
 */
@Composable
fun DocumentViewerContent(
    documentContent: WindowContent.DocumentContent,
    modifier: Modifier = Modifier
) {
    when (documentContent.documentType) {
        DocumentType.PDF -> PdfViewer(documentContent.uri, modifier)
        DocumentType.IMAGE -> ImageViewer(documentContent.uri, modifier)
        DocumentType.TEXT -> TextViewer(documentContent.uri, modifier)
        DocumentType.VIDEO -> VideoViewer(documentContent, modifier)  // Phase 3: FR-3.3
        DocumentType.UNKNOWN -> UnsupportedTypeView(documentContent.mimeType, modifier)
    }
}

@Composable
private fun PdfViewer(uri: String, modifier: Modifier = Modifier) {
    // Use Google Docs viewer as WebView fallback
    val googleDocsUrl = "https://docs.google.com/viewer?url=$uri&embedded=true"
    WebViewContent(
        webContent = WindowContent.WebContent(url = googleDocsUrl),
        modifier = modifier
    )
}

@Composable
private fun ImageViewer(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        isLoading = true
        error = null
        try {
            bitmap = withContext(Dispatchers.IO) {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(Uri.parse(uri))
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load image"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Error loading image",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Document image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun TextViewer(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var text by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        isLoading = true
        error = null
        try {
            text = withContext(Dispatchers.IO) {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(Uri.parse(uri))
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.use { it.readText() }
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load text"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Error loading text",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            text != null -> {
                Text(
                    text = text ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

/**
 * Video viewer with playback position persistence (Phase 3: FR-3.3)
 * Currently shows placeholder - full implementation pending
 */
@Composable
private fun VideoViewer(documentContent: WindowContent.DocumentContent, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Video Player",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Playback position: ${documentContent.playbackPosition}ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = documentContent.uri,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun UnsupportedTypeView(mimeType: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Unsupported document type",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = mimeType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
