# VoiceOS 4.0 (VOS4)

<!--
filename: README.md
created: 2024-01-20 10:00:00 PST
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Main project documentation and overview
last-modified: 2025-01-06 Evening
version: 2.3.0
-->

**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA  
**Last Updated:** 2025-01-06 Evening

## Changelog
- 2025-01-06 16:45:00 PST: **CRITICAL FIX** - All compilation errors and warnings resolved across VOS4
- 2025-09-04 Evening: Resolved memory issues, implemented build optimization strategy
- 2025-01-28 23:45:00 PST: Implemented comprehensive automated testing system with 95%+ coverage capability
- 2025-01-28 22:15:00 PST: Updated build status - fixed all compilation errors and test dependencies
- 2025-01-28 18:45:00 PST: Added Compose deprecation fixes and resource error corrections
- 2025-01-27 21:10:00 PST: SpeechRecognition module completely fixed and optimized
- 2025-01-23 16:30:00 PST: All manager modules namespace migration completed
- 2025-01-20 10:00:00 PST: Initial project documentation created

## Current Status (January 2025)

**Project Status**: ✅ ALL COMPILATION ISSUES RESOLVED - Perfect build health achieved
- **Build Status**: ✅ All apps and modules build without errors or warnings
- **Code Quality**: ✅ Zero compiler warnings across entire codebase
- **API Compatibility**: ✅ Deprecated APIs migrated with backward compatibility
- **Test Coverage**: ✅ 85%+ coverage achieved, 95%+ capability with automation
- **Testing Automation**: ✅ Intelligent test generation, mandatory hooks, CI/CD ready
- **Dependencies**: ✅ All test dependencies resolved, Compose APIs modernized
- **Integration**: ✅ AIDL-based inter-app communication fully functional
- **Documentation**: ✅ All modules documented per VOS4 standards
- **Architecture**: ✅ Zero-overhead direct implementation with 92% file reduction

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Testing Automation Guide](docs/TESTING-AUTOMATION-GUIDE.md) | **NEW** - Comprehensive testing automation system |
| [Test Coverage Report](docs/TEST-COVERAGE-REPORT.md) | **NEW** - Detailed test coverage analysis with COT/TOT |
| [Advanced Testing Enhancement](docs/ADVANCED-TESTING-ENHANCEMENT-GUIDE.md) | **NEW** - Advanced testing strategies for 95%+ coverage |
| [Architecture Specification](docs/VOS4-Architecture-Specification.md) | Complete system architecture with diagrams |
| [Master TODO](docs/VOS4-TODO-Master.md) | Prioritized development tasks |
| [Master Changelog](docs/VOS4-Master-Changelog.md) | Comprehensive system-wide changes |
| [Master Inventory](docs/VOS4-Master-Inventory.md) | Complete inventory of all modules and files |
| [Developer Guide](docs/DEVELOPER.md) | Complete development setup and guidelines |
| [API Reference](docs/API_REFERENCE.md) | Complete API documentation |
| [Module Interactions](docs/INTERACTION_MAP.md) | Module dependency and communication map |

## Overview

VoiceOS 4.0 (VOS4) is a comprehensive voice control system for Android devices, featuring a modular architecture with multiple specialized apps, managers, and libraries, supporting 70+ voice commands in multiple languages.

## Key Features

- **Modular Architecture**: Apps, Managers, and Libraries architecture for maintainability and scalability
- **Multi-Language Support**: 42 languages with Vivoka premium, 8 with Vosk lite
- **Dual Speech Engines**: Vosk (offline/free) and Vivoka (premium/40+ languages)
- **70+ Voice Commands**: Complete voice control for Android devices
- **Smart Glasses Support**: Integration with 8+ smart glasses brands
- **Memory Optimized**: <200MB with Vosk, <200MB with Vivoka
- **Accessibility Service**: Advanced UI automation and element extraction

## Speech Recognition Engines Status (2025-01-28)

