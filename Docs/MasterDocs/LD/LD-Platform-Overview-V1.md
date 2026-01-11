# LD-Platform-Overview-V1 - Avanues Platform Living Document

**Type:** Living Document | **Version:** 1 | **Last Updated:** 2026-01-11

---

## Platform Summary

The Avanues Platform is a comprehensive voice-first accessibility and AI assistant ecosystem built with Kotlin Multiplatform (KMP).

### Core Products

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AVANUES PLATFORM                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   VoiceOS   │  │     AVA     │  │  WebAvanue  │  │   Cockpit   │    │
│  │  Voice-First│  │ AI Voice   │  │Voice Browser│  │  Dashboard  │    │
│  │Accessibility│  │ Assistant  │  │             │  │             │    │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │
│         │                │                │                │            │
│         └────────────────┼────────────────┼────────────────┘            │
│                          │                │                              │
│                    ┌─────▼────────────────▼─────┐                       │
│                    │    SHARED MODULES          │                       │
│                    │ ┌─────┐ ┌─────┐ ┌─────┐   │                       │
│                    │ │ NLU │ │ LLM │ │ RAG │   │                       │
│                    │ └─────┘ └─────┘ └─────┘   │                       │
│                    └────────────────────────────┘                       │
│                                                                          │
│                    ┌────────────────────────────┐                       │
│                    │    COMMON LIBRARIES        │                       │
│                    │ VUID, VoiceOS Utils, Core  │                       │
│                    │ Database, AvaElements, UI  │                       │
│                    └────────────────────────────┘                       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Module Status Matrix

| Module | Status | Platform | Version | Lines |
|--------|--------|----------|---------|-------|
| VoiceOSCoreNG | Production | KMP | 1.0.0 | 8,000+ |
| VoiceOS App | Production | Android | 1.0.0 | 3,600+ |
| AVA | Production | KMP | 1.0.0 | 288 files |
| LLM | Production | KMP | 2.0 | 60+ classes |
| NLU | Production | KMP | 1.0 | 50+ classes |
| RAG | Production | KMP | 1.0 | 40+ classes |
| WebAvanue | Development | KMP | 0.9 | 33,000+ |
| Common | Production | KMP | - | 14 libraries |

---

## Technology Stack

### Languages & Frameworks

| Layer | Technology |
|-------|------------|
| Shared Logic | Kotlin Multiplatform (KMP) |
| Android UI | Jetpack Compose |
| iOS UI | SwiftUI (planned) |
| Desktop | Tauri + React |
| Web | React + TypeScript |
| Database | SQLDelight |

### AI/ML Components

| Component | Technology |
|-----------|------------|
| NLU | ONNX Runtime + BERT/mALBERT |
| LLM (On-device) | MLC LLM + TVM Runtime |
| LLM (Cloud) | Claude, GPT-4, Gemini |
| Speech | Vivoka, Android STT, Vosk |
| Embeddings | Sentence Transformers |

---

## Architecture Principles

### 1. Voice-First Design
All features designed for voice interaction first, with touch as secondary input.

### 2. Privacy-First Processing
95%+ processing happens on-device. Cloud is fallback only.

### 3. Cross-Platform Parity
Single codebase via KMP serves Android, iOS, Desktop.

### 4. Accessibility Native
WCAG 2.1 AA compliance built into every component.

### 5. Modular Architecture
Each module can be used independently or composed together.

---

## Key Metrics (Current)

### Performance

| Metric | Target | Current |
|--------|--------|---------|
| Command Latency | <100ms | 52ms |
| NLU Accuracy | 95% | 89% |
| LLM Response | <3s | 2.5s |
| RAG Search | <100ms | 80ms |
| App Startup | <2s | 1.8s |

### Coverage

| Metric | Value |
|--------|-------|
| Test Coverage | 85%+ |
| Code Documentation | 90% |
| Languages Supported | 52+ |
| Voice Commands | 100+ |

---

## Integration Points

```
VoiceOS (Accessibility Layer)
    │
    ├──► VoiceOSCoreNG (Command Processing)
    │       ├──► NLU (Intent Classification)
    │       ├──► Handlers (Action Execution)
    │       └──► Speech Engines
    │
    ├──► AVA (AI Assistant)
    │       ├──► LLM (Response Generation)
    │       ├──► RAG (Document Retrieval)
    │       ├──► Actions (90+ handlers)
    │       └──► Memory (Long-term storage)
    │
    └──► WebAvanue (Voice Browser)
            ├──► DOM Scraping
            ├──► Voice Commands
            └──► Tab Management
```

---

## Deployment Targets

### Android
- Min SDK: 28 (Android 9)
- Target SDK: 34 (Android 14)
- Architecture: arm64-v8a

### iOS (Planned)
- Min Version: iOS 14+
- Architecture: arm64

### Desktop (Planned)
- Tauri 2.0
- Windows, macOS, Linux

---

## Roadmap Summary

### Q1 2026
- [ ] VoiceOS iOS port
- [ ] AVA multimodal (vision)
- [ ] WebAvanue Desktop

### Q2 2026
- [ ] Cockpit production release
- [ ] Enterprise features
- [ ] SDK for third-party apps

---

## Document Links

| Document | Location |
|----------|----------|
| VoiceOSCoreNG | [README.md](../VoiceOSCoreNG/README.md) |
| VoiceOS App | [README.md](../VoiceOS/README.md) |
| AVA | [README.md](../AVA/README.md) |
| LLM | [README.md](../LLM/README.md) |
| NLU | [README.md](../NLU/README.md) |
| RAG | [README.md](../RAG/README.md) |
| WebAvanue | [README.md](../WebAvanue/README.md) |
| Common | [README.md](../Common/README.md) |

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-11 | V1 | Initial creation |

---

**Maintainer:** Avanues Platform Team
**Next Review:** 2026-02-01
