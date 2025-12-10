/**
 * DiagnosticOverlayService.kt - Live diagnostic visual overlay with real-time status painting
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/debugging/DiagnosticOverlayService.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (Swarm Agent 4)
 * Created: 2025-12-08
 *
 * Enhanced overlay service that paints elements with colors based on their STATUS during exploration:
 * - ✅ GREEN: Clicked successfully
 * - 🚫 RED: Blocked (dangerous pattern)
 * - ⏭️ ORANGE: Skipped (optimization - has VUID)
 * - ⏳ BLUE: Pending (discovered but not processed)
 *
 * Updates in REAL-TIME as exploration progresses.
 */

package com.augmentalis.voiceoscore.learnapp.debugging

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.augmentalis.voiceoscore.learnapp.models.*
import com.augmentalis.voiceoscore.learnapp.tracking.ElementDiagnosticTracker
import java.util.concurrent.ConcurrentHashMap

/**
 * Diagnostic Overlay Service
 *
 * Paints elements with status-based colors during exploration.
 * Updates live as elements are clicked, blocked, or skipped.
 *
 * ## Color Coding (NEW - Status Based)
 * - ✅ Green: CLICKED successfully
 * - 🚫 Red: BLOCKED (dangerous)
 * - ⏭️ Orange: NOT_CLICKED (skipped - has VUID)
 * - ⏳ Blue: PENDING (discovered)
 *
 * ## Features
 * - Live updates as exploration progresses
 * - Tap element → show reason popup
 * - Animated current target element
 * - Status counts in legend
 * - Optional auto-hide (default: always on during learning)
 *
 * ## Usage
 *
 * ```kotlin
 * // Start overlay with diagnostic tracker
 * DiagnosticOverlayService.startOverlay(context, diagnosticTracker)
 *
 * // Element status updates automatically via tracker listener
 *
 * // Highlight current target
 * DiagnosticOverlayService.highlightElement(context, elementUuid)
 *
 * // Stop overlay
 * DiagnosticOverlayService.stopOverlay(context)
 * ```
 */
class DiagnosticOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: DiagnosticOverlayView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OVERLAY -> startOverlay()
            ACTION_STOP_OVERLAY -> stopOverlay()
            ACTION_HIGHLIGHT_ELEMENT -> {
                val uuid = intent.getStringExtra(EXTRA_ELEMENT_UUID)
                uuid?.let { highlightElement(it) }
            }
            ACTION_UPDATE_STATUS -> refreshOverlay()
        }
        return START_STICKY  // Keep running during exploration
    }

    /**
     * Start diagnostic overlay
     */
    private fun startOverlay() {
        if (overlayView != null) return  // Already showing

        val tracker = diagnosticTrackerHolder ?: return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,  // Allow touches through
            PixelFormat.TRANSLUCENT
        )

        overlayView = DiagnosticOverlayView(this, tracker)
        windowManager.addView(overlayView, params)

        // Listen for diagnostic updates
        tracker.setLiveUpdateListener { diagnostic ->
            handler.post { overlayView?.updateElementStatus(diagnostic) }
        }
    }

    /**
     * Stop diagnostic overlay
     */
    private fun stopOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // Already removed
            }
            overlayView = null
        }

        // Clear listener
        diagnosticTrackerHolder?.setLiveUpdateListener(null)
    }

    /**
     * Highlight specific element (current click target)
     */
    private fun highlightElement(elementUuid: String) {
        overlayView?.highlightElement(elementUuid)
    }

    /**
     * Refresh overlay (redraw)
     */
    private fun refreshOverlay() {
        overlayView?.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopOverlay()
    }

    companion object {
        private const val ACTION_START_OVERLAY = "com.augmentalis.voiceoscore.learnapp.START_DIAGNOSTIC_OVERLAY"
        private const val ACTION_STOP_OVERLAY = "com.augmentalis.voiceoscore.learnapp.STOP_DIAGNOSTIC_OVERLAY"
        private const val ACTION_HIGHLIGHT_ELEMENT = "com.augmentalis.voiceoscore.learnapp.HIGHLIGHT_ELEMENT"
        private const val ACTION_UPDATE_STATUS = "com.augmentalis.voiceoscore.learnapp.UPDATE_STATUS"
        private const val EXTRA_ELEMENT_UUID = "element_uuid"

        /**
         * Diagnostic tracker holder (avoids Parcelable complexity)
         */
        private var diagnosticTrackerHolder: ElementDiagnosticTracker? = null

        /**
         * Start overlay
         *
         * @param context Context
         * @param tracker Diagnostic tracker
         */
        fun startOverlay(context: Context, tracker: ElementDiagnosticTracker) {
            diagnosticTrackerHolder = tracker
            context.startService(Intent(context, DiagnosticOverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            })
        }

        /**
         * Stop overlay
         *
         * @param context Context
         */
        fun stopOverlay(context: Context) {
            context.startService(Intent(context, DiagnosticOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            })
            diagnosticTrackerHolder = null
        }

        /**
         * Highlight element (show animated border)
         *
         * @param context Context
         * @param elementUuid Element UUID to highlight
         */
        fun highlightElement(context: Context, elementUuid: String) {
            context.startService(Intent(context, DiagnosticOverlayService::class.java).apply {
                action = ACTION_HIGHLIGHT_ELEMENT
                putExtra(EXTRA_ELEMENT_UUID, elementUuid)
            })
        }
    }
}

