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
import com.avanues.cockpit.ui.UtilityBelt
import com.avanues.cockpit.ui.UtilityWidget
import com.avanues.cockpit.ui.WidgetType

/**
 * Utility Belt - Compose Implementation
 *
 * Corner mini-panels for quick access widgets
 *
 * Voice Commands:
 * - "Show utilities"
 * - "Hide utilities"
 * - "Play music" / "Pause music"
 * - "Set timer 5 minutes"
 * - "Battery level?"
 */
@Composable
fun UtilityBeltCompose(
    belt: UtilityBelt,
    onWidgetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!belt.visible) return

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Bottom Left Widgets
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            belt.widgets
                .filter { it.visible }
                .take(2) // First 2 widgets on left
                .forEach { widget ->
                    UtilityWidgetCard(
                        widget = widget,
                        onClick = { onWidgetClick(widget.id) }
                    )
                }
        }

        // Bottom Right Widgets
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            belt.widgets
                .filter { it.visible }
                .drop(2) // Remaining widgets on right
                .forEach { widget ->
                    UtilityWidgetCard(
                        widget = widget,
                        onClick = { onWidgetClick(widget.id) }
                    )
                }
        }
    }
}

@Composable
private fun UtilityWidgetCard(
    widget: UtilityWidget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = getColorForWidget(widget.type),
        shadowElevation = 4.dp,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForWidget(widget.type),
                contentDescription = widget.id,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Map WidgetType to Material Icons Extended
 */
private fun getIconForWidget(type: WidgetType): ImageVector = when (type) {
    WidgetType.MUSIC_PLAYER -> Icons.Default.MusicNote
    WidgetType.TIMER -> Icons.Default.Timer
    WidgetType.BATTERY -> Icons.Default.BatteryFull
    WidgetType.NOTIFICATIONS -> Icons.Default.Notifications
    WidgetType.WEATHER -> Icons.Default.WbSunny
    WidgetType.CALENDAR -> Icons.Default.CalendarToday
}

/**
 * Map WidgetType to accent colors
 */
@Composable
private fun getColorForWidget(type: WidgetType) = when (type) {
    WidgetType.MUSIC_PLAYER -> MaterialTheme.colorScheme.primaryContainer
    WidgetType.TIMER -> MaterialTheme.colorScheme.secondaryContainer
    WidgetType.BATTERY -> MaterialTheme.colorScheme.tertiaryContainer
    WidgetType.NOTIFICATIONS -> MaterialTheme.colorScheme.primaryContainer
    WidgetType.WEATHER -> MaterialTheme.colorScheme.secondaryContainer
    WidgetType.CALENDAR -> MaterialTheme.colorScheme.tertiaryContainer
}

/**
 * Preview/Example Usage
 */
@Composable
fun UtilityBeltExample() {
    val sampleBelt = UtilityBelt(
        visible = true,
        widgets = com.avanues.cockpit.ui.defaultWidgets()
    )

    UtilityBeltCompose(
        belt = sampleBelt,
        onWidgetClick = { widgetId ->
            println("Clicked widget: $widgetId")
        }
    )
}
