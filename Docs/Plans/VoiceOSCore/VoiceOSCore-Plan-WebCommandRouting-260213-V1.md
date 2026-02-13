# Plan: Fix 45 Failing Web Browser Static Commands

**Date:** 2026-02-13
**Module:** VoiceOSCore
**Branch:** IosVoiceOS-Development
**Status:** Ready for Implementation

## Context

All 45 web static commands (18 BROWSER + 27 WEB_GESTURE) fail during device testing. The full voice command pipeline exists (WebCommandHandler, WebCommandExecutorImpl, DOMScraperBridge JS), but three interconnected routing bugs prevent commands from reaching WebCommandHandler.

---

## Root Cause Analysis (3 Bugs + 1 Missing Enum)

### Bug 1: Static Web Commands Not in Dynamic Registry

`VoiceAvanueAccessibilityService.webCommandCollectorJob` (line 209-240) only registers DOM-scraped element commands via `updateDynamicCommandsBySource("web", ...)`. Static BROWSER/WEB_GESTURE commands from .web.vos files (e.g., "go back", "refresh page", "swipe up") are **never** registered as dynamic commands. They exist in `StaticCommandRegistry` with `source="static"` — the ActionCoordinator doesn't recognize them as web-destined.

### Bug 2: extractVerbAndTarget Short-Circuits Static Commands

`ActionCoordinator.processVoiceCommand()` Step 1 calls `extractVerbAndTarget("go back")` (line 374-394). This checks `StaticCommandRegistry.findByPhrase("go back")` → finds it (NAVIGATION category) → returns `(null, null)`. With `target == null`, the entire dynamic command lookup is skipped, and the command falls through to Step 2 (static handler lookup).

### Bug 3: Priority-Based Handler Stealing

Even if Bugs 1+2 were fixed, `processCommand()` (line 171) calls `handlerRegistry.findHandler(command)` which iterates by priority. SystemHandler (priority 1) or AndroidGestureHandler (priority 2) steal overlapping phrases ("go back", "swipe up", "zoom in") before WebCommandHandler (priority 11) gets a chance.

### Missing: RETRAIN_PAGE in WebActionType

`CommandActionType.RETRAIN_PAGE` exists (CommandActionType.kt:261) and .web.vos files define "retrain page"/"rescan page" phrases. But `WebActionType` enum (IWebCommandExecutor.kt) has no `RETRAIN_PAGE`, so WebCommandHandler can't map it and WebCommandExecutorImpl can't execute it.

---

## Fix Strategy

**Core insight:** Register static BROWSER+WEB_GESTURE commands as dynamic commands with `source="web"` metadata when browser activates. Add a full-phrase pre-check in processVoiceCommand that catches web commands BEFORE extractVerbAndTarget. Add web-source bypass in processCommand to skip priority-based routing.

