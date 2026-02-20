# Foundation Module — Deep Review
**Date:** 2026-02-20
**Files Reviewed:** 31 .kt files
**Source sets:** commonMain (20), androidMain (1), iosMain (4), desktopMain (4)
**Reviewer:** Code Review Agent

---

## Summary

The Foundation module provides the KMP platform abstraction layer (ISettingsStore, ICredentialStore,
IFileSystem, IPermissionChecker), shared state primitives (ViewModelState, ListState, UiState,
ServiceState), and utility classes (NumberToWords, HashUtils). The architecture is clean and the
codec pattern for settings serialization is well-designed.

However, the module has several correctness issues that range from critical to medium severity.
The most serious are: a non-atomic `update()` function on `ViewModelState` that creates silent data
races for every ViewModel in the app; NSUserDefaultsSettings and JavaPreferences stores that both
have race conditions in their `update()` paths; a hard-coded MITM-enabling default
(`DEFAULT_HOST_KEY_MODE = "no"`) in the shared settings model; and a `Long.MIN_VALUE`
overflow that will crash any caller passing minimum Long values to `NumberToWords.convert()`.

The desktop and iOS permission checker stubs unconditionally return `true` for all permissions,
including `isAccessibilityEnabled()`, which will cause incorrect behavior in any code that
gates functionality on permission checks. The `DesktopCredentialStore` claims to implement
`ICredentialStore` but stores credentials using only Base64 (not encryption), silently violating
the interface's security contract.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `commonMain/state/ViewModelState.kt:49-51` | `update()` is non-atomic: reads `_state.value`, transforms, then writes. Concurrent callers lose updates. | Replace with `_state.update { transform(it) }` which is atomic on `MutableStateFlow`. |
| Critical | `commonMain/util/NumberToWords.kt:205` | `convert(-number)` when `number == Long.MIN_VALUE` overflows back to `Long.MIN_VALUE`, causing infinite recursion → `StackOverflowError`. | Guard: `if (number == Long.MIN_VALUE) return "negative nine quintillion..."` or clamp before negation. |
| Critical | `commonMain/settings/models/AvanuesSettings.kt:74` | `DEFAULT_HOST_KEY_MODE = "no"` hard-codes disabled SSH host-key verification as the default for VOS sync. Every fresh install is MITM-vulnerable by default. | Change default to `"strict"`. Only relax when user explicitly opts in via settings UI. |
| Critical | `iosMain/settings/UserDefaultsSettingsStore.kt:49-55` | `update()` is not synchronized. Two concurrent calls race on decode→transform→encode. The losing transform is silently dropped. | Wrap `update()` body in a `Mutex` (coroutines `kotlinx.coroutines.sync.Mutex`) using `mutex.withLock { ... }`. |
| High | `desktopMain/settings/JavaPreferencesSettingsStore.kt:36-45` | `PreferenceChangeListener` is added in `init` but never removed. `Preferences` holds a strong reference to the listener, preventing GC of the store. No `close()`/`dispose()` on `ISettingsStore`. | Add `fun close()` to `ISettingsStore`, implement it to call `prefs.removePreferenceChangeListener(...)`. |
| High | `desktopMain/settings/JavaPreferencesSettingsStore.kt:39-45` | TOCTOU race: `if (!isUpdating)` reads `@Volatile isUpdating` outside the `synchronized` block, then enters `synchronized`. Between the volatile read and acquiring the lock, `isUpdating` can change. | Move the `!isUpdating` check inside the `synchronized` block. |
| High | `iosMain/settings/UserDefaultsSettingsStore.kt:37-47` | `NSNotificationCenter` observer added in `init` but never removed via `removeObserver`. Causes observer leak if the store is garbage-collected on the Kotlin/Native side. | Store the observer reference and call `NSNotificationCenter.defaultCenter.removeObserver(observer!!)` in a `close()` method. |
| High | `desktopMain/settings/DesktopCredentialStore.kt:26-29` | `ICredentialStore` contract requires encryption at rest. `DesktopCredentialStore` uses Base64 only (their own comment says "obfuscation only"). Violates interface security contract silently. | Integrate OS-native credential manager (Keychain on macOS via JNA, Credential Manager on Windows, SecretService on Linux). If not feasible, make `store()`/`retrieve()` throw `UnsupportedOperationException` on platforms without native credential support, and document clearly. |
| High | `desktopMain/platform/DesktopPermissionChecker.kt:16-23` | All methods return `true` unconditionally, including `isAccessibilityEnabled()`. On macOS this permission requires user grant in System Preferences; returning `true` causes features to assume access they don't have. | Implement real checks: use `java.awt.Toolkit` or `com.sun.glass.ui` for macOS accessibility, or check OS-specific APIs. At minimum, throw `UnsupportedOperationException` so callers know the check is not implemented. |
| High | `iosMain/platform/IosPermissionChecker.kt:18-19` | `hasPermission()` always returns `true`. iOS microphone/camera permission may not be granted; callers are misled. | Implement real checks using `AVAudioSession.sharedInstance().recordPermission`, `AVCaptureDevice.authorizationStatus(for:)`, etc. via the KMM Swift/Kotlin bridge or use Kotlin/Native cinterop. |
| High | `commonMain/viewmodel/BaseViewModel.kt:74` | `launchIO()` launches on `Dispatchers.Default` but is documented as "Use for database/network operations". `Dispatchers.Default` is CPU-bounded; I/O operations should use `Dispatchers.IO`. | Change `Dispatchers.Default` to `Dispatchers.IO` in `launchIO()`. |
| High | `iosMain/settings/KeychainCredentialStore.kt:85-95` | `hasCredential()` uses `kSecReturnData = true` — it retrieves and decrypts the full credential data just to check existence. This is a security (data unnecessarily decrypted) and performance issue. | Use `kSecReturnData = false` (or `kSecReturnAttributes = true`) for existence checks. |
| High | `iosMain/platform/IosFileSystem.kt:57-62` | `(content as NSString)` is an unsafe Kotlin-to-ObjC cast. A Kotlin `String` is bridged to `NSString` implicitly, but explicit casting via `as` is brittle and compiler-unsupported. | Use `NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding)` or `content.encodeToByteArray()` + `NSData.create(bytes=..., length=...)`. |
| High | `commonMain/state/UiState.kt:60-70` | If `scope` is cancelled while `execute()` is in progress, the coroutine is cancelled mid-flight. `isLoading.value = false` is never set, leaving the UI in a permanent loading state. | Add `try/finally` inside the `launch` block: `try { ... } finally { if (showLoading) isLoading.value = false }`. |
| Medium | `commonMain/state/ViewModelState.kt:49-51` | Related to the Critical above: `ListState.updateItem()`, `removeItem()`, `removeAll()` all capture a `var updated/removed/count` inside `state.update {}`. Since `state.update` is itself non-atomic, these return values are unreliable under concurrent use. | Fix parent `update()` atomicity first, then verify captured-var pattern is safe post-fix. |
| Medium | `commonMain/util/NumberToWords.kt:188` | `defaultSystem: var` on a singleton `object` is mutable global state. Not thread-safe; concurrent callers that change it will race. | Make it a `@Volatile var` or better yet remove the mutable global and always pass `system` explicitly. |
| Medium | `commonMain/util/NumberToWords.kt:409-411` | `formatWesternGrouping()` does not handle negative numbers. `-1234` would produce malformed output like `-,1234`. | Add `val isNegative = number < 0; val abs = if (isNegative) -number else number` guard and prepend `-` to result. |
| Medium | `iosMain/platform/IosFileSystem.kt:51-54` | `readText()` dispatches to `Dispatchers.Default` instead of `Dispatchers.IO`. File I/O blocks threads and should not run on the CPU-bound default dispatcher. | Change to `Dispatchers.IO`. |
| Medium | `iosMain/platform/IosFileSystem.kt:65` | `fileManager.removeItemAtPath(path, error = null)` silently discards deletion errors (permission denied, locked file, etc.). | Use `memScoped { val err = alloc<ObjCObjectVar<NSError?>>(); val ok = fileManager.removeItemAtPath(path, error = err.ptr); if (!ok) throw IOException(err.value?.localizedDescription ?: "delete failed") }`. |
| Medium | `commonMain/state/ServiceState.kt:87` | Default `metadata` property implementation creates a `new MutableStateFlow(emptyMap())` every time the property is accessed. Observers collecting from `provider.metadata` will see only one emission and never update, because each access returns a new, separate flow instance. | Change the default to `val metadata: StateFlow<Map<String, String>> get() = _emptyMetadataFlow` where `_emptyMetadataFlow` is a companion/static val. Or document that implementors MUST override this property. |
| Medium | `commonMain/state/SearchState.kt:67-77` | Doc comment implies debounce support but none is implemented. Rapid calls to `search()` fire a coroutine for every keystroke. On Android this would flood the repository. | Add `debounce` via `kotlinx.coroutines.flow.debounce` or document that callers must debounce. |
| Medium | `commonMain/state/SearchState.kt:82-87` | `clear(scope)` launches a coroutine on the provided scope. If the caller's scope is cancelled before the lambda completes, `onEmpty()` is never called, leaving stale search results displayed. | Use `scope.launch` with `try/catch CancellationException` or document this limitation. |
| Medium | `iosMain/settings/UserDefaultsSettingsStore.kt:71-72` | `getLong()` uses `defaults.integerForKey(key)` which maps to `NSInteger`. On historical 32-bit iOS devices, `NSInteger` is 32-bit. Any `Long` value stored via `putLong()` that exceeds `Int.MAX_VALUE` will silently truncate on read. | Store `Long` as a `String` in `NSUserDefaults` (call `setObject(value.toString(), forKey = key)` and parse back on read) to avoid truncation. |
| Medium | `desktopMain/settings/JavaPreferencesSettingsStore.kt:50-63` | `update()` calls `loadSettings()` (reads from `prefs`) inside the `synchronized` block. `java.util.prefs.Preferences` I/O is itself synchronized internally and can block. Holding `synchronized(this)` while blocking on I/O increases deadlock risk. | Remove the `synchronized` wrapper and use a `kotlinx.coroutines.sync.Mutex` via `mutex.withLock {}` instead, which is coroutine-safe and non-blocking. |
| Medium | `commonMain/settings/models/DeveloperSettings.kt:26` | `debugMode: Boolean = true` — debug mode is `true` by default. This means debug logging is active for all production installs unless explicitly disabled. | Default `debugMode = false`. Only flip to `true` when developer mode is activated. |
| Medium | `commonMain/viewmodel/BaseViewModel.kt:86` | Default `onError` in `observe()` is a silent swallow `{ /* default: silent */ }`. Errors in Flows are silently discarded unless callers pass a handler. This hides exceptions in observable streams. | At minimum change the default to log the error: `{ e -> Log.e(...) }` or use the Logging module. |
| Low | `commonMain/state/UiState.kt:136-148` | `handleWith()` extension is a `suspend fun` that mutates `uiState` state properties directly. If called from a non-Main dispatcher, `ViewModelState` writes happen off-Main, which is safe for `MutableStateFlow` but callers may not expect this. | Document the dispatcher expectation or use `withContext(Dispatchers.Main)` for state mutations. |
| Low | `commonMain/util/NumberToWords.kt:362-390` | `parseCompound()` parsing algorithm works for simple cases but fails for numbers like "one trillion two billion three million four thousand five hundred sixty seven" if any intermediate `value >= 1000` resets `current` and adds to `result` correctly, but "one billion nine hundred ninety nine million" will work whereas "one hundred twenty three" passes through a single `current` accumulation — validated. However, `billion` and `lakh` are both in `wordToNumber` at the same level (both `>= 1000`). Parsing "one lakh five thousand" returns `105000` correctly, but "five thousand one lakh" would return `105000` too (addition is commutative), which is numerically correct but semantically odd. Acceptable but worth noting. | Add round-trip tests for all supported number systems. |
| Low | `commonMain/util/HashUtils.kt:44-46` | `isValidHash()` only validates lowercase hex. `sha256Impl` implementations on Android/Desktop use `"%02x"` format so output is always lowercase. iOS pure implementation also produces lowercase. However if any future implementation returns uppercase, `isValidHash()` returns `false` for a valid hash. | Use `it in '0'..'9' \|\| it in 'a'..'f' \|\| it in 'A'..'F'` or normalize to lowercase before checking. |
| Low | `iosMain/platform/IosFileSystem.kt:69-74` | `listFiles()` raises `IllegalStateException` if the directory doesn't exist. The interface doc says "throws Exception if the path is not a directory or can't be read" — this is consistent but the desktop implementation (`Files.list()`) would throw `java.nio.file.NoSuchFileException` instead. Inconsistent exception types across platforms. | Define a `FoundationException` hierarchy (FileNotFoundException, PermissionDeniedException) in commonMain and map platform exceptions to it. |
| Low | `commonMain/settings/SettingsMigration.kt:49-53` | `migrateVariantToStyle()` maps both `OCEAN` and `SUNSET` to `"Glass"`. The `TERRA` palette has no migration path — an old variant named `TERRA` (if it existed) would silently fall through to `DEFAULT_THEME_STYLE`. | Audit all legacy variant names and add explicit mappings or log a warning on unknown variants. |

