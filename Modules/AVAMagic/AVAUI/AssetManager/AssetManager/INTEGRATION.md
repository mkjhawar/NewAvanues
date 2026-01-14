# AssetManager Integration Guide

This guide shows how to integrate AssetManager with AvaElements and other parts of the VoiceAvenue Universal platform.

## Table of Contents

1. [AvaElements Integration](#avaelements-integration)
2. [Icon Component Enhancement](#icon-component-enhancement)
3. [Image Component Enhancement](#image-component-enhancement)
4. [Asset Picker UI Component](#asset-picker-ui-component)
5. [Build System Integration](#build-system-integration)

## AvaElements Integration

### Icon Component Enhancement

Extend the existing Icon component in AvaElements to support AssetManager references:

```kotlin
// File: AvaElements/src/commonMain/kotlin/components/Icon.kt

import com.augmentalis.universal.assetmanager.AssetManager
import com.augmentalis.universal.assetmanager.Icon as AssetIcon
import com.augmentalis.universal.assetmanager.IconFormat

@Composable
fun Icon(
    reference: String,
    size: CSSValue = 24.px,
    color: CSSValue? = null,
    className: String? = null,
    block: (IconScope.() -> Unit)? = null
) {
    val iconData = remember { mutableStateOf<AssetIcon?>(null) }
    val loading = remember { mutableStateOf(true) }

    LaunchedEffect(reference) {
        loading.value = true
        iconData.value = AssetManager.getIcon(reference)
        loading.value = false
    }

    val icon = iconData.value

    Div {
        classes(className, "icon-container")

        css {
            width = size
            height = size
            display = Display.InlineFlex
            alignItems = AlignItems.Center
            justifyContent = JustifyContent.Center

            color?.let { this.color = it }
        }

        when {
            loading.value -> {
                // Show loading placeholder
                Div {
                    classes("icon-loading")
                    css {
                        width = size
                        height = size
                        backgroundColor = Color("#f0f0f0")
                        borderRadius = 4.px
                    }
                }
            }
            icon != null -> {
                when {
                    icon.svg != null -> {
                        // Render SVG
                        RawHtml(icon.svg!!) {
                            css {
                                width = size
                                height = size
                            }
                        }
                    }
                    icon.png != null -> {
                        // Render PNG
                        val requestedSize = parsePxValue(size)
                        val pngData = icon.getPngForSize(requestedSize)

                        pngData?.let { data ->
                            Img {
                                src = "data:image/png;base64,${data.toBase64()}"
                                alt = icon.name
                                css {
                                    width = size
                                    height = size
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Show error/fallback
                Div {
                    classes("icon-error")
                    text("?")
                    css {
                        width = size
                        height = size
                        display = Display.Flex
                        alignItems = AlignItems.Center
                        justifyContent = JustifyContent.Center
                        backgroundColor = Color("#ffebee")
                        color = Color("#c62828")
                        fontWeight = FontWeight.Bold
                    }
                }
            }
        }

        block?.invoke(IconScope(this))
    }
}

class IconScope(private val element: DivScope) {
    fun onClick(handler: () -> Unit) {
        element.onClick { handler() }
    }

    fun css(block: CSSBuilder.() -> Unit) {
        element.css(block)
    }
}

// Utility to parse px values
private fun parsePxValue(value: CSSValue): Int {
    return value.toString()
        .removeSuffix("px")
        .toIntOrNull() ?: 24
}

// Utility to convert ByteArray to Base64
private fun ByteArray.toBase64(): String {
    // Platform-specific implementation
    // JVM: Base64.getEncoder().encodeToString(this)
    // JS: btoa(String.fromCharCode.apply(null, this))
    // Native: Use platform Base64 encoder
    return "" // Implement based on platform
}
```

### Image Component Enhancement

```kotlin
// File: AvaElements/src/commonMain/kotlin/components/Image.kt

import com.augmentalis.universal.assetmanager.AssetManager
import com.augmentalis.universal.assetmanager.ImageAsset

@Composable
fun Image(
    reference: String,
    alt: String? = null,
    width: CSSValue? = null,
    height: CSSValue? = null,
    objectFit: ObjectFit = ObjectFit.Cover,
    loading: ImageLoading = ImageLoading.Lazy,
    showThumbnailFirst: Boolean = true,
    className: String? = null,
    block: (ImageScope.() -> Unit)? = null
) {
    val imageData = remember { mutableStateOf<ImageAsset?>(null) }
    val imageUrl = remember { mutableStateOf<String?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val showFullImage = remember { mutableStateOf(!showThumbnailFirst) }

    LaunchedEffect(reference) {
        isLoading.value = true

        val image = AssetManager.getImage(reference)
        imageData.value = image

        if (image != null) {
            // Load image URL from repository
            val repo = AssetManager.repository
            val ref = parseImageReference(reference)

            if (ref != null) {
                val imageDataBytes = repo.loadImageData(ref.first, ref.second)
                imageDataBytes.getOrNull()?.let { bytes ->
                    imageUrl.value = createBlobUrl(bytes, image.format.mimeType)
                }
            }
        }

        isLoading.value = false
    }

    val image = imageData.value

    Img {
        classes(className, "managed-image")

        src = when {
            isLoading.value && showThumbnailFirst && image?.thumbnail != null -> {
                // Show thumbnail while loading
                "data:image/jpeg;base64,${image.thumbnail!!.toBase64()}"
            }
            imageUrl.value != null -> {
                imageUrl.value!!
            }
            else -> ""
        }

        this.alt = alt ?: image?.name ?: ""

        css {
            width?.let { this.width = it }
            height?.let { this.height = it }
            this.objectFit = objectFit
        }

        when (loading) {
            ImageLoading.Lazy -> attribute("loading", "lazy")
            ImageLoading.Eager -> attribute("loading", "eager")
        }

        // Load full image after thumbnail
        if (showThumbnailFirst && !showFullImage.value) {
            onLoad {
                showFullImage.value = true
            }
        }

        block?.invoke(ImageScope(this))
    }
}

class ImageScope(private val element: ImgScope) {
    fun onClick(handler: () -> Unit) {
        element.onClick { handler() }
    }

    fun css(block: CSSBuilder.() -> Unit) {
        element.css(block)
    }
}

enum class ImageLoading {
    Lazy,
    Eager
}

enum class ObjectFit {
    Cover,
    Contain,
    Fill,
    None,
    ScaleDown
}

private fun parseImageReference(reference: String): Pair<String, String>? {
    val parts = reference.split(":", limit = 2)
    return if (parts.size == 2) Pair(parts[0], parts[1]) else null
}

private fun createBlobUrl(bytes: ByteArray, mimeType: String): String {
    // Platform-specific implementation
    // JVM: Create data URL or temp file URL
    // JS: URL.createObjectURL(new Blob([bytes], { type: mimeType }))
    // Native: Create temp file and return file URL
    return ""
}
```

### Background Image Support

```kotlin
// Extension function for background images
fun CSSBuilder.backgroundImage(reference: String) {
    scope.launch {
        val image = AssetManager.getImage(reference)

        if (image != null) {
            val repo = AssetManager.repository
            val ref = parseImageReference(reference)

            ref?.let { (libraryId, imageId) ->
                val imageData = repo.loadImageData(libraryId, imageId)
                imageData.getOrNull()?.let { bytes ->
                    val url = createBlobUrl(bytes, image.format.mimeType)
                    this@backgroundImage.backgroundImage = "url($url)"
                }
            }
        }
    }
}

// Usage
Div {
    css {
        backgroundImage("Backgrounds:gradient-blue-purple")
        backgroundSize = BackgroundSize.Cover
        backgroundPosition = BackgroundPosition.Center
    }
}
```

## Asset Picker UI Component

Create a reusable asset picker component for selecting icons and images:

```kotlin
@Composable
fun AssetPicker(
    type: AssetPickerType,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val searchQuery = remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf<String?>(null) }
    val assets = remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    LaunchedEffect(searchQuery.value, selectedCategory.value) {
        assets.value = when (type) {
            AssetPickerType.Icon -> {
                AssetManager.searchIcons(
                    query = searchQuery.value.takeIf { it.isNotBlank() },
                    category = selectedCategory.value
                ).map { SearchResult.IconResult(it) }
            }
            AssetPickerType.Image -> {
                AssetManager.searchImages(
                    filter = ImageFilter(
                        query = searchQuery.value.takeIf { it.isNotBlank() },
                        category = selectedCategory.value
                    )
                ).map { SearchResult.ImageResult(it) }
            }
        }
    }

    Modal {
        onClose { onClose() }

        Div {
            classes("asset-picker")
            css {
                width = 800.px
                height = 600.px
                display = Display.Flex
                flexDirection = FlexDirection.Column
            }

            // Header
            Div {
                classes("asset-picker-header")
                css {
                    padding = 16.px
                    borderBottom = Border(1.px, Colors.gray300)
                }

                H2("Select ${type.name}")

                Input {
                    placeholder = "Search assets..."
                    value = searchQuery.value
                    onInput { searchQuery.value = it }
                    css {
                        width = 100.percent
                        marginTop = 8.px
                    }
                }
            }

            // Content
            Div {
                classes("asset-picker-content")
                css {
                    flex = 1
                    overflow = Overflow.Auto
                    padding = 16.px
                }

                Grid {
                    columns = if (type == AssetPickerType.Icon) 6 else 3
                    gap = 16.px

                    assets.value.forEach { result ->
                        when (result) {
                            is SearchResult.IconResult -> {
                                IconTile(result.data) {
                                    onSelect(result.data.reference)
                                    onClose()
                                }
                            }
                            is SearchResult.ImageResult -> {
                                ImageTile(result.data) {
                                    onSelect(result.data.reference)
                                    onClose()
                                }
                            }
                        }
                    }
                }
            }

            // Footer
            Div {
                classes("asset-picker-footer")
                css {
                    padding = 16.px
                    borderTop = Border(1.px, Colors.gray300)
                    display = Display.Flex
                    justifyContent = JustifyContent.End
                }

                Button {
                    text("Cancel")
                    onClick { onClose() }
                }
            }
        }
    }
}

@Composable
private fun IconTile(result: IconSearchResult, onClick: () -> Unit) {
    Div {
        classes("icon-tile")
        css {
            padding = 16.px
            border = Border(1.px, Colors.gray300)
            borderRadius = 8.px
            cursor = Cursor.Pointer
            textAlign = TextAlign.Center
            hover {
                backgroundColor = Colors.gray100
            }
        }

        onClick { onClick() }

        Icon(result.reference) {
            size = 48.px
        }

        P(result.icon.name) {
            css {
                marginTop = 8.px
                fontSize = 12.px
                color = Colors.gray600
            }
        }
    }
}

@Composable
private fun ImageTile(result: ImageSearchResult, onClick: () -> Unit) {
    Div {
        classes("image-tile")
        css {
            border = Border(1.px, Colors.gray300)
            borderRadius = 8.px
            overflow = Overflow.Hidden
            cursor = Cursor.Pointer
            hover {
                boxShadow = "0 4px 8px rgba(0,0,0,0.1)"
            }
        }

        onClick { onClick() }

        Image(result.reference) {
            width = 100.percent
            height = 150.px
            objectFit = ObjectFit.Cover
        }

        Div {
            padding = 12.px

            P(result.image.name) {
                css {
                    fontSize = 14.px
                    fontWeight = FontWeight.Medium
                }
            }

            P("${result.image.dimensions.width}x${result.image.dimensions.height}") {
                css {
                    fontSize = 12.px
                    color = Colors.gray600
                }
            }
        }
    }
}

enum class AssetPickerType {
    Icon,
    Image
}

sealed class SearchResult {
    data class IconResult(val data: IconSearchResult) : SearchResult()
    data class ImageResult(val data: ImageSearchResult) : SearchResult()
}
```

## Build System Integration

### Gradle Settings

Add AssetManager to your `settings.gradle.kts`:

```kotlin
include(":Universal:Core:AssetManager")
```

### Module Dependencies

In your app's `build.gradle.kts`:

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

### Asset Loading Task

Create a Gradle task to preload assets on build:

```kotlin
// In app's build.gradle.kts

tasks.register("preloadAssets") {
    description = "Preload asset manifests for faster startup"
    group = "build"

    doLast {
        val assetsDir = file("${rootProject.projectDir}/Universal/Assets")
        val manifestFiles = fileTree(assetsDir).matching {
            include("**/manifest.json")
        }

        println("Found ${manifestFiles.files.size} asset libraries:")
        manifestFiles.forEach { manifestFile ->
            println("  - ${manifestFile.parentFile.name}")
        }
    }
}

// Run before processResources
tasks.named("processResources") {
    dependsOn("preloadAssets")
}
```

## Platform-Specific Setup

### JVM/Android

Add image processing dependencies:

```kotlin
dependencies {
    implementation("org.imgscalr:imgscalr-lib:4.2")
    implementation("commons-io:commons-io:2.15.1")
}
```

### iOS/macOS

Configure framework exports:

```kotlin
iosTarget.binaries.framework {
    baseName = "AssetManager"
    export(project(":Universal:Core:AssetManager"))
}
```

### Web (Future)

Configure webpack to handle asset URLs:

```kotlin
browser {
    commonWebpackConfig {
        module {
            rules.add(
                json(
                    test = "\\.json$",
                    type = "json"
                )
            )
        }
    }
}
```

## Usage in Application

### Initialization

```kotlin
// In your app's main function or Application onCreate
suspend fun initializeApp() {
    // Load all asset libraries
    AssetManager.loadAllLibraries()

    // Optionally preload specific libraries
    AssetManager.getIconLibrary("MaterialIcons")
    AssetManager.getImageLibrary("Backgrounds")
}
```

### Example: User Avatar with AssetManager

```kotlin
@Composable
fun UserProfile(user: User) {
    Card {
        Row {
            // User avatar - tries custom avatar first, falls back to default
            val avatarRef = user.customAvatar
                ?: "CustomLibrary:user-avatar"

            Icon(avatarRef) {
                size = 64.px
                css {
                    borderRadius = 50.percent
                }
            }

            Column {
                H3(user.name)
                P(user.email)
            }
        }
    }
}
```

### Example: Dynamic Background Selection

```kotlin
@Composable
fun DynamicBackground() {
    val selectedBg = remember { mutableStateOf("Backgrounds:gradient-blue-purple") }
    val showPicker = remember { mutableStateOf(false) }

    Div {
        css {
            backgroundImage(selectedBg.value)
            backgroundSize = BackgroundSize.Cover
            minHeight = 100.vh
        }

        Button {
            text("Change Background")
            onClick { showPicker.value = true }
        }

        if (showPicker.value) {
            AssetPicker(
                type = AssetPickerType.Image,
                onSelect = { ref ->
                    selectedBg.value = ref
                },
                onClose = { showPicker.value = false }
            )
        }
    }
}
```

---

For more examples, see [EXAMPLES.md](EXAMPLES.md) and [README.md](README.md).
