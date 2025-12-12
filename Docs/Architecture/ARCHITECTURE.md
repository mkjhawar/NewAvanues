# WebAvanue Architecture

## System Overview

WebAvanue is a cross-platform browser built with Kotlin Multiplatform (KMP), sharing 95% of code across Android, iOS, and Desktop platforms. The architecture follows clean architecture principles with clear separation between presentation, domain, and data layers.

```
┌─────────────────────────────────────────────────────────────┐
│                     WebAvanue Browser                        │
├─────────────────────────────────────────────────────────────┤
│  Platform: Android (Phase 1) | iOS (Phase 2) | Desktop (P2) │
│  Stack: Compose Multiplatform + Kotlin + SQLDelight         │
│  Architecture: MVVM + Repository Pattern + Clean Arch        │
└─────────────────────────────────────────────────────────────┘
```

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                          UI Layer                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  BrowserUI   │  │  SettingsUI  │  │  HistoryUI   │  (Compose)│
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                  │                    │
├─────────┼─────────────────┼──────────────────┼────────────────────┤
│         │    Presentation Layer              │                    │
│  ┌──────▼────────┐ ┌─────▼───────┐  ┌───────▼──────┐            │
│  │ TabViewModel  │ │ SettingsVM  │  │ HistoryVM    │  (State)   │
│  └──────┬────────┘ └─────┬───────┘  └───────┬──────┘            │
│         │                │                   │                    │
├─────────┼────────────────┼───────────────────┼────────────────────┤
│         │     Domain Layer                   │                    │
│  ┌──────▼────────────────▼───────────────────▼──────┐            │
│  │        BrowserRepository (Interface)             │  (Logic)   │
│  └──────┬───────────────────────────────────────────┘            │
│         │                                                         │
├─────────┼─────────────────────────────────────────────────────────┤
│         │      Data Layer                                         │
│  ┌──────▼─────────────────────────────────────┐                  │
│  │     BrowserRepositoryImpl                  │  (SQLDelight)    │
│  └──────┬─────────────────────────────────────┘                  │
│         │                                                         │
│  ┌──────▼─────────────────────────────────────┐                  │
│  │          SQLDelight Database               │  (Persistence)   │
│  │  ┌───────┐ ┌─────────┐ ┌────────┐         │                  │
│  │  │ Tabs  │ │Favorites│ │History │ ...     │                  │
│  │  └───────┘ └─────────┘ └────────┘         │                  │
│  └────────────────────────────────────────────┘                  │
└──────────────────────────────────────────────────────────────────┘
```

## Layer Architecture

### 1. Presentation Layer (Universal Module)

**Purpose**: UI components and state management

**Components**:
- **Composables**: Jetpack Compose UI (@Composable functions)
- **ViewModels**: Manage UI state and business logic
- **Navigation**: Voyager screens and navigation
- **Theme**: Material 3 theming and colors

**Location**: `Modules/WebAvanue/universal/src/commonMain/kotlin/presentation/`

**Key Classes**:

```
presentation/
├── ui/
│   ├── browser/
│   │   ├── BrowserScreen.kt         # Main browser UI
│   │   ├── AddressBar.kt            # URL input bar
│   │   ├── BottomCommandBar.kt      # Command buttons
│   │   └── WebViewContainer.kt      # WebView wrapper
│   ├── tab/
│   │   ├── TabSwitcherView.kt       # Tab overview
│   │   ├── TabBar.kt                # Tab strip
│   │   └── TabGroupDialog.kt        # Tab grouping
│   ├── settings/
│   │   ├── SettingsScreen.kt        # Settings UI
│   │   └── SitePermissionsScreen.kt # Permissions UI
│   └── history/
│       └── HistoryScreen.kt         # History UI
│
├── viewmodel/
│   ├── TabViewModel.kt              # Tab state management
│   ├── SettingsViewModel.kt         # Settings state
│   ├── HistoryViewModel.kt          # History state
│   └── FavoriteViewModel.kt         # Favorites state
│
└── navigation/
    └── BrowserScreen.kt             # Voyager screen definitions