---

## Detailed Findings — Critical and High Issues

### CRITICAL 1: Non-atomic `ViewModelState.update()`
**File:** `commonMain/kotlin/com/augmentalis/foundation/state/ViewModelState.kt:49-51`

```kotlin
// CURRENT (DATA RACE):
inline fun update(transform: (T) -> T) {
    _state.value = transform(_state.value)  // Read and write are NOT atomic
}

// FIX:
inline fun update(crossinline transform: (T) -> T) {
    _state.update { transform(it) }  // MutableStateFlow.update() is atomic
}
```

This function is used by `ListState.add()`, `addFirst()`, `addAll()`, `updateItem()`, `removeItem()`,
`removeAll()`, and `updateAll()` — essentially every mutable operation in `ListState`. Under
concurrent ViewModel coroutines, two simultaneous `downloads.add(item)` calls can both read the
same list, both append to it, and one write wins, silently dropping the other item.

**Impact:** Data loss in any ViewModel that uses `ListState` from concurrent coroutines. This
includes the entire app layer.

---

### CRITICAL 2: `Long.MIN_VALUE` overflow in `NumberToWords.convert()`
**File:** `commonMain/kotlin/com/augmentalis/foundation/util/NumberToWords.kt:205`

```kotlin
// CURRENT (INFINITE RECURSION):
if (number < 0L) return "negative ${convert(-number, system)}"
// When number == Long.MIN_VALUE: -Long.MIN_VALUE == Long.MIN_VALUE (overflow)
// → infinitely recursive → StackOverflowError

// FIX:
if (number < 0L) {
    return if (number == Long.MIN_VALUE) {
        "negative nine quintillion two hundred twenty-three quadrillion..."
    } else {
        "negative ${convert(-number, system)}"
    }
}
```

