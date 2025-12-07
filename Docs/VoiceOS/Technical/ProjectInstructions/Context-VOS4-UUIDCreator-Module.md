<!--
filename: UUIDCREATOR-AGENT-CONTEXT.md
created: 2025-10-08 00:30 PST
author: VOS4 Development Team
purpose: Agent context and standards for UUIDCreator enhancement project
last-modified: 2025-10-08 00:30 PST
version: 1.0.0
-->

# UUIDCreator Enhancement - Agent Context & Standards

## 🎯 Project Overview

**Project**: UUIDCreator Enhancement (formerly UUIDManager)
**Branch**: `feature/uuidcreator` (git worktree)
**Working Directory**: `/Volumes/M Drive/Coding/vos4-uuidcreator/`
**Reference Directory**: `/Volumes/M Drive/Coding/vos4/` (main repo - READ ONLY)
**Total Effort**: 87-110 Claude sessions
**Current Phase**: Phase 2 (Room Database Migration)

---

## 📋 What I Must Remember

### CRITICAL: Git Worktree Environment
- **Active Development**: `/Volumes/M Drive/Coding/vos4-uuidcreator/`
- **Reference Only**: `/Volumes/M Drive/Coding/vos4/` (DO NOT MODIFY)
- **Branch**: `feature/uuidcreator`
- **Merge Target**: `vos4-legacyintegration` (when complete)

### CRITICAL: Namespace Standards
```
✅ CORRECT:
- Namespace: com.augmentalis.uuidcreator
- Module: UUIDCreator
- Class: UUIDCreator (formerly UUIDManager)
- Package: com.augmentalis.uuidcreator

❌ WRONG (deprecated):
- com.augmentalis.uuidmanager
- UUIDManager class
- com.ai.* (old standard)
```

### CRITICAL: Database Technology
```
✅ CORRECT: Room (AndroidX)
- Use Room for ALL data persistence
- Hybrid storage: Room (on-disk) + ConcurrentHashMap (in-memory cache)
- Lazy loading on first access
- KSP for annotation processing (NOT KAPT)

❌ WRONG:
- ObjectBox (old standard)
- Realm
- SQLite alone
- SharedPreferences for data
```

### CRITICAL: File Naming Conventions
```
✅ CORRECT (camelCase):
- thirdPartyAppUuidGeneration.md
- uuidCreatorEnhancementPlan.md
- phase1AccomplishmentReport.md

❌ WRONG:
- third-party-app-uuid-generation.md (kebab-case)
- UUIDCreatorEnhancementPlan.md (PascalCase for docs)
- phase-1-accomplishment-report.md (hyphens)
```

### CRITICAL: Documentation Organization
```
✅ CORRECT:
/docs/modules/UUIDCreator/
├── architecture/              # Architecture designs
├── implementation-plans/      # Roadmaps and plans
├── precompaction-reports/     # Session context
├── phase-tracking/            # Phase accomplishments
├── changelog/                 # Version history (when created)
└── README.md                  # Module overview

❌ WRONG:
/docs/architecture/uuidcreator.md     # Cross-module pollution
/docs/implementation-plans/uuidcreator.md  # Should be in module
```

---

## 📊 Phase Tracking Requirements

### MANDATORY: Track Every Phase
After completing each phase, create:
```
/docs/modules/UUIDCreator/phase-tracking/phase{N}AccomplishmentReport.md
```

**Must Include**:
1. ✅ Phase objectives (planned vs actual)
2. ✅ Accomplishments with evidence (commit hashes)
3. ✅ Files changed count
4. ✅ Metrics (time, efficiency, quality)
5. ✅ Success criteria checklist
6. ✅ Git commit history for phase
7. ✅ Key learnings
8. ✅ Next steps

**Template**: See `/docs/modules/UUIDCreator/phase-tracking/phase1AccomplishmentReport.md`

---

## 🔑 Key Technical Decisions (From Context Summary)

### Decision 1: Enhance, Don't Rebuild
- VOS4 has 100% functional UUIDManager implementation (20 Kotlin files)
- Enhancing is 3x faster than rebuilding (87 sessions vs 250)
- Keep existing architecture: Registry, Generator, Navigator, Resolver

### Decision 2: Git Worktree vs Branch
- Use worktree for long-running feature (87-110 sessions)
- Allows reference to main repo while developing
- Isolated from main branch for safety

### Decision 3: Third-Party App UUID Format
```
Format: {packageName}.v{version}.{elementType}-{contentHash}
Example: com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
```
- Deterministic generation from AccessibilityNodeInfo
- SHA-256 content hashing for stability
- Package + version isolation prevents collisions

