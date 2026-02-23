package com.augmentalis.cockpit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import android.util.Log
import com.augmentalis.avanueui.theme.AppearanceMode
import com.augmentalis.avanueui.theme.AvanueColorPalette
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.cockpit.content.ContentRenderer
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.spatial.AndroidSpatialOrientationSource
import com.augmentalis.cockpit.spatial.SpatialViewportController
import com.augmentalis.cockpit.viewmodel.CockpitViewModel
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.handlers.ModuleCommandCallbacks

private const val TAG = "CockpitScreen"

/**
 * Android entry point for the Cockpit screen.
 *
 * This is a thin wrapper that connects the platform-specific [ContentRenderer]
 * (which uses AndroidView for WebView, CameraX, etc.) to the cross-platform
 * [CockpitScreenContent] shell. All layout logic, command bar, and state
 * management live in commonMain.
 *
 * Also creates the spatial orientation pipeline:
 * [AndroidSpatialOrientationSource] → [SpatialViewportController] → SpatialCanvas
 */
@Composable
fun CockpitScreen(
    viewModel: CockpitViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSpecialModuleLaunch: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsState()
    val frames by viewModel.frames.collectAsState()
    val selectedFrameId by viewModel.selectedFrameId.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val backgroundSceneState by viewModel.backgroundScene.collectAsState()

    // Device-adaptive layout filtering
    val displayProfile = AvanueTheme.displayProfile
    val availableModes = remember(displayProfile) {
        LayoutModeResolver.availableModes(displayProfile)
    }

    // Spatial orientation pipeline
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val spatialSource = remember { AndroidSpatialOrientationSource(context) }
    val spatialController = remember(screenWidthPx, screenHeightPx) {
        SpatialViewportController(screenWidthPx, screenHeightPx)
    }

    // Connect/disconnect spatial pipeline with composition lifecycle
    val scope = rememberCoroutineScope()
    DisposableEffect(spatialSource, spatialController) {
        spatialController.connectToSource(spatialSource, scope)
        onDispose {
            spatialController.disconnect(spatialSource)
        }
    }

    // Wire voice command executor for Cockpit handlers.
    // Read selectedFrameId from viewModel.value at dispatch time (not from
    // compose state) to avoid stale closure capture.
    DisposableEffect(viewModel) {
        ModuleCommandCallbacks.cockpitExecutor = { actionType, _ ->
            executeCockpitCommand(viewModel, actionType)
        }
        onDispose {
            ModuleCommandCallbacks.cockpitExecutor = null
        }
    }

    // Handle non-frame module launches (e.g. CursorAvanue → VoiceTouch home)
    LaunchedEffect(viewModel) {
        viewModel.specialModuleLaunch.collect { moduleId ->
            onSpecialModuleLaunch(moduleId)
        }
    }

    CockpitScreenContent(
        sessionName = session?.name ?: "Cockpit",
        frames = frames,
        selectedFrameId = selectedFrameId,
        layoutMode = layoutMode,
        onNavigateBack = onNavigateBack,
        onReturnToDashboard = { viewModel.returnToDashboard() },
        onNavigateToSettings = onNavigateToSettings,
        onFrameSelected = { viewModel.selectFrame(it) },
        onFrameMoved = { id, x, y -> viewModel.moveFrame(id, x, y) },
        onFrameResized = { id, w, h -> viewModel.resizeFrame(id, w, h) },
        onFrameClose = { viewModel.removeFrame(it) },
        onFrameMinimize = { viewModel.toggleMinimize(it) },
        onFrameMaximize = { viewModel.toggleMaximize(it) },
        onLayoutModeChanged = { viewModel.setLayoutMode(it) },
        onAddFrame = { content, title -> viewModel.addFrame(content, title) },
        frameContent = { frame ->
            ContentRenderer(
                frame = frame,
                onContentStateChanged = { frameId, newContent ->
                    viewModel.updateFrameContent(frameId, newContent)
                }
            )
        },
        spatialController = spatialController,
        backgroundScene = backgroundSceneState,
        currentPalette = AvanueColorPalette.DEFAULT,
        currentMaterial = MaterialMode.DEFAULT,
        currentAppearance = AppearanceMode.DEFAULT,
        currentPresetId = null,
        onPaletteChanged = {},
        onMaterialChanged = {},
        onAppearanceChanged = {},
        onPresetApplied = {},
        onBackgroundSceneChanged = { viewModel.setBackgroundScene(it) },
        availableLayoutModes = availableModes,
        dashboardState = dashboardState,
        onModuleClick = { viewModel.launchModule(it) },
        onSessionClick = { viewModel.resumeSession(it) },
        onTemplateClick = { viewModel.launchTemplate(it) },
        onStepRenamed = { id, title -> viewModel.renameFrame(id, title) },
        onStepReordered = { id, delta -> viewModel.reorderFrame(id, delta) },
        onStepDeleted = { viewModel.removeFrame(it) },
        modifier = modifier
    )
}

