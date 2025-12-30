# Phase 5: UI Layer Implementation - Plan

**Version:** 1.0.0
**Status:** 📝 Planning
**Date:** 2025-11-17
**Estimate:** 15-20 hours

---

## Overview

Phase 5 implements the presentation/UI layer for WebAvanue using Jetpack Compose Multiplatform. This phase builds on the completed data layer (Phases 1-4) to create a functional browser interface.

**Goals:**
- Create Compose UI components for all browser features
- Implement ViewModels using BrowserRepository
- Set up navigation and routing
- Integrate Android WebView
- Create a working demo/sample app

---

## Architecture

### Layer Structure

```
universal/
├── presentation/                    # NEW: Compose UI layer
│   ├── viewmodel/                  # ViewModels (state management)
│   │   ├── BookmarkViewModel.kt
│   │   ├── DownloadViewModel.kt
│   │   ├── TabViewModel.kt
│   │   ├── HistoryViewModel.kt
│   │   └── SettingsViewModel.kt
│   │
│   ├── ui/                         # Compose UI components
│   │   ├── bookmark/
│   │   │   ├── BookmarkListScreen.kt
│   │   │   ├── BookmarkFolderScreen.kt
│   │   │   ├── AddBookmarkDialog.kt
│   │   │   └── BookmarkItem.kt
│   │   │
│   │   ├── download/
│   │   │   ├── DownloadListScreen.kt
│   │   │   ├── DownloadItem.kt
│   │   │   └── DownloadProgressBar.kt
│   │   │
│   │   ├── tab/
│   │   │   ├── TabBar.kt
│   │   │   ├── TabItem.kt
│   │   │   └── TabSwitcher.kt
│   │   │
│   │   ├── history/
│   │   │   ├── HistoryScreen.kt
│   │   │   └── HistoryItem.kt
│   │   │
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt
│   │   │   └── SettingsItem.kt
│   │   │
│   │   ├── browser/
│   │   │   ├── BrowserScreen.kt     # Main browser UI
│   │   │   ├── AddressBar.kt
│   │   │   ├── WebViewContainer.kt
│   │   │   └── BrowserToolbar.kt
│   │   │
│   │   └── common/                  # Reusable components
│   │       ├── SearchBar.kt
│   │       ├── EmptyState.kt
│   │       └── LoadingIndicator.kt
│   │
│   ├── navigation/                  # Navigation setup
│   │   ├── NavGraph.kt
│   │   ├── Screen.kt (sealed class)
│   │   └── NavigationManager.kt
│   │
│   └── theme/                       # Compose theme (optional)
│       ├── Color.kt
│       ├── Typography.kt
│       └── Theme.kt
│
├── domain/                          # Existing (Phase 3)
│   └── WebViewEngine.kt
│
└── platform/                        # Platform-specific
    └── android/
        └── AndroidWebViewEngine.kt  # NEW: Android WebView wrapper
```

---

## Implementation Plan

### Step 1: ViewModels (4-5 hours)

**Priority:** HIGH (required for UI)

#### 1.1 BookmarkViewModel
```kotlin
class BookmarkViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun loadBookmarks(folder: String? = null)
    fun searchBookmarks(query: String)
    fun addBookmark(url: String, title: String, folder: String?)
    fun removeBookmark(bookmarkId: String)
    fun moveToFolder(bookmarkId: String, folder: String?)
}
```

#### 1.2 DownloadViewModel
```kotlin
class DownloadViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<Download>>(emptyList())
    val activeDownloads: StateFlow<List<Download>> = _activeDownloads.asStateFlow()

    fun loadDownloads()
    fun loadActiveDownloads()
    fun cancelDownload(downloadId: String)
    fun retryDownload(downloadId: String)
    fun deleteDownload(downloadId: String)
    fun deleteAllDownloads()
}
```

