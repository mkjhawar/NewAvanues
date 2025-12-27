# Changelog

All notable changes to VoiceOS will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **LearnApp:** Exploration hanging on infinite/dynamic scrollable content ([Fix Doc](fixes/VoiceOS-learnapp-infinite-scroll-timeout-fix-50712-V1.md))
  - Root cause: No timeout protection on scrollable containers with BOTH directions (horizontal + vertical)
  - Root cause: Dynamic content (Teams channels, social feeds) causing infinite scroll loops
  - Solution: Added multi-layer timeout protection:
    - 30s total / 10s per-container timeout in ScreenExplorer.collectAllElements()
    - Limited scroll iterations for BOTH direction (10 vertical, 5 horizontal)
    - 2-minute per-screen exploration timeout
    - 45s timeout for exploreScreen() calls
  - Impact: LearnApp no longer hangs on Teams, Instagram, and other dynamic content apps
  - Files changed: `ScreenExplorer.kt`, `ScrollExecutor.kt`, `ExplorationEngine.kt`
  - Severity: HIGH (complete exploration hang)

- **LearnApp:** Progress overlay not visible during exploration, no way to interrupt
  - Root cause: ProgressOverlayManager rendering below content, no STOP button
  - Solution: New FloatingProgressWidget with:
    - TYPE_ACCESSIBILITY_OVERLAY for proper z-ordering (above all content)
    - Draggable functionality (user can move anywhere on screen)
    - Semi-transparent background (92% opacity)
    - Pause/Resume and STOP buttons
  - Impact: Users can now see progress, pause/resume, or stop exploration at any time
  - Files changed: `FloatingProgressWidget.kt` (new), `floating_progress_widget.xml` (new), `LearnAppIntegration.kt`
  - Severity: MEDIUM (usability improvement)

- **VoiceOSCore:** Compilation errors from missing infrastructure classes (2025-12-22)
  - Root cause: Multiple missing classes after cleanup restoration (HashUtils, ScrapingMode, Command, CommandSource, Theme components)
  - Root cause: Untracked files existed but weren't in git
  - Root cause: AccessibilityTheme referenced in code but didn't exist
  - Root cause: Hilt annotations used but Hilt disabled in AccessibilityService
  - Solution: Systematic fix approach via parallel swarm agents:
    - Added untracked files to git (ScrapingMode.kt, HashUtils.kt, Command.kt, CommandSource.kt)
    - Created VoiceOSTheme with Material3 to replace AccessibilityTheme
    - Created GlassmorphismUtils for UI effects
    - Created comprehensive service infrastructure (6 files)
    - Created managers and utilities (3 files)
    - Created LearnWeb components (2 files)
    - Implemented manual DI via DatabaseProvider singleton
  - Impact: 200+ compilation errors resolved, full service infrastructure in place
  - Files affected: 39 files created/modified
  - Severity: HIGH (complete build failure)

### Fixed
- **VoiceOSCore:** Dynamic command real-time element search failure ([Fix Doc](docs/fixes/VoiceOSCore-dynamic-command-realtime-search-2025-11-13.md))
  - Root cause: Broken recursive node search logic + false success reporting in Tier 3
  - Solution: Kotlin extension functions for safe AccessibilityNodeInfo lifecycle + proper result checking
  - Impact: Dynamic commands now work on unscraped/partially-scraped apps
  - Files changed: `AccessibilityNodeExtensions.kt` (new), `VoiceCommandProcessor.kt`, `VoiceOSService.kt`
  - Severity: CRITICAL (complete feature breakdown)

- **VoiceOSCore:** Voice command flow - only first command processed after service enable ([Fix Doc](docs/fixes/VoiceOSCore-voice-command-flow-2025-11-13.md)) ([ADR-001](docs/architecture/decisions/ADR-001-stateflow-vs-sharedflow-for-voice-commands.md))
  - Root cause: StateFlow-based architecture didn't properly separate state from events
  - Solution: Added SharedFlow for command events while keeping StateFlow for engine state
  - Impact: All voice commands now properly reach VoiceOSService for processing
  - Files changed: `SpeechEngineManager.kt`, `VoiceOSService.kt`

- **VoiceOSCore:** Compilation errors in VoiceRecognitionManager and NodeRecyclingUtils ([Fix Doc](docs/fixes/VoiceOSCore-compilation-errors-2025-11-13.md))
  - Root cause: ConditionalLogger API ergonomics and recursive inline function
  - Solution: Added natural parameter order overload + iterative traversal algorithm
  - Impact: Code compiles, better API ergonomics, eliminated stack overflow risk
  - Files changed: `ConditionalLogger.kt`, `NodeRecyclingUtils.kt`
  - Documentation: Added inline functions guide and logging best practices

