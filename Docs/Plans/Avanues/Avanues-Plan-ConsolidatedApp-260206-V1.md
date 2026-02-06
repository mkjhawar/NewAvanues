# Avanues Consolidated App - Modular Platform with Dual Launcher Icons

> **Plan Location**: Save to `docs/plans/Avanues/Avanues-Plan-ConsolidatedApp-260206-V1.md` as first implementation step

## Context

The project currently has **two separate apps** that should be **one**:
- `apps/voiceavanue/` — unified app with all module dependencies (VoiceOSCore, WebAvanue, VoiceCursor, Gaze, AI, Database, Foundation) but a **simple WebView** browser
- `android/apps/webavanue/` — standalone browser app with full browser features (tabs, bookmarks, history, XR) but **separate** from voice services

The user's intent is a single app called **Avanues** with:
- Two launcher icons: **VoiceAvanue** (voice/accessibility dashboard) and **WebAvanue** (full browser)
- Modular architecture so more modules/icons can be added later
- Use the existing `AvaTheme` (KMP token-based design system from `Modules/AVA/core/Theme`) instead of hardcoded `VoiceAvanueTheme`
- Foundation module (`VoiceAvanue` KMP module) as the shared resource layer

---

## Recommended Approach: Rename + Upgrade + Modularize

Rename `apps/voiceavanue` → `apps/avanues`, upgrade its browser to use the full `BrowserApp` from WebAvanue, add an `activity-alias` for the second launcher icon, and switch to `AvaTheme`. Then deprecate `android/apps/webavanue`.

**Why this approach:**
- `apps/voiceavanue` already has ALL dependencies wired (13 modules, Hilt, services, RPC)
- Adding an `activity-alias` gives a second launcher icon pointing to the same app with a different launch intent
- Replacing `VoiceAvanueTheme` with `AvaTheme` connects to the token-based design system
- Minimal code change, maximum consolidation

---

## Implementation Plan

### Task 0: Fix existing compile errors in both apps (PREREQUISITE)

Both apps have broken imports from a prior refactoring where classes moved packages but imports weren't updated.

**VoiceAvanue app (`apps/voiceavanue/`):**

| File | Line | Issue | Fix |
|------|------|-------|-----|
| `MainActivity.kt` | 31 | `import ...service.AvaAccessibilityService` — class doesn't exist | → `VoiceAvanueAccessibilityService` |
| `MainActivity.kt` | 66 | `AvaAccessibilityService.isEnabled(this)` | → `VoiceAvanueAccessibilityService.isEnabled(this)` |
| `HomeScreen.kt` | 24 | `import ...service.AvaAccessibilityService` | → `VoiceAvanueAccessibilityService` |
| `HomeScreen.kt` | 37 | `AvaAccessibilityService.isEnabled(context)` | → `VoiceAvanueAccessibilityService.isEnabled(context)` |

**WebAvanue app (`android/apps/webavanue/`):**

| File | Line | Issue | Fix |
|------|------|-------|-----|
| `AndroidManifest.xml` | 87 | `com.augmentalis.webavanue.platform.DownloadCompletionReceiver` | → `com.augmentalis.webavanue.DownloadCompletionReceiver` |
| `WebAvanueApp.kt` | 9 | `import ...feature.commands.WebAvanueActionMapper` | → `com.augmentalis.webavanue.WebAvanueActionMapper` |
| `WebAvanueApp.kt` | 18 | `import ...platform.createAndroidDriver` | → `com.augmentalis.webavanue.createAndroidDriver` |
| `WebAvanueApp.kt` | 19 | `import ...platform.DownloadCompletionReceiver` | → `com.augmentalis.webavanue.DownloadCompletionReceiver` |
| `MainActivity.kt` | 18 | `import ...feature.xr.XRManager` | → `com.augmentalis.webavanue.XRManager` |

**Root cause:** Classes were moved to root `com.augmentalis.webavanue` package during module refactoring, and `AvaAccessibilityService` was renamed to `VoiceAvanueAccessibilityService`, but app-level imports were never updated.

### Task 1: Rename app directory and package references

**What:** Rename `apps/voiceavanue/` → `apps/avanues/`

