/**
 * LearnAppPreferences.kt - SharedPreferences wrapper for LearnApp settings
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-28
 *
 * Manages user preferences for LearnApp behavior:
 * - Learning mode (AUTO_DETECT vs MANUAL)
 * - Future settings can be added here
 */

package com.augmentalis.voiceoscore.learnapp.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * LearnApp Preferences Manager
 *
 * Wrapper around SharedPreferences for LearnApp settings.
 *
 * ## Learning Modes
 *
 * - **AUTO_DETECT** (default): Shows consent dialog automatically for new apps
 * - **MANUAL**: User must manually trigger learning from settings
 *
 * ## Usage
 *
 * ```kotlin
 * val prefs = LearnAppPreferences(context)
 *
 * // Get current mode
 * val mode = prefs.getLearningMode()
 *
 * // Set mode
 * prefs.setLearningMode(LearnAppPreferences.MODE_MANUAL)
 *
 * // Check if auto-detect enabled
 * if (prefs.isAutoDetectEnabled()) {
 *     // Show consent dialog
 * }
 * ```
 *
 * @param context Application or Activity context
 *
 * @since Phase 4
 */
class LearnAppPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "learnapp_prefs"

        const val KEY_LEARNING_MODE = "learning_mode"

        const val MODE_AUTO_DETECT = "AUTO_DETECT"
        const val MODE_MANUAL = "MANUAL"

        /**
         * Default learning mode
         */
        const val DEFAULT_MODE = MODE_AUTO_DETECT
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get current learning mode
     *
     * @return MODE_AUTO_DETECT or MODE_MANUAL
     */
    fun getLearningMode(): String {
        return prefs.getString(KEY_LEARNING_MODE, DEFAULT_MODE) ?: DEFAULT_MODE
    }

    /**
     * Set learning mode
     *
     * @param mode MODE_AUTO_DETECT or MODE_MANUAL
     */
    fun setLearningMode(mode: String) {
        require(mode == MODE_AUTO_DETECT || mode == MODE_MANUAL) {
            "Invalid learning mode: $mode. Must be MODE_AUTO_DETECT or MODE_MANUAL"
        }
        prefs.edit().putString(KEY_LEARNING_MODE, mode).apply()
    }

    /**
     * Check if auto-detect mode is enabled
     *
     * @return true if AUTO_DETECT mode
     */
    fun isAutoDetectEnabled(): Boolean {
        return getLearningMode() == MODE_AUTO_DETECT
    }

    /**
     * Check if manual mode is enabled
     *
     * @return true if MANUAL mode
     */
    fun isManualMode(): Boolean {
        return getLearningMode() == MODE_MANUAL
    }

    /**
     * Reset to default settings
     */
    fun reset() {
        prefs.edit().clear().apply()
    }
}
