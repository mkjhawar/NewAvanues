# VoiceAccessibility Command Handlers Subsystem Documentation

**Document Version:** 1.0
**Created:** 2025-10-10 10:49 PDT
**Module:** VoiceAccessibility
**Subsystem:** Command Handlers
**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/handlers/`

---

## Table of Contents

1. [Subsystem Overview](#subsystem-overview)
2. [Architecture](#architecture)
3. [Handler Registry](#handler-registry)
4. [Individual Handler Documentation](#individual-handler-documentation)
5. [Command Flow](#command-flow)
6. [Extension Guide](#extension-guide)
7. [Common Patterns](#common-patterns)
8. [Error Handling](#error-handling)

---

## 1. Subsystem Overview

### Purpose

The Command Handlers subsystem is the core execution layer of VoiceAccessibility. It translates voice commands into concrete accessibility actions, managing device control, UI interaction, navigation, and system operations.

### Handler Pattern

All handlers implement the `ActionHandler` interface (an approved VOS4 exception), providing:
- **Polymorphic dispatch**: Commands route to appropriate handlers dynamically
- **Type safety**: Handlers stored and managed as a common type
- **Extensibility**: New handlers plug in without modifying core routing
- **Lifecycle management**: Initialize and dispose resources properly

### Key Responsibilities

1. **Command Execution**: Convert voice commands to accessibility actions
2. **State Management**: Track handler-specific state (drag mode, selection, overlays)
3. **Integration**: Coordinate with VoiceOSService and Android Accessibility framework
4. **Validation**: Verify prerequisites (permissions, cursor visibility, etc.)
5. **Feedback**: Provide user feedback on action success/failure

---

## 2. Architecture

### Handler Hierarchy

```
ActionHandler (interface)
├── ActionHandler.kt              - Core interface definition
├── ActionCategory (enum)         - Command categorization
│
├── System & Device Control
│   ├── SystemHandler.kt          - System navigation & settings
│   ├── DeviceHandler.kt          - Volume, brightness, connectivity
│   └── BluetoothHandler.kt       - Bluetooth-specific control
│
├── UI Interaction
│   ├── UIHandler.kt              - Element click, tap, toggle
│   ├── SelectHandler.kt          - Selection mode & context menus
│   └── NumberHandler.kt          - Number overlay interactions
│
├── Navigation & Input
│   ├── NavigationHandler.kt     - Scrolling, swiping
│   ├── InputHandler.kt           - Text input, clipboard operations
│   └── AppHandler.kt             - App launching & management
│
└── Gesture & Movement
    ├── GestureHandler.kt         - Pinch, zoom, path gestures
    ├── DragHandler.kt            - Drag gesture tracking
    └── HelpMenuHandler.kt        - Help system & command discovery
```

### Core Interface: ActionHandler

**File:** `ActionHandler.kt`

```kotlin
interface ActionHandler {
    /**
     * Execute an action
     * @param category The category of action (for logging/metrics)
     * @param action The action to execute
     * @param params Optional parameters for the action
     * @return true if the action was handled successfully
     */
    fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any> = emptyMap()
    ): Boolean

    /**
     * Check if this handler can handle the given action
     * @param action The action to check
     * @return true if this handler can process the action
     */
    fun canHandle(action: String): Boolean

    /**
     * Get supported actions for this handler
     * Used for command discovery and help
     * @return List of supported action patterns
     */
    fun getSupportedActions(): List<String>

    /**
     * Initialize the handler
     * Called once during service startup
     */
    fun initialize() {
        // Default empty implementation
    }

    /**
     * Dispose resources
     * Called when service is destroyed
     */
    fun dispose() {
        // Default empty implementation
    }
}
```

### Action Categories

```kotlin
enum class ActionCategory {
    SYSTEM,      // System-level actions (back, home, settings)
    APP,         // Application launch and control
    DEVICE,      // Device control (volume, brightness, etc)
    INPUT,       // Text input and keyboard control
    NAVIGATION,  // UI navigation and scrolling
    UI,          // UI element interaction
    GESTURE,     // Gesture-based interactions (pinch, drag, swipe)
    GAZE,        // Gaze tracking and eye-based interactions
    CUSTOM       // Custom/plugin actions
}
```

### Integration Points

1. **VoiceOSService**: Main accessibility service providing context and system access
2. **AccessibilityNodeInfo**: Android framework for UI tree traversal
3. **GestureDescription**: Android framework for gesture dispatch
4. **CursorManager**: VOS4 cursor system for visual feedback
5. **NumberOverlay**: VOS4 element labeling system

---

## 3. Handler Registry

### Initialization Flow

1. **Service Startup**: VoiceOSService creates handler instances
2. **Handler Construction**: Each handler receives service reference
3. **Handler Registration**: Handlers registered by category
4. **Handler Initialization**: `initialize()` called on each handler
5. **Ready State**: Handlers ready to process commands

### Command Routing

```kotlin
// Pseudo-code for command routing
fun routeCommand(command: String, category: ActionCategory): Boolean {
    val handler = handlerRegistry[category]

    if (handler == null || !handler.canHandle(command)) {
        // Try fallback handlers or return false
        return false
    }

    return handler.execute(category, command, extractParams(command))
}
```

### Discovery Mechanism

Handlers expose supported commands via `getSupportedActions()`:

```kotlin
val allCommands = handlers.flatMap { it.getSupportedActions() }
```

This enables:
- Help system command listing
- Command autocomplete
- Voice command validation
- Documentation generation

---

## 4. Individual Handler Documentation

### 4.1 SystemHandler

**File:** `SystemHandler.kt`
**Category:** `SYSTEM`
**Purpose:** System-level navigation and settings access

#### Supported Commands

| Voice Command | Description | Global Action |
|--------------|-------------|---------------|
| "back", "go back" | Navigate back | `GLOBAL_ACTION_BACK` |
| "home", "go home" | Go to home screen | `GLOBAL_ACTION_HOME` |
| "recent", "recent apps", "recents" | Show recent apps | `GLOBAL_ACTION_RECENTS` |
| "notifications", "notification panel" | Open notifications | `GLOBAL_ACTION_NOTIFICATIONS` |
| "settings", "quick settings" | Open quick settings | `GLOBAL_ACTION_QUICK_SETTINGS` |
| "power", "power menu" | Show power dialog | `GLOBAL_ACTION_POWER_DIALOG` |
| "screenshot", "take screenshot" | Take screenshot | `GLOBAL_ACTION_TAKE_SCREENSHOT` |
| "split screen", "split" | Toggle split screen | `GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN` |
| "lock", "lock screen" | Lock device | `GLOBAL_ACTION_LOCK_SCREEN` |
| "all apps", "app drawer" | Open app drawer | `GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS` |

#### Special Settings Commands

Pattern: "open settings [type]"

| Settings Type | Intent |
|--------------|--------|
| "wifi" | `ACTION_WIFI_SETTINGS` |
| "bluetooth" | `ACTION_BLUETOOTH_SETTINGS` |
| "accessibility" | `ACTION_ACCESSIBILITY_SETTINGS` |
| "display" | `ACTION_DISPLAY_SETTINGS` |
| "sound", "audio" | `ACTION_SOUND_SETTINGS` |
| "battery" | `ACTION_BATTERY_SAVER_SETTINGS` |
| "storage" | `ACTION_INTERNAL_STORAGE_SETTINGS` |
| "security" | `ACTION_SECURITY_SETTINGS` |
| "location" | `ACTION_LOCATION_SOURCE_SETTINGS` |
| "developer" | `ACTION_APPLICATION_DEVELOPMENT_SETTINGS` |

#### Public API

```kotlin
class SystemHandler(private val service: VoiceOSService) : ActionHandler {
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    override fun canHandle(action: String): Boolean
    override fun getSupportedActions(): List<String>
    private fun openSettings(action: String): Boolean
}
```

#### Example Usage

```kotlin
// Voice: "go back"
systemHandler.execute(ActionCategory.SYSTEM, "go back", emptyMap())
// Result: Performs GLOBAL_ACTION_BACK

