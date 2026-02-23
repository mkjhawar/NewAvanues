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
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CommandBarState
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.DashboardState
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.avanueui.display.GlassDisplayMode
import com.augmentalis.cockpit.model.LayoutMode
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
    sessionName: String,
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    layoutMode: LayoutMode,
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
    glassDisplayMode: GlassDisplayMode = GlassDisplayMode.FLAT_SCREEN,
    backgroundScene: BackgroundScene = BackgroundScene.GRADIENT,
    availableLayoutModes: List<LayoutMode> = LayoutMode.entries,
    dashboardState: DashboardState = DashboardState(),
    onModuleClick: (String) -> Unit = {},
    onSessionClick: (String) -> Unit = {},
    onTemplateClick: (String) -> Unit = {},
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var commandBarState by remember { mutableStateOf(CommandBarState.MAIN) }

    // Auto-switch command bar to content-specific state when frame selection changes
    val selectedFrame = frames.firstOrNull { it.id == selectedFrameId }
    LaunchedEffect(selectedFrameId) {
        if (selectedFrame != null) {
            commandBarState = CommandBarState.forContentType(selectedFrame.contentType)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background scene layer
        BackgroundSceneRenderer(
            scene = backgroundScene,
            modifier = Modifier.fillMaxSize()
        )

        // Content layer
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Top app bar — context-aware navigation + settings access
        TopAppBar(
            navigationIcon = {
                if (layoutMode != LayoutMode.DASHBOARD) {
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
                    text = if (layoutMode == LayoutMode.DASHBOARD) "Avanues" else sessionName,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            actions = {
                if (layoutMode == LayoutMode.DASHBOARD) {
                    IconButton(onClick = onNavigateToSettings) {
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

        // Main content area
        if (layoutMode == LayoutMode.DASHBOARD) {
            // Dashboard takes over the full content area — no frames rendered
            DashboardLayout(
                dashboardState = dashboardState,
                onModuleClick = onModuleClick,
                onSessionClick = onSessionClick,
                onTemplateClick = onTemplateClick,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else if (frames.isEmpty()) {
            EmptySessionView(
                onAddFrame = { commandBarState = CommandBarState.ADD_FRAME },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            val layoutModifier = Modifier.weight(1f).fillMaxWidth()
            val isSpatial = layoutMode in LayoutMode.SPATIAL_CAPABLE && spatialController != null
            val isPseudoSpatial = pseudoSpatialController != null &&
                glassDisplayMode == GlassDisplayMode.FLAT_SCREEN

            // Layout rendering content (shared across all wrapping modes)
            val layoutContent: @Composable () -> Unit = {
                LayoutEngine(
                    layoutMode = layoutMode,
                    frames = frames,
                    selectedFrameId = selectedFrameId,
                    onFrameSelected = onFrameSelected,
                    onFrameMoved = onFrameMoved,
                    onFrameResized = onFrameResized,
                    onFrameClose = onFrameClose,
                    onFrameMinimize = onFrameMinimize,
                    onFrameMaximize = onFrameMaximize,
                    frameContent = frameContent,
                    dashboardState = dashboardState,
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
        if (frames.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(colors.background.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${frames.size} frame${if (frames.size != 1) "s" else ""} · ${layoutModeLabel(layoutMode)}",
                    color = colors.textPrimary.copy(alpha = 0.35f),
                    fontSize = 10.sp
                )
            }
        }

        // Command bar
        CommandBar(
            state = commandBarState,
            currentLayoutMode = layoutMode,
            onStateChange = { commandBarState = it },
            onLayoutSelected = onLayoutModeChanged,
            onAddFrame = onAddFrame,
            onFrameMinimize = {
                selectedFrameId?.let { onFrameMinimize(it) }
            },
            onFrameMaximize = {
                selectedFrameId?.let { onFrameMaximize(it) }
            },
            onFrameClose = {
                selectedFrameId?.let { onFrameClose(it) }
            },
            availableLayoutModes = availableLayoutModes
        )
    } // end Column
    } // end Box
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
