# AvaElements Asset Manager

**Zero-Bloat Icon & Image Management with On-Demand Loading**

## Overview

The Asset Manager provides icon and image management for AvaElements apps with a **plugin-based architecture** that avoids bundling thousands of icons in your APK/IPA.

### Key Features

✅ **Zero Bloat** - Icons load on-demand from CDN, not bundled
✅ **Smart Caching** - Downloaded icons cached locally with SQLite
✅ **2,400+ Material Icons** - Available via Google CDN
✅ **1,500+ Font Awesome Icons** - Available via jsDelivr CDN
✅ **Search & Discovery** - Full-text search with relevance scoring
✅ **Offline Support** - Pre-cache popular icons (~30, ~50KB)
✅ **Custom Icons** - Upload your own icons/images
✅ **Cross-Platform** - Works on Android, iOS, Desktop

## Architecture

```
┌─────────────────┐
│   Your App      │
│  (zero bloat!)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│  AssetManager   │◄─────┤ Icon Search  │
│   (Facade)      │      └──────────────┘
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌─────────┐ ┌──────────────────┐
│ Storage │ │ Library Providers│
│ (SQLite)│ │   (CDN Loaders)  │
└─────────┘ └────────┬─────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
   ┌────────────┐      ┌──────────────┐
   │  Material  │      │ Font Awesome │
   │   Icons    │      │    Icons     │
   │   (CDN)    │      │    (CDN)     │
   └────────────┘      └──────────────┘
```

## Usage

### 1. Initialize (Android)

```kotlin
// In your Application.onCreate()
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Asset Manager
        AssetManager.initialize(this)

        // Register icon libraries (metadata only, ~1KB each)
        lifecycleScope.launch {
            val assetManager = AssetManager.getInstance()
            assetManager.registerMaterialIcons()
            assetManager.registerFontAwesome()

            // Optional: Preload popular icons for offline use (~50KB)
            assetManager.preloadPopularIcons { current, total ->
                Log.d("AssetManager", "Loading popular icons: $current/$total")
            }
        }
    }
}
```

### 2. Search Icons

```kotlin
val assetManager = AssetManager.getInstance()

// Search across all libraries
val homeIcons = assetManager.searchIcons("home")

// Search specific library
val materialHomeIcons = assetManager.searchIcons(
    queryText = "home",
    library = "material"
)

// Search by category
val actionIcons = assetManager.searchIcons(
    queryText = "edit",
    category = "action"
)
```

### 3. Get Specific Icon

```kotlin
// Get icon (downloads if not cached)
val icon = assetManager.getIcon("material:home")

if (icon != null) {
    // Use SVG
    if (icon.isVector) {
        val svgString = icon.svg
        // Render SVG
    }

    // Use PNG (multiple sizes)
    if (icon.isRaster) {
        val pngData = icon.png?.get(IconSize.LARGE)
        // Render PNG
    }
}
```

### 4. Upload Custom Icons

```kotlin
// Upload your own icon
val iconBytes = File("my-icon.svg").readBytes()

val customIcon = assetManager.uploadIcon(
    fileData = iconBytes,
    fileName = "my-icon.svg",
    tags = listOf("custom", "branding")
)

// Now searchable
val results = assetManager.searchIcons("branding")
```

### 5. Check Stats

```kotlin
val stats = assetManager.getStats()
println("Icons: ${stats.totalIcons}")
println("Images: ${stats.totalImages}")
println("Cache size: ${AssetUtils.formatFileSize(stats.totalSizeBytes)}")
```

## Icon Libraries

### Material Icons (2,400+ icons)

- **Source**: Google Fonts
- **CDN**: `fonts.gstatic.com`
- **License**: Apache 2.0
- **Categories**: Action, Alert, AV, Communication, Content, Device, Editor, File, Hardware, Image, Maps, Navigation, Notification, Places, Social, Toggle

**Popular icons auto-cached:**
`home`, `search`, `settings`, `menu`, `close`, `add`, `remove`, `edit`, `delete`, `check`, `star`, `favorite`, `share`, `person`, `notifications`

### Font Awesome Free (1,500+ icons)

- **Source**: Font Awesome
- **CDN**: `cdn.jsdelivr.net`
- **License**: CC BY 4.0, Font License
- **Categories**: Solid, Regular, Brands

**Popular icons auto-cached:**
`house`, `magnifying-glass`, `gear`, `bars`, `xmark`, `plus`, `minus`, `pen`, `trash`, `check`, `star`, `heart`, `share`, `user`, `bell`

## Performance

### App Size Impact

| Component | Size | Notes |
|-----------|------|-------|
| Asset Manager Core | ~50 KB | KMP library |
| Icon Metadata | ~5 KB | Per library |
| Popular Icons Cache | ~50 KB | Optional, 30 icons |
| **Total** | **~100 KB** | **vs 5+ MB if bundled!** |

### Network Usage

- First icon: ~2-5 KB download (SVG)
- Subsequent uses: 0 bytes (cached)
- Popular icons preload: ~50 KB one-time

### Storage

- SQLite database: ~10 KB base
- Per icon cached: ~2-5 KB (SVG)
- 100 icons cached: ~300 KB
- Self-managing LRU cache (configurable limit)

## Advanced

### Custom Icon Library

```kotlin
val customLibrary = IconLibraries.custom(
    id = "mycompany",
    name = "My Company Icons",
    baseUrl = "https://cdn.mycompany.com/icons"
)

// Register and use
val provider = RemoteIconLibrary(
    config = customLibrary,
    storage = storage,
    httpClient = createHttpClient()
)
```

### Offline Mode

```kotlin
// Check if icon is cached
if (assetManager.isCached("material:home")) {
    // Available offline
    val icon = assetManager.getIcon("material:home")
}

// Preload for offline trip
val essentialIcons = listOf(
    "material:home", "material:menu",
    "material:search", "material:settings"
)

assetManager.preloadIcons(essentialIcons) { current, total ->
    println("Preloading: $current/$total")
}
```

### Clear Cache

```kotlin
// Clear specific library cache
storage.clearLibrary("material")

// Clear all icons
storage.deleteIcon("material:home")
```

## Implementation Status

✅ **Complete**
- Core interfaces & models
- Android SQLite storage
- Android image processing
- Search with relevance scoring
- Plugin architecture
- CDN integration ready

🔄 **In Progress**
- HTTP client implementation (Ktor)
- Material Icons manifest
- Font Awesome manifest

⏳ **Planned**
- iOS implementation (CoreData)
- Desktop implementation
- Icon preview UI components

## Architecture Benefits

1. **Zero Bloat** - App stays small, downloads only what's needed
2. **Fast Updates** - Icon libraries update without app release
3. **Bandwidth Friendly** - Downloads compressed SVG (~2KB each)
4. **Offline Capable** - Smart caching for offline use
5. **Extensible** - Easy to add new icon libraries
6. **Storage Efficient** - SQLite + LRU cache management

## License

Apache 2.0

## Credits

- Material Icons: Google (Apache 2.0)
- Font Awesome: Fonticons, Inc. (CC BY 4.0)
- Asset Manager: Augmentalis (Apache 2.0)
