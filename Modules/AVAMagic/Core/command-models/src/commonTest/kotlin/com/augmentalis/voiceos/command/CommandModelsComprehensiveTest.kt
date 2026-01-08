/**
 * CommandModelsComprehensiveTest.kt - Comprehensive tests for command models
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.command

import kotlin.test.*

/**
 * Tests for VOSCommand data class
 */
class VOSCommandTest {

    @Test
    fun testVOSCommandCreation() {
        val command = VOSCommand(
            action = "NAVIGATE_FORWARD",
            cmd = "forward",
            syn = listOf("next", "advance")
        )

        assertEquals("NAVIGATE_FORWARD", command.action)
        assertEquals("forward", command.cmd)
        assertEquals(2, command.syn.size)
    }

    @Test
    fun testVOSCommandEquality() {
        val cmd1 = VOSCommand("ACTION", "command", listOf("syn1"))
        val cmd2 = VOSCommand("ACTION", "command", listOf("syn1"))
        val cmd3 = VOSCommand("ACTION2", "command", listOf("syn1"))

        assertEquals(cmd1, cmd2)
        assertNotEquals(cmd1, cmd3)
    }

    @Test
    fun testVOSCommandCopy() {
        val original = VOSCommand("ACTION", "cmd", listOf("syn"))
        val copy = original.copy(action = "NEW_ACTION")

        assertEquals("NEW_ACTION", copy.action)
        assertEquals("cmd", copy.cmd)
    }

    @Test
    fun testVOSCommandWithEmptySynonyms() {
        val command = VOSCommand("ACTION", "cmd", emptyList())
        assertTrue(command.syn.isEmpty())
    }
}

/**
 * Tests for Command data class
 */
class CommandTest {

    @Test
    fun testCommandCreation() {
        val command = Command(
            id = "cmd-123",
            text = "navigate forward",
            source = CommandSource.VOICE,
            timestamp = 1234567890L,
            confidence = 0.95f
        )

        assertEquals("cmd-123", command.id)
        assertEquals("navigate forward", command.text)
        assertEquals(CommandSource.VOICE, command.source)
        assertEquals(1234567890L, command.timestamp)
        assertEquals(0.95f, command.confidence)
    }

    @Test
    fun testCommandWithDefaults() {
        val command = Command(
            id = "cmd-123",
            text = "test",
            source = CommandSource.TEXT,
            timestamp = 0L
        )

        assertNull(command.context)
        assertTrue(command.parameters.isEmpty())
        assertEquals(1.0f, command.confidence)
    }

    @Test
    fun testCommandWithContext() {
        val context = CommandContext(packageName = "com.example.app")
        val command = Command(
            id = "1",
            text = "test",
            source = CommandSource.VOICE,
            context = context,
            timestamp = 0L
        )

        assertNotNull(command.context)
        assertEquals("com.example.app", command.context?.packageName)
    }

    @Test
    fun testCommandWithParameters() {
        val params = mapOf("key1" to "value1", "key2" to 42)
        val command = Command(
            id = "1",
            text = "test",
            source = CommandSource.SYSTEM,
            parameters = params,
            timestamp = 0L
        )

        assertEquals(2, command.parameters.size)
        assertEquals("value1", command.parameters["key1"])
        assertEquals(42, command.parameters["key2"])
    }
}

/**
 * Tests for CommandSource enum
 */
class CommandSourceTest {

    @Test
    fun testCommandSourceValues() {
        val sources = CommandSource.values()
        assertEquals(5, sources.size)
        assertTrue(sources.contains(CommandSource.VOICE))
        assertTrue(sources.contains(CommandSource.GESTURE))
        assertTrue(sources.contains(CommandSource.TEXT))
        assertTrue(sources.contains(CommandSource.SYSTEM))
        assertTrue(sources.contains(CommandSource.EXTERNAL))
    }

    @Test
    fun testCommandSourceValueOf() {
        assertEquals(CommandSource.VOICE, CommandSource.valueOf("VOICE"))
        assertEquals(CommandSource.GESTURE, CommandSource.valueOf("GESTURE"))
        assertEquals(CommandSource.TEXT, CommandSource.valueOf("TEXT"))
    }

