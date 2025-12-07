/**
 * DeveloperSettingsViewModel.kt - ViewModel for Developer Settings UI
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-05
 *
 * Manages setting data and category selection for the Developer Settings UI.
 */

package com.augmentalis.voiceoscore.learnapp.settings.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings

/**
 * ViewModel for managing developer settings state.
 *
 * ## Responsibilities
 * - Load settings by category
 * - Update individual settings
 * - Reset all settings to defaults
 * - Provide setting metadata for UI display
 */
class DeveloperSettingsViewModel(context: Context) : ViewModel() {

    private val developerSettings = LearnAppDeveloperSettings(context)

    private val _settingsForCategory = MutableLiveData<List<SettingItem>>()
    val settingsForCategory: LiveData<List<SettingItem>> = _settingsForCategory

    private var currentCategory = ""

    /**
     * Select a category and load its settings
     */
    fun selectCategory(category: String) {
        currentCategory = category
        loadSettingsForCategory(category)
    }

    /**
     * Update a setting value
     */
    fun updateSetting(key: String, value: Any) {
        when (key) {
            // Exploration
            LearnAppDeveloperSettings.KEY_MAX_EXPLORATION_DEPTH ->
                developerSettings.setMaxExplorationDepth(value as Int)
            LearnAppDeveloperSettings.KEY_EXPLORATION_TIMEOUT_MS ->
                developerSettings.setExplorationTimeoutMs(value as Long)
            LearnAppDeveloperSettings.KEY_ESTIMATED_INITIAL_SCREEN_COUNT ->
                developerSettings.setEstimatedInitialScreenCount(value as Int)
            LearnAppDeveloperSettings.KEY_COMPLETENESS_THRESHOLD_PERCENT ->
                developerSettings.setCompletenessThresholdPercent(value as Float)
            LearnAppDeveloperSettings.KEY_SCREEN_HASH_SIMILARITY_THRESHOLD ->
                developerSettings.setScreenHashSimilarityThreshold(value as Float)
            LearnAppDeveloperSettings.KEY_SCREEN_TRANSITION_POLL_INTERVAL_MS ->
                developerSettings.setScreenTransitionPollIntervalMs(value as Long)

            // Navigation
            LearnAppDeveloperSettings.KEY_BOUNDS_TOLERANCE_PIXELS ->
                developerSettings.setBoundsTolerancePixels(value as Int)
            LearnAppDeveloperSettings.KEY_MAX_CONSECUTIVE_CLICK_FAILURES ->
                developerSettings.setMaxConsecutiveClickFailures(value as Int)
            LearnAppDeveloperSettings.KEY_MAX_BACK_NAVIGATION_ATTEMPTS ->
                developerSettings.setMaxBackNavigationAttempts(value as Int)
            LearnAppDeveloperSettings.KEY_MIN_ALIAS_TEXT_LENGTH ->
                developerSettings.setMinAliasTextLength(value as Int)

            // Login & Consent
            LearnAppDeveloperSettings.KEY_LOGIN_TIMEOUT_MS ->
                developerSettings.setLoginTimeoutMs(value as Long)
            LearnAppDeveloperSettings.KEY_PERMISSION_CHECK_INTERVAL_MS ->
                developerSettings.setPermissionCheckIntervalMs(value as Long)
            LearnAppDeveloperSettings.KEY_PENDING_REQUEST_EXPIRY_MS ->
                developerSettings.setPendingRequestExpiryMs(value as Long)
            LearnAppDeveloperSettings.KEY_DIALOG_ANIMATION_DELAY_MS ->
                developerSettings.setDialogAnimationDelayMs(value as Long)

            // Scrolling
            LearnAppDeveloperSettings.KEY_MAX_SCROLL_ATTEMPTS ->
                developerSettings.setMaxScrollAttempts(value as Int)
            LearnAppDeveloperSettings.KEY_SCROLL_DELAY_MS ->
                developerSettings.setScrollDelayMs(value as Long)
            LearnAppDeveloperSettings.KEY_MAX_ELEMENTS_PER_SCROLLABLE ->
                developerSettings.setMaxElementsPerScrollable(value as Int)
            LearnAppDeveloperSettings.KEY_MAX_VERTICAL_SCROLL_ITERATIONS ->
                developerSettings.setMaxVerticalScrollIterations(value as Int)
            LearnAppDeveloperSettings.KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS ->
                developerSettings.setMaxHorizontalScrollIterations(value as Int)
            LearnAppDeveloperSettings.KEY_MAX_SCROLLABLE_CONTAINER_DEPTH ->
                developerSettings.setMaxScrollableContainerDepth(value as Int)
            LearnAppDeveloperSettings.KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER ->
                developerSettings.setMaxChildrenPerScrollContainer(value as Int)
            LearnAppDeveloperSettings.KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION ->
                developerSettings.setMaxChildrenPerContainerExploration(value as Int)

            // Click
            LearnAppDeveloperSettings.KEY_CLICK_RETRY_ATTEMPTS ->
                developerSettings.setClickRetryAttempts(value as Int)
            LearnAppDeveloperSettings.KEY_CLICK_RETRY_DELAY_MS ->
                developerSettings.setClickRetryDelayMs(value as Long)
            LearnAppDeveloperSettings.KEY_CLICK_DELAY_MS ->
                developerSettings.setClickDelayMs(value as Long)
            LearnAppDeveloperSettings.KEY_SCREEN_PROCESSING_DELAY_MS ->
                developerSettings.setScreenProcessingDelayMs(value as Long)

            // UI Detection
            LearnAppDeveloperSettings.KEY_MIN_TOUCH_TARGET_SIZE_PIXELS ->
                developerSettings.setMinTouchTargetSizePixels(value as Int)
            LearnAppDeveloperSettings.KEY_BOTTOM_SCREEN_REGION_THRESHOLD ->
                developerSettings.setBottomScreenRegionThreshold(value as Int)
            LearnAppDeveloperSettings.KEY_BOTTOM_NAV_THRESHOLD ->
                developerSettings.setBottomNavThreshold(value as Int)
            LearnAppDeveloperSettings.KEY_EXPANSION_WAIT_DELAY_MS ->
                developerSettings.setExpansionWaitDelayMs(value as Long)
            LearnAppDeveloperSettings.KEY_EXPANSION_CONFIDENCE_THRESHOLD ->
                developerSettings.setExpansionConfidenceThreshold(value as Float)
            LearnAppDeveloperSettings.KEY_MAX_APP_LAUNCH_EMIT_RETRIES ->
                developerSettings.setMaxAppLaunchEmitRetries(value as Int)
            LearnAppDeveloperSettings.KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS ->
                developerSettings.setAppLaunchEmitRetryDelayMs(value as Long)

            // JIT
            LearnAppDeveloperSettings.KEY_JIT_CAPTURE_TIMEOUT_MS ->
                developerSettings.setJitCaptureTimeoutMs(value as Long)
            LearnAppDeveloperSettings.KEY_JIT_MAX_TRAVERSAL_DEPTH ->
                developerSettings.setJitMaxTraversalDepth(value as Int)
            LearnAppDeveloperSettings.KEY_JIT_MAX_ELEMENTS_CAPTURED ->
                developerSettings.setJitMaxElementsCaptured(value as Int)

            // State Detection
            LearnAppDeveloperSettings.KEY_TRANSIENT_STATE_DURATION_MS ->
                developerSettings.setTransientStateDurationMs(value as Long)
            LearnAppDeveloperSettings.KEY_FLICKER_STATE_INTERVAL_MS ->
                developerSettings.setFlickerStateIntervalMs(value as Long)
            LearnAppDeveloperSettings.KEY_STABLE_STATE_DURATION_MS ->
                developerSettings.setStableStateDurationMs(value as Long)
            LearnAppDeveloperSettings.KEY_MIN_FLICKER_OCCURRENCES ->
                developerSettings.setMinFlickerOccurrences(value as Int)
            LearnAppDeveloperSettings.KEY_FLICKER_DETECTION_WINDOW_MS ->
                developerSettings.setFlickerDetectionWindowMs(value as Long)
            LearnAppDeveloperSettings.KEY_PENALTY_MAJOR_CONTRADICTION ->
                developerSettings.setPenaltyMajorContradiction(value as Float)
            LearnAppDeveloperSettings.KEY_PENALTY_MODERATE_CONTRADICTION ->
                developerSettings.setPenaltyModerateContradiction(value as Float)
            LearnAppDeveloperSettings.KEY_PENALTY_MINOR_CONTRADICTION ->
                developerSettings.setPenaltyMinorContradiction(value as Float)
            LearnAppDeveloperSettings.KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD ->
                developerSettings.setSecondaryStateConfidenceThreshold(value as Float)

            // Quality & Processing
            LearnAppDeveloperSettings.KEY_QUALITY_WEIGHT_TEXT ->
                developerSettings.setQualityWeightText(value as Float)
            LearnAppDeveloperSettings.KEY_QUALITY_WEIGHT_CONTENT_DESC ->
                developerSettings.setQualityWeightContentDesc(value as Float)
            LearnAppDeveloperSettings.KEY_QUALITY_WEIGHT_RESOURCE_ID ->
                developerSettings.setQualityWeightResourceId(value as Float)
            LearnAppDeveloperSettings.KEY_QUALITY_WEIGHT_ACTIONABLE ->
                developerSettings.setQualityWeightActionable(value as Float)
            LearnAppDeveloperSettings.KEY_MAX_COMMAND_BATCH_SIZE ->
                developerSettings.setMaxCommandBatchSize(value as Int)
            LearnAppDeveloperSettings.KEY_MIN_GENERATED_LABEL_LENGTH ->
                developerSettings.setMinGeneratedLabelLength(value as Int)

            // UI & Debug
            LearnAppDeveloperSettings.KEY_OVERLAY_AUTO_HIDE_DELAY_MS ->
                developerSettings.setOverlayAutoHideDelayMs(value as Long)
            LearnAppDeveloperSettings.KEY_VERBOSE_LOGGING ->
                developerSettings.setVerboseLogging(value as Boolean)
            LearnAppDeveloperSettings.KEY_SCREENSHOT_ON_SCREEN ->
                developerSettings.setScreenshotOnScreen(value as Boolean)
        }

        // Refresh current category
        loadSettingsForCategory(currentCategory)
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        developerSettings.resetToDefaults()
        loadSettingsForCategory(currentCategory)
    }

