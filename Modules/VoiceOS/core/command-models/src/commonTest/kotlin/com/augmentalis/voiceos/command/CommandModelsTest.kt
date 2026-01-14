package com.augmentalis.voiceos.command

import kotlin.test.*

class CommandModelsTest {

    // ===== Command Tests =====

    @Test
    fun `Command with all properties`() {
        val timestamp = 1700000000000L
        val command = Command(
            id = "cmd-001",
            text = "go back",
            source = CommandSource.VOICE,
            context = null,
            parameters = mapOf("steps" to 1),
            timestamp = timestamp,
            confidence = 0.95f
        )

        assertEquals("cmd-001", command.id)
        assertEquals("go back", command.text)
        assertEquals(CommandSource.VOICE, command.source)
        assertEquals(1, command.parameters["steps"])
        assertEquals(timestamp, command.timestamp)
        assertEquals(0.95f, command.confidence)
    }

    @Test
    fun `Command with context`() {
        val context = CommandContext(
            packageName = "com.example.app",
            activityName = "MainActivity"
        )
        val command = Command(
            id = "cmd-002",
            text = "scroll down",
            source = CommandSource.GESTURE,
            context = context,
            timestamp = 1700000000000L
        )

        assertNotNull(command.context)
        assertEquals("com.example.app", command.context?.packageName)
    }

    @Test
    fun `Command with minimal properties`() {
        val command = Command(
            id = "cmd-003",
            text = "test",
            source = CommandSource.TEXT,
            timestamp = 1700000000000L
        )

        assertNull(command.context)
        assertTrue(command.parameters.isEmpty())
        assertEquals(1.0f, command.confidence)
    }

    // ===== CommandSource Tests =====

    @Test
    fun `CommandSource enum values`() {
        val sources = CommandSource.values()
        assertEquals(5, sources.size)
        assertTrue(sources.contains(CommandSource.VOICE))
        assertTrue(sources.contains(CommandSource.GESTURE))
        assertTrue(sources.contains(CommandSource.TEXT))
        assertTrue(sources.contains(CommandSource.SYSTEM))
        assertTrue(sources.contains(CommandSource.EXTERNAL))
    }

    // ===== CommandContext Tests =====

    @Test
    fun `CommandContext with full details`() {
        val context = CommandContext(
            packageName = "com.example.app",
            activityName = "MainActivity",
            viewId = "button_submit",
            screenContent = "Login Screen",
            userLocation = "Office",
            deviceState = mapOf("battery" to 80, "network" to "wifi"),
            focusedElement = "EditText",
            customData = mapOf("theme" to "dark")
        )

        assertEquals("com.example.app", context.packageName)
        assertEquals("MainActivity", context.activityName)
        assertEquals("button_submit", context.viewId)
        assertEquals("Login Screen", context.screenContent)
        assertEquals("Office", context.userLocation)
        assertEquals(80, context.deviceState["battery"])
        assertEquals("EditText", context.focusedElement)
        assertEquals("dark", context.customData["theme"])
    }

    @Test
    fun `CommandContext with minimal properties`() {
        val context = CommandContext()

        assertNull(context.packageName)
        assertNull(context.activityName)
        assertTrue(context.deviceState.isEmpty())
        assertTrue(context.customData.isEmpty())
    }

    // ===== CommandResult Tests =====

    @Test
    fun `CommandResult success`() {
        val command = Command(
            id = "cmd-004",
            text = "test command",
            source = CommandSource.VOICE,
            timestamp = 1700000000000L
        )

        val result = CommandResult(
            success = true,
            command = command,
            response = "Command executed successfully",
            data = mapOf("result" to "success"),
            executionTime = 150
        )

        assertTrue(result.success)
        assertEquals("Command executed successfully", result.response)
        assertNull(result.error)
        assertEquals(150, result.executionTime)
    }

    @Test
    fun `CommandResult failure with error`() {
        val command = Command(
            id = "cmd-005",
            text = "invalid command",
            source = CommandSource.TEXT,
            timestamp = 1700000000000L
        )

        val error = CommandError(
            code = ErrorCode.COMMAND_NOT_FOUND,
            message = "Command not found",
            details = "The command 'invalid command' is not registered"
        )

        val result = CommandResult(
            success = false,
            command = command,
            error = error,
            executionTime = 50
        )

        assertFalse(result.success)
        assertNotNull(result.error)
        assertEquals(ErrorCode.COMMAND_NOT_FOUND, result.error?.code)
        assertEquals("Command not found", result.error?.message)
    }

    // ===== CommandError Tests =====

