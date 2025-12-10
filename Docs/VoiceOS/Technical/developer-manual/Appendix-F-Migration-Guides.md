# Appendix F: Migration Guides
## VOS4 Developer Manual

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Living Document
**Framework:** IDEACODE v5.3

---

## Table of Contents

### Part I: Version Migrations
- [F.1 VOS3 to VOS4 Migration](#f1-vos3-to-vos4-migration)
- [F.2 Database Schema Migrations](#f2-database-schema-migrations)
- [F.3 API Breaking Changes](#f3-api-breaking-changes)

### Part II: Android Platform Updates
- [F.4 Android 10 to Android 14](#f4-android-10-to-android-14)
- [F.5 Gradle 7 to Gradle 8](#f5-gradle-7-to-gradle-8)
- [F.6 Kotlin 1.8 to Kotlin 1.9](#f6-kotlin-18-to-kotlin-19)

### Part III: Dependency Updates
- [F.7 Room 2.5 to 2.6](#f7-room-25-to-26)
- [F.8 Hilt 2.44 to 2.48](#f8-hilt-244-to-248)
- [F.9 KAPT to KSP](#f9-kapt-to-ksp)

### Part IV: Breaking Changes Log
- [F.10 v4.0 Breaking Changes](#f10-v40-breaking-changes)
- [F.11 v3.0 to v4.0 Changelog](#f11-v30-to-v40-changelog)

---

## F.1 VOS3 to VOS4 Migration

### F.1.1 Overview

**Migration Scope:** Complete architectural overhaul

**Key Changes:**
1. Unified database schema (LearnApp + Scraping merged)
2. Hash-based element identification (Long IDs → String hashes)
3. Hilt dependency injection (Manual DI → Hilt)
4. Kotlin coroutines (RxJava → Coroutines)
5. Jetpack Compose UI (XML layouts → Compose)
6. Multi-module architecture (Monolith → Modules)

**Estimated Migration Time:** 40-80 hours

**Risk Level:** High (complete rewrite)

### F.1.2 Step-by-Step Migration

#### Step 1: Backup VOS3 Data

```bash
# Backup database
adb shell
run-as com.augmentalis.voiceos.v3
cd databases
cp voiceos.db /sdcard/voiceos_v3_backup.db
exit

# Pull backup
adb pull /sdcard/voiceos_v3_backup.db ./backup/
```

#### Step 2: Export VOS3 Data

```kotlin
// VOS3 Data Export Script
class VOS3DataExporter(private val context: Context) {

    suspend fun exportToJson(outputFile: File) {
        val database = VOS3Database.getInstance(context)

        val export = VOS3Export(
            apps = database.appDao().getAllApps(),
            elements = database.elementDao().getAllElements(),
            commands = database.commandDao().getAllCommands(),
            exportDate = System.currentTimeMillis()
        )

        val json = Gson().toJson(export)
        outputFile.writeText(json)
    }
}

data class VOS3Export(
    val apps: List<VOS3AppEntity>,
    val elements: List<VOS3ElementEntity>,
    val commands: List<VOS3CommandEntity>,
    val exportDate: Long
)
```

#### Step 3: Transform VOS3 Data to VOS4 Format

```kotlin
class VOS3toVOS4Transformer {

    fun transformApp(v3App: VOS3AppEntity): AppEntity {
        return AppEntity(
            packageName = v3App.packageName,
            appId = UUID.randomUUID().toString(),  // Generate new UUID
            appName = v3App.appName,
            versionCode = v3App.versionCode.toLong(),
            versionName = v3App.versionName,
            appHash = calculateAppHash(v3App.packageName, v3App.versionCode),

            // VOS3 only had scraping data (not exploration)
            explorationStatus = null,
            scrapedElementCount = v3App.elementCount,
            commandCount = v3App.commandCount,
            scrapingMode = AppEntity.MODE_DYNAMIC,
            isFullyLearned = false,

            // Timestamps
            firstScraped = v3App.firstScraped,
            lastScraped = v3App.lastScraped
        )
    }

    fun transformElement(
        v3Element: VOS3ElementEntity,
        appId: String
    ): ScrapedElementEntity {
        return ScrapedElementEntity(
            // VOS4 uses hash as PK (not Long ID)
            elementHash = calculateElementHash(v3Element),
            appId = appId,
            uuid = null,  // Generate later if needed

            className = v3Element.className,
            viewIdResourceName = v3Element.resourceId,
            text = v3Element.text,
            contentDescription = v3Element.contentDesc,
            bounds = formatBounds(v3Element.bounds),

            isClickable = v3Element.isClickable,
            isLongClickable = v3Element.isLongClickable,
            isEditable = v3Element.isEditable,
            isScrollable = v3Element.isScrollable,
            isCheckable = v3Element.isCheckable,
            isFocusable = v3Element.isFocusable,
            isEnabled = v3Element.isEnabled,

            depth = v3Element.depth,
            indexInParent = v3Element.indexInParent,
            scrapedAt = v3Element.scrapedAt,

            // New VOS4 fields (infer or null)
            semanticRole = inferSemanticRole(v3Element),
            inputType = null,
            visualWeight = null,
            isRequired = null
        )
    }

    fun transformCommand(
        v3Command: VOS3CommandEntity,
        elementHash: String
    ): GeneratedCommandEntity {
        return GeneratedCommandEntity(
            // VOS4 uses element_hash FK (not element_id Long)
            elementHash = elementHash,
            commandText = v3Command.commandText,
            actionType = v3Command.actionType,
            confidence = v3Command.confidence,
            synonyms = v3Command.synonyms.joinToString(","),  // JSON array in VOS4

            isUserApproved = v3Command.isUserApproved,
            usageCount = v3Command.usageCount,
            lastUsed = v3Command.lastUsed,
            generatedAt = v3Command.generatedAt
        )
    }

    private fun calculateElementHash(element: VOS3ElementEntity): String {
        val properties = listOf(
            element.className,
            element.resourceId ?: "",
            element.text ?: "",
            element.contentDesc ?: ""
        ).joinToString("|")

        return MessageDigest.getInstance("SHA-256")
            .digest(properties.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun inferSemanticRole(element: VOS3ElementEntity): String {
        return when {
            element.className.contains("Button") -> "button"
            element.className.contains("EditText") -> "input"
            element.className.contains("CheckBox") -> "checkbox"
            else -> "unknown"
        }
    }
}
```

#### Step 4: Import into VOS4

```kotlin
class VOS4DataImporter(
    private val appDao: AppDao,
    private val elementDao: ScrapedElementDao,
    private val commandDao: GeneratedCommandDao,
    private val database: VoiceOSAppDatabase
) {

    suspend fun importFromVOS3(v3Export: VOS3Export) {
        val transformer = VOS3toVOS4Transformer()

        database.withTransaction {
            // 1. Import apps
            val appMap = mutableMapOf<String, String>()  // v3PackageName -> v4AppId

            v3Export.apps.forEach { v3App ->
                val v4App = transformer.transformApp(v3App)
                appDao.insert(v4App)
                appMap[v3App.packageName] = v4App.appId
            }

            // 2. Import elements
            val elementMap = mutableMapOf<Long, String>()  // v3ElementId -> v4ElementHash

            v3Export.elements.forEach { v3Element ->
                val appId = appMap[v3Element.packageName]
                    ?: throw IllegalStateException("App not found for ${v3Element.packageName}")

                val v4Element = transformer.transformElement(v3Element, appId)
                elementDao.insert(v4Element)
                elementMap[v3Element.id] = v4Element.elementHash
            }

            // 3. Import commands
            v3Export.commands.forEach { v3Command ->
                val elementHash = elementMap[v3Command.elementId]
                    ?: throw IllegalStateException("Element not found for ${v3Command.elementId}")

                val v4Command = transformer.transformCommand(v3Command, elementHash)
                commandDao.insert(v4Command)
            }
        }

        Log.d(TAG, "VOS3 import complete:")
        Log.d(TAG, "  Apps: ${v3Export.apps.size}")
        Log.d(TAG, "  Elements: ${v3Export.elements.size}")
        Log.d(TAG, "  Commands: ${v3Export.commands.size}")
    }
}
```

#### Step 5: Verify Migration

```kotlin
class MigrationVerifier(
    private val appDao: AppDao,
    private val elementDao: ScrapedElementDao,
    private val commandDao: GeneratedCommandDao
) {

    suspend fun verifyMigration(v3Export: VOS3Export): VerificationResult {
        val issues = mutableListOf<String>()

        // Check app count
        val v4AppCount = appDao.getAppCount()
        if (v4AppCount != v3Export.apps.size) {
            issues.add("App count mismatch: VOS3=${v3Export.apps.size}, VOS4=$v4AppCount")
        }

        // Check element count
        val v4ElementCount = elementDao.getElementCount()
        if (v4ElementCount != v3Export.elements.size) {
            issues.add("Element count mismatch: VOS3=${v3Export.elements.size}, VOS4=$v4ElementCount")
        }

        // Check command count
        val v4CommandCount = commandDao.getCommandCount()
        if (v4CommandCount != v3Export.commands.size) {
            issues.add("Command count mismatch: VOS3=${v3Export.commands.size}, VOS4=$v4CommandCount")
        }

        // Check FK integrity
        val orphanedElements = elementDao.getElementsWithInvalidAppId()
        if (orphanedElements.isNotEmpty()) {
            issues.add("Found ${orphanedElements.size} elements with invalid app_id")
        }

        val orphanedCommands = commandDao.getCommandsWithInvalidElementHash()
        if (orphanedCommands.isNotEmpty()) {
            issues.add("Found ${orphanedCommands.size} commands with invalid element_hash")
        }

        return VerificationResult(
            success = issues.isEmpty(),
            issues = issues
        )
    }
}

data class VerificationResult(
    val success: Boolean,
    val issues: List<String>
)
```

---

## F.2 Database Schema Migrations

### F.2.1 Migration 1 to 2

**Changes:**
- Unified `apps` table (merged LearnApp + Scraping)
- Unique constraint on `element_hash`
- Changed `generated_commands` FK from `element_id` to `element_hash`

**SQL Migration:**
```sql
-- Backup existing tables
ALTER TABLE apps RENAME TO apps_old;
ALTER TABLE scraped_apps RENAME TO scraped_apps_old;

-- Create new unified apps table
CREATE TABLE apps (
    package_name TEXT NOT NULL PRIMARY KEY,
    app_id TEXT NOT NULL,
    app_name TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    version_name TEXT NOT NULL,
    app_hash TEXT NOT NULL,
    -- ... (all other fields)
);

-- Migrate data from scraped_apps_old (priority)
INSERT OR REPLACE INTO apps (...)
SELECT ... FROM scraped_apps_old;

-- Migrate data from apps_old (no conflicts)
INSERT OR REPLACE INTO apps (...)
SELECT ... FROM apps_old
WHERE package_name NOT IN (SELECT package_name FROM apps);

-- Update element_hash to unique
CREATE UNIQUE INDEX index_scraped_elements_element_hash ON scraped_elements(element_hash);

-- Migrate generated_commands
CREATE TABLE generated_commands_new (...);
INSERT INTO generated_commands_new (...)
SELECT gc.*, se.element_hash
FROM generated_commands gc
INNER JOIN scraped_elements se ON gc.element_id = se.id;

DROP TABLE generated_commands;
ALTER TABLE generated_commands_new RENAME TO generated_commands;

-- Cleanup
DROP TABLE apps_old;
DROP TABLE scraped_apps_old;
```

### F.2.2 Migration 2 to 3

**Changes:**
- Added feature flags (`learn_app_enabled`, `dynamic_scraping_enabled`, `max_scrape_depth`)

**SQL Migration:**
```sql
ALTER TABLE apps ADD COLUMN learn_app_enabled INTEGER NOT NULL DEFAULT 1;
ALTER TABLE apps ADD COLUMN dynamic_scraping_enabled INTEGER NOT NULL DEFAULT 1;
ALTER TABLE apps ADD COLUMN max_scrape_depth INTEGER DEFAULT NULL;
```

### F.2.3 Migration 3 to 4

**Changes:**
- Updated FK constraints to point to unified `apps` table
- Dropped `scraped_apps` table

**SQL Migration:**
```sql
-- Recreate scraped_elements with FK to apps
ALTER TABLE scraped_elements RENAME TO scraped_elements_old;

CREATE TABLE scraped_elements (
    -- ... (all columns)
    FOREIGN KEY(app_id) REFERENCES apps(app_id) ON DELETE CASCADE
);

INSERT INTO scraped_elements SELECT * FROM scraped_elements_old;
DROP TABLE scraped_elements_old;

-- Recreate screen_contexts with FK to apps
ALTER TABLE screen_contexts RENAME TO screen_contexts_old;

CREATE TABLE screen_contexts (
    -- ... (all columns)
    FOREIGN KEY(app_id) REFERENCES apps(app_id) ON DELETE CASCADE
);

INSERT INTO screen_contexts SELECT * FROM screen_contexts_old;
DROP TABLE screen_contexts_old;

-- Drop scraped_apps table
DROP TABLE IF EXISTS scraped_apps;
```

---

## F.3 API Breaking Changes

### F.3.1 VOS4.0 Breaking Changes

**1. ScrapedAppDao Removed**

**Before (VOS3):**
```kotlin
val scrapedAppDao = database.scrapedAppDao()
val app = scrapedAppDao.getApp(packageName)
```

**After (VOS4):**
```kotlin
val appDao = database.appDao()
val app = appDao.getApp(packageName)
```

**2. Element ID Changed from Long to Hash**

**Before (VOS3):**
```kotlin
@Entity
data class ScrapedElementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // ...
)

val element = elementDao.getElementById(12345L)
```

**After (VOS4):**
```kotlin
@Entity
data class ScrapedElementEntity(
    @PrimaryKey val elementHash: String,
    // ...
)

val element = elementDao.getElementByHash("abc123...")
```

**3. Generated Commands FK Changed**

**Before (VOS3):**
```kotlin
@Entity
data class GeneratedCommandEntity(
    val elementId: Long,  // FK to scraped_elements.id
    // ...
)
```

**After (VOS4):**
```kotlin
@Entity
data class GeneratedCommandEntity(
    val elementHash: String,  // FK to scraped_elements.element_hash
    // ...
)
```

**4. RxJava Removed**

**Before (VOS3):**
```kotlin
fun getApps(): Single<List<AppEntity>>
fun observeApps(): Observable<List<AppEntity>>
```

**After (VOS4):**
```kotlin
suspend fun getApps(): List<AppEntity>
fun getAppsFlow(): Flow<List<AppEntity>>
```

---

## F.4 Android 10 to Android 14

### F.4.1 Scoped Storage

**Change:** Android 11+ requires scoped storage

**Before (Android 10):**
```kotlin
// Direct file access
val file = File("/sdcard/Downloads/data.json")
file.writeText(data)
```

**After (Android 11+):**
```kotlin
// Use MediaStore
val resolver = context.contentResolver
val contentValues = ContentValues().apply {
    put(MediaStore.Downloads.DISPLAY_NAME, "data.json")
    put(MediaStore.Downloads.MIME_TYPE, "application/json")
}

val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
uri?.let {
    resolver.openOutputStream(it)?.use { stream ->
        stream.write(data.toByteArray())
    }
}
```

### F.4.2 Package Visibility

**Change:** Android 11+ restricts package queries

**Before (Android 10):**
```kotlin
val packages = packageManager.getInstalledPackages(0)
```

**After (Android 11+):**
```xml
<!-- AndroidManifest.xml -->
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
    </intent>
</queries>
```

```kotlin
val packages = packageManager.getInstalledPackages(0)  // Now works
```

---

## F.5 Gradle 7 to Gradle 8

### F.5.1 Kotlin DSL Changes

**Before (Gradle 7):**
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdkVersion(34)
}
```

**After (Gradle 8):**
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")  // KSP replaces KAPT
}

android {
    compileSdk = 34  // Use property syntax
}
```

---

## F.6 Kotlin 1.8 to Kotlin 1.9

### F.6.1 Coroutines Changes

**New:** Structured concurrency improvements

**Before (1.8):**
```kotlin
GlobalScope.launch {  // Deprecated
    // ...
}
```

**After (1.9):**
```kotlin
coroutineScope {  // Structured concurrency
    launch {
        // ...
    }
}
```

---

## F.7 Room 2.5 to 2.6

### F.7.1 AutoMigration Support

**New Feature:** Automatic schema migrations

**Before (Manual):**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE apps ADD COLUMN new_field TEXT")
    }
}
```

**After (Auto):**
```kotlin
@Database(
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class MyDatabase : RoomDatabase()
```

---

## F.8 Hilt 2.44 to 2.48

### F.8.1 KSP Support

**New:** Native KSP support (faster than KAPT)

**Before (KAPT):**
```kotlin
plugins {
    kotlin("kapt")
}

dependencies {
    kapt("com.google.dagger:hilt-compiler:2.44")
}
```

**After (KSP):**
```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

---

## F.9 KAPT to KSP

### F.9.1 Complete Migration

**Step 1: Update Plugins**
```kotlin
plugins {
    // Remove KAPT
    // kotlin("kapt")

    // Add KSP
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
}
```

**Step 2: Update Dependencies**
```kotlin
dependencies {
    // Before: KAPT
    // kapt("androidx.room:room-compiler:2.6.0")
    // kapt("com.google.dagger:hilt-compiler:2.48")

    // After: KSP
    ksp("androidx.room:room-compiler:2.6.0")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

**Step 3: Update Source Sets (if needed)**
```kotlin
android {
    sourceSets {
        // Before: KAPT
        // getByName("main").java.srcDirs("build/generated/source/kapt/main")

        // After: KSP
        getByName("main").java.srcDirs("build/generated/ksp/main/kotlin")
    }
}
```

**Benefits:**
- 2-3x faster compilation
- Lower memory usage
- Better error messages

---

## F.10 v4.0 Breaking Changes

### F.10.1 Complete List

**Database:**
1. ❌ `scraped_apps` table removed → Use `apps` table
2. ❌ `ScrapedAppDao` removed → Use `AppDao`
3. ❌ Element IDs changed from `Long` to `String` (hash)
4. ❌ Command FK changed from `element_id` to `element_hash`

**Architecture:**
1. ❌ RxJava removed → Use Kotlin Coroutines + Flow
2. ❌ Manual DI removed → Use Hilt
3. ❌ XML layouts removed → Use Jetpack Compose
4. ❌ Monolithic structure → Multi-module architecture

**APIs:**
1. ❌ `VoiceOSServiceLegacy` removed → Use `VoiceOSService`
2. ❌ Synchronous APIs removed → Use suspend functions
3. ❌ Callbacks removed → Use Flow

**Gradle:**
1. ❌ KAPT removed → Use KSP
2. ❌ Gradle 7 → Gradle 8 required
3. ❌ Kotlin 1.8 → Kotlin 1.9 required

---

## F.11 v3.0 to v4.0 Changelog

### Features Added
- ✅ Unified database (LearnApp + Scraping)
- ✅ Hash-based element identification
- ✅ Multi-engine speech recognition
- ✅ Screen transition tracking
- ✅ User interaction history
- ✅ Element relationship modeling
- ✅ Feature flags for gradual rollout

### Features Removed
- ❌ RxJava support
- ❌ XML layouts
- ❌ Manual dependency injection
- ❌ Separate LearnApp/Scraping databases

### Features Deprecated
- ⚠️ `VoiceOSServiceLegacy` (removed in v5.0)
- ⚠️ `targetSdk` in library modules (use `testOptions.targetSdk`)

---

## Summary

**Migration Paths:**
- **VOS3 → VOS4:** Full data migration supported via export/import
- **Database v1 → v4:** Automatic via Room migrations
- **KAPT → KSP:** Simple plugin swap
- **Android 10 → 14:** Platform compatibility ensured

**Key Takeaways:**
1. Always backup data before migration
2. Test migrations on non-production devices first
3. Use verification scripts to ensure data integrity
4. Follow step-by-step guides for complex migrations

---

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete
**End of Appendices**
