package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * DesktopModeIndicator - Shows when desktop mode is active
 *
 * Features:
 * - Small badge/chip indicating desktop mode
 * - Animated appearance/disappearance
 * - Dark 3D theme matching command bar
 * - Clickable to toggle desktop mode
 *
 * @param isDesktopMode Whether desktop mode is currently active
 * @param onClick Callback when indicator is clicked (to toggle mode)
 * @param modifier Modifier for customization
 */
@Composable
fun DesktopModeIndicator(
    isDesktopMode: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDesktopMode,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            color = Color(0xFF60A5FA).copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.height(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Desktop Mode",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Desktop",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * CompactDesktopModeIndicator - Minimal version showing just an icon
 *
 * Used in space-constrained areas like AddressBar
 */
@Composable
fun CompactDesktopModeIndicator(
    isDesktopMode: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDesktopMode,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = Color(0xFF60A5FA).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Desktop Mode Active",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
