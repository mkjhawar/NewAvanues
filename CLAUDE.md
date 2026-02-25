# NewAvanues Repository Rules

## MANDATORY RULE #1: SCRAPING SYSTEM PROTECTION (ZERO TOLERANCE)

**DO NOT remove, delete, replace, disable, comment out, refactor away, or otherwise eliminate ANY part of the working scraping system without FIRST:**

1. **Asking the user for explicit permission** via AskUserQuestion
2. **Stating the COMPLETE reason** why you believe the change is necessary
3. **Describing EXACTLY what you plan to do** - which files, functions, tables, or components will be affected
4. **Waiting for user approval** before making ANY changes

### What "scraping system" means (protected scope):

- **Android scraping**: `Modules/VoiceOSCore/` - `VoiceOSAccessibilityService`, `AndroidScreenExtractor`, and all accessibility-based UI traversal code
- **Web scraping**: `Modules/WebAvanue/` - `DOMScraperBridge` and all JavaScript DOM traversal/injection code
- **Database schema**: `Modules/Database/` - `ScrapedApp.sq`, `ScrapedElement.sq`, `ScrapedHierarchy.sq`, `ScrapedWebElement.sq`, `ScreenContext.sq`, `GeneratedCommand.sq`, `ScreenTransition.sq`, `UserInteraction.sq` and their repository interfaces/implementations
- **RPC layer**: `VoiceOSService.kt` scrapeScreen methods and related DTOs
- **Command generation**: Any code that generates voice commands from scraped elements
- **Supporting infrastructure**: Repositories, data models, element hashing, deduplication logic, form grouping, screen context tracking

### This rule applies to ALL of the following actions:

- Deleting files or functions
- Removing database tables or columns
- Replacing the implementation with a "new" or "better" version
- Refactoring that changes the scraping behavior or data flow
- Migrating to a different scraping approach
- Commenting out or disabling scraping functionality
- Removing dependencies that the scraping system relies on

### Why this rule exists:

The scraping system is a complex, production-grade, dual-platform (Android + Web) UI element discovery and voice command generation pipeline. It has been built, tested, and refined over many sessions. Accidental removal or replacement causes catastrophic loss of working functionality that takes significant effort to rebuild.

**Violation of this rule is a session-ending error. No exceptions. No "I'll add it back later." No stubs.**

---

## MANDATORY RULE #2: FILE PLACEMENT (ZERO TOLERANCE)

**Every new file MUST be placed in the correct location. NEVER create files in ad-hoc or incorrect paths. If unsure, ASK the user before creating the file.**

### Modules: `Modules/ModuleName/`

Shared/reusable library code lives in modules. These follow KMP source set conventions:

```
Modules/
  ModuleName/
    src/
      commonMain/kotlin/...    # Cross-platform shared code
      androidMain/kotlin/...   # Android-specific implementations
      iosMain/kotlin/...       # iOS-specific implementations
      desktopMain/kotlin/...   # Desktop-specific implementations
      jvmMain/kotlin/...       # JVM-specific implementations
```

- Platform-specific code goes in the correct source set (`androidMain`, `iosMain`, etc.), NOT in a separate folder
- NEVER create platform folders outside the KMP `src/` structure for module code
- If a module doesn't exist yet, ask the user before creating a new `Modules/NewName/` directory

### Apps: `Apps/Platform/AppName/`

All applications (runnable targets with UI) live under `Apps/`, organized by platform first, then app name:

```
Apps/
  Android/
    VoiceOS/
    VoiceCursor/
    WebAvanue/
    ...
  iOS/
    ...
  Web/
    ...
  Desktop/
    ...
```

**Correct placement rules:**
- Android apps go in `Apps/Android/AppName/`
- iOS apps go in `Apps/iOS/AppName/`
- Web apps go in `Apps/Web/AppName/`
- Desktop apps go in `Apps/Desktop/AppName/`
- KMP/multiplatform apps go in `Apps/KMP/AppName/` (if the app target is multiplatform itself)

**NEVER place apps in:**
- `android/Apps/` (legacy location - DO NOT ADD new apps here)
- `Apps/` root without a platform subfolder
- `Modules/` (modules are libraries, not apps)
- Any other invented path

### How to decide: Module or App?

| It is a... | Place it in... |
|------------|---------------|
| Reusable library/SDK with no UI entry point | `Modules/ModuleName/` |
| Runnable application with a UI/launcher | `Apps/Platform/AppName/` |
| Platform-specific implementation of a module | `Modules/ModuleName/src/{platform}Main/` |

### Why this rule exists:

The repo has accumulated apps in inconsistent locations (`/Apps/`, `/android/Apps/`, mixed paths) causing confusion about where things live. This rule establishes a single, unambiguous convention going forward.

**If you are about to create a file and are not 100% certain it's in the right place, STOP and ask the user.**

---

## MANDATORY RULE #3: THEME SYSTEM v5.1 (ZERO TOLERANCE)

**ALL UI code MUST use the decoupled AvanueUI Theme v5.1 system. No exceptions.**

### Three Independent Axes

| Axis | Enum | Values | Default |
|------|------|--------|---------|
| Color Palette | `AvanueColorPalette` | SOL, LUNA, TERRA, HYDRA | HYDRA |
| Material Style | `MaterialMode` | Glass, Water, Cupertino, MountainView | Water |
| Appearance | `AppearanceMode` | Light, Dark, Auto | Auto |

