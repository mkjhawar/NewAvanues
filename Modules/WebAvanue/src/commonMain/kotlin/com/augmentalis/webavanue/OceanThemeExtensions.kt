package com.augmentalis.webavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.avanueui.OceanTheme

/**
 * Ocean Theme Extensions
 *
 * Modifiers, gradients, shapes, and defaults for glassmorphic Ocean theme.
 * These extensions provide reusable styling utilities that map 1:1 to MagicUI.
 *
 * MIGRATION NOTE:
 * When migrating to MagicUI, these modifiers will delegate to MagicUI's
 * built-in glass effects, gradients, and shape tokens.
 */

// ============================================================================
// GLASS MODIFIERS
// ============================================================================

/**
 * Apply glassmorphic effect to any composable
 *
 * MagicUI equivalent: Modifier.magicGlass()
 *
 * @param backgroundColor Base color (will be made translucent)
 * @param glassLevel Effect strength (LIGHT, MEDIUM, HEAVY)
 * @param border Optional border configuration
 * @param shape Shape for border (if border provided)
 */
fun Modifier.glass(
    backgroundColor: Color,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    border: GlassBorder? = null,
    shape: Shape = GlassDefaults.shape
): Modifier {
    val (opacity, blurRadius) = when (glassLevel) {
        GlassLevel.LIGHT -> 0.05f to 6.dp
        GlassLevel.MEDIUM -> 0.08f to 8.dp
        GlassLevel.HEAVY -> 0.12f to 10.dp
    }

    return this
        .background(backgroundColor.copy(alpha = opacity))
        .blur(blurRadius)
        .then(
            if (border != null) {
                Modifier.border(
                    width = border.width,
                    color = border.color,
                    shape = shape
                )
            } else Modifier
        )
}

/**
 * Apply light glassmorphic effect (8% opacity, 8dp blur)
 *
 * MagicUI equivalent: Modifier.magicGlassLight()
 */
fun Modifier.glassLight(
    backgroundColor: Color = OceanTheme.surface,
    border: GlassBorder? = GlassDefaults.borderSubtle
): Modifier = glass(backgroundColor, GlassLevel.LIGHT, border)

/**
 * Apply medium glassmorphic effect (12% opacity, 12dp blur)
 *
 * MagicUI equivalent: Modifier.magicGlassMedium()
 */
fun Modifier.glassMedium(
    backgroundColor: Color = OceanTheme.surface,
    border: GlassBorder? = GlassDefaults.border
): Modifier = glass(backgroundColor, GlassLevel.MEDIUM, border)

/**
 * Apply heavy glassmorphic effect (20% opacity, 16dp blur)
 *
 * MagicUI equivalent: Modifier.magicGlassHeavy()
 */
fun Modifier.glassHeavy(
    backgroundColor: Color = OceanTheme.surface,
    border: GlassBorder? = GlassDefaults.borderStrong
): Modifier = glass(backgroundColor, GlassLevel.HEAVY, border)

/**
 * Apply frosted glass effect with gradient
 *
 * MagicUI equivalent: Modifier.magicFrosted()
 */
fun Modifier.glassFrosted(
    gradient: Brush = OceanGradients.surfaceGradient,
    blurRadius: Dp = 10.dp,
    border: GlassBorder? = GlassDefaults.border
): Modifier = this
    .background(gradient)
    .blur(blurRadius)
    .then(
        if (border != null) {
            Modifier.border(
                width = border.width,
                color = border.color,
                shape = GlassDefaults.shape
            )
        } else Modifier
    )

// ============================================================================
// OCEAN GLASS - Preset glass styles
// ============================================================================

/**
 * Ocean Glass presets for common use cases
 *
 * MagicUI equivalent: MagicUI.GlassPresets
 */
object OceanGlass {
    /**
     * Card glass (medium effect, standard border)
     */
    fun Modifier.card() = glassMedium(
        backgroundColor = OceanTheme.surface,
        border = GlassDefaults.border
    )

    /**
     * Surface glass (light effect, subtle border)
     */
    fun Modifier.surface() = glassLight(
        backgroundColor = OceanTheme.surface,
        border = GlassDefaults.borderSubtle
    )

