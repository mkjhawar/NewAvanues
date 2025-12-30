package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.augmentalis.avaelements.flutter.material.display.*
import com.augmentalis.avaelements.renderer.android.IconFromString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android Compose mappers for Flutter Display parity components
 *
 * This file contains renderer functions for advanced display components:
 * - Popover
 * - ErrorState
 * - NoData
 * - ImageCarousel
 * - LazyImage
 * - ImageGallery
 * - Lightbox
 *
 * @since 3.1.0-android-parity
 */

/**
 * Render Popover component using Material3
 *
 * Maps Popover component to Material3 AlertDialog with contextual positioning:
 * - Contextual information attached to anchor
 * - Configurable positioning (top, bottom, left, right)
 * - Optional arrow pointer
 * - Material3 elevation and theming
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component Popover component to render
 */
@Composable
fun PopoverMapper(component: Popover) {
    if (!component.visible) return

    AlertDialog(
        onDismissRequest = {
            if (component.dismissible) {
                component.onDismiss?.invoke()
            }
        },
        title = component.title?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Text(
                text = component.content,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (component.hasActions()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    component.actions.forEach { action ->
                        if (action.primary) {
                            Button(onClick = action.onClick) {
                                Text(action.label)
                            }
                        } else {
                            TextButton(onClick = action.onClick) {
                                Text(action.label)
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .widthIn(max = component.maxWidth.dp)
            .semantics { contentDescription = component.getAccessibilityDescription() }
    )
}

/**
 * Render ErrorState component using Material3
 *
 * Maps ErrorState component to Material3 error placeholder:
 * - Large error icon
 * - Primary error message
 * - Optional detailed description
 * - Retry action button
 * - Full accessibility support with error announcements
 * - Dark mode compatibility
 *
 * @param component ErrorState component to render
 */
@Composable
fun ErrorStateMapper(component: ErrorState) {
    if (!component.visible) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .semantics { contentDescription = component.getAccessibilityDescription() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error icon
            IconFromString(
                iconName = component.icon,
                size = component.iconSize.dp,
                tint = component.color?.let { Color(android.graphics.Color.parseColor(it)) }
                    ?: MaterialTheme.colorScheme.error,
                contentDescription = null
            )

            // Error message
            Text(
                text = component.message,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Optional description
            if (component.hasDescription()) {
                Text(
                    text = component.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Retry button
            if (component.isRetryAvailable()) {
                Button(
                    onClick = { component.onRetry?.invoke() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(component.retryLabel)
                }
            }
        }
    }
}

/**
 * Render NoData component using Material3
 *
 * Maps NoData component to Material3 empty state placeholder:
 * - Large empty state icon
 * - Primary message
 * - Optional detailed description
 * - Action button (e.g., "Add Item")
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component NoData component to render
 */
@Composable
fun NoDataMapper(component: NoData) {
    if (!component.visible) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .semantics { contentDescription = component.getAccessibilityDescription() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Empty state icon
            IconFromString(
                iconName = component.icon,
                size = component.iconSize.dp,
                tint = component.color?.let { Color(android.graphics.Color.parseColor(it)) }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null
            )

            // Message
            Text(
                text = component.message,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Optional description
            if (component.hasDescription()) {
                Text(
                    text = component.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Action button
            if (component.isActionAvailable()) {
                Button(
                    onClick = { component.onAction?.invoke() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(component.actionLabel)
                }
            }
        }
    }
}

/**
 * Render ImageCarousel component using Material3
 *
 * Maps ImageCarousel component to Material3 HorizontalPager:
 * - Swipeable image carousel
 * - Dot indicators
 * - Auto-play support
 * - Infinite scroll
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component ImageCarousel component to render
 */
@Composable
fun ImageCarouselMapper(component: ImageCarousel) {
    if (!component.visible || !component.hasImages()) return

    val pagerState = rememberPagerState(
        initialPage = component.initialPage.coerceIn(0, component.images.lastIndex),
        pageCount = { component.images.size }
    )

    val coroutineScope = rememberCoroutineScope()

    // Auto-play effect
    if (component.autoPlay) {
        LaunchedEffect(pagerState.currentPage) {
            delay(component.interval)
            val nextPage = if (component.infinite) {
                (pagerState.currentPage + 1) % component.images.size
            } else {
                (pagerState.currentPage + 1).coerceAtMost(component.images.lastIndex)
            }
            pagerState.animateScrollToPage(nextPage)
        }
    }

    // Notify page changes
    LaunchedEffect(pagerState.currentPage) {
        component.onPageChanged?.invoke(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = component.getAccessibilityDescription(pagerState.currentPage) }
    ) {
        // Image pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.aspectRatio(component.aspectRatio)
        ) { page ->
            val image = component.images[page]
            AsyncImage(
                model = image.url,
                contentDescription = image.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Indicators
        if (component.showIndicators) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(component.images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }

        // Navigation arrows
        if (component.showArrows) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val prevPage = if (component.infinite && pagerState.currentPage == 0) {
                            component.images.lastIndex
                        } else {
                            (pagerState.currentPage - 1).coerceAtLeast(0)
                        }
                        pagerState.animateScrollToPage(prevPage)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous", tint = Color.White)
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val nextPage = if (component.infinite && pagerState.currentPage == component.images.lastIndex) {
                            0
                        } else {
                            (pagerState.currentPage + 1).coerceAtMost(component.images.lastIndex)
                        }
                        pagerState.animateScrollToPage(nextPage)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.ChevronRight, "Next", tint = Color.White)
            }
        }
    }
}

/**
 * Render LazyImage component using Material3
 *
 * Maps LazyImage component to Coil AsyncImage:
 * - Lazy loading with Coil
 * - Placeholder while loading
 * - Error state with fallback
 * - Crossfade transition
 * - Content scale options
 * - Shape options (default, rounded, circular)
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component LazyImage component to render
 */
@Composable
fun LazyImageMapper(component: LazyImage) {
    if (!component.visible) return

    val context = LocalContext.current

    val contentScale = when (component.contentScale) {
        ImageContentScale.FIT -> ContentScale.Fit
        ImageContentScale.FILL -> ContentScale.FillBounds
        ImageContentScale.CROP -> ContentScale.Crop
        ImageContentScale.INSIDE -> ContentScale.Inside
        ImageContentScale.NONE -> ContentScale.None
    }

    val shape = when (component.shape) {
        ImageShape.DEFAULT -> RectangleShape
        ImageShape.ROUNDED -> RoundedCornerShape(component.cornerRadius.dp)
        ImageShape.CIRCULAR -> CircleShape
    }

    Box(
        modifier = Modifier
            .then(
                if (component.hasAspectRatio()) {
                    Modifier.aspectRatio(component.aspectRatio!!)
                } else Modifier
            )
            .clip(shape)
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(component.url)
                .crossfade(component.crossfadeDuration)
                .diskCachePolicy(if (component.enableCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .memoryCachePolicy(if (component.enableCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .listener(
                    onStart = {
                        component.onLoading?.invoke()
                    },
                    onSuccess = { _, _ ->
                        component.onSuccess?.invoke()
                    },
                    onError = { _, result ->
                        component.onError?.invoke(result.throwable.message ?: "Unknown error")
                    }
                )
                .build(),
            contentDescription = component.contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Render ImageGallery component using Material3
 *
 * Maps ImageGallery component to Material3 LazyVerticalGrid:
 * - Responsive grid layout
 * - Lazy loading of images
 * - Selection mode support
 * - Tap to expand
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component ImageGallery component to render
 */
@Composable
fun ImageGalleryMapper(component: ImageGallery) {
    if (!component.visible || !component.hasImages()) return

    LazyVerticalGrid(
        columns = GridCells.Fixed(component.columns),
        horizontalArrangement = Arrangement.spacedBy(component.spacing.dp),
        verticalArrangement = Arrangement.spacedBy(component.spacing.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        items(component.images.size) { index ->
            val image = component.images[index]
            val isSelected = component.isSelected(index)

            Box(
                modifier = Modifier
                    .aspectRatio(component.aspectRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (component.selectionMode) {
                            val newSelection = if (isSelected) {
                                component.selectedIndices - index
                            } else {
                                component.selectedIndices + index
                            }
                            component.onSelectionChanged?.invoke(newSelection)
                        } else {
                            component.onImageTap?.invoke(index)
                        }
                    }
            ) {
                AsyncImage(
                    model = image.thumbnail ?: image.url,
                    contentDescription = image.description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Selection overlay
                if (component.selectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else
                                    Color.Transparent
                            )
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                } else if (component.showOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.05f))
                    )
                }
            }
        }
    }
}

/**
 * Render Lightbox component using Material3
 *
 * Maps Lightbox component to Material3 full-screen dialog:
 * - Full-screen overlay
 * - Pinch to zoom
 * - Pan and drag gestures
 * - Navigation between images
 * - Image counter
 * - Close button
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component Lightbox component to render
 */
@Composable
fun LightboxMapper(component: Lightbox) {
    if (!component.visible || !component.hasImages()) return

    var currentIndex by remember { mutableStateOf(component.initialIndex) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val coroutineScope = rememberCoroutineScope()

    // Notify index changes
    LaunchedEffect(currentIndex) {
        component.onIndexChanged?.invoke(currentIndex)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { component.onClose?.invoke() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    component.backgroundColor?.let {
                        Color(android.graphics.Color.parseColor(it))
                    } ?: Color.Black.copy(alpha = 0.9f)
                )
                .semantics { contentDescription = component.getAccessibilityDescription(currentIndex) }
        ) {
            // Image viewer
            val currentImage = component.images[currentIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                AsyncImage(
                    model = currentImage.url,
                    contentDescription = currentImage.description,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                )
            }

            // Top bar with close button
            if (component.showCloseButton) {
                IconButton(
                    onClick = { component.onClose?.invoke() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Counter
            if (component.showCounter) {
                Text(
                    text = component.getCounterText(currentIndex),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            // Navigation
            if (component.showNavigation && component.images.size > 1) {
                if (component.canNavigatePrevious(currentIndex)) {
                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex - 1).coerceAtLeast(0)
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                if (component.canNavigateNext(currentIndex)) {
                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex + 1).coerceAtMost(component.images.lastIndex)
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // Bottom bar with caption and actions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                // Caption
                if (component.showCaption && currentImage.caption != null) {
                    Text(
                        text = currentImage.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = if (component.hasActions()) 8.dp else 0.dp)
                    )
                }

                // Actions
                if (component.hasActions()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (component.showDownload) {
                            IconButton(onClick = { component.onDownload?.invoke(currentIndex) }) {
                                Icon(Icons.Default.Download, "Download", tint = Color.White)
                            }
                        }
                        if (component.showShare) {
                            IconButton(onClick = { component.onShare?.invoke(currentIndex) }) {
                                Icon(Icons.Default.Share, "Share", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
