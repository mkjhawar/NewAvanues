package com.augmentalis.Avanues.web.universal.presentation.ui.xr

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * XR Performance Warning - Shows non-intrusive performance warnings during XR sessions.
 *
 * REQ-XR-007: Performance Optimization
 *
 * Displays warnings for:
 * - Low FPS
 * - Low battery
 * - High temperature
 * - High battery drain
 */
@Composable
fun XRPerformanceWarning(
    warningType: String, // "low_fps", "battery_low", "thermal", "drain"
    severity: String, // "low", "medium", "high", "critical"
    message: String,
    recommendation: String,
    onDismiss: () -> Unit,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Pulsing animation for critical warnings
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val backgroundColor = when (severity) {
        "critical" -> Color(0xFFD32F2F).copy(alpha = if (severity == "critical") pulseAlpha else 1f)
        "high" -> Color(0xFFF57C00)
        "medium" -> Color(0xFFFFA726)
        else -> Color(0xFFFFB74D)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Warning icon
            Icon(
                imageVector = when (warningType) {
                    "low_fps" -> Icons.Default.Info
                    "battery_low", "battery_critical" -> Icons.Default.Info
                    "thermal" -> Icons.Default.Info
                    "drain" -> Icons.Default.Info
                    else -> Icons.Default.Info
                },
                contentDescription = "Warning",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            // Message column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Close button
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Critical Performance Dialog - Full-screen blocking dialog for critical issues.
 */
@Composable
fun XRCriticalWarningDialog(
    title: String,
    message: String,
    actionText: String = "Exit XR Session",
    onAction: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Critical warning icon
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(64.dp)
                )

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action button
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(actionText)
                }

                // Dismiss option
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Understand the Risk")
                }
            }
        }
    }
}

/**
 * FPS Warning Overlay - Minimal overlay showing current FPS with color coding.
 */
@Composable
fun XRFPSIndicator(
    fps: Float,
    showWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        fps >= 55f -> Color(0xFF4CAF50).copy(alpha = 0.8f) // Green
        fps >= 45f -> Color(0xFFFFA726).copy(alpha = 0.8f) // Orange
        else -> Color(0xFFF44336).copy(alpha = 0.8f) // Red
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "FPS",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "${fps.toInt()} FPS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (showWarning && fps < 45f) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Warning",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
