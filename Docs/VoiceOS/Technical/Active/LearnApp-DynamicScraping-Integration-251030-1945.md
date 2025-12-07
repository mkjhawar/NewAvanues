# LearnApp + VoiceOSCore Dynamic Scraping - Unified Integration

**Date:** 2025-10-30 19:45 PDT
**Status:** DESIGN - Integration of passive and active scraping
**Priority:** CRITICAL - Must work together without duplication

---

## 🎯 Requirement

**User Request:**
> "Dynamic scraping in VoiceOSCore should work just like LearnApp - scrape full pages, menus, etc. Keep track of what's registered. Mark as PARTIAL/PENDING so we don't duplicate when user navigates again. When LearnApp runs, it learns what hasn't been learned yet."

---

## 📊 Two Scraping Modes

### **Mode 1: Dynamic Scraping (Passive)**
- **Service:** VoiceOSCore AccessibilityScrapingIntegration
- **Trigger:** User navigates naturally (window change events)
- **Behavior:** Scrapes current screen passively as user uses app
- **Database Status:** `PARTIAL` (incomplete, opportunistic)
- **Coverage:** Captures what user sees, misses unexplored paths

### **Mode 2: LearnApp (Active)**
- **Service:** LearnApp ExplorationEngine
- **Trigger:** User explicitly starts learning
- **Behavior:** Proactively clicks all elements, explores all paths
- **Database Status:** `FULLY_LEARNED` (comprehensive)
- **Coverage:** Captures entire app (all screens, all elements)

---

## 🔄 Integration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SHARED INFRASTRUCTURE                    │
├─────────────────────────────────────────────────────────────┤
│  LauncherDetector (device-agnostic)                        │
│  WindowManager (multi-window detection)                    │
│  ExpandableControlDetector (dropdowns/menus)               │
│  ScrapingMode (DYNAMIC / LEARN_APP)                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────────────────────────┐
        │                                       │
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│ Dynamic Scraping │                  │    LearnApp      │
│  (VoiceOSCore)   │                  │   (Proactive)    │
├──────────────────┤                  ├──────────────────┤
│ • Passive        │                  │ • Active         │
│ • As user goes   │                  │ • Clicks all     │
│ • Single window  │                  │ • Multi-window   │
│ • No menus       │                  │ • Opens menus    │
│ • PARTIAL status │                  │ • LEARNED status │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
        └───────────────┬───────────────────────┘
                        ↓
        ┌───────────────────────────────────────┐
        │      UNIFIED DATABASE SYSTEM          │
        ├───────────────────────────────────────┤
        │ • Deduplication (hash-based)          │
        │ • Mode tracking (dynamic vs learned)  │
        │ • Completion percentage               │
        │ • Incremental learning                │
        └───────────────────────────────────────┘
```

---

## 🗄️ Database Schema Enhancement

### **Current ScrapedAppEntity:**

```kotlin
@Entity(tableName = "scraped_apps")
data class ScrapedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "app_id")
    val appId: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "is_fully_learned")
    val isFullyLearned: Boolean = false,  // ✅ Already exists

    @ColumnInfo(name = "learn_completed_at")
    val learnCompletedAt: Long? = null,   // ✅ Already exists
)
```

### **NEW: Add Scraping Mode Field**

```kotlin
@Entity(tableName = "scraped_apps")
data class ScrapedAppEntity(
    // ... existing fields ...

    // ✅ NEW: Track scraping mode
    @ColumnInfo(name = "scraping_mode")
    val scrapingMode: String = "DYNAMIC",  // DYNAMIC, LEARN_APP

    // ✅ NEW: Completion percentage (0-100)
    @ColumnInfo(name = "completion_percent")
    val completionPercent: Float = 0f,

    // ✅ NEW: Last scraping update
    @ColumnInfo(name = "last_scraped_at")
    val lastScrapedAt: Long = System.currentTimeMillis(),

    // ✅ NEW: Element counts
    @ColumnInfo(name = "total_screens")
    val totalScreens: Int = 0,

    @ColumnInfo(name = "total_elements")
    val totalElements: Int = 0
)
```

### **Scraping Mode Enum:**

```kotlin
enum class ScrapingMode {
    /**
     * Dynamic scraping: Passive, as user navigates.
     * App is partially learned, incomplete coverage.
     */
    DYNAMIC,

