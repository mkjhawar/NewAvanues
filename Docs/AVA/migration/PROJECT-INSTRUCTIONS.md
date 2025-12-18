# MainAvanues Monorepo - Project Instructions

**Version:** 1.0.0
**Last Updated:** 2025-11-28
**Purpose:** Centralized instructions for all apps and platforms - single reference point

---

## 📖 How to Use This File

**For AI assistants:**
1. Read this file at session start (along with FOLDER-REGISTRY.md)
2. Reference the specific app/platform section you're working on
3. Follow the instructions exactly as specified
4. Update this file when project requirements change

**For developers:**
1. Find your app section (VoiceOS, AVA, WebAvanue, AVAConnect)
2. Read Master instructions for overall architecture
3. Read Platform instructions for platform-specific details
4. Follow naming conventions and folder structure from FOLDER-REGISTRY.md

---

## 🎯 Global Monorepo Rules

### Code Sharing Strategy
- **Target:** 70%+ KMP code sharing across platforms
- **Priority:** Share business logic, database, models, domain code
- **Platform-specific:** UI, platform APIs, native integrations
- **Exception:** Apps may differ due to backend platform requirements

### Build System
- **Type:** Hybrid approach
- **Root build:** Single Gradle multi-module build (build.gradle.kts)
- **Shared modules:** Centralized in common/ with unified dependency versions
- **App builds:** Can have independent configurations when needed

### Team Model
- **Current:** Mixed/solo developer
- **Organization:** Platform-first structure (android/, ios/, desktop/)
- **Flexibility:** Structure supports both platform teams and feature teams

### Folder Structure Enforcement
- **ALWAYS** check FOLDER-REGISTRY.md before creating folders
- **NEVER** create folder variations (e.g., `voice-recognition/` vs `voice_recognition/`)
- **ALWAYS** use kebab-case for folders and files
- **NEVER** use type prefixes (use `authentication/` NOT `feature-authentication/`)

---

## 📱 VoiceOS

### Master Instructions (All Platforms)

**Location:** `docs/voiceos/Master/`
**Project Root:** `{platform}/voiceos/` (where platform = android, ios, desktop)

#### Overview
Voice-controlled operating system with accessibility features, plugin system, and universal IPC.

#### Architecture Principles
1. **Voice-First Design:** All features must be accessible via voice commands
2. **Accessibility:** Full support for users with disabilities
3. **Plugin System:** MagicCode plugins with KMP support (Android, iOS, JVM)
4. **Performance:** 10-20x faster initialization via embedding cache (target: 0.2-0.5s)
5. **Universal IPC:** Cross-process communication for system integration

#### Core Modules (common/)
- `common/voice/` - Voice recognition/synthesis (shared)
- `common/accessibility/` - Accessibility features (shared)
- `common/speech/` - Speech processing (shared)
- `common/plugin/` - Plugin system (shared)
- `common/ipc/` - Universal IPC (shared)
- `common/database/` - SQLDelight database (shared, post-consolidation)

#### Database Tables (38 total - SQLDelight 2.0.1)
See: `docs/voiceos/Master/database-schema.md`

**Key Tables:**
- Command management
- Voice data
- Plugin registry
- IPC channels
- Localization
- User settings

#### Performance Requirements
- **Initialization:** < 0.5s (via intent embedding cache)
- **Voice recognition latency:** < 200ms
- **Command execution:** < 100ms
- **Plugin load time:** < 300ms

#### Testing Requirements
- **Unit test coverage:** 90%+ on critical paths
- **Voice command testing:** All commands must have test cases
- **Accessibility testing:** WCAG 2.1 AA compliance
- **Performance benchmarks:** Must meet latency requirements

---

### Platform-Specific Instructions

#### Android (android/voiceos/)

**Location:** `docs/voiceos/Platform/android/`

**Android-Specific Features:**
- Android Accessibility Service integration
- Overlay window system (voice cursor)
- System-level voice commands
- Android IPC (Binder, ContentProvider)

