package com.augmentalis.cockpit.mvp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.components.GlassmorphicSurface

/**
 * Floating top navigation bar with glassmorphic styling
 * Shows current workspace context and window count
 * Adapts to display cutouts (notch, punch hole, dynamic island)
 * Includes haptic feedback for button interactions
 */
@Composable
fun TopNavigationBar(
    windowCount: Int,
    onToggleHeadCursor: () -> Unit,
    isHeadCursorEnabled: Boolean,
    onToggleSpatialMode: () -> Unit,
    isSpatialMode: Boolean,
    workspaceName: String = "Cockpit Workspace",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticFeedbackManager(context) }

    // Get display cutout insets to avoid camera/island
    val displayCutoutInsets = WindowInsets.displayCutout
    val topInset = with(LocalDensity.current) {
        displayCutoutInsets.asPaddingValues().calculateTopPadding()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp + topInset,
                bottom = 12.dp,
                start = 16.dp,
                end = 16.dp
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        GlassmorphicSurface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Menu icon
                IconButton(
                    onClick = {
                        hapticManager.performLightTap()
                        /* TODO: Open workspace menu */
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = OceanTheme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center: Workspace name and context
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = workspaceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = OceanTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$windowCount ${if (windowCount == 1) "window" else "windows"} active",
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTheme.textTertiary
                    )
                }

                // Right: Head cursor toggle + Status indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Head cursor toggle
                    IconButton(
                        onClick = {
                            hapticManager.performMediumTap()
                            onToggleHeadCursor()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isHeadCursorEnabled) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = if (isHeadCursorEnabled) "Disable head cursor" else "Enable head cursor",
                            tint = if (isHeadCursorEnabled) OceanTheme.success else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Spatial mode toggle (2D/3D)
                    IconButton(
                        onClick = {
                            hapticManager.performMediumTap()
                            onToggleSpatialMode()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpatialMode) Icons.Default.ViewInAr else Icons.Default.ViewStream,
                            contentDescription = if (isSpatialMode) "Switch to 2D mode" else "Switch to 3D mode",
                            tint = if (isSpatialMode) OceanTheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Status indicator
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (windowCount > 0) OceanTheme.success else Color.White.copy(alpha = 0.3f)
                    ) {}
                }
            }
        }
    }
}
