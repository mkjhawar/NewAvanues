/**
 * CommandDisambiguationOverlay.kt - Duplicate command disambiguation overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.ui.overlays

import android.content.Context
import android.graphics.Rect
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius

/**
 * Data class for duplicate command options
 */
data class DuplicateCommandOption(
    val index: Int,
    val command: String,
    val bounds: Rect,
    val description: String? = null,
    val nodeInfo: Any? = null // AccessibilityNodeInfo reference
)

/**
 * Multi-language number word support
 */
object NumberWordConverter {
    private val englishNumbers = mapOf(
        1 to "one", 2 to "two", 3 to "three", 4 to "four", 5 to "five",
        6 to "six", 7 to "seven", 8 to "eight", 9 to "nine", 10 to "ten"
    )
    
    private val spanishNumbers = mapOf(
        1 to "uno", 2 to "dos", 3 to "tres", 4 to "cuatro", 5 to "cinco",
        6 to "seis", 7 to "siete", 8 to "ocho", 9 to "nueve", 10 to "diez"
    )
    
    private val frenchNumbers = mapOf(
        1 to "un", 2 to "deux", 3 to "trois", 4 to "quatre", 5 to "cinq",
        6 to "six", 7 to "sept", 8 to "huit", 9 to "neuf", 10 to "dix"
    )
    
    fun getNumberWord(number: Int, language: String = "en"): String {
        return when (language.lowercase()) {
            "es" -> spanishNumbers[number] ?: number.toString()
            "fr" -> frenchNumbers[number] ?: number.toString()
            else -> englishNumbers[number] ?: number.toString()
        }
    }
}

/**
 * Overlay for disambiguating duplicate commands
 */
