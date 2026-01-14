# Specification: Universal QR Scanner Module for Offline Licensing

**Project:** AvaCloudCentral / NewAvanues
**Date:** 2026-01-11
**Version:** 1.0
**Platforms:** Android, iOS, Desktop, Web (KMP)

---

## Executive Summary

Create a universal QR code scanning module using Kotlin Multiplatform (KMP) that provides a consistent API across all platforms while leveraging platform-specific camera/scanning implementations. The module will integrate with the existing Offline License System to enable license activation via QR codes.

---

## Problem Statement

The current offline licensing system can generate QR codes containing signed license payloads, but there's no client-side scanning capability. Each platform has different camera APIs:
- **Android:** CameraX + ML Kit
- **iOS:** AVFoundation + Vision
- **Desktop:** Webcam + ZXing (or file import)
- **Web:** MediaDevices API + jsQR

Without a unified approach, we'd have 4 completely separate implementations with no code reuse.

---

## Proposed Solution

Use KMP's `expect/actual` pattern to create:
1. **Common API** in `shared/licensing` - defines the scanning interface and result types
2. **Platform implementations** - each platform implements the scanning logic
3. **UI Components** - platform-specific UI that uses the common API

---

## Architecture

```
kmp/shared/licensing/
├── commonMain/kotlin/.../qrscanner/
│   ├── QrScannerService.kt          # expect interface
│   ├── QrScanResult.kt              # sealed class for results
│   ├── QrScannerConfig.kt           # configuration options
│   └── QrScannerCallback.kt         # callback interface
├── androidMain/kotlin/.../qrscanner/
│   └── QrScannerService.kt          # actual - CameraX + ML Kit
├── iosMain/kotlin/.../qrscanner/
│   └── QrScannerService.kt          # actual - AVFoundation
├── desktopMain/kotlin/.../qrscanner/
│   └── QrScannerService.kt          # actual - ZXing + webcam/file
└── jsMain/kotlin/.../qrscanner/
    └── QrScannerService.kt          # actual - Web APIs

kmp/clients/
├── android/app/.../ui/screens/
│   └── QrScannerScreen.kt           # Compose UI using service
├── ios/.../Views/
│   └── QRScannerView.swift          # SwiftUI using service
└── desktop/.../ui/
    └── QrScannerWindow.kt           # Compose Desktop UI
```

---

## Functional Requirements

### FR-1: Common QR Scanner Interface
- **FR-1.1:** Define `QrScannerService` expect class with `startScanning()`, `stopScanning()`, `isScanning` state
- **FR-1.2:** Define `QrScanResult` sealed class: `Success(content: String)`, `Error(message: String)`, `PermissionDenied`
- **FR-1.3:** Define `QrScannerCallback` interface for async scan results
- **FR-1.4:** Support configuration: camera facing (front/back), continuous vs single scan

### FR-2: Android Implementation
- **FR-2.1:** Use CameraX for camera preview
- **FR-2.2:** Use ML Kit Barcode Scanning for QR detection
- **FR-2.3:** Handle camera permissions via Accompanist
- **FR-2.4:** Provide haptic feedback on scan

### FR-3: iOS Implementation
- **FR-3.1:** Use AVFoundation `AVCaptureSession` for camera
- **FR-3.2:** Use Vision framework `VNDetectBarcodesRequest` for QR detection
- **FR-3.3:** Handle camera permission via Info.plist + runtime request
- **FR-3.4:** Provide haptic feedback via UIImpactFeedbackGenerator

### FR-4: Desktop Implementation
- **FR-4.1:** Support webcam scanning via OpenCV or JavaCV (optional)
- **FR-4.2:** Support file/image import with ZXing decoding
- **FR-4.3:** Drag-and-drop QR image support

### FR-5: Web Implementation
- **FR-5.1:** Use `navigator.mediaDevices.getUserMedia` for camera access
- **FR-5.2:** Use jsQR or ZXing-js for QR detection
- **FR-5.3:** Canvas-based frame processing

