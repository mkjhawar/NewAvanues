# LearnApp Phase 3 - OS-Level Architecture Analysis

**Date:** 2025-10-30 22:54 PDT
**Analyst:** Android OS Architecture Expert
**Purpose:** OS-level concerns and system integration patterns for Phase 3 consolidation
**Context:** Production deployment readiness assessment

---

## Executive Summary

This analysis evaluates VoiceOS Phase 3 architecture from an Android OS-level perspective, focusing on system service integration, process architecture, resource management, and production deployment concerns.

**Key Findings:**
- ✅ Current in-process architecture is CORRECT for production
- ✅ Single AccessibilityService pattern is optimal for API 29+
- ⚠️ Two separate databases create synchronization risks
- ⚠️ No explicit resource budgeting or monitoring
- ⚠️ Android 11+ QUERY_ALL_PACKAGES permission required but not declared
- ✅ Multi-window support properly implemented (Phase 1)

**Critical Recommendations:**
1. **Database Consolidation:** Merge LearnAppDatabase into VoiceOSAppDatabase (HIGH PRIORITY)
2. **Resource Monitoring:** Add MemoryMonitor and CPUMonitor classes
3. **Permission Hardening:** Add QUERY_ALL_PACKAGES to manifest with justification
4. **Rollout Strategy:** Implement feature flags for safe Phase 3 deployment

**Overall Risk:** MEDIUM-LOW (architecture sound, needs operational hardening)

---

## 1. System Service Coordination

### Current Architecture (✅ CORRECT)

**VoiceOS System Service Integration:**
```
VoiceOSService (AccessibilityService)
├── PackageManager (launcher detection, app metadata)
├── WindowManager (multi-window detection via AccessibilityService API)
└── ActivityManager (implicit via AccessibilityEvent.packageName)
```

**Implementation Details:**
- **PackageManager:** Used in `LauncherDetector` (Phase 1)
  - `queryIntentActivities()` for launcher identification
  - `getPackageInfo()` for version codes (with cache optimization)
  - ✅ Proper cache invalidation on window state changes

- **WindowManager:** Accessed via `AccessibilityService.getWindows()`
  - Phase 1 added `FLAG_RETRIEVE_INTERACTIVE_WINDOWS` (API 21+)
  - Multi-window detection using `AccessibilityWindowInfo`
  - ✅ Correctly uses accessibility API (not direct WindowManager)

- **ActivityManager:** Implicit usage
  - Foreground app detected via `AccessibilityEvent.packageName`
  - No direct ActivityManager queries
  - ✅ Efficient event-driven approach

### ADR-013: System Service Integration Pattern

**Decision:** Use AccessibilityService as primary system integration point

**Rationale:**
1. **Single Source of Truth:** AccessibilityEvent provides package, window, and component info
2. **Battery Efficiency:** Event-driven (0-100 Hz) vs polling (∞ Hz)
3. **Permission Minimization:** No PACKAGE_USAGE_STATS or REAL_GET_TASKS needed
4. **Multi-Window Native Support:** FLAG_RETRIEVE_INTERACTIVE_WINDOWS (API 21+)

**Implementation:**
```kotlin
// CORRECT (current implementation)
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
        TYPE_WINDOW_STATE_CHANGED -> {
            val packageName = event.packageName.toString()
            val windows = windows // Uses FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            // Process multi-window state
        }
    }
}

// ANTI-PATTERN (do NOT do this)
// ❌ Polling ActivityManager (battery drain)
scope.launch {
    while (true) {
        val topApp = activityManager.getRunningTasks(1)
        delay(100) // 10 Hz polling = battery drain
    }
}
```

**Monitoring Requirements:**
- Track `AccessibilityEvent` frequency (should be 0-100 Hz)
- Monitor `getWindows()` call frequency (should match TYPE_WINDOW_STATE_CHANGED)
- Log PackageManager cache hit rate (target >90%)

---

## 2. Process Architecture

### ADR-014: In-Process Scraping (Recommended)

**Decision:** Keep all scraping in VoiceOSService process (in-process architecture)

**Architecture:**
```
┌─────────────────────────────────────────────┐
│ VoiceOSService Process                      │
│ ├── AccessibilityService                    │
│ │   └── onAccessibilityEvent() [Main Thread]│
│ ├── AccessibilityScrapingIntegration        │
│ │   ├── DYNAMIC scraping [IO Dispatcher]    │
│ │   └── LEARN_APP scraping [IO Dispatcher]  │
│ ├── Room Databases                          │
│ │   ├── VoiceOSAppDatabase (DYNAMIC)        │
│ │   └── LearnAppDatabase (LEARN_APP)        │ ← CONSOLIDATE THIS
│ └── Coroutines                               │
│     ├── Main Dispatcher (UI, events)        │
│     ├── IO Dispatcher (database, scraping)  │
│     └── Default Dispatcher (CPU work)       │
└─────────────────────────────────────────────┘
```

### Option Analysis

#### ✅ Option A: In-Process (RECOMMENDED - Current)

**Pros:**
- **Lowest Latency:** Direct memory access, no IPC overhead
- **Simplest Lifecycle:** Single service lifecycle management
- **No Binder Limits:** No 1MB Binder transaction limit issues
- **Easier Debugging:** Single process to attach debugger
- **Memory Sharing:** Efficient AccessibilityNodeInfo caching

**Cons:**
- **ANR Risk:** Heavy scraping can cause ANR (mitigated by coroutines)
- **Memory Pressure:** All scraping in one process (need monitoring)
- **Crash Impact:** Service crash loses both modes (acceptable risk)

**Production Suitability:** ✅ EXCELLENT
- ANR mitigated by CoroutineScope(SupervisorJob + Dispatchers.IO)
- Memory pressure manageable with current footprint (~15MB idle target)
- Crash isolation not critical (both modes dependent on accessibility)

#### ❌ Option B: Out-of-Process (NOT RECOMMENDED)

**Pros:**
- **Crash Isolation:** LEARN_APP crash doesn't kill DYNAMIC
- **ANR Isolation:** Heavy LEARN_APP work won't ANR main service
- **Resource Limits:** Separate memory/CPU quotas

**Cons:**
- **IPC Overhead:** AccessibilityNodeInfo transfer via Binder (1MB limit)
- **Latency Penalty:** 2-5ms IPC roundtrip per scraping operation
- **Lifecycle Complexity:** Two service lifecycles to coordinate
- **Battery Cost:** Two processes = 2x overhead (~2-5MB per process)
- **Binder Starvation:** Deep UI trees can exceed 1MB Binder limit

**Production Suitability:** ❌ POOR
- IPC overhead negates performance benefits
- Binder limits problematic for complex UIs
- Two-process coordination adds complexity
- Not justified by risk profile

#### ⚠️ Option C: Hybrid (CONDITIONAL)

**Architecture:**
```
Main Process:
├── DYNAMIC scraping (hot path, real-time)
└── Room write operations

Scraper Process:
├── LEARN_APP scraping (cold path, batch)
└── Room read operations
```

**When to Consider:**
- LEARN_APP crashes become frequent (>1% of sessions)
- ANRs occur during LEARN_APP despite coroutines
- Memory pressure consistently exceeds 100MB

**Current Justification:** ❌ NOT NEEDED
- No evidence of crashes or ANRs
- Memory footprint well within limits
- Added complexity not justified

### ADR-014 Recommendation

**Keep in-process architecture with operational monitoring:**

1. **ANR Prevention:**
   ```kotlin
   class AccessibilityScrapingIntegration {
       private val scrapingScope = CoroutineScope(
           SupervisorJob() +
           Dispatchers.IO +
           CoroutineExceptionHandler { _, ex ->
               Log.e(TAG, "Scraping error", ex)
           }
       )

       fun onAccessibilityEvent(event: AccessibilityEvent) {
           scrapingScope.launch {
               withContext(Dispatchers.IO) {
                   // Long-running work off main thread
                   scrapeCurrentWindow(event)
               }
           }
       }
   }
   ```

2. **Memory Monitoring:**
   ```kotlin
   class MemoryMonitor(private val context: Context) {
       fun checkMemoryPressure(): MemoryStatus {
           val runtime = Runtime.getRuntime()
           val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576

           return when {
               usedMB > 100 -> MemoryStatus.CRITICAL
               usedMB > 50 -> MemoryStatus.WARNING
               else -> MemoryStatus.NORMAL
           }
       }
   }
   ```

3. **ANR Watchdog:**
   ```kotlin
   class ANRWatchdog {
       private val mainHandler = Handler(Looper.getMainLooper())

       fun checkMainThreadResponsiveness() {
           val start = System.currentTimeMillis()
           mainHandler.post {
               val latency = System.currentTimeMillis() - start
               if (latency > 100) { // >100ms = potential ANR
                   Log.w(TAG, "Main thread latency: ${latency}ms")
               }
           }
       }
   }
   ```

---

## 3. Resource Management

### Current State Assessment

**Memory Management:** ⚠️ NO EXPLICIT BUDGETING
- Target: <15MB idle (from build.gradle comments)
- Actual: Unknown (no monitoring in production)
- Two databases: LearnAppDatabase + AppScrapingDatabase (duplication risk)
- No memory pressure detection
- No cache eviction policy

**CPU Management:** ⚠️ NO EXPLICIT LIMITS
- Background scraping uses Dispatchers.IO (good)
- No CPU throttling during battery saver mode
- No scraping rate limiting
- Thread pool size: Dispatchers.IO default (64 threads max)

