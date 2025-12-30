# VOS4 Developer Manual

**Last Updated:** 2025-12-03
**Version:** 2.0

## Table of Contents

### Getting Started
1. [Getting Started](#getting-started)
2. [Development Environment Setup](#development-environment-setup)
3. [Module Development](#module-development)

### Core Development
4. [Kotlin Coding Standards](#kotlin-coding-standards)
5. [Testing Guidelines](#testing-guidelines)
6. [Performance Optimization](#performance-optimization)
7. [Debugging Guide](#debugging-guide)

### UI Development
8. [Back Navigation Implementation](/docs/manuals/developer/ui/back-navigation-implementation-251203.md) ⭐ NEW
   - Scaffold with TopAppBar pattern
   - BackHandler integration
   - Material Design 3 theming
   - Accessibility optimizations

### Features
9. [Manual Command Assignment (VOS-META-001)](/docs/manuals/developer/features/manual-command-assignment-implementation-251203.md) ⭐ NEW
   - Database foundation (Phase 1)
   - Speech recognition UI (Phase 2)
   - Quality scoring system
   - Synonym management

### Release & Operations
10. [Release Process](#release-process)
11. [Troubleshooting](#troubleshooting)

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

---

## Kotlin Coding Standards

### Null Safety Patterns

**CRITICAL:** Never use force unwrap (`!!`) in production code. Use these safe patterns instead:

#### Pattern 1: Local Value Capture (Cache Access)
```kotlin
// ❌ WRONG - Unsafe
if (cachedValue != null) {
    return cachedValue!!
}

// ✅ CORRECT - Safe
val cached = cachedValue
if (cached != null) {
    return cached
}
```

#### Pattern 2: Elvis with Early Return
```kotlin
// ❌ WRONG - Unsafe
val result = field!!
processResult(result)

// ✅ CORRECT - Safe
val result = field ?: return
processResult(result)

// ✅ CORRECT - With logging
val result = field ?: run {
    Log.w(TAG, "Field is null")
    return
}
```

#### Pattern 3: Elvis with Also (Lazy Init)
```kotlin
// ❌ WRONG - Unsafe
if (_service == null) {
    _service = createService()
}
return _service!!

// ✅ CORRECT - Safe
return _service ?: createService().also { _service = it }
```

#### Pattern 4: requireNotNull with Return Value
```kotlin
// ❌ WRONG - Discards return
requireNotNull(field) { "Field required" }
doSomething(field!!)

// ✅ CORRECT - Captures return
val value = requireNotNull(field) { "Field required" }
doSomething(value)
```

#### Pattern 5: Result Unwrapping
```kotlin
// ❌ WRONG - Unsafe after isFailure check
if (result.isFailure) {
    return Result.failure(result.exceptionOrNull()!!)
}

// ✅ CORRECT - Use getOrThrow
if (result.isFailure) {
    return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
}

// ✅ BETTER - Use getOrThrow directly
val value = result.getOrThrow()
```

#### Pattern 6: Self-Referential Runnables
```kotlin
// ❌ WRONG - Uses !! in self-reference
runnable = Runnable {
    doWork()
    handler.postDelayed(runnable!!, DELAY)
}
handler.post(runnable!!)

// ✅ CORRECT - Object with 'this' reference
val runnable = object : Runnable {
    override fun run() {
        doWork()
        handler.postDelayed(this, DELAY)
    }
}
this.runnable = runnable
handler.post(runnable)
```

### Map and Collection Access

```kotlin
// ❌ WRONG - Unsafe map access
val value = map[key]!!

// ✅ CORRECT - With elvis
val value = map[key] ?: defaultValue

// ✅ CORRECT - With containsKey check + capture
val value = map[key]
if (value != null) {
    processValue(value)
}
```

### Compose UI Patterns

```kotlin
// ❌ WRONG - Using !! in Compose
if (stats != null) {
    Text(text = stats!!.count.toString())
}

// ✅ CORRECT - Local capture for smart cast
val s = stats
if (s != null) {
    Text(text = s.count.toString())
}

// ✅ CORRECT - With let
stats?.let { s ->
    Text(text = s.count.toString())
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
