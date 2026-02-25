/**
 * KeyboardSettingsActivity.kt - Keyboard settings UI
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.preferences

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.ListPreference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.EditTextPreference
import com.augmentalis.voicekeyboard.R

/**
 * Settings activity for the Voice Keyboard
 */
class KeyboardSettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set content view with fragment container
        setContentView(R.layout.activity_keyboard_settings)
        
        // Add preferences fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, KeyboardSettingsFragment())
                .commit()
        }
        
        // Setup action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.keyboard_settings)
        }
        
        // Setup modern back handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

/**
 * Preferences fragment for keyboard settings
 */
class KeyboardSettingsFragment : PreferenceFragmentCompat() {
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.keyboard_preferences, rootKey)
        
        setupPreferences()
    }
    
    private fun setupPreferences() {
        // Setup dictation timeout preference
        findPreference<SeekBarPreference>("pref_dictation_timeout")?.apply {
            min = 3
            max = 30
            setDefaultValue(5)
            showSeekBarValue = true
            summary = resources.getQuantityString(R.plurals.dictation_timeout_summary, value, value)
            setOnPreferenceChangeListener { _, newValue ->
                val timeout = newValue as Int
                summary = resources.getQuantityString(R.plurals.dictation_timeout_summary, timeout, timeout)
                true
            }
        }
        
        // Setup theme preference
        findPreference<ListPreference>("pref_keyboard_theme")?.apply {
            entries = arrayOf(
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system)
            )
            entryValues = arrayOf("LIGHT", "DARK", "SYSTEM")
            setDefaultValue("SYSTEM")
            summary = entry
            setOnPreferenceChangeListener { _, newValue ->
                val index = findIndexOfValue(newValue as String)
                summary = entries[index]
                true
            }
        }
        
        // Setup key height preference
        findPreference<SeekBarPreference>("pref_key_height")?.apply {
            min = 40
            max = 100
            setDefaultValue(60)
            showSeekBarValue = true
            summary = "$value dp"
            setOnPreferenceChangeListener { _, newValue ->
                summary = "$newValue dp"
                true
            }
        }
        
        // Setup dictation commands
        findPreference<EditTextPreference>("pref_dictation_start_command")?.apply {
            summary = text ?: "dictation"
            setOnPreferenceChangeListener { _, newValue ->
                summary = newValue as String
                true
            }
        }
        
        findPreference<EditTextPreference>("pref_dictation_stop_command")?.apply {
            summary = text ?: "end dictation"
            setOnPreferenceChangeListener { _, newValue ->
                summary = newValue as String
                true
            }
        }
    }
}