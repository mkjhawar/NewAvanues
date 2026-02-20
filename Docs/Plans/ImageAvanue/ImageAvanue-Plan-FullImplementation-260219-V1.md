# ImageAvanue Full Implementation Plan

**Document:** ImageAvanue-Plan-FullImplementation-260219-V1.md
**Date:** 2026-02-19
**Branch:** Cockpit-Development
**Author:** Manoj Jhawar
**Mode:** .yolo .cot

---

## Summary

ImageAvanue goes from a 2-file shell (ImageItem + ImageViewer stubs) to a fully-featured KMP image gallery, viewer, and filter editor. The module targets a KMP Score of ~55% (shared models, interfaces, and filter DSL in commonMain; platform renderers in androidMain/desktopMain). It integrates into the Cockpit multi-window system via the existing `FrameContent.Image` slot and adds 18 voice commands across 5 locales.

---

## Problem Statement

### Current State (Pre-Implementation)

| File | Location | Status |
|------|----------|--------|
| `ImageItem.kt` | `commonMain` | Minimal — 4 fields only (id, path, name, date) |
| `ImageViewer.kt` | `androidMain` | Skeleton — hardcoded placeholder, no real image loading |

**Gaps:**
- No gallery screen (MediaStore query, thumbnail grid)
- No image loading library wired (Coil 3 not added)
- No filter engine (grayscale, sepia, blur, sharpen, brightness, contrast)
- No pan/zoom/rotate/flip interaction
- No voice commands (0 of 18 implemented)
- No Cockpit state wiring (FrameContent.Image slot is a stub)
- No EXIF metadata display
- No desktopMain implementation
- KMP Score ~10% (no shared business logic)

---

## Architecture Overview

```
commonMain
  model/
    ImageItem.kt         (enhanced — full metadata)
    ImageFilter.kt       (new — sealed class filter DSL)
    ImageEditorState.kt  (new — viewer + editor state)
  controller/
    IImageController.kt  (new — platform interface)

androidMain
  ui/
    ImageGalleryScreen.kt  (new — MediaStore grid)
    ImageViewer.kt         (rewrite — pan/zoom + Coil)
    ImageEditorToolbar.kt  (new — filter/rotate/flip chips)
  controller/
    AndroidImageController.kt  (new — ContentResolver impl)
    ImageFilterEngine.kt       (new — Bitmap pipeline)

desktopMain
  controller/
    DesktopImageController.kt  (new — filesystem impl)

VoiceOSCore/androidMain/handlers/
  ImageCommandHandler.kt   (new — routes IMAGE_* commands)
```

---

## Phase 1: commonMain Models

### 1.1 ImageItem.kt (ENHANCE)

**Path:** `Modules/ImageAvanue/src/commonMain/kotlin/com/augmentalis/imageavanue/model/ImageItem.kt`

**Current fields:** `id`, `path`, `name`, `date`

**Add:**

```kotlin
@Serializable
data class ImageItem(
    val id: Long,
    val uri: String,
    val path: String,
    val name: String,
    val mimeType: String,                          // "image/jpeg", "image/png", etc.
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,                           // epoch seconds
    val dateModified: Long,                        // epoch seconds
    val thumbnailUri: String? = null,
    val exifData: Map<String, String> = emptyMap() // EXIF key-value pairs
)
```

All fields nullable-safe for platforms that cannot supply full EXIF (desktop filesystem).

---

### 1.2 ImageFilter.kt (NEW)

**Path:** `Modules/ImageAvanue/src/commonMain/kotlin/com/augmentalis/imageavanue/model/ImageFilter.kt`

```kotlin
@Serializable
sealed class ImageFilter {
    @SerialName("grayscale")  object Grayscale : ImageFilter()
    @SerialName("sepia")      object Sepia     : ImageFilter()

    @SerialName("blur")
    @Serializable
    data class Blur(val radius: Float = 8f) : ImageFilter()      // 1..25

    @SerialName("sharpen")
    @Serializable
    data class Sharpen(val amount: Float = 1f) : ImageFilter()   // 0..3

    @SerialName("brightness")
    @Serializable
    data class Brightness(val value: Float = 0f) : ImageFilter() // -1..1

    @SerialName("contrast")
    @Serializable
    data class Contrast(val value: Float = 1f) : ImageFilter()   // 0..2
}
```

