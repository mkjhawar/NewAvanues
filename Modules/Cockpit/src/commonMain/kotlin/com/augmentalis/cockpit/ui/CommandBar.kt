package com.augmentalis.cockpit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Maximize
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.CommandBarState
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.LayoutMode
import kotlinx.coroutines.delay

/**
 * Context-aware command bar docked at the bottom of CockpitScreen.
 *
 * Shows action chips that change based on the current [CommandBarState].
 * - MAIN: Add Frame, Layout, Frame Actions
 * - ADD_FRAME: Content type options (Web, PDF, Image, etc.)
 * - LAYOUT_PICKER: All layout modes as selectable chips
 * - FRAME_ACTIONS: Minimize, Maximize, Close, content-specific
 *
 * Back button navigates to [CommandBarState.parent]. State transitions
 * are animated with slide + fade.
 */
@Composable
fun CommandBar(
    state: CommandBarState,
    currentLayoutMode: LayoutMode,
    onStateChange: (CommandBarState) -> Unit,
    onLayoutSelected: (LayoutMode) -> Unit,
    onAddFrame: (FrameContent, String) -> Unit,
    onFrameMinimize: () -> Unit,
    onFrameMaximize: () -> Unit,
    onFrameClose: () -> Unit,
    onCommandDispatched: (String) -> Unit = {},
    availableLayoutModes: List<LayoutMode> = LayoutMode.entries,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val barHeight = CockpitConstants.COMMAND_BAR_HEIGHT.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.9f))
    ) {
        // Command chips — animated state transitions
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (slideInVertically { it / 4 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 4 } + fadeOut())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
        ) { currentState ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (non-root states)
                if (currentState.parent != null) {
                    CommandChip(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Back",
                        isActive = false,
                        onClick = { onStateChange(currentState.parent!!) }
                    )
                }

                // Render chips based on current state
                when (currentState) {
                    CommandBarState.MAIN -> {
                        CommandChip(Icons.Default.Add, "Add Frame", false) {
                            onStateChange(CommandBarState.ADD_FRAME)
                        }
                        CommandChip(layoutModeIcon(currentLayoutMode), "Layout", false) {
                            onStateChange(CommandBarState.LAYOUT_PICKER)
                        }
                        CommandChip(Icons.Default.TouchApp, "Frame", false) {
                            onStateChange(CommandBarState.FRAME_ACTIONS)
                        }
                    }

                    CommandBarState.ADD_FRAME -> {
                        addFrameOptions().forEach { (label, content) ->
                            val icon = contentTypeIcon(content)
                            CommandChip(icon, label, false) {
                                onAddFrame(content, label)
                                onStateChange(CommandBarState.MAIN)
                            }
                        }
                    }

                    CommandBarState.LAYOUT_PICKER -> {
                        availableLayoutModes.forEach { mode ->
                            CommandChip(
                                icon = layoutModeIcon(mode),
                                label = layoutModeLabel(mode),
                                isActive = mode == currentLayoutMode,
                                onClick = {
                                    onLayoutSelected(mode)
                                    onStateChange(CommandBarState.MAIN)
                                }
                            )
                        }
                    }

                    CommandBarState.FRAME_ACTIONS -> {
                        CommandChip(Icons.Default.Minimize, "Minimize", false, onFrameMinimize)
                        CommandChip(Icons.Default.Fullscreen, "Maximize", false, onFrameMaximize)
                        CommandChip(Icons.Default.Close, "Close", false, onFrameClose)
                    }

                    CommandBarState.WEB_ACTIONS -> {
                        CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Back", false) { onCommandDispatched("cockpit.web.back") }
                        CommandChip(Icons.Default.Language, "Forward", false) { onCommandDispatched("cockpit.web.forward") }
                        CommandChip(Icons.Default.Language, "Refresh", false) { onCommandDispatched("cockpit.web.refresh") }
                        CommandChip(Icons.Default.ZoomIn, "Zoom In", false) { onCommandDispatched("cockpit.web.zoom_in") }
                        CommandChip(Icons.Default.ZoomOut, "Zoom Out", false) { onCommandDispatched("cockpit.web.zoom_out") }
                    }

                    CommandBarState.PDF_ACTIONS -> {
                        CommandChip(Icons.Default.ArrowUpward, "Prev Page", false) { onCommandDispatched("cockpit.pdf.prev_page") }
                        CommandChip(Icons.Default.ArrowDownward, "Next Page", false) { onCommandDispatched("cockpit.pdf.next_page") }
                        CommandChip(Icons.Default.ZoomIn, "Zoom In", false) { onCommandDispatched("cockpit.pdf.zoom_in") }
                        CommandChip(Icons.Default.ZoomOut, "Zoom Out", false) { onCommandDispatched("cockpit.pdf.zoom_out") }
                    }

                    CommandBarState.IMAGE_ACTIONS -> {
                        CommandChip(Icons.Default.ZoomIn, "Zoom In", false) { onCommandDispatched("cockpit.image.zoom_in") }
                        CommandChip(Icons.Default.ZoomOut, "Zoom Out", false) { onCommandDispatched("cockpit.image.zoom_out") }
                        CommandChip(Icons.Default.SwapHoriz, "Rotate", false) { onCommandDispatched("cockpit.image.rotate") }
                    }

                    CommandBarState.VIDEO_ACTIONS -> {
                        CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Rewind", false) { onCommandDispatched("cockpit.video.rewind") }
                        CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Play/Pause", false) { onCommandDispatched("cockpit.video.play_pause") }
                        CommandChip(Icons.Default.Fullscreen, "Fullscreen", false) { onCommandDispatched("cockpit.video.fullscreen") }
                    }

                    CommandBarState.NOTE_ACTIONS -> {
                        CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Undo", false) { onCommandDispatched("cockpit.note.undo") }
                        CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Redo", false) { onCommandDispatched("cockpit.note.redo") }
                    }

                    CommandBarState.CAMERA_ACTIONS -> {
                        CommandChip(Icons.Default.SwapHoriz, "Flip", false) { onCommandDispatched("cockpit.camera.flip") }
                        CommandChip(Icons.Default.TouchApp, "Capture", false) { onCommandDispatched("cockpit.camera.capture") }
                    }

                    CommandBarState.SCROLL_COMMANDS,
                    CommandBarState.ZOOM_COMMANDS,
                    CommandBarState.SPATIAL_COMMANDS -> {
                        // Auto-transition back to MAIN after brief display
                        LaunchedEffect(currentState) {
                            delay(1500)
                            onStateChange(CommandBarState.MAIN)
                        }
                        Text(
                            text = "Commands available via voice",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual action chip in the command bar.
 * Active state uses primary color, inactive uses surface.
 */
@Composable
private fun CommandChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    val chipShape = RoundedCornerShape(20.dp)
    val bgColor = if (isActive) colors.primary else colors.surface
    val contentColor = if (isActive) colors.onPrimary else colors.textPrimary.copy(alpha = 0.8f)
    val borderColor = if (isActive) colors.primary else colors.border

    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(chipShape)
            .border(1.dp, borderColor, chipShape)
            .background(bgColor, chipShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Voice: click $label"
                role = Role.Button
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Icon for each layout mode — used in CommandBar and layout picker.
 */
fun layoutModeIcon(mode: LayoutMode): ImageVector = when (mode) {
    LayoutMode.FREEFORM -> Icons.Default.Dashboard
    LayoutMode.GRID -> Icons.Default.GridView
    LayoutMode.SPLIT_LEFT, LayoutMode.SPLIT_RIGHT -> Icons.Default.Splitscreen
    LayoutMode.COCKPIT -> Icons.Default.ViewCarousel
    LayoutMode.T_PANEL -> Icons.Default.ViewColumn
    LayoutMode.MOSAIC -> Icons.Default.Dashboard
    LayoutMode.FULLSCREEN -> Icons.Default.Fullscreen
    LayoutMode.WORKFLOW -> Icons.Default.ViewColumn
    LayoutMode.ROW -> Icons.Default.ViewColumn
    LayoutMode.CAROUSEL -> Icons.Default.ViewCarousel
    LayoutMode.SPATIAL_DICE -> Icons.Default.Casino
    LayoutMode.GALLERY -> Icons.Default.PhotoLibrary
}

/**
 * Human-readable label for each layout mode.
 */
fun layoutModeLabel(mode: LayoutMode): String = when (mode) {
    LayoutMode.FREEFORM -> "Freeform"
    LayoutMode.GRID -> "Grid"
    LayoutMode.SPLIT_LEFT -> "Split Left"
    LayoutMode.SPLIT_RIGHT -> "Split Right"
    LayoutMode.COCKPIT -> "Flight Deck"
    LayoutMode.T_PANEL -> "T-Panel"
    LayoutMode.MOSAIC -> "Mosaic"
    LayoutMode.FULLSCREEN -> "Fullscreen"
    LayoutMode.WORKFLOW -> "Workflow"
    LayoutMode.ROW -> "Row"
    LayoutMode.CAROUSEL -> "Carousel"
    LayoutMode.SPATIAL_DICE -> "Dice-5"
    LayoutMode.GALLERY -> "Gallery"
}

/**
 * Content type options for the ADD_FRAME state.
 */
fun addFrameOptions(): List<Pair<String, FrameContent>> = listOf(
    "Web" to FrameContent.Web(),
    "PDF" to FrameContent.Pdf(),
    "Image" to FrameContent.Image(),
    "Video" to FrameContent.Video(),
    "Note" to FrameContent.Note(),
    "Camera" to FrameContent.Camera(),
    "Voice Note" to FrameContent.VoiceNote(),
    "Whiteboard" to FrameContent.Whiteboard(),
    "Signature" to FrameContent.Signature(),
    "Screen Cast" to FrameContent.ScreenCast(),
    "External App" to FrameContent.ExternalApp()
)
