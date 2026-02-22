# AvanueUI Module — Quality Analysis Review
**Date:** 260222
**Reviewer:** code-reviewer agent
**Scope:** `Modules/AvanueUI/` (431 kt files, 21 sub-modules)
**Branch:** VoiceOS-1M-SpeechEngine

---

## Summary

AvanueUI is a large, multi-sub-module design system that operates at two distinct layers: a **custom DSL/renderer layer** (Theme, Core, Renderers, CoreTypes, etc.) and a **Compose-facing component layer** (Adapters, Voice, ThemeBuilder overlay). The DSL layer is architecturally sound but contains **massive stub coverage** — essentially every `render()` method in `Components.kt` and all `Data/` sub-module components throws `TODO("Platform rendering not yet implemented")`, making the DSL layer non-functional at runtime. The Compose-facing layer has **pervasive `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*` violations** (banned by CLAUDE.md Rule #3) across 10+ files spanning Renderers, Adapters, Voice, and ThemeBuilder. AVID voice identifiers are completely absent from all components. The `ThemeBuilder` tool, which is supposed to demonstrate AvanueUI v5.1 compliance, instead uses `MaterialTheme{}` as its root provider. The `Data/` sub-module uses the banned `com.avanueui.*` package namespace. The `CloudThemeRepository` implements 9 interface methods as no-ops.

---

## Score

| Dimension | Score | Notes |
|-----------|-------|-------|
| Readability | 7/10 | DSL type model is clear; file naming is consistent |
| SOLID | 5/10 | ISP violated by `CloudThemeRepository` (9 stubs); SRP inconsistent in Adapters |
| DRY | 6/10 | Some duplication between `Core/display/` and `Data/` for identical component types |
| Error Handling | 4/10 | TODO-stubs silently fail; Cloud repo returns null/empty with only println logging |
| Security | 8/10 | No secrets found; no injection vectors |
| Performance | 7/10 | No N+1 or allocation issues identified |
| Tests | 5/10 | Tests exist for VoiceCommandRouter; none for components or renderers |
| **OVERALL** | **5.5/10** | Blocked by mass render stubs and theme violations |

**Health: RED** — DSL render layer entirely non-functional; theme system violates project-wide mandate in 10+ files.

---

## P0 Issues (Blocking — will crash or produce wrong output at runtime)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `Core/src/commonMain/.../base/Components.kt:17,30,42,54,66,84,100,113,127,148,161,174,191,213,228,250,264,279,324,340,358,383,413,435,447,464,485` | **28 `render()` methods all throw `TODO("Platform rendering not yet implemented")`**. Every core component (Column, Row, Card, Text, Button, Image, Checkbox, TextField, Switch, Icon, Radio, Slider, Dropdown, DatePicker, TimePicker, FileUpload, SearchBar, Rating, Dialog, Toast, Alert, ProgressBar, Spinner, Badge, Tooltip, etc.) crashes if `render()` is called. Any caller exercising the DSL layer at runtime will get `NotImplementedError`. | Implement platform-specific rendering. For Android: delegate to Compose in an `androidMain` expect/actual `render()` impl. Mark abstract in commonMain if platform-specific. Do NOT ship TODO bodies. |
| Critical | `Data/src/commonMain/.../Chip.kt:56`, `Avatar.kt:52`, `Paper.kt:51`, `Stepper.kt:58`, `Timeline.kt:64`, `List.kt:62`, `TreeView.kt:63`, `EmptyState.kt:54`, `Table.kt:64`, `Carousel.kt:65`, `Accordion.kt:63`, `DataGrid.kt:70`, `Skeleton.kt:47`, `Divider.kt:49` | **14 additional `render()` stubs** in the `Data/` sub-module — entire sub-module is non-functional at runtime. | Same fix as above. |
| Critical | `AvanueLanguageServer/src/main/.../stubs/ParserStubs.kt:18,28,38` | `VosParser`, `JsonDSLParser`, `CompactSyntaxParser` all return `Result.failure(NotImplementedError("Parser not available"))`. The LSP's validation and component parsing capabilities are entirely absent. `// TODO: Remove when actual modules are available` has been in-file since inception. | Connect to real parser modules or remove the LSP submodule until parsers exist. A shipped stub that always fails violates Rule 1. |

---

## P1 Issues (High — wrong behavior, theme violations, architectural problems)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `ThemeBuilder/src/jvmMain/.../Main.kt:55-57` | **`MaterialTheme{}` is the root theme provider** for the entire ThemeBuilder app: `MaterialTheme(colorScheme = if (state.isDarkMode) darkColorScheme() else lightColorScheme())`. The ThemeBuilder is the tool for authoring AvanueUI themes — it must itself use `AvanueThemeProvider`. This is both a functional contradiction and a direct violation of CLAUDE.md Rule #3. The `TODO(RULE3)` comments acknowledge this but leave it unresolved. | Replace `MaterialTheme{}` root with `AvanueThemeProvider(colors=..., glass=..., water=..., materialMode=..., isDark=...)`. Add `:Modules:AvanueUI:DesignSystem` dependency to `ThemeBuilder`'s `jvmMain` source set. |
| High | `ThemeBuilder/src/jvmMain/.../Main.kt:115,149,217,239,273,288,307,360,415,428,473,525,579` | **13+ `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*` references** throughout the ThemeBuilder UI (TopAppBar, Gallery, Canvas, Inspector, dialogs). All are banned by CLAUDE.md Rule #3. They are acknowledged with `TODO(RULE3)` but still active. | After fixing the root provider, replace all these with `AvanueTheme.colors.*`. |
| High | `Adapters/src/androidMain/.../ComposeUIImplementation.kt:359-364,443,499` | **`MaterialTheme.typography.*` used in the Compose adapter layer** (`textVariant` mapper, card title, tab title). The adapter is the bridge between DSL and Compose — it must use `AvanueTheme` to carry the design system identity. | Replace with `AvanueTheme.typography.*` (or the appropriate token from the type scale). |
| High | `Renderers/Android/src/.../mappers/AvatarMapper.kt:56` | `MaterialTheme.typography.titleMedium` used for initials text. Same file also imports `com.augmentalis.avanueui.theme.AvanueTheme` and correctly uses `AvanueTheme.colors.primaryContainer` — the typography access is an inconsistency. | Replace `MaterialTheme.typography.titleMedium` with `AvanueTheme.typography.titleMedium` (or the AvanueUI type token). |
| High | `Renderers/Android/src/.../mappers/BreadcrumbMapper.kt:57,61,81`, `ModalMapper.kt:57`, `ToastMapper.kt:76`, `AlertMapper.kt:79,88`, `AdvancedFeedbackMappers.kt:84` | **7 renderer mapper files** use `MaterialTheme.typography.*` for text styles, bypassing the AvanueUI token system. | Replace all with `AvanueTheme.typography.*`. |
| High | `Renderers/Android/src/.../extensions/LayoutDisplayExtensions.kt` (L708,717,743,753,824,927,978,984,1024,1049,1104,1255,1261,1268,1307,1352,1372,1416,1451,1606,1748,1757) | **22+ `MaterialTheme.typography.*` accesses** in `LayoutDisplayExtensions.kt` alone — the largest single file violation. | Systematic replace-all with AvanueTheme typography tokens. |
| High | `src/androidMain/.../overlay/MagicCommandOverlay.kt:263,440` | `MaterialTheme.typography.titleMedium` and `.labelSmall` in the voice command overlay. This component is voice-facing and sits in the critical path for VoiceOS UX. | Replace with `AvanueTheme.typography.*`. |
| High | `src/commonMain/.../settings/SettingsComponents.kt:72,166,170,178,228,289,293,374,378,533` | **10 `MaterialTheme.typography.*` usages** in the shared settings components. These are cross-platform components that must use the AvanueUI token system. | Replace with `AvanueTheme.typography.*`. |
| High | `Theme/src/commonMain/.../ThemeRepository.kt:246-298` | `CloudThemeRepository` implements 9 interface methods (`saveUniversalTheme`, `loadUniversalTheme`, `saveAppTheme`, `loadAppTheme`, `deleteAppTheme`, `saveAppOverride`, `loadAppOverride`, `deleteAppOverride`, `loadAllAppThemes`, `clearAll`) as complete no-ops with only `println()` logging. Returns null/empty from all read operations. This violates Rule 1. | Either implement actual cloud persistence or remove `CloudThemeRepository` from the codebase until it can be implemented. Do not ship a class that silently does nothing. |
| High | `Theme/src/commonMain/.../ThemeIO.kt:251,262` | `ThemeIO.defaultImporters()` returns an empty list; `ThemeIO.defaultExporters()` returns an empty list. All import/export capability is commented out. The ThemeIO pipeline is completely non-functional. | Implement at least `JsonThemeImporter` and `JsonThemeExporter` or remove the dead interface. |
| High | `AssetManager/src/.../AssetVersionManager.kt:325,331` | `saveVersionHistory()` returns `Result.success(Unit)` without persisting anything. `loadVersionHistory()` always returns null. Version history is silently lost. | Implement file/datastore persistence or surface an error. Do not return success for a no-op write. |

---

## P2 Issues (Medium — code smells, maintainability, structural gaps)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Medium | `Data/src/commonMain/kotlin/com/avanueui/data/` (all 14 files) | **Banned package namespace `com.avanueui`**. CLAUDE.md explicitly bans `com.avanueui.*`. These 14 files use this namespace. The canonical namespace is `com.augmentalis.avamagic.*` or `com.augmentalis.avanueui.*`. | Rename package in all 14 Data files to `com.augmentalis.avamagic.ui.data.*`. Update imports in consumers. |
| Medium | `Core/build.gradle.kts:22-26`, `Theme/build.gradle.kts:21-68`, `Display/build.gradle.kts:20,44` (and 6 other sub-modules) | **iOS, JVM, Desktop targets disabled with TODO comments** — 8 sub-modules are effectively Android-only despite claiming to be KMP. The comment "temporarily Android-only" appears across the entire module. Until these are re-enabled, the module cannot fulfill the KMP promise. | Track this as a tracked debt item. Either add a `multiplatformTargets` property to centralize the decision, or formally mark affected sub-modules as `androidMain`-only libraries. |
| Medium | `Core/src/commonMain/.../display/` and `Data/src/commonMain/` | **Duplicate component definitions** — both `Core/display/Avatar.kt` and `Data/Avatar.kt` exist, same for `Chip`, `Table`, `TreeView`, `Badge`, `Tooltip`. Two parallel component trees exist without a clear canonical one. DRY violation. | Consolidate to one canonical location. `Core/` likely wins; `Data/` should be removed or scoped to data-presentation-specific extensions only. |
| Medium | `AvanueLanguageServer/src/main/.../MagicUIWorkspaceService.kt:41,51,63,160,178,194` | 6 empty/no-op handler methods in the LSP workspace service (`didChangeConfiguration`, `didChangeWatchedFiles`, `didChangeWorkspaceFolders`, `executeCommand`, `formatting`, `codeAction`). The LSP provides no real functionality beyond stub stubs. | Either implement or explicitly document that these LSP events are no-op intentionally. Blank methods with TODO comments look like forgotten work. |
| Medium | `ARGScanner/src/commonMain/.../ARGScanner.kt:128` | `watch()` suspend function has `// TODO: Implement file system watcher` body — the function is a no-op. File watching is documented as a key feature in `ARGScanner/README.md`. | Implement using `kotlinx-io` or platform expect/actual. |
| Medium | `Voice/src/main/java/.../VoiceComponents.kt:1-9` | File header: `Author: VOS4 Development Team`. This violates Rule 7 (no team/AI attribution in author fields — use "Manoj Jhawar" or omit). | Change to `@author Manoj Jhawar` or remove the author line. |
| Medium | All interactive composables across `Renderers/Android/`, `Voice/`, `Adapters/` | **Zero AVID voice identifiers** on any Compose component in this module. Buttons, chips, inputs, dialogs, navigation items — none carry `semantics { contentDescription = "Voice: ..." }`. AVID is a zero-tolerance requirement per CLAUDE.md. | Add AVID semantics to all interactive Compose elements: `Modifier.semantics { contentDescription = "Voice: click [label]" }`. |
| Medium | `Renderers/Android/src/.../extensions/InputExtensions.kt:47,52,87,92` | `MaterialTheme.typography.*` usages (4 occurrences) in input field extensions — missed in mapper count above. | Replace with AvanueTheme typography tokens. |
| Medium | `src/commonMain/.../components/navigation/GroupedListDetail.kt:131,186,238` | 3 `MaterialTheme.typography.*` usages in grouped list navigation. | Replace with AvanueTheme typography tokens. |

---

## Code Smells

| File | Smell | Detail |
|------|-------|--------|
| `Core/base/Components.kt` | God file | 492 lines defining 28 component data classes, all with identical `TODO` render bodies. Should be one class per file or at least grouped by category into separate files. |
| `ThemeBuilder/Main.kt` | Rule 1 violation masked by TODO comments | 10+ `TODO(RULE3)` comments acknowledge the violation exists but the code still ships. Comments do not make violations acceptable. |
| `Data/build.gradle.kts` | MinSdk mismatch | `Data` module sets `minSdk = 26` while `Core` sets `minSdk = 24`. Downstream consumers at API 24-25 importing both will hit a conflict. Needs alignment at API 24. |
| `Voice/` module | Wrong source set path | Files live in `src/main/java/` (not `src/androidMain/kotlin/`). This is the legacy Android source set convention, not KMP convention. All KMP modules must use `src/androidMain/kotlin/`. |
| `Adapters/ComposeUIImplementation.kt` | Old package namespace | Package `net.ideahq.avamagic` is a legacy domain. All new code uses `com.augmentalis.*`. This file is the only consumer of `net.ideahq.*` — it is isolated but inconsistent. |
| `Theme/ThemeRepository.kt` | ISP violation | `CloudThemeRepository` is forced to implement 9 methods it does not support. The `ThemeRepository` interface should be split: `LocalThemeRepository` and `CloudThemeRepository` with separate interface contracts. |

---

## Missing Implementations (Known Gaps)

| Gap | Location | Impact |
|-----|----------|--------|
| All DSL render implementations | `Core/base/Components.kt`, `Data/src/` | Runtime crash for any DSL caller |
| Theme importers/exporters | `Theme/ThemeIO.kt:250-266` | ThemeIO API returns nothing |
| Cloud persistence | `Theme/ThemeRepository.kt:246-298` | All cloud saves lost silently |
| AssetVersionManager persistence | `AssetManager/AssetVersionManager.kt:325-334` | Version history lost silently |
| ARGScanner file watching | `ARGScanner/ARGScanner.kt:128` | Feature listed in README, not implemented |
| iOS/JVM/Desktop targets | 8 sub-modules | `Modules/AvanueUI:Core/Theme/Display/Input/Layout/Data/Feedback/Floating` Android-only |
| AVID on all components | All Compose components | Voice-first requirement unmet across entire module |
| AvanueLanguageServer parsing | `stubs/ParserStubs.kt` | All parse operations return `NotImplementedError` |

---

## Deprecated Usage

No deprecated AvanueUI v5.1 APIs found (`OceanColors`, `SunsetColors`, `LiquidColors`, `AvanueThemeVariant`, `MaterialMode.PLAIN`) — the module predates or is below the DesignSystem layer, so it does not import those. The module's own internal theme (`GlassAvanue`, `Theme`, `ColorScheme`) is a parallel system rather than the canonical v5.1 axes.

**Key observation:** `AvanueColorPalette`, `MaterialMode` (v5.1 axes), `AvanueTheme.colors.*`, and `AvanueThemeProvider` are **entirely absent** from the module. The module operates on its own `Theme`/`ColorScheme`/`ThemePlatform` type system (which is the pre-v5.1 generic layer). The Android Compose renderers then bypass both layers and fall back to `MaterialTheme.*`. This means the v5.1 design system is not actually wired into any rendering path within this module.

---

## Recommendations

1. **Immediately**: Remove or properly implement the `CloudThemeRepository` and `ThemeIO` stubs. Silently returning null/empty from all operations while calling it a "repository" is the most dangerous pattern in the module — callers will think data is being saved.

2. **High priority**: Add `:Modules:AvanueUI:DesignSystem` dependency to `ThemeBuilder`'s `jvmMain` source set and replace `MaterialTheme{}` root with `AvanueThemeProvider`. All `TODO(RULE3)` items then resolve.

3. **High priority**: Run a systematic `MaterialTheme.typography.*` → `AvanueTheme.typography.*` replacement across all Compose files in the module. This affects at minimum: `LayoutDisplayExtensions.kt` (22 hits), `AdvancedInputMappers.kt` (8 hits), `InputExtensions.kt` (7 hits), `ComposeUIImplementation.kt` (7 hits), `SettingsComponents.kt` (10 hits), `MagicCommandOverlay.kt` (2 hits), `VoiceComponents.kt` (1 hit), and 5 mapper files.

4. **High priority**: Add AVID semantics to all interactive Compose elements. A helper extension like `fun Modifier.avid(label: String) = semantics { contentDescription = "Voice: $label" }` can accelerate this across the module.

5. **Medium priority**: Consolidate `Core/display/` and `Data/` duplicate component types. Pick one location (prefer `Core/`) and remove duplicates.

6. **Medium priority**: Rename `Data/` package from `com.avanueui.data` to `com.augmentalis.avamagic.ui.data`.

7. **Medium priority**: Migrate `Voice/src/main/java/` to `Voice/src/androidMain/kotlin/` to comply with KMP source set conventions.

8. **Low priority**: Replace `Author: VOS4 Development Team` in `VoiceComponents.kt` header with `@author Manoj Jhawar`.

9. **Low priority**: Align `minSdk` between `Data` (26) and `Core` (24) — set both to 24.