```

**Threading**: Dispatchers.Main (UI updates)

**State Management**: StateFlow for reactive UI updates

### 2. Domain Layer (CoreData Module - Domain Package)

**Purpose**: Business logic and domain models

**Components**:
- **Models**: Data classes (Tab, Favorite, Settings, etc.)
- **Repository Interfaces**: Define data operations
- **Validation**: URL validation, input validation
- **Errors**: Domain-specific error types

**Location**: `Modules/WebAvanue/coredata/src/commonMain/kotlin/domain/`

**Key Models**:

```kotlin
// Tab - Browser tab entity
data class Tab(
    val id: String,
    val url: String,
    val title: String,
    val favicon: String?,
    val isActive: Boolean,
    val isPinned: Boolean,
    val lastAccessedAt: Instant,
    val sessionData: String?  // Navigation history
)

// BrowserSettings - Global browser configuration
data class BrowserSettings(
    val theme: Theme,
    val defaultSearchEngine: SearchEngine,
    val blockAds: Boolean,
    val enableJavaScript: Boolean,
    val useDesktopMode: Boolean,
    // ... 70+ settings
)

// Favorite - Bookmark entity
data class Favorite(
    val id: String,
    val url: String,
    val title: String,
    val folderId: String?,
    val visitCount: Int
)
```

**Repository Interface**:

```kotlin
interface BrowserRepository {
    // Tabs
    suspend fun createTab(tab: Tab): Result<Tab>
    suspend fun getAllTabs(): Result<List<Tab>>
    fun observeTabs(): Flow<List<Tab>>
    suspend fun updateTab(tab: Tab): Result<Unit>
    suspend fun closeTab(tabId: String): Result<Unit>

    // Favorites
    suspend fun addFavorite(favorite: Favorite): Result<Favorite>
    fun observeFavorites(): Flow<List<Favorite>>

    // Settings
    suspend fun getSettings(): Result<BrowserSettings>
    suspend fun updateSettings(settings: BrowserSettings): Result<Unit>
    fun observeSettings(): Flow<BrowserSettings>

    // ... more operations
}
```

### 3. Data Layer (CoreData Module - Data Package)

**Purpose**: Data persistence and repository implementation

**Components**:
- **SQLDelight Schema**: Database tables and queries
- **BrowserRepositoryImpl**: Repository implementation
- **Mappers**: Convert between DB and Domain models
- **Drivers**: Platform-specific database drivers

**Location**: `Modules/WebAvanue/coredata/src/commonMain/`

**Database Schema**:

```sql
-- Tabs table
CREATE TABLE IF NOT EXISTS tab (
    id TEXT PRIMARY KEY NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    favicon TEXT,
    is_active INTEGER NOT NULL DEFAULT 0,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    last_accessed_at INTEGER NOT NULL,
    position INTEGER NOT NULL,
    session_data TEXT,
    scroll_x_position INTEGER NOT NULL DEFAULT 0,
    scroll_y_position INTEGER NOT NULL DEFAULT 0,
    zoom_level INTEGER NOT NULL DEFAULT 3,
    is_desktop_mode INTEGER NOT NULL DEFAULT 0
);

-- Favorites table
CREATE TABLE IF NOT EXISTS favorite (
    id TEXT PRIMARY KEY NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    favicon TEXT,
    folder_id TEXT,
    created_at INTEGER NOT NULL,
    last_modified_at INTEGER NOT NULL,
    visit_count INTEGER NOT NULL DEFAULT 0,
    position INTEGER NOT NULL
);

