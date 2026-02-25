package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AppearanceMode
import com.augmentalis.avanueui.theme.AvanueColorPalette
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.theme.ThemePreset
import com.augmentalis.cockpit.model.CockpitScreenState
import com.augmentalis.cockpit.model.CommandBarState
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.avanueui.display.GlassDisplayMode
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.model.SimplifiedShellMode
import com.augmentalis.cockpit.spatial.PseudoSpatialController
import com.augmentalis.cockpit.spatial.SpatialViewportController

/**
 * Cross-platform Cockpit screen shell.
 *
 * Layers a [BackgroundSceneRenderer] behind the content Column containing
 * TopAppBar, LayoutEngine, and CommandBar. The [backgroundScene] parameter
 * selects between gradient, starfield, scanline grid, or transparent.
 *
 * Platform-specific content rendering is injected via the [frameContent]
 * composable lambda parameter. This composable lives in commonMain — it
 * uses only Compose Multiplatform APIs and AvanueUI tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreenContent(
    state: CockpitScreenState,
    onNavigateBack: () -> Unit,
    onReturnToDashboard: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onFrameSelected: (String) -> Unit,
    onFrameMoved: (String, Float, Float) -> Unit,
    onFrameResized: (String, Float, Float) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    onLayoutModeChanged: (LayoutMode) -> Unit,
    onAddFrame: (FrameContent, String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    spatialController: SpatialViewportController? = null,
    pseudoSpatialController: PseudoSpatialController? = null,
    onPaletteChanged: (AvanueColorPalette) -> Unit = {},
    onMaterialChanged: (MaterialMode) -> Unit = {},
    onAppearanceChanged: (AppearanceMode) -> Unit = {},
    onPresetApplied: (ThemePreset) -> Unit = {},
    onBackgroundSceneChanged: (BackgroundScene) -> Unit = {},
    onModuleClick: (String) -> Unit = {},
    onSessionClick: (String) -> Unit = {},
    onTemplateClick: (String) -> Unit = {},
    onContentAction: (ContentAction) -> Unit = {},
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
    onVoiceActivate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var commandBarState by remember { mutableStateOf(CommandBarState.MAIN) }
    var showThemePanel by remember { mutableStateOf(false) }

    // Auto-switch command bar to content-specific state when frame selection changes
    val selectedFrame = state.frames.firstOrNull { it.id == state.selectedFrameId }
    LaunchedEffect(state.selectedFrameId) {
        if (selectedFrame != null) {
            commandBarState = CommandBarState.forContentType(selectedFrame.contentType)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background scene layer
        BackgroundSceneRenderer(
            scene = state.backgroundScene,
            modifier = Modifier.fillMaxSize()
        )

        // Content layer
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Top app bar — context-aware navigation + settings access
        TopAppBar(
            navigationIcon = {
                if (state.layoutMode != LayoutMode.DASHBOARD) {
                    // In a session: back arrow returns to Dashboard
                    IconButton(onClick = onReturnToDashboard) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voice: click Back",
                            tint = colors.textPrimary
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (state.layoutMode == LayoutMode.DASHBOARD) "Avanues" else state.sessionName,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            actions = {
                if (state.layoutMode == LayoutMode.DASHBOARD) {
                    IconButton(onClick = { showThemePanel = !showThemePanel }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Voice: click Settings",
                            tint = colors.textPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Main content area — shell mode determines home screen rendering
        if (state.layoutMode == LayoutMode.DASHBOARD) {
            // Home screen: render based on shell mode
            when (state.shellMode) {
                SimplifiedShellMode.CLASSIC -> DashboardLayout(
                    dashboardState = state.dashboardState,
                    onModuleClick = onModuleClick,
                    onSessionClick = onSessionClick,
                    onTemplateClick = onTemplateClick,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                SimplifiedShellMode.AVANUE_VIEWS -> AvanueViewsStreamLayout(
                    dashboardState = state.dashboardState,
                    onModuleClick = onModuleClick,
                    onSessionClick = onSessionClick,
                    onVoiceFabClick = onVoiceActivate,
                    onMoreModulesClick = { commandBarState = CommandBarState.ADD_FRAME },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                SimplifiedShellMode.LENS -> LensLayout(
                    dashboardState = state.dashboardState,
                    onModuleClick = onModuleClick,
                    onSessionClick = onSessionClick,
                    onVoiceActivate = onVoiceActivate,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                SimplifiedShellMode.CANVAS -> ZenCanvasLayout(
                    dashboardState = state.dashboardState,
                    onModuleClick = onModuleClick,
                    onVoiceActivate = onVoiceActivate,
                    onSearchClick = { commandBarState = CommandBarState.ADD_FRAME },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        } else if (state.frames.isEmpty()) {
            EmptySessionView(
                onAddFrame = { commandBarState = CommandBarState.ADD_FRAME },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            val layoutModifier = Modifier.weight(1f).fillMaxWidth()
            val isSpatial = state.layoutMode in LayoutMode.SPATIAL_CAPABLE && spatialController != null
            val isPseudoSpatial = pseudoSpatialController != null &&
                state.glassDisplayMode == GlassDisplayMode.FLAT_SCREEN

            // Layout rendering content (shared across all wrapping modes)
            val layoutContent: @Composable () -> Unit = {
                LayoutEngine(
                    layoutMode = state.layoutMode,
                    frames = state.frames,
                    selectedFrameId = state.selectedFrameId,
                    onFrameSelected = onFrameSelected,
                    onFrameMoved = onFrameMoved,
                    onFrameResized = onFrameResized,
                    onFrameClose = onFrameClose,
                    onFrameMinimize = onFrameMinimize,
                    onFrameMaximize = onFrameMaximize,
                    frameContent = frameContent,
                    dashboardState = state.dashboardState,
                    onModuleClick = onModuleClick,
                    onSessionClick = onSessionClick,
                    onTemplateClick = onTemplateClick,
                    onStepRenamed = onStepRenamed,
                    onStepReordered = onStepReordered,
                    onStepDeleted = onStepDeleted,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isSpatial) {
                // Glass/headset: true spatial viewport panning via head tracking
                SpatialCanvas(
                    controller = spatialController!!,
                    modifier = layoutModifier
                ) {
                    layoutContent()
                }
            } else if (isPseudoSpatial) {
                // Flat screen: parallax depth illusion with gyroscope + HUD aesthetic
                PseudoSpatialCanvas(
                    controller = pseudoSpatialController!!,
                    modifier = layoutModifier,
                    foregroundContent = layoutContent
                )
            } else {
                // No spatial effects — render layout directly
                Box(modifier = layoutModifier) {
                    layoutContent()
                }
            }
        }

        // Status bar
        if (state.frames.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(colors.background.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.frames.size} frame${if (state.frames.size != 1) "s" else ""} · ${layoutModeLabel(state.layoutMode)}",
                    color = colors.textPrimary.copy(alpha = 0.35f),
                    fontSize = 10.sp
                )
            }
        }

        // Command bar — Classic shell uses full CommandBar, simplified shells use flat bar
        if (state.shellMode == SimplifiedShellMode.CLASSIC || state.layoutMode == LayoutMode.DASHBOARD) {
            CommandBar(
                state = commandBarState,
                currentLayoutMode = state.layoutMode,
                onStateChange = { commandBarState = it },
                onLayoutSelected = onLayoutModeChanged,
                onAddFrame = onAddFrame,
                onFrameMinimize = {
                    state.selectedFrameId?.let { onFrameMinimize(it) }
                },
                onFrameMaximize = {
                    state.selectedFrameId?.let { onFrameMaximize(it) }
                },
                onFrameClose = {
                    state.selectedFrameId?.let { onFrameClose(it) }
                },
                onContentAction = onContentAction,
                availableLayoutModes = state.availableLayoutModes
            )
        } else {
            // Simplified shells: flat contextual action bar
            ContextualActionBar(
                contentTypeId = selectedFrame?.contentType,
                onActionClick = { actionId ->
                    // Map action IDs to ContentAction enum values
                    val action = contentActionFromId(actionId)
                    if (action != null) onContentAction(action)
                },
                onMoreClick = { commandBarState = CommandBarState.MAIN },
                visible = state.frames.isNotEmpty(),
            )
        }
    } // end Column

        // Theme settings panel overlay
        if (showThemePanel) {
            ThemeSettingsPanel(
                currentPalette = state.currentPalette,
                currentMaterial = state.currentMaterial,
                currentAppearance = state.currentAppearance,
                currentPresetId = state.currentPresetId,
                currentBackgroundScene = state.backgroundScene,
                onPaletteChanged = onPaletteChanged,
                onMaterialChanged = onMaterialChanged,
                onAppearanceChanged = onAppearanceChanged,
                onPresetApplied = onPresetApplied,
                onBackgroundSceneChanged = onBackgroundSceneChanged,
                onDismiss = { showThemePanel = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    } // end Box
}

/**
 * Maps flat action bar action IDs (from [ContextualActionProvider]) to
 * [ContentAction] enum values used by the platform content renderer.
 *
 * Returns null for non-content actions (frame/layout/tool actions),
 * which are handled separately by their own callbacks.
 */
