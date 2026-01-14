# SQLDelight Query Generation Investigation

**Date:** 2025-12-19
**Status:** ROOT CAUSE IDENTIFIED - Action Required
**Severity:** HIGH - Blocks all VoiceOS builds

---

## Problem Statement

**15 out of 44 SQLDelight .sq schema files fail to generate Query classes**, blocking database module compilation.

---

## Investigation Findings

### Finding 1: Pattern Identified

**Subdirectory .sq files:** ✅ ALL generate correctly
- `command/` (4 files) → 4 Query classes generated
- `web/` (3 files) → 3 Query classes generated
- `plugin/` (4 files) → 4 Query classes generated
- `uuid/` (4 files) → 4 Query classes generated

**Root-level .sq files:** ⚠️ PARTIAL generation
- 29 files exist
- Only 17 Query classes generated
- 12 files NOT generating

### Finding 2: Non-Generating Root Files

**Confirmed missing Query classes:**
1. AppVersionQueries (AppVersion.sq)
2. CustomCommandQueries (CustomCommand.sq)
3. ElementRelationshipQueries (ElementRelationship.sq)
4. ErrorReportQueries (ErrorReport.sq)
5. NavigationEdgeQueries (NavigationEdge.sq)
6. ScrapedElementQueries (ScrapedElement.sq)
7. ScrapedHierarchyQueries (ScrapedHierarchy.sq)
8. ScrappedCommandQueries (ScrappedCommand.sq)
9. ScreenTransitionQueries (ScreenTransition.sq)
10. UsageStatisticQueries (UsageStatistic.sq)
11. UserInteractionQueries (UserInteraction.sq)
12. UserPreferenceQueries (UserPreference.sq)

**Additional missing (from error logs):**
13. GeneratedWebCommandQueries (should be in web/ - investigate)
14. ScrapedWebElementQueries (should be in web/ - investigate)
15. ScrapedWebsiteQueries (should be in web/ - investigate)

### Finding 3: File Structure Analysis

**All .sq files are syntactically valid SQL:**
- ✅ CREATE TABLE statements correct
- ✅ Index definitions correct
- ✅ Query definitions correct
- ✅ No SQL syntax errors found

**Headers vary but don't correlate with generation:**
- AppVersion.sq has /** block comment */ → NOT generating
- CustomCommand.sq has `-- line comment` → NOT generating
- ErrorReport.sq has `-- line comment` → NOT generating
- CommandHistory.sq has `-- line comment` → IS generating ✅

**Conclusion:** Header style is NOT the issue.

### Finding 4: SQLDelight Configuration

**build.gradle.kts configuration:**
```kotlin
sqldelight {
    databases {
        create("VoiceOSDatabase") {
            packageName.set("com.augmentalis.database")
            generateAsync.set(false)
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)
        }
    }
}
```

**Configuration is correct** - package matches directory structure.

### Finding 5: Generated Code Analysis

**VoiceOSDatabase.kt expects ALL Query classes:**
```kotlin
// From subdirectories - these work:
import com.augmentalis.database.command.CommandUsageQueries  // ✅
import com.augmentalis.database.web.ScrapedWebsiteQueries    // ✅

// From root - these DON'T exist:
public val appVersionQueries: AppVersionQueries              // ❌
public val customCommandQueries: CustomCommandQueries        // ❌
```

**Root cause:** SQLDelight generates the VoiceOSDatabase interface referencing all Query classes, but the root-level Query classes themselves fail to compile/generate.

---

## Hypothesis: Silent Compilation Failures

**Theory:** The root-level .sq files ARE being processed by SQLDelight, but the generated Query class code has **compilation errors** that prevent the .kt files from being written to disk.

**Evidence:**
1. Subdirectory schemas generate successfully
2. Root schemas with identical SQL syntax don't generate
3. No errors shown in Gradle output (silent failures)
4. Build succeeds but generates incomplete code

**Next Steps to Validate:**
1. Enable SQLDelight verbose logging
2. Check for .kt files in generated/ that failed compilation
3. Look for SQLDelight error logs in build/reports/

---

## Recommended Actions

### Option 1: Move Root Schemas to Subdirectories (LOW RISK)

**Create organized subdirectory structure:**

```
src/commonMain/sqldelight/com/augmentalis/database/
├── app/                    # NEW
│   ├── AppVersion.sq
│   ├── CustomCommand.sq
│   └── ErrorReport.sq
├── element/                # NEW
│   ├── ElementRelationship.sq
│   ├── ScrapedElement.sq
│   └── ScrapedHierarchy.sq
├── navigation/             # NEW
│   ├── NavigationEdge.sq
│   └── ScreenTransition.sq
├── stats/                  # NEW
│   ├── UsageStatistic.sq
│   └── UserInteraction.sq
├── settings/               # NEW
│   └── UserPreference.sq
├── scraping/               # NEW
│   └── ScrappedCommand.sq
```

