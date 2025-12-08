# Database Voice Commands Specification

**Feature:** Voice Commands for Unified Database Interaction
**Version:** 4.1.1
**Status:** Planned
**Priority:** Medium
**Estimated Effort:** 2-3 hours
**Target Release:** v4.1.1

---

## Overview

Add voice commands that allow users to query and interact with the unified VoiceOSAppDatabase directly through voice. This provides visibility into database state, migration status, and app learning progress.

**Goal:** Make the unified database user-accessible through natural voice commands.

---

## Problem Statement

After v4.1 database consolidation:
- Users have no way to query database statistics via voice
- Migration status is invisible to users
- No voice commands for database management (export, clear, optimize)
- Learning progress is not voice-accessible

**User Story:**
> "As a VoiceOS user, I want to ask 'show database stats' and hear how many apps I've learned, so I can track my progress."

---

## Command Categories

### 1. Database Statistics Commands

**Purpose:** Query database metrics and statistics

**Commands:**

| Voice Input | Response Format | Example |
|-------------|----------------|---------|
| "show database stats" | Full statistics summary | "You have 47 apps. 32 fully explored, 15 partially scraped." |
| "how many learned apps" | Learned app count | "32 apps fully learned, 15 partially learned." |
| "how many apps" | Total app count | "47 apps in database." |
| "database size" | Storage metrics | "Database is 12.5 MB with 4,847 elements." |
| "element count" | Total UI elements | "4,847 UI elements across 47 apps." |

**Implementation:**
```kotlin
fun getStatistics(): String {
    val appCount = appDao.getAppCount()
    val fullyLearned = appDao.getFullyLearnedCount()
    val elementCount = scrapedElementDao.getTotalCount()
    val dbSize = getDatabaseFileSize()

    return "You have $appCount apps. $fullyLearned fully explored, " +
           "${appCount - fullyLearned} partial. " +
           "Database is ${formatSize(dbSize)} with $elementCount elements."
}
```

---

### 2. Migration Status Commands

**Purpose:** Check database consolidation migration status

**Commands:**

| Voice Input | Response Format | Example |
|-------------|----------------|---------|
| "migration status" | Migration summary | "Migration complete. 47 apps migrated, 0 errors." |
| "database migration complete" | Boolean status | "Yes, migration completed successfully." |
| "show migrated apps" | Migration details | "Migrated 32 from LearnApp, 15 from Scraping." |
| "migration errors" | Error report | "No migration errors." or "3 apps failed migration." |

**Implementation:**
```kotlin
fun getMigrationStatus(): String {
    val helper = DatabaseMigrationHelper(context)
    return if (helper.isMigrationComplete()) {
        val stats = getMigrationStatistics()
        "Migration complete. ${stats.totalApps} apps migrated, ${stats.errors} errors."
    } else {
        "Migration pending. Will run on next app launch."
    }
}
```

---

### 3. App Query Commands

**Purpose:** Query specific app information

**Commands:**

| Voice Input | Response Format | Example |
|-------------|----------------|---------|
| "list learned apps" | App list with completion | "Instagram 100%, Twitter 75%, Facebook 50%..." |
| "show app details for [app]" | Detailed app info | "Instagram: 47 screens, 312 elements, last explored 3 days ago." |
| "which apps need learning" | Apps below threshold | "Twitter needs learning: 25% complete." |
| "most learned app" | Top app by completion | "Instagram is most learned with 47 screens." |
| "recently learned apps" | Apps by last explored | "Instagram (today), Twitter (yesterday)..." |

**Implementation:**
```kotlin
fun getLearnedApps(): String {
    val apps = appDao.getAllApps()
        .sortedByDescending { it.exploredElementCount ?: 0 }
        .take(10)

    return apps.joinToString(", ") { app ->
        val percent = calculateCompletionPercent(app)
        "${app.appName} $percent%"
    }
}

fun getAppDetails(appName: String): String {
    val app = appDao.getAppByName(appName) ?: return "App not found."

    val screens = app.totalScreens ?: 0
    val elements = app.exploredElementCount ?: 0
    val lastExplored = formatTimestamp(app.lastExplored)

    return "$appName: $screens screens, $elements elements, last explored $lastExplored."
}
```

---

### 4. Database Management Commands

**Purpose:** Perform database operations

**Commands:**