**Files to modify:**
- `apps/avanues/build.gradle.kts` — change `applicationId` to `com.augmentalis.avanues`, update `namespace`
- `settings.gradle.kts` — update include path
- `apps/avanues/src/main/AndroidManifest.xml` — update labels
- String resources — `app_name` → "Avanues"

**Keep package name `com.augmentalis.voiceavanue` internally** to avoid mass refactoring all Kotlin files. Only the `applicationId` and display name change. This is standard Android practice — `applicationId` and Kotlin package can differ.

### Task 2: Add dual launcher icons via activity-alias

**What:** Add an `<activity-alias>` in AndroidManifest for the WebAvanue launcher icon.

**AndroidManifest.xml changes:**
```xml
<!-- Primary: VoiceAvanue (voice dashboard) -->
<activity android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <!-- Remove LAUNCHER from here, move to alias -->
</activity>

<!-- Launcher 1: VoiceAvanue icon -->
<activity-alias
    android:name=".VoiceAvanueAlias"
    android:targetActivity=".MainActivity"
    android:exported="true"
    android:icon="@mipmap/ic_voice_launcher"
    android:label="VoiceAvanue"
    android:roundIcon="@mipmap/ic_voice_launcher_round">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <meta-data android:name="launch_mode" android:value="voice" />
</activity-alias>

<!-- Launcher 2: WebAvanue icon -->
<activity-alias
    android:name=".WebAvanueAlias"
    android:targetActivity=".MainActivity"
    android:exported="true"
    android:icon="@mipmap/ic_web_launcher"
    android:label="WebAvanue"
    android:roundIcon="@mipmap/ic_web_launcher_round">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <meta-data android:name="launch_mode" android:value="browser" />
</activity-alias>
```

**MainActivity change:** Read the alias metadata to determine initial route:
```kotlin
val launchMode = intent?.component?.className?.let { className ->
    if (className.contains("WebAvanue")) "browser" else "voice"
} ?: "voice"

// Pass to NavHost startDestination
```

### Task 3: Replace simple BrowserScreen with full BrowserApp

**What:** Replace `apps/avanues/.../ui/browser/BrowserScreen.kt` (simple WebView, 230 lines) with integration of the full `BrowserApp` composable from `android/apps/webavanue/`.

**Approach:**
- The `BrowserApp` composable from `android/apps/webavanue/src/main/.../BrowserApp.kt` takes a `BrowserRepository` parameter
- Create a `BrowserRepository` provider in Hilt `AppModule` using the WebAvanue module's `BrowserDatabase`
- Replace `BrowserScreen` composable to wrap `BrowserApp(repository)`

**Files:**
- Modify: `apps/avanues/.../ui/browser/BrowserScreen.kt` — wrap `BrowserApp`
- Modify: `apps/avanues/.../di/AppModule.kt` — add `BrowserRepository` + `BrowserDatabase` providers
- Add dependency: `apps/avanues/build.gradle.kts` — ensure `kotlinx.datetime`, `napier`, `security-crypto` from WebAvanue app are included

### Task 4: Switch to AvaTheme

**What:** Replace hardcoded `VoiceAvanueTheme` with KMP `AvaTheme` from `Modules/AVA/core/Theme`.

**Files:**
- Modify: `apps/avanues/.../MainActivity.kt` — `AvaTheme { }` instead of `VoiceAvanueTheme { }`
- Delete: `apps/avanues/.../ui/theme/Theme.kt` (hardcoded theme, 89 lines)
- The `AvaTheme` already provides: token-based colors, typography, shapes, spacing, elevation, glassmorphic components, dark mode, dynamic color (Material You)

### Task 5: Modular navigation structure for future icons

**What:** Refactor `Routes` and `VoiceAvanueApp` composable to support a modular mode system.

```kotlin
// Each module registers its route
enum class AvanueMode(val route: String, val label: String) {
    VOICE("voice_home", "VoiceAvanue"),
    BROWSER("browser", "WebAvanue"),
    SETTINGS("settings", "Settings")
    // Future: CURSOR("cursor", "VoiceCursor"), GAZE("gaze", "GazeControl")
}
```

**Navigation flow:**
- `VoiceAvanueAlias` → `startDestination = AvanueMode.VOICE.route`
- `WebAvanueAlias` → `startDestination = AvanueMode.BROWSER.route`
- Future aliases → their respective `startDestination`

