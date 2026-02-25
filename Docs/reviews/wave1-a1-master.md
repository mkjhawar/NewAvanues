# Wave 1 — Architecture Master Index
# Tier: Foundation + Infrastructure
# Review date: 260222

---

## Foundation | 31 kt | Tier 1
PURPOSE: Provides base KMP abstractions — state management helpers (ViewModelState, ListState,
UiState, SearchState), coroutine-based ViewModel base classes, SHA-256 hashing, number-to-words
conversion, settings interfaces and codecs, and cross-platform platform abstractions for file
system, permissions, and credential storage.
WHY: Centralises the boilerplate every feature module needs (StateFlow wrappers, CoroutineScope
lifecycle, typed settings persistence) so individual modules do not re-implement them. Also provides
the canonical ISettingsStore/ICredentialStore contract that Android/iOS/Desktop each implement once.
DEPS: kotlinx.coroutines.core (commonMain), kotlinx.coroutines.android (androidMain)
CONSUMERS: VoiceOSCore, Cockpit, DeviceManager, AvanuesShared (via export), most feature modules
KMP: commonMain, androidMain, iosMain, desktopMain (Note: androidMain is missing IFileSystem and
IPermissionChecker actuals — only Sha256Android.kt exists there)
TABLES: none
KEY_CLASSES: ISettingsStore<T>, ICredentialStore, ViewModelState<T>, BaseViewModel,
AvanuesSettingsCodec, SettingsCodec<T>, HashUtils, NumberToWords
HEALTH: YELLOW — Missing androidMain actuals for IFileSystem and IPermissionChecker; IosPermissionChecker
always returns true; BaseViewModel.launchIO() dispatches to Default not IO; Long truncation risk
in UserDefaultsSettingsStore; mutable global state in NumberToWords.defaultSystem.

---

## Logging | 11 kt | Tier 1
PURPOSE: Cross-platform KMP logging module with lazy message evaluation, configurable minimum log
level, and automatic PII redaction via regex-based pattern matching for email, phone, SSN, credit
card, address, and name fields.
WHY: Prevents PII from appearing in device logs (GDPR/privacy compliance). Consolidates what
were previously duplicate logging implementations in multiple modules into one canonical interface
with expect/actual dispatching to android.util.Log, NSLog, and System.err respectively.
DEPS: none (self-contained — no external dependencies in build.gradle.kts)
CONSUMERS: VoiceOSCore, Cockpit, and any module that imports this logging abstraction
KMP: commonMain (Logger interface, LogLevel, PIISafeLogger, PIIRedactionHelper, LoggerFactory
expect), androidMain (AndroidLogger, LoggerFactory actual), iosMain (IosLogger, LoggerFactory
actual), desktopMain (DesktopLogger, LoggerFactory actual)
TABLES: none
KEY_CLASSES: Logger (interface), LoggerFactory (expect object), PIISafeLogger, PIISafeLoggerFactory,
PIIRedactionHelper, LogLevel
HEALTH: GREEN (minor) — PIISafeLogger defeats lazy evaluation by evaluating message() before
level check; PHONE_PATTERN and CREDIT_CARD_PATTERN produce false positives on non-PII data;
package namespace is com.avanues.logging instead of com.augmentalis.logging (inconsistency);
no logger caching in PIISafeLoggerFactory static convenience methods.

---

