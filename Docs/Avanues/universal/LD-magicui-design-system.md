# MagicUI Universal Design System

Version: 1.0.0 | Date: 2025-11-28 | Platforms: Android, iOS, Web

---

## Design Principles

### Spatial Hierarchy (Z-Axis)

| Level | Component | Elevation | Blur |
|-------|-----------|-----------|------|
| 0 | Background | 0dp | 0px |
| 1 | Cards | 2dp | 0px |
| 2 | Windows | 8dp | 40px |
| 3 | Modals | 16dp | 40px |
| 4 | Toasts | 24dp | 24px |
| 5 | Tooltips | 32dp | 24px |

### Touch Targets

```yaml
minimum: 48dp × 48dp
buttons: 48dp height
list_items: 56dp height
dock_icons: 56dp × 56dp
```

### Nested Corner Radii

Formula: `inner_radius + padding = outer_radius`

Example: Container 16dp, Padding 8dp → Inner 8dp

---

## Ocean Theme Colors

### Base Palette

```kotlin
DeepOcean      = #0A1929
OceanDepth     = #0F172A
OceanMid       = #1E293B
OceanShallow   = #334155

CoralBlue      = #3B82F6  // Primary
TurquoiseCyan  = #06B6D4  // Secondary
SeafoamGreen   = #10B981  // Success
SunsetOrange   = #F59E0B  // Warning
CoralRed       = #EF4444  // Error

PearlWhite     = #F8FAFC
SeaMist        = #E2E8F0
StormGray      = #94A3B8
DeepFog        = #475569
```

### Opacity Layers

```yaml
surface:
  5: "rgba(255,255,255,0.05)"   # #FFFFFF0D
  10: "rgba(255,255,255,0.10)"  # #FFFFFF1A
  15: "rgba(255,255,255,0.15)"  # #FFFFFF26
  20: "rgba(255,255,255,0.20)"  # #FFFFFF33
  30: "rgba(255,255,255,0.30)"  # #FFFFFF4D

border:
  10: "rgba(255,255,255,0.10)"  # #FFFFFF1A
  20: "rgba(255,255,255,0.20)"  # #FFFFFF33
  30: "rgba(255,255,255,0.30)"  # #FFFFFF4D

text:
  primary: "rgba(255,255,255,0.90)"    # #FFFFFFE6
  secondary: "rgba(255,255,255,0.80)"  # #FFFFFFCC
  muted: "rgba(255,255,255,0.60)"      # #FFFFFF99
  disabled: "rgba(255,255,255,0.40)"   # #FFFFFF66
```

### Gradients

```css
/* Background */
linear-gradient(180deg, #0A1929 0%, #0F172A 25%, #1E293B 75%, #0F172A 100%)

/* Accent */
linear-gradient(90deg, rgba(59,130,246,0.2) 0%, rgba(6,182,212,0.2) 100%)

/* Ambient Light 1 */
radial-gradient(circle at 25% 10%, rgba(59,130,246,0.2) 0%, transparent 50%)

/* Ambient Light 2 */
radial-gradient(circle at 75% 90%, rgba(6,182,212,0.2) 0%, transparent 50%)
```

---

## Typography Scale

| Token | Size | Line Height | Weight | Usage |
|-------|------|-------------|--------|-------|
| display | 57sp | 64 | 300 | Large headers |
| headline-large | 32sp | 40 | 500 | Page titles |
| headline-medium | 28sp | 36 | 500 | Section headers |
| headline-small | 24sp | 32 | 500 | Card titles |
| title-large | 22sp | 28 | 500 | List headers |
| title-medium | 16sp | 24 | 500 | Subheaders |
| title-small | 14sp | 20 | 500 | Captions |
| body-large | 16sp | 24 | 400 | Primary text |
| body-medium | 14sp | 20 | 400 | Secondary text |
| body-small | 12sp | 16 | 400 | Tertiary text |
| label-large | 14sp | 20 | 500 | Buttons |
| label-medium | 12sp | 16 | 500 | Form labels |
| label-small | 11sp | 16 | 500 | Helper text |

---

## Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| space-0 | 0px | None |
| space-1 | 4px | Tight |
| space-2 | 8px | Compact |
| space-3 | 12px | Small |
| space-4 | 16px | Base |
| space-5 | 20px | Medium |
| space-6 | 24px | Section |
| space-8 | 32px | Large |
| space-10 | 40px | XL |
| space-12 | 48px | XXL |
| space-16 | 64px | Hero |
| space-20 | 80px | Page |

---

## Component: Data Table

### Specifications

```yaml
row_height: 56dp
header_height: 64dp
cell_padding: 16dp horizontal, 12dp vertical
borders: 1px solid border-10
alternating_rows: odd rows surface-5
hover_state: surface-5
selected_row: surface-15 + 4dp left border (CoralBlue)
```

### Template

```yaml
component:
  type: DataTable
  style:
    background: transparent
    borderRadius: 16
    border: { color: border-10, width: 1 }

  headerStyle:
    background: surface-10
    height: 64
    padding: { horizontal: 16, vertical: 12 }

  rowStyle:
    height: 56
    padding: { horizontal: 16, vertical: 12 }
    alternateBackground: surface-5
    hoverBackground: surface-5
    selectedBackground: surface-15
    selectedBorder: { left: 4, color: CoralBlue }
```

---

## Component: Todo List

### Specifications

```yaml
item_height: 72dp minimum
checkbox_size: 24dp × 24dp
priority_badge: 8dp height, rounded-4dp
status_colors:
  pending: text-muted
  in_progress: CoralBlue
  completed: SeafoamGreen + strikethrough
```

### Template

```yaml
component:
  type: TodoList
  style:
    background: surface-5
    borderRadius: 16
    border: { color: border-10, width: 1 }
    padding: 8

  itemStyle:
    minHeight: 72
    padding: 16
    gap: 8
    borderBottom: { color: border-10, width: 1 }
```

---

## Component: VR Window

### Specifications

```yaml
title_bar_height: 56dp
corner_radius: 24dp (desktop), 0dp (mobile)
shadow: 0dp 8dp 32dp rgba(0,0,0,0.4)
backdrop_blur: 40px
border: 1px border-20

states:
  active:
    background: surface-10 + blur
    border: border-20
    shadow: 8dp + blue-glow
  inactive:
    background: surface-5 + blur
    border: border-10
    shadow: 4dp
```

### Standard Sizes

```yaml
small: { width: 600px, height: 400px }
medium: { width: 800px, height: 600px }
large: { width: 1100px, height: 700px }
xl: { width: 1400px, height: 900px }
full: { width: 90vw, height: 90vh }
```

---

## Component: Modal Dialog

### Specifications

```yaml
width: 400px (small), 600px (medium), 800px (large)
max_height: 80vh
corner_radius: 24dp
background: surface-20 + blur-40px
border: 1px border-30
overlay: rgba(0,0,0,0.5)
animation: fade-scale 200ms ease-out
```

### Template

```yaml
component:
  type: Dialog
  style:
    width: 600
    maxHeight: 80vh
    borderRadius: 24
    background: surface-20
    backdropBlur: 40
    border: { color: border-30, width: 1 }

  overlayStyle:
    background: rgba(0,0,0,0.5)
    backdropBlur: 8
```

---

## Component: Toast

### Specifications

```yaml
width: auto (max 400px)
height: 48dp minimum
position: bottom-center (desktop), top-center (mobile)
corner_radius: 12dp
background: surface-30 + blur-24px
duration: 3s (info), 5s (warning/error), 2s (success)

variants:
  info: { icon: ℹ, border: CoralBlue }
  success: { icon: ✓, border: SeafoamGreen }
  warning: { icon: ⚠, border: SunsetOrange }
  error: { icon: ✗, border: CoralRed }
```

---

## Component: Snackbar

### Specifications

```yaml
width: auto (max 600px)
height: 56dp
position: bottom-left (desktop), bottom-center (mobile)
corner_radius: 8dp
background: OceanShallow solid (no blur)
duration: 5s (with action), 3s (without)
```

---

## Layout: Dashboard

