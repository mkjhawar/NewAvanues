/**
 * DeveloperSettingsActivity.kt - Host Activity for LearnApp Developer Settings
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-05
 *
 * Simple activity that hosts DeveloperSettingsFragment for configuring
 * all LearnApp developer settings.
 */

package com.augmentalis.voiceoscore.learnapp.settings.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.augmentalis.voiceoscore.R

/**
 * Activity for configuring LearnApp developer settings.
 *
 * ## Usage
 * ```kotlin
 * // Launch from VoiceOS
 * LearnAppIntegration.openDeveloperSettings(context)
 * ```
 *
 * ## Features
 * - Hosts DeveloperSettingsFragment
 * - Provides toolbar with back navigation
 * - Full-screen settings UI
 */
class DeveloperSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_settings)

        // Setup toolbar with back navigation
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Developer Settings"
        }

        // Load fragment if this is first creation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, DeveloperSettingsFragment.newInstance())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle back button press
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
