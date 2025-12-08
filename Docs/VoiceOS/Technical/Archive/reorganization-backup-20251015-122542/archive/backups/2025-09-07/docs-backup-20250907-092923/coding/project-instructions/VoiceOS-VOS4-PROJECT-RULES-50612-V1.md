# VOS4 Project-Specific Rules

**Project:** VOS4 - Voice Operating System v4  
**Created:** 2025-01-30  
**Version:** 1.0.0  
**Master Rules:** `/Volumes/M Drive/Coding/Warp/Agent-Instructions/`  

## 📍 VOS4-Specific Context

This document contains rules and guidelines specific to VOS4 that extend (but never contradict) the master rules.

## 🏗️ VOS4 Architecture

### Module Structure
```
/VOS4/
├── app/                    # Main application (com.augmentalis.voiceos)
├── apps/                   # Standalone applications
│   ├── SpeechRecognition/  # 6 engines implemented
│   ├── VoiceAccessibility/ # Direct command execution
│   └── VoiceUI/           # Overlay system
├── managers/              # System managers
│   ├── CommandsManager/   # 70+ commands
│   └── DataManager/       # ObjectBox integration
├── libraries/             # Shared libraries
│   └── UUIDCreator/       # UUID targeting system
└── services/             # Core services
```

### Namespace Convention
```kotlin
// VOS4 now uses com.augmentalis.* pattern
package com.augmentalis.voiceos      // Main app
package com.augmentalis.speechrecognition
package com.augmentalis.commandsmanager
package com.augmentalis.datamanager

// OLD pattern (DEPRECATED - being migrated)
package com.ai.*  // NO LONGER USED
```

## 📋 VOS4-Specific Standards

### Speech Recognition Engines
VOS4 has 6 implemented engines:
1. Vosk (offline, <30MB)
2. Vivoka (commercial, <60MB)
3. Google (cloud-based)
4. Sphinx (offline)
5. Mozilla DeepSpeech
6. Custom hybrid engine

### Command Processing
```kotlin
// VOS4 uses direct command mapping
class CommandsManager {
    // 70+ implemented commands
    private val commands = mapOf(
        "open browser" to OpenBrowserCommand(),
        "take screenshot" to ScreenshotCommand(),
        // ... 68+ more
    )
}
```

### UUID System
```kotlin
// VOS4's unique UUID targeting system
class UUIDCreator {
    // Assigns UUIDs to UI elements for voice targeting
    fun assignUUID(node: AccessibilityNodeInfo): String
    fun targetByUUID(uuid: String): AccessibilityNodeInfo?
}
```

## 🔄 Migration Status

### Completed Migrations
- ✅ SpeechRecognition app (6 engines)
- ✅ CommandsManager (70+ commands)
- ✅ DataManager (ObjectBox)
- ✅ Namespace migration to com.augmentalis.*

### In Progress
- 🟡 VoiceUI consolidation
- 🟡 Accessibility service optimization

### Pending
- ⭕ Legacy code cleanup
- ⭕ Performance optimization pass

## 📊 VOS4 Performance Targets

| Component | Target | Current | Status |
|-----------|--------|---------|--------|
| App Startup | <1s | 1.2s | 🟡 |
| Speech Init | <500ms | 450ms | ✅ |
| Command Exec | <100ms | 95ms | ✅ |
| Memory (Vosk) | <30MB | 28MB | ✅ |
| Memory (Vivoka) | <60MB | 55MB | ✅ |

## 🚨 VOS4-Specific Gotchas

### Known Issues
1. **Gradle Pipes** - Never pipe gradle commands (causes "Task '2' not found")
2. **Module Dependencies** - Some modules still have circular dependencies
3. **Resource IDs** - Avoid R.id conflicts between modules

### Workarounds
```kotlin
// Gradle - use individual commands
./gradlew clean
./gradlew build

// NOT: ./gradlew clean | build
```

## 📝 VOS4 Documentation Requirements

### Module Documentation
Each VOS4 module requires:
```
/[module]/docs/
├── [Module]-Architecture.md
├── [Module]-API-Reference.md
├── [Module]-Developer-Manual.md
├── [Module]-Changelog.md
├── [Module]-Implementation-Status.md
└── [Module]-Master-Inventory.md
```

### Living Documents
These must be updated with EVERY change:
- Module changelogs
- Implementation status
- Master inventories
- API references

## 🔧 VOS4 Build Configuration

### Standard Module Config
```kotlin
// build.gradle.kts
android {
    namespace = "com.augmentalis.[module]"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
}

dependencies {
    implementation(project(":libraries:common"))
    // NO transitive dependencies
}
```

## 📋 Current Priorities

### High Priority
1. Complete VoiceUI consolidation
2. Fix remaining namespace issues
3. Optimize startup time to <1s
4. Complete documentation

### Medium Priority
1. Clean up legacy code
2. Implement missing tests
3. Performance profiling

### Low Priority
1. UI polish
2. Additional language support
3. Advanced features

## 🎯 VOS4 Success Metrics

### Code Quality
- Zero critical bugs
- <0.1% crash rate  
- >80% test coverage

### Performance
- All targets green
- Consistent performance
- Low memory footprint

### Documentation
- 100% API coverage
- All modules documented
- Changelogs current

## 📚 Key VOS4 Documents

### Architecture
- `/docs/ARCHITECTURE.md`
- `/docs/INTERACTION_MAP.md`
- `/docs/BUILD-ERROR-FIX-PLAN.md`

### Status
- `/docs/Status/Current/`
- `/docs/SpeechRecognition-Status-Report-2025-01-24.md`

### Planning
- `/docs/TODO/VOS4-TODO-Master.md`
- `/docs/ROADMAP.md`

## 🔄 VOS4 Workflow

### Daily Development
1. Check CURRENT-TASK-PRIORITY.md
2. Update working on task
3. Follow COT/ROT/TOT for issues
4. Update documentation
5. Commit with proper staging

### Weekly Tasks
1. Update implementation status
2. Review and update TODOs
3. Performance profiling
4. Documentation review

---

**These rules are specific to VOS4. Always check master rules first.**

*Last Updated: 2025-01-30*  
*Version: 1.0.0*