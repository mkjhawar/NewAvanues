/**
 * ARVisionTheme.kt
 * Path: /CodeImport/HUDManager/src/main/java/com/augmentalis/hudmanager/ui/ARVisionTheme.kt
 * 
 * Created: 2025-01-23
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: ARVision-inspired UI theme with liquid iOS vibrancy
 * Provides glass morphism and fluid animations for HUD elements
 */

package com.augmentalis.voiceoscore.managers.hudmanager.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * ARVision color palette with vibrancy and transparency
 */
object ARVisionColors {
    // Glass morphism backgrounds
    val GlassPrimary = Color(0x30FFFFFF)
    val GlassSecondary = Color(0x20FFFFFF) 
    val GlassTertiary = Color(0x15FFFFFF)
    val GlassHighlight = Color(0x40FFFFFF)
    
    // Liquid iOS vibrancy colors
    val VibrantBlue = Color(0xFF007AFF)
    val VibrantGreen = Color(0xFF34C759)
    val VibrantOrange = Color(0xFFFF9500)
    val VibrantRed = Color(0xFFFF3B30)
    val VibrantPurple = Color(0xFFAF52DE)
    val VibrantPink = Color(0xFFFF2D92)
    
    // Adaptive colors that respond to environment
    val AdaptiveText = Color(0xFFFFFFFF)
    val AdaptiveSecondaryText = Color(0xCCFFFFFF)
    val AdaptiveTertiaryText = Color(0x99FFFFFF)
    
    // System colors for HUD elements
    val SystemSuccess = Color(0xFF30D158)
    val SystemWarning = Color(0xFFFFD60A)
    val SystemError = Color(0xFFFF453A)
    val SystemInfo = Color(0xFF64D2FF)
}

/**
 * ARVision typography with liquid scaling
 */
object ARVisionTypography {
    val Display = androidx.compose.material3.Typography().displayLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
        fontSize = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
    
    val Headline = androidx.compose.material3.Typography().headlineLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = androidx.compose.ui.unit.TextUnit(32f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
    
    val Title = androidx.compose.material3.Typography().titleLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
    
    val Body = androidx.compose.material3.Typography().bodyLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
    
    val Caption = androidx.compose.material3.Typography().labelMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
}

/**
 * Glass morphism brush for backgrounds
 */
fun glassMorphismBrush(
    colors: List<Color> = listOf(
        ARVisionColors.GlassHighlight,
        ARVisionColors.GlassPrimary,
        ARVisionColors.GlassSecondary
    )
): Brush = Brush.verticalGradient(colors)

/**
 * Liquid animation specs
 */
object LiquidAnimations {
    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy, 
        stiffness = Spring.StiffnessMediumLow
    )
    
    val TweenFast = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )
    
    val TweenSmooth = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
    
    val TweenLiquid = tween<Float>(
        durationMillis = 800,
        easing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
    )
}

/**
 * ARVision component shapes
 */
object ARVisionShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val ExtraLarge = RoundedCornerShape(32.dp)
    
    // Liquid shapes that respond to interaction
    val LiquidSmall = RoundedCornerShape(12.dp)
    val LiquidMedium = RoundedCornerShape(20.dp)
    val LiquidLarge = RoundedCornerShape(28.dp)
}

/**
 * Glass panel modifier with blur and vibrancy
 */
fun Modifier.glassPanel(
    blur: androidx.compose.ui.unit.Dp = 20.dp,
    alpha: Float = 0.7f,
    shape: Shape = ARVisionShapes.Medium
): Modifier = this
    .clip(shape)
    .background(
        brush = glassMorphismBrush(),
        alpha = alpha
    )
    .blur(radius = blur)

/**
 * Liquid button modifier with haptic feedback
 */
@Composable
fun Modifier.liquidButton(
    isPressed: Boolean = false,
    @Suppress("UNUSED_PARAMETER") hapticEnabled: Boolean = true
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = LiquidAnimations.SpringBouncy
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 8f,
        animationSpec = LiquidAnimations.SpringSmooth
    )
    
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = elevation
        }
}

/**
 * Vibrancy effect modifier
 */
fun Modifier.vibrancy(
    intensity: Float = 1.0f,
    blur: androidx.compose.ui.unit.Dp = 15.dp
): Modifier = this
    .background(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.3f * intensity),
                Color.White.copy(alpha = 0.1f * intensity),
                Color.Transparent
            )
        )
    )
    .blur(radius = blur)

/**
 * Floating HUD element with depth
 */
@Composable
fun FloatingHUDElement(
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Subtle floating animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Surface(
        modifier = modifier.graphicsLayer {
            translationY = offsetY
        },
        shape = ARVisionShapes.LiquidMedium,
        color = Color.Transparent,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier
                .glassPanel()
                .vibrancy()
        ) {
            content()
        }
    }
}

/**
 * Confidence indicator with liquid animation
 */
@Composable 
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier,
    color: Color = ARVisionColors.VibrantBlue
) {
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = LiquidAnimations.TweenLiquid
    )
    
    val shimmer by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Canvas(modifier = modifier.size(80.dp, 8.dp)) {
        drawConfidenceBar(
            confidence = animatedConfidence,
            shimmer = shimmer,
            color = color
        )
    }
}

private fun DrawScope.drawConfidenceBar(
    confidence: Float,
    shimmer: Float,
    color: Color
) {
    val width = size.width
    val height = size.height
    val cornerRadius = height / 2f
    
    // Background
    drawRoundRect(
        color = ARVisionColors.GlassTertiary,
        size = androidx.compose.ui.geometry.Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
    )
    
    // Confidence fill with liquid animation
    val fillWidth = width * confidence
    if (fillWidth > 0f) {
        // Main fill
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    color.copy(alpha = 0.8f),
                    color.copy(alpha = 1.0f),
                    color.copy(alpha = 0.8f)
                )
            ),
            size = androidx.compose.ui.geometry.Size(fillWidth, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
        )
        
        // Shimmer effect
        val shimmerPosition = fillWidth * shimmer
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                startX = shimmerPosition - 20f,
                endX = shimmerPosition + 20f
            ),
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(fillWidth, height)
        )
    }
}

/**
 * Voice command badge with liquid morphing
 */
@Composable
fun VoiceCommandBadge(
    text: String,
    confidence: Float,
    category: String,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (category) {
        "NAVIGATION" -> ARVisionColors.VibrantBlue
        "GESTURE" -> ARVisionColors.VibrantGreen  
        "SYSTEM" -> ARVisionColors.VibrantOrange
        "ACCESSIBILITY" -> ARVisionColors.VibrantPurple
        else -> ARVisionColors.GlassPrimary
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1.0f,
        animationSpec = LiquidAnimations.SpringBouncy
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (confidence > 0.5f) 1.0f else 0.6f,
        animationSpec = LiquidAnimations.TweenSmooth
    )
    
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = ARVisionShapes.LiquidSmall,
        color = backgroundColor.copy(alpha = 0.2f)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .vibrancy(intensity = confidence)
        ) {
            androidx.compose.material3.Text(
                text = text,
                style = ARVisionTypography.Caption,
                color = ARVisionColors.AdaptiveText
            )
        }
    }
}