**Battery Management:** ⚠️ BASIC PROTECTION ONLY
- Phase 3 check: `MIN_BATTERY_LEVEL_FOR_LEARNING = 20`
- No Doze mode compatibility checks
- No JobScheduler for deferrable work
- Foreground service pattern used (good for user awareness)

**Storage Management:** ⚠️ NO CLEANUP STRATEGY
- Two databases grow unbounded
- No TTL on scraped data
- No database compaction
- Room WAL mode not explicitly configured

### ADR-015: Resource Budget Specification

**Memory Budget:**
```kotlin
object ResourceBudget {
    // Process Memory Limits
    const val MEMORY_IDLE_TARGET_MB = 15
    const val MEMORY_WARNING_THRESHOLD_MB = 50
    const val MEMORY_CRITICAL_THRESHOLD_MB = 100

    // Database Limits
    const val DATABASE_MAX_SIZE_MB = 50
    const val SCRAPED_ELEMENTS_MAX_AGE_DAYS = 30
    const val SESSION_MAX_AGE_DAYS = 90

    // Cache Limits
    const val PACKAGE_INFO_CACHE_SIZE = 100
    const val NODE_INFO_CACHE_SIZE = 50

    // Performance Limits
    const val SCRAPING_MAX_DEPTH = 50 // Already in code
    const val SCRAPING_TIMEOUT_MS = 5000
    const val SCRAPING_MAX_RATE_PER_MIN = 60
}
```

**CPU Budget:**
```kotlin
class CPUThrottler {
    fun shouldThrottleScraping(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)

        return when {
            // Battery Saver mode active
            powerManager.isPowerSaveMode -> true

            // Battery low
            getBatteryLevel() < 20 -> true

            // Doze mode
            powerManager.isDeviceIdleMode -> true

            else -> false
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
```

**Storage Budget:**
```kotlin
class StorageManager(private val database: VoiceOSAppDatabase) {
    suspend fun enforceStorageLimits() {
        // Delete old scraped elements
        database.scrapedElementDao().deleteOlderThan(
            Date(System.currentTimeMillis() - 30.days.inWholeMilliseconds)
        )

        // Delete old sessions
        database.explorationSessionDao().deleteOlderThan(
            Date(System.currentTimeMillis() - 90.days.inWholeMilliseconds)
        )

        // Vacuum database
        database.query("VACUUM", emptyArray())
    }

    suspend fun getDatabaseSize(): Long {
        val dbFile = context.getDatabasePath("voiceos_app_database")
        return dbFile.length()
    }
}
```

### Resource Monitoring Dashboard

**Metrics to Track:**
```kotlin
data class ResourceMetrics(
    val timestamp: Long = System.currentTimeMillis(),

    // Memory
    val memoryUsedMB: Long,
    val memoryAvailableMB: Long,
    val memoryStatus: MemoryStatus,

    // CPU
    val scrapingEventsPerMin: Int,
    val mainThreadLatencyMs: Long,
    val backgroundThreadCount: Int,

    // Storage
    val databaseSizeMB: Long,
    val scrapedElementCount: Int,
    val sessionCount: Int,

    // Battery
    val batteryLevel: Int,
    val isPowerSaveMode: Boolean,
    val isCharging: Boolean,

    // Performance
    val lastScrapingDurationMs: Long,
    val avgScrapingDurationMs: Long,
    val scrapingErrorRate: Float
)

enum class MemoryStatus {
    NORMAL,     // <50MB
    WARNING,    // 50-100MB
    CRITICAL    // >100MB
}
```

**Action Thresholds:**
```kotlin
class ResourceEnforcer(private val metrics: ResourceMetrics) {
    fun getRecommendedAction(): ResourceAction {
        return when {
            // Critical memory pressure
            metrics.memoryStatus == MemoryStatus.CRITICAL ->
                ResourceAction.STOP_LEARNING

            // High memory + low battery
            metrics.memoryStatus == MemoryStatus.WARNING &&
            metrics.batteryLevel < 30 ->
                ResourceAction.THROTTLE_LEARNING

            // Battery saver mode
            metrics.isPowerSaveMode ->
                ResourceAction.DYNAMIC_ONLY

            // Database too large
            metrics.databaseSizeMB > 50 ->
                ResourceAction.CLEANUP_OLD_DATA

            else ->
                ResourceAction.NORMAL_OPERATION
        }
    }
}

enum class ResourceAction {
    NORMAL_OPERATION,      // All modes enabled
    THROTTLE_LEARNING,     // Reduce LEARN_APP frequency
    DYNAMIC_ONLY,          // Disable LEARN_APP
    STOP_LEARNING,         // Stop all scraping
    CLEANUP_OLD_DATA       // Run storage manager
}
```

---

## 4. Android Lifecycle Integration

### Current Service Lifecycle (✅ MOSTLY CORRECT)

**VoiceOSService Lifecycle:**
```kotlin
class VoiceOSService : AccessibilityService() {

    // ✅ CORRECT: Called once when service bound
    override fun onServiceConnected() {
        // Initialize components
        scrapingIntegration = AccessibilityScrapingIntegration(this, this)

        // Configure service
        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    // ✅ CORRECT: Event-driven
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        scrapingIntegration.onAccessibilityEvent(event)
    }

    // ⚠️ MISSING: Cleanup on service destroyed
    override fun onDestroy() {
        super.onDestroy()
        // TODO: Cancel coroutines, close databases
    }
}
```

### Database Lifecycle Issues

**Problem:** Two databases with different lifecycles
```kotlin
// VoiceOSCore: AppScrapingDatabase (DYNAMIC mode)
class AccessibilityScrapingIntegration {
    private val database = AppScrapingDatabase.getInstance(context)
    // ⚠️ When is this closed?
}

// LearnApp: LearnAppDatabase (LEARN_APP mode)
class LearnAppRepository {
    private val database = LearnAppDatabase.getInstance(context)
    // ⚠️ When is this closed?
}
```

**Issue:** Room singleton pattern keeps databases open indefinitely
- No explicit `close()` calls
- Relies on process termination
- May hold locks during service restart

### ADR-016: Database Lifecycle Management

**Recommendation 1: Consolidate Databases** (HIGH PRIORITY)

VoiceOSAppDatabase already exists (created 2025-10-30) but not fully utilized:
```kotlin
@Database(
    entities = [
        // Unified entities
        AppEntity::class,
        ScreenEntity::class,
        ExplorationSessionEntity::class,

        // Legacy scraping entities (TODO: merge)
        ScrapedAppEntity::class,
        ScrapedElementEntity::class,
        // ... other entities
    ],
    version = 1
)
abstract class VoiceOSAppDatabase : RoomDatabase()
```

**Migration Plan:**
1. **Phase 3A:** Move LearnApp data to VoiceOSAppDatabase
2. **Phase 3B:** Merge duplicate entities (AppEntity ← ScrapedAppEntity + LearnedAppEntity)
3. **Phase 3C:** Deprecate LearnAppDatabase

**Benefits:**
- Single database lifecycle
- Atomic transactions across modes
- No synchronization issues
- Unified queries
- Smaller memory footprint

**Recommendation 2: Explicit Lifecycle Management**

```kotlin
class VoiceOSService : AccessibilityService() {

    private var database: VoiceOSAppDatabase? = null
    private var scrapingScope: CoroutineScope? = null

    override fun onServiceConnected() {
        // Initialize database
        database = VoiceOSAppDatabase.getInstance(applicationContext)

        // Create coroutine scope
        scrapingScope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

        // Initialize scraping
        scrapingIntegration = AccessibilityScrapingIntegration(
            context = this,
            accessibilityService = this,
            database = database!!,
            scope = scrapingScope!!
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancel all coroutines
        scrapingScope?.cancel()
        scrapingScope = null

        // Close database (if last reference)
        database?.close()
        database = null

        Log.i(TAG, "Service destroyed, resources cleaned up")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Database and coroutines persist through config changes
        Log.d(TAG, "Configuration changed, keeping resources alive")
    }
}
```

**Room WAL Configuration:**
```kotlin
companion object {
    fun getInstance(context: Context): VoiceOSAppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                VoiceOSAppDatabase::class.java,
                DATABASE_NAME
            )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // ← Add this
            .build()

            INSTANCE = instance
            instance
        }
    }
}
```

**WAL Benefits:**
- Concurrent reads/writes (DYNAMIC + LEARN_APP can run simultaneously)
- Better performance (30-50% faster writes)
- Less disk I/O
- Automatic checkpointing

---

## 5. Security & Permissions

### Current Permission State

**Declared Permissions (inferred from code):**
```xml
<!-- VoiceOSCore/src/main/AndroidManifest.xml -->
<manifest>
    <!-- Implicit: BIND_ACCESSIBILITY_SERVICE (system grants when user enables) -->
    <!-- ⚠️ MISSING: QUERY_ALL_PACKAGES (required for Android 11+) -->
</manifest>
```

**Required Permissions:**
1. ✅ **BIND_ACCESSIBILITY_SERVICE** - Granted when user enables service
2. ❌ **QUERY_ALL_PACKAGES** - NOT declared (Android 11+ requirement)
3. ⚠️ **FOREGROUND_SERVICE** - Not declared (needed for mic access)
4. ⚠️ **FOREGROUND_SERVICE_MICROPHONE** - Not declared (Android 14+ requirement)

