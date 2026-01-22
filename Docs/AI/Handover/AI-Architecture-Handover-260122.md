# AI Architecture Rework - Handover Document

**Date:** 2026-01-22
**Branch:** `AI-Architecture-Rework`
**Status:** Analysis Phase - Ready to Start

---

## Project Overview

### Primary Goal
Create a **Universal Plugin Architecture** for the NewAvanues codebase that enables **accessibility-first voice and gaze control** for hand-challenged/handicapped users.

### Target Users
- **Hand-challenged individuals** who cannot use traditional touch/mouse input
- Rely on **voice commands** and **gaze/eye-tracking** for device control
- Need personalized, adaptive AI that learns their speech patterns

---

## Key Decisions Made

### 1. Platform Support
**All platforms:** Android, iOS, macOS, Windows, Linux

### 2. LLM Strategy
- **Hybrid approach**: On-device primary, cloud optional
- **Plugin-based**: Users can choose their preferred LLM
- On-device: llama.cpp/GGUF, Whisper
- Cloud: OpenAI, Anthropic (optional)

### 3. Speech Recognition
- **Plugin-based architecture** supporting all engines
- **Primary focus**: Vivoka
- Also support: Vosk, Whisper, GoogleCloud, AndroidSTT

### 4. Architecture Approach
- **KMP-First**: Kotlin Multiplatform for cross-platform orchestration
- **Rust Later**: Add Rust acceleration for performance-critical components (inference, vector search, tokenization)
- **AVU Format**: Use Avanues Universal Format instead of JSON for configuration

---

## Completed Work

### 1. Universal Plugin Architecture Plan
**File:** `Docs/AI/Plans/UniversalPlugin-Architecture-Plan-260122.md`
- Comprehensive design for universal plugin system
- Module-specific contracts (VoiceOSCore, AI, Speech)
- AccessibilityDataProvider interface
- Plugin manifest format (AVU)

### 2. AVU Format Updates
**Files:**
- `Modules/AVUCodec/src/commonMain/kotlin/com/augmentalis/avucodec/AVUEncoder.kt` - Added 12 plugin manifest codes
- `Modules/AVUCodec/src/commonMain/kotlin/com/augmentalis/avucodec/AVUDecoder.kt` - Added manifest parsing
- `Docs/VoiceOS/Technical/specifications/AVU-Universal-Format-Spec-260122-V2.md` - v2.0 spec

### 3. Plugin System Integration
**Files:**
- `Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/AvuManifestParser.kt` (NEW)
- `Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginLoader.kt` (MODIFIED - supports both YAML and AVU)

### 4. Earlier Architecture Plans (Reference)
- `Docs/AI/Plans/RustAI-Architecture-Plan-260122-V1.md` - Initial Rust-only design
- `Docs/AI/Plans/RustAI-Architecture-Plan-260122-V2-PluginFirst.md` - Plugin-first rewrite
- `Docs/AI/Plans/RustAI-Architecture-Plan-260122-V3-KMPFirst.md` - KMP-first approach (recommended)

---

## Next Steps Required

### Immediate: Deep Analysis of All Modules
Need to analyze and document:

1. **`/Modules/AI/*`** - All AI submodules (ALC, LLM, NLU, RAG, Chat, Memory, Teach)
2. **`/Modules/SpeechRecognition/*`** - Speech engine abstractions
3. **`/Modules/VoiceOSCore/*`** - Core voice control system (~47,600 LOC)
4. **`/Modules/PluginSystem/*`** - Existing plugin infrastructure
5. **`/Modules/AVA/*`** - Assistant core logic

### Analysis Goals
- Understand current architecture and data flows
- Identify what to preserve vs rewrite
- Map dependencies between modules
- Document findings to files for reference

---

## Key Codebase Context

### Module Inventory (22 modules found)
| Module | Purpose |
|--------|---------|
| VoiceOSCore | Core voice accessibility engine |
| AI | AI capabilities (LLM, NLU, RAG, etc.) |
| AVA | Assistant logic hub |
| SpeechRecognition | Speech engine abstraction |
| PluginSystem | Plugin loading/management |
| Database | SQLDelight persistence |
| AvidCreator | Element identification |
| AvaMagic | UI framework |
| Translation | Localization |
| UniversalRPC | Cross-platform IPC |
| + 12 more... | See exploration results |

### VoiceOSCore Database (45+ schemas)
Critical data for AI personalization:
- `ScrapedElement` - UI elements with bounds (for gaze targeting)
- `CommandHistory` - Voice command success/failure
- `RecognitionLearning` - Personalized speech patterns per engine
- `GestureLearning` - Gaze dwell calibration
- `ContextPreference` - Smart command ranking
- `NavigationEdge` - App navigation graph
- `GeneratedCommand` - AI-generated voice commands

### Existing Plugin Interfaces Found
- `AIPluginInterface` - Base AI plugin contract
- `TextGenerationPlugin`, `NLPPlugin`, `EmbeddingPlugin` - AI-specific
- `SpeechEnginePluginInterface` - Speech recognition contract
- `AccessibilityPluginInterface` - Accessibility event handling

---

## Git Status

### Branch
`AI-Architecture-Rework` - pushed to origin

### Last Commit
`267d4659` - feat(plugin-system): Add AVU format support for universal plugin architecture

### Untracked Files (from previous sessions)
- `Docs/AI/Plans/RustAI-Architecture-Plan-260122-V*.md` (3 files)
- `Docs/VoiceOSCore/Plans/CodeAvenue-Plan-*.md` (4 files)

---

## How to Continue

### Start Command
```
cd /Volumes/M-Drive/Coding/NewAvanues
git checkout AI-Architecture-Rework
```

### Prompt for New Session
```
Continue the AI Architecture Rework project. Read the handover document at:
Docs/AI/Handover/AI-Architecture-Handover-260122.md

The next step is to perform a deep analysis of all modules:
1. /Modules/AI/* - all AI submodules
2. /Modules/SpeechRecognition/* - speech engines
3. /Modules/VoiceOSCore/* - core voice system
4. /Modules/PluginSystem/* - existing plugin infrastructure
5. /Modules/AVA/* - assistant core

Document all findings to files. Focus on understanding:
- Current architecture and data flows
- What to preserve vs rewrite
- Dependencies between modules
- How VoiceOSCore data can feed AI for accessibility personalization
```

---

## Key Files to Read First

1. `Docs/AI/Plans/UniversalPlugin-Architecture-Plan-260122.md` - The master plan
2. `Docs/AI/Plans/RustAI-Architecture-Plan-260122-V3-KMPFirst.md` - KMP-first approach
3. `Modules/PluginSystem/src/commonMain/kotlin/.../core/PluginManifest.kt` - Existing manifest structure
4. `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/` - Core voice system

---

## Contacts & Resources

- **Repository:** https://gitlab.com/AugmentalisES/newavanues
- **Branch:** AI-Architecture-Rework
- **Working Directory:** /Volumes/M-Drive/Coding/NewAvanues

---

**End of Handover**