/**
 * Maps Cockpit voice commands to CockpitViewModel operations.
 *
 * Handles three command groups:
 * 1. Frame management: add/minimize/maximize/close
 * 2. Layout switching: grid/split/freeform/fullscreen/workflow
 * 3. Content insertion: web/camera/note/pdf/image/video/whiteboard/terminal
 *
 * Reads selectedFrameId from viewModel.selectedFrameId.value at execution time
 * to avoid stale Compose state closure capture.
 */
private fun executeCockpitCommand(
    viewModel: CockpitViewModel,
    actionType: CommandActionType,
): HandlerResult {
    val selected = viewModel.selectedFrameId.value
    Log.d(TAG, "executeCockpitCommand: $actionType, selectedFrame=$selected")

    return when (actionType) {
        // ── Frame Management ──────────────────────────────────────────
        CommandActionType.ADD_FRAME -> {
            viewModel.addFrame(FrameContent.Web(), "New Frame")
            HandlerResult.success("Frame added")
        }
        CommandActionType.MINIMIZE_FRAME -> {
            selected ?: return HandlerResult.failure(
                "No frame selected — select a frame first", recoverable = true
            )
            viewModel.toggleMinimize(selected)
            HandlerResult.success("Frame minimized")
        }
        CommandActionType.MAXIMIZE_FRAME -> {
            selected ?: return HandlerResult.failure(
                "No frame selected — select a frame first", recoverable = true
            )
            viewModel.toggleMaximize(selected)
            HandlerResult.success("Frame maximized")
        }
        CommandActionType.CLOSE_FRAME -> {
            selected ?: return HandlerResult.failure(
                "No frame selected — select a frame first", recoverable = true
            )
            viewModel.removeFrame(selected)
            HandlerResult.success("Frame closed")
        }

        // ── Layout Switching ──────────────────────────────────────────
        CommandActionType.LAYOUT_GRID -> {
            viewModel.setLayoutMode(LayoutMode.GRID)
            HandlerResult.success("Switched to grid layout")
        }
        CommandActionType.LAYOUT_SPLIT -> {
            viewModel.setLayoutMode(LayoutMode.SPLIT_LEFT)
            HandlerResult.success("Switched to split layout")
        }
        CommandActionType.LAYOUT_FREEFORM -> {
            viewModel.setLayoutMode(LayoutMode.FREEFORM)
            HandlerResult.success("Switched to freeform layout")
        }
        CommandActionType.LAYOUT_FULLSCREEN -> {
            viewModel.setLayoutMode(LayoutMode.FULLSCREEN)
            HandlerResult.success("Switched to fullscreen")
        }
        CommandActionType.LAYOUT_WORKFLOW -> {
            viewModel.setLayoutMode(LayoutMode.WORKFLOW)
            HandlerResult.success("Switched to workflow layout")
        }
        CommandActionType.LAYOUT_PICKER -> {
            // Cycle through frame-based layout modes (skip DASHBOARD — it's a separate home view)
            val modes = LayoutMode.FRAME_LAYOUTS.toList()
            val currentIndex = modes.indexOf(viewModel.layoutMode.value)
            val nextMode = modes[(currentIndex + 1) % modes.size]
            viewModel.setLayoutMode(nextMode)
            HandlerResult.success("Layout: ${nextMode.name.lowercase()}")
        }

        // ── Content Insertion ─────────────────────────────────────────
        CommandActionType.ADD_WEB -> {
            viewModel.addFrame(FrameContent.Web(), "Web")
            HandlerResult.success("Web frame added")
        }
        CommandActionType.ADD_CAMERA -> {
            viewModel.addFrame(FrameContent.Camera(), "Camera")
            HandlerResult.success("Camera frame added")
        }
        CommandActionType.ADD_NOTE -> {
            viewModel.addFrame(FrameContent.Note(), "Note")
            HandlerResult.success("Note frame added")
        }
        CommandActionType.ADD_PDF -> {
            viewModel.addFrame(FrameContent.Pdf(), "PDF")
            HandlerResult.success("PDF frame added")
        }
        CommandActionType.ADD_IMAGE -> {
            viewModel.addFrame(FrameContent.Image(), "Image")
            HandlerResult.success("Image frame added")
        }
        CommandActionType.ADD_VIDEO -> {
            viewModel.addFrame(FrameContent.Video(), "Video")
            HandlerResult.success("Video frame added")
        }
        CommandActionType.ADD_WHITEBOARD -> {
            viewModel.addFrame(FrameContent.Whiteboard(), "Whiteboard")
            HandlerResult.success("Whiteboard frame added")
        }
        CommandActionType.ADD_TERMINAL -> {
            viewModel.addFrame(FrameContent.Terminal(), "Terminal")
            HandlerResult.success("Terminal frame added")
        }

        else -> {
            Log.w(TAG, "Unhandled cockpit action: $actionType")
            HandlerResult.failure(
                "Cockpit action $actionType not handled",
                recoverable = true
            )
        }
    }
}
