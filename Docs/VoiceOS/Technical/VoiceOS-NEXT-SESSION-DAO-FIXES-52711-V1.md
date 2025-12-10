# Next Session: Complete DAO→SQLDelight Migration

**Date:** 2025-11-27
**Status:** 78 errors remaining (down from 91)
**Priority:** HIGH - Complete Scraping DAO replacements

---

## ✅ Already Completed

1. ✅ LearnApp fully fixed (9 @Transaction errors)
2. ✅ UuidAliasManager stub created
3. ✅ LauncherDetector stub created
4. ✅ UUIDCreatorDatabase stub created
5. ✅ Database reference updated: `VoiceOSAppDatabase` → `VoiceOSCoreDatabaseAdapter`

---

## 🔧 Remaining Work (78 errors)

### Critical DAO Replacements Needed

All in `AccessibilityScrapingIntegration.kt` - **40+ DAO method calls**

#### Pattern 1: Direct Repository Access (Simple)

These have direct SQLDelight equivalents:

```kotlin
// OLD: database.scrapedHierarchyDao().deleteHierarchyForApp(appId)
// NEW: database.databaseManager.scrapedHierarchies.deleteByApp(appId)

// OLD: database.scrapedHierarchyDao().insertBatch(hierarchy)
// NEW: database.databaseManager.scrapedHierarchies.insertBatch(hierarchy)

// OLD: database.generatedCommandDao().insertBatch(commands)
// NEW: database.databaseManager.generatedCommands.insertBatch(commands)

// OLD: database.elementRelationshipDao().insertAll(relationships)
// NEW: database.databaseManager.elementRelationships.insertBatch(relationships)

// OLD: database.screenContextDao().insert(screenContext)
// NEW: database.databaseManager.screenContexts.insert(screenContext)

// OLD: database.screenContextDao().getByScreenHash(hash)
// NEW: database.databaseManager.screenContexts.getByHash(hash)

// OLD: database.scrapedElementDao().getElementByHash(hash)
// NEW: database.databaseManager.scrapedElements.getByHash(hash)

// OLD: database.userInteractionDao().insert(interaction)
// NEW: database.databaseManager.userInteractions.insert(interaction)

// OLD: database.elementStateHistoryDao().insertOrIgnore(state)
// NEW: database.databaseManager.elementStateHistory.insertOrIgnore(state)
```

#### Pattern 2: Complex Updates (Need Custom Logic)

These need helper methods or custom implementation:

**Line 315:** `database.appDao().getAppByHash(hash)`
```kotlin
// TODO: Create helper method in VoiceOSCoreDatabaseAdapter
// suspend fun getAppByHash(hash: String): AppEntity?
```

**Line 321:** `database.appDao().incrementScrapeCountById(id)`
```kotlin
// TODO: Get app, increment count, update
// val app = database.getApp(packageName)
// if (app != null) {
//     database.updateApp(app.copy(scrapeCount = app.scrapeCount + 1))
// }
```

**Line 377:** `database.scrapedElementDao().insertBatchWithIds(elements)`
```kotlin
// TODO: This returns List<Long> of assigned IDs
// SQLDelight approach: Insert batch, then query back IDs
// This is complex - may need to implement in repository
```

**Line 395:** `database.scrapedElementDao().getElementCountForApp(appId)`
```kotlin
// NEW: database.databaseManager.scrapedElements.countByApp(appId)
```

**Line 543:** `database.screenContextDao().incrementVisitCount(hash, time)`
```kotlin
// TODO: Get context, increment, update
// val context = database.databaseManager.screenContexts.getByHash(hash)
// if (context != null) {
//     database.databaseManager.screenContexts.update(
//         context.copy(visitCount = context.visitCount + 1, lastVisitedAt = time)
//     )
// }
```

**Line 604:** `database.scrapedElementDao().updateFormGroupIdBatch(hashes, groupId)`
```kotlin
// TODO: Batch update formGroupId for multiple element hashes
// May need custom SQL or loop through elements
```

**Line 747:** `database.appDao().updateElementCountById(id, count)`
```kotlin
// TODO: Get app by ID, update count
```

**Line 748:** `database.appDao().updateCommandCountById(id, count)`
```kotlin
// TODO: Get app by ID, update count
```

**Line 725:** `database.screenTransitionDao().recordTransition(...)`
```kotlin
// NEW: database.databaseManager.screenTransitions.insert(ScreenTransitionDTO(...))
```

---

### Entity Construction Errors (~7)

**ScrapedApp entity - Parameter name mismatches:**

Lines with errors:
- Line 332: Cannot find parameter: `appId`
- Line 336: Cannot find parameter: `appHash`
- Line 337: Cannot find parameter: `firstScraped`
- Line 339: Cannot find parameter: `scrapeCount`
- Line 340: Cannot find parameter: `scrapingMode`