// Voice: "open settings wifi"
systemHandler.execute(ActionCategory.SYSTEM, "open settings wifi", emptyMap())
// Result: Opens WiFi settings screen
```

#### Error Conditions

- **API Level Restrictions**: Some actions require Android P+ (API 28+) or S+ (API 31+)
- **Permission Denied**: Settings intents may fail if permissions missing
- **Activity Not Found**: Specific settings may not exist on all devices

---

### 4.2 DeviceHandler

**File:** `DeviceHandler.kt`
**Category:** `DEVICE`
**Purpose:** Device control (volume, brightness, connectivity)

#### Supported Commands

**Volume Control:**
- "volume up" - Increase volume
- "volume down" - Decrease volume
- "volume mute", "mute" - Mute audio
- "volume unmute", "unmute" - Unmute audio

**Brightness Control:**
- "brightness up" - Increase brightness (steps of 25)
- "brightness down" - Decrease brightness (steps of 25)
- "brightness max", "maximum brightness" - Set to 255
- "brightness min", "minimum brightness" - Set to 10 (not 0 to avoid dark screen)

**Connectivity (Settings Launch):**
- "wifi on", "turn on wifi" - Open WiFi settings
- "wifi off", "turn off wifi" - Open WiFi settings
- "bluetooth on", "turn on bluetooth" - Open Bluetooth settings
- "bluetooth off", "turn off bluetooth" - Open Bluetooth settings
- "airplane mode on", "flight mode on" - Open airplane mode settings
- "airplane mode off", "flight mode off" - Open airplane mode settings

**Sound Modes:**
- "silent mode", "silent" - Set ringer to silent
- "vibrate mode", "vibrate" - Set ringer to vibrate
- "normal mode", "sound on" - Set ringer to normal

**Do Not Disturb:**
- "do not disturb on", "dnd on" - Open DND settings
- "do not disturb off", "dnd off" - Open DND settings

#### Public API

```kotlin
class DeviceHandler(private val service: VoiceOSService) : ActionHandler {
    private val audioManager: AudioManager

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun adjustVolume(direction: Int): Boolean
    private fun setRingerMode(mode: Int): Boolean
    private fun adjustBrightness(increase: Boolean): Boolean
    private fun setBrightness(value: Int): Boolean
    private fun openSettings(action: String): Boolean
}
```

#### Example Usage

```kotlin
// Voice: "volume up"
deviceHandler.execute(ActionCategory.DEVICE, "volume up", emptyMap())
// Result: Increases music volume by one step with UI feedback

// Voice: "brightness max"
deviceHandler.execute(ActionCategory.DEVICE, "brightness max", emptyMap())
// Result: Sets screen brightness to 255 and switches to manual mode

// Voice: "silent mode"
deviceHandler.execute(ActionCategory.DEVICE, "silent mode", emptyMap())
// Result: Sets ringer mode to RINGER_MODE_SILENT
```

#### Implementation Notes

**Volume Control:**
- Uses `AudioManager.STREAM_MUSIC` for media volume
- Shows Android volume UI with `FLAG_SHOW_UI`

**Brightness Control:**
- Requires `SCREEN_BRIGHTNESS_MODE_MANUAL`
- Range: 10-255 (not 0 to avoid completely dark screen)
- Step size: 25 (approximately 10%)

**Connectivity:**
- Android 10+ restricts programmatic WiFi/Bluetooth control
- Solution: Open settings screens for user control
- Maintains user control and security

---

### 4.3 BluetoothHandler

**File:** `BluetoothHandler.kt`
**Category:** `DEVICE`
**Purpose:** Bluetooth-specific device control

#### Supported Commands

- "bluetooth on", "bluetooth enable", "turn on bluetooth", "enable bluetooth"
- "bluetooth off", "bluetooth disable", "turn off bluetooth", "disable bluetooth"
- "bluetooth toggle", "toggle bluetooth"
- "bluetooth settings", "bluetooth setup", "pair device"

#### Public API

```kotlin
class BluetoothHandler(private val service: VoiceOSService) : ActionHandler {
    private var bluetoothAdapter: BluetoothAdapter?

    override fun initialize()
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun enableBluetooth(enable: Boolean): Boolean
    private fun toggleBluetooth(): Boolean
    private fun openBluetoothSettings(): Boolean
    override fun dispose()
}
```

#### Example Usage

```kotlin
// Voice: "bluetooth on"
bluetoothHandler.execute(ActionCategory.DEVICE, "bluetooth on", emptyMap())
// API < 33: Calls bluetoothAdapter.enable()
// API >= 33: Opens Bluetooth settings

// Voice: "pair device"
bluetoothHandler.execute(ActionCategory.DEVICE, "pair device", emptyMap())
// Result: Opens Bluetooth settings for pairing
```

#### Permission Handling

**Android 12+ (API 31+):**
- Requires `BLUETOOTH_CONNECT` permission
- Checks permission before adapter operations
- Fallback: Opens Bluetooth settings if permission denied

**Android 13+ (API 33+):**
- Direct enable/disable deprecated
- Always opens Bluetooth settings
- Maintains user control and privacy

#### Fallback Strategy

1. Try direct Bluetooth control (if API < 33 and permissions granted)
2. If SecurityException or API >= 33: Open Bluetooth settings
3. If Bluetooth settings fail: Open general settings as final fallback

---

### 4.4 AppHandler

**File:** `AppHandler.kt`
**Category:** `APP`
**Purpose:** Application launching and management

#### Supported Commands

Dynamic commands based on `service.getAppCommands()`:
- "open [app name]"
- "launch [app name]"
- App-specific command mappings

#### Public API

```kotlin
class AppHandler(private val service: VoiceOSService) : ActionHandler {
    private val packageManager: PackageManager

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    override fun canHandle(action: String): Boolean
    override fun getSupportedActions(): List<String>

    private fun launchApp(packageName: String?): Boolean
}
```

#### Example Usage

```kotlin
// Assuming app command mapping: "open chrome" -> "com.android.chrome"

// Voice: "open chrome"
appHandler.execute(ActionCategory.APP, "open chrome", emptyMap())
// Result: Launches Chrome browser

// Voice: "launch settings"
appHandler.execute(ActionCategory.APP, "launch settings", emptyMap())
// Result: Launches Android Settings app
```

#### Implementation Details

**Command Resolution:**
1. Normalize command to lowercase and trim
2. Look up package name in app commands map
3. Get launch intent from PackageManager
4. Launch with `FLAG_ACTIVITY_NEW_TASK`

**Error Handling:**
- App not found: Returns false, logs warning
- No launch intent: Returns false (app may not be launchable)
- Launch exception: Catches and logs, returns false

**App Commands Source:**
- Provided by VoiceOSService via `getAppCommands()`
- Maps voice commands to package names
- Extensible for custom app mappings

---

### 4.5 NavigationHandler

**File:** `NavigationHandler.kt`
**Category:** `NAVIGATION`
**Purpose:** UI navigation and scrolling

#### Supported Commands

**Scrolling:**
- "scroll up", "page up" - Scroll backward (content moves down)
- "scroll down", "page down" - Scroll forward (content moves up)
- "scroll left" - Horizontal scroll backward
- "scroll right" - Horizontal scroll forward

**Swipe Gestures:**
- "swipe up" - Swipe up (scroll down - content moves up)
- "swipe down" - Swipe down (scroll up - content moves down)
- "swipe left" - Swipe left (scroll right)
- "swipe right" - Swipe right (scroll left)

**Navigation:**
- "next" - Move to next item
- "previous" - Move to previous item

#### Public API

```kotlin
class NavigationHandler(private val service: VoiceOSService) : ActionHandler {
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun performScrollAction(
        rootNode: AccessibilityNodeInfo,
        action: Int,
        horizontal: Boolean = false
    ): Boolean

    private fun findScrollableNode(
        node: AccessibilityNodeInfo,
        horizontal: Boolean
    ): AccessibilityNodeInfo?
}
```

#### Example Usage

```kotlin
// Voice: "scroll down"
navigationHandler.execute(ActionCategory.NAVIGATION, "scroll down", emptyMap())
// Result: Finds scrollable node and performs ACTION_SCROLL_FORWARD

// Voice: "page up"
navigationHandler.execute(ActionCategory.NAVIGATION, "page up", emptyMap())
// Result: Performs ACTION_SCROLL_BACKWARD on scrollable container
```

#### Scrolling Algorithm

1. **Find Scrollable Node:**
   - Start at root node
   - Recursively search for `isScrollable` nodes
   - Return first scrollable node found

2. **Perform Action:**
   - `ACTION_SCROLL_FORWARD`: Scroll down/right
   - `ACTION_SCROLL_BACKWARD`: Scroll up/left

3. **Directionality:**
   - Vertical scrolling (default): Up/Down
   - Horizontal scrolling: Left/Right

#### Scroll vs Swipe Semantics

- **Scroll**: Content-centric (scroll down = content moves up)
- **Swipe**: Gesture-centric (swipe up = content moves up)
- Handler normalizes to accessibility actions

---

### 4.6 InputHandler

**File:** `InputHandler.kt`
**Category:** `INPUT`
**Purpose:** Text input and keyboard control

#### Supported Commands

**Text Input:**
- "type [text]", "enter text [text]", "input [text]" - Enter text

**Deletion:**
- "delete", "backspace" - Delete last character
- "clear text", "clear all" - Clear all text

**Selection:**
- "select all" - Select all text in field

**Clipboard:**
- "copy" - Copy selected text
- "cut" - Cut selected text
- "paste" - Paste from clipboard

**Undo/Redo:**
- "undo" - Undo last action (not yet implemented)
- "redo" - Redo last action (not yet implemented)

**Search:**
- "search [query]", "find [query]" - Enter search query

#### Public API

```kotlin
class InputHandler(private val service: VoiceOSService) : ActionHandler {
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun enterText(text: String): Boolean
    private fun performDelete(): Boolean
    private fun clearText(): Boolean
    private fun selectAll(): Boolean
    private fun performCopy(): Boolean
    private fun performCut(): Boolean
    private fun performPaste(): Boolean
    private fun performSearch(query: String): Boolean
    private fun findFocusedNode(): AccessibilityNodeInfo?
    private fun findSearchField(node: AccessibilityNodeInfo): AccessibilityNodeInfo?
}
```

#### Example Usage

```kotlin
// Voice: "type hello world"
inputHandler.execute(ActionCategory.INPUT, "type hello world", emptyMap())
// Result: Inserts "hello world" into focused editable field

