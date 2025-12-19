# AVA AI - Clean Architecture Overview

## Architecture Structure

AVA AI follows **Clean Architecture** principles with dependencies flowing inward:

```
platform/ (Outermost - UI & Platform-specific code)
    ↓
features/ (Middle - Feature modules & business capabilities)
    ↓
core/ (Innermost - Domain logic & shared code)
```

### Dependency Rule
**Inner layers know nothing about outer layers.** Dependencies only point inward.

- `core/` has NO dependencies on features or platform
- `features/` depends on `core/` but NOT on `platform/`
- `platform/` depends on both `features/` and `core/`

---

## Layer Structure

### Core Layer (Inner - No External Dependencies)

Located in `/Volumes/M Drive/Coding/AVA AI/core/`

#### `/core/domain/` - Business Logic & Entities
- **Purpose**: Pure business logic, domain entities, use cases
- **Dependencies**: None (pure Kotlin/Java)
- **Phase**: 1.0, Week 1-2
- **Contains**: Entities, Use Cases, Repository Interfaces, Domain Events

#### `/core/data/` - Data Implementation
- **Purpose**: Repository implementations, data sources, caching
- **Dependencies**: core/domain only
- **Phase**: 1.0, Week 1-2
- **Contains**: Repository implementations, Room database, data mappers

#### `/core/common/` - Shared Utilities
- **Purpose**: Cross-cutting utilities, extensions, constants
- **Dependencies**: Minimal (Kotlin stdlib)
- **Phase**: 1.0, Week 1-2 (ongoing)
- **Contains**: Extensions, utilities, Result types, logging

---

### Features Layer (Middle - Business Capabilities)

Located in `/Volumes/M Drive/Coding/AVA AI/features/`

Each feature is a self-contained business capability that depends only on core layer.

#### `/features/nlu/` - Natural Language Understanding
- **Purpose**: On-device NLU using ONNX + MobileBERT
- **Phase**: 1.0, Week 3-5
- **Capabilities**: Intent classification, entity extraction, semantic understanding

#### `/features/llm/` - Large Language Model Integration
- **Purpose**: Adaptive LLM Coordinator (ALC) + Cloud/Local LLMs
- **Phase**: 1.0, Week 9-12
- **Capabilities**: LLM routing, conversation management, streaming responses

#### `/features/rag/` - Retrieval Augmented Generation
- **Purpose**: Context-aware responses from user documents
- **Phase**: 1.1+ (Post-MVP)
- **Capabilities**: Document ingestion, embeddings, vector search

#### `/features/memory/` - Memory System
- **Purpose**: Persistent context-aware memory
- **Phase**: 1.2
- **Capabilities**: Conversation history, long-term memory, episodic/semantic memory

#### `/features/voice/` - Voice Input/Output
- **Purpose**: Speech-to-text and text-to-speech
- **Phase**: 1.0, Week 13-14
- **Capabilities**: STT, TTS, voice activity detection

---

### Platform Layer (Outer - UI & Platform Code)

Located in `/Volumes/M Drive/Coding/AVA AI/platform/`

Platform-specific implementations that orchestrate features into user-facing applications.

#### `/platform/android/` - Android Application
- **Purpose**: Android mobile app
- **Phase**: 1.0, Week 1-14
- **Technology**: Jetpack Compose, Android SDK, Hilt DI

#### `/platform/desktop/` - Desktop Application
- **Purpose**: Cross-platform desktop app (Windows, macOS, Linux)
- **Phase**: 2.0+ (Post-MVP)
- **Technology**: Compose Multiplatform Desktop

#### `/platform/shared-ui/` - Shared UI Components
- **Purpose**: Reusable Compose components for Android & Desktop
- **Phase**: 1.0, Week 6-8
- **Technology**: Compose Multiplatform, Material Design 3

---

## Development Phases

### Phase 1.0 - MVP (Weeks 1-14)
1. **Weeks 1-2**: Core foundation (domain, data, common)
2. **Weeks 3-5**: NLU implementation (ONNX + MobileBERT)
3. **Weeks 6-8**: UI development (shared-ui + Android)
4. **Weeks 9-12**: LLM integration (ALC + Cloud LLMs)
5. **Weeks 13-14**: Voice I/O

### Phase 1.1 - RAG Enhancement
- RAG system implementation
- Document ingestion and vector search

### Phase 1.2 - Memory & Context
- Advanced memory system
- Context management