```yaml
structure:
  - header: { breadcrumbs, user_menu, search }
  - hero: { height: 200, background: gradient-accent }
  - metrics_grid: { columns: 4, gap: 16 }
  - chart: { height: 400 }
  - data_table: { pagination: true }
```

---

## Layout: List/Feed

```yaml
structure:
  - filter_bar: { height: 64, search: true }
  - list_container:
      item_height: 80
      template:
        - avatar: { size: 48 }
        - title: { style: title-medium }
        - subtitle: { style: body-medium }
        - preview: { style: body-small, lines: 2 }
        - timestamp: { style: label-small }
```

---

## Layout: Detail/Content

```yaml
structure:
  - navigation_bar: { height: 56 }
  - hero_image: { height: 300 }
  - metadata: { padding: 24 }
  - content_body: { padding: 24, max_width: 720 }
  - related_items: { layout: horizontal-scroll }
```

---

## Layout: Form

```yaml
structure:
  - form_header: { padding: 24 }
  - sections:
      - title
      - fields[]
  - form_actions:
      padding: 24
      justify: space-between
```

---

## Layout: Multi-Window

```yaml
structure:
  background:
    - gradient
    - grid_pattern: { spacing: 50, opacity: 0.1 }
    - ambient_lights: { count: 2, opacity: 0.2 }

  windows:
    - browser: { x: 200, y: 80, w: 1100, h: 700 }
    - email: { x: 250, y: 120, w: 1000, h: 650 }

  dock:
    position: bottom-center
    apps: [launcher, browser, email, calendar, documents, conference]
```

---

## Platform: Android

### Compose Mapping

```kotlin
// Colors
MaterialTheme(
    colorScheme = darkColorScheme(
        primary = CoralBlue,
        secondary = TurquoiseCyan,
        tertiary = SeafoamGreen,
        error = CoralRed,
        background = DeepOcean,
        surface = OceanDepth
    )
)

// Glassmorphic Surface
Box(
    modifier = Modifier
        .blur(40.dp)  // Android 12+
        .background(surface10)
        .border(1.dp, border20, RoundedCornerShape(16.dp))
)

// Fallback (Android < 12)
Box(
    modifier = Modifier
        .background(surface10.copy(alpha = 0.95f))
)
```

---

## Platform: iOS

### SwiftUI Mapping

```swift
// Colors
extension Color {
    static let deepOcean = Color(hex: "0A1929")
    static let surface10 = Color.white.opacity(0.10)
}

// Glassmorphic Surface
content
    .background(.ultraThinMaterial)  // Native blur
    .background(
        RoundedRectangle(cornerRadius: 16)
            .fill(Color.surface10)
    )
    .overlay(
        RoundedRectangle(cornerRadius: 16)
            .strokeBorder(Color.border20, lineWidth: 1)
    )
```

---

## Platform: Web

### React + Tailwind Mapping

```tsx
// Tailwind Config
module.exports = {
  theme: {
    extend: {
      colors: {
        'deep-ocean': '#0A1929',
        'surface-10': 'rgba(255,255,255,0.10)',
      }
    }
  }
}

// Component
<div className="bg-surface-10 backdrop-blur-[40px] border border-border-20 rounded-2xl">
  {content}
</div>

// Fallback
@supports not (backdrop-filter: blur(40px)) {
  .backdrop-blur-\[40px\] {
    background: rgba(15, 23, 42, 0.95);
  }
}
```

---

## Command Bar (Floating Toolbar)

### Position by Orientation

```yaml
landscape:
  position: bottom-center
  layout: horizontal
  height: 64dp
  width: auto (content-fit)
  margin_bottom: 24dp

portrait:
  position: right-center  # Or left-center based on handedness
  layout: vertical
  width: 64dp
  height: auto (content-fit)
  margin_side: 16dp

auto_switch: true  # Reorient on rotation
animation: slide + morph 300ms ease-out
```

### Command Bar Specs