    /**
     * Elevated glass (heavy effect, strong border)
     */
    fun Modifier.elevated() = glassHeavy(
        backgroundColor = OceanTheme.surfaceElevated,
        border = GlassDefaults.borderStrong
    )

    /**
     * Dialog glass (heavy effect with gradient)
     */
    fun Modifier.dialog() = glassFrosted(
        gradient = OceanGradients.dialogGradient,
        blurRadius = 10.dp,
        border = GlassDefaults.border
    )

    /**
     * Bubble glass (medium effect, no border)
     */
    fun Modifier.bubble() = glassMedium(
        backgroundColor = OceanTheme.surface,
        border = null
    )

    /**
     * Button glass (light effect with primary color)
     */
    fun Modifier.button() = glassLight(
        backgroundColor = OceanTheme.primary,
        border = GlassBorder(
            width = 1.dp,
            color = OceanTheme.primary.copy(alpha = 0.5f)
        )
    )

    /**
     * Chip glass (light effect, subtle border)
     */
    fun Modifier.chip() = glassLight(
        backgroundColor = OceanTheme.surfaceElevated,
        border = GlassDefaults.borderSubtle
    )
}

// ============================================================================
// OCEAN GRADIENTS
// ============================================================================

/**
 * Gradient presets for Ocean theme
 *
 * MagicUI equivalent: MagicUI.Gradients
 */
object OceanGradients {
    /**
     * Surface gradient (subtle slate gradient)
     */
    val surfaceGradient = Brush.verticalGradient(
        colors = listOf(
            OceanTheme.surface.copy(alpha = 0.15f),
            OceanTheme.surfaceElevated.copy(alpha = 0.10f)
        )
    )

    /**
     * Dialog gradient (stronger gradient for elevated surfaces)
     */
    val dialogGradient = Brush.verticalGradient(
        colors = listOf(
            OceanTheme.surfaceElevated.copy(alpha = 0.25f),
            OceanTheme.surface.copy(alpha = 0.15f)
        )
    )

    /**
     * Primary gradient (blue gradient for buttons/accents)
     */
    val primaryGradient = Brush.horizontalGradient(
        colors = listOf(
            OceanTheme.primary.copy(alpha = 0.20f),
            OceanTheme.primaryLight.copy(alpha = 0.15f)
        )
    )

    /**
     * Shimmer gradient (animated loading effect)
     */
    val shimmerGradient = Brush.horizontalGradient(
        colors = listOf(
            OceanTheme.surface.copy(alpha = 0.05f),
            OceanTheme.primary.copy(alpha = 0.10f),
            OceanTheme.surface.copy(alpha = 0.05f)
        )
    )

    /**
     * Success gradient (green gradient for success states)
     */
    val successGradient = Brush.horizontalGradient(
        colors = listOf(
            OceanTheme.success.copy(alpha = 0.20f),
            OceanTheme.success.copy(alpha = 0.10f)
        )
    )

    /**
     * Error gradient (red gradient for error states)
     */
    val errorGradient = Brush.horizontalGradient(
        colors = listOf(
            OceanTheme.error.copy(alpha = 0.20f),
            OceanTheme.error.copy(alpha = 0.10f)
        )
    )

    /**
     * Warning gradient (amber gradient for warning states)
     */
    val warningGradient = Brush.horizontalGradient(
        colors = listOf(
            OceanTheme.warning.copy(alpha = 0.20f),
            OceanTheme.warning.copy(alpha = 0.10f)
        )
    )

    /**
     * Radial gradient (for spotlight effects)
     */
    val radialGradient = Brush.radialGradient(
        colors = listOf(
            OceanTheme.primary.copy(alpha = 0.15f),
            OceanTheme.surface.copy(alpha = 0.05f),
            Color.Transparent
        )
    )
}

// ============================================================================
// OCEAN SHAPES
// ============================================================================

/**
 * Shape presets for Ocean theme
 *
 * MagicUI equivalent: MagicUI.Shapes
 */
object GlassShapes {
    /**
     * Default shape (12dp rounded corners)
     */
    val default = RoundedCornerShape(12.dp)

    /**
     * Small shape (8dp rounded corners)
     */
    val small = RoundedCornerShape(8.dp)

    /**
     * Large shape (16dp rounded corners)
     */
    val large = RoundedCornerShape(16.dp)

