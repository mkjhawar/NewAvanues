/**
 * DynamicCommandRegistryTest.kt - Comprehensive unit tests for DynamicCommandRegistry
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Tests all aspects of the dynamic command system
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.dynamic

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DynamicCommandRegistryTest {

    private lateinit var registry: DynamicCommandRegistry
    private var testCommandIdCounter = 0

    @Before
    fun setup() {
        registry = DynamicCommandRegistry(maxCommandsPerNamespace = 100)
        testCommandIdCounter = 0
    }

    @After
    fun tearDown() {
        // Cleanup
    }

    // Helper function to create test commands
    private fun createTestCommand(
        id: String = "test_command_${testCommandIdCounter++}",
        phrases: List<String> = listOf("test phrase"),
        priority: Int = 50,
        namespace: String = "default",
        enabled: Boolean = true
    ): VoiceCommand {
        return VoiceCommand(
            id = id,
            phrases = phrases,
            priority = priority,
            namespace = namespace,
            enabled = enabled,
            action = { CommandResult.Success }
        )
    }

    // ========== Registration Tests ==========

    @Test
    fun `test register single command successfully`() = runTest {
        val command = createTestCommand(
            id = "cmd_001",
            phrases = listOf("hello world")
        )

        val result = registry.registerCommand(command)
        assertTrue("Command should register successfully", result.isSuccess)

        val allCommands = registry.getAllCommands()
        assertEquals("Should have 1 command", 1, allCommands.size)
        assertEquals("Command ID should match", "cmd_001", allCommands[0].id)
    }

    @Test
    fun `test register multiple commands in same namespace`() = runTest {
        val commands = listOf(
            createTestCommand(id = "cmd_001", phrases = listOf("open app")),
            createTestCommand(id = "cmd_002", phrases = listOf("close app")),
            createTestCommand(id = "cmd_003", phrases = listOf("go back"))
        )

        commands.forEach { cmd ->
            val result = registry.registerCommand(cmd)
            assertTrue("Each command should register", result.isSuccess)
        }

        val allCommands = registry.getAllCommands()
        assertEquals("Should have 3 commands", 3, allCommands.size)
    }

    @Test
    fun `test register command with duplicate ID fails`() = runTest {
        val command1 = createTestCommand(id = "duplicate", phrases = listOf("first"))
        val command2 = createTestCommand(id = "duplicate", phrases = listOf("second"))

        val result1 = registry.registerCommand(command1)
        assertTrue("First registration should succeed", result1.isSuccess)

        val result2 = registry.registerCommand(command2)
        assertTrue("Duplicate registration should fail", result2.isFailure)
    }

    @Test
    fun `test register command in different namespaces`() = runTest {
        val cmd1 = createTestCommand(id = "cmd_001", namespace = "namespace1")
        val cmd2 = createTestCommand(id = "cmd_002", namespace = "namespace2")

        registry.registerCommand(cmd1)
        registry.registerCommand(cmd2)

        val ns1Commands = registry.getAllCommands(namespace = "namespace1")
        val ns2Commands = registry.getAllCommands(namespace = "namespace2")

        assertEquals("Namespace1 should have 1 command", 1, ns1Commands.size)
        assertEquals("Namespace2 should have 1 command", 1, ns2Commands.size)
    }

    @Test
    fun `test register command exceeding namespace capacity fails`() = runTest {
        val smallRegistry = DynamicCommandRegistry(maxCommandsPerNamespace = 2)

        val cmd1 = createTestCommand(id = "cmd_001")
        val cmd2 = createTestCommand(id = "cmd_002")
        val cmd3 = createTestCommand(id = "cmd_003")

        assertTrue(smallRegistry.registerCommand(cmd1).isSuccess)
        assertTrue(smallRegistry.registerCommand(cmd2).isSuccess)
        assertTrue("Third command should fail due to capacity",
            smallRegistry.registerCommand(cmd3).isFailure)
    }

    // ========== Unregistration Tests ==========

    @Test
    fun `test unregister existing command successfully`() = runTest {
        val command = createTestCommand(id = "cmd_001")
        registry.registerCommand(command)

        val result = registry.unregisterCommand("cmd_001")
        assertTrue("Unregister should succeed", result.isSuccess)

        val allCommands = registry.getAllCommands()
        assertEquals("Should have 0 commands after unregister", 0, allCommands.size)
    }

    @Test
    fun `test unregister non-existent command fails`() = runTest {
        val result = registry.unregisterCommand("nonexistent")
        assertTrue("Unregister of non-existent command should fail", result.isFailure)
    }

    @Test
    fun `test unregister from specific namespace`() = runTest {
        val cmd1 = createTestCommand(id = "cmd_001", namespace = "ns1")
        val cmd2 = createTestCommand(id = "cmd_002", namespace = "ns2")

        registry.registerCommand(cmd1)
        registry.registerCommand(cmd2)

        registry.unregisterCommand("cmd_001", namespace = "ns1")

        val ns1Commands = registry.getAllCommands(namespace = "ns1")
        val ns2Commands = registry.getAllCommands(namespace = "ns2")

        assertEquals("NS1 should be empty", 0, ns1Commands.size)
        assertEquals("NS2 should still have 1 command", 1, ns2Commands.size)
    }

    // ========== Command Resolution Tests ==========

    @Test
    fun `test resolve exact phrase match`() = runTest {
        val command = createTestCommand(
            id = "cmd_001",
            phrases = listOf("open calculator", "calculator"),
            priority = 50
        )
        registry.registerCommand(command)

        val matches = registry.resolveCommand("open calculator")
        assertEquals("Should find 1 match", 1, matches.size)
        assertEquals("Should match correct command", "cmd_001", matches[0].id)
    }

    @Test
    fun `test resolve returns highest priority command first`() = runTest {
        val cmd1 = createTestCommand(
            id = "low_priority",
            phrases = listOf("go home"),
            priority = 30
        )
        val cmd2 = createTestCommand(
            id = "high_priority",
            phrases = listOf("go home"),
            priority = 80
        )

        registry.registerCommand(cmd1, checkConflicts = false)
        registry.registerCommand(cmd2, checkConflicts = false)

        val matches = registry.resolveCommand("go home")
        assertTrue("Should have at least 1 match", matches.isNotEmpty())
        assertEquals("Highest priority should be first", "high_priority", matches[0].id)
    }

    @Test
    fun `test resolve with namespace filter`() = runTest {
        val cmd1 = createTestCommand(
            id = "cmd_ns1",
            phrases = listOf("test"),
            namespace = "namespace1"
        )
        val cmd2 = createTestCommand(
            id = "cmd_ns2",
            phrases = listOf("test"),
            namespace = "namespace2"
        )

        registry.registerCommand(cmd1, checkConflicts = false)
        registry.registerCommand(cmd2, checkConflicts = false)

        val matches = registry.resolveCommand("test", namespace = "namespace1")
        assertEquals("Should find 1 match in namespace1", 1, matches.size)
        assertEquals("Should match namespace1 command", "cmd_ns1", matches[0].id)
    }

    @Test
    fun `test resolve returns empty for no match`() = runTest {
        val command = createTestCommand(phrases = listOf("hello"))
        registry.registerCommand(command)

        val matches = registry.resolveCommand("goodbye")
        assertEquals("Should find no matches", 0, matches.size)
    }

    @Test
    fun `test resolve ignores disabled commands`() = runTest {
        val command = createTestCommand(
            id = "disabled_cmd",
            phrases = listOf("test"),
            enabled = false
        )
        registry.registerCommand(command)

        val matches = registry.resolveCommand("test", enabledOnly = true)
        assertEquals("Should not find disabled command", 0, matches.size)
    }

    // ========== Fuzzy Matching Tests ==========

    @Test
    fun `test find similar commands with fuzzy matching`() = runTest {
        val command = createTestCommand(
            id = "cmd_001",
            phrases = listOf("open calculator")
        )
        registry.registerCommand(command)

        val similar = registry.findSimilarCommands(
            phrase = "open calcuator", // Typo
            minSimilarity = 0.8f
        )

        assertTrue("Should find similar command", similar.isNotEmpty())
        assertTrue("Similarity should be high", similar[0].second >= 0.8f)
    }

    @Test
    fun `test find similar commands respects min similarity`() = runTest {
        val command = createTestCommand(
            id = "cmd_001",
            phrases = listOf("completely different phrase")
        )
        registry.registerCommand(command)

        val similar = registry.findSimilarCommands(
            phrase = "abc",
            minSimilarity = 0.8f
        )

        assertEquals("Should find no similar commands", 0, similar.size)
    }

    // ========== Conflict Detection Tests ==========

    @Test
    fun `test detect exact phrase conflict`() = runTest {
        val cmd1 = createTestCommand(
            id = "cmd_001",
            phrases = listOf("go back"),
            priority = 50
        )
        registry.registerCommand(cmd1)

        val cmd2 = createTestCommand(
            id = "cmd_002",
            phrases = listOf("go back"),
            priority = 50
        )

        val conflicts = registry.detectConflicts(cmd2)
        assertTrue("Should detect conflict", conflicts.isNotEmpty())
        assertEquals("Should be exact match conflict",
            ConflictType.EXACT_MATCH, conflicts[0].conflictType)
    }

    @Test
    fun `test critical conflict prevents registration`() = runTest {
        val cmd1 = createTestCommand(
            id = "cmd_001",
            phrases = listOf("test phrase"),
            priority = 50
        )
        registry.registerCommand(cmd1)

        val cmd2 = createTestCommand(
            id = "cmd_002",
            phrases = listOf("test phrase"),
            priority = 50 // Same priority = critical conflict
        )

        val result = registry.registerCommand(cmd2, checkConflicts = true)
        assertTrue("Critical conflict should prevent registration", result.isFailure)
    }

    @Test
    fun `test non-critical conflict allows registration`() = runTest {
        val cmd1 = createTestCommand(
            id = "cmd_001",
            phrases = listOf("test phrase"),
            priority = 30
        )
        registry.registerCommand(cmd1)

        val cmd2 = createTestCommand(
            id = "cmd_002",
            phrases = listOf("test phrase"),
            priority = 80 // Different priority = non-critical
        )

        val result = registry.registerCommand(cmd2, checkConflicts = true)
        assertTrue("Non-critical conflict should allow registration", result.isSuccess)
    }

    // ========== Priority Management Tests ==========

    @Test
    fun `test update command priority`() = runTest {
        val command = createTestCommand(id = "cmd_001", priority = 50)
        registry.registerCommand(command)

        val result = registry.updateCommandPriority("cmd_001", 80)
        assertTrue("Priority update should succeed", result.isSuccess)

        val retrieved = registry.getAllCommands()[0]
        assertEquals("Priority should be updated", 80, retrieved.priority)
    }

    @Test
    fun `test update priority with invalid value fails`() = runTest {
        val command = createTestCommand(id = "cmd_001")
        registry.registerCommand(command)

        val result = registry.updateCommandPriority("cmd_001", 150) // Out of range
        assertTrue("Invalid priority should fail", result.isFailure)
    }

    // ========== Enable/Disable Tests ==========

    @Test
    fun `test enable and disable command`() = runTest {
        val command = createTestCommand(id = "cmd_001", enabled = true)
        registry.registerCommand(command)

        registry.setCommandEnabled("cmd_001", false)
        var retrieved = registry.getAllCommands()[0]
        assertFalse("Command should be disabled", retrieved.enabled)

        registry.setCommandEnabled("cmd_001", true)
        retrieved = registry.getAllCommands()[0]
        assertTrue("Command should be enabled", retrieved.enabled)
    }

    // ========== Namespace Management Tests ==========

    @Test
    fun `test clear namespace`() = runTest {
        val cmd1 = createTestCommand(id = "cmd_001", namespace = "test_ns")
        val cmd2 = createTestCommand(id = "cmd_002", namespace = "test_ns")
        val cmd3 = createTestCommand(id = "cmd_003", namespace = "other_ns")

        registry.registerCommand(cmd1)
        registry.registerCommand(cmd2)
        registry.registerCommand(cmd3)

        val result = registry.clearNamespace("test_ns")
        assertTrue("Clear should succeed", result.isSuccess)
        assertEquals("Should have cleared 2 commands", 2, result.getOrNull())

        val testNsCommands = registry.getAllCommands(namespace = "test_ns")
        val otherNsCommands = registry.getAllCommands(namespace = "other_ns")

        assertEquals("test_ns should be empty", 0, testNsCommands.size)
        assertEquals("other_ns should still have 1 command", 1, otherNsCommands.size)
    }

    // ========== Statistics Tests ==========

    @Test
    fun `test get registry statistics`() = runTest {
        val cmd1 = createTestCommand(id = "cmd_001", enabled = true)
        val cmd2 = createTestCommand(id = "cmd_002", enabled = false)

        registry.registerCommand(cmd1)
        registry.registerCommand(cmd2)

        val stats = registry.getStatistics()

        assertEquals("Total should be 2", 2, stats.totalCommands)
        assertEquals("Enabled should be 1", 1, stats.enabledCommands)
        assertEquals("Disabled should be 1", 1, stats.disabledCommands)
        assertTrue("Should have at least 1 namespace", stats.namespaceCount >= 1)
    }

    @Test
    fun `test get namespace statistics`() = runTest {
        val cmd1 = createTestCommand(id = "cmd_001", namespace = "test_ns")
        val cmd2 = createTestCommand(id = "cmd_002", namespace = "test_ns")

        registry.registerCommand(cmd1)
        registry.registerCommand(cmd2)

        val result = registry.getNamespaceStatistics("test_ns")
        assertTrue("Should get statistics", result.isSuccess)

        val stats = result.getOrNull()
        assertNotNull("Statistics should not be null", stats)
        assertEquals("Should have 2 commands", 2, stats?.totalCommands)
    }

    // ========== Category and Priority Level Tests ==========

    @Test
    fun `test get commands by category`() = runTest {
        val cmd1 = VoiceCommand(
            id = "cmd_001",
            phrases = listOf("test phrase"),
            category = CommandCategory.NAVIGATION,
            action = { CommandResult.Success }
        )
        val cmd2 = VoiceCommand(
            id = "cmd_002",
            phrases = listOf("test phrase 2"),
            category = CommandCategory.SYSTEM,
            action = { CommandResult.Success }
        )

        registry.registerCommand(cmd1)
        registry.registerCommand(cmd2)

        val navCommands = registry.getCommandsByCategory(CommandCategory.NAVIGATION)
        assertEquals("Should find 1 navigation command", 1, navCommands.size)
        assertEquals("Should be correct command", "cmd_001", navCommands[0].id)
    }

    @Test
    fun `test get commands by priority level`() = runTest {
        val cmdLow = createTestCommand(id = "low", priority = 20)
        val cmdHigh = createTestCommand(id = "high", priority = 75)

        registry.registerCommand(cmdLow)
        registry.registerCommand(cmdHigh)

        val lowPriorityCommands = registry.getCommandsByPriorityLevel(PriorityLevel.LOW)
        val highPriorityCommands = registry.getCommandsByPriorityLevel(PriorityLevel.HIGH)

        assertEquals("Should find 1 low priority", 1, lowPriorityCommands.size)
        assertEquals("Should find 1 high priority", 1, highPriorityCommands.size)
    }

    // ========== Listener Tests ==========

    @Test
    fun `test registration listener receives events`() = runTest {
        var registeredCalled = false
        var unregisteredCalled = false

        val listener = object : RegistrationListener {
            override fun onCommandRegistered(command: VoiceCommand, namespace: String) {
                registeredCalled = true
            }

            override fun onCommandUnregistered(commandId: String, namespace: String) {
                unregisteredCalled = true
            }
        }

        registry.addListener(listener)

        val command = createTestCommand(id = "cmd_001")
        registry.registerCommand(command)
        assertTrue("Listener should be called on registration", registeredCalled)

        registry.unregisterCommand("cmd_001")
        assertTrue("Listener should be called on unregistration", unregisteredCalled)

        registry.removeListener(listener)
    }

    // ========== Usage Tracking Tests ==========

    @Test
    fun `test record command execution updates usage`() = runTest {
        val command = createTestCommand(id = "cmd_001")
        registry.registerCommand(command)

        registry.recordCommandExecution("cmd_001")

        val retrieved = registry.getAllCommands()[0]
        assertEquals("Usage count should be 1", 1L, retrieved.usageCount)
        assertTrue("Last used should be updated", retrieved.lastUsed > 0)
    }

    // ========== Conflict Report Tests ==========

    @Test
    fun `test generate conflict report`() = runTest {
        val cmd1 = createTestCommand(
            id = "cmd_001",
            phrases = listOf("test"),
            priority = 50
        )
        val cmd2 = createTestCommand(
            id = "cmd_002",
            phrases = listOf("test"),
            priority = 50
        )

        registry.registerCommand(cmd1, checkConflicts = false)
        registry.registerCommand(cmd2, checkConflicts = false)

        val report = registry.generateConflictReport()

        assertTrue("Should detect conflicts", report.totalConflicts > 0)
        assertTrue("Should have critical conflicts", report.criticalConflicts > 0)
    }
}
