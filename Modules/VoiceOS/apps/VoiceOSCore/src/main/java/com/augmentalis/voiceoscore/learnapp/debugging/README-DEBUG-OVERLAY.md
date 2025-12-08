# LearnApp Debug Overlay

Visual debugging tool for LearnApp exploration that overlays colored highlights on UI elements.

## Features

- **Real-time element visualization** - See which elements have been learned, their VUIDs, and navigation links
- **Color-coded learning source** - Distinguish between LearnApp-learned and JIT-learned elements
- **Navigation tracking** - View which elements link to which screens
- **Toggle controls** - Enable/disable via FloatingProgressWidget buttons
- **Three verbosity levels** - Control detail level (minimal/standard/verbose)

## Color Coding

| Color | Meaning |
|-------|---------|
| 🟢 Green | LearnApp-learned element (full exploration) |
| 🔵 Blue | JIT-learned element (passive learning) |
| 🟡 Yellow | Has VUID but not linked to navigation |
| 🟠 Orange | Currently being explored |
| ⚪ Gray | Not yet learned |
| 🔴 Red | Dangerous element (will be skipped) |

## Link Indicators

- **↗** Arrow at top-right: Element navigates TO another screen
- **•** Dot at bottom-left: Element was reached FROM another screen

## Verbosity Levels

1. **MINIMAL** - Color boxes only (fastest)
2. **STANDARD** - Color + truncated VUID (8 chars)
3. **VERBOSE** - Full info: VUID, display name, navigation links

## Controls

The FloatingProgressWidget has two debug buttons:

- **Eye icon (👁)** - Toggle overlay visibility on/off
- **Notes icon (📝)** - Cycle through verbosity levels

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    LearnAppIntegration                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │           ExplorationDebugCallback                       ││
│  │  - onScreenExplored(elements, screenHash, ...)          ││
│  │  - onElementNavigated(elementKey, destinationHash)      ││
│  │  - onProgressUpdated(progress)                          ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────────┐│
│  │            FloatingProgressWidget                        ││
│  │  - Debug toggle button (eye icon)                       ││
│  │  - Verbosity button (notes icon)                        ││
│  │  - getDebugOverlayManager()                             ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────────┐│
│  │            DebugOverlayManager                           ││
│  │  - show() / hide() / toggle()                           ││
│  │  - updateElements(...)                                   ││
│  │  - cycleVerbosity()                                      ││
│  │  - recordNavigation(...)                                 ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────────┐│
│  │            LearnAppDebugOverlay (View)                   ││
│  │  - Draws colored boxes around elements                  ││
│  │  - Renders legend at bottom                             ││
│  │  - Custom Canvas drawing                                ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

1. **ExplorationEngine** fires callbacks when:
   - Screen is explored → `onScreenExplored(elements, screenHash, ...)`
   - Navigation occurs → `onElementNavigated(elementKey, destinationHash)`
   - Progress updates → `onProgressUpdated(progress)`

2. **LearnAppIntegration** receives callbacks and forwards to **DebugOverlayManager**

3. **DebugOverlayManager** converts `ElementInfo` to `DebugElementState` and updates the overlay

4. **LearnAppDebugOverlay** redraws with new state

## Files

| File | Purpose |
|------|---------|
| `DebugOverlayState.kt` | Data models (DebugElementState, DebugScreenState, etc.) |
| `LearnAppDebugOverlay.kt` | Custom View that draws element highlights |
| `DebugOverlayManager.kt` | Lifecycle manager, state coordinator |
| `ExplorationEngine.kt` | Fires debug callbacks (ExplorationDebugCallback interface) |
| `LearnAppIntegration.kt` | Wires callback to overlay manager |
| `FloatingProgressWidget.kt` | UI controls for toggle/verbosity |

## Usage

Debug overlay is **automatically enabled** when exploration starts.

To manually control:

```kotlin
// Toggle overlay
floatingProgressWidget?.getDebugOverlayManager()?.toggle()

// Set verbosity
floatingProgressWidget?.getDebugOverlayManager()?.setVerbosity(DebugVerbosity.VERBOSE)

// Disable
floatingProgressWidget?.disableDebugOverlay()
```

## Created

2025-12-08 - VOS4 Development Team
