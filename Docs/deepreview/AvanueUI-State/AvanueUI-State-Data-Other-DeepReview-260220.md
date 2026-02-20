# AvanueUI Remaining Sub-Modules — Deep Code Review
**Date:** 260220
**Scope:** 15 AvanueUI sub-module directories (87+ .kt files)
**Reviewer:** Code Reviewer Agent
**Branch:** HTTPAvanue

---

## Summary

The 15 reviewed AvanueUI sub-modules contain pervasive Rule 1 violations (stubs/TODOs that throw at
runtime or silently do nothing), a banned package prefix used throughout the entire Data module (14
files), `GlobalScope` coroutine leaks in StateManagement, one `System.currentTimeMillis()` call in
commonMain (KMP violation), hardcoded date values in the Input module, MaterialTheme usage banned
by MANDATORY RULE #3, wrong package namespaces in three Adapters platform files, and duplicate
class names across parallel StateManagement source trees. Total: **8 Critical, 19 High, 12 Medium,
6 Low** across all 15 modules.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **CRITICAL** | `Data/src/commonMain/.../DataGrid.kt:71` | `TODO("Platform rendering not yet implemented")` throws `NotImplementedError` at runtime — Rule 1 violation | Implement platform rendering via expect/actual or delegate to ComposeRenderer |
| **CRITICAL** | `Data/src/commonMain/.../Table.kt:64` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Avatar.kt:52` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Chip.kt:56` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../TreeView.kt:63` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Skeleton.kt:47` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Timeline.kt:64` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../List.kt:62` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Accordion.kt` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Carousel.kt` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../EmptyState.kt` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Paper.kt` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Stepper.kt` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Data/src/commonMain/.../Divider.kt` | `TODO("Platform rendering not yet implemented")` throws at runtime | Same as above |
| **CRITICAL** | `Input/.../InputComponents.kt:239` | `Date.now()` returns hardcoded `Date(2025, 12, 28)` — static past date, never correct at runtime | Use `kotlinx-datetime` `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())` |
| **CRITICAL** | `Input/.../InputComponents.kt:240` | `Date.fromTimestamp()` returns hardcoded `Date(2025, 12, 28)` — ignores `timestamp` parameter | Implement epoch-millis conversion via `kotlinx-datetime` `Instant.fromEpochMilliseconds(timestamp)` |
| **CRITICAL** | `Input/.../InputComponents.kt:243` | `Date.toTimestamp()` returns `0L` unconditionally — epoch zero, semantically wrong everywhere | Compute actual epoch milliseconds via `kotlinx-datetime` |
| **CRITICAL** | `Input/.../InputComponents.kt:259` | `Time.now()` returns hardcoded `Time(12, 0, 0)` — always noon | Use `Clock.System.now()` and extract local time |
| **CRITICAL** | `Adapters/src/androidMain/.../ComposeRenderer.kt` (entire file) | 12 `@Composable` functions (`MagicButtonCompose`, `MagicCardCompose`, etc.) have empty bodies with only `// Actual Compose implementation` comment — all stubs, Rule 1 | Implement actual Compose rendering per component type |
| **CRITICAL** | `Adapters/src/iosMain/.../SwiftUIBridge.kt:248-254` | `SwiftUIHostingController.createHostingController()` returns bare `UIViewController()` with comment "Placeholder" — stub, Rule 1 | Implement UIHostingController wrapping the Compose/SwiftUI view |
| **CRITICAL** | `AvanueLanguageServer/.../stubs/ParserStubs.kt:18-19,27-28,37-38` | `VosParser`, `JsonDSLParser`, `CompactSyntaxParser` — all `parseComponent()` methods return `Result.failure(NotImplementedError(...))` — Rule 1 | Implement actual parsers or integrate existing VosParser from VoiceOSCore |
| **CRITICAL** | `StateManagement/.../components/state/StatePersistence.kt:193-196` | `StateManager.saveViewModel()` body is empty with comment "This is a simplified version — in production, you'd want a better approach" — Rule 1 stub | Implement via `MagicViewModel.stateContainer` public API or companion save protocol |
| **HIGH** | `Data/src/commonMain/kotlin/com/avanueui/data/*.kt` (all 14 files) | Package prefix `com.avanueui.data` is the BANNED prefix per MANDATORY RULE #3 | Rename to `com.augmentalis.avanueui.data` or `com.augmentalis.avamagic.components.data` |
| **HIGH** | `StateManagement/.../components/state/MagicState.kt:33-37` | `MagicState.map()` calls `value.map(transform).stateIn(scope = GlobalScope, ...)` — `GlobalScope` coroutine, lifecycle-detached, leaks forever | Accept `CoroutineScope` parameter; callers pass viewModelScope or compositionLocalOf scope |
| **HIGH** | `StateManagement/.../components/state/MagicState.kt:47-51` | `MagicState.combine()` calls `GlobalScope` `stateIn` — same lifecycle leak | Same fix as above |
| **HIGH** | `StateManagement/.../components/state/MagicState.kt:114-121` | `derivedStateOf()` top-level function uses `GlobalScope.stateIn(Eagerly)` — hot, lifecycle-detached | Accept `CoroutineScope` parameter or make lazy/cold |
| **HIGH** | `StateManagement/.../components/state/DataBinding.kt:26` | `DataBinding<T>` constructor default `scope = kotlinx.coroutines.GlobalScope` — all callsites that omit scope get GlobalScope | Change default to `CoroutineScope(Dispatchers.Main + SupervisorJob())` and expose `close()` |
| **HIGH** | `StateManagement/.../components/state/DataBinding.kt:80` | `BidirectionalBinding<T>` default `scope = GlobalScope` — same issue, plus `unbind()` at L113-115 is a no-op stub | Fix scope default; implement `unbind()` by capturing launched jobs and cancelling them |
| **HIGH** | `StateManagement/.../components/state/DataBinding.kt:145` | `PropertyBinding.asFlow()` uses `GlobalScope.stateIn(Eagerly)` | Accept `CoroutineScope` as `asFlow(scope)` parameter |
| **HIGH** | `StateManagement/.../components/state/DataBinding.kt:157` | `CollectionBinding<T>` default `scope = GlobalScope` | Same fix as DataBinding |
| **HIGH** | `StateManagement/.../components/state/StatePersistence.kt:206` | `StateManager.autoSave()` launches on `kotlinx.coroutines.GlobalScope` directly — unbound lifetime | Accept `CoroutineScope` parameter; return `Job` so caller can cancel |
| **HIGH** | `StateManagement/.../state/ReactiveComponent.kt:32,65,138` | `ReactiveComponent`, `MultiStateReactiveComponent`, `ReactiveListComponent` each create `CoroutineScope(Dispatchers.Main)` per instance with no `cancel()` path exposed — scope leaks on GC | Implement `Closeable` / `AutoCloseable`; expose `dispose()` that cancels scope |
| **HIGH** | `StateManagement/.../state/MagicState.kt (outer):151-153` | `DerivedMagicStateImpl.asFlow()` returns `MutableStateFlow(value).asStateFlow()` — captures value at call time, non-reactive snapshot | Return a flow that re-derives on each collection cycle (via `flow { emit(calculation()) }`) |
| **HIGH** | `StateManagement/.../state/StateManager.kt:96` | `StateSnapshot` uses `System.currentTimeMillis()` in commonMain — JVM-only API, KMP violation | Replace with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()` |
| **HIGH** | `Adapters/src/commonMain/.../adapters/ComposeRenderer.kt:490-494` | `renderAlert()` uses `MaterialTheme.colorScheme.secondaryContainer`, `MaterialTheme.colorScheme.tertiaryContainer`, `MaterialTheme.colorScheme.errorContainer` — BANNED per MANDATORY RULE #3 | Replace with `AvanueTheme.colors.secondary`, `AvanueTheme.colors.tertiary`, `AvanueTheme.colors.error` |
| **HIGH** | `Adapters/src/androidMain/.../compose/ComposeRenderer.kt:1` | Package is `net.ideahq.avamagic.adapters.compose` — wrong namespace, not under `com.augmentalis.*` | Rename to `com.augmentalis.avamagic.adapters.compose` and update all imports |
| **HIGH** | `Adapters/src/iosMain/.../swiftui/SwiftUIBridge.kt:1` | Package is `net.ideahq.avamagic.adapters.swiftui` — wrong namespace | Rename to `com.augmentalis.avamagic.adapters.swiftui` |
| **HIGH** | `Adapters/src/jsMain/.../react/ReactBridge.kt:1` | Package is `net.ideahq.avamagic.adapters.react` — wrong namespace | Rename to `com.augmentalis.avamagic.adapters.react` |
| **HIGH** | `AvanueLanguageServer/.../MagicUITextDocumentService.kt` (~L1067) | `formatDocument()` body has `// TODO: Implement formatting` and returns `emptyList()` — stub, Rule 1 | Implement document formatting using the VOS/JSON DSL parsers and kotlinx-serialization pretty-print |
| **HIGH** | `AvanueLanguageServer/.../MagicUITextDocumentService.kt` (multiple lines) | All diagnostic `line` fields hardcoded to `0` (e.g., L316, L335, L340, L349, L365, L374, L388, L395) — every diagnostic always points to line 0, completely unusable for IDE navigation | Compute actual line offsets from document content; store offset-to-line mapping |
| **MEDIUM** | `StateManagement/.../components/state/StateScope.kt:116-119` | `clearTransient()` is an empty stub with comment "just a placeholder for future enhancement" — Rule 1 | Implement by tagging states as transient (add `enum Tag { TRANSIENT, PERSISTENT }` to `remember`) or remove method from API until implemented |
| **MEDIUM** | `StateManagement/.../state/Core.kt:8-9` | File comments explicitly say "These are stub interfaces that should be replaced with actual implementations from a Core module when available" — documented stub file | Replace with real implementations from Foundation module or remove the file |
| **MEDIUM** | `StateManagement/src/.../state/MagicState.kt` AND `StateManagement/src/.../components/state/MagicState.kt` | `MagicState<T>` defined in two files: one is an abstract class (`components.state`), the other an interface (`state`) — duplicate conflicting class names in same module | Remove one; the Compose-aware interface version in `state/MagicState.kt` is the cleaner design — migrate users and delete `components.state.MagicState` |
| **MEDIUM** | `StateManagement` module (multiple files) | 7 classes defined in BOTH `components/state/` and `state/`: `MagicState`, `StateContainer`, `DataBinding`, `FormState`, `MagicViewModel`, `StatePersistence`, `StateScope` — two parallel state hierarchies in the same module | Decide on one canonical tree; delete the other; the `state/` tree has Compose integration and is more modern |
| **MEDIUM** | `StateManagement/.../state/StateBuilder.kt:49-50` | `derivedState()` comment acknowledges "In a real implementation, you'd want to track dependencies" — incomplete reactive dependency tracking | Integrate with Compose `derivedStateOf` or implement a dependency tracker |
| **MEDIUM** | `StateManagement/.../state/ComputedState.kt:26` | `ComputedState` creates `CoroutineScope(SupervisorJob() + Dispatchers.Main)` per instance with no cancellation path exposed | Expose `dispose()` / implement `Closeable` |
| **MEDIUM** | `StateManagement/.../state/StateContainer.kt:107` | `typealias MutableMagicState<T> = MagicState<T>` — makes mutable and immutable aliases identical at the type level, losing type safety | Use a separate concrete class or sealed hierarchy for explicit mutability |
| **MEDIUM** | `XR/src/commonMain/.../xr/XRState.kt:5` | Header field `Author: VOS4 Development Team` — Rule 7 violation (must use "Manoj Jhawar" or omit entirely) | Change to `@author Manoj Jhawar` or omit |
| **MEDIUM** | `XR/src/commonMain/.../xr/CommonXRManager.kt:5` (approx) | Header field `Author: VOS4 Development Team` — Rule 7 violation | Same fix |
| **MEDIUM** | `ARGScanner/src/commonMain/.../argscanner/ARGScanner.kt:127-130` | `watch()` method body is empty with `// TODO: Implement file system watcher` comment — stub, Rule 1 | Implement using `kotlinx-io` directory watching or expect/actual platform FS event APIs |
| **MEDIUM** | `UIConvertor/.../themebridge/ThemeMigrationBridge.kt:143,151` | `isSyncingToLegacy` and `isSyncingFromLegacy` are plain `var Boolean` with no `@Volatile` or synchronization — data race if sync is triggered from multiple coroutines | Add `@Volatile` annotations; or use `AtomicBoolean` via `kotlinx-atomicfu` |
| **MEDIUM** | `Floating/.../floating/FloatingComponents.kt` | `FloatingCommandBar` is a `data class` with `var` fields — mutable `data class` anti-pattern; `copy()` and `equals()` will behave unexpectedly | Convert to plain `class` with explicit copy/equality; or use `@Immutable data class` with immutable types |
| **MEDIUM** | `Floating/.../floating/FloatingComponents.kt` | `AVAIntegration`, `SearchIntegration`, `SettingsIntegration` all have `@Transient var` lambda callback fields annotated `@Serializable` — lambdas are silently dropped on deserialization, leaving null callbacks with no error | Do not annotate callback-holding classes as `@Serializable`; split data (serializable) from behavior (not serializable) |
| **LOW** | `XR/src/commonMain/.../xr/XRState.kt` | `PerformanceWarning.timestamp: Long = 0L` with comment "should be provided by platform-specific code" but there is no enforcement (no `require`, no `init` check) | Add `init { require(timestamp > 0L) { "PerformanceWarning timestamp must be set" } }` or use `kotlinx-datetime` `Instant` type |
| **LOW** | `UIConvertor/.../themebridge/ColorConversionUtils.kt` | Imports `com.augmentalis.voiceos.colorpicker.ColorRGBA` — cross-module dependency from VoiceOS into AvanueUI | Move `ColorRGBA` to Foundation or AvanueUI CoreTypes to avoid circular/cross-module coupling |
| **LOW** | `StateManagement/.../state/DataBinding.kt` top-level factory functions | `dataBindingOf`, `MutableMagicState.bindTo`, `collectionBindingOf` all default to `GlobalScope` — callers who use the convenience functions silently get lifecycle-detached bindings | Require explicit scope parameter, removing GlobalScope default from public API |
| **LOW** | `AvanueLanguageServer/.../lsp/ParserStubs.kt:1` | File comment says "Temporary stub implementations" — this file is a declared placeholder | Track in issue tracker; do not ship in production builds. Apply `@Suppress("UNUSED")` or gate with `BuildConfig.DEBUG` |
| **LOW** | `StateManagement/src/.../state/MagicState.kt:168-174` | `magicDerivedStateOf` wraps Compose `derivedStateOf` correctly but the `asFlow()` inside the anonymous object returns `MutableStateFlow(value).asStateFlow()` — non-reactive snapshot | Same fix as DerivedMagicStateImpl.asFlow(): use cold `flow { emit(calculation()) }` |
| **LOW** | `XR/src/commonMain/.../xr/CommonXRManager.kt` | Copyright header says "Manoj Jhawar/Aman Jhawar" but Author line says "VOS4 Development Team" — inconsistent; Rule 7 requires consistent author attribution | Remove "VOS4 Development Team" from Author field; keep copyright attribution |

