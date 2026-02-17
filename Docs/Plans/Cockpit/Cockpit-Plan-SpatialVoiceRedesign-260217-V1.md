# Cockpit-Plan-SpatialVoiceRedesign-260217-V1

## Overview

Redesign the Cockpit multi-window system with SpatialVoice design language, pseudo-spatial head-tracking, and full AvanueUI v5.1 tokenization — implemented **KMP-first** so all layout logic, state machines, and Compose UI composables live in `commonMain` for Android + Desktop + future iOS.

**Branch:** `IosVoiceOS-Development`
**Module:** `Modules/Cockpit/`
**Estimated:** ~2,200 lines across 5 phases

---

## Architectural Principles

### 1. KMP-First: Maximize commonMain

**Current state:** All composables in `androidMain`, models in `commonMain`.
**Target state:** ALL layout composables + state machines + ViewModel logic in `commonMain`. Only platform-specific implementations (IMU sensor access, Android WebView, CameraX) remain in `androidMain`.

**Why:** Desktop (JVM) target already exists in `build.gradle.kts`. Putting layout composables in commonMain means they work on Android AND Desktop without duplication. Future iOS support is one `iosMain` away.

**How:** Add the JetBrains Compose Multiplatform plugin (`alias(libs.plugins.compose)`) to Cockpit's `build.gradle.kts` — same pattern as `Modules/AvanueUI/build.gradle.kts`. This unlocks `compose.runtime`, `compose.foundation`, `compose.material3` in commonMain.

### 2. AvanueUI v5.1 Tokenization

ALL UI code uses the three-axis theme system:

| Axis | Enum | Default | Purpose |
|------|------|---------|---------|
| Color Palette | `AvanueColorPalette` | HYDRA | SOL/LUNA/TERRA/HYDRA |
| Material Style | `MaterialMode` | Water | Glass/Water/Cupertino/MountainView |
| Appearance | `AppearanceMode` | Auto | Light/Dark/Auto |

- Colors: `AvanueTheme.colors.*` (NEVER `MaterialTheme.colorScheme.*`)
- Glass effects: `AvanueTheme.glass.*` + `Modifier.glass()`
- Water effects: `AvanueTheme.water.*` + `Modifier.waterEffect()`
- Unified components: `AvanueCard`, `AvanueButton`, `AvanueChip`, etc. (auto-adapt to MaterialMode)
- XR glass: `palette.colorsXR` for AR display profiles
- Touch targets: `DisplayUtils.minTouchTarget` (auto-scales per DisplayProfile)

### 3. Theme-Agnostic Architecture

The Cockpit renders correctly in ALL 32 theme combinations (4 palettes x 4 styles x light/dark). Layout composables use AvanueUI tokens exclusively — no hardcoded colors, no MaterialTheme references, no Color(0xFF...) literals.

When `config.theme.json` is set to `native`, the same layouts work with platform Material3 because AvanueUI tokens bridge to M3 colorScheme internally.

---

## Phase 0: Enable KMP Compose in Cockpit (~30 lines)

### Task 0.1: Update `build.gradle.kts`

Add JB Compose Multiplatform plugin and move Compose deps to commonMain:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)          // ← ADD THIS (JB Compose Multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Existing deps...

                // Compose Multiplatform (shared across Android + Desktop)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)

                // AvanueUI v5.1 — Theme + Unified Components (KMP)
                implementation(project(":Modules:AvanueUI"))
            }
        }

        val androidMain by getting {
            dependencies {
                // KEEP: Platform-specific content renderers
                implementation(project(":Modules:WebAvanue"))
                implementation(project(":Modules:PDFAvanue"))
                // ... (other content modules stay here)

                // KEEP: Platform-specific hardware
                implementation(project(":Modules:DeviceManager"))

                // REMOVE from here (moved to commonMain):
                // - compose.ui, compose.material3, compose.foundation
                // - project(":Modules:AvanueUI")
            }
        }
    }
}
```

### Task 0.2: Verify Build

After adding the plugin, run a sync + build to confirm all existing commonMain models still compile and the androidMain composables can still import from commonMain. No functional changes in this task.

### Files Modified:
- `Modules/Cockpit/build.gradle.kts`

---

## Phase 1: Models & State Machines (commonMain, ~250 lines)

All new data models and pure-logic state machines go in `commonMain` for full cross-platform sharing.

### Task 1.1: Add New Layout Modes

**File:** `Modules/Cockpit/src/commonMain/.../model/LayoutMode.kt`

Add 3 new enum values:

```kotlin
@Serializable
enum class LayoutMode {
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
    CAROUSEL,       // NEW — curved 3D swipe-through
    SPATIAL_DICE,   // NEW — 4 corners + 1 center (dice-5 pattern)
    GALLERY;        // NEW — media-only filtered grid

    companion object {
        val DEFAULT = COCKPIT

        fun fromString(value: String): LayoutMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }

        /** Layouts that support spatial canvas overlay */
        val SPATIAL_CAPABLE = setOf(FREEFORM, COCKPIT, MOSAIC, T_PANEL)

