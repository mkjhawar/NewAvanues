# AvanuesApp Quality Review — 260222 V1

**Scope:** `apps/avanues/src/main/` — 48 Kotlin source files
**Reviewer:** code-reviewer agent
**Branch:** VoiceOS-1M-SpeechEngine
**Date:** 2026-02-22

---

## Summary

The Avanues consolidated app is architecturally sound and well-structured. Hilt DI is correctly wired, the theme system follows v5.1 rules, and the service lifecycle management is generally good. There are however a cluster of concrete bugs and security issues that need addressing before any production release.

---

## DI Setup Analysis

### Hilt Modules

| Module | Location | Component | Notes |
|--------|----------|-----------|-------|
| `AppModule` | `di/AppModule.kt` | `SingletonComponent` | Correct — provides DB, repo, RPC configs |
| `SettingsModule` | `di/SettingsModule.kt` | `SingletonComponent` | `@IntoSet` multibinding with `@JvmSuppressWildcards` — correct |
| `SyncModule` | `di/SyncModule.kt` | `SingletonComponent` | Provides `VosSftpClient`, `VosSyncManager`, repositories |

**`@JvmSuppressWildcards` presence:** `UnifiedSettingsViewModel` uses `Set<@JvmSuppressWildcards ComposableSettingsProvider>` — correct.

**Missing `@Singleton` on `DeveloperPreferencesRepository`:** `DeveloperPreferencesRepository` is instantiated directly (`DeveloperPreferencesRepository(context)`) in both `VoiceAvanueAccessibilityService.onServiceReady()` (L146) and `DeveloperSettingsViewModel` (L31). It is NOT provided by Hilt, which means two independent instances can exist simultaneously, each reading from the same DataStore. This is safe at the DataStore level (DataStore is thread-safe and single-instance per file) but means there is no shared in-memory state, which could cause minor consistency issues if both instances cache state. This is a medium-priority architectural smell.

**Missing Hilt injection on `WorkManager` initialization provider:** `VosSyncWorker` uses `@HiltWorker` + `@AssistedInject` correctly. However, `WorkManager` must be initialized before any worker can be created. There is no `HiltWorkerFactory` setup found in `VoiceAvanueApplication`. The default `WorkManager` initialization does not know about Hilt-provided workers. Without configuring `HiltWorkerFactory`, `VosSyncWorker` will fail to inject `VosSyncManager`, `AvanuesSettingsRepository`, and `SftpCredentialStore` at runtime when `WorkManager` tries to instantiate it.

**`SftpCredentialStore` not Hilt-provided where used by `AppModule.provideSystemSettings()`:** `SettingsModule.provideSystemSettings()` takes a `SftpCredentialStore` parameter. `SftpCredentialStore` is annotated `@Singleton @Inject constructor` so Hilt can provide it. This is correct. No issue here.

**`VoiceAvanueApplication.startRpcServers()`:** Both `IVoiceOSServiceDelegate` and `IWebAvanueServiceDelegate` are passed as parameters but are never instantiated anywhere in the visible codebase. There is no Hilt binding for these interfaces, and no call site for `startRpcServers()` was found (other than `stopRpcServers()` being called in `onTerminate()`). The RPC server lifecycle is commented as "started when services are bound" but `RpcServerService.onStartCommand()` explicitly says "Actual RPC server startup is handled by VoiceAvanueApplication — this service just keeps the process alive." These two claims are contradictory. The result is `RpcServerService` is a foreground service that holds a notification but does nothing — the RPC servers are never started.

---

## Manifest Analysis

### Permissions

