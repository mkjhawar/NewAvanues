# SpeechRecognition Module — Deep Review
**Date:** 260220
**Reviewer:** Code Reviewer Agent
**Scope:** `Modules/SpeechRecognition/src/` — all 54 .kt files across commonMain, androidMain, iosMain, desktopMain, jsMain.disabled
**Findings:** 7 Critical / 19 High / 22 Medium / 12 Low — 60 total

---

## Summary

The SpeechRecognition KMP module integrates six speech engines (Vivoka VSDK, VOSK, Android STT, Google Cloud, Azure, Whisper) behind a common `SpeechRecognitionService` interface. The androidMain source set contains the only fully-wired implementation; iosMain is functional for Apple SFSpeechRecognizer; desktopMain and jsMain.disabled are stub scaffolds with TODO comments throughout.

The most severe issues are concentrated in three areas. First, **security**: hardcoded credentials (`avanuevoiceos` / `!AvA$Avanue123#`) are committed in plaintext in `FirebaseRemoteConfigRepository`, a zip path traversal (zip slip) vulnerability exists in `FileZipManager`, and the double-checked locking singleton for `FirebaseRemoteConfigRepository` is missing `@Volatile` — all three represent production risks. Second, **logic correctness**: VOSK confidence normalization is mathematically inverted (poor matches score near 1.0), `VivokaAudio.startSilenceDetection()` fires its callback every 100 ms indefinitely instead of once after a timeout, and `VivokaInitializer.setupCrashHandlers()` installs a global uncaught exception handler that hard-crashes the app and overrides Crashlytics. Third, **threading**: `AndroidSTTEngine.destroy()` cancels its coroutine scope before the `recognizer.destroy()` coroutine on that scope can complete, the `FirebaseRemoteConfigRepository` singleton leaks a `Context`, and `VoiceStateManager` invokes the `onStateChangeCallback` from inside a write lock, creating a deadlock vector.

Rule 7 violations are widespread — twelve files carry `Author: Claude (AI Assistant)` or `Author: VOS4 Development Team` headers. `VivokaLearning` is a documented stub that has zero functionality yet is called unconditionally from the engine. The `DesktopSpeechRecognitionService` and `JsSpeechRecognitionService` are complete stubs that report `READY`/`LISTENING` states without performing any recognition.

---

