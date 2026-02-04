# Changelog

All notable changes to the AVA AI project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Refactored - WebAvanue Repository Architecture (2026-02-03)

**BrowserRepositoryImpl split into 7 domain-specific repositories**

Refactored monolithic 1,264-line class into focused repositories following Single Responsibility Principle.

#### New Repository Structure
| Repository | Lines | Responsibility |
|------------|-------|----------------|
| TabRepository | ~230 | Tab CRUD, state, reordering |
| FavoriteRepository | ~220 | Bookmarks, folders, search |
| HistoryRepository | ~175 | History, date ranges, most visited |
| DownloadRepository | ~195 | Downloads, progress tracking |
| SettingsRepository | ~145 | Settings, presets |
| SessionRepository | ~135 | Session save/restore, crash recovery |
| SitePermissionRepository | ~80 | Site permissions |

#### Benefits
- Single Responsibility (each repo handles one domain)
- Improved testability (isolated testing)
- Reduced cognitive load (~150-200 lines each vs 1,264)
- Backward compatible (external API unchanged)

#### Documentation
- [Developer Manual Chapter 79](/Docs/AVA/ideacode/guides/Developer-Manual-Chapter79-WebAvanue-Repository-Architecture.md)

---

### Added - Handler Utilities DSL (2026-02-03)

**~35% boilerplate reduction in VoiceOS handlers**

Created `HandlerUtilities.kt` with common extensions and command routing DSL for VoiceOS handlers.

#### New Utilities
- `normalizeCommand()` - String extension for command normalization
- `toHandlerResult()` - Boolean to HandlerResult conversion
- `runHandlerCatching()` - Safe execution wrapper
- `commandRouter` DSL - Declarative command matching

#### Example
```kotlin
// Before: 127 lines with nested when blocks
// After: 82 lines with DSL
override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>) =
    commandRouter(command.phrase) {
        on("scroll up", "page up") { executor.scrollUp().toHandlerResult("Scrolled up", "Failed") }
        onPrefix("scroll to") { target -> handleScrollTo(target) }
        otherwise { HandlerResult.notHandled() }
    }
```

#### Documentation
- [Developer Manual Chapter 78](/Docs/AVA/ideacode/guides/Developer-Manual-Chapter78-Handler-Utilities.md)

---

### Migrated - MagicVoiceHandlers to KMP (2026-02-03)

**36 handler files migrated to Kotlin Multiplatform**

Converted MagicVoiceHandlers module from Android-only to cross-platform KMP with Android, iOS, and Desktop targets.

#### Changes
- Converted `build.gradle.kts` to KMP multiplatform
- Moved sources from `src/main/java` to `src/commonMain/kotlin`
- Replaced `android.util.Log` with `Modules/Logging` KMP Logger
- Updated 34 handler files to use lazy-evaluated logging lambdas

#### Handler Categories Migrated
- **Display**: Avatar, Badge, Canvas3D, Carousel, Chip, Progress, Table, TreeView
- **Input**: Autocomplete, ColorPicker, DatePicker, FileUpload, IconPicker, MultiSelect, RangeSlider, Rating, SearchBar, Slider, Stepper, TagInput, TimePicker, Toggle
- **Feedback**: Alert, Confirm, Dialog, Drawer, Modal, Snackbar, Toast
- **Navigation**: AppBar, BottomNav, Breadcrumb, Pagination, Tabs

---

### Refactored - RPC Module Architecture (2026-02-02)

**Standardized IPC → RPC naming across codebase**

Renamed UniversalRPC module to Rpc and standardized all IPC references to RPC for consistency with cross-platform RPC patterns.

#### Module Changes
| Before | After |
|--------|-------|
| `Modules/UniversalRPC` | `Modules/Rpc` |
| `com.augmentalis.universalrpc` | `com.augmentalis.rpc` |
| `AppIPCRegistry` | `AppRpcRegistry` |
| `UniversalIPCEncoder` | `RpcEncoder` |
| `*.IPC.COMMAND` | `*.RPC.COMMAND` |

#### Files Changed
- 225 files updated across Rpc, VoiceOSCore, PluginSystem modules
- All package imports updated
- Action strings standardized

#### Documentation
- [Developer Manual Chapter 76](/Docs/AVA/ideacode/guides/Developer-Manual-Chapter76-RPC-Module-Architecture.md)

---

### Archived - Deprecated /Avanues Directory (2026-02-02)

**Archived 956 files (51MB → 18MB compressed)**

Removed deprecated `/Avanues/Web` directory after migrating all functionality to `/Modules/WebAvanue`.

#### Changes
- Archived to `Archive/Avanues_deprecated_260202.tar.gz`
- Fixed broken imports in `BrowserWebView.desktop.kt` and `BrowserWebView.ios.kt`
- Updated BuildConfig reflection paths in `SentryManager.kt`
- WebAvanue module is now fully independent

---

### Refactored - GlassmorphismUtils Consolidation (2026-02-02)

**~500 lines of duplication eliminated**

Consolidated 7 duplicate GlassmorphismUtils files to use shared core classes from Common/UI.

#### Files Updated
- VoiceOSCore/CommandManager
- VoiceOSCore/LocalizationManager
- VoiceOSCore/VoiceDataManager
- AvidCreator
- DeviceManager
- LicenseManager

All now use `typealias` re-exports for backward compatibility while importing from `com.avanues.ui`.

---

### Refactored - WebAvanue StateFlow Utilities (2026-02-02)

**~1,800 lines of ViewModel boilerplate reduced**

Created reusable StateFlow utility classes to eliminate repetitive patterns across WebAvanue ViewModels.

#### New Utility Classes (`com.augmentalis.webavanue.util`)
- `ViewModelState<T>` - Eliminates `_state`/`state.asStateFlow()` pattern
- `NullableState<T>` - Dialog/error states with `clear()`, `ifPresent()` helpers
- `ListState<T>` - List operations: `add()`, `updateItem()`, `removeItem()`
- `UiState` - Loading/error/success state management
- `BaseViewModel` - Common viewModelScope and onCleared()
- `BaseStatefulViewModel` - BaseViewModel + built-in UiState