        /** Media-type content IDs for gallery filtering */
        val GALLERY_CONTENT_TYPES = setOf("image", "video", "camera", "screen_cast")
    }
}
```

### Task 1.2: Create SpatialPosition Model

**File:** `Modules/Cockpit/src/commonMain/.../model/SpatialPosition.kt` (NEW)

```kotlin
@Serializable
data class SpatialPosition(
    val gridX: Int = 0,   // -1 (left), 0 (center), 1 (right)
    val gridY: Int = 0    // -1 (up), 0 (center), 1 (down)
) {
    val isCenter: Boolean get() = gridX == 0 && gridY == 0
    val label: String get() = when {
        gridX == 0 && gridY == 0 -> "Center"
        gridX == -1 && gridY == 0 -> "Left"
        gridX == 1 && gridY == 0 -> "Right"
        gridX == 0 && gridY == -1 -> "Above"
        gridX == 0 && gridY == 1 -> "Below"
        gridX == -1 && gridY == -1 -> "Top-Left"
        gridX == 1 && gridY == -1 -> "Top-Right"
        gridX == -1 && gridY == 1 -> "Bottom-Left"
        gridX == 1 && gridY == 1 -> "Bottom-Right"
        else -> "($gridX,$gridY)"
    }

    companion object {
        val CENTER = SpatialPosition(0, 0)
        val LEFT = SpatialPosition(-1, 0)
        val RIGHT = SpatialPosition(1, 0)
        val ABOVE = SpatialPosition(0, -1)
        val BELOW = SpatialPosition(0, 1)
    }
}
```

### Task 1.3: Update CockpitFrame with Spatial Position

**File:** `Modules/Cockpit/src/commonMain/.../model/CockpitFrame.kt`

Add `spatialPosition` field:

```kotlin
@Serializable
data class CockpitFrame(
    val id: String,
    val sessionId: String,
    val title: String = "",
    val content: FrameContent,
    val state: FrameState = FrameState(),
    val spatialPosition: SpatialPosition = SpatialPosition.CENTER, // NEW
    val createdAt: String = "",
    val updatedAt: String = "",
)
```

### Task 1.4: Update CockpitSession with Background

**File:** `Modules/Cockpit/src/commonMain/.../model/CockpitSession.kt`

Add optional background:

```kotlin
@Serializable
data class CockpitSession(
    // ... existing fields ...
    val backgroundUri: String? = null, // NEW — optional session background
)
```

### Task 1.5: Create CommandBarState Enum

**File:** `Modules/Cockpit/src/commonMain/.../model/CommandBarState.kt` (NEW)

Hierarchical state machine for the context-aware command bar:

```kotlin
@Serializable
enum class CommandBarState {
    // Level 0: Root
    MAIN,

    // Level 1: Category menus
    ADD_FRAME,
    LAYOUT_PICKER,
    FRAME_ACTIONS,

    // Level 2: Content-specific actions
    WEB_ACTIONS,
    PDF_ACTIONS,
    IMAGE_ACTIONS,
    VIDEO_ACTIONS,
    NOTE_ACTIONS,
    CAMERA_ACTIONS,

    // Level 2: Navigation
    SCROLL_COMMANDS,
    ZOOM_COMMANDS,
    SPATIAL_COMMANDS;

    /** Parent state for back navigation */
    val parent: CommandBarState? get() = when (this) {
        MAIN -> null
        ADD_FRAME, LAYOUT_PICKER, FRAME_ACTIONS -> MAIN
        WEB_ACTIONS, PDF_ACTIONS, IMAGE_ACTIONS,
        VIDEO_ACTIONS, NOTE_ACTIONS, CAMERA_ACTIONS -> FRAME_ACTIONS
        SCROLL_COMMANDS, ZOOM_COMMANDS, SPATIAL_COMMANDS -> MAIN
    }

    /** Whether this state shows content-specific actions */
    val isContentSpecific: Boolean get() = this in setOf(
        WEB_ACTIONS, PDF_ACTIONS, IMAGE_ACTIONS,
        VIDEO_ACTIONS, NOTE_ACTIONS, CAMERA_ACTIONS
    )

    companion object {
        /** Map content type ID to the appropriate command bar state */
        fun forContentType(typeId: String): CommandBarState = when (typeId) {
            "web" -> WEB_ACTIONS
            "pdf" -> PDF_ACTIONS
            "image" -> IMAGE_ACTIONS
            "video" -> VIDEO_ACTIONS
            "note", "voice_note" -> NOTE_ACTIONS
            "camera" -> CAMERA_ACTIONS
            else -> FRAME_ACTIONS
        }
    }
}
```

### Task 1.6: Create ContentAccent Mapping

**File:** `Modules/Cockpit/src/commonMain/.../model/ContentAccent.kt` (NEW)

Maps content types to semantic color identifiers (resolved at Compose-time using AvanueTheme):

```kotlin
/**
 * Semantic accent identifier for each content type.
 * Resolved to actual Color via AvanueTheme at render time.
 */
