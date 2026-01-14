# VoiceOSCoreNG Implementation Plan - Production Readiness

**Date:** 2026-01-06
**Module:** Modules/VoiceOSCoreNG
**Branch:** VoiceOSCoreNG
**Based on:** Spec Review VoiceOSCoreNG-Spec-Review-60106-V1.md

---

## Overview

This plan addresses the **35 TODOs** and **6 major gaps** identified in the spec review to bring VoiceOSCoreNG to production readiness.

| Phase | Focus | Tasks | Priority |
|-------|-------|-------|----------|
| 1 | Database Integration | 4 | P0 |
| 2 | Android Speech Engines | 5 | P1 |
| 3 | SpeechEngineManager | 4 | P1 |
| 4 | iOS Platform | 8 | P2 |
| 5 | Desktop Platform | 6 | P2 |
| **Total** | | **27** | |

---

## Phase 1: Database Integration (P0)

**Goal:** Connect VoiceOSCoreNG to core/database SQLDelight repositories

### Tasks

| ID | Task | File(s) | Effort |
|----|------|---------|--------|
| 1.1 | Add core/database dependency to build.gradle.kts | `build.gradle.kts` | S |
| 1.2 | Create DatabaseAdapter interface in commonMain | `data/IDatabaseAdapter.kt` | M |
| 1.3 | Implement Android DatabaseAdapter | `androidMain/data/AndroidDatabaseAdapter.kt` | M |
| 1.4 | Integrate command persistence in CommandRegistry | `common/CommandRegistry.kt` | M |

**Deliverable:** Commands persist across sessions, screen contexts cached

---

## Phase 2: Android Speech Engines (P1)

**Goal:** Implement real speech recognition engines for Android

### Tasks

| ID | Task | File(s) | Effort |
|----|------|---------|--------|
| 2.1 | Implement VoskEngine using vosk-android library | `androidMain/features/VoskEngine.kt` | L |
| 2.2 | Add Vosk model downloading/management | `androidMain/features/VoskModelManager.kt` | M |
| 2.3 | Implement GoogleEngine using Cloud Speech API | `androidMain/features/GoogleEngine.kt` | L |
| 2.4 | Implement AzureEngine using Cognitive Services | `androidMain/features/AzureEngine.kt` | L |
| 2.5 | Create engine configuration UI bindings | `androidMain/features/SpeechEngineConfig.kt` | M |

**Deliverable:** Three speech engines available on Android (offline/online options)

---

## Phase 3: SpeechEngineManager (P1)

**Goal:** Create coordinator for managing multiple speech engines

### Tasks

| ID | Task | File(s) | Effort |
|----|------|---------|--------|
| 3.1 | Create ISpeechEngineManager interface | `commonMain/speech/ISpeechEngineManager.kt` | M |
| 3.2 | Implement SpeechEngineManager with StateFlow/SharedFlow | `commonMain/speech/SpeechEngineManager.kt` | L |
| 3.3 | Create platform-specific SpeechEngineManagerFactory | `*Main/speech/SpeechEngineManagerFactory.kt` | M |
| 3.4 | Integrate with CommandDispatcher | `commonMain/handlers/CommandDispatcher.kt` | M |

**Deliverable:** Coordinated engine lifecycle, state observation, event emission

---

## Phase 4: iOS Platform (P2)

**Goal:** Replace iOS stubs with UIAccessibility implementations

### Tasks

| ID | Task | File(s) | Effort |
|----|------|---------|--------|
| 4.1 | Create Swift interop bridge module | `iosMain/bridge/` | L |
| 4.2 | Implement IOSActionExecutor tap/focus/scroll | `iosMain/handlers/IOSActionExecutor.kt` | L |
| 4.3 | Implement IOSActionExecutor system actions | `iosMain/handlers/IOSActionExecutor.kt` | M |
| 4.4 | Implement IOSActionExecutor media controls | `iosMain/handlers/IOSActionExecutor.kt` | M |
| 4.5 | Replace iOS StubExecutors with real implementations | `iosMain/handlers/StubExecutors.kt` | L |
| 4.6 | Implement Apple Speech.framework adapter | `iosMain/features/AppleSpeechEngine.kt` | L |
| 4.7 | Implement PlatformExtractor for iOS accessibility tree | `iosMain/functions/PlatformExtractor.kt` | M |
| 4.8 | Create iOS test harness | `iosTest/` | M |

**Deliverable:** iOS voice control functional with Apple Speech

---

## Phase 5: Desktop Platform (P2)

**Goal:** Replace Desktop stubs with AWT Robot implementations

### Tasks

| ID | Task | File(s) | Effort |
|----|------|---------|--------|
| 5.1 | Implement DesktopActionExecutor using AWT Robot | `desktopMain/handlers/DesktopActionExecutor.kt` | M |
| 5.2 | Replace Desktop StubExecutors with Robot wrappers | `desktopMain/handlers/StubExecutors.kt` | M |
| 5.3 | Implement Vosk JNI integration | `desktopMain/features/VoskDesktopEngine.kt` | L |
| 5.4 | Add platform detection (Windows/macOS/Linux) | `desktopMain/functions/Platform.kt` | S |
| 5.5 | Implement PlatformExtractor (accessibility APIs) | `desktopMain/functions/PlatformExtractor.kt` | L |
| 5.6 | Create desktop test harness | `desktopTest/` | M |

**Deliverable:** Desktop voice control functional with Vosk

---

## Effort Legend

| Size | Description | Estimate |
|------|-------------|----------|
| S | Small - single file, straightforward | ~2 hours |
| M | Medium - multiple files, some complexity | ~4 hours |
| L | Large - significant work, external APIs | ~8+ hours |

---

## Dependencies

```
Phase 1 (Database)
    │
    └──► Phase 3 (SpeechEngineManager)
              │
    ┌─────────┴─────────┐
    │                   │
    ▼                   ▼
Phase 2 (Android)   Phase 4 (iOS)
                        │
                        ▼
                    Phase 5 (Desktop)
```

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Vosk library compatibility | Medium | High | Test early, have fallback to Android STT |
| iOS Swift interop complexity | High | Medium | Start with simple actions first |
| Desktop cross-platform issues | Medium | Medium | Abstract platform-specific code |
| Speech API rate limits | Low | Low | Use offline engines as primary |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| TODO count | 0 remaining |
| Test pass rate | 100% |
| Platform coverage | Android ✅, iOS ⚠️, Desktop ⚠️ |
| Speech engines | ≥2 per platform |
| Command execution | End-to-end working |

---

## Next Steps

1. Run `/i.implement` with Phase 1 tasks
2. Create branch per phase if needed
3. Review and merge incrementally
4. Update demo app for testing

---

**Created:** 2026-01-06 by /i.plan .tasks
