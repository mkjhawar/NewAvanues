package com.augmentalis.cockpit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.CockpitFrame

/**
 * Dice-5 layout — 4 corner windows + 1 large center.
 *
 * ```
 * ┌──────┬────────────┬──────┐
 * │  TL  │            │  TR  │
 * │      │            │      │
 * ├──────┤   CENTER   ├──────┤
 * │      │  (primary) │      │
 * │      │            │      │
 * ├──────┤            ├──────┤
 * │  BL  │            │  BR  │
 * │      │            │      │
 * └──────┴────────────┴──────┘
 * ```
 *
 * - Center: [CockpitConstants.DICE_CENTER_WEIGHT] of width, full height
 * - Corners: [CockpitConstants.DICE_CORNER_WEIGHT] of width, 50% height each
 * - Tapping a corner swaps it with center
 * - Works with 1-5 frames (empty slots are not rendered)
 */
@Composable
fun SpatialDiceLayout(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    onSwapWithCenter: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    if (frames.isEmpty()) return

    // Determine center frame (selected or first)
    val centerFrame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.first()
    val cornerFrames = remember(frames, centerFrame.id) {
        frames.filter { it.id != centerFrame.id }.take(4)
    }

    // Split corners: top-left, top-right, bottom-left, bottom-right
    val topLeft = cornerFrames.getOrNull(0)
    val topRight = cornerFrames.getOrNull(1)
    val bottomLeft = cornerFrames.getOrNull(2)
    val bottomRight = cornerFrames.getOrNull(3)

    Row(modifier = modifier.fillMaxSize().padding(4.dp)) {
        // Left column (corners TL + BL)
        if (topLeft != null || bottomLeft != null) {
            Column(
                modifier = Modifier
                    .weight(CockpitConstants.DICE_CORNER_WEIGHT / 2f)
            ) {
                if (topLeft != null) {
                    FrameWindow(
                        frame = topLeft,
                        isSelected = false,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onSwapWithCenter(topLeft.id) },
                        onClose = { onFrameClose(topLeft.id) },
                        onMinimize = { onFrameMinimize(topLeft.id) },
                        onMaximize = { onFrameMaximize(topLeft.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(2.dp)
                    ) { frameContent(topLeft) }
                }

                if (bottomLeft != null) {
                    FrameWindow(
                        frame = bottomLeft,
                        isSelected = false,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onSwapWithCenter(bottomLeft.id) },
                        onClose = { onFrameClose(bottomLeft.id) },
                        onMinimize = { onFrameMinimize(bottomLeft.id) },
                        onMaximize = { onFrameMaximize(bottomLeft.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(2.dp)
                    ) { frameContent(bottomLeft) }
                }
            }
        }

        // Center column (primary frame)
        FrameWindow(
            frame = centerFrame,
            isSelected = true,
            isDraggable = false,
            isResizable = false,
            onSelect = { onFrameSelected(centerFrame.id) },
            onClose = { onFrameClose(centerFrame.id) },
            onMinimize = { onFrameMinimize(centerFrame.id) },
            onMaximize = { onFrameMaximize(centerFrame.id) },
            modifier = Modifier
                .weight(CockpitConstants.DICE_CENTER_WEIGHT)
                .fillMaxSize()
                .padding(2.dp)
        ) { frameContent(centerFrame) }

        // Right column (corners TR + BR)
        if (topRight != null || bottomRight != null) {
            Column(
                modifier = Modifier
                    .weight(CockpitConstants.DICE_CORNER_WEIGHT / 2f)
            ) {
                if (topRight != null) {
                    FrameWindow(
                        frame = topRight,
                        isSelected = false,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onSwapWithCenter(topRight.id) },
                        onClose = { onFrameClose(topRight.id) },
                        onMinimize = { onFrameMinimize(topRight.id) },
                        onMaximize = { onFrameMaximize(topRight.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(2.dp)
                    ) { frameContent(topRight) }
                }

                if (bottomRight != null) {
                    FrameWindow(
                        frame = bottomRight,
                        isSelected = false,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onSwapWithCenter(bottomRight.id) },
                        onClose = { onFrameClose(bottomRight.id) },
                        onMinimize = { onFrameMinimize(bottomRight.id) },
                        onMaximize = { onFrameMaximize(bottomRight.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(2.dp)
                    ) { frameContent(bottomRight) }
                }
            }
        }
    }
}
