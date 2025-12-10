# Material 3 Extended Integration

Cockpit Android implementation using Material 3 and Jetpack Compose.

---

## Dependencies Added

### Compose BOM
- `androidx.compose:compose-bom:2024.02.00`

### Material 3 Core
- `androidx.compose.material3:material3`
- `androidx.compose.material3:material3-window-size-class`
- `androidx.compose.material:material-icons-extended`

### Compose UI
- `androidx.compose.ui:ui`
- `androidx.compose.ui:ui-graphics`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.compose.foundation:foundation`

### Android Integration
- `androidx.activity:activity-compose:1.8.2`
- `androidx.lifecycle:lifecycle-runtime-compose:2.7.0`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0`

---

## Theme Configuration

### CockpitTheme.kt
Maps Cockpit visual identity to Material 3 color system.

#### Color Scheme

| Cockpit Accent | Material 3 Role | Hex | Usage |
|----------------|-----------------|-----|-------|
| Blue | `primary` | #2196F3 | Communication windows |
| Orange | `secondary` | #FF9800 | Data/Analytics windows |
| Green | `tertiary` | #4CAF50 | Utility windows |
| Light Beige | `background` | #D4C5B0 | Background gradient start |
| Dark Tan | Surface variant | #B8A596 | Background gradient end |

#### Window Styles

| Style | Border | Shadow | Material 3 Mapping |
|-------|--------|--------|-------------------|
| **Minimal (Default)** | 20% dark, 1dp | Soft (12dp blur) | `outline` |
| **Glass** | 40% dark, 2dp + glow | Enhanced (20dp blur) | `outlineVariant` |

#### Usage

```kotlin
@Composable
fun MyCockpitScreen() {
    CockpitTheme(
        darkTheme = false,
        windowStyle = WindowStyle.MINIMAL
    ) {
        // Your composables here
        WindowDockCompose(...)
        ControlRailCompose(...)
    }
}
```

---

## Compose Components

### 1. WindowDockCompose
Bottom center dot indicators (macOS/Vision Pro style)

**Features:**
- Animated size/alpha on active state
- Circular indicators with scale animation
- Material 3 `primary` color for active
- Click handlers for window switching

**Icons:** N/A (uses Box with background)

**Usage:**
```kotlin
WindowDockCompose(
    dock = windowDock,
    onWindowClick = { windowId ->
        // Handle window switch
        // Voice: "Window 3"
    }
)
```

---

### 2. ControlRailCompose
Floating toolbar with system functions

**Features:**
- Material 3 `Surface` with elevation
- `extraLarge` shape (24dp corners)
- Material Icons Extended
- 5 default buttons

**Icons Mapping:**

| Action | Icon | Material Icons Extended |
|--------|------|------------------------|
| HOME | Home | `Icons.Default.Home` |
| WORKSPACE_SELECTOR | Dashboard | `Icons.Default.Dashboard` |
| LAYOUT_SELECTOR | Grid View | `Icons.Default.GridView` |
| VOICE_SETTINGS | Microphone | `Icons.Default.Mic` |
| SYSTEM_SETTINGS | Settings | `Icons.Default.Settings` |

**Usage:**
```kotlin
ControlRailCompose(
    rail = controlRail,
    onButtonClick = { action ->
        when (action) {
            RailAction.HOME -> navigateHome()
            RailAction.VOICE_SETTINGS -> openVoiceSettings()
            // Voice: "Go home", "Settings"
        }
    }
)
```

---

### 3. UtilityBeltCompose
Corner mini-panels (widgets)

**Features:**
- Material 3 `Surface` cards (56dp)
- Positioned in bottom corners
- Accent colors per widget type
- Material Icons Extended

**Icons Mapping:**

| Widget Type | Icon | Material Icons Extended |
|-------------|------|------------------------|
| MUSIC_PLAYER | Music Note | `Icons.Default.MusicNote` |
| TIMER | Timer | `Icons.Default.Timer` |
| BATTERY | Battery Full | `Icons.Default.BatteryFull` |
| NOTIFICATIONS | Notifications | `Icons.Default.Notifications` |
| WEATHER | Sun | `Icons.Default.WbSunny` |
| CALENDAR | Calendar | `Icons.Default.CalendarToday` |