**Design note:** Sealed class lets the filter pipeline treat each variant polymorphically. `@SerialName` ensures stable JSON round-trips when stored in Cockpit frame state.

---

### 1.3 ImageEditorState.kt (NEW)

**Path:** `Modules/ImageAvanue/src/commonMain/kotlin/com/augmentalis/imageavanue/model/ImageEditorState.kt`

```kotlin
@Serializable
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class ImageEditorState(
    val currentImage: ImageItem? = null,
    val appliedFilters: List<ImageFilter> = emptyList(),
    val rotation: Int = 0,          // 0 / 90 / 180 / 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRect: CropRect? = null, // null = full image
    val zoom: Float = 1f,           // 1.0 = fit; up to 4.0
    val panX: Float = 0f,
    val panY: Float = 0f,
    val showExif: Boolean = false,
    val isEditorMode: Boolean = false
)
```

This is the single source of truth serialized into `FrameWindow.contentState` in Cockpit.

---

### 1.4 IImageController.kt (NEW)

**Path:** `Modules/ImageAvanue/src/commonMain/kotlin/com/augmentalis/imageavanue/controller/IImageController.kt`

```kotlin
interface IImageController {
    // Gallery
    fun loadGallery(): Flow<List<ImageItem>>
    suspend fun loadImage(uri: String): ImageItem?

    // Navigation
    fun nextImage()
    fun previousImage()

    // Transforms (pure state mutations — delegate rendering to platform)
    fun applyFilter(filter: ImageFilter)
    fun removeFilter(filter: ImageFilter)
    fun clearFilters()
    fun rotate(degrees: Int)          // adds to current rotation, clamps to 0/90/180/270
    fun flipHorizontal()
    fun flipVertical()
    fun crop(rect: CropRect)
    fun resetCrop()

    // Actions
    suspend fun shareImage(): Boolean
    suspend fun deleteImage(): Boolean

    // Metadata
    fun getExifData(): Map<String, String>

    // State
    val editorState: StateFlow<ImageEditorState>
}
```

---

## Phase 2: androidMain Implementation

### 2.1 AndroidImageController.kt (NEW)

**Path:** `Modules/ImageAvanue/src/androidMain/kotlin/com/augmentalis/imageavanue/controller/AndroidImageController.kt`

**Responsibilities:**
- `loadGallery()`: queries `MediaStore.Images.Media` with `ContentResolver`, returns images sorted by `DATE_MODIFIED DESC`
- `loadImage(uri)`: resolves single image metadata from `ContentResolver`
- `shareImage()`: fires `Intent.ACTION_SEND` with `image/*` MIME, returns success
- `deleteImage()`: uses `ContentResolver.delete()` with `RecoverableSecurityException` handling (API 29+)
- `getExifData()`: reads via `androidx.exifinterface.media.ExifInterface`
- State: `MutableStateFlow<ImageEditorState>` with atomic transform updates

**Columns queried from MediaStore:**
`_ID`, `_DATA`, `DISPLAY_NAME`, `MIME_TYPE`, `SIZE`, `WIDTH`, `HEIGHT`, `DATE_ADDED`, `DATE_MODIFIED`

---

### 2.2 ImageFilterEngine.kt (NEW)

**Path:** `Modules/ImageAvanue/src/androidMain/kotlin/com/augmentalis/imageavanue/controller/ImageFilterEngine.kt`

**Filter pipeline** (applies filters in `appliedFilters` list order):

| Filter | Implementation |
|--------|----------------|
| `Grayscale` | `ColorMatrix` with saturation = 0 via `ColorMatrixColorFilter` |
| `Sepia` | `ColorMatrix` with warm-tint coefficients (R:0.393+0.769+0.189 row) |
| `Blur(r)` | `RenderEffect.createBlurEffect(r, r, BOUNDED)` on API 31+; StackBlur algorithm fallback for API < 31 |
| `Sharpen(a)` | 3×3 convolution kernel: center = 1+(8×a), neighbours = -a |
| `Brightness(v)` | `ColorMatrix.setScale(1+v, 1+v, 1+v, 1)` |
| `Contrast(v)` | Scale + translate matrix: `(v, 0, 0, 0, 128*(1-v))` per channel |

