package com.augmentalis.webavanue

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.OceanTheme
import com.augmentalis.webavanue.Favorite
import kotlin.math.abs

/**
 * SpatialFavoritesShelf - 3D carousel shelf for favorites
 *
 * Displays favorites as cards on a virtual "shelf" with:
 * - Cards at different z-depths creating a 3D carousel effect
 * - Center card is largest (closest)
 * - Cards fade and shrink as they move to edges
 * - Horizontal scroll/swipe to browse
 * - Tap to open favorite
 *
 * Based on IDEACODE UI Guidelines Z-Axis Spatial Layer System
 */
@Composable
fun SpatialFavoritesShelf(
    favorites: List<Favorite>,
    onFavoriteClick: (Favorite) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628).copy(alpha = 0.95f),
                        Color(0xFF1A2744).copy(alpha = 0.95f),
                        Color(0xFF0A1628).copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = OceanTheme.starActive
                )
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        if (favorites.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = OceanTheme.starActive.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No favorites yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Tap the star icon to add favorites",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            // Spatial shelf carousel
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentPadding = PaddingValues(horizontal = 60.dp),
                horizontalArrangement = Arrangement.spacedBy((-20).dp),  // Overlap cards slightly
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(favorites) { index, favorite ->
                    // Calculate position relative to viewport center
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                    val viewportCenter = listState.layoutInfo.viewportEndOffset / 2
                    val itemCenter = itemInfo?.let { it.offset + it.size / 2 } ?: 0
                    val distanceFromCenter = (itemCenter - viewportCenter) / viewportCenter.toFloat().coerceAtLeast(1f)

                    // Enhanced 3D carousel effects
                    val scale = (1f - abs(distanceFromCenter) * 0.25f).coerceIn(0.65f, 1f)
                    val alpha = (1f - abs(distanceFromCenter) * 0.35f).coerceIn(0.5f, 1f)

                    // 3D rotation - cards face the center like a carousel
                    val rotationY = distanceFromCenter * 35f  // Rotate cards toward center
                    val rotationZ = distanceFromCenter * -3f  // Slight tilt for depth

                    // Vertical offset - items farther from center drop slightly
                    val translationY = abs(distanceFromCenter) * 20f

                    SpatialFavoriteCard(
                        favorite = favorite,
                        onClick = { onFavoriteClick(favorite) },
                        modifier = Modifier
                            .width(160.dp)
                            .height(200.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                this.rotationY = rotationY
                                this.rotationZ = rotationZ
                                this.translationY = translationY.dp.toPx()
                                // Camera distance for better 3D perspective
                                cameraDistance = 10f * density
                                // Enhanced shadows for depth
                                shadowElevation = (32f * scale).coerceIn(8f, 32f)
                            }
                    )
                }
            }
        }

        // Bottom hint
        Text(
            text = "Swipe to browse • Tap to open",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

/**
 * SpatialFavoriteCard - Individual favorite card with 3D styling
 */
@Composable
fun SpatialFavoriteCard(
    favorite: Favorite,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = OceanTheme.primary.copy(alpha = 0.2f),
                spotColor = OceanTheme.primary.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = OceanTheme.surfaceElevated
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Favicon / Icon area
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                OceanTheme.primary.copy(alpha = 0.3f),
                                OceanTheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Show first letter of title as placeholder
                Text(
                    text = favorite.title.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OceanTheme.primary
                )
            }

            // Title
            Text(
                text = favorite.title,
                style = MaterialTheme.typography.titleSmall,
                color = OceanTheme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // URL (truncated)
            Text(
                text = favorite.url
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .take(20) + if (favorite.url.length > 20) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = OceanTheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Star indicator
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = OceanTheme.starActive,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Compact spatial favorites row for inline display
 */
@Composable
fun SpatialFavoritesRow(
    favorites: List<Favorite>,
    onFavoriteClick: (Favorite) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        modifier = modifier.height(120.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(favorites.take(10)) { index, favorite ->
            val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
            val viewportCenter = listState.layoutInfo.viewportEndOffset / 2
            val itemCenter = itemInfo?.let { it.offset + it.size / 2 } ?: 0
            val distanceFromCenter = (itemCenter - viewportCenter) / viewportCenter.toFloat().coerceAtLeast(1f)

            val scale = (1f - abs(distanceFromCenter) * 0.15f).coerceIn(0.85f, 1f)
            val alpha = (1f - abs(distanceFromCenter) * 0.2f).coerceIn(0.7f, 1f)

            CompactFavoriteCard(
                favorite = favorite,
                onClick = { onFavoriteClick(favorite) },
                modifier = Modifier
                    .width(80.dp)
                    .height(100.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
    }
}

/**
 * Compact favorite card for row display
 */
@Composable
fun CompactFavoriteCard(
    favorite: Favorite,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = OceanTheme.surfaceElevated,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        OceanTheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = favorite.title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = OceanTheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = favorite.title,
                style = MaterialTheme.typography.labelSmall,
                color = OceanTheme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