-- History table
CREATE TABLE IF NOT EXISTS history_entry (
    id TEXT PRIMARY KEY NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    favicon TEXT,
    visited_at INTEGER NOT NULL,
    visit_count INTEGER NOT NULL DEFAULT 1,
    visit_duration INTEGER NOT NULL DEFAULT 0
);

-- Browser settings (single row)
CREATE TABLE IF NOT EXISTS browser_settings (
    id INTEGER PRIMARY KEY DEFAULT 1,
    theme TEXT NOT NULL DEFAULT 'SYSTEM',
    default_search_engine TEXT NOT NULL DEFAULT 'GOOGLE',
    block_ads INTEGER NOT NULL DEFAULT 0,
    block_trackers INTEGER NOT NULL DEFAULT 0,
    enable_javascript INTEGER NOT NULL DEFAULT 1,
    -- ... 70+ settings columns
);
```

**Threading**: Dispatchers.IO (all database operations)

**Reactive Updates**: StateFlows in repository emit changes

### 4. Platform Layer

**Purpose**: Platform-specific implementations

**Android (Phase 1)**:
- WebView integration
- DownloadManager
- EncryptedSharedPreferences (credential storage)
- Sentry crash reporting

**iOS (Phase 2)**:
- WKWebView integration
- Keychain (credential storage)

**Desktop (Phase 2)**:
- JCEF/Chromium Embedded Framework

**Location**: `Modules/WebAvanue/universal/src/androidMain/`

**Expect/Actual Pattern**:

```kotlin
// commonMain - Interface
expect class WebViewController {
    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
    fun evaluateJavaScript(script: String)
}

// androidMain - Android implementation
actual class WebViewController(private val webView: WebView) {
    actual fun loadUrl(url: String) {
        webView.loadUrl(url)
    }
    actual fun goBack() {
        webView.goBack()
    }
    // ...
}
```

## Module Structure

```
NewAvanues-WebAvanue/
│
├── Modules/WebAvanue/
│   │
│   ├── universal/                    # 95% shared code
│   │   ├── src/
│   │   │   ├── commonMain/           # Platform-independent
│   │   │   │   ├── kotlin/
│   │   │   │   │   └── presentation/
│   │   │   │   │       ├── ui/       # Compose UI
│   │   │   │   │       └── viewmodel/
│   │   │   │   └── resources/        # Shared assets
│   │   │   │
│   │   │   ├── androidMain/          # Android-specific
│   │   │   │   └── kotlin/platform/
│   │   │   │
│   │   │   ├── iosMain/              # iOS (Phase 2)
│   │   │   └── desktopMain/          # Desktop (Phase 2)
│   │   │
│   │   └── build.gradle.kts
│   │
│   ├── coredata/                     # Data layer
│   │   ├── src/
│   │   │   ├── commonMain/
│   │   │   │   ├── sqldelight/       # DB schema
│   │   │   │   └── kotlin/
│   │   │   │       ├── repository/
│   │   │   │       ├── domain/
│   │   │   │       └── mappers/
│   │   │   │
│   │   │   ├── androidMain/          # Android driver
│   │   │   └── iosMain/              # iOS driver
│   │   │
│   │   └── build.gradle.kts
│   │
│   └── domain/                       # Interfaces only
│       └── src/commonMain/kotlin/
│           ├── model/
│           ├── repository/
│           └── validation/
│
└── android/apps/webavanue/           # Android app
    ├── app/                          # Application module
    │   └── src/main/
    │       ├── kotlin/              # Android entry point
    │       ├── res/                  # Android resources
    │       └── AndroidManifest.xml
    │
    └── build.gradle.kts
