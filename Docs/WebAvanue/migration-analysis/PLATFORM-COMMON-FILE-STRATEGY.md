# Platform-Specific vs Common Files Strategy
**Date:** 2025-11-24
**Purpose:** How to organize platform-specific (Android, iOS, Web) and common code in MainAvanues monorepo
**Based on:** Kotlin Multiplatform + Industry Best Practices

---

## 🎯 Core Principle: Source Sets, Not Folder Duplication

**DON'T** create platform folders at the monorepo root:
```
❌ BAD - Platform folders at root
MainAvanues/
├── android/       ← NO
├── ios/           ← NO
├── web/           ← NO
└── common/        ← NO
```

**DO** use KMP source sets within each library:
```
✅ GOOD - Source sets within libraries
MainAvanues/
├── apps/
│   └── ava-android/
└── libs/
    └── voice/
        └── feature-recognition/
            └── src/
                ├── commonMain/      ← Shared code (70%)
                ├── androidMain/     ← Android-specific (15%)
                ├── iosMain/         ← iOS-specific (15%)
                └── commonTest/
```

---

## 📚 Research Sources

- [Kotlin Multiplatform Project Structure](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-discover-project.html) - Official JetBrains docs
- [Advanced KMP Structure](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-advanced-project-structure.html) - Hierarchical source sets
- [KMP expect/actual Pattern](https://medium.com/@ignatiah.x/platform-specific-code-in-kotlin-multiplatform-kmp-the-expect-actual-superpower-70d02df6623a) - Platform abstraction
- [KMP Package Structure Guide](https://medium.com/@kerry.bisset/unifying-code-across-platforms-a-guide-to-kotlin-multiplatform-package-structure-1ad9fb630ddf) - Organization best practices
- [Livesport KMP Monorepo](https://medium.com/@livesportaci/kotlin-multiplatform-in-monorepo-7429b0745d1e) - Real-world implementation

---

## 🏗️ Library-Level Platform Organization

### **Each Library Has Its Own Platform Code**

```kotlin
libs/voice/feature-recognition/
├── docs/
│   └── README.md
│
├── src/
│   ├── commonMain/kotlin/            ← 70% of code lives here
│   │   └── com/ideacode/voice/
│   │       ├── VoiceRecognition.kt   ← Interface (shared)
│   │       ├── RecognitionEngine.kt  ← Business logic (shared)
│   │       └── Models.kt             ← Data models (shared)
│   │
│   ├── androidMain/kotlin/           ← 15% Android-specific
│   │   └── com/ideacode/voice/
│   │       └── VoiceRecognition.android.kt  ← Android impl
│   │
│   ├── iosMain/kotlin/               ← 15% iOS-specific (future)
│   │   └── com/ideacode/voice/
│   │       └── VoiceRecognition.ios.kt      ← iOS impl
│   │
│   ├── commonTest/kotlin/            ← Shared tests
│   ├── androidUnitTest/kotlin/       ← Android unit tests
│   └── androidInstrumentedTest/kotlin/  ← Android UI tests
│
└── build.gradle.kts
```

**Key Points:**
> "The shared module consists of three source sets: androidMain, commonMain, and iosMain. A source set is a Gradle concept for a number of files logically grouped together where each group has its own dependencies." - [Kotlin Multiplatform Docs](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-discover-project.html)

---

## 🔧 Platform-Specific Implementation Patterns

### **Pattern 1: expect/actual (Recommended)**

**When to use:** Platform-specific APIs (Camera, Bluetooth, File System, etc.)

**commonMain/kotlin/:**
```kotlin
// Declaration in common code
expect class VoiceRecognitionEngine {
    fun startListening()
    fun stopListening()
    fun getResults(): List<String>
}
```

**androidMain/kotlin/:**
```kotlin
// Android implementation
actual class VoiceRecognitionEngine {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

    actual fun startListening() {
        recognizer.startListening(intent)
    }

    actual fun stopListening() {
        recognizer.stopListening()
    }

    actual fun getResults(): List<String> {
        // Android-specific logic
    }
}
```

**iosMain/kotlin/:**
```kotlin
// iOS implementation (future)
actual class VoiceRecognitionEngine {
    private val recognizer = SFSpeechRecognizer()

    actual fun startListening() {
        // iOS-specific logic
    }

    actual fun stopListening() {
        // iOS-specific logic
    }

    actual fun getResults(): List<String> {
        // iOS-specific logic
    }
}
```

> "The expect/actual mechanism serves as the linchpin for KMP's platform capabilities. The expect keyword defines a placeholder in shared code where functionality will be implemented differently on each platform." - [Medium: KMP expect/actual](https://medium.com/@ignatiah.x/platform-specific-code-in-kotlin-multiplatform-kmp-the-expect-actual-superpower-70d02df6623a)

---

### **Pattern 2: Interface + Factory (Preferred for Most Cases)**

**When to use:** Most platform-specific features (better testability)

> "Most developers should avoid expect/actual classes in most cases. Instead, create an interface in common, implement in platform-specific code, and use expect/actual functions to load the platform's default implementation." - [Touchlab: expect/actual Best Practices](https://touchlab.co/expect-actuals-statements-kotlin-multiplatform)

**commonMain/kotlin/:**
```kotlin
// Interface in common code
interface VoiceRecognitionEngine {
    fun startListening()
    fun stopListening()
    fun getResults(): List<String>
}

// Factory function (expect)
expect fun createVoiceRecognitionEngine(): VoiceRecognitionEngine
```

**androidMain/kotlin/:**
```kotlin
// Android implementation
class AndroidVoiceRecognitionEngine : VoiceRecognitionEngine {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

    override fun startListening() {
        recognizer.startListening(intent)
    }

    override fun stopListening() {
        recognizer.stopListening()
    }

    override fun getResults(): List<String> {
        // Android-specific logic
    }
}

// Factory (actual)
actual fun createVoiceRecognitionEngine(): VoiceRecognitionEngine {
    return AndroidVoiceRecognitionEngine()
}
```

**iosMain/kotlin/:**
```kotlin
// iOS implementation
class IOSVoiceRecognitionEngine : VoiceRecognitionEngine {
    private val recognizer = SFSpeechRecognizer()

    override fun startListening() {
        // iOS-specific logic
    }

    override fun stopListening() {
        // iOS-specific logic
    }

    override fun getResults(): List<String> {
        // iOS-specific logic
    }
}

// Factory (actual)
actual fun createVoiceRecognitionEngine(): VoiceRecognitionEngine {
    return IOSVoiceRecognitionEngine()
}
```

**Benefits:**
- ✅ Easier to test (mock the interface)
- ✅ More flexible (can swap implementations)
- ✅ Better separation of concerns

---

### **Pattern 3: File Suffix Convention (Web/Mobile)**

**When to use:** React Native + Web monorepos

**For libraries that need different UI/logic per platform:**

```
libs/shared/ui-button/
└── src/
    ├── Button.tsx          ← Shared logic
    ├── Button.web.tsx      ← Web-specific implementation
    └── Button.native.tsx   ← React Native implementation
```

> "When a component needs platform-specific implementations, you can have separate files in the same folder that will be imported by their respective platforms" - [DEV Community: React Native Web Monorepo](https://dev.to/brunolemos/tutorial-100-code-sharing-between-ios-android--web-using-react-native-web-andmonorepo-4pej)

**Not recommended for MainAvanues** (we're using Kotlin, not React)

---

## 📦 When Each App Needs Platform Code

### **Apps are Platform-Specific (No commonMain)**

```
apps/ava-android/
├── docs/
├── src/
│   ├── main/kotlin/          ← Android code only
│   │   └── MainActivity.kt
│   └── test/kotlin/
└── build.gradle.kts

apps/ava-ios/                 ← Future
├── docs/
├── src/
│   └── iosMain/kotlin/       ← iOS code only
│       └── MainViewController.kt
└── build.gradle.kts

apps/webavanue-browser/
├── docs/
├── src/
│   └── main/kotlin/          ← Browser extension code
│       └── background.kt
└── build.gradle.kts
```

**Why apps don't share code:**
- Apps are platform-specific by definition
- They wire together libraries (which DO share code)
- Keeps architecture clean

---

## 🎨 UI Code: Platform-Specific or Shared?

### **Option 1: Compose Multiplatform (Recommended)**

Share UI across Android, iOS, Desktop, Web:

```kotlin
libs/shared/ui-design-system/
└── src/
    ├── commonMain/kotlin/
    │   ├── Button.kt           ← Shared Compose UI
    │   ├── TextField.kt        ← Works on all platforms
    │   └── Theme.kt
    │
    ├── androidMain/kotlin/
    │   └── AndroidSpecificUI.kt  ← Only if needed
    │
    └── iosMain/kotlin/
        └── IOSSpecificUI.kt      ← Only if needed
```

**Up to 90% UI code sharing** with Compose Multiplatform!

---

### **Option 2: Platform-Specific UI Libraries**

When UI must be platform-specific:

```
libs/shared/ui-design-system/
└── src/
    ├── commonMain/kotlin/
    │   └── ButtonContract.kt    ← Interface only
    │
    ├── androidMain/kotlin/
    │   └── AndroidButton.kt     ← Compose for Android
    │
    └── iosMain/kotlin/
        └── IOSButton.kt         ← SwiftUI wrapper (future)
```

---

## 🗂️ Real Example: Voice Recognition Library

### **Full Structure**

```kotlin
libs/voice/feature-recognition/
├── docs/
│   ├── README.md
│   ├── api.md
│   └── platform-differences.md     ← Document platform quirks
│
├── src/
│   ├── commonMain/kotlin/com/ideacode/voice/
│   │   ├── VoiceRecognizer.kt      ← Interface (shared)
│   │   ├── RecognitionResult.kt    ← Data model (shared)
│   │   ├── AudioProcessor.kt       ← Business logic (shared)
│   │   └── Utils.kt                ← Helpers (shared)
│   │
│   ├── androidMain/kotlin/com/ideacode/voice/
│   │   ├── AndroidVoiceRecognizer.kt      ← Android impl
│   │   └── AndroidAudioCapture.kt         ← Platform API usage
│   │
│   ├── iosMain/kotlin/com/ideacode/voice/
│   │   ├── IOSVoiceRecognizer.kt          ← iOS impl (future)
│   │   └── IOSAudioCapture.kt
│   │
│   ├── commonTest/kotlin/
│   │   └── VoiceRecognizerTest.kt         ← Shared tests
│   │
│   ├── androidUnitTest/kotlin/
│   │   └── AndroidVoiceRecognizerTest.kt  ← Android-specific tests
│   │
│   └── androidInstrumentedTest/kotlin/
│       └── VoiceRecognitionUITest.kt      ← Android UI tests
│
└── build.gradle.kts
```

### **build.gradle.kts Configuration**

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    // Future iOS support
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "VoiceRecognition"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core)
            // Android-specific dependencies
        }

        iosMain.dependencies {
            // iOS-specific dependencies (future)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.ideacode.voice.recognition"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}
```

---

## 📊 Code Distribution Guidelines

### **Target Percentages per Source Set**

| Source Set | Percentage | What Goes Here |
|------------|-----------|----------------|
| `commonMain` | **70%** | Business logic, data models, interfaces, algorithms |
| `androidMain` | **15%** | Android APIs, platform-specific UI |
| `iosMain` | **15%** | iOS APIs, platform-specific UI (future) |

> "With proper organization, features can share more than 90% of the exact same code across platforms" - [GitHub: React Native Web Monorepo](https://github.com/brunolemos/react-native-web-monorepo)

### **What Should Be in commonMain?**

✅ **YES - Put in commonMain:**
- Business logic
- Data models (data classes)
- ViewModels/Presenters
- Repository interfaces
- Use cases
- Domain logic
- Utilities (JSON, Date, String manipulation)
- Network models
- Database schemas

❌ **NO - Don't put in commonMain:**
- Android-specific APIs (SpeechRecognizer, Camera, etc.)
- iOS-specific APIs (SFSpeechRecognizer, etc.)
- Platform UI code (unless using Compose Multiplatform)
- File system APIs
- Bluetooth/NFC/Hardware
- Platform permissions

---

## 🔍 How to Decide: Common vs Platform?

### **Decision Tree**

```
Does this code use platform-specific APIs?
├─ NO → Put in commonMain
│      Example: Data model, business logic
│
└─ YES → Does it need different behavior per platform?
       ├─ NO → Use expect/actual with same logic
       │      Example: Log output (different APIs, same behavior)
       │
       └─ YES → Use Interface + Platform Implementation
              Example: Voice recognition (different capabilities)
```

### **Examples**

| Code | Location | Reason |
|------|----------|--------|
| `data class User(val name: String)` | `commonMain` | Pure Kotlin, no platform APIs |
| `class UserRepository(api: API)` | `commonMain` | Business logic, uses interfaces |
| `expect fun log(message: String)` | `commonMain` (expect) | Needs platform APIs |
| `actual fun log(message: String)` | `androidMain/iosMain` | Platform-specific implementation |
| `class SpeechRecognizer` | `androidMain/iosMain` only | Platform-specific API |
| `interface VoiceEngine` | `commonMain` | Contract for platform impls |

---

## 🚀 Migration Strategy for MainAvanues

### **Phase 1: Identify Shared Code**

For each existing repo (AVA, VoiceOS, etc.), analyze:

1. **100% Shared (→ commonMain)**
   - Data models
   - Business logic
   - ViewModels
   - Repository interfaces

2. **Android-Only (→ androidMain)**
   - Android APIs usage
   - AccessibilityService code
   - Android-specific UI

3. **Platform-Agnostic but Different (→ expect/actual)**
   - File I/O
   - Logging
   - Preferences

### **Phase 2: Restructure Libraries**

```bash
# OLD (AVA repo)
AVA/
└── app/src/main/java/
    ├── ui/              ← Keep in app
    ├── data/            ← Move to libs/*/data-access-*/commonMain
    ├── domain/          ← Move to libs/*/feature-*/commonMain
    └── SpeechService.kt ← Move to libs/voice/feature-recognition/androidMain

# NEW (MainAvanues monorepo)
MainAvanues/
├── apps/ava-android/
│   └── src/main/kotlin/
│       └── ui/         ← App-specific UI only
│
└── libs/voice/feature-recognition/
    └── src/
        ├── commonMain/      ← Business logic
        └── androidMain/     ← Android APIs
```

### **Phase 3: Add iOS Support (Future)**

When ready for iOS:

1. Add `iosMain` to existing libraries
2. Implement `actual` declarations
3. Create `apps/ava-ios/`
4. Reuse 70% of code from `commonMain`!

---

## 📝 Documentation Best Practices

### **Document Platform Differences**

Each library with platform-specific code should have:

```markdown
# Platform Differences

## Voice Recognition

### Android
- Uses Android SpeechRecognizer API
- Requires RECORD_AUDIO permission
- Offline mode available with language packs

### iOS (Future)
- Uses SFSpeechRecognizer
- Requires Speech recognition permission
- Online-only for best accuracy
```

### **README Template**

```markdown
# {Library Name}

## Overview
[What this library does]

## Supported Platforms
- ✅ Android (API 24+)
- 🚧 iOS (Planned)
- ❌ Web (Not applicable)

## Platform-Specific Notes

### Android
[Android-specific details]

### iOS
[iOS-specific details]

## Usage

### Common Code
```kotlin
// Works on all platforms
val recognizer = createVoiceRecognizer()
recognizer.start()
```

### Platform-Specific
```kotlin
// Android only
val androidRecognizer = AndroidVoiceRecognizer()
```

## Dependencies
[Platform-specific dependencies]
```

---

## ✅ Key Takeaways

1. **No Platform Folders at Root** - Use source sets within libraries
2. **70% in commonMain** - Maximum code sharing
3. **Interface + Factory Pattern** - Better than expect/actual classes
4. **Apps are Platform-Specific** - They don't share code, only libraries do
5. **Document Platform Differences** - Make it clear what works where

---

## 🔗 Additional Resources

- [Kotlin Multiplatform Hierarchy](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-hierarchy.html)
- [KMP Wrapper Pattern](https://www.revenuecat.com/blog/engineering/kmp-wrapper-pattern/)
- [expect/actual in 5 Minutes](https://touchlab.co/expect-actuals-statements-kotlin-multiplatform)
- [KMP for Native Developers Book](https://santimattius.github.io/kmp-for-mobile-native-developers-book/)

---

**Next:** Apply this strategy to MainAvanues migration plan
