package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.CockpitFrame
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Freeform canvas that positions frame windows at absolute coordinates
 * within a container. Supports drag-to-move, resize handles, and
 * magnetic edge snapping.
 *
 * Windows are rendered in z-order (lowest first, highest on top).
 * The selected frame is always brought to the top.
 */
@Composable
fun FreeformCanvas(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameMoved: (String, Float, Float) -> Unit,
    onFrameResized: (String, Float, Float) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    // Sort frames by z-order so higher z-order renders on top
    val sortedFrames = remember(frames) {
        frames.sortedBy { it.state.zOrder }
    }

    // Calculate snap guides from all visible frames
    val snapEdges = remember(sortedFrames) {
        buildSnapEdges(sortedFrames, canvasWidth, canvasHeight)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
                canvasHeight = size.height.toFloat()
            }
    ) {
        sortedFrames.forEach { frame ->
            if (!frame.state.isVisible) return@forEach

            if (frame.state.isMinimized) {
                // Minimized frames are hidden from freeform — shown in taskbar
                return@forEach
            }

            if (frame.state.isMaximized) {
                // Maximized frame fills the entire canvas
                Box(modifier = Modifier.fillMaxSize()) {
                    FrameWindow(
                        frame = frame,
                        isSelected = frame.id == selectedFrameId,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onFrameSelected(frame.id) },
                        onClose = { onFrameClose(frame.id) },
                        onMinimize = { onFrameMinimize(frame.id) },
                        onMaximize = { onFrameMaximize(frame.id) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        frameContent(frame)
                    }
                }
                return@forEach
            }

            // Normal freeform positioning
            val offsetX = frame.state.posX
            val offsetY = frame.state.posY
            val frameWidth = frame.state.width
            val frameHeight = frame.state.height

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            with(density) { offsetX.dp.roundToPx() },
                            with(density) { offsetY.dp.roundToPx() }
                        )
                    }
                    .size(
                        width = frameWidth.dp,
                        height = frameHeight.dp
                    )
            ) {
                FrameWindow(
                    frame = frame,
                    isSelected = frame.id == selectedFrameId,
                    isDraggable = true,
                    isResizable = true,
                    onSelect = { onFrameSelected(frame.id) },
                    onClose = { onFrameClose(frame.id) },
                    onMinimize = { onFrameMinimize(frame.id) },
                    onMaximize = { onFrameMaximize(frame.id) },
                    onDrag = { dx, dy ->
                        val newX = (frame.state.posX + dx).coerceIn(0f, (canvasWidth / density.density) - 60f)
                        val newY = (frame.state.posY + dy).coerceIn(0f, (canvasHeight / density.density) - 40f)

                        // Apply snap guides
                        val (snappedX, snappedY) = applySnap(
                            newX, newY, frameWidth, frameHeight,
                            snapEdges, CockpitConstants.SNAP_THRESHOLD_DP.toFloat(),
                            frame.id
                        )
                        onFrameMoved(frame.id, snappedX, snappedY)
                    },
                    onResize = { dw, dh ->
                        val newW = (frame.state.width + dw)
                            .coerceIn(CockpitConstants.MIN_FRAME_WIDTH_DP.toFloat(), CockpitConstants.MAX_FRAME_WIDTH_DP.toFloat())
                        val newH = (frame.state.height + dh)
                            .coerceIn(CockpitConstants.MIN_FRAME_HEIGHT_DP.toFloat(), CockpitConstants.MAX_FRAME_HEIGHT_DP.toFloat())
                        onFrameResized(frame.id, newW, newH)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    frameContent(frame)
                }
            }
        }

        // Minimized frames taskbar at bottom
        val minimizedFrames = frames.filter { it.state.isMinimized }
        if (minimizedFrames.isNotEmpty()) {
            MinimizedTaskbar(
                frames = minimizedFrames,
                onRestore = { frameId ->
                    onFrameMinimize(frameId) // toggle — ViewModel handles the restore
                },
                modifier = Modifier
                    .offset {
                        IntOffset(0, (canvasHeight - with(density) { 48.dp.toPx() }).roundToInt())
                    }
            )
        }
    }
}

/**
 * Snap edge data structure for magnetic snapping.
 */
data class SnapEdge(
    val frameId: String,
    val position: Float,
    val isHorizontal: Boolean // true = horizontal edge (y), false = vertical edge (x)
)

/**
 * Build snap edges from all visible frames plus canvas boundaries.
 */
private fun buildSnapEdges(
    frames: List<CockpitFrame>,
    canvasWidth: Float,
    canvasHeight: Float
): List<SnapEdge> {
    val edges = mutableListOf<SnapEdge>()

    // Canvas boundaries
    edges.add(SnapEdge("canvas", 0f, isHorizontal = false)) // left
    edges.add(SnapEdge("canvas", 0f, isHorizontal = true))  // top
    if (canvasWidth > 0) edges.add(SnapEdge("canvas", canvasWidth, isHorizontal = false))  // right
    if (canvasHeight > 0) edges.add(SnapEdge("canvas", canvasHeight, isHorizontal = true)) // bottom

    // Frame edges
    frames.filter { it.state.isVisible && !it.state.isMinimized && !it.state.isMaximized }.forEach { frame ->
        val s = frame.state
        // Vertical edges (left and right of frame)
        edges.add(SnapEdge(frame.id, s.posX, isHorizontal = false))
        edges.add(SnapEdge(frame.id, s.posX + s.width, isHorizontal = false))
        // Horizontal edges (top and bottom of frame)
        edges.add(SnapEdge(frame.id, s.posY, isHorizontal = true))
        edges.add(SnapEdge(frame.id, s.posY + s.height, isHorizontal = true))
    }

    return edges
}

/**
 * Apply magnetic snapping: if the frame's edge is within [threshold] of a snap edge,
 * snap to that edge position.
 */
private fun applySnap(
    x: Float, y: Float,
    width: Float, height: Float,
    snapEdges: List<SnapEdge>,
    threshold: Float,
    excludeFrameId: String
): Pair<Float, Float> {
    var snappedX = x
    var snappedY = y

    // Check vertical (x) snapping — left edge and right edge
    val verticalEdges = snapEdges.filter { !it.isHorizontal && it.frameId != excludeFrameId }
    for (edge in verticalEdges) {
        // Snap left edge
        if (abs(x - edge.position) < threshold) {
            snappedX = edge.position
            break
        }
        // Snap right edge
        if (abs((x + width) - edge.position) < threshold) {
            snappedX = edge.position - width
            break
        }
    }

    // Check horizontal (y) snapping — top edge and bottom edge
    val horizontalEdges = snapEdges.filter { it.isHorizontal && it.frameId != excludeFrameId }
    for (edge in horizontalEdges) {
        // Snap top edge
        if (abs(y - edge.position) < threshold) {
            snappedY = edge.position
            break
        }
        // Snap bottom edge
        if (abs((y + height) - edge.position) < threshold) {
            snappedY = edge.position - height
            break
        }
    }

    return snappedX to snappedY
}
