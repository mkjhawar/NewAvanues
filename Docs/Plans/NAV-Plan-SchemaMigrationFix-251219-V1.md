# Schema Migration Fix - Implementation Plan

**Date:** 2025-12-19
**Status:** Planning Phase
**Complexity:** Medium-High
**Estimated Time:** 4-6 hours (sequential) / 2-3 hours (with analysis tools)
**Priority:** HIGH (blocks full compilation)

---

## Executive Summary

**Problem:** Pre-existing schema migration errors in VoiceOS database code are blocking full compilation. These errors are NOT caused by Week 1 fixes but are remnants from previous schema evolution work.

**Root Cause Analysis (CoT - Chain of Thought):**

1. **Schema Evolution History:**
   - Original schema used Room with auto-generated DAOs
   - Migration to SQLDelight changed code generation patterns
   - Schema v3 added version tracking columns (appId, appVersion, versionCode, lastVerified, isDeprecated)
   - DTO layer updated but repository implementations not fully synchronized

2. **Type Resolution Issue (SQLDelightVoiceCommandRepository.kt):**
   - Multiple `toDTO()` extension functions exist for different table types
   - Kotlin compiler can't resolve which extension to use
   - Likely cause: Overlapping extension function scopes or missing explicit type annotations

3. **Missing DTO Parameters (AccessibilityScrapingIntegration.kt):**
   - ScrapedWebsite DTO constructor signature changed
   - Integration layer still using old constructor calls
   - Missing: firstScrapedAt, lastScrapedAt, appName parameters

**Impact:**
- ✅ Week 1 fixes are code-complete and validated
- ❌ Full VoiceOSCore compilation blocked
- ❌ Integration tests cannot run
- ⚠️ Production builds unavailable

---

## Tree of Thought (ToT) - Solution Approaches

### Approach 1: Incremental Fix (Recommended)
**Strategy:** Fix issues one at a time, validate after each

**Pros:**
- Lower risk of introducing new errors
- Easy to rollback if issues arise
- Can validate incrementally

**Cons:**
- Slower (4-6 hours)
- Multiple compile cycles

**Confidence:** HIGH (90%)

---

### Approach 2: Comprehensive Schema Redesign
**Strategy:** Redesign entire DTO/repository layer from scratch

**Pros:**
- Clean slate, no technical debt
- Modern best practices
- Future-proof architecture

**Cons:**
- Very time-consuming (2-3 weeks)
- High risk of breaking existing functionality
- Requires extensive testing

**Confidence:** LOW (40%) - overkill for current issue

---

### Approach 3: Generated Code Regeneration
**Strategy:** Clean all SQLDelight generated code and regenerate from scratch

**Pros:**
- Quick (30 minutes)
- May resolve type resolution issues automatically
- Low risk

**Cons:**
- May not fix DTO parameter mismatches
- Could surface additional hidden issues
- Doesn't address root cause

**Confidence:** MEDIUM (60%)

---

### **Selected Approach: Hybrid (1 + 3)**

**Reasoning (RoT - Refinement of Thought):**
1. Start with Approach 3 (regeneration) to quickly fix type resolution
2. If regeneration doesn't fully resolve, proceed with Approach 1 (incremental)
3. This maximizes speed while maintaining safety

**Estimated Time:** 2-3 hours (best case) / 4-6 hours (worst case)

---

## Implementation Plan

### Phase 1: Diagnosis & Analysis (30 min)

**Task 1.1: Analyze Type Resolution Ambiguity**
- Read SQLDelightVoiceCommandRepository.kt (lines 70-80)
- Identify all `toDTO()` extension functions in scope
- Determine which types are conflicting
- Document exact compiler error context

**Task 1.2: Analyze Missing DTO Parameters**
- Read AccessibilityScrapingIntegration.kt
- Find all ScrapedWebsite DTO instantiation calls
- Compare with current DTO constructor signature
- List all missing parameters

**Task 1.3: Examine Schema Evolution History**
- Check git history for schema changes
- Review migration files
- Identify when parameters were added

**Deliverable:** Diagnosis report with root cause analysis

---

### Phase 2: SQLDelight Code Regeneration (30 min)

**Task 2.1: Clean Generated Code**
```bash
./gradlew :Modules:VoiceOS:core:database:clean
rm -rf Modules/VoiceOS/core/database/build/generated
```