| Voice Input | Action | Response |
|-------------|--------|----------|
| "export database" | Export DB to file | "Exporting... Done. File: voiceos_backup_20251107.db" |
| "clear app data for [app]" | Delete app data | "Cleared Instagram data. 47 screens removed." |
| "optimize database" | Run VACUUM | "Optimizing... Reduced size by 2.3MB." |
| "backup database" | Create backup | "Backup created: voiceos_backup_20251107.db" |
| "database integrity check" | Verify integrity | "Database integrity OK. No errors found." |

**Implementation:**
```kotlin
fun exportDatabase(): String {
    val dbFile = context.getDatabasePath("voiceos_app_database")
    val exportFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "voiceos_backup_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.db"
    )

    dbFile.copyTo(exportFile, overwrite = true)
    return "Exported to ${exportFile.name}"
}

fun clearAppData(packageName: String): String {
    val app = appDao.getApp(packageName) ?: return "App not found."

    // Foreign key cascades will delete related data
    appDao.delete(app)

    return "Cleared ${app.appName} data. All elements and commands removed."
}

fun optimizeDatabase(): String {
    val beforeSize = getDatabaseFileSize()
    database.query("VACUUM", null)
    val afterSize = getDatabaseFileSize()
    val saved = beforeSize - afterSize

    return "Optimized database. Reduced size by ${formatSize(saved)}."
}
```

---

### 5. Mode Switching Commands

**Purpose:** Switch between exploration and scraping modes

**Commands:**

| Voice Input | Action | Response |
|-------------|--------|----------|
| "switch to exploration mode" | Enable LEARN_APP | "Switched to exploration mode. Systematic learning enabled." |
| "switch to dynamic mode" | Enable DYNAMIC | "Switched to dynamic mode. Real-time scraping enabled." |
| "what mode am I in" | Query current mode | "Currently in exploration mode." |
| "enable learn app" | Activate LearnApp | "LearnApp enabled. Say 'learn [app]' to start." |

**Implementation:**
```kotlin
fun switchMode(mode: ScrapingMode): String {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString("scraping_mode", mode.name)
        .apply()

    return when (mode) {
        ScrapingMode.LEARN_APP -> "Switched to exploration mode."
        ScrapingMode.DYNAMIC -> "Switched to dynamic mode."
    }
}
```

---

## Implementation Plan

### Phase 1: Core Handler Class

**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/commands/DatabaseCommandHandler.kt`

**Structure:**
```kotlin
class DatabaseCommandHandler(
    private val context: Context,
    private val database: VoiceOSAppDatabase
) {
    // Category 1: Statistics
    fun getStatistics(): String
    fun getAppCount(): String
    fun getElementCount(): String
    fun getDatabaseSize(): String

    // Category 2: Migration
    fun getMigrationStatus(): String
    fun getMigrationDetails(): String

    // Category 3: App Queries
    fun getLearnedApps(): String
    fun getAppDetails(appName: String): String
    fun getAppsNeedingLearning(): String

    // Category 4: Management
    fun exportDatabase(): String
    fun clearAppData(packageName: String): String
    fun optimizeDatabase(): String

    // Category 5: Mode Switching
    fun switchMode(mode: ScrapingMode): String
    fun getCurrentMode(): String

    // Command routing
    fun handleCommand(command: String): String?
}
```

**CoT: Design Decision - Return String? (nullable)**
- Returns `String` if command handled
- Returns `null` if command not recognized (allows fallback to other handlers)
- Allows chaining with existing command system

---

### Phase 2: Command Pattern Matching

**Pattern Recognition:**

```kotlin
private fun handleCommand(command: String): String? {
    val normalized = command.lowercase().trim()

    return when {
        // Statistics patterns
        normalized.matches(".*show database stats.*".toRegex()) -> getStatistics()
        normalized.matches(".*how many (learned )?apps.*".toRegex()) -> getAppCount()
        normalized.matches(".*database size.*".toRegex()) -> getDatabaseSize()
        normalized.matches(".*element count.*".toRegex()) -> getElementCount()

        // Migration patterns
        normalized.matches(".*migration status.*".toRegex()) -> getMigrationStatus()
        normalized.matches(".*show migrated apps.*".toRegex()) -> getMigrationDetails()

        // App query patterns
        normalized.matches(".*list learned apps.*".toRegex()) -> getLearnedApps()
        normalized.matches(".*show app details for (.+)".toRegex()) -> {
            val appName = ".*show app details for (.+)".toRegex()
                .find(normalized)?.groupValues?.get(1)
            appName?.let { getAppDetails(it) }
        }
        normalized.matches(".*which apps need learning.*".toRegex()) -> getAppsNeedingLearning()

        // Management patterns
        normalized.matches(".*export database.*".toRegex()) -> exportDatabase()
        normalized.matches(".*clear app data for (.+)".toRegex()) -> {
            val appName = ".*clear app data for (.+)".toRegex()
                .find(normalized)?.groupValues?.get(1)
            appName?.let { clearAppData(it) }
        }
        normalized.matches(".*optimize database.*".toRegex()) -> optimizeDatabase()

        // Mode switching patterns
        normalized.matches(".*switch to exploration mode.*".toRegex()) ->
            switchMode(ScrapingMode.LEARN_APP)
        normalized.matches(".*switch to dynamic mode.*".toRegex()) ->
            switchMode(ScrapingMode.DYNAMIC)
        normalized.matches(".*what mode.*".toRegex()) -> getCurrentMode()

        // Not a database command
        else -> null
    }
}
```

---

### Phase 3: Integration with VoiceCommandProcessor

**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/VoiceCommandProcessor.kt`

