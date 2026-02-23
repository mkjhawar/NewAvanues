package com.augmentalis.cockpit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.DashboardState
import com.augmentalis.cockpit.model.LayoutMode

/**
 * Layout engine that switches rendering strategy based on the active [LayoutMode].
 * Each mode arranges the given frames differently within the available space.
 *
 * Supports 14 layout modes:
 * - [LayoutMode.DASHBOARD]: Home/launcher view with module tiles and recent sessions
 * - [LayoutMode.CAROUSEL]: Swipe-through with 3D perspective scaling
 * - [LayoutMode.SPATIAL_DICE]: 4 corners + 1 center (dice-5 pattern)
 * - [LayoutMode.GALLERY]: Media-only filtered responsive grid
 * - Plus 10 frame-based layouts (Freeform, Grid, Split, Cockpit, etc.)
 *
 * @param layoutMode Current layout mode for the session.
 * @param frames All frames in the session (sorted by z-order for freeform).
 * @param selectedFrameId Currently focused/selected frame ID.
 * @param onFrameSelected Called when user taps/focuses a frame.
 * @param onFrameMoved Called when a frame is dragged to a new position (freeform only).
 * @param onFrameResized Called when a frame is resized (freeform only).
 * @param onFrameClose Called when user closes a frame.
 * @param onFrameMinimize Called when user minimizes a frame.
 * @param onFrameMaximize Called when user maximizes/restores a frame.
 * @param frameContent Composable slot that renders the actual content for a given frame.
 * @param dashboardState State for Dashboard mode (recent sessions, modules, templates).
 * @param onModuleClick Called when a module tile is clicked in Dashboard mode.
 * @param onSessionClick Called when a recent session card is clicked in Dashboard mode.
 * @param onTemplateClick Called when a template tile is clicked in Dashboard mode.
 * @param modifier Layout modifier.
 */