Returns `ImageBitmap` (Compose) for display. Runs on `Dispatchers.Default`.

---

### 2.3 ImageGalleryScreen.kt (NEW)

**Path:** `Modules/ImageAvanue/src/androidMain/kotlin/com/augmentalis/imageavanue/ui/ImageGalleryScreen.kt`

**Layout:**
- `Scaffold` with `TopAppBar` — title "Photos", search icon, select-all button
- `LazyVerticalGrid(columns = GridCells.Fixed(3))` — 2dp spacing
- Thumbnails: `AsyncImage` (Coil) with `CrossFade` and placeholder shimmer
- Date section headers: `Today`, `Yesterday`, `This Week`, `Earlier` (sticky headers via `stickyHeader`)
- Selection mode: long-press activates, checkboxes overlay thumbnails, action bar appears at bottom
- Action bar: Share selected, Delete selected, Cancel

**AvanueUI:**
- Background: `verticalGradient(AvanueTheme.colors.background, AvanueTheme.colors.surface.copy(0.6f))`
- TopAppBar: `containerColor = Color.Transparent`

**AVID semantics (MANDATORY):**
```kotlin
// Each thumbnail:
Modifier.semantics {
    contentDescription = "Voice: open image ${item.name}"
    role = Role.Image
}
// Select-all button:
Modifier.semantics { contentDescription = "Voice: click select all" }
// Share button:
Modifier.semantics { contentDescription = "Voice: click share selected" }
// Delete button:
Modifier.semantics { contentDescription = "Voice: click delete selected" }
```

---

### 2.4 ImageViewer.kt (REWRITE)

**Path:** `Modules/ImageAvanue/src/androidMain/kotlin/com/augmentalis/imageavanue/ui/ImageViewer.kt`

**Layout:**
- Full-screen `Box` with transformable modifier:
  ```kotlin
  val state = rememberTransformableState { zoomChange, panChange, _ ->
      controller.applyZoomPan(zoomChange, panChange)
  }
  Modifier.transformable(state)
  ```
- `AsyncImage` (Coil) filling the box, with `graphicsLayer` applying rotation, scale, translation from `editorState`
- **Filter overlay:** `ImageFilterEngine.applyFilters(bitmap, editorState.appliedFilters)` on snap, `DrawScope` preview for live filter preview
- **EXIF overlay:** animated slide-up panel, shows key EXIF fields (make, model, date, GPS, focal length, aperture, ISO)
- **Nav arrows:** `IconButton` left/right at vertical centre (hidden when at first/last image)
- **Control bar:** shown on tap, auto-hides after 3s:
  - Info (EXIF toggle), Edit (toggle editor mode), Share, Delete

**AVID semantics:**
```kotlin
// Left/right arrows:
Modifier.semantics { contentDescription = "Voice: click previous image" }
Modifier.semantics { contentDescription = "Voice: click next image" }
// EXIF toggle:
Modifier.semantics { contentDescription = "Voice: click show info" }
// Share:
Modifier.semantics { contentDescription = "Voice: click share image" }
```

---

### 2.5 ImageEditorToolbar.kt (NEW)

**Path:** `Modules/ImageAvanue/src/androidMain/kotlin/com/augmentalis/imageavanue/ui/ImageEditorToolbar.kt`

**Layout:** Bottom sheet composable with two rows:

**Row 1 — Filters (scrollable `LazyRow` of `FilterChip`):**
Original | Grayscale | Sepia | Blur | Sharpen | Brighter | Darker | More Contrast | Less Contrast

**Row 2 — Transform buttons:**
Rotate Left (90°) | Rotate Right (90°) | Flip Horizontal | Flip Vertical | Crop | Reset All

**AVID on every chip and button.** Active filter chips show `AvanueTheme.colors.primary` tint.

