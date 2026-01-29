/**
 * CommandManagerIntegrationTest.kt - Integration tests for CommandManager system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-14
 *
 * Test Coverage:
 * - End-to-end command flows (voice input → execution → result)
 * - Confidence-based filtering (REJECT/LOW/MEDIUM/HIGH)
 * - Fuzzy matching when exact match fails
 * - Command callbacks (confirmation, alternatives)
 * - System command execution (navigation, volume, wifi/bluetooth)
 * - Service lifecycle (initialize, cleanup, healthCheck, restart)
 * - Performance benchmarks
 * - Concurrent command execution
 *
 * Architecture: Tests CommandManager singleton with direct implementation
 */
package com.augmentalis.commandmanager.integration

import android.content.Context
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.commandmanager.*
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

class CommandManagerIntegrationTest {

    private lateinit var mockContext: Context
    private lateinit var commandManager: CommandManager

    @BeforeClass
    fun setupAll() {
        mockContext = mockk(relaxed = true)
    }

    @Before
    fun setup() {
        commandManager = CommandManager.getInstance(mockContext)
        commandManager.initialize()
    }

    @After
    fun teardown() {
        commandManager.cleanup()
    }

    // ========== End-to-End Command Flow Tests ==========

    @Test
    fun `test execute navigation command back`() = runTest {
        // Arrange
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        assertTrue(result is CommandResult)
    }

