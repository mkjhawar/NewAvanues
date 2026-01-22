package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.augmentalis.avamagic.ui.core.feedback.Alert
import com.augmentalis.avamagic.ui.core.feedback.AlertSeverity
import com.augmentalis.avamagic.ui.core.feedback.AlertVariant
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer

/**
 * AlertMapper - Maps Alert component to Material3 Card with alert styling
 *
 * Supports different severity levels (Info, Success, Warning, Error) and
 * variants (Filled, Outlined, Standard).
 *
 * Material Design 3 doesn't have a dedicated Alert component, so this
 * implementation uses Cards with appropriate styling and colors.
 */
class AlertMapper : ComponentMapper<Alert> {

    override fun map(component: Alert, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val colors = getSeverityColors(component.severity)
            val icon = component.icon?.let { getIconForSeverity(component.severity) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = when (component.variant) {
                    AlertVariant.Filled -> CardDefaults.cardColors(
                        containerColor = colors.container
                    )
                    AlertVariant.Outlined -> CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                    AlertVariant.Standard -> CardDefaults.cardColors()
                },
                border = when (component.variant) {
                    AlertVariant.Outlined -> BorderStroke(1.dp, colors.border)
                    else -> null
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Icon
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = component.severity.name,
                            tint = colors.icon,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }

                    // Content
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Title
                        if (component.title != null) {
                            Text(
                                text = component.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Message
                        Text(
                            text = component.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text
                        )

                        // Actions
                        if (component.actions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                component.actions.forEach { action ->
                                    TextButton(onClick = action.onClick ?: {}) {
                                        Text(action.label)
                                    }
                                }
                            }
                        }
                    }

                    // Close button
                    if (component.closeable) {
                        IconButton(
                            onClick = component.onClose ?: {},
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close alert",
                                tint = colors.icon
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun getIconForSeverity(severity: AlertSeverity): ImageVector {
        return when (severity) {
            AlertSeverity.Info -> Icons.Default.Info
            AlertSeverity.Success -> Icons.Default.CheckCircle
            AlertSeverity.Warning -> Icons.Default.Warning
            AlertSeverity.Error -> Icons.Default.Error
        }
    }

    @Composable
    private fun getSeverityColors(severity: AlertSeverity): AlertColors {
        return when (severity) {
            AlertSeverity.Info -> AlertColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                border = MaterialTheme.colorScheme.primary,
                icon = MaterialTheme.colorScheme.primary,
                text = MaterialTheme.colorScheme.onPrimaryContainer
            )
            AlertSeverity.Success -> AlertColors(
                container = Color(0xFF4CAF50).copy(alpha = 0.1f),
                border = Color(0xFF4CAF50),
                icon = Color(0xFF4CAF50),
                text = MaterialTheme.colorScheme.onSurface
            )
            AlertSeverity.Warning -> AlertColors(
                container = Color(0xFFFF9800).copy(alpha = 0.1f),
                border = Color(0xFFFF9800),
                icon = Color(0xFFFF9800),
                text = MaterialTheme.colorScheme.onSurface
            )
            AlertSeverity.Error -> AlertColors(
                container = MaterialTheme.colorScheme.errorContainer,
                border = MaterialTheme.colorScheme.error,
                icon = MaterialTheme.colorScheme.error,
                text = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }

    private data class AlertColors(
        val container: Color,
        val border: Color,
        val icon: Color,
        val text: Color
    )
}
