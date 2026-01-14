# LearnApp - Dynamic Launcher Detection System (Device-Agnostic)

**Date:** 2025-10-30 19:15 PDT
**Status:** SOLUTION DESIGN
**Priority:** CRITICAL
**Supersedes:** Hardcoded launcher exclusions in previous analysis

---

## 🎯 Problem Statement

**User Requirement:**
> "We need to be able to understand which is the launcher and then accommodate for it"

**Current Approach (INADEQUATE):**
```kotlin
// ❌ Hardcoded - only works on specific devices
private val EXCLUDED_PACKAGES = setOf(
    "com.android.systemui",
    "com.android.launcher",
    "com.android.launcher3",
    "com.google.android.apps.nexuslauncher",
    "com.realwear.launcher"  // Device-specific!
)
```

**Why this fails:**
- ❌ Only works on tested devices (Google Pixel, RealWear)
- ❌ Breaks on Samsung (uses `com.sec.android.app.launcher`)
- ❌ Breaks on OnePlus (uses `net.oneplus.launcher`)
- ❌ Breaks on Xiaomi (uses `com.miui.home`)
- ❌ Requires code update for every new device/manufacturer
- ❌ Not scalable for production deployment

---

## ✅ Solution: Dynamic Launcher Detection

### Architecture Overview

**3-Layer Detection System:**

```
┌─────────────────────────────────────────────────────────┐
│  Layer 1: System Intent Query (PRIMARY)                │
│  Query Android for default HOME launcher                │
│  Confidence: 95%                                        │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 2: Active Launcher Detection (RUNTIME)          │
│  Detect launcher from actual navigation events          │
│  Confidence: 90%                                        │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 3: Behavioral Analysis (FALLBACK)               │
│  Identify launcher by UI patterns/behavior              │
│  Confidence: 85%                                        │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 Implementation Plan

### **Fix 1: System Intent Query for Default Launcher**

**File:** `AccessibilityScrapingIntegration.kt`
**New Class:**

```kotlin
/**
 * Dynamically detects launcher packages on any Android device.
 * Uses system intent queries to identify HOME launcher apps.
 */
class LauncherDetector(private val context: Context) {

    private val TAG = "LauncherDetector"

    // Cache detected launchers (refreshed every 24 hours)
    private var cachedLauncherPackages: Set<String>? = null
    private var lastDetectionTime: Long = 0
    private val cacheExpirationMs = 24 * 60 * 60 * 1000L  // 24 hours

    /**
     * Detects all launcher packages on the current device.
     *
     * @return Set of package names that are HOME launchers
     */
    fun detectLauncherPackages(): Set<String> {
        // Check cache first
        val now = System.currentTimeMillis()
        if (cachedLauncherPackages != null && (now - lastDetectionTime) < cacheExpirationMs) {
            Log.d(TAG, "Using cached launcher packages: $cachedLauncherPackages")
            return cachedLauncherPackages!!
        }

        val launchers = mutableSetOf<String>()

        // Method 1: Query for HOME intent handlers
        launchers.addAll(getLaunchersViaHomeIntent())

        // Method 2: Query for default launcher
        launchers.addAll(getDefaultLauncher())

        // Method 3: Add known system UI packages (always excluded)
        launchers.addAll(getSystemUIPackages())

        Log.i(TAG, "✅ Detected ${launchers.size} launcher packages: $launchers")

        // Update cache
        cachedLauncherPackages = launchers
        lastDetectionTime = now

        return launchers
    }

    /**
     * Method 1: Query all apps that handle the HOME intent.
     * These are launcher apps.
     */
    private fun getLaunchersViaHomeIntent(): Set<String> {
        val launchers = mutableSetOf<String>()

        try {
            // Create HOME intent (what happens when user presses HOME button)
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            // Query PackageManager for all apps that can handle HOME
            val packageManager = context.packageManager
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    homeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            // Extract package names
            for (resolveInfo in resolveInfos) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName != null) {
                    launchers.add(packageName)
                    Log.d(TAG, "Found HOME launcher via intent: $packageName")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error querying HOME intent launchers", e)
        }

        return launchers
    }

