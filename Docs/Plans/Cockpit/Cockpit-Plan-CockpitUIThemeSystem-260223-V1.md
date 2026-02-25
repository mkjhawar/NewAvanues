# Cockpit UI + Theme System Plan
**Module:** Cockpit | **Date:** 260223 | **Version:** V1
**Branch:** VoiceOS-1M-SpeechEngine
**Mode:** .tcr .swarm .auto (CoT+ToT, parallel agents, auto-approve)

---

## 1. Executive Summary

This plan creates TWO Cockpit UIs and extends the theme system:

| Deliverable | Platform | Location | Status |
|-------------|----------|----------|--------|
| **Browser Cockpit** | Web (HTML/CSS/JS) | `Demo/cockpit-browser/` | NEW — rewrite of Task_Cockpit |
| **KMP Cockpit** | Android/Desktop | `Modules/Cockpit/` | ENHANCE existing |
| **Theme Presets** | KMP (commonMain) | `Modules/AvanueUI/` | EXTEND v5.1 |
| **Developer Settings** | Android | `apps/avanues/` | ADD theme customization |

---

## 2. Task_Cockpit Analysis

### 2.1 Architecture Assessment

The legacy Task_Cockpit at `/Users/manoj_mbpm14/Downloads/aijunk/Cockpit/Task_Cockpit` is an **Android-only View-based** implementation:

| Component | Tech | Assessment |
|-----------|------|------------|
| `CockpitActivity` | AppCompatActivity + NavHostFragment | Clean entry point, 14 TaskMode variants |
| `TaskFragment` | Fragment + ViewBinding + ViewPager2 | **1,386 lines** — monolith mixing UI, commands, auth, scanning |
| `FrameFragment` | Fragment per content type | Good concept, but tightly coupled to XML |
| `TaskViewModel` | AndroidX ViewModel + StateFlow | Solid state management |
| `ViewMode` enum | 7 layout modes | Maps to our 14 LayoutModes |
| Theme | `AugmentalisTheme` (legacy) | Pre-AvanueUI, custom JSON-based |
| Voice | Direct accessibility bridge | Now superseded by VoiceOSCore handlers |
| Persistence | Room/SQLite | Now superseded by SQLDelight |

### 2.2 Feature Mapping (Task_Cockpit → Current Cockpit)

| Task_Cockpit Feature | Current Cockpit Equivalent | Gap |
|---|---|---|
| ViewPager cockpit | LayoutMode.CAROUSEL | DONE |
| Row layout | LayoutMode.ROW | DONE |
| Grid layout | LayoutMode.GRID | DONE |
| Multi-left/right | LayoutMode.SPLIT_LEFT/RIGHT | DONE |
| Fullscreen | LayoutMode.FULLSCREEN | DONE |
| Workflow mode | LayoutMode.WORKFLOW | DONE |
| Gallery filter | LayoutMode.GALLERY | DONE |
| Frame prev/next navigation | Voice commands (frame N) | DONE |
| Hide/Minimize/Close frame | FrameWindow traffic lights | DONE |
| Web auth (basic auth dialog) | Not in Cockpit | **GAP: Add to WebContent** |
| Barcode scanning to web | Not in Cockpit | **GAP: Low priority** |
| Custom theme JSON | AvanueUI v5.1 (32 combos) | SUPERSEDED |
| Note editing | FrameContent.Note | Partial (placeholder UI) |
| PDF page navigation | FrameContent.Pdf | Partial (placeholder UI) |
| Workflow step reorder/edit | WorkflowSidebar | **GAP: Add CRUD ops** |

### 2.3 What to Salvage

1. **TaskMode concept** — Multi-entry-point pattern (open list, open task by name, create from media URI). Map to Cockpit deep links.
2. **Workflow step CRUD** — Edit name/description, reorder up/down, delete. Port logic to CockpitViewModel.
3. **Frame layout calculation** — The grid layout `columnCount`/`rowSpec` math is identical to our GridLayout. Confirmed: no regression.
4. **Web auth flow** — BasicAuth dialog + barcode scanner for credentials. Add as optional feature in ContentRenderer.

**Verdict:** Task_Cockpit's core features are **already implemented** in the KMP Cockpit. The gaps (web auth, workflow CRUD, note/PDF content actions) are incremental additions, not architectural changes. The browser-based cockpit should be a **clean rewrite** based on v4.5 demo + current KMP architecture, NOT a port of the messy legacy code.

