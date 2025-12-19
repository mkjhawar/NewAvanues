/**
 * ContextMenuOverlay.kt - Voice-activated context menu overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.augmentalis.voiceoscore.accessibility.ui.overlays.ComposeViewLifecycleOwner

/**
 * Data class representing a menu item
 */
data class MenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val number: Int? = null,  // Optional number for voice selection
    val enabled: Boolean = true,
    val action: () -> Unit
)

/**
 * Menu position mode
 */
enum class MenuPosition {
    CENTER,      // Center of screen
    AT_POINT,    // At specific point
    CURSOR       // At cursor/focus position
}

/**
 * Overlay that displays voice-activated context menu
 */
class ContextMenuOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false
    private var tts: TextToSpeech? = null
    private var highlightedItemState by mutableStateOf<String?>(null)

    // Mutable state for menu
    private var itemsState by mutableStateOf<List<MenuItem>>(emptyList())
    private var titleState by mutableStateOf<String?>(null)
    private var positionState by mutableStateOf(Point(0, 0))

    /**
     * Show menu at center of screen
     */
    fun showMenu(
        items: List<MenuItem>,
        title: String? = null
    ) {
        showMenuAt(items, null, title)
    }

    /**
     * Show menu at specific position
     */
    fun showMenuAt(
        items: List<MenuItem>,
        position: Point?,
        title: String? = null
    ) {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }

        itemsState = items
        titleState = title
        position?.let { positionState = it }

        if (!isShowing) {
            initTts()
            windowManager.addView(overlayView, createLayoutParams(position))
            isShowing = true
        }

        val menuDescription = if (title != null) {
            "$title menu opened with ${items.size} options"
        } else {
            "Menu opened with ${items.size} options"
        }
        announceForAccessibility(menuDescription)
    }

    /**
     * Update menu items without recreating overlay
     */
    fun updateItems(items: List<MenuItem>) {
        itemsState = items
    }

    /**
     * Highlight menu item
     */
    fun highlightItem(id: String) {
        highlightedItemState = id
        val item = itemsState.find { it.id == id }
        item?.let {
            announceForAccessibility("${it.label} highlighted")
        }
    }

    /**
     * Hide the overlay
     */
    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try {
                    announceForAccessibility("Menu closed")
                    windowManager.removeView(it)
                    isShowing = false
                    shutdownTts()
                } catch (e: IllegalArgumentException) {
                    // View not attached, ignore
                }
            }
        }
    }

    /**
     * Select item by ID
     */
    fun selectItemById(id: String): Boolean {
        val item = itemsState.find { it.id == id && it.enabled }
        return if (item != null) {
            item.action()
            true
        } else {
            false
        }
    }

    /**
     * Select item by number (for voice commands)
     */
    fun selectItemByNumber(number: Int): Boolean {
        val item = itemsState.find { it.number == number && it.enabled }
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
        shutdownTts()
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
                ContextMenuUI(
                    items = itemsState,
                    title = titleState,
                    highlightedItem = highlightedItemState,
                    onDismiss = { hide() },
                    onItemHighlight = { id -> highlightItem(id) }
                )
            }
        }
    }

    /**
     * Create window layout parameters for overlay
     */
    private fun createLayoutParams(position: Point?): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (position != null) {
                gravity = Gravity.TOP or Gravity.START
                x = position.x
                y = position.y
            } else {
                gravity = Gravity.CENTER
            }
        }
    }

    /**
     * Initialize Text-to-Speech
     */
    private fun initTts() {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                }
            }
        }
    }

    /**
     * Announce message for accessibility
     */
    private fun announceForAccessibility(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "accessibility_announcement")
    }

    /**
     * Shutdown Text-to-Speech
     */
    private fun shutdownTts() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

/**
 * Composable UI for context menu
 */
@Composable
private fun ContextMenuUI(
    items: List<MenuItem>,
    title: String?,
    highlightedItem: String?,
    onDismiss: () -> Unit,
    onItemHighlight: (String) -> Unit
) {
    AnimatedVisibility(
        visible = items.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(150))
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xEE1E1E1E) // Dark semi-transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Optional title
                title?.let {
                    MenuTitle(title = it)
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                }

                // Menu items
                items.forEach { item ->
                    ContextMenuItemUI(
                        item = item,
                        isHighlighted = item.id == highlightedItem
                    )
                }

                // Voice instruction
                if (items.any { it.number != null }) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                    VoiceInstruction()
                }
            }
        }
    }
}

/**
 * Menu title
 */
@Composable
private fun MenuTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Individual context menu item
 */
@Composable
private fun ContextMenuItemUI(
    item: MenuItem,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) { item.action() }
            .background(
                if (isHighlighted) Color(0xFF2196F3).copy(alpha = 0.2f)
                else if (!item.enabled) Color.Transparent
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Optional number badge
        item.number?.let { number ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2196F3).copy(alpha = if (item.enabled) 1f else 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Optional icon
        item.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = item.label,
                tint = if (item.enabled) Color.White else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        // Label
        Text(
            text = item.label,
            color = if (item.enabled) Color.White else Color.Gray,
            fontSize = 14.sp,
            fontWeight = if (item.enabled) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Voice instruction footer
 */
@Composable
private fun VoiceInstruction() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice input",
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = "Say number to select",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
