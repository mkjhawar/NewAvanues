# Foundation Phase 2: Android Interface Wiring

**Date**: 2026-02-15
**Branch**: `IosVoiceOS-Development`
**Status**: Complete, build verified
**Commits**: `260e1287`, `04cbea5e`, `69104fca`

## Summary

Wired existing Android implementations to Foundation KMP interfaces, and created two new platform abstraction interfaces (`IPermissionChecker`, `IFileSystem`). This completes Phase 2 of the KMP Settings Abstraction initiative (Phase 1 was model extraction on 260213).

**See also:** Developer Manual Chapter 96 — KMP Foundation Platform Abstractions

## Changes Made

### 1. ISettingsStore Interface Update
- **File**: `Modules/Foundation/src/commonMain/.../settings/ISettingsStore.kt`
- Changed `fun settings(): Flow<T>` → `val settings: Flow<T>`
- Reason: Kotlin `val` property generates `getSettings()` JVM method, not `settings()`. Using `val` in the interface is more idiomatic and preserves backward compatibility with all existing callers that already access `.settings` as a property.

### 2. AvanuesSettingsRepository → ISettingsStore<AvanuesSettings>
- **File**: `apps/avanues/.../data/AvanuesSettingsRepository.kt`
- Now implements `ISettingsStore<AvanuesSettings>`
- `val settings` → `override val settings`
- Extracted `readFromPreferences(Preferences): AvanuesSettings` (reusable reader)
- Added `writeToPreferences(MutablePreferences, AvanuesSettings)` (reverse mapper)
- Added `override suspend fun update(block)` — atomic read-transform-write via DataStore `edit {}`
- All existing `updateXxx()` methods preserved for efficient single-field writes
- Fixed 2 smart-cast issues: `cursorAccentOverride` and `vosLastSyncTime` are nullable properties from Foundation module — Kotlin requires local val to smart-cast across modules

### 3. DeveloperPreferencesRepository → ISettingsStore<DeveloperSettings>
- **File**: `apps/avanues/.../data/DeveloperPreferences.kt`
- Now implements `ISettingsStore<DeveloperSettings>`
- `val settings` → `override val settings`
- Extracted `readFromPreferences` / `writeToPreferences` methods
- Added `override suspend fun update(block)` for atomic multi-field updates
- Renamed `update(key, value)` → `updateKey(key, value)` to avoid signature ambiguity
- Updated 30 call sites in `DeveloperSettingsViewModel.kt`

### 4. SftpCredentialStore → ICredentialStore
- **File**: `apps/avanues/.../data/SftpCredentialStore.kt`
- Now implements `ICredentialStore` from Foundation
- Added 4 generic interface methods: `store(key, value)`, `retrieve(key)`, `delete(key)`, `hasCredential(key)`
- Existing specific methods (`storePassword`, `getPassword`, etc.) preserved for backward compatibility

### 5. IPermissionChecker (NEW)
- **File**: `Modules/Foundation/src/commonMain/.../platform/IPermissionChecker.kt`
- Methods: `hasPermission(String)`, `requestPermission(String)`, `isAccessibilityEnabled()`, `canDrawOverlays()`
- Android impl: PackageManager + ActivityCompat + Settings.canDrawOverlays
- iOS impl: Info.plist + framework-specific permission APIs

### 6. IFileSystem (NEW)
- **File**: `Modules/Foundation/src/commonMain/.../platform/IFileSystem.kt`
- Methods: `getExternalStoragePath()`, `getDocumentsPath()`, `getAppFilesPath()`, `exists()`, `readText()`, `writeText()`, `delete()`, `listFiles()`, `createDirectories()`
- Android impl: Environment + Context.filesDir
- iOS impl: FileManager + NSSearchPath

## Build Verification
- `./gradlew :apps:avanues:compileDebugKotlin` — SUCCESS
- `./gradlew :Modules:Foundation:compileKotlinMetadata` — SUCCESS

## Design Decisions
- `val` in interface (not `fun`) → preserves property access syntax at all call sites
- `writeToPreferences` writes ALL fields (not diff-based) → simpler, DataStore `edit {}` is already atomic
- Kept individual `updateXxx()` methods alongside `update(block)` → callers that only toggle one setting don't pay for full-object serialization
- `DeveloperPreferencesRepository.update(key, value)` renamed to `updateKey` → avoids JVM method signature confusion with interface `update(block)`

## Next Steps (Phase 3 — DONE)
Phase 3 (iOS + Desktop implementations) was completed same day. See:
- Fix doc: `docs/fixes/Foundation/Foundation-Fix-Phase3PlatformImplementations-260215-V1.md`
- Developer Manual: Chapter 96 — KMP Foundation Platform Abstractions

### Future Work
Per audit doc: `Docs/Analysis/VoiceOSCore/VoiceOSCore-Analysis-KMPMigrationAudit-260213-V1.md`
- Create Android `actual` implementations of `IPermissionChecker`, `IFileSystem`
- Move OverlayStateManager, DynamicCommandGenerator to VoiceOSCore commonMain
- Extract ViewModel logic from UnifiedSettingsViewModel, VosSyncViewModel
