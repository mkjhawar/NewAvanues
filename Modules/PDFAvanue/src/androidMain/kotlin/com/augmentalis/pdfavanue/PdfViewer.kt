package com.augmentalis.pdfavanue

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.pdfavanue.model.PdfSearchResult
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.handlers.ModuleCommandCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

/**
 * Full-featured PDF viewer with three view modes, search, and voice support.
 *
 * View modes:
 *   - SINGLE_PAGE: One page at a time with swipe/tap navigation
 *   - CONTINUOUS_SCROLL: All pages in a vertical lazy list
 *   - TWO_PAGE_SPREAD: Side-by-side pages (landscape/tablet)
 *
 * Search: Text search via PdfRenderer API 35+ with result navigation.
 * Voice: All interactive elements have AVID contentDescription semantics.
 */
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
    var viewMode by remember { mutableStateOf(PdfViewMode.SINGLE_PAGE) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<PdfSearchResult>() }
    var activeSearchIndex by remember { mutableIntStateOf(-1) }
    var isSearching by remember { mutableStateOf(false) }

    // Continuous scroll state
    val lazyListState = rememberLazyListState()

    // Open document
    LaunchedEffect(uri) {
        isLoading = true; error = null
        try {
            val doc = engine.openDocument(uri)
            pageCount = doc.pageCount
            currentPage = initialPage.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
        } catch (e: Exception) { error = "Failed to open PDF: ${e.message}" }
        isLoading = false
    }

    // Render current page for SINGLE_PAGE mode
    LaunchedEffect(currentPage, containerWidth, containerHeight, viewMode) {
        if (viewMode != PdfViewMode.SINGLE_PAGE) return@LaunchedEffect
        if (!engine.isOpen() || containerWidth <= 0 || containerHeight <= 0) return@LaunchedEffect
        isLoading = true
        try {
            pageBytes = engine.renderPage(
                currentPage,
                (containerWidth * 2).coerceAtMost(4096),
                (containerHeight * 2).coerceAtMost(4096)
            )
            onPageChanged(currentPage)
        } catch (e: Exception) { error = "Render failed: ${e.message}" }
        isLoading = false
    }

    // Track scroll position in continuous mode to update currentPage
    LaunchedEffect(viewMode, lazyListState) {
        if (viewMode != PdfViewMode.CONTINUOUS_SCROLL) return@LaunchedEffect
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { visiblePage ->
                if (visiblePage != currentPage) {
                    currentPage = visiblePage
                    onPageChanged(visiblePage)
                }
            }
    }

    // Navigate to search result page
    LaunchedEffect(activeSearchIndex) {
        if (activeSearchIndex < 0 || activeSearchIndex >= searchResults.size) return@LaunchedEffect
        val targetPage = searchResults[activeSearchIndex].pageIndex
        when (viewMode) {
            PdfViewMode.CONTINUOUS_SCROLL -> {
                lazyListState.animateScrollToItem(targetPage)
            }
            PdfViewMode.TWO_PAGE_SPREAD -> {
                currentPage = targetPage - (targetPage % 2) // Align to even page
            }
            else -> {
                currentPage = targetPage
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            GlobalScope.launch(Dispatchers.IO + NonCancellable) {
                engine.closeCurrent()
            }
        }
    }

    // Wire voice command executor for PDF navigation and zoom controls
    DisposableEffect(Unit) {
        ModuleCommandCallbacks.pdfExecutor = { actionType, metadata ->
            executePdfCommand(
                actionType = actionType,
                metadata = metadata,
                getCurrentPage = { currentPage },
                setCurrentPage = { currentPage = it },
                getPageCount = { pageCount },
                getZoom = { zoom },
                setZoom = { zoom = it },
                setOffset = { x, y -> offsetX = x; offsetY = y },
                getViewMode = { viewMode },
                scrollToPage = { page ->
                    if (viewMode == PdfViewMode.CONTINUOUS_SCROLL) {
                        scope.launch { lazyListState.animateScrollToItem(page) }
                    }
                },
                onPageChanged = onPageChanged
            )
        }
        onDispose { ModuleCommandCallbacks.pdfExecutor = null }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar (collapsible)
        AnimatedVisibility(
            visible = showSearch,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    if (searchQuery.isNotBlank()) {
                        scope.launch {
                            isSearching = true
                            val results = engine.search(searchQuery)
                            searchResults.clear()
                            searchResults.addAll(results)
                            activeSearchIndex = if (results.isNotEmpty()) 0 else -1
                            isSearching = false
                        }
                    }
                },
                onClose = {
                    showSearch = false
                    searchResults.clear()
                    activeSearchIndex = -1
                    searchQuery = ""
                },
                onPrevious = {
                    if (searchResults.isNotEmpty() && activeSearchIndex > 0) {
                        activeSearchIndex--
                    }
                },
                onNext = {
                    if (searchResults.isNotEmpty() && activeSearchIndex < searchResults.size - 1) {
                        activeSearchIndex++
                    }
                },
                resultCount = searchResults.size,
                activeIndex = activeSearchIndex,
                isSearching = isSearching
            )
        }

        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged {
                    containerWidth = it.width
                    containerHeight = it.height
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                error != null -> {
                    Text(
                        error ?: "",
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                isLoading && viewMode == PdfViewMode.SINGLE_PAGE -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = colors.primary
                    )
                }
                pageCount > 0 -> {
                    when (viewMode) {
                        PdfViewMode.SINGLE_PAGE -> {
                            SinglePageView(
                                pageBytes = pageBytes,
                                zoom = zoom,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                onGesture = { panX, panY, gestureZoom ->
                                    zoom = (zoom * gestureZoom).coerceIn(0.5f, 5f)
                                    offsetX += panX
                                    offsetY += panY
                                },
                                currentPage = currentPage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PdfViewMode.CONTINUOUS_SCROLL -> {
                            ContinuousScrollView(
                                engine = engine,
                                pageCount = pageCount,
                                lazyListState = lazyListState,
                                containerWidth = containerWidth,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PdfViewMode.TWO_PAGE_SPREAD -> {
                            TwoPageSpreadView(
                                engine = engine,
                                currentPage = currentPage,
                                pageCount = pageCount,
                                containerWidth = containerWidth,
                                containerHeight = containerHeight,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Page indicator overlay for continuous scroll
            if (viewMode == PdfViewMode.CONTINUOUS_SCROLL && pageCount > 0) {
                val visiblePage by remember {
                    derivedStateOf { lazyListState.firstVisibleItemIndex + 1 }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "$visiblePage / $pageCount",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom toolbar
        if (pageCount > 0) {
            BottomToolbar(
                currentPage = currentPage,
                pageCount = pageCount,
                viewMode = viewMode,
                zoom = zoom,
                showSearch = showSearch,
                onPreviousPage = {
                    if (currentPage > 0) {
                        currentPage--
                        zoom = 1f; offsetX = 0f; offsetY = 0f
                        if (viewMode == PdfViewMode.CONTINUOUS_SCROLL) {
                            scope.launch { lazyListState.animateScrollToItem(currentPage) }
                        }
                    }
                },
                onNextPage = {
                    if (currentPage < pageCount - 1) {
                        currentPage++
                        zoom = 1f; offsetX = 0f; offsetY = 0f
                        if (viewMode == PdfViewMode.CONTINUOUS_SCROLL) {
                            scope.launch { lazyListState.animateScrollToItem(currentPage) }
                        }
                    }
                },
                onZoomOut = { zoom = (zoom * 0.8f).coerceAtLeast(0.5f) },
                onZoomIn = { zoom = (zoom * 1.25f).coerceAtMost(5f) },
                onToggleViewMode = {
                    viewMode = when (viewMode) {
                        PdfViewMode.SINGLE_PAGE -> PdfViewMode.CONTINUOUS_SCROLL
                        PdfViewMode.CONTINUOUS_SCROLL -> PdfViewMode.TWO_PAGE_SPREAD
                        PdfViewMode.TWO_PAGE_SPREAD -> PdfViewMode.SINGLE_PAGE
                    }
                    // Reset transform state on mode change
                    zoom = 1f; offsetX = 0f; offsetY = 0f
                },
                onToggleSearch = { showSearch = !showSearch }
            )
        }
    }
}

// =============================================================================
// Single Page View
// =============================================================================

@Composable
private fun SinglePageView(
    pageBytes: ByteArray?,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    onGesture: (panX: Float, panY: Float, zoom: Float) -> Unit,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onGesture(pan.x, pan.y, gestureZoom)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        pageBytes?.let { bytes ->
            val bitmap = remember(bytes) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
            bitmap?.let {
                Image(
                    it,
                    "PDF page ${currentPage + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }
        }
    }
}

// =============================================================================
// Continuous Scroll View
// =============================================================================

@Composable
private fun ContinuousScrollView(
    engine: AndroidPdfEngine,
    pageCount: Int,
    lazyListState: LazyListState,
    containerWidth: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            count = pageCount,
            key = { it }
        ) { pageIndex ->
            PdfPageItem(
                engine = engine,
                pageIndex = pageIndex,
                renderWidth = containerWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Voice: scroll to Page ${pageIndex + 1}"
                    }
            )
        }
    }
}

/**
 * Renders a single PDF page as a Composable item for use in LazyColumn.
 *
 * Uses [AndroidPdfEngine.renderPageBitmap] to render directly to Bitmap,
 * avoiding double encode/decode. Caches the rendered bitmap in remembered state
 * and re-renders when the page index or width changes.
 */
@Composable
private fun PdfPageItem(
    engine: AndroidPdfEngine,
    pageIndex: Int,
    renderWidth: Int,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }
    var error by remember(pageIndex) { mutableStateOf<String?>(null) }

    // Get page aspect ratio to determine render height
    val pageInfo = remember(pageIndex) { engine.getPage(pageIndex) }
    val aspectRatio = pageInfo?.aspectRatio ?: (8.5f / 11f) // Default to letter
    val effectiveWidth = renderWidth.coerceIn(100, 4096)
    val renderHeight = (effectiveWidth / aspectRatio).toInt().coerceIn(100, 8192)

    LaunchedEffect(pageIndex, effectiveWidth) {
        if (effectiveWidth <= 0) return@LaunchedEffect
        isLoading = true
        error = null
        try {
            bitmap = engine.renderPageBitmap(pageIndex, effectiveWidth, renderHeight)
        } catch (e: Exception) {
            error = "Page ${pageIndex + 1}: ${e.message}"
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        when {
            error != null -> {
                Text(
                    error ?: "",
                    color = colors.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
            isLoading || bitmap == null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colors.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Page ${pageIndex + 1}",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            else -> {
                bitmap?.let { bmp ->
                    Image(
                        bmp.asImageBitmap(),
                        contentDescription = "PDF page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// =============================================================================
// Two-Page Spread View
// =============================================================================

@Composable
private fun TwoPageSpreadView(
    engine: AndroidPdfEngine,
    currentPage: Int,
    pageCount: Int,
    containerWidth: Int,
    containerHeight: Int,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    // Align to even page for spread
    val leftPage = currentPage - (currentPage % 2)
    val rightPage = leftPage + 1
    val halfWidth = (containerWidth / 2).coerceAtLeast(100)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left page
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            if (leftPage in 0 until pageCount) {
                SpreadPageItem(
                    engine = engine,
                    pageIndex = leftPage,
                    renderWidth = halfWidth,
                    renderHeight = containerHeight,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surface.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("", color = colors.textSecondary)
                }
            }
        }

        // Right page
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rightPage in 0 until pageCount) {
                SpreadPageItem(
                    engine = engine,
                    pageIndex = rightPage,
                    renderWidth = halfWidth,
                    renderHeight = containerHeight,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surface.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun SpreadPageItem(
    engine: AndroidPdfEngine,
    pageIndex: Int,
    renderWidth: Int,
    renderHeight: Int,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }

    LaunchedEffect(pageIndex, renderWidth, renderHeight) {
        if (renderWidth <= 0 || renderHeight <= 0) return@LaunchedEffect
        isLoading = true
        try {
            bitmap = engine.renderPageBitmap(
                pageIndex,
                renderWidth.coerceAtMost(2048),
                renderHeight.coerceAtMost(4096)
            )
        } catch (_: Exception) { /* silently fail, show placeholder */ }
        isLoading = false
    }

    Box(
        modifier = modifier.background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading || bitmap == null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colors.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Page ${pageIndex + 1}",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            else -> {
                bitmap?.let { bmp ->
                    Image(
                        bmp.asImageBitmap(),
                        contentDescription = "PDF page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// =============================================================================
// Search Bar
// =============================================================================

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    resultCount: Int,
    activeIndex: Int,
    isSearching: Boolean
) {
    val colors = AvanueTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search input
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.background.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Voice: click Search PDF" }
                )
                if (query.isNotEmpty()) {
                    Text(
                        if (isSearching) "..."
                        else if (resultCount > 0) "${activeIndex + 1}/$resultCount"
                        else if (query.isNotBlank()) "0 results"
                        else "",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Navigate results
        if (resultCount > 0) {
            IconButton(
                onClick = onPrevious,
                enabled = activeIndex > 0,
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Voice: click Previous Result" }
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    "Previous result",
                    tint = if (activeIndex > 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onNext,
                enabled = activeIndex < resultCount - 1,
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Voice: click Next Result" }
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    "Next result",
                    tint = if (activeIndex < resultCount - 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp)
                .semantics { contentDescription = "Voice: click Close Search" }
        ) {
            Icon(
                Icons.Default.Close,
                "Close search",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =============================================================================
// Bottom Toolbar
// =============================================================================

@Composable
private fun BottomToolbar(
    currentPage: Int,
    pageCount: Int,
    viewMode: PdfViewMode,
    zoom: Float,
    showSearch: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleSearch: () -> Unit
) {
    val colors = AvanueTheme.colors
    val viewModeIcon = when (viewMode) {
        PdfViewMode.SINGLE_PAGE -> Icons.Default.ViewDay
        PdfViewMode.CONTINUOUS_SCROLL -> Icons.Default.ViewStream
        PdfViewMode.TWO_PAGE_SPREAD -> Icons.Default.ViewColumn
    }
    val viewModeLabel = when (viewMode) {
        PdfViewMode.SINGLE_PAGE -> "Single"
        PdfViewMode.CONTINUOUS_SCROLL -> "Scroll"
        PdfViewMode.TWO_PAGE_SPREAD -> "Spread"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.surface.copy(alpha = 0.9f))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous page
        IconButton(
            onClick = onPreviousPage,
            enabled = currentPage > 0,
            modifier = Modifier.semantics { contentDescription = "Voice: click Previous Page" }
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                "Previous",
                tint = if (currentPage > 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.3f)
            )
        }

        // Page indicator
        Text(
            "${currentPage + 1} / $pageCount",
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        // Zoom controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Voice: click Zoom Out" }
            ) {
                Icon(
                    Icons.Default.ZoomOut,
                    "Zoom out",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                "${(zoom * 100).toInt()}%",
                color = colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Voice: click Zoom In" }
            ) {
                Icon(
                    Icons.Default.ZoomIn,
                    "Zoom in",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // View mode toggle
        IconButton(
            onClick = onToggleViewMode,
            modifier = Modifier.semantics {
                contentDescription = "Voice: click View Mode"
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    viewModeIcon,
                    viewModeLabel,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    viewModeLabel,
                    color = colors.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Search toggle
        IconButton(
            onClick = onToggleSearch,
            modifier = Modifier.semantics { contentDescription = "Voice: click Search" }
        ) {
            Icon(
                Icons.Default.Search,
                "Search",
                tint = if (showSearch) colors.primary else colors.textPrimary
            )
        }

        // Next page
        IconButton(
            onClick = onNextPage,
            enabled = currentPage < pageCount - 1,
            modifier = Modifier.semantics { contentDescription = "Voice: click Next Page" }
        ) {
            Icon(
                Icons.Default.ChevronRight,
                "Next",
                tint = if (currentPage < pageCount - 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.3f)
            )
        }
    }
}

// =============================================================================
// Voice Command Executor
// =============================================================================

@Suppress("LongParameterList")
private fun executePdfCommand(
    actionType: CommandActionType,
    metadata: Map<String, String>,
    getCurrentPage: () -> Int,
    setCurrentPage: (Int) -> Unit,
    getPageCount: () -> Int,
    getZoom: () -> Float,
    setZoom: (Float) -> Unit,
    setOffset: (Float, Float) -> Unit,
    @Suppress("unused") getViewMode: () -> PdfViewMode,
    scrollToPage: (Int) -> Unit,
    onPageChanged: (Int) -> Unit,
): HandlerResult {
    val pageCount = getPageCount()
    if (pageCount == 0) return HandlerResult.failure("No PDF document open", recoverable = true)

    return when (actionType) {
        CommandActionType.PDF_NEXT_PAGE -> {
            val current = getCurrentPage()
            if (current < pageCount - 1) {
                val newPage = current + 1
                setCurrentPage(newPage); setZoom(1f); setOffset(0f, 0f)
                scrollToPage(newPage); onPageChanged(newPage)
                HandlerResult.success("Page ${newPage + 1} of $pageCount")
            } else HandlerResult.failure("Already on last page", recoverable = true)
        }
        CommandActionType.PDF_PREVIOUS_PAGE -> {
            val current = getCurrentPage()
            if (current > 0) {
                val newPage = current - 1
                setCurrentPage(newPage); setZoom(1f); setOffset(0f, 0f)
                scrollToPage(newPage); onPageChanged(newPage)
                HandlerResult.success("Page ${newPage + 1} of $pageCount")
            } else HandlerResult.failure("Already on first page", recoverable = true)
        }
        CommandActionType.PDF_FIRST_PAGE -> {
            setCurrentPage(0); setZoom(1f); setOffset(0f, 0f)
            scrollToPage(0); onPageChanged(0)
            HandlerResult.success("Page 1 of $pageCount")
        }
        CommandActionType.PDF_LAST_PAGE -> {
            val last = pageCount - 1
            setCurrentPage(last); setZoom(1f); setOffset(0f, 0f)
            scrollToPage(last); onPageChanged(last)
            HandlerResult.success("Page $pageCount of $pageCount")
        }
        CommandActionType.PDF_GO_TO_PAGE -> {
            val target = (metadata["page"] ?: metadata["number"])?.toIntOrNull()
            if (target != null && target in 1..pageCount) {
                val idx = target - 1
                setCurrentPage(idx); setZoom(1f); setOffset(0f, 0f)
                scrollToPage(idx); onPageChanged(idx)
                HandlerResult.success("Page $target of $pageCount")
            } else HandlerResult.failure("Say a number between 1 and $pageCount", recoverable = true)
        }
        CommandActionType.PDF_ZOOM_IN, CommandActionType.ZOOM_IN -> {
            val z = (getZoom() * 1.25f).coerceAtMost(5f); setZoom(z)
            HandlerResult.success("Zoom ${(z * 100).toInt()}%")
        }
        CommandActionType.PDF_ZOOM_OUT, CommandActionType.ZOOM_OUT -> {
            val z = (getZoom() * 0.8f).coerceAtLeast(0.5f); setZoom(z)
            if (z <= 1f) setOffset(0f, 0f)
            HandlerResult.success("Zoom ${(z * 100).toInt()}%")
        }
        CommandActionType.PDF_FIT_PAGE -> {
            setZoom(1f); setOffset(0f, 0f)
            HandlerResult.success("Fit to page")
        }
        else -> HandlerResult.failure("Unsupported PDF action: $actionType", recoverable = true)
    }
}
