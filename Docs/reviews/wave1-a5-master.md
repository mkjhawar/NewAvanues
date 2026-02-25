# Wave 1-A5 Master Analysis Entries
**Modules:** Gaze | VoiceIsolation | DeviceManager
**Date:** 260222 | **Full report:** `docs/reviews/DeviceManager-Gaze-VoiceIso-Review-QualityAnalysis-260222-V1.md`

---

## MODULE: Gaze
**Files:** 4 kt | **Score:** 62/100 | **Health:** FAIR
**P0:** 0 | **P1:** 1 | **P2:** 3

### Status
Clean interface design and well-typed data model. Android and Desktop factories are intentional stubs (ML Kit deferred — tracked in backlog). No iOS actual exists (compile error on iOS target). The key correctness bug is the `StubGazeTracker.state` getter.

### P1 Issues
- `GazeTracker.kt:104-105` — `StubGazeTracker.state` getter creates a new `MutableStateFlow` instance on every property access. Collectors on previously returned flows never receive updates. Fix: declare `private val _state = MutableStateFlow(GazeTrackerState())` as a backing property.

### P2 Issues
- `GazeTrackerPlatform.kt` (iosMain) — File does not exist. `expect object GazeTrackerFactory` has no iOS `actual`. Add stub returning `StubGazeTracker()`.
- `GazeTypes.kt:43` — `GazePoint.timestamp` and `GazeSample.timestamp` default to `0L`. No time injection. All emitted samples have zero timestamp unless callers set it manually.
- `GazeTrackerPlatform.kt` (android/desktop) — TODO comments inline with production code. Move to PROJECT-TODO-BACKLOG.md.

### Known Intentional (Do Not Re-flag)
- `GazeTrackerFactory.create()` returning `StubGazeTracker()` on Android/Desktop — intentional. ML Kit removal is a tracked backlog item.
- `isAvailable()` returning `false` — correct given stub status.

---

## MODULE: VoiceIsolation
**Files:** 7 kt | **Score:** 70/100 | **Health:** FAIR
**P0:** 0 | **P1:** 2 | **P2:** 3

### Status
Android implementation is production-quality: uses native `NoiseSuppressor`, `AcousticEchoCanceler`, and `AutomaticGainControl` with proper session-based lifecycle management and correct `release()` sequencing. iOS and Desktop are silent stubs that report success.

### P1 Issues
- `VoiceIsolation.ios.kt:31-38` — `initialize()` returns `true` and sets `isInitialized = true` with zero audio pipeline setup. State shows `isActive = true` while nothing processes audio. Any caller gating on `isActive` (e.g., VoiceOSCore SpeechEngine) will believe iOS voice isolation is running when it is not. Fix: return `false` and leave `isInitialized = false` until AVAudioEngine/Voice Processing I/O is wired.
- `VoiceIsolation.desktop.kt:31-38` — Same false-success stub as iOS. Fix: same approach.

### P2 Issues
- `VoiceIsolation.ios.kt:36,49,56,71` and `VoiceIsolation.desktop.kt:36,49,56,71` — 8 total `println()` calls across iOS and Desktop actuals. Use platform Logger or Napier.
- `VoiceIsolation.android.kt:93-120` — `updateConfig()` when toggling re-enable says "will apply on next initialize()" but no re-init path exposes the stored `audioSessionId`. Toggling voice isolation back on does not restore hardware effects. Store session ID and auto-reinitialize internally.
- `VoiceIsolation.android.kt:246-252` — `updateState()` calls `checkFeatureAvailability()` which queries hardware 3 times per state update. Cache `FeatureAvailability` at construction time.

### Known Correct (Do Not Re-flag)
- Android `disableAllEffects()` release + null pattern is correct and idiomatic.
- `VoiceIsolationConfig.init {}` block validation for range checks is correct.
- `VoiceIsolationFactory.create(Context)` overload providing explicit context is good API design.

---

## MODULE: DeviceManager
**Files:** 82 kt | **Score:** 48/100 | **Health:** POOR
**P0:** 0 | **P1:** 7 | **P2:** 10

### Status
Large module with genuinely good architecture in the core layer (IMUManager throttling, sensor fusion algorithms, capability injection pattern, AudioService audio focus management). However the dashboard UI layer contains multiple Rule 1 violations (simulated data, stub operations), incorrect BatteryManager field usage, severely wrong iOS capability detection, and zero voice semantics. The module is Android-only in practice despite having KMP structure in commonMain/iosMain/desktopMain; iOS and Desktop factories have multiple hardcoded or stub values.

