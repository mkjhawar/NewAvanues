# VOS4 Project Inventory
**Generated:** 2025-09-04  
**Version:** 4.0.0  
**Copyright:** © Augmentalis Inc | Intelligent Devices LLC  

## Project Overview
- **Total Source Files:** 503 (Kotlin/Java)
- **Total Documentation:** 530 Markdown files
- **Build System:** Gradle 8.11.1 with Kotlin DSL
- **Min SDK:** 28 (Android 9.0)
- **Target SDK:** 33 (Android 13)
- **Compile SDK:** 34 (Android 14)

## Directory Structure

### 📁 Root Level Configuration Files
```
/Volumes/M Drive/Coding/vos4/
├── build.gradle.kts           # Root build configuration
├── settings.gradle.kts         # Project module settings
├── gradle.properties           # Gradle properties and versions
├── local.properties            # Local SDK paths (not in VCS)
├── .gitignore                  # Git ignore rules
├── .gitlab-ci.yml             # GitLab CI/CD pipeline
├── README.md                   # Project documentation
├── .warp.md                   # Warp terminal rules
├── .cursor.md                 # Cursor IDE configuration
└── claude.md                  # Claude AI context
```

### 📱 Application Modules (/apps)
```
apps/
├── VoiceAccessibility/        # Voice accessibility service
│   ├── src/main/java/         # Source code
│   ├── build.gradle.kts       # Module build config
│   └── AndroidManifest.xml    # Android manifest
├── VoiceCursor/               # Voice-controlled cursor
│   ├── src/main/java/         # Source code
│   └── build.gradle.kts       # Module build config
├── VoiceRecognition/          # Speech recognition UI
│   ├── src/main/java/         # Source code
│   └── build.gradle.kts       # Module build config
└── VoiceUI/                   # Voice UI components
    ├── src/main/java/         # Source code
    └── build.gradle.kts       # Module build config
```

### 📚 Library Modules (/libraries)
```
libraries/
├── DeviceManager/             # Device management library
│   ├── src/main/java/         # Source code
│   ├── docs/                   # Module documentation
│   └── build.gradle.kts       # Module build config
├── SpeechRecognition/         # Speech recognition engine
│   ├── src/main/java/         # Core recognition code
│   ├── data/                   # Recognition data models
│   └── build.gradle.kts       # Module build config
├── UUIDManager/               # UUID generation/management
│   ├── src/main/java/         # UUID implementation
│   ├── docs/                   # UUID documentation
│   └── build.gradle.kts       # Module build config
├── VoiceUI/                   # Legacy UI components
│   └── src/                    # Legacy source
└── VoiceUIElements/           # Modern UI elements
    ├── src/main/java/         # UI components
    └── build.gradle.kts       # Module build config
```

### 🛠️ Manager Modules (/managers)
```
managers/
├── CommandManager/            # Voice command processing
│   ├── src/main/java/         # Command implementation
│   └── build.gradle.kts       # Module build config
├── HUDManager/                # Heads-up display
│   ├── src/main/java/         # HUD implementation
│   └── build.gradle.kts       # Module build config
├── LicenseManager/            # License management
│   ├── src/main/java/         # License validation
│   └── build.gradle.kts       # Module build config
├── LocalizationManager/       # Multi-language support
│   ├── src/main/java/         # Localization code
│   ├── res/values-*/          # Language resources
│   └── build.gradle.kts       # Module build config
└── VoiceDataManager/          # ObjectBox data persistence
    ├── src/main/java/
    │   └── .../datamanager/
    │       ├── core/          # ObjectBox initialization
    │       ├── entities/      # 13 ObjectBox entities
    │       └── generated/     # Generated MyObjectBox stub
    └── build.gradle.kts       # KAPT + ObjectBox config
```