// Voice: "select all"
inputHandler.execute(ActionCategory.INPUT, "select all", emptyMap())
// Result: Selects all text in current field (sets selection 0 to text.length)

// Voice: "copy"
inputHandler.execute(ActionCategory.INPUT, "copy", emptyMap())
// Result: Copies selected text to clipboard
```

#### Text Entry Strategy

**Primary Method:**
```kotlin
val arguments = Bundle().apply {
    putCharSequence(
        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
        text
    )
}
node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
```

**Fallback for Append:**
- Get current text
- Concatenate new text
- Set combined text

#### Focus Finding

1. **Input Focus**: `node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)`
2. **Accessibility Focus**: `node.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)`
3. Returns first available focus

#### Search Field Discovery

Searches for editable nodes with:
- Content description containing "search"
- Text containing "search"
- Hint text containing "search"

Recursively searches node tree.

---

### 4.7 UIHandler

**File:** `UIHandler.kt`
**Category:** `UI`
**Purpose:** UI element interaction

#### Supported Commands

**Click Actions:**
- "click [element]", "tap [element]", "press [element]" - Single click
- "long click [element]", "long press [element]" - Long press
- "double tap [element]", "double click [element]" - Double click

**Expand/Collapse:**
- "expand [element]" - Expand expandable element
- "collapse [element]" - Collapse expandable element

**Check/Toggle:**
- "check [element]" - Check checkbox/switch
- "uncheck [element]" - Uncheck checkbox/switch
- "toggle [element]" - Toggle state

**Focus:**
- "focus [element]" - Set focus to element

**Dismiss:**
- "dismiss", "close" - Dismiss dialog/modal

#### Public API

```kotlin
class UIHandler(private val service: VoiceOSService) : ActionHandler {
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun performClick(target: String): Boolean
    private fun performLongClick(target: String): Boolean
    private fun performDoubleClick(target: String): Boolean
    private fun performExpand(target: String): Boolean
    private fun performCollapse(target: String): Boolean
    private fun performCheck(target: String, check: Boolean): Boolean
    private fun performToggle(target: String): Boolean
    private fun performFocus(target: String): Boolean
    private fun performDismiss(): Boolean

    private fun findNodeByText(text: String): AccessibilityNodeInfo?
    private fun findNodeByDescription(description: String): AccessibilityNodeInfo?
}
```

#### Example Usage

```kotlin
// Voice: "click submit"
uiHandler.execute(ActionCategory.UI, "click submit", emptyMap())
// Result: Finds node with text "submit" and performs ACTION_CLICK

// Voice: "check notifications"
uiHandler.execute(ActionCategory.UI, "check notifications", emptyMap())
// Result: Checks checkbox labeled "notifications" if unchecked

// Voice: "expand more options"
uiHandler.execute(ActionCategory.UI, "expand more options", emptyMap())
// Result: Performs ACTION_EXPAND on element with text "more options"
```

#### Element Finding Strategy

**1. Find by Text:**
- Recursively traverse UI tree
- Compare node text (case-insensitive)
- Match if node text contains search text

**2. Find by Content Description:**
- Recursively traverse UI tree
- Compare node content description (case-insensitive)
- Match if description contains search text

**3. Fallback Order:**
- Try text match first
- If not found, try content description match
- Return null if neither succeeds

#### Check/Uncheck Logic

```kotlin
private fun performCheck(target: String, check: Boolean): Boolean {
    val node = findNodeByText(target) ?: findNodeByDescription(target)
    if (node == null) return false

    // Check current state
    val isChecked = node.isChecked
    if (isChecked == check) return true // Already in desired state

    // Toggle to desired state
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
}
```

#### Dismiss Strategy

1. Search for dismiss/close/cancel/ok buttons by text
2. Search for dismiss/close buttons by content description
3. Fallback: Perform global back action

---

### 4.8 SelectHandler

**File:** `SelectHandler.kt`
**Category:** `UI`
**Purpose:** Selection mode and cursor-based interactions

#### Supported Commands

**Selection Mode:**
- "select mode", "selection mode" - Enter selection mode
- "back", "cancel selection" - Exit selection mode

**Select Actions:**
- "select" - Context-dependent select (cursor or standard)
- "context menu", "menu", "action menu" - Show context menu

**Text Selection:**
- "select all" - Select all text in field
- "select text [text]" - Select specific text
- "clear selection" - Clear current selection

**Clipboard (with Selection Context):**
- "copy" - Copy selected text
- "cut" - Cut selected text
- "paste" - Paste from clipboard

#### Public API

```kotlin
class SelectHandler(private val service: VoiceOSService) : ActionHandler {
    private var isSelectionMode: Boolean
    private var currentSelection: SelectionContext?

    data class SelectionContext(
        val node: AccessibilityNodeInfo?,
        val bounds: Rect?,
        val selectionStart: Int = -1,
        val selectionEnd: Int = -1,
        val isTextSelection: Boolean = false
    )

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun enterSelectionMode(): Boolean
    private fun handleSelectAction(): Boolean
    private fun showContextMenuAtCursor(): Boolean
    private fun performSelectAtCurrentPosition(): Boolean
    private fun handleBackAction(): Boolean
    private fun selectAll(): Boolean
    private fun selectText(text: String?): Boolean
    private fun clearSelection(): Boolean
    private fun performClipboardAction(action: Int): Boolean

    fun isInSelectionMode(): Boolean
    fun getCurrentSelection(): SelectionContext?
}
```

#### Example Usage

```kotlin
// Voice: "selection mode"
selectHandler.execute(ActionCategory.UI, "selection mode", emptyMap())
// Result: Enters selection mode, shows indicator

// Voice: "select"
// (with cursor visible)
selectHandler.execute(ActionCategory.UI, "select", emptyMap())
// Result: Shows context menu at cursor position

// Voice: "select text hello"
selectHandler.execute(ActionCategory.UI, "select text hello", mapOf("text" to "hello"))
// Result: Finds and selects "hello" in current text field

// Voice: "copy"
selectHandler.execute(ActionCategory.UI, "copy", emptyMap())
// Result: Copies current selection to clipboard
```

#### Selection Context Tracking

**SelectionContext Data:**
- `node`: The AccessibilityNodeInfo being selected
- `bounds`: Screen bounds of the selection
- `selectionStart`: Text selection start index
- `selectionEnd`: Text selection end index
- `isTextSelection`: Whether this is a text selection vs UI element

**Context Updates:**
- Updated when entering selection mode
- Updated when performing selection actions
- Cleared when exiting selection mode

#### Context-Dependent Select

**With Cursor Visible:**
```kotlin
// Shows context menu at cursor position
if (isCursorVisible()) {
    showContextMenuAtCursor()
}
```

**Without Cursor:**
```kotlin
// Standard selection behavior
// Finds focused node and performs appropriate action
performSelectAtCurrentPosition()
```

#### Text Selection Implementation

**Select All:**
```kotlin
val bundle = Bundle().apply {
    putInt(ACTION_ARGUMENT_SELECTION_START_INT, 0)
    putInt(ACTION_ARGUMENT_SELECTION_END_INT, text.length)
}
node.performAction(ACTION_SET_SELECTION, bundle)
```

**Select Text:**
1. Find editable node
2. Get current text
3. Search for text substring
4. Set selection to found range
5. Update SelectionContext

---

### 4.9 NumberHandler

**File:** `NumberHandler.kt`
**Category:** `UI`
**Purpose:** Number overlay display and interaction

#### Supported Commands

**Overlay Control:**
- "show numbers", "numbers on", "label elements", "number overlay" - Show overlay
- "hide numbers", "numbers off" - Hide overlay
- "toggle numbers" - Toggle overlay visibility

**Number Commands:**
- "click number [N]", "tap [N]", "select number [N]" - Click numbered element
- "scroll number [N]" - Scroll numbered element
- "long [N]" - Long press numbered element

#### Public API

```kotlin
class NumberHandler(private val service: VoiceOSService) : ActionHandler {
    private var isNumberOverlayVisible: Boolean
    private var numberedElements: MutableMap<Int, ElementInfo>
    private var currentNumberCount: Int

    data class ElementInfo(
        val node: AccessibilityNodeInfo,
        val bounds: Rect,
        val description: String,
        val isClickable: Boolean,
        val isScrollable: Boolean
    )

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun showNumberOverlay(): Boolean
    private fun hideNumberOverlay(): Boolean
    private fun handleNumberCommand(action: String): Boolean
    private fun findInteractiveElements(rootNode: AccessibilityNodeInfo): List<ElementInfo>
    private fun isInteractiveElement(node: AccessibilityNodeInfo): Boolean
    private fun clickElement(elementInfo: ElementInfo): Boolean

