package com.augmentalis.webavanue.ui.screen.effects

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * BlurEffect - Platform-specific blur implementation for Android
 *
 * Currently uses translucent surfaces as blur is complex in Compose.
 * Future: Can add RenderEffect blur for API 31+ when Compose support matures.
 */

/**
 * Check if device supports native blur effects
 */
actual fun supportsBlur(): Boolean {
    // For now, return false until we implement proper RenderEffect support
    // Future: return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return false
}

/**
 * Apply glassmorphism effect with translucent surface
 *
 * @param blurRadius Blur radius (currently unused, reserved for future)
 * @param backgroundColor Background/tint color with alpha
 * @param borderColor Optional border color for glass edge effect
 * @param borderWidth Border width
 */
@Composable
actual fun Modifier.glassmorphism(
    blurRadius: Float,
    backgroundColor: Color,
    borderColor: Color?,
    borderWidth: Float
): Modifier {
    return this.drawBehind {
        // Draw translucent background
        drawRect(backgroundColor)

        // Draw border if specified
        borderColor?.let { color ->
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                size = size,
                style = Stroke(width = borderWidth)
            )
        }
    }
}
