# LD-Module-Registry-V1 - Module Registry Living Document

**Type:** Living Document | **Version:** 1 | **Last Updated:** 2026-01-11

---

## Module Registry

This document tracks all modules in the Avanues Platform with their current status, dependencies, and key metrics.

---

## Core Modules

### VoiceOSCoreNG

| Property | Value |
|----------|-------|
| **Path** | `/Modules/VoiceOSCoreNG` |
| **Type** | KMP Library |
| **Status** | Production |
| **Version** | 1.0.0 |
| **Lines of Code** | 8,000+ |
| **Key Classes** | VoiceOSCoreNG, ActionCoordinator, HandlerRegistry |
| **Dependencies** | Common/VUID, Common/VoiceOS/* |

**Capabilities:**
- Voice command processing (5-level priority)
- Handler system (11 categories)
- Speech engine abstraction (6 engines)
- NLU/LLM integration
- Dynamic command registry

---

### VoiceOS App

| Property | Value |
|----------|-------|
| **Path** | `/android/apps/voiceoscoreng` |
| **Type** | Android App |
| **Status** | Production |
| **Version** | 1.0.0 |
| **Lines of Code** | 3,600+ |
| **Key Classes** | VoiceOSAccessibilityService, OverlayService |
| **Dependencies** | VoiceOSCoreNG, Common/Database |

**Capabilities:**
- Accessibility service
- Numbers overlay
- Screen exploration
- Continuous monitoring

---

### AVA

| Property | Value |
|----------|-------|
| **Path** | `/Modules/AVA` |
| **Type** | KMP Library + Android App |
| **Status** | Production |
| **Version** | 1.0.0 |
| **Files** | 288 Kotlin files |
| **Key Classes** | ActionsManager, ChatViewModel, MemoryManager |
| **Dependencies** | LLM, NLU, RAG, Common/* |

**Capabilities:**
- AI voice assistant
- 90+ intent handlers
- Dual NLU (MobileBERT + mALBERT)
- Memory system with decay
- Wake word detection

---

### LLM

| Property | Value |
|----------|-------|
| **Path** | `/Modules/LLM` |
| **Type** | KMP Library |
| **Status** | Production |
| **Version** | 2.0 |
| **Classes** | 60+ |
| **Key Classes** | HybridResponseGenerator, LocalLLMProvider, CloudLLMProvider |
| **Dependencies** | Common/Core |

**Capabilities:**
- On-device inference (MLC + TVM)
- Cloud fallback (4 providers)
- Multilingual (140+ languages)
- Streaming responses
- Cost tracking

---

### NLU

| Property | Value |
|----------|-------|
| **Path** | `/Modules/NLU`, `/Modules/Shared/NLU` |
| **Type** | KMP Library |
| **Status** | Production |
| **Version** | 1.0 |
| **Classes** | 50+ |
| **Key Classes** | HybridIntentClassifier, IntentClassifier, BertTokenizer |
| **Dependencies** | Common/VUID |

**Capabilities:**
- BERT-based classification
- 52+ languages (mALBERT)
- Hybrid matching (exact/fuzzy/semantic)
- Self-learning
- <100ms latency

---

### RAG

| Property | Value |
|----------|-------|
| **Path** | `/Modules/RAG` |
| **Type** | KMP Library |
| **Status** | Production |
| **Version** | 1.0 |
| **Classes** | 40+ |
| **Key Classes** | SQLiteRAGRepository, RAGChatEngine, KMeansClustering |
| **Dependencies** | Common/Database |

**Capabilities:**
- Document indexing (PDF, DOCX, MD, HTML)
- Semantic search
- K-means clustering (256 clusters)
- LRU query cache
- Chat integration

---

### WebAvanue

| Property | Value |
|----------|-------|
| **Path** | `/Modules/WebAvanue` |
| **Type** | KMP Library + Apps |
| **Status** | Development |
| **Version** | 0.9 |
| **Lines of Code** | 33,000+ |
| **Key Classes** | DOMScraperBridge, VoiceCommandGenerator, TabManager |
| **Dependencies** | VoiceOSCoreNG, Common/* |

**Capabilities:**
- Voice-controlled browser
- DOM scraping with VUIDs
- Tab management
- Reader mode
- Encrypted storage

---

## Common Libraries

| Library | Path | Type | Status | Purpose |
|---------|------|------|--------|---------|
| VUID | `/Common/VUID` | KMP | Production | Unique ID generation |
| VoiceOS Result | `/Common/VoiceOS/result` | KMP | Production | Type-safe errors |
| VoiceOS Hash | `/Common/VoiceOS/hash` | KMP | Production | SHA-256 hashing |
| VoiceOS Constants | `/Common/VoiceOS/constants` | KMP | Production | Configuration |
| VoiceOS Validation | `/Common/VoiceOS/validation` | KMP | Production | SQL escaping |
| VoiceOS Exceptions | `/Common/VoiceOS/exceptions` | KMP | Production | Exception hierarchy |
| VoiceOS Database | `/Common/VoiceOS/database` | KMP | Production | SQLDelight persistence |
| Core AssetManager | `/Common/Core/AssetManager` | KMP | Production | Icon/image management |
| Core ThemeManager | `/Common/Core/ThemeManager` | KMP | Production | Universal themes |
| AvaElements | `/Common/AvaElements` | KMP | Production | UI component system |
| Database | `/Common/Database` | KMP | Production | KMP database wrapper |
| UI | `/Common/UI` | Android | Production | Compose components |
| Utils | `/Common/Utils` | Android | Production | Utility helpers |

---

## Dependency Graph

```
VoiceOS App
    └── VoiceOSCoreNG
            ├── Common/VUID
            ├── Common/VoiceOS/*
            └── Common/Database

AVA
    ├── LLM
    │     └── Common/Core
    ├── NLU
    │     └── Common/VUID
    ├── RAG
    │     └── Common/Database
    └── Common/*

WebAvanue
    ├── VoiceOSCoreNG
    └── Common/*

All Modules
    └── Common Libraries
```

---

## Build Requirements

| Requirement | Version |
|-------------|---------|
| JDK | 17 (required) |
| Gradle | 8.x |
| Android SDK | 34 |
| Kotlin | 1.9.20+ |
| Min Android | API 28 |

---

## Test Coverage

| Module | Unit Tests | Integration | Coverage |
|--------|------------|-------------|----------|
| VoiceOSCoreNG | 50+ | 20+ | 85% |
| VoiceOS App | 30+ | 15+ | 80% |
| AVA | 100+ | 50+ | 85% |
| LLM | 40+ | 20+ | 80% |
| NLU | 50+ | 25+ | 90% |
| RAG | 30+ | 15+ | 85% |
| WebAvanue | 400+ | 50+ | 85% |
| Common | 175+ | 50+ | 90% |

---

## Change Log

| Date | Module | Change |
|------|--------|--------|
| 2026-01-11 | All | Initial registry creation |
| 2026-01-09 | VoiceOSCoreNG | AppHandler added |
| 2026-01-09 | VoiceOS App | Numbers overlay improvements |

---

**Maintainer:** Platform Team
**Next Review:** Weekly