### Phase 1: ActionCoordinator — Web Command Pre-Check + Bypass

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/actions/ActionCoordinator.kt`

**Change 1a — processVoiceCommand()** (before line 421):
Add full-phrase dynamic registry check BEFORE extractVerbAndTarget:

```kotlin
// Pre-check: Full-phrase match for web-source commands.
// Web static commands ("go back", "swipe up") are registered as dynamic
// commands with source="web" when browser is active. This check catches
// them before extractVerbAndTarget short-circuits on StaticCommandRegistry.
if (commandRegistry.size > 0) {
    val fullPhraseMatch = commandRegistry.findByPhrase(normalizedText)
    if (fullPhraseMatch != null && fullPhraseMatch.metadata["source"] == "web") {
        LoggingUtils.d("Web full-phrase match: '${fullPhraseMatch.phrase}'", TAG)
        return processCommand(fullPhraseMatch)
    }
}
```

This goes BEFORE the existing `val (verb, target) = extractVerbAndTarget(normalizedText)` line.

**Change 1b — processCommand()** (line 171-198):
For commands with `source="web"`, bypass priority-based `findHandler()` and route directly to BROWSER category handlers:

```kotlin
val handler = if (command.metadata["source"] == "web") {
    handlerRegistry.getHandlersForCategory(ActionCategory.BROWSER)
        .firstOrNull { it.canHandle(command) }
        ?: handlerRegistry.findHandler(command)  // Fallback
} else {
    handlerRegistry.findHandler(command)
}
```

Uses existing `IHandlerRegistry.getHandlersForCategory()` (IHandlerRegistry.kt:73).

### Phase 2: VoiceAvanueAccessibilityService — Register Static Web Commands

**File:** `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/service/VoiceAvanueAccessibilityService.kt`

In `webCommandCollectorJob` (line 209-240), after registering DOM-scraped commands, also register static BROWSER + WEB_GESTURE commands with `source="web"` override:

```kotlin
// After existing DOM command registration:
val browserStaticCmds = StaticCommandRegistry.byCategoryAsQuantized(CommandCategory.BROWSER)
val gestureStaticCmds = StaticCommandRegistry.byCategoryAsQuantized(CommandCategory.WEB_GESTURE)
val webStaticCommands = (browserStaticCmds + gestureStaticCmds).map { cmd ->
    cmd.copy(metadata = cmd.metadata + mapOf("source" to "web"))
}
voiceOSCore?.actionCoordinator?.updateDynamicCommandsBySource("web_static", webStaticCommands)
```

In the `phrases.isEmpty()` branch, add: `clearDynamicCommandsBySource("web_static")`

Key: Source tag `"web_static"` keeps them separate from DOM-scraped commands (`"web"`) for independent lifecycle.

### Phase 3: RETRAIN_PAGE Support

**File 3a:** `IWebCommandExecutor.kt` — Add `RETRAIN_PAGE` to `WebActionType` enum after `HOVER_OUT`

**File 3b:** `WebCommandHandler.kt` — Add `CommandActionType.RETRAIN_PAGE -> WebActionType.RETRAIN_PAGE` in `resolveWebActionType()` + add "retrain page"/"rescan page"/"rescan" in `resolveFromPhrase()`

**File 3c:** `WebCommandExecutorImpl.kt` — Intercept RETRAIN_PAGE before buildScript:

```kotlin
override suspend fun executeWebAction(action: WebAction): WebActionResult {
    if (action.actionType == WebActionType.RETRAIN_PAGE) {
        callback.requestRetrain()
        return WebActionResult(true, "Page retrain requested")
    }
    val script = buildScript(action)
    // ... rest unchanged
}
```

---

## Files Modified

| # | File | Change |
|---|------|--------|
| 1 | `Modules/VoiceOSCore/src/commonMain/.../actions/ActionCoordinator.kt` | Web pre-check + bypass |
| 2 | `apps/avanues/src/main/.../service/VoiceAvanueAccessibilityService.kt` | Register static web commands |
| 3 | `Modules/VoiceOSCore/src/commonMain/.../interfaces/IWebCommandExecutor.kt` | Add RETRAIN_PAGE enum |
| 4 | `Modules/VoiceOSCore/src/commonMain/.../handler/WebCommandHandler.kt` | Map RETRAIN_PAGE |
| 5 | `Modules/WebAvanue/src/commonMain/.../WebCommandExecutorImpl.kt` | Handle RETRAIN_PAGE |

## Dependencies — All Exist, No New Module Deps

- `StaticCommandRegistry.byCategoryAsQuantized()` (StaticCommandRegistry.kt:128)
- `IHandlerRegistry.getHandlersForCategory()` (IHandlerRegistry.kt:73)
- `CommandCategory.BROWSER`, `WEB_GESTURE` (CommandModels.kt)
- `CommandActionType.RETRAIN_PAGE` (CommandActionType.kt:261)
- `BrowserVoiceOSCallback.requestRetrain()` (VoiceAvanueAccessibilityService.kt:253)

## Verification

1. `./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid :Modules:WebAvanue:compileDebugKotlinAndroid :apps:avanues:compileDebugKotlin`
2. Browser active: "go back" → WebCommandHandler → browser back (not SystemHandler)
3. Browser NOT active: "go back" → SystemHandler → Android back (unchanged)
4. "retrain page" → WebCommandHandler → RETRAIN_PAGE → requestRetrain()