32 valid combinations (any palette x any style x light/dark). Auto follows system preference.

### MANDATORY Rules

1. **Use `AvanueColorPalette` + `MaterialMode` + `AppearanceMode`** — NEVER use the deprecated `AvanueThemeVariant`
2. **Use `AvanueTheme.colors.*`** — NEVER use `MaterialTheme.colorScheme.*`
3. **Use unified components** (`AvanueCard`, `AvanueButton`, etc.) — they auto-adapt to `MaterialMode`
4. **Use `palette.colors(isDark)` / `palette.glass(isDark)` / `palette.water(isDark)`** for appearance-aware colors
5. **Use `AvanueTheme.isDark`** when components need appearance-aware logic
6. **Cupertino style**: 0dp elevation, 12dp corners, 0.33dp hairline borders, no glass/water effects
7. **MountainView style**: Standard M3 tonal elevation, M3 shape scale, no glass/water effects

### BANNED (DEPRECATED)

- `AvanueThemeVariant` — use `AvanueColorPalette` + `MaterialMode` independently
- `MaterialMode.PLAIN` — renamed to `MaterialMode.MountainView`
- `OceanColors` / `SunsetColors` / `LiquidColors` — use `LunaColors` / `SolColors` / `HydraColors`
- `OceanGlass` / `SunsetGlass` / `LiquidGlass` — use `LunaGlass` / `SolGlass` / `HydraGlass`
- `OceanWater` / `SunsetWater` / `LiquidWater` — use `LunaWater` / `SolWater` / `HydraWater`

### DataStore Keys

- `theme_palette` — stores `AvanueColorPalette` name (SOL/LUNA/TERRA/HYDRA)
- `theme_style` — stores `MaterialMode` name (Glass/Water/Cupertino/MountainView)
- `theme_appearance` — stores `AppearanceMode` name (Light/Dark/Auto)
- Old `theme_variant` key auto-migrates to new keys

### When Creating UI

1. Use `AvanueThemeProvider(colors = palette.colors(isDark), glass = palette.glass(isDark), water = palette.water(isDark), materialMode = style, isDark = isDark)`
2. Read colors: `AvanueTheme.colors.primary`, `AvanueTheme.colors.surface`, etc.
3. Read appearance: `AvanueTheme.isDark` for appearance-aware logic
4. Use unified components (`AvanueCard`, `AvanueSurface`, `AvanueButton`, etc.) — they handle all 4 material modes
5. Default palette: HYDRA (royal sapphire). Default style: Water. Default appearance: Auto

---

## MANDATORY RULE #4: FLAT PACKAGE STRUCTURE (NEW FILES ONLY)

**All NEW files MUST use a flat package structure. Do NOT create nested subdirectories when the folder name is redundant with its contents.**

### The Rule

When creating a new Kotlin file, the package path after the module's base package should be **at most 2 levels deep**. Do not nest further unless the subdirectory adds genuine semantic meaning that isn't already in the class name or sibling folder.

### Redundancy Examples (BAD → GOOD)

| BAD (redundant nesting) | GOOD (flat) | Why |
|---|---|---|
| `commandmanager/database/sqldelight/VoiceCommandDaoAdapter.kt` | `commandmanager/sqldelight/VoiceCommandDaoAdapter.kt` | `database` is redundant — `sqldelight` already implies DB |
| `voiceavanue/service/VoiceAvanueAccessibilityService.kt` | `voiceavanue/VoiceAvanueAccessibilityService.kt` | `service/` is redundant — the class name says "Service" |
| `commandmanager/loader/CommandLoader.kt` | `commandmanager/CommandLoader.kt` | `loader/` is redundant — the class name says "Loader" |
| `commandmanager/registry/CommandRegistry.kt` | `commandmanager/CommandRegistry.kt` | `registry/` is redundant — the class name says "Registry" |

### When Nesting IS Acceptable

| Pattern | Acceptable? | Why |
|---|---|---|
| `handlers/MediaHandler.kt` | YES | Groups 12+ handlers by concern — genuine category |
| `ui/editor/CommandEditorViewModel.kt` | YES | `ui/` separates UI from logic, `editor/` groups related screens |
| `speech/SpeechMode.kt` | YES | `speech/` is a distinct domain, not redundant with class name |
| `vos/VosFileImporter.kt` | YES | `vos/` groups VOS-specific operations (importer, exporter, sync) |

### Deciding: Nest or Not?

Ask: **"If I removed this folder, would the file's purpose be ambiguous?"**
- YES → keep the folder (it adds semantic value)
- NO → remove it (the class name already tells you what it is)

### Scope

- **New files only** — do NOT retroactively move existing files (breaks imports, Hilt DI, KSP codegen, git history)
- **Flatten opportunistically** — when refactoring an area (e.g., removing a deprecated class), flatten its neighbors if convenient
- **Never flatten across module boundaries** — KMP source sets (`commonMain`, `androidMain`, etc.) structure is sacred

### Why this rule exists:

The repo has accumulated deeply nested packages where folder names repeat information already in the class name (`database/sqldelight/`, `service/SomeService.kt`, `loader/SomeLoader.kt`). This makes paths longer without adding clarity. A flatter structure is easier to navigate and follows KMP conventions where packages reflect domain boundaries, not implementation roles.
