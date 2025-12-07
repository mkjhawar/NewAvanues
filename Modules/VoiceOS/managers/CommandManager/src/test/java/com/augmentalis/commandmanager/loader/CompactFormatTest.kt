/**
 * CompactFormatTest.kt - Tests for compact JSON format parsing
 *
 * Tests:
 * - ArrayJsonParser with compact format
 * - CommandLoader with multi-language support
 * - Database integration with compact commands
 */

package com.augmentalis.commandmanager.loader

import com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before

class CompactFormatTest {

    private lateinit var sampleCompactJson: String
    private lateinit var sampleMultiLanguageJson: Map<String, String>

    @Before
    fun setUp() {
        // Sample compact JSON (English)
        sampleCompactJson = """
        {
          "version": "1.0",
          "locale": "en-US",
          "fallback": "en-US",
          "updated": "2025-11-13",
          "author": "VOS4 Team",
          "commands": [
            ["navigate_home", "navigate home", ["go home", "return home"], "Navigate Home (Navigation)"],
            ["turn_on_bluetooth", "turn on bluetooth", ["bluetooth on", "enable bluetooth"], "Turn On Bluetooth (Connectivity)"],
            ["scroll_up", "scroll up", ["swipe up", "page up"], "Scroll Up (Scrolling)"]
          ]
        }
        """.trimIndent()

        // Sample multi-language JSONs
        sampleMultiLanguageJson = mapOf(
            "en-US" to sampleCompactJson,
            "de-DE" to """
            {
              "version": "1.0",
              "locale": "de-DE",
              "fallback": "en-US",
              "updated": "2025-11-13",
              "author": "VOS4 Team",
              "commands": [
                ["navigate_home", "navigieren Startseite", ["gehen Startseite", "zurück Startseite"], "Navigate Home (Navigation)"],
                ["turn_on_bluetooth", "einschalten Bluetooth", ["Bluetooth on", "aktivieren Bluetooth"], "Turn On Bluetooth (Connectivity)"]
              ]
            }
            """.trimIndent(),
            "es-ES" to """
            {
              "version": "1.0",
              "locale": "es-ES",
              "fallback": "en-US",
              "updated": "2025-11-13",
              "author": "VOS4 Team",
              "commands": [
                ["navigate_home", "navegar inicio", ["ir inicio", "volver inicio"], "Navigate Home (Navigation)"],
                ["turn_on_bluetooth", "activar Bluetooth", ["Bluetooth on", "habilitar Bluetooth"], "Turn On Bluetooth (Connectivity)"]
              ]
            }
            """.trimIndent()
        )
    }

