# Quick Start: Task 3 - Pagination Implementation

**Read Full Handover:** `/contextsave/VoiceOS-Handover-P2-Tasks-51213.md`

---

## 🚀 Instant Setup (30 seconds)

```bash
# 1. Set JDK 17 (CRITICAL - do this first!)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 2. Verify it worked
java -version  # Should show: 17.0.13

# 3. Test current build
./gradlew :Modules:VoiceOS:core:database:compileDebugKotlinAndroid

# Expected: BUILD SUCCESSFUL ✅
```

---

## ✅ What's Already Done (Tasks 1 & 2)

1. **Fixed plan documentation** - Replaced `Dispatchers.IO` with `Dispatchers.Default`
2. **Created singleton tests** - 10 test cases for VoiceOSDatabaseManager
3. **Fixed existing tests** - PaginationTest now compiles cleanly

**Status:** All builds passing ✅, 2 of 3 tasks complete ✅

---

## 📋 What You Need to Do: Task 3

**Goal:** Implement pagination by app package name

**Current Problem:**
```kotlin
// Line 163-169 in SQLDelightGeneratedCommandRepository.kt
override suspend fun getByPackagePaginated(...): List<GeneratedCommandDTO> {
    // Returns empty list - NOT IMPLEMENTED!
    emptyList()
}
```

**Root Cause:** Missing `appId` field in `GeneratedCommand` SQL schema

---

## 🎯 Implementation Steps (3.5-4 hours)

### Step 1: Update SQL Schema (30 min)
**File:** `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/GeneratedCommand.sq`

Add after existing table definition:
```sql
-- Add appId column
ALTER TABLE commands_generated
ADD COLUMN appId TEXT DEFAULT '' NOT NULL;

-- Add index for pagination
CREATE INDEX IF NOT EXISTS idx_gc_app_id
ON commands_generated(appId, id);

-- Pagination query
getByPackagePaginated:
SELECT * FROM commands_generated
WHERE appId = :packageName
ORDER BY id ASC
LIMIT :limit
OFFSET :offset;

-- Keyset pagination (faster)
getByPackageKeysetPaginated:
SELECT * FROM commands_generated
WHERE appId = :packageName AND id > :lastId
ORDER BY id ASC
LIMIT :limit;
```

**Verify:**
```bash
./gradlew :Modules:VoiceOS:core:database:compileCommonMainKotlinMetadata
# Should regenerate SQLDelight code with new queries
```

---

### Step 2: Update DTO (5 min)
**File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/GeneratedCommandDTO.kt`

Add field:
```kotlin
data class GeneratedCommandDTO(
    val id: Long,
    val elementHash: String,
    val commandText: String,
    val actionType: String,
    val confidence: Double,
    val synonyms: String?,
    val isUserApproved: Long = 0,
    val usageCount: Long = 0,
    val lastUsed: Long? = null,
    val createdAt: Long,
    val appId: String = ""  // ← ADD THIS
)
```

---

### Step 3: Implement Repository (20 min)
**File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightGeneratedCommandRepository.kt`

Replace lines 163-169:
```kotlin
override suspend fun getByPackagePaginated(
    packageName: String,
    limit: Int,
    offset: Int
): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
    require(packageName.isNotEmpty()) { "Package name cannot be empty" }
    require(limit in 1..1000) { "Limit must be between 1 and 1000" }
    require(offset >= 0) { "Offset must be >= 0" }

    queries.getByPackagePaginated(packageName, limit.toLong(), offset.toLong())
        .executeAsList()
        .map { it.toGeneratedCommandDTO() }
}
```

**Verify:**
```bash
./gradlew :Modules:VoiceOS:core:database:compileDebugKotlinAndroid
```

---

### Step 4: Update Interface (10 min)
**File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IGeneratedCommandRepository.kt`

Add method signature (find the right location in interface):
```kotlin
/**
 * Get commands for a package with pagination.
 */
suspend fun getByPackagePaginated(
    packageName: String,
    limit: Int,
    offset: Int
): List<GeneratedCommandDTO>
```

---

### Step 5: Update All GeneratedCommandDTO Creations (60 min)

**Find all usages:**
```bash
grep -r "GeneratedCommandDTO(" Modules/VoiceOS/ --include="*.kt" -n
```

**For each usage, add `appId` parameter:**
```kotlin
// BEFORE:
GeneratedCommandDTO(
    id = 0,
    // ... other fields ...
    createdAt = System.currentTimeMillis()
)

// AFTER:
GeneratedCommandDTO(
    id = 0,
    // ... other fields ...
    createdAt = System.currentTimeMillis(),
    appId = packageName  // ← ADD THIS (get from context)
)
```

**Common files to update:**
- `LearnAppCore.kt` (main command generation)
- All test files creating GeneratedCommandDTO
- Any integration code

---

### Step 6: Add Tests (45 min)

**Create:** `Modules/VoiceOS/core/database/src/androidInstrumentedTest/kotlin/com/augmentalis/database/PaginationByPackageTest.kt`

**Template provided in full handover document** (lines 650-750)

Key tests:
- ✅ Returns correct commands for package
- ✅ Pagination limits work
- ✅ Offset works correctly
- ✅ Input validation
- ✅ Empty results when no matches

---

### Step 7: Verification (30 min)

```bash
# Compile everything
./gradlew :Modules:VoiceOS:core:database:build

# Compile tests
./gradlew :Modules:VoiceOS:core:database:compileDebugAndroidTestKotlin

# Run tests (if emulator available)
./gradlew :Modules:VoiceOS:core:database:connectedAndroidTest
```

---

## 🚨 Critical Rules

1. **ALWAYS use JDK 17** (not 21, not 24)
2. **Use `Dispatchers.Default`** in commonMain (NOT Dispatchers.IO)
3. **SQLite booleans are Long** (0L = false, 1L = true)
4. **Test after each step** (don't batch compilation checks)
5. **Use `appId = ""` as default** for backward compatibility

---

## 📖 Full Documentation

**Comprehensive Handover:** `/contextsave/VoiceOS-Handover-P2-Tasks-51213.md` (900+ lines)

Contains:
- Detailed implementation plan with code examples
- Migration strategy options
- Full test suite template
- Troubleshooting guide
- Lessons learned from Tasks 1 & 2

---

## ✅ Verification Checklist

Before starting:
- [ ] JDK 17 verified
- [ ] Current branch: `VoiceOS-Development`
- [ ] Read full handover document
- [ ] Latest code pulled

After each step:
- [ ] Step compiles successfully
- [ ] No regression in existing tests
- [ ] SQLDelight code regenerates (if schema changed)

Before commit:
- [ ] All builds passing
- [ ] All tests compiling
- [ ] No hardcoded values
- [ ] Followed KMP rules (no Dispatchers.IO, no System.*, etc.)

---

## 📊 Progress Tracking

**Estimated Time:** 3.5-4 hours
**Difficulty:** Medium (mostly tedious updates)

| Step | Time | Status |
|------|------|--------|
| 1. SQL Schema | 30 min | ⏳ Pending |
| 2. DTO Update | 5 min | ⏳ Pending |
| 3. Repository | 20 min | ⏳ Pending |
| 4. Interface | 10 min | ⏳ Pending |
| 5. Update Usages | 60 min | ⏳ Pending |
| 6. Add Tests | 45 min | ⏳ Pending |
| 7. Verification | 30 min | ⏳ Pending |

---

**You're all set! Start with the full handover report for complete context, then follow the 7 steps above.**

**Good luck! 🚀**
