# Eliminate AvaMagic — Promote All Modules to Top-Level

> **Branch**: `060226-1-consolidation-framework`
> **Save to**: `docs/plans/Consolidation/Consolidation-Plan-AvaMagicElimination-260207-V1.md`

## Context

`Modules/AvaMagic/` is a branding-only parent directory containing 26 active Gradle modules. It adds unnecessary nesting and the "AvaMagic" name is no longer applicable. All sub-modules should be promoted to top-level `Modules/` entries.

The biggest move is AVAUI (22 sub-modules, 315 .kt files) → `Modules/AvanueUI/`. AVAUI has **zero coupling** to other AvaMagic siblings and **zero external consumers**. The current `Modules/AvanueUI` (9 files, theme/tokens) overlaps with AVAUI's DesignSystem and Foundation — **AVAUI files take priority** as the more thorough implementation.

---

## Phase 1: Merge AVAUI into Modules/AvanueUI (AVAUI is primary)

**Priority**: HIGHEST | **22 sub-modules, 315 .kt files**

### Step 1.1: Back up current AvanueUI root files

Current `Modules/AvanueUI/src/` has 9 files with Ocean-specific and Voice UI-specific content that AVAUI lacks. These need to be preserved and merged into appropriate AVAUI sub-modules.

**Files to preserve** (unique content not in AVAUI):
- `AvanueTheme.kt` — AvanueTheme composable name (AVAUI calls it MagicTheme), `LocalBreakpoints`, `BreakpointTokens`
- `DesignTokens.kt` — Ocean glass colors (GlassUltraLight→GlassDense), Voice UI sizes (CommandBarHeight, VoiceButtonSize, ChatBubbleMaxWidth), SpatialSizeTokens, BreakpointTokens
- `OceanTheme.kt` — Centralized Ocean color object (unique to AvanueUI)
- `OceanThemeExtensions.kt` — Modifier extensions
- `GlassmorphicComponents.kt` — `GlassIndicator()`, `OceanGradientBackground()` (unique components)
- Platform actuals: `AvanueTheme.android.kt`, `AvanueTheme.desktop.kt`, `AvanueTheme.ios.kt`

### Step 1.2: Delete current AvanueUI source dirs

Remove `Modules/AvanueUI/src/` entirely. Keep `build.gradle.kts` (will be rewritten).

### Step 1.3: Move AVAUI sub-modules into Modules/AvanueUI/

```
mv Modules/AvaMagic/AVAUI/Core          → Modules/AvanueUI/Core
mv Modules/AvaMagic/AVAUI/CoreTypes     → Modules/AvanueUI/CoreTypes
mv Modules/AvaMagic/AVAUI/Foundation    → Modules/AvanueUI/Foundation
mv Modules/AvaMagic/AVAUI/Theme         → Modules/AvanueUI/Theme
mv Modules/AvaMagic/AVAUI/ThemeBridge   → Modules/AvanueUI/ThemeBridge
mv Modules/AvaMagic/AVAUI/DesignSystem  → Modules/AvanueUI/DesignSystem
mv Modules/AvaMagic/AVAUI/StateManagement → Modules/AvanueUI/StateManagement
mv Modules/AvaMagic/AVAUI/UIConvertor   → Modules/AvanueUI/UIConvertor
mv Modules/AvaMagic/AVAUI/Input         → Modules/AvanueUI/Input
mv Modules/AvaMagic/AVAUI/Display       → Modules/AvanueUI/Display
mv Modules/AvaMagic/AVAUI/Feedback      → Modules/AvanueUI/Feedback
mv Modules/AvaMagic/AVAUI/Layout        → Modules/AvanueUI/Layout
mv Modules/AvaMagic/AVAUI/Navigation    → Modules/AvanueUI/Navigation
mv Modules/AvaMagic/AVAUI/Floating      → Modules/AvanueUI/Floating
mv Modules/AvaMagic/AVAUI/Data          → Modules/AvanueUI/Data
mv Modules/AvaMagic/AVAUI/Voice         → Modules/AvanueUI/Voice
mv Modules/AvaMagic/AVAUI/Adapters      → Modules/AvanueUI/Adapters
mv Modules/AvaMagic/AVAUI/TemplateLibrary → Modules/AvanueUI/TemplateLibrary
mv Modules/AvaMagic/AVAUI/VoiceCommandRouter → Modules/AvanueUI/VoiceCommandRouter
mv Modules/AvaMagic/AVAUI/ARGScanner    → Modules/AvanueUI/ARGScanner
mv Modules/AvaMagic/AVAUI/AssetManager  → Modules/AvanueUI/AssetManager
mv Modules/AvaMagic/AVAUI/Renderers     → Modules/AvanueUI/Renderers
mv Modules/AvaMagic/AVAUI/XR            → Modules/AvanueUI/XR
mv Modules/AvaMagic/AVAUI/ThemeBuilder  → Modules/AvanueUI/ThemeBuilder
```

