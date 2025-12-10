# AVA AI

**Augmented Voice Assistant** - User-trainable AI assistant with privacy-first local processing, VOS4 integration, and smart glasses support.

**Status**: ✅ Production Ready (v1.0.0) - Phase 2.0 RAG Integration Complete
**Release Date**: 2025-11-22
**Phase 2.0 Deployment**: ✅ READY FOR PRODUCTION
**Constitution**: [`.ideacode/memory/principles.md`](.ideacode/memory/principles.md)
**Architecture**: [`project_planning/Project_CodingInstructions/ARCHITECTURE_COMPARISON.md`](project_planning/Project_CodingInstructions/ARCHITECTURE_COMPARISON.md)

## Architecture Overview

AVA uses a **hybrid architecture** combining proven AvaAssistant foundation with enterprise enhancements:

### Core Stack (Hybrid)
- **NLU**: ONNX Runtime Mobile + DistilBERT/MobileBERT (12MB, <50ms intent classification)
- **LLM**: MLC LLM + Gemma (2B/7B models) OR llama.cpp fallback
- **Training**: Teach-Ava (user-correctable AI without cloud retraining)
- **Database**: SQLDelight 2.1 (cross-platform) + Room façade (Android)
- **Vector Search**: Faiss (local RAG for document knowledge)
- **UI**: Jetpack Compose Multiplatform
- **Platform**: Kotlin Multiplatform (Android, iOS, macOS, Windows)

### VOS4 Integration (Phase 4)
- **Speech Recognition**: Reuses VOS4 (Vosk, Vivoka, AndroidSTT)
- **UI Theme**: Reuses VOS4 GlassmorphismTheme
- **Accessibility**: Reuses VOS4 AccessibilityService
- **Plugin**: MagicCode PluginSystem for dual deployment

### Key Differentiators
- ✅ **Teach-Ava**: User-trainable intent recognition with immediate feedback
- ✅ **Privacy-First**: 95%+ local processing (ONNX + MLC)
- ✅ **Constitutional AI**: 7 principles with >90% adherence (Phase 3)
- ✅ **Smart Glasses**: 8+ devices (Meta Ray-Ban, Vuzix, RealWear, XReal, Rokid, Even Realities)
- ✅ **Cross-Platform**: Android, iOS, macOS, Windows via KMP

## Quick Links

- **📘 Project Overview**: [`project_planning/AVA-Project-Overview-v1.0-20251026.md`](project_planning/AVA-Project-Overview-v1.0-20251026.md) - Comprehensive project documentation
- **📖 Old Project**: `/Volumes/M Drive/Coding/AVA Old /` - Feature-complete reference implementation
- **📚 Developer Manuals**: `/Volumes/M Drive/Coding/AVA Old /AVA Project/AVA pre build/Documentation/Manuals/Manuals - Developer/ava/`

## Project Structure

```
/AVA AI/
├── app/                     # Main application module
│   ├── src/main/java/com/augmentalis/ava/
│   │   ├── core/            # Core functionality (NLP, inference)
│   │   ├── data/            # Data layer (repositories, sources)
│   │   ├── di/              # Dependency injection (Hilt)
│   │   ├── domain/          # Domain layer (use cases, models)
│   │   ├── framework/       # Framework layer (AVAchat, VoiceOS)
│   │   ├── service/         # Background services
│   │   ├── ui/              # Presentation layer (Compose)
│   │   └── util/            # Utilities
│   ├── src/test/            # Unit tests (target: 80%+ coverage)
│   └── src/androidTest/     # Integration & UI tests
├── buildSrc/                # Build configuration and custom plugins
├── config/                  # Project-wide configuration files
├── gradle/                  # Gradle wrapper and configuration
├── project_planning/        # Planning documentation
│   ├── AVA-Project-Overview-v1.0-20251026.md  # Comprehensive overview
│   ├── Project_CodingInstructions/  # Project-specific AI instructions
│   ├── review/              # Project reviews and analyses
│   └── issues/              # Issue tracking
└── archive_docs/            # Archived documentation
```

## 6-Phase Development Roadmap

**Current Phase**: Constitution Approved - Ready for Phase 1 Specification

