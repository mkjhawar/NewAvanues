# VoiceOSCore-Fix-DualCoordinatorHandlerLookup-260210-V1

## Bug Fix: Dynamic & Static Commands Not Executing

### Issue
All dynamic commands (e.g., "Extra dim") and static commands fail with:
```
No handler found for: tap extra dim
```
Voice recognition, command registry lookup, and phrase construction all succeed,
but `findHandler()` returns null every time.

### Root Cause
**Two separate ActionCoordinators exist with different handler registries:**

1. `VoiceAvanueAccessibilityService.actionCoordinator` (line 101 of `onServiceReady`)
   - Created as bare `ActionCoordinator(commandRegistry = registry)`
   - **NO handlers registered** (empty HandlerRegistry)

2. `VoiceOSCore.coordinator` (internal, private)
   - Created inside VoiceOSCore constructor
   - Has `AndroidGestureHandler`, `SystemHandler`, `AppHandler` registered via `initialize()`

Both share the same `CommandRegistry` (so dynamic command lookup works), but each has
its own `HandlerRegistry`. Since `getActionCoordinator()` returned the bare coordinator,
`findHandler()` always returned null.

### Data Flow (Before Fix)
```
Voice: "Extra dim"
  -> processVoiceCommand("extra dim")
  -> CommandRegistry.findByPhrase("extra dim") -> FOUND (shared registry) OK
  -> actionTypeToPhrase(CLICK, "extra dim") -> "tap extra dim" OK
  -> processCommand(QuantizedCommand(phrase="tap extra dim"))
  -> handlerRegistry.findHandler("tap extra dim") -> NULL (empty registry!)
  -> "No handler found for: tap extra dim"
```

### Fix
1. **VoiceOSCore.kt**: Expose internal ActionCoordinator via `actionCoordinator` property
2. **VoiceAvanueAccessibilityService.kt**: `getActionCoordinator()` now prefers
   `voiceOSCore?.actionCoordinator` (which has handlers) over the bare fallback

### Files Modified
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/VoiceOSCore.kt`
  - Added `val actionCoordinator: ActionCoordinator get() = coordinator`
- `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/service/VoiceAvanueAccessibilityService.kt`
  - Updated `getActionCoordinator()` to prefer VoiceOSCore's coordinator

### Test
After fix, the flow should be:
```
Voice: "Extra dim"
  -> processVoiceCommand("extra dim")
  -> CommandRegistry.findByPhrase("extra dim") -> FOUND
  -> "tap extra dim"
  -> handlerRegistry.findHandler("tap extra dim") -> AndroidGestureHandler
  -> AndroidGestureHandler.execute(CLICK, bounds=859,2257,996,2337)
  -> Gesture dispatched -> SUCCESS
```

### Note on Thread Safety
`voiceOSCore` is written from `Dispatchers.IO` and read from accessibility event thread.
Neither field uses `@Volatile`. This is a pre-existing concern, not introduced by this fix.
