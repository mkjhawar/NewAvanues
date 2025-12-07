# LearnApp UX Improvements - Implementation Plan

**Feature:** LearnApp UX Improvements
**Spec:** specs/learnapp-ux-improvements/spec.md
**Platform:** Android
**Created:** 2025-11-28
**Status:** Ready for Implementation

---

## Executive Summary

**Total Estimated Time:** 24-30 hours
**Implementation Phases:** 5
**Critical Path:** Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5
**Swarm Recommendation:** No (single platform, sequential dependencies)
**Risk Level:** Medium (database changes, performance requirements)

---

## Implementation Strategy

### Phased Approach

This implementation uses a **sequential phased approach** where each phase builds on the previous:

**Phase 1: Immediate Fix** (1 hour)
- Restore broken functionality
- Critical priority, ships independently

**Phase 2: Database & Repository Layer** (4-6 hours)
- Foundation for all other phases
- Database schema changes
- Repository implementations

**Phase 3: Just-in-Time Learning** (6-8 hours)
- New core feature
- Passive learning engine
- Most complex technical component

**Phase 4: Settings UI** (8-10 hours)
- User interface for all three modes
- App list, search, filter, sort
- Integration with learning engine

**Phase 5: Improved Consent Dialog & Onboarding** (5-6 hours)
- Polish and first-run experience
- Final UX improvements

### Why Sequential (No Swarm)?

1. **Single Platform:** Android only (no multi-platform parallelization benefit)
2. **Strong Dependencies:** Phase 2 must complete before 3, 4, or 5 can start
3. **Shared Components:** All phases modify same codebase areas
4. **Database Schema:** Schema changes in Phase 2 block all other work
5. **Small Team Overhead:** Coordination overhead > time saved for 5 phases

**Estimated Time Savings from Swarm:** -10% (coordination overhead)
**Recommendation:** Sequential implementation

---

## Phase 1: Immediate Fix (Critical)

**Goal:** Restore LearnApp functionality immediately
**Time Estimate:** 1 hour
**Risk:** Low
**Can Ship Independently:** Yes

### Tasks

#### Task 1.1: Uncomment LearnAppIntegration Initialization
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`
**Action:** Uncomment line 922
**Time:** 5 minutes

```kotlin
// BEFORE (line 922):
// learnAppIntegration = LearnAppIntegration.initialize(applicationContext, this)

// AFTER:
learnAppIntegration = LearnAppIntegration.initialize(applicationContext, this)
```

#### Task 1.2: Verify SYSTEM_ALERT_WINDOW Permission
**File:** `app/src/main/AndroidManifest.xml`
**Action:** Verify permission exists
**Time:** 5 minutes

```xml
<!-- Should already exist, verify: -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

#### Task 1.3: Add Runtime Permission Check
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`
**Action:** Add permission check in onCreate() if not present
**Time:** 15 minutes

```kotlin
override fun onCreate() {
    super.onCreate()

    // Check overlay permission
    if (!Settings.canDrawOverlays(this)) {
        Log.w(TAG, "Overlay permission not granted, consent dialog will be limited")
        // Note: Can't request from service, user must grant in settings
    }

    // Initialize LearnApp
    learnAppIntegration = LearnAppIntegration.initialize(applicationContext, this)
}
```

#### Task 1.4: Add Debug Logging
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/integration/LearnAppIntegration.kt`
**Action:** Add debug logs for initialization and event flow
**Time:** 15 minutes

```kotlin
companion object {
    private const val TAG = "LearnAppIntegration"

    fun initialize(context: Context, service: AccessibilityService): LearnAppIntegration {
        Log.d(TAG, "Initializing LearnAppIntegration")
        return LearnAppIntegration(context, service).apply {
            Log.d(TAG, "LearnAppIntegration initialized successfully")
        }
    }
}

private fun setupEventListeners() {
    Log.d(TAG, "Setting up event listeners")
    scope.launch {
        appLaunchDetector.appLaunchEvents
            .collect { event ->
                Log.d(TAG, "Received app launch event: $event")
                // ... existing code
            }
    }
}
```

#### Task 1.5: Test Consent Dialog Appears
**Action:** Manual testing on device
**Time:** 20 minutes

**Test Steps:**
1. Rebuild and install app
2. Enable VoiceOS accessibility service
3. Launch a new app (e.g., Chrome)
4. Verify consent dialog appears within 500ms
5. Test Approve and Decline buttons
6. Verify preference is persisted

### Phase 1 Deliverables

- ✅ LearnAppIntegration initializes on service startup
- ✅ Consent dialog appears for new app launches
- ✅ Permission flow works correctly
- ✅ Debug logging in place

### Phase 1 Tests

**Unit Tests:**
- LearnAppIntegration.initialize() creates instance
- Permission check returns correct boolean

**Manual Tests:**
- Consent dialog appears for new apps
- Dialog responds to user input
- Preference persists across restarts

---

## Phase 2: Database & Repository Layer

**Goal:** Foundation for all new features
**Time Estimate:** 4-6 hours
**Dependencies:** None (can start immediately, but blocks all other phases)
**Risk:** Medium (schema changes)

### Tasks

#### Task 2.1: Extend learned_apps Table Schema
**File:** `libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/LearnedApp.sq`
**Action:** Add new columns to existing table
**Time:** 30 minutes

```sql
-- Add migration for new columns
ALTER TABLE learned_apps ADD COLUMN learning_mode TEXT NOT NULL DEFAULT 'AUTO_DETECT';
ALTER TABLE learned_apps ADD COLUMN status TEXT NOT NULL DEFAULT 'NOT_LEARNED';
ALTER TABLE learned_apps ADD COLUMN progress INTEGER NOT NULL DEFAULT 0;
ALTER TABLE learned_apps ADD COLUMN command_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE learned_apps ADD COLUMN screens_explored INTEGER NOT NULL DEFAULT 0;
ALTER TABLE learned_apps ADD COLUMN total_screens INTEGER;
ALTER TABLE learned_apps ADD COLUMN is_auto_detect_enabled INTEGER NOT NULL DEFAULT 1;
```

