# Project Context

**Mandatory AI Reference** - Read this before any implementation

## Project Overview
- **Name:** Avanues Ecosystem
- **Purpose:** Kotlin Multiplatform ecosystem providing voice-first accessibility and platform capabilities
- **Target Users:**
  - End users requiring voice accessibility (VoiceOS)
  - Developers building voice-enabled apps (Avanue Platform)
- **Platforms:** Android, iOS, macOS, Windows

## Architecture

### Two-Tier System

1. **VoiceOS** (Brand - Accessibility Service)
   - Package: `com.augmentalis.voiceos`
   - Free standalone app
   - Core accessibility features
   - Prompts users to install Avanue apps for full functionality

2. **Avanue Platform** (Feature Apps)
   - **Avanues Core**: `com.augmentalis.avanue.core` (FREE)
   - **AIAvanue**: `com.augmentalis.avanue.ai` ($9.99)
   - **BrowserAvanue**: `com.augmentalis.avanue.browser` ($4.99)
   - **NoteAvanue**: `com.augmentalis.avanue.notes` (FREE/$2.99)

### Directory Structure

```
Avanues/
├── avanues/                    # Platform Code (shared infrastructure)
│   ├── core/                   # Core systems (AvaUI, AvaCode, etc.)
│   └── libraries/              # Reusable libraries (components, speech, etc.)
└── apps/                       # Standalone Applications
    ├── voiceos/                # VoiceOS accessibility service
    ├── avanues-app/        # Avanues core platform
    ├── aiavanue/               # AI capabilities
    ├── browseravanue/          # Voice browser
    └── noteavanue/             # Voice notes
```

## Key Components

### Platform Infrastructure (avanues/)

1. **AvaUI** - UI component runtime system
   - Cross-platform UI abstraction layer
   - Android Compose, iOS SwiftUI, Desktop renderers
   - Theme management and state management

2. **AvaCode** - DSL generator
   - Declarative UI DSL
   - Code generation to platform-native code
   - App Store compliant (interpreted as data)

3. **AvaElements** - UI component library
   - 13 Phase 1 components (complete)
   - 35 Phase 3 advanced components (planned)
   - Platform-specific renderers

4. **VoiceOSBridge** - IPC system
   - VoiceOS ↔ Avanue app communication
   - Capability discovery
   - Android: Intents/AIDL, iOS: URL schemes

5. **Platform Libraries**
   - Speech recognition
   - Voice keyboard
   - Device management
   - Translation/i18n
   - Logging & preferences

### Applications (apps/)

Each app is independently deployable with shared infrastructure dependencies.

## Dependencies

### Core Technologies
- **Kotlin Multiplatform** (KMP) - v1.9.25
- **Jetpack Compose** - v1.7.1 (Android)
- **Compose Multiplatform** - Desktop (macOS/Windows)
- **SwiftUI** - iOS (via Kotlin/Native)
- **Gradle** - v8.10+ (composite builds)

### Major Libraries
- Kotlin Coroutines & Flow
- Kotlinx Serialization
- SQLDelight (database)
- Ktor (networking)

## Constraints

### Technical Constraints
1. **App Store Compliance**: No dynamic code loading (DSL interpreted as data)
2. **Platform Native**: Must use native UI (Compose/SwiftUI) for each platform
3. **IPC Only**: Apps communicate via standard IPC mechanisms
4. **KMP Structure**: All shared code in `commonMain`, platform code in `androidMain/iosMain/jvmMain`

### Business Constraints
1. **Free Tier**: VoiceOS and Avanues Core must remain free
2. **Independent Deployment**: Each app versioned and released independently
3. **Privacy First**: No telemetry without explicit opt-in

### Quality Constraints
1. **Test Coverage**: 80%+ for all modules
2. **Documentation**: KDoc for all public APIs
3. **Null Safety**: Zero tolerance for !! operator in production
4. **Architecture**: Modular, platform-aware, independently deployable

## Development Workflow

### IDEACODE 5.0 Framework
- Location: `/Volumes/M Drive/Coding/ideacode/`
- Profile: `android-app` (multi-platform)
- Version: 5.0

### Key Principles
- Ownership-based organization (avanues/ vs apps/)
- Platform-aware structure (android/ios/macos/windows/shared)
- Documentation before commits
- No AI attribution in git commits
- Explicit file staging only

## Current Status

### Phase 1: Complete ✅
- 13 core AvaElements components
- Android Compose renderer
- State management with Flow
- Universal Theme Manager
- AvaCode DSL generator

### Phase 2: 50% Complete 🔄
- Asset Manager (30%)
- Theme Builder UI (20%)
- Database system (planned)

### Phase 3: Planned ⏳
- 35 advanced UI components
- iOS SwiftUI renderer
- Desktop renderers
- Full IPC system

---

**Last Updated:** 2025-11-09
**IDEACODE Version:** 5.0
**Created by Manoj Jhawar, manoj@ideahq.net**