    fun getNumberedElements(): Map<Int, ElementInfo>
    fun isNumberOverlayActive(): Boolean
}
```

#### Example Usage

```kotlin
// Voice: "show numbers"
numberHandler.execute(ActionCategory.UI, "show numbers", emptyMap())
// Result: Scans screen, numbers interactive elements, shows overlay

// Voice: "tap 5"
numberHandler.execute(ActionCategory.UI, "tap 5", emptyMap())
// Result: Clicks element numbered 5, hides overlay after 1 second

// Voice: "hide numbers"
numberHandler.execute(ActionCategory.UI, "hide numbers", emptyMap())
// Result: Clears numbered elements, hides overlay
```

#### Interactive Element Detection

**Criteria:**
```kotlin
private fun isInteractiveElement(node: AccessibilityNodeInfo): Boolean {
    return node.isClickable ||
           node.isScrollable ||
           node.isCheckable ||
           node.isEditable ||
           node.isFocusable ||
           node.actionList.any {
               it.id == ACTION_CLICK ||
               it.id == ACTION_LONG_CLICK ||
               it.id == ACTION_SCROLL_FORWARD ||
               it.id == ACTION_SCROLL_BACKWARD
           }
}
```

**Filtering:**
- Minimum size: 20x20 pixels
- On-screen only (left >= 0, top >= 0)
- Ignores duplicate/overlapping elements

**Sorting:**
- Primary: Top to bottom (`bounds.top`)
- Secondary: Left to right (`bounds.left`)

#### Number Overlay Lifecycle

1. **Show Overlay:**
   - Find interactive elements
   - Assign numbers sequentially
   - Display numbered overlays (TODO: integrate with overlay system)
   - Show user feedback
   - Auto-hide after 30 seconds

2. **Handle Number Command:**
   - Extract number from voice command
   - Look up ElementInfo
   - Perform appropriate action (click, scroll, long press)
   - Hide overlay after 1 second

3. **Hide Overlay:**
   - Clear numbered elements map
   - Hide overlay visualization
   - Reset number counter

#### Element Description

**Priority:**
1. Content description (if not empty)
2. Text content (if not empty)
3. Class name (simplified, e.g., "Button" from "android.widget.Button")
4. Default: "Element"

#### Number Command Parsing

```kotlin
private fun extractNumberFromCommand(action: String): Int? {
    val words = action.lowercase().split(" ")
    return words.firstNotNullOfOrNull { it.toIntOrNull() }
}

