package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CockpitFrame

/**
 * Horizontal taskbar showing minimized frame tabs.
 * Tapping a tab restores the frame to its previous position.
 */
@Composable
fun MinimizedTaskbar(
    frames: List<CockpitFrame>,
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(colors.surface.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        frames.forEach { frame ->
            val chipShape = RoundedCornerShape(6.dp)
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(chipShape)
                    .background(colors.surface, chipShape)
                    .clickable { onRestore(frame.id) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = frame.title.ifBlank { frame.content.type() },
                    color = colors.onSurface.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun com.augmentalis.cockpit.model.FrameContent.type(): String = when (this) {
    is com.augmentalis.cockpit.model.FrameContent.Web -> "Web"
    is com.augmentalis.cockpit.model.FrameContent.Pdf -> "PDF"
    is com.augmentalis.cockpit.model.FrameContent.Image -> "Image"
    is com.augmentalis.cockpit.model.FrameContent.Video -> "Video"
    is com.augmentalis.cockpit.model.FrameContent.Note -> "Note"
    is com.augmentalis.cockpit.model.FrameContent.Camera -> "Camera"
    is com.augmentalis.cockpit.model.FrameContent.VoiceNote -> "Voice"
    is com.augmentalis.cockpit.model.FrameContent.Form -> "Form"
    is com.augmentalis.cockpit.model.FrameContent.Signature -> "Signature"
    is com.augmentalis.cockpit.model.FrameContent.Map -> "Map"
    is com.augmentalis.cockpit.model.FrameContent.Whiteboard -> "Whiteboard"
    is com.augmentalis.cockpit.model.FrameContent.Terminal -> "Terminal"
    is com.augmentalis.cockpit.model.FrameContent.AiSummary -> "AI Summary"
    is com.augmentalis.cockpit.model.FrameContent.ScreenCast -> "Cast"
    is com.augmentalis.cockpit.model.FrameContent.Widget -> "Widget"
}
