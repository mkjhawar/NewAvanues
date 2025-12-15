# Screenshot Capture Feature

## Overview

The screenshot capture feature allows users to capture webpage screenshots in two modes:
- **Visible Area**: Captures only the current viewport
- **Full Page**: Captures the entire page by scrolling and stitching

## Architecture

### Common Layer (KMP)

#### Core Interfaces
- `ScreenshotCapture` - Platform-agnostic interface for screenshot operations
- `ScreenshotData` - Wrapper for platform-specific image data (expect/actual)
- `ScreenshotType` - Enum for VISIBLE_AREA or FULL_PAGE
- `ScreenshotResult` - Sealed class for Success, Error, Progress states

#### Request/Response
- `ScreenshotRequest` - Configuration for capture (type, quality, save options)
- `ScreenshotResult.Progress` - Progress updates (0.0 to 1.0)
- `ScreenshotResult.Success` - Contains ScreenshotData and optional filepath
- `ScreenshotResult.Error` - Error message and cause

#### UI Components
- `ScreenshotTypeDialog` - User selects VISIBLE_AREA or FULL_PAGE
- `ScreenshotProgressDialog` - Shows progress with cancel option
- `ScreenshotPreviewDialog` - Shows captured screenshot with Save/Share actions
- `ScreenshotErrorDialog` - Displays error with optional retry

#### Manager
- `ScreenshotManager` - High-level orchestrator for capture workflow
  - Manages state transitions
  - Handles progress updates
  - Provides clean API for UI integration

### Android Implementation

#### Screenshot Capture
- `AndroidScreenshotCapture` - WebView-based implementation
  - **Visible Area**: Uses `View.draw(Canvas)` on WebView
  - **Full Page**: Scrolls through page, captures tiles, stitches together
  - Memory management: Limits max height to 15,000px
  - Progress callbacks during full page capture

#### File Management
- Saves to `Pictures/WebAvanue/` directory
- Filename format: `Screenshot_YYYYMMDD_HHMMSS.png`
- Uses MediaStore API for Android 10+ (scoped storage)
- Uses legacy file API for older versions

#### Notifications
- `ScreenshotNotificationHelper` - Shows "Screenshot saved" notification
- Actions: View, Share, Delete
- Uses notification channels (Android O+)

## Usage

### Basic Usage (UI Layer)

```kotlin
// 1. Create ScreenshotManager
val screenshotManager = remember { ScreenshotManager(rememberCoroutineScope()) }

// 2. Track state
var screenshotState by remember { mutableStateOf<ScreenshotState>(ScreenshotState.Idle) }

// 3. Start capture
Button(onClick = {
    screenshotManager.startScreenshotCapture(webView) { state ->
        screenshotState = state
    }
}) {
    Text("Screenshot")
}

// 4. Show dialogs based on state
when (val state = screenshotState) {
    ScreenshotState.SelectingType -> {
        ScreenshotTypeDialog(
            onDismiss = { screenshotManager.reset { screenshotState = it } },
            onSelectType = { type ->
                screenshotManager.captureScreenshot(type) { screenshotState = it }
            }
        )
    }

    is ScreenshotState.Capturing -> {
        ScreenshotProgressDialog(
            progress = state.progress,
            message = state.message,
            onCancel = { screenshotManager.cancelCapture { screenshotState = it } }
        )
    }

    is ScreenshotState.Success -> {
        ScreenshotPreviewDialog(
            screenshotPath = state.filepath,
            onSave = { /* Already saved */ },
            onShare = {
                state.filepath?.let { path ->
                    scope.launch {
                        screenshotManager.shareScreenshot(path)
                    }
                }
            },
            onDismiss = { screenshotManager.reset { screenshotState = it } }
        )
    }

    is ScreenshotState.Error -> {
        ScreenshotErrorDialog(
            error = state.error,
            onDismiss = { screenshotManager.reset { screenshotState = it } }
        )
    }

    ScreenshotState.Idle -> { /* No dialog */ }
}
```

### Advanced Usage (Direct API)

```kotlin
// Create screenshot capture instance
val screenshotCapture = createScreenshotCapture(webView)

// Capture with Flow
screenshotCapture.capture(
    ScreenshotRequest(
        type = ScreenshotType.FULL_PAGE,
        quality = 80,
        saveToGallery = true
    )
).collect { result ->
    when (result) {
        is ScreenshotResult.Progress -> {
            println("${result.progress * 100}%: ${result.message}")
        }
        is ScreenshotResult.Success -> {
            println("Saved to: ${result.filepath}")
            // result.data is ScreenshotData (wraps Bitmap on Android)
        }
        is ScreenshotResult.Error -> {
            println("Error: ${result.error}")
        }
    }
}
```

## ViewModel Integration

```kotlin
// In TabViewModel
fun captureScreenshot(
    type: ScreenshotType,
    quality: Int = 80,
    saveToGallery: Boolean = true,
    onProgress: (Float, String) -> Unit,
    onComplete: (ScreenshotData, String?) -> Unit,
    onError: (String) -> Unit
)
```

This method logs the request. Actual capture is handled by the UI layer with WebView access.

## Memory Management

### Full Page Capture
- Pages are captured in viewport-sized tiles
- Each tile is stitched to final bitmap immediately
- Intermediate tiles are recycled after stitching
- Maximum page height: 15,000 pixels (prevents OOM)
- Long pages are truncated with warning

### Cleanup
- Call `ScreenshotData.recycle()` when done to free memory
- `ScreenshotManager.cleanup()` cancels ongoing captures

## Notifications

After successful capture:
1. Shows "Screenshot saved" notification
2. Actions:
   - **View**: Opens screenshot in gallery
   - **Share**: Opens Android share sheet
   - **Delete**: Deletes file and dismisses notification

## File Storage

### Android 10+ (Scoped Storage)
- Uses MediaStore API
- Saves to: `Pictures/WebAvanue/`
- Automatic media scanning

### Android 9 and below
- Direct file write to external storage
- Manual media scanner broadcast

## Error Handling

Common errors:
- No active tab
- WebView not available
- Storage permission denied
- Insufficient storage space
- Page too large (>15,000px)
- Memory allocation failure

All errors are wrapped in `ScreenshotResult.Error` with user-friendly messages.

## Testing

### Unit Tests (TODO)
- `ScreenshotFilenameUtils` - Filename generation
- `ScreenshotManager` - State transitions
- `ScreenshotRequest` - Validation

### Integration Tests (TODO)
- Visible area capture
- Full page capture (various page heights)
- File saving (MediaStore and legacy)
- Share intent
- Memory management (OOM scenarios)

## Future Enhancements

- [ ] Selection mode (capture specific area)
- [ ] Edit screenshot before saving (crop, annotate)
- [ ] Screenshot history/gallery
- [ ] iOS implementation (WKWebView)
- [ ] Desktop implementation (JavaFX/Swing)
- [ ] Web implementation (Canvas API)
- [ ] PDF export
- [ ] OCR text extraction from screenshot

## Dependencies

- Kotlin Coroutines (Flow, suspend functions)
- Jetpack Compose (UI components)
- Android Graphics (Bitmap, Canvas)
- Android Storage (MediaStore, FileProvider)
- kotlinx-datetime (timestamp formatting)

## Permissions Required

### Android
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

Android 10+ uses scoped storage (no permission required for app-specific directories).

## FileProvider Configuration

Add to `AndroidManifest.xml`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

Add `res/xml/file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="screenshots" path="Pictures/WebAvanue/" />
</paths>
```