**Color Mapping:**

| Widget Type | Material 3 Container |
|-------------|---------------------|
| MUSIC_PLAYER | `primaryContainer` (Blue) |
| TIMER | `secondaryContainer` (Orange) |
| BATTERY | `tertiaryContainer` (Green) |
| NOTIFICATIONS | `primaryContainer` (Blue) |
| WEATHER | `secondaryContainer` (Orange) |
| CALENDAR | `tertiaryContainer` (Green) |

**Usage:**
```kotlin
UtilityBeltCompose(
    belt = utilityBelt,
    onWidgetClick = { widgetId ->
        when (widgetId) {
            "music" -> toggleMusic()
            "timer" -> showTimer()
            // Voice: "Play music", "Set timer"
        }
    }
)
```

---

## Typography

### CockpitTypography (Type.kt)

| Style | Usage | Font Weight | Size |
|-------|-------|-------------|------|
| `titleLarge` | Window titles | SemiBold | 22sp |
| `titleMedium` | Control Rail buttons | Medium | 16sp |
| `titleSmall` | Utility Belt labels | Medium | 14sp |
| `bodyLarge` | Body text | Normal | 16sp |
| `labelLarge` | Labels | Medium | 14sp |

---

## Shapes

### CockpitShapes (Shape.kt)

| Shape | Corner Radius | Usage |
|-------|---------------|-------|
| `extraSmall` | 4dp | Small UI elements |
| `small` | 8dp | Minimal windows |
| `medium` | 12dp | Glass windows, widgets |
| `large` | 16dp | Large windows, theater mode |
| `extraLarge` | 24dp | Control Rail |

---

## Voice Integration with Material 3

### Voice Commands → Material 3 Animations

| Voice Command | Material 3 Effect |
|---------------|------------------|
| "Focus window 3" | WindowDock: Animate scale/alpha |
| "Show controls" | ControlRail: Fade in with elevation |
| "Glass borders" | Theme: Switch to `outlineVariant` |
| "Show utilities" | UtilityBelt: Slide in from corners |

### Spatial Audio → Visual Feedback

| Audio Event | Visual Response |
|-------------|-----------------|
| Window announced | Pulse glow on WindowDock indicator |
| Command confirmed | Ripple effect on ControlRail button |
| Error feedback | Shake animation on relevant component |

---

## Build Configuration

### build.gradle.kts

```kotlin
android {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}
```

---

## Example: Full Cockpit Screen

```kotlin
@Composable
fun CockpitWorkspaceScreen(
    viewModel: CockpitViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    CockpitTheme(
        windowStyle = uiState.windowStyle
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Background gradient
            GradientBackground()

            // Floating windows (rendered via ARCore/Sceneform)
            WindowRenderer(windows = uiState.windows)

            // UI Controls (overlay)
            ControlRailCompose(
                rail = uiState.controlRail,
                onButtonClick = viewModel::onControlAction,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            WindowDockCompose(
                dock = uiState.windowDock,
                onWindowClick = viewModel::focusWindow,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            UtilityBeltCompose(
                belt = uiState.utilityBelt,
                onWidgetClick = viewModel::onWidgetAction
            )
        }
    }
}
```

---

## Material 3 Extended Icons Used

### From `material-icons-extended`

- `Icons.Default.Home`
- `Icons.Default.Dashboard`
- `Icons.Default.GridView`
- `Icons.Default.Mic`
- `Icons.Default.Settings`
- `Icons.Default.MusicNote`
- `Icons.Default.Timer`
- `Icons.Default.BatteryFull`
- `Icons.Default.Notifications`
- `Icons.Default.WbSunny`
- `Icons.Default.CalendarToday`

All icons are vector drawables, fully scalable and themeable.

---

## Next Steps

1. **ARCore Integration**: Render windows in 3D space
2. **Window Content Rendering**: Virtual displays for Android apps
3. **Voice Integration**: Connect to VoiceOS bridge
4. **Gesture Handlers**: Touch, gaze, controller inputs
5. **Animations**: Material Motion for layout switching

---

**Version**: 1.0
**Updated**: 2025-12-08
**Compose BOM**: 2024.02.00
**Material 3**: Latest stable