    private fun loadSettingsForCategory(category: String) {
        val settingsByCategory = developerSettings.getSettingsByCategory()
        val descriptions = developerSettings.getSettingDescriptions()
        val allSettings = developerSettings.getAllSettings()

        val keys = settingsByCategory[category] ?: emptyList()
        val items = keys.map { key ->
            val value = allSettings[key] ?: getDefaultValue(key)
            val description = descriptions[key] ?: ""
            val type = getSettingType(key)

            SettingItem(
                key = key,
                label = formatLabel(key),
                description = description,
                value = value,
                type = type,
                minValue = getMinValue(key, type),
                maxValue = getMaxValue(key, type)
            )
        }

        _settingsForCategory.value = items
    }

    private fun getDefaultValue(key: String): Any {
        return when (key) {
            LearnAppDeveloperSettings.KEY_MAX_EXPLORATION_DEPTH -> LearnAppDeveloperSettings.DEFAULT_MAX_EXPLORATION_DEPTH
            LearnAppDeveloperSettings.KEY_VERBOSE_LOGGING -> LearnAppDeveloperSettings.DEFAULT_VERBOSE_LOGGING
            LearnAppDeveloperSettings.KEY_SCREENSHOT_ON_SCREEN -> LearnAppDeveloperSettings.DEFAULT_SCREENSHOT_ON_SCREEN
            else -> 0
        }
    }

