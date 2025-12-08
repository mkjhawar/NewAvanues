# Changelog - VoiceOS Accessibility Module

All notable changes to the VoiceOS Accessibility module will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.5] - 2025-01-28

### Fixed
- **Build Configuration Error**: Resolved "This application is not configured to use dynamic features" error
  - **Root Cause**: App-to-app dependency `:apps:VoiceRecognition` was incorrectly included in build.gradle.kts
  - **Solution**: Removed direct app module dependency; Android build system interprets app-to-app dependencies as dynamic feature modules
  - **Architecture**: Maintained shared code access through `:libraries:SpeechRecognition` library dependency
  - **Communication**: Apps now properly communicate via AIDL service binding (correct pattern for cross-app interaction)

- **Packaging Conflicts**: Resolved META-INF duplicate file conflicts during APK assembly
  - **Exclusions Added**: META-INF/DEPENDENCIES, META-INF/LICENSE*, META-INF/NOTICE*, META-INF/INDEX.LIST
  - **Impact**: Multiple dependencies containing identical META-INF files no longer cause build failures
  - **Technical**: Added comprehensive packaging exclusions block to android configuration

### Technical Details
- **Build System Rule**: App modules cannot depend on other app modules in Android
- **Correct Pattern**: Libraries can depend on libraries, apps can depend on libraries
- **Inter-App Communication**: AIDL service binding for cross-app functionality
- **Verification**: APK builds successfully with `./gradlew :apps:VoiceAccessibility:assembleDebug`

## [1.0.4] - 2025-08-28

### Major Enhancement - Voice Recognition Integration

#### Added
- **AIDL Voice Integration**: Complete integration with VoiceRecognition service
  - **VoiceRecognitionBinder**: AIDL client wrapper for service communication
  - **VoiceRecognitionManager**: High-level integration manager
  - **Voice Command Processing**: Real-time voice-to-command conversion
  - **Multi-Engine Support**: Integration with Google STT, Vivoka, and Cloud engines

- **Voice Command Support**: Comprehensive voice command categories
  - **System Commands**: "go back", "go home", "recent apps", "take screenshot"
  - **App Commands**: "open [app name]", "close app", "switch to [app]"
  - **UI Commands**: "click [element]", "scroll down", "type [text]"
  - **Device Commands**: "volume up", "turn on wifi", "increase brightness"

- **Service Binding Architecture**: Standard Android service binding
  - Automatic service connection and recovery
  - Callback-based recognition result handling
  - State management and error recovery
  - Connection timeout handling with retry logic

#### Enhanced
- **ActionCoordinator Integration**: Voice recognition results route through existing command system
  - Voice commands processed through same handler architecture
  - Maintains all existing performance optimizations (Fast Path, Caching, etc.)
  - Recognition confidence threshold filtering
  - Real-time partial result processing

- **Architecture Documentation**: Updated system architecture diagrams
  - Added Voice Integration Layer to architecture overview
  - AIDL IPC communication flow diagrams
  - Voice command processing flow documentation
  - Service binding relationship diagrams

#### Technical Implementation
- **AIDL Integration**: 
  - Uses IVoiceRecognitionService and IRecognitionCallback interfaces
  - RemoteCallbackList for multi-client callback management
  - Secure inter-process communication with error handling
  - Recognition state synchronization between services

- **Voice Processing Flow**:
  ```
  Voice Input → VoiceRecognition Service → AIDL IPC → 
  VoiceAccessibility → ActionCoordinator → Handler Execution
  ```

- **Error Handling**: Comprehensive voice recognition error recovery
  - Speech timeout recovery with automatic restart
  - No match detection with retry logic
  - Engine failure fallback mechanisms
  - Connection loss recovery with exponential backoff

#### Integration Benefits
- **Unified Command Processing**: Voice and traditional commands use same execution paths
- **Performance Consistency**: Voice commands benefit from Fast Path routing and caching
- **Extensibility**: Existing custom handlers automatically support voice commands
- **Reliability**: AIDL service binding provides robust cross-process communication

## [1.0.3] - 2025-08-28

### Major Enhancement - Library to Full Application Conversion

#### Added
- **Complete Android Application**: Converted from library module to standalone installable app
  - Full `com.android.application` configuration with applicationId: `com.augmentalis.voiceaccessibility`
  - Launcher icon and app entry in device app drawer
  - Standalone APK installation capability

- **Modern UI Interface**: Comprehensive glassmorphism-based user interface
  - **MainActivity**: Main dashboard with service status, quick actions, and navigation
  - **AccessibilitySettings**: Comprehensive settings screen with real-time configuration
  - **CommandTestingScreen**: Built-in command testing interface with feedback
  - **ARVision Design**: VoiceCursor-inspired glassmorphism theming matching VOS4 standards

