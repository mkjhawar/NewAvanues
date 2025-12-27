# Command Logic and Storage Architecture Decision

**Created:** 2025-10-14 23:26:00 PDT
**Type:** Architecture Decision Record (ADR)
**Status:** Pending Approval
**Priority:** Critical - Affects entire CommandRegistry implementation

---

## Question

**Where should command logic and command definitions live?**

1. **In CommandManager** with CommandHandlers storing and tracking commands?
2. **In individual modules** (VoiceCursor, VoiceKeyboard) with CommandHandlers as thin routing layers?

---

## Current State Analysis

### What Exists Today

I analyzed the current codebase and found **both patterns coexisting**:

#### Pattern 1: CommandManager Contains Actions (Current)
```
CommandManager/
├── actions/
│   ├── CursorActions.kt         ← Cursor command logic HERE
│   ├── NavigationActions.kt     ← Navigation logic HERE
│   ├── VolumeActions.kt         ← Volume logic HERE
│   ├── SystemActions.kt         ← System logic HERE
│   ├── GestureActions.kt        ← Gesture logic HERE
│   ├── DragActions.kt           ← Drag logic HERE
│   ├── TextActions.kt           ← Text logic HERE
│   ├── AppActions.kt            ← App logic HERE
│   └── (10+ more action files)
└── CommandManager.kt
    └── Stores action maps:
        - navigationActions = mapOf("nav_back" -> BackAction())
        - volumeActions = mapOf("volume_up" -> VolumeUpAction())
        - systemActions = mapOf("wifi_toggle" -> WifiToggleAction())
```

**BUT:** CursorActions **delegates** to VoiceCursorAPI:
```kotlin
// CommandManager/actions/CursorActions.kt
object CursorActions {
    suspend fun moveCursor(direction: CursorDirection, distance: Float): Boolean {
        // Delegates to VoiceCursorAPI (module logic)
        val currentPosition = VoiceCursorAPI.getCurrentPosition()
        val newPosition = calculateNewPosition(direction, distance)
        return VoiceCursorAPI.moveTo(newPosition, animate = true)
    }

    suspend fun click(): Boolean {
        // Delegates to VoiceCursorAPI (module logic)
        return VoiceCursorAPI.click()
    }
}
```

#### Pattern 2: Module Contains Logic (VoiceCursor)
```
VoiceCursor/
├── VoiceCursorAPI.kt            ← Public API with cursor logic
│   ├── initialize()
│   ├── showCursor()
│   ├── hideCursor()
│   ├── moveTo()
│   ├── click()
│   └── (all cursor operations)
├── manager/
│   └── CursorOverlayManager.kt  ← Internal implementation
└── core/
    └── CursorConfig.kt          ← Cursor configuration
```

**Result:** **Hybrid approach** - CommandManager has action wrappers, but they delegate to module APIs.

---

## Architectural Options

### Option A: CommandManager Stores Commands (Centralized)