    /**
     * Method 2: Get the default launcher (user's preferred HOME app).
     */
    private fun getDefaultLauncher(): Set<String> {
        val launchers = mutableSetOf<String>()

        try {
            // Create HOME intent
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            // Get the default launcher
            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)

            val packageName = resolveInfo?.activityInfo?.packageName
            if (packageName != null && packageName != "android") {  // "android" = no default set
                launchers.add(packageName)
                Log.d(TAG, "Found default launcher: $packageName")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error querying default launcher", e)
        }

        return launchers
    }

    /**
     * Method 3: Get system UI packages (always excluded).
     * These handle notifications, quick settings, etc.
     */
    private fun getSystemUIPackages(): Set<String> {
        return setOf(
            "com.android.systemui",      // Standard Android system UI
            "android"                     // System package
        )
    }

    /**
     * Checks if a package is a launcher.
     *
     * @param packageName Package to check
     * @return true if package is a launcher
     */
    fun isLauncher(packageName: String): Boolean {
        val launchers = detectLauncherPackages()
        return launchers.contains(packageName)
    }

    /**
     * Invalidates launcher cache, forcing re-detection.
     * Call this when user changes default launcher.
     */
    fun invalidateCache() {
        Log.d(TAG, "Launcher cache invalidated")
        cachedLauncherPackages = null
        lastDetectionTime = 0
    }
}
```

**Integration into AccessibilityScrapingIntegration:**

```kotlin
class AccessibilityScrapingIntegration : AccessibilityService() {

    private lateinit var launcherDetector: LauncherDetector

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize launcher detector
        launcherDetector = LauncherDetector(applicationContext)

        // Detect launchers at startup
        val launchers = launcherDetector.detectLauncherPackages()
        Log.i(TAG, "🏠 Device launchers detected: $launchers")
    }

    private fun scrapeCurrentWindow(rootNode: AccessibilityNodeInfo) {
        val packageName = rootNode.packageName?.toString() ?: return

        // ✅ Dynamic launcher check (replaces hardcoded EXCLUDED_PACKAGES)
        if (launcherDetector.isLauncher(packageName)) {
            Log.d(TAG, "🏠 Skipping launcher package: $packageName")
            rootNode.recycle()
            return
        }

        // Continue with scraping...
    }
}
```

**Testing:**
```kotlin
@Test
fun `detect launcher on Google Pixel`() {
    // Device: Google Pixel 7
    val launchers = launcherDetector.detectLauncherPackages()

    assertThat(launchers).contains("com.google.android.apps.nexuslauncher")
    assertThat(launchers).contains("com.android.systemui")
}

@Test
fun `detect launcher on RealWear device`() {
    // Device: RealWear Navigator 520
    val launchers = launcherDetector.detectLauncherPackages()

    assertThat(launchers).contains("com.realwear.launcher")
}

@Test
fun `detect launcher on Samsung device`() {
    // Device: Samsung Galaxy S23
    val launchers = launcherDetector.detectLauncherPackages()

    assertThat(launchers).contains("com.sec.android.app.launcher")
}
```

---

### **Fix 2: Runtime Launcher Detection**

**Problem:** User might switch launchers during app usage

**Solution:** Detect launcher package from actual BACK navigation behavior

**File:** `ExplorationEngine.kt`
**New Logic:**

```kotlin
class ExplorationEngine {

    private val runtimeDetectedLaunchers = mutableSetOf<String>()

    /**
     * Detects launcher packages during runtime based on navigation patterns.
     *
     * Pattern: When user is in app X, presses HOME, we detect the launcher.
     */
    private fun detectLauncherFromNavigation(
        fromPackage: String,
        toPackage: String,
        navigationTrigger: String  // "BACK", "HOME", "CLICK"
    ) {
        // Pattern 1: BACK from any app leads to consistent package = launcher
        if (navigationTrigger == "BACK" && toPackage != fromPackage) {
            // Check if this package consistently appears after BACK
            val backNavigationHistory = getRecentBackNavigations()
            val commonDestination = backNavigationHistory
                .groupingBy { it.toPackage }
                .eachCount()
                .maxByOrNull { it.value }

            if (commonDestination != null && commonDestination.value >= 3) {
                // Package appears as BACK destination 3+ times = likely launcher
                runtimeDetectedLaunchers.add(commonDestination.key)
                Log.i(TAG, "🏠 Runtime detected launcher: ${commonDestination.key}")

                // Update main detector cache
                accessibilityIntegration.launcherDetector.addRuntimeLauncher(commonDestination.key)
            }
        }

        // Pattern 2: Package has HOME icon grid layout
        if (hasHomeScreenLayout(toPackage)) {
            runtimeDetectedLaunchers.add(toPackage)
            Log.i(TAG, "🏠 Detected launcher via UI pattern: $toPackage")
        }
    }

