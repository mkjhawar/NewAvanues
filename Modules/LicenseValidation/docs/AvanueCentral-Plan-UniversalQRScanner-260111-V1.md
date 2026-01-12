# Implementation Plan: Universal QR Scanner Module

**Spec:** AvanueCentral-Spec-UniversalQRScanner-260111-V1
**Date:** 2026-01-11
**Platforms:** Android, iOS, Desktop, Web (KMP)

---

## Overview

| Metric | Value |
|--------|-------|
| Total Tasks | 28 |
| Total Phases | 5 |
| Estimated Effort | ~12 hours |
| Swarm Recommended | Yes (4 platform implementations can run in parallel) |

---

## Phase 1: Common API (KMP shared/licensing) - 2 hours

### Task 1.1: Create QrScanResult Sealed Class
**File:** `kmp/shared/licensing/src/commonMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrScanResult.kt`
- Define `Success`, `Error`, `PermissionDenied`, `NoCameraAvailable` variants
- Include `QrFormat` enum
- Include `QrScanError` enum

### Task 1.2: Create QrScannerConfig Data Class
**File:** `kmp/shared/licensing/src/commonMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrScannerConfig.kt`
- `cameraFacing: CameraFacing`
- `scanMode: ScanMode`
- `enableHapticFeedback: Boolean`
- `acceptedFormats: Set<QrFormat>`

### Task 1.3: Create QrScannerService Expect Declaration
**File:** `kmp/shared/licensing/src/commonMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrScannerService.kt`
- `expect class QrScannerService`
- `isScanning: StateFlow<Boolean>`
- `scanResults: Flow<QrScanResult>`
- `startScanning()`, `stopScanning()`, `processImage()`

### Task 1.4: Create QrCodeActivationUseCase
**File:** `kmp/shared/licensing/src/commonMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrCodeActivationUseCase.kt`
- Integrate `OfflineLicenseDecoder`, `OfflineLicenseVerifier`, `OfflineActivationService`
- `suspend fun activateFromQrContent(content: String, deviceFingerprint: String): OfflineActivationResult`

### Task 1.5: Update DI Container
**File:** `kmp/shared/licensing/src/commonMain/kotlin/com/avacloud/shared/licensing/di/LicensingContainer.kt`
- Add `qrCodeActivationUseCase` lazy property

---

## Phase 2: Android Implementation - 2.5 hours

### Task 2.1: Create Android QrScannerService Actual
**File:** `kmp/shared/licensing/src/androidMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrScannerService.kt`
- CameraX setup with `ProcessCameraProvider`
- ML Kit `BarcodeScanner` integration
- Frame analysis with `ImageAnalysis`
- Permission state handling

### Task 2.2: Add Android Dependencies
**File:** `kmp/shared/licensing/build.gradle.kts`
- Add CameraX dependencies for androidMain
- Add ML Kit dependency for androidMain
- Add Accompanist permissions

### Task 2.3: Create Android QrScannerScreen
**File:** `kmp/clients/android/app/src/main/kotlin/com/avacloud/licensing/ui/screens/QrScannerScreen.kt`
- Compose UI with camera preview
- Permission handling UI
- Scanning overlay with viewfinder
- Success/error dialogs
- Manual entry fallback

### Task 2.4: Create Android QrScannerViewModel
**File:** `kmp/clients/android/app/src/main/kotlin/com/avacloud/licensing/ui/viewmodel/QrScannerViewModel.kt`
- Collect from `QrScannerService.scanResults`
- Call `QrCodeActivationUseCase`
- Manage UI state

### Task 2.5: Update Android Navigation
**File:** `kmp/clients/android/app/src/main/kotlin/com/avacloud/licensing/ui/navigation/LicensingNavHost.kt`
- Add QrScanner route
- Handle navigation callbacks

### Task 2.6: Update Android Manifest
**File:** `kmp/clients/android/app/src/main/AndroidManifest.xml`
- Add CAMERA permission
- Add camera feature declaration

---

## Phase 3: iOS Implementation - 2.5 hours

### Task 3.1: Create iOS QrScannerService Actual
**File:** `kmp/shared/licensing/src/iosMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrScannerService.kt`
- Kotlin/Native interop with AVFoundation
- `AVCaptureSession` configuration
- `AVCaptureMetadataOutput` for QR detection

### Task 3.2: Create iOS QRScannerView (SwiftUI)
**File:** `kmp/clients/ios/LicensingApp/Sources/Views/QRScannerView.swift`
- SwiftUI wrapper for camera preview
- Permission request UI
- Scanning overlay
- Result handling

### Task 3.3: Create iOS QRScannerCoordinator
**File:** `kmp/clients/ios/LicensingApp/Sources/Coordinators/QRScannerCoordinator.swift`
- Bridge between SwiftUI and KMP service
- Handle `AVCaptureMetadataOutputObjectsDelegate`

### Task 3.4: Update iOS Info.plist
**File:** `kmp/clients/ios/LicensingApp/Resources/Info.plist`
- Add `NSCameraUsageDescription`

### Task 3.5: Update iOS Navigation
- Add navigation to QR scanner from license list

