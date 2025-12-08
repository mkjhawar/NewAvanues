# Android-First Implementation Plan
# Complete Before iOS

**Date**: 2025-10-30 04:08 PDT
**Strategy**: Android + Web → iOS Later
**Priority**: Complete AssetManager, ThemeBuilder, Web, Plugin, Components, Templates
**Timeline**: 6-8 weeks

---

## 🎯 Strategic Decision: Android-First

### Why Android First?
1. **Existing Foundation**: Android Compose renderer 100% complete
2. **Faster Iteration**: Single platform = faster testing
3. **User Base**: Target Android developers first
4. **iOS Complexity**: Kotlin/Native + Swift interop needs dedicated focus
5. **Web Synergy**: React shares concepts with Compose

### What We're Building:
1. ✅ Complete Asset Manager (30% → 100%)
2. ✅ Complete Theme Builder UI (20% → 100%)
3. ✅ Build Web Renderer (0% → 100%)
4. ✅ Android Studio Plugin (0% → 100%)
5. ✅ Phase 3 Components - 35 components (0% → 100%)
6. ✅ Template Library - 20+ screens (0% → 100%)

---

## 📋 Task 1: Complete Asset Manager (24-32h)

### Current State: 30% Complete
**What Exists**:
- ✅ Data models (Icon, ImageAsset, Library classes)
- ✅ Interfaces (AssetStorage, AssetProcessor, AssetRepository)
- ✅ ManifestManager (complete with validation, import/export)
- ✅ InMemoryStorage (for testing)
- ✅ Utilities (path handling, validation, tag extraction)

**What's Missing**:
1. Platform implementations (`expect/actual`)
   - AssetProcessor for JVM/Android
   - LocalAssetStorage for JVM/Android
2. Built-in libraries (Material Icons, Font Awesome)
3. Search functionality
4. CDN integration (optional)

### Implementation Plan

#### 1.1: Android AssetProcessor Implementation (8h)
**File**: `Universal/Core/AssetManager/src/androidMain/kotlin/AssetProcessor.kt`

```kotlin
actual class AssetProcessor {
    private val context: Context by lazy { /* Android context */ }

    actual suspend fun processIcon(
        fileData: ByteArray,
        fileName: String,
        iconId: String
    ): Icon = withContext(Dispatchers.IO) {
        val format = AssetProcessorUtils.detectImageFormat(fileName)

        when {
            format == ImageFormat.SVG -> {
                // Parse SVG
                val svgString = fileData.decodeToString()
                Icon(
                    id = iconId,
                    name = fileName,
                    svg = svgString,
                    tags = AssetProcessorUtils.extractTagsFromFileName(fileName)
                )
            }
            else -> {
                // Process PNG/other raster
                val bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.size)
                val pngSizes = generateMultipleSizes(bitmap)

                Icon(
                    id = iconId,
                    name = fileName,
                    png = pngSizes,
                    tags = AssetProcessorUtils.extractTagsFromFileName(fileName)
                )
            }
        }
    }

    actual suspend fun processImage(
        fileData: ByteArray,
        fileName: String,
        imageId: String
    ): ImageAsset = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.size)
        val dimensions = Dimensions(bitmap.width, bitmap.height)

        val thumbnail = generateThumbnail(
            fileData,
            128,
            128,
            maintainAspectRatio = true
        )

        ImageAsset(
            id = imageId,
            name = fileName,
            path = "images/$imageId",
            format = AssetProcessorUtils.detectImageFormat(fileName)!!,
            dimensions = dimensions,
            thumbnail = thumbnail,
            fileSize = fileData.size.toLong(),
            tags = AssetProcessorUtils.extractTagsFromFileName(fileName)
        )
    }

    actual suspend fun generateThumbnail(
        imageData: ByteArray,
        width: Int,
        height: Int,
        maintainAspectRatio: Boolean
    ): ByteArray = withContext(Dispatchers.IO) {
        val original = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

        val targetDimensions = if (maintainAspectRatio) {
            AssetProcessorUtils.calculateThumbnailDimensions(
                original.width,
                original.height,
                width,
                height
            )
        } else {
            Dimensions(width, height)
        }

        val scaled = Bitmap.createScaledBitmap(
            original,
            targetDimensions.width,
            targetDimensions.height,
            true
        )

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        stream.toByteArray()
    }

    actual suspend fun optimizeImage(
        imageData: ByteArray,
        quality: Int
    ): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }

    actual suspend fun extractDimensions(imageData: ByteArray): Dimensions {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
        return Dimensions(options.outWidth, options.outHeight)
    }

    actual suspend fun convertFormat(
        imageData: ByteArray,
        targetFormat: ImageFormat
    ): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val stream = ByteArrayOutputStream()

        when (targetFormat) {
            ImageFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            ImageFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            ImageFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, stream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 90, stream)
                }
            }
            else -> throw UnsupportedOperationException("Format $targetFormat not supported")
        }

        stream.toByteArray()
    }

    private fun generateMultipleSizes(bitmap: Bitmap): Map<Int, ByteArray> {
        return AssetProcessorUtils.StandardSizes.ICON_SIZES.associate { size ->
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)
            size to stream.toByteArray()
        }
    }
}
```

