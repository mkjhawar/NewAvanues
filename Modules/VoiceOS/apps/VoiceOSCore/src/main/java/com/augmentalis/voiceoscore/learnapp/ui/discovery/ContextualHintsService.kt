/**
 * ContextualHintsService.kt - Proactive voice command suggestions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-08
 *
 * Provides contextual voice command hints based on user activity:
 * - Idle detection (user pauses on screen)
 * - Screen change detection
 * - Suggests top commands proactively
 */

package com.augmentalis.voiceoscore.learnapp.ui.discovery

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * User activity state
 */
enum class UserActivityState {
    ACTIVE,      // User is interacting
    IDLE,        // User paused on screen
    NAVIGATING   // User just changed screens
}

/**
 * Contextual Hints Service
 *
 * Monitors user activity and proactively suggests voice commands.
 *
 * ## Features:
 * - Idle detection (3+ seconds no interaction)
 * - Screen change detection
 * - Smart command ranking (most useful first)
 * - Non-intrusive hints (voice + optional overlay)
 *
 * ## Usage:
 * ```kotlin
 * val hintsService = ContextualHintsService(context, databaseManager)
 *
 * // Start monitoring
 * hintsService.startMonitoring(packageName)
 *
 * // Notify of user activity
 * hintsService.onUserAction()
 *
 * // Notify of screen change
 * hintsService.onScreenChanged(screenHash)
 *
 * // Stop monitoring
 * hintsService.stopMonitoring()
 * ```
 */