**Update queries:**
```sql
-- Update insert query to include new fields
insert:
INSERT OR REPLACE INTO learned_apps (
    package_name, app_name, learning_mode, status, progress,
    command_count, screens_explored, total_screens, last_updated,
    first_learned, version_code, is_auto_detect_enabled
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- Add new queries
getByLearningMode:
SELECT * FROM learned_apps WHERE learning_mode = ?;

getByStatus:
SELECT * FROM learned_apps WHERE status = ?;

updateProgress:
UPDATE learned_apps SET progress = ?, screens_explored = ? WHERE package_name = ?;

updateStatus:
UPDATE learned_apps SET status = ? WHERE package_name = ?;

getJustInTimeApps:
SELECT * FROM learned_apps WHERE status = 'JIT_ACTIVE';
```

#### Task 2.2: Create user_preferences Table
**File:** `libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/UserPreference.sq`
**Action:** Create new table and queries
**Time:** 30 minutes

```sql
CREATE TABLE user_preferences (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
);

-- Queries
getAllPreferences:
SELECT * FROM user_preferences;

getPreference:
SELECT value FROM user_preferences WHERE key = ?;

setPreference:
INSERT OR REPLACE INTO user_preferences (key, value) VALUES (?, ?);

deletePreference:
DELETE FROM user_preferences WHERE key = ?;

-- Common preferences
hasOnboardingCompleted:
SELECT value FROM user_preferences WHERE key = 'onboarding_completed';

getDefaultLearningMode:
SELECT value FROM user_preferences WHERE key = 'default_learning_mode';

isAutoDetectEnabled:
SELECT value FROM user_preferences WHERE key = 'auto_detect_enabled';
```

#### Task 2.3: Create app_consent_history Table
**File:** `libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/AppConsentHistory.sq`
**Action:** Create new table and queries
**Time:** 30 minutes

```sql
CREATE TABLE app_consent_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT NOT NULL,
    user_choice TEXT NOT NULL, -- APPROVED, DECLINED, DONT_ASK_AGAIN, SKIPPED
    timestamp INTEGER NOT NULL,
    FOREIGN KEY (package_name) REFERENCES learned_apps(package_name) ON DELETE CASCADE
);

CREATE INDEX idx_consent_package ON app_consent_history(package_name);
CREATE INDEX idx_consent_timestamp ON app_consent_history(timestamp);

-- Queries
insert:
INSERT OR REPLACE INTO app_consent_history (package_name, user_choice, timestamp)
VALUES (?, ?, ?);

getHistoryForApp:
SELECT * FROM app_consent_history WHERE package_name = ? ORDER BY timestamp DESC;

getLatestChoiceForApp:
SELECT user_choice FROM app_consent_history
WHERE package_name = ?
ORDER BY timestamp DESC
LIMIT 1;

getAllHistory:
SELECT * FROM app_consent_history ORDER BY timestamp DESC;

deleteHistoryForApp:
DELETE FROM app_consent_history WHERE package_name = ?;
```

#### Task 2.4: Create DTOs
**Files:** `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/`
**Action:** Create UserPreferenceDTO and AppConsentHistoryDTO
**Time:** 30 minutes

```kotlin
// UserPreferenceDTO.kt
data class UserPreferenceDTO(
    val key: String,
    val value: String
)

// AppConsentHistoryDTO.kt
data class AppConsentHistoryDTO(
    val id: Long,
    val packageName: String,
    val userChoice: ConsentChoice,
    val timestamp: Long
)

enum class ConsentChoice {
    APPROVED,
    DECLINED,
    DONT_ASK_AGAIN,
    SKIPPED
}

// Update LearnedAppDTO.kt
data class LearnedAppDTO(
    val packageName: String,
    val appName: String,
    val learningMode: LearningMode,
    val status: LearningStatus,
    val progress: Int,
    val commandCount: Int,
    val screensExplored: Int,
    val totalScreens: Int?,
    val lastUpdated: Long,
    val firstLearned: Long?,
    val versionCode: Int?,
    val isAutoDetectEnabled: Boolean
)

enum class LearningMode {
    AUTO_DETECT,
    MANUAL,
    JUST_IN_TIME
}

enum class LearningStatus {
    NOT_LEARNED,
    LEARNING,
    LEARNED,
    FAILED,
    JIT_ACTIVE
}
```

#### Task 2.5: Create Repository Interfaces
**Files:** `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/`
**Action:** Create IUserPreferenceRepository and IAppConsentHistoryRepository
**Time:** 30 minutes

```kotlin
// IUserPreferenceRepository.kt
interface IUserPreferenceRepository {
    suspend fun getPreference(key: String): String?
    suspend fun setPreference(key: String, value: String)
    suspend fun getAllPreferences(): List<UserPreferenceDTO>
    suspend fun deletePreference(key: String)

    // Convenience methods
    suspend fun hasOnboardingCompleted(): Boolean
    suspend fun getDefaultLearningMode(): LearningMode
    suspend fun isAutoDetectEnabled(): Boolean
}

// IAppConsentHistoryRepository.kt
interface IAppConsentHistoryRepository {
    suspend fun recordConsent(packageName: String, choice: ConsentChoice)
    suspend fun getHistoryForApp(packageName: String): List<AppConsentHistoryDTO>
    suspend fun getLatestChoiceForApp(packageName: String): ConsentChoice?
    suspend fun getAllHistory(): List<AppConsentHistoryDTO>
    suspend fun deleteHistoryForApp(packageName: String)
}
```

#### Task 2.6: Implement SQLDelight Repositories
**Files:** `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/`
**Action:** Implement repositories
**Time:** 1.5 hours

