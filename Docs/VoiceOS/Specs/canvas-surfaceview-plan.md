# Canvas/SurfaceView Accessibility Implementation Plan

**Version:** 1.0
**Date:** 2025-11-30
**Spec:** canvas-surfaceview-accessibility-spec.md
**Status:** Ready for Implementation

---

## Executive Summary

This plan details the implementation of Canvas/SurfaceView accessibility support for VoiceOS, enabling voice control of content invisible to standard Android accessibility APIs. The implementation follows a 6-phase approach over 9 weeks.

---

## Phase 1: Canvas Detection (Week 1)

### Objective
Detect Canvas/SurfaceView nodes in the accessibility tree and track apps that use them.

### Files to Create

| File | Location | Description |
|------|----------|-------------|
| `CanvasDetector.kt` | `voiceoscore/canvas/detection/` | Core detection logic |
| `CanvasDetectedApp.sq` | `database/` | SQLDelight schema |
| `CanvasAnalytics.kt` | `voiceoscore/canvas/analytics/` | Metrics collection |

### Files to Modify

| File | Changes |
|------|---------|
| `AccessibilityScrapingIntegration.kt` | Add canvas detection hook in `scrapeCurrentWindowImpl()` |

### Key Implementation

```kotlin
class CanvasDetector(private val context: Context) {

    companion object {
        private val CANVAS_CLASS_NAMES = setOf(
            "android.view.SurfaceView",
            "android.view.TextureView",
            "com.unity3d.player.UnityPlayer",
            "com.google.android.exoplayer2.ui.PlayerView",
            "androidx.media3.ui.PlayerView"
        )
    }

    fun isCanvasOrSurfaceView(node: AccessibilityNodeInfo): Boolean
    fun analyzeCanvasCharacteristics(node: AccessibilityNodeInfo): CanvasAnalysis
    suspend fun recordDetection(packageName: String, className: String, bounds: Rect)
}

data class CanvasAnalysis(
    val isCanvas: Boolean,
    val canvasType: CanvasType,
    val confidence: Float,
    val engineType: GameEngineType?
)

enum class CanvasType { SURFACE_VIEW, TEXTURE_VIEW, GAME_ENGINE, VIDEO_PLAYER, CUSTOM_DRAWING, UNKNOWN }
enum class GameEngineType { UNITY, UNREAL, COCOS2D, LIBGDX, UNKNOWN }
```

### Database Schema

```sql
CREATE TABLE canvas_detected_app (
    package_name TEXT NOT NULL PRIMARY KEY,
    first_detected_at INTEGER NOT NULL,
    last_detected_at INTEGER NOT NULL,
    detection_count INTEGER NOT NULL DEFAULT 1,
    canvas_type TEXT NOT NULL,
    has_plugin INTEGER NOT NULL DEFAULT 0,
    ocr_enabled INTEGER NOT NULL DEFAULT 0,
    engine_type TEXT
);
```

### Success Criteria
- [ ] Canvas/SurfaceView detection accuracy > 95%
- [ ] Detection adds < 5ms to scraping time
- [ ] All detected apps persisted to database

---

## Phase 2: OCR Integration (Weeks 2-3)

### Objective
Implement OCR text extraction using ML Kit (bundled) and Tesseract.

### Files to Create

| File | Description |
|------|-------------|
| `OCREngine.kt` | Engine interface |
| `MLKitOCREngine.kt` | ML Kit implementation |
| `TesseractOCREngine.kt` | Tesseract implementation |
| `OCREngineFactory.kt` | Factory with build variant support |
| `ScreenshotCaptureUtil.kt` | Region screenshot utility |

### Key Implementation

```kotlin
interface OCREngine {
    val engineName: String
    val isAvailable: Boolean
    suspend fun extractText(bitmap: Bitmap): OCRResult
    suspend fun initialize(): Result<Unit>
    fun release()
}

data class OCRResult(
    val success: Boolean,
    val elements: List<TextElement>,
    val processingTimeMs: Long,
    val engineUsed: String,
    val confidence: Float
)

data class TextElement(
    val text: String,
    val bounds: Rect,
    val confidence: Float,
    val language: String?
)
```

### Build Variants

```kotlin
android {
    flavorDimensions += "ocr"
    productFlavors {
        create("playStore") { buildConfigField("String", "OCR_ENGINE", "\"mlkit_dynamic\"") }
        create("aosp") { buildConfigField("String", "OCR_ENGINE", "\"tesseract\"") }
        create("full") { buildConfigField("String", "OCR_ENGINE", "\"mlkit_bundled,tesseract\"") }
    }
}
```

### Success Criteria
- [ ] OCR extracts text from 80%+ of readable Canvas content
- [ ] Processing time < 500ms on mid-range devices
- [ ] Works offline (Tesseract fallback)

---

## Phase 3: Caching (Week 4)

### Objective
Implement encrypted caching of OCR results.

### Files to Create

