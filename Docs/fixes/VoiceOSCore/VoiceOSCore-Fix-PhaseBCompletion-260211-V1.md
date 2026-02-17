# VoiceOSCore-Fix-PhaseBCompletion-260211-V1

## Overview

Completes VOS Distribution Phase B with 5 remaining items: runtime importer wiring, security hardening, WorkManager background sync, E2E test documentation, and Phase C crowd-sourcing foundation.

**Branch**: `VoiceOSCore-KotlinUpdate`
**Plan**: `docs/plans/VoiceOSCore/VoiceOSCore-Plan-PhaseBCompletion-260211-V1.md`

## Changes

### Phase 1: Runtime Importer Wiring (CRITICAL)
- **VosSyncManager.kt**: Changed constructor `importer` from val to `@Volatile private var _importer`, added `setImporter()` late-binding method
- **VoiceAvanueAccessibilityService.kt**: Added `SyncEntryPoint` @EntryPoint interface, wires `VosFileImporter` to `VosSyncManager` after DB initialization using `CommandDatabase.getInstance().voiceCommandDao()`
- **SyncModule.kt**: Updated comment explaining late-binding pattern

### Phase 2: Security Hardening
- **VosSftpClient.kt**: Added configurable `hostKeyChecking` parameter to `connect()` supporting "no"/"accept-new"/"yes" modes; removed username from logs; masked file paths in progress logs
- **SftpCredentialStore.kt** (NEW): EncryptedSharedPreferences with MasterKey.AES256_GCM for SFTP password and SSH key passphrase storage, with fallback to regular SharedPreferences
- **VosSyncManager.kt**: Passes `hostKeyChecking` through to VosSftpClient for all operations
- **VosSyncViewModel.kt**: Injected `SftpCredentialStore`, updated `buildAuthMode()` to read from encrypted store
- **SystemSettingsProvider.kt**: Added password field with `PasswordVisualTransformation`, host key verification dropdown (No/Accept New/Strict)
- **SettingsComponents.kt**: Added `visualTransformation` parameter to `SettingsTextFieldRow`

### Phase 3: WorkManager Background Sync
- **VosSyncWorker.kt** (NEW): `@HiltWorker` CoroutineWorker with periodic/one-time scheduling, network+battery constraints, exponential backoff (30s, 3 retries)
- **VosSyncViewModel.kt**: Added `updateAutoSync()`, `updateSyncInterval()` methods
- **VosSyncScreen.kt**: Added auto-sync toggle and interval dropdown (1/2/4/8/12/24 hours)

### Phase 4: E2E Test Documentation
- **VoiceOSCore-Analysis-VOSSyncE2ETestPlan-260211-V1.md** (NEW): Docker SFTP test server setup, test matrix (upload/download/sync x auth modes x error conditions)

### Phase 5: Phase C Crowd-Sourcing Foundation
- **PhraseSuggestion.sq** (NEW): SQLDelight table with command_id, phrases, locale, status, source
- **PhraseSuggestionDTO.kt** (NEW): Data transfer object
- **IPhraseSuggestionRepository.kt** (NEW): Repository interface (CRUD + getPendingByLocale + count)
- **SQLDelightPhraseSuggestionRepository.kt** (NEW): SQLDelight implementation
- **VoiceOSDatabaseManager.kt**: Registered `phraseSuggestions` repository
- **PhraseSuggestionDialog.kt** (NEW): Composable dialog for submitting alternative phrases
- **VosSyncScreen.kt**: Added suggestions section with pending count and export button
- **VosSyncViewModel.kt**: Added `submitSuggestion()`, `exportSuggestions()` methods

### DataStore Keys (3 new)
- `vos_sftp_host_key_mode: String = "no"` — SSH host key verification mode
- `vos_auto_sync_enabled: Boolean = false` — periodic background sync toggle
- `vos_sync_interval_hours: Int = 4` — sync interval (1-24 hours)

### Dependencies Added
- `androidx.hilt:hilt-work:1.1.0` + `androidx.hilt:hilt-compiler:1.1.0` — @HiltWorker support
- `androidx.security:security-crypto` — EncryptedSharedPreferences

## Files

### New (8)
| File | Purpose |
|------|---------|
| `SftpCredentialStore.kt` | Encrypted SFTP credential storage |
| `VosSyncWorker.kt` | @HiltWorker periodic background sync |
| `PhraseSuggestion.sq` | SQLDelight table for suggestions |
| `PhraseSuggestionDTO.kt` | Data transfer object |
| `IPhraseSuggestionRepository.kt` | Repository interface |
| `SQLDelightPhraseSuggestionRepository.kt` | Repository implementation |
| `PhraseSuggestionDialog.kt` | Suggestion UI dialog |
| `E2E Test Plan doc` | Test procedure documentation |

### Modified (14)
| File | Change |
|------|--------|
| `VosSyncManager.kt` | Late-binding setImporter(), hostKeyChecking passthrough |
| `VosSftpClient.kt` | Configurable StrictHostKeyChecking, log sanitization |
| `VoiceAvanueAccessibilityService.kt` | SyncEntryPoint + importer wiring |
| `SyncModule.kt` | Added IPhraseSuggestionRepository provider |
| `SettingsModule.kt` | Passes SftpCredentialStore to SystemSettingsProvider |
| `VosSyncViewModel.kt` | Credential store, periodic sync, suggestions |
| `VosSyncScreen.kt` | Auto-sync toggle, suggestions section |
| `SystemSettingsProvider.kt` | Password field, host key mode dropdown |
| `AvanuesSettingsRepository.kt` | 3 new DataStore keys + update methods |
| `SettingsComponents.kt` | visualTransformation parameter |
| `VoiceOSDatabaseManager.kt` | phraseSuggestions repository |
| `build.gradle.kts` (app) | hilt-work, security-crypto deps |
| `libs.versions.toml` | hilt-work-compiler alias |
| `Plan doc` | Phase B completion plan |

## Verification
- Database module: BUILD SUCCESSFUL
- VoiceOSCore module: BUILD SUCCESSFUL
- Full app (compileDebugKotlin): BUILD SUCCESSFUL
- No new errors, only pre-existing deprecation warnings
