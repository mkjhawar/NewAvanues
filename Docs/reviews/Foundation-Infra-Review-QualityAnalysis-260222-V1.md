# Foundation + Infrastructure Layer — Quality Analysis — 260222

---

# Foundation Quality Report — 260222

## Summary
SCORE: 81 | HEALTH: YELLOW
FILES: 31 kt | LINES: ~1900 | KMP: yes (commonMain + androidMain + iosMain + desktopMain)

Foundation is architecturally sound: clean KMP abstractions, consistent codec pattern, proper
platform separation. The score dips to YELLOW due to three concrete problems: (1) Android
`IPermissionChecker` and `IFileSystem` have no `androidMain` actual implementation despite the
iosMain and desktopMain implementations existing, (2) `IosPermissionChecker` always returns `true`
for every runtime permission making it semantically useless, and (3) `BaseViewModel.launchIO()`
quietly dispatches to `Dispatchers.Default` instead of `Dispatchers.IO` which is a naming lie.

---

## P0 Critical Issues

- **[Foundation — IPermissionChecker / IFileSystem]** Android implementations do NOT exist.
  The `Modules/Foundation/src/androidMain/` directory contains only `Sha256Android.kt`. There is no
  `AndroidPermissionChecker.kt` and no `AndroidFileSystem.kt`. The iosMain and desktopMain have
  full implementations. This means any Android consumer of `IPermissionChecker` or `IFileSystem`
  from this module has no runtime binding. (Note: Android platform code for these concerns exists
  in Modules/Utilities instead, which is a separate competing abstraction — see Medium issues.)

## P1 High Issues

- **[BaseViewModel.kt:74]** `launchIO()` uses `Dispatchers.Default` not `Dispatchers.IO`.
  Method name promises IO thread (for database/network), but executes on computation pool.
  Can cause thread-pool starvation for blocking I/O calls passed to this method.

- **[IosPermissionChecker.kt:18-24]** Both `hasPermission()` and `requestPermission()` always
  return `true` regardless of actual iOS permission state. The comment says "real permission
  requests are handled by the Swift app layer" but there is no callback or notification mechanism
  to surface actual grant status back to the KMP layer. Callers that check `hasPermission()` before
  performing a privileged operation will silently proceed even when permission is denied on device.
  `isAccessibilityEnabled()` hardcodes `false` — logically inconsistent with always-`true` above.

- **[DesktopPermissionChecker.kt:16-22]** Same semantics as iOS: always `true`. Slightly more
  defensible on desktop (no runtime permission model), but `isAccessibilityEnabled()` returning
  `true` unconditionally on desktop is misleading — desktop does not have accessibility services
  in the Android/VoiceOS sense.

- **[UserDefaultsSettingsStore.kt:81-82]** `getLong()` calls `defaults.integerForKey(key)` which
  returns `NSInteger` — a 32-bit signed integer on 32-bit iOS devices (though all supported iOS
  hardware is 64-bit, the type is platform-width). The NSUserDefaults API does not have a native
  64-bit integer setter/getter; values are stored as platform integers. Long values > Int.MAX_VALUE
  (e.g., Unix timestamps in milliseconds) will silently truncate on any theoretical 32-bit target.
  For safety, these should be stored as Double (`setDouble`/`doubleForKey`) or as String.

## P2 Medium Issues

- **[Foundation module vs Utilities module]** Foundation defines `IFileSystem` / `IPermissionChecker`
  interfaces without an `androidMain` actual, while `Modules/Utilities` has a competing
  `expect class FileSystem` with full Android/iOS/Desktop actuals. Two parallel abstractions for
  file system operations — one interface-based, one expect/actual-based — will confuse maintainers.
  Needs architectural consolidation.

- **[Sha256Android.kt vs Sha256Jvm.kt]** Both `androidMain` and `desktopMain` SHA-256 are
  functionally identical (MessageDigest.getInstance("SHA-256") + joinToString "%02x").
  They should share a `jvmMain` source set to avoid duplication. Foundation's build.gradle.kts
  declares a `jvm("desktop")` target but does not define a shared `jvmMain` source set.