    @Test
    fun testCommandSourceInWhenExpression() {
        fun isUserInitiated(source: CommandSource) = when (source) {
            CommandSource.VOICE, CommandSource.GESTURE, CommandSource.TEXT -> true
            CommandSource.SYSTEM, CommandSource.EXTERNAL -> false
        }

        assertTrue(isUserInitiated(CommandSource.VOICE))
        assertFalse(isUserInitiated(CommandSource.SYSTEM))
    }
}

/**
 * Tests for CommandContext data class
 */
class CommandContextTest {

    @Test
    fun testCommandContextDefaults() {
        val context = CommandContext()

        assertNull(context.packageName)
        assertNull(context.activityName)
        assertTrue(context.deviceState.isEmpty())
        assertTrue(context.customData.isEmpty())
    }

    @Test
    fun testCommandContextFullyPopulated() {
        val context = CommandContext(
            packageName = "com.app",
            activityName = "MainActivity",
            viewId = "button1",
            screenContent = "Screen text",
            userLocation = "Home",
            deviceState = mapOf("battery" to 85),
            focusedElement = "Button",
            customData = mapOf("key" to "value")
        )

        assertEquals("com.app", context.packageName)
        assertEquals("MainActivity", context.activityName)
        assertEquals("button1", context.viewId)
        assertEquals(1, context.deviceState.size)
        assertEquals(1, context.customData.size)
    }

    @Test
    fun testCommandContextCopy() {
        val original = CommandContext(packageName = "com.app")
        val copy = original.copy(activityName = "NewActivity")

        assertEquals("com.app", copy.packageName)
        assertEquals("NewActivity", copy.activityName)
    }
}

/**
 * Tests for CommandResult data class
 */
class CommandResultTest {

    @Test
    fun testSuccessfulCommandResult() {
        val command = Command("1", "test", CommandSource.VOICE, timestamp = 0L)
        val result = CommandResult(
            success = true,
            command = command,
            response = "Success",
            executionTime = 100L
        )

        assertTrue(result.success)
        assertEquals("Success", result.response)
        assertNull(result.error)
        assertEquals(100L, result.executionTime)
    }

    @Test
    fun testFailedCommandResult() {
        val command = Command("1", "test", CommandSource.VOICE, timestamp = 0L)
        val error = CommandError(ErrorCode.EXECUTION_FAILED, "Failed")
        val result = CommandResult(
            success = false,
            command = command,
            error = error
        )

        assertFalse(result.success)
        assertNotNull(result.error)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
    }
}

/**
 * Tests for CommandError and ErrorCode
 */
class CommandErrorTest {

    @Test
    fun testCommandErrorCreation() {
        val error = CommandError(
            code = ErrorCode.INVALID_PARAMETERS,
            message = "Invalid param",
            details = "Expected number"
        )

        assertEquals(ErrorCode.INVALID_PARAMETERS, error.code)
        assertEquals("Invalid param", error.message)
        assertEquals("Expected number", error.details)
    }

    @Test
    fun testErrorCodeValues() {
        val codes = ErrorCode.values()
        assertEquals(14, codes.size)
        assertTrue(codes.contains(ErrorCode.MODULE_NOT_AVAILABLE))
        assertTrue(codes.contains(ErrorCode.COMMAND_NOT_FOUND))
        assertTrue(codes.contains(ErrorCode.PERMISSION_DENIED))
    }

    @Test
    fun testErrorCodeCategories() {
        fun isRecoverableError(code: ErrorCode) = when (code) {
            ErrorCode.TIMEOUT, ErrorCode.NETWORK_ERROR, ErrorCode.CANCELLED -> true
            else -> false
        }

        assertTrue(isRecoverableError(ErrorCode.TIMEOUT))
        assertFalse(isRecoverableError(ErrorCode.PERMISSION_DENIED))
    }
}

/**
 * Tests for CommandDefinition and CommandParameter
 */
class CommandDefinitionTest {

    @Test
    fun testCommandDefinitionBasic() {
        val definition = CommandDefinition(
            id = "nav_forward",
            name = "Navigate Forward",
            description = "Move to next item",
            category = "NAVIGATION",
            patterns = listOf("forward", "next")
        )

        assertEquals("nav_forward", definition.id)
        assertEquals(2, definition.patterns.size)
        assertTrue(definition.parameters.isEmpty())
        assertEquals(listOf("en"), definition.supportedLanguages)
    }

