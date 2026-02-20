# VoiceOSCore Deep Code Review — Part 1: Handlers + Managers
**Date:** 260220
**Scope:** `Modules/VoiceOSCore/src/androidMain/kotlin/.../handlers/` · `.../managers/hudmanager/` · `.../managers/localizationmanager/` · `.../commandmanager/CommandHandler.kt`
**Reviewer:** Code-Reviewer Agent (Sonnet 4.6)

---

## Summary

This review covers 18 handler files and approximately 22 manager/HUD/localization files. The **handlers/** package is clean and production-quality. The **HUD subsystem** (`managers/hudmanager/`) was imported from an older VOS4 codebase and carries significant technical debt: wrong author attribution, multiple unimplemented stubs, thread-safety violations between `synchronized` blocks and Compose `mutableStateOf`, and zero AVID voice semantics on all interactive UI elements. The **LocalizationManagerActivity** uses raw `MaterialTheme` wrapping instead of `AvanueTheme`. The `NoteCommandHandler` returns success strings without ever calling any real controller implementation. `AICommandHandler` and `CastCommandHandler` always return failures — they are functional stubs.

**Totals: 4 CRITICAL · 13 HIGH · 11 MEDIUM · 6 LOW**

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **CRITICAL** | `managers/hudmanager/HUDManager.kt:100,184-205` | **Thread-safety — mixed synchronization model.** `activeConsumers` is guarded by `synchronized(activeConsumers)`, but `_isActive.value` (Compose `mutableStateOf`) is mutated inside that `synchronized` block. Compose state must only be written from the Main thread; `synchronized` does not guarantee the Main thread. A background caller reaching `registerConsumer()` can write to `_isActive.value` from an arbitrary thread, causing a Compose threading invariant violation that silently corrupts state or crashes on newer API levels. | Move `_isActive.value = true` outside the `synchronized` block and post it to the Main thread via `withContext(Dispatchers.Main)` or `Handler(Looper.getMainLooper()).post { }`. Alternatively, replace the `synchronized` set with a `ConcurrentHashMap` and use `AtomicBoolean` for the active flag, updating Compose state through a coroutine on `Dispatchers.Main`. |
| **CRITICAL** | `managers/hudmanager/spatial/SpatialRenderer.kt:32-33,125-133,261-268` | **Thread-safety — mutable collections accessed from multiple coroutines without synchronization.** `spatialElements: MutableMap` and `renderLayers: MutableMap<RenderLayer, MutableList>` are written by `addSpatialElement()`/`removeSpatialElement()` (called from `spatialScope` coroutines) and also read by `clearAll()` and `getRenderableElements()` which can be called from any thread. No `Mutex` or `ConcurrentHashMap` protects them. Concurrent modification will throw `ConcurrentModificationException` at runtime under realistic usage. | Replace `mutableMapOf` / `mutableListOf` with `ConcurrentHashMap` / `CopyOnWriteArrayList`, or add a `Mutex` and use `mutex.withLock { }` around all access sites. |
| **CRITICAL** | `managers/hudmanager/rendering/HUDRenderer.kt:50-60,179-234` | **Rendering loop accesses `hudElements` list on two threads without consistent synchronization.** `addHUDElement()` and `removeHUDElement()` lock on `synchronized(hudElements)`. But `startRenderLoop()` / `renderFrame()` / `renderHUDElement()` iterate `hudElements` inside a coroutine on `Dispatchers.Default` also using `synchronized(hudElements)` — which is correct in isolation but `renderFrame()` calls `holder.lockCanvas()` which must be on the rendering thread, while `startRendering()` uses `Dispatchers.Default`. This mixes canvas thread expectations with coroutine dispatching. On top of that, `renderQueue` list (lines 53, 134-137) uses two separate `synchronized` calls with different lock objects (`hudElements` vs `renderQueue`), breaking atomicity. | Consolidate the render loop onto a dedicated `Dispatchers.IO` thread or a `HandlerThread`, use a single `ReentrantLock` to guard both `hudElements` and `renderQueue`, and document the threading contract clearly. |
| **CRITICAL** | `managers/localizationmanager/ui/LocalizationManagerActivity.kt:75-86` | **Theme violation — raw `MaterialTheme` wrapping the entire screen.** The activity wraps content in `MaterialTheme(colorScheme = darkColorScheme())` using hardcoded Material3 dark colors. This bypasses the project-mandatory `AvanueTheme` system (v5.1) entirely. All downstream text colors use raw `Color.White` literals with hardcoded alpha rather than `AvanueTheme.colors.*` tokens. | Replace `MaterialTheme(colorScheme = darkColorScheme())` with `AvanueThemeProvider(...)` using the HYDRA palette (default). Replace all `Color.White`, `Color(0xFF0A0E27)`, and `color.copy(alpha = x)` literals with `AvanueTheme.colors.*` tokens. |
| **HIGH** | `handlers/NoteCommandHandler.kt:71-142` | **Handler is a full dispatch stub — no real controller calls.** All 30+ `CommandActionType` branches return `HandlerResult.success("Bold toggled")` etc. without ever calling an `INoteController` implementation. The KDoc says "dispatches to the active INoteController instance via a static holder" but no such holder exists in the file. Voice commands for NoteAvanue silently succeed but do nothing. | Implement the static holder pattern described in the KDoc: `object NoteControllerHolder { var controller: INoteController? = null }`. Each branch must call the real controller method and propagate failure if the controller is null or the operation fails. |
| **HIGH** | `handlers/AICommandHandler.kt:39-48` | **All AI commands always return failure.** `AI_SUMMARIZE`, `AI_CHAT`, `AI_RAG_SEARCH`, `AI_TEACH`, `AI_CLEAR_CONTEXT` unconditionally return `HandlerResult.failure("... requires AI:Chat module integration")`. Users will receive failure feedback on every AI voice command. Unlike `CastCommandHandler`, there is no connection point established — not even a callback object or bridge. | Either connect via a callback registry (following `VoiceControlCallbacks` pattern) or, if the AI module is not ready, suppress these commands from being registered in `AndroidHandlerFactory` so they return `notHandled()` (no feedback) rather than explicit failures. |
| **HIGH** | `handlers/CastCommandHandler.kt:39-48` | **All cast commands always return failure.** Same issue as AICommandHandler — commands are registered and matched but always fail with messages like "No active cast session to stop". Users triggering "stop casting" when nothing is casting get a failure toast rather than a graceful no-op. | Add a static `ICastManager` holder (mirroring `VoiceControlCallbacks`) wired from the RemoteCast module. Check if a session is active before returning failure vs `notHandled()`. |
| **HIGH** | `handlers/TextHandler.kt:105-121` | **"Delete" command erases ALL text rather than the focused word/character.** `performDelete()` calls `ACTION_SET_TEXT` with an empty string, wiping the entire field. The command is named "delete" / "erase" — users expect to delete the last typed word or character, not clear everything. This is a destructive data-loss bug for any text field. | Use `ACTION_SET_SELECTION` + `ACTION_CUT` to delete the selected region, or dispatch a backspace key event `(KeyEvent.KEYCODE_DEL)` for character-level delete. If the intent is to clear the field, rename to "clear field" and add a confirmation. |
| **HIGH** | `managers/hudmanager/HUDManager.kt:363-378` | **IMU orientation data silently dropped — hardcoded yaw/pitch = 0.** In `startIMUIntegration()`, the collected `orientationData` from `imuManager.orientationFlow` is passed to `SpatialRenderer.updateHeadOrientation()` (correct), but the subsequent `voiceUIRenderer?.adjustForHeadMovement()` call constructs `voiceui.hud.OrientationData(yaw = 0f, pitch = 0f, roll = 0f)` ignoring the actual data. Head movement compensation in the renderer stub will always apply zero offsets. | Extract the actual yaw/pitch/roll from the IMU `orientationData` object and pass them to `adjustForHeadMovement`. At minimum, document that this is a known gap with a `// TODO` referencing the IMU data structure. |
| **HIGH** | `managers/hudmanager/core/ContextManager.kt:480-490` | **Location tracking, WiFi scanning, and noise detection are empty stubs called during initialize().** `startLocationTracking()`, `startWifiScanning()`, and `startNoiseDetection()` have empty bodies, yet `analyzeCurrentContext()` uses `wifiNetworks`, `noiseLevel`, and `currentLocation` in its scoring algorithm. The environment detector will always score zero for these signals, degrading detection quality silently. The `noiseLevel` field is initialized to `0f` and never updated. | Implement the three sensor setup functions or document them clearly as Phase 2 with `TODO("Phase 2: Location-based context")`. At a minimum, add a log warning when called so developers know which signals are active. |
| **HIGH** | `managers/hudmanager/rendering/HUDRenderer.kt:504-523` | **Three rendering methods are empty stubs in production code.** `renderNotification()`, `renderControlPanel()`, and `renderGazeIndicator()` each contain only `// Stub implementation`. These are called from the active render loop (`renderHUDElement()` line 289-295) when elements of those types are present. Notifications and control panels will silently render as blank frames. | Implement the rendering logic for each type, or throw a specific exception during development to surface the gap rather than silently rendering blank. |
| **HIGH** | `managers/hudmanager/spatial/SpatialRenderer.kt:247-256` | **`parseOrientationData()` always returns zero-orientation.** The comment says "would parse actual IMU data — placeholder implementation" but it unconditionally returns `HeadOrientation(yaw=0, pitch=0, roll=0)`. This is called from `updateHeadOrientation()` which feeds `updateSpatialStability()` — the entire spatial compensation loop runs on zeroed data regardless of head movement. | Parse the actual IMU `orientationData` type (it comes from `DeviceManager.IMUManager`). Define a concrete parameter type instead of `Any` to make the data contract explicit and avoid silent zeroing. |
| **HIGH** | `managers/hudmanager/HUDManager.kt:517-529` | **`calibrateSpatialMapping()` ignores its `calibrationPoints` parameter.** The function receives calibration points but never uses them — it just hard-codes `trackingQuality = 0.95f` and `isCalibrated = true`. The `@Suppress("UNUSED_PARAMETER")` annotation makes this visible. Any calibration workflow will silently "succeed" with a fake quality score. | Either implement real calibration using the provided points, or remove the method from the public API and replace it with a clear `TODO` documenting the expected algorithm (e.g., homography calculation for a set of reference points). |
| **HIGH** | `managers/hudmanager/HUDManager.kt:644-647` | **`calculateAverageFPS()` always returns 60.0f.** This hardcoded stub feeds `onRenderFrame()` which posts the metric to `_renderingStats` LiveData. Any observer that acts on FPS data (e.g., adaptive quality, telemetry) will always see a false 60 FPS regardless of actual performance. | Compute real FPS by tracking frame timestamps in a ring buffer (or delegate to `HUDRenderer.getCurrentFPS()` which already measures it at line 275 of `VoiceUIStubs.kt`). |
| **MEDIUM** | `managers/hudmanager/ui/HUDSettingsUI.kt:466-468` | **Animations toggle switch is a no-op.** The `VisualSettingsSection` renders a Switch for "Animations" but its `onCheckedChange = { /* Update animations */ }` is a comment-only closure — toggling it has no effect. The setting is also not read from `visual.animations` for the `checked` value — it reads `visual.animations` for display but the change callback discards the value. | Wire `onCheckedChange = { settingsManager.updateSettings { copy(visual = visual.copy(animations = it)) } }` or remove the toggle if the feature is not yet implemented. |
| **MEDIUM** | `managers/hudmanager/ui/HUDSettingsUI.kt` (multiple composables) | **AVID violations — zero voice semantics on any HUD Settings interactive element.** The entire `HUDSettingsScreen` contains `Switch`, `RadioButton`, `FilterChip`, `IconButton`, `Button`, `Slider`, and `clickable` rows with no `Modifier.semantics { contentDescription = "..." }` on any element. The project mandate requires AVID on all interactive elements. This is a settings screen for a voice-first OS — the irony of voice settings having no voice accessibility is significant. | Add `Modifier.semantics { contentDescription = "Voice: toggle HUD display" }` (and equivalent) to every interactive element. For the `Switch` elements in `PrivacyToggle`, `MasterToggle`, `VisualSettingsSection`, and `PerformanceSettingsSection`, each needs a semantics descriptor combining the label and current state. |
| **MEDIUM** | `managers/localizationmanager/ui/LocalizationManagerActivity.kt` (multiple composables) | **AVID violations — all interactive elements in LocalizationManager UI missing voice semantics.** `CurrentLanguageCard` buttons ("Change Language", "Test Speech"), `IconButton` elements in `HeaderSection`, `ActionButton` components in `QuickActionsCard`, `DownloadedLanguageChip` delete buttons, and `LanguageCard` clickable items all have no `contentDescription` or semantics. | Add semantics to every interactive element following the AVID pattern: `Modifier.semantics { contentDescription = "Voice: change language" }` etc. |
| **MEDIUM** | `managers/hudmanager/core/ContextManager.kt:432-462` | **Mode application methods are empty no-ops.** `adjustUIOpacity()`, `enableSilentMode()`, `enableVoiceOnlyMode()`, `enableHandsFreeMode()`, `enableStandardMode()`, `prioritizeTextTranscription()`, `prioritizeNavigation()`, `prioritizeSafetyAlerts()` are all empty bodies. `applyModeSettings()` calls all of them but achieves nothing — mode switching in the HUD does not actually change any behavior. | Each method needs a concrete implementation or a callback mechanism to communicate with `SpatialRenderer`, `VoiceIndicatorSystem`, and the TTS layer. Document which subsystem each method is intended to control. |
| **MEDIUM** | `managers/hudmanager/spatial/SpatialRenderer.kt:596-598` | **Three context query methods are hardcoded stubs.** `hasActiveNotifications()` always returns `false`, `isNavigating()` always returns `false`, `isListeningForVoice()` always returns `true`. These drive `updateContextualVisibility()` which controls what HUD elements are visible in Contextual mode. In practice, Contextual mode will always show voice command indicators and always hide navigation/notifications regardless of actual state. | Connect these to real signals: `hasActiveNotifications()` should query the Android `NotificationManager`, `isNavigating()` should check if a navigation app is in the foreground, `isListeningForVoice()` should read from the `VoiceOSAccessibilityService` listening state. |
| **MEDIUM** | `managers/hudmanager/HUDManager.kt:86-90` | **`hudScope` uses `Dispatchers.Main` with `SupervisorJob` but the scope is never cancelled when `cleanup()` is called.** `cleanup()` at line 606 clears maps and updates state but does NOT cancel `hudScope`. Only `dispose()` at line 670 cancels the scope. If a consumer calls `cleanup()` without `dispose()`, ongoing coroutines (`startContextMonitoring`, `observeSettings`, `startIMUIntegration`) continue running indefinitely, creating a coroutine leak. | Either merge `cleanup()` and `dispose()` into a single `dispose()`, or have `cleanup()` cancel and recreate the scope. Document the lifecycle contract clearly. |
| **MEDIUM** | `managers/hudmanager/HUDManager.kt:403-419` | **`startHUD()` / `stopHUD()` bypass `activeConsumers` tracking.** `registerConsumer()` / `unregisterConsumer()` maintain the consumer tracking set to automatically start/stop HUD services. But `startHUD()` and `stopHUD()` set `_isActive.value` and call `startHUDServices()` / `stopHUDServices()` directly, bypassing the consumer set. A call to `startHUD()` followed by `unregisterConsumer()` with an empty consumer set will immediately stop the HUD the consumer just started, creating a confusing behavioral inconsistency. | Remove `startHUD()` and `stopHUD()` from the public API (or make them internal) and route all start/stop through `registerConsumer()` / `unregisterConsumer()`. |
| **MEDIUM** | `managers/hudmanager/ui/HUDSettingsUI.kt:466-468` | **`VisualSettingsSection` "Animations" Switch reads from `visual.animations` but callback discards value.** The checked state correctly reflects the model (`checked = visual.animations`) but `onCheckedChange = { /* Update animations */ }` is a no-op. This creates a broken toggle — it appears clickable but always snaps back. | Same fix as the MEDIUM issue above: wire the callback properly or remove the feature until it is implemented. |
| **LOW** | Multiple HUD files (HUDManager, SpatialRenderer, ContextManager, VoiceIndicatorSystem, GazeTracker, Enhancer, HUDRenderer, HUDSettingsManager, HUDSettingsUI, ARVisionTheme, HUDSettings) | **Wrong author attribution — "VOS4 Development Team" in all HUD file headers.** Rule 7 (ABSOLUTE): no AI/team attribution in author fields. The correct author per the project rules is "Manoj Jhawar" or the field is omitted. "VOS4 Development Team" implies an external team authored this code. 38+ files are affected (see grep output). | Replace all `Author: VOS4 Development Team` headers with `Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC` (matching the handler file header pattern) or remove the Author field entirely. |
| **LOW** | `managers/hudmanager/ui/ARVisionTheme.kt:65-88` | **`ARVisionTypography` creates new `Typography()` instances on every property access.** Each property (`Display`, `Headline`, `Title`, etc.) calls `androidx.compose.material3.Typography()` which allocates a full Typography object to copy a single text style from it. These are `object` properties so they run once, but the pattern of constructing throwaway instances is wasteful and confusing. | Use `TextStyle(fontWeight = ..., fontSize = ...)` directly without going through a default Typography instance. This removes the M3 dependency from these constants. |
| **LOW** | `managers/hudmanager/rendering/HUDRenderer.kt:260-269` | **`RenderMode.OVERLAY` is referenced in `renderBackgroundEffects()` but is not a member of the `RenderMode` enum.** The rendering enum only defines `SPATIAL_AR`, `OVERLAY`, and `IMMERSIVE` (lines 537-542). However `startRendering()` defaults to `RenderMode.SPATIAL_AR` and `updateModeRendering()` in `VoiceUIStubs.kt` references `HUDMode` not `RenderMode`. Verify that `RenderMode.OVERLAY` properly maps from a HUD mode during rendering initialization. | Confirm the mapping path from `HUDMode` → `RenderMode` and add a conversion function so the render mode is always set correctly when the HUD mode changes. |
| **LOW** | `managers/hudmanager/core/ContextManager.kt:480-490` | **`startLocationTracking()` and `startWifiScanning()` register as `LocationListener` but call `locationManager.removeUpdates(this)` in `dispose()` without ever calling `requestLocationUpdates()`.** Calling `removeUpdates()` on a listener that was never registered is a no-op in the Android API, but it indicates these methods were intended to register and then were left incomplete. | When implementing location tracking, ensure `requestLocationUpdates()` is called in `startLocationTracking()` with appropriate permission checks (ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION). |

---

## Recommendations

Prioritized by impact:

### 1. Fix the three thread-safety violations (CRITICAL — can crash or corrupt state)

- `HUDManager.registerConsumer()`: Move Compose state mutations out of `synchronized` blocks, use `Dispatchers.Main` coroutine instead.
- `SpatialRenderer`: Replace bare `mutableMap`/`mutableList` with `ConcurrentHashMap` / thread-safe collections and add a `Mutex` for operations that require atomicity across both maps.
- `HUDRenderer` render loop: Unify the threading model — either a dedicated render thread or a coroutine with an explicit dispatcher; never mix `synchronized` blocks with coroutine dispatchers.

### 2. Fix the LocalizationManagerActivity theme violation (CRITICAL)

The entire LocalizationManager screen is wrapped in raw `MaterialTheme(colorScheme = darkColorScheme())`. Replace with `AvanueThemeProvider`. This is a zero-tolerance rule violation.

### 3. Wire NoteCommandHandler to a real INoteController (HIGH)

Every note command silently succeeds without doing anything. Define `NoteControllerHolder`, wire it from `NoteAvanue`'s Compose entry point, and add null-check logic that returns `notHandled()` when the controller is absent.

### 4. Fix TextHandler "delete" command — it erases the entire text field (HIGH)

This is a user-data-loss bug. Replace `ACTION_SET_TEXT("")` with a backspace key dispatch or selection-based cut operation.

### 5. Add AVID semantics to all HUD settings and localization UI (MEDIUM — zero-tolerance rule)

HUDSettingsUI.kt and LocalizationManagerActivity.kt contain dozens of interactive elements (Switch, Button, RadioButton, FilterChip, IconButton) with no voice semantics at all. For a voice-first OS, the settings screen for voice must be voice-accessible. Add `Modifier.semantics { contentDescription = "Voice: [action] [label]" }` to every interactive element.

### 6. Resolve real implementation gaps in SpatialRenderer and ContextManager (HIGH)

- `parseOrientationData()` hardcodes zeros — implement real IMU data parsing.
- `startLocationTracking()` / `startWifiScanning()` / `startNoiseDetection()` are empty — implement or clearly document as Phase 2.
- `hasActiveNotifications()` / `isNavigating()` / `isListeningForVoice()` are hardcoded stubs — connect to real Android signals.

### 7. Implement or suppress AI and Cast command handlers (HIGH)

These handlers are registered and match voice commands but always return failure to the user. Either connect them to real module callbacks (follow `VoiceControlCallbacks` pattern) or prevent them from registering in `AndroidHandlerFactory` until the module integrations are complete, so they fall through to `notHandled()` silently.

### 8. Fix author attribution across all HUD/manager files (LOW — project rule)

Replace all 38+ occurrences of `Author: VOS4 Development Team` with `Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC` matching the handler file convention. Affects: HUDManager, SpatialRenderer, VoiceIndicatorSystem, GazeTracker, ContextManager, Enhancer, HUDRenderer, HUDSettingsManager, HUDSettings, HUDSettingsUI, ARVisionTheme, LocalizationManagerActivity, LocalizationViewModel, and 25+ more files.

### 9. Fix the no-op animations toggle in HUDSettingsUI (MEDIUM)

`onCheckedChange = { /* Update animations */ }` makes the toggle appear interactive but discards the value. This is a broken UI control in a shipped settings screen.

### 10. Cleanup `HUDManager` lifecycle (MEDIUM)

Merge `cleanup()` and `dispose()` or document their contract. `cleanup()` does not cancel the coroutine scope, creating a potential coroutine leak. Remove or internalize `startHUD()` / `stopHUD()` to prevent consumers from bypassing the `activeConsumers` tracking.

---

## Files Reviewed

**Handlers (androidMain):**
- `handlers/MediaHandler.kt` — Clean. No issues.
- `handlers/ScreenHandler.kt` — Clean. Thorough API level handling.
- `handlers/TextHandler.kt` — HIGH: Delete command erases full field.
- `handlers/InputHandler.kt` — Minor: `val success = ...` variable at line 49 is computed but unused (only the side-effect of `setShowMode` matters).
- `handlers/VoiceControlHandler.kt` — Clean. Good callback pattern.
- `handlers/ReadingHandler.kt` — Clean. Proper TTS lifecycle.
- `handlers/AppControlHandler.kt` — Clean.
- `handlers/NoteCommandHandler.kt` — HIGH: All branches are success stubs with no real dispatch.
- `handlers/AnnotationCommandHandler.kt` — HIGH (same as Note): All branches are success stubs; no `IAnnotationController` holder exists.
- `handlers/ImageCommandHandler.kt` — HIGH (same): All branches are success stubs.
- `handlers/VideoCommandHandler.kt` — HIGH (same): All branches are success stubs.
- `handlers/CastCommandHandler.kt` — HIGH: All branches return explicit failure.
- `handlers/AICommandHandler.kt` — HIGH: All branches return explicit failure.
- `handlers/CockpitCommandHandler.kt` — HIGH (same as Note/Annotation): All branches are success stubs.

**HUD Manager (androidMain):**
- `managers/hudmanager/HUDManager.kt` — CRITICAL thread-safety, multiple HIGH/MEDIUM issues.
- `managers/hudmanager/spatial/SpatialRenderer.kt` — CRITICAL thread-safety, HIGH stub rendering issues.
- `managers/hudmanager/spatial/VoiceIndicatorSystem.kt` — Acceptable. Minor concern: `displayCommands()` auto-hides after 5 seconds even if `context == PERSISTENT` check is inverted (line 77 checks `context != PERSISTENT` to auto-hide — this is correct).
- `managers/hudmanager/spatial/GazeTracker.kt` — Intentional stub (ML Kit disabled). Properly documented.
- `managers/hudmanager/core/ContextManager.kt` — HIGH: empty stub sensor methods degrade environment detection.
- `managers/hudmanager/accessibility/Enhancer.kt` — MEDIUM: mode methods are no-ops.
- `managers/hudmanager/rendering/HUDRenderer.kt` — CRITICAL thread-safety, HIGH stub render methods.
- `managers/hudmanager/ui/ARVisionTheme.kt` — LOW: Typography allocation pattern.
- `managers/hudmanager/ui/HUDSettingsUI.kt` — CRITICAL theme violation (via LocalizationActivity), MEDIUM AVID violations, MEDIUM broken animations toggle.
- `managers/hudmanager/settings/HUDSettingsManager.kt` — Clean. Solid settings persistence with `kotlinx.serialization`.
- `managers/hudmanager/settings/HUDSettings.kt` — Clean.
- `managers/hudmanager/stubs/VoiceUIStubs.kt` — Acceptable. Well-documented bridge code.

**Localization Manager (androidMain):**
- `managers/localizationmanager/ui/LocalizationManagerActivity.kt` — CRITICAL `MaterialTheme` theme violation, MEDIUM AVID violations throughout.

**CommandManager:**
- `commandmanager/CommandHandler.kt` — Clean interface definition. Good documentation.
