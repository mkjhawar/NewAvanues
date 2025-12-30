# VoiceOS 4 ScreenContext API Reference

**Created:** 2025-10-18 22:52:00 PDT
**Author:** Manoj Jhawar
**Module:** VoiceOSCore
**Package:** `com.augmentalis.voiceoscore.scraping.entities`, `com.augmentalis.voiceoscore.scraping.dao`
**Phase:** 2 (AI Context Inference)

## Overview

The ScreenContext API provides screen-level context information for accessibility scraping, enabling AI to understand application flows, navigation patterns, and screen purpose. This is part of Phase 2's context inference capabilities.

### Purpose

- Capture high-level context about screens/windows where elements appear
- Enable AI to understand user flows and navigation patterns
- Classify screens by type (login, checkout, settings, etc.)
- Track screen visit patterns and navigation hierarchy
- Support form context understanding for better command suggestions

### Key Components

| Component | Type | Purpose |
|-----------|------|---------|
| `ScreenContextEntity` | Entity | Room database entity for screen context |
| `ScreenContextDao` | DAO | Data access object for screen context operations |

---

## ScreenContextEntity

### Package
```kotlin
package com.augmentalis.voiceoscore.scraping.entities
```

### Entity Declaration
```kotlin
@Entity(
    tableName = "screen_contexts",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedAppEntity::class,
            parentColumns = ["app_id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screen_hash"], unique = true),
        Index("app_id"),
        Index("package_name"),
        Index("screen_type")
    ]
)
data class ScreenContextEntity(...)
```

### Properties

| Property | Type | Column Name | Description |
|----------|------|-------------|-------------|
| `id` | `Long` | `id` | Auto-generated primary key |
| `screenHash` | `String` | `screen_hash` | MD5 hash of screen signature (packageName + activityName + window title) |
| `appId` | `String` | `app_id` | Foreign key to ScrapedAppEntity |
| `packageName` | `String` | `package_name` | Package name of the app |
| `activityName` | `String?` | `activity_name` | Activity/window class name (if available) |
| `windowTitle` | `String?` | `window_title` | Window title or header text |
| `screenType` | `String?` | `screen_type` | Inferred screen type (see Screen Types) |
| `formContext` | `String?` | `form_context` | Form-specific context (see Form Contexts) |
| `navigationLevel` | `Int` | `navigation_level` | Depth in navigation hierarchy (0 = main screen, 1+ = nested) |
| `primaryAction` | `String?` | `primary_action` | Primary user action on this screen (see Primary Actions) |
| `elementCount` | `Int` | `element_count` | Number of interactive elements on screen |
| `hasBackButton` | `Boolean` | `has_back_button` | Whether screen has back navigation |
| `firstScraped` | `Long` | `first_scraped` | Timestamp when screen was first scraped |
| `lastScraped` | `Long` | `last_scraped` | Timestamp when screen was last scraped |
| `visitCount` | `Int` | `visit_count` | Number of times screen has been scraped |

### Screen Types

Inferred screen types (via `ScreenContextInferenceHelper.inferScreenType()`):

| Type | Description | Example Use Cases |
|------|-------------|-------------------|
| `"login"` | Login/authentication screen | Sign in pages, authentication prompts |
| `"signup"` | Registration screen | Account creation, sign up forms |
| `"checkout"` | Checkout/payment screen | Purchase flows, payment pages |
| `"cart"` | Shopping cart screen | Cart/basket/bag views |
| `"settings"` | Settings/preferences screen | App settings, configuration |
| `"home"` | Main/home screen | App dashboard, main feed |
| `"search"` | Search screen | Search interfaces, find pages |
| `"profile"` | User profile screen | Account info, profile pages |
| `"detail"` | Detail view screen | Item details, info pages |
| `"list"` | List/browse screen | Browse results, list views |
| `"form"` | Generic form screen | Multi-field input forms |

### Form Contexts

Form-specific context values (via `ScreenContextInferenceHelper.inferFormContext()`):

| Context | Description |
|---------|-------------|
| `"registration"` | User registration form |
| `"payment"` | Payment/billing form |
| `"address"` | Address/shipping form |
| `"contact"` | Contact information form |
| `"feedback"` | Feedback/review form |
| `"search"` | Search form |

### Primary Actions

Primary user actions (via `ScreenContextInferenceHelper.inferPrimaryAction()`):