```kotlin
// SQLDelightUserPreferenceRepository.kt
class SQLDelightUserPreferenceRepository(
    private val queries: UserPreferenceQueries
) : IUserPreferenceRepository {

    override suspend fun getPreference(key: String): String? = withContext(Dispatchers.IO) {
        queries.getPreference(key).executeAsOneOrNull()
    }

    override suspend fun setPreference(key: String, value: String) = withContext(Dispatchers.IO) {
        queries.setPreference(key, value)
    }

    override suspend fun getAllPreferences(): List<UserPreferenceDTO> = withContext(Dispatchers.IO) {
        queries.getAllPreferences().executeAsList().map {
            UserPreferenceDTO(it.key, it.value)
        }
    }

    override suspend fun deletePreference(key: String) = withContext(Dispatchers.IO) {
        queries.deletePreference(key)
    }

    override suspend fun hasOnboardingCompleted(): Boolean = withContext(Dispatchers.IO) {
        queries.hasOnboardingCompleted().executeAsOneOrNull() == "true"
    }

    override suspend fun getDefaultLearningMode(): LearningMode = withContext(Dispatchers.IO) {
        val value = queries.getDefaultLearningMode().executeAsOneOrNull()
        LearningMode.valueOf(value ?: "AUTO_DETECT")
    }

    override suspend fun isAutoDetectEnabled(): Boolean = withContext(Dispatchers.IO) {
        queries.isAutoDetectEnabled().executeAsOneOrNull() == "true"
    }
}

// SQLDelightAppConsentHistoryRepository.kt
class SQLDelightAppConsentHistoryRepository(
    private val queries: AppConsentHistoryQueries
) : IAppConsentHistoryRepository {

    override suspend fun recordConsent(packageName: String, choice: ConsentChoice) = withContext(Dispatchers.IO) {
        queries.insert(packageName, choice.name, System.currentTimeMillis())
    }

    override suspend fun getHistoryForApp(packageName: String): List<AppConsentHistoryDTO> = withContext(Dispatchers.IO) {
        queries.getHistoryForApp(packageName).executeAsList().map {
            AppConsentHistoryDTO(
                id = it.id,
                packageName = it.package_name,
                userChoice = ConsentChoice.valueOf(it.user_choice),
                timestamp = it.timestamp
            )
        }
    }

    override suspend fun getLatestChoiceForApp(packageName: String): ConsentChoice? = withContext(Dispatchers.IO) {
        queries.getLatestChoiceForApp(packageName).executeAsOneOrNull()?.let {
            ConsentChoice.valueOf(it)
        }
    }

    override suspend fun getAllHistory(): List<AppConsentHistoryDTO> = withContext(Dispatchers.IO) {
        queries.getAllHistory().executeAsList().map {
            AppConsentHistoryDTO(
                id = it.id,
                packageName = it.package_name,
                userChoice = ConsentChoice.valueOf(it.user_choice),
                timestamp = it.timestamp
            )
        }
    }

    override suspend fun deleteHistoryForApp(packageName: String) = withContext(Dispatchers.IO) {
        queries.deleteHistoryForApp(packageName)
    }
}
```

#### Task 2.7: Update VoiceOSDatabaseManager
**File:** `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/VoiceOSDatabaseManager.kt`
**Action:** Add new repositories to manager
**Time:** 30 minutes

```kotlin
class VoiceOSDatabaseManager(
    private val database: Database
) {
    // Existing repositories
    val scrapedApps: IScrapedAppRepository = SQLDelightScrapedAppRepository(database.scrapedAppQueries)
    // ... other existing repositories

    // NEW repositories
    val userPreferences: IUserPreferenceRepository = SQLDelightUserPreferenceRepository(database.userPreferenceQueries)
    val appConsentHistory: IAppConsentHistoryRepository = SQLDelightAppConsentHistoryRepository(database.appConsentHistoryQueries)

    // Existing transaction support
    fun <R> transaction(body: () -> R): R = database.transaction { body() }
}
```

#### Task 2.8: Update Database Adapter
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/database/VoiceOSCoreDatabaseAdapter.kt`
**Action:** Expose new repositories
**Time:** 15 minutes

```kotlin
class VoiceOSCoreDatabaseAdapter private constructor(context: Context) {
    val databaseManager: VoiceOSDatabaseManager = // ... existing

    // NEW: Expose repositories for LearnApp
    val userPreferences: IUserPreferenceRepository
        get() = databaseManager.userPreferences

    val appConsentHistory: IAppConsentHistoryRepository
        get() = databaseManager.appConsentHistory

    // ... existing code
}
```

### Phase 2 Deliverables

- ✅ learned_apps table extended with new columns
- ✅ user_preferences table created
- ✅ app_consent_history table created
- ✅ All DTOs created
- ✅ All repository interfaces defined
- ✅ All repository implementations completed
- ✅ Database manager updated
- ✅ Adapter exposes new repositories

### Phase 2 Tests

**Unit Tests (90%+ coverage):**
```kotlin
// UserPreferenceRepositoryTest.kt
class UserPreferenceRepositoryTest {
    @Test
    fun testSetAndGetPreference()

    @Test
    fun testOnboardingCompleted()

    @Test
    fun testDefaultLearningMode()

    @Test
    fun testAutoDetectEnabled()
}

// AppConsentHistoryRepositoryTest.kt
class AppConsentHistoryRepositoryTest {
    @Test
    fun testRecordConsent()

    @Test
    fun testGetLatestChoice()

    @Test
    fun testMultipleConsentsPerApp()

    @Test
    fun testCascadeDelete()
}

// LearnedAppRepositoryTest.kt (update existing)
class LearnedAppRepositoryTest {
    @Test
    fun testNewFields()

    @Test
    fun testGetByLearningMode()

    @Test
    fun testGetByStatus()

    @Test
    fun testUpdateProgress()
}
```

---

## Phase 3: Just-in-Time Learning Engine

**Goal:** Implement passive screen-by-screen learning
**Time Estimate:** 6-8 hours
**Dependencies:** Phase 2 (database schema)
**Risk:** High (performance-critical)

### Tasks

#### Task 3.1: Create JustInTimeLearner Component
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/learning/JustInTimeLearner.kt`
**Action:** Create passive learning engine
**Time:** 2 hours

