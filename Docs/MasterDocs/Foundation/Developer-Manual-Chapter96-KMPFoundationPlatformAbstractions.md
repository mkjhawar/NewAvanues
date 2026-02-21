# Chapter 96: KMP Foundation Platform Abstractions

**Module:** Foundation (`Modules/Foundation/`)
**Scope:** Cross-platform settings, credentials, file system, permissions
**Version:** 1.0 (Phase 1+2+3 Complete)
**Last Updated:** 2026-02-15

---

## 1. Overview

The Foundation KMP module provides a set of cross-platform interfaces and implementations that abstract away platform-specific persistence and system APIs. This allows shared KMP code (VoiceOSCore, WebAvanue, etc.) to read/write settings, store credentials securely, perform file I/O, and check permissions without importing any platform-specific code.

### Architecture Layers

```
┌─────────────────────────────────────────────────┐
│  App Layer (Android: Hilt DI, iOS: Swift/Koin)  │
│  Provides concrete instances to ViewModels/etc. │
└───────────────────────┬─────────────────────────┘
                        │ injects
┌───────────────────────▼─────────────────────────┐
│  Foundation commonMain Interfaces               │
│  ISettingsStore<T>, ICredentialStore,            │
│  IFileSystem, IPermissionChecker                 │
└───────────────────────┬─────────────────────────┘
                        │ implemented by
┌───────────────────────▼─────────────────────────┐
│  Platform Implementations                        │
│  Android: DataStore + EncryptedSharedPrefs       │
│  iOS:     NSUserDefaults + Keychain              │
│  Desktop: java.util.prefs + java.nio.file        │
└─────────────────────────────────────────────────┘
```

---

## 2. Interfaces

### 2.1 ISettingsStore<T>

**Location:** `commonMain/.../settings/ISettingsStore.kt`

Reactive settings persistence. Emits current settings via Flow, provides atomic updates.

```kotlin
interface ISettingsStore<T> {
    val settings: Flow<T>           // Observe settings reactively
    suspend fun update(block: (T) -> T) // Atomic read-modify-write
}
```

**Type parameter T:** A settings data class (`AvanuesSettings` or `DeveloperSettings`).

### 2.2 ICredentialStore

**Location:** `commonMain/.../settings/ICredentialStore.kt`

Secure credential storage (encrypted at rest).

```kotlin
interface ICredentialStore {
    suspend fun store(key: String, value: String)
    suspend fun retrieve(key: String): String?
    suspend fun delete(key: String)
    suspend fun hasCredential(key: String): Boolean
}
```

### 2.3 IFileSystem

**Location:** `commonMain/.../platform/IFileSystem.kt`

Cross-platform file I/O with path resolution.

```kotlin
interface IFileSystem {
    fun getExternalStoragePath(): String?
    fun getDocumentsPath(): String
    fun getAppFilesPath(): String
    fun exists(path: String): Boolean
    suspend fun readText(path: String): String
    suspend fun writeText(path: String, content: String)
    suspend fun delete(path: String): Boolean
    suspend fun listFiles(directoryPath: String): List<String>
    suspend fun createDirectories(path: String): Boolean
}
```

### 2.4 IPermissionChecker

**Location:** `commonMain/.../platform/IPermissionChecker.kt`

Permission querying for KMP shared code.

```kotlin
interface IPermissionChecker {
    suspend fun hasPermission(permission: String): Boolean
    suspend fun requestPermission(permission: String): Boolean
    fun isAccessibilityEnabled(): Boolean
    fun canDrawOverlays(): Boolean
}
```

---

## 3. Settings Data Models

### 3.1 AvanuesSettings

**Location:** `commonMain/.../settings/models/AvanuesSettings.kt`

25+ fields covering cursor, voice, theme v5.1, VOS sync. Key defaults:

