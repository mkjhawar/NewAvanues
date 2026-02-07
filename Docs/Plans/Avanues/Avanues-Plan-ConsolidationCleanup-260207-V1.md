# Avanues Consolidation Cleanup - Implementation Plan

**Date:** 2026-02-07
**Branch:** 060226-1-consolidation-framework
**Mode:** .yolo (auto-chain)
**Scope:** 4 workstreams, ~18 tasks

---

## Overview

Complete the Avanues consolidation by:
1. Integrating WebAvanue's full BrowserApp (replacing the minimal BrowserScreen.kt stub)
2. Moving CursorOverlayService into VoiceCursor module
3. Removing singleton anti-patterns
4. Creating branded notification icons

**Platforms:** Android only
**Swarm Recommended:** Yes (workstreams 1-4 are independent after Phase 1)

---

## CoT Reasoning

### BrowserApp Integration (Workstream 1 - HIGHEST PRIORITY)

**Problem:** `BrowserScreen.kt` is a disconnected standalone WebView that duplicates basic browser functionality while ignoring the full WebAvanue module (tabs, bookmarks, history, downloads, Voyager nav, WebXR, proper security).

**Solution:** Delete BrowserScreen.kt, wire WebAvanue's `BrowserApp` composable into the Avanues NavHost.

**Key insight - Lifecycle:** BrowserApp has `DisposableEffect(Unit) { onDispose { runExitCleanup() } }` which fires when the composable leaves composition (i.e., when navigating away from browser in NavHost). This would destroy all ViewModels and WebView pool every time the user goes Browser→Home→Browser.

**Fix:** Create `ViewModelHolder` at Activity scope via a Hilt ViewModel wrapper. BrowserApp's internal `remember { ViewModelHolder.create() }` gets bypassed — we pass pre-created ViewModels. OR: Hoist BrowserApp above NavHost with visibility toggling instead of navigation-based composition/decomposition.

**Selected approach:** Hilt ViewModel wrapper at Activity scope. ViewModelHolder survives NavHost navigation. BrowserApp still manages its own Voyager nav internally. BackHandler pops NavHost when Voyager has no back stack.

**Dependency chain:**
1. Add BrowserDatabase + BrowserRepository to Hilt (AppModule)
2. Create BrowserViewModelWrapper (Activity-scoped Hilt ViewModel holding ViewModelHolder)
3. Modify NavHost browser route to use BrowserApp with injected ViewModels
4. Add BackHandler for NavHost pop
5. Delete BrowserScreen.kt

### CursorOverlay → VoiceCursor Module (Workstream 2)

**Problem:** CursorOverlayService + CursorOverlayView are in app layer but belong with VoiceCursor module.

