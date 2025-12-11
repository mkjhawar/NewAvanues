package com.augmentalis.cockpit.mvp

import android.content.Context
import android.content.SharedPreferences
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.presets.WindowPreset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Manages window presets for quick window creation
 * Persists presets to SharedPreferences as JSON
 *
 * Supports:
 * - Save preset with title, type, content, group name
 * - Load all presets
 * - Load presets by group
 * - Delete preset by ID
 */
class WindowPresetManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("window_presets", Context.MODE_PRIVATE)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Save a window preset
     *
     * @param title Window title
     * @param type Window type (WEB_APP, ANDROID_APP, etc.)
     * @param content Window content configuration
     * @param groupName Group name for organization (default: "default")
     * @param color Accent color for window
     */
    fun savePreset(
        title: String,
        type: WindowType,
        content: WindowContent,
        groupName: String = "default",
        color: String = "#4ECDC4"
    ) {
        val preset = WindowPreset(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            content = content,
            groupName = groupName,
            color = color,
            createdAt = System.currentTimeMillis()
        )

        val presets = loadPresets().toMutableList()
        presets.add(preset)

        val presetsJson = json.encodeToString(presets)
        prefs.edit().putString("presets", presetsJson).apply()
    }

    /**
     * Load all window presets
     *
     * @return List of presets, or empty list if none saved
     */
    fun loadPresets(): List<WindowPreset> {
        val presetsJson = prefs.getString("presets", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WindowPreset>>(presetsJson)
        } catch (e: Exception) {
            // Log error and return empty list if JSON is corrupted
            emptyList()
        }
    }

    /**
     * Delete a preset by ID
     *
     * @param id Preset ID to delete
     */
    fun deletePreset(id: String) {
        val presets = loadPresets().filter { it.id != id }
        val presetsJson = json.encodeToString(presets)
        prefs.edit().putString("presets", presetsJson).apply()
    }

    /**
     * Get presets by group name
     *
     * @param groupName Group name to filter by
     * @return List of presets in the specified group
     */
    fun getPresetsByGroup(groupName: String): List<WindowPreset> {
        return loadPresets().filter { it.groupName == groupName }
    }

    /**
     * Get all unique group names
     *
     * @return Sorted list of group names
     */
    fun getGroupNames(): List<String> {
        return loadPresets().map { it.groupName }.distinct().sorted()
    }

    /**
     * Clear all presets (for testing/reset)
     */
    fun clearAllPresets() {
        prefs.edit().remove("presets").apply()
    }
}
