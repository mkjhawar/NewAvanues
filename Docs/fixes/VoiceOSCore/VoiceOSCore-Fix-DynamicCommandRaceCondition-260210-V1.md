# VoiceOSCore-Fix-DynamicCommandRaceCondition-260210-V1

## Bug Fix: Dynamic Commands Show `registry size: 0` During Runtime

### Issue
Dynamic commands intermittently fail with `Dynamic command registry size: 0` during
voice command processing, especially after switching between apps or screens.
Commands "work a few times, but most of the time" show empty registry.

### Root Cause
**Race condition between synchronous clear and asynchronous re-registration.**

In `handleScreenChangeDebounced()`, on package change:
```kotlin
getActionCoordinator().clearDynamicCommands()  // SYNC: registry = 0 instantly
handleScreenChange(event)                       // ASYNC: coroutine on Dispatchers.Default
```

The async coroutine must:
1. Get `rootInActiveWindow` (can return null during app transition)
2. Extract elements (accessibility tree traversal, 100-500ms)
3. Fingerprint screen
4. Generate commands
5. Call `commandRegistry.update()` (mutex-protected)

**Any voice command arriving between the sync clear and the async update sees `registry size: 0`.**

Additionally, if the coroutine fails at any early-return point (null root, empty
elements, fingerprint match), the registry stays at 0 permanently until the next
successful scan.

### Why `clearDynamicCommands()` Was Unnecessary
`CommandRegistry.update()` already does an **atomic full-replace**:
```kotlin
private fun updateInternal(newCommands: List<QuantizedCommand>) {
    val newCommands = mutableMapOf<String, QuantizedCommand>()
    // Builds from scratch — NEVER merges with existing
    for (cmd in validCommands) { newCommands[key] = cmd }
    snapshot = CommandSnapshot(newCommands, newLabelCache)  // Atomic write
}
```

There is no command accumulation risk because `update()` replaces the entire
snapshot. The explicit `clear()` before the async scan was defense-in-depth
that created worse UX than the problem it prevented.

### Trade-off Analysis
| Scenario | With clear (before) | Without clear (after) |
|----------|--------------------|-----------------------|
| Voice command during scan | `registry size: 0` → "Unknown command" | Stale commands → may not match, or tap wrong spot briefly |
| Scan fails entirely | Registry = 0 permanently | Old commands persist until next scan |
| Normal operation | Race window ~100-500ms | No race window |

Stale commands during the brief scan window are strictly better UX than
no commands at all.

### Fix
**`VoiceOSAccessibilityService.kt`** (`handleScreenChangeDebounced()`):
- Removed `getActionCoordinator().clearDynamicCommands()` from package change path
- Added `lastScreenHash = ""` to force re-scan for new app (prevents stale
  fingerprint match from skipping the scan)
- `CommandRegistry.update()` inside `handleScreenChange()` handles atomic replacement

### Data Flow (After Fix)
```
App switch: A → B
  T=0:   Package change detected, lastScreenHash = "" (force re-scan)
  T=0:   handleScreenChange() → coroutine starts on Dispatchers.Default
  T=0:   Registry still has App A's commands (stale but functional)
  T=100: Voice: "Extra dim" → registry has App A's stale commands
         → might match if element existed in App A, otherwise "no match"
  T=300: Coroutine completes → commandRegistry.update() atomically
         replaces with App B's commands
  T=301: Voice: "Extra dim" → registry has App B's correct commands → SUCCESS
```

### Files Modified
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/VoiceOSAccessibilityService.kt`
  - Replaced `clearDynamicCommands()` with `lastScreenHash = ""` in package change handler

### Related Fixes
- `VoiceOSCore-Fix-DualCoordinatorHandlerLookup-260210-V1.md` — Fixed handler lookup
- Previous commit `346659c9` — Fixed static command grammar + initial refreshScreen
