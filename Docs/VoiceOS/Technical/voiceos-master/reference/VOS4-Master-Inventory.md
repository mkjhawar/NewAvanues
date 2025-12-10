<!--
filename: VOS4-Master-Inventory.md
created: 2025-01-28 14:30:00 PST
author: VOS4 Development Team
purpose: Complete master inventory of VOS4 project components
last-modified: 2025-09-04 09:50:00 PDT
version: 1.2.0
changelog:
- 2025-09-04 09:50:00 PDT: Updated inventory to reflect current repository state, removed VoiceUING, added VoiceUIElements library
- 2025-01-29 09:30:00 PST: Updated with latest compilation status and renamed files
- 2025-01-28 14:30:00 PST: Initial creation following DOCUMENTATION-GUIDE standards
-->

# VOS4 Master Inventory

## 🚨 CRITICAL: Living Document Requirements
This document MUST be updated immediately when:
- Creating new files, classes, or functions
- Merging or consolidating code
- Discovering duplicates
- Changing module structure

## Overall Project Status (2025-09-04)

### Build Status
- **VoiceAccessibility**: ✅ BUILD SUCCESSFUL (0 errors)
- **SpeechRecognition**: ✅ BUILD SUCCESSFUL (0 errors)
- **VoiceCursor**: ❌ Has compilation errors (not yet addressed)
- **Main App**: ❌ Test configuration issues

### Recent Major Changes
1. **Repository Structure Update**: Removed VoiceUING (non-existent), maintained VoiceUIElements library
2. **Voice-First Architecture**: All modules now SDK-ready with intents and APIs
3. **Fixed 284 compilation errors** in VoiceAccessibility
4. **Removed 50+ deprecated files** with version suffixes (V2, V3, Refactored)
5. **Consolidated UIScrapingEngine** variants
6. **Integrated common components** across all speech engines
7. **Updated package structure**: com.augmentalis.voiceos.speech
8. **SOLID Principles**: Full adherence across all modules

### Architecture Compliance
- ✅ Zero-overhead principles enforced
- ✅ No version suffixes in class names
- ✅ Common components pattern implemented
- ✅ AIDL for cross-process communication
- ✅ Hybrid foreground service for Android 12+
- ✅ Voice-first, touch/gesture later interaction model
- ✅ SDK-ready modules with intents and API calls
- ✅ SOLID principles adherence

## Apps Directory
Location: `/Volumes/M Drive/Coding/vos4/apps/`

### VoiceAccessibility
- **Location**: `/apps/VoiceAccessibility/`
- **Purpose**: Main accessibility service with voice control and UI automation
- **Status**: ✅ BUILD SUCCESSFUL - All compilation errors resolved
- **Key Services**:
  - VoiceAccessibilityService (main service)
  - VoiceOSService (alternative implementation)
  - VoiceOSAccessibility (alternative implementation with hybrid foreground service)
  - VoiceOnSentry (foreground service for Android 12+)
- **Recent Changes**:
  - Fixed all 284 compilation errors
  - Resolved JVM signature conflicts (isVisible, cursorManager, dragPositionFlow)
  - Removed UIScrapingEngineV2 (consolidated into UIScrapingEngine)
  - Updated imports: speechrecognition.api → voiceos.speech.api

### VoiceCursor
- **Location**: `/apps/VoiceCursor/`
- **Purpose**: Voice-controlled cursor overlay system
- **Status**: Active
- **Key Services**:
  - VoiceCursorOverlayService (foreground service)
  - VoiceCursorAccessibilityService (gesture dispatch)

### VoiceRecognition
- **Location**: `/apps/VoiceRecognition/`
- **Purpose**: AIDL-based cross-app voice recognition service
- **Status**: Active
- **Key Service**: VoiceRecognitionService

### VoiceUI
- **Location**: `/apps/VoiceUI/`
- **Purpose**: Legacy voice UI (being replaced by VoiceUING)
- **Status**: Deprecated, reference only

## Libraries Directory
Location: `/Volumes/M Drive/Coding/vos4/libraries/`

### SpeechRecognition
- **Location**: `/libraries/SpeechRecognition/`
- **Purpose**: Multi-engine speech recognition library
- **Status**: ✅ BUILD SUCCESSFUL - Fully refactored with common components
- **Engines** (in `/voiceos/speech/engines/`):
  - VivokaEngine (VSDK-based) - Using VoiceStateManager
  - VoskEngine (offline recognition) - Using VoiceStateManager
  - WhisperEngine (OpenAI Whisper) - Using VoiceStateManager
  - AndroidSTTEngine (Android native) - Using VoiceStateManager
  - GoogleCloudEngine (Google Cloud Speech) - Using ErrorRecoveryManager
- **Common Components** (in `/voiceos/speech/engines/common/`):
  - VoiceStateManager (centralized voice state)
  - CommandProcessor (command processing)
  - ErrorRecoveryManager (error handling)
  - AudioStateManager (audio state management)
  - ResultProcessor (result normalization)
- **Recent Changes**:
  - Removed GoogleCloudSpeechLite (unused)
  - Removed 50+ deprecated files with version suffixes
  - Consolidated all engines to use common components
  - Fixed package structure: com.augmentalis.voiceos.speech

### DeviceManager
- **Location**: `/libraries/DeviceManager/`
- **Purpose**: Device capability and hardware management
- **Status**: Active

### UUIDManager
- **Location**: `/libraries/UUIDManager/`
- **Purpose**: UUID generation and management
- **Status**: Active