    /**
     * LearnApp mode: Active, comprehensive exploration.
     * App is fully learned (or attempted to be).
     */
    LEARN_APP
}
```

---

## 🔧 Implementation Strategy

### **Shared Infrastructure (Used by Both Modes)**

All these components work identically for both dynamic and LearnApp:

1. **LauncherDetector** - Filters launcher packages
2. **WindowManager** - Detects all windows (main + overlays)
3. **ExpandableControlDetector** - Identifies dropdowns/menus
4. **Element Deduplication** - Hash-based, prevents duplicates

### **Mode-Specific Behavior**

| Feature | Dynamic Scraping | LearnApp |
|---------|-----------------|----------|
| **Window Detection** | ✅ Multi-window | ✅ Multi-window |
| **Launcher Filtering** | ✅ Yes | ✅ Yes |
| **Element Registration** | ✅ All visible elements | ✅ All elements |
| **Element Clicking** | ❌ NO (passive) | ✅ YES (active) |
| **Menu Expansion** | ❌ NO (only if user opens) | ✅ YES (proactive) |
| **Dropdown Exploration** | ❌ NO | ✅ YES |
| **Completion Tracking** | ⚠️ Opportunistic | ✅ Comprehensive |
| **Database Status** | `DYNAMIC` (partial) | `LEARN_APP` (complete) |

---

## 📋 Detailed Implementation

### **File 1: ScrapingCoordinator.kt (NEW)**

**Purpose:** Central coordination between dynamic and LearnApp scraping

```kotlin
/**
 * Coordinates scraping between dynamic (passive) and LearnApp (active) modes.
 * Ensures no duplication, tracks completion, manages mode transitions.
 */
