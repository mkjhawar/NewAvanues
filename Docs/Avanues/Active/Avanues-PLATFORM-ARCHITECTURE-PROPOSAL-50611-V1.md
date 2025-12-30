# AVANUES Platform Architecture Proposal

**Date**: 2025-11-06
**Status**: 🔵 PROPOSAL FOR REVIEW
**Version**: 1.0.0

---

## Executive Summary

This document proposes the complete architecture for **AVANUES** - a unified cross-platform development platform that consolidates:
- **AvaUI** - Declarative UI framework
- **AvaCode** - Forms & Workflows DSL
- **MagicData** - Database system (renamed from Database)
- **VOS4** - Voice OS components
- **Additional components** (proposed below)

**Key Changes**:
1. Rename `Avanues` → `AVANUES`
2. Rename `Universal/IDEAMagic/Database` → `Universal/IDEAMagic/MagicData`
3. Integrate VOS4 into AVANUES structure
4. Add supporting infrastructure components

---

## Table of Contents

1. [Platform Vision](#platform-vision)
2. [Naming Strategy](#naming-strategy)
3. [Proposed Structure](#proposed-structure)
4. [Component Breakdown](#component-breakdown)
5. [VOS4 Integration](#vos4-integration)
6. [Additional Recommended Components](#additional-recommended-components)
7. [Implementation Plan](#implementation-plan)
8. [Migration Impact Analysis](#migration-impact-analysis)

---

## Platform Vision

### What is AVANUES?

**AVANUES** is a unified platform for building cross-platform applications with voice, accessibility, and data-driven capabilities.

```
┌─────────────────────────────────────────────────────────────────┐
│                        AVANUES Platform                          │
│  "Augmentalis Voice-Accessible Application Universal Framework" │
└─────────────────────────────────────────────────────────────────┘

        ┌──────────────┬──────────────┬──────────────┬──────────────┐
        │   AvaUI    │  AvaCode   │  MagicData   │     VOS4     │
        │              │              │              │              │
        │ Declarative  │ Forms &      │  Database    │  Voice OS    │
        │ UI Framework │ Workflows    │  System      │  Components  │
        └──────────────┴──────────────┴──────────────┴──────────────┘

                             Built for:
        ┌──────────────┬──────────────┬──────────────┬──────────────┐
        │   AVA AI     │  AVAConnect  │ BrowserAvanue│    Future    │
        │              │              │              │     Apps     │
        └──────────────┴──────────────┴──────────────┴──────────────┘
```

### Why "AVANUES"?

**Etymology**:
- **AVA** - Core product family (AVA AI, AVAConnect, AVAvanue apps)
- **AVENUE** - Path/platform for building applications
- **AVANUES** - Platform providing multiple avenues for app development

**Not "Avanues"** because:
- Platform supports non-voice apps (BrowserAvanue)
- AvaUI/AvaCode/MagicData are general-purpose frameworks
- "Voice" implies audio-only, AVANUES is multimodal

---

## Naming Strategy

### Primary Rename: Avanues → AVANUES

**Scope of Changes**:
```bash
# Directory rename
/Volumes/M-Drive/Coding/Avanues → /Volumes/M-Drive/Coding/AVANUES

# Package names (keep existing for backwards compatibility)
com.augmentalis.avanues → Keep as-is (Android package names shouldn't change)

# Project/display names
Avanues → AVANUES (in documentation, UI, branding)
```

**Rationale**:
- Directory reflects new platform identity
- Package names stay for app store consistency
- User-facing branding updated to AVANUES

### Secondary Rename: Database → MagicData

**Scope of Changes**:
```bash
# Module path
Universal/IDEAMagic/Database → Universal/IDEAMagic/MagicData

# Package names
com.augmentalis.avamagic.database → com.augmentalis.avamagic.magicdata

# Class names
Database → MagicDataClient (for clarity)
DatabaseService → MagicDataService
DatabaseClient → MagicDataClient
```

**Rationale**:
- Consistency with AvaUI, AvaCode naming
- "MagicData" emphasizes data management capabilities
- More memorable brand identity

---

## Proposed Structure

### High-Level Directory Structure

```
AVANUES/
├── Universal/
│   └── IDEAMagic/
│       ├── AvaUI/              # UI Framework (Phases 1-4)
│       │   ├── Foundation/       # Colors, Typography, Layout
│       │   ├── Core/             # Components, Transforms
│       │   └── Adapters/         # Compose, SwiftUI, HTML renderers
│       │
│       ├── AvaCode/            # DSL Framework (Phases 5-6)
│       │   ├── Forms/            # Form validation DSL
│       │   └── Workflows/        # State machine DSL
│       │
│       ├── MagicData/            # Database System (renamed)
│       │   ├── Core/             # Collection-based storage
│       │   ├── IPC/              # AIDL/ContentProvider
│       │   └── Adapters/         # SQLite, Realm, MongoDB
│       │
│       ├── VOS4/                 # Voice OS Components
│       │   ├── Core/             # Voice engine abstraction
│       │   ├── Recognition/      # Speech-to-text
│       │   ├── Synthesis/        # Text-to-speech
│       │   ├── NLU/              # Natural language understanding
│       │   └── Commands/         # Voice command routing
│       │
│       ├── Templates/            # App Templates (Phase 7)
│       │   ├── Core/             # Template engine
│       │   └── Library/          # E-Commerce, Task Management, etc.
│       │
│       ├── Plugins/              # Plugin Infrastructure
│       │   ├── Core/             # Plugin manager
│       │   ├── Registry/         # Plugin discovery
│       │   └── Security/         # Signature verification
│       │
│       └── IPC/                  # IPC Infrastructure
│           ├── AIDL/             # AIDL interfaces
│           ├── ContentProvider/  # ContentProvider base
│           └── Protocols/        # IPC protocol definitions
│
├── android/
│   ├── app/                      # Main AVANUES Android app
│   ├── avanues/
│   │   ├── libraries/            # Android-specific libraries
│   │   └── modules/              # Internal modules
│   └── plugins/                  # External plugins (separate apps)
│
├── ios/
│   ├── AVANUES/                  # Main AVANUES iOS app
│   ├── Extensions/               # App Extensions
│   └── Frameworks/               # iOS frameworks
│
├── desktop/
│   ├── macos/                    # macOS app
│   ├── windows/                  # Windows app
│   └── linux/                    # Linux app
│
├── apps/                         # Sample/demo apps
│   ├── AVA-AI/                   # AVA AI reference app
│   ├── AVAConnect/               # AVAConnect reference app
│   └── demos/                    # Demo applications
│
├── docs/                         # Documentation
│   ├── Active/                   # Current work docs
│   ├── Future-Ideas/             # Future enhancements
│   ├── Archive/                  # Historical docs
│   └── manuals/                  # Developer manuals
│
└── tools/                        # Development tools
    ├── cli/                      # AVANUES CLI
    ├── generators/               # Code generators
    └── validators/               # Validation tools
```

### Module Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                         AVANUES Apps                             │
│              (AVA AI, AVAConnect, BrowserAvanue)                 │
└───────────────────────┬─────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  AvaCode   │ │   AvaUI    │ │    VOS4      │
│ (Forms/      │ │ (Components) │ │ (Voice)      │
│  Workflows)  │ │              │ │              │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       │                │                │
       └────────────────┼────────────────┘
                        │
                        ▼
                ┌──────────────┐
                │  MagicData   │
                │ (Database)   │
                └──────┬───────┘
                       │
                       ▼
                ┌──────────────┐
                │  IPC Layer   │
                │ (AIDL/CP)    │
                └──────────────┘
```

---

## Component Breakdown

### 1. AvaUI - Declarative UI Framework

**What**: Cross-platform UI framework with declarative components

**Structure**:
```
Universal/IDEAMagic/AvaUI/
├── Foundation/
│   ├── Colors.kt           # Color system
│   ├── Typography.kt       # Font system
│   ├── Layout.kt           # Layout primitives
│   ├── Spacing.kt          # Spacing scale
│   └── Elevation.kt        # Shadow/elevation
│
├── Core/
│   ├── form/               # 4 form components
│   ├── display/            # 8 display components
│   ├── feedback/           # 4 feedback components
│   ├── layout/             # 4 layout components
│   ├── opengl/             # 3D transforms
│   └── camera/             # 3D camera
│
└── Adapters/
    ├── compose/            # Jetpack Compose renderer
    ├── swiftui/            # SwiftUI renderer
    └── html/               # HTML/CSS renderer
```

**Status**: ✅ Complete (Phases 1-4, 73 files, 9,700 LOC)

### 2. AvaCode - Forms & Workflows DSL

**What**: Domain-specific language for forms and workflows

**Structure**:
```
Universal/IDEAMagic/AvaCode/
├── Forms/
│   ├── Form.kt             # Form definition
│   ├── Field.kt            # Field types
│   ├── Validation.kt       # Validation rules
│   ├── Renderer.kt         # UI rendering
│   └── Submission.kt       # Form submission
│
└── Workflows/
    ├── Workflow.kt         # State machine DSL
    ├── State.kt            # State definitions
    ├── Transition.kt       # Transitions
    ├── Action.kt           # Side effects
    └── Renderer.kt         # Visual editor
```

**Status**: ✅ Complete (Phases 5-6)

### 3. MagicData - Database System

**What**: Collection-based database with IPC access

**Structure**:
```
Universal/IDEAMagic/MagicData/
├── Core/
│   ├── MagicDataClient.kt    # Main client (renamed from Database)
│   ├── Collection.kt          # Collection API
│   ├── Document.kt            # Document model
│   ├── Query.kt               # Query builder
│   └── Index.kt               # Indexing
│
├── IPC/
│   ├── MagicDataService.kt    # AIDL service
│   ├── IMagicData.aidl        # AIDL interface
│   ├── MagicDataProvider.kt   # ContentProvider
│   └── Serialization.kt       # Parcelable helpers
│
└── Adapters/
    ├── SQLiteAdapter.kt       # SQLite backend
    ├── RealmAdapter.kt        # Realm backend
    └── MongoAdapter.kt        # MongoDB backend (future)
```

**Changes Required**:
- Rename `Database` → `MagicDataClient`
- Rename `DatabaseService` → `MagicDataService`
- Update package: `com.augmentalis.avamagic.magicdata`
- Update all import statements

**Status**: ⚠️ Needs renaming

### 4. VOS4 - Voice OS Components

**What**: Voice operating system components for speech recognition, synthesis, NLU

**Proposed Structure**:
```
Universal/IDEAMagic/VOS4/
├── Core/
│   ├── VoiceEngine.kt         # Abstract voice engine
│   ├── VoiceSession.kt        # Session management
│   ├── VoiceConfig.kt         # Configuration
│   └── VoiceState.kt          # State management
│
├── Recognition/
│   ├── SpeechRecognizer.kt    # STT interface
│   ├── engines/
│   │   ├── WhisperEngine.kt   # OpenAI Whisper
│   │   ├── VoskEngine.kt      # VOSK
│   │   └── AndroidSTT.kt      # Android built-in
│   └── models/                # Language models
│
├── Synthesis/
│   ├── TextToSpeech.kt        # TTS interface
│   ├── engines/
│   │   ├── GoogleTTS.kt       # Google TTS
│   │   └── AndroidTTS.kt      # Android built-in
│   └── voices/                # Voice profiles
│
├── NLU/
│   ├── IntentParser.kt        # Intent detection
│   ├── EntityExtractor.kt     # Named entity recognition
│   ├── ContextManager.kt      # Conversation context
│   └── models/                # NLU models
│
└── Commands/
    ├── CommandRouter.kt       # Route voice commands
    ├── CommandRegistry.kt     # Command definitions
    └── handlers/              # Command handlers
```

**Integration Plan**: See [VOS4 Integration](#vos4-integration) section below

**Status**: 🔵 To be integrated

### 5. Templates - App Generation (Phase 7)

**What**: Generate complete apps from declarative configuration

**Structure**:
```
Universal/IDEAMagic/Templates/
├── Core/
│   ├── Feature.kt             # 70 feature flags
│   ├── TemplateMetadata.kt    # Template metadata
│   ├── AppTemplate.kt         # Template interface
│   ├── AppConfig.kt           # DSL builder
│   ├── BrandingConfig.kt      # App branding
│   ├── DatabaseConfig.kt      # Database setup
│   └── TemplateGenerator.kt   # Code generation
│
└── Library/
    ├── ECommerceTemplate.kt   # E-commerce app
    ├── TaskTemplate.kt        # Task management
    ├── SocialTemplate.kt      # Social media
    ├── LMSTemplate.kt         # Learning management
    └── HealthTemplate.kt      # Healthcare app
```

**Status**: 🟡 In Progress (Week 1 complete, Week 2+ pending)

---

## VOS4 Integration

### Current VOS4 Status

**Question**: Where is VOS4 now?

**Locations to check**:
1. `/Volumes/M-Drive/Coding/Warp/vos4/` - Standalone VOS4 project?
2. `/Volumes/M-Drive/Coding/Avanues/android/avanues/libraries/` - Already integrated?
3. Separate repository?

### Integration Strategy

#### Option A: VOS4 as KMP Module (Recommended)

**If VOS4 is voice-specific code**:
```
Universal/IDEAMagic/VOS4/
└── [structure from Component Breakdown above]
```

**Advantages**:
- ✅ Cross-platform (Android, iOS, Desktop)
- ✅ Consistent with AvaUI/AvaCode/MagicData
- ✅ Shareable across all AVANUES apps
- ✅ Single codebase for voice features

**Migration**:
```bash
# 1. Create structure
mkdir -p Universal/IDEAMagic/VOS4/{Core,Recognition,Synthesis,NLU,Commands}

# 2. Copy existing VOS4 code
cp -r [current-vos4-location]/* Universal/IDEAMagic/VOS4/

# 3. Convert to KMP
# - Create build.gradle.kts with kotlin("multiplatform")
# - Split platform-specific code (androidMain, iosMain)
# - Update package names to com.augmentalis.avamagic.vos4

# 4. Update settings.gradle.kts
include(":Universal:IDEAMagic:VOS4:Core")
include(":Universal:IDEAMagic:VOS4:Recognition")
# ... etc
```

#### Option B: VOS4 as Android Library

**If VOS4 has heavy Android dependencies**:
```
android/avanues/libraries/vos4/
└── [existing structure]
```

**Advantages**:
- ✅ Keep existing Android-specific code as-is
- ✅ No migration needed

**Disadvantages**:
- ❌ Not available for iOS/Desktop
- ❌ Can't be used in BrowserAvanue
- ❌ Less consistent with platform architecture

### Recommended Approach

**Hybrid Strategy**:
1. **Core voice abstractions** → Universal/IDEAMagic/VOS4/Core (KMP)
2. **Platform implementations** → androidMain/iosMain/desktopMain
3. **Android-specific engines** → Keep in android/avanues/libraries/

**Example**:
```kotlin
// Universal/IDEAMagic/VOS4/Core/src/commonMain/kotlin/
interface VoiceEngine {
    suspend fun recognize(audio: ByteArray): RecognitionResult
    suspend fun synthesize(text: String): ByteArray
}

// Universal/IDEAMagic/VOS4/Core/src/androidMain/kotlin/
class AndroidVoiceEngine : VoiceEngine {
    // Android-specific implementation using WhisperEngine
}

// Universal/IDEAMagic/VOS4/Core/src/iosMain/kotlin/
class IOSVoiceEngine : VoiceEngine {
    // iOS-specific implementation using Speech framework
}
```

**Migration Effort**: 8-16 hours (depends on current VOS4 structure)

---

## Additional Recommended Components

### 6. Plugins - Plugin Infrastructure

**Why**: Centralize plugin management for external modules

**Structure**:
```
Universal/IDEAMagic/Plugins/
├── Core/
│   ├── PluginManager.kt       # Discover and load plugins
│   ├── Plugin.kt              # Plugin interface
│   ├── PluginMetadata.kt      # Plugin manifest
│   └── PluginState.kt         # Plugin lifecycle
│
├── Registry/
│   ├── PluginRegistry.kt      # Available plugins
│   ├── PluginInstaller.kt     # Install/uninstall
│   └── PluginUpdater.kt       # Updates
│
└── Security/
    ├── SignatureVerifier.kt   # Verify plugin signatures
    ├── PermissionManager.kt   # Plugin permissions
    └── Sandbox.kt             # Plugin sandboxing
```

**Use Case**: Managing external plugins like VoiceKeyboard, AVAAssistant

**Status**: 🔵 Proposed (needed for plugin architecture)

### 7. IPC - IPC Infrastructure

**Why**: Centralize all IPC code (AIDL + ContentProvider)

**Structure**:
```
Universal/IDEAMagic/IPC/
├── AIDL/
│   ├── IAIDLService.kt        # Base AIDL service
│   ├── AIDLClient.kt          # Base AIDL client
│   └── Serialization.kt       # Parcelable helpers
│
├── ContentProvider/
│   ├── BaseContentProvider.kt # Base provider
│   ├── ProviderClient.kt      # Provider client
│   └── URIBuilder.kt          # URI construction
│
└── Protocols/
    ├── RequestProtocol.kt     # Request format
    ├── ResponseProtocol.kt    # Response format
    └── ErrorProtocol.kt       # Error handling
```

**Use Case**: MagicData IPC, Plugin IPC, cross-process communication

**Status**: 🟡 Partially exists in MagicData, should be extracted

### 8. Renderers - Platform Renderers

**Why**: Abstract AvaUI rendering to different platforms

**Structure**:
```
Universal/IDEAMagic/Renderers/
├── Compose/
│   ├── ComposeRenderer.kt     # Jetpack Compose renderer
│   └── modifiers/             # Compose-specific modifiers
│
├── SwiftUI/
│   ├── SwiftUIRenderer.kt     # SwiftUI renderer
│   └── modifiers/             # SwiftUI-specific modifiers
│
└── HTML/
    ├── HTMLRenderer.kt        # HTML/CSS renderer
    └── styles/                # CSS generation
```

**Note**: This may already exist in `AvaUI/Adapters/` - verify and possibly rename

**Status**: ✅ May already exist

### 9. CLI - Command-Line Tools

**Why**: Developer tools for AVANUES projects

**Structure**:
```
tools/cli/
├── bin/
│   └── avanues               # CLI binary
├── commands/
│   ├── create.ts             # Create new project
│   ├── generate.ts           # Generate code
│   ├── build.ts              # Build project
│   ├── test.ts               # Run tests
│   └── plugin.ts             # Plugin management
└── package.json
```

**Commands**:
```bash
avanues create my-app --template ecommerce
avanues generate component MyButton
avanues generate form UserRegistration
avanues plugin install voice-keyboard
avanues build android --release
```

**Status**: 🔵 Proposed (would accelerate development)

### 10. Validators - Code Quality Tools

**Why**: Enforce AVANUES best practices

**Structure**:
```
tools/validators/
├── UIValidator.kt            # Validate AvaUI usage
├── FormValidator.kt          # Validate AvaCode forms
├── WorkflowValidator.kt      # Validate workflows
├── PluginValidator.kt        # Validate plugin structure
└── AccessibilityValidator.kt # WCAG compliance
```

**Use Cases**:
- Validate WCAG 2.1 AA compliance
- Check for anti-patterns
- Verify plugin security

**Status**: 🔵 Proposed (quality assurance)

---

## Implementation Plan

### Phase 1: Rename Operations (4-6 hours)

**Week 1, Days 1-2**

#### Task 1.1: Rename Avanues → AVANUES
```bash
# 1. Commit current work
git add .
git commit -m "Save work before AVANUES rename"

# 2. Rename directory
cd /Volumes/M-Drive/Coding/
mv Avanues AVANUES

# 3. Update git remote (if needed)
cd AVANUES
git remote set-url origin [new-url-if-changed]

# 4. Update documentation references
find docs/ -type f -name "*.md" -exec sed -i '' 's/Avanues/AVANUES/g' {} +

# 5. Update README
sed -i '' 's/Avanues/AVANUES/g' README.md

# 6. Update CLAUDE.md
sed -i '' 's/Avanues/AVANUES/g' CLAUDE.md
```

**Files to update**:
- All `docs/**/*.md` files
- README.md
- CLAUDE.md
- settings.gradle.kts (comments only)
- .ideacode-v2/config.yml

**Note**: DO NOT change Android package names (com.augmentalis.avanues) - breaks app store

#### Task 1.2: Rename Database → MagicData
```bash
# 1. Rename directory
cd Universal/IDEAMagic/
git mv Database MagicData

# 2. Update package declarations
find MagicData/ -name "*.kt" -exec sed -i '' 's/package com\.augmentalis\.avamagic\.database/package com.augmentalis.avamagic.magicdata/g' {} +

# 3. Update imports across entire project
find . -name "*.kt" -exec sed -i '' 's/import com\.augmentalis\.avamagic\.database/import com.augmentalis.avamagic.magicdata/g' {} +

# 4. Rename class: Database → MagicDataClient
sed -i '' 's/class Database/class MagicDataClient/g' MagicData/Core/src/commonMain/kotlin/com/augmentalis/avamagic/magicdata/Database.kt
mv MagicData/Core/src/commonMain/kotlin/com/augmentalis/avamagic/magicdata/Database.kt \
   MagicData/Core/src/commonMain/kotlin/com/augmentalis/avamagic/magicdata/MagicDataClient.kt

# 5. Update references to Database class
find . -name "*.kt" -exec sed -i '' 's/Database\.getInstance/MagicDataClient.getInstance/g' {} +

# 6. Update build.gradle.kts module paths
find . -name "build.gradle.kts" -exec sed -i '' 's/:Universal:IDEAMagic:Database/:Universal:IDEAMagic:MagicData/g' {} +

# 7. Update settings.gradle.kts
sed -i '' 's/include(":Universal:IDEAMagic:Database/include(":Universal:IDEAMagic:MagicData/g' settings.gradle.kts
```

**Verification**:
```bash
# Build all modules
./gradlew build

# Expected: 0 errors (all references updated)
```

**Estimated time**: 2-3 hours

### Phase 2: VOS4 Integration (8-16 hours)

**Week 1, Days 3-5**

#### Task 2.1: Analyze Current VOS4 Location
```bash
# Find existing VOS4 code
find /Volumes/M-Drive/Coding/ -type d -name "*vos4*" -o -name "*VOS4*"

# Check likely locations:
# 1. /Volumes/M-Drive/Coding/Warp/vos4/
# 2. /Volumes/M-Drive/Coding/AVANUES/android/avanues/libraries/
# 3. Standalone repo
```

**Deliverable**: VOS4 location analysis document

#### Task 2.2: Create VOS4 KMP Structure
```bash
# 1. Create module structure
mkdir -p Universal/IDEAMagic/VOS4/{Core,Recognition,Synthesis,NLU,Commands}

# 2. Create build.gradle.kts for each module
# (Use AvaUI/Foundation/build.gradle.kts as template)

# 3. Update settings.gradle.kts
echo "include(':Universal:IDEAMagic:VOS4:Core')" >> settings.gradle.kts
echo "include(':Universal:IDEAMagic:VOS4:Recognition')" >> settings.gradle.kts
# ... etc
```

#### Task 2.3: Migrate VOS4 Code
```bash
# 1. Copy existing code
cp -r [vos4-location]/core/* Universal/IDEAMagic/VOS4/Core/src/commonMain/kotlin/

# 2. Update package names
find Universal/IDEAMagic/VOS4/ -name "*.kt" -exec sed -i '' 's/package com\.augmentalis\.vos4/package com.augmentalis.avamagic.vos4/g' {} +

# 3. Split platform-specific code
# Move Android-specific code → androidMain/
# Move iOS-specific code → iosMain/
# Keep common abstractions → commonMain/

# 4. Build and fix errors
./gradlew :Universal:IDEAMagic:VOS4:Core:build
```

**Estimated time**: 8-12 hours (depends on VOS4 complexity)

### Phase 3: Add New Components (16-24 hours)

**Week 2**

#### Task 3.1: Create IPC Module
```bash
# 1. Extract IPC code from MagicData
mkdir -p Universal/IDEAMagic/IPC/{AIDL,ContentProvider,Protocols}

# 2. Move AIDL files
mv Universal/IDEAMagic/MagicData/IPC/* Universal/IDEAMagic/IPC/AIDL/

# 3. Create base classes
# - BaseAIDLService.kt
# - BaseContentProvider.kt

# 4. Update MagicData to use IPC module
# implementation(project(":Universal:IDEAMagic:IPC:AIDL"))
```

**Estimated time**: 6-8 hours

#### Task 3.2: Create Plugins Module
```bash
# 1. Create structure
mkdir -p Universal/IDEAMagic/Plugins/{Core,Registry,Security}

# 2. Implement PluginManager
# 3. Implement PluginRegistry
# 4. Implement SignatureVerifier

# 5. Test with sample plugin
```

**Estimated time**: 8-12 hours

#### Task 3.3: Create CLI Tool (Optional)
```bash
# 1. Create tools/cli/ directory
mkdir -p tools/cli/{bin,commands}

# 2. Initialize npm project
cd tools/cli
npm init -y

# 3. Install dependencies
npm install commander chalk inquirer

# 4. Implement commands
# - create.ts (create new project)
# - generate.ts (generate code)
```

**Estimated time**: 4-6 hours

### Phase 4: Documentation Updates (4-6 hours)

**Week 2**

#### Task 4.1: Update Developer Manuals
- [ ] Update IDEAMAGIC-UI-DEVELOPER-MANUAL.md (add VOS4 chapter)
- [ ] Update AI-Module-Porting-Guide.md (use MagicData instead of Database)
- [ ] Create AVANUES-PLATFORM-GUIDE.md (overview for new developers)

#### Task 4.2: Update Module READMEs
- [ ] Universal/IDEAMagic/AvaUI/README.md
- [ ] Universal/IDEAMagic/AvaCode/README.md
- [ ] Universal/IDEAMagic/MagicData/README.md (update from Database)
- [ ] Universal/IDEAMagic/VOS4/README.md (create new)

#### Task 4.3: Update Architecture Docs
- [ ] Update IPC-Module-Plugin-Data-Exchange-Flow.md (use MagicData)
- [ ] Update PLUGIN-ARCHITECTURE-ANALYSIS-20251106.md (AVANUES branding)

**Estimated time**: 4-6 hours

### Phase 5: Testing & Validation (8-12 hours)

**Week 3**

#### Task 5.1: Build Verification
```bash
# 1. Clean build
./gradlew clean

# 2. Build all modules
./gradlew build

# Expected: 0 errors
```

#### Task 5.2: Module Integration Tests
```bash
# Test MagicData (renamed)
./gradlew :Universal:IDEAMagic:MagicData:Core:test

# Test VOS4 (integrated)
./gradlew :Universal:IDEAMagic:VOS4:Core:test

# Test IPC
./gradlew :Universal:IDEAMagic:IPC:AIDL:test
```

#### Task 5.3: App-Level Testing
```bash
# Build AVA AI app with new structure
./gradlew :android:app:assembleDebug

# Test on device
adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# Verify:
# - MagicData works (renamed from Database)
# - VOS4 components accessible
# - IPC communication functional
```

**Estimated time**: 8-12 hours

---

## Migration Impact Analysis

### Code Changes

#### Files to Rename
| Old Path | New Path | Impact |
|----------|----------|--------|
| `Avanues/` | `AVANUES/` | Low (directory only) |
| `Universal/IDEAMagic/Database/` | `Universal/IDEAMagic/MagicData/` | **High** (all imports) |
| `Database.kt` | `MagicDataClient.kt` | **High** (all usages) |

#### Package Name Changes
| Old Package | New Package | Files Affected |
|-------------|-------------|----------------|
| `com.augmentalis.avamagic.database` | `com.augmentalis.avamagic.magicdata` | ~50 files |
| `com.augmentalis.vos4` | `com.augmentalis.avamagic.vos4` | ~30 files (estimated) |

#### Import Statement Changes
```kotlin
// Before
import com.augmentalis.avamagic.database.Database
import com.augmentalis.avamagic.database.Collection

// After
import com.augmentalis.avamagic.magicdata.MagicDataClient
import com.augmentalis.avamagic.magicdata.Collection
```

**Estimated files affected**: 100-150 Kotlin files

### Build System Changes

#### settings.gradle.kts
```kotlin
// Before
include(":Universal:IDEAMagic:Database:Core")
include(":Universal:IDEAMagic:Database:IPC")

// After
include(":Universal:IDEAMagic:MagicData:Core")
include(":Universal:IDEAMagic:MagicData:IPC")
include(":Universal:IDEAMagic:VOS4:Core")
include(":Universal:IDEAMagic:VOS4:Recognition")
include(":Universal:IDEAMagic:Plugins:Core")
include(":Universal:IDEAMagic:IPC:AIDL")
```

#### build.gradle.kts (per module)
```kotlin
// Before
dependencies {
    implementation(project(":Universal:IDEAMagic:Database:Core"))
}

// After
dependencies {
    implementation(project(":Universal:IDEAMagic:MagicData:Core"))
    implementation(project(":Universal:IDEAMagic:VOS4:Core"))
}
```

**Estimated files affected**: 30-40 build.gradle.kts files

### Documentation Changes

#### README.md Files
- [ ] Root README.md
- [ ] Universal/IDEAMagic/README.md
- [ ] Universal/IDEAMagic/AvaUI/README.md
- [ ] Universal/IDEAMagic/AvaCode/README.md
- [ ] Universal/IDEAMagic/MagicData/README.md (create new)
- [ ] Universal/IDEAMagic/VOS4/README.md (create new)

#### Developer Manuals
- [ ] IDEAMAGIC-UI-DEVELOPER-MANUAL.md (update references)
- [ ] AI-Module-Porting-Guide.md (Database → MagicData)
- [ ] IPC-Module-Plugin-Data-Exchange-Flow.md (Database → MagicData)
- [ ] PLUGIN-ARCHITECTURE-ANALYSIS-20251106.md (Avanues → AVANUES)

**Estimated files affected**: 20-30 Markdown files

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Breaking builds during rename** | High | High | Use git, test incrementally |
| **Missing import updates** | Medium | High | Use sed for bulk updates, then compile |
| **Documentation out of sync** | Low | Medium | Update docs immediately after code |
| **VOS4 migration complexity** | Medium | Medium | Analyze first, migrate incrementally |
| **Lost functionality** | Low | High | Comprehensive testing after each phase |

### Rollback Strategy

**If migration fails**:
```bash
# 1. Revert to last commit before rename
git reset --hard [commit-before-rename]

# 2. Or use branch strategy
git checkout -b avanues-migration
# Do all work in branch
# Only merge when fully tested
```

**Recommendation**: Use branch for entire migration

---

## Timeline Summary

| Phase | Duration | Dependencies | Deliverables |
|-------|----------|--------------|--------------|
| **Phase 1: Rename** | 4-6 hours | None | AVANUES directory, MagicData module |
| **Phase 2: VOS4** | 8-16 hours | Phase 1 | VOS4 KMP modules integrated |
| **Phase 3: New Components** | 16-24 hours | Phase 2 | IPC, Plugins, CLI (optional) |
| **Phase 4: Documentation** | 4-6 hours | Phase 1-3 | Updated manuals, READMEs |
| **Phase 5: Testing** | 8-12 hours | Phase 1-4 | Verified builds, integration tests |
| **Total** | **40-64 hours** | - | Complete AVANUES platform |

**Recommended Schedule**: 2-3 weeks (with testing and validation)

---

## Final Structure Summary

```
AVANUES Platform
├── AvaUI         # Declarative UI framework (73 files, 9,700 LOC)
├── AvaCode       # Forms & Workflows DSL (Phase 5-6)
├── MagicData       # Database system (renamed from Database)
├── VOS4            # Voice OS components (integrated)
├── Templates       # App generation (Phase 7, in progress)
├── Plugins         # Plugin infrastructure (proposed)
├── IPC             # IPC layer (extracted from MagicData)
└── CLI             # Developer tools (optional)

Apps Built on AVANUES:
├── AVA AI          # Voice assistant
├── AVAConnect      # Connectivity suite
├── BrowserAvanue   # Browser integration
└── [Future apps]   # Healthcare, Education, etc.
```

---

## Decision Points

### Require User Decisions

1. **VOS4 Location**: Where is VOS4 code currently? Should I analyze it?
2. **VOS4 Integration**: KMP module (recommended) or Android-only?
3. **New Components**: Should we add Plugins, IPC, CLI modules now or later?
4. **Timeline**: Immediate migration or phased over 2-3 weeks?
5. **Branch Strategy**: Work in `avanues-migration` branch or direct on main?

### Recommendations

**Immediate (Do Now)**:
1. ✅ Rename Avanues → AVANUES
2. ✅ Rename Database → MagicData
3. ✅ Create branch `avanues-migration` for safety

**Short-term (This Week)**:
4. ✅ Analyze VOS4 location and structure
5. ✅ Create VOS4 KMP module structure
6. ✅ Begin VOS4 migration

**Medium-term (Next 2 Weeks)**:
7. ✅ Complete VOS4 integration
8. ✅ Add IPC module (extract from MagicData)
9. ✅ Add Plugins module (needed for plugin architecture)
10. ✅ Update all documentation

**Long-term (Future)**:
11. 🔵 Add CLI tool (accelerates development)
12. 🔵 Add Validators (quality assurance)

---

## Next Steps

### Immediate Action Required

**User Decision Needed**:
1. Approve this architecture proposal
2. Confirm VOS4 integration strategy
3. Confirm timeline (immediate vs phased)

**Once Approved**:
1. Create `avanues-migration` branch
2. Start Phase 1: Rename operations
3. Analyze VOS4 location
4. Begin implementation per plan

---

**Status**: 🔵 AWAITING USER APPROVAL
**Effort**: 40-64 hours (2-3 weeks with testing)
**Impact**: Complete platform rebranding and consolidation

---

**Document Version**: 1.0.0
**Author**: Claude Code (Sonnet 4.5)
**Date**: 2025-11-06
