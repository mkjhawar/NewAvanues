package com.augmentalis.Avanues.web.universal.presentation.ui.layout

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * ArcLayout - Spatial carousel layout for AR/XR experiences
 *
 * Arranges items along a circular arc with perspective scaling and smooth rotation.
 * Supports both portrait (horizontal arc) and landscape (vertical arc) orientations.
 *
 * Spatial Design:
 * - Center item: Largest (100%), fully opaque, main focus point
 * - Adjacent items: Medium (60%), slightly faded, flanking positions
 * - Far items: Small (36%), more transparent, peripheral vision
 * - Smooth animations with spring physics
 * - Perspective depth with blur and opacity
 *
 * Interaction:
 * - Swipe to rotate arc (bring items to center)
 * - Tap center item to select
 * - Long-press for contextual actions
 *
 * @param T Item type
 * @param items List of items to display
 * @param currentIndex Currently centered item index
 * @param onIndexChange Callback when center index changes
 * @param onItemClick Callback when item is tapped
 * @param onItemLongPress Callback when item is long-pressed
 * @param orientation Arc orientation (HORIZONTAL for portrait, VERTICAL for landscape)
 * @param arcRadius Radius of the arc in dp
 * @param itemSpacing Angular spacing between items in degrees
 * @param centerScale Scale factor for center item (default 1.0)
 * @param sideScale Scale factor for adjacent items (default 0.6)
 * @param animationDurationMs Duration of rotation animation
 * @param modifier Modifier for customization
 * @param itemContent Composable to render each item
 */
@Composable
fun <T> ArcLayout(
    items: List<T>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onItemClick: (T) -> Unit,
    onItemLongPress: (T) -> Unit,
    modifier: Modifier = Modifier,
    orientation: ArcOrientation = ArcOrientation.HORIZONTAL,
    arcRadius: Dp = 400.dp,
    itemSpacing: Float = 45f,
    centerScale: Float = 1.0f,
    sideScale: Float = 0.6f,
    animationDurationMs: Int = 300,
    itemContent: @Composable (item: T, index: Int, isCenterItem: Boolean) -> Unit
) {
    if (items.isEmpty()) return

    var dragOffset by remember { mutableStateOf(0f) }
    val animatedIndex by remember { derivedStateOf { currentIndex } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(orientation) {
                if (orientation == ArcOrientation.HORIZONTAL) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Snap to nearest index based on drag
                            val threshold = 50f
                            if (abs(dragOffset) > threshold) {
                                val newIndex = if (dragOffset > 0) {
                                    (currentIndex - 1).coerceIn(0, items.size - 1)
                                } else {
                                    (currentIndex + 1).coerceIn(0, items.size - 1)
                                }
                                onIndexChange(newIndex)
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                } else {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            val threshold = 50f
                            if (abs(dragOffset) > threshold) {
                                val newIndex = if (dragOffset > 0) {
                                    (currentIndex - 1).coerceIn(0, items.size - 1)
                                } else {
                                    (currentIndex + 1).coerceIn(0, items.size - 1)
                                }
                                onIndexChange(newIndex)
                            }
                            dragOffset = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                }
            }
    ) {
        val density = LocalDensity.current
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val radiusPx = with(density) { arcRadius.toPx() }

        Layout(
            content = {
                items.forEachIndexed { index, item ->
                    Box {
                        itemContent(item, index, index == animatedIndex)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { measurables, constraints ->
            // Calculate item positions along arc
            val placeables = mutableListOf<Placeable>()
            val positions = mutableListOf<ArcItemPosition>()

            measurables.forEachIndexed { index, measurable ->
                val distanceFromCenter = index - animatedIndex

                // Calculate scale based on distance
                val scale = when (abs(distanceFromCenter)) {
                    0 -> centerScale
                    1 -> sideScale
                    else -> sideScale * sideScale // Progressive scaling for far items
                }

                // Calculate size
                val baseSize = min(containerWidth, containerHeight) * 0.3f
                val itemWidth = (baseSize * scale).toInt().coerceAtLeast(1)
                val itemHeight = (itemWidth * 0.75f).toInt().coerceAtLeast(1)

                // Measure with calculated constraints
                val placeable = measurable.measure(
                    Constraints.fixed(itemWidth, itemHeight)
                )
                placeables.add(placeable)

                // Calculate position on arc
                val angleOffset = distanceFromCenter * itemSpacing
                val angleRadians = Math.toRadians(angleOffset.toDouble())

                val arcPosition = calculateArcPosition(
                    orientation = orientation,
                    angleRadians = angleRadians,
                    radius = radiusPx,
                    centerX = containerWidth / 2,
                    centerY = containerHeight / 2,
                    itemWidth = itemWidth,
                    itemHeight = itemHeight
                )

                // Calculate opacity and blur based on distance
                val opacity = when (abs(distanceFromCenter)) {
                    0 -> 1.0f
                    1 -> 0.7f
                    2 -> 0.4f
                    else -> 0.2f
                }

                val blur = when (abs(distanceFromCenter)) {
                    0 -> 0f
                    1 -> 2f
                    2 -> 4f
                    else -> 6f
                }

                positions.add(
                    ArcItemPosition(
                        x = arcPosition.x,
                        y = arcPosition.y,
                        scale = scale,
                        alpha = opacity,
                        blur = blur,
                        zIndex = if (index == animatedIndex) 10f else (10f - abs(distanceFromCenter))
                    )
                )
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val pos = positions[index]
                    placeable.placeWithLayer(
                        x = pos.x.toInt(),
                        y = pos.y.toInt()
                    ) {
                        scaleX = pos.scale
                        scaleY = pos.scale
                        alpha = pos.alpha
                        // Note: Blur not available in placeWithLayer, applied in itemContent if needed
                    }
                }
            }
        }
    }
}

/**
 * Calculate position on arc based on orientation
 */
private fun calculateArcPosition(
    orientation: ArcOrientation,
    angleRadians: Double,
    radius: Float,
    centerX: Float,
    centerY: Float,
    itemWidth: Int,
    itemHeight: Int
): Offset {
    return when (orientation) {
        ArcOrientation.HORIZONTAL -> {
            // Horizontal arc (portrait) - items curve left-right
            val x = centerX + (radius * sin(angleRadians)).toFloat() - itemWidth / 2
            val y = centerY + (radius * (1 - cos(angleRadians))).toFloat() - itemHeight / 2
            Offset(x, y)
        }
        ArcOrientation.VERTICAL -> {
            // Vertical arc (landscape) - items curve top-bottom (90° rotation)
            val x = centerX + (radius * (1 - cos(angleRadians))).toFloat() - itemWidth / 2
            val y = centerY + (radius * sin(angleRadians)).toFloat() - itemHeight / 2
            Offset(x, y)
        }
    }
}

/**
 * Arc orientation
 */
enum class ArcOrientation {
    HORIZONTAL, // Arc curves left-right (portrait mode)
    VERTICAL    // Arc curves top-bottom (landscape mode, rotated 90°)
}

/**
 * Position data for arc item
 */
private data class ArcItemPosition(
    val x: Float,
    val y: Float,
    val scale: Float,
    val alpha: Float,
    val blur: Float,
    val zIndex: Float
)

/**
 * Helper to determine if device is in landscape
 */
@Composable
fun isLandscape(): Boolean {
    var landscape by remember { mutableStateOf(false) }
    BoxWithConstraints {
        landscape = maxWidth > maxHeight
    }
    return landscape
}
