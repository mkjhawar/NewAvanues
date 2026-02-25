# Cockpit Voice-First UI Shell System Specification

**Version**: 1.0
**Date**: 2026-02-25
**Status**: Implemented
**Copyright**: Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC

---

## 1. System Overview

The Voice-First UI Shell System simplifies Cockpit's original 15-layout interface into 4 cohesive interaction paradigms, each optimized for a different user profile and device class. All shells share:

- **Unified settings model** persisted in Foundation (`AvanuesSettings.shellMode` + `AvanuesSettings.defaultArrangement`)
- **Layout abstraction** via `ArrangementIntent` → `LayoutMode` resolution
- **Voice command routing** through VoiceOSCore handlers (unchanged; only UI presentation differs)
- **Contextual action system** via `ContextualActionProvider` (flat 5-6 top actions + searchable "More" sheet)
- **Frame types** — 18 content types (Web, PDF, Note, Camera, Whiteboard, etc.) with platform-specific renderers

### Architecture Relationships

```
Settings Layer:
  AvanuesSettings.shellMode → SimplifiedShellMode enum
  AvanuesSettings.defaultArrangement → ArrangementIntent enum

UI Presentation Layer:
  SimplifiedShellMode → shell-specific home screen + navigation UX

Frame Management Layer:
  ArrangementIntent + FrameCount + DisplayProfile → IntentResolver.resolve() → LayoutMode
  LayoutMode → 15 concrete layout implementations (Cockpit, Grid, Carousel, etc.)

Action Layer:
  FrameContent.typeId → ContextualActionProvider.topActionsForContent() → 5-6 action chips
                     → ContextualActionProvider.allActionsForContent() → grouped "More" sheet

Voice Layer:
  Voice phrase → VoiceOSCore handler (unchanged)
  Handler calls executor → ViewModel updates → UI re-renders
```

---

## 2. Shell Modes

Four simplified UI shells replace the original Cockpit single-mode + 15-layout system.

| Mode | Label | Description | Best For | Default? |
|------|-------|-------------|----------|----------|
| `CLASSIC` | Classic | Original Cockpit Dashboard + hierarchical CommandBar with 15-layout selector | Power users, backward compat | No |
| `AVANUE_VIEWS` | AvanueViews | Ambient card stream surfacing content by context priority (active work, suggestions) | Casual users, smart glasses, ambient computing | No |
| `LENS` | Lens | Universal command palette — single search/voice bar for everything (modules, commands, settings, files) | Power users, keyboard warriors, desktop | **YES (DEFAULT)** |
| `CANVAS` | Canvas | Infinite zoomable spatial canvas with module islands and semantic zoom levels | Creative workers, tablet users, spatial computing | No |

### Model Definition

```kotlin
@Serializable
enum class SimplifiedShellMode(
    val displayLabel: String,
    val description: String,
) {
    CLASSIC(
        displayLabel = "Classic",
        description = "Traditional dashboard with module tiles and command bar"
    ),
    AVANUE_VIEWS(
        displayLabel = "AvanueViews",
        description = "Ambient card stream — context-aware, minimal"
    ),
    LENS(
        displayLabel = "Lens",
        description = "Universal command palette — one search bar for everything"
    ),
    CANVAS(
        displayLabel = "Canvas",
        description = "Spatial zen canvas — zoom to navigate, organic layout"
    );

    companion object {
        val DEFAULT = LENS
        fun fromString(value: String): SimplifiedShellMode = /* ... */
    }
}
```

### Storage & Retrieval

- **Key**: `SettingsKeys.SHELL_MODE` ("shell_mode")
- **Type**: String (serialized enum name)
- **Default**: "LENS"
- **DataStore path**: `AvanuesSettings.shellMode` → stored as `SimplifiedShellMode.LENS.name`
- **Mutation**: Update via `AvanuesSettingsRepository.updateShellMode(newMode)`

---

## 3. Arrangement Intent