### ADR-017: Permission Hardening

**Required Updates to AndroidManifest.xml:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Android 11+ package visibility (for LauncherDetector) -->
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />

    <!-- Foreground service for background mic access -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <!-- Android 14+ foreground service type -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

    <!-- Accessibility service binding (system-level permission) -->
    <permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"
        android:protectionLevel="signature" />

    <application>
        <service
            android:name=".accessibility.VoiceOSService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true"
            android:foregroundServiceType="microphone">

            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>

            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>
</manifest>
```

**Play Store Justification (required for QUERY_ALL_PACKAGES):**

```
Justification for QUERY_ALL_PACKAGES:

VoiceOS requires QUERY_ALL_PACKAGES to detect launcher applications across
all Android devices (Samsung, Pixel, RealWear, XR devices). This is used
in the LauncherDetector component to:

1. Identify launcher apps via queryIntentActivities(HOME intent)
2. Exclude launcher UI elements from voice command generation
3. Prevent "launcher contamination" bug in accessibility scraping

This permission is essential for the accessibility service to function
correctly across diverse Android devices with different launcher implementations.

Code location: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/detection/LauncherDetector.kt
```

### Data Security Assessment

**Scraped Data Sensitivity:** ⚠️ MEDIUM-HIGH RISK

**Data Types Collected:**
```kotlin
@Entity(tableName = "scraped_elements")
data class ScrapedElementEntity(
    val text: String?,              // ⚠️ May contain PII (names, emails)
    val contentDescription: String?, // ⚠️ May contain sensitive info
    val resourceId: String?,         // ✅ Low risk (com.app:id/button)
    val className: String?,          // ✅ Low risk (android.widget.Button)
    val viewIdResourceName: String?, // ✅ Low risk
    val bounds: String?,             // ✅ Low risk (coordinates)
    val isClickable: Boolean,        // ✅ Low risk
    val packageName: String          // ✅ Low risk
)
```

**PII Risk Examples:**
- Text fields: "John Doe", "john@example.com"
- Content descriptions: "Profile picture of John Doe"
- Navigation labels: "My Account", "Payment Methods"

**Recommended Mitigations:**

1. **PII Detection & Redaction:**
   ```kotlin
   class PIIRedactor {
       private val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
       private val phonePattern = Regex("\\d{3}-\\d{3}-\\d{4}")

       fun redact(text: String?): String? {
           if (text == null) return null

           return text
               .replace(emailPattern, "[EMAIL]")
               .replace(phonePattern, "[PHONE]")
               // Add more patterns as needed
       }
   }
   ```

2. **User Consent UI:**
   ```kotlin
   class ConsentDialogManager {
       fun showDataCollectionConsent(service: AccessibilityService) {
           // Show overlay dialog explaining:
           // - What data is collected (UI structure, text, labels)
           // - Why it's needed (voice command generation)
           // - How it's stored (local device only, not uploaded)
           // - How to disable (Settings > Accessibility > VoiceOS > Disable)
       }
   }
   ```

3. **Encryption at Rest:**
   ```kotlin
   // Add to VoiceOSAppDatabase
   companion object {
       fun getInstance(context: Context): VoiceOSAppDatabase {
           return INSTANCE ?: synchronized(this) {
               val passphrase = getOrCreateDatabasePassphrase(context)
               val factory = SupportFactory(passphrase)

               val instance = Room.databaseBuilder(...)
                   .openHelperFactory(factory) // ← SQLCipher encryption
                   .build()

               INSTANCE = instance
               instance
           }
       }
   }
   ```

4. **Data Retention Policy:**
   ```kotlin
   object DataRetentionPolicy {
       const val SCRAPED_ELEMENTS_TTL_DAYS = 30
       const val SESSIONS_TTL_DAYS = 90
       const val USER_INTERACTIONS_TTL_DAYS = 60

       suspend fun enforceRetention(database: VoiceOSAppDatabase) {
           val cutoffDate = System.currentTimeMillis() - (TTL_DAYS * 24 * 60 * 60 * 1000)
           database.scrapedElementDao().deleteOlderThan(cutoffDate)
           database.explorationSessionDao().deleteOlderThan(cutoffDate)
       }
   }
   ```

### Attack Surface Analysis

**Binder Attack Surface:** ✅ LOW RISK
- AccessibilityService uses system Binder (protected by signature permission)
- No custom AIDL interfaces exposed
- No exported components (except AccessibilityService with system permission)

**Intent Filtering:** ✅ SECURE
```xml
<!-- VoiceOSService only responds to system intents -->
<intent-filter>
    <action android:name="android.accessibilityservice.AccessibilityService" />
</intent-filter>
```

**Exported Components:** ✅ MINIMAL EXPOSURE
- Only AccessibilityService exported (required)
- Protected by BIND_ACCESSIBILITY_SERVICE (signature permission)
- No other exported activities, services, or receivers

**Recommendations:**
1. ✅ Keep minimal exported surface
2. ⚠️ Add explicit `android:exported="false"` to all non-accessibility components
3. ✅ Continue using signature-level permissions

---

## 6. Android Version Compatibility

### Current Support Matrix

**Build Configuration:**
```kotlin
// modules/apps/VoiceOSCore/build.gradle.kts
android {
    compileSdk = 34        // Android 14 (API 34)
    defaultConfig {
        minSdk = 29        // Android 10 (API 29) ✅
        targetSdk = 34     // Android 14 (API 34) ✅
    }
}

// modules/apps/LearnApp/build.gradle.kts
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 29        // ✅ Consistent
        targetSdk = 34     // ✅ Consistent
    }
}
```

**API Level Support:** API 29 - API 34 (Android 10 - 14)

### Version-Specific Features Used

**API 21+ (Android 5.0 Lollipop):**
```kotlin
// ✅ FLAG_RETRIEVE_INTERACTIVE_WINDOWS (used in Phase 1)
serviceInfo.flags = flags or
    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
```

**API 26+ (Android 8.0 Oreo):**
```kotlin
// ✅ TYPE_APPLICATION_OVERLAY (used in WindowManagerTest)
// Replaced with hex constant for compatibility:
type = 0x00000004 /* AccessibilityWindowInfo.TYPE_APPLICATION_OVERLAY (API 26+) */
```

**API 29+ (Android 10 Q) - Current Min SDK:**
- ✅ Scoped Storage (automatically handled by Room)
- ✅ Background location restrictions (not applicable)
- ✅ Package visibility restrictions (need QUERY_ALL_PACKAGES)

**API 30+ (Android 11 R):**
```kotlin
// ⚠️ Package visibility restrictions (requires QUERY_ALL_PACKAGES)
// Used in LauncherDetector.kt
val launchers = packageManager.queryIntentActivities(homeIntent, flags)
// ⚠️ Will return empty list without QUERY_ALL_PACKAGES permission
```

**API 31+ (Android 12 S):**
- ⚠️ Foreground service restrictions (need foregroundServiceType)
- ⚠️ PendingIntent mutability (use FLAG_IMMUTABLE)

**API 33+ (Android 13 Tiramisu):**
- ⚠️ Notification permission (need POST_NOTIFICATIONS)
- ⚠️ Runtime notification permission prompt

**API 34+ (Android 14 Upside Down Cake):**
- ⚠️ Foreground service type MICROPHONE (need FOREGROUND_SERVICE_MICROPHONE)

### ADR-018: Android Version Compatibility Strategy

**Compatibility Matrix:**

| Feature | API 29-30 | API 31-32 | API 33 | API 34+ | Fallback |
|---------|-----------|-----------|--------|---------|----------|
| Multi-window detection | ✅ FLAG_RETRIEVE_INTERACTIVE_WINDOWS | ✅ | ✅ | ✅ | None needed |
| Launcher detection | ⚠️ Limited (no QUERY_ALL_PACKAGES) | ✅ | ✅ | ✅ | Fallback to hardcoded list |
| Foreground service | ✅ | ⚠️ Need type | ⚠️ Need permission | ⚠️ Need MICROPHONE type | None |
| Notifications | ✅ | ✅ | ⚠️ Need permission | ⚠️ Need permission | Silent operation |
| Database encryption | ✅ SQLCipher | ✅ | ✅ | ✅ | None needed |

**Version-Specific Code Pattern:**

```kotlin
class CompatibilityHelper {

    fun setupForegroundService(service: Service, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            service.startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE // API 31+
            )
        } else {
            service.startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true // Always granted on API <33
    }

    fun queryLaunchers(packageManager: PackageManager): List<ResolveInfo> {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
            // Need QUERY_ALL_PACKAGES
            packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            // API 29: No flags needed
            packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        }
    }
}
```

**Graceful Degradation Plan:**

| Feature | Degraded Behavior |
|---------|-------------------|
| **Launcher Detection (no QUERY_ALL_PACKAGES)** | Fall back to hardcoded launcher list + log warning |
| **Notifications (no POST_NOTIFICATIONS on API 33+)** | Silent operation, show in-app message |
| **Foreground Service (restricted)** | Defer mic access until user interaction |

**Example Graceful Degradation:**
```kotlin
class LauncherDetector(private val context: Context) {