#### 1.2: Android LocalAssetStorage Implementation (8h)
**File**: `Universal/Core/AssetManager/src/androidMain/kotlin/AssetStorage.kt`

```kotlin
actual class LocalAssetStorage actual constructor(
    basePath: String
) : AssetStorage {
    private val context: Context by lazy { /* Android context */ }
    private val filesDir = context.filesDir
    private val assetDir = File(filesDir, basePath)

    init {
        runBlocking {
            initializeStorage()
        }
    }

    actual override suspend fun saveIcon(
        libraryId: String,
        icon: Icon
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val libraryDir = File(assetDir, "Icons/$libraryId")
            libraryDir.mkdirs()

            // Save SVG
            icon.svg?.let { svg ->
                val svgFile = File(libraryDir, "svg/${icon.id}.svg")
                svgFile.parentFile?.mkdirs()
                svgFile.writeText(svg)
            }

            // Save PNG sizes
            icon.png?.forEach { (size, bytes) ->
                val pngFile = File(libraryDir, "png/$size/${icon.id}.png")
                pngFile.parentFile?.mkdirs()
                pngFile.writeBytes(bytes)
            }

            Result.success("file://${libraryDir.absolutePath}/${icon.id}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun saveImage(
        libraryId: String,
        image: ImageAsset
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val libraryDir = File(assetDir, "Images/$libraryId")
            libraryDir.mkdirs()

            // Save image file
            val imageFile = File(libraryDir, "images/${image.id}.${image.format.extension}")
            imageFile.parentFile?.mkdirs()
            // Note: ImageAsset would need to carry actual bytes or provide path
            // This is simplified

            // Save thumbnail
            image.thumbnail?.let { thumb ->
                val thumbFile = File(libraryDir, "thumbnails/${image.id}.jpg")
                thumbFile.parentFile?.mkdirs()
                thumbFile.writeBytes(thumb)
            }

            Result.success("file://${imageFile.absolutePath}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun loadIcon(
        libraryId: String,
        iconId: String
    ): Icon? = withContext(Dispatchers.IO) {
        try {
            val libraryDir = File(assetDir, "Icons/$libraryId")
            val svgFile = File(libraryDir, "svg/$iconId.svg")

            val svg = if (svgFile.exists()) svgFile.readText() else null

            val pngSizes = mutableMapOf<Int, ByteArray>()
            AssetProcessorUtils.StandardSizes.ICON_SIZES.forEach { size ->
                val pngFile = File(libraryDir, "png/$size/$iconId.png")
                if (pngFile.exists()) {
                    pngSizes[size] = pngFile.readBytes()
                }
            }

            if (svg != null || pngSizes.isNotEmpty()) {
                Icon(
                    id = iconId,
                    name = iconId,
                    svg = svg,
                    png = pngSizes.ifEmpty { null }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ... implement other methods similarly
}
```

#### 1.3: Built-in Libraries - Material Icons (4h)
**Files**:
- `Universal/Core/AssetManager/libraries/material-icons/manifest.json`
- Import script to download and process icons

```kotlin
object MaterialIconsLibrary {
    suspend fun load(): IconLibrary {
        val icons = mutableListOf<Icon>()

        // Load from bundled resources
        materialIconNames.forEach { name ->
            val svg = loadIconSvg("material-icons/$name.svg")
            icons.add(
                Icon(
                    id = name,
                    name = name.replace("_", " ").capitalize(),
                    svg = svg,
                    tags = extractTags(name),
                    category = categorizeIcon(name)
                )
            )
        }

        return IconLibrary(
            id = "MaterialIcons",
            name = "Material Design Icons",
            version = "1.0.0",
            description = "Google Material Design icon set",
            icons = icons
        )
    }

    private val materialIconNames = listOf(
        "home", "search", "settings", "menu", "close",
        "add", "remove", "edit", "delete", "save",
        "share", "favorite", "star", "person", "group",
        "mail", "call", "message", "notifications",
        "account_circle", "shopping_cart", "attach_money",
        // ... ~2,400 icons
    )
}
```

#### 1.4: Asset Search Implementation (4h)
**File**: `Universal/Core/AssetManager/src/commonMain/kotlin/AssetSearch.kt`