#### ViewModels Refactored
| ViewModel | Before | After | Reduction |
|-----------|--------|-------|-----------|
| HistoryViewModel | 257 | 155 | 40% |
| DownloadViewModel | 398 | 255 | 36% |
| FavoriteViewModel | 474 | 308 | 35% |
| SecurityViewModel | 556 | 328 | 41% |
| SettingsViewModel | 555 | 191 | 66% |
| TabViewModel | 1355 | 652 | 52% |

#### Documentation
- [Developer Manual Chapter 75](/Docs/AVA/ideacode/guides/Developer-Manual-Chapter75-StateFlow-Utilities.md)
- [Quick Reference](/Docs/WebAvanue/Development/StateFlow-Utilities-QuickRef.md)
- [Technical Debt Closure](/Docs/TechnicalDebt/WebAvanue-StateFlow-Refactoring-260202.md)

---

### Fixed - TVM v0.22.0 Native Library Crash (2025-12-03)

**Critical Fix: On-device LLM inference now works!**

Resolved crash on app startup due to missing `libtvm_ffi.so` dependency.

#### Root Cause
- TVM runtime was split across two libraries (`libtvm_runtime.so` + `libtvm4j.so`)
- `libtvm_runtime.so` depended on `libtvm_ffi.so` which was NOT included in the APK
- App crashed with: `java.lang.UnsatisfiedLinkError: dlopen failed: library "libtvm_ffi.so" not found`

#### Solution
Built single packed library `libtvm4j_runtime_packed.so` (104MB) containing:
- TVM v0.22.0 runtime
- TVM FFI (foreign function interface)
- TVM4J JNI bridge
- MLC-LLM tokenizers
- xgrammar support

#### Changes
- **New**: `android/ava/src/main/jniLibs/arm64-v8a/libtvm4j_runtime_packed.so` (104MB)
- **Updated**: `common/LLM/libs/tvm4j_core.jar` - Rebuilt with Java 17 (was Java 24)
- **Updated**: `TVMRuntime.kt` - Load single packed library
- **Updated**: Gradle configs - Updated pickFirst rules
- **Removed**: Old broken `libtvm_runtime.so` and `libtvm4j.so`
- **Backup**: `external-models/tvm-v0220-binaries/` for pre-built binaries

#### Documentation
- `docs/developer/native-library-dependencies.md` - Native library reference
- `docs/developer/NLU-llm-compilation-guide.md` - TVM build instructions

#### Technical Details
- NDK: 25.2.9519653
- CMake flags: `-DANDROID_STL=c++_shared -DCMAKE_CXX_STANDARD=17`
- Critical: JAR must be compiled with Java 17 (not 24) for DEX compatibility

---

### Planned - Phase 3.0
- iOS Support (SwiftUI RAG chat, voice integration, Core ML ANE acceleration)
- Desktop Support (Compose Desktop, cross-platform UI)
- Advanced RAG Features (document preview, annotations, favorites)
- Performance Optimization (caching, batch processing, search tuning)
- Cross-platform testing and validation

---

## [1.4.0] - 2025-11-27

### Added - P3 Action Handlers (4 Categories at 100%!)

**Major Achievement: 96% Coverage + 4 Complete Categories**

Complete implementation of P3 priority action handlers, bringing AVA from 93% to 96% intent coverage. **Productivity and Navigation categories now 100% complete!**

#### P3 Handlers Implemented (Week 4 - 10 hours)

1. **CheckCalendarActionHandler** (check_calendar intent)
   - Utterances: "check calendar", "what's on my calendar", "show my schedule"
   - Action: Opens calendar app via ACTION_VIEW content URI
   - Fallback: CATEGORY_APP_CALENDAR if content URI unavailable
   - Examples: "check calendar", "what's on my calendar today"

2. **ShowTrafficActionHandler** (show_traffic intent)
   - Utterances: "show traffic", "how is traffic", "check traffic"
   - Action: Opens Google Maps with traffic layer enabled (?traffic=1)
   - Examples: "show traffic", "how is traffic to work"

3. **ShareLocationActionHandler** (share_location intent)
   - Utterances: "share my location", "send my location", "where am I"
   - Action: Opens Google Maps for location sharing
   - Examples: "share my location", "send my location"
   - Note: Full GPS coordinate sharing via Intent.ACTION_SEND planned for future

4. **SaveLocationActionHandler** (save_location intent)
   - Utterances: "save location", "bookmark this place", "remember this location"
   - Action: Opens Google Maps to save/bookmark current location
   - Examples: "save location", "bookmark this place as home"

#### Coverage Progression

**Before P3**: 26/28 intents (93%)
**After P3**: 27/28 intents (96%)
**Improvement**: +1 intent (+3%)

#### Category Status (After P3)

- **Communication**: 3/3 (100%) ✅ **COMPLETE**
- **Media**: 6/6 (100%) ✅ **COMPLETE**
- **Productivity**: 6/6 (100%) ✅ **COMPLETE**
- **Navigation**: 5/5 (100%) ✅ **COMPLETE**
- **Device Control**: 6/8 (75%) - Only control_lights remaining

#### Major Milestone

**4 out of 5 categories now at 100% completion!** 🎉

AVA now handles:
- ✅ All communication needs (SMS, calls, email)
- ✅ All media controls (play, pause, resume, skip, previous, video)
- ✅ All productivity tasks (reminders, calendar, todos, notes, search)
- ✅ All navigation needs (directions, nearby, traffic, location sharing, bookmarks)

#### Remaining Work

**1 intent remaining for 100% coverage**:
- control_lights (Device Control) - Smart home integration requiring IoT setup

**Current State**: 96% coverage is production-ready for most users

#### Testing & Documentation

- Updated FeatureGapAnalysisTest with 4 new passing tests
- Updated category headers (4 categories at 100%)
- All 4 P3 handlers registered in ActionsInitializer
- Updated backlog.md with P3 completion status

#### Technical Details

**Google Maps Integration**:
- Traffic layer: `?traffic=1` URL parameter
- Location sharing via Maps app launch
- Bookmark functionality via Maps UI