**Task 2.2: Regenerate SQLDelight Code**
```bash
./gradlew :Modules:VoiceOS:core:database:generateCommonMainDatabaseInterface
./gradlew :Modules:VoiceOS:core:database:generateAndroidDebugDatabaseInterface
```

**Task 2.3: Validate Regeneration**
```bash
./gradlew :Modules:VoiceOS:core:database:compileDebugKotlinAndroid
```

**Expected Outcome:** Type resolution ambiguity resolved (if caused by stale generated code)

---

### Phase 3: Fix Type Resolution Ambiguity (1-2 hours)

**Task 3.1: Add Explicit Type Annotations**

**File:** `/Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightVoiceCommandRepository.kt`

**Current (line 73):**
```kotlin
override suspend fun getAll(): List<VoiceCommandDTO> = withContext(Dispatchers.Default) {
    queries.getAllCommands().executeAsList().map { it.toDTO() }
}
```

**Fix Option 1: Explicit Type Parameter**
```kotlin
override suspend fun getAll(): List<VoiceCommandDTO> = withContext(Dispatchers.Default) {
    queries.getAllCommands().executeAsList().map { cmd: Commands_static ->
        cmd.toDTO()
    }
}
```

**Fix Option 2: Qualified Extension Call**
```kotlin
import com.augmentalis.database.dto.VoiceCommandDTO
import com.augmentalis.database.dto.toDTO as commandToDTO

override suspend fun getAll(): List<VoiceCommandDTO> = withContext(Dispatchers.Default) {
    queries.getAllCommands().executeAsList().map { it.commandToDTO() }
}
```

**Fix Option 3: Explicit Conversion Function**
```kotlin
override suspend fun getAll(): List<VoiceCommandDTO> = withContext(Dispatchers.Default) {
    queries.getAllCommands().executeAsList().map(::convertToDTO)
}

private fun convertToDTO(cmd: Commands_static): VoiceCommandDTO = cmd.toDTO()
```

**Recommended:** Option 1 (most explicit, least invasive)

**Validation:**
```bash
./gradlew :Modules:VoiceOS:core:database:compileDebugKotlinAndroid
```

---

### Phase 4: Fix Missing DTO Parameters (1-2 hours)

**Task 4.1: Identify All ScrapedWebsite Instantiations**

**Command:**
```bash
grep -rn "ScrapedWebsite(" Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/
```

**Task 4.2: Review Current DTO Signature**

**File:** `/Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/ScrapedWebsiteDTO.kt`

**Expected Constructor:**
```kotlin
data class ScrapedWebsiteDTO(
    val url: String,
    val packageName: String,
    val appName: String,              // MISSING in old calls
    val firstScrapedAt: Long,         // MISSING in old calls
    val lastScrapedAt: Long,          // MISSING in old calls
    val scrapedElements: List<...>,
    // ... other fields
)
```

**Task 4.3: Update AccessibilityScrapingIntegration.kt**

**File:** `/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt`

**Current (estimated around line 278):**
```kotlin
ScrapedWebsite(
    url = url,
    packageName = packageName,
    scrapedElements = elements
)
```

**Fix:**
```kotlin
ScrapedWebsite(
    url = url,
    packageName = packageName,
    appName = packageManager.getApplicationLabel(
        packageManager.getApplicationInfo(packageName, 0)
    ).toString(),
    firstScrapedAt = System.currentTimeMillis(),
    lastScrapedAt = System.currentTimeMillis(),
    scrapedElements = elements
)
```

**Alternative (if appName already available):**
```kotlin
ScrapedWebsite(
    url = url,
    packageName = packageName,
    appName = event.packageName?.toString() ?: packageName,
    firstScrapedAt = System.currentTimeMillis(),
    lastScrapedAt = System.currentTimeMillis(),
    scrapedElements = elements
)
```

**Task 4.4: Search for Other DTO Instantiation Sites**
```bash
grep -rn "ScrapedWebsite(" Modules/VoiceOS/apps/VoiceOSCore/
grep -rn "ScrapedWebsiteDTO(" Modules/VoiceOS/
```

**Validation:**
```bash
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:compileDebugKotlin
```

---

### Phase 5: Full Build Validation (30 min)

**Task 5.1: Clean Build All Modules**
```bash
./gradlew clean
./gradlew :Modules:VoiceOS:core:database:build
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnAppDev:assembleDebug
```

