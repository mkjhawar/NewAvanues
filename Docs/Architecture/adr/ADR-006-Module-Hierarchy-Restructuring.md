# ADR-006: Module Hierarchy Restructuring

**Date:** 2026-01-16
**Status:** Implemented
**Decision Makers:** Architecture Team
**Architectural Significance:** High

## Summary

Restructure the monorepo module hierarchy from flat Modules/ layout to organized category-based hierarchy: AI/, Voice/, Actions/. This consolidation reduces redundancy, clarifies ownership, and enables better cross-platform code sharing via KMP.

## Context

### Problem Statement
The previous module structure had:
- Scattered AI modules (NLU, RAG, LLM, memory) at different levels
- Overlapping overlay implementations in AVA/Overlay and VoiceOSCoreNG
- AVA submodules that should be shared KMP libraries
- Redundant file paths (e.g., `ava/features/nlu` and `modules/nlu`)

### Background
- AVA started as Android-only, modules grew organically
- VoiceOSCoreNG was created as KMP refactor with overlay abstractions
- No clear separation between AI, Voice, and UI concerns

### Scope
- Module directory restructuring
- settings.gradle.kts updates
- build.gradle.kts dependency path updates
- Package names remain unchanged (no code breaks)

## Decision

**Restructure modules into category-based hierarchy with clear ownership.**

### Core Components

#### Component 1: AI Modules (`Modules/AI/`)
| Module | Purpose | Source |
|--------|---------|--------|
| NLU | Natural language understanding | Moved from Modules/NLU |
| RAG | Retrieval-augmented generation | Moved from Modules/RAG |
| LLM | Language model providers | Moved from Modules/LLM |
| Memory | Short/long-term AI memory | Moved from AVA/memory |
| Chat | AI chat interface | Moved from AVA/Chat |
| Teach | AI teaching/training | Moved from AVA/Teach |

#### Component 2: Voice Modules (`Modules/Voice/`)
| Module | Purpose | Source |
|--------|---------|--------|
| Core | VoiceOSCoreNG - KMP voice control | Moved from Modules/VoiceOSCoreNG |
| WakeWord | Wake word detection | Moved from AVA/WakeWord |

#### Component 3: Actions Module (`Modules/Actions/`)
| Module | Purpose | Source |
|--------|---------|--------|
| Actions | Intent handlers, action execution | Moved from AVA/Actions |

#### Component 4: Overlay Organization
| Type | Location | Rationale |
|------|----------|-----------|
| VoiceOS-specific overlays | Voice/Core/features/ | Tightly coupled to voice command processing |
| Generic UI components | AvaUI:Floating | Reusable across all apps (VoiceOrb, GlassMorphicPanel) |

#### Component 5: VoiceOSCoreNG Subfolder Exception
Voice/Core is allowed to use subfolders (`features/`, `cursor/`, etc.) due to the large quantity of files. This is an **exception** to the flat package structure rule.

### Implementation Approach

1. **Phase 1:** Create AI/ and Voice/ directories
2. **Phase 2:** Move AI modules (NLU, RAG, LLM, Memory)
3. **Phase 3:** Move Voice modules (Core, WakeWord)
4. **Phase 4:** Move Actions module
5. **Phase 5:** Move Chat and Teach to AI/
6. **Phase 6:** Cleanup duplicate overlay/ folder
7. **Phase 7:** Final build verification

**Tech Debt - AVA/Overlay UI Migration:**
Generic UI components (VoiceOrb, GlassMorphicPanel, SuggestionChips) should eventually move to AvaUI:Floating, but have dependencies on overlay-specific code. Deferred to future cleanup.

## Alternatives Considered

### Alternative 1: Keep Flat Structure
**Rationale:** Less disruption
**Benefits:** No migration needed
**Drawbacks:** Continued confusion, no clear ownership
**Rejected Because:** Technical debt compounds over time

### Alternative 2: Per-Platform Modules
**Rationale:** Separate by platform (android/, ios/, common/)
**Benefits:** Clear platform separation
**Drawbacks:** Duplicates KMP structure, more complex
**Rejected Because:** KMP already handles platform separation

## Consequences

### Positive Outcomes
- **Clear ownership:** AI team owns AI/, Voice team owns Voice/
- **Easier navigation:** Related modules grouped together
- **Better discoverability:** New developers find code faster
- **Reduced redundancy:** Single source of truth for each capability

### Negative Impacts
- **Migration effort:** One-time cost to update paths
- **IDE reconfiguration:** May need to reimport project

### Trade-offs
- **Short-term disruption** for long-term clarity
- **More nested paths** but better organization

## Implementation

### Prerequisites
- Git working tree clean
- All tests passing

### Implementation Plan

#### Phase 1-3: Core Migration (Completed)
- [x] Create Modules/AI/ and Modules/Voice/
- [x] Move NLU, RAG, LLM to AI/
- [x] Move VoiceOSCoreNG to Voice/Core
- [x] Move WakeWord to Voice/WakeWord
- [x] Update settings.gradle.kts
- [x] Update dependent build.gradle.kts files

#### Phase 4-5: Feature Migration (Completed)
- [x] Move Actions to Modules/Actions
- [x] Move Chat to AI/Chat
- [x] Move Teach to AI/Teach

#### Phase 6: Overlay Cleanup (In Progress)
- [ ] Delete duplicate overlay/ folder in Voice/Core
- [ ] Move VoiceOrb, GlassMorphicPanel to AvaUI:Floating
- [ ] Delete AVA/Overlay after migration

### Success Criteria

#### Quantitative Metrics
- Zero build failures after restructuring
- All tests pass
- No duplicate module definitions

#### Qualitative Metrics
- Clear module ownership
- Intuitive navigation for developers

### Rollback Plan
```bash
git checkout HEAD -- Modules/ settings.gradle.kts
```

## Module Structure After Migration

```
Modules/
├── AI/
│   ├── NLU/          # Natural language understanding
│   ├── RAG/          # Retrieval-augmented generation
│   ├── LLM/          # Language model providers
│   ├── Memory/       # AI memory system
│   ├── Chat/         # AI chat interface
│   └── Teach/        # AI teaching system
├── Voice/
│   ├── Core/         # VoiceOSCoreNG (subfolder exception)
│   └── WakeWord/     # Wake word detection
├── Actions/          # Intent handlers
├── AVA/
│   ├── core/         # Foundation (Domain, Utils, Data, Theme)
│   └── Overlay/      # (To be migrated to AvaUI:Floating)
├── AvaMagic/
│   └── AvaUI/
│       └── Floating/ # Generic floating UI components
└── ...
```

## Related Documents
- `docs/ideacode/plans/module-restructure-plan.md` - Original implementation plan
- `settings.gradle.kts` - Module configuration

## Review and Updates

| Date | Change | Reason | By |
|------|--------|---------|-----|
| 2026-01-16 | ADR Created | Document module restructuring | Claude |
| 2026-01-16 | Status: Implemented | Phases 1-5 complete | Claude |

---
**Template Version:** 1.0