---

## Module-by-Module Summary

### StateManagement (17 files)
The module is architecturally split into two parallel class hierarchies:
- `components/state/` — Flow-based state (`abstract class MagicState`, `StateContainer`, etc.)
- `state/` — Compose-integrated state (`interface MagicState`, `MutableMagicState`, etc.)

Seven classes share names across these two trees. This creates import ambiguity and indicates the
module was built twice from different starting points without reconciliation. The `state/` tree is
the superior design (Compose-aware, cleaner API). The `components/state/` tree is the older
layer and should be deprecated and removed.

The dominant pattern defect across `components/state/` is defaulting to `GlobalScope` for
coroutine scopes. Every binding, persistence auto-save, and derived state flow is attached to
`GlobalScope`. This means these objects live for the entire app process lifetime regardless of
which composable or viewmodel created them.

`StateManager.saveViewModel()` (StatePersistence.kt:193) is a complete stub — empty body, no
persistence occurs. `StateScope.clearTransient()` (StateScope.kt:116) is similarly empty.
`BidirectionalBinding.unbind()` (DataBinding.kt:113) is documented as a no-op. Three stubs in one
module.

### Data (14 files)
All 14 files use the banned `com.avanueui.data` package prefix. All 14 `render()` implementations
throw `NotImplementedError` via `TODO()`. This entire module is non-functional at runtime. No
consumer can call `render()` on any Data component without a crash. This is the highest-density
stub cluster in the codebase.

