# WebAvanue Module — Deep Code Review
**Date:** 260220
**Scope:** `Modules/WebAvanue/src/` (commonMain + androidMain + iosMain, ~280 .kt files)
**Reviewer:** Code-Reviewer Agent
**Branch:** HTTPAvanue

---

## Summary

WebAvanue is a mature KMP browser module with strong architectural foundations: a well-structured
repository layer, a Mutex-protected KMP ViewModel, a functional DOM scraping pipeline protected
under Mandatory Rule #1, and broad AVID semantics coverage across 190 occurrences in 49 UI files.
However, the module contains several security vulnerabilities that require immediate attention,
including XSS-class JavaScript injection bugs in the VoiceOS bridge and find-in-page controller,
broken CBC encryption with a static/deterministic IV in SecureScriptLoader, and a credentials
store that defaults to unencrypted SharedPreferences. Additionally, a set of high-impact runtime
bugs — most critically a wrong-list index in the disambiguation flow and two fire-and-forget
async patterns in evaluateJavaScript — would cause silent wrong behavior or data loss.

A systematic perimeter issue is the parallel theme system: the module maintains its own
`AppTheme`/`AppColors`/`WebAvanueColors`/`OceanDialog` stack with hardcoded hex colors, bypassing
AvanueTheme v5.1 entirely. While the module-specific Ocean Blue branding intent is documented in
`WebAvanueColors.kt`, the `OceanDialog` component uses raw `MaterialTheme.typography` references
and hardcoded color literals, making it incompatible with the project-wide theme switching rule.
The iOS platform additionally ships two stubs that report success without doing any real work:
`WebAvanueRpcServer.ios.kt` sets `running = true` but starts no server, and
`WebViewPoolManager.ios.kt` exposes a different API surface than the `expect` declaration, causing
a compile-time contract mismatch. No Rule 7 violations (AI/Claude attribution) were found anywhere
in the module.

---

