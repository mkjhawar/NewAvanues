package com.augmentalis.voiceui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MagicDreamTheme - A dreamy, gradient-rich theme with soft aesthetics
 * Based on modern design trends with purple/pink gradients and glassmorphism
 */
object MagicDreamColors {
    // Primary Gradient Colors
    val GradientStart = Color(0xFF9C88FF)  // Soft Purple
    val GradientMiddle = Color(0xFFB794F6) // Light Purple
    val GradientEnd = Color(0xFFF687B3)    // Soft Pink
    
    // Surface Colors
    val CardBackground = Color(0xFFFFFFFE) // Almost white with slight warmth
    val CardBackgroundDark = Color(0xFF2D2D44) // Dark mode card
    val CardShadow = Color(0x1A000000)     // Subtle shadow
    val CardBorder = Color(0x0D000000)     // Very subtle border
    
    // Text Colors
    val TextPrimary = Color(0xFF2D3436)    // Dark grey for main text
    val TextSecondary = Color(0xFF636E72)  // Medium grey for secondary
    val TextTertiary = Color(0xFF95A5A6)   // Light grey for hints
    val TextOnGradient = Color(0xFFFFFFFF) // White on gradients
    
    // Interactive Colors
    val ButtonGradientStart = Color(0xFF667EEA)  // Indigo
    val ButtonGradientEnd = Color(0xFF764BA2)    // Purple
    val ButtonHover = Color(0xFF5A67D8)          // Darker on hover
    val ButtonDisabled = Color(0xFFE2E8F0)       // Light grey
    
    // Accent Colors
    val AccentPurple = Color(0xFF8B7AE3)
    val AccentPink = Color(0xFFED64A6)
    val AccentBlue = Color(0xFF4299E1)
    val AccentGreen = Color(0xFF48BB78)
    val AccentOrange = Color(0xFFED8936)
    
    // Status Colors
    val Success = Color(0xFF48BB78)
    val Warning = Color(0xFFECC94B)
    val Error = Color(0xFFF56565)
    val Info = Color(0xFF4299E1)
    
    // Background Colors
    val BackgroundLight = Color(0xFFF8F9FF)  // Very light purple tint
    val BackgroundDark = Color(0xFF1A1A2E)   // Dark navy
    
    // Glassmorphism
    val GlassBackground = Color(0xCCFFFFFF)  // 80% white
    val GlassBackgroundDark = Color(0xCC1A1A2E) // 80% dark
    val GlassBorder = Color(0x33FFFFFF)      // 20% white border
}

/**
 * MagicDreamShapes - Soft, rounded shapes for the dreamy aesthetic
 */
object MagicDreamShapes {
    val ExtraSmall = RoundedCornerShape(8.dp)
    val Small = RoundedCornerShape(12.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val ExtraLarge = RoundedCornerShape(32.dp)
    val Pill = RoundedCornerShape(50)
    val Circle = CircleShape
    
    // Special shapes
    val CardShape = RoundedCornerShape(20.dp)
    val ButtonShape = RoundedCornerShape(28.dp)
    val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val DialogShape = RoundedCornerShape(28.dp)
}

/**
 * MagicDreamTypography - Clean, modern typography
 */
object MagicDreamTypography {
    private val fontFamily = FontFamily.Default // Can be customized
    
    val displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    )
    
    val displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    )
    
    val displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )
    
    val headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )
    
    val headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    
    val headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    val titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    val titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    
    val bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    
    val bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    val labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
}

/**
 * MagicDreamElevations - Soft shadows for depth
 */
object MagicDreamElevations {
    val card = 4.dp
    val button = 6.dp
    val dialog = 24.dp
    val bottomSheet = 16.dp
    val fab = 12.dp
}

/**
 * Gradient brush creators
 */
fun dreamGradientBrush(
    colors: List<Color> = listOf(
        MagicDreamColors.GradientStart,
        MagicDreamColors.GradientMiddle,
        MagicDreamColors.GradientEnd
    ),
    start: Offset = Offset(0f, 0f),
    end: Offset = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
) = Brush.linearGradient(
    colors = colors,
    start = start,
    end = end
)