---

## 3. Browser-Based Cockpit (Clean Rewrite)

### 3.1 Architecture

```
Demo/cockpit-browser/
├── index.html              # Entry point
├── css/
│   ├── themes.css          # Theme tokens as CSS custom properties
│   ├── layouts.css         # 14 layout mode styles
│   ├── components.css      # Frame, CommandBar, Dashboard
│   └── glass.css           # Glass/Water/Spatial effects
├── js/
│   ├── app.js              # Main app controller
│   ├── cockpit-vm.js       # ViewModel (mirrors CockpitViewModel.kt)
│   ├── layout-engine.js    # Layout mode renderer
│   ├── frame-manager.js    # Frame CRUD + content types
│   ├── command-bar.js      # Voice command bar
│   ├── theme-engine.js     # Theme switching (4 palettes × 4 modes × 2 appearances)
│   ├── persistence.js      # IndexedDB for sessions/frames
│   └── spatial.js          # PseudoSpatial parallax + gyroscope
└── assets/
    └── icons/              # Module icons (SVG)
```

### 3.2 Feature Parity Matrix

| Feature | KMP Cockpit | Browser Cockpit | Priority |
|---------|-------------|-----------------|----------|
| 14 layout modes | YES | YES | P0 |
| Dashboard launcher | YES | YES | P0 |
| Frame CRUD | YES | YES | P0 |
| 17 content types | YES | Web/PDF/Image/Video/Note only | P0 |
| Traffic light chrome | YES | YES | P0 |
| Command bar | YES | YES | P0 |
| Theme switching (32 combos) | YES | YES | P0 |
| PseudoSpatial parallax | YES | YES (DeviceOrientation API) | P1 |
| Glass/Water effects | CSS backdrop-filter | CSS backdrop-filter | P1 |
| Session persistence | SQLDelight | IndexedDB | P1 |
| Templates | YES | YES | P1 |
| Auto-save | YES (500ms debounce) | YES | P1 |
| Triptych layout | Demo v4.5 only | YES (port from v4.5) | P1 |
| See-through backgrounds | Demo v4.5 only | YES | P1 |
| Device presets | YES (LayoutModeResolver) | YES (responsive) | P1 |
| Voice commands | VoiceOSCore | Web Speech API (stretch) | P2 |
| Spatial head tracking | SpatialViewportController | Not feasible in browser | Skip |

### 3.3 Theme CSS Custom Properties

```css
:root[data-palette="hydra"][data-appearance="dark"] {
  --av-primary: #4A6CF7;
  --av-on-primary: #FFFFFF;
  --av-secondary: #7C4AF7;
  --av-surface: #1A1B2E;
  --av-background: #0D0E1A;
  --av-text-primary: #E8EAED;
  --av-text-secondary: #9AA0A6;
  --av-border: rgba(255,255,255,0.12);
  --av-error: #CF6679;
  --av-success: #4CAF50;
  --av-warning: #FFB74D;
  /* Glass tokens */
  --av-glass-blur: 20px;
  --av-glass-opacity: 0.15;
  --av-glass-border: rgba(255,255,255,0.08);
  /* Water tokens */
  --av-water-opacity: 0.6;
  --av-water-refraction: 2px;
  /* Shape tokens */
  --av-radius-sm: 4px;
  --av-radius-md: 8px;
  --av-radius-lg: 12px;
  --av-radius-xl: 16px;
}
```

All 32 combinations defined as `[data-palette][data-appearance]` selectors. MaterialMode controls which effect layer is applied (glass blur, water caustics, cupertino flat, mountainview elevation).

### 3.4 Implementation Phases

| Phase | Scope | Est. Lines | Priority |
|-------|-------|-----------|----------|
| B1 | Core shell: Dashboard + FrameWindow + CommandBar + 4 basic layouts (Grid, Fullscreen, Split, Row) | ~1,200 | P0 |
| B2 | All 14 layout modes + theme engine (32 combos) | ~800 | P0 |
| B3 | Content types (Web iframe, PDF viewer, Image zoom, Video player, Note editor) | ~600 | P0 |
| B4 | PseudoSpatial parallax + Glass/Water CSS effects + Triptych | ~500 | P1 |
| B5 | IndexedDB persistence + session templates + auto-save | ~400 | P1 |
| B6 | Device preset simulator (phone/tablet/glass dropdown) + responsive breakpoints | ~300 | P1 |