---

### 2.6 ImageCommandHandler.kt (NEW)

**Path:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/handlers/ImageCommandHandler.kt`

```kotlin
class ImageCommandHandler(
    private val context: Context,
    private val controller: IImageController
) : BaseHandler() {

    override fun canHandle(category: ActionCategory) = category == ActionCategory.IMAGE

    override fun handle(command: StaticCommand): HandlerResult {
        return when (command.actionType) {
            CommandActionType.IMAGE_GALLERY          -> navigateToGallery()
            CommandActionType.IMAGE_OPEN             -> openCurrentImage()
            CommandActionType.IMAGE_NEXT             -> { controller.nextImage(); HandlerResult.Success }
            CommandActionType.IMAGE_PREVIOUS         -> { controller.previousImage(); HandlerResult.Success }
            CommandActionType.IMAGE_ZOOM_IN          -> { controller.applyZoom(1.5f); HandlerResult.Success }
            CommandActionType.IMAGE_ZOOM_OUT         -> { controller.applyZoom(0.67f); HandlerResult.Success }
            CommandActionType.IMAGE_ZOOM_RESET       -> { controller.applyZoom(1f); HandlerResult.Success }
            CommandActionType.IMAGE_ROTATE_LEFT      -> { controller.rotate(-90); HandlerResult.Success }
            CommandActionType.IMAGE_ROTATE_RIGHT     -> { controller.rotate(90); HandlerResult.Success }
            CommandActionType.IMAGE_FLIP_HORIZONTAL  -> { controller.flipHorizontal(); HandlerResult.Success }
            CommandActionType.IMAGE_FLIP_VERTICAL    -> { controller.flipVertical(); HandlerResult.Success }
            CommandActionType.IMAGE_FILTER_GRAYSCALE -> { controller.applyFilter(ImageFilter.Grayscale); HandlerResult.Success }
            CommandActionType.IMAGE_FILTER_SEPIA     -> { controller.applyFilter(ImageFilter.Sepia); HandlerResult.Success }
            CommandActionType.IMAGE_FILTER_BLUR      -> { controller.applyFilter(ImageFilter.Blur()); HandlerResult.Success }
            CommandActionType.IMAGE_FILTER_SHARPEN   -> { controller.applyFilter(ImageFilter.Sharpen()); HandlerResult.Success }
            CommandActionType.IMAGE_FILTER_CLEAR     -> { controller.clearFilters(); HandlerResult.Success }
            CommandActionType.IMAGE_SHARE            -> shareImage()
            CommandActionType.IMAGE_DELETE           -> deleteImage()
            else -> HandlerResult.NotHandled
        }
    }
}
```

**Registration:** Add to `AndroidHandlerFactory.createHandlers()`.

---

## Phase 3: desktopMain

### 3.1 DesktopImageController.kt (NEW)

**Path:** `Modules/ImageAvanue/src/desktopMain/kotlin/com/augmentalis/imageavanue/controller/DesktopImageController.kt`

**Responsibilities:**
- `loadGallery()`: walks `~/Pictures` (and user-selected folders) via `java.nio.file.Files.walk`, filters by MIME type extension
- `loadImage(uri)`: reads file path, extracts dimensions via `ImageIO.read()`, partial EXIF via Apache Commons Imaging or fallback metadata
- `shareImage()`: `java.awt.Desktop.getDesktop().open(file)` — opens in system viewer
- `deleteImage()`: `Files.delete(path)` with confirmation via state flag
- Filter engine: delegates to shared `AwtImageFilterEngine` (same convolution logic, AWT `BufferedImage`)

**Note:** desktopMain does not have MediaStore. Gallery is filesystem-only with path-based discovery.

---

## Phase 4: Cockpit Integration

The `FrameContent.Image` slot already exists in Cockpit. These wiring points need updating from stub to real state.

### 4.1 ContentRenderer.kt (UPDATE)

**Path:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/ui/ContentRenderer.kt`

Replace stub `FrameContent.Image` branch:

