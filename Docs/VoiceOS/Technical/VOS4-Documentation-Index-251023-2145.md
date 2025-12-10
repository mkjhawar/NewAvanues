# VOS4 Documentation Index

**Project:** VoiceOS (VOS4) - Voice-Controlled Android Accessibility Framework
**Last Updated:** 2025-10-23 21:45:35 PDT
**Version:** 4.0
**Status:** Active Development

---

## Quick Navigation

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Module Documentation](#module-documentation)
- [Quick Start Guides](#quick-start-guides)
- [Documentation Organization](#documentation-organization)
- [Module Dependencies](#module-dependencies)
- [Additional Resources](#additional-resources)

---

## Overview

VoiceOS (VOS4) is a comprehensive voice-controlled Android accessibility framework that empowers users to control their Android devices entirely through voice commands. Built on Android's AccessibilityService architecture, VOS4 provides advanced screen scraping, AI-powered context inference, voice recognition, and intelligent command processing.

VOS4 leverages cutting-edge technology including on-device speech recognition (Whisper.cpp), real-time UI element analysis, hash-based deduplication, and semantic understanding to create a seamless voice-first mobile experience. The system supports multi-app accessibility, learns from user interactions through LearnApp, and provides developers with powerful tools to extend voice control capabilities to any Android application.

**Key Capabilities:**
- **Voice-First Control**: Complete device control through natural language commands
- **Accessibility Scraping**: Real-time UI element capture with hash-based deduplication
- **AI Context Inference**: Screen-level understanding, form detection, and relationship modeling
- **On-Device Speech Recognition**: Privacy-focused Whisper.cpp integration
- **Smart Command Processing**: Context-aware command routing and execution
- **Developer Tools**: LearnApp for voice command creation and testing
- **Extensible Architecture**: Modular design supporting 20 specialized modules

---

## System Architecture

### High-Level System Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          VOS4 System Architecture                    │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  USER INTERACTION LAYER                                              │
├─────────────────────────────────────────────────────────────────────┤
│  Voice Input  →  Speech Recognition  →  Command Processing           │
│                                                                       │
│  ┌──────────┐      ┌──────────────┐      ┌──────────────┐          │
│  │ Microphone│  →  │ SpeechRecog  │  →   │ CommandMgr   │          │
│  │ VoiceUI   │      │ (Whisper.cpp)│      │              │          │
│  └──────────┘      └──────────────┘      └──────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  CORE SERVICE LAYER                                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  VoiceOSCore (Main AccessibilityService)                     │   │
│  │  ┌───────────────┐  ┌───────────────┐  ┌──────────────┐    │   │
│  │  │ Accessibility │  │ AI Context    │  │ Scraping DB  │    │   │
│  │  │ Scraping      │  │ Inference     │  │ (Room v7)    │    │   │
│  │  └───────────────┘  └───────────────┘  └──────────────┘    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ VoiceCursor  │  │ HUDManager   │  │ DeviceManager│             │
│  │ (Navigation) │  │ (Overlay)    │  │ (Hardware)   │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  DATA & PERSISTENCE LAYER                                            │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ VoiceDataMgr │  │ UUIDCreator  │  │ Localization │             │
│  │ (ObjectBox)  │  │ (Persistent) │  │ Manager      │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  DEVELOPER TOOLS                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ LearnApp     │  │ MagicUI      │  │ VoiceKeyboard│             │
│  │ (Dev Tool)   │  │ (UI Builder) │  │              │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘

Legend:
  → : Data Flow
  │ : Module Boundary
  ┌─┐: Component/Module
```

---

## Module Documentation

### Core Application Modules

#### **VoiceOSCore** - Main Accessibility Service

The heart of VOS4, providing accessibility scraping, AI context inference, and core voice control functionality.

**Documentation:**
- [Module Overview & README](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/README.md)
- [Developer Manual](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/developer-manual/)
- [User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/user-manual/)
- [Scraping Database Developer Manual](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/scraping-database-developer-manual-251023-2052.md)
- [Architecture Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/architecture/)
- [API Reference](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/reference/api/)
  - [Overlay API Reference](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/reference/api/Overlay-API-Reference-251009-0403.md)
- [Changelog](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/changelog/)
- [Integration Documentation Index](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/INTEGRATION-DOCUMENTATION-INDEX-251015-1914.md)

**Key Features:**
- Accessibility Service implementation
- UI element scraping with hash-based deduplication (MD5)
- AI-powered context inference (screen types, forms, relationships)
- Database v7 with comprehensive migration support
- Screen transition tracking and user flow analysis
- Form field grouping and relationship modeling

---

#### **LearnApp** - Developer Training Tool

Interactive developer tool for creating, testing, and debugging voice commands within VOS4.

**Documentation:**
- [Developer Manual](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/developer-manual.md)
- [User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/user-manual.md)
- [LearnApp Developer Guide (Comprehensive)](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/LEARNAPP-DEVELOPER-GUIDE-251015-1914.md)
- [LearnApp Roadmap](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/LEARNAPP-ROADMAP-251015-1914.md)
- [VOS4 Integration Guide](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/VOS4-INTEGRATION-GUIDE-251015-1914.md)
- [Android Accessibility Research](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/ANDROID-ACCESSIBILITY-RESEARCH-251015-1914.md)
- [Architecture](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/architecture/)
- [Changelog](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/changelog/)
- [Status Reports](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/status/)

**Key Features:**
- Voice command creation and testing
- Real-time accessibility tree visualization
- Command debugging and validation
- Integration with VoiceOSCore scraping database
- Developer-focused UI with command inspector

---

#### **CommandManager** - Voice Command Processing

Intelligent command routing, parsing, and execution engine for VOS4 voice commands.

**Documentation:**
- [Module README](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/README.md)
- [Developer Manual](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/developer-manual.md)
- [User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/user-manual.md)
- [Database Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/database/)
- [Implementation Guides](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/implementation/)
- [Changelog](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/changelog/)
- [Reference](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/reference/)

**Key Features:**
- Context-aware command parsing
- Command routing and execution
- UUID-based command persistence
- Flow-based reactive queries
- Database integration with Room/KSP
- Comprehensive test coverage

---

#### **SpeechRecognition** - Voice Input Processing

On-device speech recognition using Whisper.cpp for privacy-focused voice input.

**Documentation:**
- [Module README](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/README.md)
- [Developer Manual](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/developer-manual.md)
- [User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/user-manual.md)
- [Architecture](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/architecture/)
- [Implementation](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/implementation/)
- [Reference](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/reference/)
- [Changelog](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/changelog/)

**Key Features:**
- Whisper.cpp integration for on-device recognition
- Privacy-focused (no cloud dependencies)
- Real-time audio processing
- Multi-language support
- Low-latency recognition
- Optimized for Android

---

#### **VoiceCursor** - Navigation Control

Advanced cursor and navigation control for hands-free device interaction.

**Documentation:**
- [Module README](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/README.md)
- [Developer Reference](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/developer-manual/)
- [Architecture](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/architecture/)
- [Reference](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/reference/)
- [Diagrams](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/diagrams/)
- [Changelog](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/changelog/)
- [Status Reports](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/status/)
- [IMU Issue Analysis](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/VoiceCursor-IMU-Issue-Complete-Analysis-251017-0605.md)

**Key Features:**
- Voice-controlled cursor movement
- Gesture-based navigation
- Click and tap actions
- Scroll control
- Element selection
- Hands-free interaction

---

### Supporting Library Modules

#### **VoiceUI** - Voice Interface Components
User interface components and overlays for voice interaction feedback.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceUI/)

---

#### **MagicUI** - UI Builder & DSL
Dynamic UI creation framework with declarative DSL for building VOS4 interfaces.

**Documentation:**
- [Module README](/Volumes/M Drive/Coding/vos4/docs/modules/MagicUI/README.md)
- [MagicUI Specification](/Volumes/M Drive/Coding/vos4/docs/modules/MagicUI/MagicUI-Specification-UI-Creator-251019-0118.md)
- [Documentation Context](/Volumes/M Drive/Coding/vos4/docs/modules/MagicUI/MagicUI-Documentation-Context-251019-0127.md)
- [AI Agent Implementation Instructions](/Volumes/M Drive/Coding/vos4/docs/modules/MagicUI/AI-AGENT-IMPLEMENTATION-INSTRUCTIONS-251015-1914.md)
- [Architecture](/Volumes/M Drive/Coding/vos4/docs/modules/MagicUI/architecture/)
  - [Master Implementation Guide](/Volumes/M Drive/Coding/vos4/docs/modules/MagicUI/architecture/00-MASTER-IMPLEMENTATION-GUIDE-251015-1914.md)
- [Status Reports](/Volumes/M Drive/Coding/vos4/docs/modules/MagicUI/status/)

---

#### **VoiceKeyboard** - Voice-Controlled Input
Voice-driven keyboard replacement for text input.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceKeyboard/)

---

#### **VoiceUIElements** - UI Component Library
Reusable UI elements optimized for voice interaction.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceUIElements/)

---

#### **UUIDCreator** - Persistent Identification
UUID generation and management for persistent element tracking.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/UUIDCreator/)

---

#### **Translation** - Internationalization
Multi-language support and translation services.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/Translation/)

---

#### **VoiceOsLogger** - Logging Framework
Centralized logging and diagnostics for VOS4.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOsLogger/)

---

#### **MagicElements** - Advanced UI Components
Specialized UI components for advanced voice interactions.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/MagicElements/)

---

### Manager Modules

#### **HUDManager** - Heads-Up Display
Overlay management for persistent visual feedback.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/HUDManager/)

---

#### **DeviceManager** - Hardware Integration
Device hardware access and management (sensors, buttons, etc.).

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/DeviceManager/)
- [Changelog](/Volumes/M Drive/Coding/vos4/docs/modules/DeviceManager/changelog/)

---

#### **VoiceDataManager** - Data Persistence
ObjectBox-based data persistence layer for voice commands and user data.

**Documentation:**
- [Module Documentation](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceDataManager/)

---

#### **LocalizationManager** - Locale Support
Locale and language management for multi-region support.

**Documentation:**
- [Module README](/Volumes/M Drive/Coding/vos4/docs/modules/LocalizationManager/README.md)
- [Enhancement Summary](/Volumes/M Drive/Coding/vos4/docs/modules/LocalizationManager/LOCALIZATION-ENHANCEMENTS-SUMMARY-2025-09-06.md)

---

#### **LicenseManager** - License & Authentication
License validation and authentication services.

**Documentation:**
- [Module README](/Volumes/M Drive/Coding/vos4/docs/modules/LicenseManager/README.md)

---

#### **VoiceRecognition** - Recognition Engine Integration
Additional voice recognition capabilities and engine integration.

**Documentation:**
- [Module README](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceRecognition/README.md)

---

## Quick Start Guides

### For Developers: Getting Started with VOS4 Development

**Prerequisites:**
- Android Studio Arctic Fox or later
- Android SDK 28+ (target SDK 34)
- Kotlin 1.9.0+
- Gradle 8.0+
- Git for version control

**5-Step Quick Start:**

**1. Clone and Setup**
```bash
cd "/Volumes/M Drive/Coding/vos4"
./gradlew build
```

**2. Review Core Documentation**
- Read [VoiceOSCore README](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/README.md)
- Review [VOS4 Coding Standards](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Coding-Standards.md)
- Check [Architecture Overview](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/architecture/)

**3. Understand Module Structure**
- Review [Module Dependencies](#module-dependencies) diagram below
- Explore module documentation in `/docs/modules/[ModuleName]/`
- Check naming conventions: [NAMING-CONVENTIONS.md](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/standards/NAMING-CONVENTIONS.md)

**4. Set Up Development Environment**
- Install LearnApp on test device: `./gradlew :modules:apps:LearnApp:installDebug`
- Enable accessibility permissions for VoiceOSCore
- Review [LearnApp Developer Guide](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/LEARNAPP-DEVELOPER-GUIDE-251015-1914.md)

**5. Start Contributing**
- Check [Project TODO List](/Volumes/M Drive/Coding/vos4/coding/TODO/)
- Review [Pre-Implementation Q&A Protocol](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Pre-Implementation-QA.md)
- Follow [VOS4 Commit Protocol](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Commit.md)

**Additional Resources:**
- [IDEADEV Framework](/Volumes/M Drive/Coding/vos4/docs/Active/How-To-Use-IDEADEV-Framework-VOS4-251018-1906.md) for complex features
- [VOS4 Agent Deployment](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Agent-Deployment.md)
- [VOS4 Documentation Protocol](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Documentation.md)

---

### For Users: Getting Started with VoiceOS

**Prerequisites:**
- Android device running Android 9.0 (API 28) or higher
- Microphone permissions
- ~200MB storage space
- Accessibility service permissions

**5-Step Quick Start:**

**1. Installation**
- Download VoiceOS APK from release page
- Install on your Android device
- Grant installation permissions

**2. Enable Accessibility Service**
- Open Android Settings → Accessibility
- Find "VoiceOS" in service list
- Toggle ON and accept permissions
- Confirm accessibility access

**3. Configure Voice Input**
- Open VoiceOS app
- Follow setup wizard
- Grant microphone permissions
- Test voice recognition (say "Hello VoiceOS")

**4. Learn Basic Commands**
- Say "Help" to hear available commands
- Try basic navigation: "Go home", "Go back", "Open [app name]"
- Practice scrolling: "Scroll down", "Scroll up"
- Test clicking: "Click [element name]"

**5. Explore Advanced Features**
- Review [User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/user-manual/)
- Learn about [Voice Cursor](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceCursor/)
- Configure custom commands
- Enable LearnApp mode for command creation

**User Resources:**
- [VoiceOSCore User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/VoiceOSCore/user-manual/)
- [LearnApp User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/LearnApp/user-manual.md)
- [CommandManager User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/CommandManager/user-manual.md)
- [SpeechRecognition User Manual](/Volumes/M Drive/Coding/vos4/docs/modules/SpeechRecognition/user-manual.md)

---

## Documentation Organization

### Documentation Directory Structure

VOS4 follows a strict documentation organization model based on the master template defined in `/Volumes/M Drive/Coding/Docs/agents/instructions/Guide-Documentation-Structure.md`.

**Key Principles:**
- ✅ **CODE**: `/modules/` contains `.kt/.java/.xml` files ONLY
- ✅ **DOCS**: `/docs/` contains ALL documentation, tracking, and task files
- ❌ **NO MIXING**: Never mix code and documentation in the same folder

### Directory Layout

```
/vos4/docs/
├── master/                        # Project-wide tracking
│   ├── changelogs/               # Master changelog
│   ├── status/                   # Project status reports
│   ├── tasks/                    # TODO lists and backlogs
│   └── inventories/              # Resource inventories
│
├── planning/                      # Planning & architecture
│   ├── project/                  # Requirements, roadmap
│   ├── architecture/             # System architecture
│   │   └── decisions/           # ADRs (Architecture Decision Records)
│   ├── implementation/           # Implementation plans
│   └── features/                 # Feature specifications
│
├── modules/                       # Module-specific docs (20 modules)
│   ├── VoiceOSCore/              # PascalCase folder names
│   │   ├── architecture/
│   │   ├── changelog/
│   │   ├── developer-manual/
│   │   ├── user-manual/
│   │   ├── reference/
│   │   ├── status/
│   │   └── README.md
│   ├── LearnApp/
│   ├── CommandManager/
│   └── ... (17 more modules)
│
├── voiceos-master/                # System-wide documentation
│   ├── architecture/             # System architecture
│   ├── guides/                   # User and developer guides
│   ├── reference/                # Technical references
│   ├── standards/                # Coding and naming standards
│   └── testing/                  # Test documentation
│
├── visuals/                       # Visual documentation
│   ├── system/                   # System diagrams
│   ├── sequences/                # Sequence diagrams
│   └── technical/                # Technical diagrams
│
├── templates/                     # Templates & standards
│   ├── document-templates/
│   └── standards/
│       └── NAMING-CONVENTIONS.md
│
├── commits/                       # Commit documentation
│   ├── current/                  # Active commit reviews
│   └── archives/                 # Historical commits
│
├── scripts/                       # Automation scripts
│
├── ProjectInstructions/           # VOS4-specific protocols
│   ├── Protocol-VOS4-Coding-Standards.md
│   ├── Protocol-VOS4-Documentation.md
│   ├── Protocol-VOS4-Commit.md
│   └── Protocol-VOS4-Agent-Deployment.md
│
├── Active/                        # Current work snapshots
│
├── Archive/                       # Deprecated documentation
│
└── documentation-control/         # Documentation management
```

### Naming Conventions

**Documentation Files:**
```
PascalCase-With-Hyphens-YYMMDD-HHMM.md

Examples:
- VoiceOS-Status-Report-251023-2145.md
- DatabaseManager-Implementation-Guide-251017-0508.md
- Feature-Implementation-Complete-251015-0912.md
```

**Module Folders (PascalCase):**
```
/docs/modules/VoiceOSCore/
/docs/modules/CommandManager/
/docs/modules/LearnApp/
/docs/modules/SpeechRecognition/
```

**System Folders (kebab-case):**
```
/docs/voiceos-master/
/docs/planning/
/docs/commits/
```

**Timestamp Format:**
```bash
# Generate timestamp
date "+%y%m%d-%H%M"

# Example: 251023-2145
# Year: 25 (2025)
# Month: 10 (October)
# Day: 23
# Hour: 21 (9 PM)
# Minute: 45
```

**Reference:** [NAMING-CONVENTIONS.md](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/standards/NAMING-CONVENTIONS.md)

---

## Module Dependencies

### VOS4 Module Dependency Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                     VOS4 Module Dependency Graph                     │
└─────────────────────────────────────────────────────────────────────┘

                    ┌──────────────────┐
                    │   VoiceOSCore    │ (Main Service)
                    │  (Accessibility) │
                    └────────┬─────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ CommandMgr   │    │ VoiceCursor  │    │ HUDManager   │
│              │    │              │    │              │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       │                   └───────┬───────────┘
       │                           │
       ▼                           ▼
┌──────────────┐          ┌──────────────┐
│ SpeechRecog  │          │ DeviceManager│
│ (Whisper.cpp)│          │              │
└──────┬───────┘          └──────────────┘
       │
       │                   ┌──────────────┐
       └──────────────────▶│   VoiceUI    │
                           │              │
                           └──────┬───────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
              ▼                   ▼                   ▼
     ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
     │ VoiceUIElems │    │   MagicUI    │    │ MagicElements│
     │              │    │   (Builder)  │    │              │
     └──────────────┘    └──────────────┘    └──────────────┘


    ┌─────────────────────────────────────────────────────────┐
    │               DATA & PERSISTENCE LAYER                   │
    ├─────────────────────────────────────────────────────────┤
    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
    │  │VoiceDataMgr  │  │ UUIDCreator  │  │ Translation  │ │
    │  │ (ObjectBox)  │  │              │  │              │ │
    │  └──────────────┘  └──────────────┘  └──────────────┘ │
    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
    │  │Localization  │  │ LicenseMgr   │  │VoiceOsLogger │ │
    │  │   Manager    │  │              │  │              │ │
    │  └──────────────┘  └──────────────┘  └──────────────┘ │
    └─────────────────────────────────────────────────────────┘


    ┌─────────────────────────────────────────────────────────┐
    │               DEVELOPER TOOLS (Standalone)               │
    ├─────────────────────────────────────────────────────────┤
    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
    │  │  LearnApp    │  │VoiceKeyboard │  │VoiceRecognit.│ │
    │  │  (Dev Tool)  │  │              │  │  (Engine)    │ │
    │  └──────────────┘  └──────────────┘  └──────────────┘ │
    └─────────────────────────────────────────────────────────┘


Legend:
  ┌─┐ : Module
  │  : Dependency (uses/depends on)
  ▼  : Direction of dependency

Dependency Types:
  - Core Dependencies: Required for basic functionality
  - Optional Dependencies: Enhanced features
  - Data Layer: Shared across all modules
  - Developer Tools: Independent, can run standalone
```

### Module Categories

**Core Application Modules (5):**
1. VoiceOSCore - Main accessibility service
2. VoiceCursor - Navigation control
3. VoiceUI - UI framework
4. VoiceRecognition - Recognition engine
5. LearnApp - Developer tool

**Library Modules (9):**
6. DeviceManager - Hardware integration
7. MagicElements - Advanced UI components
8. MagicUI - UI builder & DSL
9. SpeechRecognition - Speech processing
10. Translation - Internationalization
11. UUIDCreator - Persistent IDs
12. VoiceKeyboard - Voice input
13. VoiceOsLogger - Logging framework
14. VoiceUIElements - UI components

**Manager Modules (5):**
15. CommandManager - Command processing
16. HUDManager - Overlay management
17. LicenseManager - Licensing
18. LocalizationManager - Locale support
19. VoiceDataManager - Data persistence

---

## Additional Resources

### Project Management & Tracking

**Current Work:**
- [Project TODO Master](/Volumes/M Drive/Coding/vos4/coding/TODO/)
- [Project Status Reports](/Volumes/M Drive/Coding/vos4/coding/STATUS/)
- [Active Work Snapshots](/Volumes/M Drive/Coding/vos4/docs/Active/)

**Planning & Architecture:**
- [Architecture Documentation](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/architecture/)
- [Implementation Plans](/Volumes/M Drive/Coding/vos4/docs/planning/implementation/)
- [Feature Specifications](/Volumes/M Drive/Coding/vos4/docs/planning/features/)
- [Architecture Decision Records (ADRs)](/Volumes/M Drive/Coding/vos4/docs/planning/architecture/decisions/)

### VOS4-Specific Protocols

**Development Protocols:**
- [VOS4 Coding Standards](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Coding-Standards.md)
- [VOS4 Documentation Protocol](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Documentation.md)
- [VOS4 Commit Protocol](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Commit.md)
- [VOS4 Agent Deployment](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Agent-Deployment.md)
- [VOS4 Pre-Implementation Q&A](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/Protocol-VOS4-Pre-Implementation-QA.md)

### Universal Development Standards

**General Protocols (apply to all projects):**
- [Master CLAUDE.md](/Volumes/M Drive/Coding/Docs/agents/claude/CLAUDE.md)
- [Coding Standards](/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Coding-Standards.md)
- [Documentation Protocol](/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Documentation.md)
- [Commit Protocol](/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Commit.md)
- [IDEADEV Framework](/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-IDEADEV-Universal-Framework.md)
- [Precompaction Protocol](/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Precompaction.md)
- [Zero Tolerance Policies](/Volumes/M Drive/Coding/Docs/agents/instructions/Reference-Zero-Tolerance-Policies.md)

### Testing & Quality

**Testing Documentation:**
- [VoiceOS Testing](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/testing/)
- [Module Test Docs](/Volumes/M Drive/Coding/vos4/docs/modules/[ModuleName]/testing/)

### Technical References

**System-Wide References:**
- [Naming Conventions](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/standards/NAMING-CONVENTIONS.md)
- [Technical References](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/reference/)
- [Developer Guides](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/guides/)
- [API Documentation](/Volumes/M Drive/Coding/vos4/docs/voiceos-master/reference/)

### Visual Documentation

**Diagrams & Visuals:**
- [System Diagrams](/Volumes/M Drive/Coding/vos4/docs/visuals/system/)
- [Sequence Diagrams](/Volumes/M Drive/Coding/vos4/docs/visuals/sequences/)
- [Technical Diagrams](/Volumes/M Drive/Coding/vos4/docs/visuals/technical/)
- [Module-Specific Diagrams](/Volumes/M Drive/Coding/vos4/docs/modules/[ModuleName]/diagrams/)

### Changelog & Status

**Version History:**
- [Master Changelog](/Volumes/M Drive/Coding/vos4/docs/master/changelogs/CHANGELOG-MASTER.md)
- [Current Changelog](/Volumes/M Drive/Coding/vos4/docs/master/changelogs/CHANGELOG-CURRENT.md)
- [Module Changelogs](/Volumes/M Drive/Coding/vos4/docs/modules/[ModuleName]/changelog/)

**Project Status:**
- [Project Status Current](/Volumes/M Drive/Coding/vos4/docs/master/status/PROJECT-STATUS-CURRENT.md)
- [Project Status Summary](/Volumes/M Drive/Coding/vos4/docs/master/status/PROJECT-STATUS-SUMMARY.md)
- [Module Status Reports](/Volumes/M Drive/Coding/vos4/docs/modules/[ModuleName]/status/)

---

## Contributing to Documentation

### Documentation Standards

When creating or updating VOS4 documentation:

**1. Generate Timestamp:**
```bash
date "+%y%m%d-%H%M"
```

**2. Follow Naming Convention:**
```
PascalCase-With-Hyphens-YYMMDD-HHMM.md
```

**3. Location Guidelines:**
- **Module-specific docs** → `/docs/modules/[ModuleName]/`
- **System-wide docs** → `/docs/voiceos-master/`
- **Current work** → `/docs/Active/`
- **Deprecated** → `/docs/Archive/`

**4. Required Sections:**
- Quick Links (to related docs)
- Timestamp and status
- Clear headings and navigation
- Cross-references to related documentation

**5. Update Tracking Files:**
- Module changelog
- Project status reports
- TODO lists (if applicable)

### Documentation Templates

All documentation should follow the master template structure:
- [Guide-Documentation-Structure.md](/Volumes/M Drive/Coding/Docs/agents/instructions/Guide-Documentation-Structure.md)

---

## Support & Contact

**Project Location:** `/Volumes/M Drive/Coding/vos4`
**Documentation Root:** `/Volumes/M Drive/Coding/vos4/docs/`
**Quick Reference:** [CLAUDE.md](/Volumes/M Drive/Coding/vos4/CLAUDE.md)

**For Issues or Questions:**
- Check module-specific README files
- Review [Active Work](/Volumes/M Drive/Coding/vos4/docs/Active/)
- Consult [VOS4 Protocols](/Volumes/M Drive/Coding/vos4/Docs/ProjectInstructions/)

---

**Document Version:** 1.0
**Created:** 2025-10-23 21:45:35 PDT
**Last Updated:** 2025-10-23 21:45:35 PDT
**Timestamp:** 251023-2145
**Status:** Active - Master Documentation Index

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
