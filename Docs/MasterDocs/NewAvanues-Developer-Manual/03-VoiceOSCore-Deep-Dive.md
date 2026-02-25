---
title: "Chapter 03 — VoiceOSCore Deep Dive"
owner: "Voice Platform"
status: "active"
last_reviewed: "2026-02-10"
source_of_truth: true
---

# Chapter 03 — VoiceOSCore Deep Dive

## 3.1 Purpose

VoiceOSCore is the deterministic execution kernel of the voice platform. It receives interpreted command text and transforms it into concrete actions through handler orchestration.

## 3.2 Core Roles

| Component | Responsibility |
|---|---|
| `VoiceOSCoreNG` facade | Lifecycle + command processing API |
| `ActionCoordinator` | Multi-stage command resolution |
| `HandlerRegistry` | Priority-based handler routing |
| `CommandRegistry` | Dynamic, screen-aware command set |
| `StaticCommandRegistry` | Predefined command corpus |

## 3.3 Command Resolution Pipeline

```text
Voice Text
  -> exact phrase resolution
  -> fuzzy match
  -> static handler route
  -> NLU semantic classification
  -> LLM fallback interpretation
  -> failure/clarification
```

### Why this ordering
- Keeps common commands fast and deterministic.
- Reduces unnecessary model invocation.
- Preserves long-tail natural language support.

## 3.4 Handler System Architecture

Implemented categories include (not exhaustive):

- System (home/back/notifications)
- Navigation (scroll/swipe)
- UI interaction (click/focus/long-press)
- Input (type/clear/select/copy/paste)
- App launch/switch
- Media/device/accessibility/custom categories

## 3.5 Dynamic Command Lifecycle

### Overview

```text
Screen changes (accessibility event)
  -> debounce (100ms)
  -> extract UI elements (AndroidScreenExtractor)
  -> fingerprint screen (ScreenFingerprinter)
  -> generate dynamic commands (CommandGenerator)
  -> atomic replace in CommandRegistry
  -> update speech engine grammar (static + dynamic + app phrases)
  -> route utterances against current screen context
```

### Atomic Command Registry Replace

`CommandRegistry` uses a volatile `CommandSnapshot` reference with mutex-protected writes. The `update()` method builds a **new map from scratch** and atomically replaces the entire snapshot — it never merges with existing commands.

```text
update(newCommands) → mutex.withLock {
    build new CommandSnapshot(map, labelCache)
    snapshot = newSnapshot  // single atomic write
}
```

**Critical rule:** Never call `clearDynamicCommands()` before `handleScreenChange()`. The clear executes synchronously (registry = 0 instantly), while screen processing launches an async coroutine (100-500ms). Voice commands arriving in the gap see an empty registry. Instead, invalidate the screen fingerprint hash (`lastScreenHash = ""`) to force re-scan, and rely on `update()` to atomically replace stale commands with fresh ones.

### Screen Change Handling on Package Switch

When the foreground app changes:

```text
1. Invalidate screen fingerprint (lastScreenHash = "")
2. Call handleScreenChange() → launches coroutine on Dispatchers.Default
3. Extract elements → generate commands → atomic replace
```

Stale commands during the brief scan window (~100-500ms) are strictly better UX than "no commands available".

### Static Command Grammar Management

`VoiceOSCore.updateCommands()` sends the **entire** command set to the speech engine every time. This must include:

```text
allCommands = StaticCommandRegistry.allPhrases()  // ~185 phrases
            + dynamicCommands                      // screen-specific
            + appHandlerPhrases                    // app-specific handlers
```

Omitting static phrases causes grammar loss — the speech engine replaces its entire grammar on each `updateCommands()` call.

## 3.6 Element Extraction and Parent Clickability

### Accessibility Tree Traversal

`AndroidScreenExtractor` recursively walks the Android accessibility node tree via `AccessibilityNodeInfo`. Key behaviors:

- **Depth limit**: MAX_DEPTH = 30 prevents stack overflow
- **Visibility filter**: Skips `!isVisibleToUser` nodes
- **Size filter**: Skips elements < 10px but still traverses their children
- **Dynamic container detection**: Tracks RecyclerView/ListView/ScrollView for list index assignment
- **Node recycling**: Calls `recycle()` on API < 34 to prevent memory leaks

### Parent Clickability Propagation