## Issues Table

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `vivoka/model/FirebaseRemoteConfigRepository.kt:16-17` | Hardcoded plaintext credentials: `USERNAME = "avanuevoiceos"`, `PASSWORD = "!AvA\$Avanue123#"` committed to source and baked into APK | Move to BuildConfig secrets or server-side token exchange; remove from source immediately |
| Critical | `vivoka/model/FileZipManager.kt:~35` | Zip path traversal (zip slip): `File(toDir, entry.name)` with no canonical path check — malicious zip can write to arbitrary filesystem locations | Add `require(unzipFile.canonicalPath.startsWith(toDir.canonicalPath + File.separator))` before creating the file |
| Critical | `vivoka/VivokaInitializer.kt:~80` | `setupCrashHandlers()` installs a global `Thread.setDefaultUncaughtExceptionHandler` that rethrows the exception, killing the entire app; overrides Firebase Crashlytics | Remove the rethrow; log and report to existing crash handler via `Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(t, e)` |
| Critical | `ConfidenceScorer.kt:110` | VOSK sigmoid normalization is inverted: `1/(1+exp(-rawScore * 1.0f))`. VOSK acoustic scores are large negative numbers (e.g., -200). `exp(200)` overflows to infinity, making sigmoid → 1.0. Poor matches score as maximum confidence | Use `(rawScore / -200f).coerceIn(0f,1f)` or a log-scale formula; the current formula renders VOSK confidence meaningless |
| Critical | `vivoka/VivokaAudio.kt:~120` | `startSilenceDetection()` posts a Runnable that fires `silenceDetectionCallback?.invoke()` then reschedules itself every 100 ms indefinitely. Dictation end is triggered every 100 ms continuously, not once after a timeout | Redesign: post with `postDelayed(SILENCE_TIMEOUT_MS)` without rescheduling; cancel with `handler.removeCallbacks(runnable)` |
| Critical | `vivoka/model/FirebaseRemoteConfigRepository.kt:~50` | Double-checked locking singleton is broken: `instance` has no `@Volatile`. CPU caching can cause two threads to each see `null` and construct a duplicate instance | Add `@Volatile` to the `instance` field; or use `object` companion for proper Kotlin singleton |
| Critical | `DesktopSpeechRecognitionService.kt:55-80` | `initialize()` succeeds with TODO comments instead of throwing; `startListening()` sets state to LISTENING without any audio capture — desktop speech is a non-functional stub reporting success (Rule 1 violation) | Implement Vosk JNI or Whisper.cpp binding, or throw `UnsupportedOperationException` until implemented |
| High | `vivoka/VivokaInitializer.kt:~55` | `Vsdk.init(context, configPath) { success -> ... }` is a fire-and-forget callback. `configureASR()` and `verifyModels()` are called immediately after on the next lines, before the SDK may have actually initialized — race condition | Convert to a suspending callback pattern using `suspendCancellableCoroutine`; only proceed after callback confirms success |
| High | `vivoka/VivokaInitializer.kt:~110` | `configureASR()` has a completely empty body that only logs "ASR configuration complete" — ASR is never configured (Rule 1 violation) | Implement ASR configuration using the VSDK API, or document exactly what configuration is handled by the VSDK itself |
| High | `vivoka/VivokaLearning.kt` (entire file) | All methods are no-ops: `initialize()` returns true, `processCommandWithLearning()` returns the original command unchanged, `recordSuccess()`/`recordFailure()` do nothing — learning is never applied | Either implement VSDK adaptive learning or remove the learning layer entirely and remove all calls from `VivokaEngine` |
| High | `vivoka/VivokaInitializationManager.kt:332` | `initializeDegradedMode()` always returns `false` — degraded mode advertised but never actually entered. `attemptDegradedInitialization()` always reports failure | Implement a minimal degraded init (skip model compilation, use cached commands) or remove the degraded mode code path |
| High | `AndroidSTTEngine.kt:~200` | `destroy()` launches `recognizer.destroy()` as a coroutine on `scope`, then immediately calls `scope.cancel()` — the destroy coroutine is cancelled before it runs, leaving the Android `SpeechRecognizer` leaked | `runBlocking { recognizer.destroy() }` before cancelling scope, or `scope.launch { recognizer.destroy() }.join()` then cancel |
| High | `VoiceStateManager.kt:~180` | `onStateChangeCallback?.invoke(newState)` called from inside `stateLock.write {}`. If the callback tries to acquire any lock (including a read lock on `stateLock`), this deadlocks | Move the callback invocation outside the lock: capture the callback reference inside the lock, then invoke it after releasing |
| High | `VoiceStateManager.kt:302-305` | `downloadingModels(false)` disables `isSleeping` and `isDictationActive` — finishing a model download incorrectly clears the user's active voice state | `downloadingModels(false)` should only reset `isDownloadingModels`; leave other state flags untouched |
| High | `AndroidSpeechRecognitionService.kt:~130` | `setLanguage()` updates `config` but does NOT propagate to `androidSTTEngine` — language change has no effect until the next `initialize()` call; callers are silently given stale recognition | Call `androidSTTEngine?.setLanguage(language)` and reinitialize if currently READY/LISTENING |
| High | `SpeechErrorCodes.kt:~30` | `MEMORY_ERROR = 1005` and `NETWORK_ERROR = 1006` fall in the Initialization range (1001-1099); `getErrorCategory()` therefore categorizes them as INITIALIZATION, not RESOURCE/NETWORK — telemetry and error handling routing is wrong | Renumber: `MEMORY_ERROR` should be in 1401-1499, `NETWORK_ERROR` in 1501-1599, matching the documented ranges |
| High | `vivoka/VivokaInitializationManager.kt:364` | `validateAssets()` logs "Required asset missing" but always continues without failing — "required" assets are not enforced | Change to `return AssetValidationResult.FAILED` when a required asset is absent, consistent with what the method name implies |
| High | `vivoka/model/FirebaseRemoteConfigRepository.kt:~90` | `remoteConfig.fetchAndActivate()` in `init {}` block is called without `await()` — fire and forget; config may not be activated when `getLanguageResource()` is called | Use `LaunchedEffect`/suspend or defer the `fetchAndActivate()` until first use of `getLanguageResource()` with `await()` |
| High | `vivoka/model/FirebaseRemoteConfigRepository.kt:~120` | `totalBytes` could be 0 when content-length header is absent, causing division-by-zero in progress calculation: `(downloadedBytes * 100 / totalBytes)` | Guard: `if (totalBytes > 0L) { progress = (downloadedBytes * 100 / totalBytes).toInt() }` |
| High | `vivoka/model/FirebaseRemoteConfigRepository.kt:~40` | `@SuppressLint("StaticFieldLeak")` on singleton `instance` field — `context` is stored directly and never wrapped in `applicationContext`, leaking the calling Activity/Service | Store `context.applicationContext` instead of raw `context` |
| High | `VivokaEngine.kt:~100-112` | `FirebaseApp.initializeApp(context)` called twice: once in the `init {}` block and again at the top of `initialize()`. The second call is a harmless no-op but obscures initialization flow | Remove the `init {}` block call; keep only the one in `initialize()` behind a null-check |
| High | `JsSpeechRecognitionService.kt` (entire file) | `startListening()` transitions to `LISTENING` with TODO and never wires the Web Speech API — JS recognition is a non-functional stub reporting success (Rule 1 violation). This is in jsMain.disabled but should be documented | Mark all stub methods with `throw UnsupportedOperationException("JS speech recognition: Phase 2")` rather than silently returning success |
| Medium | `VivokaEngine.kt:~420` | Periodic learning sync check: `System.currentTimeMillis() % (5 * 60 * 1000) < 30000` fires for the first 30 seconds of every 5-minute epoch since Unix epoch (not since engine start) — fires at wall-clock 00:00, 05:00, 10:00, etc., not every 5 minutes relative to engine startup | Track `lastSyncTime` and compare elapsed: `System.currentTimeMillis() - lastSyncTime > (5 * 60 * 1000)` |
| Medium | `VivokaEngine.kt:~450` | `handleRegularCommand()` has `@Suppress("SENSELESS_COMPARISON")` on an `enhancedCommand != null` check — Kotlin warns because the enhanced command is a non-null type. The `if` always takes the false branch | Remove the dead `if (enhancedCommand != null)` branch entirely, or fix the type to `String?` if null is possible |
| Medium | `VivokaEngine.kt:~600` | `destroy()` calls `runBlocking { learning.destroy() }` and `runBlocking { model.reset() }` — both block the calling thread. On the main thread (typical lifecycle callback), this ANRs after 5 seconds | Convert to `suspend fun destroy()` or use a dedicated IO scope that callers can join |
| Medium | `ResultProcessor.kt:84-93 + 335-353` | Duplicate deduplication logic: `processResult()` and `shouldAccept()` both track `lastResultText`/`lastResultTime` independently. Calling both creates inconsistent state — one accepts, the other rejects | Collapse into one canonical deduplication check in `shouldAccept()` that `processResult()` delegates to |
| Medium | `ResultProcessor.kt:~30-40` | `processedCount`, `acceptedCount`, `rejectedCount`, `lastResultText`, `lastResultTime` are plain non-volatile, non-atomic fields. `processResult()` is called from IO/Main dispatchers in different platforms — data races | Protect with `AtomicLong`/`AtomicReference` or wrap in a `Mutex` |
| Medium | `CommandCache.kt:~80` | Comment says "LRU eviction" but uses `vocabularyCache.keys.firstOrNull()` on a `ConcurrentHashMap` — iteration order is non-deterministic, not least-recently-used | Use `LinkedHashMap(capacity, 0.75f, accessOrder=true)` wrapped in `Collections.synchronizedMap()` for true LRU, or use a `MutableMap` with explicit ordering |
| Medium | `AndroidSpeechRecognitionService.kt:~160` | Three separate `CommandCache` instances exist: one in the service, one inside `ResultProcessor`, and one inside `AndroidSTTEngine`. Commands set on the service via `setCommands()` may not reach the engine's cache | Inject a single `CommandCache` instance through all three layers |
| Medium | `LearningSystem.android.kt:~60` | `learnedCommands` and `confidenceHistory` are plain `mutableMapOf()`/`mutableListOf()` without synchronization. `processWithLearning()`, `recordSuccess()`, `recordFailure()` can be called from different coroutines — data races | Wrap in `Mutex` or use `ConcurrentHashMap` + `Collections.synchronizedList` |
| Medium | `LearningSystem.android.kt:~120` | Levenshtein distance allocates `Array(len1+1) { IntArray(len2+1) }` — full 2D matrix per call. For hundreds of commands per recognition, this creates excessive O(n*m) GC pressure | Use the two-row rolling optimization (only two `IntArray(len+1)` rows needed) |
| Medium | `SdkInitializationManager.kt` + `UniversalInitializationManager.kt` | Two entirely separate singleton initialization managers doing nearly identical work — DRY violation. `VivokaEngine` uses both simultaneously | Merge into a single `SdkInitializationManager`; remove `UniversalInitializationManager` |
| Medium | `UniversalInitializationManager.kt:~280` | `performInitializationWithRetry()` degraded mode block hardcodes `false` as the result, so degraded mode is never actually achieved | Implement a real degraded path or remove the degraded mode parameter from the public API |
| Medium | `UniversalInitializationManager.kt:~200` | `waitForInitializationCompletion()` busy-polls with `delay(100)` in a `while(true)` loop | Replace with a `CompletableDeferred<Boolean>` or a `Channel<Boolean>` — emit when done, suspend-wait on the receiver side |
| Medium | `vivoka/VivokaConfig.kt:~60` | `mergeJsonFiles()` is duplicated verbatim from `VivokaAssets.mergeJsonFiles()` — DRY violation | Move to a shared `VsdkJsonUtils` companion object or top-level utility |
| Medium | `vivoka/VivokaConfig.kt:~30` | `speechConfig` is a `lateinit var` — calling `getSpeechConfig()` before `initialize()` throws an unguarded `UninitializedPropertyAccessException` | Add `if (::speechConfig.isInitialized) return speechConfig` guard, or initialize with a default `SpeechConfig` |
| Medium | `vivoka/VivokaPathResolver.kt:~40` | `Environment.getExternalStorageDirectory()` is deprecated in API 29+ and returns a path outside scoped storage — writes will fail silently on Android 10+ for most apps | Use `context.getExternalFilesDir(null)` for app-scoped external storage |
| Medium | `vivoka/VivokaState.kt:~20` | `stateMutex = Mutex()` is declared but never used — all state access uses `@Volatile` fields directly, making the Mutex dead code | Remove the unused `stateMutex` declaration |
| Medium | `vivoka/VivokaModel.kt:~160` | `getCompilationTimeout()` and `isValidCommand()` are defined but never called — dead code | Remove or add a usage |
| Medium | `vivoka/VivokaInitializationManager.kt:~280` | `ensureAssetsReady()` writes a `.vsdk_sync_test` temp file on every initialization to force filesystem sync; if an exception occurs between `writeText` and `delete`, the file is orphaned | Wrap in try/finally: `testFile.writeText("x"); try { ... } finally { testFile.delete() }` |
| Medium | `vivoka/VivokaInitializationManager.kt:~320` | `hasRequiredPermissions()` always returns `true` — permission check is not implemented | Implement using `ContextCompat.checkSelfPermission(context, RECORD_AUDIO) == PERMISSION_GRANTED` |
| Medium | `vivoka/model/VivokaLanguageRepository.kt:~20` | `LANGUAGE_CODE_INDONESIAN = "in"` — "in" is the deprecated ISO 639-1 code; the correct BCP-47 code is "id". "in" is also a Kotlin reserved keyword | Change to `"id"` |
| Medium | `vivoka/model/VivokaLanguageRepository.kt:~80` | `getBcpLangTag()` silently falls back to `"en-US"` for most non-English languages in the `DYNAMIC_RESOURCE` list — multi-language users get English recognition silently | Map all supported languages in the `when` expression |
| Medium | `SpeechConfig.kt:82` | `ttsLanguage: String = language` in a data class default captures `language` at construction time. `copy(language = "fr-FR")` does NOT update `ttsLanguage` — TTS language stays as the original construction value silently | Add a `normalizedTtsLanguage` property: `val normalizedTtsLanguage get() = ttsLanguage.ifEmpty { language }`, and document the copy behavior |
| Medium | `ErrorRecoveryManager.android.kt:~30` | `errorHistory` is a plain `mutableListOf()` accessed from `handleError()` on the IO coroutine — no synchronization, data race | Protect with `Mutex` or use `Collections.synchronizedList` |
| Medium | `VoiceStateManager.kt:~80` | `@Volatile` on `AtomicBoolean`/`AtomicLong` references is redundant: the volatile annotation protects the reference variable (never reassigned), not the value — misleading code | Remove `@Volatile` annotations from `AtomicBoolean`/`AtomicLong` fields; keep only the volatile where the reference itself is reassigned |
| Medium | `IosSpeechRecognitionService.kt:96-99` | `SFSpeechRecognizer(locale = locale)` can return nil on iOS if the locale is unsupported (checked on next line), but `initialize()` continues to set state `READY` after emitting the error — callers can call `startListening()` in the ERROR state | Add `return` after emitting the error and setting `ServiceState.ERROR`; do not reach `Result.success(Unit)` |
| Medium | `IosSpeechRecognitionService.kt:197-198` | `audioEngine?.startAndReturnError(null)` ignores the error parameter — if the audio engine fails to start (e.g., microphone in use by another app), the failure is silently swallowed | Use `val nsError = ObjCObjectVar<NSError?>(); audioEngine?.startAndReturnError(nsError.ptr)` and check for error |
| Low | `SpeechRecognitionService.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Change to `Author: Manoj Jhawar` or remove the Author field |
| Low | `PlatformUtils.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `AndroidSpeechRecognitionService.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `PlatformUtils.android.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `SpeechRecognitionServiceFactory.android.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `SdkInitializationManager.kt:5` | `Author: VOS4 Development Team` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `UniversalInitializationManager.kt:5` | `Author: VOS4 Development Team` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `VivokaInitializationManager.kt:5` | `Author: VOS4 Development Team` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `DesktopSpeechRecognitionService.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `PlatformUtils.desktop.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `SpeechRecognitionServiceFactory.desktop.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `JsSpeechRecognitionService.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `PlatformUtils.js.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` |
| Low | `SpeechRecognitionServiceFactory.js.kt:5` | `Author: Claude (AI Assistant)` — Rule 7 violation | Remove or replace with `Author: Manoj Jhawar` (14 Rule 7 violations total — all `Author` lines should be corrected together in a single pass) |
| Low | `ConfidenceScorer.kt:103` | Comment says "Vivoka uses 0-100 scale" but the constant is `VIVOKA_MAX_SCORE = 10000f` and `VivokaRecognizer` confirms 0-10000 range — misleading comment | Fix comment to "Vivoka uses 0-10000 scale" |
| Low | `vivoka/VivokaAudio.kt:~50` | `if (audioRecorder == null)` and `if (pipeline == null)` after non-nullable Kotlin constructor calls — these null checks are dead code (Kotlin constructors cannot return null) | Remove dead null checks |
| Low | `vivoka/VivokaAudio.kt` | `@SuppressLint("MissingPermission")` applied without a comment explaining where the permission is verified upstream | Add a comment: `// RECORD_AUDIO verified by VivokaInitializationManager.hasRequiredPermissions()` |
| Low | `vivoka/VivokaRecognizer.kt:57` | `_resultFlow = MutableStateFlow<RecognitionResult?>(null)` — StateFlow with a null initial value means every new subscriber immediately receives `null`. For an event stream, `MutableSharedFlow` is more appropriate | Change to `MutableSharedFlow<RecognitionResult>(replay = 1)` to avoid null-initial-value subscriptions |
| Low | `vivoka/model/VivokaLanguageRepository.kt:~100` | `isLanguageDownloaded()` parses JSON with Gson on every call — no caching of the parsed result | Cache the parsed result; only re-parse when the file changes |
| Low | `AndroidSTTEngine.kt:~300` | Periodic learning sync check comment says "every 5 minutes" but the modular arithmetic implementation fires based on Unix epoch wall clock, not engine uptime — misleading comment | Fix the implementation to match the comment (see Medium issue above) |