    @Test
    fun testCommandDefinitionWithParameters() {
        val param = CommandParameter(
            name = "speed",
            type = ParameterType.NUMBER,
            required = true,
            defaultValue = 1
        )

        val definition = CommandDefinition(
            id = "scroll",
            name = "Scroll",
            description = "Scroll view",
            category = "NAVIGATION",
            patterns = listOf("scroll"),
            parameters = listOf(param)
        )

        assertEquals(1, definition.parameters.size)
        assertEquals("speed", definition.parameters[0].name)
    }

    @Test
    fun testParameterTypes() {
        val types = ParameterType.values()
        assertEquals(6, types.size)
        assertTrue(types.contains(ParameterType.STRING))
        assertTrue(types.contains(ParameterType.NUMBER))
        assertTrue(types.contains(ParameterType.BOOLEAN))
        assertTrue(types.contains(ParameterType.LIST))
        assertTrue(types.contains(ParameterType.MAP))
        assertTrue(types.contains(ParameterType.CUSTOM))
    }
}

/**
 * Tests for CommandHistoryEntry
 */
class CommandHistoryEntryTest {

    @Test
    fun testHistoryEntryCreation() {
        val command = Command("1", "test", CommandSource.VOICE, timestamp = 1000L)
        val result = CommandResult(true, command)
        val entry = CommandHistoryEntry(command, result, 1000L)

        assertEquals(command, entry.command)
        assertEquals(result, entry.result)
        assertEquals(1000L, entry.timestamp)
    }

    @Test
    fun testHistoryEntryEquality() {
        val command = Command("1", "test", CommandSource.VOICE, timestamp = 1000L)
        val result = CommandResult(true, command)
        val entry1 = CommandHistoryEntry(command, result, 1000L)
        val entry2 = CommandHistoryEntry(command, result, 1000L)

        assertEquals(entry1, entry2)
    }
}

/**
 * Tests for CommandEvent and EventType
 */
class CommandEventTest {

    @Test
    fun testCommandEventCreation() {
        val command = Command("1", "test", CommandSource.VOICE, timestamp = 0L)
        val event = CommandEvent(
            type = EventType.COMMAND_RECEIVED,
            command = command,
            message = "Command received",
            timestamp = 1000L
        )

        assertEquals(EventType.COMMAND_RECEIVED, event.type)
        assertNotNull(event.command)
        assertEquals("Command received", event.message)
    }

    @Test
    fun testEventTypeValues() {
        val types = EventType.values()
        assertEquals(6, types.size)
        assertTrue(types.contains(EventType.COMMAND_RECEIVED))
        assertTrue(types.contains(EventType.COMMAND_EXECUTING))
        assertTrue(types.contains(EventType.COMMAND_COMPLETED))
        assertTrue(types.contains(EventType.COMMAND_FAILED))
    }
}

/**
 * Tests for CommandInfo and CommandStats
 */
class CommandInfoTest {

    @Test
    fun testCommandInfoDefaults() {
        val info = CommandInfo("1", "Test Command", "TEST")

        assertEquals("1", info.id)
        assertEquals("Test Command", info.name)
        assertEquals("TEST", info.category)
        assertFalse(info.isCustom)
        assertEquals(0, info.usageCount)
    }

    @Test
    fun testCommandStatsCreation() {
        val stats = CommandStats(
            totalCommands = 100,
            successfulCommands = 90,
            failedCommands = 10,
            averageExecutionTime = 50L,
            topCommands = listOf("forward", "back")
        )

        assertEquals(100, stats.totalCommands)
        assertEquals(90, stats.successfulCommands)
        assertEquals(10, stats.failedCommands)
        assertEquals(50L, stats.averageExecutionTime)
        assertEquals(2, stats.topCommands.size)
    }

    @Test
    fun testStatsSuccessRate() {
        val stats = CommandStats(100, 90, 10, 50L, emptyList())
        val successRate = (stats.successfulCommands.toFloat() / stats.totalCommands) * 100
        assertEquals(90.0f, successRate)
    }
}