---

## 4. KMP Cockpit Enhancements

### 4.1 Current State (54 files, well-architected)

The existing KMP Cockpit module is mature:
- **commonMain (36 files):** ViewModel, 14 layout modes, FrameWindow, GlassFrameChrome, PseudoSpatialCanvas, DashboardLayout, CommandBar, models
- **androidMain (6 files):** CockpitScreen, ContentRenderer, AndroidSpatialOrientationSource, AndroidCockpitRepository, AndroidExternalAppResolver, AiSummaryPanel
- **desktopMain (3 files):** DesktopSpatialOrientationSource, DesktopCockpitRepository, DesktopExternalAppResolver
- **commonTest (3 files):** FrameContentTest, CockpitSessionTest, LayoutModeTest

### 4.2 Enhancement Backlog

| ID | Enhancement | Files | Priority |
|----|-------------|-------|----------|
| K1 | **Workflow CRUD:** Edit step name/description, reorder, delete from CockpitViewModel | CockpitViewModel.kt, WorkflowSidebar.kt | P0 |
| K2 | **Content actions:** Wire PDF prev/next page, web back/forward, image zoom to frame content callbacks | CommandBar.kt, ContentRenderer.kt | P0 |
| K3 | **Theme preset picker:** Add MaterialMode selector in Cockpit settings panel | NEW: CockpitSettingsPanel.kt | P1 |
| K4 | **Centering fix:** Audit all 14 layout modes for proper centering (reported issue) | LayoutEngine.kt, all layout composables | P0 |
| K5 | **Triptych layout mode:** Port v4.5 triptych design as LayoutMode.TRIPTYCH | NEW: TriptychLayout.kt, LayoutMode.kt | P1 |
| K6 | **Background scenes:** See-through/scene background toggle (from v4.5 BG feature) | CockpitScreenContent.kt | P1 |
| K7 | **Deep links:** LaunchMode entry points matching Task_Cockpit's TaskMode | CockpitScreen.kt | P2 |
| K8 | **Web BasicAuth:** Auth dialog in WebView content renderer | ContentRenderer.kt | P2 |

### 4.3 Centering Audit (K4 — P0)

All layouts must center content properly. Known issues:
- **FlightDeckLayout:** Bottom row not centered when frame count < column count
- **GridLayout:** Single frame not centered vertically
- **DashboardLayout:** Module tiles not centering in 3-column grid when count isn't multiple of 3
- **MosaicLayout:** Primary frame vertical alignment off-center on tablet

Fix pattern: Use `Arrangement.Center` + `Alignment.CenterHorizontally/CenterVertically` where missing. Audit each layout composable systematically.

---

## 5. Theme System Extension

### 5.1 Current v5.1 Architecture (3 Axes × 32 Combos)

```
AvanueColorPalette: SOL | LUNA | TERRA | HYDRA
  × MaterialMode: Glass | Water | Cupertino | MountainView
  × AppearanceMode: Light | Dark | Auto
= 32 visual combinations
```

### 5.2 Premade Theme Presets

Theme presets are **curated combinations** of the 3 axes + additional style overrides. They don't add new axes — they're opinionated selections within the existing system + custom tokens.

| Preset Name | Palette | MaterialMode | Appearance | Additional Overrides |
|-------------|---------|-------------|------------|---------------------|
| **Cupertino** | Any | Cupertino | Auto | 0dp elevation, 12dp corners, 0.33dp hairline borders, SF-style spacing |
| **MountainView** | Any | MountainView | Auto | Standard M3 tonal elevation, M3 shape scale |
| **MountainViewXR** | Any | MountainView | Dark | Boosted luminance, orbiting panels, 3D depth tokens |
| **MetaFacial** | LUNA | Glass | Dark | Curved panel radius, hand-gesture zones, 160px min touch targets |
| **Neumorphic** | Any | *NEW: Neumorphic* | Auto | Dual shadows, surface=background, concave/raised states |
| **VisionOS** | HYDRA | Glass | Dark | 24dp glass blur, specular highlights, ambient tint, layered depth |
| **LiquidUI** | HYDRA | Water | Auto | Blob shapes, morphing animations, organic corners (16-32dp) |

