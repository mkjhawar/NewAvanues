# NewAvanues Architecture

Living document describing the system architecture.

---

## Overview

NewAvanues is a platform-centric Kotlin Multiplatform monorepo containing:
- Voice assistant apps (AVA, VoiceOS)
- Browser module (WebAvanue)
- Device connectivity module (AvaConnect)
- Shared KMP libraries

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         NewAvanues                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   android/  │  │    ios/     │  │  desktop/   │            │
│  │  ┌───────┐  │  │  ┌───────┐  │  │  ┌───────┐  │            │
│  │  │voiceos│  │  │  │voiceos│  │  │  │voiceos│  │            │
│  │  ├───────┤  │  │  ├───────┤  │  │  ├───────┤  │            │
│  │  │  ava  │  │  │  │  ava  │  │  │  │  ava  │  │            │
│  │  ├───────┤  │  │  └───────┘  │  │  └───────┘  │            │
│  │  │avanues│  │  └─────────────┘  └─────────────┘            │
│  │  ├───────┤  │                                               │
│  │  │connect│  │                                               │
│  │  └───────┘  │                                               │
│  └─────────────┘                                               │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                      Modules/                            │  │
│  │  ┌─────────────────┐    ┌─────────────────┐             │  │
│  │  │   WebAvanue     │    │   AvaConnect    │             │  │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │             │  │
│  │  │  │ universal │  │    │  │ universal │  │             │  │
│  │  │  └───────────┘  │    │  └───────────┘  │             │  │
│  │  └─────────────────┘    └─────────────────┘             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                      Common/                             │  │
│  │  ┌──────────┐ ┌───────┐ ┌─────┐ ┌────┐ ┌───────┐        │  │
│  │  │ Database │ │ Voice │ │ NLU │ │ UI │ │ Utils │        │  │
│  │  └──────────┘ └───────┘ └─────┘ └────┘ └───────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Layer Architecture

| Layer | Components | Responsibility |
|-------|------------|----------------|
| Apps | android/, ios/, desktop/ | Platform-specific UI, entry points |
| Modules | Modules/ | Cross-platform feature modules |
| Common | Common/ | Shared business logic, data, utilities |

---

## Common Libraries

| Library | Responsibility |
|---------|----------------|
| Database | SQLDelight database layer, repositories |
| Voice | Voice recognition, audio processing |
| NLU | Natural language understanding, intent parsing |
| UI | MagicUI components, theming |
| Utils | Logging, UUID, shared utilities |

---

## Data Flow

```
User Input
    │
    ▼
┌─────────────┐
│  Platform   │  (android/ios/desktop app)
│     UI      │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  ViewModel  │  (Common/UI or Module/universal)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Use Case   │  (Common/{domain})
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │  (Common/Database)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  SQLDelight │  (Common/Database)
└─────────────┘
```

---

## Key Integrations

| Integration | Components | Description |
|-------------|------------|-------------|
| NLU → VoiceOS | NLU, Voice | Intent classification feeds command execution |
| Database → All | Database | Unified data layer across all apps |
| UI → All | UI | MagicUI components shared across platforms |

---

## Technology Stack

| Area | Technology |
|------|------------|
| Language | Kotlin |
| Framework | Kotlin Multiplatform |
| Database | SQLDelight |
| UI (Android) | Jetpack Compose + MagicUI |
| UI (iOS) | SwiftUI + MagicUI |
| UI (Desktop) | Compose Desktop + MagicUI |
| Build | Gradle |

---

*Version: 1 | Updated: 2025-12-03*