## Issues Table

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `androidMain/.../WebAvanueVoiceOSBridge.kt` | `clickBySelector(selector)` injects selector directly into JS: `document.querySelector('$selector')` — no escaping, single-quote in selector breaks JS / enables injection | Escape via `escapeJs()` before interpolation; reject selectors containing unbalanced quotes |
| Critical | `androidMain/.../WebAvanueVoiceOSBridge.kt` | Same unescaped injection in `focusElement(selector)`, `inputText(selector, value)`, `scrollToElement(selector)` | Apply `escapeJs()` to all selector and value arguments before JS interpolation |
| Critical | `androidMain/.../AndroidWebViewController.kt:173` | `findInPage(query)` injects raw query into JS: `window.find('$query', ...)` — search query with `'` or `` ` `` escapes the string literal | Escape query before interpolation or use `evaluateJavaScript` with proper parameter passing |
| Critical | `androidMain/.../SecureScriptLoader.kt` | AES/CBC used with IV derived deterministically from `MD5("${packageName}:${Build.FINGERPRINT}")` — static IV per device means same plaintext always produces same ciphertext, breaking confidentiality | Switch to AES/GCM with random 12-byte IV prepended to ciphertext per operation; use SHA-256 (not MD5) if a deterministic value is needed as a key derivation input |
| Critical | `androidMain/.../SecureScriptLoader.kt:72-73` | MD5 used for IV derivation — MD5 is cryptographically broken (collision attacks, no pre-image resistance) | Replace with SHA-256 at minimum; or move to AES-GCM with random IV (see above) |
| Critical | `androidMain/.../AndroidWebView.kt:222` | `WebViewFactory.createWebView()` throws `NotImplementedError("Use WebViewComposable instead for Android")` — violates Rule 1 (no stubs) | Implement the factory to create the Android WebView, or delete the expect/actual and use only the `@Composable` path if factory is not needed |
| Critical | `iosMain/.../rpc/WebAvanueRpcServer.kt:22-25` | iOS RPC `start()` sets `running = true` and logs nothing — no network server is started; `isRunning()` returns `true` falsely. Any caller that checks `isRunning()` to decide if RPC is available will get wrong answer | Either implement iOS RPC (e.g., via HTTPAvanue), or make `start()` throw `UnsupportedOperationException` so callers can detect the gap, not silently proceed |
| Critical | `iosMain/.../WebViewPoolManager.ios.kt` | `actual object WebViewPoolManager` declares `preWarmWebView()`, `getWebView()`, `returnWebView()`, `clearPool()`, `getPoolSize()` — but the `expect` in commonMain declares `removeWebView(tabId)`, `clearAllWebViews()`, `clearCookiesOnExit()`. This is a compile-time API contract mismatch | Reconcile the expect/actual surfaces: decide which API belongs in commonMain and implement the other platform to match |
| Critical | `commonMain/.../BrowserVoiceOSCallback.kt` | `commandGenerator` (plain `mutableListOf` + `mutableMapOf`) is mutated in `onDOMScraped()` on the WebView main thread while `persistCommands()` and `checkDatabaseCache()` coroutines on `Dispatchers.Default` call `commandGenerator.getAllCommands()` concurrently — unsynchronized data race | Protect `commandGenerator` mutations behind a `kotlinx.coroutines.sync.Mutex` with `withLock` in all callers, or use a `ConcurrentHashMap` + `CopyOnWriteArrayList` |
| High | `commonMain/.../BrowserVoiceOSCallback.kt:758-762` | `selectDisambiguationOption(index)` indexes into `commandGenerator.getAllCommands()` (full command list, potentially thousands of commands) instead of the disambiguation match list — will execute entirely the wrong command for any index > 0 | Store the last disambiguation matches in a field (`_pendingDisambiguationMatches`) and index into that list |
| High | `androidMain/.../AndroidWebView.kt:151-157` | `evaluateJavaScript()` is a `suspend fun` that sets `result` inside the DownloadManager callback — the suspend function has already returned before the callback fires; result is always null | Use `suspendCancellableCoroutine` with `resume(value)` inside the callback; cancel with `null` on error |
| High | `iosMain/.../IOSWebView.kt:197-204` | Same pattern: `evaluateJavaScript()` assigns `result` in the WKWebView callback lambda but the function returns immediately; result is always null | Same fix: `suspendCancellableCoroutine { cont -> webView.evaluateJavaScript(script) { v, e -> cont.resume(v?.toString()) } }` |
| High | `iosMain/.../IOSWebView.kt:207-221` | `captureScreenshot()` assigns `imageData` in the WKSnapshotConfiguration completion handler but returns before it fires — always returns null | Use `suspendCancellableCoroutine` |
| High | `androidMain/.../AndroidWebView.kt` | `setDesktopMode(false)` sets `settings.userAgentString = webView.settings.userAgentString` (the current UA, which is already desktop) — once in desktop mode, toggling back to mobile keeps the desktop UA | Store the original UA before first modification and restore that stored value |
| High | `androidMain/.../SecureStorage.kt` | Default `useEncryption = false` — credentials (passwords) stored in plaintext SharedPreferences by default; encrypted storage is opt-in | Change default to `useEncryption = true`; fall back to unencrypted only if `EncryptedSharedPreferences` init fails with explicit error logging |
| High | `androidMain/.../SecureStorage.kt` | URL key normalization strips protocol only (`replace("https://", "").replace("http://", "")`) — `https://bank.com/login` and `https://bank.com/dashboard` produce the same key `bank.com/login` and `bank.com/dashboard` — actually differ, but `bank.com` and `bank.com/` produce same key causing credential collisions for root vs trailing-slash URLs | Normalize to scheme+host only, OR use URL parsing (avoid manual string manipulation) |
| High | `androidMain/.../SecureStorage.kt` | `println("✅ SecureStorage: Credentials stored for $normalizedUrl")` — credential operations logged to Logcat (visible in `adb logcat`) | Replace `println` with `Log.d` gated behind a debug flag; never log normalized credential keys |
| High | `commonMain/.../GestureMapper.kt` | `GESTURE_COPY` → `"await window.AvanuesGestures.copy()"`, `GESTURE_CUT` → `"await window.AvanuesGestures.cut()"` — `await` used as a top-level statement in a non-async script context; `evaluateJavascript` runs this as a regular expression, causing `SyntaxError: await is only valid in async functions` | Wrap in an IIFE: `"(async () => { await window.AvanuesGestures.copy(); })()"` |
| High | `commonMain/.../BrowserRepositoryImpl.kt` | `getDatabaseSize()` always returns `Result.success(0L)` — hardcoded stub | Implement using `DatabaseDriver.getDatabaseFile().length()` or equivalent SQLDelight `PRAGMA page_count * page_size` query |
| High | `androidMain/.../WebViewLifecycle.kt` | `WebViewPool.get(tabId)` reads from `LinkedHashMap` without synchronization — the `@Synchronized` annotation is on `put()`/`remove()` but NOT on `get()` — concurrent reads during eviction cause race | Mark `get()` as `@Synchronized` or replace the map with `Collections.synchronizedMap()` wrapping |
| High | `androidMain/.../WebViewLifecycle.kt` | `removeEldestEntry` calls `Handler(Looper.getMainLooper()).post { eldest.value.destroy() }` — async post means the WebView may still be handed out to a new request before `destroy()` runs | Call `eldest.value.destroy()` synchronously on the main thread or use a flag to mark the WebView as destroyed before eviction |
| High | `androidMain/.../WebViewLifecycle.kt` | `Parcel.marshall()` / `Parcel.unmarshall()` used to serialize WebView back/forward state — this is an Android private API with no ABI stability guarantees; can crash on future Android versions | Use only the supported `WebBackForwardList` for display and the standard `WebView.saveState(Bundle)` / `restoreState(Bundle)` API |
| High | `androidMain/.../AndroidWebViewController.kt` | `clearCookiesInternal()` sets `success = false` inside async callback but checks `success` after — the function has already returned `true` (the `Result.success(true)` path runs before the callback fires) | Use `suspendCancellableCoroutine` pattern; resume with `false` inside callback if cookies-cleared flag is false |
| High | `commonMain/.../AdBlocker.kt` + `TrackerBlocker.kt` | `blockedCount: Int` is a plain `var` accessed from `WebView` resource-interception callbacks which may fire on multiple threads — no `@Volatile` and no synchronization | Use `AtomicInt` (or `@Volatile` with compare-and-set) for the counter |
| High | `commonMain/.../TrackerBlocker.kt` | Patterns `Regex(".*track.*")`, `Regex(".*pixel.*")`, `Regex(".*analytics.*")` are extremely broad — will block `tracking.css` on legitimate news sites, `pixel.nytimes.com` images, `analytics.google.com` (intentional), but also `pixelart.com`, `stackexchange.com/analytics` (false positives) | Replace `.*track.*` with domain/path anchored patterns; use a curated blocklist (EasyList / EasyPrivacy format) instead of generic word matching |
| High | `commonMain/.../AdBlocker.kt` | Pattern `Regex(".*ad\\..*")` will block `https://address.com`, `https://gradebook.example.com/add.html`, and similar legitimate URLs containing "ad." | Anchor pattern to known ad-serving domains; do not use substring matching on full URL strings |
| High | `androidMain/.../rpc/WebAvanueJsonRpcServer.kt` | `isRunning` is a plain `var Boolean` read from coroutine scope and `stop()` on potentially different threads — data race | Replace with `@Volatile var` or `AtomicBoolean` |
| High | `androidMain/.../rpc/WebAvanueJsonRpcServer.kt` | Raw TCP socket server on port 50055 with no authentication and no TLS — any process on the same device can send arbitrary RPC commands to control the browser | Require at minimum a session token negotiated at startup; ideally bind to loopback only and document the security model |
| High | `iosMain/.../VoiceCommandService.ios.kt` | `startListening()` and `speak()` are no-ops (just `println()`); `isListening()` always returns `false` — iOS voice command service is non-functional; callers get no error | Either implement via `SFSpeechRecognizer` / `AVSpeechSynthesizer`, or throw `UnsupportedOperationException` so callers can branch |
| Medium | `commonMain/.../AppTheme.kt` + `AppColors.kt` + `OceanDialog.kt` + `WebAvanueColors.kt` + `AvaMagicColors.kt` | Module maintains a parallel color/theme system (`AppColors`, `AppTheme`, `LocalAppColors`, `WebAvanueColors`) with hardcoded hex color literals. `OceanDialog.kt` uses `MaterialTheme.typography.headlineSmall` directly. None of these use `AvanueTheme.colors.*` or `AvanueColorPalette` | Migrate to AvanueTheme v5.1: map `WebAvanueColors` to a custom `AvanueColorPalette` entry; replace `OceanDialog` color literals with `AvanueTheme.colors.*`; delete `AppTheme`/`AppColors` parallel system |
| Medium | `commonMain/.../AvaMagicColors.kt` | `AvanuesThemeService.getCurrentTheme()` returns hardcoded colors with a `TODO: Query VoiceOS theme service` comment — ecosystem theme integration is not implemented | Implement actual integration with `AvanueTheme` DataStore keys (`theme_palette`, `theme_style`, `theme_appearance`) or delete the class and consolidate into `WebAvanueColors` |
| Medium | `commonMain/.../TabViewModel.kt:407-412,462` | `scrollUp()`, `scrollDown()`, `scrollLeft()`, `scrollRight()`, `scrollToTop()`, `scrollToBottom()`, `freezePage()` are all empty no-op stubs | Implement via `IWebViewController.scrollBy(dx, dy)` dispatch or document as "deferred to WebView native scroll" and remove the stubs if not needed |
| Medium | `commonMain/.../TabViewModel.kt:548-561` | `captureScreenshot()` logs "Capturing screenshot" but performs no action — stub | Dispatch to `IWebViewController.captureScreenshot()` and update `_screenshotResult` StateFlow |
| Medium | `commonMain/.../BookmarkImportExport.kt` | `parseHtmlWithData()` (~90 lines) duplicates all HTML parsing logic from `importFromHtml()` (~60 lines); `parseBookmarkEntryInternal()` / `parseFolderEntryInternal()` duplicate `parseBookmarkEntry()` / `parseFolderEntry()` — ~200 lines of copy-pasted parsing code | Extract shared parsing into private helper `parseBookmarkNode(node, addTimestamps: Boolean): Bookmark?` and call from both paths |
| Medium | `commonMain/.../VoiceCommandService.kt` | `normalized == "favorites"` matches BOTH `ShowFavorites` (line 110) and `OpenBookmarks` (line 123) in the `when` block — `OpenBookmarks` branch is dead code (first match always wins) | Change `OpenBookmarks` to match `"bookmarks"` or `"open bookmarks"` to distinguish from `"favorites"` |
| Medium | `commonMain/.../VoiceCommandGenerator.kt` | `commands` (`mutableListOf`) and `wordIndex` (`mutableMapOf`) are plain collections with no thread safety; `addElements()` and `findMatches()` called from different coroutine contexts | Protect access with `Mutex`; or build commands as immutable snapshots and replace atomically |
| Medium | `commonMain/.../VoiceCommandGenerator.kt` | `calculateMatchScore()` does bidirectional prefix matching: `commandWords[i].startsWith(spokenWords[i]) || spokenWords[i].startsWith(commandWords[i])` — spoken word `"a"` will match command word `"about"`, `"add"`, `"any"` giving spuriously high scores | Use prefix matching only from spoken→command direction (user abbreviates commands, not vice versa) |
| Medium | `commonMain/.../BrowserVoiceOSCallback.kt` | `computeUrlHash()` uses `String.hashCode()` (32-bit, non-cryptographic) — trivial collisions; two different URLs with the same `hashCode()` would overwrite each other's cached commands | Replace with SHA-256 truncated to 64 bits (16 hex chars) for collision resistance |
| Medium | `commonMain/.../ScreenshotManager.kt` | `state: ScreenshotState` is a plain `var` class field — no reactive updates; callers must poll or register callbacks separately | Replace with `MutableStateFlow<ScreenshotState>` |
| Medium | `androidMain/.../SecureScriptLoader.kt` | `clearCache()` calls `System.gc()` which does NOT guarantee clearing decrypted `String` objects from memory (JVM GC hint only) | Use `CharArray` / `ByteArray` for sensitive script content and call `fill(0)` on explicit clear; avoid `String` which is immutable and cannot be zeroed |
| Medium | `androidMain/.../XRManager.kt` | `CoroutineScope(Dispatchers.Main + SupervisorJob())` created in constructor — never cancelled; `LifecycleObserver.ON_DESTROY` stops performance monitoring but does not cancel the scope; scope leaks if activity is recreated | Cancel the scope in `ON_DESTROY`: `scope.cancel()` |
| Medium | `androidMain/.../DatabaseDriver.kt` | `migratePlaintextToEncrypted()` anonymous `SqlDriver` has `executeQuery()` throwing `UnsupportedOperationException("Not needed for migration")` — if `BrowserDatabase.Schema.create()` internally calls `executeQuery` during schema migration, the entire migration crashes silently | Implement a passthrough `executeQuery()` that delegates to the read-only source driver |
| Medium | `commonMain/.../BrowserRepositoryImpl.kt` | `clearAllData()` manually enumerates all 35+ settings fields in raw SQL — schema knowledge is duplicated; if new settings fields are added this function is silently incomplete | Add a `SettingsRepository.resetToDefaults()` method and call it instead |
| Medium | `commonMain/.../BrowserRepositoryImpl.kt` | `initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` — `cleanup()` cancels the Job but the Scope itself is never cancelled via `scope.cancel()`; lingering coroutines may continue after `cleanup()` returns | Call `initScope.cancel()` in `cleanup()` |
| Medium | `iosMain/.../IOSWebView.kt:115-147` | KVO observer registered on `webView.addObserver(...)` for `"estimatedProgress"` key only — but `observeValueForKeyPath` handles 6 different key paths (`loading`, `title`, `URL`, `canGoBack`, `canGoForward`, `estimatedProgress`). Only `estimatedProgress` is actually registered; the other 5 will never fire | Register the observer for all 6 key paths, or use one `addObserver` call per key path |
| Medium | `iosMain/.../IOSWebView.kt:175-178` | `clearHistory()` loads `""` (blank HTML) into the WebView — this navigates to a blank page as a side effect; the back/forward list is cleared but the current page content is also destroyed | Consider using `WKWebView.backForwardList` APIs or navigate to the actual current URL after clearing state |
| Medium | `iosMain/.../IOSWebView.kt:224-231` | `findInPage(text)` in iOS injects `window.find('$text', ...)` with no JS escaping — same injection class as Android `findInPage` | Apply JS string escaping to `text` before interpolation |
| Medium | `iosMain/.../IOSWebView.kt:331-378` | `WebViewUIDelegate`: JS `alert()` creates and configures a `UIAlertController` but then calls `completionHandler()` before presenting it (no view controller reference available) — alert dialog is never shown to user; all JS alerts silently succeed | Require a `UIViewController` reference in the delegate constructor and call `present(alertController, animated: true)` before calling `completionHandler` inside the OK handler |
| Medium | `androidMain/.../WebAvanueVoiceOSBridge.kt` | `println()` used for all diagnostic logging (4+ occurrences) | Replace with `Log.d("WebAvanueVoiceOSBridge", ...)` |
| Medium | Across 8+ files | `println()` used for logging in: `WebViewLifecycle.kt` (10+ calls), `DatabaseDriver.kt` (5+ calls), `BrowserVoiceOSCallback.kt` (15+ calls), `SecureStorage.kt` (4 calls), `RetryPolicy.kt` (2 calls), `DownloadHelper.kt` (2 calls), `VoiceCommandService.ios.kt` (2 calls) | Replace all `println()` with `Napier.d/e/w()` (KMP-compatible) or `Log.d/e` (Android) |
| Low | `commonMain/.../CertificatePinningHandler.kt` | `reportToMonitoring()` and `showSecurityAlert()` are private methods that are commented out and never called — dead code | Delete the commented-out implementations; add a proper `onPinningFailure(host)` callback parameter if monitoring is desired |
| Low | `commonMain/.../DownloadQueue.kt` | `FilenameUtils.decodeUrlEncoded()` decodes only single-byte `%XX` sequences using `toInt(16).toChar()` — multi-byte UTF-8 percent-encoded sequences (e.g., `%E2%80%99` for Unicode right-apostrophe) will be decoded as three garbage characters | Use `java.net.URLDecoder.decode(url, "UTF-8")` on Android, or use KMP-safe `encodeURIComponent` reverse equivalent via `UrlEncoder` already in the module |
| Low | `androidMain/.../rpc/WebAvanueJsonRpcServer.kt` | HTTP framing logic reads until blank line (`line.isBlank()`) — breaks with requests containing binary content or large JSON bodies split across TCP segments | Use proper `Content-Length` header parsing to read exactly N body bytes after blank line |
| Low | `commonMain/.../ReadingModeExtractor.kt` | `getExtractionScript()` generates a JS IIFE that returns `JSON.stringify(...)` — the Kotlin `trimIndent()` on a multiline string may introduce unintended indentation inside the JS; some minifiers or strict JS engines may reject indented `return` statements inside immediately-invoked functions | Use explicit `"""` delimiters without `trimIndent()` for JS, or pass the script through a proper minifier |
| Low | `commonMain/.../SettingsValidation.kt` | Validation logic for home page URL does not validate against the `allowedSchemes` list consistently — `http://` and `https://` pass, but `javascript:` and `data:` scheme URLs would also be accepted as home page URIs | Add explicit scheme allowlist check |
| Low | `commonMain/.../BrowserVoiceOSCallback.kt` | `System.currentTimeMillis()` called directly in a `commonMain` file — should use `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()` for true KMP portability (currently acceptable only because callers are Android-platform callbacks, but the class is in commonMain) | Replace with KMP-safe `Clock.System.now().toEpochMilliseconds()` |
| Low | `commonMain/.../BrowserTextCommands.kt` | No AVID `contentDescription` semantics on the `BrowserTextCommand` composable buttons (clear, submit, copy actions) | Add `Modifier.semantics { contentDescription = "Voice: click ..." }` to each interactive element |

