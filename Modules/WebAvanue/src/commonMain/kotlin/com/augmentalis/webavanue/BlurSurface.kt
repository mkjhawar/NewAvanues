package com.augmentalis.webavanue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.glassmorphism
import com.augmentalis.webavanue.supportsBlur
import com.augmentalis.webavanue.LocalAppColors

/**
 * BlurSurface - Glassmorphism surface with automatic API detection
 *
 * Provides a reusable glass surface component that:
 * - Android 12+ (API 31+): True backdrop blur with translucent surface
 * - Android 11 and below: Solid surface with same visual style
 *
 * Usage:
 * ```kotlin
 * BlurSurface(
 *     intensity = GlassIntensity.Medium,
 *     modifier = Modifier.fillMaxWidth()
 * ) {
 *     // Your content here
 * }
 * ```
 */

/**
 * Glass intensity levels
 */
enum class GlassIntensity {
    Light,   // 5% opacity, 8px blur
    Medium,  // 8% opacity, 12px blur
    Heavy    // 12% opacity, 16px blur
}

/**
 * BlurSurface composable
 *
 * @param modifier Modifier for customization
 * @param intensity Glass effect intensity
 * @param shape Surface shape (default: rounded 12dp)
 * @param elevation Shadow elevation in dp
 * @param border Optional border stroke
 * @param enableGlass Enable glassmorphism effect (true by default)
 * @param content Composable content
 */
@Composable
fun BlurSurface(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.Medium,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 4.dp,
    border: BorderStroke? = null,
    enableGlass: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    val hasBlur = supportsBlur()

    // Get glass parameters based on intensity
    val (blurRadius, backgroundColor, borderColor) = when (intensity) {
        GlassIntensity.Light -> Triple(
            8f,
            if (hasBlur && enableGlass) Color(0x0D1E293B) else Color(0xFF1E293B),
            Color(0x262563EB)
        )
        GlassIntensity.Medium -> Triple(
            12f,
            if (hasBlur && enableGlass) Color(0x14334155) else Color(0xFF334155),
            Color(0x332563EB)
        )
        GlassIntensity.Heavy -> Triple(
            16f,
            if (hasBlur && enableGlass) Color(0x1F334155) else Color(0xFF475569),
            Color(0x402563EB)
        )
    }

    if (hasBlur && enableGlass) {
        // API 31+: Use glassmorphism with blur
        Box(
            modifier = modifier
                .glassmorphism(
                    blurRadius = blurRadius,
                    backgroundColor = backgroundColor,
                    borderColor = borderColor,
                    borderWidth = 1f
                )
        ) {
            Surface(
                modifier = Modifier,
                shape = shape,
                color = Color.Transparent,
                shadowElevation = elevation,
                border = border,
                content = content
            )
        }
    } else {
        // API 30 and below: Use solid surface
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            shadowElevation = elevation,
            border = border ?: BorderStroke(1.dp, borderColor),
            content = content
        )
    }
}

/**
 * Glass card - Pre-configured BlurSurface for card-like elements
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.Medium,
    content: @Composable () -> Unit
) {
    BlurSurface(
        modifier = modifier,
        intensity = intensity,
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        content = content
    )
}

/**
 * Glass panel - Pre-configured BlurSurface for panel/section elements
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.Light,
    content: @Composable () -> Unit
) {
    BlurSurface(
        modifier = modifier,
        intensity = intensity,
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        content = content
    )
}

/**
 * Glass button surface - Pre-configured for button-like elements
 */
@Composable
fun GlassButton(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.Medium,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BlurSurface(
        modifier = modifier,
        intensity = intensity,
        shape = RoundedCornerShape(8.dp),
        elevation = 6.dp,
        content = content
    )
}