Instead of exposing 15 raw layout modes to users, the UI presents 4 natural layout intents that map directly to voice commands.

| Intent | Voice Command | Icon | Description | Resolves To |
|--------|---|---|---|---|
| `FOCUS` | "focus" | fullscreen | Single frame fills the screen | `FULLSCREEN` |
| `COMPARE` | "compare" | vertical_split | Two frames side by side | `SPLIT_LEFT` / `T_PANEL` / `SPLIT_RIGHT` (varies by device) |
| `OVERVIEW` | "overview" | grid_view | All frames in an auto-arranged grid | `GRID` / `MOSAIC` / `ROW` (varies by frame count and device) |
| `PRESENT` | "present" | slideshow | Showcase/carousel mode for presentations | `CAROUSEL` / `TRIPTYCH` / `FULLSCREEN` |

### Intent Resolver Algorithm

The `IntentResolver` object maps each intent to the optimal `LayoutMode` based on **frame count**, **display profile**, and **spatial availability**.

#### FOCUS Intent
```kotlin
fun resolveForFocus(): LayoutMode = LayoutMode.FULLSCREEN
```
Always fullscreen, regardless of device.

#### COMPARE Intent (2 frames side-by-side)
```kotlin
fun resolveForCompare(frameCount: Int, displayProfile: DisplayProfile): LayoutMode =
    if (frameCount < 2) LayoutMode.FULLSCREEN
    else when (displayProfile) {
        // Glasses: always SPLIT_LEFT (primary left, reference right)
        GLASS_MICRO, GLASS_COMPACT, GLASS_STANDARD -> SPLIT_LEFT

        // Phone: T_PANEL (top/bottom) works better than side-by-side
        PHONE -> T_PANEL

        // Tablet/Desktop/HD: true side-by-side split
        TABLET, GLASS_HD -> SPLIT_LEFT
    }
```

#### OVERVIEW Intent (3+ frames in grid)
```kotlin
fun resolveForOverview(frameCount: Int, displayProfile: DisplayProfile, spatialAvailable: Boolean): LayoutMode =
    when {
        frameCount <= 1 -> FULLSCREEN
        frameCount == 2 -> resolveForCompare(frameCount, displayProfile)

        // Glass: limited space, use ROW for scrollable overview
        displayProfile.isGlass -> ROW

        // 3 frames: MOSAIC (1 large + 2 smaller)
        frameCount == 3 -> MOSAIC

        // 4 frames on tablet+: clean 2x2 GRID
        frameCount == 4 -> GRID

        // 5 frames with spatial: SPATIAL_DICE (4 corners + center)
        frameCount == 5 && spatialAvailable -> SPATIAL_DICE

        // 5-6 frames: MOSAIC adapts well
        frameCount in 5..6 -> MOSAIC

        // 7+ frames: GRID scales best
        else -> GRID
    }
```

#### PRESENT Intent (carousel/showcase)
```kotlin
fun resolveForPresent(frameCount: Int, displayProfile: DisplayProfile): LayoutMode =
    when {
        // Single frame or glass: fullscreen
        frameCount <= 1 || displayProfile.isGlass -> FULLSCREEN

        // 3 frames exactly: TRIPTYCH (book spread)
        frameCount == 3 -> TRIPTYCH

        // Other frame counts: CAROUSEL with perspective
        else -> CAROUSEL
    }
```

### Bidirectional Mapping

When users manually change a layout mode (classic mode only), the system infers the most likely intent:

```kotlin
fun inferIntent(layoutMode: LayoutMode): ArrangementIntent = when (layoutMode) {
    FULLSCREEN -> FOCUS
    SPLIT_LEFT, SPLIT_RIGHT -> COMPARE
    GRID, MOSAIC, T_PANEL, COCKPIT, ROW, FREEFORM,
    SPATIAL_DICE, WORKFLOW, GALLERY -> OVERVIEW
    CAROUSEL, TRIPTYCH -> PRESENT
    DASHBOARD -> OVERVIEW
}
```

