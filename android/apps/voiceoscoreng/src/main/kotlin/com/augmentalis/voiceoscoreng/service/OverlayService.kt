package com.augmentalis.voiceoscoreng.service

import android.annotation.SuppressLint
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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import android.util.Log
import com.augmentalis.voiceoscoreng.MainActivity
import com.augmentalis.voiceoscoreng.app.R
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "OverlayService"

/**
 * Foreground service that displays the numbers overlay on top of all apps.
 * The overlay shows numbered badges on list items for voice selection.
 *
 * Settings are accessed via System Settings > Accessibility > VoiceOS > Settings
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var numbersOverlayView: View? = null
    private var dialogOverlayView: View? = null  // Separate touchable overlay for dialogs
    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()
    )

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val CHANNEL_ID = "voiceos_overlay_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            Log.d(TAG, "start() called")
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service")
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "startForeground done")
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            showNumbersOverlay()
            observeDialogState()  // Start observing dialog state
            Log.d(TAG, "showNumbersOverlay done")
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Called when the app's task is removed (swiped from recent apps).
     * Restart the service to maintain overlay persistence.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved - restarting service to maintain overlay")
        val restartIntent = Intent(applicationContext, OverlayService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartIntent,
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
        serviceScope.cancel()
        removeNumbersOverlay()
        removeDialogOverlay()
        super.onDestroy()
    }

    /**
     * Observe the dialog state and show/hide dialog overlay accordingly.
     */
    private fun observeDialogState() {
        Log.d(TAG, "Starting dialog state observation")
        serviceScope.launch {
            VoiceOSAccessibilityService.showAppDetectionDialog.collect { packageName ->
                Log.d(TAG, "Dialog state changed: packageName=$packageName")
                if (packageName != null) {
                    Log.d(TAG, "Showing dialog for package: $packageName")
                    showDialogOverlay()
                } else {
                    Log.d(TAG, "Hiding dialog")
                    removeDialogOverlay()
                }
            }
        }
    }

    /**
     * Show the dialog overlay (touchable).
     */
    private fun showDialogOverlay() {
        if (dialogOverlayView != null) return

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                // Touchable but doesn't block other touches outside the dialog
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.FILL
            }

            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    DialogOverlayContent()
                }
            }

            dialogOverlayView = composeView
            windowManager.addView(composeView, params)
            Log.d(TAG, "Dialog overlay added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog overlay", e)
        }
    }

    /**
     * Remove the dialog overlay.
     */
    private fun removeDialogOverlay() {
        dialogOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing dialog overlay", e)
            }
            dialogOverlayView = null
            Log.d(TAG, "Dialog overlay removed")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoiceOS Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VoiceOS accessibility overlay"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Show the numbers overlay - full screen transparent layer with numbered badges.
     */
    private fun showNumbersOverlay() {
        if (numbersOverlayView != null) return

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                // Not focusable, not touchable - just visual overlay
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.FILL
            }

            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
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
// Numbers Overlay - Shows numbered badges on list items for voice selection
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Full-screen overlay that displays numbered badges at element positions.
 * Users can say "first", "second", "1", "2", etc. to select items.
 */
