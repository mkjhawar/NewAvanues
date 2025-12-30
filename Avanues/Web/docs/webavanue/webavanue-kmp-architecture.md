# WebAvanue - KMP Architecture Guide

**Version:** 2.0
**Date:** 2025-12-01
**KMP Coverage:** 88%

---

## Overview

WebAvanue is a Kotlin Multiplatform browser with 88% shared code. This document describes the architecture and abstraction patterns used to maximize code reuse.

---

## Folder Structure

```
common/libs/webavanue/
├── coredata/                      # Data layer (100% shared)
│   └── src/commonMain/
│       ├── domain/model/          # Tab, Favorite, History, Download, Settings
│       ├── domain/repository/     # BrowserRepository interface
│       └── data/repository/       # BrowserRepositoryImpl (SQLDelight)
│
├── universal/                     # Presentation & XR layer
│   ├── src/commonMain/            # 88% of code (21,377 LOC)
│   │   ├── commands/              # WebAvanueActionMapper
│   │   ├── download/              # DownloadQueue, FilenameUtils
│   │   ├── presentation/
│   │   │   ├── controller/        # CommonWebViewController, GestureMapper
│   │   │   ├── ui/                # Compose UI components
│   │   │   └── viewmodel/         # ViewModels
│   │   └── xr/                    # CommonXRManager, CommonPerformanceMonitor
│   │
│   └── src/androidMain/           # 12% Android-specific (2,904 LOC)
│       ├── download/              # SystemTime.android.kt
│       ├── presentation/
│       │   └── controller/        # AndroidWebViewController
│       └── xr/                    # AndroidXRManager, XRPerformanceMonitor
```

---

## Abstraction Patterns

### 1. WebViewController Abstraction

**Common Layer** (`commonMain`):
```kotlin
// CommonWebViewController.kt - 353 LOC of shared logic
abstract class CommonWebViewController(
    protected val tabViewModel: TabViewModel
) : WebViewController {

    // Shared scroll handling
    override suspend fun scrollUp(): ActionResult {
        tabViewModel.scrollUp()
        return ActionResult.success("Scrolled up")
    }

    // Uses GestureMapper for gesture → JS mapping
    override suspend fun performGesture(...): ActionResult {
        val script = GestureMapper.mapToScript(gestureType, x, y, modifiers)
        return executeGestureScript("JSON.stringify($script)")
    }

    // Abstract methods for platform-specific ops
    protected abstract suspend fun evaluateJavaScript(script: String): String?
    protected abstract fun clearCookiesInternal(): Boolean
}
```

**Android Layer** (`androidMain`):
```kotlin
// AndroidWebViewController.kt - 141 LOC
class AndroidWebViewController(
    private val webViewProvider: () -> WebView?,
    tabViewModel: TabViewModel
) : CommonWebViewController(tabViewModel) {

    override suspend fun evaluateJavaScript(script: String): String? {
        return webView?.evaluateJavaScript(script)
    }

    override fun clearCookiesInternal(): Boolean {
        CookieManager.getInstance().removeAllCookies(...)
        return true
    }
}
```

### 2. XR Performance Monitor Abstraction

**Common Layer** (`commonMain`):
```kotlin
// CommonPerformanceMonitor.kt - 328 LOC
abstract class CommonPerformanceMonitor {
    // Shared thresholds
    companion object {
        const val FPS_WARNING_THRESHOLD = 45f
        const val BATTERY_CRITICAL_LEVEL = 10
    }

    // Shared warning generation
    private fun checkWarnings() {
        if (m.averageFps < FPS_CRITICAL_THRESHOLD) {
            newWarnings.add(PerformanceWarning(...))
        }
    }

    // Platform-specific battery reading
    protected abstract fun getBatteryLevel(): Int
}
```

**Android Layer** (`androidMain`):
```kotlin
// XRPerformanceMonitor.kt - 77 LOC
class XRPerformanceMonitor : CommonPerformanceMonitor() {

    override fun getBatteryLevel(): Int {
        val intent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    }
}
```

### 3. Download Queue Abstraction

**Common Layer** (`commonMain`):
```kotlin
// DownloadQueue.kt - 323 LOC
interface DownloadQueue {
    suspend fun enqueue(request: DownloadRequest): String?
    suspend fun cancel(downloadId: String): Boolean
    fun observeProgress(downloadId: String): Flow<DownloadProgress>
}

data class DownloadRequest(
    val url: String,
    val filename: String,
    val mimeType: String? = null
)

object FilenameUtils {
    fun guessFilename(url: String, contentDisposition: String?, mimeType: String?): String
}
```

**Android Layer** (`androidMain`):
```kotlin
// Uses Android DownloadManager
class AndroidDownloadQueue : DownloadQueue {
    override suspend fun enqueue(request: DownloadRequest): String? {
        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE)
        return downloadManager.enqueue(DownloadManager.Request(...))
    }
}
```

---

## GestureMapper (100% Shared)

Maps 50+ gesture types to JavaScript calls:

```kotlin
// GestureMapper.kt - 179 LOC (commonMain)
object GestureMapper {
    fun mapToScript(gestureType: String, x: Float, y: Float, modifiers: Int): String? {
        return when (gestureType) {
            "GESTURE_CLICK" -> "window.AvanuesGestures.click($x, $y)"
            "GESTURE_SWIPE_LEFT" -> "window.AvanuesGestures.swipeLeft($x, $y)"
            "GESTURE_ZOOM_IN" -> "window.AvanuesGestures.zoomIn($x, $y)"
            // ... 50+ gesture mappings
            else -> null
        }
    }
}
```

---

## KMP Statistics

| Component | commonMain | androidMain | % Shared |
|-----------|------------|-------------|----------|
| WebViewController | 353 LOC | 141 LOC | 71% |
| XR PerformanceMonitor | 328 LOC | 77 LOC | 81% |
| XR SessionManager | 215 LOC | 68 LOC | 76% |
| GestureMapper | 179 LOC | 0 LOC | 100% |
| DownloadQueue | 323 LOC | 8 LOC | 98% |
| **Total** | **21,377 LOC** | **2,904 LOC** | **88%** |

---

## Adding New Platform (iOS/Desktop)

To add iOS support:

1. **Create `iosMain` folder**
2. **Implement abstract methods:**
   ```kotlin
   // IosWebViewController.kt
   class IosWebViewController : CommonWebViewController() {
       override suspend fun evaluateJavaScript(script: String): String? {
           return wkWebView.evaluateJavaScript(script)
       }
   }
   ```

3. **Create XR implementations:**
   ```kotlin
   // IosXRManager.kt
   class IosXRManager : CommonXRManager {
       // Use ARKit instead of ARCore
   }
   ```

4. **Create download queue:**
   ```kotlin
   // IosDownloadQueue.kt
   class IosDownloadQueue : DownloadQueue {
       // Use URLSession
   }
   ```

---

## Key Files Reference

| File | Purpose | LOC |
|------|---------|-----|
| `GestureMapper.kt` | Gesture → JS mapping | 179 |
| `GestureCoordinateResolver.kt` | Coordinate fallback logic | 145 |
| `CommonWebViewController.kt` | Shared WebView operations | 353 |
| `DownloadQueue.kt` | Download abstraction + FilenameUtils | 323 |
| `CommonPerformanceMonitor.kt` | XR metrics & warnings | 328 |
| `CommonSessionManager.kt` | XR session state machine | 215 |
| `CommonXRManager.kt` | XR manager interface | 155 |
| `XRState.kt` | XR state models | 135 |

---

**Last Updated:** 2025-12-01
**Maintainer:** WebAvanue Team
