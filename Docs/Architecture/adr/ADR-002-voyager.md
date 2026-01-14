# ADR-002: Voyager for Navigation

**Status:** Accepted
**Date:** 2025-12-12
**Deciders:** WebAvanue Architecture Team

## Context

WebAvanue requires a navigation solution for managing screen transitions in a Kotlin Multiplatform Compose application. The navigation library must:

1. Support Compose Multiplatform (Android, iOS, Desktop)
2. Provide type-safe navigation arguments
3. Enable tab-based navigation (browser tabs concept)
4. Support ViewModel/ScreenModel lifecycle management
5. Offer smooth screen transitions and animations
6. Work with nested navigation patterns

We needed to choose between Jetpack Navigation Compose (Android-first), Decompose, Voyager, or custom navigation.

## Decision

We will use **Voyager** as our navigation framework across all platforms.

Voyager provides:
- **KMP-First Design**: Built specifically for Compose Multiplatform
- **Type-Safe Navigation**: Compile-time checked routes
- **ScreenModel Integration**: Built-in ViewModel alternative for KMP
- **Tab Navigation**: Native tab navigator for browser-style tabs
- **Transitions**: Customizable screen transitions
- **Lifecycle Management**: Proper lifecycle handling per screen

## Rationale

### Why Voyager Over Alternatives

1. **True KMP**: Unlike Jetpack Navigation (Android-only), Voyager works on all platforms
2. **Simplicity**: Cleaner API than Decompose with less boilerplate
3. **Tab Support**: Built-in `TabNavigator` perfect for browser tabs
4. **ScreenModel**: Provides KMP-compatible ViewModel alternative
5. **Community**: Active development and good documentation
6. **Performance**: Lightweight with minimal overhead

### Technical Benefits

- **Screen-Based Navigation**: Each screen is a class implementing `Screen` interface
- **Type Safety**: Pass objects directly, not serialized strings
- **Scoped Dependencies**: Koin/DI integration for screen-level dependencies
- **State Preservation**: Automatic state saving/restoration
- **Back Stack Management**: Built-in back stack with configurable behavior

### Development Benefits

- **IntelliJ IDEA Support**: Full IDE autocomplete and refactoring
- **Easy Testing**: Simple to test navigation logic with fake screens
- **Minimal Learning Curve**: Intuitive API similar to Jetpack Navigation
- **Good Documentation**: Clear examples and guides

## Consequences

### Positive

- ✅ **Cross-Platform**: Same navigation code on Android, iOS, Desktop
- ✅ **Type-Safe**: Compile-time errors for invalid navigation
- ✅ **Tab Navigator**: Perfect fit for browser multi-tab architecture
- ✅ **ScreenModel**: Built-in ViewModel for sharing state
- ✅ **Transitions**: Smooth animations between screens
- ✅ **Lightweight**: Minimal runtime overhead (~200KB)
- ✅ **Active Development**: Regular updates and bug fixes

### Negative

- ⚠️ **Smaller Community**: Less popular than Jetpack Navigation (mitigated: sufficient docs and examples)
- ⚠️ **Breaking Changes**: API may evolve (mitigated: pin to stable versions)

### Mitigation Strategies

1. **API Changes**: Pin to stable version (1.0.0), update cautiously with testing
2. **Community**: Contribute fixes and examples back to Voyager project
3. **Fallback**: Abstract navigation behind repository pattern for easy migration if needed

## Alternatives Considered

### Alternative 1: Jetpack Navigation Compose

- **Pros:**
  - Official Google library
  - Mature and well-documented
  - Large community
  - Deep integration with Android ecosystem
  - SafeArgs plugin for type safety

- **Cons:**
  - Android-only (no iOS or Desktop support)
  - String-based routes (less type-safe)
  - Requires serialization for complex arguments
  - No built-in tab navigation

- **Why Rejected:** Not a KMP solution. Would require platform-specific navigation implementations, defeating the purpose of KMP.

### Alternative 2: Decompose

- **Pros:**
  - True KMP library
  - Component-based architecture
  - Lifecycle-aware
  - Back stack management
  - State preservation

- **Cons:**
  - More boilerplate than Voyager
  - Steeper learning curve
  - Component-based model more complex than screen-based
  - Less intuitive API for simple navigation

