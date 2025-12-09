# Cockpit Spatial Workspace Library

**Voice-first** cross-platform KMP library for managing floating windows in AR and 2D environments. Designed for deep integration with VoiceOS accessibility service.

## Overview

Cockpit provides a **voice-controlled** spatial workspace system similar to Apple Vision Pro-style floating windows, but with a unique identity. Users work in a virtual "cockpit" of floating screens arranged around them in AR or 2D curved layouts.

**CRITICAL**: Voice is the primary interaction method. All features are fully operable via voice commands through VoiceOS integration.

## Features

- **🎤 Voice-First Design**: All operations accessible via natural language commands
- **🔊 VoiceOS Integration**: Deep integration with VoiceOS accessibility service
- **Multi-Window Management**: Run multiple apps, tools, and content panels side-by-side
- **Spatial Positioning**: 3D coordinate system for window placement
- **Layout Presets**: Pre-configured layouts (Arc, Grid, Stack, Theater)
- **Hands-Free Operation**: Complete workflow without touch or mouse
- **Spatial Audio Feedback**: 3D audio cues for window positions
- **Cross-Platform**: Android, iOS, Desktop, Web support
- **Unique Visual Identity**: Dark-glass borders, accent colors, curved windows
- **Multiple Input Methods**: Voice (PRIMARY), Touch, Gaze, Controllers, Keyboard (fallback)

## Window Types

| Type | Description |
|------|-------------|
| `ANDROID_APP` | Android apps via virtual displays |
| `WEB_APP` | Web apps via WebView |
| `REMOTE_DESKTOP` | PC/Mac streaming |
| `WIDGET` | Tools (calculator, music, timers, AI chat) |

## Layout Presets

| Preset | Description | Voice Command |
|--------|-------------|---------------|
| `LINEAR_HORIZONTAL` | 5-6 windows in horizontal array (DEFAULT) | "Linear mode" |
| `ARC_3_FRONT` | Three windows in curved arc | "Arc mode" |
| `GRID_2x2` | Four evenly sized windows | "Grid mode" |
| `STACK_CENTER` | One main + stacked background | "Stack mode" |
| `THEATER` | Large cinema window + side tools | "Theater mode" |

**Default**: LINEAR_HORIZONTAL provides familiar macOS/Vision Pro style experience.

## Voice Commands (VoiceOS Integration)

### Window Management
```
"Open Gmail"              → Opens Gmail in new window
"Close browser"           → Closes browser window
"Focus calculator"        → Brings calculator to front
"Show all windows"        → Overview mode
```

### Layout Control
```
"Linear mode"             → Switches to LINEAR_HORIZONTAL (default)
"Arc mode"                → Switches to ARC_3_FRONT layout
"Grid mode"               → Switches to GRID_2x2 layout
"Theater mode"            → Switches to THEATER layout
"Move email to the right" → Repositions window
"Make browser bigger"     → Increases window size
```

### Visual Customization
```
"Minimal borders"         → Clean macOS-style (default)
"Glass borders"           → Dark-glass with glow effect
"Show dock"               → Display window indicators
"Show controls"           → Display Control Rail
```

### Workspace Management
```
"Save workspace as work"  → Saves current layout
"Load morning routine"    → Restores saved workspace
"Next workspace"          → Cycles workspaces
```

### Content Interaction (via VoiceOS Accessibility)
```
"Read this window"        → VoiceOS reads window contents
"Click sign in button"    → VoiceOS clicks in focused window
"Scroll down"             → VoiceOS scrolls current window
```

## Platform Support

| Platform | Status | Rendering | Input | VoiceOS Integration |
|----------|--------|-----------|-------|---------------------|
| Android | ✅ Active | ARCore + Compose | Voice (PRIMARY), Touch, Gaze | Full accessibility service |
| iOS | 🚧 Planned | ARKit + SwiftUI | Voice (PRIMARY), Touch, Eye | VoiceOver integration |
| Desktop | 🚧 Planned | OpenGL/Vulkan | Voice (PRIMARY), Mouse, Kbd | System voice commands |
| Web | 🚧 Planned | WebXR + Canvas | Voice (PRIMARY), Mouse, Kbd | Web Speech API |

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
│   │   ├── core/
│   │   │   ├── window/        # Window model, lifecycle
│   │   │   └── workspace/     # Workspace management
│   │   ├── layout/
│   │   │   ├── presets/       # Arc, Grid, Stack, Theater
│   │   │   └── positioning/   # Spatial positioning
│   │   ├── voice/             # VOICE-FIRST CRITICAL
│   │   │   ├── VoiceOSBridge.kt
│   │   │   ├── commands/      # Voice command parsing
│   │   │   ├── feedback/      # Audio feedback
│   │   │   └── spatial-audio/ # 3D audio positioning
│   │   ├── interactions/
│   │   │   ├── voice/         # Voice handlers (PRIMARY)
│   │   │   ├── touch/         # Touch (FALLBACK)
│   │   │   ├── gaze/          # Gaze (FALLBACK)
│   │   │   └── controller/    # AR controllers (FALLBACK)
│   │   ├── rendering/         # Platform-agnostic rendering
│   │   ├── ar/                # AR spatial tracking
│   │   └── ui/                # Control Rail, Utility Belt
│   ├── androidMain/
│   ├── iosMain/
│   └── desktopMain/
├── build.gradle.kts
└── README.md
```

## Dependencies

| Library | Purpose |
|---------|---------|
| **VoiceOS** | Voice command processing, accessibility service integration |
| **Common/Voice** | Voice recognition (shared library) |
| **Common/NLU** | Natural language understanding |
| kotlinx-coroutines | Async operations |
| kotlinx-serialization | Workspace persistence |
| ARCore/ARKit | AR spatial tracking |
| Compose/SwiftUI | Platform UI |

## Visual Identity

### Window Styles (User-Configurable)
- **Minimal (Default)**: Very subtle borders, clean macOS-style
- **Glass**: Dark-glass borders with soft inner glow
- **Background**: Neutral gradient with soft ambient lighting
- **Shadows**: Soft drop shadows (enhanced with glow in Glass mode)
- **Perspective**: Slight 3D tilt for depth

### Colors & Effects
- **Accent Colors**: Blue (communication), Orange (data/analytics), Green (utilities)
- **Curvature**: Optional horizontal curve on large windows
- **Lighting**: Soft ambient lighting, neutral gradient background

### Control UI (Hybrid)
- **Window Dock**: Bottom center with dot indicators (like macOS/Vision Pro)
- **Control Rail**: Floating toolbar (Home, Workspace, Layout, Voice, Settings)
- **Utility Belt**: Corner mini-panels (music, timers, battery)

## Documentation

- [Module Instructions](/.claude/CLAUDE.md)
- [Architecture Guide](../../Docs/Common/Cockpit/)

## License

Proprietary - Avanues Platform

## Version

1.0 - Initial setup (2025-12-08)
