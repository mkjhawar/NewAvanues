# Foundation Fix: Phase 3 Platform Implementations

**Module:** Foundation
**Type:** Fix/Completion
**Date:** 2026-02-15
**Version:** V1
**Branch:** IosVoiceOS-Development + kmpvoiceos-update (synced)
**Commit:** `4ba761dc` (IosVoiceOS-Development), `51982391` (kmpvoiceos-update)

---

## Summary

Completed KMP Settings Abstraction Phase 3: iOS and Desktop platform implementations for all 4 Foundation interfaces.

## What Was Done

### Phase 3 Deliverables (11 files, 809 insertions)

#### CommonMain — Codec Infrastructure (3 files)
| File | Purpose |
|------|---------|
| `SettingsCodec.kt` | Generic `SettingsCodec<T>` interface + `PreferenceReader` / `PreferenceWriter` abstractions |
| `AvanuesSettingsCodec.kt` | Codec for `AvanuesSettings` — maps 25+ fields to SettingsKeys with theme v5.1 migration |
| `DeveloperSettingsCodec.kt` | Codec for `DeveloperSettings` — maps 16 fields to SettingsKeys |

#### Desktop/JVM (4 files)
| File | Backing API | Notes |
|------|------------|-------|
| `DesktopFileSystem.kt` | `java.nio.file.Files` | Home/Documents/.avanues paths, IO dispatcher |
| `DesktopPermissionChecker.kt` | N/A | All methods return `true` (no desktop permission model) |
| `DesktopCredentialStore.kt` | `java.util.prefs.Preferences` | Base64 obfuscation (not cryptographic) |
| `JavaPreferencesSettingsStore.kt` | `java.util.prefs.Preferences` | Generic `ISettingsStore<T>`, reactive via `PreferenceChangeListener` |

#### iOS/Kotlin Native (4 files)
| File | Backing API | Notes |
|------|------------|-------|
| `IosFileSystem.kt` | `NSFileManager` | Documents/Library paths, Default dispatcher |
| `IosPermissionChecker.kt` | N/A | Bridge for KMP code; real requests via Swift layer |
| `KeychainCredentialStore.kt` | iOS Security (Keychain) | `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`, `memScoped` memory mgmt |
| `UserDefaultsSettingsStore.kt` | `NSUserDefaults` | Generic `ISettingsStore<T>`, reactive via `NSUserDefaultsDidChangeNotification` |

## Architecture

```
commonMain/
  ISettingsStore<T>  ──┐
  ICredentialStore   ──┤  Interfaces
  IFileSystem        ──┤
  IPermissionChecker ──┘
  SettingsCodec<T>   ──── Generic read/write mapping
  AvanuesSettingsCodec ── Concrete codec for AvanuesSettings
  DeveloperSettingsCodec ── Concrete codec for DeveloperSettings

androidMain/  (app layer - Phase 2)
  AvanuesSettingsRepository : ISettingsStore<AvanuesSettings>  (DataStore)
  DeveloperPreferencesRepository : ISettingsStore<DeveloperSettings>  (DataStore)
  SftpCredentialStore : ICredentialStore  (EncryptedSharedPreferences)

desktopMain/  (Phase 3)
  JavaPreferencesSettingsStore<T> : ISettingsStore<T>  (java.util.prefs)
  DesktopCredentialStore : ICredentialStore  (java.util.prefs + Base64)
  DesktopFileSystem : IFileSystem  (java.nio.file)
  DesktopPermissionChecker : IPermissionChecker  (stubs)

iosMain/  (Phase 3)
  UserDefaultsSettingsStore<T> : ISettingsStore<T>  (NSUserDefaults)
  KeychainCredentialStore : ICredentialStore  (iOS Keychain)
  IosFileSystem : IFileSystem  (NSFileManager)
  IosPermissionChecker : IPermissionChecker  (bridge to Swift)
```

## Usage Examples

### Desktop
```kotlin
val settingsStore = JavaPreferencesSettingsStore(
    nodePath = "/com/augmentalis/avanues/settings",
    codec = AvanuesSettingsCodec
)
settingsStore.settings.collect { settings ->
    println("Palette: ${settings.themePalette}")
}
```

### iOS
```kotlin
val settingsStore = UserDefaultsSettingsStore(
    suiteName = null, // standard defaults
    codec = AvanuesSettingsCodec
)
val credentialStore = KeychainCredentialStore()
credentialStore.store("sftp_password", "secret123")
```

## Known Limitations

| Platform | Limitation | Mitigation |
|----------|-----------|-----------|
| Desktop | DesktopCredentialStore uses Base64, not encryption | Document; integrate OS keyring in future |
| iOS | IosPermissionChecker returns defaults | Real permission handling done by Swift layer |
| iOS | Keychain API interop requires `@Suppress("UNCHECKED_CAST")` | Toll-free bridging NSDictionary↔CFDictionaryRef is safe |

## Phase Summary

| Phase | Status | Commits |
|-------|--------|---------|
| Phase 1: KMP interface extraction | DONE (260213) | `4548bbbc`, `0c4251e0`, `0e6eed08` |
| Phase 2: Android ISettingsStore/ICredentialStore impl | DONE (260215) | `260e1287`, `04cbea5e`, `69104fca` |
| Phase 3: iOS + Desktop implementations | DONE (260215) | `4ba761dc` |