#### 1.3 TabViewModel
```kotlin
class TabViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTab = MutableStateFlow<Tab?>(null)
    val activeTab: StateFlow<Tab?> = _activeTab.asStateFlow()

    fun loadTabs()
    fun createTab(url: String, title: String)
    fun closeTab(tabId: String)
    fun switchTab(tabId: String)
    fun updateTab(tab: Tab)
}
```

#### 1.4 HistoryViewModel
```kotlin
class HistoryViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun loadHistory()
    fun searchHistory(query: String)
    fun clearHistory()
    fun clearHistoryByTimeRange(start: Instant, end: Instant)
}
```

#### 1.5 SettingsViewModel
```kotlin
class SettingsViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<BrowserSettings?>(null)
    val settings: StateFlow<BrowserSettings?> = _settings.asStateFlow()

    fun loadSettings()
    fun updateSettings(settings: BrowserSettings)
    fun toggleDesktopMode()
}
```

**Files:** 5 ViewModels
**Estimate:** 4-5 hours

---

### Step 2: Core UI Components (6-8 hours)

**Priority:** HIGH

#### 2.1 Bookmark UI (2 hours)

**BookmarkListScreen.kt**
```kotlin
@Composable
fun BookmarkListScreen(
    viewModel: BookmarkViewModel = viewModel(),
    onBookmarkClick: (Bookmark) -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val folders by viewModel.folders.collectAsState()

    Column {
        SearchBar(
            query = viewModel.searchQuery.collectAsState().value,
            onQueryChange = { viewModel.searchBookmarks(it) }
        )

        LazyColumn {
            folders.forEach { folder ->
                item { FolderItem(folder, onFolderClick = {}) }
            }

            items(bookmarks) { bookmark ->
                BookmarkItem(
                    bookmark = bookmark,
                    onClick = { onBookmarkClick(bookmark) },
                    onDelete = { viewModel.removeBookmark(bookmark.id) }
                )
            }
        }
    }
}
```

**BookmarkItem.kt**
```kotlin
@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(bookmark.title) },
        supportingContent = { Text(bookmark.url) },
        leadingContent = {
            Icon(Icons.Default.Bookmark, "Bookmark")
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete")
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
```

**Files:** BookmarkListScreen.kt, BookmarkFolderScreen.kt, AddBookmarkDialog.kt, BookmarkItem.kt
**Estimate:** 2 hours

---

#### 2.2 Download UI (2 hours)

**DownloadListScreen.kt**
```kotlin
@Composable
fun DownloadListScreen(
    viewModel: DownloadViewModel = viewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    Column {
        if (activeDownloads.isNotEmpty()) {
            Text("Active Downloads", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(activeDownloads) { download ->
                    DownloadItem(
                        download = download,
                        onCancel = { viewModel.cancelDownload(download.id) },
                        onRetry = null
                    )
                }
            }
        }

        Text("All Downloads", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(downloads) { download ->
                DownloadItem(
                    download = download,
                    onCancel = if (download.isActive) {
                        { viewModel.cancelDownload(download.id) }
                    } else null,
                    onRetry = if (download.isFailed) {
                        { viewModel.retryDownload(download.id) }
                    } else null,
                    onDelete = { viewModel.deleteDownload(download.id) }
                )
            }
        }
    }
}
```

**DownloadItem.kt**
```kotlin
@Composable
fun DownloadItem(
    download: Download,
    onCancel: (() -> Unit)?,
    onRetry: (() -> Unit)?,
    onDelete: (() -> Unit)? = null
) {
    Column {
        ListItem(
            headlineContent = { Text(download.fileName) },
            supportingContent = {
                Column {
                    Text("Status: ${download.status}")
                    if (download.isActive) {
                        LinearProgressIndicator(
                            progress = download.progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${download.progressPercentage}%")
                    }
                }
            },
            trailingContent = {
                Row {
                    onCancel?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                    onRetry?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Refresh, "Retry")
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            }
        )
    }
}
```

