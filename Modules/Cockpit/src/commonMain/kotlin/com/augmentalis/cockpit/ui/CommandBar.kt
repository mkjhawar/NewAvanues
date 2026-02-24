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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.CockpitConstants
import com.augmentalis.cockpit.model.CommandBarState
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.LayoutMode

/**
 * Typed content action emitted by the CommandBar when the user taps
 * a content-specific chip (e.g. "Back" in WEB_ACTIONS, "Next Page" in PDF_ACTIONS).
 *
 * The platform layer (CockpitScreen) routes these to the appropriate content
 * renderer (WebViewContainer, PdfViewerState, etc.) for execution.
 */
enum class ContentAction {
    // Web
    WEB_BACK, WEB_FORWARD, WEB_REFRESH, WEB_ZOOM_IN, WEB_ZOOM_OUT,
    // PDF
    PDF_PREV_PAGE, PDF_NEXT_PAGE, PDF_ZOOM_IN, PDF_ZOOM_OUT,
    // Image
    IMAGE_ZOOM_IN, IMAGE_ZOOM_OUT, IMAGE_ROTATE,
    // Video
    VIDEO_REWIND, VIDEO_PLAY_PAUSE, VIDEO_FULLSCREEN,
    // Note (formatting + snapshot-based undo/redo)
    NOTE_BOLD, NOTE_ITALIC, NOTE_UNDERLINE, NOTE_STRIKETHROUGH,
    NOTE_UNDO, NOTE_REDO, NOTE_SAVE,
    // Camera
    CAMERA_FLIP, CAMERA_CAPTURE,
    // Whiteboard / Annotation
    WB_PEN, WB_HIGHLIGHTER, WB_ERASER, WB_UNDO, WB_REDO, WB_CLEAR,
}

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
 *
 * @param onContentAction Called when a content-specific action is triggered.
 *   The platform layer routes this to the appropriate content renderer.
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
    onContentAction: (ContentAction) -> Unit = {},
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
                        CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Back", false) {
                            onContentAction(ContentAction.WEB_BACK)
                        }
                        CommandChip(Icons.AutoMirrored.Filled.ArrowForward, "Forward", false) {
                            onContentAction(ContentAction.WEB_FORWARD)
                        }
                        CommandChip(Icons.Default.Language, "Refresh", false) {
                            onContentAction(ContentAction.WEB_REFRESH)
                        }
                        CommandChip(Icons.Default.ZoomIn, "Zoom In", false) {
                            onContentAction(ContentAction.WEB_ZOOM_IN)
                        }
                        CommandChip(Icons.Default.ZoomOut, "Zoom Out", false) {
                            onContentAction(ContentAction.WEB_ZOOM_OUT)
                        }
                    }

                    CommandBarState.PDF_ACTIONS -> {
                        CommandChip(Icons.Default.ArrowUpward, "Prev Page", false) {
                            onContentAction(ContentAction.PDF_PREV_PAGE)
                        }
                        CommandChip(Icons.Default.ArrowDownward, "Next Page", false) {
                            onContentAction(ContentAction.PDF_NEXT_PAGE)
                        }
                        CommandChip(Icons.Default.ZoomIn, "Zoom In", false) {
                            onContentAction(ContentAction.PDF_ZOOM_IN)
                        }
                        CommandChip(Icons.Default.ZoomOut, "Zoom Out", false) {
                            onContentAction(ContentAction.PDF_ZOOM_OUT)
                        }
                    }

                    CommandBarState.IMAGE_ACTIONS -> {
                        CommandChip(Icons.Default.ZoomIn, "Zoom In", false) {
                            onContentAction(ContentAction.IMAGE_ZOOM_IN)
                        }
                        CommandChip(Icons.Default.ZoomOut, "Zoom Out", false) {
                            onContentAction(ContentAction.IMAGE_ZOOM_OUT)
                        }
                        CommandChip(Icons.Default.SwapHoriz, "Rotate", false) {
                            onContentAction(ContentAction.IMAGE_ROTATE)
                        }
                    }

                    CommandBarState.VIDEO_ACTIONS -> {
                        CommandChip(Icons.Default.FastRewind, "Rewind", false) {
                            onContentAction(ContentAction.VIDEO_REWIND)
                        }
                        CommandChip(Icons.Default.PlayArrow, "Play/Pause", false) {
                            onContentAction(ContentAction.VIDEO_PLAY_PAUSE)
                        }
                        CommandChip(Icons.Default.Fullscreen, "Fullscreen", false) {
                            onContentAction(ContentAction.VIDEO_FULLSCREEN)
                        }
                    }

                    CommandBarState.NOTE_ACTIONS -> {
                        CommandChip(Icons.Default.FormatBold, "Bold", false) {
                            onContentAction(ContentAction.NOTE_BOLD)
                        }
                        CommandChip(Icons.Default.FormatItalic, "Italic", false) {
                            onContentAction(ContentAction.NOTE_ITALIC)
                        }
                        CommandChip(Icons.Default.FormatUnderlined, "Underline", false) {
                            onContentAction(ContentAction.NOTE_UNDERLINE)
                        }
                        CommandChip(Icons.Default.FormatStrikethrough, "Strikethrough", false) {
                            onContentAction(ContentAction.NOTE_STRIKETHROUGH)
                        }
                        CommandChip(Icons.Default.Undo, "Undo", false) {
                            onContentAction(ContentAction.NOTE_UNDO)
                        }
                        CommandChip(Icons.Default.Redo, "Redo", false) {
                            onContentAction(ContentAction.NOTE_REDO)
                        }
                        CommandChip(Icons.Default.Save, "Save", false) {
                            onContentAction(ContentAction.NOTE_SAVE)
                        }
                    }

                    CommandBarState.CAMERA_ACTIONS -> {
                        CommandChip(Icons.Default.SwapHoriz, "Flip", false) {
                            onContentAction(ContentAction.CAMERA_FLIP)
                        }
                        CommandChip(Icons.Default.TouchApp, "Capture", false) {
                            onContentAction(ContentAction.CAMERA_CAPTURE)
                        }
                    }

                    CommandBarState.WHITEBOARD_ACTIONS -> {
                        CommandChip(Icons.Default.Draw, "Pen", false) {
                            onContentAction(ContentAction.WB_PEN)
                        }
                        CommandChip(Icons.Default.Brush, "Highlight", false) {
                            onContentAction(ContentAction.WB_HIGHLIGHTER)
                        }
                        CommandChip(Icons.Default.Delete, "Eraser", false) {
                            onContentAction(ContentAction.WB_ERASER)
                        }
                        CommandChip(Icons.Default.Undo, "Undo", false) {
                            onContentAction(ContentAction.WB_UNDO)
                        }
                        CommandChip(Icons.Default.Redo, "Redo", false) {
                            onContentAction(ContentAction.WB_REDO)
                        }
                        CommandChip(Icons.Default.Close, "Clear", false) {
                            onContentAction(ContentAction.WB_CLEAR)
                        }
                    }

                    CommandBarState.SCROLL_COMMANDS -> {
                        CommandChip(Icons.Default.ArrowUpward, "Scroll Up", false) {
                            onContentAction(ContentAction.WEB_ZOOM_IN) // Reuse zoom for scroll context
                        }
                        CommandChip(Icons.Default.ArrowDownward, "Scroll Down", false) {
                            onContentAction(ContentAction.WEB_ZOOM_OUT)
                        }
                    }

                    CommandBarState.ZOOM_COMMANDS -> {
                        CommandChip(Icons.Default.ZoomIn, "Zoom In", false) {
                            onContentAction(ContentAction.WEB_ZOOM_IN)
                        }
                        CommandChip(Icons.Default.ZoomOut, "Zoom Out", false) {
                            onContentAction(ContentAction.WEB_ZOOM_OUT)
                        }
                    }

                    CommandBarState.SPATIAL_COMMANDS -> {
                        CommandChip(Icons.Default.ScreenSearchDesktop, "Reset View", false) {
                            // Spatial reset is handled at the canvas level, not content level
                        }
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
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = "Voice: click $label" },
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
    LayoutMode.DASHBOARD -> Icons.Default.Dashboard
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
    LayoutMode.TRIPTYCH -> Icons.Default.ViewColumn
}