### Adapters (9 files)
The androidMain `ComposeRenderer.kt` has 12 stub `@Composable` functions. The iosMain
`SwiftUIBridge.kt` returns a bare `UIViewController()`. Both platform-specific files also use the
wrong package namespace (`net.ideahq.avamagic.*` instead of `com.augmentalis.*`). The commonMain
`ComposeRenderer.kt` uses banned `MaterialTheme.colorScheme.*` in `renderAlert()`.

### Input (2 files)
`Date.now()`, `Date.fromTimestamp()`, and `Date.toTimestamp()` are hardcoded to a static past date
(2025-12-28) and zero respectively. `Time.now()` returns always noon. Any UI using DatePicker or
TimePicker from this module will display the wrong date/time silently.

### AvanueLanguageServer (8 files)
All three parsers in `ParserStubs.kt` fail with `NotImplementedError`. `formatDocument()` is a
stub returning empty. All diagnostic line numbers are hardcoded to zero, making the LSP entirely
useless for editor navigation. The LSP is non-functional as shipped.

### ARGScanner
`watch()` is empty. The file watcher feature is completely unimplemented.

### XR (6 files)
Clean data models with one Rule 7 violation (`Author: VOS4 Development Team`) in both `XRState.kt`
and `CommonXRManager.kt`.