```

## Threading Model

WebAvanue uses Kotlin Coroutines with structured concurrency:

```
┌─────────────────────────────────────────────────────────────┐
│                     Threading Model                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  UI Layer (Dispatchers.Main)                                │
│  ┌──────────────────────────────────────────┐               │
│  │  @Composable fun BrowserScreen()         │               │
│  │  ├─ val tabs by viewModel.tabs.collect() │               │
│  │  └─ Button(onClick = { vm.createTab() }) │               │
│  └──────────────┬───────────────────────────┘               │
│                 │                                            │
│  ViewModel (Dispatchers.Main)                               │
│  ┌──────────────▼───────────────────────────┐               │
│  │  fun createTab() {                       │               │
│  │    viewModelScope.launch { // Main       │               │
│  │      _isLoading.value = true             │               │
│  │      repository.createTab(tab) // ───────┼─────────┐     │
│  │        .onSuccess { /* Main */ }         │         │     │
│  │    }                                     │         │     │
│  │  }                                       │         │     │
│  └──────────────────────────────────────────┘         │     │
│                                                        │     │
│  Repository (Dispatchers.IO)                          │     │
│  ┌────────────────────────────────────────────────────▼───┐ │
│  │  suspend fun createTab() = withContext(Dispatchers.IO) {│ │
│  │    queries.insertTab(tab.toDbModel())                  │ │
│  │    refreshTabs() // Updates StateFlow on Main          │ │
│  │    Result.success(tab)                                 │ │
│  │  }                                                      │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Rules**:
1. **UI Operations**: Dispatchers.Main (Compose, StateFlow updates)
2. **Database Operations**: Dispatchers.IO (SQLDelight queries)
3. **CPU Work**: Dispatchers.Default (parsing, computation)
4. **Scope Management**: SupervisorJob for error isolation

See [ADR-004: Threading Model](adr/ADR-004-threading-model.md) for details.

## State Management

WebAvanue uses **StateFlow** for reactive state:

```kotlin
class TabViewModel(private val repository: BrowserRepository) {
    // State: All tabs
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    // State: Active tab
    private val _activeTab = MutableStateFlow<Tab?>(null)
    val activeTab: StateFlow<Tab?> = _activeTab.asStateFlow()

    init {
        // Observe repository changes
        viewModelScope.launch {
            repository.observeTabs().collect { tabs ->
                _tabs.value = tabs
            }
        }
    }
}

// UI observes state
@Composable
fun BrowserScreen(viewModel: TabViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    // UI automatically recomposes when state changes
}
```

**Flow**: Repository → ViewModel → UI

## Navigation Architecture

WebAvanue uses **Voyager** for screen navigation:

