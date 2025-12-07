package com.augmentalis.voiceui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MagicRow.kt - Horizontal layout widget with customizable spacing
 * 
 * Single Responsibility: Provides a horizontal row layout with spacing control
 * Direct implementation with zero interfaces following VOS4 standards
 * Supports both "gap" and "spacing" parameters for compatibility
 */
@Composable
fun MagicRow(
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    spacing: Dp = gap, // Support both "gap" and "spacing" parameters
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (spacing > 0.dp) Arrangement.spacedBy(spacing) else horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}