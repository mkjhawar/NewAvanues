# VoiceOS Database Migration Guide: UUID → VUID

**Module:** VoiceOS/core/database
**Migration:** Version 4 → 5
**Date:** 2025-12-23
**Author:** Claude (IDEACODE v12.1)
**Status:** Ready for Testing

---

## Executive Summary

This migration renames all UUID-related tables and columns to VUID (Visual Unique ID) to align with UUIDCreator library terminology and improve code clarity.

**Impact:**
- Schema nomenclature change only
- No data loss
- Full rollback support
- All application code requires updates

---

## Migration Overview

### Changes Summary

| Component | Before | After |
|-----------|--------|-------|
| **Tables** | | |
| Element storage | `uuid_elements` | `vuid_elements` |
| Hierarchy | `uuid_hierarchy` | `vuid_hierarchy` |
| Analytics | `uuid_analytics` | `vuid_analytics` |
| Aliases | `uuid_aliases` | `vuid_aliases` |
| **Columns** | | |
| Primary key | `uuid` | `vuid` |
| Parent reference | `parent_uuid` | `parent_vuid` |
| Child reference | `child_uuid` | `child_vuid` |
| **Indexes** | | |
| All indexes | `idx_uuid_*` | `idx_vuid_*` |
| **DTOs** | | |
| Element DTO | `UUIDElementDTO` | `VUIDElementDTO` |
| Hierarchy DTO | `UUIDHierarchyDTO` | `VUIDHierarchyDTO` |
| Analytics DTO | `UUIDAnalyticsDTO` | `VUIDAnalyticsDTO` |
| Alias DTO | `UUIDAliasDTO` | `VUIDAliasDTO` |
| **Repositories** | | |
| Interface | `IUUIDRepository` | `IVUIDRepository` |
| Implementation | `SQLDelightUUIDRepository` | `SQLDelightVUIDRepository` |
| **Query Files** | | |
| Element queries | `UUIDElement.sq` | `VUIDElement.sq` |
| Hierarchy queries | `UUIDHierarchy.sq` | `VUIDHierarchy.sq` |
| Analytics queries | `UUIDAnalytics.sq` | `VUIDAnalytics.sq` |
| Alias queries | `UUIDAlias.sq` | `VUIDAlias.sq` |

---

## Files Created/Modified

### Migration Files

| File | Location | Purpose |
|------|----------|---------|
| `4.sqm` | `src/commonMain/sqldelight/migrations/` | Forward migration (UUID→VUID) |
| `4-rollback.sqm` | `src/commonMain/sqldelight/migrations/` | Rollback migration (VUID→UUID) |

### Query Files (NEW)

| File | Location |
|------|----------|
| `VUIDElement.sq` | `src/commonMain/sqldelight/com/augmentalis/database/vuid/` |
| `VUIDHierarchy.sq` | `src/commonMain/sqldelight/com/augmentalis/database/vuid/` |
| `VUIDAnalytics.sq` | `src/commonMain/sqldelight/com/augmentalis/database/vuid/` |
| `VUIDAlias.sq` | `src/commonMain/sqldelight/com/augmentalis/database/vuid/` |

### DTO Files (NEW)

| File | Location |
|------|----------|
| `VUIDElementDTO.kt` | `src/commonMain/kotlin/com/augmentalis/database/dto/` |
| `VUIDHierarchyDTO.kt` | `src/commonMain/kotlin/com/augmentalis/database/dto/` |
| `VUIDAnalyticsDTO.kt` | `src/commonMain/kotlin/com/augmentalis/database/dto/` |
| `VUIDAliasDTO.kt` | `src/commonMain/kotlin/com/augmentalis/database/dto/` |

### Repository Files (NEW)

| File | Location |
|------|----------|
| `IVUIDRepository.kt` | `src/commonMain/kotlin/com/augmentalis/database/repositories/` |
| `SQLDelightVUIDRepository.kt` | `src/commonMain/kotlin/com/augmentalis/database/repositories/impl/` |

---

## Pre-Migration Checklist

### 1. Database Backup

```bash
# Backup production database
adb pull /data/data/com.augmentalis.voiceos/databases/voiceos.db ./backups/voiceos-pre-migration-4-$(date +%Y%m%d-%H%M%S).db

# Verify backup integrity
sqlite3 ./backups/voiceos-pre-migration-*.db "PRAGMA integrity_check;"
```

### 2. Test on Production Copy

