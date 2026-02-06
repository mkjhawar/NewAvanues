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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.avanues.themes.OceanTheme
import com.augmentalis.webavanue.AppIcon
import com.augmentalis.webavanue.IconVariant
import com.augmentalis.webavanue.OceanDesignTokens
import kotlinx.coroutines.delay

/**
 * Network connection status
 */
enum class NetworkStatus {
    CONNECTED,      // Normal operation
    DISCONNECTED,   // No network
    SLOW,           // Poor connection
    RECONNECTING    // Attempting to reconnect
}

/**
 * NetworkStatusIndicator - Visual feedback for network connectivity
 *
 * FR-008: Network Status Monitoring
 * - Shows visual alert when network is unavailable
 * - Displays connection quality indicators
 * - Auto-hides when connection is stable
 * - Glassmorphic Ocean theme styling
 *
 * @param status Current network status
 * @param modifier Modifier for customization
 * @param autoHideDuration Duration to show stable connection before hiding (ms)
 */
@Composable
fun NetworkStatusIndicator(
    status: NetworkStatus,
    modifier: Modifier = Modifier,
    autoHideDuration: Long = 3000
) {
    // Track if we should show the indicator
    var showIndicator by remember { mutableStateOf(false) }
    var lastStatus by remember { mutableStateOf(status) }

    // Show indicator when status changes or when not connected
    LaunchedEffect(status) {
        when {
            status != NetworkStatus.CONNECTED -> {
                // Always show for non-connected states
                showIndicator = true
            }
            lastStatus != NetworkStatus.CONNECTED && status == NetworkStatus.CONNECTED -> {
                // Just reconnected - show briefly then hide
                showIndicator = true
                delay(autoHideDuration)
                showIndicator = false
            }
            else -> {
                // Already connected and was connected - hide
                showIndicator = false
            }
        }
        lastStatus = status
    }

    AnimatedVisibility(
        visible = showIndicator,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = OceanDesignTokens.Spacing.lg,
                    vertical = OceanDesignTokens.Spacing.sm
                ),
            color = OceanDesignTokens.Surface.elevated,
            shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.md),
            tonalElevation = OceanDesignTokens.Elevation.md,
            shadowElevation = OceanDesignTokens.Elevation.lg
        ) {
            Column {
                NetworkStatusContent(status = status)
            }
        }
    }
}

@Composable
private fun ColumnScope.NetworkStatusContent(status: NetworkStatus) {
    val (icon, iconVariant, title, message) = when (status) {
        NetworkStatus.CONNECTED -> {
            Quadruple(
                Icons.Default.CheckCircle,
                IconVariant.Success,
                "Connected",
                "Network connection restored"
            )
        }
        NetworkStatus.DISCONNECTED -> {
            Quadruple(
                Icons.Default.WifiOff,
                IconVariant.Error,
                "No Connection",
                "Check your network settings"
            )
        }
        NetworkStatus.SLOW -> {
            Quadruple(
                Icons.Default.SignalCellularConnectedNoInternet0Bar,
                IconVariant.Warning,
                "Slow Connection",
                "Network performance may be degraded"
            )
        }
        NetworkStatus.RECONNECTING -> {
            Quadruple(
                Icons.Default.Sync,
                IconVariant.Primary,
                "Reconnecting",
                "Attempting to restore connection..."
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(OceanDesignTokens.Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with optional rotation animation for reconnecting
        if (status == NetworkStatus.RECONNECTING) {
            val infiniteTransition = rememberInfiniteTransition(label = "reconnect")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            AppIcon(
                imageVector = icon,
                contentDescription = title,
                variant = iconVariant,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            )
        } else {
            AppIcon(
                imageVector = icon,
                contentDescription = title,
                variant = iconVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OceanDesignTokens.Text.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = OceanDesignTokens.Text.secondary
            )
        }
    }
}

/**
 * Helper data class for network status details
 * Updated to use IconVariant instead of Color for icon styling
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Platform-specific network status monitor
 * Expect declaration - implemented differently on each platform
 */
@Composable
expect fun rememberNetworkStatusMonitor(): NetworkStatus
