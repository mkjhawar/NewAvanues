# NewAvanues - Project Context

**Repository:** NewAvanues
**Type:** Monorepo
**Version:** 12.0.0
**Updated:** 2025-12-15

---

## Overview

NewAvanues is a unified monorepo containing multiple integrated platforms for voice-first AI assistance, accessibility, and web interaction.

## Key Components

| Module | Purpose | Technology |
|--------|---------|------------|
| VoiceOS | Voice-first Android accessibility service | Kotlin, Jetpack Compose, SQLDelight |
| AVA | AI assistant platform | Kotlin Multiplatform |
| WebAvanue | Voice-controlled web platform | KMP, Web technologies |
| Cockpit | System management dashboard | Kotlin, SQLDelight |
| NLU | Natural language understanding | Python/Kotlin, ML models |
| Common | Shared libraries and utilities | Kotlin Multiplatform |

## Architecture Principles

- **Voice-First:** All interfaces prioritize voice interaction
- **Cross-Platform:** Kotlin Multiplatform for code sharing
- **Modular:** Independent modules with clear boundaries
- **Database:** SQLDelight for all persistence (NOT Room)
- **Testing:** 90%+ coverage requirement

## Module Dependencies

See: `.ideacode/registries/CROSS-MODULE-DEPENDENCIES.md`

## Living Documentation

All modules maintain living docs in `Docs/{Module}/LivingDocs/`:
- Architecture guides
- API contracts
- Module state tracking
- Development guidelines

## References

- **Architecture:** NAV-Docs-Architecture-5121522-V1.md
- **API Contracts:** NAV-Docs-APIContracts-5121522-V1.md
- **IPC Methods:** NAV-Docs-IPCMethods-5121522-V1.md
- **Intent Registry:** NAV-Docs-IntentRegistry-5121522-V1.md

---

For detailed technical specifications, see individual module documentation in `Docs/{Module}/`.