### Step 1.4: Merge unique AvanueUI content into AVAUI sub-modules

**Into `AvanueUI/DesignSystem/`:**
- Merge Ocean glass colors, Voice UI sizes, SpatialSizeTokens, BreakpointTokens into DesignTokens.kt
- Rename `MagicTheme` composable → `AvanueTheme` (consistency with project name)
- Rename `MagicThemeExtensions` → `AvanueTheme` companion/object
- Add `LocalBreakpoints` CompositionLocal
- Move `OceanTheme.kt` here (centralized color definitions)
- Merge platform actuals (dynamic color support)

**Into `AvanueUI/Foundation/`:**
- Merge `GlassIndicator()` and `OceanGradientBackground()` into existing GlassmorphicComponents.kt
- Merge `OceanThemeExtensions.kt` modifier extensions into existing OceanThemeExtensions.kt

### Step 1.5: Rewrite Modules/AvanueUI/build.gradle.kts

The root `Modules/AvanueUI/build.gradle.kts` becomes a thin aggregator or is removed (sub-modules are independent). If consumers need a single dependency, keep it as an umbrella module that re-exports sub-modules via `api()`.

### Step 1.6: Update settings.gradle.kts

Replace all 22 includes:
```
:Modules:AvaMagic:AvaUI:Core       → :Modules:AvanueUI:Core
:Modules:AvaMagic:AvaUI:CoreTypes  → :Modules:AvanueUI:CoreTypes
... (all 22)
```

### Step 1.7: Update internal build.gradle.kts project references

~35 references across ~25 build files:
```
project(":Modules:AvaMagic:AvaUI:Core") → project(":Modules:AvanueUI:Core")
```

### Step 1.8: Update namespace strings in build.gradle.kts files

~15 build files: `com.augmentalis.avamagic.avaui.*` → `com.avanueui.*`

### Step 1.9: Update Kotlin package declarations

14 .kt files with `package com.augmentalis.avamagic.avaui.*` → `com.avanueui.*`
Move source directories accordingly: `com/augmentalis/avamagic/avaui/` → `com/avanueui/`

Note: ~301 files use `com.augmentalis.avamagic.*` packages (without `avaui`) — these stay as-is for now (package rename is separate from module move).

### Step 1.10: Update consumer imports

Consumers that currently import `com.avanueui.*` (36 files from prior consolidation):
- If AVAUI packages replace them, update to new package paths
- WebAvanue (22 files), AI/Chat (2 files), apps/avanues (1 file)

### Verification
```bash
./gradlew :Modules:AvanueUI:Core:compileDebugKotlinAndroid
./gradlew :Modules:AvanueUI:DesignSystem:compileDebugKotlinAndroid
./gradlew :Modules:AvanueUI:Foundation:compileDebugKotlinAndroid
./gradlew :apps:avanues:assembleDebug
```

---

## Phase 2: Move AVACode to Top-Level

**Priority**: HIGH | **47 .kt files**

### Step 2.1: Move directory

```
mv Modules/AvaMagic/AVACode → Modules/AVACode
```

### Step 2.2: Update settings.gradle.kts

```
:Modules:AvaMagic:AVACode → :Modules:AVACode
```

### Step 2.3: Update build.gradle.kts dependency

