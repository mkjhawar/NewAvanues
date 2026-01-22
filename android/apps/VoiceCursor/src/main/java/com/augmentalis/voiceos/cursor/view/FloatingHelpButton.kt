/**
 * FloatingHelpButton.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/FloatingHelpButton.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Floating help button overlay for quick access to help
 * Module: VoiceCursor
 */

package com.augmentalis.voiceos.cursor.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.augmentalis.voiceos.cursor.help.VoiceCursorHelpMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Floating help button that stays on screen for quick access
 */
@SuppressLint("ViewConstructor")
class FloatingHelpButton(
    private val context: Context,
    private val windowManager: WindowManager
) : FrameLayout(context) {
    
    companion object {
        private const val BUTTON_SIZE_DP = 56
        private const val MARGIN_DP = 16
        private const val LONG_PRESS_DURATION = 500L
        private const val DRAG_THRESHOLD = 20
    }
    
    private val helpMenu = VoiceCursorHelpMenu(context)
    private var isShowing = false
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var longPressJob: Job? = null
    
    // Proper coroutine scope tied to the view lifecycle
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val layoutParams: WindowManager.LayoutParams
    
    init {
        // Set up the button view
        val size = (BUTTON_SIZE_DP * context.resources.displayMetrics.density).toInt()
        val margin = (MARGIN_DP * context.resources.displayMetrics.density).toInt()
        
        layoutParams = WindowManager.LayoutParams().apply {
            width = size
            height = size
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.END
            x = margin
            y = margin * 4 // Position below status bar
        }
        
        // Create the button appearance
        setBackgroundResource(android.R.drawable.ic_menu_help)
        elevation = 8f
        
        // Add ripple effect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
        }
        
        // Set up touch handling
        setupTouchHandling()
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    
                    // Start long press detection for dragging
                    longPressJob = coroutineScope.launch {
                        delay(LONG_PRESS_DURATION)
                        isDragging = true
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    if (isDragging) {
                        // Move the button
                        layoutParams.x = (initialX - deltaX).toInt()
                        layoutParams.y = (initialY + deltaY).toInt()
                        windowManager.updateViewLayout(this, layoutParams)
                    } else if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                        // Cancel long press if moved too much
                        longPressJob?.cancel()
                    }
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    longPressJob?.cancel()
                    if (!isDragging) {
                        // Regular click - show help menu
                        performClick()
                    }
                    isDragging = false
                    true
                }
                
                MotionEvent.ACTION_CANCEL -> {
                    longPressJob?.cancel()
                    isDragging = false
                    true
                }
                
                else -> false
            }
        }
        
        setOnClickListener {
            showHelpMenu()
        }
    }
    
    /**
     * Show the floating help button
     */
    fun show() {
        if (!isShowing) {
            try {
                windowManager.addView(this, layoutParams)
                isShowing = true
                
                // Animate entrance
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Hide the floating help button
     */
    fun hide() {
        if (isShowing) {
            animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    try {
                        windowManager.removeView(this)
                        isShowing = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .start()
        }
    }
    
    /**
     * Show the help menu
     */
    private fun showHelpMenu() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        helpMenu.showHelpMenu()
    }
    
    /**
     * Toggle button visibility
     */
    fun toggle() {
        if (isShowing) hide() else show()
    }
    
    /**
     * Check if button is showing
     */
    fun isButtonShowing(): Boolean = isShowing
    
    /**
     * Clean up resources
     */
    fun destroy() {
        hide()
        longPressJob?.cancel()
        coroutineScope.cancel() // Clean up the coroutine scope
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw help icon
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }
        
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = width * 0.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        // Draw background circle
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            width / 2f - 4,
            paint
        )
        
        // Draw question mark
        canvas.drawText(
            "?",
            width / 2f,
            height / 2f + textPaint.textSize / 3,
            textPaint
        )
    }
}

/**
 * Manager for the floating help button
 */
class FloatingHelpButtonManager(private val context: Context) {
    
    private var floatingButton: FloatingHelpButton? = null
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    /**
     * Show the floating help button
     */
    fun showFloatingButton() {
        if (floatingButton == null) {
            floatingButton = FloatingHelpButton(context, windowManager)
        }
        floatingButton?.show()
    }
    
    /**
     * Hide the floating help button
     */
    fun hideFloatingButton() {
        floatingButton?.hide()
    }
    
    /**
     * Toggle floating button visibility
     */
    fun toggleFloatingButton() {
        floatingButton?.toggle() ?: showFloatingButton()
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        floatingButton?.destroy()
        floatingButton = null
    }
}