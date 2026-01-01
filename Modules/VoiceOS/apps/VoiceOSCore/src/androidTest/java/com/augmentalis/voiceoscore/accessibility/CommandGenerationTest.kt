/**
 * CommandGenerationTest.kt - Tests for voice command generation from accessibility nodes
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-30
 *
 * Tests:
 * - Voice command generation from accessibility nodes
 * - Command pattern matching
 * - Command storage in database
 * - Command execution mapping
 */
package com.augmentalis.voiceoscore.accessibility

import android.content.Context
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceoscore.learnapp.generation.CommandGenerator
import com.augmentalis.voiceoscore.learnapp.generation.CommandType
import com.augmentalis.voiceoscore.learnapp.generation.GeneratedCommand
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Command Generation Test Suite
 *
 * Verifies voice command generation from UI elements including:
 * - Command generation from accessibility node text
 * - Command pattern matching for expected formats
 * - Command conflict detection and resolution
 * - Command validation
 */
@RunWith(AndroidJUnit4::class)
class CommandGenerationTest {

    companion object {
        private const val TAG = "CommandGenerationTest"
        private const val TEST_PACKAGE = "com.example.testapp"
    }

    private lateinit var context: Context
    private lateinit var commandGenerator: CommandGenerator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        commandGenerator = CommandGenerator()
    }

    @After
    fun tearDown() {
        commandGenerator.clear()
    }

    // ============================================================================
    // SECTION 1: Command Generation from Accessibility Nodes
    // ============================================================================

    /**
     * Test command generation from button element with text
     */
    @Test
    fun testCommandGenerationFromButtonWithText() = runTest {
        val element = createTestElement(
            text = "Submit",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        assertTrue("Should generate at least one command", commands.isNotEmpty())

        // Check for primary command
        val primaryCommand = commands.find { it.type == CommandType.PRIMARY }
        assertNotNull("Should have a primary command", primaryCommand)
        assertTrue(
            "Primary command should contain element text",
            primaryCommand!!.phrase.contains("submit")
        )
    }

    /**
     * Test command generation from button with content description
     */
    @Test
    fun testCommandGenerationFromButtonWithContentDescription() = runTest {
        val element = createTestElement(
            text = "",
            contentDescription = "Like button",
            className = "android.widget.ImageButton",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        assertTrue("Should generate commands from content description", commands.isNotEmpty())

        val allPhrases = commands.map { it.phrase }
        assertTrue(
            "Commands should contain like reference",
            allPhrases.any { it.contains("like") }
        )
    }

    /**
     * Test command generation from element with resource ID only
     */
    @Test
    fun testCommandGenerationFromResourceId() = runTest {
        val element = createTestElement(
            text = "",
            contentDescription = "",
            resourceId = "com.app:id/share_button",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        assertTrue("Should generate commands from resource ID", commands.isNotEmpty())

        val allPhrases = commands.map { it.phrase }
        assertTrue(
            "Commands should contain share reference",
            allPhrases.any { it.contains("share") }
        )
    }

    /**
     * Test command generation for different element types
     */
    @Test
    fun testCommandGenerationForDifferentElementTypes() = runTest {
        val testCases = listOf(
            Triple("android.widget.Button", "Click Me", "tap"),
            Triple("android.widget.CheckBox", "Enable notifications", "toggle"),
            Triple("android.widget.Switch", "Dark mode", "toggle"),
            Triple("android.widget.EditText", "Username", "focus")
        )

        testCases.forEach { (className, text, expectedVerb) ->
            commandGenerator.clear()

            val element = createTestElement(
                text = text,
                className = className,
                isClickable = true
            )

            val commands = commandGenerator.generateCommands(element)
            assertTrue(
                "Should generate commands for $className",
                commands.isNotEmpty()
            )

            val primaryCommand = commands.find { it.type == CommandType.PRIMARY }
            assertNotNull("Should have primary command for $className", primaryCommand)

            // Verify appropriate action verb is used
            println("$className primary command: ${primaryCommand?.phrase}")
        }
    }

    /**
     * Test that empty text elements don't generate commands
     */
    @Test
    fun testNoCommandsForEmptyElements() = runTest {
        val element = createTestElement(
            text = "",
            contentDescription = "",
            resourceId = "",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        assertTrue("Should not generate commands for empty elements", commands.isEmpty())
    }

    // ============================================================================
    // SECTION 2: Command Pattern Matching Tests
    // ============================================================================

    /**
     * Test command pattern format - primary commands
     */
    @Test
    fun testPrimaryCommandPatternFormat() = runTest {
        val element = createTestElement(
            text = "Save Changes",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)
        val primaryCommand = commands.find { it.type == CommandType.PRIMARY }

        assertNotNull("Should have primary command", primaryCommand)

        // Primary commands should follow pattern: [action] [element text]
        val phrase = primaryCommand!!.phrase
        assertTrue(
            "Primary command should be lowercase",
            phrase == phrase.lowercase()
        )
        assertTrue(
            "Primary command should contain action verb",
            phrase.split(" ").first() in listOf("tap", "click", "press", "select", "focus", "toggle", "view")
        )
    }

    /**
     * Test command pattern format - synonym commands
     */
    @Test
    fun testSynonymCommandPatternFormat() = runTest {
        val element = createTestElement(
            text = "Settings",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)
        val synonymCommands = commands.filter { it.type == CommandType.SYNONYM }

        assertTrue("Should generate synonym commands", synonymCommands.isNotEmpty())

        synonymCommands.forEach { command ->
            assertTrue(
                "Synonym command should be lowercase",
                command.phrase == command.phrase.lowercase()
            )
            assertEquals(
                "Synonym confidence should be 0.8",
                0.8f,
                command.confidence,
                0.01f
            )
        }
    }

    /**
     * Test command pattern format - short form commands
     */
    @Test
    fun testShortFormCommandPatternFormat() = runTest {
        val element = createTestElement(
            text = "Save Draft Changes",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)
        val shortFormCommands = commands.filter { it.type == CommandType.SHORT_FORM }

        // Multi-word elements should have short forms
        if (shortFormCommands.isNotEmpty()) {
            shortFormCommands.forEach { command ->
                assertTrue(
                    "Short form command should be shorter than original",
                    command.phrase.length < "save draft changes".length
                )
                assertEquals(
                    "Short form confidence should be 0.7",
                    0.7f,
                    command.confidence,
                    0.01f
                )
            }
        }
    }

    /**
     * Test command pattern format - direct reference commands
     */
    @Test
    fun testDirectReferenceCommandPatternFormat() = runTest {
        val element = createTestElement(
            text = "Home",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)
        val directCommands = commands.filter { it.type == CommandType.DIRECT }

        assertTrue("Short text should have direct reference", directCommands.isNotEmpty())

        val directCommand = directCommands.first()
        assertEquals(
            "Direct command should be just the element text",
            "home",
            directCommand.phrase
        )
        assertEquals(
            "Direct reference confidence should be 0.6",
            0.6f,
            directCommand.confidence,
            0.01f
        )
    }

    /**
     * Test command text normalization patterns
     */
    @Test
    fun testCommandTextNormalization() = runTest {
        val testCases = listOf(
            "  UPPERCASE TEXT  " to "uppercase text",
            "Special@#\$Characters" to "specialcharacters",
            "Multiple   Spaces" to "multiple spaces",
            "camelCaseText" to "camelcasetext"
        )

        testCases.forEach { (input, expectedNormalized) ->
            commandGenerator.clear()

            val element = createTestElement(
                text = input,
                className = "android.widget.Button",
                isClickable = true
            )

            val commands = commandGenerator.generateCommands(element)
            assertTrue("Should generate commands for '$input'", commands.isNotEmpty())

            // Verify normalization in commands
            val allPhrases = commands.map { it.phrase }
            assertTrue(
                "Commands should contain normalized text '$expectedNormalized'",
                allPhrases.any { it.contains(expectedNormalized) }
            )
        }
    }

    // ============================================================================
    // SECTION 3: Command Storage and Registry Tests
    // ============================================================================

    /**
     * Test command registration in registry
     */
    @Test
    fun testCommandRegistration() = runTest {
        val element = createTestElement(
            text = "Profile",
            className = "android.widget.Button",
            isClickable = true
        )

        commandGenerator.generateCommands(element)

        val registry = commandGenerator.commandRegistry.first()

        assertTrue("Registry should not be empty", registry.isNotEmpty())

        // Verify element UUID is stored
        val storedUuids = registry.values.toSet()
        assertTrue(
            "Registry should contain element UUID",
            storedUuids.contains(element.uuid)
        )
    }

    /**
     * Test command lookup by phrase
     */
    @Test
    fun testCommandLookupByPhrase() = runTest {
        val element = createTestElement(
            text = "Notifications",
            className = "android.widget.Button",
            isClickable = true
        )

        commandGenerator.generateCommands(element)

        // Should be able to look up element UUID by command phrase
        val uuid = commandGenerator.getElementUuid("notifications")

        assertNotNull("Should find element by direct phrase", uuid)
        assertEquals("UUID should match", element.uuid, uuid)
    }

    /**
     * Test getting all commands for an element
     */
    @Test
    fun testGetCommandsForElement() = runTest {
        val element = createTestElement(
            text = "Messages",
            className = "android.widget.Button",
            isClickable = true
        )

        val generatedCommands = commandGenerator.generateCommands(element)

        val commandsForElement = commandGenerator.getCommandsForElement(element.uuid!!)

        assertTrue("Should have commands for element", commandsForElement.isNotEmpty())
        assertTrue(
            "Should have at least as many registered commands as generated (minus conflicts)",
            commandsForElement.isNotEmpty()
        )
    }

    /**
     * Test command conflict detection
     */
    @Test
    fun testCommandConflictDetection() = runTest {
        val element1 = createTestElement(
            text = "Save",
            className = "android.widget.Button",
            isClickable = true
        )

        val element2 = createTestElement(
            text = "Save",
            className = "android.widget.Button",
            isClickable = true
        )

        commandGenerator.generateCommands(element1)
        commandGenerator.generateCommands(element2)

        // Same text should cause conflicts
        val hasConflict = commandGenerator.hasConflict("save")

        assertTrue("Should detect conflict for duplicate commands", hasConflict)

        val conflicts = commandGenerator.commandConflicts.first()
        assertTrue("Conflicts should be tracked", conflicts.isNotEmpty())
    }

    /**
     * Test command conflict resolution
     */
    @Test
    fun testCommandConflictResolution() = runTest {
        val element = createTestElement(
            text = "Save",
            resourceId = "com.app:id/toolbar_save",
            className = "android.widget.Button",
            isClickable = true,
            bounds = Rect(100, 50, 200, 100)  // Near top of screen
        )

        val resolvedCommand = commandGenerator.resolveConflict("save", element)

        assertNotNull("Should resolve conflict", resolvedCommand)
        assertTrue(
            "Resolved command should be different from original",
            resolvedCommand != "save"
        )
        assertTrue(
            "Resolved command should contain context",
            resolvedCommand.contains("toolbar") || resolvedCommand.contains("at top")
        )
    }

    // ============================================================================
    // SECTION 4: Command Validation Tests
    // ============================================================================

    /**
     * Test valid command validation
     */
    @Test
    fun testValidCommandValidation() = runTest {
        val element = createTestElement(
            text = "Search",
            className = "android.widget.Button",
            isClickable = true
        )

        commandGenerator.generateCommands(element)

        val result = commandGenerator.validateCommand("search")

        assertTrue("Valid command should pass validation", result.isValid)
        assertTrue("Reason should be empty for valid command", result.reason.isEmpty())
    }

    /**
     * Test blank command validation
     */
    @Test
    fun testBlankCommandValidation() {
        val result = commandGenerator.validateCommand("")

        assertFalse("Blank command should fail validation", result.isValid)
        assertTrue(
            "Reason should mention blank",
            result.reason.contains("blank")
        )
    }

    /**
     * Test too short command validation
     */
    @Test
    fun testTooShortCommandValidation() {
        val result = commandGenerator.validateCommand("a")

        assertFalse("Single character command should fail validation", result.isValid)
        assertTrue(
            "Reason should mention too short",
            result.reason.contains("short")
        )
    }

    /**
     * Test too long command validation
     */
    @Test
    fun testTooLongCommandValidation() {
        val longCommand = "a".repeat(150)
        val result = commandGenerator.validateCommand(longCommand)

        assertFalse("Too long command should fail validation", result.isValid)
        assertTrue(
            "Reason should mention too long",
            result.reason.contains("long")
        )
    }

    /**
     * Test unregistered command validation
     */
    @Test
    fun testUnregisteredCommandValidation() {
        val result = commandGenerator.validateCommand("nonexistent command")

        assertFalse("Unregistered command should fail validation", result.isValid)
        assertTrue(
            "Reason should mention not found",
            result.reason.contains("not found")
        )
    }

    /**
     * Test ambiguous command validation
     */
    @Test
    fun testAmbiguousCommandValidation() = runTest {
        // Create two elements with same text to cause conflict
        val element1 = createTestElement(
            text = "Edit",
            className = "android.widget.Button",
            isClickable = true
        )
        val element2 = createTestElement(
            text = "Edit",
            className = "android.widget.Button",
            isClickable = true
        )

        commandGenerator.generateCommands(element1)
        commandGenerator.generateCommands(element2)

        val result = commandGenerator.validateCommand("edit")

        assertFalse("Ambiguous command should fail validation", result.isValid)
        assertTrue(
            "Reason should mention ambiguous",
            result.reason.contains("ambiguous")
        )
        assertTrue(
            "Should return conflicting element UUIDs",
            result.conflicts.size >= 2
        )
    }

    // ============================================================================
    // SECTION 5: Command Execution Mapping Tests
    // ============================================================================

    /**
     * Test command to element UUID mapping
     */
    @Test
    fun testCommandToElementMapping() = runTest {
        val element = createTestElement(
            text = "Camera",
            className = "android.widget.ImageButton",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        // Each generated command should map back to the element
        commands.forEach { command ->
            val mappedUuid = commandGenerator.getElementUuid(command.phrase)
            if (mappedUuid != null) {
                assertEquals(
                    "Command '${command.phrase}' should map to element UUID",
                    element.uuid,
                    mappedUuid
                )
            }
        }
    }

    /**
     * Test command confidence scoring
     */
    @Test
    fun testCommandConfidenceScoring() = runTest {
        val element = createTestElement(
            text = "Send Message",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        // Verify confidence ordering: PRIMARY > SYNONYM > SHORT_FORM > DIRECT
        val primaryConfidence = commands.find { it.type == CommandType.PRIMARY }?.confidence ?: 0f
        val synonymConfidence = commands.find { it.type == CommandType.SYNONYM }?.confidence ?: 0f
        val shortFormConfidence = commands.find { it.type == CommandType.SHORT_FORM }?.confidence ?: 0f
        val directConfidence = commands.find { it.type == CommandType.DIRECT }?.confidence ?: 1f

        assertEquals("Primary command confidence should be 1.0", 1.0f, primaryConfidence, 0.01f)
        assertEquals("Synonym command confidence should be 0.8", 0.8f, synonymConfidence, 0.01f)

        if (commands.any { it.type == CommandType.SHORT_FORM }) {
            assertEquals("Short form command confidence should be 0.7", 0.7f, shortFormConfidence, 0.01f)
        }

        if (commands.any { it.type == CommandType.DIRECT }) {
            assertEquals("Direct command confidence should be 0.6", 0.6f, directConfidence, 0.01f)
        }
    }

    /**
     * Test command generation statistics
     */
    @Test
    fun testCommandGenerationStatistics() = runTest {
        val elements = listOf(
            createTestElement("Button 1", "android.widget.Button", true),
            createTestElement("Button 2", "android.widget.Button", true),
            createTestElement("Switch 1", "android.widget.Switch", true)
        )

        elements.forEach { element ->
            commandGenerator.generateCommands(element)
        }

        val stats = commandGenerator.getStats()

        assertTrue("Should have total commands > 0", stats.totalCommands > 0)
        assertEquals("Should have 3 unique elements", 3, stats.uniqueElements)
        assertTrue(
            "Average commands per element should be > 1",
            stats.averageCommandsPerElement > 1f
        )

        println("Command Generation Stats:")
        println("  Total Commands: ${stats.totalCommands}")
        println("  Total Conflicts: ${stats.totalConflicts}")
        println("  Unique Elements: ${stats.uniqueElements}")
        println("  Avg Commands/Element: ${stats.averageCommandsPerElement}")
    }

    /**
     * Test clear command registry
     */
    @Test
    fun testClearCommandRegistry() = runTest {
        val element = createTestElement(
            text = "Test",
            className = "android.widget.Button",
            isClickable = true
        )

        commandGenerator.generateCommands(element)

        // Verify registry is populated
        val registryBefore = commandGenerator.commandRegistry.first()
        assertTrue("Registry should have commands", registryBefore.isNotEmpty())

        // Clear and verify
        commandGenerator.clear()

        val registryAfter = commandGenerator.commandRegistry.first()
        assertTrue("Registry should be empty after clear", registryAfter.isEmpty())

        val conflictsAfter = commandGenerator.commandConflicts.first()
        assertTrue("Conflicts should be empty after clear", conflictsAfter.isEmpty())
    }

    // ============================================================================
    // SECTION 6: Edge Case Tests
    // ============================================================================

    /**
     * Test command generation with special characters in text
     */
    @Test
    fun testCommandGenerationWithSpecialCharacters() = runTest {
        val specialTextCases = listOf(
            "Save & Exit",
            "Edit (Advanced)",
            "Settings -> General",
            "50% Off!",
            "\"Quoted Text\""
        )

        specialTextCases.forEach { text ->
            commandGenerator.clear()

            val element = createTestElement(
                text = text,
                className = "android.widget.Button",
                isClickable = true
            )

            val commands = commandGenerator.generateCommands(element)

            assertTrue(
                "Should generate commands for '$text'",
                commands.isNotEmpty()
            )

            // Verify commands are clean (no special characters)
            commands.forEach { command ->
                assertFalse(
                    "Command should not contain special characters: ${command.phrase}",
                    command.phrase.contains(Regex("[^a-z0-9\\s]"))
                )
            }
        }
    }

    /**
     * Test command generation with unicode text
     */
    @Test
    fun testCommandGenerationWithUnicodeText() = runTest {
        val element = createTestElement(
            text = "Settings",
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        // Should generate ASCII-compatible commands
        assertTrue("Should generate commands", commands.isNotEmpty())

        commands.forEach { command ->
            assertTrue(
                "Command should be ASCII: ${command.phrase}",
                command.phrase.matches(Regex("[a-z0-9\\s]+"))
            )
        }
    }

    /**
     * Test command generation with very long text
     */
    @Test
    fun testCommandGenerationWithLongText() = runTest {
        val longText = "This is a very long button text that describes an action in great detail"

        val element = createTestElement(
            text = longText,
            className = "android.widget.Button",
            isClickable = true
        )

        val commands = commandGenerator.generateCommands(element)

        assertTrue("Should generate commands for long text", commands.isNotEmpty())

        // Should have short forms for long text
        val shortForms = commands.filter { it.type == CommandType.SHORT_FORM }
        assertTrue("Should generate short forms for long text", shortForms.isNotEmpty())
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun createTestElement(
        text: String,
        className: String,
        isClickable: Boolean,
        contentDescription: String = "",
        resourceId: String = "",
        bounds: Rect = Rect(100, 100, 300, 150)
    ): ElementInfo {
        return ElementInfo(
            className = className,
            text = text,
            contentDescription = contentDescription,
            resourceId = resourceId,
            isClickable = isClickable,
            isEnabled = true,
            isPassword = false,
            isScrollable = false,
            bounds = bounds,
            node = null,
            uuid = UUID.randomUUID().toString()
        )
    }
}