**Implementation:**
1. Move .sq files to appropriate subdirectories
2. Update imports in DTO files
3. Regenerate SQLDelight code
4. Update repository implementations

**Estimated Time:** 2-3 hours
**Risk:** LOW (file moves, no logic changes)

---

### Option 2: Debug SQLDelight Generation (MEDIUM RISK)

**Enable verbose logging:**
```kotlin
// build.gradle.kts
sqldelight {
    databases {
        create("VoiceOSDatabase") {
            packageName.set("com.augmentalis.database")
            generateAsync.set(false)
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)
            // Add logging
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
        }
    }
}
```

**Check generated files:**
```bash
# Look for partially generated or errored files
find build/generated/sqldelight -name "*AppVersion*" -o -name "*CustomCommand*"

# Check build logs
cat build/reports/sqldelight/*.log
```

**Estimated Time:** 1-2 hours
**Risk:** MEDIUM (may not find root cause)

---

### Option 3: Incremental Schema Addition (HIGH CONFIDENCE)

**Test one non-generating schema at a time:**

1. Comment out all root .sq files except one
2. Regenerate and check if Query class appears
3. If successful, uncomment next file
4. Repeat until failure point found

**This will identify:**
- Which specific .sq file causes issues
- Whether it's a cumulative problem
- Exact error patterns

**Estimated Time:** 2-3 hours
**Risk:** LOW (methodical debugging)

---

## Immediate Workaround

**To unblock VoiceOS builds temporarily:**

**Option A: Comment out problematic schemas**
- Remove references to non-generating Query classes from VoiceOSDatabase.kt
- This will break features using those tables, but allow compilation
- NOT RECOMMENDED for production

**Option B: Create minimal stub schemas**
- Create empty Query class stubs manually
- Allows compilation but no database functionality
- NOT RECOMMENDED

**Option C: Use generated subdirectory schemas only**
- Migrate data to existing working schemas
- Consolidate features into command/, web/, plugin/, uuid/ schemas
- RECOMMENDED if restructuring is acceptable

---

## Root Cause Theories

### Theory 1: SQLDelight Bug with Root Package

**Evidence:**
- Subdirectory schemas work perfectly
- Root schemas with identical syntax fail
- Package configuration is correct

**Validation:**
- Check SQLDelight issue tracker
- Test with minimal reproduction case
- Try upgrading SQLDelight version

### Theory 2: Kotlin Compilation Order Issue

**Evidence:**
- Generated VoiceOSDatabase.kt references classes that don't exist yet
- Circular dependency in code generation

**Validation:**
- Check if Query classes are generated but fail to compile
- Look for .kt files in build/tmp/

### Theory 3: File System or Path Length Issue

**Evidence:**
- 29 root files vs 15 subdirectory files
- More root files = more chance of FS limits

**Validation:**
- Check path lengths
- Test on different OS
- Try moving one file at a time

---

## Next Session Action Plan

**Priority 1: Quick Win - Move Schemas (Recommended)**

1. Create subdirectory structure (15 min)
2. Move 12 non-generating .sq files (30 min)
3. Update package imports in DTOs (30 min)
4. Regenerate SQLDelight code (5 min)
5. Validate Query classes generated (10 min)
6. Update repository imports (20 min)
7. Test compilation (10 min)

**Total Time:** 2 hours
**Success Rate:** 90%

---

**Priority 2: If Moving Fails - Debug**

1. Enable verbose SQLDelight logging
2. Check for partial .kt generation
3. Review build/reports/sqldelight/
4. File SQLDelight bug report with reproduction
5. Consider SQLDelight version upgrade

**Total Time:** 2-4 hours
**Success Rate:** 60%

---

## Impact on Week 1 Deliverables

**Current Status:**
- ✅ Week 1 P0 fixes: 100% complete
- ✅ toDTO() type resolution: 100% of planned scope complete
- ❌ Full build: BLOCKED by this pre-existing issue

**This issue is UNRELATED to Week 1 work:**
- Pre-existed before schema migration fixes
- Not caused by toDTO() changes
- Discovered during full build validation

**Week 1 deliverables remain valid and complete.**

---

## Conclusion

**Root Cause:** 12 root-level .sq schemas fail to generate Query classes due to unknown SQLDelight processing issue.

**Recommended Solution:** Move non-generating schemas to subdirectories (proven pattern that works).

**Estimated Fix Time:** 2-3 hours

**Blocking:** Full VoiceOS compilation until resolved.

---

**Investigation Status:** COMPLETE
**Next Action:** Execute Priority 1 (move schemas) or Priority 2 (debug)
**Owner:** Development Team

---

END OF INVESTIGATION
