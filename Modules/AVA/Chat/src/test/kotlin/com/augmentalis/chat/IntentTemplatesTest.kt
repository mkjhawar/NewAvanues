package com.augmentalis.chat

import com.augmentalis.llm.response.IntentTemplates
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for IntentTemplates.
 *
 * Validates template mapping, fallback behavior, and helper functions.
 * Ensures all supported intents have valid templates and unknown intents
 * receive appropriate teaching prompts.
 */
class IntentTemplatesTest {

    @Test
    fun `getResponse returns correct template for control_lights`() {
        val response = IntentTemplates.getResponse("control_lights")
        assertEquals("I'll control the lights for you.", response)
    }

    @Test
    fun `getResponse returns correct template for check_weather`() {
        val response = IntentTemplates.getResponse("check_weather")
        assertEquals("Let me check the weather for you.", response)
    }

    @Test
    fun `getResponse returns correct template for set_alarm`() {
        val response = IntentTemplates.getResponse("set_alarm")
        assertEquals("Setting an alarm for you.", response)
    }

    @Test
    fun `getResponse returns correct template for show_history`() {
        val response = IntentTemplates.getResponse("show_history")
        assertEquals("Here's your conversation history.", response)
    }

    @Test
    fun `getResponse returns correct template for new_conversation`() {
        val response = IntentTemplates.getResponse("new_conversation")
        assertEquals("Starting a new conversation.", response)
    }

    @Test
    fun `getResponse returns correct template for teach_ava`() {
        val response = IntentTemplates.getResponse("teach_ava")
        assertEquals("I'm ready to learn! What would you like to teach me?", response)
    }

    @Test
    fun `getResponse returns correct template for unknown`() {
        val response = IntentTemplates.getResponse("unknown")
        assertEquals("I'm not sure I understood. Would you like to teach me?", response)
    }

    @Test
    fun `getResponse returns unknown template for unrecognized intent`() {
        val response = IntentTemplates.getResponse("nonexistent_intent")
        assertEquals("I'm not sure I understood. Would you like to teach me?", response)
    }

    @Test
    fun `getResponse returns unknown template for empty string`() {
        val response = IntentTemplates.getResponse("")
        assertEquals("I'm not sure I understood. Would you like to teach me?", response)
    }

    @Test
    fun `getAllTemplates returns all templates`() {
        val templates = IntentTemplates.getAllTemplates()

        // Verify count (includes overlay-specific intents + device control + info + productivity + system/meta + fallback)
        assertEquals(17, templates.size)

        // Verify all expected intents are present
        assertTrue(templates.containsKey("control_lights"))
        assertTrue(templates.containsKey("check_weather"))
        assertTrue(templates.containsKey("set_alarm"))
        assertTrue(templates.containsKey("show_history"))
        assertTrue(templates.containsKey("new_conversation"))
        assertTrue(templates.containsKey("teach_ava"))
        assertTrue(templates.containsKey("unknown"))
    }

    @Test
    fun `getAllTemplates returns immutable copy`() {
        val templates1 = IntentTemplates.getAllTemplates()
        val templates2 = IntentTemplates.getAllTemplates()

        // Verify they are separate instances (defensive copy)
        assertNotSame(templates1, templates2)
        assertEquals(templates1, templates2)
    }

    @Test
    fun `hasTemplate returns true for existing intents`() {
        assertTrue(IntentTemplates.hasTemplate("control_lights"))
        assertTrue(IntentTemplates.hasTemplate("check_weather"))
        assertTrue(IntentTemplates.hasTemplate("set_alarm"))
        assertTrue(IntentTemplates.hasTemplate("show_history"))
        assertTrue(IntentTemplates.hasTemplate("new_conversation"))
        assertTrue(IntentTemplates.hasTemplate("teach_ava"))
    }

    @Test
    fun `hasTemplate returns false for unknown intent`() {
        assertFalse(IntentTemplates.hasTemplate("unknown"))
    }

    @Test
    fun `hasTemplate returns false for nonexistent intent`() {
        assertFalse(IntentTemplates.hasTemplate("nonexistent_intent"))
        assertFalse(IntentTemplates.hasTemplate(""))
        assertFalse(IntentTemplates.hasTemplate("random_string"))
    }

    @Test
    fun `getSupportedIntents returns all intents except unknown`() {
        val intents = IntentTemplates.getSupportedIntents()

        // Verify count (17 total - 1 unknown = 16)
        assertEquals(16, intents.size)

        // Verify core supported intents are present
        assertTrue(intents.contains("control_lights"))
        assertTrue(intents.contains("check_weather"))
        assertTrue(intents.contains("set_alarm"))
        assertTrue(intents.contains("show_history"))
        assertTrue(intents.contains("new_conversation"))
        assertTrue(intents.contains("teach_ava"))

        // Verify unknown is excluded
        assertFalse(intents.contains("unknown"))
    }

    @Test
    fun `templates are not empty or blank`() {
        val templates = IntentTemplates.getAllTemplates()

        templates.forEach { (intent, template) ->
            assertFalse("Template for '$intent' should not be empty", template.isEmpty())
            assertFalse("Template for '$intent' should not be blank", template.isBlank())
        }
    }

    @Test
    fun `templates end with proper punctuation`() {
        val templates = IntentTemplates.getAllTemplates()

        templates.forEach { (intent, template) ->
            val lastChar = template.last()
            assertTrue(
                "Template for '$intent' should end with punctuation (. or !), got '$lastChar'",
                lastChar == '.' || lastChar == '!' || lastChar == '?'
            )
        }
    }

    @Test
    fun `templates are reasonably concise`() {
        val templates = IntentTemplates.getAllTemplates()

        templates.forEach { (intent, template) ->
            assertTrue(
                "Template for '$intent' should be concise (<120 chars), got ${template.length} chars",
                template.length < 120
            )
        }
    }

    @Test
    fun `unknown template invites user teaching`() {
        val unknownTemplate = IntentTemplates.getResponse("unknown")

        assertTrue(
            "Unknown template should mention teaching",
            unknownTemplate.contains("teach", ignoreCase = true)
        )
    }
}
