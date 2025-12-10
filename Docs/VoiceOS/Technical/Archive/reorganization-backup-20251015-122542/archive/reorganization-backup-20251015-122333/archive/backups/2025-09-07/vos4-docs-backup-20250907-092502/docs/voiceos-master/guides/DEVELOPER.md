<!--
filename: DEVELOPER.md
created: 2025-01-23 20:40:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Complete development guide for VoiceOS 4.0
last-modified: 2025-01-23 20:40:00 PST
version: 1.0.0
-->

# VOS4 Developer Guide
> Complete development guide for VoiceOS 4.0
> Last Updated: 2025-01-23 20:40:00 PST (Migrated from docs-old)

**Note:** This document has been migrated from `/docs-old/` and updated for VOS4 architecture and current development status.

## Table of Contents
1. [Getting Started](#getting-started)
2. [Development Environment](#development-environment)
3. [Project Structure](#project-structure)
4. [Building the Project](#building-the-project)
5. [Module Development](#module-development)
6. [Debugging](#debugging)
7. [Testing](#testing)
8. [Contributing](#contributing)
9. [Code Standards](#code-standards)
10. [Troubleshooting](#troubleshooting)

## Getting Started

### Prerequisites
- **JDK 17** or higher
- **Android Studio Hedgehog** (2023.1.1) or later
- **Android SDK 34** (Android 14)
- **Gradle 8.5+**
- **Git**
- **8GB RAM minimum** (16GB recommended)
- **10GB free disk space**

### Initial Setup

```bash
# Clone the repository
git clone https://your-repo-url/VOS4.git
cd VOS4

# Switch to development branch
git checkout VOS4

# Build the project
./gradlew build

# Run tests
./gradlew test
```

## Development Environment

### Android Studio Configuration

1. **Import Project**
   - Open Android Studio
   - Select "Import Project"
   - Navigate to VOS4 directory
   - Select build.gradle.kts

2. **SDK Configuration**
   - File → Project Structure → SDK Location
   - Set Android SDK path
   - Ensure SDK 34 is installed

3. **Gradle Settings**
   - File → Settings → Build, Execution, Deployment → Build Tools → Gradle
   - Use Gradle from: gradle-wrapper.properties
   - Gradle JDK: JDK 17

### Environment Variables

```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

## Project Structure

```
VOS4/
├── app/                          # Main VoiceOS application
│   ├── src/main/java/           # Application code
│   │   └── VoiceOSApplication.kt # Main application class
│   └── build.gradle.kts         # App build configuration
│
├── apps/                         # Standalone applications
│   ├── VoiceAccessibility/      # Android accessibility service
│   ├── SpeechRecognition/       # Multi-engine STT (6 engines)
│   ├── VoiceUI/                 # Voice/spatial UI framework
│   └── DeviceMGR/               # Unified hardware management
│
├── managers/                     # System managers
│   ├── CoreMGR/                 # Module registry & lifecycle
│   ├── CommandsMGR/             # Command processing (70+ commands)
│   ├── DataMGR/                 # ObjectBox persistence
│   ├── GlassesMGR/              # Smart glasses integration
│   ├── LocalizationMGR/         # Multi-language support
│   ├── LicenseMGR/              # License & subscription
│   └── HUDManager/              # AR HUD system (90-120 FPS)
│
├── libraries/                    # Shared libraries
│   ├── VoiceUIElements/         # Pre-built UI components
│   └── UUIDManager/             # Unique identifier management
│
├── docs/                        # Documentation
├── gradle/                      # Gradle wrapper
├── build.gradle.kts             # Root build file
├── settings.gradle.kts          # Project settings
└── gradle.properties            # Gradle properties
```

## Building the Project

### Build Commands

```bash
# Clean build
./gradlew clean build

# Build specific module
./gradlew :apps:VoiceAccessibility:build
./gradlew :managers:CommandsMGR:build

# Build main app
./gradlew :app:assembleDebug

# Build release APK
./gradlew :app:assembleRelease

# Install on device
./gradlew :app:installDebug

# Run all tests
./gradlew test

# Run specific module tests
./gradlew :managers:DataMGR:test
```

### Build Variants

- **Debug**: Development build with debugging enabled
- **Release**: Optimized production build
- **Benchmark**: Performance testing build

### Build Configuration (VOS4 Pattern)

```kotlin
// In module's build.gradle.kts
android {
    namespace = "com.ai.modulename"  // VOS4: com.ai.* pattern
    compileSdk = 34
    
    defaultConfig {
        minSdk = 28        // Android 9
        targetSdk = 34     // Android 14
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}
```

## Module Development

### Creating a New Module (VOS4 Direct Implementation Pattern)

1. **Create Module Structure**
```bash
# Create module directory
mkdir -p apps/mymodule/src/main/java/com/ai/mymodule
mkdir -p apps/mymodule/src/main/res
mkdir -p apps/mymodule/src/test/java
```

2. **Create build.gradle.kts**
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("io.objectbox") // VOS4: Mandatory ObjectBox
}

android {
    namespace = "com.ai.mymodule"  // VOS4: com.ai.* namespace
    compileSdk = 34
    
    defaultConfig {
        minSdk = 28
        targetSdk = 34
    }
}

dependencies {
    // VOS4: Direct dependencies, no core interfaces
    implementation(project(":managers:DataMGR"))
    implementation(project(":libraries:UUIDManager"))
    
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("io.objectbox:objectbox-android:3.7.1")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.7")
}
```

3. **Implement Direct Module Pattern (VOS4)**
```kotlin
package com.ai.mymodule

import android.content.Context

// VOS4: Direct implementation, no interfaces
class MyModule(private val context: Context) {
    private var isInitialized = false
    
    suspend fun initialize(): Boolean {
        return try {
            // Initialize module components
            isInitialized = true
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun shutdown() {
        // Cleanup resources
        isInitialized = false
    }
    
    fun isReady(): Boolean = isInitialized
    
    // Direct methods without interface abstraction
    fun processData(data: String): Result {
        // Direct processing
    }
}
```

4. **Add ObjectBox Entities (Mandatory)**
```kotlin
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class MyEntity(
    @Id var id: Long = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

5. **Add to settings.gradle.kts**
```kotlin
include(":apps:mymodule")
```

### VOS4 Module Architecture Patterns

#### 1. Direct Repository Pattern (No Interfaces)
```kotlin
class MyRepository(private val box: Box<MyEntity>) {
    suspend fun getAll(): List<MyEntity> = withContext(Dispatchers.IO) {
        box.all
    }
    
    suspend fun insert(entity: MyEntity): Long = withContext(Dispatchers.IO) {
        box.put(entity)
    }
    
    suspend fun update(entity: MyEntity) = withContext(Dispatchers.IO) {
        box.put(entity)
    }
    
    suspend fun delete(entity: MyEntity) = withContext(Dispatchers.IO) {
        box.remove(entity.id)
    }
}
```

#### 2. Direct Handler Pattern (Commands)
```kotlin
class MyCommandHandler {
    fun invoke(command: Command): CommandResult {
        // Direct command processing
        return CommandResult(success = true, message = "Processed")
    }
}

// Registration with direct invoke
actionRegistry["my_command"] = MyCommandHandler()::invoke
```

#### 3. Direct Access Pattern (Configuration)
```kotlin
// VOS4: Direct access, no helpers
val muteCommand = config?.muteCommand ?: "mute ava"
val language = config?.language ?: "en-US"

// ❌ WRONG: Helper methods (zero overhead principle)
fun getParameter(name: String) = parameters[name]  // NO!
```

### Module Communication (VOS4)

#### Direct Module Access
```kotlin
// Get module reference directly
class MyConsumer(private val context: Context) {
    private val dataModule = DataModule(context)
    
    fun doSomething() {
        val result = dataModule.getData()
    }
}
```

#### Event-Based Communication
```kotlin
// Publishing events
EventBus.post(MyEvent(data))

// Subscribing to events
class MyObserver {
    private val subscription = EventBus.subscribe<MyEvent> { event ->
        // Handle event
    }
    
    fun cleanup() {
        EventBus.unsubscribe(subscription)
    }
}
```

## Debugging

### Logging (VOS4)

```kotlin
import android.util.Log

class MyModule {
    companion object {
        private const val TAG = "MyModule"
    }
    
    fun doSomething() {
        Log.d(TAG, "Debug message")
        Log.i(TAG, "Info message")
        Log.w(TAG, "Warning message")
        Log.e(TAG, "Error message", exception)
    }
}
```

### HUDManager Development (NEW)

#### Quick Start
```kotlin
// Initialize HUDManager
val hudManager = HUDManager.getInstance(context)
hudManager.initialize()

// Show AR notification
hudManager.showSpatialNotification(
    HUDNotification(
        message = "Hello AR World",
        priority = NotificationPriority.NORMAL,
        position = SpatialPosition.CENTER
    )
)

// Show localized notification (42+ languages)
hudManager.showLocalizedNotification(
    translationKey = "hud.notification.incoming_call",
    args = arrayOf("John Doe")
)
```

#### System API Integration
```kotlin
// Via Intent (for third-party apps)
val intent = Intent("com.augmentalis.voiceos.ACTION_SHOW_NOTIFICATION")
intent.putExtra("message", "New notification")
intent.putExtra("position", "TOP_CENTER")
context.sendBroadcast(intent)

// Via ContentProvider
val uri = Uri.parse("content://com.augmentalis.voiceos.hud.provider/elements")
val values = ContentValues().apply {
    put("type", "NOTIFICATION")
    put("message", "Task completed")
}
contentResolver.insert(uri, values)
```

#### Performance Requirements
- **Target FPS**: 90-120 FPS for AR displays
- **Memory**: < 50MB usage
- **Initialization**: < 500ms
- **Battery**: < 2% per hour

See `/docs/modules/HUDManager/` for complete documentation.

### Android Studio Debugger

1. **Set Breakpoints**: Click line numbers in editor
2. **Run Debug Mode**: Click debug icon or Shift+F9
3. **Inspect Variables**: Use Variables panel
4. **Step Through Code**: F8 (step over), F7 (step into)
5. **Evaluate Expression**: Alt+F8

### ADB Commands

```bash
# View logs
adb logcat | grep "MyModule"

# Clear logs
adb logcat -c

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Uninstall app
adb uninstall com.augmentalis.voiceos

# Start activity
adb shell am start -n com.augmentalis.voiceos/.MainActivity

# View accessibility service status
adb shell settings get secure enabled_accessibility_services
```

### Memory Profiling

1. **Android Studio Profiler**
   - View → Tool Windows → Profiler
   - Select device and app
   - Monitor CPU, Memory, Network, Energy

2. **ObjectBox Database Browser**
   - Use ObjectBox Data Browser for database inspection
   - Monitor entity counts and memory usage

## Testing

### Unit Testing (VOS4)

```kotlin
// In src/test/java
class MyModuleTest {
    private lateinit var module: MyModule
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = mockk<Context>()
        module = MyModule(context)
    }
    
    @Test
    fun `test module initialization`() = runTest {
        val result = module.initialize()
        assertTrue(result)
        assertTrue(module.isReady())
    }
    
    @Test
    fun `test direct command processing`() = runTest {
        val command = Command("test", listOf("test pattern"), confidence = 0.8f)
        val result = module.processCommand(command)
        assertTrue(result.success)
    }
}
```

### Integration Testing

```kotlin
// In src/androidTest/java
@RunWith(AndroidJUnit4::class)
class MyModuleIntegrationTest {
    @get:Rule
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    @Test
    fun testModuleIntegration() = runTest {
        val module = MyModule(context)
        val dataModule = DataModule(context)
        
        module.initialize()
        dataModule.initialize()
        
        // Test cross-module communication
        val result = module.processWithData(dataModule.getData())
        assertNotNull(result)
    }
}
```

### UI Testing (VOS4 with Compose)

```kotlin
@Test
fun testVoiceUIComponent() {
    composeTestRule.setContent {
        VoiceUITheme {
            MyVoiceComponent()
        }
    }
    
    composeTestRule
        .onNodeWithText("Voice Command")
        .performClick()
    
    composeTestRule
        .onNodeWithText("Command Executed")
        .assertIsDisplayed()
}
```

## Contributing

### Git Workflow

1. **Create Feature Branch**
```bash
git checkout -b feature/module-name
```

2. **Make Changes**
```bash
# Add files
git add .

# Commit with descriptive message
git commit -m "feat: Add new feature to module"
```

3. **Push Changes**
```bash
git push origin feature/module-name
```

4. **Create Pull Request**
   - Go to repository
   - Create PR from feature branch to VOS4 branch
   - Add description and reviewers

### Commit Message Format (VOS4)

```
<type>: <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style
- `refactor`: Code refactoring
- `test`: Testing
- `chore`: Maintenance

**Example:**
```
feat: Add voice command for DeviceMGR audio control

- Implement direct audio command processing
- Add volume control with accessibility integration
- Support 70+ voice commands with ::invoke pattern

Fixes #123
```

## Code Standards (VOS4)

### Kotlin Style Guide

```kotlin
// Package name - lowercase with com.ai.* pattern
package com.ai.mymodule

// Imports - alphabetical order
import android.content.Context
import android.util.Log
import kotlinx.coroutines.launch

// Class documentation
/**
 * MyModule provides functionality for...
 * 
 * VOS4: Direct implementation without interfaces
 * @property context Android context
 * @constructor Creates a new MyModule instance
 */
class MyModule(private val context: Context) {
    
    // Constants - UPPER_SNAKE_CASE
    companion object {
        private const val TAG = "MyModule"
        const val MAX_RETRIES = 3
    }
    
    // Properties - camelCase
    private var isInitialized = false
    private val repository by lazy { MyRepository(objectBox.boxFor()) }
    
    // Functions - camelCase with direct implementation
    /**
     * Initializes the module
     * @return true if successful
     */
    suspend fun initialize(): Boolean {
        return try {
            // Implementation
            true
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            false
        }
    }
    
    // Direct parameter access (VOS4 principle)
    fun processCommand(text: String, confidence: Float = 0.8f): CommandResult {
        // Direct processing, no helper methods
        return CommandResult(success = true)
    }
}
```

### VOS4 Architecture Guidelines

1. **Direct Implementation**: No interfaces unless absolutely necessary
2. **Zero Overhead**: Direct parameter access, no helper methods
3. **ObjectBox Mandatory**: All data persistence through ObjectBox
4. **Namespace Compliance**: com.ai.* for all modules
5. **Self-Containment**: All components in same module
6. **XR Ready**: All UI components compatible with Android XR

## Troubleshooting

### Common Issues

#### 1. Build Failures

**Problem**: Gradle build fails
```
Execution failed for task ':apps:MyModule:compileDebugKotlin'
```

**Solution**:
```bash
# Clean and rebuild
./gradlew clean build

# Invalidate caches in Android Studio
# File → Invalidate Caches and Restart
```

#### 2. Module Not Found (VOS4)

**Problem**: Module not recognized
```
Unresolved reference: MyModule
```

**Solution**:
- Check settings.gradle.kts includes module
- Verify namespace follows com.ai.* pattern
- Sync project with Gradle
- Check direct imports (no interface layers)

#### 3. ObjectBox Issues

**Problem**: ObjectBox entity errors

**Solution**:
```kotlin
// Ensure proper entity annotation
@Entity
data class MyEntity(
    @Id var id: Long = 0,  // Must be var with default 0
    val name: String
)

// Clean and rebuild after entity changes
./gradlew clean build
```

#### 4. Memory Issues (VOS4)

**Problem**: OutOfMemoryError during build

**Solution**:
```gradle
# In gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m

# Check DeviceMGR consolidation working correctly
# Monitor memory usage in Profiler
```

#### 5. Command Processing Issues

**Problem**: Commands not executing

**Solution**:
```kotlin
// Check direct handler assignment
actionRegistry["my_command"] = MyCommandHandler()::invoke

// Verify patterns not phrases
val patterns = listOf("my command", "execute my action")

// Check confidence threshold
if (command.confidence >= 0.7f) {
    // Process command
}
```

### Debug Tips (VOS4)

1. **Enable Verbose Logging**
```kotlin
if (BuildConfig.DEBUG) {
    Log.v(TAG, "VOS4 verbose logging enabled")
}
```

2. **Use ObjectBox Browser**
```kotlin
// View ObjectBox database
// Download ObjectBox Browser from docs.objectbox.io
```

3. **Profile Performance**
```kotlin
measureTimeMillis {
    // Code to measure
}.also { time ->
    Log.d(TAG, "VOS4 operation took $time ms")
}
```

4. **Test Accessibility Service**
```bash
# Check service status
adb shell settings get secure enabled_accessibility_services

# Enable service
adb shell settings put secure enabled_accessibility_services com.ai.voiceaccessibility/.AccessibilityService
```

## Resources

### Documentation
- [Android Developers](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [ObjectBox Docs](https://docs.objectbox.io)
- [Android XR Documentation](https://developer.android.com/xr)

### VOS4 Libraries Used
- **Jetpack Compose**: UI framework with XR support
- **Coroutines**: Async programming
- **ObjectBox**: Database (mandatory)
- **Vosk**: Offline STT engine
- **Vivoka**: Cloud STT engine
- **Material3**: Design system
- **Android XR**: Spatial computing framework

### Tools
- **Android Studio**: IDE
- **ADB**: Android Debug Bridge
- **Gradle**: Build system
- **Git**: Version control
- **ObjectBox Browser**: Database inspection
- **Android XR Emulator**: XR testing

## Support

### Contact
- **Author**: Manoj Jhawar
- **Team**: Augmentalis Inc
- **Repository**: VOS4
- **Architecture**: VOS4 Direct Implementation

### Reporting Issues
1. Check existing issues
2. Create detailed bug report with VOS4 context
3. Include logs and steps to reproduce
4. Specify module and namespace
5. Tag with appropriate labels

### Getting Help
- Review VOS4 documentation
- Check FAQ section
- Follow VOS4 architecture principles
- Test with direct implementation pattern

---

*Migrated from docs-old/DEVELOPER.md*  
*Updated for VOS4 architecture*  
*Last Updated: 2025-01-23*