---

## Detailed Findings

### Critical-1: JavaScript Injection / XSS in VoiceOS Bridge
**File:** `androidMain/.../WebAvanueVoiceOSBridge.kt`

```kotlin
// VULNERABLE — selector injected into JS string without escaping
fun clickBySelector(selector: String) {
    val script = "document.querySelector('$selector').click();"
    webView.evaluateJavascript(script, null)
}
```

If `selector = "'); alert('XSS"`, the executed JS becomes:
```javascript
document.querySelector(''); alert('XSS').click();
```

This is exploitable when user-supplied or website-supplied element identifiers are routed through
the voice command bridge. Same pattern exists in `focusElement`, `inputText`, `scrollToElement`.

**Fix:** Apply JS string escaping before interpolation. A safe implementation:
```kotlin
fun clickBySelector(selector: String) {
    // escapeJs replaces \ → \\, ' → \', " → \", newline → \n
    val safeSelector = selector.escapeJs()
    val script = "document.querySelector('$safeSelector')?.click();"
    webView.evaluateJavascript(script, null)
}
```
Note: The module already has `escapeJs()` in `SecureScriptLoader.kt` — reuse it.

---

### Critical-2: Broken Encryption in SecureScriptLoader
**File:** `androidMain/.../SecureScriptLoader.kt`

