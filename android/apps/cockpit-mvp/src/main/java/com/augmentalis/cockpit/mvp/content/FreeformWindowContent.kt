package com.augmentalis.cockpit.mvp.content

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.WindowContent
import com.augmentalis.cockpit.mvp.FreeformWindowManager

/**
 * Freeform window content renderer for WindowContent.FreeformAppContent
 *
 * Uses FreeformWindowManager to capture and render Android app windows.
 * Requires MediaProjection permission to be granted.
 */
@Composable
fun FreeformWindowContent(
    freeformContent: WindowContent.FreeformAppContent,
    freeformManager: FreeformWindowManager?,
    modifier: Modifier = Modifier
) {
    if (freeformManager == null) {
        // FreeformWindowManager not available
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Freeform windows not available",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = freeformContent.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // TODO: Integrate with FreeformWindowManager
    // This will be implemented when MediaProjection permission flow is complete
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Freeform window (requires permission)",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = freeformContent.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "MediaProjection permission needed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
