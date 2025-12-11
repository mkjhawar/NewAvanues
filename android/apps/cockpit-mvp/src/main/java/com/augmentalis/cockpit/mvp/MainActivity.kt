package com.augmentalis.cockpit.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.cockpit.mvp.theme.AppTheme
import com.augmentalis.cockpit.mvp.theme.CockpitThemeProvider

/**
 * Cockpit MVP - Main Activity
 *
 * Features:
 * - Functional window management (add, remove, reset)
 * - Glassmorphic Ocean Theme UI
 * - Orientation support (portrait: vertical, landscape: horizontal)
 * - Display cutout detection and handling
 * - Head-based navigation with IMU/DeviceManager
 * - MagicUI migration-ready architecture
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge for full screen immersion
        enableEdgeToEdge()

        setContent {
            CockpitThemeProvider(theme = AppTheme.OCEAN) {
                CockpitMVPScreen()
            }
        }
    }
}

@Composable
fun CockpitMVPScreen() {
    val context = LocalContext.current
    val viewModel: WorkspaceViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )
    val windows by viewModel.windows.collectAsState()
    val positions by viewModel.windowPositions.collectAsState()
    val windowColors by viewModel.windowColors.collectAsState()
    val isHeadCursorEnabled by viewModel.isHeadCursorEnabled.collectAsState()
    val isSpatialMode by viewModel.isSpatialMode.collectAsState()
    val layoutPreset by viewModel.layoutPreset.collectAsState()
    val selectedWindowId by viewModel.selectedWindowId.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main workspace with windows (2D flat or 3D curved)
        if (isSpatialMode) {
            SpatialWorkspaceView(
                windows = windows,
                layoutPreset = layoutPreset,
                windowColors = windowColors,
                selectedWindowId = selectedWindowId,
                onRemoveWindow = { viewModel.removeWindow(it) },
                onCycleLayoutPreset = { forward -> viewModel.cycleLayoutPreset(forward) },
                onWindowHover = { windowId -> viewModel.setSelectedWindow(windowId) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WorkspaceView(
                windows = windows,
                positions = positions,
                selectedWindowId = selectedWindowId,
                onRemoveWindow = { viewModel.removeWindow(it) },
                onMinimizeWindow = { viewModel.minimizeWindow(it) },
                onToggleWindowSize = { viewModel.toggleWindowSize(it) },
                onSelectWindow = { viewModel.selectWindow(it) },
                onUpdateWindowContent = { windowId, content -> viewModel.updateWindowContent(windowId, content) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top navigation bar with spatial mode toggle
        TopNavigationBar(
            windowCount = windows.size,
            onToggleHeadCursor = { viewModel.toggleHeadCursor() },
            isHeadCursorEnabled = isHeadCursorEnabled,
            onToggleSpatialMode = { viewModel.toggleSpatialMode() },
            isSpatialMode = isSpatialMode,
            workspaceName = if (isSpatialMode) {
                "Spatial - ${viewModel.getLayoutPresetName()}"
            } else {
                "Cockpit Workspace"
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom control panel (floating pill-shaped)
        ControlPanel(
            windowCount = windows.size,
            maxWindows = 6,
            onAddWindow = { title, type, color, content ->
                viewModel.addWindow(title, type, color, content)
            },
            onReset = { viewModel.resetWorkspace() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Head cursor overlay (when enabled)
        if (isHeadCursorEnabled) {
            HeadCursorOverlay(
                isEnabled = isHeadCursorEnabled,
                onCursorPositionChange = { x, y ->
                    // Pass cursor position to ViewModel for hit detection
                    // Actual hit detection happens in SpatialWorkspaceView
                    viewModel.updateCursorPosition(x, y)
                }
            )
        }
    }
}
