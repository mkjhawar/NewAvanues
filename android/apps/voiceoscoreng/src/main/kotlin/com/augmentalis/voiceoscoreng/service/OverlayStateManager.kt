package com.augmentalis.voiceoscoreng.service

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "OverlayStateManager"

/**
 * Manages overlay-related state for VoiceOS accessibility service.
 *
 * Extracted from VoiceOSAccessibilityService.kt for SOLID compliance.
 * Single Responsibility: Manage overlay visibility, modes, and themes.
 *
 * This is a singleton object as overlay state is shared across the app.
 */
object OverlayStateManager {

    // ===== Enums =====

    /**
     * Numbers overlay mode:
     * - ON: Always show numbers on all clickable elements
     * - OFF: Never show numbers
     * - AUTO: Show numbers only when there are list items (emails, messages, etc.)
     */
    enum class NumbersOverlayMode {
        ON,    // Always show
        OFF,   // Never show
        AUTO   // Show only for lists/duplicates
    }

    /**
     * Instruction bar mode:
     * - ON: Always show instruction bar
     * - OFF: Never show instruction bar
     * - AUTO: Show briefly then fade out (default)
     */
    enum class InstructionBarMode {
        ON,    // Always visible
        OFF,   // Never visible
        AUTO   // Show then fade after 3 seconds
    }

    /**
     * Badge color themes for numbered badges.
     */
    enum class BadgeTheme(val backgroundColor: Long, val textColor: Long) {
        GREEN(0xFF4CAF50, 0xFFFFFFFF),       // Default green
        BLUE(0xFF2196F3, 0xFFFFFFFF),        // Blue
        PURPLE(0xFF9C27B0, 0xFFFFFFFF),      // Purple
        ORANGE(0xFFFF9800, 0xFF000000),      // Orange with black text
        RED(0xFFF44336, 0xFFFFFFFF),         // Red
        TEAL(0xFF009688, 0xFFFFFFFF),        // Teal
        PINK(0xFFE91E63, 0xFFFFFFFF)         // Pink
    }

    /**
     * Preference for per-app numbers mode.
     * Stored in SharedPreferences with key "app_numbers_mode_{packageName}"
     */
    enum class AppNumbersPreference {
        ASK,       // Not yet decided, show dialog
        ALWAYS,    // Always show numbers in this app
        AUTO,      // Use AUTO mode for this app
        NEVER      // Never show numbers in this app
    }

    /**
     * Data for displaying numbered badges on screen elements.
     * Used by the numbers overlay to show which elements can be selected by number.
     * Example: User says "first" or "1" to click element with number=1
     */
    data class NumberOverlayItem(
        val number: Int,           // 1-based display number (matches "first", "second", etc.)
        val label: String,         // Short label for display (e.g., sender name)
        val left: Int,             // Element bounds
        val top: Int,
        val right: Int,
        val bottom: Int,
        val vuid: String           // Target VUID for executing action
    )

    // ===== Target Apps =====

    /**
     * Apps that commonly have list-based UIs where numbers overlay is helpful.
     * These apps will trigger a first-time prompt asking the user about numbers mode.
     */
    val TARGET_APPS = setOf(
        // Email clients
        "com.google.android.gm",           // Gmail
        "com.microsoft.office.outlook",    // Outlook
        "com.samsung.android.email.provider", // Samsung Mail
        "com.yahoo.mobile.client.android.mail", // Yahoo Mail
        "me.bluemail.mail",                // BlueMail
        "org.mozilla.thunderbird",         // Thunderbird
        // Messaging apps
        "com.google.android.apps.messaging", // Google Messages
        "com.whatsapp",                    // WhatsApp
        "org.telegram.messenger",          // Telegram
        "com.discord",                     // Discord
        "com.Slack",                       // Slack
        // Social media
        "com.twitter.android",             // Twitter/X
        "com.instagram.android",           // Instagram
        "com.facebook.katana",             // Facebook
        "com.linkedin.android",            // LinkedIn
        // Task/Note apps
        "com.todoist",                     // Todoist
        "com.google.android.keep",         // Google Keep
        "com.microsoft.todos",             // Microsoft To Do
        // Shopping/Lists
        "com.amazon.mShop.android.shopping", // Amazon
        "com.google.android.apps.shopping.express", // Google Shopping
        "com.google.android.deskclock"     // Google Clock
    )

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

    private val _showAppDetectionDialog = MutableStateFlow<String?>(null) // packageName or null
    val showAppDetectionDialog: StateFlow<String?> = _showAppDetectionDialog.asStateFlow()

    private val _currentDetectedAppName = MutableStateFlow<String?>(null)
    val currentDetectedAppName: StateFlow<String?> = _currentDetectedAppName.asStateFlow()

    // ===== Callback interface for preference storage =====

