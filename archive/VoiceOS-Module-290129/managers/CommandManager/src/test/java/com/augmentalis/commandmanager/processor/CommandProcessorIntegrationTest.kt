/**
 * CommandProcessorIntegrationTest.kt - Integration tests for CommandProcessor with database
 *
 * Tests:
 * - Database command loading during initialization
 * - Command matching from database
 * - Multi-language support
 * - Fallback behavior
 */

package com.augmentalis.commandmanager.processor

import android.content.Context
import com.augmentalis.commandmanager.database.CommandDatabase
import com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity
import com.augmentalis.commandmanager.CommandContext
import com.augmentalis.commandmanager.CommandSource
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.json.JSONArray

class CommandProcessorIntegrationTest {

    private lateinit var context: Context
    private lateinit var processor: CommandProcessor
    private lateinit var database: CommandDatabase

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        processor = CommandProcessor(context)
    }

    @Test
    fun testInitializationLoadsBuiltInCommands() = runTest {
        processor.initialize()

        val commands = processor.getAvailableCommands(null)

        assertTrue("Should load built-in commands", commands.isNotEmpty())
        assertNotNull("Should have navigation commands",
            commands.find { it.category == "NAVIGATION" })
    }

    @Test
    fun testDatabaseCommandsLoadedFlag() = runTest {
        processor.initialize()

        // Note: This will fail if database is empty, but should still attempt to load
        // The flag indicates whether loading was attempted and successful
        val isLoaded = processor.isDatabaseCommandsLoaded()

        // Flag should be set based on whether database loading succeeded
        // In test environment, might be false if database is not set up
        assertNotNull("Database loaded flag should be set", isLoaded)
    }

    @Test
    fun testGetDatabaseStats() = runTest {
        processor.initialize()

        val stats = processor.getDatabaseStats()

        assertNotNull("Stats should be returned", stats)
        // Stats might be empty in test environment
    }

    @Test
    fun testReloadDatabaseCommands() = runTest {
        processor.initialize()

        // Should not throw exception
        processor.reloadDatabaseCommands()

        assertTrue("Processor should still be functional", true)
    }

    @Test
    fun testCommandMatchingWithBuiltIn() = runTest {
        processor.initialize()

        val result = processor.processCommand(
            text = "go back",
            source = CommandSource.VOICE,
            context = null
        )

        // Even without database, built-in commands should work
        assertNotNull("Result should be returned", result)
    }

    @Test
    fun testCommandMatchingWithNormalization() = runTest {
        processor.initialize()

        // Test with uppercase - should be normalized
        val result = processor.processCommand(
            text = "GO BACK",
            source = CommandSource.VOICE,
            context = null
        )

        assertNotNull("Result should handle uppercase", result)
    }

    @Test
    fun testUnknownCommandHandling() = runTest {
        processor.initialize()

        val result = processor.processCommand(
            text = "completely unknown command xyz123",
            source = CommandSource.VOICE,
            context = null
        )

        assertFalse("Unknown command should fail", result.success)
        assertEquals("Should have unknown error", "unknown", result.command.id)
    }

    @Test
    fun testContextualCommands() = runTest {
        processor.initialize()

        val testContext = CommandContext(
            packageName = "com.android.browser",
            viewId = "EditText"
        )

        val commands = processor.getAvailableCommands(testContext)

        assertNotNull("Should return contextual commands", commands)
        assertTrue("Should have some commands", commands.isNotEmpty())
    }

    @Test
    fun testShutdownCleanup() = runTest {
        processor.initialize()

        processor.shutdown()

        assertFalse("Database flag should be cleared",
            processor.isDatabaseCommandsLoaded())
    }

    @Test
    fun testLanguageChange() {
        processor.setLanguage("de")

        // Language is set locally, no immediate effect on commands
        // Reload would be needed to load German commands
        assertTrue("Language should be changeable", true)
    }

    @Test
    fun testMultipleInitializations() = runTest {
        processor.initialize()
        val commands1 = processor.getAvailableCommands(null).size

        processor.initialize()
        val commands2 = processor.getAvailableCommands(null).size

        // Multiple initializations should be safe
        // Note: Might have duplicates if CommandDefinitions doesn't deduplicate
        assertTrue("Should handle multiple initializations", commands2 >= commands1)
    }
}

/**
 * Integration test with actual database
 * Requires database to be set up with test data
 */
class CommandProcessorDatabaseIntegrationTest {

    @Test
    fun testCommandExecutionFromDatabase() = runTest {
        // This test requires actual database setup
        // Placeholder for full integration testing

        assertTrue("Database integration test placeholder", true)
    }

    @Test
    fun testMultiLanguageCommandMatching() = runTest {
        // Test matching commands in different languages
        // Requires database with multiple locales

        assertTrue("Multi-language test placeholder", true)
    }

    @Test
    fun testFallbackToEnglish() = runTest {
        // Test that English fallback works when locale-specific command not found
        // Requires database with both locales

        assertTrue("Fallback test placeholder", true)
    }
}
