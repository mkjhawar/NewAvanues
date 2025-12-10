/**
 * LearnAppDeveloperSettings.kt - Developer options for LearnApp tuning
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-05
 * Updated: 2025-12-05 (v2.1 - Added 2 new click delay settings: click_delay_ms, screen_processing_delay_ms)
 *
 * Provides configurable settings for LearnApp exploration parameters.
 * These were previously hardcoded values that developers can now adjust.
 *
 * ## Settings Categories (11 total)
 *
 * 1. **Exploration Settings** - DFS depth, timeout, screen count, similarity
 * 2. **Navigation Settings** - Back attempts, click failures, bounds tolerance
 * 3. **Login Settings** - Login screen timeout
 * 4. **Consent Settings** - Permission polling intervals
 * 5. **Scroll Settings** - Scroll attempts, delays, iterations
 * 6. **Click Settings** - Click retries, delays
 * 7. **UI Element Detection** - Touch targets, screen regions
 * 8. **Detection & Classification** - Expansion delays, confidence thresholds
 * 9. **JIT Learning** - Capture timeout, depth, element limits
 * 10. **State Detection** - Transient/stable durations, flicker detection
 * 11. **Debug Settings** - Logging, screenshots
 *
 * ## Usage
 *
 * ```kotlin
 * val devSettings = LearnAppDeveloperSettings(context)
 *
 * // Get exploration max depth
 * val maxDepth = devSettings.getMaxExplorationDepth()
 *
 * // Set custom timeout
 * devSettings.setExplorationTimeoutMs(600_000) // 10 minutes
 *
 * // Reset to defaults
 * devSettings.resetToDefaults()
 * ```
 *
 * @param context Application context
 * @since 1.0.0
 */

package com.augmentalis.voiceoscore.learnapp.settings

import android.content.Context
import android.content.SharedPreferences

class LearnAppDeveloperSettings(context: Context) {