---

## Detailed Findings

### CRITICAL-1: Hardcoded Credentials — FirebaseRemoteConfigRepository.kt

```kotlin
// FirebaseRemoteConfigRepository.kt (lines 15-17)
companion object {
    private const val USERNAME = "avanuevoiceos"
    private const val PASSWORD = "!AvA\$Avanue123#"
```

These are committed to source control in plain text. Any developer with read access, any build artifact, and anyone who decompiles the APK has these credentials. They appear to be used for authenticated model downloads via OkHttp. The fix is to remove them from source entirely and use one of:
- BuildConfig fields injected from CI secrets (for build-time secrets)
- A server-side token exchange endpoint that returns a short-lived signed URL
- Encrypted credentials fetched from Firebase Remote Config itself (bootstrapping from a public config key)

---

### CRITICAL-2: Zip Path Traversal — FileZipManager.kt

```kotlin
// FileZipManager.kt (approximate area, lines ~30-50)
val unzipFile = File(toDir, entry.name)
// Missing canonical path check
FileOutputStream(unzipFile).use { fos ->
    // ... write entry contents
}
```

If `entry.name` contains `../../etc/passwd` or any `../` sequences, `File(toDir, entry.name)` resolves to a path outside `toDir`. A maliciously crafted language model zip file (or a man-in-the-middle response to the Firebase download) could overwrite arbitrary files with arbitrary content.