- **[NumberToWords.kt:188]** `defaultSystem: NumberSystem = NumberSystem.WESTERN` is a mutable
  global on an `object`. Mutable global state in a shared library is a thread-safety concern and
  makes behaviour non-deterministic when multiple callers use different systems concurrently.
  Should either be removed (pass `system` explicitly every call) or protected with synchronization.

- **[NumberToWords.kt:205-206]** `Long.MIN_VALUE` handling: the comment says `-Long.MIN_VALUE`
  overflows, and the code returns `"negative ${convert(Long.MAX_VALUE, system)}"`. This is
  semantically incorrect — Long.MIN_VALUE is -9223372036854775808, but Long.MAX_VALUE is
  9223372036854775807 (one smaller). The output will say the wrong number.

- **[SettingsMigration.kt:36-54]** `TERRA` palette has no legacy migration mapping. If an
  old device had a custom variant that maps to `TERRA` this path is silently ignored and defaults
  to `HYDRA`. This is acceptable only if `TERRA` is entirely new (v5.1 only) — should be documented.

- **[ServiceState.kt:86-87]** Default implementation of `ServiceStateProvider.metadata` creates
  a new `MutableStateFlow(emptyMap())` on every call to the property getter (because it's a `get()`
  with an expression body). Observers collecting this flow will immediately complete with no further
  emissions because the reference is thrown away. Implementors who forget to override this will
  see no metadata. Should be changed to an immutable default `val` or an abstract property.

- **[SearchState.kt:116-158]** `SettingsUpdater` class is defined in the same file as `SearchState`.
  Unrelated classes sharing a file violates single-responsibility at the file level and makes
  discovery harder. `SettingsUpdater` belongs in a dedicated file.

- **[DeveloperSettings.kt:27]** `debugMode: Boolean = true` — debug mode is ON by default in
  production data class. If a fresh install gets the default object before any persistence reads,
  debug mode runs in prod. Default should be `false`.

---

## Code Smells

- **Duplicate SHA-256**: `androidMain/Sha256Android.kt` and `desktopMain/Sha256Jvm.kt` are byte-for-byte identical — DRY violation.
- **File packing**: `SearchState.kt` contains both `SearchState` and `SettingsUpdater`.
- **Missing androidMain platform implementations**: `IFileSystem` / `IPermissionChecker` — asymmetric KMP surface.

## Missing Implementations

- **`AndroidPermissionChecker`** — no `androidMain` actual for `IPermissionChecker`. Android
  callers cannot use the Foundation interface for permission checking.
- **`AndroidFileSystem`** — no `androidMain` actual for `IFileSystem`. Android callers must use
  the competing `Utilities.FileSystem` class instead, creating confusion.
- **iOS `IPermissionChecker`** — structurally present but semantically empty (all `true`). Needs
  a real bridging mechanism that reads iOS permission status via AVFoundation/CoreLocation APIs.

## Deprecated Usage

- No use of deprecated APIs detected. Correctly uses `kotlin.concurrent.Volatile` annotation
  instead of the deprecated `@Volatile` form.

---

---

# Logging Quality Report — 260222

## Summary
SCORE: 88 | HEALTH: GREEN
FILES: 11 kt | LINES: ~430 | KMP: yes (commonMain + androidMain + iosMain + desktopMain)

Clean, self-contained KMP logging module. Proper expect/actual pattern. PII redaction is
a genuine feature. The two most notable issues are: (1) PIISafeLogger defeats lazy evaluation
by eagerly calling `message()` even when the underlying delegate would short-circuit on level,
and (2) `PIISafeLoggerFactory` creates a new logger instance per static convenience call rather
than caching, which is minor but wasteful.

---

## P0 Critical Issues

_None._

## P1 High Issues

- **[PIISafeLogger.kt:59-66]** Lazy evaluation is broken. Every method in `PIISafeLogger`
  calls `message()` eagerly via `redact(message())` before delegating to the underlying logger.
  The delegate logger will then check its own level and potentially discard the message — but the
  lambda has already been evaluated and redacted. For expensive message lambdas (string
  interpolations, toString calls), this eliminates all the performance benefit of the lazy API.
  Fix: check `isLoggable(level)` before evaluating `message()`.