    companion object {
        private const val PREFS_NAME = "learnapp_developer_settings"

        // ========== EXPLORATION SETTINGS ==========

        /** Maximum DFS exploration depth. Default: 10 levels deep */
        const val KEY_MAX_EXPLORATION_DEPTH = "max_exploration_depth"
        const val DEFAULT_MAX_EXPLORATION_DEPTH = 10

<<<<<<< HEAD
        /** Maximum exploration duration in milliseconds. Default: 1,080,000 (18 minutes) */
        const val KEY_EXPLORATION_TIMEOUT_MS = "exploration_timeout_ms"
        const val DEFAULT_EXPLORATION_TIMEOUT_MS = 1_080_000L
=======
        /** Maximum exploration duration in milliseconds. Default: 300,000 (5 minutes) */
        const val KEY_EXPLORATION_TIMEOUT_MS = "exploration_timeout_ms"
        const val DEFAULT_EXPLORATION_TIMEOUT_MS = 300_000L
>>>>>>> AVA-Development

        /** Estimated initial screen count for progress calculation. Default: 20 */
        const val KEY_ESTIMATED_INITIAL_SCREEN_COUNT = "estimated_initial_screen_count"
        const val DEFAULT_ESTIMATED_INITIAL_SCREEN_COUNT = 20

        /** Completeness threshold (%) for marking app as fully learned. Default: 95% */
        const val KEY_COMPLETENESS_THRESHOLD_PERCENT = "completeness_threshold_percent"
        const val DEFAULT_COMPLETENESS_THRESHOLD_PERCENT = 95f

        /** Screen hash similarity threshold for revisit detection. Default: 0.85 */
        const val KEY_SCREEN_HASH_SIMILARITY_THRESHOLD = "screen_hash_similarity_threshold"
        const val DEFAULT_SCREEN_HASH_SIMILARITY_THRESHOLD = 0.85f

        /** Screen transition poll interval in milliseconds. Default: 100 */
        const val KEY_SCREEN_TRANSITION_POLL_INTERVAL_MS = "screen_transition_poll_interval_ms"
        const val DEFAULT_SCREEN_TRANSITION_POLL_INTERVAL_MS = 100L

        // ========== NAVIGATION SETTINGS ==========

        /** Bounds tolerance in pixels for element refresh matching. Default: 20 */
        const val KEY_BOUNDS_TOLERANCE_PIXELS = "bounds_tolerance_pixels"
        const val DEFAULT_BOUNDS_TOLERANCE_PIXELS = 20

        /** Max consecutive element click failures before abandoning screen. Default: 5 */
        const val KEY_MAX_CONSECUTIVE_CLICK_FAILURES = "max_consecutive_click_failures"
        const val DEFAULT_MAX_CONSECUTIVE_CLICK_FAILURES = 5

        /** Max back navigation attempts to recover when stuck. Default: 3 */
        const val KEY_MAX_BACK_NAVIGATION_ATTEMPTS = "max_back_navigation_attempts"
        const val DEFAULT_MAX_BACK_NAVIGATION_ATTEMPTS = 3

        /** Minimum text length for element alias generation. Default: 3 */
        const val KEY_MIN_ALIAS_TEXT_LENGTH = "min_alias_text_length"
        const val DEFAULT_MIN_ALIAS_TEXT_LENGTH = 3

        // ========== LOGIN SETTINGS ==========

        /** Login screen wait timeout in milliseconds. Default: 600,000 (10 minutes) */
        const val KEY_LOGIN_TIMEOUT_MS = "login_timeout_ms"
        const val DEFAULT_LOGIN_TIMEOUT_MS = 600_000L

        // ========== CONSENT SETTINGS ==========

        /** Permission check interval in milliseconds. Default: 1,000 (1 second) */
        const val KEY_PERMISSION_CHECK_INTERVAL_MS = "permission_check_interval_ms"
        const val DEFAULT_PERMISSION_CHECK_INTERVAL_MS = 1000L

        /** Pending request expiry in milliseconds. Default: 60,000 (1 minute) */
        const val KEY_PENDING_REQUEST_EXPIRY_MS = "pending_request_expiry_ms"
        const val DEFAULT_PENDING_REQUEST_EXPIRY_MS = 60_000L

        /** Dialog animation delay in milliseconds. Default: 500 */
        const val KEY_DIALOG_ANIMATION_DELAY_MS = "dialog_animation_delay_ms"
        const val DEFAULT_DIALOG_ANIMATION_DELAY_MS = 500L

        // ========== SCROLL SETTINGS ==========

        /** Maximum scroll attempts per scrollable container. Default: 5 */
        const val KEY_MAX_SCROLL_ATTEMPTS = "max_scroll_attempts"
        const val DEFAULT_MAX_SCROLL_ATTEMPTS = 5

        /** Scroll delay between attempts in milliseconds. Default: 500 */
        const val KEY_SCROLL_DELAY_MS = "scroll_delay_ms"
        const val DEFAULT_SCROLL_DELAY_MS = 500L

        /** Max elements to capture per scrollable container. Default: 20 */
        const val KEY_MAX_ELEMENTS_PER_SCROLLABLE = "max_elements_per_scrollable"
        const val DEFAULT_MAX_ELEMENTS_PER_SCROLLABLE = 20

        /** Max vertical scroll iterations. Default: 50 */
        const val KEY_MAX_VERTICAL_SCROLL_ITERATIONS = "max_vertical_scroll_iterations"
        const val DEFAULT_MAX_VERTICAL_SCROLL_ITERATIONS = 50

        /** Max horizontal scroll iterations. Default: 20 */
        const val KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS = "max_horizontal_scroll_iterations"
        const val DEFAULT_MAX_HORIZONTAL_SCROLL_ITERATIONS = 20

        /** Max nesting depth for scrollable containers. Default: 2 */
        const val KEY_MAX_SCROLLABLE_CONTAINER_DEPTH = "max_scrollable_container_depth"
        const val DEFAULT_MAX_SCROLLABLE_CONTAINER_DEPTH = 2

        /** Max children to process per scroll container. Default: 50 */
        const val KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER = "max_children_per_scroll_container"
        const val DEFAULT_MAX_CHILDREN_PER_SCROLL_CONTAINER = 50

        /** Max children per container during exploration. Default: 50 */
        const val KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION = "max_children_per_container_exploration"
        const val DEFAULT_MAX_CHILDREN_PER_CONTAINER_EXPLORATION = 50

        // ========== CLICK SETTINGS ==========

        /** Click retry attempts. Default: 3 */
        const val KEY_CLICK_RETRY_ATTEMPTS = "click_retry_attempts"
        const val DEFAULT_CLICK_RETRY_ATTEMPTS = 3

        /** Click retry delay in milliseconds. Default: 200 */
        const val KEY_CLICK_RETRY_DELAY_MS = "click_retry_delay_ms"
        const val DEFAULT_CLICK_RETRY_DELAY_MS = 200L

        /** Click delay (post-click settling) in milliseconds. Default: 300 */
        const val KEY_CLICK_DELAY_MS = "click_delay_ms"
        const val DEFAULT_CLICK_DELAY_MS = 300L

        /** Screen processing delay (screen transitions) in milliseconds. Default: 1000 */
        const val KEY_SCREEN_PROCESSING_DELAY_MS = "screen_processing_delay_ms"
        const val DEFAULT_SCREEN_PROCESSING_DELAY_MS = 1000L

        // ========== UI ELEMENT DETECTION ==========

        /** Minimum touch target size in pixels (Material Design). Default: 48 */
        const val KEY_MIN_TOUCH_TARGET_SIZE_PIXELS = "min_touch_target_size_pixels"
        const val DEFAULT_MIN_TOUCH_TARGET_SIZE_PIXELS = 48

        /** Bottom screen region threshold Y-coordinate. Default: 1600 */
        const val KEY_BOTTOM_SCREEN_REGION_THRESHOLD = "bottom_screen_region_threshold"
        const val DEFAULT_BOTTOM_SCREEN_REGION_THRESHOLD = 1600

        /** Bottom navigation detection threshold. Default: 1600 */
        const val KEY_BOTTOM_NAV_THRESHOLD = "bottom_nav_threshold"
        const val DEFAULT_BOTTOM_NAV_THRESHOLD = 1600

        // ========== DETECTION & CLASSIFICATION ==========

        /** Wait delay for expandable control expansion in ms. Default: 500 */
        const val KEY_EXPANSION_WAIT_DELAY_MS = "expansion_wait_delay_ms"
        const val DEFAULT_EXPANSION_WAIT_DELAY_MS = 500L

        /** Confidence threshold for expandable control detection. Default: 0.65 */
        const val KEY_EXPANSION_CONFIDENCE_THRESHOLD = "expansion_confidence_threshold"
        const val DEFAULT_EXPANSION_CONFIDENCE_THRESHOLD = 0.65f

        /** Max retries for emitting app launch events. Default: 3 */
        const val KEY_MAX_APP_LAUNCH_EMIT_RETRIES = "max_app_launch_emit_retries"
        const val DEFAULT_MAX_APP_LAUNCH_EMIT_RETRIES = 3

        /** Delay between app launch emit retries in ms. Default: 100 */
        const val KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS = "app_launch_emit_retry_delay_ms"
        const val DEFAULT_APP_LAUNCH_EMIT_RETRY_DELAY_MS = 100L

        // ========== JIT LEARNING ==========

        /** JIT capture timeout in milliseconds. Default: 200 */
        const val KEY_JIT_CAPTURE_TIMEOUT_MS = "jit_capture_timeout_ms"
        const val DEFAULT_JIT_CAPTURE_TIMEOUT_MS = 200L

        /** JIT max traversal depth. Default: 10 */
        const val KEY_JIT_MAX_TRAVERSAL_DEPTH = "jit_max_traversal_depth"
        const val DEFAULT_JIT_MAX_TRAVERSAL_DEPTH = 10

        /** JIT max elements captured per capture. Default: 100 */
        const val KEY_JIT_MAX_ELEMENTS_CAPTURED = "jit_max_elements_captured"
        const val DEFAULT_JIT_MAX_ELEMENTS_CAPTURED = 100

        // ========== STATE DETECTION ==========

        /** Transient state duration threshold in ms. Default: 500 */
        const val KEY_TRANSIENT_STATE_DURATION_MS = "transient_state_duration_ms"
        const val DEFAULT_TRANSIENT_STATE_DURATION_MS = 500L

        /** Flicker state interval threshold in ms. Default: 200 */
        const val KEY_FLICKER_STATE_INTERVAL_MS = "flicker_state_interval_ms"
        const val DEFAULT_FLICKER_STATE_INTERVAL_MS = 200L

        /** Stable state duration threshold in ms. Default: 2000 */
        const val KEY_STABLE_STATE_DURATION_MS = "stable_state_duration_ms"
        const val DEFAULT_STABLE_STATE_DURATION_MS = 2000L

        /** Minimum flicker occurrences for detection. Default: 3 */
        const val KEY_MIN_FLICKER_OCCURRENCES = "min_flicker_occurrences"
        const val DEFAULT_MIN_FLICKER_OCCURRENCES = 3

        /** Flicker detection window in ms. Default: 5000 */
        const val KEY_FLICKER_DETECTION_WINDOW_MS = "flicker_detection_window_ms"
        const val DEFAULT_FLICKER_DETECTION_WINDOW_MS = 5000L

        /** Penalty for major contradiction. Default: 0.3 */
        const val KEY_PENALTY_MAJOR_CONTRADICTION = "penalty_major_contradiction"
        const val DEFAULT_PENALTY_MAJOR_CONTRADICTION = 0.3f

        /** Penalty for moderate contradiction. Default: 0.15 */
        const val KEY_PENALTY_MODERATE_CONTRADICTION = "penalty_moderate_contradiction"
        const val DEFAULT_PENALTY_MODERATE_CONTRADICTION = 0.15f

        /** Penalty for minor contradiction. Default: 0.05 */
        const val KEY_PENALTY_MINOR_CONTRADICTION = "penalty_minor_contradiction"
        const val DEFAULT_PENALTY_MINOR_CONTRADICTION = 0.05f

        /** Secondary state confidence threshold. Default: 0.5 */
        const val KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD = "secondary_state_confidence_threshold"
        const val DEFAULT_SECONDARY_STATE_CONFIDENCE_THRESHOLD = 0.5f

        // ========== METADATA QUALITY ==========

        /** Quality weight for text. Default: 0.3 */
        const val KEY_QUALITY_WEIGHT_TEXT = "quality_weight_text"
        const val DEFAULT_QUALITY_WEIGHT_TEXT = 0.3f

        /** Quality weight for content description. Default: 0.25 */
        const val KEY_QUALITY_WEIGHT_CONTENT_DESC = "quality_weight_content_desc"
        const val DEFAULT_QUALITY_WEIGHT_CONTENT_DESC = 0.25f

        /** Quality weight for resource ID. Default: 0.3 */
        const val KEY_QUALITY_WEIGHT_RESOURCE_ID = "quality_weight_resource_id"
        const val DEFAULT_QUALITY_WEIGHT_RESOURCE_ID = 0.3f

        /** Quality weight for actionability. Default: 0.15 */
        const val KEY_QUALITY_WEIGHT_ACTIONABLE = "quality_weight_actionable"
        const val DEFAULT_QUALITY_WEIGHT_ACTIONABLE = 0.15f

        // ========== CORE PROCESSING ==========

        /** Max voice command batch size before database write. Default: 200 */
        const val KEY_MAX_COMMAND_BATCH_SIZE = "max_command_batch_size"
        const val DEFAULT_MAX_COMMAND_BATCH_SIZE = 200

        /** Minimum generated label length. Default: 2 */
        const val KEY_MIN_GENERATED_LABEL_LENGTH = "min_generated_label_length"
        const val DEFAULT_MIN_GENERATED_LABEL_LENGTH = 2

        // ========== UI OVERLAY ==========

        /** Overlay auto-hide delay in ms. Default: 5000 */
        const val KEY_OVERLAY_AUTO_HIDE_DELAY_MS = "overlay_auto_hide_delay_ms"
        const val DEFAULT_OVERLAY_AUTO_HIDE_DELAY_MS = 5000L

        // ========== DEBUG SETTINGS ==========

        /** Enable verbose logging. Default: false */
        const val KEY_VERBOSE_LOGGING = "verbose_logging"
        const val DEFAULT_VERBOSE_LOGGING = false

        /** Enable screenshot capture on each screen. Default: false */
        const val KEY_SCREENSHOT_ON_SCREEN = "screenshot_on_screen"
        const val DEFAULT_SCREENSHOT_ON_SCREEN = false
<<<<<<< HEAD

        /** Enable VUID creation debug overlay. Default: false */
        const val KEY_DEBUG_OVERLAY_ENABLED = "debug_overlay_enabled"
        const val DEFAULT_DEBUG_OVERLAY_ENABLED = false
=======
>>>>>>> AVA-Development
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ========== EXPLORATION SETTINGS ==========

    fun getMaxExplorationDepth(): Int =
        prefs.getInt(KEY_MAX_EXPLORATION_DEPTH, DEFAULT_MAX_EXPLORATION_DEPTH)

    fun setMaxExplorationDepth(depth: Int) {
        require(depth in 1..50) { "Depth must be between 1 and 50" }
        prefs.edit().putInt(KEY_MAX_EXPLORATION_DEPTH, depth).apply()
    }

    fun getExplorationTimeoutMs(): Long =
        prefs.getLong(KEY_EXPLORATION_TIMEOUT_MS, DEFAULT_EXPLORATION_TIMEOUT_MS)

    fun setExplorationTimeoutMs(timeoutMs: Long) {
        require(timeoutMs in 30_000..3_600_000) { "Timeout must be between 30 seconds and 1 hour" }
        prefs.edit().putLong(KEY_EXPLORATION_TIMEOUT_MS, timeoutMs).apply()
    }

    fun getEstimatedInitialScreenCount(): Int =
        prefs.getInt(KEY_ESTIMATED_INITIAL_SCREEN_COUNT, DEFAULT_ESTIMATED_INITIAL_SCREEN_COUNT)

    fun setEstimatedInitialScreenCount(count: Int) {
        require(count in 1..100) { "Count must be between 1 and 100" }
        prefs.edit().putInt(KEY_ESTIMATED_INITIAL_SCREEN_COUNT, count).apply()
    }

    fun getCompletenessThresholdPercent(): Float =
        prefs.getFloat(KEY_COMPLETENESS_THRESHOLD_PERCENT, DEFAULT_COMPLETENESS_THRESHOLD_PERCENT)

    fun setCompletenessThresholdPercent(percent: Float) {
        require(percent in 50f..100f) { "Threshold must be between 50% and 100%" }
        prefs.edit().putFloat(KEY_COMPLETENESS_THRESHOLD_PERCENT, percent).apply()
    }

    fun getScreenHashSimilarityThreshold(): Float =
        prefs.getFloat(KEY_SCREEN_HASH_SIMILARITY_THRESHOLD, DEFAULT_SCREEN_HASH_SIMILARITY_THRESHOLD)

    fun setScreenHashSimilarityThreshold(threshold: Float) {
        require(threshold in 0.5f..1.0f) { "Threshold must be between 0.5 and 1.0" }
        prefs.edit().putFloat(KEY_SCREEN_HASH_SIMILARITY_THRESHOLD, threshold).apply()
    }

    fun getScreenTransitionPollIntervalMs(): Long =
        prefs.getLong(KEY_SCREEN_TRANSITION_POLL_INTERVAL_MS, DEFAULT_SCREEN_TRANSITION_POLL_INTERVAL_MS)

    fun setScreenTransitionPollIntervalMs(intervalMs: Long) {
        require(intervalMs in 10L..1000L) { "Poll interval must be between 10ms and 1000ms" }
        prefs.edit().putLong(KEY_SCREEN_TRANSITION_POLL_INTERVAL_MS, intervalMs).apply()
    }

    // ========== NAVIGATION SETTINGS ==========

    fun getBoundsTolerancePixels(): Int =
        prefs.getInt(KEY_BOUNDS_TOLERANCE_PIXELS, DEFAULT_BOUNDS_TOLERANCE_PIXELS)

    fun setBoundsTolerancePixels(pixels: Int) {
        require(pixels in 0..100) { "Tolerance must be between 0 and 100 pixels" }
        prefs.edit().putInt(KEY_BOUNDS_TOLERANCE_PIXELS, pixels).apply()
    }

    fun getMaxConsecutiveClickFailures(): Int =
        prefs.getInt(KEY_MAX_CONSECUTIVE_CLICK_FAILURES, DEFAULT_MAX_CONSECUTIVE_CLICK_FAILURES)

    fun setMaxConsecutiveClickFailures(count: Int) {
        require(count in 1..20) { "Count must be between 1 and 20" }
        prefs.edit().putInt(KEY_MAX_CONSECUTIVE_CLICK_FAILURES, count).apply()
    }

    fun getMaxBackNavigationAttempts(): Int =
        prefs.getInt(KEY_MAX_BACK_NAVIGATION_ATTEMPTS, DEFAULT_MAX_BACK_NAVIGATION_ATTEMPTS)

    fun setMaxBackNavigationAttempts(attempts: Int) {
        require(attempts in 1..10) { "Attempts must be between 1 and 10" }
        prefs.edit().putInt(KEY_MAX_BACK_NAVIGATION_ATTEMPTS, attempts).apply()
    }

    fun getMinAliasTextLength(): Int =
        prefs.getInt(KEY_MIN_ALIAS_TEXT_LENGTH, DEFAULT_MIN_ALIAS_TEXT_LENGTH)

    fun setMinAliasTextLength(length: Int) {
        require(length in 1..10) { "Length must be between 1 and 10" }
        prefs.edit().putInt(KEY_MIN_ALIAS_TEXT_LENGTH, length).apply()
    }

    // ========== LOGIN SETTINGS ==========

    fun getLoginTimeoutMs(): Long =
        prefs.getLong(KEY_LOGIN_TIMEOUT_MS, DEFAULT_LOGIN_TIMEOUT_MS)

    fun setLoginTimeoutMs(timeoutMs: Long) {
        require(timeoutMs in 60_000..1_800_000) { "Login timeout must be between 1 and 30 minutes" }
        prefs.edit().putLong(KEY_LOGIN_TIMEOUT_MS, timeoutMs).apply()
    }

    // ========== CONSENT SETTINGS ==========

    fun getPermissionCheckIntervalMs(): Long =
        prefs.getLong(KEY_PERMISSION_CHECK_INTERVAL_MS, DEFAULT_PERMISSION_CHECK_INTERVAL_MS)

    fun setPermissionCheckIntervalMs(intervalMs: Long) {
        require(intervalMs in 500..10_000) { "Interval must be between 500ms and 10 seconds" }
        prefs.edit().putLong(KEY_PERMISSION_CHECK_INTERVAL_MS, intervalMs).apply()
    }

    fun getPendingRequestExpiryMs(): Long =
        prefs.getLong(KEY_PENDING_REQUEST_EXPIRY_MS, DEFAULT_PENDING_REQUEST_EXPIRY_MS)

    fun setPendingRequestExpiryMs(expiryMs: Long) {
        require(expiryMs in 10_000..300_000) { "Expiry must be between 10 seconds and 5 minutes" }
        prefs.edit().putLong(KEY_PENDING_REQUEST_EXPIRY_MS, expiryMs).apply()
    }

    fun getDialogAnimationDelayMs(): Long =
        prefs.getLong(KEY_DIALOG_ANIMATION_DELAY_MS, DEFAULT_DIALOG_ANIMATION_DELAY_MS)

    fun setDialogAnimationDelayMs(delayMs: Long) {
        require(delayMs in 100L..2000L) { "Dialog animation delay must be between 100ms and 2000ms" }
        prefs.edit().putLong(KEY_DIALOG_ANIMATION_DELAY_MS, delayMs).apply()
    }

    // ========== SCROLL SETTINGS ==========

    fun getMaxScrollAttempts(): Int =
        prefs.getInt(KEY_MAX_SCROLL_ATTEMPTS, DEFAULT_MAX_SCROLL_ATTEMPTS)

    fun setMaxScrollAttempts(attempts: Int) {
        require(attempts in 1..20) { "Scroll attempts must be between 1 and 20" }
        prefs.edit().putInt(KEY_MAX_SCROLL_ATTEMPTS, attempts).apply()
    }

    fun getScrollDelayMs(): Long =
        prefs.getLong(KEY_SCROLL_DELAY_MS, DEFAULT_SCROLL_DELAY_MS)

    fun setScrollDelayMs(delayMs: Long) {
        require(delayMs in 100..2_000) { "Scroll delay must be between 100ms and 2 seconds" }
        prefs.edit().putLong(KEY_SCROLL_DELAY_MS, delayMs).apply()
    }

    fun getMaxElementsPerScrollable(): Int =
        prefs.getInt(KEY_MAX_ELEMENTS_PER_SCROLLABLE, DEFAULT_MAX_ELEMENTS_PER_SCROLLABLE)

    fun setMaxElementsPerScrollable(count: Int) {
        require(count in 5..100) { "Count must be between 5 and 100" }
        prefs.edit().putInt(KEY_MAX_ELEMENTS_PER_SCROLLABLE, count).apply()
    }

    fun getMaxVerticalScrollIterations(): Int =
        prefs.getInt(KEY_MAX_VERTICAL_SCROLL_ITERATIONS, DEFAULT_MAX_VERTICAL_SCROLL_ITERATIONS)

    fun setMaxVerticalScrollIterations(iterations: Int) {
        require(iterations in 5..200) { "Iterations must be between 5 and 200" }
        prefs.edit().putInt(KEY_MAX_VERTICAL_SCROLL_ITERATIONS, iterations).apply()
    }

    fun getMaxHorizontalScrollIterations(): Int =
        prefs.getInt(KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS, DEFAULT_MAX_HORIZONTAL_SCROLL_ITERATIONS)

    fun setMaxHorizontalScrollIterations(iterations: Int) {
        require(iterations in 5..100) { "Iterations must be between 5 and 100" }
        prefs.edit().putInt(KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS, iterations).apply()
    }

    fun getMaxScrollableContainerDepth(): Int =
        prefs.getInt(KEY_MAX_SCROLLABLE_CONTAINER_DEPTH, DEFAULT_MAX_SCROLLABLE_CONTAINER_DEPTH)

    fun setMaxScrollableContainerDepth(depth: Int) {
        require(depth in 1..10) { "Depth must be between 1 and 10" }
        prefs.edit().putInt(KEY_MAX_SCROLLABLE_CONTAINER_DEPTH, depth).apply()
    }

    fun getMaxChildrenPerScrollContainer(): Int =
        prefs.getInt(KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER, DEFAULT_MAX_CHILDREN_PER_SCROLL_CONTAINER)

    fun setMaxChildrenPerScrollContainer(count: Int) {
        require(count in 10..200) { "Count must be between 10 and 200" }
        prefs.edit().putInt(KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER, count).apply()
    }

    fun getMaxChildrenPerContainerExploration(): Int =
        prefs.getInt(KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION, DEFAULT_MAX_CHILDREN_PER_CONTAINER_EXPLORATION)

    fun setMaxChildrenPerContainerExploration(count: Int) {
        require(count in 10..200) { "Count must be between 10 and 200" }
        prefs.edit().putInt(KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION, count).apply()
    }

    // ========== CLICK SETTINGS ==========

    fun getClickRetryAttempts(): Int =
        prefs.getInt(KEY_CLICK_RETRY_ATTEMPTS, DEFAULT_CLICK_RETRY_ATTEMPTS)

    fun setClickRetryAttempts(attempts: Int) {
        require(attempts in 1..10) { "Click retry attempts must be between 1 and 10" }
        prefs.edit().putInt(KEY_CLICK_RETRY_ATTEMPTS, attempts).apply()
    }

    fun getClickRetryDelayMs(): Long =
        prefs.getLong(KEY_CLICK_RETRY_DELAY_MS, DEFAULT_CLICK_RETRY_DELAY_MS)

    fun setClickRetryDelayMs(delayMs: Long) {
        require(delayMs in 50..1_000) { "Click retry delay must be between 50ms and 1 second" }
        prefs.edit().putLong(KEY_CLICK_RETRY_DELAY_MS, delayMs).apply()
    }

    fun getClickDelayMs(): Long =
        prefs.getLong(KEY_CLICK_DELAY_MS, DEFAULT_CLICK_DELAY_MS)

    fun setClickDelayMs(delayMs: Long) {
        require(delayMs in 100..2_000) { "Click delay must be between 100ms and 2 seconds" }
        prefs.edit().putLong(KEY_CLICK_DELAY_MS, delayMs).apply()
    }

    fun getScreenProcessingDelayMs(): Long =
        prefs.getLong(KEY_SCREEN_PROCESSING_DELAY_MS, DEFAULT_SCREEN_PROCESSING_DELAY_MS)

    fun setScreenProcessingDelayMs(delayMs: Long) {
        require(delayMs in 100..5_000) { "Screen processing delay must be between 100ms and 5 seconds" }
        prefs.edit().putLong(KEY_SCREEN_PROCESSING_DELAY_MS, delayMs).apply()
    }

    // ========== UI ELEMENT DETECTION ==========

    fun getMinTouchTargetSizePixels(): Int =
        prefs.getInt(KEY_MIN_TOUCH_TARGET_SIZE_PIXELS, DEFAULT_MIN_TOUCH_TARGET_SIZE_PIXELS)

    fun setMinTouchTargetSizePixels(pixels: Int) {
        require(pixels in 24..96) { "Size must be between 24 and 96 pixels" }
        prefs.edit().putInt(KEY_MIN_TOUCH_TARGET_SIZE_PIXELS, pixels).apply()
    }

    fun getBottomScreenRegionThreshold(): Int =
        prefs.getInt(KEY_BOTTOM_SCREEN_REGION_THRESHOLD, DEFAULT_BOTTOM_SCREEN_REGION_THRESHOLD)

    fun setBottomScreenRegionThreshold(threshold: Int) {
        require(threshold in 500..3000) { "Threshold must be between 500 and 3000" }
        prefs.edit().putInt(KEY_BOTTOM_SCREEN_REGION_THRESHOLD, threshold).apply()
    }

    fun getBottomNavThreshold(): Int =
        prefs.getInt(KEY_BOTTOM_NAV_THRESHOLD, DEFAULT_BOTTOM_NAV_THRESHOLD)

    fun setBottomNavThreshold(threshold: Int) {
        require(threshold in 500..3000) { "Threshold must be between 500 and 3000" }
        prefs.edit().putInt(KEY_BOTTOM_NAV_THRESHOLD, threshold).apply()
    }

    // ========== DETECTION & CLASSIFICATION ==========

    fun getExpansionWaitDelayMs(): Long =
        prefs.getLong(KEY_EXPANSION_WAIT_DELAY_MS, DEFAULT_EXPANSION_WAIT_DELAY_MS)

    fun setExpansionWaitDelayMs(delayMs: Long) {
        require(delayMs in 100..2000) { "Delay must be between 100ms and 2 seconds" }
        prefs.edit().putLong(KEY_EXPANSION_WAIT_DELAY_MS, delayMs).apply()
    }

    fun getExpansionConfidenceThreshold(): Float =
        prefs.getFloat(KEY_EXPANSION_CONFIDENCE_THRESHOLD, DEFAULT_EXPANSION_CONFIDENCE_THRESHOLD)

    fun setExpansionConfidenceThreshold(threshold: Float) {
        require(threshold in 0.3f..1.0f) { "Threshold must be between 0.3 and 1.0" }
        prefs.edit().putFloat(KEY_EXPANSION_CONFIDENCE_THRESHOLD, threshold).apply()
    }

    fun getMaxAppLaunchEmitRetries(): Int =
        prefs.getInt(KEY_MAX_APP_LAUNCH_EMIT_RETRIES, DEFAULT_MAX_APP_LAUNCH_EMIT_RETRIES)

    fun setMaxAppLaunchEmitRetries(retries: Int) {
        require(retries in 1..10) { "Retries must be between 1 and 10" }
        prefs.edit().putInt(KEY_MAX_APP_LAUNCH_EMIT_RETRIES, retries).apply()
    }

    fun getAppLaunchEmitRetryDelayMs(): Long =
        prefs.getLong(KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS, DEFAULT_APP_LAUNCH_EMIT_RETRY_DELAY_MS)

    fun setAppLaunchEmitRetryDelayMs(delayMs: Long) {
        require(delayMs in 50..1000) { "Delay must be between 50ms and 1 second" }
        prefs.edit().putLong(KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS, delayMs).apply()
    }

    // ========== JIT LEARNING ==========

    fun getJitCaptureTimeoutMs(): Long =
        prefs.getLong(KEY_JIT_CAPTURE_TIMEOUT_MS, DEFAULT_JIT_CAPTURE_TIMEOUT_MS)

    fun setJitCaptureTimeoutMs(timeoutMs: Long) {
        require(timeoutMs in 50..1000) { "Timeout must be between 50ms and 1 second" }
        prefs.edit().putLong(KEY_JIT_CAPTURE_TIMEOUT_MS, timeoutMs).apply()
    }

    fun getJitMaxTraversalDepth(): Int =
        prefs.getInt(KEY_JIT_MAX_TRAVERSAL_DEPTH, DEFAULT_JIT_MAX_TRAVERSAL_DEPTH)

    fun setJitMaxTraversalDepth(depth: Int) {
        require(depth in 1..30) { "Depth must be between 1 and 30" }
        prefs.edit().putInt(KEY_JIT_MAX_TRAVERSAL_DEPTH, depth).apply()
    }

    fun getJitMaxElementsCaptured(): Int =
        prefs.getInt(KEY_JIT_MAX_ELEMENTS_CAPTURED, DEFAULT_JIT_MAX_ELEMENTS_CAPTURED)

    fun setJitMaxElementsCaptured(count: Int) {
        require(count in 10..500) { "Count must be between 10 and 500" }
        prefs.edit().putInt(KEY_JIT_MAX_ELEMENTS_CAPTURED, count).apply()
    }

    // ========== STATE DETECTION ==========

    fun getTransientStateDurationMs(): Long =
        prefs.getLong(KEY_TRANSIENT_STATE_DURATION_MS, DEFAULT_TRANSIENT_STATE_DURATION_MS)

    fun setTransientStateDurationMs(durationMs: Long) {
        require(durationMs in 100..2000) { "Duration must be between 100ms and 2 seconds" }
        prefs.edit().putLong(KEY_TRANSIENT_STATE_DURATION_MS, durationMs).apply()
    }

    fun getFlickerStateIntervalMs(): Long =
        prefs.getLong(KEY_FLICKER_STATE_INTERVAL_MS, DEFAULT_FLICKER_STATE_INTERVAL_MS)

    fun setFlickerStateIntervalMs(intervalMs: Long) {
        require(intervalMs in 50..1000) { "Interval must be between 50ms and 1 second" }
        prefs.edit().putLong(KEY_FLICKER_STATE_INTERVAL_MS, intervalMs).apply()
    }

    fun getStableStateDurationMs(): Long =
        prefs.getLong(KEY_STABLE_STATE_DURATION_MS, DEFAULT_STABLE_STATE_DURATION_MS)

    fun setStableStateDurationMs(durationMs: Long) {
        require(durationMs in 500..10000) { "Duration must be between 500ms and 10 seconds" }
        prefs.edit().putLong(KEY_STABLE_STATE_DURATION_MS, durationMs).apply()
    }

    fun getMinFlickerOccurrences(): Int =
        prefs.getInt(KEY_MIN_FLICKER_OCCURRENCES, DEFAULT_MIN_FLICKER_OCCURRENCES)

    fun setMinFlickerOccurrences(count: Int) {
        require(count in 2..10) { "Count must be between 2 and 10" }
        prefs.edit().putInt(KEY_MIN_FLICKER_OCCURRENCES, count).apply()
    }

    fun getFlickerDetectionWindowMs(): Long =
        prefs.getLong(KEY_FLICKER_DETECTION_WINDOW_MS, DEFAULT_FLICKER_DETECTION_WINDOW_MS)

    fun setFlickerDetectionWindowMs(windowMs: Long) {
        require(windowMs in 1000..30000) { "Window must be between 1 second and 30 seconds" }
        prefs.edit().putLong(KEY_FLICKER_DETECTION_WINDOW_MS, windowMs).apply()
    }

    fun getPenaltyMajorContradiction(): Float =
        prefs.getFloat(KEY_PENALTY_MAJOR_CONTRADICTION, DEFAULT_PENALTY_MAJOR_CONTRADICTION)

    fun setPenaltyMajorContradiction(penalty: Float) {
        require(penalty in 0.0f..1.0f) { "Penalty must be between 0.0 and 1.0" }
        prefs.edit().putFloat(KEY_PENALTY_MAJOR_CONTRADICTION, penalty).apply()
    }

    fun getPenaltyModerateContradiction(): Float =
        prefs.getFloat(KEY_PENALTY_MODERATE_CONTRADICTION, DEFAULT_PENALTY_MODERATE_CONTRADICTION)

    fun setPenaltyModerateContradiction(penalty: Float) {
        require(penalty in 0.0f..1.0f) { "Penalty must be between 0.0 and 1.0" }
        prefs.edit().putFloat(KEY_PENALTY_MODERATE_CONTRADICTION, penalty).apply()
    }

    fun getPenaltyMinorContradiction(): Float =
        prefs.getFloat(KEY_PENALTY_MINOR_CONTRADICTION, DEFAULT_PENALTY_MINOR_CONTRADICTION)

    fun setPenaltyMinorContradiction(penalty: Float) {
        require(penalty in 0.0f..1.0f) { "Penalty must be between 0.0 and 1.0" }
        prefs.edit().putFloat(KEY_PENALTY_MINOR_CONTRADICTION, penalty).apply()
    }

    fun getSecondaryStateConfidenceThreshold(): Float =
        prefs.getFloat(KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD, DEFAULT_SECONDARY_STATE_CONFIDENCE_THRESHOLD)

    fun setSecondaryStateConfidenceThreshold(threshold: Float) {
        require(threshold in 0.1f..1.0f) { "Threshold must be between 0.1 and 1.0" }
        prefs.edit().putFloat(KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD, threshold).apply()
    }

    // ========== METADATA QUALITY ==========

    fun getQualityWeightText(): Float =
        prefs.getFloat(KEY_QUALITY_WEIGHT_TEXT, DEFAULT_QUALITY_WEIGHT_TEXT)

    fun setQualityWeightText(weight: Float) {
        require(weight in 0.0f..1.0f) { "Weight must be between 0.0 and 1.0" }
        prefs.edit().putFloat(KEY_QUALITY_WEIGHT_TEXT, weight).apply()
    }

    fun getQualityWeightContentDesc(): Float =
        prefs.getFloat(KEY_QUALITY_WEIGHT_CONTENT_DESC, DEFAULT_QUALITY_WEIGHT_CONTENT_DESC)

    fun setQualityWeightContentDesc(weight: Float) {
        require(weight in 0.0f..1.0f) { "Weight must be between 0.0 and 1.0" }
        prefs.edit().putFloat(KEY_QUALITY_WEIGHT_CONTENT_DESC, weight).apply()
    }

    fun getQualityWeightResourceId(): Float =
        prefs.getFloat(KEY_QUALITY_WEIGHT_RESOURCE_ID, DEFAULT_QUALITY_WEIGHT_RESOURCE_ID)

    fun setQualityWeightResourceId(weight: Float) {
        require(weight in 0.0f..1.0f) { "Weight must be between 0.0 and 1.0" }
        prefs.edit().putFloat(KEY_QUALITY_WEIGHT_RESOURCE_ID, weight).apply()
    }

    fun getQualityWeightActionable(): Float =
        prefs.getFloat(KEY_QUALITY_WEIGHT_ACTIONABLE, DEFAULT_QUALITY_WEIGHT_ACTIONABLE)

    fun setQualityWeightActionable(weight: Float) {
        require(weight in 0.0f..1.0f) { "Weight must be between 0.0 and 1.0" }
        prefs.edit().putFloat(KEY_QUALITY_WEIGHT_ACTIONABLE, weight).apply()
    }

    // ========== CORE PROCESSING ==========

    fun getMaxCommandBatchSize(): Int =
        prefs.getInt(KEY_MAX_COMMAND_BATCH_SIZE, DEFAULT_MAX_COMMAND_BATCH_SIZE)

    fun setMaxCommandBatchSize(size: Int) {
        require(size in 10..1000) { "Size must be between 10 and 1000" }
        prefs.edit().putInt(KEY_MAX_COMMAND_BATCH_SIZE, size).apply()
    }

    fun getMinGeneratedLabelLength(): Int =
        prefs.getInt(KEY_MIN_GENERATED_LABEL_LENGTH, DEFAULT_MIN_GENERATED_LABEL_LENGTH)

    fun setMinGeneratedLabelLength(length: Int) {
        require(length in 1..10) { "Length must be between 1 and 10" }
        prefs.edit().putInt(KEY_MIN_GENERATED_LABEL_LENGTH, length).apply()
    }

    // ========== UI OVERLAY ==========

    fun getOverlayAutoHideDelayMs(): Long =
        prefs.getLong(KEY_OVERLAY_AUTO_HIDE_DELAY_MS, DEFAULT_OVERLAY_AUTO_HIDE_DELAY_MS)

    fun setOverlayAutoHideDelayMs(delayMs: Long) {
        require(delayMs in 1000..30000) { "Delay must be between 1 second and 30 seconds" }
        prefs.edit().putLong(KEY_OVERLAY_AUTO_HIDE_DELAY_MS, delayMs).apply()
    }

    // ========== DEBUG SETTINGS ==========

    fun isVerboseLoggingEnabled(): Boolean =
        prefs.getBoolean(KEY_VERBOSE_LOGGING, DEFAULT_VERBOSE_LOGGING)

    fun setVerboseLogging(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VERBOSE_LOGGING, enabled).apply()
    }