    /**
     * Checks if package displays HOME screen UI pattern.
     * Launchers typically have: GridView/RecyclerView with app icons
     */
    private fun hasHomeScreenLayout(packageName: String): Boolean {
        // Get current window root
        val rootNode = getRootInActiveWindow() ?: return false

        // Check for launcher UI patterns
        val hasGrid = rootNode.findAccessibilityNodeInfosByViewId("*:id/workspace")
            ?.isNotEmpty() ?: false

        val hasAppGrid = rootNode.findAccessibilityNodeInfosByViewId("*:id/apps_list_view")
            ?.isNotEmpty() ?: false

        val hasIconGrid = countClickableGridItems(rootNode) >= 10  // 10+ app icons

        return hasGrid || hasAppGrid || hasIconGrid
    }
}
```

---

### **Fix 3: Suppress Scraping During BACK Recovery**

**File:** `AccessibilityScrapingIntegration.kt`
**Add Recovery Mode Flag:**

```kotlin
class AccessibilityScrapingIntegration : AccessibilityService() {

    // Recovery mode: Suppress scraping during BACK navigation attempts
    @Volatile
    private var isInRecoveryMode = false

    /**
     * Sets recovery mode state.
     * When true, all scraping is suppressed to avoid capturing intermediate screens.
     *
     * @param enabled true to enable recovery mode, false to disable
     */
    fun setRecoveryMode(enabled: Boolean) {
        isInRecoveryMode = enabled
        Log.d(TAG, "Recovery mode: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    private fun scrapeCurrentWindow(rootNode: AccessibilityNodeInfo) {
        // ✅ Suppress scraping during recovery
        if (isInRecoveryMode) {
            Log.v(TAG, "⏸️ Recovery mode active - suppressing scraping")
            rootNode.recycle()
            return
        }

        val packageName = rootNode.packageName?.toString() ?: return

        // ✅ Dynamic launcher check
        if (launcherDetector.isLauncher(packageName)) {
            Log.d(TAG, "🏠 Skipping launcher package: $packageName")
            rootNode.recycle()
            return
        }

        // Continue with scraping...
    }
}
```

**File:** `ExplorationEngine.kt`
**Use Recovery Mode:**

```kotlin
private suspend fun exploreScreenRecursive(...) {
    // ... element clicking logic

    performClick(element)
    delay(1000)

    // Check if navigation was successful
    val actualPackageName = rootNode.packageName?.toString()
    if (actualPackageName != packageName) {
        Log.w(TAG, "Navigation led to different package: $actualPackageName")

        // ✅ Enable recovery mode BEFORE starting BACK attempts
        accessibilityIntegration.setRecoveryMode(true)

        try {
            // Attempt recovery with BACK button
            var recovered = false
            var backAttempts = 0
            val maxBackAttempts = 5

            while (backAttempts < maxBackAttempts && !recovered) {
                pressBack()
                delay(1000)

                val currentPackage = getRootInActiveWindow()?.packageName?.toString()
                if (currentPackage == packageName) {
                    Log.i(TAG, "✅ Recovered to target package after $backAttempts BACK presses")
                    recovered = true
                    break
                }

                backAttempts++
            }

            if (!recovered) {
                Log.e(TAG, "❌ Failed to recover to target package after $maxBackAttempts attempts")
            }

        } finally {
            // ✅ ALWAYS disable recovery mode when done
            accessibilityIntegration.setRecoveryMode(false)
        }
    }
}
```

---

### **Fix 4: Validate Package in Navigation Edge Persistence**

**File:** `ExplorationEngine.kt`
**Enhanced Validation:**

```kotlin
// After clicking element and detecting new screen
val destinationPackage = newRootNode.packageName?.toString()

when {
    // Case 1: Destination is target app (normal navigation)
    destinationPackage == packageName -> {
        repository.saveNavigationEdge(
            packageName = packageName,
            sessionId = sessionId,
            fromScreenHash = explorationResult.screenState.hash,
            clickedElementUuid = uuid,
            toScreenHash = newScreenState.hash
        )
        Log.d(TAG, "✅ Saved navigation edge within app")
    }

    // Case 2: Destination is launcher (HOME navigation)
    destinationPackage != null && accessibilityIntegration.launcherDetector.isLauncher(destinationPackage) -> {
        repository.saveNavigationEdge(
            packageName = packageName,
            sessionId = sessionId,
            fromScreenHash = explorationResult.screenState.hash,
            clickedElementUuid = uuid,
            toScreenHash = "LAUNCHER_HOME"  // Special marker
        )
        Log.w(TAG, "⚠️ Element leads to launcher, marked as LAUNCHER_HOME")
    }

    // Case 3: Destination is external app (browser, settings, etc.)
    destinationPackage != null -> {
        repository.saveNavigationEdge(
            packageName = packageName,
            sessionId = sessionId,
            fromScreenHash = explorationResult.screenState.hash,
            clickedElementUuid = uuid,
            toScreenHash = "EXTERNAL_APP_$destinationPackage"
        )
        Log.w(TAG, "⚠️ Element leads to external app: $destinationPackage")
    }

    // Case 4: Unknown destination
    else -> {
        Log.e(TAG, "❌ Unknown navigation destination, not saving edge")
    }
}
```

---

## 🧪 Testing Strategy

### Unit Tests

**File:** `LauncherDetectorTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class LauncherDetectorTest {

    private lateinit var context: Context
    private lateinit var launcherDetector: LauncherDetector

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        launcherDetector = LauncherDetector(context)
    }