**Fix:** Check ScrapedApp entity constructor and use correct parameter names, or create via DTO:

```kotlin
// Instead of:
val app = ScrapedApp(
    appId = ...,
    appHash = ...,
    firstScraped = ...,
    scrapeCount = ...,
    scrapingMode = MODE_DYNAMIC
)

// Use:
val appDTO = ScrapedAppDTO(
    appId = packageName,
    packageName = packageName,
    versionCode = versionCode,
    versionName = versionName,
    appHash = appHash,
    isFullyLearned = 0,
    scrapingMode = "DYNAMIC",
    scrapeCount = 1,
    elementCount = 0,
    commandCount = 0,
    firstScrapedAt = System.currentTimeMillis(),
    lastScrapedAt = System.currentTimeMillis()
)
database.databaseManager.scrapedApps.insert(appDTO)
```

---

### Missing Constants

**ScrapingMode.MODE_DYNAMIC** and similar constants need to be defined or replaced with strings:

```kotlin
// OLD: AppEntity.MODE_DYNAMIC
// NEW: "DYNAMIC"

// OLD: AppEntity.MODE_LEARN_APP
// NEW: "LEARN_APP"
```

---

## 📋 Implementation Strategy

### Step 1: Add Helper Methods to VoiceOSCoreDatabaseAdapter

```kotlin
// In VoiceOSCoreDatabaseAdapter.kt

suspend fun getAppByHash(hash: String): AppEntity? {
    return databaseManager.scrapedApps.getByHash(hash)?.toAppEntity()
}

suspend fun incrementScrapeCount(packageName: String) {
    val app = getApp(packageName)
    if (app != null) {
        // Increment scrape count and update
        val dto = app.toScrapedAppDTO().copy(
            scrapeCount = app.toScrapedAppDTO().scrapeCount + 1
        )
        databaseManager.scrapedApps.insert(dto)
    }
}

// Similar helpers for other increment/update operations
```

### Step 2: Replace Simple DAO Calls

Use find & replace for straightforward mappings:

1. `database.scrapedHierarchyDao()` → `database.databaseManager.scrapedHierarchies`
2. `database.generatedCommandDao()` → `database.databaseManager.generatedCommands`
3. `database.elementRelationshipDao()` → `database.databaseManager.elementRelationships`
4. `database.screenContextDao()` → `database.databaseManager.screenContexts`
5. `database.scrapedElementDao()` → `database.databaseManager.scrapedElements`
6. `database.userInteractionDao()` → `database.databaseManager.userInteractions`
7. `database.elementStateHistoryDao()` → `database.databaseManager.elementStateHistory`
8. `database.screenTransitionDao()` → `database.databaseManager.screenTransitions`

### Step 3: Handle Complex Cases

For increment/update operations, use the helper methods created in Step 1.

### Step 4: Fix Entity Construction

Replace AppEntity/ScrapedApp construction with DTO construction where needed.

### Step 5: Test Compilation

```bash
./gradlew :modules:apps:VoiceOSCore:compileDebugKotlin
```

Target: 0 errors

---

## 📊 Expected Results

**Before:** 78 errors
**After:** 0 errors (100% compilation success)
**Time Estimate:** 2-3 hours for complete DAO migration

---

## 🔗 Related Files

**Files to Modify:**
1. `AccessibilityScrapingIntegration.kt` - Main file with 40+ DAO calls
2. `CommandGenerator.kt` - Check for DAO calls
3. `VoiceCommandProcessor.kt` - Check for DAO calls
4. `VoiceOSCoreDatabaseAdapter.kt` - Add helper methods

**Files Already Modified:**
1. ✅ `LearnAppRepository.kt` - All @Transaction converted
2. ✅ `LearnAppDatabaseAdapter.kt` - Transaction method added
3. ✅ `LearnAppDao.kt` - Transaction method added
4. ✅ `UuidAliasManager.kt` - Stub created
5. ✅ `LauncherDetector.kt` - Stub created
6. ✅ `UUIDCreatorDatabase.kt` - Stub created

---

## 💡 Pro Tips

1. **Use database.databaseManager directly** for all repository access
2. **Check repository interface** to see available methods before replacing
3. **Convert entities to DTOs** when inserting/updating
4. **Use helper methods** for complex operations (increment, batch updates)
5. **Test frequently** - Compile after each batch of changes

---

**Status:** Ready for next session
**Priority:** HIGH
**Estimated Completion:** 2-3 hours

**Next Step:** Start with Step 1 (Add helper methods), then proceed systematically through Steps 2-5.
