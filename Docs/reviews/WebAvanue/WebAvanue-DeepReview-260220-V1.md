# WebAvanue Deep Code Review — 260220 V1

**Module:** `Modules/WebAvanue/`
**Branch:** HTTPAvanue
**Date:** 2026-02-20
**Reviewer:** Code-reviewer agent (code-reviewer)
**Scope:** ~265 .kt files across commonMain / androidMain / iosMain / desktopMain + test source sets

---

## Summary

WebAvanue is the KMP browser module powering the in-app browser experience across Android, iOS, and Desktop. The module is well-structured with a broad feature set (tab management, download engine, XR support, DOM scraping bridge, RPC server, reading mode, bookmark import/export, privacy browsing). However, the review found **4 critical issues** — two of which are JS-injection security vulnerabilities — plus a large number of high-severity stubs and logic bugs. The protected scraping system (`DOMScraperBridge`, `WebAvanueVoiceOSBridge`) is intact and must not be touched. Theme and AVID violations are widespread across UI components. A significant pattern of "security theater" appears in several files where code exists but produces no real security effect.

**Scraping system status:** PROTECTED and functional on Android. iOS `voiceOSCallback` parameter accepted but never wired.

---

## Issues

| # | Severity | File:Line | Issue | Suggestion |
|---|----------|-----------|-------|------------|
| 1 | Critical | `androidMain/.../WebAvanueVoiceOSBridge.kt:138` | `clickBySelector(selector)` embeds the selector string directly into a JS template: `` "...document.querySelector('$selector')..." `` — no escaping. If the selector originates from user-dictated voice input, an attacker can inject arbitrary JavaScript (e.g., `') ; maliciousCode() ; ('`) via speech. | Escape single quotes in `selector` before interpolation, or pass the selector as a JSON argument: `val arg = org.json.JSONObject.quote(selector); "...document.querySelector($arg)..."` — never raw-interpolate untrusted strings into JS. |
| 2 | Critical | `androidMain/.../AndroidWebViewController.kt:173` | `findInPage(query)` embeds `query` directly in JS: `` "window.find('$query', ...)" `` — no escaping. Voice-dictated search queries can contain single quotes that break out of the JS string literal, enabling arbitrary code execution in the WebView context. | Escape `query` before interpolation: replace `'` with `\\'`, or use `evaluateJavascript` with a separate JS variable assignment instead of direct string interpolation. |
| 3 | Critical | `androidMain/.../AndroidWebView.kt:222` | `WebViewFactory.createWebView()` throws `NotImplementedError("Use WebViewComposable instead for Android")` — calling this factory crashes at runtime. Any code path that invokes this factory directly will crash. | Remove the factory method and document the composable-only pattern at the call site. If the factory is part of a shared interface, remove it from the interface or replace the body with the correct composable invocation path. |
| 4 | Critical | `androidMain/.../SecureStorage.kt:49` | `useEncryption` defaults to `false`. Browser credentials (HTTP Basic Auth passwords) are stored in **plaintext** `SharedPreferences` unless the caller explicitly opts in. This violates the principle of secure-by-default. | Flip the default: `useEncryption: Boolean = true`. Use `EncryptedSharedPreferences` always. If migration from the old plaintext store is needed, write a one-time migration on first access. |
| 5 | High | `iosMain/.../rpc/WebAvanueRpcServer.kt:22-26` | `start()` sets `running = true` but does nothing else. The iOS RPC server silently pretends to be running. Any caller checking `isRunning()` will get `true` while no actual server exists. | Implement the iOS RPC server, or have `start()` log a conspicuous warning and keep `running = false`. Never return `true` from `isRunning()` when no server is bound. |
| 6 | High | `desktopMain/.../rpc/WebAvanueRpcServer.kt:22-26` | Same stub behavior as iOS — `start()` sets `running = true` without binding any socket. Desktop RPC is silently non-functional. | Same fix as #5. |
| 7 | High | `commonMain/.../TabViewModel.kt:407-412` | `scrollUp()`, `scrollDown()`, `scrollLeft()`, `scrollRight()`, `scrollToTop()`, `scrollToBottom()` — all six methods are **empty no-ops**. Scroll voice commands dispatched through VoiceOS will silently do nothing. | Implement scroll delegation to the active `WebViewController`. The pattern is already established for other commands — call `activeController?.scrollUp()` etc. |
| 8 | High | `commonMain/.../TabViewModel.kt:548-562` | `captureScreenshot()` logs a message but neither calls `onComplete` nor `onError` — both callbacks are silently dropped. The `ScreenshotManager` / `ScreenshotCapture` infrastructure already exists and is fully implemented; this method just doesn't use it. | Replace the stub body with a call to `ScreenshotManager.captureScreenshot()` using the active WebView reference. |
| 9 | High | `androidMain/.../SecureScriptLoader.kt:L50-130` | The `ScriptFragments` constants (e.g., `FRAG_HEADER`) are plain Base64. The device-key derivation (`derivedKey`, `derivedIv`) runs at class initialization time, but the fragments stored as constants were never encrypted with those device keys — the decrypt call will silently produce garbage or throw a `BadPaddingException`. This is security theater: the "encryption" provides no protection because the ciphertext and key are both statically compiled in. | Either (a) actually encrypt the JS fragments with the device key at build time using a Gradle task and ship the encrypted blobs as resources, or (b) use code obfuscation (R8/ProGuard) for the script strings and remove the false encryption facade. |
| 10 | High | `androidMain/.../SecureScriptLoader.kt:213` | `clearCache()` calls `System.gc()` — this does not clear the decrypted script strings from memory; GC is not guaranteed to run and does not zero memory. Sensitive script content may remain in heap for an indefinite period. | Hold decrypted scripts in a `ByteArray` that is zeroed (`fill(0)`) explicitly after use, rather than relying on GC. |
| 11 | High | `androidMain/.../CertificatePinningHandler.kt:116,133` | `reportToMonitoring()` and `showSecurityAlert()` are private methods with empty/commented-out bodies. Certificate pin violations are detected but never reported and never shown to the user. Security monitoring is completely silent on pin failures. | Implement both methods. `showSecurityAlert()` should call back to the UI layer (e.g., via a callback or SharedFlow). `reportToMonitoring()` should log to the structured logger at minimum. |
| 12 | High | `commonMain/.../AvaMagicColors.kt:116` | `AvanuesThemeService.getCurrentTheme()` returns a hardcoded `SystemTheme()` with placeholder hex colors. The TODO comment reads "Query VoiceOS theme service". The entire `AvaMagicColors` runtime theming system is inoperative — all color tokens it provides are wrong/placeholder values. | Connect to the real `AvanueTheme` via `AvanueThemeProvider` / `AvanueTheme.colors.*`. The `AvaMagicColors` indirection layer should be removed or replaced by a proper AvanueTheme bridge. |
| 13 | High | `commonMain/.../WebAvanueActionMapper.kt:97-101` | `ADD_BOOKMARK` case returns `ActionResult.success()` but has a TODO comment — the bookmark is **never actually saved**. The action mapper reports success for a write that never happens. | Implement the bookmark save call, or return `ActionResult.error("Not implemented")` until it is. Never return `success()` for an operation that did not execute. |
| 14 | High | `commonMain/.../AdBlocker.kt` | Pattern `Regex(".*ad\\..*")` is far too broad — matches `chad.com`, `gonad.example.com`, `download.example.com` (no, `.*ad\.` matches any domain containing `ad.`). Pattern `Regex(".*ads\\..*")` matches `brands.example.com`. These false positives will break browsing on legitimate sites. | Use prefix/suffix matching against a curated allowlist, or restrict patterns to well-known ad CDN hostnames. Consider integrating EasyList or uBlock Origin host-level lists rather than home-grown catch-all patterns. |
| 15 | High | `commonMain/.../TrackerBlocker.kt` | Pattern `Regex(".*track.*")` matches `soundtrack.com`, `racetrack.example.com`. Pattern `Regex(".*analytics.*")` blocks legitimate internal analytics. Pattern `Regex(".*metrics.*")` blocks Prometheus/Grafana scrape endpoints on dev/staging sites. | Same recommendation as #14 — replace with curated hostname-level lists rather than substring patterns. |
| 16 | High | `iosMain/.../WebViewContainer.ios.kt:150-169` | KVO observer added via `addObserver(observer, forKeyPath: "estimatedProgress", ...)` is **never removed**. There is no `update` block cleanup, no `DisposableEffect`, and no `UIKitView` dispose hook that calls `removeObserver()`. This causes a memory leak and will crash on iOS when the WebView is deallocated while the observer is still registered. | Add a `DisposableEffect` or use the `onRelease` block of `UIKitView` (if available in the Compose Multiplatform API) to call `view.removeObserver(observer, forKeyPath: "estimatedProgress")`. |
| 17 | High | `commonMain/.../AdBlocker.kt` and `androidMain/.../WebViewConfigurator.kt` | Ad-blocking logic is duplicated across `AdBlocker.kt`, `TrackerBlocker.kt`, and the hardcoded `shouldInterceptRequest()` blocklist in `WebViewConfigurator`. Three separate blocking mechanisms with overlapping but inconsistent coverage. | Consolidate all URL blocking into `AdBlocker` / `TrackerBlocker`. `WebViewConfigurator.shouldInterceptRequest()` should delegate to those classes exclusively. |
| 18 | Medium | `commonMain/.../AppTheme.kt:82` | `AppTheme` wraps `MaterialTheme` directly (using `androidx.compose.material3.darkColorScheme` / `lightColorScheme`) — a violation of the mandatory AvanueTheme v5.1 system. The module maintains its own parallel theme stack instead of using `AvanueTheme`. | Replace `MaterialTheme` wrapping with `AvanueThemeProvider` using `AvanueColorPalette` + `MaterialMode` + `AppearanceMode`. Move to `AvanueTheme.colors.*` for all token reads. |
| 19 | Medium | `commonMain/.../FavoritesBar.kt` | `AddFavoriteButton` and `FavoriteItem` interactive elements have no AVID semantics (`contentDescription`). Voice commands cannot target favorites bar items. Also uses `MaterialTheme.typography.bodySmall` directly (theme violation) and hardcoded color literals not from any token system. | Add `Modifier.semantics { contentDescription = "Voice: click [item title]" }` to every interactive element. Replace `MaterialTheme.typography.*` with `AvanueTheme` / `AppTypography`. Replace hardcoded colors with `LocalAppColors.current.*` or `AvanueTheme.colors.*`. |
| 20 | Medium | `commonMain/.../OceanDialog.kt` | `OceanTextButton` composable buttons have no AVID semantics. Uses `MaterialTheme.typography.headlineSmall` directly. Hardcoded color values not from AvanueTheme. | Same pattern as #19. |
| 21 | Medium | `androidMain/.../WebViewConfigurator.kt:222-231` | ANR prevention logic: `` view?.progress ?: 100 < 20 ``. Due to operator precedence this evaluates as `view?.progress ?: (100 < 20)` = `view?.progress ?: false`. When `view` is null the Elvis returns `false`, so the ANR protection branch is **never entered** when the view is null. | Add parentheses: `(view?.progress ?: 100) < 20`. |
| 22 | Medium | `androidMain/.../WebViewConfigurator.kt` | Security-relevant events (SSL errors, JS dialog suppression, geolocation permission grants) are logged via `println()` — no structured logging, no log level control, and these messages appear unfiltered in production logcat. | Replace all `println()` calls with the module's `Logger` class (structured logging, redactable in release builds). |
| 23 | Medium | `commonMain/.../AvuProtocol.kt:78` | `unescape()` processes `%3A` (→ `:`) before `%25` (→ `%`). If a value contains the literal sequence `%3A`, it is correctly decoded to `:`. However, if a value legitimately contains `%25` followed by `3A` (i.e., a percent-encoded colon in a double-encoded string), the order means the `%25` replacement happens after `%3A` — double-decoding corrupts data. The escape/unescape ordering must be the exact reverse of each other. | Ensure `unescape()` processes `%25` last (it must be last to avoid double-decoding). Verify the escape() and unescape() functions are exact inverses by adding a round-trip property test. |
| 24 | Medium | `androidMain/.../AndroidDownloadQueue.kt:34-35` | `downloadIdMap` and `reverseIdMap` are plain `mutableMapOf` accessed from both `Dispatchers.IO` coroutines and potentially the main thread. No synchronization primitive (Mutex, ConcurrentHashMap) protects these maps. | Replace with `java.util.concurrent.ConcurrentHashMap` or protect all accesses with a `Mutex`. |
| 25 | Medium | `androidMain/.../WebAvanueDownloadManager.kt:257` | `_downloadStates.value = _downloadStates.value.toMutableMap().apply { put(...) }` — read-modify-write on a `StateFlow.value` is not atomic. Under concurrent downloads this produces lost updates. | Use `MutableStateFlow.update { it + (id to state) }` which uses `compareAndSet` internally and is thread-safe. |
| 26 | Medium | `commonMain/.../VoiceCommandGenerator.kt:192-193` | `commands: MutableList` and `wordIndex: MutableMap` are unsynchronized class-level fields. `addElements()` and `findMatches()` can be called concurrently from the DOM scraping flow and the voice recognition pipeline without any synchronization, causing `ConcurrentModificationException` or silent data loss. | Wrap mutations in a `ReentrantLock` / `Mutex`, or replace with `CopyOnWriteArrayList` + `ConcurrentHashMap`. |
| 27 | Medium | `commonMain/.../BookmarkImportExport.kt:412-549` | The entire `importFromHtml` parsing logic from `BookmarkImportExport.importFromHtml()` (lines 191-259) is duplicated verbatim in the extension function `parseHtmlWithData()` (lines 417-480), and again in two private helper functions `parseBookmarkEntryInternal()` and `parseFolderEntryInternal()` which are copies of the private methods already inside the object. This is a textbook DRY violation — three copies of the same parser. | Delete `parseBookmarkEntryInternal` and `parseFolderEntryInternal`. Expose the private methods from the object or refactor `parseHtmlWithData` to call `importFromHtml` internally and separately gather the parsed data. |
| 28 | Medium | `androidMain/.../WebViewConfigurator.kt` | `shouldInterceptRequest()` hardcodes a minimal ad-block list that partially overlaps with `AdBlocker.kt`. The `WebViewClient` subclass is 400+ lines with a single responsibility violation — it handles SSL, geolocation, JS dialogs, download detection, ad-blocking, and request interception all in one class. | Extract `SslHandlingWebViewClient`, `PermissionsWebViewClient`, and `AdBlockingWebViewClient` layers, or use a delegate pattern to separate concerns. |
| 29 | Medium | `commonMain/.../GestureMapper.kt:61-62` | `GESTURE_COPY` → `"await window.AvanuesGestures.copy()"` and `GESTURE_CUT` → `"await window.AvanuesGestures.cut()"`. The generated JS uses `await` at the top level of a regular (non-async) IIFE context. `await` at non-async scope is a syntax error and will throw a `SyntaxError` in all modern JS engines, making copy/cut gestures permanently broken. | Wrap the generated script in an `async IIFE`: `` "(async () => { ${script} })()" ``. Or if the surrounding executor already wraps in async context, document that contract explicitly. |
| 30 | Medium | `commonMain/.../UrlValidation.kt:88-90` | `data:` URLs are passed through as `Valid` without any content inspection. `data:text/html,<script>alert(1)</script>` is a valid XSS vector when loaded directly in a WebView. | Validate `data:` URL MIME type. Allow only safe types (`data:image/*`, `data:text/plain`). Reject `data:text/html`, `data:application/javascript`, and similar executable types. |
| 31 | Medium | `androidMain/.../AndroidWebView.kt:152-157` | `evaluateJavaScript()` uses a callback-based API and returns before the callback fires — the coroutine resumes immediately with the `result` variable still `null`. The function appears to be a suspending wrapper but is actually a fire-and-forget, so callers expecting a JS evaluation result will always receive `null`. | Use `kotlinx.coroutines.suspendCancellableCoroutine` with a `CancellableContinuation` to properly bridge the async callback into coroutine suspension. |
| 32 | Medium | `androidMain/.../AndroidWebView.kt:159` | `captureScreenshot()` calls `Bitmap.createBitmap(webView.width, webView.height, ...)` and then `webView.draw(canvas)`. Drawing from a non-main thread causes `CalledFromWrongThreadException`. This function does not enforce main-thread execution. | Add `withContext(Dispatchers.Main) { ... }` around the bitmap creation and draw call, or use `webView.createPrintDocumentAdapter` pattern which is thread-safe. |
| 33 | Medium | `commonMain/.../TabViewModel.kt` | `updateTabStateAndPersist()` calls `repository.updateTab()` inside a `stateMutex.withLock` block without `await` — the DB write is launched fire-and-forget while the mutex is held. Errors from the DB write are silently swallowed. | Use `coroutineScope { launch { repository.updateTab(...) } }` outside the mutex, or at minimum add a `.catch {}` to log failures. Hold the mutex for state mutation only, not for the I/O call. |
| 34 | Low | `iosMain/.../WebViewContainer.ios.kt` | `voiceOSCallback` parameter is accepted by the composable but **never connected** to WKWebView. The DOM scraping pipeline (protected) simply does not run on iOS — voice commands generated from live web content are not available on iOS. | Wire `voiceOSCallback` to a `WKScriptMessageHandler` that calls the callback on DOM events, mirroring the `@JavascriptInterface` pattern on Android. This is a feature gap, not a bug in the protected system. |
| 35 | Low | `androidMain/.../XRBrowserOverlay.kt:44` | `onDismiss = { /* User dismissed indicator */ }` — XR session indicator dismiss callback is a no-op. User has no way to dismiss the session overlay. | Connect to `XRManager.stopSession()` or an appropriate state update on dismiss. |
| 36 | Low | `androidMain/.../WebAvanueVoiceOSBridge.kt` | `VoiceOSInterface.onDOMReady()` and `onDOMChanged()` have comment "Could trigger auto-scrape here" — the auto-scrape on DOM mutation is never wired. The scraper only runs when explicitly called, missing dynamic page updates. | Implement auto-scrape trigger on `onDOMChanged()` with a debounce (e.g., 500ms) to avoid flooding on rapid DOM changes. |
| 37 | Low | Multiple files | Extensive use of `println()` for debug logging throughout `WebViewConfigurator.kt`, `AndroidWebView.kt`, `CertificatePinningHandler.kt`, `WebAvanueJsonRpcServer.kt`. These appear unfiltered in release builds. | Replace all `println()` with `Logger.debug()` / `Logger.error()` from the module's own `Logger` abstraction. |
| 38 | Low | `androidMain/.../WebAvanueJsonRpcServer.kt` | Raw TCP `ServerSocket` JSON-RPC server has no authentication and no TLS. Any local process on the device can connect and invoke browser actions. The server also has no max-payload size limit — a malicious local process can send an unbounded payload causing OOM. | Add a maximum read size (e.g., 64 KB) to `handleClient()`. For authentication, generate a session token at server start and require it in the first RPC frame. |
| 39 | Low | `androidMain/.../WebViewConfigurator.kt:109` | `isDesktopMode` user-agent string is `applicationNameForUserAgent` during `WKWebViewConfiguration` creation (iOS pattern). On Android, this should be `view.settings.userAgentString`. The iOS-style property name suggests copy-paste from iOS code. | Verify `WebViewConfigurator` sets `webView.settings.userAgentString` (not `applicationNameForUserAgent`). |
| 40 | Low | `commonMain/.../BookmarkImportExport.kt:382-388` | `generateExportFilename()` calls `now.toString()` and then does string replacements to produce a timestamp. `kotlinx.datetime.Instant.toString()` returns ISO-8601 format (`2026-02-20T14:30:00Z`) — the `.substring(0, 15)` will produce `20260220_143000` correctly in most cases, but if the milliseconds or timezone offset shifts the string layout, the substring index will be wrong. | Use explicit `LocalDateTime` formatting with `DateTimeFormatter` or `kotlinx-datetime`'s `LocalDateTime.format()` to produce a deterministic filename. |
| 41 | Low | `commonMain/.../ReadingModeExtractor.kt` | The Readability extraction script uses `document.querySelectorAll('div, section')` to score all divs/sections on the page (Priority 4 fallback). On large pages this can iterate thousands of elements synchronously on the main JS thread, causing noticeable UI jank. | Add a size guard: limit `candidates.length` (e.g., cap at 200 elements) before scoring. Or consider async iteration with `requestIdleCallback` for the fallback path. |
| 42 | Low | `androidMain/.../CertificatePinningHandler.kt:64-68` | Four TODO comments left unresolved: "TODO: Add actual certificate pins", "TODO: Validate certificate chain", "TODO: Check HPKP headers", "TODO: Log pin violation". The pinning handler has real structural code but none of the actual pin validation is implemented. | Implement real pin validation using SHA-256 public key hashes. Remove the TODO comments once implemented. |