| Action | Description |
|--------|-------------|
| `"submit"` | Submit/send form data |
| `"search"` | Search/find content |
| `"browse"` | Browse/explore content |
| `"purchase"` | Buy/purchase/checkout |
| `"view"` | View content (default) |

### Database Schema

#### Table: `screen_contexts`

```sql
CREATE TABLE screen_contexts (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    screen_hash TEXT NOT NULL,
    app_id TEXT NOT NULL,
    package_name TEXT NOT NULL,
    activity_name TEXT,
    window_title TEXT,
    screen_type TEXT,
    form_context TEXT,
    navigation_level INTEGER NOT NULL DEFAULT 0,
    primary_action TEXT,
    element_count INTEGER NOT NULL DEFAULT 0,
    has_back_button INTEGER NOT NULL DEFAULT 0,
    first_scraped INTEGER NOT NULL,
    last_scraped INTEGER NOT NULL,
    visit_count INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY(app_id) REFERENCES scraped_apps(app_id) ON DELETE CASCADE
);
```

#### Indices

```sql
CREATE UNIQUE INDEX index_screen_contexts_screen_hash ON screen_contexts(screen_hash);
CREATE INDEX index_screen_contexts_app_id ON screen_contexts(app_id);
CREATE INDEX index_screen_contexts_package_name ON screen_contexts(package_name);
CREATE INDEX index_screen_contexts_screen_type ON screen_contexts(screen_type);
```

### Usage Examples

#### Creating a Screen Context

```kotlin
import com.augmentalis.voiceoscore.scraping.entities.ScreenContextEntity
import java.security.MessageDigest

// Generate screen hash
fun generateScreenHash(packageName: String, activityName: String?, windowTitle: String?): String {
    val signature = "$packageName|$activityName|$windowTitle"
    val md5 = MessageDigest.getInstance("MD5")
    return md5.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
}

// Create screen context
val screenContext = ScreenContextEntity(
    screenHash = generateScreenHash(packageName, activityName, windowTitle),
    appId = "com.example.app",
    packageName = "com.example.app",
    activityName = "com.example.app.LoginActivity",
    windowTitle = "Sign In",
    screenType = "login",
    formContext = null,
    navigationLevel = 0,
    primaryAction = "submit",
    elementCount = 5,
    hasBackButton = false,
    firstScraped = System.currentTimeMillis(),
    lastScraped = System.currentTimeMillis(),
    visitCount = 1
)
```

#### With AI Inference

```kotlin
import com.augmentalis.voiceoscore.scraping.ScreenContextInferenceHelper

val inferenceHelper = ScreenContextInferenceHelper()

// Infer screen properties
val screenType = inferenceHelper.inferScreenType(
    windowTitle = "Checkout",
    activityName = "com.example.app.CheckoutActivity",
    elements = scrapedElements
)

val formContext = inferenceHelper.inferFormContext(scrapedElements)
val primaryAction = inferenceHelper.inferPrimaryAction(scrapedElements)
val navigationLevel = inferenceHelper.inferNavigationLevel(
    hasBackButton = true,
    windowTitle = windowTitle
)

val screenContext = ScreenContextEntity(
    screenHash = generateScreenHash(packageName, activityName, windowTitle),
    appId = appId,
    packageName = packageName,
    activityName = activityName,
    windowTitle = windowTitle,
    screenType = screenType,
    formContext = formContext,
    navigationLevel = navigationLevel,
    primaryAction = primaryAction,
    elementCount = scrapedElements.size,
    hasBackButton = true
)
```

---

## ScreenContextDao

### Package
```kotlin
package com.augmentalis.voiceoscore.scraping.dao
```

### Interface Declaration
```kotlin
@Dao
interface ScreenContextDao
```

### Methods

#### Insert/Update Operations