private fun contentActionFromId(actionId: String): ContentAction? = when (actionId) {
    // Web
    "web_back" -> ContentAction.WEB_BACK
    "web_forward" -> ContentAction.WEB_FORWARD
    "web_refresh" -> ContentAction.WEB_REFRESH
    "web_zoom_in" -> ContentAction.WEB_ZOOM_IN
    "web_zoom_out" -> ContentAction.WEB_ZOOM_OUT
    // PDF
    "pdf_prev" -> ContentAction.PDF_PREV_PAGE
    "pdf_next" -> ContentAction.PDF_NEXT_PAGE
    "pdf_zoom_in" -> ContentAction.PDF_ZOOM_IN
    "pdf_zoom_out" -> ContentAction.PDF_ZOOM_OUT
    // Image
    "image_zoom_in" -> ContentAction.IMAGE_ZOOM_IN
    "image_zoom_out" -> ContentAction.IMAGE_ZOOM_OUT
    "image_rotate" -> ContentAction.IMAGE_ROTATE
    // Video
    "video_rewind" -> ContentAction.VIDEO_REWIND
    "video_play_pause" -> ContentAction.VIDEO_PLAY_PAUSE
    "video_fullscreen" -> ContentAction.VIDEO_FULLSCREEN
    // Note
    "note_bold" -> ContentAction.NOTE_BOLD
    "note_italic" -> ContentAction.NOTE_ITALIC
    "note_underline" -> ContentAction.NOTE_UNDERLINE
    "note_undo" -> ContentAction.NOTE_UNDO
    "note_redo" -> ContentAction.NOTE_REDO
    "note_save" -> ContentAction.NOTE_SAVE
    // Camera
    "camera_flip" -> ContentAction.CAMERA_FLIP
    "camera_capture" -> ContentAction.CAMERA_CAPTURE
    // Whiteboard
    "wb_pen" -> ContentAction.WB_PEN
    "wb_highlight" -> ContentAction.WB_HIGHLIGHTER
    "wb_eraser" -> ContentAction.WB_ERASER
    "wb_undo" -> ContentAction.WB_UNDO
    "wb_redo" -> ContentAction.WB_REDO
    "wb_clear" -> ContentAction.WB_CLEAR
    else -> null
}

/**
 * Empty state shown when no frames exist in the session.
 */
@Composable
private fun EmptySessionView(
    onAddFrame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Dashboard,
                "Empty cockpit",
                tint = colors.border,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .then(Modifier.height(72.dp))
            )
            Text(
                text = "No frames yet",
                color = colors.textPrimary.copy(alpha = 0.6f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Use the command bar below to add content",
                color = colors.textPrimary.copy(alpha = 0.3f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