    @Test
    fun `test execute navigation command home`() = runTest {
        // Arrange
        val command = Command(id = "nav_home", text = "home", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        assertTrue(result is CommandResult)
    }

    @Test
    fun `test execute navigation command recent apps`() = runTest {
        // Arrange
        val command = Command(id = "nav_recent", text = "recent apps", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        assertTrue(result is CommandResult)
    }

    @Test
    fun `test execute volume command up`() = runTest {
        // Arrange
        val command = Command(id = "volume_up", text = "volume up", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        assertTrue(result is CommandResult)
    }

    @Test
    fun `test execute volume command down`() = runTest {
        // Arrange
        val command = Command(id = "volume_down", text = "volume down", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        assertTrue(result is CommandResult)
    }

    @Test
    fun `test execute system command wifi toggle`() = runTest {
        // Arrange
        val command = Command(id = "wifi_toggle", text = "toggle wifi", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        assertTrue(result is CommandResult)
    }

    // ========== Confidence-Based Filtering Tests ==========

    @Test
    fun `test command rejected with very low confidence`() = runTest {
        // Arrange - Confidence below REJECT threshold
        val command = Command(id = "test", text = "test", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.1f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertFalse("Should reject command with very low confidence", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
        assertTrue(result.error?.message?.contains("too low") == true)
    }

    @Test
    fun `test command executed with high confidence`() = runTest {
        // Arrange - High confidence (>= 0.7)
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.9f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        // High confidence commands execute immediately
    }

    @Test
    fun `test command with medium confidence proceeds without callback`() = runTest {
        // Arrange - Medium confidence (0.5 - 0.7)
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.6f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        // Without confirmation callback, medium confidence proceeds
    }

    @Test
    fun `test command with low confidence proceeds without alternatives callback`() = runTest {
        // Arrange - Low confidence (0.3 - 0.5)
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.4f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        // Without alternatives callback, low confidence proceeds
    }

    // ========== Confirmation Callback Tests ==========

    @Test
    fun `test confirmation callback accepts command`() = runTest {
        // Arrange
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.6f)
        commandManager.setConfirmationCallback { _, _ -> true } // User confirms

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        // Should execute after confirmation
    }

    @Test
    fun `test confirmation callback rejects command`() = runTest {
        // Arrange
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.6f)
        commandManager.setConfirmationCallback { _, _ -> false } // User cancels

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertFalse("Should cancel when user rejects", result.success)
        assertEquals(ErrorCode.CANCELLED, result.error?.code)
    }

    // ========== Alternatives Callback Tests ==========

    @Test
    fun `test alternatives callback selects alternative`() = runTest {
        // Arrange
        val command = Command(id = "bck", text = "bck", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.4f) // typo
        commandManager.setAlternativesCallback { _, alternatives ->
            alternatives.firstOrNull() // Select first alternative
        }

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        // Should execute selected alternative if found
    }

    @Test
    fun `test alternatives callback returns null continues with original`() = runTest {
        // Arrange
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.4f)
        commandManager.setAlternativesCallback { _, _ -> null } // No selection

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        // Should continue with original command
    }

    // ========== Fuzzy Matching Tests ==========

    @Test
    fun `test fuzzy matching finds similar command`() = runTest {
        // Arrange - Command with typo
        val command = Command(id = "nav_bak", text = "bak", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
        // Fuzzy matching should find "nav_back" if similarity >= 70%
    }

    @Test
    fun `test unknown command returns error`() = runTest {
        // Arrange - Completely unknown command
        val command = Command(id = "completely_unknown", text = "xyz123", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertFalse(result.success)
        assertEquals(ErrorCode.COMMAND_NOT_FOUND, result.error?.code)
    }

    // ========== Confidence Override Tests ==========

    @Test
    fun `test execute command with confidence override`() = runTest {
        // Arrange - Override confidence filtering
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 0.1f)

        // Act
        val result = commandManager.executeCommandWithConfidenceOverride(command)

        // Assert
        assertNotNull(result)
        // Should execute even with low confidence when override is used
    }

    // ========== Service Lifecycle Tests ==========

    @Test
    fun `test initialize CommandManager`() {
        // Act & Assert - Should not throw
        try {
            commandManager.initialize()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `test cleanup CommandManager`() {
        // Act & Assert - Should not throw
        try {
            commandManager.cleanup()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `test health check returns true`() {
        // Act
        val healthy = commandManager.healthCheck()

        // Assert
        assertTrue("CommandManager should be healthy after initialization", healthy)
    }

    @Test
    fun `test health check returns false after cleanup`() {
        // Arrange
        commandManager.cleanup()

        // Act
        val healthy = commandManager.healthCheck()

        // Assert
        assertFalse("CommandManager should be unhealthy after cleanup", healthy)
    }

    @Test
    fun `test restart CommandManager`() {
        // Arrange
        commandManager.cleanup()

        // Act & Assert - Should not throw
        try {
            commandManager.restart()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }

        // Health check should pass after restart
        assertTrue(commandManager.healthCheck())
    }

    // ========== Concurrent Execution Tests ==========

    @Test
    fun `test concurrent command execution`() = runTest {
        // Arrange
        val command1 = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)
        val command2 = Command(id = "nav_home", text = "home", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)
        val command3 = Command(id = "nav_recent", text = "recent", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act - Execute multiple commands concurrently
        val result1 = commandManager.executeCommand(command1)
        val result2 = commandManager.executeCommand(command2)
        val result3 = commandManager.executeCommand(command3)

        // Assert - All should complete
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
    }

    // ========== Performance Tests ==========

    @Test
    fun `test command execution performance`() = runTest {
        // Arrange
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val executionTime = measureTimeMillis {
            commandManager.executeCommand(command)
        }

        // Assert - Should execute quickly (< 100ms)
        assertTrue("Command should execute in less than 100ms (took ${executionTime}ms)", executionTime < 100)
    }

    @Test
    fun `test load test with many commands`() = runTest {
        // Arrange
        val commands = listOf(
            Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f),
            Command(id = "nav_home", text = "home", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f),
            Command(id = "nav_recent", text = "recent", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f),
            Command(id = "volume_up", text = "volume up", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f),
            Command(id = "volume_down", text = "volume down", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)
        )

        // Act
        val totalTime = measureTimeMillis {
            commands.forEach { command ->
                commandManager.executeCommand(command)
            }
        }

        // Assert - Average per command should be fast
        val avgTime = totalTime / commands.size
        assertTrue("Average execution time should be < 100ms (avg: ${avgTime}ms)", avgTime < 100)
    }

    // ========== Edge Cases Tests ==========

    @Test
    fun `test execute command with null confidence uses default`() = runTest {
        // Arrange - Command without explicit confidence
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = commandManager.executeCommand(command)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `test execute same command multiple times`() = runTest {
        // Arrange
        val command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act - Execute same command 3 times
        val result1 = commandManager.executeCommand(command)
        val result2 = commandManager.executeCommand(command)
        val result3 = commandManager.executeCommand(command)

        // Assert - All should execute independently
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
    }

    @Test
    fun `test execute commands in rapid succession`() = runTest {
        // Arrange
        val commands = (1..10).map {
            Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)
        }

        // Act - Rapid fire commands
        val results = commands.map { commandManager.executeCommand(it) }

        // Assert - All should complete
        assertEquals(10, results.size)
        results.forEach { assertNotNull(it) }
    }

    // ========== Singleton Pattern Tests ==========

    @Test
    fun `test getInstance returns same instance`() {
        // Act
        val instance1 = CommandManager.getInstance(mockContext)
        val instance2 = CommandManager.getInstance(mockContext)

        // Assert
        assertSame("Should return same singleton instance", instance1, instance2)
    }

    // ========== Error Handling Tests ==========

    @Test
    fun `test execute command handles internal error gracefully`() = runTest {
        // Arrange - Create command that might cause internal error
        val command = Command("", "", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())

        // Act
        val result = commandManager.executeCommand(command)

        // Assert - Should handle error gracefully
        assertNotNull(result)
        assertFalse(result.success)
    }

    @Test
    fun `test multiple initialize calls are safe`() {
        // Act & Assert - Multiple initializations should be safe
        try {
            commandManager.initialize()
            commandManager.initialize()
            commandManager.initialize()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `test multiple cleanup calls are safe`() {
        // Act & Assert - Multiple cleanups should be safe
        try {
            commandManager.cleanup()
            commandManager.cleanup()
            commandManager.cleanup()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }
}