##### insert()
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insert(screenContext: ScreenContextEntity): Long
```

Inserts or replaces a screen context.

**Parameters:**
- `screenContext`: ScreenContextEntity to insert

**Returns:** Row ID of inserted record

**Example:**
```kotlin
val rowId = screenContextDao.insert(screenContext)
```

##### update()
```kotlin
@Update
suspend fun update(screenContext: ScreenContextEntity)
```

Updates an existing screen context.

**Parameters:**
- `screenContext`: ScreenContextEntity to update

**Example:**
```kotlin
val updated = screenContext.copy(visitCount = screenContext.visitCount + 1)
screenContextDao.update(updated)
```

##### incrementVisitCount()
```kotlin
@Query("UPDATE screen_contexts SET visit_count = visit_count + 1, last_scraped = :timestamp WHERE screen_hash = :screenHash")
suspend fun incrementVisitCount(screenHash: String, timestamp: Long)
```

Increments visit count and updates last scraped timestamp.

**Parameters:**
- `screenHash`: Hash of the screen to update
- `timestamp`: New timestamp for last_scraped

**Example:**
```kotlin
screenContextDao.incrementVisitCount(screenHash, System.currentTimeMillis())
```

#### Query Operations

##### getByScreenHash()
```kotlin
@Query("SELECT * FROM screen_contexts WHERE screen_hash = :screenHash")
suspend fun getByScreenHash(screenHash: String): ScreenContextEntity?
```

Retrieves a screen context by its hash.

**Parameters:**
- `screenHash`: Hash of the screen

**Returns:** ScreenContextEntity or null if not found

**Example:**
```kotlin
val screenContext = screenContextDao.getByScreenHash(screenHash)
if (screenContext != null) {
    // Screen exists, update visit count
    screenContextDao.incrementVisitCount(screenHash, System.currentTimeMillis())
} else {
    // New screen, insert
    screenContextDao.insert(newScreenContext)
}
```

##### getScreensForApp()
```kotlin
@Query("SELECT * FROM screen_contexts WHERE app_id = :appId ORDER BY last_scraped DESC")
suspend fun getScreensForApp(appId: String): List<ScreenContextEntity>
```

Gets all screen contexts for a specific app, ordered by most recent.

**Parameters:**
- `appId`: Application identifier

**Returns:** List of ScreenContextEntity

**Example:**
```kotlin
val appScreens = screenContextDao.getScreensForApp("com.example.app")
println("App has ${appScreens.size} known screens")
```

##### getScreensByType()
```kotlin
@Query("SELECT * FROM screen_contexts WHERE screen_type = :screenType ORDER BY last_scraped DESC")
suspend fun getScreensByType(screenType: String): List<ScreenContextEntity>
```

Gets all screens of a specific type.

**Parameters:**
- `screenType`: Type of screen (e.g., "login", "checkout")

**Returns:** List of ScreenContextEntity

**Example:**
```kotlin
// Find all login screens across all apps
val loginScreens = screenContextDao.getScreensByType("login")
```

##### getMostVisitedScreens()
```kotlin
@Query("SELECT * FROM screen_contexts WHERE app_id = :appId ORDER BY visit_count DESC LIMIT :limit")
suspend fun getMostVisitedScreens(appId: String, limit: Int = 10): List<ScreenContextEntity>
```

Gets the most frequently visited screens for an app.

**Parameters:**
- `appId`: Application identifier
- `limit`: Maximum number of results (default: 10)

**Returns:** List of ScreenContextEntity ordered by visit count

**Example:**
```kotlin
val topScreens = screenContextDao.getMostVisitedScreens("com.example.app", limit = 5)
topScreens.forEach { screen ->
    println("${screen.windowTitle}: ${screen.visitCount} visits")
}
```

##### getRecentScreens()
```kotlin
@Query("SELECT * FROM screen_contexts ORDER BY last_scraped DESC LIMIT :limit")
suspend fun getRecentScreens(limit: Int = 20): List<ScreenContextEntity>
```

Gets recently accessed screens across all apps.

**Parameters:**
- `limit`: Maximum number of results (default: 20)

**Returns:** List of ScreenContextEntity ordered by last scraped

**Example:**
```kotlin
val recentScreens = screenContextDao.getRecentScreens(limit = 10)
```

#### Delete Operations

##### deleteScreensForApp()
```kotlin
@Query("DELETE FROM screen_contexts WHERE app_id = :appId")
suspend fun deleteScreensForApp(appId: String): Int
```

Deletes all screens for a specific app.

**Parameters:**
- `appId`: Application identifier

**Returns:** Number of rows deleted

**Example:**
```kotlin
val deleted = screenContextDao.deleteScreensForApp("com.example.app")
println("Deleted $deleted screens")
```

##### deleteOldScreens()
```kotlin
@Query("DELETE FROM screen_contexts WHERE last_scraped < :timestamp")
suspend fun deleteOldScreens(timestamp: Long): Int
```

Deletes screens not visited since the specified timestamp.

**Parameters:**
- `timestamp`: Cutoff timestamp (screens older than this are deleted)

**Returns:** Number of rows deleted

**Example:**
```kotlin
// Delete screens not visited in 30 days
val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
val deleted = screenContextDao.deleteOldScreens(thirtyDaysAgo)
```

#### Statistics Operations

##### getScreenCount()
```kotlin
@Query("SELECT COUNT(*) FROM screen_contexts")
suspend fun getScreenCount(): Int
```

Gets total number of screen contexts.

**Returns:** Total screen count

**Example:**
```kotlin
val totalScreens = screenContextDao.getScreenCount()
```

##### getScreenCountForApp()
```kotlin
@Query("SELECT COUNT(*) FROM screen_contexts WHERE app_id = :appId")
suspend fun getScreenCountForApp(appId: String): Int
```

Gets number of screens for a specific app.

**Parameters:**
- `appId`: Application identifier

**Returns:** Screen count for app

**Example:**
```kotlin
val appScreenCount = screenContextDao.getScreenCountForApp("com.example.app")
```

---

## Integration with Scraping Pipeline

### Complete Integration Example

```kotlin
class AccessibilityScraper(
    private val screenContextDao: ScreenContextDao,
    private val scrapedElementDao: ScrapedElementDao
) {
    private val inferenceHelper = ScreenContextInferenceHelper()

    suspend fun scrapeScreen(
        packageName: String,
        activityName: String?,
        windowTitle: String?,
        rootNode: AccessibilityNodeInfo
    ) {
        // 1. Scrape elements first
        val elements = scrapeElements(rootNode)
        scrapedElementDao.insertAll(elements)

        // 2. Generate screen hash
        val screenHash = generateScreenHash(packageName, activityName, windowTitle)

        // 3. Check if screen exists
        val existingScreen = screenContextDao.getByScreenHash(screenHash)

        if (existingScreen != null) {
            // Update existing screen
            screenContextDao.incrementVisitCount(screenHash, System.currentTimeMillis())
        } else {
            // 4. Infer screen context
            val screenType = inferenceHelper.inferScreenType(windowTitle, activityName, elements)
            val formContext = inferenceHelper.inferFormContext(elements)
            val primaryAction = inferenceHelper.inferPrimaryAction(elements)
            val hasBackButton = elements.any { it.contentDescription?.contains("back", ignoreCase = true) == true }
            val navigationLevel = inferenceHelper.inferNavigationLevel(hasBackButton, windowTitle)

            // 5. Create screen context
            val screenContext = ScreenContextEntity(
                screenHash = screenHash,
                appId = packageName,
                packageName = packageName,
                activityName = activityName,
                windowTitle = windowTitle,
                screenType = screenType,
                formContext = formContext,
                navigationLevel = navigationLevel,
                primaryAction = primaryAction,
                elementCount = elements.size,
                hasBackButton = hasBackButton
            )

            // 6. Insert screen context
            screenContextDao.insert(screenContext)
        }
    }
}
```

---

## Use Cases

### 1. Screen Type Analysis

```kotlin
// Analyze screen type distribution for an app
suspend fun analyzeAppScreenTypes(appId: String): Map<String, Int> {
    val screens = screenContextDao.getScreensForApp(appId)
    return screens
        .groupBy { it.screenType ?: "unknown" }
        .mapValues { it.value.size }
}