### FR-6: Integration with Offline Licensing
- **FR-6.1:** Scanned QR content passed to `OfflineLicenseDecoder`
- **FR-6.2:** Decoded payload verified via `OfflineLicenseVerifier`
- **FR-6.3:** Valid licenses activated via `OfflineActivationService`

---

## Non-Functional Requirements

### NFR-1: Performance
- QR detection latency < 500ms on modern devices
- Camera preview at 30fps minimum
- Memory usage < 50MB for camera session

### NFR-2: Security
- No QR content transmitted to external servers
- All processing done locally
- Signature verification before activation

### NFR-3: Accessibility
- Screen reader support for scan results
- High contrast mode for viewfinder
- Audio feedback option for successful scans

### NFR-4: Offline Capability
- Full functionality without internet connection
- Local signature verification

---

## Platform-Specific Dependencies

| Platform | Camera | QR Decoder | Permission |
|----------|--------|------------|------------|
| Android | CameraX 1.3.x | ML Kit 17.x | Accompanist |
| iOS | AVFoundation | Vision | Info.plist |
| Desktop | OpenCV/File | ZXing 3.x | N/A |
| Web | MediaDevices | jsQR/ZXing-js | Browser prompt |

---

## API Design

### Common Interface (expect)

```kotlin
// QrScannerService.kt
expect class QrScannerService {
    val isScanning: StateFlow<Boolean>
    val scanResults: Flow<QrScanResult>

    suspend fun startScanning(config: QrScannerConfig = QrScannerConfig.DEFAULT)
    suspend fun stopScanning()
    fun processImage(imageData: ByteArray): QrScanResult?
}

// QrScanResult.kt
sealed class QrScanResult {
    data class Success(val content: String, val format: QrFormat) : QrScanResult()
    data class Error(val error: QrScanError) : QrScanResult()
    object PermissionDenied : QrScanResult()
    object NoCameraAvailable : QrScanResult()
}

// QrScannerConfig.kt
data class QrScannerConfig(
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val scanMode: ScanMode = ScanMode.SINGLE,
    val enableHapticFeedback: Boolean = true,
    val acceptedFormats: Set<QrFormat> = setOf(QrFormat.QR_CODE)
)

enum class CameraFacing { FRONT, BACK }
enum class ScanMode { SINGLE, CONTINUOUS }
enum class QrFormat { QR_CODE, DATA_MATRIX, AZTEC, PDF417 }
```

---

## UI Components

### Android (Compose)
```kotlin
@Composable
fun QrScannerScreen(
    onScanResult: (QrScanResult) -> Unit,
    onNavigateBack: () -> Unit
)
```

### iOS (SwiftUI)
```swift
struct QRScannerView: View {
    var onScanResult: (QrScanResult) -> Void
    var onDismiss: () -> Void
}
```

### Desktop (Compose Desktop)
```kotlin
@Composable
fun QrScannerWindow(
    onScanResult: (QrScanResult) -> Unit,
    onClose: () -> Unit
)
```

---

## Testing Strategy

1. **Unit Tests (commonTest)**
   - QrScanResult handling
   - Config validation
   - Mock scanner tests

2. **Integration Tests**
   - End-to-end: scan → decode → verify → activate
   - Error handling paths

3. **UI Tests (Platform-specific)**
   - Permission flows
   - Camera preview rendering
   - Result display

---

## Out of Scope

- Barcode generation (already exists in backend)
- Multi-QR batch scanning
- AR overlay features
- Cloud-based QR processing

---

## Acceptance Criteria

1. [ ] Common API compiles for all platforms
2. [ ] Android: Camera preview + scan works on API 26+
3. [ ] iOS: Camera preview + scan works on iOS 13+
4. [ ] Desktop: File import + scan works
5. [ ] Web: Browser camera + scan works on Chrome/Safari/Firefox
6. [ ] Integration with OfflineActivationService successful
7. [ ] Unit tests pass on all platforms
8. [ ] Manual entry fallback works on all platforms

---

## Dependencies

- **Existing:** Phase 1 (Backend) and Phase 2 (KMP Offline Validation) completed
- **New:** Platform-specific camera/scanning libraries listed above

---

**Ready for implementation planning.**
