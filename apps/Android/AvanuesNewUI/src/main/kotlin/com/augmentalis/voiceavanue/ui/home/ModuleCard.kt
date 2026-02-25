/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.foundation.state.ServiceState

/**
 * Returns the status border color: green=running, blue=ready, orange=degraded, red=error/stopped.
 */
@Composable
fun statusBorderColor(state: ServiceState): androidx.compose.ui.graphics.Color {
    return when (state) {
        is ServiceState.Running -> AvanueTheme.colors.success
        is ServiceState.Ready -> AvanueTheme.colors.info
        is ServiceState.Degraded -> AvanueTheme.colors.warning
        is ServiceState.Error -> AvanueTheme.colors.error
        is ServiceState.Stopped -> AvanueTheme.colors.error
    }
}

/**
 * Returns a human-readable status label and color for a ServiceState.
 */
@Composable
fun statusLabel(state: ServiceState): Pair<String, androidx.compose.ui.graphics.Color> {
    return when (state) {
        is ServiceState.Running -> "ON" to AvanueTheme.colors.success
        is ServiceState.Ready -> "READY" to AvanueTheme.colors.info
        is ServiceState.Degraded -> "DEGRADED" to AvanueTheme.colors.warning
        is ServiceState.Error -> "ERROR" to AvanueTheme.colors.error
        is ServiceState.Stopped -> "OFF" to AvanueTheme.colors.error
    }
}

/**
 * Returns a recognizable icon for each module type.
 */
fun moduleIcon(moduleId: String): ImageVector {
    return when (moduleId) {
        "voiceavanue" -> Icons.Default.Mic
        "webavanue" -> Icons.Default.Public
        "voicecursor" -> Icons.Default.Mouse
        else -> Icons.Default.Apps
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ModuleCard(module: ModuleStatus, onClick: () -> Unit) {
    val borderColor = statusBorderColor(module.state)
    val icon = moduleIcon(module.moduleId)
    val (statusText, statusColor) = statusLabel(module.state)
    val isStopped = module.state is ServiceState.Stopped
    val contentAlpha = if (isStopped) 0.6f else 1f

    AvanueCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().fillMaxHeight().heightIn(min = 56.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 200.dp
            val cardPadding = if (isCompact) SpacingTokens.sm else SpacingTokens.md

            Row(
                modifier = Modifier.fillMaxWidth().padding(cardPadding),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Module icon with status dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 32.dp else 40.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = borderColor.copy(alpha = contentAlpha),
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(borderColor)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = module.displayName,
                            style = if (isCompact) MaterialTheme.typography.labelLarge
                                   else MaterialTheme.typography.titleSmall,
                            color = AvanueTheme.colors.textPrimary.copy(alpha = contentAlpha),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary.copy(alpha = contentAlpha),
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (module.metadata.isNotEmpty()) {
                        Text(
                            text = module.metadata.entries.joinToString(" \u00B7 ") { "${it.key}: ${it.value}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = AvanueTheme.colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = AvanueTheme.colors.textDisabled,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
