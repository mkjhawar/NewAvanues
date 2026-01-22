# Module Consolidation Analysis

**Date:** 2026-01-18 | **Version:** V3 | **Author:** Claude
**Status:** In Progress - Full Module Audit

---

## Executive Summary

This document tracks the consolidation, migration, and cleanup of ALL modules in the NewAvanues monorepo. The goal is to establish clear ownership, eliminate duplication, and ensure a clean KMP architecture with logical groupings.

---

## 1. Full Module Inventory

### All Top-Level Modules

| Module | Files | KMP | Android | iOS | Desktop | JS | Legacy | Sub-modules | Status |
|--------|-------|-----|---------|-----|---------|----|----|-------------|--------|
| Actions | 40 | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ | 1 | EVALUATE |
| AI | 392 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 7 | KEEP |
| AVA | 162 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 2 | KEEP |
| AvaMagic | 847 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 20+ | KEEP |
| AVID | 9 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | 1 | KEEP |
| AvidCreator | 39 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | 1 | **MERGE→AvaMagic** |
| Database | 32 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | 1 | EVALUATE |
| DeviceManager | 146 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | 1 | KEEP |
| DeviceManagerKMP | **0** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 1 | **REMOVE** (empty) |
| LicenseManager | 7 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | 1 | **CONSOLIDATE** |
| LicenseSDK | 2 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | 1 | **CONSOLIDATE** |
| LicenseValidation | 14 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | 1 | **CONSOLIDATE** |
| PluginSystem | 79 | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | 1 | KEEP |
| SpeechRecognition | 121 | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | 1 | KEEP |
| Translation | 1 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | 0 | **REMOVE** (1 file) |
| UniversalRPC | 178 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | 1 | KEEP |
| Utilities | 20 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | 1 | **MERGE→AVA** |
| Voice | 327 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 2 | **CONSOLIDATE** |
| VoiceKeyboard | 22 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | 1 | EVALUATE |
| VoiceOS | 907 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 5+ | **CONSOLIDATE** |
| VoiceOSCore | 222 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | 1 | **MASTER** |
| VoiceOSCoreNG | **0** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 0 | **REMOVE** (empty) |
| WebAvanue | 269 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 2 | KEEP |

**Legend:** KMP = Has commonMain, ✅ = Has platform source set, Legacy = Has src/main instead of KMP structure

---

## 2. Critical Duplicates & Empty Modules

### Empty Modules (REMOVE)
| Module | Action |
|--------|--------|
| **DeviceManagerKMP** | DELETE - 0 files, duplicate name |
| **VoiceOSCoreNG** | DELETE - 0 files, empty shell |
| **Translation** | DELETE or MERGE - Only 1 file |

### Duplicate/Overlapping Modules

#### VoiceOS Core Logic (3 LOCATIONS!)
| Location | Package | Files | Action |
|----------|---------|-------|--------|
| `Modules/VoiceOSCore` | `com.augmentalis.voiceoscore` | 222 | **MASTER** |
| `Modules/Voice/Core` | `com.augmentalis.voiceoscoreng` | 173 | MERGE→VoiceOSCore |
| `Modules/VoiceOS/VoiceOSCore` | `com.augmentalis.voiceoscoreng` | 490 | MERGE→VoiceOSCore |

**Total: 885 files across 3 locations need consolidation!**

#### License Modules (3 MODULES)
| Module | Files | Action |
|--------|-------|--------|
| LicenseManager | 7 | MERGE into single License module |
| LicenseSDK | 2 | MERGE into single License module |
| LicenseValidation | 14 | MERGE into single License module |

#### Device Manager
| Module | Files | Action |
|--------|-------|--------|
| DeviceManager | 146 | KEEP (full KMP) |
| DeviceManagerKMP | 0 | DELETE (empty) |

---

## 3. Sub-Module Breakdown

### AI Module (392 files, 7 sub-modules)
| Sub-module | Files | Purpose |
|------------|-------|---------|
| ALC | 31 | App Learning Core |
| Chat | 64 | Chat functionality |
| LLM | 90 | LLM integration |
| Memory | 9 | Memory management |
| NLU | 101 | Natural Language Understanding |
| RAG | 78 | Retrieval Augmented Generation |
| Teach | 19 | Teaching/training |

