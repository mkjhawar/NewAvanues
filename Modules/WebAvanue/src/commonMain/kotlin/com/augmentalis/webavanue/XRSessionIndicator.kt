package com.augmentalis.webavanue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * XR Session Indicator - Shows active XR session status and performance metrics.
 *
 * Displays:
 * - Session mode (AR/VR)
 * - Session state (active/paused/requesting)
 * - Performance metrics (FPS, battery, temp)
 * - Performance warnings
 *
 * REQ-XR-005: Session Lifecycle Management
 * REQ-XR-007: Performance Optimization
 */
@Composable
fun XRSessionIndicator(
    sessionMode: String?, // "AR", "VR", "XR", null
    sessionState: String, // "active", "paused", "requesting", "inactive"
    fps: Float = 0f,
    batteryLevel: Int = 100,
    temperature: Float = 0f,
    warningLevel: String = "none", // "none", "low", "medium", "high", "critical"
    uptime: String = "00:00",
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Animation for pulsing effect when active
    val infiniteTransition = rememberInfiniteTransition(label = "xr_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Show indicator when session is not inactive
    AnimatedVisibility(
        visible = sessionState != "inactive",
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = getBackgroundColor(sessionState, warningLevel),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top row: Mode badge + state + close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode badge
                        sessionMode?.let {
                            ModeBadge(
                                mode = it,
                                isActive = sessionState == "active",
                                pulseAlpha = if (sessionState == "active") pulseAlpha else 1f
                            )
                        }

                        // State text
                        Text(
                            text = getStateText(sessionState),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = getTextColor(sessionState, warningLevel)
                        )
                    }

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Dismiss",
                            tint = getTextColor(sessionState, warningLevel),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Performance metrics (only when active)
                if (sessionState == "active" && fps > 0) {
                    Divider(color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // FPS
                        MetricChip(
                            icon = Icons.Default.Info,
                            label = "FPS",
                            value = "${fps.toInt()}",
                            color = getFpsColor(fps)
                        )

                        // Battery
                        MetricChip(
                            icon = Icons.Default.Info,
                            label = "Battery",
                            value = "$batteryLevel%",
                            color = getBatteryColor(batteryLevel)
                        )

                        // Temperature (if > 0)
                        if (temperature > 0) {
                            MetricChip(
                                icon = Icons.Default.Info,
                                label = "Temp",
                                value = "${temperature.toInt()}°C",
                                color = getTempColor(temperature)
                            )
                        }

                        // Uptime
                        MetricChip(
                            icon = Icons.Default.Info,
                            label = "Time",
                            value = uptime,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Warning message (if present)
                if (warningLevel != "none" && sessionState == "active") {
                    Divider(color = Color.White.copy(alpha = 0.2f))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning",
                            tint = getWarningIconColor(warningLevel),
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = getWarningMessage(warningLevel),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeBadge(
    mode: String,
    isActive: Boolean,
    pulseAlpha: Float
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = when (mode) {
            "AR" -> Color(0xFF4CAF50).copy(alpha = if (isActive) pulseAlpha else 1f)
            "VR" -> Color(0xFF2196F3).copy(alpha = if (isActive) pulseAlpha else 1f)
            else -> Color(0xFF9C27B0).copy(alpha = if (isActive) pulseAlpha else 1f)
        }
    ) {
        Text(
            text = mode,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// Helper functions

private fun getBackgroundColor(state: String, warningLevel: String): Color {
    return when {
        warningLevel == "critical" -> Color(0xFFD32F2F) // Red
        warningLevel == "high" -> Color(0xFFF57C00) // Orange
        warningLevel == "medium" -> Color(0xFFFFA726) // Light orange
        state == "active" -> Color(0xFF1976D2) // Blue
        state == "paused" -> Color(0xFF616161) // Gray
        state == "requesting" -> Color(0xFF7B1FA2) // Purple
        else -> Color(0xFF424242) // Dark gray
    }
}

private fun getTextColor(state: String, warningLevel: String): Color {
    return Color.White
}

private fun getStateText(state: String): String {
    return when (state) {
        "active" -> "XR Session Active"
        "paused" -> "XR Session Paused"
        "requesting" -> "Starting XR Session..."
        "ended" -> "XR Session Ended"
        else -> "XR"
    }
}

private fun getFpsColor(fps: Float): Color {
    return when {
        fps >= 55f -> Color(0xFF4CAF50) // Green
        fps >= 45f -> Color(0xFFFFA726) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun getBatteryColor(level: Int): Color {
    return when {
        level > 20 -> Color.White.copy(alpha = 0.9f)
        level > 10 -> Color(0xFFFFA726) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun getTempColor(temp: Float): Color {
    return when {
        temp < 40f -> Color.White.copy(alpha = 0.9f)
        temp < 43f -> Color(0xFFFFA726) // Orange
        else -> Color(0xFFF44336) // Red
    }
}


private fun getWarningIconColor(warningLevel: String): Color {
    return when (warningLevel) {
        "critical" -> Color(0xFFFFEBEE) // Light red
        "high" -> Color(0xFFFFF3E0) // Light orange
        "medium" -> Color(0xFFFFFDE7) // Light yellow
        else -> Color.White.copy(alpha = 0.9f)
    }
}

private fun getWarningMessage(warningLevel: String): String {
    return when (warningLevel) {
        "critical" -> "Critical: Exit XR session immediately"
        "high" -> "Warning: Performance degraded, take a break"
        "medium" -> "Notice: Lower performance mode recommended"
        "low" -> "Tip: Monitor battery and temperature"
        else -> ""
    }
}