```kotlin
is FrameContent.Image -> {
    val editorState = rememberSaveable { mutableStateOf(
        frame.contentState?.let { Json.decodeFromString<ImageEditorState>(it) }
            ?: ImageEditorState()
    )}
    ImageViewer(
        editorState = editorState.value,
        controller = LocalImageController.current,
        onStateChange = { editorState.value = it }
    )
}
```

### 4.2 ContentAccent.kt (UPDATE)

```kotlin
FrameContent.Image -> AvanueTheme.colors.secondary
```

### 4.3 AndroidCockpitRepository.kt (VERIFY)

Confirm the `FrameContent.Image` deserialize fallback is present. If the `when` branch is missing or falls through to `Unknown`, add:

```kotlin
"Image" -> FrameContent.Image(payload["uri"] as? String ?: "")
```

---

## Phase 5: VOS Commands

### 5.1 New CommandActionType Values

Add to `CommandActionType.kt` under `// IMAGE` group:

```kotlin
IMAGE_GALLERY,
IMAGE_OPEN,
IMAGE_NEXT,
IMAGE_PREVIOUS,
IMAGE_ZOOM_IN,
IMAGE_ZOOM_OUT,
IMAGE_ZOOM_RESET,
IMAGE_ROTATE_LEFT,
IMAGE_ROTATE_RIGHT,
IMAGE_FLIP_HORIZONTAL,
IMAGE_FLIP_VERTICAL,
IMAGE_FILTER_GRAYSCALE,
IMAGE_FILTER_SEPIA,
IMAGE_FILTER_BLUR,
IMAGE_FILTER_SHARPEN,
IMAGE_FILTER_CLEAR,
IMAGE_SHARE,
IMAGE_DELETE,
```

### 5.2 New ActionCategory Value

Add to `ActionCategory.kt`:

```kotlin
IMAGE(priority = 16)
```

(After `CAMERA = 14`, `ANNOTATION = 15`, before `CUSTOM = 16` — shift CUSTOM to 17.)

### 5.3 VOS Command Definitions (en-US.app.vos)

```
# IMAGE COMMANDS
image gallery | open gallery | show photos              | IMAGE_GALLERY          | IMAGE | 1.0
open image | view image | open photo                    | IMAGE_OPEN             | IMAGE | 1.0
next image | next photo | show next                     | IMAGE_NEXT             | IMAGE | 1.0
previous image | previous photo | go back               | IMAGE_PREVIOUS         | IMAGE | 1.0
zoom in | zoom image in | magnify                        | IMAGE_ZOOM_IN          | IMAGE | 1.0
zoom out | zoom image out | shrink                       | IMAGE_ZOOM_OUT         | IMAGE | 1.0
reset zoom | fit image | zoom reset                     | IMAGE_ZOOM_RESET       | IMAGE | 1.0
rotate left | rotate image left | turn left              | IMAGE_ROTATE_LEFT      | IMAGE | 1.0
rotate right | rotate image right | turn right           | IMAGE_ROTATE_RIGHT     | IMAGE | 1.0
flip horizontal | mirror image | flip left right         | IMAGE_FLIP_HORIZONTAL  | IMAGE | 1.0
flip vertical | flip upside down | flip top bottom       | IMAGE_FLIP_VERTICAL    | IMAGE | 1.0
grayscale | black and white | remove color               | IMAGE_FILTER_GRAYSCALE | IMAGE | 1.0
sepia | sepia tone | vintage filter                      | IMAGE_FILTER_SEPIA     | IMAGE | 1.0
blur | blur image | soften                               | IMAGE_FILTER_BLUR      | IMAGE | 1.0
sharpen | sharpen image | enhance sharpness              | IMAGE_FILTER_SHARPEN   | IMAGE | 1.0
clear filters | remove filters | original image          | IMAGE_FILTER_CLEAR     | IMAGE | 1.0
share image | share photo | send photo                   | IMAGE_SHARE            | IMAGE | 1.0
delete image | delete photo | remove photo               | IMAGE_DELETE           | IMAGE | 1.0
```

### 5.4 Locale Files

Each of the following locale VOS files gets the same 18 commands translated:

