package com.augmentalis.voiceui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceui.theme.MagicThemeData

/**
 * MagicCard.kt - Themed card widget
 * 
 * Single Responsibility: Provides a themed card container widget
 * Direct implementation with zero interfaces following VOS4 standards
 */
@Composable
fun MagicCard(
    modifier: Modifier = Modifier,
    theme: MagicThemeData? = null,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        elevation = elevation,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme?.surface ?: MaterialTheme.colorScheme.surface
        )
    ) {
        Column(content = content)
    }
}