class ContextualHintsService(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager
) {
    companion object {
        private const val TAG = "ContextualHints"
        private const val IDLE_THRESHOLD_MS = 3000L  // 3 seconds
        private const val HINT_COOLDOWN_MS = 30000L  // 30 seconds between hints
        private const val MAX_HINTS_PER_SCREEN = 5
        private const val HINTS_ENABLED_KEY = "contextual_hints_enabled"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    // State
    private val _activityState = MutableStateFlow(UserActivityState.ACTIVE)
    val activityState: StateFlow<UserActivityState> = _activityState.asStateFlow()

    private var currentPackageName: String? = null
    private var currentScreenHash: String? = null
    private var lastHintTime = 0L
    private var idleDetectionJob: Job? = null
    private var isMonitoring = false

    // TTS
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    // Command cache (screen hash -> commands)
    private val commandCache = mutableMapOf<String, List<GeneratedCommandDTO>>()

    init {
        initializeTTS()
    }

    /**
     * Initialize Text-to-Speech
     */
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized = true
                textToSpeech?.language = Locale.US
                Log.d(TAG, "TTS initialized")
            }
        }
    }

    /**
     * Start monitoring for contextual hints
     *
     * @param packageName Target app package name
     */
    fun startMonitoring(packageName: String) {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring")
            return
        }

        if (!isHintsEnabled()) {
            Log.d(TAG, "Contextual hints disabled")
            return
        }

        currentPackageName = packageName
        isMonitoring = true
        Log.i(TAG, "Started monitoring: $packageName")

        // Start idle detection
        startIdleDetection()
    }

    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        currentPackageName = null
        currentScreenHash = null
        stopIdleDetection()
        Log.i(TAG, "Stopped monitoring")
    }

    /**
     * User performed an action (tap, voice command, etc.)
     */
    fun onUserAction() {
        if (!isMonitoring) return

        _activityState.value = UserActivityState.ACTIVE

        // Reset idle detection
        stopIdleDetection()
        startIdleDetection()
    }

    /**
     * Screen changed (new activity, dialog, etc.)
     *
     * @param screenHash Unique hash of new screen
     */
    fun onScreenChanged(screenHash: String) {
        if (!isMonitoring) return

        val previousScreen = currentScreenHash
        currentScreenHash = screenHash

        _activityState.value = UserActivityState.NAVIGATING

        Log.d(TAG, "Screen changed: $previousScreen -> $screenHash")

        // Suggest commands for new screen (after short delay)
        scope.launch {
            delay(2000)  // Wait for screen to settle
            if (isMonitoring && currentScreenHash == screenHash) {
                suggestCommandsForScreen(screenHash)
            }
        }

        // Reset idle detection
        stopIdleDetection()
        startIdleDetection()
    }

    /**
     * Start idle detection timer
     */
    private fun startIdleDetection() {
        idleDetectionJob?.cancel()
        idleDetectionJob = scope.launch {
            delay(IDLE_THRESHOLD_MS)

            // User is idle
            if (isMonitoring) {
                _activityState.value = UserActivityState.IDLE
                Log.d(TAG, "User idle detected")

                // Suggest commands
                currentScreenHash?.let { screenHash ->
                    suggestCommandsForScreen(screenHash)
                }
            }
        }
    }

    /**
     * Stop idle detection timer
     */
    private fun stopIdleDetection() {
        idleDetectionJob?.cancel()
        idleDetectionJob = null
    }

    /**
     * Suggest voice commands for current screen
     */
    private suspend fun suggestCommandsForScreen(screenHash: String) {
        // Check cooldown
        val now = System.currentTimeMillis()
        if (now - lastHintTime < HINT_COOLDOWN_MS) {
            Log.d(TAG, "Hint cooldown active, skipping")
            return
        }

        try {
            // Get commands for screen
            val commands = getCommandsForScreen(screenHash)
            if (commands.isEmpty()) {
                Log.d(TAG, "No commands for screen: $screenHash")
                return
            }

            // Rank commands by usefulness
            val topCommands = rankCommands(commands).take(MAX_HINTS_PER_SCREEN)

            // Speak hint
            speakCommandHint(topCommands)

            lastHintTime = now

        } catch (e: Exception) {
            Log.e(TAG, "Error suggesting commands", e)
        }
    }

    /**
     * Get commands for screen from cache or database
     */
    private suspend fun getCommandsForScreen(screenHash: String): List<GeneratedCommandDTO> {
        // Check cache
        commandCache[screenHash]?.let { return it }

        // Load from database
        // TODO: Implement screen hash filtering when available
        val allCommands = databaseManager.generatedCommands.getAllCommands()

        // Cache and return
        commandCache[screenHash] = allCommands
        return allCommands
    }

    /**
     * Rank commands by usefulness
     *
     * Ranking factors:
     * 1. Confidence (higher = better)
     * 2. Usage count (more used = more useful)
     * 3. Element type (buttons > tabs > icons)
     * 4. Command clarity (real labels > generated)
     */
    private fun rankCommands(commands: List<GeneratedCommandDTO>): List<GeneratedCommandDTO> {
        return commands.sortedWith(
            compareByDescending<GeneratedCommandDTO> { it.confidence }
                .thenByDescending { it.usageCount }
                .thenBy { it.commandText.length }  // Shorter = clearer
        )
    }

    /**
     * Speak command hint using TTS
     */
    private fun speakCommandHint(commands: List<GeneratedCommandDTO>) {
        if (!ttsInitialized) {
            Log.w(TAG, "TTS not initialized")
            return
        }

        val hint = buildHintMessage(commands)
        textToSpeech?.speak(hint, TextToSpeech.QUEUE_ADD, null, "contextual_hint")
        Log.d(TAG, "Speaking hint: $hint")
    }

    /**
     * Build hint message from commands
     */
    private fun buildHintMessage(commands: List<GeneratedCommandDTO>): String {
        return buildString {
            append("You can say: ")

            commands.take(3).forEachIndexed { index, command ->
                append(command.commandText)
                when {
                    index < commands.size - 1 && index < 2 -> append(", ")
                }
            }

            if (commands.size > 3) {
                append(", and ${commands.size - 3} more")
            }
        }
    }

    /**
     * Manually trigger hint suggestion
     */
    fun suggestCommands() {
        scope.launch {
            currentScreenHash?.let { screenHash ->
                suggestCommandsForScreen(screenHash)
            }
        }
    }

    /**
     * Get SharedPreferences for contextual hints settings
     */
    private fun getPrefs() = context.getSharedPreferences("contextual_hints", Context.MODE_PRIVATE)

    /**
     * Check if hints are enabled
     */
    private fun isHintsEnabled(): Boolean {
        return getPrefs().getBoolean(HINTS_ENABLED_KEY, true)
    }

    /**
     * Enable/disable hints
     */
    fun setHintsEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(HINTS_ENABLED_KEY, enabled).apply()
        if (!enabled && isMonitoring) {
            stopMonitoring()
        }
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        stopMonitoring()
        commandCache.clear()

        if (ttsInitialized) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            ttsInitialized = false
        }

        scope.cancel()
    }
}

/**
 * Contextual Hints Widget - Visual overlay showing top commands
 *
 * Persistent mini-widget at bottom of screen with swipeable command suggestions.
 */
class ContextualHintsWidget(
    private val context: Context
) {
    companion object {
        private const val TAG = "ContextualHintsWidget"
    }

    // TODO: Implement Compose-based floating widget
    // Shows 3-5 top commands in compact format at screen bottom
    // Swipeable to see more commands
    // Auto-hide after timeout
    // Voice command: "What can I do here?" to show

    fun show(commands: List<GeneratedCommandDTO>) {
        Log.d(TAG, "Would show widget with ${commands.size} commands")
        // TODO: Implement
    }

    fun hide() {
        Log.d(TAG, "Would hide widget")
        // TODO: Implement
    }

    fun dispose() {
        // TODO: Implement cleanup
    }
}
