# Chapter 88: Avanues Consolidated App Architecture

## Overview

The Avanues app consolidates what were previously two separate Android apps — **VoiceAvanue** (voice/accessibility dashboard) and **WebAvanue** (full-featured browser) — into a single installable APK with multiple launcher icons. This architecture supports modular expansion: future modules (VoiceCursor, GazeControl, etc.) can each get their own launcher icon and entry point while sharing a single app process, data layer, and service infrastructure.

## Motivation

| Before (Two Apps) | After (One App) |
|---|---|
| VoiceAvanue: voice dashboard + simple WebView | Avanues: voice dashboard + full browser |
| WebAvanue: full browser without voice | Single APK, shared services |
| Duplicate dependencies, separate processes | Unified data layer, single accessibility service |
| RPC bridge needed for cross-app communication | Direct in-process calls |
| Two installs required | One install, two launcher icons |

## App Identity

```
applicationId = "com.augmentalis.avanues"     // Package identity on device
namespace     = "com.augmentalis.voiceavanue" // R class / Kotlin source package
```

The `applicationId` defines what Android sees as the installed app. The `namespace` matches existing Kotlin package paths to avoid mass-refactoring source files. This is standard Android practice — they can differ.

## Dual Launcher Icons via Activity-Alias

Android's `<activity-alias>` mechanism provides multiple launcher entries that all target the same `MainActivity`:

```xml
<!-- The real activity (no LAUNCHER intent) -->
<activity android:name=".MainActivity"
    android:exported="true"
    android:label="Avanues"
    android:launchMode="singleTask" />

<!-- Launcher 1: VoiceAvanue -->
<activity-alias
    android:name=".VoiceAvanueAlias"
    android:targetActivity=".MainActivity"
    android:exported="true"
    android:label="VoiceAvanue">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <meta-data android:name="launch_mode" android:value="voice" />
</activity-alias>

<!-- Launcher 2: WebAvanue -->
<activity-alias
    android:name=".WebAvanueAlias"
    android:targetActivity=".MainActivity"
    android:exported="true"
    android:label="WebAvanue">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <meta-data android:name="launch_mode" android:value="browser" />
</activity-alias>
```

**Key properties:**
- Zero runtime overhead — resolved at install time
- Each alias can have its own icon, label, and metadata
- All share the same `MainActivity` process and state
- `launchMode="singleTask"` ensures tapping either icon brings up the same instance

## Modular Navigation with AvanueMode

```kotlin
enum class AvanueMode(val route: String, val label: String) {
    VOICE("voice_home", "VoiceAvanue"),
    BROWSER("browser", "WebAvanue"),
    SETTINGS("settings", "Settings")
    // Future: CURSOR("cursor", "VoiceCursor"), GAZE("gaze", "GazeControl")
}
```

The `MainActivity` determines which mode to start based on the alias that launched it:

```kotlin
private fun determineLaunchMode(intent: Intent?): AvanueMode {
    val className = intent?.component?.className ?: return AvanueMode.VOICE
    return when {
        className.contains("WebAvanue") -> AvanueMode.BROWSER
        else -> AvanueMode.VOICE
    }
}
```

The `AvanuesApp` composable uses Jetpack Navigation with `AvanueMode.route` as destinations:

```kotlin
@Composable
fun AvanuesApp(startMode: AvanueMode = AvanueMode.VOICE) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startMode.route) {
        composable(AvanueMode.VOICE.route) { HomeScreen(...) }
        composable(AvanueMode.BROWSER.route) { BrowserScreen(...) }
        composable(AvanueMode.SETTINGS.route) { SettingsScreen(...) }
    }
}
```

## Adding a New Module/Launcher

To add a new modular launcher (e.g., VoiceCursor):

1. **Add enum entry:**
   ```kotlin
   CURSOR("cursor", "VoiceCursor")
   ```

