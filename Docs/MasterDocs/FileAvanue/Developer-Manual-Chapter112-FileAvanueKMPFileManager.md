# Chapter 112: FileAvanue — KMP Cross-Platform File Manager Module

**Module**: `Modules/FileAvanue/`
**Package**: `com.augmentalis.fileavanue`
**Created**: 2026-02-25
**Branch**: AvanueViews

---

## 1. Overview

FileAvanue is a Kotlin Multiplatform (KMP) file manager module providing a provider-based storage abstraction that unifies local, cloud, and network file browsing behind a single polymorphic interface (`IStorageProvider`). The module maximizes code sharing (~60%) by placing all state management, sorting, filtering, selection, and navigation logic in `commonMain` via `FileBrowserController`, while platform source sets contribute only storage provider implementations and UI composables.

### 1.1 Key Design Principles

| Principle | Implementation |
|-----------|---------------|
| Provider polymorphism | `IStorageProvider` interface — each backend (local/cloud/network) has genuinely different I/O patterns |
| Maximum code sharing | `FileBrowserController` in commonMain handles ALL state, sorting, filtering, selection |
| Platform-only I/O | Android MediaStore, Desktop java.nio.file, Darwin NSFileManager, Web File API |
| AvanueUI theming | All colors via `AvanueTheme.colors.*`, all 32 theme combinations supported |
| Voice-first AVID | Every interactive element has `Modifier.semantics { contentDescription = "Voice: ..." }` |
| Cockpit integration | `FrameContent.File` sealed class variant with full CommandBar wiring |

### 1.2 KMP Targets

| Target | Source Set | Provider | UI |
|--------|-----------|----------|----|
| Android | androidMain | `AndroidLocalStorageProvider` (MediaStore + java.io.File) | Compose + AvanueUI |
| Desktop (JVM) | desktopMain | `DesktopLocalStorageProvider` (java.nio.file) | Future |
| iOS | iosMain → darwinMain | `DarwinLocalStorageProvider` (NSFileManager) | Future |
| macOS | macosMain → darwinMain | `DarwinLocalStorageProvider` (NSFileManager) | Future |
| Web/JS | jsMain | `WebStorageProvider` (File System Access API) | Future |

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────┐
│                   commonMain                         │
│  ┌─────────────┐  ┌────────────────────────┐        │
│  │  FileItem    │  │ FileBrowserController  │        │
│  │  PathSegment │  │  ┌──────────────────┐  │        │
│  │  StorageInfo │  │  │ StateFlow<State>  │  │        │
│  │  FileCategory│  │  │ sort / filter    │  │        │
│  │  FileSortMode│  │  │ select / navigate│  │        │
│  └─────────────┘  │  │ search / delete  │  │        │
│                    │  └──────────────────┘  │        │
│  ┌─────────────┐  └────────────────────────┘        │
│  │IStorageProvider│                                  │
│  │  (interface)    │  ┌─────────────┐                │
│  └─────────────┘  │  │  MimeTypes   │                │
│                    │  └─────────────┘                │
│  expect fun createLocalStorageProvider()             │
└─────────────────────────────────────────────────────┘
         │                    │                  │
    ┌────┴────┐         ┌────┴────┐        ┌────┴────┐
    │ android │         │ desktop │        │ darwin  │
    │ Main    │         │ Main    │        │ Main    │
    │─────────│         │─────────│        │─────────│
    │MediaStore│         │java.nio │        │NSFile   │
    │java.io   │         │Files    │        │Manager  │
    │StatFs    │         │Attrs    │        │SearchPath│
    │──────────│         └─────────┘        └─────────┘
    │Compose UI│
    │Dashboard │
    │Browser   │
    │DetailSheet│
    └──────────┘