    fun getLaunchers(): List<String> {
        return try {
            // Try PackageManager query (requires QUERY_ALL_PACKAGES on API 30+)
            queryLaunchersViaPackageManager()
        } catch (e: SecurityException) {
            Log.w(TAG, "QUERY_ALL_PACKAGES not granted, using fallback", e)
            // Fall back to known launchers
            KNOWN_LAUNCHERS.also {
                showPermissionRequiredNotification()
            }
        }
    }

    companion object {
        private val KNOWN_LAUNCHERS = setOf(
            "com.google.android.apps.nexuslauncher", // Pixel
            "com.android.launcher3",                  // AOSP
            "com.sec.android.app.launcher",           // Samsung
            "com.realwear.wearhf"                     // RealWear
        )
    }
}
```

**Testing Matrix:**

| Device | API Level | Test Focus |
|--------|-----------|------------|
| **Pixel 4** | 29 (Q) | Min SDK baseline, no API 30+ features |
| **Pixel 5** | 31 (S) | Foreground service restrictions |
| **Pixel 6** | 33 (T) | Notification permission |
| **Pixel 7** | 34 (U) | FOREGROUND_SERVICE_MICROPHONE |
| **RealWear Navigator 520** | 29-31 | XR device, production target |

---

## 7. System Integration Patterns

### Event Delivery Pattern (✅ CURRENT - CORRECT)

**Pattern:** Observer Pattern via AccessibilityEvent

```kotlin
// System → VoiceOS (push model, event-driven)
class VoiceOSService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Event pushed by Android framework
        when (event.eventType) {
            TYPE_WINDOW_STATE_CHANGED -> handleWindowChange(event)
            TYPE_VIEW_CLICKED -> handleClick(event)
        }
    }
}
```

**Benefits:**
- ✅ Zero polling (battery efficient)
- ✅ Real-time (0-50ms latency)
- ✅ System-driven (no app lifecycle coupling)

**Alternative Pattern (❌ ANTI-PATTERN):**
```kotlin
// ❌ DO NOT DO THIS (polling model)
scope.launch {
    while (isActive) {
        val windows = accessibilityService.windows
        processWindows(windows)
        delay(100) // Polling every 100ms = battery drain
    }
}
```

### Data Sharing Pattern: Repository Pattern (✅ CURRENT - GOOD)

**Pattern:** Repository as data access abstraction

```kotlin
// LearnApp uses Repository pattern
class LearnAppRepository(private val dao: LearnAppDao) {
    suspend fun getApp(packageName: String): LearnedAppEntity? {
        return dao.getAppByPackageName(packageName)
    }
}

// VoiceOSCore uses DAO directly (simpler, acceptable)
class AccessibilityScrapingIntegration {
    private val database = AppScrapingDatabase.getInstance(context)

    suspend fun scrapeApp(packageName: String) {
        database.scrapedAppDao().insertApp(scrapedApp)
    }
}
```

**Recommendation:** ✅ Keep current pattern
- Repository pattern for LearnApp (more complex logic)
- Direct DAO access for VoiceOSCore (simpler scraping)

**Alternative: ContentProvider** (❌ NOT RECOMMENDED)
```kotlin
// ❌ Overkill for single-process app
class VoiceOSContentProvider : ContentProvider() {
    override fun query(...): Cursor? {
        // Adds IPC overhead, not needed
    }
}
```

### Configuration Management (⚠️ NEEDS IMPROVEMENT)

**Current Pattern:** SharedPreferences (scattered)

```kotlin
// AccessibilityScrapingIntegration.kt
private val preferences = context.getSharedPreferences(
    "voiceos_interaction_learning",
    Context.MODE_PRIVATE
)

// (Other components likely use different SharedPreferences keys)
```

**Problem:** No centralized configuration
- Multiple SharedPreferences files
- No type safety
- No validation
- No defaults

**Recommended Pattern:** Centralized Configuration Manager

```kotlin
object VoiceOSConfig {
    private const val PREF_NAME = "voiceos_config"

    // Feature flags
    var interactionLearningEnabled: Boolean by BooleanPref(
        key = "interaction_learning_enabled",
        default = true
    )

    var dynamicScrapingEnabled: Boolean by BooleanPref(
        key = "dynamic_scraping_enabled",
        default = true
    )

    // Resource limits
    var minBatteryLevelForLearning: Int by IntPref(
        key = "min_battery_level_learning",
        default = 20,
        range = 0..100
    )

    var maxDatabaseSizeMB: Int by IntPref(
        key = "max_database_size_mb",
        default = 50,
        range = 10..500
    )

    // Performance tuning
    var scrapingMaxDepth: Int by IntPref(
        key = "scraping_max_depth",
        default = 50,
        range = 10..100
    )

    // Feature flags for rollout
    var phase3DatabaseMergeEnabled: Boolean by BooleanPref(
        key = "phase3_database_merge_enabled",
        default = false // Disable by default, enable via remote config
    )
}

// Usage:
if (VoiceOSConfig.interactionLearningEnabled) {
    trackInteraction(event)
}
```

**Benefits:**
- Type-safe configuration
- Centralized defaults
- Easy feature flag rollout
- Firebase Remote Config integration ready

---

## 8. Production Deployment Concerns

### ADR-019: Rollout Strategy

**Phased Rollout Plan:**

**Phase 3A: Internal Testing (Week 1)**
- Deploy to internal test devices (2-3 devices)
- Test scenarios:
  - DYNAMIC scraping (existing functionality)
  - LEARN_APP mode (new launcher detection)
  - Mode switching (DYNAMIC ↔ LEARN_APP)
  - Database writes from both modes
- Success criteria: 0 crashes, <15MB memory, <2% battery/hour

**Phase 3B: Alpha Release (Week 2)**
- Deploy to 5-10 alpha testers
- Enable Phase 3 via feature flag:
  ```kotlin
  VoiceOSConfig.phase3DatabaseMergeEnabled = true
  ```
- Monitor:
  - Crash rate (target <0.1%)
  - ANR rate (target <0.05%)
  - Memory usage (target <20MB)
  - Battery drain (target <3%/hour)
- Rollback plan: Disable feature flag

**Phase 3C: Beta Release (Week 3-4)**
- Deploy to 50-100 beta testers
- Enable for 50% of beta users (A/B test)
- Monitor:
  - Launcher detection accuracy (target >95%)
  - Scraping errors (target <1%)
  - Database size growth (target <2MB/day)
- Rollback plan: Remote config disable

**Phase 3D: Production Release (Week 5+)**
- Gradual rollout: 5% → 25% → 50% → 100%
- Staged over 2-4 weeks
- Monitor:
  - Play Store crash rate (target <0.1%)
  - ANR rate (target <0.05%)
  - Uninstall rate (target <5%)
  - 1-star reviews (investigate all)

### Feature Flag Implementation

**Firebase Remote Config:**
```kotlin
class FeatureFlagManager(private val context: Context) {

    private val remoteConfig = Firebase.remoteConfig

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 // 1 hour
            }
        )

        remoteConfig.setDefaultsAsync(
            mapOf(
                "phase3_enabled" to false,
                "phase3_database_merge" to false,
                "learn_app_mode_enabled" to true,
                "dynamic_mode_enabled" to true,
                "min_battery_level" to 20
            )
        )
    }

    suspend fun fetchAndActivate() {
        remoteConfig.fetchAndActivate().await()
        applyFeatureFlags()
    }

    private fun applyFeatureFlags() {
        VoiceOSConfig.phase3DatabaseMergeEnabled =
            remoteConfig.getBoolean("phase3_database_merge")

        VoiceOSConfig.interactionLearningEnabled =
            remoteConfig.getBoolean("learn_app_mode_enabled")

        VoiceOSConfig.minBatteryLevelForLearning =
            remoteConfig.getLong("min_battery_level").toInt()
    }

    fun isPhase3Enabled(): Boolean {
        return remoteConfig.getBoolean("phase3_enabled")
    }
}
```

**Usage in Code:**
```kotlin
class AccessibilityScrapingIntegration {

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!featureFlagManager.isPhase3Enabled()) {
            // Use legacy scraping path
            legacyScrapeCurrentWindow(event)
            return
        }

        // Use Phase 3 unified scraping
        unifiedScrapeCurrentWindow(event)
    }
}
```

### Monitoring Dashboard

**Required Metrics (Firebase Analytics + Crashlytics):**

```kotlin
object VoiceOSAnalytics {

    // Performance metrics
    fun logScrapingPerformance(
        mode: ScrapingMode,
        durationMs: Long,
        elementsFound: Int,
        success: Boolean
    ) {
        Firebase.analytics.logEvent("scraping_performance") {
            param("mode", mode.name)
            param("duration_ms", durationMs)
            param("elements_found", elementsFound)
            param("success", success)
        }
    }

    // Resource metrics
    fun logResourceUsage(
        memoryMB: Long,
        batteryLevel: Int,
        databaseSizeMB: Long
    ) {
        Firebase.analytics.logEvent("resource_usage") {
            param("memory_mb", memoryMB)
            param("battery_level", batteryLevel)
            param("database_mb", databaseSizeMB)
        }
    }

    // Error metrics
    fun logScrapingError(
        mode: ScrapingMode,
        errorType: String,
        errorMessage: String
    ) {
        Firebase.crashlytics.recordException(
            ScrapingException(mode, errorType, errorMessage)
        )

        Firebase.analytics.logEvent("scraping_error") {
            param("mode", mode.name)
            param("error_type", errorType)
        }
    }