### Task 6: Rename existing apps to *-legacy

**What:** Keep both existing apps running for comparison:
- `apps/voiceavanue/` → `apps/voiceavanue-legacy/`
- `android/apps/webavanue/` → `android/apps/webavanue-legacy/`
- Update `settings.gradle.kts` include paths with `-legacy` suffix
- Both continue to build/run alongside the new `apps/avanues/`

### Task 7: RPC → dormant, AVU DSL is primary

**What:** AVU DSL wire protocol is the primary communication channel. RPC stays in code but is NOT auto-started.

**Changes:**
- `VoiceAvanueApplication.onCreate()` — remove `initializeModules()` RPC auto-start
- RPC server classes stay in VoiceOSCore/WebAvanue modules (not deleted)
- Add a Settings toggle: "Enable RPC Server (for 3rd-party compiled apps)" — starts RPC on demand
- Internal module communication uses direct calls through VoiceAvanue EventBus
- Plugin communication uses AVU DSL interpreter pipeline (`.avp` files)

### Task 8: Create launcher icon resources

**What:** Create placeholder launcher icons for both modes.

- `ic_voice_launcher` — voice/microphone themed icon
- `ic_web_launcher` — globe/browser themed icon
- For now, use existing icons or simple adaptive icon variants
- Proper branded icons can be designed later

### Task 9: Build verification + Commit

---

## Critical Files

| File | Action |
|------|--------|
| `apps/voiceavanue/build.gradle.kts` | Rename dir, change applicationId to `com.augmentalis.avanues` |
| `apps/voiceavanue/src/main/AndroidManifest.xml` | Add activity-aliases, update labels |
| `apps/voiceavanue/.../MainActivity.kt` | Read launch mode from alias, use AvaTheme |
| `apps/voiceavanue/.../ui/browser/BrowserScreen.kt` | Replace with full BrowserApp wrapper |
| `apps/voiceavanue/.../ui/theme/Theme.kt` | Delete (replaced by AvaTheme) |
| `apps/voiceavanue/.../di/AppModule.kt` | Add BrowserRepository provider |
| `settings.gradle.kts` | Update include path |
| `Modules/AVA/core/Theme/...AvaTheme.kt` | Already exists — reuse |
| `android/apps/webavanue/.../BrowserApp.kt` | Reference — this is what we integrate |

## Existing Code to Reuse

| Component | Location |
|-----------|----------|
| `AvaTheme` | `Modules/AVA/core/Theme/src/commonMain/.../AvaTheme.kt` |
| `DesignTokens` | `Modules/AVA/core/Theme/src/commonMain/.../DesignTokens.kt` |
| `GlassmorphicComponents` | `Modules/AVA/core/Theme/src/commonMain/.../GlassmorphicComponents.kt` |
| `BrowserApp` composable | `android/apps/webavanue/.../BrowserApp.kt` |
| `BrowserRepository` | `Modules/WebAvanue/src/commonMain/.../domain/repository/BrowserRepository.kt` |
| `VoiceAvanueApplication` | `apps/voiceavanue/.../VoiceAvanueApplication.kt` — keeps all RPC/module init |
| `VoiceAvanueAccessibilityService` | `apps/voiceavanue/.../service/` — Android framework requires app-level concrete class |

### Note: Accessibility Service Pattern

The abstract `VoiceOSAccessibilityService` → concrete `VoiceAvanueAccessibilityService` pattern is an **Android framework constraint** — the manifest must declare a class in the app's package. This is NOT optional overhead. The app class delegates everything to the module. Zero runtime cost — just method forwarding.

---

## Verification

1. **Build**: `./gradlew :apps:avanues:assembleDebug` compiles
2. **Launcher**: Two icons appear in launcher — "VoiceAvanue" and "WebAvanue"
3. **VoiceAvanue icon**: Opens to voice dashboard (HomeScreen)
4. **WebAvanue icon**: Opens directly to full browser (BrowserApp with tabs, bookmarks)
5. **Theme**: UI uses AvaTheme ocean blue glassmorphic design, not old blue/white
6. **Services**: Accessibility service, voice recognition, RPC servers still functional
7. **Navigation**: Can navigate between voice home and browser within the app