**Integration Point:** Add database command handler before system commands

```kotlin
class VoiceCommandProcessor(private val context: Context) {

    private val database = VoiceOSAppDatabase.getInstance(context)
    private val databaseCommandHandler = DatabaseCommandHandler(context, database)

    suspend fun processVoiceInput(input: String): VoiceCommandResult {

        // 1. Try dynamic app-specific commands (existing)
        val dynamicResult = tryDynamicCommand(input)
        if (dynamicResult != null) return dynamicResult

        // 2. Try database commands (NEW - add before system commands)
        val databaseResult = databaseCommandHandler.handleCommand(input)
        if (databaseResult != null) {
            return VoiceCommandResult.Success(databaseResult)
        }

        // 3. Try static system commands (existing)
        val systemResult = trySystemCommand(input)
        if (systemResult != null) return systemResult

        // 4. No match found
        return VoiceCommandResult.NoMatch
    }
}
```

**CoT: Priority Ordering**
1. Dynamic commands first (app-specific, highest priority)
2. Database commands second (new category)
3. System commands third (fallback)
4. No match last

**Why this order?**
- Dynamic commands are context-specific (current app)
- Database commands are VoiceOS-specific (our feature)
- System commands are generic (Android system)

---

### Phase 4: DAO Extensions

**Add helper methods to existing DAOs:**

**AppDao.kt additions:**
```kotlin
@Query("SELECT COUNT(*) FROM apps")
suspend fun getAppCount(): Int

@Query("SELECT COUNT(*) FROM apps WHERE is_fully_learned = 1")
suspend fun getFullyLearnedCount(): Int

@Query("SELECT * FROM apps WHERE app_name LIKE '%' || :name || '%' LIMIT 1")
suspend fun getAppByName(name: String): AppEntity?

@Query("""
    SELECT * FROM apps
    WHERE (explored_element_count * 100.0 / NULLIF(explored_element_count + scraped_element_count, 0)) < 50
    ORDER BY explored_element_count DESC
    LIMIT 10
""")
suspend fun getAppsNeedingLearning(): List<AppEntity>
```

**ScrapedElementDao.kt additions:**
```kotlin
@Query("SELECT COUNT(*) FROM scraped_elements")
suspend fun getTotalCount(): Int

@Query("SELECT COUNT(*) FROM scraped_elements WHERE app_id = :appId")
suspend fun getElementCountForApp(appId: String): Int
```

---

### Phase 5: Testing

**Unit Tests:** `DatabaseCommandHandlerTest.kt`

```kotlin
class DatabaseCommandHandlerTest {

    @Test
    fun `getStatistics returns correct format`() {
        val handler = DatabaseCommandHandler(context, mockDatabase)
        val result = handler.getStatistics()

        assertTrue(result.contains("apps"))
        assertTrue(result.contains("fully explored"))
    }

    @Test
    fun `handleCommand routes show database stats correctly`() {
        val handler = DatabaseCommandHandler(context, mockDatabase)
        val result = handler.handleCommand("show database stats")

        assertNotNull(result)
        assertTrue(result!!.contains("apps"))
    }

    @Test
    fun `handleCommand returns null for unrecognized command`() {
        val handler = DatabaseCommandHandler(context, mockDatabase)
        val result = handler.handleCommand("unrelated command")

        assertNull(result)
    }
}
```