Many Android UIs (especially Settings, launchers, email clients) use this pattern:

```text
LinearLayout (clickable=true, text="")      ← no voice content → skipped
  ImageView (clickable=false)               ← no voice content → skipped
  TextView "Network & internet" (clickable=false) ← has text but not clickable
```

Without parent clickability propagation, the TextView has voice content but `isActionable=false`, so no voice command is generated. The fix:

1. `traverseNode()` tracks `isParentClickable` as an OR-propagated flag
2. `AccessibilityNodeAdapter.toElementInfo()` sets `isParentClickable` on the `ElementInfo`
3. `ElementInfo.isActionable` includes `isParentClickable`:
   ```
   isActionable = isClickable || isScrollable || isLongClickable || isParentClickable
   ```

Gesture dispatch works because Android touch events propagate up the view hierarchy — tapping at the TextView's coordinates triggers the parent LinearLayout's click handler.

### Command Generation Gate

`CommandGenerator.fromElement()` requires `element.isActionable` to return a command. The `shouldIncludeElement()` filter in `AndroidScreenExtractor` includes elements that are either actionable OR have text+enabled state, but command generation has the stricter `isActionable` gate.

## 3.7 Speech Engine Strategy

VoiceOSCore integrates multiple engines (Android STT, Google/Azure cloud, Vivoka, Vosk, Apple Speech), abstracted behind a consistent speech interface.

Design result:
- Pluggable engine choice by environment.
- Offline/privacy options.
- Custom vocabulary and grammar updates where supported.

## 3.8 State Model

Typical service progression:

```text
Uninitialized -> Initializing -> Ready -> Listening -> Processing -> Ready
                                     \-> Error
```

`StateFlow`/`Flow` exposure enables UI feedback and diagnostics integration.

## 3.9 Integration Contract

### Required from host app
1. **Lifecycle ownership** (`initialize`/`dispose`).
2. **Platform executor wiring** (e.g., Android accessibility service).
3. **Speech result bridge** — collect from `voiceOSCore.speechResults` and route to `processVoiceCommand()`.

### Speech Result Bridge (Critical)

VoiceOSCore creates the speech engine directly via `speechEngineFactory.createEngine()` and exposes results through `speechResults: SharedFlow<SpeechResult>`. The host app **must** collect from this flow:

```text
serviceScope.launch {
    voiceOSCore.speechResults
        .conflate()  // drop intermediate if processing is slow
        .collect { result ->
            if (result.isFinal && result.confidence >= threshold) {
                processVoiceCommand(result.text, result.confidence)
            }
        }
}
```

> **Note:** The confidence `threshold` is adaptive via `AdaptiveTimingManager.getConfidenceFloor()` (default 0.45f, range 0.3-0.7). This replaced a hardcoded 0.5f check. See Chapter 102 Section 21 for the full adaptive timing system.

Without this bridge, Vivoka recognizes speech and emits results, but nobody calls `processCommand()` and no action executes.

**Note:** `SpeechEngineManager` exists with its own result collection, but VoiceOSCore bypasses it. The host app is responsible for the bridge.

### Dual Coordinator Pattern

The host app may create a bare `ActionCoordinator` for direct use, but VoiceOSCore creates its own coordinator with registered handlers. `getActionCoordinator()` must prefer the VoiceOSCore instance:

```text
fun getActionCoordinator(): ActionCoordinator {
    return voiceOSCore?.actionCoordinator  // has handlers registered
        ?: actionCoordinator               // bare fallback
}
```

Using the bare coordinator results in handler lookup failures — `findHandler()` returns null for every command.

### Optional but recommended
1. NLU model integration.
2. LLM fallback integration.
3. Dynamic command updates per screen transition.

## 3.10 Failure Modes and Recovery

| Failure Class | Recovery Pattern |
|---|---|
| Low confidence recognition | reprompt + alternatives |
| Multiple UI matches | disambiguation prompt |
| Handler unavailable | category fallback / not handled |
| Platform action failure | retry with alternate action path |
| Model unavailable | degrade to deterministic command set |

## 3.11 Practical Extension Pattern

To add a new command category:

1. Add/extend action type.
2. Implement new handler with explicit `supportedActions`.
3. Register handler in registry with intentional priority.
4. Add static/dynamic command phrases.
5. Add test cases for ambiguity and fallback behavior.
