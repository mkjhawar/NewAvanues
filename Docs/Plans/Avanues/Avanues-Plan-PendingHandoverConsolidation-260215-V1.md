# Pending Handover Consolidation Plan

**Date**: 2026-02-15
**Branch**: `IosVoiceOS-Development` (primary), multiple source branches
**Mode**: YOLO + ToT + CoT + Swarm
**Scope**: All 7 pending handover items, prioritized by impact + code proximity

---

## ToT Analysis: Approach Selection

### Branch A: Sequential by Priority (Rejected)
Work each item start-to-finish. Simple but ignores code proximity — jumping between VoiceOSCore, WebAvanue, AvanueUI, and app layer wastes context.

### Branch B: Grouped by Module + Priority (Selected)
Group items by code proximity, order groups by priority. Maximizes context reuse within each group. Allows swarm parallelism between independent groups.

### Branch C: Triage First (Partial)
Check which items are superseded before planning. This is correct for items that may be stale — incorporated into Branch B as a preliminary triage phase.

---

## Phase 0: Triage — Verify Superseded Items (30 min) — COMPLETED 260215

Verified 3 potentially-stale handovers:

| # | Handover | Result | Action Taken |
|---|----------|--------|-------------|
| 5 | AvanueWaterUI unified components | **CONFIRMED SUPERSEDED** | Unified components exist (`AvanueCard`, `AvanueButton`, `AvanueSurface`) with `MaterialMode` switching. Archived to `Archives/Handover/` |
| 6 | Glass transparency fix | **CONFIRMED SUPERSEDED** | Theme v5.1 replaced all glass surfaces. `OceanThemeExtensions.kt` no longer exists. Archived to `Archives/Handover/` |
| 3 | AddressBar crash | **DEFERRED** to Phase 3.1 | Needs device verification |
| 7 | Dashboard | **PARTIAL** | UI exists (2700+ lines HomeScreen), but needs assessment if still relevant |

**Total archived this session: 11 handovers** (9 completed + 2 superseded)

---

## Phase 1: VoiceOSCore — Static Command Handlers (CRITICAL) — 87.5% ALREADY DONE

**Source**: `handover-260211-static-command-dispatch.md`
**Impact**: ~~91/109 voice commands silently fail~~ Only BrowserHandler missing (47 web commands)
**Discovery (260215)**: 7 of 8 handlers ALREADY EXIST with full implementations
**Branch**: Both branches synced via merge `4dea5af0`

### Tasks

#### 1.1 Create Plan Doc
- Path: `docs/plans/VoiceOSCore/VoiceOSCore-Plan-StaticCommandHandlers-260215-V1.md`
- Architecture: 8 new `IHandler` implementations following `AndroidCursorHandler` pattern
- Registration in `AndroidHandlerFactory`

#### 1.2 MediaHandler (7 commands — highest user impact)
- Actions: play, pause, next, prev, stop, vol up/down, mute
- Extract from: `ActionFactory.DynamicMediaAction` + `DynamicVolumeAction`
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/MediaHandler.kt`
- Category: `ActionCategory.MEDIA`

#### 1.3 ScreenHandler (8 commands)
- Actions: brightness up/down, wifi on/off/toggle, bluetooth on/off/toggle, screenshot, flashlight, rotate
- Extract from: `ActionFactory.DynamicWiFiAction`, `DynamicBluetoothAction`, `DynamicNavigationAction`
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/ScreenHandler.kt`
- Category: `ActionCategory.DEVICE`

#### 1.4 TextHandler (7 commands)
- Actions: copy, paste, cut, select all, undo, redo, delete
- Extract from: `ActionFactory.DynamicEditingAction`
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/TextHandler.kt`
- Category: `ActionCategory.INPUT`

#### 1.5 VoiceControlHandler (9 commands — needs service-level access)
- Actions: mute/wake voice, dictation start/stop, show commands/help, numbers on/off/auto
- Special: needs SpeechEngine reference + overlay control
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/VoiceControlHandler.kt`

#### 1.6 InputHandler (2 commands)
- Actions: show/hide keyboard
- Extract from: `ActionFactory.DynamicKeyboardAction`
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/InputHandler.kt`

#### 1.7 AppControlHandler (1 command)
- Actions: close app
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/AppControlHandler.kt`

