# AvanueUI — Master Analysis Entry
**Date:** 260222 | **Reviewer:** code-reviewer agent | **Health:** RED

---

## PURPOSE

AvanueUI is the shared design system and component library for the entire NewAvanues ecosystem. It operates at three distinct layers:

1. **DSL / Type Model Layer** — platform-agnostic component data classes (`Core/base/Components.kt`, `Data/`) that form a declarative component tree. Each component has a `render(renderer: Renderer): Any` method intended for platform dispatch.
2. **Theme / Style Layer** — `Theme` module provides `Theme`, `ColorScheme`, `Typography`, `Shapes`, `MaterialSystem` data classes, plus `GlassAvanue` predefined themes and `ThemeManager`/`ThemeRepository` persistence. `ThemeBuilder` is a JVM desktop tool for live theme authoring.
3. **Platform Renderer Layer** — `Renderers/Android/` and `Renderers/iOS/` map DSL component types to Compose/SwiftUI primitives via mapper classes. `Adapters/` provides a `ComposeUIImplementation` adapter. `CoreTypes/` provides type-safe `MagicDp`/`MagicSp`/`MagicPx`/`MagicColor` value classes.

Supporting sub-modules: `Voice/` (voice UI components), `VoiceCommandRouter/` (KMP voice intent routing), `AvanueUIVoiceHandlers/` (handler registry per component type), `StateManagement/` (reactive state containers), `ARGScanner/` (asset registry scanner), `AssetManager/` (library version management), `AvanueLanguageServer/` (LSP for the DSL format), `XR/` (XR-specific extensions), `Floating/` (floating panel components).

---

## WHY

The module exists to:
- Provide a single source of truth for design tokens, typography, and color (replacing ad-hoc per-screen theming)
- Allow the DSL to be written once and rendered on Android, iOS, Desktop, and Web
- Provide voice-first interaction semantics for every UI component via AVID
- Support the AvanueUI v5.1 design system (3-axis: `AvanueColorPalette` x `MaterialMode` x `AppearanceMode`)

---

## DEPS

### Consumed by this module
- `androidx.compose.ui`, `androidx.compose.material3`, `androidx.compose.foundation` (androidMain only)
- `kotlinx-coroutines-core`, `kotlinx-serialization-json`, `kotlinx.datetime`
- `coil` (Adapters — image loading)
- `androidx.core:core-ktx`, `com.google.android.material`
- `org.eclipse.lsp4j` (AvanueLanguageServer — JVM only)

### Internal deps (sub-module graph)
```
Theme → Core
Data → Core
Display → Core
Input → Core
Feedback → Core
Layout → Core
Floating → Core
Navigation → Core
Adapters → Core, CoreTypes
Renderers/Android → Core, CoreTypes, AvanueUI (DesignSystem — partial)
ThemeBuilder → Core, Theme
AvanueUIVoiceHandlers → (commonMain only, no Compose dep)
VoiceCommandRouter → (commonMain + androidMain)
```

---

## CONSUMERS

- `Apps/Android/VoiceOS/` — via `MagicCommandOverlay`, `VoiceComponents`
- `Modules/VoiceOSCore/` — references AvanueUI theme tokens
- `Modules/Cockpit/` — consumes AvanueUI components for frame content rendering
- `Modules/DeviceManager/` — imports theme tokens (with legacy MaterialTheme violations noted separately)
- `ThemeBuilder` — standalone JVM desktop app for theme authoring

---

## KMP

| Target | Status |
|--------|--------|
| Android | Fully declared; renderer and adapter fully implemented |
| iOS | `Renderers/iOS/` exists with SwiftUI mappers; `iosMain` source sets in Core/Theme/Data disabled via TODO |
| Desktop (JVM) | `ThemeBuilder` targets JVM; `Core/Theme/Data` JVM targets disabled via TODO; `Renderers/Desktop/` partially implemented |
| Web | No web source sets; `Renderers/Web/` contains only a standalone HTML visual builder tool |

**KMP Score: Low** — Despite being structured as KMP, the critical `Core`, `Theme`, `Data`, `Display`, `Input`, `Layout`, `Feedback`, and `Floating` sub-modules are Android-only. iOS/JVM targets are commented out across 8 sub-modules with `// TODO: Re-enable` notes. The module does not fulfill its multiplatform contract.

---

## KEY_CLASSES