    /**
     * Extra large shape (24dp rounded corners)
     */
    val extraLarge = RoundedCornerShape(24.dp)

    /**
     * Chip shape (8dp rounded corners)
     */
    val chipShape = RoundedCornerShape(8.dp)

    /**
     * Button shape (12dp rounded corners)
     */
    val buttonShape = RoundedCornerShape(12.dp)

    /**
     * FAB shape (16dp rounded corners)
     */
    val fabShape = RoundedCornerShape(16.dp)

    /**
     * Dialog shape (16dp rounded corners)
     */
    val dialogShape = RoundedCornerShape(16.dp)

    /**
     * Bottom sheet shape (16dp top corners only)
     */
    val bottomSheetShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    /**
     * Bubble shape - start aligned (incoming message)
     */
    val bubbleStart = RoundedCornerShape(
        topStart = 2.dp,
        topEnd = 12.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )

    /**
     * Bubble shape - end aligned (outgoing message)
     */
    val bubbleEnd = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 2.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )

    /**
     * Circle shape (for avatars, icons)
     */
    val circle = CircleShape
}

// ============================================================================
// GLASS DEFAULTS
// ============================================================================

/**
 * Default values for glass components
 *
 * MagicUI equivalent: MagicUI.Defaults
 */
object GlassDefaults {
    /**
     * Default shape (12dp rounded corners)
     */
    val shape: Shape = GlassShapes.default

    /**
     * Default border (1dp, white 30% opacity)
     */
    val border = GlassBorder(
        width = 1.dp,
        color = OceanTheme.border
    )

    /**
     * Subtle border (0.5dp, white 10% opacity)
     */
    val borderSubtle = GlassBorder(
        width = 0.5.dp,
        color = OceanTheme.borderSubtle
    )

    /**
     * Strong border (1.5dp, white 30% opacity)
     */
    val borderStrong = GlassBorder(
        width = 1.5.dp,
        color = OceanTheme.borderStrong
    )

    /**
     * Focused border (2dp, primary color)
     */
    val borderFocused = GlassBorder(
        width = 2.dp,
        color = OceanTheme.borderFocused
    )

    /**
     * Default card colors for glass cards
     */
    @Composable
    fun cardColors(): CardColors = CardDefaults.cardColors(
        containerColor = OceanTheme.surface,
        contentColor = OceanTheme.textPrimary,
        disabledContainerColor = OceanTheme.surface.copy(alpha = 0.5f),
        disabledContentColor = OceanTheme.textDisabled
    )

    /**
     * Default spacing (16dp)
     */
    val spacing: Dp = 16.dp

    /**
     * Small spacing (8dp)
     */
    val spacingSmall: Dp = 8.dp

    /**
     * Large spacing (24dp)
     */
    val spacingLarge: Dp = 24.dp

    /**
     * Extra large spacing (32dp)
     */
    val spacingExtraLarge: Dp = 32.dp

    /**
     * Minimum touch target (48dp)
     */
    val minTouchTarget: Dp = 48.dp

    /**
     * Default elevation (0dp - glass effect instead)
     */
    val elevation: Dp = 0.dp

    /**
     * Elevated surface elevation (2dp)
     */
    val elevationElevated: Dp = 2.dp

    /**
     * Dialog elevation (6dp)
     */
    val elevationDialog: Dp = 6.dp
}

// ============================================================================
// HELPER EXTENSIONS
// ============================================================================

/**
 * Apply standard Ocean theme padding
 */
fun Modifier.oceanPadding() = this.then(
    Modifier.padding(GlassDefaults.spacing)
)

/**
 * Apply small Ocean theme padding
 */
fun Modifier.oceanPaddingSmall() = this.then(
    Modifier.padding(GlassDefaults.spacingSmall)
)

/**
 * Apply large Ocean theme padding
 */
fun Modifier.oceanPaddingLarge() = this.then(
    Modifier.padding(GlassDefaults.spacingLarge)
)

/**
 * Ensure minimum touch target size (48x48dp)
 */
fun Modifier.oceanMinTouchTarget() = this.then(
    Modifier.sizeIn(
        minWidth = GlassDefaults.minTouchTarget,
        minHeight = GlassDefaults.minTouchTarget
    )
)