```
┌─────────────────────────────────────────────────────┐
│              Navigation Flow                         │
├─────────────────────────────────────────────────────┤
│                                                      │
│  BrowserScreen (Main)                               │
│  ┌────────────────────────────────┐                 │
│  │  TopBar: AddressBar            │                 │
│  │  Content: WebView              │                 │
│  │  BottomBar: CommandBar         │                 │
│  │    ├─ Settings Button ──────────┼──┐             │
│  │    ├─ History Button ───────────┼──┼──┐          │
│  │    └─ Downloads Button ─────────┼──┼──┼──┐       │
│  └────────────────────────────────┘  │  │  │       │
│                                       │  │  │       │
│  ┌────────────────────────────────┐  │  │  │       │
│  │  SettingsScreen                │◄─┘  │  │       │
│  │  (navigator.push(Settings))    │     │  │       │
│  └────────────────────────────────┘     │  │       │
│                                          │  │       │
│  ┌────────────────────────────────┐     │  │       │
│  │  HistoryScreen                 │◄────┘  │       │
│  │  (navigator.push(History))     │        │       │
│  └────────────────────────────────┘        │       │
│                                             │       │
│  ┌────────────────────────────────┐        │       │
│  │  DownloadScreen                │◄───────┘       │
│  │  (navigator.push(Download))    │                │
│  └────────────────────────────────┘                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**Pattern**:
```kotlin
// Screen definition
class BrowserScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        BrowserUI(
            onOpenSettings = { navigator.push(SettingsScreen()) }
        )
    }
}
```

See [ADR-002: Voyager Navigation](adr/ADR-002-voyager.md) for details.

## WebView Architecture

WebAvanue uses **WebViewLifecycle** for managing WebView instances:

```
Tab Lifecycle:
┌─────────────────────────────────────────────────────┐
│                                                      │
│  Tab Created          Tab Visible       Tab Hidden  │
│  (No WebView)         (WebView Active)  (Paused)    │
│                                                      │
│  ┌─────────┐         ┌─────────┐       ┌─────────┐ │
│  │Tab(id=1)│  ─────► │WebView  │ ────► │WebView  │ │
│  │url: ... │ create  │ + Tab   │ pause │(paused) │ │
│  └─────────┘         └─────────┘       └─────────┘ │
│      │                    │                  │      │
│      │ Memory: ~1KB       │ Memory: ~40MB    │~40MB │
│      └────────────────────┴──────────────────┘      │
│                           │                         │
│                           │ destroy (low memory)    │
│                           ▼                         │
│                      ┌─────────┐                    │
│                      │Tab(id=1)│                    │
│                      │(no WV)  │                    │
│                      └─────────┘                    │
│                                                      │
└──────────────────────────────────────────────────────┘
```

**Lifecycle States**:
1. **Created**: Tab exists, no WebView (saves memory)
2. **Active**: WebView created and visible
3. **Paused**: WebView backgrounded (JavaScript paused)
4. **Destroyed**: WebView released (can recreate later)

See [ADR-005: WebView Lifecycle](adr/ADR-005-webview-lifecycle.md) for details.

## Component Diagrams

### Tab Creation Flow

```
User                   ViewModel              Repository            Database
  │                        │                       │                    │
  │  Click "New Tab"       │                       │                    │
  ├───────────────────────►│                       │                    │
  │                        │  createTab(url)       │                    │
  │                        ├──────────────────────►│                    │
  │                        │                       │  insertTab(dbTab)  │
  │                        │                       ├───────────────────►│
  │                        │                       │                    │
  │                        │                       │◄───────────────────┤
  │                        │                       │  Success           │
  │                        │                       │                    │
  │                        │                       │  observeTabs()     │
  │                        │                       ├───────────────────►│
  │                        │                       │                    │
  │                        │  tabs: Flow<List<Tab>>│◄───────────────────┤
  │                        │◄──────────────────────┤  [Tab1, Tab2]      │
  │                        │                       │                    │
  │  UI Recomposes         │                       │                    │
  │◄───────────────────────┤                       │                    │
  │  (new tab visible)     │                       │                    │
  │                        │                       │                    │
```

### Settings Update Flow

```
User                 ViewModel          Repository         Database       WebView
  │                      │                   │                 │              │
  │  Toggle Dark Mode    │                   │                 │              │
  ├─────────────────────►│                   │                 │              │
  │                      │ updateSettings()  │                 │              │
  │                      ├──────────────────►│                 │              │
  │                      │                   │ updateSettings()│              │
  │                      │                   ├────────────────►│              │
  │                      │                   │                 │              │
  │                      │                   │◄────────────────┤              │
  │                      │                   │  Success        │              │
  │                      │                   │                 │              │
  │                      │  settings: Flow   │                 │              │
  │                      │◄──────────────────┤                 │              │
  │                      │  (theme=DARK)     │                 │              │
  │                      │                   │                 │              │
  │                      │  Apply to WebView │                 │              │
  │                      ├───────────────────┴─────────────────┴─────────────►│
  │                      │  webView.setBackgroundColor(dark)                  │
  │                      │                                                    │
  │  UI Recomposes       │                                                    │
  │◄─────────────────────┤                                                    │
  │  (dark theme)        │                                                    │
```

## Design Patterns

### 1. Repository Pattern

Abstracts data sources (database, network) behind interface.

```kotlin
interface BrowserRepository {
    suspend fun getAllTabs(): Result<List<Tab>>
}