```kotlin
// INSECURE — deterministic IV per device
private fun deriveIV(): ByteArray {
    val seed = "${packageName}:${Build.FINGERPRINT}"
    val md = MessageDigest.getInstance("MD5")  // ← MD5 is broken
    return md.digest(seed.toByteArray()).take(16).toByteArray()
}

// AES/CBC with static IV — same plaintext always produces same ciphertext
private fun decryptScript(encrypted: ByteArray): String {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val ivSpec = IvParameterSpec(deriveIV())  // ← same IV every time
    cipher.init(Cipher.DECRYPT_MODE, getKey(), ivSpec)
    return String(cipher.doFinal(encrypted))
}
```

CBC mode with a static IV leaks whether two script blocks share a common prefix
(identical first blocks produce identical first ciphertext blocks). More critically, this
enables offline dictionary attacks: the IV is fully predictable from public information
(`Build.FINGERPRINT` appears in `adb shell getprop`).

**Fix:** Move to AES/GCM with a random 12-byte nonce stored alongside the ciphertext:
```kotlin
fun encrypt(plaintext: ByteArray): ByteArray {
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
    val ciphertext = cipher.doFinal(plaintext)
    return iv + ciphertext  // prepend IV to ciphertext
}

fun decrypt(data: ByteArray): ByteArray {
    val iv = data.take(12).toByteArray()
    val ciphertext = data.drop(12).toByteArray()
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
    return cipher.doFinal(ciphertext)
}
```