```
┌─────────────────────────────────────────────────────────────┐
│                    CommandManager                            │
│  (Central Command Storage and Tracking)                     │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Command Definitions + Action Logic                     │ │
│  │                                                         │ │
│  │ navigationActions = mapOf(                             │ │
│  │   "nav_back" -> BackAction { service.goBack() },      │ │
│  │   "nav_home" -> HomeAction { service.goHome() }       │ │
│  │ )                                                       │ │
│  │                                                         │ │
│  │ cursorActions = mapOf(                                 │ │
│  │   "cursor up" -> CursorUpAction { moveCursor(UP) },   │ │
│  │   "click" -> ClickAction { performClick() }           │ │
│  │ )                                                       │ │
│  │                                                         │ │
│  │ keyboardActions = mapOf(                               │ │
│  │   "type hello" -> TypeAction { keyboard.type(...) }   │ │
│  │ )                                                       │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ CommandRegistry (Routing)                              │ │
│  │   ├─→ VoiceOSCommandHandler (wraps navigationActions)  │ │
│  │   ├─→ VoiceCursorCommandHandler (wraps cursorActions)  │ │
│  │   └─→ VoiceKeyboardCommandHandler (wraps keyboardActions)│ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
        │
        ↓ (delegates for execution only)
┌──────────────────────────────────────────────────────────────┐
│  Modules (Implementation Only)                               │
│  ├─ VoiceCursor: CursorOverlayManager (render, positioning) │
│  ├─ VoiceKeyboard: InputMethodService (IME functionality)   │
│  └─ VoiceOS: ActionCoordinator (system actions)             │
└──────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ Central command registry - easy to see all commands
- ✅ CommandManager becomes "source of truth"
- ✅ Easy to track command usage/metrics in one place
- ✅ Simpler for modules - they just execute, no command knowledge
- ✅ Cross-module commands easier (e.g., "type then click")

**Cons:**
- ❌ CommandManager becomes massive (already has 17 action files!)
- ❌ Tight coupling - CommandManager must know about all modules
- ❌ Hard to add third-party commands (requires editing CommandManager)
- ❌ Module independence lost - cursor can't evolve commands independently
- ❌ Violates separation of concerns - CommandManager shouldn't know cursor details

---

### Option B: Modules Own Commands (Distributed) ⭐ **RECOMMENDED**

```
┌─────────────────────────────────────────────────────────────┐
│                    CommandRegistry                           │
│  (Routing Only - No Command Storage)                        │
│                                                              │
│  suspend fun routeCommand(command: String): Boolean {       │
│      for (handler in handlers) {                            │
│          if (handler.canHandle(command)) {                  │
│              return handler.handleCommand(command)          │
│          }                                                   │
│      }                                                       │
│      return false                                           │
│  }                                                           │
└────────────┬────────────────────────────────────────────────┘
             │ Routes to appropriate handler
             │
    ┌────────┴────────┬─────────────┬────────────┐
    ↓                 ↓             ↓            ↓
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│VoiceOS      │  │VoiceCursor  │  │VoiceKeyboard│  │ThirdParty   │
│Handler      │  │Handler      │  │Handler      │  │Handler      │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │                │
       ↓                ↓                ↓                ↓
┌─────────────────────────────────────────────────────────────┐
│         Modules Own Command Definitions + Logic              │
│                                                              │
│  VoiceOS Module:                                            │
│  ├─ Commands: ["back", "home", "recent apps"]              │
│  ├─ Logic: ActionCoordinator.handleCommand()               │
│  └─ Storage: In-memory + Database                          │
│                                                              │
│  VoiceCursor Module:                                        │
│  ├─ Commands: ["cursor up/down/left/right", "click"]       │
│  ├─ Logic: VoiceCursorAPI.moveTo(), click()                │
│  └─ Storage: VoiceCursor internal state                    │
│                                                              │
│  VoiceKeyboard Module:                                      │
│  ├─ Commands: ["type [text]", "delete", "enter"]           │
│  ├─ Logic: VoiceKeyboard.type(), delete()                  │
│  └─ Storage: Keyboard internal state                       │
│                                                              │
│  Third-Party Module (Game):                                │
│  ├─ Commands: ["attack", "jump", "inventory"]              │
│  ├─ Logic: GameController.execute()                        │
│  └─ Storage: Game module state                             │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ **True module independence** - each module evolves its commands independently
- ✅ **Clean separation of concerns** - modules own their domain
- ✅ **Easy third-party integration** - just implement CommandHandler
- ✅ **Scalable** - adding new modules doesn't touch CommandManager
- ✅ **Testable** - test each module's commands in isolation
- ✅ **Matches existing VoiceCursorAPI pattern** - already designed this way!
- ✅ **CommandRegistry stays thin** - pure routing logic only

**Cons:**
- ❌ Commands distributed - no central "command list" view
- ❌ Cross-module commands require coordination
- ❌ Need to query all handlers to get full command list

---

## Your Suggestion: Hybrid Approach

You suggested:
> "Let the modules have the logic, and have CommandHandler be responsible for storing and tracking commands and the actions they instantiate."

This is **Option B+** - modules own logic, but handlers track/expose commands:

