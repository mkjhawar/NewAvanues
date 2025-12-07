/**
 * MenuView.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/view/MenuView.kt
 * 
 * Created: 2025-01-26 00:30 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: ARVision-themed glass morphism context menu for cursor actions
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-26 00:30 PST): Initial creation with ARVision glass morphism styling
 */

package com.augmentalis.voiceos.cursor.view

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceos.cursor.R
import com.augmentalis.voiceos.cursor.core.CursorOffset
// import com.augmentalis.voiceuielements.themes.arvision.glassMorphism
// import com.augmentalis.voiceuielements.themes.arvision.GlassMorphismConfig
// import com.augmentalis.voiceuielements.themes.arvision.DepthLevel

// Import theme utils for validation
import com.augmentalis.licensemanager.ui.glassMorphism
import com.augmentalis.licensemanager.ui.GlassMorphismConfig
import com.augmentalis.licensemanager.ui.DepthLevel

/**
 * Cursor action menu with ARVision glass morphism styling
 * Uses VoiceUIElements glass morphism for consistent theming
 */
@Composable
fun MenuView(
    isVisible: Boolean,
    position: CursorOffset,
    onAction: (CursorAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    
    // Glass morphism configuration for ARVision theme
    val glassMorphismConfig = remember {
        GlassMorphismConfig(
            cornerRadius = 20.dp,
            backgroundOpacity = 0.8f,
            borderOpacity = 0.6f,
            borderWidth = 0.5.dp,
            tintColor = Color(0xFF007AFF), // ARVision systemBlue
            tintOpacity = 0.1f
        )
    }
    
    // Animation for menu appearance
    val animationSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
            animationSpec = animationSpec,
            initialScale = 0.8f
        ),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
            animationSpec = tween(150),
            targetScale = 0.8f
        )
    ) {
        Box(
            modifier = modifier
                .offset(
                    x = position.x.dp - 100.dp, // Center menu on cursor
                    y = position.y.dp - 150.dp  // Position above cursor
                )
                .width(200.dp)
                .glassMorphism(
                    config = glassMorphismConfig,
                    depth = DepthLevel(1.2f)
                )
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Menu title
                Text(
                    text = stringResource(R.string.cursor_menu_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1D1D1F) // ARVision label color
                )
                
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(0.5.dp)
                        .background(Color(0x40000000))
                )
                
                // Menu items
                val menuItems = getMenuItems()
                
                menuItems.forEach { item ->
                    CursorMenuItemView(
                        item = item,
                        onAction = { action ->
                            // Haptic feedback
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(50)
                            }
                            
                            onAction(action)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual menu item with ARVision styling and interaction feedback
 */
@Composable
private fun CursorMenuItemView(
    item: CursorMenuItem,
    onAction: (CursorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "menu_item_scale"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp) // ARVision touch target
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isPressed) {
                        listOf(
                            Color(0x1A007AFF),
                            Color(0x0D007AFF)
                        )
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    }
                )
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                onAction(item.action)
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = item.title,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF007AFF) // ARVision systemBlue
        )
        
        // Title
        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1D1D1F), // ARVision label color
            modifier = Modifier.weight(1f)
        )
        
        // Subtle arrow indicator
        Icon(
            painter = painterResource(android.R.drawable.ic_menu_send),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF8E8E93) // ARVision systemGray
        )
    }
    
    // Handle press state for animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * Menu item data class
 */
private data class CursorMenuItem(
    val title: String,
    val iconRes: Int,
    val action: CursorAction
)

/**
 * Available cursor actions
 */
enum class CursorAction {
    SINGLE_CLICK,
    DOUBLE_CLICK, 
    LONG_PRESS,
    DRAG_START,
    DRAG_END,
    SCROLL_UP,
    SCROLL_DOWN,
    CENTER_CURSOR,
    HIDE_CURSOR,
    TOGGLE_COORDINATES,
    SHOW_HELP,
    SHOW_SETTINGS,
    CALIBRATE_CLICK
}

/**
 * Get menu items configuration
 */
@Composable
private fun getMenuItems(): List<CursorMenuItem> {
    return listOf(
        CursorMenuItem(
            title = stringResource(R.string.action_click),
            iconRes = R.drawable.menu_item_click,
            action = CursorAction.SINGLE_CLICK
        ),
        CursorMenuItem(
            title = stringResource(R.string.action_double_click),
            iconRes = R.drawable.menu_item_click,
            action = CursorAction.DOUBLE_CLICK
        ),
        CursorMenuItem(
            title = stringResource(R.string.action_long_press),
            iconRes = R.drawable.menu_item_click,
            action = CursorAction.LONG_PRESS
        ),
        CursorMenuItem(
            title = stringResource(R.string.action_drag),
            iconRes = R.drawable.menu_item_drag,
            action = CursorAction.DRAG_START
        ),
        CursorMenuItem(
            title = stringResource(R.string.action_scroll),
            iconRes = R.drawable.menu_item_scroll,
            action = CursorAction.SCROLL_UP
        ),
        CursorMenuItem(
            title = stringResource(R.string.action_center),
            iconRes = android.R.drawable.ic_menu_mylocation,
            action = CursorAction.CENTER_CURSOR
        ),
        CursorMenuItem(
            title = stringResource(R.string.action_toggle_coordinates),
            iconRes = android.R.drawable.ic_menu_view,
            action = CursorAction.TOGGLE_COORDINATES
        ),
        CursorMenuItem(
            title = stringResource(R.string.action_hide),
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
            action = CursorAction.HIDE_CURSOR
        )
    )
}

/**
 * Click accuracy calibration data class
 */
data class ClickCalibrationData(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val accuracy: Float = 1.0f,
    val sampleCount: Int = 0
)

/**
 * Edge detection result
 */
data class EdgeDetectionResult(
    val isAtEdge: Boolean,
    val edgeType: EdgeType,
    val bounceVector: Offset
)

/**
 * Edge types for detection
 */
enum class EdgeType {
    NONE,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}