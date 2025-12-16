# NewAvanues - System Architecture

**Version:** 12.0.0 | **Updated:** 2025-12-15

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     User Interfaces                          │
│   ┌──────────┐      ┌──────────┐       ┌──────────┐        │
│   │ VoiceOS  │      │   AVA    │       │WebAvanue │        │
│   │(Android) │      │  (Multi) │       │  (Web)   │        │
│   └─────┬────┘      └─────┬────┘       └─────┬────┘        │
└─────────┼─────────────────┼──────────────────┼─────────────┘
          │                 │                  │
          │                 │                  │
┌─────────▼─────────────────▼──────────────────▼─────────────┐
│                    Business Logic Layer                      │
│   ┌──────────────┐    ┌──────────────┐   ┌──────────────┐ │
│   │     NLU      │    │   Cockpit    │   │    Common    │ │
│   │   Service    │    │ (Management) │   │ (Libraries)  │ │
│   └──────────────┘    └──────────────┘   └──────────────┘ │
└──────────────────────────────────────────────────────────────┘
          │                                          │
          │                                          │
┌─────────▼──────────────────────────────────────────▼─────────┐
│                      Data Layer                               │
│        ┌──────────────┐              ┌──────────────┐        │
│        │  SQLDelight  │              │  ML Models   │        │
│        │  (Database)  │              │  (Trained)   │        │
│        └──────────────┘              └──────────────┘        │
└──────────────────────────────────────────────────────────────┘
```

---

## Component Interactions

### Voice Flow
```
User Voice → VoiceOS → NLU → Intent/Entity → AVA → Response → VoiceOS → Audio
```

### Management Flow
```
Admin → WebAvanue → Cockpit → Module APIs → Configuration Updates
```

---

## Technology Stack by Layer

### Presentation Layer
- **Android:** Jetpack Compose + Material Design 3
- **iOS:** SwiftUI (planned)
- **Web:** React + TypeScript + Tailwind CSS

### Business Logic Layer
- **Kotlin Multiplatform:** Shared business logic
- **Python:** ML/NLU processing
- **Kotlin:** Android-specific logic

### Data Layer
- **SQLDelight:** Cross-platform database (Android + KMP)
- **ML Models:** TensorFlow/PyTorch models
- **Configuration:** IDC format files

---

## Communication Patterns

### Synchronous
- REST APIs (WebAvanue ↔ Backend)
- Direct function calls (within modules)

### Asynchronous
- Android Intents (VoiceOS ↔ AVA)
- WebSockets (real-time updates)
- Kotlin Coroutines/Flow

---

## Scalability Considerations

| Aspect | Approach |
|--------|----------|
| Module Independence | Each module can scale independently |
| Data Storage | SQLDelight supports multi-platform persistence |
| NLU Processing | Stateless service, horizontally scalable |
| Configuration | IDC format reduces memory footprint |

---

## Security Architecture

- **Authentication:** JWT-based (WebAvanue)
- **Data Encryption:** At-rest encryption for sensitive data
- **IPC Security:** Intent permissions on Android
- **API Security:** Token-based authentication

---

## For More Details

See: [Core Architecture Living Doc](../Docs/NewAvanues/LivingDocs/LD-NAV-Core-Architecture-V1.md)

---

**Maintained By:** NewAvanues Team
