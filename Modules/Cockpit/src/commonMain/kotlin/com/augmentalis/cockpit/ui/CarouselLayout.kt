package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.CockpitFrame
import kotlin.math.absoluteValue

/**
 * Carousel layout — swipe-through with pseudo-3D perspective.
 *
 * Center frame renders full-size, adjacent frames are scaled down and
 * rotated along the Y-axis for a curved carousel effect. Frame number
 * badge shows "Frame N of M" at the top.
 *
 * Uses [HorizontalPager] from Compose Foundation for gesture handling
 * and settling animation.
 */
@Composable
fun CarouselLayout(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    if (frames.isEmpty()) return

    val colors = AvanueTheme.colors
    val initialPage = frames.indexOfFirst { it.id == selectedFrameId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { frames.size }

    // Notify parent when page settles on a new frame
    LaunchedEffect(pagerState.settledPage) {
        val settledFrame = frames.getOrNull(pagerState.settledPage)
        if (settledFrame != null && settledFrame.id != selectedFrameId) {
            onFrameSelected(settledFrame.id)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Frame counter badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Frame ${pagerState.currentPage + 1} of ${frames.size}",
                color = colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        colors.surface.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Carousel pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            pageSpacing = 8.dp,
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val frame = frames[pageIndex]

            // Calculate page offset for perspective transform
            val pageOffset = ((pagerState.currentPage - pageIndex) +
                    pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Scale: center=1.0, adjacent=CAROUSEL_ADJACENT_SCALE
                        val scale = lerp(
                            start = 1f,
                            stop = CockpitConstants.CAROUSEL_ADJACENT_SCALE,
                            fraction = pageOffset.coerceIn(0f, 1f)
                        )
                        scaleX = scale
                        scaleY = scale

                        // Y-axis rotation for 3D perspective
                        rotationY = lerp(
                            start = 0f,
                            stop = CockpitConstants.CAROUSEL_ROTATION_DEGREES,
                            fraction = pageOffset.coerceIn(0f, 1f)
                        ) * if (pagerState.currentPage > pageIndex) 1f else -1f

                        // Alpha fade for adjacent pages
                        alpha = lerp(
                            start = 1f,
                            stop = CockpitConstants.CAROUSEL_ADJACENT_ALPHA,
                            fraction = pageOffset.coerceIn(0f, 1f)
                        )
                    }
            ) {
                FrameWindow(
                    frame = frame,
                    isSelected = pageIndex == pagerState.currentPage,
                    isDraggable = false,
                    isResizable = false,
                    frameNumber = pageIndex + 1,
                    onSelect = { onFrameSelected(frame.id) },
                    onClose = { onFrameClose(frame.id) },
                    onMinimize = { onFrameMinimize(frame.id) },
                    onMaximize = { onFrameMaximize(frame.id) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    frameContent(frame)
                }
            }
        }
    }
}
