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
    private lateinit var notificationManager: NotificationManager
    private var numbersOverlayView: View? = null
    private var dialogOverlayView: View? = null  // Separate touchable overlay for dialogs
    private var debugFabView: View? = null       // Debug FAB overlay
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
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        try {
            startForeground(NOTIFICATION_ID, createNotification(isListening = false, transcription = null))
            Log.d(TAG, "startForeground done")
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            showNumbersOverlay()
            showDebugFab()  // Show debug FAB overlay
            observeDialogState()  // Start observing dialog state
            observeVoiceState()   // Start observing voice state for notification updates
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
        removeDebugFab()
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
     * Observe voice listening state and update notification accordingly.
     */
    private fun observeVoiceState() {
        Log.d(TAG, "Starting voice state observation")
        serviceScope.launch {
            // Observe voice listening state
            VoiceOSAccessibilityService.isVoiceListening.collect { isListening ->
                Log.d(TAG, "Voice listening state changed: $isListening")
                updateNotification(isListening, VoiceOSAccessibilityService.lastTranscription.value)
            }
        }
        serviceScope.launch {
            // Observe transcription
            VoiceOSAccessibilityService.lastTranscription.collect { transcription ->
                Log.d(TAG, "Transcription changed: $transcription")
                updateNotification(VoiceOSAccessibilityService.isVoiceListening.value, transcription)
            }
        }
    }

    /**
     * Update the foreground notification with current voice state.
     */
    private fun updateNotification(isListening: Boolean, transcription: String?) {
        val notification = createNotification(isListening, transcription)
        notificationManager.notify(NOTIFICATION_ID, notification)
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

    private fun createNotification(isListening: Boolean, transcription: String?): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dynamic title based on voice state
        val title = if (isListening) {
            "🎤 VoiceOS Listening"
        } else {
            "VoiceOS Active"
        }

        // Dynamic content based on transcription
        val content = when {
            transcription != null -> "\"${transcription.take(40)}${if (transcription.length > 40) "..." else ""}\""
            isListening -> "Say a command..."
            else -> "Voice commands ready"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(if (isListening) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
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

    /**
     * Show the debug FAB overlay (touchable).
     */
    private fun showDebugFab() {
        if (debugFabView != null) return

        try {
            // Start with WRAP_CONTENT, will be updated when expanded
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = 16
                y = 100
            }

            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    DebugFabOverlay(
                        onPositionChange = { newGravity, newX, newY ->
                            updateDebugFabPosition(newGravity, newX, newY)
                        },
                        onExpandedChange = { isExpanded ->
                            updateDebugFabSize(isExpanded)
                        }
                    )
                }
            }

            debugFabView = composeView
            windowManager.addView(composeView, params)
            Log.d(TAG, "Debug FAB overlay added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing debug FAB overlay", e)
        }
    }

    /**
     * Update the debug FAB position.
     */
    private fun updateDebugFabPosition(gravity: Int, x: Int, y: Int) {
        debugFabView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                params.gravity = gravity
                params.x = x
                params.y = y
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating debug FAB position", e)
            }
        }
    }

    /**
     * Update the debug FAB size when expanded/collapsed.
     * This is crucial for touch events to work on the expanded panel.
     */
    private fun updateDebugFabSize(isExpanded: Boolean) {
        debugFabView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                if (isExpanded) {
                    // Expanded: Fixed size to ensure touch bounds cover the panel
                    val displayMetrics = resources.displayMetrics
                    val panelWidth = (280 * displayMetrics.density).toInt()
                    val panelHeight = (displayMetrics.heightPixels * 0.5f).toInt() // Max half screen
                    params.width = panelWidth
                    params.height = panelHeight
                    Log.d(TAG, "Debug FAB expanded: ${panelWidth}x${panelHeight}")
                } else {
                    // Collapsed: Small FAB size
                    params.width = WindowManager.LayoutParams.WRAP_CONTENT
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                    Log.d(TAG, "Debug FAB collapsed: WRAP_CONTENT")
                }
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating debug FAB size", e)
            }
        }
    }

    /**
     * Remove the debug FAB overlay.
     */
    private fun removeDebugFab() {
        debugFabView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing debug FAB overlay", e)
            }
            debugFabView = null
            Log.d(TAG, "Debug FAB overlay removed")
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