enum class ContentAccent {
    INFO,       // Web — blue
    ERROR,      // PDF — red
    PRIMARY,    // Image — sapphire
    SECONDARY,  // Video — purple
    SUCCESS,    // Note — green
    WARNING,    // Camera — amber
    TERTIARY;   // Other — neutral

    companion object {
        fun forContentType(typeId: String): ContentAccent = when (typeId) {
            "web" -> INFO
            "pdf" -> ERROR
            "image" -> PRIMARY
            "video" -> SECONDARY
            "note", "voice_note" -> SUCCESS
            "camera" -> WARNING
            else -> TERTIARY
        }
    }
}
```

### Task 1.7: Add Spatial Constants

**File:** `Modules/Cockpit/src/commonMain/.../CockpitConstants.kt`

Add spatial grid and command bar constants:

```kotlin
object CockpitConstants {
    // ... existing constants ...

    // ── Spatial Grid ────────────────────────────────────────
    /** Default spatial grid dimensions (3x3 = center + 8 directions) */
    const val SPATIAL_GRID_COLUMNS = 3
    const val SPATIAL_GRID_ROWS = 3

    /** Degrees of head rotation per screen-width of panning */
    const val SPATIAL_DEGREES_PER_SCREEN = 30f

    /** Deadzone in degrees — no movement within this range */
    const val SPATIAL_DEADZONE_DEGREES = 5f

    /** Spatial canvas lerp factor for smooth transitions (0..1) */
    const val SPATIAL_LERP_FACTOR = 0.15f

    // ── Command Bar ────────────────────────────────────────
    /** Command bar height in dp (tablet/phone) */
    const val COMMAND_BAR_HEIGHT = 56f

    /** Command bar height in dp (glass displays) */
    const val COMMAND_BAR_HEIGHT_GLASS = 48f

    /** Max buttons shown on glass command bar */
    const val COMMAND_BAR_MAX_GLASS_BUTTONS = 5

    // ── Carousel ────────────────────────────────────────────
    /** Scale factor for adjacent carousel frames */
    const val CAROUSEL_ADJACENT_SCALE = 0.80f

    /** Y-axis rotation in degrees for adjacent carousel frames */
    const val CAROUSEL_ROTATION_DEGREES = 15f

    /** Alpha for adjacent carousel frames */
    const val CAROUSEL_ADJACENT_ALPHA = 0.6f

    // ── Dice-5 ──────────────────────────────────────────────
    /** Center frame weight in Dice-5 layout */
    const val DICE_CENTER_WEIGHT = 0.55f

