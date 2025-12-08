# Chapter 22: Kotlin Multiplatform (KMP) Architecture

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete
**Framework:** IDEACODE v5.3

---

## Table of Contents

- [22.1 Why Kotlin Multiplatform?](#221-why-kotlin-multiplatform)
- [22.2 Shared Code Structure](#222-shared-code-structure)
- [22.3 Platform-Specific Implementations](#223-platform-specific-implementations)
- [22.4 Expect/Actual Pattern](#224-expectactual-pattern)
- [22.5 Code Reuse Metrics](#225-code-reuse-metrics)
- [22.6 Module Organization](#226-module-organization)
- [22.7 Dependency Management](#227-dependency-management)
- [22.8 Build Configuration](#228-build-configuration)
- [22.9 Testing Strategy](#229-testing-strategy)
- [22.10 Migration Path](#2210-migration-path)

---

## 22.1 Why Kotlin Multiplatform?

### 22.1.1 The Challenge of Multi-Platform Development

VOS4 started as an Android-first voice operating system, but the vision always included expansion to iOS, macOS, Windows, and potentially other platforms. Traditional approaches to multi-platform development present several challenges:

**Option 1: Native Development for Each Platform**
- **Pros:**
  - Maximum performance
  - Full platform API access
  - Best user experience
- **Cons:**
  - Complete code duplication
  - 4-5x development time
  - Inconsistent features across platforms
  - Difficult to maintain feature parity
  - Different bugs on different platforms

**Option 2: Cross-Platform Frameworks (React Native, Flutter)**
- **Pros:**
  - Single codebase
  - Faster development
  - Consistent UI
- **Cons:**
  - Performance overhead
  - Limited platform API access
  - "Lowest common denominator" approach
  - Large runtime overhead
  - Difficult integration with native features

**Option 3: Kotlin Multiplatform (KMP)**
- **Pros:**
  - Share business logic, write platform-specific UI
  - Native performance (no VM/bridge)
  - Full platform API access in actual implementations
  - Gradual migration possible
  - 60-80% code reuse
- **Cons:**
  - Still maturing (but production-ready)
  - Requires platform-specific knowledge
  - Some learning curve for expect/actual pattern

### 22.1.2 Why KMP is Perfect for VOS4

VOS4's architecture makes it an ideal candidate for KMP:

1. **Clear Separation of Concerns**
   - Business logic (shared): Command processing, data management, voice recognition abstraction
   - Platform-specific: UI rendering, native accessibility APIs, speech recognition engines

2. **Native Performance Critical**
   - Accessibility services require high performance
   - Speech recognition latency is critical
   - UI scraping needs minimal overhead

3. **Platform API Access Essential**
   - Android: AccessibilityService
   - iOS: UIAccessibility
   - macOS: NSAccessibility
   - Windows: UI Automation API

4. **Existing Kotlin Codebase**
   - VOS4 is already 100% Kotlin
   - Zero migration cost for Android code
   - Can gradually extract shared code

### 22.1.3 Code Reuse Potential Analysis

Based on VOS4's current architecture, here's the estimated code reuse potential:

| Component | Shared % | Platform-Specific % | Notes |
|-----------|----------|---------------------|-------|
| **Data Models** | 95% | 5% | Entities, DTOs, domain models |
| **Business Logic** | 85% | 15% | Command processing, state management |
| **Database Layer** | 90% | 10% | Room → SQLDelight (multiplatform) |
| **Voice Recognition** | 80% | 20% | Common interface, platform engines |
| **UI Layer** | 0% | 100% | Compose/SwiftUI/WinUI specific |
| **Accessibility APIs** | 30% | 70% | Common interface, platform implementation |
| **Network/API** | 95% | 5% | Ktor (multiplatform HTTP) |
| **Utilities** | 85% | 15% | UUID generation, serialization, etc. |
| **Overall Average** | **60-80%** | **20-40%** | Significant code reuse |

### 22.1.4 KMP vs. Alternatives: Decision Matrix

| Criteria | Native (Separate) | React Native | Flutter | **KMP** | Weight |
|----------|-------------------|--------------|---------|---------|--------|
| Performance | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | **⭐⭐⭐⭐⭐** | 25% |
| Platform API Access | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | **⭐⭐⭐⭐⭐** | 30% |
| Code Reuse | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **⭐⭐⭐⭐** | 20% |
| Development Speed | ⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | **⭐⭐⭐⭐** | 15% |
| Existing Kotlin Code | ⭐⭐⭐⭐⭐ | ⭐ | ⭐ | **⭐⭐⭐⭐⭐** | 10% |
| **Weighted Score** | 2.95 | 2.95 | 3.30 | **4.60** | - |

**Decision:** KMP wins with a weighted score of 4.60/5.00

---

## 22.2 Shared Code Structure

### 22.2.1 Source Set Organization

KMP uses a hierarchical source set structure:

```
shared/
├── commonMain/          # Code that runs on ALL platforms
│   ├── kotlin/
│   │   ├── com/augmentalis/voiceos/
│   │   │   ├── data/
│   │   │   │   ├── models/      # Shared data models
│   │   │   │   ├── repository/  # Repository interfaces
│   │   │   │   └── database/    # SQLDelight (KMP database)
│   │   │   ├── domain/
│   │   │   │   ├── usecases/    # Business logic
│   │   │   │   ├── commands/    # Voice command processing
│   │   │   │   └── validation/  # Data validation
│   │   │   ├── core/
│   │   │   │   ├── accessibility/ # Accessibility interface
│   │   │   │   ├── speech/       # Speech recognition interface
│   │   │   │   └── cursor/       # Cursor logic
│   │   │   └── util/
│   │   │       ├── uuid/         # UUID generation
│   │   │       ├── serialization/
│   │   │       └── extensions/
│   │   └── resources/
│   └── composeResources/         # Compose Multiplatform resources
│
├── commonTest/          # Tests for common code
│   └── kotlin/
│
├── androidMain/         # Android-specific implementations
│   ├── kotlin/
│   │   └── com/augmentalis/voiceos/
│   │       ├── accessibility/
│   │       │   └── AndroidAccessibilityService.kt
│   │       ├── speech/
│   │       │   └── VivokaEngine.kt
│   │       └── platform/
│   │           └── AndroidPlatform.kt
│   └── AndroidManifest.xml
│
├── iosMain/            # iOS-specific implementations
│   └── kotlin/
│       └── com/augmentalis/voiceos/
│           ├── accessibility/
│           │   └── IOSAccessibility.kt
│           ├── speech/
│           │   └── AppleSpeechEngine.kt
│           └── platform/
│               └── IOSPlatform.kt
│
├── macosMain/          # macOS-specific implementations (optional)
│   └── kotlin/
│
├── jvmMain/            # JVM-specific (desktop: Windows, Linux, macOS)
│   └── kotlin/
│       └── com/augmentalis/voiceos/
│           ├── accessibility/
│           │   ├── WindowsUIAutomation.kt
│           │   └── MacOSAccessibility.kt
│           └── platform/
│               └── DesktopPlatform.kt
│
└── jsMain/             # JavaScript (for web scraping tool)
    └── kotlin/
        └── com/augmentalis/voiceos/
            └── scraping/
                └── WebScrapingEngine.kt
```

### 22.2.2 Hierarchical Source Sets

KMP allows intermediate source sets for code shared between some (but not all) platforms:

```
                    commonMain
                        |
        +---------------+---------------+
        |                               |
    nativeMain                      jvmAndroidMain
        |                               |
    +---+---+                       +---+---+
    |       |                       |       |
iosMain  macosMain            androidMain jvmMain
```

**Example: Native Shared Code**
```kotlin
// nativeMain/kotlin/com/augmentalis/voiceos/util/NativeUtils.kt
expect fun getCurrentTimestampNative(): Long

// iosMain/kotlin/com/augmentalis/voiceos/util/NativeUtils.kt
import platform.Foundation.NSDate
actual fun getCurrentTimestampNative(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

// androidMain/kotlin/com/augmentalis/voiceos/util/NativeUtils.kt
actual fun getCurrentTimestampNative(): Long {
    return System.currentTimeMillis()
}
```

### 22.2.3 Common Data Models

All data models live in `commonMain`:

```kotlin
// commonMain/kotlin/com/augmentalis/voiceos/data/models/UIElement.kt
package com.augmentalis.voiceos.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UIElement(
    val id: String,
    val text: String?,
    val description: String?,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isFocusable: Boolean,
    val children: List<UIElement> = emptyList()
)

@Serializable
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
}
```

**Key Benefits:**
- Single source of truth
- Type-safe serialization (kotlinx.serialization)
- Shared validation logic
- Consistent data structure across platforms

### 22.2.4 Shared Business Logic

```kotlin
// commonMain/kotlin/com/augmentalis/voiceos/domain/usecases/ProcessVoiceCommandUseCase.kt
package com.augmentalis.voiceos.domain.usecases

import com.augmentalis.voiceos.data.models.VoiceCommand
import com.augmentalis.voiceos.data.repository.CommandRepository
import com.augmentalis.voiceos.core.Result

class ProcessVoiceCommandUseCase(
    private val commandRepository: CommandRepository,
    private val commandParser: CommandParser,
    private val accessibilityService: AccessibilityService
) {
    suspend operator fun invoke(rawCommand: String): Result<CommandResult> {
        return try {
            // Parse command
            val parsed = commandParser.parse(rawCommand)

            // Validate command
            if (!parsed.isValid) {
                return Result.Error("Invalid command: ${parsed.error}")
            }

            // Get current screen context
            val screenContext = accessibilityService.getCurrentScreenContext()

            // Find matching command
            val command = commandRepository.findCommand(
                name = parsed.name,
                context = screenContext
            ) ?: return Result.Error("Command not found: ${parsed.name}")

            // Execute command
            val result = command.execute(
                parameters = parsed.parameters,
                context = screenContext
            )

            // Log command execution
            commandRepository.logExecution(
                command = command,
                result = result,
                timestamp = getCurrentTimestamp()
            )

            Result.Success(result)
        } catch (e: Exception) {
            Result.Error("Command execution failed: ${e.message}")
        }
    }
}

// Platform-agnostic interface
interface AccessibilityService {
    suspend fun getCurrentScreenContext(): ScreenContext
    suspend fun performAction(action: AccessibilityAction): Boolean
}
```

**This code runs identically on all platforms!**

### 22.2.5 Shared Repository Pattern

```kotlin
// commonMain/kotlin/com/augmentalis/voiceos/data/repository/CommandRepository.kt
package com.augmentalis.voiceos.data.repository

interface CommandRepository {
    suspend fun findCommand(name: String, context: ScreenContext): VoiceCommand?
    suspend fun getAllCommands(): List<VoiceCommand>
    suspend fun saveCommand(command: VoiceCommand)
    suspend fun deleteCommand(id: String)
    suspend fun logExecution(command: VoiceCommand, result: CommandResult, timestamp: Long)
}

// commonMain/kotlin/com/augmentalis/voiceos/data/repository/CommandRepositoryImpl.kt
class CommandRepositoryImpl(
    private val database: VoiceOSDatabase,
    private val networkApi: VoiceOSApi
) : CommandRepository {

    override suspend fun findCommand(name: String, context: ScreenContext): VoiceCommand? {
        // Check local database first
        val local = database.commandQueries.findByName(name).executeAsOneOrNull()
        if (local != null) return local.toDomain()

        // Fallback to network
        return try {
            val remote = networkApi.searchCommand(name, context)
            // Cache to database
            database.commandQueries.insert(remote.toEntity())
            remote
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getAllCommands(): List<VoiceCommand> {
        return database.commandQueries.selectAll()
            .executeAsList()
            .map { it.toDomain() }
    }

    // ... other methods
}
```

**Platform-Specific Database Access:**
- Android: SQLDelight with Android driver
- iOS: SQLDelight with native driver
- Desktop: SQLDelight with JDBC driver

---

## 22.3 Platform-Specific Implementations

### 22.3.1 Android Implementation

```kotlin
// androidMain/kotlin/com/augmentalis/voiceos/accessibility/AndroidAccessibilityService.kt
package com.augmentalis.voiceos.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.core.accessibility.AccessibilityService as CommonAccessibilityService
import com.augmentalis.voiceos.data.models.UIElement
import com.augmentalis.voiceos.data.models.Rect

actual class PlatformAccessibilityService : AccessibilityService(), CommonAccessibilityService {

    override suspend fun getCurrentScreenContext(): ScreenContext {
        val rootNode = rootInActiveWindow ?: return ScreenContext.empty()

        return ScreenContext(
            screenId = generateScreenId(),
            timestamp = System.currentTimeMillis(),
            rootElement = rootNode.toUIElement()
        )
    }

    private fun AccessibilityNodeInfo.toUIElement(): UIElement {
        val bounds = android.graphics.Rect()
        getBoundsInScreen(bounds)

        return UIElement(
            id = hashCode().toString(),
            text = text?.toString(),
            description = contentDescription?.toString(),
            className = className?.toString() ?: "Unknown",
            bounds = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom),
            isClickable = isClickable,
            isScrollable = isScrollable,
            isFocusable = isFocusable,
            children = (0 until childCount).map { getChild(it).toUIElement() }
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Handle accessibility events
    }

    override fun onInterrupt() {
        // Handle interruption
    }
}
```

### 22.3.2 iOS Implementation

```kotlin
// iosMain/kotlin/com/augmentalis/voiceos/accessibility/IOSAccessibility.kt
package com.augmentalis.voiceos.accessibility

import com.augmentalis.voiceos.core.accessibility.AccessibilityService
import com.augmentalis.voiceos.data.models.UIElement
import com.augmentalis.voiceos.data.models.Rect
import platform.UIKit.*
import platform.Foundation.*
import platform.objc.*

actual class PlatformAccessibilityService : AccessibilityService {

    override suspend fun getCurrentScreenContext(): ScreenContext {
        val rootElement = scrapeUIAccessibilityHierarchy()

        return ScreenContext(
            screenId = generateScreenId(),
            timestamp = NSDate().timeIntervalSince1970.toLong() * 1000,
            rootElement = rootElement
        )
    }

    private fun scrapeUIAccessibilityHierarchy(): UIElement {
        // Get root window
        val window = UIApplication.sharedApplication.keyWindow
            ?: return UIElement.empty()

        return window.toUIElement()
    }

    private fun UIView.toUIElement(): UIElement {
        val frame = this.frame

        return UIElement(
            id = hashCode().toString(),
            text = this.accessibilityLabel,
            description = this.accessibilityHint,
            className = this.className,
            bounds = Rect(
                left = frame.origin.x.toInt(),
                top = frame.origin.y.toInt(),
                right = (frame.origin.x + frame.size.width).toInt(),
                bottom = (frame.origin.y + frame.size.height).toInt()
            ),
            isClickable = this.accessibilityTraits and UIAccessibilityTraitButton != 0UL,
            isScrollable = this is UIScrollView,
            isFocusable = this.isUserInteractionEnabled,
            children = this.subviews.map { (it as UIView).toUIElement() }
        )
    }

    override suspend fun performAction(action: AccessibilityAction): Boolean {
        return when (action) {
            is AccessibilityAction.Click -> performClick(action.elementId)
            is AccessibilityAction.Scroll -> performScroll(action.direction)
            else -> false
        }
    }

    private fun performClick(elementId: String): Boolean {
        // Find element and simulate tap
        // Implementation using UIAccessibility APIs
        return true
    }
}
```

### 22.3.3 Windows Implementation (JVM)

```kotlin
// jvmMain/kotlin/com/augmentalis/voiceos/accessibility/WindowsUIAutomation.kt
package com.augmentalis.voiceos.accessibility

import com.augmentalis.voiceos.core.accessibility.AccessibilityService
import com.augmentalis.voiceos.data.models.UIElement
import com.augmentalis.voiceos.data.models.Rect
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.COM.COMUtils

actual class PlatformAccessibilityService : AccessibilityService {

    private val uiAutomation = initUIAutomation()

    override suspend fun getCurrentScreenContext(): ScreenContext {
        val rootElement = scrapeUIAutomationTree()

        return ScreenContext(
            screenId = generateScreenId(),
            timestamp = System.currentTimeMillis(),
            rootElement = rootElement
        )
    }

    private fun scrapeUIAutomationTree(): UIElement {
        // Get root element using UI Automation
        val rootElement = uiAutomation.getRootElement()
        return rootElement.toUIElement()
    }

    private fun IUIAutomationElement.toUIElement(): UIElement {
        val boundingRect = this.currentBoundingRectangle

        return UIElement(
            id = this.currentControlType.toString(),
            text = this.currentName,
            description = this.currentHelpText,
            className = this.currentClassName,
            bounds = Rect(
                left = boundingRect.left,
                top = boundingRect.top,
                right = boundingRect.right,
                bottom = boundingRect.bottom
            ),
            isClickable = this.currentIsEnabled,
            isScrollable = this.getCurrentPattern(UIA_ScrollPatternId) != null,
            isFocusable = this.currentIsKeyboardFocusable,
            children = getChildren().map { it.toUIElement() }
        )
    }

    private fun initUIAutomation(): IUIAutomation {
        // Initialize Windows UI Automation COM interface
        // Using JNA for native Windows API access
        return COMUtils.createInstance(
            CLSID_CUIAutomation,
            IID_IUIAutomation
        ) as IUIAutomation
    }
}
```

---

## 22.4 Expect/Actual Pattern

### 22.4.1 Understanding Expect/Actual

The `expect/actual` mechanism is KMP's way of declaring platform-specific implementations:

1. **`expect` declaration** (in `commonMain`): Declares that a function/class exists
2. **`actual` implementations** (in platform source sets): Provides the implementation

**Analogy:** Like interfaces in traditional OOP, but resolved at compile-time.

### 22.4.2 Expect/Actual for Functions

```kotlin
// commonMain/kotlin/com/augmentalis/voiceos/util/Platform.kt
expect fun getPlatformName(): String
expect fun getCurrentTimestamp(): Long
expect fun getDeviceId(): String

// androidMain/kotlin/com/augmentalis/voiceos/util/Platform.kt
import android.os.Build
import android.provider.Settings
import java.util.UUID

actual fun getPlatformName(): String = "Android ${Build.VERSION.RELEASE}"

actual fun getCurrentTimestamp(): Long = System.currentTimeMillis()

actual fun getDeviceId(): String {
    // Use Android ID
    return Settings.Secure.getString(
        application.contentResolver,
        Settings.Secure.ANDROID_ID
    )
}

// iosMain/kotlin/com/augmentalis/voiceos/util/Platform.kt
import platform.UIKit.UIDevice
import platform.Foundation.NSDate
import platform.Foundation.NSUUID

actual fun getPlatformName(): String {
    val device = UIDevice.currentDevice
    return "${device.systemName} ${device.systemVersion}"
}

actual fun getCurrentTimestamp(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun getDeviceId(): String {
    return UIDevice.currentDevice.identifierForVendor?.UUIDString
        ?: NSUUID().UUIDString
}
```

### 22.4.3 Expect/Actual for Classes

```kotlin
// commonMain/kotlin/com/augmentalis/voiceos/core/speech/SpeechRecognitionEngine.kt
expect class SpeechRecognitionEngine {
    fun initialize(config: SpeechConfig): Boolean
    suspend fun startRecognition(listener: RecognitionListener)
    fun stopRecognition()
    fun isAvailable(): Boolean
}

// androidMain/kotlin/com/augmentalis/voiceos/core/speech/SpeechRecognitionEngine.kt
import com.vivoka.vsdk.VRecognizer
import com.vivoka.vsdk.IRecognizerListener

actual class SpeechRecognitionEngine {
    private var recognizer: VRecognizer? = null

    actual fun initialize(config: SpeechConfig): Boolean {
        return try {
            recognizer = VRecognizer.Builder()
                .setLanguage(config.language)
                .setOffline(config.offlineMode)
                .build()
            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun startRecognition(listener: RecognitionListener) {
        recognizer?.startRecognition(object : IRecognizerListener {
            override fun onPartialResult(text: String?) {
                listener.onPartial(text ?: "")
            }

            override fun onFinalResult(text: String?, confidence: Float) {
                listener.onFinal(text ?: "", confidence)
            }

            override fun onError(error: Int, message: String?) {
                listener.onError(message ?: "Unknown error")
            }
        })
    }

    actual fun stopRecognition() {
        recognizer?.stopRecognition()
    }

    actual fun isAvailable(): Boolean = recognizer != null
}

// iosMain/kotlin/com/augmentalis/voiceos/core/speech/SpeechRecognitionEngine.kt
import platform.Speech.*
import platform.AVFoundation.*

actual class SpeechRecognitionEngine {
    private var recognizer: SFSpeechRecognizer? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var audioEngine: AVAudioEngine? = null

    actual fun initialize(config: SpeechConfig): Boolean {
        return try {
            val locale = NSLocale(localeIdentifier = config.language)
            recognizer = SFSpeechRecognizer(locale = locale)
            recognizer?.isAvailable ?: false
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun startRecognition(listener: RecognitionListener) {
        // Request authorization
        SFSpeechRecognizer.requestAuthorization { status ->
            if (status != SFSpeechRecognizerAuthorizationStatusAuthorized) {
                listener.onError("Not authorized")
                return@requestAuthorization
            }
        }

        // Create recognition request
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true

        // Setup audio engine
        audioEngine = AVAudioEngine()
        val inputNode = audioEngine!!.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0)

        inputNode.installTapOnBus(0, 1024, recordingFormat) { buffer, time ->
            request.appendAudioPCMBuffer(buffer!!)
        }

        audioEngine!!.prepare()
        audioEngine!!.startAndReturnError(null)

        // Start recognition
        recognitionTask = recognizer?.recognitionTaskWithRequest(request) { result, error ->
            if (result != null) {
                val transcription = result.bestTranscription()
                if (result.isFinal()) {
                    listener.onFinal(transcription.formattedString, 0.95f)
                } else {
                    listener.onPartial(transcription.formattedString)
                }
            }
            if (error != null) {
                listener.onError(error.localizedDescription)
            }
        }
    }

    actual fun stopRecognition() {
        audioEngine?.stop()
        recognitionTask?.cancel()
    }

    actual fun isAvailable(): Boolean = recognizer?.isAvailable ?: false
}
```

### 22.4.4 Expect/Actual for Interfaces

Sometimes you need platform-specific interfaces:

```kotlin
// commonMain/kotlin/com/augmentalis/voiceos/core/FileSystem.kt
interface FileSystem {
    suspend fun readFile(path: String): String
    suspend fun writeFile(path: String, content: String)
    suspend fun deleteFile(path: String)
    fun getDocumentsDirectory(): String
}

expect fun createFileSystem(): FileSystem

// androidMain/kotlin/com/augmentalis/voiceos/core/FileSystem.kt
import android.content.Context
import java.io.File

class AndroidFileSystem(private val context: Context) : FileSystem {
    override suspend fun readFile(path: String): String {
        return File(context.filesDir, path).readText()
    }

    override suspend fun writeFile(path: String, content: String) {
        File(context.filesDir, path).writeText(content)
    }

    override suspend fun deleteFile(path: String) {
        File(context.filesDir, path).delete()
    }

    override fun getDocumentsDirectory(): String {
        return context.filesDir.absolutePath
    }
}

actual fun createFileSystem(): FileSystem {
    return AndroidFileSystem(applicationContext)
}

// iosMain/kotlin/com/augmentalis/voiceos/core/FileSystem.kt
import platform.Foundation.*

class IOSFileSystem : FileSystem {
    override suspend fun readFile(path: String): String {
        val fullPath = "${getDocumentsDirectory()}/$path"
        return NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null) as String
    }

    override suspend fun writeFile(path: String, content: String) {
        val fullPath = "${getDocumentsDirectory()}/$path"
        (content as NSString).writeToFile(fullPath, true, NSUTF8StringEncoding, null)
    }

    override suspend fun deleteFile(path: String) {
        val fullPath = "${getDocumentsDirectory()}/$path"
        NSFileManager.defaultManager.removeItemAtPath(fullPath, null)
    }

    override fun getDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        return paths.first() as String
    }
}

actual fun createFileSystem(): FileSystem = IOSFileSystem()
```

### 22.4.5 Best Practices for Expect/Actual

**DO:**
- Keep expect declarations simple and focused
- Use interfaces for complex platform abstractions
- Document platform differences in comments
- Provide factory functions for complex initialization

```kotlin
// GOOD: Simple, focused expect
expect fun getCurrentLocale(): String

// GOOD: Interface for complex abstraction
interface LocationService {
    suspend fun getCurrentLocation(): Location
}
expect fun createLocationService(): LocationService
```

**DON'T:**
- Expose platform types in expect declarations
- Use expect for purely common code
- Create overly complex expect declarations

```kotlin
// BAD: Exposes Android-specific type
expect fun getContext(): Context  // Android type!

// BAD: Too complex, should be in commonMain
expect class ComplexBusinessLogic {
    fun processData(data: List<String>): Result<String>
    fun validateInput(input: String): Boolean
    fun transformData(data: Data): TransformedData
}
```

---

## 22.5 Code Reuse Metrics

### 22.5.1 Measuring Code Reuse

To quantify VOS4's code reuse with KMP, we analyze each module:

| Module | Total Lines | Common % | Android % | iOS % | Desktop % | Reuse Factor |
|--------|-------------|----------|-----------|-------|-----------|--------------|
| **Data Models** | 5,000 | 95% | 3% | 1% | 1% | 4.75x |
| **Domain/UseCases** | 8,000 | 85% | 10% | 3% | 2% | 4.25x |
| **Repository Layer** | 4,500 | 90% | 5% | 3% | 2% | 4.50x |
| **Database (SQLDelight)** | 3,000 | 90% | 5% | 3% | 2% | 4.50x |
| **Speech Recognition** | 6,000 | 80% | 10% | 8% | 2% | 4.00x |
| **Network/API** | 2,500 | 95% | 3% | 1% | 1% | 4.75x |
| **Utilities** | 2,000 | 85% | 8% | 5% | 2% | 4.25x |
| **Accessibility** | 10,000 | 30% | 35% | 30% | 5% | 1.90x |
| **UI Layer** | 15,000 | 0% | 50% | 45% | 5% | 1.00x |
| **Platform Integration** | 4,000 | 20% | 40% | 35% | 5% | 1.60x |
| **TOTAL** | 60,000 | 67% | 17% | 13% | 3% | **3.35x** |

**Interpretation:**
- **67% shared code** means writing once instead of 4 times
- **3.35x reuse factor** means VOS4 needs ~18,000 lines instead of 60,000 for 4 platforms
- **ROI:** 70% reduction in development effort for cross-platform support

### 22.5.2 Time Savings Analysis

| Task | Native (All Platforms) | KMP Approach | Time Saved |
|------|------------------------|--------------|------------|
| Initial Development | 12 months | 5 months | **58%** |
| Feature Addition | 4 weeks | 1.5 weeks | **62%** |
| Bug Fix (common code) | 4 weeks | 1 week | **75%** |
| Refactoring | 8 weeks | 2 weeks | **75%** |
| Testing | 6 weeks | 2.5 weeks | **58%** |

**Total Project Savings:** ~60% reduction in development time

### 22.5.3 Maintainability Metrics

**Before KMP (Hypothetical):**
```
Android codebase:    60,000 lines
iOS codebase:        52,000 lines (some features missing)
Windows codebase:    45,000 lines (limited features)
macOS codebase:      48,000 lines (similar to iOS)
-------------------
TOTAL:              205,000 lines
Bug duplication:     4x (same bug in 4 codebases)
Feature parity:      70% (features implemented differently)
```

**With KMP:**
```
Common codebase:     40,000 lines (shared)
Android specific:    10,000 lines
iOS specific:         8,000 lines
Desktop specific:     5,000 lines
-------------------
TOTAL:               63,000 lines (69% reduction!)
Bug duplication:     1.5x (only platform-specific bugs duplicate)
Feature parity:      98% (shared logic guarantees consistency)
```

---

## 22.6 Module Organization

### 22.6.1 Proposed KMP Module Structure

```
vos4-multiplatform/
├── shared/                          # KMP shared module
│   ├── build.gradle.kts             # KMP configuration
│   ├── commonMain/
│   ├── commonTest/
│   ├── androidMain/
│   ├── androidTest/
│   ├── iosMain/
│   ├── iosTest/
│   ├── jvmMain/                     # Desktop (Windows, macOS, Linux)
│   └── jvmTest/
│
├── androidApp/                      # Android application
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── kotlin/                  # Android UI (Jetpack Compose)
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── src/test/
│
├── iosApp/                          # iOS application
│   ├── iosApp/
│   │   ├── ContentView.swift        # SwiftUI
│   │   ├── VOS4Bridge.swift         # Kotlin→Swift bridge
│   │   └── Info.plist
│   └── iosApp.xcodeproj
│
├── desktopApp/                      # Desktop application (Windows/macOS/Linux)
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── Main.kt                  # Compose Desktop
│
├── webScraper/                      # JavaScript web scraping tool
│   ├── build.gradle.kts
│   └── src/jsMain/kotlin/
│       └── WebScraperExtension.kt
│
└── modules/                         # Library modules
    ├── speechRecognition/
    ├── deviceManager/
    ├── voiceKeyboard/
    └── uiElements/
```

### 22.6.2 Gradle Configuration (KMP)

**Root `build.gradle.kts`:**
```kotlin
plugins {
    kotlin("multiplatform") version "1.9.20" apply false
    kotlin("plugin.serialization") version "1.9.20" apply false
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library") version "8.1.2" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
```

**`shared/build.gradle.kts`:**
```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.squareup.sqldelight")
}

kotlin {
    android()

    ios()
    iosSimulatorArm64()

    jvm("desktop")

    js(IR) {
        browser()
    }

    sourceSets {
        // Common
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("io.ktor:ktor-client-core:2.3.5")
                implementation("com.squareup.sqldelight:runtime:1.5.5")

                // Dependency Injection (expect/actual)
                implementation("org.kodein.di:kodein-di:7.20.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        // Android
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
                implementation("com.squareup.sqldelight:android-driver:1.5.5")

                // Vivoka (Android speech)
                implementation("com.vivoka:vsdk:3.0.0")
            }
        }

        // iOS
        val iosMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
            }
        }

        // Desktop (JVM)
        val desktopMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:sqlite-driver:1.5.5")

                // Windows UI Automation (JNA)
                implementation("net.java.dev.jna:jna:5.13.0")
                implementation("net.java.dev.jna:jna-platform:5.13.0")
            }
        }

        // JavaScript (web scraping)
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.voiceos.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
    }
}

sqldelight {
    database("VoiceOSDatabase") {
        packageName = "com.augmentalis.voiceos.db"
        sourceFolders = listOf("sqldelight")
    }
}
```

### 22.6.3 Dependency Injection (Kodein DI)

KMP-compatible DI framework:

```kotlin
// commonMain/kotlin/com/augmentalis/voiceos/di/CommonModule.kt
package com.augmentalis.voiceos.di

import org.kodein.di.*

val commonModule = DI.Module("common") {
    // Database
    bind<VoiceOSDatabase>() with singleton {
        createDatabase(instance())
    }

    // Repositories
    bind<CommandRepository>() with singleton {
        CommandRepositoryImpl(instance(), instance())
    }

    bind<ScreenRepository>() with singleton {
        ScreenRepositoryImpl(instance())
    }

    // Use Cases
    bind<ProcessVoiceCommandUseCase>() with singleton {
        ProcessVoiceCommandUseCase(
            commandRepository = instance(),
            commandParser = instance(),
            accessibilityService = instance()
        )
    }

    // Network
    bind<VoiceOSApi>() with singleton {
        createVoiceOSApi()
    }
}

// Platform-specific modules
expect val platformModule: DI.Module

// androidMain/kotlin/com/augmentalis/voiceos/di/PlatformModule.kt
actual val platformModule = DI.Module("android") {
    // Android-specific dependencies
    bind<Context>() with singleton {
        applicationContext
    }

    bind<DatabaseDriver>() with singleton {
        AndroidSqliteDriver(
            VoiceOSDatabase.Schema,
            instance(),
            "voiceos.db"
        )
    }

    bind<SpeechRecognitionEngine>() with singleton {
        VivokaEngine(instance())
    }

    bind<AccessibilityService>() with singleton {
        AndroidAccessibilityService()
    }
}

// iosMain/kotlin/com/augmentalis/voiceos/di/PlatformModule.kt
actual val platformModule = DI.Module("ios") {
    bind<DatabaseDriver>() with singleton {
        NativeSqliteDriver(
            VoiceOSDatabase.Schema,
            "voiceos.db"
        )
    }

    bind<SpeechRecognitionEngine>() with singleton {
        AppleSpeechEngine()
    }

    bind<AccessibilityService>() with singleton {
        IOSAccessibility()
    }
}

// Complete DI setup
val appDI = DI {
    import(commonModule)
    import(platformModule)
}
```

---

## 22.7 Dependency Management

### 22.7.1 Multiplatform Libraries

These libraries support KMP out-of-the-box:

| Library | Purpose | Version | Platforms |
|---------|---------|---------|-----------|
| **kotlinx.coroutines** | Async/concurrency | 1.7.3 | All |
| **kotlinx.serialization** | JSON/serialization | 1.6.0 | All |
| **Ktor** | HTTP client | 2.3.5 | All |
| **SQLDelight** | Database | 1.5.5 | All |
| **kotlinx.datetime** | Date/time | 0.4.1 | All |
| **Kodein-DI** | Dependency injection | 7.20.2 | All |
| **Napier** | Logging | 2.6.1 | All |
| **Kermit** | Logging (alternative) | 1.2.2 | All |
| **Koin** | DI (alternative) | 3.5.0 | All |

### 22.7.2 Platform-Specific Libraries

**Android:**
```kotlin
val androidMain by getting {
    dependencies {
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("androidx.compose.ui:ui:1.5.4")
        implementation("com.google.dagger:hilt-android:2.48")
        implementation("androidx.room:room-runtime:2.6.0")

        // Vivoka SDK (speech)
        implementation("com.vivoka:vsdk:3.0.0")
    }
}
```

**iOS:**
```kotlin
val iosMain by getting {
    dependencies {
        // Most iOS dependencies are system frameworks
        // Kotlin/Native can import them directly
    }
}
```

**Desktop (JVM):**
```kotlin
val desktopMain by getting {
    dependencies {
        implementation("org.jetbrains.compose.desktop:desktop-jvm:1.5.10")
        implementation("net.java.dev.jna:jna:5.13.0")

        // Windows Speech Recognition
        implementation("com.github.oshi:oshi-core:6.4.6")
    }
}
```

### 22.7.3 Version Catalogs (for consistency)

`gradle/libs.versions.toml`:
```toml
[versions]
kotlin = "1.9.20"
coroutines = "1.7.3"
serialization = "1.6.0"
ktor = "2.3.5"
sqldelight = "1.5.5"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
sqldelight-runtime = { module = "com.squareup.sqldelight:runtime", version.ref = "sqldelight" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-application = { id = "com.android.application", version = "8.1.2" }
```

Use in `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
            }
        }
    }
}
```

---

## 22.8 Build Configuration

### 22.8.1 Gradle Build Performance

**Parallel Builds:**
```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m

# KMP-specific
kotlin.mpp.stability.nowarn=true
kotlin.native.ignoreDisabledTargets=true
```

**Build Script:**
```kotlin
// build.gradle.kts (root)
tasks.register("buildAllPlatforms") {
    group = "build"
    description = "Builds all platform artifacts"

    dependsOn(
        ":androidApp:assembleRelease",
        ":iosApp:linkReleaseFrameworkIosArm64",
        ":desktopApp:packageDistributionForCurrentOS"
    )
}
```

### 22.8.2 iOS Framework Generation

KMP compiles Kotlin code to a native framework for iOS:

```kotlin
// shared/build.gradle.kts
kotlin {
    ios {
        binaries {
            framework {
                baseName = "shared"
                isStatic = true

                // Export dependencies to Swift
                export("com.squareup.sqldelight:runtime:1.5.5")
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
    }
}
```

**Generated Framework:**
```
shared.framework/
├── Headers/
│   ├── shared.h
│   └── shared-Swift.h
├── Modules/
│   └── module.modulemap
└── shared (binary)
```

**Usage in Swift:**
```swift
import shared

class VoiceCommandProcessor {
    private let useCase: ProcessVoiceCommandUseCase

    init() {
        self.useCase = ProcessVoiceCommandUseCase(
            commandRepository: DIContainer.shared.commandRepository,
            commandParser: DIContainer.shared.commandParser,
            accessibilityService: DIContainer.shared.accessibilityService
        )
    }

    func processCommand(_ command: String) async throws -> CommandResult {
        return try await useCase.invoke(rawCommand: command)
    }
}
```

### 22.8.3 Desktop Packaging

**Compose Desktop Packaging:**
```kotlin
// desktopApp/build.gradle.kts
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

compose.desktop {
    application {
        mainClass = "com.augmentalis.voiceos.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,      // macOS
                TargetFormat.Msi,      // Windows
                TargetFormat.Deb       // Linux
            )

            packageName = "VOS4"
            packageVersion = "4.0.0"
            description = "Voice Operating System 4"
            vendor = "Augmentalis"

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "com.augmentalis.voiceos"
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = "VOS4"
                upgradeUuid = "12345678-1234-1234-1234-123456789012"
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
```

**Build Commands:**
```bash
# Build for current OS
./gradlew :desktopApp:packageDistributionForCurrentOS

# Build specific formats
./gradlew :desktopApp:packageDmg      # macOS
./gradlew :desktopApp:packageMsi      # Windows
./gradlew :desktopApp:packageDeb      # Linux

# Output: desktopApp/build/compose/binaries/main/{dmg,msi,deb}/
```

---

## 22.9 Testing Strategy

### 22.9.1 Common Tests

Tests in `commonTest` run on ALL platforms:

```kotlin
// commonTest/kotlin/com/augmentalis/voiceos/ProcessVoiceCommandUseCaseTest.kt
package com.augmentalis.voiceos

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ProcessVoiceCommandUseCaseTest {

    private lateinit var useCase: ProcessVoiceCommandUseCase
    private lateinit var mockRepository: MockCommandRepository
    private lateinit var mockParser: MockCommandParser
    private lateinit var mockAccessibility: MockAccessibilityService

    @BeforeTest
    fun setup() {
        mockRepository = MockCommandRepository()
        mockParser = MockCommandParser()
        mockAccessibility = MockAccessibilityService()

        useCase = ProcessVoiceCommandUseCase(
            commandRepository = mockRepository,
            commandParser = mockParser,
            accessibilityService = mockAccessibility
        )
    }

    @Test
    fun `test valid command execution`() = runTest {
        // Given
        val rawCommand = "click button ok"
        mockParser.setResult(ParsedCommand("click", mapOf("target" to "button ok")))
        mockAccessibility.setScreenContext(createMockScreen())
        mockRepository.addCommand(VoiceCommand("click", "button ok"))

        // When
        val result = useCase(rawCommand)

        // Then
        assertTrue(result is Result.Success)
        assertEquals("button ok", (result as Result.Success).data.target)
    }

    @Test
    fun `test invalid command returns error`() = runTest {
        // Given
        val rawCommand = "invalid command"
        mockParser.setResult(ParsedCommand.invalid("Unknown command"))

        // When
        val result = useCase(rawCommand)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Invalid command"))
    }

    @Test
    fun `test command not found returns error`() = runTest {
        // Given
        val rawCommand = "nonexistent command"
        mockParser.setResult(ParsedCommand("nonexistent", emptyMap()))

        // When
        val result = useCase(rawCommand)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Command not found"))
    }
}
```

**Running Common Tests:**
```bash
# Run on all platforms
./gradlew :shared:allTests

# Run on specific platform
./gradlew :shared:jvmTest          # JVM
./gradlew :shared:androidUnitTest  # Android
./gradlew :shared:iosX64Test       # iOS Simulator
```

### 22.9.2 Platform-Specific Tests

```kotlin
// androidTest/kotlin/com/augmentalis/voiceos/AndroidAccessibilityServiceTest.kt
package com.augmentalis.voiceos

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class AndroidAccessibilityServiceTest {

    @Test
    fun testAccessibilityServiceInitialization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = PlatformAccessibilityService()

        // Test Android-specific functionality
        assertNotNull(service.rootInActiveWindow)
    }
}

// iosTest/kotlin/com/augmentalis/voiceos/IOSAccessibilityTest.kt
package com.augmentalis.voiceos

import platform.UIKit.UIApplication
import kotlin.test.*

class IOSAccessibilityTest {

    @Test
    fun testIOSAccessibilityInitialization() {
        val service = PlatformAccessibilityService()

        // Test iOS-specific functionality
        assertNotNull(UIApplication.sharedApplication)
    }
}
```

### 22.9.3 Test Coverage

**Coverage Report:**
```bash
./gradlew :shared:allTests --coverage

# Generate HTML report
./gradlew :shared:koverHtmlReport
```

**Target Coverage:**
- Common code: 85%+
- Platform-specific: 70%+
- Overall: 80%+

---

## 22.10 Migration Path

### 22.10.1 Phase 1: Extract Common Code (Month 1-2)

**Step 1: Create KMP Module**
```bash
# Create shared module
mkdir -p shared/src/commonMain/kotlin
mkdir -p shared/src/androidMain/kotlin
mkdir -p shared/src/iosMain/kotlin

# Create build.gradle.kts
touch shared/build.gradle.kts
```

**Step 2: Move Data Models**
```bash
# Move existing Android models to commonMain
cp modules/VoiceOSCore/src/main/kotlin/data/models/* \
   shared/src/commonMain/kotlin/com/augmentalis/voiceos/data/models/
```

**Step 3: Remove Android Dependencies**
```kotlin
// Before (Android-specific)
data class UIElement(
    val node: AccessibilityNodeInfo,  // Android type!
    val text: String?
)

// After (platform-agnostic)
data class UIElement(
    val id: String,
    val text: String?,
    val bounds: Rect,
    val className: String
)
```

### 22.10.2 Phase 2: Platform Abstraction (Month 3-4)

**Create Expect/Actual for Platform-Specific Code:**

1. **Accessibility Service**
```kotlin
// commonMain
expect class PlatformAccessibilityService {
    suspend fun getCurrentScreenContext(): ScreenContext
    suspend fun performAction(action: AccessibilityAction): Boolean
}

// androidMain
actual class PlatformAccessibilityService {
    // Move existing Android AccessibilityService code here
}

// iosMain
actual class PlatformAccessibilityService {
    // New iOS implementation
}
```

2. **Speech Recognition**
```kotlin
// commonMain
expect class SpeechRecognitionEngine {
    fun initialize(config: SpeechConfig): Boolean
    suspend fun startRecognition(listener: RecognitionListener)
    fun stopRecognition()
}

// androidMain (existing Vivoka code)
actual class SpeechRecognitionEngine { ... }

// iosMain (new Apple Speech Framework code)
actual class SpeechRecognitionEngine { ... }
```

### 22.10.3 Phase 3: Database Migration (Month 5)

**Migrate Room → SQLDelight:**

1. **Create SQLDelight Schema**
```sql
-- shared/src/commonMain/sqldelight/com/augmentalis/voiceos/db/Screen.sq

CREATE TABLE Screen (
    id TEXT PRIMARY KEY NOT NULL,
    packageName TEXT NOT NULL,
    windowTitle TEXT,
    timestamp INTEGER NOT NULL
);

selectAll:
SELECT * FROM Screen;

findById:
SELECT * FROM Screen WHERE id = ?;

insert:
INSERT INTO Screen(id, packageName, windowTitle, timestamp)
VALUES (?, ?, ?, ?);
```

2. **Generate Kotlin Code**
```bash
./gradlew :shared:generateCommonMainVoiceOSDatabaseInterface
```

3. **Use in Common Code**
```kotlin
// commonMain
class ScreenRepositoryImpl(
    private val database: VoiceOSDatabase
) : ScreenRepository {
    override suspend fun getAllScreens(): List<Screen> {
        return database.screenQueries.selectAll()
            .executeAsList()
            .map { it.toDomain() }
    }
}
```

### 22.10.4 Phase 4: iOS App Development (Month 6-8)

**Create iOS App with SwiftUI:**

```swift
// iosApp/ContentView.swift
import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = VoiceCommandViewModel()

    var body: some View {
        VStack {
            Text("VOS4 - Voice OS")
                .font(.largeTitle)

            Button("Start Listening") {
                viewModel.startListening()
            }

            if let command = viewModel.recognizedCommand {
                Text("Command: \(command)")
            }
        }
    }
}

class VoiceCommandViewModel: ObservableObject {
    @Published var recognizedCommand: String?

    private let useCase: ProcessVoiceCommandUseCase

    init() {
        self.useCase = DIContainer.shared.processVoiceCommandUseCase
    }

    func startListening() {
        Task {
            do {
                let result = try await useCase.invoke(rawCommand: "test")
                await MainActor.run {
                    self.recognizedCommand = result.toString()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }
}
```

### 22.10.5 Phase 5: Desktop App (Month 9-10)

**Create Compose Desktop App:**

```kotlin
// desktopApp/src/main/kotlin/Main.kt
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.augmentalis.voiceos.domain.usecases.ProcessVoiceCommandUseCase
import kotlinx.coroutines.launch

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "VOS4 - Voice Operating System"
    ) {
        VOS4App()
    }
}

@Composable
@Preview
fun VOS4App() {
    val scope = rememberCoroutineScope()
    var recognizedCommand by remember { mutableStateOf("") }
    val useCase = remember { appDI.direct.instance<ProcessVoiceCommandUseCase>() }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text("VOS4 - Voice Operating System", style = MaterialTheme.typography.h4)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val result = useCase("test command")
                        recognizedCommand = result.toString()
                    }
                }
            ) {
                Text("Start Listening")
            }

            if (recognizedCommand.isNotEmpty()) {
                Text("Command: $recognizedCommand")
            }
        }
    }
}
```

### 22.10.6 Timeline Summary

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| **Phase 1** | Month 1-2 | Common code extracted, KMP module created |
| **Phase 2** | Month 3-4 | Platform abstraction complete, expect/actual implemented |
| **Phase 3** | Month 5 | Database migrated to SQLDelight |
| **Phase 4** | Month 6-8 | iOS app functional, feature parity with Android |
| **Phase 5** | Month 9-10 | Desktop (Windows/macOS) app complete |
| **Testing** | Month 11-12 | Comprehensive testing, bug fixes, optimization |
| **TOTAL** | 12 months | Full cross-platform VOS4 |

---

## Summary

Kotlin Multiplatform (KMP) is the optimal choice for VOS4's cross-platform expansion:

**Key Benefits:**
- **60-80% code reuse** across Android, iOS, Windows, macOS
- **Native performance** (no runtime overhead)
- **Full platform API access** (expect/actual pattern)
- **Gradual migration** (existing Android code reusable)
- **Type safety** across platforms (Kotlin everywhere)

**Implementation Strategy:**
1. Extract common business logic to `commonMain`
2. Create platform-specific implementations with expect/actual
3. Migrate database to SQLDelight (multiplatform)
4. Build platform-specific UIs (Compose/SwiftUI/WinUI)
5. Leverage shared code for 3.35x development efficiency

**ROI:**
- ~60% reduction in development time
- 70% less code to maintain
- Guaranteed feature parity across platforms
- Single source of truth for business logic

**Next Steps:**
- [Chapter 23: iOS Implementation](23-iOS-Implementation.md) - Detailed iOS architecture
- [Chapter 24: macOS Implementation](24-macOS-Implementation.md) - macOS-specific features
- [Chapter 25: Windows Implementation](25-Windows-Implementation.md) - Windows integration

---

**Chapter Status:** ✅ Complete
**Last Updated:** 2025-11-02
**Page Count:** 72 pages