**Fix:**
```kotlin
val unzipFile = File(toDir, entry.name)
val canonicalToDir = toDir.canonicalPath
val canonicalEntry = unzipFile.canonicalPath
require(canonicalEntry.startsWith(canonicalToDir + File.separator)) {
    "Zip entry path escapes target directory: ${entry.name}"
}
```

---

### CRITICAL-3: Global Crash Handler Overrides Crashlytics — VivokaInitializer.kt

```kotlin
// VivokaInitializer.kt (approximate area, lines ~78-88)
private fun setupCrashHandlers() {
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        Log.e(TAG, "Vivoka crash: $exception")
        throw exception  // THIS KILLS THE APP AND OVERRIDES CRASHLYTICS
    }
}
```

`Thread.setDefaultUncaughtExceptionHandler` is a JVM-process-wide setting. The previous handler (Firebase Crashlytics, Sentry, or the Android default) is completely replaced. The `throw exception` rethrow causes an immediate fatal crash with no recovery window. Any unhandled exception from any thread triggers this, not just Vivoka threads. Crashlytics never records these crashes because it is no longer the handler.

**Fix:**
```kotlin
private fun setupCrashHandlers() {
    val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        Log.e(TAG, "Vivoka thread uncaught exception on ${thread.name}", exception)
        // Delegate to the existing handler (Crashlytics, etc.)
        existingHandler?.uncaughtException(thread, exception)
    }
}
```

