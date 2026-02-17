# VoiceOSCore-Fix-VOSSftpSyncPhaseB-260211-V1

## Summary

Phase B of the VOS Distribution system: SFTP sync layer with developer-facing settings toggle and full sync management screen. Builds on Phase A (committed `e5974fdc`) which established the VOS file split, registry, exporter, and importer.

## What Was Done

### New Files (6)

| File | Location | Purpose |
|------|----------|---------|
| `SyncModels.kt` | `VoiceOSCore/.../vos/sync/` | Sealed classes and data models: SftpResult, SftpAuthMode, RemoteFileInfo, ServerManifest, ManifestEntry, SyncStatus, SyncProgress, SyncResult |
| `VosSftpClient.kt` | `VoiceOSCore/.../vos/sync/` | JSch SFTP wrapper: connect/disconnect, upload/download, listFiles, fetchManifest/uploadManifest. All I/O in Dispatchers.IO, 30s timeout, StrictHostKeyChecking=no for dev |
| `VosSyncManager.kt` | `VoiceOSCore/.../vos/sync/` | Sync orchestrator: testConnection, uploadLocalFiles (queries getNotUploaded, uploads, updates registry), downloadNewFiles (compares manifest hashes, downloads + imports), syncAll. Observable via StateFlow<SyncStatus> |
| `VosSyncViewModel.kt` | `apps/avanues/.../ui/sync/` | @HiltViewModel exposing sync actions (test/upload/download/fullSync), settings flow, registry file list, action messages |
| `VosSyncScreen.kt` | `apps/avanues/.../ui/sync/` | Full management UI: connection status card, progress indicator, 4 action buttons (Test/Upload/Download/Full Sync), file registry list with upload status badges |
| `SyncModule.kt` | `apps/avanues/.../di/` | Hilt DI: provides VosSftpClient (singleton), IVosFileRegistryRepository (from VoiceOSDatabaseManager), VosSyncManager (singleton) |

### Modified Files (10)

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Added `jsch = "0.2.16"` version + `jsch` library (`com.github.mwiede:jsch`) |
| `apps/avanues/build.gradle.kts` | Added `implementation(libs.jsch)` + `implementation(libs.androidx.work.runtime.ktx)` |
| `Modules/VoiceOSCore/build.gradle.kts` | Added `implementation(libs.jsch)` to androidMain |
| `VosFileRegistry.sq` | Added `getNotUploaded` query (WHERE uploaded_at IS NULL AND source = 'local' AND is_active = 1) |
| `IVosFileRegistryRepository.kt` | Added `suspend fun getNotUploaded(): List<VosFileRegistryDTO>` |
| `SQLDelightVosFileRegistryRepository.kt` | Implemented `getNotUploaded()` |
| `AvanuesSettingsRepository.kt` | 7 new DataStore keys: vosSyncEnabled, vosSftpHost/Port/Username/RemotePath/KeyPath, vosLastSyncTime + 7 update methods |
| `SettingsComponents.kt` | Added `SettingsTextFieldRow` composable (editable OutlinedTextField settings row) |
| `SystemSettingsProvider.kt` | Added "Developer: VOS Sync" section: toggle, 5 SFTP config fields, "Manage VOS Sync" navigation row |
| `MainActivity.kt` | Added `VOS_SYNC` route to AvanueMode enum + composable in NavHost |
| `UnifiedSettingsScreen.kt` | Added `onNavigateToVosSync` parameter, wires callback to SystemSettingsProvider |

## Architecture

```
SystemSettingsProvider                    VosSyncScreen
  ├── VOS Sync toggle (on/off)            ├── Connection status
  ├── SFTP Host / Port / Username         ├── Upload All / Download All
  ├── Remote Path                         ├── Full Sync button
  ├── SSH Key File path                   ├── Progress indicator
  └── [Manage VOS Sync →]                 └── File list (registry entries)
        │                                        │
        └───────── navigates to ─────────────────┘
                                                  │
                                           VosSyncViewModel
                                                  │
                                           VosSyncManager
                                           ├── uploadLocalFiles()
                                           ├── downloadNewFiles()
                                           └── testConnection()
                                                  │
                                           VosSftpClient (JSch)
                                           ├── connect / disconnect
                                           ├── upload / download
                                           └── listFiles / fetchManifest
```

## Key Design Decisions

1. **JSch fork**: Using `com.github.mwiede:jsch:0.2.16` (modern fork of JSch, actively maintained, on Maven Central)
2. **StrictHostKeyChecking=no**: Dev-only setting for testing without known_hosts management
3. **Manifest-based sync**: Server-side `manifest.json` tracks all available files with content hashes for delta sync
4. **Importer = null in DI**: The VoiceCommandDaoAdapter is runtime-managed, not Hilt-injectable. Download imports will be wired when the importer is available at runtime
5. **Dev toggle**: VOS Sync hidden behind `vosSyncEnabled` toggle — no UI unless developer enables it
6. **Auth modes**: SSH key (preferred) or password fallback

## DataStore Keys

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `vos_sync_enabled` | Boolean | false | Master toggle for sync section |
| `vos_sftp_host` | String | "" | Server hostname or IP |
| `vos_sftp_port` | Int | 22 | SFTP port |
| `vos_sftp_username` | String | "" | SSH username |
| `vos_sftp_remote_path` | String | "/vos" | Server directory |
| `vos_sftp_key_path` | String | "" | SSH private key file path |
| `vos_last_sync_time` | Long | null | Timestamp of last sync |

## Verification

- [x] Database module compiles with new .sq query
- [x] VoiceOSCore compiles with sync layer (BUILD SUCCESSFUL)
- [x] Full app compiles (BUILD SUCCESSFUL)
- [ ] Settings → System → "VOS Sync" toggle appears
- [ ] Enable toggle → SFTP config fields appear
- [ ] "Manage VOS Sync" → navigates to sync screen
- [ ] "Test Connection" with valid SFTP server → success
- [ ] "Upload All" → files appear on server
- [ ] Place .vos on server → "Download All" → imported to DB

## Branch

`VoiceOSCore-KotlinUpdate`