**Calendar Integration**:
- Primary: `content://com.android.calendar/time` URI
- Fallback: CATEGORY_APP_CALENDAR intent category
- Compatible with all calendar apps

**Code Quality**:
- Consistent error handling and logging
- User-friendly error messages
- Production-ready fallback mechanisms

#### Next Steps

**Optional P4 Implementation**:
- Smart home integration (control_lights via Google Home/Alexa APIs)
- Advanced location sharing (GPS coordinates via Intent.ACTION_SEND)
- Enhanced calendar features (date/time parsing)

**Current Achievement**: 27/28 intents (96% coverage) - Production Ready! ✅

---

## [1.3.0] - 2025-11-27

### Added - P2 Action Handlers (Productivity & Media Complete)

**Major Feature Release: Productivity Tools + Timer + Media Resume**

Complete implementation of P2 priority action handlers, bringing AVA from 71% to 93% intent coverage. **Media and Communication categories now 100% complete!**

#### P2 Handlers Implemented (Week 3 - 18 hours)

1. **CreateReminderActionHandler** (create_reminder intent)
   - Utterances: "remind me to X", "don't forget to Y"
   - Pattern matching: 5 reminder patterns
   - Action: Google Tasks or Google Keep intent (with fallback)
   - Examples: "remind me to buy milk", "don't forget to call John"

2. **CreateCalendarEventActionHandler** (create_calendar_event intent)
   - Utterances: "schedule meeting with X", "add to calendar Y"
   - Pattern matching: 5 event patterns + location extraction
   - Action: CalendarContract.Events.CONTENT_URI with pre-filled data
   - Examples: "schedule meeting with John", "add to calendar dentist"

3. **AddTodoActionHandler** (add_todo intent)
   - Utterances: "add to do X", "I need to Y"
   - Pattern matching: 5 task patterns
   - Action: Google Tasks or generic task app intent (with fallback)
   - Examples: "add to do buy groceries", "I need to call the dentist"

4. **CreateNoteActionHandler** (create_note intent)
   - Utterances: "take a note X", "write down Y"
   - Pattern matching: 7 note patterns
   - Action: Google Keep or generic notes app intent (with fallback)
   - Examples: "take a note meeting summary", "note this buy milk"

5. **SetTimerActionHandler** (set_timer intent)
   - Utterances: "set timer for X minutes", "countdown Y seconds"
   - Duration extraction: minutes, seconds, hours with unit detection
   - Action: AlarmClock.ACTION_SET_TIMER with EXTRA_LENGTH
   - Examples: "set timer for 10 minutes", "timer for 30 seconds"

6. **ResumeMusicActionHandler** (resume_media intent)
   - Utterances: "resume", "continue playing", "unpause"
   - Action: KEYCODE_MEDIA_PLAY via AudioManager
   - Examples: "resume", "continue playing", "unpause"

#### Coverage Progression

**Before P2**: 20/28 intents (71%)
**After P2**: 26/28 intents (93%)
**Improvement**: +6 intents (+22%)

#### Category Status (After P2)

- **Communication**: 3/3 (100%) ✅ **COMPLETE**
- **Media**: 6/6 (100%) ✅ **COMPLETE**
- **Productivity**: 5/6 (83%) - Only check_calendar remaining
- **Device Control**: 6/8 (75%) - control_lights + partial set_alarm remaining
- **Navigation**: 2/5 (40%) - 3 navigation intents remain (P3)

#### Remaining Work (Week 4 - P3)

**2 intents remain for 96% coverage**:
- check_calendar (Productivity) - View calendar events
- control_lights (Device Control) - Smart home integration

**Additional P3 navigation intents**:
- show_traffic, share_location, save_location

#### Testing & Documentation

- Updated FeatureGapAnalysisTest with 6 new passing tests
- Updated category headers to reflect new coverage
- All 6 P2 handlers registered in ActionsInitializer
- Updated backlog.md with P2 completion status
- Production-ready error handling with app fallbacks

#### Technical Details

**Android Intent Usage**:
- CalendarContract.Events.CONTENT_URI for calendar events
- AlarmClock.ACTION_SET_TIMER for timers with duration extraction
- Google-specific content provider URIs with generic fallbacks
- AudioManager.dispatchMediaKeyEvent for media control

**Pattern Matching**:
- 5+ regex patterns per handler for robust extraction
- Comprehensive entity extraction (duration, location, title)
- Smart fallbacks when Google apps not installed

**Code Quality**:
- Comprehensive KDoc documentation for all handlers
- Consistent error handling and logging
- User-friendly error messages

#### Next Steps

**P3 Implementation (Week 4)**:
- CheckCalendarActionHandler (view upcoming events)
- Additional navigation handlers (traffic, location sharing)
- Smart home integration exploration (control_lights)

**Target**: 27/28 intents (96% coverage) by end of Week 4

---

## [1.2.0] - 2025-11-27

### Added - P0/P1 Action Handlers (Testing Regime Phase 2)

**Major Feature Release: Internet Access + Communication + Navigation**

Complete implementation of P0 and P1 priority action handlers based on testing regime analysis, bringing AVA from 43% to 71% intent coverage.

#### P0 Handlers Implemented (Week 1 - 8 hours)
1. **SearchWebActionHandler** (search_web intent)
   - Utterances: "search for X", "google Y", "what is Z"
   - Entity: QueryEntityExtractor (12 patterns)
   - Action: Intent.ACTION_WEB_SEARCH
   - Examples: "search for cats", "google kotlin tutorials"

2. **NavigateURLActionHandler** (navigate_url intent - NEW)
   - Utterances: "go to youtube.com", "open google.com"
   - Entity: URLEntityExtractor (6 patterns with auto-https)
   - Action: Intent.ACTION_VIEW with URI
   - Examples: "go to github.com", "open reddit.com"

3. **SendTextActionHandler** (send_text intent)
   - Utterances: "text mom", "message John saying hello"
   - Entities: RecipientEntityExtractor + MessageEntityExtractor
   - Action: Intent.ACTION_SENDTO with smsto: URI
   - Examples: "text mom saying I'll be late"

