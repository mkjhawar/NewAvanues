/**
 * AccessibilityOverlayService.kt - Visual debugging overlay service
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/debugging/AccessibilityOverlayService.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-29
 *
 * Service that draws colored boxes around elements to show their classification during exploration.
 * Helps developers visually debug element detection and classification.
 */

package com.augmentalis.voiceoscore.learnapp.debugging

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.WindowManager
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings

/**
 * Accessibility Overlay Service
 *
 * Draws visual overlay showing element classifications during exploration.
 * Helps developers see what LearnApp is detecting and how it's classified.
 *
 * ## Color Coding
 * - Green: Safe clickable elements
 * - Red: Dangerous elements (won't be clicked)
 * - Blue: Login fields
 * - Yellow: Disabled elements
 * - Gray: Non-clickable elements
 *
 * ## Usage
 *
 * ```kotlin
 * // Show overlay with elements
 * AccessibilityOverlayService.showOverlay(context, elementsList)
 *
 * // Hide overlay
 * AccessibilityOverlayService.hideOverlay(context)
 * ```
 *
 * ## Implementation Notes
 * - Uses static holder for element list (avoids Parcelable complexity)
 * - Auto-hides after 5 seconds
 * - Requires TYPE_ACCESSIBILITY_OVERLAY permission (granted to accessibility services)
 * - Non-focusable and non-touchable (doesn't interfere with app interaction)
 *
 * @since 1.0.0
 */
class AccessibilityOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val developerSettings by lazy { LearnAppDeveloperSettings(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_SHOW_OVERLAY -> {
                showElementOverlay()
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Show element overlay
     *
     * Displays colored boxes around elements from the static holder.
     */
    private fun showElementOverlay() {
        // Remove existing overlay
        hideOverlay()

        val elements = elementHolder ?: return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        overlayView = OverlayView(this, elements)
        windowManager.addView(overlayView, params)

        // Auto-hide after configurable delay (default: 5 seconds)
        // FIX (2025-12-05): Wired to developerSettings
        val autoHideDelay = developerSettings.getOverlayAutoHideDelayMs()
        handler.postDelayed({
            hideOverlay()
        }, autoHideDelay)
    }

    /**
     * Hide overlay
     *
     * Removes the overlay view from screen.
     */
    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // Already removed
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.augmentalis.voiceoscore.learnapp.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.augmentalis.voiceoscore.learnapp.HIDE_OVERLAY"

        /**
         * Static holder for element list (avoids Parcelable complexity)
         * Set this before calling showOverlay()
         */
        private var elementHolder: List<ElementInfo>? = null

        /**
         * Show overlay with element visualization
         *
         * @param context Context
         * @param elements List of elements to visualize
         */
        fun showOverlay(context: Context, elements: List<ElementInfo>) {
            elementHolder = elements
            val intent = Intent(context, AccessibilityOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
            context.startService(intent)
        }

        /**
         * Hide overlay
         *
         * @param context Context
         */
        fun hideOverlay(context: Context) {
            val intent = Intent(context, AccessibilityOverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
            elementHolder = null
        }
    }
}

/**
 * Overlay View - draws colored boxes around elements
 *
 * Custom view that renders element boundaries with color-coded classifications
 * and a legend showing what each color means.
 */
private class OverlayView(
    context: Context,
    private val elements: List<ElementInfo>
) : View(context) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 32f
        color = Color.WHITE
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw element boxes
        elements.forEachIndexed { index, element ->
            drawElementBox(canvas, index, element)
        }

        // Draw legend
        drawLegend(canvas)
    }

    /**
     * Draw colored box around element
     *
     * @param canvas Canvas to draw on
     * @param index Element index
     * @param element Element info
     */
    private fun drawElementBox(canvas: Canvas, index: Int, element: ElementInfo) {
        // Choose color based on classification
        paint.color = getColorForClassification(element.classification)

        // Draw rectangle around element
        val bounds = element.bounds
        canvas.drawRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            paint
        )

        // Draw element number
        val label = "#$index"
        canvas.drawText(
            label,
            bounds.left.toFloat() + 8,
            bounds.top.toFloat() + 40,
            textPaint
        )

        // Draw element type (if space available)
        if (bounds.height() > 80) {
            val type = element.className.substringAfterLast('.')
            textPaint.textSize = 24f
            canvas.drawText(
                type,
                bounds.left.toFloat() + 8,
                bounds.top.toFloat() + 70,
                textPaint
            )
            textPaint.textSize = 32f
        }
    }

    /**
     * Get color for classification
     *
     * @param classification Classification string
     * @return Color int
     */
    private fun getColorForClassification(classification: String?): Int {
        return when (classification) {
            "safe_clickable" -> Color.GREEN
            "dangerous" -> Color.RED
            "login" -> Color.BLUE
            "disabled" -> Color.YELLOW
            "non_clickable" -> Color.GRAY
            else -> Color.LTGRAY
        }
    }

    /**
     * Draw legend showing color meanings
     *
     * @param canvas Canvas to draw on
     */
    private fun drawLegend(canvas: Canvas) {
        val legendX = 20f
        val legendY = height - 300f
        val legendWidth = 300f
        val legendHeight = 250f

        // Semi-transparent background
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(200, 0, 0, 0)
        canvas.drawRect(legendX, legendY, legendX + legendWidth, legendY + legendHeight, paint)

        // Legend items
        paint.style = Paint.Style.STROKE
        textPaint.textSize = 24f

        val items = listOf(
            Pair(Color.GREEN, "Safe Clickable"),
            Pair(Color.RED, "Dangerous"),
            Pair(Color.BLUE, "Login Field"),
            Pair(Color.YELLOW, "Disabled"),
            Pair(Color.GRAY, "Non-Clickable")
        )

        items.forEachIndexed { index, (color, label) ->
            drawLegendItem(canvas, legendX, legendY, index, color, label)
        }
    }

    /**
     * Draw single legend item
     *
     * @param canvas Canvas to draw on
     * @param legendX Legend X position
     * @param legendY Legend Y position
     * @param index Item index
     * @param color Item color
     * @param label Item label
     */
    private fun drawLegendItem(
        canvas: Canvas,
        legendX: Float,
        legendY: Float,
        index: Int,
        color: Int,
        label: String
    ) {
        val y = legendY + 30 + (index * 45f)

        // Color box
        paint.color = color
        canvas.drawRect(legendX + 20, y - 20, legendX + 40, y, paint)

        // Label
        textPaint.color = Color.WHITE
        canvas.drawText(label, legendX + 50, y, textPaint)
    }
}
