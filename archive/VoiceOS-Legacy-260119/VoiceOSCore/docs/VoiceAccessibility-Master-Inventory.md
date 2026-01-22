<!--
filename: VoiceAccessibility-Master-Inventory.md
created: 2025-01-28 14:50:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Complete inventory of VoiceAccessibility module components
last-modified: 2025-01-28 14:50:00 PST
version: 1.0.0
-->

# VoiceAccessibility Master Inventory

## Changelog
<!-- Most recent first -->
- 2025-01-28 14:50:00 PST: Initial creation following DOCUMENTATION-GUIDE standards

## Module Overview
- **Location**: `/Volumes/M Drive/Coding/vos4/apps/VoiceAccessibility/`
- **Package**: `com.augmentalis.voiceos.accessibility`
- **Purpose**: Android accessibility service providing voice control and UI automation
- **Status**: Active development, fixing compilation errors

## Directory Structure
```
VoiceAccessibility/
├── src/main/
│   ├── aidl/           # AIDL interfaces
│   ├── java/com/augmentalis/voiceos/accessibility/
│   │   ├── config/      # Configuration classes
│   │   ├── extractors/  # UI extraction engines
│   │   ├── handlers/    # Action handlers
│   │   ├── managers/    # High-level managers
│   │   ├── recognition/ # Voice recognition
│   │   ├── service/     # Core services
│   │   ├── state/       # State management
│   │   ├── ui/          # User interface
│   │   └── viewmodel/   # ViewModels
│   └── res/             # Resources
├── docs/                # Module documentation
└── build.gradle.kts     # Build configuration
```

## Services

### Core Services
1. **VoiceAccessibilityService** (`service/VoiceAccessibilityService.kt`)
   - Main accessibility service implementation
   - Lazy-loaded components for performance
   
2. **VoiceOSService** (`VoiceOSService.kt`)
   - Alternative service implementation
   - Hybrid foreground service approach
   
3. **VoiceOSAccessibility** (`service/VoiceOSAccessibility.kt`)
   - Another alternative implementation
   
4. **VoiceOnSentry** (`VoiceOnSentry.kt`)
   - Foreground service for Android 12+ microphone access
   - Only runs when needed (background + voice active)

## Handlers (implements ActionHandler interface)

All in `handlers/` package:

1. **SystemHandler** - System navigation (back, home, notifications)
2. **GazeHandler** - Gaze tracking with HUDManager integration
3. **GestureHandler** - Touch gestures (swipe, pinch)
4. **DragHandler** - Drag operations
5. **NavigationHandler** - UI navigation and scrolling
6. **UIHandler** - UI element interaction
7. **InputHandler** - Text input and keyboard control
8. **AppHandler** - Application launching
9. **DeviceHandler** - Device control (volume, brightness)
10. **BluetoothHandler** - Bluetooth operations
11. **NumberHandler** - Numbered element selection
12. **SelectHandler** - Selection modes
13. **HelpMenuHandler** - Help system

## Managers

All in `managers/` package:

1. **ActionCoordinator** - Central command routing system
2. **AppCommandManager** - Application launching (v1)
3. **AppCommandManagerV2** - Enhanced app control
4. **CursorManager** - Cursor functionality integration
5. **DynamicCommandGenerator** - Runtime command generation

## UI Components

### Overlays (`ui/overlays/`)
1. **BaseOverlay** - Base class for all overlays
2. **NumberOverlay** - Number labels for elements
3. **VoiceStatusOverlay** - Voice recognition status
4. **CommandLabelOverlay** - Command labels
5. **GridOverlay** - Grid navigation
6. **CursorMenuOverlay** - Cursor context menu
7. **HelpOverlay** - Help system
8. **CommandDisambiguationOverlay** - Command clarification

### Screens (`ui/screens/`)
1. **SettingsScreen** - Configuration UI
2. **CommandTestingScreen** - Command testing

### Main UI (`ui/`)
1. **MainActivity** - Main app entry
2. **AccessibilityDashboard** - Service monitoring
3. **AccessibilitySettings** - Settings management

## State Management (`state/`)
1. **DialogStateMachine** - Dialog flow control
2. **UIState** - UI state tracking

## Configuration (`config/`)
1. **ServiceConfiguration** - Complete service config with SR6-HYBRID patterns

## Extractors (`extractors/`)
1. **UIScrapingEngine** - Advanced UI analysis (merged from V2/V3)

## Recognition (`recognition/`)
1. **VoiceRecognitionManager** - Integration with AIDL service
2. **VoiceRecognitionBinder** - Service binding

## ViewModels (`viewmodel/`)
1. **MainViewModel** - Main activity state
2. **AccessibilityViewModel** - Service state
3. **SettingsViewModel** - Configuration management
4. **PerformanceMode** - Performance mode enum

## AIDL Interfaces
1. **IVoiceRecognitionService.aidl** - Main service interface
2. **IRecognitionCallback.aidl** - Callback interface
3. **RecognitionData.aidl** - Data transfer objects

## Dependencies
- androidx.lifecycle:lifecycle-process:2.6.2
- Jetpack Compose UI libraries
- Coroutines
- Android Accessibility framework

## Known Issues (Being Fixed)
1. JVM signature clashes (getCursorManager, getDragPositionFlow, isVisible)
2. Need to complete common component integration

## Naming Violations Fixed
- Removed UIScrapingEngineV2/V3 references
- Using UIScrapingEngine (merged version)
- No "Common" prefixes
- No version suffixes

## Integration Points
- SpeechRecognition library for voice input
- HUDManager for gaze tracking
- VoiceDataManager for persistence
- AIDL for cross-app communication