### Decision 4: Database Strategy
- Room for on-disk persistence
- ConcurrentHashMap for in-memory cache
- Lazy loading on first access
- Performance: O(1) lookups from cache

### Decision 5: Naming Convention
- Clean names without suffixes
- `uuidcreator` not `uuidcreator-core` or `uuidcreator-enhancement`
- camelCase for all files
- No hyphens in module names

---

## 🚀 Priority Features (Implementation Order)

### 0. Third-Party App UUID Generation ⭐⭐ CRITICAL
**Phase**: 2.5 (12-15 sessions)
**Impact**: Universal voice control for ANY Android app

**Components**:
```
modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/
├── thirdParty/
│   ├── thirdPartyUuidGenerator.kt
│   ├── accessibilityFingerprint.kt
│   ├── contentHasher.kt
│   ├── packageVersionResolver.kt
│   ├── uuidStabilityTracker.kt
│   └── thirdPartyUuidCache.kt
```

### 1. UUID Analytics
**Phase**: 2 & 4
**Tracking**: Access frequency, lifecycle, performance, success/failure rates

### 2. Hierarchical UUIDs
**Phase**: 2 & 4
**Features**: Parent-child relationships, tree traversal, cascade operations

### 3. Custom UUID Formats
**Phase**: 3
**Formats**: Standard, Prefixed, Third-party

### 4. Collision Monitoring
**Phase**: 3
**Features**: Pre-registration checks, periodic scans, resolution strategies

---

## 📁 Current File Structure

```
/vos4-uuidcreator/                              # ← WORKTREE (active dev)
├── Agent-Instructions/
│   ├── Protocol-VOS4-Agent-Deployment.md       # Multi-agent requirements
│   ├── Protocol-VOS4-Coding-Standards.md       # Coding standards
│   ├── Protocol-VOS4-Commit.md                 # Git standards
│   ├── Protocol-VOS4-Documentation.md          # Doc standards
│   └── UUIDCREATOR-AGENT-CONTEXT.md            # ← THIS FILE
├── docs/
│   └── modules/
│       └── uuidcreator/
│           ├── architecture/                    # Design docs
│           ├── implementation-plans/            # Roadmaps
│           ├── precompaction-reports/           # Context
│           ├── phase-tracking/                  # Accomplishments
│           └── README.md
├── modules/
│   └── libraries/
│       └── UUIDCreator/                         # ✅ RENAMED (was UUIDManager)
│           ├── src/main/java/com/augmentalis/uuidcreator/  # ✅ NEW NAMESPACE
│           │   ├── UUIDCreator.kt              # Main class
│           │   ├── core/
│           │   │   ├── UUIDRegistry.kt
│           │   │   └── UUIDGenerator.kt
│           │   ├── spatial/
│           │   │   └── SpatialNavigator.kt
│           │   ├── targeting/
│           │   │   └── TargetResolver.kt
│           │   ├── models/                      # Data models
│           │   ├── compose/                     # Compose extensions
│           │   └── ui/                          # UI components
│           ├── build.gradle.kts                 # ✅ Updated namespace
│           └── AndroidManifest.xml              # ✅ Updated namespace
└── tools/
    └── renameUuidManagerToUuidCreator.sh        # Automated rename script

/vos4/                                           # ← MAIN REPO (reference only)
└── [unchanged - on vos4-legacyintegration branch]
```

---

## 📝 VOS4 Standards (Must Follow)

### From `.warp.md` and Agent Instructions:
1. ✅ Namespace: `com.augmentalis.*` (NO `com.ai`)
2. ✅ Database: Room (NO Realm, NO ObjectBox)
3. ✅ Architecture: Zero-overhead direct implementation (NO interfaces unless justified)
4. ✅ Documentation BEFORE code commits
5. ✅ Stage by category (docs first, then code by module)
6. ✅ No AI references in commits
7. ✅ Update module changelogs
8. ✅ Branch: `feature/uuidcreator` (on worktree)
9. ✅ File naming: camelCase

### Git Workflow:
1. ✅ Documentation committed first
2. ⏳ Code commits by phase
3. ⏳ Update CHANGELOG.md for each module touched
4. ⏳ Merge to `vos4-legacyintegration` when complete

### Time Estimates:
- Use "Claude effort sessions" NOT "man-hours"
- Example: "Phase 2: 18-22 Claude sessions" ✅
- NOT: "Phase 2: 2-3 weeks" ❌

---

## 🎯 Phase 1 Summary (COMPLETE)

