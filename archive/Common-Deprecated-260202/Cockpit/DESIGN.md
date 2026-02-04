# Cockpit Design Specification

Based on preferred embodiment (macOS/Vision Pro style) with voice-first philosophy.

---

## Visual Design (From Preferred Embodiment Image)

### Layout
- **Default**: LINEAR_HORIZONTAL - 5-6 windows in horizontal array
- **Spacing**: Even spacing (~0.4m between windows)
- **Perspective**: Slight 3D tilt (5-10 degrees)
- **Background**: Neutral gradient (beige/tan) with soft ambient lighting
- **Shadows**: Soft drop shadows under each window

### Window Styles (User-Configurable)

| Style | Borders | Shadows | Use Case |
|-------|---------|---------|----------|
| **Minimal (Default)** | Very subtle (1dp, 20% dark) | Soft drop shadows | Clean macOS-style |
| **Glass** | Dark-glass with glow (2dp + 8dp blur) | Enhanced shadows + glow | Unique Cockpit identity |

Voice: `"Minimal borders"` or `"Glass borders"`

### Control UI (Hybrid Approach)

```
┌─────────────────────────────────────────────────────┐
│  Control Rail (Top/Side - Floating)                 │
│  [Home] [Workspace] [Layout] [Voice] [Settings]    │
└─────────────────────────────────────────────────────┘

     ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐
     │ 1  │  │ 2  │  │ 3  │  │ 4  │  │ 5  │
     │    │  │    │  │    │  │ ◉  │  │    │  Windows
     └────┘  └────┘  └────┘  └────┘  └────┘

              ○ ○ ○ ◉ ○ ○
           Window Dock (Bottom Center)

[Music] [Timer]              [Battery] [Notifications]
 Utility Belt (Bottom Corners)
```

---

## Layout Presets (5 Total)

| Preset | Description | Max Windows | Voice Command |
|--------|-------------|-------------|---------------|
| **LINEAR_HORIZONTAL** | Horizontal array (DEFAULT) | 6 | "Linear mode" |
| **ARC_3_FRONT** | Curved arc in front | 3 | "Arc mode" |
| **GRID_2x2** | Four evenly sized | 4 | "Grid mode" |
| **STACK_CENTER** | One main + stacked | 5 | "Stack mode" |
| **THEATER** | Large cinema + tools | 4 | "Theater mode" |

---

## Voice Commands (Complete List)

### Window Management
```
"Open Gmail"              → Opens Gmail in new window
"Close browser"           → Closes browser window
"Focus calculator"        → Brings calculator to front
"Show all windows"        → Overview mode
"Hide music player"       → Sets window invisible
"Window 3"                → Switches to 3rd window
"Next window"             → Cycles to next
"Previous window"         → Cycles to previous
```

### Layout Control
```
"Linear mode"             → LINEAR_HORIZONTAL (default)
"Arc mode"                → ARC_3_FRONT
"Grid mode"               → GRID_2x2
"Stack mode"              → STACK_CENTER
"Theater mode"            → THEATER
"Move email to the right" → Repositions window
"Make browser bigger"     → Increases size
"Pin this window"         → Pins window
```

### Visual Customization
```
"Minimal borders"         → Clean macOS-style (default)
"Glass borders"           → Dark-glass with glow
"Show dock"               → Display window indicators
"Hide dock"               → Hide window indicators
"Show controls"           → Display Control Rail
"Hide controls"           → Hide Control Rail
"Show utilities"          → Display Utility Belt
```

### Workspace Management
```
"Save workspace as work"  → Saves current layout
"Load morning routine"    → Restores saved workspace
"Next workspace"          → Cycles workspaces
"Delete this workspace"   → Removes workspace
```

### Content Interaction (via VoiceOS)
```
"Read this window"        → VoiceOS reads contents
"Click sign in button"    → VoiceOS clicks in window
"Scroll down"             → VoiceOS scrolls window
"What's in the email window?" → VoiceOS describes content
```

---

## VoiceOS Integration

### Communication Protocol

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

### Integration Points

| Feature | VoiceOS Role |
|---------|--------------|
| Window Management | Open/close/focus windows via voice |
| Layout Control | Switch layouts, move/resize windows |
| Navigation | Window switching, workspace cycling |
| Content Access | Read window contents via accessibility |
| App Control | Click buttons, fill forms in windows |
| Spatial Audio | 3D audio cues for window positions |

---

## UI Components (Implemented)

### Core UI
- **WindowDock.kt**: Bottom center dot indicators
- **ControlRail.kt**: Floating toolbar (5 buttons)
- **UtilityBelt.kt**: Corner widgets (4 default widgets)
- **WindowStyle.kt**: Configurable styles (Minimal/Glass)

### Layouts
- **LinearHorizontalLayout.kt**: Default horizontal array layout
- *Arc/Grid/Stack/Theater layouts*: To be implemented

### Core Types
- **Vector3D.kt**: 3D spatial positioning
- **Quaternion**: Rotation representation

---

## Visual Specs (From Preferred Embodiment)

### Colors
| Element | Color | Hex |
|---------|-------|-----|
| Background Gradient Start | Light beige | #D4C5B0 |
| Background Gradient End | Darker tan | #B8A596 |
| Ambient Light | Warm white | #FFF8E1 |
| Accent Blue (Communication) | - | TBD |
| Accent Orange (Data) | - | TBD |
| Accent Green (Utilities) | - | TBD |

### Dimensions
| Element | Value | Unit |
|---------|-------|------|
| Window Spacing | 0.4 | meters |
| Default Window Width | 0.8 | meters |
| Default Window Height | 0.6 | meters |
| View Distance | 2.0 | meters |
| Tilt Angle | 5-10 | degrees |
| Curvature Radius | 3.0 | meters |
| Border Width (Minimal) | 1 | dp |
| Border Width (Glass) | 2 | dp |
| Shadow Offset Y | 4-6 | dp |
| Shadow Blur | 12-20 | dp |

---

## Architecture

```
Common/Cockpit/
├── core/
│   ├── window/            # Window model, lifecycle
│   └── workspace/         # Workspace, Vector3D, Quaternion
├── layout/
│   ├── presets/           # LinearHorizontal, Arc, Grid, etc.
│   └── positioning/       # Spatial positioning algorithms
├── voice/                 # VOICE-FIRST CRITICAL
│   ├── VoiceOSBridge.kt   # VoiceOS integration
│   ├── commands/          # Voice command parsing
│   ├── feedback/          # Audio feedback
│   └── spatial-audio/     # 3D audio positioning
├── interactions/
│   ├── voice/             # Voice handlers (PRIMARY)
│   ├── touch/             # Touch gestures (FALLBACK)
│   ├── gaze/              # Gaze tracking (FALLBACK)
│   └── controller/        # AR controllers (FALLBACK)
├── rendering/             # Platform-agnostic rendering
├── ar/                    # AR spatial tracking
└── ui/                    # Control components
    ├── WindowDock.kt
    ├── ControlRail.kt
    ├── UtilityBelt.kt
    └── theme/
        └── WindowStyle.kt
```

---

## Next Steps

1. **Implement remaining layout presets**: Arc, Grid, Stack, Theater
2. **Build VoiceOSBridge**: Complete voice integration
3. **Platform-specific rendering**: Android (Compose + ARCore)
4. **Voice command parser**: NLU integration
5. **Spatial audio system**: 3D audio feedback
6. **Workspace persistence**: Save/load layouts
7. **Testing**: Voice-first interaction flows

---

**Version**: 1.0
**Updated**: 2025-12-08
**Based On**: Preferred embodiment (macOS/Vision Pro style)