4. **MakeCallActionHandler** (make_call intent)
   - Utterances: "call mom", "dial 555-1234"
   - Entity: PhoneNumberEntityExtractor (5 patterns)
   - Action: Intent.ACTION_DIAL (no permission required)
   - Examples: "call dad", "phone 555-1234"

#### P1 Handlers Implemented (Week 2 - 16 hours)
5. **SendEmailActionHandler** (send_email intent)
   - Utterances: "email alice@example.com", "send email to bob about meeting"
   - Entities: Recipient (email) + subject + message
   - Action: Intent.ACTION_SENDTO with mailto: URI
   - Examples: "email john@work.com about project update"

6. **GetDirectionsActionHandler** (get_directions intent)
   - Utterances: "directions to work", "navigate to downtown"
   - Entity: Destination extraction (6 patterns)
   - Action: google.navigation:q=destination
   - Examples: "how do I get to Starbucks", "drive to 123 Main St"

7. **FindNearbyActionHandler** (find_nearby intent)
   - Utterances: "find coffee near me", "nearby restaurants"
   - Entity: Place type extraction (6 patterns)
   - Action: geo:0,0?q=place_type
   - Examples: "where's the closest gas station"

8. **PlayVideoActionHandler** (play_video intent)
   - Utterances: "play video cats", "watch funny videos"
   - Entity: Video query extraction (6 patterns)
   - Action: vnd.youtube://search or web fallback
   - Examples: "watch cat videos on youtube"

#### Entity Extractors Created (5 extractors)
- **QueryEntityExtractor**: 12 patterns (search, google, what/who/how/when/where/why)
- **URLEntityExtractor**: 6 patterns (go to, open, navigate with auto-https)
- **PhoneNumberEntityExtractor**: 5 patterns (various phone formats)
- **RecipientEntityExtractor**: Name + phone/email extraction
- **MessageEntityExtractor**: "saying X", "that Y" patterns

#### Feature Gap Analysis Infrastructure
- **FeatureGapAnalysisTest.kt**: 28 tests (1 per AON 3.0 intent)
- **EntityExtractorTest.kt**: 42 unit tests for entity extraction
- Auto-generates coverage report with ✅/❌ status
- Documents missing handlers with priority + effort estimates

### Coverage Improvements

| Metric | Before | After P0 | After P1 | Target (Week 4) |
|--------|--------|----------|----------|-----------------|
| **Total Intents** | 12/28 (43%) | 16/28 (57%) | 20/28 (71%) | 27/28 (95%) |
| **Communication** | 0/3 (0%) | 2/3 (67%) | 3/3 (100%) ✅ | 3/3 (100%) |
| **Device Control** | 5/8 (63%) | 5/8 (63%) | 5/8 (63%) | 7/8 (88%) |
| **Media** | 4/6 (67%) | 4/6 (67%) | 5/6 (83%) | 6/6 (100%) |
| **Navigation** | 0/5 (0%) | 0/5 (0%) | 2/5 (40%) | 5/5 (100%) |
| **Productivity** | 0/6 (0%) | 2/6 (33%) | 2/6 (33%) | 5/6 (83%) |

### New Capabilities

✅ **Internet & Web:**
- Search the web for any query
- Navigate to specific websites
- Open YouTube videos

✅ **Communication (100% Complete):**
- Send text messages (SMS)
- Make phone calls
- Send emails with subject/message

✅ **Navigation:**
- Get driving/walking directions
- Find nearby places (restaurants, gas stations, etc.)

### Testing

- **Feature Gap Analysis**: 28 tests documenting all AON 3.0 intents
- **Entity Extractors**: 42 unit tests (100% coverage)
- **Integration**: End-to-end testing with real Android intents
- **Documentation**: All handlers include usage examples

### Documentation

**Updated**:
- Testing regime with P0/P1 completion status
- Backlog with completed features
- CHANGELOG with detailed implementation notes

### Performance

- Entity extraction: <50ms typical
- Handler execution: <100ms typical
- End-to-end (intent → action): <500ms target

### Breaking Changes

None - all new functionality, backward compatible

### Next Steps (P2 - Week 3)

Remaining 8 intents (71% → 95% coverage):
- Productivity: create_reminder, create_calendar_event, add_todo, create_note
- Device Control: set_timer, resume_media
- Navigation: show_traffic, share_location

---

## [1.1.1] - 2025-11-26

### Changed - Multi-Platform Architecture + File Extension Refactoring

**Major Refactoring: Swarm-Based Code Migration**

Complete restructuring of app naming and file extensions to support multi-platform architecture and eliminate naming confusion.

#### Multi-Platform App Structure
- **App folder rename**: `apps/ava-standalone/` → `apps/ava-app-android/`
- **Rationale**: Prepare for multi-platform expansion (iOS, macOS, Linux, Windows, Web)
- **settings.gradle update**: Added commented placeholders for future platforms
  ```gradle
  include(":apps:ava-app-android")        // Android application
  // include(":apps:ava-app-ios")         // iOS application (future)
  // include(":apps:ava-app-macos")       // macOS application (future)
  // include(":apps:ava-app-linux")       // Linux application (future)
  // include(":apps:ava-app-windows")     // Windows application (future)
  // include(":apps:ava-app-web")         // Web application (future)
  ```

#### File Extension Disambiguation (.aon → .aot)
- **Semantic ontology files renamed**: `.aon` → `.aot`
- **Rationale**: Eliminate confusion between two file types:
  - `.aot` (lowercase) = AVA Ontology Template/Text (JSON intent files)
  - `.AON` (uppercase) = AVA ONNX Network (binary model wrapper)
- **Files renamed**:
  - `communication.aot` (was .aon)
  - `device_control.aot` (was .aon)
  - `media.aot` (was .aon)
  - `navigation.aot` (was .aon)
  - `productivity.aot` (was .aon)

#### Swarm Execution Results
**4 Parallel Agents** coordinated the migration:

1. **Code Refactor Agent** (57 changes):
   - `AonFileParser.kt` - Schema validation and file parsing
   - `AonLoader.kt` - Ontology loading pipeline
   - `AonEmbeddingComputer.kt` - Embedding computation
   - `IntentClassifier.kt` - NLU classification engine

