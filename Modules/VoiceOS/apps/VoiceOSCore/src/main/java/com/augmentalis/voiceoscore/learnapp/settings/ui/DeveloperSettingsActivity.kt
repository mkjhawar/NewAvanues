/**
 * DeveloperSettingsActivity.kt - Developer settings activity for LearnApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Activity for configuring LearnApp developer settings.
 */

package com.augmentalis.voiceoscore.learnapp.settings.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppPreferences

/**
 * Developer Settings Activity
 *
 * Activity for configuring LearnApp developer settings.
 * Provides UI for adjusting timing, logging, and debug options.
 */
class DeveloperSettingsActivity : AppCompatActivity() {

    private lateinit var developerSettings: LearnAppDeveloperSettings
    private lateinit var preferences: LearnAppPreferences
    private lateinit var settingsAdapter: SettingsAdapter
    private lateinit var settingsList: MutableList<SettingItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize settings
        developerSettings = LearnAppDeveloperSettings(this)
        preferences = LearnAppPreferences(this)

        // Set up UI
        setupUI()
    }

    private fun setupUI() {
        // Create RecyclerView programmatically
        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@DeveloperSettingsActivity)
            id = android.R.id.list
        }
        setContentView(recyclerView)

        // Set title
        title = "LearnApp Developer Settings"

        // Build settings list
        settingsList = buildSettingsList().toMutableList()

        // Create adapter with callback
        settingsAdapter = SettingsAdapter { key, newValue ->
            handleSettingChange(key, newValue)
        }
        settingsAdapter.submitList(settingsList)

        recyclerView.adapter = settingsAdapter
    }

    private fun buildSettingsList(): List<SettingItem> {
        return listOf(
            // Logging section
            SettingItem(
                key = "verbose_logging",
                label = "Verbose Logging",
                description = "Enable detailed logging for debugging",
                type = SettingType.TOGGLE,
                value = developerSettings.isVerboseLoggingEnabled()
            ),

            // Timing section
            SettingItem(
                key = "click_delay",
                label = "Click Delay (ms)",
                description = "Delay between element clicks",
                type = SettingType.NUMBER_LONG,
                value = developerSettings.getClickDelayMs()
            ),
            SettingItem(
                key = "scroll_delay",
                label = "Scroll Delay (ms)",
                description = "Delay after scrolling",
                type = SettingType.NUMBER_LONG,
                value = developerSettings.getScrollDelayMs()
            ),
            SettingItem(
                key = "screen_change_delay",
                label = "Screen Change Delay (ms)",
                description = "Delay waiting for screen changes",
                type = SettingType.NUMBER_LONG,
                value = developerSettings.getExplorationStepDelay()
            ),

            // Exploration section
            SettingItem(
                key = "max_screens",
                label = "Max Screens",
                description = "Maximum screens to explore",
                type = SettingType.NUMBER_INT,
                value = developerSettings.getMaxExplorationDepth()
            ),
            SettingItem(
                key = "max_elements",
                label = "Max Elements Per Screen",
                description = "Maximum elements to process per screen",
                type = SettingType.NUMBER_INT,
                value = developerSettings.getBatchSize()
            ),
            SettingItem(
                key = "max_depth",
                label = "Max Exploration Depth",
                description = "Maximum depth for DFS exploration",
                type = SettingType.NUMBER_INT,
                value = developerSettings.getMaxExplorationDepth()
            ),

            // Debug section
            SettingItem(
                key = "debug_mode",
                label = "Debug Mode",
                description = "Enable debug overlays and extra logging",
                type = SettingType.TOGGLE,
                value = preferences.isDebugModeEnabled
            ),
            SettingItem(
                key = "show_exploration_overlay",
                label = "Show Exploration Overlay",
                description = "Show visual overlay during exploration",
                type = SettingType.TOGGLE,
                value = preferences.showExplorationOverlay
            ),

            // Auto-detect section
            SettingItem(
                key = "auto_detect",
                label = "Auto-Detect Mode",
                description = "Automatically detect app launches",
                type = SettingType.TOGGLE,
                value = preferences.isAutoDetectEnabled()
            )
        )
    }

    private fun handleSettingChange(key: String, newValue: Any) {
        when (key) {
            "verbose_logging" -> developerSettings.setVerboseLoggingEnabled(newValue as Boolean)
            "click_delay" -> developerSettings.setExplorationStepDelay((newValue as Number).toLong())
            "scroll_delay" -> developerSettings.setExplorationStepDelay((newValue as Number).toLong())
            "screen_change_delay" -> developerSettings.setExplorationStepDelay((newValue as Number).toLong())
            "max_screens" -> developerSettings.setMaxExplorationDepth((newValue as Number).toInt())
            "max_elements" -> developerSettings.setBatchSize((newValue as Number).toInt())
            "max_depth" -> developerSettings.setMaxExplorationDepth((newValue as Number).toInt())
            "debug_mode" -> preferences.isDebugModeEnabled = newValue as Boolean
            "show_exploration_overlay" -> preferences.showExplorationOverlay = newValue as Boolean
            "auto_detect" -> preferences.setAutoDetectEnabled(newValue as Boolean)
        }

        Toast.makeText(this, "Setting updated", Toast.LENGTH_SHORT).show()
    }
}