@Composable
fun LayoutEngine(
    layoutMode: LayoutMode,
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameMoved: (String, Float, Float) -> Unit,
    onFrameResized: (String, Float, Float) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    dashboardState: DashboardState = DashboardState(),
    onModuleClick: (String) -> Unit = {},
    onSessionClick: (String) -> Unit = {},
    onTemplateClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visibleFrames = frames.filter { it.state.isVisible && !it.state.isMinimized }

    when (layoutMode) {
        LayoutMode.DASHBOARD -> DashboardLayout(
            dashboardState = dashboardState,
            onModuleClick = onModuleClick,
            onSessionClick = onSessionClick,
            onTemplateClick = onTemplateClick,
            modifier = modifier
        )

        LayoutMode.FREEFORM -> FreeformLayout(
            frames = frames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameMoved = onFrameMoved,
            onFrameResized = onFrameResized,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.GRID -> GridLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.SPLIT_LEFT, LayoutMode.SPLIT_RIGHT -> SplitLayout(
            frames = visibleFrames,
            splitLeft = layoutMode == LayoutMode.SPLIT_LEFT,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.COCKPIT -> FlightDeckLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.T_PANEL -> TPanelLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.MOSAIC -> MosaicLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.FULLSCREEN -> FullscreenLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.WORKFLOW -> WorkflowSidebar(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            displayProfile = AvanueTheme.displayProfile,
            onStepSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.ROW -> RowLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.CAROUSEL -> CarouselLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.SPATIAL_DICE -> SpatialDiceLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            onSwapWithCenter = { cornerFrameId ->
                // Swap = select the corner frame (makes it the new center)
                onFrameSelected(cornerFrameId)
            },
            frameContent = frameContent,
            modifier = modifier
        )

        LayoutMode.GALLERY -> GalleryLayout(
            frames = visibleFrames,
            selectedFrameId = selectedFrameId,
            onFrameSelected = onFrameSelected,
            onFrameClose = onFrameClose,
            onFrameMinimize = onFrameMinimize,
            onFrameMaximize = onFrameMaximize,
            frameContent = frameContent,
            modifier = modifier
        )
    }
}

/**
 * Freeform layout — each frame is absolutely positioned with drag/resize.
 * Delegates to [FreeformCanvas] for the actual gesture handling.
 */
@Composable
private fun FreeformLayout(
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
    FreeformCanvas(
        frames = frames,
        selectedFrameId = selectedFrameId,
        onFrameSelected = onFrameSelected,
        onFrameMoved = onFrameMoved,
        onFrameResized = onFrameResized,
        onFrameClose = onFrameClose,
        onFrameMinimize = onFrameMinimize,
        onFrameMaximize = onFrameMaximize,
        frameContent = frameContent,
        modifier = modifier
    )
}

/**
 * Grid layout — frames arranged in a responsive grid (adaptive columns).
 */
@Composable
private fun GridLayout(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnCount = when {
        frames.size <= 1 -> 1
        frames.size <= 4 -> 2
        else -> 3
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier.fillMaxSize().padding(4.dp)
    ) {
        items(frames, key = { it.id }) { frame ->
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
                    .then(
                        if (frames.size <= 2) Modifier.fillMaxSize()
                        else Modifier
                    )
            ) {
                frameContent(frame)
            }
        }
    }
}

/**
 * Split layout — primary frame on one side, remaining stacked on the other.
 */
@Composable
private fun SplitLayout(
    frames: List<CockpitFrame>,
    splitLeft: Boolean,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    if (frames.isEmpty()) return

    val primaryFrame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.first()
    val secondaryFrames = frames.filter { it.id != primaryFrame.id }

    Row(modifier = modifier.fillMaxSize().padding(4.dp)) {
        val primaryMod = Modifier.weight(0.6f).fillMaxSize().padding(4.dp)
        val secondaryMod = Modifier.weight(0.4f).fillMaxSize().padding(4.dp)

        if (splitLeft) {
            // Primary on left
            FrameWindow(
                frame = primaryFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = { onFrameSelected(primaryFrame.id) },
                onClose = { onFrameClose(primaryFrame.id) },
                onMinimize = { onFrameMinimize(primaryFrame.id) },
                onMaximize = { onFrameMaximize(primaryFrame.id) },
                modifier = primaryMod
            ) { frameContent(primaryFrame) }

            Column(modifier = secondaryMod) {
                secondaryFrames.forEach { frame ->
                    FrameWindow(
                        frame = frame,
                        isSelected = frame.id == selectedFrameId,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onFrameSelected(frame.id) },
                        onClose = { onFrameClose(frame.id) },
                        onMinimize = { onFrameMinimize(frame.id) },
                        onMaximize = { onFrameMaximize(frame.id) },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp)
                    ) { frameContent(frame) }
                }
            }
        } else {
            // Secondary on left
            Column(modifier = secondaryMod) {
                secondaryFrames.forEach { frame ->
                    FrameWindow(
                        frame = frame,
                        isSelected = frame.id == selectedFrameId,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onFrameSelected(frame.id) },
                        onClose = { onFrameClose(frame.id) },
                        onMinimize = { onFrameMinimize(frame.id) },
                        onMaximize = { onFrameMaximize(frame.id) },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp)
                    ) { frameContent(frame) }
                }
            }

            // Primary on right
            FrameWindow(
                frame = primaryFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = { onFrameSelected(primaryFrame.id) },
                onClose = { onFrameClose(primaryFrame.id) },
                onMinimize = { onFrameMinimize(primaryFrame.id) },
                onMaximize = { onFrameMaximize(primaryFrame.id) },
                modifier = primaryMod
            ) { frameContent(primaryFrame) }
        }
    }
}

/**
 * Flight Deck layout — mimics an airplane cockpit instrument panel.
 * Adapts from 1-6+ frames with top status strip, main PFD/ND pair,
 * and bottom instrument row.
 */