## Utilities | 20 kt | Tier 2
PURPOSE: KMP expect/actual wrappers for device-level capabilities: DeviceInfo (platform type,
memory, battery, device fingerprint for LaaS licensing), FileSystem (app/cache/documents
directories, read/write/delete), NetworkMonitor (connectivity state as StateFlow), Logger (basic
tag-based logging), Settings (key-value store).
WHY: Provides a single instantiable class per capability that each platform can construct without
dependency injection, primarily for the Utilities use-case before the Foundation interfaces and
Koin were adopted. The DeviceFingerprint API is unique — implements a multi-signal device hash
for LaaS license validation across Android/iOS/Desktop.
DEPS: kotlinx.coroutines.core, kotlinx.coroutines.android (androidMain), kotlinx.coroutines.swing
(desktopMain, conditional)
CONSUMERS: LaaS licensing code, legacy pre-Koin code paths, any module needing raw DeviceInfo
KMP: commonMain (5 expects + data classes), androidMain (5 actuals — all require manual
initialize(context) call before use), iosMain (5 actuals), desktopMain (5 actuals)
TABLES: none
KEY_CLASSES: DeviceInfo, DeviceFingerprint, NetworkMonitor, FileSystem, Settings, DeviceInfoFactory
HEALTH: YELLOW — Android classes require manual static init (crash if forgotten, not thread-safe);
iOS getAvailableMemory() hardcodes 30%; Desktop getBatteryInfo() always returns 100%; Desktop
hasFeature() always true for mic/camera/wifi; blocking DNS ping in isConnected() can block main thread;
duplicate Logger and FileSystem concerns vs Modules/Foundation and Modules/Logging; deprecated
NSUserDefaults.synchronize() in iOS Settings; FileSystem.android.delete() uses deleteRecursively()
for single-file contract.

---

## Localization | 4 kt | Tier 2
PURPOSE: KMP expect/actual language manager (Localizer) with 42-language declaration, key-based
translation lookup backed by an in-memory TranslationProvider, and a StateFlow-based reactive
language-change API. Persists selected language via SharedPreferences (Android) / Preferences API
(Desktop).
WHY: Provides a single source of truth for voice command string translations and UI messages across
all Avanues platforms. Decouples VoiceOSCore's command phrase localization from platform UI
layers.
DEPS: kotlinx.coroutines.core, kotlinx.coroutines.android (androidMain)
CONSUMERS: VoiceOSCore (command phrase localization), HUD display strings, Settings UI
KMP: commonMain (Localizer expect, TranslationProvider, LanguageSupport), androidMain (Localizer
actual — complete), desktopMain (Localizer actual — complete). iosMain DOES NOT EXIST — module
has no iOS target at all in build.gradle.kts.
TABLES: none
KEY_CLASSES: Localizer (expect/actual), TranslationProvider, LanguageSupport
HEALTH: RED — iosMain actual for Localizer is entirely missing; iOS builds will fail to compile.
36 of 42 declared languages have no translations (silent English fallback makes API a lie).
formatString() has argument-ordering bug with %s/%d. shutdown() is not thread-safe.
Locale code mismatch ("en" vs "en-US") between Localization and Foundation SettingsKeys.
compileSdk inconsistency (34 vs 35 in other modules).

---

## AvanuesShared | 3 kt | Tier 3
PURPOSE: iOS umbrella KMP module that re-exports VoiceOSCore, Database, Foundation, AVID,
SpeechRecognition, and Logging as a single CocoaPods framework (AvanuesShared.xcframework)
for the iOS Xcode project. Also provides the KoinHelper entry point and empty platform Koin
modules for the iOS DI container.
WHY: CocoaPods / Swift Package Manager cannot consume multiple KMP frameworks directly; an umbrella
re-exporting all modules as one framework is the standard pattern. Without this, Swift would need
separate import statements and potential symbol conflicts.
DEPS: (all via api()): VoiceOSCore, Database, Foundation, AVID, SpeechRecognition, Logging,
kotlinx.coroutines.core, kotlinx.serialization.json, kotlinx.datetime, koin.core, ktor.client.core,
ktor.client.content.negotiation, ktor.serialization.kotlinx.json (commonMain);
sqldelight.android.driver + ktor.client.okhttp (androidMain);
sqldelight.native.driver + ktor.client.darwin (iosMain)
CONSUMERS: iOS Xcode project via CocoaPods; indirectly exposes all listed modules to Swift
KMP: commonMain, androidMain, iosMain (iosX64/iosArm64/iosSimulatorArm64 via iosMain source set)
TABLES: none
KEY_CLASSES: KoinHelper, avanuesSharedModule (val), iosPlatformModule (val)
HEALTH: YELLOW — Both Koin modules are empty (no bindings registered); iosPlatformModule declares
it will register database drivers but does not; KoinHelper.doStartKoin() takes no parameters
making it impossible to inject additional modules; Utilities and Localization not exported (iOS
misses FileSystem actuals and Localizer entirely); androidMain has SQLDelight driver dependency
that belongs in app-layer DI, not the umbrella.