    // Feature usage
    fun logModeSwitch(
        fromMode: ScrapingMode,
        toMode: ScrapingMode,
        reason: String
    ) {
        Firebase.analytics.logEvent("mode_switch") {
            param("from_mode", fromMode.name)
            param("to_mode", toMode.name)
            param("reason", reason)
        }
    }
}
```

**Monitoring Dashboard (Firebase Console):**

| Metric | Alert Threshold | Action |
|--------|----------------|--------|
| Crash rate | >0.1% | Immediate investigation |
| ANR rate | >0.05% | Review coroutine usage |
| Memory usage (P95) | >50MB | Enable memory profiling |
| Battery drain (avg) | >5%/hour | Review scraping frequency |
| Database size (P95) | >100MB | Enforce cleanup policy |
| Scraping error rate | >5% | Review launcher detection |
| Mode switch failures | >1% | Review state machine |

### Rollback Plan

**Rollback Trigger Conditions:**
1. Crash rate >0.5% (5x normal)
2. ANR rate >0.2% (4x normal)
3. Memory >100MB (2x target)
4. Battery drain >10%/hour (2x target)
5. Critical bugs reported by >5% of users

**Rollback Procedure:**

**Method 1: Feature Flag Kill Switch (Fastest - 5 minutes)**
```kotlin
// Firebase Remote Config
phase3_enabled = false
phase3_database_merge = false
```
- Propagates to devices within 1 hour (fetch interval)
- App continues using legacy scraping
- No app update required
- Users retain data

**Method 2: Emergency App Update (Medium - 24 hours)**
```kotlin
// Remove Phase 3 code paths
class AccessibilityScrapingIntegration {
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Revert to Phase 1 implementation
        legacyScrapeCurrentWindow(event)
    }
}
```
- Publish emergency update to Play Store
- Force update via Firebase Remote Config
- Requires user to update app
- Clean rollback, no code duplication

**Method 3: Database Rollback (Slowest - 48-72 hours)**
```kotlin
// Migrate data back to separate databases
class DatabaseRollbackMigration : Migration(2, 1) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Export VoiceOSAppDatabase data
        // Recreate LearnAppDatabase
        // Migrate data back
    }
}
```
- Only needed if database merge causes corruption
- Requires app update
- Data loss risk (backup first)

**Monitoring Post-Rollback:**
- Verify metrics return to baseline
- Monitor user feedback (Play Store reviews)
- Investigate root cause
- Document lessons learned

### Incident Response Plan

**Severity Levels:**

**P0 (Critical - Service Down):**
- Crash rate >5%
- ANR rate >1%
- App unusable
- Response: Immediate rollback (Method 1), emergency on-call

**P1 (High - Major Degradation):**
- Crash rate 0.5-5%
- Memory >100MB
- Battery drain >10%/hour
- Response: Rollback within 4 hours, root cause analysis

**P2 (Medium - Minor Issues):**
- Crash rate 0.1-0.5%
- Scraping errors >5%
- Response: Investigate, hotfix within 48 hours

**P3 (Low - Cosmetic):**
- Non-critical bugs
- Performance not meeting goals but acceptable
- Response: Fix in next release

**Incident Template:**
```markdown
## Incident Report: [Title]

**Date:** YYYY-MM-DD HH:MM
**Severity:** P0/P1/P2/P3
**Status:** INVESTIGATING / ROLLBACK INITIATED / RESOLVED

### Timeline
- HH:MM - Issue detected (metric X exceeded threshold Y)
- HH:MM - Investigation started
- HH:MM - Root cause identified
- HH:MM - Rollback initiated (Method 1/2/3)
- HH:MM - Rollback confirmed, metrics normalized

### Root Cause
[Technical explanation]

### Impact
- Affected users: X%
- Duration: X hours
- Data loss: Yes/No

### Resolution
- Rollback method: Feature flag / App update / Database migration
- Code changes: [Link to commit]
- Monitoring: [Link to dashboard]

### Lessons Learned
1. [What went wrong]
2. [What worked well]
3. [How to prevent in future]

### Action Items
- [ ] Fix root cause (Owner, Due Date)
- [ ] Add monitoring (Owner, Due Date)
- [ ] Update tests (Owner, Due Date)
```

---

## 9. Resource Budget Summary

### Memory Budget

**Target Breakdown:**
```
VoiceOSService Process (Target: <50MB):
├── Baseline (Android framework)      ~8-12MB
├── Room Databases                      ~3-5MB
│   ├── VoiceOSAppDatabase (unified)    ~2-3MB
│   └── LearnAppDatabase (deprecated)   ~1-2MB ← Remove in Phase 3
├── Coroutines & Thread Pools           ~1-2MB
├── PackageManager caches               ~1-2MB
├── AccessibilityNodeInfo cache         ~2-5MB
├── Scraping buffers                    ~1-3MB
└── Misc (Hilt, Gson, etc.)             ~2-5MB
───────────────────────────────────────────────
Total (Current):                        ~20-36MB
Total (After Phase 3 consolidation):   ~18-31MB
```

**Action Thresholds:**
- **<20MB:** ✅ Excellent (current target)
- **20-50MB:** ✅ Good (acceptable)
- **50-100MB:** ⚠️ Warning (enable monitoring)
- **>100MB:** 🔴 Critical (stop learning, cleanup)

### CPU Budget

**Target Breakdown:**
```
Idle State (<2% CPU):
├── AccessibilityEvent listening         0-0.1%
├── Coroutine overhead                   0.1-0.3%
└── Background maintenance               0-0.2%

Active State (Window change, <10% CPU):
├── Event processing                     0.5-1%
├── Node traversal                       2-5%
├── Database writes                      1-2%
└── UUID generation                      0.5-1%

