# VoiceOS4 Developer Manual - Index

**Version:** 4.1.1
**Last Updated:** 2025-11-07
**Framework:** VOS4 with IDEACODE v5.3

## 🔥 Latest Update (v4.1.1 - 2025-11-07)

**Voice Commands & Testing Documentation Complete**
- Added 20 voice commands for database interaction (v4.1.1 feature)
- Comprehensive [VoiceOS Testing Manual](../testing/VoiceOS-Testing-Manual.md) created
- Updated: Chapter 32 (Testing Strategy)
- See: [Database Voice Commands Spec](../planning/Database-Voice-Commands-Specification.md)

**Previous Update (v4.1.0):**
- Database Consolidation (3 → 1 database)
- See: [ADR-005](../planning/architecture/decisions/ADR-005-Database-Consolidation-Activation-2511070830.md)

---

## Chapters

### Chapter 1: Project Overview & Architecture
- System design and component relationships
- Integration points with Android system
- Performance and scalability considerations

### Chapter 2: Foundation & Dependencies
- Room database architecture **(v4.1: Unified VoiceOSAppDatabase)**
- Hilt dependency injection setup
- Coroutines and async patterns
- External library integrations

### Chapter 3: VoiceOSCore Module ✅
**Location:** `/Volumes/M-Drive/Coding/Warp/vos4/docs/developer-manual/03-VoiceOSCore-Module.md`
**Status:** Complete (48 pages)

**Sections:**
1. **Overview (5 pages)** - Module purpose, architecture, design principles
2. **Accessibility Service (10 pages)** - VoiceOSService lifecycle, event processing, command execution tiers
3. **UI Scraping Engine (10 pages)** - Window scraping, hash deduplication, interaction tracking
4. **Database Layer (8 pages)** - VoiceOSAppDatabase (v4.1 unified), migrations, entity relationships
5. **Voice Command Processing (5 pages)** - Command pipeline, confidence filtering, context creation
6. **Cursor & Overlays (5 pages)** - VoiceCursor integration, gesture types, overlay system
7. **Screen Context Inference (5 pages)** - Screen type detection, form relationships, semantic understanding

**Key Classes Covered:**
- VoiceOSService.kt (1522 lines) - Core accessibility service
- AccessibilityScrapingIntegration.kt (1763 lines) - UI scraping pipeline
- VoiceOSAppDatabase.kt (527 lines) - Room database implementation
- UIScrapingEngine.kt (advanced extraction)
- CursorGestureHandler.kt (gesture dispatch)

**Code Examples:** 38 examples totaling ~2500 lines

### Chapter 4: LearnApp Integration (Planned)
- Automatic app exploration
- Consent dialogs
- Screen discovery
- Learning progress tracking

### Chapter 5: Command System (Planned)
- CommandManager integration
- Handler architecture
- Dynamic command generation
- Multi-language support

### Chapter 6: Voice & Speech (Planned)
- Speech recognition setup
- Confidence scoring
- Language detection
- VAD and filtering

### Chapter 7: Testing & Quality (Planned)
- Unit test patterns
- Integration test setup
- Performance benchmarks
- Debugging utilities

### Chapter 8: Deployment & Operations (Planned)
- APK building
- Service configuration
- Logging and monitoring
- Crash reporting

---

## Document Statistics

| Metric | Count |
|--------|-------|
| **Total Pages** | 48 |
| **Code Examples** | 38 |
| **Diagrams** | 3 |
| **Code Lines Included** | ~2,500 |
| **File Size** | 64 KB |

---

## Quick Navigation

### By Component
- **VoiceOSService** → Chapter 3, Part 2
- **Accessibility Scraping** → Chapter 3, Part 3
- **Database Layer** → Chapter 3, Part 4
- **Voice Commands** → Chapter 3, Part 5
- **Cursor Control** → Chapter 3, Part 6
- **Screen Inference** → Chapter 3, Part 7

### By Task
- **Understand architecture** → Chapter 1 + Chapter 3, Part 1
- **Debug event processing** → Chapter 3, Part 2.2
- **Modify scraping logic** → Chapter 3, Part 3.2
- **Add new commands** → Chapter 3, Part 5
- **Implement new gesture** → Chapter 3, Part 6

### By Technology
- **Android Accessibility** → Chapter 3, Parts 2 & 3
- **Room Database** → Chapter 3, Part 4
- **Coroutines** → Chapter 3, Part 2.3-2.5
- **Hilt DI** → Chapter 3, Part 1.4
- **Gesture Handling** → Chapter 3, Part 6

---

## Version History

| Version | Date | Status | Content |
|---------|------|--------|---------|
| **4.1.1** | **2025-11-07** | **Complete** | **Voice Commands (20 commands), Testing Manual, Chapter 32 update** |
| 4.1.0 | 2025-11-07 | Complete | Database Consolidation (ADR-005), Chapter 16/17/Appendix B updates |
| 1.0 | 2025-11-03 | Complete | Chapter 3: VoiceOSCore Module (48 pages) |
| 0.1 | TBD | Planned | Chapter 1-2: Foundation |

---

## How to Use This Manual

### For New Contributors
1. Start with **Chapter 1** (overview)
2. Read **Chapter 3, Part 1** (architecture)
3. Study the relevant component (Part 2-7)
4. Review code examples
5. Check test files for patterns

### For Debugging
1. Identify the component (Chapter 3)
2. Find the relevant part
3. Read event/execution flow diagrams
4. Check logging patterns
5. Follow examples

### For Feature Development
1. Understand current architecture (Chapter 3, Part 1)
2. Identify integration points
3. Find similar examples
4. Follow design principles
5. Maintain code style

---

**Last Updated:** 2025-11-07
**Framework:** VOS4 + IDEACODE v5.3
**Status:** In Progress (v4.1.0 - Database Consolidation Complete)