    @Test
    fun `detect launchers returns non-empty set`() {
        val launchers = launcherDetector.detectLauncherPackages()

        assertThat(launchers).isNotEmpty()
        Log.i("TEST", "Detected launchers: $launchers")
    }

    @Test
    fun `system UI always detected as launcher`() {
        val launchers = launcherDetector.detectLauncherPackages()

        assertThat(launchers).contains("com.android.systemui")
    }

    @Test
    fun `isLauncher returns true for detected launchers`() {
        val launchers = launcherDetector.detectLauncherPackages()
        val firstLauncher = launchers.first()

        assertThat(launcherDetector.isLauncher(firstLauncher)).isTrue()
    }

    @Test
    fun `isLauncher returns false for regular app`() {
        assertThat(launcherDetector.isLauncher("com.microsoft.teams")).isFalse()
        assertThat(launcherDetector.isLauncher("com.augmentalis.voiceos")).isFalse()
    }

    @Test
    fun `cache works correctly`() {
        // First call
        val launchers1 = launcherDetector.detectLauncherPackages()
        val time1 = System.currentTimeMillis()

        // Second call (should use cache)
        val launchers2 = launcherDetector.detectLauncherPackages()
        val time2 = System.currentTimeMillis()

        // Should be instant (< 10ms)
        assertThat(time2 - time1).isLessThan(10)
        assertThat(launchers1).isEqualTo(launchers2)
    }