    /** Corner frame weight in Dice-5 layout */
    const val DICE_CORNER_WEIGHT = 0.45f
}
```

### Files Modified/Created:
- MODIFY: `commonMain/.../model/LayoutMode.kt`
- MODIFY: `commonMain/.../model/CockpitFrame.kt`
- MODIFY: `commonMain/.../model/CockpitSession.kt`
- MODIFY: `commonMain/.../CockpitConstants.kt`
- CREATE: `commonMain/.../model/SpatialPosition.kt`
- CREATE: `commonMain/.../model/CommandBarState.kt`
- CREATE: `commonMain/.../model/ContentAccent.kt`

---

## Phase 2: KMP Layout Composables (commonMain, ~600 lines)

All layout composables move to commonMain. They use only Compose Multiplatform APIs + AvanueUI tokens — zero platform-specific code.

### Task 2.1: CarouselLayout (NEW)

**File:** `Modules/Cockpit/src/commonMain/.../ui/CarouselLayout.kt` (NEW)

Center frame full-size, left/right adjacent frames scaled + rotated for pseudo-3D perspective:

- Uses `HorizontalPager` from `androidx.compose.foundation.pager`
- `graphicsLayer { rotationY, scaleX, scaleY, alpha }` for 3D effect
- Frame number badge: "Frame 2 of 5" in top-center
- On phone: primary layout mode (best for small screens)
- On tablet: center ~70% width, side previews ~15% each
- On glass: single frame only, voice "next/previous frame" to cycle
- All colors via `AvanueTheme.colors.*`

### Task 2.2: SpatialDiceLayout (NEW)

**File:** `Modules/Cockpit/src/commonMain/.../ui/SpatialDiceLayout.kt` (NEW)

Dice-5 pattern: 4 corner windows + 1 large center:

```
┌──────┬────────────┬──────┐
│  TL  │            │  TR  │
│ ~15% │            │ ~15% │
├──────┤   CENTER   ├──────┤
│      │   ~40%     │      │
│      │ (primary)  │      │
├──────┤            ├──────┤
│  BL  │            │  BR  │
│ ~15% │            │ ~15% │
└──────┴────────────┴──────┘
```

- Center: `CockpitConstants.DICE_CENTER_WEIGHT`, fully interactive, selected by default
- Corners: `CockpitConstants.DICE_CORNER_WEIGHT`, show content thumbnails
- Tap corner to swap with center (callback to ViewModel)
- Uses `AvanueTheme.colors.border` for inactive borders, `AvanueTheme.colors.primary` for selected
- Glass: center only, voice "show top left" to swap

### Task 2.3: GalleryLayout (NEW)

**File:** `Modules/Cockpit/src/commonMain/.../ui/GalleryLayout.kt` (NEW)

Media-only filtered grid (Image + Video + Camera + ScreenCast):

- Filters frames by `LayoutMode.GALLERY_CONTENT_TYPES`
- Responsive grid: 2 cols (phone), 3-4 cols (tablet), 1 col (glass)
- Uses `LazyVerticalGrid` with `GridCells.Adaptive`
- Each cell renders via `FrameWindow` with aspect-ratio-preserving content
- Non-media frames hidden but NOT removed (restored when leaving gallery)
- Uses `AvanueTheme.colors.surface` for card backgrounds

### Task 2.4: WorkflowLayout Redesign

**File:** `Modules/Cockpit/src/commonMain/.../ui/WorkflowSidebar.kt` (NEW)

Split into sidebar component + main content area:

**Sidebar (30% width on tablet):**
- Numbered step list (vertical, tappable)
- Each step: step number badge, frame title, content type icon, completion indicator
- Badge uses `AvanueTheme.colors.primary` background, `AvanueTheme.colors.onPrimary` text
- Active step highlighted with `AvanueTheme.colors.primaryContainer`
- Toggle position: left/right
- Toggle visibility: hide/show

**Main content area (70% width):**
- Selected step's frame at full size

**Phone adaptation:** Sidebar becomes bottom sheet (list of steps, swipe up)
**Glass adaptation:** Voice navigation only ("step 3", "next step")

### Task 2.5: Update LayoutEngine

**File:** `Modules/Cockpit/src/commonMain/.../ui/LayoutEngine.kt` (MOVE + MODIFY)

Move `LayoutEngine.kt` from androidMain to commonMain. Add `when` branches for the 3 new layout modes:

```kotlin
LayoutMode.CAROUSEL -> CarouselLayout(...)
LayoutMode.SPATIAL_DICE -> SpatialDiceLayout(...)
LayoutMode.GALLERY -> GalleryLayout(...)
```

**Migration plan:** The existing layout composables (GridLayout, SplitLayout, FlightDeckLayout, etc.) are pure Compose with no Android-specific APIs. They can move to commonMain as-is. The only change is adding the new branches.

### Files Modified/Created:
- MOVE+MODIFY: `androidMain/.../ui/LayoutEngine.kt` → `commonMain/.../ui/LayoutEngine.kt`
- CREATE: `commonMain/.../ui/CarouselLayout.kt`
- CREATE: `commonMain/.../ui/SpatialDiceLayout.kt`
- CREATE: `commonMain/.../ui/GalleryLayout.kt`
- CREATE: `commonMain/.../ui/WorkflowSidebar.kt`

---

## Phase 3: Command Bar (commonMain UI + logic, ~400 lines)

### Task 3.1: CommandBar Composable

**File:** `Modules/Cockpit/src/commonMain/.../ui/CommandBar.kt` (NEW)

Bottom-docked action bar with context-aware command sets:

- Fixed bottom bar, `CockpitConstants.COMMAND_BAR_HEIGHT` dp
- Horizontal scrollable row of action chips using `AvanueChip` from AvanueUI
- Each button: Material icon + short label
- Back button returns to `CommandBarState.parent`
- Active state: `AvanueTheme.colors.primary` pill background
- Inactive: `AvanueTheme.colors.surface` background
- Glass mode: max `COMMAND_BAR_MAX_GLASS_BUTTONS` large buttons
- Voice hint: subtitle text below bar shows last recognized command (fades after 2s)
- Voice-visual sync: matching button briefly flashes on voice command

**State management:**
- `CommandBarState` drives which buttons are shown
- Transitions managed by ViewModel (or composable-local state)
- Content-specific states auto-selected when a frame is focused

### Task 3.2: Wire Command Bar into CockpitScreen

**File:** `Modules/Cockpit/src/commonMain/.../ui/CockpitScreen.kt` (MOVE + MODIFY)

Move CockpitScreen to commonMain (it only uses Compose APIs + AvanueTheme). Replace the current dropdown menus (layout picker, add frame) with the CommandBar:

- Remove `showLayoutMenu` DropdownMenu
- Remove `showAddFrameMenu` DropdownMenu
- Add `CommandBar` at bottom of Column
- Keep TopAppBar with session name (use `AvanueTheme.colors.surface` or Transparent)
- SpatialVoice background gradient preserved: `verticalGradient(background, surface.copy(0.6f), background)`

### Task 3.3: Update CockpitScreen Layout Mode Icons

Add icons for new layout modes:

```kotlin
LayoutMode.CAROUSEL -> Icons.Default.ViewCarousel
LayoutMode.SPATIAL_DICE -> Icons.Default.Casino  // dice icon
LayoutMode.GALLERY -> Icons.Default.PhotoLibrary  // gallery icon
```

### Files Modified/Created:
- CREATE: `commonMain/.../ui/CommandBar.kt`
- MOVE+MODIFY: `androidMain/.../ui/CockpitScreen.kt` → `commonMain/.../ui/CockpitScreen.kt`

**Note:** CockpitScreen currently imports `ContentRenderer` which IS Android-specific (uses AndroidView). Solution: make `frameContent` a composable lambda parameter (already is!) — the Android app passes `ContentRenderer` from androidMain. CockpitScreen itself doesn't import ContentRenderer directly since it receives frameContent as a slot.

Wait — looking again, CockpitScreen DOES import ContentRenderer directly on line 47. **Solution:** Extract ContentRenderer call into the calling site (the app's navigation/DI layer), and have CockpitScreen only take the `frameContent` lambda. This is actually the cleaner architecture.

**Revised approach:** Create a new `CockpitScreenContent.kt` in commonMain that takes a `frameContent: @Composable (CockpitFrame) -> Unit` slot. The existing `CockpitScreen.kt` in androidMain becomes a thin wrapper that provides the `ContentRenderer` implementation.

---

## Phase 4: Spatial Canvas & Head Tracking (~500 lines)

### Task 4.1: Spatial Orientation Interface (commonMain)

**File:** `Modules/Cockpit/src/commonMain/.../spatial/ISpatialOrientationSource.kt` (NEW)

KMP interface for orientation data — platform implementations provide the actual sensor:

```kotlin
interface ISpatialOrientationSource {
    /** Flow of (yaw, pitch) in degrees. Null emissions mean sensor unavailable. */
    val orientationFlow: Flow<SpatialOrientation>