## P2 Medium Issues

- **[PIISafeLoggerFactory.kt:35-47]** Static convenience methods (`v`, `d`, `i`, etc.) each
  call `getLogger(tag)` which constructs a new `PIISafeLogger` wrapping a new platform logger
  every single call. Logger instances should be cached (e.g., a `ConcurrentHashMap<String,
  PIISafeLogger>` on JVM, or simple map on other targets).

- **[PIIRedactionHelper.kt:33-34]** `PHONE_PATTERN` is overly broad. The regex
  `(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}` will match
  many non-phone number sequences such as version strings (`3.2.1.1234`), timestamps
  (`2026-02-22`), and even coordinates. False positives will redact useful debug information.

- **[PIIRedactionHelper.kt:39-41]** `CREDIT_CARD_PATTERN` alternative `\\b\\d{13,19}\\b` will
  match any 13–19 digit sequence. This includes Android IDs, screen dimensions in large strings,
  and many debug identifiers. The pattern is too broad for production log redaction.

- **[Logging module package]** Package is `com.avanues.logging` while all other modules use
  `com.augmentalis.*`. This inconsistency will cause confusion and may prevent cross-module
  imports without IDE reconfiguration. Should be aligned to `com.augmentalis.logging`.

---

## Code Smells

- **Package namespace mismatch**: `com.avanues.logging` vs project-wide `com.augmentalis.*`
- **Eager evaluation in lazy API**: `PIISafeLogger` forces message evaluation before level check.

## Missing Implementations

- No actual missing — all 3 platforms (Android, iOS, Desktop) have full `LoggerFactory` actuals.

## Deprecated Usage

- None detected.

---

---

# Utilities Quality Report — 260222

## Summary
SCORE: 68 | HEALTH: YELLOW
FILES: 20 kt | LINES: ~1400 | KMP: yes (commonMain + androidMain + iosMain + desktopMain)

Utilities duplicates concerns already present in Foundation (`IFileSystem` vs `expect class
FileSystem`, `IPermissionChecker` vs nothing in Utilities, separate Logger in both modules).
The module is functionally complete and all expect/actual declarations have proper implementations
on Android, iOS, and Desktop. However, several issues need attention: Android `DeviceInfo` and
`NetworkMonitor` use a static `lateinit Context` pattern that requires manual initialization and
throws at runtime if forgotten; `DeviceInfo.ios.kt` hardcodes `30%` as available memory; Desktop
`DeviceInfo.getBatteryInfo()` always returns 100% charged with no power-save mode; and `Utilities`
is registered under `com.augmentalis.ava.platform` namespace which collides with the older AVA
namespace convention.

---

## P0 Critical Issues

- **[DeviceInfo.android.kt:28-31 / FileSystem.android.kt:12-15 / NetworkMonitor.android.kt:15-22 /
  Settings.android.kt:9-12]** All four Android `expect` classes use `private lateinit var context`
  + a static `initialize(context)` companion method. This is an anti-pattern: (a) any caller that
  constructs a `DeviceInfo()` without calling `DeviceInfo.initialize(context)` first will crash
  with `UninitializedPropertyAccessException`; (b) the static `appContext` field is a memory leak
  risk if `applicationContext` is not used consistently (it is used, but the pattern is fragile);
  (c) the initialization is not thread-safe — `appContext` is assigned without synchronization.
  Prefer constructor injection or a Hilt/Koin-provided context.

## P1 High Issues

- **[DeviceInfo.ios.kt:122-128]** `getAvailableMemory()` hardcodes `totalMemory * 0.3f` as
  available memory. iOS does not provide a direct API for free memory in the same way Android does,
  but returning a fixed 30% approximation is incorrect and will mislead callers using `isLowMemory()`
  (which triggers at 80% used — the hardcoded value means `isLowMemory()` always returns false on
  iOS until physicalMemory × 0.3 / physicalMemory < 0.2, which never happens with this formula).
  Use `vm_statistics64` via Mach APIs for real values, or clearly document the approximation.

