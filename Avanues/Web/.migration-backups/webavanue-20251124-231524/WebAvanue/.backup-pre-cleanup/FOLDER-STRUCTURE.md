# WebAvanue - IDEACODE Folder Structure

**Version:** 1.0
**Date:** 2025-11-17
**Compliance:** IDEACODE Framework v8.4

---

## 📁 Project Structure Overview

WebAvanue follows the **IDEACODE folder organization standards** with clear separation between universal (shared) code and platform-specific implementations.

```
WebAvanue/
├── Universal/              # 🌐 SHARED CODE (95% of codebase)
│   ├── domain/            # Domain models and business logic
│   │   ├── model/         # Tab, Favorite, HistoryEntry, BrowserSettings
│   │   ├── repository/    # Repository interfaces (60+ operations)
│   │   └── usecase/       # Use cases (Tab, Favorite, History, Settings)
│   ├── data/              # Data layer implementations
│   │   ├── repository/    # BrowserRepositoryImpl (SQLDelight)
│   │   ├── local/         # Local database drivers
│   │   └── mapper/        # Data mappers
│   ├── platform/          # Platform abstraction interfaces
│   │   ├── WebView.kt     # WebView interface (expect/actual)
│   │   ├── WebViewConfig.kt
│   │   └── WebViewEvent.kt
│   ├── presentation/      # Shared UI logic (ViewModels, State)
│   └── tests/             # Common unit tests (90%+ coverage)
│       ├── domain/        # Domain model tests
│       ├── repository/    # Repository tests
│       └── usecase/       # Use case tests
│
├── Android/               # 📱 ANDROID-SPECIFIC CODE
│   ├── platform/          # Android WebView implementation
│   │   └── AndroidWebView.kt
│   └── tests/             # Android-specific tests
│       └── AndroidWebViewTest.kt
│
├── iOS/                   # 🍎 iOS-SPECIFIC CODE
│   ├── platform/          # iOS WKWebView implementation
│   │   └── IOSWebView.kt
│   └── tests/             # iOS-specific tests
│       └── IOSWebViewTest.kt
│
├── Desktop/               # 🖥️ DESKTOP-SPECIFIC CODE
│   ├── platform/          # Desktop JavaFX WebView implementation
│   │   └── DesktopWebView.kt
│   └── tests/             # Desktop-specific tests
│       └── DesktopWebViewTest.kt
│
├── app/                   # Android application module
├── BrowserCoreData/       # (Legacy - being migrated)
└── build.gradle.kts       # Root build configuration
```

---

## 🎯 Design Principles

### 1. **Universal First**
- **95% of code** lives in `/Universal`
- Domain models, business logic, data layer all shared
- Maximum code reuse across platforms

### 2. **Platform Isolation**
- Each platform folder contains ONLY platform-specific code
- WebView implementations (Android WebView, iOS WKWebView, Desktop JavaFX)
- No cross-platform dependencies

### 3. **Clean Architecture**
- **Domain Layer**: Pure Kotlin, no platform dependencies
- **Data Layer**: Repository pattern with SQLDelight
- **Presentation Layer**: Shared ViewModels and state
- **Platform Layer**: Expect/actual for platform APIs

---

## 📦 Module Breakdown

### Universal Module (Shared Code)

**Purpose:** Contains all cross-platform business logic and UI

**Key Files:**
- `domain/model/Tab.kt` - Browser tab model
- `domain/model/Favorite.kt` - Bookmark model
- `domain/model/HistoryEntry.kt` - History model
- `domain/model/BrowserSettings.kt` - Settings model
- `domain/repository/BrowserRepository.kt` - Repository interface (60+ operations)
- `domain/usecase/TabUseCases.kt` - Tab management use cases
- `platform/WebView.kt` - WebView interface (expect)

**Dependencies:**
- Kotlin Stdlib
- Kotlinx Coroutines
- Kotlinx Serialization
- SQLDelight (database)
- Koin (dependency injection)

**Test Coverage:** 90%+ (407+ tests)

---

### Android Module (Android-Specific)

**Purpose:** Android WebView implementation using `android.webkit.WebView`

**Key Files:**
- `platform/AndroidWebView.kt` - Android WebView wrapper
- `tests/AndroidWebViewTest.kt` - Android tests (18 tests)

**Features:**
- Full WebView API support
- JavaScript execution
- Screenshot capture
- Download management
- Custom WebViewClient and WebChromeClient