**Files:** DownloadListScreen.kt, DownloadItem.kt, DownloadProgressBar.kt
**Estimate:** 2 hours

---

#### 2.3 Tab Management UI (2 hours)

**TabBar.kt**
```kotlin
@Composable
fun TabBar(
    viewModel: TabViewModel = viewModel(),
    onNewTab: () -> Unit
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyRow(modifier = Modifier.weight(1f)) {
            items(tabs) { tab ->
                TabItem(
                    tab = tab,
                    isActive = tab.id == activeTab?.id,
                    onClick = { viewModel.switchTab(tab.id) },
                    onClose = { viewModel.closeTab(tab.id) }
                )
            }
        }

        IconButton(onClick = onNewTab) {
            Icon(Icons.Default.Add, "New Tab")
        }
    }
}
```

**TabItem.kt**
```kotlin
@Composable
fun TabItem(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tab.title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(16.dp))
            }
        }
    }
}
```

**Files:** TabBar.kt, TabItem.kt, TabSwitcher.kt
**Estimate:** 2 hours

---

#### 2.4 Browser Screen (2-3 hours)

**BrowserScreen.kt**
```kotlin
@Composable
fun BrowserScreen(
    tabViewModel: TabViewModel = viewModel(),
    onNavigate: (Screen) -> Unit
) {
    val activeTab by tabViewModel.activeTab.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab bar
        TabBar(
            viewModel = tabViewModel,
            onNewTab = { tabViewModel.createTab("", "New Tab") }
        )

        // Address bar
        AddressBar(
            url = activeTab?.url ?: "",
            onUrlChange = { url ->
                activeTab?.let { tab ->
                    tabViewModel.updateTab(tab.copy(url = url))
                }
            },
            onBookmarkClick = { onNavigate(Screen.Bookmarks) },
            onDownloadClick = { onNavigate(Screen.Downloads) },
            onHistoryClick = { onNavigate(Screen.History) },
            onSettingsClick = { onNavigate(Screen.Settings) }
        )

        // WebView container
        activeTab?.let { tab ->
            WebViewContainer(
                url = tab.url,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

**AddressBar.kt**
```kotlin
@Composable
fun AddressBar(
    url: String,
    onUrlChange: (String) -> Unit,
    onBookmarkClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Enter URL") },
            singleLine = true
        )

        IconButton(onClick = onBookmarkClick) {
            Icon(Icons.Default.Bookmark, "Bookmarks")
        }
        IconButton(onClick = onDownloadClick) {
            Icon(Icons.Default.Download, "Downloads")
        }
        IconButton(onClick = onHistoryClick) {
            Icon(Icons.Default.History, "History")
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, "Settings")
        }
    }
}
```

**Files:** BrowserScreen.kt, AddressBar.kt, WebViewContainer.kt, BrowserToolbar.kt
**Estimate:** 2-3 hours

---

### Step 3: Navigation (1-2 hours)

**Screen.kt**
```kotlin
sealed class Screen(val route: String) {
    object Browser : Screen("browser")
    object Bookmarks : Screen("bookmarks")
    object Downloads : Screen("downloads")
    object History : Screen("history")
    object Settings : Screen("settings")
}
```

**NavGraph.kt**
```kotlin
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Browser.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Browser.route) {
            BrowserScreen(onNavigate = { screen ->
                navController.navigate(screen.route)
            })
        }

        composable(Screen.Bookmarks.route) {
            BookmarkListScreen(onBookmarkClick = { bookmark ->
                // Navigate back to browser with URL
                navController.popBackStack()
            })
        }

        composable(Screen.Downloads.route) {
            DownloadListScreen()
        }

        composable(Screen.History.route) {
            HistoryScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
```

**Files:** Screen.kt, NavGraph.kt, NavigationManager.kt
**Estimate:** 1-2 hours

---

### Step 4: Android WebView Integration (2-3 hours)

**AndroidWebViewEngine.kt**
```kotlin
actual class AndroidWebViewEngine(
    private val context: Context
) : WebViewEngine {

    private var webView: WebView? = null

    override fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    override fun goBack() {
        webView?.goBack()
    }

    override fun goForward() {
        webView?.goForward()
    }

    override fun reload() {
        webView?.reload()
    }

    override fun canGoBack(): Boolean = webView?.canGoBack() ?: false
    override fun canGoForward(): Boolean = webView?.canGoForward() ?: false

    fun createWebView(parent: ViewGroup): WebView {
        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    // Update loading state
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    // Update loaded state
                }
            }
            webView = this
        }
    }
}
```

**WebViewContainer.kt (Android)**
```kotlin
@Composable
fun WebViewContainer(
    url: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val engine = AndroidWebViewEngine(context)
            engine.createWebView(FrameLayout(context))
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}
```

**Files:** AndroidWebViewEngine.kt, WebViewContainer.kt
**Estimate:** 2-3 hours

---

### Step 5: Testing & Refinement (2-3 hours)

- Test all ViewModels with sample data
- Test UI components in isolation
- Test navigation flows
- Test WebView integration
- Fix bugs and polish UI

**Estimate:** 2-3 hours

---

## Implementation Order

1. ✅ **ViewModels** (4-5 hours) - Foundation for UI
2. ✅ **Tab Management UI** (2 hours) - Core browser feature
3. ✅ **Browser Screen + WebView** (2-3 hours) - Main screen
4. ✅ **Navigation** (1-2 hours) - Connect screens
5. ✅ **Bookmark UI** (2 hours) - Essential feature
6. ✅ **Download UI** (2 hours) - Important feature
7. ✅ **History UI** (1 hour) - Quick win
8. ✅ **Settings UI** (1 hour) - Configuration
9. ✅ **Testing & Polish** (2-3 hours) - Quality assurance

**Total:** 15-20 hours

---

## Success Criteria

### Functional Requirements
- ✅ User can create and close tabs
- ✅ User can navigate web pages via address bar
- ✅ User can view and manage bookmarks
- ✅ User can view download progress
- ✅ User can view browsing history
- ✅ User can configure browser settings

### Technical Requirements
- ✅ All ViewModels use BrowserRepository correctly
- ✅ UI updates reactively when data changes (Flow)
- ✅ Navigation works between all screens
- ✅ WebView renders web content correctly
- ✅ No memory leaks
- ✅ Smooth 60fps performance

### Quality Requirements
- ✅ Code follows Kotlin style guide
- ✅ All public APIs documented
- ✅ UI follows Material Design 3 guidelines
- ✅ Accessibility support (content descriptions, etc.)
- ✅ Error handling (no crashes)

---

## Risks & Mitigations

### Risk 1: WebView Performance
**Mitigation:** Use hardware acceleration, limit concurrent WebViews

### Risk 2: Memory Leaks
**Mitigation:** Properly dispose ViewModels, cancel coroutines

### Risk 3: UI Complexity
**Mitigation:** Break into small, reusable components

### Risk 4: Navigation State
**Mitigation:** Use Navigation Compose library, save state properly

---

## Out of Scope (Phase 6+)

- iOS WKWebView implementation
- Desktop JCEF implementation
- Advanced features (extensions, ad blocking, etc.)
- Syncing across devices
- Theming customization

---

## Documentation

After Phase 5 completion:
- Chapter 08: Universal Module (UI layer)
- Chapter 10: Tab Management (UI)
- Chapter 11: History & Favorites (UI)
- Chapter 12: Bookmark System (UI)
- Chapter 13: Download Manager (UI)
- Chapter 14: Browser Settings (UI)

---

**Version History:**
- 1.0.0 (2025-11-17) - Initial Phase 5 plan

**Author:** Manoj Jhawar <manoj@ideahq.net>
**License:** Proprietary - Augmentalis Inc.