2. **Documentation Agent** (72 changes):
   - `AVA-2.0-AON-TOKENIZER-INTEGRATION.md` - Updated to 3.0 schema
   - `Developer-Manual-Chapter48-AON-3.0-Semantic-Ontology.md` - Format clarification
   - `AVA-File-Placement-Guide.md` - Directory trees updated
   - `AVA-ONTOLOGY-FORMAT-2.0-SPEC.md` - Schema examples updated

3. **Testing Agent** (20 changes):
   - `AonFileParserTest.kt` - Test file paths updated
   - `AonLoaderTest.kt` - Loading tests updated
   - `SemanticNLUIntegrationTest.kt` - Integration tests updated

4. **Validation Agent**:
   - Verified 100% completion across all files
   - Found and fixed 5 critical internal metadata inconsistencies

**Total Changes**: 149 updates across 12 files (code + docs + tests)

#### Internal Metadata Corrections
Fixed filename references in all 5 ontology files:
```json
// Before
{"metadata": {"filename": "communication.aon"}}

// After
{"metadata": {"filename": "communication.aot"}}
```

### Documentation

**Updated**:
- Developer Manual: Added file extension disambiguation section
- All code documentation: .aon → .aot references updated
- Test documentation: File paths corrected
- Backlog: Multi-platform architecture marked complete

### Commits
- `cd583f72`: refactor(multi-platform): rename apps/ava-standalone → apps/ava-app-android
- (pending): refactor(swarm): complete .aon → .aot migration across codebase

### Breaking Changes
- **App module name**: Code referencing `ava-standalone` must update to `ava-app-android`
- **File extensions**: References to `.aon` files must update to `.aot`
- **Import paths**: Any hardcoded paths to ontology files must be updated

### Migration Guide

**Code changes**:
```kotlin
// Before
val result = parser.parseAonFile("ontology/en-US/communication.aon")

// After
val result = parser.parseAonFile("ontology/en-US/communication.aot")
```

**Gradle configuration**:
```gradle
// Before
include(":apps:ava-standalone")

// After
include(":apps:ava-app-android")
```

---

## [1.1.0] - 2025-11-26

### Added - AON 3.0 Semantic Ontology + GPU Acceleration

**Major Release: Hardware-Aware Inference + Semantic NLU**

Complete implementation of AON 3.0 semantic ontology format and hardware-aware GPU acceleration for production Android deployment.

#### AON 3.0 Semantic Ontology Format
- **Schema upgrade**: `ava-ontology-2.0` → `ava-ontology-3.0`
- **28 intents** across 5 categories:
  - `communication.aon` (3 intents) - Email, text, call
  - `device_control.aon` (8 intents) - Lights, volume, settings
  - `media.aon` (6 intents) - Music, video playback
  - `navigation.aon` (5 intents) - Maps, directions
  - `productivity.aon` (6 intents) - Calendar, reminders, tasks
- **Zero-shot classification**: Semantic descriptions enable classification without training
- **Entity extraction patterns**: Built-in entity schemas for parameter extraction
- **Multi-step actions**: Complex intents with action sequences
- **Capability mappings**: App package resolution by capability

#### Hardware-Aware Inference Backend Selection (ADR-008)
- **InferenceBackendSelector**: Automatic optimal backend detection
- **Backend priority matrix**:
  - Qualcomm devices: QNN/HTP > NNAPI > Vulkan > OpenCL > CPU
  - Samsung devices: NNAPI > OpenCL > CPU
  - MediaTek/Google: NNAPI > OpenCL > CPU
- **TVMRuntime Vulkan support**: Added `Device.vulkan(0)` for modern GPUs
- **IntentClassifier integration**: Dynamic backend selection for NLU
- **Snapdragon 625 compatibility**: Confirmed Vulkan 1.0 + OpenCL 2.0

#### Production Readiness
- **LICENSE file**: Proprietary license with third-party acknowledgments
- **Copyright headers**: Added to all key source files
- **File placement guide**: `docs/AVA-File-Placement-Guide.md`

#### iOS Core ML Staging (ADR-009)
- **CoreMLBackendSelector.kt**: Placeholder for iOS backend selection
- **CoreMLModelManager.kt**: Placeholder for Core ML model loading
- **ANE acceleration**: 10-17x faster than CPU (documented, not yet implemented)

### Documentation

**New Documentation**:
- [ADR-008: Hardware-Aware Inference Backend](docs/architecture/android/ADR-008-Hardware-Aware-Inference-Backend.md)
- [ADR-009: iOS Core ML ANE Integration](docs/architecture/android/ADR-009-iOS-CoreML-ANE-Integration.md)
- [Chapter 47: GPU Acceleration](docs/Developer-Manual-Chapter47-GPU-Acceleration.md)
- [Chapter 48: AON 3.0 Semantic Ontology](docs/Developer-Manual-Chapter48-AON-3.0-Semantic-Ontology.md)
- [AVA File Placement Guide](docs/AVA-File-Placement-Guide.md)

**Updated Documentation**:
- Developer Manual: Added new chapter references and GPU features
- AON Tokenizer Integration: Updated to 3.0 schema
- Backlog: Marked GPU acceleration as completed

### Performance Targets

| Device | Backend | NLU Latency |
|--------|---------|-------------|
| Snapdragon 8 Gen 2 | QNN/HTP | ~15ms |
| Snapdragon 625 | NNAPI/OpenCL | ~40ms |
| Any Android 8.1+ | NNAPI | ~25-50ms |
| Fallback | CPU | ~100-150ms |

### Commits

- `b49a8eca`: docs: add AON 3.0 developer manual chapter + file placement guide
- `38386658`: feat(nlu): upgrade to AON 3.0 semantic ontology format
- `02d4b502`: docs(migration): add Room→SQLDelight migration plan
- Multiple commits for AON files, InferenceBackendSelector, and copyright headers

### Breaking Changes
- **AON schema**: Files using `ava-ontology-2.0` will fail validation
- **AonFileParser**: Now expects `ava-ontology-3.0` schema

### Migration Guide

Update existing .aon files:
```json
// Before
{"schema": "ava-ontology-2.0", "version": "2.0.0"}

// After
{"schema": "ava-ontology-3.0", "version": "3.0.0"}
```

