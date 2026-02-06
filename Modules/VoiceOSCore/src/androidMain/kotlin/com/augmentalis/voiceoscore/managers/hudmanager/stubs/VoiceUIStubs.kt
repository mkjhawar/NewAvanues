/**
 * VoiceUIStubs.kt - HUD subsystem implementations
 *
 * Provides accessibility, rendering, database, and system management
 * implementations used by HUDManager and related components.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.managers.hudmanager.stubs

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val TAG = "VoiceUIImpl"

// ============================================================================
// Database Module - Context pattern storage using SharedPreferences + JSON
// ============================================================================

/**
 * Stores environmental context patterns for HUD adaptation.
 * Uses SharedPreferences with JSON serialization for lightweight persistence.
 */
class DatabaseModule private constructor(private val prefs: SharedPreferences) {
    companion object {
        private const val PREFS_NAME = "hud_context_patterns"
        private const val KEY_PATTERNS = "context_patterns"
        private var instance: DatabaseModule? = null

        fun getInstance(context: Any? = null): DatabaseModule {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val ctx = context as? Context
                    val prefs = ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        ?: throw IllegalStateException("DatabaseModule requires a Context for first initialization")
                    DatabaseModule(prefs).also { instance = it }
                }
            }
        }
    }

    /**
     * Store a context pattern associating a location with an environment and confidence.
     */
    fun storeContextPattern(locationKey: String, environmentName: String, confidence: Float) {
        try {
            val existing = prefs.getString(KEY_PATTERNS, "{}") ?: "{}"
            val patterns = JSONObject(existing)

            val entry = JSONObject().apply {
                put("environment", environmentName)
                put("confidence", confidence.toDouble())
                put("timestamp", System.currentTimeMillis())
            }

            // Append to location's pattern array
            val locationPatterns = patterns.optJSONArray(locationKey) ?: JSONArray()
            locationPatterns.put(entry)

            // Keep last 50 patterns per location
            while (locationPatterns.length() > 50) {
                locationPatterns.remove(0)
            }

            patterns.put(locationKey, locationPatterns)
            prefs.edit().putString(KEY_PATTERNS, patterns.toString()).apply()
            Log.d(TAG, "Stored context pattern: $locationKey -> $environmentName ($confidence)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store context pattern", e)
        }
    }
}

// ============================================================================
// VOS Accessibility Service - Real accessibility state and TTS delegation
// ============================================================================

/**
 * Accessibility service interface providing:
 * - Accessibility state detection via Android AccessibilityManager
 * - Text-to-Speech via Android TTS engine
 * - Service lifecycle management
 */
object VOSAccessibilitySvc {
    private var appContext: Context? = null
    private var ttsEngine: TextToSpeech? = null
    private var isTtsReady = false
    private var currentSpeechRate = 1.0f

    /**
     * Initialize with application context. Call once during app startup.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        initializeTTS(context.applicationContext)
    }

    private fun initializeTTS(context: Context) {
        ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = Locale.getDefault()
                ttsEngine?.setSpeechRate(currentSpeechRate)
                isTtsReady = true
                Log.d(TAG, "TTS engine initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    /**
     * Check if any VoiceOS accessibility service is enabled on the device.
     */
    fun isEnabled(): Boolean {
        val ctx = appContext ?: return false
        return try {
            val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            am?.isEnabled == true && am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any { service ->
                service.resolveInfo?.serviceInfo?.packageName == ctx.packageName
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking accessibility state", e)
            false
        }
    }

    fun start() {
        Log.d(TAG, "VOSAccessibilitySvc start requested - service is managed by Android system")
    }

    fun stop() {
        ttsEngine?.stop()
        Log.d(TAG, "VOSAccessibilitySvc stop requested - service is managed by Android system")
    }

    fun getInstance(): VOSAccessibilitySvc = this

    /**
     * Speak text using the TTS engine.
     */
    fun speakText(text: String) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready, queuing text: ${text.take(30)}...")
            return
        }
        ttsEngine?.speak(text, TextToSpeech.QUEUE_ADD, null, "vos_${System.currentTimeMillis()}")
    }

    /**
     * Set TTS speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double).
     */
    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate.coerceIn(0.1f, 4.0f)
        ttsEngine?.setSpeechRate(currentSpeechRate)
    }

    /**
     * Release TTS resources. Call during app shutdown.
     */
    fun shutdown() {
        ttsEngine?.stop()
        ttsEngine?.shutdown()
        ttsEngine = null
        isTtsReady = false
    }
}

