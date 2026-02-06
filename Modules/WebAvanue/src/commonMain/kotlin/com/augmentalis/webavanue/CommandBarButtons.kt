package com.augmentalis.webavanue

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.core.theme.OceanTheme
import com.augmentalis.webavanue.AppIcon
import com.augmentalis.webavanue.IconVariant
import com.augmentalis.webavanue.OceanDesignTokens

/**
 * CommandButton - Standard command button with icon and label
 *
 * Design:
 * - 46dp size optimized for 6 buttons in portrait mode
 * - Glassmorphic surface with border
 * - Icon + label layout
 * - Uses OceanDesignTokens for all colors and spacing
 * - MagicUI-ready architecture
 */
@Composable
fun CommandButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    backgroundColor: Color = OceanDesignTokens.Surface.elevated,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    // FIX: Reduced button size from 48dp to 46dp to fit 6 buttons in portrait without scrolling
    // 6 buttons × 46dp + 5 gaps × 4dp + 2 padding × 10dp = 276 + 20 + 20 = 316dp (fits 360dp screens)
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(46.dp)
            .height(46.dp),
        shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.md),
        color = if (isActive) OceanDesignTokens.Surface.primary else backgroundColor,
        border = BorderStroke(1.dp, OceanDesignTokens.Border.default),
        shadowElevation = OceanDesignTokens.Elevation.sm
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(OceanDesignTokens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = label,
                variant = if (isActive) IconVariant.OnPrimary else IconVariant.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) OceanDesignTokens.Text.onPrimary else OceanDesignTokens.Text.secondary,
                maxLines = 1
            )
        }
    }
}

/**
 * ZoomLevelButton - Compact button showing zoom percentage
 */
@Composable
fun ZoomLevelButton(
    label: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(48.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = OceanTheme.surfaceElevated,
        border = BorderStroke(1.dp, OceanTheme.border),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = OceanTheme.textPrimary
            )
        }
    }
}

/**
 * VoiceButton - Voice input button with pulse animation
 */
@Composable
fun VoiceButton(
    isListening: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    activeColor: Color = OceanTheme.primary,
    backgroundColor: Color = OceanTheme.surfaceElevated,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = pulseAlpha))
            )
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isListening) activeColor else backgroundColor)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Stop listening" else "Voice input",
                tint = OceanTheme.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * DesktopModeButton - Toggle between desktop and mobile mode
 */
@Composable
fun DesktopModeButton(
    isDesktopMode: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    activeColor: Color = OceanTheme.primary,
    backgroundColor: Color = OceanTheme.surfaceElevated,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(48.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDesktopMode) activeColor else backgroundColor,
        border = BorderStroke(1.dp, OceanTheme.border),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                contentDescription = if (isDesktopMode) "Exit Desktop Mode" else "Enter Desktop Mode",
                tint = if (isDesktopMode) OceanTheme.textOnPrimary else OceanTheme.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isDesktopMode) "Mobile" else "Desktop",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDesktopMode) OceanTheme.textOnPrimary else OceanTheme.textSecondary,
                maxLines = 1
            )
        }
    }
}