### Storage & Voice Routing

- **Key**: `SettingsKeys.DEFAULT_ARRANGEMENT` ("default_arrangement")
- **Type**: String (serialized intent name)
- **Default**: "FOCUS"
- **Voice trigger**: Accessibility service detects phrase "focus", "compare", "overview", or "present"
  - Calls `ArrangementIntent.fromVoiceCommand(phrase)` → dispatches layout command
  - Handler resolves intent → updates UI layout via LayoutMode

---

## 4. Contextual Action Provider

Replaces the 13-state `CommandBarState` hierarchy with a flat, single-level action API. Actions are content-type-aware and displayed as 5-6 visual chips in the action bar, with a searchable "More" sheet for full catalog.

### Action Model

```kotlin
@Serializable
data class QuickAction(
    val id: String,           // Unique ID (e.g., "web_back", "note_bold")
    val label: String,        // Display label (e.g., "Back", "Bold")
    val iconName: String,     // Material icon name (e.g., "arrow_back", "format_bold")
)

@Serializable
data class ActionGroup(
    val category: String,     // "Web", "Frame", "Layout", "Tools"
    val actions: List<QuickAction>,
)
```

### Top Actions by Content Type

Each content type has a curated set of 5-6 most-used actions displayed directly in the action bar.

| Content Type | Top Actions |
|---|---|
| **Web** | Back, Forward, Refresh, Zoom In, Zoom Out |
| **PDF** | Prev Page, Next Page, Zoom In, Zoom Out, Search |
| **Image** | Zoom In, Zoom Out, Rotate, Share |
| **Video** | Rewind, Play/Pause, Forward, Fullscreen |
| **Note** | Bold, Italic, Underline, Undo, Redo, Save |
| **Camera** | Flip, Capture, Flash |
| **Whiteboard** | Pen, Highlight, Eraser, Undo, Redo, Clear |
| **Terminal** | Clear, Copy, Top, Bottom |
| **Map** | Zoom In, Zoom Out, Center, Layers |
| **Default** | Minimize, Maximize, Close |

### Full Action Groups

Accessed via "More" button → bottom sheet:

1. **Content Actions** — Type-specific actions (same as top actions)
2. **Frame Actions** — Minimize, Maximize, Close, Fullscreen, Pin
3. **Layout Actions** — Focus, Compare, Overview, Present (mapped to intents)
4. **Tools** — Share, Screenshot, Clone Frame, Send To, AI Summary

### API

```kotlin
object ContextualActionProvider {

    /// Get 5-6 top actions for a content type
    fun topActionsForContent(contentTypeId: String): List<QuickAction>

    /// Get all actions grouped by category
    fun allActionsForContent(contentTypeId: String): List<ActionGroup>

    /// Search actions by label or ID
    fun searchActions(contentTypeId: String, query: String): List<QuickAction>
}
```

### Content Type IDs

All 18 content types supported:

```
"web", "pdf", "image", "video", "note", "voice_note", "camera", "whiteboard",
"terminal", "map", "form", "signature", "voice", "ai_summary", "screen_cast",
"widget", "file", "external_app"
```

---

## 5. Voice Command Matrix

Voice commands are agnostic to shell mode — they route through VoiceOSCore handlers as before. The shell mode only affects **visual presentation**.

### Top-Level Intent Commands

| Phrase | Handler | Executor | Effect |
|--------|---------|----------|--------|
| "focus" | CockpitLayoutHandler | updateLayout(ArrangementIntent.FOCUS) | Sets LayoutMode.FULLSCREEN |
| "compare" | CockpitLayoutHandler | updateLayout(ArrangementIntent.COMPARE) | Resolves COMPARE → SPLIT_LEFT/T_PANEL |
| "overview" | CockpitLayoutHandler | updateLayout(ArrangementIntent.OVERVIEW) | Resolves OVERVIEW → GRID/MOSAIC/ROW |
| "present" | CockpitLayoutHandler | updateLayout(ArrangementIntent.PRESENT) | Resolves PRESENT → CAROUSEL/TRIPTYCH |