    @Test
    fun `CommandError with details`() {
        val error = CommandError(
            code = ErrorCode.PERMISSION_DENIED,
            message = "Permission denied",
            details = "Missing RECORD_AUDIO permission"
        )

        assertEquals(ErrorCode.PERMISSION_DENIED, error.code)
        assertEquals("Permission denied", error.message)
        assertEquals("Missing RECORD_AUDIO permission", error.details)
    }

    @Test
    fun `CommandError without details`() {
        val error = CommandError(
            code = ErrorCode.UNKNOWN,
            message = "Unknown error"
        )

        assertNull(error.details)
    }

    // ===== ErrorCode Tests =====

    @Test
    fun `ErrorCode enum values`() {
        val codes = ErrorCode.values()
        assertEquals(13, codes.size)
        assertTrue(codes.contains(ErrorCode.MODULE_NOT_AVAILABLE))
        assertTrue(codes.contains(ErrorCode.COMMAND_NOT_FOUND))
        assertTrue(codes.contains(ErrorCode.INVALID_PARAMETERS))
        assertTrue(codes.contains(ErrorCode.PERMISSION_DENIED))
        assertTrue(codes.contains(ErrorCode.EXECUTION_FAILED))
        assertTrue(codes.contains(ErrorCode.TIMEOUT))
        assertTrue(codes.contains(ErrorCode.NETWORK_ERROR))
        assertTrue(codes.contains(ErrorCode.UNKNOWN))
        assertTrue(codes.contains(ErrorCode.UNKNOWN_COMMAND))
        assertTrue(codes.contains(ErrorCode.MISSING_CONTEXT))
        assertTrue(codes.contains(ErrorCode.CANCELLED))
        assertTrue(codes.contains(ErrorCode.NO_ACCESSIBILITY_SERVICE))
        assertTrue(codes.contains(ErrorCode.ACTION_FAILED))
    }

    // ===== CommandDefinition Tests =====

    @Test
    fun `CommandDefinition with full details`() {
        val definition = CommandDefinition(
            id = "cmd-def-001",
            name = "Go Back",
            description = "Navigate back one screen",
            category = "navigation",
            patterns = listOf("go back", "back", "previous"),
            parameters = listOf(
                CommandParameter(
                    name = "steps",
                    type = ParameterType.NUMBER,
                    required = false,
                    defaultValue = 1,
                    description = "Number of steps to go back"
                )
            ),
            requiredPermissions = listOf("android.permission.ACCESS_FINE_LOCATION"),
            supportedLanguages = listOf("en", "es", "fr"),
            requiredContext = setOf("packageName", "activityName")
        )

        assertEquals("cmd-def-001", definition.id)
        assertEquals("Go Back", definition.name)
        assertEquals(3, definition.patterns.size)
        assertEquals(1, definition.parameters.size)
        assertEquals("steps", definition.parameters[0].name)
        assertTrue(definition.supportedLanguages.contains("en"))
        assertTrue(definition.requiredContext.contains("packageName"))
    }

    @Test
    fun `CommandDefinition with minimal properties`() {
        val definition = CommandDefinition(
            id = "cmd-def-002",
            name = "Test",
            description = "Test command",
            category = "test",
            patterns = listOf("test")
        )

        assertTrue(definition.parameters.isEmpty())
        assertTrue(definition.requiredPermissions.isEmpty())
        assertEquals(1, definition.supportedLanguages.size)
        assertEquals("en", definition.supportedLanguages[0])
        assertTrue(definition.requiredContext.isEmpty())
    }

    // ===== CommandParameter Tests =====

    @Test
    fun `CommandParameter required with default`() {
        val param = CommandParameter(
            name = "count",
            type = ParameterType.NUMBER,
            required = true,
            defaultValue = 5,
            description = "Number of items"
        )

        assertEquals("count", param.name)
        assertEquals(ParameterType.NUMBER, param.type)
        assertTrue(param.required)
        assertEquals(5, param.defaultValue)
    }

    @Test
    fun `CommandParameter optional without default`() {
        val param = CommandParameter(
            name = "message",
            type = ParameterType.STRING,
            required = false
        )

        assertEquals("message", param.name)
        assertFalse(param.required)
        assertNull(param.defaultValue)
        assertNull(param.description)
    }

    // ===== ParameterType Tests =====

    @Test
    fun `ParameterType enum values`() {
        val types = ParameterType.values()
        assertEquals(6, types.size)
        assertTrue(types.contains(ParameterType.STRING))
        assertTrue(types.contains(ParameterType.NUMBER))
        assertTrue(types.contains(ParameterType.BOOLEAN))
        assertTrue(types.contains(ParameterType.LIST))
        assertTrue(types.contains(ParameterType.MAP))
        assertTrue(types.contains(ParameterType.CUSTOM))
    }

    // ===== CommandHistoryEntry Tests =====

