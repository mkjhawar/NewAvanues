# Cockpit Spatial Workspace Library

Cross-platform KMP library for managing floating windows in AR and 2D environments.

## Overview

Cockpit provides a spatial workspace system similar to Apple Vision Pro-style floating windows, but with a unique identity. Users work in a virtual "cockpit" of floating screens arranged around them in AR or 2D curved layouts.

## Features

- **Multi-Window Management**: Run multiple apps, tools, and content panels side-by-side
- **Spatial Positioning**: 3D coordinate system for window placement
- **Layout Presets**: Pre-configured layouts (Arc, Grid, Stack, Theater)
- **Cross-Platform**: Android, iOS, Desktop, Web support
- **Unique Visual Identity**: Dark-glass borders, accent colors, curved windows
- **Multiple Input Methods**: Touch, voice, gaze, controllers, keyboard

## Window Types

| Type | Description |
|------|-------------|
| `ANDROID_APP` | Android apps via virtual displays |
| `WEB_APP` | Web apps via WebView |
| `REMOTE_DESKTOP` | PC/Mac streaming |
| `WIDGET` | Tools (calculator, music, timers, AI chat) |

## Layout Presets

| Preset | Description |
|--------|-------------|
| `ARC_3_FRONT` | Three windows in curved arc |
| `GRID_2x2` | Four evenly sized windows |
| `STACK_CENTER` | One main + stacked background |
| `THEATER` | Large cinema window + side tools |

## Platform Support

| Platform | Status | Rendering | Input |
|----------|--------|-----------|-------|
| Android | ✅ Active | ARCore + Compose | Touch, Voice, Gaze |
| iOS | 🚧 Planned | ARKit + SwiftUI | Touch, Voice, Eye Tracking |
| Desktop | 🚧 Planned | OpenGL/Vulkan | Mouse, Keyboard, Voice |
| Web | 🚧 Planned | WebXR + Canvas | Mouse, Keyboard |

## Quick Start

```kotlin
// Create workspace
val workspace = Workspace(
    layoutPreset = LayoutPreset.ARC_3_FRONT
)

// Add window
workspace.addWindow(
    AppWindow(
        id = "browser",
        title = "Web Browser",
        type = WindowType.WEB_APP,
        sourceId = "https://example.com",
        position = Vector3D(0f, 0f, -2f),
        widthMeters = 1.5f,
        heightMeters = 1.0f
    )
)

// Focus window
workspace.focusWindow("browser")
```

## Architecture

```
Common/Cockpit/
├── src/
│   ├── commonMain/kotlin/com/avanues/cockpit/
│   │   ├── core/              # Window model, workspace logic
│   │   ├── layout/            # Preset layouts, positioning
│   │   ├── interactions/      # Gesture, voice, input handling
│   │   ├── rendering/         # Platform-agnostic rendering
│   │   ├── ar/                # AR-specific functionality
│   │   └── ui/                # UI components
│   ├── androidMain/
│   ├── iosMain/
│   └── desktopMain/
├── build.gradle.kts
└── README.md
```

## Dependencies

| Library | Purpose |
|---------|---------|
| kotlinx-coroutines | Async operations |
| kotlinx-serialization | Workspace persistence |
| ARCore/ARKit | AR spatial tracking |
| Compose/SwiftUI | Platform UI |

## Visual Identity

- **Window Borders**: Thin dark-glass with soft inner glow
- **Accent Colors**: Blue (communication), Orange (data/analytics), Green (utilities)
- **Curvature**: Horizontal curve on large windows
- **Control Rail**: Floating curved toolbar
- **Utility Belt**: Lower mini-panels

## Documentation

- [Module Instructions](/.claude/CLAUDE.md)
- [Architecture Guide](../../Docs/Common/Cockpit/)

## License

Proprietary - Avanues Platform

## Version

1.0 - Initial setup (2025-12-08)