| File | Description |
|------|-------------|
| `ScreenHashCalculator.kt` | Perceptual screen hash |
| `OCRCacheManager.kt` | Cache with TTL and encryption |
| `OcrCache.sq` | SQLDelight cache schema |

### Key Implementation

```kotlin
class OCRCacheManager(
    private val context: Context,
    private val database: VoiceOSAppDatabase,
    private val encryptionManager: DataEncryptionManager,
    private val ttlMs: Long = 30 * 60 * 1000L // 30 minutes
) {
    suspend fun getCached(packageName: String, screenBitmap: Bitmap): OCRResult?
    suspend fun cache(packageName: String, screenBitmap: Bitmap, result: OCRResult)
    suspend fun clearExpired()
}
```

### Success Criteria
- [ ] Cache hit rate > 80% for repeated screens
- [ ] Cache lookup < 50ms
- [ ] All cached data encrypted with AES-256-GCM

---

## Phase 4: Plugin Architecture (Weeks 5-6)

### Objective
Create extensible plugin system for app-specific Canvas accessibility.

### Files to Create

| File | Description |
|------|-------------|
| `CanvasAccessibilityPlugin.kt` | Plugin interface |
| `CanvasPluginManager.kt` | Plugin discovery and lifecycle |
| `PluginSecurityVerifier.kt` | Signature verification |
| `CanvasPlugin.sq` | Plugin registry schema |

### Key Implementation

```kotlin
interface CanvasAccessibilityPlugin {
    val pluginId: String
    val version: String
    val supportedPackages: List<String>

    fun canHandle(packageName: String, className: String): Boolean
    suspend fun getElements(context: Context, rootNode: AccessibilityNodeInfo, screenshot: Bitmap?): List<SyntheticElement>
    suspend fun performAction(element: SyntheticElement, action: AccessibilityAction): Boolean
    fun onLoad()
    fun onUnload()
}

data class SyntheticElement(
    val id: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val className: String,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val confidence: Float,
    val source: ElementSource,
    val pluginId: String
)

enum class ElementSource { OCR, PLUGIN, HEURISTIC }
enum class AccessibilityAction { CLICK, LONG_CLICK, FOCUS, SCROLL_FORWARD, SCROLL_BACKWARD, SET_TEXT, CUSTOM }
```

### Success Criteria
- [ ] Plugin architecture supports 3+ internal plugins
- [ ] External plugins discovered via ContentProvider
- [ ] Signature verification rejects unsigned plugins

---

## Phase 5: Internal Plugins (Weeks 7-8)

### Objective
Implement Unity and ExoPlayer internal plugins.

### Files to Create

| File | Description |
|------|-------------|
| `UnityCanvasPlugin.kt` | Unity game plugin |
| `UnityReflectionHelper.kt` | Unity reflection utilities |
| `ExoPlayerCanvasPlugin.kt` | Video player plugin |
| `ExoPlayerControlsMapper.kt` | Map ExoPlayer controls |

### Success Criteria
- [ ] Unity plugin detects and handles Unity games
- [ ] ExoPlayer plugin maps standard video controls
- [ ] Graceful degradation when game doesn't support accessibility

---

## Phase 6: Documentation (Week 9)

### Objective
Create SDK documentation and sample app.

### Deliverables

| File | Description |
|------|-------------|
| `docs/canvas-accessibility/SDK-Guide.md` | Integration guide |
| `docs/canvas-accessibility/API-Reference.md` | API documentation |
| `samples/canvas-plugin/` | Sample plugin project |

### Success Criteria
- [ ] SDK guide covers all integration scenarios
- [ ] Sample app demonstrates complete workflow
- [ ] Partner verification process documented

---

## Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| 1: Canvas Detection | Week 1 | CanvasDetector, database tracking |
| 2: OCR Integration | Weeks 2-3 | MLKit/Tesseract engines, build variants |
| 3: Caching | Week 4 | Encrypted cache, screen hashing |
| 4: Plugin Architecture | Weeks 5-6 | Plugin interface, manager, security |
| 5: Internal Plugins | Weeks 7-8 | Unity plugin, ExoPlayer plugin |
| 6: Documentation | Week 9 | SDK guide, sample app |

---

## Critical Integration Points

1. **AccessibilityScrapingIntegration.kt** - Hook into `scrapeCurrentWindowImpl()` for canvas detection
2. **DataEncryptionManager.kt** - Reuse existing AES-256-GCM for cache encryption
3. **AccessibilityPluginInterface.kt** - Reference for plugin interface pattern
4. **VoiceOSCore build.gradle.kts** - Add product flavors for OCR engines

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| OCR accuracy poor | Train custom Tesseract model, allow user feedback |
| Battery drain | On-demand only, aggressive caching, user opt-in |
| Privacy concerns | Local-only processing, encryption, clear consent |
| Plugin security | Signature verification, sandboxing, partner program |
| APK size | Build variants, dynamic delivery for OCR models |

---

**Next Steps:** Begin Phase 1 implementation with CanvasDetector class
