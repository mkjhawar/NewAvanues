# VoiceOS Issue: Command Generation Architecture Gap

**Issue ID:** VoiceOS-Issue-CommandGenArchitecture-60105-V1
**Created:** 2026-01-05
**Priority:** High
**Status:** Open
**Component:** VoiceOSCoreNG

---

## Summary

Voice command generation logic (`generateCommands()`) is misplaced in the Android test app instead of the KMP shared module. Commands are created as local `GeneratedCommand` objects, displayed in UI, then discarded - creating a "dead end" where no voice execution is possible.

---

## Current State

### Location of Command Generation
- **File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`
- **Function:** `generateCommands()` (lines 401-448)
- **Output:** Local `GeneratedCommand` data class (Android-only)

### What `generateCommands()` Does
```kotlin
data class GeneratedCommand(
    val phrase: String,           // "tap Reset"
    val alternates: List<String>, // ["press Reset", "select Reset", "Reset"]
    val targetVuid: String,       // "abc123-B12345678"
    val action: String,           // "tap", "scroll", "focus"
    val element: ElementInfo,     // Full element reference
    val derivedLabel: String      // "Reset"
)
```

### Problems
1. **Platform-Specific:** Logic is Android-only, not in KMP `commonMain`
2. **Wrong Type:** Uses local `GeneratedCommand` instead of KMP `QuantizedCommand`
3. **No Persistence:** Commands are displayed then discarded
4. **No Confidence Scoring:** Missing quality/reliability metrics
5. **No Synonyms:** Limited alternate phrases (hardcoded patterns)
6. **Dead End:** Cannot execute voice commands - data goes nowhere

---

## Expected State

### Location
- **File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/command/CommandGenerator.kt`
- **Output:** `QuantizedCommand` from `avu/QuantizedCommand.kt`

### Existing KMP Type (Unused)
```kotlin
// Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../avu/QuantizedCommand.kt
data class QuantizedCommand(
    val uuid: String = "",
    val phrase: String,
    val actionType: CommandActionType,
    val targetVuid: String?,
    val confidence: Float
)
```

### Data Flow (Required)
```
AccessibilityEvent → ElementExtractor → CommandGenerator → QuantizedCommand → Persistence
                                                                                    ↓
                                                                           Voice Execution
```

---

## Impact

| Area | Impact |
|------|--------|
| Voice Execution | **Blocked** - No commands available for matching |
| Cross-Platform | **Blocked** - Android-only implementation |
| Testing | **Limited** - Can view commands but not test execution |
| AVU Export | **Blocked** - No `QuantizedCommand` objects to export |

---

## Root Cause

Test app was developed for UI visualization without considering the command execution pipeline. The `GeneratedCommand` type was created locally for display purposes rather than integrating with the KMP architecture.

---

## Constraints

1. **Minimal Overhead:** Avoid unnecessary processing or storage
2. **AVU Export Deferred:** Export functionality is future work
3. **Memory Efficient:** Don't hold large command lists in memory
4. **Cross-Platform:** Solution must work in KMP `commonMain`

---

## Related Files

| File | Purpose |
|------|---------|
| `VoiceOSAccessibilityService.kt:401-448` | Current (broken) implementation |
| `QuantizedCommand.kt` | KMP command type (unused) |
| `QuantizedElement.kt` | KMP element type |
| `VUIDGenerator.kt` | VUID generation (working) |
| `JitProcessor.kt` | JIT processing (Android-specific) |

---

## Acceptance Criteria

1. [ ] `generateCommands()` logic moved to KMP `commonMain`
2. [ ] Outputs `QuantizedCommand` instead of local type
3. [ ] Commands persisted for voice execution lookup
4. [ ] Test app displays commands from shared implementation
5. [ ] No regression in scan/display functionality
6. [ ] Minimal memory/processing overhead

---

## Notes

- AVU export is explicitly deferred - focus on in-memory/persistence pipeline
- The most efficacious approach is preferred over comprehensive solutions
- Existing `QuantizedCommand` type should be used without modification if possible
