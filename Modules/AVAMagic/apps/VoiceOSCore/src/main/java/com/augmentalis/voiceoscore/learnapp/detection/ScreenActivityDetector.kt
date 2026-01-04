/**
 * ScreenActivityDetector.kt - Intelligent screen activity detection for LearnApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Detects screen activity, blocked states (login screens, permission dialogs),
 * and provides rename functionality integration for the exploration engine.
 *
 * Features:
 * - Login screen detection via keyword analysis
 * - Permission dialog detection
 * - Auto-pause triggers for exploration mode
 * - Integration with RenameHintOverlay for element labeling
 * - Database integration for persisting screen activity data
 */
package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.integration.BlockedState
import com.augmentalis.voiceoscore.learnapp.ui.RenameHintOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Screen Activity Detector
 *
 * Intelligent detector that analyzes accessibility node trees to identify:
 * - Login screens requiring user authentication
 * - Permission dialogs requiring user action
 * - Screen changes for exploration coordination
 *
 * @param context Application context
 * @param databaseManager VoiceOS database manager for persisting screen data
 * @param renameHintOverlay Overlay for showing rename hints on detected elements
 */
class ScreenActivityDetector(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager,
    private val renameHintOverlay: RenameHintOverlay
) {
    companion object {
        private const val TAG = "ScreenActivityDetector"

        // Login detection keywords (weighted by specificity)
        private val LOGIN_KEYWORDS_HIGH = listOf(
            "sign in", "login", "log in", "sign into"
        )
        private val LOGIN_KEYWORDS_MEDIUM = listOf(
            "username", "password", "email address", "phone number",
            "continue with google", "continue with facebook", "continue with apple",
            "create account", "sign up", "register", "forgot password"
        )
        private val LOGIN_KEYWORDS_LOW = listOf(
            "email", "phone", "remember me", "keep me signed in"
        )

        // Permission dialog keywords
        private val PERMISSION_KEYWORDS_HIGH = listOf(
            "allow", "don't allow", "deny", "while using the app",
            "only this time", "ask every time"
        )
        private val PERMISSION_KEYWORDS_MEDIUM = listOf(
            "permission", "access to", "wants to access",
            "allow access", "grant permission"
        )

        // Detection thresholds
        private const val LOGIN_THRESHOLD_HIGH = 1
        private const val LOGIN_THRESHOLD_COMBINED = 3
        private const val PERMISSION_THRESHOLD = 2

        // Debounce interval for screen analysis (ms)
        private const val ANALYSIS_DEBOUNCE_MS = 500L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State management
    private val _blockedState = MutableStateFlow<BlockedState?>(null)
    val blockedState: StateFlow<BlockedState?> = _blockedState.asStateFlow()

    private val _isScreenActive = MutableStateFlow(true)
    val isScreenActive: StateFlow<Boolean> = _isScreenActive.asStateFlow()

    private val _currentPackage = MutableStateFlow<String?>(null)
    val currentPackage: StateFlow<String?> = _currentPackage.asStateFlow()

    private val _currentActivity = MutableStateFlow<String?>(null)
    val currentActivity: StateFlow<String?> = _currentActivity.asStateFlow()

    // Events
    private val _screenChangeEvents = MutableSharedFlow<ScreenChangeEvent>(replay = 1)
    val screenChangeEvents: SharedFlow<ScreenChangeEvent> = _screenChangeEvents.asSharedFlow()

    // Internal state
    private val isInitialized = AtomicBoolean(false)
    private val lastAnalysisTime = AtomicLong(0)
    private val screenTextCache = ConcurrentHashMap<String, CachedScreenAnalysis>()

    // Statistics
    private var totalAnalyses = 0L
    private var loginDetections = 0L
    private var permissionDetections = 0L
    private var cacheHits = 0L

    /**
     * Initialize the detector.
     */
    fun initialize() {
        if (isInitialized.getAndSet(true)) {
            Log.d(TAG, "Already initialized")
            return
        }
        Log.i(TAG, "ScreenActivityDetector initialized")
    }

    /**
     * Analyze the current screen for blocked states.
     *
     * @param rootNode The root accessibility node of the current screen
     * @param packageName Current package name
     * @param activityName Current activity name (optional)
     * @return The detected blocked state, or null if none
     */
    fun analyzeScreen(
        rootNode: AccessibilityNodeInfo?,
        packageName: String? = null,
        activityName: String? = null
    ): BlockedState? {
        if (rootNode == null) {
            Log.v(TAG, "Root node is null, skipping analysis")
            return null
        }

        // Debounce rapid calls
        val now = System.currentTimeMillis()
        val lastTime = lastAnalysisTime.get()
        if (now - lastTime < ANALYSIS_DEBOUNCE_MS) {
            return _blockedState.value
        }
        lastAnalysisTime.set(now)

        totalAnalyses++

        // Update package/activity tracking
        packageName?.let { _currentPackage.value = it }
        activityName?.let { _currentActivity.value = it }

        // Check cache first
        val cacheKey = "${packageName ?: "unknown"}_${activityName ?: "unknown"}"
        val cached = screenTextCache[cacheKey]
        if (cached != null && now - cached.timestamp < 5000) {
            cacheHits++
            return cached.blockedState
        }

        // Extract and analyze screen text
        val screenText = extractScreenText(rootNode).lowercase()
        val analysis = analyzeText(screenText)

        // Cache result
        screenTextCache[cacheKey] = CachedScreenAnalysis(
            timestamp = now,
            blockedState = analysis,
            screenText = screenText.take(500) // Limit cached text size
        )

        // Clean old cache entries
        if (screenTextCache.size > 20) {
            val cutoff = now - 30000
            screenTextCache.entries.removeIf { it.value.timestamp < cutoff }
        }

        // Update state
        _blockedState.value = analysis

        // Emit screen change event
        scope.launch {
            _screenChangeEvents.emit(ScreenChangeEvent(
                packageName = packageName,
                activityName = activityName,
                blockedState = analysis,
                timestamp = now
            ))
        }

        if (analysis != null) {
            when (analysis) {
                BlockedState.LOGIN_REQUIRED -> {
                    loginDetections++
                    Log.d(TAG, "Login screen detected in $packageName")
                }
                BlockedState.PERMISSION_REQUIRED -> {
                    permissionDetections++
                    Log.d(TAG, "Permission dialog detected in $packageName")
                }
            }
        }

        return analysis
    }

    /**
     * Analyze screen text for blocked states.
     */
    private fun analyzeText(screenText: String): BlockedState? {
        // Check for permission dialog first (higher priority)
        val permissionScoreHigh = PERMISSION_KEYWORDS_HIGH.count { screenText.contains(it) }
        val permissionScoreMedium = PERMISSION_KEYWORDS_MEDIUM.count { screenText.contains(it) }

        if (permissionScoreHigh >= 1 || (permissionScoreHigh + permissionScoreMedium) >= PERMISSION_THRESHOLD) {
            return BlockedState.PERMISSION_REQUIRED
        }

        // Check for login screen
        val loginScoreHigh = LOGIN_KEYWORDS_HIGH.count { screenText.contains(it) }
        val loginScoreMedium = LOGIN_KEYWORDS_MEDIUM.count { screenText.contains(it) }
        val loginScoreLow = LOGIN_KEYWORDS_LOW.count { screenText.contains(it) }

        // High confidence login keywords
        if (loginScoreHigh >= LOGIN_THRESHOLD_HIGH) {
            return BlockedState.LOGIN_REQUIRED
        }

        // Combined score check
        val combinedScore = (loginScoreHigh * 3) + (loginScoreMedium * 2) + loginScoreLow
        if (combinedScore >= LOGIN_THRESHOLD_COMBINED) {
            return BlockedState.LOGIN_REQUIRED
        }

        return null
    }

    /**
     * Extract text content from an accessibility node tree.
     */
    private fun extractScreenText(node: AccessibilityNodeInfo): String {
        val builder = StringBuilder()
        extractTextRecursive(node, builder, 0)
        return builder.toString()
    }

    private fun extractTextRecursive(
        node: AccessibilityNodeInfo,
        builder: StringBuilder,
        depth: Int
    ) {
        // Limit recursion depth to prevent stack overflow
        if (depth > 30) return

        // Extract text content
        node.text?.let { text ->
            if (text.isNotBlank()) {
                builder.append(text).append(" ")
            }
        }

        // Extract content description
        node.contentDescription?.let { desc ->
            if (desc.isNotBlank()) {
                builder.append(desc).append(" ")
            }
        }

        // Extract hint text if available (API 26+)
        try {
            node.hintText?.let { hint ->
                if (hint.isNotBlank()) {
                    builder.append(hint).append(" ")
                }
            }
        } catch (e: NoSuchMethodError) {
            // Older API, ignore
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            try {
                node.getChild(i)?.let { child ->
                    extractTextRecursive(child, builder, depth + 1)
                    child.recycle()
                }
            } catch (e: Exception) {
                // Child may have become stale, continue with others
            }
        }
    }

    /**
     * Show rename hint for an element.
     *
     * @param elementId Element identifier
     * @param currentLabel Current label of the element
     * @param bounds Element bounds on screen
     */
    fun showRenameHint(
        elementId: String,
        currentLabel: String,
        bounds: android.graphics.Rect
    ) {
        try {
            renameHintOverlay.show(currentLabel)
            Log.d(TAG, "Showing rename hint for element: $elementId with label: $currentLabel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show rename hint", e)
        }
    }

    /**
     * Hide the rename hint overlay.
     */
    fun hideRenameHint() {
        try {
            renameHintOverlay.hide()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide rename hint", e)
        }
    }

    /**
     * Handle a window state change event.
     *
     * @param packageName New package name
     * @param className New class/activity name
     */
    fun onWindowStateChanged(packageName: String?, className: String?) {
        _currentPackage.value = packageName
        _currentActivity.value = className

        // Reset blocked state on window change
        _blockedState.value = null

        Log.v(TAG, "Window changed: $packageName / $className")
    }

    /**
     * Reset the blocked state.
     */
    fun resetBlockedState() {
        _blockedState.value = null
        Log.d(TAG, "Blocked state reset")
    }

    /**
     * Update screen active state.
     */
    fun setScreenActive(active: Boolean) {
        _isScreenActive.value = active
        Log.d(TAG, "Screen active: $active")
    }

    /**
     * Get detection statistics.
     */
    fun getStats(): DetectionStats {
        return DetectionStats(
            totalAnalyses = totalAnalyses,
            loginDetections = loginDetections,
            permissionDetections = permissionDetections,
            cacheHits = cacheHits,
            cacheSize = screenTextCache.size
        )
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        scope.cancel()
        screenTextCache.clear()
        isInitialized.set(false)
        Log.d(TAG, "ScreenActivityDetector cleaned up")
    }

    /**
     * Screen change event data class.
     */
    data class ScreenChangeEvent(
        val packageName: String?,
        val activityName: String?,
        val blockedState: BlockedState?,
        val timestamp: Long
    )

    /**
     * Cached screen analysis result.
     */
    private data class CachedScreenAnalysis(
        val timestamp: Long,
        val blockedState: BlockedState?,
        val screenText: String
    )

    /**
     * Detection statistics.
     */
    data class DetectionStats(
        val totalAnalyses: Long,
        val loginDetections: Long,
        val permissionDetections: Long,
        val cacheHits: Long,
        val cacheSize: Int
    )
}
