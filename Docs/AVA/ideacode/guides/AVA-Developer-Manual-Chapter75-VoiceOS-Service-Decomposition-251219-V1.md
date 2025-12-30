# Chapter 75: VoiceOS Service Decomposition

**Created:** 2025-12-19
**Author:** VoiceOS Team
**Status:** IMPLEMENTED

## 75.1. Overview

The VoiceOS Accessibility Service has been decomposed from a 2365-line god object into focused, single-responsibility components following SOLID principles. This chapter documents the architecture, components, and integration patterns.

**Problem Solved:**
- VoiceOSService handled too many responsibilities (speech recognition, overlays, command routing, service lifecycle)
- Difficult to test, maintain, and extend
- Violated Single Responsibility Principle

**Solution:**
- Extracted 4 focused managers
- Added interface abstraction (IVoiceOSContext)
- Created handler registry system
- Maintained backward compatibility

## 75.2. Architecture Components

### 75.2.1. CommandDispatcher

**Path:** `accessibility/handlers/CommandDispatcher.kt`

Routes voice commands through a multi-tier execution system:

| Tier | Component | Priority | Purpose |
|------|-----------|----------|---------|
| Rename | RenameCommandHandler | HIGHEST | On-demand command renaming |
| Web | WebCommandCoordinator | HIGH | Browser-specific commands |
| Tier 1 | CommandManager | PRIMARY | Unified command system |
| Tier 2 | VoiceCommandProcessor | SECONDARY | Hash-based app commands |
| Tier 3 | ActionCoordinator | FALLBACK | Legacy handler-based commands |

**Key Features:**
- Automatic tier fallthrough on failure
- Confidence threshold filtering (< 0.5 rejected)
- Context creation for command execution
- Fallback mode for graceful degradation

**Usage:**
```kotlin
commandDispatcher = CommandDispatcher(
    context = applicationContext,
    accessibilityService = this,
    actionCoordinator = actionCoordinator,
    webCommandCoordinator = webCommandCoordinator,
    onRenameCommand = { cmd, pkg -> handleRenameCommand(cmd, pkg) }
)
commandDispatcher.processVoiceCommand("open settings", 0.95f)
```

### 75.2.2. HandlerRegistry

**Path:** `accessibility/handlers/HandlerRegistry.kt`

Manages registration and lookup of action handlers with priority-based resolution.

**Category Priority Order:**
1. SYSTEM - System commands (stop, back, home)
2. NAVIGATION - Navigation actions
3. APP - App launching
4. GAZE - Gaze interactions
5. GESTURE - Gesture interactions
6. UI - UI element interaction
7. DEVICE - Device control
8. INPUT - Text input
9. CUSTOM - Custom handlers

**Usage:**
```kotlin
val registry = HandlerRegistry()
registry.registerHandler(ActionCategory.SYSTEM, SystemHandler(context))
registry.registerHandler(ActionCategory.APP, AppHandler(context))

// Find handler for action
val handler = registry.findHandler("go back")
handler?.handle("go back")
```

### 75.2.3. VoiceRecognitionManager

**Path:** `accessibility/recognition/VoiceRecognitionManager.kt`

Manages speech recognition lifecycle with callback-based command delivery.

**Responsibilities:**
- SpeechEngineManager initialization
- Dual collectors for state + command events
- Start/stop listening control
- Configuration updates

**Usage:**
```kotlin
voiceRecognitionManager = VoiceRecognitionManager(
    speechEngineManager = speechEngineManager,
    onCommandReceived = { command, confidence ->
        commandDispatcher?.processVoiceCommand(command, confidence)
    }
)
voiceRecognitionManager?.initialize()
voiceRecognitionManager?.startListening()
```

### 75.2.4. ServiceLifecycleManager

**Path:** `accessibility/managers/ServiceLifecycleManager.kt`

Handles accessibility service lifecycle events with event filtering and queuing.

**Key Features:**
- AccessibilityServiceInfo configuration
- Event filtering (memory pressure, priority)
- Event debouncing
- Event queuing during initialization
- Foreground service management (Android 12+)
- ProcessLifecycleOwner integration

### 75.2.5. OverlayCoordinator

**Path:** `accessibility/overlays/OverlayCoordinator.kt`

Centralizes overlay display management across overlay types.

**Managed Overlays:**
- NumberedSelectionOverlay
- ContextMenuOverlay
- HelpOverlay
- RenameHintOverlay (future)
- VoiceCursor (future)

## 75.3. Interface Abstractions

### 75.3.1. IVoiceOSContext

**Path:** `accessibility/IVoiceOSContext.kt`

Abstracts VoiceOSService dependencies for testability and Dependency Inversion compliance.

```kotlin
interface IVoiceOSContext {
    val context: Context
    val accessibilityService: AccessibilityService
    val windowManager: WindowManager
    val packageManager: PackageManager
    val rootInActiveWindow: AccessibilityNodeInfo?

    fun performGlobalAction(action: Int): Boolean
    fun getAppCommands(): Map<String, String>
    fun getSystemService(name: String): Any?
    fun startActivity(intent: Intent)
    fun showToast(message: String)
    fun vibrate(duration: Long)
}
```