### Added
- **VoiceOSCore:** P3 Cleanup functionality for deprecated command management ([Plan Doc](plans/VoiceOS-Plan-Cleanup-5221200-V1.md))
  - **CleanupManager:** Core business logic with safety mechanisms
    - 90% deletion safety limit with preview/validation
    - Configurable grace period (1-365 days, default 30)
    - User-approved command preservation
    - Batch deletion with progress callbacks (1000 commands/batch)
    - Automatic VACUUM when >10% commands deleted
  - **CleanupWorker:** WorkManager background job for automated cleanup
    - Weekly schedule during device charging (battery not low)
    - Configurable grace period and user-approved preservation
    - Retry logic for transient failures
  - **CleanupPreviewUI:** Material3 Compose UI with comprehensive preview
    - Real-time statistics (commands to delete/preserve, apps affected)
    - Safety level indicator (SAFE/MODERATE/HIGH_RISK)
    - Affected apps breakdown with app names
    - Progress tracking during execution
  - **Manual Dependency Injection:** Custom DatabaseProvider singleton
    - Hilt unavailable in AccessibilityService context
    - ViewModelFactory pattern for ViewModel creation
  - **Comprehensive Test Coverage:** 40 unit tests targeting 90%+ coverage
    - Preview calculation accuracy, safety limit enforcement
    - Grace period validation, user-approved preservation
    - Batch deletion progress, VACUUM threshold trigger
    - Error handling and dry run mode
  - Files added:
    - `CleanupManager.kt`, `CleanupWorker.kt`, `DatabaseProvider.kt`
    - `CleanupPreviewUiState.kt`, `CleanupPreviewViewModel.kt`
    - `CleanupPreviewScreen.kt`, `CleanupPreviewActivity.kt`
    - `CleanupManagerTest.kt` (40 test cases)
  - Migration: SQLDelight database (Room → SQLDelight for KMP compatibility)
  - Integration:
    - Automatic scheduling via VoiceOSService.initializeVersionManagement() at line 610
    - Manual trigger via DeveloperSettingsActivity → "Cleanup Deprecated Commands" action
    - Weekly background execution during device charging (battery not low)
  - Impact: Enables safe automated cleanup of deprecated commands with user control

- **DeveloperSettingsActivity:** ACTION setting type for clickable actions ([Settings Doc](apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/settings))
  - New SettingType.ACTION for launching activities or triggering actions
  - SettingsAdapter implementation with support for TOGGLE, NUMBER_*, TEXT, ACTION types
  - "Cleanup Deprecated Commands" action launches CleanupPreviewActivity
  - Programmatic RecyclerView-based UI for dynamic settings

- **ConditionalLogger:** Natural parameter order overload for exception logging
  - New: `e(TAG, exception) { "message" }` with trailing lambda syntax
  - Backward compatible with existing `e(TAG, { "message" }, exception)` syntax
  - Follows Kotlin idioms and improves developer experience

- **Developer Manual:** Chapter 33.7.5 - Logging Best Practices
  - ConditionalLogger usage patterns with examples
  - Error logging with natural parameter order
  - Lazy evaluation benefits and performance impact
  - PII protection with PIILoggingWrapper
  - Common logging patterns (lifecycle, state, methods)
  - CI/CD enforcement documentation
  - Location: `docs/developer-manual/33-Code-Quality-Standards.md`

- **Developer Manual:** Chapter 33.7.6 - Inline Functions Best Practices
  - When to use/avoid inline functions with code examples
  - Visibility rules (public/internal required for accessed members)
  - Recursion limitations and iterative rewrites
  - Performance characteristics table
  - Code review checklist
  - Quick decision guide
  - Location: `docs/developer-manual/33-Code-Quality-Standards.md`