### Phase 1: AvaAssistant Foundation (Months 1-2) - **2-MONTH MVP**
- ONNX NLU engine with intent classification
- Teach-Ava training system (manual + auto-learning)
- KeywordFallbackClassifier with rules.json
- MLC LLM + llama.cpp for complex queries
- SQLDelight database with Room façade
- Android + Desktop working apps
- **Deliverable**: Standalone AI assistant with user training

### Phase 2: Knowledge RAG (Month 3)
- Faiss vector database integration
- ONNX embedding engine (MiniLM-e5-small)
- Document ingestion (PDF, web scraping)
- Knowledge base querying
- **Deliverable**: Document-aware AI with semantic search

### Phase 3: Constitutional AI (Month 4)
- Self-critique system with 7 principles
- >90% adherence scoring
- Response revision pipeline
- Explainability features ("why did you say that?")
- **Deliverable**: Ethical, trustworthy AI responses

### Phase 4: VOS4 Integration (Month 5)
- MagicCode plugin wrapper
- VOS4 speech recognition integration (Vosk, Vivoka, AndroidSTT)
- VOS4 GlassmorphismTheme reuse
- VOS4 AccessibilityService integration
- Dual deployment (standalone + plugin)
- **Deliverable**: Zero-duplication VOS4 ecosystem integration

### Phase 5: Smart Glasses Ecosystem (Months 6-7)
- 8+ device support (Meta Ray-Ban, Vuzix M400/Z100, RealWear HMT-1, XReal Air Pro, Rokid Max, Even Realities G1)
- VisionOS-inspired UI with glassmorphism
- WebRTC casting to glasses
- Voice-first interaction (minimal touch)
- Battery optimization (<10% per hour)
- **Deliverable**: Hands-free smart glasses AI assistant

### Phase 6: Enterprise Features (Months 8-9)
- Multi-tenant with Row Level Security (RLS)
- Supabase cloud sync (E2E encrypted, opt-in)
- WCAG 2.1 AAA accessibility compliance
- Workflow creation (PDF/web → guided steps)
- Vision integration (OCR + object recognition)
- **Deliverable**: Enterprise-ready AI platform

**Total Timeline**: 9 months (2-month MVP, 7 months to full feature parity)

## Core Features (Hybrid Architecture)

### 1. Hybrid NLU Engine
- **ONNX Intent Classification**: <50ms latency, 12MB models (DistilBERT/MobileBERT)
- **Rules-Based Fallback**: KeywordFallbackClassifier with dynamic rules.json
- **LLM Fallback**: MLC LLM + Gemma for complex queries requiring generation
- **Training Pipeline**: User corrections → rules updates (no model retraining)

### 2. Teach-Ava Training System (Unique Differentiator)
- **Manual Training**: User promotes successful utterances to training examples
- **Auto-Learning**: Automatically log successful interactions to rules
- **Immediate Feedback**: Dynamic rules.json reload (<10ms)
- **Privacy-Preserving**: All training data stored locally in SQLDelight
- **No Cloud Required**: User personalization without uploading data

### 3. Advanced Memory Systems
- **Faiss RAG**: Local vector search for documents, PDFs, manuals
- **Cognitive Memory**: Working (7±2 items), Episodic (conversations), Semantic (patterns), Procedural (skills)
- **Memory Consolidation**: Background processing during idle
- **Forget Capability**: User-controlled memory deletion

### 4. Constitutional AI & Ethics (Phase 3)
- **7 Core Principles**: Helpful, Harmless, Honest, Privacy-Respecting, Inclusive, Autonomy-Promoting, Transparent
- **Self-Critique System**: >90% adherence target
- **Response Revision**: Reject/revise violating responses
- **Explainability**: "Why did you say that?" feature

### 5. VOS4 Integration (Phase 4)
- **Speech Recognition**: Reuses VOS4 SpeechRecognitionManager (Vosk, Vivoka, AndroidSTT)
- **UI Theme**: Reuses VOS4 GlassmorphismTheme
- **Accessibility**: Reuses VOS4 AccessibilityService
- **Dual Deployment**: Standalone + MagicCode plugin modes

