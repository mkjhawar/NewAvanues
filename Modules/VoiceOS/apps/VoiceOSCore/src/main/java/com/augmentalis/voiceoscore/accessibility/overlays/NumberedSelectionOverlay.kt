/**
 * NumberedSelectionOverlay.kt - Numbered item selection overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.augmentalis.voiceoscore.accessibility.ui.overlays.ComposeViewLifecycleOwner
import androidx.compose.ui.window.Dialog
import com.augmentalis.voiceos.accessibility.AnchorPoint
import com.augmentalis.voiceos.accessibility.BadgeStyle
import com.augmentalis.voiceos.accessibility.ElementVoiceState

/**
 * Data class representing a selectable item
 */
data class SelectableItem(
    val number: Int,
    val label: String,
    val bounds: Rect,
    val action: () -> Unit
)

/**
 * Overlay that displays numbered items for voice selection
 */
class NumberedSelectionOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false

    // Mutable state for items
    private var itemsState = mutableStateOf<List<SelectableItem>>(emptyList())

    /**
     * Show overlay with numbered items
     */
    fun showItems(items: List<SelectableItem>) {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }

        itemsState.value = items

        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
    }

    /**
     * Update items without recreating overlay
     */
    fun updateItems(items: List<SelectableItem>) {
        itemsState.value = items
    }

    /**
     * Hide the overlay
     */
    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try {
                    windowManager.removeView(it)
                    isShowing = false
                } catch (e: IllegalArgumentException) {
                    // View not attached, ignore
                }
            }
        }
    }

    /**
     * Select item by number
     */
    fun selectItem(number: Int): Boolean {
        val item = itemsState.value.find { it.number == number }
        return if (item != null) {
            item.action()
            true
        } else {
            false
        }
    }

    /**
     * Check if overlay is currently visible
     */
    fun isVisible(): Boolean = isShowing

    /**
     * Dispose and clean up resources
     */
    fun dispose() {
        hide()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        overlayView = null
    }

    /**
     * Create the compose view for the overlay
     */
    private fun createOverlayView(): ComposeView {
        val owner = ComposeViewLifecycleOwner().also {
            lifecycleOwner = it
            it.onCreate()
        }

        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val items by remember { itemsState }

                NumberedSelectionUI(items = items)
            }
        }
    }

    /**
     * Create window layout parameters for overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
        }
    }
}

/**
 * Composable UI for numbered selection
 */
@Composable
private fun NumberedSelectionUI(items: List<SelectableItem>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        // Display number badges at item positions
        items.forEach { item ->
            NumberBadge(
                number = item.number,
                label = item.label,
                bounds = item.bounds
            )
        }

        // Instruction panel at bottom
        InstructionPanel(
            message = "Say a number to select",
            count = items.size
        )
    }
}

/**
 * Number badge positioned at item location
 * Now uses Material 3 circular badge design with color-coded states
 */
@Composable
private fun NumberBadge(
    number: Int,
    label: String,
    bounds: Rect
) {
    // Determine element state based on whether it has a custom name
    val state = if (label.isNotEmpty()) {
        ElementVoiceState.ENABLED_WITH_NAME
    } else {
        ElementVoiceState.ENABLED_NO_NAME
    }

    // Get color based on state (Material 3 colors)
    val badgeColor = when (state) {
        ElementVoiceState.ENABLED_WITH_NAME -> Color(0xFF4CAF50)  // Green
        ElementVoiceState.ENABLED_NO_NAME -> Color(0xFFFF9800)    // Orange
        ElementVoiceState.DISABLED -> Color(0xFF9E9E9E)           // Grey
    }

    // Position badge at top-right of bounds (4px offset)
    Box(
        modifier = Modifier
            .absoluteOffset(
                x = (bounds.right - 36).dp,  // 32dp badge + 4dp offset
                y = (bounds.top + 4).dp
            )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            // Circular badge with drop shadow
            Box(
                modifier = Modifier
                    .size(32.dp)  // 32dp diameter
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Optional label tooltip (shown below badge if label exists)
    if (label.isNotEmpty()) {
        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = (bounds.right - 80).dp,  // Centered under badge
                    y = (bounds.top + 40).dp      // Below the badge
                )
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xEE000000))  // Semi-transparent black
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label.take(20),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Instruction panel at bottom of screen
 */
@Composable
private fun InstructionPanel(
    message: String,
    count: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xEE000000)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Microphone icon
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(32.dp)
                )

                // Instructions
                Column {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$count items available",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Helper extension for absolute positioning
 */
@Composable
private fun Modifier.absoluteOffset(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp): Modifier {
    return this.offset(x = x, y = y)
}
