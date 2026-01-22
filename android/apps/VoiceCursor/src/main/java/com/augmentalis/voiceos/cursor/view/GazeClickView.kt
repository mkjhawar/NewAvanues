/**
 * GazeClickView.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/GazeClickView.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 2.0.0
 * 
 * Purpose: Visual feedback view for gaze click animations with proper overlay handling
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0: Ported from Legacy VoiceOS with modern architecture
 * - v2.0.0: Fixed cursor visibility and z-order issues, added proper overlay permissions
 */

package com.augmentalis.voiceos.cursor.view

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import com.augmentalis.voiceos.cursor.R
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.helper.CursorHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Visual feedback view for gaze click animations
 * Shows expanding circle animation when gaze click is triggered
 * Handles proper overlay display with z-order and visibility fixes
 */
@SuppressLint("ViewConstructor", "AppCompatCustomView")
class GazeClickView(
    context: Context, 
    private val offset: CursorOffset
) : ImageView(context) {
    
    companion object {
        private const val TAG = "GazeClickView"
        private const val ANIMATION_DELAY_MS = 220L
        private const val APP_VOICE_NOTIFICATION_CONTENT_DESCRIPTION = "Gaze Click Animation"
        
        // Size multipliers for animation stages
        private const val SIZE_MULTIPLIER_SMALL = 0.2f
        private const val SIZE_MULTIPLIER_MEDIUM = 0.4f
        private const val SIZE_MULTIPLIER_LARGE = 0.6f
    }
    
    // Animation bitmaps with proper scaling
    private val baseSize = resources.getDimensionPixelSize(R.dimen.cursor_size_medium)
    private val bitmapSize1 = (baseSize * SIZE_MULTIPLIER_LARGE).roundToInt()
    private val bitmapSize2 = (baseSize * SIZE_MULTIPLIER_MEDIUM).roundToInt()
    private val bitmapSize3 = (baseSize * SIZE_MULTIPLIER_SMALL).roundToInt()
    
    // Create bitmaps with proper gaze circle drawables
    private val bitmap1 = createGazeBitmap(R.drawable.ic_gaze_circle_small, bitmapSize3)
    private val bitmap2 = createGazeBitmap(R.drawable.ic_gaze_circle_medium, bitmapSize2)
    private val bitmap3 = createGazeBitmap(R.drawable.ic_gaze_circle_big, bitmapSize1)
    
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
    
    // Enhanced layout parameters with proper overlay handling
    private val layoutParams = WindowManager.LayoutParams().apply {
        format = PixelFormat.TRANSLUCENT
        
        // Use TYPE_ACCESSIBILITY_OVERLAY for proper z-order
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        
        // Enhanced flags for better visibility and overlay behavior
        flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        
        // Calculate proper size with padding
        val size = resources.getDimensionPixelSize(R.dimen.cursor_size_large)
        width = size
        height = size
        
        // Center the view at the cursor position
        x = offset.x.toInt() - (size / 2)
        y = offset.y.toInt() - (size / 2)
        
        scaleType = ScaleType.CENTER_INSIDE
        gravity = Gravity.START or Gravity.TOP
        contentDescription = APP_VOICE_NOTIFICATION_CONTENT_DESCRIPTION
        
        // Handle display cutout for full screen display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // Set highest priority for proper z-order
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Ensure this appears above other system UI elements
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
    }
    
    init {
        // Start the gaze click animation sequence
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Stage 1: Large circle (fade in)
                setImageBitmap(bitmap3)
                delay(ANIMATION_DELAY_MS)
                
                // Stage 2: Medium circle
                setImageBitmap(bitmap2)
                delay(ANIMATION_DELAY_MS)
                
                // Stage 3: Small circle (fade out effect)
                setImageBitmap(bitmap1)
                delay(ANIMATION_DELAY_MS)
                
                // Auto-hide after animation completes
                hide()
            } catch (e: Exception) {
                // Handle any animation errors gracefully
                hide()
            }
        }
    }
    
    /**
     * Create gaze animation bitmap with fallback handling
     */
    private fun createGazeBitmap(drawableRes: Int, size: Int): android.graphics.Bitmap {
        return try {
            CursorHelper.getBitmapFromVectorDrawable(context, drawableRes)?.scale(size, size, false)
                ?: createFallbackBitmap(size)
        } catch (e: Exception) {
            createFallbackBitmap(size)
        }
    }
    
    /**
     * Create fallback bitmap if drawables are missing
     */
    private fun createFallbackBitmap(size: Int): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = androidx.core.content.ContextCompat.getColor(context, R.color.cursor_primary)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }
        
        val radius = (size / 2).toFloat() - paint.strokeWidth
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), radius, paint)
        return bitmap
    }
    
    /**
     * Show the gaze click animation view with improved error handling
     */
    fun show(): Boolean {
        return try {
            // Check if already attached to prevent duplicate additions
            if (ViewCompat.isAttachedToWindow(this)) {
                return false
            }
            
            // Check overlay permission before showing
            if (!hasOverlayPermission()) {
                return false
            }
            
            // Add to window manager with enhanced error handling
            windowManager.addView(this, layoutParams)
            
            // Ensure view is brought to front for proper z-order
            bringToFront()
            
            true
        } catch (e: WindowManager.BadTokenException) {
            // Handle bad token exceptions gracefully
            false
        } catch (e: SecurityException) {
            // Handle security exceptions for overlay permission
            false
        } catch (e: Exception) {
            // Handle any other exceptions
            false
        }
    }
    
    /**
     * Hide the gaze click animation view with proper cleanup
     */
    fun hide() {
        try {
            if (ViewCompat.isAttachedToWindow(this)) {
                windowManager.removeView(this)
            }
        } catch (e: IllegalArgumentException) {
            // View was already removed, ignore
        } catch (e: Exception) {
            // Log other exceptions but don't crash
            e.printStackTrace()
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true // No permission needed for older versions
        }
    }
    
    /**
     * Force view to front for proper z-order
     */
    override fun bringToFront() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = Float.MAX_VALUE
            }
        } catch (e: Exception) {
            // Ignore elevation setting errors
        }
    }
    
    /**
     * Update position for reuse of the same view instance
     */
    fun updatePosition(newOffset: CursorOffset) {
        try {
            if (ViewCompat.isAttachedToWindow(this)) {
                val size = layoutParams.width
                layoutParams.x = newOffset.x.toInt() - (size / 2)
                layoutParams.y = newOffset.y.toInt() - (size / 2)
                windowManager.updateViewLayout(this, layoutParams)
            }
        } catch (e: Exception) {
            // Handle update errors gracefully
            e.printStackTrace()
        }
    }
    
    /**
     * Check if the view is currently visible
     */
    fun isShowing(): Boolean {
        return ViewCompat.isAttachedToWindow(this)
    }
}