### Action Commands

Action chips are displayed visually; users can click or voice-activate them.

**Example for Web frame:**
- User says "back" → VoiceOSCore routes to WebCommandHandler
- WebCommandHandler executes `navigateBack()` on WebFrame
- WebFrame ViewModel updates state → UI updates URL bar

**Example for Note frame:**
- User says "bold" → VoiceOSCore routes to NoteCommandHandler
- NoteCommandHandler executes `applyStyle(BOLD)` on NoteContent
- RichTextEditor applies style → ViewModel updates → UI highlights selected text

### Handler Registration

Handlers register via `AndroidHandlerFactory.createHandlers()` (or platform-specific equivalent):

```kotlin
private fun createHandlers(): List<IHandler> {
    return listOf(
        // Core handlers
        MediaHandler(),
        ScreenHandler(),
        TextHandler(),
        InputHandler(),
        AppControlHandler(),
        ReadingHandler(),
        VoiceControlHandler(),

        // Content-specific handlers
        WebCommandHandler(),
        PdfCommandHandler(),
        NoteCommandHandler(),
        CameraCommandHandler(),
        WhiteboardCommandHandler(),

        // Layout/Navigation handlers
        CockpitLayoutHandler(),

        // ... 5+ more
    )
}
```

---

## 6. Settings Schema

All Cockpit-related settings are persisted in `AvanuesSettings` (Foundation commonMain).

### Cockpit Settings

| Key | Type | Default | Description | Validation |
|-----|------|---------|-------------|------------|
| `shell_mode` | String (enum) | "LENS" | Active shell: CLASSIC, AVANUE_VIEWS, LENS, CANVAS | Must match SimplifiedShellMode name |
| `default_arrangement` | String (enum) | "FOCUS" | Default layout intent: FOCUS, COMPARE, OVERVIEW, PRESENT | Must match ArrangementIntent name |
| `cockpit_max_frames` | Int | 6 | Max concurrent frames (limits memory + UI density) | 1–12 |
| `cockpit_autosave_interval` | String | "1m" | Save session: 15s, 1m, 5m, 30m, 1h, never | Must match TimeUnit pattern |
| `cockpit_background_scene` | String | "GRADIENT" | Home screen background: GRADIENT, BLUR, SOLID, DYNAMIC | Enum validation |
| `cockpit_spatial_enabled` | Boolean | false | Enable head-tracking spatial canvas (IMU) | N/A |
| `cockpit_spatial_sensitivity` | String | "NORMAL" | Spatial tracking sensitivity: LOW, NORMAL, HIGH | Enum validation |
| `cockpit_canvas_zoom_persist` | Boolean | true | Save/restore canvas zoom level on app restart | N/A |

### Related Settings (Other Modules)

These settings affect Cockpit content rendering or voice command behavior:

**Theme (AvanueUI v5.1):**
| Key | Type | Default |
|-----|------|---------|
| `theme_palette` | String | "HYDRA" |
| `theme_style` | String | "Water" |
| `theme_appearance` | String | "Auto" |

**Voice Control:**
| Key | Type | Default |
|-----|------|---------|
| `voice_feedback` | Boolean | true |
| `voice_command_locale` | String | "en-US" |
| `wake_word_enabled` | Boolean | false |
| `wake_word_keyword` | String | "HEY_AVA" |

**Content Module Defaults:**
| Key | Module | Type | Default |
|-----|--------|------|---------|
| `pdf_view_mode` | PDFAvanue | String | "Continuous" |
| `camera_default_lens` | PhotoAvanue | String | "Back" |
| `note_autosave` | NoteAvanue | String | "15s" |
| `web_desktop_mode` | WebAvanue | Boolean | false |
| `video_resume` | VideoAvanue | Boolean | true |
| `annotation_default_tool` | AnnotationAvanue | String | "Pen" |
| `image_show_exif` | ImageAvanue | Boolean | false |
| `cast_target_fps` | RemoteCast | Int | 15 |
| `file_view_mode` | FileAvanue | String | "List" |

