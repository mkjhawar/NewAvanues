package com.augmentalis.voiceui.widgets

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.voiceui.theme.MagicThemeData

/**
 * MagicFloatingActionButton.kt - Themed floating action button widget
 * 
 * Single Responsibility: Provides a themed FAB widget
 * Direct implementation with zero interfaces following VOS4 standards
 */
@Composable
fun MagicFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    theme: MagicThemeData? = null
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = theme?.primary ?: MaterialTheme.colorScheme.primary,
        shape = CircleShape
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}