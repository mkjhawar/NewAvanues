# Canvas/SurfaceView Accessibility Support Specification

**Version:** 1.0
**Date:** 2025-11-30
**Author:** CCA
**Status:** Draft

---

## Executive Summary

Enable VoiceOS LearnApp to extract actionable UI information from Canvas and SurfaceView content that is invisible to standard Android accessibility APIs. Implements a layered approach: (1) enhance existing JIT/LearnApp scraping, (2) OCR fallback for text extraction, (3) plugin architecture for game-specific integrations.

---

## Problem Statement

### Current State
- LearnApp can only scrape View-based UI via AccessibilityNodeInfo
- Canvas/SurfaceView content appears as single empty node with no children
- Games, video players, PDF viewers, custom charts are invisible to VoiceOS

### Desired State
- Detect Canvas/SurfaceView elements and attempt alternative extraction
- Use OCR to extract visible text from screenshots
- Provide plugin SDK for game developers to expose their UI
- Maintain AOSP + Play Store compatibility with offline capability

### Impact
- **Affected Apps:** ~30% of apps use Canvas/SurfaceView for some UI
- **User Impact:** Cannot voice-control games, video players, custom views

---

## Functional Requirements

### Phase 1: Enhanced Detection (Priority: HIGH)

| ID | Requirement | Acceptance Criteria |
|----|-------------|---------------------|
| FR-001 | Detect Canvas/SurfaceView nodes in accessibility tree | isCanvasOrSurfaceView() returns true for known class names |
| FR-002 | Log warning when Canvas/SurfaceView detected | Log includes package name, class name, bounds |
| FR-003 | Track apps with Canvas content in database | New table: canvas_detected_apps |
| FR-004 | Expose metrics for Canvas detection rate | Analytics shows % of screens with Canvas |

### Phase 2: OCR Fallback (Priority: HIGH)

| ID | Requirement | Acceptance Criteria |
|----|-------------|---------------------|
| FR-010 | Capture screenshot of Canvas region | Bitmap captured with correct bounds |
| FR-011 | Support ML Kit bundled model (AOSP compatible) | Works on devices without Google Play Services |
| FR-012 | Support Tesseract as fallback engine | Works offline with bundled training data |
| FR-013 | Extract text with bounding boxes | Returns list of (text, Rect) pairs |
| FR-014 | Convert OCR results to virtual AccessibilityNodeInfo | Synthetic nodes with text and bounds |
| FR-015 | Cache OCR results by screen hash | Avoid re-processing unchanged screens |
| FR-016 | User setting to enable/disable OCR | Default: disabled (privacy/battery) |

### Phase 3: Plugin Architecture (Priority: MEDIUM)

| ID | Requirement | Acceptance Criteria |
|----|-------------|---------------------|
| FR-020 | Define CanvasAccessibilityPlugin interface | Interface with getElements(), performAction() |
| FR-021 | Plugin discovery via ContentProvider | Plugins register via manifest |
| FR-022 | Plugin verification and sandboxing | Signature verification, permission checks |
| FR-023 | Internal plugin: Unity games | Detect Unity, query via reflection/IPC |
| FR-024 | Internal plugin: ExoPlayer | Extract video controls |
| FR-025 | Publish SDK for third-party developers | Maven/GitHub package |

### Phase 4: AccessibilityDelegate SDK (Priority: LOW - Future)

| ID | Requirement | Acceptance Criteria |
|----|-------------|---------------------|
| FR-030 | Create VoiceOS AccessibilityDelegate base class | Extends View.AccessibilityDelegate |
| FR-031 | Documentation for app developers | Integration guide published |
| FR-032 | Sample integration app | Demo app showing usage |

---

## Non-Functional Requirements

| ID | Category | Requirement | Target |
|----|----------|-------------|--------|
| NFR-001 | Performance | OCR processing time | < 500ms per screen |
| NFR-002 | Performance | Memory usage for OCR | < 50MB additional |
| NFR-003 | Battery | OCR should not run continuously | Only on-demand or screen change |
| NFR-004 | Privacy | Screenshots must not leave device | Local processing only |
| NFR-005 | Privacy | OCR cache encrypted | AES-256 encryption |
| NFR-006 | Compatibility | Work on AOSP (no GMS) | ML Kit bundled model |
| NFR-007 | Compatibility | Work on Play Store builds | Standard ML Kit |
| NFR-008 | Size | OCR model size impact | < 25MB APK increase |
| NFR-009 | Offline | Full functionality offline | Tesseract fallback |

---

