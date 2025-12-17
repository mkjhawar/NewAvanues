/**
 * CursorMenuOverlay.kt - Cursor context menu overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.ui.overlays

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.augmentalis.voiceoscore.accessibility.ui.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.math.*

/**
 * Cursor context menu overlay
 */
class CursorMenuOverlay(
    context: Context,
    private val onActionSelected: (CursorAction) -> Unit
) : BaseOverlay(context, OverlayType.POSITIONED) {
    
    
    companion object {
        private const val TAG = "CursorMenuOverlay"
        private val MENU_RADIUS = 80.dp
        val ITEM_SIZE = 40.dp
        private const val AUTO_HIDE_DELAY = 5000L
    }
    
    private var _isExpanded by mutableStateOf(false)
    private var _cursorX by mutableStateOf(0)
    private var _cursorY by mutableStateOf(0)
    
    /**
     * Show menu at cursor position
     */
    fun showAtCursor(x: Int, y: Int) {
        _cursorX = x
        _cursorY = y
        updatePosition(x - MENU_RADIUS.value.toInt(), y - MENU_RADIUS.value.toInt())
        
        if (!isVisible()) {
            show()
        }
        
        _isExpanded = true
        
        // Auto-hide after delay
        overlayScope.launch {
            delay(AUTO_HIDE_DELAY)
            hideMenu()
        }
    }
    
    /**
     * Hide menu with animation
     */
    fun hideMenu() {
        _isExpanded = false
        
        // Hide overlay after animation completes
        overlayScope.launch {
            delay(300) // Wait for animation
            hide()
        }
    }
    
    @Composable
    override fun OverlayContent() {
        val expandedState by animateFloatAsState(
            targetValue = if (_isExpanded) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "expand_animation"
        )
        
        Box(
            modifier = Modifier.size(MENU_RADIUS * 2),
            contentAlignment = Alignment.Center
        ) {
            // Center point indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .glassMorphism(
                        config = GlassMorphismConfig(
                            cornerRadius = 4.dp,
                            backgroundOpacity = 0.3f,
                            borderOpacity = 0.5f,
                            borderWidth = 1.dp,
                            tintColor = Color(0xFF4285F4),
                            tintOpacity = 0.4f
                        ),
                        depth = DepthLevel(1f)
                    )
            )
            
            // Menu items arranged in circle
            CursorAction.values().forEachIndexed { index, action ->
                val angle = (index * 360f / CursorAction.values().size) + 45f // Start at 45 degrees
                val radius = MENU_RADIUS.value * expandedState
                
                val x = cos(Math.toRadians(angle.toDouble())).toFloat() * radius
                val y = sin(Math.toRadians(angle.toDouble())).toFloat() * radius
                
                AnimatedVisibility(
                    visible = expandedState > 0.1f,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    CursorMenuItem(
                        modifier = Modifier
                            .offset(x.dp, y.dp)
                            .graphicsLayer {
                                scaleX = expandedState
                                scaleY = expandedState
                                alpha = expandedState
                            },
                        action = action,
                        onClick = {
                            onActionSelected(action)
                            hideMenu()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Cleanup resources when overlay is destroyed
     */
    override fun dispose() {
        overlayScope.cancel()
        super.dispose()
    }
}

@Composable
private fun CursorMenuItem(
    modifier: Modifier = Modifier,
    action: CursorAction,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .size(CursorMenuOverlay.ITEM_SIZE)
            .clickable { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 20.dp,
                    backgroundOpacity = 0.2f,
                    borderOpacity = 0.3f,
                    borderWidth = 1.dp,
                    tintColor = action.color,
                    tintOpacity = 0.25f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * Cursor action items for the menu
 */
enum class CursorAction(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    CLICK("Click", Icons.Default.TouchApp, Color(0xFF4CAF50)),
    LONG_CLICK("Hold", Icons.Default.Timer, Color(0xFFFF9800)),
    SCROLL_UP("Scroll Up", Icons.Default.KeyboardArrowUp, Color(0xFF2196F3)),
    SCROLL_DOWN("Scroll Down", Icons.Default.KeyboardArrowDown, Color(0xFF2196F3)),
    BACK("Back", Icons.AutoMirrored.Filled.ArrowBack, Color(0xFF9C27B0)),
    HOME("Home", Icons.Default.Home, Color(0xFF607D8B)),
    MENU("Menu", Icons.Default.MoreVert, Color(0xFF795548)),
    CLOSE("Close", Icons.Default.Close, Color(0xFFFF5722))
}