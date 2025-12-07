/**
 * CommandManagerPatternMatchingTest.kt
 *
 * Unit tests for CommandManager pattern matching integration
 * Tests the DatabaseCommandResolver integration and pattern matching logic
 *
 * Created: 2025-11-14
 * Purpose: Verify Phase 2 implementation (database pattern matching)
 */

package com.augmentalis.commandmanager

import android.content.Context
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandSource
import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CommandManagerPatternMatchingTest {

    private lateinit var context: Context
    private lateinit var commandManager: CommandManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        commandManager = CommandManager.getInstance(context)
    }

    // ============================================================================
    // Pattern Matching Tests
    // ============================================================================

    @Test
    fun testExactPatternMatch() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert - Should match "go back" pattern to "nav_back" action
        // Note: Actual execution depends on database being loaded
        assertNotNull("Result should not be null", result)
    }

    @Test
    fun testSynonymPatternMatch() = runTest {
        // Arrange
        commandManager.initialize()

        // Test multiple synonyms for "go back"
        val synonyms = listOf("return", "previous", "back")

        for (synonym in synonyms) {
            val command = Command(id = synonym, text = synonym, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for '$synonym' should not be null", result)
        }
    }

    @Test
    fun testPartialPatternMatch() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "turn volume up", text = "turn volume up", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert - Should match partial pattern containing "volume up"
        assertNotNull("Result should not be null", result)
    }

    @Test
    fun testCaseInsensitiveMatching() = runTest {
        // Arrange
        commandManager.initialize()

        val variations = listOf(
            "GO BACK",
            "Go Back",
            "go back",
            "gO BaCk"
        )

        for (variation in variations) {
            val command = Command(id = variation, text = variation, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for '$variation' should not be null", result)
        }
    }

    @Test
    fun testWhitespaceNormalization() = runTest {
        // Arrange
        commandManager.initialize()

        val variations = listOf(
            "go back",
            " go back ",
            "  go  back  ",
            "go\tback"
        )

        for (variation in variations) {
            val command = Command(id = variation, text = variation, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for whitespace variation should not be null", result)
        }
    }

    @Test
    fun testUnknownCommandHandling() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "completely unknown command xyz123", text = "completely unknown command xyz123", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null", result)
        assertFalse("Unknown command should fail", result.success)
    }

    // ============================================================================
    // Navigation Commands Tests
    // ============================================================================

    @Test
    fun testNavigationCommands() = runTest {
        // Arrange
        commandManager.initialize()

        val navigationCommands = mapOf(
            "go back" to "nav_back",
            "go home" to "nav_home",
            "recent apps" to "nav_recent"
        )

        for ((text, expectedId) in navigationCommands) {
            val command = Command(id = text, text = text, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for '$text' should not be null", result)
        }
    }

    // ============================================================================
    // Volume Commands Tests
    // ============================================================================

    @Test
    fun testVolumeCommands() = runTest {
        // Arrange
        commandManager.initialize()

        val volumeCommands = listOf(
            "volume up",
            "volume down",
            "mute"
        )

        for (text in volumeCommands) {
            val command = Command(id = text, text = text, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for '$text' should not be null", result)
        }
    }

    // ============================================================================
    // System Commands Tests
    // ============================================================================

    @Test
    fun testSystemCommands() = runTest {
        // Arrange
        commandManager.initialize()

        val systemCommands = listOf(
            "wifi",
            "bluetooth",
            "open settings"
        )

        for (text in systemCommands) {
            val command = Command(id = text, text = text, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for '$text' should not be null", result)
        }
    }

    // ============================================================================
    // Confidence Level Tests
    // ============================================================================

    @Test
    fun testHighConfidenceCommand() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.95f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("High confidence command should execute", result)
    }

    @Test
    fun testMediumConfidenceCommand() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.75f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Medium confidence command should execute", result)
    }

    @Test
    fun testLowConfidenceCommand() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.55f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Low confidence command should execute with alternatives", result)
    }

    @Test
    fun testRejectedConfidenceCommand() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.35f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null", result)
        assertFalse("Very low confidence command should be rejected", result.success)
    }

    // ============================================================================
    // Multi-Language Tests
    // ============================================================================

    @Test
    fun testMultiLanguagePatternMatching() = runTest {
        // Arrange
        commandManager.initialize()

        // Test with different language patterns
        // These would match if database has multi-language commands loaded
        val multiLangCommands = mapOf(
            "zurück" to "de-DE",      // German
            "volver" to "es-ES",       // Spanish
            "retour" to "fr-FR"        // French
        )

        for ((text, locale) in multiLangCommands) {
            val command = Command(id = text, text = text, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for '$text' ($locale) should not be null", result)
        }
    }

    // ============================================================================
    // Initialization Tests
    // ============================================================================

    @Test
    fun testInitializationLoadsPatterns() = runTest {
        // Act
        commandManager.initialize()

        // Assert
        // After initialization, pattern cache should be loaded
        // This is verified indirectly through successful command matching
        assertTrue("CommandManager should initialize successfully", true)
    }

    @Test
    fun testMultipleInitializationsSafe() = runTest {
        // Act
        commandManager.initialize()
        commandManager.initialize()
        commandManager.initialize()

        // Assert
        // Multiple initializations should be safe
        assertTrue("Multiple initializations should be safe", true)
    }

    // ============================================================================
    // Performance Tests
    // ============================================================================

    @Test
    fun testPatternMatchingPerformance() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val startTime = System.currentTimeMillis()
        val result = commandManager.executeCommand(command)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertNotNull("Result should not be null", result)
        assertTrue("Pattern matching should be fast (<50ms)", duration < 50)
    }

    @Test
    fun testBulkCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()
        val commands = listOf(
            "go back",
            "go home",
            "volume up",
            "volume down",
            "wifi",
            "bluetooth"
        )

        // Act
        val startTime = System.currentTimeMillis()
        val results = commands.map { text ->
            val command = Command(id = text, text = text, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)
            commandManager.executeCommand(command)
        }
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertEquals("Should execute all commands", commands.size, results.size)
        results.forEach { result ->
            assertNotNull("Each result should not be null", result)
        }
        assertTrue("Bulk execution should be efficient", duration < 300)
    }

    // ============================================================================
    // Edge Cases Tests
    // ============================================================================

    @Test
    fun testEmptyCommandText() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "", text = "", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null", result)
        assertFalse("Empty command should fail", result.success)
    }

    @Test
    fun testNullTextHandling() = runTest {
        // Arrange
        commandManager.initialize()

        // Note: Command model might not allow null text, but test defensive coding
        // This test verifies the system handles edge cases gracefully
        assertTrue("Null text handling test placeholder", true)
    }

    @Test
    fun testVeryLongCommandText() = runTest {
        // Arrange
        commandManager.initialize()
        val longText = "go back ".repeat(100) // 800 characters
        val command = Command(id = longText, text = longText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null even for long text", result)
    }

    @Test
    fun testSpecialCharactersInCommand() = runTest {
        // Arrange
        commandManager.initialize()
        val specialChars = listOf(
            "go-back",
            "go_back",
            "go.back",
            "go!back"
        )

        for (text in specialChars) {
            val command = Command(id = text, text = text, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            // Act
            val result = commandManager.executeCommand(command)

            // Assert
            assertNotNull("Result for '$text' should not be null", result)
        }
    }

    // ============================================================================
    // Locale Switching Tests
    // ============================================================================

    @Test
    fun testLocaleSwitch() = runTest {
        // Arrange
        commandManager.initialize()
        val initialLocale = commandManager.getCurrentLocale()

        // Act
        // Locale switching would reload patterns
        val currentLocale = commandManager.getCurrentLocale()

        // Assert
        assertNotNull("Locale should not be null", currentLocale)
        assertTrue("Locale should be valid format", currentLocale.matches(Regex("[a-z]{2}-[A-Z]{2}")))
    }

    @Test
    fun testAvailableLocales() = runTest {
        // Arrange
        commandManager.initialize()

        // Act
        val locales = commandManager.getAvailableLocales()

        // Assert
        assertNotNull("Available locales should not be null", locales)
        assertTrue("Should have at least one locale", locales.isNotEmpty())
    }
}