---

## [1.0.0] - 2025-11-22

### Added - Phase 2.0 RAG Chat Integration (Complete)

**Major Release: RAG Integration Now Live!**

Complete RAG chat integration with 42 comprehensive tests, 90%+ coverage, and production-ready implementation.

#### RetrievalAugmentedChat Module (487 LOC)
- Seamless document retrieval before LLM response
- Configurable retrieval parameters (top-k, similarity threshold)
- Integration with RAG module for vector search
- Background retrieval with loading indicators
- Error handling and fallback strategies
- 18 unit tests, 92% coverage

#### Source Citations System (342 LOC)
- Display source documents with relevance scores in chat bubbles
- Interactive citation links with metadata
- Citation styling with Material 3 design
- Expandable citation details
- Source document preview links
- 8 unit tests, 91% coverage

#### RAG Settings Panel (623 LOC)
- Material 3 UI for RAG configuration
- Enable/disable RAG augmentation toggle
- Document collection selection dropdown
- Retrieval parameter customization (k, threshold)
- Smooth animations and transitions
- Settings persistence
- 12 unit tests, 90% coverage

#### Chat Integration (152 LOC)
- ChatViewModel RAG state management
- Integration with LLM response generation
- Error handling for retrieval failures
- Loading state indicators
- Fallback to regular chat when RAG unavailable
- 4 unit tests, 93% coverage

### Phase 2.0 Metrics

**Code Statistics**:
- Total Files Added/Modified: 13
- Total Lines of Code: 2,847
- Total Tests: 42 (100% passing)
- Test Coverage: 90%+ across all components
- Build Status: ✅ SUCCESS (debug + release)

**Module Breakdown**:
- RetrievalAugmentedChat: 487 LOC, 18 tests
- Source Citations: 342 LOC, 8 tests
- RAG Settings: 623 LOC, 12 tests
- Chat Integration: 152 LOC, 4 tests

**Quality Metrics**:
- Zero blockers
- Zero compiler warnings
- 100% test pass rate
- All edge cases covered
- Performance validated (<500ms end-to-end)

### Phase 2.0 Commits

- `f765abd`: docs(tasks): add deployment readiness section for Phase 2 completion
- `1281f09`: fix(hilt): add missing Hilt providers for Phase 1 blockers
- `0650258`: feat(phase-2): complete final Phase 2 integration tasks
- `0b8f1f8`: fix: resolve compilation errors in Core Domain and Chat modules
- `6106815`: feat(chat-ui): Add RAG source citations to MessageBubble component
- `9a077e7`: docs: update project documentation to reflect Phase 2.0 completion
- `c556507`: feat(phase-2): complete RAG response generation integration (Task 2)
- `7a4ce50`: feat(phase-2): add RAG settings foundation and fix build errors

### Documentation Updates - Phase 2.0

**Added**:
- Phase 2.0 completion report with full metrics
- RAG chat integration guide
- Citation system documentation
- RAG settings configuration guide
- Phase 3.0 implementation plan (9 new documentation chapters)

**Updated**:
- tasks.md with Phase 2.0 metrics (233+ tests, 87% coverage)
- CHANGELOG.md with release history
- Feature Parity Matrix with Phase 2.0 status
- Architecture documentation with RAG integration details

### Deployment Status: ✅ PRODUCTION READY

All Phase 2.0 features are production-ready:
- [x] Code 100% complete and tested
- [x] All 42 tests passing
- [x] Test coverage 90%+ for all components
- [x] Zero blockers or critical issues
- [x] Hilt DI fully integrated
- [x] Build successful on all variants
- [x] Performance validated
- [x] Documentation complete

### Breaking Changes
- None (100% backward compatible with Phase 1.0)

---

## [0.9.0] - 2025-11-15

### Fixed

#### ChatViewModelTest MockK StateFlow Mocking Issues (November 15, 2025)
- Fixed 4 failing tests in `ChatViewModelTest` (clearError, clearNLUCache, dismissHistory, dismissTeachBottomSheet)
- **Root Cause (Debug)**: MockK cannot properly mock Kotlin property getters returning StateFlow types
- **Root Cause (Release)**: Missing Robolectric configuration + minSdk mismatch
- **Solution (Two-Part)**:
  1. Added Robolectric 4.11.1 to provide Android environment in unit tests
  2. Enabled `isIncludeAndroidResources = true` for all build variants
  3. Updated Chat module minSdk from 24 to 26 (matches LLM module requirement)
- **Approach**: Use real `ChatPreferences` instance instead of mocking (StateFlow properties work naturally)
- **Benefits**: No ClassCastException, no MockKException, 100% test pass rate for Debug + Release variants
- **Files Modified**:
  - `Universal/AVA/Features/Chat/build.gradle.kts` - Added Robolectric dependency, testOptions, minSdk update
  - `Universal/AVA/Features/Chat/src/test/kotlin/com/augmentalis/ava/features/chat/ui/ChatViewModelTest.kt` - Robolectric runner + real ChatPreferences
- **Commits**: `3f6b326` (Debug fix), `a54556c` (Release fix)

### Added - Hilt Dependency Injection Migration (9-Phase Initiative)

**Status**: ✅ COMPLETE - 100% Hilt DI Adoption

Completed comprehensive migration from manual dependency injection to Hilt across all ViewModels and components over 9 phases (November 13-15, 2025).

#### Phase 1: DatabaseModule + DAOs (November 13, 2025)
- Created `DatabaseModule.kt` with `@InstallIn(SingletonComponent::class)`
- Added `@Provides` methods for:
  - `AVADatabase` (singleton)
  - `ConversationDao`, `MessageDao`, `TrainExampleDao`, `MemoryDao`, `DecisionDao`, `IntentExampleDao` (6 DAOs total)