### 5.3 Theme Preset Data Model

```kotlin
// In Modules/AvanueUI/src/commonMain/
data class ThemePreset(
    val id: String,                    // "cupertino", "mountainview", etc.
    val displayName: String,           // "Cupertino"
    val description: String,           // "Apple-inspired clean design"
    val palette: AvanueColorPalette?,  // null = user's choice
    val materialMode: MaterialMode,
    val appearance: AppearanceMode?,   // null = user's choice (Auto)
    val overrides: ThemeOverrides = ThemeOverrides()
)

data class ThemeOverrides(
    // Shape
    val cornerRadiusSm: Dp? = null,
    val cornerRadiusMd: Dp? = null,
    val cornerRadiusLg: Dp? = null,
    // Elevation
    val defaultElevation: Dp? = null,
    val cardElevation: Dp? = null,
    // Border
    val borderWidth: Dp? = null,
    val borderOpacity: Float? = null,
    // Shadow (for neumorphic)
    val dualShadow: Boolean = false,
    val lightShadowOffset: Dp? = null,
    val darkShadowOffset: Dp? = null,
    val lightShadowBlur: Dp? = null,
    val darkShadowBlur: Dp? = null,
    // Glass (for visionOS)
    val glassBlur: Dp? = null,
    val specularHighlight: Boolean = false,
    val ambientTint: Boolean = false,
    // Animation
    val springDamping: Float? = null,
    val transitionDuration: Int? = null, // ms
    // Layout
    val minTouchTarget: Dp? = null,
    val panelCurvature: Float? = null,  // 0f = flat, 1f = full curve
    // Typography scale factor
    val typographyScale: Float? = null,
)
```

### 5.4 Preset Registry

```kotlin
object ThemePresetRegistry {
    val CUPERTINO = ThemePreset(
        id = "cupertino",
        displayName = "Cupertino",
        description = "Apple-inspired clean design",
        palette = null, // user's choice
        materialMode = MaterialMode.Cupertino,
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 12.dp,
            defaultElevation = 0.dp,
            borderWidth = 0.33.dp,
            borderOpacity = 0.2f,
            springDamping = 0.85f,
            transitionDuration = 350
        )
    )

    val MOUNTAIN_VIEW = ThemePreset(
        id = "mountainview",
        displayName = "MountainView",
        description = "Google Material 3 Extended",
        palette = null,
        materialMode = MaterialMode.MountainView,
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 12.dp,
            cardElevation = 2.dp,
            transitionDuration = 300
        )
    )

    val MOUNTAIN_VIEW_XR = ThemePreset(
        id = "mountainview_xr",
        displayName = "MountainView XR",
        description = "Material Design for Spatial Computing",
        palette = null,
        materialMode = MaterialMode.MountainView,
        appearance = AppearanceMode.Dark,
        overrides = ThemeOverrides(
            cornerRadiusMd = 16.dp,
            cardElevation = 8.dp,
            panelCurvature = 0.3f,
            typographyScale = 1.1f,
            minTouchTarget = 80.dp,
            transitionDuration = 400
        )
    )

    val META_FACIAL = ThemePreset(
        id = "meta_facial",
        displayName = "MetaFacial",
        description = "Meta Horizon OS-inspired",
        palette = AvanueColorPalette.LUNA,
        materialMode = MaterialMode.Glass,
        appearance = AppearanceMode.Dark,
        overrides = ThemeOverrides(
            cornerRadiusLg = 24.dp,
            panelCurvature = 0.5f,
            minTouchTarget = 160.dp, // hand-gesture friendly
            glassBlur = 16.dp,
            transitionDuration = 250
        )
    )

    val NEUMORPHIC = ThemePreset(
        id = "neumorphic",
        displayName = "Neumorphic",
        description = "Soft shadow, extruded elements",
        palette = null,
        materialMode = MaterialMode.MountainView, // base, overridden by dualShadow
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 16.dp,
            cornerRadiusLg = 24.dp,
            defaultElevation = 0.dp,
            dualShadow = true,
            lightShadowOffset = 6.dp,
            darkShadowOffset = 6.dp,
            lightShadowBlur = 16.dp,
            darkShadowBlur = 16.dp,
            borderWidth = 0.dp
        )
    )

    val VISION_OS = ThemePreset(
        id = "visionos",
        displayName = "VisionOS",
        description = "Apple Vision spatial glass panels",
        palette = AvanueColorPalette.HYDRA,
        materialMode = MaterialMode.Glass,
        appearance = AppearanceMode.Dark,
        overrides = ThemeOverrides(
            cornerRadiusMd = 16.dp,
            cornerRadiusLg = 28.dp,
            glassBlur = 24.dp,
            specularHighlight = true,
            ambientTint = true,
            cardElevation = 0.dp,
            panelCurvature = 0.15f,
            transitionDuration = 450,
            springDamping = 0.75f
        )
    )

    val LIQUID_UI = ThemePreset(
        id = "liquid_ui",
        displayName = "LiquidUI",
        description = "Fluid organic design",
        palette = AvanueColorPalette.HYDRA,
        materialMode = MaterialMode.Water,
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 20.dp,
            cornerRadiusLg = 32.dp,
            springDamping = 0.6f,
            transitionDuration = 500,
            typographyScale = 1.05f
        )
    )

    val ALL = listOf(
        CUPERTINO, MOUNTAIN_VIEW, MOUNTAIN_VIEW_XR,
        META_FACIAL, NEUMORPHIC, VISION_OS, LIQUID_UI
    )

    fun findById(id: String): ThemePreset? = ALL.find { it.id == id }
}
```

