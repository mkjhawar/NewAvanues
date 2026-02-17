package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.ContentAccent
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.FrameContent

/**
 * Individual window chrome wrapping frame content.
 *
 * Features:
 * - Title bar with content type icon, frame number, window controls
 * - Themed border color per content type (via [ContentAccent])
 * - Drag gesture on title bar (for freeform mode)
 * - Resize handle at bottom-right corner
 * - Spatial lock indicator when frame is positioned off-center
 * - Step number badge (for workflow mode)
 *
 * All colors use AvanueTheme.colors — works across all 32 theme combinations.
 */
@Composable
fun FrameWindow(
    frame: CockpitFrame,
    isSelected: Boolean,
    isDraggable: Boolean = false,
    isResizable: Boolean = false,
    frameNumber: Int? = null,
    onSelect: () -> Unit = {},
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onResize: (Float, Float) -> Unit = { _, _ -> },
    stepNumber: Int? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val colors = AvanueTheme.colors
    val accentColor = resolveAccentColor(frame.accent)
    val borderColor = if (isSelected) accentColor else colors.border
    val titleBarColor = if (isSelected) colors.surface else colors.background
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .background(colors.surface, shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelect() }
    ) {
        // Title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(titleBarColor)
                .then(
                    if (isDraggable) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(
                                    dragAmount.x / density.density,
                                    dragAmount.y / density.density
                                )
                            }
                        }
                    } else Modifier
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number badge (workflow mode)
            if (stepNumber != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(colors.primary, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = colors.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            // Frame number badge
            if (frameNumber != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(colors.primaryContainer, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = frameNumber.toString(),
                        color = colors.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            // Content type icon
            Icon(
                imageVector = contentTypeIcon(frame.content),
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )

            Spacer(Modifier.width(4.dp))

            // Title
            Text(
                text = frame.title.ifBlank { "Untitled" },
                color = colors.textPrimary.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Spatial lock indicator
            if (frame.isSpatiallyLocked) {
                val arrowIcon = spatialDirectionIcon(frame.spatialPosition.gridX, frame.spatialPosition.gridY)
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = "Locked: ${frame.spatialPosition.label}",
                    tint = colors.warning,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(2.dp))
            }

            // Window controls
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Minimize,
                    "Minimize",
                    tint = colors.textPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onMaximize,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    if (frame.state.isMaximized) Icons.Default.FullscreenExit
                    else Icons.Default.Fullscreen,
                    if (frame.state.isMaximized) "Restore" else "Maximize",
                    tint = colors.textPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "Close",
                    tint = colors.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()

            // Resize handle (bottom-right corner)
            if (isResizable) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onResize(
                                    dragAmount.x / density.density,
                                    dragAmount.y / density.density
                                )
                            }
                        }
                ) {
                    Icon(
                        Icons.Default.CropFree,
                        "Resize",
                        tint = colors.textPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(12.dp).align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Resolves a [ContentAccent] to an actual Color using the current AvanueTheme.
 */
@Composable
fun resolveAccentColor(accent: ContentAccent): Color {
    val colors = AvanueTheme.colors
    return when (accent) {
        ContentAccent.INFO -> colors.info
        ContentAccent.ERROR -> colors.error
        ContentAccent.PRIMARY -> colors.primary
        ContentAccent.SECONDARY -> colors.secondary
        ContentAccent.SUCCESS -> colors.success
        ContentAccent.WARNING -> colors.warning
        ContentAccent.TERTIARY -> colors.tertiary
    }
}

/**
 * Returns the Material icon for a content type.
 */
fun contentTypeIcon(content: FrameContent): ImageVector = when (content) {
    is FrameContent.Web -> Icons.Default.Language
    is FrameContent.Pdf -> Icons.Default.PictureAsPdf
    is FrameContent.Image -> Icons.Default.Image
    is FrameContent.Video -> Icons.Default.VideoLibrary
    is FrameContent.Note -> Icons.Default.StickyNote2
    is FrameContent.Camera -> Icons.Default.PhotoCamera
    is FrameContent.VoiceNote -> Icons.Default.Mic
    is FrameContent.Voice -> Icons.Default.VolumeUp
    is FrameContent.Form -> Icons.Default.Assignment
    is FrameContent.Signature -> Icons.Default.Draw
    is FrameContent.Map -> Icons.Default.Map
    is FrameContent.Whiteboard -> Icons.Default.Draw
    is FrameContent.Terminal -> Icons.Default.Terminal
    is FrameContent.AiSummary -> Icons.Default.AutoAwesome
    is FrameContent.ScreenCast -> Icons.Default.Cast
    is FrameContent.Widget -> Icons.Default.Widgets
}

/**
 * Returns a directional arrow icon for spatial position indication.
 */
private fun spatialDirectionIcon(gridX: Int, gridY: Int): ImageVector = when {
    gridX < 0 -> Icons.Default.KeyboardArrowLeft
    gridX > 0 -> Icons.Default.KeyboardArrowRight
    gridY < 0 -> Icons.Default.KeyboardArrowUp
    gridY > 0 -> Icons.Default.KeyboardArrowDown
    else -> Icons.Default.CropFree // center — shouldn't reach here if isSpatiallyLocked
}