| Locale | File |
|--------|------|
| es-ES | `assets/vos/es-ES.app.vos` |
| fr-FR | `assets/vos/fr-FR.app.vos` |
| de-DE | `assets/vos/de-DE.app.vos` |
| hi-IN | `assets/vos/hi-IN.app.vos` |

Translation strings are derived from existing VOS locale patterns. Pipe-delimited synonyms per locale.

---

## Phase 6: Dependency Updates

### 6.1 Modules/ImageAvanue/build.gradle.kts

```kotlin
commonMain.dependencies {
    // existing deps...
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
}

androidMain.dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
```

### 6.2 Version Catalog (libs.versions.toml)

```toml
[versions]
coil3 = "3.0.4"

[libraries]
coil3-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil3-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }
exifinterface = { module = "androidx.exifinterface:exifinterface", version = "1.3.7" }
```

---

## File Inventory

### Files Created / Enhanced

| File | Source Set | Action | Notes |
|------|-----------|--------|-------|
| `model/ImageItem.kt` | commonMain | ENHANCE | +8 fields, full metadata |
| `model/ImageFilter.kt` | commonMain | NEW | Sealed class, 6 filters |
| `model/ImageEditorState.kt` | commonMain | NEW | Viewer + editor state |
| `controller/IImageController.kt` | commonMain | NEW | Platform interface |
| `ui/ImageGalleryScreen.kt` | androidMain | NEW | MediaStore grid, selection |
| `ui/ImageViewer.kt` | androidMain | REWRITE | Coil, pan/zoom, filters |
| `ui/ImageEditorToolbar.kt` | androidMain | NEW | Filter chips + transforms |
| `controller/AndroidImageController.kt` | androidMain | NEW | ContentResolver impl |
| `controller/ImageFilterEngine.kt` | androidMain | NEW | Bitmap filter pipeline |
| `controller/DesktopImageController.kt` | desktopMain | NEW | Filesystem impl |
| `handlers/ImageCommandHandler.kt` | VoiceOSCore androidMain | NEW | Routes 18 IMAGE_* commands |
| `ContentRenderer.kt` | Cockpit androidMain | UPDATE | Real state wiring for Image |
| `ContentAccent.kt` | Cockpit commonMain | UPDATE | Image accent color |
| `CommandActionType.kt` | VoiceOSCore commonMain | UPDATE | +18 IMAGE_* values |
| `ActionCategory.kt` | VoiceOSCore commonMain | UPDATE | +IMAGE(priority=16) |
| `en-US.app.vos` | assets | UPDATE | +18 image commands |
| `es-ES.app.vos` | assets | UPDATE | +18 translated |
| `fr-FR.app.vos` | assets | UPDATE | +18 translated |
| `de-DE.app.vos` | assets | UPDATE | +18 translated |
| `hi-IN.app.vos` | assets | UPDATE | +18 translated |
| `build.gradle.kts` | ImageAvanue | UPDATE | Coil 3 + ExifInterface |
| `libs.versions.toml` | root | UPDATE | coil3, exifinterface versions |

**Total: 4 commonMain + 5 androidMain + 1 desktopMain + 1 VoiceOSCore handler + 5 Cockpit/VOS updates + 2 build files = 18 files touched**

### Files NOT Touched (Protected)

Per `CLAUDE.md` MANDATORY RULE #1 (Scraping System Protection):
- `VoiceOSAccessibilityService.kt` — not modified
- `AndroidScreenExtractor.kt` — not modified
- All `ScrapedApp.sq`, `ScrapedElement.sq`, etc. — not modified

---

## Testing Checklist

### Unit Tests

| Component | Test | Method |
|-----------|------|--------|
| `ImageFilterEngine` | Grayscale output has R=G=B per pixel | Bitmap pixel sampling |
| `ImageFilterEngine` | Sepia warm tint within expected RGB range | ColorMatrix math |
| `ImageFilterEngine` | Blur radius 0 = no change | Pixel diff |
| `ImageEditorState` | Rotation clamps to 0/90/180/270 | Unit test |
| `ImageEditorState` | JSON round-trip (serialize/deserialize) | Kotlin serialization |
| `ImageFilter` | Sealed class `@SerialName` stable across versions | JSON output check |
| `AndroidImageController` | `nextImage()` wraps at end of list | State assertion |
| `AndroidImageController` | `rotate(90)` accumulates correctly | State assertion |