**Dependencies:**
- Android SDK
- AndroidX WebKit
- Compose Multiplatform

---

### iOS Module (iOS-Specific)

**Purpose:** iOS WebView implementation using `WKWebView`

**Key Files:**
- `platform/IOSWebView.kt` - iOS WKWebView wrapper
- `tests/IOSWebViewTest.kt` - iOS tests (20 tests)

**Features:**
- WKWebView integration
- WKNavigationDelegate for navigation events
- WKUIDelegate for JavaScript alerts
- KVO observers for state updates

**Dependencies:**
- iOS SDK (Foundation, UIKit, WebKit)
- Compose Multiplatform

---

### Desktop Module (Desktop-Specific)

**Purpose:** Desktop WebView implementation using JavaFX WebView

**Key Files:**
- `platform/DesktopWebView.kt` - Desktop JavaFX WebView wrapper
- `tests/DesktopWebViewTest.kt` - Desktop tests (22 tests)

**Features:**
- JavaFX WebView integration
- SwingPanel for Compose Desktop
- Platform.runLater threading model
- JCEF stub for future Chromium integration

**Dependencies:**
- JavaFX
- Compose Desktop
- Swing

---

## 🔧 Build Configuration

### Root `build.gradle.kts`

```kotlin
plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("app.cash.sqldelight") version "2.1.0" apply false
}
```

### Module Build Configuration

Each platform module uses Kotlin Multiplatform with appropriate targets:

- **Universal**: `androidTarget`, `iosX64`, `iosArm64`, `jvm("desktop")`
- **Android**: `androidTarget` only
- **iOS**: `iosX64`, `iosArm64`, `iosSimulatorArm64`
- **Desktop**: `jvm("desktop")`

---

## 🧪 Testing Strategy

### Universal Tests (`/Universal/tests/`)
- **Domain Model Tests**: Tab, Favorite, HistoryEntry, BrowserSettings
- **Repository Tests**: BrowserRepositoryTest (25+ tests)
- **Use Case Tests**: TabUseCases, FavoriteUseCases, HistoryUseCases
- **Coverage Target**: 90%+

### Platform Tests
- **Android Tests**: AndroidWebViewTest (18 tests)
- **iOS Tests**: IOSWebViewTest (20 tests)
- **Desktop Tests**: DesktopWebViewTest (22 tests)
- **Coverage Target**: 80%+

### Running Tests

```bash
# Run all tests
./gradlew test

# Run Universal tests
./gradlew :Universal:test

# Run Android tests
./gradlew :Android:test

# Run iOS tests
./gradlew :iOS:iosX64Test

# Run Desktop tests
./gradlew :Desktop:desktopTest
```

---

## 📊 Code Distribution

| Module | Lines of Code | Percentage | Description |
|--------|---------------|------------|-------------|
| **Universal** | ~9,500 | 95% | Domain, data, presentation, platform interfaces |
| **Android** | ~200 | 2% | AndroidWebView implementation |
| **iOS** | ~180 | 1.8% | IOSWebView implementation |
| **Desktop** | ~220 | 2.2% | DesktopWebView implementation |

**Total:** ~10,100 lines of production code + 2,500 lines of test code

---

## 🚀 Migration Guide

### From Legacy Structure

The legacy `BrowserCoreData` module is being phased out. Here's the migration:

**Old Structure:**
```
BrowserCoreData/
├── src/commonMain/     → Universal/
├── src/androidMain/    → Android/
├── src/iosMain/        → iOS/
└── src/desktopMain/    → Desktop/
```

**Migration Steps:**
1. ✅ Move common code to `Universal/`
2. ✅ Move platform code to respective folders
3. ✅ Update import statements
4. 🔄 Update build configurations (in progress)
5. ⏳ Delete legacy `BrowserCoreData/` module

---

## 📚 References

- **IDEACODE Framework**: v8.4
- **Protocol**: `Protocol-File-Organization-v2.0.md`
- **Project Type**: Cross-Platform Browser (KMP)
- **Profile**: Library + Application

---

## ✅ Compliance Checklist

- [x] Universal folder contains 95%+ shared code
- [x] Platform folders contain ONLY platform-specific implementations
- [x] Clean separation of domain/data/presentation layers
- [x] Tests organized by platform
- [x] Build configurations follow KMP best practices
- [x] Documentation complete and up-to-date

---

**Last Updated:** 2025-11-17
**Maintainer:** WebAvanue Team
**Status:** ✅ IDEACODE Compliant