```kotlin
class JustInTimeLearner(
    private val context: Context,
    private val databaseAdapter: VoiceOSCoreDatabaseAdapter,
    private val scope: CoroutineScope
) {
    private val TAG = "JustInTimeLearner"

    // Track currently active JIT apps
    private val activeJitApps = mutableSetOf<String>()

    // Debounce screen events (100ms window)
    private val screenEventDebouncer = DebounceHelper(100L)

    // Cache of recently learned screens (avoid re-learning)
    private val learnedScreenCache = LruCache<String, Long>(50) // screen hash -> timestamp

    fun startJustInTimeLearning(packageName: String) {
        scope.launch {
            Log.d(TAG, "Starting just-in-time learning for $packageName")

            // Update database
            databaseAdapter.databaseManager.transaction {
                val app = learnedApps.getByPackage(packageName) ?: return@transaction
                learnedApps.updateStatus(packageName, LearningStatus.JIT_ACTIVE.name)
                learnedApps.updateProgress(packageName, 0, 0)
            }

            activeJitApps.add(packageName)
        }
    }

    fun stopJustInTimeLearning(packageName: String) {
        scope.launch {
            Log.d(TAG, "Stopping just-in-time learning for $packageName")
            activeJitApps.remove(packageName)
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only process window state changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Only process if JIT active for this app
        if (!activeJitApps.contains(packageName)) {
            return
        }

        // Debounce rapid events
        if (!screenEventDebouncer.shouldProcess(packageName)) {
            return
        }

        // Process in background (performance-critical: <50ms target)
        scope.launch(Dispatchers.Default) {
            processScreenForJitLearning(event, packageName)
        }
    }

    private suspend fun processScreenForJitLearning(event: AccessibilityEvent, packageName: String) {
        val startTime = System.currentTimeMillis()

        try {
            val source = event.source ?: return

            // Generate screen hash
            val screenHash = generateScreenHash(source)

            // Check cache - don't re-learn recent screens
            if (isScreenRecentlyLearned(screenHash)) {
                Log.d(TAG, "Screen already learned recently, skipping")
                return
            }

            // Extract elements from current screen only (no exploration)
            val elements = extractElementsFromNode(source)

            if (elements.isEmpty()) {
                Log.d(TAG, "No actionable elements found on screen")
                return
            }

            // Save to database
            saveJitScreenData(packageName, screenHash, elements)

            // Update cache
            learnedScreenCache.put(screenHash, System.currentTimeMillis())

            // Update progress
            incrementScreensLearned(packageName)

            // Show subtle notification
            showLearningNotification(packageName, elements.size)

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "JIT learning completed in ${duration}ms (target: <50ms)")

            if (duration > 50) {
                Log.w(TAG, "JIT learning exceeded performance target: ${duration}ms")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in JIT learning", e)
        }
    }

    private fun generateScreenHash(node: AccessibilityNodeInfo): String {
        // Use ElementHasher from scraping system
        return ElementHasher.hashNode(node)
    }

    private fun isScreenRecentlyLearned(screenHash: String): Boolean {
        val timestamp = learnedScreenCache.get(screenHash) ?: return false
        val age = System.currentTimeMillis() - timestamp
        return age < 60_000 // 1 minute cache
    }

    private fun extractElementsFromNode(node: AccessibilityNodeInfo): List<ScrapedElementDTO> {
        val elements = mutableListOf<ScrapedElementDTO>()

        fun traverse(n: AccessibilityNodeInfo, depth: Int = 0) {
            if (depth > 20) return // Prevent infinite recursion

            // Only extract actionable elements
            if (n.isClickable || n.isLongClickable || n.isCheckable || n.isFocusable) {
                elements.add(convertToScrapedElementDTO(n))
            }

            // Traverse children
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { traverse(it, depth + 1) }
            }
        }

        traverse(node)
        return elements
    }

    private suspend fun saveJitScreenData(
        packageName: String,
        screenHash: String,
        elements: List<ScrapedElementDTO>
    ) = withContext(Dispatchers.IO) {
        databaseAdapter.databaseManager.transaction {
            // Save screen context
            val screenContext = ScreenContextDTO(
                screenHash = screenHash,
                appId = packageName,
                timestamp = System.currentTimeMillis(),
                // ... other fields
            )
            screenContexts.insert(screenContext)

            // Save elements
            elements.forEach { element ->
                scrapedElements.insert(element)
            }
        }
    }

    private suspend fun incrementScreensLearned(packageName: String) = withContext(Dispatchers.IO) {
        val current = databaseAdapter.databaseManager.learnedApps.getByPackage(packageName)
        val newCount = (current?.screensExplored ?: 0) + 1
        databaseAdapter.databaseManager.learnedApps.updateProgress(packageName, 0, newCount)
    }

    private fun showLearningNotification(packageName: String, elementCount: Int) {
        // Show subtle toast (1 second, non-intrusive)
        scope.launch(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Learning this screen... ($elementCount elements)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    suspend fun convertToFullLearning(packageName: String) {
        // User requested to upgrade JIT to full learning
        Log.d(TAG, "Converting $packageName from JIT to full learning")

        stopJustInTimeLearning(packageName)

        // Update status
        databaseAdapter.databaseManager.learnedApps.updateStatus(
            packageName,
            LearningStatus.LEARNING.name
        )

        // Trigger full exploration (delegate to ExplorationEngine)
        // This will be called from UI layer
    }
}

class DebounceHelper(private val windowMs: Long) {
    private val lastProcessed = mutableMapOf<String, Long>()

    fun shouldProcess(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastProcessed[key] ?: 0

        return if (now - last > windowMs) {
            lastProcessed[key] = now
            true
        } else {
            false
        }
    }
}
```