    fun isScreenshotOnScreenEnabled(): Boolean =
        prefs.getBoolean(KEY_SCREENSHOT_ON_SCREEN, DEFAULT_SCREENSHOT_ON_SCREEN)

    fun setScreenshotOnScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_ON_SCREEN, enabled).apply()
    }

<<<<<<< HEAD
    fun isDebugOverlayEnabled(): Boolean =
        prefs.getBoolean(KEY_DEBUG_OVERLAY_ENABLED, DEFAULT_DEBUG_OVERLAY_ENABLED)

    fun setDebugOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEBUG_OVERLAY_ENABLED, enabled).apply()
    }

=======
>>>>>>> AVA-Development
    // ========== UTILITY METHODS ==========

    /** Reset all settings to defaults */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /** Get all current settings as a map (for debugging/display) */
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            // Exploration
            KEY_MAX_EXPLORATION_DEPTH to getMaxExplorationDepth(),
            KEY_EXPLORATION_TIMEOUT_MS to getExplorationTimeoutMs(),
            KEY_ESTIMATED_INITIAL_SCREEN_COUNT to getEstimatedInitialScreenCount(),
            KEY_COMPLETENESS_THRESHOLD_PERCENT to getCompletenessThresholdPercent(),
            KEY_SCREEN_HASH_SIMILARITY_THRESHOLD to getScreenHashSimilarityThreshold(),
            KEY_SCREEN_TRANSITION_POLL_INTERVAL_MS to getScreenTransitionPollIntervalMs(),
            // Navigation
            KEY_BOUNDS_TOLERANCE_PIXELS to getBoundsTolerancePixels(),
            KEY_MAX_CONSECUTIVE_CLICK_FAILURES to getMaxConsecutiveClickFailures(),
            KEY_MAX_BACK_NAVIGATION_ATTEMPTS to getMaxBackNavigationAttempts(),
            KEY_MIN_ALIAS_TEXT_LENGTH to getMinAliasTextLength(),
            // Login
            KEY_LOGIN_TIMEOUT_MS to getLoginTimeoutMs(),
            // Consent
            KEY_PERMISSION_CHECK_INTERVAL_MS to getPermissionCheckIntervalMs(),
            KEY_PENDING_REQUEST_EXPIRY_MS to getPendingRequestExpiryMs(),
            KEY_DIALOG_ANIMATION_DELAY_MS to getDialogAnimationDelayMs(),
            // Scroll
            KEY_MAX_SCROLL_ATTEMPTS to getMaxScrollAttempts(),
            KEY_SCROLL_DELAY_MS to getScrollDelayMs(),
            KEY_MAX_ELEMENTS_PER_SCROLLABLE to getMaxElementsPerScrollable(),
            KEY_MAX_VERTICAL_SCROLL_ITERATIONS to getMaxVerticalScrollIterations(),
            KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS to getMaxHorizontalScrollIterations(),
            KEY_MAX_SCROLLABLE_CONTAINER_DEPTH to getMaxScrollableContainerDepth(),
            KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER to getMaxChildrenPerScrollContainer(),
            KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION to getMaxChildrenPerContainerExploration(),
            // Click
            KEY_CLICK_RETRY_ATTEMPTS to getClickRetryAttempts(),
            KEY_CLICK_RETRY_DELAY_MS to getClickRetryDelayMs(),
            KEY_CLICK_DELAY_MS to getClickDelayMs(),
            KEY_SCREEN_PROCESSING_DELAY_MS to getScreenProcessingDelayMs(),
            // UI Element Detection
            KEY_MIN_TOUCH_TARGET_SIZE_PIXELS to getMinTouchTargetSizePixels(),
            KEY_BOTTOM_SCREEN_REGION_THRESHOLD to getBottomScreenRegionThreshold(),
            KEY_BOTTOM_NAV_THRESHOLD to getBottomNavThreshold(),
            // Detection & Classification
            KEY_EXPANSION_WAIT_DELAY_MS to getExpansionWaitDelayMs(),
            KEY_EXPANSION_CONFIDENCE_THRESHOLD to getExpansionConfidenceThreshold(),
            KEY_MAX_APP_LAUNCH_EMIT_RETRIES to getMaxAppLaunchEmitRetries(),
            KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS to getAppLaunchEmitRetryDelayMs(),
            // JIT Learning
            KEY_JIT_CAPTURE_TIMEOUT_MS to getJitCaptureTimeoutMs(),
            KEY_JIT_MAX_TRAVERSAL_DEPTH to getJitMaxTraversalDepth(),
            KEY_JIT_MAX_ELEMENTS_CAPTURED to getJitMaxElementsCaptured(),
            // State Detection
            KEY_TRANSIENT_STATE_DURATION_MS to getTransientStateDurationMs(),
            KEY_FLICKER_STATE_INTERVAL_MS to getFlickerStateIntervalMs(),
            KEY_STABLE_STATE_DURATION_MS to getStableStateDurationMs(),
            KEY_MIN_FLICKER_OCCURRENCES to getMinFlickerOccurrences(),
            KEY_FLICKER_DETECTION_WINDOW_MS to getFlickerDetectionWindowMs(),
            KEY_PENALTY_MAJOR_CONTRADICTION to getPenaltyMajorContradiction(),
            KEY_PENALTY_MODERATE_CONTRADICTION to getPenaltyModerateContradiction(),
            KEY_PENALTY_MINOR_CONTRADICTION to getPenaltyMinorContradiction(),
            KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD to getSecondaryStateConfidenceThreshold(),
            // Metadata Quality
            KEY_QUALITY_WEIGHT_TEXT to getQualityWeightText(),
            KEY_QUALITY_WEIGHT_CONTENT_DESC to getQualityWeightContentDesc(),
            KEY_QUALITY_WEIGHT_RESOURCE_ID to getQualityWeightResourceId(),
            KEY_QUALITY_WEIGHT_ACTIONABLE to getQualityWeightActionable(),
            // Core Processing
            KEY_MAX_COMMAND_BATCH_SIZE to getMaxCommandBatchSize(),
            KEY_MIN_GENERATED_LABEL_LENGTH to getMinGeneratedLabelLength(),
            // UI Overlay
            KEY_OVERLAY_AUTO_HIDE_DELAY_MS to getOverlayAutoHideDelayMs(),
            // Debug
            KEY_VERBOSE_LOGGING to isVerboseLoggingEnabled(),