    /** Start tracking with a consumer ID */
    fun startTracking(consumerId: String): Boolean

    /** Stop tracking */
    fun stopTracking(consumerId: String)

    /** Whether tracking is currently active */
    fun isTracking(): Boolean
}

data class SpatialOrientation(
    val yawDegrees: Float,    // Horizontal head turn
    val pitchDegrees: Float,  // Vertical head tilt
    val timestamp: Long
)
```

### Task 4.2: Android IMU Implementation (androidMain)

**File:** `Modules/Cockpit/src/androidMain/.../spatial/AndroidSpatialOrientationSource.kt` (NEW)

Wraps `IMUPublicAPI` from DeviceManager:

```kotlin
class AndroidSpatialOrientationSource(context: Context) : ISpatialOrientationSource {
    private val imuAPI = IMUPublicAPI(context)

    override val orientationFlow: Flow<SpatialOrientation> =
        imuAPI.orientationFlow.map { orientation ->
            val euler = orientation.eulerAngles.toDegrees()
            SpatialOrientation(
                yawDegrees = euler.yaw,
                pitchDegrees = euler.pitch,
                timestamp = orientation.timestamp
            )
        }

    override fun startTracking(consumerId: String): Boolean = imuAPI.startTracking()
    override fun stopTracking(consumerId: String) = imuAPI.stopTracking()
    override fun isTracking(): Boolean = imuAPI.isTracking()
}
```

### Task 4.3: Desktop Stub Implementation (desktopMain)

**File:** `Modules/Cockpit/src/desktopMain/.../spatial/DesktopSpatialOrientationSource.kt` (NEW)

No IMU on desktop — uses mouse drag or keyboard as fallback:

```kotlin
class DesktopSpatialOrientationSource : ISpatialOrientationSource {
    private val _orientationFlow = MutableStateFlow(SpatialOrientation(0f, 0f, 0L))
    override val orientationFlow: Flow<SpatialOrientation> = _orientationFlow

    /** Desktop simulates orientation via manual input */
    fun setOrientation(yaw: Float, pitch: Float) {
        _orientationFlow.value = SpatialOrientation(yaw, pitch, System.currentTimeMillis())
    }

