/**
 * NumberOverlay.kt - Number overlay for UI element identification
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.augmentalis.voiceoscore.accessibility.handlers.NumberHandler.ElementInfo
import com.augmentalis.voiceoscore.accessibility.ui.utils.*
import com.augmentalis.voiceos.constants.VoiceOSConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel

/**
 * Number overlay displaying numbered labels on interactive elements
 */
class NumberOverlay(
    context: Context,
    private val onNumberSelected: (Int) -> Unit,
    private val onDismiss: () -> Unit
) : BaseOverlay(context, OverlayType.FULLSCREEN) {
    
    
    companion object {
        private const val TAG = "NumberOverlay"
        private val AUTO_HIDE_DELAY = VoiceOSConstants.Overlays.AUTO_HIDE_LONG_MS
        val LABEL_SIZE = 24.dp
        private val MAX_VISIBLE_NUMBERS = VoiceOSConstants.Overlays.MAX_OVERLAYS_VISIBLE
    }
    
    private var _numberedElements by mutableStateOf<Map<Int, NumberedElement>>(emptyMap())
    private var _isShowing by mutableStateOf(false)
    
    /**
     * Numbered element with position and display info
     */
    data class NumberedElement(
        val number: Int,
        val elementInfo: ElementInfo,
        val screenX: Float,
        val screenY: Float,
        val description: String,
        val isClickable: Boolean
    )
    
    /**
     * Show number overlay with elements
     */
    fun showWithElements(elements: Map<Int, ElementInfo>) {
        val numberedElements = mutableMapOf<Int, NumberedElement>()
        
        elements.forEach { (number, elementInfo) ->
            if (number <= MAX_VISIBLE_NUMBERS) {
                // Position label at top-left of element bounds
                val screenX = elementInfo.bounds.left.toFloat()
                val screenY = elementInfo.bounds.top.toFloat()
                
                numberedElements[number] = NumberedElement(
                    number = number,
                    elementInfo = elementInfo,
                    screenX = screenX,
                    screenY = screenY,
                    description = elementInfo.description,
                    isClickable = elementInfo.isClickable
                )
            }
        }
        
        _numberedElements = numberedElements
        _isShowing = true
        
        if (!isVisible()) {
            show()
        }
        
        // Auto-hide after delay
        overlayScope.launch {
            delay(AUTO_HIDE_DELAY)
            hideNumberOverlay()
        }
    }
    
    /**
     * Hide number overlay
     */
    fun hideNumberOverlay() {
        _isShowing = false
        _numberedElements = emptyMap()
        
        overlayScope.launch {
            delay(300) // Wait for animation
            hide()
            onDismiss()
        }
    }
    
    /**
     * Handle number selection
     */
    fun selectNumber(number: Int) {
        if (_numberedElements.containsKey(number)) {
            onNumberSelected(number)
            hideNumberOverlay()
        }
    }
    
    /**
     * Cleanup resources when overlay is destroyed
     */
    override fun dispose() {
        overlayScope.cancel()
        super.dispose()
    }
    
    @Composable
    override fun OverlayContent() {
        val density = LocalDensity.current
        
        AnimatedVisibility(
            visible = _isShowing,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            // Full screen container for number labels
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Render each numbered element
                _numberedElements.values.forEach { numberedElement ->
                    NumberLabel(
                        modifier = Modifier.offset(
                            x = with(density) { numberedElement.screenX.toDp() },
                            y = with(density) { numberedElement.screenY.toDp() }
                        ),
                        numberedElement = numberedElement,
                        onClick = { selectNumber(numberedElement.number) }
                    )
                }
                
                // Instructions overlay at bottom
                InstructionsPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    elementCount = _numberedElements.size,
                    onDismiss = ::hideNumberOverlay
                )
            }
        }
    }
}

@Composable
private fun NumberLabel(
    modifier: Modifier = Modifier,
    numberedElement: NumberOverlay.NumberedElement,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")
    
    // Pulsing glow animation
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Card(
        modifier = modifier
            .size(NumberOverlay.LABEL_SIZE)
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.25f,
                    borderOpacity = 0.4f,
                    borderWidth = 1.5.dp,
                    tintColor = if (numberedElement.isClickable) Color(0xFF4CAF50) else Color(0xFF2196F3),
                    tintOpacity = 0.3f
                ),
                depth = DepthLevel(0.8f)
            )
            .graphicsLayer {
                // Add subtle pulsing glow
                shadowElevation = (4.dp.toPx() * pulseAlpha)
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = numberedElement.number.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    // Tooltip with element description (positioned below the number)
    if (numberedElement.description.isNotEmpty()) {
        Card(
            modifier = Modifier
                .offset(y = NumberOverlay.LABEL_SIZE + 4.dp)
                .widthIn(max = 120.dp)
                .glassMorphism(
                    config = GlassMorphismConfig(
                        cornerRadius = 8.dp,
                        backgroundOpacity = 0.15f,
                        borderOpacity = 0.25f,
                        borderWidth = 1.dp,
                        tintColor = Color.Black,
                        tintOpacity = 0.2f
                    ),
                    depth = DepthLevel(0.4f)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Text(
                text = numberedElement.description,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InstructionsPanel(
    modifier: Modifier = Modifier,
    elementCount: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.2f,
                    borderOpacity = 0.3f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.25f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$elementCount elements numbered",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Say \"tap [number]\" to interact or \"hide numbers\" to close",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFFF5722).copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hide Numbers",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }
    }
}