private fun isNumberCommand(action: String): Boolean {
    val words = action.lowercase().split(" ")
    return words.any { word ->
        word.toIntOrNull() != null &&
        (words.contains("tap") || words.contains("click") ||
         words.contains("select") || words.contains("scroll") ||
         words.contains("long"))
    }
}
```

---

### 4.10 GestureHandler

**File:** `GestureHandler.kt`
**Category:** `GESTURE`
**Purpose:** Complex gesture interactions (pinch, zoom, paths)

#### Supported Commands

**Pinch/Zoom:**
- "pinch open", "zoom in", "pinch in" - Pinch open (zoom in)
- "pinch close", "zoom out", "pinch out" - Pinch close (zoom out)

**Drag:**
- "drag" - Drag gesture with start/end coordinates

**Swipe:**
- "swipe", "swipe up", "swipe down", "swipe left", "swipe right"

**Custom Path:**
- "gesture", "path gesture" - Custom path gesture

#### Public API

```kotlin
class GestureHandler(private val service: VoiceOSService) : ActionHandler {
    companion object {
        const val PINCH_DURATION_MS: Int = 400
        const val PINCH_DISTANCE_CLOSE = 200
        const val PINCH_DISTANCE_FAR = 800
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun performPinchOpen(x: Int, y: Int): Boolean
    private fun performPinchClose(x: Int, y: Int): Boolean
    private fun performDrag(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean
    private fun performSwipe(direction: String, centerX: Int, centerY: Int, distance: Int): Boolean
    private fun performPathGesture(pathPoints: List<Point>, duration: Long): Boolean
    private fun pinchGesture(x: Int, y: Int, startSpacing: Int, endSpacing: Int)

    fun performClickAt(x: Float, y: Float): Boolean
    fun performLongPressAt(x: Float, y: Float): Boolean
    fun performDoubleClickAt(x: Float, y: Float): Boolean
}
```

#### Example Usage

```kotlin
// Voice: "pinch open"
gestureHandler.execute(ActionCategory.GESTURE, "pinch open", mapOf(
    "x" to screenCenterX,
    "y" to screenCenterY
))
// Result: Two-finger pinch open gesture at screen center

// Voice: "swipe left"
gestureHandler.execute(ActionCategory.GESTURE, "swipe left", mapOf(
    "x" to screenCenterX,
    "y" to screenCenterY,
    "distance" to 400
))
// Result: Swipe left gesture (400 pixels)

// Custom path gesture
val pathPoints = listOf(
    Point(100, 100),
    Point(200, 150),
    Point(300, 100)
)
gestureHandler.execute(ActionCategory.GESTURE, "path gesture", mapOf(
    "path" to pathPoints,
    "duration" to 500L
))
// Result: Custom path gesture through specified points
```

#### Pinch Gesture Implementation

**Two-Finger Simulation:**
```kotlin
private fun pinchGesture(x: Int, y: Int, startSpacing: Int, endSpacing: Int) {
    // First finger
    val x1 = x - startSpacing / 2  // Start position
    val y1 = y - startSpacing / 2
    val x2 = x - endSpacing / 2    // End position
    val y2 = y - endSpacing / 2

    val path1 = Path()
    path1.moveTo(x1.toFloat(), y1.toFloat())
    path1.lineTo(x2.toFloat(), y2.toFloat())
    val stroke1 = StrokeDescription(path1, 0, PINCH_DURATION_MS.toLong(), false)

    // Second finger (opposite side)
    val x3 = x + startSpacing / 2
    val y3 = y + startSpacing / 2
    val x4 = x + endSpacing / 2
    val y4 = y + endSpacing / 2

    val path2 = Path()
    path2.moveTo(x3.toFloat(), y3.toFloat())
    path2.lineTo(x4.toFloat(), y4.toFloat())
    val stroke2 = StrokeDescription(path2, 0, PINCH_DURATION_MS.toLong(), false)

    // Combine into gesture
    val gesture = GestureDescription.Builder()
        .addStroke(stroke1)
        .addStroke(stroke2)
        .build()

    dispatchGesture(gesture)
}
```

**Pinch Open (Zoom In):**
- Start spacing: 200 pixels (close together)
- End spacing: 800 pixels (far apart)
- Duration: 400ms

**Pinch Close (Zoom Out):**
- Start spacing: 800 pixels (far apart)
- End spacing: 200 pixels (close together)
- Duration: 400ms

#### Drag Gesture

```kotlin
private fun performDrag(
    startX: Int, startY: Int,
    endX: Int, endY: Int,
    duration: Long
): Boolean {
    val path = Path()
    path.moveTo(startX.toFloat(), startY.toFloat())
    path.lineTo(endX.toFloat(), endY.toFloat())

    val stroke = StrokeDescription(path, 0, duration)
    val gesture = GestureDescription.Builder()
        .addStroke(stroke)
        .build()

    return dispatchGesture(gesture)
}
```

#### Swipe Gesture

**Direction Mapping:**
- "up": centerX to centerX, centerY to centerY - distance
- "down": centerX to centerX, centerY to centerY + distance
- "left": centerX to centerX - distance, centerY to centerY
- "right": centerX to centerX + distance, centerY to centerY
- "" (default): Right

**Default Parameters:**
- Distance: 400 pixels
- Duration: 300ms

#### Path Gesture

**Custom Path:**
```kotlin
private fun performPathGesture(pathPoints: List<Point>, duration: Long): Boolean {
    val path = Path()
    val firstPoint = pathPoints[0]
    path.moveTo(firstPoint.x.toFloat(), firstPoint.y.toFloat())

    // Add remaining points
    for (i in 1 until pathPoints.size) {
        val point = pathPoints[i]
        path.lineTo(point.x.toFloat(), point.y.toFloat())
    }

    val stroke = StrokeDescription(path, 0, duration)
    val gesture = GestureDescription.Builder()
        .addStroke(stroke)
        .build()

    return dispatchGesture(gesture)
}
```

#### Coordinate-Based Gestures

**Public Helpers:**
```kotlin
// Single click at coordinates
fun performClickAt(x: Float, y: Float): Boolean

// Long press at coordinates
fun performLongPressAt(x: Float, y: Float): Boolean

// Double click at coordinates (with delay)
fun performDoubleClickAt(x: Float, y: Float): Boolean
```

Used by other handlers for precise positioning.

#### Gesture Queue Management

**Queue Processing:**
```kotlin
private val gestureQueue = LinkedList<GestureDescription>()

private fun dispatchGestureHandler() {
    if (gestureQueue.isEmpty()) return

    val gesture = gestureQueue[0]
    service.dispatchGesture(gesture, gestureResultCallback, null)
}

private val gestureResultCallback = object : GestureResultCallback() {
    override fun onCompleted(gestureDescription: GestureDescription) {
        synchronized(lock) {
            if (gestureQueue.isNotEmpty()) {
                gestureQueue.removeAt(0)
                if (gestureQueue.isNotEmpty()) {
                    dispatchGestureHandler()
                }
            }
        }
    }

    override fun onCancelled(gestureDescription: GestureDescription) {
        synchronized(lock) {
            gestureQueue.clear()
        }
    }
}
```

Ensures gestures execute sequentially without overlapping.

---

### 4.11 DragHandler

**File:** `DragHandler.kt`
**Category:** `GESTURE`
**Purpose:** Drag gesture tracking with cursor integration

#### Supported Commands

- "drag start", "start drag", "cursor drag" - Start drag mode
- "drag stop", "stop drag" - Stop drag mode
- "continuous drag" - Start drag immediately at current cursor position
- "drag handle", "drag up down" - Dynamic drag commands

#### Public API

```kotlin
class DragHandler(private val service: VoiceOSService) : ActionHandler {
    companion object {
        private const val DRAG_POLLING_DELAY_MS = 100L
        private const val MOVEMENT_THRESHOLD_PIXELS = 5
    }

    enum class DragEvent {
        START, MOVE, STOP, NONE
    }

    val dragPositionFlow: StateFlow<Triple<Int, Int, DragEvent>>

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun startDrag(): Boolean
    private fun stopDrag(): Boolean
    private fun startContinuousDrag(): Boolean
    private fun startCursorTracking()
    private fun checkCursorMovement(currX: Int, currY: Int, stopDragging: Boolean = false)
    private fun mouseDown(x: Int, y: Int)
    private fun mouseMove(x: Int, y: Int)
    private fun mouseUp(x: Int, y: Int)
    private fun buildGesture(x1: Int, y1: Int, x2: Int, y2: Int, isContinuedGesture: Boolean, willContinue: Boolean): GestureDescription

    fun isDragActive(): Boolean
}
```

#### Example Usage

```kotlin
// Voice: "start drag"
dragHandler.execute(ActionCategory.GESTURE, "start drag", emptyMap())
// Result: Enters drag mode, tracks cursor movements

// (User moves cursor with head tracking)
// DragHandler automatically tracks movement and generates drag gestures

// Voice: "stop drag"
dragHandler.execute(ActionCategory.GESTURE, "stop drag", emptyMap())
// Result: Completes drag operation, exits drag mode

// Voice: "continuous drag"
dragHandler.execute(ActionCategory.GESTURE, "continuous drag", emptyMap())
// Result: Immediately starts drag from current cursor position
```

#### Drag Mode Lifecycle

**1. Start Drag:**
```kotlin
private fun startDrag(): Boolean {
    // Verify cursor is visible
    if (!service.isCursorVisible()) return false

    // Clean up any existing drag
    stopDrag()

    // Reset state
    isInitialValue = true
    prevX = 0
    prevY = 0

    // Start drag tracking job
    dragJob = dragScope.launch {
        dragPositionFlow.collectLatest { result ->
            when (result.third) {
                DragEvent.START -> mouseDown(result.first, result.second)
                DragEvent.MOVE -> mouseMove(result.first, result.second)
                DragEvent.STOP -> mouseUp(result.first, result.second)
                else -> {}
            }
        }
    }

    // Start cursor movement tracking
    startCursorTracking()

    return true
}
```

**2. Cursor Tracking:**
```kotlin
private fun startCursorTracking() {
    dragScope.launch {
        while (dragJob?.isActive == true) {
            val offset = service.getCursorPosition()
            checkCursorMovement(offset.x.toInt(), offset.y.toInt())
            delay(DRAG_POLLING_DELAY_MS)
        }
    }
}
```

**3. Movement Detection:**
```kotlin
private fun checkCursorMovement(currX: Int, currY: Int, stopDragging: Boolean = false) {
    // Skip if no significant movement
    if (abs(currX - prevX) < MOVEMENT_THRESHOLD_PIXELS &&
        abs(currY - prevY) < MOVEMENT_THRESHOLD_PIXELS) {
        return
    }

    // Initialize previous position
    if (prevX == 0) prevX = currX
    if (prevY == 0) prevY = currY

    // Calculate deltas
    val deltaX = currX - prevX
    val deltaY = currY - prevY

    // Emit appropriate drag event
    if (abs(deltaY) > abs(deltaX) || abs(deltaX) > abs(deltaY)) {
        if (isInitialValue) {
            isInitialValue = false
            _dragPositionFlow.value = Triple(currX, currY, DragEvent.START)
        } else {
            _dragPositionFlow.value = Triple(
                currX, currY,
                if (!stopDragging) DragEvent.MOVE else DragEvent.STOP
            )
        }
    }

    // Update previous position
    prevX = currX
    prevY = currY
}
```

**4. Stop Drag:**
```kotlin
private fun stopDrag(): Boolean {
    // Cancel tracking job
    dragJob?.cancel()
    dragJob = null

    // Complete ongoing drag
    if (isMouseDown && service.isCursorVisible()) {
        val offset = service.getCursorPosition()
        mouseUp(offset.x.toInt(), offset.y.toInt())
    }

    return true
}
```

#### Continuous Stroke Gestures

**Mouse Down (Start Stroke):**
```kotlin
private fun mouseDown(x: Int, y: Int) {
    synchronized(lock) {
        val gesture = buildGesture(
            x, y, x, y,
            isContinuedGesture = false,
            willContinue = true
        )
        gestureList.add(gesture)
        if (gestureList.size == 1) dispatchGestureHandler()
        prevX = x
        prevY = y
        isMouseDown = true
    }
}
```

**Mouse Move (Continue Stroke):**
```kotlin
private fun mouseMove(x: Int, y: Int) {
    synchronized(lock) {
        if (!isMouseDown) return
        if (prevX == x && prevY == y) return

        val gesture = buildGesture(
            prevX, prevY, x, y,
            isContinuedGesture = true,
            willContinue = true
        )
        gestureList.add(gesture)
        if (gestureList.size == 1) dispatchGestureHandler()
        prevX = x
        prevY = y
    }
}
```

**Mouse Up (End Stroke):**
```kotlin
private fun mouseUp(x: Int, y: Int) {
    if (isMouseDown) {
        synchronized(lock) {
            val gesture = buildGesture(
                prevX, prevY, x, y,
                isContinuedGesture = true,
                willContinue = false
            )
            gestureList.add(gesture)
            if (gestureList.size == 1) dispatchGestureHandler()
            isMouseDown = false
        }
    }
}
```

#### Gesture Building

**Continued Stroke:**
```kotlin
private fun buildGesture(
    x1: Int, y1: Int, x2: Int, y2: Int,
    isContinuedGesture: Boolean,
    willContinue: Boolean
): GestureDescription {
    // Ensure positive coordinates
    val x1Positive = max(0f, x1.toFloat())
    val y1Positive = max(0f, y1.toFloat())
    val x2Positive = max(0f, x2.toFloat())
    val y2Positive = max(0f, y2.toFloat())

    // Build path
    val path = Path()
    path.moveTo(x1Positive, y1Positive)
    if (x1Positive != x2Positive || y1Positive != y2Positive) {
        path.lineTo(x2Positive, y2Positive)
    }

    // Create stroke description
    val stroke: StrokeDescription? = if (!isContinuedGesture) {
        // New stroke
        StrokeDescription(path, 0, 1, willContinue)
    } else {
        // Continue previous stroke
        currentStroke?.continueStroke(path, 0, 1, willContinue)
    }

    // Build gesture
    val builder = GestureDescription.Builder()
    if (stroke != null) {
        builder.addStroke(stroke)
    }
    val gestureDescription = builder.build()
    currentStroke = stroke

    return gestureDescription
}
```

Key: `currentStroke?.continueStroke()` creates continuous gesture.

#### Integration Points

- **Cursor Position**: `service.getCursorPosition()`
- **Cursor Visibility**: `service.isCursorVisible()`
- **Gesture Dispatch**: `service.dispatchGesture()`

Requires cursor system for tracking.

---

### 4.12 HelpMenuHandler

**File:** `HelpMenuHandler.kt`
**Category:** `UI` (or `SYSTEM`)
**Purpose:** Help system and command discovery

#### Supported Commands

**Help Menu:**
- "show help", "help menu", "help me" - Show main help menu
- "hide help", "close help" - Hide help menu

**Command List:**
- "show commands", "command list", "what can i say", "voice commands" - Show all commands
- "hide commands", "close commands" - Hide command list

**Tutorial:**
- "tutorial", "getting started" - Show getting started tutorial

**Documentation:**
- "user guide", "documentation", "help center" - Open external documentation

**Category Help:**
- "navigation help" - Show navigation commands
- "system help" - Show system commands
- "app help" - Show app commands
- "input help" - Show input commands
- "ui help" - Show UI commands
- "accessibility help" - Show accessibility commands

#### Public API

```kotlin
class HelpMenuHandler(private val service: VoiceOSService) : ActionHandler {
    companion object {
        private val HELP_CATEGORIES = mapOf(
            "navigation" to listOf(...),
            "system" to listOf(...),
            "apps" to listOf(...),
            "input" to listOf(...),
            "ui" to listOf(...),
            "accessibility" to listOf(...)
        )
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean

    private fun showHelpMenu(): Boolean
    private fun hideHelpMenu(): Boolean
    private fun showCommandList(): Boolean
    private fun hideCommandList(): Boolean
    private fun showCategoryHelp(category: String): Boolean
    private fun showTutorial(): Boolean
    private fun openDocumentation(): Boolean

    fun getAllCommands(): Map<String, List<String>>
    fun isHelpVisible(): Boolean
    fun getCurrentHelpCategory(): String?
}
```

#### Example Usage

```kotlin
// Voice: "what can i say"
helpMenuHandler.execute(ActionCategory.UI, "what can i say", emptyMap())
// Result: Shows comprehensive command list with all categories

// Voice: "navigation help"
helpMenuHandler.execute(ActionCategory.UI, "navigation help", emptyMap())
// Result: Shows navigation-specific commands

// Voice: "tutorial"
helpMenuHandler.execute(ActionCategory.UI, "tutorial", emptyMap())
// Result: Shows getting started tutorial
```

#### Help Categories

**Navigation Commands:**
- go back, go home, scroll up, scroll down, scroll left, scroll right
- page up, page down

**System Commands:**
- volume up, volume down, mute
- brightness up, brightness down
- wifi on/off, bluetooth on/off

**App Commands:**
- open [app name], launch [app name]
- close app, switch app, recent apps, app drawer

**Input Commands:**
- type [text], say [text]
- backspace, enter, space, clear text
- select all, copy, paste, cut

**UI Commands:**
- tap, click, double tap, long press
- swipe up/down/left/right
- pinch open, pinch close

**Accessibility Commands:**
- show cursor, hide cursor
- gaze on, gaze off
- select mode, show numbers, hide numbers
- help menu, what can i say

#### Help Display

**Current Implementation:**
- Uses Toast messages for help text
- Auto-hide after timeout (8-15 seconds)
- Logs help content for accessibility

**TODO:**
- Integrate with proper overlay system
- Persistent help panel option
- Interactive help navigation
- Command search functionality

#### Tutorial Content

```kotlin
private fun showTutorial(): Boolean {
    val tutorialText = """
        VOS4 Tutorial:

        1. BASICS:
        • Say commands clearly and naturally
        • Wait for beep before speaking
        • Commands work in any app

        2. GETTING AROUND:
        • "go back" - Previous screen
        • "go home" - Home screen
        • "scroll down" - Scroll content

        3. OPENING APPS:
        • "open [app name]" - Launch app
        • "recent apps" - See recent

        4. GETTING HELP:
        • "what can I say" - All commands
        • "help menu" - This tutorial

        Say 'hide help' when done
    """.trimIndent()

    showHelpToast(tutorialText)
    return true
}
```

#### External Documentation

```kotlin
private fun openDocumentation(): Boolean {
    val docUrl = "https://vos4.augmentalis.com/docs"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(docUrl)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    service.startActivity(intent)
    return true
}
```

Fallback: Show built-in help if URL cannot open.

#### Command Discovery API

```kotlin
fun getAllCommands(): Map<String, List<String>> {
    return HELP_CATEGORIES.toMap()
}
```

Used by:
- External command listing
- Voice command validation
- Autocomplete systems
- Documentation generation

---

## 5. Command Flow

### End-to-End Command Processing

```
┌─────────────────────────────────────────────────────────────────┐
│                      USER VOICE INPUT                            │
│                     "scroll down"                                │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SPEECH RECOGNITION                              │
│              (SpeechRecognition Library)                         │
│  - Audio capture                                                 │
│  - Speech-to-text conversion                                     │
│  - Returns recognized text                                       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    VOICEOSSERVICE                                │
│              (Main Accessibility Service)                        │
│  - Receives voice command text                                   │
│  - Determines command category                                   │
│  - Routes to appropriate handler                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   HANDLER REGISTRY                               │
│             (Category → Handler Mapping)                         │
│  handlerRegistry[NAVIGATION] → NavigationHandler                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  NAVIGATIONHANDLER                               │
│                                                                  │
│  1. canHandle("scroll down") → true                              │
│  2. execute(NAVIGATION, "scroll down", {})                       │
│  3. Match action: "scroll down" → ACTION_SCROLL_FORWARD          │
│  4. Find scrollable node in UI tree                              │
│  5. Perform accessibility action                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│            ANDROID ACCESSIBILITY FRAMEWORK                       │
│                                                                  │
│  - node.performAction(ACTION_SCROLL_FORWARD)                     │
│  - Forwards to target app                                        │
│  - App receives scroll event                                     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      UI UPDATE                                   │
│                                                                  │
│  - Content scrolls down                                          │
│  - User sees visual feedback                                     │
│  - Handler returns success (true)                                │
└─────────────────────────────────────────────────────────────────┘
```

### Command Routing Decision Tree

```
Voice Command → VoiceOSService
│
├─ Category Detection
│  ├─ Contains "back", "home", "settings" → SYSTEM
│  ├─ Contains "open", "launch" → APP
│  ├─ Contains "scroll", "swipe" → NAVIGATION
│  ├─ Contains "type", "enter text" → INPUT
│  ├─ Contains "click", "tap" → UI
│  ├─ Contains "pinch", "zoom" → GESTURE
│  ├─ Contains "volume", "brightness" → DEVICE
│  └─ Default → Try all handlers
│
├─ Handler Selection
│  └─ handlerRegistry[category]
│
├─ Handler Validation
│  └─ handler.canHandle(action)
│      ├─ true → Execute
│      └─ false → Try next handler
│
├─ Execution
│  └─ handler.execute(category, action, params)
│      ├─ Success → Return true
│      └─ Failure → Return false
│
└─ Feedback
   ├─ Success → Visual/audio confirmation
   └─ Failure → Error message or fallback
```

### Multi-Handler Coordination

Some commands require coordination between handlers:

**Example: Number Selection with Click**

```
"tap 5" command flow:

1. NumberHandler.canHandle("tap 5")
   - Detects number command → true

2. NumberHandler.execute(UI, "tap 5", {})
   - Extracts number: 5
   - Looks up ElementInfo for number 5
   - Gets element bounds

3. NumberHandler.clickElement(elementInfo)
   - Calls node.performAction(ACTION_CLICK)
   - OR uses GestureHandler for coordinate-based click

4. GestureHandler.performClickAt(x, y)
   - Builds gesture at element coordinates
   - Dispatches gesture

Result: Element 5 clicked, overlay hidden
```

**Example: Cursor Drag**

```
"start drag" command flow:

1. DragHandler.execute(GESTURE, "start drag", {})
   - Verifies cursor is visible
   - Starts drag tracking

2. DragHandler monitors cursor position
   - Polls service.getCursorPosition() every 100ms
   - Detects movement > 5 pixels

3. DragHandler generates drag gestures
   - mouseDown() → Start stroke
   - mouseMove() → Continue stroke
   - mouseUp() → End stroke

4. VoiceOSService.dispatchGesture()
   - Dispatches continuous gesture to Android

Result: Drag gesture follows cursor movement
```

---

## 6. Extension Guide

### Adding a New Handler

**Step 1: Create Handler Class**

```kotlin
package com.augmentalis.voiceos.accessibility.handlers

import com.augmentalis.voiceos.accessibility.VoiceOSService

class CustomHandler(
    private val service: VoiceOSService
) : ActionHandler {

    companion object {
        private const val TAG = "CustomHandler"

        val SUPPORTED_ACTIONS = listOf(
            "custom command 1",
            "custom command 2"
        )
    }

    override fun initialize() {
        // Initialize resources
    }

    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.contains(it) }
    }

    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        // Implement command execution
        return when {
            action.contains("custom command 1") -> {
                handleCommand1()
            }
            action.contains("custom command 2") -> {
                handleCommand2()
            }
            else -> false
        }
    }