```yaml
background: surface-20 + blur-40px
border: 1px border-30
corner_radius: 32dp (pill shape)
shadow: 0 8dp 32dp rgba(0,0,0,0.3)

items:
  size: 48dp × 48dp
  gap: 8dp
  icon_size: 24dp
  active_indicator: 4dp dot (CoralBlue)

voice_button:
  size: 56dp × 56dp (primary)
  position: center
  glow: CoralBlue pulse when listening
  states:
    idle: surface-10
    listening: CoralBlue + pulse animation
    processing: TurquoiseCyan + spin
    error: CoralRed

expand_behavior:
  collapsed: voice_button only
  expanded: full command palette
  trigger: long_press or "Hey Magic"
```

### Command Bar by Device

```yaml
phone:
  default: collapsed (voice only)
  reveal: hover/long-press shows commands
  dismiss: tap outside or 3s timeout

tablet:
  default: expanded horizontal
  collapse: swipe down to minimize

desktop:
  default: expanded horizontal
  dock_mode: can dock to edge

smart_glasses:
  default: expanded (always visible)
  position: bottom of FOV
  opacity: 60% until gaze

vr_headset:
  default: follows head (world-locked)
  distance: 1.2m from user
  curve: 15° arc
```

---

## Voice-First Architecture

### Interaction Hierarchy

```yaml
primary: voice  # Always available
secondary:
  phone: touch + hover reveal
  tablet: touch + stylus
  desktop: mouse + keyboard
  glasses: gaze + gesture
  vr: controllers + hand tracking

fallback: touch (when voice unavailable)
```

### Voice Command Visibility

```yaml
phone:
  commands_visible: false (hidden by default)
  reveal_trigger:
    - hover over command bar
    - long press anywhere
    - say "show commands"
  auto_hide: 3s after last interaction

tablet:
  commands_visible: true (labels shown)
  voice_hint: shown on focus

desktop:
  commands_visible: true
  keyboard_shortcuts: shown on hover

smart_glasses:
  commands_visible: true (always)
  display_mode: floating labels
  gaze_highlight: command highlights on look
  voice_confirm: "select" or dwell 800ms

vr_headset:
  commands_visible: true
  interaction: point + trigger or voice
  haptic_feedback: on selection
```

### Voice Activation Patterns

```yaml
wake_word: "Hey Magic" or "OK Magic"
confirmation: audible chime + visual pulse
commands:
  navigation: "go to [screen]", "open [app]", "back"
  actions: "save", "send", "delete", "cancel"
  selection: "select [item]", "choose [option]"
  control: "scroll up/down", "zoom in/out"
  system: "show commands", "hide", "help"

continuous_listening:
  glasses: always (low power mode)
  phone: after wake word (30s window)
  desktop: push-to-talk or wake word
```

---

## Orientation-Aware Layouts

### Portrait Mode

```yaml
phone_portrait:
  command_bar: right edge, vertical
  navigation: bottom tabs (5 max)
  content: single column
  header: compact (48dp)
  safe_areas:
    top: status bar + notch
    bottom: gesture indicator

tablet_portrait:
  command_bar: bottom, horizontal
  navigation: side rail (72dp)
  content: single column (max 600dp)
  split_view: 30/70 or 40/60

desktop_portrait:  # Rotated monitor
  command_bar: bottom
  navigation: left sidebar (240dp)
  content: centered (max 800dp)
```

### Landscape Mode

```yaml
phone_landscape:
  command_bar: bottom center, horizontal
  navigation: hidden (swipe reveal)
  content: two-column or full-width
  header: hidden or minimal
  immersive: true (hide system UI)

tablet_landscape:
  command_bar: bottom center
  navigation: left sidebar (280dp)
  content: multi-column grid
  split_view: 50/50 or custom

desktop_landscape:
  command_bar: bottom center (dockable)
  navigation: left sidebar (280dp collapsed, 320dp expanded)
  content: flexible grid (12 columns)
  windows: floating multi-window
```

### Auto-Switch Behavior

```yaml
detection:
  sensor: accelerometer + gyroscope
  threshold: 45° rotation held 500ms
  debounce: 300ms (prevent rapid switching)

transition:
  animation: crossfade 200ms + layout morph 300ms
  preserve_scroll: true
  preserve_selection: true
  preserve_focus: true

lock_orientation:
  user_override: true
  api: setOrientationLock(portrait|landscape|auto)
  persist: per-screen preference
```

---

## Device-Specific Layouts