- **VoiceOSCore:** Infrastructure components for service management and coordination ([Developer Manual](manuals/developer/VoiceOS-Infrastructure-Components-Developer-Manual-5221201-V1.md))
  - **Service Infrastructure:** Core service coordination and management
    - `IVoiceOSService`: Public API interface for external service interactions
    - `IVoiceOSServiceInternal`: Internal component coordination API
    - `Const`: Centralized service configuration constants (channels, notifications, priorities, thresholds)
    - `Debouncer`: Coroutine-based event throttling (prevents event flooding, 300ms default delay)
    - `ResourceMonitor`: CPU/memory monitoring with health checks (/proc filesystem, 30s polling)
    - `EventPriorityManager`: Priority-based event queue with PriorityBlockingQueue (CRITICAL→HIGH→NORMAL→LOW)
  - **Managers & Utilities:**
    - `InstalledAppsManager`: App installation tracking with PackageManager + Flow-based observation
    - `AppVersionDetector`: Version change detection (upgrades, downgrades, new apps, reinstalls)
    - `RenameCommandHandler`: Voice command renaming with validation and TTS feedback
  - **AI & Semantic Analysis:**
    - `SemanticInferenceHelper`: Intent inference and command-element matching (login, signup, submit patterns)
  - **LearnWeb Components:** Web application voice control
    - `WebViewScrapingEngine`: DOM extraction via JavaScript injection (XPath generation, element traversal)
    - `WebCommandGenerator`: Voice command generation from web elements (click, scroll, input, focus actions)
  - **UI Components:**
    - `VoiceOSTheme`: Material3 theme system (replaces missing AccessibilityTheme)
    - `GlassmorphismUtils`: Glassmorphism UI effects (0.8 alpha, 16dp blur, depth levels)
    - `WidgetOverlayHelper`: WindowManager integration for floating overlays (TYPE_ACCESSIBILITY_OVERLAY)
  - **Metrics & Models:**
    - `VUIDCreationMetrics`: Exploration metrics tracking (VUIDs created, elements detected, error rates)
    - `ExplorationState`: Added PausedForLogin and PausedByUser states with progress tracking
  - **Utilities:**
    - `HashUtils`: SHA-256 hashing for element/app/screen fingerprinting
    - `ScrapingMode`: Scraping mode enumeration (FULL, INCREMENTAL, SELECTIVE, MINIMAL)
  - **Manual Dependency Injection:** DatabaseProvider singleton (Hilt unavailable in AccessibilityService)
  - **Integration Points:**
    - VoiceOSService uses IVoiceOSServiceInternal for component coordination
    - LearnAppIntegration uses ResourceMonitor for exploration health checks
    - AccessibilityScrapingIntegration uses SemanticInferenceHelper for intent inference
    - CleanupPreviewActivity uses VoiceOSTheme for Material3 UI
  - Files added (39 total):
    - Service: `IVoiceOSService.kt`, `IVoiceOSServiceInternal.kt`, `Const.kt`, `Debouncer.kt`, `ResourceMonitor.kt`, `EventPriorityManager.kt`
    - Managers: `InstalledAppsManager.kt`, `AppVersionDetector.kt`, `RenameCommandHandler.kt`
    - AI: `SemanticInferenceHelper.kt`
    - LearnWeb: `WebViewScrapingEngine.kt`, `WebCommandGenerator.kt`
    - UI: `Theme.kt`, `GlassmorphismUtils.kt`, `WidgetOverlayHelper.kt`
    - Metrics: `VUIDCreationMetrics.kt`
    - Utils: `HashUtils.kt`, `ScrapingMode.kt`
    - Docs: `VoiceOS-Infrastructure-Components-Developer-Manual-5221201-V1.md`
  - Impact: Comprehensive infrastructure for service management, resource monitoring, web scraping, and UI components

### Changed
- **Documentation:** Reorganized developer manual structure
  - Moved logging best practices from standalone guide to Chapter 33.7.5
  - Moved inline functions best practices from standalone guide to Chapter 33.7.6
  - Updated Developer Manual to v4.3.1
  - Updated table of contents with new sections
  - Removed: `docs/developer-manual/logging-api-best-practices.md` (consolidated)
  - Removed: `docs/developer-manual/inline-functions-guide.md` (consolidated)
  - Rationale: Developer manual organized by function/feature, not standalone guides

- **NodeRecyclingUtils:** Rewritten `traverseSafely()` to use iterative algorithm
  - Changed from recursive to iterative depth-first traversal
  - Enables inline optimization (~30% performance gain)
  - Eliminates stack overflow risk (handles 1000+ node trees)
  - Maintains same traversal order (DFS, left-to-right)
  - Changed TAG visibility from private to public (required for inline functions)

### Deprecated

### Removed

### Security

---

## Template for Future Releases

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Fixed
- Bug fixes

### Deprecated
- Soon-to-be removed features

### Removed
- Now removed features

### Security
- Security-related changes
```