    private fun handleCommand1(): Boolean {
        // Implementation
        return true
    }

    private fun handleCommand2(): Boolean {
        // Implementation
        return true
    }

    override fun dispose() {
        // Clean up resources
    }
}
```

**Step 2: Register Handler**

In `VoiceOSService.kt`:

```kotlin
class VoiceOSService : AccessibilityService() {

    private val handlerRegistry = mutableMapOf<ActionCategory, ActionHandler>()

    override fun onCreate() {
        super.onCreate()

        // Register existing handlers
        handlerRegistry[ActionCategory.SYSTEM] = SystemHandler(this)
        handlerRegistry[ActionCategory.DEVICE] = DeviceHandler(this)
        // ... other handlers ...

        // Register new handler
        handlerRegistry[ActionCategory.CUSTOM] = CustomHandler(this)

        // Initialize all handlers
        handlerRegistry.values.forEach { it.initialize() }
    }
}
```

**Step 3: Add Command Routing**

```kotlin
fun processVoiceCommand(command: String) {
    val category = determineCategory(command)
    val handler = handlerRegistry[category]

    if (handler?.canHandle(command) == true) {
        val success = handler.execute(category, command, extractParams(command))
        if (success) {
            showFeedback("Command executed successfully")
        } else {
            showFeedback("Command failed")
        }
    } else {
        showFeedback("Command not recognized")
    }
}
```

### Extending Existing Handlers

**Add New Command to Existing Handler:**

```kotlin
// In NavigationHandler.kt
companion object {
    val SUPPORTED_ACTIONS = listOf(
        "scroll up", "scroll down",
        // Add new command
        "scroll to top",
        "scroll to bottom"
    )
}