- **App-Level Features**:
  - Real-time service status monitoring with visual indicators  
  - Automated permission request flows (Accessibility, Overlay, Audio)
  - Interactive service enable/disable functionality
  - Live command testing with success/failure feedback
  - Performance metrics display with real-time updates
  - Command history logging with timestamps

- **Enhanced User Experience**:
  - Intuitive service management without manual Settings navigation
  - Visual feedback for all accessibility service operations
  - Streamlined onboarding process for new users
  - Advanced debugging tools and performance monitoring

#### Enhanced
- **Service Integration**: Seamless integration between app UI and VoiceAccessibilityService
  - Service state synchronization with UI
  - Configuration changes applied instantly to background service  
  - Service continues operation when app is closed
  - API remains fully accessible for external integration

- **Documentation**: Complete documentation update reflecting app status
  - Updated README.md with app-specific features and UI screens
  - Installation instructions for standalone app deployment
  - App feature documentation with UI component descriptions
  - Updated API reference for both app and service usage

#### Technical Details
- **Build System**: Migrated from `com.android.library` to `com.android.application`
- **Dependencies**: Added Jetpack Compose UI framework with material3 components
- **Architecture**: MVVM pattern with ViewModel state management
- **Theme System**: Complete AccessibilityTheme implementation with glassmorphism effects
- **Navigation**: Navigation between main, settings, and testing screens
- **Backwards Compatibility**: All existing API functionality preserved for integration use

## [1.0.2] - 2025-08-28

### Refactored
- **Service Naming**: Cleaned up redundant and confusing naming patterns
  - Renamed main service file from `VoiceAccessibility.kt` to `AccessibilityService.kt`
  - Renamed service class from `VoiceAccessibility` to `AccessibilityService`
  - Updated all import statements and references across the codebase
  - Updated AndroidManifest.xml service declaration
  - Renamed `VoiceAccessibilityTheme` to `AccessibilityTheme` for consistency
  - Updated documentation titles to use "VoiceOS Accessibility" for display names
  - Maintained package structure as `com.augmentalis.voiceaccessibility`

## [1.0.1] - 2025-01-28

### Fixed
- **InputHandler.kt**: Removed unused variable warnings
  - Fixed unused `focusedNode` variable in `performUndo()` method (line 217)
  - Fixed unused `focusedNode` variable in `performRedo()` method (line 225)
  - Both methods now properly check for focused node existence without storing in unused variable

### Verified
- **CursorManager.kt**: Confirmed proper handling of deprecated Android APIs
  - Properly uses `WindowMetrics` API for Android R (API 30) and above
  - Correctly falls back to deprecated `defaultDisplay` and `getSize()` methods for older versions
  - All deprecation warnings properly suppressed with `@Suppress("DEPRECATION")` annotations
  - Correctly handles `TYPE_APPLICATION_OVERLAY` vs deprecated `TYPE_PHONE` based on Android version

### Code Quality
- Eliminated all compilation warnings related to unused variables
- Ensured backward compatibility while using modern Android APIs where available
- Maintained consistent code style and documentation

## [1.0.0] - 2025-01-27

### Added
- Initial release of VoiceAccessibility module
- Merged best features from VoiceAccessibility and VoiceAccessibility-HYBRID implementations
- Implemented fast path routing for common commands (70-80% performance improvement)
- Added SR6-HYBRID configuration patterns for advanced customization
- Complete handler architecture with 6 specialized handlers:
  - SystemHandler: System-level commands and navigation
  - AppHandler: Application management and launching
  - DeviceHandler: Device settings and controls
  - InputHandler: Text input and keyboard management
  - NavigationHandler: UI navigation and scrolling
  - UIHandler: UI element interaction
- Manager components for advanced functionality:
  - ActionCoordinator: Intelligent command routing
  - CursorManager: Visual cursor for precise selection
  - DynamicCommandGenerator: Context-aware command generation
  - AppCommandManager: App-specific command management
- Performance optimization features:
  - Lazy initialization reducing memory usage by 50-70%
  - Command caching with LRU strategy
  - Coroutine-based async operations
  - WeakReference for service instance management
- Comprehensive documentation for all skill levels
- Full API reference with examples
- Performance metrics and benchmarking tools

### Technical Details
- **Architecture**: Direct implementation pattern (VOS4 compliant)
- **Minimum SDK**: Android 9.0 (API 28)
- **Language**: Kotlin 1.9.0+
- **Build System**: Gradle 8.0+
- **Interface Exception**: ActionHandler interface approved for polymorphic dispatch

## Version Guidelines

### Version Number Format
- **MAJOR**: Incompatible API changes
- **MINOR**: Backwards-compatible functionality additions
- **PATCH**: Backwards-compatible bug fixes

### Branch Strategy
- `main`: Stable releases
- `develop`: Active development
- `feature/*`: New features
- `bugfix/*`: Bug fixes
- `hotfix/*`: Critical production fixes

---

**Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC**
