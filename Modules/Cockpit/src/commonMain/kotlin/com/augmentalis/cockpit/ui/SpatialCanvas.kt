package com.augmentalis.cockpit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.spatial.SpatialViewportController

/**
 * Spatial canvas that applies viewport translation to its content.
 *
 * The canvas is a large virtual surface that can be panned by:
 * - Head tracking via [SpatialViewportController] (glasses/headset)
 * - Touch drag gestures (phone/tablet fallback)
 *
 * The viewport offset is applied via `graphicsLayer { translationX, translationY }`
 * for hardware-accelerated smooth movement.
 *
 * Features:
 * - Lock/unlock FAB (bottom-right)
 * - Center view FAB (bottom-left)
 * - Edge indicators showing content direction when viewport is offset
 *
 * @param controller The viewport controller providing offset data
 * @param enableTouchFallback Whether touch drag should pan the canvas (phone/tablet)
 * @param content The composable content to render inside the spatial viewport
 */
@Composable
fun SpatialCanvas(
    controller: SpatialViewportController,
    enableTouchFallback: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val viewportOffset by controller.viewportOffset.collectAsState()
    val isLocked by controller.isLocked.collectAsState()
    val colors = AvanueTheme.colors
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        // Spatial content area with viewport translation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enableTouchFallback && !isLocked) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                controller.applyManualOffset(dragAmount.x, dragAmount.y)
                            }
                        }
                    } else Modifier
                )
                .graphicsLayer {
                    translationX = viewportOffset.x
                    translationY = viewportOffset.y
                },
            content = content
        )

        // Lock/unlock FAB
        FloatingActionButton(
            onClick = { controller.toggleLock() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp),
            shape = CircleShape,
            containerColor = if (isLocked)
                colors.warning.copy(alpha = 0.9f)
            else
                colors.surface.copy(alpha = 0.8f),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp
            )
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (isLocked) "Unlock spatial" else "Lock spatial",
                tint = if (isLocked) colors.onPrimary else colors.textPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Center view FAB
        FloatingActionButton(
            onClick = { controller.centerView() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(40.dp),
            shape = CircleShape,
            containerColor = colors.surface.copy(alpha = 0.8f),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusWeak,
                contentDescription = "Center view",
                tint = colors.textPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Edge indicators — show chevrons when viewport is offset
        val edgeThreshold = 10f // Minimum px offset before showing indicator
        val indicatorColor = colors.primary.copy(alpha = 0.4f)

        // Left edge (content to the left — viewport panned right)
        AnimatedVisibility(
            visible = viewportOffset.x > edgeThreshold,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            EdgeIndicator(
                icon = Icons.Default.ChevronLeft,
                gradientDirection = EdgeDirection.LEFT,
                color = indicatorColor
            )
        }

        // Right edge (content to the right — viewport panned left)
        AnimatedVisibility(
            visible = viewportOffset.x < -edgeThreshold,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            EdgeIndicator(
                icon = Icons.Default.ChevronRight,
                gradientDirection = EdgeDirection.RIGHT,
                color = indicatorColor
            )
        }

        // Top edge (content above — viewport panned down)
        AnimatedVisibility(
            visible = viewportOffset.y > edgeThreshold,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            EdgeIndicator(
                icon = Icons.Default.KeyboardArrowUp,
                gradientDirection = EdgeDirection.TOP,
                color = indicatorColor
            )
        }

        // Bottom edge (content below — viewport panned up)
        AnimatedVisibility(
            visible = viewportOffset.y < -edgeThreshold,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EdgeIndicator(
                icon = Icons.Default.KeyboardArrowDown,
                gradientDirection = EdgeDirection.BOTTOM,
                color = indicatorColor
            )
        }
    }
}

/**
 * Direction for edge indicator gradient fade.
 */
private enum class EdgeDirection { LEFT, RIGHT, TOP, BOTTOM }

/**
 * Subtle edge indicator showing a chevron with a gradient fade from the edge.
 */
@Composable
private fun EdgeIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientDirection: EdgeDirection,
    color: Color,
    modifier: Modifier = Modifier
) {
    val gradientBrush = when (gradientDirection) {
        EdgeDirection.LEFT -> Brush.horizontalGradient(
            listOf(color.copy(alpha = 0.3f), Color.Transparent)
        )
        EdgeDirection.RIGHT -> Brush.horizontalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.3f))
        )
        EdgeDirection.TOP -> Brush.verticalGradient(
            listOf(color.copy(alpha = 0.3f), Color.Transparent)
        )
        EdgeDirection.BOTTOM -> Brush.verticalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.3f))
        )
    }

    val isHorizontal = gradientDirection == EdgeDirection.LEFT || gradientDirection == EdgeDirection.RIGHT

    Box(
        modifier = modifier
            .then(
                if (isHorizontal)
                    Modifier.width(32.dp).fillMaxHeight()
                else
                    Modifier.height(32.dp).fillMaxWidth()
            )
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Content in this direction",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}