override fun execute(...): Boolean {
    return when (normalizedAction) {
        "scroll up", "page up" -> performScrollAction(rootNode, ACTION_SCROLL_BACKWARD)
        "scroll down", "page down" -> performScrollAction(rootNode, ACTION_SCROLL_FORWARD)

        // Add new command handling
        "scroll to top" -> scrollToExtreme(rootNode, toTop = true)
        "scroll to bottom" -> scrollToExtreme(rootNode, toTop = false)

        else -> false
    }
}

private fun scrollToExtreme(rootNode: AccessibilityNodeInfo, toTop: Boolean): Boolean {
    val scrollableNode = findScrollableNode(rootNode, horizontal = false) ?: return false

    // Scroll multiple times to reach extreme
    repeat(20) {
        scrollableNode.performAction(
            if (toTop) ACTION_SCROLL_BACKWARD else ACTION_SCROLL_FORWARD
        )
        Thread.sleep(50)
    }

    return true
}
```

### Custom Command Parameters

**Define Parameter Extraction:**

```kotlin
data class CommandParams(
    val text: String? = null,
    val number: Int? = null,
    val x: Int? = null,
    val y: Int? = null,
    val duration: Long? = null
)

fun extractParams(command: String): Map<String, Any> {
    val params = mutableMapOf<String, Any>()

    // Extract number
    val numberRegex = """\d+""".toRegex()
    numberRegex.find(command)?.value?.toIntOrNull()?.let {
        params["number"] = it
    }

    // Extract text after trigger word
    if (command.startsWith("type ")) {
        params["text"] = command.removePrefix("type ").trim()
    }

    // Add more extraction logic

    return params
}
```

**Use Parameters in Handler:**

```kotlin
override fun execute(
    category: ActionCategory,
    action: String,
    params: Map<String, Any>
): Boolean {
    val text = params["text"] as? String
    val number = params["number"] as? Int
    val x = params["x"] as? Int
    val y = params["y"] as? Int

    return when {
        action.startsWith("type ") && text != null -> {
            enterText(text)
        }
        action.startsWith("tap ") && number != null -> {
            tapNumber(number)
        }
        action.startsWith("click at") && x != null && y != null -> {
            clickAtCoordinates(x, y)
        }
        else -> false
    }
}
```

---

## 7. Common Patterns

### Pattern 1: Node Tree Traversal

**Recursive Search:**

```kotlin
private fun findNodeByText(
    node: AccessibilityNodeInfo,
    searchText: String
): AccessibilityNodeInfo? {
    // Check current node
    node.text?.toString()?.lowercase()?.let { nodeText ->
        if (nodeText.contains(searchText)) {
            return node
        }
    }

    // Recursively check children
    for (i in 0 until node.childCount) {
        node.getChild(i)?.let { child ->
            val result = findNodeByText(child, searchText)
            if (result != null) return result
        }
    }

    return null
}
```

**Usage:**
```kotlin
val rootNode = service.rootInActiveWindow ?: return false
val targetNode = findNodeByText(rootNode, "submit")
```

### Pattern 2: Action with Fallback

**Try Primary, Fall Back to Secondary:**

```kotlin
private fun performAction(): Boolean {
    return try {
        // Try primary method
        val success = primaryMethod()
        if (success) return true

        // Fallback to secondary method
        secondaryMethod()
    } catch (e: Exception) {
        Log.e(TAG, "Error performing action", e)
        // Final fallback
        fallbackMethod()
    }
}
```

**Example:**
```kotlin
private fun enableBluetooth(enable: Boolean): Boolean {
    return try {
        // Try direct control (API < 33)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            bluetoothAdapter?.enable()
            return true
        }

        // Fallback: Open settings
        openBluetoothSettings()
    } catch (e: SecurityException) {
        // Permission denied: Open settings
        openBluetoothSettings()
    }
}
```

### Pattern 3: Gesture Queue Management

**Sequential Gesture Execution:**

```kotlin
private val gestureQueue = LinkedList<GestureDescription>()
private val lock = AtomicBoolean(false)

fun queueGesture(gesture: GestureDescription) {
    synchronized(lock) {
        gestureQueue.add(gesture)
        if (gestureQueue.size == 1) {
            dispatchNextGesture()
        }
    }
}

private fun dispatchNextGesture() {
    if (gestureQueue.isEmpty()) return

    val gesture = gestureQueue[0]
    service.dispatchGesture(gesture, object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            synchronized(lock) {
                gestureQueue.removeAt(0)
                if (gestureQueue.isNotEmpty()) {
                    dispatchNextGesture()
                }
            }
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            synchronized(lock) {
                gestureQueue.clear()
            }
        }
    }, null)
}
```

### Pattern 4: State Management with Coroutines

**Handler State Tracking:**

```kotlin
class StateHandler(private val service: VoiceOSService) : ActionHandler {
    private var isActive = false
    private var currentState: State? = null
    private val stateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    data class State(
        val data: Any,
        val timestamp: Long = System.currentTimeMillis()
    )

    private fun enterState(newState: State) {
        currentState = newState
        isActive = true

        // Auto-exit after timeout
        stateScope.launch {
            delay(30000) // 30 seconds
            if (isActive) {
                exitState()
            }
        }
    }

    private fun exitState() {
        isActive = false
        currentState = null
    }

    override fun dispose() {
        stateScope.cancel()
        exitState()
    }
}
```

### Pattern 5: Parameter Validation

**Validate Before Execution:**

```kotlin
override fun execute(
    category: ActionCategory,
    action: String,
    params: Map<String, Any>
): Boolean {
    // Validate prerequisites
    if (!validatePrerequisites()) {
        Log.w(TAG, "Prerequisites not met")
        return false
    }

    // Validate parameters
    val validParams = validateParams(action, params)
    if (!validParams) {
        Log.w(TAG, "Invalid parameters for action: $action")
        return false
    }

    // Execute action
    return performAction(action, params)
}

private fun validatePrerequisites(): Boolean {
    return service.rootInActiveWindow != null &&
           service.isServiceConnected() &&
           hasRequiredPermissions()
}

private fun validateParams(action: String, params: Map<String, Any>): Boolean {
    return when {
        action.startsWith("drag") -> {
            params.containsKey("startX") &&
            params.containsKey("startY") &&
            params.containsKey("endX") &&
            params.containsKey("endY")
        }
        action.startsWith("type") -> {
            params.containsKey("text")
        }
        else -> true // No special validation needed
    }
}
```

### Pattern 6: User Feedback

**Consistent Feedback:**

```kotlin
private fun showFeedback(message: String, duration: Int = Toast.LENGTH_SHORT) {
    try {
        Toast.makeText(service, message, duration).show()
        Log.i(TAG, "Feedback: $message")

        // Optional: TTS feedback
        // service.speak(message)
    } catch (e: Exception) {
        Log.e(TAG, "Error showing feedback", e)
    }
}

