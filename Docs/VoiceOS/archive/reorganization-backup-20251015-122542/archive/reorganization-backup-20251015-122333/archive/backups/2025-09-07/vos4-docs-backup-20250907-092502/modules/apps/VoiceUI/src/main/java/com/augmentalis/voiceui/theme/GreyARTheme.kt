package com.augmentalis.voiceui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush

/**
 * GreyAR Theme - Glassmorphic AR-style theme for VoiceUI
 * Matches the visual style shown in the AR glasses interface
 */

// Color Palette
object GreyARColors {
    // Primary colors from the image
    val CardBackground = Color(0xCC2C2C2C)  // Dark grey with 80% opacity
    val CardBackgroundLight = Color(0xB3404040)  // Lighter grey variant
    val Surface = Color(0xE6383838)  // Surface color with 90% opacity
    
    // Text colors
    val TextPrimary = Color(0xFFFFFFFF)  // Pure white for headers
    val TextSecondary = Color(0xFFE0E0E0)  // Light grey for body text
    val TextTertiary = Color(0xFFB0B0B0)  // Dimmed text
    val TextHint = Color(0xFF808080)  // Hint/placeholder text
    
    // Accent colors (from the blue buttons)
    val AccentBlue = Color(0xFF2196F3)  // Primary blue from "Get started" button
    val AccentBlueLight = Color(0xFF42A5F5)
    val AccentBlueDark = Color(0xFF1976D2)
    
    // Background
    val Background = Color(0x00000000)  // Transparent for AR overlay
    val BackgroundOverlay = Color(0x80000000)  // Semi-transparent black overlay
    
    // Additional UI colors
    val Divider = Color(0x33FFFFFF)  // 20% white for dividers
    val Border = Color(0x4DFFFFFF)  // 30% white for borders
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFF44336)
    val Warning = Color(0xFFFF9800)
    
    // Gradients for glassmorphism
    val GlassGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0x4D2C2C2C),  // 30% opacity at top
            Color(0x802C2C2C)   // 50% opacity at bottom
        )
    )
}

// Typography
val GreyARTypography = Typography(
    // Display styles
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        color = GreyARColors.TextPrimary
    ),
    
    // Headlines (like "Build websites easier")
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        color = GreyARColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = GreyARColors.TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = GreyARColors.TextPrimary
    ),
    
    // Body text (like the paragraph text)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = GreyARColors.TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = GreyARColors.TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = GreyARColors.TextTertiary
    ),
    
    // Labels and buttons
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = GreyARColors.TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = GreyARColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = GreyARColors.TextTertiary
    )
)

// Shapes
object GreyARShapes {
    val CardShape = RoundedCornerShape(12.dp)  // Rounded corners like in the image
    val ButtonShape = RoundedCornerShape(24.dp)  // Fully rounded buttons
    val InputShape = RoundedCornerShape(8.dp)  // Slightly rounded inputs
    val DialogShape = RoundedCornerShape(16.dp)  // Dialog corners
}

// Material 3 Color Scheme
private val GreyARColorScheme = darkColorScheme(
    primary = GreyARColors.AccentBlue,
    onPrimary = Color.White,
    primaryContainer = GreyARColors.AccentBlueDark,
    onPrimaryContainer = Color.White,
    
    secondary = GreyARColors.TextSecondary,
    onSecondary = Color.Black,
    secondaryContainer = GreyARColors.CardBackgroundLight,
    onSecondaryContainer = GreyARColors.TextPrimary,
    
    tertiary = GreyARColors.TextTertiary,
    onTertiary = Color.Black,
    
    background = GreyARColors.Background,
    onBackground = GreyARColors.TextPrimary,
    
    surface = GreyARColors.CardBackground,
    onSurface = GreyARColors.TextPrimary,
    surfaceVariant = GreyARColors.CardBackgroundLight,
    onSurfaceVariant = GreyARColors.TextSecondary,
    
    error = GreyARColors.Error,
    onError = Color.White,
    
    outline = GreyARColors.Border,
    outlineVariant = GreyARColors.Divider,
    
    scrim = GreyARColors.BackgroundOverlay
)

// Custom theme configuration
data class GreyARThemeConfig(
    val enableGlassmorphism: Boolean = true,
    val enableAnimations: Boolean = true,
    val cardElevation: Float = 8f,
    val cardBlurRadius: Float = 20f
)

// Local composition for theme config
val LocalGreyARThemeConfig = staticCompositionLocalOf { GreyARThemeConfig() }

/**
 * GreyAR Theme Composable
 */
@Composable
fun GreyARTheme(
    config: GreyARThemeConfig = GreyARThemeConfig(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalGreyARThemeConfig provides config) {
        MaterialTheme(
            colorScheme = GreyARColorScheme,
            typography = GreyARTypography,
            shapes = Shapes(
                extraSmall = GreyARShapes.InputShape,
                small = GreyARShapes.InputShape,
                medium = GreyARShapes.CardShape,
                large = GreyARShapes.DialogShape,
                extraLarge = GreyARShapes.DialogShape
            ),
            content = content
        )
    }
}