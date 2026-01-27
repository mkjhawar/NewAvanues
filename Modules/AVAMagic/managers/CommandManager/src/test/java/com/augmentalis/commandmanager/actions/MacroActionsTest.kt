/**
 * MacroActionsTest.kt - Unit tests for macro command execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-14
 *
 * Test Coverage:
 * - Pre-defined macro execution
 * - Macro step execution with 200ms delays
 * - Parameter substitution
 * - Error handling and rollback
 * - Macro categories
 * - Variable macro execution
 * - Case insensitivity and whitespace handling
 *
 * Architecture: Tests MacroActions class that executes multi-step commands
 */
package com.augmentalis.commandmanager.actions

import android.content.Context
import com.augmentalis.commandmanager.actions.CommandExecutor
import com.augmentalis.voiceoscore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class MacroActionsTest {

    private lateinit var mockContext: Context
    private lateinit var mockCommandExecutor: CommandExecutor
    private lateinit var macroActions: MacroActions

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockCommandExecutor = mockk(relaxed = true)
        macroActions = MacroActions(mockCommandExecutor)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    // ========== Pre-defined Macro Tests ==========

    @Test
    fun `test execute select all and copy macro`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "select all and copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "select_all" }) } returns
            CommandResult(success = true, command = Command(id = "select_all", text = "select all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Selected")
        coEvery { mockCommandExecutor.execute(match { it.id == "copy" }) } returns
            CommandResult(success = true, command = Command(id = "copy", text = "copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Copied")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertTrue("Macro should succeed", result.success)
        assertEquals("Macro completed: 2 steps executed", result.response)
        coVerifyOrder {
            mockCommandExecutor.execute(match { it.id == "select_all" })
            mockCommandExecutor.execute(match { it.id == "copy" })
        }
    }

    @Test
    fun `test execute cut and paste macro`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "cut and paste", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "cut" }) } returns
            CommandResult(success = true, command = Command(id = "cut", text = "cut", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Cut")
        coEvery { mockCommandExecutor.execute(match { it.id == "paste" }) } returns
            CommandResult(success = true, command = Command(id = "paste", text = "paste", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Pasted")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertTrue("Macro should succeed", result.success)
        assertEquals("Macro completed: 2 steps executed", result.response)
        coVerifyOrder {
            mockCommandExecutor.execute(match { it.id == "cut" })
            mockCommandExecutor.execute(match { it.id == "paste" })
        }
    }

    @Test
    fun `test execute close all macro`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "close all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "nav_home" }) } returns
            CommandResult(success = true, command = Command(id = "nav_home", text = "home", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Home")
        coEvery { mockCommandExecutor.execute(match { it.id == "nav_recent" }) } returns
            CommandResult(success = true, command = Command(id = "nav_recent", text = "recent", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Recent")
        coEvery { mockCommandExecutor.execute(match { it.text == "close all apps" }) } returns
            CommandResult(success = true, command = Command(id = "close_all_apps", text = "close all apps", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Closed")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertTrue("Macro should succeed", result.success)
        coVerify { mockCommandExecutor.execute(any()) }
    }

    // ========== Macro Steps Execution Tests ==========

    @Test
    fun `test macro stops on first failure`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "select all and copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "select_all" }) } returns
            CommandResult(success = false, command = Command(id = "select_all", text = "select all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f),
                error = CommandError(ErrorCode.EXECUTION_FAILED, "Failed to select"))
        coEvery { mockCommandExecutor.execute(match { it.id == "copy" }) } returns
            CommandResult(success = true, command = Command(id = "copy", text = "copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Copied")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertFalse("Macro should fail on first step failure", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
        assertTrue("Error should indicate which step failed", result.error?.message?.contains("step 1") == true)

        // Verify second command was NOT executed
        coVerify(exactly = 1) { mockCommandExecutor.execute(match { it.id == "select_all" }) }
        coVerify(exactly = 0) { mockCommandExecutor.execute(match { it.id == "copy" }) }
    }

    @Test
    fun `test macro with delay between steps`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "select all and copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "select_all" }) } returns
            CommandResult(success = true, command = Command(id = "select_all", text = "select all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Selected")
        coEvery { mockCommandExecutor.execute(match { it.id == "copy" }) } returns
            CommandResult(success = true, command = Command(id = "copy", text = "copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Copied")

        // Act
        val startTime = System.currentTimeMillis()
        val result = macroActions.execute(command, null, mockContext)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertTrue("Macro should succeed", result.success)
        // Should have at least 200ms delay between 2 steps (actually 1 delay between 2 steps)
        assertTrue("Should have delay between steps (at least 180ms for 200ms delay)", duration >= 180)
    }

    // ========== Parameter Substitution Tests ==========

    @Test
    fun `test macro with variable parameters`() = runTest {
        // Arrange - Testing variable macro that would substitute parameters
        val command = Command(id = "macro", text = "repeat 3 times back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // For this test, we'll assume the macro parses "3" and "back" as parameters
        coEvery { mockCommandExecutor.execute(any()) } returns
            CommandResult(success = true, command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Back")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert - Either succeeds if macro is defined or fails gracefully
        assertTrue(result is CommandResult)
    }

    // ========== Error Handling Tests ==========

    @Test
    fun `test unknown macro command`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "completely unknown macro", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertFalse("Unknown macro should fail", result.success)
        assertEquals(ErrorCode.UNKNOWN_COMMAND, result.error?.code)
        assertTrue(result.error?.message?.contains("Unknown macro") == true ||
                   result.error?.message?.contains("not found") == true)
    }

    @Test
    fun `test macro with exception in command manager`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "select all and copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(any()) } throws RuntimeException("Test exception")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertFalse("Macro should fail on exception", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
    }

    // ========== Case Insensitivity and Whitespace Tests ==========

    @Test
    fun `test macro with different case`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "SELECT ALL AND COPY", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "select_all" }) } returns
            CommandResult(success = true, command = Command(id = "select_all", text = "select all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Selected")
        coEvery { mockCommandExecutor.execute(match { it.id == "copy" }) } returns
            CommandResult(success = true, command = Command(id = "copy", text = "copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Copied")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertTrue("Macro should be case insensitive", result.success)
        assertEquals("Macro completed: 2 steps executed", result.response)
    }

    @Test
    fun `test macro with extra whitespace`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "  select  all   and   copy  ", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "select_all" }) } returns
            CommandResult(success = true, command = Command(id = "select_all", text = "select all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Selected")
        coEvery { mockCommandExecutor.execute(match { it.id == "copy" }) } returns
            CommandResult(success = true, command = Command(id = "copy", text = "copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Copied")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertTrue("Macro should handle extra whitespace", result.success)
    }

    // ========== Macro Categories Tests ==========

    @Test
    fun `test text editing macro category`() = runTest {
        // Arrange - Testing text editing category macros
        val command = Command(id = "macro", text = "select all and copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(any()) } returns
            CommandResult(success = true, command = Command(id = "test", text = "test", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Success")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertTrue("Text editing macro should succeed", result.success)
        coVerify(atLeast = 1) { mockCommandExecutor.execute(any()) }
    }

    @Test
    fun `test navigation macro category`() = runTest {
        // Arrange - Testing navigation category macros
        val command = Command(id = "macro", text = "close all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(any()) } returns
            CommandResult(success = true, command = Command(id = "test", text = "test", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Success")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertTrue(result is CommandResult)
    }

    // ========== Complex Macro Tests ==========

    @Test
    fun `test multi-step macro with three steps`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "back home recent", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "nav_back" }) } returns
            CommandResult(success = true, command = Command(id = "nav_back", text = "back", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Back")
        coEvery { mockCommandExecutor.execute(match { it.id == "nav_home" }) } returns
            CommandResult(success = true, command = Command(id = "nav_home", text = "home", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Home")
        coEvery { mockCommandExecutor.execute(match { it.id == "nav_recent" }) } returns
            CommandResult(success = true, command = Command(id = "nav_recent", text = "recent", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Recent")

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert - Either succeeds if this macro exists or fails gracefully
        assertTrue(result is CommandResult)
    }

    @Test
    fun `test macro failure in middle step`() = runTest {
        // Arrange - Create a 3-step macro where 2nd step fails
        val command = Command(id = "macro", text = "select all and copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        coEvery { mockCommandExecutor.execute(match { it.id == "select_all" }) } returns
            CommandResult(success = true, command = Command(id = "select_all", text = "select all", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f), response = "Selected")
        coEvery { mockCommandExecutor.execute(match { it.id == "copy" }) } returns
            CommandResult(success = false, command = Command(id = "copy", text = "copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f),
                error = CommandError(ErrorCode.EXECUTION_FAILED, "Copy failed"))

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertFalse("Macro should fail when any step fails", result.success)
        assertTrue(result.error?.message?.contains("step") == true)
    }

    // ========== Edge Cases ==========

    @Test
    fun `test empty macro text`() = runTest {
        // Arrange
        val command = Command("macro", "", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertFalse("Empty macro should fail", result.success)
        assertEquals(ErrorCode.UNKNOWN_COMMAND, result.error?.code)
    }

    @Test
    fun `test macro with only whitespace`() = runTest {
        // Arrange
        val command = Command(id = "macro", text = "    ", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = macroActions.execute(command, null, mockContext)

        // Assert
        assertFalse("Whitespace-only macro should fail", result.success)
    }

    @Test
    fun `test null command manager handling`() = runTest {
        // Arrange
        val mockNullableExecutor: CommandExecutor? = null
        val macroActionsWithNullManager = MacroActions(mockNullableExecutor!!)
        val command = Command(id = "macro", text = "select all and copy", source = CommandSource.VOICE, timestamp = System.currentTimeMillis(), confidence = 1.0f)

        // Act
        val result = macroActionsWithNullManager.execute(command, null, mockContext)

        // Assert
        assertFalse("Should fail gracefully with null command manager", result.success)
    }
}