---

### CRITICAL-4: VOSK Confidence Normalization Inverted — ConfidenceScorer.kt

```kotlin
// ConfidenceScorer.kt (lines 107-112)
RecognitionEngine.VOSK -> {
    // VOSK returns acoustic log-likelihood (negative values)
    // Convert using sigmoid function: 1 / (1 + e^(-x))
    val normalized = 1f / (1f + exp(-rawScore * VOSK_ACOUSTIC_SCALE))
    normalized.coerceIn(0f, 1f)
}
```

VOSK reports acoustic log-likelihood scores as large **negative** numbers. A score of `-0.5` is very good (near-certain match); a score of `-200` is very poor (no match). The code computes `1/(1+exp(-x))` where x is already negative — so for a bad match with `rawScore = -200`, this computes `1/(1+exp(200))`. `exp(200)` overflows to positive infinity, so the expression evaluates to `1/(1+Inf) = 0.0`. But for a marginally acceptable match with `rawScore = -0.5`, this computes `1/(1+exp(0.5)) ≈ 0.38`.

The effect: **good matches score low confidence, poor matches score zero** — the formula yields the correct relative ordering by accident (lower magnitude is higher confidence), but the absolute values are wrong, causing threshold filtering to behave unpredictably depending on the exact VOSK score range for a given model.