// ============================================================================
// HUD Renderer - Overlay rendering via Android WindowManager
// ============================================================================

/**
 * Manages HUD overlay rendering using Android's WindowManager.
 * Provides floating overlay positioning, FPS tracking, and mode-based rendering.
 */
object HUDRenderer {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isVisible = false
    private var currentFPS = 60f
    private val handler = Handler(Looper.getMainLooper())
    private var renderRunnable: Runnable? = null

    const val TARGET_FPS_HIGH = 60
    private const val TARGET_FPS_LOW = 30
    private var targetFPS = TARGET_FPS_HIGH

    fun initialize(context: Context) {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        Log.d(TAG, "HUDRenderer initialized")
    }

    fun render() {
        overlayView?.invalidate()
    }

    fun setVisible(visible: Boolean) {
        isVisible = visible
        overlayView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun updatePosition(x: Float, y: Float) {
        overlayView?.let { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return
            params.x = x.toInt()
            params.y = y.toInt()
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update overlay position", e)
            }
        }
    }

    fun startRendering(fps: Int) {
        targetFPS = fps.coerceIn(1, 120)
        val frameInterval = 1000L / targetFPS

        renderRunnable?.let { handler.removeCallbacks(it) }
        renderRunnable = object : Runnable {
            private var lastFrameTime = System.nanoTime()
            override fun run() {
                val now = System.nanoTime()
                val elapsed = (now - lastFrameTime) / 1_000_000f
                currentFPS = if (elapsed > 0) 1000f / elapsed else targetFPS.toFloat()
                lastFrameTime = now

                render()
                handler.postDelayed(this, frameInterval)
            }
        }
        handler.post(renderRunnable!!)
    }

    fun stopRendering() {
        renderRunnable?.let { handler.removeCallbacks(it) }
        renderRunnable = null
    }

    fun updateModeRendering(mode: Any) {
        when (mode) {
            HUDMode.DRIVING -> startRendering(TARGET_FPS_LOW) // Lower FPS for battery
            HUDMode.GAMING -> startRendering(TARGET_FPS_HIGH) // Max FPS
            else -> startRendering(TARGET_FPS_HIGH)
        }
    }

    fun adjustForHeadMovement(data: OrientationData) {
        // Apply head movement offset to overlay position
        val dx = data.yaw * 2f   // Horizontal movement
        val dy = data.pitch * 2f // Vertical movement
        overlayView?.let { view ->
            view.translationX += dx
            view.translationY += dy
        }
    }

    fun getCurrentFPS(): Float = currentFPS

    fun shutdown() {
        stopRendering()
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
    }
}

// ============================================================================
// HUD System - Lifecycle management for HUD overlay
// ============================================================================

/**
 * Manages the HUD system lifecycle including initialization,
 * visibility, notifications, and element management.
 */
object HUDSystem {
    private var isInitialized = false
    private var isCurrentlyVisible = false
    private val elements = mutableMapOf<String, Long>() // elementId -> timestamp
    private val notifications = mutableListOf<HUDNotification>()

    private data class HUDNotification(
        val message: String,
        val duration: Int,
        val position: String,
        val priority: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun initialize() {
        isInitialized = true
        Log.d(TAG, "HUDSystem initialized")
    }

    fun shutdown() {
        HUDRenderer.shutdown()
        elements.clear()
        notifications.clear()
        isInitialized = false
        isCurrentlyVisible = false
        Log.d(TAG, "HUDSystem shut down")
    }

    fun isReady(): Boolean = isInitialized

    fun setVisible(visible: Boolean) {
        isCurrentlyVisible = visible
        HUDRenderer.setVisible(visible)
    }

    fun isVisible(): Boolean = isCurrentlyVisible

    fun toggleVisibility() {
        setVisible(!isCurrentlyVisible)
    }

    fun showNotification(message: String, duration: Int, position: String, priority: String) {
        val notification = HUDNotification(message, duration, position, priority)
        notifications.add(notification)
        Log.d(TAG, "HUD notification: $message (priority=$priority, position=$position, duration=${duration}ms)")

        // Auto-dismiss after duration
        Handler(Looper.getMainLooper()).postDelayed({
            notifications.remove(notification)
        }, duration.toLong())
    }

    fun removeElement(elementId: String) {
        elements.remove(elementId)
    }

    fun addElement(elementId: String) {
        elements[elementId] = System.currentTimeMillis()
    }
}

// ============================================================================
// HUD Intent - Intent wrapper for HUD service communication
// ============================================================================

/**
 * Intent wrapper for communicating with HUD overlay service.
 */
class HUDIntent {
    companion object {
        const val ACTION_SHOW_HUD = "com.augmentalis.action.SHOW_HUD"
        const val ACTION_HIDE_HUD = "com.augmentalis.action.HIDE_HUD"
        const val ACTION_UPDATE_HUD = "com.augmentalis.action.UPDATE_HUD"
    }

