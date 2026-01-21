/**
 * ListeningPill.kt - Minimal listening indicator overlay
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

enum class ListeningState {
    WAKE_DETECTED, LISTENING, PROCESSING, TIMEOUT
}

class ListeningPill(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false

    private var stateValue by mutableStateOf(ListeningState.LISTENING)
    private var partialTextValue by mutableStateOf("")

    fun showWakeDetected() { stateValue = ListeningState.WAKE_DETECTED; show() }
    fun showListening() { stateValue = ListeningState.LISTENING; partialTextValue = ""; show() }
    fun updatePartialText(text: String) { partialTextValue = text; stateValue = ListeningState.LISTENING }
    fun showProcessing() { stateValue = ListeningState.PROCESSING }
    fun showTimeout() { stateValue = ListeningState.TIMEOUT }

    fun show() {
        if (overlayView == null) overlayView = createOverlayView()
        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
    }

    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try { windowManager.removeView(it); isShowing = false } catch (e: IllegalArgumentException) { }
            }
        }
    }

    fun isVisible(): Boolean = isShowing

    fun dispose() {
        hide()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        overlayView = null
    }

    private fun createOverlayView(): ComposeView {
        val owner = ComposeViewLifecycleOwner().also { lifecycleOwner = it; it.onCreate() }
        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { ListeningPillUI(state = stateValue, partialText = partialTextValue) }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 60 }
    }
}

@Composable
private fun ListeningPillUI(state: ListeningState, partialText: String) {
    val pillColor = when (state) {
        ListeningState.WAKE_DETECTED -> Color(0xFF4CAF50)
        ListeningState.LISTENING -> Color(0xFF2196F3)
        ListeningState.PROCESSING -> Color(0xFFFF9800)
        ListeningState.TIMEOUT -> Color(0xFF9E9E9E)
    }
    val displayText = when (state) {
        ListeningState.WAKE_DETECTED -> "Hey AVA"
        ListeningState.LISTENING -> if (partialText.isNotEmpty()) partialText else "Listening..."
        ListeningState.PROCESSING -> "Processing..."
        ListeningState.TIMEOUT -> "Timed out"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pillPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (state == ListeningState.LISTENING) 0.6f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse), label = "pillAlpha"
    )

    Surface(modifier = Modifier.padding(8.dp), shape = RoundedCornerShape(28.dp), color = Color(0xEE1E1E1E), shadowElevation = 8.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(pillColor.copy(alpha = pulseAlpha * 0.3f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, displayText, tint = pillColor, modifier = Modifier.size(20.dp))
            }
            Text(displayText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            if (state == ListeningState.LISTENING) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { index ->
                        val barHeight by infiniteTransition.animateFloat(
                            initialValue = 4f, targetValue = when (index) { 0 -> 12f; 1 -> 16f; else -> 10f },
                            animationSpec = infiniteRepeatable(animation = tween(300 + index * 100), repeatMode = RepeatMode.Reverse), label = "bar$index"
                        )
                        Box(modifier = Modifier.width(3.dp).height(barHeight.dp).clip(RoundedCornerShape(1.5.dp)).background(pillColor))
                    }
                }
            }
        }
    }
}