### UIConvertor (3 files)
`ThemeMigrationBridge.kt` has unsynchronized `var Boolean` sync flags. `ColorConversionUtils.kt`
has a cross-module dependency on VoiceOS that should be moved to Foundation.

### Floating (1 file)
Mutable `data class` + `@Serializable` with lambda fields — silent data loss on deserialization.

### Display, Feedback, Layout, Navigation, VoiceCommandRouter, CoreTypes (clean)
These modules contain clean data/model definitions with no render stubs, no banned packages, no
MaterialTheme violations, and no GlobalScope usage. No issues of severity Medium or above found.

---

## Recommendations

1. **Data module: mass package rename + implement render()**
   All 14 files need package renamed from `com.avanueui.data` to `com.augmentalis.avamagic.components.data`.
   The `render()` method on each `Component` should delegate to a `ComposeRenderer.render(this)` call
   via the expect/actual Renderer abstraction. Use the existing `ComposeRenderer` in Adapters as the
   actual implementation target.

2. **StateManagement: choose one tree, delete the other**
   The `state/` (Compose-integrated) tree is the right long-term design. Migrate any unique
   functionality from `components/state/` into `state/`, then delete `components/state/` entirely.
   This eliminates all 7 duplicate class conflicts.

3. **Replace GlobalScope with caller-provided CoroutineScope everywhere in StateManagement**
   `DataBinding`, `BidirectionalBinding`, `CollectionBinding`, `PropertyBinding.asFlow()`,
   `StateManager.autoSave()`, `MagicState.map()`, `MagicState.combine()`, `derivedStateOf()` —
   all must accept an explicit `CoroutineScope` with no GlobalScope default. The public API surface
   of this module is almost entirely lifecycle-detached today.