class ScrapingCoordinator(
    private val database: AppScrapingDatabase,
    private val launcherDetector: LauncherDetector,
    private val windowManager: WindowManager
) {
    private val TAG = "ScrapingCoordinator"

    /**
     * Gets or creates app record with appropriate scraping mode.
     *
     * @param packageName Package being scraped
     * @param mode Scraping mode (DYNAMIC or LEARN_APP)
     * @return App ID
     */
    suspend fun getOrCreateApp(
        packageName: String,
        mode: ScrapingMode
    ): String {
        // Check if app already exists
        var app = database.scrapedAppDao().getAppByPackage(packageName)

        if (app == null) {
            // Create new app record
            val appId = generateAppId(packageName)
            app = ScrapedAppEntity(
                appId = appId,
                packageName = packageName,
                appName = getAppName(packageName),
                scrapingMode = mode.name,
                completionPercent = 0f,
                isFullyLearned = false,
                lastScrapedAt = System.currentTimeMillis()
            )
            database.scrapedAppDao().insert(app)
            Log.i(TAG, "Created new app: $packageName (mode=$mode)")
        } else {
            // App exists - check mode transition
            val currentMode = ScrapingMode.valueOf(app.scrapingMode)

            if (currentMode != mode) {
                handleModeTransition(app, currentMode, mode)
            } else {
                // Same mode - update timestamp
                database.scrapedAppDao().updateLastScrapedAt(
                    app.appId,
                    System.currentTimeMillis()
                )
            }
        }

        return app.appId
    }

    /**
     * Handles transition between scraping modes.
     *
     * DYNAMIC → LEARN_APP: Upgrade to comprehensive learning
     * LEARN_APP → DYNAMIC: Downgrade to passive (shouldn't happen normally)
     */
    private suspend fun handleModeTransition(
        app: ScrapedAppEntity,
        oldMode: ScrapingMode,
        newMode: ScrapingMode
    ) {
        Log.i(TAG, "Mode transition: $oldMode → $newMode for ${app.packageName}")

        when (newMode) {
            ScrapingMode.LEARN_APP -> {
                // Upgrading to LearnApp - keep existing data, mark as in-progress
                database.scrapedAppDao().updateScrapingMode(
                    app.appId,
                    ScrapingMode.LEARN_APP.name
                )
                Log.i(TAG, "✅ Upgraded ${app.packageName} to LEARN_APP mode")
            }
            ScrapingMode.DYNAMIC -> {
                // Downgrading to dynamic (unusual - log warning)
                Log.w(TAG, "⚠️ Downgrading ${app.packageName} from LEARN_APP to DYNAMIC (unusual)")
                database.scrapedAppDao().updateScrapingMode(
                    app.appId,
                    ScrapingMode.DYNAMIC.name
                )
            }
        }
    }

    /**
     * Checks if screen has already been scraped in current mode.
     * Prevents duplicate scraping when user returns to same screen.
     *
     * @param screenHash Screen fingerprint
     * @param appId App ID
     * @return true if screen already scraped
     */
    suspend fun isScreenAlreadyScraped(
        screenHash: String,
        appId: String
    ): Boolean {
        val existingScreen = database.screenContextDao().getScreenByHash(screenHash)

        if (existingScreen != null && existingScreen.appId == appId) {
            Log.v(TAG, "Screen already scraped: $screenHash")
            return true
        }

        return false
    }

    /**
     * Checks if element has already been registered.
     * Uses hash-based deduplication.
     *
     * @param elementHash Element fingerprint
     * @return true if element already exists
     */
    suspend fun isElementAlreadyRegistered(elementHash: String): Boolean {
        return database.scrapedElementDao().getElementByHash(elementHash) != null
    }

    /**
     * Updates app completion percentage after scraping.
     */
    suspend fun updateCompletion(
        appId: String,
        screensScraped: Int,
        elementsRegistered: Int
    ) {
        database.scrapedAppDao().updateStats(
            appId = appId,
            totalScreens = screensScraped,
            totalElements = elementsRegistered,
            lastScrapedAt = System.currentTimeMillis()
        )
    }

    /**
     * Marks app as fully learned after LearnApp completes.
     *
     * @param appId App ID
     * @param completionPercent Final completion percentage
     */
    suspend fun markAsFullyLearned(
        appId: String,
        completionPercent: Float
    ) {
        database.scrapedAppDao().markAsFullyLearned(
            appId = appId,
            completionPercent = completionPercent,
            completionTimestamp = System.currentTimeMillis()
        )

        // Switch to DYNAMIC mode for future passive scraping
        database.scrapedAppDao().updateScrapingMode(
            appId,
            ScrapingMode.DYNAMIC.name
        )

        Log.i(TAG, "✅ App $appId marked as FULLY LEARNED (${completionPercent}%)")
    }
}
```

---

### **File 2: AccessibilityScrapingIntegration.kt (ENHANCE)**

**Purpose:** Add multi-window + menu scraping to dynamic mode

**Current Behavior:** Single window, no menu exploration
**New Behavior:** Multi-window, attempts to scrape menus if visible

```kotlin
class AccessibilityScrapingIntegration : AccessibilityService() {