---

### Critical-3: `WebViewFactory.createWebView()` Stub — Rule 1 Violation
**File:** `androidMain/.../AndroidWebView.kt:222`

```kotlin
actual class WebViewFactory {
    actual fun createWebView(config: WebViewConfig): WebView {
        // STUB — throws at runtime
        throw NotImplementedError("Use WebViewComposable instead for Android")
    }
}
```

This violates Rule 1 (no stubs). If any code path calls `WebViewFactory().createWebView()` on
Android, the app crashes. The iOS implementation (`IOSWebView.kt:384`) correctly returns an
`IOSWebView` instance.

**Fix:** Implement the factory to return a properly configured Android `WebView` wrapped in the
`WebView` interface, mirroring the iOS implementation. Alternatively, if the factory is truly
unused on Android (all creation goes through `WebViewComposable`), delete the `expect/actual`
contract and remove `WebViewFactory` from commonMain entirely.

---

### Critical-4: iOS WebViewPoolManager API Contract Mismatch
**File:** `iosMain/.../WebViewPoolManager.ios.kt` vs `commonMain/.../WebViewPoolManager.kt`

commonMain `expect`:
```kotlin
expect object WebViewPoolManager {
    fun removeWebView(tabId: String)
    fun clearAllWebViews()
    fun clearCookiesOnExit()
}
```

