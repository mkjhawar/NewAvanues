/**
 * OverlayStateManager.kt - Manages overlay-related state for command overlay service
 *
 * Migrated from VoiceOS OverlayService to Avanues consolidated app.
 * Singleton object: overlay state is shared across accessibility service and overlay service.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "OverlayStateManager"

/**
 * Manages overlay-related state for the command overlay service.
 *
 * Tracks numbered badge items, overlay modes, badge themes,
 * and per-app number preferences.
 */
object OverlayStateManager {

    // ===== Enums =====

    /**
     * Numbers overlay mode:
     * - ON: Always show numbers on all clickable elements
     * - OFF: Never show numbers
     * - AUTO: Show numbers only when there are list items
     */
    enum class NumbersOverlayMode {
        ON, OFF, AUTO
    }

    /**
     * Instruction bar mode:
     * - ON: Always show instruction bar
     * - OFF: Never show instruction bar
     * - AUTO: Show briefly then fade out (default)
     */
    enum class InstructionBarMode {
        ON, OFF, AUTO
    }

    /**
     * Badge color themes for numbered badges.
     */
    enum class BadgeTheme(val backgroundColor: Long, val textColor: Long) {
        GREEN(0xFF4CAF50, 0xFFFFFFFF),
        BLUE(0xFF2196F3, 0xFFFFFFFF),
        PURPLE(0xFF9C27B0, 0xFFFFFFFF),
        ORANGE(0xFFFF9800, 0xFF000000),
        RED(0xFFF44336, 0xFFFFFFFF),
        TEAL(0xFF009688, 0xFFFFFFFF),
        PINK(0xFFE91E63, 0xFFFFFFFF)
    }

    /**
     * Per-app numbers mode preference.
     */
    enum class AppNumbersPreference {
        ASK, ALWAYS, AUTO, NEVER
    }

    /**
     * Data for displaying numbered badges on screen elements (Layer 2).
     */
    data class NumberOverlayItem(
        val number: Int,
        val label: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val avid: String
    )

    /**
     * Data for displaying text labels under icon-only elements (Layer 1).
     * Always visible regardless of numbers overlay mode.
     */
    data class IconLabelItem(
        val label: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val avid: String
    )

    // ===== Target Apps =====

    val TARGET_APPS = setOf(
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.samsung.android.email.provider",
        "com.google.android.apps.messaging",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "com.Slack",
        "com.twitter.android",
        "com.instagram.android",
        "com.facebook.katana",
        "com.linkedin.android",
        "com.todoist",
        "com.google.android.keep",
        "com.microsoft.todos",
        "com.amazon.mShop.android.shopping"
    )

    // ===== StateFlows =====

    private val _numberedOverlayItems = MutableStateFlow<List<NumberOverlayItem>>(emptyList())
    val numberedOverlayItems: StateFlow<List<NumberOverlayItem>> = _numberedOverlayItems.asStateFlow()

    private val _iconLabelItems = MutableStateFlow<List<IconLabelItem>>(emptyList())
    val iconLabelItems: StateFlow<List<IconLabelItem>> = _iconLabelItems.asStateFlow()

    private val _numbersOverlayMode = MutableStateFlow(NumbersOverlayMode.AUTO)
    val numbersOverlayMode: StateFlow<NumbersOverlayMode> = _numbersOverlayMode.asStateFlow()

    private val _showNumbersOverlayComputed = MutableStateFlow(false)
    val showNumbersOverlayComputed: StateFlow<Boolean> = _showNumbersOverlayComputed.asStateFlow()

    private val _instructionBarMode = MutableStateFlow(InstructionBarMode.AUTO)
    val instructionBarMode: StateFlow<InstructionBarMode> = _instructionBarMode.asStateFlow()

    private val _badgeTheme = MutableStateFlow(BadgeTheme.GREEN)
    val badgeTheme: StateFlow<BadgeTheme> = _badgeTheme.asStateFlow()

    private val _showAppDetectionDialog = MutableStateFlow<String?>(null)
    val showAppDetectionDialog: StateFlow<String?> = _showAppDetectionDialog.asStateFlow()

    private val _currentDetectedAppName = MutableStateFlow<String?>(null)
    val currentDetectedAppName: StateFlow<String?> = _currentDetectedAppName.asStateFlow()

    // ===== Callback interface for preference storage =====

    var preferenceCallback: PreferenceCallback? = null

    interface PreferenceCallback {
        fun saveAppNumbersPreference(packageName: String, preference: AppNumbersPreference)
    }

    // ===== Numbers Overlay Methods =====

    fun setNumbersOverlayMode(mode: NumbersOverlayMode) {
        _numbersOverlayMode.value = mode
        updateNumbersOverlayVisibility()
        Log.d(TAG, "Numbers overlay mode: $mode")
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
        Log.d(TAG, "Numbers overlay: mode=$mode, hasItems=$hasItems, showing=$shouldShow")
    }

    fun updateNumberedOverlayItems(items: List<NumberOverlayItem>) {
        _numberedOverlayItems.value = items
        updateNumbersOverlayVisibility()
        if (items.isNotEmpty()) {
            Log.d(TAG, "Numbered overlay: ${items.size} items for voice selection")
        }
    }

    fun clearOverlayItems() {
        val hadNumbered = _numberedOverlayItems.value.isNotEmpty()
        val hadLabels = _iconLabelItems.value.isNotEmpty()
        if (hadNumbered) {
            _numberedOverlayItems.value = emptyList()
        }
        if (hadLabels) {
            _iconLabelItems.value = emptyList()
        }
        if (hadNumbered || hadLabels) {
            Log.d(TAG, "Cleared overlay items (numbered=$hadNumbered, labels=$hadLabels)")
            updateNumbersOverlayVisibility()
        }
    }

    fun updateIconLabelItems(items: List<IconLabelItem>) {
        _iconLabelItems.value = items
        if (items.isNotEmpty()) {
            Log.d(TAG, "Icon labels: ${items.size} text labels for icon-only elements")
        }
    }

    fun clearIconLabelItems() {
        if (_iconLabelItems.value.isNotEmpty()) {
            Log.d(TAG, "Clearing ${_iconLabelItems.value.size} icon label items")
            _iconLabelItems.value = emptyList()
        }
    }

    // ===== Instruction Bar =====

    fun setInstructionBarMode(mode: InstructionBarMode) {
        _instructionBarMode.value = mode
        Log.d(TAG, "Instruction bar mode: $mode")
    }

    // ===== Badge Theme =====

    fun setBadgeTheme(theme: BadgeTheme) {
        _badgeTheme.value = theme
        Log.d(TAG, "Badge theme: $theme")
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
        Log.d(TAG, "Showing app detection dialog for: $appName ($packageName)")
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
        Log.d(TAG, "App detection response for $packageName: $preference")

        if (preference != AppNumbersPreference.NEVER) {
            onExplore?.invoke()
        }
    }
}
