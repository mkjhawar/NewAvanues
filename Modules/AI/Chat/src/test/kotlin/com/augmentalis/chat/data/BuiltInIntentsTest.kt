package com.augmentalis.chat.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for BuiltInIntents
 *
 * Tests:
 * - Built-in intent constants are correctly defined
 * - ALL_INTENTS list contains expected intents
 * - FAST_KEYWORDS map is correctly configured
 * - Helper functions work correctly
 *
 * @author Manoj Jhawar
 * @since 2025-12-18
 */
class BuiltInIntentsTest {

    // ==================== Built-in Intents Tests ====================

    @Test
    fun `ALL_INTENTS contains all expected intents`() {
        val expected = listOf(
            "control_lights",
            "control_temperature",
            "check_weather",
            "show_time",
            "set_alarm",
            "set_reminder",
            "show_history",
            "new_conversation",
            "teach_ava",
            "unknown"
        )

        expected.forEach { intent ->
            assertTrue(
                BuiltInIntents.ALL_INTENTS.contains(intent),
                "ALL_INTENTS should contain '$intent'"
            )
        }
    }

    @Test
    fun `ALL_INTENTS has correct count`() {
        // 10 original + 11 system control + 8 navigation = 29 total
        assertEquals(29, BuiltInIntents.ALL_INTENTS.size, "Should have 29 built-in intents")
    }

    @Test
    fun `ALL_INTENTS contains system control intents`() {
        val systemIntents = listOf(
            "system_stop", "system_back", "system_cancel", "system_home",
            "system_help", "system_quit", "system_exit", "system_pause",
            "system_resume", "system_mute", "system_unmute"
        )

        systemIntents.forEach { intent ->
            assertTrue(
                BuiltInIntents.ALL_INTENTS.contains(intent),
                "ALL_INTENTS should contain '$intent'"
            )
        }
    }

    @Test
    fun `ALL_INTENTS contains navigation intents`() {
        val navIntents = listOf(
            "navigation_up", "navigation_down", "navigation_left", "navigation_right",
            "navigation_next", "navigation_previous", "navigation_select", "navigation_enter"
        )

        navIntents.forEach { intent ->
            assertTrue(
                BuiltInIntents.ALL_INTENTS.contains(intent),
                "ALL_INTENTS should contain '$intent'"
            )
        }
    }

    @Test
    fun `FAST_KEYWORDS all map to valid intents in ALL_INTENTS`() {
        // Issue 1.2 contract validation: All fast keyword intents must exist in ALL_INTENTS
        BuiltInIntents.FAST_KEYWORDS.forEach { (keyword, intent) ->
            assertTrue(
                BuiltInIntents.ALL_INTENTS.contains(intent),
                "FAST_KEYWORD '$keyword' maps to '$intent' which should be in ALL_INTENTS"
            )
        }
    }

    // ==================== FAST_KEYWORDS Tests ====================

    @Test
    fun `FAST_KEYWORDS is not empty`() {
        assertTrue(BuiltInIntents.FAST_KEYWORDS.isNotEmpty(), "FAST_KEYWORDS should not be empty")
    }

    @Test
    fun `FAST_KEYWORDS contains system commands`() {
        val systemCommands = listOf("stop", "back", "cancel", "home", "help", "quit", "exit")

        systemCommands.forEach { keyword ->
            assertTrue(
                BuiltInIntents.FAST_KEYWORDS.containsKey(keyword),
                "FAST_KEYWORDS should contain '$keyword'"
            )
        }
    }

    @Test
    fun `FAST_KEYWORDS contains voice control commands`() {
        val voiceCommands = listOf("pause", "resume", "mute", "unmute")

        voiceCommands.forEach { keyword ->
            assertTrue(
                BuiltInIntents.FAST_KEYWORDS.containsKey(keyword),
                "FAST_KEYWORDS should contain '$keyword'"
            )
        }
    }

    @Test
    fun `FAST_KEYWORDS contains navigation commands`() {
        val navCommands = listOf("up", "down", "left", "right", "next", "previous", "select", "enter")

        navCommands.forEach { keyword ->
            assertTrue(
                BuiltInIntents.FAST_KEYWORDS.containsKey(keyword),
                "FAST_KEYWORDS should contain '$keyword'"
            )
        }
    }

    @Test
    fun `FAST_KEYWORDS maps to correct intent names`() {
        assertEquals("system_stop", BuiltInIntents.FAST_KEYWORDS["stop"])
        assertEquals("system_back", BuiltInIntents.FAST_KEYWORDS["back"])
        assertEquals("system_cancel", BuiltInIntents.FAST_KEYWORDS["cancel"])
        assertEquals("system_home", BuiltInIntents.FAST_KEYWORDS["home"])
        assertEquals("navigation_up", BuiltInIntents.FAST_KEYWORDS["up"])
        assertEquals("navigation_down", BuiltInIntents.FAST_KEYWORDS["down"])
        assertEquals("navigation_select", BuiltInIntents.FAST_KEYWORDS["select"])
    }