iOS `actual`:
```kotlin
actual object WebViewPoolManager {
    actual fun preWarmWebView()         // ← not in expect
    actual fun getWebView(): Any?       // ← not in expect
    actual fun returnWebView(webView: Any)  // ← not in expect
    actual fun clearPool()              // ← not in expect
    actual fun getPoolSize(): Int       // ← not in expect
    // removeWebView(), clearAllWebViews(), clearCookiesOnExit() → MISSING
}
```

This is either a compile error (if the build checks expect/actual completeness) or the iOS
actual satisfies a DIFFERENT expect than the one declared in commonMain — meaning the iOS
implementation is orphaned from the shared interface.

**Fix:** Align the expect/actual surfaces. Decide which methods belong in the shared contract
and implement them consistently on both Android and iOS.

---

### Critical-5: DOM Scraping Data Race in BrowserVoiceOSCallback
**File:** `commonMain/.../BrowserVoiceOSCallback.kt`

```kotlin
// Called on WebView main thread (UI thread):
override fun onDOMScraped(data: ScrapedWebData) {
    commandGenerator.clearCommands()         // ← mutates on main thread
    commandGenerator.addElements(data.elements)  // ← mutates on main thread

    scope.launch(Dispatchers.Default) {      // ← runs concurrently
        val commands = commandGenerator.getAllCommands()  // ← reads on Default
        persistCommands(commands)
    }

    scope.launch(Dispatchers.Default) {      // ← runs concurrently
        checkDatabaseCache(commandGenerator.getAllCommands())  // ← reads on Default
    }
}
```

`mutableListOf` and `mutableMapOf` are not thread-safe. Concurrent `clearCommands()` +
`getAllCommands()` can produce `ConcurrentModificationException` or return partial state.

**Fix:**
```kotlin
private val commandMutex = Mutex()

override fun onDOMScraped(data: ScrapedWebData) {
    scope.launch {
        commandMutex.withLock {
            commandGenerator.clearCommands()
            commandGenerator.addElements(data.elements)
        }
        val snapshot = commandMutex.withLock { commandGenerator.getAllCommands().toList() }
        persistCommands(snapshot)
        checkDatabaseCache(snapshot)
    }
}
```

