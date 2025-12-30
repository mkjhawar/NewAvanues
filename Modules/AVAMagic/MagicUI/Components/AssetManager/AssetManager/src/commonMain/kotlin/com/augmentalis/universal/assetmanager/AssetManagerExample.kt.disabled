package com.augmentalis.universal.assetmanager

/**
 * Comprehensive examples for using the Asset Management System
 *
 * This file contains examples demonstrating:
 * - Registering icon and image libraries
 * - Searching and retrieving assets
 * - Processing uploaded assets
 * - Version management
 * - Integration with UI components
 */

/**
 * Example 1: Creating and registering an icon library
 */
suspend fun example1_CreateIconLibrary() {
    // Create icons with SVG and multiple PNG sizes
    val homeIcon = Icon(
        id = "home",
        name = "Home",
        svg = """<svg viewBox="0 0 24 24"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>""",
        png = mapOf(
            16 to byteArrayOf(), // PNG data for 16x16
            24 to byteArrayOf(), // PNG data for 24x24
            32 to byteArrayOf(), // PNG data for 32x32
            48 to byteArrayOf()  // PNG data for 48x48
        ),
        tags = listOf("house", "building", "residence"),
        category = "Navigation",
        keywords = listOf("home", "house", "main")
    )

    val userIcon = Icon(
        id = "user",
        name = "User",
        svg = """<svg viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>""",
        tags = listOf("person", "avatar", "profile"),
        category = "User",
        keywords = listOf("user", "person", "account")
    )

    // Create icon library
    val iconLibrary = IconLibrary(
        id = "MyIcons",
        name = "My Icon Library",
        version = "1.0.0",
        description = "Custom icon library for my application",
        icons = listOf(homeIcon, userIcon),
        metadata = mapOf(
            "author" to "Your Name",
            "license" to "MIT",
            "website" to "https://example.com"
        )
    )

    // Register the library
    AssetManager.registerIconLibrary(iconLibrary, persist = true)

    println("Icon library registered with ${iconLibrary.icons.size} icons")
}

/**
 * Example 2: Creating and registering an image library
 */
suspend fun example2_CreateImageLibrary() {
    // Create image assets
    val backgroundImage = ImageAsset(
        id = "gradient-blue",
        name = "Blue Gradient Background",
        path = "images/gradient-blue.png",
        format = ImageFormat.PNG,
        dimensions = Dimensions(1920, 1080),
        thumbnail = byteArrayOf(), // Thumbnail data
        fileSize = 245678,
        tags = listOf("gradient", "blue", "background"),
        category = "Backgrounds",
        metadata = mapOf(
            "colorScheme" to "blue",
            "orientation" to "landscape"
        )
    )

    val heroImage = ImageAsset(
        id = "hero-1",
        name = "Hero Image 1",
        path = "images/hero-1.jpg",
        format = ImageFormat.JPEG,
        dimensions = Dimensions(2560, 1440),
        fileSize = 512000,
        tags = listOf("hero", "banner", "main"),
        category = "Banners"
    )

    // Create image library
    val imageLibrary = ImageLibrary(
        id = "Backgrounds",
        name = "Background Images",
        description = "Collection of background images",
        images = listOf(backgroundImage, heroImage),
        metadata = mapOf(
            "collection" to "2024",
            "theme" to "modern"
        )
    )

    // Register the library
    AssetManager.registerImageLibrary(imageLibrary, persist = true)

    println("Image library registered with ${imageLibrary.images.size} images")
}

/**
 * Example 3: Searching for icons
 */
suspend fun example3_SearchIcons() {
    // Search by query
    val homeIcons = AssetManager.searchIcons(query = "home")
    println("Found ${homeIcons.size} icons matching 'home'")
    homeIcons.forEach { result ->
        println("  - ${result.libraryName}: ${result.icon.name} (${result.reference})")
    }

    // Search by tags
    val navigationIcons = AssetManager.searchIcons(tags = setOf("navigation"))
    println("\nFound ${navigationIcons.size} icons tagged 'navigation'")

    // Search by category
    val userIcons = AssetManager.searchIcons(category = "User")
    println("\nFound ${userIcons.size} icons in 'User' category")

    // Search in specific libraries
    val materialIcons = AssetManager.searchIcons(
        query = "arrow",
        libraryIds = setOf("MaterialIcons")
    )
    println("\nFound ${materialIcons.size} arrow icons in MaterialIcons")
}

/**
 * Example 4: Searching for images
 */