LEARN_APP Mode (<20% CPU):
├── Deep UI traversal                    10-15%
├── Hash computation                     2-3%
├── Database batch writes                2-3%
└── ExplorationEngine logic              1-2%
```

**Action Thresholds:**
- **<5% idle:** ✅ Excellent
- **5-10% idle:** ⚠️ Warning (investigate)
- **>10% idle:** 🔴 Critical (hot path issue)

### Battery Budget

**Target:** <3% battery drain per hour (screen off, idle)

**Breakdown:**
```
Hourly Battery Consumption:
├── AccessibilityService binding        0.1-0.2%
├── Event processing (0-10 Hz)          0.3-0.5%
├── Database writes (batched)           0.1-0.2%
├── Wake locks (none expected)          0%
└── Network (none)                      0%
───────────────────────────────────────────────
Total (DYNAMIC mode):                   ~0.5-0.9% per hour ✅
Total (LEARN_APP active):               ~5-10% per hour ⚠️ (acceptable, user-initiated)
```

**Action Thresholds:**
- **<2%/hour:** ✅ Excellent
- **2-5%/hour:** ✅ Good (target met)
- **5-10%/hour:** ⚠️ Warning (LEARN_APP mode only)
- **>10%/hour:** 🔴 Critical (wake lock issue?)

### Storage Budget

**Database Size:**
```
VoiceOSAppDatabase (Target: <50MB):
├── AppEntity (~50 apps × 1KB)              ~50KB
├── ScreenEntity (~500 screens × 2KB)       ~1MB
├── ScrapedElementEntity (~10K elements × 1KB) ~10MB
├── ScrapedHierarchyEntity (~5K × 500B)     ~2.5MB
├── GeneratedCommandEntity (~2K × 500B)     ~1MB
├── UserInteractionEntity (~5K × 300B)      ~1.5MB
├── ExplorationSessionEntity (~100 × 2KB)   ~200KB
└── Indices + WAL overhead                  ~5MB
──────────────────────────────────────────────────
Total (Typical):                            ~21MB ✅
Total (Heavy user):                         ~45MB ✅
```

**Action Thresholds:**
- **<30MB:** ✅ Excellent
- **30-50MB:** ✅ Good (within budget)
- **50-100MB:** ⚠️ Warning (cleanup old data)
- **>100MB:** 🔴 Critical (vacuum + prune)

---

## 10. System Integration ADRs (Summary)

### ADR-013: System Service Integration
- **Decision:** Use AccessibilityService as primary integration point
- **Rationale:** Event-driven, battery efficient, minimal permissions
- **Status:** ✅ Implemented (Phase 1)

### ADR-014: In-Process Scraping
- **Decision:** Keep in-process architecture
- **Rationale:** Lowest latency, simplest lifecycle, no Binder limits
- **Status:** ✅ Implemented (current)

### ADR-015: Resource Budget
- **Decision:** Enforce explicit resource budgets
- **Rationale:** Prevent resource exhaustion, enable monitoring
- **Status:** ⚠️ Needs implementation (Phase 3)

### ADR-016: Database Lifecycle
- **Decision:** Consolidate to VoiceOSAppDatabase + WAL mode
- **Rationale:** Single lifecycle, atomic transactions, concurrent R/W
- **Status:** ⚠️ Partially implemented (VoiceOSAppDatabase exists, consolidation pending)

### ADR-017: Permission Hardening
- **Decision:** Add QUERY_ALL_PACKAGES + foreground service permissions
- **Rationale:** Android 11+ compatibility, Play Store compliance
- **Status:** ❌ Not implemented (needs manifest updates)

### ADR-018: Android Version Compatibility
- **Decision:** Support API 29-34 with graceful degradation
- **Rationale:** Cover 95%+ of target devices
- **Status:** ✅ Mostly implemented (need graceful degradation helpers)

### ADR-019: Rollout Strategy
- **Decision:** Phased rollout with feature flags
- **Rationale:** Safe deployment, fast rollback capability
- **Status:** ❌ Not implemented (needs Firebase setup)

---

## 11. Compatibility Matrix

### Device Support Matrix

| Device Category | Android Version | Support Level | Notes |
|----------------|----------------|---------------|-------|
| **Pixel (Google)** | API 29-34 | ✅ Primary | Reference implementation |
| **Samsung Galaxy** | API 29-34 | ✅ Primary | OneUI compatibility |
| **RealWear Navigator** | API 29-31 | ✅ Primary | XR target device |
| **Generic Android** | API 29-34 | ✅ Full | AOSP devices |
| **Android TV** | API 29-34 | ⚠️ Limited | No touch, voice only |
| **Android Auto** | N/A | ❌ Not supported | Automotive OS |
| **Wear OS** | N/A | ❌ Not supported | Wearables |

### Feature Availability by API Level

| Feature | API 29 | API 30 | API 31 | API 32 | API 33 | API 34 |
|---------|--------|--------|--------|--------|--------|--------|
| Multi-window detection | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Launcher detection | ⚠️ Limited | ⚠️ Need permission | ✅ | ✅ | ✅ | ✅ |
| DYNAMIC scraping | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| LEARN_APP mode | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Foreground service | ✅ | ✅ | ⚠️ Need type | ✅ | ✅ | ✅ |
| Notifications | ✅ | ✅ | ✅ | ✅ | ⚠️ Need permission | ⚠️ Need permission |
| Database encryption | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Legend:**
- ✅ Full support
- ⚠️ Partial support (graceful degradation)
- ❌ Not supported

---

## 12. Operational Playbook

### Pre-Deployment Checklist

**Code Readiness:**
- [ ] All Phase 3 tests passing (LauncherDetectorTest, WindowManagerTest, etc.)
- [ ] Memory profiler run (target <50MB)
- [ ] Battery profiler run (target <5%/hour)
- [ ] ANR detection enabled (StrictMode)
- [ ] Crashlytics integrated

**Configuration:**
- [ ] Feature flags configured (Firebase Remote Config)
- [ ] Resource budgets defined (ResourceBudget.kt)
- [ ] Monitoring alerts set up (Firebase Console)
- [ ] Rollback plan documented

**Permissions:**
- [ ] AndroidManifest.xml updated (QUERY_ALL_PACKAGES, foreground service)
- [ ] Play Store justification written
- [ ] Privacy policy updated (data collection disclosure)

**Testing:**
- [ ] Tested on API 29 (min SDK)
- [ ] Tested on API 34 (target SDK)
- [ ] Tested on RealWear device (XR target)
- [ ] Tested launcher detection (5+ launcher apps)
- [ ] Tested DYNAMIC ↔ LEARN_APP mode switching

### Deployment Day Checklist

**Pre-Launch (T-1 hour):**
- [ ] Verify feature flags OFF (phase3_enabled = false)
- [ ] Verify monitoring dashboard accessible
- [ ] On-call engineer assigned
- [ ] Rollback procedure reviewed

**Launch (T=0):**
- [ ] Upload APK to Play Store (production track, 5% rollout)
- [ ] Verify APK live in Play Store
- [ ] Test download on clean device
- [ ] Enable feature flag for 5% of users (phase3_enabled = true)

**Post-Launch (T+1 hour):**
- [ ] Monitor crash rate (target <0.1%)
- [ ] Monitor ANR rate (target <0.05%)
- [ ] Monitor memory (Firebase Performance)
- [ ] Check Play Store reviews (respond to issues)

**Day 1-3:**
- [ ] Daily monitoring check (morning + evening)
- [ ] Increase rollout to 25% if metrics good
- [ ] Respond to user feedback within 24 hours

**Week 1:**
- [ ] Increase rollout to 50%
- [ ] Weekly metrics review meeting
- [ ] Adjust monitoring alerts if needed

**Week 2-4:**
- [ ] Increase rollout to 100%
- [ ] Continue monitoring
- [ ] Document lessons learned

### Monitoring Dashboard (Firebase Console)

**Real-Time Metrics (Refresh: 1 minute):**
```
┌─────────────────────────────────────────────┐
│ VoiceOS Phase 3 - Live Dashboard           │
├─────────────────────────────────────────────┤
│ Crash Rate:        0.05% ✅ (<0.1% target)  │
│ ANR Rate:          0.02% ✅ (<0.05% target) │
│ Memory (P95):      32MB ✅ (<50MB target)   │
│ Battery (avg):     2.1%/hr ✅ (<5% target)  │
│ DB Size (P95):     28MB ✅ (<50MB target)   │
│ Scraping Errors:   1.2% ✅ (<5% target)     │
│                                             │
│ Phase 3 Rollout:   25% (increasing to 50%) │
│ Active Users:      1,234                    │
│ Sessions/day:      4,567                    │
└─────────────────────────────────────────────┘
```

**Alert Configuration (Firebase Alerts):**
| Alert | Condition | Notification | Action |
|-------|-----------|--------------|--------|
| High Crash Rate | >0.1% | Email + Slack | Investigate immediately |
| High ANR Rate | >0.05% | Email + Slack | Review coroutines |
| Memory Pressure | >100MB (P95) | Email | Enable profiling |
| Battery Drain | >10%/hour (avg) | Email | Review wake locks |
| Scraping Errors | >10% | Email | Review launcher detection |

### Incident Response Runbook

**Scenario 1: High Crash Rate (>0.5%)**

**Symptoms:**
- Firebase Crashlytics shows crash spike
- Stack trace points to Phase 3 code
- Affects >0.5% of users

**Response:**
1. **Immediate (5 minutes):**
   - Disable feature flag: `phase3_enabled = false`
   - Verify crashes stop within 1 hour
   - Post status update to Slack

2. **Investigation (1-2 hours):**
   - Analyze crash stack traces (Firebase Crashlytics)
   - Reproduce locally if possible
   - Identify root cause

3. **Resolution (4-24 hours):**
   - If quick fix: Hotfix + emergency release
   - If complex: Keep feature flag OFF, fix in next version
   - Post-mortem: Document root cause + prevention

**Scenario 2: Memory Leak**

**Symptoms:**
- Memory usage climbs over time
- Device becomes slow/unresponsive
- Eventual OOM crash

**Response:**
1. **Immediate:**
   - Enable memory profiling on test device
   - Capture heap dump
   - Check for obvious leaks (static references, listeners)

2. **Investigation:**
   - Use Android Studio Memory Profiler
   - Look for retained AccessibilityNodeInfo objects
   - Check database connection leaks

3. **Resolution:**
   - Add explicit cleanup in onDestroy()
   - Implement weak references where appropriate
   - Add memory monitoring to detect future leaks

**Scenario 3: ANR (Application Not Responding)**

**Symptoms:**
- Firebase Vitals shows ANR spike
- Main thread blocked >5 seconds
- Users report app freezing

**Response:**
1. **Immediate:**
   - Analyze ANR traces (Firebase Crashlytics)
   - Identify blocking operation (likely on main thread)

2. **Investigation:**
   - Check for synchronous database calls on main thread
   - Check for heavy computation in onAccessibilityEvent
   - Verify coroutine dispatcher usage

3. **Resolution:**
   - Move blocking work to Dispatchers.IO
   - Add timeout to long-running operations
   - Add ANR watchdog (log warnings at 100ms latency)

---

## 13. Security Assessment

### Attack Surface Summary

**Minimal Exposure:** ✅ LOW RISK

**Exposed Components:**
1. **AccessibilityService** - System-level, signature permission
2. **Room Databases** - Local storage, no network exposure
3. **SharedPreferences** - Local storage, MODE_PRIVATE

**Not Exposed:**
- No custom AIDL interfaces
- No ContentProviders
- No exported Activities/Services (except AccessibilityService)
- No BroadcastReceivers
- No network communication

### Data Protection Audit

**Sensitive Data Collected:**

| Data Type | Sensitivity | Storage | Encryption | Retention |
|-----------|-------------|---------|------------|-----------|
| UI Element Text | HIGH (may contain PII) | Room DB | ⚠️ Recommended | 30 days |
| Content Descriptions | MEDIUM (may contain PII) | Room DB | ⚠️ Recommended | 30 days |
| Package Names | LOW | Room DB | Optional | 90 days |
| Resource IDs | LOW | Room DB | Optional | 90 days |
| Bounds (coordinates) | LOW | Room DB | Optional | 30 days |
| Interaction History | MEDIUM (usage patterns) | Room DB | ⚠️ Recommended | 60 days |

**PII Risk Examples:**
```
High Risk Text:
- "John Doe" (name in profile)
- "john@example.com" (email in form)
- "555-1234" (phone number)
- "4111 1111 1111 1111" (credit card)

