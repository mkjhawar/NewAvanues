package com.augmentalis.cockpit.mvp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.components.GlassmorphicCard
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowType

@Composable
fun WindowCard(
    window: AppWindow,
    color: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false
) {
    GlassmorphicCard(
        modifier = modifier
            .width(OceanTheme.windowWidthDefault)
            .height(OceanTheme.windowHeightDefault),
        isFocused = isFocused
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Accent color indicator at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(android.graphics.Color.parseColor(color)))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Window header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = window.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = OceanTheme.textPrimary
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OceanTheme.textPrimary
                        )
                    }
                }

                // Window content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = getWindowTypeLabel(window.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTheme.textSecondary
                    )
                    Text(
                        text = "Voice: \"${window.voiceName}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTheme.textSecondary
                    )
                    Text(
                        text = "Position: (${window.position.x.format()}, ${window.position.y.format()}, ${window.position.z.format()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTheme.textTertiary
                    )
                }
            }
        }
    }
}

private fun getWindowTypeLabel(type: WindowType): String = when (type) {
    WindowType.ANDROID_APP -> "Android App"
    WindowType.WEB_APP -> "Web App"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
    WindowType.WIDGET -> "Widget"
}

private fun Float.format() = "%.2f".format(this)
