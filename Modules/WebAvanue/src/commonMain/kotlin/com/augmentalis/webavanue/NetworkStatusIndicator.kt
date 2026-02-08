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
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.avanueui.tokens.ElevationTokens
import com.augmentalis.webavanue.IconVariant
import com.augmentalis.webavanue.toColor
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
                    horizontal = SpacingTokens.md,
                    vertical = SpacingTokens.sm
                ),
            color = AvanueTheme.colors.surfaceElevated,
            shape = RoundedCornerShape(ShapeTokens.sm),
            tonalElevation = ElevationTokens.md,
            shadowElevation = ElevationTokens.lg
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
            .padding(SpacingTokens.md),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconVariant.toColor(),
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconVariant.toColor(),
                modifier = Modifier.size(32.dp)
            )
        }

        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AvanueTheme.colors.textPrimary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textSecondary
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
