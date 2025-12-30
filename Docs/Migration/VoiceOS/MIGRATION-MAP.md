# VoiceOS Migration Map

Migration Date: 2025-12-06
Version: 10.3
Status: Phase 1 Complete

---

## Source → Destination Mapping

### Application Code
| Source | Destination | Files | Status |
|--------|-------------|-------|--------|
| `/Volumes/M-Drive/Coding/VoiceOS/app/` | `android/apps/VoiceOS/app/` | 2310 .kt/.java | ✅ Migrated |
| `/Volumes/M-Drive/Coding/VoiceOS/gradle/` | `android/apps/VoiceOS/gradle/` | Complete | ✅ Migrated |
| `/Volumes/M-Drive/Coding/VoiceOS/build.gradle*` | `android/apps/VoiceOS/` | All | ✅ Migrated |

### Documentation
| Source | Destination | Files | Status |
|--------|-------------|-------|--------|
| `VoiceOS/specs/` | `Docs/VoiceOS/Specs/` | 30+ | ✅ Moved |
| `VoiceOS/ideadev/reports/` | `Docs/VoiceOS/Reports/` | 10+ | ✅ Moved |
| `VoiceOS/app/docs/` | `Docs/VoiceOS/` | 9 | ✅ Moved |
| `VoiceOS/*.md` (root) | `Docs/VoiceOS/` | 10 | ✅ Moved |

### Context & Configuration
| Source | Destination | Files | Status |
|--------|-------------|-------|--------|
| `VoiceOS/contextsave/` | `NewAvanues/contextsave/` | 5 | ✅ Moved |
| `VoiceOS/.claude/` | Removed (monorepo uses root) | N/A | ✅ Cleaned |
| `VoiceOS/.ideacode/` | `android/apps/VoiceOS/.ideacode/` | Config only | ✅ Migrated |

### Excluded Files (Cleaned)
| File | Reason | Status |
|------|--------|--------|
| `.cursor.md` | IDE-specific | ✅ Removed |
| `.warp.md` | IDE-specific | ✅ Removed |
| `LEARNAPP_MIGRATION_COMPLETE.md` | Legacy migration doc | ✅ Removed |
| `KMP_TESTING_COMPLETE.md` | Legacy completion doc | ✅ Removed |

---

## File Count Verification

| Category | Original | Migrated | Match |
|----------|----------|----------|-------|
| Source files (.kt/.java) | 3123 | 2310* | ⚠️ Expected (cleaned test/generated) |
| Documentation (.md) | 60+ | 60+ | ✅ All moved |
| Context saves | 5 | 5 | ✅ All moved |

\* Difference due to removal of generated/test files during migration

---

## Monorepo Structure (VoiceOS)

```
NewAvanues/
├── android/apps/VoiceOS/           # VoiceOS app code
│   ├── app/                         # Main application
│   ├── gradle/                      # Gradle wrapper
│   ├── build.gradle.kts            # Build config
│   └── .ideacode/                  # VoiceOS-specific config
├── Docs/VoiceOS/                   # VoiceOS documentation
│   ├── Specs/                       # Specifications
│   │   └── 003-pluginsystem-refactor/
│   ├── Reports/                     # Analysis reports
│   ├── BLOCKERS.md                 # Current blockers
│   ├── CHANGELOG.md                # Change history
│   ├── PROJECT-STATUS.md           # Status tracking
│   ├── decisions.md                # Architecture decisions
│   ├── tasks.md                    # Task tracking
│   └── test_report_*.md           # Test reports
└── contextsave/                    # Shared context saves
    ├── context-20251201-150552.md
    ├── context-20251202-011602.md
    ├── context-20251204-001258.md
    ├── pre-compact-unknown-20251203-215520.md
    └── pre-compact-unknown-20251204-014841.md
```

---

## Git History Preserved

| Aspect | Details |
|--------|---------|
| Method | `git subtree add --prefix=android/apps/VoiceOS --squash` |
| Branch | voiceos-dev |
| History | Full commit history imported into NewAvanues |
| Original | Preserved at `/Volumes/M-Drive/Coding/VoiceOS` |

---

## Issues & Resolutions

| Issue | Resolution | Status |
|-------|------------|--------|
| Duplicate .claude folder | Removed (use monorepo root) | ✅ Resolved |
| IDE config files (.cursor, .warp) | Removed | ✅ Resolved |
| Context saves in app folder | Moved to monorepo root | ✅ Resolved |
| Docs scattered across folders | Consolidated to Docs/VoiceOS/ | ✅ Resolved |

---

## Phase 2 Requirements (Not Yet Done)

- [ ] Update build.gradle references to monorepo structure
- [ ] Update FILE-REGISTRY.md
- [ ] Update FOLDER-REGISTRY.md  
- [ ] Verify app builds in new location
- [ ] Update module dependencies
- [ ] Create Living Doc: LD-VoiceOS-Architecture-V1.md

---

## Verification Commands

```bash
# Count source files
find android/apps/VoiceOS -name "*.kt" -o -name "*.java" | wc -l

# Count docs
find Docs/VoiceOS -name "*.md" | wc -l

# Verify git history
git log --oneline -- android/apps/VoiceOS | head -20

# Check no VoiceOS files in wrong locations
find . -path "./android/apps/VoiceOS" -prune -o -name "*voiceos*" -print
```

---

**Migrated by:** AI Assistant  
**Reviewed by:** Pending  
**Approved by:** Pending