### Settings Update Flow

```
User changes "Shell Mode" in Settings screen
  → SettingsProvider detects change (DataStore observer)
  → calls AvanuesSettingsRepository.updateShellMode(newMode)
  → persists to DataStore
  → SettingsViewModel.shelModeFlow emits new value
  → CockpitScreen collects new shell mode
  → re-renders home screen with new shell UI
```

---

## 7. Display Profiles

Cockpit adapts layout, action visibility, and voice feedback based on device class.

| Profile | Display | Use Case | Density | Max Frames | Spatial Support |
|---------|---------|----------|---------|------------|-----------------|
| `PHONE` | ~4"–6" 1080p | Handheld, voice-primary | 1.0x | 3–4 | Limited (head tracking via IMU) |
| `TABLET` | ~7"–12" 1440p+ | Handheld+stylus, reading | 1.5x | 4–6 | Yes (IMU) |
| `GLASS_MICRO` | ~0.5" 640p | Ultra-compact monocular | 2.5x | 1–2 | Limited (fixed FOV) |
| `GLASS_COMPACT` | ~1" 720p | Compact monocular (Z100) | 2.0x | 2–3 | Limited (fixed FOV) |
| `GLASS_STANDARD` | ~1.5" 1080p | Standard binocular (Vuzix) | 1.5x | 3–4 | Yes (IMU + stereo) |
| `GLASS_HD` | ~2" 1440p | Premium binocular (Rokid) | 1.0x | 4–6 | Yes (IMU + stereo) |
| `DESKTOP` | >13" 2560p+ | Stationary workstation | 0.75x | 6–12 | Yes (cursor position) |
| `COMPACT` | Foldables, splits | Variable | 1.5x | 3–6 | Limited |
| `STANDARD` | Generic (fallback) | Default | 1.0x | 6 | No |
| `HD` | Large displays | Premium | 1.0x | 12 | Yes |

### Auto-Detection

```kotlin
val profile = DisplayProfile.fromContext(context) // Android:
  -> measures screen size, density, orientation
  -> checks IMU availability (AccelerometerManager.isAvailable)
  -> returns appropriate profile
```

### Density Scaling

All fonts, spacing, icons scale based on profile:
- GLASS_MICRO: 2.5x zoom; reading distance = 20 cm; content very dense
- PHONE: 1.0x baseline; 40–50 cm distance
- TABLET: 1.5x; larger touch targets
- DESKTOP: 0.75x; smaller, detailed UI
- GLASS_STANDARD: 1.5x; binocular parallax compensated

### Layout Decisions

`IntentResolver` uses display profile to choose optimal `LayoutMode`:

```kotlin
resolve(
    intent = ArrangementIntent.COMPARE,
    frameCount = 2,
    displayProfile = DisplayProfile.GLASS_COMPACT,  // ← determines output
    spatialAvailable = false
) → LayoutMode.SPLIT_LEFT  // Always SPLIT_LEFT on glass
```

---

## 8. Glass-Specific Behaviors

When `DisplayProfile.isGlass == true`, Cockpit enables glass-optimized UX:

### 1. Pagination Instead of Dense Grids