// Result: {"login": 1, "home": 1, "settings": 2, "form": 3}
```

### 2. Navigation Path Reconstruction

```kotlin
// Get user's recent navigation path
suspend fun getRecentNavigationPath(limit: Int = 10): List<String> {
    return screenContextDao.getRecentScreens(limit)
        .map { it.windowTitle ?: it.activityName ?: "Unknown" }
}

// Result: ["Settings", "Home", "Profile", "Login"]
```

### 3. Form Screen Discovery

```kotlin
// Find all form screens for better command suggestions
suspend fun findFormScreens(): List<ScreenContextEntity> {
    return screenContextDao.getScreensByType("form")
}
```

### 4. User Behavior Analysis

```kotlin
// Analyze which screens users visit most
suspend fun analyzeUserBehavior(appId: String) {
    val topScreens = screenContextDao.getMostVisitedScreens(appId, limit = 5)

    topScreens.forEach { screen ->
        println("Screen: ${screen.windowTitle}")
        println("  Type: ${screen.screenType}")
        println("  Visits: ${screen.visitCount}")
        println("  Primary Action: ${screen.primaryAction}")
    }
}
```

---

## Performance Considerations

### Indexing Strategy

1. **screen_hash** (unique): Fast lookups for screen existence checks
2. **app_id**: Fast filtering by application
3. **package_name**: Fast package-based queries
4. **screen_type**: Fast type-based filtering

### Query Optimization

```kotlin
// Efficient: Uses index on screen_hash
val screen = screenContextDao.getByScreenHash(screenHash)