| Permission | Usage | Issue |
|-----------|-------|-------|
| `WRITE_SETTINGS` | Voice brightness/rotation commands | Declared **twice** (lines 47–48 and 60–62). Duplicate. |
| `BLUETOOTH` + `BLUETOOTH_ADMIN` | Voice Bluetooth commands | Declared **twice** (lines 51–54 and 64–68). Duplicate. |
| `CHANGE_WIFI_STATE` | Voice WiFi toggle | Declared **twice** (lines 57–58 and 71–72). Duplicate. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Overlay + RPC services | Correct but requires Play Store justification form |
| `CAMERA` | "Eye tracking and WebXR" | Declared but no runtime permission request flow is visible in the reviewed code |
| `READ_EXTERNAL_STORAGE` | maxSdkVersion=32 | Correct scoped storage handling |
| `WRITE_EXTERNAL_STORAGE` | maxSdkVersion=29 | Correct |

**Six permission declarations are exact duplicates.** This is harmless at runtime but represents a copy-paste error during consolidation that should be cleaned up.

### Services

| Service | Type | Exported | Issue |
|---------|------|----------|-------|
| `VoiceAvanueAccessibilityService` | Accessibility | `false` | Correct |
| `CommandOverlayService` | `specialUse` | `false` | Correct |
| `CursorOverlayService` | `specialUse` | `false` | External package (`com.augmentalis.voicecursor`) — correct |
| `VoiceRecognitionService` | `microphone` | `false` | See issue below |
| `RpcServerService` | `specialUse` | `false` | Functionally dead — see DI section |

**`VoiceRecognitionService` is registered with `foregroundServiceType="microphone"` but does not access the microphone.** The actual microphone access is inside `VoiceOSCore` which runs inside `VoiceAvanueAccessibilityService`. `VoiceRecognitionService.onStartCommand()` calls `processVoiceCommand("", 0f)` — an empty string with zero confidence — as a "wake up" signal. This is a misuse: passing empty string with confidence 0 as a command is not a defined API contract; behavior depends on `VoiceOSCore` internals. Furthermore, the service uses `foregroundServiceType="microphone"` while not using the microphone, which may trigger Play Store policy checks.

### Activity

| Item | Value | Issue |
|------|-------|-------|
| `launchMode` | `singleTask` | Correct for voice app |
| Deep link | `http` + `https` intent filter | No `autoVerify="true"` — app will not be a verified deep link handler. User will see disambiguation dialog for all HTTP/HTTPS links. |
| `settingsActivity` in accessibility config | `com.augmentalis.avaunified.ui.settings.SettingsActivity` | **Wrong package.** The app's namespace is `com.augmentalis.voiceavanue`. This points to a non-existent class. The Android accessibility settings screen will show a broken "Settings" link. |

### Activity-Alias

The manifest declares one `MainActivity` but the code comment and `determineLaunchMode()` refer to `WebAvanueAlias` and `VoiceAvanueAlias`. **No `activity-alias` declarations are present in the manifest.** Without `activity-alias` entries, the dual launcher icon feature described in the class header of `MainActivity` (and in the memory file) is non-functional. The only launcher icon that exists is the main Avanues hub.

---

## Entry Points

### Launchers

| Entry Point | Status |
|-------------|--------|
| Avanues (hub) | Working — `MainActivity` with `.ic_launcher_avanues` icon |
| VoiceAvanue alias | Missing — no `<activity-alias>` in manifest |
| WebAvanue alias | Missing — no `<activity-alias>` in manifest |

### Deep Links

HTTP/HTTPS deep links are declared but lack `android:autoVerify="true"` in the intent filter. This means the app will compete with every browser for all web links rather than being a verified handler.

### Boot Receiver

`BootReceiver` is declared with `exported="false"` but the intent-filter includes `BOOT_COMPLETED` and `QUICKBOOT_POWERON`. Android requires `exported="true"` for system broadcast receivers (a receiver with an intent filter is implicitly exported by the system; setting it to `false` will cause the broadcast to be silently dropped on Android 9+). The boot receiver will never fire.

---

## P0 / P1 / P2 Issue Summary

