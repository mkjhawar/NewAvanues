/**
 * BaseOverlay.kt - Base overlay infrastructure for VoiceOS
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-03
 * 
 * Base class for all system overlays with glassmorphism design.
 * Provides common functionality for overlay management.
 */
package com.augmentalis.voiceos.accessibility.ui.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.augmentalis.voiceos.accessibility.ui.theme.AccessibilityTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Base class for system overlays
 */
abstract class BaseOverlay(
    protected val context: Context,
    protected val overlayType: OverlayType = OverlayType.FLOATING
) {
    companion object {
        private const val TAG = "BaseOverlay"
    }
    
    protected val windowManager: WindowManager = 
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    protected var overlayView: View? = null
    protected var overlayVisible = false
    protected val overlayScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Create the overlay layout parameters
     */
    protected open fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            width = when (overlayType) {
                OverlayType.FULLSCREEN -> WindowManager.LayoutParams.MATCH_PARENT
                OverlayType.FLOATING -> WindowManager.LayoutParams.WRAP_CONTENT
                OverlayType.POSITIONED -> WindowManager.LayoutParams.WRAP_CONTENT
            }
            
            height = when (overlayType) {
                OverlayType.FULLSCREEN -> WindowManager.LayoutParams.MATCH_PARENT
                OverlayType.FLOATING -> WindowManager.LayoutParams.WRAP_CONTENT
                OverlayType.POSITIONED -> WindowManager.LayoutParams.WRAP_CONTENT
            }
            
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            flags = when (overlayType) {
                OverlayType.FULLSCREEN -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                OverlayType.FLOATING -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                OverlayType.POSITIONED -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
            
            format = PixelFormat.TRANSLUCENT
            gravity = getDefaultGravity()
        }
    }
    
    /**
     * Get default gravity for this overlay type
     */
    protected open fun getDefaultGravity(): Int {
        return when (overlayType) {
            OverlayType.FULLSCREEN -> Gravity.FILL
            OverlayType.FLOATING -> Gravity.TOP or Gravity.START
            OverlayType.POSITIONED -> Gravity.TOP or Gravity.START
        }
    }
    
    /**
     * Create the compose content for this overlay
     */
    @Composable
    abstract fun OverlayContent()
    
    /**
     * Show the overlay
     */
    open fun show(): Boolean {
        if (overlayVisible) return true
        
        return try {
            Log.d(TAG, "Showing overlay: ${this::class.simpleName}")
            
            val composeView = ComposeView(context).apply {
                setContent {
                    AccessibilityTheme {
                        DisposableEffect(Unit) {
                            onDispose {
                                // Cleanup when compose view is disposed
                            }
                        }
                        OverlayContent()
                    }
                }
            }
            
            val layoutParams = createLayoutParams()
            windowManager.addView(composeView, layoutParams)
            
            overlayView = composeView
            overlayVisible = true
            
            onOverlayShown()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${this::class.simpleName}", e)
            false
        }
    }
    
    /**
     * Hide the overlay
     */
    open fun hide(): Boolean {
        if (!overlayVisible) return true
        
        return try {
            Log.d(TAG, "Hiding overlay: ${this::class.simpleName}")
            
            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
            }
            
            overlayVisible = false
            onOverlayHidden()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay: ${this::class.simpleName}", e)
            false
        }
    }
    
    /**
     * Toggle overlay visibility
     */
    fun toggle(): Boolean {
        return if (overlayVisible) hide() else show()
    }
    
    /**
     * Update overlay position (for positioned overlays)
     */
    open fun updatePosition(x: Int, y: Int): Boolean {
        if (!overlayVisible || overlayType != OverlayType.POSITIONED) return false
        
        return try {
            overlayView?.let { view ->
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                layoutParams.x = x
                layoutParams.y = y
                windowManager.updateViewLayout(view, layoutParams)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update overlay position", e)
            false
        }
    }
    
    /**
     * Check if overlay is currently visible
     */
    fun isVisible(): Boolean = overlayVisible
    
    /**
     * Called when overlay is shown
     */
    protected open fun onOverlayShown() {
        // Override in subclasses if needed
    }
    
    /**
     * Called when overlay is hidden
     */
    protected open fun onOverlayHidden() {
        // Override in subclasses if needed
    }
    
    /**
     * Dispose of the overlay and clean up resources
     */
    open fun dispose() {
        Log.d(TAG, "Disposing overlay: ${this::class.simpleName}")
        hide()
        overlayScope.cancel()
    }
}

/**
 * Types of overlays supported
 */
enum class OverlayType {
    FULLSCREEN,  // Full screen overlay
    FLOATING,    // Floating window that can be interacted with
    POSITIONED   // Positioned overlay at specific coordinates (non-interactive)
}