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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CommandBarState
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.spatial.SpatialViewportController

/**
 * Cross-platform Cockpit screen shell.
 *
 * Contains the SpatialVoice background gradient, TopAppBar, LayoutEngine,
 * and CommandBar. Platform-specific content rendering is injected via the
 * [frameContent] composable lambda parameter.
 *
 * This composable lives in commonMain — it uses only Compose Multiplatform
 * APIs and AvanueUI tokens. The Android app-level code calls this and
 * passes its ContentRenderer as [frameContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreenContent(
    sessionName: String,
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    layoutMode: LayoutMode,
    onNavigateBack: () -> Unit,
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
    availableLayoutModes: List<LayoutMode> = LayoutMode.entries,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var commandBarState by remember { mutableStateOf(CommandBarState.MAIN) }

    // Auto-switch command bar to content-specific state when frame selection changes
    val selectedFrame = frames.firstOrNull { it.id == selectedFrameId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.surface.copy(alpha = 0.6f),
                        colors.background
                    )
                )
            )
    ) {
        // Top app bar — simplified (layout picker moved to CommandBar)
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
            },
            title = {
                Text(
                    text = sessionName,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Main content area
        if (frames.isEmpty()) {
            EmptySessionView(
                onAddFrame = { commandBarState = CommandBarState.ADD_FRAME },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            val layoutModifier = Modifier.weight(1f).fillMaxWidth()
            val isSpatial = layoutMode in LayoutMode.SPATIAL_CAPABLE && spatialController != null

            if (isSpatial) {
                SpatialCanvas(
                    controller = spatialController!!,
                    modifier = layoutModifier
                ) {
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
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
                    modifier = layoutModifier
                )
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
    }
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