Medium Risk Content Descriptions:
- "Profile picture of John Doe"
- "Message from Jane: Hello"
- "Payment method ending in 1234"
```

**Recommended Mitigations:**

1. **PII Redaction (HIGH PRIORITY):**
   ```kotlin
   class PIIRedactor {
       private val patterns = mapOf(
           "email" to Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
           "phone" to Regex("\\d{3}[-.]?\\d{3}[-.]?\\d{4}"),
           "credit_card" to Regex("\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}"),
           "ssn" to Regex("\\d{3}-\\d{2}-\\d{4}")
       )

       fun redact(text: String?): String? {
           if (text == null) return null
           var result = text
           patterns.forEach { (type, pattern) ->
               result = result.replace(pattern, "[${type.uppercase()}]")
           }
           return result
       }
   }

   // Usage:
   val redactor = PIIRedactor()
   val element = ScrapedElementEntity(
       text = redactor.redact(nodeInfo.text?.toString()),
       contentDescription = redactor.redact(nodeInfo.contentDescription?.toString()),
       // ...
   )
   ```

2. **Database Encryption (MEDIUM PRIORITY):**
   ```kotlin
   // Add SQLCipher dependency
   dependencies {
       implementation("net.zetetic:android-database-sqlcipher:4.5.4")
   }

   // Encrypt database
   companion object {
       fun getInstance(context: Context): VoiceOSAppDatabase {
           val passphrase = getOrCreatePassphrase(context)
           val factory = SupportFactory(
               SQLiteDatabase.getBytes(passphrase.toCharArray())
           )

           return Room.databaseBuilder(...)
               .openHelperFactory(factory)
               .build()
       }

       private fun getOrCreatePassphrase(context: Context): String {
           val keystore = AndroidKeyStore()
           return keystore.getOrCreateKey("voiceos_db_key")
       }
   }
   ```

3. **User Consent UI (HIGH PRIORITY):**
   ```kotlin
   class ConsentManager(private val context: Context) {

       fun showConsentDialog(service: AccessibilityService) {
           val dialog = AlertDialog.Builder(service, R.style.ConsentDialog)
               .setTitle("Data Collection Notice")
               .setMessage("""
                   VoiceOS collects UI information to generate voice commands:

                   • Button labels and text
                   • Screen layouts
                   • App navigation structure

                   This data:
                   • Stays on your device
                   • Is never uploaded
                   • Can be deleted anytime

                   Sensitive data (emails, passwords) is automatically redacted.
               """.trimIndent())
               .setPositiveButton("Accept") { _, _ ->
                   markConsentGiven()
               }
               .setNegativeButton("Decline") { _, _ ->
                   disableService()
               }
               .setNeutralButton("Privacy Policy") { _, _ ->
                   showPrivacyPolicy()
               }
               .setCancelable(false)
               .create()

           dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
           dialog.show()
       }
   }
   ```

### Privacy Policy Requirements

**Required Disclosures:**

**Data Collection:**
- What: UI element properties (text, labels, structure)
- Why: Generate voice commands for accessibility
- How: Accessibility API (AccessibilityService)
- Where: Stored locally on device
- When: Real-time during app usage + user-initiated learning

**Data Retention:**
- Scraped elements: 30 days
- Exploration sessions: 90 days
- User interactions: 60 days
- Automatic cleanup after retention period

**User Rights:**
- Right to access data (via debug export)
- Right to delete data (clear app data)
- Right to opt-out (disable accessibility service)

**Third-Party Sharing:**
- None - all data stays on device
- No analytics data sent to servers
- No crash reports contain PII (after redaction)

---

## 14. Conclusion & Recommendations

### Overall System Health: ✅ GOOD (with improvements needed)

**Strengths:**
1. ✅ Sound architecture (in-process AccessibilityService)
2. ✅ Event-driven design (battery efficient)
3. ✅ Multi-window support (Phase 1 FLAG_RETRIEVE_INTERACTIVE_WINDOWS)
4. ✅ Coroutine-based async (ANR prevention)
5. ✅ Minimal attack surface (no network, no custom IPC)

**Weaknesses:**
1. ⚠️ Two separate databases (synchronization risk)
2. ⚠️ No resource monitoring (memory, CPU, battery)
3. ⚠️ Missing permissions (QUERY_ALL_PACKAGES on API 30+)
4. ⚠️ No PII redaction (privacy risk)
5. ⚠️ No rollout infrastructure (feature flags, monitoring)

### Critical Path to Production (Priority Order)

**Phase 3A: Database Consolidation** (HIGH PRIORITY, 2-3 days)
- Migrate LearnApp data to VoiceOSAppDatabase
- Merge duplicate entities (AppEntity ← ScrapedAppEntity + LearnedAppEntity)
- Deprecate LearnAppDatabase
- Enable WAL mode for concurrent R/W
- **Risk:** MEDIUM (data migration, extensive testing needed)

**Phase 3B: Permission Hardening** (HIGH PRIORITY, 1 day)
- Add QUERY_ALL_PACKAGES to AndroidManifest.xml
- Add FOREGROUND_SERVICE_MICROPHONE (API 34+)
- Write Play Store justification
- Update privacy policy
- **Risk:** LOW (manifest changes, no code changes)

**Phase 3C: PII Redaction** (HIGH PRIORITY, 1-2 days)
- Implement PIIRedactor class
- Apply redaction to all text/contentDescription
- Add user consent dialog
- Test on real-world apps
- **Risk:** MEDIUM (regex accuracy, false positives)

**Phase 3D: Resource Monitoring** (MEDIUM PRIORITY, 2-3 days)
- Implement MemoryMonitor, CPUThrottler, StorageManager
- Add ResourceEnforcer action thresholds
- Integrate Firebase Performance Monitoring
- Add ANR watchdog
- **Risk:** LOW (monitoring only, doesn't affect functionality)

**Phase 3E: Rollout Infrastructure** (MEDIUM PRIORITY, 1-2 days)
- Set up Firebase Remote Config
- Implement FeatureFlagManager
- Configure monitoring alerts
- Write rollback runbook
- **Risk:** LOW (operational infrastructure)

### Total Estimated Effort: 7-11 days

### Go/No-Go Decision Matrix

**Criteria for Production Release:**

| Criterion | Requirement | Current Status | Phase 3 Target |
|-----------|-------------|----------------|----------------|
| **Functionality** | ||||
| DYNAMIC scraping | ✅ Works | ✅ Implemented | ✅ Keep |
| LEARN_APP mode | ✅ Works | ✅ Implemented | ✅ Enhance |
| Launcher detection | ✅ Works on 5+ launchers | ✅ Phase 1 | ✅ Keep |
| Multi-window support | ✅ FLAG_RETRIEVE_INTERACTIVE_WINDOWS | ✅ Phase 1 | ✅ Keep |
| **Performance** | ||||
| Memory idle | <20MB target | ⚠️ Unknown | ✅ Monitor + enforce |
| Memory active | <50MB target | ⚠️ Unknown | ✅ Monitor + enforce |
| CPU idle | <2% target | ⚠️ Unknown | ✅ Monitor + throttle |
| Battery drain | <5%/hour target | ⚠️ Unknown | ✅ Monitor + throttle |
| **Stability** | ||||
| Crash rate | <0.1% target | ⚠️ Unknown | ✅ Monitor + rollback |
| ANR rate | <0.05% target | ⚠️ Unknown | ✅ Monitor + rollback |
| Database integrity | No corruption | ✅ Room ACID | ✅ Keep + WAL |
| **Security** | ||||
| Permissions justified | All declared + justified | ❌ Missing QUERY_ALL_PACKAGES | ✅ Add + justify |
| PII protection | Redaction + encryption | ❌ Not implemented | ✅ Implement |
| User consent | Disclosure + opt-out | ❌ Not implemented | ✅ Implement |
| **Operations** | ||||
| Monitoring | Real-time metrics | ❌ Not implemented | ✅ Firebase setup |
| Feature flags | Remote enable/disable | ❌ Not implemented | ✅ Firebase Remote Config |
| Rollback plan | Documented + tested | ❌ Not documented | ✅ Document + test |

**GO Criteria (All Required):**
- ✅ All High Priority items complete (3A, 3B, 3C)
- ✅ Monitoring infrastructure live (Firebase)
- ✅ Rollback plan tested (feature flag disable verified)
- ✅ Privacy policy updated + published
- ✅ Internal testing complete (0 P0/P1 bugs)

**NO-GO Criteria (Any Present):**
- ❌ PII redaction not implemented (privacy risk)
- ❌ No rollback mechanism (operational risk)
- ❌ Crash rate >0.1% in internal testing
- ❌ Memory >100MB in internal testing
- ❌ Database corruption observed in testing

### Final Recommendation

**APPROVE for production with conditions:**

1. **Complete High Priority items** (3A, 3B, 3C) - ~4-6 days
2. **Phased rollout** - 5% → 25% → 50% → 100% over 4 weeks
3. **Monitor closely** - Daily checks for first week
4. **Fast rollback ready** - Feature flag kill switch tested

**Confidence Level:** HIGH (80%)
- Architecture is sound (in-process, event-driven)
- Phase 1 multi-window support solid foundation
- Main risks are operational (monitoring, rollout)
- Technical risks manageable (PII redaction, permissions)

**Reservations:**
- Two databases increase complexity (consolidation reduces risk)
- No production metrics baseline (need monitoring first)
- PII handling critical for privacy (must implement before launch)

---

## Appendix A: Code Examples

### Example 1: Resource Monitoring Integration

```kotlin
// File: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/monitoring/ResourceMonitor.kt

class ResourceMonitor(private val context: Context) {

    private val database by lazy { VoiceOSAppDatabase.getInstance(context) }
    private val metrics = MutableStateFlow(ResourceMetrics())

