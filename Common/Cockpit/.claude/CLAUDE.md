# Cockpit - Spatial Workspace Library

Cockpit is a **voice-first** cross-platform spatial workspace library for managing floating windows in AR/2D environments. Designed for deep integration with VoiceOS accessibility service.

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

**Voice-first** spatial workspace system enabling multi-window layouts similar to Apple Vision Pro but with unique Cockpit identity. Manages floating windows containing apps, tools, dashboards, and remote desktops.

**CRITICAL**: Voice is the primary interaction method. All features MUST be fully operable via voice commands through VoiceOS integration.

---

## Voice-First Design

| Principle | Requirement |
|-----------|-------------|
| Voice Primary | Every action accessible via voice command |
| VoiceOS Integration | Deep integration with VoiceOS accessibility service |
| Natural Language | Support conversational commands, not just keywords |
| Voice Feedback | Audio confirmation for all actions |
| Hands-Free Operation | Complete workflow possible without touch/mouse |
| Multi-Modal Fallback | Touch/gaze/keyboard as secondary options |

---

## VoiceOS Integration

### Core Integration Points

| Component | VoiceOS Role |
|-----------|--------------|
| Window Management | "Open browser", "Close calculator", "Focus email" |
| Layout Control | "Switch to theater mode", "Arrange in arc" |
| Navigation | "Next window", "Previous workspace", "Go to settings" |
| Workspace Actions | "Save workspace as work setup", "Load evening layout" |
| Window Manipulation | "Move email left", "Make browser bigger", "Pin this window" |
| Content Interaction | Read window contents, interact with apps via voice |

### VoiceOS Communication Protocol

```kotlin
interface VoiceOSBridge {
    // Cockpit → VoiceOS
    suspend fun requestVoiceInput(): VoiceCommand
    suspend fun announceAction(action: String)
    suspend fun requestAccessibilityInfo(windowId: String): AccessibilityNode

    // VoiceOS → Cockpit
    fun onVoiceCommand(command: VoiceCommand)
    fun onAccessibilityEvent(event: AccessibilityEvent)
    fun onGazeTarget(target: GazeTarget)
}
```

### Accessibility Service Integration

| Feature | Implementation |
|---------|----------------|
| Window Content Access | VoiceOS reads window contents via accessibility nodes |
| App Control | VoiceOS controls apps within windows (click buttons, fill forms) |
| Navigation Context | VoiceOS announces current window, layout, focus state |
| Spatial Audio Cues | Audio feedback for window positions in 3D space |
| Voice Shortcuts | Custom voice macros for common workspace operations |

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
    val visible: Boolean,

    // Voice-first additions
    val voiceName: String,              // How VoiceOS refers to this window
    val voiceDescription: String,       // Announced when window gains focus
    val voiceShortcuts: List<String>,   // Custom voice commands for this window
    val spatialAudioEnabled: Boolean    // 3D audio positioning for voice feedback
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

## Voice Command Examples

### Window Management
| Command | Action |
|---------|--------|
| "Open Gmail" | Opens Gmail in new window |
| "Close browser" | Closes browser window |
| "Focus calculator" | Brings calculator to front |
| "Show all windows" | Overview mode with all windows |
| "Hide music player" | Sets window invisible |

### Layout & Positioning
| Command | Action |
|---------|--------|
| "Theater mode" | Switches to THEATER layout preset |
| "Arrange in arc" | Switches to ARC_3_FRONT layout |
| "Move email to the right" | Repositions window spatially |
| "Make browser bigger" | Increases window dimensions |
| "Pin this window" | Pins current focused window |

### Workspace Management
| Command | Action |
|---------|--------|
| "Save workspace as morning routine" | Saves current layout with name |
| "Load work setup" | Restores saved workspace |
| "Next workspace" | Cycles to next workspace |
| "Delete this workspace" | Removes current workspace |

### Content Interaction (via VoiceOS)
| Command | Action |
|---------|--------|
| "Read this window" | VoiceOS reads window contents |
| "Click sign in button" | VoiceOS clicks button in focused window |
| "Scroll down" | VoiceOS scrolls current window |
| "What's in the email window?" | VoiceOS describes window content |

---

## Interaction Rules

| Action | Behavior |
|--------|----------|
| **Voice (Primary)** | All operations via natural language commands |
| Focus | Window enlarges, full opacity, spatial audio cue |
| Background | Windows shrink, reduced opacity |
| Move/Resize | Voice primary, gesture/controller fallback |
| Voice Feedback | Audio confirmation with spatial positioning |

---

## Platform Support

| Platform | Rendering | Input | VoiceOS Integration |
|----------|-----------|-------|---------------------|
| Android | ARCore + Compose | Voice (PRIMARY), Touch, Gaze | Full accessibility service |
| iOS | ARKit + SwiftUI | Voice (PRIMARY), Touch, Eye Tracking | VoiceOver integration |
| Desktop | OpenGL/Vulkan | Voice (PRIMARY), Mouse, Keyboard | System voice commands |
| Web | WebXR + Canvas | Voice (PRIMARY), Mouse, Keyboard | Web Speech API |

---

## Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Voice-First** | Voice is primary input, all features voice-accessible |
| **VoiceOS Native** | Deep integration with VoiceOS accessibility service |
| Platform Agnostic | Core logic in commonMain |
| Extensible | Plugin architecture for custom windows |
| Performance | 60fps minimum, efficient rendering |
| Accessibility | Voice primary, gaze/touch/keyboard fallback |
| Original Identity | No Apple cloning, unique Cockpit style |
| Hands-Free Capable | Complete workflow without touch/mouse |

---

## Architecture

```
Common/Cockpit/
├── core/
│   ├── window/            # Window model, lifecycle
│   └── workspace/         # Workspace management
├── layout/
│   ├── presets/           # Arc, Grid, Stack, Theater layouts
│   └── positioning/       # Spatial positioning algorithms
├── voice/                 # VOICE-FIRST CRITICAL
│   ├── VoiceOSBridge.kt   # VoiceOS integration interface
│   ├── commands/          # Voice command parsing & execution
│   ├── feedback/          # Audio feedback system
│   └── spatial-audio/     # 3D audio positioning
├── interactions/
│   ├── voice/             # Voice gesture handlers (PRIMARY)
│   ├── touch/             # Touch gestures (FALLBACK)
│   ├── gaze/              # Gaze tracking (FALLBACK)
│   └── controller/        # AR controller support (FALLBACK)
├── rendering/
│   ├── interfaces/        # Platform-agnostic rendering
│   └── composition/       # Window composition logic
├── ar/
│   ├── tracking/          # Spatial tracking
│   └── anchoring/         # Window anchoring in 3D
└── ui/
    ├── control-rail/      # Voice-activated toolbar
    └── utility-belt/      # Voice-controlled mini-panels
```

---

## Dependencies

| Library | Purpose |
|---------|---------|
| **VoiceOS** | Voice command processing, accessibility service integration |
| kotlinx-coroutines | Async operations |
| kotlinx-serialization | Workspace save/load |
| ARCore/ARKit | AR spatial tracking |
| Compose/SwiftUI | Platform UI |
| Common/Voice | Voice recognition (shared library) |
| Common/NLU | Natural language understanding |

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