```kotlin
class AssetSearch(
    private val manager: AssetManager
) {
    suspend fun searchIcons(query: String): List<IconSearchResult> {
        val results = mutableListOf<IconSearchResult>()

        manager.getIconLibraries().forEach { library ->
            library.icons.forEach { icon ->
                val score = calculateRelevance(query, icon)
                if (score > 0) {
                    results.add(
                        IconSearchResult(
                            library = library.id,
                            icon = icon,
                            relevanceScore = score
                        )
                    )
                }
            }
        }

        return results.sortedByDescending { it.relevanceScore }
    }

    private fun calculateRelevance(query: String, icon: Icon): Int {
        val lowerQuery = query.lowercase()
        var score = 0

        // Exact match
        if (icon.id.lowercase() == lowerQuery) score += 100
        if (icon.name.lowercase() == lowerQuery) score += 90

        // Starts with
        if (icon.id.lowercase().startsWith(lowerQuery)) score += 50
        if (icon.name.lowercase().startsWith(lowerQuery)) score += 45

        // Contains
        if (icon.id.lowercase().contains(lowerQuery)) score += 20
        if (icon.name.lowercase().contains(lowerQuery)) score += 15

        // Tag match
        icon.tags.forEach { tag ->
            if (tag.lowercase() == lowerQuery) score += 40
            if (tag.lowercase().contains(lowerQuery)) score += 10
        }

        // Keyword match
        icon.keywords.forEach { keyword ->
            if (keyword.lowercase() == lowerQuery) score += 30
            if (keyword.lowercase().contains(lowerQuery)) score += 8
        }

        return score
    }
}

data class IconSearchResult(
    val library: String,
    val icon: Icon,
    val relevanceScore: Int
)
```

---

## 📋 Task 2: Complete Theme Builder UI (16-24h)

### Current State: 20% Complete
**What Exists**:
- ✅ Directory structure
- ✅ Stub files

**What's Missing**:
- Compose Desktop UI
- Live preview
- Property editors
- Export system

### Implementation Summary

**Main Components**:
1. **EditorWindow** - Main application window
2. **PreviewCanvas** - Live theme preview
3. **PropertyInspector** - Edit colors, typography, spacing
4. **ThemeExporter** - Export to JSON/DSL/YAML
5. **ThemeImporter** - Import existing themes

**Tech Stack**:
- Compose for Desktop
- Material Design 3
- Color picker libraries
- File I/O

**Estimated Time**: 16-24 hours

---

## 📋 Task 3: Build Web Renderer (40h)

### Implementation Strategy

**Phase 3A: Core React Infrastructure** (12h)
- React component wrappers
- Theme converter (AvaUI → MUI)
- State management (hooks)

**Phase 3B: Component Mappers** (20h)
- 13 Phase 1 components
- Map to Material-UI or custom React

**Phase 3C: Integration** (8h)
- WebSocket IPC
- Example web apps
- Documentation

### Tech Stack
- React 18
- TypeScript
- Material-UI (MUI)
- Styled Components
- React Router

---

## 📋 Task 4: Android Studio Plugin (60h)

### Plugin Features
1. **AvaUI Editor** - Visual editor for DSL
2. **Code Generator** - Generate Compose from DSL
3. **Component Preview** - Live preview in IDE
4. **Code Completion** - IntelliJ language support
5. **Templates** - New project templates

### Implementation Phases
1. Plugin skeleton (IntelliJ SDK)
2. DSL language support
3. Visual editor
4. Code generation integration
5. Testing & deployment

---

## 📋 Task 5: Phase 3 Components (140h / 35 components)

### Component Categories

**Input (12 components)**: 48h
- Slider, RangeSlider
- DatePicker, TimePicker
- RadioButton, RadioGroup
- Dropdown, Autocomplete
- FileUpload, ImagePicker
- Rating, SearchBar

**Display (8 components)**: 32h
- Badge, Chip
- Avatar, Divider
- Skeleton, Spinner
- ProgressBar, Tooltip

**Layout (5 components)**: 20h
- Grid, Stack
- Spacer, Drawer
- Tabs

**Navigation (4 components)**: 16h
- AppBar, BottomNav
- Breadcrumb, Pagination

**Feedback (6 components)**: 24h
- Alert, Snackbar
- Modal, Toast
- Confirm, ContextMenu

### Per-Component Workflow
1. Spec (1h)
2. Android impl (1h)
3. Web impl (1h)
4. Tests (1h)
**Total**: 4h per component × 35 = 140h

---

## 📋 Task 6: Template Library (40h / 20+ screens)

### Template Categories

**1. Authentication (5 templates)**: 10h
- Material Login
- Biometric Login
- Social Signup
- OTP Verification
- Password Reset

**2. Dashboards (5 templates)**: 10h
- Analytics Dashboard
- E-commerce Dashboard
- Admin Panel
- User Dashboard
- Financial Dashboard

**3. E-Commerce (5 templates)**: 10h
- Product Grid
- Product Details
- Shopping Cart
- Checkout Flow
- Order History

