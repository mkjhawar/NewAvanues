package com.augmentalis.cockpit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.content.ContentRenderer
import com.augmentalis.cockpit.spatial.AndroidSpatialOrientationSource
import com.augmentalis.cockpit.spatial.SpatialViewportController
import com.augmentalis.cockpit.viewmodel.CockpitViewModel

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
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsState()
    val frames by viewModel.frames.collectAsState()
    val selectedFrameId by viewModel.selectedFrameId.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()

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

    CockpitScreenContent(
        sessionName = session?.name ?: "Cockpit",
        frames = frames,
        selectedFrameId = selectedFrameId,
        layoutMode = layoutMode,
        onNavigateBack = onNavigateBack,
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
                onContentStateChanged = { frameId, jsonState ->
                    viewModel.updateContentState(frameId, jsonState)
                }
            )
        },
        spatialController = spatialController,
        availableLayoutModes = availableModes,
        modifier = modifier
    )
}
