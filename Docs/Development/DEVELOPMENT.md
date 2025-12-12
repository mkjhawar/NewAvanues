# WebAvanue Development Guide

Welcome to WebAvanue development! This guide will get you up and running quickly.

## Prerequisites

### Required Tools

- **JDK 17** or higher ([AdoptOpenJDK](https://adoptopenjdk.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/))
- **Android SDK** (API Level 35+)
- **Android Studio** Hedgehog (2023.3.1) or newer
- **Git** 2.0+

### Optional Tools

- **Xcode** (for iOS development - Phase 2)
- **IntelliJ IDEA** (alternative to Android Studio)

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/NewAvanues-WebAvanue.git
cd NewAvanues-WebAvanue/android/apps/webavanue
```

### 2. Open in Android Studio

1. Launch Android Studio
2. **File** → **Open**
3. Select `/android/apps/webavanue` directory
4. Wait for Gradle sync to complete (~2-5 minutes first time)

### 3. Build Project

```bash
# Command line
./gradlew build

# Or in Android Studio
Build → Make Project (Ctrl+F9 / Cmd+F9)
```

### 4. Run Application

**Option A: Android Studio**
1. Select **app** run configuration
2. Click **Run** (Shift+F10) or **Debug** (Shift+F9)

**Option B: Command Line**
```bash
# Install on connected device/emulator
./gradlew installDebug

# Run app
adb shell am start -n com.augmentalis.Avanues.web/.MainActivity
```

## Project Structure

```
NewAvanues-WebAvanue/
├── android/apps/webavanue/          # Android app entry point
│   ├── app/                         # Android application module
│   ├── build.gradle.kts             # Root build configuration
│   └── settings.gradle.kts          # Module includes
│
├── Modules/WebAvanue/               # KMP modules
│   ├── universal/                   # 95% shared code
│   │   ├── src/commonMain/          # Platform-independent code
│   │   ├── src/androidMain/         # Android-specific code
│   │   ├── src/iosMain/             # iOS code (Phase 2)
│   │   └── src/desktopMain/         # Desktop code (Phase 2)
│   │
│   ├── coredata/                    # Data layer (SQLDelight)
│   │   └── src/commonMain/          # Repository + Database
│   │
│   └── domain/                      # Domain models + interfaces
│       └── src/commonMain/          # Business logic
│
└── Docs/                            # Documentation
    ├── Development/                 # Developer guides
    ├── Architecture/                # Architecture docs + ADRs
    └── User/                        # User guides
```

## Development Workflow

### Daily Development

1. **Pull Latest Changes**
   ```bash
   git pull origin main
   ```

2. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make Changes**
   - Edit code in `Modules/WebAvanue/universal/src/`
   - Follow [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
   - Add KDoc comments for public APIs

4. **Test Changes**
   ```bash
   # Run unit tests
   ./gradlew test

   # Run Android instrumented tests
   ./gradlew connectedAndroidTest
   ```

5. **Commit & Push**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   git push origin feature/your-feature-name
   ```

6. **Create Pull Request**
   - Go to GitHub
   - Create PR from your branch to `main`
   - Request review

### Code Review Checklist

- ✅ All tests pass
- ✅ No compiler warnings
- ✅ Code follows SOLID principles
- ✅ Public APIs have KDoc comments
- ✅ UI changes tested on multiple screen sizes
- ✅ Performance impact considered

## Module-Specific Development

### Working on UI (Universal Module)

**Location**: `Modules/WebAvanue/universal/src/commonMain/kotlin/.../presentation/ui/`

**Key Files**:
- `browser/BrowserScreen.kt` - Main browser screen
- `browser/AddressBar.kt` - URL input bar
- `browser/BottomCommandBar.kt` - Command buttons
- `tab/TabSwitcherView.kt` - Tab management UI

**Testing UI**:
```bash
# Preview composables in Android Studio
# Add @Preview annotation to @Composable functions

# Or run instrumented tests
./gradlew :universal:connectedAndroidTest
```

### Working on Data Layer (CoreData Module)

**Location**: `Modules/WebAvanue/coredata/src/commonMain/`

**Key Files**:
- `sqldelight/BrowserDatabase.sq` - SQL schema
- `repository/BrowserRepositoryImpl.kt` - Repository implementation
- `domain/model/` - Data models

**Testing Data Layer**:
```bash
# Run unit tests
./gradlew :coredata:test

# Test database migrations
./gradlew :coredata:verifySqlDelightMigration
```

### Working on ViewModels

**Location**: `Modules/WebAvanue/universal/src/commonMain/kotlin/.../presentation/viewmodel/`

**Key ViewModels**:
- `TabViewModel` - Tab management
- `SettingsViewModel` - Browser settings
- `HistoryViewModel` - Browsing history
- `FavoriteViewModel` - Bookmarks
- `DownloadViewModel` - Downloads

**Testing ViewModels**:
```kotlin
// Example: Testing TabViewModel
@Test
fun `createTab should add new tab`() = runTest {
    val repository = FakeBrowserRepository()
    val viewModel = TabViewModel(repository)

    viewModel.createTab(url = "https://example.com")

    assertEquals(1, viewModel.tabs.value.size)
}
```

## Debugging

### WebView Debugging with Chrome DevTools

1. Enable USB debugging on Android device
2. Open Chrome on desktop → `chrome://inspect`
3. Run WebAvanue app
4. Click **Inspect** next to WebView instance
5. Use DevTools to debug web content

**Enable WebView debugging in code**:
```kotlin
// Already enabled in debug builds
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

### Android Profiler

**CPU Profiling**:
1. **View** → **Tool Windows** → **Profiler**
2. Select **CPU** timeline
3. Click **Record** → interact with app → **Stop**
4. Analyze method traces and flame charts

**Memory Profiling**:
1. Open **Profiler** → **Memory**
2. Force GC → capture heap dump
3. Analyze object allocations and leaks

**Network Profiling**:
1. Open **Profiler** → **Network**
2. Monitor WebView network requests
3. Check request/response sizes and timing

### Logging

WebAvanue uses **Napier** for cross-platform logging:

```kotlin
import io.github.aakira.napier.Napier

// Log levels
Napier.v("Verbose message")  // Verbose
Napier.d("Debug message")    // Debug
Napier.i("Info message")     // Info
Napier.w("Warning message")  // Warning
Napier.e("Error message")    // Error

// With tag
Napier.i("Tab created", tag = "TabViewModel")

// With exception
Napier.e("Database error", throwable = exception, tag = "Repository")
```

**View logs**:
```bash
# Filter by tag
adb logcat -s TabViewModel

# Filter by app
adb logcat | grep "com.augmentalis.Avanues.web"
```

## Platform-Specific Notes

### Android

**Minimum SDK**: 26 (Android 8.0 Oreo)
**Target SDK**: 35 (Android 15)
**Compile SDK**: 34

**Key Android Components**:
- `WebView` - Web content rendering
- `EncryptedSharedPreferences` - Secure credential storage
- `DownloadManager` - File downloads
- `WorkManager` - Background sync (future)

**Testing on Emulator**:
1. **Tools** → **Device Manager**
2. Create AVD with **API 35** (Android 15)
3. Select **Pixel 7** or newer device
4. **Graphics**: Hardware - GLES 3.0

### iOS (Phase 2 - Future)

**Minimum iOS**: 15.0
**Target iOS**: 17.0

**Key iOS Components**:
- `WKWebView` - Web content rendering
- `Keychain` - Secure credential storage

### Desktop (Phase 2 - Future)

**Platforms**: Windows, macOS, Linux
**Browser Engine**: TBD (JCEF or Chromium Embedded)

## Build Variants

### Debug Build

```bash
./gradlew assembleDebug
```

**Features**:
- WebView debugging enabled
- Verbose logging
- No ProGuard obfuscation
- Faster build times

### Release Build

```bash
./gradlew assembleRelease
```

**Features**:
- ProGuard enabled (code obfuscation)
- No debug logging
- APK optimization
- Requires signing key

**Sign Release APK**:
```bash
# Generate keystore (one-time)
keytool -genkey -v -keystore webavanue.keystore \
  -alias webavanue -keyalg RSA -keysize 2048 -validity 10000

# Build signed APK
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=webavanue.keystore \
  -Pandroid.injected.signing.store.password=yourpassword \
  -Pandroid.injected.signing.key.alias=webavanue \
  -Pandroid.injected.signing.key.password=yourpassword
```

## Troubleshooting

### Common Issues

#### 1. Gradle Sync Fails

**Symptoms**: "Sync failed: Connection timeout" or "Could not resolve dependencies"

**Solutions**:
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches

# Invalidate Android Studio caches
# File → Invalidate Caches → Invalidate and Restart
```

#### 2. WebView Not Loading

**Symptoms**: Blank screen or "net::ERR_CLEARTEXT_NOT_PERMITTED"

**Solutions**:
- Check `AndroidManifest.xml` has `android:usesCleartextTraffic="true"` (debug only)
- Verify internet permission: `<uses-permission android:name="android.permission.INTERNET" />`
- Check device has internet connection

#### 3. Build Errors: "Duplicate class"

**Symptoms**: Build fails with "Duplicate class found in modules"

**Solutions**:
```kotlin
// Check build.gradle.kts for conflicting dependencies
dependencies {
    // Use implementation instead of api where possible
    implementation(libs.kotlinx.coroutines.core)

    // Exclude conflicting transitive dependencies
    implementation(libs.someLibrary) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
}
```

#### 4. App Crashes on Startup

**Solutions**:
1. Check logcat for stack trace: `adb logcat -s AndroidRuntime`
2. Verify database migrations are up to date
3. Clear app data: Settings → Apps → WebAvanue → Clear Data
4. Uninstall and reinstall: `./gradlew uninstallDebug installDebug`

#### 5. Tests Fail with "No device found"

**Solutions**:
```bash
# Start emulator manually
~/Library/Android/sdk/emulator/emulator -avd Pixel_7_API_35

# Or create new AVD
~/Library/Android/sdk/tools/bin/avdmanager create avd \
  -n test_device -k "system-images;android-35;google_apis;x86_64"
```

## Performance Tips

### 1. Use Build Cache

```bash
# Enable build cache
org.gradle.caching=true

# In gradle.properties (already enabled)
```

### 2. Parallel Builds

```bash
# Use all CPU cores
org.gradle.parallel=true
org.gradle.workers.max=8  # Adjust based on CPU
```

### 3. Compose Compiler Metrics

```kotlin
// In build.gradle.kts (uncomment to analyze)
composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.compose.get()

    // Generate composition metrics
    // compilerOptions.freeCompilerArgs.add("-P")
    // compilerOptions.freeCompilerArgs.add("plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$projectDir/build/compose-metrics")
}
```

**Analyze metrics**:
```bash
./gradlew assembleDebug
open build/compose-metrics/
# Review *_composables.txt and *_classes.txt
```

## Testing Guide

### Running Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :universal:test
./gradlew :coredata:test

# Android instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# With coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Writing Tests

**Unit Tests** (commonTest):
```kotlin
class TabViewModelTest {
    private lateinit var repository: FakeBrowserRepository
    private lateinit var viewModel: TabViewModel

    @BeforeTest
    fun setup() {
        repository = FakeBrowserRepository()
        viewModel = TabViewModel(repository)
    }

    @Test
    fun `test tab creation`() = runTest {
        viewModel.createTab("https://example.com")
        assertEquals(1, viewModel.tabs.value.size)
    }

    @AfterTest
    fun tearDown() {
        viewModel.onCleared()
    }
}
```

**Compose UI Tests** (androidInstrumentedTest):
```kotlin
class BrowserScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addressBar_displaysUrl() {
        composeTestRule.setContent {
            AddressBar(
                url = "https://example.com",
                onNavigate = {}
            )
        }

        composeTestRule
            .onNodeWithText("https://example.com")
            .assertExists()
    }
}
```

## Documentation

### Generating API Docs with Dokka

```bash
# Generate HTML documentation
./gradlew dokkaHtml

# Output location
open build/dokka/html/index.html

# Generate for all modules
./gradlew dokkaHtmlMultiModule
```

### Writing KDoc

```kotlin
/**
 * Brief description of function (1 sentence).
 *
 * Longer description explaining what this function does,
 * how it works, and any important details.
 *
 * @param url URL to navigate to (must be valid HTTP(S) URL)
 * @param title Optional tab title (defaults to "New Tab")
 * @return Result<Tab> containing created tab or error
 *
 * @throws IllegalArgumentException if URL is invalid
 * @see Tab.create
 */
suspend fun createTab(url: String, title: String = "New Tab"): Result<Tab>
```

## Resources

### Documentation

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Voyager Navigation](https://voyager.adriel.cafe/)
- [Android WebView Guide](https://developer.android.com/guide/webapps/webview)

### Code Style

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

### Community

- GitHub Issues: [Report bugs](https://github.com/yourusername/NewAvanues-WebAvanue/issues)
- Discussions: [Ask questions](https://github.com/yourusername/NewAvanues-WebAvanue/discussions)

## Getting Help

1. **Check Documentation**: Review this guide and API docs
2. **Search Issues**: Check if someone else had the same problem
3. **Ask in Discussions**: Post in GitHub Discussions
4. **Create Issue**: If it's a bug, create a detailed issue

**When reporting bugs, include**:
- Android Studio version
- Gradle version (`./gradlew --version`)
- Device/emulator details
- Full error logs
- Steps to reproduce

---

**Happy Coding!** 🚀