suspend fun example4_SearchImages() {
    // Search with filter
    val filter = ImageFilter(
        query = "gradient",
        formats = setOf(ImageFormat.PNG, ImageFormat.WEBP),
        minWidth = 1920,
        orientation = ImageOrientation.LANDSCAPE,
        tags = setOf("background")
    )

    val images = AssetManager.searchImages(filter = filter)
    println("Found ${images.size} images matching filter")
    images.forEach { result ->
        println("  - ${result.image.name}: ${result.image.dimensions.width}x${result.image.dimensions.height}")
    }

    // Search by category
    val banners = AssetManager.searchImages(
        filter = ImageFilter(category = "Banners")
    )
    println("\nFound ${banners.size} banner images")
}

/**
 * Example 5: Retrieving specific assets
 */
suspend fun example5_RetrieveAssets() {
    // Get icon by reference
    val icon = AssetManager.getIcon("MyIcons:home")
    if (icon != null) {
        println("Icon: ${icon.name}")
        println("Has SVG: ${icon.svg != null}")
        println("Available PNG sizes: ${icon.getAvailableSizes()}")

        // Get specific PNG size
        val png24 = icon.getPngForSize(24)
        println("PNG 24x24: ${png24?.size ?: 0} bytes")
    }

    // Get image by reference
    val image = AssetManager.getImage("Backgrounds:gradient-blue")
    if (image != null) {
        println("\nImage: ${image.name}")
        println("Dimensions: ${image.dimensions.width}x${image.dimensions.height}")
        println("Format: ${image.format}")
        println("File size: ${image.fileSize} bytes")
        println("Aspect ratio: ${image.getAspectRatio()}")
        println("Is landscape: ${image.isLandscape()}")
    }
}

/**
 * Example 6: Processing uploaded assets
 */
suspend fun example6_ProcessUploadedAssets() {
    // Simulate uploaded icon file
    val uploadedIconData = byteArrayOf() // Raw file bytes from upload
    val iconFileName = "new-icon.svg"

    // Process the icon (platform-specific processor needed)
    // val processor = AssetProcessor()
    // val processedIcon = processor.processIcon(
    //     fileData = uploadedIconData,
    //     fileName = iconFileName,
    //     iconId = "new-icon"
    // )

    // Simulate uploaded image file
    val uploadedImageData = byteArrayOf() // Raw file bytes from upload
    val imageFileName = "photo.jpg"

    // Process the image (platform-specific processor needed)
    // val processedImage = processor.processImage(
    //     fileData = uploadedImageData,
    //     fileName = imageFileName,
    //     imageId = "photo-1"
    // )

    println("Asset processing requires platform-specific implementation")
}

/**
 * Example 7: Version management
 */
suspend fun example7_VersionManagement() {
    val versionManager = AssetVersionManager()

    // Publish a new version
    val library = AssetManager.getIconLibrary("MyIcons")
    if (library != null) {
        val assetIds = library.icons.map { it.id }

        val result = versionManager.publishVersion(
            libraryId = "MyIcons",
            libraryType = LibraryType.ICON,
            version = "1.0.0",
            changelog = "Initial release with home and user icons",
            assetIds = assetIds,
            metadata = mapOf("releaseDate" to "2024-01-01")
        )

        result.onSuccess { version ->
            println("Published version: ${version.version}")
            println("Changelog: ${version.changelog}")
            println("Assets: ${version.assetIds.size}")
        }
    }

    // Get version history
    val history = versionManager.getVersionHistory("MyIcons", LibraryType.ICON)
    println("\nVersion history:")
    history.getSortedVersions().forEach { version ->
        println("  v${version.version} - ${version.changelog}")
    }

    // Get current version
    val currentVersion = versionManager.getCurrentVersion("MyIcons", LibraryType.ICON)
    println("\nCurrent version: ${currentVersion?.version}")

    // Publish another version
    versionManager.publishVersion(
        libraryId = "MyIcons",
        libraryType = LibraryType.ICON,
        version = "1.1.0",
        changelog = "Added new icons and fixed bugs",
        assetIds = assetIds + listOf("settings", "logout"),
        metadata = mapOf("releaseDate" to "2024-02-01")
    )

    // Compare versions
    val diff = versionManager.compareVersions(
        libraryId = "MyIcons",
        libraryType = LibraryType.ICON,
        fromVersion = "1.0.0",
        toVersion = "1.1.0"
    )

    if (diff != null) {
        println("\nChanges from 1.0.0 to 1.1.0:")
        println("Added: ${diff.addedAssets}")
        println("Removed: ${diff.removedAssets}")
        println("Total changes: ${diff.getTotalChanges()}")
    }

    // Rollback to previous version
    val rollbackResult = versionManager.rollback(
        libraryId = "MyIcons",
        libraryType = LibraryType.ICON,
        targetVersion = "1.0.0"
    )

    rollbackResult.onSuccess { newVersion ->
        println("\nRolled back to 1.0.0 as version ${newVersion.version}")
    }
}

/**
 * Example 8: Using assets with UI components
 */