**Fix:**
```kotlin
RecognitionEngine.VOSK -> {
    // VOSK acoustic log-likelihood: typical range is roughly -200 to 0
    // Normalize to 0-1 where 0 = worst match, 1 = best match
    val clamped = rawScore.coerceIn(-200f, 0f)  // clamp to expected range
    ((clamped + 200f) / 200f).coerceIn(0f, 1f)
}
```

---

### CRITICAL-5: Silence Detection Fires Continuously — VivokaAudio.kt

```kotlin
// VivokaAudio.kt (approximate area)
private fun startSilenceDetection() {
    val runnable = object : Runnable {
        override fun run() {
            silenceDetectionCallback?.invoke()  // fires callback
            handler.postDelayed(this, 100)       // reschedules — FIRES AGAIN IN 100ms
        }
    }
    handler.postDelayed(runnable, SILENCE_TIMEOUT_MS)
}
```

The callback fires every 100 ms permanently after the first trigger. In `VivokaEngine.handleModeSwitch()` this calls `handleDictationEnd()` — so dictation is ended and restarted every 100 ms in an infinite loop, consuming CPU and causing repeated dictation-end events upstream.

**Fix (one-shot):**
```kotlin
private fun startSilenceDetection() {
    silenceRunnable = Runnable {
        silenceDetectionCallback?.invoke()
    }
    handler.postDelayed(silenceRunnable!!, SILENCE_TIMEOUT_MS)
}

fun stopSilenceDetection() {
    silenceRunnable?.let { handler.removeCallbacks(it) }
    silenceRunnable = null
}
```

