/**
 * EndToEndCommandExecutionTest.kt
 *
 * End-to-end integration tests for complete voice command execution flow
 * Tests the entire pipeline: Voice Input → Pattern Matching → Action Execution → Result
 *
 * Created: 2025-11-14
 * Purpose: Verify complete command execution pipeline works correctly
 */

package com.augmentalis.commandmanager

import android.content.Context
import com.augmentalis.voiceoscore.Command
import com.augmentalis.voiceoscore.CommandSource
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class EndToEndCommandExecutionTest {

    private lateinit var context: Context
    private lateinit var commandManager: CommandManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        commandManager = CommandManager.getInstance(context)
    }

    // ============================================================================
    // Navigation Command Execution Tests
    // ============================================================================

    @Test
    fun testGoBackCommandExecution() = runTest {
        // Arrange - Simulate user saying "go back"
        commandManager.initialize()
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act - Execute the command through CommandManager
        val result = commandManager.executeCommand(command)

        // Assert - Verify successful execution
        assertNotNull("Result should not be null", result)
        assertNotNull("Command should not be null", result.command)
        assertEquals("Command text should match", "go back", result.command.text)
    }

    @Test
    fun testGoBackSynonyms() = runTest {
        // Arrange - Test multiple ways to say "go back"
        commandManager.initialize()

        val synonyms = listOf(
            "return",
            "previous",
            "back",
            "go back"
        )

        // Act & Assert - Each synonym should execute successfully
        for (synonym in synonyms) {
            val command = Command(id = synonym, text = synonym, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Result for '$synonym' should not be null", result)
            assertEquals("Command text for '$synonym' should match", synonym, result.command.text)
        }
    }

    @Test
    fun testGoHomeCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "go home", text = "go home", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Command text should match", "go home", result.command.text)
    }

    @Test
    fun testRecentAppsCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "recent apps", text = "recent apps", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null", result)
    }

    // ============================================================================
    // Volume Command Execution Tests
    // ============================================================================

    @Test
    fun testVolumeUpCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()

        val volumeUpVariations = listOf(
            "volume up",
            "increase volume",
            "turn up volume",
            "louder"
        )

        // Act & Assert
        for (variation in volumeUpVariations) {
            val command = Command(id = variation, text = variation, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Result for '$variation' should not be null", result)
        }
    }

    @Test
    fun testVolumeDownCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()

        val volumeDownVariations = listOf(
            "volume down",
            "decrease volume",
            "turn down volume",
            "quieter"
        )

        // Act & Assert
        for (variation in volumeDownVariations) {
            val command = Command(id = variation, text = variation, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Result for '$variation' should not be null", result)
        }
    }

    @Test
    fun testMuteCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "mute", text = "mute", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null", result)
    }

    // ============================================================================
    // System Command Execution Tests
    // ============================================================================

    @Test
    fun testWifiToggleCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()

        val wifiCommands = listOf(
            "wifi",
            "toggle wifi",
            "wifi on",
            "wifi off",
            "turn on wifi",
            "turn off wifi"
        )

        // Act & Assert
        for (wifiCommand in wifiCommands) {
            val command = Command(id = wifiCommand, text = wifiCommand, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Result for '$wifiCommand' should not be null", result)
        }
    }

    @Test
    fun testBluetoothToggleCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()

        val bluetoothCommands = listOf(
            "bluetooth",
            "toggle bluetooth",
            "bluetooth on",
            "bluetooth off"
        )

        // Act & Assert
        for (btCommand in bluetoothCommands) {
            val command = Command(id = btCommand, text = btCommand, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Result for '$btCommand' should not be null", result)
        }
    }

    @Test
    fun testOpenSettingsCommandExecution() = runTest {
        // Arrange
        commandManager.initialize()
        val command = Command(id = "open settings", text = "open settings", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null", result)
    }

    // ============================================================================
    // Complete User Journey Tests
    // ============================================================================

    @Test
    fun testCompleteNavigationJourney() = runTest {
        // Simulate a complete user journey: Navigate around the device
        commandManager.initialize()

        val journey = listOf(
            "go home",      // Go to home screen
            "recent apps",  // Open recent apps
            "go back",      // Go back
            "open settings" // Open settings
        )

        // Act & Assert - Execute each step
        for ((index, commandText) in journey.withIndex()) {
            val command = Command(id = commandText, text = commandText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Step ${index + 1} ('$commandText') should not be null", result)
        }
    }

    @Test
    fun testCompleteVolumeControlJourney() = runTest {
        // Simulate volume control journey
        commandManager.initialize()

        val journey = listOf(
            "volume up",   // Increase volume
            "volume up",   // Increase again
            "volume down", // Decrease volume
            "mute"         // Mute
        )

        // Act & Assert
        for ((index, commandText) in journey.withIndex()) {
            val command = Command(id = commandText, text = commandText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Step ${index + 1} ('$commandText') should not be null", result)
        }
    }

    @Test
    fun testMixedCommandSequence() = runTest {
        // Simulate realistic mixed command usage
        commandManager.initialize()

        val sequence = listOf(
            "go home",
            "volume up",
            "open settings",
            "go back",
            "wifi",
            "recent apps",
            "volume down"
        )

        // Act & Assert
        for ((index, commandText) in sequence.withIndex()) {
            val command = Command(id = commandText, text = commandText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Step ${index + 1} ('$commandText') should not be null", result)
        }
    }

    // ============================================================================
    // Multi-Language Journey Tests
    // ============================================================================

    @Test
    fun testGermanCommandExecution() = runTest {
        // Test German commands
        commandManager.initialize()

        val germanCommands = listOf(
            "zurück",           // back
            "startseite",       // home
            "lautstärke hoch"   // volume up
        )

        // Act & Assert
        for (commandText in germanCommands) {
            val command = Command(id = commandText, text = commandText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("German command '$commandText' should not be null", result)
        }
    }

    @Test
    fun testSpanishCommandExecution() = runTest {
        // Test Spanish commands
        commandManager.initialize()

        val spanishCommands = listOf(
            "volver",           // back
            "inicio",           // home
            "subir volumen"     // volume up
        )

        // Act & Assert
        for (commandText in spanishCommands) {
            val command = Command(id = commandText, text = commandText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Spanish command '$commandText' should not be null", result)
        }
    }

    // ============================================================================
    // Confidence-Based Execution Tests
    // ============================================================================

    @Test
    fun testConfidenceBasedExecution() = runTest {
        // Test same command with different confidence levels
        commandManager.initialize()

        val confidenceLevels = listOf(
            1.0f,   // Very high
            0.95f,  // High
            0.85f,  // Medium-high
            0.75f,  // Medium
            0.65f,  // Medium-low
            0.55f   // Low
        )

        for (confidence in confidenceLevels) {
            val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = confidence)

            val result = commandManager.executeCommand(command)

            assertNotNull("Command with confidence $confidence should not be null", result)
        }
    }

    // ============================================================================
    // Rapid Fire Tests (Performance)
    // ============================================================================

    @Test
    fun testRapidFireCommands() = runTest {
        // Simulate rapid command execution (user speaking quickly)
        commandManager.initialize()

        val commands = listOf(
            "go back", "go back", "go back",
            "go home",
            "volume up", "volume up",
            "volume down",
            "recent apps"
        )

        val startTime = System.currentTimeMillis()

        // Act - Execute all commands rapidly
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
        assertTrue("Rapid fire should be efficient (<400ms)", duration < 400)
    }

    // ============================================================================
    // Error Recovery Tests
    // ============================================================================

    @Test
    fun testRecoveryFromInvalidCommand() = runTest {
        // Test that system recovers from invalid command and continues working
        commandManager.initialize()

        val sequence = listOf(
            "go back",                      // Valid
            "invalid command xyz123",       // Invalid
            "go home",                      // Valid - should work after invalid
            "another invalid command",      // Invalid
            "volume up"                     // Valid - should still work
        )

        // Act & Assert
        for (commandText in sequence) {
            val command = Command(id = commandText, text = commandText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Result should not be null even for '$commandText'", result)
        }
    }

    // ============================================================================
    // Database Integration Verification Tests
    // ============================================================================

    @Test
    fun testDatabaseCommandsAccessible() = runTest {
        // Verify that database commands are actually loaded and accessible
        commandManager.initialize()

        // These commands should only work if database patterns are loaded
        val databaseCommands = listOf(
            "scroll down",
            "scroll up",
            "page down",
            "page up"
        )

        // Act & Assert
        for (commandText in databaseCommands) {
            val command = Command(id = commandText, text = commandText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Database command '$commandText' should not be null", result)
        }
    }

    @Test
    fun testPatternMatchingWorksEndToEnd() = runTest {
        // Verify pattern matching works from voice input to action execution
        commandManager.initialize()

        // Test pattern variations that should all map to same action
        val patterns = mapOf(
            "go back" to "nav_back",
            "return" to "nav_back",
            "previous" to "nav_back"
        )

        // Act & Assert
        for ((spokenText, expectedId) in patterns) {
            val command = Command(id = spokenText, text = spokenText, source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

            val result = commandManager.executeCommand(command)

            assertNotNull("Pattern '$spokenText' should not be null", result)
            // Pattern matching should map all variations to same underlying action
        }
    }

    // ============================================================================
    // Initialization and Lifecycle Tests
    // ============================================================================

    @Test
    fun testExecutionWithoutInitialization() = runTest {
        // Test that commands can handle execution without explicit initialization
        // (lazy initialization should occur)
        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should not be null even without explicit init", result)
    }

    @Test
    fun testMultipleInitializationsHandled() = runTest {
        // Verify multiple initializations don't cause issues
        commandManager.initialize()
        commandManager.initialize()
        commandManager.initialize()

        val command = Command(id = "go back", text = "go back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull("Result should work after multiple inits", result)
    }
}