/**
 * Tests for CommandCategory enum
 */
class CommandCategoryTest {

    @Test
    fun testCommandCategoryValues() {
        val categories = CommandCategory.values()
        assertEquals(11, categories.size)
        assertTrue(categories.contains(CommandCategory.NAVIGATION))
        assertTrue(categories.contains(CommandCategory.TEXT))
        assertTrue(categories.contains(CommandCategory.MEDIA))
    }

    @Test
    fun testCommandCategoryGrouping() {
        fun isInputCategory(category: CommandCategory) = when (category) {
            CommandCategory.TEXT, CommandCategory.INPUT, CommandCategory.VOICE, CommandCategory.GESTURE -> true
            else -> false
        }

        assertTrue(isInputCategory(CommandCategory.TEXT))
        assertFalse(isInputCategory(CommandCategory.NAVIGATION))
    }
}

/**
 * Tests for AccessibilityActions object
 */
class AccessibilityActionsTest {

    @Test
    fun testAccessibilityActionConstants() {
        assertEquals(0x10000, AccessibilityActions.ACTION_SELECT_ALL)
        assertEquals(0x20000, AccessibilityActions.ACTION_BACKUP_AND_RESET_SETTINGS)
    }

    @Test
    fun testAccessibilityActionValuesAreUnique() {
        val actions = setOf(
            AccessibilityActions.ACTION_SELECT_ALL,
            AccessibilityActions.ACTION_BACKUP_AND_RESET_SETTINGS
        )
        assertEquals(2, actions.size)
    }
}

/**
 * Integration tests for command models
 */
class CommandModelsIntegrationTest {

    @Test
    fun testCompleteCommandFlow() {
        // Create command
        val context = CommandContext(packageName = "com.app")
        val command = Command(
            id = "cmd-1",
            text = "navigate forward",
            source = CommandSource.VOICE,
            context = context,
            timestamp = 1000L,
            confidence = 0.9f
        )

        // Create result
        val result = CommandResult(
            success = true,
            command = command,
            response = "Navigated forward",
            executionTime = 50L
        )

        // Create history entry
        val historyEntry = CommandHistoryEntry(command, result, 1000L)

        // Create event
        val event = CommandEvent(
            type = EventType.COMMAND_COMPLETED,
            command = command,
            result = result,
            timestamp = 1000L
        )

        // Verify complete flow
        assertEquals(command, historyEntry.command)
        assertEquals(result, historyEntry.result)
        assertEquals(EventType.COMMAND_COMPLETED, event.type)
        assertTrue(result.success)
    }

    @Test
    fun testCommandDefinitionWithExecutionContext() {
        val param1 = CommandParameter("target", ParameterType.STRING, required = true)
        val param2 = CommandParameter("speed", ParameterType.NUMBER, defaultValue = 1)

        val definition = CommandDefinition(
            id = "navigate",
            name = "Navigate",
            description = "Navigate to target",
            category = "NAVIGATION",
            patterns = listOf("go to {target}", "navigate to {target}"),
            parameters = listOf(param1, param2),
            requiredPermissions = listOf("android.permission.ACCESS_FINE_LOCATION"),
            supportedLanguages = listOf("en", "es"),
            requiredContext = setOf("packageName", "screenContent")
        )

        // Verify
        assertEquals(2, definition.parameters.size)
        assertTrue(definition.parameters[0].required)
        assertFalse(definition.parameters[1].required)
        assertEquals(1, definition.requiredPermissions.size)
        assertEquals(2, definition.supportedLanguages.size)
        assertEquals(2, definition.requiredContext.size)
    }

    @Test
    fun testCommandErrorHandling() {
        val command = Command("1", "test", CommandSource.VOICE, timestamp = 0L)

        val errors = listOf(
            CommandError(ErrorCode.COMMAND_NOT_FOUND, "Not found"),
            CommandError(ErrorCode.PERMISSION_DENIED, "No permission"),
            CommandError(ErrorCode.TIMEOUT, "Timed out")
        )

        errors.forEach { error ->
            val result = CommandResult(false, command, error = error)
            assertFalse(result.success)
            assertNotNull(result.error)
        }
    }
}
