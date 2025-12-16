# NewAvanues - Project Context

**Version:** 12.0.0 | **Updated:** 2025-12-15

---

## Project Overview

NewAvanues is a voice-first, AI-assisted platform providing seamless interaction across Android, iOS, and web platforms through natural language understanding and accessibility services.

### Vision

Enable users to interact with technology naturally through voice, making digital experiences accessible and intuitive for everyone.

---

## Core Modules

| Module | Purpose | Platform | Status |
|--------|---------|----------|--------|
| **VoiceOS** | Android accessibility service for voice-first interaction | Android | ✅ Active |
| **AVA** | Cross-platform AI assistant | Android, iOS, Web | ✅ Active |
| **WebAvanue** | Web management platform | Web | ✅ Active |
| **Cockpit** | Centralized module management | Cross-platform | ✅ Active |
| **NLU** | Natural language understanding service | Service | ✅ Active |
| **Common** | Shared libraries and utilities | All | ✅ Active |

---

## Key Technologies

- **Backend:** Kotlin Multiplatform, Python (ML)
- **Android:** Kotlin, Jetpack Compose, SQLDelight
- **iOS:** Swift, SwiftUI (planned)
- **Web:** React, TypeScript, Tailwind CSS
- **Database:** SQLDelight (cross-platform)
- **ML/NLU:** TensorFlow/PyTorch, Python

---

## Project Structure

```
NewAvanues/
├── Modules/               # Module implementations
│   ├── VoiceOS/          # Android accessibility
│   ├── AVA/              # AI assistant
│   ├── WebAvanue/        # Web platform
│   ├── Cockpit/          # Management
│   └── NLU/              # NLU service
├── Common/               # Shared libraries
├── Docs/                 # Living documentation
├── docs/project-info/    # Central reference (this directory)
└── .ideacode/           # IDEACODE configuration
```

---

## Development Model

- **Monorepo:** All modules in single repository
- **Branch Strategy:** Module-specific development branches
- **Configuration:** IDC format (60-80% smaller than YAML)
- **Documentation:** Living docs updated with code changes

---

## Quality Standards

| Metric | Target | Current |
|--------|--------|---------|
| Test Coverage | 90%+ | In Progress |
| API Documentation | 100% | In Progress |
| IPC Coverage | 100% | In Progress |

---

## Related Documentation

- [Architecture](ARCHITECTURE.md)
- [API Contracts](API-CONTRACTS.md)
- [IPC Methods](IPC-METHODS.md)
- [Intent Registry](INTENT-REGISTRY.md)
- [Core Architecture](../Docs/NewAvanues/LivingDocs/LD-NAV-Core-Architecture-V1.md)

---

**Maintained By:** NewAvanues Team