Glass screens are small (0.5–2" diagonal, 600–1440p). Dense grids (3x3) are unreadable.

**Glass:**
- OVERVIEW intent → `LayoutMode.ROW` (horizontal scrollable strip)
- User scrolls left/right to see frames
- Page indicator: "Frame 1 of 6"

**Desktop:**
- OVERVIEW intent → `LayoutMode.GRID` (2x2 or 3x3)
- All frames visible simultaneously

### 2. colorsXR Palette

For AR/transparent glasses, Cockpit uses **colorsXR** colors — always dark, high luminance, additive blending for visibility over real-world scene.

```kotlin
// In AvanueTheme
val isDark = true  // Always dark on glass
val colors = if (isXR) palette.colorsXR else palette.colors(isDark)
```

**colorsXR characteristics:**
- Background: 0xFF1a1a1a (near-black, see-through)
- Primary: 0xFF1565C0 + luminance boost (30%)
- Surface: 0xFF2a2a2a
- Text: 0xFFFFFFFF (max contrast)

### 3. Voice-Only Mode

On GLASS_MICRO (Ultralite SDK), visual chrome is minimized:

- No visible app bar (title in corner only)
- Action bar hidden by default (swipe to reveal)
- Voice feedback essential: "Ready for command"
- Text is larger (16–18pt minimum)

### 4. Touch Target Sizing

Glass-specific touch targets (buttons, chips) are 44dp × 44dp (vs. 36dp on phone).

---

## 9. Platform Support Matrix

| Feature | Android | iOS | Desktop | Web |
|---------|---------|-----|---------|-----|
| SimplifiedShellMode | FULL | Planned | FULL | Planned |
| ArrangementIntent | FULL | Planned | FULL | Planned |
| ContextualActionProvider | FULL | Partial* | FULL | Partial* |
| Spatial Canvas (IMU) | FULL | Accelerometer only | Manual (cursor) | No |
| Glass Profile Detection | FULL | No | No | No |
| Content Renderers (18 types) | 15/18 | 8/18 | 10/18 | 12/18 |
| Voice Commands | FULL | Planned | Limited** | Limited** |
| Settings Persistence | DataStore | UserDefaults | Java Prefs | LocalStorage |

**Notes:**
- *Partial: subset of actions; full "More" sheet coming in Phase 2
- **Limited: voice via WebAvanue only (no system-level STT)

### Phase Roadmap

| Phase | Scope | Target Date | Status |
|-------|-------|-------------|--------|
| **Phase 1 (V1.0)** | Android SimplifiedShellMode + ArrangementIntent + ContextualActionProvider | 2026-02-25 | DONE |
| **Phase 2** | iOS shell modes + improved action sheets; Desktop refinements | 2026-03-15 | In Progress |
| **Phase 3** | Web shell modes (Next.js) + full action provider parity | 2026-04-01 | Planned |
| **Phase 4** | AVANUE_VIEWS ambient card engine; CANVAS semantic zoom | 2026-04-30 | Planned |
| **Phase 5** | Glass-specific UX refinements (GLASS_MICRO pagination, colorsXR edge cases) | 2026-05-31 | Planned |

---

## 10. Data Flow Diagrams

### Settings → UI Update Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ Settings Screen                                                  │
│ User toggles: Shell Mode = AVANUE_VIEWS → LENS                 │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ AvanuesSettingsRepository.updateShellMode(LENS)                 │
│ → ISettingsStore.save(SettingsKeys.SHELL_MODE, "LENS")         │
│ → DataStore/UserDefaults/JavaPrefs persists to disk            │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ SettingsDataStore.preferencesFlow (StateFlow)                   │
│ emits new AvanuesSettings(shellMode = "LENS", ...)             │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ CockpitViewModel.shellModeFlow (StateFlow)                       │
│ observes preferencesFlow, maps to SimplifiedShellMode           │
│ emits SimplifiedShellMode.LENS                                  │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ CockpitScreen Composable                                         │
│ collectAsState(shellModeFlow) → currentShellMode = LENS         │
│ renders Shell implementation:                                    │
│  - LENS: SearchBar + action chips + recent results             │
│  - AVANUE_VIEWS: VerticalCardStream                             │
│  - CANVAS: ZoomableCanvas                                       │
│  - CLASSIC: DashboardGrid + 15-layout picker                    │
└──────────────────────────────────────────────────────────────────┘
```

### Voice Command → Layout Update Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ Accessibility Service (Android) / Voice Input (iOS)              │
│ Detects phrase: "overview"                                       │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ VoiceOSCore CommandDispatcher                                    │
│ routes phrase → CockpitLayoutHandler.canHandle() = true         │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ CockpitLayoutHandler.handle("overview")                          │
│ calls: LayoutExecutor.updateLayout(ArrangementIntent.OVERVIEW)  │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ IntentResolver.resolve(                                          │
│   intent = OVERVIEW,                                             │
│   frameCount = 4,                                                │
│   displayProfile = TABLET,                                       │
│   spatialAvailable = true                                        │
│ ) → returns LayoutMode.GRID                                      │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ CockpitViewModel.layoutModeFlow emits LayoutMode.GRID           │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ CockpitScreen re-renders with GridLayout                         │
│ frames arranged in 2x2 grid                                      │
└──────────────────────────────────────────────────────────────────┘
```

### Content Type → Action Provider Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ Frame focused: Web(url="google.com")                             │
│ ContentType = "web"                                              │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ ActionBar Composable                                              │
│ calls: ContextualActionProvider.topActionsForContent("web")     │
│ returns: [Back, Forward, Refresh, ZoomIn, ZoomOut]             │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ ActionBar renders 5 action chips + "More" button                 │
│ User clicks "More" button                                        │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ BottomSheet opens                                                 │
│ calls: ContextualActionProvider.allActionsForContent("web")    │
│ returns: [                                                        │
│   ActionGroup("Web", [Back, Forward, Refresh, ZoomIn, ZoomOut]), │
│   ActionGroup("Frame", [Minimize, Maximize, Close, ...]),        │
│   ActionGroup("Layout", [Focus, Compare, Overview, Present]),    │
│   ActionGroup("Tools", [Share, Screenshot, Clone, ...])          │
│ ]                                                                  │
└──────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────┐
│ MoreActionsSheet renders 4 groups with searchable filter         │
│ User selects "Screenshot" → executes via executor                │
└──────────────────────────────────────────────────────────────────┘
```

---

## 11. API Reference

### SimplifiedShellMode

```kotlin
@Serializable
enum class SimplifiedShellMode(
    val displayLabel: String,
    val description: String,
) {
    CLASSIC, AVANUE_VIEWS, LENS, CANVAS

    companion object {
        val DEFAULT = LENS
        fun fromString(value: String): SimplifiedShellMode
    }
}
```

### ArrangementIntent

```kotlin
@Serializable
enum class ArrangementIntent(
    val displayLabel: String,
    val voiceCommand: String,
    val iconName: String,
    val description: String,
) {
    FOCUS, COMPARE, OVERVIEW, PRESENT

    companion object {
        fun fromVoiceCommand(command: String): ArrangementIntent?
        fun fromString(value: String): ArrangementIntent?
    }
}
```

### IntentResolver

```kotlin
object IntentResolver {
    fun resolve(
        intent: ArrangementIntent,
        frameCount: Int,
        displayProfile: DisplayProfile = DisplayProfile.PHONE,
        spatialAvailable: Boolean = false,
    ): LayoutMode

    fun inferIntent(layoutMode: LayoutMode): ArrangementIntent
}
```

### ContextualActionProvider

```kotlin
object ContextualActionProvider {
    fun topActionsForContent(contentTypeId: String): List<QuickAction>
    fun allActionsForContent(contentTypeId: String): List<ActionGroup>
    fun searchActions(contentTypeId: String, query: String): List<QuickAction>
}

@Serializable
data class QuickAction(
    val id: String,
    val label: String,
    val iconName: String,
)

@Serializable
data class ActionGroup(
    val category: String,
    val actions: List<QuickAction>,
)
```

### FrameContent

```kotlin
@Serializable
sealed class FrameContent {
    abstract val typeId: String

    // 18 subtypes
    data class Web(...) : FrameContent()
    data class Pdf(...) : FrameContent()
    data class Image(...) : FrameContent()
    data class Video(...) : FrameContent()
    data class Note(...) : FrameContent()
    data class Camera(...) : FrameContent()
    data class VoiceNote(...) : FrameContent()
    data class Form(...) : FrameContent()
    data class Signature(...) : FrameContent()
    data class Voice(...) : FrameContent()
    data class Map(...) : FrameContent()
    data class Whiteboard(...) : FrameContent()
    data class Terminal(...) : FrameContent()
    data class AiSummary(...) : FrameContent()
    data class ScreenCast(...) : FrameContent()
    data class Widget(...) : FrameContent()
    data class File(...) : FrameContent()
    data class ExternalApp(...) : FrameContent()
}
```

### LayoutMode

```kotlin
@Serializable
enum class LayoutMode {
    DASHBOARD,
    FREEFORM,
    GRID,
    SPLIT_LEFT,
    SPLIT_RIGHT,
    COCKPIT,
    T_PANEL,
    MOSAIC,
    FULLSCREEN,
    WORKFLOW,
    ROW,
    CAROUSEL,
    SPATIAL_DICE,
    GALLERY,
    TRIPTYCH;

    companion object {
        val DEFAULT = COCKPIT
        val SPATIAL_CAPABLE = setOf(FREEFORM, COCKPIT, MOSAIC, T_PANEL, SPATIAL_DICE)
        val FRAME_LAYOUTS: Set<LayoutMode> = entries.toSet() - DASHBOARD
    }
}
```

### Settings Keys (Foundation)

```kotlin
object SettingsKeys {
    const val SHELL_MODE = "shell_mode"
    const val DEFAULT_ARRANGEMENT = "default_arrangement"
    const val COCKPIT_MAX_FRAMES = "cockpit_max_frames"
    const val COCKPIT_AUTOSAVE_INTERVAL = "cockpit_autosave_interval"
    const val COCKPIT_BACKGROUND_SCENE = "cockpit_background_scene"
    const val COCKPIT_SPATIAL_ENABLED = "cockpit_spatial_enabled"
    const val COCKPIT_SPATIAL_SENSITIVITY = "cockpit_spatial_sensitivity"
    const val COCKPIT_CANVAS_ZOOM_PERSIST = "cockpit_canvas_zoom_persist"
    // ... 40+ additional keys for all modules
}
```

### AvanuesSettings

```kotlin
data class AvanuesSettings(
    val shellMode: String = DEFAULT_SHELL_MODE,  // "LENS"
    val defaultArrangement: String = DEFAULT_ARRANGEMENT,  // "FOCUS"
    val cockpitMaxFrames: Int = DEFAULT_MAX_FRAMES,  // 6
    val cockpitAutosaveInterval: String = DEFAULT_AUTOSAVE_INTERVAL,  // "1m"
    val cockpitBackgroundScene: String = DEFAULT_BACKGROUND_SCENE,  // "GRADIENT"
    val cockpitSpatialEnabled: Boolean = false,
    val cockpitSpatialSensitivity: String = DEFAULT_SPATIAL_SENSITIVITY,  // "NORMAL"
    val cockpitCanvasZoomPersist: Boolean = true,
    // ... 100+ additional fields for all modules
) {
    companion object {
        const val DEFAULT_SHELL_MODE = "LENS"
        const val DEFAULT_ARRANGEMENT = "FOCUS"
        const val DEFAULT_MAX_FRAMES = 6
        // ... 40+ additional constants
    }
}
```

---

## Reference Documents

- **Chapter 112**: `Docs/MasterDocs/Cockpit/Developer-Manual-Chapter112-SimplifiedVoiceFirstUIShells.md`
- **Chapter 97**: `Docs/MasterDocs/Cockpit/Developer-Manual-Chapter97-CockpitSpatialVoiceMultiWindow.md` (Layout modes reference)
- **Chapter 91**: `Docs/MasterDocs/AvanueUI/Developer-Manual-Chapter91-AvanueUIDesignSystem.md` (Theme system reference)
- **Chapter 96**: `Docs/MasterDocs/Foundation/Developer-Manual-Chapter96-KMPFoundationPlatformAbstractions.md` (Settings persistence)

---

**End of Document**