/**
 * Diagnostic Overlay View
 *
 * Custom view that paints elements with status-based colors.
 * Updates live as diagnostics are recorded.
 */
@SuppressLint("ViewConstructor")
private class DiagnosticOverlayView(
    context: Context,
    private val tracker: ElementDiagnosticTracker
) : View(context) {

    // Element UUID → diagnostic cache (for fast lookup during draw)
    private val diagnosticCache = ConcurrentHashMap<String, ElementDiagnostic>()

    // Currently highlighted element (animated)
    private var highlightedUuid: String? = null
    private var highlightAnimationPhase = 0f

    // Paint objects
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 28f
        color = Color.WHITE
        isAntiAlias = true
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    private val iconPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 32f
        color = Color.WHITE
        isAntiAlias = true
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    // Animation handler
    private val handler = Handler(Looper.getMainLooper())
    private val animationRunnable = object : Runnable {
        override fun run() {
            highlightAnimationPhase = (highlightAnimationPhase + 0.1f) % 1f
            invalidate()
            handler.postDelayed(this, 50)  // 20 FPS
        }
    }

    init {
        // Start animation loop
        handler.post(animationRunnable)

        // Load initial diagnostics
        refreshCache()
    }

    /**
     * Update element status (called from live listener)
     */
    fun updateElementStatus(diagnostic: ElementDiagnostic) {
        diagnosticCache[diagnostic.elementUuid] = diagnostic
        invalidate()  // Redraw
    }

    /**
     * Highlight element (current click target)
     */
    fun highlightElement(elementUuid: String) {
        highlightedUuid = elementUuid
        highlightAnimationPhase = 0f
        invalidate()
    }

    /**
     * Refresh diagnostic cache from tracker
     */
    private fun refreshCache() {
        diagnosticCache.clear()
        tracker.getStatistics()  // Trigger cache population
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all elements
        diagnosticCache.values.forEach { diagnostic ->
            drawElement(canvas, diagnostic)
        }

        // Draw legend
        drawLegend(canvas)
    }

    /**
     * Draw single element with status-based styling
     */
    private fun drawElement(canvas: Canvas, diagnostic: ElementDiagnostic) {
        // Parse bounds (stored as JSON: "{left:x, top:y, right:x, bottom:y}")
        val bounds = parseBounds(diagnostic.elementBounds) ?: return

        val isHighlighted = diagnostic.elementUuid == highlightedUuid

        // Get color for status
        val color = diagnostic.reason.getOverlayColor()

        // Border style based on status
        when (diagnostic.status) {
            ElementStatus.CLICKED -> {
                borderPaint.strokeWidth = 6f
                borderPaint.pathEffect = null  // Solid
            }
            ElementStatus.BLOCKED -> {
                borderPaint.strokeWidth = 8f
                borderPaint.pathEffect = null  // Solid (thick)
            }
            ElementStatus.NOT_CLICKED -> {
                borderPaint.strokeWidth = 4f
                borderPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)  // Dashed
            }
            ElementStatus.PENDING -> {
                borderPaint.strokeWidth = 3f
                borderPaint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)  // Dotted
            }
        }

        // Animated border for highlighted element
        if (isHighlighted) {
            val pulse = 1f + 0.3f * Math.sin(highlightAnimationPhase * 2 * Math.PI).toFloat()
            borderPaint.strokeWidth *= pulse
        }

        // Draw fill (semi-transparent)
        val alpha = when (diagnostic.status) {
            ElementStatus.BLOCKED -> 76  // 30%
            ElementStatus.CLICKED -> 51  // 20%
            ElementStatus.NOT_CLICKED -> 38  // 15%
            ElementStatus.PENDING -> 13  // 5%
        }
        fillPaint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawRect(bounds, fillPaint)

        // Draw border
        borderPaint.color = color
        canvas.drawRect(bounds, borderPaint)

        // Draw status icon
        val icon = diagnostic.reason.getIcon()
        val iconX = bounds.left + 8f
        val iconY = bounds.top + 36f
        canvas.drawText(icon, iconX, iconY, iconPaint)

        // Draw element label (if space available)
        if (bounds.height() > 60) {
            val label = diagnostic.elementText.take(20)
            if (label.isNotBlank()) {
                textPaint.textSize = 20f
                canvas.drawText(label, iconX + 40, iconY, textPaint)
            }
        }
    }

    /**
     * Draw legend showing status counts
     */
    private fun drawLegend(canvas: Canvas) {
        val statusCounts = tracker.getStatusCounts()
        val legendX = 20f
        val legendY = height - 350f
        val legendWidth = 280f
        val legendHeight = 300f

        // Semi-transparent background
        fillPaint.color = Color.argb(220, 33, 33, 33)
        canvas.drawRect(legendX, legendY, legendX + legendWidth, legendY + legendHeight, fillPaint)

        // Title
        textPaint.textSize = 24f
        textPaint.color = Color.WHITE
        canvas.drawText("ELEMENT STATUS", legendX + 15, legendY + 35, textPaint)

        // Status items
        val items = listOf(
            Triple(ElementStatus.CLICKED, ElementStatusReason.CLICKED_SUCCESSFULLY, "Clicked"),
            Triple(ElementStatus.BLOCKED, ElementStatusReason.BLOCKED_CALL_ACTION, "Blocked"),
            Triple(ElementStatus.NOT_CLICKED, ElementStatusReason.CLICK_CAP_REACHED, "UUID'd/Skip"),
            Triple(ElementStatus.PENDING, ElementStatusReason.NOT_YET_REACHED, "Pending")
        )

        items.forEachIndexed { index, (status, reason, label) ->
            val count = statusCounts[status] ?: 0
            val y = legendY + 75 + (index * 50f)

            // Color box
            val color = reason.getOverlayColor()
            fillPaint.color = color
            canvas.drawRect(legendX + 20, y - 20, legendX + 45, y + 5, fillPaint)

            // Icon
            val icon = reason.getIcon()
            iconPaint.textSize = 28f
            canvas.drawText(icon, legendX + 55, y, iconPaint)

            // Label + count
            textPaint.textSize = 20f
            textPaint.color = Color.WHITE
            canvas.drawText("$label ($count)", legendX + 90, y, textPaint)
        }

        // Footer
        textPaint.textSize = 16f
        textPaint.color = Color.argb(200, 255, 255, 255)
        canvas.drawText("Tap element for reason", legendX + 15, legendY + legendHeight - 15, textPaint)
    }

    /**
     * Parse bounds string to Rect
     *
     * Expected format: "{left:x, top:y, right:x, bottom:y}" or "Rect(x, y - x, y)"
     */
    private fun parseBounds(boundsStr: String): RectF? {
        if (boundsStr.isBlank()) return null

        return try {
            // Try JSON format first
            if (boundsStr.contains("left")) {
                val left = boundsStr.substringAfter("left").substringAfter(":").substringBefore(",").trim().toFloat()
                val top = boundsStr.substringAfter("top").substringAfter(":").substringBefore(",").trim().toFloat()
                val right = boundsStr.substringAfter("right").substringAfter(":").substringBefore(",").trim().toFloat()
                val bottom = boundsStr.substringAfter("bottom").substringAfter(":").substringBefore("}").trim().toFloat()
                RectF(left, top, right, bottom)
            } else {
                // Try Rect(x, y - x, y) format
                val parts = boundsStr.substringAfter("(").substringBefore(")").split(",", "-")
                if (parts.size == 4) {
                    RectF(
                        parts[0].trim().toFloat(),
                        parts[1].trim().toFloat(),
                        parts[2].trim().toFloat(),
                        parts[3].trim().toFloat()
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animationRunnable)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Find tapped element
            val x = event.x
            val y = event.y

            diagnosticCache.values.forEach { diagnostic ->
                val bounds = parseBounds(diagnostic.elementBounds)
                if (bounds != null && bounds.contains(x, y)) {
                    // TODO: Show reason popup
                    // For now, just highlight it
                    highlightElement(diagnostic.elementUuid)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