class CommandDisambiguationOverlay(
    context: Context,
    private val onOptionSelected: (DuplicateCommandOption) -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : BaseOverlay(context, OverlayType.FLOATING) {
    
    
    companion object {
        private const val TAG = "CommandDisambiguationOverlay"
        private const val AUTO_DISMISS_DELAY = 10000L // 10 seconds
        private const val ANIMATION_DURATION = 300
        private const val MAX_OPTIONS_VISIBLE = 9
        private const val GLASSMORPHISM_BLUR = 10f
    }
    
    private var duplicateOptions = mutableStateListOf<DuplicateCommandOption>()
    private var currentLanguage by mutableStateOf("en")
    private var isAnimating by mutableStateOf(false)
    
    /**
     * Show disambiguation options for duplicate commands
     */
    fun showDuplicates(
        @Suppress("UNUSED_PARAMETER") command: String,
        options: List<DuplicateCommandOption>,
        language: String = "en"
    ) {
        overlayScope.launch {
            currentLanguage = language
            isAnimating = true
            
            // Limit options and assign indices
            val limitedOptions = options.take(MAX_OPTIONS_VISIBLE).mapIndexed { index, option ->
                option.copy(index = index + 1)
            }
            
            duplicateOptions.clear()
            duplicateOptions.addAll(limitedOptions)
            
            if (!isVisible()) {
                show()
            }
            
            // Auto-dismiss after delay
            delay(AUTO_DISMISS_DELAY)
            if (isVisible()) {
                dismissOverlay()
            }
        }
    }
    
    /**
     * Dismiss the overlay with animation
     */
    private fun dismissOverlay() {
        overlayScope.launch {
            isAnimating = false
            delay(ANIMATION_DURATION.toLong())
            duplicateOptions.clear()
            hide()
            onDismiss()
        }
    }
    
    @Composable
    override fun OverlayContent() {
        AnimatedVisibility(
            visible = duplicateOptions.isNotEmpty() && isAnimating,
            enter = fadeIn(animationSpec = tween(ANIMATION_DURATION)) + 
                    scaleIn(initialScale = 0.8f, animationSpec = tween(ANIMATION_DURATION)),
            exit = fadeOut(animationSpec = tween(ANIMATION_DURATION)) + 
                   scaleOut(targetScale = 0.8f, animationSpec = tween(ANIMATION_DURATION))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { dismissOverlay() }, // Click outside to dismiss
                contentAlignment = Alignment.Center
            ) {
                // Glassmorphism background
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            renderEffect = BlurEffect(
                                GLASSMORPHISM_BLUR,
                                GLASSMORPHISM_BLUR,
                                TileMode.Decal
                            )
                        }
                ) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        size = size
                    )
                }
                
                // Disambiguation panel
                DisambiguationPanel()
            }
        }
    }
    
    @Composable
    private fun DisambiguationPanel() {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(enabled = false) {}, // Prevent click-through
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Select which \"${duplicateOptions.firstOrNull()?.command ?: ""}\" to click:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // Options with custom canvas drawing
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(duplicateOptions) { _, option ->
                        DuplicateOptionItem(
                            option = option,
                            language = currentLanguage,
                            onClick = {
                                onOptionSelected(option)
                                dismissOverlay()
                            }
                        )
                    }
                }
                
                // Instructions
                Text(
                    text = "Say the number or tap to select",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
    
    @Composable
    private fun DuplicateOptionItem(
        option: DuplicateCommandOption,
        language: String,
        onClick: () -> Unit
    ) {
        val animatedScale = remember { Animatable(1f) }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(animatedScale.value)
                .clickable {
                    overlayScope.launch {
                        animatedScale.animateTo(
                            targetValue = 0.95f,
                            animationSpec = tween(100)
                        )
                        animatedScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        onClick()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.4f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number circle with custom canvas
                NumberCircle(
                    number = option.index,
                    language = language
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Command details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = option.command,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    option.description?.let { desc ->
                        Text(
                            text = desc,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                // Visual position indicator
                PositionIndicator(bounds = option.bounds)
            }
        }
    }
    
    @Composable
    private fun NumberCircle(
        number: Int,
        language: String
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Gradient circle background
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Yellow.copy(alpha = 0.8f),
                            Color.Yellow.copy(alpha = 0.4f)
                        ),
                        center = center,
                        radius = size.minDimension / 2
                    ),
                    radius = size.minDimension / 2
                )
                
                // Circle border
                drawCircle(
                    color = Color.Yellow,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = number.toString(),
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = NumberWordConverter.getNumberWord(number, language),
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    @Composable
    private fun PositionIndicator(bounds: Rect) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        
        Canvas(
            modifier = Modifier.size(40.dp, 30.dp)
        ) {
            val relativeX = bounds.centerX().toFloat() / screenWidth
            val relativeY = bounds.centerY().toFloat() / screenHeight
            
            // Draw minimap
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.3f),
                size = size,
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            
            // Draw position dot
            drawCircle(
                color = Color.Yellow,
                radius = 3.dp.toPx(),
                center = Offset(
                    x = size.width * relativeX,
                    y = size.height * relativeY
                )
            )
        }
    }
    
    /**
     * Handle voice command for number selection
     */
    fun handleVoiceCommand(spokenText: String) {
        // Try to extract number from spoken text
        val number = extractNumber(spokenText)
        
        number?.let { num ->
            val option = duplicateOptions.find { it.index == num }
            option?.let {
                onOptionSelected(it)
                dismissOverlay()
            }
        }
    }
    
    /**
     * Extract number from spoken text
     */
    private fun extractNumber(text: String): Int? {
        // Try direct number parsing
        text.toIntOrNull()?.let { return it }
        
        // Try word to number conversion
        val words = text.lowercase().split(" ")
        for (word in words) {
            for (i in 1..10) {
                if (word == NumberWordConverter.getNumberWord(i, currentLanguage)) {
                    return i
                }
            }
        }
        
        return null
    }
    
    override fun dispose() {
        overlayScope.cancel()
        duplicateOptions.clear()
        super.dispose()
    }
}

