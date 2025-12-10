# Appendix C: Troubleshooting Guide
## VOS4 Developer Manual

**Version:** 4.3.0
**Last Updated:** 2025-11-12
**Status:** Living Document
**Framework:** IDEACODE v5.3

---

## Table of Contents

### Part I: Build Issues
- [C.1 Gradle Build Errors](#c1-gradle-build-errors)
- [C.2 Dependency Resolution](#c2-dependency-resolution)
- [C.3 Compilation Errors](#c3-compilation-errors)

### Part II: Runtime Errors
- [C.4 Accessibility Service Issues](#c4-accessibility-service-issues)
- [C.5 Speech Recognition Errors](#c5-speech-recognition-errors)
- [C.6 Database Errors](#c6-database-errors)
- [C.7 IPC/AIDL Errors](#c7-ipcaidl-errors)

### Part III: Performance Issues
- [C.8 Memory Problems](#c8-memory-problems)
- [C.9 CPU/Battery Drain](#c9-cpubattery-drain)
- [C.10 Slow UI/ANR](#c10-slow-uianr)

### Part IV: Common Fixes
- [C.11 Test Module Fix](#c11-test-module-fix)
- [C.12 TargetSDK Deprecation Fix](#c12-targetsdk-deprecation-fix)
- [C.13 FK Constraint Errors](#c13-fk-constraint-errors)
- [C.14 Screen Duplication Fix](#c14-screen-duplication-fix)

---

## C.1 Gradle Build Errors

### C.1.1 "No matching variant" Error

**Symptom:**
```
> No matching variant of com.google.dagger:hilt-android:2.48 was found.
```

**Cause:** Pure JVM test module trying to use Android AAR dependencies

**Solution:**
```kotlin
// In tests/voiceoscore-unit-tests/build.gradle.kts
plugins {
    id("com.android.library")  // Change from java-library
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.augmentalis.voiceoscore.tests"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Now Android AAR dependencies work
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
}
```

### C.1.2 "Duplicate class" Error

**Symptom:**
```
> Duplicate class kotlin.collections.ArrayDeque found in modules
```

**Cause:** Multiple versions of Kotlin stdlib in classpath

**Solution:**
```kotlin
// In gradle/libs.versions.toml
[versions]
kotlin = "1.9.25"  // Use consistent version everywhere

// Or force resolution in root build.gradle.kts
allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.25")
        }
    }
}
```

### C.1.3 KSP Annotation Processing Fails

**Symptom:**
```
> [ksp] Error: @Dao class not generated
```

**Cause:** Room/Hilt annotation processors not configured correctly

**Solution:**
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"  // Match Kotlin version
}

dependencies {
    // Use ksp, not kapt
    ksp("androidx.room:room-compiler:2.6.0")
    ksp("com.google.dagger:hilt-compiler:2.48")

    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
}
```

### C.1.4 Google Services JSON Missing

**Symptom:**
```
> File google-services.json is missing
```

**Cause:** Firebase plugin enabled but config file not provided

**Solution (Temporary Disable):**
```kotlin
// In app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Temporarily disabled for APK build
    // id("com.google.gms.google-services")
}
```

**Solution (Proper Fix):**
1. Download `google-services.json` from Firebase Console
2. Place in `app/google-services.json`
3. Re-enable plugin

---

## C.2 Dependency Resolution

### C.2.1 Transitive Dependency Conflicts

**Symptom:**
```
> Could not resolve com.squareup.okhttp3:okhttp:4.10.0
> Required by:
>     project :app > com.squareup.retrofit2:retrofit:2.9.0
>     project :app > com.google.cloud:google-cloud-speech:2.0.0
```

**Diagnosis:**
```bash
./gradlew app:dependencies --configuration releaseRuntimeClasspath
```

**Solution:**
```kotlin
dependencies {
    // Force specific version
    implementation("com.squareup.okhttp3:okhttp:4.11.0") {
        force = true
    }

    // Or exclude from one dependency
    implementation("com.squareup.retrofit2:retrofit:2.9.0") {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
}
```

### C.2.2 AAR vs JAR Confusion

**Problem:** Library produces JAR but Android app expects AAR

**Fix:**
```kotlin
// In library module
android {
    // Produces AAR (Android Archive)
    libraryVariants.all { variant ->
        variant.outputs.all {
            outputFileName = "${project.name}-${variant.name}.aar"
        }
    }
}
```

---

## C.3 Compilation Errors

### C.3.1 "Unresolved reference" for Hilt

**Symptom:**
```kotlin
import dagger.hilt.android.AndroidEntryPoint  // Unresolved
```

**Cause:** Hilt plugin not applied or KSP not running

**Fix:**
```kotlin
// In module build.gradle.kts
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// In root build.gradle.kts
plugins {
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
}
```

### C.3.2 "Cannot access database" - Room

**Symptom:**
```kotlin
val db = VoiceOSAppDatabase.getInstance(context)  // Unresolved
```

**Cause:** Room annotation processor didn't run

**Fix:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew :modules:apps:VoiceOSCore:kspDebugKotlin
./gradlew build
```

### C.3.3 targetSdk Deprecation Warning

**Symptom:**
```
WARNING: targetSdk is deprecated in library modules. Use testOptions.targetSdk or lint.targetSdk instead.
```

**Location:** Multiple `build.gradle.kts` files in `modules/apps/`

**Fix:**
```kotlin
// BEFORE (deprecated)
android {
    defaultConfig {
        targetSdk = 34  // ❌ Deprecated
    }
}

// AFTER (correct)
android {
    defaultConfig {
        // Remove targetSdk from here
    }

    testOptions {
        targetSdk = 34  // ✅ Use for test configuration
    }

    lint {
        targetSdk = 34  // ✅ Use for lint checks
    }
}
```

**Files to Fix:**
- `modules/apps/LearnApp/build.gradle.kts:14`
- `modules/apps/VoiceCursor/build.gradle.kts:13`
- `modules/apps/VoiceOSCore/build.gradle.kts:36`

---

## C.4 Accessibility Service Issues

### C.4.1 Service Not Starting

**Symptom:** VoiceOSService doesn't receive events

**Diagnosis:**
```kotlin
// Check if service is enabled
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, VoiceOSService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServicesSetting?.contains(expectedComponentName.flattenToString()) == true
}
```

**Fix 1: Check Manifest**
```xml
<manifest>
    <application>
        <service
            android:name=".accessibility.VoiceOSService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>

    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"
        tools:ignore="ProtectedPermissions" />
</manifest>
```

**Fix 2: Check Service Config**
```xml
<!-- res/xml/accessibility_service_config.xml -->
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackSpoken"
    android:accessibilityFlags="flagRetrieveInteractiveWindows|flagReportViewIds"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity=".accessibility.ui.MainActivity" />
```

**Fix 3: Prompt User to Enable**
```kotlin
fun promptEnableAccessibility(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}
```

### C.4.2 Events Not Received

**Symptom:** `onAccessibilityEvent()` never called

**Diagnosis:**
```kotlin
override fun onServiceConnected() {
    super.onServiceConnected()

    Log.d(TAG, "Service connected - event types: ${serviceInfo?.eventTypes}")
    Log.d(TAG, "Flags: ${serviceInfo?.flags}")
    Log.d(TAG, "Notification timeout: ${serviceInfo?.notificationTimeout}")
}
```

**Fix:**
```kotlin
override fun onServiceConnected() {
    serviceInfo = AccessibilityServiceInfo().apply {
        // Event types to monitor
        eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                     AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                     AccessibilityEvent.TYPE_VIEW_CLICKED

        // Feedback type
        feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN

        // Flags
        flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

        // Package names (empty = all packages)
        packageNames = null

        // Notification timeout (ms)
        notificationTimeout = 100L
    }
}
```

### C.4.3 Root Node is Null

**Symptom:** `rootInActiveWindow` returns `null`

**Cause:** Service not connected, or window not accessible

**Fix:**
```kotlin
fun scrapeCurrentScreen() {
    // Wait for service connection
    if (!isServiceRunning()) {
        Log.w(TAG, "Service not running")
        return
    }

    // Get root node with retry
    val rootNode = rootInActiveWindow
    if (rootNode == null) {
        Log.w(TAG, "Root node null - retrying in 500ms")
        Handler(Looper.getMainLooper()).postDelayed({
            scrapeCurrentScreen()  // Retry
        }, 500)
        return
    }

    // Proceed with scraping
    traverseNode(rootNode)
    rootNode.recycle()  // IMPORTANT: Recycle to avoid memory leak
}
```

### C.4.4 Memory Leak from AccessibilityNodeInfo

**Symptom:** App crashes with OOM after extended use

**Cause:** Not recycling `AccessibilityNodeInfo` objects

**Fix:**
```kotlin
fun traverseNode(node: AccessibilityNodeInfo) {
    try {
        // Process node
        processNode(node)

        // Traverse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child)
                child.recycle()  // ✅ Recycle each child
            }
        }
    } finally {
        // DO NOT recycle root node here if passed from caller
        // Only recycle if you created the node yourself
    }
}

// Usage
val rootNode = rootInActiveWindow
try {
    traverseNode(rootNode)
} finally {
    rootNode?.recycle()  // ✅ Recycle root when done
}
```

---

## C.5 Speech Recognition Errors

### C.5.1 "Speech Recognizer Not Available"

**Symptom:**
```
ERROR: Speech recognizer not available on this device
```

**Diagnosis:**
```kotlin
fun isSpeechRecognitionAvailable(context: Context): Boolean {
    val packageManager = context.packageManager
    val activities = packageManager.queryIntentActivities(
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
        0
    )
    return activities.isNotEmpty()
}
```

**Fix 1: Install Google App**
- Google app provides speech recognition
- Install from Play Store

**Fix 2: Use Alternative Engine**
```kotlin
// Switch to Vivoka (offline)
val config = VivokaConfig(
    apiKey = "your_api_key",
    languageModel = "en-US",
    enableWakeWord = false
)

val result = vivokaEngine.initialize(context, config)
```

### C.5.2 Audio Permission Denied

**Symptom:**
```
ERROR: RECORD_AUDIO permission denied
```

**Fix:**
```kotlin
// Request permission
if (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) != PackageManager.PERMISSION_GRANTED
) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        REQUEST_CODE_AUDIO
    )
}

// Handle result
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    if (requestCode == REQUEST_CODE_AUDIO) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted - start listening
            speechEngine.start()
        } else {
            // Permission denied
            showPermissionDeniedDialog()
        }
    }
}
```

### C.5.3 Storage Permission Denied (API 30+)

**Symptom:**
```
PermissionManager: Storage permission check: false (API 36)
PermissionManager: Permission denied (count: 1, don't ask again: true)
```

**Cause:** On Android 11+ (API 30+), `READ_EXTERNAL_STORAGE` is deprecated and `MANAGE_EXTERNAL_STORAGE` is required for non-media files.

**Root Cause Analysis:**

| Issue | Details |
|-------|---------|
| **Deprecated Permission** | `READ_EXTERNAL_STORAGE` is non-functional on API 33+ |
| **Wrong Check Method** | Using `checkSelfPermission()` instead of `Environment.isExternalStorageManager()` |
| **Scoped Storage** | Granular media permissions don't work for custom file types (`.model`, `.voiceos`) |

**Fix 1: Update Manifest (Both app/ and VoiceOSCore/)**

```xml
<!-- Remove old permissions -->
<!-- DELETE:
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
-->

<!-- Add MANAGE_EXTERNAL_STORAGE for API 30+ -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />

<!-- Keep legacy permissions for API 23-29 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="29" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29"
    tools:ignore="ScopedStorage" />
```

**Fix 2: Update Permission Check**

```kotlin
// PermissionManager.kt
fun hasStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // API 30+: Use correct check method
        Environment.isExternalStorageManager()
    } else {
        // API 23-29: Legacy check
        ContextCompat.checkSelfPermission(context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

**Fix 3: Request Permission (API 30+)**

```kotlin
// Cannot use runtime dialog on API 30+
// Must direct user to system settings
fun requestStoragePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Show rationale dialog
        AlertDialog.Builder(context)
            .setTitle("Storage Access Required")
            .setMessage("VoiceOS needs 'All files access' permission...")
            .setPositiveButton("Open Settings") { _, _ ->
                // Navigate to "All files access" settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .show()
    } else {
        // API 23-29: Use runtime permission dialog
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_CODE_STORAGE
        )
    }
}
```

**User Instructions (API 30+):**

1. Open VoiceOS app
2. Dialog appears: "Storage Access Required"
3. Click "Open Settings"
4. Navigate to: **Settings → Apps → VoiceOS → Special app access → All files access**
5. Enable toggle: **"Allow access to manage all files"**
6. Return to VoiceOS app
7. Permission granted ✅

**Verification:**

```bash
# Check permission status via adb
adb shell dumpsys package com.augmentalis.voiceos | grep MANAGE_EXTERNAL_STORAGE

# Expected output (granted):
# requested permissions:
#   android.permission.MANAGE_EXTERNAL_STORAGE: granted=true

# Check via code
Log.d("Permission", "Has storage: ${Environment.isExternalStorageManager()}")
# Should log: true
```

**Alternative: Use App-Specific Storage (No Permissions)**

If `MANAGE_EXTERNAL_STORAGE` is not granted:

```kotlin
fun getModelsDirectory(): File {
    return if (Environment.isExternalStorageManager()) {
        // Preferred: Shared storage
        File(Environment.getExternalStorageDirectory(), ".voiceos/models")
    } else {
        // Fallback: App-specific storage (no permissions required)
        // Location: /sdcard/Android/data/com.augmentalis.voiceos/files/models/
        context.getExternalFilesDir("models")!!
    }
}
```

**Reference:** See Chapter 19 - Security Design → Storage Permission Migration

### C.5.4 "No Speech Input" Timeout

**Symptom:** Recognition times out after 5-10 seconds of silence

**Fix:**
```kotlin
// Android STT Engine
val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)  // 2 sec
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
}
```

---

## C.6 Database Errors

### C.6.1 Foreign Key Constraint Failed

**Symptom:**
```
android.database.sqlite.SQLiteConstraintException:
FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)
```

**Cause:** Trying to insert element with non-existent `app_id`

**Fix:**
```kotlin
// Always insert app first
val app = AppEntity(
    packageName = "com.example.app",
    appId = "550e8400-e29b-41d4-a716-446655440000",
    appName = "Example App",
    versionCode = 1,
    versionName = "1.0",
    appHash = "abc123..."
)
appDao.insert(app)

// Then insert elements (app_id FK now valid)
val element = ScrapedElementEntity(
    elementHash = "def456...",
    appId = app.appId,  // ✅ Valid FK
    className = "android.widget.Button",
    // ...
)
scrapedElementDao.insert(element)
```

**Debugging:**
```kotlin
// Check if app exists
val appExists = appDao.appExists(packageName)
if (!appExists) {
    Log.e(TAG, "App not found - cannot insert elements")
    return
}

// Check app_id value
val app = appDao.getApp(packageName)
Log.d(TAG, "App ID: ${app?.appId}")
```

### C.6.2 Migration Failed

**Symptom:**
```
android.database.sqlite.SQLiteException:
no such table: apps (code 1 SQLITE_ERROR)
```

**Cause:** Migration not applied or database file corrupted

**Fix 1: Force Migration**
```kotlin
val db = Room.databaseBuilder(context, VoiceOSAppDatabase::class.java, "voiceos_app_database")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    .fallbackToDestructiveMigration()  // ⚠️ Deletes all data if migration fails
    .build()
```

**Fix 2: Manual Migration**
```bash
# Connect to device
adb shell

# Navigate to app data
cd /data/data/com.augmentalis.voiceos/databases

# Backup database
cp voiceos_app_database voiceos_app_database.backup

# Delete database (forces recreation)
rm voiceos_app_database

# Exit and restart app
exit
```

**Fix 3: Inspect Migration**
```kotlin
// Enable SQL logging
val db = Room.databaseBuilder(context, VoiceOSAppDatabase::class.java, "voiceos_app_database")
    .setQueryCallback({ sqlQuery, bindArgs ->
        Log.d("RoomQuery", "SQL: $sqlQuery, Args: $bindArgs")
    }, Executors.newSingleThreadExecutor())
    .build()
```

### C.6.3 Screen Duplication Issue

**Symptom:** Duplicate screens created with different `windowTitle` but same content

**Cause:** Using `windowId` (changes frequently) instead of `windowTitle` for screen hash

**Fix:**
```kotlin
// BEFORE (incorrect)
val screenHash = calculateHash(
    packageName = packageName,
    windowId = windowInfo.id.toString(),  // ❌ Changes frequently
    activityName = activityName
)

// AFTER (correct)
val screenHash = calculateHash(
    packageName = packageName,
    windowTitle = windowInfo.title?.toString() ?: "",  // ✅ Stable
    activityName = activityName
)
```

**Migration to Fix Duplicates:**
```sql
-- Find duplicates
SELECT package_name, window_title, COUNT(*) as count
FROM screen_contexts
GROUP BY package_name, window_title
HAVING count > 1;

-- Merge duplicates (keep oldest)
DELETE FROM screen_contexts
WHERE id NOT IN (
    SELECT MIN(id)
    FROM screen_contexts
    GROUP BY package_name, window_title
);
```

---

## C.7 IPC/AIDL Errors

### C.7.1 Service Binding Failed

**Symptom:**
```kotlin
ERROR: Failed to bind to VoiceOS IPC service
```

**Diagnosis:**
```bash
# Check if VoiceOSIPCService is exported
adb shell dumpsys package com.augmentalis.voiceoscore | grep -A 5 VoiceOSIPCService

# Check if accessibility service is running
adb shell dumpsys accessibility | grep VoiceOSService
```

**Common Causes:**

**1. Accessibility Service Not Enabled**
```bash
# Solution: Enable accessibility service
adb shell settings put secure enabled_accessibility_services \
  com.augmentalis.voiceos/com.augmentalis.voiceoscore.accessibility.VoiceOSService

adb shell settings put secure accessibility_enabled 1
```

**2. VoiceOSIPCService Not Started**
```bash
# Check logcat for startup errors
adb logcat -s VoiceOSService:* VoiceOSServiceBinder:* VoiceOSIPCService:*

# Look for:
# "VoiceOSIPCService created and bound successfully"
# "Started VoiceOSIPCService successfully"
```

**3. Wrong Intent Action**
```kotlin
// WRONG
val intent = Intent(context, VoiceOSIPCService::class.java)

// CORRECT
val intent = Intent().apply {
    action = "com.augmentalis.voiceoscore.BIND_IPC"
    `package` = "com.augmentalis.voiceoscore"
}
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

**4. Signature Protection Mismatch**
```bash
# Check if apps are signed with same certificate
adb shell pm list packages -f | grep augmentalis

# Verify signatures match
adb shell dumpsys package com.augmentalis.voiceos | grep "signatures="
adb shell dumpsys package com.augmentalis.voiceos.ipctest | grep "signatures="
```

### C.7.2 AIDL Method Call Returns Null

**Symptom:**
```kotlin
val result = voiceOSService?.getAvailableCommands()
// result is null
```

**Cause 1: Service Not Ready**
```kotlin
// Solution: Check service status first
if (voiceOSService?.isServiceReady() == true) {
    val commands = voiceOSService?.getAvailableCommands()
} else {
    Log.w(TAG, "Service not ready - cannot get commands")
}
```

**Cause 2: Database Not Initialized**
```bash
# Check logs for database initialization
adb logcat -s VoiceOSService:* | grep -i "database"

# Expected:
# "Database initialized successfully"
# "AppScrapingDatabase instance created"
```

**Cause 3: Exception in Service Method**
```kotlin
// Check service-side logs
adb logcat -s VoiceOSServiceBinder:* | grep -i "error"

// Look for stack traces in IPC method implementations
```

### C.7.3 Callback Not Receiving Events

**Symptom:**
```kotlin
// Callback never called
voiceOSService?.registerCallback(myCallback)
// No onCommandRecognized, onCommandExecuted, etc.
```

**Fix 1: Verify Callback Implementation**
```kotlin
// Must extend IVoiceOSCallback.Stub
private val callback = object : IVoiceOSCallback.Stub() {
    override fun onCommandRecognized(command: String?, confidence: Float) {
        // Must use runOnUiThread if updating UI
        runOnUiThread {
            Log.d(TAG, "Command recognized: $command")
        }
    }

    override fun onCommandExecuted(command: String?, success: Boolean, message: String?) {
        runOnUiThread {
            Log.d(TAG, "Command executed: $command, success: $success")
        }
    }

    override fun onServiceStateChanged(state: Int, message: String?) {
        runOnUiThread {
            Log.d(TAG, "Service state changed: $state")
        }
    }

    override fun onScrapingComplete(elementsJson: String?, elementCount: Int) {
        runOnUiThread {
            Log.d(TAG, "Scraping complete: $elementCount elements")
        }
    }
}
```

**Fix 2: Register Callback After Binding**
```kotlin
private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        voiceOSService = IVoiceOSService.Stub.asInterface(service)

        // Register callback AFTER service connected
        try {
            voiceOSService?.registerCallback(callback)
            Log.d(TAG, "Callback registered successfully")
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register callback", e)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        voiceOSService = null
    }
}
```

**Fix 3: Unregister on Disconnect**
```kotlin
override fun onDestroy() {
    try {
        voiceOSService?.unregisterCallback(callback)
    } catch (e: RemoteException) {
        Log.e(TAG, "Error unregistering callback", e)
    }

    unbindService(serviceConnection)
    super.onDestroy()
}
```

### C.7.4 RemoteException During IPC Call

**Symptom:**
```
android.os.RemoteException: Transaction failed
```

**Cause 1: Dead Object Exception**
```kotlin
// Service crashed or was killed
try {
    val result = voiceOSService?.executeCommand("go home")
} catch (e: DeadObjectException) {
    Log.e(TAG, "Service died - rebinding", e)
    // Rebind to service
    bindToService()
}
```

**Cause 2: Binder Transaction Too Large**
```kotlin
// PROBLEM: Returning too much data (> 1MB)
fun learnCurrentApp(): String {
    // Returns 5MB of JSON - exceeds binder limit
    return gson.toJson(allElements)  // ❌
}

// SOLUTION: Limit data size
fun learnCurrentApp(): String {
    val elements = allElements.take(50)  // ✅ Limit to 50 elements
    return gson.toJson(elements)
}
```

**Cause 3: Service Method Throws Exception**
```bash
# Check service logs for exceptions
adb logcat -s VoiceOSServiceBinder:* | grep -A 10 "Exception"

# Look for:
# NullPointerException
# IllegalStateException
# Database errors
```

### C.7.5 AIDL Compilation Errors

**Symptom:**
```
e: Unresolved reference: IVoiceOSService
```

**Fix 1: AIDL Files in Correct Location**
```bash
# Verify AIDL structure
ls -la modules/apps/VoiceOSCore/src/main/aidl/com/augmentalis/voiceoscore/

# Should contain:
# IVoiceOSService.aidl
# IVoiceOSCallback.aidl
# CommandResult.aidl (if using parcelable)
```

**Fix 2: Build AIDL Files**
```bash
# Clean and rebuild
./gradlew clean

# Build AIDL interfaces
./gradlew :modules:apps:VoiceOSCore:compileDebugAidl

# Check generated files
ls -la modules/apps/VoiceOSCore/build/generated/aidl_source_output_dir/debug/out/
```

**Fix 3: Sync AIDL Across Modules**
```bash
# Copy AIDL files to test client
cp -r modules/apps/VoiceOSCore/src/main/aidl/com/augmentalis/voiceoscore/ \
     modules/apps/VoiceOSIPCTest/src/main/aidl/com/augmentalis/

# Rebuild test client
./gradlew :modules:apps:VoiceOSIPCTest:assembleDebug
```

### C.7.6 SecurityException: Permission Denied

**Symptom:**
```
java.lang.SecurityException: Permission Denial:
attempting to connect to service with protectionLevel=signature
```

**Cause:** Apps not signed with same certificate

**Diagnosis:**
```bash
# Get signing certificate for VoiceOS
adb shell dumpsys package com.augmentalis.voiceos | grep "Signing certificate"

# Get signing certificate for test client
adb shell dumpsys package com.augmentalis.voiceos.ipctest | grep "Signing certificate"

# Certificates must match exactly
```

**Fix 1: Use Same Signing Key**
```kotlin
// In both modules' build.gradle.kts
android {
    signingConfigs {
        create("debug") {
            storeFile = file("../keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
```

**Fix 2: Development Only - Lower Protection Level**
```xml
<!-- ONLY FOR DEVELOPMENT - NOT PRODUCTION -->
<service
    android:name=".accessibility.VoiceOSIPCService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:protectionLevel="normal"  <!-- ⚠️ Insecure - dev only -->
    android:exported="true">
</service>
```

### C.7.7 IPC Test Client Build Failures

**Symptom:**
```
e: Cannot find a parameter with this name: command
e: Type mismatch in callback
```

**Cause:** AIDL interface mismatch between server and client

**Fix:**
```bash
# 1. Clean both modules
./gradlew :modules:apps:VoiceOSCore:clean
./gradlew :modules:apps:VoiceOSIPCTest:clean

# 2. Sync AIDL files (must be identical)
rm -rf modules/apps/VoiceOSIPCTest/src/main/aidl/com/augmentalis/voiceoscore/
cp -r modules/apps/VoiceOSCore/src/main/aidl/com/augmentalis/voiceoscore/ \
     modules/apps/VoiceOSIPCTest/src/main/aidl/com/augmentalis/

# 3. Rebuild in order (server first)
./gradlew :modules:apps:VoiceOSCore:assembleDebug
./gradlew :modules:apps:VoiceOSIPCTest:assembleDebug
```

### C.7.8 Debugging IPC Issues

**Enable Verbose Logging:**
```bash
# Enable all IPC logs
adb shell setprop log.tag.VoiceOSIPCService VERBOSE
adb shell setprop log.tag.VoiceOSServiceBinder VERBOSE
adb shell setprop log.tag.VoiceOSIPCTest VERBOSE

# View logs
adb logcat -s VoiceOSIPCService:V VoiceOSServiceBinder:V VoiceOSIPCTest:V
```

**Check Service Binding Status:**
```bash
# List all bound services
adb shell dumpsys activity services | grep -A 10 VoiceOSIPCService

# Expected output:
# ServiceRecord{...VoiceOSIPCService}
# app=ProcessRecord{...}
# bindings:
#   * IntentBindRecord{...}
```

**Verify AIDL Method Signatures:**
```bash
# Generate AIDL Java source to inspect
./gradlew :modules:apps:VoiceOSCore:compileDebugAidl

# View generated interface
cat modules/apps/VoiceOSCore/build/generated/aidl_source_output_dir/debug/out/com/augmentalis/voiceoscore/IVoiceOSService.java
```

**Common Logcat Patterns:**

**Success:**
```
I/VoiceOSService: VoiceOSIPCService created and bound successfully
I/VoiceOSServiceBinder: Binder onBind() called - returning IVoiceOSService
I/VoiceOSIPCTest: Service connected successfully
```

**Failure:**
```
E/VoiceOSIPCTest: Failed to bind service - check accessibility service enabled
W/VoiceOSServiceBinder: Service not ready - returning null
E/VoiceOSIPCService: RemoteException during callback notification
```

---

## C.8 Memory Problems

### C.8.1 Out of Memory (OOM) Crashes

**Symptom:**
```
java.lang.OutOfMemoryError: Failed to allocate a 12345678 byte allocation
```

**Diagnosis:**
```kotlin
// Log memory usage
val runtime = Runtime.getRuntime()
val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
val maxMemory = runtime.maxMemory() / 1024 / 1024
Log.d(TAG, "Memory: $usedMemory MB / $maxMemory MB")
```

**Fix 1: Increase Heap Size**
```xml
<!-- AndroidManifest.xml -->
<application
    android:largeHeap="true">  <!-- Increases max heap -->
</application>
```

**Fix 2: Recycle AccessibilityNodeInfo**
```kotlin
// Always recycle
val node = parent.getChild(i)
try {
    processNode(node)
} finally {
    node?.recycle()
}
```

**Fix 3: Limit Scraping Depth**
```kotlin
const val MAX_DEPTH = 10

fun traverseNode(node: AccessibilityNodeInfo, depth: Int = 0) {
    if (depth > MAX_DEPTH) {
        Log.w(TAG, "Max depth reached - skipping subtree")
        return
    }

    // Process node
    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child != null) {
            traverseNode(child, depth + 1)
            child.recycle()
        }
    }
}
```

### C.8.2 Memory Leaks

**Diagnosis:**
```bash
# Use LeakCanary
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

**Common Leaks:**

**1. Context Leaks**
```kotlin
// BAD - leaks Activity
class MyManager(private val context: Context)

// GOOD - use Application context
class MyManager(private val context: Context) {
    private val appContext = context.applicationContext
}
```

**2. Listener Leaks**
```kotlin
// BAD - listener never removed
speechEngine.addListener(myListener)

// GOOD - remove in onDestroy
override fun onDestroy() {
    speechEngine.removeListener(myListener)
    super.onDestroy()
}
```

---

## C.9 CPU/Battery Drain

### C.9.1 High CPU Usage

**Diagnosis:**
```bash
# Profile CPU usage
adb shell top | grep "com.augmentalis.voiceos"
```

**Fix 1: Debounce Events**
```kotlin
class Debouncer(private val delayMs: Long) {
    private var lastAction: Long = 0

    fun execute(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastAction > delayMs) {
            action()
            lastAction = now
        }
    }
}

// Usage
val debouncer = Debouncer(500)  // 500ms debounce

override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    debouncer.execute {
        processEvent(event)
    }
}
```

**Fix 2: Throttle Scraping**
```kotlin
private var lastScrape: Long = 0
private val SCRAPE_INTERVAL_MS = 1000  // 1 second

fun scrapeCurrentScreen() {
    val now = System.currentTimeMillis()
    if (now - lastScrape < SCRAPE_INTERVAL_MS) {
        Log.d(TAG, "Skipping scrape - too soon")
        return
    }

    lastScrape = now
    performScrape()
}
```

### C.9.2 Battery Drain

**Fix 1: Use WorkManager for Background Tasks**
```kotlin
// Instead of continuous foreground service
val work = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueue(work)
```

**Fix 2: Reduce Wake Locks**
```kotlin
// Use partial wake lock (not full)
val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
    .newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,  // Screen can turn off
        "VoiceOS::SpeechRecognition"
    )

wakeLock.acquire(10*60*1000L /*10 minutes*/)
try {
    // Perform work
} finally {
    wakeLock.release()
}
```

---

## C.10 Slow UI/ANR

### C.10.1 Application Not Responding (ANR)

**Symptom:**
```
ANR in com.augmentalis.voiceos (pid 12345)
Reason: Input dispatching timed out
```

**Cause:** Long-running operation on main thread

**Fix:**
```kotlin
// BAD - blocks UI thread
fun scrapeCurrentScreen() {
    val elements = scrapingEngine.scrapeAllElements()  // 5+ seconds
    database.insert(elements)  // 2+ seconds
}

// GOOD - use coroutines
fun scrapeCurrentScreen() {
    viewModelScope.launch(Dispatchers.IO) {
        val elements = scrapingEngine.scrapeAllElements()
        database.insert(elements)

        withContext(Dispatchers.Main) {
            // Update UI
            showScrapeComplete(elements.size)
        }
    }
}
```

### C.10.2 Slow Database Queries

**Fix 1: Use Indices**
```sql
-- Slow
SELECT * FROM scraped_elements WHERE text LIKE '%submit%';

-- Fast (if indexed)
SELECT * FROM scraped_elements WHERE element_hash = 'abc123';
```

**Fix 2: Batch Operations**
```kotlin
// Slow - multiple transactions
elements.forEach { element ->
    dao.insert(element)
}

// Fast - single transaction
dao.insertBatch(elements)
```

---

## C.11 Test Module Fix

**Problem:** `voiceoscore-unit-tests` module fails to build

**Error:**
```
No matching variant of com.google.dagger:hilt-android:2.48 was found
```

**Root Cause:** Module configured as pure JVM (`java-library`) but uses Android AAR dependencies

**Complete Fix:**
```kotlin
// tests/voiceoscore-unit-tests/build.gradle.kts
plugins {
    id("com.android.library")  // Change from java-library
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.augmentalis.voiceoscore.tests"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Project dependencies
    implementation(project(":modules:apps:VoiceOSCore"))

    // Android test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")

    // Hilt (AAR)
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    testImplementation("com.google.dagger:hilt-android-testing:2.48")

    // Room (AAR)
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    testImplementation("androidx.room:room-testing:2.6.0")
}
```

---

## C.12 TargetSDK Deprecation Fix

**Warning:**
```
WARNING: targetSdk is deprecated in library modules
```

**Affected Files:**
- `modules/apps/LearnApp/build.gradle.kts:14`
- `modules/apps/VoiceCursor/build.gradle.kts:13`
- `modules/apps/VoiceOSCore/build.gradle.kts:36`

**Fix:**
```kotlin
// BEFORE
android {
    defaultConfig {
        targetSdk = 34  // ❌ Deprecated
    }
}

// AFTER
android {
    defaultConfig {
        // Remove targetSdk
    }

    testOptions {
        targetSdk = 34  // ✅ Correct location
    }

    lint {
        targetSdk = 34  // ✅ For lint checks
    }
}
```

---

## C.13 FK Constraint Errors

**Error:**
```
FOREIGN KEY constraint failed (code 787)
```

**Debugging Steps:**

1. **Check FK exists:**
```sql
SELECT * FROM apps WHERE app_id = '550e8400-...';
```

2. **Check FK value matches:**
```kotlin
Log.d(TAG, "Inserting element with app_id: ${element.appId}")
val app = appDao.getAppById(element.appId)
Log.d(TAG, "App exists: ${app != null}")
```

3. **Enable FK debugging:**
```sql
PRAGMA foreign_keys=ON;  -- Enable FK constraints
PRAGMA foreign_key_check;  -- Check violations
```

**Common Fixes:**
- Insert parent entity first
- Use correct app_id (from apps.app_id, not apps.package_name)
- Check for typos in FK values

---

## C.14 Screen Duplication Fix

**Issue:** Same screen created multiple times with different IDs

**Root Cause:** Using unstable `windowId` instead of stable `windowTitle`

**Detection:**
```sql
-- Find duplicates
SELECT package_name, window_title, COUNT(*) as count
FROM screen_contexts
GROUP BY package_name, window_title
HAVING count > 1
ORDER BY count DESC;
```

**Fix:**
```kotlin
// Update screen hash calculation
fun calculateScreenHash(
    packageName: String,
    windowTitle: String,  // ✅ Use title (stable)
    activityName: String?
): String {
    val content = "$packageName|$windowTitle|$activityName"
    return MessageDigest.getInstance("SHA-256")
        .digest(content.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
```

---

## Summary

**Most Common Issues:**
1. Build errors (test module, dependencies)
2. Accessibility service not enabled
3. IPC service binding failures (NEW - Phase 3)
4. Database FK constraints
5. Memory leaks from AccessibilityNodeInfo
6. ANR from main thread blocking

**Quick Diagnostics:**
```bash
# Build issues
./gradlew clean build --stacktrace

# Runtime logs
adb logcat | grep "VoiceOS"

# IPC debugging
adb logcat -s VoiceOSIPCService:* VoiceOSServiceBinder:* VoiceOSIPCTest:*

# Database inspection
adb shell
run-as com.augmentalis.voiceos
cd databases
sqlite3 voiceos_app_database
```

**IPC Troubleshooting Quick Reference:**
```bash
# Check accessibility service
adb shell settings get secure enabled_accessibility_services

# Check service binding
adb shell dumpsys activity services | grep VoiceOSIPCService

# Enable verbose IPC logging
adb shell setprop log.tag.VoiceOSIPCService VERBOSE
adb shell setprop log.tag.VoiceOSServiceBinder VERBOSE
```

---

**Version:** 4.3.0
**Last Updated:** 2025-11-12
**Status:** Complete
**Next Appendix:** [Appendix D: Glossary](Appendix-D-Glossary.md)
**Previous Chapter:** [Chapter 39: Testing and Validation Guide](39-Testing-Validation-Guide.md)
