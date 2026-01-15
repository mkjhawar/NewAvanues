# Implementation Plan: Module Restructuring

## Overview
**Objective:** Restructure AVA modules into organized Modules/AI/, Modules/Voice/, Modules/Actions/ hierarchy

| Attribute | Value |
|-----------|-------|
| Platforms | Android, iOS, Desktop, JS (all KMP) |
| Swarm Recommended | **YES** (20+ tasks, parallel moves possible) |
| Estimated Tasks | 24 |
| Risk Level | Low (package names unchanged) |

## Current → Target Structure

```
CURRENT                          TARGET
─────────────────────────────    ─────────────────────────────
Modules/                         Modules/
├── NLU/                         ├── AI/
├── RAG/                         │   ├── NLU/
├── LLM/                         │   ├── RAG/
├── AVA/                         │   ├── LLM/
│   ├── Chat/                    │   ├── Memory/  (from AVA/memory)
│   ├── memory/                  │   ├── Chat/    (from AVA/Chat)
│   ├── Teach/                   │   └── Teach/   (from AVA/Teach)
│   ├── WakeWord/                │
│   ├── Actions/                 ├── Voice/
│   ├── Overlay/ (DELETE)        │   ├── Core/    (VoiceOSCoreNG)
│   ├── voice/ (DELETE)          │   └── WakeWord/
│   └── core/                    │
├── VoiceOSCoreNG/               ├── Actions/     (from AVA/Actions, KMP)
└── ...                          │
                                 ├── AVA/
                                 │   └── core/    (foundation only)
                                 └── ...
```

## Phases

### Phase 1: Infrastructure Setup
**Duration:** 5 min | **Parallelizable:** No

| Task | Description |
|------|-------------|
| 1.1 | Create `Modules/AI/` directory |
| 1.2 | Create `Modules/Voice/` directory |
| 1.3 | Commit cleanup of redundant NLU/RAG ava paths |

### Phase 2: AI Module Migration (Safe Moves)
**Duration:** 20 min | **Parallelizable:** Yes (Swarm)

| Task | Description | Risk |
|------|-------------|------|
| 2.1 | Move `Modules/NLU/` → `Modules/AI/NLU/` | Low |
| 2.2 | Move `Modules/RAG/` → `Modules/AI/RAG/` | Low |
| 2.3 | Move `Modules/LLM/` → `Modules/AI/LLM/` | Low |
| 2.4 | Create `Modules/AI/Memory/` from `AVA/memory/` | Low |
| 2.5 | Update `settings.gradle.kts` for AI modules | Low |
| 2.6 | Update dependent build.gradle.kts files | Low |
| 2.7 | Build verification: `./gradlew :Modules:AI:NLU:compileKotlinMetadata` | - |

### Phase 3: Voice Module Migration
**Duration:** 15 min | **Parallelizable:** Yes (Swarm)

| Task | Description | Risk |
|------|-------------|------|
| 3.1 | Move `Modules/VoiceOSCoreNG/` → `Modules/Voice/Core/` | Low |
| 3.2 | Move `AVA/WakeWord/` → `Modules/Voice/WakeWord/` | Low |
| 3.3 | Update `settings.gradle.kts` for Voice modules | Low |
| 3.4 | Update dependent build.gradle.kts files | Low |
| 3.5 | Build verification | - |

### Phase 4: Actions Module (KMP Conversion)
**Duration:** 30 min | **Parallelizable:** Partial

| Task | Description | Risk |
|------|-------------|------|
| 4.1 | Create `Modules/Actions/` with KMP structure | Medium |
| 4.2 | Move pure-KMP handlers from `AVA/Actions/` to `commonMain` | Medium |
| 4.3 | Move Android-specific handlers to `androidMain` | Low |
| 4.4 | Create expect/actual for platform handlers | Medium |
| 4.5 | Update settings.gradle.kts | Low |
| 4.6 | Build verification | - |

### Phase 5: Chat Module (KMP Enhancement)
**Duration:** 25 min | **Parallelizable:** Partial

| Task | Description | Risk |
|------|-------------|------|
| 5.1 | Move `AVA/Chat/` → `Modules/AI/Chat/` | Medium |
| 5.2 | Ensure KMP structure (commonMain has interfaces) | Low |
| 5.3 | Move Android UI to androidMain | Low |
| 5.4 | Update dependencies to use new AI module paths | Medium |
| 5.5 | Build verification | - |

### Phase 6: Teach Module Migration
**Duration:** 10 min | **Parallelizable:** Yes

| Task | Description | Risk |
|------|-------------|------|
| 6.1 | Move `AVA/Teach/` → `Modules/AI/Teach/` | Low |
| 6.2 | Update settings.gradle.kts | Low |
| 6.3 | Build verification | - |

### Phase 7: Cleanup
**Duration:** 10 min | **Parallelizable:** No

| Task | Description | Risk |
|------|-------------|------|
| 7.1 | Delete `AVA/Overlay/` (duplicate of Voice/Core overlay) | Low |
| 7.2 | Delete `AVA/voice/` (empty) | Zero |
| 7.3 | Delete `AVA/memory/` (moved to AI/Memory) | Low |
| 7.4 | Delete `AVA/WakeWord/` (moved to Voice/WakeWord) | Low |
| 7.5 | Delete `AVA/Actions/` (moved to Modules/Actions) | Low |
| 7.6 | Delete `AVA/Chat/` (moved to AI/Chat) | Low |
| 7.7 | Delete `AVA/Teach/` (moved to AI/Teach) | Low |
| 7.8 | Verify AVA/core remains as foundation | - |
| 7.9 | Final full build verification | - |
| 7.10 | Git commit all changes | - |

## Time Estimates

| Execution Mode | Duration | Notes |
|----------------|----------|-------|
| Sequential | ~2 hours | One task at a time |
| Parallel (Swarm) | ~45 min | Phases 2-6 parallelized |
| **Savings** | 1h 15m (63%) | Swarm recommended |

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Build breaks | Package names unchanged, only paths change |
| Git history loss | Use `git mv` to preserve history |
| Dependency errors | Update settings.gradle.kts before build.gradle.kts |
| Runtime errors | No reflection on paths, only packages |

## Verification Checkpoints

After each phase:
```bash
./gradlew compileKotlinMetadata --no-daemon
```

Final verification:
```bash
./gradlew build --no-daemon
```

## Rollback Plan

If issues occur:
```bash
git checkout HEAD -- Modules/ settings.gradle.kts
```

---
**Created:** 2026-01-16
**Author:** Claude (IDEACODE)
**Status:** Ready for execution
