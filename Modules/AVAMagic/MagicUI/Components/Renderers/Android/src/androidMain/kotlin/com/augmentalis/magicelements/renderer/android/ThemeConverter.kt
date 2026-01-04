package com.augmentalis.avaelements.renderer.android

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.magicui.components.core.Font
import com.augmentalis.magicui.components.core.Theme
import com.augmentalis.magicui.components.core.Color as MagicColor
import com.augmentalis.magicui.components.core.Typography as MagicTypography
import com.augmentalis.magicui.components.core.Shapes as MagicShapes

/**
 * ThemeConverter - Converts AvaElements Theme to Material3 theme
 *
 * This class handles the conversion of cross-platform AvaElements themes
 * to Android's Material Design 3 theme system.
 */
class ThemeConverter {

    /**
     * Convert AvaElements ColorScheme to Material3 ColorScheme
     */
    fun toMaterialColorScheme(colorScheme: com.augmentalis.avanues.avamagic.components.core.ColorScheme): ColorScheme {
        return ColorScheme(
            primary = colorScheme.primary.toComposeColor(),
            onPrimary = colorScheme.onPrimary.toComposeColor(),
            primaryContainer = colorScheme.primaryContainer.toComposeColor(),
            onPrimaryContainer = colorScheme.onPrimaryContainer.toComposeColor(),

            secondary = colorScheme.secondary.toComposeColor(),
            onSecondary = colorScheme.onSecondary.toComposeColor(),
            secondaryContainer = colorScheme.secondaryContainer.toComposeColor(),
            onSecondaryContainer = colorScheme.onSecondaryContainer.toComposeColor(),

            tertiary = colorScheme.tertiary.toComposeColor(),
            onTertiary = colorScheme.onTertiary.toComposeColor(),
            tertiaryContainer = colorScheme.tertiaryContainer.toComposeColor(),
            onTertiaryContainer = colorScheme.onTertiaryContainer.toComposeColor(),

            error = colorScheme.error.toComposeColor(),
            onError = colorScheme.onError.toComposeColor(),
            errorContainer = colorScheme.errorContainer.toComposeColor(),
            onErrorContainer = colorScheme.onErrorContainer.toComposeColor(),

            surface = colorScheme.surface.toComposeColor(),
            onSurface = colorScheme.onSurface.toComposeColor(),
            surfaceVariant = colorScheme.surfaceVariant.toComposeColor(),
            onSurfaceVariant = colorScheme.onSurfaceVariant.toComposeColor(),

            background = colorScheme.background.toComposeColor(),
            onBackground = colorScheme.onBackground.toComposeColor(),

            outline = colorScheme.outline.toComposeColor(),
            outlineVariant = colorScheme.outlineVariant.toComposeColor(),

            scrim = colorScheme.scrim.toComposeColor(),

            inverseSurface = colorScheme.inverseSurface?.toComposeColor()
                ?: colorScheme.onSurface.toComposeColor(),
            inverseOnSurface = colorScheme.inverseOnSurface?.toComposeColor()
                ?: colorScheme.surface.toComposeColor(),
            inversePrimary = colorScheme.inversePrimary?.toComposeColor()
                ?: colorScheme.primary.toComposeColor(),

            surfaceTint = colorScheme.surfaceTint?.toComposeColor()
                ?: colorScheme.primary.toComposeColor(),

            // Surface container levels
            surfaceDim = colorScheme.surface.toComposeColor(),
            surfaceBright = colorScheme.surfaceVariant.toComposeColor(),
            surfaceContainerLowest = colorScheme.surface.toComposeColor(),
            surfaceContainerLow = colorScheme.surface.toComposeColor(),
            surfaceContainer = colorScheme.surfaceVariant.toComposeColor(),
            surfaceContainerHigh = colorScheme.surfaceVariant.toComposeColor(),
            surfaceContainerHighest = colorScheme.surfaceVariant.toComposeColor()
        )
    }