override fun execute(...): Boolean {
    val success = performAction()

    if (success) {
        showFeedback("Action completed successfully")
    } else {
        showFeedback("Action failed", Toast.LENGTH_LONG)
    }

    return success
}
```

### Pattern 7: Permission Checking

**Runtime Permission Check:**

```kotlin
private fun hasRequiredPermissions(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Android 12+: Check BLUETOOTH_CONNECT
            ContextCompat.checkSelfPermission(
                service,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        else -> {
            // Earlier versions: Check BLUETOOTH
            ContextCompat.checkSelfPermission(
                service,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

private fun executeWithPermissionCheck(): Boolean {
    if (!hasRequiredPermissions()) {
        Log.w(TAG, "Required permissions not granted")
        // Fallback action or inform user
        return false
    }

    // Execute action
    return performAction()
}
```

---

## 8. Error Handling

### Error Categories

**1. Prerequisite Failures:**
- No root node available
- Service not connected
- Permissions not granted
- Required feature not supported

**2. Node Not Found:**
- Element not present in UI tree
- Element moved or destroyed
- Invalid search criteria

**3. Action Failures:**
- Action not supported by node
- Action rejected by system
- Timing issues (too fast/slow)

**4. System Errors:**
- SecurityException
- OutOfMemoryError
- RemoteException

### Error Handling Strategies

**Strategy 1: Graceful Degradation**

```kotlin
override fun execute(...): Boolean {
    return try {
        // Try preferred method
        val success = performPreferredAction()
        if (success) return true

        // Try alternative method
        val alternativeSuccess = performAlternativeAction()
        if (alternativeSuccess) {
            Log.i(TAG, "Used alternative method")
            return true
        }

        // Final fallback
        Log.w(TAG, "All methods failed, using fallback")
        performFallbackAction()
    } catch (e: Exception) {
        Log.e(TAG, "Error executing action", e)
        false
    }
}
```

**Strategy 2: Retry with Backoff**

```kotlin
private suspend fun performWithRetry(
    maxAttempts: Int = 3,
    delayMs: Long = 500,
    action: () -> Boolean
): Boolean {
    repeat(maxAttempts) { attempt ->
        try {
            if (action()) {
                return true
            }

            if (attempt < maxAttempts - 1) {
                Log.d(TAG, "Retry attempt ${attempt + 1} after ${delayMs}ms")
                delay(delayMs * (attempt + 1)) // Exponential backoff
            }
        } catch (e: Exception) {
            Log.e(TAG, "Attempt ${attempt + 1} failed", e)
            if (attempt == maxAttempts - 1) {
                throw e
            }
        }
    }
    return false
}
```

**Strategy 3: Validation Before Action**

```kotlin
private fun performActionSafely(node: AccessibilityNodeInfo, action: Int): Boolean {
    // Validate node
    if (!isNodeValid(node)) {
        Log.w(TAG, "Invalid node, cannot perform action")
        return false
    }

    // Validate action support
    if (!supportsAction(node, action)) {
        Log.w(TAG, "Node does not support action: $action")
        return false
    }

    // Perform action with exception handling
    return try {
        node.performAction(action)
    } catch (e: Exception) {
        Log.e(TAG, "Error performing action", e)
        false
    }
}

private fun isNodeValid(node: AccessibilityNodeInfo?): Boolean {
    if (node == null) return false

    return try {
        // Try to access a property to verify node is not recycled
        node.className
        true
    } catch (e: IllegalStateException) {
        false
    }
}

private fun supportsAction(node: AccessibilityNodeInfo, action: Int): Boolean {
    return node.actionList.any { it.id == action }
}
```

**Strategy 4: Timeout Protection**

```kotlin
private suspend fun executeWithTimeout(
    timeoutMs: Long = 5000,
    action: suspend () -> Boolean
): Boolean {
    return withTimeout(timeoutMs) {
        try {
            action()
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Action timed out after ${timeoutMs}ms")
            false
        }
    }
}
```

### Error Recovery

**Recovery from Node Invalidation:**

```kotlin
private var cachedNode: AccessibilityNodeInfo? = null

private fun getValidNode(): AccessibilityNodeInfo? {
    // Check if cached node is still valid
    if (cachedNode != null && isNodeValid(cachedNode)) {
        return cachedNode
    }

    // Refresh node
    val rootNode = service.rootInActiveWindow ?: return null
    cachedNode = findTargetNode(rootNode)

    return cachedNode
}
```

**Recovery from Permission Denial:**

```kotlin
private fun executeWithPermissionRecovery(): Boolean {
    return try {
        performAction()
    } catch (e: SecurityException) {
        Log.e(TAG, "Permission denied", e)

        // Inform user
        showFeedback("Permission required. Opening settings...")

        // Navigate to settings
        openPermissionSettings()

        true // Action handled by opening settings
    }
}
```

**Recovery from Service Disconnection:**

```kotlin
private fun executeWithServiceCheck(): Boolean {
    if (!service.isServiceConnected()) {
        Log.w(TAG, "Service not connected, attempting reconnection")

        // Attempt to reinitialize
        initialize()

        // Retry after short delay
        Thread.sleep(500)

        if (!service.isServiceConnected()) {
            Log.e(TAG, "Service reconnection failed")
            return false
        }
    }

    return performAction()
}
```

### Logging Best Practices

**Structured Logging:**

```kotlin
private fun logCommandExecution(
    action: String,
    params: Map<String, Any>,
    result: Boolean,
    durationMs: Long
) {
    val status = if (result) "SUCCESS" else "FAILURE"
    val paramsStr = params.entries.joinToString(", ") { "${it.key}=${it.value}" }

    Log.i(TAG, "[$status] $action | params: [$paramsStr] | duration: ${durationMs}ms")
}

override fun execute(...): Boolean {
    val startTime = System.currentTimeMillis()

    try {
        val result = performAction(action, params)
        val duration = System.currentTimeMillis() - startTime

        logCommandExecution(action, params, result, duration)

        return result
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        Log.e(TAG, "[EXCEPTION] $action | duration: ${duration}ms", e)
        return false
    }
}
```

**Error Reporting:**

```kotlin
private fun reportError(
    action: String,
    error: Throwable,
    context: Map<String, Any> = emptyMap()
) {
    val errorReport = buildString {
        appendLine("Error in ${this@Handler::class.simpleName}")
        appendLine("Action: $action")
        appendLine("Error: ${error.javaClass.simpleName} - ${error.message}")

        if (context.isNotEmpty()) {
            appendLine("Context:")
            context.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
        }

        appendLine("Stack trace:")
        appendLine(error.stackTraceToString())
    }

    Log.e(TAG, errorReport)

    // Optional: Send to crash reporting service
    // CrashReporter.report(error, errorReport)
}
```

---

## Appendix A: Quick Reference

### Handler Capabilities Matrix

| Handler | Click | Scroll | Input | System | Gestures | State Mgmt |
|---------|-------|--------|-------|--------|----------|------------|
| SystemHandler | - | - | - | ✓ | - | - |
| DeviceHandler | - | - | - | ✓ | - | - |
| BluetoothHandler | - | - | - | ✓ | - | - |
| AppHandler | - | - | - | ✓ | - | - |
| NavigationHandler | - | ✓ | - | - | - | - |
| InputHandler | - | - | ✓ | - | - | - |
| UIHandler | ✓ | - | - | - | - | - |
| SelectHandler | ✓ | - | ✓ | - | - | ✓ |
| NumberHandler | ✓ | ✓ | - | - | - | ✓ |
| GestureHandler | ✓ | - | - | - | ✓ | - |
| DragHandler | - | - | - | - | ✓ | ✓ |
| HelpMenuHandler | - | - | - | - | - | ✓ |

### Common Commands Quick List

**System:**
- back, home, recent apps, notifications, settings

**Device:**
- volume up/down, brightness up/down, wifi/bluetooth on/off

**Navigation:**
- scroll up/down/left/right, page up/down, swipe directions

**Input:**
- type [text], delete, clear text, select all, copy, paste

**UI:**
- click/tap [element], long press [element], expand/collapse

**Gestures:**
- pinch open/close, drag start/stop, swipe [direction]

**Number:**
- show/hide numbers, tap [number]

**Help:**
- what can i say, help menu, tutorial

---

## Appendix B: Migration Notes

### Legacy Avenue to VOS4

**Major Changes:**
1. **Interface Adoption**: All handlers now implement `ActionHandler` interface
2. **Direct Implementation**: No intermediate abstraction layers
3. **Unified Naming**: Consistent naming conventions across handlers
4. **State Management**: Enhanced coroutine-based state tracking
5. **Error Handling**: Improved exception handling and fallbacks

**Migration Status:**
- SystemHandler: ✓ Fully migrated
- DeviceHandler: ✓ Fully migrated
- BluetoothHandler: ✓ Migrated with Android 12+ support
- DragHandler: ✓ Migrated with 100% functional equivalence
- GestureHandler: ✓ Migrated with queue management
- HelpMenuHandler: ✓ Migrated with enhanced categories
- NumberHandler: ✓ Migrated with improved element detection
- SelectHandler: ✓ Migrated with cursor integration

**Breaking Changes:**
- None - All functionality preserved

---

## Appendix C: Future Enhancements

### Planned Features

1. **Overlay System Integration**
   - Replace Toast messages with proper overlays
   - Persistent help panels
   - Visual feedback for command execution

2. **Context Menu System**
   - Rich context menus for selection mode
   - Action history and quick repeat
   - Custom command shortcuts

3. **Command Chaining**
   - Execute multiple commands in sequence
   - Conditional command execution
   - Command macros

4. **Advanced Gesture Recognition**
   - Custom gesture patterns
   - Multi-finger gestures
   - Gesture recording and playback

5. **Accessibility Metrics**
   - Command usage statistics
   - Performance monitoring
   - Error rate tracking

6. **Plugin System**
   - Custom handler registration
   - Third-party command extensions
   - App-specific command packs

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-10 | VOS4 Documentation Agent | Initial comprehensive documentation |

---

**End of Command Handlers Subsystem Documentation**