// Efficient: Uses index on app_id + ordering
val appScreens = screenContextDao.getScreensForApp(appId)

// Efficient: Uses index on screen_type
val loginScreens = screenContextDao.getScreensByType("login")
```

### Memory Management

- Use pagination for large result sets
- Clean up old screens periodically using `deleteOldScreens()`
- Limit result sizes with LIMIT clauses

---

## Thread Safety

All DAO methods are `suspend` functions and must be called from:
- Coroutines
- Within a coroutine scope
- Using `runBlocking` (not recommended for production)

**Example:**
```kotlin
viewModelScope.launch {
    val screen = screenContextDao.getByScreenHash(screenHash)
    // Process screen
}
```

---

## Testing Examples

### Unit Test Example

```kotlin
@RunWith(AndroidJUnit4::class)
class ScreenContextDaoTest {

    @Test
    fun testInsertAndRetrieve() = runBlocking {
        val screenContext = ScreenContextEntity(
            screenHash = "test_hash_123",
            appId = "com.test.app",
            packageName = "com.test.app",
            activityName = "MainActivity",
            windowTitle = "Test Screen",
            screenType = "login",
            formContext = null,
            navigationLevel = 0,
            primaryAction = "submit",
            elementCount = 5,
            hasBackButton = false
        )

        screenContextDao.insert(screenContext)

        val retrieved = screenContextDao.getByScreenHash("test_hash_123")
        assertNotNull(retrieved)
        assertEquals("login", retrieved?.screenType)
    }

    @Test
    fun testIncrementVisitCount() = runBlocking {
        val screenHash = "test_hash_456"
        val screenContext = createTestScreen(screenHash)

        screenContextDao.insert(screenContext)
        screenContextDao.incrementVisitCount(screenHash, System.currentTimeMillis())

        val updated = screenContextDao.getByScreenHash(screenHash)
        assertEquals(2, updated?.visitCount)
    }
}
```

---

## Related APIs

- **ElementRelationshipEntity**: Models relationships between UI elements on screens
- **ScreenTransitionEntity**: Tracks navigation between screens
- **ScreenContextInferenceHelper**: Helper for inferring screen context
- **ScrapedElementEntity**: Individual UI elements on screens
- **ScrapedAppEntity**: Application-level data

---

## Migration Notes

### Adding to Existing Database

If adding to an existing Room database, include migration:

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE screen_contexts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                screen_hash TEXT NOT NULL,
                app_id TEXT NOT NULL,
                package_name TEXT NOT NULL,
                activity_name TEXT,
                window_title TEXT,
                screen_type TEXT,
                form_context TEXT,
                navigation_level INTEGER NOT NULL DEFAULT 0,
                primary_action TEXT,
                element_count INTEGER NOT NULL DEFAULT 0,
                has_back_button INTEGER NOT NULL DEFAULT 0,
                first_scraped INTEGER NOT NULL,
                last_scraped INTEGER NOT NULL,
                visit_count INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(app_id) REFERENCES scraped_apps(app_id) ON DELETE CASCADE
            )
        """)

        database.execSQL("CREATE UNIQUE INDEX index_screen_contexts_screen_hash ON screen_contexts(screen_hash)")
        database.execSQL("CREATE INDEX index_screen_contexts_app_id ON screen_contexts(app_id)")
        database.execSQL("CREATE INDEX index_screen_contexts_package_name ON screen_contexts(package_name)")
        database.execSQL("CREATE INDEX index_screen_contexts_screen_type ON screen_contexts(screen_type)")
    }
}
```

---

**Version:** 1.0.0
**Last Updated:** 2025-10-18 22:52:00 PDT
**Author:** Manoj Jhawar
**Module:** VoiceOSCore (Phase 2)
**Related Documentation:**
- ElementRelationship API Reference
- ScreenTransition API Reference
- ScreenContextInferenceHelper API Reference
