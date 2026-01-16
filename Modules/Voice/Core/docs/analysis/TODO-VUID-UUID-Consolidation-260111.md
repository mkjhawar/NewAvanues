# TODO: VUID/UUID Consolidation - Full Examination Required

**Created:** 2026-01-11
**Status:** DEFERRED
**Priority:** After MagicCode/MagicUI consolidation complete

---

## Context

During the AVAMagic consolidation analysis, we identified two UUIDCreator implementations:

| Location | Files | Features |
|----------|-------|----------|
| `Common/Libraries/uuidcreator/` | 26+ | Basic UUID, Room DB |
| `Modules/AVAMagic/Libraries/UUIDCreator/` | 43 | VUID, Clickability, Compose, Flutter, Spatial, Third-party |

**Reason for deferral:** VoiceOSCoreNG has dependencies on UUID/VUID that may break during migration.

---

## Required Examination

### 1. Dependency Analysis
- [ ] Find all imports of `com.augmentalis.uuidcreator` across codebase
- [ ] Find all imports of `com.augmentalis.vuidcreator` (if exists)
- [ ] Map which modules depend on which UUIDCreator
- [ ] Identify VoiceOSCoreNG-specific dependencies

### 2. Feature Comparison (Detailed)
- [ ] Compare `UUIDGenerator.kt` implementations
- [ ] Compare database schemas (Room vs SQLDelight)
- [ ] Compare model classes (UUID* vs VUID*)
- [ ] Identify unique features in each

### 3. Migration Impact Assessment
- [ ] List breaking changes if Common version is used
- [ ] List breaking changes if AVAMagic version is used
- [ ] Estimate refactoring effort for each option

### 4. VoiceOSCoreNG Specific
- [ ] Check `VoiceOSCoreNG` usage of VUID
- [ ] Check screen hash / element identification code
- [ ] Check command registry VUID references
- [ ] Test VUID generation in isolation

### 5. Decision Points
- [ ] Keep both (no consolidation)?
- [ ] Use AVAMagic as master (more features)?
- [ ] Merge unique features to Common?
- [ ] Create new KMP-compatible version?

---

## Files to Examine

### Common/Libraries/uuidcreator/
```
src/main/java/com/augmentalis/uuidcreator/
├── UUIDCreator.kt
├── api/IUUIDManager.kt
├── core/UUIDGenerator.kt
├── core/UUIDRegistry.kt
├── database/UUIDCreatorDatabase.kt
├── database/dao/*.kt
├── database/entities/*.kt
├── database/repository/UUIDRepository.kt
├── integration/*.kt
├── models/*.kt
└── ui/*.kt
```

### AVAMagic/Libraries/UUIDCreator/
```
src/main/java/com/augmentalis/uuidcreator/
├── UUIDCreator.kt
├── VUIDCreator.kt                      # UNIQUE
├── VUIDCreatorServiceBinder.kt         # UNIQUE
├── alias/UuidAliasManager.kt           # UNIQUE
├── api/IUUIDManager.kt
├── api/IVUIDManager.kt                 # UNIQUE
├── compose/ComposeExtensions.kt        # UNIQUE
├── core/ClickabilityDetector.kt        # UNIQUE
├── core/UUIDGenerator.kt
├── core/VUIDGenerator.kt               # UNIQUE
├── core/VUIDRegistry.kt                # UNIQUE
├── database/*.kt
├── flutter/FlutterIdentifierExtractor.kt  # UNIQUE
├── formats/CustomUuidGenerator.kt      # UNIQUE
├── migration/VuidMigrator.kt           # UNIQUE
├── models/*.kt
├── spatial/SpatialNavigator.kt         # UNIQUE
├── targeting/TargetResolver.kt         # UNIQUE
├── thirdparty/*.kt                     # UNIQUE
└── ui/*.kt
```

---

## When to Execute

Execute this examination AFTER:
1. MagicCode → AVACode consolidation complete
2. MagicUI → AVAUI consolidation complete
3. All builds passing
4. Tests passing

---

## Command to Start

```
/i.analyze .code Modules/AVAMagic/Libraries/UUIDCreator Common/Libraries/uuidcreator
```

Then create spec and plan for VUID/UUID consolidation.

---

*This file serves as a reminder to complete the VUID/UUID consolidation examination.*
