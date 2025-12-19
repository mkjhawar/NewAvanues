# AvaElements Master Plan & Roadmap

**Project**: Avanues AvaElements Cross-Platform UI Library
**Created**: 2025-10-29
**Last Updated**: 2025-10-29
**Current Phase**: Phase 1 Complete ✅ → Phase 2 Starting

---

## 📋 Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Status](#current-status)
3. [Master Roadmap](#master-roadmap)
4. [New Requirements](#new-requirements)
5. [Pending Tasks](#pending-tasks)
6. [Technical Architecture](#technical-architecture)
7. [Resource Allocation](#resource-allocation)

---

## Executive Summary

AvaElements is a comprehensive cross-platform UI library supporting 7 major design systems with dual syntax (DSL + YAML). The project follows a 20-week implementation plan across 5 phases.

### Vision
- **Universal Theme System**: One theme across all Avanues apps, with per-app override capability
- **Visual Theme Builder**: Live preview theme editor with real-time component visualization
- **Asset Management**: User-uploadable icon and image libraries with versioning
- **50 Components**: Complete UI component library
- **7 Platform Themes**: iOS, macOS, visionOS, Windows, Android XR, Material 3, Samsung One UI

### Current Progress
- ✅ **Phase 1 Complete**: Foundation infrastructure (Weeks 1-4)
- ⏳ **Phase 2 Starting**: Platform renderers + Theme Builder (Weeks 5-8)

---

## Current Status

### ✅ Completed (Phase 1)

#### Infrastructure
- [x] Type system (Color, Size, Spacing, Font, etc.) - 550 lines
- [x] Component architecture with modifier system - 180 lines
- [x] Theme system with 7 platform themes - 450 lines
- [x] DSL builder with type-safe scopes - 800 lines
- [x] YAML parser with bidirectional conversion - 700 lines
- [x] Build system (KMP: Android, iOS, Desktop) - configured
- [x] Documentation (3 major docs, 1 summary) - 4,000+ lines

#### Components (13/50)
- [x] Button, Text, TextField, Checkbox, Switch, Icon, Image, Card
- [x] Column, Row, Container, ScrollView, Card (layout)

#### Examples
- [x] 4 DSL examples (Login, Settings, Dashboard, visionOS)
- [x] 3 YAML examples (login-screen, settings-screen, dashboard)

#### Documentation
- [x] MAGICELEMENTS_SPECIFICATION.md (2,178 lines)
- [x] PLATFORM_THEMES_SPEC.md (988 lines)
- [x] PHASE1_COMPLETION_SUMMARY.md (508 lines)
- [x] examples/README.md

#### Git Commits
- [x] bd6ef8c: KMP conversion analysis
- [x] 4563be3: Comprehensive specification
- [x] d1544e5: Phase 1 foundation (3,597 lines added)
- [x] 801018a: Phase 1 summary

---

## Master Roadmap

### Phase 1: Foundation ✅ COMPLETE
**Timeline**: Weeks 1-4 (COMPLETED)
**Status**: ✅ 100% Complete

#### Deliverables
- ✅ Type system with all primitives and composites
- ✅ Component architecture with modifiers
- ✅ Theme system (7 platforms defined)
- ✅ DSL builder
- ✅ YAML parser
- ✅ 8 foundation components
- ✅ 5 layout components
- ✅ Examples and documentation

**Code Stats**: 3,030 lines | 13 components | 7 themes specified

---

### Phase 2: Platform Renderers + Theme Builder 🔄 IN PROGRESS
**Timeline**: Weeks 5-8 (CURRENT PHASE)
**Status**: 🔄 0% Complete
**Priority**: HIGH

#### Original Goals (from spec)
1. Implement 3 platform themes
2. Theme system architecture
3. Material 3, iOS 26, Windows 11 renderers

#### **NEW REQUIREMENTS** (Added 2025-10-29)

##### 2A. Visual Theme Builder (NEW)
**Goal**: Interactive theme editor with live preview

**Features**:
- Real-time component preview as user edits theme
- Visual color picker with palette generation
- Typography editor with font previews
- Spacing/sizing visual adjustments
- Material effects editor (glass, mica, shadows)
- Export themes to DSL/YAML/JSON
- Import existing themes
- Theme validation

**Components Needed**:
1. Theme Editor UI (Compose Desktop or Web-based)
2. Live Preview Canvas
3. Property Inspector Panel
4. Component Gallery
5. Export/Import System

**Technical Requirements**:
- Hot-reload for instant preview
- Undo/redo system
- Theme versioning
- Collaborative editing (future)

##### 2B. Universal Theme System (NEW)
**Goal**: Centralized theme management across all Avanues apps

**Architecture**:
```
Avanues/
├── Universal/
│   └── Core/
│       └── ThemeManager/
│           ├── UniversalTheme.kt          # Global theme singleton
│           ├── ThemeRepository.kt         # Persistence layer
│           ├── ThemeSync.kt               # Cross-app synchronization
│           └── ThemeOverride.kt           # Per-app overrides
└── apps/
    ├── app1/ → uses Universal theme or overrides
    ├── app2/ → uses Universal theme or overrides
    └── app3/ → uses Universal theme or overrides
```

**Features**:
- Single source of truth for Avanues theme
- Per-app theme override system
- Theme inheritance hierarchy
- Runtime theme switching
- Theme synchronization across apps
- Cloud backup (optional)

**Persistence**:
```kotlin
// Universal theme (applies to all apps)
UniversalTheme.setGlobal(Themes.iOS26LiquidGlass)

// Per-app override
AppTheme.override(
    appId = "avanues.mobile",
    theme = customTheme
)

// Check effective theme
val effectiveTheme = ThemeManager.resolveTheme(appId)
// Returns: app override if exists, else universal theme
```

##### 2C. Asset Management System (NEW)
**Goal**: User-uploadable icon and image libraries

**Icon Library Management**:
```kotlin
interface IconLibrary {
    val id: String
    val name: String
    val version: String
    val icons: List<IconAsset>
    val license: License
}

data class IconAsset(
    val name: String,
    val svg: String?,           // Vector
    val png: ByteArray?,        // Raster
    val sizes: List<Int>,       // [16, 24, 32, 48, 64, 128, 256]
    val tags: List<String>,
    val categories: List<String>
)
```

**Image Library Management**:
```kotlin
interface ImageLibrary {
    val id: String
    val name: String
    val images: List<ImageAsset>
}

data class ImageAsset(
    val name: String,
    val path: String,
    val format: ImageFormat,    // PNG, JPG, WebP, HEIC
    val dimensions: Dimensions,
    val sizeBytes: Long,
    val thumbnail: ByteArray?,
    val metadata: ImageMetadata
)
```

**Asset Uploader Features**:
- Drag-and-drop upload
- Bulk import (ZIP, folder)
- Format conversion (SVG ↔ PNG, etc.)
- Automatic thumbnail generation
- Size optimization
- CDN integration
- Version control
- Search and filtering
- Usage tracking

**Storage Architecture**:
```
Avanues/
├── Universal/
│   └── Assets/
│       ├── Icons/
│       │   ├── MaterialIcons/
│       │   ├── FontAwesome/
│       │   ├── CustomLibrary1/
│       │   └── manifest.json
│       └── Images/
│           ├── Backgrounds/
│           ├── Illustrations/
│           ├── Photos/
│           └── manifest.json
```

**Asset Access**:
```kotlin
// DSL usage
Icon("custom:user-avatar") {
    library = "CustomLibrary1"
    tint = Color.Primary
}

Image("backgrounds:gradient-blue") {
    library = "Backgrounds"
    contentScale = ContentScale.Crop
}

// YAML usage
Icon:
  name: "custom:user-avatar"
  library: "CustomLibrary1"
  tint: "#007AFF"
```

#### Updated Phase 2 Deliverables
1. ✅ Theme system architecture (already done)
2. 🆕 Visual Theme Builder with live preview
3. 🆕 Universal Theme Manager
4. 🆕 Asset Management System
5. 🆕 Icon Library Uploader
6. 🆕 Image Library Uploader
7. Android Compose renderer
8. iOS SwiftUI bridge
9. Desktop Compose renderer
10. Material 3 theme implementation

**Estimated Effort**: 6-8 weeks (extended from original 4 weeks)

---

### Phase 3: Advanced Components
**Timeline**: Weeks 9-12 (originally), now Weeks 13-16
**Status**: ⏳ Pending
**Priority**: MEDIUM

#### Goals
- 35 additional components
- Form components (8): Radio, Slider, Dropdown, DatePicker, TimePicker, FileUpload, SearchBar, Rating
- Feedback components (7): Dialog, Toast, Alert, ProgressBar, Spinner, Badge, Tooltip
- Navigation components (6): AppBar, BottomNav, Tabs, Drawer, Breadcrumb, Pagination
- Data Display components (14): Table, List, Accordion, Stepper, Timeline, TreeView, Carousel, Avatar, etc.

#### Deliverables
- 43 total components (8 foundation + 35 advanced)
- Component documentation
- Usage examples

**Estimated Effort**: 4 weeks

---

### Phase 4: Remaining Platform Themes
**Timeline**: Weeks 17-20 (adjusted)
**Status**: ⏳ Pending
**Priority**: MEDIUM

#### Goals
- Complete remaining 4 platform themes
- macOS 26 Tahoe (Week 17)
- visionOS 2 Spatial Glass (Week 18)
- Android XR Spatial Material (Week 19)
- Samsung One UI 7 (Week 20)

#### Deliverables
- All 7 platform themes fully implemented
- Theme switcher
- Cross-platform theme testing

**Estimated Effort**: 4 weeks

---

### Phase 5: Advanced Components & Polish
**Timeline**: Weeks 21-24 (adjusted)
**Status**: ⏳ Pending
**Priority**: LOW

#### Goals
- 7 specialized components: ColorPicker, CodeEditor, Map, Chart, RichTextEditor, DragDrop, Video
- Testing infrastructure
- Performance optimization
- Accessibility improvements
- API documentation
- Release preparation

#### Deliverables
- 50 total components
- Full test coverage
- Production-ready library
- Published documentation

**Estimated Effort**: 4 weeks

---

## New Requirements (Detailed)

### 1. Visual Theme Builder

#### Architecture
```
ThemeBuilder/
├── UI/
│   ├── EditorWindow.kt           # Main editor window
│   ├── PreviewCanvas.kt          # Live component preview
│   ├── PropertyInspector.kt      # Theme property editor
│   ├── ComponentGallery.kt       # All 50 components
│   ├── ColorPicker.kt            # Advanced color picker
│   └── TypographyEditor.kt       # Font/text editor
├── Engine/
│   ├── ThemeCompiler.kt          # Compile theme to code
│   ├── HotReload.kt              # Live preview updates
│   ├── ThemeValidator.kt         # Validation rules
│   └── ThemeExporter.kt          # Export to DSL/YAML/JSON
└── State/
    ├── ThemeState.kt             # Current editing state
    ├── UndoRedoManager.kt        # History management
    └── AutoSave.kt               # Periodic saves
```

#### UI Layout
```
┌─────────────────────────────────────────────────────────────┐
│ Theme Builder - Avanues                          [_][□][X] │
├─────────────────┬───────────────────────┬───────────────────┤
│ Component       │   Preview Canvas      │ Properties        │
│ Gallery         │                       │                   │
│                 │  ┌─────────────────┐  │ Color Scheme      │
│ □ Button        │  │ Login Screen    │  │ ├─ Primary       │
│ □ Text          │  │                 │  │ │  ├─ Main: #007AFF│
│ □ TextField     │  │  Welcome Back   │  │ │  └─ Light: #E5F2FF│
│ □ Checkbox      │  │                 │  │ ├─ Secondary      │
│ □ Switch        │  │  [TextField]    │  │ └─ Error          │
│ □ ...           │  │  [TextField]    │  │                   │
│                 │  │  [Sign In]      │  │ Typography        │
│ Layouts         │  │                 │  │ ├─ Title: 24px    │
│ □ Column        │  └─────────────────┘  │ ├─ Body: 16px     │
│ □ Row           │                       │ └─ Caption: 12px  │
│ □ Card          │  Component: Button    │                   │
│                 │  State: Default       │ Spacing           │
│ Themes          │                       │ ├─ Small: 8dp     │
│ • Material 3    │  Preview Updates      │ ├─ Medium: 16dp   │
│ • iOS 26        │  Automatically ✓      │ └─ Large: 24dp    │
│ • Windows 11    │                       │                   │
│                 │  [Export Theme ▼]     │ [Apply] [Reset]   │
└─────────────────┴───────────────────────┴───────────────────┘
```

#### Features
1. **Live Preview**
   - Real-time updates as properties change
   - Multiple component states (default, hover, pressed, disabled)
   - Multiple screen sizes (mobile, tablet, desktop)
   - Dark/light mode toggle

2. **Color System**
   - Visual color picker with HSL/RGB/Hex
   - Palette generator (complementary, analogous, triadic)
   - Accessibility checker (WCAG contrast ratios)
   - Material 3 dynamic color from seed color
   - Color history and favorites

3. **Typography**
   - Font family selector (system + custom)
   - Size, weight, style controls
   - Line height and letter spacing
   - Preview text editor
   - Responsive scaling

4. **Export Options**
   - DSL (Kotlin code)
   - YAML (declarative)
   - JSON (configuration)
   - CSS Variables (web)
   - Platform-specific (Android XML, iOS plist)

5. **Collaboration** (Future)
   - Share theme via URL
   - Team libraries
   - Version control integration
   - Comments and annotations

#### Implementation Priority
1. **Week 5-6**: Core editor UI and preview canvas
2. **Week 7**: Color and typography editors
3. **Week 8**: Export system and validation

---

### 2. Universal Theme System

#### Architecture

##### ThemeManager
```kotlin
object ThemeManager {
    // Global Avanues theme
    private var universalTheme: Theme = Themes.Material3Light

    // Per-app overrides
    private val appThemes = mutableMapOf<String, Theme>()

    // Set universal theme (affects all apps)
    fun setUniversalTheme(theme: Theme) {
        universalTheme = theme
        notifyAllApps()
        persist()
    }

    // Override for specific app
    fun setAppTheme(appId: String, theme: Theme) {
        appThemes[appId] = theme
        notifyApp(appId)
        persist()
    }

    // Clear app override (revert to universal)
    fun clearAppTheme(appId: String) {
        appThemes.remove(appId)
        notifyApp(appId)
        persist()
    }

    // Get effective theme for app
    fun getTheme(appId: String): Theme {
        return appThemes[appId] ?: universalTheme
    }

    // Check if app has override
    fun hasOverride(appId: String): Boolean {
        return appThemes.containsKey(appId)
    }
}
```

##### Theme Inheritance
```kotlin
data class ThemeConfig(
    val source: ThemeSource,
    val theme: Theme,
    val overrides: Map<String, Any> = emptyMap()
)

enum class ThemeSource {
    UNIVERSAL,      // Uses Avanues universal theme
    APP_OVERRIDE,   // App-specific theme
    USER_CUSTOM     // User-created theme
}

// Usage in app
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Option 1: Use universal theme
        val theme = ThemeManager.getTheme("com.avanues.mobile")

        // Option 2: Override for this app only
        ThemeManager.setAppTheme(
            appId = "com.avanues.mobile",
            theme = Themes.iOS26LiquidGlass
        )

        // Option 3: Partial override (inherit most, change some)
        val customTheme = ThemeManager.getTheme("com.avanues.mobile").copy(
            colorScheme = myCustomColors
        )
        ThemeManager.setAppTheme("com.avanues.mobile", customTheme)
    }
}
```

##### Persistence
```kotlin
interface ThemeRepository {
    suspend fun saveUniversalTheme(theme: Theme)
    suspend fun loadUniversalTheme(): Theme?

    suspend fun saveAppTheme(appId: String, theme: Theme)
    suspend fun loadAppTheme(appId: String): Theme?
    suspend fun loadAllAppThemes(): Map<String, Theme>

    suspend fun deleteAppTheme(appId: String)
}

class LocalThemeRepository : ThemeRepository {
    // Stores themes in:
    // - Universal/Core/ThemeManager/themes/universal.json
    // - Universal/Core/ThemeManager/themes/apps/{appId}.json
}

class CloudThemeRepository : ThemeRepository {
    // Syncs to cloud storage (Firebase, AWS, etc.)
    // Enables theme sync across devices
}
```

##### Theme Synchronization
```kotlin
class ThemeSync(
    private val localRepo: LocalThemeRepository,
    private val cloudRepo: CloudThemeRepository
) {
    // Sync universal theme across all devices
    suspend fun syncUniversal() {
        val local = localRepo.loadUniversalTheme()
        val cloud = cloudRepo.loadUniversalTheme()

        // Conflict resolution: latest wins
        val latest = resolveConflict(local, cloud)
        localRepo.saveUniversalTheme(latest)
        cloudRepo.saveUniversalTheme(latest)
    }

    // Sync app-specific themes
    suspend fun syncApp(appId: String) {
        // Similar to above
    }
}
```

#### Storage Format

##### Universal Theme File
```json
// Universal/Core/ThemeManager/themes/universal.json
{
  "version": "1.0.0",
  "lastModified": "2025-10-29T10:30:00Z",
  "theme": {
    "name": "Avanues Default",
    "platform": "iOS26_LiquidGlass",
    "colorScheme": {
      "mode": "Light",
      "primary": "#007AFF",
      "onPrimary": "#FFFFFF",
      // ... full color scheme
    },
    "typography": {
      "displayLarge": {
        "family": "SF Pro Display",
        "size": 57,
        "weight": "Regular"
      },
      // ... full typography
    },
    "shapes": { /* ... */ },
    "spacing": { /* ... */ },
    "elevation": { /* ... */ },
    "material": { /* ... */ }
  }
}
```

##### App Override File
```json
// Universal/Core/ThemeManager/themes/apps/com.avanues.mobile.json
{
  "appId": "com.avanues.mobile",
  "version": "1.0.0",
  "lastModified": "2025-10-29T11:00:00Z",
  "overrideType": "FULL", // or "PARTIAL"
  "theme": {
    // Full theme or just overrides
    "colorScheme": {
      "primary": "#FF3B30"  // Different from universal
    }
  }
}
```

#### Implementation Priority
1. **Week 5**: ThemeManager core architecture
2. **Week 6**: Persistence layer (local + cloud)
3. **Week 7**: Theme synchronization
4. **Week 8**: Integration with Theme Builder

---

### 3. Asset Management System

#### Architecture

##### Icon Library
```kotlin
data class IconLibrary(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val license: License,
    val icons: List<Icon>,
    val categories: List<String>,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

data class Icon(
    val id: String,
    val name: String,
    val displayName: String,
    val svg: String?,              // Vector format (preferred)
    val png: Map<Int, ByteArray>?, // Raster: size → bytes
    val categories: List<String>,
    val tags: List<String>,
    val variants: List<IconVariant> = emptyList()
)

data class IconVariant(
    val name: String,  // "filled", "outlined", "rounded", etc.
    val svg: String?,
    val png: Map<Int, ByteArray>?
)

enum class License {
    MIT, APACHE_2, GPL_3, CC_BY, CC_BY_SA, PROPRIETARY
}
```

##### Image Library
```kotlin
data class ImageLibrary(
    val id: String,
    val name: String,
    val images: List<ImageAsset>,
    val categories: List<String>,
    val totalSizeBytes: Long
)

data class ImageAsset(
    val id: String,
    val name: String,
    val path: String,              // Relative path in library
    val format: ImageFormat,
    val dimensions: Dimensions,
    val sizeBytes: Long,
    val thumbnail: ByteArray?,     // 256x256 preview
    val metadata: ImageMetadata
)

data class Dimensions(val width: Int, val height: Int)

enum class ImageFormat {
    PNG, JPG, WEBP, SVG, GIF, HEIC
}

data class ImageMetadata(
    val exif: Map<String, String>? = null,
    val dpi: Int? = null,
    val colorSpace: String? = null,
    val hasAlpha: Boolean = false
)
```

##### Asset Manager
```kotlin
object AssetManager {
    // Icon libraries
    private val iconLibraries = mutableMapOf<String, IconLibrary>()

    // Image libraries
    private val imageLibraries = mutableMapOf<String, ImageLibrary>()

    // Register icon library
    fun registerIconLibrary(library: IconLibrary) {
        iconLibraries[library.id] = library
        persist(library)
    }

    // Get icon by name
    fun getIcon(libraryId: String, iconName: String): Icon? {
        return iconLibraries[libraryId]?.icons?.find { it.name == iconName }
    }

    // Search icons
    fun searchIcons(query: String, tags: List<String> = emptyList()): List<Icon> {
        return iconLibraries.values
            .flatMap { it.icons }
            .filter { icon ->
                icon.name.contains(query, ignoreCase = true) ||
                icon.tags.any { it.contains(query, ignoreCase = true) }
            }
    }

    // Similar for images...
}
```

##### Asset Uploader UI
```kotlin
@Composable
fun AssetUploaderScreen() {
    var selectedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var uploadProgress by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Drag and drop zone
        DropZone(
            onFilesDropped = { files ->
                selectedFiles = files
                processFiles(files)
            }
        ) {
            Text("Drag and drop icons/images here")
            Text("or click to browse", style = MaterialTheme.typography.bodySmall)
        }

        // File list
        LazyColumn {
            items(selectedFiles) { file ->
                FileRow(file, onRemove = { selectedFiles -= file })
            }
        }

        // Upload button
        Button(
            onClick = { uploadAssets(selectedFiles) },
            enabled = selectedFiles.isNotEmpty()
        ) {
            Text("Upload ${selectedFiles.size} files")
        }

        // Progress bar
        if (uploadProgress > 0) {
            LinearProgressIndicator(progress = uploadProgress)
        }
    }
}
```

##### Asset Processing Pipeline
```kotlin
class AssetProcessor {
    // Process uploaded icon
    suspend fun processIcon(file: File): Icon {
        // 1. Detect format
        val format = detectFormat(file)

        // 2. Parse SVG or rasterize if needed
        val svg = if (format == "svg") {
            file.readText()
        } else {
            // Convert PNG/JPG to SVG using potrace or similar
            convertToSvg(file)
        }

        // 3. Generate PNG variants if SVG exists
        val pngVariants = if (svg != null) {
            generatePngSizes(svg, listOf(16, 24, 32, 48, 64, 128, 256))
        } else {
            // Use original raster
            mapOf(extractSize(file) to file.readBytes())
        }

        // 4. Extract metadata
        val tags = extractTags(file.nameWithoutExtension)

        return Icon(
            id = UUID.randomUUID().toString(),
            name = file.nameWithoutExtension,
            displayName = file.nameWithoutExtension.toDisplayName(),
            svg = svg,
            png = pngVariants,
            categories = inferCategories(tags),
            tags = tags
        )
    }

    // Process uploaded image
    suspend fun processImage(file: File): ImageAsset {
        // 1. Read dimensions
        val dimensions = readDimensions(file)

        // 2. Generate thumbnail
        val thumbnail = generateThumbnail(file, 256, 256)

        // 3. Extract metadata
        val metadata = extractImageMetadata(file)

        // 4. Optimize if needed
        val optimized = if (shouldOptimize(file)) {
            optimizeImage(file)
        } else {
            file
        }

        return ImageAsset(
            id = UUID.randomUUID().toString(),
            name = file.nameWithoutExtension,
            path = computeRelativePath(optimized),
            format = detectImageFormat(file),
            dimensions = dimensions,
            sizeBytes = optimized.length(),
            thumbnail = thumbnail,
            metadata = metadata
        )
    }
}
```

##### Asset Storage Structure
```
Universal/Assets/
├── Icons/
│   ├── MaterialIcons/
│   │   ├── manifest.json
│   │   ├── svg/
│   │   │   ├── home.svg
│   │   │   ├── settings.svg
│   │   │   └── ...
│   │   └── png/
│   │       ├── 16/
│   │       ├── 24/
│   │       ├── 32/
│   │       └── ...
│   ├── FontAwesome/
│   │   └── ...
│   └── CustomLibrary1/
│       └── ...
├── Images/
│   ├── Backgrounds/
│   │   ├── manifest.json
│   │   ├── gradient-blue.png
│   │   ├── gradient-purple.png
│   │   └── thumbnails/
│   ├── Illustrations/
│   │   └── ...
│   └── Photos/
│       └── ...
└── cdn-config.json  # CDN settings (optional)
```

##### Asset Manifest
```json
// Universal/Assets/Icons/CustomLibrary1/manifest.json
{
  "id": "custom-lib-1",
  "name": "Custom Library 1",
  "version": "1.0.0",
  "author": "Avanues Team",
  "license": "MIT",
  "icons": [
    {
      "id": "icon-001",
      "name": "user-avatar",
      "displayName": "User Avatar",
      "svg": "svg/user-avatar.svg",
      "png": {
        "16": "png/16/user-avatar.png",
        "24": "png/24/user-avatar.png",
        "32": "png/32/user-avatar.png"
      },
      "categories": ["user", "profile"],
      "tags": ["user", "avatar", "profile", "account"]
    }
  ],
  "categories": ["user", "interface", "social"],
  "totalIcons": 120,
  "createdAt": 1698765432000,
  "updatedAt": 1698765432000
}
```

#### CDN Integration (Optional)
```kotlin
interface AssetCDN {
    suspend fun upload(asset: Asset): String  // Returns CDN URL
    suspend fun download(url: String): ByteArray
    fun getUrl(assetId: String): String
}

class CloudinaryAssetCDN : AssetCDN {
    override suspend fun upload(asset: Asset): String {
        // Upload to Cloudinary
        // Returns: https://res.cloudinary.com/avanues/image/upload/v123/icon.png
    }
}

// Usage in components
Icon("user-avatar") {
    library = "CustomLibrary1"
    source = AssetManager.getIconUrl("custom-lib-1", "user-avatar")
    // Resolves to local file or CDN URL
}
```

#### Asset Versioning
```kotlin
data class AssetVersion(
    val version: String,          // "1.0.0"
    val changelog: String,
    val publishedAt: Long,
    val assets: List<String>      // Asset IDs
)

class AssetVersionManager {
    fun publishVersion(libraryId: String, version: String) {
        // Tag current state as version
        // Generate changelog
        // Notify users of update
    }

    fun rollback(libraryId: String, version: String) {
        // Restore library to previous version
    }

    fun getVersionHistory(libraryId: String): List<AssetVersion> {
        // Return all versions
    }
}
```

#### Implementation Priority
1. **Week 5**: Asset data models and storage structure
2. **Week 6**: Icon uploader with processing pipeline
3. **Week 7**: Image uploader with optimization
4. **Week 8**: Asset browser UI and search

---

## Pending Tasks (Full List)

### Immediate Priority (Phase 2 - Weeks 5-8)

#### Theme Builder
- [ ] Design Theme Builder UI architecture
- [ ] Implement live preview system for theme changes
- [ ] Create color picker with palette generation
- [ ] Create typography editor
- [ ] Create spacing/sizing editor
- [ ] Implement material effects editor (glass, mica, shadows)
- [ ] Build export system (DSL, YAML, JSON, CSS)
- [ ] Build import system
- [ ] Implement theme validation
- [ ] Add undo/redo system
- [ ] Add auto-save functionality

#### Universal Theme System
- [ ] Create ThemeManager singleton
- [ ] Implement theme persistence layer (local)
- [ ] Implement theme persistence layer (cloud)
- [ ] Create theme synchronization system
- [ ] Build per-app override system
- [ ] Create theme inheritance hierarchy
- [ ] Add runtime theme switching
- [ ] Implement conflict resolution
- [ ] Create theme migration tools

#### Asset Management
- [ ] Design asset storage structure
- [ ] Create icon library data models
- [ ] Create image library data models
- [ ] Implement icon uploader UI
- [ ] Implement image uploader UI
- [ ] Build asset processing pipeline
- [ ] Add SVG to PNG conversion
- [ ] Add PNG to SVG conversion (potrace)
- [ ] Implement thumbnail generation
- [ ] Add image optimization
- [ ] Create asset browser UI
- [ ] Implement asset search
- [ ] Add asset versioning
- [ ] Integrate CDN (optional)
- [ ] Create asset manifest system

#### Platform Renderers
- [ ] Implement Android Compose renderer
- [ ] Map all 13 components to Compose
- [ ] Integrate Material 3 theme
- [ ] Implement iOS SwiftUI bridge
- [ ] Map all 13 components to SwiftUI
- [ ] Integrate iOS 26 Liquid Glass theme
- [ ] Implement Desktop Compose renderer
- [ ] Map all 13 components to Compose Desktop
- [ ] Integrate Windows 11 Fluent 2 theme
- [ ] Implement state management with Flow
- [ ] Create reactive state system
- [ ] Build two-way data binding

### Medium Priority (Phase 3 - Weeks 9-12 → 13-16)

#### Form Components (8)
- [ ] Radio button
- [ ] Slider
- [ ] Dropdown
- [ ] DatePicker
- [ ] TimePicker
- [ ] FileUpload
- [ ] SearchBar
- [ ] Rating

#### Feedback Components (7)
- [ ] Dialog
- [ ] Toast
- [ ] Alert
- [ ] ProgressBar
- [ ] Spinner
- [ ] Badge
- [ ] Tooltip

#### Navigation Components (6)
- [ ] AppBar
- [ ] BottomNav
- [ ] Tabs
- [ ] Drawer
- [ ] Breadcrumb
- [ ] Pagination

#### Data Display Components (14)
- [ ] Table
- [ ] List
- [ ] Accordion
- [ ] Stepper
- [ ] Timeline
- [ ] TreeView
- [ ] Carousel
- [ ] Avatar
- [ ] Chip
- [ ] Divider
- [ ] Paper
- [ ] Skeleton
- [ ] Empty State
- [ ] Data Grid

### Lower Priority (Phase 4 - Weeks 17-20)

#### Remaining Platform Themes
- [ ] macOS 26 Tahoe theme
- [ ] visionOS 2 Spatial Glass theme
- [ ] Android XR Spatial Material theme
- [ ] Samsung One UI 7 theme

### Future Priority (Phase 5 - Weeks 21-24)

#### Advanced Components (7)
- [ ] ColorPicker
- [ ] CodeEditor
- [ ] Map
- [ ] Chart
- [ ] RichTextEditor
- [ ] DragDrop
- [ ] Video

#### Testing & Documentation
- [ ] Unit tests for all components
- [ ] Integration tests
- [ ] Performance tests
- [ ] Accessibility audit
- [ ] API documentation
- [ ] Usage guides
- [ ] Migration guides
- [ ] Video tutorials

#### Polish & Release
- [ ] Performance optimization
- [ ] Memory optimization
- [ ] Bundle size optimization
- [ ] Accessibility improvements (WCAG 2.1 AA)
- [ ] Bug fixes
- [ ] Release preparation
- [ ] Beta testing
- [ ] Production release

---

## Technical Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Avanues Apps                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Mobile  │  │ Desktop  │  │   Web    │  │  VisionOS│   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
└───────┼─────────────┼─────────────┼─────────────┼──────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                          │
        ┌─────────────────▼─────────────────┐
        │     Universal Theme Manager        │
        │  ┌─────────────────────────────┐  │
        │  │   Global Theme (Singleton)  │  │
        │  └─────────────────────────────┘  │
        │  ┌─────────────────────────────┐  │
        │  │   Per-App Overrides (Map)   │  │
        │  └─────────────────────────────┘  │
        │  ┌─────────────────────────────┐  │
        │  │    Theme Persistence        │  │
        │  │  ├─ Local Storage            │  │
        │  │  └─ Cloud Sync               │  │
        │  └─────────────────────────────┘  │
        └───────────────┬───────────────────┘
                        │
        ┌───────────────▼───────────────┐
        │    AvaElements Library       │
        │  ┌───────────────────────────┐ │
        │  │   Theme System (7 themes) │ │
        │  └───────────────────────────┘ │
        │  ┌───────────────────────────┐ │
        │  │   Components (50 total)   │ │
        │  └───────────────────────────┘ │
        │  ┌───────────────────────────┐ │
        │  │   DSL Builder             │ │
        │  └───────────────────────────┘ │
        │  ┌───────────────────────────┐ │
        │  │   YAML Parser             │ │
        │  └───────────────────────────┘ │
        └───────────────┬───────────────┘
                        │
        ┌───────────────▼───────────────┐
        │    Platform Renderers          │
        │  ┌──────┐ ┌──────┐ ┌────────┐│
        │  │Android│Compose│Desktop  ││
        │  ├──────┤ ├──────┤ ├────────┤│
        │  │  iOS  │ SwiftUI│  Web    ││
        │  └──────┘ └──────┘ └────────┘│
        └───────────────────────────────┘
                        │
        ┌───────────────▼───────────────┐
        │      Asset Manager             │
        │  ┌───────────────────────────┐│
        │  │   Icon Libraries          ││
        │  │  ├─ Material Icons        ││
        │  │  ├─ Font Awesome          ││
        │  │  └─ Custom Libraries      ││
        │  └───────────────────────────┘│
        │  ┌───────────────────────────┐│
        │  │   Image Libraries         ││
        │  │  ├─ Backgrounds           ││
        │  │  ├─ Illustrations         ││
        │  │  └─ Photos                ││
        │  └───────────────────────────┘│
        │  ┌───────────────────────────┐│
        │  │   CDN Integration (opt)   ││
        │  └───────────────────────────┘│
        └───────────────────────────────┘
                        │
        ┌───────────────▼───────────────┐
        │      Theme Builder             │
        │  ┌───────────────────────────┐│
        │  │   Visual Editor           ││
        │  └───────────────────────────┘│
        │  ┌───────────────────────────┐│
        │  │   Live Preview Canvas     ││
        │  └───────────────────────────┘│
        │  ┌───────────────────────────┐│
        │  │   Export System           ││
        │  └───────────────────────────┘│
        └───────────────────────────────┘
```

### Module Structure

```
Universal/
├── Core/
│   ├── ThemeManager/
│   │   ├── src/commonMain/kotlin/
│   │   │   ├── ThemeManager.kt
│   │   │   ├── ThemeRepository.kt
│   │   │   ├── ThemeSync.kt
│   │   │   └── ThemeOverride.kt
│   │   ├── themes/
│   │   │   ├── universal.json
│   │   │   └── apps/
│   │   │       ├── com.avanues.mobile.json
│   │   │       └── ...
│   │   └── build.gradle.kts
│   │
│   └── AssetManager/
│       ├── src/commonMain/kotlin/
│       │   ├── AssetManager.kt
│       │   ├── IconLibrary.kt
│       │   ├── ImageLibrary.kt
│       │   └── AssetProcessor.kt
│       └── build.gradle.kts
│
├── Libraries/
│   ├── AvaElements/
│   │   ├── Core/                      # ✅ Phase 1 complete
│   │   │   ├── src/commonMain/kotlin/
│   │   │   │   ├── core/
│   │   │   │   │   ├── Types.kt       # ✅
│   │   │   │   │   ├── Component.kt   # ✅
│   │   │   │   │   └── Theme.kt       # ✅
│   │   │   │   ├── dsl/
│   │   │   │   │   ├── AvaUI.kt     # ✅
│   │   │   │   │   └── Components.kt  # ✅
│   │   │   │   └── yaml/
│   │   │   │       └── YamlParser.kt  # ✅
│   │   │   └── build.gradle.kts       # ✅
│   │   │
│   │   ├── Renderers/                 # 🔄 Phase 2 in progress
│   │   │   ├── Android/
│   │   │   │   └── ComposeRenderer.kt
│   │   │   ├── iOS/
│   │   │   │   └── SwiftUIBridge.kt
│   │   │   ├── Desktop/
│   │   │   │   └── DesktopRenderer.kt
│   │   │   └── Web/
│   │   │       └── ReactRenderer.kt
│   │   │
│   │   ├── ThemeBuilder/              # 🔄 Phase 2 new
│   │   │   ├── UI/
│   │   │   │   ├── EditorWindow.kt
│   │   │   │   ├── PreviewCanvas.kt
│   │   │   │   └── PropertyInspector.kt
│   │   │   └── Engine/
│   │   │       ├── ThemeCompiler.kt
│   │   │       └── HotReload.kt
│   │   │
│   │   ├── examples/                  # ✅ Phase 1 complete
│   │   │   ├── DSLExample.kt
│   │   │   ├── login-screen.yaml
│   │   │   ├── settings-screen.yaml
│   │   │   └── dashboard.yaml
│   │   │
│   │   └── docs/                      # ✅ Phase 1 complete
│   │       ├── MAGICELEMENTS_SPECIFICATION.md
│   │       ├── PLATFORM_THEMES_SPEC.md
│   │       ├── PHASE1_COMPLETION_SUMMARY.md
│   │       └── MASTER_PLAN.md (this file)
│   │
│   └── Preferences/
│
└── Assets/                            # 🔄 Phase 2 new
    ├── Icons/
    │   ├── MaterialIcons/
    │   ├── FontAwesome/
    │   └── Custom/
    ├── Images/
    │   ├── Backgrounds/
    │   ├── Illustrations/
    │   └── Photos/
    └── cdn-config.json
```

---

## Resource Allocation

### Team Structure (Recommended)

#### Phase 2 (Current - Weeks 5-8)
- **2 Engineers** on Platform Renderers (Android, iOS, Desktop)
- **1 Engineer** on Theme Builder UI
- **1 Engineer** on Asset Management System
- **1 Engineer** on Universal Theme Manager
- **1 Designer** for Theme Builder UX
- **Total**: 5 engineers + 1 designer

#### Phase 3 (Weeks 13-16)
- **3 Engineers** on Component Development (35 components)
- **1 Engineer** on Testing
- **1 Designer** for Component Design
- **Total**: 4 engineers + 1 designer

#### Phase 4 (Weeks 17-20)
- **2 Engineers** on Platform Themes (4 remaining)
- **1 Engineer** on Documentation
- **Total**: 3 engineers

#### Phase 5 (Weeks 21-24)
- **2 Engineers** on Advanced Components (7 components)
- **1 Engineer** on Testing & Performance
- **1 Engineer** on Documentation & Release
- **Total**: 4 engineers

### Estimated Total Effort

| Phase | Duration | Engineer-Weeks | Notes |
|-------|----------|----------------|-------|
| Phase 1 | 4 weeks | 4 weeks | ✅ Complete (1 engineer × 4 weeks) |
| Phase 2 | 6-8 weeks | 30-40 weeks | 5 engineers × 6-8 weeks |
| Phase 3 | 4 weeks | 16 weeks | 4 engineers × 4 weeks |
| Phase 4 | 4 weeks | 12 weeks | 3 engineers × 4 weeks |
| Phase 5 | 4 weeks | 16 weeks | 4 engineers × 4 weeks |
| **Total** | **22-24 weeks** | **78-88 weeks** | **~6 months** |

### Budget Estimate (Rough)

Assuming average engineer cost of $150/hour:

| Phase | Engineer-Weeks | Hours | Cost |
|-------|----------------|-------|------|
| Phase 1 | 4 | 160 | $24,000 |
| Phase 2 | 35 | 1,400 | $210,000 |
| Phase 3 | 16 | 640 | $96,000 |
| Phase 4 | 12 | 480 | $72,000 |
| Phase 5 | 16 | 640 | $96,000 |
| **Total** | **83** | **3,320** | **$498,000** |

*Note: Add 20-30% for designer, project management, and contingency.*

---

## Success Metrics

### Phase 2 Goals
- [ ] Theme Builder functional with live preview
- [ ] Universal Theme System deployed to all Avanues apps
- [ ] Asset Management System with 3+ icon libraries
- [ ] Android, iOS, Desktop renderers working for all 13 components
- [ ] State management system integrated
- [ ] 90% code coverage on new features

### Phase 3 Goals
- [ ] 35 additional components implemented (43 total)
- [ ] All components work on 3 platforms (Android, iOS, Desktop)
- [ ] Component documentation complete
- [ ] 80% code coverage

### Phase 4 Goals
- [ ] All 7 platform themes complete
- [ ] Theme switcher tested across all platforms
- [ ] Cross-platform theme consistency verified

### Phase 5 Goals
- [ ] 50 total components
- [ ] Production-ready library
- [ ] Full documentation
- [ ] 85%+ code coverage
- [ ] Performance benchmarks pass
- [ ] Accessibility audit pass (WCAG 2.1 AA)

---

## Risk Assessment

### High Risk
1. **Scope Creep**: Theme Builder and Asset Management add significant complexity
   - **Mitigation**: Clearly defined MVP for each feature, phased rollout

2. **Platform Rendering Complexity**: Each platform has unique constraints
   - **Mitigation**: Start with one platform (Android) as reference implementation

3. **Theme Synchronization**: Conflicts between devices/users
   - **Mitigation**: Implement last-write-wins with manual conflict resolution

### Medium Risk
1. **Performance**: 50 components with real-time preview may be slow
   - **Mitigation**: Implement virtual scrolling, lazy loading, caching

2. **Asset Storage**: Large icon/image libraries could consume significant storage
   - **Mitigation**: CDN integration, on-demand loading, compression

3. **Cross-Platform Consistency**: Themes may look different on each platform
   - **Mitigation**: Visual regression testing, design tokens, strict guidelines

### Low Risk
1. **YAML Parsing**: Current implementation is placeholder
   - **Mitigation**: Integrate proper YAML library (kaml) in Phase 2

2. **Testing Coverage**: Unit tests not yet created
   - **Mitigation**: Add tests incrementally, aim for 80%+ coverage

---

## Next Steps

### Immediate Actions (This Week)
1. **Review and approve** this Master Plan
2. **Decide on Phase 2 priorities**: Theme Builder vs. Asset Management vs. Renderers
3. **Assign team members** to Phase 2 tasks
4. **Set up project tracking** (JIRA, Linear, etc.)
5. **Create design mockups** for Theme Builder UI
6. **Define asset storage** requirements (local vs. CDN)

### Week 5 Kickoff
1. Begin Theme Builder UI design
2. Start Universal Theme Manager implementation
3. Begin Android Compose renderer
4. Set up asset storage structure

### Phase 2 Milestones
- **End of Week 5**: Theme Builder UI prototype
- **End of Week 6**: Universal Theme Manager functional
- **End of Week 7**: Asset Management MVP
- **End of Week 8**: Android renderer complete, Theme Builder beta

---

## Conclusion

The AvaElements project has successfully completed Phase 1 with a solid foundation. Phase 2 significantly expands scope with three new major features:

1. **Visual Theme Builder** - Interactive theme editor with live preview
2. **Universal Theme System** - Centralized theme management across all apps
3. **Asset Management System** - User-uploadable icon and image libraries

These additions will extend Phase 2 from 4 weeks to 6-8 weeks, but will provide tremendous value by enabling:
- Non-technical users to create and customize themes
- Consistent branding across all Avanues apps
- Flexible asset management with custom icons and images

The updated timeline projects 22-24 weeks (6 months) for full completion, with an estimated investment of ~$500K. However, each phase delivers incremental value and can be deployed independently.

**Recommendation**: Proceed with Phase 2, prioritizing Theme Builder and Universal Theme System first, followed by Asset Management.

---

**Document Version**: 1.0
**Status**: Draft for Review
**Next Review**: End of Week 5 (Phase 2 checkpoint)
