// Author: Manoj Jhawar
// Purpose: Enhanced display and overlay management component with external display support

package com.augmentalis.devicemanager.display

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import com.augmentalis.devicemanager.DeviceInfo
import com.augmentalis.devicemanager.*

/**
 * Display Overlay Manager Component
 * Handles display overlays, external displays, and desktop modes (DeX, Android Desktop)
 * Supports tethered and wireless smart glasses, monitors, TVs
 * Renamed from VosDisplayManager to avoid conflict with android.hardware.display.DisplayManager
 */
class DisplayOverlayManager(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val overlays = mutableMapOf<String, View>()
    private val deviceInfo by lazy { DeviceInfo(context) }
    
    // State flows for reactive display updates
    private val _displayMode = MutableStateFlow(DisplayMode.STANDARD)
    val displayMode: StateFlow<DisplayMode> = _displayMode
    
    private val _externalDisplays = MutableStateFlow<List<ExternalDisplay>>(emptyList())
    val externalDisplays: StateFlow<List<ExternalDisplay>> = _externalDisplays
    
    init {
        detectDisplayConfiguration()
        registerDisplayListener()
    }
    
    /**
     * Check if overlay permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Create system overlay
     */
    fun createOverlay(
        id: String,
        view: View,
        position: OverlayPosition = OverlayPosition.TOP_CENTER,
        width: Int = WindowManager.LayoutParams.WRAP_CONTENT,
        height: Int = WindowManager.LayoutParams.WRAP_CONTENT
    ): Boolean {
        if (!hasOverlayPermission()) return false
        if (overlays.containsKey(id)) return false
        
        val params = WindowManager.LayoutParams().apply {
            this.width = width
            this.height = height
            this.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            this.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            this.format = PixelFormat.TRANSLUCENT
            this.gravity = position.toGravity()
        }
        
        try {
            windowManager.addView(view, params)
            overlays[id] = view
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Update overlay position
     */
    fun updateOverlay(id: String, position: OverlayPosition) {
        overlays[id]?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.gravity = position.toGravity()
            windowManager.updateViewLayout(view, params)
        }
    }
    
    /**
     * Remove overlay
     */
    fun removeOverlay(id: String) {
        overlays[id]?.let { view ->
            windowManager.removeView(view)
            overlays.remove(id)
        }
    }
    
    /**
     * Show all overlays
     */
    fun showOverlays() {
        overlays.values.forEach { it.visibility = View.VISIBLE }
    }
    
    /**
     * Hide all overlays
     */
    fun hideOverlays() {
        overlays.values.forEach { it.visibility = View.GONE }
    }
    
    /**
     * Get display configuration
     */
    fun getDisplayConfig(): DisplayConfig {
        val (rotation, refreshRate) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            val display = context.display
            Pair(
                display?.rotation ?: 0,
                display?.refreshRate ?: 60f
            )
        } else {
            // Android 10 and below (API 29-)
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            Pair(display.rotation, display.refreshRate)
        }
        
        val (isHdr, isWideColorGamut) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ has better display capabilities API
            val display = context.display
            if (display != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Pair(display.isHdr, display.isWideColorGamut)
            } else {
                Pair(false, false)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8-10 (API 26-29)
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            Pair(display.isHdr, display.isWideColorGamut)
        } else {
            // Android 7 and below
            Pair(false, false)
        }
        
        return DisplayConfig(
            rotation = rotation,
            refreshRate = refreshRate,
            isHdr = isHdr,
            isWideColorGamut = isWideColorGamut,
            overlayCount = overlays.size
        )
    }
    
    /**
     * Detect current display configuration
     */
    private fun detectDisplayConfiguration() {
        // Check for DeX mode
        if (deviceInfo.isDeXMode()) {
            _displayMode.value = DisplayMode.DEX_MODE
        } 
        // Check for desktop mode
        else if (deviceInfo.isDesktopMode()) {
            _displayMode.value = DisplayMode.DESKTOP_MODE
        }
        // Check for external displays
        else if (hasExternalDisplay()) {
            _displayMode.value = DisplayMode.EXTENDED
        }
        // Standard mobile mode
        else {
            _displayMode.value = DisplayMode.STANDARD
        }
        
        // Update external displays list
        _externalDisplays.value = deviceInfo.getExternalDisplays()
    }
    
    /**
     * Check if external display is connected
     */
    fun hasExternalDisplay(): Boolean {
        return displayManager?.displays?.any { 
            it.displayId != Display.DEFAULT_DISPLAY 
        } ?: false
    }
    
    /**
     * Get primary external display
     */
    fun getPrimaryExternalDisplay(): Display? {
        return displayManager?.displays?.firstOrNull { 
            it.displayId != Display.DEFAULT_DISPLAY 
        }
    }
    
    /**
     * Create overlay on specific display
     */
    fun createOverlayOnDisplay(
        id: String,
        view: View,
        displayId: Int,
        position: OverlayPosition = OverlayPosition.TOP_CENTER,
        width: Int = WindowManager.LayoutParams.WRAP_CONTENT,
        height: Int = WindowManager.LayoutParams.WRAP_CONTENT
    ): Boolean {
        if (!hasOverlayPermission()) return false
        if (overlays.containsKey(id)) return false
        
        val display = displayManager?.getDisplay(displayId) ?: return false
        
        val params = WindowManager.LayoutParams().apply {
            this.width = width
            this.height = height
            this.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            this.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            this.format = PixelFormat.TRANSLUCENT
            this.gravity = position.toGravity()
            
            // Set display for Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.preferredDisplayModeId = display.mode.modeId
            }
        }
        
        try {
            windowManager.addView(view, params)
            overlays[id] = view
            return true
        } catch (e: Exception) {
            Log.e("DisplayOverlayManager", "Failed to create overlay on display $displayId", e)
            return false
        }
    }
    
    /**
     * Extend VoiceUI to external display
     */
    fun extendToExternalDisplay(displayId: Int): Boolean {
        val display = displayManager?.getDisplay(displayId) ?: return false
        
        // Create a presentation context for the external display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val presentationContext = context.createDisplayContext(display)
            // Your VoiceUI can now be extended to this display using presentationContext
            return true
        }
        
        return false
    }
    
    /**
     * Configure for smart glasses
     */
    fun configureForSmartGlasses(glassesType: SmartGlassesType) {
        when (glassesType) {
            SmartGlassesType.TETHERED -> {
                // Configuration for tethered glasses (e.g., via USB-C)
                _displayMode.value = DisplayMode.SMART_GLASSES_TETHERED
            }
            SmartGlassesType.WIRELESS -> {
                // Configuration for wireless glasses (e.g., via WiFi Direct)
                _displayMode.value = DisplayMode.SMART_GLASSES_WIRELESS
            }
            SmartGlassesType.STANDALONE -> {
                // Configuration for standalone glasses
                _displayMode.value = DisplayMode.SMART_GLASSES_STANDALONE
            }
        }
    }
    
    /**
     * Register display listener for changes
     */
    private fun registerDisplayListener() {
        displayManager?.registerDisplayListener(displayListener, null)
    }
    
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            detectDisplayConfiguration()
            Log.d("DisplayOverlayManager", "Display added: $displayId")
        }
        
        override fun onDisplayRemoved(displayId: Int) {
            detectDisplayConfiguration()
            Log.d("DisplayOverlayManager", "Display removed: $displayId")
        }
        
        override fun onDisplayChanged(displayId: Int) {
            detectDisplayConfiguration()
            Log.d("DisplayOverlayManager", "Display changed: $displayId")
        }
    }
    
    /**
     * Get display mode for UI adaptation
     */
    fun getDisplayModeForUI(): DisplayMode {
        return _displayMode.value
    }
    
    /**
     * Check if running in desktop mode (DeX or Android Desktop)
     */
    fun isInDesktopMode(): Boolean {
        return _displayMode.value == DisplayMode.DEX_MODE || 
               _displayMode.value == DisplayMode.DESKTOP_MODE
    }
    
    /**
     * Get optimal UI scaling for current display configuration
     */
    fun getOptimalUIScale(): Float {
        return when (_displayMode.value) {
            DisplayMode.DEX_MODE -> 1.5f  // Larger UI for desktop
            DisplayMode.DESKTOP_MODE -> 1.5f
            DisplayMode.EXTENDED -> 1.2f  // Slightly larger for external
            DisplayMode.SMART_GLASSES_TETHERED -> 0.8f  // Smaller for glasses
            DisplayMode.SMART_GLASSES_WIRELESS -> 0.8f
            DisplayMode.SMART_GLASSES_STANDALONE -> 0.8f
            else -> 1.0f  // Standard mobile scale
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        overlays.keys.toList().forEach { removeOverlay(it) }
        displayManager?.unregisterDisplayListener(displayListener)
        deviceInfo.release()
    }
}

