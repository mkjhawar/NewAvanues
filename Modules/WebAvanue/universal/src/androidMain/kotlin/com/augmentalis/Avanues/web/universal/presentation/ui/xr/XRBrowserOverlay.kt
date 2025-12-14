package com.augmentalis.webavanue.ui.screen.xr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.feature.xr.XRState

/**
 * XRBrowserOverlay - Android-specific XR UI overlay for BrowserScreen
 *
 * Shows:
 * - XRSessionIndicator when session is active
 * - XRPerformanceWarning banners for active warnings
 *
 * Usage in BrowserScreen (Android only):
 * ```kotlin
 * if (xrState != null) {
 *     XRBrowserOverlay(xrState = xrState)
 * }
 * ```
 *
 * @param xrState XR state from XRManager (from commonMain)
 * @param modifier Modifier for the overlay container
 */
@Composable
fun XRBrowserOverlay(
    xrState: XRState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Session Indicator
        if (xrState.isSessionActive && xrState.sessionMode != null) {
            XRSessionIndicator(
                sessionMode = xrState.sessionMode,
                sessionState = xrState.sessionState,
                fps = xrState.performanceMetrics.fps,
                batteryLevel = xrState.performanceMetrics.batteryLevel,
                temperature = xrState.performanceMetrics.batteryTemperature,
                warningLevel = xrState.warnings.firstOrNull()?.severity?.name?.lowercase() ?: "none",
                uptime = formatUptime(xrState.performanceMetrics.uptime),
                onDismiss = { /* User dismissed indicator */ },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        // Performance Warnings - up to 3, stacked below indicator
        xrState.warnings.take(3).forEachIndexed { index, warning ->
            XRPerformanceWarning(
                warningType = warning.type.name.lowercase(),
                severity = warning.severity.name.lowercase(),
                message = warning.message,
                recommendation = warning.recommendation,
                onDismiss = { /* Dismiss warning */ },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (80 + index * 70).dp)
            )
        }
    }
}

/**
 * Format uptime from milliseconds to MM:SS format.
 */
private fun formatUptime(uptimeMillis: Long): String {
    val totalSeconds = uptimeMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
