/**
 * AnimatedLanguageDisplay.kt - Professional language change animations
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 */
package com.augmentalis.voiceoscore.managers.localizationmanager.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceoscore.managers.localizationmanager.ui.LocalizationColors

/**
 * Animated current language display for header
 * 
 * Features:
 * - Smooth fade transition when language changes
 * - Professional and subtle animation
 * - Configurable animation timing
 * - Maintains accessibility
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedCurrentLanguage(
    currentLanguage: String,
    animationEnabled: Boolean = true,
    fontSize: TextUnit = 12.sp,
    color: Color = Color(0xFF81C784),
    modifier: Modifier = Modifier
) {
    if (animationEnabled) {
        AnimatedContent(
            targetState = currentLanguage,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) togetherWith fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutLinearInEasing
                    )
                )
            },
            modifier = modifier
        ) { language ->
            Text(
                text = "Current Language: ${language.uppercase()}",
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    } else {
        // Static display when animation is disabled
        Text(
            text = "Current Language: ${currentLanguage.uppercase()}",
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = modifier
        )
    }
}

/**
 * Enhanced animated language display with highlight effect
 * 
 * Shows a brief highlight when language changes to draw attention
 */
@Composable
fun AnimatedCurrentLanguageWithHighlight(
    currentLanguage: String,
    animationEnabled: Boolean = true,
    fontSize: TextUnit = 12.sp,
    baseColor: Color = Color(0xFF81C784),
    highlightColor: Color = LocalizationColors.StatusActive,
    modifier: Modifier = Modifier
) {
    var hasChanged by remember { mutableStateOf(false) }
    
    // Track language changes
    LaunchedEffect(currentLanguage) {
        hasChanged = true
        kotlinx.coroutines.delay(800) // Highlight duration
        hasChanged = false
    }
    
    val animatedColor by animateColorAsState(
        targetValue = if (animationEnabled && hasChanged) highlightColor else baseColor,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (animationEnabled && hasChanged) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    if (animationEnabled) {
        AnimatedContent(
            targetState = currentLanguage,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = tween(250)
                ) + fadeIn(animationSpec = tween(250)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            },
            modifier = modifier
        ) { language ->
            Text(
                text = "Current Language: ${language.uppercase()}",
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = animatedColor,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
            )
        }
    } else {
        Text(
            text = "Current Language: ${currentLanguage.uppercase()}",
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = baseColor,
            modifier = modifier
        )
    }
}

/**
 * Simple animated language chip for language selection
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedLanguageChip(
    language: String,
    isSelected: Boolean,
    animationEnabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            LocalizationColors.StatusActive
        } else {
            Color.Gray.copy(alpha = 0.3f)
        },
        animationSpec = tween(durationMillis = 300)
    )
    
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 1.dp,
        animationSpec = tween(durationMillis = 300)
    )
    
    Surface(
        onClick = onClick,
        color = animatedBackgroundColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        shadowElevation = animatedElevation,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = language,
            transitionSpec = {
                if (animationEnabled) {
                    slideInVertically(
                        initialOffsetY = { height -> height / 2 },
                        animationSpec = tween(200)
                    ) + fadeIn(animationSpec = tween(200)) togetherWith
                    slideOutVertically(
                        targetOffsetY = { height -> -height / 2 },
                        animationSpec = tween(150)
                    ) + fadeOut(animationSpec = tween(150))
                } else {
                    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                }
            }
        ) { lang ->
            Text(
                text = lang.uppercase(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Animated language transition indicator
 * Shows a smooth transition when switching between languages
 */
@Composable
fun LanguageTransitionIndicator(
    fromLanguage: String?,
    toLanguage: String,
    isTransitioning: Boolean,
    animationEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isTransitioning && fromLanguage != null && animationEnabled) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // From language (fading out)
            AnimatedVisibility(
                visible = true,
                exit = fadeOut(animationSpec = tween(400)) + 
                       shrinkHorizontally(animationSpec = tween(400))
            ) {
                Text(
                    text = fromLanguage.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
            
            // Arrow
            Text(
                text = "→",
                fontSize = 12.sp,
                color = LocalizationColors.StatusActive
            )
            
            // To language (fading in)
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) + 
                        expandHorizontally(animationSpec = tween(400, delayMillis = 200))
            ) {
                Text(
                    text = toLanguage.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LocalizationColors.StatusActive
                )
            }
        }
    } else {
        // Static display
        Text(
            text = toLanguage.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF81C784),
            modifier = modifier
        )
    }
}