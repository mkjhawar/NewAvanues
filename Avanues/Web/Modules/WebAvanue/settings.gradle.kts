pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

rootProject.name = "WebAvanue"
include(":app")
include(":BrowserCoreData")

// Universal module - DISABLED pending UI screen updates
//
// COMPLETED in this session:
// ✅ Added Voyager navigation (replacing Jetpack Navigation)
// ✅ Created Screen.kt with Voyager screens (BrowserScreenNav, etc.)
// ✅ Updated BrowserApp.kt with Voyager Navigator
// ✅ Updated SettingsScreen.kt for new BrowserSettings fields
// ✅ All ViewModels updated:
//    - TabViewModel: Uses BrowserCoreData types, TabUiState wrapper
//    - FavoriteViewModel: Uses Favorite/FavoriteFolder, Result API
//    - HistoryViewModel: Uses HistoryEntry, Result API
//    - SettingsViewModel: Uses new BrowserSettings with enums
//    - DownloadViewModel: Stub with Download model
// ✅ Download model added to BrowserCoreData
// ✅ Deleted old duplicate code in universal/src/kotlin/
// ✅ Deleted old NavGraph.kt and NavigationManager.kt
//
// REMAINING WORK (UI Screens):
// 1. BookmarkListScreen.kt: Update ViewModel method names:
//    - bookmarks → favorites
//    - searchBookmarks → searchFavorites
//    - loadBookmarks → loadFavorites
//    - removeBookmark → removeFavorite
//    - addBookmark → addFavorite
//    - folders is List<FavoriteFolder> not List<String>
// 2. BookmarkItem.kt/AddBookmarkDialog.kt:
//    - Remove duplicate Bookmark typealias
//    - Change folder → folderId
// 3. BrowserScreen.kt: Access tab fields via TabUiState
// 4. DownloadItem.kt: Add CANCELLED case to when expressions
// 5. HistoryItem.kt: Fix visitedAt type (Instant not Long)
// 6. Import fixes: Icons.Default.CloudDownload/Error/Pause/etc.
//
// UI screens have been updated - universal module enabled
include(":universal")
