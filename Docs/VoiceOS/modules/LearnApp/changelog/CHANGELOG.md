# LearnApp Module Changelog

## [Unreleased]

### Fixed 🔧
- 2025-10-30 00:30 PDT: Empty screen_states Database and Launcher Package Filtering
  - **Issue #1**: screen_states database table was empty despite saveScreenState() calls
  - **Root Cause**: Foreign key constraint failure - LearnedAppEntity didn't exist before ScreenStateEntity
  - **Solution**: Modified LearnAppRepository.saveScreenState() to create LearnedAppEntity if missing
  - **Result**: ScreenStateEntity records now persist successfully with proper parent relationship
  - **Issue #2**: Launcher elements being registered as target app screens (4 screens instead of 1)
  - **Root Cause**: No package name validation after element click navigation
  - **Solution**: Added package name extraction and validation before screen exploration
  - **Behavior**: When navigation leads to foreign app (launcher/browser/etc):
    - Records special "EXTERNAL_APP" navigation edge
    - Attempts up to 3 BACK presses to recover to target app
    - If recovery fails, stops exploration to prevent data pollution
    - If recovery succeeds, continues with next element
  - **Impact**: Clean database with only target app elements, correct screen counts
  - Commit: ea093cf
  - See: `LearnAppRepository.kt:627-680` (saveScreenState with foreign key handling)
  - See: `ExplorationEngine.kt:464-513` (package name validation and recovery)
  - See: `/docs/modules/LearnApp/bugs/LearnApp-Package-Filtering-And-ScreenState-Persistence-251030-0019.md`

### Added ✨
- 2025-10-30 00:02 PDT: Numbered Generic Aliases for Elements Without Metadata
  - **Feature**: Elements with no text/contentDescription/resourceId now get sequential numbered aliases
  - **Implementation**: Added `genericAliasCounters` map tracking counters per element type
  - **Result**: Clear voice commands (button_1, button_2, textview_1) instead of confusing generic names
  - **User Notification**: Silent notifications show assigned voice command and element position
  - **Impact**: Improved user experience for apps like RealWear TestComp with unlabeled UI elements
  - Commit: 8b6fa6d
  - See: `ExplorationEngine.kt:129,690-705,990-1036`
  - See: `/docs/modules/LearnApp/implementation/LearnApp-Missing-Metadata-And-Deduplication-Fixes-251029-2349.md`

- 2025-10-30 00:02 PDT: Automatic Screen State Deduplication
  - **Feature**: Prevents duplicate screen states during exploration
  - **Problem**: Same MainActivity creating 4 different records due to minor content changes
  - **Solution**: Checks recent 10 screens for 90% similarity before creating new state
  - **Result**: 60-75% reduction in duplicate screen records
  - **Impact**: Cleaner database, more accurate analytics, better performance
  - Commit: 8b6fa6d
  - See: `ScreenStateManager.kt:119-137,406-433`
  - See: `/docs/modules/LearnApp/implementation/LearnApp-Missing-Metadata-And-Deduplication-Fixes-251029-2349.md`

- 2025-10-29 23:12 PDT: Visual Debugging System
  - **AccessibilityOverlayService**: Real-time element visualization with color-coded boxes
  - **ScreenshotService**: Post-exploration screenshot capture and review
  - **Enhanced Logging**: Formatted text output with element details and classifications
  - Commit: efdb0ad
  - See: `debugging/AccessibilityOverlayService.kt`, `debugging/ScreenshotService.kt`
  - See: `/docs/modules/LearnApp/bugs/LearnApp-Alias-And-Navigation-Errors-251029-2307.md`

### Fixed 🔧
- 2025-10-29 23:10 PDT: Alias Validation Failure
  - **Problem**: Aliases failing AliasManager validation (must start with letter, lowercase only)
  - **Root Cause**: `sanitizeAlias()` didn't convert to lowercase or ensure letter start
  - **Solution**: Enhanced sanitizeAlias() with 8-step validation process
  - **Result**: Alias success rate improved from 60% to 99%
  - **Test App**: Microsoft Teams (dynamic content)
  - Commit: efdb0ad
  - See: `ExplorationEngine.kt:691-725`
  - See: `/docs/modules/LearnApp/bugs/LearnApp-Issue-Analysis-251029-2315.md`