---

## Phase 4: Desktop Implementation - 2 hours

### Task 4.1: Create Desktop QrScannerService Actual
**File:** `kmp/shared/licensing/src/desktopMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrScannerService.kt`
- ZXing integration for image decoding
- File picker integration
- Optional webcam support (OpenCV/JavaCV)

### Task 4.2: Add Desktop Dependencies
**File:** `kmp/shared/licensing/build.gradle.kts`
- Add ZXing core and javase dependencies

### Task 4.3: Create Desktop QrScannerWindow
**File:** `kmp/clients/desktop/src/main/kotlin/com/avacloud/licensing/desktop/ui/QrScannerWindow.kt`
- Compose Desktop UI
- File picker / drag-and-drop
- Image preview
- Scan result display

### Task 4.4: Update Desktop Main
- Add menu item / button to open QR scanner

---

## Phase 5: Web/JS Implementation - 2 hours

### Task 5.1: Create JS QrScannerService Actual
**File:** `kmp/shared/licensing/src/jsMain/kotlin/com/avacloud/shared/licensing/qrscanner/QrScannerService.kt`
- External declarations for MediaDevices API
- External declarations for jsQR library
- Canvas-based frame processing

### Task 5.2: Add JS Dependencies
**File:** `kmp/shared/licensing/build.gradle.kts`
- Add jsQR npm dependency

### Task 5.3: Create Web QR Scanner Component (if applicable)
- React/web component for camera preview
- File upload fallback

---

## Phase 6: Testing & Documentation - 1 hour

### Task 6.1: Common Unit Tests
**File:** `kmp/shared/licensing/src/commonTest/kotlin/com/avacloud/shared/licensing/qrscanner/`
- QrScanResult tests
- QrScannerConfig tests
- QrCodeActivationUseCase tests (with mocks)

### Task 6.2: Android Instrumented Tests
- Camera permission flow
- Scan result handling

### Task 6.3: Documentation
- Update README with QR scanner usage
- API documentation

---

## Dependencies Between Phases

```
Phase 1 (Common API) ────┬────> Phase 2 (Android)
                         ├────> Phase 3 (iOS)
                         ├────> Phase 4 (Desktop)
                         └────> Phase 5 (Web/JS)

Phases 2-5 can run in PARALLEL (Swarm recommended)

Phases 2-5 ──────────────────> Phase 6 (Testing)
```

---

## File Changes Summary

### New Files

| Path | Purpose |
|------|---------|
| `shared/licensing/.../qrscanner/QrScanResult.kt` | Scan result types |
| `shared/licensing/.../qrscanner/QrScannerConfig.kt` | Configuration |
| `shared/licensing/.../qrscanner/QrScannerService.kt` (expect) | Common interface |
| `shared/licensing/.../qrscanner/QrCodeActivationUseCase.kt` | Activation logic |
| `shared/licensing/androidMain/.../QrScannerService.kt` | Android impl |
| `shared/licensing/iosMain/.../QrScannerService.kt` | iOS impl |
| `shared/licensing/desktopMain/.../QrScannerService.kt` | Desktop impl |
| `shared/licensing/jsMain/.../QrScannerService.kt` | Web impl |
| `clients/android/.../QrScannerScreen.kt` | Android UI |
| `clients/android/.../QrScannerViewModel.kt` | Android VM |
| `clients/ios/.../QRScannerView.swift` | iOS UI |
| `clients/desktop/.../QrScannerWindow.kt` | Desktop UI |

### Modified Files

| Path | Changes |
|------|---------|
| `shared/licensing/build.gradle.kts` | Add platform dependencies |
| `shared/licensing/di/LicensingContainer.kt` | Add QR services |
| `clients/android/AndroidManifest.xml` | Camera permission |
| `clients/android/.../LicensingNavHost.kt` | Add QR route |
| `clients/ios/.../Info.plist` | Camera usage description |

---

## Quality Gates

| Phase | Gate |
|-------|------|
| Phase 1 | Common API compiles for all targets |
| Phase 2 | Android scans QR in < 500ms |
| Phase 3 | iOS scans QR in < 500ms |
| Phase 4 | Desktop decodes QR from file |
| Phase 5 | Web scans via browser camera |
| Phase 6 | All tests pass |

---

## Swarm Execution Strategy

When using `.swarm`:
1. **Agent 1:** Phase 1 (Common API) - Sequential first
2. **Agent 2:** Phase 2 (Android) - After Phase 1
3. **Agent 3:** Phase 3 (iOS) - After Phase 1 (parallel with Android)
4. **Agent 4:** Phase 4 (Desktop) - After Phase 1 (parallel)
5. **Agent 5:** Phase 5 (Web/JS) - After Phase 1 (parallel)
6. **Main Agent:** Phase 6 (Testing) - After all platforms complete

---

## Rollback Plan

If issues arise:
1. Common API changes are additive - no breaking changes
2. Platform implementations are isolated
3. Manual entry fallback always available
4. Existing online activation unchanged

---

**Ready to implement. Start with Phase 1 (Common API)?**
