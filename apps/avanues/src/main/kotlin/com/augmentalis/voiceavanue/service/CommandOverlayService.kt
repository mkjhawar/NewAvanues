/**
 * CommandOverlayService.kt - Foreground service for numbered badge overlay
 *
 * Displays numbered badges on screen elements for voice selection.
 * Migrated from VoiceOS OverlayService, adapted for Avanues consolidated app.
 *
 * Flow:
 * 1. VoiceAvanueAccessibilityService starts this service
 * 2. DynamicCommandGenerator updates OverlayStateManager with overlay items
 * 3. This service observes OverlayStateManager and renders badges
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.augmentalis.voiceavanue.MainActivity

private const val TAG = "CommandOverlayService"

/**
 * Foreground service that displays numbered badge overlay on top of all apps.
 */
class CommandOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var notificationManager: NotificationManager
    private var numbersOverlayView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val CHANNEL_ID = "avanues_overlay_channel"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            Log.d(TAG, "start() called")
            val intent = Intent(context, CommandOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CommandOverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            showNumbersOverlay()
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved - restarting service to maintain overlay")
        val restartIntent = Intent(applicationContext, CommandOverlayService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeNumbersOverlay()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Avanues Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Numbered badge overlay for voice commands"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceOS Active")
            .setContentText("Voice commands ready")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    private fun showNumbersOverlay() {
        if (numbersOverlayView != null) return

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.FILL
            }

            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@CommandOverlayService)
                setViewTreeSavedStateRegistryOwner(this@CommandOverlayService)
                setContent {
                    NumbersOverlayContent()
                }
            }

            numbersOverlayView = composeView
            windowManager.addView(composeView, params)
            Log.d(TAG, "Numbers overlay added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing numbers overlay", e)
        }
    }

    private fun removeNumbersOverlay() {
        numbersOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing numbers overlay", e)
            }
            numbersOverlayView = null
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Numbers Overlay Composables
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NumbersOverlayContent() {
    val showOverlay by OverlayStateManager.showNumbersOverlayComputed.collectAsState()
    val items by OverlayStateManager.numberedOverlayItems.collectAsState()
    val mode by OverlayStateManager.numbersOverlayMode.collectAsState()

    if (!showOverlay) return
    if (mode == OverlayStateManager.NumbersOverlayMode.AUTO && items.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        items.forEach { item ->
            key(item.avid) {
                NumberBadge(item)
            }
        }

        if (items.isNotEmpty()) {
            NumbersInstructionPanel(
                itemCount = items.size,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun NumberBadge(item: OverlayStateManager.NumberOverlayItem) {
    val theme by OverlayStateManager.badgeTheme.collectAsState()
    val density = LocalDensity.current

    val badgeSize = 28.dp
    val offsetXPx = (item.left + 8).coerceAtLeast(0)
    val offsetYPx = (item.top + 8).coerceAtLeast(0)
    val offsetXDp = with(density) { offsetXPx.toDp() }
    val offsetYDp = with(density) { offsetYPx.toDp() }

    Box(modifier = Modifier.offset(x = offsetXDp, y = offsetYDp)) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(CircleShape)
                .background(Color(theme.backgroundColor))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.number.toString(),
                color = Color(theme.textColor),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (item.label.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .offset(y = badgeSize + 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xEE000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.label.take(15),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NumbersInstructionPanel(
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val mode by OverlayStateManager.instructionBarMode.collectAsState()
    if (mode == OverlayStateManager.InstructionBarMode.OFF) return

    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "instruction_fade"
    )

    LaunchedEffect(mode, itemCount) {
        if (mode == OverlayStateManager.InstructionBarMode.AUTO) {
            visible = true
            kotlinx.coroutines.delay(3000)
            visible = false
        } else {
            visible = true
        }
    }

    if (alpha <= 0f) return

    Card(
        modifier = modifier
            .padding(16.dp)
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE000000)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice",
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "Say a number to select",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$itemCount items: \"first\", \"second\", \"1\", \"2\"...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