AVACode depends on `project(":Modules:AvaMagic:AvaUI:Core")` → `project(":Modules:AvanueUI:Core")`

### Step 2.4: Update namespace

`com.augmentalis.avamagic.avacode` — leave as-is for now (package rename is optional, separate concern).

### Verification
```bash
./gradlew :Modules:AVACode:compileDebugKotlinAndroid
```

---

## Phase 3: Move Remaining AvaMagic Modules to Top-Level

**Priority**: HIGH | **4 active modules**

### Step 3.1: Move IPC

```
mv Modules/AvaMagic/IPC → Modules/IPC
```
- Update settings: `:Modules:AvaMagic:IPC` → `:Modules:IPC`
- Dependencies: `project(":Modules:AVU")` + `project(":Modules:DeviceManager")` — already top-level, no change needed

### Step 3.2: Move AVURuntime

```
mv Modules/AvaMagic/AVURuntime → Modules/AVURuntime
```
- Update settings: `:Modules:AvaMagic:AVURuntime` → `:Modules:AVURuntime`
- Update dep: `project(":Modules:AvaMagic:AvaUI:Core")` → `project(":Modules:AvanueUI:Core")`
- Note: This is an empty stub module (0 .kt files). Consider deleting if unused.

### Step 3.3: Move MagicVoiceHandlers

```
mv Modules/AvaMagic/MagicVoiceHandlers → Modules/MagicVoiceHandlers
```
- Update settings: `:Modules:AvaMagic:MagicVoiceHandlers` → `:Modules:MagicVoiceHandlers`

### Step 3.4: Move MagicTools/LanguageServer

```
mv Modules/AvaMagic/MagicTools/LanguageServer → Modules/LanguageServer
```
- Update settings: `:Modules:AvaMagic:MagicTools:LanguageServer` → `:Modules:LanguageServer`
- Flatten from nested MagicTools/ to top-level

### Verification
```bash
./gradlew :Modules:IPC:compileDebugKotlinAndroid
./gradlew :Modules:AVURuntime:compileDebugKotlinAndroid
./gradlew :Modules:MagicVoiceHandlers:compileDebugKotlinAndroid
./gradlew :Modules:LanguageServer:compileDebugKotlinAndroid
```

---

## Phase 4: Delete AvaMagic Directory

### Step 4.1: Verify empty

After all moves, check that only inactive/archived modules remain in `Modules/AvaMagic/`.

Inactive modules (NOT in settings.gradle.kts):
- Plugins, Preferences, Data, Logging, Observability, PluginRecovery, VoiceIntegration, LearnAppCore
- AVAUI/Renderers/iOS, Renderers/Desktop (not in settings)
- AVAUI/ThemeBuilder, XR (not in settings)

### Step 4.2: Archive or delete

Move any remaining content to `Archives/AvaMagic/` or delete outright.

### Step 4.3: Remove AvaMagic references

- Remove any remaining AvaMagic comments from settings.gradle.kts
- Update any docs referencing AvaMagic paths

### Final Verification
```bash
./gradlew :apps:avanues:assembleDebug
```

---

## Scope Summary

| Phase | Description | Dirs Moved | Settings Lines | Build Refs | Risk |
|-------|------------|-----------|---------------|-----------|------|
| 1 | AVAUI → AvanueUI | 24 | 22 | ~35 | MEDIUM (overlap merge) |
| 2 | AVACode → top-level | 1 | 1 | 1 | LOW |
| 3 | IPC, AVURuntime, etc. | 4 | 4 | ~3 | LOW |
| 4 | Delete AvaMagic | 1 | cleanup | cleanup | LOW |
| **Total** | | **30** | **27** | **~39** | |

## Key Decisions

- **AVAUI files are primary** where overlap exists with current AvanueUI
- **AvanueTheme naming** is kept (MagicTheme renamed to AvanueTheme)
- **Package `com.avanueui`** for the theme/designsystem layer
- **Package `com.augmentalis.avamagic.*`** left as-is in moved modules (bulk package rename is a separate future task)
- **Inactive AvaMagic modules** archived, not moved to top-level
- **No external consumers** of AvaMagic exist — migration is fully internal
