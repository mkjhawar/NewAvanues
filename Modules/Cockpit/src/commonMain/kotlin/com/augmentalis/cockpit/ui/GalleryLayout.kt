package com.augmentalis.cockpit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.LayoutMode

/**
 * Gallery layout — media-only filtered grid.
 *
 * Filters frames to only show media content types (image, video, camera,
 * screen cast) as defined by [LayoutMode.GALLERY_CONTENT_TYPES].
 * Non-media frames are hidden but NOT removed — they're restored when
 * switching away from gallery mode.
 *
 * Responsive grid adapts column count based on available width using
 * [GridCells.Adaptive] with a minimum cell width of 180dp.
 */
@Composable
fun GalleryLayout(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    // Filter to media-only content types
    val mediaFrames = remember(frames) {
        frames.filter { it.contentType in LayoutMode.GALLERY_CONTENT_TYPES }
    }

    if (mediaFrames.isEmpty()) {
        // Empty state — no media frames available
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Media Frames",
                    color = colors.textPrimary.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Add image, video, camera, or cast frames to use Gallery mode",
                    color = colors.textPrimary.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    // Responsive grid with adaptive columns
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        items(mediaFrames, key = { it.id }) { frame ->
            FrameWindow(
                frame = frame,
                isSelected = frame.id == selectedFrameId,
                isDraggable = false,
                isResizable = false,
                onSelect = { onFrameSelected(frame.id) },
                onClose = { onFrameClose(frame.id) },
                onMinimize = { onFrameMinimize(frame.id) },
                onMaximize = { onFrameMaximize(frame.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                frameContent(frame)
            }
        }
    }
}