#### Task 3.2: Integrate JustInTimeLearner into LearnAppIntegration
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/integration/LearnAppIntegration.kt`
**Action:** Wire up JIT learner
**Time:** 1 hour

```kotlin
class LearnAppIntegration private constructor(
    private val context: Context,
    private val service: AccessibilityService
) {
    // Existing components
    private val consentDialogManager: ConsentDialogManager
    private val appLaunchDetector: AppLaunchDetector
    private val explorationEngine: ExplorationEngine

    // NEW: JIT learner
    private val justInTimeLearner: JustInTimeLearner

    init {
        // ... existing initialization

        justInTimeLearner = JustInTimeLearner(
            context = context,
            databaseAdapter = VoiceOSCoreDatabaseAdapter.getInstance(context),
            scope = scope
        )

        // Load JIT-active apps on initialization
        scope.launch {
            loadJitActiveApps()
        }
    }

    private suspend fun loadJitActiveApps() {
        val adapter = VoiceOSCoreDatabaseAdapter.getInstance(context)
        val jitApps = adapter.databaseManager.learnedApps.getByStatus("JIT_ACTIVE")

        jitApps.forEach { app ->
            justInTimeLearner.startJustInTimeLearning(app.packageName)
        }

        Log.d(TAG, "Loaded ${jitApps.size} JIT-active apps")
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Existing: app launch detection
        appLaunchDetector.onAccessibilityEvent(event)

        // NEW: JIT learning
        justInTimeLearner.onAccessibilityEvent(event)
    }

    // NEW: Public API for JIT control
    suspend fun enableJustInTimeLearning(packageName: String, appName: String) {
        justInTimeLearner.startJustInTimeLearning(packageName)
    }

    suspend fun convertToFullLearning(packageName: String) {
        justInTimeLearner.convertToFullLearning(packageName)
        // Then start full exploration
        explorationEngine.startExploration(packageName)
    }
}
```

#### Task 3.3: Update ConsentDialogManager for Skip Button
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/ConsentDialogManager.kt`
**Action:** Add Skip button that activates JIT mode
**Time:** 1.5 hours

```kotlin
suspend fun showConsentDialog(packageName: String, appName: String) {
    // ... existing consent logic

    // NEW: Check consent history
    val consentHistory = VoiceOSCoreDatabaseAdapter.getInstance(context)
        .appConsentHistory.getLatestChoiceForApp(packageName)

    if (consentHistory == ConsentChoice.DONT_ASK_AGAIN) {
        Log.d(TAG, "User previously chose 'Don't ask again' for $packageName")
        return
    }

    if (consentHistory == ConsentChoice.SKIPPED) {
        // User previously skipped, JIT should already be active
        Log.d(TAG, "User previously skipped (JIT active) for $packageName")
        return
    }

    // Show dialog with NEW Skip button
    withContext(Dispatchers.Main) {
        consentDialog.show(
            appName = appName,
            onApprove = { dontAskAgain ->
                scope.launch {
                    handleApproval(packageName, dontAskAgain)
                }
            },
            onDecline = { dontAskAgain ->
                scope.launch {
                    handleDeclination(packageName, dontAskAgain)
                }
            },
            onSkip = { // NEW callback
                scope.launch {
                    handleSkip(packageName)
                }
            }
        )
    }
}

private suspend fun handleSkip(packageName: String) {
    Log.d(TAG, "User skipped full learning for $packageName (enabling JIT)")

    // Record consent choice
    VoiceOSCoreDatabaseAdapter.getInstance(context)
        .appConsentHistory.recordConsent(packageName, ConsentChoice.SKIPPED)

    // Enable just-in-time learning
    learnAppIntegration.enableJustInTimeLearning(packageName, appName)

    // Emit response
    _consentResponses.emit(
        ConsentResponse.Skipped(packageName = packageName)
    )

    // Dismiss dialog
    withContext(Dispatchers.Main) {
        consentDialog.dismiss()
    }
}

sealed class ConsentResponse {
    data class Approved(val packageName: String, val dontAskAgain: Boolean) : ConsentResponse()
    data class Declined(val packageName: String, val reason: String) : ConsentResponse()
    data class Skipped(val packageName: String) : ConsentResponse() // NEW
}
```

#### Task 3.4: Update ConsentDialogWidget UI
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/ConsentDialogWidget.kt`
**Action:** Add Skip button to dialog layout
**Time:** 1.5 hours

```kotlin
class ConsentDialogWidget(private val context: Context) {

    fun show(
        appName: String,
        onApprove: (dontAskAgain: Boolean) -> Unit,
        onDecline: (dontAskAgain: Boolean) -> Unit,
        onSkip: () -> Unit // NEW callback
    ) {
        // Create dialog view
        val dialogView = createDialogView(appName, onApprove, onDecline, onSkip)

        // ... show dialog
    }

    private fun createDialogView(
        appName: String,
        onApprove: (Boolean) -> Unit,
        onDecline: (Boolean) -> Unit,
        onSkip: () -> Unit
    ): View {
        // Inflate improved layout
        val view = LayoutInflater.from(context).inflate(
            R.layout.dialog_consent_improved,
            null
        )

        // Title
        view.findViewById<TextView>(R.id.title).text = "🆕 New App Detected: $appName"

        // Description (updated with benefits)
        view.findViewById<TextView>(R.id.description).text = """
            🎤 Would you like to learn $appName?

            This will:
            • Map buttons and screens (2-3 min)
            • Create ~30-50 voice commands
            • Let you control $appName with voice

            Or:
            • Skip to learn naturally as you use it
            • Learn later from Settings
        """.trimIndent()

        // Checkboxes
        val dontAskCheckbox = view.findViewById<CheckBox>(R.id.checkbox_dont_ask)
        dontAskCheckbox.text = "Don't ask again for $appName"

        val disableAllCheckbox = view.findViewById<CheckBox>(R.id.checkbox_disable_all)
        disableAllCheckbox.text = "Disable auto-detection for all apps"

        // Buttons
        view.findViewById<Button>(R.id.btn_not_now).setOnClickListener {
            val dontAsk = dontAskCheckbox.isChecked
            onDecline(dontAsk)

            if (disableAllCheckbox.isChecked) {
                disableAutoDetectionGlobally()
            }
        }

        // NEW: Skip button
        view.findViewById<Button>(R.id.btn_skip).setOnClickListener {
            onSkip()

            if (disableAllCheckbox.isChecked) {
                disableAutoDetectionGlobally()
            }
        }

        view.findViewById<Button>(R.id.btn_learn_now).setOnClickListener {
            val dontAsk = dontAskCheckbox.isChecked
            onApprove(dontAsk)

            if (disableAllCheckbox.isChecked) {
                disableAutoDetectionGlobally()
            }
        }

        return view
    }

