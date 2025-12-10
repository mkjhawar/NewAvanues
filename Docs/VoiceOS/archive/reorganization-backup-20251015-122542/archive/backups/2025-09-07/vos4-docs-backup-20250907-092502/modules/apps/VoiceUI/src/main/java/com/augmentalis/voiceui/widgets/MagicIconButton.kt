package com.augmentalis.voiceui.widgets

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.voiceui.theme.MagicThemeData

/**
 * MagicIconButton.kt - Themed icon button widget
 * 
 * Single Responsibility: Provides a themed icon button widget
 * Direct implementation with zero interfaces following VOS4 standards
 */
@Composable
fun MagicIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    theme: MagicThemeData? = null
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = theme?.primary ?: MaterialTheme.colorScheme.primary
        )
    }
}