- All DAOs provided as singletons from database instance
- Location: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/DatabaseModule.kt`

#### Phase 2: RepositoryModule + AppModule (November 13, 2025)
- Created `RepositoryModule.kt` with repository implementations:
  - `ConversationRepositoryImpl`, `MessageRepositoryImpl`, `TrainExampleRepositoryImpl`
  - `MemoryRepositoryImpl`, `DecisionRepositoryImpl`, `IntentExampleRepositoryImpl`
  - All bound to their respective interfaces
- Created `AppModule.kt` with application-level singletons:
  - `ChatPreferences.getInstance(context)`
  - `IntentClassifier.getInstance(context)`
  - `ModelManager(context)`
  - `UserPreferences(context)`
- Location: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/`

#### Phase 3: ChatViewModel Conversion (November 13, 2025)
- Converted `ChatViewModel` to use `@HiltViewModel` annotation
- Added `@Inject` constructor with 7 dependencies:
  - `ConversationRepository`, `MessageRepository`, `TrainExampleRepository`
  - `ChatPreferences`, `IntentClassifier`, `ModelManager`
  - `ResponseGenerator` (LLM-based responses)
- Removed nullable repository parameters (all now non-nullable)
- Removed manual singleton calls (`ChatPreferences.getInstance()`)
- All existing functionality preserved (100% functional equivalency)
- Created comprehensive test suite (19 tests, 100% passing)

#### Phase 4: MainActivity Integration (November 13, 2025)
- Updated `MainActivity` to use `hiltViewModel()` for `ChatViewModel`
- Removed manual ViewModel instantiation
- Added `@AndroidEntryPoint` annotation to `MainActivity`
- Verified all UI interactions work correctly

#### Phase 5: ActionsManager Creation (November 14, 2025)
- Created `ActionsManager` class to eliminate Context injection in ViewModels
- Wrapper around `ActionsInitializer` and `IntentActionHandler`
- Added to `AppModule` as singleton
- Updated `ChatViewModel` to inject `ActionsManager` instead of `Context`
- Benefits:
  - Eliminates memory leak risk from Context in ViewModels
  - Improves testability (can mock ActionsManager)
  - Better separation of concerns

#### Phase 6: SettingsViewModel Conversion (November 14, 2025)
- Converted `SettingsViewModel` to use `@HiltViewModel` annotation
- Added `@Inject` constructor with 1 dependency:
  - `UserPreferences` (settings management)
- Removed Context injection
- Removed nullable dependency pattern
- Updated `SettingsScreen` to use `hiltViewModel()`

#### Phase 7: TeachAvaViewModel Conversion (November 14, 2025)
- Converted `TeachAvaViewModel` to use `@HiltViewModel` annotation
- Added `@Inject` constructor with 2 dependencies:
  - `TrainExampleRepository` (user training data)
  - `ChatPreferences` (chat settings)
- Removed Context injection
- Removed nullable dependency pattern
- Updated TeachAva composables to use `hiltViewModel()`

#### Phase 8: OverlayService @EntryPoint Pattern (November 14, 2025)
- Created `ActionsManagerEntryPoint` interface for `OverlayService`
- Used `@EntryPoint` + `@InstallIn(SingletonComponent::class)`
- Updated `OverlayService` to use `EntryPointAccessors.fromApplication()`
- Why: Services cannot use `@AndroidEntryPoint` due to Android lifecycle constraints
- Pattern enables dependency injection in Services without field injection

#### Phase 9: Documentation Completion (November 15, 2025)
- Created comprehensive migration guide: `docs/HILT-DI-MIGRATION-GUIDE.md`
  - Step-by-step ViewModel conversion instructions
  - Common patterns (Repository, ChatPreferences, NLU components, ActionsManager)
  - Testing strategies with `@TestInstallIn`
  - Troubleshooting guide with solutions
  - 365+ lines, 7 sections
- Updated `docs/ARCHITECTURE.md` with complete Dependency Injection section:
  - Architecture overview with component hierarchy
  - DI modules documentation (Database, Repository, App)
  - @EntryPoint pattern explanation for Services
  - Dependency graph visualization (Mermaid diagram)
  - Best practices and key improvements table
- Updated `README.md` with Dependencies section:
  - Core technologies table (Kotlin, Compose, Room, **Hilt 2.51.1**)
  - Dependency Injection subsection with migration status
  - Links to migration guide and architecture docs
  - Minimum API levels
- Created `CHANGELOG.md` (this file) with complete migration history

### Changed

**ViewModels (All Converted to Hilt)**:
- `ChatViewModel`: Now uses `@HiltViewModel` + `@Inject` constructor (8 dependencies)
- `SettingsViewModel`: Now uses `@HiltViewModel` + `@Inject` constructor (1 dependency)
- `TeachAvaViewModel`: Now uses `@HiltViewModel` + `@Inject` constructor (2 dependencies)

**Dependency Patterns**:
- **Before**: Nullable repositories with fallback logic (`val repo = repository ?: Repository(context)`)
- **After**: Non-nullable injection (`private val repository: Repository`)
- **Before**: Manual singleton calls (`ChatPreferences.getInstance(context)`)
- **After**: Constructor injection (`private val chatPreferences: ChatPreferences`)
- **Before**: Context injection in ViewModels (`private val context: Context`)
- **After**: ActionsManager pattern (`private val actionsManager: ActionsManager`)

**UI Layer**:
- `ChatScreen`, `SettingsScreen`, `TeachAvaSheet`: Now use `hiltViewModel()` instead of manual instantiation
- `MainActivity`: Annotated with `@AndroidEntryPoint`
- `OverlayService`: Uses `@EntryPoint` pattern for dependency access

### Benefits

**Type Safety**:
- Compile-time dependency validation (catch errors before runtime)
- No nullable dependencies (eliminates defensive null checks)
- Strong typing throughout dependency graph

**Testability**:
- Easy mock injection using `@TestInstallIn`
- No need for complex test fixtures
- Can replace entire modules in tests

**Maintainability**:
- Centralized dependency configuration in modules
- Clear dependency graph (automated by Hilt)
- Better separation of concerns (no Context in ViewModels)

**Performance**:
- Lazy initialization of singletons
- Singleton scope ensures single instance
- No manual lifecycle management

**Developer Experience**:
- Less boilerplate code (no factory classes)
- Auto-generated dependency code at compile time
- Clear error messages for missing dependencies

### Migration Statistics

