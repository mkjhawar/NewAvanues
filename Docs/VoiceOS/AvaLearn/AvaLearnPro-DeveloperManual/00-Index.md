# AvaLearnPro Developer Manual

**Product:** AvaLearnPro (Developer Edition)
**Version:** 2.0.0
**Last Updated:** 2025-12-11
**Document Set:** VoiceOS-AvaLearnPro-DeveloperManual
**Audience:** Developers, QA Engineers, Technical Staff

---

## Manual Index

| Chapter | Title | Description |
|---------|-------|-------------|
| [01](./01-Introduction.md) | Introduction & Architecture | System overview, design decisions, module structure |
| [02](./02-AIDL-Interface.md) | AIDL Interface Reference | Complete IPC interface documentation |
| [03](./03-Core-Classes.md) | Core Classes & Data Models | All classes, data structures, enums |
| [04](./04-UI-Components.md) | UI Components & Theme | Jetpack Compose components, Ocean Blue Dark theme |
| [05](./05-Event-Streaming.md) | Event Streaming System | Real-time event capture and processing |
| [06](./06-Safety-Implementation.md) | Safety System Implementation | DoNotClick, Login Detection, Loop Prevention |
| [07](./07-AVU-Export-System.md) | AVU Export System | Export pipeline, format specification, encryption |
| [08](./08-Debugging-Tools.md) | Debugging & Analysis Tools | Logs console, element inspector, event viewer |
| [09](./09-ADR-Decisions.md) | Architecture Decision Records | Design decisions and rationale |
| [10](./10-API-Reference.md) | Complete API Reference | All public APIs, methods, callbacks |

---

## Quick Start (Developer)

1. **Clone**: Get the VoiceOS repository
2. **Build**: `./gradlew :Modules:VoiceOS:apps:LearnAppDev:assembleDebug`
3. **Install**: `adb install -r LearnAppDev-debug.apk`
4. **Enable**: Grant accessibility to VoiceOS Core
5. **Connect**: AvaLearnPro auto-binds to JIT service
6. **Debug**: Use Logs tab for real-time events

---

## Developer Edition Features

| Feature | User Edition | Developer Edition |
|---------|--------------|-------------------|
| Theme | Ocean Blue Light | Ocean Blue Dark + Cyan |
| Logging | Toast only | Full console (500 entries) |
| Elements | Hidden | Inspector with properties |
| Events | Not visible | Real-time stream viewer |
| AVU Export | Encrypted | Plain text (readable) |
| Neo4j | None | Graph visualization (planned) |
| Build Flags | Release | Debug + logging enabled |

---

## Package Information

| Property | Value |
|----------|-------|
| Package Name | `com.augmentalis.learnappdev` |
| Application ID | `com.augmentalis.learnappdev` |
| Debug Suffix | `.debug` |
| Minimum SDK | 34 (Android 14) |
| Target SDK | 35 (Android 15) |

---

## Build Configuration

```kotlin
// build.gradle.kts
buildConfigField("Boolean", "IS_DEVELOPER_EDITION", "true")
buildConfigField("Boolean", "ENABLE_NEO4J", "true")
buildConfigField("Boolean", "ENABLE_LOGGING", "true")
```

---

## Module Dependencies

```
LearnAppDev
    ├── LearnAppCore (business logic)
    │   ├── JITLearning (AIDL interfaces)
    │   └── Core:Database (SQLDelight)
    └── VoiceOSCore (service provider)
```

---

## Document Conventions

| Symbol | Meaning |
|--------|---------|
| **Bold** | Class names, important terms |
| `Code` | Technical values, code snippets |
| `📦` | Package reference |
| `🔧` | Configuration |
| `⚠️` | Warning/caution |

---

## Related Documents

- AvaLearnLite User Manual
- VoiceOS Architecture Guide
- AVU Format Specification
- AIDL Interface Specification
- JITLearning Module Documentation

---

**Copyright 2025 Augmentalis. All rights reserved.**
