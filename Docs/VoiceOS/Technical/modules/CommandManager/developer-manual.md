# CommandManager Developer Manual

**Module:** CommandManager
**Location:** `/modules/managers/CommandManager`
**Namespace:** `com.augmentalis.commandmanager`
**Last Updated:** 2025-12-01
**Version:** 2.0.0 (SQLDelight Migration)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Action Types](#action-types)
5. [Command Definition Format](#command-definition-format)
6. [Adding New Commands](#adding-new-commands)
7. [Macro System](#macro-system)
8. [Multi-Language Support](#multi-language-support)
9. [Testing](#testing)
10. [API Reference](#api-reference)

---

## Overview

CommandManager is the central command processing and routing module for VOS4. It handles:

- **Command Registration**: Register and manage voice command handlers
- **Command Routing**: Route commands to appropriate action handlers
- **Command Execution**: Execute commands with confidence-based filtering
- **Macro Support**: Pre-defined and custom macro sequences
- **Multi-Language**: Locale-aware command loading
- **Context Awareness**: Context-sensitive command availability

### Key Features

- ✅ **Direct Implementation**: Zero overhead, no unnecessary abstractions
- ✅ **Thread-Safe**: Concurrent command execution support
- ✅ **Confidence-Based**: Automatic filtering based on speech recognition confidence
- ✅ **Fuzzy Matching**: Finds best match when exact command not found
- ✅ **Extensible**: Easy to add new commands and actions
- ✅ **Type-Safe**: Strong typing throughout Kotlin codebase

---

## Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────────┐
│                    VoiceOSService                       │
│                 (Accessibility Service)                 │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│                   CommandManager                        │
│  ┌────────────────────────────────────────────────┐    │
│  │  Command Processing Pipeline                   │    │
│  │  1. Receive Command                            │    │
│  │  2. Confidence Filtering                       │    │
│  │  3. Fuzzy Matching (if needed)                 │    │
│  │  4. Route to Action Handler                    │    │
│  │  5. Execute & Return Result                    │    │
│  └────────────────────────────────────────────────┘    │
└──────────────────────┬──────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   ┌─────────┐  ┌──────────┐  ┌──────────┐
   │Navigation│  │  Editing │  │  System  │
   │ Actions  │  │  Actions │  │ Actions  │
   └─────────┘  └──────────┘  └──────────┘
```

### Component Architecture

```
CommandManager
├── CommandRegistry         # System-wide handler registration
├── CommandDefinitions     # Built-in command catalog
├── CommandLoader          # Multi-language command loading
├── CommandLocalizer       # Locale management
├── ConfidenceScorer       # Confidence-based filtering
├── actions/               # Action implementations
│   ├── BaseAction        # Abstract base class
│   ├── NavigationActions # System navigation
│   ├── EditingActions    # Text editing
│   ├── CursorActions     # VoiceCursor integration
│   ├── SystemActions     # System control
│   ├── VolumeActions     # Audio control
│   ├── MacroActions      # Macro sequences
│   ├── ScrollActions     # Scrolling
│   ├── GestureActions    # Gestures
│   └── ... (16 total)
├── models/               # Data models
│   ├── Command          # Command data class
│   ├── CommandResult    # Execution result
│   ├── CommandContext   # Execution context
│   └── CommandDefinition # Command metadata
├── database/            # SQLDelight database (KMP-ready)
│   ├── CommandDatabase  # Database initialization
│   └── sqldelight/      # SQLDelight adapters
│       ├── VoiceCommandDaoAdapter    # Command CRUD operations
│       ├── CommandUsageDaoAdapter    # Usage tracking
│       ├── DatabaseVersionDaoAdapter # Version management
│       ├── VoiceCommandEntity        # Command data class
│       └── TypeAliases               # Backward compatibility
└── routing/            # Command routing
    └── IntentDispatcher # Intent-based routing
```

### Design Principles

1. **Direct Implementation**: No interfaces unless strategically valuable
2. **Performance First**: Optimized for low-latency command execution
3. **Fail-Safe**: Always return a result, never throw exceptions to caller
4. **Immutable Data**: All data models are immutable (`data class`)
5. **Coroutine-Based**: All execution is suspendable for async operations

### Database Architecture (SQLDelight)

As of v2.0.0, CommandManager uses **SQLDelight** instead of Room for database operations. This enables Kotlin Multiplatform (KMP) compatibility.

#### SQLDelight Schemas

Located in: `libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/command/`

| Schema | Purpose |
|--------|---------|
| `VoiceCommand.sq` | Voice command CRUD operations |
| `CommandUsage.sq` | Command usage tracking and analytics |
| `DatabaseVersion.sq` | Database version management |

#### Adapter Classes

CommandManager uses adapter classes to maintain backward compatibility with the previous Room-style API:

```kotlin
// SQLDelight adapter - wraps generated queries
class VoiceCommandDaoAdapter(
    private val queries: VoiceCommandQueries
) {
    suspend fun insert(command: VoiceCommandEntity) = withContext(Dispatchers.IO) {
        queries.insertCommand(
            id = command.id,
            locale = command.locale,
            primary_text = command.primaryText,
            // ... other fields
        )
    }

    suspend fun getCommandsForLocale(locale: String): List<VoiceCommandEntity> =
        withContext(Dispatchers.IO) {
            queries.getCommandsForLocale(locale).executeAsList().map { it.toEntity() }
        }
}
```

#### Migration from Room

The migration preserved all existing functionality:

| Room | SQLDelight Equivalent |
|------|----------------------|
| `@Entity` | `.sq` schema definition |
| `@Dao` | `*DaoAdapter` class |
| `@Query` | SQL in `.sq` file |
| `Room.databaseBuilder()` | `VoiceOSDatabase()` factory |

---

## Core Components

### 1. CommandManager

**Location:** `com.augmentalis.commandmanager.CommandManager`

The main entry point for command execution.

#### Initialization

```kotlin
// Get singleton instance
val commandManager = CommandManager.getInstance(context)

// Initialize
commandManager.initialize()
```

#### Command Execution

```kotlin
// Create command
val command = Command(
    id = "nav_back",
    text = "go back",
    source = CommandSource.VOICE,
    confidence = 0.95f
)

// Execute with confidence filtering
val result = commandManager.executeCommand(command)

// Check result
if (result.success) {
    Log.d(TAG, "Success: ${result.response}")
} else {
    Log.e(TAG, "Error: ${result.error?.message}")
}
```

#### Confidence Levels

CommandManager uses four confidence levels:

| Level | Range | Behavior |
|-------|-------|----------|
| **HIGH** | 0.85-1.00 | Execute immediately |
| **MEDIUM** | 0.70-0.84 | Request confirmation (if callback set) |
| **LOW** | 0.50-0.69 | Show alternatives (if callback set) |
| **REJECT** | 0.00-0.49 | Reject command, return error |

#### Setting Callbacks

```kotlin
// Confirmation callback for MEDIUM confidence
commandManager.setConfirmationCallback { command, confidenceLevel ->
    // Show confirmation dialog to user
    // Return true to proceed, false to cancel
    showConfirmationDialog(command)
}

// Alternatives callback for LOW confidence
commandManager.setAlternativesCallback { command, alternatives ->
    // Show alternatives to user
    // Return selected command ID or null to cancel
    showAlternativesDialog(alternatives)
}
```

#### Multi-Language Support

```kotlin
// Get current locale
val locale = commandManager.getCurrentLocale() // e.g., "en-US"

// Get available locales
val locales = commandManager.getAvailableLocales() // ["en-US", "es-ES", "fr-FR", "de-DE"]

// Switch locale
val success = commandManager.switchLocale("es-ES")

// Reset to system locale
commandManager.resetToSystemLocale()
```

### 2. CommandRegistry

**Location:** `com.augmentalis.commandmanager.CommandRegistry`

Singleton registry for system-wide command routing.

#### Registration

```kotlin
// Register handler for a module
val handler = CursorCommandHandler()
CommandRegistry.registerHandler("voicecursor", handler)

// Unregister handler
CommandRegistry.unregisterHandler("voicecursor")
```

#### Routing

```kotlin
// Route command to appropriate handler
val success = CommandRegistry.routeCommand("cursor up")
// Returns true if handled, false if no handler found
```

#### Handler Discovery

```kotlin
// Get specific handler
val handler = CommandRegistry.getHandler("voicecursor")

// Get all handlers
val allHandlers = CommandRegistry.getAllHandlers()

// Get all supported commands
val commands = CommandRegistry.getAllSupportedCommands()

// Check registration
val isRegistered = CommandRegistry.isHandlerRegistered("voicecursor")
```

#### Thread Safety

All `CommandRegistry` operations are thread-safe:
- Uses `ConcurrentHashMap` internally
- Safe concurrent registration/routing
- No external synchronization needed

### 3. CommandDefinitions

**Location:** `com.augmentalis.commandmanager.definitions.CommandDefinitions`

Manages built-in command definitions.

#### Usage

```kotlin
val definitions = CommandDefinitions()
definitions.loadBuiltInCommands()

// Get all definitions
val allDefs = definitions.getAllDefinitions()

// Get contextual commands
val context = CommandContext(
    packageName = "com.android.chrome",
    viewId = "EditText"
)
val contextualDefs = definitions.getContextualCommands(context)

// Add custom definition
val customDef = CommandDefinition(
    id = "custom_command",
    name = "My Custom Command",
    description = "Does something custom",
    category = "CUSTOM",
    patterns = listOf("do custom thing", "custom action")
)
definitions.addCustomDefinition(customDef)
```

#### Command Categories

- `NAVIGATION` - System navigation (back, home, recents)
- `INPUT` - Text input and editing (type, copy, paste)
- `MEDIA` - Media control (volume, mute)
- `SYSTEM` - System control (WiFi, Bluetooth, settings)
- `APP_CONTROL` - App management (open, close)
- `ACCESSIBILITY` - Accessibility features
- `CUSTOM` - User-defined commands

---

## Action Types

CommandManager includes 16 action types:

### 1. NavigationActions

**Location:** `actions/NavigationActions.kt`

System navigation commands.

#### Available Actions

| Action | Command | Description |
|--------|---------|-------------|
| `BackAction` | "go back", "back" | Navigate back |
| `HomeAction` | "go home", "home" | Go to home screen |
| `RecentAppsAction` | "recent apps", "recents" | Open recent apps |
| `NotificationsAction` | "notifications" | Open notification panel |
| `QuickSettingsAction` | "quick settings" | Open quick settings |
| `PowerDialogAction` | "power dialog" | Open power menu |
| `SplitScreenAction` | "split screen" | Toggle split screen |
| `LockScreenAction` | "lock screen" | Lock device |
| `ScreenshotAction` | "screenshot", "take screenshot" | Capture screenshot |
| `AccessibilitySettingsAction` | "accessibility settings" | Open accessibility settings |
| `DismissNotificationAction` | "dismiss notifications" | Close notification panel |
| `AllAppsAction` | "all apps" | Open app drawer |

#### Implementation Pattern

```kotlin
class BackAction : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        return if (performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_BACK)) {
            createSuccessResult(command, "Navigated back")
        } else {
            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to navigate back")
        }
    }
}
```

### 2. EditingActions

**Location:** `actions/EditingActions.kt`

Text editing and manipulation commands.

#### Available Actions

| Action | Command | Description |
|--------|---------|-------------|
| Copy | "copy" | Copy selected text to clipboard |
| Paste | "paste" | Paste clipboard content |
| Cut | "cut" | Cut selected text to clipboard |
| Select All | "select all" | Select all text in focused field |
| Undo | "undo" | Undo last edit (API 24+) |
| Redo | "redo" | Redo last undo (API 24+) |

#### Key Features

- Clipboard integration
- Fallback to node-level actions
- Focus detection for editable fields
- Android version compatibility

#### Example

```kotlin
class EditingActions(
    private val context: Context,
    private val accessibilityService: AccessibilityService?
) : BaseAction() {

    private fun performCopy(command: Command): CommandResult {
        val success = accessibilityService?.performGlobalAction(
            AccessibilityNodeInfo.ACTION_COPY
        ) ?: false

        return if (success) {
            createSuccessResult(command, "Text copied")
        } else {
            // Fallback to node-level copy
            val fallbackSuccess = performNodeCopy()
            if (fallbackSuccess) {
                createSuccessResult(command, "Text copied (fallback)")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Copy failed")
            }
        }
    }
}
```

### 3. CursorActions

**Location:** `actions/CursorActions.kt`

VoiceCursor integration - delegates to `VoiceCursorAPI`.

#### Available Actions

**Movement:**
- `moveCursor(direction, distance)` - Move cursor in direction
- `centerCursor()` - Center cursor on screen

**Click Actions:**
- `click()` - Single click
- `doubleClick()` - Double click
- `longPress()` - Long press

**Visibility:**
- `showCursor(config)` - Show cursor with config
- `hideCursor()` - Hide cursor
- `toggleCursor()` - Toggle visibility

**Configuration:**
- `showCoordinates()` - Show coordinate display
- `hideCoordinates()` - Hide coordinate display
- `toggleCoordinates()` - Toggle coordinate display
- `setCursorType(type)` - Set cursor appearance

**Scrolling:**
- `scrollUp()` - Scroll up at cursor position
- `scrollDown()` - Scroll down at cursor position

**Advanced:**
- `showMenu()` - Show cursor context menu
- `openSettings(context)` - Open cursor settings
- `calibrate()` - Calibrate cursor tracking (TODO)

#### Example

```kotlin
// Move cursor
val success = CursorActions.moveCursor(CursorDirection.UP, distance = 50f)

// Perform click
val clickSuccess = CursorActions.click()

// Show cursor with custom config
val config = CursorConfig(
    type = CursorType.Hand,
    showCoordinates = true
)
val showSuccess = CursorActions.showCursor(config)
```

### 4. SystemActions

**Location:** `actions/SystemActions.kt`

System control and device management.

#### Available Actions

**Connectivity:**
- `WifiToggleAction` - Toggle WiFi
- `WifiEnableAction` - Enable WiFi
- `WifiDisableAction` - Disable WiFi
- `BluetoothToggleAction` - Toggle Bluetooth
- `BluetoothEnableAction` - Enable Bluetooth
- `BluetoothDisableAction` - Disable Bluetooth

**Settings:**
- `OpenSettingsAction` - Open system settings (with category parameter)

**Information:**
- `DeviceInfoAction` - Get device information
- `BatteryStatusAction` - Get battery status
- `NetworkStatusAction` - Get network connection status
- `StorageInfoAction` - Get storage information

#### Settings Categories

```kotlin
// Open specific settings page
val command = Command(
    id = "open_settings",
    text = "open wifi settings",
    source = CommandSource.VOICE,
    parameters = mapOf("category" to "wifi")
)
```

**Supported Categories:**
- `wifi`, `bluetooth`, `sound`, `display`, `battery`, `storage`
- `apps`, `security`, `privacy`, `accessibility`
- `language`, `date`, `location`, `accounts`, `backup`
- `developer`, `system`, `about`, `network`, `hotspot`, `vpn`, `nfc`

### 5. VolumeActions

**Location:** `actions/VolumeActions.kt`

Audio volume control.

#### Available Actions

| Action | Parameters | Description |
|--------|------------|-------------|
| Volume Up | `steps` (optional), `stream` (optional) | Increase volume |
| Volume Down | `steps` (optional), `stream` (optional) | Decrease volume |
| Mute | `stream` (optional) | Mute audio stream |
| Unmute | `stream` (optional) | Unmute audio stream |

**Audio Streams:**
- `MUSIC` - Media playback
- `RING` - Ringtone
- `NOTIFICATION` - Notifications
- `ALARM` - Alarms
- `SYSTEM` - System sounds
- `VOICE_CALL` - Phone calls

### 6. MacroActions

**Location:** `actions/MacroActions.kt`

Pre-defined macro sequences.

**See [Macro System](#macro-system) section for details.**

### 7-16. Other Action Types

**ScrollActions:** Scrolling commands (up, down, left, right)
**GestureActions:** Gesture commands (swipe, pinch, rotate)
**DragActions:** Drag and drop operations
**DictationActions:** Dictation mode control
**TextActions:** Text manipulation (insert, replace, format)
**AppActions:** Application control (launch, switch, close)
**NotificationActions:** Notification management
**OverlayActions:** Overlay control (HUD, popups)
**ShortcutActions:** Custom shortcuts

---

## Command Definition Format

Commands are defined using the `CommandDefinition` data class.

### Structure

```kotlin
data class CommandDefinition(
    val id: String,                          // Unique identifier
    val name: String,                        // Display name
    val description: String,                 // Human-readable description
    val category: String,                    // Category (see categories above)
    val patterns: List<String>,              // Voice command patterns
    val parameters: List<CommandParameter> = emptyList(),  // Optional parameters
    val requiredPermissions: List<String> = emptyList(),   // Required Android permissions
    val supportedLanguages: List<String> = listOf("en"),   // Supported languages
    val requiredContext: Set<String> = emptySet()          // Required context (e.g., "text_input")
)
```

### Example Definition

```kotlin
CommandDefinition(
    id = "cursor_click",
    name = "Click",
    description = "Perform click action at cursor position",
    category = "INPUT",
    patterns = listOf("click", "tap", "press", "select"),
    parameters = listOf(
        CommandParameter("target", ParameterType.STRING, required = false),
        CommandParameter("x", ParameterType.NUMBER, required = false),
        CommandParameter("y", ParameterType.NUMBER, required = false)
    ),
    requiredPermissions = emptyList(),
    supportedLanguages = listOf("en", "es", "fr", "de"),
    requiredContext = emptySet()
)
```

### Parameter Definition

```kotlin
data class CommandParameter(
    val name: String,              // Parameter name
    val type: ParameterType,       // Parameter type
    val required: Boolean = false, // Is required?
    val defaultValue: Any? = null, // Default value if not provided
    val description: String? = null // Parameter description
)

enum class ParameterType {
    STRING,   // Text string
    NUMBER,   // Numeric value
    BOOLEAN,  // True/false
    LIST,     // List of values
    MAP,      // Key-value pairs
    CUSTOM    // Custom type
}
```

---

## Adding New Commands

### Step 1: Create Action Class

Create a new action class in `actions/` directory:

```kotlin
// File: actions/MyCustomActions.kt
package com.augmentalis.commandmanager.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.augmentalis.commandmanager.models.*

class MyCustomActions : BaseAction() {

    companion object {
        private const val TAG = "MyCustomActions"
    }

    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        return when (command.id) {
            "my_custom_command" -> performCustomAction(command, context)
            else -> createErrorResult(
                command,
                ErrorCode.UNKNOWN_COMMAND,
                "Unknown command: ${command.id}"
            )
        }
    }

    private fun performCustomAction(
        command: Command,
        context: Context
    ): CommandResult {
        return try {
            // Your custom logic here
            val result = doSomethingCustom()

            if (result) {
                createSuccessResult(command, "Custom action completed")
            } else {
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Custom action failed"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Custom action error", e)
            createErrorResult(
                command,
                ErrorCode.EXECUTION_FAILED,
                "Error: ${e.message}"
            )
        }
    }

    private fun doSomethingCustom(): Boolean {
        // Your implementation
        return true
    }
}
```

### Step 2: Add Command Definition

Add to `CommandDefinitions.kt`:

```kotlin
private fun loadCustomCommands() {
    builtInDefinitions["my_custom_command"] = CommandDefinition(
        id = "my_custom_command",
        name = "My Custom Command",
        description = "Does something custom",
        category = "CUSTOM",
        patterns = listOf("do custom thing", "custom action", "my command"),
        parameters = listOf(
            CommandParameter("param1", ParameterType.STRING, required = false)
        )
    )
}
```

### Step 3: Register in CommandManager

Add to `CommandManager.kt` initialization:

```kotlin
private val customActions = mapOf(
    "my_custom_command" to MyCustomActions()
)

private suspend fun executeCommandInternal(command: Command): CommandResult {
    var action = when {
        // ... existing mappings ...
        command.id == "my_custom_command" -> customActions[command.id]
        else -> null
    }

    // ... rest of execution logic ...
}
```

### Step 4: Add Tests

Create test file `actions/MyCustomActionsTest.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MyCustomActionsTest {

    private lateinit var actions: MyCustomActions
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        actions = MyCustomActions()
    }

    @Test
    fun testCustomAction() = runTest {
        val command = Command(
            id = "my_custom_command",
            text = "do custom thing",
            source = CommandSource.VOICE
        )

        val result = actions.execute(command, null, context)

        assertTrue(result.success)
        assertEquals("Custom action completed", result.response)
    }
}
```

### Step 5: Document

1. Add to this manual under [Action Types](#action-types)
2. Add to user manual with user-friendly description
3. Update changelog

---

## Macro System

Macros allow sequential execution of multiple commands.

### Pre-Defined Macros

**Location:** `actions/MacroActions.kt`

#### Available Macros

| Macro | Steps | Category | Description |
|-------|-------|----------|-------------|
| **Select All and Copy** | `["select all", "copy"]` | EDITING | Select all text and copy to clipboard |
| **Select All and Cut** | `["select all", "cut"]` | EDITING | Select all text and cut to clipboard |
| **Paste and Enter** | `["paste", "press enter"]` | EDITING | Paste content and press Enter |
| **Screenshot and Share** | `["screenshot", "share"]` | PRODUCTIVITY | Take screenshot and open share dialog |

### Macro Structure

```kotlin
data class Macro(
    val name: String,
    val steps: List<String>,
    val category: MacroCategory,
    val description: String,
    val parameters: List<MacroParameter> = emptyList()
)

enum class MacroCategory {
    EDITING,        // Text/content editing workflows
    NAVIGATION,     // App/screen navigation workflows
    ACCESSIBILITY,  // Accessibility-focused workflows
    PRODUCTIVITY    // General productivity workflows
}
```

### Macro Variables

Macros support parameterized steps:

```kotlin
data class MacroParameter(
    val name: String,
    val type: MacroParameterType,
    val description: String,
    val defaultValue: String? = null,
    val required: Boolean = false
)

enum class MacroParameterType {
    APP,      // Application name/package
    TEXT,     // Text string
    NUMBER    // Numeric value
}
```

**Example with Variables:**

```kotlin
Macro(
    name = "Open App and Navigate",
    steps = listOf(
        "open [app]",
        "wait 500",
        "navigate to [section]"
    ),
    category = MacroCategory.NAVIGATION,
    description = "Open app and navigate to section",
    parameters = listOf(
        MacroParameter("app", MacroParameterType.APP, "App to open", required = true),
        MacroParameter("section", MacroParameterType.TEXT, "Section to navigate to")
    )
)
```

### Executing Macros

```kotlin
val macroActions = MacroActions(commandExecutor)

val command = Command(
    id = "macro",
    text = "select all and copy",
    source = CommandSource.VOICE,
    parameters = mapOf() // Variables go here
)

val result = macroActions.execute(command, accessibilityService, context)

// Check result
if (result.success) {
    val data = result.data as MacroExecutionData
    println("Completed ${data.completedSteps}/${data.totalSteps} steps")
}
```

### Macro Execution Data

```kotlin
data class MacroExecutionData(
    val macroName: String,
    val totalSteps: Int,
    val completedSteps: Int,
    val stepResults: List<StepResult>,
    val executionTimeMs: Long = 0
)

data class StepResult(
    val stepNumber: Int,
    val stepText: String,
    val success: Boolean,
    val error: String? = null
)
```

### Creating Custom Macros (Future)

Currently stubbed for V2. Will support:
- User-created macros
- Macro sharing (export/import)
- Conditional logic (if/then/else)
- Loop support
- Macro marketplace

---

## Multi-Language Support

CommandManager supports multiple languages through a locale-aware system.

### Supported Locales

- `en-US` - English (United States)
- `es-ES` - Spanish (Spain)
- `fr-FR` - French (France)
- `de-DE` - German (Germany)

### Command Files

Located in: `src/main/assets/commands/`

```
commands/
├── en-US/
│   ├── navigation.json
│   ├── editing.json
│   └── system.json
├── es-ES/
│   ├── navigation.json
│   └── ...
└── ...
```

### JSON Format

```json
{
  "locale": "en-US",
  "commands": [
    {
      "id": "nav_back",
      "patterns": ["go back", "back", "navigate back", "previous", "return"],
      "category": "NAVIGATION",
      "description": "Navigate back to previous screen"
    },
    {
      "id": "nav_home",
      "patterns": ["go home", "home", "home screen", "main screen"],
      "category": "NAVIGATION",
      "description": "Go to home screen"
    }
  ]
}
```

### Adding New Locale

1. **Create locale directory** in `assets/commands/` (e.g., `ja-JP/`)

2. **Create command JSON files** for each category

3. **Translate command patterns** to target language

4. **Test locale loading:**

```kotlin
val commandManager = CommandManager.getInstance(context)
val success = commandManager.switchLocale("ja-JP")
```

### Locale Fallback

If requested locale not found:
1. Try language-only match (e.g., `en-US` → `en`)
2. Fall back to English (`en-US`)
3. Log warning and continue

---

## Testing

### Test Structure

```
src/test/java/com/augmentalis/commandmanager/
├── CommandManagerTest.kt
├── CommandRegistryTest.kt
├── actions/
│   ├── EditingActionsTest.kt
│   ├── MacroActionsTest.kt
│   ├── CursorActionsTest.kt
│   └── ... (all action tests)
├── cache/
│   └── CommandCacheTest.kt
├── context/
│   ├── CommandContextManagerTest.kt
│   └── ContextAwareCommandsTest.kt
├── dynamic/
│   └── DynamicCommandRegistryTest.kt
├── integration/
│   └── CommandManagerIntegrationTest.kt
├── learning/
│   └── HybridLearningServiceTest.kt
└── ui/
    └── CommandViewModelTest.kt
```

### Running Tests

```bash
# Run all tests
./gradlew :CommandManager:test

# Run specific test class
./gradlew :CommandManager:test --tests EditingActionsTest

# Run with coverage
./gradlew :CommandManager:jacocoTestReport
```

### Writing Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class MyActionTest {

    private lateinit var action: MyAction
    private lateinit var context: Context
    private lateinit var mockAccessibilityService: AccessibilityService

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mockAccessibilityService = mock()
        action = MyAction(context, mockAccessibilityService)
    }

    @Test
    fun testActionExecution() = runTest {
        // Arrange
        val command = Command(
            id = "test_command",
            text = "test",
            source = CommandSource.VOICE
        )

        // Act
        val result = action.execute(command, mockAccessibilityService, context)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.response)
    }

    @Test
    fun testActionFailure() = runTest {
        // Test error handling
        val command = Command(
            id = "invalid_command",
            text = "invalid",
            source = CommandSource.VOICE
        )

        val result = action.execute(command, mockAccessibilityService, context)

        assertFalse(result.success)
        assertNotNull(result.error)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
    }
}
```

### Test Coverage

Target: **80%+ coverage** for all action classes

Current coverage: See `build/reports/jacoco/test/html/index.html`

---

## API Reference

### CommandManager

#### Methods

```kotlin
// Instance
fun getInstance(context: Context): CommandManager

// Lifecycle
fun initialize()
fun cleanup()
fun restart()

// Command Execution
suspend fun executeCommand(command: Command): CommandResult
suspend fun executeCommandWithConfidenceOverride(command: Command): CommandResult

// Callbacks
fun setConfirmationCallback(callback: (Command, ConfidenceLevel) -> Boolean)
fun setAlternativesCallback(callback: (Command, List<String>) -> String?)
fun setServiceCallback(callback: ServiceCallback)

// Multi-Language
fun getCurrentLocale(): String
suspend fun getAvailableLocales(): List<String>
suspend fun switchLocale(locale: String): Boolean
suspend fun resetToSystemLocale(): Boolean

// Health
fun healthCheck(): Boolean
```

### CommandRegistry

#### Methods

```kotlin
// Registration
fun registerHandler(moduleId: String, handler: CommandHandler)
fun unregisterHandler(moduleId: String)

// Routing
suspend fun routeCommand(command: String): Boolean

// Discovery
fun getHandler(moduleId: String): CommandHandler?
fun getAllHandlers(): List<CommandHandler>
fun getAllSupportedCommands(): List<String>
fun isHandlerRegistered(moduleId: String): Boolean
fun getHandlerCount(): Int

// Management
fun clearAllHandlers() // WARNING: Testing only
```

### Data Models

#### Command

```kotlin
data class Command(
    val id: String,
    val text: String,
    val source: CommandSource,
    val context: CommandContext? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)
```

#### CommandResult

```kotlin
data class CommandResult(
    val success: Boolean,
    val command: Command,
    val response: String? = null,
    val data: Any? = null,
    val error: CommandError? = null,
    val executionTime: Long = 0
)
```

#### CommandError

```kotlin
data class CommandError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null
)

enum class ErrorCode {
    MODULE_NOT_AVAILABLE,
    COMMAND_NOT_FOUND,
    INVALID_PARAMETERS,
    PERMISSION_DENIED,
    EXECUTION_FAILED,
    TIMEOUT,
    NETWORK_ERROR,
    UNKNOWN,
    UNKNOWN_COMMAND,
    MISSING_CONTEXT,
    CANCELLED,
    NO_ACCESSIBILITY_SERVICE,
    ACTION_FAILED
}
```

#### CommandContext

```kotlin
data class CommandContext(
    val packageName: String? = null,
    val activityName: String? = null,
    val viewId: String? = null,
    val screenContent: String? = null,
    val userLocation: String? = null,
    val deviceState: Map<String, Any> = emptyMap(),
    val focusedElement: String? = null,
    val customData: Map<String, Any> = emptyMap()
)
```

---

## Quick Reference

### Common Patterns

**Execute Command:**
```kotlin
val result = commandManager.executeCommand(command)
```

**Check Result:**
```kotlin
if (result.success) {
    // Success
} else {
    Log.e(TAG, result.error?.message)
}
```

**Register Handler:**
```kotlin
CommandRegistry.registerHandler("mymodule", handler)
```

**Route Command:**
```kotlin
val success = CommandRegistry.routeCommand("my command")
```

**Switch Locale:**
```kotlin
commandManager.switchLocale("es-ES")
```

### Best Practices

1. ✅ **Always check result.success** before accessing data
2. ✅ **Use suspend functions** for all command execution
3. ✅ **Log errors** for debugging and analytics
4. ✅ **Provide user feedback** via callbacks
5. ✅ **Handle permissions** gracefully
6. ✅ **Test edge cases** (null accessibility service, missing context)
7. ✅ **Use BaseAction** as parent for all actions
8. ✅ **Follow naming conventions** (PascalCase classes, camelCase methods)

---

## Related Documentation

- [User Manual](./user-manual.md) - End-user command reference
- [Changelog](./changelog/CHANGELOG.md) - Version history
- [Architecture](./architecture/) - System architecture docs
- [API Reference](./reference/api/) - Detailed API docs

---

**Last Updated:** 2025-12-01
**Maintained By:** VOS4 Development Team
**Version:** 2.0.0 (SQLDelight Migration)