---

### CRITICAL 3: Default SFTP host key mode is "no" (MITM-enabling)
**File:** `commonMain/kotlin/com/augmentalis/foundation/settings/models/AvanuesSettings.kt:74`

```kotlin
// CURRENT (INSECURE DEFAULT):
const val DEFAULT_HOST_KEY_MODE = "no"

// FIX:
const val DEFAULT_HOST_KEY_MODE = "strict"
```

`DEFAULT_HOST_KEY_MODE = "no"` is the value passed to VosSftpClient's `hostKeyChecking` parameter
(flagged separately in `VosSftpClient.kt`). Having the insecure value as the model default means
every fresh install silently disables host key checking for VOS file sync. An attacker on the local
network can intercept and modify VOS command files being synced to the device without any warning.
This is the root-cause model default that needs fixing here.

---

### CRITICAL 4: `UserDefaultsSettingsStore.update()` has no synchronization
**File:** `iosMain/kotlin/com/augmentalis/foundation/settings/UserDefaultsSettingsStore.kt:49-55`

```kotlin
// CURRENT (RACE CONDITION):
override suspend fun update(block: (T) -> T) {
    val current = codec.decode(reader)   // Read
    val updated = block(current)          // Transform
    codec.encode(updated, writer)          // Write — concurrent caller may overlap
    defaults.synchronize()
    _settings.value = updated
}

// FIX: Add a Mutex
private val mutex = Mutex()

override suspend fun update(block: (T) -> T) = mutex.withLock {
    val current = codec.decode(reader)
    val updated = block(current)
    codec.encode(updated, writer)
    defaults.synchronize()
    _settings.value = updated
}
```

