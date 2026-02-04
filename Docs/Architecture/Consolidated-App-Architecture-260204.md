# Consolidated App Architecture

**Date**: 2026-02-04
**Version**: 2.2.0
**Status**: Active

---

## Overview

The NewAvanues platform is a modular, cross-platform voice and AI accessibility system built on Kotlin Multiplatform (KMP). This document describes the consolidated architecture after the module reorganization.

## Module Structure

```
NewAvanues/
├── Modules/
│   ├── VoiceOSCore/           # Core voice OS functionality (KMP)
│   ├── AI/                    # AI subsystem
│   │   ├── NLU/              # Natural Language Understanding
│   │   ├── LLM/              # Large Language Models (ALC)
│   │   ├── Chat/             # Chat/conversation management
│   │   └── RAG/              # Retrieval Augmented Generation
│   ├── AVAMagic/             # Magic UI and app learning
│   │   ├── AVAUI/            # UI components and renderers
│   │   ├── IPC/              # Inter-process communication
│   │   ├── LearnAppCore/     # App exploration/learning
│   │   └── Data/             # Data management
│   ├── AVUCodec/             # AVU format encoder/decoder (KMP)
│   ├── AVID/                 # Element identifier system (KMP)
│   ├── Rpc/                  # gRPC communication layer
│   ├── WebAvanue/            # Browser module
│   ├── Localization/         # Multi-language support
│   ├── DeviceManager/        # Device capabilities
│   ├── SpeechRecognition/    # Speech engines (Vosk, Vivoka)
│   └── PluginSystem/         # Plugin architecture
├── Avanues/
│   └── Web/                  # Web-based applications
└── Docs/                     # Documentation
```

## Communication Architecture

### gRPC Services (Modules/Rpc/Common/proto/)

| Service | Proto File | Purpose |
|---------|------------|---------|
| VoiceOSService | `voiceos.proto` | Core accessibility, commands, screen scraping |
| VoiceRecognitionService | `recognition.proto` | Speech-to-text |
| NLUService | `nlu.proto` | Intent recognition |
| AvaService | `ava.proto` | AVA assistant |
| WebAvanueService | `webavanue.proto` | Browser integration |
| PluginService | `plugin.proto` | Plugin lifecycle |
| VoiceCursorService | `cursor.proto` | Voice cursor navigation |
| CockpitService | `cockpit.proto` | Desktop control center |
| ExplorationService | `exploration.proto` | App learning |

### Communication Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Apps                            │
│   (VoiceOSCoreNG, WebAvanue, AVA, Cockpit)                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              UniversalClient (Modules/Rpc)                  │
│   ├── PlatformClient (expect/actual)                       │
│   └── RegistryAwareClient (service discovery)              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    gRPC Transport                           │
│   ├── Android: gRPC-Android, Binder fallback               │
│   ├── iOS: gRPC-Swift, XPC fallback                        │
│   ├── Desktop: gRPC-Kotlin, UDS                            │
│   └── Web: gRPC-Web, WebSocket fallback                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Service Implementations                   │
│   (VoiceOSCore, NLU, Recognition, etc.)                    │
└─────────────────────────────────────────────────────────────┘
```

## Data Format: AVU 2.2

All data exchange uses **Avanues Universal Format v2.2**:

```
# Avanues Universal Format v2.2
# Type: <type>
---
schema: avu-2.2
version: 2.2.0
locale: en-US
project: voiceos
metadata:
  file: <filename>
  category: <category>
---
<DATA_LINES>
```

### File Extensions

| Extension | Type | Description |
|-----------|------|-------------|
| `.vos` | VOS | Voice commands and screens |
| `.aai` | AVA AI | NLU intents and patterns |
| `.acd` | App Category | App classification database |
| `.aot` | Ontology | Semantic intent ontology |
| `.syn` | Synonyms | Synonym packs |
| `.amf` | Magic Format | UI layouts and themes |

## Database Architecture

### SQLDelight (KMP)

- **Location**: `Modules/AVA/core/Data/src/commonMain/sqldelight/`
- **Databases**:
  - `AVADatabase` - Intents, examples, ontology
  - `VoiceOSDatabase` - Commands, screens, elements
  - `AppCategoryDatabase` - App classification

### Key Tables

```sql
-- Intent examples (NLU)
intent_example (
  id, intent_id, text, locale, source, format_version
)

-- Voice commands
voice_command (
  id, action, primary_text, synonyms, category, locale
)

-- App categories
app_category_override (
  package_name, category, confidence
)
```

## External File Injection Architecture

### Storage Paths

```
External Storage Base (app-specific)
├── core/           # Core system files
│   ├── intents/    # Core intent definitions
│   └── commands/   # System commands
├── voiceos/        # VoiceOS-specific
│   ├── learned/    # Learned app data
│   └── screens/    # Screen definitions
├── user/           # User customizations
│   ├── intents/    # Custom intents
│   ├── commands/   # Custom commands
│   └── synonyms/   # Custom synonyms
└── downloaded/     # Downloaded packs
    ├── languages/  # Language packs
    └── models/     # AI models
```

### File Loaders

| Loader | Location | Purpose |
|--------|----------|---------|
| `CommandLoader` | VoiceOSCore | Load .vos commands with locale fallback |
| `VOSCommandIngestion` | VoiceOSCore | Batch command ingestion |
| `IntentSourceCoordinator` | AI/NLU | Load .ava intents from multiple sources |
| `AonLoader` | AI/NLU | Load .aot ontology files |
| `AppCategoryLoader` | VoiceOSCore | Load .acd app categories |
| `SynonymLoader` | VoiceOSCore | Load .syn synonym packs |

### Injection Flow

```
External Files (.vos, .ava, .acd)
          │
          ▼
┌─────────────────────────┐
│  File Reader/Parser     │
│  - Schema validation    │
│  - Format detection     │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Entity Converter       │
│  - To database entities │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Database Ingestion     │
│  - Batch insert         │
│  - Version tracking     │
└─────────────────────────┘
```

## Localization System

### Supported Languages

- **Vosk** (offline): 8 languages
- **Vivoka** (premium): 42+ languages

### Localization Flow

```
User selects language
        │
        ▼
┌─────────────────────────┐
│  Localizer.setLanguage  │
│  (SharedPreferences)    │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  CommandLoader          │
│  - Load locale files    │
│  - English fallback     │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  Database Update        │
│  - Insert commands      │
│  - Mark fallback status │
└─────────────────────────┘
```

### Adding New Languages

1. Create locale folder: `assets/commands/<locale>/`
2. Add command files: `navigation.vos`, `system.vos`, etc.
3. Create intent files: `assets/intents/<locale>/`
4. Update `LanguageSupport` enum if needed

## Key Packages

| Package | Description |
|---------|-------------|
| `com.augmentalis.voiceoscore` | Core VoiceOS types and services |
| `com.augmentalis.nlu` | NLU intent processing |
| `com.augmentalis.avucodec` | AVU format encoding/decoding |
| `com.augmentalis.avid` | Element identifier generation |
| `com.augmentalis.rpc` | gRPC client/server infrastructure |
| `com.augmentalis.localization` | Multi-language support |

---

*Last updated: 2026-02-04*
