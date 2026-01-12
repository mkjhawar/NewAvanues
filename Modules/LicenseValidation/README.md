# LicenseValidation Module

Universal QR Scanner for license validation across all platforms using Kotlin Multiplatform.

## Overview

This module provides a consistent QR code scanning API across:
- **Android**: CameraX + ML Kit
- **iOS**: AVFoundation
- **Desktop**: ZXing (file import)
- **Web/JS**: MediaDevices API + jsQR

## Structure

```
src/
├── commonMain/          # Common API (expect declarations)
│   └── qrscanner/
│       ├── QrScanResult.kt      # Scan result sealed class
│       ├── QrScannerConfig.kt   # Configuration options
│       └── QrScannerService.kt  # Scanner service (expect)
├── androidMain/         # Android implementation (CameraX + ML Kit)
├── iosMain/             # iOS implementation (AVFoundation)
├── desktopMain/         # Desktop implementation (ZXing)
├── jsMain/              # Web implementation (jsQR)
└── commonTest/          # Unit tests
```

## Usage

```kotlin
val scanner = QrScannerService()

// Start scanning
scanner.startScanning(QrScannerConfig.DEFAULT)

// Collect results
scanner.scanResults.collect { result ->
    when (result) {
        is QrScanResult.Success -> handleCode(result.content)
        is QrScanResult.Error -> showError(result.error)
        QrScanResult.PermissionDenied -> requestPermission()
        QrScanResult.NoCameraAvailable -> showManualEntry()
        QrScanResult.Cancelled -> dismiss()
    }
}

// Stop scanning
scanner.stopScanning()
```

## Configuration

```kotlin
val config = QrScannerConfig(
    cameraFacing = CameraFacing.BACK,
    scanMode = ScanMode.SINGLE,
    enableHapticFeedback = true,
    acceptedFormats = setOf(QrFormat.QR_CODE),
    resolution = ScannerResolution.HD
)
```

## Dependencies

| Platform | Library |
|----------|---------|
| Android | CameraX 1.3.1, ML Kit 17.2.0 |
| iOS | AVFoundation (native) |
| Desktop | ZXing 3.5.2 |
| Web | jsQR 1.4.0 |

## License

Copyright 2026 NewAvanues. All rights reserved.
