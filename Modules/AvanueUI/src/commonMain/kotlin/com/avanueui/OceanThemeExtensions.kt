// filename: common/core/Theme/src/commonMain/kotlin/com/augmentalis/ava/core/theme/OceanThemeExtensions.kt
// created: 2025-12-03
// author: AVA AI Team
// (c) Augmentalis Inc, Intelligent Devices LLC

package com.avanueui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ocean Theme Extensions for Material3
 *
 * Provides convenient extension functions and modifiers for applying
 * Ocean theme glassmorphic styling throughout the app.
 *
 * Current: Jetpack Compose + Material3
 * Future: MagicUI (cross-platform)
 *
 * Migration path: When MagicUI becomes available, these extensions
 * will be updated to use MagicUI internally while maintaining the
 * same API surface.
 *
 * @author AVA AI Team
 * @version 1.0.0
 */

// ===== MODIFIER EXTENSIONS =====

/**
 * Apply glassmorphic background styling to any composable.
 *
 * @param intensity Glass opacity level
 * @param shape Corner shape
 * @param showBorder Whether to show border
 */
fun Modifier.glassSurface(
    intensity: GlassIntensity = GlassIntensity.LIGHT,
    shape: Shape = RoundedCornerShape(ShapeTokens.Medium),
    showBorder: Boolean = true
): Modifier = this
    .clip(shape)
    .background(intensity.toColorStatic())
    .then(
        if (showBorder) {
            Modifier.border(
                width = 1.dp,
                color = ColorTokens.Outline,
                shape = shape
            )
        } else Modifier
    )

/**
 * Apply Ocean gradient background.
 */
fun Modifier.oceanGradient(): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                ColorTokens.GradientStart,
                ColorTokens.GradientMid,
                ColorTokens.GradientEnd
            )
        )
    )

/**
 * Apply horizontal Ocean gradient.
 */
fun Modifier.oceanGradientHorizontal(): Modifier = this
    .background(
        brush = Brush.horizontalGradient(
            colors = listOf(
                ColorTokens.GradientStart,
                ColorTokens.GradientEnd
            )
        )
    )

/**
 * Apply glassmorphic card styling.
 */
fun Modifier.glassCard(
    intensity: GlassIntensity = GlassIntensity.MEDIUM,
    cornerRadius: Dp = ShapeTokens.Large
): Modifier = glassSurface(
    intensity = intensity,
    shape = RoundedCornerShape(cornerRadius),
    showBorder = true
)

/**
 * Apply glassmorphic bubble styling for chat messages.
 *
 * @param isUser Whether this is a user message
 */
fun Modifier.glassBubble(
    isUser: Boolean
): Modifier {
    val shape = RoundedCornerShape(
        topStart = ShapeTokens.Large,
        topEnd = ShapeTokens.Large,
        bottomStart = if (isUser) ShapeTokens.Large else ShapeTokens.ExtraSmall,
        bottomEnd = if (isUser) ShapeTokens.ExtraSmall else ShapeTokens.Large
    )

    return if (isUser) {
        this
            .clip(shape)
            .background(ColorTokens.Primary.copy(alpha = 0.9f))
    } else {
        this
            .clip(shape)
            .background(ColorTokens.GlassMedium)
            .border(1.dp, ColorTokens.OutlineVariant, shape)
    }
}

// ===== HELPER FUNCTIONS =====

/**
 * Get glass color for intensity without Composable context.
 * Use for modifier chains where @Composable is not available.
 */
fun GlassIntensity.toColorStatic(): Color = when (this) {
    GlassIntensity.ULTRA_LIGHT -> ColorTokens.GlassUltraLight
    GlassIntensity.LIGHT -> ColorTokens.GlassLight
    GlassIntensity.MEDIUM -> ColorTokens.GlassMedium
    GlassIntensity.HEAVY -> ColorTokens.GlassHeavy
    GlassIntensity.DENSE -> ColorTokens.GlassDense
}

// ===== OCEAN THEME OBJECT EXTENSIONS =====

/**
 * Ocean theme glass colors for easy access.
 */
object OceanGlass {
    // Surface levels
    val ultraLight: Color = ColorTokens.GlassUltraLight
    val light: Color = ColorTokens.GlassLight
    val medium: Color = ColorTokens.GlassMedium
    val heavy: Color = ColorTokens.GlassHeavy
    val dense: Color = ColorTokens.GlassDense