    @Test
    fun `CommandHistoryEntry captures execution`() {
        val timestamp = 1700000000000L
        val command = Command(
            id = "cmd-006",
            text = "test",
            source = CommandSource.VOICE,
            timestamp = timestamp
        )

        val result = CommandResult(
            success = true,
            command = command,
            executionTime = 100
        )

        val entry = CommandHistoryEntry(
            command = command,
            result = result,
            timestamp = timestamp
        )

        assertEquals(command, entry.command)
        assertEquals(result, entry.result)
        assertEquals(timestamp, entry.timestamp)
    }

    // ===== CommandEvent Tests =====

    @Test
    fun `CommandEvent for command lifecycle`() {
        val timestamp = 1700000000000L
        val command = Command(
            id = "cmd-007",
            text = "test",
            source = CommandSource.VOICE,
            timestamp = timestamp
        )

        val event = CommandEvent(
            type = EventType.COMMAND_RECEIVED,
            command = command,
            message = "Command received from voice input",
            timestamp = timestamp
        )

        assertEquals(EventType.COMMAND_RECEIVED, event.type)
        assertEquals(command, event.command)
        assertNull(event.result)
        assertEquals("Command received from voice input", event.message)
    }

    @Test
    fun `CommandEvent for system event`() {
        val timestamp = 1700000000000L
        val event = CommandEvent(
            type = EventType.COMMAND_REGISTERED,
            message = "New command registered",
            timestamp = timestamp
        )

        assertEquals(EventType.COMMAND_REGISTERED, event.type)
        assertNull(event.command)
        assertNull(event.result)
    }

    // ===== EventType Tests =====

    @Test
    fun `EventType enum values`() {
        val types = EventType.values()
        assertEquals(6, types.size)
        assertTrue(types.contains(EventType.COMMAND_RECEIVED))
        assertTrue(types.contains(EventType.COMMAND_EXECUTING))
        assertTrue(types.contains(EventType.COMMAND_COMPLETED))
        assertTrue(types.contains(EventType.COMMAND_FAILED))
        assertTrue(types.contains(EventType.COMMAND_REGISTERED))
        assertTrue(types.contains(EventType.COMMAND_UNREGISTERED))
    }

    // ===== CommandInfo Tests =====

    @Test
    fun `CommandInfo for standard command`() {
        val info = CommandInfo(
            id = "cmd-info-001",
            name = "Go Home",
            category = "navigation",
            isCustom = false,
            usageCount = 42
        )

        assertEquals("cmd-info-001", info.id)
        assertEquals("Go Home", info.name)
        assertEquals("navigation", info.category)
        assertFalse(info.isCustom)
        assertEquals(42, info.usageCount)
    }

    @Test
    fun `CommandInfo for custom command`() {
        val info = CommandInfo(
            id = "custom-001",
            name = "My Custom Command",
            category = "custom",
            isCustom = true,
            usageCount = 5
        )

        assertTrue(info.isCustom)
    }

    @Test
    fun `CommandInfo with defaults`() {
        val info = CommandInfo(
            id = "cmd-info-002",
            name = "Test",
            category = "test"
        )

        assertFalse(info.isCustom)
        assertEquals(0, info.usageCount)
    }

    // ===== CommandStats Tests =====

    @Test
    fun `CommandStats calculation`() {
        val stats = CommandStats(
            totalCommands = 100,
            successfulCommands = 85,
            failedCommands = 15,
            averageExecutionTime = 120,
            topCommands = listOf("go back", "go home", "scroll down")
        )

        assertEquals(100, stats.totalCommands)
        assertEquals(85, stats.successfulCommands)
        assertEquals(15, stats.failedCommands)
        assertEquals(120, stats.averageExecutionTime)
        assertEquals(3, stats.topCommands.size)
        assertTrue(stats.topCommands.contains("go back"))
    }

    @Test
    fun `CommandStats success rate calculation`() {
        val stats = CommandStats(
            totalCommands = 100,
            successfulCommands = 85,
            failedCommands = 15,
            averageExecutionTime = 120,
            topCommands = emptyList()
        )

        // Verify total = successful + failed
        assertEquals(stats.totalCommands, stats.successfulCommands + stats.failedCommands)
    }

    // ===== CommandCategory Tests =====

    @Test
    fun `CommandCategory enum values`() {
        val categories = CommandCategory.values()
        assertEquals(11, categories.size)
        assertTrue(categories.contains(CommandCategory.NAVIGATION))
        assertTrue(categories.contains(CommandCategory.TEXT))
        assertTrue(categories.contains(CommandCategory.MEDIA))
        assertTrue(categories.contains(CommandCategory.SYSTEM))
        assertTrue(categories.contains(CommandCategory.APP))
        assertTrue(categories.contains(CommandCategory.ACCESSIBILITY))
        assertTrue(categories.contains(CommandCategory.VOICE))
        assertTrue(categories.contains(CommandCategory.GESTURE))
        assertTrue(categories.contains(CommandCategory.CUSTOM))
        assertTrue(categories.contains(CommandCategory.INPUT))
        assertTrue(categories.contains(CommandCategory.APP_CONTROL))
    }