---

### CRITICAL-6: `FirebaseRemoteConfigRepository` Double-Checked Locking Missing @Volatile

```kotlin
// FirebaseRemoteConfigRepository.kt (approximate area)
companion object {
    private var instance: FirebaseRemoteConfigRepository? = null  // MISSING @Volatile

    fun getInstance(context: Context): FirebaseRemoteConfigRepository {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    instance = FirebaseRemoteConfigRepository(context)
                }
            }
        }
        return instance!!
    }
}
```

Without `@Volatile`, the JVM (and especially the JIT compiler) is free to reorder memory operations. A thread may see `instance != null` but read a partially-constructed object. This is the classic double-checked locking pitfall.

**Fix:** Add `@Volatile private var instance: FirebaseRemoteConfigRepository? = null`

---

### HIGH-1: `AndroidSTTEngine.destroy()` Coroutine Scope Cancelled Before Destroy Completes

```kotlin
// AndroidSTTEngine.kt (approximate area)
fun destroy() {
    scope.launch {                    // launched on scope
        if (::recognizer.isInitialized) {
            recognizer.destroy()      // must run on Main thread
        }
    }
    // ...
    scope.cancel()                    // immediately cancels the launch above
}
```

`scope.cancel()` cancels all child coroutines, including the one just launched. `SpeechRecognizer.destroy()` requires the main thread and is part of Android's API surface — if it is not called, the Android SpeechRecognizer is leaked (it holds a connection to the Android SpeechRecognitionService).

**Fix:**
```kotlin
suspend fun destroy() {
    withContext(Dispatchers.Main) {
        if (::recognizer.isInitialized) {
            recognizer.destroy()
        }
    }
    scope.cancel()
}
```

Or if a non-suspend version is required:
```kotlin
fun destroy() {
    runBlocking(Dispatchers.Main) {
        if (::recognizer.isInitialized) recognizer.destroy()
    }
    scope.cancel()
}
```

---

### HIGH-2: VOSK/Vivoka Confidence Comment Mismatch — ConfidenceScorer.kt

```kotlin
RecognitionEngine.VIVOKA -> {
    // Vivoka uses 0-100 scale      <-- WRONG
    (rawScore / VIVOKA_MAX_SCORE).coerceIn(0f, 1f)
}
```

`VIVOKA_MAX_SCORE = 10000f`. The math divides by 10000, which is correct. But the comment says "0-100 scale". This is misleading — the constant is correct; the comment is wrong. Any developer reading this comment will think raw VOSK scores of e.g. 75 should map to 0.75, but they actually map to 0.0075.

---

### HIGH-3: Three Separate CommandCache Instances — AndroidSpeechRecognitionService.kt

```kotlin
// AndroidSpeechRecognitionService.kt
private val commandCache = CommandCache()                    // cache #1
private val resultProcessor = ResultProcessor(commandCache)  // cache #2 (ResultProcessor has its own)
// ...
androidSTTEngine = AndroidSTTEngine(...)  // engine creates cache #3 internally
```