```bash
# Create test copy
cp ./backups/voiceos-pre-migration-*.db ./test/voiceos-test.db

# Run migration on test copy
sqlite3 ./test/voiceos-test.db < src/commonMain/sqldelight/migrations/4.sqm

# Verify migration success
sqlite3 ./test/voiceos-test.db "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;"

# Expected output should include:
# - vuid_elements
# - vuid_hierarchy
# - vuid_analytics
# - vuid_aliases
```

### 3. Code Compatibility Check

**Files requiring updates:**
- All files importing `UUIDElementDTO`, `UUIDHierarchyDTO`, etc.
- All files using `IUUIDRepository` or `SQLDelightUUIDRepository`
- All files accessing `uuid_elements`, `uuid_hierarchy`, etc. directly

**Search commands:**
```bash
# Find all UUID-related imports
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS
grep -r "import.*UUIDElementDTO" --include="*.kt"
grep -r "import.*IUUIDRepository" --include="*.kt"

# Find all direct table references
grep -r "uuid_elements" --include="*.kt"
grep -r "uuid_hierarchy" --include="*.kt"
```

---

## Migration Execution Steps

### Step 1: Apply Migration

```bash
# Production deployment
# Migration will be auto-applied on app startup via SQLDelight migration system
```

### Step 2: Verify Migration Success

```sql
-- Check table existence
SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'vuid_%';

-- Verify row counts (should match pre-migration)
SELECT
    (SELECT COUNT(*) FROM vuid_elements) as elements,
    (SELECT COUNT(*) FROM vuid_hierarchy) as hierarchy,
    (SELECT COUNT(*) FROM vuid_analytics) as analytics,
    (SELECT COUNT(*) FROM vuid_aliases) as aliases;

-- Verify foreign keys
PRAGMA foreign_key_check;

-- Verify indexes
SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_vuid_%';
```

### Step 3: Monitor Application Logs

```bash
# Watch for migration errors
adb logcat | grep -i "migration\|vuid\|database"
```

---

## Rollback Procedure

If migration fails or issues are detected:

```bash
# Option 1: Apply rollback migration
sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db < migrations/4-rollback.sqm

# Option 2: Restore from backup
adb push ./backups/voiceos-pre-migration-*.db /data/data/com.augmentalis.voiceos/databases/voiceos.db

# Restart app
adb shell am force-stop com.augmentalis.voiceos
```

---

## Post-Migration Tasks

### 1. Code Updates Required

**Repository injection:**
```kotlin
// OLD
private val uuidRepository: IUUIDRepository

// NEW
private val vuidRepository: IVUIDRepository
```

**DTO usage:**
```kotlin
// OLD
val element: UUIDElementDTO = ...

// NEW
val element: VUIDElementDTO = ...
```

**Query access:**
```kotlin
// OLD (via SQLDelight generated code)
database.uUIDElementQueries

// NEW
database.vUIDElementQueries
```

### 2. Testing Checklist

| Test Case | Status | Notes |
|-----------|--------|-------|
| Element CRUD operations | ⬜ | Insert/update/delete/query |
| Hierarchy relationships | ⬜ | Parent-child FK constraints |
| Analytics tracking | ⬜ | Access counts, execution metrics |
| Alias operations | ⬜ | Insert/query/batch operations |
| Foreign key cascades | ⬜ | Element deletion cascades |
| Transaction integrity | ⬜ | Batch operations rollback |
| Migration idempotency | ⬜ | Re-run migration safely |
| Rollback completeness | ⬜ | Rollback restores exact state |

### 3. Performance Validation

```kotlin
// Test batch alias insertion performance (should be <100ms for 63 elements)
val startTime = System.currentTimeMillis()
vuidRepository.insertAliasesBatch(aliases)
val duration = System.currentTimeMillis() - startTime
// Expected: duration < 100ms
```

---

## Known Issues & Limitations

### 1. Old Query Files
- Old `UUIDElement.sq`, `UUIDHierarchy.sq`, etc. files should be **DELETED** after migration
- SQLDelight will generate duplicate code if both old and new files exist

### 2. Dependency Updates
- All modules depending on database module must be updated simultaneously
- Includes: `learnapp`, `elementmanager`, `uuidaliasmanager`, etc.

### 3. Generated Code
- SQLDelight will regenerate all query classes
- Clean build required: `./gradlew clean build`

---

## Verification Queries

### Pre-Migration State Capture

```sql
-- Save counts
CREATE TEMP TABLE pre_migration_counts AS
SELECT
    (SELECT COUNT(*) FROM uuid_elements) as uuid_elements,
    (SELECT COUNT(*) FROM uuid_hierarchy) as uuid_hierarchy,
    (SELECT COUNT(*) FROM uuid_analytics) as uuid_analytics,
    (SELECT COUNT(*) FROM uuid_aliases) as uuid_aliases;

-- Save sample records
CREATE TEMP TABLE pre_migration_samples AS
SELECT * FROM uuid_elements LIMIT 5;
```