/**
 * Human-readable label for each layout mode.
 */
fun layoutModeLabel(mode: LayoutMode): String = when (mode) {
    LayoutMode.DASHBOARD -> "Dashboard"
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
    LayoutMode.TRIPTYCH -> "Triptych"
}

/**
 * Content type options for the ADD_FRAME state.
 */
fun addFrameOptions(): List<Pair<String, FrameContent>> = listOf(
    // P0: Core content types
    "Web" to FrameContent.Web(),
    "PDF" to FrameContent.Pdf(),
    "Image" to FrameContent.Image(),
    "Video" to FrameContent.Video(),
    "Note" to FrameContent.Note(),
    "Camera" to FrameContent.Camera(),
    // P1: Extended
    "Voice Note" to FrameContent.VoiceNote(),
    "Voice" to FrameContent.Voice(),
    "Form" to FrameContent.Form(),
    "Whiteboard" to FrameContent.Whiteboard(),
    "Signature" to FrameContent.Signature(),
    // P2: Advanced
    "Map" to FrameContent.Map(),
    "Terminal" to FrameContent.Terminal(),
    "Widget" to FrameContent.Widget(),
    "Screen Cast" to FrameContent.ScreenCast(),
    "External App" to FrameContent.ExternalApp(),
    // Killer Features
    "AI Summary (Beta)" to FrameContent.AiSummary(),
)