### AvaMagic Module (847 files, 20+ sub-modules)
| Sub-module | Purpose |
|------------|---------|
| AVACode | Code generation |
| AvaUI | UI components (27 sub-components) |
| AVURuntime | Runtime engine |
| Core | Core types |
| Data | Data handling |
| IPC | Inter-process communication |
| LearnAppCore | App learning |
| Logging | Logging system |
| MagicTools | Developer tools |
| managers | Various managers |
| Observability | Monitoring |
| PluginRecovery | Plugin recovery |
| Plugins | Plugin system |
| Preferences | Preferences |
| VoiceIntegration | Voice integration |

### AvaUI Sub-components (27)
Adapters, ARGScanner, AssetManager, Core, CoreTypes, Data, DesignSystem, Display, Docs, Examples, Feedback, Floating, Foundation, Input, Layout, Navigation, Renderers, StateManagement, TemplateLibrary, Theme, ThemeBridge, ThemeBuilder, UIConvertor, Voice, VoiceCommandRouter

### VoiceOS Module (907 files)
| Sub-module | Path | Files |
|------------|------|-------|
| **VoiceOSCore** | VoiceOS/VoiceOSCore | 490 |
| accessibility-types | VoiceOS/core/accessibility-types | 3 |
| command-models | VoiceOS/core/command-models | 3 |
| constants | VoiceOS/core/constants | 3 |
| database | VoiceOS/core/database | 220 |
| exceptions | VoiceOS/core/exceptions | 2 |
| hash | VoiceOS/core/hash | 5 |
| json-utils | VoiceOS/core/json-utils | 4 |
| result | VoiceOS/core/result | 2 |
| text-utils | VoiceOS/core/text-utils | 4 |
| validation | VoiceOS/core/validation | 2 |
| voiceos-logging | VoiceOS/core/voiceos-logging | 15 |
| CommandManager | VoiceOS/managers/CommandManager | 112 |
| HUDManager | VoiceOS/managers/HUDManager | 17 |
| LocalizationManager | VoiceOS/managers/LocalizationManager | 15 |
| VoiceDataManager | VoiceOS/managers/VoiceDataManager | 10 |

### Voice Module (327 files, 2 sub-modules)
| Sub-module | Files | Package |
|------------|-------|---------|
| Core | 173 | `com.augmentalis.voiceoscoreng` |
| WakeWord | ~154 | - |

---

## 4. Proposed Optimal Folder Structure

```
Modules/
│
├── AI/                          # AI & Machine Learning
│   ├── ALC/                     # App Learning Core
│   ├── Chat/                    # Chat functionality
│   ├── LLM/                     # LLM integration
│   ├── Memory/                  # Memory management
│   ├── NLU/                     # Natural Language Understanding
│   ├── RAG/                     # Retrieval Augmented Generation
│   └── Teach/                   # Teaching/training
│
├── AVA/                         # Core Utilities & Foundation
│   ├── Core/                    # Core types, utilities
│   ├── Overlay/                 # Overlay system
│   ├── Utilities/               # ← MERGE from Modules/Utilities
│   ├── Hash/                    # ← MOVE from VoiceOS/core/hash
│   ├── JsonUtils/               # ← MOVE from VoiceOS/core/json-utils
│   ├── Result/                  # ← MOVE from VoiceOS/core/result
│   └── TextUtils/               # ← MOVE from VoiceOS/core/text-utils
│
├── AvaMagic/                    # UI Framework & AVID System
│   ├── AvaUI/                   # UI components (keep as-is)
│   ├── AVURuntime/              # Runtime engine
│   ├── AVID/                    # ← MOVE from Modules/AVID
│   ├── AvidCreator/             # ← MERGE from Modules/AvidCreator
│   ├── Core/                    # Core types
│   ├── Data/                    # Data handling
│   ├── IPC/                     # Inter-process communication
│   ├── Logging/                 # Logging system
│   ├── Plugins/                 # Plugin system
│   └── VoiceIntegration/        # Voice integration
│
├── Device/                      # Device & Hardware
│   └── DeviceManager/           # ← RENAME from Modules/DeviceManager
│
├── License/                     # Licensing (CONSOLIDATED)
│   ├── Core/                    # ← MERGE LicenseSDK + LicenseManager
│   └── Validation/              # ← FROM LicenseValidation
│
├── Speech/                      # Speech & Audio
│   ├── Recognition/             # ← RENAME from SpeechRecognition
│   └── WakeWord/                # ← MOVE from Voice/WakeWord
│
├── Voice/                       # Voice Control System
│   ├── Core/                    # ← UNIFIED VoiceOSCore (MASTER)
│   │   └── (all voice logic consolidated here)
│   ├── Database/                # ← FROM VoiceOS/core/database
│   ├── Managers/
│   │   ├── CommandManager/      # ← FROM VoiceOS/managers/
│   │   ├── HUDManager/
│   │   ├── LocalizationManager/
│   │   └── VoiceDataManager/
│   ├── Keyboard/                # ← FROM VoiceKeyboard
│   ├── Actions/                 # ← FROM Modules/Actions
│   └── Types/
│       ├── AccessibilityTypes/  # ← FROM VoiceOS/core/accessibility-types
│       └── CommandModels/       # ← FROM VoiceOS/core/command-models
│
├── Network/                     # Networking & Communication
│   ├── UniversalRPC/            # RPC system
│   └── PluginSystem/            # Plugin communication
│
└── Web/                         # Web Platform
    └── WebAvanue/               # Web utilities
```

