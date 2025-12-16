package com.augmentalis.cockpit.mvp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.HapticFeedbackManager
import com.augmentalis.cockpit.mvp.OceanTheme

/**
 * Window control bar with minimize, maximize, and close buttons
 * Displays window title and provides haptic feedback
 *
 * Per spec FR-1:
 * - Minimize: Sets window.isHidden = true, haptic medium tap
 * - Maximize: Toggles window.isLarge, haptic medium tap
 * - Close: Removes window, haptic light tap
 */
@Composable
fun WindowControlBar(
    title: String,
    isHidden: Boolean,
    isLarge: Boolean,
    onMinimize: () -> Unit,
    onToggleSize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticFeedbackManager(context) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Window title
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = OceanTheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Right: Control buttons (minimize, maximize, close)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimize button
            IconButton(
                onClick = {
                    hapticManager.performMediumTap()
                    onMinimize()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Minimize,
                    contentDescription = "Minimize window",
                    tint = OceanTheme.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Maximize/Restore button
            IconButton(
                onClick = {
                    hapticManager.performMediumTap()
                    onToggleSize()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isLarge) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isLarge) "Restore window" else "Maximize window",
                    tint = if (isLarge) OceanTheme.primary else OceanTheme.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Close button
            IconButton(
                onClick = {
                    hapticManager.performLightTap()
                    onClose()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close window",
                    tint = OceanTheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