    override fun startTracking(consumerId: String) = true
    override fun stopTracking(consumerId: String) {}
    override fun isTracking() = true
}
```

### Task 4.4: SpatialViewportController (commonMain)

**File:** `Modules/Cockpit/src/commonMain/.../spatial/SpatialViewportController.kt` (NEW)

Consumes `ISpatialOrientationSource` and maps orientation to viewport offset:

- Maps yaw → horizontal canvas pan (sensitivity: `SPATIAL_DEGREES_PER_SCREEN`)
- Maps pitch → vertical canvas pan
- Deadzone: ±`SPATIAL_DEADZONE_DEGREES` from center = no movement
- Smooth interpolation: `lerp(currentOffset, targetOffset, SPATIAL_LERP_FACTOR)`
- Snap-to-grid: optional snapping to nearest grid cell center
- "Center view" reset: returns offset to (0, 0) with spring animation
- Outputs: `StateFlow<Offset>` for canvas translationX/translationY

### Task 4.5: SpatialCanvas Composable (commonMain)

**File:** `Modules/Cockpit/src/commonMain/.../ui/SpatialCanvas.kt` (NEW)

Wrapper composable that applies spatial viewport translation:

- Large `Box` with `graphicsLayer { translationX, translationY }` driven by `SpatialViewportController`
- Edge indicators: subtle gradient arrows at screen edges when content exists off-screen
- Minimap overlay (optional): small 3x3 grid showing dots for locked frames
- Only the visible viewport region renders full content; off-screen positions render thumbnails
- AvanueUI tokens for all colors: edge indicators use `AvanueTheme.colors.primary.copy(alpha = 0.3f)`

### Task 4.6: Lock/Unlock ViewModel Logic

**File:** `Modules/Cockpit/src/commonMain/.../viewmodel/CockpitSpatialExt.kt` (NEW)

Extension functions for CockpitViewModel (or a separate SpatialManager class):

```kotlin
fun CockpitViewModel.lockFrame(frameId: String, position: SpatialPosition)
fun CockpitViewModel.unlockFrame(frameId: String)
fun CockpitViewModel.centerView()
fun CockpitViewModel.showOverview()  // zoom out to see entire grid
```

### Task 4.7: Phone/Tablet Fallback (commonMain)

In SpatialCanvas.kt:

- **Phone:** Swipe gestures replace head tracking (`detectDragGestures` to control viewport offset)
- **Tablet:** Edge peek — locked frames show as 20dp strip on screen edges, drag to reveal
- **Both:** Minimap toggleable via CommandBar
- Detection: Use `AvanueTheme.displayProfile` to determine PHONE/TABLET/GLASS

### Files Modified/Created:
- CREATE: `commonMain/.../spatial/ISpatialOrientationSource.kt`
- CREATE: `commonMain/.../spatial/SpatialOrientation.kt` (data class)
- CREATE: `commonMain/.../spatial/SpatialViewportController.kt`
- CREATE: `commonMain/.../ui/SpatialCanvas.kt`
- CREATE: `commonMain/.../viewmodel/CockpitSpatialExt.kt`
- CREATE: `androidMain/.../spatial/AndroidSpatialOrientationSource.kt`
- CREATE: `desktopMain/.../spatial/DesktopSpatialOrientationSource.kt`

---

## Phase 5: Frame Chrome & Visual Polish (commonMain, ~300 lines)

### Task 5.1: Enhanced FrameWindow

**File:** `Modules/Cockpit/src/commonMain/.../ui/FrameWindow.kt` (MOVE + MODIFY)

Move FrameWindow from androidMain to commonMain. Enhancements:

1. **Themed border colors per content type:**
   - Resolve `ContentAccent` → actual Color via AvanueTheme at render time
   - `ContentAccent.INFO` → `AvanueTheme.colors.info`
   - `ContentAccent.ERROR` → `AvanueTheme.colors.error`
   - `ContentAccent.PRIMARY` → `AvanueTheme.colors.primary`
   - etc.
   - Selected frame uses accent color; unselected uses `AvanueTheme.colors.border`

2. **Glass title bar:**
   - Use `Modifier.glass()` from AvanueUI for title bar background (when `MaterialMode == Glass`)
   - Cupertino: hairline border, 0dp elevation
   - MountainView: standard M3 tonal elevation
   - Water: `Modifier.waterEffect()` on title bar

3. **Frame number badge:**
   - Show "Frame N" in title bar (e.g., "Frame 2 : Web Browser")
   - Badge uses `AvanueTheme.colors.primaryContainer` background

4. **Spatial lock indicator:**
   - When `frame.spatialPosition.isCenter == false`, show directional arrow icon (←↑→↓) in title bar
   - Arrow tinted with `AvanueTheme.colors.warning`

5. **Content type icons:**
   - Replace text labels ("WEB", "PDF") with Material icons:
   - Web → `Icons.Default.Language`
   - PDF → `Icons.Default.PictureAsPdf`
   - Image → `Icons.Default.Image`
   - Video → `Icons.Default.VideoLibrary`
   - Note → `Icons.Default.StickyNote2`
   - Camera → `Icons.Default.PhotoCamera`

### Task 5.2: Move Remaining UI to commonMain

Move these pure-Compose files from androidMain to commonMain:

- `FreeformCanvas.kt` — pure Compose (offset, gesture detection). No Android APIs.
- `MinimizedTaskbar.kt` — pure Compose (Row, Text, clickable). No Android APIs.

### Task 5.3: Android-Only ContentRenderer Wrapper

Keep `ContentRenderer.kt` in androidMain (it uses `AndroidView` for WebView, CameraX, etc.).

Create a thin expect/actual for the app integration point:

**commonMain:** `expect` that the platform provides a `frameContent` composable lambda
**androidMain:** `actual` that delegates to `ContentRenderer`

Or simpler: the app-level code (in `apps/avanues/`) passes `ContentRenderer` as the `frameContent` lambda to `CockpitScreen`. No expect/actual needed — just dependency injection via composable parameter.

### Files Modified/Created:
- MOVE+MODIFY: `androidMain/.../ui/FrameWindow.kt` → `commonMain/.../ui/FrameWindow.kt`
- MOVE: `androidMain/.../ui/FreeformCanvas.kt` → `commonMain/.../ui/FreeformCanvas.kt`
- MOVE: `androidMain/.../ui/MinimizedTaskbar.kt` → `commonMain/.../ui/MinimizedTaskbar.kt`
- KEEP in androidMain: `ContentRenderer.kt`, `AndroidCockpitRepository.kt`

---

## Phase 6: Device-Responsive Adaptation (commonMain, ~200 lines)

### Task 6.1: Layout Mode Availability by DisplayProfile

**File:** `Modules/Cockpit/src/commonMain/.../ui/LayoutModeResolver.kt` (NEW)

Determines which layout modes are available and optimal per device:

| Layout Mode | Phone | Tablet | AR Glass |
|------------|-------|--------|----------|
| CAROUSEL | **Default** | Yes | Voice-only |
| GRID | 1-2 cols | 2-3 cols | 1 col |
| SPLIT_L/R | Vertical stack | Side-by-side | N/A |
| COCKPIT | 2-frame limit | Full 6-slot | 2-frame |
| SPATIAL_DICE | Swipe corners | Full layout | Voice swap |
| GALLERY | 2 cols | 3-4 cols | 1 col scroll |
| FREEFORM | Simplified | Full drag/resize | N/A |
| FULLSCREEN | Yes | Yes | **Default** |

```kotlin
object LayoutModeResolver {
    fun defaultMode(profile: DisplayProfile): LayoutMode = when {
        profile.isGlass -> LayoutMode.FULLSCREEN
        profile == DisplayProfile.PHONE -> LayoutMode.CAROUSEL
        else -> LayoutMode.COCKPIT
    }