**Integration Tests:** Device testing with real voice input

```kotlin
@Test
fun `voice command show database stats executes`() {
    // Trigger voice recognition
    voiceInput("show database stats")

    // Verify response
    val response = waitForVoiceResponse()
    assertTrue(response.contains("apps"))
}
```

---

## Command Reference Summary

**Total Commands:** 20 voice commands across 5 categories

| Category | Commands | Priority |
|----------|----------|----------|
| Statistics | 5 | High |
| Migration | 4 | Medium |
| App Queries | 5 | High |
| Management | 4 | Low |
| Mode Switching | 4 | Medium |

---

## User Experience Flow

**Example Interaction:**

```
User: "Show database stats"
VoiceOS: "You have 47 apps. 32 fully explored, 15 partial.
         Database is 12.5 MB with 4,847 elements."

User: "Which apps need learning?"
VoiceOS: "Twitter needs learning: 25% complete.
         Facebook: 35% complete.
         WhatsApp: 40% complete."

User: "Show app details for Instagram"
VoiceOS: "Instagram: 47 screens, 312 elements,
         last explored 3 days ago. 100% complete."

User: "Export database"
VoiceOS: "Exporting database to Downloads... Done.
         File: voiceos_backup_20251107.db"
```

---

## Error Handling

**Database Access Errors:**
```kotlin
try {
    val stats = getStatistics()
    return stats
} catch (e: SQLException) {
    Log.e(TAG, "Database error: ${e.message}", e)
    return "Unable to access database. Please try again."
}
```

**Permission Errors (Export):**
```kotlin
if (!hasStoragePermission()) {
    return "Storage permission required to export database."
}
```

**App Not Found:**
```kotlin
val app = appDao.getAppByName(appName)
if (app == null) {
    return "App '$appName' not found in database."
}
```

---

## Performance Considerations

**Query Optimization:**
- All database queries use `suspend` functions (coroutines)
- Statistics queries cached for 5 seconds
- Expensive queries (list all apps) limited to 10 results

**Response Time:**
- Target: <500ms for all commands
- Cached statistics: <50ms
- Database queries: <200ms
- File operations (export): 1-3 seconds

---

## Success Metrics

**Quantitative:**
- 20 voice commands implemented
- <500ms average response time
- 100% test coverage for command handler
- Zero crashes from database commands

**Qualitative:**
- Natural language recognition works
- Responses are clear and concise
- Commands feel intuitive to use
- Provides value to power users

---

## Future Enhancements (Post v4.1.1)

**v4.2 Potential Additions:**
- Voice-driven database queries ("show me apps with more than 50 elements")
- Natural language app search ("find social media apps")
- Database comparison ("compare learning progress to last week")
- Voice-activated backup scheduling
- Database analytics dashboard integration

---

## Dependencies

**Required:**
- VoiceOSAppDatabase (v4.1+)
- VoiceCommandProcessor (existing)
- Room DAOs (AppDao, ScrapedElementDao)
- Coroutines (kotlinx-coroutines)

**Optional:**
- Storage permissions (for export/backup)
- PreferenceManager (for mode switching)

---

## Implementation Checklist

- [ ] Create DatabaseCommandHandler.kt
- [ ] Add DAO helper methods (getAppCount, etc.)
- [ ] Implement command pattern matching
- [ ] Integrate with VoiceCommandProcessor
- [ ] Add unit tests (DatabaseCommandHandlerTest.kt)
- [ ] Add integration tests (voice input → response)
- [ ] Test all 20 voice commands on device
- [ ] Verify response times <500ms
- [ ] Update documentation (Chapter 3, API reference)
- [ ] Create user guide for database voice commands

---

**Estimated Time:** 2-3 hours
**Files to Create:** 2 (handler + tests)
**Files to Modify:** 3 (VoiceCommandProcessor, AppDao, ScrapedElementDao)
**Total Lines of Code:** ~400-500 lines

---

**Status:** ⏳ Documented, ready for implementation
**Next Step:** Implement DatabaseCommandHandler.kt with CoT comments
