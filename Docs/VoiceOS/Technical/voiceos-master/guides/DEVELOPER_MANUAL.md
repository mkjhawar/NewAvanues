# VOS4 Developer Manual

## Table of Contents
1. [Getting Started](#getting-started)
2. [Development Environment Setup](#development-environment-setup)
3. [Module Development](#module-development)
   - [Creating a New Module](#creating-a-new-module)
   - [Zero-Overhead Implementation Pattern](#zero-overhead-implementation-pattern)
   - [AIDL Service Implementation](#aidl-service-implementation)
   - [Plugin System API](#plugin-system-api)
   - [ActionFactory Dynamic Actions](#actionfactory-dynamic-actions)
   - [NotificationListenerService Integration](#notificationlistenerservice-integration)
   - [Intent Routing Persistence](#intent-routing-persistence)
4. [Testing Guidelines](#testing-guidelines)
5. [Performance Optimization](#performance-optimization)
6. [Debugging Guide](#debugging-guide)
7. [Release Process](#release-process)
8. [Troubleshooting](#troubleshooting)

---

## Getting Started

### Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: Version 17
- **Android SDK**: API levels 28-34
- **Kotlin**: Version 1.9.22
- **Gradle**: Version 8.11.1
- **Git**: Version control

### Project Setup

1. **Clone the Repository**
```bash
git clone https://gitlab.com/your-repo/vos4.git
cd vos4
```

2. **Open in Android Studio**
- File → Open → Select VOS4 directory
- Wait for Gradle sync to complete

3. **Configure SDK**
```groovy
// In local.properties
sdk.dir=/path/to/Android/sdk
```

4. **Build the Project**
```bash
./gradlew build
```

---

## Development Environment Setup

### Android Studio Configuration

#### Recommended Plugins
- **Kotlin**: Latest version
- **Rainbow Brackets**: Code readability
- **Key Promoter X**: Learn shortcuts
- **ADB Idea**: Quick ADB commands
- **Codota**: AI code completion

#### Code Style Settings
```xml
<!-- .idea/codeStyles/Project.xml -->
<code_scheme name="Project" version="173">
  <JetCodeStyleSettings>
    <option name="PACKAGES_TO_USE_STAR_IMPORTS">
      <value />
    </option>
    <option name="NAME_COUNT_TO_USE_STAR_IMPORT" value="99" />
    <option name="NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS" value="99" />
  </JetCodeStyleSettings>
  <codeStyleSettings language="kotlin">
    <option name="RIGHT_MARGIN" value="120" />
    <option name="KEEP_BLANK_LINES_IN_CODE" value="1" />
  </codeStyleSettings>
</code_scheme>
```

#### Run Configurations

1. **Main App**
```
Name: VOS4 App
Module: app
Deploy: APK from app bundle
Launch: Default Activity
```

2. **DeviceManager Tests**
```
Name: DeviceManager Tests
Module: libraries.DeviceManager
Test: All in package
Package: com.augmentalis.devicemanager
```

### Git Configuration

#### Branch Strategy
```bash
# Main branches
main          # Production-ready code
develop       # Development branch
feature/*     # Feature branches
bugfix/*      # Bug fixes
hotfix/*      # Emergency fixes
release/*     # Release preparation
```

#### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>

# Examples:
feat(device): Add AR glasses detection
fix(speech): Resolve memory leak in VAD
docs(api): Update authentication documentation
test(network): Add UWB ranging tests
```

---

## Module Development

### Creating a New Module

1. **Module Structure**
```bash
libraries/NewModule/
├── src/
│   ├── main/
│   │   ├── java/com/augmentalis/newmodule/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── test/
├── build.gradle.kts
├── proguard-rules.pro
└── README.md
```

2. **Module Build Configuration**
```kotlin
// build.gradle.kts
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.augmentalis.newmodule"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 28
        targetSdk = 34
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    // Core dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

3. **Register Module**
```kotlin
// settings.gradle.kts
include(":libraries:NewModule")
```

### Zero-Overhead Implementation Pattern

#### Direct Access Pattern
```kotlin
// ❌ Don't: Wrapper pattern
class DeviceWrapper {
    private val device = getDevice()
    fun getInfo(): Info = device.getInfo()
}

// ✅ Do: Direct access
inline fun getDeviceInfo(): DeviceInfo = Build.MODEL
```

#### State Management
```kotlin
class ModuleManager {
    // Use StateFlow for reactive state
    private val _state = MutableStateFlow(ModuleState())
    val state: StateFlow<ModuleState> = _state.asStateFlow()
    
    // Update state efficiently
    fun updateState(update: ModuleState.() -> ModuleState) {
        _state.update { it.update() }
    }
}
```

#### Memory Management
```kotlin
// Object pooling for frequent allocations
object BufferPool {
    private val pool = LinkedList<ByteBuffer>()
    
    fun acquire(size: Int): ByteBuffer {
        return pool.poll()?.clear() ?: ByteBuffer.allocateDirect(size)
    }
    
    fun release(buffer: ByteBuffer) {
        if (pool.size < MAX_POOL_SIZE) {
            pool.offer(buffer)
        }
    }
}
```

### AIDL Service Implementation

1. **Define AIDL Interface**
```aidl
// IModuleService.aidl
package com.augmentalis.module;

interface IModuleService {
    void startOperation(in Bundle config);
    void stopOperation();
    String getStatus();
    void registerCallback(IModuleCallback callback);
}
```

2. **Implement Service**
```kotlin
class ModuleService : Service() {
    private val binder = object : IModuleService.Stub() {
        override fun startOperation(config: Bundle) {
            // Implementation
        }
        
        override fun stopOperation() {
            // Implementation
        }
        
        override fun getStatus(): String {
            return currentStatus
        }
        
        override fun registerCallback(callback: IModuleCallback) {
            callbacks.register(callback)
        }
    }
    
    override fun onBind(intent: Intent): IBinder = binder
}
```

3. **Client Connection**
```kotlin
class ModuleClient(private val context: Context) {
    private var service: IModuleService? = null
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IModuleService.Stub.asInterface(binder)
        }
        
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }
    
    fun connect() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.augmentalis.module",
                "com.augmentalis.module.ModuleService"
            )
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
}
```

### Plugin System API

VOS4 supports dynamic action plugins with versioning and compatibility checking.

#### Plugin Interface

```kotlin
interface ActionPlugin {
    val pluginId: String
    val version: String
    val supportedActions: List<String>

    // API versioning (v1 by default)
    val apiVersion: Int
        get() = 1

    // Command category (CUSTOM by default)
    val category: String
        get() = "CUSTOM"

    suspend fun execute(action: String, params: Map<String, Any>): PluginResult
    fun getMetadata(): Map<String, Any>
}
```

#### Plugin Metadata

```kotlin
data class PluginMetadata(
    val pluginId: String,
    val version: String,
    val name: String,
    val description: String,
    val author: String,
    val minVOSVersion: Int,
    val apiVersion: Int,
    val category: String,
    val requestedPermissions: List<PluginPermission>,
    val signatureHash: String,
    val packageName: String,
    val className: String
)
```

#### Compatibility Checking

```kotlin
// Check compatibility using metadata
val result = PluginVersioning.checkCompatibility(
    metadata = pluginMetadata,
    currentVOSVersion = BuildConfig.VERSION_CODE
)

when (result) {
    is CompatibilityResult.Compatible -> loadPlugin(metadata)
    is CompatibilityResult.Incompatible -> {
        Log.w(TAG, "Plugin ${metadata.pluginId} incompatible: ${result.reason}")
    }
}
```

#### Plugin Manager Usage

```kotlin
// Get plugin permissions
val permissions = pluginManager.getPluginPermissions(pluginId)

// Get plugin load timestamp
val loadedAt = pluginManager.getPluginLoadedAt(pluginId)

// Load plugin with manifest
val success = pluginManager.loadPlugin(context, pluginPackage)
```

### ActionFactory Dynamic Actions

The ActionFactory supports 8 categories of dynamic actions with real Android API implementations.

#### Scroll Actions

```kotlin
// DynamicScrollAction uses AccessibilityNodeInfo + gesture fallback
class DynamicScrollAction(
    private val direction: String,  // up, down, left, right, top, bottom
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(...): CommandResult {
        // Try accessibility scroll first
        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode != null) {
            val action = when (direction) {
                "up", "top" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "down", "bottom" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else -> return error
            }
            return if (scrollableNode.performAction(action)) success else fail
        }

        // Fallback to gesture-based scrolling
        val gesture = createScrollGesture(direction, displayMetrics)
        accessibilityService.dispatchGesture(gesture, null, null)
    }
}
```

#### Media Actions

```kotlin
// DynamicMediaAction uses AudioManager key events
class DynamicMediaAction(
    private val action: String  // play, pause, stop, next, previous, etc.
) : BaseAction() {
    override suspend fun execute(...): CommandResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val keyCode = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "play_pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "volume_up" -> KeyEvent.KEYCODE_VOLUME_UP
            "volume_down" -> KeyEvent.KEYCODE_VOLUME_DOWN
            "mute" -> KeyEvent.KEYCODE_VOLUME_MUTE
            else -> return error
        }

        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
```

#### Interaction Actions

```kotlin
// DynamicInteractionAction uses GestureDescription
class DynamicInteractionAction(
    private val action: String,  // tap, long_press, swipe, double_tap
    private val params: Map<String, Any>
) : BaseAction() {
    private fun createTapGesture(x: Float, y: Float): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        return GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, 50))
            .build()
    }

    private fun createLongPressGesture(x: Float, y: Float): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        return GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, 1000))
            .build()
    }

    private fun createSwipeGesture(
        startX: Float, startY: Float,
        endX: Float, endY: Float
    ): GestureDescription {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        return GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, 300))
            .build()
    }
}
```

#### App Actions

```kotlin
// DynamicAppAction with package name resolution
class DynamicAppAction(
    private val appName: String,
    private val action: String  // open, close
) : BaseAction() {
    private fun getCommonAppPackage(appName: String): String? {
        return when (appName.lowercase()) {
            "chrome", "google chrome" -> "com.android.chrome"
            "youtube" -> "com.google.android.youtube"
            "gmail" -> "com.google.android.gm"
            "maps", "google maps" -> "com.google.android.apps.maps"
            "calendar" -> "com.google.android.calendar"
            "phone", "dialer" -> "com.google.android.dialer"
            "messages", "sms" -> "com.google.android.apps.messaging"
            "camera" -> "com.android.camera2"
            "photos", "google photos" -> "com.google.android.apps.photos"
            "settings" -> "com.android.settings"
            "clock" -> "com.google.android.deskclock"
            "calculator" -> "com.google.android.calculator"
            "contacts" -> "com.google.android.contacts"
            "files" -> "com.google.android.documentsui"
            "play store" -> "com.android.vending"
            "spotify" -> "com.spotify.music"
            "whatsapp" -> "com.whatsapp"
            "instagram" -> "com.instagram.android"
            "facebook" -> "com.facebook.katana"
            "twitter", "x" -> "com.twitter.android"
            else -> null
        }
    }
}
```

### NotificationListenerService Integration

VOS4 captures and manages notifications via a dedicated listener service.

#### Service Implementation

```kotlin
class VoiceOSNotificationListener : NotificationListenerService() {
    companion object {
        @Volatile
        var instance: VoiceOSNotificationListener? = null
            private set

        val isConnected: Boolean
            get() = instance != null
    }

    private val notifications = ConcurrentHashMap<String, NotificationData>()

    override fun onListenerConnected() {
        instance = this
        // Load existing notifications
        activeNotifications?.forEach { sbn ->
            processNotification(sbn)
        }
    }

    override fun onListenerDisconnected() {
        instance = null
        notifications.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        notifications.remove(sbn.key)
    }

    fun getActiveNotifications(): List<NotificationData> {
        return notifications.values
            .sortedByDescending { it.postTime }
            .toList()
    }

    fun dismissNotification(key: String): Boolean {
        val notification = notifications[key] ?: return false
        if (!notification.isClearable) return false
        cancelNotification(key)
        notifications.remove(key)
        return true
    }
}
```

#### Notification Data Model

```kotlin
data class NotificationData(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val appName: String,
    val postTime: Long,
    val contentIntent: PendingIntent?,
    val isOngoing: Boolean,
    val isClearable: Boolean
)
```

#### Manifest Declaration

```xml
<service
    android:name=".notifications.VoiceOSNotificationListener"
    android:label="VoiceOS Notification Access"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

#### Usage in Actions

```kotlin
// Get notification listener instance
val listener = VoiceOSNotificationListener.instance
if (listener == null) {
    Log.w(TAG, "NotificationListener not connected")
    return emptyList()
}

// Get all notifications
val notifications = listener.getActiveNotifications()

// Dismiss specific notification
val success = listener.dismissNotification(notificationKey)

// Check if connected
if (VoiceOSNotificationListener.isConnected) {
    // Proceed with notification operations
}
```

### Intent Routing Persistence

The IntentDispatcher supports persistent routing rules and analytics.

#### Persistence Setup

```kotlin
class IntentDispatcher(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "intent_dispatcher"
        private const val KEY_ROUTING_RULES = "routing_rules"
        private const val KEY_FALLBACK_ATTEMPTS = "fallback_attempts"
        private const val KEY_SUCCESSFUL_ROUTES = "successful_routes"
        private const val KEY_FAILED_ROUTES = "failed_routes"
        private const val MAX_FEEDBACK_HISTORY = 500
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var fallbackAttempts = prefs.getLong(KEY_FALLBACK_ATTEMPTS, 0L)
    private var successfulRoutes = prefs.getLong(KEY_SUCCESSFUL_ROUTES, 0L)
    private var failedRoutes = prefs.getLong(KEY_FAILED_ROUTES, 0L)

    init {
        loadRoutingRules()
    }
}
```

#### Routing Rules Storage

```kotlin
private fun loadRoutingRules() {
    val json = prefs.getString(KEY_ROUTING_RULES, null) ?: return
    try {
        val rulesArray = JSONArray(json)
        for (i in 0 until rulesArray.length()) {
            val obj = rulesArray.getJSONObject(i)
            val rule = DynamicRoutingRule(
                handlerCategory = obj.getString("handlerCategory"),
                confidenceMultiplier = obj.getDouble("confidenceMultiplier").toFloat(),
                successCount = obj.getInt("successCount"),
                failureCount = obj.getInt("failureCount"),
                lastUsed = obj.getLong("lastUsed"),
                averageLatencyMs = obj.getDouble("averageLatencyMs").toFloat()
            )
            routingRules[rule.handlerCategory] = rule
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load routing rules", e)
    }
}

private fun saveRoutingRules() {
    val rulesArray = JSONArray()
    for (rule in routingRules.values) {
        rulesArray.put(JSONObject().apply {
            put("handlerCategory", rule.handlerCategory)
            put("confidenceMultiplier", rule.confidenceMultiplier.toDouble())
            put("successCount", rule.successCount)
            put("failureCount", rule.failureCount)
            put("lastUsed", rule.lastUsed)
            put("averageLatencyMs", rule.averageLatencyMs.toDouble())
        })
    }
    prefs.edit()
        .putString(KEY_ROUTING_RULES, rulesArray.toString())
        .apply()
}
```

#### Analytics Persistence

```kotlin
private fun saveAnalytics() {
    prefs.edit()
        .putLong(KEY_FALLBACK_ATTEMPTS, fallbackAttempts)
        .putLong(KEY_SUCCESSFUL_ROUTES, successfulRoutes)
        .putLong(KEY_FAILED_ROUTES, failedRoutes)
        .apply()
}

fun persistState() {
    saveRoutingRules()
    saveAnalytics()
}

fun getFeedbackCount(): Int = feedbackHistory.size
```

#### Feedback History Management

```kotlin
private fun cleanupFeedbackHistory() {
    if (feedbackHistory.size > MAX_FEEDBACK_HISTORY) {
        // Remove oldest entries
        val toRemove = feedbackHistory.size - MAX_FEEDBACK_HISTORY
        feedbackHistory.entries
            .sortedBy { it.value.timestamp }
            .take(toRemove)
            .forEach { feedbackHistory.remove(it.key) }
    }
}
```

---

## Testing Guidelines

### Unit Testing

#### Test Structure
```kotlin
class DeviceManagerTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var deviceManager: DeviceManager
    private val mockContext = mockk<Context>()
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        deviceManager = DeviceManager(mockContext)
    }
    
    @Test
    fun `getDeviceInfo returns correct device type`() {
        // Given
        every { mockContext.resources } returns mockk()
        
        // When
        val info = deviceManager.getDeviceInfo()
        
        // Then
        assertThat(info.deviceType).isNotNull()
    }
}
```

#### Coroutine Testing
```kotlin
@ExperimentalCoroutinesApi
class SpeechManagerTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()
    
    @Test
    fun `startRecognition emits correct states`() = runTest {
        // Given
        val manager = SpeechManager()
        val states = mutableListOf<RecognitionState>()
        
        // When
        val job = launch {
            manager.state.toList(states)
        }
        manager.startRecognition()
        
        // Then
        assertThat(states).contains(RecognitionState.LISTENING)
        job.cancel()
    }
}
```

### Integration Testing

```kotlin
@MediumTest
@RunWith(AndroidJUnit4::class)
class ModuleIntegrationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)
    
    @Test
    fun `modules communicate correctly`() {
        activityRule.scenario.onActivity { activity ->
            // Test inter-module communication
            val deviceManager = DeviceManager.getInstance(activity)
            val speechManager = SpeechManager(activity)
            
            // Verify integration
            assertThat(deviceManager.isInitialized).isTrue()
            assertThat(speechManager.isReady).isTrue()
        }
    }
}
```

### UI Testing

```kotlin
@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun `voice command triggers correct action`() {
        // Simulate voice input
        onView(withId(R.id.mic_button)).perform(click())
        
        // Verify UI response
        onView(withId(R.id.status_text))
            .check(matches(withText("Listening...")))
        
        // Simulate command
        simulateVoiceCommand("open settings")
        
        // Verify navigation
        intended(hasComponent(SettingsActivity::class.java.name))
    }
}
```

---

## Performance Optimization

### Memory Optimization

#### Leak Detection
```kotlin
class MemoryLeakDetector {
    fun detectLeaks() {
        if (BuildConfig.DEBUG) {
            // Use LeakCanary
            LeakCanary.config = LeakCanary.config.copy(
                dumpHeap = true,
                retainedVisibleThreshold = 3
            )
        }
    }
}
```

#### Memory Profiling
```kotlin
// Track allocations
class AllocationTracker {
    private val allocations = mutableMapOf<String, Int>()
    
    inline fun <T> track(tag: String, block: () -> T): T {
        val before = Runtime.getRuntime().totalMemory()
        val result = block()
        val after = Runtime.getRuntime().totalMemory()
        
        allocations[tag] = (after - before).toInt()
        return result
    }
}
```

### CPU Optimization

#### Profiling
```kotlin
@OptIn(ExperimentalTime::class)
inline fun <T> measureTimeMillis(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - start
    return result to duration
}

// Usage
val (result, time) = measureTimeMillis {
    processVoiceCommand(command)
}
Log.d("Performance", "Command processed in ${time}ms")
```

#### Thread Management
```kotlin
object ThreadPools {
    val IO = Executors.newFixedThreadPool(4)
    val COMPUTATION = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )
    val SINGLE = Executors.newSingleThreadExecutor()
}
```

### Battery Optimization

```kotlin
class BatteryOptimizer {
    fun optimizeSensorUsage() {
        if (isInDozeMode()) {
            // Reduce sensor sampling rate
            sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }
    
    fun batchNetworkRequests() {
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "sync",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(
                    15, TimeUnit.MINUTES
                ).build()
            )
    }
}
```

---

## Debugging Guide

### Logging Strategy

```kotlin
object Logger {
    private const val TAG = "VOS4"
    
    inline fun d(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }
    
    inline fun e(throwable: Throwable, message: () -> String) {
        Log.e(TAG, message(), throwable)
        
        if (!BuildConfig.DEBUG) {
            // Send to crash reporting
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }
}
```

### ADB Commands

```bash
# Logcat filtering
adb logcat -s VOS4:V

# Clear app data
adb shell pm clear com.augmentalis.vos4

# Grant permissions
adb shell pm grant com.augmentalis.vos4 android.permission.RECORD_AUDIO

# Performance profiling
adb shell dumpsys gfxinfo com.augmentalis.vos4

# Memory info
adb shell dumpsys meminfo com.augmentalis.vos4

# Battery stats
adb shell dumpsys batterystats --charged com.augmentalis.vos4
```

### Remote Debugging

```kotlin
class RemoteDebugger {
    fun enableDebugMode() {
        if (BuildConfig.DEBUG) {
            // Enable Stetho for Chrome DevTools
            Stetho.initializeWithDefaults(context)
            
            // Enable Flipper
            val client = AndroidFlipperClient.getInstance(context)
            client.addPlugin(InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()))
            client.addPlugin(NetworkFlipperPlugin())
            client.start()
        }
    }
}
```

---

## Release Process

### Pre-Release Checklist

- [ ] All tests passing
- [ ] No memory leaks detected
- [ ] Performance benchmarks met
- [ ] ProGuard rules updated
- [ ] Version numbers updated
- [ ] CHANGELOG updated
- [ ] Documentation current
- [ ] Security scan completed

### Build Configuration

```kotlin
// app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH"))
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Release Build

```bash
# Clean build
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Build AAB for Play Store
./gradlew bundleRelease

# Run release tests
./gradlew testReleaseUnitTest
./gradlew connectedReleaseAndroidTest
```

### Version Management

```kotlin
// version.gradle.kts
ext {
    set("versionCode", 40000)
    set("versionName", "4.0.0")
    set("minSdkVersion", 28)
    set("targetSdkVersion", 34)
    set("compileSdkVersion", 34)
}
```

---

## Troubleshooting

### Common Issues

#### Build Failures

**Issue**: `Unresolved reference` errors
```bash
# Solution: Clear caches and rebuild
./gradlew clean
rm -rf ~/.gradle/caches
./gradlew build --refresh-dependencies
```

**Issue**: Duplicate class errors
```groovy
// Solution: Exclude duplicates in build.gradle
android {
    packagingOptions {
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/LICENSE'
    }
}
```

#### Runtime Issues

**Issue**: `ClassNotFoundException`
```proguard
# Solution: Add to ProGuard rules
-keep class com.augmentalis.** { *; }
```

**Issue**: AIDL service not binding
```kotlin
// Solution: Check service declaration in manifest
<service
    android:name=".MyService"
    android:exported="true"
    android:process=":remote">
    <intent-filter>
        <action android:name="com.augmentalis.action.BIND" />
    </intent-filter>
</service>
```

#### Performance Issues

**Issue**: Slow app startup
```kotlin
// Solution: Lazy initialization
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Defer heavy initialization
        GlobalScope.launch(Dispatchers.IO) {
            initializeHeavyComponents()
        }
    }
}
```

**Issue**: Memory leaks
```kotlin
// Solution: Use weak references
class MyCallback(activity: Activity) {
    private val activityRef = WeakReference(activity)
    
    fun onEvent() {
        activityRef.get()?.let { activity ->
            // Use activity
        }
    }
}
```

### Debug Tools

- **Android Studio Profiler**: CPU, Memory, Network monitoring
- **Layout Inspector**: UI hierarchy debugging
- **Database Inspector**: SQLite/Room debugging
- **Logcat**: Real-time logging
- **ADB**: Command-line debugging
- **Stetho**: Chrome DevTools integration
- **Flipper**: Desktop debugging platform
- **LeakCanary**: Memory leak detection

---

## Resources

### Documentation
- [Android Developers](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs)
- [Material Design](https://material.io)

### Libraries
- [AndroidX](https://developer.android.com/jetpack/androidx)
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt](https://dagger.dev/hilt)

### Tools
- [Android Studio](https://developer.android.com/studio)
- [Gradle](https://gradle.org)
- [Git](https://git-scm.com)

### Community
- [Stack Overflow](https://stackoverflow.com/questions/tagged/android)
- [Reddit r/androiddev](https://www.reddit.com/r/androiddev)
- [Android Developers Blog](https://android-developers.googleblog.com)
