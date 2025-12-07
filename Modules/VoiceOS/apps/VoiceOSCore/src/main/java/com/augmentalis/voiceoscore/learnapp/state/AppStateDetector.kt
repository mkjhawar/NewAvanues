/**
 * AppStateDetector.kt - Detects application states during exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-09
 *
 * Analyzes UI patterns using AccessibilityNodeInfo to detect common app states
 * such as login screens, loading indicators, error messages, and ready states.
 * Uses pattern recognition with confidence scoring for robust detection.
 */
package com.augmentalis.voiceoscore.learnapp.state

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Possible states an app can be in during exploration
 */
enum class AppState {
    UNKNOWN,          // Unable to determine state
    LOGIN,            // Login or authentication screen
    LOADING,          // Loading or splash screen
    ERROR,            // Error state or crash
    READY,            // Normal operational state
    PERMISSION,       // Permission request dialog
    TUTORIAL,         // Onboarding or tutorial
    EMPTY_STATE,      // Empty state (no content)
    DIALOG,           // Modal dialog present
    BACKGROUND        // App in background
}

/**
 * State detection result with confidence scoring
 */
data class StateDetectionResult(
    val state: AppState,
    val confidence: Float,  // 0.0 to 1.0
    val indicators: List<String>,  // What indicated this state
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if detection confidence is high enough to act on
     */
    fun isConfident(threshold: Float = 0.7f): Boolean {
        return confidence >= threshold
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        val confidencePercent = (confidence * 100).toInt()
        return "$state ($confidencePercent% confident): ${indicators.joinToString(", ")}"
    }
}

/**
 * State transition event
 */