2. **Add activity-alias in AndroidManifest.xml:**
   ```xml
   <activity-alias
       android:name=".VoiceCursorAlias"
       android:targetActivity=".MainActivity"
       android:label="VoiceCursor"
       android:icon="@mipmap/ic_cursor_launcher">
       <intent-filter>
           <action android:name="android.intent.action.MAIN" />
           <category android:name="android.intent.category.LAUNCHER" />
       </intent-filter>
       <meta-data android:name="launch_mode" android:value="cursor" />
   </activity-alias>
   ```

3. **Add composable route:**
   ```kotlin
   composable(AvanueMode.CURSOR.route) { CursorScreen(...) }
   ```

4. **Update `determineLaunchMode()`:**
   ```kotlin
   className.contains("VoiceCursor") -> AvanueMode.CURSOR
   ```

## Theme System

The app uses `AvaTheme` from `Modules/AVA/core/Theme` — a KMP Compose theme that wraps `MaterialTheme` with additional design tokens:

```kotlin
AvaTheme {
    // MaterialTheme.colorScheme — ocean blue palette with dark mode + Material You
    // LocalAvaSpacing — standardized spacing tokens
    // LocalAvaElevation — elevation levels
    // LocalAvaBreakpoints — responsive breakpoints
    // GlassmorphicCard, GlassmorphicSurface — special components
}
```

All existing Compose UI (`@Composable` functions using `MaterialTheme.colorScheme`, `MaterialTheme.typography`, etc.) works without modification inside `AvaTheme`.

## Communication Architecture

| Channel | Purpose | Status |
|---------|---------|--------|
| AVU DSL | Plugin communication (.avp text files) | PRIMARY |
| Direct calls | In-process module-to-module | ACTIVE |
| EventBus | Loose-coupled module events | ACTIVE |
| RPC | 3rd-party compiled app integration | DORMANT (on-demand) |

RPC servers exist in VoiceOSCore and WebAvanue modules but are NOT auto-started. They can be enabled via a Settings toggle for legacy or 3rd-party integration scenarios.

## Directory Structure

```
apps/avanues/                              # Consolidated app
├── build.gradle.kts                       # applicationId=com.augmentalis.avanues
├── src/main/
│   ├── AndroidManifest.xml                # Dual activity-alias + services
│   ├── kotlin/com/augmentalis/voiceavanue/
│   │   ├── MainActivity.kt               # AvaTheme + AvanueMode routing
│   │   ├── VoiceAvanueApplication.kt     # App lifecycle, module init
│   │   ├── di/AppModule.kt               # Hilt DI
│   │   ├── service/
│   │   │   ├── VoiceAvanueAccessibilityService.kt  # Abstract bridge
│   │   │   ├── VoiceRecognitionService.kt
│   │   │   ├── CursorOverlayService.kt
│   │   │   ├── RpcServerService.kt       # Dormant
│   │   │   └── BootReceiver.kt
│   │   └── ui/
│   │       ├── home/HomeScreen.kt        # Voice dashboard
│   │       ├── browser/BrowserScreen.kt  # Browser (simple, to be upgraded)
│   │       └── settings/SettingsScreen.kt
│   └── res/
│       ├── mipmap-*/                      # Launcher icons
│       └── xml/                           # Service configs
│
apps/voiceavanue-legacy/                   # Original VoiceAvanue (reference)
android/apps/webavanue-legacy/             # Original WebAvanue (reference)
```

## Legacy Apps

Both original apps are preserved as `-legacy` variants with different `applicationId`s so they can coexist on a device:
- `com.augmentalis.voiceavanue.legacy`
- `com.augmentalis.webavanue.legacy`

These will be removed once the consolidated app is production-stable.

## Key Design Decisions

1. **Kept source namespace as `com.augmentalis.voiceavanue`** — avoids refactoring dozens of files that import the R class. Only the `applicationId` (device identity) changed.

2. **Activity-alias over multiple Activities** — simpler, zero-overhead, all modes share process state. Alternative (separate Activities) would require inter-activity communication.

3. **AvaTheme over VoiceAvanueTheme** — the token-based KMP theme is the project's design system. The old hardcoded theme was deleted.

4. **RPC dormant, not deleted** — existing module code stays, just not auto-started. Available for future 3rd-party integration.
