/**
 * CommandDiscoveryIntegration.kt - Command discovery integration for LearnApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 * Modified: 2025-12-18
 *
 * Integration component that auto-observes ExplorationEngine.state() via StateFlow
 * and triggers discovery flow (visual overlay, audio summary, tutorial) when exploration completes.
 */
package com.augmentalis.voiceoscore.learnapp.integration

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import com.augmentalis.voiceoscore.learnapp.models.ExplorationStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Command Discovery Integration
 *
 * Phase 3 component that auto-observes ExplorationEngine state and triggers
 * discovery flow when exploration completes.
 *
 * Features:
 * - Visual overlay showing discovered commands (10s auto-hide)
 * - Audio summary of discovered commands via TTS
 * - State transition handling for all exploration states
 * - Automatic cleanup on failure/cancel
 *
 * @param context Application context for overlays and TTS
 * @param explorationEngine The exploration engine to observe
 */
class CommandDiscoveryIntegration(
    private val context: Context,
    private val explorationEngine: ExplorationEngine
) {
    companion object {
        private const val TAG = "CommandDiscovery"
        private const val OVERLAY_AUTO_HIDE_DELAY_MS = 10000L
        private const val OVERLAY_FADE_DURATION_MS = 300L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var isActive = false
    private var overlayView: View? = null
    private var autoHideJob: Job? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initializeTts()
        startObserving()
    }

    /**
     * Initialize Text-to-Speech engine for audio summaries.
     */
    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                if (isTtsReady) {
                    Log.d(TAG, "TTS initialized successfully")
                } else {
                    Log.w(TAG, "TTS language not supported")
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    /**
     * Start observing exploration engine state.
     *
     * Handles all state transitions and triggers appropriate actions:
     * - Completed: Shows overlay and plays audio summary
     * - Failed: Shows error notification
     * - Running: Logs progress updates
     * - Paused: Logs pause state
     */
    private fun startObserving() {
        isActive = true
        scope.launch {
            try {
                explorationEngine.explorationState.collectLatest { state ->
                    handleStateChange(state)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing exploration state", e)
            }
        }
        Log.d(TAG, "Command discovery observation started")
    }

    /**
     * Handle exploration state changes.
     *
     * @param state Current exploration state
     */
    private suspend fun handleStateChange(state: ExplorationState) {
        Log.d(TAG, "Exploration state changed: ${state::class.simpleName}")

        when (state) {
            is ExplorationState.Idle -> {
                dismissOverlayIfShowing()
            }

            is ExplorationState.ConsentRequested -> {
                Log.d(TAG, "Consent requested for app: ${state.appName}")
            }

            is ExplorationState.ConsentCancelled -> {
                Log.d(TAG, "Consent cancelled for package: ${state.packageName}")
            }

            is ExplorationState.Preparing -> {
                Log.d(TAG, "Preparing exploration for: ${state.packageName}")
            }

            is ExplorationState.Running -> {
                Log.d(TAG, "Exploration running: ${state.progress.currentScreen}")
            }

            is ExplorationState.PausedForLogin -> {
                Log.d(TAG, "Exploration paused for login: ${state.packageName}")
            }

            is ExplorationState.PausedByUser -> {
                Log.d(TAG, "Exploration paused by user: ${state.packageName}")
            }

            is ExplorationState.Paused -> {
                Log.d(TAG, "Exploration paused: ${state.reason}")
            }

            is ExplorationState.Completed -> {
                Log.i(TAG, "Exploration completed for: ${state.packageName}")
                onExplorationCompleted(state.packageName, state.stats)
            }

            is ExplorationState.Failed -> {
                Log.e(TAG, "Exploration failed for: ${state.packageName}", state.error)
                onExplorationFailed(state.packageName, state.error)
            }
        }
    }

    /**
     * Handle successful exploration completion.
     *
     * Shows visual overlay with discovered commands and plays audio summary.
     *
     * @param packageName The explored app package name
     * @param stats Exploration statistics
     */
    private fun onExplorationCompleted(packageName: String, stats: ExplorationStats) {
        mainHandler.post {
            showDiscoveryOverlay(packageName, stats)
            playAudioSummary(packageName, stats)
            scheduleAutoHide()
        }
    }

    /**
     * Handle exploration failure.
     *
     * Shows error notification to user.
     *
     * @param packageName The app that failed to explore
     * @param error The error that caused the failure
     */
    private fun onExplorationFailed(packageName: String, error: Throwable) {
        mainHandler.post {
            showErrorOverlay(packageName, error.message ?: "Unknown error")
            scheduleAutoHide()
        }
    }

    /**
     * Show discovery overlay with exploration results.
     *
     * @param packageName The explored app package name
     * @param stats Exploration statistics
     */
    private fun showDiscoveryOverlay(packageName: String, stats: ExplorationStats) {
        dismissOverlayIfShowing()

        val appName = getAppName(packageName)
        overlayView = createDiscoveryOverlayView(appName, stats)

        val params = createOverlayLayoutParams(
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            y = 100
        )

        overlayView?.let { view ->
            try {
                windowManager.addView(view, params)
                fadeIn(view, OVERLAY_FADE_DURATION_MS)
                Log.d(TAG, "Discovery overlay shown for: $appName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show discovery overlay", e)
            }
        }
    }

    /**
     * Show error overlay for failed exploration.
     *
     * @param packageName The app that failed
     * @param errorMessage Error description
     */
    private fun showErrorOverlay(packageName: String, errorMessage: String) {
        dismissOverlayIfShowing()

        val appName = getAppName(packageName)
        overlayView = createErrorOverlayView(appName, errorMessage)

        val params = createOverlayLayoutParams(
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            y = 100
        )

        overlayView?.let { view ->
            try {
                windowManager.addView(view, params)
                fadeIn(view, OVERLAY_FADE_DURATION_MS)
                Log.d(TAG, "Error overlay shown for: $appName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show error overlay", e)
            }
        }
    }

    /**
     * Create discovery overlay view showing exploration results.
     *
     * @param appName Display name of the explored app
     * @param stats Exploration statistics
     * @return Configured overlay view
     */
    private fun createDiscoveryOverlayView(appName: String, stats: ExplorationStats): View {
        val container = FrameLayout(context).apply {
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xF0333333.toInt())
        }

        val innerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val titleView = TextView(context).apply {
            text = "Commands Discovered"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val appNameView = TextView(context).apply {
            text = appName
            textSize = 14f
            setTextColor(0xFFCCCCCC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 12)
        }

        val statsView = TextView(context).apply {
            text = buildStatsText(stats)
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.3f)
        }

        val hintView = TextView(context).apply {
            text = "Tap to dismiss or wait 10 seconds"
            textSize = 11f
            setTextColor(0xFF999999.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        innerLayout.addView(titleView)
        innerLayout.addView(appNameView)
        innerLayout.addView(statsView)
        innerLayout.addView(hintView)
        container.addView(innerLayout)

        container.setOnClickListener {
            dismissOverlayIfShowing()
        }

        return container
    }

    /**
     * Create error overlay view.
     *
     * @param appName Display name of the app
     * @param errorMessage Error description
     * @return Configured error overlay view
     */
    private fun createErrorOverlayView(appName: String, errorMessage: String): View {
        val container = FrameLayout(context).apply {
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xF0442222.toInt())
        }

        val innerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val titleView = TextView(context).apply {
            text = "Exploration Failed"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val appNameView = TextView(context).apply {
            text = appName
            textSize = 14f
            setTextColor(0xFFCCCCCC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 12)
        }

        val errorView = TextView(context).apply {
            text = errorMessage
            textSize = 13f
            setTextColor(0xFFFF9999.toInt())
            gravity = Gravity.CENTER
            maxLines = 3
        }

        val hintView = TextView(context).apply {
            text = "Tap to dismiss"
            textSize = 11f
            setTextColor(0xFF999999.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        innerLayout.addView(titleView)
        innerLayout.addView(appNameView)
        innerLayout.addView(errorView)
        innerLayout.addView(hintView)
        container.addView(innerLayout)

        container.setOnClickListener {
            dismissOverlayIfShowing()
        }

        return container
    }

    /**
     * Build stats text for display.
     *
     * @param stats Exploration statistics
     * @return Formatted stats string
     */
    private fun buildStatsText(stats: ExplorationStats): String {
        return buildString {
            append("Screens explored: ${stats.totalScreens}\n")
            append("Elements found: ${stats.totalElements}\n")
            append("Navigation paths: ${stats.totalEdges}\n")
            append("Duration: ${formatDuration(stats.durationMs)}")
        }
    }

    /**
     * Format duration in milliseconds to human-readable string.
     *
     * @param durationMs Duration in milliseconds
     * @return Formatted duration string
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${remainingSeconds}s"
        }
    }

    /**
     * Play audio summary using TTS.
     *
     * @param packageName The explored app package name
     * @param stats Exploration statistics
     */
    private fun playAudioSummary(packageName: String, stats: ExplorationStats) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready, skipping audio summary")
            return
        }

        val appName = getAppName(packageName)
        val summary = buildAudioSummary(appName, stats)

        textToSpeech?.speak(
            summary,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "discovery_summary"
        )
        Log.d(TAG, "Playing audio summary: $summary")
    }

    /**
     * Build audio summary text.
     *
     * @param appName Display name of the app
     * @param stats Exploration statistics
     * @return Summary text for TTS
     */
    private fun buildAudioSummary(appName: String, stats: ExplorationStats): String {
        return buildString {
            append("Finished learning $appName. ")
            append("Found ${stats.totalElements} elements ")
            append("across ${stats.totalScreens} screens.")
        }
    }

    /**
     * Schedule automatic overlay dismissal after timeout.
     */
    private fun scheduleAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(OVERLAY_AUTO_HIDE_DELAY_MS)
            mainHandler.post {
                dismissOverlayIfShowing()
            }
        }
    }

    /**
     * Dismiss overlay if currently showing.
     */
    private fun dismissOverlayIfShowing() {
        autoHideJob?.cancel()
        autoHideJob = null

        overlayView?.let { view ->
            fadeOut(view, OVERLAY_FADE_DURATION_MS) {
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing overlay view", e)
                }
            }
        }
        overlayView = null
    }

    /**
     * Get display name for package.
     *
     * @param packageName Package name to look up
     * @return App display name or package name if not found
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not get app name for: $packageName", e)
            packageName.substringAfterLast(".")
        }
    }

    /**
     * Create overlay layout parameters.
     *
     * @param gravity Gravity for positioning
     * @param y Y offset from gravity anchor
     * @return WindowManager.LayoutParams configured for overlay
     */
    private fun createOverlayLayoutParams(gravity: Int, y: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.y = y
        }
    }

    /**
     * Fade in animation for view.
     *
     * @param view View to fade in
     * @param durationMs Animation duration in milliseconds
     */
    private fun fadeIn(view: View, durationMs: Long) {
        val animation = AlphaAnimation(0f, 1f).apply {
            duration = durationMs
        }
        view.startAnimation(animation)
    }

    /**
     * Fade out animation for view with completion callback.
     *
     * @param view View to fade out
     * @param durationMs Animation duration in milliseconds
     * @param onComplete Callback after animation completes
     */
    private fun fadeOut(view: View, durationMs: Long, onComplete: () -> Unit) {
        val animation = AlphaAnimation(1f, 0f).apply {
            duration = durationMs
        }
        view.startAnimation(animation)
        mainHandler.postDelayed(onComplete, durationMs)
    }

    /**
     * Clean up resources.
     *
     * Stops observation, dismisses overlay, and releases TTS.
     */
    fun cleanup() {
        isActive = false
        autoHideJob?.cancel()

        mainHandler.post {
            dismissOverlayIfShowing()
        }

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsReady = false

        scope.cancel()
        Log.d(TAG, "Command discovery integration cleaned up")
    }
}