### P0 — Breaks core functionality at runtime

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| P0-1 | `AndroidManifest.xml:184` | `BootReceiver` declared `exported="false"` with a system intent-filter — receiver silently never fires on Android 9+ | Set `android:exported="true"` |
| P0-2 | `accessibility_service_config.xml:10` | `android:settingsActivity` points to `com.augmentalis.avaunified.ui.settings.SettingsActivity` — wrong package, class does not exist | Change to `com.augmentalis.voiceavanue.ui.settings.UnifiedSettingsActivity` or remove the attribute |
| P0-3 | `AndroidManifest.xml` | No `<activity-alias>` entries — dual launcher icon feature is entirely missing despite being the core feature of the consolidation | Add `<activity-alias>` entries for `.VoiceAvanueAlias` and `.WebAvanueAlias` |
| P0-4 | `di/SyncModule.kt:46-53` | `VosSyncWorker` uses `@HiltWorker` but `WorkManager` is never configured with `HiltWorkerFactory` in `VoiceAvanueApplication`. Hilt-injected workers will fail at runtime with `IllegalStateException: WorkerFactory returned null` | Configure `WorkManagerInitializer` with `HiltWorkerFactory` in the `Application` class |

### P1 — Significant bug or security issue

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| P1-1 | `service/RpcServerService.kt:35-45` | `RpcServerService` is a registered foreground service that does nothing. `startRpcServers()` is never called (no Hilt bindings for delegates, no call site). Service holds a persistent notification for a dead subsystem. | Either remove the service or wire actual delegate implementations |
| P1-2 | `service/VoiceRecognitionService.kt:59` | `processVoiceCommand("", 0f)` is called as a "wake up" signal — passes empty string with zero confidence. No defined contract for this; behavior is implementation-dependent and can cause unexpected command routing | Remove this call or use the proper `startListening()` API |
| P1-3 | `service/VoiceRecognitionService.kt` | Service declared with `foregroundServiceType="microphone"` but does not use the microphone itself — potential Play Store policy violation | Change to `specialUse` or remove the service entirely |
| P1-4 | `data/AvanuesSettingsRepository.kt:125` | Default `vosSftpHostKeyMode` is `"no"` — SSH host key checking disabled by default. This enables MITM attacks on all SFTP sync operations. Existing MEMORY.md note confirms this is a pre-existing bug in `VosSftpClient`; the default value here reinforces it at the settings layer | Default should be `"strict"` or `"ask"`, not `"no"` |
| P1-5 | `AndroidManifest.xml:120-126` | HTTP/HTTPS deep link intent-filter missing `android:autoVerify="true"` — app will show disambiguation chooser for all HTTP/HTTPS links rather than being the verified handler | Add `android:autoVerify="true"` to the intent-filter |

### P2 — Code quality / maintainability issue

