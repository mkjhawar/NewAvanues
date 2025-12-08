# Scraping DAO → SQLDelight Mapping Guide

**Date:** 2025-11-27
**Purpose:** Map all Room DAO calls to SQLDelight repository calls
**Files:** AccessibilityScrapingIntegration.kt, CommandGenerator.kt, VoiceCommandProcessor.kt

---

## Database Reference

**Before:**
```kotlin
private val database: VoiceOSAppDatabase = VoiceOSAppDatabase.getInstance(context)
```

**After:**
```kotlin
private val database: VoiceOSCoreDatabaseAdapter = VoiceOSCoreDatabaseAdapter.getInstance(context)
```

---

## DAO Method Mappings

### App DAO Methods

| Room DAO Call | SQLDelight Replacement | Notes |
|--------------|------------------------|-------|
| `database.appDao().getAppByHash(hash)` | `database.databaseManager.scrapedApps.getByHash(hash)?.toAppEntity()` | Returns DTO, need to convert |
| `database.appDao().insert(app)` | `database.insertApp(app)` | Helper method handles conversion |
| `database.appDao().incrementScrapeCountById(id)` | Custom logic needed | No direct equivalent |
| `database.appDao().updateElementCountById(id, count)` | Custom logic needed | Update via insert |
| `database.appDao().updateCommandCountById(id, count)` | Custom logic needed | Update via insert |
| `database.appDao().updateScrapingModeById(id, mode)` | Custom logic needed | Update via insert |
| `database.appDao().markAsFullyLearnedById(id, time)` | Custom logic needed | Update via insert |

### ScrapedElement DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.scrapedElementDao().insertBatchWithIds(elements)` | `database.databaseManager.scrapedElements.insertBatch(elements)` |
| `database.scrapedElementDao().getElementCountForApp(appId)` | `database.databaseManager.scrapedElements.countByApp(appId)` |
| `database.scrapedElementDao().getElementByHash(hash)` | `database.databaseManager.scrapedElements.getByHash(hash)` |
| `database.scrapedElementDao().updateFormGroupIdBatch(hashes, id)` | Custom logic needed |
| `database.scrapedElementDao().upsertElement(element)` | `database.databaseManager.scrapedElements.upsert(element)` |
| `database.scrapedElementDao().getElementsByAppId(appId)` | `database.databaseManager.scrapedElements.getByApp(appId)` |

### ScrapedHierarchy DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.scrapedHierarchyDao().deleteHierarchyForApp(appId)` | `database.databaseManager.scrapedHierarchies.deleteByApp(appId)` |
| `database.scrapedHierarchyDao().insertBatch(hierarchy)` | `database.databaseManager.scrapedHierarchies.insertBatch(hierarchy)` |

### GeneratedCommand DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.generatedCommandDao().insertBatch(commands)` | `database.databaseManager.generatedCommands.insertBatch(commands)` |

### ScreenContext DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.screenContextDao().getByScreenHash(hash)` | `database.databaseManager.screenContexts.getByHash(hash)` |
| `database.screenContextDao().incrementVisitCount(hash, time)` | Custom logic needed |
| `database.screenContextDao().insert(context)` | `database.databaseManager.screenContexts.insert(context)` |
| `database.screenContextDao().getScreenByHash(hash)` | `database.databaseManager.screenContexts.getByHash(hash)` |

### ElementRelationship DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.elementRelationshipDao().insertAll(relationships)` | `database.databaseManager.elementRelationships.insertBatch(relationships)` |

### ScreenTransition DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.screenTransitionDao().recordTransition(...)` | `database.databaseManager.screenTransitions.insert(...)` |

### UserInteraction DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.userInteractionDao().insert(interaction)` | `database.databaseManager.userInteractions.insert(interaction)` |

### ElementStateHistory DAO Methods

| Room DAO Call | SQLDelight Replacement |
|--------------|------------------------|
| `database.elementStateHistoryDao().insertOrIgnore(state)` | `database.databaseManager.elementStateHistory.insertOrIgnore(state)` |

---

## Entity Construction Issues

### ScrapedApp Entity

**Problem:** ScrapedApp constructor parameters don't match DTO

**Need to check:**
- appId parameter vs constructor
- appHash parameter vs constructor
- firstScraped parameter vs constructor
- scrapeCount parameter vs constructor
- scrapingMode parameter vs constructor

**Solution:** Use proper parameter names or create via DTO conversion

---

## Implementation Strategy

1. ✅ Replace database reference (VoiceOSAppDatabase → VoiceOSCoreDatabaseAdapter)
2. ✅ Create LauncherDetector stub
3. ✅ Create UUIDCreatorDatabase stub
4. ⏭️ Replace all DAO calls with SQLDelight repository calls
5. ⏭️ Fix entity construction parameter mismatches
6. ⏭️ Implement custom logic for methods without direct equivalents
7. ⏭️ Test compilation

---

**Status:** In Progress
**Next:** Implement DAO call replacements systematically