**Build Configuration:**
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Kotlin version: 1.9.x
- AGP version: 8.x

**Key Dependencies:**
- Android Accessibility Framework
- Android Speech Recognition (native)
- Jetpack Compose (UI)
- SQLDelight Android Driver

**Platform-Specific Modules:**
- Voice Cursor (overlay)
- Accessibility service
- System command executor

#### iOS (ios/voiceos/)

**Location:** `docs/voiceos/Platform/ios/`

**iOS-Specific Features:**
- iOS Accessibility (VoiceOver integration)
- Siri Shortcuts integration
- System Extensions (where permitted)
- iOS IPC (XPC, App Groups)

**Build Configuration:**
- Min iOS: 14.0
- Target iOS: Latest
- Swift version: 5.x
- Xcode version: Latest stable

**Key Dependencies:**
- iOS Speech Framework
- AVFoundation (audio)
- SwiftUI or UIKit (UI)
- SQLDelight Native Driver

#### Desktop (desktop/voiceos/)

**Location:** `docs/voiceos/Platform/desktop/`

**Desktop-Specific Features:**
- System-wide hotkeys
- Desktop accessibility APIs (macOS, Windows, Linux)
- System tray integration
- Desktop IPC (D-Bus, COM, XPC)

**Build Configuration:**
- Platforms: macOS, Windows, Linux
- JVM target: 17+
- Compose Desktop (UI)

**Key Dependencies:**
- JNA (native access)
- Compose Desktop
- SQLDelight JVM Driver

---

## 🤖 AVA (AI Assistant)

### Master Instructions (All Platforms)

**Location:** `docs/ava/Master/`
**Project Root:** `{platform}/ava/` (where platform = android, ios, desktop)

#### Overview
Intelligent AI assistant with natural language understanding, large language model integration, and retrieval-augmented generation.

#### Architecture Principles
1. **Privacy-First:** All AI processing on-device when possible
2. **Fast Initialization:** 95% embedding cache hit rate (0.2s initialization)
3. **Dual Model Support:** MobileBERT-384 + mALBERT-768 embeddings
4. **RAG Architecture:** Retrieval-augmented generation for context-aware responses
5. **Teach-Ava System:** User feedback loop for continuous learning

#### Core Modules (common/)
- `common/nlu/` - Natural Language Understanding (shared)
- `common/llm/` - Large Language Model integration (shared)
- `common/rag/` - Retrieval-Augmented Generation (shared)
- `common/database/` - SQLDelight database (shared, post-consolidation)
- `common/cloud/` - Cloud sync services (shared)

#### Database Tables (11 total - SQLDelight 2.0.1)
See: `docs/ava/Master/database-schema.md`

**Key Tables:**
- `conversation` - Chat conversations
- `message` - Chat messages (CASCADE delete)
- `intent_embedding` - Pre-computed embeddings (CRITICAL for 95% faster init)
- `embedding_metadata` - Model version tracking
- `intent_example` - NLU training examples
- `train_example` - Teach-Ava examples
- `train_example_fts` - FTS4 full-text search (50-100x faster)
- `decision` - Decision logging
- `learning` - Feedback tracking
- `memory` - Long-term memory with embeddings
- `semantic_intent_ontology` - AVA 2.0 .aon support

#### Performance Requirements
- **NLU initialization:** < 0.2s (via embedding cache)
- **Intent matching:** < 50ms
- **LLM response:** < 2s (streaming)
- **RAG retrieval:** < 100ms
- **FTS search:** < 5ms

#### Model Files (ava-ai-models-external/)
- `embeddings/` (379 MB) - MobileBERT + mALBERT models
- `llm/` (2.1 GB) - Gemma, Phi, SmolLM models
- `wakeword/` (1 MB) - Porcupine wake word detection
- Total: 2.5 GB

#### Testing Requirements
- **Unit test coverage:** 90%+ on critical paths
- **NLU accuracy:** > 95% intent recognition
- **Embedding cache hit rate:** ≥ 95%
- **FTS search latency:** ≤ 5ms

