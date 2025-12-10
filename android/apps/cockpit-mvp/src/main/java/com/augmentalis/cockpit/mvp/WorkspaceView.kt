package com.augmentalis.cockpit.mvp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.AppWindow

/**
 * Main workspace view with glassmorphic 2D flat rendering
 * Supports portrait (vertical) and landscape (horizontal) layouts
 *
 * Note: 3D curved spatial rendering (Phase 2) components are ready but integration
 * pending HUDManager dependency resolution. See: Cockpit-MVP-Phase2-Architecture-Ready-50912.md
 */
@Composable
fun WorkspaceView(
    windows: List<AppWindow>,
    positions: Map<String, com.avanues.cockpit.core.workspace.Vector3D>,
    onRemoveWindow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OceanTheme.backgroundStart,
                        OceanTheme.backgroundEnd
                    )
                )
            )
    ) {
        // Detect orientation: portrait if height > width
        val isPortrait = maxHeight > maxWidth

        if (windows.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OceanTheme.spacingXLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No Windows",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OceanTheme.textTertiary
                )
                Spacer(modifier = Modifier.height(OceanTheme.spacingSmall))
                Text(
                    text = "Tap + to add a window",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textDisabled
                )
            }
        } else {
            if (isPortrait) {
                // Portrait mode: Vertical stacking
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OceanTheme.spacingDefault),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        OceanTheme.spacingDefault,
                        Alignment.CenterVertically
                    )
                ) {
                    windows.forEachIndexed { index, window ->
                        val color = getWindowColor(index)
                        WindowCard(
                            window = window,
                            color = color,
                            onClose = { onRemoveWindow(window.id) },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }
            } else {
                // Landscape mode: Horizontal layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            vertical = OceanTheme.spacingDefault,
                            horizontal = OceanTheme.spacingSmall
                        ),
                    horizontalArrangement = Arrangement.spacedBy(
                        OceanTheme.spacingDefault,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    windows.forEachIndexed { index, window ->
                        val color = getWindowColor(index)
                        WindowCard(
                            window = window,
                            color = color,
                            onClose = { onRemoveWindow(window.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get window color for visual differentiation
 * Uses color cycling for multiple windows
 */
private fun getWindowColor(index: Int): String {
    val colors = listOf(
        "#FF6B9D", // Pink
        "#4ECDC4", // Teal
        "#95E1D3", // Mint
        "#FFD93D", // Yellow
        "#FF8B94", // Coral
        "#A8E6CF"  // Light green
    )
    return colors[index % colors.size]
}