// Enums for display configuration

enum class DisplayMode {
    STANDARD,           // Standard mobile display
    EXTENDED,           // External display connected
    DEX_MODE,          // Samsung DeX mode
    DESKTOP_MODE,      // Android Desktop mode
    SMART_GLASSES_TETHERED,    // Tethered smart glasses
    SMART_GLASSES_WIRELESS,    // Wireless smart glasses
    SMART_GLASSES_STANDALONE   // Standalone smart glasses
}

enum class SmartGlassesType {
    TETHERED,   // Connected via USB-C/DisplayPort
    WIRELESS,   // Connected via WiFi/Bluetooth
    STANDALONE  // Running directly on glasses
}

enum class OverlayPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;
    
    fun toGravity(): Int = when (this) {
        TOP_LEFT -> Gravity.TOP or Gravity.START
        TOP_CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        TOP_RIGHT -> Gravity.TOP or Gravity.END
        CENTER_LEFT -> Gravity.CENTER_VERTICAL or Gravity.START
        CENTER -> Gravity.CENTER
        CENTER_RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
        BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
        BOTTOM_CENTER -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
    }
}

data class DisplayConfig(
    val rotation: Int,
    val refreshRate: Float,
    val isHdr: Boolean,
    val isWideColorGamut: Boolean,
    val overlayCount: Int
)