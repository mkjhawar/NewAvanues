package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.CockpitFrame

/**
 * Triptych layout — three-panel "book spread" with angled side wings.
 *
 * ```
 * ┌─────────┐  ┌──────────────────┐  ┌─────────┐
 * │         ╱  │                  │  ╲         │
 * │  LEFT  ╱   │     CENTER       │   ╲ RIGHT  │
 * │       ╱    │    (elevated)    │    ╲       │
 * │      ╱     │                  │     ╲      │
 * └─────╱      └──────────────────┘      ╲─────┘
 *  rotY +16°                          rotY -16°
 * ```
 *
 * - Left wing: [CockpitConstants.TRIPTYCH_LEFT_WEIGHT] weight, rotateY(+16deg), origin right-center
 * - Center:    [CockpitConstants.TRIPTYCH_CENTER_WEIGHT] weight, slight scale-up + shadow for depth
 * - Right wing: [CockpitConstants.TRIPTYCH_RIGHT_WEIGHT] weight, rotateY(-16deg), origin left-center
 * - Glass-styled panels with rounded corners and semi-transparent surface
 *
 * Adapts to frame count:
 * - 1 frame: center only (full weight)
 * - 2 frames: left + center (no right wing)
 * - 3+ frames: left + center + right (selected frame goes to center)
 *
 * @param frames All visible frames in the session.
 * @param selectedFrameId Currently focused/selected frame ID.
 * @param onFrameSelected Called when user taps/focuses a frame.
 * @param onFrameClose Called when user closes a frame.
 * @param onFrameMinimize Called when user minimizes a frame.
 * @param onFrameMaximize Called when user maximizes/restores a frame.
 * @param frameContent Composable slot that renders the actual content for a given frame.
 * @param modifier Layout modifier.
 */
@Composable
fun TriptychLayout(
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

    val panelShape = RoundedCornerShape(14.dp)
    val panelBackground = AvanueTheme.colors.surface.copy(alpha = 0.3f)
    val rotationDeg = CockpitConstants.TRIPTYCH_ROTATION_DEGREES

    // Determine center frame (selected or frame[1] if available, else first)
    val centerFrame: CockpitFrame
    val leftFrame: CockpitFrame?
    val rightFrame: CockpitFrame?

    when {
        frames.size == 1 -> {
            centerFrame = frames[0]
            leftFrame = null
            rightFrame = null
        }
        frames.size == 2 -> {
            // Two frames: first goes left, second goes center
            leftFrame = frames[0]
            centerFrame = frames.firstOrNull { it.id == selectedFrameId }
                ?.let { selected ->
                    // If selected is the left frame, keep layout stable
                    if (selected.id == frames[0].id) frames[1] else selected
                }
                ?: frames[1]
            rightFrame = null
        }
        else -> {
            // 3+ frames: selected (or frame[1]) goes center, frame[0] goes left, next remaining goes right
            val selected = frames.firstOrNull { it.id == selectedFrameId }
            centerFrame = selected ?: frames[1]
            val remaining = remember(frames, centerFrame.id) {
                frames.filter { it.id != centerFrame.id }
            }
            leftFrame = remaining.getOrNull(0)
            rightFrame = remaining.getOrNull(1)
        }
    }

    Row(
        modifier = modifier.fillMaxSize().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left Wing ──────────────────────────────────────────
        if (leftFrame != null) {
            Box(
                modifier = Modifier
                    .weight(CockpitConstants.TRIPTYCH_LEFT_WEIGHT)
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotationDeg
                        // transform-origin: right center
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                        cameraDistance = 12f * density
                    }
                    .clip(panelShape)
                    .background(panelBackground)
            ) {
                FrameWindow(
                    frame = leftFrame,
                    isSelected = leftFrame.id == selectedFrameId,
                    isDraggable = false,
                    isResizable = false,
                    onSelect = { onFrameSelected(leftFrame.id) },
                    onClose = { onFrameClose(leftFrame.id) },
                    onMinimize = { onFrameMinimize(leftFrame.id) },
                    onMaximize = { onFrameMaximize(leftFrame.id) },
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                ) { frameContent(leftFrame) }
            }
        }

        // ── Center Panel (elevated) ────────────────────────────
        Box(
            modifier = Modifier
                .weight(
                    if (leftFrame == null && rightFrame == null) 1f
                    else CockpitConstants.TRIPTYCH_CENTER_WEIGHT
                )
                .fillMaxSize()
                .shadow(8.dp, panelShape)
                .graphicsLayer {
                    // Slight scale-up simulates translateZ(14px) depth
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
                .clip(panelShape)
                .background(panelBackground)
        ) {
            FrameWindow(
                frame = centerFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = { onFrameSelected(centerFrame.id) },
                onClose = { onFrameClose(centerFrame.id) },
                onMinimize = { onFrameMinimize(centerFrame.id) },
                onMaximize = { onFrameMaximize(centerFrame.id) },
                modifier = Modifier.fillMaxSize().padding(2.dp)
            ) { frameContent(centerFrame) }
        }

        // ── Right Wing ─────────────────────────────────────────
        if (rightFrame != null) {
            Box(
                modifier = Modifier
                    .weight(CockpitConstants.TRIPTYCH_RIGHT_WEIGHT)
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = -rotationDeg
                        // transform-origin: left center
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        cameraDistance = 12f * density
                    }
                    .clip(panelShape)
                    .background(panelBackground)
            ) {
                FrameWindow(
                    frame = rightFrame,
                    isSelected = rightFrame.id == selectedFrameId,
                    isDraggable = false,
                    isResizable = false,
                    onSelect = { onFrameSelected(rightFrame.id) },
                    onClose = { onFrameClose(rightFrame.id) },
                    onMinimize = { onFrameMinimize(rightFrame.id) },
                    onMaximize = { onFrameMaximize(rightFrame.id) },
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                ) { frameContent(rightFrame) }
            }
        }
    }
}
