# Cockpit Fix: Deferred Review Items — 260224-V1

**Module:** Cockpit + AvanueUI
**Branch:** VoiceOS-1M-SpeechEngine
**Date:** 2026-02-24
**Commits:** `e5ca62e4a`, `414833c30`, `c0efa242f`
**Origin:** 3-agent review after Cockpit UI + Theme System plan (commit `ed69d9fa9`)

---

## Context

After completing the Cockpit UI + Theme System plan, a 3-agent review identified 19 findings. 9 were fixed immediately during the review session. This fix addresses the remaining 10 items across 8 files plus 1 new file, organized in 3 phases by dependency order.

---

## Fixes Applied

### Phase 1: commonMain Fixes (Commit `e5ca62e4a`)

| # | Fix | Priority | File | Change |
|---|-----|----------|------|--------|
| 1.1 | NeumorphicModifier Paint hoisting | P2 | `AvanueUI/.../NeumorphicModifier.kt` | Moved `Paint()` allocation from inside `drawIntoCanvas` lambda to enclosing `drawNeumorphicLayer` scope. Paint is now reused across 8 blur passes per call instead of being allocated each time. |
| 1.2 | ThemeSettingsPanel AvanueCard | P0 | `Cockpit/.../ThemeSettingsPanel.kt` | Replaced `material3.Card` + `CardDefaults` with `AvanueCard`. Moved `clickable` into `onClick` param. Active/inactive tint applied via `Modifier.background()` inside content Column. Theme compliance restored. |
| 1.3 | CockpitViewModel updateFrame helper | P2 | `Cockpit/.../CockpitViewModel.kt` | Extracted `private fun updateFrame(frameId, transform)` centralizing the map-match-copy-save pattern. Refactored 7 methods (`selectFrame`, `moveFrame`, `resizeFrame`, `toggleMinimize`, `toggleMaximize`, `updateFrameContent`, `renameFrame`) into one-liners. Timestamp stamping via `transform(frame).copy(updatedAt = Clock.System.now().toString())`. |
| 1.4 | CockpitViewModel SecureRandom IDs | P3 | `Cockpit/.../CockpitViewModel.kt` | Replaced `kotlin.random.Random.nextLong()` with `kotlin.uuid.Uuid.random()` (Kotlin 2.1.0 stdlib, `@ExperimentalUuidApi`). Uses `toHexString().take(12)` for compact representation. Timestamp prefix retained for sortability. |
| 3.5 | CommandBarState dead mappings | P1 | `Cockpit/.../CommandBarState.kt` | `forContentType()` now maps `"note"`, `"voice_note"`, and `"camera"` to `FRAME_ACTIONS` instead of `NOTE_ACTIONS`/`CAMERA_ACTIONS`. NoteAvanue and PhotoAvanue don't expose undo/redo or flip/capture APIs — showing dead chips violates Rule 1. Enum values retained for future API wiring. |

### Phase 2: CockpitScreenState Refactor (Commit `414833c30`)

| # | Fix | Priority | File | Change |
|---|-----|----------|------|--------|
| 1.5 | CockpitScreenState data class | P2 | `Cockpit/.../CockpitScreenState.kt` (NEW) | Groups 12 read-only state params: `sessionName`, `frames`, `selectedFrameId`, `layoutMode`, `dashboardState`, `availableLayoutModes`, `backgroundScene`, `glassDisplayMode`, `currentPalette`, `currentMaterial`, `currentAppearance`, `currentPresetId`. Callbacks remain as separate lambda params (Compose convention). |
| 2.1 | Apply CockpitScreenState | — | `CockpitScreenContent.kt` + `CockpitScreen.kt` | CockpitScreenContent accepts `state: CockpitScreenState` instead of 12 individual params. All internal references qualified as `state.frames`, `state.layoutMode`, etc. CockpitScreen constructs `CockpitScreenState(...)` from collected StateFlows. Parameter count reduced from ~39 to ~28. |
| 3.1 | CockpitScreen LoggerFactory | P1 | `Cockpit/.../CockpitScreen.kt` | Replaced `android.util.Log` + `TAG` constant with `LoggerFactory.getLogger("CockpitScreen")`. All 3 log calls use lazy lambdas (`logger.d { "..." }`, `logger.w { "..." }`) — prevents string interpolation in release builds. |

### Phase 3: androidMain Platform Fixes (Commit `c0efa242f`)

| # | Fix | Priority | File | Change |
|---|-----|----------|------|--------|
| 3.2 | ContentRenderer Image+Video actions | P1 | `Cockpit/.../ContentRenderer.kt` | **Image:** Added `zoom`/`rotation` state (`mutableFloatStateOf`) + `LaunchedEffect` collecting `contentActionFlow` for `IMAGE_ZOOM_IN/OUT/ROTATE`. Image wrapped in `graphicsLayer { scaleX/Y = zoom; rotationZ = rotation }`. **Video:** Added `isPlaying` state + `LaunchedEffect` for `VIDEO_PLAY_PAUSE` (toggle) and `VIDEO_REWIND` (seek back 10s). `VIDEO_FULLSCREEN` is no-op at content level (layout concern). |
| 3.3 | ContentRenderer Map WebView restriction | P1 | `Cockpit/.../ContentRenderer.kt` | Replaced bare `WebViewClient()` with custom `shouldOverrideUrlLoading` that blocks navigation to non-`openstreetmap.org` hosts: `return !host.endsWith("openstreetmap.org")`. Prevents XSS/redirect attacks from embedded map content. |
| 3.4 | AndroidExternalAppResolver validation | P1 | `Cockpit/.../AndroidExternalAppResolver.kt` | `launchAdjacent()` now validates explicit `activityName` via `PackageManager.getActivityInfo()` before launching. If the Activity is not found or not exported, falls back to `getLaunchIntentForPackage()` instead of crashing with `ActivityNotFoundException`. |

---

## Files Modified

| File | Module | Source Set | Lines Changed |
|------|--------|-----------|---------------|
| `NeumorphicModifier.kt` | AvanueUI | commonMain | +3 / -3 |
| `ThemeSettingsPanel.kt` | Cockpit | commonMain | +8 / -13 |
| `CockpitViewModel.kt` | Cockpit | commonMain | +38 / -69 |
| `CommandBarState.kt` | Cockpit | commonMain | +7 / -3 |
| `CockpitScreenState.kt` (NEW) | Cockpit | commonMain | +44 |
| `CockpitScreenContent.kt` | Cockpit | commonMain | +38 / -47 |
| `CockpitScreen.kt` | Cockpit | androidMain | +24 / -10 |
| `ContentRenderer.kt` | Cockpit | androidMain | +68 / -14 |
| `AndroidExternalAppResolver.kt` | Cockpit | androidMain | +20 / -3 |

**Total:** 9 files (1 new), +250 / -162 lines

---

## Verification

- `./gradlew :Modules:Cockpit:compileDebugKotlinAndroid --rerun-tasks` — **BUILD SUCCESSFUL** (287 tasks, 0 errors)
- Only pre-existing deprecation warnings (PhotoAvanue CameraX, CommandBar Icons.Filled.Undo/Redo)

---

## Developer Manual Updates

- **Chapter 97** (Cockpit SpatialVoice Multi-Window): Updated sections 3 (CommandBarState), 4 (CockpitScreenContent), 7 (CockpitViewModel), 8 (CockpitScreen), 11 (ExternalAppResolver), 12 (Updates)