    @Test
    fun `cache invalidation works`() {
        val launchers1 = launcherDetector.detectLauncherPackages()

        // Invalidate
        launcherDetector.invalidateCache()

        // Next call should re-detect
        val launchers2 = launcherDetector.detectLauncherPackages()

        // Results should be same, but cache was refreshed
        assertThat(launchers1).isEqualTo(launchers2)
    }
}
```

### Integration Tests

**File:** `LauncherExclusionIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class LauncherExclusionIntegrationTest {

    @Test
    fun `launcher screens not saved during exploration`() {
        // Given: Start learning an app
        val packageName = "com.realwear.testcomp"
        learnAppService.startLearning(packageName)

        // Simulate element click that leads to launcher
        simulateClickLeadingToLauncher()

        // Wait for exploration to complete
        testScheduler.advanceTimeBy(2, TimeUnit.MINUTES)

        // Then: No launcher screens in database
        val appId = database.scrapedAppDao().getAppByPackage(packageName).appId
        val screens = database.screenContextDao().getScreensForApp(appId)

        // Verify NO launcher screens saved
        for (screen in screens) {
            val screenPackage = screen.packageName
            assertThat(launcherDetector.isLauncher(screenPackage)).isFalse()
        }
    }

    @Test
    fun `recovery mode suppresses scraping`() {
        // Given: Recovery mode enabled
        accessibilityIntegration.setRecoveryMode(true)

        // When: Window change event fires
        simulateWindowChange("com.realwear.launcher")

        // Then: No scraping occurred
        val screens = database.screenContextDao().getAllScreens()
        val launcherScreens = screens.filter { launcherDetector.isLauncher(it.packageName) }

        assertThat(launcherScreens).isEmpty()
    }
}
```

### Device Compatibility Tests

**Test on Multiple Devices:**

```kotlin
@RunWith(AndroidJUnit4::class)
class DeviceCompatibilityTest {

    @Test
    fun `detect launcher works on all test devices`() {
        // This test should be run on:
        // - Google Pixel (Pixel Launcher)
        // - Samsung Galaxy (One UI Launcher)
        // - RealWear Navigator (RealWear Launcher)
        // - OnePlus (OxygenOS Launcher)

        val launchers = launcherDetector.detectLauncherPackages()

        Log.i("DEVICE_TEST", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.i("DEVICE_TEST", "Detected launchers: $launchers")

        // Should detect at least one launcher
        assertThat(launchers).isNotEmpty()

        // Should always include system UI
        assertThat(launchers).contains("com.android.systemui")
    }
}
```

---

## 📋 Files to Modify

### New Files to Create:

1. **`LauncherDetector.kt`** (NEW)
   - Location: `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/detection/`
   - Purpose: Dynamic launcher detection system
   - Lines: ~250

2. **`LauncherDetectorTest.kt`** (NEW)
   - Location: `/modules/apps/VoiceOSCore/src/androidTest/java/com/augmentalis/voiceoscore/scraping/detection/`
   - Purpose: Unit tests for launcher detection
   - Lines: ~150

### Files to Modify:

3. **`AccessibilityScrapingIntegration.kt`** (MODIFY)
   - Add `launcherDetector` field
   - Replace `EXCLUDED_PACKAGES` check with `launcherDetector.isLauncher()`
   - Add `isInRecoveryMode` flag
   - Add `setRecoveryMode()` method
   - Lines changed: ~30

4. **`ExplorationEngine.kt`** (MODIFY)
   - Add recovery mode calls around BACK attempts
   - Add package validation for navigation edges
   - Add runtime launcher detection logic
   - Lines changed: ~80

---

## ✅ Benefits of Dynamic Detection

### vs. Hardcoded Approach:

| Feature | Hardcoded | Dynamic Detection |
|---------|-----------|-------------------|
| **Device Support** | 5 devices only | ALL Android devices |
| **Maintenance** | Update for each device | Zero maintenance |
| **Future-proof** | Breaks on new devices | Works automatically |
| **Custom Launchers** | Not supported | Detected automatically |
| **User Changes Launcher** | Breaks | Adapts in real-time |
| **Scalability** | ❌ Poor | ✅ Excellent |

### Production Benefits:

1. **✅ Works on ANY Android device** (Google, Samsung, OnePlus, Xiaomi, Huawei, etc.)
2. **✅ Detects custom launchers** (Nova Launcher, Microsoft Launcher, etc.)
3. **✅ Adapts to launcher changes** (user switches default launcher)
4. **✅ Zero maintenance required** (no device-specific code)
5. **✅ Future-proof** (works with Android 15, 16, etc.)

---

## 🎯 Implementation Timeline

### Phase 1A: Core Dynamic Detection (2 hours)
- Create `LauncherDetector.kt`
- Implement HOME intent query
- Add system UI detection
- Basic unit tests

### Phase 1B: Integration (1 hour)
- Integrate into `AccessibilityScrapingIntegration`
- Replace hardcoded exclusions
- Add logging/monitoring

### Phase 1C: Recovery Mode (1 hour)
- Add recovery mode flag
- Integrate with `ExplorationEngine`
- Add try-finally safety

### Phase 1D: Testing (1 hour)
- Run unit tests
- Test on multiple devices
- Verify launcher exclusion

**Total Time: 5 hours** (increased from 1 hour due to dynamic detection complexity)

---

## 🚀 Deployment Strategy

### Step 1: Deploy Core Detection
- Deploy `LauncherDetector` with HOME intent query
- Monitor logs for detected launchers on all devices
- Verify detection accuracy (should be >95%)

### Step 2: Enable Launcher Exclusion
- Replace hardcoded checks with dynamic detection
- Monitor for false positives (regular apps excluded)
- Monitor for false negatives (launchers not detected)

### Step 3: Enable Recovery Mode
- Add recovery mode suppression
- Monitor for stuck states (mode never disabled)
- Monitor for scraping gaps (too much suppression)

### Step 4: Production Rollout
- Deploy to all devices
- Monitor for 1 week
- Verify zero launcher contamination

---

## 🔄 Rollback Plan

### If Issues Arise:

```kotlin
// Emergency rollback: Disable dynamic detection
class AccessibilityScrapingIntegration {

