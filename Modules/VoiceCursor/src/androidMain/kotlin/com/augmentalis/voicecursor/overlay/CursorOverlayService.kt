/**
 * CursorOverlayService.kt - Cursor overlay foreground service
 *
 * Creates a SMALL, positioned Android system overlay window that tracks the
 * cursor position. Unlike a full-screen overlay, this tiny window only covers
 * the cursor dot area, preventing touch blocking on Android 12+ where
 * TYPE_APPLICATION_OVERLAY with MATCH_PARENT blocks "untrusted touches".
 *
 * Architecture:
 * - KMP CursorController manages position/state (shared with iOS/Desktop)
 * - KMP CursorOverlaySpec computes overlay sizing (shared logic)
 * - This service handles Android-specific WindowManager overlay
 * - CursorOverlayView draws the cursor dot + dwell ring at view center
 * - WindowManager.updateViewLayout() repositions the overlay on each frame
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
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.augmentalis.avanueui.theme.AvanueModuleAccents
import com.augmentalis.voicecursor.core.ClickDispatcher
import com.augmentalis.voicecursor.core.CursorAction
import com.augmentalis.voicecursor.core.CursorConfig
import com.augmentalis.voicecursor.core.CursorController
import com.augmentalis.voicecursor.core.CursorInput
import com.augmentalis.voicecursor.core.CursorOverlaySpec
import com.augmentalis.voicecursor.core.CursorState
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
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var overlaySpec: CursorOverlaySpec? = null
    private var clickDispatcher: ClickDispatcher? = null
    private var launchIntent: PendingIntent? = null
    private var displayDensity: Float = 1f

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
        displayDensity = resources.displayMetrics.density
        initializeCursorController()
        createOverlayWindow()
        observeCursorState()

        Log.i(TAG, "Cursor Overlay Service started (density=$displayDensity)")
        return START_STICKY
    }

    private fun initializeCursorController() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        val config = buildConfigFromAccent()

        cursorController = CursorController(config).apply {
            // initialize() auto-centers cursor on screen
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

    /**
     * Create a small, positioned overlay window that covers only the cursor area.
     *
     * Uses KMP CursorOverlaySpec for sizing (shared logic), then creates an
     * Android-specific WindowManager overlay. The overlay is repositioned
     * via updateViewLayout() as the cursor moves.
     *
     * Key flags:
     * - FLAG_NOT_FOCUSABLE: Doesn't steal keyboard focus
     * - FLAG_NOT_TOUCHABLE: Touches pass through to apps below
     * - FLAG_LAYOUT_NO_LIMITS: Allows positioning near screen edges
     */
    private fun createOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val config = cursorController?.getConfig() ?: CursorConfig()
        val spec = CursorOverlaySpec.fromConfig(config, displayDensity)
        overlaySpec = spec

        // Initial position: cursor center (set by CursorController.initialize())
        val state = cursorController?.state?.value ?: CursorState()
        val origin = spec.overlayOrigin(state.position.x, state.position.y)

        val params = WindowManager.LayoutParams(
            spec.sizePx,
            spec.sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = origin.x.toInt()
            y = origin.y.toInt()
        }
        overlayLayoutParams = params

        overlayView = CursorOverlayView(this, displayDensity).also { view ->
            view.applyConfig(config, displayDensity)
        }
        windowManager?.addView(overlayView, params)

        Log.i(TAG, "Overlay window created (${spec.sizePx}x${spec.sizePx}px)")
    }

    /**
     * Observe cursor state and reposition the overlay window on each update.
     */
    private fun observeCursorState() {
        serviceScope.launch {
            cursorController?.state?.collect { state ->
                overlayView?.updateCursorState(state)
                repositionOverlay(state)
            }
        }
    }

    /**
     * Reposition the small overlay window to track the cursor position.
     * Called on every state change from the CursorController.
     */
    private fun repositionOverlay(state: CursorState) {
        val params = overlayLayoutParams ?: return
        val spec = overlaySpec ?: return
        val view = overlayView ?: return

        if (!state.isVisible) {
            if (view.visibility != View.GONE) {
                view.visibility = View.GONE
            }
            return
        }

        if (view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
        }

        val origin = spec.overlayOrigin(state.position.x, state.position.y)
        params.x = origin.x.toInt()
        params.y = origin.y.toInt()

        try {
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reposition overlay", e)
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
     * Propagates color/size config to both the controller and overlay view,
     * and recalculates overlay sizing.
     */
    fun updateConfig(config: CursorConfig) {
        cursorController?.updateConfig(config)
        overlayView?.applyConfig(config, displayDensity)
        // Recalculate overlay size for new config
        val newSpec = CursorOverlaySpec.fromConfig(config, displayDensity)
        overlaySpec = newSpec
        overlayLayoutParams?.let { params ->
            params.width = newSpec.sizePx
            params.height = newSpec.sizePx
        }
    }

    /**
     * Set the ClickDispatcher to handle click events.
     * This should be called by the app layer to inject the AccessibilityService-based dispatcher.
     */
    fun setClickDispatcher(dispatcher: ClickDispatcher) {
        clickDispatcher = dispatcher
        Log.d(TAG, "ClickDispatcher set")
    }

    /**
     * Set the PendingIntent to launch when the notification is tapped.
     * This should be called by the app layer to inject the appropriate MainActivity intent.
     */
    fun setLaunchIntent(intent: PendingIntent) {
        launchIntent = intent
    }

    /**
     * Get the CursorController for this service.
     * Used by external components (e.g., CursorActions) to wire voice commands
     * to the same controller the overlay observes.
     */
    fun getCursorController(): CursorController? = cursorController

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
        overlayLayoutParams = null
        overlaySpec = null

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
 * Draws at the VIEW CENTER, not at screen coordinates. The parent service
 * repositions the overlay window to track the cursor, so this view always
 * renders at its center point.
 *
 * All visual properties (colors, sizes, strokes) are driven by CursorConfig
 * via [applyConfig]. No hardcoded colors — everything comes from the config,
 * which in turn reads from AvanueModuleAccents for theme integration.
 */
internal class CursorOverlayView(
    context: Context,
    private val density: Float = 1f
) : View(context) {

    private var isVisible = false
    private var isDwelling = false
    private var dwellProgress = 0f
    private var cursorRadius = 12f
    private var dwellRingGap = 8f

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

    // Outer glow for visibility on any background
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x66000000.toInt() // Semi-transparent black shadow
    }

    init {
        applyConfig(CursorConfig(), density)
    }

    /**
     * Apply all visual properties from the cursor config, density-scaled.
     * Safe to call from any thread (posts invalidate).
     */
    fun applyConfig(config: CursorConfig, displayDensity: Float) {
        cursorRadius = config.cursorRadius * displayDensity
        dwellRingGap = 8f * displayDensity

        cursorPaint.color = config.color.toInt()
        cursorPaint.alpha = config.cursorAlpha

        cursorBorderPaint.color = config.borderColor.toInt()
        cursorBorderPaint.strokeWidth = config.borderStrokeWidth * displayDensity

        dwellPaint.color = config.dwellRingColor.toInt()
        dwellPaint.strokeWidth = config.dwellRingStrokeWidth * displayDensity

        glowPaint.strokeWidth = 2f * displayDensity

        postInvalidate()
    }

    fun updateCursorState(state: CursorState) {
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

        // Draw at view center — the overlay window is repositioned to track cursor
        val cx = width / 2f
        val cy = height / 2f

        // Draw dwell progress ring
        if (isDwelling && dwellProgress > 0f) {
            val sweepAngle = dwellProgress * 360f
            val dwellRadius = cursorRadius + dwellRingGap
            canvas.drawArc(
                cx - dwellRadius,
                cy - dwellRadius,
                cx + dwellRadius,
                cy + dwellRadius,
                -90f,
                sweepAngle,
                false,
                dwellPaint
            )
        }

        // Draw outer glow for visibility on any background
        canvas.drawCircle(cx, cy, cursorRadius + cursorBorderPaint.strokeWidth, glowPaint)

        // Draw cursor dot
        canvas.drawCircle(cx, cy, cursorRadius, cursorPaint)
        canvas.drawCircle(cx, cy, cursorRadius, cursorBorderPaint)
    }
}
