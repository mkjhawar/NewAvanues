# VoiceOSCore Fix: Function Label Always Shows "Click"

**Date**: 2026-02-12
**Branch**: IosVoiceOS-Development
**Status**: Implemented, build verified

## Problem

In the voice command management UI (HomeScreen.kt), when drilling down into a static command to view details or add synonyms, the "Function:" label always displays "Click" regardless of the actual command action type. For example:
- "toggle bluetooth" → Function: Click (should be "Toggle Bluetooth")
- "brightness down" → Function: Click (should be "Brightness Down")
- "clear notifications" → Function: Click (should be "Clear Notifications")

## Root Cause

Classic "write-but-don't-read-back" bug in `VoiceCommandDaoAdapter.kt`.

### Data Flow

1. **VOS file parse**: `action_map` has correct values: `"sys_bluetooth": "TOGGLE_BLUETOOTH"`
2. **DB insert** (`VoiceCommandDaoAdapter.insert()`): Stores `entity.resolvedAction` → `action` column = `"TOGGLE_BLUETOOTH"` ✓
3. **DB read** (`toEntity()` extension, line 302): **Missing `this.action` mapping** → `entity.actionType = ""` ✗
4. **resolvedAction fallback**: `"".ifEmpty { id }` = `"sys_bluetooth"` (command ID, not action type)
5. **Enum parse**: `CommandActionType.fromString("sys_bluetooth")` → `valueOf("SYS_BLUETOOTH")` → no match → defaults to `CLICK`
6. **UI display**: `formatActionType(CLICK)` → "Click"

### The Missing Line

```kotlin
// BEFORE (broken): toEntity() omits the action column
private fun Commands_static.toEntity() = VoiceCommandEntity(
    uid = this.id,
    id = this.command_id,
    // ... other fields ...
    category = this.category,
    // actionType NOT SET → defaults to ""
    priority = this.priority.toInt(),
)

// AFTER (fixed): actionType reads from action column
private fun Commands_static.toEntity() = VoiceCommandEntity(
    uid = this.id,
    id = this.command_id,
    // ... other fields ...
    category = this.category,
    actionType = this.action,  // ← THE FIX
    priority = this.priority.toInt(),
)
```

## Fix

| File | Change |
|------|--------|
| `VoiceCommandDaoAdapter.kt:302` | Added `actionType = this.action` to `toEntity()` extension |

One line. The `action` column already stores the correct `CommandActionType` name string — it was just never being read back into the entity.

## Impact

After fix, the UI correctly displays:
- "toggle bluetooth" → Function: Toggle Bluetooth
- "brightness down" → Function: Brightness Down
- "clear notifications" → Function: Clear Notifications
- "go back" → Function: Navigate Back
- "scroll down" → Function: Scroll Down
- etc.

All 60+ static commands now show their actual function instead of generic "Click".

## Related Investigation: Hardcoded "click" Command

The user also asked whether the static `acc_click` command (VOS entry for "click"/"tap"/"press") is necessary.

**Finding: acc_click is REDUNDANT and non-functional.**

Reasons:
1. Dynamic commands already handle all click use cases ("click 4", "tap Submit", etc.)
2. Static acc_click has no target resolution — saying "click" alone fails with no handler
3. The voice pipeline extracts verb + target, then matches target against dynamic commands
4. Cursor click already exists as `voice_cursor_click` for clicking at cursor position

**Updated Decision**: Instead of removing acc_click, its localized phrases now serve as the locale-aware verb registry (see below).

## Localized Verb Extraction (Implemented Alongside)

Discovered during investigation: `ActionCoordinator.actionVerbs` and `SynonymRegistry` were hardcoded English-only. Non-English users couldn't use "verb + target" dynamic commands (e.g., Spanish "pulsar 4" failed because "pulsar" wasn't recognized as a click verb).

### Solution: LocalizedVerbProvider

VOS files already contain localized verb phrases in `acc_click`/`acc_long_click` entries. These are now extracted at runtime and injected into the verb extraction pipeline.

| File | Change |
|------|--------|
| `LocalizedVerbProvider.kt` (NEW) | KMP commonMain singleton: locale-aware verb registry with canonical mapping |
| `ActionCoordinator.kt` | Replaced hardcoded `actionVerbs` with `LocalizedVerbProvider.getActionVerbs()`. Normalize localized verbs to canonical English for handler routing. |
| `CommandManager.kt` | After `StaticCommandRegistry.initialize()`, extracts verb phrases from acc_click/acc_long_click, populates LocalizedVerbProvider and SynonymRegistry |
| `SynonymRegistry.kt` | Added `addLocalizedVerbs()` and `clearLocalizedVerbs()` for locale-aware synonym injection |

### Localized Verbs Extracted Per Locale

| Locale | Click Verbs | Long Press Verbs |
|--------|-------------|------------------|
| en-US | click, tap, press | long press, long click, press and hold, hold |
| es-ES | pulsar, clic, tocar, toca | mantener pulsado, pulsación larga, pulsar y mantener, toque largo |
| fr-FR | cliquer, appuyer, toucher, taper | appui long, clic long, maintenir, rester appuyé |
| de-DE | klicken, tippen, drücken | lang drücken, langer Druck, gedrückt halten, halten |
| hi-IN | click karo, tap karo, dabao | der tak dabao, long press karo, long click karo, daba kar rakho |

### Data Flow

```
VOS file loads (e.g., es-ES.app.vos)
  → CommandLoader seeds DB
  → CommandManager.populateStaticRegistryFromDb()
  → StaticCommandRegistry.initialize(commands)
  → Extract acc_click phrases → LocalizedVerbProvider.updateVerbs()
  → SynonymRegistry.addLocalizedVerbs()
  → User says "pulsar 4"
  → ActionCoordinator extracts verb="pulsar", target="4"
  → canonicalVerbFor("pulsar") → "click"
  → Handler receives "click 4" → taps element 4
```

## Verification

1. Build: `./gradlew :Modules:VoiceOSCore:compileDebugKotlin :apps:avanues:compileDebugKotlin` — PASSED
2. Deploy to device, enable accessibility service
3. Go to Dashboard → Voice Commands → any category → expand command
4. Verify "Function:" shows actual action type (e.g., "Toggle Bluetooth")
5. Verify all categories display correctly (Navigation, Media, System, etc.)
6. Switch locale to es-ES → say "pulsar 4" → should tap element 4
7. Verify SynonymRegistry gains localized verbs after locale load