---

### Platform-Specific Instructions

#### Android (android/ava/)

**Location:** `docs/ava/Platform/android/`

**Android-Specific Features:**
- On-device inference (ONNX Runtime, TFLite)
- Android ML Kit integration
- Wake word detection (Porcupine)
- Background service for continuous listening

**Build Configuration:**
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Kotlin version: 1.9.x
- KMP modules: Core, Data, Domain, Features (NLU, LLM, Chat, RAG)

**Key Dependencies:**
- ONNX Runtime Mobile
- TensorFlow Lite
- Jetpack Compose (UI)
- SQLDelight Android Driver
- Kotlinx Serialization

**Model Deployment:**
- Models stored in: `apps/ava-app-android/src/main/assets/models/`
- Side-load location: `/sdcard/ava-ai-models/`
- Deploy command: `adb push ava-ai-models-external/{embeddings,llm,wakeword}/ /sdcard/ava-ai-models/`

#### iOS (ios/ava/)

**Location:** `docs/ava/Platform/ios/`

**iOS-Specific Features:**
- Core ML integration
- Siri integration
- Apple Neural Engine optimization
- Wake word detection

**Build Configuration:**
- Min iOS: 14.0
- Target iOS: Latest
- Swift + Kotlin (KMP)
- Core ML models

**Key Dependencies:**
- Core ML
- Natural Language framework
- SQLDelight Native Driver

#### Desktop (desktop/ava/)

**Location:** `docs/ava/Platform/desktop/`

**Desktop-Specific Features:**
- ONNX Runtime (desktop)
- System-wide AI assistance
- Keyboard shortcuts
- Tray icon integration

**Build Configuration:**
- Platforms: macOS, Windows, Linux
- JVM target: 17+
- ONNX Runtime JNI

**Key Dependencies:**
- ONNX Runtime
- Compose Desktop
- SQLDelight JVM Driver

---

## 🌐 WebAvanue

### Master Instructions (All Platforms)

**Location:** `docs/webavanue/Master/`
**Project Root:** `{platform}/webavanue/` (where platform = android, ios, desktop)

#### Overview
Privacy-focused cross-platform browser with 95% KMP code sharing, advanced tab management, and performance optimizations.

#### Architecture Principles
1. **Privacy-First:** No tracking, built-in ad blocking
2. **95% KMP Sharing:** Maximum code reuse across platforms
3. **Performance:** Tab switching < 50ms, favorite lookup 20x faster
4. **Data Layer:** 407+ tests, 90%+ coverage (BrowserCoreData)
5. **Universal Design:** Consistent experience across platforms

#### Core Modules (common/)
- `common/browser-core/` - Browser engine logic (shared)
- `common/webview/` - WebView wrapper (shared)
- `common/database/` - SQLDelight database (shared, post-consolidation)
- `common/cloud/` - Sync services (shared)

#### Database Tables (7 total - SQLDelight)
See: `docs/webavanue/Master/database-schema.md`

**Key Tables:**
- `tab` - Browser tabs
- `history` - Browsing history
- `favorite` - Favorites/bookmarks
- `bookmark` - Bookmark folders
- `download` - Download manager
- `settings` - User preferences
- `auth` - Authentication data

#### Performance Requirements
- **Tab switching:** < 50ms (4x faster than baseline)
- **Favorite lookup:** < 10ms (20x faster than baseline)
- **Page load:** Optimized with caching
- **Startup time:** < 1s

#### Testing Requirements
- **Unit test coverage:** 90%+ (BrowserCoreData module)
- **Test count:** 407+ tests (passing)
- **Platform testing:** All platforms must pass full test suite

---

### Platform-Specific Instructions

#### Android (android/webavanue/)

**Location:** `docs/webavanue/Platform/android/`

**Android-Specific Features:**
- Android WebView integration
- Custom tabs support
- Download manager integration
- Android share sheet

