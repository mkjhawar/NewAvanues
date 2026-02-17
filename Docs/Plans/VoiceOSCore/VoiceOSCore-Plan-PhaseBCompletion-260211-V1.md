# VoiceOSCore-Plan-PhaseBCompletion-260211-V1

## Overview

Complete VOS Distribution Phase B with 5 remaining items: runtime importer wiring, security hardening, WorkManager background sync, E2E test documentation, and Phase C crowd-sourcing foundation.

**Platform**: Android
**Branch**: `VoiceOSCore-KotlinUpdate`
**Swarm**: Yes (3 parallel groups)
**Estimated**: 18 tasks

## Architecture Context

Phase B core (commit `5737f371`) established: VosSftpClient, VosSyncManager, VosSyncScreen, SyncModule DI, 7 DataStore keys. The following items complete the system.

---

## Phase 1: Runtime Importer Wiring (CRITICAL)

**Problem**: `SyncModule.kt` passes `importer = null` because `VoiceCommandDaoAdapter` is created inside `CommandDatabase` (lazy singleton), not Hilt-injectable. Downloaded VOS files are saved to disk but never imported into the command DB.

**Solution**: Late-binding setter pattern on `VosSyncManager` — wire the importer in `VoiceAvanueAccessibilityService.onServiceReady()` after DB initialization.

### Tasks

**1.1** Add `setImporter(importer: VosFileImporter)` method to `VosSyncManager`
- File: `VoiceOSCore/.../vos/sync/VosSyncManager.kt`
- Add `private var _importer: VosFileImporter? = importer` (replace val)
- Add public `fun setImporter(importer: VosFileImporter)` setter
- Update `downloadNewFiles()` to use `_importer`

**1.2** Wire importer in `VoiceAvanueAccessibilityService.onServiceReady()`
- File: `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt`
- After DB initialization + CommandManager creation:
  - Create `VosFileImporter(registry, commandDao)`
  - Get `VosSyncManager` from Hilt (inject or EntryPoint)
  - Call `syncManager.setImporter(importer)`
- Use `@EntryPoint` interface to get VosSyncManager from Hilt in Service context

**1.3** Update `SyncModule.kt` comment
- File: `apps/avanues/.../di/SyncModule.kt`
- Update comment on `importer = null` to explain late-binding pattern

---

## Phase 2: Security Hardening (HIGH)

**Problem**: VosSftpClient has dev-only security settings: `StrictHostKeyChecking=no`, plaintext credentials in DataStore, username in logs.

**Solution**: Configurable host key checking, EncryptedSharedPreferences for SFTP credentials, log sanitization.

### Tasks

**2.1** Add configurable `StrictHostKeyChecking` to VosSftpClient
- File: `VoiceOSCore/.../vos/sync/VosSftpClient.kt`
- Add `hostKeyChecking: String = "no"` parameter to `connect()`
- Support values: `"no"` (dev), `"accept-new"` (first-connect trust), `"yes"` (strict)
- Add known_hosts file path parameter for `"yes"` mode
- Update `VosSyncManager` to pass through the setting

**2.2** Create `SftpCredentialStore` with EncryptedSharedPreferences
- New file: `apps/avanues/.../data/SftpCredentialStore.kt`
- Follow WebAvanue `SecureStorage` pattern (MasterKey.AES256_GCM)
- Store: SFTP password, SSH key passphrase
- Methods: `storePassword()`, `getPassword()`, `storePassphrase()`, `getPassphrase()`, `clearAll()`
- Keep non-sensitive config (host, port, username, remote path) in DataStore

**2.3** Integrate `SftpCredentialStore` into `VosSyncViewModel`
- File: `apps/avanues/.../ui/sync/VosSyncViewModel.kt`
- Inject `SftpCredentialStore`
- Update `buildAuthMode()` to read password/passphrase from encrypted store
- Add `SftpAuthMode.Password` field to settings UI (optional, behind toggle)

**2.4** Sanitize logs in VosSftpClient
- File: `VoiceOSCore/.../vos/sync/VosSftpClient.kt`
- Remove username from `Log.i()` messages
- Never log host key fingerprints at INFO level (only DEBUG)
- Mask file paths in progress logs (show filename only, not full path)

**2.5** Add password settings field to SystemSettingsProvider
- File: `apps/avanues/.../ui/settings/providers/SystemSettingsProvider.kt`
- Add password SettingsTextFieldRow (visualTransformation = PasswordVisualTransformation)
- Add DataStore key: `vos_sftp_host_key_mode` (String, default "no")
- Add host key mode dropdown: No / Accept New / Strict

**2.6** Add `SftpCredentialStore` to Hilt DI
- File: `apps/avanues/.../di/SyncModule.kt`
- Provide `SftpCredentialStore` singleton with `@ApplicationContext`

---

## Phase 3: WorkManager Background Sync (MEDIUM)

**Problem**: VOS sync is manual-only (button press). Need periodic background sync when WiFi available.

**Solution**: `@HiltWorker` VosSyncWorker with network constraints, configurable interval, exponential backoff.

### Tasks

**3.1** Create `VosSyncWorker` (@HiltWorker)
- New file: `apps/avanues/.../sync/VosSyncWorker.kt`
- Extend `CoroutineWorker`
- Inject `VosSyncManager`, `AvanuesSettingsRepository` via `@AssistedInject`
- `doWork()`: read settings → buildAuthMode → syncAll() → Result.success/retry/failure
- Constraints: CONNECTED network, battery not low
- Backoff: EXPONENTIAL, 30s initial, max 3 retries