---

### High-1: Wrong List Indexed in Disambiguation
**File:** `commonMain/.../BrowserVoiceOSCallback.kt:758-762`

```kotlin
fun selectDisambiguationOption(index: Int) {
    val commands = commandGenerator.getAllCommands()  // WRONG LIST — full database
    if (index < commands.size) {
        executeCommand(commands[index])  // executes command #N from entire command set
    }
}
```

When disambiguation shows the user 3 options (e.g., "click Login button", "click Login link",
"click Login form") and the user says "first one", `index = 0` would correctly resolve to the
first option — but only by accident. If any other commands were added after the disambiguation
options were generated, the indices no longer align. Index 1 or 2 will nearly always execute
the wrong command.

**Fix:** Store the disambiguation candidates list:
```kotlin
private var _pendingDisambiguationCandidates: List<VoiceCommand> = emptyList()

fun showDisambiguation(matches: List<VoiceCommand>) {
    _pendingDisambiguationCandidates = matches
    // present to user...
}

fun selectDisambiguationOption(index: Int) {
    val candidates = _pendingDisambiguationCandidates
    if (index in candidates.indices) {
        executeCommand(candidates[index])
        _pendingDisambiguationCandidates = emptyList()
    }
}
```

---

### High-2: `evaluateJavaScript()` Race — Both Platforms
**File:** `androidMain/.../AndroidWebView.kt:151-157`, `iosMain/.../IOSWebView.kt:197-204`

**Android:**
```kotlin
override suspend fun evaluateJavaScript(script: String): String? {
    var result: String? = null
    webView.evaluateJavascript(script) { value ->
        result = value  // ← fires AFTER the function returns
    }
    return result  // ← always null
}
```

The callback is posted to the main thread AFTER `evaluateJavascript` returns control — the
`suspend fun` body has already reached `return result` with `result == null`.

**Fix (Android):**
```kotlin
override suspend fun evaluateJavaScript(script: String): String? =
    suspendCancellableCoroutine { cont ->
        webView.evaluateJavascript(script) { value ->
            cont.resume(value)
        }
    }
```

**Fix (iOS):**
```kotlin
override suspend fun evaluateJavaScript(script: String): String? =
    suspendCancellableCoroutine { cont ->
        webView.evaluateJavaScript(script) { value, error ->
            cont.resume(if (error == null) value?.toString() else null)
        }
    }
```

---

### High-3: JS Injection in `findInPage` — Both Platforms
**File:** `androidMain/.../AndroidWebViewController.kt:173`, `iosMain/.../IOSWebView.kt:227`

```kotlin
// Android:
fun findInPage(query: String) {
    webView.evaluateJavascript("window.find('$query', true, false, true)", null)
}

// iOS:
override fun findInPage(text: String) {
    val script = "window.find('$text', false, false, true, false, true, false);"
    webView.evaluateJavaScript(script, null)
}
```

A search query of `'); document.cookie='stolen';//` breaks out of the `window.find()` call.
While `window.find()` is deprecated, the escaping issue is real and applies to any JS string
interpolation.

**Fix:** Escape the query before interpolation:
```kotlin
fun findInPage(query: String) {
    val safe = query.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
    webView.evaluateJavascript("window.find('$safe', true, false, true)", null)
}
```

---

### High-4: GestureMapper `await` in Non-Async Context
**File:** `commonMain/.../GestureMapper.kt`

```kotlin
GESTURE_COPY -> "await window.AvanuesGestures.copy()"   // ← SyntaxError
GESTURE_CUT  -> "await window.AvanuesGestures.cut()"    // ← SyntaxError
GESTURE_PASTE -> "await window.AvanuesGestures.paste()" // ← SyntaxError
```

These strings are passed directly to `WebView.evaluateJavascript()` as top-level expressions.
`await` is only valid inside `async` functions or at the top level of ES modules. WebView
`evaluateJavascript` executes in a classic (non-module) context. Result: these gestures always
throw a `SyntaxError` and silently fail (since the error is discarded).

**Fix:**
```kotlin
GESTURE_COPY  -> "(async () => { await window.AvanuesGestures.copy(); })()"
GESTURE_CUT   -> "(async () => { await window.AvanuesGestures.cut(); })()"
GESTURE_PASTE -> "(async () => { await window.AvanuesGestures.paste(); })()"
```

---

### High-5: iOS KVO Observer Only Registered for One Key Path
**File:** `iosMain/.../IOSWebView.kt:115-147`