class BrowserRepositoryImpl(db: Database) : BrowserRepository {
    override suspend fun getAllTabs() = withContext(Dispatchers.IO) {
        // Implementation details hidden
    }
}
```

### 2. MVVM (Model-View-ViewModel)

Separates UI from business logic.

```kotlin
// Model
data class Tab(val id: String, val url: String)

// ViewModel
class TabViewModel(repository: BrowserRepository) {
    val tabs: StateFlow<List<Tab>> = ...
}

// View
@Composable
fun BrowserScreen(viewModel: TabViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    // Render UI
}
```

### 3. Dependency Injection (Koin)

Provides dependencies without manual instantiation.

```kotlin
val appModule = module {
    single { BrowserDatabase(get()) }
    single<BrowserRepository> { BrowserRepositoryImpl(get()) }
    factory { TabViewModel(get()) }
}
```

### 4. Observer Pattern (Flow)

Reactive updates from data source to UI.

```kotlin
repository.observeTabs()  // Flow<List<Tab>>
    .collect { tabs ->
        _tabs.value = tabs  // UI updates automatically
    }
```

### 5. Facade Pattern

SimplifyWebView API with WebViewController.

```kotlin
class WebViewController(private val webView: WebView) {
    fun loadUrl(url: String) { /* complex WebView setup */ }
    fun goBack() { /* navigation logic */ }
}
```

## Performance Optimizations

### 1. Lazy Loading

Only load data when needed:
- 10 recent tabs on startup (not all 100 tabs)
- WebView created only when tab visible

### 2. Database Indexing

```sql
CREATE INDEX idx_tab_last_accessed ON tab(last_accessed_at DESC);
CREATE INDEX idx_history_visited_at ON history_entry(visited_at DESC);
```

### 3. ACID Transactions

Batch operations for consistency:

```kotlin
database.transaction {
    tabIds.forEach { queries.deleteTab(it) }
}
```

### 4. StateFlow Batching

Combine multiple state updates:

```kotlin
data class CombinedUiState(
    val tabs: List<Tab>,
    val activeTab: Tab?,
    val error: String?
)
```

## Security Architecture

See [SECURITY.md](../SECURITY.md) for full details.

**Key Features**:
- AES-256 database encryption (SQLCipher)
- EncryptedSharedPreferences for credentials
- Certificate pinning for network security
- ProGuard obfuscation

## Testing Strategy

### Unit Tests (commonTest)

Test business logic without platform dependencies.

```kotlin
@Test
fun `createTab should add tab to list`() = runTest {
    val repository = FakeBrowserRepository()
    val viewModel = TabViewModel(repository)

    viewModel.createTab("https://example.com")

    assertEquals(1, viewModel.tabs.value.size)
}
```

### Integration Tests (androidInstrumentedTest)

Test database operations and UI.

```kotlin
@Test
fun browserRepositoryImpl_createTab_insertsToDatabase() = runTest {
    val db = createTestDatabase()
    val repository = BrowserRepositoryImpl(db)

    repository.createTab(Tab.create(url = "https://test.com"))

    val tabs = repository.getAllTabs().getOrThrow()
    assertEquals(1, tabs.size)
}
```

## References

- [ADR-001: SQLDelight](adr/ADR-001-sqldelight.md)
- [ADR-002: Voyager](adr/ADR-002-voyager.md)
- [ADR-003: KMP Architecture](adr/ADR-003-kmp-architecture.md)
- [ADR-004: Threading Model](adr/ADR-004-threading-model.md)
- [ADR-005: WebView Lifecycle](adr/ADR-005-webview-lifecycle.md)
- [DEVELOPMENT.md](../Development/DEVELOPMENT.md)
- [SECURITY.md](../../SECURITY.md)

## Revision History

| Version | Date       | Changes                      |
|---------|------------|------------------------------|
| 1.0     | 2025-12-12 | Initial architecture document |
