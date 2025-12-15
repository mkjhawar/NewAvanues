package com.augmentalis.webavanue.ui.screen.spatial

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.augmentalis.webavanue.ui.screen.theme.OceanTheme
import com.augmentalis.webavanue.ui.viewmodel.TabUiState
import kotlin.math.abs

/**
 * SpatialTabSwitcher - 3D Z-axis tab switcher with depth perspective
 *
 * Displays tabs in a stacked card view with:
 * - Active tab at front (z=0)
 * - Other tabs recede into the background with decreasing scale/opacity
 * - Swipe left/right to navigate between tabs
 * - Tap on background tab to bring it forward
 *
 * Based on IDEACODE UI Guidelines Z-Axis Spatial Layer System
 */
@Composable
fun SpatialTabSwitcher(
    tabs: List<TabUiState>,
    activeTabId: String?,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabPin: (String) -> Unit = {},
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = tabs.indexOfFirst { it.tab.id == activeTabId }.coerceAtLeast(0)
    var currentIndex by remember { mutableStateOf(activeIndex) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Animate current index changes
    val animatedIndex by animateFloatAsState(
        targetValue = currentIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "index"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF1A2744),
                        Color(0xFF0A1628)
                    )
                )
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // Snap to nearest tab based on drag direction
                        if (abs(dragOffset) > 100) {
                            if (dragOffset > 0 && currentIndex > 0) {
                                currentIndex--
                            } else if (dragOffset < 0 && currentIndex < tabs.size - 1) {
                                currentIndex++
                            }
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        // Header with close and new tab buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(100f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Tab count indicator
            Text(
                text = "${currentIndex + 1} / ${tabs.size}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            // New tab button
            IconButton(onClick = onNewTab) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = Color.White
                )
            }
        }

        // Spatial tab stack
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Render tabs from back to front - enhanced 3D stack effect
            tabs.forEachIndexed { index, tabState ->
                val relativeIndex = index - animatedIndex - (dragOffset / 300f)

                // Enhanced 3D z-depth effects for visible stack
                val zDepth = relativeIndex * 0.5f  // Each tab is 0.5m apart
                val scale = (1f - abs(zDepth) * 0.12f).coerceIn(0.5f, 1f)
                val alpha = (1f - abs(zDepth) * 0.18f).coerceIn(0.3f, 1f)

                // 3D stacking: vertical offset + horizontal fan for visible depth
                val translationY = zDepth * 60f  // Vertical offset
                val translationX = relativeIndex * 25f  // Horizontal fan offset

                // 3D rotation for perspective effect
                val rotationX = zDepth * -8f  // Tilt forward/backward
                val rotationY = relativeIndex * 3f  // Slight Y rotation for fan effect
                val rotationZ = relativeIndex * -1.5f  // Slight tilt for natural stack

                // Only render visible tabs (within range)
                if (abs(relativeIndex) <= 5) {
                    SpatialTabCard(
                        tabState = tabState,
                        isActive = index == currentIndex,
                        onSelect = {
                            currentIndex = index
                            onTabSelect(tabState.tab.id)
                        },
                        onClose = { onTabClose(tabState.tab.id) },
                        onPin = { onTabPin(tabState.tab.id) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(0.7f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                this.translationY = translationY.dp.toPx()
                                this.translationX = translationX.dp.toPx()
                                this.rotationX = rotationX
                                this.rotationY = rotationY
                                this.rotationZ = rotationZ
                                // Set camera distance for better 3D perspective
                                cameraDistance = 12f * density
                                // Shadow increases with distance
                                shadowElevation = if (index == currentIndex) 32f else 12f * scale
                            }
                            .zIndex(-abs(relativeIndex))
                    )
                }
            }
        }

        // Bottom indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentIndex) 12.dp else 8.dp)
                        .background(
                            color = if (index == currentIndex) OceanTheme.primary else Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { currentIndex = index }
                )
            }
        }

        // Swipe hint text
        Text(
            text = "← Swipe to navigate →",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * SpatialTabCard - Individual tab card with 3D styling
 *
 * Features:
 * - Pin/unpin tab functionality
 * - Visual indicator for pinned tabs
 * - Close button
 */
@Composable
fun SpatialTabCard(
    tabState: TabUiState,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onPin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPinned = tabState.tab.isPinned

    Card(
        modifier = modifier
            .shadow(
                elevation = if (isActive) 24.dp else 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isPinned) Color(0xFFFFD700).copy(alpha = 0.3f) else OceanTheme.primary.copy(alpha = 0.3f),
                spotColor = if (isPinned) Color(0xFFFFD700).copy(alpha = 0.5f) else OceanTheme.primary.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) OceanTheme.surface else OceanTheme.surfaceElevated
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isPinned) BorderStroke(2.dp, Color(0xFFFFD700).copy(alpha = 0.6f)) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tab header with title, pin button, and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            isPinned -> Color(0xFFFFD700).copy(alpha = 0.15f)
                            isActive -> OceanTheme.primary.copy(alpha = 0.2f)
                            else -> Color.Black.copy(alpha = 0.2f)
                        }
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favicon placeholder with pin indicator overlay
                Box(
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (isPinned) Color(0xFFFFD700).copy(alpha = 0.3f) else OceanTheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    // Pin indicator on favicon
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                // Title
                Text(
                    text = tabState.tab.title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isActive) OceanTheme.textPrimary else OceanTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                // Pin button
                IconButton(
                    onClick = onPin,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "Unpin tab" else "Pin tab",
                        tint = if (isPinned) Color(0xFFFFD700) else OceanTheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Close button (disabled for pinned tabs - must unpin first)
                IconButton(
                    onClick = onClose,
                    enabled = !isPinned,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isPinned) "Unpin to close" else "Close tab",
                        tint = if (isPinned) OceanTheme.textSecondary.copy(alpha = 0.4f) else OceanTheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // URL bar simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = tabState.tab.url.ifBlank { "about:blank" },
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Content preview area (placeholder)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Show loading indicator or preview
                if (tabState.isLoading) {
                    CircularProgressIndicator(
                        color = OceanTheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    // Content preview placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tabState.tab.title.take(1).uppercase().ifEmpty { "?" },
                            style = MaterialTheme.typography.displayLarge,
                            color = OceanTheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
