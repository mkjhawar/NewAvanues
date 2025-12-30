# ADR-003: Kotlin Multiplatform Architecture

**Status:** Accepted
**Date:** 2025-12-12
**Deciders:** WebAvanue Architecture Team

## Context

WebAvanue needs to target multiple platforms (Android, iOS, Desktop) with minimal code duplication. We needed to decide on the overall architecture strategy for code sharing across platforms while maintaining platform-specific capabilities.

Key requirements:
1. Share 90%+ of business logic across platforms
2. Support platform-specific UI when needed (WebView, native components)
3. Maintain type safety and compile-time verification
4. Enable independent platform releases
5. Provide good developer experience with IDE support

## Decision

We will use **Kotlin Multiplatform (KMP)** with a **modular architecture** that separates shared and platform-specific code.

Architecture layers:
- **Universal Module**: 95% shared code (UI, ViewModels, business logic)
- **CoreData Module**: 100% shared data layer (database, repository)
- **Domain Module**: 100% shared domain models and interfaces
- **Platform Modules**: Platform-specific implementations (5%)

Code organization:
```
src/
├── commonMain/      # Shared code (90-95%)
├── androidMain/     # Android-specific
├── iosMain/         # iOS-specific (Phase 2)
└── desktopMain/     # Desktop-specific (Phase 2)
```

## Rationale

### Why KMP Over Alternatives

1. **Native Performance**: Compiles to native code on each platform
2. **Type Safety**: Full Kotlin type system across platforms
3. **Platform Access**: Can call platform APIs via expect/actual
4. **Shared Business Logic**: Single source of truth for core logic
5. **IDE Support**: Full IntelliJ IDEA/Android Studio support
6. **Gradual Adoption**: Can share incrementally, not all-or-nothing

### Technical Benefits

- **Compose Multiplatform**: Share UI code with Jetpack Compose
- **Coroutines**: Async operations work identically on all platforms
- **Flow**: Reactive streams shared across platforms
- **Dependency Injection**: Koin works in commonMain
- **Testing**: Write tests once, run on all platforms

### Business Benefits

- **Faster Development**: Write once, deploy to 3+ platforms
- **Consistent UX**: Same business logic = consistent behavior
- **Easier Maintenance**: Fix bugs once, not per platform
- **Smaller Teams**: Don't need separate iOS and Android teams

## Consequences

### Positive

- ✅ **95% Code Sharing**: Achieved in Universal module
- ✅ **Single Codebase**: One repo for all platforms
- ✅ **Consistent Logic**: Same behavior on Android, iOS, Desktop
- ✅ **Type Safety**: Compile-time errors catch issues early
- ✅ **Native Performance**: No runtime penalty vs native apps
- ✅ **Platform Flexibility**: Can drop to native when needed
- ✅ **Developer Productivity**: Write code once, test once

### Negative

- ⚠️ **Build Complexity**: Multi-platform Gradle setup (mitigated: well-documented)
- ⚠️ **Platform Quirks**: Need expect/actual for platform APIs (mitigated: minimal usage)
- ⚠️ **Tooling Maturity**: KMP tooling still evolving (mitigated: stable for our use cases)
- ⚠️ **Learning Curve**: Team must learn KMP patterns (mitigated: good documentation)

### Mitigation Strategies

1. **Build Complexity**: Use version catalogs and build convention plugins
2. **Platform APIs**: Abstract platform differences behind interfaces
3. **Tooling**: Pin to stable Kotlin versions, use proven libraries
4. **Learning**: Provide training and code examples

## Module Architecture

### 1. Universal Module (95% Shared)

**Purpose**: Share UI, ViewModels, and presentation logic

**Structure**:
```kotlin
universal/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   ├── presentation/
│   │   │   │   ├── ui/          # Compose UI (shared)
│   │   │   │   └── viewmodel/   # ViewModels
│   │   │   └── utils/           # Utilities
│   │   └── resources/           # Shared resources
│   ├── androidMain/
│   │   └── kotlin/
│   │       └── platform/        # Android WebView, etc.
│   ├── iosMain/                 # Phase 2
│   └── desktopMain/             # Phase 2
└── build.gradle.kts
```

**Shared Components**:
- Compose UI (BrowserScreen, SettingsScreen, etc.)
- ViewModels (TabViewModel, SettingsViewModel, etc.)
- Navigation (Voyager screens)
- Business logic
- Utilities (Logger, validators, etc.)

### 2. CoreData Module (100% Shared)

**Purpose**: Data persistence and repository pattern

**Structure**:
```kotlin
coredata/
├── src/
│   ├── commonMain/
│   │   ├── sqldelight/          # Database schema
│   │   ├── kotlin/
│   │   │   ├── repository/      # BrowserRepositoryImpl
│   │   │   ├── domain/model/    # Domain models
│   │   │   └── mappers/         # DB ↔ Domain mapping
│   ├── androidMain/
│   │   └── kotlin/db/           # AndroidSqlDriver
│   └── iosMain/                 # NativeSqlDriver
└── build.gradle.kts
```

**Fully Shared**:
- SQLDelight schema and queries
- Repository implementation
- Domain models (Tab, Favorite, Settings, etc.)
- Mappers (DB ↔ Domain)

**Platform-Specific**:
- Only database driver initialization

### 3. Domain Module (100% Shared)