### Phone Layout Templates

```yaml
phone_dashboard_portrait:
  structure:
    - header: { height: 56, sticky: true }
    - hero_card: { height: 180, swipeable: true }
    - quick_actions: { height: 80, scroll: horizontal }
    - feed: { flex: 1, virtual_scroll: true }
    - command_bar: { position: right, width: 64 }

phone_dashboard_landscape:
  structure:
    - sidebar: { width: 280, collapsible: true }
    - main:
        - header: { height: 48 }
        - content: { columns: 2, gap: 16 }
    - command_bar: { position: bottom, height: 64 }
```

### Tablet Layout Templates

```yaml
tablet_dashboard_portrait:
  structure:
    - rail: { width: 72 }
    - main:
        - header: { height: 64 }
        - content: { columns: 2, gap: 24 }
    - command_bar: { position: bottom }

tablet_dashboard_landscape:
  structure:
    - sidebar: { width: 320 }
    - main:
        - header: { height: 64 }
        - content: { columns: 3, gap: 24 }
    - command_bar: { position: bottom }
```

### Smart Glasses Layout

```yaml
glasses_hud:
  field_of_view: 52° diagonal
  safe_zone: center 70% (avoid edges)
  text_size_min: 24sp (legibility at distance)

  layout:
    - status_bar:
        position: top-right
        content: [time, battery, connectivity]
        opacity: 40%

    - focus_area:
        position: center
        size: 60% FOV
        content: primary task

    - command_bar:
        position: bottom
        opacity: 60% (100% on gaze)
        items: [back, home, voice, more]

    - notifications:
        position: top-left
        max_visible: 2
        auto_dismiss: 5s

  gaze_interaction:
    dwell_time: 800ms
    highlight: border glow
    confirm: voice "select" or blink
```

### VR Headset Layout

```yaml
vr_environment:
  world_scale: 1:1
  ui_distance: 1.0-2.0m from user
  ui_curvature: 15° arc for comfort

  layout:
    - environment: { 360° background }
    - windows:
        default_size: 1.2m × 0.8m
        min_distance: 0.5m
        max_distance: 4.0m
        grabbable: true
        resizable: true

    - command_bar:
        position: follows gaze (lazy follow)
        distance: 0.8m
        opacity: 50% (100% on look)

    - keyboard:
        type: virtual floating
        position: waist height
        haptics: on key press

  comfort:
    refresh_rate: 90Hz min
    latency_max: 20ms
    text_density: reduced 20%
```

---

## Responsive Breakpoints

```yaml
mobile:
  min: 0px
  max: 599px
  columns: 1
  padding: 16dp
  font_scale: 1.0
  orientation: portrait_primary

tablet:
  min: 600px
  max: 1023px
  columns: 2-3
  padding: 24dp
  font_scale: 1.0
  orientation: landscape_primary

desktop:
  min: 1024px
  max: 1439px
  columns: 3-4
  padding: 32dp
  font_scale: 1.0

wide:
  min: 1440px
  max: 1919px
  columns: 4-6
  padding: 48dp

ultra:
  min: 1920px
  columns: 6-12
  padding: 64dp

glasses:
  fov: 52°
  resolution: 1280×720 per eye
  safe_zone: 70% center

vr:
  fov: 110°
  resolution: 2160×2160 per eye
  pixel_density: 20ppd min
```

---

## Accessibility

```yaml
touch_targets: 48dp × 48dp minimum
contrast_ratio: 4.5:1 (normal), 3:1 (large)
text_size_min: 12sp (labels), 14sp (body)
focus_indicator: 2px border, high contrast

screen_reader:
  - contentDescription required
  - accessibilityRole required
  - accessibilityState for dynamic content

keyboard_nav:
  - tab_order: visual hierarchy
  - enter_space: activates buttons
  - arrow_keys: navigate lists/grids
  - escape: closes modals
```

---

## References

- VisionOS: https://developer.apple.com/videos/play/wwdc2023/10076/
- Fluent: https://fluent2.microsoft.design/elevation
- Glassmorphism: https://www.nngroup.com/articles/glassmorphism/
- VR UI: https://arxiv.org/html/2508.09358v1
