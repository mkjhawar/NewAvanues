# Avanues Consolidated App — Handover Report

**Date:** 2026-02-06
**Branch:** `claude/060226-avu-dsl-evolution`
**Commit:** `ac2888fa` — `feat(apps): Create consolidated Avanues app with dual launcher icons` (102 files)

---

## What Was Accomplished

### 1. Compile Error Fixes (Prerequisite)

Before the consolidated app could be created, several compile errors across existing apps were resolved.

**VoiceAvanue app:**
- `AvaAccessibilityService` renamed to `VoiceAvanueAccessibilityService` in `MainActivity.kt` and `HomeScreen.kt`

**WebAvanue app — fixed all broken imports from prior refactoring:**
- `feature.commands.*`, `platform.*`, `telemetry.*`, `ui.viewmodel.*`, `ui.util.*`, `security.*`, `ui.screen.theme.*`, `feature.xr.*`, `feature.download.*`, `presentation.*` → root `com.augmentalis.webavanue` package
- `domain.model.*`, `domain.repository.*` → root `com.augmentalis.webavanue` package
- `universal.presentation.*` → root package (test files)
- Exception: `com.augmentalis.webavanue.data.db.BrowserDatabase` stays as-is (SQLDelight generated)

**ActionCoordinator constructor fix:**
- `ActionCoordinator(CommandRegistry())` → `ActionCoordinator(commandRegistry = CommandRegistry())` (named parameter required — first positional param is `IVoiceCommandInterpreter`)

### 2. Avanues Consolidated App (`apps/avanues/`)

Created from `apps/voiceavanue/` as base (has all 13 module dependencies).

- `applicationId = "com.augmentalis.avanues"`, `namespace = "com.augmentalis.voiceavanue"` (namespace kept to match R class)
- Dual launcher icons via `<activity-alias>` pattern:
  - `.VoiceAvanueAlias` → voice dashboard (HomeScreen)
  - `.WebAvanueAlias` → browser (BrowserScreen)
  - Both have `<meta-data android:name="launch_mode" android:value="voice|browser">`
- `AvaTheme` from `Modules/AVA/core/Theme` replaces hardcoded `VoiceAvanueTheme`
- `AvanueMode` enum for modular navigation: VOICE, BROWSER, SETTINGS (extensible for CURSOR, GAZE, etc.)
- `determineLaunchMode(intent)` reads alias class name to route to correct start destination
- RPC dormant (not auto-started), AVU DSL is primary communication

### 3. Legacy Apps Preserved

- `apps/voiceavanue-legacy/` — copy of original VoiceAvanue with `applicationId = "com.augmentalis.voiceavanue.legacy"`
- `android/apps/webavanue-legacy/` — copy of original WebAvanue with `applicationId = "com.augmentalis.webavanue.legacy"`, `minSdk = 29`
- Both compile successfully alongside `apps/avanues`

### 4. settings.gradle.kts Updated

```kotlin
include(":apps:avanues")                    // CONSOLIDATED
include(":apps:voiceavanue-legacy")         // Legacy
include(":android:apps:webavanue-legacy")   // Legacy
```

### 5. Build Verification

All three apps compile: `:apps:avanues`, `:apps:voiceavanue-legacy`, `:android:apps:webavanue-legacy` — BUILD SUCCESSFUL

---

## Key Files Modified/Created

| File | Action |
|------|--------|
| `apps/avanues/` (entire directory) | NEW — consolidated app |
| `apps/avanues/build.gradle.kts` | applicationId=com.augmentalis.avanues |
| `apps/avanues/src/main/AndroidManifest.xml` | Dual activity-alias launchers |
| `apps/avanues/.../MainActivity.kt` | AvaTheme + AvanueMode routing |
| `apps/avanues/.../ui/theme/Theme.kt` | DELETED (replaced by AvaTheme) |
| `apps/avanues/.../VoiceAvanueApplication.kt` | RPC dormant, AVU DSL primary |
| `apps/voiceavanue-legacy/` | NEW — legacy copy |
| `android/apps/webavanue-legacy/` | NEW — legacy copy |
| `settings.gradle.kts` | Updated includes |
| `apps/voiceavanue/.../MainActivity.kt` | Fixed AvaAccessibilityService ref |
| `apps/voiceavanue/.../HomeScreen.kt` | Fixed AvaAccessibilityService ref |
| `android/apps/webavanue/.../WebAvanueApp.kt` | Fixed 7 broken imports |
| `android/apps/webavanue/.../MainActivity.kt` | Fixed 6 broken imports |
| `android/apps/webavanue/.../BrowserApp.kt` | Fixed domain.model + platform imports |
| `android/apps/webavanue/.../Dialogs.kt` | Fixed domain.model import |
| `android/apps/webavanue/.../DatabaseMigrationHelper.kt` | Fixed platform import |