#### 1.8 ReadingHandler (2 commands)
- Actions: read screen, stop reading
- Uses: existing `VOSAccessibilitySvc.speakText()` TTS engine
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/ReadingHandler.kt`

#### 1.9 BrowserHandler (47 commands)
- Delegates to existing `WebCommandHandler`/`IWebCommandExecutor`
- File: `Modules/VoiceOSCore/src/androidMain/.../handler/BrowserHandler.kt`

#### 1.10 Expand SystemHandler + Register All
- Add VOS synonyms to SystemHandler ("previous screen", "open recents", etc.)
- Register all 8 new handlers in `AndroidHandlerFactory`
- Update `ActionFactory.kt` category routing

#### 1.11 Build + Test
- Compile verification
- Test all 107 VOS commands

**Swarm opportunity**: Tasks 1.2-1.9 (8 handlers) are independent — can be swarm-dispatched.

### Handler Discovery (260215)

| Handler | Commands | Status |
|---------|----------|--------|
| MediaHandler | 13 | DONE |
| ScreenHandler | 20 | DONE |
| TextHandler | 8 | DONE |
| InputHandler | 6 | DONE |
| AppControlHandler | 4 | DONE |
| ReadingHandler | 7 | DONE |
| VoiceControlHandler | 18 | DONE |
| **BrowserHandler** | **47** | **MISSING** |
| **Total** | **76 / 123** | **7/8 handlers** |

All 7 existing handlers are registered in `AndroidHandlerFactory.createHandlers()` (11 total: 4 pre-existing + 7 new). Only BrowserHandler needs to be created — it delegates to existing `WebCommandHandler`/`IWebCommandExecutor`.

**Revised Phase 1 effort: ~1 hr** (just BrowserHandler + integration test)

---

## Phase 2: VoiceOSCore — Overlay Numbering Bugs (HIGH)

**Source**: `handover-260212-overlay-numbering.md`
**Impact**: Duplicate badge numbers + stale overlays during navigation
**Branch**: `VoiceOSCore-KotlinUpdate` (needs cherry-pick)
**Code proximity**: Same module + app service layer as Phase 1

### Tasks

#### 2.1 Bug 1: AVID Truncation Fix
- File: `apps/avanues/.../service/OverlayItemGenerator.kt`
- Change: `.take(40)` → `.take(120)` for text AND contentDescription
- Also: Remove `deduplicateAvids()` and its calls (ordinal instability makes things worse)

#### 2.2 Bug 2: fromScroll Parameter
- File: `apps/avanues/.../service/DynamicCommandGenerator.kt` — add `fromScroll: Boolean = false` param to `processScreen()`
- File: `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt` — pass `fromScroll=true` in `onScrollSettled`, `fromScroll=false` in `refreshOverlayBadges`
- Logic: `if (isNewScreen && isTargetApp && !fromScroll)` → clear overlays

#### 2.3 Optionally remove `onInAppNavigation()` callback
- It's ineffective for Fragment-based apps (Gmail, etc.)
- The `fromScroll` approach is more reliable

---

## Phase 3: WebAvanue — Browser Scoping + Crash Investigation (MEDIUM)

**Sources**: `handover-260210-2100.md`, `handover-260210-2300.md`
**Branch**: `VoiceOSCore-KotlinUpdate`

### Tasks

#### 3.1 Verify AddressBar Crash Status
- Check if app launches on current branch
- If crash persists: pull `adb logcat` trace
- If resolved: archive handover-260210-2300

#### 3.2 Complete Fix 3: Browser Command Scoping
- Add web command clearing when foreground switches away from browser:
  ```kotlin
  // In onCommandsUpdated():
  if (!isBrowser) BrowserVoiceOSCallback.clearActiveWebPhrases()
  ```
- Cancel `webCommandCollectorJob` in `onDestroy()`

#### 3.3 Web DOM Scraping Persistence (PLAN ONLY)
- Create plan doc for persisting web scraping across sessions
- Investigate `ScrapedWebCommand.sq` table for reuse
- NOT implementing — just the plan document

---

## Phase 4: Validate Superseded Items (LOW)

### 4.1 Verify AvanueWaterUI → Theme v5.1 (handover-260210-1800)
- Confirm unified components exist: `AvanueCard`, `AvanueButton`, `AvanueSurface`
- Confirm `MaterialMode` enum handles Glass/Water/Cupertino/MountainView
- If yes → archive handover, done
- If partial → note gaps

### 4.2 Verify Glass Transparency → Theme v5.1 (handover-260208-glass-fix)
- Confirm surfaces use solid colors, glass modifier is luminance-adaptive
- Check if files referenced (`OceanThemeExtensions.kt`) still exist or were migrated
- If yes → archive handover, done

### 4.3 Verify Dashboard State (handover-260207-dashboard)
- Check if `DashboardViewModel.kt` and `HomeScreen.kt` exist from background agents
- Check if they compile
- Assess: is dashboard feature still relevant given all the changes since?
- Create status report — likely needs fresh approach

---

## Estimated Effort (Revised 260215)

| Phase | Original | Revised | Status |
|-------|----------|---------|--------|
| 0 (Triage) | 30 min | 30 min | **DONE** |
| 1 (Handlers) | 4-5 hrs | **1 hr** (only BrowserHandler) | 87.5% done |
| 2 (Overlay) | 1 hr | 1 hr | Pending |
| 3 (WebAvanue) | 1.5 hrs | 1 hr | Pending |
| 4 (Validate) | 30 min | 15 min | Partially done |
| **Total** | **7.5-8 hrs** | **~3.5 hrs remaining** | |

## Swarm Dispatch Plan

**Parallel Group A** (Phase 1, tasks 1.2-1.9): 8 handler agents
**Parallel Group B** (Phase 0 + Phase 4): triage/validation agents
**Sequential**: Phase 2 (overlay), Phase 3 (WebAvanue)

## Branch Strategy (Updated 260215)

All work on `IosVoiceOS-Development`. Branches are now synced:
- `IosVoiceOS-Development` → `VoiceOSCore-KotlinUpdate` via merge `4dea5af0` + `44422099`
- Both pushed to origin
- No more cherry-pick needed — future sync is just `git merge IosVoiceOS-Development` from KotlinUpdate

---

## Summary: Prioritized Task Order

1. **Phase 0**: Triage superseded items (quick wins — archive 2-3 handovers)
2. **Phase 1**: Static command handlers (CRITICAL — 91 failing commands)
3. **Phase 2**: Overlay numbering bugs (HIGH — user-facing visual defect)
4. **Phase 3**: WebAvanue browser scoping (MEDIUM)
5. **Phase 4**: Validate remaining items (LOW — likely archive)