- **[DeviceInfo.desktop.kt:70-78]** `getBatteryInfo()` always returns `BatteryInfo(level=100,
  isCharging=false, isPowerSaveMode=false)`. The comment says it would "require platform-specific
  native code". Returning 100% charged is misleading — a laptop on battery at 5% will report 100%.
  Should return `BatteryInfo(level=-1, isCharging=false)` to signal "unavailable" or use a
  `BatteryInfo?` nullable return type (requires interface change).

- **[DeviceInfo.desktop.kt:119-130]** `hasFeature()` returns `true` for MICROPHONE, CAMERA,
  BLUETOOTH, and WIFI unconditionally on desktop. These assumptions are false on many desktop
  deployments (headless servers, VMs, Bluetooth-less desktops). Should probe actual system APIs
  or return `false` by default with feature detection.

- **[NetworkMonitor.desktop.kt:88-97]** Connectivity check uses `InetAddress.isReachable()` to
  ping `8.8.8.8`. This is a blocking DNS + ICMP operation on `Dispatchers.IO`. However,
  `checkConnectivity()` is also called from `isConnected()` which is a non-suspend function —
  meaning it can block the calling thread. If called from the main thread, this will ANR.

- **[Utilities module — Duplicate Logger]** `Utilities` defines its own `expect object Logger`
  (`com.augmentalis.ava.platform.Logger`) in `Logger.kt` and `Logger.android/ios/desktop.kt`.
  This is a complete duplication of the `Modules/Logging` module which provides a superior
  interface-based KMP logger with lazy evaluation and PII safety. Two competing logging APIs in
  the same codebase is a maintenance hazard.

## P2 Medium Issues

- **[DeviceInfo.ios.kt:101-120]** `hasFeature()` hardcodes `true` for MICROPHONE, CAMERA,
  BLUETOOTH, WIFI, and BIOMETRICS on all iOS devices regardless of actual device capabilities
  (e.g., iPod Touch has no cellular, old iPads lack Face ID). Reasonable approximation for typical
  iPhone/iPad, but misleading for edge cases.

- **[FileSystem.android.kt:64-71]** `delete()` calls `file.deleteRecursively()`. This will
  silently delete entire directory trees when passed a directory path. The contract says "Delete
  file at path" — the caller likely expects a single-file delete. Directories should be rejected
  or the method should require explicit recursive flag.

- **[NetworkMonitor.desktop.kt:21-22]** `CoroutineScope` is created at construction but
  `stopMonitoring()` does not cancel it — only cancels the monitoring job. The scope itself leaks.
  Should call `scope.cancel()` in `stopMonitoring()` or implement `Closeable`.