### 5.5 Integration with AvanueThemeProvider

The preset system **layers on top of** the existing v5.1 architecture:

```kotlin
@Composable
fun AvanueThemeProvider(
    // Existing params...
    colors: AvanueColorScheme,
    glass: AvanueGlassScheme,
    water: AvanueWaterScheme,
    displayProfile: DisplayProfile,
    materialMode: MaterialMode,
    isDark: Boolean,
    // NEW: optional preset overrides
    themeOverrides: ThemeOverrides = ThemeOverrides(),
    content: @Composable () -> Unit
)
```

When a preset is active, the app resolves:
1. Palette from preset (or user's current choice if preset.palette == null)
2. MaterialMode from preset
3. Appearance from preset (or user's choice if null)
4. ThemeOverrides passed to provider → unified components read overrides via CompositionLocal

### 5.6 DataStore Keys

```
theme_palette    = "HYDRA"           # AvanueColorPalette name
theme_style      = "Glass"           # MaterialMode name
theme_appearance = "Auto"            # AppearanceMode name
theme_preset     = "visionos"        # ThemePreset ID (null = custom/manual)
```

When preset is set, palette/style/appearance are derived from preset. When user manually changes any axis, preset resets to null (custom mode).

---

## 6. Developer Settings UI

### 6.1 Theme Settings Hierarchy

```
Settings > Appearance
├── Theme Preset (dropdown)
│   ├── Custom (manual axes)
│   ├── Cupertino
│   ├── MountainView
│   ├── MountainView XR
│   ├── MetaFacial
│   ├── Neumorphic
│   ├── VisionOS
│   └── LiquidUI
├── Color Palette (SOL/LUNA/TERRA/HYDRA) — editable unless preset locks it
├── Material Style (Glass/Water/Cupertino/MountainView) — editable unless preset locks it
├── Appearance (Light/Dark/Auto) — editable unless preset locks it
└── Advanced
    ├── Corner Radius (slider)
    ├── Elevation (slider)
    ├── Animation Speed (slider)
    └── Panel Curvature (slider, glass displays only)
```

### 6.2 Scope Levels

Per the request, theme settings apply at 5 levels:

| Level | Scope | Storage |
|-------|-------|---------|
| **Global** | All apps | Foundation DataStore (theme_* keys) |
| **App Level** | Per-app override | DataStore per-app key |
| **Module Level** | Per-module accent color | AvanueModuleAccents registry |
| **Cockpit-Specific** | Dashboard layout/style | Cockpit DataStore |
| **Spatial/PseudoSpatial** | Glass-specific overrides | DisplayProfile-conditional |

Module-level accent colors already exist via `AvanueModuleAccents.set(moduleId, ModuleAccent)`. The settings screen exposes this as a color picker per module.

---

## 7. Implementation Roadmap

### Phase 1: Core (P0) — Browser + KMP

| Task | Type | Files | Agent |
|------|------|-------|-------|
| 1.1 Browser shell (Dashboard + 4 layouts) | NEW | `Demo/cockpit-browser/**` | general-purpose |
| 1.2 KMP centering audit (14 layouts) | FIX | LayoutEngine.kt, all layouts | code-reviewer |
| 1.3 Workflow CRUD in ViewModel | ENHANCE | CockpitViewModel.kt, WorkflowSidebar.kt | code-quality-enforcer |
| 1.4 Content action wiring (CommandBar) | ENHANCE | CommandBar.kt, ContentRenderer.kt | general-purpose |

### Phase 2: Theme System (P0-P1)

| Task | Type | Files | Agent |
|------|------|-------|-------|
| 2.1 ThemePreset data model | NEW | `AvanueUI/theme/ThemePreset.kt` | general-purpose |
| 2.2 ThemePresetRegistry (7 presets) | NEW | `AvanueUI/theme/ThemePresetRegistry.kt` | general-purpose |
| 2.3 ThemeOverrides CompositionLocal | ENHANCE | AvanueTheme.kt | code-quality-enforcer |
| 2.4 Browser theme engine (32 combos CSS) | NEW | `Demo/cockpit-browser/css/themes.css` | general-purpose |

### Phase 3: Enhanced UI (P1)

| Task | Type | Files | Agent |
|------|------|-------|-------|
| 3.1 Triptych layout mode | NEW | TriptychLayout.kt, LayoutMode.kt | general-purpose |
| 3.2 Background scenes | ENHANCE | CockpitScreenContent.kt | general-purpose |
| 3.3 Browser: all 14 layouts + theme selector | ENHANCE | `Demo/cockpit-browser/**` | general-purpose |
| 3.4 Browser: PseudoSpatial + Glass CSS | NEW | `Demo/cockpit-browser/js/spatial.js` | general-purpose |

### Phase 4: Settings + Polish (P1-P2)

| Task | Type | Files | Agent |
|------|------|-------|-------|
| 4.1 Theme settings screen (preset picker) | NEW | CockpitSettingsPanel.kt or SettingsProvider | general-purpose |
| 4.2 Module accent color picker | ENHANCE | AvanueModuleAccents, Settings | general-purpose |
| 4.3 Browser: IndexedDB persistence | NEW | `Demo/cockpit-browser/js/persistence.js` | general-purpose |
| 4.4 Deep link entry points | ENHANCE | CockpitScreen.kt | general-purpose |

---

## 8. Technical Decisions (Auto-Approved, .auto mode)

### D1: Browser Cockpit Location
**Decision:** `Demo/cockpit-browser/` (NOT inside Modules/)
**Reason:** The browser cockpit is a standalone HTML/CSS/JS app, not a KMP module. It mirrors the v4.5 demo approach. It can be served from any HTTP server or opened directly in a browser.

### D2: ThemePreset vs New MaterialMode Values
**Decision:** Presets are curated combos + ThemeOverrides, NOT new MaterialMode enum values
**Reason:** Adding Neumorphic/VisionOS/etc. as MaterialMode values would require every unified component to handle them. Instead, presets compose existing MaterialModes with overrides. The neumorphic dual-shadow is implemented as a Modifier extension that any component can opt into when `themeOverrides.dualShadow == true`.

### D3: Browser Theme Token Strategy
**Decision:** CSS custom properties with `[data-palette][data-appearance]` attribute selectors
**Reason:** Zero-JS theme switching (attribute change triggers CSS cascade). All 32 combos are defined as CSS rulesets. MaterialMode is handled via class toggling (`.mode-glass`, `.mode-water`, `.mode-cupertino`, `.mode-mountainview`).

### D4: Triptych as New LayoutMode
**Decision:** Add `LayoutMode.TRIPTYCH` (15th layout mode)
**Reason:** The v4.5 triptych is distinct from SPLIT_LEFT/RIGHT — it's a 3-panel angled cockpit layout with 14-16 degree panel angles. It deserves its own mode for voice command targeting ("switch to triptych").

### D5: Browser Content Types
**Decision:** Limit to 5 content types (Web iframe, PDF.js, Image, HTML5 Video, Rich-text Note)
**Reason:** Camera, VoiceNote, AI Summary, Terminal, etc. require platform APIs not available in a browser context. The 5 supported types cover the core use case.

### D6: Neumorphic Implementation
**Decision:** Custom `Modifier.neumorphicShadow()` in AvanueUI, activated by `themeOverrides.dualShadow`
**Reason:** Standard Compose elevation can't do dual-direction or inner shadows. The modifier draws Canvas shadows manually. On API < 31, uses software rendering; API 31+ uses RenderEffect blur.

---

## 9. File Creation Summary

### New Files

| File | Location | Purpose |
|------|----------|---------|
| `ThemePreset.kt` | `Modules/AvanueUI/src/commonMain/kotlin/com/augmentalis/avanueui/theme/` | Data model |
| `ThemePresetRegistry.kt` | `Modules/AvanueUI/src/commonMain/kotlin/com/augmentalis/avanueui/theme/` | 7 preset definitions |
| `ThemeOverrides.kt` | `Modules/AvanueUI/src/commonMain/kotlin/com/augmentalis/avanueui/theme/` | Override tokens |
| `NeumorphicModifier.kt` | `Modules/AvanueUI/src/commonMain/kotlin/com/augmentalis/avanueui/components/` | Dual-shadow modifier |
| `TriptychLayout.kt` | `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/` | Triptych layout |
| `CockpitSettingsPanel.kt` | `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/` | In-cockpit settings |
| `Demo/cockpit-browser/**` | `Demo/cockpit-browser/` | Full browser cockpit (8+ files) |

### Modified Files

| File | Changes |
|------|---------|
| `LayoutMode.kt` | Add TRIPTYCH entry |
| `LayoutEngine.kt` | Add TRIPTYCH case |
| `LayoutModeResolver.kt` | Add TRIPTYCH availability/defaults |
| `CommandBar.kt` | Wire content-specific actions, add triptych icon |
| `CockpitViewModel.kt` | Add workflow CRUD methods |
| `WorkflowSidebar.kt` | Add edit/reorder/delete step UI |
| `CockpitScreenContent.kt` | Add background scene support |
| `AvanueTheme.kt` | Add LocalThemeOverrides CompositionLocal |
| `AvanueThemeProvider` | Accept ThemeOverrides parameter |

---

## 10. Swarm Dispatch Plan

For `.swarm` execution, these agent groups can run in parallel:

**Wave 1 (Independent):**
- Agent A: Browser cockpit Phase B1 (shell + 4 layouts)
- Agent B: KMP centering audit (14 layouts)
- Agent C: Theme data model (ThemePreset, ThemeOverrides, ThemePresetRegistry)

**Wave 2 (Depends on Wave 1):**
- Agent D: Browser cockpit Phase B2 (all 14 layouts + theme engine)
- Agent E: KMP workflow CRUD + content actions
- Agent F: AvanueTheme integration (LocalThemeOverrides, provider update)

**Wave 3 (Depends on Wave 2):**
- Agent G: Browser cockpit Phase B3-B4 (content types + spatial)
- Agent H: TriptychLayout + background scenes
- Agent I: Neumorphic modifier

**Wave 4 (Depends on all):**
- Agent J: Settings screen + persistence
- Agent K: Code review + quality enforcement

---

## 11. Success Criteria

- [ ] Browser cockpit renders all 14 layout modes correctly
- [ ] Theme engine supports all 32 palette×mode×appearance combos
- [ ] 7 premade presets selectable from settings
- [ ] All 14 KMP layouts properly centered on all device profiles
- [ ] Workflow CRUD (add/edit/reorder/delete steps) functional
- [ ] Content-specific CommandBar actions wired (PDF page nav, web back/forward)
- [ ] Triptych layout available as 15th layout mode
- [ ] AvanueUI v5.1 compliance: no MaterialTheme.colorScheme usage, all AvanueTheme.colors.*

---

## 12. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Neumorphic dual-shadow performance on low-end Android | Medium | Guard with `Build.VERSION.SDK_INT >= 31` check; fallback to single shadow |
| Browser cockpit scope creep | High | Strict P0/P1/P2 prioritization; P2 items are stretch goals |
| ThemeOverrides adding complexity to unified components | Medium | Overrides are optional (null = default); components only read what they need |
| 15 layout modes becoming unwieldy | Low | LayoutModeResolver filters per device; users see 8-10 relevant modes |
| CSS custom property browser support | Low | Supported in all modern browsers since 2017 |

---

*Plan authored for Cockpit UI + Theme System implementation. Ready for execution.*
