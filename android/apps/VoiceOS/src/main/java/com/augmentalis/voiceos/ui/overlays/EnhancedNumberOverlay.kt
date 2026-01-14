/**
 * EnhancedNumberOverlay.kt - Number overlay with FG/BG element support
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-02
 */
package com.augmentalis.voiceos.ui.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

data class EnhancedSelectableItem(
    val number: Int, val label: String, val bounds: Rect,
    val isForeground: Boolean = true, val windowTitle: String? = null, val action: () -> Unit
)

class EnhancedNumberOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false
    private var tts: TextToSpeech? = null

    private val itemsState = mutableStateOf<List<EnhancedSelectableItem>>(emptyList())
    private var showBackgroundElements by mutableStateOf(true)

    fun showItems(items: List<EnhancedSelectableItem>) {
        if (overlayView == null) overlayView = createOverlayView()
        itemsState.value = items
        if (!isShowing) {
            initTts()
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
        val fgCount = items.count { it.isForeground }
        val bgCount = items.count { !it.isForeground }
        val msg = buildString {
            append("${items.size} items available. ")
            if (bgCount > 0) append("$fgCount foreground, $bgCount background. ")
            append("Say a number to select.")
        }
        announceForAccessibility(msg)
    }

    fun setShowBackgroundElements(show: Boolean) { showBackgroundElements = show }
    fun updateItems(items: List<EnhancedSelectableItem>) { itemsState.value = items }

    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try { windowManager.removeView(it); isShowing = false; shutdownTts() } catch (e: IllegalArgumentException) { }
            }
        }
    }

    fun selectItem(number: Int): Boolean {
        val item = itemsState.value.find { it.number == number } ?: return false
        provideHapticFeedback()
        val msg = if (item.isForeground) "Selected ${item.label.ifEmpty { "item $number" }}"
        else "Selected background element ${item.label.ifEmpty { "item $number" }} from ${item.windowTitle ?: "background window"}"
        announceForAccessibility(msg)
        item.action()
        return true
    }

    fun isVisible(): Boolean = isShowing
    fun dispose() { hide(); shutdownTts(); lifecycleOwner?.onDestroy(); lifecycleOwner = null; overlayView = null }

    private fun createOverlayView(): ComposeView {
        val owner = ComposeViewLifecycleOwner().also { lifecycleOwner = it; it.onCreate() }
        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val items by remember { itemsState }
                val showBg = showBackgroundElements
                EnhancedNumberOverlayUI(if (showBg) items else items.filter { it.isForeground }, items.any { !it.isForeground })
            }
        }
    }

    private fun createLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.FILL }

    private fun initTts() { if (tts == null) tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() } }
    private fun announceForAccessibility(msg: String) { tts?.speak(msg, TextToSpeech.QUEUE_ADD, null, "a11y") }
    private fun shutdownTts() { tts?.stop(); tts?.shutdown(); tts = null }

    private fun provideHapticFeedback() {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") it.vibrate(50) }
        } catch (_: Exception) { }
    }
}

@Composable
private fun EnhancedNumberOverlayUI(items: List<EnhancedSelectableItem>, hasBackgroundItems: Boolean) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
        items.forEach { EnhancedNumberBadge(it) }
        EnhancedInstructionPanel(items.size, items.count { it.isForeground }, items.count { !it.isForeground }, hasBackgroundItems)
    }
}

@Composable
private fun EnhancedNumberBadge(item: EnhancedSelectableItem) {
    val badgeColor = when { !item.isForeground -> Color(0xFFFF9800); item.label.isNotEmpty() -> Color(0xFF4CAF50); else -> Color(0xFFF57C00) }
    val badgeAlpha = if (item.isForeground) 1f else 0.7f
    val borderStyle = if (item.isForeground) Stroke(6f) else Stroke(6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f))

    Box(modifier = Modifier.offset(x = (item.bounds.right - 36).dp, y = (item.bounds.top + 4).dp)) {
        AnimatedVisibility(true, fadeIn() + scaleIn(), fadeOut() + scaleOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(32.dp).alpha(badgeAlpha).clip(CircleShape).background(badgeColor)
                    .drawBehind { drawCircle(Color.White, style = borderStyle) }, contentAlignment = Alignment.Center) {
                    Text(item.number.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (!item.isForeground) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF9800).copy(alpha = 0.3f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        Text("BG", color = Color(0xFFFF9800), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (item.label.isNotEmpty()) {
        Box(modifier = Modifier.offset(x = (item.bounds.right - 80).dp, y = (item.bounds.top + (if (item.isForeground) 40 else 52)).dp)) {
            AnimatedVisibility(true, fadeIn(), fadeOut()) {
                Box(modifier = Modifier.alpha(if (item.isForeground) 1f else 0.7f).clip(RoundedCornerShape(8.dp)).background(Color(0xEE000000))
                    .then(if (!item.isForeground) Modifier.border(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(8.dp)) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(item.label.take(20), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EnhancedInstructionPanel(count: Int, fgCount: Int, bgCount: Int, hasBackgroundItems: Boolean) {
    Box(Modifier.fillMaxSize().padding(bottom = 16.dp), Alignment.BottomCenter) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color(0xEE000000)), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Mic, "Voice", tint = Color(0xFF2196F3), modifier = Modifier.size(32.dp))
                    Column {
                        Text("Say a number to select", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (hasBackgroundItems) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                    Spacer(Modifier.width(4.dp))
                                    Text("$fgCount FG", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                                    Spacer(Modifier.width(4.dp))
                                    Text("$bgCount BG", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text("$count items available", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
                if (hasBackgroundItems) {
                    Spacer(Modifier.height(8.dp))
                    Text("Background elements shown with dashed border", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }
            }
        }
    }
}
