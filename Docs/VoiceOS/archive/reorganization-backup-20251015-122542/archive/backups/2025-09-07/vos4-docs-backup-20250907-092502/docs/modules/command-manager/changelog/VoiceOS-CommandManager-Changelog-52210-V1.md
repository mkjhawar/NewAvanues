# CommandManager Module Changelog

## [1.2.1] - 2025-09-06

### Fixed
- **Compilation Warnings**: Resolved additional compilation warnings
  - Added @Suppress annotations for intentional unused parameters in CursorActions UUID methods
  - Added @Suppress annotations for intentional unused parameters in GestureActions stub methods  
  - Added @Suppress annotations for unused context parameter in CommandValidator
  - Fixed remaining deprecated network API warnings in SystemActions with proper suppressions

### Changed
- **Code Quality**: Improved warning suppression documentation
  - All intentional stub parameters now properly documented with @Suppress annotations
  - Clear separation between actual issues and architectural placeholders
  - Better code maintainability for future UUIDManager integration

### Technical Details
- UUID action methods: parameters preserved for future UUIDManager integration
- Gesture action handlers: parameters preserved for future delegation implementation
- Security validation: context parameter preserved for future context-aware validation
- All changes maintain 100% functional equivalency

## [1.2.0] - 2025-09-06

### Fixed
- **Deprecated API Usage**: Fixed 43 deprecation warnings across multiple action classes
  - Updated display API usage to use WindowMetrics for Android 11+ with fallback for older versions
  - Fixed versionCode deprecation by using longVersionCode for API 28+
  - Updated input method management for Android 12+ compatibility
  - Added version-aware Bluetooth enable/disable for Android 13+
  - Replaced getRunningTasks with UsageStatsManager for Android 5.1+
  
### Changed
- **Code Quality Improvements**: Fixed all unused parameter and variable shadowing warnings
  - Renamed lambda parameter in AppActions.kt to avoid variable shadowing
  - Removed unused activeNetwork variable in SystemActions.kt
  - Documented intentionally unused parameters in stub implementations
  
### Added
- **Version-Aware API Handling**: Implemented comprehensive API level checks
  - Added Build.VERSION.SDK_INT checks for all deprecated APIs
  - Proper @Suppress("DEPRECATION") annotations for legacy fallbacks
  - Graceful degradation for older Android versions (minimum API 28)
  
### Technical Details
- Created unified getScreenDimensions() helper methods in CursorActions, DragActions, and ScrollActions
- Implemented modern/legacy dual-path pattern for API compatibility
- All changes maintain 100% functional equivalency
- Zero breaking changes to existing API contracts

### Affected Files
- `/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/AppActions.kt`
- `/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/CursorActions.kt`
- `/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/DragActions.kt`
- `/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/ScrollActions.kt`
- `/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/DictationActions.kt`
- `/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/SystemActions.kt`
- `/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/ContextManager.kt`

## [1.1.0] - 2025-09-04

### Changed
- **Build System Updates**: Updated Kotlin/Compose compatibility for Kotlin 1.9.25
  - Applied Kotlin Compose Compiler 1.5.15 for stable builds
  - Aligned all dependencies with system-wide standards
- **Naming Convention Compliance**: Verified and maintained VOS4 naming standards
  - Confirmed no prohibited suffixes in any classes
  - Enhanced code consistency across the module
- **ObjectBox Integration**: Applied ObjectBox stub workaround where applicable
  - Ensures stable compilation for command caching functionality
  - Maintains performance and reliability

### Enhanced
- **Test Infrastructure**: Updated test dependencies with standardized versions
  - Added proper mockito-kotlin and coroutines-test support
  - Enhanced testing capabilities for command processing
- **Documentation**: Updated module documentation to reflect current architecture
- **Performance**: Build times improved through dependency alignment

### Technical Details
- Kotlin Compose Compiler version aligned with 1.5.15
- Test dependencies standardized across all modules
- Build configuration optimized for performance

## [1.0.0] - 2025-01-23

### Changed
- **Module Rename**: Renamed from `CommandsManager` to `CommandManager` for consistency
- **Namespace Migration**: Migrated from deprecated `com.ai.*` to `com.augmentalis.commandmanager.*`
- **Directory Structure**: Reorganized module structure to match new naming convention
- **Dependencies**: Verified coroutines dependencies are properly configured

### Fixed
- Resolved coroutines import issues
- Fixed package declarations throughout the module
- Updated all module references in dependent modules

### Technical Details
- All 21 Kotlin files updated with new package namespace
- Build configuration updated in `build.gradle.kts`
- Settings.gradle.kts updated to reflect new module name
- Removed duplicate legacy directories (CommandsMGR, etc.)

### Affected Files
- `/managers/CommandManager/` - Main module directory (renamed)
- `/app/build.gradle.kts` - Updated module reference
- `/apps/VoiceUI/build.gradle.kts` - Updated module reference
- `/app/src/main/java/com/augmentalis/voiceos/VoiceOS.kt` - Updated imports and property names
- `/settings.gradle.kts` - Updated module registration

### Verification
- Build successful with warnings only (deprecated API usage)
- All coroutines functionality working
- Module compiles without errors