    private fun disableAutoDetectionGlobally() {
        // Save preference
        CoroutineScope(Dispatchers.IO).launch {
            VoiceOSCoreDatabaseAdapter.getInstance(context)
                .userPreferences.setPreference("auto_detect_enabled", "false")
        }
    }
}
```

#### Task 3.5: Create Improved Dialog Layout
**File:** `modules/apps/VoiceOSCore/src/main/res/layout/dialog_consent_improved.xml`
**Action:** Create new layout with Skip button
**Time:** 1 hour

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp"
    android:background="@drawable/dialog_background">

    <!-- Title -->
    <TextView
        android:id="@+id/title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="20sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary"
        android:contentDescription="Dialog title" />

    <!-- Description -->
    <TextView
        android:id="@+id/description"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:textSize="16sp"
        android:textColor="@color/text_secondary"
        android:lineSpacingMultiplier="1.2" />

    <!-- Checkboxes -->
    <CheckBox
        android:id="@+id/checkbox_dont_ask"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:minHeight="48dp"
        android:textSize="14sp" />

    <CheckBox
        android:id="@+id/checkbox_disable_all"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:minHeight="48dp"
        android:textSize="14sp" />

    <!-- Button row -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:orientation="horizontal"
        android:gravity="end">

        <Button
            android:id="@+id/btn_not_now"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:minWidth="88dp"
            android:minHeight="48dp"
            android:text="Not Now"
            android:contentDescription="Decline learning for now" />

        <!-- NEW: Skip button -->
        <Button
            android:id="@+id/btn_skip"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:minWidth="88dp"
            android:minHeight="48dp"
            android:text="Skip"
            android:contentDescription="Skip full learning, enable just-in-time mode" />

        <Button
            android:id="@+id/btn_learn_now"
            style="@style/Widget.Material3.Button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:minWidth="88dp"
            android:minHeight="48dp"
            android:text="Learn Now"
            android:contentDescription="Start learning this app now" />
    </LinearLayout>
</LinearLayout>
```

### Phase 3 Deliverables

- ✅ JustInTimeLearner component implemented
- ✅ Passive learning on AccessibilityEvent (<50ms performance)
- ✅ Skip button in consent dialog
- ✅ JIT mode persists across app restarts
- ✅ Subtle "Learning..." notifications
- ✅ Convert JIT → Full learning API

### Phase 3 Tests

**Unit Tests:**
```kotlin
// JustInTimeLearnerTest.kt
class JustInTimeLearnerTest {
    @Test
    fun testStartJitLearning()

    @Test
    fun testPassiveScreenLearning()

    @Test
    fun testPerformanceUnder50ms()

    @Test
    fun testDebouncingPreventsRapidFire()

    @Test
    fun testCachePreventsDuplicates()

    @Test
    fun testConvertToFullLearning()
}

// ConsentDialogManagerTest.kt (updated)
class ConsentDialogManagerTest {
    @Test
    fun testSkipButtonActivatesJit()

    @Test
    fun testSkipRecordedInHistory()

    @Test
    fun testDontAskAgainRespected()
}
```

**Performance Tests:**
```kotlin
@Test
fun testJitLearningLatency() {
    val events = generateMockAccessibilityEvents(100)

    val durations = events.map { event ->
        measureTimeMillis {
            justInTimeLearner.onAccessibilityEvent(event)
        }
    }

    val avgDuration = durations.average()
    val maxDuration = durations.maxOrNull()

    assertThat(avgDuration).isLessThan(50.0) // <50ms average
    assertThat(maxDuration).isLessThan(100) // <100ms worst case
}
```

---

## Phase 4: Settings UI (Manual Learning)

**Goal:** User interface for all three learning modes
**Time Estimate:** 8-10 hours
**Dependencies:** Phase 2 (database), Phase 3 (JIT engine)
**Risk:** Medium (complex UI, state management)

### Tasks

#### Task 4.1: Create VoiceLearningActivity
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/VoiceLearningActivity.kt`
**Action:** Create main settings activity
**Time:** 2 hours

```kotlin
class VoiceLearningActivity : AppCompatActivity() {