**Purpose**: Domain models and repository interfaces

**Structure**:
```kotlin
domain/
├── src/commonMain/kotlin/
│   ├── model/               # Data classes
│   ├── repository/          # Repository interfaces
│   ├── validation/          # URL, input validators
│   └── errors/              # Domain-specific errors
└── build.gradle.kts
```

**Fully Shared**:
- All domain models
- Repository interfaces
- Validation logic
- Error types

### 4. Platform Modules (5%)

**Android**: WebView, EncryptedSharedPreferences, DownloadManager
**iOS**: WKWebView, Keychain (Phase 2)
**Desktop**: JCEF/CEF WebView (Phase 2)

**Pattern**:
```kotlin
// commonMain - Define interface
expect class WebViewController {
    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
}

// androidMain - Android implementation
actual class WebViewController {
    private val webView: WebView = ...
    actual fun loadUrl(url: String) { webView.loadUrl(url) }
    actual fun goBack() { webView.goBack() }
    actual fun goForward() { webView.goForward() }
}

// iosMain - iOS implementation
actual class WebViewController {
    private val webView: WKWebView = ...
    actual fun loadUrl(url: String) { /* WKWebView code */ }
    actual fun goBack() { /* WKWebView code */ }
    actual fun goForward() { /* WKWebView code */ }
}
```

## Alternatives Considered

### Alternative 1: Native Development (Separate Codebases)

- **Pros:**
  - Best platform integration
  - Full access to platform APIs
  - Mature tooling per platform
  - Platform-specific optimizations

- **Cons:**
  - Duplicate code (3x development effort)
  - Inconsistent behavior across platforms
  - 3x testing effort
  - 3x bug fixing effort
  - Requires separate iOS and Android teams

- **Why Rejected:** Development cost too high. Business logic would be written 3 times (Kotlin, Swift, Java/Kotlin), with high risk of divergence and bugs.

### Alternative 2: React Native

- **Pros:**
  - Mature cross-platform framework
  - Large community
  - Hot reload
  - JavaScript/TypeScript ecosystem

- **Cons:**
  - JavaScript runtime (slower than native)
  - Bridge overhead for native calls
  - Not suitable for WebView-heavy apps
  - Difficult to customize WebView deeply
  - Different language from native Android (Kotlin)

- **Why Rejected:** React Native's JavaScript bridge adds overhead for WebView operations. WebAvanue is fundamentally a WebView wrapper requiring deep platform integration, making React Native's abstraction layer a liability.

### Alternative 3: Flutter

- **Pros:**
  - Good cross-platform support
  - Fast rendering (Skia engine)
  - Single codebase (Dart)
  - Mature tooling

- **Cons:**
  - Dart language (different from Kotlin)
  - Larger app size
  - WebView integration not first-class
  - Difficult to share code with existing Kotlin projects
  - Less access to latest Android APIs

- **Why Rejected:** Dart is not Kotlin, preventing code sharing with other NewAvanues projects. Flutter's WebView support (webview_flutter) is less mature than native WebView integration.

### Alternative 4: Xamarin

- **Pros:**
  - Cross-platform with C#
  - Microsoft-backed
  - Good platform integration

- **Cons:**
  - C# language (not Kotlin)
  - Being deprecated in favor of .NET MAUI
  - Smaller community
  - Heavier runtime
  - Poor fit for Kotlin ecosystem

- **Why Rejected:** Xamarin is being phased out for .NET MAUI. C# doesn't integrate with Kotlin ecosystem or existing NewAvanues codebase.

## Implementation Strategy

### Phase 1: Android (Current)

1. ✅ Implement Universal module with commonMain + androidMain
2. ✅ Implement CoreData module with SQLDelight
3. ✅ Implement Domain module with shared models
4. ✅ Android app module consuming Universal module

### Phase 2: iOS (Future)

1. Add iosMain source sets to Universal and CoreData
2. Implement iOS-specific WebView wrapper
3. Create iOS app target
4. Test and optimize iOS build

### Phase 3: Desktop (Future)

1. Add desktopMain source sets
2. Integrate JCEF or Chromium Embedded Framework
3. Create Desktop app target
4. Platform-specific window management

## Best Practices

### 1. Maximize Shared Code

- Put code in commonMain unless platform-specific
- Use expect/actual sparingly
- Abstract platform differences behind interfaces

### 2. Dependency Management

- Use version catalog for consistent versions
- Prefer KMP-compatible libraries (coroutines, serialization, etc.)
- Use Koin for dependency injection (KMP-compatible)

### 3. Testing

- Write tests in commonTest when possible
- Platform tests only for platform-specific code
- Use Fake repositories for testing ViewModels

### 4. Platform APIs

```kotlin
// Good: Interface in commonMain
interface PlatformLogger {
    fun log(message: String)
}

// Android implementation
class AndroidLogger : PlatformLogger {
    override fun log(message: String) {
        Log.d("WebAvanue", message)
    }
}

// Use DI to provide platform instance
```

## References

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [KMP by Tutorials](https://www.raywenderlich.com/books/kotlin-multiplatform-by-tutorials)
- [TouchLab KMP Resources](https://touchlab.co/kotlin-multiplatform/)

## Revision History

| Version | Date       | Changes                           |
|---------|------------|-----------------------------------|
| 1.0     | 2025-12-12 | Initial ADR documenting decision  |
