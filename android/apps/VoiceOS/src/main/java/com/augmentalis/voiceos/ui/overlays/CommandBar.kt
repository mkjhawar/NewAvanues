/**
 * CommandBar.kt - Expanded command interface overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-02
 */
package com.augmentalis.voiceos.ui.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class CommandCategory(val id: String, val name: String, val icon: ImageVector, val commands: List<QuickCommand>)
data class QuickCommand(val id: String, val phrase: String, val description: String? = null, val icon: ImageVector? = null)

class CommandBar(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false

    private var categoriesState by mutableStateOf<List<CommandCategory>>(emptyList())
    private var selectedCategoryId by mutableStateOf<String?>(null)
    private var isListening by mutableStateOf(false)
    private var partialText by mutableStateOf("")
    private var onCommandSelected: ((QuickCommand) -> Unit)? = null

    fun show(categories: List<CommandCategory>) {
        categoriesState = categories
        selectedCategoryId = categories.firstOrNull()?.id
        if (overlayView == null) overlayView = createOverlayView()
        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
    }

    fun setListening(listening: Boolean, partial: String = "") { isListening = listening; partialText = partial }
    fun setOnCommandSelectedListener(listener: (QuickCommand) -> Unit) { onCommandSelected = listener }

    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try { windowManager.removeView(it); isShowing = false } catch (e: IllegalArgumentException) { }
            }
        }
    }

    fun isVisible(): Boolean = isShowing
    fun dispose() { hide(); lifecycleOwner?.onDestroy(); lifecycleOwner = null; overlayView = null }

    private fun createOverlayView(): ComposeView {
        val owner = ComposeViewLifecycleOwner().also { lifecycleOwner = it; it.onCreate() }
        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                CommandBarUI(categoriesState, selectedCategoryId, isListening, partialText,
                    { selectedCategoryId = it }, { cmd -> onCommandSelected?.invoke(cmd); hide() }, { hide() })
            }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
    }

    companion object {
        fun defaultCategories(): List<CommandCategory> = listOf(
            CommandCategory("navigation", "Navigate", Icons.Default.Navigation, listOf(
                QuickCommand("scroll_up", "Scroll up"), QuickCommand("scroll_down", "Scroll down"),
                QuickCommand("go_back", "Go back"), QuickCommand("go_home", "Go home"), QuickCommand("open_recents", "Show recents"))),
            CommandCategory("actions", "Actions", Icons.Default.TouchApp, listOf(
                QuickCommand("tap", "Tap [element]"), QuickCommand("long_press", "Long press"),
                QuickCommand("double_tap", "Double tap"), QuickCommand("swipe_left", "Swipe left"), QuickCommand("swipe_right", "Swipe right"))),
            CommandCategory("input", "Input", Icons.Default.Keyboard, listOf(
                QuickCommand("type", "Type [text]"), QuickCommand("clear", "Clear text"),
                QuickCommand("copy", "Copy text"), QuickCommand("paste", "Paste"), QuickCommand("select_all", "Select all"))),
            CommandCategory("system", "System", Icons.Default.Settings, listOf(
                QuickCommand("volume_up", "Volume up"), QuickCommand("volume_down", "Volume down"),
                QuickCommand("mute", "Mute"), QuickCommand("notifications", "Show notifications"), QuickCommand("quick_settings", "Quick settings")))
        )
    }
}

@Composable
private fun CommandBarUI(
    categories: List<CommandCategory>, selectedCategoryId: String?, isListening: Boolean, partialText: String,
    onCategorySelected: (String) -> Unit, onCommandSelected: (QuickCommand) -> Unit, onDismiss: () -> Unit
) {
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Color(0xEE1E1E1E), shadowElevation = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(12.dp))

            if (isListening) {
                val infiniteTransition = rememberInfiniteTransition(label = "listening")
                val pulseAlpha by infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "pulse")
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2196F3).copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, null, tint = Color(0xFF2196F3).copy(alpha = pulseAlpha), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (partialText.isNotEmpty()) partialText else "Listening...", color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Voice Commands", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.7f)) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    Surface(modifier = Modifier.clickable { onCategorySelected(category.id) }, shape = RoundedCornerShape(20.dp),
                        color = if (category.id == selectedCategoryId) Color(0xFF2196F3) else Color(0xFF2E2E2E)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(category.icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(category.name, color = Color.White, fontSize = 13.sp, fontWeight = if (category.id == selectedCategoryId) FontWeight.Medium else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            selectedCategory?.let { cat ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cat.commands.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { command ->
                                Surface(modifier = Modifier.weight(1f).clickable { onCommandSelected(command) }, shape = RoundedCornerShape(12.dp), color = Color(0xFF2E2E2E)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        command.icon?.let { Icon(it, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("\"${command.phrase}\"", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            command.description?.let { Text(it, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1) }
                                        }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Say \"Hey AVA\" followed by a command, or tap to select", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
