# WebAvanue Module

## Overview
Cross-platform web browser built with Kotlin Multiplatform (KMP).

## Platforms
- Android
- iOS
- Desktop (JVM)

## Architecture
```
WebAvanue/
├── app/                  # Android app entry point
├── BrowserCoreData/      # Shared data layer (SQLDelight, repositories)
├── universal/            # Shared UI layer (Compose Multiplatform)
├── Android/              # Android-specific implementations
├── iOS/                  # iOS-specific implementations
└── Desktop/              # Desktop-specific implementations
```

## Tech Stack
- **UI**: Compose Multiplatform
- **Database**: SQLDelight
- **Async**: Kotlin Coroutines & Flow
- **DI**: Manual dependency injection

## Key Components
- `BrowserRepository` - Data access layer
- `TabManager` - Tab state management
- `FavoritesManager` - Bookmarks management
- `HistoryEntry` - Browsing history

## Build
```bash
./gradlew build
./gradlew :app:assembleDebug  # Android
```