    /**
     * Convert AvaElements Typography to Material3 Typography
     */
    fun toMaterialTypography(typography: MagicTypography): Typography {
        return Typography(
            displayLarge = typography.displayLarge.toTextStyle(),
            displayMedium = typography.displayMedium.toTextStyle(),
            displaySmall = typography.displaySmall.toTextStyle(),

            headlineLarge = typography.headlineLarge.toTextStyle(),
            headlineMedium = typography.headlineMedium.toTextStyle(),
            headlineSmall = typography.headlineSmall.toTextStyle(),

            titleLarge = typography.titleLarge.toTextStyle(),
            titleMedium = typography.titleMedium.toTextStyle(),
            titleSmall = typography.titleSmall.toTextStyle(),

            bodyLarge = typography.bodyLarge.toTextStyle(),
            bodyMedium = typography.bodyMedium.toTextStyle(),
            bodySmall = typography.bodySmall.toTextStyle(),

            labelLarge = typography.labelLarge.toTextStyle(),
            labelMedium = typography.labelMedium.toTextStyle(),
            labelSmall = typography.labelSmall.toTextStyle()
        )
    }

    /**
     * Convert AvaElements Shapes to Material3 Shapes
     */
    fun toMaterialShapes(shapes: MagicShapes): Shapes {
        return Shapes(
            extraSmall = RoundedCornerShape(shapes.extraSmall.topLeft.dp),
            small = RoundedCornerShape(shapes.small.topLeft.dp),
            medium = RoundedCornerShape(shapes.medium.topLeft.dp),
            large = RoundedCornerShape(shapes.large.topLeft.dp),
            extraLarge = RoundedCornerShape(shapes.extraLarge.topLeft.dp)
        )
    }

    /**
     * Compose wrapper that applies the Material3 theme
     */
    @Composable
    fun WithMaterialTheme(
        theme: Theme,
        content: @Composable () -> Unit
    ) {
        MaterialTheme(
            colorScheme = toMaterialColorScheme(theme.colorScheme),
            typography = toMaterialTypography(theme.typography),
            shapes = toMaterialShapes(theme.shapes),
            content = content
        )
    }
}

/**
 * Extension function to convert AvaElements Color to Compose Color
 */
fun MagicColor.toComposeColor(): Color {
    return Color(
        red = this.red / 255f,
        green = this.green / 255f,
        blue = this.blue / 255f,
        alpha = this.alpha
    )
}

/**
 * Extension function to convert AvaElements Font to Compose TextStyle
 */
fun Font.toTextStyle(): TextStyle {
    return TextStyle(
        fontSize = this.size.sp,
        fontWeight = this.weight.toFontWeight(),
        fontStyle = this.style.toFontStyle()
    )
}

/**
 * Convert AvaElements Font.Weight to Compose FontWeight
 */
fun Font.Weight.toFontWeight(): FontWeight {
    return when (this) {
        Font.Weight.Thin -> FontWeight.Thin
        Font.Weight.ExtraLight -> FontWeight.ExtraLight
        Font.Weight.Light -> FontWeight.Light
        Font.Weight.Regular -> FontWeight.Normal
        Font.Weight.Medium -> FontWeight.Medium
        Font.Weight.SemiBold -> FontWeight.SemiBold
        Font.Weight.Bold -> FontWeight.Bold
        Font.Weight.ExtraBold -> FontWeight.ExtraBold
        Font.Weight.Black -> FontWeight.Black
    }
}

/**
 * Convert AvaElements Font.Style to Compose FontStyle
 */
fun Font.Style.toFontStyle(): FontStyle {
    return when (this) {
        Font.Style.Normal -> FontStyle.Normal
        Font.Style.Italic -> FontStyle.Italic
        Font.Style.Oblique -> FontStyle.Italic // Compose doesn't have oblique
    }
}
