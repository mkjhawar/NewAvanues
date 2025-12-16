# NewAvanues - System Architecture

**Version:** 12.0.0
**Updated:** 2025-12-15

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    NewAvanues Monorepo                   │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────┐  ┌──────┐  ┌──────────┐  ┌────────┐        │
│  │ VoiceOS │  │ AVA  │  │WebAvanue │  │Cockpit │        │
│  └────┬────┘  └───┬──┘  └────┬─────┘  └───┬────┘        │
│       │           │          │            │              │
│       └───────────┴──────────┴────────────┘              │
│                      │                                    │
│           ┌──────────┴──────────┐                        │
│           │                     │                        │
│        ┌──▼──┐              ┌───▼───┐                    │
│        │ NLU │              │Common │                    │
│        └─────┘              └───────┘                    │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

## Module Descriptions

### VoiceOS
**Purpose:** Android accessibility service for voice-first system control
**Key Features:**
- System-wide accessibility events
- Voice command processing
- Gesture recognition
- Context-aware automation

**Technology Stack:**
- Kotlin
- Jetpack Compose
- SQLDelight (database)
- Material Design 3

### AVA
**Purpose:** AI assistant platform
**Key Features:**
- Conversational AI
- Task automation
- Context management
- Multi-modal interaction

**Technology Stack:**
- Kotlin Multiplatform
- Coroutines & Flow
- SQLDelight

### WebAvanue
**Purpose:** Voice-controlled web browser and platform
**Key Features:**
- Voice navigation
- Content extraction
- Privacy protection (ad/tracker blocking)
- Cross-platform web rendering

**Technology Stack:**
- Kotlin Multiplatform
- Web technologies
- Platform-specific WebView integration

### Cockpit
**Purpose:** System management and monitoring dashboard
**Key Features:**
- Module management
- System health monitoring
- Configuration management
- Performance metrics

**Technology Stack:**
- Kotlin
- SQLDelight
- IDC configuration format

### NLU
**Purpose:** Natural language understanding and intent recognition
**Key Features:**
- Intent classification
- Entity extraction
- Context tracking
- Model training pipeline

**Technology Stack:**
- Python (training)
- Kotlin (runtime)
- TensorFlow/PyTorch
- Cross-platform model deployment

### Common
**Purpose:** Shared libraries and utilities
**Key Features:**
- Core utilities
- Shared data models
- Cross-module interfaces
- Base components

**Technology Stack:**
- Kotlin Multiplatform
- Platform-agnostic code

## Data Flow

### Voice Command Processing

```
User Voice Input
      │
      ▼
  VoiceOS (Capture)
      │
      ▼
  NLU (Process Intent)
      │
      ▼
  AVA (Execute Action)
      │
      ▼
  Target Module (VoiceOS/WebAvanue/Cockpit)
      │
      ▼
  User Feedback
```

## Communication Patterns

### Inter-Process Communication (IPC)
- Android Intents (VoiceOS ↔ AVA)
- Content Providers (Database sharing)
- Broadcast Receivers (System events)

See: `NAV-Docs-IPCMethods-5121522-V1.md`

### API Contracts
- RESTful APIs for web services
- gRPC for high-performance module communication
- Message queues for async operations

See: `NAV-Docs-APIContracts-5121522-V1.md`

## Database Architecture

**Database Technology:** SQLDelight (NOT Room)

**Schema Organization:**
- Module-specific databases
- Shared schemas in Common
- Cross-module views
- Migration management

**Repositories:**
```kotlin
interface IRepository<T> {
    suspend fun insert(item: T): Long
    suspend fun update(item: T)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): T?
    fun observeAll(): Flow<List<T>>
}
```

## Build System

**Tool:** Gradle with Kotlin DSL

**Module Structure:**
```
NewAvanues/
├── Modules/
│   ├── VoiceOS/
│   │   ├── apps/         # Android apps
│   │   ├── core/         # Core logic
│   │   └── libraries/    # Module libraries
│   ├── AVA/
│   ├── WebAvanue/
│   ├── Cockpit/
│   └── NLU/
└── Common/               # Shared code
```

## Quality Gates

| Metric | Requirement |
|--------|-------------|
| Test Coverage | 90%+ |
| IPC Coverage | 100% |
| API Documentation | 100% |
| Intent Registration | 100% |

## References

- **Living Docs:** `Docs/{Module}/LivingDocs/LD-*-V1.md`
- **Registries:** `.ideacode/registries/`
- **Module Docs:** `Modules/{Module}/.claude/CLAUDE.md`

---

For detailed API specifications and IPC methods, see related documentation files.