@Composable
private fun NumbersOverlayContent() {
    val showOverlay by VoiceOSAccessibilityService.showNumbersOverlayComputed.collectAsState()
    val items by VoiceOSAccessibilityService.numberedOverlayItems.collectAsState()
    val mode by VoiceOSAccessibilityService.numbersOverlayMode.collectAsState()

    // Don't show if mode is OFF or if computed visibility says no
    if (!showOverlay) {
        return
    }

    // For AUTO mode, also check if there are items
    if (mode == OverlayStateManager.NumbersOverlayMode.AUTO && items.isEmpty()) {
        return
    }

    // Full-screen transparent container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Draw numbered badge for each item
        items.forEach { item ->
            NumberBadge(item)
        }

        // Instruction panel at bottom
        if (items.isNotEmpty()) {
            NumbersInstructionPanel(
                itemCount = items.size,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Dialog overlay content - shown in a separate touchable overlay.
 */
@Composable
private fun DialogOverlayContent() {
    val showAppDialog by VoiceOSAccessibilityService.showAppDetectionDialog.collectAsState()
    val appName by VoiceOSAccessibilityService.currentDetectedAppName.collectAsState()

    showAppDialog?.let { packageName ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AppDetectionDialog(
                appName = appName ?: packageName,
                packageName = packageName,
                modifier = Modifier
            )
        }
    }
}

/**
 * Dialog shown when user enters a target app for the first time.
 * Asks user how they want to handle numbers overlay for this app.
 */
@Composable
private fun AppDetectionDialog(
    appName: String,
    packageName: String,
    modifier: Modifier = Modifier
) {
    // Semi-transparent backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    )

    Card(
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 320.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Enable Voice Numbers?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App name
            Text(
                text = appName,
                color = Color(0xFF6366F1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = "This app has list items that can be selected by voice. Would you like to enable numbered badges?",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto (Recommended)
                DialogButton(
                    text = "Auto (Recommended)",
                    color = Color(0xFF10B981),
                    isPrimary = true,
                    onClick = {
                        VoiceOSAccessibilityService.handleAppDetectionResponse(
                            packageName,
                            OverlayStateManager.AppNumbersPreference.AUTO
                        )
                    }
                )

                // Always Show
                DialogButton(
                    text = "Always Show",
                    color = Color(0xFF3B82F6),
                    onClick = {
                        VoiceOSAccessibilityService.handleAppDetectionResponse(
                            packageName,
                            OverlayStateManager.AppNumbersPreference.ALWAYS
                        )
                    }
                )

                // Never
                DialogButton(
                    text = "Never",
                    color = Color(0xFF6B7280),
                    onClick = {
                        VoiceOSAccessibilityService.handleAppDetectionResponse(
                            packageName,
                            OverlayStateManager.AppNumbersPreference.NEVER
                        )
                    }
                )

                // Ask Later
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ask Later",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable {
                                VoiceOSAccessibilityService.dismissAppDetectionDialog()
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    color: Color,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) color else color.copy(alpha = 0.15f),
            contentColor = if (isPrimary) Color.White else color
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

/**
 * Individual numbered badge positioned at element bounds.
 * Uses theme colors from settings.
 */
@Composable
private fun NumberBadge(item: OverlayStateManager.NumberOverlayItem) {
    // Get theme from settings
    val theme by VoiceOSAccessibilityService.badgeTheme.collectAsState()
    val density = LocalDensity.current

    // Position badge at top-left corner of element
    // Element bounds are in PIXELS, must convert to dp for Compose offset
    val badgeSize = 28.dp
    val offsetXPx = (item.left + 8).coerceAtLeast(0)  // Left side, slight inset
    val offsetYPx = (item.top + 8).coerceAtLeast(0)   // Top of element, slight inset

    // Convert pixels to dp
    val offsetXDp = with(density) { offsetXPx.toDp() }
    val offsetYDp = with(density) { offsetYPx.toDp() }

    Box(
        modifier = Modifier
            .offset(x = offsetXDp, y = offsetYDp)
    ) {
        // Badge circle with number - uses theme colors
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

        // Show short label below badge if available
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

/**
 * Instruction panel shown at bottom of screen.
 * Supports fade animation based on InstructionBarMode setting.
 */
@Composable
private fun NumbersInstructionPanel(
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val mode by VoiceOSAccessibilityService.instructionBarMode.collectAsState()

    // Don't show if mode is OFF
    if (mode == OverlayStateManager.InstructionBarMode.OFF) {
        return
    }

    // For AUTO mode, show briefly then fade out
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "instruction_fade"
    )

    // Trigger fade after delay in AUTO mode
    LaunchedEffect(mode, itemCount) {
        if (mode == OverlayStateManager.InstructionBarMode.AUTO) {
            visible = true
            kotlinx.coroutines.delay(3000)  // Show for 3 seconds
            visible = false
        } else {
            visible = true  // Always visible in ON mode
        }
    }

    // Don't render if completely faded
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
            // Mic icon
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