**3.2** Add sync scheduling to `VosSyncViewModel`
- File: `apps/avanues/.../ui/sync/VosSyncViewModel.kt`
- Add `schedulePeriodicSync()` / `cancelPeriodicSync()` methods
- Use `PeriodicWorkRequestBuilder<VosSyncWorker>(interval, TimeUnit.HOURS)`
- Interval configurable: 1, 2, 4, 8 hours (DataStore key: `vos_sync_interval_hours`)
- `enqueueUniquePeriodicWork("vos_sync", KEEP, request)`

**3.3** Add sync schedule toggle to VosSyncScreen
- File: `apps/avanues/.../ui/sync/VosSyncScreen.kt`
- Add "Auto Sync" switch + interval dropdown after action buttons
- Show next scheduled sync time if enabled

**3.4** Add DataStore keys for background sync
- File: `apps/avanues/.../data/AvanuesSettingsRepository.kt`
- Add: `vos_auto_sync_enabled: Boolean = false`, `vos_sync_interval_hours: Int = 4`
- Add update methods

---

## Phase 4: E2E Test Documentation (LOW)

**Problem**: Runtime verification checklist is manual. Need structured test plan.

### Tasks

**4.1** Create E2E test procedure document
- New file: `docs/analysis/VoiceOSCore/VoiceOSCore-Analysis-VOSSyncE2ETestPlan-260211-V1.md`
- Test server setup instructions (Docker SFTP container)
- Test matrix: upload/download/full-sync × auth modes × error conditions
- Expected results for each scenario
- Known edge cases: large files, network interruption, manifest corruption

---

## Phase 5: Phase C Crowd-Sourcing Foundation (FUTURE)

**Problem**: No mechanism for users to suggest alternative phrases for voice commands.

**Solution**: `PhraseSuggestionDialog` triggered from HelpScreen, local SQLDelight storage, VOS export.

### Tasks

**5.1** Add `phrase_suggestions` table to SQLDelight
- File: `Modules/Database/.../PhraseSuggestion.sq`
- Columns: id, command_id, original_phrase, suggested_phrase, locale, created_at, status (pending/approved/rejected), source (user/community)

**5.2** Create `PhraseSuggestionRepository` interface + impl
- Files: `Modules/Database/.../repositories/`
- CRUD ops + `getPendingByLocale()`, `getApprovedForCommand()`

**5.3** Create `PhraseSuggestionDialog` composable
- New file: `apps/avanues/.../ui/help/PhraseSuggestionDialog.kt`
- Shows original command phrase, text field for suggestion, locale selector
- Submit saves to local DB
- Follows AvanueTheme/SpatialVoice design

**5.4** Wire long-press in Help screen
- Find HelpScreen composable, add long-press gesture on command items
- Long-press → `PhraseSuggestionDialog(commandId, originalPhrase, locale)`

**5.5** Create suggestion export utility
- New file: `VoiceOSCore/.../vos/PhraseSuggestionExporter.kt`
- Export pending suggestions as JSON for review
- Format compatible with VOS seed file structure

**5.6** Add "Suggestions" section to VosSyncScreen
- Show pending suggestion count
- "Export Suggestions" button → saves JSON to Downloads/

---

## Execution Order (Swarm Groups)

```
Group A (Parallel):     Group B (Parallel):     Group C (After A+B):
├── Phase 1 (Importer)  ├── Phase 3 (WorkMgr)   ├── Phase 4 (E2E docs)
└── Phase 2 (Security)  └── Phase 5 (Crowd)     └── Build verify + commit
```

- **Group A**: Phases 1+2 touch VosSyncManager/VosSftpClient — same code proximity
- **Group B**: Phases 3+5 are independent features
- **Group C**: After everything compiles, document + commit

## Files Summary

### New (7)
| File | Purpose |
|------|---------|
| `SftpCredentialStore.kt` | Encrypted SFTP credential storage |
| `VosSyncWorker.kt` | @HiltWorker periodic background sync |
| `PhraseSuggestion.sq` | SQLDelight table for suggestions |
| `IPhraseSuggestionRepository.kt` | Repository interface |
| `SQLDelightPhraseSuggestionRepository.kt` | Repository impl |
| `PhraseSuggestionDialog.kt` | Suggestion UI dialog |
| `PhraseSuggestionExporter.kt` | JSON export utility |

### Modified (12)
| File | Change |
|------|--------|
| `VosSyncManager.kt` | Late-binding `setImporter()`, hostKeyChecking passthrough |
| `VosSftpClient.kt` | Configurable StrictHostKeyChecking, log sanitization |
| `VoiceAvanueAccessibilityService.kt` | Wire importer at runtime |
| `SyncModule.kt` | Provide SftpCredentialStore, update comments |
| `VosSyncViewModel.kt` | Inject credential store, schedule sync methods |
| `VosSyncScreen.kt` | Auto-sync toggle, suggestion section |
| `SystemSettingsProvider.kt` | Password field, host key mode dropdown |
| `AvanuesSettingsRepository.kt` | 3 new keys (auto_sync, interval, host_key_mode) |
| `VoiceOSDatabaseManager.kt` | Register PhraseSuggestion table |
| Help screen composable | Long-press gesture for suggestions |
| `libs.versions.toml` | hilt-work dependency if missing |
| `build.gradle.kts` (app) | hilt-work compiler if missing |

## Verification
1. Build: `./gradlew :apps:avanues:compileDebugKotlin`
2. Importer wired: downloaded VOS files import into command DB
3. Security: credentials stored in EncryptedSharedPreferences
4. Background sync: WorkManager schedules periodic work
5. Suggestions: long-press → dialog → saves to DB → export works
