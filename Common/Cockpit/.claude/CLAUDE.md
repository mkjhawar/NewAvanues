# Cockpit - Spatial Workspace Library

Cockpit is a cross-platform spatial workspace library for managing floating windows in AR/2D environments.

---

## Module Info

| Property | Value |
|----------|-------|
| Type | Package (KMP Library) |
| Platforms | Android, iOS, Desktop, Web |
| Path | `Common/Cockpit` |
| Namespace | `com.avanues.cockpit` |

---

## Core Concept

Spatial workspace system enabling multi-window layouts similar to Apple Vision Pro but with unique Cockpit identity. Manages floating windows containing apps, tools, dashboards, and remote desktops.

---

## Window Model

```kotlin
data class AppWindow(
    val id: String,
    val title: String,
    val type: WindowType,
    val sourceId: String,
    val position: Vector3D,
    val rotation: Quaternion,
    val widthMeters: Float,
    val heightMeters: Float,
    val zLayer: Int,
    val pinned: Boolean,
    val visible: Boolean
)

enum class WindowType {
    ANDROID_APP,
    WEB_APP,
    REMOTE_DESKTOP,
    WIDGET
}
```

---

## Layout Presets

| Preset | Description |
|--------|-------------|
| ARC_3_FRONT | Three windows in curved arc |
| GRID_2x2 | Four evenly sized windows |
| STACK_CENTER | One main + stacked background |
| THEATER | Large cinema window + side tools |

---

## Visual Identity

| Element | Style |
|---------|-------|
| Window Borders | Thin dark-glass with soft glow |
| Accent Colors | Blue (comm), Orange (data), Green (utils) |
| Curvature | Horizontal curve on large windows |
| Control Rail | Floating curved toolbar (Home, Workspace, Layout, Voice, Settings) |
| Utility Belt | Lower mini-panels (music, timers, battery) |

---

## Interaction Rules

| Action | Behavior |
|--------|----------|
| Focus | Window enlarges, full opacity |
| Background | Windows shrink, reduced opacity |
| Move/Resize | Gesture, controller, or voice |
| Voice Commands | Open/close, resize, layout switch |

---

## Platform Support

| Platform | Rendering | Input |
|----------|-----------|-------|
| Android | ARCore + Compose | Touch, Voice, Gaze, Controllers |
| iOS | ARKit + SwiftUI | Touch, Voice, Gaze, Eye Tracking |
| Desktop | OpenGL/Vulkan | Mouse, Keyboard, Voice, Gaze |
| Web | WebXR + Canvas | Mouse, Keyboard, Voice |

---

## Architecture

```
Common/Cockpit/
├── core/              # Window model, workspace logic
├── layout/            # Preset layouts, positioning
├── interactions/      # Gesture, voice, input handling
├── rendering/         # Platform-agnostic rendering interfaces
├── ar/                # AR-specific functionality
└── ui/                # UI components (Control Rail, Utility Belt)
```

---

## Design Principles

| Principle | Implementation |
|-----------|----------------|
| Platform Agnostic | Core logic in commonMain |
| Extensible | Plugin architecture for custom windows |
| Performance | 60fps minimum, efficient rendering |
| Accessibility | Voice, gaze, touch, keyboard support |
| Original Identity | No Apple cloning, unique Cockpit style |

---

## Dependencies

| Library | Purpose |
|---------|---------|
| kotlinx-coroutines | Async operations |
| kotlinx-serialization | Workspace save/load |
| ARCore/ARKit | AR spatial tracking |
| Compose/SwiftUI | Platform UI |

---

## Quality Standards

| Metric | Target |
|--------|--------|
| Test Coverage | 90%+ |
| API Stability | Stable before 1.0 |
| Documentation | All public APIs |
| Performance | <16ms frame time |

---

**Version:** 1.0 | **Updated:** 2025-12-08