---

### HIGH 1: Memory leak — `JavaPreferencesSettingsStore` listener never removed
**File:** `desktopMain/kotlin/com/augmentalis/foundation/settings/JavaPreferencesSettingsStore.kt:39-46`

The `PreferenceChangeListener` lambda is added to `prefs` in `init {}` and never removed.
`java.util.prefs.Preferences` holds a strong reference to the listener, preventing GC of the store.
Since there is no `close()` method on `ISettingsStore`, callers have no way to release it.

Fix: Add `fun close()` to `ISettingsStore` and implement it in all stores to remove listeners.
The Android DataStore equivalent does not need explicit cleanup (it's scope-bound), so the
interface method can default to a no-op.

---

### HIGH 2: Memory leak — `UserDefaultsSettingsStore` observer never removed
**File:** `iosMain/kotlin/com/augmentalis/foundation/settings/UserDefaultsSettingsStore.kt:37-47`

```kotlin
// CURRENT (LEAK):
observer = NSNotificationCenter.defaultCenter.addObserverForName(
    name = NSUserDefaultsDidChangeNotification, ...
) { _ -> _settings.value = codec.decode(reader) }
// observer is stored but removeObserver is never called

// FIX: Add close() or deinit-equivalent
fun close() {
    observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
    observer = null
}
```

---

### HIGH 3: `DesktopCredentialStore` — Base64 is not encryption
**File:** `desktopMain/kotlin/com/augmentalis/foundation/settings/DesktopCredentialStore.kt`

The `ICredentialStore` contract (stated in `ICredentialStore.kt:17-19`): "All values are stored
encrypted at rest." `DesktopCredentialStore` encodes with Base64, which is trivially reversible
by anyone with file system access. The SFTP password and private key passphrase stored via this
class are effectively plaintext on disk. Any VOS sync credentials stored on desktop are exposed.

This is particularly dangerous because `VOS_SFTP_KEY_PATH` and SFTP credentials can be stored
through this path.

---

### HIGH 4: `launchIO()` uses wrong dispatcher
**File:** `commonMain/kotlin/com/augmentalis/foundation/viewmodel/BaseViewModel.kt:74`

```kotlin
// CURRENT (BUG): uses CPU-bound dispatcher for I/O
protected fun launchIO(block: suspend CoroutineScope.() -> Unit) {
    viewModelScope.launch(Dispatchers.Default, block = block)
}

// FIX:
protected fun launchIO(block: suspend CoroutineScope.() -> Unit) {
    viewModelScope.launch(Dispatchers.IO, block = block)
}
```

`Dispatchers.Default` has a thread pool bounded by CPU count (typically 4-8). Blocking I/O on this
dispatcher starves CPU-bound coroutines and can cause UI jank. Every ViewModel that calls
`launchIO { repository.loadFromDatabase() }` is putting database I/O on the wrong thread pool.

---

### HIGH 5: `KeychainCredentialStore.hasCredential()` retrieves data unnecessarily
**File:** `iosMain/kotlin/com/augmentalis/foundation/settings/KeychainCredentialStore.kt:85-95`

```kotlin
// CURRENT: Full data retrieval just for existence check
setObject(kCFBooleanTrue!!, forKey = kSecReturnData as NSCopyingProtocol)  // Decrypts value

// FIX: Check for attributes only
setObject(kCFBooleanTrue!!, forKey = kSecReturnAttributes as NSCopyingProtocol)  // No decryption
```

The Keychain decrypts the stored credential on every `hasCredential()` call. This is unnecessary
work and unnecessarily exposes decrypted data in memory.

---

### HIGH 6: `UiState.execute()` leaks loading state on scope cancellation
**File:** `commonMain/kotlin/com/augmentalis/foundation/state/UiState.kt:60-70`

```kotlin
// CURRENT: if scope cancelled mid-operation, isLoading never reset
scope.launch {
    if (showLoading) isLoading.value = true
    // ... scope cancelled here ...
    if (showLoading) isLoading.value = false  // Never reached
}

// FIX:
scope.launch {
    if (showLoading) isLoading.value = true
    try {
        operation()
            .onSuccess { saveSuccess.value = true }
            .onFailure { e -> error.value = e.message ?: "Unknown error" }
    } finally {
        if (showLoading) isLoading.value = false
    }
}
```

Same fix needed in `executeWithMessage()` at lines 82-93.

---

## Rule 7 Violations

None found. All file headers use "Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC" or omit
author entirely. No AI/Claude attribution present.

---

## Test Coverage Assessment

No test files found in `Foundation/src/`. The codec, migration, and NumberToWords utilities are
pure functions well-suited for unit testing. At minimum the following should be tested:

- `NumberToWords`: edge cases (0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE), all three number systems,
  `convertWithSuffix()` for each prefix/suffix currency
- `SettingsMigration`: all legacy variant names including unknown variants
- `AvanuesSettingsCodec` and `DeveloperSettingsCodec`: round-trip encode→decode
- `HashUtils.isValidHash()`: valid and invalid hash strings
- `ViewModelState.update()`: concurrent update correctness (after atomicity fix)

---

## Summary Counts

| Severity | Count |
|----------|-------|
| Critical | 4 |
| High | 9 |
| Medium | 11 |
| Low | 4 |
| **Total** | **28** |