    private val viewModel: AppLearningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_learning)

        setupToolbar()
        setupSearchBar()
        setupFilterSort()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Voice Learning"
    }

    private fun setupSearchBar() {
        val searchView = findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun setupFilterSort() {
        findViewById<Chip>(R.id.chip_filter_all).setOnClickListener {
            viewModel.setFilter(AppFilter.ALL)
        }
        findViewById<Chip>(R.id.chip_filter_learned).setOnClickListener {
            viewModel.setFilter(AppFilter.LEARNED)
        }
        findViewById<Chip>(R.id.chip_filter_not_learned).setOnClickListener {
            viewModel.setFilter(AppFilter.NOT_LEARNED)
        }
        findViewById<Chip>(R.id.chip_filter_jit).setOnClickListener {
            viewModel.setFilter(AppFilter.JUST_IN_TIME)
        }

        findViewById<Button>(R.id.btn_sort).setOnClickListener {
            showSortOptions()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val adapter = AppLearningAdapter(
            onLearnClick = { app -> viewModel.startLearning(app.packageName) },
            onJitClick = { app -> viewModel.enableJustInTime(app.packageName) },
            onAutoDetectClick = { app -> viewModel.enableAutoDetect(app.packageName) },
            onReLearnClick = { app -> viewModel.reLearn(app.packageName) },
            onViewCommandsClick = { app -> viewCommands(app.packageName) },
            onCompleteClick = { app -> viewModel.convertToFullLearning(app.packageName) },
            onCancelClick = { app -> viewModel.cancelLearning(app.packageName) }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupObservers() {
        viewModel.filteredApps.observe(this) { apps ->
            (findViewById<RecyclerView>(R.id.recycler_view).adapter as AppLearningAdapter)
                .submitList(apps)
        }

        viewModel.learningProgress.observe(this) { progress ->
            // Update progress for apps currently being learned
            // Progress is Map<PackageName, LearningProgress>
        }
    }

    // ... helper methods
}
```

#### Task 4.2: Create AppLearningViewModel
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/AppLearningViewModel.kt`
**Action:** State management for settings UI
**Time:** 2.5 hours

```kotlin
class AppLearningViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseAdapter = VoiceOSCoreDatabaseAdapter.getInstance(application)
    private val learnAppIntegration = // Get from VoiceOSService

    // State
    private val _allApps = MutableLiveData<List<AppLearningUI State>>()
    private val _searchQuery = MutableLiveData("")
    private val _currentFilter = MutableLiveData(AppFilter.ALL)
    private val _currentSort = MutableLiveData(AppSort.ALPHABETICAL)

    // Derived state
    val filteredApps: LiveData<List<AppLearningUIState>> = combine(
        _allApps, _searchQuery, _currentFilter, _currentSort
    ) { apps, query, filter, sort ->
        apps.orEmpty()
            .filter { it.matchesSearch(query) }
            .filter { it.matchesFilter(filter) }
            .sortedBy(sort)
    }

    // Learning progress (real-time)
    val learningProgress = MutableLiveData<Map<String, LearningProgress>>()

    init {
        loadAllApps()
        observeLearningProgress()
    }

    private fun loadAllApps() {
        viewModelScope.launch {
            val installedApps = getInstalledApps()
            val learnedApps = databaseAdapter.databaseManager.learnedApps.getAll()

            val uiStates = installedApps.map { packageInfo ->
                val learned = learnedApps.find { it.packageName == packageInfo.packageName }

                AppLearningUIState(
                    packageName = packageInfo.packageName,
                    appName = packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                    icon = packageInfo.applicationInfo.loadIcon(packageManager),
                    status = learned?.status?.let { LearningStatus.valueOf(it) } ?: LearningStatus.NOT_LEARNED,
                    learningMode = learned?.learningMode?.let { LearningMode.valueOf(it) },
                    progress = learned?.progress ?: 0,
                    commandCount = learned?.commandCount ?: 0,
                    screensExplored = learned?.screensExplored ?: 0,
                    totalScreens = learned?.totalScreens,
                    lastUpdated = learned?.lastUpdated
                )
            }

            _allApps.postValue(uiStates)
        }
    }

    private fun getInstalledApps(): List<PackageInfo> {
        val pm = getApplication<Application>().packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Non-system apps
            .mapNotNull { pm.getPackageInfo(it.packageName, 0) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _currentFilter.value = filter
    }

    fun setSort(sort: AppSort) {
        _currentSort.value = sort
    }

    // Actions
    fun startLearning(packageName: String) {
        viewModelScope.launch {
            // Update status to LEARNING
            databaseAdapter.databaseManager.learnedApps.updateStatus(
                packageName,
                LearningStatus.LEARNING.name
            )

            // Start exploration
            learnAppIntegration.startFullLearning(packageName)

            // Reload
            loadAllApps()
        }
    }

    fun enableJustInTime(packageName: String) {
        viewModelScope.launch {
            val app = _allApps.value?.find { it.packageName == packageName }
            learnAppIntegration.enableJustInTimeLearning(packageName, app?.appName ?: "")
            loadAllApps()
        }
    }

    fun enableAutoDetect(packageName: String) {
        viewModelScope.launch {
            databaseAdapter.databaseManager.learnedApps.updateAutoDetect(packageName, true)
            loadAllApps()
        }
    }

    fun reLearn(packageName: String) {
        // Same as startLearning but clears existing data first
        viewModelScope.launch {
            // Clear existing learned data
            databaseAdapter.databaseManager.transaction {
                scrapedElements.deleteByApp(packageName)
                generatedCommands.deleteByApp(packageName)
                // ... clear other related data
            }

            startLearning(packageName)
        }
    }

    fun convertToFullLearning(packageName: String) {
        viewModelScope.launch {
            learnAppIntegration.convertToFullLearning(packageName)
            loadAllApps()
        }
    }

    fun cancelLearning(packageName: String) {
        viewModelScope.launch {
            learnAppIntegration.cancelExploration(packageName)

            databaseAdapter.databaseManager.learnedApps.updateStatus(
                packageName,
                LearningStatus.NOT_LEARNED.name
            )

            loadAllApps()
        }
    }

    private fun observeLearningProgress() {
        // Observe exploration progress from LearnAppIntegration
        // Update learningProgress LiveData in real-time
    }
}

data class AppLearningUIState(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val status: LearningStatus,
    val learningMode: LearningMode?,
    val progress: Int,
    val commandCount: Int,
    val screensExplored: Int,
    val totalScreens: Int?,
    val lastUpdated: Long?
) {
    fun matchesSearch(query: String): Boolean {
        return query.isEmpty() || appName.contains(query, ignoreCase = true)
    }

    fun matchesFilter(filter: AppFilter): Boolean {
        return when (filter) {
            AppFilter.ALL -> true
            AppFilter.LEARNED -> status == LearningStatus.LEARNED
            AppFilter.NOT_LEARNED -> status == LearningStatus.NOT_LEARNED
            AppFilter.IN_PROGRESS -> status == LearningStatus.LEARNING
            AppFilter.JUST_IN_TIME -> status == LearningStatus.JIT_ACTIVE
        }
    }
}

enum class AppFilter {
    ALL, LEARNED, NOT_LEARNED, IN_PROGRESS, JUST_IN_TIME
}

enum class AppSort {
    ALPHABETICAL, RECENTLY_USED, LEARNING_STATUS
}
```

#### Task 4.3: Create AppLearningAdapter (RecyclerView)
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/AppLearningAdapter.kt`
**Action:** Adapter for app list
**Time:** 2 hours

(Implementation omitted for brevity - standard RecyclerView ListAdapter)

#### Task 4.4: Create Layouts
**Files:** Various layout files
**Action:** Create activity and list item layouts
**Time:** 2.5 hours

(Layout XML omitted for brevity - includes activity_voice_learning.xml, item_app_learned.xml, item_app_learning.xml, item_app_not_learned.xml, item_app_jit.xml)

#### Task 4.5: Wire up from Settings
**File:** Add entry point to launch VoiceLearningActivity
**Time:** 30 minutes

### Phase 4 Deliverables

- ✅ VoiceLearningActivity implemented
- ✅ AppLearningViewModel with state management
- ✅ RecyclerView adapter for app list
- ✅ Search, filter, sort functionality
- ✅ All three learning mode buttons functional
- ✅ Real-time progress updates

### Phase 4 Tests

**UI Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class VoiceLearningActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(VoiceLearningActivity::class.java)

    @Test
    fun testSearchFiltersApps()

    @Test
    fun testFilterChipsWork()

    @Test
    fun testLearnButtonStartsLearning()

    @Test
    fun testJitButtonActivatesJit()

    @Test
    fun testProgressUpdatesInRealTime()
}
```

---

## Phase 5: Onboarding & Polish

**Goal:** First-run experience and final UX polish
**Time Estimate:** 5-6 hours
**Dependencies:** All previous phases
**Risk:** Low

### Tasks

#### Task 5.1: Create OnboardingActivity
**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/OnboardingActivity.kt`
**Action:** 3-screen onboarding flow
**Time:** 2.5 hours

(Implementation follows standard ViewPager2 pattern with 3 fragments)

#### Task 5.2: Login Flow Support in ExplorationEngine
**File:** Update ExplorationEngine to detect and pause for login
**Time:** 2 hours

(Add login screen detection heuristics and pause/resume logic)

#### Task 5.3: Final Polish & Bug Fixes
**Action:** Address any issues from testing
**Time:** 1.5 hours

### Phase 5 Deliverables

- ✅ Onboarding flow complete
- ✅ Login detection and pause/resume
- ✅ All polish items addressed

### Phase 5 Tests

**Integration Tests:**
```kotlin
@Test
fun testOnboardingCompletionSetsPreference()

@Test
fun testGuidedModeLearnsSampleApps()

@Test
fun testLoginDetectionPausesExploration()
```

---

## Testing Strategy

### Unit Test Coverage Target: 90%+

**Critical Components:**
- All repositories (100% coverage)
- JustInTimeLearner (95% coverage)
- ConsentDialogManager (90% coverage)
- AppLearningViewModel (90% coverage)

### Integration Tests

1. End-to-end learning flows
2. Database persistence across modes
3. Permission handling
4. Real-time progress updates

### Performance Tests

1. JIT learning latency (<50ms)
2. Memory usage during exploration
3. Battery impact measurement
4. Database write throughput

### UI Tests

1. All user flows in VoiceLearningActivity
2. Consent dialog interactions
3. Onboarding navigation
4. Accessibility (TalkBack)

---

## Risk Mitigation

### Risk: JIT Performance Impact

**Mitigation:**
- Aggressive event filtering
- Debouncing (100ms window)
- Background processing (Dispatchers.Default)
- Performance monitoring and alerts

### Risk: Database Schema Migration Issues

**Mitigation:**
- Test migrations on existing data
- Backup existing learned_apps data
- Rollback plan if migration fails

### Risk: User Confusion (3 Modes)

**Mitigation:**
- Clear onboarding explaining each mode
- Sensible defaults (Auto-detect for most users)
- In-app help tooltips
- User testing before launch

---

## Rollout Schedule

**Week 1:** Phase 1 (Immediate Fix)
- Deploy ASAP to restore functionality
- Monitor crash logs
- Gather feedback on basic consent dialog

**Week 2-3:** Phases 2 & 3 (Database + JIT)
- Internal testing only
- Performance validation
- Database migration testing

**Week 4:** Phase 4 (Settings UI)
- Beta release to 50-100 users
- Gather UX feedback on three modes
- A/B test usage patterns

**Week 5:** Phase 5 (Onboarding + Polish)
- Full release
- Onboarding for all new users
- Monitor success metrics

---

## Success Criteria

**Phase 1:**
- ✅ 0 crashes from LearnAppIntegration
- ✅ Consent dialog shows for 95%+ new app launches

**Phase 2:**
- ✅ All database migrations succeed
- ✅ 0 data loss from existing users

**Phase 3:**
- ✅ JIT learning <50ms average latency
- ✅ 40%+ users try Skip button (JIT mode)

**Phase 4:**
- ✅ Settings UI loads <500ms
- ✅ Search/filter/sort all functional

**Phase 5:**
- ✅ 80%+ onboarding completion rate
- ✅ Login detection 95%+ accurate

---

## Post-Implementation

### Monitoring

- Crash rates (target: <2%)
- JIT performance metrics (target: <50ms avg)
- Battery usage (target: <5% per hour)
- Learning success rate (target: 90%+ completions)

### Analytics

- Mode usage breakdown (Auto vs Manual vs JIT)
- Skip button click-through rate
- Onboarding completion rate
- Average apps learned per user

### Future Work (V2)

Documented in spec.md Appendix - Out of Scope section.

---

**Plan Version:** 1.0
**Status:** Ready for Implementation
**Next Step:** Begin Phase 1 (Immediate Fix)