**Solution:**
- Move `CursorOverlayService` + `CursorOverlayView` → `VoiceCursor/src/androidMain/`
- Create `ClickDispatcher` interface in `VoiceCursor/src/commonMain/` for click dispatch abstraction
- App implements `ClickDispatcher` using `AccessibilityService.dispatchGesture()`
- App manifest still declares the service, referencing module class
- String resources: Use hardcoded strings in module (notification text doesn't need localization framework)

**Files to move:**
- `apps/avanues/.../service/CursorOverlayService.kt` → `Modules/VoiceCursor/src/androidMain/.../overlay/CursorOverlayService.kt`
- `CursorOverlayView` (inner class) goes with it
- New: `Modules/VoiceCursor/src/commonMain/.../core/ClickDispatcher.kt`
- New: `apps/avanues/.../service/AccessibilityClickDispatcher.kt`

### Singleton Removal (Workstream 3)

**Analysis:**
| Singleton | Removable? | Reason |
|-----------|-----------|--------|
| `VoiceAvanueApplication.getInstance()` | YES | Callers should use `@ApplicationContext` via Hilt |
| `VoiceAvanueAccessibilityService.getInstance()` | NO | Android manages lifecycle, no DI alternative |
| `CursorOverlayService.getInstance()` | BECOMES UNNECESSARY | When moved to module, use ClickDispatcher interface instead |

**Scope:** Remove `VoiceAvanueApplication.companion.instance` and all callers. Replace with Hilt `@ApplicationContext` injection.

### Notification Icons (Workstream 4)

**Current:** 3 services use deprecated `android.R.drawable.*`
**Fix:** Create 3 vector drawable XMLs in `res/drawable/`:
- `ic_notification_rpc.xml` (hub/server icon for RpcServerService)
- `ic_notification_cursor.xml` (crosshair icon for CursorOverlayService)
- `ic_notification_voice.xml` (mic icon for VoiceRecognitionService)

---

## Phases

### Phase 1: Hilt Wiring for BrowserDatabase (Foundation)
**Must complete before Workstream 1**

| # | Task | File |
|---|------|------|
| 1.1 | Add BrowserDatabase provider using createAndroidDriver() | `AppModule.kt` |
| 1.2 | Add BrowserRepository provider (BrowserRepositoryImpl) | `AppModule.kt` |
| 1.3 | Create BrowserViewModelWrapper (Activity-scoped HiltViewModel wrapping ViewModelHolder) | `ui/browser/BrowserViewModelWrapper.kt` |

### Phase 2: WebAvanue BrowserApp Integration (Workstream 1)

| # | Task | File |
|---|------|------|
| 2.1 | Replace NavHost browser route with BrowserApp composable | `MainActivity.kt` |
| 2.2 | Add BackHandler for NavHost pop when Voyager has no back stack | `MainActivity.kt` |
| 2.3 | Delete BrowserScreen.kt | `ui/browser/BrowserScreen.kt` |
| 2.4 | Revert fix #5 security changes (no longer needed — WebAvanue handles it) | N/A (file deleted) |

### Phase 3: CursorOverlay Module Migration (Workstream 2)

| # | Task | File |
|---|------|------|
| 3.1 | Create ClickDispatcher interface in VoiceCursor commonMain | `Modules/VoiceCursor/src/commonMain/.../core/ClickDispatcher.kt` |
| 3.2 | Move CursorOverlayService + View to VoiceCursor androidMain | `Modules/VoiceCursor/src/androidMain/.../overlay/` |
| 3.3 | Refactor service to accept ClickDispatcher via Intent extras or singleton | `CursorOverlayService.kt` |
| 3.4 | Create AccessibilityClickDispatcher in app layer | `apps/avanues/.../service/AccessibilityClickDispatcher.kt` |
| 3.5 | Update AndroidManifest to reference module class path | `AndroidManifest.xml` |
| 3.6 | Remove old CursorOverlayService from app | `apps/avanues/.../service/CursorOverlayService.kt` |

### Phase 4: Singleton Cleanup (Workstream 3)

| # | Task | File |
|---|------|------|
| 4.1 | Remove VoiceAvanueApplication.companion.instance | `VoiceAvanueApplication.kt` |
| 4.2 | Replace getInstance() callers with @ApplicationContext | Various |

### Phase 5: Notification Icons (Workstream 4)

| # | Task | File |
|---|------|------|
| 5.1 | Create ic_notification_rpc.xml vector drawable | `res/drawable/` |
| 5.2 | Create ic_notification_cursor.xml vector drawable | `res/drawable/` |
| 5.3 | Create ic_notification_voice.xml vector drawable | `res/drawable/` |
| 5.4 | Update all 3 services to use new icons | 3 service files |

---

## Time Estimates

| Phase | Sequential | Parallel (Swarm) |
|-------|-----------|-----------------|
| Phase 1 (Foundation) | 30 min | 30 min (sequential - dependency) |
| Phase 2 (BrowserApp) | 45 min | 45 min (depends on Phase 1) |
| Phase 3 (CursorOverlay) | 60 min | Can run parallel with Phase 2 |
| Phase 4 (Singletons) | 20 min | Can run parallel |
| Phase 5 (Icons) | 15 min | Can run parallel |
| **Total** | **2h 50m** | **~1h 30m** |
| **Savings** | - | **1h 20m (47%)** |

## Swarm Strategy

After Phase 1 completes:
- **Agent A:** Phase 2 (BrowserApp integration)
- **Agent B:** Phase 3 (CursorOverlay migration)
- **Agent C:** Phase 4 + 5 (Singletons + Icons)

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| BrowserApp lifecycle cleanup on NavHost navigation | Activity-scoped Hilt ViewModel for ViewModelHolder |
| Voyager + Jetpack Navigation conflict | BrowserApp manages internal Voyager nav; BackHandler bridges to Jetpack |
| CursorOverlayService move breaks manifest | Update manifest class reference; verify at build time |
| BrowserDatabase migration conflicts with VoiceOSDatabase | Separate database files (browser.db vs voiceos.db) |