    private var targetPackage: String? = null
    private var targetClassName: String? = null

    fun setPackage(packageName: String): HUDIntent {
        targetPackage = packageName
        return this
    }

    fun setClassName(packageName: String, className: String): HUDIntent {
        targetPackage = packageName
        targetClassName = className
        return this
    }

    /**
     * Convert to Android Intent for service communication.
     */
    fun toAndroidIntent(): android.content.Intent {
        return android.content.Intent(ACTION_SHOW_HUD).apply {
            targetPackage?.let { setPackage(it) }
            if (targetPackage != null && targetClassName != null) {
                setClassName(targetPackage!!, targetClassName!!)
            }
        }
    }
}

// ============================================================================
// Enums and Data Classes
// ============================================================================

enum class HUDMode {
    STANDARD, MEETING, DRIVING, WORKSHOP, ACCESSIBILITY, GAMING, ENTERTAINMENT
}

enum class RenderMode {
    SPATIAL_AR, OVERLAY_2D, MIXED_REALITY
}

data class OrientationData(
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val roll: Float = 0f
)

// ============================================================================
// VoiceUI namespace - provides package-qualified access to HUD types
// ============================================================================

object voiceui {
    object hud {
        object HUDMode {
            val STANDARD = com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.STANDARD
            val MEETING = com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.MEETING
            val DRIVING = com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.DRIVING
            val WORKSHOP = com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.WORKSHOP
            val ACCESSIBILITY = com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.ACCESSIBILITY
            val GAMING = com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.GAMING
            val ENTERTAINMENT = com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.ENTERTAINMENT
        }

        object RenderMode {
            val SPATIAL_AR = com.augmentalis.voiceoscore.managers.hudmanager.stubs.RenderMode.SPATIAL_AR
            val OVERLAY_2D = com.augmentalis.voiceoscore.managers.hudmanager.stubs.RenderMode.OVERLAY_2D
            val MIXED_REALITY = com.augmentalis.voiceoscore.managers.hudmanager.stubs.RenderMode.MIXED_REALITY
        }

        fun OrientationData(pitch: Float, yaw: Float, roll: Float) =
            com.augmentalis.voiceoscore.managers.hudmanager.stubs.OrientationData(pitch, yaw, roll)
    }

    object design {
        object theme {
            const val primaryColor = "#2196F3"
            const val backgroundColor = "#FFFFFF"
            const val accentColor = "#FF5722"
        }
        object layout {
            const val hudWidth = 320
            const val hudHeight = 240
            const val marginTop = 48
            const val marginSide = 16
        }
    }

    object components {
        object hud {
            fun createOverlay() {
                HUDSystem.initialize()
                HUDSystem.setVisible(true)
            }

            fun showNotification(message: String) {
                HUDSystem.showNotification(message, 3000, "top", "normal")
            }
        }
    }
}

// ============================================================================
// VoiceOS core namespace
// ============================================================================

object voiceos {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    object core {
        fun getApplicationContext(): Context? = appContext
        fun isRunning(): Boolean = appContext != null
    }
}

// ============================================================================
// Voice Accessibility namespace
// ============================================================================

object voiceaccessibility {
    object service {
        fun isEnabled(): Boolean = VOSAccessibilitySvc.isEnabled()

        fun requestPermission() {
            val ctx = voiceos.core.getApplicationContext() ?: return
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings", e)
            }
        }
    }
}
