# Fix: Web Voice Commands Not Executing in Browser

**Module:** VoiceOSCore + WebAvanue + apps/avanues
**Issue:** Web DOM voice commands are recognized by speech engine but fail to execute
**Severity:** HIGH
**Date:** 2026-02-11
**Status:** IMPLEMENTED — Source-Tagged Command Registry

---

## Symptoms

User says "send a message" while on a web page. Speech engine recognizes it (conf: 0.6849).
ActionCoordinator finds only 10 commands in dynamic registry, none matching.
Available commands are browser chrome elements: [add to favorites, https, settings, back, fwd].

```
DEBUG/ActionCoordinator: Dynamic command registry size: 10
DEBUG/CommandRegistry: findByPhrase: no match for 'send a message'. Available: [add to favorites, https, settings, back, fwd]
```

## Root Cause: Registry Overwrite Race Condition

`CommandRegistry.update()` atomically REPLACES all commands. Two competing updaters:

1. **Accessibility tree scan** (`VoiceOSAccessibilityService.handleScreenChange()`):
   - Fires on EVERY accessibility event (window state, content change, click, scroll)
   - Calls `coordinator.updateDynamicCommands(accessibilityCommands)`
   - These are browser chrome elements only (not WebView DOM content)

2. **Web DOM scrape** (`VoiceAvanueAccessibilityService` webCommandCollectorJob):
   - Fires when BrowserVoiceOSCallback.activeWebPhrases emits
   - Calls `voiceOSCore.actionCoordinator.updateDynamicCommands(quantizedWebCommands)`
   - These are actual web page elements (buttons, links, forms)

Each updater replaces ALL commands with its own set, destroying the other's commands.
Accessibility events fire much more frequently, so web commands are almost always overwritten.

**Speech grammar is NOT affected** because VoiceOSCore.updateCommands() maintains
`webCommandPhrases` separately and always merges them. That's why recognition works but execution fails.

## Fix: Source-Tagged Command Merging

Each command source manages its own slice of the registry. Updating one source
never removes another source's commands.

### Architecture

```
CommandRegistry
├── sourceKeys: Map<String, Set<String>>
│   ├── "accessibility" → {"add to favorites", "settings", "back", "fwd", ...}
│   └── "web" → {"send a message", "sign in", "search", ...}
├── commands: Map<String, QuantizedCommand>  (merged view of all sources)
└── labelCache: Map<String, String>          (rebuilt on each update)
```

`updateBySource("accessibility", ...)` removes only accessibility keys, adds new ones.
`updateBySource("web", ...)` removes only web keys, adds new ones.
Both sources coexist peacefully.

### Changes Made

#### 1. CommandRegistry.kt (commonMain)
- Added `sourceKeys` field to `CommandSnapshot` data class
- Added `updateBySource(source, commands)` — sync version for non-coroutine contexts
- Added `updateBySourceSuspend(source, commands)` — suspend version for coroutines
- Added `clearBySource(source)` — clears only one source's commands
- Added `updateBySourceInternal(source, commands)` — shared logic with key conflict resolution
- Key conflict handling: when two sources register the same phrase, last writer wins with logging
- `update()` and `clear()` remain unchanged for backward compatibility (replace all, clear all source tracking)
- `addAll()` now preserves `sourceKeys` from existing snapshot

#### 2. ActionCoordinator.kt (commonMain)
- Added `updateDynamicCommandsBySource(source, commands)` — delegates to `CommandRegistry.updateBySourceSuspend()`
- Added `clearDynamicCommandsBySource(source)` — delegates to `CommandRegistry.clearBySource()`

#### 3. VoiceOSAccessibilityService.kt (androidMain)
- Changed `handleScreenChange()` from `coordinator.updateDynamicCommands(commands)` to `coordinator.updateDynamicCommandsBySource("accessibility", commands)`

#### 4. VoiceAvanueAccessibilityService.kt (app-level)
- Changed `webCommandCollectorJob` from `updateDynamicCommands(quantizedWebCommands)` to `updateDynamicCommandsBySource("web", quantizedWebCommands)`
- Changed empty-phrases cleanup from implicit (no-op) to explicit `clearDynamicCommandsBySource("web")`

## Expected Behavior After Fix

```
T0: DOM scrape → updateBySource("web", webCommands) → Registry: web + accessibility
T1: Accessibility event → updateBySource("accessibility", a11yCommands) → Registry: web preserved + new a11y
T2: User says "send a message" → findByPhrase matches web command → WebCommandHandler executes via JS
```

## Files Modified
- `Modules/VoiceOSCore/src/commonMain/.../command/CommandRegistry.kt`
- `Modules/VoiceOSCore/src/commonMain/.../actions/ActionCoordinator.kt`
- `Modules/VoiceOSCore/src/androidMain/.../VoiceOSAccessibilityService.kt`
- `apps/avanues/src/main/.../service/VoiceAvanueAccessibilityService.kt`

## Extensibility
New sources can be added trivially:
- `updateBySource("plugin:my-avp", pluginCommands)` for AVU DSL plugins
- `updateBySource("nlu", nluCommands)` for NLU-generated commands
- `updateBySource("macro", macroCommands)` for user macros