    @Test
    fun `FAST_KEYWORDS all values follow naming convention`() {
        BuiltInIntents.FAST_KEYWORDS.values.forEach { intent ->
            assertTrue(
                intent.matches(Regex("^[a-z]+_[a-z]+$")),
                "Intent '$intent' should follow category_action naming convention"
            )
        }
    }

    // ==================== Helper Function Tests ====================

    @Test
    fun `isBuiltIn returns true for built-in intents`() {
        assertTrue(BuiltInIntents.isBuiltIn("control_lights"))
        assertTrue(BuiltInIntents.isBuiltIn("check_weather"))
        assertTrue(BuiltInIntents.isBuiltIn("teach_ava"))
        assertTrue(BuiltInIntents.isBuiltIn("unknown"))
    }

    @Test
    fun `isBuiltIn returns false for custom intents`() {
        assertFalse(BuiltInIntents.isBuiltIn("play_music"))
        assertFalse(BuiltInIntents.isBuiltIn("order_pizza"))
        assertFalse(BuiltInIntents.isBuiltIn("custom_action"))
    }

    @Test
    fun `getCategory returns correct categories`() {
        assertEquals("device_control", BuiltInIntents.getCategory("control_lights"))
        assertEquals("device_control", BuiltInIntents.getCategory("control_temperature"))
        assertEquals("information", BuiltInIntents.getCategory("check_weather"))
        assertEquals("information", BuiltInIntents.getCategory("show_time"))
        assertEquals("productivity", BuiltInIntents.getCategory("set_alarm"))
        assertEquals("productivity", BuiltInIntents.getCategory("set_reminder"))
        assertEquals("system", BuiltInIntents.getCategory("show_history"))
        assertEquals("system", BuiltInIntents.getCategory("new_conversation"))
        assertEquals("system", BuiltInIntents.getCategory("teach_ava"))
        assertEquals("system_control", BuiltInIntents.getCategory("system_stop"))
        assertEquals("system_control", BuiltInIntents.getCategory("system_mute"))
        assertEquals("navigation", BuiltInIntents.getCategory("navigation_up"))
        assertEquals("navigation", BuiltInIntents.getCategory("navigation_select"))
        assertEquals("custom", BuiltInIntents.getCategory("unknown_intent"))
    }

    @Test
    fun `getDisplayLabel returns human-readable labels`() {
        assertEquals("Control Lights", BuiltInIntents.getDisplayLabel("control_lights"))
        assertEquals("Check Weather", BuiltInIntents.getDisplayLabel("check_weather"))
        assertEquals("Set Alarm", BuiltInIntents.getDisplayLabel("set_alarm"))
        assertEquals("Teach AVA", BuiltInIntents.getDisplayLabel("teach_ava"))
        assertEquals("Unknown", BuiltInIntents.getDisplayLabel("unknown"))
    }

    @Test
    fun `getDisplayLabel formats custom intents correctly`() {
        assertEquals("Play Music", BuiltInIntents.getDisplayLabel("play_music"))
        assertEquals("Order Food", BuiltInIntents.getDisplayLabel("order_food"))
    }

    @Test
    fun `getExampleUtterances returns examples for built-in intents`() {
        val lightExamples = BuiltInIntents.getExampleUtterances("control_lights")
        assertTrue(lightExamples.isNotEmpty(), "Should have examples for control_lights")
        assertTrue(lightExamples.any { it.contains("lights", ignoreCase = true) })

        val weatherExamples = BuiltInIntents.getExampleUtterances("check_weather")
        assertTrue(weatherExamples.isNotEmpty(), "Should have examples for check_weather")
        assertTrue(weatherExamples.any { it.contains("weather", ignoreCase = true) })

        val teachExamples = BuiltInIntents.getExampleUtterances("teach_ava")
        assertTrue(teachExamples.isNotEmpty(), "Should have examples for teach_ava")
        assertTrue(teachExamples.size >= 3, "teach_ava should have at least 3 examples")
    }

    @Test
    fun `getExampleUtterances returns empty for unknown intents`() {
        val examples = BuiltInIntents.getExampleUtterances("unknown_custom_intent")
        assertTrue(examples.isEmpty(), "Should return empty list for unknown intents")
    }

    // ==================== Constants Tests ====================

    @Test
    fun `intent constants match expected values`() {
        assertEquals("control_lights", BuiltInIntents.CONTROL_LIGHTS)
        assertEquals("control_temperature", BuiltInIntents.CONTROL_TEMPERATURE)
        assertEquals("check_weather", BuiltInIntents.CHECK_WEATHER)
        assertEquals("show_time", BuiltInIntents.SHOW_TIME)
        assertEquals("set_alarm", BuiltInIntents.SET_ALARM)
        assertEquals("set_reminder", BuiltInIntents.SET_REMINDER)
        assertEquals("show_history", BuiltInIntents.SHOW_HISTORY)
        assertEquals("new_conversation", BuiltInIntents.NEW_CONVERSATION)
        assertEquals("teach_ava", BuiltInIntents.TEACH_AVA)
        assertEquals("unknown", BuiltInIntents.UNKNOWN)
    }
}