```kotlin
webView.addObserver(
    observer,
    forKeyPath = "estimatedProgress",  // ← only this key registered
    options = ...,
    context = null
)
// observeValueForKeyPath handles 6 keys: loading, title, URL, canGoBack, canGoForward, estimatedProgress
// but only "estimatedProgress" fires — other 5 StateFlows never update
```

This means `pageTitle`, `isLoading`, `currentUrl`, `canGoBack`, `canGoForward` StateFlows never
update from KVO. The navigation delegate partially compensates (updates `_isLoading`,
`_pageTitle`, `_currentUrl`), but `_canGoBack` / `_canGoForward` are only updated via
`updateNavigationState()` called in `didFinishNavigation` — not during provisional navigation
or on `goBack()`/`goForward()` calls.

**Fix:** Register one observer call per key path:
```kotlin
listOf("estimatedProgress", "loading", "title", "URL", "canGoBack", "canGoForward").forEach { key ->
    webView.addObserver(observer, forKeyPath = key, options = ..., context = null)
}
```

And correspondingly remove all 6 in `dispose()`.

---

### High-6: iOS JS Alert Never Shown to User
**File:** `iosMain/.../IOSWebView.kt:331-356`

```kotlin
override fun webView(...runJavaScriptAlertPanelWithMessage: String, ...completionHandler: () -> Unit) {
    val alertController = UIAlertController.alertControllerWithTitle(...)
    alertController.addAction(UIAlertAction.actionWithTitle("OK", handler = { completionHandler() }))
    // ← alertController is configured but NEVER presented (no UIViewController reference)
    completionHandler()  // ← called immediately; dialog discarded
}
```

All `window.alert()` calls on iOS silently succeed without showing the user any dialog. Websites
relying on alert confirmation (e.g., "Are you sure you want to delete?") get the OK response
immediately with no user interaction.

**Fix:** Pass a `UIViewController` reference to `WebViewUIDelegate` and call
`viewController.presentViewController(alertController, animated = true, completion = null)`.

---

## Patterns Summary

### `println()` Logging (Replace With `Napier.d/e/w()` or `Log.d/e`)
Files using `println()` instead of structured logging:
- `WebViewLifecycle.kt` — 10+ occurrences (lifecycle events, pool state)
- `BrowserVoiceOSCallback.kt` — 15+ occurrences (scraping results, DB cache hits)
- `DatabaseDriver.kt` — 5+ occurrences (migration status)
- `SecureStorage.kt` — 4 occurrences (credential store operations — especially sensitive)
- `RetryPolicy.kt` — 2 occurrences (retry attempt logging)
- `DownloadHelper.kt` — 2 occurrences (download path fallbacks)
- `VoiceCommandService.ios.kt` — 2 occurrences (stub acknowledgement)
- `WebAvanueVoiceOSBridge.kt` — 4+ occurrences (bridge diagnostics)

**Total:** ~44 `println()` calls across 8+ files. Use `Napier` (KMP-compatible) in commonMain,
`android.util.Log` in androidMain.

### Unsynchronized Mutable Counters (Apply `AtomicInt`)
- `AdBlocker.kt`: `blockedCount: Int var`
- `TrackerBlocker.kt`: `blockedCount: Int var`

Both are incremented from WebView resource interception callbacks which may fire on background
threads.

### Theme System Non-Compliance (Migrate to AvanueTheme v5.1)
Five files form a parallel color system that bypasses AvanueTheme:
- `AppColors.kt` — interface with 40+ color properties
- `AppTheme.kt` — wraps `MaterialTheme(colorScheme = ...)`
- `WebAvanueColors.kt` — implements `AppColors` with hardcoded hex literals
- `AvaMagicColors.kt` — stub integration with VoiceOS theme service
- `OceanDialog.kt` — uses `MaterialTheme.typography` and hardcoded `Color(0xFF1E293B)`

Action: Map the Ocean Blue brand palette to a new `AvanueColorPalette.OCEAN` entry in the
AvanueUI DesignSystem module. Replace `LocalAppColors.current.primary` usage with
`AvanueTheme.colors.primary` throughout UI files.

---

## Statistics

| Severity | Count |
|----------|-------|
| Critical | 9 |
| High | 17 |
| Medium | 17 |
| Low | 7 |
| **Total** | **50** |

**Rule 7 violations:** 0 (no AI/Claude attribution found anywhere in the module)
**MaterialTheme.colorScheme violations:** 0 (parallel AppColors system used instead — different issue)
**Scraping system integrity:** Intact — no scraping code flagged for removal (Mandatory Rule #1 observed)
**AVID semantics coverage:** 190 occurrences across 49 UI files — good baseline; `BrowserTextCommands.kt` action buttons are missing semantics (Low)

---

*Report generated: 260220 | Scope: Modules/WebAvanue/src/ (~280 .kt files) | Branch: HTTPAvanue*
