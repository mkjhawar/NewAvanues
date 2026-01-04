# Asset Manager Usage Examples

This document provides practical examples for using the Asset Manager system.

## Table of Contents

1. [Basic Setup](#basic-setup)
2. [Creating and Registering Libraries](#creating-and-registering-libraries)
3. [Loading Assets](#loading-assets)
4. [Searching and Filtering](#searching-and-filtering)
5. [Integration with AvaElements](#integration-with-avaelements)
6. [Advanced Use Cases](#advanced-use-cases)

## Basic Setup

### Initialize AssetManager

```kotlin
import com.augmentalis.universal.assetmanager.*

// Load all libraries from storage on application startup
suspend fun initializeAssets() {
    AssetManager.loadAllLibraries()

    // Optionally, register additional runtime libraries
    val customLibrary = createCustomIconLibrary()
    AssetManager.registerIconLibrary(customLibrary)
}
```

### Creating Icon Libraries from Files

```kotlin
suspend fun importIconLibrary(
    libraryId: String,
    libraryName: String,
    iconFiles: List<File>
) {
    val processor = AssetProcessor()
    val icons = mutableListOf<Icon>()

    iconFiles.forEach { file ->
        val fileData = file.readBytes()
        val iconId = AssetProcessorUtils.generateIdFromFileName(file.name)

        val icon = processor.processIcon(
            fileData = fileData,
            fileName = file.name,
            iconId = iconId
        )

        icons.add(icon)
    }

    val library = IconLibrary(
        id = libraryId,
        name = libraryName,
        version = "1.0.0",
        icons = icons
    )

    AssetManager.registerIconLibrary(library, persist = true)
}
```

## Creating and Registering Libraries

### Manual Icon Library Creation

```kotlin
fun createCustomIconLibrary(): IconLibrary {
    // SVG icon
    val homeIconSvg = """
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
          <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
        </svg>
    """.trimIndent()

    val homeIcon = Icon(
        id = "home",
        name = "Home",
        svg = homeIconSvg,
        tags = listOf("navigation", "house", "main"),
        category = "Navigation"
    )

    // Icon with multiple PNG sizes
    val userIcon = Icon(
        id = "user",
        name = "User Profile",
        png = mapOf(
            24 to loadIconPng("user_24.png"),
            48 to loadIconPng("user_48.png"),
            96 to loadIconPng("user_96.png")
        ),
        tags = listOf("user", "profile", "account"),
        category = "User"
    )

    return IconLibrary(
        id = "AppIcons",
        name = "Application Icons",
        version = "1.0.0",
        description = "Core application icon set",
        icons = listOf(homeIcon, userIcon),
        metadata = mapOf(
            "author" to "Augmentalis",
            "license" to "MIT"
        )
    )
}
```

### Image Library Creation

```kotlin
suspend fun createBackgroundLibrary(): ImageLibrary {
    val processor = AssetProcessor()

    // Process a gradient background
    val gradientData = loadImageFile("gradient-blue-purple.png")
    val gradientDimensions = processor.extractDimensions(gradientData)
    val gradientThumbnail = processor.generateThumbnail(
        imageData = gradientData,
        width = 256,
        height = 256
    )

    val gradientImage = ImageAsset(
        id = "gradient-blue-purple",
        name = "Blue to Purple Gradient",
        path = "backgrounds/gradient-blue-purple.png",
        format = ImageFormat.PNG,
        dimensions = gradientDimensions,
        thumbnail = gradientThumbnail,
        fileSize = gradientData.size.toLong(),
        tags = listOf("gradient", "blue", "purple", "background"),
        category = "Gradients"
    )

    return ImageLibrary(
        id = "Backgrounds",
        name = "Background Images",
        description = "Collection of background images",
        images = listOf(gradientImage)
    )
}
```

## Loading Assets

### Get Icon by Reference

```kotlin
// Simple reference format: "LibraryId:IconId"
suspend fun loadIcon() {
    val icon = AssetManager.getIcon("MaterialIcons:home")

    icon?.let {
        when {
            it.svg != null -> {
                // Use SVG
                println("SVG Icon: ${it.svg}")
            }
            it.png != null -> {
                // Use PNG, get closest size to 48px
                val pngData = it.getPngForSize(48)
                println("PNG Icon data: ${pngData?.size} bytes")
            }
        }
    }
}
```

### Get Image by Reference

```kotlin
suspend fun loadHeroImage() {
    val image = AssetManager.getImage("Photos:hero-image-1")

    image?.let {
        println("Image: ${it.name}")
        println("Dimensions: ${it.dimensions.width}x${it.dimensions.height}")
        println("Format: ${it.format}")
        println("Has thumbnail: ${it.thumbnail != null}")

        // Load actual image data from repository
        val repository = AssetManager.repository
        val imageData = repository.loadImageData(
            libraryId = "Photos",
            imageId = "hero-image-1"
        )
    }
}
```

### Load All Libraries

```kotlin
suspend fun listAllAssets() {
    val iconLibraries = AssetManager.getAllIconLibraries()
    val imageLibraries = AssetManager.getAllImageLibraries()

    println("=== Icon Libraries ===")
    iconLibraries.forEach { library ->
        println("${library.name} (${library.icons.size} icons)")
        library.icons.forEach { icon ->
            println("  - ${icon.name} [${icon.id}]")
        }
    }

    println("\n=== Image Libraries ===")
    imageLibraries.forEach { library ->
        println("${library.name} (${library.images.size} images)")
        library.images.forEach { image ->
            println("  - ${image.name} [${image.id}]")
        }
    }
}
```

## Searching and Filtering

### Search Icons

```kotlin
// Simple text search
suspend fun searchIconsByQuery(query: String) {
    val results = AssetManager.searchIcons(query = query)

    println("Found ${results.size} icons matching '$query':")
    results.forEach { result ->
        println("  ${result.reference} - ${result.icon.name}")
        println("    Library: ${result.libraryName}")
        println("    Category: ${result.icon.category}")
        println("    Tags: ${result.icon.tags.joinToString()}")
    }
}

// Search with filters
suspend fun searchNavigationIcons() {
    val results = AssetManager.searchIcons(
        query = null,
        tags = setOf("navigation"),
        category = "Navigation"
    )

    results.forEach { result ->
        println("${result.reference}: ${result.icon.name}")
    }
}

// Search specific libraries
suspend fun searchInMaterialIcons(query: String) {
    val results = AssetManager.searchIcons(
        query = query,
        libraryIds = setOf("MaterialIcons")
    )

    println("Material Icons matching '$query': ${results.size}")
}
```

### Search Images

```kotlin
// Search with comprehensive filters
suspend fun searchLandscapeBackgrounds() {
    val filter = ImageFilter(
        query = "background",
        formats = setOf(ImageFormat.JPEG, ImageFormat.PNG),
        minWidth = 1920,
        minHeight = 1080,
        orientation = ImageOrientation.LANDSCAPE,
        tags = setOf("background", "gradient")
    )

    val results = AssetManager.searchImages(filter = filter)

    println("Found ${results.size} landscape backgrounds:")
    results.forEach { result ->
        val img = result.image
        println("  ${result.reference}")
        println("    ${img.dimensions.width}x${img.dimensions.height}")
        println("    Aspect ratio: ${"%.2f".format(img.getAspectRatio())}")
    }
}

// Search by specific dimensions
suspend fun findSquareImages() {
    val filter = ImageFilter(
        minWidth = 800,
        maxWidth = 1200,
        minHeight = 800,
        maxHeight = 1200,
        orientation = ImageOrientation.SQUARE
    )

    val results = AssetManager.searchImages(filter = filter)

    results.forEach { result ->
        println("${result.reference}: ${result.image.dimensions.width}x${result.image.dimensions.height}")
    }
}
```

## Integration with AvaElements

### Using Icons in Components

```kotlin
import com.augmentalis.universal.avaelements.*

// Simple icon usage
Div {
    Icon("MaterialIcons:home") {
        size = 24.px
        color = Colors.primary
    }

    Icon("CustomLibrary:user-avatar") {
        size = 48.px
        color = Colors.secondary
    }
}

// Dynamic icon selection
fun UserAvatar(userId: String) {
    val iconRef = getUserIconReference(userId) // Returns "CustomLibrary:user-$userId"

    Icon(iconRef) {
        size = 64.px
        borderRadius = 50.percent
        border = Border(1.px, Colors.gray300)
    }
}

// Icon button
Button {
    Icon("MaterialIcons:search") {
        size = 20.px
        marginRight = 8.px
    }
    Text("Search")
}
```

### Using Images in Components

```kotlin
// Hero section with background
Section {
    background = BackgroundImage("Backgrounds:gradient-blue-purple")
    backgroundSize = BackgroundSize.Cover

    Div {
        padding = 64.px
        H1("Welcome to VoiceAvenue")
    }
}

// Product showcase
Grid {
    columns = 3
    gap = 16.px

    products.forEach { product ->
        Card {
            Image("Products:${product.id}") {
                width = 100.percent
                height = 200.px
                objectFit = ObjectFit.Cover
            }

            Div {
                padding = 16.px
                H3(product.name)
                P(product.description)
            }
        }
    }
}

// Lazy-loaded gallery
fun PhotoGallery(photoIds: List<String>) {
    Grid {
        columns = 4
        gap = 8.px

        photoIds.forEach { photoId ->
            Image("Photos:$photoId") {
                width = 100.percent
                aspectRatio = "1 / 1"
                objectFit = ObjectFit.Cover
                loading = ImageLoading.Lazy
                borderRadius = 8.px

                onClick {
                    showLightbox(photoId)
                }
            }
        }
    }
}
```

### Dynamic Asset Loading

```kotlin
// Component that loads and displays asset metadata
@Composable
fun AssetPreview(reference: String) {
    val asset = remember { mutableStateOf<Any?>(null) }
    val loading = remember { mutableStateOf(true) }

    LaunchedEffect(reference) {
        loading.value = true

        // Try to load as icon first
        val icon = AssetManager.getIcon(reference)
        if (icon != null) {
            asset.value = icon
        } else {
            // Try as image
            val image = AssetManager.getImage(reference)
            asset.value = image
        }

        loading.value = false
    }

    Div {
        if (loading.value) {
            Text("Loading...")
        } else {
            when (val a = asset.value) {
                is Icon -> {
                    Icon(reference) { size = 64.px }
                    Text("Icon: ${a.name}")
                    Text("Tags: ${a.tags.joinToString()}")
                }
                is ImageAsset -> {
                    Image(reference) { width = 200.px }
                    Text("Image: ${a.name}")
                    Text("Dimensions: ${a.dimensions.width}x${a.dimensions.height}")
                }
                else -> {
                    Text("Asset not found: $reference")
                }
            }
        }
    }
}
```

## Advanced Use Cases

### Batch Import Icons

```kotlin
suspend fun batchImportIcons(
    libraryId: String,
    libraryName: String,
    directory: File
) {
    val processor = AssetProcessor()
    val config = ProcessingConfig(
        generateMultipleSizes = true,
        iconSizes = listOf(16, 24, 32, 48, 64, 96, 128),
        extractTags = true
    )

    val icons = mutableListOf<Icon>()
    val stats = BatchProcessingStats(
        totalFiles = 0,
        successCount = 0,
        errorCount = 0,
        skippedCount = 0,
        totalSizeBytes = 0L,
        processingTimeMs = 0L
    )

    val startTime = System.currentTimeMillis()

    directory.listFiles()?.forEach { file ->
        if (file.extension in listOf("svg", "png")) {
            try {
                val icon = processor.processIcon(
                    fileData = file.readBytes(),
                    fileName = file.name,
                    iconId = AssetProcessorUtils.generateIdFromFileName(file.name)
                )
                icons.add(icon)
                stats.successCount++
            } catch (e: Exception) {
                println("Error processing ${file.name}: ${e.message}")
                stats.errorCount++
            }
        }
    }

    val library = IconLibrary(
        id = libraryId,
        name = libraryName,
        version = "1.0.0",
        icons = icons
    )

    AssetManager.registerIconLibrary(library)

    println("Import complete:")
    println("  Success: ${stats.successCount}")
    println("  Errors: ${stats.errorCount}")
    println("  Time: ${System.currentTimeMillis() - startTime}ms")
}
```

### Custom Asset Repository

```kotlin
// Example: In-memory repository for testing
class InMemoryAssetRepository : AssetRepository {
    private val iconLibraries = mutableMapOf<String, IconLibrary>()
    private val imageLibraries = mutableMapOf<String, ImageLibrary>()
    private val iconData = mutableMapOf<String, ByteArray>()
    private val imageData = mutableMapOf<String, ByteArray>()

    override suspend fun saveIconLibrary(library: IconLibrary): Result<Unit> {
        iconLibraries[library.id] = library
        return Result.success(Unit)
    }

    override suspend fun loadIconLibrary(id: String): Result<IconLibrary?> {
        return Result.success(iconLibraries[id])
    }

    override suspend fun loadAllIconLibraries(): List<IconLibrary> {
        return iconLibraries.values.toList()
    }

    // ... implement other methods
}

// Use in tests
@Test
fun testAssetSearch() = runTest {
    val testRepo = InMemoryAssetRepository()
    AssetManager.repository = testRepo

    val library = IconLibrary(
        id = "test",
        name = "Test Library",
        version = "1.0.0",
        icons = listOf(/* test icons */)
    )

    AssetManager.registerIconLibrary(library)

    val results = AssetManager.searchIcons(query = "home")
    assertEquals(1, results.size)
}
```

### Icon Color Variants

```kotlin
suspend fun generateColorVariants(
    iconReference: String,
    colors: List<String>
): Map<String, Icon> {
    val icon = AssetManager.getIcon(iconReference) ?: return emptyMap()

    if (icon.svg == null) {
        println("Icon must have SVG format for color variants")
        return emptyMap()
    }

    return colors.associateWith { color ->
        // Modify SVG to change fill color
        val coloredSvg = icon.svg.replace(
            """fill="[^"]*"""".toRegex(),
            """fill="$color""""
        )

        icon.copy(
            id = "${icon.id}-$color",
            svg = coloredSvg,
            tags = icon.tags + listOf("variant", color)
        )
    }
}
```

### Asset Analytics

```kotlin
class AssetUsageTracker {
    private val usageCount = mutableMapOf<String, Int>()

    fun trackAssetUsage(reference: String) {
        usageCount[reference] = (usageCount[reference] ?: 0) + 1
    }

    fun getMostUsedAssets(limit: Int = 10): List<Pair<String, Int>> {
        return usageCount.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun getUnusedAssets(allReferences: List<String>): List<String> {
        return allReferences.filter { it !in usageCount }
    }
}

// Usage
val tracker = AssetUsageTracker()

Icon("MaterialIcons:home") {
    onRender {
        tracker.trackAssetUsage("MaterialIcons:home")
    }
}

// Later, analyze usage
val topAssets = tracker.getMostUsedAssets(10)
println("Top 10 most used assets:")
topAssets.forEach { (ref, count) ->
    println("  $ref: $count times")
}
```

## Helper Functions

```kotlin
// Extension function to check if icon has specific size
fun Icon.hasSize(size: Int): Boolean {
    return png?.containsKey(size) == true
}

// Extension function to get best icon format
fun Icon.getBestFormat(): IconFormat? {
    return when {
        svg != null -> IconFormat.SVG
        png != null -> IconFormat.PNG
        else -> null
    }
}

// Helper to create icon reference
fun iconRef(libraryId: String, iconId: String): String {
    return "$libraryId:$iconId"
}

// Helper to create image reference
fun imageRef(libraryId: String, imageId: String): String {
    return "$libraryId:$imageId"
}
```

## Best Practices

1. **Preload Critical Assets**: Load frequently used assets on app startup
2. **Use Lazy Loading**: For galleries, use lazy loading to improve performance
3. **Cache Search Results**: Cache search results for common queries
4. **Validate References**: Always check if asset exists before using
5. **Use Thumbnails**: Display thumbnails first, load full images on demand
6. **Tag Consistently**: Use consistent tagging conventions across libraries
7. **Version Libraries**: Update library versions when making changes
8. **Monitor Usage**: Track asset usage to identify unused assets

---

For more information, see the main [README.md](README.md).