- **[Settings.ios.kt:16-17]** `defaults.synchronize()` is called on every write. Apple deprecated
  `synchronize()` in iOS 12 (it's a no-op in modern iOS but shows a deprecation warning).
  Should be removed.

- **[DeviceInfo.desktop.kt:263-295]** `getMachineId()` spawns child processes (`reg query`,
  `ioreg`, reads `/etc/machine-id`). These block the calling thread and can fail unpredictably in
  sandboxed environments (Docker, CI). No timeout is applied to the spawned processes — if `reg` or
  `ioreg` hangs, the calling thread blocks indefinitely.

---

## Code Smells

- **Competing Logger**: Utilities defines its own `Logger` expect/actual that duplicates `Modules/Logging`.
- **Competing FileSystem**: Utilities `FileSystem` and Foundation `IFileSystem` serve the same purpose.
- **Static init pattern**: All Android expect classes require `Companion.initialize(context)` — error-prone.
- **Always-true feature detection**: Both iOS and Desktop `hasFeature()` return hardcoded `true` for most features.

## Missing Implementations

- No missing actual declarations. All 5 expects (`DeviceInfo`, `DeviceInfoFactory`, `FileSystem`,
  `FileSystemFactory`, `NetworkMonitor`, `NetworkMonitorFactory`, `Settings`, `SettingsFactory`,
  `Logger`) have Android, iOS, and Desktop actuals.

## Deprecated Usage

- `Settings.ios.kt`: `NSUserDefaults.synchronize()` — deprecated since iOS 12. Remove.
- `DeviceInfo.android.kt:301-304`: `wifiManager.connectionInfo.macAddress` — deprecated since
  Android API 29. The code correctly guards with `SDK_INT < Q` but the deprecation annotation
  is suppressed. Acceptable for the pre-Q path only.

---

---

# Localization Quality Report — 260222

## Summary
SCORE: 53 | HEALTH: RED
FILES: 4 kt | LINES: ~270 | KMP: yes BUT incomplete — iosMain actual is MISSING

The module has a critical structural defect: `Localizer` is declared `expect class` with Android
and Desktop actuals, but there is NO iosMain actual. The build.gradle.kts does not declare any
iOS targets at all (no `iosX64()`, `iosArm64()`, etc.). This means the Localization module
cannot be compiled for any iOS target. Additionally, `TranslationProvider` lists 42 languages
in `LanguageSupport.VIVOKA_LANGUAGES` but only 6 have any translation data — 36 languages are
declared as "supported" but will silently fall back to English for every key lookup.

---

## P0 Critical Issues

- **[Localization/build.gradle.kts — entire file]** No iOS targets declared. The module
  defines `expect class Localizer` but has zero `iosMain` actual implementations. Any iOS
  build that transitively imports this module will fail to compile with "Expected class 'Localizer'
  has no actual declaration in module". The module cannot ship to iOS as-is.

- **[TranslationProvider.kt:24]** 36 of 42 declared languages fall through to English silently:
  `val langTranslations = translations[languageCode] ?: translations["en"] ?: emptyMap()`
  The `LanguageSupport.VIVOKA_LANGUAGES` map lists `it`, `pt`, `ru`, `ar`, `nl`, `pl`, `tr`,
  `hi`, `th`, `cs`, `da`, `fi`, `el`, `he`, `hu`, `no`, `sv`, `uk`, `bg`, `hr`, `ro`, `sk`,
  `sl`, `et`, `lv`, `lt`, `is`, `ga`, `mt`, `sq`, `mk`, `sr`, `bs`, `cy`, `ko` as supported,
  but none have translation maps. Callers checking `isLanguageSupported("it")` get `true` then
  receive English strings from `translate()`. This is a lie in the API.

## P1 High Issues

- **[Localizer.android.kt:66]** `shutdown()` sets `instance = null` inside the companion but
  without synchronization. If one thread calls `shutdown()` while another is in `getInstance()`
  inside the `synchronized(this)` block that tests `instance ?: ...`, the null write is not
  protected by the same lock. This is the double-checked locking bug: the outer `instance ?: synchronized`
  check is not itself atomic. Use `AtomicReference<Localizer?>` or hold the lock for the null write.

- **[Localizer.desktop.kt:57]** Same unsynchronized `instance = null` in `shutdown()` as Android.

- **[TranslationProvider.kt:37-44]** `formatString()` replaces `%s` and `%d` independently for
  each arg with `replaceFirst`. If a message has both `%s` and `%d`, the `%d` call will replace
  the first `%s` occurrence again (because `replaceFirst` is sequential). Example:
  `"Error %d at %s"` with args `(404, "login")` → first pass replaces `%s` → `"Error %d at 404"`,
  second pass replaces `%d` → `"Error login at 404"` (wrong). Needs unified indexed replacement.

## P2 Medium Issues

- **[Localization/build.gradle.kts:65]** `compileSdk = 34` while Foundation uses 35 and
  AvanuesShared uses 35. Inconsistent compileSdk across modules — should be unified.

- **[TranslationProvider.kt:46-170]** All translation data is hard-coded in Kotlin source.
  This makes adding/updating translations a code change requiring a rebuild. For a module
  claiming "42+ language support" the data should be in resources files or a bundled asset,
  not inline maps.

- **[LanguageSupport.kt:55]** `VOSK_LANGUAGES` uses two-letter codes (`"en"`, `"es"`) while
  `SettingsKeys.VOICE_COMMAND_LOCALE` and `AvanuesSettings.DEFAULT_VOICE_LOCALE` both use
  BCP-47 tags (`"en-US"`). Code that tries to match a stored locale against `VOSK_LANGUAGES`
  will always miss because `"en-US"` is not in the set. Language code normalization is absent.

---

## Code Smells

- **Hard-coded translations in source**: Inline maps in Kotlin rather than resource files.
- **Silent language fallback**: 36 languages advertised as supported but silently return English.
- **Two different locale code formats**: `"en"` vs `"en-US"` used in adjacent systems.

## Missing Implementations

- **`iosMain` actual for `Localizer`**: ENTIRELY MISSING. iOS builds will fail to compile.
- **Translations for 36 declared languages**: `it`, `pt`, `ru`, `ar`, `ko`, and 31 more have
  no translation maps. These are functionally broken — claims support but delivers English.

## Deprecated Usage

- None detected.

---

---

# AvanuesShared Quality Report — 260222

## Summary
SCORE: 72 | HEALTH: YELLOW
FILES: 3 kt | LINES: ~80 | KMP: yes (commonMain + iosMain)

AvanuesShared is a thin iOS umbrella module that re-exports other KMP modules as a single
CocoaPods framework. The module itself is minimal by design. The primary issues are: (1) both
`avanuesSharedModule` and `iosPlatformModule` are empty Koin modules — they exist but provide
zero bindings, making them dead code at runtime; (2) `KoinHelper.doStartKoin()` is the only
Koin entry point and it does not accept external module parameters, preventing the iOS app from
injecting app-layer bindings; (3) the module exports Foundation but not Utilities or Localization,
meaning iOS consumers get `IFileSystem` (Foundation) but not the `FileSystem` expect/actual
(Utilities) or `Localizer` (Localization) that actually implement cross-platform concerns.

---

## P0 Critical Issues

_None._

## P1 High Issues

- **[AvanuesSharedModule.kt:13-17 / IosPlatformModule.kt:16-20]** Both Koin modules are empty.
  The comment in `iosPlatformModule` says "Individual database drivers will be registered
  per-database as VoiceOSCore and WebAvanue database schemas are separate" — but no database
  driver is actually registered. Any class injected from iOS that depends on a `SqlDriver` will
  throw `NoBeanDefFoundException` at runtime. This is a latent crash in the iOS app's DI container.

- **[KoinHelper.kt:14-22]** `doStartKoin()` accepts no parameters. The iOS app cannot pass any
  custom modules or override bindings. Any time platform-specific or test-specific bindings are
  needed, the helper must be rewritten. Pattern should accept a `List<Module>` parameter:
  `fun doStartKoin(additionalModules: List<Module> = emptyList())`.

## P2 Medium Issues

- **[AvanuesShared/build.gradle.kts:60-65]** Exports Foundation, AVID, SpeechRecognition, Logging,
  VoiceOSCore, Database — but NOT Utilities and NOT Localization. iOS callers therefore get
  `IFileSystem` (interface only, no implementation from Foundation) and no `Localizer`. The iOS
  umbrella framework is incomplete — a Swift caller cannot access the `Localizer` API at all.

- **[AvanuesShared/build.gradle.kts:108-109]** androidMain dependency on
  `libs.sqldelight.android.driver` is present but the Android platform module is not in this
  module — Android SQLDelight setup should live in `VoiceOSCore` or app-layer DI, not the
  umbrella module. This creates a hard coupling between the umbrella and SQLDelight on Android.

---

## Code Smells

- **Empty Koin modules**: Both `avanuesSharedModule` and `iosPlatformModule` have zero bindings — dead code.
- **Rigid Koin init**: No way to extend with additional modules without modifying `KoinHelper.kt`.

## Missing Implementations

- **`iosPlatformModule` database driver binding**: Empty module means SQLDelight driver is never
  registered for iOS. Any database-dependent code injected via Koin will fail.

## Deprecated Usage

- None detected.