### Phase 2.0 - Desktop Platform
- Desktop application
- Cross-platform optimization

---

## Benefits of This Architecture

### 1. Testability
- Core domain logic is pure and easily testable
- Features can be tested independently
- UI can be tested with mocked features

### 2. Modularity
- Each feature is self-contained
- Features can be developed in parallel
- Easy to add/remove features

### 3. Platform Independence
- Business logic (core + features) is platform-agnostic
- Easy to add new platforms (iOS, web, etc.)
- Maximize code reuse between Android and Desktop

### 4. Maintainability
- Clear separation of concerns
- Changes in one layer don't affect others
- Easy to understand and navigate

### 5. Scalability
- New features can be added without modifying existing code
- Team can work on different features independently
- Architecture supports growth from MVP to enterprise

---

## Dependency Injection (Hilt)

**Status:** ✅ COMPLETE - 100% Hilt DI Adoption (All 9 Phases)
**Last Updated:** November 15, 2025

### Overview

AVA has **fully migrated to Hilt dependency injection** across all ViewModels and components. All manual DI has been eliminated, resulting in improved testability, type safety, and maintainability.

### Architecture

AVA uses **Hilt** for dependency injection across all layers:

```
AvaApplication (@HiltAndroidApp)
    ↓
SingletonComponent
    ├── DatabaseModule (Phase 1)
    │   ├── AVADatabase
    │   └── DAOs (6 total: Conversation, Message, TrainExample, Memory, Decision, IntentExample)
    ├── RepositoryModule (Phase 2)
    │   └── Repositories (6 total: Conversation, Message, TrainExample, Memory, Decision, IntentExample)
    └── AppModule (Phase 2)
        ├── ChatPreferences
        ├── IntentClassifier
        ├── ModelManager
        ├── UserPreferences
        └── ActionsManager (Phase 5 - eliminates Context in ViewModels)
    ↓
ViewModels (@HiltViewModel) - All Migrated
    ├── ChatViewModel (Phase 3) - 8 dependencies
    ├── SettingsViewModel (Phase 6) - 1 dependency
    └── TeachAvaViewModel (Phase 7) - 2 dependencies
    ↓
Services (@EntryPoint)
    └── OverlayService (Phase 8) - uses ActionsManagerEntryPoint
```

### DI Modules

**Location:** `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/`

| Module | Purpose | Components | Created |
|--------|---------|------------|---------|
| **DatabaseModule** | Database + DAOs | AVADatabase, 6 DAOs (Conversation, Message, TrainExample, Memory, Decision, IntentExample) | Phase 1 |
| **RepositoryModule** | Repository implementations | 6 repositories matching DAOs | Phase 2 |
| **AppModule** | App-level singletons | ChatPreferences, UserPreferences, IntentClassifier, ModelManager, ActionsManager | Phase 2 |

### Component Scopes

| Scope | Lifetime | Use For | Example |
|-------|----------|---------|---------|
| `@Singleton` | Application | Database, Preferences, Services | `AVADatabase`, `ChatPreferences` |
| `@ViewModelScoped` | ViewModel | ViewModel-specific dependencies | Repositories injected into `@HiltViewModel` |
| `@ActivityScoped` | Activity | Activity-scoped objects | (Not currently used) |

### @EntryPoint Pattern (For Services)

Services (like `OverlayService`) cannot use `@AndroidEntryPoint` due to Android limitations. Instead, we use the **@EntryPoint pattern**:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActionsManagerEntryPoint {
    fun actionsManager(): ActionsManager
}

// Usage in OverlayService
val actionsManager = EntryPointAccessors
    .fromApplication(applicationContext, ActionsManagerEntryPoint::class.java)
    .actionsManager()