    private fun getSettingType(key: String): SettingType {
        return when {
            key.contains("_threshold") || key.contains("_percent") || key.contains("_weight") || key.contains("penalty") ->
                SettingType.SLIDER
            key == LearnAppDeveloperSettings.KEY_VERBOSE_LOGGING ||
            key == LearnAppDeveloperSettings.KEY_SCREENSHOT_ON_SCREEN ->
                SettingType.TOGGLE
            key.contains("_ms") ->
                SettingType.NUMBER_LONG
            else ->
                SettingType.NUMBER_INT
        }
    }

    private fun formatLabel(key: String): String {
        return key
            .removePrefix("learnapp_")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun getMinValue(key: String, type: SettingType): Number {
        return when (type) {
            SettingType.SLIDER -> 0f
            SettingType.NUMBER_LONG -> 50L
            SettingType.NUMBER_INT -> 1
            SettingType.TOGGLE -> 0
        }
    }

    private fun getMaxValue(key: String, type: SettingType): Number {
        return when (type) {
            SettingType.SLIDER -> 1f
            SettingType.NUMBER_LONG -> 3_600_000L
            SettingType.NUMBER_INT -> 500
            SettingType.TOGGLE -> 1
        }
    }
}

/**
 * Factory for creating DeveloperSettingsViewModel with Context
 */
class DeveloperSettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeveloperSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeveloperSettingsViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
