# Implementation Plan: P4→P2→P3→P1→P5 Sprint Commit & Verify

## Overview
Platforms: Android, iOS, JVM/Desktop, JS/Browser (KMP)
Swarm Recommended: Yes (5 independent verification tasks)
Estimated: 5 segments, ~15 min total

## Segment 1: P4 — WebAvanue Test Fixes
**Files:**
- `Modules/WebAvanue/src/commonTest/.../SettingsStateMachineTest.kt` — `testScope.runTest` fix
- `Modules/WebAvanue/src/commonMain/.../BrowserSettingsModel.kt` — dead `android.util.Log` import
- `Modules/WebAvanue/src/commonMain/.../SettingsViewModel.kt` — dead `android.util.Log` import
- `Modules/WebAvanue/src/androidUnitTest/.../EncryptionManagerTest.kt` — `@Ignore` (AndroidKeyStore)
- `Modules/WebAvanue/src/androidUnitTest/.../EncryptedDatabaseTest.kt` — `@Ignore` (SQLCipher)

**Verify:** `./gradlew :Modules:WebAvanue:compileDebugKotlinAndroid`
**Commit:** `fix(WebAvanue): fix test scope mismatch, remove dead imports, @Ignore device-only tests`

## Segment 2: P2 — JS Target for Logging, Foundation, HTTPAvanue, NetAvanue
**Build files (4):**
- `Modules/Logging/build.gradle.kts` — add `js(IR)`
- `Modules/Foundation/build.gradle.kts` — add `js(IR)`
- `Modules/HTTPAvanue/build.gradle.kts` — add `js(IR)`
- `Modules/NetAvanue/build.gradle.kts` — add `js(IR)`

**New jsMain files (14):**
- Logging: JsLogger.kt, LoggerFactory.kt
- Foundation: Sha256Js.kt
- HTTPAvanue: PlatformTime.js.kt, Resources.js.kt, Socket.js.kt, CompressionJs.kt, Sha1Js.kt, MdnsAdvertiser.js.kt
- NetAvanue: CapabilityCollector.js.kt, DeviceFingerprint.js.kt, UdpSocket.js.kt, TimeMillis.js.kt, LocalAddresses.js.kt

**Verify:** `./gradlew :Modules:NetAvanue:compileKotlinJs`
**Commit:** `feat(NetAvanue): add JS/browser target with full dependency chain (Logging→Foundation→HTTPAvanue→NetAvanue)`

## Segment 3: P3 — PluginSystem Expect/Actual Fixes
**Files:**
- `Modules/PluginSystem/src/jvmMain/.../security/PermissionStorage.kt` — rewrite to match expect API
- `Modules/PluginSystem/src/jvmMain/.../themes/FontLoader.kt` — remove nested type re-declarations
- `Modules/PluginSystem/src/iosMain/.../themes/FontLoader.kt` — remove nested type re-declarations
- `Modules/PluginSystem/src/iosMain/.../platform/IosPluginExample.kt` — DELETED (stale)

**Verify:** `./gradlew :Modules:PluginSystem:compileKotlinJvm`
**Commit:** `fix(PluginSystem): fix JVM PermissionStorage + FontLoader expect/actual mismatches, delete stale IosPluginExample`

## Segment 4: P1 — SpeechRecognition Google Cloud STT v2 Completion
**Files:**
- `Modules/SpeechRecognition/build.gradle.kts` — add Foundation dependency
- `Modules/SpeechRecognition/src/androidMain/.../GoogleCloudApiClient.kt` — phrase hints
- `Modules/SpeechRecognition/src/androidMain/.../GoogleCloudStreamingClient.kt` — phrase hints
- `Modules/SpeechRecognition/src/androidMain/.../GoogleCloudEngine.kt` — forward commandCache
- NEW: `Modules/SpeechRecognition/src/androidMain/.../GoogleCloudSettingsProvider.kt`

**Verify:** `./gradlew :Modules:SpeechRecognition:compileDebugKotlinAndroid`
**Commit:** `feat(SpeechRecognition): add phrase hints + settings provider for Google Cloud STT v2`

## Segment 5: P5 — NetAvanue Review Follow-ups
**Files:**
- `Modules/NetAvanue/src/commonMain/.../CapabilityScorer.kt` — formula dedup
- `Modules/NetAvanue/src/commonMain/.../IceCandidatePair.kt` — data class → class
- `Modules/NetAvanue/src/commonMain/.../SocketIOPacket.kt` — add JSON parse logging

**Verify:** `./gradlew :Modules:NetAvanue:compileKotlinDesktop`
**Commit:** `refactor(NetAvanue): dedup scoring formula, fix IceCandidatePair mutability, add parse logging`

## Time Estimates
Sequential: 15 min (compile + commit each)
Parallel (Swarm verify): 8 min (compile all in parallel, commit sequential)