    /**
     * Callback interface for saving app preferences.
     * Set by VoiceOSAccessibilityService to handle SharedPreferences.
     */
    var preferenceCallback: PreferenceCallback? = null

    interface PreferenceCallback {
        fun saveAppNumbersPreference(packageName: String, preference: AppNumbersPreference)
    }

    // ===== Numbers Overlay Methods =====

    /**
     * Set the numbers overlay mode.
     * Voice commands: "numbers on", "numbers off", "numbers auto"
     */
    fun setNumbersOverlayMode(mode: NumbersOverlayMode) {
        _numbersOverlayMode.value = mode
        updateNumbersOverlayVisibility()
        Log.d(TAG, "Numbers overlay mode: $mode")
    }

    /**
     * Cycle through overlay modes: OFF -> AUTO -> ON -> OFF
     * Voice command: "show numbers" or "toggle numbers"
     */
    fun cycleNumbersOverlayMode() {
        val newMode = when (_numbersOverlayMode.value) {
            NumbersOverlayMode.OFF -> NumbersOverlayMode.AUTO
            NumbersOverlayMode.AUTO -> NumbersOverlayMode.ON
            NumbersOverlayMode.ON -> NumbersOverlayMode.OFF
        }
        setNumbersOverlayMode(newMode)
    }

    /**
     * Update visibility based on current mode and items.
     * Called when mode changes or when items are updated.
     */
    fun updateNumbersOverlayVisibility() {
        val mode = _numbersOverlayMode.value
        val hasItems = _numberedOverlayItems.value.isNotEmpty()

        val shouldShow = when (mode) {
            NumbersOverlayMode.ON -> true
            NumbersOverlayMode.OFF -> false
            NumbersOverlayMode.AUTO -> hasItems  // Only show when list items exist
        }

        _showNumbersOverlayComputed.value = shouldShow
        Log.d(TAG, "Numbers overlay: mode=$mode, hasItems=$hasItems, showing=$shouldShow")
    }

    /**
     * Update the numbered overlay items.
     * Called by DynamicCommandGenerator after processing list items.
     */
    fun updateNumberedOverlayItems(items: List<NumberOverlayItem>) {
        _numberedOverlayItems.value = items
        updateNumbersOverlayVisibility()
        if (items.isNotEmpty()) {
            Log.d(TAG, "Numbered overlay: ${items.size} items for voice selection")
        }
    }

    /**
     * Legacy toggle for backward compatibility.
     */
    fun setShowNumbersOverlay(show: Boolean) {
        setNumbersOverlayMode(if (show) NumbersOverlayMode.ON else NumbersOverlayMode.OFF)
    }

    // ===== Instruction Bar Methods =====

    fun setInstructionBarMode(mode: InstructionBarMode) {
        _instructionBarMode.value = mode
        Log.d(TAG, "Instruction bar mode: $mode")
    }

    fun cycleInstructionBarMode() {
        val newMode = when (_instructionBarMode.value) {
            InstructionBarMode.OFF -> InstructionBarMode.AUTO
            InstructionBarMode.AUTO -> InstructionBarMode.ON
            InstructionBarMode.ON -> InstructionBarMode.OFF
        }
        setInstructionBarMode(newMode)
    }

    // ===== Badge Theme Methods =====

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

    // ===== App Detection Dialog Methods =====

    /**
     * Show the app detection dialog for a specific package.
     */
    fun showAppDetectionDialogFor(packageName: String, appName: String) {
        _currentDetectedAppName.value = appName
        _showAppDetectionDialog.value = packageName
        Log.d(TAG, "Showing app detection dialog for: $appName ($packageName)")
    }

    /**
     * Dismiss the app detection dialog.
     */
    fun dismissAppDetectionDialog() {
        _showAppDetectionDialog.value = null
        _currentDetectedAppName.value = null
    }

    /**
     * Handle user response from app detection dialog.
     * @param onExplore Callback to trigger screen exploration after preference change
     */
    fun handleAppDetectionResponse(
        packageName: String,
        preference: AppNumbersPreference,
        onExplore: (() -> Unit)? = null
    ) {
        preferenceCallback?.saveAppNumbersPreference(packageName, preference)
        dismissAppDetectionDialog()

        // Apply the preference immediately
        when (preference) {
            AppNumbersPreference.ALWAYS -> setNumbersOverlayMode(NumbersOverlayMode.ON)
            AppNumbersPreference.AUTO -> setNumbersOverlayMode(NumbersOverlayMode.AUTO)
            AppNumbersPreference.NEVER -> setNumbersOverlayMode(NumbersOverlayMode.OFF)
            AppNumbersPreference.ASK -> { /* No change, will ask again next time */ }
        }
        Log.d(TAG, "App detection response for $packageName: $preference")

        // Refresh the screen to populate overlay items with the new setting
        if (preference != AppNumbersPreference.NEVER) {
            onExplore?.invoke()
        }
    }
}