    @Test
    fun testParseCompactJsonStructure() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)

        assertTrue("Parse should succeed", result is ArrayJsonParser.ParseResult.Success)

        val success = result as ArrayJsonParser.ParseResult.Success
        assertEquals("Should parse 3 commands", 3, success.commands.size)
        assertEquals("Locale should be en-US", "en-US", success.locale)
        assertEquals("Version should be 1.0", "1.0", success.version)
    }

    @Test
    fun testParseCompactCommandArray() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        val firstCommand = success.commands[0]

        // Verify VoiceCommandEntity fields
        assertEquals("Action ID should match", "navigate_home", firstCommand.id)
        assertEquals("Locale should match", "en-US", firstCommand.locale)
        assertEquals("Primary text should match", "navigate home", firstCommand.primaryText)
        assertEquals("Description should match", "Navigate Home (Navigation)", firstCommand.description)
        assertTrue("Is fallback should be true", firstCommand.isFallback)
    }

    @Test
    fun testParseSynonyms() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        val firstCommand = success.commands[0]

        // Parse synonyms from JSON string
        val synonymsJson = JSONArray(firstCommand.synonyms)

        assertEquals("Should have 2 synonyms", 2, synonymsJson.length())
        assertEquals("First synonym should match", "go home", synonymsJson.getString(0))
        assertEquals("Second synonym should match", "return home", synonymsJson.getString(1))
    }

    @Test
    fun testMultipleCommands() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        assertEquals("Should have 3 commands", 3, success.commands.size)

        val commandIds = success.commands.map { it.id }
        assertTrue("Should contain navigate_home", commandIds.contains("navigate_home"))
        assertTrue("Should contain turn_on_bluetooth", commandIds.contains("turn_on_bluetooth"))
        assertTrue("Should contain scroll_up", commandIds.contains("scroll_up"))
    }

    @Test
    fun testCategoryExtraction() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        val homeCommand = success.commands.find { it.id == "navigate_home" }
        assertNotNull("navigate_home command should exist", homeCommand)

        // Category extracted from action ID
        assertEquals("Category should be navigation", "navigation", homeCommand!!.category)
    }

    @Test
    fun testGermanLocale() {
        val germanJson = sampleMultiLanguageJson["de-DE"]!!
        val result = ArrayJsonParser.parseCommandsJson(germanJson, isFallback = false)

        assertTrue("Parse should succeed", result is ArrayJsonParser.ParseResult.Success)

        val success = result as ArrayJsonParser.ParseResult.Success
        assertEquals("Locale should be de-DE", "de-DE", success.locale)
        assertEquals("Should have 2 commands", 2, success.commands.size)

        val homeCommand = success.commands.find { it.id == "navigate_home" }
        assertEquals("German primary text", "navigieren Startseite", homeCommand!!.primaryText)
        assertFalse("Should not be fallback", homeCommand.isFallback)
    }

    @Test
    fun testSpanishLocale() {
        val spanishJson = sampleMultiLanguageJson["es-ES"]!!
        val result = ArrayJsonParser.parseCommandsJson(spanishJson, isFallback = false)

        val success = result as ArrayJsonParser.ParseResult.Success
        assertEquals("Locale should be es-ES", "es-ES", success.locale)

        val homeCommand = success.commands.find { it.id == "navigate_home" }
        assertEquals("Spanish primary text", "navegar inicio", homeCommand!!.primaryText)
    }

    @Test
    fun testFallbackFlag() {
        // Test with fallback = true
        val resultFallback = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val successFallback = resultFallback as ArrayJsonParser.ParseResult.Success

        assertTrue("Fallback commands should have isFallback=true",
            successFallback.commands.all { it.isFallback })

        // Test with fallback = false
        val germanJson = sampleMultiLanguageJson["de-DE"]!!
        val resultNonFallback = ArrayJsonParser.parseCommandsJson(germanJson, isFallback = false)
        val successNonFallback = resultNonFallback as ArrayJsonParser.ParseResult.Success

        assertFalse("Non-fallback commands should have isFallback=false",
            successNonFallback.commands.any { it.isFallback })
    }

    @Test
    fun testInvalidJsonFormat() {
        val invalidJson = """{"invalid": "structure"}"""

        val result = ArrayJsonParser.parseCommandsJson(invalidJson, isFallback = true)

        assertTrue("Should return error for invalid JSON", result is ArrayJsonParser.ParseResult.Error)
    }

    @Test
    fun testMissingCommandsArray() {
        val missingCommands = """
        {
          "version": "1.0",
          "locale": "en-US"
        }
        """.trimIndent()

        val result = ArrayJsonParser.parseCommandsJson(missingCommands, isFallback = true)

        assertTrue("Should return error when commands array missing",
            result is ArrayJsonParser.ParseResult.Error)
    }

    @Test
    fun testInvalidCommandArrayLength() {
        val invalidLength = """
        {
          "version": "1.0",
          "locale": "en-US",
          "commands": [
            ["action", "cmd", ["syn"]]
          ]
        }
        """.trimIndent()

        val result = ArrayJsonParser.parseCommandsJson(invalidLength, isFallback = true)

        // Should skip invalid commands but still succeed with 0 commands
        assertTrue("Should handle invalid command gracefully", result is ArrayJsonParser.ParseResult.Success)
        val success = result as ArrayJsonParser.ParseResult.Success
        assertEquals("Should have 0 commands (invalid skipped)", 0, success.commands.size)
    }

    @Test
    fun testEmptyCommandsArray() {
        val emptyCommands = """
        {
          "version": "1.0",
          "locale": "en-US",
          "fallback": "en-US",
          "commands": []
        }
        """.trimIndent()

        val result = ArrayJsonParser.parseCommandsJson(emptyCommands, isFallback = true)

        assertTrue("Should succeed with empty commands", result is ArrayJsonParser.ParseResult.Success)
        val success = result as ArrayJsonParser.ParseResult.Success
        assertEquals("Should have 0 commands", 0, success.commands.size)
    }

    @Test
    fun testCommandArrayWithSpecialCharacters() {
        val specialChars = """
        {
          "version": "1.0",
          "locale": "en-US",
          "fallback": "en-US",
          "commands": [
            ["test_action", "it's working", ["don't stop", "can't fail"], "It's Working (Test)"]
          ]
        }
        """.trimIndent()

        val result = ArrayJsonParser.parseCommandsJson(specialChars, isFallback = true)

        assertTrue("Should handle special characters", result is ArrayJsonParser.ParseResult.Success)
        val success = result as ArrayJsonParser.ParseResult.Success

        val command = success.commands[0]
        assertEquals("Primary text with apostrophe", "it's working", command.primaryText)

        val synonyms = JSONArray(command.synonyms)
        assertEquals("First synonym", "don't stop", synonyms.getString(0))
    }

    @Test
    fun testCommandArrayWithUnicode() {
        val unicode = """
        {
          "version": "1.0",
          "locale": "de-DE",
          "fallback": "en-US",
          "commands": [
            ["test_action", "Menü öffnen", ["Einstellungen", "Zurück"], "Open Menu (Test)"]
          ]
        }
        """.trimIndent()

        val result = ArrayJsonParser.parseCommandsJson(unicode, isFallback = false)

        assertTrue("Should handle Unicode characters", result is ArrayJsonParser.ParseResult.Success)
        val success = result as ArrayJsonParser.ParseResult.Success

        val command = success.commands[0]
        assertEquals("Unicode in primary text", "Menü öffnen", command.primaryText)
    }

    @Test
    fun testValidateCompactJsonStructure() {
        assertTrue("Should validate correct structure",
            ArrayJsonParser.isValidCommandsJson(sampleCompactJson))
    }

    @Test
    fun testValidateInvalidStructure() {
        val invalid = """{"version": "1.0"}"""

        assertFalse("Should reject invalid structure",
            ArrayJsonParser.isValidCommandsJson(invalid))
    }

    @Test
    fun testDefaultPriority() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        // All commands should have default priority
        assertTrue("All commands should have default priority (50)",
            success.commands.all { it.priority == 50 })
    }

    @Test
    fun testVersionField() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        assertEquals("Version should be 1.0", "1.0", success.version)
    }

    @Test
    fun testFallbackLocale() {
        val result = ArrayJsonParser.parseCommandsJson(sampleCompactJson, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        assertEquals("Fallback locale should be en-US", "en-US", success.fallbackLocale)
    }

    @Test
    fun testManySynonyms() {
        // Test with volume command that has many synonyms
        val manySynonyms = """
        {
          "version": "1.0",
          "locale": "en-US",
          "fallback": "en-US",
          "commands": [
            ["set_volume_5", "volume five", ["set volume five", "volume level five", "five percent", "5 percent", "volume 5"], "Set Volume 5 (Volume)"]
          ]
        }
        """.trimIndent()

        val result = ArrayJsonParser.parseCommandsJson(manySynonyms, isFallback = true)
        val success = result as ArrayJsonParser.ParseResult.Success

        val command = success.commands[0]
        val synonyms = JSONArray(command.synonyms)

        assertEquals("Should have 5 synonyms", 5, synonyms.length())
    }

    @Test
    fun testActionIdCategoryMapping() {
        // Test category extraction from action IDs
        val testCases = mapOf(
            "navigate_home" to "navigation",
            "turn_on_bluetooth" to "connectivity",
            "scroll_up" to "scroll",
            "open_settings" to "settings",
            "set_volume_5" to "volume"
        )

        for ((actionId, expectedCategory) in testCases) {
            val category = VoiceCommandEntity.getCategoryFromId(actionId)
            assertEquals("Category for $actionId", expectedCategory, category)
        }
    }
}