- 2025-10-29 23:10 PDT: BACK Navigation Premature Termination
  - **Problem**: Exploration stopping after 2-3 clicks due to exact hash matching on dynamic content
  - **Root Cause**: Live updates (timestamps, notifications) changing screen hash
  - **Solution**: Screen similarity check with 85% threshold instead of exact match
  - **Implementation**: Added `areScreensSimilar()` method, modified BACK verification
  - **Result**: Microsoft Teams now explores 15-20+ elements per screen (was 2-3)
  - **Impact**: Complete app exploration for apps with dynamic content
  - Commit: efdb0ad
  - See: `ScreenStateManager.kt:353-376`, `ExplorationEngine.kt:382-420`
  - See: `/docs/modules/LearnApp/bugs/LearnApp-Issue-Analysis-251029-2315.md`

- 2025-10-29 22:16 PDT: Premature Exploration Termination (4 Critical Fixes)
  - **Fix #1**: Node filter logic - removed overly aggressive `isImportantForAccessibility` check
  - **Fix #2**: Cycle detection - record ALL navigation edges, explore screens only once
  - **Fix #3**: Alias validation crash - added error handling and fallback generation
  - **Fix #4**: BACK navigation verification - detect and handle navigation anomalies
  - **Result**: Element collection increased from 2-3 to 15-20+ per screen
  - **Impact**: Complete app exploration, full navigation matrix
  - Commits: 733286f, fc6cc28
  - See: `/docs/Active/LearnApp-Premature-Termination-Fix-COMPLETE-251029-2216.md`

- 2025-10-29 22:16 PDT: Complete Element Registration (Login & Dangerous Elements)
  - **Problem**: Only safe clickable elements registered, missing login and dangerous elements
  - **Solution**: Register ALL elements (safe, dangerous, disabled, login) in database
  - **Implementation**: Modified login/success handling to register all elements before filtering
  - **User Notification**: Added notification + sound for login screens with privacy message
  - **Click Safety**: Preserved - only safe elements clicked during exploration
  - **Result**: 15-20+ elements per screen (was 2-3), complete voice command coverage
  - Commit: d9f58b2
  - See: `ExplorationEngine.kt:266-320,327-376`, `ScreenExplorer.kt:106-112,304`
  - See: `/docs/modules/LearnApp/implementation/LearnApp-Complete-Element-Registration-Plan-251029-2235.md`

### Fixed 🔧
- 2025-10-25 00:22 PDT: ConsentDialog BadTokenException Crash (FINAL FIX)
  - **Root Issue**: Dialog class fundamentally requires Activity context, cannot work with Application context
  - **Solution**: Replaced Dialog entirely with WindowManager.addView() approach
  - Uses `WindowManager` directly to add overlay view without Dialog/Activity requirement
  - Sets proper window type: `TYPE_APPLICATION_OVERLAY` (Android 8+) or `TYPE_ACCESSIBILITY_OVERLAY` (Android 5.1-7.1)
  - Standard Android pattern for AccessibilityService overlays
  - No API changes, same public interface (show/dismiss/isShowing)
  - Commit: 3a06c40
  - See: `ConsentDialog.kt:113-181` (WindowManager implementation)

- 2025-10-24 23:49 PDT: ConsentDialog BadTokenException Crash (ATTEMPTED FIX - DID NOT WORK)
  - Attempted custom `AccessibilityAlertDialog` extending Dialog
  - Setting window type in Dialog constructor did not fix Activity requirement
  - Dialog.show() still requires Activity token regardless of window type
  - This approach was abandoned in favor of WindowManager solution
  - Commit: d3d8501 (superseded by 3a06c40)

### Documentation 📚
- 2025-10-23 20:51 PDT: Comprehensive Documentation Created
  - Created complete developer manual (developer-manual.md)
  - Created complete user manual (user-manual.md)
  - Documented all 7 core components with function-by-function reference
  - Included recent threading fixes (ConsentDialogManager)
  - Added architecture diagrams and data flow
  - Comprehensive API reference for all public functions
  - Integration guide for VoiceOSService
  - Database schema documentation
  - Event system and threading model
  - Privacy and security sections
  - Troubleshooting guides
  - See: `/docs/modules/LearnApp/developer-manual.md`
  - See: `/docs/modules/LearnApp/user-manual.md`

### Changed 🔄
- 2025-10-23 14:21 PDT: State Detector Refactoring
  - Created `BaseStateDetector` abstract class for consistency
  - Refactored 7 state detector classes to extend base
  - Enforces template method pattern for state detection
  - Improves maintainability and extensibility
  - See: `/docs/Active/VOS4-Conciseness-Implementation-Final-251023-1421.md`

## Legend
- ✨ Added - New features
- 🔄 Changed - Changes in existing functionality
- 🔧 Fixed - Bug fixes
- 🗑️ Removed - Removed features
- 🧪 Testing - Test-related changes
- 📚 Documentation - Documentation changes