data class StateTransition(
    val fromState: AppState,
    val toState: AppState,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Configuration for state detection behavior
 */
data class StateDetectorConfig(
    val enableMLPatterns: Boolean = false,  // Use ML models (future)
    val confidenceThreshold: Float = 0.7f,
    val enableTransitionCallbacks: Boolean = true,
    val logDetections: Boolean = true
)

/**
 * Detects application states through UI pattern analysis
 *
 * This class analyzes accessibility node trees to identify common UI patterns
 * associated with different app states. Uses heuristic pattern matching with
 * confidence scoring. Can be extended with ML models for improved accuracy.
 *
 * @param config Configuration for detection behavior
 */
class AppStateDetector(
    private val config: StateDetectorConfig = StateDetectorConfig()
) {

    companion object {
        private const val TAG = "AppStateDetector"

        // Pattern keywords for state detection
        private val LOGIN_KEYWORDS = setOf(
            "login", "sign in", "log in", "signin", "username", "password",
            "email", "authenticate", "sign up", "register", "create account"
        )

        private val LOADING_KEYWORDS = setOf(
            "loading", "please wait", "processing", "refreshing",
            "fetching", "syncing", "updating"
        )

        private val ERROR_KEYWORDS = setOf(
            "error", "failed", "failure", "problem", "issue", "couldn't",
            "unable", "cannot", "retry", "try again", "oops"
        )

        private val PERMISSION_KEYWORDS = setOf(
            "permission", "allow", "deny", "access", "authorize",
            "grant", "enable", "location", "camera", "microphone", "storage"
        )

        private val TUTORIAL_KEYWORDS = setOf(
            "welcome", "tutorial", "getting started", "onboarding",
            "skip", "next", "learn", "guide", "walkthrough"
        )

        private val EMPTY_STATE_KEYWORDS = setOf(
            "no items", "nothing here", "empty", "no results", "no data",
            "no content", "get started", "add your first"
        )

        private val DIALOG_KEYWORDS = setOf(
            "ok", "cancel", "yes", "no", "confirm", "dismiss", "close"
        )
    }

    // Current detected state
    private val _currentState = MutableStateFlow(
        StateDetectionResult(AppState.UNKNOWN, 0.0f, emptyList())
    )
    val currentState: StateFlow<StateDetectionResult> = _currentState.asStateFlow()

    // State transition history
    private val _transitions = MutableStateFlow<List<StateTransition>>(emptyList())
    val transitions: StateFlow<List<StateTransition>> = _transitions.asStateFlow()

    // Previous state for transition detection
    private var previousState: AppState = AppState.UNKNOWN

    /**
     * Analyze an accessibility node tree and detect app state
     *
     * @param rootNode Root of accessibility node tree
     * @return Detection result with confidence score
     */
    fun detectState(rootNode: AccessibilityNodeInfo?): StateDetectionResult {
        if (rootNode == null) {
            return createResult(AppState.UNKNOWN, 0.0f, listOf("Null root node"))
        }

        // Collect all text and hints from node tree
        val textContent = mutableListOf<String>()
        val viewIdResources = mutableListOf<String>()
        val classNames = mutableListOf<String>()

        traverseNodeTree(rootNode, textContent, viewIdResources, classNames)

        if (config.logDetections) {
            Log.d(TAG, "Analyzing ${textContent.size} text elements, " +
                    "${viewIdResources.size} IDs, ${classNames.size} classes")
        }

        // Run pattern detection for each possible state
        val detections = listOf(
            detectLoginState(textContent, viewIdResources, classNames),
            detectLoadingState(textContent, viewIdResources, classNames),
            detectErrorState(textContent, viewIdResources, classNames),
            detectPermissionState(textContent, viewIdResources, classNames),
            detectTutorialState(textContent, viewIdResources, classNames),
            detectEmptyState(textContent, viewIdResources, classNames),
            detectDialogState(textContent, viewIdResources, classNames)
        )

        // Find detection with highest confidence
        val bestDetection = detections.maxByOrNull { it.confidence }
            ?: createResult(AppState.UNKNOWN, 0.0f, listOf("No patterns matched"))

        // If no high-confidence detection, assume READY state
        val finalResult = if (bestDetection.confidence < config.confidenceThreshold) {
            createResult(AppState.READY, 0.6f, listOf("Default state"))
        } else {
            bestDetection
        }

        updateState(finalResult)
        return finalResult
    }

    /**
     * Detect login screen patterns
     */
    @Suppress("UNUSED_PARAMETER")
    private fun detectLoginState(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Check text content
        val loginTextMatches = textContent.count { text ->
            LOGIN_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
        if (loginTextMatches > 0) {
            score += 0.3f
            indicators.add("$loginTextMatches login keywords in text")
        }

        // Check for EditText fields (username/password inputs)
        val editTextCount = classNames.count { it.contains("EditText") }
        if (editTextCount >= 2) {
            score += 0.4f
            indicators.add("$editTextCount input fields")
        }

        // Check for Button with login-related text
        val hasLoginButton = textContent.any { text ->
            (text.contains("login", ignoreCase = true) ||
             text.contains("sign in", ignoreCase = true)) &&
            classNames.any { it.contains("Button") }
        }
        if (hasLoginButton) {
            score += 0.3f
            indicators.add("Login button detected")
        }

        return createResult(AppState.LOGIN, score.coerceAtMost(1.0f), indicators)
    }

    /**
     * Detect loading state patterns
     */
    @Suppress("UNUSED_PARAMETER")
    private fun detectLoadingState(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Check for ProgressBar
        val hasProgressBar = classNames.any { it.contains("ProgressBar") }
        if (hasProgressBar) {
            score += 0.5f
            indicators.add("Progress indicator present")
        }

        // Check for loading text
        val loadingTextMatches = textContent.count { text ->
            LOADING_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
        if (loadingTextMatches > 0) {
            score += 0.4f
            indicators.add("Loading text: $loadingTextMatches matches")
        }

        // Minimal content suggests loading
        if (textContent.size < 5 && hasProgressBar) {
            score += 0.2f
            indicators.add("Minimal content with progress")
        }

        return createResult(AppState.LOADING, score.coerceAtMost(1.0f), indicators)
    }

    /**
     * Detect error state patterns
     */
    @Suppress("UNUSED_PARAMETER")
    private fun detectErrorState(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Check for error keywords
        val errorTextMatches = textContent.count { text ->
            ERROR_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
        if (errorTextMatches > 0) {
            score += 0.6f
            indicators.add("$errorTextMatches error keywords")
        }

        // Check for retry button
        val hasRetryButton = textContent.any { text ->
            (text.contains("retry", ignoreCase = true) ||
             text.contains("try again", ignoreCase = true)) &&
            classNames.any { it.contains("Button") }
        }
        if (hasRetryButton) {
            score += 0.3f
            indicators.add("Retry button present")
        }

        return createResult(AppState.ERROR, score.coerceAtMost(1.0f), indicators)
    }

    /**
     * Detect permission request patterns
     */
    @Suppress("UNUSED_PARAMETER")
    private fun detectPermissionState(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Check for permission keywords
        val permissionMatches = textContent.count { text ->
            PERMISSION_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
        if (permissionMatches > 0) {
            score += 0.5f
            indicators.add("$permissionMatches permission keywords")
        }

        // Check for Allow/Deny buttons
        val hasAllowDenyButtons = textContent.any { it.contains("allow", ignoreCase = true) } &&
                                   textContent.any { it.contains("deny", ignoreCase = true) }
        if (hasAllowDenyButtons) {
            score += 0.4f
            indicators.add("Allow/Deny buttons present")
        }

        return createResult(AppState.PERMISSION, score.coerceAtMost(1.0f), indicators)
    }

    /**
     * Detect tutorial/onboarding patterns
     */
    @Suppress("UNUSED_PARAMETER")
    private fun detectTutorialState(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Check for tutorial keywords
        val tutorialMatches = textContent.count { text ->
            TUTORIAL_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
        if (tutorialMatches > 0) {
            score += 0.5f
            indicators.add("$tutorialMatches tutorial keywords")
        }

        // Check for Skip/Next buttons
        val hasNavigationButtons = textContent.any { it.contains("skip", ignoreCase = true) } ||
                                    textContent.any { it.contains("next", ignoreCase = true) }
        if (hasNavigationButtons) {
            score += 0.3f
            indicators.add("Navigation buttons present")
        }

        return createResult(AppState.TUTORIAL, score.coerceAtMost(1.0f), indicators)
    }

    /**
     * Detect empty state patterns
     */
    @Suppress("UNUSED_PARAMETER")
    private fun detectEmptyState(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Check for empty state keywords
        val emptyMatches = textContent.count { text ->
            EMPTY_STATE_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
        if (emptyMatches > 0) {
            score += 0.6f
            indicators.add("$emptyMatches empty state keywords")
        }

        return createResult(AppState.EMPTY_STATE, score.coerceAtMost(1.0f), indicators)
    }

    /**
     * Detect dialog patterns
     */
    @Suppress("UNUSED_PARAMETER")
    private fun detectDialogState(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Check for dialog class
        val hasDialogClass = classNames.any { it.contains("Dialog", ignoreCase = true) }
        if (hasDialogClass) {
            score += 0.5f
            indicators.add("Dialog class present")
        }

        // Check for dialog buttons
        val dialogButtonCount = textContent.count { text ->
            DIALOG_KEYWORDS.any { keyword -> text.equals(keyword, ignoreCase = true) }
        }
        if (dialogButtonCount >= 2) {
            score += 0.4f
            indicators.add("$dialogButtonCount dialog buttons")
        }

        return createResult(AppState.DIALOG, score.coerceAtMost(1.0f), indicators)
    }

    /**
     * Recursively traverse accessibility node tree
     */
    private fun traverseNodeTree(
        node: AccessibilityNodeInfo,
        textContent: MutableList<String>,
        viewIdResources: MutableList<String>,
        classNames: MutableList<String>
    ) {
        // Collect text content
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { textContent.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { textContent.add(it) }
        node.hintText?.toString()?.takeIf { it.isNotBlank() }?.let { textContent.add(it) }

        // Collect view ID
        node.viewIdResourceName?.let { viewIdResources.add(it) }

        // Collect class name
        node.className?.toString()?.let { classNames.add(it) }

        // Traverse children
        for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    traverseNodeTree(child, textContent, viewIdResources, classNames)
                    // child.recycle() // Deprecated - Android handles this automatically
                }
        }
    }

    /**
     * Create state detection result
     */
    private fun createResult(
        state: AppState,
        confidence: Float,
        indicators: List<String>
    ): StateDetectionResult {
        return StateDetectionResult(state, confidence, indicators)
    }

    /**
     * Update current state and emit transitions
     */
    private fun updateState(result: StateDetectionResult) {
        if (config.enableTransitionCallbacks && result.state != previousState) {
            val transition = StateTransition(
                fromState = previousState,
                toState = result.state,
                confidence = result.confidence
            )
            _transitions.value = _transitions.value + transition

            if (config.logDetections) {
                Log.i(TAG, "State transition: ${transition.fromState} -> ${transition.toState} " +
                        "(${(transition.confidence * 100).toInt()}%)")
            }
        }

        _currentState.value = result
        previousState = result.state

        if (config.logDetections) {
            Log.d(TAG, result.getDescription())
        }
    }

    /**
     * Reset detector state
     */
    fun reset() {
        _currentState.value = StateDetectionResult(AppState.UNKNOWN, 0.0f, emptyList())
        _transitions.value = emptyList()
        previousState = AppState.UNKNOWN
        Log.d(TAG, "Detector state reset")
    }

    /**
     * Get state transition history
     */
    fun getTransitionHistory(): List<StateTransition> {
        return _transitions.value
    }

    /**
     * Get current state synchronously
     */
    fun getCurrentState(): StateDetectionResult {
        return _currentState.value
    }
}
