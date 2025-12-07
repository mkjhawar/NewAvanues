# Issue #2: Module Migration Continuity Document
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA
**Created:** 2025-01-19
**Purpose:** Maintain context for module migration from VOS2 to VOS3

## Current Situation

### Modules to Migrate from VOS2

#### 1. **DeviceInfo Module** (/vos2/modules/DeviceInfo)
- **Status**: EXISTS in VOS2, STUB in VOS3
- **Key Classes**:
  - `DeviceInfoProvider.kt` - Main device information provider
  - `DpiCalculator.kt` - DPI calculation for displays
  - `DeviceProfile.kt` - Device profile model
  - `DisplayProfile.kt` - Display characteristics
  - `HardwareModels.kt` - Hardware model definitions
  - `ScalingProfile.kt` - UI scaling profiles
- **Action**: Port directly to VOS3 with api package structure

#### 2. **UIKit Module** (/vos2/modules/UIKit)
- **Status**: EXISTS in VOS2, STUB in VOS3
- **Key Classes**:
  - `BaseViewModel.kt` - Base ViewModel implementation
  - `VoiceFirstComponents.kt` - Voice-optimized UI components
  - `NavigationCoordinator.kt` - Navigation management
  - `StateManager.kt` - State management
  - `ThemeManager.kt` - Theme management
- **Action**: Port to VOS3 with self-contained architecture

#### 3. **UIBlocks Module** (/vos2/modules/UIBlocks)
- **Status**: EXISTS in VOS2, NOT in VOS3
- **Note**: This is an implementation of UI elements for UIKit
- **Key Classes**:
  - `UIKitApi.kt` - Public API
  - `SpatialButton.kt`, `SpatialCard.kt`, `SpatialTextField.kt` - Base components
  - `SpatialBottomSheet.kt`, `SpatialDialog.kt` - Containers
  - `DpiAwareTheme.kt` - DPI-aware theming
  - `GlassMorphism.kt`, `SpatialTheme.kt` - visionOS-style theming
- **Action**: Create new module in VOS3 or merge with UIKit

#### 4. **UpdateSystem Module** (/vos2/modules/UpdateSystem)
- **Status**: EXISTS in VOS2, STUB in VOS3
- **Key Classes**:
  - `UpdateManager.kt` - Main update orchestrator
  - `UpdateChecker.kt` - Check for updates
  - `UpdateDownloader.kt` - Download updates
  - `UpdateInstaller.kt` - Install updates
  - `RollbackManager.kt` - Rollback functionality
- **Action**: Port to VOS3 with enhanced security

#### 5. **CommunicationSystems Module** (/vos2/modules/CommunicationSystems)
- **Status**: EXISTS in VOS2 as CommunicationSystems, STUB in VOS3 as Communication
- **Key Classes**:
  - `EventBus.kt` - Inter-module communication
  - `AuthenticationManager.kt` - Auth management
  - `NetworkClient.kt` - Network operations
  - `NetworkMonitor.kt` - Network state monitoring
  - `WebSocketManager.kt` - WebSocket connections
- **Action**: Port to VOS3, rename to match VOS3 naming

#### 6. **Localization Module**
- **Status**: Embedded in AccessibilityService in VOS2, Basic stub in VOS3
- **Current Implementation in VOS2**:
  - CSV-based localization files (commands_*.csv, messages_*.csv)
  - Languages: en-US, es-MX, fr-CA, zh-CN
  - `LocalizationTasks.kt` - Build tasks for localization
  - `LocalizedStrings.kt` - Generated localized strings
  - `SupportedLanguages.kt` - Language definitions
- **Action**: Create comprehensive standalone module

### Modules to be Provided Later
- **Browser** - Will be provided
- **FileManager** - Will be provided  
- **Keyboard** - Will be provided
- **Launcher** - Will be provided

## Migration Strategy

### For Each Module:
1. Create `api` package with public interfaces
2. Move implementation to `internal` package
3. Ensure self-contained compilation
4. Add module-specific PRD.md and DEVELOPER.md
5. Update dependencies to use VOS3 core

### Module Dependencies Map
```
DeviceInfo → Core
UIKit → Core, DeviceInfo
UIBlocks → UIKit, Core
UpdateSystem → Core, CommunicationSystems
CommunicationSystems → Core
Localization → Core
```

## Localization Module Recommendation

### Option A: Comprehensive Localization Module (Recommended)
Create a full localization module that:
- Supports 42+ languages (as per requirements)
- Dynamic language loading
- CSV and JSON format support
- Build-time string generation
- Runtime language switching
- Pluralization support
- Date/time formatting
- Number formatting
- RTL support

### Option B: Basic Port from VOS2
- Port existing CSV-based system
- Support 4 languages initially
- Add more languages incrementally

### Option C: Use Android Resources
- Leverage Android's built-in localization
- Less flexible for dynamic loading
- Simpler implementation

**Recommendation**: Option A - Build comprehensive localization module to support VOS3's 42-language requirement

## Next Steps

1. **Immediate Actions**:
   - Port DeviceInfo module
   - Port UIKit module
   - Decide on UIBlocks (separate or merge)
   - Port UpdateSystem module
   - Port CommunicationSystems module

2. **Localization Decision**:
   - Implement comprehensive localization module
   - Support CSV import from VOS2
   - Add JSON support for modern tooling
   - Implement build-time optimization

3. **Documentation**:
   - Create PRD for each migrated module
   - Create DEVELOPER.md with migration notes
   - Update integration documentation

## Module Status Summary

| Module | VOS2 Status | VOS3 Status | Action Required |
|--------|------------|-------------|-----------------|
| DeviceInfo | Complete | Stub | Port from VOS2 |
| UIKit | Complete | Stub | Port from VOS2 |
| UIBlocks | Complete | Missing | Create new/merge |
| UpdateSystem | Complete | Stub | Port from VOS2 |
| CommunicationSystems | Complete | Stub (as Communication) | Port and rename |
| Localization | Embedded | Basic stub | Create comprehensive |
| Browser | N/A | Stub | Wait for code |
| FileManager | N/A | Stub | Wait for code |
| Keyboard | N/A | Stub | Wait for code |
| Launcher | N/A | Stub | Wait for code |

## Technical Notes

### VOS2 Module Structure
- Uses Dagger/Hilt for DI
- Gradle-based build system
- Some modules use AAR libraries (Vivoka)
- ObjectBox for data persistence

### VOS3 Requirements
- Self-contained modules
- Can compile as standalone apps
- No circular dependencies
- EventBus for loose coupling
- ObjectBox mandatory for data

## Files to Reference After Compact

This document contains all necessary context for Issue #2 module migration. Reference this after any memory compaction to continue work on:
1. Module porting from VOS2
2. Localization module implementation
3. Documentation creation for migrated modules