## Technical Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    VoiceOSService                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐    ┌──────────────────┐              │
│  │ Standard Scraper │    │ Canvas Detector  │              │
│  │ (AccessibilityNode)│   │                  │              │
│  └────────┬─────────┘    └────────┬─────────┘              │
│           │                       │                         │
│           ▼                       ▼                         │
│  ┌──────────────────────────────────────────┐              │
│  │         CanvasAccessibilityManager        │              │
│  ├──────────────────────────────────────────┤              │
│  │ - detectCanvasContent()                   │              │
│  │ - requestOCRExtraction()                  │              │
│  │ - queryPlugins()                          │              │
│  │ - mergeResults()                          │              │
│  └──────────────────────────────────────────┘              │
│           │                                                 │
│           ├─────────────────┬─────────────────┐            │
│           ▼                 ▼                 ▼            │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │  OCREngine  │   │PluginManager│   │ CacheManager│      │
│  ├─────────────┤   ├─────────────┤   ├─────────────┤      │
│  │ - MLKit     │   │ - Discovery │   │ - ScreenHash│      │
│  │ - Tesseract │   │ - Verify    │   │ - TTL       │      │
│  └─────────────┘   │ - Execute   │   │ - Encrypt   │      │
│                    └─────────────┘   └─────────────┘      │
│                          │                                  │
│                          ▼                                  │
│                 ┌─────────────────┐                        │
│                 │ Plugin Interface │                        │
│                 ├─────────────────┤                        │
│                 │ UnityPlugin     │                        │
│                 │ ExoPlayerPlugin │                        │
│                 │ (Third-party)   │                        │
│                 └─────────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

```
1. AccessibilityEvent received
       │
       ▼
2. Standard scraping attempts
       │
       ▼
3. Canvas/SurfaceView detected? ──NO──► Return standard results
       │
      YES
       │
       ▼
4. Check plugin available for package? ──YES──► Query plugin
       │                                              │
      NO                                              │
       │                                              │
       ▼                                              │
5. OCR enabled by user? ──NO──► Log & skip            │
       │                                              │
      YES                                             │
       │                                              │
       ▼                                              │
6. Check cache for screen hash ──HIT──► Return cached │
       │                                              │
      MISS                                            │
       │                                              │
       ▼                                              │
7. Capture screenshot region                          │
       │                                              │
       ▼                                              │
8. Run OCR (MLKit or Tesseract)                       │
       │                                              │
       ▼                                              │
9. Convert to synthetic nodes                         │
       │                                              │
       ▼                                              │
10. Cache results ◄─────────────────────────────────┘
       │
       ▼
11. Merge with standard results
       │
       ▼
12. Return combined element list
```

---

## OCR Engine Strategy

### Build Variants

| Variant | OCR Engine | GMS Required | Size | Use Case |
|---------|------------|--------------|------|----------|
| `playStore` | ML Kit (dynamic) | Yes | +2MB | Play Store distribution |
| `playstoreBundled` | ML Kit (bundled) | No | +20MB | Play Store + offline |
| `aosp` | Tesseract only | No | +15MB | AOSP/custom ROMs |
| `full` | ML Kit + Tesseract | No | +35MB | Maximum compatibility |

### Gradle Configuration

```kotlin
android {
    flavorDimensions += "ocr"
    productFlavors {
        create("playStore") {
            dimension = "ocr"
            buildConfigField("String", "OCR_ENGINE", "\"mlkit_dynamic\"")
        }
        create("aosp") {
            dimension = "ocr"
            buildConfigField("String", "OCR_ENGINE", "\"tesseract\"")
        }
        create("full") {
            dimension = "ocr"
            buildConfigField("String", "OCR_ENGINE", "\"mlkit_bundled,tesseract\"")
        }
    }
}
```

---

## Plugin Interface Definition

```kotlin
/**
 * Interface for Canvas/SurfaceView accessibility plugins.
 *
 * Plugins can be:
 * - Internal (bundled with VoiceOS)
 * - External (installed as separate APK)
 * - Partner (verified third-party)
 */
interface CanvasAccessibilityPlugin {

    /**
     * Unique plugin identifier
     */
    val pluginId: String

    /**
     * Package names this plugin handles
     */
    val supportedPackages: List<String>

    /**
     * Check if plugin can handle this view
     */
    fun canHandle(packageName: String, className: String): Boolean

    /**
     * Extract accessible elements from canvas content
     *
     * @param context Current context
     * @param rootNode The Canvas/SurfaceView node
     * @param screenshot Optional screenshot bitmap
     * @return List of synthetic accessible elements
     */
    suspend fun getElements(
        context: Context,
        rootNode: AccessibilityNodeInfo,
        screenshot: Bitmap?
    ): List<SyntheticElement>

    /**
     * Perform action on a synthetic element
     *
     * @param element Target element from getElements()
     * @param action Action to perform (click, long_click, etc.)
     * @return True if action was performed
     */
    suspend fun performAction(
        element: SyntheticElement,
        action: AccessibilityAction
    ): Boolean
}

/**
 * Synthetic element representing Canvas content
 */
data class SyntheticElement(
    val id: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val className: String,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val confidence: Float,  // OCR confidence or plugin certainty
    val source: ElementSource  // OCR, PLUGIN, HEURISTIC
)

enum class ElementSource {
    OCR,           // Extracted via OCR
    PLUGIN,        // Provided by plugin
    HEURISTIC      // Inferred from position/size
}
```

