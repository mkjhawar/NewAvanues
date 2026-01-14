# AssetManager Quick Start Guide

Get started with AssetManager in 5 minutes.

## Installation

### 1. Add Module Dependency

In your module's `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Universal:Core:AssetManager"))
            }
        }
    }
}
```

### 2. Initialize on Startup

```kotlin
import com.augmentalis.universal.assetmanager.AssetManager

suspend fun main() {
    // Load all libraries from Universal/Assets/
    AssetManager.loadAllLibraries()

    // Your app code
}
```

## Basic Usage

### Using Icons

```kotlin
// In AvaElements components
Icon("MaterialIcons:home") {
    size = 24.px
    color = Colors.primary
}

Icon("CustomLibrary:user-avatar") {
    size = 48.px
}
```

### Using Images

```kotlin
Image("Backgrounds:gradient-blue-purple") {
    width = 100.percent
    height = 400.px
    objectFit = ObjectFit.Cover
}

Image("Photos:hero-image-1") {
    width = 800.px
}
```

### Programmatic Access

```kotlin
// Get an icon
val icon = AssetManager.getIcon("MaterialIcons:home")

// Get an image
val image = AssetManager.getImage("Backgrounds:gradient-blue-purple")

// Search icons
val results = AssetManager.searchIcons(
    query = "home",
    tags = setOf("navigation")
)

// Search images
val imageResults = AssetManager.searchImages(
    filter = ImageFilter(
        query = "background",
        minWidth = 1920
    )
)
```

## Creating Your First Library

### Icon Library

```kotlin
val myIcons = IconLibrary(
    id = "MyIcons",
    name = "My Custom Icons",
    version = "1.0.0",
    icons = listOf(
        Icon(
            id = "custom-icon",
            name = "Custom Icon",
            svg = "<svg>...</svg>",
            tags = listOf("custom", "icon")
        )
    )
)

AssetManager.registerIconLibrary(myIcons)
```

### Image Library

```kotlin
val myImages = ImageLibrary(
    id = "MyImages",
    name = "My Images",
    images = listOf(
        ImageAsset(
            id = "background-1",
            name = "Background Image",
            path = "backgrounds/bg1.jpg",
            format = ImageFormat.JPEG,
            dimensions = Dimensions(1920, 1080),
            tags = listOf("background")
        )
    )
)

AssetManager.registerImageLibrary(myImages)
```

## Directory Structure

Place your assets in:

```
Universal/Assets/
├── Icons/
│   └── YourLibrary/
│       ├── manifest.json
│       └── svg/
│           └── your-icon.svg
└── Images/
    └── YourLibrary/
        ├── manifest.json
        ├── images/
        │   └── your-image.jpg
        └── thumbnails/
            └── your-image.jpg
```

## Manifest Format

### Icon Library Manifest

```json
{
  "id": "YourLibrary",
  "name": "Your Icon Library",
  "version": "1.0.0",
  "icons": [
    {
      "id": "icon-id",
      "name": "Icon Name",
      "hasSvg": true,
      "pngSizes": [24, 48],
      "tags": ["tag1", "tag2"],
      "category": "Category"
    }
  ]
}
```

### Image Library Manifest

```json
{
  "id": "YourLibrary",
  "name": "Your Image Library",
  "images": [
    {
      "id": "image-id",
      "name": "Image Name",
      "fileName": "image.jpg",
      "format": "JPEG",
      "width": 1920,
      "height": 1080,
      "fileSize": 245678,
      "hasThumbnail": true,
      "tags": ["tag1", "tag2"],
      "category": "Category"
    }
  ]
}
```

## Reference Format

Assets are referenced using the format: `LibraryId:AssetId`

Examples:
- `MaterialIcons:home`
- `CustomLibrary:user-avatar`
- `Backgrounds:gradient-blue-purple`
- `Photos:hero-image-1`

## Next Steps

- Read the full [README.md](README.md) for complete documentation
- See [EXAMPLES.md](EXAMPLES.md) for detailed code examples
- Check [INTEGRATION.md](INTEGRATION.md) for AvaElements integration
- Explore the example libraries in `Universal/Assets/`

## Common Tasks

### Search by Tag

```kotlin
val icons = AssetManager.searchIcons(tags = setOf("navigation"))
```

### Filter Images by Size

```kotlin
val images = AssetManager.searchImages(
    filter = ImageFilter(
        minWidth = 1920,
        minHeight = 1080,
        orientation = ImageOrientation.LANDSCAPE
    )
)
```

### Get All Libraries

```kotlin
val iconLibraries = AssetManager.getAllIconLibraries()
val imageLibraries = AssetManager.getAllImageLibraries()
```

### Check if Asset Exists

```kotlin
val icon = AssetManager.getIcon("MaterialIcons:home")
if (icon != null) {
    // Use icon
} else {
    // Show fallback
}
```

## Support

- Documentation: See README.md, EXAMPLES.md, and INTEGRATION.md
- Source Code: `/Volumes/M Drive/Coding/Avanues/Universal/Core/AssetManager/`
- Assets: `/Volumes/M Drive/Coding/Avanues/Universal/Assets/`
