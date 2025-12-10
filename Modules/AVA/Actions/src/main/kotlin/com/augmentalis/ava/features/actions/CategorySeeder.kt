/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.features.actions

import android.util.Log
import com.augmentalis.ava.core.data.repository.IntentCategoryRepository

/**
 * CategorySeeder - Populates intent category database on first launch
 *
 * Phase 2: Seeds database with all existing intent-category mappings.
 * Extracted from CategoryCapabilityRegistry for runtime database population.
 *
 * Usage:
 * ```kotlin
 * val seeder = CategorySeeder(repository)
 * seeder.seedCategories()
 * ```
 *
 * @author Manoj Jhawar
 * @since 2025-12-06
 */
class CategorySeeder(
    private val repository: IntentCategoryRepository
) {

    companion object {
        private const val TAG = "CategorySeeder"
    }

    /**
     * Seed database with all known intent-category mappings
     *
     * Runs in background (caller should use coroutine scope)
     */
    suspend fun seedCategories() {
        Log.i(TAG, "Seeding intent category database...")
        val startTime = System.currentTimeMillis()

        try {
            // ==================== CONNECTIVITY ====================
            // WiFi, Bluetooth, Airplane mode, Network control
            saveIntent("wifi_on", "connectivity", false, 100)
            saveIntent("wifi_off", "connectivity", false, 100)
            saveIntent("wifi_toggle", "connectivity", false, 100)
            saveIntent("bluetooth_on", "connectivity", false, 90)
            saveIntent("bluetooth_off", "connectivity", false, 90)
            saveIntent("bluetooth_toggle", "connectivity", false, 90)
            saveIntent("airplane_mode_on", "connectivity", false, 80)
            saveIntent("airplane_mode_off", "connectivity", false, 80)
            saveIntent("airplane_mode_toggle", "connectivity", false, 80)

            // ==================== VOLUME ====================
            // Volume control, mute, sound
            saveIntent("volume_up", "volume", false, 100)
            saveIntent("volume_down", "volume", false, 100)
            saveIntent("volume_mute", "volume", false, 100)
            saveIntent("volume_unmute", "volume", false, 100)
            saveIntent("volume_set", "volume", false, 90)
            saveIntent("ringer_mode_silent", "volume", false, 80)
            saveIntent("ringer_mode_vibrate", "volume", false, 80)
            saveIntent("ringer_mode_normal", "volume", false, 80)

            // ==================== MEDIA ====================
            // Play, pause, skip, music control
            saveIntent("play_media", "media", false, 100)
            saveIntent("pause_media", "media", false, 100)
            saveIntent("skip_next", "media", false, 100)
            saveIntent("skip_previous", "media", false, 100)
            saveIntent("stop_media", "media", false, 100)
            saveIntent("next_track", "media", false, 95)
            saveIntent("previous_track", "media", false, 95)
            saveIntent("play_music", "media", false, 90)
            saveIntent("pause_music", "media", false, 90)

            // ==================== SYSTEM ====================
            // Settings, apps, launch, open
            saveIntent("open_settings", "system", false, 100)
            saveIntent("open_app", "system", false, 100)
            saveIntent("launch_app", "system", false, 100)
            saveIntent("close_app", "system", false, 90)
            saveIntent("open_url", "system", false, 85)
            saveIntent("open_browser", "system", false, 80)

            // ==================== NAVIGATION ====================
            // App navigation, back, home, recent
            saveIntent("navigate_back", "navigation", false, 100)
            saveIntent("navigate_home", "navigation", false, 100)
            saveIntent("navigate_recent", "navigation", false, 100)
            saveIntent("open_recent_apps", "navigation", false, 95)
            saveIntent("go_back", "navigation", false, 95)
            saveIntent("go_home", "navigation", false, 95)

            // ==================== PRODUCTIVITY ====================
            // Alarm, timer, reminder, note, calendar
            saveIntent("set_alarm", "productivity", false, 100)
            saveIntent("set_timer", "productivity", false, 100)
            saveIntent("set_reminder", "productivity", false, 100)
            saveIntent("add_note", "productivity", false, 95)
            saveIntent("create_note", "productivity", false, 95)
            saveIntent("open_calendar", "productivity", false, 85)
            saveIntent("view_calendar", "productivity", false, 85)

            // ==================== CALCULATION ====================
            // Math operations, calculation
            saveIntent("perform_calculation", "calculation", false, 100)
            saveIntent("calculate", "calculation", false, 95)
            saveIntent("add", "calculation", false, 90)
            saveIntent("subtract", "calculation", false, 90)
            saveIntent("multiply", "calculation", false, 90)
            saveIntent("divide", "calculation", false, 90)

            // ==================== SMART HOME ====================
            // Smart home control, lights, temperature
            saveIntent("control_lights", "smart_home", false, 100)
            saveIntent("lights_on", "smart_home", false, 100)
            saveIntent("lights_off", "smart_home", false, 100)
            saveIntent("lights_toggle", "smart_home", false, 100)
            saveIntent("control_temperature", "smart_home", false, 95)
            saveIntent("set_temperature", "smart_home", false, 95)
            saveIntent("control_device", "smart_home", false, 85)

            // ==================== INFORMATION ====================
            // Weather, time, date, questions
            saveIntent("get_weather", "information", false, 100)
            saveIntent("show_weather", "information", false, 100)
            saveIntent("get_time", "information", false, 100)
            saveIntent("what_time", "information", false, 100)
            saveIntent("get_date", "information", false, 100)
            saveIntent("what_date", "information", false, 100)
            saveIntent("ask_question", "information", false, 90)

            // ==================== COMMUNICATION ====================
            // Email, SMS, calls
            saveIntent("send_email", "communication", false, 100)
            saveIntent("compose_email", "communication", false, 100)
            saveIntent("send_sms", "communication", false, 100)
            saveIntent("compose_sms", "communication", false, 100)
            saveIntent("make_call", "communication", false, 100)
            saveIntent("call", "communication", false, 95)
            saveIntent("answer_call", "communication", false, 90)
            saveIntent("end_call", "communication", false, 90)

            // ==================== GESTURE (VoiceOS - requires accessibility) ====================
            saveIntent("swipe_left", "gesture", true, 100)
            saveIntent("swipe_right", "gesture", true, 100)
            saveIntent("swipe_up", "gesture", true, 100)
            saveIntent("swipe_down", "gesture", true, 100)
            saveIntent("pinch", "gesture", true, 90)
            saveIntent("spread", "gesture", true, 90)
            saveIntent("long_press", "gesture", true, 85)

            // ==================== CURSOR (VoiceOS - requires accessibility) ====================
            saveIntent("show_cursor", "cursor", true, 100)
            saveIntent("hide_cursor", "cursor", true, 100)
            saveIntent("move_cursor_up", "cursor", true, 100)
            saveIntent("move_cursor_down", "cursor", true, 100)
            saveIntent("move_cursor_left", "cursor", true, 100)
            saveIntent("move_cursor_right", "cursor", true, 100)
            saveIntent("cursor_click", "cursor", true, 100)
            saveIntent("cursor_double_click", "cursor", true, 95)
            saveIntent("cursor_long_click", "cursor", true, 90)

            // ==================== SCROLL (VoiceOS - requires accessibility) ====================
            saveIntent("scroll_up", "scroll", true, 100)
            saveIntent("scroll_down", "scroll", true, 100)
            saveIntent("scroll_left", "scroll", true, 100)
            saveIntent("scroll_right", "scroll", true, 100)

            // ==================== DRAG (VoiceOS - requires accessibility) ====================
            saveIntent("drag_element", "drag", true, 100)
            saveIntent("drag_drop", "drag", true, 100)

            // ==================== KEYBOARD (VoiceOS - requires accessibility) ====================
            saveIntent("type_text", "keyboard", true, 100)
            saveIntent("delete_text", "keyboard", true, 100)
            saveIntent("select_text", "keyboard", true, 95)
            saveIntent("copy_text", "keyboard", true, 95)
            saveIntent("paste_text", "keyboard", true, 95)
            saveIntent("cut_text", "keyboard", true, 95)

            // ==================== EDITING (VoiceOS - requires accessibility) ====================
            saveIntent("edit_field", "editing", true, 100)
            saveIntent("clear_field", "editing", true, 100)
            saveIntent("focus_field", "editing", true, 95)

            // ==================== GAZE (VoiceOS - requires accessibility) ====================
            saveIntent("enable_gaze", "gaze", true, 100)
            saveIntent("disable_gaze", "gaze", true, 100)
            saveIntent("gaze_click", "gaze", true, 95)

            // ==================== OVERLAYS (VoiceOS - requires accessibility) ====================
            saveIntent("show_overlay", "overlays", true, 100)
            saveIntent("hide_overlay", "overlays", true, 100)
            saveIntent("toggle_overlay", "overlays", true, 95)

            // ==================== DIALOG (VoiceOS - requires accessibility) ====================
            saveIntent("confirm_dialog", "dialog", true, 100)
            saveIntent("cancel_dialog", "dialog", true, 100)
            saveIntent("show_dialog", "dialog", true, 95)

            // ==================== MENU (VoiceOS - requires accessibility) ====================
            saveIntent("open_menu", "menu", true, 100)
            saveIntent("close_menu", "menu", true, 100)
            saveIntent("select_menu_item", "menu", true, 95)

            // ==================== DICTATION (VoiceOS - requires accessibility) ====================
            saveIntent("start_dictation", "dictation", true, 100)
            saveIntent("stop_dictation", "dictation", true, 100)
            saveIntent("cancel_dictation", "dictation", true, 95)

            val elapsed = System.currentTimeMillis() - startTime
            val count = repository.getTotalCount()
            Log.i(TAG, "Seeding complete: $count intent mappings in ${elapsed}ms")

        } catch (e: Exception) {
            Log.e(TAG, "Error seeding categories", e)
            throw e
        }
    }

    /**
     * Save a single intent category mapping
     */
    private suspend fun saveIntent(
        intent: String,
        category: String,
        requiresAccessibility: Boolean,
        priority: Int
    ) {
        repository.saveIntentCategory(intent, category, requiresAccessibility, priority)
        Log.v(TAG, "Seeded: $intent -> $category (accessibility: $requiresAccessibility, priority: $priority)")
    }
}