// ═══════════════════════════════════════════════════════════════════════════════
// Debug FAB Overlay - Floating debug panel for monitoring VoiceOS state
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Position options for the debug panel.
 */
enum class DebugPanelPosition {
    TOP_START, TOP_END, BOTTOM_START, BOTTOM_END
}

/**
 * Size options for the expanded debug panel.
 */
enum class DebugPanelSize(val fraction: Float, val label: String) {
    COMPACT(0.2f, "1/5"),
    SMALL(0.25f, "1/4"),
    MEDIUM(0.33f, "1/3"),
    LARGE(0.5f, "1/2")
}

/**
 * Debug FAB Overlay - Collapsible floating panel for monitoring VoiceOS.
 *
 * Features:
 * - Collapsed: Small FAB showing element count
 * - Expanded: Metrics panel with controls
 * - Numbers mode toggle (OFF/ON/AUTO)
 * - Position controls (corners)
 * - Size controls (1/5, 1/4, 1/3, 1/2 of screen)
 */
@Composable
private fun DebugFabOverlay(
    onPositionChange: (Int, Int, Int) -> Unit,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(DebugPanelPosition.BOTTOM_END) }
    var panelSize by remember { mutableStateOf(DebugPanelSize.SMALL) }

    // Notify parent when expanded state changes for window size updates
    LaunchedEffect(isExpanded) {
        onExpandedChange(isExpanded)
    }

    // Collect metrics from accessibility service
    val screenInfo by VoiceOSAccessibilityService.currentScreenInfo.collectAsState()
    val numberedItems by VoiceOSAccessibilityService.numberedOverlayItems.collectAsState()
    val numbersMode by VoiceOSAccessibilityService.numbersOverlayMode.collectAsState()
    val isConnected by VoiceOSAccessibilityService.isConnected.collectAsState()
    val lastError by VoiceOSAccessibilityService.lastError.collectAsState()
    val isVoiceListening by VoiceOSAccessibilityService.isVoiceListening.collectAsState()
    val lastTranscription by VoiceOSAccessibilityService.lastTranscription.collectAsState()

    // Animation for expand/collapse
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "expand"
    )

    // Show transcription bubble when voice is detected
    Column(
        horizontalAlignment = Alignment.End
    ) {
        // Transcription bubble (show above FAB when speaking)
        lastTranscription?.let { text ->
            Card(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .widthIn(max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0101020))
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text.take(50),
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isExpanded) {
            // Expanded panel
            DebugMetricsPanel(
                screenInfo = screenInfo,
                numberedItems = numberedItems,
                numbersMode = numbersMode,
                isConnected = isConnected,
                isVoiceListening = isVoiceListening,
                lastError = lastError,
                panelSize = panelSize,
                position = position,
                onCollapse = { isExpanded = false },
                onSizeChange = { panelSize = it },
                onPositionChange = { newPos ->
                    position = newPos
                    val (gravity, x, y) = when (newPos) {
                        DebugPanelPosition.TOP_START -> Triple(Gravity.TOP or Gravity.START, 16, 100)
                        DebugPanelPosition.TOP_END -> Triple(Gravity.TOP or Gravity.END, 16, 100)
                        DebugPanelPosition.BOTTOM_START -> Triple(Gravity.BOTTOM or Gravity.START, 16, 100)
                        DebugPanelPosition.BOTTOM_END -> Triple(Gravity.BOTTOM or Gravity.END, 16, 100)
                    }
                    onPositionChange(gravity, x, y)
                },
                onNumbersModeChange = { mode ->
                    OverlayStateManager.setNumbersOverlayMode(mode)
                }
            )
        } else {
            // Collapsed FAB with voice indicator
            DebugFabCollapsed(
                elementCount = numberedItems.size,
                isConnected = isConnected,
                isVoiceListening = isVoiceListening,
                hasError = lastError != null,
                onClick = { isExpanded = true }
            )
        }
    }
}

/**
 * Collapsed FAB showing quick status with voice indicator.
 */
