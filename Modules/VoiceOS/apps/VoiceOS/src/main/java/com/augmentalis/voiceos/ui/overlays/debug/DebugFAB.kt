/**
 * DebugFAB.kt - Floating debug button for VoiceOS testing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-02
 */
package com.augmentalis.voiceos.ui.overlays.debug

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.augmentalis.voiceos.ui.overlays.ComposeViewLifecycleOwner
import kotlin.math.roundToInt

/**
 * Debug FAB status indicator states
 */
enum class DebugFABStatus {
    IDLE,           // Grey - service running but idle
    LISTENING,      // Blue - actively listening for "Hey AVA"
    PROCESSING,     // Orange - processing speech
    ACTIVE,         // Green - command being executed
    ERROR           // Red - error state
}

/**
 * Floating Action Button for debug panel access
 */
class DebugFAB(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ComposeViewLifecycleOwner? = null
    private var isShowing = false

    private var statusState by mutableStateOf(DebugFABStatus.IDLE)
    private var onClickListener: (() -> Unit)? = null
    private var positionY by mutableFloatStateOf(200f)

    fun show() {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }
        if (!isShowing) {
            windowManager.addView(overlayView, createLayoutParams())
            isShowing = true
        }
    }

    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try {
                    windowManager.removeView(it)
                    isShowing = false
                } catch (e: IllegalArgumentException) { }
            }
        }
    }

    fun updateStatus(status: DebugFABStatus) {
        statusState = status
    }

    fun setOnClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    fun isVisible(): Boolean = isShowing

    fun dispose() {
        hide()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        overlayView = null
    }

    private fun createOverlayView(): ComposeView {
        val owner = ComposeViewLifecycleOwner().also {
            lifecycleOwner = it
            it.onCreate()
        }

        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                DebugFABUI(
                    status = statusState,
                    onDrag = { _, y ->
                        positionY += y
                        updatePosition()
                    },
                    onClick = { onClickListener?.invoke() }
                )
            }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = positionY.roundToInt()
        }
    }

    private fun updatePosition() {
        overlayView?.let { view ->
            try {
                val params = view.layoutParams as? WindowManager.LayoutParams
                params?.let {
                    it.y = positionY.roundToInt()
                    windowManager.updateViewLayout(view, it)
                }
            } catch (e: Exception) { }
        }
    }
}

@Composable
private fun DebugFABUI(
    status: DebugFABStatus,
    onDrag: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            DebugFABStatus.IDLE -> Color(0xFF9E9E9E)
            DebugFABStatus.LISTENING -> Color(0xFF2196F3)
            DebugFABStatus.PROCESSING -> Color(0xFFFF9800)
            DebugFABStatus.ACTIVE -> Color(0xFF4CAF50)
            DebugFABStatus.ERROR -> Color(0xFFF44336)
        },
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == DebugFABStatus.LISTENING) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabScale"
    )

    Box(
        modifier = Modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
                .clickable { onClick() },
            shape = CircleShape,
            color = Color(0xEE1E1E1E),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Debug Panel",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 2.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}