### P1 Issues
- `DeviceViewModel.kt:361-382` — `loadAudioDevices()` returns hardcoded list of 2 fake devices. Use `AudioManager.getDevices(GET_DEVICES_ALL)`.
- `DeviceViewModel.kt:562-576` — `testSensors()` is a `delay(2000)` stub reporting all sensors operational. Implement or remove.
- `DeviceViewModel.kt:582-599` — `runDiagnostics()` is a `delay(3000)` stub. Implement or remove.
- `DeviceViewModel.kt:270-271` — Battery `temperature` and `voltage` both computed from `BATTERY_PROPERTY_CURRENT_NOW` (instantaneous current, not temperature or voltage). UI shows wrong data. Fix: use `ACTION_BATTERY_CHANGED` extras.
- `DeviceCapabilityFactory.ios.kt:220-226` — `hasTouchId()` always `true`, `hasFaceId()` always `false`. Wrong for all Face ID devices (iPhone X+). Fix: use `LAContext.biometryType`.
- `DeviceCapabilityFactory.ios.kt:205-207` — `hasNfcCapability()` always `true`. Fix: use `NFCTagReaderSession.readingAvailable`.
- `audio/AudioCapture.kt:115-120` — `audioRecord?.release()` called while recording coroutine may still be in `audioRecord?.read()`. Potential native crash. Fix: await job completion before releasing.

### P2 Issues
- `DeviceInfoUI.kt:395-401` — `AudioTab` shows placeholder text `"Audio manager information would go here"`. Rule 1.
- `DeviceViewModel.kt:421` — `checkUWBSupport()` hardcodes `false`. Wire to `deviceManager.uwb?.uwbState?.value?.isSupported`.
- `DeviceViewModel.kt:437-443` — IMU monitoring coroutine body is entirely commented out. `imuData` StateFlow never updates.
- `DeviceCapabilityFactory.desktop.kt:99-110` — Display hardcoded `1920x1080 / 96dpi / 60Hz`. Use `GraphicsEnvironment` APIs.
- `DeviceCapabilityFactory.ios.kt:201-203` — `getStorageGb()` hardcoded `64`. Use `FileManager.attributesOfFileSystem`.
- `SensorFusionManager.kt:269-335` — `lastTimestamp` read/write outside `sensorLock` — TOCTOU race allowing duplicate frame emission. Fix: move inside lock or use `AtomicLong.compareAndSet`.
- `DeviceManager.kt:206` — `shutdown()` does not cancel `scope`. Coroutines launched in `initializeAll()` continue after shutdown. Add `scope.cancel()`.
- `DeviceManager.kt:144-148` — LiDAR manager gated on `totalSensorCount > 0` (loads for any device with sensors). Add dedicated `hasLidar: Boolean` flag.
- `DeviceManagerActivity.kt`, `DeviceViewModel.kt`, `GlassmorphismUtils.kt` — `Author: VOS4 Development Team` (3 Rule 7 violations).
- All interactive elements across `DeviceManagerActivity.kt` and `DeviceInfoUI.kt` — Zero AVID semantics. Full semantic pass required.

### Known Correct / Intentional (Do Not Re-flag)
- `IMUManager.onSensorChanged()` — timestamp throttle check at L339-342 runs BEFORE `scope.launch{}`, eliminating the 120Hz coroutine-per-event concern from the earlier Network review entry.
- `SensorFusionManager.sensorLock` — correctly guards `lastAcceleration`, `lastAngularVelocity`, `lastMagneticField` writes.
- `DeviceManagerActivity.kt` — `AvanueThemeProvider` with `HydraColors`/`HydraGlass`/`HydraWater` is correct v5.1 usage.
- `IMUDataPool.use {}` inline extension — correct pool-and-copy pattern; the pooled object is released BEFORE emit, preventing pool corruption.
- `DeviceViewModel.scanBluetoothDevices()` — Uses real `BluetoothManager.startDiscovery()` with proper timeout fallback. Production-quality.
- `DeviceViewModel.scanWiFiNetworks()` — Uses real `WiFiManager.startScan()` + `drop(1).first()` pattern for fresh scan results. Production-quality.
- `DeviceCapabilityFactory.desktop.kt` — `detectMacModel()` via `ProcessBuilder("sysctl")` is a reasonable approach for macOS model detection.
