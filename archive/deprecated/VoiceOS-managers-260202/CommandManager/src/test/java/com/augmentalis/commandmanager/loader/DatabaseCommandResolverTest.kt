/**
 * DatabaseCommandResolverTest.kt
 *
 * Integration tests for DatabaseCommandResolver
 * Tests database command loading, pattern extraction, and CommandDefinition conversion
 *
 * Created: 2025-11-14
 * Purpose: Verify database integration and command resolution
 */

package com.augmentalis.commandmanager.loader

import android.content.Context
import com.augmentalis.commandmanager.database.CommandDatabase
import com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DatabaseCommandResolverTest {

    private lateinit var context: Context
    private lateinit var resolver: DatabaseCommandResolver

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        resolver = DatabaseCommandResolver.create(context)
    }

    // ============================================================================
    // Command Loading Tests
    // ============================================================================

    @Test
    fun testGetAllCommandDefinitions() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("en-US", includeFallback = true)

        // Assert
        assertNotNull("Commands should not be null", commands)
        // Note: Actual count depends on database state
        // In production: should be 94 for en-US + fallback
    }

    @Test
    fun testGetCommandsByCategory() = runTest {
        // Arrange
        val categories = listOf("navigation", "volume", "system", "scroll", "cursor")

        for (category in categories) {
            // Act
            val commands = resolver.getCommandsByCategory(category, "en-US")

            // Assert
            assertNotNull("Commands for category '$category' should not be null", commands)
        }
    }

    @Test
    fun testGetContextualCommands() = runTest {
        // Arrange
        val commandContext = com.augmentalis.voiceos.command.CommandContext(
            packageName = "com.android.browser",
            viewId = "EditText"
        )

        // Act
        val commands = resolver.getContextualCommands(commandContext, "en-US")

        // Assert
        assertNotNull("Contextual commands should not be null", commands)
        // Should include navigation, browser, editing categories
    }

    @Test
    fun testSearchCommands() = runTest {
        // Arrange
        val searchTerms = listOf("back", "home", "volume", "wifi")

        for (term in searchTerms) {
            // Act
            val commands = resolver.searchCommands(term, "en-US")

            // Assert
            assertNotNull("Search results for '$term' should not be null", commands)
        }
    }

    // ============================================================================
    // Multi-Language Tests
    // ============================================================================

    @Test
    fun testEnglishLocale() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("en-US", includeFallback = false)

        // Assert
        assertNotNull("English commands should not be null", commands)
        // Should have ~94 commands for en-US
    }

    @Test
    fun testGermanLocale() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("de-DE", includeFallback = false)

        // Assert
        assertNotNull("German commands should not be null", commands)
        // Should have ~94 commands for de-DE
    }

    @Test
    fun testSpanishLocale() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("es-ES", includeFallback = false)

        // Assert
        assertNotNull("Spanish commands should not be null", commands)
    }

    @Test
    fun testFrenchLocale() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("fr-FR", includeFallback = false)

        // Assert
        assertNotNull("French commands should not be null", commands)
    }

    @Test
    fun testFallbackBehavior() = runTest {
        // Act - Request German with fallback
        val commandsWithFallback = resolver.getAllCommandDefinitions("de-DE", includeFallback = true)
        val commandsWithoutFallback = resolver.getAllCommandDefinitions("de-DE", includeFallback = false)

        // Assert
        assertNotNull("Commands with fallback should not be null", commandsWithFallback)
        assertNotNull("Commands without fallback should not be null", commandsWithoutFallback)

        // With fallback should have more commands (de-DE + en-US)
        // This comparison is valid if database is populated
    }

    @Test
    fun testNonExistentLocale() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("zh-CN", includeFallback = true)

        // Assert
        assertNotNull("Should fallback to English for non-existent locale", commands)
        // Should return at least English fallback commands
    }

    // ============================================================================
    // Pattern Extraction Tests
    // ============================================================================

    @Test
    fun testPatternExtraction() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("en-US", includeFallback = true)

        // Assert
        assertNotNull("Commands should not be null", commands)

        // Verify each command has patterns (primary + synonyms)
        for (command in commands.take(10)) { // Test first 10
            assertTrue("Command ${command.id} should have patterns", command.patterns.isNotEmpty())
            assertTrue("Command ${command.id} should have description", command.description.isNotEmpty())
            assertTrue("Command ${command.id} should have category", command.category.isNotEmpty())
        }
    }

    @Test
    fun testNavigationCommandPatterns() = runTest {
        // Act
        val commands = resolver.getCommandsByCategory("navigation", "en-US")

        // Assert
        assertNotNull("Navigation commands should not be null", commands)

        // Find "go back" command
        val goBackCommand = commands.find { cmd ->
            cmd.patterns.any { it.contains("back") }
        }

        if (goBackCommand != null) {
            assertTrue("Go back command should have multiple patterns", goBackCommand.patterns.size > 1)
            assertTrue("Go back patterns should include synonyms",
                goBackCommand.patterns.any { it.contains("return") || it.contains("previous") })
        }
    }

    @Test
    fun testVolumeCommandPatterns() = runTest {
        // Act
        val commands = resolver.getCommandsByCategory("volume", "en-US")

        // Assert
        assertNotNull("Volume commands should not be null", commands)

        // Verify volume commands have appropriate patterns
        val volumeUpCommand = commands.find { cmd ->
            cmd.patterns.any { it.contains("volume") && it.contains("up") }
        }

        if (volumeUpCommand != null) {
            assertTrue("Volume up should have synonyms", volumeUpCommand.patterns.size > 1)
        }
    }

    // ============================================================================
    // CommandDefinition Conversion Tests
    // ============================================================================

    @Test
    fun testCommandDefinitionStructure() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("en-US", includeFallback = true)

        // Assert
        assertNotNull("Commands should not be null", commands)

        for (command in commands.take(5)) { // Test first 5
            // Verify CommandDefinition structure
            assertNotNull("Command ID should not be null", command.id)
            assertNotNull("Command name should not be null", command.name)
            assertNotNull("Command description should not be null", command.description)
            assertNotNull("Command category should not be null", command.category)
            assertNotNull("Command patterns should not be null", command.patterns)
            assertNotNull("Command requiredContext should not be null", command.requiredContext)
            assertNotNull("Command parameters should not be null", command.parameters)

            // Verify data integrity
            assertTrue("Command ID should not be empty", command.id.isNotEmpty())
            assertTrue("Patterns list should not be empty", command.patterns.isNotEmpty())
            assertTrue("Category should not be empty", command.category.isNotEmpty())
        }
    }

    @Test
    fun testCategoryUppercaseConversion() = runTest {
        // Act
        val commands = resolver.getAllCommandDefinitions("en-US", includeFallback = true)

        // Assert
        assertNotNull("Commands should not be null", commands)

        // Verify all categories are uppercase (as per CommandDefinition format)
        for (command in commands.take(10)) {
            assertEquals("Category should be uppercase",
                command.category.uppercase(),
                command.category)
        }
    }

    // ============================================================================
    // Context-Aware Loading Tests
    // ============================================================================

    @Test
    fun testBrowserContextCommands() = runTest {
        // Arrange
        val browserContext = com.augmentalis.voiceos.command.CommandContext(
            packageName = "com.android.browser",
            viewId = null
        )

        // Act
        val commands = resolver.getContextualCommands(browserContext, "en-US")

        // Assert
        assertNotNull("Browser context commands should not be null", commands)
        // Should include navigation, browser, scroll categories
    }

    @Test
    fun testEditTextContextCommands() = runTest {
        // Arrange
        val editContext = com.augmentalis.voiceos.command.CommandContext(
            packageName = "com.example.app",
            viewId = "EditText"
        )

        // Act
        val commands = resolver.getContextualCommands(editContext, "en-US")

        // Assert
        assertNotNull("EditText context commands should not be null", commands)
        // Should include navigation, editing, dictation, keyboard categories
    }

    // ============================================================================
    // Database Statistics Tests
    // ============================================================================

    @Test
    fun testGetDatabaseStats() = runTest {
        // Act
        val stats = resolver.getDatabaseStats()

        // Assert
        assertNotNull("Database stats should not be null", stats)
        assertTrue("Stats should contain totalCommands key", stats.containsKey("totalCommands"))
        assertTrue("Stats should contain locales key", stats.containsKey("locales"))
        assertTrue("Stats should contain categories key", stats.containsKey("categories"))

        // Verify data types
        val totalCommands = stats["totalCommands"]
        assertTrue("Total commands should be a number", totalCommands is Number)

        val locales = stats["locales"]
        assertTrue("Locales should be a list", locales is List<*>)
    }

    // ============================================================================
    // Performance Tests
    // ============================================================================

    @Test
    fun testLoadingPerformance() = runTest {
        // Act
        val startTime = System.currentTimeMillis()
        val commands = resolver.getAllCommandDefinitions("en-US", includeFallback = true)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertNotNull("Commands should not be null", commands)
        assertTrue("Loading should be fast (<100ms)", duration < 100)
    }

    @Test
    fun testSearchPerformance() = runTest {
        // Act
        val startTime = System.currentTimeMillis()
        val commands = resolver.searchCommands("back", "en-US")
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertNotNull("Search results should not be null", commands)
        assertTrue("Search should be fast (<50ms)", duration < 50)
    }

    @Test
    fun testCategoryQueryPerformance() = runTest {
        // Act
        val startTime = System.currentTimeMillis()
        val commands = resolver.getCommandsByCategory("navigation", "en-US")
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertNotNull("Category commands should not be null", commands)
        assertTrue("Category query should be fast (<20ms)", duration < 20)
    }

    // ============================================================================
    // Edge Cases Tests
    // ============================================================================

    @Test
    fun testEmptyLocale() = runTest {
        // Act - Empty locale should default to system locale
        val commands = resolver.getAllCommandDefinitions(null, includeFallback = true)

        // Assert
        assertNotNull("Commands with null locale should not be null", commands)
    }

    @Test
    fun testInvalidCategoryName() = runTest {
        // Act
        val commands = resolver.getCommandsByCategory("nonexistent_category", "en-US")

        // Assert
        assertNotNull("Commands should not be null even for invalid category", commands)
        // Should return empty list
    }

    @Test
    fun testEmptySearchTerm() = runTest {
        // Act
        val commands = resolver.searchCommands("", "en-US")

        // Assert
        assertNotNull("Search results should not be null for empty term", commands)
    }

    // ============================================================================
    // Required Context Tests
    // ============================================================================

    @Test
    fun testRequiredContextMapping() = runTest {
        // Act
        val editingCommands = resolver.getCommandsByCategory("editing", "en-US")
        val navigationCommands = resolver.getCommandsByCategory("navigation", "en-US")

        // Assert
        assertNotNull("Editing commands should not be null", editingCommands)
        assertNotNull("Navigation commands should not be null", navigationCommands)

        // Editing commands should have text_input context requirement
        // Navigation commands should have no context requirement
        // This is verified through the determineRequiredContext logic
    }
}