---

## Next Items to Tackle

### HIGH PRIORITY

**1. Integrate Full BrowserApp into Avanues**
- Current: `apps/avanues/.../ui/browser/BrowserScreen.kt` is still the **simple WebView** (230 lines)
- Needed: Replace with full `BrowserApp` composable from `android/apps/webavanue/.../BrowserApp.kt` (tabs, bookmarks, history, downloads, XR)
- Approach: Create `BrowserRepository` provider in DI, wrap `BrowserApp(repository)` composable
- Files: `apps/avanues/.../ui/browser/BrowserScreen.kt`, `apps/avanues/.../di/AppModule.kt`

**2. Launcher Icons**
- Both aliases currently use the same default icon
- Need distinct icons: mic/voice for VoiceAvanue, globe/web for WebAvanue
- Can use adaptive icon XML with different foreground drawables

**3. Deep Link Intent Handling**
- `ACTION_VIEW` with `http/https` scheme should route to browser mode
- Voice activation intents should route to voice mode

### MEDIUM PRIORITY

**4. Settings Toggle for RPC Server**
- Add "Enable RPC Server (for 3rd-party compiled apps)" toggle in SettingsScreen
- Starts/stops RPC server on demand
- Currently RPC code is present but dormant

**5. Cross-Mode Navigation**
- Bottom bar or drawer to switch between VoiceAvanue and WebAvanue within the app
- Currently each alias opens its mode but no in-app switching UI

**6. Remove Original Apps from Build**
- Once consolidated app is stable, remove `apps/voiceavanue/` and `android/apps/webavanue/` from settings.gradle.kts
- Keep `-legacy` copies for reference

### LOW PRIORITY

**7. Module-Level Launcher Extensibility**
- Add VoiceCursor, GazeControl as future `AvanueMode` entries
- Each gets its own `<activity-alias>` in manifest

**8. Delete Legacy Apps**
- After Avanues app is production-stable, remove legacy directories entirely

---

## Architecture Notes

### Activity-Alias Pattern
Zero-cost abstraction — Android framework resolves the alias at install time, no runtime overhead. Each alias points to the same `MainActivity` but carries different metadata that `determineLaunchMode(intent)` reads to select the start destination.

### Namespace vs ApplicationId
- `namespace` (`com.augmentalis.voiceavanue`) generates the R class and is used for internal resource resolution
- `applicationId` (`com.augmentalis.avanues`) is the installed package identity visible to users and the Play Store
- This is a standard Android pattern; they do not need to match

### AvaTheme
KMP Compose theme from `Modules/AVA/core/Theme` providing `MaterialTheme` + `CompositionLocalProvider` for spacing, elevation, breakpoints. All standard `@Composable` UI continues working — no migration needed for existing composables.

### AvanueMode
Extensible enum pattern for adding new modes:
1. Add enum entry to `AvanueMode`
2. Add `<activity-alias>` in `AndroidManifest.xml`
3. Add composable route in `NavHost`

No other changes required — the routing infrastructure handles the rest.

---

## Dependencies and Context

- This work builds on the compile fixes committed in `f6112393` and `bdda9064`
- The AVU DSL evolution plan (see `Docs/VoiceOSCore/Handover/VoiceOSCore-Handover-CompileFixes-AVU-DSL-260206.md`) runs in parallel
- The consolidated app is the deployment target for AVU DSL runtime once interpreter work is complete