    private lateinit var launcherDetector: LauncherDetector
    private lateinit var windowManager: WindowManager
    private lateinit var expandableDetector: ExpandableControlDetector
    private lateinit var scrapingCoordinator: ScrapingCoordinator

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // ✅ NEW: Multi-window scraping
        scrapeAllWindows(event.packageName?.toString())
    }

    /**
     * ✅ NEW: Scrapes all windows for package (not just active window).
     */
    private suspend fun scrapeAllWindows(packageName: String?) {
        if (packageName == null) return

        // Skip launcher packages
        if (launcherDetector.isLauncher(packageName)) {
            Log.d(TAG, "🏠 Skipping launcher: $packageName")
            return
        }

        // Get or create app record (DYNAMIC mode)
        val appId = scrapingCoordinator.getOrCreateApp(
            packageName = packageName,
            mode = ScrapingMode.DYNAMIC
        )

        // ✅ NEW: Get ALL windows (not just active)
        val windows = windowManager.getAppWindows(packageName, launcherDetector)

        if (windows.isEmpty()) {
            Log.v(TAG, "No windows found for $packageName")
            return
        }

        Log.d(TAG, "📱 Dynamic scraping: ${windows.size} windows for $packageName")

        // Scrape each window
        for (windowInfo in windows) {
            scrapeWindow(windowInfo, appId, packageName)
        }
    }

    /**
     * ✅ ENHANCED: Scrapes single window with multi-window awareness.
     */
    private suspend fun scrapeWindow(
        windowInfo: WindowManager.WindowInfo,
        appId: String,
        packageName: String
    ) {
        val rootNode = windowInfo.rootNode ?: return

        // Calculate screen hash
        val screenHash = calculateScreenHash(rootNode, packageName)

        // ✅ Check if screen already scraped (deduplication)
        if (scrapingCoordinator.isScreenAlreadyScraped(screenHash, appId)) {
            Log.v(TAG, "Screen already scraped, skipping: $screenHash")
            return
        }

        Log.i(TAG, "🔍 Scraping window: type=${windowInfo.type}, overlay=${windowInfo.isOverlay}")

        // Scrape element tree (existing logic, already handles hierarchy)
        val elements = scrapeNode(rootNode, appId, packageName, depth = 0)

        // ✅ NEW: Detect expandable controls
        val expandableControls = elements.filter { element ->
            expandableDetector.isExpandableControl(element)
        }

        if (expandableControls.isNotEmpty()) {
            Log.d(TAG, "📋 Detected ${expandableControls.size} expandable controls (menus/dropdowns)")
            // Note: In DYNAMIC mode, we only REGISTER them, don't click
            // LearnApp will click them during active exploration
        }

        // Save screen context
        val screenContext = ScreenContextEntity(
            screenHash = screenHash,
            appId = appId,
            packageName = packageName,
            className = rootNode.className?.toString(),
            windowType = windowInfo.type.name,
            isOverlay = windowInfo.isOverlay,
            scrapedAt = System.currentTimeMillis()
        )
        database.screenContextDao().insert(screenContext)

        // Update app stats
        scrapingCoordinator.updateCompletion(
            appId = appId,
            screensScraped = database.screenContextDao().getScreenCountForApp(appId),
            elementsRegistered = database.scrapedElementDao().getElementCountForApp(appId)
        )

        Log.i(TAG, "✅ Dynamic scraping complete: $screenHash, ${elements.size} elements")
    }

    /**
     * ✅ EXISTING: Recursive element scraping (already has deduplication).
     * No changes needed - already uses hash-based deduplication.
     */
    private suspend fun scrapeNode(
        node: AccessibilityNodeInfo,
        appId: String,
        packageName: String,
        depth: Int
    ): List<ScrapedElementEntity> {
        // Existing implementation
        // Already handles:
        // - Hash-based deduplication (checks if element exists)
        // - Hierarchy tracking
        // - Element registration

        // No changes needed!
        return existingScrapingLogic(node, appId, packageName, depth)
    }
}
```

---

### **File 3: ExplorationEngine.kt (INTEGRATE)**

**Purpose:** LearnApp uses same shared infrastructure, learns what's missing

```kotlin
class ExplorationEngine(
    private val accessibilityService: AccessibilityService,
    private val windowManager: WindowManager,
    private val clickTracker: ElementClickTracker,
    private val launcherDetector: LauncherDetector,
    private val expandableDetector: ExpandableControlDetector,
    private val scrapingCoordinator: ScrapingCoordinator  // ✅ NEW
) {

    suspend fun startExploration(packageName: String) {
        Log.i(TAG, "🚀 Starting LearnApp exploration: $packageName")

        // ✅ Get or create app (LEARN_APP mode)
        val appId = scrapingCoordinator.getOrCreateApp(
            packageName = packageName,
            mode = ScrapingMode.LEARN_APP
        )

        // Check if app was previously in DYNAMIC mode
        val app = database.scrapedAppDao().getById(appId)
        if (app != null && app.scrapingMode == ScrapingMode.DYNAMIC.name) {
            Log.i(TAG, "📦 App was previously scraped in DYNAMIC mode")
            Log.i(TAG, "   Screens: ${app.totalScreens}, Elements: ${app.totalElements}")
            Log.i(TAG, "   Will learn remaining content...")
        }

        // Start exploration (uses shared WindowManager, etc.)
        exploreScreenRecursive(packageName, appId, depth = 0)

        // Mark as fully learned
        val stats = clickTracker.getStats()
        scrapingCoordinator.markAsFullyLearned(
            appId = appId,
            completionPercent = stats.overallCompleteness
        )

        Log.i(TAG, "✅ LearnApp complete: ${stats.overallCompleteness}% coverage")
    }

    private suspend fun exploreScreenRecursive(
        packageName: String,
        appId: String,
        depth: Int
    ) {
        // ✅ Same multi-window approach as dynamic scraping
        val windows = windowManager.getAppWindows(packageName, launcherDetector)

        for (windowInfo in windows) {
            exploreWindow(windowInfo, packageName, appId, depth)
        }
    }

    private suspend fun exploreWindow(
        windowInfo: WindowManager.WindowInfo,
        packageName: String,
        appId: String,
        depth: Int
    ) {
        val rootNode = windowInfo.rootNode ?: return

        // Scrape all elements (uses same deduplication as dynamic scraping)
        val allElements = scrapeAndRegisterElements(rootNode, appId, packageName)

        // ✅ Click safe elements (only in LEARN_APP mode)
        val clickableElements = allElements.filter {
            it.classification == Classification.SafeClickable
        }

        for (element in clickableElements) {
            // ✅ Check if already clicked in previous exploration
            if (clickTracker.wasElementClicked(screenHash, element.uuid)) {
                Log.v(TAG, "Element already clicked, skipping: ${element.uuid}")
                continue
            }

            // ✅ Check if expandable control
            if (expandableDetector.isExpandableControl(element)) {
                handleExpandableControl(element, packageName, appId, depth)
            } else {
                handleRegularElement(element, packageName, appId, depth)
            }

            // Mark as clicked
            clickTracker.markElementClicked(screenHash, element.uuid)
        }
    }

    /**
     * ✅ Scrapes and registers elements (shared logic with dynamic scraping).
     * Uses same deduplication system.
     */
    private suspend fun scrapeAndRegisterElements(
        rootNode: AccessibilityNodeInfo,
        appId: String,
        packageName: String
    ): List<ElementInfo> {
        // Call AccessibilityScrapingIntegration's scraping logic
        // Already handles deduplication via hash checking
        return accessibilityIntegration.scrapeWindow(rootNode, appId, packageName)
    }
}
```

---

## 🔄 Interaction Flow Examples

### **Example 1: User First Opens App (Dynamic Scraping)**

```
1. User opens Teams app
   ↓
