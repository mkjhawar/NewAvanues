/**
 * CursorOverlayService.kt - Cursor overlay foreground service
 *
 * Creates an Android system overlay window and renders a cursor using
 * VoiceCursor module's CursorController for position tracking and
 * GazeClickManager for dwell-click support.
 *
 * Input source: Other services (e.g., AccessibilityService, Gaze module)
 * call updateCursorInput() to feed position data to the controller.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voicecursor.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.augmentalis.avanueui.theme.AvanueModuleAccents
import com.augmentalis.voicecursor.core.ClickDispatcher
import com.augmentalis.voicecursor.core.CursorAction
import com.augmentalis.voicecursor.core.CursorConfig
import com.augmentalis.voicecursor.core.CursorController
import com.augmentalis.voicecursor.core.CursorInput
import com.augmentalis.voicecursor.core.CursorState
import com.augmentalis.voicecursor.core.FilterStrength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "CursorOverlayService"
private const val CHANNEL_ID = "ava_cursor_service"
private const val NOTIFICATION_ID = 1002

class CursorOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var cursorController: CursorController? = null
    private var overlayView: CursorOverlayView? = null
    private var windowManager: WindowManager? = null
    private var clickDispatcher: ClickDispatcher? = null
    private var launchIntent: PendingIntent? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Cursor Overlay Service starting")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        instance = this
        initializeCursorController()
        createOverlayWindow()
        observeCursorState()

        return START_STICKY
    }

    private fun initializeCursorController() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        val config = buildConfigFromAccent()

        cursorController = CursorController(config).apply {
            initialize(screenWidth, screenHeight)

            onClick { position ->
                Log.d(TAG, "Cursor click at (${position.x}, ${position.y})")
                performClick(position.x.toInt(), position.y.toInt())
            }

            onDwellStart {
                overlayView?.setDwelling(true)
            }

            onDwellEnd {
                overlayView?.setDwelling(false)
            }
        }

        Log.i(TAG, "CursorController initialized (${screenWidth}x${screenHeight})")
    }

    /**
     * Build a CursorConfig with colors from AvanueModuleAccents.
     * Called at init and whenever config is rebuilt from settings.
     */
    private fun buildConfigFromAccent(base: CursorConfig = CursorConfig()): CursorConfig {
        val accentArgb = AvanueModuleAccents.getAccentArgb("voicecursor").toLong() and 0xFFFFFFFFL
        val onAccentArgb = AvanueModuleAccents.getOnAccentArgb("voicecursor").toLong() and 0xFFFFFFFFL
        return base.copy(
            color = accentArgb,
            borderColor = onAccentArgb,
            dwellRingColor = accentArgb
        )
    }

    private fun createOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = CursorOverlayView(this).also { view ->
            cursorController?.let { controller ->
                view.applyConfig(controller.getConfig())
            }
        }
        windowManager?.addView(overlayView, layoutParams)

        Log.i(TAG, "Overlay window created")
    }

    private fun observeCursorState() {
        serviceScope.launch {
            cursorController?.state?.collect { state ->
                overlayView?.updateCursorState(state)
            }
        }
    }

    /**
     * Feed cursor input from external sources (Gaze module, accessibility events, etc.).
     * Call this from the accessibility service or gaze tracker.
     */
    fun updateCursorInput(input: CursorInput) {
        val action = cursorController?.update(
            input = input,
            currentTimeMs = System.currentTimeMillis(),
            isOverInteractive = true
        )

        when (action) {
            is CursorAction.DwellClick -> {
                val state = cursorController?.state?.value ?: return
                performClick(state.position.x.toInt(), state.position.y.toInt())
            }
            is CursorAction.Click -> {
                val state = cursorController?.state?.value ?: return
                performClick(state.position.x.toInt(), state.position.y.toInt())
            }
            else -> { /* No action needed */ }
        }
    }

    /**
     * Update cursor configuration from settings.
     * Propagates color/size config to both the controller and overlay view.
     */
    fun updateConfig(config: CursorConfig) {
        cursorController?.updateConfig(config)
        overlayView?.applyConfig(config)
    }

    /**
     * Set the ClickDispatcher to handle click events.
     * This should be called by the app layer to inject the AccessibilityService-based dispatcher.
     */
    fun setClickDispatcher(dispatcher: ClickDispatcher) {
        clickDispatcher = dispatcher
    }

    /**
     * Set the PendingIntent to launch when the notification is tapped.
     * This should be called by the app layer to inject the appropriate MainActivity intent.
     */
    fun setLaunchIntent(intent: PendingIntent) {
        launchIntent = intent
    }

    /**
     * Perform a click at the current cursor position.
     * Used by voice command handlers to dispatch "cursor click" / "click here".
     * @return true if click was dispatched, false if cursor not visible or dispatcher not set
     */
    fun performClickAtCurrentPosition(): Boolean {
        val state = cursorController?.state?.value ?: return false
        if (!state.isVisible) return false
        performClick(state.position.x.toInt(), state.position.y.toInt())
        return true
    }

    private fun performClick(x: Int, y: Int) {
        val dispatcher = clickDispatcher
        if (dispatcher != null) {
            dispatcher.dispatchClick(x, y)
        } else {
            Log.w(TAG, "ClickDispatcher not set - click dispatch unavailable")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        super.onDestroy()

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove overlay view", e)
            }
        }
        overlayView = null

        cursorController?.dispose()
        cursorController = null

        serviceScope.cancel()

        Log.i(TAG, "Cursor Overlay Service destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cursor Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = launchIntent ?: PendingIntent.getActivity(
            this,
            0,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cursor Active")
            .setContentText("Gaze control enabled")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        @Volatile
        private var instance: CursorOverlayService? = null

        fun getInstance(): CursorOverlayService? = instance
    }
}