- **Total Effort**: 22 hours across 9 phases
- **Code Files Modified**: 15+ files
- **ViewModels Converted**: 3 (ChatViewModel, SettingsViewModel, TeachAvaViewModel)
- **DI Modules Created**: 3 (DatabaseModule, RepositoryModule, AppModule)
- **Tests Created**: 19+ unit tests (100% pass rate)
- **Breaking Changes**: 0 (100% functional equivalency maintained)
- **Test Coverage**: 100% for Hilt-injected components

### Documentation

- **Migration Guide**: `docs/HILT-DI-MIGRATION-GUIDE.md` (365+ lines, step-by-step instructions)
- **Architecture Guide**: `docs/ARCHITECTURE.md` (complete DI section with Mermaid diagrams)
- **Developer Manual**: `docs/Developer-Manual-Chapter32-Hilt-DI.md` (comprehensive Hilt documentation)
- **Specification**: `.ideacode/specs/SPEC-hilt-di-implementation.md` (full 9-phase implementation spec)
- **Migration Report**: `docs/HILT-DI-MIGRATION-2025-11-13.md` (detailed migration notes)

### Contributors

- **Manoj Jhawar** (manoj@ideahq.net) - Architecture, implementation, testing, documentation
- **AI Assistant** (Claude Code) - Code generation, test creation, documentation writing

### Related Issues

- Issue #3: Hilt configuration exists but not being used
- Tech Debt: Manual DI with nullable dependencies and Context injection

---

## [0.8.0] - 2025-11-09

### Added - P8 Test Coverage Initiative (Week 3)

**Strategic Coverage**: Achieved 90-95% test coverage for Core and LLM modules

#### Core Module Tests (67 tests total)
- `ConversationMapperTest.kt` (16 tests) - Round-trip entity↔domain conversion
- `MessageMapperTest.kt` (24 tests) - All MessageRole types, JSON metadata handling
- `DecisionMapperTest.kt` (25 tests) - All DecisionType enum values, confidence/timestamp precision
- `IntentExampleMapperTest.kt` (2 tests) - Basic mapper validation

**Coverage**: ~90% for Core mapper layer

#### LLM Module Tests (52 new tests)
- `DownloadStateTest.kt` (52 tests) - Sealed class states, extension functions, format testing
  - All states: Downloading, Paused, Completed, Cancelled, Error, NotStarted, Verifying, Extracting
  - Extension functions: `canResume()`, `canPause()`, `canRetry()`, `isTerminal()`, `getProgressPercentage()`, `formatSpeed()`, `formatTimeRemaining()`
  - Edge cases: zero bytes, negative progress, null values, very large numbers

**Coverage**: ~100% for DownloadState sealed class

#### Test Patterns Established
- Round-trip testing (entity → domain → entity preservation)
- Edge case coverage (empty data, special characters, unicode, large datasets)
- Boundary testing (zero, max, negative values)
- Enum exhaustiveness (all enum values tested)
- Extension function testing (all util functions covered)

#### Metrics
- **Total Tests Created**: 117 tests (Core: 65, LLM: 52)
- **Time Investment**: 3.5 hours
- **Velocity**: 33.4 tests/hour average
- **Pass Rate**: 100% (all tests passing)

### Documentation
- Updated `docs/P8-TEST-COVERAGE-STATUS.md` with Week 3 completion status

---

## [0.7.0] - 2025-11-08

### Added - RAG Module Test Coverage (P8 Week 2)

**Comprehensive Coverage**: 138 tests across RAG module

#### Tests Created
- `DocumentRepositoryTest.kt` (26 tests) - Document CRUD, search, metadata updates
- `EmbeddingRepositoryTest.kt` (20 tests) - Embedding storage, retrieval, similarity search
- `ONNXEmbeddingProviderTest.kt` (40 tests) - ONNX model inference, batch processing
- `RAGQueryEngineTest.kt` (28 tests) - Query processing, context generation
- `DocumentProcessorTest.kt` (24 tests) - Document chunking, metadata extraction

**Coverage**: 95%+ across RAG module

#### Key Features Tested
- Document lifecycle (ingest → chunk → embed → search)
- Similarity search with cosine distance
- Batch embedding generation
- Context retrieval for LLM queries
- Error handling and edge cases

---

## [0.6.0] - 2025-11-06

### Added - Phase 6 Hilt ViewModel EntryPoint Learning

**Documentation**: Created comprehensive learning resource for Hilt `@EntryPoint` pattern

#### Document: `docs/Developer-Manual-Addendum-2025-11-09-Phase6-Hilt-ViewModel-EntryPoint-Learning.md`

**Purpose**: Explains `@EntryPoint` pattern for Services that cannot use `@AndroidEntryPoint`

**Key Learnings**:
- Services have unique lifecycle (cannot use `@AndroidEntryPoint`)
- `@EntryPoint` provides dependency access from `EntryPointAccessors`
- Pattern: Define interface → `@InstallIn(SingletonComponent)` → Access from Service
- Use case: OverlayService needs ActionsManager without Context injection

**Example**:
```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActionsManagerEntryPoint {
    fun actionsManager(): ActionsManager
}

// In OverlayService
val actionsManager = EntryPointAccessors
    .fromApplication(applicationContext, ActionsManagerEntryPoint::class.java)
    .actionsManager()
```

---

## [0.5.0] - 2025-11-05

### Added - Initial Project Setup

- Clean Architecture structure (Core → Features → Platform)
- Room database with 6 DAOs
- Repository pattern with 6 repositories
- NLU module with ONNX intent classification
- LLM module with response generation
- Chat UI with Jetpack Compose
- Teach-Ava training system
- Basic documentation structure

### Infrastructure
- IDEACODE v8.4 framework integration
- Git repository initialization
- Gradle multi-module build system
- Android SDK 24+ (target: 34)

---

[Unreleased]: https://github.com/augmentalis/ava/compare/v0.9.0...HEAD
[0.9.0]: https://github.com/augmentalis/ava/compare/v0.8.0...v0.9.0
[0.8.0]: https://github.com/augmentalis/ava/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/augmentalis/ava/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/augmentalis/ava/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/augmentalis/ava/releases/tag/v0.5.0