fun dreamRadialGradient(
    colors: List<Color> = listOf(
        MagicDreamColors.GradientStart,
        MagicDreamColors.GradientEnd
    )
) = Brush.radialGradient(colors = colors)

/**
 * Main theme composable
 */
@Composable
fun MagicDreamTheme(
    darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER")
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = MagicDreamColors.AccentPurple,
            onPrimary = Color.White,
            primaryContainer = MagicDreamColors.AccentPurple.copy(alpha = 0.12f),
            onPrimaryContainer = MagicDreamColors.AccentPurple,
            
            secondary = MagicDreamColors.AccentPink,
            onSecondary = Color.White,
            secondaryContainer = MagicDreamColors.AccentPink.copy(alpha = 0.12f),
            onSecondaryContainer = MagicDreamColors.AccentPink,
            
            tertiary = MagicDreamColors.AccentBlue,
            onTertiary = Color.White,
            
            background = MagicDreamColors.BackgroundDark,
            onBackground = Color.White,
            
            surface = MagicDreamColors.CardBackgroundDark,
            onSurface = Color.White,
            surfaceVariant = MagicDreamColors.CardBackgroundDark.copy(alpha = 0.8f),
            onSurfaceVariant = MagicDreamColors.TextSecondary,
            
            error = MagicDreamColors.Error,
            onError = Color.White,
            
            outline = MagicDreamColors.CardBorder,
            outlineVariant = MagicDreamColors.CardBorder.copy(alpha = 0.5f),
            
            scrim = Color.Black.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = MagicDreamColors.AccentPurple,
            onPrimary = Color.White,
            primaryContainer = MagicDreamColors.AccentPurple.copy(alpha = 0.08f),
            onPrimaryContainer = MagicDreamColors.AccentPurple,
            
            secondary = MagicDreamColors.AccentPink,
            onSecondary = Color.White,
            secondaryContainer = MagicDreamColors.AccentPink.copy(alpha = 0.08f),
            onSecondaryContainer = MagicDreamColors.AccentPink,
            
            tertiary = MagicDreamColors.AccentBlue,
            onTertiary = Color.White,
            
            background = MagicDreamColors.BackgroundLight,
            onBackground = MagicDreamColors.TextPrimary,
            
            surface = MagicDreamColors.CardBackground,
            onSurface = MagicDreamColors.TextPrimary,
            surfaceVariant = MagicDreamColors.CardBackground.copy(alpha = 0.95f),
            onSurfaceVariant = MagicDreamColors.TextSecondary,
            
            error = MagicDreamColors.Error,
            onError = Color.White,
            
            outline = MagicDreamColors.CardBorder,
            outlineVariant = MagicDreamColors.CardBorder.copy(alpha = 0.5f),
            
            scrim = Color.Black.copy(alpha = 0.32f)
        )
    }
    
    val typography = Typography(
        displayLarge = MagicDreamTypography.displayLarge,
        displayMedium = MagicDreamTypography.displayMedium,
        displaySmall = MagicDreamTypography.displaySmall,
        headlineLarge = MagicDreamTypography.headlineLarge,
        headlineMedium = MagicDreamTypography.headlineMedium,
        headlineSmall = MagicDreamTypography.headlineSmall,
        titleLarge = MagicDreamTypography.titleLarge,
        titleMedium = MagicDreamTypography.titleMedium,
        bodyLarge = MagicDreamTypography.bodyLarge,
        bodyMedium = MagicDreamTypography.bodyMedium,
        labelLarge = MagicDreamTypography.labelLarge
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

/**
 * Animated gradient background
 */
@Composable
fun MagicDreamGradientBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    
    val animatedOffset = if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offset"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MagicDreamColors.GradientStart,
                        MagicDreamColors.GradientMiddle,
                        MagicDreamColors.GradientEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(
                        x = if (animated) animatedOffset.value * 1000f else 500f,
                        y = if (animated) animatedOffset.value * 1000f else 500f
                    )
                )
            )
    )
}