---

## 5. Module Groupings Summary

### Voice Domain (CONSOLIDATE ALL)
**Current:** 6 scattered locations → **Target:** 1 unified `Voice/` folder

| Current Location | Files | Target |
|-----------------|-------|--------|
| Modules/VoiceOSCore | 222 | Voice/Core |
| Modules/Voice/Core | 173 | Voice/Core |
| Modules/VoiceOS/VoiceOSCore | 490 | Voice/Core |
| Modules/VoiceOS/core/* | 263 | Voice/Types, Voice/Database |
| Modules/VoiceOS/managers/* | 154 | Voice/Managers |
| Modules/VoiceKeyboard | 22 | Voice/Keyboard |
| Modules/Actions | 40 | Voice/Actions |

### AVA Domain (Generic Utilities)
| Current Location | Target |
|-----------------|--------|
| Modules/Utilities | AVA/Utilities |
| VoiceOS/core/hash | AVA/Hash |
| VoiceOS/core/json-utils | AVA/JsonUtils |
| VoiceOS/core/result | AVA/Result |
| VoiceOS/core/text-utils | AVA/TextUtils |

### AvaMagic Domain (UI/AVID)
| Current Location | Target |
|-----------------|--------|
| Modules/AVID | AvaMagic/AVID |
| Modules/AvidCreator | AvaMagic/AvidCreator |

### License Domain (CONSOLIDATE)
| Current Location | Target |
|-----------------|--------|
| Modules/LicenseManager | License/Core |
| Modules/LicenseSDK | License/Core |
| Modules/LicenseValidation | License/Validation |

### Speech Domain
| Current Location | Target |
|-----------------|--------|
| Modules/SpeechRecognition | Speech/Recognition |
| Modules/Voice/WakeWord | Speech/WakeWord |

---

## 6. Modules to DELETE

| Module | Reason |
|--------|--------|
| DeviceManagerKMP | Empty (0 files) |
| VoiceOSCoreNG | Empty (0 files) |
| Translation | Near-empty (1 file) |
| Voice/Core | After merge to Voice/Core |
| VoiceOS/VoiceOSCore | After merge to Voice/Core |

---

## 7. KMP Migration Status

### Full KMP (Ready) ✅
- DeviceManager
- AVID
- LicenseValidation
- SpeechRecognition
- UniversalRPC
- Utilities
- VoiceOSCore
- PluginSystem (partial)

### Needs KMP Migration
- AI/* (all sub-modules)
- AvaMagic/* (all sub-modules)
- Voice/WakeWord
- VoiceOS/core/database
- VoiceOS/managers/*
- WebAvanue

### Android-Only (Evaluate)
- Actions
- AvidCreator
- LicenseManager
- VoiceKeyboard
- Translation

---

## 8. Action Items

| Priority | Task | Status |
|----------|------|--------|
| **P0** | Delete empty modules (DeviceManagerKMP, VoiceOSCoreNG) | TODO |
| **P0** | Consolidate 3 VoiceOSCore locations into Voice/Core | TODO |
| **P1** | Consolidate License modules | TODO |
| **P1** | Move generic utils to AVA | TODO |
| **P1** | Move AVID/AvidCreator into AvaMagic | TODO |
| **P2** | Reorganize folder structure per proposal | TODO |
| **P2** | KMP migrate remaining modules | TODO |
| **P3** | Delete/archive Translation module | TODO |

---

## 9. Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-18 | V1 | Initial document - Voice modules only |
| 2026-01-18 | V2 | Complete Voice audit with file counts |
| 2026-01-18 | V3 | **Full module inventory**, all 23 top-level modules audited, KMP status, duplicates identified, proposed optimal folder structure |

