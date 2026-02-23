package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Minimal frame chrome for smart glass displays.
 *
 * Unlike [FrameWindow] which has macOS-style traffic lights, drag handles,
 * and full desktop chrome, this provides a stripped-down glass-optimized version:
 * - Thin 24dp title strip (no traffic lights, no resize handles)
 * - Text halo (dark shadow outline) for outdoor readability on see-through displays
 * - No window controls — glass frames are managed by voice and head gestures
 *
 * Designed for narrow-FOV glass devices where every pixel counts and
 * decorative UI chrome must be minimized.
 *
 * All colors use [AvanueTheme.colors] — works across all 32 theme combinations.
 *
 * @param title Display name for the frame
 * @param frameNumber 1-based frame index for voice reference ("go to frame 2")
 * @param isSelected Whether this frame is currently focused
 * @param content The actual frame content to render below the title strip
 */
@Composable
fun GlassFrameChrome(
    title: String,
    frameNumber: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        // Minimal title strip
        GlassTitleStrip(
            title = title,
            frameNumber = frameNumber,
            isSelected = isSelected
        )

        // Content area
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * Thin title strip for glass frame chrome.
 *
 * Shows frame number and title with text halo for outdoor readability.
 * Selected frames use the primary accent color; unselected use muted text.
 * Background is 30% opacity surface color to maintain see-through transparency.
 */
@Composable
private fun GlassTitleStrip(
    title: String,
    frameNumber: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isSelected) {
        AvanueTheme.colors.primary
    } else {
        AvanueTheme.colors.textSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(AvanueTheme.colors.surface.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$frameNumber. $title",
            style = haloTextStyle(textColor),
            maxLines = 1
        )
    }
}

/**
 * Text style with dark halo/shadow for outdoor readability on see-through displays.
 *
 * The dark outline (3dp blur, 60% black shadow at 0,0 offset) ensures text remains
 * visible against bright real-world backgrounds seen through optical waveguide displays.
 * This is critical for see-through glasses like Vuzix M4000 or RealWear Navigator
 * where the background is the physical world, not a controlled dark surface.
 *
 * @param color The text foreground color
 * @param fontSize Font size in sp (default 14)
 */
fun haloTextStyle(
    color: Color,
    fontSize: Int = 14
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.6f),
        offset = Offset(0f, 0f),
        blurRadius = 3f
    )
)

/**
 * Peek panel strip shown at the edge of FOV on medium-FOV glass devices.
 *
 * Shows adjacent frame title as a narrow strip (24dp wide) at the left or right
 * edge of the viewport. On head-turn toward the peek panel, the adjacent frame
 * should expand/transition into view (handled by the parent spatial controller).
 *
 * Uses 20% opacity surface background and muted text for minimal visual intrusion.
 *
 * @param title The title of the adjacent frame shown in the peek panel
 * @param isLeft Whether this peek panel is on the left edge (true) or right edge (false)
 */
@Composable
fun PeekPanel(
    title: String,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd

    Box(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .background(AvanueTheme.colors.surface.copy(alpha = 0.2f)),
        contentAlignment = alignment
    ) {
        Text(
            text = title,
            style = haloTextStyle(
                color = AvanueTheme.colors.textTertiary,
                fontSize = 10
            ),
            maxLines = 1,
            modifier = Modifier.padding(2.dp)
        )
    }
}

/**
 * Status strip for glass displays — shown at top of the glass viewport.
 *
 * Displays the active frame index and total frame count for voice navigation
 * ("go to frame 2 of 4"). Uses transparent background and halo text
 * for minimal visual intrusion on see-through displays.
 *
 * @param frameCount Total number of frames in the session
 * @param activeFrameIndex 0-based index of the currently active frame
 */
@Composable
fun GlassStatusStrip(
    frameCount: Int,
    activeFrameIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Color.Transparent)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Frame ${activeFrameIndex + 1}/$frameCount",
            style = haloTextStyle(
                color = AvanueTheme.colors.textTertiary,
                fontSize = 11
            )
        )
    }
}
