/**
 * OverlayService.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/service/OverlayService.kt
 * 
 * Created: 2025-01-26 02:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Foreground service managing VoiceCursor system overlay and lifecycle
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-26 02:00 PST): Initial creation with overlay management and notifications
 */

package com.augmentalis.voiceos.cursor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
// import androidx.lifecycle.setViewTreeLifecycleOwner
// import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.augmentalis.voiceos.cursor.R
import com.augmentalis.voiceos.cursor.service.VoiceCursorAccessibilityService
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.core.CursorType
import com.augmentalis.voiceos.cursor.core.FilterStrength
import com.augmentalis.voiceos.cursor.helper.VoiceCursorIMUIntegration
import com.augmentalis.voiceos.cursor.view.MenuView
import com.augmentalis.voiceos.cursor.view.CursorAction
import com.augmentalis.voiceos.cursor.view.CursorView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service managing VoiceCursor overlay system
 * Handles window management, notifications, and service lifecycle
 */
class VoiceCursorOverlayService : Service() {
    
    companion object {
        private const val TAG = "VoiceCursorOverlay"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voicecursor_overlay"
        
        // Action constants
        private const val ACTION_TOGGLE_CURSOR = "toggle_cursor"
        private const val ACTION_CENTER_CURSOR = "center_cursor"
        private const val ACTION_SHOW_MENU = "show_menu"
        private const val ACTION_TOGGLE_COORDINATES = "toggle_coordinates"
        private const val ACTION_STOP_SERVICE = "stop_service"
        
        // Service control
        fun start(context: Context, config: CursorConfig? = null) {
            val intent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                config?.let { putExtra("cursor_config", it) }
            }
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, VoiceCursorOverlayService::class.java)
            context.stopService(intent)
        }
    }
    
    // Window management
    private var windowManager: WindowManager? = null
    private var cursorView: CursorView? = null
    private var menuComposeView: ComposeView? = null
    
    // Configuration and state
    private var cursorConfig = CursorConfig()
    private var isOverlayVisible = true
    private var isMenuVisible = false
    
    // IMU integration
    private var imuIntegration: VoiceCursorIMUIntegration? = null
    
    // Service scope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Broadcast receiver for actions
    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_TOGGLE_CURSOR -> toggleCursor()
                ACTION_CENTER_CURSOR -> centerCursor()
                ACTION_SHOW_MENU -> showMenu()
                ACTION_TOGGLE_COORDINATES -> toggleCoordinateDisplay()
                ACTION_STOP_SERVICE -> stopSelf()
            }
        }
    }
    
    // SharedPreferences listener for real-time settings updates
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        cursorView?.let { view ->
            val sharedPrefs = getSharedPreferences("voice_cursor_prefs", Context.MODE_PRIVATE)
            
            // Build updated config based on changed setting
            val currentConfig = loadCursorConfig()
            val updatedConfig = when (key) {
                "cursor_type" -> {
                    val typeString = sharedPrefs.getString("cursor_type", "Normal") ?: "Normal"
                    val type = when (typeString) {
                        "Hand" -> CursorType.Hand
                        "Custom" -> CursorType.Custom
                        else -> CursorType.Normal
                    }
                    currentConfig.copy(type = type)
                }
                "cursor_size" -> {
                    val size = sharedPrefs.getInt("cursor_size", 48)
                    currentConfig.copy(size = size)
                }
                "cursor_color" -> {
                    val color = sharedPrefs.getInt("cursor_color", Color.BLUE)
                    currentConfig.copy(color = color)
                }
                "cursor_speed" -> {
                    val speed = sharedPrefs.getInt("cursor_speed", 8)
                    currentConfig.copy(speed = speed)
                }
                "gaze_enabled" -> {
                    val enabled = sharedPrefs.getBoolean("gaze_enabled", false)
                    // If gaze is disabled, set delay to 0, otherwise use stored delay
                    val delay = if (enabled) {
                        sharedPrefs.getLong("gaze_delay", 1500L)
                    } else {
                        0L
                    }
                    currentConfig.copy(gazeClickDelay = delay)
                }
                "gaze_delay" -> {
                    val delay = sharedPrefs.getLong("gaze_delay", 1500L)
                    currentConfig.copy(gazeClickDelay = delay)
                }
                "smoothing_strength" -> {
                    val filterStrengthValue = sharedPrefs.getInt("smoothing_strength", 50)
                    val filterStrength = when {
                        filterStrengthValue < 30 -> FilterStrength.Low
                        filterStrengthValue < 70 -> FilterStrength.Medium
                        else -> FilterStrength.High
                    }
                    currentConfig.copy(filterStrength = filterStrength)
                }
                "jitter_filter_enabled" -> {
                    val enabled = sharedPrefs.getBoolean("jitter_filter_enabled", true)
                    currentConfig.copy(jitterFilterEnabled = enabled)
                }
                "motion_sensitivity" -> {
                    val sensitivity = sharedPrefs.getFloat("motion_sensitivity", 0.7f)
                    currentConfig.copy(motionSensitivity = sensitivity)
                }
                else -> currentConfig
            }
            
            // Apply the updated configuration
            if (updatedConfig != currentConfig) {
                view.updateCursorConfiguration(updatedConfig)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService created")
        
        // Initialize window manager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Create notification channel
        createNotificationChannel()
        
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_TOGGLE_CURSOR)
            addAction(ACTION_CENTER_CURSOR)
            addAction(ACTION_SHOW_MENU)
            addAction(ACTION_TOGGLE_COORDINATES)
            addAction(ACTION_STOP_SERVICE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(actionReceiver, filter)
        }
        
        // Register SharedPreferences listener for real-time settings updates
        val sharedPrefs = getSharedPreferences("voice_cursor_prefs", Context.MODE_PRIVATE)
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)

    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OverlayService starting")
        
        // Enhanced permission validation with retry mechanism
        if (!validateAndRequestPermissions()) {
            Log.e(TAG, "Overlay permission not granted, service will stop")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Extract configuration if provided
        @Suppress("DEPRECATION")
        intent?.getParcelableExtra<CursorConfig>("cursor_config")?.let {
            cursorConfig = it
        }
        
        // Only initialize overlay if not already created
        if (cursorView == null) {
            // Start foreground service with notification
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            // Initialize cursor overlay
            initializeCursorOverlay()
        } else {
            // Just handle the action if overlay already exists
            Log.d(TAG, "Overlay already exists, handling intent actions only")
        }
        
        // Handle intent actions
        handleIntentActions(intent)
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OverlayService destroyed")
        
        // Cleanup overlay
        removeOverlay()
        
        // Cleanup IMU integration
        imuIntegration?.dispose()
        
        // Cancel coroutines
        serviceScope.cancel()
        
        // Unregister receiver
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }
        
        // Unregister SharedPreferences listener
        try {
            val sharedPrefs = getSharedPreferences("voice_cursor_prefs", Context.MODE_PRIVATE)
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering prefs listener", e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Load cursor configuration from SharedPreferences
     */
    private fun loadCursorConfig(): CursorConfig {
        val sharedPrefs = getSharedPreferences("voice_cursor_prefs", Context.MODE_PRIVATE)
        
        val typeString = sharedPrefs.getString("cursor_type", "Normal") ?: "Normal"
        val type = when (typeString) {
            "Hand" -> CursorType.Hand
            "Custom" -> CursorType.Custom
            else -> CursorType.Normal
        }
        
        val gazeEnabled = sharedPrefs.getBoolean("gaze_enabled", false)
        val gazeDelay = if (gazeEnabled) {
            sharedPrefs.getLong("gaze_delay", 1500L)
        } else {
            0L
        }
        
        // Load filter strength setting
        val filterStrengthValue = sharedPrefs.getInt("smoothing_strength", 50)
        val filterStrength = when {
            filterStrengthValue < 30 -> FilterStrength.Low
            filterStrengthValue < 70 -> FilterStrength.Medium
            else -> FilterStrength.High
        }
        
        return CursorConfig(
            type = type,
            size = sharedPrefs.getInt("cursor_size", 48),
            color = sharedPrefs.getInt("cursor_color", Color.BLUE),
            speed = sharedPrefs.getInt("cursor_speed", 8),
            gazeClickDelay = gazeDelay,
            showCoordinates = sharedPrefs.getBoolean("show_coordinates", false),
            jitterFilterEnabled = sharedPrefs.getBoolean("jitter_filter_enabled", true),
            filterStrength = filterStrength,
            motionSensitivity = sharedPrefs.getFloat("motion_sensitivity", 0.7f)
        )
    }
    
    /**
     * Check if overlay permission is granted with comprehensive checks
     */
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Settings.canDrawOverlays(this)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking overlay permission", e)
                false
            }
        } else {
            true
        }
    }
    
    /**
     * Request overlay permission from user
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting overlay permission", e)
                // Fallback to general settings
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Error opening settings", fallbackException)
                }
            }
        }
    }
    
    /**
     * Enhanced permission validation with retry mechanism
     */
    private fun validateAndRequestPermissions(): Boolean {
        if (!hasOverlayPermission()) {
            Log.w(TAG, "Overlay permission not granted, requesting...")
            requestOverlayPermission()
            
            // Show notification about permission requirement
            showPermissionRequiredNotification()
            return false
        }
        return true
    }
    
    /**
     * Show notification about permission requirement
     */
    private fun showPermissionRequiredNotification() {
        val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = android.net.Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val settingsPendingIntent = PendingIntent.getActivity(
            this, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceCursor Permission Required")
            .setContentText("Tap to grant overlay permission for VoiceCursor")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(settingsPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoiceCursor Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Provides virtual cursor overlay for hands-free navigation"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create service notification with actions
     */
    private fun createNotification(): Notification {
        val stopIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(ACTION_STOP_SERVICE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val centerIntent = PendingIntent.getBroadcast(
            this, 1,
            Intent(ACTION_CENTER_CURSOR),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val menuIntent = PendingIntent.getBroadcast(
            this, 2,
            Intent(ACTION_SHOW_MENU),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceCursor Active")
            .setContentText("Cursor overlay is running")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_mylocation,
                "Center",
                centerIntent
            )
            .addAction(
                android.R.drawable.ic_menu_manage,
                "Menu",
                menuIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopIntent
            )
            .build()
    }
    
    /**
     * Initialize cursor overlay system
     */
    private fun initializeCursorOverlay() {
        try {
            // Create cursor view
            createView()
            
            // Initialize IMU integration
            imuIntegration = VoiceCursorIMUIntegration.createModern(this).apply {
                setOnPositionUpdate { _ ->
                    cursorView?.let { view ->
                        // Update cursor position
                        serviceScope.launch {
                            // Update view position via post to main thread
                            view.post {
                                // Position update handled internally by View
                            }
                        }
                    }
                }
                start()
            }
            
            Log.d(TAG, "Cursor overlay initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing cursor overlay", e)
            stopSelf()
        }
    }
    
    /**
     * Create cursor view and add to window manager
     */
    private fun createView() {
        cursorView = CursorView(this).apply {
            updateCursorStyle(cursorConfig)
            
            // Set up callbacks
            onMenuRequest = { position ->
                showMenuAtPosition(position)
            }
            
            onCursorMove = { position ->
                // Update accessibility service with position
                VoiceCursorAccessibilityService.getInstance()?.updateCursorPosition(position)
            }
            
            onGazeAutoClick = { position ->
                Log.d(TAG, "Gaze auto-click at (${position.x}, ${position.y})")
                // Handled by accessibility service
            }
        }
        
        // Add cursor view to window manager
        val layoutParams = createOverlayLayoutParams()
        windowManager?.addView(cursorView, layoutParams)
        
        // Configure modern fullscreen behavior for API 30+
        configureModernFullscreen(cursorView)
        
        // Start cursor tracking
        cursorView?.startTracking()
    }
    
    /**
     * Create layout parameters for overlay window with improved z-order and visibility
     */
    private fun createOverlayLayoutParams(): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Base flags common to all Android versions
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // Add fullscreen flag only for older Android versions (pre-API 30)
        val flags = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            baseFlags or WindowManager.LayoutParams.FLAG_FULLSCREEN
        } else {
            baseFlags
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            
            // Handle display cutout for full screen display
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            
            // Set highest priority for proper z-order - cursor should be on top
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use FLAG_LAYOUT_IN_SCREEN for proper overlay behavior
                // Note: flags is already set above, this is handled there
            }

            // For API 30+, we'll handle fullscreen via WindowInsetsController instead
            // This will be applied after the view is added to the window
        }
    }

    /**
     * Configure modern fullscreen behavior for API 30+ using WindowInsetsController
     * This replaces the deprecated FLAG_FULLSCREEN for newer Android versions
     */
    private fun configureModernFullscreen(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && view != null) {
            // Use the modern WindowCompat API to get WindowInsetsController
            view.post {
                // For overlay services, we need to handle this differently since we don't have a Window
                // We'll suppress the deprecation warning as this is the only way for overlay views
                @Suppress("DEPRECATION")
                androidx.core.view.ViewCompat.getWindowInsetsController(view)?.let { controller ->
                    // Hide the system bars (status bar and navigation bar)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    
                    // Set the behavior for when system bars are swiped
                    controller.systemBarsBehavior = 
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    
                    Log.d(TAG, "Modern fullscreen configured using WindowInsetsController for API ${Build.VERSION.SDK_INT}")
                }
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.d(TAG, "Using legacy FLAG_FULLSCREEN for API ${Build.VERSION.SDK_INT}")
        }
    }
    
    /**
     * Show cursor menu at specified position
     */
    private fun showMenuAtPosition(position: CursorOffset) {
        if (isMenuVisible) {
            hideMenu()
            return
        }
        
        isMenuVisible = true
        
        // Create compose view for menu
        menuComposeView = ComposeView(this).apply {
            setContent {
                MenuView(
                    isVisible = isMenuVisible,
                    position = position,
                    onAction = { action ->
                        handleMenuAction(action)
                        hideMenu()
                    },
                    onDismiss = {
                        hideMenu()
                    }
                )
            }
        }
        
        // Add menu to window manager
        val menuParams = createMenuLayoutParams()
        windowManager?.addView(menuComposeView, menuParams)
        
        // Auto-hide menu after 5 seconds
        serviceScope.launch {
            delay(5000)
            hideMenu()
        }
    }
    
    /**
     * Create layout parameters for menu overlay
     */
    private fun createMenuLayoutParams(): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
    
    /**
     * Hide cursor menu
     */
    private fun hideMenu() {
        isMenuVisible = false
        menuComposeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing menu view", e)
            }
        }
        menuComposeView = null
    }
    
    /**
     * Handle menu action selection
     */
    private fun handleMenuAction(action: CursorAction) {
        val accessibilityService = VoiceCursorAccessibilityService.getInstance()
        val currentPosition = cursorView?.getClickPosition() ?: CursorOffset(0f, 0f)
        
        when (action) {
            CursorAction.CENTER_CURSOR -> centerCursor()
            CursorAction.HIDE_CURSOR -> hideCursor()
            CursorAction.TOGGLE_COORDINATES -> toggleCoordinateDisplay()
            else -> {
                // Dispatch action to accessibility service
                accessibilityService?.executeAction(action, currentPosition)
            }
        }
    }
    
    /**
     * Handle intent actions
     */
    private fun handleIntentActions(intent: Intent?) {
        intent?.let {
            when (it.getStringExtra("action")) {
                "update_config" -> {
                    @Suppress("DEPRECATION")
                    it.getParcelableExtra<CursorConfig>("config")?.let { config ->
                        updateConfiguration(config)
                    }
                }
                "center_cursor" -> centerCursor()
                "show_cursor" -> showCursor()
                "hide_cursor" -> hideCursor()
                "enable_gaze" -> enableGaze()
                "disable_gaze" -> disableGaze()
                "toggle_coordinates" -> toggleCoordinateDisplay()
                "show_coordinates" -> showCoordinates()
                "hide_coordinates" -> hideCoordinates()
                "update_gaze_delay" -> {
                    val delay = it.getLongExtra("gaze_delay", 1500L)
                    updateGazeDelay(delay)
                }
                else -> {
                    Log.d(TAG, "Unknown action: ${it.getStringExtra("action")}")
                }
            }
        }
    }
    
    /**
     * Toggle cursor visibility
     */
    private fun toggleCursor() {
        if (isOverlayVisible) {
            hideCursor()
        } else {
            showCursor()
        }
    }
    
    /**
     * Show cursor overlay
     */
    private fun showCursor() {
        isOverlayVisible = true
        cursorView?.visibility = android.view.View.VISIBLE
    }
    
    /**
     * Hide cursor overlay
     */
    private fun hideCursor() {
        isOverlayVisible = false
        cursorView?.visibility = android.view.View.GONE
    }
    
    /**
     * Center cursor on screen
     */
    private fun centerCursor() {
        cursorView?.centerCursor()
    }
    
    /**
     * Show cursor menu
     */
    private fun showMenu() {
        val position = cursorView?.getClickPosition() ?: CursorOffset(500f, 500f)
        showMenuAtPosition(position)
    }
    
    /**
     * Update cursor configuration
     */
    private fun updateConfiguration(config: CursorConfig) {
        Log.d(TAG, "Updating configuration: $config")
        cursorConfig = config
        cursorView?.updateCursorStyle(config)
        imuIntegration?.setSensitivity(config.speed / 10.0f)
        Log.d(TAG, "Configuration updated successfully")
    }
    
    /**
     * Enable gaze tracking
     */
    private fun enableGaze() {
        Log.d(TAG, "Enabling gaze tracking")
        cursorView?.enableGaze()
    }
    
    /**
     * Disable gaze tracking
     */
    private fun disableGaze() {
        Log.d(TAG, "Disabling gaze tracking")
        cursorView?.disableGaze()
    }
    
    /**
     * Update gaze click delay
     */
    private fun updateGazeDelay(delayMs: Long) {
        Log.d(TAG, "Updating gaze delay to: ${delayMs}ms")
        // Update the gaze configuration in the cursor config
        val updatedConfig = cursorConfig.copy(gazeClickDelay = delayMs)
        updateConfiguration(updatedConfig)
    }
    
    /**
     * Toggle coordinate display
     */
    private fun toggleCoordinateDisplay() {
        Log.d(TAG, "Toggling coordinate display")
        val updatedConfig = cursorConfig.copy(showCoordinates = !cursorConfig.showCoordinates)
        updateConfiguration(updatedConfig)
        cursorView?.toggleCoordinateDisplay()
    }
    
    /**
     * Show coordinates
     */
    private fun showCoordinates() {
        Log.d(TAG, "Showing coordinates")
        val updatedConfig = cursorConfig.copy(showCoordinates = true)
        updateConfiguration(updatedConfig)
        cursorView?.setCoordinateDisplay(true)
    }
    
    /**
     * Hide coordinates
     */
    private fun hideCoordinates() {
        Log.d(TAG, "Hiding coordinates")
        val updatedConfig = cursorConfig.copy(showCoordinates = false)
        updateConfiguration(updatedConfig)
        cursorView?.setCoordinateDisplay(false)
    }
    
    /**
     * Remove overlay from window manager
     */
    private fun removeOverlay() {
        cursorView?.let { view ->
            view.stopTracking()
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing cursor view", e)
            }
        }
        cursorView = null
        
        hideMenu()
    }
}