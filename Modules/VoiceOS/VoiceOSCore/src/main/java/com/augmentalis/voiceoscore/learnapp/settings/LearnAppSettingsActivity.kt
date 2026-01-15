/**
 * LearnAppSettingsActivity.kt - Settings screen for LearnApp
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-28
 *
 * Allows user to configure LearnApp behavior:
 * - Toggle between AUTO_DETECT and MANUAL learning modes
 * - View and manage learned apps (future)
 */

package com.augmentalis.voiceoscore.learnapp.settings

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.augmentalis.voiceoscore.R

/**
 * LearnApp Settings Activity
 *
 * Provides UI for configuring LearnApp preferences.
 *
 * ## Features
 *
 * - Learning mode selection (AUTO_DETECT vs MANUAL)
 * - Preferences persistence across app restarts
 * - Material Design 3 UI
 *
 * ## Usage
 *
 * ```kotlin
 * // Launch from settings menu
 * val intent = Intent(context, LearnAppSettingsActivity::class.java)
 * startActivity(intent)
 * ```
 *
 * @since Phase 4
 */
class LearnAppSettingsActivity : AppCompatActivity() {

    private lateinit var preferences: LearnAppPreferences
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioAutoDetect: RadioButton
    private lateinit var radioManual: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learnapp_settings)

        // Initialize preferences
        preferences = LearnAppPreferences(this)

        // Find views
        radioGroup = findViewById(R.id.radio_group_learning_mode)
        radioAutoDetect = findViewById(R.id.radio_auto_detect)
        radioManual = findViewById(R.id.radio_manual)

        // Set up action bar
        supportActionBar?.apply {
            title = "LearnApp Settings"
            setDisplayHomeAsUpEnabled(true)
        }

        // Load current mode
        loadCurrentMode()

        // Set up listeners
        setupListeners()
    }

    /**
     * Load and apply current learning mode
     */
    private fun loadCurrentMode() {
        val isAutoDetect = preferences.isAutoDetectEnabled()

        if (isAutoDetect) {
            radioAutoDetect.isChecked = true
        } else {
            radioManual.isChecked = true
        }
    }

    /**
     * Set up radio button listeners
     */
    private fun setupListeners() {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_auto_detect -> {
                    saveLearningMode(true)
                    showModeSavedToast("Auto-Detect mode enabled")
                }
                R.id.radio_manual -> {
                    saveLearningMode(false)
                    showModeSavedToast("Manual mode enabled")
                }
            }
        }
    }

    /**
     * Save learning mode to preferences
     */
    private fun saveLearningMode(autoDetect: Boolean) {
        preferences.setAutoDetectEnabled(autoDetect)
    }

    /**
     * Show toast notification when mode is saved
     */
    private fun showModeSavedToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Handle back button in action bar
     */
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