---

## Recommendations

### Security (Address Immediately)

1. **Fix JS injection vulnerabilities (#1, #2) before any release.** Both `clickBySelector` and `findInPage` pass untrusted strings directly into JS template literals. Even if current voice input is sanitized at a higher layer, this is a defense-in-depth failure that creates XSS risk from any code path that calls these methods.

2. **Flip `SecureStorage` default to encrypted (#4).** Browser credentials should never be stored in plaintext. The `EncryptedSharedPreferences` infrastructure is already in the project — use it unconditionally.

3. **Fix the `await` syntax error in GestureMapper (#29).** Copy and Cut voice gestures are completely broken on all platforms due to this JS syntax error. Wrap generated async gestures in an `async IIFE`.

4. **Restrict `data:` URL validation (#30).** Allow only non-executable MIME types. `data:text/html` is a direct XSS vector.

5. **Implement `CertificatePinningHandler` (#42).** The structural scaffolding exists; fill in the four TODO items with real SHA-256 pin validation.

### Stubs / Silent Failures (High Priority)

6. **Remove the `WebViewFactory.createWebView()` stub (#3).** A method that throws `NotImplementedError` at runtime must not exist in production code. Either delete it or implement it.

7. **Implement TabViewModel scroll methods (#7) and `captureScreenshot` (#8).** Both are complete no-ops while the underlying infrastructure (`VoiceOSWebController`, `ScreenshotManager`) is fully implemented. The wiring is simply missing.

8. **Fix iOS and Desktop RPC server stubs (#5, #6).** `isRunning() == true` while no socket is bound is a lie that causes callers to believe the server is ready. At minimum, keep `running = false` and throw or log a warning.

9. **Fix `AvaMagicColors` stub (#12).** The entire runtime theming bridge returns hardcoded placeholder values. Connect it to `AvanueTheme` or remove the indirection layer.

10. **Fix `ADD_BOOKMARK` false success (#13).** An action that claims `ActionResult.success()` while never saving data is a logic trap. Fix or fail explicitly.

### Theme & AVID

11. **Migrate `AppTheme.kt` to AvanueTheme v5.1 (#18).** This is a mandatory project rule. The current parallel theme stack using `MaterialTheme` directly must be replaced with `AvanueThemeProvider`.

12. **Add AVID semantics to all interactive elements (#19, #20).** `FavoritesBar`, `OceanDialog`, and `XRBrowserOverlay` dismiss buttons are all missing `contentDescription` voice identifiers. This is a zero-tolerance project rule.

13. **Remove `MaterialTheme.*` usages from `FavoritesBar.kt` and `OceanDialog.kt` (#19, #20).** Use `AvanueTheme.colors.*` and `AppTypography` consistently.

### Thread Safety

14. **Fix concurrent access to `downloadIdMap` (#24), `_downloadStates` (#25), and `VoiceCommandGenerator` fields (#26).** These race conditions will surface under normal concurrent usage (multiple downloads, concurrent DOM scraping + voice recognition). Use `ConcurrentHashMap`, `StateFlow.update{}`, and `Mutex` respectively.

15. **Fix `updateTabStateAndPersist` lock+fire-and-forget pattern (#33).** Holding a `Mutex` across a fire-and-forget I/O call is both incorrect (errors are silently dropped) and unnecessarily broad (DB write does not need mutex protection).

### Code Quality

16. **Eliminate the `BookmarkImportExport` triplication (#27).** The same parsing logic appears three times. This is a straightforward DRY fix — expose shared private helpers or refactor `parseHtmlWithData` to call `importFromHtml` and collect data separately.

17. **Fix the `ANR prevention` operator precedence bug (#21).** `view?.progress ?: 100 < 20` never executes the ANR protection path when `view` is null. Add parentheses: `(view?.progress ?: 100) < 20`.

18. **Fix `evaluateJavaScript` callback bridge (#31).** The function appears to be a suspending wrapper but returns before the JS callback fires. Callers will always receive `null`.

19. **Replace all `println()` calls with `Logger` (#37).** Security events (SSL errors, permission grants, pin violations) logged via `println` are not appropriate for production.

---

## Intentional / Out of Scope

- **`DOMScraperBridge` and `WebAvanueVoiceOSBridge`**: Protected under CLAUDE.md Rule #1. Intact and functional on Android. Bug documentation only — no changes suggested.
- **iOS RPC server stub**: Documented as "not yet implemented for iOS" — tracked in this review. No iOS gRPC/RPC runtime available. This is a known Phase 2 item.
- **`WebAvanueJsonRpcServer` (no TLS / no auth)**: Documented as low severity for local-only use. The risk is acknowledged; hardening is recommended but not blocking.
- **XR feature stubs**: `XRManager` and associated components are functional at the state management layer. Camera integration is Android-specific and correctly placed in `androidMain`.

---

## Finding Counts by Severity

| Severity | Count |
|----------|-------|
| Critical | 4 |
| High | 14 |
| Medium | 16 |
| Low | 8 |
| **Total** | **42** |

---

*Author: Manoj Jhawar | Review date: 260220 | Module: WebAvanue | Branch: HTTPAvanue*