| Field | Default | Category |
|-------|---------|----------|
| `themePalette` | `"HYDRA"` | Theme v5.1 |
| `themeStyle` | `"Water"` | Theme v5.1 |
| `themeAppearance` | `"Auto"` | Theme v5.1 |
| `cursorSize` | `48` | Cursor |
| `voiceLocale` | `"en-US"` | Voice |
| `vosSftpPort` | `22` | VOS Sync |

### 3.2 DeveloperSettings

**Location:** `commonMain/.../settings/models/DeveloperSettings.kt`

16 fields for developer/debug configuration (STT timing, verbosity, engine selection).

### 3.3 SettingsKeys

**Location:** `commonMain/.../settings/SettingsKeys.kt`

All persistence key strings as constants. Platform implementations MUST use these keys for consistency.

---

## 4. SettingsCodec Pattern

**Location:** `commonMain/.../settings/SettingsCodec.kt`

The codec pattern eliminates N-platform field mapping duplication. Each settings model defines its field mapping ONCE; platform stores use the codec.

```kotlin
interface SettingsCodec<T> {
    val defaultValue: T
    fun decode(reader: PreferenceReader): T
    fun encode(value: T, writer: PreferenceWriter)
}
```

**PreferenceReader/PreferenceWriter** abstract typed get/put operations. Each platform implements these (6-7 methods) wrapping its native API.

### Available Codecs

| Codec | Data Class | Migration |
|-------|-----------|-----------|
| `AvanuesSettingsCodec` | `AvanuesSettings` | Theme v5.1 (variant → palette + style) |
| `DeveloperSettingsCodec` | `DeveloperSettings` | None |

### How It Works

```
AvanuesSettingsCodec.decode(reader)
    → reader.getString(SettingsKeys.THEME_PALETTE, "HYDRA")
    → reader.getBoolean(SettingsKeys.CURSOR_ENABLED, false)
    → ... 25+ fields ...
    → returns AvanuesSettings(...)

AvanuesSettingsCodec.encode(settings, writer)
    → writer.putString(SettingsKeys.THEME_PALETTE, settings.themePalette)
    → writer.putBoolean(SettingsKeys.CURSOR_ENABLED, settings.cursorEnabled)
    → ... 25+ fields ...
```

---

## 5. Platform Implementations

### 5.1 Android (App Layer)

| Class | Backing | Dep |
|-------|---------|-----|
| `AvanuesSettingsRepository` | Jetpack DataStore | `@Inject` via Hilt |
| `DeveloperPreferencesRepository` | Jetpack DataStore | Manual creation |
| `SftpCredentialStore` | EncryptedSharedPreferences (AES256-GCM) | `@Inject` via Hilt |

**Note:** Android implementations live in the app layer (`apps/avanues/`), not in Foundation `androidMain`, because they depend on Android-specific DI (Hilt) and DataStore extensions.

### 5.2 iOS (Foundation iosMain)

| Class | Backing | Notes |
|-------|---------|-------|
| `UserDefaultsSettingsStore<T>` | `NSUserDefaults` | Reactive via `NSUserDefaultsDidChangeNotification`. `update()` uses Mutex for atomic read-modify-write |
| `KeychainCredentialStore` | iOS Keychain (Security framework) | `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` |
| `IosFileSystem` | `NSFileManager` | Documents + Library paths |
| `IosPermissionChecker` | Stubs | Real permission requests via Swift layer |

**Usage in iOS app:**
```kotlin
// In Koin module or manual DI
val settingsStore = UserDefaultsSettingsStore(
    suiteName = null,
    codec = AvanuesSettingsCodec
)
val credentialStore = KeychainCredentialStore()
val fileSystem = IosFileSystem()
```

### 5.3 Desktop/JVM (Foundation desktopMain)

| Class | Backing | Notes |
|-------|---------|-------|
| `JavaPreferencesSettingsStore<T>` | `java.util.prefs.Preferences` | Reactive via `PreferenceChangeListener` |
| `DesktopCredentialStore` | `java.util.prefs.Preferences` + AES-256-GCM | Machine-derived key at `~/.avanues/credential.key`; ciphertext = [12B IV][GCM ciphertext+tag] → Base64 |
| `DesktopFileSystem` | `java.nio.file` | User home + Documents + ~/.avanues |
| `DesktopPermissionChecker` | All `true` | Desktop has no runtime permission model |