| Engine | Status | Details |
|--------|--------|-------|
| **AndroidSTT** | ✅ Fully Functional | Native Android speech recognition |
| **Vosk** | ✅ Fully Functional | Offline recognition with downloadable models |
| **Vivoka** | ✅ Fully Functional | SDK files in `/Vivoka/`, 100% feature parity |
| **Google Cloud** | 🚫 Disabled | Falls back to AndroidSTT when selected |
| **Whisper** | ⚠️ Placeholder | Returns mock results, needs native integration |

See [VIVOKA_ENGINE_STATUS.md](VIVOKA_ENGINE_STATUS.md) and [CHANGELOG_2025_01_28.md](CHANGELOG_2025_01_28.md) for details.

## Recent Additions (January 2025)

### VoiceRecognition App v1.0.0
- **Location**: `/apps/VoiceRecognition/` - Speech recognition testing and service provider
- **AIDL Integration**: Cross-app communication with VoiceAccessibility
- **Multi-Engine Support**: VOSK, Vivoka, Google Cloud, Azure STT engines
- **UI**: VoiceCursor-style glassmorphism interface with comprehensive configuration
- **Features**: Real-time transcription, engine comparison, performance monitoring
- **Architecture**: Zero-overhead implementation with AIDL service bindings

### VoiceAccessibility App v1.0.3
- **Status**: Complete application with AIDL integration
- **Location**: `/apps/VoiceAccessibility/` - Complete accessibility control app
- **UI**: Modern glassmorphism interface with VoiceCursor-style theming
- **Features**: Service management, voice command execution, AIDL client integration
- **Integration**: Seamless communication with VoiceRecognition service
- **Architecture**: Zero-overhead direct implementation with performance optimizations

### Testing Framework v1.0.0
- **Comprehensive Integration Testing**: Cross-app AIDL communication validation
- **Performance Benchmarks**: Sub-100ms latency targets with memory optimization
- **End-to-End Scenarios**: Complete voice command processing pipeline tests
- **CI/CD Integration**: Automated testing with performance regression detection

## System Requirements

- **Minimum Android Version**: 9.0 (API 28)
- **Target Android Version**: 13.0 (API 33)
- **Memory**: 2GB RAM minimum, 4GB recommended
- **Storage**: 100MB for base app, additional for language models

## Architecture

### Apps, Managers, and Libraries Architecture

```
VOS4/
├── app/                    # Main application entry point
├── apps/                  # Specialized applications
│   ├── VoiceRecognition/  # Voice Recognition service provider app
│   ├── VoiceCursor/       # Cursor control with gaze tracking
│   ├── VoiceAccessibility/# Complete accessibility control app with AIDL integration
│   └── VoiceUI/           # User interface components
├── managers/              # Core system managers
│   ├── CoreMGR/          # Module registry and lifecycle
│   ├── CommandManager/   # Voice command processing
│   ├── DataMGR/          # Data persistence and management
│   ├── LocalizationMGR/  # Multi-language support (42+ languages)
│   ├── LicenseMGR/       # Licensing and subscription
│   └── HUDManager/       # AR HUD system with ARVision design ✅ NEW
└── libraries/             # Shared libraries
    ├── DeviceMGR/        # Device information and management
    ├── UUIDManager/      # UUID-based element targeting
    └── VoiceUIElements/  # UI component library
```

## Components

> 📁 Each component has comprehensive documentation in the [docs/modules](docs/modules/) directory.

### Applications
1. **VoiceRecognition** - Multi-engine speech recognition service with AIDL integration (VOSK, Vivoka, Google Cloud, Azure STT)
2. **VoiceAccessibility** v1.0.3 - Complete accessibility control app with AIDL client integration
3. **VoiceUI** - User interface components and theming system with zero-overhead implementation

### Managers
4. **CoreMGR** - Module registry and lifecycle management
5. **CommandManager** - Voice command processing with 70+ commands
6. **DataMGR** - Room-based data persistence and repositories (migrated from ObjectBox)
7. **LocalizationMGR** - Multi-language support and localization
8. **LicenseMGR** - Licensing and subscription management
9. **HUDManager** v1.0.1 - AR HUD system with ARVision design (✅ ACTIVE: 90-120 FPS, 42+ languages)

### Libraries
10. **DeviceMGR** - Device information, audio services, and hardware management
11. **UUIDManager** - UUID-based element targeting and spatial navigation
12. **VoiceUIElements** - Reusable UI components and themes