```
┌─────────────────────────────────────────────────────────────┐
│              CommandHandler (Interface)                      │
│                                                              │
│  interface CommandHandler {                                 │
│      val moduleId: String                                   │
│      val supportedCommands: List<String>  ← TRACKING        │
│      fun canHandle(command: String): Boolean                │
│      suspend fun handleCommand(command: String): Boolean    │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
         ↑ Implementations
         │
┌────────┴─────────────────────────────────────────────────────┐
│  VoiceCursorCommandHandler                                   │
│                                                              │
│  override val supportedCommands = listOf(  ← TRACKING       │
│      "cursor up", "cursor down", "cursor left", "cursor right",│
│      "cursor up [distance]", "cursor down [distance]",      │
│      "click", "double click", "long press",                 │
│      "show cursor", "hide cursor", "center cursor"          │
│  )                                                           │
│                                                              │
│  override fun canHandle(command: String): Boolean {         │
│      return command.startsWith("cursor") ||                 │
│             command.startsWith("click") ||                  │
│             command.contains("cursor")                      │
│  }                                                           │
│                                                              │
│  override suspend fun handleCommand(command: String): Boolean {│
│      // Delegate to VoiceCursorAPI (module logic)           │
│      return when {                                          │
│          command == "cursor up" -> VoiceCursorAPI.moveUp()  │
│          command == "click" -> VoiceCursorAPI.click()       │
│          // ... etc                                         │
│      }                                                       │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
```

**Benefits of Hybrid:**
- ✅ Module logic stays in modules (VoiceCursorAPI)
- ✅ CommandHandler tracks/documents supported commands
- ✅ Easy to query: `handler.supportedCommands`
- ✅ Clean API for discovering available commands
- ✅ Handler acts as "contract" between registry and module

---

## Comparison Table

| Aspect | Option A (Centralized) | Option B (Distributed) | Your Hybrid (B+) |
|--------|------------------------|------------------------|------------------|
| **Command Logic** | CommandManager | Modules | Modules ✅ |
| **Command Storage** | CommandManager maps | Module internal | Module internal ✅ |
| **Command Tracking** | CommandManager | None | CommandHandler.supportedCommands ✅ |
| **Module Independence** | ❌ Low | ✅ High | ✅ High |
| **Central Discovery** | ✅ Easy | ❌ Hard | ✅ Easy (via handlers) |
| **Third-Party Support** | ❌ Hard | ✅ Easy | ✅ Easy |
| **Testability** | ❌ Coupled | ✅ Independent | ✅ Independent |
| **Cross-Module Commands** | ✅ Easy | ❌ Hard | ⚠️ Medium |
| **CommandManager Size** | ❌ Massive | ✅ Minimal | ✅ Minimal |

---

## Recommendation: Your Hybrid Approach (Option B+)

**I agree with your suggestion** - it's the best of both worlds:

### Architecture Design

```kotlin
// CommandHandler tracks commands, delegates execution to module
class VoiceCursorCommandHandler : CommandHandler {
    override val moduleId = "voicecursor"

    // TRACKING: Handler exposes supported commands
    override val supportedCommands = listOf(
        "cursor up", "cursor down", "cursor left", "cursor right",
        "cursor up [distance]", "cursor down [distance]",
        "click", "double click", "long press",
        "show cursor", "hide cursor", "center cursor",
        "snap to element"
    )

    override fun canHandle(command: String): Boolean {
        // Quick pre-check for routing
        return command.startsWith("cursor") ||
               command in listOf("click", "double click", "long press") ||
               command.contains("cursor")
    }

    override suspend fun handleCommand(command: String): Boolean {
        // LOGIC: Delegate to module (VoiceCursorAPI)
        return when {
            command == "cursor up" -> VoiceCursorAPI.moveUp()
            command.startsWith("cursor up ") -> {
                val distance = extractDistance(command)
                VoiceCursorAPI.moveUp(distance)
            }
            command == "click" -> VoiceCursorAPI.click()
            command == "show cursor" -> VoiceCursorAPI.showCursor()
            // ... etc
            else -> {
                Log.w(TAG, "Unhandled cursor command: $command")
                false
            }
        }
    }

    // Helper for parsing
    private fun extractDistance(command: String): Float {
        return command.substringAfter("cursor up ").toFloatOrNull() ?: 50f
    }
}
```

### What Goes Where