suspend fun example8_UIIntegration() {
    // Configure CDN (optional)
    val cdnConfig = CdnConfig(
        baseUrl = "https://cdn.example.com",
        iconPath = "icons",
        imagePath = "images",
        cachingEnabled = true
    )
    cdnConfig.apply()

    // Get icon URL
    val iconUrl = AssetResolver.getIconUrl(
        reference = "MyIcons:home",
        format = IconFormat.SVG,
        size = 24
    )
    println("Icon URL: $iconUrl")

    // Get image URL
    val imageUrl = AssetResolver.getImageUrl("Backgrounds:gradient-blue")
    println("Image URL: $imageUrl")

    // Resolve icon source (for direct use in components)
    val iconSource = AssetResolver.resolveIcon(
        reference = "MyIcons:home",
        format = IconFormat.SVG
    )
    println("Icon source type: ${iconSource?.javaClass?.simpleName}")

    // Resolve image source
    val imageSource = AssetResolver.resolveImage("Backgrounds:gradient-blue")
    println("Image source type: ${imageSource?.javaClass?.simpleName}")

    // Preload assets for better performance
    AssetPreloader.preloadIcons(
        listOf("MyIcons:home", "MyIcons:user")
    )
    AssetPreloader.preloadImages(
        listOf("Backgrounds:gradient-blue")
    )
    println("\nAssets preloaded")
}

/**
 * Example 9: Batch operations
 */
suspend fun example9_BatchOperations() {
    // Load all libraries at startup
    AssetManager.loadAllLibraries()

    val iconLibraries = AssetManager.getAllIconLibraries()
    val imageLibraries = AssetManager.getAllImageLibraries()

    println("Loaded libraries:")
    println("  Icon libraries: ${iconLibraries.size}")
    println("  Image libraries: ${imageLibraries.size}")

    // Get all icons across all libraries
    val allIcons = AssetManager.searchIcons()
    println("\nTotal icons available: ${allIcons.size}")

    // Get all images across all libraries
    val allImages = AssetManager.searchImages()
    println("Total images available: ${allImages.size}")

    // Group icons by category
    val iconsByCategory = allIcons.groupBy { it.icon.category }
    println("\nIcons by category:")
    iconsByCategory.forEach { (category, icons) ->
        println("  ${category ?: "Uncategorized"}: ${icons.size} icons")
    }
}

/**
 * Example 10: Custom library with variants
 */
suspend fun example10_IconVariants() {
    // Icon libraries can have variants (filled, outlined, rounded, etc.)
    // This is useful for Material Design icons or custom icon sets

    val homeIconFilled = Icon(
        id = "home-filled",
        name = "Home (Filled)",
        svg = """<svg viewBox="0 0 24 24"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>""",
        tags = listOf("home", "filled"),
        category = "Navigation"
    )

    val homeIconOutlined = Icon(
        id = "home-outlined",
        name = "Home (Outlined)",
        svg = """<svg viewBox="0 0 24 24"><path d="M12 5.69l5 4.5V18h-2v-6H9v6H7v-7.81l5-4.5M12 3L2 12h3v8h6v-6h2v6h6v-8h3L12 3z"/></svg>""",
        tags = listOf("home", "outlined"),
        category = "Navigation"
    )

    val variantLibrary = IconLibrary(
        id = "MaterialIconsVariants",
        name = "Material Icons with Variants",
        version = "1.0.0",
        description = "Material Design icons with different style variants",
        icons = listOf(homeIconFilled, homeIconOutlined),
        metadata = mapOf("variants" to "filled,outlined")
    )

    AssetManager.registerIconLibrary(variantLibrary)

    // Search for specific variants
    val filledIcons = AssetManager.searchIcons(tags = setOf("filled"))
    val outlinedIcons = AssetManager.searchIcons(tags = setOf("outlined"))

    println("Filled icons: ${filledIcons.size}")
    println("Outlined icons: ${outlinedIcons.size}")
}

/**
 * Main example runner
 */
suspend fun runAllExamples() {
    println("=== Asset Management System Examples ===\n")

    println("Example 1: Create Icon Library")
    example1_CreateIconLibrary()

    println("\n\nExample 2: Create Image Library")
    example2_CreateImageLibrary()

    println("\n\nExample 3: Search Icons")
    example3_SearchIcons()

    println("\n\nExample 4: Search Images")
    example4_SearchImages()

    println("\n\nExample 5: Retrieve Assets")
    example5_RetrieveAssets()

    println("\n\nExample 6: Process Uploaded Assets")
    example6_ProcessUploadedAssets()

    println("\n\nExample 7: Version Management")
    example7_VersionManagement()

    println("\n\nExample 8: UI Integration")
    example8_UIIntegration()

    println("\n\nExample 9: Batch Operations")
    example9_BatchOperations()

    println("\n\nExample 10: Icon Variants")
    example10_IconVariants()
}
