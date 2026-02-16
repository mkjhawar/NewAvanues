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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.FrameContent

/**
 * Individual window chrome wrapping frame content.
 * Provides title bar with content type icon, window controls (minimize, maximize, close),
 * drag gesture on title bar (for freeform), and resize handle at bottom-right.
 */
@Composable
fun FrameWindow(
    frame: CockpitFrame,
    isSelected: Boolean,
    isDraggable: Boolean = false,
    isResizable: Boolean = false,
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
    val borderColor = if (isSelected) colors.primary else colors.outline
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
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            onSelect()
                        }
                    }
                }
            }
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

            // Content type indicator
            Text(
                text = contentTypeLabel(frame.content),
                color = colors.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.width(6.dp))

            // Title
            Text(
                text = frame.title.ifBlank { "Untitled" },
                color = colors.onSurface.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Window controls
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Minimize,
                    "Minimize",
                    tint = colors.onSurface.copy(alpha = 0.5f),
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
                    tint = colors.onSurface.copy(alpha = 0.5f),
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
                        tint = colors.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(12.dp).align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Returns a short label for the content type (shown in the title bar).
 */
private fun contentTypeLabel(content: FrameContent): String = when (content) {
    is FrameContent.Web -> "WEB"
    is FrameContent.Pdf -> "PDF"
    is FrameContent.Image -> "IMG"
    is FrameContent.Video -> "VID"
    is FrameContent.Note -> "NOTE"
    is FrameContent.Camera -> "CAM"
    is FrameContent.VoiceNote -> "MIC"
    is FrameContent.Form -> "FORM"
    is FrameContent.Signature -> "SIG"
    is FrameContent.Map -> "MAP"
    is FrameContent.Whiteboard -> "DRAW"
    is FrameContent.Terminal -> "TERM"
    is FrameContent.AiSummary -> "AI"
    is FrameContent.ScreenCast -> "CAST"
    is FrameContent.Widget -> "WIDGET"
}
