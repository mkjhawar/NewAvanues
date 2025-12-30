# WebAvanue Universal Module

## Overview

The Universal module contains 95% of WebAvanue's shared code across Android, iOS, and Desktop platforms using Kotlin Multiplatform (KMP).

## Architecture

This module implements a clean, layered architecture:

- **Presentation Layer**: Compose UI + ViewModels
- **Domain Layer**: Business logic (via BrowserCoreData dependency)
- **Data Layer**: Repository access (via BrowserCoreData dependency)

## Key Components

### ViewModels

- `TabViewModel` - Tab management and navigation
- `SettingsViewModel` - Browser settings management
- `HistoryViewModel` - Browsing history
- `FavoriteViewModel` - Favorites/bookmarks
- `DownloadViewModel` - Download management
- `SecurityViewModel` - Security and permissions

### UI Components

- **Browser**: Main browser screen, address bar, command bar
- **Tabs**: Tab switcher, tab bar, tab groups
- **Settings**: Settings screens and dialogs
- **History**: History list and search
- **Downloads**: Download manager UI
- **Security**: Security dialogs and permissions

### Platform Abstractions

Platform-specific implementations are provided in:
- `androidMain/` - Android WebView integration
- `iosMain/` - iOS WKWebView (Phase 2)
- `desktopMain/` - Desktop browser engine (Phase 2)

## Dependencies

- **BrowserCoreData**: Shared data layer with SQLDelight database
- **Voyager**: Navigation framework
- **Compose Multiplatform**: UI framework
- **Kotlinx Coroutines**: Async operations
- **Napier**: Logging

## Testing

- Unit tests: `commonTest/`
- Android instrumented tests: `androidInstrumentedTest/`
- Platform-specific tests in respective source sets

## Documentation

API documentation is generated with Dokka. Run:

```bash
./gradlew :universal:dokkaHtml
```

Output: `build/dokka/html/`