**Build Configuration:**
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Kotlin version: 1.9.x
- 95% KMP code (only 5% Android-specific)

**Key Dependencies:**
- Android WebView
- Jetpack Compose (UI)
- SQLDelight Android Driver

#### iOS (ios/webavanue/)

**Location:** `docs/webavanue/Platform/ios/`

**iOS-Specific Features:**
- WKWebView integration
- Safari extension support (future)
- iOS share sheet
- iCloud sync

**Build Configuration:**
- Min iOS: 14.0
- Target iOS: Latest
- Swift + Kotlin (KMP)
- 95% KMP code

**Key Dependencies:**
- WebKit (WKWebView)
- SwiftUI or UIKit
- SQLDelight Native Driver

#### Desktop (desktop/webavanue/)

**Location:** `docs/webavanue/Platform/desktop/`

**Desktop-Specific Features:**
- JavaFX WebView or CEF (Chromium Embedded Framework)
- Desktop bookmarks bar
- Keyboard shortcuts
- Multi-window support

**Build Configuration:**
- Platforms: macOS, Windows, Linux
- JVM target: 17+
- Compose Desktop (UI)

**Key Dependencies:**
- JavaFX WebView or CEF
- Compose Desktop
- SQLDelight JVM Driver

---

## 🔌 AVAConnect

### Master Instructions (All Platforms)

**Location:** `docs/avaconnect/Master/`
**Project Root:** `{platform}/avaconnect/` (where platform = android, ios)

#### Overview
Connectivity and integration library for cross-app communication and service discovery.

#### Architecture Principles
1. **Universal IPC:** Cross-process communication
2. **Service Discovery:** Automatic detection of AVA/VoiceOS services
3. **Security:** Encrypted communication channels
4. **Lightweight:** Minimal overhead, fast initialization

#### Core Modules (common/)
- `common/ipc/` - Universal IPC (shared)
- `common/cloud/` - Cloud services (shared)
- `common/database/` - SQLDelight database (shared, post-consolidation)

#### Platform Support
- Android: Full support
- iOS: Full support
- Desktop: Planned (future)

---

### Platform-Specific Instructions

#### Android (android/avaconnect/)

**Location:** `docs/avaconnect/Platform/android/`

**Android-Specific Features:**
- Android IPC (Binder, AIDL)
- ContentProvider for data sharing
- Broadcast receivers
- Service binding

**Build Configuration:**
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Kotlin version: 1.9.x

#### iOS (ios/avaconnect/)

**Location:** `docs/avaconnect/Platform/ios/`

**iOS-Specific Features:**
- XPC framework
- App Groups
- URL schemes
- Universal Links

**Build Configuration:**
- Min iOS: 14.0
- Target iOS: Latest
- Swift + Kotlin (KMP)

---

## 📦 Common Modules (KMP Shared)

**Location:** `common/{module-name}/`

### Database Module (common/database/)

**Purpose:** Unified SQLDelight database (post-consolidation)

**Status:** Migration in progress (Phase 1 complete for AVA)

**Tables:** 42 tables (consolidated from 55 unique tables across 4 repos)

**Strategy:**
- Sequential migration: VoiceOS → AVA → WebAvanue
- INSERT OR REPLACE pattern (VOS4 proven strategy)
- Automatic rollback on failure
- Zero data loss

**Performance:**
- VoiceOS: 10-20x faster (4-5s → 0.2-0.5s)
- AVA: 95% embedding cache hit rate maintained
- WebAvanue: 20x faster favorite lookup maintained

### Voice Module (common/voice/)

**Purpose:** Voice recognition and synthesis (shared)

**Used By:** VoiceOS, AVA

**Platform Support:**
- Android: Android Speech Recognition API
- iOS: iOS Speech Framework
- Desktop: OS-specific APIs

### NLU Module (common/nlu/)

**Purpose:** Natural Language Understanding (shared)

**Used By:** AVA, VoiceOS