/**
 * Custom View that renders the cursor dot and dwell progress ring.
 * Uses Canvas for efficient drawing — no Compose overhead for this always-on overlay.
 *
 * All visual properties (colors, sizes, strokes) are driven by CursorConfig
 * via [applyConfig]. No hardcoded colors — everything comes from the config,
 * which in turn reads from AvanueModuleAccents for theme integration.
 */
internal class CursorOverlayView(context: Context) : View(context) {

    private var cursorX = 0f
    private var cursorY = 0f
    private var isVisible = false
    private var isDwelling = false
    private var dwellProgress = 0f
    private var cursorRadius = 12f

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cursorBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val dwellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        // Apply defaults matching CursorConfig defaults
        applyConfig(CursorConfig())
    }

    /**
     * Apply all visual properties from the cursor config.
     * Safe to call from any thread (posts invalidate).
     */
    fun applyConfig(config: CursorConfig) {
        cursorRadius = config.cursorRadius

        cursorPaint.color = config.color.toInt()
        cursorPaint.alpha = config.cursorAlpha

        cursorBorderPaint.color = config.borderColor.toInt()
        cursorBorderPaint.strokeWidth = config.borderStrokeWidth

        dwellPaint.color = config.dwellRingColor.toInt()
        dwellPaint.strokeWidth = config.dwellRingStrokeWidth

        postInvalidate()
    }

    fun updateCursorState(state: CursorState) {
        cursorX = state.position.x
        cursorY = state.position.y
        isVisible = state.isVisible
        dwellProgress = state.dwellProgress
        isDwelling = state.isDwellInProgress
        postInvalidate()
    }

    fun setDwelling(dwelling: Boolean) {
        isDwelling = dwelling
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isVisible) return

        // Draw dwell progress ring
        if (isDwelling && dwellProgress > 0f) {
            val sweepAngle = dwellProgress * 360f
            val dwellRadius = cursorRadius + 8f
            canvas.drawArc(
                cursorX - dwellRadius,
                cursorY - dwellRadius,
                cursorX + dwellRadius,
                cursorY + dwellRadius,
                -90f,
                sweepAngle,
                false,
                dwellPaint
            )
        }

        // Draw cursor dot
        canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorPaint)
        canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorBorderPaint)
    }
}