**Usage in Desktop app:**
```kotlin
val settingsStore = JavaPreferencesSettingsStore(
    nodePath = "/com/augmentalis/avanues/settings",
    codec = AvanuesSettingsCodec
)
```

---

## 6. Adding New Settings

To add a new settings field across all platforms:

1. **Add field** to `AvanuesSettings` or `DeveloperSettings` data class (with default value)
2. **Add key** to `SettingsKeys`
3. **Add mapping** to the appropriate codec (`AvanuesSettingsCodec` or `DeveloperSettingsCodec`) — both `decode()` and `encode()`
4. **Android only:** Update `readFromPreferences()` and `writeToPreferences()` in the Repository class
5. **iOS/Desktop:** Automatic — codecs handle the mapping

---

## 7. File Map

```
Modules/Foundation/src/
  commonMain/kotlin/com/augmentalis/foundation/
    settings/
      ISettingsStore.kt           # Interface
      ICredentialStore.kt         # Interface
      SettingsCodec.kt            # PreferenceReader/Writer + SettingsCodec<T>
      AvanuesSettingsCodec.kt     # Codec for AvanuesSettings
      DeveloperSettingsCodec.kt   # Codec for DeveloperSettings
      SettingsKeys.kt             # All key constants
      SettingsMigration.kt        # Theme v5.1 migration
      models/
        AvanuesSettings.kt        # Data class
        DeveloperSettings.kt      # Data class
    platform/
      IFileSystem.kt              # Interface
      IPermissionChecker.kt       # Interface

  androidMain/                    # (empty — Android impls in app layer)

  iosMain/kotlin/com/augmentalis/foundation/
    settings/
      UserDefaultsSettingsStore.kt   # ISettingsStore<T> via NSUserDefaults
      KeychainCredentialStore.kt     # ICredentialStore via Keychain
    platform/
      IosFileSystem.kt              # IFileSystem via NSFileManager
      IosPermissionChecker.kt       # IPermissionChecker (bridge)

  desktopMain/kotlin/com/augmentalis/foundation/
    settings/
      JavaPreferencesSettingsStore.kt  # ISettingsStore<T> via java.util.prefs
      DesktopCredentialStore.kt        # ICredentialStore via Preferences+AES-256-GCM
    platform/
      DesktopFileSystem.kt            # IFileSystem via java.nio.file
      DesktopPermissionChecker.kt     # IPermissionChecker (all true)
```

---

## 8. Build Configuration

The Foundation module's `build.gradle.kts` targets:

| Target | Source Set | Always Compiled? |
|--------|-----------|-----------------|
| Android | `androidMain` | Yes |
| Desktop (JVM) | `desktopMain` | Yes |
| iOS (x64/arm64/sim) | `iosMain` | Conditional (`kotlin.mpp.enableNativeTargets=true` or iOS task) |

**Dependencies:** Only `kotlinx-coroutines-core` (commonMain). No external deps needed for iOS (platform.Foundation, platform.Security are Kotlin/Native stdlib) or Desktop (JDK stdlib).

---

## 9. Commit History

| Commit | Description | Phase |
|--------|-------------|-------|
| `4548bbbc` | Extract settings models to Foundation commonMain | Phase 1 |
| `0c4251e0` | Fix imports after extraction | Phase 1 |
| `0e6eed08` | Disable KSP2 for KMP type resolution | Phase 1 |
| `260e1287` | Android ISettingsStore/ICredentialStore implementations | Phase 2 |
| `04cbea5e` | IFileSystem + IPermissionChecker interfaces | Phase 2 |
| `69104fca` | Smart-cast fix for nullable properties | Phase 2 |
| `4ba761dc` | iOS + Desktop implementations + SettingsCodec | Phase 3 |