### Integration Tests

| Scenario | Expected |
|----------|----------|
| Gallery loads from MediaStore with test content provider | Returns list of `ImageItem` with all fields |
| Apply Grayscale then Sepia then Clear | State has empty filter list, original displayed |
| Voice command IMAGE_ZOOM_IN | Zoom increases from 1.0 to 1.5 |
| Voice command IMAGE_FILTER_GRAYSCALE | Grayscale added to appliedFilters |
| Cockpit FrameContent.Image serialization | `ImageEditorState` round-trips via JSON |

### Manual Verification

- [ ] Gallery grid shows thumbnails with Coil async loading (shimmer while loading)
- [ ] Date section headers (Today / Yesterday / This Week / Earlier) appear correctly
- [ ] Pan/zoom works with pinch-to-zoom and two-finger pan
- [ ] Grayscale filter visually desaturates the image
- [ ] Sepia filter shows warm brown tint
- [ ] Blur filter produces visibly soft image
- [ ] Rotate left button rotates image 90 degrees counterclockwise
- [ ] Flip horizontal mirrors the image
- [ ] EXIF overlay shows make/model/date/ISO on tap
- [ ] Share intent opens system share sheet
- [ ] Voice command "show photos" opens gallery
- [ ] Voice command "grayscale" applies filter
- [ ] Cockpit Image frame shows ImageViewer with correct state persistence

---

## Implementation Order

Execute phases in this order to minimize integration conflicts:

1. **Phase 1** — commonMain models (zero platform dependencies, establishes contracts)
2. **Phase 5 (partial)** — Add `CommandActionType` IMAGE_* values and `ActionCategory.IMAGE` (needed before handler compiles)
3. **Phase 2.1 + 2.2** — `AndroidImageController` + `ImageFilterEngine` (business logic, no UI dependencies)
4. **Phase 2.4** — `ImageViewer.kt` rewrite (uses controller + Coil)
5. **Phase 2.3** — `ImageGalleryScreen.kt` (uses Coil thumbnails + controller)
6. **Phase 2.5** — `ImageEditorToolbar.kt` (pure UI, uses controller API)
7. **Phase 2.6** — `ImageCommandHandler.kt` (uses controller interface)
8. **Phase 3** — `DesktopImageController.kt`
9. **Phase 4** — Cockpit wiring (`ContentRenderer`, `ContentAccent`, `AndroidCockpitRepository`)
10. **Phase 5 (remaining)** — VOS command strings in all 5 locale files
11. **Phase 6** — Dependency updates (build files last to avoid Gradle sync mid-implementation)

---

## KMP Score Projection

| Source Set | Files | Shared Logic |
|-----------|-------|-------------|
| commonMain | 4 | Models, interface, filter DSL |
| androidMain | 5 | MediaStore, Bitmap pipeline, Compose UI |
| desktopMain | 1 | Filesystem, AWT |
| iosMain | 0 (future) | PHAsset, CoreImage |

**Current KMP Score: ~55%** (4 of 10 implementation files in commonMain + interface contract shared across platforms)

---

## Related Documentation

- `Docs/MasterDocs/NewAvanues-Developer-Manual/Developer-Manual-Chapter98-PhotoAvanueKMPCamera.md` — Camera module (sister module)
- `Docs/MasterDocs/NewAvanues-Developer-Manual/Developer-Manual-Chapter93-VoiceCommandPipelineLocalization.md` — VOS command format
- `Docs/MasterDocs/Cockpit/Developer-Manual-Chapter97-CockpitSpatialVoiceMultiWindow.md` — Cockpit integration pattern
- `docs/plans/Modules-Spec-SixModuleImplementation-260219-V1.md` — Unified spec (all 6 modules)
- `Docs/handover/handover-260219-0100.md` — Session handover with research findings

---

**Author:** Manoj Jhawar | NewAvanues | 2026-02-19