### 📖 Documentation (/docs)
```
docs/
├── Analysis/                  # Technical analysis docs
├── api/                       # API documentation
├── architecture/              # System architecture
│   ├── core/                  # Core architecture
│   └── patterns/              # Design patterns
├── Archive/                   # Historical documentation
├── development/               # Development guides
├── diagrams/                  # Architecture diagrams
├── guides/                    # Developer guides
├── Implementation/            # Implementation details
├── Issues/                    # Issue tracking/solutions
│   ├── Fix_MyObjectBox_Stub_20250904.md
│   └── ObjectBox-KAPT-Analysis-2025-01-29.md
├── modules/                   # Module-specific docs
├── Planning/                  # Project planning
├── project-management/        # PM documentation
├── Status/                    # Current status reports
├── technical/                 # Technical specifications
├── TODO/                      # Task tracking
└── DOCUMENTATION-INDEX.md     # Documentation map
```

### 🤖 Agent Instructions (/Agent-Instructions)
```
Agent-Instructions/
├── MASTER-AI-INSTRUCTIONS.md  # AI agent guidelines
├── MASTER-STANDARDS.md         # Coding standards
├── CODING-STANDARDS.md         # Detailed standards
├── CODING-GUIDE.md            # Development guide
└── SESSION-LEARNINGS.md      # Learning records
```

### 🔧 Build & Tools
```
gradle/                        # Gradle wrapper
scripts/                       # Build/utility scripts
tools/                         # Development tools
tests/                         # Test configurations
templates/                     # Code templates
.githooks/                     # Git hooks
.github/                       # GitHub configuration
```

### 🗣️ Speech Engines
```
Vosk/                          # Vosk speech model
vivoka/                        # Vivoka integration
```

## Key Entity Classes (ObjectBox)

### VoiceDataManager Entities
1. **RecognitionLearning** - Speech recognition learning data
2. **LanguageModel** - Language model configurations
3. **TouchGesture** - Touch gesture mappings
4. **GestureLearningData** - Gesture learning patterns
5. **UserSequence** - User interaction sequences
6. **DeviceProfile** - Device configurations
7. **CustomCommand** - Custom voice commands
8. **RetentionSettings** - Data retention policies
9. **ErrorReport** - Error tracking
10. **UserPreference** - User preferences
11. **CommandHistoryEntry** - Command history
12. **AnalyticsSettings** - Analytics configuration
13. **UsageStatistic** - Usage statistics

## Module Dependencies

### Core Dependencies
- **Kotlin:** 1.9.25
- **Compose BOM:** 2024.04.01
- **ObjectBox:** 4.3.1
- **Coroutines:** 1.8.1
- **AndroidX Core:** 1.12.0
- **Hilt:** 2.51.1

### Build Plugins
- `com.android.application` / `com.android.library`
- `org.jetbrains.kotlin.android`
- `kotlin-kapt` (for ObjectBox)
- `io.objectbox` (database plugin)
- `dagger.hilt.android.plugin`
- `com.google.devtools.ksp`

## Project Statistics
- **Total Modules:** 19
- **Application Modules:** 4
- **Library Modules:** 5
- **Manager Modules:** 5
- **Documentation Folders:** 32
- **Active Development:** VOS4 migration in progress

## Critical Files

### Configuration
- `/build.gradle.kts` - Root build configuration
- `/gradle.properties` - Version management
- `/.warp.md` - Warp terminal AI rules

### Documentation
- `/docs/Issues/Fix_MyObjectBox_Stub_20250904.md` - ObjectBox fix guide
- `/docs/DOCUMENTATION-INDEX.md` - Documentation map
- `/Agent-Instructions/MASTER-STANDARDS.md` - Coding standards

### Core Implementation
- `/managers/VoiceDataManager/src/.../core/ObjectBox.kt` - DB initialization
- `/libraries/SpeechRecognition/src/.../SpeechRecognitionEngine.kt` - Speech engine
- `/managers/CommandManager/src/.../CommandProcessor.kt` - Command processing

## Build Commands
```bash
# Clean build
./gradlew clean build

# Module-specific build
./gradlew :managers:VoiceDataManager:build

# KAPT processing
./gradlew :managers:VoiceDataManager:kaptDebugKotlin

# Run tests
./gradlew test

# Generate APK
./gradlew assembleDebug
```

## Notes
- Project uses KAPT for ObjectBox annotation processing
- Voice-first architecture with SDK-ready modules
- SOLID principles applied throughout
- Multi-module architecture for separation of concerns
- Extensive documentation with 530+ markdown files

---
*Last Updated: 2025-09-04*
