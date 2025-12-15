package com.augmentalis.cockpit.mvp.content.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WidgetType
import com.augmentalis.cockpit.mvp.OceanTheme

/**
 * WidgetRenderer - Renders native widget content
 *
 * Dispatches to specific widget implementations based on WidgetType:
 * - CALCULATOR → CalculatorWidget
 * - WEATHER → WeatherWidget (TODO)
 * - CLOCK → ClockWidget (TODO)
 * - NOTES → NotesWidget (TODO)
 * - SYSTEM_MONITOR → SystemMonitorWidget (TODO)
 * - CUSTOM → CustomWidget (TODO)
 *
 * @param widgetContent Widget configuration and state
 * @param onStateChanged Callback when widget state changes
 * @param modifier Modifier for positioning
 */
@Composable
fun WidgetRenderer(
    widgetContent: WindowContent.WidgetContent,
    onStateChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Call state changed callback on initialization to signal widget is ready
    LaunchedEffect(widgetContent.widgetType) {
        onStateChanged("initialized")
    }

    when (widgetContent.widgetType) {
        WidgetType.CALCULATOR -> {
            CalculatorWidgetPlaceholder(modifier = modifier)
        }
        WidgetType.WEATHER -> {
            WidgetPlaceholder("Weather Widget", "Coming soon", modifier)
        }
        WidgetType.CLOCK -> {
            WidgetPlaceholder("Clock Widget", "Coming soon", modifier)
        }
        WidgetType.NOTES -> {
            WidgetPlaceholder("Notes Widget", "Coming soon", modifier)
        }
        WidgetType.SYSTEM_MONITOR -> {
            WidgetPlaceholder("System Monitor", "Coming soon", modifier)
        }
        WidgetType.CUSTOM -> {
            WidgetPlaceholder("Custom Widget", "Not implemented", modifier)
        }
    }
}

/**
 * Calculator widget placeholder (will be replaced with full implementation)
 */
@Composable
private fun CalculatorWidgetPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.backgroundStart),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = "Calculator",
                modifier = Modifier.size(64.dp),
                tint = OceanTheme.primary
            )

            Text(
                text = "Calculator",
                style = MaterialTheme.typography.headlineMedium,
                color = OceanTheme.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Native calculator widget\n\nFull implementation coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = OceanTheme.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple calculator preview
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(0.75f),
                color = OceanTheme.glassSurface.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.displayLarge,
                        color = OceanTheme.textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Generic widget placeholder
 */
@Composable
private fun WidgetPlaceholder(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.backgroundStart),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OceanTheme.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OceanTheme.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