### Post-Migration Validation

```sql
-- Compare counts
SELECT
    (SELECT COUNT(*) FROM vuid_elements) = (SELECT uuid_elements FROM pre_migration_counts) as elements_match,
    (SELECT COUNT(*) FROM vuid_hierarchy) = (SELECT uuid_hierarchy FROM pre_migration_counts) as hierarchy_match,
    (SELECT COUNT(*) FROM vuid_analytics) = (SELECT uuid_analytics FROM pre_migration_counts) as analytics_match,
    (SELECT COUNT(*) FROM vuid_aliases) = (SELECT uuid_aliases FROM pre_migration_counts) as aliases_match;

-- Verify data integrity
SELECT
    ve.vuid,
    ve.name,
    COUNT(vh.child_vuid) as child_count,
    va.access_count,
    (SELECT COUNT(*) FROM vuid_aliases WHERE vuid = ve.vuid) as alias_count
FROM vuid_elements ve
LEFT JOIN vuid_hierarchy vh ON ve.vuid = vh.parent_vuid
LEFT JOIN vuid_analytics va ON ve.vuid = va.vuid
GROUP BY ve.vuid
LIMIT 10;
```

---

## Support & Troubleshooting

### Migration Fails on Step 1-4

**Symptom:** Migration script fails during table rename
**Cause:** Existing FK constraints preventing rename
**Solution:** Rollback and check for orphaned records

```sql
-- Check for orphaned hierarchy records
SELECT * FROM uuid_hierarchy h
WHERE NOT EXISTS (SELECT 1 FROM uuid_elements e WHERE e.uuid = h.parent_uuid)
   OR NOT EXISTS (SELECT 1 FROM uuid_elements e WHERE e.uuid = h.child_uuid);

-- Check for orphaned analytics records
SELECT * FROM uuid_analytics a
WHERE NOT EXISTS (SELECT 1 FROM uuid_elements e WHERE e.uuid = a.uuid);

-- Check for orphaned alias records
SELECT * FROM uuid_aliases a
WHERE NOT EXISTS (SELECT 1 FROM uuid_elements e WHERE e.uuid = a.uuid);
```

### Application Crashes After Migration

**Symptom:** App crashes on startup or database access
**Cause:** Code still referencing old table/column names
**Solution:** Verify all code updates completed

```bash
# Find remaining UUID references
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS
grep -r "UUIDElementDTO\|IUUIDRepository\|uuid_elements" --include="*.kt" | grep -v ".backup"
```

### Performance Degradation

**Symptom:** Queries slower after migration
**Cause:** Missing indexes after migration
**Solution:** Verify all indexes recreated

```sql
-- Check index count (should be 14 total)
SELECT COUNT(*) FROM sqlite_master
WHERE type='index' AND name LIKE 'idx_vuid_%';

-- Expected indexes:
-- idx_vuid_element_name, idx_vuid_element_type, idx_vuid_element_parent, idx_vuid_element_timestamp
-- idx_vuid_hierarchy_parent, idx_vuid_hierarchy_child, idx_vuid_hierarchy_depth, idx_vuid_hierarchy_path
-- idx_vuid_analytics_access_count, idx_vuid_analytics_last_accessed, idx_vuid_analytics_lifecycle
-- idx_vuid_alias_alias, idx_vuid_alias_vuid
```

---

## Success Criteria

Migration is considered successful when:

1. ✅ All 4 VUID tables exist and populated
2. ✅ All 4 UUID tables are deleted
3. ✅ Row counts match pre-migration state
4. ✅ All foreign key constraints valid
5. ✅ All 13+ indexes recreated
6. ✅ Application starts without crashes
7. ✅ Element CRUD operations functional
8. ✅ Batch alias insertion <100ms for 63 elements
9. ✅ No errors in application logs
10. ✅ Rollback migration tested successfully

---

## Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| Code preparation | 1 hour | ✅ Complete |
| Testing on copy | 30 mins | ⬜ Pending |
| Production backup | 5 mins | ⬜ Pending |
| Migration execution | 5 mins | ⬜ Pending |
| Verification | 15 mins | ⬜ Pending |
| Code updates | 2 hours | ⬜ Pending |
| Integration testing | 1 hour | ⬜ Pending |
| **Total** | **~5 hours** | |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-23 | Claude | Initial migration guide |

---

**CRITICAL REMINDER:** Always test on production copy first. Never run migration on production database without verified backup and successful test run.