```

**Why?**: Services have unique lifecycle constraints that prevent standard field injection.

### Migration Status

**✅ ALL 9 PHASES COMPLETE (100%):**

1. ✅ **Phase 1**: DatabaseModule + DAOs (Nov 13, 2025)
2. ✅ **Phase 2**: RepositoryModule + AppModule (Nov 13, 2025)
3. ✅ **Phase 3**: ChatViewModel conversion (Nov 13, 2025)
4. ✅ **Phase 4**: MainActivity integration (Nov 13, 2025)
5. ✅ **Phase 5**: ActionsManager creation (Nov 14, 2025)
6. ✅ **Phase 6**: SettingsViewModel conversion (Nov 14, 2025)
7. ✅ **Phase 7**: TeachAvaViewModel conversion (Nov 14, 2025)
8. ✅ **Phase 8**: OverlayService @EntryPoint pattern (Nov 14, 2025)
9. ✅ **Phase 9**: Documentation completion (Nov 15, 2025)

**Total Effort:** 22 hours across 9 phases
**Test Coverage:** 100% (all 19+ tests passing)
**Breaking Changes:** None (100% functional equivalency maintained)

### Key Improvements

| Before (Manual DI) | After (Hilt) | Benefit |
|-------------------|--------------|---------|
| Context in ViewModels | ActionsManager pattern | Eliminates memory leak risk |
| Nullable repositories | Non-nullable injection | Type-safe, no defensive checks |
| Manual singleton calls | `@Inject` constructor | Compile-time validation |
| Hard to test | `@TestInstallIn` | Easy mock injection |
| Tight coupling | Dependency inversion | Better separation of concerns |

### Dependency Graph Visualization

```mermaid
graph TD
    A[AvaApplication @HiltAndroidApp] --> B[SingletonComponent]

    B --> C[DatabaseModule]
    B --> D[RepositoryModule]
    B --> E[AppModule]

    C --> F[AVADatabase]
    F --> G[6 DAOs]

    D --> H[6 Repositories]
    G --> H

    E --> I[ChatPreferences]
    E --> J[UserPreferences]
    E --> K[IntentClassifier]
    E --> L[ModelManager]
    E --> M[ActionsManager]

    N[@HiltViewModel ChatViewModel] --> H
    N --> I
    N --> K
    N --> L
    N --> M

    O[@HiltViewModel SettingsViewModel] --> J

    P[@HiltViewModel TeachAvaViewModel] --> H
    P --> I

    Q[OverlayService @EntryPoint] --> M
```

### Best Practices

1. **Always use `@HiltViewModel`** for new ViewModels
2. **Never inject Context** - use ActionsManager pattern instead
3. **Make dependencies non-nullable** - Hilt guarantees injection
4. **Use `@Singleton` for app-level objects** - Database, Preferences
5. **Use `@EntryPoint` for Services** - Cannot use `@AndroidEntryPoint`
6. **Test with `@TestInstallIn`** - Replace modules in tests

### Resources

- **Migration Guide:** `docs/HILT-DI-MIGRATION-GUIDE.md` - Step-by-step ViewModel conversion
- **Developer Manual:** `docs/Developer-Manual-Chapter32-Hilt-DI.md` - Comprehensive Hilt architecture
- **Spec:** `.ideacode/specs/SPEC-hilt-di-implementation.md` - Full 9-phase implementation spec
- **Migration Report:** `docs/HILT-DI-MIGRATION-2025-11-13.md` - Detailed migration notes

---

## Getting Started

1. **Start with Core**: Implement domain entities and use cases first
2. **Build Features**: Develop each feature independently, starting with NLU
3. **Add Platform**: Wire everything together in the Android app
4. **Use Hilt DI**: Inject dependencies instead of manual instantiation
5. **Iterate**: Add more features and platforms as needed

Each directory contains a `README.md` with detailed information about:
- Purpose and responsibilities
- Implementation phase
- Dependencies
- Architecture notes

---

## Directory Overview

```
/Volumes/M Drive/Coding/AVA AI/
│
├── core/                       # Core Layer (No external dependencies)
│   ├── domain/                 # Entities, use cases, interfaces
│   ├── data/                   # Repository implementations
│   └── common/                 # Utilities, extensions
│
├── features/                   # Features Layer (Depends on core)
│   ├── nlu/                    # ONNX NLU (Phase 1.0, Week 3-5)
│   ├── llm/                    # ALC + LLMs (Phase 1.0, Week 9-12)
│   ├── rag/                    # RAG system (Phase 1.1+)
│   ├── memory/                 # Memory system (Phase 1.2)
│   └── voice/                  # Voice I/O (Phase 1.0, Week 13-14)
│
└── platform/                   # Platform Layer (Depends on features + core)
    ├── android/                # Android app (Phase 1.0)
    ├── desktop/                # Desktop app (Phase 2.0+)
    └── shared-ui/              # Shared Compose UI (Phase 1.0)
```

---

**Architecture Version**: 1.1
**Last Updated**: November 13, 2025
**Status**: Core structure implemented, Hilt DI migration in progress (Phase 3 complete)