**Task 5.2: Run Database Tests**
```bash
./gradlew :Modules:VoiceOS:core:database:test
```

**Task 5.3: Run Integration Tests (if available)**
```bash
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:testDebugUnitTest
```

**Success Criteria:**
- ✅ All 3 VoiceOS apps compile successfully
- ✅ All database tests pass (120+ tests)
- ✅ No type resolution errors
- ✅ No missing parameter errors

---

### Phase 6: Documentation & Commit (30 min)

**Task 6.1: Update Schema Migration Log**

**File:** `/Docs/VoiceOS/Technical/NAV-VOS-Schema-Migration-Log-251219-V1.md`

**Contents:**
- Date of fix
- Root cause analysis
- Files modified
- Validation results
- Lessons learned

**Task 6.2: Create Git Commit**

```bash
git add Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightVoiceCommandRepository.kt \
Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt

git commit -m "fix(schema): resolve migration errors blocking compilation

Type resolution ambiguity in SQLDelightVoiceCommandRepository
Missing DTO parameters in AccessibilityScrapingIntegration

- Add explicit type annotations to avoid toDTO() ambiguity
- Add missing ScrapedWebsite parameters (appName, firstScrapedAt, lastScrapedAt)
- Full build validation passed (all 3 apps compile)

Ref: NAV-Plan-SchemaMigrationFix-251219-V1.md"
```

---

## Risk Analysis

### Risk 1: Additional Hidden Issues
**Probability:** MEDIUM (40%)
**Impact:** Delays by 1-2 hours
**Mitigation:** Incremental validation after each fix

### Risk 2: Breaking Existing Functionality
**Probability:** LOW (20%)
**Impact:** Requires rollback and redesign
**Mitigation:** Git commits after each validated phase

### Risk 3: Regeneration Surfaces More Errors
**Probability:** MEDIUM (30%)
**Impact:** Adds 2-3 hours
**Mitigation:** Have rollback strategy ready

---

## Success Metrics

| Metric | Target | Validation Method |
|--------|--------|-------------------|
| Compilation Success | 100% (3/3 apps) | `./gradlew assembleDebug` |
| Test Pass Rate | 100% (120+ tests) | `./gradlew test` |
| Type Errors | 0 | Compiler output |
| DTO Errors | 0 | Compiler output |
| Build Time | <5 min | Gradle build cache |

---

## Rollback Plan

If fixes introduce regressions:

```bash
# Rollback to Week 1 completion state
git reset --hard HEAD~1

# Alternative: Stash current work
git stash save "schema-migration-fix-attempt-1"

# Return to known-good state
git checkout <week1-completion-commit>
```

---

## Future Prevention

**Recommendation 1: Schema Evolution Checklist**
- Update .sq files
- Regenerate SQLDelight code
- Update DTO constructors
- Update all repository implementations
- Update all integration layer calls
- Run full test suite

**Recommendation 2: Pre-commit Hooks**
- Add compilation check to git hooks
- Require all tests to pass before commit
- Flag DTO constructor signature changes

**Recommendation 3: Living Documentation**
- Maintain schema evolution log
- Document all breaking changes
- Link related files in comments

---

## Task Breakdown (for TodoWrite)

1. ✅ Week 1 P0 fixes complete
2. 🔄 Diagnose type resolution ambiguity (30 min)
3. ⏳ Diagnose missing DTO parameters (30 min)
4. ⏳ Regenerate SQLDelight code (30 min)
5. ⏳ Fix type resolution in VoiceCommandRepository (1-2 hours)
6. ⏳ Fix missing parameters in AccessibilityScrapingIntegration (1-2 hours)
7. ⏳ Full build validation (30 min)
8. ⏳ Create documentation & commit (30 min)

**Total Estimated Time:** 4-6 hours (sequential) / 2-3 hours (optimistic with regeneration)

---

## Next Steps

1. Execute Phase 1 (Diagnosis) - understand exact error context
2. Attempt Phase 2 (Regeneration) - quick win if successful
3. If regeneration insufficient, proceed with Phase 3-4 (Manual fixes)
4. Validate with Phase 5 (Full build)
5. Document with Phase 6 (Commit & log)

---

**Plan Status:** READY FOR EXECUTION
**Recommended Start:** Immediately (blocks production builds)
**Owner:** Implementation team
**Review:** Architecture team (for schema evolution lessons)

---

END OF PLAN