**Benefits:**
- Handlers depend on interface, not concrete service
- Enables mock implementations for testing
- Follows SOLID Dependency Inversion Principle

## 75.4. Action Handler Interface

**Path:** `accessibility/handlers/ActionHandler.kt`

Standard interface for all action handlers:

```kotlin
interface ActionHandler {
    fun canHandle(action: String): Boolean
    fun handle(action: String): Boolean
    suspend fun handleAsync(action: String): Boolean
    fun getSupportedActions(): List<String>
    fun initialize()
    fun dispose()
}
```

**Implementations:**
- `SystemHandler` - System commands (back, home, recents)
- `AppHandler` - App launching
- `NavigationHandler` - Screen navigation
- `UIHandler` - UI element interaction
- `InputHandler` - Text input

## 75.5. Integration Pattern

### VoiceOSService Integration

```kotlin
class VoiceOSService : AccessibilityService(), IVoiceOSContext {

    // Decomposed managers
    private var voiceRecognitionManager: VoiceRecognitionManager? = null
    private var overlayCoordinator: OverlayCoordinator? = null
    private var commandDispatcher: CommandDispatcher? = null
    private var serviceLifecycleManager: ServiceLifecycleManager? = null

    override fun onCreate() {
        super.onCreate()

        serviceLifecycleManager = ServiceLifecycleManager(
            service = this,
            context = applicationContext,
            speechEngineManager = speechEngineManager,
            onEventReceived = { event -> handleAccessibilityEvent(event) },
            onServiceReady = { onComponentsReady() }
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceLifecycleManager?.onServiceConnected()
    }

    private fun onComponentsReady() {
        // Initialize managers
        voiceRecognitionManager = VoiceRecognitionManager(...)
        overlayCoordinator = OverlayCoordinator(this)
        commandDispatcher = CommandDispatcher(...)

        // Wire up tiers
        commandDispatcher?.setCommandManager(commandManagerInstance)
        commandDispatcher?.setVoiceCommandProcessor(voiceCommandProcessor)
    }

    override fun onDestroy() {
        voiceRecognitionManager?.cleanup()
        overlayCoordinator?.cleanup()
        commandDispatcher?.cleanup()
        serviceLifecycleManager?.cleanup()
        super.onDestroy()
    }
}
```

## 75.6. Command Flow

```
Voice Input
    ↓
VoiceRecognitionManager (callback)
    ↓
CommandDispatcher.processVoiceCommand()
    ↓
┌─ Rename Tier ─→ RenameCommandHandler
│        (if "rename X to Y")
├─ Web Tier ────→ WebCommandCoordinator
│        (if browser detected)
├─ Tier 1 ─────→ CommandManager (PRIMARY)
│        (unified command system)
├─ Tier 2 ─────→ VoiceCommandProcessor (SECONDARY)
│        (hash-based app commands)
└─ Tier 3 ─────→ ActionCoordinator → HandlerRegistry
         (legacy handler-based)
              ↓
         ActionHandler.handle()
```

## 75.7. SOLID Compliance

| Principle | Implementation |
|-----------|----------------|
| **S** - Single Responsibility | Each manager has one clear purpose |
| **O** - Open/Closed | Managers extensible via callbacks |
| **L** - Liskov Substitution | Managers can be mocked/replaced |
| **I** - Interface Segregation | Focused interfaces, no fat APIs |
| **D** - Dependency Inversion | IVoiceOSContext abstraction |

## 75.8. Testing Strategy

### Unit Tests

1. **CommandDispatcher**
   - Test tier fallthrough (T1 → T2 → T3)
   - Test web command routing
   - Test rename command detection
   - Test fallback mode

2. **HandlerRegistry**
   - Test handler registration/unregistration
   - Test priority-based lookup
   - Test category management

3. **VoiceRecognitionManager**
   - Test engine initialization
   - Test start/stop listening
   - Test command event processing

4. **ServiceLifecycleManager**
   - Test service connected flow
   - Test event filtering
   - Test event queuing

### Integration Tests

- Full command flow: Speech → Recognition → Dispatch → Execution
- Overlay updates during command execution
- Service lifecycle events

## 75.9. Migration Notes

- Old IPC-based manager renamed to `VoiceRecognitionManagerIPC`
- All existing functionality preserved
- No breaking changes to public API
- Backward compatible with existing integrations
- Managers can be enabled/disabled independently

## 75.10. Related Files

| File | Purpose |
|------|---------|
| `CommandDispatcher.kt` | Multi-tier command routing |
| `HandlerRegistry.kt` | Handler registration/lookup |
| `IVoiceOSContext.kt` | Context abstraction interface |
| `VoiceRecognitionManager.kt` | Speech recognition lifecycle |
| `ServiceLifecycleManager.kt` | Service lifecycle events |
| `OverlayCoordinator.kt` | Overlay management |
| `ActionCoordinator.kt` | Handler execution coordination |
| `MetricsCollector.kt` | Performance metrics |

## 75.11. Living Documentation

Full decomposition details: `Docs/VoiceOS/LivingDocs/LD-VOS-Service-Decomposition-V1.md`