    // Accent colors
    val primary: Color = ColorTokens.Primary
    val secondary: Color = ColorTokens.Secondary

    // Semantic colors with glass effect
    val successGlass: Color = ColorTokens.Success.copy(alpha = 0.15f)
    val warningGlass: Color = ColorTokens.Warning.copy(alpha = 0.15f)
    val errorGlass: Color = ColorTokens.Error.copy(alpha = 0.15f)
    val infoGlass: Color = ColorTokens.Info.copy(alpha = 0.15f)
}

/**
 * Ocean theme gradient brushes.
 */
object OceanGradients {
    val vertical: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                ColorTokens.GradientStart,
                ColorTokens.GradientMid,
                ColorTokens.GradientEnd
            )
        )

    val horizontal: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(
                ColorTokens.GradientStart,
                ColorTokens.GradientEnd
            )
        )

    val radial: Brush
        get() = Brush.radialGradient(
            colors = listOf(
                ColorTokens.GradientStart,
                ColorTokens.GradientEnd
            )
        )

    /**
     * Create a custom gradient from primary to a target color.
     */
    fun toColor(targetColor: Color): Brush = Brush.verticalGradient(
        colors = listOf(ColorTokens.Primary, targetColor)
    )
}

/**
 * Ocean theme shape tokens as RoundedCornerShape.
 */
object OceanShapes {
    val extraSmall: Shape = RoundedCornerShape(ShapeTokens.ExtraSmall)
    val small: Shape = RoundedCornerShape(ShapeTokens.Small)
    val medium: Shape = RoundedCornerShape(ShapeTokens.Medium)
    val large: Shape = RoundedCornerShape(ShapeTokens.Large)
    val extraLarge: Shape = RoundedCornerShape(ShapeTokens.ExtraLarge)
    val full: Shape = RoundedCornerShape(ShapeTokens.Full)

    /**
     * Create asymmetric bubble shape for chat.
     */
    fun bubble(isUser: Boolean): Shape = RoundedCornerShape(
        topStart = ShapeTokens.Large,
        topEnd = ShapeTokens.Large,
        bottomStart = if (isUser) ShapeTokens.Large else ShapeTokens.ExtraSmall,
        bottomEnd = if (isUser) ShapeTokens.ExtraSmall else ShapeTokens.Large
    )
}

// ===== MATERIAL3 COLOR SCHEME EXTENSIONS =====

/**
 * Extension to get glass-enhanced surface colors from MaterialTheme.
 */
object GlassColorScheme {
    val surfaceGlass: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)

    val surfaceVariantGlass: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val primaryContainerGlass: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)

    val errorContainerGlass: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
}

// ===== COMPONENT DEFAULTS =====

/**
 * Default values for glassmorphic components.
 * Centralized for easy adjustment.
 */
object GlassDefaults {
    // Surface
    val defaultSurfaceIntensity = GlassIntensity.LIGHT
    val defaultCardIntensity = GlassIntensity.MEDIUM
    val defaultOverlayIntensity = GlassIntensity.HEAVY

    // Borders
    val borderWidth: Dp = 1.dp
    val borderColor: Color = ColorTokens.Outline
    val borderColorVariant: Color = ColorTokens.OutlineVariant

    // Shapes
    val defaultCornerRadius: Dp = ShapeTokens.Medium
    val cardCornerRadius: Dp = ShapeTokens.Large
    val bubbleCornerRadius: Dp = ShapeTokens.Large
    val chipCornerRadius: Dp = ShapeTokens.Full

    // Animation
    val transitionDuration = 300
    val springStiffness = 400f
    val springDamping = 0.8f

    // Sizing
    val minTouchTarget: Dp = SizeTokens.MinTouchTarget
    val bubbleMaxWidth: Dp = SizeTokens.ChatBubbleMaxWidth
}

/**
 * Blur values for glass effects.
 * Note: Actual blur requires Android 12+ (API 31+) with RenderEffect.
 * These values define the intended blur amount for documentation
 * and future implementation when blur is available.
 */
object GlassBlur {
    val none: Dp = 0.dp
    val subtle: Dp = 4.dp
    val light: Dp = 8.dp
    val medium: Dp = 12.dp
    val heavy: Dp = 16.dp
    val intense: Dp = 24.dp
}