- **Why Rejected:** Decompose's component-based architecture is powerful but overkill for our needs. Voyager's screen-based model is simpler and more intuitive for browser navigation.

### Alternative 3: Custom Navigation

- **Pros:**
  - Full control over implementation
  - Tailored exactly to our needs
  - No external dependencies

- **Cons:**
  - Significant development time
  - Must implement all features from scratch
  - Testing burden
  - Maintenance burden
  - Reinventing the wheel

- **Why Rejected:** Building custom navigation would delay development by weeks. Voyager provides everything we need out-of-the-box with active maintenance.

### Alternative 4: Appyx (formerly Badoo RIBs)

- **Pros:**
  - KMP support
  - Model-driven navigation
  - Gesture-based transitions
  - Modular architecture

- **Cons:**
  - Complex API
  - Steep learning curve
  - Overkill for browser navigation
  - Smaller community than Voyager

- **Why Rejected:** Appyx is designed for complex, modular apps with gesture-based navigation. Our browser needs simple screen-to-screen navigation, making Appyx unnecessarily complex.

## Implementation Notes

### Navigation Structure

```kotlin
// Screen definition
class BrowserScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { BrowserScreenModel() }

        BrowserUI(
            screenModel = screenModel,
            onOpenSettings = { navigator.push(SettingsScreen()) }
        )
    }
}

// Tab navigation for browser tabs
@Composable
fun TabNavigatorContent() {
    TabNavigator(BrowserTab) { tabNavigator ->
        Scaffold(
            bottomBar = {
                TabBar(tabNavigator = tabNavigator)
            }
        ) {
            CurrentTab()
        }
    }
}

// ScreenModel for state management
class BrowserScreenModel : ScreenModel {
    private val repository = BrowserRepository()
    val tabs = repository.observeTabs().stateIn(...)

    fun createTab(url: String) {
        repository.createTab(url)
    }

    override fun onDispose() {
        // Cleanup
    }
}
```

### Directory Organization

```
presentation/
├── navigation/
│   ├── Screen.kt                   # Base screen interface
│   ├── BrowserNavigator.kt         # Navigator wrapper
│   └── Transitions.kt              # Custom transitions
├── screens/
│   ├── browser/
│   │   └── BrowserScreen.kt        # Main browser screen
│   ├── settings/
│   │   └── SettingsScreen.kt       # Settings screen
│   ├── history/
│   │   └── HistoryScreen.kt        # History screen
│   └── downloads/
│       └── DownloadScreen.kt       # Downloads screen
└── screenmodels/
    ├── BrowserScreenModel.kt       # Browser state management
    └── SettingsScreenModel.kt      # Settings state management
```

### Example Navigation Flow

```kotlin
// Navigate forward
navigator.push(SettingsScreen())

// Navigate back
navigator.pop()

// Replace current screen
navigator.replace(LoginScreen())

// Pop to root
navigator.popUntilRoot()

// Tab navigation
tabNavigator.current = HistoryTab
```

### Transition Animations

```kotlin
// Custom fade transition
Navigator(BrowserScreen()) { navigator ->
    FadeTransition(navigator) {
        CurrentScreen()
    }
}

// Slide transition
SlideTransition(
    navigator = navigator,
    orientation = SlideOrientation.Horizontal
) {
    CurrentScreen()
}
```

## Integration with Other Components

### With Koin DI

```kotlin
class SettingsScreen : Screen {
    @Composable
    override fun Content() {
        val repository: BrowserRepository = koinInject()
        val viewModel = remember { SettingsViewModel(repository) }
        // ...
    }
}
```

### With State Preservation

```kotlin
class BrowserScreenModel : StateScreenModel<BrowserState>(BrowserState()) {
    // State automatically saved/restored on process death
}
```

## References

- [Voyager Documentation](https://voyager.adriel.cafe/)
- [Voyager GitHub](https://github.com/adrielcafe/voyager)
- [Compose Multiplatform Navigation Guide](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html)

## Revision History

| Version | Date       | Changes                           |
|---------|------------|-----------------------------------|
| 1.0     | 2025-12-12 | Initial ADR documenting decision  |
