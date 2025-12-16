/**
 * CursorOverlayManager.kt
 *
 * Created: 2025-09-26 16:20:54 IST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Manages cursor overlay without being a service
 * Extracted from VoiceCursorOverlayService functionality
 * Module: VoiceCursor System
 */

package com.augmentalis.voiceos.cursor.manager

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.compose.ui.platform.ComposeView
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.core.CursorType
import com.augmentalis.voiceos.cursor.core.FilterStrength
import com.augmentalis.voiceos.cursor.helper.VoiceCursorIMUIntegration
import com.augmentalis.voiceos.cursor.view.CursorAction
import com.augmentalis.voiceos.cursor.view.CursorView
import com.augmentalis.voiceos.cursor.view.MenuView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages cursor overlay without being a service
 * Extracted from VoiceCursorOverlayService functionality
 */
class CursorOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "CursorOverlayManager"
    }

    // Extracted from VoiceCursorOverlayService lines 90-100
    private var windowManager: WindowManager? = null
    private var cursorView: CursorView? = null
    private var menuComposeView: ComposeView? = null
    private var cursorConfig = CursorConfig()
    private var isOverlayVisible = false
    private var isMenuVisible = false
    private var imuIntegration: VoiceCursorIMUIntegration? = null

    // Reference to gesture handler for action dispatch
    private var gestureHandler: CursorGestureHandler? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // SharedPreferences for configuration
    private var sharedPrefs: SharedPreferences? = null

    /**
     * Initialize overlay manager with accessibility service
     */
    fun initialize(accessibilityService: AccessibilityService): Boolean {
        return try {
            gestureHandler = CursorGestureHandler(accessibilityService)
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            sharedPrefs = context.getSharedPreferences("voice_cursor_prefs", Context.MODE_PRIVATE)

            Log.d(TAG, "CursorOverlayManager initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CursorOverlayManager", e)
            false
        }
    }

    /**
     * Show cursor overlay
     * Extracted from VoiceCursorOverlayService.initializeCursorOverlay() (lines 484-510)
     */
    fun showCursor(config: CursorConfig = CursorConfig()): Boolean {
        return try {
            if (isOverlayVisible) return true

            cursorConfig = if (config == CursorConfig()) loadCursorConfig() else config
            createView()
            initializeIMU()
            isOverlayVisible = true

            Log.d(TAG, "Cursor overlay shown successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing cursor overlay", e)
            false
        }
    }

    /**
     * Hide cursor overlay
     * Extracted from VoiceCursorOverlayService.removeOverlay() (lines 810-822)
     */
    fun hideCursor(): Boolean {
        return try {
            removeOverlay()
            isOverlayVisible = false
            Log.d(TAG, "Cursor overlay hidden")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding cursor overlay", e)
            false
        }
    }

    /**
     * Center cursor on screen
     * Extracted from VoiceCursorOverlayService.centerCursor() (lines 728-730)
     */
    fun centerCursor(): Boolean {
        return try {
            cursorView?.centerCursor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error centering cursor", e)
            false
        }
    }

    /**
     * Move cursor to specified position
     *
     * @param position The target position to move the cursor to
     * @param animate Whether to animate the movement (default: true) - reserved for future use
     * @return true if cursor was moved successfully, false otherwise
     */
    @Suppress("UNUSED_PARAMETER")
    fun moveTo(position: CursorOffset, animate: Boolean = true): Boolean {
        return try {
            cursorView?.moveCursorTo(position)
            Log.d(TAG, "Cursor moved to position: (${position.x}, ${position.y})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error moving cursor to position", e)
            false
        }
    }

    /**
     * Execute cursor action
     */
    fun executeAction(action: CursorAction, position: CursorOffset?): Boolean {
        val targetPosition = position ?: getCurrentPosition()
        return gestureHandler?.executeAction(action, targetPosition) ?: false
    }

    /**
     * Update cursor configuration
     * Extracted from VoiceCursorOverlayService.updateConfiguration() (lines 743-749)
     */
    fun updateConfiguration(config: CursorConfig): Boolean {
        return try {
            cursorConfig = config
            cursorView?.updateCursorStyle(config)
            imuIntegration?.setSensitivity(config.speed / 10.0f)
            Log.d(TAG, "Configuration updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating configuration", e)
            false
        }
    }

    /**
     * Get current cursor position
     */
    fun getCurrentPosition(): CursorOffset {
        return cursorView?.getClickPosition() ?: CursorOffset(0f, 0f)
    }

    /**
     * Check if cursor is visible
     */
    fun isVisible(): Boolean = isOverlayVisible

    /**
     * Dispose resources
     */
    fun dispose() {
        removeOverlay()
        imuIntegration?.dispose()
        serviceScope.cancel()
        gestureHandler?.dispose()
        gestureHandler = null
    }

    // Private methods extracted from VoiceCursorOverlayService

    /**
     * Load cursor configuration from SharedPreferences
     * Extracted from VoiceCursorOverlayService.loadCursorConfig() (lines 288-324)
     */
    private fun loadCursorConfig(): CursorConfig {
        val prefs = sharedPrefs ?: return CursorConfig()

        val typeString = prefs.getString("cursor_type", "Normal") ?: "Normal"
        val type = when (typeString) {
            "Hand" -> CursorType.Hand
            "Custom" -> CursorType.Custom
            else -> CursorType.Normal
        }

        val gazeEnabled = prefs.getBoolean("gaze_enabled", false)
        val gazeDelay = if (gazeEnabled) {
            prefs.getLong("gaze_delay", 1500L)
        } else {
            0L
        }

        // Load filter strength setting
        val filterStrengthValue = prefs.getInt("smoothing_strength", 50)
        val filterStrength = when {
            filterStrengthValue < 30 -> FilterStrength.Low
            filterStrengthValue < 70 -> FilterStrength.Medium
            else -> FilterStrength.High
        }

        return CursorConfig(
            type = type,
            size = prefs.getInt("cursor_size", 48),
            color = prefs.getInt("cursor_color", Color.BLUE),
            speed = prefs.getInt("cursor_speed", 8),
            gazeClickDelay = gazeDelay,
            showCoordinates = prefs.getBoolean("show_coordinates", false),
            jitterFilterEnabled = prefs.getBoolean("jitter_filter_enabled", true),
            filterStrength = filterStrength,
            motionSensitivity = prefs.getFloat("motion_sensitivity", 0.7f)
        )
    }

    /**
     * Create cursor view and add to window manager
     * Extracted from VoiceCursorOverlayService.createView() (lines 515-573)
     */
    private fun createView() {
        cursorView = CursorView(context).apply {
            updateCursorStyle(cursorConfig)

            // Set up callbacks
            onMenuRequest = { position ->
                showMenuAtPosition(position)
            }

            onCursorMove = { position ->
                // Update gesture handler with position
                gestureHandler?.updateCursorPosition(position)
            }

            onGazeAutoClick = { position ->
                Log.d(TAG, "Gaze auto-click at (${position.x}, ${position.y})")
                gestureHandler?.executeAction(CursorAction.SINGLE_CLICK, position)
            }
        }

        // Create parent layout - extracted implementation
        val cursorParentLayout = RelativeLayout(context).apply {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        cursorView?.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        // Create WindowManager.LayoutParams
        val lp = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT

            layoutInDisplayCutoutMode = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        cursorParentLayout.addView(cursorView)
        windowManager?.addView(cursorParentLayout, lp)
        cursorView?.startTracking()
    }

    /**
     * Initialize IMU integration
     * Extracted from VoiceCursorOverlayService (lines 490-503)
     */
    private fun initializeIMU() {
        imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
            setOnPositionUpdate { _ ->
                cursorView?.let { view ->
                    serviceScope.launch {
                        view.post {
                            // Position update handled internally by View
                        }
                    }
                }
            }
            start()
        }
    }

    /**
     * Remove overlay from window manager
     * Extracted from VoiceCursorOverlayService.removeOverlay() (lines 810-822)
     */
    private fun removeOverlay() {
        cursorView?.let { view ->
            view.stopTracking()
            try {
                windowManager?.removeView(view.parent as ViewGroup)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing cursor view", e)
            }
        }
        cursorView = null
        hideMenu()
    }

    /**
     * Show cursor menu at specified position
     * Extracted from VoiceCursorOverlayService.showMenuAtPosition() (lines 580-614)
     */
    private fun showMenuAtPosition(position: CursorOffset) {
        if (isMenuVisible) {
            hideMenu()
            return
        }

        isMenuVisible = true

        menuComposeView = ComposeView(context).apply {
            setContent {
                MenuView(
                    isVisible = isMenuVisible,
                    position = position,
                    onAction = { action ->
                        executeAction(action, position)
                        hideMenu()
                    },
                    onDismiss = {
                        hideMenu()
                    }
                )
            }
        }

        val menuParams = createMenuLayoutParams()
        windowManager?.addView(menuComposeView, menuParams)

        serviceScope.launch {
            delay(5000)
            hideMenu()
        }
    }

    /**
     * Hide cursor menu
     * Extracted from VoiceCursorOverlayService.hideMenu() (lines 637-647)
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
     * Create layout parameters for menu overlay
     * Extracted from VoiceCursorOverlayService.createMenuLayoutParams() (lines 619-632)
     */
    private fun createMenuLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
}