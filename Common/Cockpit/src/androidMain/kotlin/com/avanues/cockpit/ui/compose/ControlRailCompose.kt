package com.avanues.cockpit.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.ui.ControlRail
import com.avanues.cockpit.ui.RailAction
import com.avanues.cockpit.ui.RailButton

/**
 * Control Rail - Compose Implementation
 *
 * Floating toolbar for system functions
 *
 * Voice Commands:
 * - "Show controls" / "Hide controls"
 * - "Go home"
 * - "Switch workspace"
 * - "Change layout"
 * - "Settings"
 */
@Composable
fun ControlRailCompose(
    rail: ControlRail,
    onButtonClick: (RailAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!rail.visible) return

    Surface(
        modifier = modifier
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rail.buttons.forEach { button ->
                ControlRailButton(
                    button = button,
                    onClick = { onButtonClick(button.action) }
                )
            }
        }
    }
}

@Composable
private fun ControlRailButton(
    button: RailButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = getIconForAction(button.action),
            contentDescription = button.label,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Map RailAction to Material Icons Extended
 */
private fun getIconForAction(action: RailAction): ImageVector = when (action) {
    RailAction.HOME -> Icons.Default.Home
    RailAction.WORKSPACE_SELECTOR -> Icons.Default.Dashboard
    RailAction.LAYOUT_SELECTOR -> Icons.Default.GridView
    RailAction.VOICE_SETTINGS -> Icons.Default.Mic
    RailAction.SYSTEM_SETTINGS -> Icons.Default.Settings
}

/**
 * Preview/Example Usage
 */
@Composable
fun ControlRailExample() {
    val sampleRail = ControlRail(
        visible = true,
        buttons = com.avanues.cockpit.ui.defaultButtons()
    )

    ControlRailCompose(
        rail = sampleRail,
        onButtonClick = { action ->
            println("Clicked: $action")
        }
    )
}
