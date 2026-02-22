package com.augmentalis.pdfavanue

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

@Composable
fun PdfViewer(
    uri: String,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = AvanueTheme.colors
    val engine = remember { AndroidPdfEngine(context) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(initialPage) }
    var pageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableIntStateOf(800) }
    var containerHeight by remember { mutableIntStateOf(1200) }

    LaunchedEffect(uri) {
        isLoading = true; error = null
        try {
            val doc = engine.openDocument(uri)
            pageCount = doc.pageCount
            currentPage = initialPage.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
        } catch (e: Exception) { error = "Failed to open PDF: ${e.message}" }
        isLoading = false
    }

    LaunchedEffect(currentPage, containerWidth, containerHeight) {
        if (!engine.isOpen() || containerWidth <= 0 || containerHeight <= 0) return@LaunchedEffect
        isLoading = true
        try {
            pageBytes = engine.renderPage(currentPage, (containerWidth * 2).coerceAtMost(4096), (containerHeight * 2).coerceAtMost(4096))
            onPageChanged(currentPage)
        } catch (e: Exception) { error = "Render failed: ${e.message}" }
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            GlobalScope.launch(Dispatchers.IO + NonCancellable) {
                engine.closeCurrent()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .onSizeChanged { containerWidth = it.width; containerHeight = it.height }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x; offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                error != null -> Text(error ?: "", color = colors.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                isLoading -> CircularProgressIndicator(modifier = Modifier.size(48.dp), color = colors.primary)
                pageBytes != null -> {
                    val bitmap = remember(pageBytes) {
                        pageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
                    }
                    bitmap?.let {
                        Image(it, "PDF page ${currentPage + 1}", contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = offsetX, translationY = offsetY))
                    }
                }
            }
        }
        if (pageCount > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentPage > 0) { currentPage--; zoom = 1f; offsetX = 0f; offsetY = 0f } },
                    enabled = currentPage > 0,
                    modifier = Modifier.semantics { contentDescription = "Voice: click Previous Page" }
                ) {
                    Icon(Icons.Default.ChevronLeft, "Previous", tint = colors.textPrimary)
                }
                Text("${currentPage + 1} / $pageCount", color = colors.textPrimary, fontSize = 14.sp)
                Row {
                    IconButton(
                        onClick = { zoom = (zoom * 0.8f).coerceAtLeast(0.5f) },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Zoom Out" }
                    ) {
                        Icon(Icons.Default.ZoomOut, "Zoom out", tint = colors.textPrimary)
                    }
                    IconButton(
                        onClick = { zoom = (zoom * 1.25f).coerceAtMost(5f) },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Zoom In" }
                    ) {
                        Icon(Icons.Default.ZoomIn, "Zoom in", tint = colors.textPrimary)
                    }
                }
                IconButton(
                    onClick = { if (currentPage < pageCount - 1) { currentPage++; zoom = 1f; offsetX = 0f; offsetY = 0f } },
                    enabled = currentPage < pageCount - 1,
                    modifier = Modifier.semantics { contentDescription = "Voice: click Next Page" }
                ) {
                    Icon(Icons.Default.ChevronRight, "Next", tint = colors.textPrimary)
                }
            }
        }
    }
}