### VoiceUIElements
- **Location**: `/libraries/VoiceUIElements/`
- **Purpose**: Reusable voice-first UI components following SOLID principles
- **Status**: Active
- **Key Features**:
  - Voice-first interaction patterns
  - Touch and gesture as secondary inputs
  - SDK-ready components with exposed intents
  - Modular theme system
  - Component configuration via JSON

### VoiceUI (Library)
- **Location**: `/libraries/VoiceUI/`
- **Purpose**: Legacy voice UI library components
- **Status**: Deprecated, maintained for compatibility

## Managers Directory
Location: `/Volumes/M Drive/Coding/vos4/managers/`

### HUDManager
- **Location**: `/managers/HUDManager/`
- **Package**: `com.augmentalis.hudmanager`
- **Purpose**: Head-up display and gaze tracking management
- **Status**: Active
- **Key Classes**:
  - HUDManager (main manager)
  - GazeTracker (gaze tracking)
  - GazeTarget (gaze target data)
  - UIElement (UI element representation)
  - Spatial classes in `hudmanager.spatial` package

### VoiceDataManager
- **Location**: `/managers/VoiceDataManager/`
- **Purpose**: ObjectBox database management for voice data
- **Status**: Active
- **Uses**: ObjectBox with KAPT annotation processing

### CommandManager
- **Location**: `/managers/CommandManager/`
- **Purpose**: Central command processing and routing
- **Status**: Active
- **SDK Features**: Voice command API endpoints

### LicenseManager
- **Location**: `/managers/LicenseManager/`
- **Purpose**: License management and validation
- **Status**: Active

### LocalizationManager
- **Location**: `/managers/LocalizationManager/`
- **Purpose**: Internationalization and localization
- **Status**: Active

## Key Classes by Module

### VoiceAccessibility Classes

#### Handlers (implements ActionHandler interface)
- SystemHandler - System navigation
- GazeHandler - Gaze tracking integration
- GestureHandler - Touch gestures
- DragHandler - Drag operations
- NavigationHandler - UI navigation
- UIHandler - UI element interaction
- InputHandler - Text input
- AppHandler - Application management
- DeviceHandler - Device control
- BluetoothHandler - Bluetooth operations
- NumberHandler - Numbered selection
- SelectHandler - Selection modes
- HelpMenuHandler - Help system

#### Managers
- ActionCoordinator - Central command routing
- AppCommandManager/V2 - Application control
- CursorManager - Cursor integration
- DynamicCommandGenerator - Runtime commands

#### UI Components
- BaseOverlay - Base overlay class
- NumberOverlay - Number labels
- VoiceStatusOverlay - Status display
- CommandLabelOverlay - Command labels
- GridOverlay - Grid navigation
- CursorMenuOverlay - Cursor menu
- CommandDisambiguationOverlay - Command clarification

#### State Management
- DialogStateMachine - Dialog state
- UIState - UI state tracking

#### Extractors
- UIScrapingEngine - UI analysis (merged from V2/V3)

### SpeechRecognition Common Classes
- VoiceStateManager - Voice state management
- CommandProcessor - Command processing
- ErrorRecoveryManager - Error recovery
- ServiceState - Service state
- TimeoutManager - Timeout management
- PerformanceMonitor - Performance tracking
- LearningSystem - Learning system
- ResultProcessor - Result processing
- CommandCache - Command caching
- AudioStateManager - Audio state

## Import Corrections Required

### Correct Package Paths
```kotlin
// VoiceAccessibility imports
import com.augmentalis.voiceos.speech.api.SpeechListenerManager  // NOT speechrecognition.api
import com.augmentalis.voiceos.speech.api.RecognitionResult     // NOT speechrecognition.api
import androidx.lifecycle.ProcessLifecycleOwner                  // Android framework
import androidx.lifecycle.DefaultLifecycleObserver              // Android framework

// HUDManager imports
import com.augmentalis.hudmanager.HUDManager                    // NOT hud package
import com.augmentalis.hudmanager.spatial.GazeTracker
import com.augmentalis.hudmanager.spatial.GazeTarget
```

### Services to Use
- **Foreground Service**: VoiceOnSentry (NOT VoiceOSForegroundService)
- **UI Scraping**: UIScrapingEngine (NOT UIScrapingEngineV2/V3)

## Naming Violations to Fix
- NO version suffixes allowed (V2, V3, New, Enhanced, Refactored)
- NO "Common" prefix (use package structure instead)
- Direct implementations preferred over interfaces

## Duplication Areas Identified

### Already Fixed
- Voice state management consolidated to VoiceStateManager
- Error handling consolidated to ErrorRecoveryManager
- Command processing can use CommandProcessor
- Service state uses ServiceState

### Still Needs Work
- AndroidSTTEngine uses custom timeout instead of TimeoutManager
- Some engines not fully using CommandProcessor

## TODO Integration Points
- Complete AndroidSTTEngine TimeoutManager integration
- Ensure all engines use CommandProcessor
- Verify all compilation errors resolved
- Update module-specific inventories

## Build Configuration
- Gradle with Kotlin DSL
- ObjectBox for persistence (requires KAPT)
- Android API 31+ for Android 12 features
- androidx.lifecycle:lifecycle-process:2.6.2 for ProcessLifecycleOwner

## Testing Structure
- Unit tests in `src/test/`
- Integration tests in `src/androidTest/`
- Performance benchmarks included
- Chaos engineering tests for reliability

---
**Note**: This is a living document. Update immediately when making changes to the codebase.