| ID | File:Line | Issue | Fix |
|----|-----------|-------|-----|
| P2-1 | `AndroidManifest.xml:47-72` | `WRITE_SETTINGS`, `BLUETOOTH`, `BLUETOOTH_ADMIN`, `CHANGE_WIFI_STATE` each declared twice — copy-paste error from consolidation | Remove duplicates |
| P2-2 | `di/AppModule.kt` | `DeveloperPreferencesRepository` is not Hilt-provided — instantiated directly in two places (`VoiceAvanueAccessibilityService` L146, `DeveloperSettingsViewModel` L31) | Add `@Provides @Singleton` for `DeveloperPreferencesRepository` in `AppModule` |
| P2-3 | `service/CommandOverlayService.kt:146-159` | `onTaskRemoved()` uses `AlarmManager.set()` (non-exact) to restart the service after swipe-dismiss. On Android 12+ this requires `SCHEDULE_EXACT_ALARM` permission for exact alarms, or use `setAndAllowWhileIdle()`. Additionally, `PendingIntent.FLAG_ONE_SHOT` means the service can only restart once after swipe | Use `AlarmManager.setExactAndAllowWhileIdle()` or rely on `START_STICKY` and `onDestroy()` restart |
| P2-4 | `service/VoiceAvanueAccessibilityService.kt:402` | A second `AvanuesSettingsRepository(applicationContext)` is constructed inside `serviceScope.launch` (L402). `AppModule` already provides one as a singleton. The service cannot use Hilt field injection directly, but should use `EntryPointAccessors` to get the singleton instead | Create a second `EntryPoint` interface exposing `AvanuesSettingsRepository`, then use `EntryPointAccessors.fromApplication()` |
| P2-5 | `service/VoiceAvanueAccessibilityService.kt:140` | `VoiceOSDatabaseManager.getInstance()` is called directly with a new `DatabaseDriverFactory(applicationContext)` — bypasses the Hilt singleton `DatabaseDriverFactory` already provided by `AppModule`. Could create a second DB connection during initialization | Use `EntryPointAccessors` to get the Hilt-provided `VoiceOSDatabaseManager` |
| P2-6 | `ui/home/DashboardViewModel.kt:298` | `webAvanueState` is hardcoded as `ServiceState.Ready` with static metadata regardless of actual browser state — always shows "Available / 0 tabs" | Observe actual `BrowserRepository` state or `WebAvanue` module StateFlow |
| P2-7 | `ui/hub/HubModule.kt:84` | `CursorAvanue` module routes to `AvanueMode.SETTINGS.route` with a `// TODO` comment — navigates to Settings instead of a dedicated cursor screen | Track as a known limitation; remove TODO when CURSOR mode is added |
| P2-8 | `ui/hub/SpatialOrbitHub.kt` | Uses `MaterialTheme` import alongside `AvanueTheme` (seen in imports L41 — `import androidx.compose.material3.MaterialTheme`) — potential theme mixing | Verify all color/style reads go through `AvanueTheme.*`, not `MaterialTheme.*` |
| P2-9 | `service/CommandOverlayService.kt:193` | Notification small icon uses `android.R.drawable.ic_menu_view` (system drawable) — no branding | Use `R.drawable.ic_notification_voice` or a purpose-built overlay icon |
| P2-10 | `VoiceAvanueApplication.kt:41` | `applicationScope` uses `Dispatchers.Main` but all coroutine work in `initializeModules()` is dispatched to `Dispatchers.IO`. The scope dispatcher is irrelevant since `launch(Dispatchers.IO)` overrides it, but using `Main` as the default is misleading | Use `Dispatchers.Default` or `SupervisorJob()` alone as scope dispatcher |

---

## AVID / Voice Semantics Observations

- `SpatialOrbitHub.kt` module nodes are clickable but AVID semantics were not confirmed (file read was truncated at L80). Needs a follow-up check.
- `CommandOverlayService` overlay badges are non-interactive by design (FLAG_NOT_TOUCHABLE) — no AVID needed.
- `NumbersInstructionPanel` and `FeedbackToast` are display-only — no AVID needed.
- `HubDashboardScreen` dock buttons (Settings, About, Developer) in `SpatialOrbitHub.kt` — need AVID `contentDescription` semantics blocks.

---

## Theme Compliance

- `MainActivity.kt` — `AvanueThemeProvider` used correctly with `palette.colors(isDark)`, `palette.glass(isDark)`, `palette.water(isDark)`. Compliant.
- `SpatialOrbitHub.kt` L41 imports `MaterialTheme` — needs verification that no direct `MaterialTheme.colorScheme.*` reads occur.
- `CommandOverlayService.kt` composables use hardcoded `Color(0xEE000000)` etc. — acceptable for an overlay that must not change with theme, but note that badge colors are hardcoded and do not follow `AvanueTheme`.
- `DeveloperPreferences.kt` defaults `debugMode = true` — this means debug mode ships ON by default. Should be `false` in production.

---

## Test Coverage

No test files were found in `apps/avanues/src/test/` or `src/androidTest/` beyond the standard empty placeholders. The `build.gradle.kts` declares test dependencies but no test cases exist for:
- DI module binding correctness
- `DynamicCommandGenerator.calculateStructuralChangeRatio()`
- `AvanuesSettingsRepository` read/write round-trip
- `VosSyncWorker` retry logic

This is a P2 gap — particularly for the settings repository which has custom JSON serialization for user synonyms.
