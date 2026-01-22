# Analysis: Numeric Voice Commands Not Clicking

**Date:** 2026-01-20 | **Version:** V1 | **Author:** Claude

## Summary

Numeric voice commands (1, 2, 3...9) were recognized by the UI (shown in floating menu) but the click action was not being performed. Text commands like Delete, Plus, Minus worked correctly. This issue persisted across multiple calculator apps.

## Findings

| Category | Status | Details |
|----------|--------|---------|
| Root Cause | IDENTIFIED | Numeric commands were visual-only overlays, never added to command registry |
| Speech Engine | PARTIAL | Numeric phrases were not taught to Vivoka grammar |
| Command Registry | MISSING | No QuantizedCommand objects created for numeric badges |
| Bounds Resolution | N/A | Never reached - commands failed at lookup stage |

## Root Cause Analysis

### The Command Generation Pipeline

When the system scans a screen, `DynamicCommandGenerator.generateCommands()` was creating:

1. **Element-based commands** (from element labels) - "Delete", "Plus", "More options" ✓
2. **Index-based commands** (ordinal words) - "first", "second", "item 4" ✓
3. **Label-based commands** (from list content) - "Lifemiles", "Gmail" ✓
4. **Overlay items** (visual badges) - NumberOverlayItem with number=1,2,3... ✓

**MISSING: Numeric commands** ("1", "2", "3") were generated ONLY as `NumberOverlayItem` objects for visual display but NEVER converted to `QuantizedCommand` objects.

### Code Flow Comparison

**Text Command Path ("Delete" succeeds):**
```
User says "Delete"
→ Speech engine recognizes (trained on "Delete")
→ ActionCoordinator.processVoiceCommand("delete")
→ commandRegistry.findByPhrase("delete") FOUND ✓
→ AndroidGestureHandler.execute() with bounds
→ Click succeeds
```

**Numeric Command Path ("1" fails):**
```
User says "1"
→ Speech engine recognizes (NOT trained - fails)
→ ActionCoordinator.processVoiceCommand("1")
→ commandRegistry.findByPhrase("1") NOT FOUND ✗
→ HandlerResult.failure("Unknown command: 1")
→ No click, silent failure
```

## Fix Applied

### 1. Added `generateNumericCommands()` to CommandGenerator

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/CommandGenerator.kt`

Created new method that:
- Takes list items with valid listIndex
- Groups by listIndex, keeps best element per index
- Sorts by visual position (listIndex)
- Creates QuantizedCommand with phrase = "1", "2", "3"... (raw number)
- Includes bounds in metadata for BoundsResolver

### 2. Updated DynamicCommandGenerator

**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/DynamicCommandGenerator.kt`

Changes:
- Call `CommandGenerator.generateNumericCommands()` after label commands
- Add numeric commands to `commandRegistry.addAll(numericCommands)`
- Include numeric phrases in speech engine update
- Added `numericCommands` field to `CommandGenerationResult` data class
- Updated both `generateCommands()` and `generateCommandsIncremental()` methods

## Files Modified

| File | Change |
|------|--------|
| `CommandGenerator.kt:169-221` | Added `generateNumericCommands()` method |
| `DynamicCommandGenerator.kt:51-59` | Updated `CommandGenerationResult` data class |
| `DynamicCommandGenerator.kt:136-143` | Generate and register numeric commands |
| `DynamicCommandGenerator.kt:159-166` | Include numeric phrases in speech engine |
| `DynamicCommandGenerator.kt:185-192` | Return numericCommands in result |
| `DynamicCommandGenerator.kt:279-288` | Added numeric commands to incremental update |

## Expected Behavior After Fix

1. **First visit to screen:** Scan UI, generate numeric commands, add to registry, teach to speech engine
2. **User says "1":** Speech engine recognizes, registry lookup succeeds, BoundsResolver finds element, click executes
3. **User says "tap 2":** ActionCoordinator extracts target "2", same flow as above
4. **Scroll/content change:** Incremental update includes numeric commands for visible items

## Verification Steps

1. Build app: `./gradlew :android:apps:voiceoscoreng:assembleDebug`
2. Install on device
3. Open Calculator app
4. Verify numeric badges appear (1, 2, 3...)
5. Say "1" - should click first numbered element
6. Say "tap 2" - should click second numbered element
7. Test on 3rd party calculator app to verify fix works universally

## Related Commits

- `0451106b` - Prevent continuous speech engine updates on same screen
- `0d0fc3ec` - Add incremental command generation for scroll updates
- `59415d80` - Integrate BoundsResolver into click execution flow
- `83ee6a8b` - Migrate BoundsResolver to VoiceOSCore module
- `eb17c484` - Update speech engine when loading cached commands

## Architecture Notes

The numeric command flow now follows the same pattern as index commands:

```
generateOverlayItems()     → Visual display (badge with "1")
generateNumericCommands()  → Voice command (phrase: "1", bounds, vuid)
commandRegistry.addAll()   → Register for lookup
updateSpeechEngine()       → Teach Vivoka grammar
```

This ensures the numbered badges shown to users match actual executable voice commands.
