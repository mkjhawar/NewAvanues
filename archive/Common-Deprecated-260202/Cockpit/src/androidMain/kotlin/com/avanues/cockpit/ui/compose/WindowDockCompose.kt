package com.avanues.cockpit.ui.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.ui.DockWindow
import com.avanues.cockpit.ui.WindowDock

/**
 * Window Dock - Compose Implementation
 *
 * Bottom center dot indicators (macOS/Vision Pro style)
 *
 * Voice Commands:
 * - "Show dock" / "Hide dock"
 * - "Window 3"
 * - "Next window" / "Previous window"
 */
@Composable
fun WindowDockCompose(
    dock: WindowDock,
    onWindowClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!dock.visible) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dock.windows.forEach { window ->
                WindowDockIndicator(
                    window = window,
                    onClick = { onWindowClick(window.id) },
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun WindowDockIndicator(
    window: DockWindow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val size by animateDpAsState(
        targetValue = if (window.isActive) 14.dp else 10.dp,
        label = "dock_indicator_size"
    )

    val alpha by animateFloatAsState(
        targetValue = if (window.isActive) 1f else 0.5f,
        label = "dock_indicator_alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (window.isActive) 1.2f else 1f,
        label = "dock_indicator_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .alpha(alpha)
            .background(
                color = when {
                    window.isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

/**
 * Preview/Example Usage
 */
@Composable
fun WindowDockExample() {
    val sampleDock = WindowDock(
        windows = listOf(
            DockWindow("1", "Messages", null, false, "messages"),
            DockWindow("2", "Safari", null, false, "browser"),
            DockWindow("3", "Photos", null, false, "photos"),
            DockWindow("4", "Sheets", null, true, "sheets"),
            DockWindow("5", "Mail", null, false, "email")
        ),
        activeWindowId = "4",
        visible = true
    )

    WindowDockCompose(
        dock = sampleDock,
        onWindowClick = { windowId ->
            println("Clicked window: $windowId")
        }
    )
}