    // ===== AccessibilityActions Tests =====

    @Test
    fun `AccessibilityActions constants`() {
        assertEquals(0x10000, AccessibilityActions.ACTION_SELECT_ALL)
        assertEquals(0x20000, AccessibilityActions.ACTION_BACKUP_AND_RESET_SETTINGS)
    }

    // ===== Data Class Equality Tests =====

    @Test
    fun `Command equality`() {
        val timestamp = 1700000000000L
        val cmd1 = Command("id", "text", CommandSource.VOICE, null, emptyMap(), timestamp, 1.0f)
        val cmd2 = Command("id", "text", CommandSource.VOICE, null, emptyMap(), timestamp, 1.0f)
        val cmd3 = Command("other", "text", CommandSource.VOICE, null, emptyMap(), timestamp, 1.0f)

        assertEquals(cmd1, cmd2)
        assertNotEquals(cmd1, cmd3)
    }

    @Test
    fun `CommandContext equality`() {
        val ctx1 = CommandContext(packageName = "com.example.app")
        val ctx2 = CommandContext(packageName = "com.example.app")
        val ctx3 = CommandContext(packageName = "com.other.app")

        assertEquals(ctx1, ctx2)
        assertNotEquals(ctx1, ctx3)
    }

    @Test
    fun `CommandError equality`() {
        val err1 = CommandError(ErrorCode.TIMEOUT, "Timeout", "Details")
        val err2 = CommandError(ErrorCode.TIMEOUT, "Timeout", "Details")
        val err3 = CommandError(ErrorCode.TIMEOUT, "Timeout", "Other")

        assertEquals(err1, err2)
        assertNotEquals(err1, err3)
    }

    // ===== Copy Tests =====

    @Test
    fun `Command copy with modifications`() {
        val original = Command(
            id = "cmd-008",
            text = "original",
            source = CommandSource.VOICE,
            timestamp = 1700000000000L
        )

        val modified = original.copy(text = "modified")

        assertEquals("cmd-008", modified.id)
        assertEquals("modified", modified.text)
        assertEquals(CommandSource.VOICE, modified.source)
    }

    @Test
    fun `CommandContext copy with modifications`() {
        val original = CommandContext(
            packageName = "com.example.app",
            activityName = "MainActivity"
        )

        val modified = original.copy(activityName = "SettingsActivity")

        assertEquals("com.example.app", modified.packageName)
        assertEquals("SettingsActivity", modified.activityName)
    }

    // ===== Integration Tests =====

    @Test
    fun `Complete command execution flow`() {
        // Create command
        val timestamp = 1700000000000L
        val command = Command(
            id = "integration-001",
            text = "go home",
            source = CommandSource.VOICE,
            context = CommandContext(packageName = "com.example.app"),
            timestamp = timestamp,
            confidence = 0.92f
        )

        // Execute (simulated)
        val result = CommandResult(
            success = true,
            command = command,
            response = "Navigated home",
            executionTime = 85
        )

        // Create history entry
        val historyEntry = CommandHistoryEntry(
            command = command,
            result = result,
            timestamp = timestamp + 100
        )

        // Create event
        val event = CommandEvent(
            type = EventType.COMMAND_COMPLETED,
            command = command,
            result = result,
            message = "Command completed successfully",
            timestamp = timestamp + 200
        )

        // Verify full flow
        assertTrue(result.success)
        assertEquals(command, historyEntry.command)
        assertEquals(result, historyEntry.result)
        assertEquals(EventType.COMMAND_COMPLETED, event.type)
    }

    @Test
    fun `Command failure flow with error`() {
        val timestamp = 1700000000000L
        val command = Command(
            id = "integration-002",
            text = "invalid",
            source = CommandSource.TEXT,
            timestamp = timestamp
        )

        val error = CommandError(
            code = ErrorCode.COMMAND_NOT_FOUND,
            message = "Command not found",
            details = "No matching command pattern"
        )

        val result = CommandResult(
            success = false,
            command = command,
            error = error,
            executionTime = 25
        )

        val event = CommandEvent(
            type = EventType.COMMAND_FAILED,
            command = command,
            result = result,
            message = "Command execution failed",
            timestamp = timestamp + 50
        )

        // Verify failure flow
        assertFalse(result.success)
        assertNotNull(result.error)
        assertEquals(ErrorCode.COMMAND_NOT_FOUND, result.error?.code)
        assertEquals(EventType.COMMAND_FAILED, event.type)
    }
}