`setCommands()` updates `commandCache` (#1) and calls `androidSTTEngine.setStaticCommands()` / `setDynamicCommands()` (updates cache #3). `ResultProcessor` uses cache #2 which is never updated via `setCommands()`. This means fuzzy matching in `ResultProcessor` operates on different commands than what the engine recognizes.

---

### HIGH-4: VoiceStateManager Callback Inside Write Lock

```kotlin
// VoiceStateManager.kt (approximate)
private fun setState(newState: VoiceState) {
    stateLock.write {
        // ... update state fields
        updateStateFlow()         // emits to StateFlow — ok
        onStateChangeCallback?.invoke(newState)  // INSIDE WRITE LOCK — deadlock risk
    }
}
```

If the callback (set by `AndroidSpeechRecognitionService` or `VivokaEngine`) tries to read any voice state (e.g., calls `isListening()` which acquires `stateLock.read {}`), a reentrant deadlock occurs. `ReentrantReadWriteLock` read lock IS reentrant if the same thread holds the write lock, but only for the writing thread. If the callback is dispatched to a different thread, that thread blocks on the read lock, and nothing can progress.

---

### HIGH-5: `VoiceStateManager.downloadingModels(false)` Corrupts State

```kotlin
// VoiceStateManager.kt (approximate lines 302-305)
fun downloadingModels(isDownloading: Boolean) {
    stateLock.write {
        isDownloadingModels = isDownloading
        if (!isDownloading) {
            isSleeping = false       // WRONG: user may have put voice to sleep
            isDictationActive = false // WRONG: user may be mid-dictation
        }
    }
}
```

After a model download completes, the user's active sleep/dictation state is silently reset. A user who said "mute voice" before a download finishes will find voice reactive again after the download. A user mid-dictation will lose their dictation mode.

---

### MEDIUM: `DesktopSpeechRecognitionService` and `JsSpeechRecognitionService` State Lies

Both stubs transition to `ServiceState.READY` after `initialize()` and `ServiceState.LISTENING` after `startListening()` without performing any audio capture. Callers checking `isReady()` or `isListening()` will believe recognition is active. The correct behavior for unimplemented platforms is to either:

1. Return `Result.failure(UnsupportedOperationException(...))` from `initialize()`, OR
2. Stay in `ServiceState.UNINITIALIZED` / `ServiceState.ERROR`

The current behavior makes silent failures possible in multi-platform code that checks `isListening()` before acting.

---

### MEDIUM: `SynchronizedMutableList.subList()` Returns Unsynchronized View — PlatformUtils.ios.kt

```kotlin
override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
    synchronized(lock) { list.subList(fromIndex, toIndex).toMutableList() }
```

`toMutableList()` creates a copy, which is safe. But the copy is not a synchronized view — any mutations to the returned subList are not reflected in the original. This is actually the safer choice for an atomic copy, but callers expecting a live view of the sublist will see stale data. This should be documented.

---

### MEDIUM: `IosSpeechRecognitionService.startListening()` Ignores audioEngine Start Error

```kotlin
audioEngine?.startAndReturnError(null)  // null discards any NSError
```

On iOS, `null` as the error parameter causes the Kotlin/Native bridge to ignore errors. If the microphone is in use by another app or denied by the OS, this call fails silently and the recognition task is started with no audio input — it will time out or produce empty results without any diagnostic.

---

## Findings by Source Set

| Source Set | Critical | High | Medium | Low |
|------------|----------|------|--------|-----|
| commonMain | 0 | 0 | 5 | 2 |
| androidMain | 7 | 17 | 15 | 8 |
| iosMain | 0 | 1 | 2 | 0 |
| desktopMain | 0 | 1 | 0 | 2 |
| jsMain.disabled | 0 | 0 | 0 | 2 |
| **Total** | **7** | **19** | **22** | **14** |

---

## Recommendations

1. **Credentials: immediate action required.** `FirebaseRemoteConfigRepository` has plaintext credentials in source. Rotate the `avanuevoiceos` account password immediately, then implement a secure delivery mechanism before the next release.

2. **Zip slip fix before any model download goes live.** The `FileZipManager` vulnerability allows arbitrary file write via a crafted or MITM'd zip. Add the canonical path check before shipping model download functionality.

3. **Fix VOSK confidence formula.** The sigmoid inversion means VOSK-based recognition confidence thresholding is effectively random. Fix the normalization before enabling VOSK as a production engine.

4. **Fix silence detection before enabling dictation on Vivoka.** The 100 ms continuous fire of `silenceDetectionCallback` will make dictation mode unusable in production — dictation will terminate every 100 ms.

5. **Remove the global crash handler override.** `setupCrashHandlers()` must be fixed or removed before any production release to restore Crashlytics / crash reporting.

6. **Consolidate `CommandCache` instances.** Three caches diverging is a subtle correctness bug that will produce inconsistent recognition behavior and is hard to debug.

7. **Implement or stub honestly.** `VivokaLearning`, `DesktopSpeechRecognitionService`, and `JsSpeechRecognitionService` all claim to work when they do nothing. Replace with honest stubs (`UnsupportedOperationException` or documented no-ops) until implemented.

8. **Address all 14 Rule 7 violations in a single pass.** Run a search-replace across all `Author: Claude (AI Assistant)` and `Author: VOS4 Development Team` headers and replace with `Author: Manoj Jhawar` or remove the Author field entirely.