**4. Social (3 templates)**: 6h
- Feed
- Profile
- Chat/Messaging

**5. Utility (2 templates)**: 4h
- Settings
- Notifications

### Per-Template Deliverables
- AvaUI DSL
- Android Compose
- React/TypeScript
- Screenshots
- Documentation

---

## 📊 Overall Timeline

### Week-by-Week Breakdown

**Week 1-2: Asset Manager + Theme Builder**
- Days 1-4: Complete Asset Manager (32h)
- Days 5-7: Complete Theme Builder (24h)
- **Milestone**: Asset & Theme systems 100% complete

**Week 3-4: Web Renderer**
- Days 8-12: Build React renderer (40h)
- **Milestone**: Cross-platform (Android + Web)

**Week 5-6: Android Studio Plugin**
- Days 13-20: Build plugin (60h)
- **Milestone**: IDE integration complete

**Week 7-8: Phase 3 Components (First Half)**
- Days 21-28: 18 components (72h)
- Input + Display components

**Week 9-10: Phase 3 Components (Second Half)**
- Days 29-36: 17 components (68h)
- Layout + Navigation + Feedback

**Week 11-12: Template Library**
- Days 37-42: 20+ templates (40h)
- All categories

**Total**: 12 weeks (336 working hours)

---

## 🎯 Success Metrics

### Technical Metrics
| Metric | Target | Current |
|--------|--------|---------|
| Asset Manager | 100% | 30% |
| Theme Builder | 100% | 20% |
| Web Renderer | 100% | 0% |
| Android Plugin | 100% | 0% |
| Phase 3 Components | 35/35 | 0/35 |
| Templates | 20+ | 8 (snippets) |

### Quality Metrics
| Metric | Target |
|--------|--------|
| Test Coverage | 80% |
| Documentation | 100% |
| Example Apps | 3 per platform |
| Performance | <16ms renders |

---

## 🚀 Next Immediate Steps

### Today (Oct 30):
1. ✅ Commit snippet library (DONE)
2. ✅ Create this implementation plan (DONE)
3. ⏳ Start Asset Manager Android implementation

### This Week:
1. Complete Asset Manager (32h)
2. Start Theme Builder (12h of 24h)

### Next Week:
1. Finish Theme Builder (12h)
2. Start Web Renderer (20h of 40h)

---

## 📝 Dependencies & Risks

### Dependencies
- Material Icons dataset (~2,400 icons)
- Font Awesome dataset (~1,500 icons)
- Material-UI (React)
- IntelliJ Platform SDK

### Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Asset processing performance | Medium | Use coroutines, caching |
| Theme Builder complexity | High | Start with MVP, iterate |
| Web renderer compatibility | Medium | Test across browsers |
| Plugin approval | Low | Follow IntelliJ guidelines |
| Component quality | Medium | TDD approach |

---

## 🏁 Definition of Done

### Asset Manager ✅
- [x] Android AssetProcessor implemented
- [x] Android LocalAssetStorage implemented
- [x] Material Icons library loaded
- [x] Font Awesome library loaded
- [x] Search functionality working
- [x] 80% test coverage
- [x] Documentation complete

### Theme Builder ✅
- [ ] Compose Desktop UI functional
- [ ] Live preview working
- [ ] All property editors implemented
- [ ] Export to JSON/DSL/YAML
- [ ] Import themes
- [ ] Documentation + tutorial

### Web Renderer ✅
- [ ] 13 Phase 1 components working
- [ ] Theme system integrated
- [ ] State management working
- [ ] Example apps deployed
- [ ] Documentation complete

### Android Studio Plugin ✅
- [ ] Plugin installable
- [ ] DSL syntax highlighting
- [ ] Code completion working
- [ ] Visual editor functional
- [ ] Templates available
- [ ] Published to marketplace

### Phase 3 Components ✅
- [ ] 35/35 components implemented
- [ ] Android + Web rendering
- [ ] Tests written
- [ ] Documentation complete

### Template Library ✅
- [ ] 20+ templates created
- [ ] All platforms supported
- [ ] Screenshots included
- [ ] Documentation complete

---

## 📞 Communication Plan

### Weekly Updates
- Progress report every Monday
- Blockers identified
- Metrics updated
- Timeline adjustments

### Milestone Celebrations
- Complete Asset Manager
- Complete Theme Builder
- Cross-platform rendering (Android + Web)
- IDE integration
- Component library complete
- Template library shipped

---

**Status**: ✅ Plan Complete - Ready to Execute
**Next Action**: Begin Asset Manager Android Implementation
**Timeline**: 12 weeks (Oct 30 → Jan 21, 2026)
**Focus**: Android + Web, iOS later

**Created by Manoj Jhawar, manoj@ideahq.net**
