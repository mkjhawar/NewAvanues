/**
 * FeedbackToast.kt - Action confirmation toast overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-02
 */
package com.augmentalis.voiceos.ui.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

enum class FeedbackType { SUCCESS, ERROR, INFO, WARNING }

data class FeedbackData(val type: FeedbackType, val message: String, val details: String? = null, val durationMs: Long = 2000)

class FeedbackToast(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    private var feedbackData by mutableStateOf<FeedbackData?>(null)

    fun showSuccess(message: String, details: String? = null, durationMs: Long = 2000) = show(FeedbackData(FeedbackType.SUCCESS, message, details, durationMs))
    fun showError(message: String, details: String? = null, durationMs: Long = 3000) = show(FeedbackData(FeedbackType.ERROR, message, details, durationMs))
    fun showInfo(message: String, details: String? = null, durationMs: Long = 2000) = show(FeedbackData(FeedbackType.INFO, message, details, durationMs))
    fun showWarning(message: String, details: String? = null, durationMs: Long = 2500) = show(FeedbackData(FeedbackType.WARNING, message, details, durationMs))

    fun show(data: FeedbackData) {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        feedbackData = data
        if (overlayView == null) overlayView = createOverlayView()
        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
        dismissRunnable = Runnable { hide() }
        handler.postDelayed(dismissRunnable!!, data.durationMs)
    }

    fun hide() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null
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
            setContent { feedbackData?.let { FeedbackToastUI(it) } }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = 120 }
    }
}

@Composable
private fun FeedbackToastUI(data: FeedbackData) {
    val (icon, color) = when (data.type) {
        FeedbackType.SUCCESS -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        FeedbackType.ERROR -> Icons.Default.Error to Color(0xFFF44336)
        FeedbackType.INFO -> Icons.Default.Info to Color(0xFF2196F3)
        FeedbackType.WARNING -> Icons.Default.Warning to Color(0xFFFF9800)
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
        Surface(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(16.dp), color = Color(0xEE1E1E1E), shadowElevation = 12.dp) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(icon, data.type.name, tint = color, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(data.message, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    data.details?.let { Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp) }
                }
            }
        }
    }
}