<<<<<<< HEAD
            KEY_SCREENSHOT_ON_SCREEN to isScreenshotOnScreenEnabled(),
            KEY_DEBUG_OVERLAY_ENABLED to isDebugOverlayEnabled()
=======
            KEY_SCREENSHOT_ON_SCREEN to isScreenshotOnScreenEnabled()
>>>>>>> AVA-Development
        )
    }

    /** Get setting descriptions (for UI display) */
    fun getSettingDescriptions(): Map<String, String> {
        return mapOf(
            // Exploration
            KEY_MAX_EXPLORATION_DEPTH to "Maximum depth for DFS exploration (1-50)",
            KEY_EXPLORATION_TIMEOUT_MS to "Maximum exploration time in milliseconds",
            KEY_ESTIMATED_INITIAL_SCREEN_COUNT to "Estimated initial screen count for progress (1-100)",
            KEY_COMPLETENESS_THRESHOLD_PERCENT to "Threshold % for marking app fully learned (50-100)",
            KEY_SCREEN_HASH_SIMILARITY_THRESHOLD to "Screen similarity threshold for revisits (0.5-1.0)",
            KEY_SCREEN_TRANSITION_POLL_INTERVAL_MS to "Screen transition poll interval (10-1000ms)",
            // Navigation
            KEY_BOUNDS_TOLERANCE_PIXELS to "Pixel tolerance for element refresh matching (0-100)",
            KEY_MAX_CONSECUTIVE_CLICK_FAILURES to "Max click failures before abandoning screen (1-20)",
            KEY_MAX_BACK_NAVIGATION_ATTEMPTS to "Max back button attempts to recover (1-10)",
            KEY_MIN_ALIAS_TEXT_LENGTH to "Minimum text length for element aliases (1-10)",
            // Login
            KEY_LOGIN_TIMEOUT_MS to "Time to wait for user login (ms)",
            // Consent
            KEY_PERMISSION_CHECK_INTERVAL_MS to "Interval to check overlay permission (ms)",
            KEY_PENDING_REQUEST_EXPIRY_MS to "Time before pending request expires (ms)",
            KEY_DIALOG_ANIMATION_DELAY_MS to "Dialog animation/settle delay (100-2000ms)",
            // Scroll
            KEY_MAX_SCROLL_ATTEMPTS to "Maximum scroll attempts per container (1-20)",
            KEY_SCROLL_DELAY_MS to "Delay between scroll attempts (ms)",
            KEY_MAX_ELEMENTS_PER_SCROLLABLE to "Max elements to capture per scrollable (5-100)",
            KEY_MAX_VERTICAL_SCROLL_ITERATIONS to "Max vertical scroll iterations (5-200)",
            KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS to "Max horizontal scroll iterations (5-100)",
            KEY_MAX_SCROLLABLE_CONTAINER_DEPTH to "Max nesting depth for scrollables (1-10)",
            KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER to "Max children per scroll container (10-200)",
            KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION to "Max children per container exploration (10-200)",
            // Click
            KEY_CLICK_RETRY_ATTEMPTS to "Number of click retry attempts (1-10)",
            KEY_CLICK_RETRY_DELAY_MS to "Delay between click retries (ms)",
            KEY_CLICK_DELAY_MS to "Post-click settling delay (ms)",
            KEY_SCREEN_PROCESSING_DELAY_MS to "Screen transition/processing delay (ms)",
            // UI Element Detection
            KEY_MIN_TOUCH_TARGET_SIZE_PIXELS to "Minimum touch target size (24-96 px)",
            KEY_BOTTOM_SCREEN_REGION_THRESHOLD to "Bottom screen region Y threshold (500-3000)",
            KEY_BOTTOM_NAV_THRESHOLD to "Bottom navigation detection threshold (500-3000)",
            // Detection & Classification
            KEY_EXPANSION_WAIT_DELAY_MS to "Delay for expandable control expansion (ms)",
            KEY_EXPANSION_CONFIDENCE_THRESHOLD to "Confidence threshold for expandable detection (0.3-1.0)",
            KEY_MAX_APP_LAUNCH_EMIT_RETRIES to "Max retries for app launch events (1-10)",
            KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS to "Delay between launch emit retries (ms)",
            // JIT Learning
            KEY_JIT_CAPTURE_TIMEOUT_MS to "JIT capture timeout (ms)",
            KEY_JIT_MAX_TRAVERSAL_DEPTH to "JIT max traversal depth (1-30)",
            KEY_JIT_MAX_ELEMENTS_CAPTURED to "JIT max elements captured (10-500)",
            // State Detection
            KEY_TRANSIENT_STATE_DURATION_MS to "Transient state duration threshold (ms)",
            KEY_FLICKER_STATE_INTERVAL_MS to "Flicker state interval threshold (ms)",
            KEY_STABLE_STATE_DURATION_MS to "Stable state duration threshold (ms)",
            KEY_MIN_FLICKER_OCCURRENCES to "Min flicker occurrences for detection (2-10)",
            KEY_FLICKER_DETECTION_WINDOW_MS to "Flicker detection window (ms)",
            KEY_PENALTY_MAJOR_CONTRADICTION to "Penalty for major contradiction (0.0-1.0)",
            KEY_PENALTY_MODERATE_CONTRADICTION to "Penalty for moderate contradiction (0.0-1.0)",
            KEY_PENALTY_MINOR_CONTRADICTION to "Penalty for minor contradiction (0.0-1.0)",
            KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD to "Secondary state confidence threshold (0.1-1.0)",
            // Metadata Quality
            KEY_QUALITY_WEIGHT_TEXT to "Quality weight for text (0.0-1.0)",
            KEY_QUALITY_WEIGHT_CONTENT_DESC to "Quality weight for content description (0.0-1.0)",
            KEY_QUALITY_WEIGHT_RESOURCE_ID to "Quality weight for resource ID (0.0-1.0)",
            KEY_QUALITY_WEIGHT_ACTIONABLE to "Quality weight for actionability (0.0-1.0)",
            // Core Processing
            KEY_MAX_COMMAND_BATCH_SIZE to "Max voice command batch size (10-1000)",
            KEY_MIN_GENERATED_LABEL_LENGTH to "Min generated label length (1-10)",
            // UI Overlay
            KEY_OVERLAY_AUTO_HIDE_DELAY_MS to "Overlay auto-hide delay (ms)",
            // Debug
            KEY_VERBOSE_LOGGING to "Enable detailed logging for debugging",
<<<<<<< HEAD
            KEY_SCREENSHOT_ON_SCREEN to "Capture screenshot on each new screen",
            KEY_DEBUG_OVERLAY_ENABLED to "Show VUID creation debug overlay during exploration"
=======
            KEY_SCREENSHOT_ON_SCREEN to "Capture screenshot on each new screen"
>>>>>>> AVA-Development
        )
    }

    /** Get settings grouped by category (for UI tabs) */
    fun getSettingsByCategory(): Map<String, List<String>> {
        return mapOf(
            "Exploration" to listOf(
                KEY_MAX_EXPLORATION_DEPTH,
                KEY_EXPLORATION_TIMEOUT_MS,
                KEY_ESTIMATED_INITIAL_SCREEN_COUNT,
                KEY_COMPLETENESS_THRESHOLD_PERCENT,
                KEY_SCREEN_HASH_SIMILARITY_THRESHOLD,
                KEY_SCREEN_TRANSITION_POLL_INTERVAL_MS
            ),
            "Navigation" to listOf(
                KEY_BOUNDS_TOLERANCE_PIXELS,
                KEY_MAX_CONSECUTIVE_CLICK_FAILURES,
                KEY_MAX_BACK_NAVIGATION_ATTEMPTS,
                KEY_MIN_ALIAS_TEXT_LENGTH
            ),
            "Login & Consent" to listOf(
                KEY_LOGIN_TIMEOUT_MS,
                KEY_PERMISSION_CHECK_INTERVAL_MS,
                KEY_PENDING_REQUEST_EXPIRY_MS,
                KEY_DIALOG_ANIMATION_DELAY_MS
            ),
            "Scrolling" to listOf(
                KEY_MAX_SCROLL_ATTEMPTS,
                KEY_SCROLL_DELAY_MS,
                KEY_MAX_ELEMENTS_PER_SCROLLABLE,
                KEY_MAX_VERTICAL_SCROLL_ITERATIONS,
                KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS,
                KEY_MAX_SCROLLABLE_CONTAINER_DEPTH,
                KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER,
                KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION
            ),
            "Click & Interaction" to listOf(
                KEY_CLICK_RETRY_ATTEMPTS,
                KEY_CLICK_RETRY_DELAY_MS,
                KEY_CLICK_DELAY_MS,
                KEY_SCREEN_PROCESSING_DELAY_MS
            ),
            "UI Detection" to listOf(
                KEY_MIN_TOUCH_TARGET_SIZE_PIXELS,
                KEY_BOTTOM_SCREEN_REGION_THRESHOLD,
                KEY_BOTTOM_NAV_THRESHOLD,
                KEY_EXPANSION_WAIT_DELAY_MS,
                KEY_EXPANSION_CONFIDENCE_THRESHOLD,
                KEY_MAX_APP_LAUNCH_EMIT_RETRIES,
                KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS
            ),
            "JIT Learning" to listOf(
                KEY_JIT_CAPTURE_TIMEOUT_MS,
                KEY_JIT_MAX_TRAVERSAL_DEPTH,
                KEY_JIT_MAX_ELEMENTS_CAPTURED
            ),
            "State Detection" to listOf(
                KEY_TRANSIENT_STATE_DURATION_MS,
                KEY_FLICKER_STATE_INTERVAL_MS,
                KEY_STABLE_STATE_DURATION_MS,
                KEY_MIN_FLICKER_OCCURRENCES,
                KEY_FLICKER_DETECTION_WINDOW_MS,
                KEY_PENALTY_MAJOR_CONTRADICTION,
                KEY_PENALTY_MODERATE_CONTRADICTION,
                KEY_PENALTY_MINOR_CONTRADICTION,
                KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD
            ),
            "Quality & Processing" to listOf(
                KEY_QUALITY_WEIGHT_TEXT,
                KEY_QUALITY_WEIGHT_CONTENT_DESC,
                KEY_QUALITY_WEIGHT_RESOURCE_ID,
                KEY_QUALITY_WEIGHT_ACTIONABLE,
                KEY_MAX_COMMAND_BATCH_SIZE,
                KEY_MIN_GENERATED_LABEL_LENGTH
            ),
            "UI & Debug" to listOf(
                KEY_OVERLAY_AUTO_HIDE_DELAY_MS,
                KEY_VERBOSE_LOGGING,
<<<<<<< HEAD
                KEY_SCREENSHOT_ON_SCREEN,
                KEY_DEBUG_OVERLAY_ENABLED
=======
                KEY_SCREENSHOT_ON_SCREEN
>>>>>>> AVA-Development
            )
        )
    }
}
