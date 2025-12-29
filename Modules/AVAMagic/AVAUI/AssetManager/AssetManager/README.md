# Asset Manager

A comprehensive asset management system for managing custom icon and image libraries in the VoiceAvenue Universal platform.

## Overview

AssetManager provides a centralized system for registering, storing, searching, and retrieving icon and image assets across all platform targets (JVM, Android, iOS, macOS).

## Features

- **Icon Library Management**: Support for SVG and multi-resolution PNG icons
- **Image Library Management**: Support for all common image formats (JPEG, PNG, GIF, WebP, SVG)
- **Manifest-based Storage**: JSON manifests for easy library metadata management
- **Thumbnail Generation**: Automatic thumbnail generation for images
- **Search Capabilities**: Full-text search across icons and images with tag support
- **Platform-agnostic API**: Works across all Kotlin Multiplatform targets
- **Extensible Storage**: Interface-based repository pattern for custom storage backends
- **AvaElements Integration**: Direct integration with UI components

## Architecture

### Core Components

1. **AssetManager** - Central registry and API for asset operations
2. **IconLibrary** - Model for icon collections with multi-format support
3. **ImageLibrary** - Model for image collections with metadata
4. **AssetProcessor** - Processing pipeline for asset optimization
5. **AssetRepository** - Persistence interface with local file system implementation

## Usage Guide

### 1. Registering Icon Libraries

```kotlin
import com.augmentalis.universal.assetmanager.*

// Create an icon library
val iconLibrary = IconLibrary(
    id = "CustomIcons",
    name = "My Custom Icons",
    version = "1.0.0",
    description = "Application-specific icon set",
    icons = listOf(
        Icon(
            id = "custom-home",
            name = "Home Icon",
            svg = "<svg>...</svg>",
            png = mapOf(
                24 to homeIcon24px,
                48 to homeIcon48px,
                96 to homeIcon96px
            ),
            tags = listOf("navigation", "home", "main"),
            category = "Navigation",
            keywords = listOf("house", "start", "index")
        )
    )
)

// Register the library
AssetManager.registerIconLibrary(iconLibrary)
```

### 2. Registering Image Libraries

```kotlin
val imageLibrary = ImageLibrary(
    id = "AppImages",
    name = "Application Images",
    description = "Marketing and UI images",
    images = listOf(
        ImageAsset(
            id = "hero-banner",
            name = "Hero Banner",
            path = "images/hero-banner.jpg",
            format = ImageFormat.JPEG,
            dimensions = Dimensions(1920, 1080),
            thumbnail = thumbnailBytes,
            fileSize = 245678L,
            tags = listOf("hero", "banner", "marketing"),
            category = "Marketing"
        )
    )
)

AssetManager.registerImageLibrary(imageLibrary)
```

### 3. Retrieving Assets

```kotlin
// Get an icon by reference
val icon = AssetManager.getIcon("CustomIcons:custom-home")

// Get an image by reference
val image = AssetManager.getImage("AppImages:hero-banner")

// Direct retrieval
val icon2 = AssetManager.getIcon("MaterialIcons", "home")
val image2 = AssetManager.getImage("Backgrounds", "gradient-blue-purple")
```

### 4. Searching Assets

```kotlin
// Search icons
val iconResults = AssetManager.searchIcons(
    query = "home",
    tags = setOf("navigation"),
    category = "Navigation"
)

iconResults.forEach { result ->
    println("${result.libraryName}: ${result.icon.name} (${result.reference})")
}

// Search images with filters
val imageResults = AssetManager.searchImages(
    filter = ImageFilter(
        query = "background",
        formats = setOf(ImageFormat.PNG, ImageFormat.JPEG),
        minWidth = 1920,
        orientation = ImageOrientation.LANDSCAPE,
        tags = setOf("gradient")
    )
)
```

### 5. Processing Assets

