/**
 * OverlayStateManager.kt - Manages overlay-related state for command overlay service
 *
 * Singleton object: overlay state is shared across accessibility service and overlay service.
 * KMP-compatible — StateFlows work identically on all platforms.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "OverlayStateManager"

/**
 * Manages overlay-related state for the command overlay service.
 *
 * Tracks numbered badge items, overlay modes, badge themes,
 * and per-app number preferences.
 *
 * Uses the canonical [NumbersOverlayMode] enum from NumbersOverlayHandler
 * (ON/OFF/AUTO) — no duplicate enums.
 */
object OverlayStateManager {

    // ===== StateFlows =====

    private val _numberedOverlayItems = MutableStateFlow<List<NumberOverlayItem>>(emptyList())
    val numberedOverlayItems: StateFlow<List<NumberOverlayItem>> = _numberedOverlayItems.asStateFlow()

    private val _numbersOverlayMode = MutableStateFlow(NumbersOverlayMode.AUTO)
    val numbersOverlayMode: StateFlow<NumbersOverlayMode> = _numbersOverlayMode.asStateFlow()

    private val _showNumbersOverlayComputed = MutableStateFlow(false)
    val showNumbersOverlayComputed: StateFlow<Boolean> = _showNumbersOverlayComputed.asStateFlow()

    private val _instructionBarMode = MutableStateFlow(InstructionBarMode.AUTO)
    val instructionBarMode: StateFlow<InstructionBarMode> = _instructionBarMode.asStateFlow()

    private val _badgeTheme = MutableStateFlow(BadgeTheme.GREEN)
    val badgeTheme: StateFlow<BadgeTheme> = _badgeTheme.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _showAppDetectionDialog = MutableStateFlow<String?>(null)
    val showAppDetectionDialog: StateFlow<String?> = _showAppDetectionDialog.asStateFlow()

    private val _currentDetectedAppName = MutableStateFlow<String?>(null)
    val currentDetectedAppName: StateFlow<String?> = _currentDetectedAppName.asStateFlow()

    // ===== Callback interface for preference storage =====

    var preferenceCallback: PreferenceCallback? = null

    // ===== Numbers Overlay Methods =====

    fun setNumbersOverlayMode(mode: NumbersOverlayMode) {
        _numbersOverlayMode.value = mode
        updateNumbersOverlayVisibility()
        LoggingUtils.d("Numbers overlay mode: $mode", TAG)
    }

    fun cycleNumbersOverlayMode() {
        val newMode = when (_numbersOverlayMode.value) {
            NumbersOverlayMode.OFF -> NumbersOverlayMode.AUTO
            NumbersOverlayMode.AUTO -> NumbersOverlayMode.ON
            NumbersOverlayMode.ON -> NumbersOverlayMode.OFF
        }
        setNumbersOverlayMode(newMode)
    }

    fun updateNumbersOverlayVisibility() {
        val mode = _numbersOverlayMode.value
        val hasItems = _numberedOverlayItems.value.isNotEmpty()

        val shouldShow = when (mode) {
            NumbersOverlayMode.ON -> true
            NumbersOverlayMode.OFF -> false
            NumbersOverlayMode.AUTO -> hasItems
        }

        _showNumbersOverlayComputed.value = shouldShow
        LoggingUtils.d("Numbers overlay: mode=$mode, hasItems=$hasItems, showing=$shouldShow", TAG)
    }

    fun updateNumberedOverlayItems(items: List<NumberOverlayItem>) {
        _numberedOverlayItems.value = items
        updateNumbersOverlayVisibility()
        if (items.isNotEmpty()) {
            LoggingUtils.d("Numbered overlay: ${items.size} items for voice selection", TAG)
        }
    }

    fun clearOverlayItems() {
        if (_numberedOverlayItems.value.isNotEmpty()) {
            LoggingUtils.d("Clearing ${_numberedOverlayItems.value.size} overlay items", TAG)
            _numberedOverlayItems.value = emptyList()
            updateNumbersOverlayVisibility()
        }
    }

    // ===== Instruction Bar =====

    fun setInstructionBarMode(mode: InstructionBarMode) {
        _instructionBarMode.value = mode
        LoggingUtils.d("Instruction bar mode: $mode", TAG)
    }

    // ===== Feedback Toast =====

    /**
     * Show a transient feedback message to the user.
     *
     * Used by VoiceControlCallbacks to confirm voice control actions
     * (e.g., "Voice muted", "Dictation started"). The overlay service
     * auto-dismisses after a brief delay.
     */
    fun showFeedback(message: String) {
        _feedbackMessage.value = message
        LoggingUtils.d("Feedback: $message", TAG)
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    // ===== Badge Theme =====

    fun setBadgeTheme(theme: BadgeTheme) {
        _badgeTheme.value = theme
        LoggingUtils.d("Badge theme: $theme", TAG)
    }

    fun cycleBadgeTheme() {
        val themes = BadgeTheme.entries
        val currentIndex = themes.indexOf(_badgeTheme.value)
        val nextIndex = (currentIndex + 1) % themes.size
        setBadgeTheme(themes[nextIndex])
    }

    // ===== App Detection Dialog =====

    fun showAppDetectionDialogFor(packageName: String, appName: String) {
        _currentDetectedAppName.value = appName
        _showAppDetectionDialog.value = packageName
        LoggingUtils.d("Showing app detection dialog for: $appName ($packageName)", TAG)
    }

    fun dismissAppDetectionDialog() {
        _showAppDetectionDialog.value = null
        _currentDetectedAppName.value = null
    }

    fun handleAppDetectionResponse(
        packageName: String,
        preference: AppNumbersPreference,
        onExplore: (() -> Unit)? = null
    ) {
        preferenceCallback?.saveAppNumbersPreference(packageName, preference)
        dismissAppDetectionDialog()

        when (preference) {
            AppNumbersPreference.ALWAYS -> setNumbersOverlayMode(NumbersOverlayMode.ON)
            AppNumbersPreference.AUTO -> setNumbersOverlayMode(NumbersOverlayMode.AUTO)
            AppNumbersPreference.NEVER -> setNumbersOverlayMode(NumbersOverlayMode.OFF)
            AppNumbersPreference.ASK -> { /* No change */ }
        }
        LoggingUtils.d("App detection response for $packageName: $preference", TAG)

        if (preference != AppNumbersPreference.NEVER) {
            onExplore?.invoke()
        }
    }
}