    fun startMonitoring(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                updateMetrics()
                delay(60_000) // Update every minute
            }
        }
    }

    private suspend fun updateMetrics() {
        val runtime = Runtime.getRuntime()
        val memoryUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val powerManager = context.getSystemService(PowerManager::class.java)

        val newMetrics = ResourceMetrics(
            memoryUsedMB = memoryUsedMB,
            memoryStatus = when {
                memoryUsedMB > 100 -> MemoryStatus.CRITICAL
                memoryUsedMB > 50 -> MemoryStatus.WARNING
                else -> MemoryStatus.NORMAL
            },
            batteryLevel = batteryLevel,
            isPowerSaveMode = powerManager.isPowerSaveMode,
            databaseSizeMB = getDatabaseSize() / 1_048_576
        )

        metrics.value = newMetrics

        // Log to Firebase
        VoiceOSAnalytics.logResourceUsage(
            memoryUsedMB,
            batteryLevel,
            newMetrics.databaseSizeMB
        )

        // Take action if needed
        enforceResourceLimits(newMetrics)
    }

    private suspend fun enforceResourceLimits(metrics: ResourceMetrics) {
        when (getRecommendedAction(metrics)) {
            ResourceAction.STOP_LEARNING -> {
                Log.w(TAG, "Critical resource pressure, stopping learning")
                // Disable LEARN_APP mode
            }
            ResourceAction.CLEANUP_OLD_DATA -> {
                Log.i(TAG, "Database too large, cleaning up")
                StorageManager(database).enforceStorageLimits()
            }
            else -> { /* No action needed */ }
        }
    }

    private fun getDatabaseSize(): Long {
        return context.getDatabasePath("voiceos_app_database").length()
    }
}
```

### Example 2: PII Redaction Pipeline

```kotlin
// File: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/privacy/PIIRedactor.kt

class PIIRedactor {

    private val patterns = mapOf(
        "EMAIL" to Regex(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            RegexOption.IGNORE_CASE
        ),
        "PHONE" to Regex(
            "\\(?\\d{3}\\)?[- .]?\\d{3}[- .]?\\d{4}"
        ),
        "CREDIT_CARD" to Regex(
            "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}"
        ),
        "SSN" to Regex(
            "\\d{3}-\\d{2}-\\d{4}"
        ),
        "URL" to Regex(
            "https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}[/\\w.-]*",
            RegexOption.IGNORE_CASE
        )
    )

    fun redact(text: String?): String? {
        if (text == null) return null
        if (text.length < 3) return text // Too short to contain PII

        var result = text
        patterns.forEach { (type, pattern) ->
            result = result.replace(pattern, "[${type}]")
        }

        return result
    }

    fun redactElement(element: ScrapedElementEntity): ScrapedElementEntity {
        return element.copy(
            text = redact(element.text),
            contentDescription = redact(element.contentDescription)
        )
    }
}

// Usage in AccessibilityScrapingIntegration:
class AccessibilityScrapingIntegration {
    private val piiRedactor = PIIRedactor()

    private suspend fun scrapeElement(node: AccessibilityNodeInfo): ScrapedElementEntity {
        val rawElement = ScrapedElementEntity(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            // ... other fields
        )

        return piiRedactor.redactElement(rawElement)
    }
}
```

### Example 3: Feature Flag Integration

```kotlin
// File: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/config/FeatureFlagManager.kt

class FeatureFlagManager(private val context: Context) {

    private val remoteConfig = Firebase.remoteConfig

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 // 1 hour
            }
        )

        remoteConfig.setDefaultsAsync(
            mapOf(
                "phase3_enabled" to false,
                "phase3_database_merge" to false,
                "pii_redaction_enabled" to true,
                "resource_monitoring_enabled" to true,
                "min_battery_level" to 20
            )
        )
    }

    suspend fun initialize() {
        try {
            remoteConfig.fetchAndActivate().await()
            Log.i(TAG, "Feature flags loaded from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch feature flags, using defaults", e)
        }
    }

    fun isPhase3Enabled(): Boolean {
        return remoteConfig.getBoolean("phase3_enabled")
    }

    fun isDatabaseMergeEnabled(): Boolean {
        return remoteConfig.getBoolean("phase3_database_merge")
    }

    fun isPIIRedactionEnabled(): Boolean {
        return remoteConfig.getBoolean("pii_redaction_enabled")
    }
}

// Usage in VoiceOSService:
class VoiceOSService : AccessibilityService() {

    private lateinit var featureFlagManager: FeatureFlagManager

    override fun onServiceConnected() {
        super.onServiceConnected()

        featureFlagManager = FeatureFlagManager(this)

        serviceScope.launch {
            featureFlagManager.initialize()
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        if (featureFlagManager.isPhase3Enabled()) {
            Log.i(TAG, "Phase 3 features enabled")
            // Use unified database
            database = VoiceOSAppDatabase.getInstance(this)
        } else {
            Log.i(TAG, "Phase 3 features disabled, using legacy path")
            // Use separate databases
        }
    }
}
```

---

## Appendix B: Testing Strategy

### Unit Testing (JUnit + Robolectric)

**Target Coverage:** >80%

**Key Test Classes:**
1. `LauncherDetectorTest.kt` (✅ 27 tests, Phase 1)
2. `WindowManagerTest.kt` (✅ 35+ tests, Phase 1)
3. `ResourceMonitorTest.kt` (NEW, ~15 tests)
4. `PIIRedactorTest.kt` (NEW, ~20 tests)
5. `FeatureFlagManagerTest.kt` (NEW, ~10 tests)

**Example Test:**
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 34]) // Test min and max SDK
class PIIRedactorTest {

    private lateinit var redactor: PIIRedactor

    @Before
    fun setup() {
        redactor = PIIRedactor()
    }

    @Test
    fun `redact email addresses`() {
        val input = "Contact us at support@example.com for help"
        val output = redactor.redact(input)

        assertThat(output).isEqualTo("Contact us at [EMAIL] for help")
        assertThat(output).doesNotContain("@")
    }

    @Test
    fun `redact phone numbers`() {
        val input = "Call 555-123-4567 or (555) 123-4567"
        val output = redactor.redact(input)

        assertThat(output).isEqualTo("Call [PHONE] or [PHONE]")
    }

    @Test
    fun `preserve non-PII text`() {
        val input = "Click the Submit button"
        val output = redactor.redact(input)

        assertThat(output).isEqualTo(input)
    }

    @Test
    fun `handle null input`() {
        val output = redactor.redact(null)
        assertThat(output).isNull()
    }
}
```

### Integration Testing (Instrumentation + Espresso)

**Target Coverage:** Critical paths only (~30%)

**Key Test Scenarios:**
1. DYNAMIC scraping on window change
2. LEARN_APP mode full exploration
3. Database writes from both modes
4. Mode switching (DYNAMIC ↔ LEARN_APP)
5. Resource monitoring triggers

**Example Test:**
```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class Phase3IntegrationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun testDynamicScrapingStoresData() {
        // Start VoiceOSService
        val service = startAccessibilityService()

        // Simulate window state change event
        val event = AccessibilityEvent.obtain(TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = "com.android.settings"
        }
        service.onAccessibilityEvent(event)

        // Wait for async scraping
        Thread.sleep(1000)

        // Verify database has data
        val database = VoiceOSAppDatabase.getInstance(targetContext)
        val app = runBlocking {
            database.scrapedAppDao().getAppByPackageName("com.android.settings")
        }

        assertThat(app).isNotNull()
        assertThat(app?.packageName).isEqualTo("com.android.settings")
    }
}
```

### Manual Testing Checklist

**Device Matrix:**
- [ ] Pixel 4 (API 29 - min SDK)
- [ ] Pixel 5 (API 31 - foreground service restrictions)
- [ ] Pixel 7 (API 34 - target SDK)
- [ ] RealWear Navigator 520 (API 29-31 - XR target)

**Test Scenarios:**
- [ ] Enable VoiceOS accessibility service
- [ ] Launch 5+ different apps (test DYNAMIC scraping)
- [ ] Trigger LEARN_APP mode (test deep exploration)
- [ ] Switch between apps (test multi-window detection)
- [ ] Check database size (should be <50MB after 1 hour)
- [ ] Check battery drain (should be <5%/hour screen off)
- [ ] Check memory (should be <50MB in Settings > Apps)
- [ ] Trigger low battery (test throttling at <20%)
- [ ] Enable battery saver (test DYNAMIC_ONLY mode)
- [ ] Test launcher detection (5+ launchers: Pixel, Samsung, Nova, etc.)
- [ ] Test PII redaction (create test app with emails/phones)
- [ ] Test feature flag (disable phase3_enabled remotely)

---

## Document Metadata

**Author:** Android OS Architecture Expert
**Date:** 2025-10-30 22:54 PDT
**Version:** 1.0.0
**Status:** FINAL - READY FOR REVIEW
**Review Status:** PENDING
**Next Review:** After user feedback

**Related Documents:**
- `/docs/Active/LearnApp-Phase1-Test-Fixes-251030-2141.md` (Phase 1 completion)
- `/docs/Active/PHASE3-ANALYSIS-INDEX.md` (Phase 3 interface analysis)
- `/docs/planning/architecture/decisions/ADR-002-Strategic-Interfaces-251009-0511.md`

**Code References:**
- `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`
- `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt`
- `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/database/VoiceOSAppDatabase.kt`
- `/modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/database/LearnAppDatabase.kt`
- `/modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/detection/LauncherDetector.kt`

---

**END OF ANALYSIS**