```kotlin
val processor = AssetProcessor()

// Process an icon from file
val icon = processor.processIcon(
    fileData = iconFileBytes,
    fileName = "home-icon.svg",
    iconId = "home"
)

// Process an image with thumbnail
val image = processor.processImage(
    fileData = imageFileBytes,
    fileName = "hero-banner.jpg",
    imageId = "hero-banner"
)

// Generate thumbnail
val thumbnail = processor.generateThumbnail(
    imageData = originalImageBytes,
    width = 256,
    height = 256,
    maintainAspectRatio = true
)

// Optimize image
val optimized = processor.optimizeImage(
    imageData = originalImageBytes,
    quality = 85
)
```

### 6. AvaElements Integration

Use assets directly in AvaElements components:

```kotlin
// Using icons
Icon("MaterialIcons:home") {
    size = 24.px
    color = Colors.primary
}

Icon("CustomLibrary:user-avatar") {
    size = 48.px
}

// Using images
Image("Backgrounds:gradient-blue-purple") {
    width = 100.percent
    height = 400.px
    objectFit = ObjectFit.Cover
}

Image("Photos:hero-image-1") {
    width = 800.px
    loading = ImageLoading.Lazy
}
```

## Storage Structure

Assets are stored in the `Universal/Assets/` directory:

```
Universal/Assets/
├── Icons/
│   ├── MaterialIcons/
│   │   ├── manifest.json          # Library metadata
│   │   ├── svg/                   # SVG icon files
│   │   │   ├── home.svg
│   │   │   ├── user.svg
│   │   │   └── settings.svg
│   │   └── png/                   # PNG icon files
│   │       ├── home_24.png
│   │       ├── home_48.png
│   │       └── home_96.png
│   └── CustomLibrary/
│       ├── manifest.json
│       └── svg/
│           └── user-avatar.svg
└── Images/
    ├── Backgrounds/
    │   ├── manifest.json
    │   ├── images/                # Original images
    │   │   ├── gradient-blue-purple.png
    │   │   └── geometric-pattern.svg
    │   └── thumbnails/            # Generated thumbnails
    │       ├── gradient-blue-purple.jpg
    │       └── geometric-pattern.jpg
    └── Photos/
        ├── manifest.json
        ├── images/
        │   └── hero-office.jpg
        └── thumbnails/
            └── hero-office.jpg
```

## Manifest Format

### Icon Library Manifest

```json
{
  "id": "MaterialIcons",
  "name": "Material Design Icons",
  "version": "1.0.0",
  "description": "Google Material Design icon library",
  "metadata": {
    "author": "Google",
    "license": "Apache 2.0"
  },
  "icons": [
    {
      "id": "home",
      "name": "Home",
      "hasSvg": true,
      "pngSizes": [24, 48, 96],
      "tags": ["navigation", "house"],
      "category": "Navigation",
      "keywords": ["house", "main", "start"]
    }
  ]
}
```

### Image Library Manifest

```json
{
  "id": "Backgrounds",
  "name": "Background Images",
  "description": "Background images and gradients",
  "metadata": {
    "author": "Augmentalis"
  },
  "images": [
    {
      "id": "gradient-blue-purple",
      "name": "Blue to Purple Gradient",
      "fileName": "gradient-blue-purple.png",
      "format": "PNG",
      "width": 1920,
      "height": 1080,
      "fileSize": 245678,
      "hasThumbnail": true,
      "tags": ["gradient", "blue", "purple"],
      "category": "Gradients",
      "metadata": {
        "colors": "blue, purple"
      }
    }
  ]
}
```

## API Reference

### AssetManager

Static methods for asset management:

- `registerIconLibrary(library, persist)` - Register an icon library
- `registerImageLibrary(library, persist)` - Register an image library
- `getIcon(reference)` - Retrieve an icon by reference
- `getImage(reference)` - Retrieve an image by reference
- `searchIcons(query, libraryIds, tags, category)` - Search icons
- `searchImages(filter, libraryIds)` - Search images
- `loadAllLibraries()` - Load all libraries from storage

### Icon

Data class representing an icon:

- `id: String` - Unique identifier
- `name: String` - Display name
- `svg: String?` - SVG content
- `png: Map<Int, ByteArray>?` - PNG data by size
- `tags: List<String>` - Searchable tags
- `category: String?` - Category
- `keywords: List<String>` - Search keywords
- `hasFormat(format)` - Check format availability
- `getPngForSize(size)` - Get PNG for specific size
- `matchesQuery(query)` - Search helper