2. AccessibilityScrapingIntegration.onAccessibilityEvent() fires
   ↓
3. ScrapingCoordinator.getOrCreateApp("com.microsoft.teams", DYNAMIC)
   ↓ (App doesn't exist)
4. Creates new ScrapedAppEntity:
   - appId = "teams-abc123"
   - scrapingMode = "DYNAMIC"
   - completionPercent = 0%
   - isFullyLearned = false
   ↓
5. WindowManager.getAppWindows() → [MainWindow]
   ↓
6. Scrapes main window:
   - 15 elements registered
   - Screen hash saved
   ↓
7. User navigates to "Calls" screen
   ↓
8. Scrapes "Calls" screen:
   - 8 new elements
   - Total: 2 screens, 23 elements
   ↓
9. Database shows:
   - scrapingMode = "DYNAMIC"
   - completionPercent = ~20% (guessed, incomplete)
   - isFullyLearned = false
```

### **Example 2: User Returns to Same Screen (Deduplication)**

```
1. User goes back to Teams main screen
   ↓
2. AccessibilityScrapingIntegration fires
   ↓
3. Calculates screen hash
   ↓
4. ScrapingCoordinator.isScreenAlreadyScraped(screenHash)
   ↓ (Returns TRUE - already scraped)
5. ✅ SKIPS scraping (no duplication)
   ↓
6. Logs: "Screen already scraped, skipping"
```

### **Example 3: User Runs LearnApp (Active Learning)**

```
1. User clicks "Learn Teams App" button
   ↓
2. ExplorationEngine.startExploration("com.microsoft.teams")
   ↓
3. ScrapingCoordinator.getOrCreateApp("com.microsoft.teams", LEARN_APP)
   ↓ (App exists with DYNAMIC mode)
4. Logs: "App was previously scraped in DYNAMIC mode"
       "Screens: 2, Elements: 23"
       "Will learn remaining content..."
   ↓
5. Updates mode: DYNAMIC → LEARN_APP
   ↓
6. Starts comprehensive exploration:
   - Clicks all safe elements
   - Opens dropdowns/menus
   - Explores all paths
   ↓
7. After 4 minutes:
   - 12 screens discovered
   - 245 elements registered (includes 222 NEW from menus/dropdowns)
   - completionPercent = 98%
   ↓
8. ScrapingCoordinator.markAsFullyLearned(appId, 98%)
   ↓
9. Database updated:
   - scrapingMode = "DYNAMIC" (switched back for future passive scraping)
   - completionPercent = 98%
   - isFullyLearned = true
   - learnCompletedAt = 1234567890
```

### **Example 4: User Opens New Screen After LearnApp (Incremental)**

```
1. User navigates to NEW screen in Teams (added in app update)
   ↓
2. Dynamic scraping detects new screen
   ↓
3. Checks: isScreenAlreadyScraped() → FALSE (new screen)
   ↓
4. Scrapes new screen:
   - 5 new elements registered
   ↓
5. Database updated:
   - totalScreens: 12 → 13
   - totalElements: 245 → 250
   - completionPercent: 98% → 97% (denominator increased)
   - isFullyLearned: true → false (NEW content found)
   ↓
6. Next time user runs LearnApp:
   - Will explore ONLY this new screen's elements
   - Mark as fully learned again after completion
```

---

## ✅ Key Benefits of This Approach

### **1. No Duplication**
- ✅ Hash-based deduplication prevents duplicate elements
- ✅ Screen hash prevents re-scraping same screen
- ✅ Coordinator checks existing data before scraping

### **2. Incremental Learning**
- ✅ Dynamic scraping builds partial data as user navigates
- ✅ LearnApp fills in gaps (unexplored paths, hidden menus)
- ✅ App updates detected (new screens trigger re-learning)

### **3. Shared Infrastructure**
- ✅ LauncherDetector used by both modes
- ✅ WindowManager used by both modes
- ✅ ExpandableControlDetector used by both modes
- ✅ Same database, same deduplication, same scraping logic

### **4. Clear State Management**
- ✅ Database shows mode: DYNAMIC (partial) vs LEARN_APP (complete)
- ✅ Completion percentage tracks progress
- ✅ isFullyLearned flag indicates production readiness

---

## 📊 Database Migration

### **Migration Required:**

```kotlin
@Database(
    entities = [ScrapedAppEntity::class, ...],
    version = 8,  // ✅ Increment version
    autoMigrations = [
        AutoMigration(from = 7, to = 8)
    ]
)
abstract class AppScrapingDatabase : RoomDatabase() {
    // ...
}
```

### **Migration Script:**

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns
        database.execSQL(
            "ALTER TABLE scraped_apps ADD COLUMN scraping_mode TEXT NOT NULL DEFAULT 'DYNAMIC'"
        )
        database.execSQL(
            "ALTER TABLE scraped_apps ADD COLUMN completion_percent REAL NOT NULL DEFAULT 0.0"
        )
        database.execSQL(
            "ALTER TABLE scraped_apps ADD COLUMN last_scraped_at INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE scraped_apps ADD COLUMN total_screens INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE scraped_apps ADD COLUMN total_elements INTEGER NOT NULL DEFAULT 0"
        )

        // Mark existing apps as DYNAMIC mode (were passively scraped)
        database.execSQL(
            "UPDATE scraped_apps SET scraping_mode = 'DYNAMIC' WHERE is_fully_learned = 0"
        )

        // Mark fully learned apps as LEARN_APP mode (historical)
        database.execSQL(
            "UPDATE scraped_apps SET scraping_mode = 'LEARN_APP', completion_percent = 100.0 WHERE is_fully_learned = 1"
        )
    }
}
```

---

## 🧪 Testing Strategy

### **Test 1: Dynamic Scraping Deduplication**
```kotlin
@Test
fun `dynamic scraping skips already scraped screens`() {
    // User opens Teams
    dynamicScraper.scrape("com.microsoft.teams")

    // User closes and reopens Teams
    dynamicScraper.scrape("com.microsoft.teams")

    // Verify screen only scraped once
    val screens = database.screenContextDao().getScreensForApp(appId)
    assertThat(screens).hasSize(1)  // Not 2!
}
```

### **Test 2: Mode Transition (Dynamic → LearnApp)**
```kotlin
@Test
fun `learnapp upgrades dynamic mode to comprehensive`() {
    // Dynamic scraping first
    dynamicScraper.scrape("com.microsoft.teams")
    val afterDynamic = database.scrapedAppDao().getAppByPackage("com.microsoft.teams")
    assertThat(afterDynamic.scrapingMode).isEqualTo("DYNAMIC")
    assertThat(afterDynamic.totalElements).isEqualTo(23)

    // LearnApp exploration
    learnApp.startExploration("com.microsoft.teams")
    val afterLearn = database.scrapedAppDao().getAppByPackage("com.microsoft.teams")
    assertThat(afterLearn.scrapingMode).isEqualTo("DYNAMIC")  // Switches back
    assertThat(afterLearn.isFullyLearned).isTrue()
    assertThat(afterLearn.totalElements).isGreaterThan(200)  // Found hidden content
}
```

### **Test 3: Incremental Learning**
```kotlin
@Test
fun `new screens detected after learnapp completes`() {
    // LearnApp completes
    learnApp.startExploration("com.microsoft.teams")
    val afterLearn = database.scrapedAppDao().getAppByPackage("com.microsoft.teams")
    assertThat(afterLearn.isFullyLearned).isTrue()

    // Simulate app update with new screen
    dynamicScraper.scrapeNewScreen("com.microsoft.teams", "NewFeatureScreen")

    // Verify app marked as incomplete again
    val afterUpdate = database.scrapedAppDao().getAppByPackage("com.microsoft.teams")
    assertThat(afterUpdate.isFullyLearned).isFalse()  // New content found
}
```

---

## 📁 Files to Create/Modify

### **NEW FILES (1):**
1. `ScrapingCoordinator.kt` (~300 lines) - Mode coordination

### **MODIFIED FILES (2):**
1. `AccessibilityScrapingIntegration.kt` (~100 lines changed) - Multi-window dynamic scraping
2. `ExplorationEngine.kt` (~50 lines changed) - Integration with coordinator

### **MODIFIED SCHEMAS (1):**
1. `ScrapedAppEntity.kt` (~20 lines added) - New fields
2. `AppScrapingDatabase.kt` - Migration 7→8

---

## ⏱️ Implementation Timeline

**Additional Time:** 4 hours (on top of existing 12-hour plan)

**Breakdown:**
- 2 hours: Create ScrapingCoordinator.kt
- 1 hour: Enhance AccessibilityScrapingIntegration.kt (multi-window dynamic)
- 0.5 hour: Integrate with ExplorationEngine.kt
- 0.5 hour: Database migration + DAO methods

**Total Project Time:** 16 hours (12 base + 4 integration)

---

## 🎯 Success Criteria

After implementation:

1. **Dynamic Scraping:**
   - ✅ Scrapes all windows (main + overlays)
   - ✅ Detects expandable controls (doesn't click them)
   - ✅ Marks apps as PARTIAL (not fully learned)
   - ✅ Deduplication prevents re-scraping same screens

2. **LearnApp:**
   - ✅ Uses existing dynamic data
   - ✅ Fills in gaps (clicks menus, explores all paths)
   - ✅ Marks apps as FULLY_LEARNED
   - ✅ Accurate completion percentage

3. **Integration:**
   - ✅ No duplicate elements in database
   - ✅ Mode transitions work correctly
   - ✅ Incremental learning detects new content
   - ✅ Both modes use same infrastructure

---

**Document Status:** DESIGN COMPLETE - Ready for Implementation
**Dependencies:** Requires Phase 1 (WindowManager, LauncherDetector, etc.) to be completed first
**Next Steps:** Implement after Phase 1-2 complete, before production deployment