### 6. Smart Glasses Ecosystem (Phase 5)
- **8+ Devices**: Meta Ray-Ban, Vuzix M400/Z100, RealWear HMT-1, XReal Air Pro, Rokid Max, Even Realities G1
- **VisionOS UI**: Translucent panels with glassmorphism
- **Voice-First**: Minimal touch required
- **Battery Optimized**: <10% per hour active use
- **WebRTC Casting**: Real-time screen sharing to glasses

## Dependencies

### Core Technologies

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Kotlin | 2.0.20 | Primary language (KMP) |
| **UI Framework** | Jetpack Compose | 1.7.5 | Declarative UI |
| **Database** | Room | 2.6.1 | Local data persistence |
| **Dependency Injection** | Hilt | 2.51.1 | DI framework (100% coverage) |
| **NLU Engine** | ONNX Runtime Mobile | 1.19.0 | On-device intent classification |
| **LLM Runtime** | MLC LLM | Latest | On-device LLM inference |
| **Vector Search** | Faiss | Latest | RAG and semantic search |
| **Coroutines** | Kotlinx Coroutines | 1.9.0 | Async operations |
| **Serialization** | Kotlinx Serialization | 1.7.3 | JSON handling |
| **Testing** | Robolectric | 4.11.1 | Android environment for unit tests |

### Dependency Injection (Hilt)

**Status**: ✅ COMPLETE - 100% Hilt DI Adoption (All 9 Phases)

AVA uses **Hilt 2.51.1** for dependency injection across all ViewModels and components:

- **DatabaseModule** - Provides `AVADatabase` and 6 DAOs
- **RepositoryModule** - Provides 6 repository implementations
- **AppModule** - Provides singletons (ChatPreferences, IntentClassifier, ModelManager, ActionsManager)

**ViewModels**:
- ✅ ChatViewModel (8 dependencies)
- ✅ SettingsViewModel (1 dependency)
- ✅ TeachAvaViewModel (2 dependencies)

**Services**:
- ✅ OverlayService (@EntryPoint pattern for ActionsManager)

**Migration Guide**: See [`docs/HILT-DI-MIGRATION-GUIDE.md`](docs/HILT-DI-MIGRATION-GUIDE.md) for step-by-step ViewModel conversion instructions.

**Architecture Details**: See [`docs/ARCHITECTURE.md#dependency-injection-hilt`](docs/ARCHITECTURE.md#dependency-injection-hilt) for complete DI architecture.

### Minimum API Levels

- **Android**: API 28 (Android 9 Pie) - Target: API 34 (Android 14)
  - Note: Android 9+ provides modern features while supporting ~96% of devices
- **iOS**: iOS 15.0+
- **macOS**: macOS 12.0+
- **Windows**: Windows 10 (1809+)

---

## Documentation

### Key Documents
- **Project Overview**: `project_planning/AVA-Project-Overview-v1.0-20251026.md`
  - Executive summary, architecture, features, roadmap, success metrics
- **Old Project Docs**: `/Volumes/M Drive/Coding/AVA Old /AVA Project/AVA pre build/Documentation/`
  - Developer manuals, build guides, implementation details

### Getting Started
1. Read the [Project Overview](project_planning/AVA-Project-Overview-v1.0-20251026.md)
2. Review the old codebase for reference implementation
3. Set up development environment (Android Studio, ARCore-capable device)
4. Follow IDEACODE workflow for feature development

## Development

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 28+ (target: 34)
- ARCore-capable Android device for AR features
- VoiceOS framework (to be configured)

### Build
```bash
./gradlew build
```

### Test
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Integration tests
```

### Run
```bash
./gradlew installDebug
adb shell am start -n com.augmentalis.ava/.MainActivity
```

## Contributing

This is a proprietary project. Development follows the IDEACODE protocol:
1. `/idea.principles` - Establish governance
2. `/idea.specify` - Create feature specification
3. `/idea.clarify` - Resolve ambiguities
4. `/idea.plan` - Implementation plan
5. `/idea.tasks` - Task breakdown
6. `/idea.implement` - Execute with IDE Loop
7. `/idea.analyze` - Verify compliance

## License

**Proprietary** - All rights reserved

**Copyright**: © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar

---

**Created by Manoj Jhawar, manoj@ideahq.net**