@Composable
private fun FlightDeckLayout(
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

    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
        when {
            frames.size == 1 -> {
                val frame = frames[0]
                DeckFrameWindow(
                    frame = frame,
                    isSelected = true,
                    selectedFrameId = selectedFrameId,
                    onFrameSelected = onFrameSelected,
                    onFrameClose = onFrameClose,
                    onFrameMinimize = onFrameMinimize,
                    onFrameMaximize = onFrameMaximize,
                    frameContent = frameContent,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(2.dp)
                )
            }

            frames.size == 2 -> {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    frames.forEach { frame ->
                        DeckFrameWindow(
                            frame = frame,
                            isSelected = frame.id == selectedFrameId,
                            selectedFrameId = selectedFrameId,
                            onFrameSelected = onFrameSelected,
                            onFrameClose = onFrameClose,
                            onFrameMinimize = onFrameMinimize,
                            onFrameMaximize = onFrameMaximize,
                            frameContent = frameContent,
                            modifier = Modifier.weight(1f).fillMaxSize().padding(2.dp)
                        )
                    }
                }
            }

            frames.size <= 4 -> {
                val mainFrames = frames.take(2)
                val bottomFrames = frames.drop(2)

                Row(modifier = Modifier.weight(0.65f).fillMaxWidth()) {
                    mainFrames.forEach { frame ->
                        DeckFrameWindow(
                            frame = frame,
                            isSelected = frame.id == selectedFrameId,
                            selectedFrameId = selectedFrameId,
                            onFrameSelected = onFrameSelected,
                            onFrameClose = onFrameClose,
                            onFrameMinimize = onFrameMinimize,
                            onFrameMaximize = onFrameMaximize,
                            frameContent = frameContent,
                            modifier = Modifier.weight(1f).fillMaxSize().padding(2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.weight(0.35f).fillMaxWidth()) {
                    bottomFrames.forEach { frame ->
                        DeckFrameWindow(
                            frame = frame,
                            isSelected = frame.id == selectedFrameId,
                            selectedFrameId = selectedFrameId,
                            onFrameSelected = onFrameSelected,
                            onFrameClose = onFrameClose,
                            onFrameMinimize = onFrameMinimize,
                            onFrameMaximize = onFrameMaximize,
                            frameContent = frameContent,
                            modifier = Modifier.weight(1f).fillMaxSize().padding(2.dp)
                        )
                    }
                }
            }

            else -> {
                val topFrame = frames[0]
                val mainFrames = frames.subList(1, 3)
                val bottomFrames = frames.drop(3).take(3)

                DeckFrameWindow(
                    frame = topFrame,
                    isSelected = topFrame.id == selectedFrameId,
                    selectedFrameId = selectedFrameId,
                    onFrameSelected = onFrameSelected,
                    onFrameClose = onFrameClose,
                    onFrameMinimize = onFrameMinimize,
                    onFrameMaximize = onFrameMaximize,
                    frameContent = frameContent,
                    modifier = Modifier.weight(0.18f).fillMaxWidth().padding(2.dp)
                )

                Row(modifier = Modifier.weight(0.52f).fillMaxWidth()) {
                    mainFrames.forEach { frame ->
                        DeckFrameWindow(
                            frame = frame,
                            isSelected = frame.id == selectedFrameId,
                            selectedFrameId = selectedFrameId,
                            onFrameSelected = onFrameSelected,
                            onFrameClose = onFrameClose,
                            onFrameMinimize = onFrameMinimize,
                            onFrameMaximize = onFrameMaximize,
                            frameContent = frameContent,
                            modifier = Modifier.weight(1f).fillMaxSize().padding(2.dp)
                        )
                    }
                }

                if (bottomFrames.isNotEmpty()) {
                    Row(modifier = Modifier.weight(0.30f).fillMaxWidth()) {
                        bottomFrames.forEach { frame ->
                            DeckFrameWindow(
                                frame = frame,
                                isSelected = frame.id == selectedFrameId,
                                selectedFrameId = selectedFrameId,
                                onFrameSelected = onFrameSelected,
                                onFrameClose = onFrameClose,
                                onFrameMinimize = onFrameMinimize,
                                onFrameMaximize = onFrameMaximize,
                                frameContent = frameContent,
                                modifier = Modifier.weight(1f).fillMaxSize().padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * T-Panel layout — primary frame takes 60% height on top,
 * secondary frames share the bottom 40% in a horizontal row.
 */
@Composable
private fun TPanelLayout(
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

    val primaryFrame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.first()
    val secondaryFrames = frames.filter { it.id != primaryFrame.id }

    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
        val primaryWeight = if (secondaryFrames.isEmpty()) 1f else 0.6f
        FrameWindow(
            frame = primaryFrame,
            isSelected = true,
            isDraggable = false,
            isResizable = false,
            onSelect = { onFrameSelected(primaryFrame.id) },
            onClose = { onFrameClose(primaryFrame.id) },
            onMinimize = { onFrameMinimize(primaryFrame.id) },
            onMaximize = { onFrameMaximize(primaryFrame.id) },
            modifier = Modifier.weight(primaryWeight).fillMaxWidth().padding(2.dp)
        ) { frameContent(primaryFrame) }

        if (secondaryFrames.isNotEmpty()) {
            Row(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
                secondaryFrames.forEach { frame ->
                    FrameWindow(
                        frame = frame,
                        isSelected = frame.id == selectedFrameId,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onFrameSelected(frame.id) },
                        onClose = { onFrameClose(frame.id) },
                        onMinimize = { onFrameMinimize(frame.id) },
                        onMaximize = { onFrameMaximize(frame.id) },
                        modifier = Modifier.weight(1f).fillMaxSize().padding(2.dp)
                    ) { frameContent(frame) }
                }
            }
        }
    }
}

/**
 * Mosaic layout — selected frame gets 50% area (left side),
 * remaining frames tile vertically on the right side.
 */
@Composable
private fun MosaicLayout(
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

    val primaryFrame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.first()
    val others = frames.filter { it.id != primaryFrame.id }

    if (others.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(4.dp)) {
            FrameWindow(
                frame = primaryFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = { onFrameSelected(primaryFrame.id) },
                onClose = { onFrameClose(primaryFrame.id) },
                onMinimize = { onFrameMinimize(primaryFrame.id) },
                onMaximize = { onFrameMaximize(primaryFrame.id) },
                modifier = Modifier.fillMaxSize().padding(2.dp)
            ) { frameContent(primaryFrame) }
        }
    } else {
        Row(modifier = modifier.fillMaxSize().padding(4.dp)) {
            FrameWindow(
                frame = primaryFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = { onFrameSelected(primaryFrame.id) },
                onClose = { onFrameClose(primaryFrame.id) },
                onMinimize = { onFrameMinimize(primaryFrame.id) },
                onMaximize = { onFrameMaximize(primaryFrame.id) },
                modifier = Modifier.weight(1f).fillMaxSize().padding(2.dp)
            ) { frameContent(primaryFrame) }

            Column(modifier = Modifier.weight(1f)) {
                others.forEach { frame ->
                    FrameWindow(
                        frame = frame,
                        isSelected = frame.id == selectedFrameId,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = { onFrameSelected(frame.id) },
                        onClose = { onFrameClose(frame.id) },
                        onMinimize = { onFrameMinimize(frame.id) },
                        onMaximize = { onFrameMaximize(frame.id) },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(2.dp)
                    ) { frameContent(frame) }
                }
            }
        }
    }
}

/**
 * Fullscreen layout — only the selected frame is visible, fills the screen.
 */
@Composable
private fun FullscreenLayout(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val frame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.firstOrNull() ?: return

    Box(modifier = modifier.fillMaxSize()) {
        frameContent(frame)
    }
}

/**
 * Row layout — frames arranged horizontally in equal-width columns.
 */
@Composable
private fun RowLayout(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize().padding(4.dp)) {
        frames.forEach { frame ->
            FrameWindow(
                frame = frame,
                isSelected = frame.id == selectedFrameId,
                isDraggable = false,
                isResizable = false,
                onSelect = { onFrameSelected(frame.id) },
                onClose = { onFrameClose(frame.id) },
                // RowLayout uses fixed equal-width columns — minimize/maximize not applicable.
                onMinimize = {},
                onMaximize = {},
                modifier = Modifier.weight(1f).fillMaxSize().padding(4.dp)
            ) { frameContent(frame) }
        }
    }
}

/**
 * Helper composable to reduce boilerplate in FlightDeckLayout.
 */
@Composable
private fun DeckFrameWindow(
    frame: CockpitFrame,
    isSelected: Boolean,
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    FrameWindow(
        frame = frame,
        isSelected = isSelected,
        isDraggable = false,
        isResizable = false,
        onSelect = { onFrameSelected(frame.id) },
        onClose = { onFrameClose(frame.id) },
        onMinimize = { onFrameMinimize(frame.id) },
        onMaximize = { onFrameMaximize(frame.id) },
        modifier = modifier
    ) {
        frameContent(frame)
    }
}