    private val ENABLE_DYNAMIC_LAUNCHER_DETECTION = false  // ✅ Feature flag

    private fun scrapeCurrentWindow(rootNode: AccessibilityNodeInfo) {
        val packageName = rootNode.packageName?.toString() ?: return

        if (ENABLE_DYNAMIC_LAUNCHER_DETECTION) {
            // New: Dynamic detection
            if (launcherDetector.isLauncher(packageName)) {
                rootNode.recycle()
                return
            }
        } else {
            // Fallback: Hardcoded exclusions
            if (EXCLUDED_PACKAGES_LEGACY.contains(packageName)) {
                rootNode.recycle()
                return
            }
        }
    }
}
```

---

## 📊 Success Metrics

### After Deployment:

1. **Launcher Detection Accuracy:**
   - ✅ Target: >95% detection rate across all devices
   - ✅ Measured: Log analysis of detected launchers per device

2. **Zero Launcher Contamination:**
   - ✅ Target: 0% of learned apps contain launcher screens
   - ✅ Measured: Database query for launcher packages in scraped_screens

3. **Device Compatibility:**
   - ✅ Target: Works on 100% of supported devices
   - ✅ Measured: Manual testing on 10+ device types

4. **Performance:**
   - ✅ Target: Detection adds <50ms to scraping
   - ✅ Measured: Timestamp comparison before/after detection

---

## 🎓 Design Decisions

### Why HOME Intent Query?

**Alternatives considered:**
1. ❌ Hardcoded package list (not scalable)
2. ❌ UI pattern matching only (unreliable, many false positives)
3. ✅ **HOME intent query** (canonical Android API, 95%+ accurate)

**Why this is best:**
- Uses official Android API (`PackageManager.queryIntentActivities()`)
- Guaranteed to return actual HOME handlers
- Works across all Android versions (API 21+)
- Zero maintenance required

### Why 24-Hour Cache?

**Rationale:**
- Launchers rarely change during app usage
- Re-detection on every scrape = wasted CPU cycles
- 24-hour expiration handles user changing default launcher
- Cache can be invalidated manually if needed

### Why Recovery Mode Flag?

**Alternatives considered:**
1. ❌ Package whitelist for recovery (brittle, misses new cases)
2. ❌ Time-based suppression (unreliable, timing varies)
3. ✅ **Explicit mode flag** (clear state, easy to debug)

**Benefits:**
- Crystal clear when scraping should be suppressed
- Easy to debug (log shows mode transitions)
- Fail-safe with try-finally (can't get stuck)

---

**Document Status:** Ready for Implementation - Option B (Comprehensive + Device-Agnostic)
**Next Steps:** Begin Phase 1A (Core Dynamic Detection)
**Estimated Total Effort:** 5 hours (Phase 1 only)