| Class/Object | File | Role |
|---|---|---|
| `Theme` | `Core/base/Theme.kt` | Root theme data class with `ColorScheme`, `Typography`, `Shapes`, `MaterialSystem` |
| `ColorScheme` | `Core/base/Theme.kt` | Full color token set (Material3-compatible); predefined: `Material3Light`, `iOS26Light`, `Windows11Light` |
| `GlassAvanue` | `Core/base/GlassAvanue.kt` | Predefined glassmorphic theme (Light/Dark/Auto); context-aware via `forContext(AppContext)` |
| `ThemePlatform` | `Core/base/Theme.kt` | Enum: iOS26_LiquidGlass, macOS26_Tahoe, visionOS2, Windows11_Fluent2, AndroidXR, Material3_Expressive, SamsungOneUI7 |
| `Components.kt` | `Core/base/Components.kt` | 28 DSL component data classes — ALL `render()` methods are `TODO` stubs |
| `ThemeManager` | `Theme/commonMain/.../ThemeManager.kt` | Theme persistence coordinator |
| `ThemeRepository` | `Theme/commonMain/.../ThemeRepository.kt` | Interface + `CloudThemeRepository` (9 no-op impl) + local impl |
| `ThemeIO` | `Theme/commonMain/.../ThemeIO.kt` | Import/export pipeline — all importers/exporters are empty lists |
| `MagicDp/MagicSp/MagicPx/MagicColor` | `CoreTypes/CoreTypes.kt` | Type-safe value classes for dimensions and colors |
| `ComposeRenderer` | `Renderers/Android/` | Dispatches DSL component tree to Compose |
| `AvatarMapper`, `BreadcrumbMapper`, etc. | `Renderers/Android/mappers/` | Per-component Compose implementations |
| `ComposeUIImplementation` | `Adapters/androidMain/` | Alternative adapter: maps DSL `Component` to Compose UI |
| `ThemeBuilderApp` | `ThemeBuilder/jvmMain/Main.kt` | JVM desktop theme editor — currently uses `MaterialTheme{}` root (Rule #3 violation) |
| `VoiceCommandRouter` | `VoiceCommandRouter/commonMain/` | Routes voice intents to component action handlers |
| `MagicVoiceHandlerRegistry` | `AvanueUIVoiceHandlers/commonMain/` | Registry of per-component voice handlers (display, input, navigation, feedback categories) |
| `ParserStubs` | `AvanueLanguageServer/stubs/` | Temporary stubs — all parse methods throw `NotImplementedError` |

---

## HEALTH

**Overall: RED**

### Critical Issues (P0)
- **28 `render()` stubs** in `Core/base/Components.kt` — every DSL component throws `TODO("Platform rendering not yet implemented")` if called
- **14 additional `render()` stubs** in `Data/` sub-module components
- **`ParserStubs.kt`** — all 3 parsers (`VosParser`, `JsonDSLParser`, `CompactSyntaxParser`) return `NotImplementedError`

### High Issues (P1)
- **`MaterialTheme{}` root** in `ThemeBuilder/Main.kt:55` — the AvanueUI authoring tool uses Material3, not AvanueTheme
- **13+ `MaterialTheme.colorScheme.*` violations** in `ThemeBuilder/Main.kt`
- **22+ `MaterialTheme.typography.*` violations** in `LayoutDisplayExtensions.kt`
- **10 `MaterialTheme.typography.*` violations** in `SettingsComponents.kt`
- **7 violations** in `ComposeUIImplementation.kt`
- **7 violations** in `InputExtensions.kt`
- **8 violations** in `AdvancedInputMappers.kt`
- **5 violations** across `BreadcrumbMapper`, `ModalMapper`, `ToastMapper`, `AlertMapper`, `AdvancedFeedbackMappers`
- **`CloudThemeRepository`** — 9 interface methods are silent no-ops (returns null/empty, logs to stdout)
- **`ThemeIO.defaultImporters()` / `defaultExporters()`** — returns empty lists; no import/export works
- **`AssetVersionManager.saveVersionHistory()`** — returns `Result.success(Unit)` without writing anything

### Medium Issues (P2)
- `Data/` sub-module uses banned `com.avanueui.*` package namespace (14 files)
- 8 sub-modules have iOS/JVM/Desktop targets commented out — not truly multiplatform
- Duplicate component types in `Core/display/` and `Data/` (Avatar, Chip, Table, TreeView, Badge, Tooltip)
- `Voice/src/main/java/` uses legacy source set path instead of `androidMain/kotlin/`
- `VoiceComponents.kt` has `Author: VOS4 Development Team` — violates Rule 7
- `Adapters/ComposeUIImplementation.kt` uses legacy `net.ideahq.avamagic` package namespace
- `minSdk` mismatch: `Data` = 26, `Core` = 24
- Zero AVID semantics on any Compose component

### Positive Notes
- `GlassAvanue` theme object is well-structured with context-aware adaptation (`forContext`, `adaptToAmbientLight`, `withAccent`)
- `CoreTypes` value classes are clean, zero-cost, and well-documented
- `MagicVoiceHandlerRegistry` and handler structure in `AvanueUIVoiceHandlers/` is a good architectural pattern
- `VoiceCommandRouter` has tests (`VoiceCommandRouterTest.kt`) — only tested sub-module found
- `ProgressBarComponent` has a runtime `init {}` validation (`require(value in 0.0f..1.0f)`) — a good practice among the stubs
- `AvatarMapper` mixes `AvanueTheme.colors.*` (correct) and `MaterialTheme.typography.*` (incorrect) — shows the path to correct usage

### Full Report
`/Volumes/M-Drive/Coding/NewAvanues/docs/reviews/AvanueUI-Review-QualityAnalysis-260222-V1.md`