@Composable
private fun DebugFabCollapsed(
    elementCount: Int,
    isConnected: Boolean,
    isVoiceListening: Boolean,
    hasError: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        hasError -> Color(0xFFEF4444)  // Red for error
        !isConnected -> Color(0xFF6B7280)  // Gray for disconnected
        elementCount > 0 -> Color(0xFF10B981)  // Green for active
        else -> Color(0xFF3B82F6)  // Blue for idle
    }

    // Pulsing animation for voice listening
    val pulseAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(isVoiceListening) {
        if (isVoiceListening) {
            while (true) {
                pulseAnim.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(500)
                )
                pulseAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(500)
                )
            }
        } else {
            pulseAnim.snapTo(1f)
        }
    }

    Box(contentAlignment = Alignment.Center) {
        // Outer pulse ring when listening
        if (isVoiceListening) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = pulseAnim.value
                        scaleY = pulseAnim.value
                        alpha = 2f - pulseAnim.value
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.4f))
            )
        }

        // Main FAB
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (elementCount > 0) {
                Text(
                    text = elementCount.toString(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Debug",
                    tint = if (isVoiceListening) Color(0xFF10B981) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Voice listening indicator dot
        if (isVoiceListening && elementCount == 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * Expanded metrics panel with detailed info and controls.
 */
@Composable
private fun DebugMetricsPanel(
    screenInfo: com.augmentalis.commandmanager.ScreenInfo?,
    numberedItems: List<OverlayStateManager.NumberOverlayItem>,
    numbersMode: OverlayStateManager.NumbersOverlayMode,
    isConnected: Boolean,
    isVoiceListening: Boolean,
    lastError: String?,
    panelSize: DebugPanelSize,
    position: DebugPanelPosition,
    onCollapse: () -> Unit,
    onSizeChange: (DebugPanelSize) -> Unit,
    onPositionChange: (DebugPanelPosition) -> Unit,
    onNumbersModeChange: (OverlayStateManager.NumbersOverlayMode) -> Unit
) {
    val density = LocalDensity.current
    val screenHeightDp = with(density) {
        android.content.res.Resources.getSystem().displayMetrics.heightPixels.toDp()
    }
    val panelHeight = screenHeightDp * panelSize.fraction

    Card(
        modifier = Modifier
            .width(280.dp)
            .heightIn(max = panelHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0101020)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header with collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VoiceOS Debug",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Size selector
                    DebugPanelSize.entries.forEach { size ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (size == panelSize) Color(0xFF3B82F6)
                                    else Color(0xFF374151)
                                )
                                .clickable { onSizeChange(size) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = size.label,
                                color = Color.White,
                                fontSize = 8.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Collapse button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF374151))
                            .clickable(onClick = onCollapse),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status indicators row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 10.sp
                    )
                }
                // Voice status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isVoiceListening) Color(0xFF10B981) else Color(0xFF6B7280),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isVoiceListening) "Listening" else "Off",
                        color = if (isVoiceListening) Color(0xFF10B981) else Color(0xFF6B7280),
                        fontSize = 10.sp
                    )
                }
            }

            // Error message
            lastError?.let { error ->
                Text(
                    text = error.take(50),
                    color = Color(0xFFEF4444),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFF374151), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Metrics
            MetricRow("Elements", numberedItems.size.toString())
            screenInfo?.let { info ->
                MetricRow("Actionable", info.actionableCount.toString())
                MetricRow("Commands", info.commandCount.toString())
                MetricRow("Package", info.packageName.substringAfterLast("."))
                info.activityName?.let { activity ->
                    MetricRow("Activity", activity.substringAfterLast("."))
                }
                MetricRow("Cached", if (info.isCached) "Yes" else "No")
            } ?: run {
                MetricRow("Screen", "Not scanned")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFF374151), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Numbers mode toggle
            Text(
                text = "Numbers Mode",
                color = Color(0xFF9CA3AF),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OverlayStateManager.NumbersOverlayMode.entries.forEach { mode ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (mode == numbersMode) Color(0xFF3B82F6)
                                else Color(0xFF374151)
                            )
                            .clickable { onNumbersModeChange(mode) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (mode == numbersMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Position controls
            Text(
                text = "Position",
                color = Color(0xFF9CA3AF),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    DebugPanelPosition.TOP_START to "TL",
                    DebugPanelPosition.TOP_END to "TR",
                    DebugPanelPosition.BOTTOM_START to "BL",
                    DebugPanelPosition.BOTTOM_END to "BR"
                ).forEach { (pos, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (pos == position) Color(0xFF3B82F6)
                                else Color(0xFF374151)
                            )
                            .clickable { onPositionChange(pos) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (pos == position) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single metric row in the debug panel.
 */
@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
