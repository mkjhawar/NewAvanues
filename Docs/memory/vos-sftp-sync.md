# VOS SFTP Sync (Phase B) — 260211

## Overview
SFTP sync layer for distributing VOS files between devices and central server. Hidden behind dev toggle.

## Key Files (6 new)
| File | Location | Purpose |
|------|----------|---------|
| SyncModels.kt | VoiceOSCore/.../vos/sync/ | SftpResult, SftpAuthMode, SyncStatus, SyncProgress, SyncResult, ServerManifest |
| VosSftpClient.kt | VoiceOSCore/.../vos/sync/ | JSch wrapper: connect/upload/download/manifest ops, Dispatchers.IO, 30s timeout |
| VosSyncManager.kt | VoiceOSCore/.../vos/sync/ | Orchestrator: testConnection, uploadLocalFiles, downloadNewFiles, syncAll |
| VosSyncViewModel.kt | apps/avanues/.../ui/sync/ | @HiltViewModel: sync actions, settings flow, registry files |
| VosSyncScreen.kt | apps/avanues/.../ui/sync/ | Full UI: status card, progress, 4 buttons, file list |
| SyncModule.kt | apps/avanues/.../di/ | Hilt: VosSftpClient + VosSyncManager singletons |

## Modified Files (10)
- gradle/libs.versions.toml: jsch 0.2.16
- apps/avanues/build.gradle.kts: jsch + work-runtime
- Modules/VoiceOSCore/build.gradle.kts: jsch
- VosFileRegistry.sq: getNotUploaded query
- IVosFileRegistryRepository.kt + impl: getNotUploaded()
- AvanuesSettingsRepository.kt: 7 VOS sync keys
- SettingsComponents.kt: SettingsTextFieldRow
- SystemSettingsProvider.kt: Dev VOS Sync section
- MainActivity.kt: VOS_SYNC route
- UnifiedSettingsScreen.kt: onNavigateToVosSync plumbing

## DataStore Keys
- vos_sync_enabled (Boolean, false)
- vos_sftp_host (String, "")
- vos_sftp_port (Int, 22)
- vos_sftp_username (String, "")
- vos_sftp_remote_path (String, "/vos")
- vos_sftp_key_path (String, "")
- vos_last_sync_time (Long, null)

## Design Decisions
- JSch fork: com.github.mwiede:jsch:0.2.16 (modern, actively maintained)
- StrictHostKeyChecking=no for dev
- Manifest-based delta sync with SHA-256 content hashes
- Importer=null in Hilt DI (VoiceCommandDaoAdapter is runtime-managed)
- Auth: SSH key preferred, password fallback
- Navigation: AvanueMode.VOS_SYNC, Settings → System → VOS Sync toggle → Manage

## Phase B Completion (commit b4c9e55d)
- **Runtime importer wiring**: VosSyncManager.setImporter() late-binding, wired via @EntryPoint in AccessibilityService
- **Security hardening**: SftpCredentialStore (EncryptedSharedPreferences), configurable hostKeyChecking ("no"/"accept-new"/"yes"), log sanitization
- **WorkManager sync**: VosSyncWorker (@HiltWorker), periodic scheduling (1-24h), network+battery constraints, exponential backoff
- **PhraseSuggestion DB**: SQLDelight table + DTO + repo + dialog (Phase C crowd-sourcing foundation)
- **E2E test doc**: VoiceOSCore-Analysis-VOSSyncE2ETestPlan-260211-V1.md

## Additional DataStore Keys (Phase B completion)
- vos_sftp_host_key_mode (String, "no")
- vos_auto_sync_enabled (Boolean, false)
- vos_sync_interval_hours (Int, 4)

## Additional Dependencies (Phase B completion)
- androidx.hilt:hilt-work:1.1.0 + compiler
- androidx.security:security-crypto

## Docs
- Fix doc (Phase B core): docs/fixes/VoiceOSCore/VoiceOSCore-Fix-VOSSftpSyncPhaseB-260211-V1.md
- Fix doc (Phase B completion): docs/fixes/VoiceOSCore/VoiceOSCore-Fix-PhaseBCompletion-260211-V1.md
- Plan doc: docs/plans/VoiceOSCore/VoiceOSCore-Plan-PhaseBCompletion-260211-V1.md
- Chapter 95 Section 6 updated with actual implementation
