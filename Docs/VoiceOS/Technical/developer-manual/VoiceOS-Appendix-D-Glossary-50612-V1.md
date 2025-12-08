# Appendix D: Glossary
## VOS4 Developer Manual

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Living Document
**Framework:** IDEACODE v5.3

---

## Table of Contents

- [D.1 Acronyms & Abbreviations](#d1-acronyms--abbreviations)
- [D.2 Technical Terms](#d2-technical-terms)
- [D.3 VOS4-Specific Terms](#d3-vos4-specific-terms)
- [D.4 Android Platform Terms](#d4-android-platform-terms)
- [D.5 Database Terms](#d5-database-terms)
- [D.6 Architecture Patterns](#d6-architecture-patterns)

---

## D.1 Acronyms & Abbreviations

### A

**AAR (Android Archive)**
- Android library package format
- Contains compiled code, resources, and manifest
- Used for distributing Android libraries
- Example: `voiceoscore.aar`

**ANR (Application Not Responding)**
- Android dialog shown when app blocks UI thread >5 seconds
- Caused by long-running operations on main thread
- Solution: Use coroutines/background threads

**API (Application Programming Interface)**
- Set of functions/classes exposed for external use
- Defines how software components interact
- VOS4 public APIs documented in Appendix A

**APK (Android Package)**
- Android application package file format
- Contains compiled code, resources, and assets
- Installed on Android devices

**AOSP (Android Open Source Project)**
- Open-source Android operating system
- VOS4 built on AOSP foundation

### C

**CCA (Claude Code AI)**
- AI code reviewer for VOS4
- Ensures code quality and consistency

**COT (Chain of Thought)**
- Reasoning process before implementation
- Documented in code comments
- Required by IDEACODE protocol

**CTA (Call to Action)**
- Primary action button on screen
- Used in screen classification

### D

**DAO (Data Access Object)**
- Interface for database operations
- Room generates implementation
- Example: `AppDao`, `ScrapedElementDao`

**DI (Dependency Injection)**
- Design pattern for providing dependencies
- VOS4 uses Hilt for DI
- Reduces coupling between components

**DSL (Domain-Specific Language)**
- Language designed for specific domain
- MagicUI uses DSL for UI generation

**DYNAMIC Mode**
- Real-time scraping during normal app use
- Opportunistic element discovery
- Contrast: LEARN_APP mode (systematic exploration)

### E

**ER Diagram (Entity-Relationship Diagram)**
- Visual database schema representation
- Shows tables and relationships
- Appendix B contains VOS4 ER diagram

### F

**FK (Foreign Key)**
- Database constraint linking tables
- Ensures referential integrity
- VOS4 uses CASCADE delete for cleanup

### H

**Hilt**
- Dependency injection library for Android
- Built on Dagger
- Simplifies DI configuration

### I

**IDEACODE**
- Development framework for VOS4
- Current version: v5.3
- Defines protocols and standards

### K

**KMP (Kotlin Multiplatform)**
- Share code across platforms (Android, iOS, desktop)
- Future VOS4 expansion strategy
- Enables cross-platform voice OS

**KSP (Kotlin Symbol Processing)**
- Annotation processing for Kotlin
- Replaces KAPT (faster, better)
- Used for Room, Hilt code generation

### L

**LEARN_APP Mode**
- Systematic app exploration
- Builds complete navigation graph
- Contrast: DYNAMIC mode (opportunistic scraping)

### O

**OOM (Out of Memory)**
- Memory allocation failure
- Common cause: Not recycling AccessibilityNodeInfo
- Solution: Proper resource management

### P

**PK (Primary Key)**
- Unique identifier for database row
- VOS4 uses natural keys (package names) where possible
- Example: `apps.package_name`

### R

**ROT (Reflection on Thought)**
- Post-implementation validation
- Reviews COT decisions
- Required by IDEACODE protocol

**Room**
- Android SQLite abstraction library
- Compile-time SQL verification
- VOS4 database layer

### S

**SDK (Software Development Kit)**
- Tools for developing software
- Android SDK, Vivoka SDK, etc.
- Provides APIs, libraries, documentation

**SQL (Structured Query Language)**
- Database query language
- Used by Room under the hood
- Direct SQL in migrations

**STT (Speech-to-Text)**
- Convert speech to text
- Core VOS4 functionality
- Multiple engine support (Google, Vivoka, Whisper)

**SRP (Single Responsibility Principle)**
- Each class has one reason to change
- Part of SOLID principles
- VOS4 architectural guideline

### T

**TTS (Text-to-Speech)**
- Convert text to speech
- Used for voice feedback
- Android TTS integration

### U

**UI (User Interface)**
- Visual elements user interacts with
- VOS4 scrapes UI for voice commands
- Compose-based UI

**UUID (Universally Unique Identifier)**
- 128-bit identifier
- Format: `550e8400-e29b-41d4-a716-446655440000`
- Used for app IDs, element IDs

### V

**VOS (VoiceOS)**
- Voice-enabled operating system
- Current version: VOS4
- Previous versions: VOS3, VOS2, VOS1

### W

**WAL (Write-Ahead Logging)**
- SQLite journal mode
- Improves concurrency
- Enabled in VOS4 database

---

## D.2 Technical Terms

### Accessibility Service
Android service that receives UI events and can interact with UI elements. VOS4's core component for scraping and control.

**Key Methods:**
- `onAccessibilityEvent()` - Receive UI events
- `onServiceConnected()` - Service initialization
- `rootInActiveWindow` - Get root UI node

**Permissions Required:**
```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```

### Accessibility Tree
Hierarchical representation of UI elements exposed by Android accessibility framework.

**Structure:**
```
Root (AccessibilityNodeInfo)
├── ViewGroup
│   ├── TextView (text: "Welcome")
│   └── Button (text: "Submit")
└── ViewGroup
    └── EditText (hint: "Enter name")
```

**Traversal:**
```kotlin
fun traverse(node: AccessibilityNodeInfo) {
    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        traverse(child)
        child.recycle()  // Important!
    }
}
```

### Annotation Processing
Compile-time code generation from annotations.

**VOS4 Uses:**
- `@Entity` → Room generates table schema
- `@Dao` → Room generates implementation
- `@Inject` → Hilt generates DI code
- `@AndroidEntryPoint` → Hilt integration

**Configuration:**
```kotlin
plugins {
    id("com.google.devtools.ksp")  // Kotlin Symbol Processing
}

dependencies {
    ksp("androidx.room:room-compiler:2.6.0")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

### Cascading Delete
Automatic deletion of child records when parent is deleted.

**Example:**
```sql
FOREIGN KEY(app_id) REFERENCES apps(app_id) ON DELETE CASCADE
```

When app deleted:
```sql
DELETE FROM apps WHERE package_name = 'com.example.app';
```

Automatically deletes:
- All `scraped_elements` for that app
- All `generated_commands` for those elements
- All related data (cascades through FKs)

### Coroutine
Kotlin feature for asynchronous programming.

**Key Concepts:**
- `suspend fun` - Can be paused/resumed
- `launch` - Fire and forget
- `async/await` - Return result
- `Dispatchers.IO` - I/O operations
- `Dispatchers.Main` - UI updates

**VOS4 Usage:**
```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val data = database.loadData()  // Background thread
    withContext(Dispatchers.Main) {
        updateUI(data)  // Main thread
    }
}
```

### Element Hash
SHA-256 hash of UI element properties for unique identification.

**Calculation:**
```kotlin
fun calculateElementHash(element: AccessibilityNodeInfo): String {
    val properties = listOf(
        element.className,
        element.viewIdResourceName,
        element.text,
        element.contentDescription,
        element.bounds.flattenToString()
    ).joinToString("|")

    return MessageDigest.getInstance("SHA-256")
        .digest(properties.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
```

**Properties:**
- Unique per element configuration
- Stable across app sessions (same UI → same hash)
- Used as primary key for `scraped_elements`

### Flow (Kotlin)
Reactive stream of data that emits values over time.

**Room Integration:**
```kotlin
@Dao
interface AppDao {
    @Query("SELECT * FROM apps")
    fun getAllAppsFlow(): Flow<List<AppEntity>>  // Reactive
}

// Usage
viewModel.appsFlow.collect { apps ->
    updateUI(apps)  // Called whenever data changes
}
```

**Benefits:**
- Automatic UI updates
- Lifecycle-aware
- Backpressure handling

### Gesture Dispatching
Programmatically simulating touch gestures via accessibility service.

**Types:**
- Click (single tap)
- Long click (press and hold)
- Swipe (directional gesture)
- Scroll (continuous gesture)

**Implementation:**
```kotlin
fun performClick(x: Float, y: Float) {
    val path = Path().apply {
        moveTo(x, y)
    }

    val gesture = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        .build()

    dispatchGesture(gesture, callback, null)
}
```

### Hash-Based Lookup
Using hash as primary key for O(1) lookup performance.

**VOS4 Implementation:**
```kotlin
@Entity(tableName = "scraped_elements")
data class ScrapedElementEntity(
    @PrimaryKey
    @ColumnInfo(name = "element_hash")
    val elementHash: String,  // SHA-256 hash
    // ...
)

@Dao
interface ScrapedElementDao {
    @Query("SELECT * FROM scraped_elements WHERE element_hash = :hash")
    suspend fun getElement(hash: String): ScrapedElementEntity?  // O(1) lookup
}
```

**Benefits:**
- Constant-time lookups
- Unique identification
- Deduplication

### Hierarchical Database
Database with parent-child relationships between entities.

**VOS4 Hierarchy:**
```
apps (parent)
├── scraped_elements (child, FK: app_id)
│   └── generated_commands (grandchild, FK: element_hash)
├── screen_contexts (child, FK: app_id)
│   ├── screen_transitions (grandchild, FK: screen_hash)
│   └── user_interactions (grandchild, FK: screen_hash)
└── exploration_sessions (child, FK: app_id)
```

**Advantages:**
- Cascading deletes
- Referential integrity
- Clear data ownership

### Migration (Database)
Process of updating database schema while preserving data.

**VOS4 Migrations:**
- v1→v2: Unified apps table, unique element hashes
- v2→v3: Added feature flags
- v3→v4: FK consolidation, removed scraped_apps

**Example:**
```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Backup data
        db.execSQL("ALTER TABLE scraped_elements RENAME TO scraped_elements_old")

        // Create new schema
        db.execSQL("CREATE TABLE scraped_elements (...)")

        // Migrate data
        db.execSQL("INSERT INTO scraped_elements SELECT * FROM scraped_elements_old")

        // Cleanup
        db.execSQL("DROP TABLE scraped_elements_old")
    }
}
```

### Reactive Programming
Programming paradigm where data flows automatically trigger updates.

**VOS4 Usage:**
- Room Flow queries
- LiveData observers
- StateFlow/SharedFlow

**Example:**
```kotlin
// Data source (Room)
@Query("SELECT * FROM apps")
fun getAllAppsFlow(): Flow<List<AppEntity>>

// ViewModel
val appsFlow: Flow<List<AppEntity>> = appDao.getAllAppsFlow()

// UI (Composable)
val apps by viewModel.appsFlow.collectAsState(initial = emptyList())
LazyColumn {
    items(apps) { app ->
        AppItem(app)
    }
}
// UI updates automatically when data changes
```

### Referential Integrity
Database constraint ensuring foreign key values reference existing parent records.

**Enforcement:**
```sql
FOREIGN KEY(app_id) REFERENCES apps(app_id)
```

**Violations:**
```sql
-- Error: FK constraint failed
INSERT INTO scraped_elements (app_id, ...) VALUES ('nonexistent_id', ...);
```

**VOS4 Strategy:**
- Always insert parent before child
- Use CASCADE delete for cleanup
- Enable FK constraints: `PRAGMA foreign_keys=ON`

### Screen Hash
SHA-256 hash uniquely identifying a screen.

**Calculation:**
```kotlin
fun calculateScreenHash(
    packageName: String,
    windowTitle: String,
    activityName: String?
): String {
    val content = "$packageName|$windowTitle|$activityName"
    return MessageDigest.getInstance("SHA-256")
        .digest(content.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
```

**Uses:**
- Unique screen identification
- Deduplication
- Navigation flow tracking

### Semantic Role
AI-inferred role of UI element.

**Common Roles:**
- `button` - Action trigger
- `input` - Text entry
- `label` - Informational text
- `checkbox` - Boolean selection
- `radio` - Exclusive selection
- `dropdown` - Menu/picker
- `link` - Navigation link

**Inference:**
```kotlin
fun inferSemanticRole(element: AccessibilityNodeInfo): String {
    return when {
        element.isClickable && element.className.contains("Button") -> "button"
        element.isEditable -> "input"
        element.isCheckable && !element.className.contains("Radio") -> "checkbox"
        element.isCheckable && element.className.contains("Radio") -> "radio"
        element.text != null && !element.isClickable -> "label"
        else -> "unknown"
    }
}
```

### Service (Android)
Long-running background component.

**Types:**
- **Foreground Service:** Visible notification, can run indefinitely
- **Background Service:** Limited by system (killed when app not visible)
- **Bound Service:** Tied to component lifecycle

**VOS4 Services:**
- `VoiceOSService` (AccessibilityService) - Foreground
- `VoiceKeyboardService` (InputMethodService) - Bound

### Singleton Pattern
Design pattern ensuring only one instance of a class exists.

**VOS4 Usage:**
```kotlin
@Database(entities = [...], version = 4)
abstract class VoiceOSAppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: VoiceOSAppDatabase? = null

        fun getInstance(context: Context): VoiceOSAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(...)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**Benefits:**
- Single database connection
- Reduced resource usage
- Thread-safe initialization

---

## D.3 VOS4-Specific Terms

### Command Generator
Component that creates voice commands from UI elements.

**Input:** `ScrapedElementEntity`
**Output:** `GeneratedCommandEntity`

**Process:**
1. Extract element properties (text, contentDescription, viewId)
2. Infer semantic role
3. Generate command phrases
4. Calculate confidence scores
5. Add synonyms

**Example:**
```kotlin
Input:
  className: "android.widget.Button"
  text: "Submit"
  contentDescription: "Submit form"

Output:
  commandText: "click submit button"
  synonyms: ["submit", "send form", "click submit"]
  confidence: 0.92
  actionType: "click"
```

### Cursor Offset
X,Y coordinates of voice cursor on screen.

**Data Class:**
```kotlin
data class CursorOffset(
    val x: Float,
    val y: Float
)
```

**Usage:**
```kotlin
val position = cursorPositionTracker.getCurrentPosition()
Log.d(TAG, "Cursor at (${position.x}, ${position.y})")
```

### Element Relationship
Semantic connection between UI elements.

**Types:**
- `parent_child` - Hierarchical
- `label_for` - Label describes element
- `input_for` - Input belongs to form
- `button_for` - Button submits form
- `group_member` - Part of button group

**Storage:**
```kotlin
@Entity(tableName = "element_relationships")
data class ElementRelationshipEntity(
    val sourceElementHash: String,
    val targetElementHash: String?,
    val relationshipType: String,
    val confidence: Float
)
```

### Exploration Session
LearnApp session exploring an app.

**Lifecycle:**
1. Start exploration
2. Discover screens
3. Map navigation
4. Mark complete/failed

**Entity:**
```kotlin
@Entity(tableName = "exploration_sessions")
data class ExplorationSessionEntity(
    val sessionId: String,
    val appId: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String,  // IN_PROGRESS, COMPLETE, FAILED
    val screensDiscovered: Int,
    val edgesDiscovered: Int
)
```

### Screen Context
Metadata about a screen/activity.

**Properties:**
- `screenHash` - Unique identifier
- `screenType` - Classification (login, list, detail)
- `formContext` - Form type (login_form, signup_form)
- `navigationLevel` - Depth in nav stack
- `primaryAction` - Main CTA
- `elementCount` - UI element count

**Inference:**
```kotlin
fun inferScreenType(elements: List<ScrapedElementEntity>): String {
    val hasPasswordField = elements.any { it.inputType?.contains("password") == true }
    val hasSubmitButton = elements.any { it.text?.contains("submit", ignoreCase = true) == true }

    return when {
        hasPasswordField && hasSubmitButton -> "login"
        elements.size > 10 && elements.count { it.isClickable } > 5 -> "list"
        elements.size <= 5 -> "detail"
        else -> "unknown"
    }
}
```

### Screen Transition
Navigation from one screen to another.

**Tracking:**
```kotlin
@Entity(tableName = "screen_transitions")
data class ScreenTransitionEntity(
    val fromScreenHash: String,
    val toScreenHash: String,
    val transitionCount: Int,
    val avgTransitionTime: Long?
)
```

**Analysis:**
```sql
-- Most common navigation paths
SELECT
    from_screen_hash,
    to_screen_hash,
    transition_count
FROM screen_transitions
ORDER BY transition_count DESC
LIMIT 10;
```

### UIScrapingEngine
Core component for extracting UI elements.

**Responsibilities:**
- Traverse accessibility tree
- Extract element properties
- Calculate element hashes
- Generate voice commands
- Save to database

**Usage:**
```kotlin
val result = uiScrapingEngine.scrapeCurrentScreen(packageName)

if (result.success) {
    Log.d(TAG, "Scraped ${result.elementCount} elements")
    Log.d(TAG, "Generated ${result.commandCount} commands")
}
```

### Voice Command Registry
Central registry of available voice commands.

**Sources:**
- Static commands (built-in)
- Dynamic commands (generated from UI)
- User-defined commands

**Registration:**
```kotlin
commandManager.registerCommand(
    VoiceCommandEntity(
        id = UUID.randomUUID().toString(),
        commandText = "go home",
        actionType = "navigate",
        actionData = "home_screen"
    )
)
```

---

## D.4 Android Platform Terms

### Activity
Android component representing a single screen.

**Lifecycle:**
- `onCreate()` - Initialize
- `onStart()` - Become visible
- `onResume()` - Start interacting
- `onPause()` - Stop interacting
- `onStop()` - No longer visible
- `onDestroy()` - Cleanup

### Content Provider
Android component for sharing data between apps.

**VOS4 Usage:**
- Read installed apps
- Access app metadata

### Intent
Message object for requesting actions from other components.

**Types:**
- **Explicit:** Target specific component
- **Implicit:** Request action (system chooses handler)

**Examples:**
```kotlin
// Explicit
val intent = Intent(this, MainActivity::class.java)
startActivity(intent)

// Implicit
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
startActivity(intent)
```

### Manifest
XML file declaring app components, permissions, and metadata.

**Key Sections:**
```xml
<manifest>
    <uses-permission android:name="..." />  <!-- Permissions -->
    <uses-feature android:name="..." />  <!-- Features -->

    <application>
        <activity android:name="..." />  <!-- Activities -->
        <service android:name="..." />  <!-- Services -->
        <receiver android:name="..." />  <!-- Broadcast receivers -->
        <provider android:name="..." />  <!-- Content providers -->
    </application>
</manifest>
```

### Package Name
Unique identifier for Android app.

**Format:** Reverse domain notation
**Examples:**
- `com.augmentalis.voiceos`
- `com.android.chrome`
- `com.example.app`

**Usage:**
- App identification
- Permission scoping
- Data isolation

### Permission
Security mechanism controlling access to sensitive features.

**Types:**
- **Normal:** Granted automatically
- **Dangerous:** Require user approval
- **Special:** Require system settings

**VOS4 Permissions:**
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />  <!-- Dangerous -->
<uses-permission android:name="android.permission.INTERNET" />  <!-- Normal -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />  <!-- Special -->
```

### View
Android UI element (button, text, image, etc.).

**Common Views:**
- `TextView` - Display text
- `EditText` - Text input
- `Button` - Clickable button
- `ImageView` - Display image
- `RecyclerView` - Scrollable list
- `ViewGroup` - Container (layouts)

---

## D.5 Database Terms

### ACID Properties
Database transaction guarantees.

**Acronym:**
- **Atomicity:** All or nothing
- **Consistency:** Valid state to valid state
- **Isolation:** Concurrent transactions don't interfere
- **Durability:** Committed changes persist

**Room Support:** ✅ Full ACID compliance (SQLite)

### Index
Database structure for fast lookups.

**Types:**
- **Unique Index:** No duplicate values
- **Non-Unique Index:** Duplicates allowed
- **Composite Index:** Multiple columns

**VOS4 Indices:**
```sql
CREATE UNIQUE INDEX index_scraped_elements_element_hash ON scraped_elements(element_hash);
CREATE INDEX index_scraped_elements_app_id ON scraped_elements(app_id);
```

**Performance:**
- Indexed query: O(log n)
- Non-indexed query: O(n)

### Join (SQL)
Combine rows from multiple tables.

**Types:**
- **INNER JOIN:** Matching rows only
- **LEFT JOIN:** All left + matching right
- **RIGHT JOIN:** All right + matching left
- **FULL JOIN:** All rows from both

**Example:**
```sql
SELECT gc.command_text, se.text
FROM generated_commands gc
INNER JOIN scraped_elements se ON gc.element_hash = se.element_hash;
```

### Transaction
Group of database operations executed atomically.

**Usage:**
```kotlin
database.withTransaction {
    appDao.insert(app)
    elementDao.insertBatch(elements)
    commandDao.insertBatch(commands)
    // All or nothing - if any fails, all rollback
}
```

---

## D.6 Architecture Patterns

### Dependency Injection
Providing dependencies from external source instead of creating internally.

**Without DI:**
```kotlin
class MyViewModel {
    private val dao = AppDatabase.getInstance().appDao()  // ❌ Tight coupling
}
```

**With DI (Hilt):**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val dao: AppDao  // ✅ Injected dependency
) : ViewModel()
```

### MVVM (Model-View-ViewModel)
Architectural pattern separating UI from business logic.

**Components:**
- **Model:** Data layer (database, network)
- **View:** UI layer (Composables, Activities)
- **ViewModel:** Presentation logic

**VOS4 Example:**
```kotlin
// Model
@Entity
data class AppEntity(...)

// ViewModel
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appDao: AppDao
) : ViewModel() {
    val apps: Flow<List<AppEntity>> = appDao.getAllAppsFlow()
}

// View (Composable)
@Composable
fun AppsScreen(viewModel: AppsViewModel) {
    val apps by viewModel.apps.collectAsState(initial = emptyList())
    LazyColumn {
        items(apps) { app ->
            AppItem(app)
        }
    }
}
```

### Repository Pattern
Abstraction layer between data sources and business logic.

**VOS4 Usage:**
```kotlin
class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val apiService: ApiService
) {
    suspend fun getApps(): List<AppEntity> {
        // Try local first
        val localApps = appDao.getAllApps()
        if (localApps.isNotEmpty()) return localApps

        // Fallback to API
        val remoteApps = apiService.fetchApps()
        appDao.insertBatch(remoteApps)
        return remoteApps
    }
}
```

### SOLID Principles

**Single Responsibility:** One class, one responsibility
- ✅ `UIScrapingEngine` - Only scrapes UI
- ✅ `CommandGenerator` - Only generates commands

**Open/Closed:** Open for extension, closed for modification
- ✅ `SpeechEngine` interface - Multiple implementations
- ✅ Add new engine without modifying existing code

**Liskov Substitution:** Subtypes should be substitutable
- ✅ Any `SpeechEngine` can replace another

**Interface Segregation:** Many specific interfaces > one general
- ✅ `IVoiceOSService` - Focused interface
- ✅ Clients depend only on methods they use

**Dependency Inversion:** Depend on abstractions
- ✅ Depend on `SpeechEngine` interface
- ✅ Not on concrete `VivokaEngine` class

---

## Summary

**Total Terms:** 150+

**Categories:**
- Acronyms: 30+
- Technical Terms: 40+
- VOS4-Specific: 20+
- Android Platform: 15+
- Database: 15+
- Architecture: 10+

**Most Important Terms:**
1. Accessibility Service
2. Element Hash
3. Screen Hash
4. DYNAMIC vs LEARN_APP Mode
5. Foreign Key (FK)
6. Migration
7. Coroutine
8. Flow
9. Hilt (DI)
10. Room

---

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete
**Next Appendix:** [Appendix E: Code Examples](Appendix-E-Code-Examples.md)
