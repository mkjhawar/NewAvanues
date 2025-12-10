package com.augmentalis.cockpit.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun CockpitMVPScreen(
    viewModel: WorkspaceViewModel = viewModel()
) {
    val windows by viewModel.windows.collectAsState()
    val positions by viewModel.windowPositions.collectAsState()
    val isHeadCursorEnabled by viewModel.isHeadCursorEnabled.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main workspace with windows
        WorkspaceView(
            windows = windows,
            positions = positions,
            onRemoveWindow = { viewModel.removeWindow(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Top navigation bar
        TopNavigationBar(
            windowCount = windows.size,
            onToggleHeadCursor = { viewModel.toggleHeadCursor() },
            isHeadCursorEnabled = isHeadCursorEnabled,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom control panel (floating pill-shaped)
        ControlPanel(
            windowCount = windows.size,
            maxWindows = 6,
            onAddWindow = { title, type, color ->
                viewModel.addWindow(title, type, color)
            },
            onReset = { viewModel.resetWorkspace() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Head cursor overlay (when enabled)
        if (isHeadCursorEnabled) {
            HeadCursorOverlay(
                isEnabled = isHeadCursorEnabled,
                onCursorPositionChange = { x, y ->
                    // TODO: Handle cursor position for window selection/interaction
                }
            )
        }
    }
}