**Models:**
- MobileBERT-384 (lightweight)
- mALBERT-768 (high accuracy)

**Performance:**
- Initialization: < 0.2s (via embedding cache)
- Intent matching: < 50ms

### Other Shared Modules

- `common/accessibility/` - Accessibility features
- `common/llm/` - LLM integration
- `common/rag/` - RAG architecture
- `common/cloud/` - Cloud sync
- `common/ipc/` - Universal IPC
- `common/webview/` - WebView wrapper
- `common/browser-core/` - Browser logic
- `common/speech/` - Speech processing
- `common/plugin/` - Plugin system

---

## 🔧 Build System Instructions

### Root Build (build.gradle.kts)

**Type:** Single Gradle multi-module build

**Configuration:**
- Kotlin version: 1.9.x
- Gradle version: 8.x
- AGP version: 8.x (for Android)
- SQLDelight version: 2.0.1

**Modules:**
```kotlin
include(
    // Android apps
    ":android:voiceos",
    ":android:ava",
    ":android:webavanue",
    ":android:avaconnect",

    // iOS apps
    ":ios:voiceos",
    ":ios:ava",
    ":ios:webavanue",

    // Desktop apps
    ":desktop:voiceos",
    ":desktop:ava",
    ":desktop:webavanue",

    // Common modules
    ":common:database",
    ":common:voice",
    ":common:nlu",
    // ... etc
)
```

### Hybrid Build Strategy

**Shared Core:**
- Centralized version catalog (libs.versions.toml)
- Unified dependency management
- Shared Gradle plugins
- Common build logic

**App Independence:**
- Apps can override dependency versions when needed
- Platform-specific build configurations
- Independent release cycles

---

## 📝 Documentation Instructions

### Where to Document

| Document Type | Location | Filename Pattern |
|--------------|----------|------------------|
| **App Architecture** | `docs/{app}/Master/` | `{component}-architecture.md` |
| **App Specifications** | `docs/{app}/Master/` | `{feature}-spec.md` |
| **App Vision** | `docs/{app}/Master/` | `vision.md` or `README.md` |
| **App Flows** | `docs/{app}/Master/` | `{flow}-flow.md` or `.mmd` diagram |
| **Platform Implementation** | `docs/{app}/Platform/{platform}/` | `{feature}-implementation.md` |
| **Platform Build** | `docs/{app}/Platform/{platform}/` | `build-instructions.md` |
| **IDEACODE Specs** | `docs/ideacode/specs/` | `{number}-{name}/spec.md` |
| **IDEACODE Protocols** | `docs/ideacode/protocols/` | `Protocol-{Name}.md` |
| **Migration Docs** | `docs/migration/` | `{migration}-{type}-YYYYMMDD.md` |
| **Obsolete Docs** | `docs/archive/{platform}/` | Original filename preserved |

### Master vs Platform Documentation

**Master Documentation (docs/{app}/Master/):**
- Universal architecture (applies to all platforms)
- Core business logic and domain models
- API contracts and interfaces
- User flows and requirements
- Vision and strategic direction
- Database schemas (shared)

**Platform Documentation (docs/{app}/Platform/{platform}/):**
- Platform-specific implementation details
- Native API integrations
- Platform-specific UI/UX
- Build and deployment instructions
- Platform-specific testing
- Performance benchmarks (per platform)

### File Naming Conventions

**Always use kebab-case:**
- ✅ `voice-recognition-architecture.md`
- ❌ `VoiceRecognitionArchitecture.md`
- ❌ `voice_recognition_architecture.md`

**NO type prefixes:**
- ✅ `authentication-spec.md`
- ❌ `spec-authentication.md`

**Timestamped files (reports, ADRs):**
- Pattern: `{name}-YYYYMMDDHHMM.md`
- Example: `migration-progress-20251128.md`

---

## 🗃️ Archive Instructions

### When to Archive

Archive documentation when:
1. Feature has been removed from codebase
2. Document is obsolete (superseded by newer version)
3. Platform is deprecated
4. Document is no longer relevant but has historical value

