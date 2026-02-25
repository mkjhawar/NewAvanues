# Wave 4 Day 1 — Master Analysis Entry

**Session:** 260222
**Reviewer:** code-reviewer agent
**Scope:** `apps/avanues/` — Avanues Consolidated App (48 kt files)

---

## Module Reviewed

**`apps/avanues/`** — Primary consolidated Android app combining VoiceAvanue + WebAvanue.

- applicationId: `com.augmentalis.avanues`
- namespace: `com.augmentalis.voiceavanue`
- Hilt DI, Compose, WorkManager, DataStore, KMP module bridge

Full quality report: `docs/reviews/AvanuesApp-Review-QualityAnalysis-260222-V1.md`

---

## Finding Counts

| Severity | Count |
|----------|-------|
| P0 (Critical) | 4 |
| P1 (High) | 5 |
| P2 (Medium/Low) | 10 |
| **Total** | **19** |

---

## P0 Summary (fix before any release)

1. **`BootReceiver` never fires** — `exported="false"` on a system broadcast receiver. Auto-start on boot is silently broken.
2. **Accessibility settings link is broken** — `android:settingsActivity` references `com.augmentalis.avaunified.ui.settings.SettingsActivity` (wrong package). The Android accessibility settings will show a broken link.
3. **Dual launcher icons missing** — No `<activity-alias>` entries in manifest. The core feature of the consolidation (VoiceAvanue + WebAvanue separate launcher icons) is not wired.
4. **`HiltWorkerFactory` not configured** — `VosSyncWorker` uses `@HiltWorker` but `WorkManager` is not initialized with `HiltWorkerFactory`. All background SFTP sync will fail with `IllegalStateException` at runtime.

## P1 Summary (fix before public beta)

1. **`RpcServerService` is a dead foreground service** — holds a persistent notification but the RPC servers are never started (no delegate implementations, no call site).
2. **`VoiceRecognitionService` misuse** — calls `processVoiceCommand("", 0f)` as a wake signal (no defined contract), and is declared with `foregroundServiceType="microphone"` while not accessing the microphone.
3. **SFTP host key checking defaults to `"no"`** — `AvanuesSettingsRepository` defaults `vosSftpHostKeyMode` to `"no"`, disabling SSH host key verification and enabling MITM on all VOS sync operations.
4. **HTTP/HTTPS deep links not verified** — missing `android:autoVerify="true"`; app will show disambiguation chooser instead of being the primary HTTP handler.
5. **`RpcServerService` has no actual RPC functionality** — `startRpcServers()` in the Application class requires delegate implementations that are never bound or provided anywhere in the app.

---

## Architectural Observations

### What is well-designed
- Hilt multibinding for settings providers (`@IntoSet` + `@JvmSuppressWildcards`) is implemented correctly.
- `AvanueThemeProvider` in `MainActivity` correctly uses the three-axis v5.1 theme system (palette + material mode + appearance).
- `VoiceAvanueAccessibilityService` lifecycle management (coroutine scope, job tracking, cleanup in `onDestroy()`) is thorough.
- `DynamicCommandGenerator` structural change ratio algorithm is clean and well-commented.
- `SftpCredentialStore` uses `EncryptedSharedPreferences` with AES256_GCM with correct fallback handling.
- `BootReceiver` uses `goAsync()` + `CoroutineScope` correctly to do async work in a `BroadcastReceiver`.
- `VosSyncWorker` constraints (network + battery) and retry policy are appropriate.

### What needs architectural attention
- `DeveloperPreferencesRepository` is not Hilt-managed — instantiated directly in two places, creating two separate instances.
- `VoiceAvanueAccessibilityService` directly constructs `AvanuesSettingsRepository` and `VoiceOSDatabaseManager` bypassing Hilt singletons — should use `EntryPointAccessors`.
- `VoiceRecognitionService` is architecturally confused — it is a foreground service lifecycle keeper but calls into the accessibility service to "wake it up", which is not a defined lifecycle contract.
- All 7 content modules in `HubModuleRegistry` route to `AvanueMode.COCKPIT.route` — they currently have no per-module routing, just "open Cockpit". This means clicking PDFAvanue, VideoAvanue, etc. all open the same empty Cockpit.

---

## DRY / Code Smell Observations

- `buildAuthMode()` appears in both `VosSyncWorker.kt` and `VosSyncViewModel.kt` with identical logic — should be extracted to a shared utility or extension on `AvanuesSettings`.
- Notification channel `"ava_rpc_service"` is defined in both `VoiceAvanueApplication.kt` (L83) and `RpcServerService.kt` (L25). Channel creation is idempotent so no crash, but the duplicate constant should be resolved.
- `downloadDir` path construction (`Downloads/commands`) is duplicated in both `VosSyncWorker.doWork()` and `VosSyncViewModel.downloadAll()` / `fullSync()`.

---

## Next Session Recommendations

Priority order:

1. Fix P0-1: `BootReceiver exported="false"` → `true`
2. Fix P0-2: `accessibility_service_config.xml` wrong `settingsActivity` class
3. Fix P0-3: Add `<activity-alias>` entries for VoiceAvanue and WebAvanue icons
4. Fix P0-4: Add `HiltWorkerFactory` initialization to `VoiceAvanueApplication`
5. Fix P1-4: Change default `vosSftpHostKeyMode` from `"no"` to `"strict"`
6. Fix P2-1: Remove duplicate permission declarations
7. Fix P2-2: Make `DeveloperPreferencesRepository` Hilt-provided
8. Fix P2-4 + P2-5: Use `EntryPointAccessors` in `VoiceAvanueAccessibilityService` for singleton access
9. Address `VoiceRecognitionService` — clarify role or remove
10. Fix debug mode default (`debugMode = true` in `DeveloperPreferences`)