4. **Input module: replace hardcoded dates with kotlinx-datetime**
   Add `org.jetbrains.kotlinx:kotlinx-datetime` to the Input module's commonMain dependencies.
   Implement `Date.now()` and `Time.now()` using `Clock.System.now()`.
   Implement `Date.fromTimestamp()` using `Instant.fromEpochMilliseconds(timestamp).toLocalDateTime()`.
   Implement `Date.toTimestamp()` by constructing an `Instant` from the date fields.

5. **Adapters androidMain: implement the 12 stub Composable functions**
   Each function corresponds to a specific component type (Button, Card, TextField, etc.).
   Map each to the equivalent AvanueUI unified component (`AvanueButton`, `AvanueCard`, etc.) using
   `AvanueTheme.colors.*` tokens. This makes the entire Adapters layer functional.

6. **Fix wrong package namespaces in Adapters platform files**
   `net.ideahq.avamagic.adapters.*` → `com.augmentalis.avamagic.adapters.*` in androidMain,
   iosMain, and jsMain files. Update all import references.

7. **Remove MaterialTheme.colorScheme usage from Adapters commonMain ComposeRenderer**
   `renderAlert()` L490-494: replace `MaterialTheme.colorScheme.secondaryContainer/tertiaryContainer/errorContainer`
   with `AvanueTheme.colors.secondary`, `AvanueTheme.colors.surface`, `AvanueTheme.colors.error`.

8. **Fix System.currentTimeMillis() KMP violation in StateManager.kt:96**
   `StateSnapshot.timestamp` default uses `System.currentTimeMillis()` — JVM-only.
   Replace with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`.

9. **Fix Rule 7 violations in XR files**
   `XRState.kt` and `CommonXRManager.kt`: change `Author: VOS4 Development Team` to
   `@author Manoj Jhawar` or remove the author line entirely.

10. **Implement ARGScanner.watch() or gate it as unimplemented**
    Either add a real file system watcher using `kotlinx-io` or an expect/actual platform API,
    or throw `UnsupportedOperationException("File watching not yet supported")` with a GitHub issue
    reference so callers are aware of the limitation rather than silently doing nothing.

11. **Add @Volatile to ThemeMigrationBridge sync flags**
    `isSyncingToLegacy` and `isSyncingFromLegacy` should be `@Volatile var Boolean` or replaced with
    `kotlinx-atomicfu` `AtomicBoolean` to prevent data races on the migration bridge.

12. **Refactor FloatingCommandBar from mutable data class**
    Remove `var` fields; separate `FloatingCommandBarConfig` (immutable, serializable, no lambdas)
    from `FloatingCommandBarState` (mutable, not serializable, holds callbacks).

---

**Total findings:** 8 Critical (22 individual instances) | 19 High | 12 Medium | 6 Low