---

## Database Schema

```sql
-- Track apps with Canvas/SurfaceView content
CREATE TABLE canvas_detected_apps (
    package_name TEXT PRIMARY KEY,
    first_detected_at INTEGER NOT NULL,
    last_detected_at INTEGER NOT NULL,
    detection_count INTEGER NOT NULL DEFAULT 1,
    has_plugin INTEGER NOT NULL DEFAULT 0,
    ocr_enabled INTEGER NOT NULL DEFAULT 0,
    notes TEXT
);

-- Cache OCR results
CREATE TABLE ocr_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    screen_hash TEXT NOT NULL UNIQUE,
    package_name TEXT NOT NULL,
    elements_json TEXT NOT NULL,  -- Encrypted JSON
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    ocr_engine TEXT NOT NULL
);

-- Index for cache cleanup
CREATE INDEX idx_ocr_cache_expires ON ocr_cache(expires_at);

-- Registered plugins
CREATE TABLE canvas_plugins (
    plugin_id TEXT PRIMARY KEY,
    package_name TEXT NOT NULL,
    supported_packages TEXT NOT NULL,  -- JSON array
    signature_hash TEXT NOT NULL,
    installed_at INTEGER NOT NULL,
    last_used_at INTEGER,
    is_internal INTEGER NOT NULL DEFAULT 0,
    is_verified INTEGER NOT NULL DEFAULT 0
);
```

---

## User Stories

| ID | Story | Acceptance Criteria |
|----|-------|---------------------|
| US-001 | As a user, I want VoiceOS to detect when an app uses Canvas so I know voice control is limited | Toast/notification shown on first detection |
| US-002 | As a user, I want to enable OCR to extract text from games | Settings toggle, works on next screen |
| US-003 | As a user, I want OCR results cached so repeated screens are fast | Second view < 50ms |
| US-004 | As a developer, I want to create a plugin for my game | SDK documentation available |
| US-005 | As a developer, I want my plugin to be verified by VoiceOS | Verification process documented |

---

## Success Criteria

- [ ] Canvas/SurfaceView detection accuracy > 95%
- [ ] OCR extracts text from 80%+ of readable Canvas content
- [ ] OCR processing < 500ms on mid-range devices
- [ ] Plugin architecture supports 3+ internal plugins
- [ ] Works on AOSP without Google Play Services
- [ ] APK size increase < 25MB for full variant
- [ ] User can enable/disable OCR per app
- [ ] No screenshots leave device (privacy)

---

## Implementation Phases

| Phase | Scope | Duration | Dependencies |
|-------|-------|----------|--------------|
| 1 | Canvas detection + logging | 1 week | None |
| 2 | OCR engine integration | 2 weeks | Phase 1 |
| 3 | Caching + encryption | 1 week | Phase 2 |
| 4 | Plugin interface | 2 weeks | Phase 1 |
| 5 | Internal plugins (Unity, ExoPlayer) | 2 weeks | Phase 4 |
| 6 | SDK documentation + samples | 1 week | Phase 4 |

---

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| OCR accuracy poor on stylized fonts | Medium | High | Train custom Tesseract model |
| Battery drain from continuous OCR | High | High | On-demand only, aggressive caching |
| Privacy concerns with screenshots | Medium | Critical | Local-only, encrypted, user opt-in |
| Plugin security vulnerabilities | Medium | Critical | Signature verification, sandboxing |
| APK size too large | Low | Medium | Build variants, dynamic delivery |

---

## Open Questions

1. Should OCR run automatically or require explicit "Scan" command?
2. Maximum cache size and TTL for OCR results?
3. Should we support real-time OCR for video content?
4. Partner program pricing/terms for verified plugins?

---

## References

- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition)
- [Tesseract Android](https://github.com/adaptech-cz/Tesseract4Android)
- [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast)
- [Android AccessibilityDelegate](https://developer.android.com/reference/android/view/View.AccessibilityDelegate)

---

**Next Steps:** `/plan` to create implementation plan