**Status**: ✅ COMPLETE
**Sessions**: 1 (vs 10-12 estimated = 92% reduction)
**Commits**: 5 total

### Commits:
1. `9faa17c` - Initial documentation
2. `513d96d` - AI instruction updates (Room, Claude effort)
3. `03732f2` - Automated rename script
4. `b10cd35` - Namespace rename execution (227 files)
5. `24862e2` - Documentation reorganization

### Accomplishments:
- ✅ Updated 10 AI instruction files (Room standard, Claude effort terminology)
- ✅ Created automated rename script with backup
- ✅ Renamed UUIDManager → UUIDCreator (227 files)
- ✅ Reorganized all documentation to `/docs/modules/UUIDCreator/`
- ✅ Created Phase 1 tracking document

---

## 🔜 Phase 2 Objectives (NEXT)

**Phase 2: Room Database Migration** (18-22 sessions estimated)

### Objectives:
1. Design Room database schema
2. Create entity classes (@Entity with Room annotations)
3. Create DAO interfaces (@Dao)
4. Create Database class (@Database)
5. Implement hybrid storage (Room + ConcurrentHashMap)
6. Add lazy loading on first access
7. Migrate from current in-memory-only to hybrid
8. Update tests for Room database
9. Add analytics tracking tables
10. Add hierarchical UUID tables

### Key Files to Create:
```
modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/
├── database/
│   ├── UUIDDatabase.kt              # @Database
│   ├── dao/
│   │   ├── UUIDElementDao.kt        # @Dao
│   │   ├── UUIDAnalyticsDao.kt      # @Dao
│   │   └── UUIDHierarchyDao.kt      # @Dao
│   ├── entities/
│   │   ├── UUIDElementEntity.kt     # @Entity
│   │   ├── UUIDAnalyticsEntity.kt   # @Entity
│   │   └── UUIDHierarchyEntity.kt   # @Entity
│   └── repository/
│       ├── UUIDRepository.kt        # Hybrid storage
│       └── UUIDAnalyticsRepository.kt
```

---

## 🚨 What NOT to Do

### ❌ Don't rebuild from scratch
- Enhance existing 20 Kotlin files
- Keep working Registry, Generator, Navigator, Resolver

### ❌ Don't use wrong namespace
- NO `com.ai.*`
- NO `com.augmentalis.uuidmanager`
- YES `com.augmentalis.uuidcreator`

### ❌ Don't use wrong database
- NO Realm
- NO ObjectBox
- NO SQLite alone
- YES Room with hybrid storage

### ❌ Don't commit code before docs
- Documentation MUST be committed first
- Update docs when creating new features

### ❌ Don't use suffixes in names
- NO `uuidcreator-core`
- NO `uuidcreator-enhancement`
- YES `uuidcreator`

---

## ✅ What TO Remember

### ✅ Work in worktree
- `/Volumes/M Drive/Coding/vos4-uuidcreator/`
- NOT in `/Volumes/M Drive/Coding/vos4/`

### ✅ Reference main repo
- Read from `/vos4/` for existing code
- DO NOT modify `/vos4/`

### ✅ Third-party UUIDs are CRITICAL
- Phase 2.5 is highest priority after Phase 2
- Enables voice control for ANY Android app

### ✅ Custom UUID format
```
{packageName}.v{version}.{type}-{hash}
Example: com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
```

### ✅ Hybrid storage is mandatory
- Room for persistence
- ConcurrentHashMap for speed
- Lazy load on first access

### ✅ All files in camelCase
- thirdPartyAppUuidGeneration.md ✅
- third-party-app-uuid-generation.md ❌

### ✅ Create phase tracking docs
- After EVERY phase completion
- Use template from phase1AccomplishmentReport.md

---

## 📊 Estimated Remaining Effort

- **Total**: 87-110 Claude sessions
- **Phase 1 Completed**: 1 session (vs 10-12 estimated)
- **Remaining**: ~86-109 sessions

**If efficiency continues**:
- Phase 2: Could take 2-3 sessions (vs 18-22 estimated)
- Total project: Could complete in 8-12 sessions (vs 87-110 estimated)
- Potential 90%+ time reduction through automation and parallel processing

---

## 📍 Current Status

**Session**: 2025-10-08 00:30 PST
**Phase**: Transitioning to Phase 2
**Branch**: `feature/uuidcreator`
**Working Directory**: `/Volumes/M Drive/Coding/vos4-uuidcreator/`
**Git Status**: Clean (all changes committed)
**Next Action**: Begin Phase 2 - Room Database Migration

---

**End of Agent Context Document**