```
┌─────────────────────────────────────────────────────────────┐
│  CommandRegistry (Thin Routing Layer)                       │
│  - No command storage                                       │
│  - No command logic                                         │
│  - Pure router: find handler → delegate                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  CommandHandlers (Tracking + Routing)                       │
│  ✅ Store: supportedCommands list (for discovery)           │
│  ✅ Logic: canHandle() - quick pattern matching             │
│  ✅ Logic: handleCommand() - parse command and delegate     │
│  ✅ Helper: Parameter extraction (distance, text, etc.)     │
│  ❌ NO: Core implementation logic                           │
│  ❌ NO: State management                                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Module APIs (Core Logic + State)                           │
│  ✅ Store: Module internal state (cursor position, etc.)    │
│  ✅ Logic: Core functionality (move, click, type, etc.)     │
│  ✅ Logic: Business rules (validation, constraints)         │
│  ✅ Store: Configuration (CursorConfig, KeyboardSettings)   │
│  ❌ NO: Command parsing                                     │
│  ❌ NO: Voice command knowledge                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementation Strategy

### Phase 1: CommandHandlers as Thin Wrappers
```kotlin
// VoiceCursorCommandHandler.kt (in CommandManager or VoiceCursor module?)
class VoiceCursorCommandHandler : CommandHandler {
    override val supportedCommands = /* ... */
    override fun canHandle(command: String) = /* pattern matching */
    override suspend fun handleCommand(command: String): Boolean {
        // Delegate to VoiceCursorAPI
        return VoiceCursorAPI.handleVoiceCommand(command)
    }
}
```

### Phase 2: Module APIs Expose Command Handling
```kotlin
// VoiceCursorAPI.kt (in VoiceCursor module)
object VoiceCursorAPI {
    // Existing methods
    fun showCursor(): Boolean { /* ... */ }
    fun moveTo(position: CursorOffset): Boolean { /* ... */ }
    fun click(): Boolean { /* ... */ }

    // NEW: Voice command handler (called by CommandHandler)
    suspend fun handleVoiceCommand(command: String): Boolean {
        return when {
            command == "cursor up" -> moveUp()
            command == "click" -> click()
            // ... all cursor commands
            else -> false
        }
    }
}
```

### Phase 3: CommandRegistry Routes to Handlers
```kotlin
// CommandRegistry.kt
suspend fun routeCommand(command: String): Boolean {
    for ((moduleId, handler) in handlers) {
        if (handler.canHandle(command)) {
            return handler.handleCommand(command)
        }
    }
    return false
}
```

---

## Decision Point: Where Do CommandHandlers Live?

**Option 1: In CommandManager module**
```
CommandManager/
├── CommandHandler.kt (interface)
├── CommandRegistry.kt
└── handlers/
    ├── VoiceOSCommandHandler.kt
    ├── VoiceCursorCommandHandler.kt
    └── VoiceKeyboardCommandHandler.kt
```
**Pros:** Central location, easier to maintain
**Cons:** CommandManager must depend on all modules

**Option 2: In each module** ⭐ **BETTER**
```
VoiceCursor/
├── VoiceCursorAPI.kt
└── VoiceCursorCommandHandler.kt  ← Lives with module

VoiceKeyboard/
├── VoiceKeyboardAPI.kt
└── VoiceKeyboardCommandHandler.kt  ← Lives with module

VoiceOSCore/
├── VoiceOSService.kt
└── VoiceOSCommandHandler.kt  ← Lives with module
```
**Pros:** True module independence, no circular dependencies
**Cons:** Handler implementations distributed

**Recommendation:** **Option 2** - handlers live with their modules.

---

## Summary: Your Hybrid Approach is Best

### What You Suggested
> "Let the modules have the logic, and have CommandHandler be responsible for storing and tracking commands and the actions they instantiate."

**This is exactly right!** Here's the architecture:

1. **CommandRegistry**: Pure router (no storage, no logic)
2. **CommandHandlers**: Track commands via `supportedCommands`, delegate execution
3. **Module APIs**: Contain all business logic and state

### Implementation Plan
```
Phase 1: Create CommandHandler interface in CommandManager
Phase 2: Implement VoiceOSCommandHandler (wraps ActionCoordinator)
Phase 3: Implement VoiceCursorCommandHandler (wraps VoiceCursorAPI)
Phase 4: Implement VoiceKeyboardCommandHandler (wraps VoiceKeyboard)
Phase 5: Update CommandRegistry to route through handlers
Phase 6: Remove old direct CommandManager action maps
```

---

## Next Steps

1. **Confirm this architecture approach**
2. **Decide where handlers live** (in modules or CommandManager?)
3. **Begin Phase 1 implementation**

**Your approval needed:** Does this hybrid approach (handlers track, modules contain logic) align with your vision?

---

**Last Updated:** 2025-10-14 23:26:00 PDT