### Archive Structure

**Location:** `docs/archive/{platform}/`

**Platforms:**
- `docs/archive/android/` - Archived Android docs
- `docs/archive/ios/` - Archived iOS docs
- `docs/archive/desktop/` - Archived Desktop docs

### Archive Process

1. **Identify obsolete document**
2. **Move to archive:** `docs/archive/{platform}/{original-filename}`
3. **Preserve filename:** Keep original filename for reference
4. **Add archive note:** Add header to archived file:
   ```markdown
   # ARCHIVED

   **Date Archived:** YYYY-MM-DD
   **Reason:** [Why archived]
   **Superseded By:** [Link to new doc, if applicable]

   ---

   [Original content below]
   ```
5. **Update references:** Remove links to archived doc from active docs

### DO NOT Archive

**NEVER archive:**
- Specifications (specs) - Keep in `docs/ideacode/specs/`
- Architecture documents - Keep in `docs/{app}/Master/`
- Migration docs - Keep in `docs/migration/`
- .mmd diagram files - Keep unless completely irrelevant

---

## 🚀 Migration Instructions (Monorepo Consolidation)

### Current Status
**Phase:** Planning
**Target:** Consolidate VoiceOS, AVA, WebAvanue, MainAvanues into single monorepo

### Migration Principles
1. **Systematic file placement** - Check FOLDER-REGISTRY.md before moving files
2. **Obsolete file removal** - Remove files not used for reference
3. **Documentation preservation** - Keep manuals, specs, architecture, .mmd files
4. **Archive non-relevant** - Move obsolete docs to `docs/archive/{platform}/`
5. **Follow naming conventions** - Use kebab-case, no type prefixes
6. **Update registries** - Update FOLDER-REGISTRY.md as folders are created

### Files to Preserve
- ✅ All specifications
- ✅ All architecture documents
- ✅ All .mmd diagram files
- ✅ All developer manuals
- ✅ All migration guides
- ✅ All ADRs (Architecture Decision Records)

### Files to Remove
- ❌ Duplicate files (keep newest version)
- ❌ Build artifacts (.apk, .ipa, .jar, .class, .o)
- ❌ IDE files (.idea, .vscode, *.swp)
- ❌ Temporary files (.tmp, .bak, .cache)
- ❌ Obsolete code (commented-out, unused modules)

---

## 🔄 Update Instructions

### When to Update This File

Update PROJECT-INSTRUCTIONS.md when:
1. Adding a new app to the monorepo
2. Changing architecture principles
3. Updating performance requirements
4. Changing build system configuration
5. Modifying documentation structure
6. Adding new shared modules

### Update Process

1. **Edit section** - Modify the relevant app/platform section
2. **Update version** - Increment version at top of file
3. **Update timestamp** - Change "Last Updated" date
4. **Document change** - Add entry to "Update History" section below
5. **Commit changes** - Create git commit with description

---

## 📊 Update History

| Date | Section | Change | Author |
|------|---------|--------|--------|
| 2025-11-28 | All | Initial creation of centralized project instructions | System |

---

## 🔍 Quick Reference

**Before starting work:**
1. ✅ Read FOLDER-REGISTRY.md
2. ✅ Read this file (PROJECT-INSTRUCTIONS.md)
3. ✅ Check relevant app section
4. ✅ Check relevant platform section
5. ✅ Follow naming conventions
6. ✅ Update registries after creating folders

**Before committing:**
1. ✅ Verify folder names match FOLDER-REGISTRY.md
2. ✅ Verify file names use kebab-case
3. ✅ Update FOLDER-REGISTRY.md if new folders created
4. ✅ Update PROJECT-INSTRUCTIONS.md if requirements changed
5. ✅ Run tests (90%+ coverage required)
6. ✅ Document changes in commit message

---

**CRITICAL:** This file is the single source of truth for project instructions.
**ALWAYS** reference this file when working on any app or platform.