    fun isAvailable(mode: LayoutMode, profile: DisplayProfile): Boolean
    fun maxFrames(mode: LayoutMode, profile: DisplayProfile): Int
}
```

### Task 6.2: XR Glass Adaptations

When `displayProfile.isGlass`:
- Colors: use `palette.colorsXR` (transparent bg, boosted luminance)
- Command bar: max 5 buttons, enlarged touch targets via `DisplayUtils.minTouchTarget`
- Frame chrome: minimal — no resize handle, thin 1dp border, no glass/water effects
- Spatial canvas: head-tracking enabled by default
- Default layout: FULLSCREEN with voice switching

### Task 6.3: Phone Adaptations

When `displayProfile == PHONE`:
- Default layout: CAROUSEL (swipe between frames)
- Spatial canvas: swipe to navigate (no head tracking)
- Command bar: collapsible (swipe up to expand)
- COCKPIT/MOSAIC: max 3 frames
- Workflow sidebar → bottom sheet

### Files Modified/Created:
- CREATE: `commonMain/.../ui/LayoutModeResolver.kt`
- MODIFY: Various layout composables to read `AvanueTheme.displayProfile`

---

## File Summary

### commonMain (NEW + MOVED — KMP cross-platform)

| File | Type | Lines Est. |
|------|------|-----------|
| `model/SpatialPosition.kt` | NEW | 40 |
| `model/CommandBarState.kt` | NEW | 60 |
| `model/ContentAccent.kt` | NEW | 30 |
| `model/LayoutMode.kt` | MODIFY | +20 |
| `model/CockpitFrame.kt` | MODIFY | +5 |
| `model/CockpitSession.kt` | MODIFY | +5 |
| `CockpitConstants.kt` | MODIFY | +40 |
| `ui/LayoutEngine.kt` | MOVE+MODIFY | ~720 (existing + 3 new branches) |
| `ui/FrameWindow.kt` | MOVE+MODIFY | ~280 (existing + enhancements) |
| `ui/FreeformCanvas.kt` | MOVE | ~253 (as-is) |
| `ui/MinimizedTaskbar.kt` | MOVE | ~90 (as-is) |
| `ui/CockpitScreenContent.kt` | NEW | ~200 |
| `ui/CarouselLayout.kt` | NEW | ~150 |
| `ui/SpatialDiceLayout.kt` | NEW | ~120 |
| `ui/GalleryLayout.kt` | NEW | ~80 |
| `ui/WorkflowSidebar.kt` | NEW | ~130 |
| `ui/CommandBar.kt` | NEW | ~250 |
| `ui/SpatialCanvas.kt` | NEW | ~180 |
| `ui/LayoutModeResolver.kt` | NEW | ~60 |
| `spatial/ISpatialOrientationSource.kt` | NEW | ~30 |
| `spatial/SpatialViewportController.kt` | NEW | ~150 |
| `viewmodel/CockpitSpatialExt.kt` | NEW | ~50 |

### androidMain (KEEP — platform-specific only)

| File | Type | Lines Est. |
|------|------|-----------|
| `content/ContentRenderer.kt` | KEEP | ~278 (unchanged) |
| `repository/AndroidCockpitRepository.kt` | KEEP | unchanged |
| `ui/CockpitScreen.kt` | MODIFY | thin wrapper providing ContentRenderer |
| `spatial/AndroidSpatialOrientationSource.kt` | NEW | ~40 |

### desktopMain (NEW — platform-specific only)

| File | Type | Lines Est. |
|------|------|-----------|
| `spatial/DesktopSpatialOrientationSource.kt` | NEW | ~30 |

### build.gradle.kts

| File | Type | Change |
|------|------|--------|
| `build.gradle.kts` | MODIFY | Add `compose` plugin, move Compose deps to commonMain |

---

## Implementation Order

### Batch 1: Build Config + Models (Phase 0 + 1)
1. Update `build.gradle.kts` — add JB Compose plugin, restructure deps
2. Verify build compiles
3. Add LayoutMode entries (CAROUSEL, SPATIAL_DICE, GALLERY)
4. Create SpatialPosition, CommandBarState, ContentAccent models
5. Update CockpitFrame (add spatialPosition) and CockpitSession (add backgroundUri)
6. Update CockpitConstants with spatial/carousel/command-bar constants

### Batch 2: Move Existing UI to commonMain (Phase 2 + 5 partial)
7. Move FrameWindow.kt to commonMain (enhance with themed borders, icons, glass title bar)
8. Move FreeformCanvas.kt to commonMain (no changes needed)
9. Move MinimizedTaskbar.kt to commonMain (no changes needed)
10. Move LayoutEngine.kt to commonMain

### Batch 3: New Layouts (Phase 2)
11. Create CarouselLayout.kt
12. Create SpatialDiceLayout.kt
13. Create GalleryLayout.kt
14. Create WorkflowSidebar.kt (redesigned workflow)
15. Wire new layouts into LayoutEngine

### Batch 4: Command Bar (Phase 3)
16. Create CommandBar.kt composable
17. Create CockpitScreenContent.kt in commonMain
18. Update CockpitScreen.kt in androidMain as thin wrapper
19. Wire command bar state transitions

### Batch 5: Spatial Canvas (Phase 4)
20. Create ISpatialOrientationSource interface (commonMain)
21. Create AndroidSpatialOrientationSource (androidMain)
22. Create DesktopSpatialOrientationSource (desktopMain)
23. Create SpatialViewportController (commonMain)
24. Create SpatialCanvas.kt composable (commonMain)
25. Add lock/unlock logic to ViewModel
26. Phone/tablet fallback gestures

### Batch 6: Polish (Phase 5 + 6)
27. LayoutModeResolver for device-specific behavior
28. XR glass adaptations
29. Phone carousel default + bottom sheet adaptations
30. Touch target enforcement

---

## Verification Checklist

- [ ] Build compiles on Android target after Phase 0
- [ ] Build compiles on Desktop target after Phase 0
- [ ] All 13 layout modes render correctly in emulator
- [ ] Carousel: swipe between frames, verify 3D perspective on side frames
- [ ] Dice-5: 5 frames display correctly, tap corner to swap with center
- [ ] Gallery: only media frames shown, non-media hidden
- [ ] Workflow sidebar: step list on left, content on right, tap to navigate
- [ ] Command bar: state transitions (MAIN → ADD_FRAME → back), content-specific commands
- [ ] Spatial lock: lock frame left, verify it disappears from center, swipe/head-turn to find it
- [ ] Frame chrome: themed borders per content type, frame numbers, content icons
- [ ] Glass profile: XR colors, minimal chrome, enlarged touch targets
- [ ] Phone profile: carousel default, command bar collapsible
- [ ] AvanueUI theme: switch palette (SOL/LUNA/TERRA/HYDRA) — all UI updates
- [ ] AvanueUI style: switch MaterialMode (Glass/Water/Cupertino/MountainView) — components adapt
- [ ] AvanueUI appearance: toggle Light/Dark — colors invert properly
- [ ] Desktop target: builds and renders basic layout (no IMU, no content renderers)

---

## Dependencies

| Dependency | Module | Used For |
|-----------|--------|----------|
| AvanueUI v5.1 | `:Modules:AvanueUI` | Theme, tokens, unified components |
| DeviceManager | `:Modules:DeviceManager` | IMU orientation flow (Android only) |
| Foundation | `:Modules:Foundation` | StateFlow utilities, KMP abstractions |
| JB Compose | `compose.*` | Compose Multiplatform in commonMain |
| HorizontalPager | `compose.foundation` | Carousel layout |

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Moving files from androidMain → commonMain breaks imports | Do incrementally: move one file, build, fix, next file |
| JB Compose API differences from Jetpack Compose | Minimal — JB Compose wraps same APIs, same import paths |
| HorizontalPager availability in JB Compose | Available since Compose 1.5+ (project is on 1.7.3) |
| IMU integration adds latency to UI | SpatialViewportController uses lerp smoothing + deadzone |
| Too many layout modes confuse users | LayoutModeResolver hides unavailable modes per device |
| Desktop build breaks | Desktop has no content renderers — just layout. Use empty placeholder. |