```

### 2.1 Data Flow

1. **Controller creates** with list of `IStorageProvider` implementations
2. **UI observes** `controller.state: StateFlow<FileBrowserState>`
3. **User action** (tap, sort, search) calls controller method
4. **Controller** calls provider, sorts/filters results, updates state
5. **UI recomposes** from new state snapshot

All state mutations flow through `FileBrowserController._state.update {}` — no direct UI state management beyond local ephemeral state (search text visibility, dropdown expansion).

---

## 3. Data Models (commonMain)

### 3.1 FileItem

**File**: `model/FileItem.kt`

Platform-agnostic representation of a file or directory. URI format is opaque and provider-dependent.

| Property | Type | Description |
|----------|------|-------------|
| `uri` | String | Platform URI or path (content://, absolute path, resource ID) |
| `name` | String | Display name |
| `mimeType` | String | MIME type (default: `*/*`) |
| `fileSizeBytes` | Long | Size in bytes |
| `isDirectory` | Boolean | Whether this is a directory |
| `dateCreated` | Long | Epoch milliseconds |
| `dateModified` | Long | Epoch milliseconds |
| `parentUri` | String | Parent directory URI |
| `thumbnailUri` | String | Image thumbnail URI for preview |
| `childCount` | Int | Number of children (-1 = unknown/not a dir) |
| `isHidden` | Boolean | Whether file starts with dot |
| `providerId` | String | Which provider owns this item |

**Computed properties**: `extension`, `isImage`, `isVideo`, `isAudio`, `isPdf`, `isArchive`, `isDocument`, `formattedSize`

### 3.2 FileBrowserState

**File**: `model/FileBrowserState.kt`

Complete UI state managed by `FileBrowserController`. All fields are `@Serializable` for potential persistence.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `currentPath` | String | `""` | Current directory path |
| `breadcrumbs` | List\<PathSegment\> | `[]` | Navigation breadcrumb trail |
| `items` | List\<FileItem\> | `[]` | Files in current view (sorted/filtered) |
| `selectedUris` | Set\<String\> | `{}` | Multi-select URIs |
| `isLoading` | Boolean | false | Loading indicator |
| `error` | String? | null | Error message |
| `sortMode` | FileSortMode | NAME_ASC | Current sort |
| `viewMode` | FileViewMode | LIST | List or grid |
| `showHidden` | Boolean | false | Show dot-files |
| `currentProviderId` | String | `"local"` | Active provider |
| `storageInfo` | StorageInfo? | null | Storage usage |
| `searchQuery` | String | `""` | Active search query |

**Derived**: `hasSelection`, `selectionCount`, `allSelected`

### 3.3 FileCategory

**File**: `model/FileCategory.kt`

Dashboard categories: `IMAGES`, `VIDEOS`, `AUDIO`, `DOCUMENTS`, `DOWNLOADS`, `RECENT`. Each has `displayName`, `mimePrefix`, and `iconName`.

### 3.4 FileSortMode / FileViewMode

Seven sort modes (Name A-Z/Z-A, Date Oldest/Newest, Size Smallest/Largest, Type A-Z). Two view modes (LIST, GRID). Directories always sort before files regardless of mode.

---

## 4. Provider Interface

**File**: `IStorageProvider.kt`

```kotlin
interface IStorageProvider {
    val providerId: String
    val displayName: String
    val type: StorageProviderType   // LOCAL, CLOUD, NETWORK
    val isConnected: Boolean

    suspend fun listFiles(path: String, showHidden: Boolean = false): List<FileItem>
    suspend fun getFileInfo(uri: String): FileItem?
    suspend fun getStorageInfo(): StorageInfo?
    fun getRootPath(): String
    fun getPathSegments(path: String): List<PathSegment>
    suspend fun searchFiles(path: String, query: String): List<FileItem>
    suspend fun getRecentFiles(limit: Int = 20): List<FileItem>
    suspend fun getCategoryFiles(category: FileCategory): List<FileItem>
    suspend fun deleteFile(uri: String): Boolean
    suspend fun renameFile(uri: String, newName: String): Boolean
    suspend fun createDirectory(parentPath: String, name: String): FileItem?
}
```

This is **real polymorphism** — each backend has fundamentally different I/O patterns, authentication, and capability sets. This is NOT indirection.

### 4.1 Platform Factory

```kotlin
// commonMain
expect fun createLocalStorageProvider(): IStorageProvider

// androidMain
actual fun createLocalStorageProvider(): IStorageProvider =
    AndroidLocalStorageProvider(appContext!!)

// desktopMain
actual fun createLocalStorageProvider(): IStorageProvider =
    DesktopLocalStorageProvider()

// darwinMain (shared iOS + macOS)
actual fun createLocalStorageProvider(): IStorageProvider =
    DarwinLocalStorageProvider()
```

---

## 5. FileBrowserController (commonMain)

**File**: `FileBrowserController.kt`

The shared controller managing ALL state transitions. Platform code never mutates state directly.

### 5.1 Key Operations

| Method | Description |
|--------|-------------|
| `loadDirectory(path)` | List files at path, apply sort/filter, update breadcrumbs |
| `navigateToRoot()` | Go to provider root |
| `navigateToParent()` | Pop one breadcrumb level |
| `navigateToBreadcrumb(segment)` | Jump to specific breadcrumb |
| `selectItem(uri)` | Toggle selection (add/remove) |
| `clearSelection()` | Clear all selections |
| `toggleSelectAll()` | Select all or deselect all |
| `setSortMode(mode)` | Re-sort current items |
| `setViewMode(mode)` | Switch list/grid |
| `toggleShowHidden()` | Toggle + reload directory |
| `setProvider(providerId)` | Switch provider + load root |
| `searchFiles(query)` | Recursive search from current path |
| `loadCategory(category)` | Load category files (e.g., all images) |
| `loadRecent()` | Load recently modified files |
| `deleteSelected()` | Delete all selected items |
| `getCategoryCounts()` | File count per category for dashboard |

### 5.2 Sorting Algorithm

Directories are always grouped first, then sorted independently:

```kotlin
private fun sortItems(items: List<FileItem>, mode: FileSortMode): List<FileItem> {
    val dirs = items.filter { it.isDirectory }
    val files = items.filter { !it.isDirectory }
    // Sort each group by mode
    return sortedDirs + sortedFiles
}
```

---

## 6. Platform Storage Providers

### 6.1 AndroidLocalStorageProvider

**File**: `androidMain/.../AndroidLocalStorageProvider.kt`

Dual-mode browsing:

| Mode | Trigger | Implementation |
|------|---------|---------------|
| Category | `path = "category:IMAGES"` | MediaStore queries (same pattern as `ImageGalleryScreen.queryImages()`) |
| Directory | `path = "/storage/emulated/0/Documents"` | `java.io.File.listFiles()` |

**MediaStore queries**: Standard projection (`_ID`, `DISPLAY_NAME`, `MIME_TYPE`, `SIZE`, `DATE_MODIFIED`) with content URI construction via `ContentUris.withAppendedId()`. Results capped at 500 per query.

**Search**: Recursive `java.io.File` traversal with depth limit (5 levels) and result cap (200 items) to prevent memory issues.

**Storage info**: `StatFs(Environment.getExternalStorageDirectory())` for `totalBytes` / `availableBytes`.

**Initialization**: `initFileAvanue(context)` must be called once before use (stores `applicationContext` in a module-level `lateinit`).

### 6.2 DesktopLocalStorageProvider

**File**: `desktopMain/.../DesktopLocalStorageProvider.kt`

Uses `java.nio.file.Files.list()` for directory listing, `BasicFileAttributes` for metadata, and `Files.probeContentType()` with `MimeTypes` fallback for MIME detection.

Category browsing maps to well-known directories: `~/Pictures`, `~/Videos`, `~/Music`, `~/Documents`, `~/Downloads`.

### 6.3 DarwinLocalStorageProvider

**File**: `darwinMain/.../DarwinLocalStorageProvider.kt`

Shared between iOS and macOS via the `darwinMain` source set hierarchy:

```
darwinMain
├── iosMain (iosX64, iosArm64, iosSimulatorArm64)
└── macosMain (macosX64, macosArm64)
```

Uses `NSFileManager.defaultManager.contentsOfDirectoryAtPath()` for listing, `attributesOfItemAtPath()` for metadata (`NSFileSize`, `NSFileModificationDate`, `NSFileCreationDate`), and `NSSearchPathForDirectoriesInDomains(NSDocumentDirectory)` for root path.

### 6.4 WebStorageProvider

**File**: `jsMain/.../WebStorageProvider.kt`

In-memory cache populated via `setCachedEntries()` from the UI layer. Designed for future integration with the File System Access API (`window.showDirectoryPicker()`) where supported. Uses `navigator.storage.estimate()` for storage info.

---

## 7. MimeTypes Registry (commonMain)

**File**: `MimeTypes.kt`

Consolidated 100+ extension-to-MIME mappings from two existing modules:

| Source | Types |
|--------|-------|
| `WebAvanue/DownloadQueue.kt` FilenameUtils | 30+ mappings |
| `HTTPAvanue/StaticFileMiddleware.kt` getContentType | 20+ mappings |
| New additions | 50+ additional types |

### 7.1 Categories

| Category | Count | Examples |
|----------|-------|---------|
| Images | 14 | jpg, png, webp, svg, heic, avif |
| Video | 10 | mp4, mkv, webm, avi, mov |
| Audio | 11 | mp3, ogg, flac, wav, aac |
| Documents | 12 | pdf, doc, docx, xls, pptx |
| Text/Code | 18 | txt, html, css, js, kt, py |
| Archives | 7 | zip, rar, 7z, tar, gz |
| Fonts | 4 | ttf, otf, woff, woff2 |
| Misc | 12 | json, xml, yaml, wasm, db |

### 7.2 API

```kotlin
MimeTypes.fromExtension("pdf")    // "application/pdf"
MimeTypes.fromFilename("doc.xlsx") // "application/vnd.openxmlformats-..."
MimeTypes.isImage("image/png")     // true
MimeTypes.isDocument("application/pdf") // true
```

---

## 8. Android UI Screens

All UI uses `AvanueTheme.colors.*` (never `MaterialTheme.colorScheme`) and includes AVID voice identifiers on every interactive element.

### 8.1 FileManagerDashboard

**File**: `androidMain/.../FileManagerDashboard.kt`

Shown when FileAvanue launches with no path (blank `FrameContent.File.path`).

| Section | Description |
|---------|-------------|
| StorageCard | LinearProgressIndicator showing used/free space, color changes to error above 85% |
| CategoryGrid | 3-column LazyVerticalGrid of category cards (Images, Videos, Audio, Documents, Downloads, Recent) with item counts |
| Browse Device | Single row card that navigates to the filesystem root |
| Recent Files | LazyColumn of the 10 most recently modified files |

### 8.2 FileBrowserScreen

**File**: `androidMain/.../FileBrowserScreen.kt`

Full file browser with navigation and file management.

| Component | Description |
|-----------|-------------|
| Toolbar | Back button (conditional), BreadcrumbBar (horizontal scroll), Search toggle, Sort dropdown, View mode toggle |
| BreadcrumbBar | Scrollable Row of PathSegment chips, "/" separators, current segment highlighted |
| SearchBar | Conditional TextField with trailing search icon |
| SelectionBar | Appears during multi-select: count, Select All/Deselect, Clear, Delete |
| Content - Grid | `LazyVerticalGrid(GridCells.Fixed(3))` with `FileItemCard` (Coil thumbnails for images, Material icons for others) |
| Content - List | `LazyColumn` with `FileItemRow` (40dp icon/thumbnail + name + size) |

**Click handling**: Tap navigates directories or opens files. Long-press enters selection mode and shows detail sheet. In selection mode, tap toggles selection.

### 8.3 FileDetailSheet

**File**: `androidMain/.../FileDetailSheet.kt`

`ModalBottomSheet` on long-press showing:

| Section | Content |
|---------|---------|
| Image preview | Coil SubcomposeAsyncImage (200dp height, only for image files) |
| Metadata | DetailRow pairs: Path, Size, Type, Modified, Created |
| Actions | Open (Button), Share (Intent.ACTION_SEND), Copy Path (clipboard), Delete (outlined, error-colored) |

---

## 9. Cockpit Integration

### 9.1 FrameContent.File

**File**: `Cockpit/.../model/FrameContent.kt`

```kotlin
@Serializable @SerialName("file")
data class File(
    val path: String = "",
    val viewMode: String = "list",
    val sortMode: String = "name_asc",
    val providerId: String = "local",
) : FrameContent() { override val typeId = TYPE_FILE }
```

### 9.2 Dashboard Registration

**File**: `Cockpit/.../model/DashboardState.kt`

```kotlin
DashboardModule(
    id = "fileavanue",
    displayName = "FileAvanue",
    subtitle = "File manager",
    iconName = "folder",
    contentType = "file",
    accentColorHex = 0xFF78909CL  // Blue-grey accent
)
```

### 9.3 CommandBar Wiring

**File**: `Cockpit/.../model/CommandBarState.kt`

| Addition | Details |
|----------|---------|
| `FILE_ACTIONS` enum entry | Content-specific state for file management |
| Parent mapping | `FILE_ACTIONS -> FRAME_ACTIONS` |
| Content-specific set | Added to `CONTENT_SPECIFIC_STATES` |
| Content type routing | `"file" -> FILE_ACTIONS` in `forContentType()` |

**File**: `Cockpit/.../ui/CommandBar.kt`

| ContentAction | CommandBar Chip | Description |
|---------------|----------------|-------------|
| `FILE_UP` | Go Up | Navigate to parent directory |
| `FILE_SORT` | Sort | Cycle sort modes |
| `FILE_VIEW_MODE` | View | Toggle list/grid |
| `FILE_SELECT_ALL` | Select All | Toggle selection |
| `FILE_SEARCH` | Search | Focus search field |

### 9.4 Content Rendering

**File**: `Cockpit/.../content/ContentRenderer.kt`

The `FrameContent.File` rendering branch:

1. Creates `AndroidLocalStorageProvider` via `remember { createLocalStorageProvider() }`
2. Creates `FileBrowserController` via `remember { FileBrowserController(listOf(provider)) }`
3. Handles `contentActionFlow` events (FILE_UP, FILE_SORT, FILE_VIEW_MODE, FILE_SELECT_ALL, FILE_SEARCH)
4. **Dashboard mode** (blank path): Renders `FileManagerDashboard`
5. **Browser mode** (path set): Renders `FileBrowserScreen`
6. **Detail sheet**: Triggered on long-press via `FileDetailSheet`

### 9.5 Frame Icon

**File**: `Cockpit/.../ui/FrameWindow.kt`

```kotlin
is FrameContent.File -> Icons.Default.Folder
```

---

## 10. Cross-Platform String.format

FileAvanue introduced an `expect/actual` pattern for `String.format()` since it's not available in Kotlin common stdlib:

| Platform | Implementation |
|----------|---------------|
| Android | `java.lang.String.format(format, *args)` |
| Desktop | `java.lang.String.format(format, *args)` |
| JS | Regex-based `%s`/`%d`/`%.Nf` replacement with `toFixed()` |
| Darwin | Manual precision formatting with `kotlin.math` helpers |

---

## 11. File Inventory

### 11.1 New Files (19)

| # | File | Source Set |
|---|------|-----------|
| 1 | `Modules/FileAvanue/build.gradle.kts` | Build |
| 2 | `.../commonMain/.../model/FileItem.kt` | commonMain |
| 3 | `.../commonMain/.../model/FileBrowserState.kt` | commonMain |
| 4 | `.../commonMain/.../model/FileCategory.kt` | commonMain |
| 5 | `.../commonMain/.../IStorageProvider.kt` | commonMain |
| 6 | `.../commonMain/.../FileBrowserController.kt` | commonMain |
| 7 | `.../commonMain/.../MimeTypes.kt` | commonMain |
| 8 | `.../commonMain/.../StorageProviderFactory.kt` | commonMain (expect) |
| 9 | `.../androidMain/.../AndroidLocalStorageProvider.kt` | androidMain |
| 10 | `.../androidMain/.../FileBrowserScreen.kt` | androidMain |
| 11 | `.../androidMain/.../FileManagerDashboard.kt` | androidMain |
| 12 | `.../androidMain/.../FileDetailSheet.kt` | androidMain |
| 13 | `.../desktopMain/.../DesktopLocalStorageProvider.kt` | desktopMain |
| 14 | `.../darwinMain/.../DarwinLocalStorageProvider.kt` | darwinMain |
| 15 | `.../jsMain/.../WebStorageProvider.kt` | jsMain |
| 16 | `.../androidMain/.../StringFormat.android.kt` | androidMain |
| 17 | `.../desktopMain/.../StringFormat.desktop.kt` | desktopMain |
| 18 | `.../jsMain/.../StringFormat.js.kt` | jsMain |
| 19 | `.../darwinMain/.../StringFormat.darwin.kt` | darwinMain |

### 11.2 Modified Files (8)

| File | Change |
|------|--------|
| `settings.gradle.kts` | `include(":Modules:FileAvanue")` |
| `Cockpit/build.gradle.kts` | FileAvanue dependency |
| `Cockpit/.../FrameContent.kt` | `File` sealed class variant |
| `Cockpit/.../DashboardState.kt` | FileAvanue module entry |
| `Cockpit/.../CommandBarState.kt` | `FILE_ACTIONS` state + wiring |
| `Cockpit/.../CommandBar.kt` | FILE_* actions + chips |
| `Cockpit/.../FrameWindow.kt` | File icon |
| `Cockpit/.../ContentRenderer.kt` | File rendering branch |

---

## 12. Future Phases

### Phase 5: Cloud Storage Providers

| Provider | API | OAuth |
|----------|-----|-------|
| Google Drive | REST v3 | Google Sign-In |
| Dropbox | REST v2 | OAuth2 PKCE |
| OneDrive | Microsoft Graph | MSAL |
| Box | REST v2 | OAuth2 |

Each implements `IStorageProvider` with `StorageProviderType.CLOUD`.

### Phase 6: Network Storage Providers

| Provider | Protocol |
|----------|----------|
| FTP | RFC 959 |
| FTPS | FTP + TLS |
| SFTP | SSH File Transfer |

Port from legacy AVANUE Workstation at `/Users/manoj_mbpm14/Downloads/Coding/Avanue/AVANUE/`.

### Phase 7: Advanced Features

- Clipboard operations (cut/copy/paste)
- Drag-and-drop between frames
- File preview (PDF, video, audio playback)
- Batch rename
- Favorites / bookmarks
- Recent directories quick-access

---

## 13. Voice Commands (AVID)

Every interactive element in FileAvanue UI has AVID voice identifiers:

| Voice Command | Element | Action |
|---------------|---------|--------|
| "click Images" | Category card | Load image category |
| "click Videos" | Category card | Load video category |
| "click Browse Device" | Dashboard row | Navigate to root |
| "click Go Up" | Toolbar button / CommandBar chip | Navigate to parent |
| "click Search" | Toolbar toggle | Show/execute search |
| "click Sort" | Toolbar dropdown | Open sort menu |
| "click Name A-Z" | Sort option | Set sort mode |
| "click View Mode" | Toolbar toggle | Switch list/grid |
| "click {filename}" | File row/card | Open file or navigate |
| "click Select All" | Selection bar / CommandBar | Select all items |
| "click Delete Selected" | Selection bar | Delete selected |
| "click Open" | Detail sheet button | Open file |
| "click Share" | Detail sheet button | Share via intent |
| "click Copy Path" | Detail sheet button | Copy URI to clipboard |
| "click Delete" | Detail sheet button | Delete file |
| "click {breadcrumb}" | Breadcrumb segment | Navigate to path |

---

## 14. Verification Checklist

| Check | Command / Action |
|-------|-----------------|
| Build | `./gradlew :Modules:FileAvanue:assembleDebug` |
| Cockpit launch | Dashboard > FileAvanue tile > FileManagerDashboard shows |
| Category browse | Tap "Images" > image grid from MediaStore |
| Directory browse | "Browse Device" > Downloads > file list with breadcrumbs |
| View modes | CommandBar "View" chip toggles list/grid |
| Sort modes | CommandBar "Sort" / toolbar sort dropdown |
| Multi-select | Long-press > select more > SelectionBar appears |
| File detail | Long-press > ModalBottomSheet with metadata + actions |
| Voice | "click Images", "click Documents", "click Go Up", "click Sort" |
| Desktop | `./gradlew :Modules:FileAvanue:desktopTest` |

---

*End of Chapter 112*
