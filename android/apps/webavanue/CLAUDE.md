# WebAvanue Android App

KMP-based browser application with VoiceOS integration.

---

## Build & Run

| Command | Purpose |
|---------|---------|
| `./gradlew :android:apps:webavanue:assembleDebug` | Build debug APK |
| `./gradlew :android:apps:webavanue:installDebug` | Install on device |
| `./gradlew :android:apps:webavanue:assembleRelease` | Build release APK |

Run from Android Studio: Select `webavanue` run configuration

---

## Architecture

| Component | Path | Purpose |
|-----------|------|---------|
| MainActivity | `app/src/main/kotlin/.../MainActivity.kt` | Entry point |
| WebAvanueApp | `app/src/main/kotlin/.../WebAvanueApp.kt` | Application class |
| DatabaseMigrationHelper | `app/src/main/kotlin/.../DatabaseMigrationHelper.kt` | Encryption migration |
| Universal Module | `Modules/WebAvanue/universal/` | KMP shared code |
| CoreData Module | `Modules/WebAvanue/coredata/` | Database layer |

---

## Key Features

| Feature | Status | Notes |
|---------|--------|-------|
| Database Encryption | ✅ Enabled by default | SQLCipher, auto-migration |
| VoiceOS IPC | ✅ Implemented | Universal IPC v2.0.0 |
| Download Management | ✅ Complete | WiFi-only, custom paths |
| Settings UI | ✅ Complete | Search, validation, presets |
| WebXR Support | ✅ Integrated | XRManager lifecycle |

---

## Database

| Type | Default | Migration |
|------|---------|-----------|
| SQLCipher | Encrypted | Auto-migrates on first launch |
| Path | `webavanue_browser.db` | App-private directory |
| Schema | SQLDelight | `Modules/WebAvanue/coredata/` |

**Migration**: Handled by `DatabaseMigrationHelper` on app startup if needed.

---

## Important Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | App-level build config |
| `AndroidManifest.xml` | Permissions, activities |
| `WebAvanueApp.kt` | Application lifecycle, DI |
| `MainActivity.kt` | Main activity, Compose setup |

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Compose | Material3 | UI framework |
| SQLDelight | Latest | Database |
| Voyager | Latest | Navigation |
| Napier | Latest | Logging |
| Sentry | Disabled (not configured) | Crash reporting |

---

## Security

| Feature | Status |
|---------|--------|
| Database Encryption | ✅ Default enabled |
| SecureStorage (credentials) | ✅ Implemented |
| Certificate Pinning | 🚧 Partial |
| Network Security Config | ✅ Configured |

---

## IPC Integration

**Protocol**: Universal IPC v2.0.0
**Action**: `com.augmentalis.avanues.web.IPC.COMMAND`
**Format**: `VCM:commandId:command:params`

Voice commands routed from VoiceOS to WebAvanue via BroadcastReceiver.

---

## Common Issues

| Issue | Solution |
|-------|----------|
| Build fails | Clean: `./gradlew clean` |
| Database locked | Uninstall app first |
| Compose preview broken | Invalidate caches & restart |
| IPC not working | Check VoiceOS installed |

---

## Testing

| Command | Purpose |
|---------|---------|
| `./gradlew :android:apps:webavanue:testDebugUnitTest` | Run unit tests |
| `./gradlew :android:apps:webavanue:connectedAndroidTest` | Run instrumented tests |

---

## Deployment

| Build Type | Signing | Minify |
|------------|---------|--------|
| Debug | Debug keystore | No |
| Release | Release keystore | Yes (R8) |

---

Inherits: `/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/.claude/CLAUDE.md`

Updated: 2025-12-15