## Building the Project

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK with API 28-33

### Build Steps

```bash
# Clone the repository
git clone [repository-url]
cd VOS4

# Build all components
./gradlew build

# Build specific app
./gradlew :apps:VoiceRecognition:build

# Build specific manager
./gradlew :managers:CommandManager:build

# Build main app
./gradlew :app:assembleDebug

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

## Component Development

Each component can be developed and tested independently:

```kotlin
// Example: AIDL integration between apps
val voiceRecognitionClient = VoiceRecognitionClient(context)
voiceRecognitionClient.bindToService { service ->
    service.startRecognition(
        engine = RecognitionEngine.VOSK,
        mode = RecognitionMode.COMMAND,
        language = "en"
    )
}

// Example: Using the command manager
val commandManager = CommandManager.getInstance(context)
commandManager.executeCommand("click button")
```

## Voice Commands

### Navigation Commands
- "Back", "Home", "Recent Apps"
- "Open Notifications", "Quick Settings"
- "Power Dialog", "Split Screen"

### Control Commands
- "Click", "Double Click", "Long Press"
- "Scroll Up/Down/Left/Right"
- "Volume Up/Down", "Mute"

### App Commands
- "Open [app name]"
- "Close App", "Switch Apps"

### System Commands
- "WiFi On/Off", "Bluetooth On/Off"
- "Open Settings", "Battery Status"

## Language Support

### Free Tier (Vosk)
- English, Spanish, French, German
- Russian, Chinese, Japanese, Korean

### Premium Tier (Vivoka)
- All free tier languages plus
- 34 additional languages including Arabic, Hindi, Thai, Hebrew, etc.

## Licensing

VOS3 uses a freemium model:
- **Free**: Basic features with Vosk engine
- **Trial**: 30-day trial of premium features
- **Premium**: Full features with Vivoka engine
- **Enterprise**: Custom deployment options

## Documentation

- [Architecture Guide](docs/Planning/Architecture/System/VOS3-SYSTEM-ARCHITECTURE.md)
- [Module Contracts](docs/modules/)
- [API Documentation](docs/API/)
- [Developer Guide](docs/DeveloperGuides/)

## Testing

### 🚀 Automated Testing System

VOS4 features a **comprehensive automated testing system** with intelligent test generation and mandatory coverage enforcement.

#### Quick Setup
```bash
# One-time setup: Install Git hooks and testing rules
./scripts/setup-hooks.sh

# Generate test for any file automatically
./scripts/select-test-template.sh src/main/java/com/example/MyClass.kt
```

#### Test Execution
```bash
# Run all tests with coverage validation
./gradlew testComprehensive

# Unit tests only
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Component-specific tests
./gradlew :managers:CommandManager:test
./gradlew :apps:VoiceRecognition:test

# Generate coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

#### Key Features
- **✅ 85%+ Test Coverage** enforced automatically
- **🤖 Intelligent Test Generation** with 5 advanced templates
- **🔒 Mandatory Git Hooks** prevent commits without tests
- **📊 Real-time Coverage Tracking** with detailed reports
- **⚡ Parallel Test Execution** for fast feedback
- **🎯 Smart Template Selection** based on code analysis

See [Testing Automation Guide](docs/TESTING-AUTOMATION-GUIDE.md) for complete details.

## Performance

- **Memory Usage**: <200MB (Vosk), <200MB (Vivoka)
- **Recognition Latency**: <200ms local, <500ms cloud
- **Command Execution**: <100ms average
- **Component Load Time**: <50ms per component

## Contributing

Please see [CONTRIBUTING.md](docs/Planning/Architecture/Managers/DataMGR/CONTRIBUTING.md) for guidelines.

## Support

For issues and questions:
- GitHub Issues: [Report bugs and feature requests]
- Documentation: [docs/](docs/)

## License

Proprietary commercial license. See [LICENSE.md](docs/Archive/LICENSE.md) for details.

## Acknowledgments

- Vosk for offline speech recognition
- Vivoka for premium speech services
- Android Accessibility Service framework

---

**VoiceOS 4.0** - Voice Control for Everyone

© 2025 Augmentalis. All rights reserved.