### ImageAsset

Data class representing an image:

- `id: String` - Unique identifier
- `name: String` - Display name
- `path: String` - Relative file path
- `format: ImageFormat` - Image format
- `dimensions: Dimensions` - Image dimensions
- `thumbnail: ByteArray?` - Thumbnail data
- `tags: List<String>` - Searchable tags
- `getAspectRatio()` - Calculate aspect ratio
- `isLandscape/isPortrait/isSquare()` - Orientation helpers

### AssetProcessor

Processing utilities:

- `processIcon(fileData, fileName, iconId)` - Process icon file
- `processImage(fileData, fileName, imageId)` - Process image file
- `generateThumbnail(imageData, width, height)` - Generate thumbnail
- `optimizeImage(imageData, quality)` - Optimize image
- `extractDimensions(imageData)` - Get image dimensions
- `convertFormat(imageData, targetFormat)` - Convert image format

### AssetRepository

Storage interface:

- `saveIconLibrary(library)` - Save icon library
- `loadIconLibrary(id)` - Load icon library
- `saveImageLibrary(library)` - Save image library
- `loadImageLibrary(id)` - Load image library
- `saveIconData(libraryId, iconId, format, data)` - Save icon data
- `loadIconData(libraryId, iconId, format)` - Load icon data

## Platform-Specific Implementations

### JVM/Android
- Uses Java ImageIO for image processing
- Uses imgscalr library for thumbnail generation
- File system storage via java.io

### iOS/macOS
- Uses UIImage/NSImage for processing
- CoreGraphics for thumbnails
- File system storage via Foundation

### Future: JS/WASM
- Canvas API for processing
- Blob/FileReader for storage
- IndexedDB for persistence

## Build Tasks

```bash
# Validate all manifest files
./gradlew :AssetManager:validateManifests

# Copy example assets
./gradlew :AssetManager:copyExampleAssets

# Clean generated assets (keeps manifests)
./gradlew :AssetManager:cleanAssets

# Run tests
./gradlew :AssetManager:test
```

## Extension Points

### Custom Storage Backend

Implement `AssetRepository` for custom storage:

```kotlin
class CloudAssetRepository(
    private val s3Client: S3Client
) : AssetRepository {
    override suspend fun saveIconLibrary(library: IconLibrary): Result<Unit> {
        // Upload to S3
    }

    override suspend fun loadIconLibrary(id: String): Result<IconLibrary?> {
        // Download from S3
    }
    // ... other methods
}

// Use custom repository
AssetManager.repository = CloudAssetRepository(s3Client)
```

### Custom Processing

Extend `AssetProcessor` for custom processing:

```kotlin
class CustomAssetProcessor : AssetProcessor() {
    override suspend fun processIcon(
        fileData: ByteArray,
        fileName: String,
        iconId: String
    ): Icon {
        // Custom icon processing
        val baseIcon = super.processIcon(fileData, fileName, iconId)

        // Add custom metadata
        return baseIcon.copy(
            metadata = baseIcon.metadata + mapOf("processed" to "custom")
        )
    }
}
```

## Best Practices

1. **Organize by Purpose**: Group icons/images into logical libraries
2. **Use Descriptive IDs**: Use kebab-case IDs like "user-avatar" instead of "icon1"
3. **Tag Comprehensively**: Add multiple tags for better searchability
4. **Optimize Before Upload**: Pre-optimize images to reduce storage
5. **Version Libraries**: Use semantic versioning for library updates
6. **Generate Thumbnails**: Always provide thumbnails for faster loading
7. **Cache Manifests**: Load manifests once and cache in AssetManager
8. **Lazy Load Assets**: Load actual asset data only when needed

## Future Enhancements

- CDN integration for remote asset delivery
- Asset compression and caching strategies
- Automatic icon color variant generation
- Sprite sheet generation for web
- Asset versioning and migration tools
- Asset analytics and usage tracking
- Batch import/export utilities
- Visual asset browser UI

## License

Copyright © 2025 Augmentalis. All rights reserved.
